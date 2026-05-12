# reinodoce-mc-tiktok

**Client-side** Forge mod for Minecraft Java that mirrors chat events
from a TikTok LIVE inside the local Minecraft chat.

- Works in singleplayer and multiplayer.
- No plugin, mod, or datapack required on the server.
- Every mirrored line is rendered with a visible `[LIVE]` prefix so the
  external origin stays identifiable. See
  [docs/threading.md](docs/threading.md).
- This branch (`1.20.1`) targets Minecraft `1.20.1`, Forge `47.4.16`,
  Java `17`.

```
/reinodoce connect @yourusername
```

For the full command surface, see [docs/commands.md](docs/commands.md).

## Install (release version)

1. Download the latest release from the GitHub Releases page. Two
   artifacts are published per release:
   - `reinodoce-mc-tiktok-<mc>-<v>.jar` — the bare mod jar.
   - `reinodoce-mc-tiktok-<mc>-<v>-packwiz.zip` — the same jar plus
     Packwiz metadata that marks the mod as client-only.
2. Install one of them into your Forge instance:

   **Vanilla Forge / CurseForge / ATLauncher / manual install** —
   copy the `.jar` into the instance's `mods/` folder.

   **Prism Launcher** (or any Packwiz-aware launcher) — extract the
   `-packwiz.zip` directly into the instance, so that both the `.jar`
   and the `mods/.index/*.pw.toml` file land in `mods/`. The launcher
   will then show the mod's Side column as `client`. See
   [docs/packwiz-bundle.md](docs/packwiz-bundle.md).
3. Start Minecraft with the matching Forge profile.
4. In game, run `/reinodoce connect @username`.

Optional — verify the download with the published checksum files:

```powershell
# Windows
Get-FileHash .\reinodoce-mc-tiktok-*.jar -Algorithm SHA256
```

```bash
# Linux / macOS
sha256sum -c SHA256SUMS.txt
sha512sum -c SHA512SUMS.txt
```

## Build locally

Requirement: JDK 17 (`JAVA_HOME` pointing at it). The Gradle wrapper jar
is tracked, and `gradle/wrapper/gradle-wrapper.properties` pins the
Gradle distribution checksum, so no system Gradle bootstrap step is
required after cloning.

Then build:

```powershell
.\gradlew.bat build
```

```bash
./gradlew build
```

The mod jar lands in `build/libs/`. Drop it into your Forge instance's
`mods/` folder to test, or run the full release-equivalent build:

```powershell
.\gradlew.bat clean build prismBundle verifyPrismMetadata
```

This produces both the bare jar and the Packwiz bundle under
`build/prism-bundle/`. The build also verifies the provisioned
`TikTokLiveJava` jar against the SHA-512 recorded in `gradle.properties`.

For deeper development topics — repo layout, the auto-provisioned
`TikTokLiveJava` jar, the lint suite, etc. — see
[docs/development.md](docs/development.md).

## Documentation

| Topic                                                | Where                                                  |
| ---------------------------------------------------- | ------------------------------------------------------ |
| `/reinodoce` command reference                       | [docs/commands.md](docs/commands.md)                   |
| Client config file fields, defaults, validation      | [docs/configuration.md](docs/configuration.md)         |
| Packwiz bundle (client-only distribution)            | [docs/packwiz-bundle.md](docs/packwiz-bundle.md)       |
| Release automation, tag format, checksum verify      | [docs/releases.md](docs/releases.md)                   |
| Branch strategy across Minecraft versions            | [docs/branches.md](docs/branches.md)                   |
| Internationalization (locale list, adding a locale)  | [docs/internationalization.md](docs/internationalization.md) |
| Threading, robustness, and the `[LIVE]` prefix       | [docs/threading.md](docs/threading.md)                 |
| Troubleshooting and known limitations                | [docs/troubleshooting.md](docs/troubleshooting.md)     |
| Development (layout, deps, lint, verification)       | [docs/development.md](docs/development.md)             |
| Changelog                                            | [CHANGELOG.md](CHANGELOG.md)                           |
| Repository contracts for AI / human contributors     | [AGENTS.md](AGENTS.md)                                 |

## License

Distributed under the **GNU Lesser General Public License v3.0 or later**
(LGPL-3.0-or-later). See [LICENSE](LICENSE) for the full text. The same
SPDX identifier is declared as `mod_license` in `gradle.properties` and
exposed to Forge via `mods.toml`.
