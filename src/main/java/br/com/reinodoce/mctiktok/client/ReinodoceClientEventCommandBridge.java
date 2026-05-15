package br.com.reinodoce.mctiktok.client;

import br.com.reinodoce.mctiktok.alert.AlertEventType;
import br.com.reinodoce.mctiktok.command.CommandResult;

/**
 * Delegates synthetic event and alert command actions for the client command service.
 */
// Command-service bridge intentionally exposes one method per public event action.
@SuppressWarnings("PMD.ExcessivePublicCount")
abstract class ReinodoceClientEventCommandBridge extends ReinodoceClientRuleCommandBridge {
    @Override
    public CommandResult setSyntheticGift(int value) {
        return coreService().setSyntheticGift(value);
    }

    @Override
    public CommandResult setSyntheticGiftComboMode(String mode) {
        return coreService().setSyntheticGiftComboMode(mode);
    }

    @Override
    public CommandResult setSyntheticFollow(boolean enabled) {
        return coreService().setSyntheticFollow(enabled);
    }

    @Override
    public CommandResult setSyntheticJoin(boolean enabled) {
        return coreService().setSyntheticJoin(enabled);
    }

    @Override
    public CommandResult setSyntheticMemberLevel(boolean enabled) {
        return coreService().setSyntheticMemberLevel(enabled);
    }

    @Override
    public CommandResult setAlertSound(AlertEventType eventType, boolean enabled) {
        return coreService().setAlertSound(eventType, enabled);
    }

    @Override
    public CommandResult setAlertSoundId(AlertEventType eventType, String soundId) {
        return coreService().setAlertSoundId(eventType, soundId);
    }

    @Override
    public CommandResult setAlertToast(AlertEventType eventType, boolean enabled) {
        return coreService().setAlertToast(eventType, enabled);
    }

    @Override
    public CommandResult setAlertToastTemplate(AlertEventType eventType, String template) {
        return coreService().setAlertToastTemplate(eventType, template);
    }

    @Override
    public CommandResult setAlertMediaMode(AlertEventType eventType, String mediaMode) {
        return coreService().setAlertMediaMode(eventType, mediaMode);
    }

    @Override
    public CommandResult setAlertCustomImage(AlertEventType eventType, String customImage) {
        return coreService().setAlertCustomImage(eventType, customImage);
    }

    @Override
    public CommandResult setAlertGiftMinValue(int value) {
        return coreService().setAlertGiftMinValue(value);
    }
}
