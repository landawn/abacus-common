package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class IndexedCharTest extends TestBase {

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
    public void testUnicodeCharacter() {
        IndexedChar indexed = IndexedChar.of('\u4E16', 5);
        assertEquals('\u4E16', indexed.value());
        assertEquals(5, indexed.index());
    }

    @Test
    public void testOf_WithIntIndex() {
        char value = 'A';
        int index = 5;

        IndexedChar indexedChar = IndexedChar.of(value, index);

        assertEquals(value, indexedChar.value());
        assertEquals(index, indexedChar.index());
    }

    @Test
    public void testOf_WithLongIndex() {
        char value = 'B';
        long index = 5000000000L;

        IndexedChar indexedChar = IndexedChar.of(value, index);

        assertEquals(value, indexedChar.value());
        assertEquals(index, indexedChar.longIndex());
    }

    @Test
    public void testZeroIndex() {
        IndexedChar indexed = IndexedChar.of('A', 0);
        assertEquals(0, indexed.index());
        assertEquals(0L, indexed.longIndex());
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

    @Test
    public void testOf_WithIntIndex_Zero() {
        char value = '\0';
        int index = 0;

        IndexedChar indexedChar = IndexedChar.of(value, index);

        assertEquals(value, indexedChar.value());
        assertEquals(index, indexedChar.index());
    }

    @Test
    public void testOf_WithIntIndex_MaxValue() {
        char value = Character.MAX_VALUE;
        int index = Integer.MAX_VALUE;

        IndexedChar indexedChar = IndexedChar.of(value, index);

        assertEquals(value, indexedChar.value());
        assertEquals(index, indexedChar.index());
    }

    @Test
    public void testOf_WithIntIndex_MinValue() {
        char value = Character.MIN_VALUE;
        int index = 0;

        IndexedChar indexedChar = IndexedChar.of(value, index);

        assertEquals(value, indexedChar.value());
        assertEquals(index, indexedChar.index());
    }

    @Test
    public void testOf_WithLongIndex_Zero() {
        char value = '0';
        long index = 0L;

        IndexedChar indexedChar = IndexedChar.of(value, index);

        assertEquals(value, indexedChar.value());
        assertEquals(index, indexedChar.index());
    }

    @Test
    public void testOf_WithLongIndex_MaxValue() {
        char value = 'Z';
        long index = Long.MAX_VALUE;

        IndexedChar indexedChar = IndexedChar.of(value, index);

        assertEquals(value, indexedChar.value());
        assertEquals(index, indexedChar.longIndex());
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
    public void testOf_WithIntIndex_NegativeIndex() {
        char value = 'A';
        int index = -1;

        assertThrows(IllegalArgumentException.class, () -> IndexedChar.of(value, index));
    }

    @Test
    public void testOf_WithLongIndex_NegativeIndex() {
        char value = 'A';
        long index = -1L;

        assertThrows(IllegalArgumentException.class, () -> IndexedChar.of(value, index));
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
    public void testValue_SpecialCharacters() {
        IndexedChar unicodeChar = IndexedChar.of('\u03B1', 0);
        assertEquals('\u03B1', unicodeChar.value());

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
    public void testHashCode_DifferentValues() {
        int index = 5;

        IndexedChar indexedChar1 = IndexedChar.of('A', index);
        IndexedChar indexedChar2 = IndexedChar.of('B', index);

        assertNotEquals(indexedChar1.hashCode(), indexedChar2.hashCode());
    }

    @Test
    public void testHashCode_DifferentIndices() {
        char value = 'A';

        IndexedChar indexedChar1 = IndexedChar.of(value, 5);
        IndexedChar indexedChar2 = IndexedChar.of(value, 6);

        assertNotEquals(indexedChar1.hashCode(), indexedChar2.hashCode());
    }

    @Test
    public void testHashCode_SpecificFormula() {
        IndexedChar indexedChar = IndexedChar.of('A', 5);
        assertEquals(5 + 'A' * 31, indexedChar.hashCode());

        IndexedChar indexedChar2 = IndexedChar.of('\0', 10);
        assertEquals(10 + '\0' * 31, indexedChar2.hashCode());
    }

    @Test
    public void testEquals_EqualObjects() {
        char value = 'A';
        int index = 5;

        IndexedChar indexedChar1 = IndexedChar.of(value, index);
        IndexedChar indexedChar2 = IndexedChar.of(value, index);

        assertTrue(indexedChar1.equals(indexedChar2));
        assertTrue(indexedChar2.equals(indexedChar1));
    }

    @Test
    public void testEquals_DifferentValues() {
        int index = 5;

        IndexedChar indexedChar1 = IndexedChar.of('A', index);
        IndexedChar indexedChar2 = IndexedChar.of('B', index);

        assertFalse(indexedChar1.equals(indexedChar2));
        assertFalse(indexedChar2.equals(indexedChar1));
    }

    @Test
    public void testEquals_DifferentIndices() {
        char value = 'A';

        IndexedChar indexedChar1 = IndexedChar.of(value, 5);
        IndexedChar indexedChar2 = IndexedChar.of(value, 6);

        assertFalse(indexedChar1.equals(indexedChar2));
        assertFalse(indexedChar2.equals(indexedChar1));
    }

    @Test
    public void testEquals_DifferentClass() {
        IndexedChar indexedChar = IndexedChar.of('A', 5);

        assertFalse(indexedChar.equals("not an IndexedChar"));
        assertFalse(indexedChar.equals('A'));
        assertFalse(indexedChar.equals(65));
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
    public void testEquals_SameObject() {
        IndexedChar indexedChar = IndexedChar.of('A', 5);

        assertTrue(indexedChar.equals(indexedChar));
    }

    @Test
    public void testEquals_Null() {
        IndexedChar indexedChar = IndexedChar.of('A', 5);

        assertFalse(indexedChar.equals(null));
    }

    @Test
    public void testEquals_EdgeCases() {
        IndexedChar nullChar = IndexedChar.of('\0', 0);
        IndexedChar maxChar = IndexedChar.of(Character.MAX_VALUE, 0);
        IndexedChar minChar = IndexedChar.of(Character.MIN_VALUE, 0);

        assertTrue(nullChar.equals(IndexedChar.of('\0', 0)));
        assertTrue(maxChar.equals(IndexedChar.of(Character.MAX_VALUE, 0)));
        assertTrue(minChar.equals(IndexedChar.of(Character.MIN_VALUE, 0)));

        assertFalse(nullChar.equals(maxChar));
        assertFalse(maxChar.equals(minChar));
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
    public void testToString_LongIndex() {
        char value = 'Z';
        long index = 1000000L;

        IndexedChar indexedChar = IndexedChar.of(value, index);

        assertEquals("[1000000]=Z", indexedChar.toString());
    }

    @Test
    public void testToString_Unicode() {
        IndexedChar greekAlpha = IndexedChar.of('\u03B1', 1);
        IndexedChar chineseChar = IndexedChar.of('中', 2);

        assertEquals("[1]=α", greekAlpha.toString());
        assertEquals("[2]=中", chineseChar.toString());
    }

    @Test
    public void testToString_SpecialCharacters() {
        IndexedChar spaceChar = IndexedChar.of(' ', 10);
        IndexedChar newlineChar = IndexedChar.of('\n', 20);
        IndexedChar tabChar = IndexedChar.of('\t', 30);
        IndexedChar nullChar = IndexedChar.of('\0', 40);

        assertEquals("[10]= ", spaceChar.toString());
        assertEquals("[20]=\n", newlineChar.toString());
        assertEquals("[30]=\t", tabChar.toString());
        assertEquals("[40]=\0", nullChar.toString());
    }

    @Test
    public void testToString_EdgeCases() {
        IndexedChar minChar = IndexedChar.of(Character.MIN_VALUE, 0);
        IndexedChar maxChar = IndexedChar.of(Character.MAX_VALUE, Long.MAX_VALUE);
        IndexedChar digitChar = IndexedChar.of('9', 99);

        assertEquals("[0]=" + Character.MIN_VALUE, minChar.toString());
        assertEquals("[" + Long.MAX_VALUE + "]=" + Character.MAX_VALUE, maxChar.toString());
        assertEquals("[99]=9", digitChar.toString());
    }

}
