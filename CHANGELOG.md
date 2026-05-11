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

## [0.1.0]

### Added

- Client-side Forge 1.20.1 mod that mirrors TikTok LIVE chat into the local Minecraft chat.
- Commands: `/reinodoce connect|disconnect|status|settings|rule|syntetic|reload`.
- Client-side mod bundle in Packwiz format with `side = "client"` metadata (`prismBundle`, `verifyPrismMetadata`); launcher-agnostic, consumable by Prism Launcher and other Packwiz-aware launchers.
- GitHub Actions release workflow with SHA-256/SHA-512 checksums, TikTokLiveJava jar caching, changelog gate, and SHA-pinned third-party actions.
