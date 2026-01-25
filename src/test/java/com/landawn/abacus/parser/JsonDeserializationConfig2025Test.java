package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class JsonDeserializationConfig2025Test extends TestBase {
    @Test
    public void test_equals() {
        JsonDeserializationConfig config1 = new JsonDeserializationConfig();
        JsonDeserializationConfig config2 = new JsonDeserializationConfig();

        assertTrue(config1.equals(config1));
        assertTrue(config1.equals(config2));
        assertTrue(config2.equals(config1));

        config2.ignoreNullOrEmpty(true);
        assertFalse(config1.equals(config2));

        config2.ignoreNullOrEmpty(false);
        assertTrue(config1.equals(config2));

        config2.readNullToEmpty(true);
        assertFalse(config1.equals(config2));

        assertFalse(config1.equals(null));
        assertFalse(config1.equals("not a config"));
    }

    @Test
    public void test_hashCode() {
        JsonDeserializationConfig config1 = new JsonDeserializationConfig();
        JsonDeserializationConfig config2 = new JsonDeserializationConfig();

        assertEquals(config1.hashCode(), config2.hashCode());

        config2.ignoreNullOrEmpty(true);
        assertNotEquals(config1.hashCode(), config2.hashCode());
    }

    @Test
    public void test_toString() {
        JsonDeserializationConfig config = new JsonDeserializationConfig();
        String str = config.toString();
        assertNotNull(str);
        assertTrue(str.contains("ignoreNullOrEmpty"));
        assertTrue(str.contains("readNullToEmpty"));
        assertTrue(str.contains("mapInstanceType"));
    }
}
