package br.com.reinodoce.mctiktok.tiktok;

import br.com.reinodoce.mctiktok.i18n.Translations;
import br.com.reinodoce.mctiktok.util.InlineMediaUrls;
import io.github.jwdeveloper.tiktok.data.models.users.User;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves, caches, and updates TikTok membership levels from badges and member messages.
 */
public class MemberLevelResolver {
    private static final Pattern NUMBER_PATTERN = Pattern.compile("(\\d+)");
    private static final int MIN_FAN_LEVEL = 1;
    private static final int MAX_FAN_LEVEL = 50;

    private final Map<Long, Integer> levels = new ConcurrentHashMap<>();
    private final Map<Long, String> usernames = new ConcurrentHashMap<>();
    private final Map<Long, String> avatarUrls = new ConcurrentHashMap<>();

    /**
     * Resolves a user's best-known membership level from cache or badges.
     *
     * @param user TikTok user model
     * @return resolved level, or zero when unknown
     */
    public int resolveLevel(User user) {
        if (user == null || user.getId() == null) {
            return 0;
        }

        long userId = user.getId();
        int cached = levels.getOrDefault(userId, 0);
        if (cached > 0) {
            return cached;
        }

        usernames.putIfAbsent(userId, chooseUserName(user));
        avatarUrls.putIfAbsent(userId, TikTokMediaResolver.resolveUserAvatarUrl(user));
        return 0;
    }

    /**
     * Applies a membership-level update for a user.
     *
     * @param userId TikTok user id
     * @param username display name from the event
     * @param avatarUrl avatar URL from the event
     * @param newLevel newly observed level
     * @return update summary
     */
    public LevelUpdate updateLevel(long userId, String username, String avatarUrl, long newLevel) {
        if (userId <= 0) {
            return new LevelUpdate(userId, chooseUserName(userId, username), chooseAvatarUrl(userId, avatarUrl), 0, 0);
        }

        String display = chooseUserName(userId, username);
        String displayAvatar = chooseAvatarUrl(userId, avatarUrl);
        int previous = levels.getOrDefault(userId, 0);
        int normalizedLevel = normalizeLevel(newLevel);
        if (normalizedLevel == 0) {
            return new LevelUpdate(userId, display, displayAvatar, previous, previous);
        }

        int effective = Math.max(previous, normalizedLevel);
        levels.put(userId, effective);
        usernames.put(userId, display);
        avatarUrls.put(userId, displayAvatar);
        return new LevelUpdate(userId, display, displayAvatar, previous, effective);
    }

    /**
     * Returns the best-known display name for a user.
     *
     * @param userId TikTok user id
     * @param fallback fallback display name
     * @return cached or fallback display name
     */
    public String getKnownUsername(long userId, String fallback) {
        return chooseUserName(userId, fallback);
    }

    /**
     * Returns the best-known avatar URL for a user.
     *
     * @param userId TikTok user id
     * @param fallback fallback avatar URL
     * @return cached or fallback avatar URL
     */
    public String getKnownAvatarUrl(long userId, String fallback) {
        return chooseAvatarUrl(userId, fallback);
    }

    /**
     * Clears cached levels and identity hints.
     */
    public void clear() {
        levels.clear();
        usernames.clear();
        avatarUrls.clear();
    }

    /**
     * Extracts the first integer member level from TikTok badge text.
     *
     * @param text badge text
     * @return parsed level, or zero when no level is present
     */
    public static int extractLevelFromText(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }

        Matcher matcher = NUMBER_PATTERN.matcher(text);
        if (matcher.find()) {
            try {
                return normalizeLevel(Long.parseLong(matcher.group(1)));
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
    }

    /**
     * Normalizes a raw TikTok fan-club level.
     *
     * @param value raw fan-level candidate
     * @return a valid fan level, or zero when unknown or invalid
     */
    public static int normalizeLevel(long value) {
        return value >= MIN_FAN_LEVEL && value <= MAX_FAN_LEVEL ? (int) value : 0;
    }

    /**
     * Resolves a fan level from fan-club-specific raw user fields.
     *
     * @param rawUser TikTok protobuf user
     * @return resolved fan level, or zero when unknown or invalid
     */
    public static int resolveRawUserLevel(io.github.jwdeveloper.tiktok.messages.data.User rawUser) {
        if (rawUser == null) {
            return 0;
        }
        if (rawUser.hasFansClubInfo()) {
            int fansClubInfoLevel = normalizeLevel(rawUser.getFansClubInfo().getFansLevel());
            if (fansClubInfoLevel > 0) {
                return fansClubInfoLevel;
            }
        }
        if (rawUser.hasFansClub() && rawUser.getFansClub().hasData()) {
            return normalizeLevel(rawUser.getFansClub().getData().getLevel());
        }
        return 0;
    }

    private String chooseUserName(User user) {
        if (user == null) {
            return Translations.tr("reinodoce.chat.user_unknown");
        }
        if (user.getProfileName() != null && !user.getProfileName().isBlank()) {
            return user.getProfileName();
        }
        if (user.getName() != null && !user.getName().isBlank()) {
            return user.getName();
        }
        return Translations.tr("reinodoce.chat.user_unknown");
    }

    private String chooseUserName(long userId, String username) {
        if (username != null && !username.isBlank()) {
            return username;
        }
        return usernames.getOrDefault(userId, Translations.tr("reinodoce.chat.user_unknown"));
    }

    private String chooseAvatarUrl(long userId, String avatarUrl) {
        if (avatarUrl != null && !avatarUrl.isBlank()) {
            return avatarUrl;
        }
        return avatarUrls.getOrDefault(userId, InlineMediaUrls.defaultAvatarUrl());
    }

    /**
     * Summary of a membership-level update.
     *
     * @param userId TikTok user id
     * @param username display name
     * @param avatarUrl avatar URL
     * @param previousLevel previous known level
     * @param newLevel effective stored level
     */
    public record LevelUpdate(long userId, String username, String avatarUrl, int previousLevel, int newLevel) {
        /**
         * Reports whether this update increased an already-known member level.
         *
         * @return true when the new level is greater than a positive previous level
         */
        public boolean isUpgrade() {
            return previousLevel > 0 && newLevel > previousLevel;
        }

        /**
         * Reports whether this update increased the effective known level, including first explicit observations.
         *
         * @return true when the new level is positive and greater than the previous known level
         */
        public boolean isLevelIncrease() {
            return newLevel > 0 && newLevel > previousLevel;
        }
    }
}
