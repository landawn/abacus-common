package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class IndexedChar2025Test extends TestBase {

    @Test
    public void testOfIntIndex() {
        IndexedChar indexed = IndexedChar.of('A', 5);
        assertEquals('A', indexed.value());
        assertEquals(5, indexed.index());
        assertEquals(5L, indexed.longIndex());
    }

    @Test
    public void testOfLongIndex() {
        IndexedChar indexed = IndexedChar.of('Z', 1000000000L);
        assertEquals('Z', indexed.value());
        assertEquals(1000000000L, indexed.longIndex());
    }

    @Test
    public void testOfNegativeIntIndex() {
        assertThrows(IllegalArgumentException.class, () -> {
            IndexedChar.of('X', -1);
        });
    }

    @Test
    public void testOfNegativeLongIndex() {
        assertThrows(IllegalArgumentException.class, () -> {
            IndexedChar.of('X', -1L);
        });
    }

    @Test
    public void testValue() {
        IndexedChar indexed = IndexedChar.of('M', 0);
        assertEquals('M', indexed.value());
    }

    @Test
    public void testValueDigit() {
        IndexedChar indexed = IndexedChar.of('5', 0);
        assertEquals('5', indexed.value());
    }

    @Test
    public void testValueSpecialChar() {
        IndexedChar indexed = IndexedChar.of('@', 0);
        assertEquals('@', indexed.value());
    }

    @Test
    public void testIndex() {
        IndexedChar indexed = IndexedChar.of('X', 42);
        assertEquals(42, indexed.index());
    }

    @Test
    public void testLongIndex() {
        long largeIndex = 5000000000L;
        IndexedChar indexed = IndexedChar.of('Y', largeIndex);
        assertEquals(largeIndex, indexed.longIndex());
    }

    @Test
    public void testHashCode() {
        IndexedChar indexed1 = IndexedChar.of('A', 5);
        IndexedChar indexed2 = IndexedChar.of('A', 5);
        assertEquals(indexed1.hashCode(), indexed2.hashCode());
    }

    @Test
    public void testHashCodeDifferentValues() {
        IndexedChar indexed1 = IndexedChar.of('A', 5);
        IndexedChar indexed2 = IndexedChar.of('B', 5);
        assertNotEquals(indexed1.hashCode(), indexed2.hashCode());
    }

    @Test
    public void testEquals() {
        IndexedChar indexed1 = IndexedChar.of('A', 5);
        IndexedChar indexed2 = IndexedChar.of('A', 5);
        IndexedChar indexed3 = IndexedChar.of('B', 5);
        IndexedChar indexed4 = IndexedChar.of('A', 10);

        assertTrue(indexed1.equals(indexed2));
        assertFalse(indexed1.equals(indexed3));
        assertFalse(indexed1.equals(indexed4));
        assertFalse(indexed1.equals(null));
        assertFalse(indexed1.equals("string"));
    }

    @Test
    public void testToString() {
        IndexedChar indexed = IndexedChar.of('X', 5);
        assertEquals("[5]=X", indexed.toString());
    }

    @Test
    public void testToStringDigit() {
        IndexedChar indexed = IndexedChar.of('7', 3);
        assertEquals("[3]=7", indexed.toString());
    }

    @Test
    public void testToStringSpace() {
        IndexedChar indexed = IndexedChar.of(' ', 0);
        assertEquals("[0]= ", indexed.toString());
    }

    @Test
    public void testZeroIndex() {
        IndexedChar indexed = IndexedChar.of('A', 0);
        assertEquals(0, indexed.index());
        assertEquals(0L, indexed.longIndex());
    }

    @Test
    public void testUnicodeCharacter() {
        IndexedChar indexed = IndexedChar.of('\u4E16', 5);
        assertEquals('\u4E16', indexed.value());
        assertEquals(5, indexed.index());
    }

    @Test
    public void testMaxCharValue() {
        IndexedChar indexed = IndexedChar.of(Character.MAX_VALUE, 0);
        assertEquals(Character.MAX_VALUE, indexed.value());
    }

    @Test
    public void testMinCharValue() {
        IndexedChar indexed = IndexedChar.of(Character.MIN_VALUE, 0);
        assertEquals(Character.MIN_VALUE, indexed.value());
    }
}
