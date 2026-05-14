package br.com.reinodoce.mctiktok.alert;

/**
 * Fully rendered alert toast request, including optional media hints.
 *
 * @param eventType alert event type
 * @param title toast title
 * @param message toast message
 * @param mediaMode requested media mode
 * @param mediaSource selected media source, or blank when unavailable
 * @param profileImageUrl TikTok profile image URL, or blank
 * @param giftImageUrl TikTok gift image URL, or blank
 * @param customImage configured custom image reference, or blank
 */
public record AlertToastPayload(
        AlertEventType eventType,
        String title,
        String message,
        AlertToastMediaMode mediaMode,
        String mediaSource,
        String profileImageUrl,
        String giftImageUrl,
        String customImage
) {
    /**
     * Normalizes null values so sinks can fall back without defensive checks.
     *
     * @param eventType alert event type
     * @param title toast title
     * @param message toast message
     * @param mediaMode requested media mode
     * @param mediaSource selected media source
     * @param profileImageUrl profile image URL
     * @param giftImageUrl gift image URL
     * @param customImage custom image reference
     */
    public AlertToastPayload {
        title = clean(title);
        message = clean(message);
        mediaMode = mediaMode == null ? AlertToastMediaMode.DEFAULT : mediaMode;
        mediaSource = clean(mediaSource);
        profileImageUrl = clean(profileImageUrl);
        giftImageUrl = clean(giftImageUrl);
        customImage = clean(customImage);
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
