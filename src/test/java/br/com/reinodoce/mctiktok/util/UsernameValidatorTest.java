package br.com.reinodoce.mctiktok.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UsernameValidatorTest {

    @Test
    void normalizeRemovesLeadingAtAndWhitespace() {
        assertEquals("user.name", UsernameValidator.normalize("  @@user.name  "));
        assertEquals("", UsernameValidator.normalize(null));
    }

    @Test
    void validationAcceptsAndRejectsExpectedFormats() {
        assertTrue(UsernameValidator.isValid("abc_123"));
        assertTrue(UsernameValidator.isValid("a.b"));
        assertFalse(UsernameValidator.isValid(""));
        assertFalse(UsernameValidator.isValid("a"));
        assertFalse(UsernameValidator.isValid("name-with-dash"));
        assertFalse(UsernameValidator.isValid("name with space"));
    }
}
