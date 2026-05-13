# Release automation (GitHub Actions)

Two workflows live in `.github/workflows/`:

- **`ci.yml`** — runs on every push (any branch except tags) and on every
  pull request. Builds the mod jar and the Packwiz bundle, runs the lint
  suite (`check`) and tests, and uploads the artifacts plus
  `SHA256SUMS.txt` / `SHA512SUMS.txt` so any commit produces a downloadable
  build under the Actions tab. Use this for verifying changes, sharing a
  preview build, or grabbing a snapshot jar without cutting a tag.
- **`release.yml`** — the publishing pipeline. Runs only on
  `mc*-v*` tag pushes (and `workflow_dispatch`) and attaches the same
  artifacts to a real GitHub Release. Tagged releases also publish the
  release jar to Modrinth through MC-Publish when the repository secret is
  configured.

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
- Runs `./gradlew clean check build prismBundle verifyPrismMetadata`,
  including the `verifyEmbeddedPackages` and `verifyCoremodResources`
  gates wired into `check`.
- Packages `build/prism-bundle/` into a helper zip.
- Generates `SHA256SUMS.txt` and `SHA512SUMS.txt` for all artifacts.
- Attaches the mod `.jar`, the bundle zip, and the checksum files to
  the release, with notes auto-generated from history.
- Publishes the bare Forge mod jar to the Modrinth project using
  `secrets.MODRINTH_TOKEN`. The Packwiz helper zip remains a GitHub
  Release artifact.

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
- Releases are reproducible: the tag-gate and changelog-gate prevent
  accidental releases, and the checksums allow post-download integrity
  verification.

## Modrinth publishing

The Modrinth publish step uses MC-Publish and the project id configured
in `.github/workflows/release.yml`. The workflow checks out the pinned
MC-Publish commit locally, verifies the resolved commit, updates only the
runtime declared in `action.yml`, and then invokes that local action.
Before the first tagged release, create a GitHub Actions repository secret
named `MODRINTH_TOKEN` with a Modrinth token that can create versions for
the project.

Version type is derived from `mod_version`:

- `*-alpha.N` and `*-snapshot` publish as Modrinth `alpha`.
- `*-beta.N`, `*-rc.N`, and `*-pre` publish as Modrinth `beta`.
- All other versions publish as Modrinth `release`.

The Modrinth upload uses the exact jar built by Gradle:
`build/libs/reinodoce-mc-tiktok-<mc_version>-<mod_version>.jar`, with
loader `forge`, game version from `minecraftVersion`, and Java `17`.

## Cutting a release

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
launchers — see [packwiz-bundle.md](packwiz-bundle.md)).

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
