package br.com.reinodoce.mctiktok.client.overlay;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

final class InlineMediaNetworkClient {
    static final int MAX_RESPONSE_BYTES = 2 * 1024 * 1024;

    private static final String USER_AGENT = "ReinoDoce-MCTikTok/0.1.0";
    private static final String HTTP_GET = "GET";
    private static final int HTTP_TIMEOUT_MILLIS = 7_500;
    private static final int REDIRECT_MIN = 300;
    private static final int REDIRECT_MAX = 399;
    private static final int SUCCESS_MIN = 200;
    private static final int SUCCESS_MAX = 299;
    private static final int MAX_REDIRECTS = 3;
    private static final int BUFFER_SIZE = 8192;

    private InlineMediaNetworkClient() {
    }

    static Response download(String sourceUrl) throws IOException {
        HttpURLConnection connection = null;
        try {
            connection = openConnection(sourceUrl);
            validateSuccess(connection.getResponseCode(), sourceUrl);
            String contentType = connection.getContentType();
            InlineMediaImageDecoder.validateContentType(contentType);
            return new Response(readAllBytes(connection), contentType);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static HttpURLConnection openConnection(String sourceUrl) throws IOException {
        URL initialUrl = new URL(sourceUrl);
        validateHttps(initialUrl);
        URL currentUrl = initialUrl;
        for (int redirectCount = 0; redirectCount <= MAX_REDIRECTS; redirectCount++) {
            HttpURLConnection connection = createConnection(currentUrl);
            int status = connection.getResponseCode();
            if (!isRedirect(status)) {
                return connection;
            }
            String location = connection.getHeaderField("Location");
            connection.disconnect();
            if (redirectCount == MAX_REDIRECTS || location == null || location.isBlank()) {
                throw new IOException("Unsupported inline media redirect for " + redactedUrl(initialUrl));
            }
            URL redirected = redirectedUrl(currentUrl, location);
            validateSameHostRedirect(initialUrl, redirected);
            currentUrl = redirected;
        }
        throw new IOException("Too many inline media redirects for " + redactedUrl(initialUrl));
    }

    private static HttpURLConnection createConnection(URL url) throws IOException {
        URLConnection raw = url.openConnection();
        if (!(raw instanceof HttpURLConnection connection)) {
            throw new IOException("Unsupported URL connection for inline media: " + redactedUrl(url));
        }
        connection.setRequestMethod(HTTP_GET);
        connection.setConnectTimeout(HTTP_TIMEOUT_MILLIS);
        connection.setReadTimeout(HTTP_TIMEOUT_MILLIS);
        connection.setInstanceFollowRedirects(false);
        connection.setRequestProperty("User-Agent", USER_AGENT);
        long contentLength = connection.getContentLengthLong();
        if (contentLength > MAX_RESPONSE_BYTES) {
            throw new IOException("Inline media response exceeds " + MAX_RESPONSE_BYTES + " bytes");
        }
        return connection;
    }

    private static byte[] readAllBytes(HttpURLConnection connection) throws IOException {
        try (InputStream inputStream = connection.getInputStream()) {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[BUFFER_SIZE];
            int total = 0;
            int read = inputStream.read(buffer);
            while (read != -1) {
                total += read;
                if (total > MAX_RESPONSE_BYTES) {
                    throw new IOException("Inline media response exceeds " + MAX_RESPONSE_BYTES + " bytes");
                }
                output.write(buffer, 0, read);
                read = inputStream.read(buffer);
            }
            return output.toByteArray();
        }
    }

    private static void validateSuccess(int status, String sourceUrl) throws IOException {
        if (status < SUCCESS_MIN || status > SUCCESS_MAX) {
            throw new IOException("Inline media request failed with HTTP " + status + " for " + redactedUrl(sourceUrl));
        }
    }

    private static void validateHttps(URL url) throws IOException {
        if (!"https".equalsIgnoreCase(url.getProtocol())) {
            throw new IOException("Unsupported inline media URL scheme for " + redactedUrl(url));
        }
    }

    private static boolean isRedirect(int status) {
        return status >= REDIRECT_MIN && status <= REDIRECT_MAX;
    }

    private static void validateSameHostRedirect(URL original, URL redirected) throws IOException {
        validateHttps(redirected);
        if (!original.getHost().equalsIgnoreCase(redirected.getHost())
                || effectivePort(original) != effectivePort(redirected)) {
            throw new IOException("Cross-host inline media redirect rejected for " + redactedUrl(original));
        }
    }

    private static URL redirectedUrl(URL currentUrl, String location) throws IOException {
        return new URL(currentUrl, location);
    }

    private static int effectivePort(URL url) {
        int port = url.getPort();
        return port == -1 ? url.getDefaultPort() : port;
    }

    private static String redactedUrl(String sourceUrl) {
        try {
            return redactedUrl(new URL(sourceUrl));
        } catch (IOException exception) {
            return "<invalid-url>";
        }
    }

    private static String redactedUrl(URL url) {
        return url.getProtocol() + "://" + url.getHost();
    }

    record Response(byte[] bytes, String contentType) {
    }
}
