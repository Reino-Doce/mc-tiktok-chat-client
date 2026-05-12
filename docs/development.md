# Development

This page covers everything beyond the basic local build. For the
TL;DR build flow see the [README](../README.md).

## Repository layout

| Path                                        | Purpose                                          |
| ------------------------------------------- | ------------------------------------------------ |
| `src/main/java/br/com/reinodoce/mctiktok/`  | Mod source.                                      |
| `src/main/resources/`                       | Assets, language bundles, coremod manifest.      |
| `assets/`                                   | Repository-level assets (icons, screenshots).    |
| `config/reinodoce-mc-tiktok-client.json`    | Runtime client config (see `configuration.md`).  |
| `libs/`                                     | Provisioned `TikTokLiveJava` jar (not checked in). |
| `build/`                                    | Gradle outputs.                                  |
| `.github/workflows/release.yml`             | Release automation (see `releases.md`).          |

The shared / multi-version layout is described in
[branches.md](branches.md).

## Stack

- Minecraft `1.20.1`
- Forge `47.4.16`
- ForgeGradle `net.minecraftforge.gradle` `[6.0,6.2)`
- Gradle wrapper `8.8`
- Java target for 1.20.1: `17`
- `TikTokLiveJava` `1.11.11-Release` (jar provisioned from a GitHub release)

## Bootstrapping the Gradle wrapper

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

If you do not have `gradle` installed, use
[SDKMAN!](https://sdkman.io/) (`sdk install gradle 8.8`) on Linux /
macOS or [Scoop](https://scoop.sh/) (`scoop install gradle@8.8`) on
Windows.

The CI workflow runs `gradle wrapper` automatically before the build,
so `mc<MC>-v<mod_version>` tags pushed for release produce artifacts
without requiring this manual step.

## TikTokLiveJava dependency

To avoid JitPack instability during the build, the project automatically
provisions:

- `Client-<version>-all.jar` from the official `TikTokLiveJava` GitHub
  release, into `libs/`.

The final mod artifact embeds this dependency inside the produced
`.jar`. For compatibility with Forge + Connector / Fabric API, libraries
provided by the host (`com.google.gson` and `org.slf4j`) are excluded
from the final mod packaging.

To point at a local jar instead of the auto-provisioned one:

```bash
./gradlew build -Ptiktoklive_jar_path=/absolute/path/to/Client-1.11.11-Release-all.jar
```

## Verification commands

```powershell
.\gradlew.bat test
.\gradlew.bat build
.\gradlew.bat check
```

`check` runs the full lint suite (Checkstyle, PMD, SpotBugs). Reports
are written to `build/reports/{checkstyle,pmd,spotbugs}/`. Individual
tools can be invoked one at a time:

```powershell
.\gradlew.bat checkstyleMain
.\gradlew.bat pmdMain
.\gradlew.bat spotbugsMain
```

Lint findings are treated as defects to fix at the call site, not to
suppress. Add a file-level suppression only with a one-line
justification — see [AGENTS.md](../AGENTS.md).

## AI agents

If you are using an AI assistant (Claude Code, Codex, etc.) to edit this
repository, read [AGENTS.md](../AGENTS.md) first. It documents the
contracts that must be preserved when changing code or docs.
