package br.com.reinodoce.mctiktok.tiktok;

import br.com.reinodoce.mctiktok.util.InlineMediaUrls;
import io.github.jwdeveloper.tiktok.data.models.users.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TikTokMediaResolverTest {

    @Test
    void fallsBackToDefaultAvatarWhenUserHasNoPicture() {
        User user = new User(42L, "viewer");

        assertEquals(InlineMediaUrls.defaultAvatarUrl(), TikTokMediaResolver.resolveUserAvatarUrl(user));
    }
}
