package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class OperationType2025Test extends TestBase {

    @Test
    public void testIntValue() {
        assertEquals(1, OperationType.QUERY.intValue());
        assertEquals(2, OperationType.ADD.intValue());
        assertEquals(4, OperationType.UPDATE.intValue());
        assertEquals(8, OperationType.DELETE.intValue());
    }

    @Test
    public void testValueOf_withValidIntValues() {
        assertEquals(OperationType.QUERY, OperationType.valueOf(1));
        assertEquals(OperationType.ADD, OperationType.valueOf(2));
        assertEquals(OperationType.UPDATE, OperationType.valueOf(4));
        assertEquals(OperationType.DELETE, OperationType.valueOf(8));
    }

    @Test
    public void testValueOf_withInvalidIntValue() {
        assertThrows(IllegalArgumentException.class, () -> OperationType.valueOf(0));
        assertThrows(IllegalArgumentException.class, () -> OperationType.valueOf(3));
        assertThrows(IllegalArgumentException.class, () -> OperationType.valueOf(16));
    }

    @Test
    public void testValueOf_withStringName() {
        assertEquals(OperationType.QUERY, OperationType.valueOf("QUERY"));
        assertEquals(OperationType.ADD, OperationType.valueOf("ADD"));
        assertEquals(OperationType.UPDATE, OperationType.valueOf("UPDATE"));
        assertEquals(OperationType.DELETE, OperationType.valueOf("DELETE"));
    }

    @Test
    public void testValues() {
        OperationType[] values = OperationType.values();
        assertEquals(4, values.length);
        assertEquals(OperationType.QUERY, values[0]);
        assertEquals(OperationType.ADD, values[1]);
        assertEquals(OperationType.UPDATE, values[2]);
        assertEquals(OperationType.DELETE, values[3]);
    }

    @Test
    public void testEnumName() {
        assertEquals("QUERY", OperationType.QUERY.name());
        assertEquals("ADD", OperationType.ADD.name());
        assertEquals("UPDATE", OperationType.UPDATE.name());
        assertEquals("DELETE", OperationType.DELETE.name());
    }

    @Test
    public void testEnumToString() {
        assertEquals("QUERY", OperationType.QUERY.toString());
        assertEquals("ADD", OperationType.ADD.toString());
        assertEquals("UPDATE", OperationType.UPDATE.toString());
        assertEquals("DELETE", OperationType.DELETE.toString());
    }

    @Test
    public void testBitwiseOperations() {
        // Test that the values are designed for bitwise operations
        int readWrite = OperationType.QUERY.intValue() | OperationType.UPDATE.intValue();
        assertEquals(5, readWrite);

        int allOperations = OperationType.QUERY.intValue() | OperationType.ADD.intValue() | OperationType.UPDATE.intValue() | OperationType.DELETE.intValue();
        assertEquals(15, allOperations);
    }
}
