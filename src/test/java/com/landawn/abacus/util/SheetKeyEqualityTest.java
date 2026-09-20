package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.landawn.abacus.TestBase;

/** SN 2845: lookup, duplicate detection, iteration and public key sets use one equality rule. */
public class SheetKeyEqualityTest extends TestBase {
    private static Object array(final int kind) {
        return switch (kind) {
            case 0 -> new boolean[] { true };
            case 1 -> new byte[] { 1 };
            case 2 -> new short[] { 1 };
            case 3 -> new char[] { 'x' };
            case 4 -> new int[] { 1 };
            case 5 -> new long[] { 1 };
            case 6 -> new float[] { Float.NaN, -0.0f };
            case 7 -> new double[] { Double.NaN, -0.0 };
            case 8 -> new String[] { "x", null };
            case 9 -> new Object[] { new int[] { 1 }, new String[] { "x", null } };
            case 10 -> new int[0];
            default -> throw new AssertionError(kind);
        };
    }

    private static <K> void assertSetContract(final Set<K> view, final K member, final K absent) {
        final Set<K> standard = new HashSet<>(view);
        assertEquals(standard, view);
        assertEquals(view, standard);
        assertEquals(standard.hashCode(), view.hashCode());
        assertTrue(view.contains(member));
        assertTrue(view.containsAll(List.of(member)));
        assertFalse(view.contains(absent));
        assertFalse(view.containsAll(List.of(absent)));
        assertSame(member, view.iterator().next());
        assertSame(member, view.toArray()[0]);
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 })
    void rawArraysUseIdentityForEveryKeyOperation(final int kind) {
        final Object row = array(kind);
        final Object column = array(kind);
        final Object equalRow = array(kind);
        final Object equalColumn = array(kind);
        final Sheet<Object, Object, Integer> sheet = Sheet.rows(List.of(row), List.of(column), new Integer[][] { { 1 } });

        assertTrue(sheet.containsRow(row));
        assertTrue(sheet.containsColumn(column));
        assertFalse(sheet.containsRow(equalRow));
        assertFalse(sheet.containsColumn(equalColumn));
        assertSetContract(sheet.rowKeySet(), row, equalRow);
        assertSetContract(sheet.columnKeySet(), column, equalColumn);
        assertEquals(Integer.valueOf(1), sheet.get(row, column));
        assertThrows(IllegalArgumentException.class, () -> sheet.get(equalRow, column));
        assertThrows(IllegalArgumentException.class, () -> sheet.get(row, equalColumn));
        assertThrows(IllegalArgumentException.class, () -> sheet.addRow(row, null));
        assertThrows(IllegalArgumentException.class, () -> sheet.addColumn(column, null));
        assertThrows(IllegalArgumentException.class, () -> new Sheet<>(List.of(row, row), List.of(column)));
        assertThrows(IllegalArgumentException.class, () -> new Sheet<>(List.of(row), List.of(column, column)));

        // Equal-content raw arrays are distinct keys, including at construction and in bulk operations.
        final Sheet<Object, Object, Integer> distinct = Sheet.rows(List.of(equalRow), List.of(equalColumn), new Integer[][] { { 2 } });
        assertNotEquals(sheet, distinct);
        assertThrows(IllegalArgumentException.class, () -> sheet.putAll(distinct));
        assertThrows(IllegalArgumentException.class, () -> sheet.copy(List.of(equalRow), List.of(column)));
        final Sheet<Object, Object, Integer> merged = sheet.merge(distinct, (a, b) -> a == null ? b : a);
        assertEquals(2, merged.rowCount());
        assertEquals(2, merged.columnCount());
        assertEquals(Integer.valueOf(1), merged.get(row, column));
        assertEquals(Integer.valueOf(2), merged.get(equalRow, equalColumn));
        assertEquals(2, new Sheet<>(List.of(row, equalRow), List.of(column, equalColumn)).rowCount());

        final var rows = sheet.rowKeySet();
        final var columns = sheet.columnKeySet();
        sheet.addRow(equalRow, List.of(2));
        sheet.addColumn(equalColumn, List.of(3, 4));
        assertEquals(List.of(row, equalRow), new ArrayList<>(rows));
        assertEquals(List.of(column, equalColumn), new ArrayList<>(columns));
        assertEquals(Integer.valueOf(4), sheet.get(equalRow, equalColumn));
        assertSame(equalRow, sheet.rowMajorCells().toList().get(2).rowKey());
        assertEquals(sheet, sheet.copy());
        assertEquals(sheet.hashCode(), sheet.copy().hashCode());
        assertEquals(Integer.valueOf(4), sheet.transposed().get(equalColumn, equalRow));
        sheet.removeRow(equalRow);
        sheet.removeColumn(equalColumn);
        assertFalse(rows.contains(equalRow));
        assertFalse(columns.contains(equalColumn));
        assertEquals(Integer.valueOf(1), sheet.get(row, column));
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 })
    void explicitWrappersUseContentEqualityInLookupAndViews(final int kind) {
        final Wrapper<Object> row = Wrapper.of(array(kind));
        final Wrapper<Object> column = Wrapper.of(array(kind));
        final Wrapper<Object> equalRow = Wrapper.of(array(kind));
        final Wrapper<Object> equalColumn = Wrapper.of(array(kind));
        final Sheet<Wrapper<Object>, Wrapper<Object>, Integer> sheet = Sheet.rows(List.of(row), List.of(column), new Integer[][] { { 1 } });
        assertTrue(sheet.containsRow(equalRow));
        assertTrue(sheet.containsColumn(equalColumn));
        assertTrue(sheet.rowKeySet().contains(equalRow));
        assertTrue(sheet.columnKeySet().contains(equalColumn));
        assertEquals(Set.of(equalRow), sheet.rowKeySet());
        assertEquals(sheet.rowKeySet(), Set.of(equalRow));
        assertEquals(Set.of(equalRow).hashCode(), sheet.rowKeySet().hashCode());
        assertEquals(Set.of(equalColumn), sheet.columnKeySet());
        assertEquals(sheet.columnKeySet(), Set.of(equalColumn));
        assertEquals(Set.of(equalColumn).hashCode(), sheet.columnKeySet().hashCode());
        assertEquals(Integer.valueOf(1), sheet.get(equalRow, equalColumn));
        assertThrows(IllegalArgumentException.class, () -> sheet.addRow(equalRow, null));
        assertThrows(IllegalArgumentException.class, () -> sheet.addColumn(equalColumn, null));
        assertThrows(IllegalArgumentException.class, () -> new Sheet<>(List.of(row, equalRow), List.of(column)));
        assertSame(row, sheet.rowKeySet().iterator().next());
        assertSame(column, sheet.columnKeySet().iterator().next());
        assertEquals(sheet, sheet.copy(List.of(equalRow), List.of(equalColumn)));
        assertEquals(sheet, Sheet.rows(List.of(equalRow), List.of(equalColumn), new Integer[][] { { 1 } }));
        final Sheet<Wrapper<Object>, Wrapper<Object>, Integer> source = Sheet.rows(List.of(equalRow), List.of(equalColumn), new Integer[][] { { 2 } });
        sheet.putAll(source);
        assertEquals(Integer.valueOf(2), sheet.get(equalRow, equalColumn));
        final Sheet<Wrapper<Object>, Wrapper<Object>, Integer> merged = sheet.merge(source, Integer::sum);
        assertEquals(1, merged.rowCount());
        assertEquals(1, merged.columnCount());
        assertEquals(Integer.valueOf(4), merged.get(equalRow, equalColumn));
    }

    @Test
    void mutatingRawArrayContentsDoesNotInvalidateItsIdentityKey() {
        final int[] row = { 1 };
        final int[] column = { 2 };
        final Sheet<int[], int[], Integer> sheet = Sheet.rows(List.of(row), List.of(column), new Integer[][] { { 3 } });
        final int hash = sheet.hashCode();
        row[0] = 4;
        column[0] = 5;
        assertEquals(Integer.valueOf(3), sheet.get(row, column));
        assertTrue(sheet.rowKeySet().contains(row));
        assertTrue(sheet.columnKeySet().contains(column));
        assertEquals(hash, sheet.hashCode());
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 })
    void renamingRawArraysUsesIdentityAndPreservesOrderAndValues(final int kind) {
        final Object first = array(kind);
        final Object second = array(kind);
        final Object replacement = array(kind);
        final Sheet<Object, Object, Integer> sheet = Sheet.rows(List.of(first, second), List.of(first, second), new Integer[][] { { 1, 2 }, { 3, 4 } });
        final var rows = sheet.rowKeySet();
        final var columns = sheet.columnKeySet();

        // The existing object is a duplicate; a new equal-content array is a distinct replacement key.
        final String rowFailure = assertThrows(IllegalArgumentException.class, () -> sheet.renameRow(first, second)).getMessage();
        final String columnFailure = assertThrows(IllegalArgumentException.class, () -> sheet.renameColumn(first, second)).getMessage();
        // Distinct arrays can render identically, so duplicate-rename diagnostics must identify the existing object.
        final String duplicateDescription = N.toString(second) + "@" + Integer.toHexString(System.identityHashCode(second));
        assertTrue(rowFailure.contains(duplicateDescription) && rowFailure.contains("row key set"), rowFailure);
        assertTrue(columnFailure.contains(duplicateDescription) && columnFailure.contains("column key set"), columnFailure);
        for (final String message : List.of(rowFailure, columnFailure)) {
            final String note = "array keys match by identity";
            assertTrue(message.contains(note), message);
            assertEquals(message.indexOf(note), message.lastIndexOf(note), message);
        }
        sheet.renameRow(first, replacement);
        sheet.renameColumn(first, replacement);
        assertEquals(List.of(replacement, second), new ArrayList<>(rows));
        assertEquals(List.of(replacement, second), new ArrayList<>(columns));
        assertFalse(sheet.containsRow(first));
        assertFalse(sheet.containsColumn(first));
        assertThrows(IllegalArgumentException.class, () -> sheet.get(first, replacement));
        assertThrows(IllegalArgumentException.class, () -> sheet.get(replacement, first));
        assertEquals(Integer.valueOf(1), sheet.get(replacement, replacement));
        assertEquals(Integer.valueOf(2), sheet.get(replacement, second));
        assertEquals(Integer.valueOf(3), sheet.get(second, replacement));
        assertEquals(Integer.valueOf(4), sheet.get(second, second));
        assertSame(replacement, sheet.rowMajorCells().iterator().next().rowKey());
        assertSame(replacement, sheet.rowMajorCells().iterator().next().columnKey());
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 })
    void renamingToAnEqualWrappedArrayRejectsTheDuplicateWithoutMutation(final int kind) {
        final Object original = "original";
        final Object existing = Wrapper.of(array(kind));
        final Object duplicate = Wrapper.of(array(kind));
        final Sheet<Object, Object, Integer> sheet = Sheet.rows(List.of(original, existing), List.of(original, existing),
                new Integer[][] { { 1, 2 }, { 3, 4 } });

        assertThrows(IllegalArgumentException.class, () -> sheet.renameRow(original, duplicate));
        assertThrows(IllegalArgumentException.class, () -> sheet.renameColumn(original, duplicate));
        assertEquals(List.of(original, existing), new ArrayList<>(sheet.rowKeySet()));
        assertEquals(List.of(original, existing), new ArrayList<>(sheet.columnKeySet()));
        assertEquals(Integer.valueOf(1), sheet.get(original, original));
        assertEquals(Integer.valueOf(2), sheet.get(original, duplicate));
        assertEquals(Integer.valueOf(3), sheet.get(duplicate, original));
        assertEquals(Integer.valueOf(4), sheet.get(duplicate, duplicate));
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void cloningPreservesArrayIdentityAcrossBothIndexesKeySetsAndCells(final boolean frozen) {
        final int[] first = { 1 };
        final int[] second = { 1 };
        final Sheet<int[], int[], Object> source = Sheet.rows(List.of(first, second), List.of(first, second),
                new Object[][] { { first, second }, { null, null } });
        source.set(second, first, source); // Initialize keyed access and include a cycle in the copied graph.
        if (frozen) {
            source.freeze();
        }

        final Sheet<int[], int[], Object> copy = frozen ? source.clone() : source.clone(false);
        final List<int[]> rows = new ArrayList<>(copy.rowKeySet());
        final List<int[]> columns = new ArrayList<>(copy.columnKeySet());
        assertEquals(frozen, copy.isFrozen());
        assertNotEquals(source, copy);
        assertNotSame(first, rows.get(0));
        assertNotSame(second, rows.get(1));
        assertNotSame(rows.get(0), rows.get(1));
        assertSame(rows.get(0), columns.get(0));
        assertSame(rows.get(1), columns.get(1));
        assertSame(rows.get(0), copy.get(rows.get(0), columns.get(0)));
        assertSame(rows.get(1), copy.get(rows.get(0), columns.get(1)));
        assertSame(copy, copy.get(rows.get(1), columns.get(0)));
        assertThrows(IllegalArgumentException.class, () -> copy.get(first, columns.get(0)));
        assertThrows(IllegalArgumentException.class, () -> copy.get(rows.get(0), first));
        assertSame(rows.get(0), copy.rowMajorCells().iterator().next().rowKey());
        assertSame(columns.get(0), copy.rowMajorCells().iterator().next().columnKey());
    }

    @Test
    void nonArrayKeysRetainValueEquality() {
        final String row = new String("row");
        final String column = new String("column");
        final Sheet<String, String, Integer> sheet = Sheet.rows(List.of(row), List.of(column), new Integer[][] { { 7 } });
        assertEquals(Integer.valueOf(7), sheet.get(new String("row"), new String("column")));
        assertTrue(sheet.containsRow(new String("row")));
        assertTrue(sheet.rowKeySet().contains(new String("row")));
        assertTrue(sheet.containsColumn(new String("column")));
        assertTrue(sheet.columnKeySet().contains(new String("column")));
        assertThrows(IllegalArgumentException.class, () -> sheet.addRow(new String("row"), null));
        assertThrows(IllegalArgumentException.class, () -> sheet.addColumn(new String("column"), null));
    }
}
