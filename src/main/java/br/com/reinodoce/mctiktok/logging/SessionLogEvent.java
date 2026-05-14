package br.com.reinodoce.mctiktok.logging;

import br.com.reinodoce.mctiktok.chat.MessageSanitizer;
import br.com.reinodoce.mctiktok.i18n.Translations;

/**
 * A mirrored TikTok event accepted for local session logging.
 *
 * @param type stable event type
 * @param username display username, when available
 * @param message display text or message text
 * @param memberLevel member level, or {@code -1} when unavailable
 * @param giftName gift name, when available
 * @param count event count, or {@code 0} when unavailable
 * @param diamonds diamond total, or {@code -1} when unavailable
 */
public record SessionLogEvent(
        String type,
        String username,
        String message,
        int memberLevel,
        String giftName,
        int count,
        long diamonds
) {
    private static final int NO_COUNT = 0;
    private static final int NO_MEMBER_LEVEL = -1;
    private static final long NO_DIAMONDS = -1L;
    private static final String TYPE_CHAT = "chat";
    private static final String TYPE_STAR_COMMENT = "star_comment";
    private static final String TYPE_GIFT = "gift";
    private static final String TYPE_FOLLOW = "follow";
    private static final String TYPE_JOIN = "join";
    private static final String TYPE_MEMBER_LEVEL = "member_level";

    /**
     * Creates a sanitized log event.
     */
    public SessionLogEvent {
        type = safe(type);
        username = safe(username);
        message = MessageSanitizer.sanitize(message);
        giftName = MessageSanitizer.sanitize(giftName);
    }

    /**
     * Creates a normal chat log event.
     *
     * @param username display username
     * @param message chat message
     * @param memberLevel resolved member level
     * @return log event
     */
    public static SessionLogEvent chat(String username, String message, int memberLevel) {
        return comment(TYPE_CHAT, username, message, memberLevel);
    }

    /**
     * Creates a highlighted star-comment log event.
     *
     * @param username display username
     * @param message chat message
     * @param memberLevel resolved member level
     * @return log event
     */
    public static SessionLogEvent starComment(String username, String message, int memberLevel) {
        return comment(TYPE_STAR_COMMENT, username, message, memberLevel);
    }

    /**
     * Creates a synthetic gift log event.
     *
     * @param username display username
     * @param giftName gift name
     * @param count displayed gift count
     * @param diamonds displayed gift diamond total
     * @return log event
     */
    public static SessionLogEvent gift(String username, String giftName, int count, long diamonds) {
        int safeCount = Math.max(1, count);
        long safeDiamonds = Math.max(0L, diamonds);
        return new SessionLogEvent(
                TYPE_GIFT,
                username,
                giftName + " x" + safeCount,
                NO_MEMBER_LEVEL,
                giftName,
                safeCount,
                safeDiamonds);
    }

    /**
     * Creates a synthetic follow log event.
     *
     * @param username display username
     * @return log event
     */
    public static SessionLogEvent follow(String username) {
        return simple(TYPE_FOLLOW, username, Translations.tr("reinodoce.chat.follow"));
    }

    /**
     * Creates a synthetic join log event.
     *
     * @param username display username
     * @return log event
     */
    public static SessionLogEvent join(String username) {
        return simple(TYPE_JOIN, username, Translations.tr("reinodoce.chat.join"));
    }

    /**
     * Creates a synthetic member-level log event.
     *
     * @param username display username
     * @param memberLevel new member level
     * @return log event
     */
    public static SessionLogEvent memberLevel(String username, int memberLevel) {
        int safeMemberLevel = Math.max(0, memberLevel);
        return new SessionLogEvent(
                TYPE_MEMBER_LEVEL,
                username,
                Translations.tr("reinodoce.chat.member_level", safeMemberLevel),
                safeMemberLevel,
                "",
                NO_COUNT,
                NO_DIAMONDS);
    }

    boolean hasMemberLevel() {
        return memberLevel >= 0;
    }

    boolean hasCount() {
        return count > 0;
    }

    boolean hasDiamonds() {
        return diamonds >= 0L;
    }

    private static SessionLogEvent comment(String type, String username, String message, int memberLevel) {
        return new SessionLogEvent(
                type,
                username,
                message,
                Math.max(0, memberLevel),
                "",
                NO_COUNT,
                NO_DIAMONDS);
    }

    private static SessionLogEvent simple(String type, String username, String message) {
        return new SessionLogEvent(type, username, message, NO_MEMBER_LEVEL, "", NO_COUNT, NO_DIAMONDS);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
