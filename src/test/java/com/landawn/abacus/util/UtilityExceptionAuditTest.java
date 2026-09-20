package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;
import java.util.AbstractCollection;
import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

class UtilityExceptionAuditTest extends TestBase {
    @Test
    void earlierScalarArgumentsWinBeforeLaterInvalidArguments() {
        assertTrue(assertThrows(IllegalArgumentException.class, () -> NoCachingNoUpdating.DisposableArray.create(null, -1)).getMessage()
                .contains("componentType"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> Numbers.divide((BigInteger) null, BigInteger.ZERO, null)).getMessage().contains("p"));
        assertEquals(BigInteger.valueOf(3), Numbers.divide(BigInteger.valueOf(7), BigInteger.valueOf(2), java.math.RoundingMode.DOWN));
    }

    @Test
    void sheetRejectsEarlierInvalidIndexOrKeyBeforeReadingIncomingValues() {
        final Sheet<String, String, Integer> sheet = new Sheet<>(List.of("row"), List.of("column"));
        final AbstractCollection<Integer> unreadable = new AbstractCollection<>() {
            @Override
            public Iterator<Integer> iterator() {
                throw new AssertionError("Validation must precede input traversal");
            }

            @Override
            public int size() {
                throw new AssertionError("Validation must precede input traversal");
            }
        };
        assertThrows(IndexOutOfBoundsException.class, () -> sheet.addRow(-1, null, unreadable));
        assertThrows(IndexOutOfBoundsException.class, () -> sheet.addColumn(-1, null, unreadable));
        assertThrows(IllegalArgumentException.class, () -> sheet.addRow("row", unreadable));
        assertThrows(IllegalArgumentException.class, () -> sheet.addColumn("column", unreadable));
        assertEquals(1, sheet.rowCount());
        assertEquals(1, sheet.columnCount());
        sheet.addRow("second", List.of(2));
        assertEquals(2, sheet.get("second", "column"));
    }

    @Test
    void frozenSheetRejectsPointValidationFirst() {
        final Sheet<String, String, Integer> sheet = new Sheet<>(List.of("row"), List.of("column"));
        sheet.freeze();
        assertThrows(IllegalStateException.class, () -> sheet.set((Sheet.Point) null, 1));
    }
}
