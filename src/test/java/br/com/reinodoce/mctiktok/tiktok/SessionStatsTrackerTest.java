package br.com.reinodoce.mctiktok.tiktok;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SessionStatsTrackerTest {
    private static final String ALICE = "alice";

    @Test
    void tracksMessagesAndUniqueChattersByStableUserId() {
        SessionStatsTracker tracker = new SessionStatsTracker();

        tracker.recordComment(42L, ALICE);
        tracker.recordComment(42L, "alice-renamed");
        tracker.recordComment(0L, "bob");
        tracker.recordComment(0L, "BOB");

        SessionStatsTracker.Snapshot snapshot = tracker.snapshot();

        assertEquals(4L, snapshot.messages());
        assertEquals(2, snapshot.uniqueChatters());
    }

    @Test
    void tracksGiftsDiamondsAndTopGifter() {
        SessionStatsTracker tracker = new SessionStatsTracker();

        tracker.recordGift(1L, ALICE, 2, 10);
        tracker.recordGift(2L, "bob", 1, 30);
        tracker.recordGift(1L, ALICE, 1, 5);

        SessionStatsTracker.Snapshot snapshot = tracker.snapshot();

        assertEquals(4L, snapshot.gifts());
        assertEquals(55L, snapshot.diamonds());
        assertEquals("bob", snapshot.topGifter());
        assertEquals(30L, snapshot.topGifterDiamonds());
    }

    @Test
    void tracksTopGifterByStableUserId() {
        SessionStatsTracker tracker = new SessionStatsTracker();

        tracker.recordGift(1L, ALICE, 1, 10);
        tracker.recordGift(1L, "alice-renamed", 1, 25);
        tracker.recordGift(0L, ALICE, 1, 20);

        SessionStatsTracker.Snapshot snapshot = tracker.snapshot();

        assertEquals(55L, snapshot.diamonds());
        assertEquals("alice-renamed", snapshot.topGifter());
        assertEquals(35L, snapshot.topGifterDiamonds());
    }

    @Test
    void resetClearsAllCounters() {
        SessionStatsTracker tracker = new SessionStatsTracker();
        tracker.recordComment(1L, ALICE);
        tracker.recordFollow();
        tracker.recordJoin();
        tracker.recordGift(1L, ALICE, 2, 10);
        tracker.recordMemberLevel();

        tracker.reset();
        SessionStatsTracker.Snapshot snapshot = tracker.snapshot();

        assertEquals(0L, snapshot.messages());
        assertEquals(0, snapshot.uniqueChatters());
        assertEquals(0L, snapshot.follows());
        assertEquals(0L, snapshot.joins());
        assertEquals(0L, snapshot.gifts());
        assertEquals(0L, snapshot.diamonds());
        assertEquals(0L, snapshot.memberLevels());
        assertEquals("", snapshot.topGifter());
    }
}
