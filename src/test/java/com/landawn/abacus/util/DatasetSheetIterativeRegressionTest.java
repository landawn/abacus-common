package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.NoCachingNoUpdating.DisposableObjArray;
import com.landawn.abacus.util.stream.Stream;

/** Regressions for cycle 2 of the Dataset/RowDataset/Sheet review ledger. */
public class DatasetSheetIterativeRegressionTest extends TestBase {
    private static Object arrayKey(final int kind) {
        return switch (kind) {
            case 0 -> new int[] { 1 };
            case 1 -> new double[] { Double.NaN, -0.0, Double.POSITIVE_INFINITY };
            case 2 -> new Object[] { new String[] { "\u6D77", null }, new int[] { 1, 2 } };
            case 3 -> new int[0];
            case 4 -> new char[] { '\uD83D', '\uDE00', '\uD800' };
            default -> throw new AssertionError(kind);
        };
    }

    private static void assertStandardSet(final java.util.Set<Object> view, final Object originalKey, final Object equalContents) {
        final java.util.Set<Object> standard = new java.util.LinkedHashSet<>(view);
        assertEquals(standard, view);
        assertEquals(view, standard);
        assertEquals(standard.hashCode(), view.hashCode());
        final Map<java.util.Set<Object>, String> map = new java.util.HashMap<>();
        map.put(standard, "value");
        assertEquals("value", map.get(view));
        map.clear();
        map.put(view, "value");
        assertEquals("value", map.get(standard));
        assertEquals(1, new java.util.HashSet<>(List.of(standard, view)).size());
        assertTrue(view.contains(originalKey));
        assertFalse(view.contains(equalContents));
        assertFalse(view.contains(null));
        standard.remove(originalKey);
        standard.add(equalContents);
        assertNotEquals(standard, view);
        assertNotEquals(view, standard);
        assertFalse(view.containsAll(standard));
        assertSame(originalKey, view.iterator().next());
        assertSame(originalKey, view.toArray()[0]);
        assertThrows(UnsupportedOperationException.class, () -> view.add(equalContents));
        assertThrows(UnsupportedOperationException.class, () -> view.remove(originalKey));
        assertThrows(UnsupportedOperationException.class, view::clear);
        assertThrows(UnsupportedOperationException.class, () -> view.removeIf(key -> true));
        final var iterator = view.iterator();
        iterator.next();
        assertThrows(UnsupportedOperationException.class, iterator::remove);
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 1, 2, 3, 4 })
    void sheetKeyViewsObeyStandardSetEqualityAndHashing(final int kind) {
        final Object row = arrayKey(kind);
        final Object column = arrayKey(kind);
        final Sheet<Object, Object, Integer> sheet = new Sheet<>(List.of(row, "\u6D77"), List.of(column, "\uD83D\uDE00"));
        assertStandardSet(sheet.rowKeySet(), row, arrayKey(kind));
        assertStandardSet(sheet.columnKeySet(), column, arrayKey(kind));
        assertTrue(sheet.rowKeySet().contains(new String("\u6D77")));
        assertTrue(sheet.columnKeySet().contains(new String("\uD83D\uDE00")));
        assertTrue(sheet.containsRow(arrayKey(kind)));
        assertTrue(sheet.containsColumn(arrayKey(kind)));
        assertNull(sheet.get(arrayKey(kind), arrayKey(kind)));
        assertThrows(IllegalArgumentException.class, () -> sheet.addRow(arrayKey(kind), null));
        assertThrows(IllegalArgumentException.class, () -> sheet.addColumn(arrayKey(kind), null));
    }

    @Test
    void sheetKeyViewsStayLiveThroughAxisChanges() {
        final Sheet<int[], int[], Integer> sheet = new Sheet<>();
        final var rows = sheet.rowKeySet();
        final var columns = sheet.columnKeySet();
        assertEquals(java.util.Set.of(), rows);
        assertEquals(0, rows.hashCode());
        assertEquals(java.util.Set.of(), columns);
        final int[] a = { 1 };
        final int[] b = { 2 };
        final int[] c = { 3 };
        sheet.addRow(a, null);
        sheet.addColumn(a, List.of(1));
        sheet.addRow(b, List.of(2));
        sheet.addColumn(b, List.of(3, 4));
        assertEquals(List.of(a, b), new ArrayList<>(rows));
        assertEquals(List.of(a, b), new ArrayList<>(columns));
        sheet.moveRow(new int[] { 2 }, 0);
        sheet.moveColumn(new int[] { 2 }, 0);
        assertEquals(List.of(b, a), new ArrayList<>(rows));
        assertEquals(List.of(b, a), new ArrayList<>(columns));
        sheet.renameRow(new int[] { 1 }, c);
        sheet.renameColumn(new int[] { 1 }, c);
        assertEquals(List.of(b, c), new ArrayList<>(rows));
        assertEquals(List.of(b, c), new ArrayList<>(columns));
        assertFalse(rows.contains(a));
        assertFalse(columns.contains(a));
        assertTrue(rows.contains(c));
        assertTrue(columns.contains(c));
        sheet.removeRow(new int[] { 2 });
        sheet.removeColumn(new int[] { 2 });
        assertEquals(java.util.Set.of(c), rows);
        assertEquals(java.util.Set.of(c).hashCode(), columns.hashCode());
        sheet.removeRow(new int[] { 3 });
        sheet.removeColumn(new int[] { 3 });
        assertEquals(java.util.Set.of(), rows);
        assertEquals(java.util.Set.of(), columns);
        assertThrows(IllegalArgumentException.class, () -> sheet.addRow(null, null));
        assertThrows(IllegalArgumentException.class, () -> sheet.addColumn(null, null));
    }

    @Test
    void sheetBulkOperationsAndCloneRetainDeepKeyLookup() {
        final Sheet<Object, Object, Integer> sheet = Sheet.rows(List.of(arrayKey(0)), List.of(arrayKey(2)), new Integer[][] { { 1 } });
        final Sheet<Object, Object, Integer> source = Sheet.rows(List.of(arrayKey(0)), List.of(arrayKey(2)), new Integer[][] { { 3 } });
        sheet.putAll(source);
        assertEquals(Integer.valueOf(3), sheet.get(arrayKey(0), arrayKey(2)));
        sheet.putAll(source, Integer::sum);
        assertEquals(Integer.valueOf(6), sheet.get(arrayKey(0), arrayKey(2)));
        assertEquals(sheet, sheet.copy(List.of(arrayKey(0)), List.of(arrayKey(2))));
        final Sheet<Object, Object, Integer> merged = sheet.merge(source, Integer::sum);
        assertEquals(1, merged.rowCount());
        assertEquals(1, merged.columnCount());
        assertEquals(Integer.valueOf(9), merged.get(arrayKey(0), arrayKey(2)));
        final Sheet<Object, Object, Integer> clone = sheet.clone(false);
        assertEquals(sheet, clone);
        assertEquals(sheet.hashCode(), clone.hashCode());
        assertNotEquals(sheet.rowKeySet(), clone.rowKeySet());
        assertNotEquals(sheet.columnKeySet(), clone.columnKeySet());
        assertStandardSet(clone.rowKeySet(), clone.rowKeySet().iterator().next(), arrayKey(0));
        assertStandardSet(clone.columnKeySet(), clone.columnKeySet().iterator().next(), arrayKey(2));
        assertEquals(Integer.valueOf(6), clone.get(arrayKey(0), arrayKey(2)));
        assertThrows(IllegalArgumentException.class, () -> sheet.putAll(new Sheet<>(List.of("absent"), List.of(arrayKey(2)))));
        assertThrows(IllegalArgumentException.class, () -> sheet.putAll(new Sheet<>(List.of(arrayKey(0)), List.of("absent")), Integer::sum));
        assertThrows(IllegalArgumentException.class, () -> sheet.putAll(null));
        assertThrows(IllegalArgumentException.class, () -> sheet.putAll(source, null));
        final Sheet<Double, String, Integer> doubles = new Sheet<>(List.of(-0.0, +0.0, Double.NaN), List.of("x"));
        assertEquals(java.util.Set.of(-0.0, +0.0, Double.NaN), doubles.rowKeySet());
        assertEquals(java.util.Set.of(-0.0, +0.0, Double.NaN).hashCode(), doubles.rowKeySet().hashCode());
    }

    private static int integerOrZero(final Object value) {
        return value == null ? 0 : ((Number) value).intValue();
    }

    private static Stream<Dataset> aggregate(final Dataset data, final int kind, final List<String> keys, final List<String> values,
            final java.util.concurrent.atomic.AtomicInteger callbacks) {
        final java.util.function.Function<DisposableObjArray, Object[]> key = row -> {
            callbacks.incrementAndGet();
            return row.copy();
        };
        final java.util.function.Function<DisposableObjArray, Integer> mapper = row -> {
            callbacks.incrementAndGet();
            return integerOrZero(row.get(0));
        };
        final var scalar = java.util.stream.Collectors.summingInt(DatasetSheetIterativeRegressionTest::integerOrZero);
        final var arrays = java.util.stream.Collectors.<Object[]> summingInt(row -> integerOrZero(row[0]));
        final var integers = java.util.stream.Collectors.summingInt(Integer::intValue);
        return switch (kind) {
            case 0 -> data.rollup(keys);
            case 1 -> data.rollup(keys, "v", "total", scalar);
            case 2 -> data.rollup(keys, values, "total", List.class);
            case 3 -> data.rollup(keys, values, "total", arrays);
            case 4 -> data.rollup(keys, values, "total", mapper, integers);
            case 5 -> data.rollup(keys, key);
            case 6 -> data.rollup(keys, key, "v", "total", scalar);
            case 7 -> data.rollup(keys, key, values, "total", List.class);
            case 8 -> data.rollup(keys, key, values, "total", arrays);
            case 9 -> data.rollup(keys, key, values, "total", mapper, integers);
            case 10 -> data.cube(keys);
            case 11 -> data.cube(keys, "v", "total", scalar);
            case 12 -> data.cube(keys, values, "total", List.class);
            case 13 -> data.cube(keys, values, "total", arrays);
            case 14 -> data.cube(keys, values, "total", mapper, integers);
            case 15 -> data.cube(keys, key);
            case 16 -> data.cube(keys, key, "v", "total", scalar);
            case 17 -> data.cube(keys, key, values, "total", List.class);
            case 18 -> data.cube(keys, key, values, "total", arrays);
            case 19 -> data.cube(keys, key, values, "total", mapper, integers);
            default -> throw new AssertionError(kind);
        };
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19 })
    void groupingSelectionsAreSnapshotsButAggregationStaysLazy(final int kind) {
        final List<String> originalKeys = List.of("\u6D77", "\uD83D\uDE00");
        final List<String> originalValues = List.of("v", "extra");
        for (final boolean changeBeforeFirst : new boolean[] { false, true }) {
            final Dataset data = Dataset.rows(List.of("\u6D77", "\uD83D\uDE00", "v", "extra"),
                    new Object[][] { { "north", null, 1, 10 }, { "north", "city", null, 20 }, { null, "city", 3, 30 } });
            final List<String> keys = new ArrayList<>(originalKeys);
            final List<String> values = new ArrayList<>(originalValues);
            final var callbacks = new java.util.concurrent.atomic.AtomicInteger();
            final var stream = aggregate(data, kind, keys, values, callbacks);
            assertEquals(0, callbacks.get());
            data.set(0, 2, 7);
            final List<Dataset> expected = aggregate(data, kind, originalKeys, originalValues, new java.util.concurrent.atomic.AtomicInteger()).toList();
            assertEquals(kind < 10 ? 3 : 4, expected.size());
            final var iterator = stream.iterator();
            final List<Dataset> actual = new ArrayList<>();
            if (!changeBeforeFirst) {
                actual.add(iterator.next());
            }
            keys.clear();
            values.set(0, "extra");
            values.remove(1);
            while (iterator.hasNext()) {
                actual.add(iterator.next());
                // Later levels must not begin using a newly emptied aggregate selection either.
                values.clear();
            }
            assertEquals(expected, actual);
            final Dataset total = actual.get(actual.size() - 1);
            final int mode = kind % 10;
            assertEquals(List.of(mode == 0 || mode == 5 ? "count" : "total"), total.columnNames());
            if (mode == 2 || mode == 7) {
                assertEquals(List.of(List.of(7, 10), Arrays.asList(null, 20), List.of(3, 30)), total.get(0, 0));
            } else {
                assertEquals(mode == 0 || mode == 5 ? 3 : 10, (int) total.get(0, 0));
            }
            if (kind % 10 >= 4) {
                assertTrue(callbacks.get() > 0);
            }
        }
        final Dataset empty = Dataset.rows(List.of("\u6D77", "\uD83D\uDE00", "v", "extra"), new Object[0][]);
        final List<Dataset> emptyLevels = aggregate(empty, kind, originalKeys, originalValues, new java.util.concurrent.atomic.AtomicInteger()).toList();
        assertEquals(kind < 10 ? 3 : 4, emptyLevels.size());
        emptyLevels.forEach(level -> assertEquals(0, level.size()));
        assertThrows(IllegalArgumentException.class, () -> aggregate(empty, kind, null, originalValues, new java.util.concurrent.atomic.AtomicInteger()));
        assertThrows(IllegalArgumentException.class, () -> aggregate(empty, kind, List.of(), originalValues, new java.util.concurrent.atomic.AtomicInteger()));
        assertThrows(IllegalArgumentException.class,
                () -> aggregate(empty, kind, Arrays.asList((String) null), originalValues, new java.util.concurrent.atomic.AtomicInteger()));
        assertThrows(IllegalArgumentException.class,
                () -> aggregate(empty, kind, List.of("absent"), originalValues, new java.util.concurrent.atomic.AtomicInteger()));
        if (List.of(2, 3, 4, 7, 8, 9).contains(kind % 10)) {
            assertThrows(IllegalArgumentException.class, () -> aggregate(empty, kind, originalKeys, null, new java.util.concurrent.atomic.AtomicInteger()));
            assertThrows(IllegalArgumentException.class,
                    () -> aggregate(empty, kind, originalKeys, List.of(), new java.util.concurrent.atomic.AtomicInteger()));
            assertThrows(IllegalArgumentException.class,
                    () -> aggregate(empty, kind, originalKeys, List.of("absent"), new java.util.concurrent.atomic.AtomicInteger()));
        }
    }

    private static Dataset filter(final Dataset data, final int kind, final int from, final int to, final int max,
            final java.util.function.Predicate<Object> predicate) {
        return switch (kind) {
            case 0 -> data.filter(from, to, "a", value -> predicate.test(value), max);
            case 1 -> data.filter(from, to, Tuple.of("a", "b"), (a, b) -> predicate.test(a), max);
            case 2 -> data.filter(from, to, Tuple.of("a", "b", "c"), (a, b, c) -> predicate.test(a), max);
            case 3 -> data.filter(from, to, List.of("a", "c"), row -> predicate.test(row.get(0)), max);
            default -> throw new AssertionError(kind);
        };
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 1, 2, 3 })
    void filterDoesNotReserveStorageForUnmatchedInput(final int kind) {
        final IllegalStateException firstRead = new IllegalStateException("first read");
        final List<Object> virtual = new java.util.AbstractList<>() {
            @Override
            public int size() {
                return Integer.MAX_VALUE;
            }

            @Override
            public Object get(final int index) {
                throw firstRead;
            }
        };
        final Dataset data = new RowDataset(List.of("a", "b", "c"), List.of(virtual, virtual, virtual), null, true);
        assertSame(firstRead, assertThrows(IllegalStateException.class, () -> filter(data, kind, 0, Integer.MAX_VALUE, Integer.MAX_VALUE, value -> false)));
        assertEquals(0, filter(data, kind, 0, Integer.MAX_VALUE, 0, value -> fail("max zero must not evaluate rows")).size());
        assertEquals(0,
                filter(data, kind, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, value -> fail("empty range must not evaluate rows")).size());
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 1, 2, 3 })
    void filterGrowsWithMatchesAndPreservesDataContracts(final int kind) {
        final List<List<Object>> columns = List.of(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        final List<Integer> expectedIndexes = new ArrayList<>();
        for (int i = 0; i < 600; i++) {
            columns.get(0).add(i % 3 == 0 ? null : "\uD83D\uDE00");
            columns.get(1).add(i);
            columns.get(2).add("\u6D77");
            if (i % 3 != 0) {
                expectedIndexes.add(i);
            }
        }
        final Dataset data = new RowDataset(List.of("a", "b", "c"), columns, Map.of("origin", "test"));
        final Dataset result = filter(data, kind, 0, data.size(), Integer.MAX_VALUE, java.util.Objects::nonNull);
        assertEquals(400, result.size());
        assertEquals(expectedIndexes, result.getColumn("b"));
        assertEquals(data.columnNames(), result.columnNames());
        assertEquals(Map.of("origin", "test"), result.getProperties());
        assertEquals("\uD83D\uDE00", result.get(0, 0));
        assertEquals("\u6D77", result.get(0, 2));
        result.set(0, 0, "changed");
        assertEquals("\uD83D\uDE00", data.get(1, 0));
        assertEquals(List.of(4), filter(data, kind, 3, 6, 1, java.util.Objects::nonNull).getColumn("b"));
        assertEquals(List.of(3), filter(data, kind, 3, 6, 1, java.util.Objects::isNull).getColumn("b"));
        final var evaluations = new java.util.concurrent.atomic.AtomicInteger();
        assertEquals(2, filter(data, kind, 3, 6, 2, value -> {
            evaluations.incrementAndGet();
            return true;
        }).size());
        assertEquals(2, evaluations.get());
        assertEquals(0, filter(data, kind, 0, data.size(), Integer.MAX_VALUE, value -> false).size());
        final Dataset empty = Dataset.rows(List.of("a", "b", "c"), new Object[0][]);
        assertEquals(empty, filter(empty, kind, 0, 0, Integer.MAX_VALUE, value -> fail("empty input")));
        assertThrows(IllegalArgumentException.class, () -> filter(data, kind, 0, 1, -1, value -> true));
        assertThrows(IndexOutOfBoundsException.class, () -> filter(data, kind, -1, 1, 1, value -> true));
        assertThrows(IndexOutOfBoundsException.class, () -> filter(data, kind, 0, 601, 1, value -> true));
    }

    private static Dataset fourColumns() {
        return Dataset.rows(List.of("a", "b", "c", "d"), new Object[][] { { 1, 2, 3, 4 } });
    }

    @ParameterizedTest
    @ValueSource(strings = { "a,a,c", "a,c,c", "c,a,c", "a,a", "b,b,b" })
    void duplicateMovesFailBeforeChangingColumns(final String selection) {
        for (final int target : new int[] { 0, 1 }) {
            final Dataset data = fourColumns();
            final var stream = data.stream("a");
            final IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                    () -> data.moveColumns(Arrays.asList(selection.split(",")), target));
            assertTrue(error.getMessage().contains("Duplicated column names"));
            assertEquals(List.of("a", "b", "c", "d"), data.columnNames());
            assertEquals(List.of(1, 2, 3, 4), data.getRow(0));
            assertEquals(2, data.getColumnIndex("c"));
            assertEquals(List.of(1), stream.toList());
        }
    }

    @Test
    void uniqueMovesPreserveRequestedOrderAndValues() {
        final Dataset data = fourColumns();
        final var originalB = data.getColumn("b");
        data.moveColumns(List.of("b", "c"), 0);
        assertEquals(List.of("b", "c", "a", "d"), data.columnNames());
        assertEquals(List.of(2, 3, 1, 4), data.getRow(0));
        data.moveColumns(List.of("d", "b"), 1);
        assertEquals(List.of("c", "d", "b", "a"), data.columnNames());
        assertEquals(List.of(3, 4, 2, 1), data.getRow(0));
        data.moveColumns(List.of("a", "b", "d", "c"), 0);
        assertEquals(List.of(1, 2, 4, 3), data.getRow(0));
        data.moveColumns(List.of("c"), 0);
        assertEquals(List.of("c", "a", "b", "d"), data.columnNames());
        assertEquals(List.of(2), originalB);
        final var stream = data.stream("a");
        data.moveColumns(List.of("c", "a"), 0);
        data.moveColumns(List.of(), Integer.MAX_VALUE);
        assertEquals(List.of(1), stream.toList());
    }

    @Test
    void moveSelectionBoundariesAndEmptyUnicodeSchema() {
        final Dataset data = fourColumns();
        assertThrows(IllegalArgumentException.class, () -> data.moveColumns(null, 0));
        assertThrows(IllegalArgumentException.class, () -> data.moveColumns(Arrays.asList((String) null), 0));
        assertThrows(IllegalArgumentException.class, () -> data.moveColumns(List.of("missing"), 0));
        assertThrows(IndexOutOfBoundsException.class, () -> data.moveColumns(List.of("a", "b"), -1));
        assertThrows(IndexOutOfBoundsException.class, () -> data.moveColumns(List.of("a", "b"), 3));
        assertEquals(List.of(1, 2, 3, 4), data.getRow(0));
        final Dataset empty = Dataset.rows(List.of("\uD83D\uDE00", "\u6D77", "z"), new Object[0][]);
        empty.moveColumns(List.of("z", "\uD83D\uDE00"), 1);
        assertEquals(List.of("\u6D77", "z", "\uD83D\uDE00"), empty.columnNames());
        assertEquals(0, empty.size());
        assertThrows(IllegalArgumentException.class, () -> empty.moveColumns(List.of("\u6D77", "\u6D77"), 0));
        final Dataset nullCell = Dataset.rows(List.of("a", "b"), new Object[][] { { null, 1 } });
        nullCell.moveColumns(List.of("b", "a"), 0);
        assertEquals(Arrays.asList(1, null), nullCell.getRow(0));
    }
}
