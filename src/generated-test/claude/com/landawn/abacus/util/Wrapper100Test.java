package com.landawn.abacus.util;


import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.ToIntFunction;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class Wrapper100Test extends TestBase {

    @Test
    public void testOfArray() {
        int[] array = { 1, 2, 3 };
        Wrapper<int[]> wrapper = Wrapper.of(array);

        Assertions.assertNotNull(wrapper);
        Assertions.assertArrayEquals(array, wrapper.value());
    }

    @Test
    public void testOfNullArray() {
        Wrapper<Object> wrapper = Wrapper.of(null);
        Assertions.assertNotNull(wrapper);
        Assertions.assertNull(wrapper.value());
    }

    @Test
    public void testOfEmptyArray() {
        int[] emptyArray = new int[0];
        Wrapper<int[]> wrapper1 = Wrapper.of(emptyArray);
        Wrapper<int[]> wrapper2 = Wrapper.of(new int[0]);

        // Empty arrays of the same type should return the same wrapper instance
        Assertions.assertSame(wrapper1, wrapper2);
    }

    @Test
    public void testOfWithCustomFunctions() {
        String value = "test";
        ToIntFunction<String> hashFunction = String::length;
        BiPredicate<String, String> equalsFunction = String::equalsIgnoreCase;

        Wrapper<String> wrapper = Wrapper.of(value, hashFunction, equalsFunction);

        Assertions.assertEquals(value, wrapper.value());
        Assertions.assertEquals(4, wrapper.hashCode()); // length of "test"
    }

    @Test
    public void testOfWithAllFunctions() {
        String value = "test";
        ToIntFunction<String> hashFunction = String::length;
        BiPredicate<String, String> equalsFunction = String::equalsIgnoreCase;
        Function<String, String> toStringFunction = s -> "Custom: " + s;

        Wrapper<String> wrapper = Wrapper.of(value, hashFunction, equalsFunction, toStringFunction);

        Assertions.assertEquals(value, wrapper.value());
        Assertions.assertTrue(wrapper.toString().contains("Custom: test"));
    }

    @Test
    public void testOfWithNullFunctions() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Wrapper.of("test", null, (a, b) -> true);
        });

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Wrapper.of("test", String::hashCode, null);
        });

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Wrapper.of("test", String::hashCode, String::equals, null);
        });
    }

    @Test
    public void testValue() {
        String value = "test";
        Wrapper<String> wrapper = Wrapper.of(value, String::hashCode, String::equals);

        Assertions.assertEquals(value, wrapper.value());
    }

    @Test
    public void testHashCodeArray() {
        int[] array1 = { 1, 2, 3 };
        int[] array2 = { 1, 2, 3 };

        Wrapper<int[]> wrapper1 = Wrapper.of(array1);
        Wrapper<int[]> wrapper2 = Wrapper.of(array2);

        // Arrays with same content should have same hash code
        Assertions.assertEquals(wrapper1.hashCode(), wrapper2.hashCode());
    }

    @Test
    public void testHashCodeCustom() {
        String value = "test";
        ToIntFunction<String> hashFunction = s -> 42; // Always return 42
        BiPredicate<String, String> equalsFunction = String::equals;

        Wrapper<String> wrapper = Wrapper.of(value, hashFunction, equalsFunction);

        Assertions.assertEquals(42, wrapper.hashCode());
    }

    @Test
    public void testEqualsArray() {
        int[] array1 = { 1, 2, 3 };
        int[] array2 = { 1, 2, 3 };
        int[] array3 = { 1, 2, 4 };

        Wrapper<int[]> wrapper1 = Wrapper.of(array1);
        Wrapper<int[]> wrapper2 = Wrapper.of(array2);
        Wrapper<int[]> wrapper3 = Wrapper.of(array3);

        Assertions.assertEquals(wrapper1, wrapper2);
        Assertions.assertNotEquals(wrapper1, wrapper3);
        Assertions.assertEquals(wrapper1, wrapper1);
        Assertions.assertNotEquals(wrapper1, null);
        Assertions.assertNotEquals(wrapper1, "string");
    }

    @Test
    public void testEqualsCustom() {
        ToIntFunction<String> hashFunction = String::length;
        BiPredicate<String, String> equalsFunction = String::equalsIgnoreCase;

        Wrapper<String> wrapper1 = Wrapper.of("TEST", hashFunction, equalsFunction);
        Wrapper<String> wrapper2 = Wrapper.of("test", hashFunction, equalsFunction);
        Wrapper<String> wrapper3 = Wrapper.of("other", hashFunction, equalsFunction);

        Assertions.assertEquals(wrapper1, wrapper2); // Case insensitive
        Assertions.assertNotEquals(wrapper1, wrapper3);
    }

    @Test
    public void testToString() {
        int[] array = { 1, 2, 3 };
        Wrapper<int[]> wrapper = Wrapper.of(array);

        String str = wrapper.toString();
        Assertions.assertTrue(str.contains("Wrapper"));
        Assertions.assertTrue(str.contains("[1, 2, 3]"));
    }

    @Test
    public void testToStringCustom() {
        Function<String, String> toStringFunction = s -> "Value is: " + s;
        Wrapper<String> wrapper = Wrapper.of("test", String::hashCode, String::equals, toStringFunction);

        String str = wrapper.toString();
        Assertions.assertTrue(str.contains("Value is: test"));
    }
}
