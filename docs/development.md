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
- `TikTokLiveJava` `1.11.12-Release` (jar provisioned from a GitHub release)

## Gradle wrapper integrity

The wrapper jar (`gradle/wrapper/gradle-wrapper.jar`) is tracked in this
repository, and `gradle/wrapper/gradle-wrapper.properties` pins the
Gradle `8.8` distribution with `distributionSha256Sum`. After cloning,
`./gradlew` (Linux / macOS) and `.\gradlew.bat` (Windows) work normally
without an ambient system Gradle bootstrap step.

CI and release workflows fail early if the wrapper jar or distribution
checksum metadata is missing.

## TikTokLiveJava dependency

To avoid JitPack instability during the build, the project automatically
provisions:

- `Client-<version>-all.jar` from the official `TikTokLiveJava` GitHub
  release, into `libs/`.

The final mod artifact embeds this dependency inside the produced
`.jar`. For compatibility with Forge + Connector / Fabric API, libraries
provided by the host (`com.google.gson` and `org.slf4j`) are excluded
from the final mod packaging.

The provisioned jar is verified against `tiktoklive_sha512` in
`gradle.properties`. Cached copies and freshly downloaded copies both
fail the build when the digest does not match.

To point at a local jar instead of the auto-provisioned one:

```bash
./gradlew build -Ptiktoklive_jar_path=/absolute/path/to/Client-1.11.12-Release-all.jar
```

When using a local jar override, also pass the matching
`-Ptiktoklive_sha512=<sha512>` value.

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
