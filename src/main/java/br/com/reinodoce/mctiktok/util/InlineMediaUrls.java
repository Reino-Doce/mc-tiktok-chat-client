package br.com.reinodoce.mctiktok.util;

public final class InlineMediaUrls {
    public static final String RESOURCE_SCHEME = "resource://";
    public static final String DEFAULT_NAMESPACE = "reinodoce_mctiktok";
    public static final String DEFAULT_AVATAR_PATH = "textures/gui/no_user_image.png";
    public static final String DEFAULT_AVATAR_URL = resourceUrl(DEFAULT_NAMESPACE, DEFAULT_AVATAR_PATH);

    private InlineMediaUrls() {
    }

    public static String defaultAvatarUrl() {
        return DEFAULT_AVATAR_URL;
    }

    public static String resourceUrl(String namespace, String path) {
        if (namespace == null || namespace.isBlank()) {
            throw new IllegalArgumentException("namespace must not be blank");
        }
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("path must not be blank");
        }
        return RESOURCE_SCHEME + namespace + "/" + path;
    }

    public static boolean isResourceUrl(String url) {
        return url != null && url.startsWith(RESOURCE_SCHEME);
    }

    public static ResourceReference parseResourceUrl(String url) {
        if (!isResourceUrl(url)) {
            throw new IllegalArgumentException("Not a resource URL: " + url);
        }

        String value = url.substring(RESOURCE_SCHEME.length());
        int slash = value.indexOf('/');
        if (slash <= 0 || slash == value.length() - 1) {
            throw new IllegalArgumentException("Invalid resource URL: " + url);
        }
        return new ResourceReference(value.substring(0, slash), value.substring(slash + 1));
    }

    public record ResourceReference(String namespace, String path) {
    }
}
