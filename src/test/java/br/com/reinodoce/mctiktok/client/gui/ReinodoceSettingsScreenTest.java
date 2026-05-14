package br.com.reinodoce.mctiktok.client.gui;

import br.com.reinodoce.mctiktok.alert.AlertEventType;
import br.com.reinodoce.mctiktok.alert.AlertSoundId;
import br.com.reinodoce.mctiktok.config.ReinodoceConfig;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReinodoceSettingsScreenTest {
    @Test
    void parseCsvListTrimsAndDropsBlankEntries() {
        assertEquals(List.of("spam", "caps", "alice"),
                ReinodoceSettingsScreen.parseCsvList(" spam, , caps ,, alice "));
    }

    @Test
    void joinCsvUsesStableCommaSpacing() {
        assertEquals("spam, caps, alice", ReinodoceSettingsScreen.joinCsv(List.of("spam", "caps", "alice")));
    }

    @Test
    void textDraftKeepsInvalidIntermediateValueUntilSave() {
        SettingsTextDrafts textDrafts = new SettingsTextDrafts();
        ReinodoceConfig config = ReinodoceConfig.defaults();
        String key = ReinodoceSettingsScreen.textKey(ReinodoceSettingsPages.Page.ALERT_GIFT, "Sound id");

        assertEquals(AlertSoundId.DEFAULT, textDrafts.value(
                key,
                () -> config.getAlertSoundId(AlertEventType.GIFT),
                value -> config.setAlertSoundId(AlertEventType.GIFT, value)));
        textDrafts.update(key, "minecraft:");

        assertEquals("minecraft:", textDrafts.value(
                key,
                () -> config.getAlertSoundId(AlertEventType.GIFT),
                value -> config.setAlertSoundId(AlertEventType.GIFT, value)));
        assertEquals(AlertSoundId.DEFAULT, config.getAlertSoundId(AlertEventType.GIFT));

        textDrafts.apply();

        assertEquals(AlertSoundId.DEFAULT, config.getAlertSoundId(AlertEventType.GIFT));
    }

    @Test
    void textDraftAppliesValidValueOnlyWhenSaved() {
        SettingsTextDrafts textDrafts = new SettingsTextDrafts();
        ReinodoceConfig config = ReinodoceConfig.defaults();
        String key = ReinodoceSettingsScreen.textKey(ReinodoceSettingsPages.Page.ALERT_GIFT, "Sound id");
        String customSound = "minecraft:entity.experience_orb.pickup";

        textDrafts.value(
                key,
                () -> config.getAlertSoundId(AlertEventType.GIFT),
                value -> config.setAlertSoundId(AlertEventType.GIFT, value));
        textDrafts.update(key, customSound);

        assertEquals(AlertSoundId.DEFAULT, config.getAlertSoundId(AlertEventType.GIFT));

        textDrafts.apply();

        assertEquals(customSound, config.getAlertSoundId(AlertEventType.GIFT));
    }
}
