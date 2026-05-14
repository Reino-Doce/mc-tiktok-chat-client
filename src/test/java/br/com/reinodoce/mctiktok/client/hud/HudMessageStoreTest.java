package br.com.reinodoce.mctiktok.client.hud;

import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HudMessageStoreTest {
    @Test
    void retainedMessagesAreBoundedByConfiguredLineCount() {
        HudMessageStore store = new HudMessageStore();

        store.add(Component.literal("one"), 2);
        store.add(Component.literal("two"), 2);
        store.add(Component.literal("three"), 2);

        assertEquals(2, store.snapshot(2).size());
        assertEquals("two", store.snapshot(2).get(0).getString());
        assertEquals("three", store.snapshot(2).get(1).getString());
    }
}
