# Packwiz bundle (client-only distribution)

This project produces a [Packwiz](https://packwiz.infra.link/) bundle in
addition to the bare mod jar. Packwiz is a format / CLI for packaging
Minecraft modpacks, consumed natively by Prism Launcher and other
Packwiz-aware tools. The format itself is not tied to any launcher.

## Why the bundle exists

When a `.jar` is dropped into an instance's `mods/` folder, some
launchers (Prism Launcher included) mark the mod as `Side = both` for
lack of metadata, even though it is client-only. The Packwiz bundle
ships a companion `.pw.toml` with `side = "client"` so the launcher
displays the Side column correctly and treats the mod as client-only.

## Bundle structure

```
mods/
  reinodoce-mc-tiktok-<mc_version>-<mod_version>.jar
  .index/
    reinodoce-mc-tiktok.pw.toml
```

Relevant fields in `.pw.toml`:

- `side = "client"` — restricts the mod to the client side.
- `x-prismlauncher-loaders = ["forge"]` — target mod loader.
- `x-prismlauncher-mc-versions = ["<mc_version>"]` — Minecraft versions.
- `hash-format = "sha512"` and `hash = "<sha512>"` — integrity check for
  the jar referenced by `filename` in the same bundle.

The `x-prismlauncher-*` prefixes are Packwiz extensions recognised by
Prism Launcher; other Packwiz consumers simply ignore them.

## Gradle tasks

The task names carry the `prism` prefix for historical reasons, but
what they emit is the standard Packwiz format. Renaming to `packwiz*`
is planned as a follow-up.

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

## Usage per launcher

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

## Published artifact

The release CI attaches a zipped version of the bundle to each release,
named `reinodoce-mc-tiktok-<mc_version>-<mod_version>-packwiz.zip`. The
zip already follows the `mods/...` + `mods/.index/...` layout, so it
can be extracted directly into the instance.

The bare `.jar` is also attached separately on each release for users
who just want a manual install.

## References

- Packwiz spec / CLI: <https://packwiz.infra.link/>
- Prism Launcher documentation: <https://prismlauncher.org/wiki/>
- This branch pins `mc_version = 1.20.1` and `forge`, reflected in the
  `x-prismlauncher-*` fields of the `.pw.toml`.
