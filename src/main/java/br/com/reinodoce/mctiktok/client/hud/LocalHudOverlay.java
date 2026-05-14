package br.com.reinodoce.mctiktok.client.hud;

import br.com.reinodoce.mctiktok.config.HudPosition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders retained LIVE lines as a lightweight local HUD overlay.
 */
@SuppressWarnings("PMD.CloseResource")
public class LocalHudOverlay {
    private static final int MARGIN = 8;
    private static final int PADDING = 4;
    private static final int LINE_HEIGHT = 10;
    private static final int LINE_GAP = 2;
    private static final int MIN_TEXT_WIDTH = 40;
    private static final int TEXT_COLOR = 0xE6FFFFFF;
    private static final int BACKGROUND_COLOR = 0x66000000;

    private final HudMessageStore messageStore;

    /**
     * Creates an overlay renderer.
     *
     * @param messageStore retained HUD message store
     */
    public LocalHudOverlay(HudMessageStore messageStore) {
        this.messageStore = messageStore;
    }

    /**
     * Renders the current HUD messages.
     *
     * @param graphics GUI graphics context
     * @param screenWidth current screen width
     * @param screenHeight current screen height
     * @param position configured HUD position
     * @param maxLines configured maximum line count
     */
    public void render(GuiGraphics graphics, int screenWidth, int screenHeight, HudPosition position, int maxLines) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.options.hideGui) {
            return;
        }
        Font font = minecraft.font;
        List<String> lines = textLines(font, screenWidth, maxLines);
        if (lines.isEmpty()) {
            return;
        }
        int width = width(font, lines);
        int height = lines.size() * LINE_HEIGHT + (lines.size() - 1) * LINE_GAP;
        int x = x(position, screenWidth, width);
        int y = y(position, screenHeight, height);
        graphics.fill(x - PADDING, y - PADDING, x + width + PADDING, y + height + PADDING, BACKGROUND_COLOR);
        for (int index = 0; index < lines.size(); index++) {
            graphics.drawString(font, lines.get(index), x, y + index * (LINE_HEIGHT + LINE_GAP), TEXT_COLOR, true);
        }
    }

    private List<String> textLines(Font font, int screenWidth, int maxLines) {
        int maxTextWidth = Math.max(MIN_TEXT_WIDTH, screenWidth - (MARGIN + PADDING) * 2);
        List<String> lines = new ArrayList<>();
        for (Component component : messageStore.snapshot(maxLines)) {
            String text = component.getString();
            lines.add(font.plainSubstrByWidth(text, maxTextWidth));
        }
        return lines;
    }

    private static int width(Font font, List<String> lines) {
        int width = 0;
        for (String line : lines) {
            width = Math.max(width, font.width(line));
        }
        return width;
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
}
