package br.com.reinodoce.mctiktok.tiktok;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModerationDuplicateTrackerTest {

    @Test
    void suppressesRepeatedMessagesWithinCooldown() {
        ModerationDuplicateTracker tracker = new ModerationDuplicateTracker();

        assertFalse(tracker.isDuplicate("Same message", 3));
        assertTrue(tracker.isDuplicate(" same MESSAGE ", 3));
        assertFalse(tracker.isDuplicate("Same message", 0));
    }
}
