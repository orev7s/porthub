#!/usr/bin/env python3
"""
Build games.json from a list of GitHub repos.

    python3 tools/build_catalog.py repos.txt -o games.json
    python3 tools/build_catalog.py repos.txt --readme --merge games.json -o games.json

Requires the GitHub CLI (`gh auth login`). Everything it knows comes from the GitHub API:
repo metadata, licence, latest release assets (.apk), README keywords. Facts it cannot
infer (which retail game, release year, publisher) come from overrides.json, so the
generated file is reproducible and never guesses.
"""

from __future__ import annotations

import argparse
import base64
import hashlib
import json
import re
import subprocess
import sys
import urllib.parse
import urllib.request
from datetime import datetime, timezone
from pathlib import Path

ACCENTS = ["#6C7BFF", "#FF6B9A", "#4ADEDE", "#FFB86B", "#B08CFF", "#57D9A3", "#7FA34A", "#B4342C"]

ABI_PATTERNS = [
    ("arm64-v8a", re.compile(r"arm64|aarch64|arm64-v8a", re.I)),
    ("armeabi-v7a", re.compile(r"armeabi|armv7|arm32", re.I)),
    ("x86_64", re.compile(r"x86_64|x64", re.I)),
    ("universal", re.compile(r"universal|all-abi", re.I)),
]


def gh(path: str):
    proc = subprocess.run(
        ["gh", "api", path, "-H", "Accept: application/vnd.github+json"],
        capture_output=True,
        text=True,
    )
    if proc.returncode != 0:
        raise RuntimeError(f"gh api {path} failed: {proc.stderr.strip()[:300]}")
    return json.loads(proc.stdout)


def slug(text: str) -> str:
    text = text.lower().replace("_", "-").replace(" ", "-")
    text = re.sub(r"[^a-z0-9-]", "", text)
    text = re.sub(r"-{2,}", "-", text).strip("-")
    for suffix in ("-android", "-port", "-recomp", "-decomp"):
        if text.endswith(suffix) and len(text) > len(suffix) + 2:
            text = text[: -len(suffix)]
    return text


def prettify(name: str) -> str:
    words = re.split(r"[-_]", name)
    return " ".join(w[:1].upper() + w[1:] for w in words if w)


def infer_port_type(text: str) -> str:
    t = text.lower()
    if "recompil" in t:
        return "recompilation"
    if "decompil" in t or "decomp" in t:
        return "decompilation"
    if "source port" in t or "sourceport" in t:
        return "source-port"
    if "engine" in t or "reimplement" in t:
        return "engine"
    return "unknown"


def infer_status(repo: dict, text: str) -> str:
    if repo.get("archived"):
        return "archived"
    t = text.lower()
    if "work in progress" in t or "wip" in t:
        return "wip"
    if "alpha" in t:
        return "alpha"
    if "beta" in t:
        return "beta"
    return "stable"


def abis_from_name(name: str) -> list[str]:
    found = [abi for abi, pattern in ABI_PATTERNS if pattern.search(name)]
    return found or []


def accent_for(key: str) -> str:
    digest = hashlib.sha1(key.encode()).hexdigest()
    return ACCENTS[int(digest[:8], 16) % len(ACCENTS)]


def readme_text(full_name: str) -> str:
    try:
        data = gh(f"repos/{full_name}/readme")
        return base64.b64decode(data.get("content", "")).decode("utf-8", "ignore")
    except Exception:
        return ""


def fetch_repo(full_name: str, with_readme: bool) -> dict:
    repo = gh(f"repos/{full_name}")
    readme = readme_text(full_name) if with_readme else ""
    blob = f"{repo.get('description') or ''}\n{readme}\n{repo.get('name','')}"

    downloads = []
    release_tag = None
    try:
        release = gh(f"repos/{full_name}/releases/latest")
        release_tag = release.get("tag_name")
        for asset in release.get("assets", []):
            name = asset.get("name", "")
            if not name.lower().endswith(".apk"):
                continue
            downloads.append(
                {
                    "label": name,
                    "kind": "apk",
                    "url": asset.get("browser_download_url"),
                    "version": release_tag,
                    "sizeMb": round(asset.get("size", 0) / 1_048_576, 1) or None,
                    "abi": (abis_from_name(name) or [None])[0],
                }
            )
    except Exception:
        release_tag = None

    supports_android = bool(downloads) or bool(re.search(r"\bandroid\b", blob, re.I))

    game = {
        "id": slug(repo["name"]),
        "title": prettify(repo["name"]),
        "portType": infer_port_type(blob),
        "status": infer_status(repo, blob),
        "description": (repo.get("description") or "").strip(),
        "tags": [t for t in repo.get("topics", [])][:8],
        "android": {
            "supported": supports_android,
            "abis": sorted({d["abi"] for d in downloads if d.get("abi")}),
            "requiresGameFiles": "owned" if re.search(r"\b(rom|iso|mpq|game files|original game)\b", blob, re.I) else "unknown",
        },
        "artwork": {"accent": accent_for(repo["full_name"])},
        "links": {
            "repo": repo["html_url"],
            "releases": f"{repo['html_url']}/releases",
        },
        "downloads": downloads,
        "lastUpdated": (repo.get("pushed_at") or "")[:10] or None,
        "stars": repo.get("stargazers_count"),
        "sources": [repo["full_name"]],
    }

    licence = (repo.get("license") or {}).get("spdx_id")
    if licence and licence not in ("NOASSERTION", "NONE"):
        game["license"] = licence

    return {k: v for k, v in game.items() if v not in (None, [], "", {})}


def _normalise_name(text: str) -> str:
    text = text.lower()
    text = re.sub(r"[®™©:;,'’\-–—!?.]", " ", text)
    return re.sub(r"\s+", " ", text).strip()


def resolve_steam_id(title: str) -> int | None:
    """Steam's public store search. Exact normalised title match only — no fuzzy guessing."""
    url = (
        "https://store.steampowered.com/api/storesearch/?term="
        f"{urllib.parse.quote(title)}&cc=us&l=en"
    )
    try:
        with urllib.request.urlopen(url, timeout=12) as response:
            data = json.loads(response.read().decode("utf-8", "ignore"))
    except Exception:
        return None

    wanted = _normalise_name(title)
    for item in data.get("items", []):
        if _normalise_name(item.get("name", "")) == wanted:
            return int(item["id"])
    return None


def deep_merge(base: dict, extra: dict) -> dict:
    out = dict(base)
    for key, value in extra.items():
        if isinstance(value, dict) and isinstance(out.get(key), dict):
            out[key] = deep_merge(out[key], value)
        else:
            out[key] = value
    return out


def validate(games: list[dict]) -> list[str]:
    problems = []
    seen = set()
    for game in games:
        gid = game.get("id")
        if not gid:
            problems.append("entry without id")
            continue
        if gid in seen:
            problems.append(f"duplicate id: {gid}")
        seen.add(gid)
        if not game.get("title"):
            problems.append(f"{gid}: missing title")
        if not (game.get("links") or {}).get("repo"):
            problems.append(f"{gid}: missing links.repo")
        for download in game.get("downloads", []):
            if not str(download.get("url", "")).startswith("https://"):
                problems.append(f"{gid}: non-https download url")
    return problems


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("repos", help="file with one owner/repo per line")
    parser.add_argument("-o", "--out", default="games.json")
    parser.add_argument("--overrides", default="overrides.json")
    parser.add_argument("--readme", action="store_true", help="scan READMEs for keywords (slower)")
    parser.add_argument("--merge", help="existing games.json to keep curated entries from")
    parser.add_argument(
        "--steam",
        action="store_true",
        help="resolve steamAppId per title so the app can pull official capsule art from Steam's CDN",
    )
    args = parser.parse_args()

    overrides = {}
    if Path(args.overrides).exists():
        overrides = json.loads(Path(args.overrides).read_text())

    names = [
        line.strip()
        for line in Path(args.repos).read_text().splitlines()
        if line.strip() and not line.strip().startswith("#")
    ]

    games, failures = [], []
    for name in names:
        try:
            game = fetch_repo(name, args.readme)
        except Exception as exc:  # keep going, report at the end
            failures.append(f"{name}: {exc}")
            continue
        game = deep_merge(game, overrides.get(name, {}))
        game["sources"] = [name]
        if args.steam and "steamAppId" not in game:
            app_id = resolve_steam_id(game.get("originalTitle") or game["title"])
            if app_id:
                game["steamAppId"] = app_id
        games.append(game)
        print(f"  ok  {name:45s} -> {game['id']}", file=sys.stderr)

    if args.merge and Path(args.merge).exists():
        known = {g.get("sources", [None])[0] for g in games} | {g["id"] for g in games}
        old = json.loads(Path(args.merge).read_text()).get("games", [])
        for game in old:
            source = (game.get("sources") or [None])[0]
            if game["id"] not in known and source not in known:
                games.append(game)
                print(f"  keep {game['id']}", file=sys.stderr)

    problems = validate(games)
    if problems:
        print("\nValidation problems:", file=sys.stderr)
        for problem in problems:
            print(f"  - {problem}", file=sys.stderr)

    catalog = {
        "version": 1,
        "generatedAt": datetime.now(timezone.utc).isoformat(timespec="seconds"),
        "count": len(games),
        "games": sorted(games, key=lambda g: g["title"].lower()),
    }
    Path(args.out).write_text(json.dumps(catalog, indent=2, ensure_ascii=False) + "\n")
    print(f"\nwrote {args.out}: {len(games)} games, {len(problems)} problems, {len(failures)} failures")
    for failure in failures:
        print(f"  ! {failure}", file=sys.stderr)
    return 0 if not failures else 1


if __name__ == "__main__":
    raise SystemExit(main())
