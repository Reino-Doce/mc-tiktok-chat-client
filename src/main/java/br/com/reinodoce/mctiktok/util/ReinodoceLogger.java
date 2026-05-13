package br.com.reinodoce.mctiktok.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shared logger holder for the mod.
 */
public final class ReinodoceLogger {
    /** Logger scoped to the mod package. */
    public static final Logger LOGGER = LoggerFactory.getLogger(ReinodoceLogger.class);

    private ReinodoceLogger() {
    }
}
