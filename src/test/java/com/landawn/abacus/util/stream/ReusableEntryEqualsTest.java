package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.util.AbstractMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class ReusableEntryEqualsTest extends TestBase {

    @Test
    public void testEqualsDoesNotResetFlag() throws Exception {
        // Get the ReusableEntry class via reflection
        Class<?> reusableEntryClass = Class.forName("com.landawn.abacus.util.stream.EntryStream$ReusableEntry");

        // Create a ReusableEntry instance
        Object entry = reusableEntryClass.getDeclaredConstructor().newInstance();

        // Get the set method and call it
        var setMethod = reusableEntryClass.getMethod("set", Object.class, Object.class);
        setMethod.invoke(entry, "key1", "value1");

        // Now call equals with another entry
        Map.Entry<String, String> otherEntry = new AbstractMap.SimpleEntry<>("key1", "value1");
        var equalsMethod = reusableEntryClass.getMethod("equals", Object.class);
        boolean result = (boolean) equalsMethod.invoke(entry, otherEntry);
        assertTrue(result, "equals() should return true");

        // Reflection wraps the target IllegalStateException in InvocationTargetException.
        InvocationTargetException thrown = assertThrows(InvocationTargetException.class, () -> {
            setMethod.invoke(entry, "key2", "value2");
        }, "set() after equals() should throw IllegalStateException if flag wasn't reset by equals()");
        assertTrue(thrown.getCause() instanceof IllegalStateException);
    }

    @Test
    public void testToStringResetsFlag() throws Exception {
        assertDoesNotThrow(() -> {
            Class<?> reusableEntryClass = Class.forName("com.landawn.abacus.util.stream.EntryStream$ReusableEntry");
            Object entry = reusableEntryClass.getDeclaredConstructor().newInstance();

            var setMethod = reusableEntryClass.getMethod("set", Object.class, Object.class);
            setMethod.invoke(entry, "key1", "value1");

            // Call toString - should reset flag
            var toStringMethod = reusableEntryClass.getMethod("toString");
            String str = (String) toStringMethod.invoke(entry);

            // Now set should work
            setMethod.invoke(entry, "key2", "value2");
        });
    }

    @Test
    public void testEqualsDoesNotResetPeerReusableEntryFlag() throws Exception {
        Class<?> reusableEntryClass = Class.forName("com.landawn.abacus.util.stream.EntryStream$ReusableEntry");
        Object a = reusableEntryClass.getDeclaredConstructor().newInstance();
        Object b = reusableEntryClass.getDeclaredConstructor().newInstance();
        var setMethod = reusableEntryClass.getMethod("set", Object.class, Object.class);
        setMethod.invoke(a, "k", "v");
        setMethod.invoke(b, "k", "v");

        var equalsMethod = reusableEntryClass.getMethod("equals", Object.class);
        assertTrue((boolean) equalsMethod.invoke(a, b));

        InvocationTargetException thrown = assertThrows(InvocationTargetException.class, () -> setMethod.invoke(b, "k2", "v2"));
        assertTrue(thrown.getCause() instanceof IllegalStateException);
    }
}
