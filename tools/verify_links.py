#!/usr/bin/env python3
"""
Verify and repair every link in games.json.

    python3 tools/verify_links.py games.json -o games.json

What it does, per game:
  1. checks the repo actually exists (GitHub API, authenticated)
  2. pulls the latest release and its real .apk assets -> downloads[]
  3. throws away third-party mirror pages (apkvision, droidports…) that ship the wrong game
  4. drops games where nothing verifiable is left

Cached in /tmp/porthub-verify-cache.json so a rerun costs nothing.
"""

from __future__ import annotations

import argparse
import json
import os
import subprocess
import sys
import threading
import time
import urllib.error
import urllib.request
from concurrent.futures import ThreadPoolExecutor
from datetime import datetime, timezone
from pathlib import Path

CACHE_PATH = Path("/tmp/porthub-verify-cache.json")
LOCK = threading.Lock()

# Pages that pretend to be a download but routinely serve the wrong game.
MIRROR_HOSTS = ("apkvision.org", "droidports.com", "apkpure.com", "apkmirror.com", "apkfab.com", "modapk")


def token() -> str:
    out = subprocess.run(["gh", "auth", "token"], capture_output=True, text=True)
    return out.stdout.strip()


TOKEN = token() if not os.environ.get("GITHUB_TOKEN") else os.environ["GITHUB_TOKEN"]


def api(path: str, cache: dict, attempts: int = 3):
    """GET api.github.com with cache + backoff. Returns (status, payload)."""
    if path in cache:
        return cache[path]

    url = f"https://api.github.com{path}"
    for attempt in range(attempts):
        request = urllib.request.Request(
            url,
            headers={
                "Authorization": f"Bearer {TOKEN}",
                "Accept": "application/vnd.github+json",
                "User-Agent": "PortHub-verify",
            },
        )
        try:
            with urllib.request.urlopen(request, timeout=20) as response:
                payload = json.loads(response.read().decode("utf-8", "ignore"))
                result = (response.status, payload)
                with LOCK:
                    cache[path] = result
                return result
        except urllib.error.HTTPError as error:
            if error.code in (403, 429) and attempt < attempts - 1:
                time.sleep(2 * (attempt + 1))       # secondary rate limit
                continue
            result = (error.code, None)
            with LOCK:
                cache[path] = result
            return result
        except Exception:
            if attempt < attempts - 1:
                time.sleep(1)
                continue
            return (0, None)
    return (0, None)


def head_ok(url: str) -> bool:
    try:
        request = urllib.request.Request(url, method="HEAD", headers={"User-Agent": "PortHub-verify"})
        with urllib.request.urlopen(request, timeout=15) as response:
            return 200 <= response.status < 400
    except urllib.error.HTTPError as error:
        return error.code not in (404, 410)
    except Exception:
        return False


def pretty_abi(name: str) -> str | None:
    lowered = name.lower()
    for abi, markers in (
        ("arm64-v8a", ("arm64", "aarch64")),
        ("armeabi-v7a", ("armeabi", "armv7", "arm32")),
        ("x86_64", ("x86_64", "x64")),
        ("universal", ("universal", "all")),
    ):
        if any(marker in lowered for marker in markers):
            return abi
    return None


def check_game(game: dict, cache: dict) -> dict | None:
    links = dict(game.get("links") or {})
    repo_url = links.get("repo") or ""
    downloads: list[dict] = []
    verified = {"at": datetime.now(timezone.utc).isoformat(timespec="seconds"), "repoAlive": False, "apk": False}

    if "github.com/" in repo_url:
        parts = repo_url.split("github.com/")[-1].strip("/").split("/")
        if len(parts) >= 2:
            full_name = f"{parts[0]}/{parts[1]}"
            status, repo = api(f"/repos/{full_name}", cache)

            if status == 200 and repo:
                verified["repoAlive"] = True
                links["repo"] = repo["html_url"]
                verified["stars"] = repo.get("stargazers_count")
                verified["pushedAt"] = (repo.get("pushed_at") or "")[:10]
                if repo.get("archived"):
                    verified["archived"] = True

                rstatus, release = api(f"/repos/{full_name}/releases/latest", cache)
                if rstatus == 200 and release:
                    verified["releaseTag"] = release.get("tag_name")
                    links["releases"] = release.get("html_url") or f"{repo['html_url']}/releases"
                    for asset in release.get("assets", []):
                        name = asset.get("name", "")
                        if not name.lower().endswith(".apk"):
                            continue
                        downloads.append(
                            {
                                "label": name,
                                "kind": "apk",
                                "url": asset["browser_download_url"],
                                "version": release.get("tag_name"),
                                "sizeMb": round(asset.get("size", 0) / 1_048_576, 1) or None,
                                "abi": pretty_abi(name),
                            }
                        )
                elif rstatus in (200, 404):
                    links.pop("releases", None)      # no published release: don't invent one
                    verified["releaseTag"] = None
            else:
                # Repo is gone (or private). Nothing on GitHub can be trusted.
                links.pop("repo", None)
                links.pop("releases", None)
                verified["repoAlive"] = False
                verified["repoStatus"] = status

    # Drop mirror pages posing as downloads; keep their URL as a website reference at most.
    kept_downloads = []
    for download in game.get("downloads", []):
        url = download.get("url", "")
        if any(host in url for host in MIRROR_HOSTS):
            if not links.get("website"):
                links["website"] = url
            continue
        if url.lower().endswith(".apk"):
            kept_downloads.append(download)

    downloads.extend(kept_downloads)
    verified["apk"] = bool(downloads)

    game["links"] = links
    game["downloads"] = downloads
    game["verified"] = verified
    if downloads or links.get("repo") or links.get("website"):
        return game
    return None


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("source")
    parser.add_argument("-o", "--out", required=True)
    parser.add_argument("--workers", type=int, default=8)
    args = parser.parse_args()

    catalog = json.loads(Path(args.source).read_text())
    games = catalog["games"]

    cache: dict = {}
    if CACHE_PATH.exists():
        try:
            cache = json.loads(CACHE_PATH.read_text())
        except Exception:
            cache = {}

    print(f"verifying {len(games)} games (cache: {len(cache)} entries)", file=sys.stderr)
    started = time.time()

    alive, dead, with_apk = 0, 0, 0
    kept: list[dict] = []

    with ThreadPoolExecutor(max_workers=args.workers) as pool:
        for index, result in enumerate(pool.map(lambda g: check_game(g, cache), games), 1):
            if result is None:
                dead += 1
            else:
                kept.append(result)
                if result["verified"].get("repoAlive"):
                    alive += 1
                if result["downloads"]:
                    with_apk += 1
            if index % 100 == 0:
                print(f"  {index}/{len(games)}  kept={len(kept)} apk={with_apk} dropped={dead}", file=sys.stderr)

    CACHE_PATH.write_text(json.dumps(cache))

    catalog["games"] = kept
    catalog["count"] = len(kept)
    catalog["verifiedAt"] = datetime.now(timezone.utc).isoformat(timespec="seconds")
    Path(args.out).write_text(json.dumps(catalog, indent=2, ensure_ascii=False) + "\n")

    print(
        f"\nkept {len(kept)}  |  repo alive {alive}  |  real apk {with_apk}  |  dropped {dead}"
        f"  |  {time.time() - started:.0f}s"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
