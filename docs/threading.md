# Threading, robustness, and the `[LIVE]` prefix

## Threading

- The TikTok integration runs **off the game thread**: socket I/O, event
  parsing, and reconnect scheduling never block the Minecraft client tick.
- Mirrored chat lines are always dispatched from the client's safe
  context, the same path used by vanilla chat rendering.
- Reconnect uses a dedicated scheduler so connect / disconnect /
  reconnect cycles cannot stack on the main thread.

## Robustness

- Local reconnect telemetry (attempt counter, visible in
  `/reinodoce status`).
- A lifecycle token prevents stale callbacks from a previous session
  from affecting the current one.
- Repeated `connect` / `disconnect` calls — and active username swaps
  — are guarded so that overlapping commands do not corrupt state.
- Error and reconnect chat warnings are throttled to reduce flooding
  when the LIVE keeps dropping.

## The `[LIVE]` prefix

This mod does **not** try to impersonate a real player on the server.
Every mirrored TikTok message is rendered with the configurable
`chatPrefix` (default `[LIVE]`) so the external origin stays
identifiable in the Minecraft chat HUD.

The prefix is a client-side render decoration — no network packet is
sent to the server. See [configuration.md](configuration.md) for how
to change it.

## Inline media bounds

Remote inline media is fetched only over HTTPS from public remote
addresses, must use a supported raster image content type, and is
bounded before rendering. The client rejects same-host redirects to
private, loopback, link-local, multicast, or otherwise non-public
addresses, oversized responses, and images whose decoded dimensions
exceed the configured pixel limit. Cached media is also cleaned up by
age and disk quota.
