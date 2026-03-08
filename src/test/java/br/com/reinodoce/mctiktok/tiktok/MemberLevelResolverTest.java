package br.com.reinodoce.mctiktok.tiktok;

import io.github.jwdeveloper.tiktok.data.models.users.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MemberLevelResolverTest {

    @Test
    void extractLevelFromTextFindsFirstNumber() {
        assertEquals(12, MemberLevelResolver.extractLevelFromText("Nivel 12 de membro"));
        assertEquals(0, MemberLevelResolver.extractLevelFromText("sem numero"));
        assertEquals(0, MemberLevelResolver.extractLevelFromText(null));
    }

    @Test
    void updateLevelOnlyMarksRealUpgrade() {
        MemberLevelResolver resolver = new MemberLevelResolver();

        MemberLevelResolver.LevelUpdate first = resolver.updateLevel(1L, "alice", "avatar://alice", 2);
        MemberLevelResolver.LevelUpdate downgrade = resolver.updateLevel(1L, "alice", "", 1);
        MemberLevelResolver.LevelUpdate upgrade = resolver.updateLevel(1L, "alice", "", 4);

        assertTrue(first.isUpgrade());
        assertFalse(downgrade.isUpgrade());
        assertEquals(2, downgrade.newLevel());
        assertEquals("avatar://alice", downgrade.avatarUrl());
        assertTrue(upgrade.isUpgrade());
        assertEquals(4, upgrade.newLevel());
    }

    @Test
    void resolveLevelUsesZeroWithoutCacheOrBadges() {
        MemberLevelResolver resolver = new MemberLevelResolver();
        User user = new User(15L, "viewer");

        assertEquals(0, resolver.resolveLevel(user));
        resolver.updateLevel(15L, "viewer", "avatar://viewer", 3);
        assertEquals(3, resolver.resolveLevel(user));
    }
}
