package br.com.reinodoce.mctiktok.tiktok;

import br.com.reinodoce.mctiktok.chat.MessageSanitizer;
import br.com.reinodoce.mctiktok.util.InlineMediaUrls;
import io.github.jwdeveloper.tiktok.data.models.Picture;
import io.github.jwdeveloper.tiktok.data.models.gifts.Gift;
import io.github.jwdeveloper.tiktok.data.models.users.User;
import io.github.jwdeveloper.tiktok.messages.data.Image;

/**
 * Extracts normalized media URLs from TikTok library models.
 */
public final class TikTokMediaResolver {
    private TikTokMediaResolver() {
    }

    /**
     * Returns the default avatar URL used when TikTok has no usable avatar.
     *
     * @return default avatar URL
     */
    public static String defaultAvatarUrl() {
        return InlineMediaUrls.defaultAvatarUrl();
    }

    /**
     * Resolves an avatar URL from the object-model user type.
     *
     * @param user TikTok user model
     * @return avatar URL or default avatar URL
     */
    public static String resolveUserAvatarUrl(User user) {
        String resolved = resolvePictureUrl(user == null ? null : user.getPicture());
        return resolved.isBlank() ? defaultAvatarUrl() : resolved;
    }

    /**
     * Resolves an avatar URL from the protobuf user type.
     *
     * @param user TikTok protobuf user model
     * @return avatar URL or default avatar URL
     */
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

    /**
     * Resolves a gift icon URL.
     *
     * @param gift TikTok gift model
     * @return icon URL or blank
     */
    public static String resolveGiftIconUrl(Gift gift) {
        if (gift == null) {
            return "";
        }
        return resolvePictureUrl(gift.getPicture());
    }

    /**
     * Resolves a URL from a TikTok picture wrapper.
     *
     * @param picture TikTok picture model
     * @return normalized URL or blank
     */
    public static String resolvePictureUrl(Picture picture) {
        if (picture == null) {
            return "";
        }
        return normalizeUrl(picture.getLink());
    }

    /**
     * Resolves a URL from a TikTok protobuf image.
     *
     * @param image TikTok image model
     * @return normalized URL or blank
     */
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
