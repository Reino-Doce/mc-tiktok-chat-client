package br.com.reinodoce.mctiktok.chat;

import br.com.reinodoce.mctiktok.config.ReinodoceConfig;

/**
 * Boundary used by TikTok event handlers to publish already-classified chat events into Minecraft chat.
 */
public interface ChatEventSink {
    /**
     * Sends a plain LIVE comment.
     *
     * @param config runtime configuration snapshot
     * @param username display name to show
     * @param message sanitized message body
     */
    void sendLiveComment(ReinodoceConfig config, String username, String message);

    /**
     * Sends a LIVE comment that may contain inline media segments.
     *
     * @param config runtime configuration snapshot
     * @param message parsed rich chat message
     */
    void sendLiveComment(ReinodoceConfig config, RichLiveMessage message);

    /**
     * Sends a plain highlighted star comment.
     *
     * @param config runtime configuration snapshot
     * @param username display name to show
     * @param message sanitized message body
     */
    void sendStarComment(ReinodoceConfig config, String username, String message);

    /**
     * Sends a highlighted star comment that may contain inline media segments.
     *
     * @param config runtime configuration snapshot
     * @param message parsed rich chat message
     */
    void sendStarComment(ReinodoceConfig config, RichLiveMessage message);

    /**
     * Sends a plain synthetic gift message.
     *
     * @param config runtime configuration snapshot
     * @param username display name to show
     * @param giftName gift display name
     * @param count gift count or combo count
     */
    void sendSyntheticGift(ReinodoceConfig config, String username, String giftName, int count);

    /**
     * Sends a synthetic gift message that may include gift/avatar media.
     *
     * @param config runtime configuration snapshot
     * @param message parsed rich gift message
     */
    void sendSyntheticGift(ReinodoceConfig config, RichLiveMessage message);

    /**
     * Sends a plain synthetic follow message.
     *
     * @param config runtime configuration snapshot
     * @param username display name to show
     */
    void sendSyntheticFollow(ReinodoceConfig config, String username);

    /**
     * Sends a synthetic follow message that may include avatar media.
     *
     * @param config runtime configuration snapshot
     * @param message parsed rich follow message
     */
    void sendSyntheticFollow(ReinodoceConfig config, RichLiveMessage message);

    /**
     * Sends a plain synthetic join message.
     *
     * @param config runtime configuration snapshot
     * @param username display name to show
     */
    void sendSyntheticJoin(ReinodoceConfig config, String username);

    /**
     * Sends a synthetic join message that may include avatar media.
     *
     * @param config runtime configuration snapshot
     * @param message parsed rich join message
     */
    void sendSyntheticJoin(ReinodoceConfig config, RichLiveMessage message);

    /**
     * Sends a plain member-level upgrade message.
     *
     * @param config runtime configuration snapshot
     * @param username display name to show
     * @param memberLevel resolved member level
     */
    void sendSyntheticMemberLevel(ReinodoceConfig config, String username, int memberLevel);

    /**
     * Sends a member-level upgrade message that may include avatar media.
     *
     * @param config runtime configuration snapshot
     * @param message parsed rich member-level message
     * @param memberLevel resolved member level
     */
    void sendSyntheticMemberLevel(ReinodoceConfig config, RichLiveMessage message, int memberLevel);

    /**
     * Sends an operator-facing system line.
     *
     * @param message status or error text
     * @param success whether the line should be styled as successful
     */
    void sendSystem(String message, boolean success);

    /**
     * Creates a sink that intentionally drops all events.
     *
     * @return no-op sink
     */
    static ChatEventSink noop() {
        return new ChatEventSink() {
            @Override
            public void sendLiveComment(ReinodoceConfig config, String username, String message) {
            }

            @Override
            public void sendLiveComment(ReinodoceConfig config, RichLiveMessage message) {
            }

            @Override
            public void sendStarComment(ReinodoceConfig config, String username, String message) {
            }

            @Override
            public void sendStarComment(ReinodoceConfig config, RichLiveMessage message) {
            }

            @Override
            public void sendSyntheticGift(ReinodoceConfig config, String username, String giftName, int count) {
            }

            @Override
            public void sendSyntheticGift(ReinodoceConfig config, RichLiveMessage message) {
            }

            @Override
            public void sendSyntheticFollow(ReinodoceConfig config, String username) {
            }

            @Override
            public void sendSyntheticFollow(ReinodoceConfig config, RichLiveMessage message) {
            }

            @Override
            public void sendSyntheticJoin(ReinodoceConfig config, String username) {
            }

            @Override
            public void sendSyntheticJoin(ReinodoceConfig config, RichLiveMessage message) {
            }

            @Override
            public void sendSyntheticMemberLevel(ReinodoceConfig config, String username, int memberLevel) {
            }

            @Override
            public void sendSyntheticMemberLevel(ReinodoceConfig config, RichLiveMessage message, int memberLevel) {
            }

            @Override
            public void sendSystem(String message, boolean success) {
            }
        };
    }
}
