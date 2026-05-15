package br.com.reinodoce.mctiktok.client;

import br.com.reinodoce.mctiktok.command.CommandResult;

/**
 * Delegates privacy and logging command actions for the client command service.
 */
abstract class ReinodoceClientPrivacyCommandBridge extends ReinodoceClientCommandBridge {
    @Override
    public CommandResult setChatEmotesEnabled(boolean enabled) {
        return coreService().setChatEmotesEnabled(enabled);
    }

    @Override
    public CommandResult setChatLogEnabled(boolean enabled) {
        return coreService().setChatLogEnabled(enabled);
    }

    @Override
    public CommandResult setMaskUsernamesInOutput(boolean enabled) {
        return coreService().setMaskUsernamesInOutput(enabled);
    }

    @Override
    public CommandResult setChatPrefix(String prefix) {
        return coreService().setChatPrefix(prefix);
    }

    @Override
    public CommandResult setChatFormat(String format) {
        return coreService().setChatFormat(format);
    }

    @Override
    public CommandResult setSessionLoggingEnabled(boolean enabled) {
        return coreService().setSessionLoggingEnabled(enabled);
    }

    @Override
    public CommandResult setSessionLoggingFormat(String format) {
        return coreService().setSessionLoggingFormat(format);
    }

    @Override
    public CommandResult setSessionLoggingRetentionDays(int days) {
        return coreService().setSessionLoggingRetentionDays(days);
    }

    @Override
    public CommandResult setSessionLoggingRetentionFiles(int files) {
        return coreService().setSessionLoggingRetentionFiles(files);
    }

    @Override
    public CommandResult setSessionLoggingAnonymized(boolean enabled) {
        return coreService().setSessionLoggingAnonymized(enabled);
    }

    @Override
    public CommandResult setSessionLoggingMetadataOnly(boolean enabled) {
        return coreService().setSessionLoggingMetadataOnly(enabled);
    }
}
