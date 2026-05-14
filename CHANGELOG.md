# Changelog

All notable changes to this project are documented in this file.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and
this project adheres to [Semantic Versioning](https://semver.org/).

The release CI requires a `## <version>` section matching `mod_version` in
`gradle.properties` before publishing a tag of the form
`mc<minecraftVersion>-v<mod_version>` (e.g. `mc1.20.1-v0.1.0`). Each
Minecraft-version branch maintains its own CHANGELOG.

Pre-release suffixes (`-alpha[.N]`, `-beta[.N]`, `-rc[.N]`, `-pre`,
`-snapshot`) on `mod_version` flag the GitHub Release as a pre-release
automatically.

## [Unreleased]

### Fixed

- Correct the synthetic event command branch spelling to `/reinodoce synthetic`.

## [0.1.3]

### Fixed

- Increase TikTokLiveJava HTTP timeout to 15 seconds to avoid reconnect
  loops on slower TikTok LIVE routes.

### Added

- Add an opt-in TikTokLiveJava integration test for checking real LIVE
  account connectivity outside the mod runtime.

## [0.1.2]

### Changed

- Update `TikTokLiveJava` to `1.11.12-Release`.
- Add a `.mrpack` release artifact that installs the jar as a normal mod
  under `mods/` with client-only file metadata, while keeping the
  existing Packwiz helper zip.
- Clarify Prism install paths for existing instances, `.mrpack` imports,
  and Packwiz side metadata with real release download URLs.

## [0.1.1]

### Added

- Add configurable LIVE chat prefix and format commands for mirrored TikTok
  messages.
- Publish tagged release jars to Modrinth automatically, with an early
  credential check for `MODRINTH_TOKEN`.

### Changed

- Keep inline media cache maintenance off the download queue and retry disk
  cleanup promptly when scheduling is rejected.
- Reuse one ready-metadata write path for inline media disk hits and cache
  access refreshes.
- Clarify release artifact checksum verification and `/reinodoce status`
  diagnostics in the docs.

## [0.1.0]

### Changed

- Verify the provisioned `TikTokLiveJava` jar against a pinned SHA-512 before embedding it in release artifacts.
- Bound inline media downloads by scheme, host address, content type, response size, decoded dimensions, and disk cache cleanup.
- Align shared Java 17 config defaults with the documented client defaults.

### Added

- Client-side Forge 1.20.1 mod that mirrors TikTok LIVE chat into the local Minecraft chat.
- Commands: `/reinodoce connect|disconnect|status|settings|rule|syntetic|reload`.
- Client-side mod bundle in Packwiz format with `side = "client"`
  metadata (`prismBundle`, `verifyPrismMetadata`) for Prism Launcher
  display.
- GitHub Actions release workflow with SHA-256/SHA-512 checksums, TikTokLiveJava jar caching, changelog gate, and SHA-pinned third-party actions.
- Command-tree and config-contract tests for the public `/reinodoce` and persisted config surfaces.
