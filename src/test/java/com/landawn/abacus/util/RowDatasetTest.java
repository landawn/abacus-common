package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.NoCachingNoUpdating.DisposableObjArray;
import com.landawn.abacus.util.Tuple.Tuple2;
import com.landawn.abacus.util.Tuple.Tuple3;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.function.BiFunction;
import com.landawn.abacus.util.function.Function;
import com.landawn.abacus.util.function.IntFunction;
import com.landawn.abacus.util.function.TriConsumer;
import com.landawn.abacus.util.function.TriFunction;
import com.landawn.abacus.util.stream.Stream;

public class RowDatasetTest extends RowDatasetTestSupport {
    @Test
    public void testEmptyFilterUsesDefaultMissingPropertyPolicy() {
        final Dataset empty = Dataset.empty().copy();
        empty.setProperties(Map.of("source", "empty"));
        final Dataset strict = empty.withMissingPropertyPolicy(Dataset.MissingPropertyPolicy.ERROR);
        final Dataset filtered = strict.filter(row -> {
            throw new AssertionError("An empty dataset must not invoke its filter");
        });

        assertEquals(Map.of("source", "empty"), filtered.getProperties());
        assertEquals(0, filtered.columnCount());
        filtered.addColumn("unknown", List.of(1));
        assertEquals(1, filtered.toList(OnlyIdBean20260906.class).size());

        final Dataset copied = strict.copy();
        copied.addColumn("unknown", List.of(1));
        assertThrows(IllegalArgumentException.class, () -> copied.toList(OnlyIdBean20260906.class));
    }

    @Test
    public void testConstructorWithMismatchedSizes() {
        final List<List<Object>> columns = createThreeRowScoreColumns();
        List<String> shortColumnNames = Arrays.asList("id", "name");
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            new RowDataset(shortColumnNames, columns);
        });
    }

    @Test
    public void testConstructorWithColumnNamesAndColumns() {
        final List<String> columnNames = Arrays.asList("id", "name", "age", "score");
        final List<List<Object>> columns = createThreeRowScoreColumns();
        RowDataset ds = new RowDataset(columnNames, columns);
        Assertions.assertNotNull(ds);
        Assertions.assertEquals(4, ds.columnCount());
        Assertions.assertEquals(3, ds.size());
    }

    @Test
    public void testConstructorWithNullColumnNames() {
        assertThrows(Exception.class, () -> {
            new RowDataset(null, columnList);
        });
    }

    @Test
    public void testConstructorWithDuplicateColumnNames() {
        List<String> dupNames = Arrays.asList("id", "name", "id");
        List<List<Object>> cols = Arrays.asList(Arrays.asList(1, 2), Arrays.asList("A", "B"), Arrays.asList(10, 20));
        assertThrows(IllegalArgumentException.class, () -> {
            new RowDataset(dupNames, cols);
        });
    }

    @Test
    public void testColumnNamesImmutable() {
        ImmutableList<String> names = dataset.columnNames();
        assertNotNull(names);
        assertEquals(4, names.size());
        assertEquals("id", names.get(0));
    }

    @Test
    public void testColumnCount() {
        final RowDataset dataset = createThreeRowScoreDataset();
        Assertions.assertEquals(4, dataset.columnCount());
    }

    @Test
    public void testContainsColumn() {
        assertTrue(dataset.containsColumn("id"));
        assertTrue(dataset.containsColumn("name"));
        assertFalse(dataset.containsColumn("nonexistent"));
        assertFalse(dataset.containsColumn("ID"));
    }

    @Test
    public void testContainsAllColumns() {
        assertTrue(dataset.containsAllColumns(Arrays.asList("id", "name")));
        assertTrue(dataset.containsAllColumns(Arrays.asList()));
        assertFalse(dataset.containsAllColumns(Arrays.asList("id", "nonexistent")));
    }

    @Test
    public void testRenameColumn() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        ds.renameColumn("id", "identifier");
        assertEquals("identifier", ds.getColumnName(0));
        assertTrue(ds.containsColumn("identifier"));
        assertFalse(ds.containsColumn("id"));
    }

    @Test
    public void testRenameColumnToSameName() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        ds.renameColumn("id", "id");
        assertEquals("id", ds.getColumnName(0));
    }

    @Test
    public void testRenameColumnWithSameName() {
        final RowDataset dataset = createThreeRowScoreDataset();
        dataset.renameColumn("id", "id");
        Assertions.assertTrue(dataset.containsColumn("id"));
    }

    @Test
    public void testRenameColumnToExistingName() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        assertThrows(IllegalArgumentException.class, () -> {
            ds.renameColumn("id", "name");
        });
    }

    @Test
    public void testRenameColumnNonExistent() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        assertThrows(IllegalArgumentException.class, () -> {
            ds.renameColumn("nonexistent", "newName");
        });
    }

    @Test
    public void testRenameColumnWithExistingName() {
        final RowDataset dataset = createThreeRowScoreDataset();
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            dataset.renameColumn("id", "name");
        });
    }

    @Test
    public void testRenameColumnWithInvalidName() {
        final RowDataset dataset = createThreeRowScoreDataset();
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            dataset.renameColumn("invalid", "new_name");
        });
    }

    @Test
    public void testRenameColumnRejectsNullOrEmptyNewName() {
        final RowDataset dataset = createThreeRowScoreDataset();

        assertThrows(IllegalArgumentException.class, () -> dataset.renameColumn("id", null));
        assertThrows(IllegalArgumentException.class, () -> dataset.renameColumn("id", ""));
        assertEquals(Arrays.asList("id", "name", "age", "score"), dataset.columnNames());
    }

    @Test
    public void testRenameColumnsWithMap() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        Map<String, String> renameMap = new LinkedHashMap<>();
        renameMap.put("id", "identifier");
        renameMap.put("name", "fullName");
        ds.renameColumns(renameMap);
        assertEquals("identifier", ds.getColumnName(0));
        assertEquals("fullName", ds.getColumnName(1));
    }

    @Test
    public void testRenameColumns() {
        final RowDataset dataset = createThreeRowScoreDataset();
        Map<String, String> oldNewNames = new HashMap<>();
        oldNewNames.put("id", "user_id");
        oldNewNames.put("name", "user_name");

        dataset.renameColumns(oldNewNames);
        Assertions.assertTrue(dataset.containsColumn("user_id"));
        Assertions.assertTrue(dataset.containsColumn("user_name"));
        Assertions.assertFalse(dataset.containsColumn("id"));
        Assertions.assertFalse(dataset.containsColumn("name"));
    }

    @Test
    public void testRenameColumnsWithEmptyMap() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        ds.renameColumns(new HashMap<>());
        assertEquals("id", ds.getColumnName(0));
    }

    @Test
    public void testRenameColumnsWithFunction() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        ds.renameColumns(Arrays.asList("id", "name"), name -> name.toUpperCase());
        assertEquals("ID", ds.getColumnName(0));
        assertEquals("NAME", ds.getColumnName(1));
        assertEquals("age", ds.getColumnName(2));
    }

    @Test
    public void testRenameColumnsFrozen() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        ds.freeze();
        assertThrows(IllegalStateException.class, () -> {
            ds.renameColumn("id", "identifier");
        });
    }

    @Test
    public void testRenameColumnsWithDuplicateNewNames() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        Map<String, String> renameMap = new HashMap<>();
        renameMap.put("id", "sameName");
        renameMap.put("name", "sameName");
        assertThrows(IllegalArgumentException.class, () -> {
            ds.renameColumns(renameMap);
        });
    }

    @Test
    public void testRenameColumnsValidatesAllNewNamesBeforeMutation() {
        final RowDataset dataset = createThreeRowScoreDataset();
        final Map<String, String> renameMap = new LinkedHashMap<>();
        renameMap.put("id", "identifier");
        renameMap.put("name", "");

        assertThrows(IllegalArgumentException.class, () -> dataset.renameColumns(renameMap));
        assertEquals(Arrays.asList("id", "name", "age", "score"), dataset.columnNames());
    }

    // ========== renameColumns - empty early return ==========

    @Test
    public void testRenameColumns_CollectionFunction_EmptySelection() {
        final RowDataset ds = new RowDataset(new ArrayList<>(Arrays.asList("id")), new ArrayList<>(Arrays.asList(new ArrayList<>(Arrays.asList(1, 2)))));

        // renameColumns(Collection, Function) is one of the strict column-selection methods, so an empty or
        // null selection is rejected rather than silently ignored - matching copy(), toList() and the set ops.
        assertThrows(IllegalArgumentException.class, () -> ds.renameColumns(new ArrayList<>(), s -> s + "_new"));
        assertThrows(IllegalArgumentException.class, () -> ds.renameColumns((Collection<String>) null, s -> s + "_new"));
        assertTrue(ds.containsColumn("id"));

        // ...but an empty selection is still the full (empty) column set on a Dataset that has no columns,
        // which is what keeps renameColumns(Function) working there.
        final RowDataset noColumns = new RowDataset(new ArrayList<>(), new ArrayList<>());
        assertDoesNotThrow(() -> noColumns.renameColumns(new ArrayList<>(), s -> s + "_new"));
        assertDoesNotThrow(() -> noColumns.renameColumns(s -> s + "_new"));
    }

    @Test
    public void testRenameColumns_Map_NewNameAlreadyExists_ThrowsIllegalArgument() {
        RowDataset ds = new RowDataset(new ArrayList<>(Arrays.asList("id", "name")),
                new ArrayList<>(Arrays.asList(new ArrayList<>(Arrays.asList(1)), new ArrayList<>(Arrays.asList("A")))));
        Map<String, String> renameMap = new LinkedHashMap<>();
        renameMap.put("id", "name"); // "name" already exists and it's a different column
        assertThrows(IllegalArgumentException.class, () -> ds.renameColumns(renameMap));
    }

    @Test
    public void testSwapColumnPositionWithSameColumn() {
        final RowDataset dataset = createThreeRowScoreDataset();
        dataset.swapColumns("id", "id");
        List<String> names = dataset.columnNames();
        Assertions.assertEquals("id", names.get(0));
    }

    @Test
    public void testSwapRowPositionWithSameRow() {
        final RowDataset dataset = createThreeRowScoreDataset();
        Object row0Name = dataset.get(0, 1);
        dataset.swapRows(0, 0);
        Assertions.assertEquals(row0Name, dataset.get(0, 1));
    }

    @Test
    public void testSetByRowAndColumn() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        ds.set(0, 1, "NewName");
        assertEquals("NewName", ds.get(0, 1));
    }

    @Test
    public void testSetByColumnIndex() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        ds.moveToRow(0);
        ds.set(1, "NewName");
        assertEquals("NewName", ds.get(0, 1));
    }

    @Test
    public void testSetByColumnName() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        ds.moveToRow(0);
        ds.set("name", "NewName");
        assertEquals("NewName", ds.get(0, 1));
    }

    @Test
    public void testSet() {
        final RowDataset dataset = createThreeRowScoreDataset();
        dataset.set(0, 0, 100);
        Assertions.assertEquals(100, (Integer) dataset.get(0, 0));

        dataset.set(1, 1, "Updated");
        Assertions.assertEquals("Updated", dataset.get(1, 1));
    }

    @Test
    public void testSetNull() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        ds.set(0, 1, null);
        assertNull(ds.get(0, 1));
        assertTrue(ds.isNull(0, 1));
    }

    @Test
    public void testSetFrozen() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        ds.freeze();
        assertThrows(IllegalStateException.class, () -> {
            ds.set(0, 1, "NewName");
        });
    }

    @Test
    public void testIsNullByRowAndColumn() {
        List<List<Object>> cols = copyColumnList();
        cols.get(1).set(0, null);
        RowDataset ds = new RowDataset(columnNames, cols);
        assertTrue(ds.isNull(0, 1));
        assertFalse(ds.isNull(1, 1));
    }

    @Test
    public void testIsNullByColumnIndex() {
        List<List<Object>> cols = copyColumnList();
        cols.get(1).set(0, null);
        RowDataset ds = new RowDataset(columnNames, cols);
        ds.moveToRow(0);
        assertTrue(ds.isNull(1));
        ds.moveToRow(1);
        assertFalse(ds.isNull(1));
    }

    @Test
    public void testIsNullByColumnName() {
        List<List<Object>> cols = copyColumnList();
        cols.get(1).set(0, null);
        RowDataset ds = new RowDataset(columnNames, cols);
        ds.moveToRow(0);
        assertTrue(ds.isNull("name"));
        ds.moveToRow(1);
        assertFalse(ds.isNull("name"));
    }

    @Test
    public void testIsNull() {
        final RowDataset dataset = createThreeRowScoreDataset();
        dataset.moveToRow(0);
        dataset.set(0, null);
        Assertions.assertTrue(dataset.isNull(0));
        Assertions.assertTrue(dataset.isNull("id"));
        Assertions.assertFalse(dataset.isNull(1));
        Assertions.assertFalse(dataset.isNull("name"));
    }

    @Test
    public void testIsNullWithRowAndColumn() {
        final RowDataset dataset = createThreeRowScoreDataset();
        dataset.set(0, 0, null);
        Assertions.assertTrue(dataset.isNull(0, 0));
        Assertions.assertFalse(dataset.isNull(0, 1));
    }

    @Test
    public void testCopyColumn() {
        List<Object> names = dataset.copyColumn("name");
        assertNotNull(names);
        assertEquals(5, names.size());
        names.set(0, "Modified");
        assertEquals("Modified", names.get(0));
        assertEquals("Alice", dataset.get(0, 1));
    }

    @Test
    public void testConvertColumn() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        ds.convertColumn("id", String.class);
        assertEquals("1", ds.get(0, 0));
    }

    @Test
    public void testConvertColumns() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        Map<String, Class<?>> typeMap = new HashMap<>();
        typeMap.put("id", String.class);
        typeMap.put("age", String.class);
        ds.convertColumns(typeMap);
        assertEquals("1", ds.get(0, 0));
        assertEquals("25", ds.get(0, 2));
    }

    // ========== convertColumns / updateColumns - empty early return ==========

    @Test
    public void testConvertColumns_EmptyMap_ReturnsEarly() {
        RowDataset ds = new RowDataset(new ArrayList<>(Arrays.asList("id")), new ArrayList<>(Arrays.asList(new ArrayList<>(Arrays.asList(1, 2)))));
        assertDoesNotThrow(() -> ds.convertColumns(new HashMap<>()));
        assertEquals(1, (int) ds.get(0, 0));
    }

    @Test
    public void testUpdateColumn() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        ds.updateColumn("age", (Integer age) -> age + 1);
        assertEquals(26, (Integer) ds.get(0, 2));
        assertEquals(31, (Integer) ds.get(1, 2));
    }

    @Test
    public void testUpdateColumnWithNull() {
        List<List<Object>> cols = copyColumnList();
        cols.get(2).set(0, null);
        RowDataset ds = new RowDataset(columnNames, cols);
        ds.updateColumn("age", (Integer age) -> age == null ? 0 : age + 1);
        assertEquals(0, (Integer) ds.get(0, 2));
        assertEquals(31, (Integer) ds.get(1, 2));
    }

    @Test
    public void testUpdateColumns() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        ds.updateColumns(Arrays.asList("age", "salary"), (i, c, v) -> {
            if (c.equals("age")) {
                return ((Integer) v) + 1;
            } else {
                return ((Double) v) * 1.1;
            }
        });
        assertEquals(26, (Integer) ds.get(0, 2));
        assertEquals(55000.0, (Double) ds.get(0, 3), 1.0);
    }

    @Test
    public void testUpdateColumns_EmptyCollection_ReturnsEarly() {
        RowDataset ds = new RowDataset(new ArrayList<>(Arrays.asList("id")), new ArrayList<>(Arrays.asList(new ArrayList<>(Arrays.asList(1, 2)))));
        assertDoesNotThrow(() -> ds.updateColumns(new ArrayList<>(), (i, col, val) -> val));
        assertEquals(1, (int) ds.get(0, 0));
    }

    @Test
    public void testCombineColumnsWithFunction() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        ds.combineColumns(Arrays.asList("name", "age"), "info", row -> row.get(0) + " is " + row.get(1));
        assertTrue(ds.containsColumn("info"));
        assertEquals("Alice is 25", ds.get(0, ds.getColumnIndex("info")));
    }

    @Test
    public void testCombineColumnsWithBiFunction() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        ds.combineColumns(Tuple.of("name", "age"), "combined", (String name, Integer age) -> name + ":" + age);
        assertTrue(ds.containsColumn("combined"));
        assertFalse(ds.containsColumn("name"));
    }

    @Test
    public void testCombineColumnsWithClass() {
        final RowDataset dataset = createThreeRowScoreDataset();
        dataset.combineColumns(Arrays.asList("name", "age"), "nameAge", a -> a.join(""));
        Assertions.assertEquals(3, dataset.columnCount());
        Assertions.assertTrue(dataset.containsColumn("nameAge"));
        Assertions.assertFalse(dataset.containsColumn("name"));
        Assertions.assertFalse(dataset.containsColumn("age"));
    }

    @Test
    public void testCombineColumnsWithTuple2() {
        final RowDataset dataset = createThreeRowScoreDataset();
        dataset.combineColumns(new Tuple2<>("name", "age"), "combined", (BiFunction<String, Integer, String>) (name, age) -> name + "_" + age);
        Assertions.assertEquals(3, dataset.columnCount());
        Assertions.assertEquals("John_25", dataset.get(0, dataset.getColumnIndex("combined")));
    }

    @Test
    public void testCombineColumnsWithTuple3() {
        final RowDataset dataset = createThreeRowScoreDataset();
        dataset.combineColumns(new Tuple3<>("id", "name", "age"), "combined",
                (TriFunction<Integer, String, Integer, String>) (id, name, age) -> id + ":" + name + ":" + age);
        Assertions.assertEquals(2, dataset.columnCount());
        Assertions.assertEquals("1:John:25", dataset.get(0, dataset.getColumnIndex("combined")));
    }

    @Test
    public void testDivideColumnWithFunction() {
        List<String> names = CommonUtil.toList("fullName");
        List<List<Object>> cols = new ArrayList<>();
        cols.add(new ArrayList<>(CommonUtil.toList("John Doe", "Jane Smith")));
        RowDataset ds = new RowDataset(names, cols);
        ds.divideColumn("fullName", CommonUtil.toList("firstName", "lastName"), (String fullName) -> CommonUtil.toList(fullName.split(" ")));
        assertTrue(ds.containsColumn("firstName"));
        assertTrue(ds.containsColumn("lastName"));
        assertFalse(ds.containsColumn("fullName"));
        assertEquals("John", ds.get(0, 0));
        assertEquals("Doe", ds.get(0, 1));
    }

    @Test
    public void testDivideColumnWithBiConsumer() {
        List<String> names = CommonUtil.toList("fullName");
        List<List<Object>> cols = new ArrayList<>();
        cols.add(new ArrayList<>(CommonUtil.toList("John Doe", "Jane Smith")));
        RowDataset ds = new RowDataset(names, cols);
        ds.divideColumn("fullName", Arrays.asList("firstName", "lastName"), (String fullName, Object[] output) -> {
            String[] parts = fullName.split(" ");
            output[0] = parts[0];
            output[1] = parts[1];
        });
        assertEquals("John", ds.get(0, 0));
        assertEquals("Doe", ds.get(0, 1));
    }

    @Test
    public void testDivideColumn_clearsReusableOutputBufferBetweenRows() {
        // Pre-fix: a shared Object[] was reused without clearing, so slots not written on a later
        // row leaked values from the previous row.
        List<String> names = CommonUtil.toList("raw");
        List<List<Object>> cols = new ArrayList<>();
        cols.add(new ArrayList<>(CommonUtil.toList("a-b", "x")));
        RowDataset ds = new RowDataset(names, cols);
        ds.divideColumn("raw", Arrays.asList("left", "right"), (String val, Object[] output) -> {
            if (val.contains("-")) {
                String[] parts = val.split("-", 2);
                output[0] = parts[0];
                output[1] = parts[1];
            } else {
                output[0] = val; // deliberately leave output[1] unset
            }
        });
        assertEquals("a", ds.get(0, 0));
        assertEquals("b", ds.get(0, 1));
        assertEquals("x", ds.get(1, 0));
        assertNull(ds.get(1, 1), "unwritten output slot must not retain previous row value");
    }

    @Test
    public void testDivideColumnWithTuple2() {
        final RowDataset dataset = createThreeRowScoreDataset();
        dataset.addColumn("pair", Arrays.asList("X|Y", "A|B", "M|N"));
        dataset.divideColumn("pair", new Tuple2<>("first", "second"), (BiConsumer<String, Pair<Object, Object>>) (val, output) -> {
            String[] parts = val.split("\\|");
            output.setLeft(parts[0]);
            output.setRight(parts[1]);
        });

        Assertions.assertEquals("X", dataset.get(0, dataset.getColumnIndex("first")));
        Assertions.assertEquals("Y", dataset.get(0, dataset.getColumnIndex("second")));
    }

    @Test
    public void testDivideColumnWithTuple3() {
        final RowDataset dataset = createThreeRowScoreDataset();
        dataset.addColumn("triple", Arrays.asList("A-B-C", "X-Y-Z", "1-2-3"));
        dataset.divideColumn("triple", new Tuple3<>("p1", "p2", "p3"), (BiConsumer<String, Triple<Object, Object, Object>>) (val, output) -> {
            String[] parts = val.split("-");
            output.setLeft(parts[0]);
            output.setMiddle(parts[1]);
            output.setRight(parts[2]);
        });

        Assertions.assertEquals("A", dataset.get(0, dataset.getColumnIndex("p1")));
        Assertions.assertEquals("B", dataset.get(0, dataset.getColumnIndex("p2")));
        Assertions.assertEquals("C", dataset.get(0, dataset.getColumnIndex("p3")));
    }

    @Test
    public void testDivideColumnWithTuple2BiConsumer() {
        RowDataset ds = new RowDataset(new ArrayList<>(Arrays.asList("full", "extra")),
                new ArrayList<>(Arrays.asList(new ArrayList<>(Arrays.asList("John-Doe", "Jane-Smith")), new ArrayList<>(Arrays.asList(1, 2)))));

        ds.divideColumn("full", Tuple.of("first", "last"), (BiConsumer<Object, com.landawn.abacus.util.Pair<Object, Object>>) (val, output) -> {
            String[] parts = ((String) val).split("-");
            output.set(parts[0], parts[1]);
        });

        assertTrue(ds.columnNames().contains("first"));
        assertTrue(ds.columnNames().contains("last"));
        assertEquals("John", ds.get(0, ds.getColumnIndex("first")));
        assertEquals("Doe", ds.get(0, ds.getColumnIndex("last")));
    }

    @Test
    public void testDivideColumnWithTuple3BiConsumer() {
        RowDataset ds = new RowDataset(new ArrayList<>(Arrays.asList("date", "extra")),
                new ArrayList<>(Arrays.asList(new ArrayList<>(Arrays.asList("2024-01-15", "2024-06-20")), new ArrayList<>(Arrays.asList(1, 2)))));

        ds.divideColumn("date", Tuple.of("year", "month", "day"), (BiConsumer<Object, Triple<Object, Object, Object>>) (val, output) -> {
            String[] parts = ((String) val).split("-");
            output.set(parts[0], parts[1], parts[2]);
        });

        assertTrue(ds.columnNames().contains("year"));
        assertTrue(ds.columnNames().contains("month"));
        assertTrue(ds.columnNames().contains("day"));
    }

    @Test
    public void testDivideColumn_WithFunction() {
        RowDataset ds = new RowDataset(new ArrayList<>(Arrays.asList("fullname", "age")),
                new ArrayList<>(Arrays.asList(new ArrayList<>(Arrays.asList("John Doe", "Jane Smith")), new ArrayList<>(Arrays.asList(30, 25)))));

        ds.divideColumn("fullname", Arrays.asList("first", "last"), (Function<Object, ? extends List<?>>) val -> Arrays.asList(((String) val).split(" ")));

        assertTrue(ds.containsColumn("first"));
        assertTrue(ds.containsColumn("last"));
        assertFalse(ds.containsColumn("fullname"));
        assertEquals("John", ds.get(0, ds.getColumnIndex("first")));
        assertEquals("Doe", ds.get(0, ds.getColumnIndex("last")));
    }

    // ===== divideColumn with BiConsumer =====

    @Test
    public void testDivideColumn_WithBiConsumer_Advanced() {
        RowDataset ds = new RowDataset(new ArrayList<>(Arrays.asList("coords", "label")),
                new ArrayList<>(Arrays.asList(new ArrayList<>(Arrays.asList("1,2", "3,4")), new ArrayList<>(Arrays.asList("A", "B")))));

        ds.divideColumn("coords", Arrays.asList("x", "y"), (BiConsumer<Object, Object[]>) (val, output) -> {
            String[] parts = ((String) val).split(",");
            output[0] = Integer.parseInt(parts[0]);
            output[1] = Integer.parseInt(parts[1]);
        });

        assertTrue(ds.containsColumn("x"));
        assertTrue(ds.containsColumn("y"));
        assertFalse(ds.containsColumn("coords"));
        assertEquals(1, (Integer) ds.get(0, ds.getColumnIndex("x")));
        assertEquals(2, (Integer) ds.get(0, ds.getColumnIndex("y")));
    }

    // ========== divideColumn - error paths ==========

    @Test
    public void testDivideColumn_WithFunction_EmptyNewColumnNames_ThrowsIllegalArgument() {
        RowDataset ds = new RowDataset(new ArrayList<>(Arrays.asList("id", "val")),
                new ArrayList<>(Arrays.asList(new ArrayList<>(Arrays.asList(1)), new ArrayList<>(Arrays.asList("a,b")))));
        assertThrows(IllegalArgumentException.class,
                () -> ds.divideColumn("val", new ArrayList<>(), (Function<Object, ? extends List<?>>) v -> Arrays.asList(((String) v).split(","))));
    }

    @Test
    public void testDivideColumn_WithFunction_OverlappingNames_ThrowsIllegalArgument() {
        RowDataset ds = new RowDataset(new ArrayList<>(Arrays.asList("id", "val")),
                new ArrayList<>(Arrays.asList(new ArrayList<>(Arrays.asList(1)), new ArrayList<>(Arrays.asList("a,b")))));
        // "id" already exists in dataset
        assertThrows(IllegalArgumentException.class,
                () -> ds.divideColumn("val", Arrays.asList("id", "x"), (Function<Object, ? extends List<?>>) v -> Arrays.asList("a", "b")));
    }

    @Test
    public void testDivideColumn_WithFunction_WrongCountFromFunc_ThrowsIllegalArgument() {
        RowDataset ds = new RowDataset(new ArrayList<>(Arrays.asList("id", "val")),
                new ArrayList<>(Arrays.asList(new ArrayList<>(Arrays.asList(1)), new ArrayList<>(Arrays.asList("a,b")))));
        // func returns 1 element but expects 2 new columns
        assertThrows(IllegalArgumentException.class,
                () -> ds.divideColumn("val", Arrays.asList("x", "y"), (Function<Object, ? extends List<?>>) v -> Arrays.asList("onlyone")));
    }

    @Test
    public void testDivideColumn_WithBiConsumer_EmptyNewColumnNames_ThrowsIllegalArgument() {
        RowDataset ds = new RowDataset(new ArrayList<>(Arrays.asList("id", "val")),
                new ArrayList<>(Arrays.asList(new ArrayList<>(Arrays.asList(1)), new ArrayList<>(Arrays.asList("abc")))));
        assertThrows(IllegalArgumentException.class, () -> ds.divideColumn("val", new ArrayList<>(), (BiConsumer<Object, Object[]>) (v, arr) -> {
        }));
    }

    @Test
    public void testDivideColumn_WithBiConsumer_OverlappingNames_ThrowsIllegalArgument() {
        RowDataset ds = new RowDataset(new ArrayList<>(Arrays.asList("id", "val")),
                new ArrayList<>(Arrays.asList(new ArrayList<>(Arrays.asList(1)), new ArrayList<>(Arrays.asList("abc")))));
        assertThrows(IllegalArgumentException.class, () -> ds.divideColumn("val", Arrays.asList("id", "x"), (BiConsumer<Object, Object[]>) (v, arr) -> {
            arr[0] = "a";
            arr[1] = "b";
        }));
    }

    @Test
    public void testColumns() {
        Stream<ImmutableList<Object>> columnStream = dataset.columns();
        assertNotNull(columnStream);
        assertEquals(4, columnStream.count());
    }

    @Test
    public void testColumnMap() {
        Map<String, ImmutableList<Object>> colMap = dataset.columnMap();
        assertNotNull(colMap);
        assertEquals(4, colMap.size());
        assertTrue(colMap.containsKey("id"));
        assertEquals(5, colMap.get("id").size());
    }

    @Test
    public void testUpdateRow() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        ds.updateRow(0, (Object val) -> {
            if (val instanceof String) {
                return ((String) val).toUpperCase();
            }
            return val;
        });
        assertEquals("ALICE", ds.get(0, 1));
    }

    @Test
    public void testUpdateRows() {
        final RowDataset dataset = createThreeRowScoreDataset();
        dataset.updateRows(new int[] { 0, 2 }, (i, c, v) -> v instanceof Integer ? (Integer) v * 2 : v);
        Assertions.assertEquals(2, (Integer) dataset.get(0, 0));
        Assertions.assertEquals(50, (Integer) dataset.get(0, 2));
        Assertions.assertEquals(2, (Integer) dataset.get(1, 0));
        Assertions.assertEquals(6, (Integer) dataset.get(2, 0));
        Assertions.assertEquals(70, (Integer) dataset.get(2, 2));
    }

    @Test
    public void testUpdateRows_2() {
        RowDataset ds = new RowDataset(new ArrayList<>(Arrays.asList("a", "b")),
                new ArrayList<>(Arrays.asList(new ArrayList<>(Arrays.asList(1, 2, 3)), new ArrayList<>(Arrays.asList(4, 5, 6)))));

        ds.updateRows(new int[] { 0, 2 }, (com.landawn.abacus.util.function.IntBiObjFunction<String, Object, Object>) (rowIndex, columnName, value) -> {
            if (value instanceof Integer) {
                return ((Integer) value) * 100;
            }
            return value;
        });

        assertEquals(100, (int) ds.get(0, 0));
        assertEquals(2, (int) ds.get(1, 0)); // row 1 untouched
        assertEquals(300, (int) ds.get(2, 0));
    }

    @Test
    public void testUpdateAll() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        ds.updateAll((Object val) -> {
            if (val instanceof Integer) {
                return ((Integer) val) + 1;
            }
            return val;
        });
        assertEquals(2, (Integer) ds.get(0, 0));
        assertEquals(26, (Integer) ds.get(0, 2));
    }

    @Test
    public void testUpdateAllWithIntBiObjFunction() {
        RowDataset ds = new RowDataset(new ArrayList<>(Arrays.asList("a", "b")),
                new ArrayList<>(Arrays.asList(new ArrayList<>(Arrays.asList(1, 2)), new ArrayList<>(Arrays.asList(3, 4)))));

        ds.updateAll((com.landawn.abacus.util.function.IntBiObjFunction<String, Object, Object>) (rowIndex, columnName, value) -> {
            if (value instanceof Integer) {
                return ((Integer) value) * 10;
            }
            return value;
        });

        assertEquals(10, (int) ds.get(0, 0));
        assertEquals(20, (int) ds.get(1, 0));
        assertEquals(30, (int) ds.get(0, 1));
        assertEquals(40, (int) ds.get(1, 1));
    }

    @Test
    public void testReplaceIf() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        ds.replaceIf(val -> val instanceof Integer && (Integer) val > 30, 999);
        assertEquals(25, (Integer) ds.get(0, 2));
        assertEquals(999, (Integer) ds.get(2, 2));
    }

    @Test
    public void testReplaceIfWithIntBiObjPredicate() {
        RowDataset ds = new RowDataset(new ArrayList<>(Arrays.asList("a", "b")),
                new ArrayList<>(Arrays.asList(new ArrayList<>(Arrays.asList(1, 2)), new ArrayList<>(Arrays.asList(3, 4)))));

        ds.replaceIf((com.landawn.abacus.util.function.IntBiObjPredicate<String, Object>) (rowIndex, columnName, value) -> {
            return value instanceof Integer && ((Integer) value) > 2;
        }, 99);

        assertEquals(1, (int) ds.get(0, 0));
        assertEquals(2, (int) ds.get(1, 0));
        assertEquals(99, (int) ds.get(0, 1));
        assertEquals(99, (int) ds.get(1, 1));
    }

    @Test
    public void testPrepend() {
        RowDataset ds1 = new RowDataset(columnNames, copyColumnList());
        RowDataset ds2 = new RowDataset(columnNames, copyColumnList());
        Object firstValue = ds1.get(0, 0);
        ds1.prepend(ds2);
        assertNotNull(ds1.get(ds2.size(), 0));
    }

    @Test
    public void testAppend() {
        RowDataset ds1 = new RowDataset(columnNames, copyColumnList());
        RowDataset ds2 = new RowDataset(columnNames, copyColumnList());
        int originalSize = ds1.size();
        ds1.append(ds2);
        assertEquals(originalSize * 2, ds1.size());
    }

    @Test
    public void testMerge() {
        RowDataset ds1 = new RowDataset(columnNames, copyColumnList());
        RowDataset ds2 = new RowDataset(columnNames, copyColumnList());
        int originalSize = ds1.size();
        ds1.merge(ds2);
        assertEquals(originalSize * 2, ds1.size());
    }

    @Test
    public void testMergeWithDifferentColumns() {
        RowDataset ds1 = new RowDataset(columnNames, copyColumnList());
        List<String> otherNames = Arrays.asList("id", "name", "department");
        List<List<Object>> otherCols = new ArrayList<>();
        otherCols.add(new ArrayList<>(Arrays.asList(6, 7)));
        otherCols.add(new ArrayList<>(Arrays.asList("Frank", "Grace")));
        otherCols.add(new ArrayList<>(Arrays.asList("IT", "HR")));
        RowDataset ds2 = new RowDataset(otherNames, otherCols);

        ds1.merge(ds2, Arrays.asList("id", "name"));
        assertEquals(7, ds1.size());
        assertTrue(ds1.containsColumn("id"));
        assertTrue(ds1.containsColumn("name"));
    }

    @Test
    public void testMergeWithRange() {
        RowDataset target = new RowDataset(new ArrayList<>(Arrays.asList("id", "name")),
                new ArrayList<>(Arrays.asList(new ArrayList<>(Arrays.asList(1, 2)), new ArrayList<>(Arrays.asList("A", "B")))));
        RowDataset source = new RowDataset(new ArrayList<>(Arrays.asList("id", "name")),
                new ArrayList<>(Arrays.asList(new ArrayList<>(Arrays.asList(3, 4, 5)), new ArrayList<>(Arrays.asList("C", "D", "E")))));

        target.merge(source, 1, 3, Arrays.asList("id", "name"));
        assertEquals(4, target.size());
        assertEquals(4, (int) target.get(2, 0));
        assertEquals("E", target.get(3, 1));
    }

    @Test
    public void testMerge_MergesPropertiesFromOtherDataset() {
        Map<String, Object> leftProps = new LinkedHashMap<>();
        leftProps.put("source", "left");
        Map<String, Object> rightProps = new LinkedHashMap<>();
        rightProps.put("source", "right");
        rightProps.put("version", 2);

        RowDataset left = new RowDataset(new ArrayList<>(Arrays.asList("id")), new ArrayList<>(Arrays.asList(new ArrayList<>(Arrays.asList(1)))), leftProps);
        RowDataset right = new RowDataset(new ArrayList<>(Arrays.asList("id")), new ArrayList<>(Arrays.asList(new ArrayList<>(Arrays.asList(2)))), rightProps);

        left.append(right);

        assertEquals(2, left.size());
        assertEquals("right", left.getProperties().get("source"));
        assertEquals(2, left.getProperties().get("version"));
    }

    @Test
    public void testMergeWithColumnNames() {
        Collection<String> columnNames = CommonUtil.toList("id", "city");
        Dataset result = ds1.copy();
        result.merge(ds2, columnNames);

        assertNotNull(result);
        assertEquals(ds1.size() + ds2.size(), result.size());
    }

    @Test
    public void testMergeMultipleDatasets() {
        Collection<Dataset> ds = CommonUtil.toList(ds1, ds2, emptyDs);
        Dataset result = CommonUtil.merge(ds);

        assertNotNull(result);
        assertEquals(ds1.size() + ds2.size() + emptyDs.size(), result.size());
    }

    @Test
    public void testMergeWithRangeAndNewColumns() {
        RowDataset target = new RowDataset(new ArrayList<>(Arrays.asList("id", "name")),
                new ArrayList<>(Arrays.asList(new ArrayList<>(Arrays.asList(1, 2)), new ArrayList<>(Arrays.asList("A", "B")))));
        RowDataset source = new RowDataset(new ArrayList<>(Arrays.asList("id", "city")),
                new ArrayList<>(Arrays.asList(new ArrayList<>(Arrays.asList(3, 4, 5)), new ArrayList<>(Arrays.asList("NYC", "LA", "SF")))));

        target.merge(source, 0, 2, Arrays.asList("id", "city"));
        assertEquals(4, target.size());
        assertTrue(target.containsColumn("city"));
        assertEquals("NYC", target.get(2, target.getColumnIndex("city")));
        assertNull(target.get(0, target.getColumnIndex("city")));
    }

    @Test
    public void testMergeWithSameColumnsRequired() {
        assertThrows(IllegalArgumentException.class, () -> {
            ds1.merge(ds2, true);
        });
    }

    @Test
    public void testPrependAppendMergeWithNullOtherThrowsIllegalArgument() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());

        assertThrows(IllegalArgumentException.class, () -> ds.prepend(null));
        assertThrows(IllegalArgumentException.class, () -> ds.append(null));
        assertThrows(IllegalArgumentException.class, () -> ds.merge((Dataset) null));
        assertThrows(IllegalArgumentException.class, () -> ds.merge(null, false));
        assertThrows(IllegalArgumentException.class, () -> ds.merge(null, Arrays.asList("id")));
        assertThrows(IllegalArgumentException.class, () -> ds.merge(null, 0, 0, Arrays.asList("id")));
    }

    @Test
    public void testMergeWithNullOrEmptySelectColumnNamesThrowsIllegalArgument() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        RowDataset other = new RowDataset(new ArrayList<>(Arrays.asList("id")), new ArrayList<>(Arrays.asList(new ArrayList<>(Arrays.asList(9)))));

        assertThrows(IllegalArgumentException.class, () -> ds.merge(other, (Collection<String>) null));
        assertThrows(IllegalArgumentException.class, () -> ds.merge(other, new ArrayList<>()));
        assertThrows(IllegalArgumentException.class, () -> ds.merge(other, 0, 1, null));
        assertThrows(IllegalArgumentException.class, () -> ds.merge(other, 0, 1, new ArrayList<>()));
    }

    @Test
    public void testMergeWithSelectColumnNamesNotInOtherThrowsIllegalArgument() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        RowDataset other = new RowDataset(new ArrayList<>(Arrays.asList("id", "city")),
                new ArrayList<>(Arrays.asList(new ArrayList<>(Arrays.asList(9)), new ArrayList<>(Arrays.asList("NYC")))));
        int sizeBefore = ds.size();
        int columnCountBefore = ds.columnCount();

        // "name" exists in this dataset but not in `other`: must throw instead of silently appending null-filled data.
        assertThrows(IllegalArgumentException.class, () -> ds.merge(other, Arrays.asList("id", "name")));
        // "country" exists in neither Dataset.
        assertThrows(IllegalArgumentException.class, () -> ds.merge(other, 0, 1, Arrays.asList("country")));

        // The failed merges must not have modified this dataset.
        assertEquals(sizeBefore, ds.size());
        assertEquals(columnCountBefore, ds.columnCount());
    }

    @Test
    public void testMergeWithZeroColumnOtherIsStillNoOp() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        RowDataset zeroColumns = new RowDataset(new ArrayList<>(), new ArrayList<>());
        int sizeBefore = ds.size();
        int columnCountBefore = ds.columnCount();

        ds.merge(zeroColumns);

        assertEquals(sizeBefore, ds.size());
        assertEquals(columnCountBefore, ds.columnCount());
    }

    // ==================== Missing test methods below ====================

    @Test
    public void testCurrentRowIndex() {
        assertEquals(0, dataset.currentRowIndex());
    }

    @Test
    public void testAbsolute() {
        Dataset ds = dataset.moveToRow(3);
        assertNotNull(ds);
        assertEquals(3, ds.currentRowIndex());
        assertEquals("Diana", ds.get("name"));
    }

    @Test
    public void testAbsoluteInvalid() {
        assertThrows(IndexOutOfBoundsException.class, () -> {
            dataset.moveToRow(10);
        });
        assertThrows(IndexOutOfBoundsException.class, () -> {
            dataset.moveToRow(-1);
        });
    }

    @Test
    public void testFirstRow() {
        Optional<Object[]> firstRow = dataset.firstRow();
        assertTrue(firstRow.isPresent());
        assertEquals(1, firstRow.get()[0]);
    }

    @Test
    public void testFirstRowAsClass() {
        final RowDataset dataset = createThreeRowScoreDataset();
        Optional<TestBean> firstRow = dataset.firstRow(TestBean.class);
        Assertions.assertTrue(firstRow.isPresent());
        Assertions.assertEquals("John", firstRow.get().name);
    }

    @Test
    public void testFirstRowWithColumns() {
        final RowDataset dataset = createThreeRowScoreDataset();
        Optional<TestBean> firstRow = dataset.firstRow(Arrays.asList("name", "age"), TestBean.class);
        Assertions.assertTrue(firstRow.isPresent());
        Assertions.assertEquals("John", firstRow.get().name);
        Assertions.assertEquals(25, firstRow.get().age);
    }

    @Test
    public void testFirstRowWithSupplier() {
        final RowDataset dataset = createThreeRowScoreDataset();
        Optional<List<Object>> firstRow = dataset.firstRow((IntFunction<List<Object>>) ArrayList::new);
        Assertions.assertTrue(firstRow.isPresent());
        Assertions.assertEquals(4, firstRow.get().size());
    }

    @Test
    public void testFirstRowWithColumnsAndSupplier() {
        final RowDataset ds = createThreeRowScoreDataset();
        Optional<Map<String, Object>> row = ds.firstRow(Arrays.asList("name", "age"), (IntFunction<Map<String, Object>>) size -> new HashMap<>());
        assertTrue(row.isPresent());
        assertEquals("John", row.get().get("name"));
    }

    @Test
    public void testFirstRowEmpty() {
        Optional<Object[]> firstRow = emptyDataset.firstRow();
        assertFalse(firstRow.isPresent());
    }

    @Test
    public void testLastRow() {
        Optional<Object[]> lastRow = dataset.lastRow();
        assertTrue(lastRow.isPresent());
        assertEquals(5, lastRow.get()[0]);
    }

    @Test
    public void testLastRowAsClass() {
        final RowDataset dataset = createThreeRowScoreDataset();
        Optional<TestBean> lastRow = dataset.lastRow(TestBean.class);
        Assertions.assertTrue(lastRow.isPresent());
        Assertions.assertEquals("Bob", lastRow.get().name);
    }

    @Test
    public void testLastRowWithColumns() {
        final RowDataset dataset = createThreeRowScoreDataset();
        Optional<TestBean> lastRow = dataset.lastRow(Arrays.asList("name", "age"), TestBean.class);
        Assertions.assertTrue(lastRow.isPresent());
        Assertions.assertEquals("Bob", lastRow.get().name);
        Assertions.assertEquals(35, lastRow.get().age);
    }

    @Test
    public void testLastRowWithSupplier() {
        final RowDataset dataset = createThreeRowScoreDataset();
        Optional<List<Object>> lastRow = dataset.lastRow((IntFunction<List<Object>>) ArrayList::new);
        Assertions.assertTrue(lastRow.isPresent());
        Assertions.assertEquals(4, lastRow.get().size());
    }

    @Test
    public void testLastRowWithColumnsAndSupplier() {
        final RowDataset ds = createThreeRowScoreDataset();
        Optional<Map<String, Object>> row = ds.lastRow(Arrays.asList("name", "age"), (IntFunction<Map<String, Object>>) size -> new HashMap<>());
        assertTrue(row.isPresent());
        assertEquals("Bob", row.get().get("name"));
    }

    @Test
    public void testLastRowEmpty() {
        Optional<Object[]> lastRow = emptyDataset.lastRow();
        assertFalse(lastRow.isPresent());
    }

    @Test
    public void testBiIterator() {
        final RowDataset dataset = createThreeRowScoreDataset();
        BiIterator<String, Integer> iter = dataset.iterator("name", "age");
        Assertions.assertTrue(iter.hasNext());

        Pair<String, Integer> pair = iter.next();
        Assertions.assertEquals("John", pair.left());
        Assertions.assertEquals(25, pair.right());
    }

    @Test
    public void testBiIteratorWithRange() {
        final RowDataset dataset = createThreeRowScoreDataset();
        BiIterator<String, Integer> iter = dataset.iterator(1, 3, "name", "age");
        Assertions.assertTrue(iter.hasNext());

        Pair<String, Integer> pair = iter.next();
        Assertions.assertEquals("Jane", pair.left());
        Assertions.assertEquals(30, pair.right());
    }

    @Test
    public void testTriIteratorWithRange() {
        final RowDataset dataset = createThreeRowScoreDataset();
        TriIterator<Integer, String, Integer> iter = dataset.iterator(0, 2, "id", "name", "age");

        int count = 0;
        while (iter.hasNext()) {
            iter.next();
            count++;
        }
        Assertions.assertEquals(2, count);
    }

    @Test
    public void testBiIteratorNoRange() {
        final RowDataset ds = createThreeRowScoreDataset();
        BiIterator<Integer, String> iter = ds.iterator("id", "name");
        List<String> results = new ArrayList<>();
        iter.forEachRemaining((id, name) -> results.add(id + ":" + name));
        assertEquals(3, results.size());
        assertEquals("1:John", results.get(0));
    }

    @Test
    public void testTriIteratorNoRange() {
        final RowDataset ds = createThreeRowScoreDataset();
        TriIterator<Integer, String, Integer> iter = ds.iterator("id", "name", "age");
        List<String> results = new ArrayList<>();
        iter.forEachRemaining((id, name, age) -> results.add(id + ":" + name + ":" + age));
        assertEquals(3, results.size());
        assertEquals("1:John:25", results.get(0));
    }

    @Test
    public void testIterator() {
        BiIterator<Integer, String> iter = dataset.iterator("id", "name");
        assertNotNull(iter);
        assertTrue(iter.hasNext());
        Pair<Integer, String> first = iter.next();
        assertEquals(1, first.left().intValue());
        assertEquals("Alice", first.right());
    }

    @Test
    public void testTriIterator() {
        TriIterator<Integer, String, Integer> iter = dataset.iterator("id", "name", "age");
        assertNotNull(iter);
        assertTrue(iter.hasNext());
        Triple<Integer, String, Integer> first = iter.next();
        assertEquals(1, first.left().intValue());
        assertEquals("Alice", first.middle());
        assertEquals(25, first.right().intValue());
    }

    @Test
    public void testForEachWithColumns() {
        final RowDataset dataset = createThreeRowScoreDataset();
        List<String> results = new ArrayList<>();
        dataset.forEach(Arrays.asList("name", "age"), (DisposableObjArray arr) -> {
            results.add(arr.get(0) + "-" + arr.get(1));
        });

        Assertions.assertEquals(3, results.size());
        Assertions.assertEquals("John-25", results.get(0));
    }

    @Test
    public void testForEachWithRange() {
        final RowDataset dataset = createThreeRowScoreDataset();
        List<String> results = new ArrayList<>();
        dataset.forEach(1, 3, (DisposableObjArray arr) -> {
            results.add(arr.get(1).toString());
        });

        Assertions.assertEquals(2, results.size());
        Assertions.assertEquals("Jane", results.get(0));
        Assertions.assertEquals("Bob", results.get(1));
    }

    @Test
    public void testForEachWithTuple2() {
        final RowDataset dataset = createThreeRowScoreDataset();
        List<String> results = new ArrayList<>();
        dataset.forEach(Tuple.of("name", "age"), (name, age) -> results.add(name + " is " + age));

        Assertions.assertEquals(3, results.size());
        Assertions.assertEquals("John is 25", results.get(0));
    }

    @Test
    public void testForEachWithTuple3() {
        final RowDataset dataset = createThreeRowScoreDataset();
        List<String> results = new ArrayList<>();
        dataset.forEach(new Tuple3<>("id", "name", "age"), (TriConsumer<Integer, String, Integer>) (id, name, age) -> results.add(id + ":" + name + ":" + age));

        Assertions.assertEquals(3, results.size());
        Assertions.assertEquals("1:John:25", results.get(0));
    }

    @Test
    public void testForEachWithRangeAndColumns() {
        final RowDataset ds = createThreeRowScoreDataset();
        List<String> results = new ArrayList<>();
        ds.forEach(0, 2, Arrays.asList("name", "age"), (DisposableObjArray arr) -> {
            results.add(arr.get(0) + "-" + arr.get(1));
        });

        assertEquals(2, results.size());
        assertEquals("John-25", results.get(0));
        assertEquals("Jane-30", results.get(1));
    }

    @Test
    public void testForEachReverseOrder() {
        final RowDataset ds = createThreeRowScoreDataset();
        List<String> results = new ArrayList<>();
        ds.forEach(2, -1, (DisposableObjArray arr) -> {
            results.add(arr.get(1).toString());
        });

        assertEquals(3, results.size());
        assertEquals("Bob", results.get(0));
        assertEquals("Jane", results.get(1));
        assertEquals("John", results.get(2));
    }

    @Test
    public void testForEach() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        final int[] count = { 0 };
        ds.forEach(row -> {
            assertNotNull(row);
            assertEquals(4, row.length());
            count[0]++;
        });
        assertEquals(5, count[0]);
    }

    @Test
    public void testForEachWithColumnNames() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        final int[] count = { 0 };
        ds.forEach(Arrays.asList("id", "name"), row -> {
            assertNotNull(row);
            assertEquals(2, row.length());
            count[0]++;
        });
        assertEquals(5, count[0]);
    }

    @Test
    public void testForEach_WithTuple2_RangeAndFromIndex() {
        List<String> names = new ArrayList<>();
        dataset.forEach(1, 3, Tuple.of("id", "name"), (id, name) -> names.add(name.toString()));

        assertEquals(2, names.size());
        assertEquals("Bob", names.get(0));
        assertEquals("Charlie", names.get(1));
    }

    @Test
    public void testForEach_WithTuple3_RangeAndFromIndex() {
        List<String> results = new ArrayList<>();
        dataset.forEach(0, 2, Tuple.of("id", "name", "age"), (id, name, age) -> results.add(id + ":" + name));

        assertEquals(2, results.size());
        assertEquals("1:Alice", results.get(0));
    }

    @Test
    public void testForEachWithRangeAndTuple2() {
        final RowDataset ds = createThreeRowScoreDataset();
        List<String> results = new ArrayList<>();
        ds.forEach(1, 3, Tuple.of("name", "age"), (Throwables.BiConsumer<String, Integer, RuntimeException>) (name, age) -> results.add(name + ":" + age));

        assertEquals(2, results.size());
        assertEquals("Jane:30", results.get(0));
        assertEquals("Bob:35", results.get(1));
    }

    @Test
    public void testForEachWithRangeAndTuple3() {
        final RowDataset ds = createThreeRowScoreDataset();
        List<String> results = new ArrayList<>();
        ds.forEach(0, 2, new Tuple3<>("id", "name", "age"),
                (Throwables.TriConsumer<Integer, String, Integer, RuntimeException>) (id, name, age) -> results.add(id + ":" + name + ":" + age));

        assertEquals(2, results.size());
        assertEquals("1:John:25", results.get(0));
        assertEquals("2:Jane:30", results.get(1));
    }

    @Test
    public void testParallelOperationsConsistency() {
        int size = 100;
        List<Object> values = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            values.add(i);
        }

        List<String> columnNames = CommonUtil.toList("value");
        List<List<Object>> columnValues = CommonUtil.toList(values);
        RowDataset largeDs = new RowDataset(columnNames, columnValues);

        Dataset seqCopy = largeDs.copy();
        Dataset parCopy = largeDs.copy();

        seqCopy.sortBy("value", Comparator.reverseOrder());
        parCopy.parallelSortBy("value", Comparator.reverseOrder());

        for (int i = 0; i < size; i++) {
            assertEquals((Object) seqCopy.moveToRow(i).get("value"), (Object) parCopy.moveToRow(i).get("value"));
        }
    }

    @Test
    public void testComplexJoinScenario() {
        List<String> columnNames1 = CommonUtil.toList("id", "value");
        List<List<Object>> columns1 = new ArrayList<>();
        columns1.add(CommonUtil.toList(1, null, 3));
        columns1.add(CommonUtil.toList("A", "B", "C"));
        Dataset dsWithNull1 = new RowDataset(columnNames1, columns1);

        List<String> columnNames2 = CommonUtil.toList("id", "score");
        List<List<Object>> columns2 = new ArrayList<>();
        columns2.add(CommonUtil.toList((Object) null, 2, 3));
        columns2.add(CommonUtil.toList(10, 20, 30));
        Dataset dsWithNull2 = new RowDataset(columnNames2, columns2);

        Dataset result = dsWithNull1.rightJoin(dsWithNull2, "id", "id");
        assertNotNull(result);
        assertEquals(3, result.size());
    }

    @Test
    public void testFlatMapSingleColumn() {
        final RowDataset dataset = createFiveRowCityDataset();
        Function<Object, Collection<String>> mapper = name -> CommonUtil.toList(((String) name).toLowerCase(), ((String) name).toUpperCase());
        Dataset flatMapped = dataset.flatMapColumn("name", "variations", "id", mapper);

        assertNotNull(flatMapped);
        assertEquals(10, flatMapped.size());
        assertTrue(flatMapped.containsColumn("variations"));
        assertTrue(flatMapped.containsColumn("id"));
    }

    @Test
    public void testFlatMapSingleColumnWithMultipleCopying() {
        final RowDataset dataset = createFiveRowCityDataset();
        Function<Object, Collection<Integer>> mapper = age -> CommonUtil.toList((Integer) age, (Integer) age + 10);
        Dataset flatMapped = dataset.flatMapColumn("age", "ages", CommonUtil.toList("id", "name"), mapper);

        assertNotNull(flatMapped);
        assertEquals(10, flatMapped.size());
        assertTrue(flatMapped.containsColumn("ages"));
        assertTrue(flatMapped.containsColumn("id"));
        assertTrue(flatMapped.containsColumn("name"));
    }

    @Test
    public void testFlatMapTuple2() {
        final RowDataset dataset = createFiveRowCityDataset();
        BiFunction<Object, Object, Collection<String>> mapper = (name, age) -> CommonUtil.toList(name + "-young", name + "-old");
        Dataset flatMapped = dataset.flatMapColumns(Tuple.of("name", "age"), "status", CommonUtil.toList("id"), mapper);

        assertNotNull(flatMapped);
        assertEquals(10, flatMapped.size());
        assertTrue(flatMapped.containsColumn("status"));
        assertTrue(flatMapped.containsColumn("id"));
    }

    @Test
    public void testFlatMapTuple3() {
        final RowDataset dataset = createFiveRowCityDataset();
        TriFunction<Object, Object, Object, Collection<String>> mapper = (id, name, age) -> CommonUtil.toList("ID" + id, "NAME" + name, "AGE" + age);
        Dataset flatMapped = dataset.flatMapColumns(Tuple.of("id", "name", "age"), "tags", CommonUtil.toList("city"), mapper);

        assertNotNull(flatMapped);
        assertEquals(15, flatMapped.size());
        assertTrue(flatMapped.containsColumn("tags"));
        assertTrue(flatMapped.containsColumn("city"));
    }

    @Test
    public void testFlatMapMultipleColumns() {
        final RowDataset dataset = createFiveRowCityDataset();
        Function<DisposableObjArray, Collection<String>> mapper = arr -> CommonUtil.toList(arr.get(0).toString(), arr.get(1).toString());
        Dataset flatMapped = dataset.flatMapColumns(CommonUtil.toList("name", "city"), "values", CommonUtil.toList("id"), mapper);

        assertNotNull(flatMapped);
        assertEquals(10, flatMapped.size());
        assertTrue(flatMapped.containsColumn("values"));
        assertTrue(flatMapped.containsColumn("id"));
    }

    @Test
    public void testNullHandling() {
        List<String> columnNames = CommonUtil.toList("col1", "col2");
        List<List<Object>> columnValues = new ArrayList<>();
        columnValues.add(CommonUtil.toList("A", null, "B"));
        columnValues.add(CommonUtil.toList(1, 2, null));

        RowDataset ds = new RowDataset(columnNames, columnValues);

        String xml = ds.toXml();
        assertNotNull(xml);
        assertTrue(xml.contains("null"));

        String csv = ds.toCsv();
        assertNotNull(csv);

        Dataset filtered = ds.filter("col1", obj -> obj != null);
        assertEquals(2, filtered.size());

        Dataset grouped = ds.groupBy("col1", "col2", "values", Collectors.toList());
        assertNotNull(grouped);

        ds.sortBy("col1");
        assertEquals(3, ds.size());
    }

    @Test
    public void testLargeDatasetOperations() {
        int size = 1000;
        List<String> columnNames = CommonUtil.toList("id", "value", "category");
        List<List<Object>> columnValues = new ArrayList<>();

        List<Object> ids = new ArrayList<>();
        List<Object> values = new ArrayList<>();
        List<Object> categories = new ArrayList<>();

        for (int i = 0; i < size; i++) {
            ids.add(i);
            values.add(i % 100);
            categories.add("CAT" + (i % 10));
        }

        columnValues.add(ids);
        columnValues.add(values);
        columnValues.add(categories);

        RowDataset largeDs = new RowDataset(columnNames, columnValues);

        largeDs.parallelSortBy("value");
        List<Object> sortedValues = largeDs.getColumn("value");
        for (int i = 1; i < sortedValues.size(); i++) {
            assertTrue(((Integer) sortedValues.get(i - 1)) <= ((Integer) sortedValues.get(i)));
        }

        Dataset grouped = largeDs.groupBy("category", "value", "sum", Collectors.summingInt(o -> (Integer) o));
        assertEquals(10, grouped.size());

        Dataset top = largeDs.topBy("value", 10);
        assertEquals(10, top.size());
    }

    @Test
    public void testSpecialCharactersInData() {
        List<String> columnNames = CommonUtil.toList("text", "value");
        List<List<Object>> columnValues = new ArrayList<>();
        columnValues.add(CommonUtil.toList("Hello, World", "Test\"Quote", "Line\nBreak", "<tag>"));
        columnValues.add(CommonUtil.toList(1, 2, 3, 4));

        RowDataset specialDs = new RowDataset(columnNames, columnValues);

        String xml = specialDs.toXml();
        assertNotNull(xml);
        assertTrue(xml.contains("&lt;tag&gt;") || xml.contains("&lt;"));

        String csv = specialDs.toCsv();
        assertNotNull(csv);
        assertTrue(csv.contains("\"Hello, World\"") || csv.contains("Hello, World"));
    }

    @Test
    public void testXmlAndCsvWithEmptyColumns() {
        Collection<String> emptyColumns = Collections.emptyList();

        String xml = dataset.toXml(0, 2, emptyColumns);
        assertNotNull(xml);
        assertTrue(xml.contains("<dataset>"));
        assertTrue(xml.contains("</dataset>"));

        String csv = dataset.toCsv(0, 2, emptyColumns);
        assertNotNull(csv);
        assertEquals("", csv.trim());
    }

    @Test
    public void testRowIndexValidation() {
        assertThrows(IndexOutOfBoundsException.class, () -> {
            dataset.toXml(-1, 3);
        });

        assertThrows(IndexOutOfBoundsException.class, () -> {
            dataset.toXml(2, 10);
        });

        assertThrows(IndexOutOfBoundsException.class, () -> {
            dataset.toCsv(5, 3, CommonUtil.toList("name"));
        });

        assertThrows(IndexOutOfBoundsException.class, () -> {
            dataset.filter(-1, 5, arr -> true);
        });

        assertThrows(IndexOutOfBoundsException.class, () -> {
            dataset.copy(0, 10);
        });
    }

    @Test
    public void testMultipleGroupByScenarios() {
        final RowDataset dataset = createFiveRowCityDataset();

        Dataset countGrouped = dataset.groupBy("city", "name", "count", Collectors.counting());
        assertNotNull(countGrouped);

        Dataset listGrouped = dataset.groupBy("city", "age", "ages", Collectors.toList());
        assertNotNull(listGrouped);

        Dataset avgGrouped = dataset.groupBy("city", "age", "avgAge", Collectors.averagingInt(o -> (Integer) o));
        assertNotNull(avgGrouped);

        Collector<Object, ?, String> joiningCollector = Collectors.mapping(Object::toString, Collectors.joining(","));
        Dataset joinedGrouped = dataset.groupBy("city", "name", "names", joiningCollector);
        assertNotNull(joinedGrouped);
        assertTrue(joinedGrouped.containsColumn("names"));
    }

    @Test
    public void testPivotWithSingleAggregateColumn() {
        List<String> columnNames = CommonUtil.toList("row", "col", "value");
        List<List<Object>> columnValues = new ArrayList<>();
        columnValues.add(CommonUtil.toList("A", "A", "B", "B"));
        columnValues.add(CommonUtil.toList("X", "Y", "X", "Y"));
        columnValues.add(CommonUtil.toList(1, 2, 3, 4));

        RowDataset ds = new RowDataset(columnNames, columnValues);
        Collector<Object, ?, Integer> sumCollector = Collectors.summingInt(o -> (Integer) o);

        Sheet<String, String, Integer> pivot = ds.pivot("row", "col", "value", sumCollector);

        assertNotNull(pivot);
        assertEquals(2, pivot.rowKeySet().size());
        assertEquals(2, pivot.columnKeySet().size());
    }

    @Test
    public void testPivotWithMultipleAggregateColumns() {
        List<String> columnNames = CommonUtil.toList("row", "col", "val1", "val2");
        List<List<Object>> columnValues = new ArrayList<>();
        columnValues.add(CommonUtil.toList("A", "A", "B", "B"));
        columnValues.add(CommonUtil.toList("X", "Y", "X", "Y"));
        columnValues.add(CommonUtil.toList(1, 2, 3, 4));
        columnValues.add(CommonUtil.toList(5, 6, 7, 8));

        RowDataset ds = new RowDataset(columnNames, columnValues);
        Collector<Object[], ?, String> joiningCollector = Collectors.mapping(arr -> arr[0] + "-" + arr[1], Collectors.joining(","));

        Sheet<String, String, String> pivot = ds.pivot("row", "col", CommonUtil.toList("val1", "val2"), joiningCollector);

        assertNotNull(pivot);
        assertEquals(2, pivot.rowKeySet().size());
        assertEquals(2, pivot.columnKeySet().size());
    }

    @Test
    public void testPivotWithRowMapper() {
        List<String> columnNames = CommonUtil.toList("row", "col", "val1", "val2");
        List<List<Object>> columnValues = new ArrayList<>();
        columnValues.add(CommonUtil.toList("A", "A", "B", "B"));
        columnValues.add(CommonUtil.toList("X", "Y", "X", "Y"));
        columnValues.add(CommonUtil.toList(1, 2, 3, 4));
        columnValues.add(CommonUtil.toList(5, 6, 7, 8));

        RowDataset ds = new RowDataset(columnNames, columnValues);
        Function<DisposableObjArray, Integer> rowMapper = arr -> (Integer) arr.get(0) + (Integer) arr.get(1);
        Collector<Integer, ?, Integer> sumCollector = Collectors.summingInt(Integer::intValue);

        Sheet<String, String, Integer> pivot = ds.pivot("row", "col", CommonUtil.toList("val1", "val2"), rowMapper, sumCollector);

        assertNotNull(pivot);
        assertEquals(2, pivot.rowKeySet().size());
        assertEquals(2, pivot.columnKeySet().size());
    }

    @Test
    public void testRollup() {
        final RowDataset dataset = createFiveRowCityDataset();
        List<Dataset> rollups = dataset.rollup(CommonUtil.toList("city", "name")).toList();

        assertNotNull(rollups);
        assertTrue(rollups.size() > 0);
    }

    @Test
    public void testRollupWithCollector() {
        final RowDataset dataset = createFiveRowCityDataset();
        Collector<Object, ?, Long> countCollector = Collectors.counting();
        List<Dataset> rollups = dataset.rollup(CommonUtil.toList("city"), "name", "count", countCollector).toList();

        assertNotNull(rollups);
        assertTrue(rollups.size() > 0);
        for (Dataset ds : rollups) {
            assertTrue(ds.containsColumn("count"));
        }
    }

    @Test
    public void testRollupWithArrayCollector() {
        final RowDataset dataset = createFiveRowCityDataset();
        Collector<Object[], ?, Long> countCollector = Collectors.counting();
        List<Dataset> rollups = dataset.rollup(CommonUtil.toList("city"), CommonUtil.toList("name", "age"), "count", countCollector).toList();

        assertNotNull(rollups);
        assertTrue(rollups.size() > 0);
        for (Dataset ds : rollups) {
            if (ds.columnCount() > 1) {
                assertTrue(ds.containsColumn("count"));
            }
        }
    }

    @Test
    public void testRollupWithKeyExtractor() {
        final RowDataset dataset = createFiveRowCityDataset();
        Function<DisposableObjArray, String> keyExtractor = arr -> arr.get(0).toString();
        List<Dataset> rollups = dataset.rollup(CommonUtil.toList("city"), keyExtractor).toList();

        assertNotNull(rollups);
        assertTrue(rollups.size() > 0);
    }

    @Test
    public void testRollupWithKeyExtractorAndCollector() {
        final RowDataset dataset = createFiveRowCityDataset();
        Function<DisposableObjArray, String> keyExtractor = arr -> arr.get(0).toString();
        Collector<Object, ?, Long> countCollector = Collectors.counting();
        List<Dataset> rollups = dataset.rollup(CommonUtil.toList("city"), keyExtractor, "name", "count", countCollector).toList();

        assertNotNull(rollups);
        assertTrue(rollups.size() > 0);
    }

    @Test
    public void testRollupWithKeyExtractorAndRowType() {
        final RowDataset dataset = createFiveRowCityDataset();
        Function<DisposableObjArray, String> keyExtractor = arr -> arr.get(0).toString();
        List<Dataset> rollups = dataset.rollup(CommonUtil.toList("city"), keyExtractor, CommonUtil.toList("name"), "names", List.class).toList();

        assertNotNull(rollups);
        assertTrue(rollups.size() > 0);
    }

    @Test
    public void testRollupWithKeyExtractorAndArrayCollector() {
        final RowDataset dataset = createFiveRowCityDataset();
        Function<DisposableObjArray, String> keyExtractor = arr -> arr.get(0).toString();
        Collector<Object[], ?, Long> countCollector = Collectors.counting();
        List<Dataset> rollups = dataset.rollup(CommonUtil.toList("city"), keyExtractor, CommonUtil.toList("name"), "count", countCollector).toList();

        assertNotNull(rollups);
        assertTrue(rollups.size() > 0);
    }

    @Test
    public void testRollupWithKeyExtractorAndRowMapper() {
        final RowDataset dataset = createFiveRowCityDataset();
        Function<DisposableObjArray, String> keyExtractor = arr -> arr.get(0).toString();
        Function<DisposableObjArray, String> rowMapper = arr -> arr.get(0).toString();
        Collector<String, ?, List<String>> collector = Collectors.toList();
        List<Dataset> rollups = dataset.rollup(CommonUtil.toList("city"), keyExtractor, CommonUtil.toList("name"), "names", rowMapper, collector).toList();

        assertNotNull(rollups);
        assertTrue(rollups.size() > 0);
    }

    @Test
    public void testRollupWithRowType() {
        final RowDataset dataset = createFiveRowCityDataset();
        List<Dataset> rollups = dataset.rollup(CommonUtil.toList("city"), CommonUtil.toList("name"), "names", List.class).toList();

        assertNotNull(rollups);
        assertTrue(rollups.size() > 0);
        for (Dataset ds : rollups) {
            if (ds.columnCount() > 1) {
                assertTrue(ds.containsColumn("names"));
            }
        }
    }

    @Test
    public void testRollupWithRowMapper() {
        final RowDataset dataset = createFiveRowCityDataset();
        Function<DisposableObjArray, String> rowMapper = arr -> arr.get(0).toString();
        Collector<String, ?, List<String>> collector = Collectors.toList();
        List<Dataset> rollups = dataset.rollup(CommonUtil.toList("city"), CommonUtil.toList("name"), "names", rowMapper, collector).toList();

        assertNotNull(rollups);
        assertTrue(rollups.size() > 0);
        for (Dataset ds : rollups) {
            if (ds.columnCount() > 1) {
                assertTrue(ds.containsColumn("names"));
            }
        }
    }

    @Test
    public void testCube() {
        final RowDataset dataset = createFiveRowCityDataset();
        List<Dataset> cubes = dataset.cube(CommonUtil.toList("city", "name")).toList();

        assertNotNull(cubes);
        assertTrue(cubes.size() > 0);
    }

    @Test
    public void testCubeWithCollector() {
        final RowDataset dataset = createFiveRowCityDataset();
        Collector<Object, ?, Long> countCollector = Collectors.counting();
        List<Dataset> cubes = dataset.cube(CommonUtil.toList("city"), "name", "count", countCollector).toList();

        assertNotNull(cubes);
        assertTrue(cubes.size() > 0);
    }

    @Test
    public void testCubeWithArrayCollector() {
        final RowDataset dataset = createFiveRowCityDataset();
        Collector<Object[], ?, Long> countCollector = Collectors.counting();
        List<Dataset> cubes = dataset.cube(CommonUtil.toList("city"), CommonUtil.toList("name"), "count", countCollector).toList();

        assertNotNull(cubes);
        assertTrue(cubes.size() > 0);
    }

    @Test
    public void testCubeWithKeyExtractor() {
        final RowDataset dataset = createFiveRowCityDataset();
        Function<DisposableObjArray, String> keyExtractor = arr -> arr.get(0).toString();
        List<Dataset> cubes = dataset.cube(CommonUtil.toList("city"), keyExtractor).toList();

        assertNotNull(cubes);
        assertTrue(cubes.size() > 0);
    }

    @Test
    public void testCubeWithKeyExtractorAndCollector() {
        final RowDataset dataset = createFiveRowCityDataset();
        Function<DisposableObjArray, String> keyExtractor = arr -> arr.get(0).toString();
        Collector<Object, ?, Long> countCollector = Collectors.counting();
        List<Dataset> cubes = dataset.cube(CommonUtil.toList("city"), keyExtractor, "name", "count", countCollector).toList();

        assertNotNull(cubes);
        assertTrue(cubes.size() > 0);
    }

    @Test
    public void testCubeWithKeyExtractorAndRowType() {
        final RowDataset dataset = createFiveRowCityDataset();
        Function<DisposableObjArray, String> keyExtractor = arr -> arr.join(", ");
        List<Dataset> cubes = dataset.cube(CommonUtil.toList("city"), keyExtractor, CommonUtil.toList("name"), "names", List.class).toList();

        cubes.forEach(Dataset::println);

        assertNotNull(cubes);
        assertEquals(2, cubes.size());

        cubes = dataset.cube(CommonUtil.toList("city", "age"), keyExtractor, CommonUtil.toList("name"), "names", List.class).toList();

        cubes.forEach(Dataset::println);

        assertNotNull(cubes);
        assertEquals(4, cubes.size());

        dataset.groupBy(CommonUtil.toList("city", "age"), keyExtractor, CommonUtil.toList("name"), "names", List.class);
    }

    @Test
    public void testEmptyDatasetOperations() {
        Dataset result1 = emptyDs.rightJoin(ds1, "col1", "id");
        assertEquals(ds1.size(), result1.size());

        Dataset result2 = emptyDs.fullJoin(ds1, "col1", "id");
        assertEquals(ds1.size(), result2.size());

        assertThrows(IllegalArgumentException.class, () -> emptyDs.union(ds1));
        assertThrows(IllegalArgumentException.class, () -> emptyDs.intersect(ds1));

        Dataset emptyDataset = CommonUtil.newEmptyDataset(ds1.columnNames());
        Dataset result3 = emptyDataset.union(ds1);
        assertTrue(result3.size() >= 0);
        Dataset result4 = emptyDataset.intersect(ds1);
        assertEquals(0, result4.size());
    }

    @Test
    public void testRenameAllColumnsWithFunction() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        ds.renameColumns(name -> "col_" + name);
        assertEquals("col_id", ds.getColumnName(0));
        assertEquals("col_name", ds.getColumnName(1));
        assertEquals("col_age", ds.getColumnName(2));
        assertEquals("col_salary", ds.getColumnName(3));
    }

    @Test
    public void testDifferenceWithKeyColumns() {
        final RowDataset dataset = createFourRowCityDataset();
        List<String> otherColumnNames = Arrays.asList("id", "name", "age", "city");
        List<List<Object>> otherColumnList = new ArrayList<>();
        otherColumnList.add(Arrays.asList(1, 2));
        otherColumnList.add(Arrays.asList("Different", "Different"));
        otherColumnList.add(Arrays.asList(30, 35));
        otherColumnList.add(Arrays.asList("LA", "Chicago"));

        RowDataset otherDataset = new RowDataset(otherColumnNames, otherColumnList);

        Collection<String> keyColumns = Arrays.asList("id");
        Dataset difference = N.difference(dataset, otherDataset, keyColumns);

        Assertions.assertEquals(2, difference.size());
    }

    @Test
    public void testSymmetricDifferenceWithKeyColumns() {
        final RowDataset dataset = createFourRowCityDataset();
        List<String> otherColumnNames = Arrays.asList("id", "name", "age", "city");
        List<List<Object>> otherColumnList = new ArrayList<>();
        otherColumnList.add(Arrays.asList(1, 5, 6));
        otherColumnList.add(Arrays.asList("Different", "Eve", "Frank"));
        otherColumnList.add(Arrays.asList(30, 40, 45));
        otherColumnList.add(Arrays.asList("LA", "Miami", "Seattle"));

        RowDataset otherDataset = new RowDataset(otherColumnNames, otherColumnList);

        Collection<String> keyColumns = Arrays.asList("id");
        Dataset symmetricDiff = N.symmetricDifference(dataset, otherDataset, keyColumns);

        Assertions.assertEquals(5, symmetricDiff.size());
    }

    @Test
    public void testDifferenceWithRequireSameColumns() {
        final RowDataset dataset = createFourRowCityDataset();
        List<String> otherColumnNames = Arrays.asList("id", "name", "age", "city");
        List<List<Object>> otherColumnList = new ArrayList<>();
        otherColumnList.add(Arrays.asList(3, 4));
        otherColumnList.add(Arrays.asList("Bob", "Alice"));
        otherColumnList.add(Arrays.asList(35, 28));
        otherColumnList.add(Arrays.asList("Chicago", "Boston"));

        RowDataset otherDataset = new RowDataset(otherColumnNames, otherColumnList);

        Dataset difference = N.difference(dataset, otherDataset, true);

        Assertions.assertEquals(3, difference.size());
    }

    @Test
    public void testSymmetricDifferenceWithRequireSameColumns() {
        final RowDataset dataset = createFourRowCityDataset();
        List<String> otherColumnNames = Arrays.asList("id", "name", "age", "city");
        List<List<Object>> otherColumnList = new ArrayList<>();
        otherColumnList.add(Arrays.asList(3, 4, 5));
        otherColumnList.add(Arrays.asList("Bob", "Alice", "Eve"));
        otherColumnList.add(Arrays.asList(35, 28, 40));
        otherColumnList.add(Arrays.asList("Chicago", "Boston", "Miami"));

        RowDataset otherDataset = new RowDataset(otherColumnNames, otherColumnList);

        Dataset symmetricDiff = N.symmetricDifference(dataset, otherDataset, true);

        Assertions.assertEquals(5, symmetricDiff.size());
    }

    @Test
    public void testSymmetricDifferenceWithKeyColumnsAndRequireSameColumns() {
        final RowDataset dataset = createFourRowCityDataset();
        List<String> otherColumnNames = Arrays.asList("id", "name", "age", "city");
        List<List<Object>> otherColumnList = new ArrayList<>();
        otherColumnList.add(Arrays.asList(2, 5));
        otherColumnList.add(Arrays.asList("Jane", "Eve"));
        otherColumnList.add(Arrays.asList(30, 40));
        otherColumnList.add(Arrays.asList("LA", "Miami"));

        RowDataset otherDataset = new RowDataset(otherColumnNames, otherColumnList);

        Collection<String> keyColumns = Arrays.asList("id");
        Dataset symmetricDiff = N.symmetricDifference(dataset, otherDataset, keyColumns, true);

        Assertions.assertEquals(4, symmetricDiff.size());
    }

    @Test
    public void testConstructorWithMismatchedColumnSizes() {
        List<List<Object>> badCols = new ArrayList<>();
        badCols.add(new ArrayList<>(Arrays.asList(1, 2, 3)));
        badCols.add(new ArrayList<>(Arrays.asList("A", "B")));
        badCols.add(new ArrayList<>(Arrays.asList(10, 20, 30)));

        assertThrows(IllegalArgumentException.class, () -> {
            new RowDataset(columnNames, badCols);
        });
    }

    @Test
    public void testConstructorWithNullColumns() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            new RowDataset(columnNames, null);
        });
    }

    @Test
    public void testConstructorWithInconsistentColumnSizes() {
        List<List<Object>> badColumns = new ArrayList<>();
        badColumns.add(Arrays.asList(1, 2));
        badColumns.add(Arrays.asList("John", "Jane", "Bob"));

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            new RowDataset(Arrays.asList("id", "name"), badColumns);
        });
    }

    @Test
    public void testSwapRowPosition() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        Object firstValue = ds.get(0, 1);
        Object lastValue = ds.get(4, 1);
        ds.swapRows(0, 4);
        assertEquals(lastValue, ds.get(0, 1));
        assertEquals(firstValue, ds.get(4, 1));
    }

    @Test
    public void testConstructorWithSingleRow() {
        List<List<Object>> singleRow = new ArrayList<>();
        singleRow.add(new ArrayList<>(Arrays.asList(1)));
        singleRow.add(new ArrayList<>(Arrays.asList("Alice")));
        singleRow.add(new ArrayList<>(Arrays.asList(25)));
        singleRow.add(new ArrayList<>(Arrays.asList(50000.0)));

        RowDataset ds = new RowDataset(columnNames, singleRow);
        assertEquals(1, ds.size());
        assertEquals(4, ds.columnCount());
    }

    @Test
    public void testSwapRowPositionSame() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        Object value = ds.get(0, 1);
        ds.swapRows(0, 0);
        assertEquals(value, ds.get(0, 1));
    }

    @Test
    public void testInnerJoinSingleColumn() {
        final RowDataset dataset = createFiveRowCityDataset();
        List<String> rightColumns = CommonUtil.toList("city", "country");
        List<List<Object>> rightValues = new ArrayList<>();
        rightValues.add(CommonUtil.toList("NYC", "LA", "Chicago"));
        rightValues.add(CommonUtil.toList("USA", "USA", "USA"));

        RowDataset right = new RowDataset(rightColumns, rightValues);

        Dataset joined = dataset.innerJoin(right, "city", "city");

        assertNotNull(joined);
        assertEquals(5, joined.size());
        assertTrue(joined.containsColumn("country"));
    }

    @Test
    public void testInnerJoinGeneratesUniqueNamesForChainedColumnCollisions() {
        final RowDataset left = new RowDataset(new ArrayList<>(Arrays.asList("id", "id_2")),
                new ArrayList<>(Arrays.asList(new ArrayList<>(Arrays.asList(1)), new ArrayList<>(Arrays.asList("left")))));
        final RowDataset right = new RowDataset(new ArrayList<>(Arrays.asList("id", "id_2")),
                new ArrayList<>(Arrays.asList(new ArrayList<>(Arrays.asList(1)), new ArrayList<>(Arrays.asList("right")))));

        final Dataset joined = left.innerJoin(right, "id", "id");

        assertEquals(Arrays.asList("id", "id_2", "id_3", "id_2_2"), joined.columnNames());
        assertEquals(1, joined.size());
        assertEquals(1, (Integer) joined.get(0, joined.getColumnIndex("id_3")));
        assertEquals("right", joined.get(0, joined.getColumnIndex("id_2_2")));
    }

    @Test
    public void testInnerJoinMultipleColumns() {
        final RowDataset dataset = createFiveRowCityDataset();
        List<String> rightColumns = CommonUtil.toList("city", "age", "salary");
        List<List<Object>> rightValues = new ArrayList<>();
        rightValues.add(CommonUtil.toList("NYC", "LA", "NYC"));
        rightValues.add(CommonUtil.toList(25, 30, 35));
        rightValues.add(CommonUtil.toList(50000, 60000, 70000));

        RowDataset right = new RowDataset(rightColumns, rightValues);

        Map<String, String> onColumns = new HashMap<>();
        onColumns.put("city", "city");
        onColumns.put("age", "age");

        Dataset joined = dataset.innerJoin(right, onColumns);

        assertNotNull(joined);
        assertTrue(joined.containsColumn("salary"));
    }

    @Test
    public void testInnerJoinWithNewColumn() {
        final RowDataset dataset = createFiveRowCityDataset();
        List<String> rightColumns = CommonUtil.toList("city", "info");
        List<List<Object>> rightValues = new ArrayList<>();
        rightValues.add(CommonUtil.toList("NYC", "LA", "Chicago"));
        rightValues.add(CommonUtil.toList("Big Apple", "City of Angels", "Windy City"));

        RowDataset right = new RowDataset(rightColumns, rightValues);

        Map<String, String> onColumns = new HashMap<>();
        onColumns.put("city", "city");

        Dataset joined = dataset.innerJoin(right, onColumns, "cityInfo", Object[].class);

        assertNotNull(joined);
        assertTrue(joined.containsColumn("cityInfo"));
    }

    @Test
    public void testInnerJoinWithCollectionSupplier() {
        final RowDataset dataset = createFiveRowCityDataset();
        List<String> rightColumns = CommonUtil.toList("city", "tag");
        List<List<Object>> rightValues = new ArrayList<>();
        rightValues.add(CommonUtil.toList("NYC", "NYC", "LA"));
        rightValues.add(CommonUtil.toList("tag1", "tag2", "tag3"));

        RowDataset right = new RowDataset(rightColumns, rightValues);

        Map<String, String> onColumns = new HashMap<>();
        onColumns.put("city", "city");

        IntFunction<List<Object>> collSupplier = size -> new ArrayList<>(size);
        Dataset joined = dataset.innerJoin(right, onColumns, "tags", Object[].class, collSupplier);

        assertNotNull(joined);
        assertTrue(joined.containsColumn("tags"));
    }

    @Test
    public void testLeftJoinWithSingleColumnName() {
        Dataset result = ds1.leftJoin(ds2, "id", "id");

        assertNotNull(result);
        assertEquals(3, result.size());
        assertTrue(result.containsColumn("id"));
        assertTrue(result.containsColumn("name"));
        assertTrue(result.containsColumn("age"));
        assertTrue(result.containsColumn("city"));
        assertTrue(result.containsColumn("salary"));

        assertEquals(1, (Integer) result.moveToRow(0).get("id"));
        assertEquals("Alice", result.moveToRow(0).get("name"));
        assertEquals(25, (Integer) result.moveToRow(0).get("age"));
        assertNull(result.moveToRow(0).get("city"));
        assertNull(result.moveToRow(0).get("salary"));

        assertEquals(2, (Integer) result.moveToRow(1).get("id"));
        assertEquals("Bob", result.moveToRow(1).get("name"));
        assertEquals(30, (Integer) result.moveToRow(1).get("age"));
        assertEquals("New York", result.moveToRow(1).get("city"));
        assertEquals(50000, (Integer) result.moveToRow(1).get("salary"));

        assertEquals(3, (Integer) result.moveToRow(2).get("id"));
        assertEquals("Charlie", result.moveToRow(2).get("name"));
        assertEquals(35, (Integer) result.moveToRow(2).get("age"));
        assertEquals("London", result.moveToRow(2).get("city"));
        assertEquals(60000, (Integer) result.moveToRow(2).get("salary"));
    }

    @Test
    public void testLeftJoinSingleColumn() {
        final RowDataset dataset = createFiveRowCityDataset();
        List<String> rightColumns = CommonUtil.toList("city", "country");
        List<List<Object>> rightValues = new ArrayList<>();
        rightValues.add(CommonUtil.toList("NYC", "LA"));
        rightValues.add(CommonUtil.toList("USA", "USA"));

        RowDataset right = new RowDataset(rightColumns, rightValues);

        Dataset joined = dataset.leftJoin(right, "city", "city");

        assertNotNull(joined);
        assertEquals(5, joined.size());
        assertTrue(joined.containsColumn("country"));
    }

    @Test
    public void testLeftJoinMultipleColumns() {
        final RowDataset dataset = createFiveRowCityDataset();
        List<String> rightColumns = CommonUtil.toList("city", "age", "bonus");
        List<List<Object>> rightValues = new ArrayList<>();
        rightValues.add(CommonUtil.toList("NYC", "LA"));
        rightValues.add(CommonUtil.toList(25, 30));
        rightValues.add(CommonUtil.toList(1000, 2000));

        RowDataset right = new RowDataset(rightColumns, rightValues);

        Map<String, String> onColumns = new HashMap<>();
        onColumns.put("city", "city");
        onColumns.put("age", "age");

        Dataset joined = dataset.leftJoin(right, onColumns);

        assertNotNull(joined);
        assertEquals(5, joined.size());
    }

    @Test
    public void testLeftJoinWithNewColumn() {
        final RowDataset dataset = createFiveRowCityDataset();
        List<String> rightColumns = CommonUtil.toList("city", "population");
        List<List<Object>> rightValues = new ArrayList<>();
        rightValues.add(CommonUtil.toList("NYC", "LA"));
        rightValues.add(CommonUtil.toList(8000000, 4000000));

        RowDataset right = new RowDataset(rightColumns, rightValues);

        Map<String, String> onColumns = new HashMap<>();
        onColumns.put("city", "city");

        Dataset joined = dataset.leftJoin(right, onColumns, "cityData", Object[].class);

        assertNotNull(joined);
        assertEquals(5, joined.size());
        assertTrue(joined.containsColumn("cityData"));
    }

    @Test
    public void testLeftJoinWithCollectionSupplier() {
        final RowDataset dataset = createFiveRowCityDataset();
        List<String> rightColumns = CommonUtil.toList("city", "feature");
        List<List<Object>> rightValues = new ArrayList<>();
        rightValues.add(CommonUtil.toList("NYC", "NYC", "LA"));
        rightValues.add(CommonUtil.toList("feature1", "feature2", "feature3"));

        RowDataset right = new RowDataset(rightColumns, rightValues);

        Map<String, String> onColumns = new HashMap<>();
        onColumns.put("city", "city");

        IntFunction<Set<Object>> collSupplier = size -> new HashSet<>(size);
        Dataset joined = dataset.leftJoin(right, onColumns, "features", Object[].class, collSupplier);

        assertNotNull(joined);
        assertEquals(5, joined.size());
        assertTrue(joined.containsColumn("features"));
    }

    @Test
    public void testRightJoinWithSingleColumnName() {
        Dataset result = ds1.rightJoin(ds2, "id", "id");

        assertNotNull(result);
        assertEquals(3, result.size());
        assertTrue(result.containsColumn("id"));
        assertTrue(result.containsColumn("name"));
        assertTrue(result.containsColumn("age"));
        assertTrue(result.containsColumn("city"));
        assertTrue(result.containsColumn("salary"));

        assertEquals(2, (Integer) result.moveToRow(0).get("id"));
        assertEquals("Bob", result.moveToRow(0).get("name"));
        assertEquals(30, (Integer) result.moveToRow(0).get("age"));
        assertEquals("New York", result.moveToRow(0).get("city"));
        assertEquals(50000, (Integer) result.moveToRow(0).get("salary"));

        assertEquals(3, (Integer) result.moveToRow(1).get("id"));
        assertEquals("Charlie", result.moveToRow(1).get("name"));
        assertEquals(35, (Integer) result.moveToRow(1).get("age"));
        assertEquals("London", result.moveToRow(1).get("city"));
        assertEquals(60000, (Integer) result.moveToRow(1).get("salary"));

        assertNull(result.moveToRow(2).get("id"));
        assertNull(result.moveToRow(2).get("name"));
        assertNull(result.moveToRow(2).get("age"));
        assertEquals("Tokyo", result.moveToRow(2).get("city"));
        assertEquals(70000, (Integer) result.moveToRow(2).get("salary"));
    }

    @Test
    public void testRightJoinWithMap() {
        Map<String, String> onColumnNames = new HashMap<>();
        onColumnNames.put("id", "id");

        Dataset result = ds1.rightJoin(ds2, onColumnNames);

        assertNotNull(result);
        assertEquals(3, result.size());

        assertEquals(2, (Integer) result.moveToRow(0).get("id"));
        assertEquals("Bob", result.moveToRow(0).get("name"));
        assertEquals(30, (Integer) result.moveToRow(0).get("age"));
    }

    @Test
    public void testRightJoinWithMultipleColumns() {
        List<String> columnNames1 = CommonUtil.toList("id", "type", "value");
        List<List<Object>> columns1 = new ArrayList<>();
        columns1.add(CommonUtil.toList(1, 1, 2));
        columns1.add(CommonUtil.toList("A", "B", "A"));
        columns1.add(CommonUtil.toList(100, 200, 300));
        Dataset multiDs1 = new RowDataset(columnNames1, columns1);

        List<String> columnNames2 = CommonUtil.toList("id", "type", "score");
        List<List<Object>> columns2 = new ArrayList<>();
        columns2.add(CommonUtil.toList(1, 2, 3));
        columns2.add(CommonUtil.toList("A", "A", "B"));
        columns2.add(CommonUtil.toList(10, 20, 30));
        Dataset multiDs2 = new RowDataset(columnNames2, columns2);

        Map<String, String> onColumnNames = new HashMap<>();
        onColumnNames.put("id", "id");
        onColumnNames.put("type", "type");

        Dataset result = multiDs1.rightJoin(multiDs2, onColumnNames);

        assertNotNull(result);
        assertEquals(3, result.size());
    }

    @Test
    public void testRightJoinWithEmptyRightDataset() {
        Dataset result = ds1.rightJoin(emptyDs, "id", "col1");

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    public void testRightJoinWithNewColumn() {
        Map<String, String> onColumnNames = new HashMap<>();
        onColumnNames.put("id", "id");

        Dataset result = ds1.rightJoin(ds2, onColumnNames, "rightData", Map.class);

        assertNotNull(result);
        assertEquals(3, result.size());
        assertTrue(result.containsColumn("rightData"));

        Map<String, Object> firstRightData = (Map<String, Object>) result.moveToRow(2).get("rightData");
        assertNotNull(firstRightData);
    }

    @Test
    public void testRightJoinWithCollectionSupplier() {
        Map<String, String> onColumnNames = new HashMap<>();
        onColumnNames.put("id", "id");

        List<String> columnNames = CommonUtil.toList("id", "value");
        List<List<Object>> columns = new ArrayList<>();
        columns.add(CommonUtil.toList(2, 2, 3));
        columns.add(CommonUtil.toList("X", "Y", "Z"));
        Dataset dsWithDuplicates = new RowDataset(columnNames, columns);

        Dataset result = ds1.rightJoin(dsWithDuplicates, onColumnNames, "values", List.class, ArrayList::new);

        assertNotNull(result);
        assertTrue(result.containsColumn("values"));
    }

    @Test
    public void testFullJoinWithSingleColumnName() {
        Dataset result = ds1.fullJoin(ds2, "id", "id");

        assertNotNull(result);
        assertEquals(4, result.size());

        assertTrue(result.containsColumn("id"));
        assertTrue(result.containsColumn("name"));
        assertTrue(result.containsColumn("age"));
        assertTrue(result.containsColumn("city"));
        assertTrue(result.containsColumn("salary"));

        assertEquals(1, (Integer) result.moveToRow(0).get("id"));
        assertEquals("Alice", result.moveToRow(0).get("name"));
        assertEquals(25, (Integer) result.moveToRow(0).get("age"));
        assertNull(result.moveToRow(0).get("city"));
        assertNull(result.moveToRow(0).get("salary"));
    }

    @Test
    public void testIntersection() {
        final RowDataset dataset = createFourRowCityDataset();
        List<String> otherColumnNames = Arrays.asList("id", "name", "age", "city");
        List<List<Object>> otherColumnList = new ArrayList<>();
        otherColumnList.add(Arrays.asList(2, 3, 5, 6));
        otherColumnList.add(Arrays.asList("Jane", "Bob", "Eve", "Frank"));
        otherColumnList.add(Arrays.asList(30, 35, 40, 45));
        otherColumnList.add(Arrays.asList("LA", "Chicago", "Miami", "Seattle"));

        RowDataset otherDataset = new RowDataset(otherColumnNames, otherColumnList);

        Dataset intersection = N.intersection(dataset, otherDataset);

        Assertions.assertEquals(2, intersection.size());
    }

    @Test
    public void testIntersectionWithKeyColumns() {
        final RowDataset dataset = createFourRowCityDataset();
        List<String> otherColumnNames = Arrays.asList("id", "name", "age", "city");
        List<List<Object>> otherColumnList = new ArrayList<>();
        otherColumnList.add(Arrays.asList(1, 2, 5, 6));
        otherColumnList.add(Arrays.asList("Different", "Different", "Eve", "Frank"));
        otherColumnList.add(Arrays.asList(30, 35, 40, 45));
        otherColumnList.add(Arrays.asList("LA", "Chicago", "Miami", "Seattle"));

        RowDataset otherDataset = new RowDataset(otherColumnNames, otherColumnList);

        Collection<String> keyColumns = Arrays.asList("id");
        Dataset intersection = N.intersection(dataset, otherDataset, keyColumns);

        Assertions.assertEquals(2, intersection.size());
    }

    @Test
    public void testIntersectionWithRequireSameColumns() {
        final RowDataset dataset = createFourRowCityDataset();
        List<String> otherColumnNames = Arrays.asList("id", "name", "age", "city");
        List<List<Object>> otherColumnList = new ArrayList<>();
        otherColumnList.add(Arrays.asList(2, 3));
        otherColumnList.add(Arrays.asList("Jane", "Bob"));
        otherColumnList.add(Arrays.asList(30, 35));
        otherColumnList.add(Arrays.asList("LA", "Chicago"));

        RowDataset otherDataset = new RowDataset(otherColumnNames, otherColumnList);

        Dataset intersection = N.intersection(dataset, otherDataset, true);

        Assertions.assertEquals(2, intersection.size());
    }

    @Test
    public void testSwapColumnPosition() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        ds.swapColumns("id", "salary");
        assertEquals("salary", ds.getColumnName(0));
        assertEquals("name", ds.getColumnName(1));
        assertEquals("age", ds.getColumnName(2));
        assertEquals("id", ds.getColumnName(3));
    }

    @Test
    public void testDifference() {
        final RowDataset dataset = createFourRowCityDataset();
        List<String> otherColumnNames = Arrays.asList("id", "name", "age", "city");
        List<List<Object>> otherColumnList = new ArrayList<>();
        otherColumnList.add(Arrays.asList(2, 3, 5, 6));
        otherColumnList.add(Arrays.asList("Jane", "Bob", "Eve", "Frank"));
        otherColumnList.add(Arrays.asList(30, 35, 40, 45));
        otherColumnList.add(Arrays.asList("LA", "Chicago", "Miami", "Seattle"));

        RowDataset otherDataset = new RowDataset(otherColumnNames, otherColumnList);

        Dataset difference = N.difference(dataset, otherDataset);

        Assertions.assertEquals(2, difference.size());
    }

    @Test
    public void testSymmetricDifference() {
        final RowDataset dataset = createFourRowCityDataset();
        List<String> otherColumnNames = Arrays.asList("id", "name", "age", "city");
        List<List<Object>> otherColumnList = new ArrayList<>();
        otherColumnList.add(Arrays.asList(2, 3, 5, 6));
        otherColumnList.add(Arrays.asList("Jane", "Bob", "Eve", "Frank"));
        otherColumnList.add(Arrays.asList(30, 35, 40, 45));
        otherColumnList.add(Arrays.asList("LA", "Chicago", "Miami", "Seattle"));

        RowDataset otherDataset = new RowDataset(otherColumnNames, otherColumnList);

        Dataset symmetricDiff = N.symmetricDifference(dataset, otherDataset);

        Assertions.assertEquals(4, symmetricDiff.size());
    }

    @Test
    public void testConstructorBasic() {
        RowDataset ds = new RowDataset(columnNames, columnList);
        assertNotNull(ds);
        assertEquals(5, ds.size());
        assertEquals(4, ds.columnCount());
    }

    @Test
    public void testConstructorWithProperties() {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("source", "test");
        props.put("version", 1);
        RowDataset ds = new RowDataset(columnNames, columnList, props);
        assertNotNull(ds);
        assertEquals("test", ds.getProperties().get("source"));
        assertEquals(1, ds.getProperties().get("version"));
    }

    @Test
    public void testConstructorWithNullProperties() {
        RowDataset ds = new RowDataset(columnNames, columnList, null);
        assertNotNull(ds);
        assertNotNull(ds.getProperties());
        assertTrue(ds.getProperties().isEmpty());
    }

    @Test
    public void testSwapColumnPositionSame() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        ds.swapColumns("id", "id");
        assertEquals("id", ds.getColumnName(0));
    }

    @Test
    public void testConstructorWithNullColumnList() {
        assertThrows(Exception.class, () -> {
            new RowDataset(columnNames, null);
        });
    }

    @Test
    public void testConstructorWithEmptyColumnName() {
        List<String> badNames = Arrays.asList("id", "", "age");
        List<List<Object>> cols = Arrays.asList(Arrays.asList(1, 2), Arrays.asList("A", "B"), Arrays.asList(10, 20));
        assertThrows(IllegalArgumentException.class, () -> {
            new RowDataset(badNames, cols);
        });
    }

    @Test
    public void testSwapColumnPositionFrozen() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        ds.freeze();
        assertThrows(IllegalStateException.class, () -> {
            ds.swapColumns("id", "name");
        });
    }

    @Test
    public void testColumnNameList() {
        ImmutableList<String> names = dataset.columnNames();
        assertNotNull(names);
        assertEquals(4, names.size());
        assertEquals("id", names.get(0));
        assertEquals("name", names.get(1));
        assertEquals("age", names.get(2));
        assertEquals("salary", names.get(3));
    }

    @Test
    public void testJoinValidatesNewColumnArgumentsBeforeLookingForMatches() {
        final RowDataset rightWithoutMatches = new RowDataset(new ArrayList<>(Arrays.asList("id")),
                new ArrayList<>(Arrays.asList(new ArrayList<>(Arrays.asList(100)))));
        final Map<String, String> onColumns = CommonUtil.asMap("id", "id");

        assertThrows(IllegalArgumentException.class, () -> dataset.innerJoin(rightWithoutMatches, onColumns, "joined", String.class));
        assertThrows(IllegalArgumentException.class, () -> dataset.innerJoin(rightWithoutMatches, onColumns, "", Object[].class));
        assertThrows(IllegalArgumentException.class, () -> dataset.innerJoin(rightWithoutMatches, onColumns, "joined", null));
    }

    @Test
    public void testComplexJoinScenarios() {
        final RowDataset dataset = createFiveRowCityDataset();
        Map<String, String> onColumns = new HashMap<>();
        onColumns.put("city", "city");

        Dataset selfJoined = dataset.innerJoin(dataset, onColumns);
        assertNotNull(selfJoined);
        assertTrue(selfJoined.size() > 0);

        List<String> rightColumns = CommonUtil.toList("city", "data");
        List<List<Object>> rightValues = new ArrayList<>();
        rightValues.add(CommonUtil.toList("Paris", "London"));
        rightValues.add(CommonUtil.toList("data1", "data2"));

        RowDataset noMatchRight = new RowDataset(rightColumns, rightValues);
        Dataset noMatchJoined = dataset.innerJoin(noMatchRight, "city", "city");
        assertEquals(0, noMatchJoined.size());

        Dataset leftJoinNoMatch = dataset.leftJoin(noMatchRight, "city", "city");
        assertEquals(5, leftJoinNoMatch.size());
    }

    @Test
    public void testInvalidColumnOperations() {
        assertThrows(IllegalArgumentException.class, () -> {
            ds1.rightJoin(ds2, "invalid_column", "id");
        });

        assertThrows(IllegalArgumentException.class, () -> {
            ds1.rightJoin(ds2, "id", "invalid_column");
        });

        Map<String, String> invalidMap = new HashMap<>();
        invalidMap.put("invalid_column", "id");

        assertThrows(IllegalArgumentException.class, () -> {
            ds1.rightJoin(ds2, invalidMap);
        });
    }

}
