package br.com.reinodoce.mctiktok.tiktok;

import io.github.jwdeveloper.tiktok.data.models.users.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserCooldownTrackerTest {
    private static final String ALICE = "alice";

    @Test
    void cooldownStartsOnlyWhenRemembered() {
        UserCooldownTracker tracker = new UserCooldownTracker();

        assertFalse(tracker.isCoolingDown(null, ALICE, 3));
        assertFalse(tracker.isCoolingDown(null, ALICE, 3));

        tracker.remember(null, ALICE, 3);

        assertTrue(tracker.isCoolingDown(null, ALICE, 3));
    }

    @Test
    void disablingCooldownClearsRememberedUsers() {
        UserCooldownTracker tracker = new UserCooldownTracker();

        tracker.remember(null, ALICE, 3);

        assertFalse(tracker.isCoolingDown(null, ALICE, 0));
        assertFalse(tracker.isCoolingDown(null, ALICE, 3));
    }

    @Test
    void cooldownUsesStableUserIdBeforeDisplayName() {
        User user = new User(10L, "Alice");
        User sameUser = new User(10L, "renamed");
        User differentUser = new User(11L, "Alice");
        UserCooldownTracker tracker = new UserCooldownTracker();

        tracker.remember(user, "Display One", 3);

        assertTrue(tracker.isCoolingDown(sameUser, "Display Two", 3));
        assertFalse(tracker.isCoolingDown(differentUser, "Display One", 3));
    }

    @Test
    void cooldownFallbackHandleIsCaseInsensitive() {
        UserCooldownTracker tracker = new UserCooldownTracker();

        tracker.remember(null, "Alice", 3);

        assertTrue(tracker.isCoolingDown(null, "alice", 3));
    }
}
