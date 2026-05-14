package br.com.reinodoce.mctiktok.client.pinned;

import br.com.reinodoce.mctiktok.config.HudPosition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders TikTok pinned messages as a dedicated local screen overlay.
 */
@SuppressWarnings("PMD.CloseResource")
public class LocalPinnedMessageOverlay {
    private static final int MARGIN = 8;
    private static final int PADDING = 5;
    private static final int LINE_HEIGHT = 10;
    private static final int LINE_GAP = 2;
    private static final int MESSAGE_GAP = 4;
    private static final int MAX_TEXT_WIDTH = 280;
    private static final int MIN_TEXT_WIDTH = 80;
    private static final int MAX_LINES_PER_MESSAGE = 4;
    private static final int TEXT_COLOR = 0xF0FFFFFF;
    private static final int BACKGROUND_COLOR = 0xA0000000;

    private final PinnedMessageStore messageStore;

    /**
     * Creates an overlay renderer.
     *
     * @param messageStore retained pinned-message store
     */
    public LocalPinnedMessageOverlay(PinnedMessageStore messageStore) {
        this.messageStore = messageStore;
    }

    /**
     * Renders current pinned messages.
     *
     * @param graphics GUI graphics context
     * @param screenWidth current screen width
     * @param screenHeight current screen height
     * @param position configured overlay position
     * @param maxMessages configured maximum message count
     */
    public void render(GuiGraphics graphics, int screenWidth, int screenHeight, HudPosition position, int maxMessages) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.options.hideGui) {
            return;
        }
        Font font = minecraft.font;
        List<RenderedMessage> messages = renderedMessages(font, screenWidth, maxMessages);
        if (messages.isEmpty()) {
            return;
        }
        int width = textWidth(screenWidth);
        int totalHeight = totalHeight(messages);
        int x = x(position, screenWidth, width);
        int y = y(position, screenHeight, totalHeight);
        for (RenderedMessage message : messages) {
            drawMessage(graphics, font, message, x, y, width);
            y += message.height() + MESSAGE_GAP;
        }
    }

    private List<RenderedMessage> renderedMessages(Font font, int screenWidth, int maxMessages) {
        int maxTextWidth = textWidth(screenWidth);
        List<RenderedMessage> rendered = new ArrayList<>();
        for (PinnedMessageStore.Entry entry : messageStore.snapshot(maxMessages)) {
            List<FormattedCharSequence> lines = font.split(entry.component(), maxTextWidth);
            if (!lines.isEmpty()) {
                rendered.add(new RenderedMessage(limitLines(lines)));
            }
        }
        return rendered;
    }

    private static List<FormattedCharSequence> limitLines(List<FormattedCharSequence> lines) {
        if (lines.size() <= MAX_LINES_PER_MESSAGE) {
            return lines;
        }
        return List.copyOf(lines.subList(0, MAX_LINES_PER_MESSAGE));
    }

    private static int totalHeight(List<RenderedMessage> messages) {
        int total = 0;
        for (RenderedMessage message : messages) {
            total += message.height();
        }
        return total + Math.max(0, messages.size() - 1) * MESSAGE_GAP;
    }

    private static int textWidth(int screenWidth) {
        return Math.max(MIN_TEXT_WIDTH, Math.min(MAX_TEXT_WIDTH, screenWidth - (MARGIN + PADDING) * 2));
    }

    private static void drawMessage(
            GuiGraphics graphics,
            Font font,
            RenderedMessage message,
            int x,
            int y,
            int width
    ) {
        int blockHeight = message.height();
        graphics.fill(x - PADDING, y - PADDING, x + width + PADDING, y + blockHeight + PADDING, BACKGROUND_COLOR);
        for (int index = 0; index < message.lines().size(); index++) {
            graphics.drawString(
                    font,
                    message.lines().get(index),
                    x,
                    y + index * (LINE_HEIGHT + LINE_GAP),
                    TEXT_COLOR,
                    true);
        }
    }

    private static int x(HudPosition position, int screenWidth, int width) {
        return switch (position) {
            case TOP_RIGHT, BOTTOM_RIGHT -> Math.max(MARGIN, screenWidth - width - MARGIN);
            case TOP_LEFT, BOTTOM_LEFT -> MARGIN;
            default -> MARGIN;
        };
    }

    private static int y(HudPosition position, int screenHeight, int height) {
        return switch (position) {
            case BOTTOM_LEFT, BOTTOM_RIGHT -> Math.max(MARGIN, screenHeight - height - MARGIN);
            case TOP_LEFT, TOP_RIGHT -> MARGIN;
            default -> MARGIN;
        };
    }

    private record RenderedMessage(List<FormattedCharSequence> lines) {
        int height() {
            return lines.size() * LINE_HEIGHT + Math.max(0, lines.size() - 1) * LINE_GAP;
        }
    }
}
