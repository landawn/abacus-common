package com.landawn.abacus.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class Keyed100Test extends TestBase {

    @Test
    public void testOf() {
        String key = "key1";
        String value = "value1";

        Keyed<String, String> keyed = Keyed.of(key, value);

        Assertions.assertEquals(key, keyed.key());
        Assertions.assertEquals(value, keyed.val());
    }

    @Test
    public void testKey() {
        Integer key = 42;
        String value = "answer";

        Keyed<Integer, String> keyed = Keyed.of(key, value);
        Assertions.assertEquals(key, keyed.key());
    }

    @Test
    public void testVal() {
        String key = "name";
        Integer value = 123;

        Keyed<String, Integer> keyed = Keyed.of(key, value);
        Assertions.assertEquals(value, keyed.val());
    }

    @Test
    public void testHashCode() {
        Keyed<String, String> keyed1 = Keyed.of("key", "value1");
        Keyed<String, String> keyed2 = Keyed.of("key", "value2");
        Keyed<String, String> keyed3 = Keyed.of("otherKey", "value1");

        // Same key should have same hash code regardless of value
        Assertions.assertEquals(keyed1.hashCode(), keyed2.hashCode());

        // Different key should (likely) have different hash code
        Assertions.assertNotEquals(keyed1.hashCode(), keyed3.hashCode());
    }

    @Test
    public void testEquals() {
        Keyed<String, String> keyed1 = Keyed.of("key", "value1");
        Keyed<String, String> keyed2 = Keyed.of("key", "value2");
        Keyed<String, String> keyed3 = Keyed.of("otherKey", "value1");

        // Same key means equal, regardless of value
        Assertions.assertEquals(keyed1, keyed2);

        // Different key means not equal
        Assertions.assertNotEquals(keyed1, keyed3);

        // Self equality
        Assertions.assertEquals(keyed1, keyed1);

        // Null and different type
        Assertions.assertNotEquals(keyed1, null);
        Assertions.assertNotEquals(keyed1, "string");
    }

    @Test
    public void testToString() {
        Keyed<String, Integer> keyed = Keyed.of("age", 25);
        String str = keyed.toString();

        Assertions.assertTrue(str.contains("key=age"));
        Assertions.assertTrue(str.contains("val=25"));
    }
}