package br.com.reinodoce.mctiktok.tiktok;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModerationDuplicateTrackerTest {
    private static final String SAME_MESSAGE = "same";

    @Test
    void suppressesRepeatedMessagesWithinCooldown() {
        ModerationDuplicateTracker tracker = new ModerationDuplicateTracker();

        assertFalse(tracker.isDuplicate("Same message", 3));
        tracker.remember("Same message", 3);
        assertTrue(tracker.isDuplicate(" same MESSAGE ", 3));
        assertFalse(tracker.isDuplicate("Same message", 0));
    }

    @Test
    void disablingCooldownClearsRememberedMessages() {
        ModerationDuplicateTracker tracker = new ModerationDuplicateTracker();

        tracker.remember(SAME_MESSAGE, 3);
        assertTrue(tracker.isDuplicate(SAME_MESSAGE, 3));
        assertFalse(tracker.isDuplicate(SAME_MESSAGE, 0));
        assertFalse(tracker.isDuplicate(SAME_MESSAGE, 3));
    }
}
