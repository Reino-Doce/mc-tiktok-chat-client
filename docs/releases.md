# Release automation (GitHub Actions)

Two workflows live in `.github/workflows/`:

- **`ci.yml`** — runs on every push (any branch except tags) and on every
  pull request. Builds the mod jar, the Packwiz bundle, and the
  `.mrpack`, runs the lint suite (`check`) and tests, and uploads the artifacts plus
  `SHA256SUMS.txt` / `SHA512SUMS.txt` so any commit produces a downloadable
  build under the Actions tab. Use this for verifying changes, sharing a
  preview build, or grabbing a snapshot jar without cutting a tag.
- **`release.yml`** — the release build and publishing pipeline.
  `workflow_dispatch` branch runs are build-only: they produce the
  Actions artifacts but do not create a GitHub Release or publish to
  Modrinth or CurseForge. Only `mc*-v*` tag refs create or update a
  GitHub Release and publish the release jar to Modrinth through
  MC-Publish and to CurseForge through the CurseForge upload API.
  Modrinth requires the repository secret `MODRINTH_TOKEN`; CurseForge
  requires the repository secret `CURSEFORGE_API_TOKEN` and repository
  variables described below.

## When a tagged release fires

Releases are created automatically when a tag of the form
`mc<minecraft_version>-v<mod_version>` is pushed to the GitHub
repository (e.g. `mc1.20.1-v0.1.0`). The workflow lives in
`.github/workflows/release.yml`.

The `mc<MC>` prefix lets every version branch (`1.20.1`, `1.21.x`, …)
publish tags independently without colliding in the same namespace.

## What the workflow does

- Builds with Temurin JDK 17, the checked-in Gradle wrapper, and the
  Gradle action cache. The wrapper distribution checksum is pinned in
  `gradle/wrapper/gradle-wrapper.properties`.
- Caches the `TikTokLiveJava` jar in `libs/` keyed by `tiktoklive_version`
  in `gradle.properties`, avoiding redownload on every run. The build
  verifies cached and freshly downloaded copies against
  `tiktoklive_sha512`.
- Enforces that the tag matches both `minecraftVersion` in `build.gradle`
  **and** `mod_version` in `gradle.properties`, and that a `## <mod_version>`
  section exists in the branch's `CHANGELOG.md`. The matching section is
  extracted into `release-notes.md` for Modrinth.
- Fails tagged releases before the build if `MODRINTH_TOKEN` is not
  configured.
- Fails tagged releases before the build if the CurseForge token,
  project id, or game version ids are missing or malformed.
- Runs `./gradlew clean check build prismBundle verifyPrismMetadata`,
  including the `verifyEmbeddedPackages` and `verifyCoremodResources`
  gates wired into `check`.
- Packages `build/prism-bundle/` into a Packwiz helper zip and builds
  `build/distributions/<artifact>.mrpack`.
- Generates `SHA256SUMS.txt` and `SHA512SUMS.txt` for all distribution
  artifacts.
- Uploads the bare Forge mod jar to CurseForge for tagged releases,
  using the `CHANGELOG.md` section as the CurseForge changelog.
- Attaches the mod `.jar`, `.mrpack`, Packwiz zip, checksum files, and
  CurseForge publish marker to the GitHub Release.
- Publishes the bare Forge mod jar to the Modrinth project using
  `secrets.MODRINTH_TOKEN`. The `.mrpack` and Packwiz helper zip remain
  GitHub Release artifacts.

## Security policy

- Every third-party action is **pinned by 40-char commit SHA** (with the
  short tag as a trailing comment). Version bumps require updating the
  SHA explicitly in the workflow.
- MC-Publish is checked out at the pinned commit before publishing. Its
  action metadata is patched from Node16 to Node24 at runtime because the
  pinned MC-Publish release still declares Node16, which GitHub-hosted
  runners no longer support.
- Modrinth credentials must be stored only as the GitHub Actions
  repository secret `MODRINTH_TOKEN`; do not commit API tokens or local
  `.env` files.
- CurseForge credentials must be stored only as the GitHub Actions
  repository secret `CURSEFORGE_API_TOKEN`. The workflow sends the token
  in the `X-Api-Token` header, never in a URL.
- Releases are reproducible: the tag-gate and changelog-gate prevent
  accidental releases, and the checksums allow post-download integrity
  verification.

## Modrinth publishing

The Modrinth publish step uses MC-Publish and the project id configured
in `.github/workflows/release.yml`. The workflow checks out the pinned
MC-Publish commit locally, verifies the resolved commit, updates only the
runtime declared in `action.yml`, and then invokes that local action.
Before any tagged release, create a GitHub Actions repository secret named
`MODRINTH_TOKEN` with a Modrinth token that can create versions for the
project. Missing credentials stop the tagged release before the build starts.

Version type is derived from `mod_version`:

- `*-alpha.N` and `*-snapshot` publish as Modrinth `alpha`.
- `*-beta.N`, `*-rc.N`, and `*-pre` publish as Modrinth `beta`.
- All other versions publish as Modrinth `release`.

The Modrinth upload uses the exact jar built by Gradle:
`build/libs/reinodoce-mc-tiktok-<mc_version>-<mod_version>.jar`, with
loader `forge`, game version from `minecraftVersion`, and Java `17`.

## CurseForge publishing

Tagged releases publish the same bare Forge mod jar to CurseForge through
the official upload API. The workflow does **not** upload the Packwiz
helper zip as the CurseForge primary file because this project ships a
client-side Forge mod, not a CurseForge modpack. The `.mrpack` and
Packwiz helper zip remain attached to the GitHub Release for launcher
and Prism/Packwiz workflows.

Before the first tagged release, configure these GitHub Actions values:

| Type | Name | Required | Notes |
| ---- | ---- | -------- | ----- |
| Secret | `CURSEFORGE_API_TOKEN` | yes | CurseForge author token used in the `X-Api-Token` header. |
| Variable | `CURSEFORGE_PROJECT_ID` | yes | Numeric CurseForge project id. |
| Variable | `CURSEFORGE_GAME_VERSIONS` | yes | Comma-separated numeric CurseForge game version ids. Include the ids CurseForge expects for this Minecraft/Forge release. |
| Variable | `CURSEFORGE_MANUAL_RELEASE` | no | Set to `true` to mark uploaded files for manual release in CurseForge. |

Use CurseForge's game versions API or the project dashboard to confirm
the exact numeric ids for Minecraft, Forge, and any required loader tags.
Those ids are CurseForge data, so they are intentionally not hard-coded
in this repository.

CurseForge release type is derived from `mod_version` using the same
suffix policy as Modrinth:

- `*-alpha.N` and `*-snapshot` publish as CurseForge `alpha`.
- `*-beta.N`, `*-rc.N`, and `*-pre` publish as CurseForge `beta`.
- All other versions publish as CurseForge `release`.

The workflow ensures the GitHub Release exists before uploading to
CurseForge, then writes a temporary `curseforge-upload-started.json`
asset before calling CurseForge. After a successful CurseForge upload it
writes `curseforge-publish.json` into the GitHub Release assets and
removes the temporary marker. The final marker contains the tag,
CurseForge project id, returned file id, uploaded artifact name, and
artifact SHA-256. On a rerun, the workflow reuses the final marker when
the tag, project id, and artifact hash match, so it does not upload the
same jar again. If a GitHub Release has a temporary started marker or
distribution assets for the tag without the final marker, the workflow
fails intentionally because the previous CurseForge state is ambiguous
and should be checked manually.

`workflow_dispatch` remains a build-only dry run when executed from a
branch. It produces the release artifacts but does not create a GitHub
Release or publish to Modrinth or CurseForge. For a real CurseForge
upload test, use a separate experimental CurseForge project, set
`CURSEFORGE_PROJECT_ID` and `CURSEFORGE_GAME_VERSIONS` to that project,
and consider `CURSEFORGE_MANUAL_RELEASE=true`.

If a CurseForge file needs to be withdrawn after upload, prefer archiving
it in the CurseForge dashboard first. Delete only when archiving is not
enough, then fix the repository and cut a new release tag.

## Cutting a release

On the branch for the target Minecraft version (e.g. `1.20.1`):

```powershell
# 1. Adjust mod_version in gradle.properties.
# 2. Add the matching entry in CHANGELOG.md (## <X.Y.Z>).
git commit -am "release: v0.1.1 (MC 1.20.1)"

# 3. Create the mc<minecraftVersion>-v<mod_version> tag and push it.
git tag mc1.20.1-v0.1.1
git push github mc1.20.1-v0.1.1
```

The published jar is named
`reinodoce-mc-tiktok-<mc_version>-<mod_version>.jar`, making the
Minecraft version explicit in the filename. The `.mrpack` follows the
same base filename with the `.mrpack` extension for launcher import. The
Packwiz helper zip follows the same pattern with the `-packwiz.zip`
suffix. See [packwiz-bundle.md](packwiz-bundle.md).

## Pre-releases (alpha / beta / rc)

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

## Checksum verification

After downloading the artifacts from the release page:

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
