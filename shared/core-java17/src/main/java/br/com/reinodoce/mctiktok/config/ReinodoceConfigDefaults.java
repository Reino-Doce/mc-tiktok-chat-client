package br.com.reinodoce.mctiktok.config;

public final class ReinodoceConfigDefaults {
    private ReinodoceConfigDefaults() {
    }

    public static ReinodoceConfig create() {
        ReinodoceConfig config = new ReinodoceConfig();
        config.setLastUsername("");
        config.setReconnectSeconds(5);
        config.setRuleFollowerOnly(false);
        config.setRuleMinMemberLevel(0);
        config.setSynteticGiftMinValue(1);
        config.setSynteticGiftComboMode("bulk");
        config.setSynteticFollowEnabled(true);
        config.setSynteticJoinEnabled(true);
        config.setSynteticMemberLevelEnabled(true);
        config.setChatEmotesEnabled(true);
        config.setChatPrefix("[LIVE]");
        return config;
    }
}
