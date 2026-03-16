package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/**
 * Tests for the documentation bugfix of Maps.putIfAbsent.
 *
 * Bug: The Javadoc examples claimed putIfAbsent returns null when the key already has
 * a non-null value, but the code actually returns the existing value.
 * The documentation was fixed to match the actual (correct) code behavior.
 */
@Tag("new-test")
public class MapsBugfixTest extends TestBase {

    /**
     * When the key already has a non-null value, putIfAbsent should return
     * the existing value (not null as the old docs claimed).
     */
    /**
    * When the key has a null value, putIfAbsent should put the new value
    * and return null (the previous null value).
    */
    /**
    * When the key is absent, putIfAbsent should put the new value
    * and return null (no previous mapping).
    */
    /**
    * Supplier overload: when the key already has a non-null value,
    * the supplier should NOT be called and the existing value should be returned.
    */
    @Test
    @DisplayName("putIfAbsent(Supplier) returns existing value and doesn't call supplier")
    public void test_putIfAbsent_supplier_existingNonNullValue() {
        Map<String, String> map = new HashMap<>();
        map.put("key1", "existing");

        Supplier<String> supplier = () -> "new_value";
        String result = Maps.putIfAbsent(map, "key1", supplier);

        assertEquals("existing", result, "Should return the existing non-null value");
        assertEquals("existing", map.get("key1"), "Map should not be modified");
    }

    /**
     * Supplier overload: when the key is absent, the supplier should be called
     * and the return value should be null.
     */
    @Test
    @DisplayName("putIfAbsent(Supplier) calls supplier and returns null when key is absent")
    public void test_putIfAbsent_supplier_absentKey() {
        Map<String, String> map = new HashMap<>();

        Supplier<String> supplier = () -> "supplied_value";
        String result = Maps.putIfAbsent(map, "key2", supplier);

        assertNull(result, "Should return null (no previous mapping)");
        assertEquals("supplied_value", map.get("key2"), "Map should now have the supplier's value");
    }

    /**
     * Supplier overload: when the key has null value, supplier should be called.
     */
    @Test
    @DisplayName("putIfAbsent(Supplier) calls supplier when key has null mapping")
    public void test_putIfAbsent_supplier_nullValue() {
        Map<String, String> map = new HashMap<>();
        map.put("key1", null);

        Supplier<String> supplier = () -> "supplied_value";
        String result = Maps.putIfAbsent(map, "key1", supplier);

        assertNull(result, "Should return null (the previous null value)");
        assertEquals("supplied_value", map.get("key1"), "Map should now have the supplier's value");
    }
}
