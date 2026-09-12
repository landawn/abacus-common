package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

public class CommonUtilCheckArgumentTest extends CommonUtilTestSupport {

    @Test
    public void testCheckArgument() {
        assertDoesNotThrow(() -> CommonUtil.checkArgument(true));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false));
    }

    @Test
    public void testCheckArgument_message() {
        assertDoesNotThrow(() -> CommonUtil.checkArgument(true, "Error message"));
        assertDoesNotThrow(() -> CommonUtil.checkArgument(true, 123));
        assertEquals("Error message", assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "Error message")).getMessage());
        assertEquals("123", assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, 123)).getMessage());
        assertEquals("", assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "")).getMessage());
        assertEquals("Special chars: \n\t\r",
                assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "Special chars: \n\t\r")).getMessage());
    }

    @Test
    public void testCheckArgument_template() {
        assertDoesNotThrow(() -> CommonUtil.checkArgument(true, "Message with %s", "arg"));
        assertDoesNotThrow(() -> CommonUtil.checkArgument(true, "Error message {} {}", "arg1", "arg2"));
        assertEquals("Message with arg",
                assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "Message with {}", "arg")).getMessage());
        assertEquals("arg1 and %s: [arg2]",
                assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "{} and %s", "arg1", "arg2")).getMessage());
        assertEquals("arg1 and arg2",
                assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "%s and %s", "arg1", "arg2")).getMessage());
        assertEquals("No placeholder: [arg1, arg2]",
                assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "No placeholder", "arg1", "arg2")).getMessage());
        assertEquals("One arg1 two: [arg2]",
                assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "One {} two", "arg1", "arg2")).getMessage());
        assertEquals("value is ''", assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "value is '%s'", "")).getMessage());
        Exception unicode = assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "Unicode: \u2605 %s", "\u2764"));
        assertTrue(unicode.getMessage().contains("\u2605"));
        assertTrue(unicode.getMessage().contains("\u2764"));
        assertEquals("value 1: [2, 3]", assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "value %s", 1, 2, 3)).getMessage());
        assertTrue(
                assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "value %s", 10, 20, 30)).getMessage().contains("[20, 30]"));
    }

    @Test
    public void testCheckArgument_oneArg() {
        assertDoesNotThrow(() -> CommonUtil.checkArgument(true, "Char: {}", 'a'));
        assertDoesNotThrow(() -> CommonUtil.checkArgument(true, "Int: {}", 10));
        assertDoesNotThrow(() -> CommonUtil.checkArgument(true, "Long: {}", 100L));
        assertDoesNotThrow(() -> CommonUtil.checkArgument(true, "Double: {}", 3.14));
        assertDoesNotThrow(() -> CommonUtil.checkArgument(true, "Object: {}", "test"));
        assertEquals("Char: a", assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "Char: {}", 'a')).getMessage());
        assertEquals("Int: 10", assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "Int: {}", 10)).getMessage());
        assertEquals("Long: 100", assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "Long: {}", 100L)).getMessage());
        assertEquals("Double: 3.14", assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "Double: {}", 3.14)).getMessage());
        assertEquals("Object: test", assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "Object: {}", "test")).getMessage());
        assertTrue(assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "Value is {}", (Object) null)).getMessage()
                .contains("null"));
    }

    @Test
    public void testCheckArgument_twoArgs() {
        assertDoesNotThrow(() -> {
            CommonUtil.checkArgument(true, "{}, {}", 'a', 'b');
            CommonUtil.checkArgument(true, "{}, {}", 'a', 42);
            CommonUtil.checkArgument(true, "{}, {}", 'a', 42L);
            CommonUtil.checkArgument(true, "{}, {}", 'a', 3.14);
            CommonUtil.checkArgument(true, "{}, {}", 'a', "test");
            CommonUtil.checkArgument(true, "{}, {}", 42, 'a');
            CommonUtil.checkArgument(true, "{}, {}", 42, 24);
            CommonUtil.checkArgument(true, "{}, {}", 42, 24L);
            CommonUtil.checkArgument(true, "{}, {}", 42, 3.14);
            CommonUtil.checkArgument(true, "{}, {}", 42, "test");
            CommonUtil.checkArgument(true, "{}, {}", 42L, 'a');
            CommonUtil.checkArgument(true, "{}, {}", 42L, 24);
            CommonUtil.checkArgument(true, "{}, {}", 42L, 24L);
            CommonUtil.checkArgument(true, "{}, {}", 42L, 3.14);
            CommonUtil.checkArgument(true, "{}, {}", 42L, "test");
            CommonUtil.checkArgument(true, "{}, {}", 3.14, 'a');
            CommonUtil.checkArgument(true, "{}, {}", 3.14, 42);
            CommonUtil.checkArgument(true, "{}, {}", 3.14, 42L);
            CommonUtil.checkArgument(true, "{}, {}", 3.14, 2.71);
            CommonUtil.checkArgument(true, "{}, {}", 3.14, "test");
            CommonUtil.checkArgument(true, "{}, {}", "test", 'a');
            CommonUtil.checkArgument(true, "{}, {}", "test", 42);
            CommonUtil.checkArgument(true, "{}, {}", "test", 42L);
            CommonUtil.checkArgument(true, "{}, {}", "test", 3.14);
            CommonUtil.checkArgument(true, "{}, {}", "a", "b");
        });
        assertEquals("a, b", assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "{}, {}", 'a', 'b')).getMessage());
        assertEquals("a 42", assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "%s %s", 'a', 42)).getMessage());
        assertEquals("a 42", assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "%s %s", 'a', 42L)).getMessage());
        assertEquals("a 3.14", assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "%s %s", 'a', 3.14)).getMessage());
        assertEquals("a test", assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "%s %s", 'a', "test")).getMessage());
        assertEquals("42 a", assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "%s %s", 42, 'a')).getMessage());
        assertEquals("42 24", assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "%s %s", 42, 24)).getMessage());
        assertEquals("42 24", assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "%s %s", 42, 24L)).getMessage());
        assertEquals("42 3.14", assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "%s %s", 42, 3.14)).getMessage());
        assertEquals("42 test", assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "%s %s", 42, "test")).getMessage());
        assertEquals("42 a", assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "%s %s", 42L, 'a')).getMessage());
        assertEquals("42 24", assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "%s %s", 42L, 24)).getMessage());
        assertEquals("42 24", assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "%s %s", 42L, 24L)).getMessage());
        assertEquals("42 3.14", assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "%s %s", 42L, 3.14)).getMessage());
        assertEquals("42 test", assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "%s %s", 42L, "test")).getMessage());
        assertEquals("3.14 a", assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "%s %s", 3.14, 'a')).getMessage());
        assertEquals("3.14 42", assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "%s %s", 3.14, 42)).getMessage());
        assertEquals("3.14 42", assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "%s %s", 3.14, 42L)).getMessage());
        assertEquals("3.14 2.71", assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "%s %s", 3.14, 2.71)).getMessage());
        assertEquals("3.14 test", assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "%s %s", 3.14, "test")).getMessage());
        assertEquals("test a", assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "%s %s", "test", 'a')).getMessage());
        assertEquals("test 42", assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "%s %s", "test", 42)).getMessage());
        assertEquals("test 42", assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "%s %s", "test", 42L)).getMessage());
        assertEquals("test 3.14", assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "%s %s", "test", 3.14)).getMessage());
        assertEquals("x y", assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "%s %s", "x", "y")).getMessage());
        assertEquals("Value 10 should be less than 5",
                assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "Value {} should be less than {}", 10, 5)).getMessage());
        assertEquals("No placeholders: [10, 5]",
                assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "No placeholders", 10, 5)).getMessage());
        assertEquals("Only one 10: [5]",
                assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "Only one {}", 10, 5)).getMessage());
    }

    @Test
    public void testCheckArgument_threeAndFourArgs() {
        assertDoesNotThrow(() -> CommonUtil.checkArgument(true, "values are %s, %s, %s", "a", "b", "c"));
        assertDoesNotThrow(() -> CommonUtil.checkArgument(true, "values are %s, %s, %s, %s", "a", "b", "c", "d"));
        Exception three = assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "values %s, %s, %s", 1, 2, 3));
        assertTrue(three.getMessage().contains("1") && three.getMessage().contains("2") && three.getMessage().contains("3"));
        Exception four = assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "values %s, %s, %s, %s", 1, 2, 3, 4));
        assertTrue(four.getMessage().contains("1") && four.getMessage().contains("4"));
        assertEquals("No placeholder: [a, b, c]",
                assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "No placeholder", "a", "b", "c")).getMessage());
        assertEquals("No placeholder [a, b, c, d]",
                assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "No placeholder", "a", "b", "c", "d")).getMessage());
        assertTrue(assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, null, "arg1", "arg2")).getMessage().contains("null"));
    }

    @Test
    public void testCheckArgument_supplier() {
        Supplier<String> supplier = () -> "Supplier error message";
        assertDoesNotThrow(() -> CommonUtil.checkArgument(true, supplier));
        assertEquals("Supplier error message", assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, supplier)).getMessage());
        final boolean[] called = { false };
        Supplier<String> tracking = () -> {
            called[0] = true;
            return "Called";
        };
        assertDoesNotThrow(() -> CommonUtil.checkArgument(true, tracking));
        assertFalse(called[0]);
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, tracking));
        assertTrue(called[0]);
    }
}
