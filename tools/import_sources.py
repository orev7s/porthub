#!/usr/bin/env python3
"""
Build games.json from the upstream directories we track.

    python3 tools/import_sources.py --src feedsrc -o games.json

Sources (all cloned under --src):
  Kymie131_openportsgames     2616 TS records  (CC BY 4.0)  - richest metadata
  MooPuu_droid-recomp         catalog.json 379 apps         - Android specifics
  Nyaldee_Ports-Launcher      ports.json 561 ports          - Android packages + art
  abduznik/awesome-android-native-ports   README tables     - breadth
  abduznik/awesome-x360-recomps           README tables     - x360 recompilations
  yibie/awesome-rea                       README tables     - RE research

Nothing is copied wholesale: each game becomes one record pointing at its project.
Multiple implementations of the same game stay as separate records; duplicates of the
same implementation are merged, richest field wins.
"""

from __future__ import annotations

import argparse
import json
import re
import unicodedata
import urllib.parse
from datetime import datetime, timezone
from pathlib import Path

# ----------------------------------------------------------------------------- helpers

ACCENTS = ["#6C7BFF", "#FF6B9A", "#4ADEDE", "#FFB86B", "#B08CFF", "#57D9A3", "#7FA34A", "#B4342C"]


def slug(text: str) -> str:
    text = unicodedata.normalize("NFKD", text).encode("ascii", "ignore").decode()
    text = text.lower().replace("&", " and ")
    text = re.sub(r"[^a-z0-9]+", "-", text)
    return re.sub(r"-{2,}", "-", text).strip("-")[:80]


def norm_key(text: str) -> str:
    return re.sub(r"[^a-z0-9]", "", text.lower())


def accent_for(key: str) -> str:
    return ACCENTS[sum(ord(c) for c in key) % len(ACCENTS)]


def clean(text: str | None, limit: int = 400) -> str | None:
    if not text:
        return None
    text = re.sub(r"\s+", " ", text).strip()
    return text[:limit]


def map_port_type(raw: str | None, text: str = "") -> str:
    value = f"{raw or ''} {text}".lower()
    if "recomp" in value:
        return "recompilation"
    if "decomp" in value:
        return "decompilation"
    if "engine" in value or "reimplement" in value:
        return "engine"
    if "source" in value or "port" in value:
        return "source-port"
    return "unknown"


def map_status(raw: str | None) -> str:
    value = (raw or "").lower()
    if any(word in value for word in ("verified", "stable", "playable", "released", "complete")):
        return "stable"
    if "beta" in value:
        return "beta"
    if "alpha" in value:
        return "alpha"
    if any(word in value for word in ("wip", "progress", "experimental", "prototype", "early")):
        return "wip"
    if any(word in value for word in ("broken", "archived", "dead")):
        return "archived"
    return "unknown"


def needs_game_files(text: str) -> str:
    value = text.lower()
    if any(p in value for p in ("bring your own", "requires the original", "game files", "original data", "you must own")):
        return "owned"
    if any(p in value for p in ("standalone", "no game data", "no assets")):
        return "none"
    return "unknown"


def abis_from(text: str) -> list[str]:
    found = []
    for abi, pattern in (("arm64-v8a", r"arm64|aarch64"), ("armeabi-v7a", r"armv7|armeabi|arm32"), ("x86_64", r"x86_64|x64")):
        if re.search(pattern, text, re.I):
            found.append(abi)
    return found


def markdown_rows(path: Path):
    """Yield (title, url, description, author) from every table in a README."""
    if not path.exists():
        return
    for line in path.read_text(encoding="utf-8", errors="ignore").splitlines():
        line = line.strip()
        if not line.startswith("|") or "](" not in line:
            continue
        cells = [c.strip() for c in line.strip("|").split("|")]
        if len(cells) < 2:
            continue
        first = cells[0]
        match = re.match(r"\[(.+?)\]\((.+?)\)", first)
        if not match:
            continue
        title, url = match.group(1).strip(), match.group(2).strip()
        if url.startswith("#") or not url.startswith("http"):
            continue
        desc = cells[1] if len(cells) > 1 else ""
        author = ""
        if len(cells) > 2:
            author_match = re.match(r"\[(.+?)\]", cells[2])
            author = author_match.group(1) if author_match else cells[2]
        yield title, url, desc, author


def repo_url(url) -> str | None:
    """Accepts a plain URL or the {type, url} objects some catalogs use."""
    if isinstance(url, dict):
        url = url.get("url") or url.get("href") or url.get("source") or ""
    if not isinstance(url, str) or not url.startswith("http"):
        return None
    url = url.split("?")[0].rstrip("/")
    if url.endswith(".git"):
        url = url[:-4]
    return url


# ----------------------------------------------------------------------------- sources

def parse_openportsgames(src: Path) -> list[dict]:
    out = []
    root = src / "Kymie131_openportsgames" / "src" / "content" / "ports"
    if not root.exists():
        return out

    for file in root.glob("*.ts"):
        text = file.read_text(encoding="utf-8", errors="ignore")

        def grab(key: str, pattern: str = r'"([^"]*)"') -> str | None:
            found = re.search(rf"{key}:\s*{pattern}", text)
            return found.group(1) if found else None

        def grab_array(key: str) -> list[str]:
            found = re.search(rf"{key}:\s*\[([^\]]*)\]", text)
            return re.findall(r'"([^"]*)"', found.group(1)) if found else []

        platforms = [p.lower() for p in grab_array("platforms")]
        sources = grab_array("sources")
        summary = grab("summary", r'"(.+?)",\s*\n')
        title = grab("title") or grab("game")
        if not title:
            continue

        cover = re.search(r"cover:\s*\{[^}]*?src:\s*\"([^\"]+)\"", text, re.S)
        licence = re.search(r"spdx:\s*\"([^\"]+)\"", text)

        out.append(
            {
                "title": title,
                "originalTitle": grab("game"),
                "year": int(grab("originalYear", r"(\d{4})")) if grab("originalYear", r"(\d{4})") else None,
                "publisher": grab("publisher"),
                "developers": grab_array("developers"),
                "portType": map_port_type(grab("portType"), title),
                "status": grab("status"),
                "summary": summary,
                "platforms": grab_array("originalSystems"),
                "repo": repo_url(sources[0]) if sources else None,
                "cover": cover.group(1) if cover else None,
                "license": licence.group(1) if licence and licence.group(1) != "NOASSERTION" else None,
                "android": "android" in platforms,
                "origin": "openportsgames",
            }
        )
    return out


def parse_droid_recomp(src: Path) -> list[dict]:
    path = src / "MooPuu_droid-recomp" / "catalog.json"
    if not path.exists():
        return []
    data = json.loads(path.read_text(encoding="utf-8", errors="ignore"))
    out = []
    for app in data.get("apps", []):
        blob = " ".join(str(app.get(k, "")) for k in ("description", "needs", "blurb"))
        out.append(
            {
                "title": app.get("name") or app.get("game"),
                "originalTitle": app.get("game"),
                "year": None,
                "publisher": None,
                "developers": [app["author"]] if app.get("author") else [],
                "portType": map_port_type(app.get("type"), blob),
                "status": app.get("status"),
                "summary": app.get("description") or app.get("blurb"),
                "platforms": [app.get("origin")] if app.get("origin") else [],
                "repo": repo_url(app["repo_url"]) if app.get("repo_url") else None,
                "cover": app.get("cover"),
                "hero": app.get("hero"),
                "license": None,
                "android": True,
                "minAndroid": (app.get("min_android") or "").replace("Android", "").replace("+", "").strip() or None,
                "abis": [a for a in app.get("architectures", []) if a in ("arm64-v8a", "armeabi-v7a", "x86_64")],
                "requires": needs_game_files(blob),
                "requiredFiles": (app.get("needs") or "").strip() or None,
                "downloads": [{"label": "Latest APK", "kind": "apk", "url": app["apk_url"]}] if app.get("apk_url") else [],
                "version": app.get("version"),
                "stars": app.get("stars"),
                "origin": "droid-recomp",
            }
        )
    return out


def parse_ports_launcher(src: Path) -> list[dict]:
    path = src / "Nyaldee_Ports-Launcher" / "ports.json"
    if not path.exists():
        return []
    data = json.loads(path.read_text(encoding="utf-8", errors="ignore"))
    out = []
    for port in data.get("ports", []):
        name = re.sub(r"\s*\([^)]*\)\s*$", "", port.get("name", "")).strip()
        if not name:
            continue
        tags = port.get("tags", [])
        out.append(
            {
                "title": name,
                "originalTitle": None,
                "year": None,
                "publisher": None,
                "developers": [],
                "portType": map_port_type(None, " ".join(tags) + " " + port.get("instructions", "")),
                "status": None,
                "summary": clean(port.get("instructions"), 240),
                "platforms": [t for t in tags if t.isupper() or t in ("ps3", "xbox360", "n64", "switch", "wii", "gc", "ps2", "psp", "pc")],
                "repo": repo_url(port["source"]) if port.get("source") else None,
                "cover": port.get("image"),
                "license": None,
                "android": True,
                "requires": needs_game_files(port.get("instructions", "")),
                "requiredFiles": _required_files(port.get("instructions", "")),
                "androidPackage": port.get("android_package"),
                "origin": "ports-launcher",
            }
        )
    return out


def parse_awesome(src: Path, folder: str, default_type: str, origin: str) -> list[dict]:
    out = []
    for title, url, desc, author in markdown_rows(src / folder / "README.md"):
        out.append(
            {
                "title": title,
                "originalTitle": None,
                "year": None,
                "publisher": None,
                "developers": [author] if author else [],
                "portType": map_port_type(None, f"{default_type} {desc}"),
                "status": None,
                "summary": desc,
                "platforms": [],
                "repo": repo_url(url) if "github.com" in url else None,
                "website": None if "github.com" in url else url,
                "cover": None,
                "license": None,
                "android": origin == "awesome-android-ports",
                "requires": needs_game_files(desc),
                "origin": origin,
            }
        )
    return out


# ----------------------------------------------------------------------------- setup

# Engines people have to install next to the port. Only well-known projects, so the
# link we show is the real upstream and not a guess.
ENGINES = [
    ("xash3d", {"name": "Xash3D FWGS", "url": "https://github.com/FWGS/xash3d-fwgs",
                "note": "GoldSrc engine — runs Half-Life and its mods."}),
    ("devilutionx", {"name": "DevilutionX", "url": "https://github.com/diasurgical/DevilutionX",
                     "note": "Diablo engine."}),
    ("scummvm", {"name": "ScummVM", "url": "https://www.scummvm.org/downloads/",
                 "note": "Point-and-click adventure engine."}),
    ("openmw", {"name": "OpenMW", "url": "https://github.com/OpenMW/openmw",
                "note": "Morrowind engine."}),
    ("fheroes2", {"name": "fheroes2", "url": "https://github.com/ihhub/fheroes2",
                  "note": "Heroes of Might and Magic II engine."}),
    ("vcmi", {"name": "VCMI", "url": "https://github.com/vcmi/vcmi",
              "note": "Heroes of Might and Magic III engine."}),
    ("openrct2", {"name": "OpenRCT2", "url": "https://github.com/OpenRCT2/OpenRCT2",
                  "note": "RollerCoaster Tycoon 2 engine."}),
    ("openra", {"name": "OpenRA", "url": "https://github.com/OpenRA/OpenRA",
                "note": "Command & Conquer engine."}),
    ("diii4a", {"name": "idTech4A++", "url": "https://github.com/glKarin/com.n0n3m4.diii4a",
                "note": "idTech 4 engine (Doom 3, Quake 4, Prey)."}),
    ("gzdoom", {"name": "GZDoom", "url": "https://github.com/ZDoom/gzdoom",
                "note": "Doom engine."}),
    ("openlara", {"name": "OpenLara", "url": "https://github.com/XProger/OpenLara",
                  "note": "Tomb Raider engine."}),
    ("shipwright", {"name": "Ship of Harkinian", "url": "https://github.com/HarbourMasters/Shipwright",
                    "note": "Ocarina of Time decompilation."}),
    ("sm64ex", {"name": "sm64ex", "url": "https://github.com/sm64pc/sm64ex",
                "note": "Super Mario 64 decompilation."}),
]


def _required_files(instructions: str) -> str | None:
    if not instructions:
        return None
    match = re.search(r"Required files:\s*(.+?)(?:\n\n|$)", instructions, re.S)
    if match:
        return clean(match.group(1), 220)
    return None


def build_setup(record: dict) -> dict:
    """Turn scattered source metadata into one consistent, honest install guide."""
    blob = " ".join(
        str(record.get(f) or "") for f in ("repo", "title", "summary", "originalTitle")
    ).lower()

    engine = next((data for key, data in ENGINES if key in blob), None)
    files_text = record.get("requiredFiles")
    package = record.get("androidPackage")
    level = record.get("requires") or "unknown"
    if files_text and level == "unknown":
        level = "owned"          # the source literally lists the files you must supply
    must_own = record.get("originalTitle") or record.get("title")
    platform = (record.get("platforms") or [None])[0]
    if platform and len(platform) <= 6:
        platform = platform.upper().replace("N64", "N64")

    steps: list[str] = []

    if level == "none":
        steps.append("Download the APK from the project's releases page.")
        steps.append("Install and open it — this build needs no game files.")
        difficulty, minutes = "easy", 5
    else:
        own_line = f"Own a legal copy of {must_own}"
        if platform:
            own_line += f" ({platform})"
        own_line += " — buy it, rip your disc, or use files you already own. The port ships no game data."
        steps.append(own_line)

        if engine:
            steps.append(f"Get {engine['name']} if the port does not bundle it.")

        steps.append("Download the port's APK from its releases page and install it.")

        if files_text:
            steps.append(f"Prepare these files: {files_text}")

        if package:
            steps.append(f"Copy them into Android/data/{package}/files — create the folders if they don't exist.")
        else:
            steps.append("Copy them into the folder the app asks for on first launch (usually Android/data/<package>/files).")

        steps.append("Open the app, point it at that folder if prompted, and play.")

        difficulty = "advanced" if engine else "medium"
        minutes = 20 if engine else 12

    buy = []
    if level != "none" and must_own:
        query = urllib.parse.quote(must_own)
        buy = [
            {"store": "Steam", "url": f"https://store.steampowered.com/search/?term={query}"},
            {"store": "GOG", "url": f"https://www.gog.com/en/games?query={query}"},
        ]

    setup = {
        "difficulty": difficulty,
        "minutes": minutes,
        "gameFiles": level,
        "mustOwn": must_own if level != "none" else None,
        "requiredFiles": files_text,
        "installPath": f"Android/data/{package}/files" if package else None,
        "steps": steps,
        "confidence": "exact" if files_text else "generic",
    }
    if engine:
        setup["engine"] = engine
    if buy:
        setup["buy"] = buy
    return {k: v for k, v in setup.items() if v not in (None, [], "", {})}


# ----------------------------------------------------------------------------- merge

PRIORITY = {"openportsgames": 0, "droid-recomp": 1, "ports-launcher": 2, "awesome-android-ports": 3, "awesome-x360": 4, "awesome-rea": 5}

SCALAR_FIELDS = (
    "originalTitle", "year", "publisher", "cover", "hero", "license", "summary",
    "minAndroid", "requires", "requiredFiles", "androidPackage",
)


def merge(records: list[dict]) -> list[dict]:
    buckets: dict[str, list[dict]] = {}
    for record in records:
        key = record.get("repo") or f"title:{norm_key(record['title'])}"
        buckets.setdefault(key.lower(), []).append(record)

    merged = []
    for key, group in buckets.items():
        group.sort(key=lambda r: PRIORITY.get(r.get("origin", ""), 9))
        base = dict(group[0])

        for other in group[1:]:
            for field in SCALAR_FIELDS:
                if not base.get(field) and other.get(field):
                    base[field] = other[field]
            base["android"] = base.get("android") or other.get("android")
            for field in ("abis", "downloads"):
                if other.get(field) and not base.get(field):
                    base[field] = other[field]
            if not base.get("developers"):
                base["developers"] = other.get("developers")
            if not base.get("platforms"):
                base["platforms"] = other.get("platforms")
            if not base.get("version") and other.get("version"):
                base["version"] = other["version"]
            if not base.get("stars") and other.get("stars"):
                base["stars"] = other["stars"]
        base["sources"] = sorted({r["origin"] for r in group})
        merged.append(base)
    return merged


def to_game(record: dict) -> dict:
    title = (record.get("title") or "").strip() or "Untitled port"
    game_id = slug(title) or "port"
    repo = record.get("repo")

    game = {
        "id": game_id,
        "title": title,
        "originalTitle": record.get("originalTitle") or None,
        "year": record.get("year"),
        "platforms": [p for p in (record.get("platforms") or []) if p],
        "developer": record.get("publisher") or (record.get("developers") or [None])[0],
        "portType": record.get("portType") or "unknown",
        "status": map_status(record.get("status")),
        "description": clean(record.get("summary"), 400) or "",
        "tags": [t for t in (record.get("platforms") or [])][:4],
        "android": {
            "supported": bool(record.get("android")),
            "minAndroid": record.get("minAndroid"),
            "abis": record.get("abis") or [],
            "requiresGameFiles": record.get("requires") or "unknown",
        },
        "artwork": {"accent": accent_for(game_id)},
        "links": {},
        "downloads": record.get("downloads") or [],
        "license": record.get("license"),
        "lastUpdated": None,
        "stars": record.get("stars"),
        "sources": record.get("sources") or [],
        "setup": build_setup(record),
    }

    if record.get("cover"):
        game["artwork"]["cover"] = record["cover"]
    if record.get("hero"):
        game["artwork"]["hero"] = record["hero"]
    if repo:
        game["links"]["repo"] = repo
        game["links"]["releases"] = f"{repo}/releases"
    if record.get("website"):
        game["links"]["website"] = record["website"]

    return {k: v for k, v in game.items() if v not in (None, [], "", {})}


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--src", default="feedsrc")
    parser.add_argument("-o", "--out", default="games.json")
    parser.add_argument("--include-non-android", action="store_true")
    args = parser.parse_args()

    src = Path(args.src)
    records: list[dict] = []
    records += parse_openportsgames(src)
    records += parse_droid_recomp(src)
    records += parse_ports_launcher(src)
    records += parse_awesome(src, "abduznik_awesome-android-native-ports", "source port", "awesome-android-ports")
    records += parse_awesome(src, "abduznik_awesome-x360-recomps", "recompilation", "awesome-x360")
    records += parse_awesome(src, "yibie_awesome-rea", "decompilation", "awesome-rea")

    print(f"parsed {len(records)} raw records")
    games = merge(records)
    print(f"merged into {len(games)} implementations")

    kept = [
        g
        for g in games
        if (g.get("android") or args.include_non_android)
        and (g.get("repo") or g.get("downloads") or g.get("website"))
    ]
    print(f"android-capable: {len(kept)}")

    seen: set[str] = set()
    final = []
    for record in sorted(kept, key=lambda r: r["title"].lower()):
        game = to_game(record)
        base_id, n = game["id"], 2
        while game["id"] in seen:  # ids must be stable + unique
            game["id"] = f"{base_id}-{n}"
            n += 1
        seen.add(game["id"])
        final.append(game)

    catalog = {
        "version": 1,
        "generatedAt": datetime.now(timezone.utc).isoformat(timespec="seconds"),
        "count": len(final),
        "games": final,
    }
    Path(args.out).write_text(json.dumps(catalog, indent=2, ensure_ascii=False) + "\n")

    by_type: dict[str, int] = {}
    for game in final:
        by_type[game["portType"]] = by_type.get(game["portType"], 0) + 1
    print(f"wrote {args.out}: {len(final)} games")
    print("by type:", dict(sorted(by_type.items(), key=lambda kv: -kv[1])))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
