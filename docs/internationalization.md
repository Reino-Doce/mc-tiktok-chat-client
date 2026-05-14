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

By default, `language` is `auto`, so the Minecraft client selects the
locale based on the player's language setting and falls back to `en_us`
when a key is missing. Operators can inspect the active language with
`/reinodoce settings language`, set an explicit override with
`/reinodoce settings language <locale>`, or return to automatic
detection with `/reinodoce settings language auto`.

TikTok LIVE connections use the effective language code when requesting
event metadata from TikTok. Fixed synthetic phrases such as joins,
follows, gifts, and member-level messages also use that effective
language. Gift names are preferred from TikTok's localized payload for
the requested language; if TikTok does not provide one, the mod falls
back to the gift name supplied by TikTok or the localized
`reinodoce.chat.gift_unknown` placeholder.

When the effective language changes while a TikTok LIVE session is
active, the client reconnects so TikTok metadata requests use the new
language.

Locale overrides must use Minecraft-style locale codes such as `en_us`,
`pt_br`, or `ja_jp`; `pt-BR` style input is accepted and normalized to
`pt_br`.

## Adding a new locale

1. Copy `en_us.json` to a new file named after the target locale (for
   example `pl_pl.json` for Polish).
2. Translate the values; keep the keys identical.
3. Rebuild — no code change is required.
