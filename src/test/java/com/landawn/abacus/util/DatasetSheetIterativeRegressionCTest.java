package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.landawn.abacus.TestBase;

/** Regressions recorded in the Dataset/RowDataset/Sheet iterative review ledger. */
public class DatasetSheetIterativeRegressionCTest extends TestBase {
    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void constructorSeparatesEveryAliasedColumn(final boolean differentWrappers) {
        for (boolean withProperties : new boolean[] { false, true }) {
            final List<Object> source = new ArrayList<>(List.of(1, 2));
            final List<Object> left = differentWrappers ? Collections.synchronizedList(source) : source;
            final List<Object> right = differentWrappers ? Collections.synchronizedList(source) : source;
            final List<String> names = new ArrayList<>(List.of("left", "right"));
            final List<List<Object>> columns = new ArrayList<>(List.of(left, right));
            final Dataset data = withProperties ? new RowDataset(names, columns, Map.of("source", "test")) : new RowDataset(names, columns);
            data.addRow(new Object[] { 3, 30 });
            assertEquals(3, data.size());
            assertEquals(List.of(1, 2, 3), data.getColumn("left"));
            assertEquals(List.of(1, 2, 30), data.getColumn("right"));
            data.swapRows(0, 2);
            assertEquals(List.of(3, 2, 1), data.getColumn("left"));
            assertEquals(List.of(30, 2, 1), data.getColumn("right"));
            data.set(0, 0, 4);
            assertEquals(30, (int) data.get(0, 1));
            data.removeRow(1);
            assertEquals(List.of(4, 1), data.getColumn("left"));
            assertEquals(List.of(30, 1), data.getColumn("right"));
            data.addRows(List.of(new Object[] { 5, 50 }, new Object[] { 6, 60 }));
            assertEquals(4, data.size());
            data.clear();
            assertEquals(0, data.size());
            assertEquals(List.of(1, 2), source);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void constructorSupportsImmutableAndEmptyInputs(final boolean withProperties) {
        final List<String> names = List.of("value");
        final List<List<Object>> columns = List.of(Arrays.asList(null, "\uD83D\uDE00"));
        final Dataset data = withProperties ? new RowDataset(names, columns, null) : new RowDataset(names, columns);
        data.addColumn("second", List.of(1, 2));
        data.addRow(new Object[] { "end", 3 });
        assertEquals(Arrays.asList(null, "\uD83D\uDE00", "end"), data.getColumn("value"));
        assertEquals(1, names.size());
        assertEquals(2, columns.get(0).size());
        final Dataset empty = withProperties ? new RowDataset(List.of(), List.of(), Map.of()) : new RowDataset(List.of(), List.of());
        empty.addColumn("value", List.of(1));
        empty.addRow(new Object[] { 2 });
        assertEquals(List.of(1, 2), empty.getColumn("value"));
    }

    @Test
    void constructorCopiesAllListLayersButSharesCellObjects() {
        final List<Object> payload = new ArrayList<>(List.of("initial"));
        final List<String> names = new ArrayList<>(List.of("value"));
        final List<Object> column = new ArrayList<>(Arrays.asList(payload, null));
        final List<List<Object>> columns = new ArrayList<>(List.of(column));
        final Dataset data = new RowDataset(names, columns, Map.of("payload", payload));
        names.set(0, "changed");
        column.clear();
        columns.clear();
        assertEquals(List.of("value"), data.columnNames());
        assertEquals(2, data.size());
        assertSame(payload, data.get(0, 0));
        assertSame(payload, data.getProperties().get("payload"));
        assertNull(data.get(1, 0));
        data.freeze();
        payload.add("shared");
        assertEquals(List.of("initial", "shared"), data.get(0, 0));
    }

    @Test
    void publicConstructorKeepsDatasetViewsLive() {
        final Dataset data = new RowDataset(List.of("value"), List.of(new ArrayList<>(List.of(1, 2))));
        final var column = data.getColumn(0);
        final var names = data.columnNames();
        final Dataset slice = data.slice(0, 1);
        data.set(0, 0, 9);
        assertEquals(9, column.get(0));
        assertEquals(9, (int) slice.get(0, 0));
        data.renameColumn("value", "renamed");
        assertEquals(List.of("renamed"), names);
        assertEquals(List.of("value"), slice.columnNames());
        final Dataset union = data.union(Dataset.rows(List.of("renamed"), new Object[][] { { 2 }, { 3 } }));
        assertEquals(List.of(9, 2, 3), union.getColumn(0));
    }

    @Test
    void publicConstructorStillRejectsInvalidShape() {
        assertThrows(IllegalArgumentException.class, () -> new RowDataset(null, List.of()));
        assertThrows(IllegalArgumentException.class, () -> new RowDataset(List.of(), null));
        assertThrows(IllegalArgumentException.class, () -> new RowDataset(List.of("x"), Arrays.asList((List<Object>) null)));
        assertThrows(IllegalArgumentException.class, () -> new RowDataset(List.of("x", "x"), List.of(List.of(), List.of())));
        assertThrows(IllegalArgumentException.class, () -> new RowDataset(List.of(""), List.of(List.of())));
        assertThrows(IllegalArgumentException.class, () -> new RowDataset(List.of("x", "y"), List.of(List.of(1), List.of())));
    }

    public record Box(Integer value) {
    }

    public record IntBox(int value) {
    }

    public record Parent(Box child) {
    }

    public static class MutableBox {
        private Integer value;

        public Integer getValue() {
            return value;
        }

        public void setValue(final Integer value) {
            this.value = value;
        }
    }

    static java.util.stream.Stream<Arguments> conversionCases() {
        return java.util.stream.Stream.of(Arguments.of(new Object[] { 1, "2" }, Arrays.asList(1, 2)),
                Arguments.of(new Object[] { "2", 1 }, Arrays.asList(2, 1)), Arguments.of(new Object[] { null, 1, "2" }, Arrays.asList(null, 1, 2)),
                Arguments.of(new Object[] { 1, null, "2" }, Arrays.asList(1, null, 2)), Arguments.of(new Object[] { 1, "2", null }, Arrays.asList(1, 2, null)),
                Arguments.of(new Object[] { null, null }, Arrays.asList(null, null)), Arguments.of(new Object[0], List.of()),
                Arguments.of(new Object[] { Integer.MIN_VALUE, "2147483647" }, List.of(Integer.MIN_VALUE, Integer.MAX_VALUE)));
    }

    @ParameterizedTest
    @MethodSource("conversionCases")
    void recordConversionIsIndependentOfRowOrderAndAccessPath(final Object[] inputs, final List<Integer> expectedValues) {
        final Object[][] rows = new Object[inputs.length][2];
        for (int i = 0; i < inputs.length; i++) {
            rows[i] = new Object[] { "row-\uD83D\uDE00-" + i, inputs[i] };
        }
        final Dataset data = Dataset.rows(List.of("id", "value"), rows);
        final List<Box> expected = expectedValues.stream().map(Box::new).toList();
        final List<IntBox> primitiveExpected = expectedValues.stream().map(v -> new IntBox(v == null ? 0 : v)).toList();
        assertEquals(expected, data.toList(Box.class));
        assertEquals(primitiveExpected, data.toList(IntBox.class));
        assertEquals(expected, data.stream(Box.class).toList());
        assertEquals(primitiveExpected, data.stream(IntBox.class).toList());
        assertEquals(expected, data.toEntities(Map.of(), Box.class));
        assertEquals(expected, new ArrayList<>(data.toMap("id", List.of("value"), Box.class).values()));
        assertEquals(primitiveExpected, new ArrayList<>(data.toMap("id", List.of("value"), IntBox.class).values()));
        final ListMultimap<String, Box> multimap = data.toMultimap("id", List.of("value"), Box.class);
        final List<MutableBox> beans = data.toList(MutableBox.class);
        final Map<String, MutableBox> suppliedBeans = data.toMap("id", List.of("value"), ignored -> new MutableBox());
        final ListMultimap<String, MutableBox> suppliedMultimap = data.toMultimap("id", List.of("value"), ignored -> new MutableBox());
        for (int i = 0; i < inputs.length; i++) {
            assertEquals(expected.get(i), data.getRow(i, Box.class));
            assertEquals(primitiveExpected.get(i), data.getRow(i, IntBox.class));
            assertEquals(expectedValues.get(i), beans.get(i).getValue());
            final String key = "row-\uD83D\uDE00-" + i;
            assertEquals(List.of(expected.get(i)), multimap.get(key));
            assertEquals(expectedValues.get(i), suppliedBeans.get(key).getValue());
            assertEquals(expectedValues.get(i), suppliedMultimap.get(key).get(0).getValue());
        }
        if (inputs.length == 0) {
            assertFalse(data.firstRow(Box.class).isPresent());
            assertFalse(data.lastRow(IntBox.class).isPresent());
        } else {
            assertEquals(expected.get(0), data.firstRow(Box.class).get());
            assertEquals(primitiveExpected.get(inputs.length - 1), data.lastRow(IntBox.class).get());
        }
    }

    @Test
    void nestedRecordsUseTheSameConversionsInListsStreamsAndMaps() {
        final Dataset data = Dataset.rows(List.of("id", "child.value"), new Object[][] { { "first", 1 }, { "second", "2" } });
        final List<Parent> expected = List.of(new Parent(new Box(1)), new Parent(new Box(2)));
        assertEquals(expected, data.toList(Parent.class));
        assertEquals(expected, data.stream(Parent.class).toList());
        assertEquals(expected.get(1), data.getRow(1, Parent.class));
        assertEquals(expected, new ArrayList<>(data.toMap("id", List.of("child.value"), Parent.class).values()));
        assertEquals(List.of(expected.get(1)), data.<String, Parent> toMultimap("id", List.of("child.value"), Parent.class).get("second"));
        final Dataset strict = data.withMissingPropertyPolicy(Dataset.MissingPropertyPolicy.ERROR);
        assertThrows(IllegalArgumentException.class, () -> strict.toList(Parent.class));
        assertEquals(expected, strict.toList(List.of("child.value"), Parent.class));
    }

    @Test
    void recordConversionPreservesAssignableIdentityAndRejectsInvalidNumbers() {
        final Integer payload = Integer.valueOf(10000);
        final Dataset data = Dataset.rows(List.of("value"), new Object[][] { { payload }, { "2" } });
        assertSame(payload, data.toList(Box.class).get(0).value());
        assertSame(payload, data.getRow(0, Box.class).value());
        for (Object invalid : new Object[] { "bad", "2147483648", Double.NaN, Double.POSITIVE_INFINITY, Long.MAX_VALUE }) {
            final Dataset bad = Dataset.rows(List.of("value"), new Object[][] { { 1 }, { invalid } });
            final Class<? extends RuntimeException> errorType = "bad".equals(invalid) ? IllegalArgumentException.class : ArithmeticException.class;
            assertThrows(errorType, () -> bad.toList(Box.class));
            assertThrows(errorType, () -> bad.getRow(1, Box.class));
            assertThrows(errorType, () -> bad.stream(IntBox.class).toList());
        }
    }

    public static class CloneNode {
        public Object value;
        public Object second;
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void cyclicSliceCloneCopiesOnlyLogicalRows(final boolean freeze) {
        final Dataset root = Dataset.rows(List.of("self-\uD83D\uDE00"), new Object[][] { { new Thread() }, { null }, { new Thread() } });
        final Dataset view = root.slice(1, 2);
        root.set(1, 0, view);
        final Dataset copy = view.clone(freeze);
        assertNotSame(view, copy);
        assertSame(copy, copy.get(0, 0));
        assertEquals(freeze, copy.isFrozen());
        assertEquals(1, copy.size());
        root.clear();
        assertEquals(1, copy.size());
        assertSame(copy, copy.get(0, 0));
    }

    @Test
    void clonePreservesMutualSlicesAndTheirPolicies() {
        final Dataset left = Dataset.rows(List.of("link"), new Object[][] { { null } });
        final Dataset right = Dataset.rows(List.of("link"), new Object[][] { { null } });
        final Dataset a = left.withMissingPropertyPolicy(Dataset.MissingPropertyPolicy.ERROR);
        final Dataset b = right.slice(0, 1);
        left.set(0, 0, b);
        right.set(0, 0, a);
        final Dataset copy = a.clone(false);
        final Dataset copyB = copy.get(0, 0);
        assertNotSame(b, copyB);
        assertSame(copy, copyB.get(0, 0));
        assertTrue(copyB.isFrozen());
        assertFalse(copy.isFrozen());
        assertThrows(IllegalArgumentException.class, () -> copy.toList(Box.class));
        assertEquals(List.of(new Box(null)), copyB.toList(Box.class));
    }

    @Test
    void clonePreservesNestedDatasetAndMetadataIdentity() {
        final Dataset root = Dataset.rows(List.of("value"), new Object[][] { { null } });
        final CloneNode node = new CloneNode();
        root.setProperties(Map.of("node", node));
        final Dataset view = root.slice(0, 1);
        node.value = view;
        node.second = new ArrayList<>(List.of(view));
        root.set(0, 0, node);
        final Dataset container = Dataset.rows(List.of("view", "node"), new Object[][] { { view, node }, { view, node } });
        container.moveToRow(1);
        container.freeze();
        final Dataset copy = container.clone();
        final Dataset copiedView = copy.get(0, 0);
        final CloneNode copiedNode = copy.get(0, 1);
        assertTrue(copy.isFrozen());
        assertEquals(1, copy.currentRowIndex());
        assertNotSame(view, copiedView);
        assertNotSame(node, copiedNode);
        assertSame(copiedView, copy.get(1, 0));
        assertSame(copiedNode, copy.get(1, 1));
        assertSame(copiedView, copiedNode.value);
        assertSame(copiedView, ((List<?>) copiedNode.second).get(0));
        assertSame(copiedNode, copiedView.getProperties().get("node"));
        assertSame(copiedNode, copiedView.get(0, 0));
    }

    @Test
    void sheetClonePreservesCyclesThroughDatasetSlices() {
        final Dataset root = Dataset.rows(List.of("sheet"), new Object[][] { { null } });
        final Dataset view = root.slice(0, 1);
        final Sheet<String, String, Object> sheet = new Sheet<>(List.of("r"), List.of("c"));
        sheet.set("r", "c", view);
        root.set(0, 0, sheet);
        final Sheet<String, String, Object> copy = sheet.clone(true);
        final Dataset copiedView = (Dataset) copy.get("r", "c");
        assertTrue(copy.isFrozen());
        assertTrue(copiedView.isFrozen());
        assertSame(copy, copiedView.get(0, 0));
        assertNotSame(view, copiedView);
        sheet.set("r", "c", null);
        assertSame(copiedView, copy.get("r", "c"));
    }

    @Test
    void cloneHandlesEmptyDatasetsAndRejectsInvalidatedSlices() {
        final Dataset empty = Dataset.empty().clone(false);
        empty.addColumn("value", List.of(1));
        assertEquals(0, Dataset.empty().columnCount());
        final Dataset root = Dataset.rows(List.of("value"), new Object[][] { { 1 } });
        final Dataset zeroRows = root.slice(0, 0).clone(false);
        zeroRows.addRow(new Object[] { null });
        assertNull(zeroRows.get(0, 0));
        final Dataset invalid = root.slice(0, 1);
        root.clear();
        assertThrows(java.util.ConcurrentModificationException.class, () -> invalid.clone(false));
    }

    @Test
    void optionalKryoDoesNotPreventLoadingDatasetOrSheet() throws Exception {
        final java.net.URL[] urls = Arrays.stream(System.getProperty("java.class.path").split(java.io.File.pathSeparator)).map(path -> {
            try {
                return java.nio.file.Path.of(path).toUri().toURL();
            } catch (java.net.MalformedURLException e) {
                throw new AssertionError(e);
            }
        }).toArray(java.net.URL[]::new);
        try (var loader = new java.net.URLClassLoader(urls, ClassLoader.getPlatformClassLoader()) {
            @Override
            protected Class<?> loadClass(final String name, final boolean resolve) throws ClassNotFoundException {
                if (name.startsWith("com.esotericsoftware.kryo.")) {
                    throw new ClassNotFoundException(name);
                }
                return super.loadClass(name, resolve);
            }
        }) {
            final Class<?> datasetType = loader.loadClass("com.landawn.abacus.util.Dataset");
            final Object empty = datasetType.getMethod("empty").invoke(null);
            final var datasetFailure = assertThrows(java.lang.reflect.InvocationTargetException.class,
                    () -> datasetType.getMethod("clone", boolean.class).invoke(empty, false));
            assertInstanceOf(UnsupportedOperationException.class, datasetFailure.getCause());
            final Class<?> sheetType = loader.loadClass("com.landawn.abacus.util.Sheet");
            final Object sheet = sheetType.getConstructor(java.util.Collection.class, java.util.Collection.class).newInstance(List.of(), List.of());
            final var sheetFailure = assertThrows(java.lang.reflect.InvocationTargetException.class,
                    () -> sheetType.getMethod("clone", boolean.class).invoke(sheet, false));
            assertInstanceOf(UnsupportedOperationException.class, sheetFailure.getCause());
        }
    }

    @Test
    void failedColumnReadLeavesSchemaAndIteratorsUnchanged() {
        final Dataset data = Dataset.rows(List.of("a"), new Object[][] { { 1 } });
        final var names = data.columnNames();
        final var pending = data.stream("a");
        final java.util.Collection<Object> broken = new java.util.AbstractCollection<>() {
            @Override
            public int size() {
                return 1;
            }

            @Override
            public java.util.Iterator<Object> iterator() {
                throw new IllegalStateException("read failed");
            }
        };
        final var failure = assertThrows(IllegalStateException.class, () -> data.addColumn(0, "broken", broken));
        assertEquals("read failed", failure.getMessage());
        assertEquals(List.of("a"), names);
        assertEquals(1, data.columnCount());
        assertEquals(0, data.getColumnIndex("a"));
        assertFalse(data.containsColumn("broken"));
        assertEquals(List.of(1), pending.toList());
        data.addColumn("ok", List.of(2));
        assertEquals(List.of(2), data.getColumn("ok"));
    }

    @Test
    void addingColumnSnapshotsLiveRowsAndColumnNames() {
        final Dataset data = Dataset.rows(List.of("a", "\uD83D\uDE00"), new Object[][] { { 1, 10 }, { 2, 20 } });
        data.addColumn("headers", data.columnNames());
        assertEquals(List.of("a", "\uD83D\uDE00"), data.getColumn("headers"));
        final Dataset square = Dataset.rows(List.of("a", "b"), new Object[][] { { 1, null }, { 2, 20 } });
        square.addColumn(0, "row", square.getRow(0));
        assertEquals(Arrays.asList(1, null), square.getColumn("row"));
        assertEquals(List.of(1, 2), square.getColumn("a"));
        assertEquals(2, square.size());
    }

    @Test
    void addingNullOrEmptyColumnsPreservesRowCounts() {
        final Dataset data = Dataset.rows(List.of("a"), new Object[][] { { 1 }, { 2 } });
        data.addColumn("null", (java.util.Collection<?>) null);
        data.addColumn("empty", List.of());
        assertEquals(Arrays.asList(null, null), data.getColumn("null"));
        assertEquals(Arrays.asList(null, null), data.getColumn("empty"));
        final Dataset empty = new RowDataset(List.of(), List.of());
        empty.addColumn("first", (java.util.Collection<?>) null);
        assertEquals(0, empty.size());
        assertEquals(List.of("first"), empty.columnNames());
        assertThrows(IndexOutOfBoundsException.class, () -> data.addColumn(-1, "bad", List.of()));
        assertThrows(IndexOutOfBoundsException.class, () -> data.addColumn(4, "bad", List.of()));
        assertThrows(IllegalArgumentException.class, () -> data.addColumn("bad", List.of(1)));
        assertEquals(3, data.columnCount());
    }

    private static com.landawn.abacus.util.stream.Stream<Integer> innerColumn(final Sheet<String, String, Integer> sheet, final String family) {
        return switch (family) {
            case "cells" -> sheet.columnCells(1, 2).first().get().map(Sheet.Cell::value);
            case "pairs" -> sheet.columns(1, 2).first().get().right();
            default -> sheet.columnStreams(1, 2).first().get();
        };
    }

    @ParameterizedTest
    @ValueSource(strings = { "values", "cells", "pairs" })
    void innerColumnStreamsObserveFirstAndLaterWrites(final String family) {
        for (boolean initialized : new boolean[] { false, true }) {
            final Sheet<String, String, Integer> sheet = new Sheet<>(List.of("r1", "r2", "r3"), List.of("left", "\uD83D\uDE00"));
            if (initialized) {
                sheet.set("r1", "left", null);
            }
            final var pending = innerColumn(sheet, family);
            sheet.set("r1", "\uD83D\uDE00", 7);
            final var iterator = pending.iterator();
            assertEquals(7, iterator.next());
            sheet.set("r2", "\uD83D\uDE00", 8);
            sheet.set("r2", "\uD83D\uDE00", null);
            assertNull(iterator.next());
            sheet.set("r3", "\uD83D\uDE00", 9);
            assertEquals(9, iterator.next());
            assertFalse(iterator.hasNext());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { "values", "cells", "pairs" })
    void innerColumnStreamsHandleEmptyAxesAndRanges(final String family) {
        final Sheet<String, String, Integer> emptyRows = new Sheet<>(List.of(), List.of("left", "right"));
        assertEquals(List.of(), innerColumn(emptyRows, family).toList());
        final Sheet<String, String, Integer> noColumns = new Sheet<>(List.of("r"), List.of());
        assertEquals(0, noColumns.columnStreams().count());
        assertEquals(0, noColumns.columnCells().count());
        assertEquals(0, noColumns.columns().count());
        assertEquals(0, emptyRows.columnStreams(1, 1).count());
        assertEquals(0, emptyRows.columnCells(1, 1).count());
        assertEquals(0, emptyRows.columns(1, 1).count());
        assertThrows(IndexOutOfBoundsException.class, () -> emptyRows.columnStreams(-1, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> emptyRows.columnCells(0, 3));
        assertThrows(IndexOutOfBoundsException.class, () -> emptyRows.columns(2, 1));
    }

    @ParameterizedTest
    @ValueSource(ints = { 1, 2 })
    void unionDoesNotAllocateByOverflowingInputRowCount(final int keyCount) {
        final List<Object> virtual = new java.util.AbstractList<>() {
            @Override
            public int size() {
                return Integer.MAX_VALUE;
            }

            @Override
            public Object get(final int index) {
                throw new IllegalStateException("first cell requested");
            }
        };
        final List<String> names = keyCount == 1 ? List.of("key") : List.of("key", "second");
        final List<List<Object>> columns = new ArrayList<>();
        for (int i = 0; i < keyCount; i++) {
            columns.add(virtual);
        }
        // Trusted storage allows a virtual boundary fixture without materializing billions of cells.
        final Dataset huge = new RowDataset(new ArrayList<>(names), columns, null, true);
        huge.freeze();
        final var failure = assertThrows(IllegalStateException.class, () -> huge.unionBy(huge, names));
        assertEquals("first cell requested", failure.getMessage());
    }

    @Test
    void unionRetainsFirstRowsForDeepNullAndUnicodeKeysAcrossSchemas() {
        final Dataset left = Dataset.rows(List.of("key", "left"),
                new Object[][] { { new int[] { 1 }, "\uD83D\uDE00" }, { new int[] { 1 }, "duplicate" }, { null, "null key" } });
        final Dataset right = Dataset.rows(List.of("key", "right"),
                new Object[][] { { new int[] { 1 }, 10 }, { new int[] { 2 }, 20 }, { null, 30 }, { new int[] { 2 }, 40 } });
        final Dataset union = left.unionBy(right, List.of("key"));
        final Dataset expected = Dataset.rows(List.of("key", "left", "right"),
                new Object[][] { { new int[] { 1 }, "\uD83D\uDE00", null }, { null, "null key", null }, { new int[] { 2 }, null, 20 } });
        assertEquals(expected, union);
        assertEquals(3, left.size());
        assertEquals(4, right.size());
        final Dataset empty = Dataset.empty().union(Dataset.empty());
        assertEquals(0, empty.size());
        empty.addColumn("value", List.of(1));
        assertEquals(1, empty.size());
    }

    @Test
    void distinctContractsUseDeepKeysAndRetainFirstEncounteredRows() {
        final Dataset data = Dataset.rows(List.of("key", "id"), new Object[][] { { new Object[] { new int[] { 1 }, "\uD83D\uDE00", null }, 1 },
                { new Object[] { new int[] { 1 }, "\uD83D\uDE00", null }, 2 }, { null, 3 }, { null, 4 } });
        assertEquals(List.of(1, 3), data.distinctBy("key").getColumn("id"));
        assertEquals(List.of(1, 3), data.distinctBy(List.of("key")).getColumn("id"));
        assertEquals(List.of(1, 3), data.distinctBy("key", value -> value).getColumn("id"));
        assertEquals(List.of(1, 3), data.distinctBy(List.of("key"), row -> row.get(0)).getColumn("id"));
        assertEquals(2, data.copy(List.of("key")).distinct().size());
        for (int mode = 0; mode < 4; mode++) {
            final Dataset copy = data.copy();
            switch (mode) {
                case 0 -> copy.removeDuplicateRowsBy("key");
                case 1 -> copy.removeDuplicateRowsBy(List.of("key"));
                case 2 -> copy.removeDuplicateRowsBy("key", value -> value);
                default -> copy.removeDuplicateRowsBy(List.of("key"), row -> row.get(0));
            }
            assertEquals(List.of(1, 3), copy.getColumn("id"));
        }
        assertEquals(4, data.size());
        assertEquals(0, data.copy(0, 0).distinctBy("key").size());
    }

    @Test
    void pointsUsePositionsEvenWhenIntegerKeysDiffer() {
        final Sheet<Integer, Integer, String> sheet = Sheet.rows(List.of(1, 0), List.of(1, 0),
                new String[][] { { "\uD83D\uDE00", null }, { "other", "key zero" } });
        final Sheet.Point origin = Sheet.Point.of(0, 0);
        assertEquals("key zero", sheet.get(0, 0));
        assertEquals("\uD83D\uDE00", sheet.get(origin));
        assertFalse(sheet.isNull(origin));
        assertEquals("\uD83D\uDE00", sheet.set(origin, null));
        assertTrue(sheet.isNull(origin));
        assertEquals("key zero", sheet.get(0, 0));
        assertThrows(IllegalArgumentException.class, () -> sheet.get((Sheet.Point) null));
        assertThrows(IllegalArgumentException.class, () -> sheet.isNull((Sheet.Point) null));
        assertThrows(IllegalArgumentException.class, () -> sheet.set((Sheet.Point) null, "x"));
        assertThrows(IndexOutOfBoundsException.class, () -> sheet.get(Sheet.Point.of(2, 0)));
        assertThrows(IndexOutOfBoundsException.class, () -> sheet.isNull(Sheet.Point.of(0, 2)));
        assertThrows(IndexOutOfBoundsException.class, () -> sheet.set(Sheet.Point.of(2, 2), "x"));
        final Sheet<Integer, Integer, String> empty = new Sheet<>(List.of(), List.of());
        assertThrows(IndexOutOfBoundsException.class, () -> empty.get(origin));
    }

    @ParameterizedTest
    @ValueSource(strings = { "root", "slice", "nested", "empty" })
    void lazyTraversalsRejectInvalidatedDatasetsOnEveryControlPath(final String shape) {
        for (String mutation : List.of("swap", "add", "remove")) {
            final Dataset root = Dataset.rows(List.of("a", "b", "\uD83D\uDE00"), new Object[][] { { 1, null, 3 }, { 4, 5, 6 } });
            final Dataset view = switch (shape) {
                case "slice" -> root.slice(0, 2);
                case "nested" -> root.slice(0, 2).slice(1, 2);
                case "empty" -> root.slice(0, 0);
                default -> root;
            };
            final List<Runnable> reads = new ArrayList<>();
            final var scalarCount = view.stream("a");
            reads.add(scalarCount::count);
            final var scalarList = view.stream("a");
            reads.add(scalarList::toList);
            final var scalarArray = view.stream("a");
            reads.add(scalarArray::toArray);
            final var scalarSkip = view.stream("a").skip(1);
            reads.add(scalarSkip::count);
            final var scalarIterator = view.stream("a").iterator();
            reads.add(scalarIterator::hasNext);
            reads.add(scalarIterator::next);
            final var typed = view.stream(Object[].class);
            reads.add(typed::count);
            final var supplied = view.stream(size -> new Object[size]);
            reads.add(supplied::count);
            final var mapped = view.stream((index, row) -> row.get(0));
            reads.add(mapped::count);
            final var pairs = view.stream(Tuple.of("a", "b"), (a, b) -> a);
            reads.add(pairs::count);
            final var triples = view.stream(Tuple.of("a", "b", "\uD83D\uDE00"), (a, b, c) -> c);
            reads.add(triples::count);
            final var bi = view.iterator("a", "b");
            reads.add(bi::hasNext);
            reads.add(bi::next);
            reads.add(() -> bi.forEachRemaining((a, b) -> fail("invalidated iterator called consumer")));
            final var tri = view.iterator("a", "b", "\uD83D\uDE00");
            reads.add(tri::hasNext);
            reads.add(tri::next);
            reads.add(() -> tri.forEachRemaining((a, b, c) -> fail("invalidated iterator called consumer")));
            final var chunks = view.split(1);
            reads.add(chunks::count);
            final var chunkList = view.split(1);
            reads.add(chunkList::toList);
            final var chunkSkip = view.split(1).skip(Long.MAX_VALUE);
            reads.add(chunkSkip::count);
            final var chunkIterator = view.split(1).iterator();
            reads.add(chunkIterator::hasNext);
            reads.add(chunkIterator::next);
            final var pages = view.paginate(1);
            if (!view.isEmpty()) {
                pages.getPage(0);
            }
            reads.add(() -> pages.getPage(0));
            reads.add(pages::firstPage);
            reads.add(pages::lastPage);
            reads.add(pages::totalPages);
            final var pageIterator = pages.iterator();
            reads.add(pageIterator::hasNext);
            reads.add(pageIterator::next);
            switch (mutation) {
                case "swap" -> root.swapRows(0, 1);
                case "add" -> root.addRow(new Object[] { 7, 8, 9 });
                default -> root.removeRow(0);
            }
            for (int i = 0; i < reads.size(); i++) {
                final Runnable read = reads.get(i);
                assertThrows(java.util.ConcurrentModificationException.class, read::run, shape + "/" + mutation + "/operation " + i);
            }
        }
    }

    @Test
    void validLazyViewsObserveValueUpdatesAndRetainIteratorSemantics() {
        final Dataset root = Dataset.rows(List.of("a", "b", "c"), new Object[][] { { 1, null, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
        final Dataset view = root.slice(0, 3).slice(0, 3);
        final BiIterator<Integer, Integer> bi = view.iterator("a", "b");
        final TriIterator<Integer, Integer, Integer> tri = view.iterator("a", "b", "c");
        final var chunks = view.split(2);
        final var pages = view.paginate(2);
        root.set(0, 0, 10);
        root.trimToSize();
        assertEquals(Pair.of(10, null), bi.next());
        final List<Integer> sums = new ArrayList<>();
        bi.forEachRemaining((a, b) -> sums.add(a + b));
        assertEquals(List.of(9, 15), sums);
        assertFalse(bi.hasNext());
        assertThrows(java.util.NoSuchElementException.class, bi::next);
        assertEquals(Triple.of(10, null, 3), tri.next());
        sums.clear();
        tri.forEachRemaining((a, b, c) -> sums.add(a + b + c));
        assertEquals(List.of(15, 24), sums);
        assertThrows(java.util.NoSuchElementException.class, tri::next);
        assertEquals(List.of(2, 1), chunks.map(Dataset::size).toList());
        assertEquals(10, pages.getPage(0).<Integer> get(0, 0));
        assertEquals(2, view.split(2).count());
        assertEquals(1, view.split(2).skip(1).count());
        assertEquals(0, view.split(2).skip(Long.MAX_VALUE).count());
        assertEquals(1, view.split(Integer.MAX_VALUE).count());
        assertEquals(0, view.slice(0, 0).split(Integer.MAX_VALUE).count());
        final var iterator = view.split(2).iterator();
        assertEquals(2, iterator.next().size());
        assertEquals(1, iterator.next().size());
        assertFalse(iterator.hasNext());
        assertThrows(java.util.NoSuchElementException.class, iterator::next);
    }

    /**
     * {@code RowDataset} distinguishes "no properties" from a caller-supplied map by comparing
     * {@code _properties} against the shared {@code EMPTY_PROPERTIES} instance with {@code ==}. Static
     * initializers run in textual order, and {@code EMPTY_DATASET} used to be declared before
     * {@code EMPTY_PROPERTIES}, so the shared empty dataset was constructed while that field was still
     * {@code null} - leaving {@code Dataset.empty()} holding {@code null} properties and flipping every
     * identity guard against it. Observable through the public API as {@code getProperties()} handing back a
     * freshly wrapped map instead of the shared empty one.
     */
    @Test
    public void test_emptyDataset_propertiesAreTheSharedEmptyMap_regression_20260918() {
        // Before the fix Dataset.empty()._properties was null, so this returned a wrapper, not the shared map.
        assertSame(N.<String, Object> emptyMap(), Dataset.empty().getProperties());

        // The identity is stable across calls, which is what the == guards in RowDataset rely on.
        assertSame(Dataset.empty().getProperties(), Dataset.empty().getProperties());

        // And the empty dataset still behaves as an empty, frozen dataset.
        assertTrue(Dataset.empty().getProperties().isEmpty());
        assertTrue(Dataset.empty().isFrozen());
    }
}
