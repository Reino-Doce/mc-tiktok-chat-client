package br.com.reinodoce.mctiktok.platform.mc1201;

import br.com.reinodoce.mctiktok.alert.AlertToastPayload;
import br.com.reinodoce.mctiktok.client.overlay.InlineMediaCache;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.network.chat.Component;

/**
 * Owns custom alert-toast presentation and media-cache reuse for Forge 1.20.1.
 */
final class AlertToastPresenter {
    private final AlertToastMediaRenderer mediaRenderer = new AlertToastMediaRenderer(new InlineMediaCache());

    boolean show(Component title, Component message, AlertToastPayload payload) {
        ToastComponent toasts = Minecraft.getInstance().getToasts();
        if (toasts == null || !AlertToastMediaSelector.shouldRenderMedia(payload)) {
            return false;
        }
        AlertMediaToast toast = toasts.getToast(
                AlertMediaToast.class,
                AlertToastMediaSelector.token(payload));
        if (toast == null) {
            toasts.addToast(new AlertMediaToast(title, message, payload, mediaRenderer));
        } else {
            toast.reset(title, message, payload);
        }
        return true;
    }
}
