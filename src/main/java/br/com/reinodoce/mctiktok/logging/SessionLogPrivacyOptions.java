package br.com.reinodoce.mctiktok.logging;

import br.com.reinodoce.mctiktok.config.ReinodoceConfig;

/**
 * Privacy and retention settings applied to local session logs.
 *
 * @param anonymized mask username fields
 * @param metadataOnly omit message body fields
 * @param retentionDays maximum log age, or zero when disabled
 * @param retentionFiles maximum retained log files, or zero when disabled
 */
record SessionLogPrivacyOptions(
        boolean anonymized,
        boolean metadataOnly,
        int retentionDays,
        int retentionFiles
) {
    static SessionLogPrivacyOptions from(ReinodoceConfig config) {
        ReinodoceConfig safeConfig = config == null ? ReinodoceConfig.defaults() : config;
        return new SessionLogPrivacyOptions(
                safeConfig.isSessionLoggingAnonymized(),
                safeConfig.isSessionLoggingMetadataOnly(),
                safeConfig.getSessionLoggingRetentionDays(),
                safeConfig.getSessionLoggingRetentionFiles());
    }
}
