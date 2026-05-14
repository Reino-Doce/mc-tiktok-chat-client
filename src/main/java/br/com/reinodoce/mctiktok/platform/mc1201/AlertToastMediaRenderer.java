package br.com.reinodoce.mctiktok.platform.mc1201;

import br.com.reinodoce.mctiktok.alert.AlertToastPayload;
import br.com.reinodoce.mctiktok.chat.RichLiveMessage;
import br.com.reinodoce.mctiktok.client.overlay.InlineMediaCache;
import net.minecraft.client.gui.GuiGraphics;

import java.util.Objects;

/**
 * Draws selected alert media into a custom toast.
 */
final class AlertToastMediaRenderer {
    private static final int IMAGE_SIZE = 32;
    private static final int IMAGE_LEFT = 8;
    private static final int OVERLAY_SIZE = 16;
    private static final int OVERLAY_OFFSET = 1;
    private static final int OVERLAY_BACKGROUND = 0xA0000000;

    private final InlineMediaCache mediaCache;

    AlertToastMediaRenderer(InlineMediaCache mediaCache) {
        this.mediaCache = Objects.requireNonNull(mediaCache, "mediaCache");
    }

    void render(GuiGraphics graphics, AlertToastPayload payload, int toastHeight) {
        RichLiveMessage.InlineMediaSegment primary = AlertToastMediaSelector.primarySegment(payload);
        if (primary == null) {
            return;
        }
        int imageTop = (toastHeight - IMAGE_SIZE) / 2;
        renderMedia(graphics, primary, new RenderArea(IMAGE_LEFT, imageTop, IMAGE_SIZE));
        renderOverlay(graphics, payload, imageTop);
    }

    private void renderOverlay(GuiGraphics graphics, AlertToastPayload payload, int imageTop) {
        RichLiveMessage.InlineMediaSegment overlay = AlertToastMediaSelector.overlaySegment(payload);
        if (overlay == null) {
            return;
        }
        int overlayLeft = IMAGE_LEFT + IMAGE_SIZE - OVERLAY_SIZE + OVERLAY_OFFSET;
        int overlayTop = imageTop + IMAGE_SIZE - OVERLAY_SIZE + OVERLAY_OFFSET;
        graphics.fill(overlayLeft - OVERLAY_OFFSET, overlayTop - OVERLAY_OFFSET,
                overlayLeft + OVERLAY_SIZE + OVERLAY_OFFSET,
                overlayTop + OVERLAY_SIZE + OVERLAY_OFFSET,
                OVERLAY_BACKGROUND);
        renderMedia(graphics, overlay, new RenderArea(overlayLeft, overlayTop, OVERLAY_SIZE));
    }

    private void renderMedia(
            GuiGraphics graphics,
            RichLiveMessage.InlineMediaSegment segment,
            RenderArea area
    ) {
        try {
            InlineMediaCache.TextureHandle handle = mediaCache.resolve(segment);
            renderSquareTexture(graphics, handle, area);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            renderSquareTexture(graphics, InlineMediaCache.TextureHandle.error(
                    InlineMediaCache.ERROR_TEXTURE,
                    InlineMediaCache.FALLBACK_DIMENSION,
                    InlineMediaCache.FALLBACK_DIMENSION), area);
        }
    }

    private static void renderSquareTexture(
            GuiGraphics graphics,
            InlineMediaCache.TextureHandle handle,
            RenderArea area
    ) {
        int safeWidth = Math.max(1, handle.sourceWidth());
        int safeHeight = Math.max(1, handle.sourceHeight());
        int crop = Math.max(1, Math.min(safeWidth, safeHeight));
        float sourceLeft = (safeWidth - crop) / 2.0F;
        float sourceTop = (safeHeight - crop) / 2.0F;
        graphics.blit(
                handle.texture(),
                area.left(),
                area.top(),
                area.size(),
                area.size(),
                sourceLeft,
                sourceTop,
                crop,
                crop,
                safeWidth,
                safeHeight);
    }

    private record RenderArea(int left, int top, int size) {
    }
}
