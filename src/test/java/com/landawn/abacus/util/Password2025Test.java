package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class Password2025Test extends TestBase {

    @Test
    public void testGetAlgorithm() {
        Password password = new Password("SHA-256");
        assertEquals("SHA-256", password.getAlgorithm());
    }

    @Test
    public void testEncrypt_notNull() {
        Password password = new Password("SHA-256");
        String encrypted = password.encrypt("testPassword");
        assertNotNull(encrypted);
    }

    @Test
    public void testEncrypt_null() {
        Password password = new Password("SHA-256");
        String encrypted = password.encrypt(null);
        assertNull(encrypted);
    }

    @Test
    public void testEncrypt_consistent() {
        Password password = new Password("SHA-256");
        String encrypted1 = password.encrypt("testPassword");
        String encrypted2 = password.encrypt("testPassword");
        assertEquals(encrypted1, encrypted2);
    }

    @Test
    public void testIsEqual_match() {
        Password password = new Password("SHA-256");
        String encrypted = password.encrypt("testPassword");
        assertTrue(password.isEqual("testPassword", encrypted));
    }

    @Test
    public void testIsEqual_noMatch() {
        Password password = new Password("SHA-256");
        String encrypted = password.encrypt("testPassword");
        assertFalse(password.isEqual("wrongPassword", encrypted));
    }

    @Test
    public void testIsEqual_bothNull() {
        Password password = new Password("SHA-256");
        assertTrue(password.isEqual(null, null));
    }

    @Test
    public void testIsEqual_plainNull() {
        Password password = new Password("SHA-256");
        assertFalse(password.isEqual(null, "encrypted"));
    }

    @Test
    public void testEquals_sameAlgorithm() {
        Password p1 = new Password("SHA-256");
        Password p2 = new Password("SHA-256");
        assertEquals(p1, p2);
    }

    @Test
    public void testEquals_differentAlgorithm() {
        Password p1 = new Password("SHA-256");
        Password p2 = new Password("MD5");
        assertNotEquals(p1, p2);
    }

    @Test
    public void testHashCode_consistent() {
        Password password = new Password("SHA-256");
        assertEquals(password.hashCode(), password.hashCode());
    }

    @Test
    public void testToString() {
        Password password = new Password("SHA-256");
        String str = password.toString();
        assertTrue(str.contains("SHA-256"));
    }
}
