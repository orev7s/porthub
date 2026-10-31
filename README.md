# PortHub

An Android browser for open-source game ports, decompilations and recompilations.
One app, one search box, every port.

- **App:** Kotlin + Jetpack Compose, dark "liquid glass" UI, single-activity, no navigation library.
- **Feed:** one `games.json` on GitHub, fetched at runtime and cached on disk.
- **No game data.** Every entry links to the project that makes the port. ROMs, ISOs and assets are never hosted here.

## Repo layout

```
porthub/
├── app/                     Android app (Compose)
│   └── src/main/assets/seed.json     offline fallback catalog
├── schema/game.schema.json  the contract for one game record
├── tools/build_catalog.py   repos.txt -> games.json via `gh api`
├── overrides.json           hand-curated facts (which game, year, developer)
└── repos.txt                the repos we track
```

## Feed schema (short version)

```json
{
  "version": 1,
  "generatedAt": "2026-10-31T12:00:00Z",
  "games": [
    {
      "id": "ship-of-harkinian",
      "title": "Ship of Harkinian",
      "originalTitle": "The Legend of Zelda: Ocarina of Time",
      "year": 1998,
      "platforms": ["N64"],
      "developer": "Nintendo",
      "portType": "decompilation",
      "status": "stable",
      "engine": "libultraship",
      "description": "…",
      "tags": ["Zelda", "60 fps"],
      "android": {
        "supported": true,
        "minAndroid": "9.0",
        "abis": ["arm64-v8a"],
        "requiresGameFiles": "owned"
      },
      "artwork": { "cover": null, "hero": null, "screenshots": [], "accent": "#7FA34A" },
      "links": { "repo": "https://github.com/…", "releases": "https://github.com/…/releases" },
      "downloads": [
        { "label": "app-arm64-v8a.apk", "kind": "apk", "url": "https://…", "version": "1.2.0", "sizeMb": 42.3, "abi": "arm64-v8a" }
      ],
      "license": "GPL-3.0",
      "lastUpdated": "2026-10-01",
      "stars": 5804
    }
  ]
}
```

`id` is the only field the app truly depends on — it is the deep link and the favourite key.
Everything else degrades gracefully when missing: no cover image means a generated
gradient tile using `artwork.accent`.

Full definition: [`schema/game.schema.json`](schema/game.schema.json).

## Building the feed

```bash
# repos.txt: one owner/repo per line
python3 tools/build_catalog.py repos.txt --readme -o games.json
```

`gh` must be authenticated (`gh auth login`). The script pulls repo metadata, licence,
stars, last push and the latest release's `.apk` assets, infers `portType`/`status` from
the description + README, then deep-merges hand-written facts from `overrides.json`:

```json
{
  "HarbourMasters/Shipwright": {
    "title": "Ship of Harkinian",
    "originalTitle": "The Legend of Zelda: Ocarina of Time",
    "year": 1998,
    "developer": "Nintendo",
    "platforms": ["N64"],
    "portType": "decompilation"
  }
}
```

Use `--merge games.json` to keep curated entries for repos you did not refetch.

## Building the app

```bash
./gradlew assembleDebug          # app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease        # signed with the debug key until a real one exists
```

The feed URL is editable at runtime in **Settings → Catalog feed**, so a fork can point
at its own catalog without a rebuild. Default:

```
https://raw.githubusercontent.com/orev7s/porthub-catalog/main/games.json
```

That feed currently carries **1102 ports** built from six upstream directories:

| Source | Contributes |
|---|---|
| [openportsgames](https://github.com/Kymie131/openportsgames) | year, publisher, genre, summary, covers (CC BY 4.0) |
| [droid-recomp](https://github.com/MooPuu/droid-recomp) | Android packages, min Android, ABIs, APK links |
| [Ports Launcher](https://github.com/Nyaldee/Ports-Launcher) | Android package ids, SteamGridDB art, install notes |
| [awesome-android-native-ports](https://github.com/abduznik/awesome-android-native-ports) | breadth |
| [awesome-x360-recomps](https://github.com/abduznik/awesome-x360-recomps) | Xbox 360 recompilations |
| [awesome-rea](https://github.com/yibie/awesome-rea) | decompilation research |

```bash
python3 tools/import_sources.py --src feedsrc -o games.json   # rebuild the feed
```

## Legal

PortHub indexes projects, it does not distribute games. Many ports legally require the
user to own the original title and build assets from their own copy — the app states this
on every detail screen (`android.requiresGameFiles`). Takedown requests go to the repo
that hosts the port, not to this catalog.

## Licence

GPL-3.0 (planned).
