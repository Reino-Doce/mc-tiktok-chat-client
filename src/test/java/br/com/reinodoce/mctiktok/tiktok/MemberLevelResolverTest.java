package br.com.reinodoce.mctiktok.tiktok;

import io.github.jwdeveloper.tiktok.data.models.badges.StringBadge;
import io.github.jwdeveloper.tiktok.data.models.users.User;
import io.github.jwdeveloper.tiktok.messages.data.BadgeStruct;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MemberLevelResolverTest {
    private static final String USERNAME = "alice";
    private static final String AVATAR_URL = "avatar://alice";

    @Test
    void extractLevelFromTextFindsFirstNumber() {
        assertEquals(12, MemberLevelResolver.extractLevelFromText("Nivel 12 de membro"));
        assertEquals(0, MemberLevelResolver.extractLevelFromText("sem numero"));
        assertEquals(0, MemberLevelResolver.extractLevelFromText("score 4000"));
        assertEquals(0, MemberLevelResolver.extractLevelFromText(null));
    }

    @Test
    void updateLevelOnlyMarksRealUpgrade() {
        MemberLevelResolver resolver = new MemberLevelResolver();

        MemberLevelResolver.LevelUpdate first = resolver.updateLevel(1L, USERNAME, AVATAR_URL, 2);
        MemberLevelResolver.LevelUpdate downgrade = resolver.updateLevel(1L, USERNAME, "", 1);
        MemberLevelResolver.LevelUpdate upgrade = resolver.updateLevel(1L, USERNAME, "", 4);

        assertFalse(first.isUpgrade());
        assertTrue(first.isLevelIncrease());
        assertFalse(downgrade.isUpgrade());
        assertFalse(downgrade.isLevelIncrease());
        assertEquals(2, downgrade.newLevel());
        assertEquals(AVATAR_URL, downgrade.avatarUrl());
        assertTrue(upgrade.isUpgrade());
        assertTrue(upgrade.isLevelIncrease());
        assertEquals(4, upgrade.newLevel());
    }

    @Test
    void updateLevelRejectsInvalidFanLevelAndKeepsPreviousValidLevel() {
        MemberLevelResolver resolver = new MemberLevelResolver();

        resolver.updateLevel(1L, USERNAME, AVATAR_URL, 4);
        MemberLevelResolver.LevelUpdate rejected = resolver.updateLevel(1L, USERNAME, "avatar://bad", 4000);

        assertFalse(rejected.isUpgrade());
        assertFalse(rejected.isLevelIncrease());
        assertEquals(4, rejected.previousLevel());
        assertEquals(4, rejected.newLevel());
        assertEquals(4, resolver.resolveLevel(new User(1L, USERNAME)));
        assertEquals(AVATAR_URL, resolver.getKnownAvatarUrl(1L, ""));
    }

    @Test
    void resolveLevelDoesNotInferFanLevelFromGenericBadgeNumbers() {
        MemberLevelResolver resolver = new MemberLevelResolver();
        User user = new User(
                16L,
                "viewer",
                null,
                0,
                0,
                List.of(new StringBadge(BadgeStruct.StringBadge.newBuilder()
                        .setStr("daily score 4000")
                        .build())));

        assertEquals(0, resolver.resolveLevel(user));
    }

    @Test
    void resolveRawUserLevelUsesFansClubInfoWhenValid() {
        io.github.jwdeveloper.tiktok.messages.data.User rawUser =
                io.github.jwdeveloper.tiktok.messages.data.User.newBuilder()
                        .setFansClubInfo(io.github.jwdeveloper.tiktok.messages.data.User.FansClubInfo.newBuilder()
                                .setFansLevel(9))
                        .setFansClub(io.github.jwdeveloper.tiktok.messages.data.FansClubMember.newBuilder()
                                .setData(io.github.jwdeveloper.tiktok.messages.data.FansClubData.newBuilder()
                                        .setLevel(7)))
                        .build();

        assertEquals(9, MemberLevelResolver.resolveRawUserLevel(rawUser));
    }

    @Test
    void resolveRawUserLevelFallsBackToFansClubDataWhenInfoInvalid() {
        io.github.jwdeveloper.tiktok.messages.data.User rawUser =
                io.github.jwdeveloper.tiktok.messages.data.User.newBuilder()
                        .setFansClubInfo(io.github.jwdeveloper.tiktok.messages.data.User.FansClubInfo.newBuilder()
                                .setFansLevel(4000))
                        .setFansClub(io.github.jwdeveloper.tiktok.messages.data.FansClubMember.newBuilder()
                                .setData(io.github.jwdeveloper.tiktok.messages.data.FansClubData.newBuilder()
                                        .setLevel(7)))
                        .build();

        assertEquals(7, MemberLevelResolver.resolveRawUserLevel(rawUser));
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
