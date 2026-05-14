package br.com.reinodoce.mctiktok.alert;

/**
 * Media references observed on a TikTok alert event.
 *
 * @param profileImageUrl TikTok user profile image URL, or blank
 * @param giftImageUrl TikTok gift image URL, or blank
 */
public record AlertMediaReferences(String profileImageUrl, String giftImageUrl) {
    /** Empty media references. */
    public static final AlertMediaReferences EMPTY = new AlertMediaReferences("", "");

    /**
     * Normalizes null values to blank strings.
     *
     * @param profileImageUrl profile image URL
     * @param giftImageUrl gift image URL
     */
    public AlertMediaReferences {
        profileImageUrl = profileImageUrl == null ? "" : profileImageUrl.trim();
        giftImageUrl = giftImageUrl == null ? "" : giftImageUrl.trim();
    }
}
