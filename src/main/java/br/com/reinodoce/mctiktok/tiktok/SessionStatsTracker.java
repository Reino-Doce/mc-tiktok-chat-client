package br.com.reinodoce.mctiktok.tiktok;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Thread-safe counters for the currently connected TikTok LIVE session.
 */
public class SessionStatsTracker {
    private static final String EMPTY = "";
    private static final long NO_DIAMONDS = 0L;
    private static final long MISSING_USER_ID = 0L;

    private final Set<String> uniqueChatters = new HashSet<>();
    private final Map<String, Long> diamondsByGifter = new HashMap<>();

    private long messages;
    private long follows;
    private long joins;
    private long gifts;
    private long diamonds;
    private long memberLevels;

    /**
     * Records an accepted chat comment.
     *
     * @param userId stable TikTok user id, or zero when unavailable
     * @param username display username fallback
     */
    public synchronized void recordComment(long userId, String username) {
        messages++;
        uniqueChatters.add(chatterKey(userId, username));
    }

    /**
     * Records an accepted follow event.
     */
    public synchronized void recordFollow() {
        follows++;
    }

    /**
     * Records an accepted join event.
     */
    public synchronized void recordJoin() {
        joins++;
    }

    /**
     * Records an accepted gift event.
     *
     * @param username gifter display username
     * @param count gift count
     * @param diamondCost diamond value per gift
     */
    public synchronized void recordGift(String username, int count, int diamondCost) {
        int safeCount = Math.max(1, count);
        long giftDiamonds = (long) Math.max(0, diamondCost) * safeCount;
        gifts += safeCount;
        diamonds += giftDiamonds;
        if (giftDiamonds > NO_DIAMONDS) {
            diamondsByGifter.merge(sanitizeStatName(username), giftDiamonds, Long::sum);
        }
    }

    /**
     * Records an accepted member-level event.
     */
    public synchronized void recordMemberLevel() {
        memberLevels++;
    }

    /**
     * Clears all counters for a new or manually reset session.
     */
    public synchronized void reset() {
        messages = 0L;
        follows = 0L;
        joins = 0L;
        gifts = 0L;
        diamonds = 0L;
        memberLevels = 0L;
        uniqueChatters.clear();
        diamondsByGifter.clear();
    }

    /**
     * Returns an immutable snapshot of the current counters.
     *
     * @return stats snapshot
     */
    public synchronized Snapshot snapshot() {
        TopGifter topGifter = topGifter();
        return new Snapshot(
                messages,
                uniqueChatters.size(),
                follows,
                joins,
                gifts,
                diamonds,
                memberLevels,
                topGifter.username(),
                topGifter.diamonds()
        );
    }

    private TopGifter topGifter() {
        String topUsername = EMPTY;
        long topDiamonds = NO_DIAMONDS;
        for (Map.Entry<String, Long> entry : diamondsByGifter.entrySet()) {
            if (entry.getValue() > topDiamonds) {
                topUsername = entry.getKey();
                topDiamonds = entry.getValue();
            }
        }
        return new TopGifter(topUsername, topDiamonds);
    }

    private static String chatterKey(long userId, String username) {
        if (userId > MISSING_USER_ID) {
            return "id:" + userId;
        }
        return "name:" + sanitizeStatName(username).toLowerCase(Locale.ROOT);
    }

    private static String sanitizeStatName(String username) {
        return TikTokUserNames.sanitizeUserName(username);
    }

    private record TopGifter(String username, long diamonds) {
    }

    /**
     * Immutable stats snapshot.
     *
     * @param messages accepted chat message count
     * @param uniqueChatters unique accepted chatters
     * @param follows accepted follow count
     * @param joins accepted join count
     * @param gifts accepted gift item count
     * @param diamonds accepted gift diamond total
     * @param memberLevels accepted member-level event count
     * @param topGifter top gifter username, or blank
     * @param topGifterDiamonds top gifter diamond total
     */
    public record Snapshot(
            long messages,
            int uniqueChatters,
            long follows,
            long joins,
            long gifts,
            long diamonds,
            long memberLevels,
            String topGifter,
            long topGifterDiamonds
    ) {
    }
}
