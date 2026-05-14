package br.com.reinodoce.mctiktok.client.hud;

import net.minecraft.network.chat.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * Bounded in-memory queue for HUD output lines.
 */
public class HudMessageStore {
    private final Deque<Component> messages = new ArrayDeque<>();

    /**
     * Adds a message and trims the queue to the configured line count.
     *
     * @param component message to retain
     * @param maxLines maximum retained line count
     */
    public synchronized void add(Component component, int maxLines) {
        if (component == null) {
            return;
        }
        messages.addLast(component);
        trim(maxLines);
    }

    /**
     * Returns retained messages after applying the current line cap.
     *
     * @param maxLines maximum retained line count
     * @return ordered retained messages
     */
    public synchronized List<Component> snapshot(int maxLines) {
        trim(maxLines);
        return List.copyOf(messages);
    }

    /**
     * Clears retained HUD messages.
     */
    public synchronized void clear() {
        messages.clear();
    }

    private void trim(int maxLines) {
        int safeMaxLines = Math.max(1, maxLines);
        while (messages.size() > safeMaxLines) {
            messages.removeFirst();
        }
    }
}
