package br.com.reinodoce.mctiktok.platform.mc1201;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;

/**
 * Renders vanilla-style toast backgrounds for custom alert toasts.
 */
final class AlertToastBackground {
    private static final int BOTTOM_CAP_HEIGHT = 4;
    private static final int TOP_CAP_HEIGHT = 28;
    private static final int MIDDLE_TEXTURE_Y = 16;
    private static final int MIDDLE_ROW_HEIGHT = 16;
    private static final int MIDDLE_STEP = 10;
    private static final int BOTTOM_TEXTURE_Y = 32;
    private static final int TOP_LEFT_CAP_WIDTH = 20;
    private static final int BODY_LEFT_CAP_WIDTH = 5;
    private static final int RIGHT_CAP_MAX_WIDTH = 60;
    private static final int TILE_WIDTH = 64;
    private static final int TEXTURE_WIDTH = 160;

    private AlertToastBackground() {
    }

    static void render(GuiGraphics graphics, int width, int height) {
        int bottomCapHeight = Math.min(BOTTOM_CAP_HEIGHT, height - TOP_CAP_HEIGHT);
        renderRow(graphics, width, 0, 0, TOP_CAP_HEIGHT);
        for (int rowTop = TOP_CAP_HEIGHT; rowTop < height - bottomCapHeight; rowTop += MIDDLE_STEP) {
            renderRow(graphics, width, MIDDLE_TEXTURE_Y, rowTop,
                    Math.min(MIDDLE_ROW_HEIGHT, height - rowTop - bottomCapHeight));
        }
        renderRow(graphics, width, BOTTOM_TEXTURE_Y - bottomCapHeight, height - bottomCapHeight, bottomCapHeight);
    }

    private static void renderRow(GuiGraphics graphics, int width, int textureY, int top, int height) {
        int leftCapWidth = textureY == 0 ? TOP_LEFT_CAP_WIDTH : BODY_LEFT_CAP_WIDTH;
        int rightCapWidth = Math.min(RIGHT_CAP_MAX_WIDTH, width - leftCapWidth);
        graphics.blit(Toast.TEXTURE, 0, top, 0, TILE_WIDTH + textureY, leftCapWidth, height);
        for (int left = leftCapWidth; left < width - rightCapWidth; left += TILE_WIDTH) {
            graphics.blit(Toast.TEXTURE, left, top, BOTTOM_TEXTURE_Y, TILE_WIDTH + textureY,
                    Math.min(TILE_WIDTH, width - left - rightCapWidth), height);
        }
        graphics.blit(
                Toast.TEXTURE,
                width - rightCapWidth,
                top,
                TEXTURE_WIDTH - rightCapWidth,
                TILE_WIDTH + textureY,
                rightCapWidth,
                height);
    }
}
