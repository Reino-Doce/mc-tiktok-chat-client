# Packwiz and Modrinth pack bundles

This project publishes three install artifacts:

- A bare Forge `.jar` for existing instances and manual installs.
- A `-packwiz.zip` helper layout with Prism / Packwiz metadata.
- A `.mrpack` Modrinth pack for importing a new client instance.

The mod itself is client-side. It runs in the local Minecraft client and
does not require the multiplayer server to install this mod.

## Why the Packwiz bundle exists

When a `.jar` is dropped into an instance's `mods/` folder, Prism has no
launcher metadata for it and can display `Side = both`. The companion
`mods/.index/*.pw.toml` file records the jar as `side = "client"` so the
Prism mod list presents the client-only side correctly.

The Forge mod behavior does not depend on this metadata. Minecraft loads
the `.jar`; Prism reads the `.index` file only for launcher-side display
and provider metadata.

## Packwiz bundle structure

```
mods/
  reinodoce-mc-tiktok-<mc_version>-<mod_version>.jar
  .index/
    reinodoce-mc-tiktok.pw.toml
```

Relevant fields in `.pw.toml`:

- `side = "client"`: marks the mod as client-side in Prism.
- `filename = "<jar>"`: points Prism at the jar in the same `mods/`
  folder.
- `x-prismlauncher-loaders = ["forge"]`: target mod loader.
- `x-prismlauncher-mc-versions = ["<mc_version>"]`: compatible
  Minecraft versions.
- `hash-format = "sha512"` and `hash = "<sha512>"`: integrity data for
  the jar referenced by `filename`.
- `download.url`: the GitHub release URL for the same jar.

The Modrinth project ID is `OrIKDIK6`. The `.pw.toml` only includes
`[update.modrinth]` when the build is given a real
`-Pmodrinth_version_id=<version ID>`, because Packwiz's `version` field
is Modrinth's unique version ID, not this project's `mod_version`.
The side metadata is local and does not depend on an update provider
block.

## Modrinth `.mrpack` structure

The `.mrpack` is a Modrinth pack archive with:

```
modrinth.index.json
```

`modrinth.index.json` declares:

- `formatVersion = 1`
- `game = "minecraft"`
- `dependencies.minecraft = "<mc_version>"`
- `dependencies.forge = "<forge_version>"`
- one file at `mods/reinodoce-mc-tiktok-<mc_version>-<mod_version>.jar`
- `files[0].env.client = "required"`
- `files[0].env.server = "unsupported"`
- SHA-1 / SHA-512 hashes, file size, and the GitHub release download URL
  for the jar

The jar is installed as a normal Forge mod under `mods/`. The
client-only behavior is declared in the `.mrpack` file metadata, not by
placing the jar in `client-overrides/`.

## Gradle tasks

| Task                  | What it does                                                  |
| --------------------- | ------------------------------------------------------------- |
| `prismMetadata`       | Generates `build/prism-index/reinodoce-mc-tiktok.pw.toml`.    |
| `prismBundle`         | Stages `build/prism-bundle/` with `mods/` and `mods/.index/`. |
| `verifyPrismMetadata` | Confirms `side=client`, `hash-format=sha512`, SHA-512 of jar. |
| `modrinthPack`        | Generates `build/distributions/<artifact>.mrpack`.            |
| `verifyModrinthPack`  | Confirms the `.mrpack` manifest, hashes, and client-only env. |

Recommended command to generate all release-style artifacts locally:

```powershell
.\gradlew.bat clean build prismBundle verifyPrismMetadata modrinthPack verifyModrinthPack
```

```bash
./gradlew clean build prismBundle verifyPrismMetadata modrinthPack verifyModrinthPack
```

Output:

- `build/libs/<jar>`
- `build/prism-bundle/mods/<jar>`
- `build/prism-bundle/mods/.index/<mod>.pw.toml`
- `build/distributions/<artifact>.mrpack`

## Usage per launcher

**Prism Launcher, existing instance:**

1. Open the instance's Mods tab.
2. Use **Add File** with the bare `.jar`, not `.mrpack` or
   `-packwiz.zip`.
3. Start Minecraft.

**Prism Launcher, optional Side column metadata for an existing
instance:**

1. Close the instance and Prism Launcher.
2. Extract the `-packwiz.zip` into the instance's Minecraft root, not
   into the `mods/` folder itself.
3. Confirm the final layout is:

   ```text
   <instance minecraft root>/mods/<jar>
   <instance minecraft root>/mods/.index/<mod>.pw.toml
   ```

4. Reopen Prism. The mod should show `Side = client`.

If you add `-packwiz.zip` through the Mods tab, Prism copies the zip into
`mods/` as a file. If you extract the zip inside `mods/`, you will get
`mods/mods/<jar>`, which Prism will not load as a mod. If Prism is open
while only the metadata file exists, it can treat that metadata as
orphaned, so copy the jar and `.index` together while Prism is closed.

**Prism Launcher / Modrinth App, new instance:**

Import the `.mrpack`. This creates an instance with the declared
Minecraft and Forge versions and installs the jar as a normal mod under
`mods/`, with `env.client = "required"` and
`env.server = "unsupported"` in `modrinth.index.json`.

**Other launchers / vanilla Forge** (CurseForge, ATLauncher, manual
installation, test servers):

Copy only the `.jar` from `build/libs/` or `build/prism-bundle/mods/`
into the Forge profile's `mods/` folder. The `.pw.toml` file is
Prism-specific launcher metadata and can be ignored elsewhere.

## Published artifacts

GitHub Releases attach:

- `reinodoce-mc-tiktok-<mc_version>-<mod_version>.jar`
- `reinodoce-mc-tiktok-<mc_version>-<mod_version>.mrpack`
- `reinodoce-mc-tiktok-<mc_version>-<mod_version>-packwiz.zip`

The bare `.jar` is the artifact to use with Prism's Mods tab **Add
File** button. The `.mrpack` is for instance import.

## References

- Packwiz Modrinth export: <https://packwiz.infra.link/tutorials/hosting/modrinth/>
- Packwiz metadata file syntax: <https://packwiz.infra.link/tutorials/creating/adding-mods/>
- Modrinth `.mrpack` format: <https://support.modrinth.com/en/articles/8802351-modrinth-modpack-format-mrpack>
- Prism Launcher ZIP resource handling: <https://prismlauncher.org/wiki/help-pages/zip-import/>
- This branch pins `mc_version = 1.20.1` and `forge`, reflected in the
  generated metadata.
