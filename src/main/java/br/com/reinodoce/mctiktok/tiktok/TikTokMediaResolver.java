package br.com.reinodoce.mctiktok.tiktok;

import br.com.reinodoce.mctiktok.chat.MessageSanitizer;
import br.com.reinodoce.mctiktok.util.InlineMediaUrls;
import io.github.jwdeveloper.tiktok.data.models.Picture;
import io.github.jwdeveloper.tiktok.data.models.gifts.Gift;
import io.github.jwdeveloper.tiktok.data.models.users.User;
import io.github.jwdeveloper.tiktok.messages.data.Image;

public final class TikTokMediaResolver {
    private TikTokMediaResolver() {
    }

    public static String defaultAvatarUrl() {
        return InlineMediaUrls.defaultAvatarUrl();
    }

    public static String resolveUserAvatarUrl(User user) {
        String resolved = resolvePictureUrl(user == null ? null : user.getPicture());
        return resolved.isBlank() ? defaultAvatarUrl() : resolved;
    }

    public static String resolveUserAvatarUrl(io.github.jwdeveloper.tiktok.messages.data.User user) {
        if (user == null) {
            return defaultAvatarUrl();
        }
        String resolved = firstNonBlank(
                user.hasAvatarLarge() ? resolveImageUrl(user.getAvatarLarge()) : "",
                user.hasAvatarMedium() ? resolveImageUrl(user.getAvatarMedium()) : "",
                user.hasAvatarThumb() ? resolveImageUrl(user.getAvatarThumb()) : "",
                user.hasAvatarJpg() ? resolveImageUrl(user.getAvatarJpg()) : "");
        return resolved.isBlank() ? defaultAvatarUrl() : resolved;
    }

    private static String firstNonBlank(String... candidates) {
        for (String candidate : candidates) {
            if (!candidate.isBlank()) {
                return candidate;
            }
        }
        return "";
    }

    public static String resolveGiftIconUrl(Gift gift) {
        if (gift == null) {
            return "";
        }
        return resolvePictureUrl(gift.getPicture());
    }

    public static String resolvePictureUrl(Picture picture) {
        if (picture == null) {
            return "";
        }
        return normalizeUrl(picture.getLink());
    }

    public static String resolveImageUrl(Image image) {
        if (image == null || image.getUrlCount() <= 0) {
            return "";
        }
        return normalizeUrl(image.getUrl(image.getUrlCount() - 1));
    }

    private static String normalizeUrl(String raw) {
        String sanitized = MessageSanitizer.sanitize(raw);
        if (sanitized.isBlank()) {
            return "";
        }
        return sanitized
                .replace("-sign-", "-")
                .replace("-sign.", ".");
    }
}
