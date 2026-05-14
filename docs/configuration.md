# Configuration

Local client configuration lives at:

```
config/reinodoce-mc-tiktok-client.json
```

(relative to the Minecraft instance — same place every Forge client
stores its config).

The file is read on mod startup and rewritten whenever a setting changes
through `/reinodoce …`. You can also edit it by hand and run
`/reinodoce reload` to pick up the changes without restarting the client.

## Fields

| Field | Type | Default | Notes |
| ----- | ---- | ------- | ----- |
| `lastUsername` | string | `""` | Last `@username` successfully started through `/reinodoce connect`. Trimmed, never null. |
| `autoConnectOnStart` | boolean | `false` | When enabled, startup attempts one connection to `lastUsername` if it is non-blank and valid. |
| `reconnectSeconds` | integer | `5` | Reconnect delay in seconds. `0` disables reconnect. Clamped to ≥ 0. |
| `ruleFollowerOnly` | boolean | `false` | Mirror only comments from followers of the streamer. |
| `ruleMinMemberLevel` | integer | `0` | Minimum LIVE member level required. `0` disables. Clamped to ≥ 0. |
| `ruleBlockedWords` | string array | `[]` | Case-insensitive blocked word fragments. Comments containing a fragment are hidden. |
| `ruleBlockedUsers` | string array | `[]` | Case-insensitive exact TikTok usernames to hide. Invalid usernames are discarded on load. |
| `ruleMaxMessageLength` | integer | `0` | Maximum accepted message length. `0` disables. Clamped to ≥ 0. |
| `ruleDuplicateCooldownSeconds` | integer | `0` | Duplicate text suppression window in seconds. `0` disables. Clamped to ≥ 0. |
| `syntheticGiftMinValue` | integer | `1` | Minimum gift diamond cost to surface. `0` disables; `1` surfaces all. Clamped to ≥ 0. |
| `syntheticGiftComboMode` | string | `"bulk"` | One of `"ignore"`, `"single"`, `"bulk"`. Unknown values fall back to `"bulk"`. |
| `syntheticFollowEnabled` | boolean | `false` | Surface new-follower events. |
| `syntheticJoinEnabled` | boolean | `false` | Surface viewer-join events. |
| `syntheticMemberLevelEnabled` | boolean | `false` | Surface member-level-up events. |
| `alertGiftSoundEnabled` | boolean | `false` | Play a local client-only sound for surfaced gifts that pass `alertGiftMinValue`. |
| `alertGiftToastEnabled` | boolean | `false` | Show a local client-only toast for surfaced gifts that pass `alertGiftMinValue`. |
| `alertGiftMinValue` | integer | `1` | Minimum single-gift diamond value for gift alerts. Clamped to >= 0. |
| `alertFollowSoundEnabled` | boolean | `false` | Play a local sound for surfaced follow events. |
| `alertFollowToastEnabled` | boolean | `false` | Show a local toast for surfaced follow events. |
| `alertJoinSoundEnabled` | boolean | `false` | Play a local sound for surfaced join events. Join alerts are throttled. |
| `alertJoinToastEnabled` | boolean | `false` | Show a local toast for surfaced join events. Join alerts are throttled. |
| `alertMemberLevelSoundEnabled` | boolean | `false` | Play a local sound for surfaced member-level-up events. |
| `alertMemberLevelToastEnabled` | boolean | `false` | Show a local toast for surfaced member-level-up events. |
| `outputMode` | string | `"chat"` | One of `"chat"`, `"actionbar"`, `"hud"`, or `"off"`. Unknown values fall back to `"chat"`. |
| `hudPosition` | string | `"top-left"` | One of `"top-left"`, `"top-right"`, `"bottom-left"`, or `"bottom-right"`. Unknown values fall back to `"top-left"`. |
| `hudLines` | integer | `6` | Maximum retained HUD lines. Clamped to 1–12. |
| `chatPrefix` | string | `"[LIVE]"` | Prefix prepended to every mirrored line. Empty / blank values revert to the default. |
| `chatFormat` | string | current layout | Template used to arrange mirrored lines. Must include `{prefix}`, `{username}`, and `{message}`; unknown tokens or blank values revert to the default. |
| `chatEmotesEnabled` | boolean | `true` | Render TikTok chat emotes inline in Minecraft chat. |
| `chatLogEnabled` | boolean | `false` | Write mirrored TikTok chat and synthetic event lines through Minecraft's chat logger. System/status/error lines remain logged. |
| `sessionLoggingEnabled` | boolean | `false` | Write accepted mirrored LIVE events to local session files under `logs/reinodoce/`. Opt-in because files may contain TikTok usernames and chat content. |
| `sessionLoggingFormat` | string | `"jsonl"` | One of `"jsonl"` or `"text"`. Unknown values fall back to `"jsonl"`. |

All commands that change a field validate and clamp the same way the
config loader does, so editing the file by hand and editing through
`/reinodoce …` produce identical state.

`chatFormat` supports exactly these tokens:

- `{prefix}` — configured LIVE prefix.
- `{username}` — TikTok display username, including avatar / emoji media
  when inline rendering is enabled.
- `{message}` — chat body or synthetic event body, including inline
  emotes and gift icons when enabled.

See [commands.md](commands.md) for the command that drives each field.

## Session log files

When `sessionLoggingEnabled` is `true`, each successful connection opens
one local file under the Minecraft instance's `logs/reinodoce/`
directory. JSONL files contain one object per line with stable fields such
as `type`, `timestamp`, `username`, `message`, `memberLevel`, `giftName`,
`count`, and `diamonds` when those values are available. Text logs use the
same fields as tab-separated key-value pairs.
