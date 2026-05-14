package br.com.reinodoce.mctiktok.platform.mc1201;

import br.com.reinodoce.mctiktok.alert.AlertToastPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;
import java.util.Objects;

/**
 * Client toast capable of rendering the media references carried by local alert payloads.
 */
final class AlertMediaToast implements Toast {
    private static final int TOAST_WIDTH = 200;
    private static final int MIN_HEIGHT = 48;
    private static final int TEXT_LEFT = 48;
    private static final int TEXT_RIGHT_PADDING = 10;
    private static final int TEXT_WIDTH = TOAST_WIDTH - TEXT_LEFT - TEXT_RIGHT_PADDING;
    private static final int LINE_SPACING = 12;
    private static final int HEIGHT_PADDING = 20;
    private static final int TITLE_ONLY_Y = 12;
    private static final int TITLE_WITH_MESSAGE_Y = 7;
    private static final int FIRST_MESSAGE_Y = 18;
    private static final int DISPLAY_MILLIS = 5_000;
    private static final int TITLE_COLOR = -256;
    private static final int MESSAGE_COLOR = -1;

    private final Object toastToken;
    private final AlertToastMediaRenderer mediaRenderer;
    private Component title = Component.empty();
    private Component message = Component.empty();
    private AlertToastPayload payload;
    private List<FormattedCharSequence> messageLines = List.of();
    private long lastChanged;
    private boolean changed = true;

    AlertMediaToast(
            Component title,
            Component message,
            AlertToastPayload payload,
            AlertToastMediaRenderer mediaRenderer
    ) {
        this.toastToken = AlertToastMediaSelector.token(payload);
        this.mediaRenderer = Objects.requireNonNull(mediaRenderer, "mediaRenderer");
        reset(title, message, payload);
    }

    void reset(Component title, Component message, AlertToastPayload payload) {
        this.title = title == null ? Component.empty() : title;
        this.message = message == null ? Component.empty() : message;
        this.payload = Objects.requireNonNull(payload, "payload");
        this.messageLines = Minecraft.getInstance().font.split(this.message, TEXT_WIDTH);
        this.changed = true;
    }

    @Override
    public Object getToken() {
        return toastToken;
    }

    @Override
    public int width() {
        return TOAST_WIDTH;
    }

    @Override
    public int height() {
        return Math.max(MIN_HEIGHT, HEIGHT_PADDING + Math.max(messageLines.size(), 1) * LINE_SPACING);
    }

    @Override
    public Toast.Visibility render(GuiGraphics graphics, ToastComponent toastComponent, long visibleMillis) {
        if (changed) {
            lastChanged = visibleMillis;
            changed = false;
        }

        AlertToastBackground.render(graphics, width(), height());
        mediaRenderer.render(graphics, payload, height());
        renderText(graphics, toastComponent.getMinecraft().font);

        double displayTime = DISPLAY_MILLIS * toastComponent.getNotificationDisplayTimeMultiplier();
        return visibleMillis - lastChanged < displayTime ? Toast.Visibility.SHOW : Toast.Visibility.HIDE;
    }

    private void renderText(GuiGraphics graphics, Font font) {
        if (messageLines.isEmpty()) {
            graphics.drawString(font, title, TEXT_LEFT, TITLE_ONLY_Y, TITLE_COLOR, false);
            return;
        }
        graphics.drawString(font, title, TEXT_LEFT, TITLE_WITH_MESSAGE_Y, TITLE_COLOR, false);
        for (int index = 0; index < messageLines.size(); index++) {
            graphics.drawString(font, messageLines.get(index), TEXT_LEFT, FIRST_MESSAGE_Y + index * LINE_SPACING,
                    MESSAGE_COLOR, false);
        }
    }
}
