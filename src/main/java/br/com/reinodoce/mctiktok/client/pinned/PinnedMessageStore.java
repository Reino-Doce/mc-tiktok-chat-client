package br.com.reinodoce.mctiktok.client.pinned;

import br.com.reinodoce.mctiktok.pinned.PinnedLiveMessage;
import net.minecraft.network.chat.Component;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.function.LongSupplier;

/**
 * Bounded in-memory queue for local pinned-message overlay entries.
 */
public class PinnedMessageStore {
    private static final long UNKNOWN_PIN_ID = 0L;

    private final Deque<Entry> messages = new ArrayDeque<>();
    private final LongSupplier clock;

    /**
     * Creates a store using the system clock.
     */
    public PinnedMessageStore() {
        this(System::currentTimeMillis);
    }

    PinnedMessageStore(LongSupplier clock) {
        this.clock = clock;
    }

    /**
     * Adds a message and trims expired or excess entries.
     *
     * @param pinId TikTok pin identifier, or zero when unavailable
     * @param component rendered pinned message
     * @param duration visible duration
     * @param maxMessages maximum retained message count
     */
    public synchronized void add(long pinId, Component component, Duration duration, int maxMessages) {
        if (component == null) {
            return;
        }
        trimExpired(clock.getAsLong());
        removeMatchingPin(pinId);
        messages.addLast(new Entry(pinId, component, expiresAt(duration)));
        trim(maxMessages);
    }

    /**
     * Returns visible messages after applying the current count cap and expiry.
     *
     * @param maxMessages maximum visible message count
     * @return ordered visible messages
     */
    public synchronized List<Entry> snapshot(int maxMessages) {
        trimExpired(clock.getAsLong());
        trim(maxMessages);
        return List.copyOf(messages);
    }

    /**
     * Clears retained pinned messages.
     */
    public synchronized void clear() {
        messages.clear();
    }

    private void removeMatchingPin(long pinId) {
        if (pinId == UNKNOWN_PIN_ID) {
            return;
        }
        messages.removeIf(entry -> entry.pinId() == pinId);
    }

    private void trimExpired(long nowMillis) {
        Iterator<Entry> iterator = messages.iterator();
        while (iterator.hasNext()) {
            Entry entry = iterator.next();
            if (entry.expiresAtMillis() <= nowMillis) {
                iterator.remove();
            }
        }
    }

    private void trim(int maxMessages) {
        int safeMaxMessages = Math.max(1, maxMessages);
        while (messages.size() > safeMaxMessages) {
            messages.removeFirst();
        }
    }

    private long expiresAt(Duration duration) {
        long visibleMillis = duration == null || duration.isZero() || duration.isNegative()
                ? PinnedLiveMessage.DEFAULT_DURATION.toMillis()
                : duration.toMillis();
        try {
            return Math.addExact(clock.getAsLong(), visibleMillis);
        } catch (ArithmeticException exception) {
            return Long.MAX_VALUE;
        }
    }

    /**
     * Retained pinned-message overlay entry.
     *
     * @param pinId TikTok pin identifier, or zero when unavailable
     * @param component rendered pinned message
     * @param expiresAtMillis absolute expiry time in milliseconds
     */
    public record Entry(long pinId, Component component, long expiresAtMillis) {
    }
}
