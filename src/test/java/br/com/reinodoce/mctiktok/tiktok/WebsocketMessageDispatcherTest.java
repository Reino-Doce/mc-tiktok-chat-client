package br.com.reinodoce.mctiktok.tiktok;

import io.github.jwdeveloper.tiktok.messages.data.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WebsocketMessageDispatcherTest {
    private static final long USER_ID = 42L;
    private static final String USERNAME = "alice";
    private static final String AVATAR_URL = "avatar://alice";
    private static final int FAN_LEVEL = 9;

    @Test
    void commentMemberLevelSeedsCacheFromRawFanClubLevel() {
        MemberLevelResolver resolver = new MemberLevelResolver();
        WebsocketMessageDispatcher dispatcher = new WebsocketMessageDispatcher(
                new TikTokRichMessageParser(), resolver, null, null, null, null);
        User rawUser = rawUserWithFanLevel(FAN_LEVEL);
        io.github.jwdeveloper.tiktok.data.models.users.User user =
                io.github.jwdeveloper.tiktok.data.models.users.User.map(rawUser);

        int memberLevel = dispatcher.resolveCommentMemberLevel(user, rawUser, USERNAME, AVATAR_URL);

        assertEquals(FAN_LEVEL, memberLevel);
        assertEquals(FAN_LEVEL, resolver.resolveLevel(user));
    }

    private static User rawUserWithFanLevel(int fanLevel) {
        return User.newBuilder()
                .setId(USER_ID)
                .setNickname(USERNAME)
                .setUsername(USERNAME)
                .setFansClubInfo(User.FansClubInfo.newBuilder()
                        .setFansLevel(fanLevel))
                .build();
    }
}
