# Branch strategy

- `shared/latest` — newest shared baseline.
- `shared/java17` — Java 17 shared baseline.
- `1.20.1` — Forge 1.20.1 adapter built on top of `shared/java17`.

Each version-specific branch is pinned to one Minecraft / Forge / Java
combination. The `1.20.1` branch, for example, is pinned to:

- Minecraft `1.20.1`
- Forge `47.4.16`
- Java `17`

## Maintenance flow

- Common changes land first on the relevant `shared/*` branch.
- They are then merged into the version-specific branch.
- There is no lateral merge between version branches.

Each version-specific branch carries its own `CHANGELOG.md` and produces
its own tags via the `mc<MC>-v<mod_version>` namespace (see
[releases.md](releases.md)).

> **Note:** Git does not allow `shared` and `shared/java17` to coexist
> as branches. That is why the newest shared baseline is named
> `shared/latest`.
