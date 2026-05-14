# `/reinodoce` commands

All commands are **client-side only** — they run on the local Minecraft
client and never reach the server. They are available in singleplayer and
multiplayer.

Bare `/reinodoce` (no subcommand) is equivalent to `/reinodoce status`.

## Connection

| Command | Argument | Effect |
| ------- | -------- | ------ |
| `/reinodoce connect <username>` | TikTok handle, 2–30 chars of `A-Z a-z 0-9 . _` (the leading `@` is optional and stripped) | Starts the TikTok LIVE connection for `<username>` and mirrors chat events into Minecraft. Persists `<username>` as `lastUsername` after a successful connection start. |
| `/reinodoce connect` | — | Starts the TikTok LIVE connection for the saved `lastUsername`. Prints a clear error when no username has been saved yet. |
| `/reinodoce disconnect` | — | Closes the active connection and cancels any pending reconnect. |
| `/reinodoce status` | — | Prints connection state, target username, reconnect timing, active rules, last error (if any), inline media renderer state, token count, cache stats, download counters, and cache eviction counters. |
| `/reinodoce stats` | — | Prints counters for the current LIVE session, including messages, unique chatters, follows, joins, gifts, diamonds, member-level events, and the top gifter. |
| `/reinodoce stats reset` | — | Clears the current session counters without disconnecting. |
| `/reinodoce reload` | — | Re-reads `config/reinodoce-mc-tiktok-client.json` from disk and propagates the new values to the active TikTok session. Use it after editing the file by hand. |

## Settings

Settings change runtime behavior and are persisted to the client config.

| Command | Argument | Default | Effect |
| ------- | -------- | ------- | ------ |
| `/reinodoce settings reconnect <seconds>` | integer ≥ 0 | `5` | Delay between reconnect attempts when the LIVE drops or is offline. `0` disables reconnect. |
| `/reinodoce settings auto-connect <true\|false>` | boolean | `false` | When `true`, the client attempts one startup connection to the saved `lastUsername`. Blank or invalid saved usernames are ignored safely. |
| `/reinodoce settings chat-emotes <true\|false>` | boolean | `true` | When `true`, TikTok chat emotes are rendered inline in the Minecraft chat HUD. |
| `/reinodoce settings chat-log <true\|false>` | boolean | `false` | When `true`, mirrored TikTok lines are written through Minecraft's chat logger. System/status/error lines remain logged either way. |
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

## Local session logging

Local session logging writes accepted mirrored LIVE events to files under
`logs/reinodoce/` inside the Minecraft instance. Logging is opt-in because
files may contain TikTok usernames and chat content.

| Command | Argument | Default | Effect |
| ------- | -------- | ------- | ------ |
| `/reinodoce logging enabled <true\|false>` | boolean | `false` | Enables or disables local session event logs. When enabled during a connection, logging starts for the current session; disabling closes the active file. |
| `/reinodoce logging format <jsonl\|text>` | enum | `jsonl` | Selects JSON Lines or plain text key-value output. Format changes apply to the next opened session file. |

Each successful connection opens one file such as
`logs/reinodoce/live-2026-05-13T20-30-00.jsonl`. A duplicate timestamp
adds a numeric suffix, for example `live-2026-05-13T20-30-00-2.jsonl`.
Files close on disconnect, reconnect, or when logging is disabled.

## Rules (incoming chat filtering)

Rules decide which TikTok chat comments are mirrored into Minecraft.

| Command | Argument | Default | Effect |
| ------- | -------- | ------- | ------ |
| `/reinodoce rule follower <true\|false>` | boolean | `false` | When `true`, only comments from accounts that follow the streamer are mirrored. |
| `/reinodoce rule min-member-level <level>` | integer ≥ 0 | `0` | Minimum LIVE member level required to mirror a comment. `0` disables this filter. |
| `/reinodoce rule block-word add <word>` | text | — | Adds a case-insensitive blocked word fragment. Messages containing the fragment are hidden. |
| `/reinodoce rule block-word remove <word>` | text | — | Removes a blocked word fragment. |
| `/reinodoce rule block-word list` | — | — | Prints the current blocked word fragments. |
| `/reinodoce rule block-user add <username>` | TikTok handle | — | Adds an exact case-insensitive blocked username. |
| `/reinodoce rule block-user remove <username>` | TikTok handle | — | Removes a blocked username. |
| `/reinodoce rule block-user list` | — | — | Prints the current blocked usernames. |
| `/reinodoce rule max-length <length>` | integer ≥ 0 | `0` | Hides messages longer than `<length>`. `0` disables this filter. |
| `/reinodoce rule duplicate-cooldown <seconds>` | integer ≥ 0 | `0` | Hides repeated duplicate message text for the selected cooldown window. `0` disables this filter. |

All rules apply together — a comment is mirrored only if it passes every
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

## Local alerts

Local alerts play client-only sounds and show client-only toasts for
synthetic events that have already passed the synthetic-event rules
above. They never send packets or chat messages to the server.

| Command | Argument | Default | Effect |
| ------- | -------- | ------- | ------ |
| `/reinodoce alert gift sound <true\|false>` | boolean | `false` | Play a local sound for surfaced gifts that pass `alertGiftMinValue`. |
| `/reinodoce alert gift toast <true\|false>` | boolean | `false` | Show a local toast for surfaced gifts that pass `alertGiftMinValue`. |
| `/reinodoce alert gift-min-value <value>` | integer >= 0 | `1` | Minimum single-gift diamond value required for gift alerts. |
| `/reinodoce alert follow sound <true\|false>` | boolean | `false` | Play a local sound for surfaced follow events. |
| `/reinodoce alert follow toast <true\|false>` | boolean | `false` | Show a local toast for surfaced follow events. |
| `/reinodoce alert join sound <true\|false>` | boolean | `false` | Play a local sound for surfaced join events. Join alerts use a global cooldown to avoid noise. |
| `/reinodoce alert join toast <true\|false>` | boolean | `false` | Show a local toast for surfaced join events. Join alerts use a global cooldown to avoid noise. |
| `/reinodoce alert member-level sound <true\|false>` | boolean | `false` | Play a local sound for surfaced member-level-up events. |
| `/reinodoce alert member-level toast <true\|false>` | boolean | `false` | Show a local toast for surfaced member-level-up events. |

## Quick example

```text
/reinodoce settings reconnect 5
/reinodoce rule follower true
/reinodoce rule min-member-level 1
/reinodoce logging enabled true
/reinodoce synthetic gift-combo bulk
/reinodoce alert gift toast true
/reinodoce connect @yourusername
/reinodoce settings auto-connect true
/reinodoce status
```

See also: [configuration.md](configuration.md) for the on-disk shape of
every setting.
