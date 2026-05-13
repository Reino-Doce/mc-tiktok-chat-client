package br.com.reinodoce.mctiktok.util;

/**
 * Helpers for internal resource URLs used by inline media segments.
 */
public final class InlineMediaUrls {
    /** Scheme prefix used for Minecraft resource-backed media. */
    public static final String RESOURCE_SCHEME = "resource://";
    /** Default mod namespace for bundled inline media textures. */
    public static final String DEFAULT_NAMESPACE = "reinodoce_mctiktok";
    /** Bundled fallback avatar texture path. */
    public static final String DEFAULT_AVATAR_PATH = "textures/gui/no_user_image.png";
    /** Resource URL for the bundled fallback avatar. */
    public static final String DEFAULT_AVATAR_URL = resourceUrl(DEFAULT_NAMESPACE, DEFAULT_AVATAR_PATH);

    private InlineMediaUrls() {
    }

    /**
     * Returns the bundled fallback avatar URL.
     *
     * @return default avatar resource URL
     */
    public static String defaultAvatarUrl() {
        return DEFAULT_AVATAR_URL;
    }

    /**
     * Builds a resource URL from a namespace and resource path.
     *
     * @param namespace Minecraft resource namespace
     * @param path resource path inside the namespace
     * @return resource URL
     */
    public static String resourceUrl(String namespace, String path) {
        if (namespace == null || namespace.isBlank()) {
            throw new IllegalArgumentException("namespace must not be blank");
        }
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("path must not be blank");
        }
        return RESOURCE_SCHEME + namespace + "/" + path;
    }

    /**
     * Checks whether a URL references an internal Minecraft resource.
     *
     * @param url URL to inspect
     * @return true when the URL starts with the resource scheme
     */
    public static boolean isResourceUrl(String url) {
        return url != null && url.startsWith(RESOURCE_SCHEME);
    }

    /**
     * Parses a resource URL into namespace and path components.
     *
     * @param url resource URL to parse
     * @return parsed resource reference
     */
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

    /**
     * Parsed Minecraft resource reference.
     *
     * @param namespace resource namespace
     * @param path resource path
     */
    public record ResourceReference(String namespace, String path) {
    }
}
