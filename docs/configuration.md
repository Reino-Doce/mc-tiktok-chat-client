# Configuration

Local client configuration lives at:

```
config/reinodoce-mc-tiktok-client.json
```

(relative to the Minecraft instance — same place every Forge client
stores its config).

The file is read on mod startup and rewritten whenever a setting changes
through `/reinodoce …` or the client settings screen opened with
`/reinodoce settings gui`. That screen now opens to a compact domain
index and persists the shared draft only when you press Done on the
top-level screen. You can also edit the file by hand and run
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
| `ruleEmoteOnlyFilterEnabled` | boolean | `false` | When enabled, hides comments whose body has no letters or digits, including inline-media-only comments. |
| `ruleLinkFilterEnabled` | boolean | `false` | When enabled, hides comments containing `http://`, `https://`, `www.`, or domain-like URLs. |
| `ruleUserCooldownSeconds` | integer | `0` | Per-user comment cooldown in seconds. `0` disables. Clamped to >= 0. |
| `ruleAllowlistMode` | boolean | `false` | When enabled, only usernames in `ruleAllowedUsers` pass comment and synthetic user routing. |
| `ruleAllowedUsers` | string array | `[]` | Case-insensitive exact TikTok usernames allowed while allowlist mode is enabled. Invalid usernames are discarded on load. |
| `ruleMaxMessageLength` | integer | `0` | Maximum accepted message length. `0` disables. Clamped to ≥ 0. |
| `ruleDuplicateCooldownSeconds` | integer | `0` | Duplicate text suppression window in seconds. `0` disables. Clamped to ≥ 0. |
| `syntheticGiftMinValue` | integer | `1` | Minimum gift diamond cost to surface. `0` disables; `1` surfaces all. Clamped to ≥ 0. |
| `syntheticGiftComboMode` | string | `"bulk"` | One of `"ignore"`, `"single"`, `"bulk"`. Unknown values fall back to `"bulk"`. |
| `syntheticFollowEnabled` | boolean | `false` | Surface new-follower events. |
| `syntheticJoinEnabled` | boolean | `false` | Surface viewer-join events. |
| `syntheticMemberLevelEnabled` | boolean | `false` | Surface member-level-up events. |
| `alertGiftSoundEnabled` | boolean | `false` | Play a local client-only sound for surfaced gifts that pass `alertGiftMinValue`. |
| `alertGiftSoundId` | string | `"default"` | Sound used for enabled gift sound alerts. `"default"` maps to `minecraft:ui.toast.in`; custom values must be namespaced Minecraft resource ids such as `minecraft:entity.experience_orb.pickup`. Malformed values fall back to `"default"` on load; unavailable sounds fall back to the default at playback. |
| `alertGiftToastEnabled` | boolean | `false` | Show a local client-only toast for surfaced gifts that pass `alertGiftMinValue`. |
| `alertGiftToastTemplate` | string | `""` | Gift toast message template. Blank means built-in localized text. Supported tokens: `{username}`, `{giftName}`, `{count}`, `{diamonds}`, `{memberLevel}`, `{profileImage}`, `{giftImage}`. Unknown tokens fall back to blank on load. |
| `alertGiftMediaMode` | string | `"none"` | Gift toast media mode. One of `"none"`, `"profile"`, `"custom"`, `"profile-custom"`, `"gift"`, or `"inline"`. `"gift"` uses the TikTok gift image when available. Invalid values fall back to `"none"`. |
| `alertGiftCustomImage` | string | `""` | Custom image reference for gift toasts. Blank or `"default"` clears it. Supports `https://...`, `resource://namespace/path`, or a namespaced resource id such as `reinodoce_mctiktok:textures/gui/no_user_image.png`. Missing or failed media falls back safely. |
| `alertGiftMinValue` | integer | `1` | Minimum single-gift diamond value for gift alerts. Clamped to >= 0. |
| `alertFollowSoundEnabled` | boolean | `false` | Play a local sound for surfaced follow events. |
| `alertFollowSoundId` | string | `"default"` | Sound used for enabled follow sound alerts. Malformed values fall back to `"default"` on load; unavailable sounds fall back to the default at playback. |
| `alertFollowToastEnabled` | boolean | `false` | Show a local toast for surfaced follow events. |
| `alertFollowToastTemplate` | string | `""` | Follow toast message template. Blank means built-in localized text. |
| `alertFollowMediaMode` | string | `"none"` | Follow toast media mode. `"profile"` uses the TikTok profile image when available. Invalid values fall back to `"none"`. |
| `alertFollowCustomImage` | string | `""` | Custom image reference for follow toasts. Blank or `"default"` clears it. |
| `alertJoinSoundEnabled` | boolean | `false` | Play a local sound for surfaced join events. Join alerts are throttled. |
| `alertJoinSoundId` | string | `"default"` | Sound used for enabled join sound alerts. Malformed values fall back to `"default"` on load; unavailable sounds fall back to the default at playback. |
| `alertJoinToastEnabled` | boolean | `false` | Show a local toast for surfaced join events. Join alerts are throttled. |
| `alertJoinToastTemplate` | string | `""` | Join toast message template. Blank means built-in localized text. |
| `alertJoinMediaMode` | string | `"none"` | Join toast media mode. Invalid values fall back to `"none"`. |
| `alertJoinCustomImage` | string | `""` | Custom image reference for join toasts. Blank or `"default"` clears it. |
| `alertMemberLevelSoundEnabled` | boolean | `false` | Play a local sound for surfaced member-level-up events. |
| `alertMemberLevelSoundId` | string | `"default"` | Sound used for enabled member-level sound alerts. Malformed values fall back to `"default"` on load; unavailable sounds fall back to the default at playback. |
| `alertMemberLevelToastEnabled` | boolean | `false` | Show a local toast for surfaced member-level-up events. |
| `alertMemberLevelToastTemplate` | string | `""` | Member-level toast message template. Blank means built-in localized text. |
| `alertMemberLevelMediaMode` | string | `"none"` | Member-level toast media mode. Invalid values fall back to `"none"`. |
| `alertMemberLevelCustomImage` | string | `""` | Custom image reference for member-level toasts. Blank or `"default"` clears it. |
| `outputMode` | string | `"chat"` | One of `"chat"`, `"actionbar"`, `"hud"`, or `"off"`. Unknown values fall back to `"chat"`. |
| `hudPosition` | string | `"top-left"` | One of `"top-left"`, `"top-right"`, `"bottom-left"`, or `"bottom-right"`. Unknown values fall back to `"top-left"`. |
| `hudLines` | integer | `6` | Maximum retained HUD lines. Clamped to 1–12. |
| `pinnedOverlayEnabled` | boolean | `true` | Render TikTok pinned-message events in a dedicated local screen overlay. The overlay is client-only and does not send packets or chat messages. |
| `pinnedOverlayPosition` | string | `"top-right"` | One of `"top-left"`, `"top-right"`, `"bottom-left"`, or `"bottom-right"`. Unknown values fall back to `"top-left"`. |
| `pinnedMessagesInOutput` | boolean | `false` | When `true`, pinned-message events also appear in the normal mirrored output selected by `outputMode`. |
| `pinnedOverlayMessages` | integer | `3` | Maximum visible pinned-message overlay entries. Clamped to 1–6. Pinned messages expire after TikTok's supplied display duration when present, otherwise after a short local fallback. |
| `burstControlEnabled` | boolean | `false` | Enables visible-output burst controls. Disabled preserves the existing output behavior. |
| `burstCommentsPerSecond` | integer | `0` | Maximum visible chat comments per second for each output route. `0` disables comment rate limiting. Clamped to 0–60. Accepted comments still update stats and session logs. |
| `burstSyntheticAggregationSeconds` | integer | `0` | Follow/join aggregation window in seconds. `0` disables aggregation. Clamped to 0–60. |
| `chatPrefix` | string | `"[LIVE]"` | Prefix prepended to every mirrored line. Empty / blank values revert to the default. |
| `chatFormat` | string | current layout | Template used to arrange mirrored lines. Must include `{prefix}`, `{username}`, and `{message}`; unknown tokens or blank values revert to the default. |
| `chatEmotesEnabled` | boolean | `true` | Render TikTok chat emotes inline in Minecraft chat. |
| `chatLogEnabled` | boolean | `false` | Write mirrored TikTok chat and synthetic event lines through Minecraft's chat logger. System/status/error lines remain logged. |
| `maskUsernamesInOutput` | boolean | `false` | Mask TikTok usernames in mirrored chat, actionbar, HUD, and pinned overlay output. |
| `language` | string | `"auto"` | `auto` follows the Minecraft client language. A locale such as `"pt_br"` overrides the language used for TikTok metadata requests and fixed synthetic phrases. Invalid values fall back to `"auto"` on load. |
| `sessionLoggingEnabled` | boolean | `false` | Write accepted mirrored LIVE events to local session files under `logs/reinodoce/`. Opt-in because files may contain TikTok usernames and chat content. |
| `sessionLoggingFormat` | string | `"jsonl"` | One of `"jsonl"` or `"text"`. Unknown values fall back to `"jsonl"`. |
| `sessionLoggingRetentionDays` | integer | `0` | Delete this mod's own session log files older than this many days when a new session log opens. `0` disables age cleanup. |
| `sessionLoggingRetentionFiles` | integer | `0` | Keep at most this many session log files after a new session log opens. `0` disables count cleanup. |
| `sessionLoggingAnonymized` | boolean | `false` | Mask username fields in session log files. |
| `sessionLoggingMetadataOnly` | boolean | `false` | Omit message body fields from session log files while preserving event metadata. |

Commands reject malformed values before saving. When the file is edited
by hand, the config loader sanitizes malformed values to defaults so the
client can keep starting safely.

`chatFormat` supports exactly these tokens:

- `{prefix}` — configured LIVE prefix.
- `{username}` — TikTok display username, including avatar / emoji media
  when inline rendering is enabled.
- `{message}` — chat body or synthetic event body, including inline
  emotes and gift icons when enabled.

See [commands.md](commands.md) for the command that drives each field.

Alert toast templates use the same brace-token style. Supported tokens
are `{username}`, `{giftName}`, `{count}`, `{diamonds}`, `{memberLevel}`,
`{profileImage}`, and `{giftImage}`. Tokens that are not relevant to the
current event render as blank. Default settings preserve the standard
text toast behavior; media modes render the selected profile, gift, or
custom image when available and fall back to safe placeholders on failed
loads.

## Session log files

When `sessionLoggingEnabled` is `true`, each successful connection opens
one local file under the Minecraft instance's `logs/reinodoce/`
directory. JSONL files contain one object per line with stable fields such
as `type`, `timestamp`, `username`, `message`, `memberLevel`, `giftName`,
`count`, and `diamonds` when those values are available. Text logs use the
same fields as tab-separated key-value pairs.

Privacy controls are opt-in and preserve existing behavior by default.
`sessionLoggingAnonymized` masks session log username fields,
`sessionLoggingMetadataOnly` omits message body fields, and retention
cleanup only deletes this mod's own non-recursive `live-*.jsonl` and
`live-*.txt` files in `logs/reinodoce/`. The `maskUsernamesInOutput`
setting affects local mirrored output surfaces, not command or status
feedback.
