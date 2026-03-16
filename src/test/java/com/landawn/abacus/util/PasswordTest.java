package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Set;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.AbstractTest;

@Tag("old-test")
public class PasswordTest extends AbstractTest {

    @Test
    public void testMD5Encryption() {
        Password pwd = new Password("MD5");
        String st = "abcksfe里飞×……￥%……&×（!@#$%^&*()_@#$^&*()_)(*&）23ro lseif";
        String encryptedPassword = pwd.encrypt(st);
        N.println(encryptedPassword);
        assertTrue(pwd.isEqual(st, encryptedPassword));
        assertFalse(pwd.isEqual(st + " ", encryptedPassword));
    }

    @Test
    public void testSHA256Encryption() {
        Password pwd = new Password("SHA-256");
        String st = "abcksfe里飞×……￥%……&×（!@#$%^&*()_@#$^&*()_)(*&）23ro lseif";
        String encryptedPassword = pwd.encrypt(st);
        N.println(encryptedPassword);
        assertTrue(pwd.isEqual(st, encryptedPassword));
        assertFalse(pwd.isEqual(st + " ", encryptedPassword));

        N.println(pwd.toString());

        Password pwd2 = new Password("SHA-256");
        Set<Password> set = CommonUtil.toSet(pwd);
        assertTrue(set.contains(pwd2));

        assertEquals("SHA-256", pwd2.getAlgorithm());

        try {
            new Password("none");
            fail("Should throw RuntimeException");
        } catch (RuntimeException e) {
        }
    }

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
