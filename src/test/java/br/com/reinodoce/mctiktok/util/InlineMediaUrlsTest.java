package br.com.reinodoce.mctiktok.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InlineMediaUrlsTest {

    @Test
    void parsesResourceUrlWithoutMinecraftTypes() {
        InlineMediaUrls.ResourceReference ref = InlineMediaUrls.parseResourceUrl("resource://reinodoce_mctiktok/textures/gui/no_user_image.png");

        assertEquals("reinodoce_mctiktok", ref.namespace());
        assertEquals("textures/gui/no_user_image.png", ref.path());
    }

    @Test
    void defaultAvatarUsesResourceScheme() {
        assertTrue(InlineMediaUrls.defaultAvatarUrl().startsWith(InlineMediaUrls.RESOURCE_SCHEME));
    }
}
