/*
 * Copyright (c) 2015, Haiyang Li. All rights reserved.
 */

package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Set;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.AbstractTest;

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
        Set<Password> set = CommonUtil.asSet(pwd);
        assertTrue(set.contains(pwd2));

        assertEquals("SHA-256", pwd2.getAlgorithm());

        try {
            new Password("none");
            fail("Should throw RuntimeException");
        } catch (RuntimeException e) {
        }
    }
}
