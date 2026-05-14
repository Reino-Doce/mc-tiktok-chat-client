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

Legacy config files that still use the old `syntetic...` spelling are
accepted on load. The config writer stores both the corrected `synthetic...`
names and the legacy `syntetic...` names so users can downgrade without
losing synthetic-event settings.

## Fields

| Field | Type | Default | Notes |
| ----- | ---- | ------- | ----- |
| `lastUsername` | string | `""` | Last `@username` successfully started through `/reinodoce connect`. Trimmed, never null. |
| `reconnectSeconds` | integer | `5` | Reconnect delay in seconds. `0` disables reconnect. Clamped to ≥ 0. |
| `ruleFollowerOnly` | boolean | `false` | Mirror only comments from followers of the streamer. |
| `ruleMinMemberLevel` | integer | `0` | Minimum LIVE member level required. `0` disables. Clamped to ≥ 0. |
| `syntheticGiftMinValue` | integer | `1` | Minimum gift diamond cost to surface. `0` disables; `1` surfaces all. Clamped to ≥ 0. |
| `syntheticGiftComboMode` | string | `"bulk"` | One of `"ignore"`, `"single"`, `"bulk"`. Unknown values fall back to `"bulk"`. |
| `syntheticFollowEnabled` | boolean | `false` | Surface new-follower events. |
| `syntheticJoinEnabled` | boolean | `false` | Surface viewer-join events. |
| `syntheticMemberLevelEnabled` | boolean | `false` | Surface member-level-up events. |
| `chatPrefix` | string | `"[LIVE]"` | Prefix prepended to every mirrored line. Empty / blank values revert to the default. |
| `chatFormat` | string | current layout | Template used to arrange mirrored lines. Must include `{prefix}`, `{username}`, and `{message}`; unknown tokens or blank values revert to the default. |
| `chatEmotesEnabled` | boolean | `true` | Render TikTok chat emotes inline in Minecraft chat. |
| `chatLogEnabled` | boolean | `false` | Write mirrored TikTok chat and synthetic event lines through Minecraft's chat logger. System/status/error lines remain logged. |

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
