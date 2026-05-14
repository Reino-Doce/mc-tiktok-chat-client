package br.com.reinodoce.mctiktok.client.pinned;

import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PinnedMessageStoreTest {
    @Test
    void retainedMessagesAreBoundedByConfiguredCount() {
        PinnedMessageStore store = new PinnedMessageStore();

        store.add(1L, Component.literal("one"), Duration.ofSeconds(10), 2);
        store.add(2L, Component.literal("two"), Duration.ofSeconds(10), 2);
        store.add(3L, Component.literal("three"), Duration.ofSeconds(10), 2);

        assertEquals(2, store.snapshot(2).size());
        assertEquals("two", store.snapshot(2).get(0).component().getString());
        assertEquals("three", store.snapshot(2).get(1).component().getString());
    }

    @Test
    void messagesExpireAfterTheirDuration() {
        AtomicLong clock = new AtomicLong(1_000L);
        PinnedMessageStore store = new PinnedMessageStore(clock::get);
        store.add(1L, Component.literal("one"), Duration.ofSeconds(2), 3);

        clock.set(2_999L);
        assertEquals(1, store.snapshot(3).size());

        clock.set(3_000L);
        assertEquals(0, store.snapshot(3).size());
    }

    @Test
    void matchingPinIdReplacesExistingEntry() {
        PinnedMessageStore store = new PinnedMessageStore();

        store.add(42L, Component.literal("old"), Duration.ofSeconds(10), 3);
        store.add(42L, Component.literal("new"), Duration.ofSeconds(10), 3);

        assertEquals(1, store.snapshot(3).size());
        assertEquals("new", store.snapshot(3).get(0).component().getString());
    }
}
