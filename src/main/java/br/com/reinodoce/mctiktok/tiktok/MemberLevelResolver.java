package br.com.reinodoce.mctiktok.tiktok;

import br.com.reinodoce.mctiktok.util.InlineMediaUrls;
import io.github.jwdeveloper.tiktok.data.models.badges.Badge;
import io.github.jwdeveloper.tiktok.data.models.badges.CombineBadge;
import io.github.jwdeveloper.tiktok.data.models.badges.StringBadge;
import io.github.jwdeveloper.tiktok.data.models.badges.TextBadge;
import io.github.jwdeveloper.tiktok.data.models.users.User;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MemberLevelResolver {
    private static final Pattern NUMBER_PATTERN = Pattern.compile("(\\d+)");

    private final Map<Long, Integer> levels = new ConcurrentHashMap<>();
    private final Map<Long, String> usernames = new ConcurrentHashMap<>();
    private final Map<Long, String> avatarUrls = new ConcurrentHashMap<>();

    public int resolveLevel(User user) {
        if (user == null || user.getId() == null) {
            return 0;
        }

        long userId = user.getId();
        int cached = levels.getOrDefault(userId, 0);
        if (cached > 0) {
            return cached;
        }

        int inferred = inferFromBadges(user.getBadges());
        if (inferred > 0) {
            levels.put(userId, inferred);
        }
        usernames.putIfAbsent(userId, chooseUserName(user));
        avatarUrls.putIfAbsent(userId, TikTokMediaResolver.resolveUserAvatarUrl(user));
        return inferred;
    }

    public LevelUpdate updateLevel(long userId, String username, String avatarUrl, int newLevel) {
        if (userId <= 0 || newLevel < 0) {
            return new LevelUpdate(userId, chooseUserName(userId, username), chooseAvatarUrl(userId, avatarUrl), 0, 0);
        }

        String display = chooseUserName(userId, username);
        String displayAvatar = chooseAvatarUrl(userId, avatarUrl);
        int previous = levels.getOrDefault(userId, 0);
        int effective = Math.max(previous, newLevel);

        levels.put(userId, effective);
        usernames.put(userId, display);
        avatarUrls.put(userId, displayAvatar);
        return new LevelUpdate(userId, display, displayAvatar, previous, effective);
    }

    public String getKnownUsername(long userId, String fallback) {
        return chooseUserName(userId, fallback);
    }

    public String getKnownAvatarUrl(long userId, String fallback) {
        return chooseAvatarUrl(userId, fallback);
    }

    public void clear() {
        levels.clear();
        usernames.clear();
        avatarUrls.clear();
    }

    public static int extractLevelFromText(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }

        Matcher matcher = NUMBER_PATTERN.matcher(text);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
    }

    private int inferFromBadges(List<Badge> badges) {
        if (badges == null || badges.isEmpty()) {
            return 0;
        }

        int maxLevel = 0;
        for (Badge badge : badges) {
            int level = 0;
            if (badge instanceof TextBadge textBadge) {
                level = extractLevelFromText(textBadge.getText());
            } else if (badge instanceof StringBadge stringBadge) {
                level = extractLevelFromText(stringBadge.getText());
            } else if (badge instanceof CombineBadge combineBadge) {
                level = Math.max(
                        extractLevelFromText(combineBadge.getText()),
                        extractLevelFromText(combineBadge.getSubText())
                );
            }
            if (level > maxLevel) {
                maxLevel = level;
            }
        }
        return maxLevel;
    }

    private String chooseUserName(User user) {
        if (user == null) {
            return "desconhecido";
        }
        if (user.getProfileName() != null && !user.getProfileName().isBlank()) {
            return user.getProfileName();
        }
        if (user.getName() != null && !user.getName().isBlank()) {
            return user.getName();
        }
        return "desconhecido";
    }

    private String chooseUserName(long userId, String username) {
        if (username != null && !username.isBlank()) {
            return username;
        }
        return usernames.getOrDefault(userId, "desconhecido");
    }

    private String chooseAvatarUrl(long userId, String avatarUrl) {
        if (avatarUrl != null && !avatarUrl.isBlank()) {
            return avatarUrl;
        }
        return avatarUrls.getOrDefault(userId, InlineMediaUrls.defaultAvatarUrl());
    }

    public record LevelUpdate(long userId, String username, String avatarUrl, int previousLevel, int newLevel) {
        public boolean isUpgrade() {
            return previousLevel > 0 && newLevel > previousLevel;
        }
    }
}
