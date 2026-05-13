package br.com.reinodoce.mctiktok.emoji;

import br.com.reinodoce.mctiktok.util.ReinodoceLogger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Trie-backed catalog of renderable Unicode emoji sequences.
 */
public final class UnicodeEmojiCatalog {
    private static final String RESOURCE_PATH = "emoji/twemoji_rgi_sequences.txt";
    private static final int HEX_RADIX = 16;
    private static final UnicodeEmojiCatalog INSTANCE = new UnicodeEmojiCatalog(loadRoot());

    private final TrieNode root;

    private UnicodeEmojiCatalog(TrieNode root) {
        this.root = root;
    }

    /**
     * Returns the shared emoji catalog loaded from bundled resources.
     *
     * @return shared catalog
     */
    public static UnicodeEmojiCatalog getInstance() {
        return INSTANCE;
    }

    /**
     * Finds the longest emoji sequence beginning at a code-point index.
     *
     * @param codePoints source code points
     * @param startIndex index to start matching
     * @return match range, or {@code null} when no emoji sequence starts there
     */
    public Match longestMatch(int[] codePoints, int startIndex) {
        TrieNode node = root;
        int cursor = startIndex;
        int endExclusive = -1;
        while (cursor < codePoints.length) {
            node = node.children.get(codePoints[cursor]);
            if (node == null) {
                break;
            }
            cursor++;
            if (node.terminal) {
                endExclusive = cursor;
            }
        }
        return endExclusive < 0 ? null : new Match(startIndex, endExclusive);
    }

    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    private static TrieNode loadRoot() {
        TrieNode root = new TrieNode();
        try (InputStream inputStream = UnicodeEmojiCatalog.class.getClassLoader().getResourceAsStream(RESOURCE_PATH)) {
            if (inputStream == null) {
                throw new IllegalStateException("Emoji catalog resource not found: " + RESOURCE_PATH);
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String trimmed = line.trim();
                    if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                        continue;
                    }

                    String[] parts = trimmed.split("-");
                    TrieNode node = root;
                    for (String part : parts) {
                        int codePoint = Integer.parseInt(part, HEX_RADIX);
                        node = node.children.computeIfAbsent(codePoint, ignored -> new TrieNode());
                    }
                    node.terminal = true;
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load emoji catalog resource " + RESOURCE_PATH, exception);
        }

        ReinodoceLogger.LOGGER.info("Loaded Unicode emoji catalog from {}", RESOURCE_PATH);
        return root;
    }

    /**
     * Inclusive/exclusive match range in a code-point array.
     *
     * @param startInclusive first matched index
     * @param endExclusive index after the last matched code point
     */
    public record Match(int startInclusive, int endExclusive) {
    }

    private static final class TrieNode {
        private final Map<Integer, TrieNode> children = new HashMap<>();
        private boolean terminal;
    }
}
