package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

public class CommonUtilCheckStateTest extends CommonUtilTestSupport {

    @Test
    public void testCheckState() {
        assertDoesNotThrow(() -> CommonUtil.checkState(true));
        assertThrows(IllegalStateException.class, () -> CommonUtil.checkState(false));

        assertDoesNotThrow(() -> CommonUtil.checkState(true, "Error message"));
        IllegalStateException msg = assertThrows(IllegalStateException.class, () -> CommonUtil.checkState(false, "Error message"));
        assertEquals("Error message", msg.getMessage());
        assertEquals("123", assertThrows(IllegalStateException.class, () -> CommonUtil.checkState(false, 123)).getMessage());

        assertDoesNotThrow(() -> CommonUtil.checkState(true, "Message with %s", "arg"));
        assertEquals("Message with arg", assertThrows(IllegalStateException.class, () -> CommonUtil.checkState(false, "Message with {}", "arg")).getMessage());
        assertEquals("arg1 and %s: [arg2]",
                assertThrows(IllegalStateException.class, () -> CommonUtil.checkState(false, "{} and %s", "arg1", "arg2")).getMessage());
        assertEquals("arg1 and arg2", assertThrows(IllegalStateException.class, () -> CommonUtil.checkState(false, "%s and %s", "arg1", "arg2")).getMessage());
        assertEquals("No placeholder: [arg1, arg2]",
                assertThrows(IllegalStateException.class, () -> CommonUtil.checkState(false, "No placeholder", "arg1", "arg2")).getMessage());
        assertTrue(
                assertThrows(IllegalStateException.class, () -> CommonUtil.checkState(false, "State {} is invalid", "CLOSED")).getMessage().contains("CLOSED"));
        assertDoesNotThrow(() -> CommonUtil.checkState(true, "State {} is wrong", "ACTIVE"));
    }

    @Test
    public void testCheckState_primitiveTemplates() {
        assertDoesNotThrow(() -> CommonUtil.checkState(true, "Char: {}", 'a'));
        assertEquals("Char: a", assertThrows(IllegalStateException.class, () -> CommonUtil.checkState(false, "Char: {}", 'a')).getMessage());
        assertEquals("Int: 10", assertThrows(IllegalStateException.class, () -> CommonUtil.checkState(false, "Int: {}", 10)).getMessage());
        assertEquals("Long: 100", assertThrows(IllegalStateException.class, () -> CommonUtil.checkState(false, "Long: {}", 100L)).getMessage());
        assertEquals("Double: 3.14", assertThrows(IllegalStateException.class, () -> CommonUtil.checkState(false, "Double: {}", 3.14)).getMessage());
        assertEquals("Object: test", assertThrows(IllegalStateException.class, () -> CommonUtil.checkState(false, "Object: {}", "test")).getMessage());

        assertDoesNotThrow(() -> CommonUtil.checkState(true, "{}, {}", 'a', 'b'));
        assertEquals("a, b", assertThrows(IllegalStateException.class, () -> CommonUtil.checkState(false, "{}, {}", 'a', 'b')).getMessage());
        assertDoesNotThrow(() -> CommonUtil.checkState(true, "State error: %s %s", 'a', 42));
        assertDoesNotThrow(() -> CommonUtil.checkState(true, "State error: %s %s", 'a', 42L));
        assertDoesNotThrow(() -> CommonUtil.checkState(true, "State error: %s %s", 'a', 3.14));
        assertDoesNotThrow(() -> CommonUtil.checkState(true, "{}, {}", 10, "obj"));
        assertEquals("10, obj", assertThrows(IllegalStateException.class, () -> CommonUtil.checkState(false, "{}, {}", 10, "obj")).getMessage());
        assertDoesNotThrow(() -> CommonUtil.checkState(true, "values %s and %s", 10, 20));
        assertDoesNotThrow(() -> CommonUtil.checkState(true, "values %s, %s, %s", 1, 2, 3));
        assertDoesNotThrow(() -> CommonUtil.checkState(true, "values %s, %s, %s, %s", 1, 2, 3, 4));
        assertThrows(IllegalStateException.class, () -> CommonUtil.checkState(false, "State error: %s %s", 'a', 'b'));
        assertThrows(IllegalStateException.class, () -> CommonUtil.checkState(false, "State error: %s %s", 'a', 42));
        assertThrows(IllegalStateException.class, () -> CommonUtil.checkState(false, "State error: %s %s", 'a', 42L));
        assertThrows(IllegalStateException.class, () -> CommonUtil.checkState(false, "State error: %s %s", 'a', 3.14));
        assertThrows(IllegalStateException.class, () -> CommonUtil.checkState(false, "invalid %s and %s", 1, 2));
        assertThrows(IllegalStateException.class, () -> CommonUtil.checkState(false, "bad %s, %s, %s", "x", "y", "z"));
        assertThrows(IllegalStateException.class, () -> CommonUtil.checkState(false, "Value {} {} {} {}", "a", "b", "c", "d"));
    }

    @Test
    public void testCheckState_supplier() {
        Supplier<String> supplier = () -> "Lazy error message";
        assertDoesNotThrow(() -> CommonUtil.checkState(true, supplier));
        assertEquals("Supplier error message for state",
                assertThrows(IllegalStateException.class, () -> CommonUtil.checkState(false, () -> "Supplier error message for state")).getMessage());

        final boolean[] called = { false };
        assertDoesNotThrow(() -> CommonUtil.checkState(true, () -> {
            called[0] = true;
            return "Should not be called";
        }));
        assertFalse(called[0]);
        assertThrows(IllegalStateException.class, () -> CommonUtil.checkState(false, () -> {
            called[0] = true;
            return "Should be called";
        }));
        assertTrue(called[0]);
    }
}
