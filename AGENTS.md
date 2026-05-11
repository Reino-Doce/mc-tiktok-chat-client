# AGENTS - mc-tiktok-chat-client

## Quick commands

```powershell
.\gradlew.bat test
.\gradlew.bat build
.\gradlew.bat clean build prismBundle verifyPrismMetadata
```

## Purpose

- Forge 1.20.1 client-side Minecraft mod that mirrors TikTok LIVE chat into the local game client.
- The public user-facing surface is the `/reinodoce` command tree plus the persisted client config file.
- The repo also owns Prism bundle metadata for a client-only distribution flow.

## Public contracts

- Command registration in `src/main/java/br/com/reinodoce/mctiktok/command/ReinodoceCommandRegistrar.java`.
- Command tree behavior in `src/main/java/br/com/reinodoce/mctiktok/command/ReinodoceCommandTree.java`.
- Mod metadata and compatibility in `src/main/resources/META-INF/mods.toml` and `build.gradle`.
- Persisted client settings in `config/reinodoce-mc-tiktok-client.json`.

## Internal dependencies

- Java 17 and ForgeGradle for Minecraft `1.20.1`.
- Provisioned `TikTokLiveJava` jar declared in `build.gradle`.
- Shared core lineage described in `README.md`.

## Safe change workflow

1. Preserve the `/reinodoce` command surface and config file shape unless the user asks for a break.
2. Keep branch-line assumptions (`1.20.1`, `shared/latest`, `shared/java17`) consistent with the current maintenance model.
3. Update `README.md` in the same change when mod behavior or Prism packaging changes.
4. Run the Gradle verification commands before finalizing.

## Verification commands

- Install: not required
- Test: `.\gradlew.bat test`
- Smoke: `.\gradlew.bat build`
- Typecheck: not configured
- Lint: `.\gradlew.bat check` (runs Checkstyle, PMD, SpotBugs)
- Individual: `.\gradlew.bat pmdMain checkstyleMain spotbugsMain`
- Reports: `build/reports/{checkstyle,pmd,spotbugs}/`

When `JAVA_HOME` is not set, `gradlew.bat` tries to pick a compatible local
Java installation (`jdk-22`, `jdk-21`, `jdk-17`, or `jre1.8`) before falling
back to the system's default Java.

## Boundaries

### Always

- Keep the mod client-side only.
- Preserve Prism metadata that marks the package as `side = "client"`.
- Treat findings from PMD / Checkstyle / SpotBugs as defects to fix at the call site, not to suppress. Add a file-level suppression only with a one-line justification.

### Ask first

- Changing Minecraft, Forge, or Java major versions.
- Renaming commands or breaking the saved config format.

### Never

- Edit `build/` outputs as source of truth.
- Commit private jars, launcher secrets, or external service credentials.

## Security notes

- Treat provisioned jars and launcher-local config as sensitive inputs.
- Do not commit credentials, cookies, or private distribution artifacts.
