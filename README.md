# reinodoce-mc-tiktok

**Client-side** Forge mod for Minecraft Java that mirrors chat events
from a TikTok LIVE inside the local Minecraft chat.

- Works in singleplayer and multiplayer.
- No plugin, mod, or datapack required on the server.
- Uses a 15-second TikTok LIVE HTTP timeout before reconnecting, which
  avoids short transient connect stalls on slower routes.
- Every mirrored line is rendered with a visible `[LIVE]` prefix so the
  external origin stays identifiable. See
  [docs/threading.md](docs/threading.md).
- Mirrored TikTok lines are hidden from Minecraft's chat log by default
  to avoid flooding `latest.log`; enable `/reinodoce settings chat-log true`
  if you want them logged.
- Optional session logs can write accepted LIVE events to
  `logs/reinodoce/` for after-stream review or diagnostics; enable them
  with `/reinodoce logging enabled true`.
- Optional local alerts can play sounds or show toasts for surfaced
  gifts, follows, joins, and member-level events; enable them with
  `/reinodoce alert ...`.
- Mirrored output can be sent to chat, actionbar, a bounded local HUD, or
  suppressed while keeping the connection active with
  `/reinodoce settings output ...`.
- Language defaults to the Minecraft client language, with an optional
  override through `/reinodoce settings language <locale|auto>`.
- This branch (`1.20.1`) targets Minecraft `1.20.1`, Forge `47.4.16`,
  Java `17`.

```
/reinodoce connect @yourusername
/reinodoce settings gui
/reinodoce settings auto-connect true
/reinodoce settings language auto
/reinodoce rule block-word add spam
/reinodoce logging enabled true
/reinodoce stats
```

For the full command surface, see [docs/commands.md](docs/commands.md).

## Install (release version)

1. Download the latest release from Modrinth or the GitHub Releases
   page. The main runtime artifacts are:
   - `reinodoce-mc-tiktok-<mc>-<v>.jar` — the bare mod jar.
   - `reinodoce-mc-tiktok-<mc>-<v>.mrpack` — a Modrinth pack import for
     creating a client instance with Forge and this client-side mod.
   - `reinodoce-mc-tiktok-<mc>-<v>-packwiz.zip` — the same jar plus
     Packwiz / Prism metadata that marks the mod as client-only in
     Prism's mod list.
   - `SHA256SUMS.txt` / `SHA512SUMS.txt` — checksums for the jar,
     `.mrpack`, and Packwiz zip on GitHub Releases.
2. Install one of them into your Forge instance:

   **Vanilla Forge / CurseForge / ATLauncher / manual install** —
   copy the `.jar` into the instance's `mods/` folder.

   **Prism Launcher, existing instance** — use the bare `.jar` with the
   Mods tab's **Add File** button. Do not add `.mrpack` or `-packwiz.zip`
   there; Prism copies those archives into `mods/` as files.

   **Prism Launcher, new instance** — import the `.mrpack`. The pack puts
   the mod jar under `mods/` and marks it client-only in the Modrinth
   pack metadata, so it can join servers that do not have it.

   Optional for an existing Prism instance: to make Prism's Side column
   show `client`, close Prism and extract `-packwiz.zip` into the
   instance's Minecraft root, so that both the `.jar` and
   `mods/.index/*.pw.toml` land under `mods/`. See
   [docs/packwiz-bundle.md](docs/packwiz-bundle.md).
3. Start Minecraft with the matching Forge profile.
4. In game, run `/reinodoce connect @username`.

Optional: verify the downloaded artifacts with the published
checksum files:

```powershell
# Windows
$checksums = Get-Content .\SHA256SUMS.txt
foreach ($line in $checksums) {
  $hash, $file = $line -split '\s+', 2
  $actual = (Get-FileHash ".\$file" -Algorithm SHA256).Hash.ToLowerInvariant()
  if ($actual -ne $hash) { throw "Checksum mismatch: $file" }
}
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

This produces the bare jar, the Packwiz bundle under
`build/prism-bundle/`, and the `.mrpack` under `build/distributions/`.
The build also verifies the provisioned
`TikTokLiveJava` jar against the SHA-512 recorded in `gradle.properties`.

For deeper development topics — repo layout, the auto-provisioned
`TikTokLiveJava` jar, the lint suite, etc. — see
[docs/development.md](docs/development.md).

## Documentation

| Topic                                                | Where                                                  |
| ---------------------------------------------------- | ------------------------------------------------------ |
| `/reinodoce` command reference                       | [docs/commands.md](docs/commands.md)                   |
| Client config file fields, defaults, validation      | [docs/configuration.md](docs/configuration.md)         |
| Packwiz and `.mrpack` bundles                        | [docs/packwiz-bundle.md](docs/packwiz-bundle.md)       |
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
