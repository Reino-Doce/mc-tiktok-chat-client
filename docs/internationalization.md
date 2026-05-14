# Internationalization

The mod ships language bundles under
`src/main/resources/assets/reinodoce_mctiktok/lang/`, aligned with the
largest TikTok LIVE gaming-streamer markets:

| Locale  | Language                       |
| ------- | ------------------------------ |
| `en_us` | English (baseline / fallback)  |
| `pt_br` | Brazilian Portuguese           |
| `id_id` | Indonesian                     |
| `es_mx` | Spanish (Mexico / LATAM)       |
| `vi_vn` | Vietnamese                     |
| `th_th` | Thai                           |
| `fil_ph`| Filipino                       |
| `tr_tr` | Turkish                        |
| `ja_jp` | Japanese                       |
| `fr_fr` | French                         |
| `de_de` | German                         |
| `ar_sa` | Arabic                         |

The Minecraft client selects the locale automatically based on the
player's language setting and falls back to `en_us` when a key is
missing.

TikTok LIVE connections also use the selected Minecraft language code
when requesting event metadata from TikTok. Fixed synthetic phrases such
as joins, follows, gifts, and member-level messages come from the mod's
Minecraft lang files. Gift names are preferred from TikTok's localized
payload for the requested language; if TikTok does not provide one, the
mod falls back to the gift name supplied by TikTok or the localized
`reinodoce.chat.gift_unknown` placeholder.

## Adding a new locale

1. Copy `en_us.json` to a new file named after the target locale (for
   example `pl_pl.json` for Polish).
2. Translate the values; keep the keys identical.
3. Rebuild — no code change is required.
