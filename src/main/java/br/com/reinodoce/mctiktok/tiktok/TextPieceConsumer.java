package br.com.reinodoce.mctiktok.tiktok;

import br.com.reinodoce.mctiktok.chat.MessageSanitizer;
import io.github.jwdeveloper.tiktok.messages.data.Text;
import io.github.jwdeveloper.tiktok.messages.data.User;

final class TextPieceConsumer {
    private static final String HEART_GLYPH = "❤";
    private static final String GIFT_FALLBACK = "[gift]";

    private TextPieceConsumer() {
    }

    static DetectedAuthor consumeAll(Text text, SegmentBuilder buffer, boolean suppressLeadingUserPiece) {
        DetectedAuthor.Builder author = new DetectedAuthor.Builder();
        for (Text.TextPiece piece : text.getPiecesListList()) {
            consumePiece(piece, buffer, author, suppressLeadingUserPiece);
        }
        return author.build();
    }

    private static void consumePiece(
            Text.TextPiece piece,
            SegmentBuilder buffer,
            DetectedAuthor.Builder author,
            boolean suppressLeadingUserPiece
    ) {
        boolean handled = tryConsumeImage(piece, buffer)
                || tryConsumeStringValue(piece, buffer)
                || tryConsumeUser(piece, buffer, author, suppressLeadingUserPiece);
        if (handled) {
            return;
        }
        handled = tryConsumePatternRef(piece, buffer) || tryConsumeHeart(piece, buffer);
        if (!handled) {
            tryConsumeGift(piece, buffer);
        }
    }

    private static boolean tryConsumeImage(Text.TextPiece piece, SegmentBuilder buffer) {
        if (!(piece.hasImageValue() && piece.getImageValue().hasImageModel())) {
            return false;
        }
        buffer.appendEmote(piece.getImageValue().getImageModel());
        return true;
    }

    private static boolean tryConsumeStringValue(Text.TextPiece piece, SegmentBuilder buffer) {
        if (piece.getStringValue().isBlank()) {
            return false;
        }
        buffer.appendText(piece.getStringValue());
        return true;
    }

    private static boolean tryConsumeUser(
            Text.TextPiece piece,
            SegmentBuilder buffer,
            DetectedAuthor.Builder author,
            boolean suppressLeadingUserPiece
    ) {
        if (!piece.hasUserValue()) {
            return false;
        }
        User user = piece.getUserValue().getUser();
        if (user != null) {
            author.capture(user);
        }
        if (!(suppressLeadingUserPiece && buffer.isEmpty())) {
            buffer.appendText(resolveDisplayName(user));
        }
        return true;
    }

    private static boolean tryConsumePatternRef(Text.TextPiece piece, SegmentBuilder buffer) {
        if (!piece.hasPatternRefValue()) {
            return false;
        }
        buffer.appendText(piece.getPatternRefValue().getDefaultPattern());
        return true;
    }

    private static boolean tryConsumeHeart(Text.TextPiece piece, SegmentBuilder buffer) {
        if (!piece.hasHeartValue()) {
            return false;
        }
        buffer.appendText(HEART_GLYPH);
        return true;
    }

    private static void tryConsumeGift(Text.TextPiece piece, SegmentBuilder buffer) {
        if (piece.hasGiftValue()) {
            buffer.appendText(GIFT_FALLBACK);
        }
    }

    static String resolveDisplayName(User user) {
        if (user == null) {
            return "";
        }
        if (!MessageSanitizer.sanitize(user.getNickname()).isBlank()) {
            return user.getNickname();
        }
        return user.getUsername();
    }

    record DetectedAuthor(String username, String avatarUrl, User user) {
        static final class Builder {
            private String pendingUsername = "";
            private String pendingAvatarUrl = "";
            private User pendingUser;

            void capture(User candidate) {
                if (pendingUser == null) {
                    pendingUser = candidate;
                }
                if (pendingUsername.isBlank()) {
                    pendingUsername = resolveDisplayName(candidate);
                }
                if (pendingAvatarUrl.isBlank()) {
                    pendingAvatarUrl = TikTokMediaResolver.resolveUserAvatarUrl(candidate);
                }
            }

            DetectedAuthor build() {
                return new DetectedAuthor(pendingUsername, pendingAvatarUrl, pendingUser);
            }
        }
    }
}
