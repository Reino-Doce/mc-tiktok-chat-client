# reinodoce-mc-tiktok

**Client-side only** Forge mod for Minecraft Java that mirrors chat events
from a TikTok LIVE inside the Minecraft chat.

## Overview

- Project: client-side Forge mod, no server dependency.
- Works in singleplayer and multiplayer.
- Does not require a plugin, mod, or datapack on the server.
- Default chat prefix: `[LIVE]`.
- Client-side commands:
  - `/reinodoce connect @username`
  - `/reinodoce disconnect`
  - `/reinodoce status`
  - `/reinodoce settings reconnect <seconds>`
  - `/reinodoce settings chat-emotes <true|false>`
  - `/reinodoce rule follower <true|false>`
  - `/reinodoce rule min-member-level <level>`
  - `/reinodoce syntetic gift <value>`
  - `/reinodoce syntetic gift-combo <ignore|single|bulk>`
  - `/reinodoce syntetic follow <true|false>`
  - `/reinodoce syntetic join <true|false>`
  - `/reinodoce syntetic member-level <true|false>`
  - `/reinodoce reload`

## Stack

- Primary Minecraft target: `1.20.1`
- Primary Forge target: `47.4.16`
- ForgeGradle: `net.minecraftforge.gradle` `[6.0,6.2)`
- Gradle wrapper: `8.8`
- Java target for 1.20.1: `17`
- TikTokLiveJava: `1.11.11-Release` (jar provisioned from a GitHub release)

## Initial setup (Gradle wrapper)

The wrapper jar (`gradle/wrapper/gradle-wrapper.jar`) is **not** tracked
in this repository — `*.jar` is listed in `.gitignore`. After cloning,
materialize it once using an existing Gradle installation on your
system:

```bash
gradle wrapper --gradle-version=8.8
```

```powershell
gradle wrapper --gradle-version=8.8
```

The version must match the one recorded in
`gradle/wrapper/gradle-wrapper.properties` (currently `8.8`). After
this step, `./gradlew` (Linux / macOS) and `.\gradlew.bat` (Windows)
work normally for all the commands described below.

If you do not have `gradle` installed yet, use
[SDKMAN!](https://sdkman.io/) (`sdk install gradle 8.8`) on Linux /
macOS or [Scoop](https://scoop.sh/) (`scoop install gradle@8.8`) on
Windows.

The CI workflow runs `gradle wrapper` automatically before the build,
so `mc<MC>-v<mod_version>` tags pushed for release produce artifacts
without requiring this manual step.

## Internationalization

The mod ships language bundles under `assets/reinodoce_mctiktok/lang/`,
aligned with the largest TikTok LIVE gaming-streamer markets:

- `en_us` — English (baseline / fallback).
- `pt_br` — Brazilian Portuguese.
- `id_id` — Indonesian.
- `es_mx` — Spanish (Mexico / LATAM).
- `vi_vn` — Vietnamese.
- `th_th` — Thai.
- `fil_ph` — Filipino.
- `tr_tr` — Turkish.
- `ja_jp` — Japanese.
- `fr_fr` — French.
- `de_de` — German.
- `ar_sa` — Arabic.

The Minecraft client selects the locale automatically based on the
player's language setting, falling back to English when a key is missing.
To add a new locale, drop a matching JSON file into the same folder; no
code change is required.

## Installation (mod usage)

1. Build the mod jar with `./gradlew build` (or `gradlew.bat build` on
   Windows).
2. Copy the jar generated under `build/libs` into the Forge client's
   `mods` folder.
3. Start Minecraft with the matching Forge profile.
4. In game, run `/reinodoce connect @username`.

## Quick example

1. `/reinodoce settings reconnect 5`
2. `/reinodoce rule follower true`
3. `/reinodoce rule min-member-level 1`
4. `/reinodoce syntetic gift-combo "bulk"`
5. `/reinodoce connect @yourusername`
6. `/reinodoce status`

## Bundle Packwiz (client-side)

This section documents the [Packwiz](https://packwiz.infra.link/) bundle
produced by this project. Packwiz is a format / CLI for packaging
Minecraft modpacks, supported natively by Prism Launcher and other tools
that read the spec; the format itself is not tied to any specific
launcher.

### Why the bundle exists

When you drop only the `.jar` into an instance's `mods/` folder, some
launchers (Prism Launcher included) mark the mod as `Side = both` for
lack of metadata, even though it is client-only. The Packwiz bundle
ships a companion `.pw.toml` with `side = "client"` so the launcher
displays the Side column correctly and treats the mod as client-only.

### Bundle structure

```
mods/
  reinodoce-mc-tiktok-<mc_version>-<mod_version>.jar
  .index/
    reinodoce-mc-tiktok.pw.toml
```

Relevant content in `.pw.toml`:

- `side = "client"` — restricts the mod to the client side.
- `x-prismlauncher-loaders = ["forge"]` — target mod loader.
- `x-prismlauncher-mc-versions = ["<mc_version>"]` — Minecraft versions.
- `hash-format = "sha512"` and `hash = "<sha512>"` — integrity check for
  the jar referenced by `filename` in the same bundle.

The `x-prismlauncher-*` prefixes are Packwiz extensions recognised by
Prism Launcher; other Packwiz consumers simply ignore them.

### Gradle tasks

The names carry the `prism` prefix for historical reasons, but what they
emit is the standard Packwiz format. Renaming to `packwiz*` is planned
as a follow-up.

| Task                  | What it does                                                  |
| --------------------- | ------------------------------------------------------------- |
| `prismMetadata`       | Generates `build/prism-index/reinodoce-mc-tiktok.pw.toml`.    |
| `prismBundle`         | Stages `build/prism-bundle/` with `mods/` and `mods/.index/`. |
| `verifyPrismMetadata` | Confirms `side=client`, `hash-format=sha512`, SHA-512 of jar. |

Recommended command to generate the bundle locally:

```powershell
.\gradlew.bat clean build prismBundle verifyPrismMetadata
```

```bash
./gradlew clean build prismBundle verifyPrismMetadata
```

Output:

- `build/prism-bundle/mods/<jar>`
- `build/prism-bundle/mods/.index/<mod>.pw.toml`

### Usage per launcher

**Prism Launcher** (native Packwiz consumption):

1. Close the instance in Prism.
2. Copy everything from `build/prism-bundle/mods/` (including `.index/`)
   into the instance's `mods/` folder.
3. Open the instance: the mod's Side column reads `client`.

**Other launchers / vanilla Forge** (CurseForge, ATLauncher, manual
installation, test servers):

1. Copy only the `.jar` from `build/libs/` (or
   `build/prism-bundle/mods/`) into the Forge profile's `mods/` folder.
2. The `.pw.toml` file is metadata — Forge ignores it; you can drop it
   in alongside if the launcher understands Packwiz, or discard it if
   not.

### Published artifact

The release CI attaches a zipped version of the bundle to each release,
named `reinodoce-mc-tiktok-<mc_version>-<mod_version>-packwiz.zip`. The
zip already follows the `mods/...` + `mods/.index/...` layout, so it
can be extracted directly into the instance.

The bare `.jar` is also attached separately on each release for users
who just want a manual install.

### References

- Packwiz spec / CLI: <https://packwiz.infra.link/>
- Prism Launcher documentation: <https://prismlauncher.org/wiki/>
- This branch pins `mc_version = 1.20.1` and `forge`, reflected in the
  `x-prismlauncher-*` fields of the `.pw.toml`.

## Configuration persistence

Local client configuration lives at:

- `config/reinodoce-mc-tiktok-client.json`

Persisted fields:

- `lastUsername`
- `reconnectSeconds`
- `ruleFollowerOnly`
- `ruleMinMemberLevel`
- `synteticGiftMinValue`
- `synteticGiftComboMode`
- `synteticFollowEnabled`
- `synteticJoinEnabled`
- `synteticMemberLevelEnabled`
- `chatPrefix`

## Branch build

This branch is pinned to:

- Minecraft `1.20.1`
- Forge `47.4.16`
- Java `17`

Local build:

```powershell
.\gradlew.bat build
```

The artifact on this branch exists for the Forge 1.20.1 adapter. The
shared core lives in the `shared/latest` and `shared/java17` branches.

## Release automation (GitHub Actions)

Releases are created automatically when a tag of the form
`mc<minecraft_version>-v<mod_version>` is pushed to the GitHub
repository (e.g. `mc1.20.1-v0.1.0`). The workflow lives in
`.github/workflows/release.yml`.

The `mc<MC>` prefix lets every version branch (1.20.1, 1.21.x, ...)
publish tags independently without colliding in the same namespace.

### What the workflow does

- Builds with Temurin JDK 17 and Gradle action cache.
- Caches the `TikTokLiveJava` jar in `libs/` keyed by
  `tiktoklive_version` in `gradle.properties`, avoiding redownload on
  every run.
- Enforces that the tag matches both `minecraftVersion` in
  `build.gradle` **and** `mod_version` in `gradle.properties`, and that
  a `## <mod_version>` section exists in the branch's `CHANGELOG.md`.
- Runs `./gradlew clean build prismBundle verifyPrismMetadata`, chaining
  the `verifyEmbeddedPackages` and `verifyCoremodResources` gates via
  `check`.
- Packages `build/prism-bundle/` into a helper zip.
- Generates `SHA256SUMS.txt` and `SHA512SUMS.txt` for all artifacts.
- Attaches the mod `.jar`, the bundle zip, and the checksum files to
  the release, with notes auto-generated from history.

### Security policy

- Every third-party action is **pinned by 40-char commit SHA** (with
  the short tag as a trailing comment). Version bumps require updating
  the SHA explicitly in the workflow.
- Releases are reproducible: the tag-gate and changelog-gate prevent
  accidental releases, and the checksums allow post-download integrity
  verification.

### Cutting a release

On the branch for the target Minecraft version (e.g. `1.20.1`):

```powershell
# 1. Adjust mod_version in gradle.properties.
# 2. Add the matching entry in CHANGELOG.md (## <X.Y.Z>).
git commit -am "release: v0.1.0 (MC 1.20.1)"

# 3. Create the mc<minecraftVersion>-v<mod_version> tag and push it.
git tag mc1.20.1-v0.1.0
git push github mc1.20.1-v0.1.0
```

The published artifact is named
`reinodoce-mc-tiktok-<mc_version>-<mod_version>.jar`, making the
Minecraft version explicit in the filename. The client bundle zip
follows the same pattern with the `-packwiz.zip` suffix (Packwiz
format, consumed by Prism Launcher and other Packwiz-compatible
launchers).

### Pre-releases (alpha / beta / rc)

If `mod_version` in `gradle.properties` ends with one of the suffixes
`-alpha[.N]`, `-beta[.N]`, `-rc[.N]`, `-pre`, or `-snapshot`, the
workflow flags the release as a **pre-release** on GitHub automatically
("Pre-release" badge and exclusion from `latest` in the API).

Example alpha tag:

```powershell
# In gradle.properties: mod_version=0.2.0-alpha.1
# In CHANGELOG.md: section "## [0.2.0-alpha.1]" present.
git commit -am "alpha: v0.2.0-alpha.1 (MC 1.20.1)"
git tag mc1.20.1-v0.2.0-alpha.1
git push github mc1.20.1-v0.2.0-alpha.1
```

To promote to a stable release, change `mod_version` to `0.2.0`, move
the CHANGELOG entry, and push `mc1.20.1-v0.2.0`.

The action can also be triggered manually from the Actions tab
(`workflow_dispatch`) to produce build artifacts without publishing.

### Checksum verification

After downloading the artifacts from the release page:

```powershell
# Windows
Get-FileHash .\reinodoce-mc-tiktok-*.jar -Algorithm SHA256
```

```bash
# Linux / macOS
sha256sum -c SHA256SUMS.txt
sha512sum -c SHA512SUMS.txt
```

## Branch strategy

- `shared/latest`: newest shared baseline.
- `shared/java17`: Java 17 shared baseline.
- `1.20.1`: Forge 1.20.1 adapter built on top of `shared/java17`.

Maintenance flow:

- Common changes land first on the relevant `shared/*` branch.
- They are then merged into the version-specific branch.
- There is no lateral merge between version branches.

Note: Git does not allow `shared` and `shared/java17` to coexist as
branches. That is why the newest shared baseline was implemented as
`shared/latest`.

## TikTokLiveJava dependency

To avoid JitPack instability during the build, the project automatically
provisions:

- `Client-<version>-all.jar` from the official TikTokLiveJava GitHub
  release.
- The final mod artifact embeds this dependency inside the produced `.jar`.
- For compatibility with Forge + Connector / Fabric API, libraries
  provided by the host (`com.google.gson` and `org.slf4j`) are excluded
  from the final mod packaging.

Optional property to use a custom path:

- `-Ptiktoklive_jar_path=<path-to-jar>`

## Threading and robustness

- The TikTok integration runs off the game thread.
- Minecraft chat is always dispatched from the client's safe context.
- Reconnect uses a dedicated scheduler.
- Local reconnect telemetry (attempt counter in `/reinodoce status`).
- Lifecycle token prevents stale callbacks from affecting the current
  session.
- Protection against repeated connect / disconnect calls and active
  username swaps.
- Error / reconnect chat warnings are throttled to reduce flooding.

## Limitations

- The TikTok integration uses an unofficial API.
- Changes on TikTok may break events or the connection without notice.
- Member level may vary per region or LIVE payload.

## Troubleshooting

- Gradle reports a Java error:
  - use JDK 17 for the 1.20.1 build (point `JAVA_HOME` at Java 17).
- Launcher fails to load the new mod build:
  - remove the old mod jar from the `mods` folder before copying the new
    artifact.
- No messages appear in game:
  - check that the chat HUD is enabled.
  - run `/reinodoce status` to inspect state and the last error.
- Invalid username:
  - use `@username` with letters, digits, `.`, or `_`.
- LIVE offline:
  - tune the reconnect interval with
    `/reinodoce settings reconnect <seconds>`.

## Important note

This mod does not try to impersonate a real player on the server. Every
LIVE message is rendered with a visible prefix (`[LIVE]`) so the external
origin stays identifiable.

## Verification

```powershell
.\gradlew.bat test
.\gradlew.bat build
```

If `JAVA_HOME` is not set, `gradlew.bat` tries to discover a compatible
local Java installation before falling back to the system `java.exe`.
This avoids the common failure where the machine's global Java is newer
than what the current Gradle wrapper supports.

## AI agents

Read `AGENTS.md` before editing this repository. Preserve the
`/reinodoce ...` commands, the `config/reinodoce-mc-tiktok-client.json`
file, and the Forge / Minecraft line documented in this README when
updating code or documentation.
