package br.com.reinodoce.mctiktok.util;

import br.com.reinodoce.mctiktok.ReinodoceMcTiktokMod;
import net.minecraft.resources.ResourceLocation;

public final class InlineMediaUrls {
    public static final String RESOURCE_SCHEME = "resource://";
    public static final ResourceLocation DEFAULT_AVATAR_TEXTURE = new ResourceLocation(
            ReinodoceMcTiktokMod.MOD_ID,
            "textures/gui/no_user_image.png"
    );

    private InlineMediaUrls() {
    }

    public static String defaultAvatarUrl() {
        return resourceUrl(DEFAULT_AVATAR_TEXTURE);
    }

    public static String resourceUrl(ResourceLocation resourceLocation) {
        return RESOURCE_SCHEME + resourceLocation.getNamespace() + "/" + resourceLocation.getPath();
    }

    public static boolean isResourceUrl(String url) {
        return url != null && url.startsWith(RESOURCE_SCHEME);
    }

    public static ResourceLocation parseResourceUrl(String url) {
        if (!isResourceUrl(url)) {
            throw new IllegalArgumentException("Not a resource URL: " + url);
        }

        String value = url.substring(RESOURCE_SCHEME.length());
        int slash = value.indexOf('/');
        if (slash <= 0 || slash == value.length() - 1) {
            throw new IllegalArgumentException("Invalid resource URL: " + url);
        }
        return new ResourceLocation(value.substring(0, slash), value.substring(slash + 1));
    }
}
