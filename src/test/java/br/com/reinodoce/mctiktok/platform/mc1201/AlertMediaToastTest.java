package br.com.reinodoce.mctiktok.platform.mc1201;

import br.com.reinodoce.mctiktok.alert.AlertEventType;
import br.com.reinodoce.mctiktok.alert.AlertToastMediaMode;
import br.com.reinodoce.mctiktok.alert.AlertToastPayload;
import br.com.reinodoce.mctiktok.util.InlineMediaUrls;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlertMediaToastTest {
    private static final String PROFILE_IMAGE = "https://cdn.example/alice.png";
    private static final String GIFT_IMAGE = "https://cdn.example/gift.png";
    private static final String CUSTOM_IMAGE = "reinodoce_mctiktok:textures/gui/no_user_image.png";

    @Test
    void mediaToastCandidateRequiresSelectedMedia() {
        assertFalse(AlertToastMediaSelector.shouldRenderMedia(payload(AlertToastMediaMode.NONE, "", "", "")));
        assertFalse(AlertToastMediaSelector.shouldRenderMedia(payload(AlertToastMediaMode.PROFILE, "", "", "")));
        assertTrue(AlertToastMediaSelector.shouldRenderMedia(payload(AlertToastMediaMode.PROFILE, PROFILE_IMAGE, "", "")));
        assertTrue(AlertToastMediaSelector.shouldRenderMedia(payload(AlertToastMediaMode.GIFT, "", GIFT_IMAGE, "")));
        assertTrue(AlertToastMediaSelector.shouldRenderMedia(payload(AlertToastMediaMode.CUSTOM, "", "", CUSTOM_IMAGE)));
    }

    @Test
    void profileCustomModeFallsBackToCustomImageWhenProfileIsMissing() {
        assertTrue(AlertToastMediaSelector.shouldRenderMedia(payload(
                AlertToastMediaMode.PROFILE_CUSTOM, "", "", CUSTOM_IMAGE)));
    }

    @Test
    void customResourceIdsAreConvertedToInlineMediaResourceUrls() {
        assertEquals(
                InlineMediaUrls.resourceUrl("reinodoce_mctiktok", "textures/gui/no_user_image.png"),
                AlertToastMediaSelector.normalizeMediaSource(CUSTOM_IMAGE));
        assertEquals(
                "resource://reinodoce_mctiktok/textures/gui/no_user_image.png",
                AlertToastMediaSelector.normalizeMediaSource(
                        "resource://reinodoce_mctiktok/textures/gui/no_user_image.png"));
        assertEquals(PROFILE_IMAGE, AlertToastMediaSelector.normalizeMediaSource(PROFILE_IMAGE));
    }

    private static AlertToastPayload payload(
            AlertToastMediaMode mode,
            String profileImage,
            String giftImage,
            String customImage
    ) {
        return new AlertToastPayload(
                AlertEventType.GIFT,
                "Alert",
                "Message",
                mode,
                "",
                profileImage,
                giftImage,
                customImage);
    }
}
