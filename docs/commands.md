# `/reinodoce` commands

All commands are **client-side only** — they run on the local Minecraft
client and never reach the server. They are available in singleplayer and
multiplayer.

Bare `/reinodoce` (no subcommand) is equivalent to `/reinodoce status`.

## Connection

| Command | Argument | Effect |
| ------- | -------- | ------ |
| `/reinodoce connect <username>` | TikTok handle, 2–30 chars of `A-Z a-z 0-9 . _` (the leading `@` is optional and stripped) | Starts the TikTok LIVE connection for `<username>` and mirrors chat events into Minecraft. Persists `<username>` as `lastUsername` after a successful connection start. |
| `/reinodoce disconnect` | — | Closes the active connection and cancels any pending reconnect. |
| `/reinodoce status` | — | Prints connection state, target username, reconnect timing, active rules, last error (if any), inline media renderer state, token count, cache stats, download counters, and cache eviction counters. |
| `/reinodoce reload` | — | Re-reads `config/reinodoce-mc-tiktok-client.json` from disk and propagates the new values to the active TikTok session. Use it after editing the file by hand. |

## Settings

Settings change runtime behavior and are persisted to the client config.

| Command | Argument | Default | Effect |
| ------- | -------- | ------- | ------ |
| `/reinodoce settings reconnect <seconds>` | integer ≥ 0 | `5` | Delay between reconnect attempts when the LIVE drops or is offline. `0` disables reconnect. |
| `/reinodoce settings chat-emotes <true\|false>` | boolean | `true` | When `true`, TikTok chat emotes are rendered inline in the Minecraft chat HUD. |
| `/reinodoce settings prefix <value>` | text | `"[LIVE]"` | Prefix rendered on every mirrored TikTok line. Blank values fall back to the default. |
| `/reinodoce settings format <template>` | text containing `{prefix}`, `{username}`, and `{message}` | current layout | Template used to arrange mirrored TikTok lines. Unknown tokens or templates missing a required token fall back to the default. |

The chat format template supports exactly these tokens:

- `{prefix}` — configured LIVE prefix.
- `{username}` — TikTok display username, including avatar / emoji media when inline rendering is enabled.
- `{message}` — chat body or synthetic event body, including inline emotes and gift icons when enabled.

Example:

```text
/reinodoce settings prefix "[TikTok]"
/reinodoce settings format "{prefix} <{username}> {message}"
```

## Rules (incoming chat filtering)

Rules decide which TikTok chat comments are mirrored into Minecraft.

| Command | Argument | Default | Effect |
| ------- | -------- | ------- | ------ |
| `/reinodoce rule follower <true\|false>` | boolean | `false` | When `true`, only comments from accounts that follow the streamer are mirrored. |
| `/reinodoce rule min-member-level <level>` | integer ≥ 0 | `0` | Minimum LIVE member level required to mirror a comment. `0` disables this filter. |

Both rules apply together — a comment is mirrored only if it passes every
active filter.

## Synthetic events (non-chat → chat lines)

Synthetic events convert non-chat TikTok LIVE events (gifts, follows,
joins, member-level changes) into visible `[LIVE]`-prefixed chat lines.

| Command | Argument | Default | Effect |
| ------- | -------- | ------- | ------ |
| `/reinodoce synthetic gift <value>` | integer ≥ 0 | `1` | Minimum gift diamond cost to surface. `0` disables gift announcements; `1` surfaces every gift. |
| `/reinodoce synthetic gift-combo <ignore\|single\|bulk>` | enum | `bulk` | How gift combos are folded. `ignore` skips combo updates; `single` emits one line per combo step; `bulk` emits one summary line per combo. |
| `/reinodoce synthetic follow <true\|false>` | boolean | `false` | Surface new-follower events. |
| `/reinodoce synthetic join <true\|false>` | boolean | `false` | Surface viewer-join events. |
| `/reinodoce synthetic member-level <true\|false>` | boolean | `false` | Surface member-level-up events. |

## Quick example

```text
/reinodoce settings reconnect 5
/reinodoce rule follower true
/reinodoce rule min-member-level 1
/reinodoce synthetic gift-combo bulk
/reinodoce connect @yourusername
/reinodoce status
```

See also: [configuration.md](configuration.md) for the on-disk shape of
every setting.
