package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class IndexedFloat2025Test extends TestBase {

    @Test
    public void testOfIntIndex() {
        IndexedFloat indexed = IndexedFloat.of(3.14f, 5);
        assertEquals(3.14f, indexed.value(), 0.001f);
        assertEquals(5, indexed.index());
    }

    @Test
    public void testOfLongIndex() {
        IndexedFloat indexed = IndexedFloat.of(2.71f, 1000000000L);
        assertEquals(2.71f, indexed.value(), 0.001f);
        assertEquals(1000000000L, indexed.longIndex());
    }

    @Test
    public void testOfNegativeIntIndex() {
        assertThrows(IllegalArgumentException.class, () -> {
            IndexedFloat.of(1.0f, -1);
        });
    }

    @Test
    public void testValue() {
        IndexedFloat indexed = IndexedFloat.of(123.456f, 0);
        assertEquals(123.456f, indexed.value(), 0.001f);
    }

    @Test
    public void testHashCode() {
        IndexedFloat indexed1 = IndexedFloat.of(3.14f, 5);
        IndexedFloat indexed2 = IndexedFloat.of(3.14f, 5);
        assertEquals(indexed1.hashCode(), indexed2.hashCode());
    }

    @Test
    public void testEquals() {
        IndexedFloat indexed1 = IndexedFloat.of(3.14f, 5);
        IndexedFloat indexed2 = IndexedFloat.of(3.14f, 5);
        IndexedFloat indexed3 = IndexedFloat.of(2.71f, 5);

        assertTrue(indexed1.equals(indexed2));
        assertFalse(indexed1.equals(indexed3));
    }

    @Test
    public void testToString() {
        IndexedFloat indexed = IndexedFloat.of(3.14f, 5);
        assertTrue(indexed.toString().contains("[5]="));
        assertTrue(indexed.toString().contains("3.14"));
    }

    @Test
    public void testZeroValue() {
        IndexedFloat indexed = IndexedFloat.of(0.0f, 0);
        assertEquals(0.0f, indexed.value(), 0.0f);
    }

    @Test
    public void testNaN() {
        IndexedFloat indexed = IndexedFloat.of(Float.NaN, 0);
        assertTrue(Float.isNaN(indexed.value()));
    }
}
