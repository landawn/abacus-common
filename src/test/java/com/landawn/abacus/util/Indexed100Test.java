package com.landawn.abacus.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class Indexed100Test extends TestBase {

    @Test
    public void testOfWithValueAndIntIndex() {
        Indexed<String> stringIndexed = Indexed.of("Hello", 5);
        Assertions.assertEquals("Hello", stringIndexed.value());
        Assertions.assertEquals(5, stringIndexed.index());

        Indexed<Integer> intIndexed = Indexed.of(42, 0);
        Assertions.assertEquals(42, intIndexed.value());
        Assertions.assertEquals(0, intIndexed.index());

        Indexed<String> nullIndexed = Indexed.of(null, 10);
        Assertions.assertNull(nullIndexed.value());
        Assertions.assertEquals(10, nullIndexed.index());
    }

    @Test
    public void testOfWithValueAndLongIndex() {
        Indexed<String> indexed = Indexed.of("World", 1000000000000L);
        Assertions.assertEquals("World", indexed.value());
        Assertions.assertEquals(1000000000000L, indexed.longIndex());

        Indexed<Double> maxIndexed = Indexed.of(3.14, Long.MAX_VALUE);
        Assertions.assertEquals(3.14, maxIndexed.value());
        Assertions.assertEquals(Long.MAX_VALUE, maxIndexed.longIndex());
    }

    @Test
    public void testOfWithNegativeIndexThrowsException() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Indexed.of("test", -1);
        });

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Indexed.of("test", -1L);
        });
    }

    @Test
    public void testValue() {
        Indexed<String> indexed = Indexed.of("test value", 3);
        Assertions.assertEquals("test value", indexed.value());

        Indexed<Integer> intIndexed = Indexed.of(100, 5);
        Assertions.assertEquals(100, intIndexed.value());

        Indexed<Object> objIndexed = Indexed.of(new Object(), 7);
        Assertions.assertNotNull(objIndexed.value());
    }

    @Test
    public void testHashCode() {
        Indexed<String> indexed1 = Indexed.of("test", 5);
        Indexed<String> indexed2 = Indexed.of("test", 5);
        Indexed<String> indexed3 = Indexed.of("test", 6);
        Indexed<String> indexed4 = Indexed.of("different", 5);
        Indexed<String> indexed5 = Indexed.of(null, 5);

        Assertions.assertEquals(indexed1.hashCode(), indexed2.hashCode());

        Assertions.assertNotEquals(indexed1.hashCode(), indexed3.hashCode());

        Assertions.assertNotEquals(indexed1.hashCode(), indexed4.hashCode());

        int nullHash = indexed5.hashCode();
        Assertions.assertEquals((int) (5 * 31), nullHash);
    }

    @Test
    public void testEquals() {
        Indexed<String> indexed1 = Indexed.of("test", 5);
        Indexed<String> indexed2 = Indexed.of("test", 5);
        Indexed<String> indexed3 = Indexed.of("test", 6);
        Indexed<String> indexed4 = Indexed.of("different", 5);
        Indexed<Integer> indexed5 = Indexed.of(5, 5);
        Indexed<String> indexed6 = Indexed.of(null, 5);
        Indexed<String> indexed7 = Indexed.of(null, 5);

        Assertions.assertTrue(indexed1.equals(indexed1));

        Assertions.assertTrue(indexed1.equals(indexed2));
        Assertions.assertTrue(indexed2.equals(indexed1));

        Assertions.assertFalse(indexed1.equals(indexed3));

        Assertions.assertFalse(indexed1.equals(indexed4));

        Assertions.assertFalse(indexed1.equals(indexed5));

        Assertions.assertFalse(indexed1.equals(null));

        Assertions.assertFalse(indexed1.equals("not an Indexed"));

        Assertions.assertTrue(indexed6.equals(indexed7));
        Assertions.assertFalse(indexed1.equals(indexed6));
    }

    @Test
    public void testToString() {
        Indexed<String> stringIndexed = Indexed.of("Hello", 5);
        Assertions.assertEquals("[5]=Hello", stringIndexed.toString());

        Indexed<Integer> intIndexed = Indexed.of(42, 10);
        Assertions.assertEquals("[10]=42", intIndexed.toString());

        Indexed<String> nullIndexed = Indexed.of(null, 3);
        String nullStr = nullIndexed.toString();
        Assertions.assertTrue(nullStr.startsWith("[3]="));

        Indexed<String> longIndexed = Indexed.of("test", 9999999999L);
        Assertions.assertEquals("[9999999999]=test", longIndexed.toString());
    }

    @Test
    public void testWithComplexObjects() {
        Object customObj = new Object() {
            @Override
            public String toString() {
                return "CustomObject";
            }
        };

        Indexed<Object> objIndexed = Indexed.of(customObj, 7);
        Assertions.assertEquals(customObj, objIndexed.value());
        Assertions.assertEquals(7, objIndexed.index());
        Assertions.assertEquals("[7]=CustomObject", objIndexed.toString());
    }
}
