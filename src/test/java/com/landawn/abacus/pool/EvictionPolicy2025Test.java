package com.landawn.abacus.pool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class EvictionPolicy2025Test extends TestBase {

    @Test
    public void testValues() {
        EvictionPolicy[] policies = EvictionPolicy.values();

        assertNotNull(policies);
        assertEquals(3, policies.length);
    }

    @Test
    public void testValueOf() {
        assertEquals(EvictionPolicy.LAST_ACCESS_TIME, EvictionPolicy.valueOf("LAST_ACCESS_TIME"));
        assertEquals(EvictionPolicy.EXPIRATION_TIME, EvictionPolicy.valueOf("EXPIRATION_TIME"));
        assertEquals(EvictionPolicy.ACCESS_COUNT, EvictionPolicy.valueOf("ACCESS_COUNT"));
    }

    @Test
    public void testLastAccessTimePolicy() {
        EvictionPolicy policy = EvictionPolicy.LAST_ACCESS_TIME;

        assertNotNull(policy);
        assertEquals("LAST_ACCESS_TIME", policy.name());
    }

    @Test
    public void testExpirationTimePolicy() {
        EvictionPolicy policy = EvictionPolicy.EXPIRATION_TIME;

        assertNotNull(policy);
        assertEquals("EXPIRATION_TIME", policy.name());
    }

    @Test
    public void testAccessCountPolicy() {
        EvictionPolicy policy = EvictionPolicy.ACCESS_COUNT;

        assertNotNull(policy);
        assertEquals("ACCESS_COUNT", policy.name());
    }

    @Test
    public void testPolicyOrdinal() {
        assertEquals(0, EvictionPolicy.LAST_ACCESS_TIME.ordinal());
        assertEquals(1, EvictionPolicy.EXPIRATION_TIME.ordinal());
        assertEquals(2, EvictionPolicy.ACCESS_COUNT.ordinal());
    }

    @Test
    public void testPolicyComparison() {
        assertEquals(EvictionPolicy.LAST_ACCESS_TIME, EvictionPolicy.values()[0]);
        assertEquals(EvictionPolicy.EXPIRATION_TIME, EvictionPolicy.values()[1]);
        assertEquals(EvictionPolicy.ACCESS_COUNT, EvictionPolicy.values()[2]);
    }
}
