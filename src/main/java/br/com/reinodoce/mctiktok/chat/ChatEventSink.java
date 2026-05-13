package br.com.reinodoce.mctiktok.chat;

/**
 * Boundary used by TikTok event handlers to publish already-classified chat events into Minecraft chat.
 */
public interface ChatEventSink {
    /**
     * Sends a plain LIVE comment.
     *
     * @param prefix configured chat prefix
     * @param username display name to show
     * @param message sanitized message body
     */
    void sendLiveComment(String prefix, String username, String message);

    /**
     * Sends a LIVE comment that may contain inline media segments.
     *
     * @param prefix configured chat prefix
     * @param message parsed rich chat message
     */
    void sendLiveComment(String prefix, RichLiveMessage message);

    /**
     * Sends a plain highlighted star comment.
     *
     * @param prefix configured chat prefix
     * @param username display name to show
     * @param message sanitized message body
     */
    void sendStarComment(String prefix, String username, String message);

    /**
     * Sends a highlighted star comment that may contain inline media segments.
     *
     * @param prefix configured chat prefix
     * @param message parsed rich chat message
     */
    void sendStarComment(String prefix, RichLiveMessage message);

    /**
     * Sends a plain synthetic gift message.
     *
     * @param prefix configured chat prefix
     * @param username display name to show
     * @param giftName gift display name
     * @param count gift count or combo count
     */
    void sendSyntheticGift(String prefix, String username, String giftName, int count);

    /**
     * Sends a synthetic gift message that may include gift/avatar media.
     *
     * @param prefix configured chat prefix
     * @param message parsed rich gift message
     */
    void sendSyntheticGift(String prefix, RichLiveMessage message);

    /**
     * Sends a plain synthetic follow message.
     *
     * @param prefix configured chat prefix
     * @param username display name to show
     */
    void sendSyntheticFollow(String prefix, String username);

    /**
     * Sends a synthetic follow message that may include avatar media.
     *
     * @param prefix configured chat prefix
     * @param message parsed rich follow message
     */
    void sendSyntheticFollow(String prefix, RichLiveMessage message);

    /**
     * Sends a plain synthetic join message.
     *
     * @param prefix configured chat prefix
     * @param username display name to show
     */
    void sendSyntheticJoin(String prefix, String username);

    /**
     * Sends a synthetic join message that may include avatar media.
     *
     * @param prefix configured chat prefix
     * @param message parsed rich join message
     */
    void sendSyntheticJoin(String prefix, RichLiveMessage message);

    /**
     * Sends a plain member-level upgrade message.
     *
     * @param prefix configured chat prefix
     * @param username display name to show
     * @param memberLevel resolved member level
     */
    void sendSyntheticMemberLevel(String prefix, String username, int memberLevel);

    /**
     * Sends a member-level upgrade message that may include avatar media.
     *
     * @param prefix configured chat prefix
     * @param message parsed rich member-level message
     * @param memberLevel resolved member level
     */
    void sendSyntheticMemberLevel(String prefix, RichLiveMessage message, int memberLevel);

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
            public void sendLiveComment(String prefix, String username, String message) {
            }

            @Override
            public void sendLiveComment(String prefix, RichLiveMessage message) {
            }

            @Override
            public void sendStarComment(String prefix, String username, String message) {
            }

            @Override
            public void sendStarComment(String prefix, RichLiveMessage message) {
            }

            @Override
            public void sendSyntheticGift(String prefix, String username, String giftName, int count) {
            }

            @Override
            public void sendSyntheticGift(String prefix, RichLiveMessage message) {
            }

            @Override
            public void sendSyntheticFollow(String prefix, String username) {
            }

            @Override
            public void sendSyntheticFollow(String prefix, RichLiveMessage message) {
            }

            @Override
            public void sendSyntheticJoin(String prefix, String username) {
            }

            @Override
            public void sendSyntheticJoin(String prefix, RichLiveMessage message) {
            }

            @Override
            public void sendSyntheticMemberLevel(String prefix, String username, int memberLevel) {
            }

            @Override
            public void sendSyntheticMemberLevel(String prefix, RichLiveMessage message, int memberLevel) {
            }

            @Override
            public void sendSystem(String message, boolean success) {
            }
        };
    }
}
