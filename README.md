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

## Prism Launcher (Side client vs both)

For local `.jar` mods, Prism Launcher usually shows `Side = both` when no
Packwiz metadata exists in the `.index`.

This project produces a Prism-specific bundle with:

- `mods/reinodoce-mc-tiktok-<version>.jar`
- `mods/.index/reinodoce-mc-tiktok.pw.toml` with `side = "client"`

Command to generate the Prism bundle:

```bash
./gradlew clean build prismBundle verifyPrismMetadata
```

On Windows:

```powershell
.\gradlew.bat clean build prismBundle verifyPrismMetadata
```

Output:

- `build/prism-bundle/mods`

Usage in Prism:

1. Close the instance in Prism.
2. Copy everything from `build/prism-bundle/mods` into the instance's
   `mods` folder.
3. Open the instance in Prism and confirm the mod's Side column reads
   `client`.

Note: using only the `.jar` without the `.pw.toml` still works under Forge,
but Prism may keep showing `both` in the UI.

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
