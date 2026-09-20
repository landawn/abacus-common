package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.NoCachingNoUpdating.DisposableObjArray;

class RowDatasetValidationOrderTest extends TestBase {
    private static RowDataset dataset() {
        return new RowDataset(new ArrayList<>(List.of("id")), new ArrayList<>(List.of(new ArrayList<Object>(List.of(1, 2)))));
    }

    @Test
    void frozenStatePrecedesInvalidArgumentsThroughDelegates() {
        RowDataset data = dataset();
        data.freeze();
        assertThrows(IllegalStateException.class, () -> data.addColumns(-1, null, null));
        assertThrows(IllegalStateException.class, () -> data.renameColumns((Function<String, String>) null));
        assertThrows(IllegalStateException.class, () -> data.removeDuplicateRowsBy("missing", (Function<Object, Object>) null));
        assertEquals(List.of("id"), data.columnNames());
        assertEquals(2, data.size());
    }

    @Test
    void rowRangePrecedesColumnAndCallbackChecks() {
        RowDataset data = dataset();
        assertThrows(IndexOutOfBoundsException.class, () -> data.filter(-1, 0, "missing", (Predicate<Object>) null, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> data.toMap(-1, 0, "missing", "missing", (IntFunction<Map<Object, Object>>) null));
        assertThrows(IndexOutOfBoundsException.class, () -> data.toList(-1, 0, (IntFunction<Object[]>) null));
    }

    @Test
    void conversionChecksRowTypeBeforeInvokingColumnCallbacks() {
        RowDataset data = dataset();
        AtomicInteger calls = new AtomicInteger();
        assertThrows(IllegalArgumentException.class, () -> data.toList(0, 1, name -> {
            calls.incrementAndGet();
            return true;
        }, Function.identity(), (Class<Object[]>) null));
        assertEquals(0, calls.get());
    }

    @Test
    void combineRejectsTheSelectionBeforeTheCallbackWithoutMutation() {
        RowDataset data = dataset();
        // Both inputs are invalid: the selection must win, including the empty-selection branch.
        for (Collection<String> columns : Arrays.<Collection<String>> asList(null, List.of())) {
            IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                    () -> data.combineColumns(columns, "combined", (Function<DisposableObjArray, Object>) null));
            assertTrue(error.getMessage().contains("Column names to be combined"), error.getMessage());
        }
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> data.combineColumns(List.of("id"), "combined", (Function<DisposableObjArray, Object>) null));
        assertTrue(error.getMessage().contains("combineFunc"), error.getMessage());
        assertEquals(List.of("id"), data.columnNames());
        assertEquals(List.of(1, 2), data.getColumn("id"));

        data.freeze();
        assertThrows(IllegalStateException.class, () -> data.combineColumns((Collection<String>) null, null, (Function<DisposableObjArray, Object>) null));
    }

    @Test
    void combineValidatesNamesBeforeCallingMapperAndPreservesSuccessfulOutput() {
        RowDataset data = dataset();
        AtomicInteger calls = new AtomicInteger();
        Function<DisposableObjArray, Object> mapper = row -> {
            calls.incrementAndGet();
            return (Integer) row.get(0) * 10;
        };
        assertThrows(IllegalArgumentException.class, () -> data.combineColumns(List.of("missing"), "combined", mapper));
        assertEquals(0, calls.get());
        data.combineColumns(List.of("id"), "combined", mapper);
        assertEquals(2, calls.get());
        assertEquals(List.of("combined"), data.columnNames());
        assertEquals(List.of(10, 20), data.getColumn("combined"));
    }

    @Test
    void updateRowsValidatesEveryIndexBeforeAnyCellChanges() {
        RowDataset data = dataset();
        AtomicInteger calls = new AtomicInteger();
        assertThrows(IndexOutOfBoundsException.class, () -> data.updateRows(new int[] { 0, 2 }, (index, column, value) -> {
            calls.incrementAndGet();
            return 99;
        }));
        assertThrows(IndexOutOfBoundsException.class, () -> data.updateRows(new int[] { -1 }, null));
        assertThrows(IllegalArgumentException.class, () -> data.updateRows(new int[0], null));
        assertEquals(0, calls.get());
        assertEquals(List.of(1, 2), data.getColumn("id"));

        // Repeated indices intentionally apply the function repeatedly to the updated value.
        data.updateRows(new int[] { 0, 0 }, (index, column, value) -> (Integer) value + 1);
        assertEquals(List.of(3, 2), data.getColumn("id"));
        data.freeze();
        assertThrows(IllegalStateException.class, () -> data.updateRows(null, null));
    }

    @Test
    void mergeValidatesSourceThenRangeThenSelectionWithoutMutation() {
        RowDataset data = dataset();
        RowDataset source = dataset();
        assertThrows(IllegalArgumentException.class, () -> data.merge(null, -1, 3, null));
        assertThrows(IndexOutOfBoundsException.class, () -> data.merge(source, -1, 3, null));
        assertThrows(IndexOutOfBoundsException.class, () -> data.merge(source, 1, 0, List.of("missing")));
        assertThrows(IllegalArgumentException.class, () -> data.merge(source, 0, 0, List.of("missing")));
        assertEquals(List.of(1, 2), data.getColumn("id"));
        data.merge(source, 1, 2, List.of("id"));
        assertEquals(List.of(1, 2, 2), data.getColumn("id"));
        data.freeze();
        assertThrows(IllegalStateException.class, () -> data.merge(null, -1, 3, null));
    }

    @Test
    void suppliedRowsValidateBeforeInvocationAndReuseTheFirstResult() {
        RowDataset data = dataset();
        AtomicInteger calls = new AtomicInteger();
        List<Object[]> supplied = new ArrayList<>();
        IntFunction<Object[]> supplier = length -> {
            calls.incrementAndGet();
            Object[] row = new Object[length];
            supplied.add(row);
            return row;
        };
        assertThrows(IndexOutOfBoundsException.class, () -> data.getRow(-1, List.of("missing"), supplier));
        assertThrows(IllegalArgumentException.class, () -> data.getRow(0, List.of("missing"), supplier));
        assertThrows(IllegalArgumentException.class, () -> data.toList(0, 0, List.of("missing"), supplier));
        assertEquals(0, calls.get());

        Object[] row = data.getRow(1, List.of("id"), supplier);
        assertSame(supplied.get(0), row);
        assertArrayEquals(new Object[] { 2 }, row);
        List<Object[]> rows = data.toList(0, 2, List.of("id"), supplier);
        assertEquals(3, calls.get());
        assertSame(supplied.get(1), rows.get(0));
        assertSame(supplied.get(2), rows.get(1));
        assertArrayEquals(new Object[] { 1 }, rows.get(0));
        assertArrayEquals(new Object[] { 2 }, rows.get(1));
    }

    @Test
    void suppliedRowsStillRejectNullAndUndersizedResults() {
        RowDataset data = dataset();
        assertThrows(IllegalArgumentException.class, () -> data.getRow(0, List.of("id"), length -> null));
        assertThrows(IllegalArgumentException.class, () -> data.toList(0, 2, List.of("id"), length -> null));
        assertThrows(IllegalArgumentException.class, () -> data.getRow(0, List.of("id"), length -> new Object[0]));
        assertThrows(IllegalArgumentException.class, () -> data.toList(0, 2, List.of("id"), length -> new Object[0]));
    }

    @Test
    void forEachRetainsReverseTraversalAndValidatesBeforeEmptyReturn() {
        RowDataset data = dataset();
        List<Integer> visited = new ArrayList<>();
        data.forEach(1, -1, List.of("id"), row -> visited.add((Integer) row.get(0)));
        assertEquals(List.of(2, 1), visited);
        visited.clear();
        data.forEach(0, 2, List.of("id"), row -> visited.add((Integer) row.get(0)));
        assertEquals(List.of(1, 2), visited);
        assertThrows(IndexOutOfBoundsException.class, () -> data.forEach(2, -1, List.of("missing"), null));
        assertThrows(IllegalArgumentException.class, () -> data.forEach(0, 0, List.of("id"), null));

        RowDataset empty = new RowDataset(List.of("id"), List.of(new ArrayList<>()));
        assertThrows(IllegalArgumentException.class, () -> empty.forEach(0, 0, List.of("id"), null));
        AtomicInteger calls = new AtomicInteger();
        empty.forEach(0, 0, List.of("id"), row -> calls.incrementAndGet());
        assertEquals(0, calls.get());
    }

    @Test
    void xmlValidationLeavesOutputUntouchedAndRetainsEmptySelectionExport() {
        RowDataset data = dataset();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        assertThrows(IndexOutOfBoundsException.class, () -> data.toXml(-1, 0, List.of("missing"), "", output));
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> data.toXml(0, 1, List.of("missing"), "", output));
        assertTrue(error.getMessage().contains("missing"), error.getMessage());
        error = assertThrows(IllegalArgumentException.class, () -> data.toXml(0, 1, List.of("id"), "", output));
        assertTrue(error.getMessage().contains("rowElementName"), error.getMessage());
        assertEquals(0, output.size());
        data.toXml(0, 1, List.of(), "row", output);
        assertEquals(data.toXml(0, 1, List.of(), "row"), output.toString(java.nio.charset.StandardCharsets.UTF_8));
        assertTrue(data.toXml(0, 1, List.of("id"), "row").contains("<id>1</id>"));
    }
}
