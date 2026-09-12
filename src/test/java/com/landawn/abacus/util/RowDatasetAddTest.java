package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.NoCachingNoUpdating.DisposableObjArray;
import com.landawn.abacus.util.Tuple.Tuple2;
import com.landawn.abacus.util.Tuple.Tuple3;
import com.landawn.abacus.util.function.BiFunction;
import com.landawn.abacus.util.function.Function;
import com.landawn.abacus.util.function.TriFunction;

public class RowDatasetAddTest extends RowDatasetTestSupport {
    @Test
    public void testAddColumn() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        List<String> dept = Arrays.asList("IT", "HR", "Finance", "Sales", "IT");
        ds.addColumn("department", dept);
        assertEquals(5, ds.columnCount());
        assertTrue(ds.containsColumn("department"));
        assertEquals("IT", ds.get(0, 4));
    }

    @Test
    public void testAddColumnAtPosition() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        List<String> dept = Arrays.asList("IT", "HR", "Finance", "Sales", "IT");
        ds.addColumn(1, "department", dept);
        assertEquals(5, ds.columnCount());
        assertEquals("department", ds.getColumnName(1));
        assertEquals("IT", ds.get(0, 1));
    }

    @Test
    public void testAddColumnWithFunction() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        ds.addColumn("age_plus_10", "age", (Integer age) -> age + 10);
        assertEquals(5, ds.columnCount());
        assertEquals(35, (Integer) ds.get(0, 4));
    }

    @Test
    public void testAddColumnWithBiFunction() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        ds.addColumn("name_age", Tuple.of("name", "age"), (String name, Integer age) -> name + ":" + age);
        assertEquals(5, ds.columnCount());
        assertEquals("Alice:25", ds.get(0, 4));
    }

    @Test
    public void testAddColumnWithTriFunction() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        ds.addColumn("combined", Tuple.of("id", "name", "age"), (Integer id, String name, Integer age) -> id + "-" + name + "-" + age);
        assertEquals(5, ds.columnCount());
        assertEquals("1-Alice-25", ds.get(0, 4));
    }

    @Test
    public void testAddColumnWithTuple2() {
        final RowDataset dataset = createThreeRowScoreDataset();
        dataset.addColumn("name_age", new Tuple2<>("name", "age"), (BiFunction<String, Integer, String>) (name, age) -> name + "-" + age);
        Assertions.assertEquals(5, dataset.columnCount());
        Assertions.assertEquals("John-25", dataset.get(0, 4));
    }

    @Test
    public void testAddColumnWithTuple3() {
        final RowDataset dataset = createThreeRowScoreDataset();
        dataset.addColumn("summary", new Tuple3<>("id", "name", "age"),
                (TriFunction<Integer, String, Integer, String>) (id, name, age) -> "ID:" + id + ",Name:" + name + ",Age:" + age);
        Assertions.assertEquals(5, dataset.columnCount());
        Assertions.assertEquals("ID:1,Name:John,Age:25", dataset.get(0, 4));
    }

    @Test
    public void testAddColumnAtPositionWithTuple2() {
        dataset.addColumn(0, "combined", Tuple.of("id", "name"), (BiFunction<Object, Object, Object>) (id, name) -> id + "-" + name);
        assertEquals("combined", dataset.getColumnName(0));
        assertEquals("1-Alice", dataset.get(0, 0));
    }

    @Test
    public void testAddColumnAtPositionWithTuple3() {
        dataset.addColumn(0, "info", Tuple.of("id", "name", "age"),
                (TriFunction<Object, Object, Object, Object>) (id, name, age) -> id + "/" + name + "/" + age);
        assertEquals("info", dataset.getColumnName(0));
        assertEquals("1/Alice/25", dataset.get(0, 0));
    }

    @Test
    public void testAddColumnEmpty() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        ds.addColumn("newCol", new ArrayList<>());
        assertEquals(5, ds.columnCount());
        assertNull(ds.get(0, 4));
    }

    @Test
    public void testAddColumnWithEmptyCollection() {
        final RowDataset dataset = createThreeRowScoreDataset();
        dataset.addColumn("empty", Collections.emptyList());
        Assertions.assertEquals(5, dataset.columnCount());
        Assertions.assertTrue(dataset.containsColumn("empty"));
        Assertions.assertNull(dataset.get(0, 4));
    }

    @Test
    public void testAddColumnWithMultipleColumnsAndFunction() {
        final RowDataset dataset = createThreeRowScoreDataset();
        dataset.addColumn("full_info", Arrays.asList("name", "age"), (Function<DisposableObjArray, String>) arr -> arr.get(0) + " (" + arr.get(1) + ")");
        Assertions.assertEquals(5, dataset.columnCount());
        Assertions.assertTrue(dataset.containsColumn("full_info"));
        Assertions.assertEquals("John (25)", dataset.get(0, 4));
    }

    @Test
    public void testAddColumnAtPositionWithFunction() {
        dataset.addColumn(1, "nameUpper", "name", (Function<Object, Object>) n -> ((String) n).toUpperCase());
        assertEquals("nameUpper", dataset.getColumnName(1));
        assertEquals("ALICE", dataset.get(0, 1));
    }

    @Test
    public void testAddColumnAtPositionWithMultipleColumns() {
        dataset.addColumn(0, "idPlusAge", Arrays.asList("id", "age"),
                (Function<NoCachingNoUpdating.DisposableObjArray, Object>) arr -> (Integer) arr.get(0) + (Integer) arr.get(1));
        assertEquals("idPlusAge", dataset.getColumnName(0));
        assertEquals(26, (int) dataset.get(0, 0)); // id=1, age=25
    }

    @Test
    public void testAddColumnWrongSize() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        List<String> dept = Arrays.asList("IT", "HR");
        assertThrows(IllegalArgumentException.class, () -> {
            ds.addColumn("department", dept);
        });
    }

    @Test
    public void testAddColumnDuplicateName() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        List<Integer> values = Arrays.asList(1, 2, 3, 4, 5);
        assertThrows(IllegalArgumentException.class, () -> {
            ds.addColumn("id", values);
        });
    }

    @Test
    public void testAddColumnWithWrongSize() {
        final RowDataset dataset = createThreeRowScoreDataset();
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            dataset.addColumn("bad", Arrays.asList("One", "Two"));
        });
    }

    // ========== addColumn - duplicate name ==========

    @Test
    public void testAddColumn_WithPosition_DuplicateName_ThrowsIllegalArgument() {
        RowDataset ds = new RowDataset(new ArrayList<>(Arrays.asList("id", "name")),
                new ArrayList<>(Arrays.asList(new ArrayList<>(Arrays.asList(1)), new ArrayList<>(Arrays.asList("A")))));
        // addColumn(int pos, String newName, String fromColName, Function func)
        assertThrows(IllegalArgumentException.class, () -> ds.addColumn(0, "name", "id", (Function<Object, Object>) v -> v));
    }

    @Test
    public void testAddColumn_WithPosition_MultiFromCols_DuplicateName_ThrowsIllegalArgument() {
        RowDataset ds = new RowDataset(new ArrayList<>(Arrays.asList("id", "name")),
                new ArrayList<>(Arrays.asList(new ArrayList<>(Arrays.asList(1)), new ArrayList<>(Arrays.asList("A")))));
        // addColumn(int pos, String newName, Collection fromColNames, Function func)
        assertThrows(IllegalArgumentException.class,
                () -> ds.addColumn(0, "name", Arrays.asList("id"), (Function<DisposableObjArray, Object>) arr -> arr.get(0)));
    }

    @Test
    public void testAddColumns() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        List<String> depts = Arrays.asList("IT", "HR", "Finance", "Sales", "IT");
        List<Boolean> active = Arrays.asList(true, true, false, true, false);
        ds.addColumns(Arrays.asList("department", "active"), Arrays.asList(depts, active));
        assertEquals(6, ds.columnCount());
        assertEquals("IT", ds.get(0, 4));
        assertEquals(true, ds.get(0, 5));
    }

    @Test
    public void testAddColumnsAtPosition() {
        int colCountBefore = dataset.columnCount();
        dataset.addColumns(1, Arrays.asList("newCol1", "newCol2"),
                Arrays.asList(new ArrayList<>(Arrays.asList("a", "b", "c", "d", "e")), new ArrayList<>(Arrays.asList("x", "y", "z", "w", "v"))));
        assertEquals(colCountBefore + 2, dataset.columnCount());
        assertEquals("newCol1", dataset.getColumnName(1));
        assertEquals("newCol2", dataset.getColumnName(2));
    }

    @Test
    public void testAddColumns_DuplicateNewColumnNames() {
        final List<String> cols = new ArrayList<>(Arrays.asList("id"));
        final List<List<Object>> data = new ArrayList<>();
        data.add(new ArrayList<>(Arrays.asList(1, 2, 3)));
        final RowDataset ds = new RowDataset(cols, data);

        final List<String> newNames = Arrays.asList("dup", "dup");
        final List<Collection<?>> newCols = new ArrayList<>();
        newCols.add(Arrays.asList("a", "b", "c"));
        newCols.add(Arrays.asList("x", "y", "z"));

        assertThrows(IllegalArgumentException.class, () -> ds.addColumns(newNames, newCols));

        assertEquals(1, ds.columnCount());
    }

    @Test
    public void testAddRowAsArray() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        Object[] newRow = new Object[] { 6, "Frank", 40, 80000.0 };
        ds.addRow(newRow);
        assertEquals(6, ds.size());
        assertEquals(6, (Integer) ds.get(5, 0));
        assertEquals("Frank", ds.get(5, 1));
    }

    @Test
    public void testAddRowAsList() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        List<Object> newRow = Arrays.asList(6, "Frank", 40, 80000.0);
        ds.addRow(newRow);
        assertEquals(6, ds.size());
        assertEquals(6, (Integer) ds.get(5, 0));
    }

    @Test
    public void testAddRowAsMap() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        Map<String, Object> newRow = new LinkedHashMap<>();
        newRow.put("id", 6);
        newRow.put("name", "Frank");
        newRow.put("age", 40);
        newRow.put("salary", 80000.0);
        ds.addRow(newRow);
        assertEquals(6, ds.size());
        assertEquals(6, (Integer) ds.get(5, 0));
    }

    @Test
    public void testAddRowAtPosition() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        Object[] newRow = new Object[] { 6, "Frank", 40, 80000.0 };
        ds.addRow(0, newRow);
        assertEquals(6, ds.size());
        assertEquals(6, (Integer) ds.get(0, 0));
        assertEquals("Frank", ds.get(0, 1));
    }

    @Test
    public void testAddRowWithArray() {
        final RowDataset dataset = createThreeRowScoreDataset();
        dataset.addRow(new Object[] { 4, "Alice", 28, 92.0 });
        Assertions.assertEquals(4, dataset.size());
        Assertions.assertEquals(4, (Integer) dataset.get(3, 0));
        Assertions.assertEquals("Alice", dataset.get(3, 1));
    }

    @Test
    public void testAddRowWithList() {
        final RowDataset dataset = createThreeRowScoreDataset();
        dataset.addRow(Arrays.asList(4, "Alice", 28, 92.0));
        Assertions.assertEquals(4, dataset.size());
        Assertions.assertEquals("Alice", dataset.get(3, 1));
    }

    @Test
    public void testAddRowWithMap() {
        final RowDataset dataset = createThreeRowScoreDataset();
        Map<String, Object> row = new HashMap<>();
        row.put("id", 4);
        row.put("name", "Alice");
        row.put("age", 28);
        row.put("score", 92.0);

        dataset.addRow(row);
        Assertions.assertEquals(4, dataset.size());
        Assertions.assertEquals("Alice", dataset.get(3, 1));
    }

    @Test
    public void testAddRowWithBean() {
        final RowDataset dataset = createThreeRowScoreDataset();
        TestBean bean = new TestBean();
        bean.id = 4;
        bean.name = "Alice";
        bean.age = 28;
        bean.score = 92.0;

        dataset.addRow(bean);
        Assertions.assertEquals(4, dataset.size());
        Assertions.assertEquals("Alice", dataset.get(3, 1));
    }

    @Test
    public void testAddRow_MapInsertAtPosition() {
        Map<String, Object> newRow = new LinkedHashMap<>();
        newRow.put("id", 99);
        newRow.put("name", "Inserted");
        newRow.put("age", 41);
        newRow.put("salary", 91000.0);

        dataset.addRow(1, newRow);

        assertEquals(6, dataset.size());
        assertEquals(Integer.valueOf(99), dataset.get(1, 0));
        assertEquals("Inserted", dataset.get(1, 1));
        assertEquals(91000.0, dataset.get(1, 3));
    }

    @Test
    public void testAddRow_BeanInsertAtPosition() {
        dataset.addRow(2, new SalaryRowBean(77, "Bean", 36, 88000.0));

        assertEquals(6, dataset.size());
        assertEquals(Integer.valueOf(77), dataset.get(2, 0));
        assertEquals("Bean", dataset.get(2, 1));
        assertEquals(88000.0, dataset.get(2, 3));
    }

    // ===== addRow at position =====

    @Test
    public void testAddRow_AtMiddlePosition_AsArray() {
        RowDataset ds = new RowDataset(new ArrayList<>(Arrays.asList("id", "name")),
                new ArrayList<>(Arrays.asList(new ArrayList<>(Arrays.asList(1, 3)), new ArrayList<>(Arrays.asList("A", "C")))));

        ds.addRow(1, new Object[] { 2, "B" });

        assertEquals(3, ds.size());
        assertEquals(2, (Integer) ds.get(1, 0));
        assertEquals("B", ds.get(1, 1));
    }

    @Test
    public void testAddRow_AtMiddlePosition_AsMap() {
        RowDataset ds = new RowDataset(new ArrayList<>(Arrays.asList("id", "name")),
                new ArrayList<>(Arrays.asList(new ArrayList<>(Arrays.asList(1, 3)), new ArrayList<>(Arrays.asList("A", "C")))));

        Map<String, Object> row = new HashMap<>();
        row.put("id", 2);
        row.put("name", "B");
        ds.addRow(1, row);

        assertEquals(3, ds.size());
        assertEquals(2, (Integer) ds.get(1, 0));
    }

    @Test
    public void testAddRowWrongSize() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        Object[] newRow = new Object[] { 6, "Frank" };
        assertThrows(IllegalArgumentException.class, () -> {
            ds.addRow(newRow);
        });
    }

    @Test
    public void testAddRow_UnsupportedRowType() {
        assertThrows(IllegalArgumentException.class, () -> dataset.addRow(0, 123));
    }

    @Test
    public void testAddRow_positionalRowMustMatchColumnCountExactly() {
        final Dataset ds = twoColumnDataset();

        // Too long: used to be silently truncated to the first columnCount() values.
        assertThrows(IllegalArgumentException.class, () -> ds.addRow(new Object[] { 4, "d", "dropped" }));
        assertThrows(IllegalArgumentException.class, () -> ds.addRow(Arrays.asList(4, "d", "dropped")));

        // Too short: rejected before and after.
        assertThrows(IllegalArgumentException.class, () -> ds.addRow(new Object[] { 4 }));
        assertThrows(IllegalArgumentException.class, () -> ds.addRow(Arrays.asList(4)));

        assertEquals(3, ds.size());

        // Exact length still works, for both supported positional shapes.
        ds.addRow(new Object[] { 4, "d" });
        ds.addRow(Arrays.asList(5, "e"));
        assertEquals(5, ds.size());
        assertEquals("d", ds.get(3, 1));
        assertEquals("e", ds.get(4, 1));
    }

    @Test
    public void testAddRow_mapAndBeanRowsUnaffectedByTheExactLengthRule() {
        final Dataset ds = twoColumnDataset();

        // Map/bean rows are matched by name, so extra entries remain harmless.
        final Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", 4);
        row.put("name", "d");
        row.put("unrelated", "ignored");

        ds.addRow(row);
        assertEquals(4, ds.size());
        assertEquals("d", ds.get(3, 1));

        // ...but a missing column is still an error.
        assertThrows(IllegalArgumentException.class, () -> ds.addRow(CommonUtil.asMap("id", 5)));
    }

    @Test
    public void testAddRows() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        List<Object[]> newRows = Arrays.asList(new Object[] { 6, "Frank", 40, 80000.0 }, new Object[] { 7, "Grace", 45, 90000.0 });
        ds.addRows(newRows);
        assertEquals(7, ds.size());
        assertEquals(6, (Integer) ds.get(5, 0));
        assertEquals(7, (Integer) ds.get(6, 0));
    }

    @Test
    public void testAddRowsAtPosition() {
        int sizeBefore = dataset.size();
        List<Object[]> newRows = Arrays.asList(new Object[] { 10, "NewPerson1", 50, 90000.0 }, new Object[] { 11, "NewPerson2", 55, 95000.0 });
        dataset.addRows(1, newRows);
        assertEquals(sizeBefore + 2, dataset.size());
        assertEquals(10, (int) dataset.get(1, 0));
        assertEquals(11, (int) dataset.get(2, 0));
    }

    @Test
    public void testAddRows_AtPosition_Array() {
        RowDataset ds = new RowDataset(new ArrayList<>(Arrays.asList("id", "name")),
                new ArrayList<>(Arrays.asList(new ArrayList<>(Arrays.asList(1, 2)), new ArrayList<>(Arrays.asList("A", "B")))));

        List<Object[]> newRows = new ArrayList<>();
        newRows.add(new Object[] { 10, "X" });
        newRows.add(new Object[] { 20, "Y" });

        ds.addRows(1, newRows);

        assertEquals(4, ds.size());
        assertEquals(10, (Integer) ds.get(1, 0));
        assertEquals(20, (Integer) ds.get(2, 0));
    }

    @Test
    public void testAddRows_AtPosition_List() {
        RowDataset ds = new RowDataset(new ArrayList<>(Arrays.asList("id", "name")),
                new ArrayList<>(Arrays.asList(new ArrayList<>(Arrays.asList(1, 2)), new ArrayList<>(Arrays.asList("A", "B")))));

        List<List<Object>> newRows = new ArrayList<>();
        newRows.add(Arrays.asList(10, "X"));
        newRows.add(Arrays.asList(20, "Y"));

        ds.addRows(0, newRows);

        assertEquals(4, ds.size());
        assertEquals(10, (Integer) ds.get(0, 0));
        assertEquals(20, (Integer) ds.get(1, 0));
    }

    @Test
    public void testAddRows_AtPosition_Map() {
        RowDataset ds = new RowDataset(new ArrayList<>(Arrays.asList("id", "name")),
                new ArrayList<>(Arrays.asList(new ArrayList<>(Arrays.asList(1)), new ArrayList<>(Arrays.asList("A")))));

        List<Map<String, Object>> newRows = new ArrayList<>();
        Map<String, Object> row1 = new LinkedHashMap<>();
        row1.put("id", 10);
        row1.put("name", "X");
        Map<String, Object> row2 = new LinkedHashMap<>();
        row2.put("id", 20);
        row2.put("name", "Y");
        newRows.add(row1);
        newRows.add(row2);

        ds.addRows(ds.size(), newRows);

        assertEquals(3, ds.size());
        assertEquals(10, (Integer) ds.get(1, 0));
    }

    // ===== addRows at position with multiple rows =====

    @Test
    public void testAddRows_MultipleAtBeginning_AsBean() {
        List<String> colNames = new ArrayList<>(Arrays.asList("id", "name", "age", "city"));
        List<List<Object>> cols = new ArrayList<>();
        cols.add(new ArrayList<>(Arrays.asList(10)));
        cols.add(new ArrayList<>(Arrays.asList("Eve")));
        cols.add(new ArrayList<>(Arrays.asList(22)));
        cols.add(new ArrayList<>(Arrays.asList("Boston")));
        RowDataset ds = new RowDataset(colNames, cols);

        List<Person> newRows = new ArrayList<>();
        newRows.add(new Person(1, "Alice", 25, "NYC"));
        newRows.add(new Person(2, "Bob", 30, "LA"));

        ds.addRows(0, newRows);

        assertEquals(3, ds.size());
        assertEquals(1, (Integer) ds.get(0, 0));
        assertEquals(2, (Integer) ds.get(1, 0));
    }

    @Test
    public void testAddRows_NullElement_ThrowsIllegalArgument() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        int sizeBefore = ds.size();

        List<Object> newRows = new ArrayList<>();
        newRows.add(new Object[] { 6, "Frank", 40, 80000.0 });
        newRows.add(null);

        assertThrows(IllegalArgumentException.class, () -> ds.addRows(newRows));
        assertThrows(IllegalArgumentException.class, () -> ds.addRows(0, newRows));

        // The failed calls must not have partially added any rows.
        assertEquals(sizeBefore, ds.size());
    }

    @Test
    public void testAddRows_MixedSupportedRepresentations() {
        final RowDataset ds = new RowDataset(new ArrayList<>(Arrays.asList("id", "name", "age", "city")),
                new ArrayList<>(Arrays.asList(new ArrayList<>(Arrays.asList(0)), new ArrayList<>(Arrays.asList("Initial")), new ArrayList<>(Arrays.asList(20)),
                        new ArrayList<>(Arrays.asList("Seattle")))));

        final Map<String, Object> mapRow = new LinkedHashMap<>();
        mapRow.put("id", 3);
        mapRow.put("name", "Map");
        mapRow.put("age", 32);
        mapRow.put("city", "Chicago");

        final List<Object> rows = Arrays.asList(new Object[] { 1, "Array", 30, "New York" }, Arrays.asList(2, "List", 31, "Boston"), mapRow,
                new Person(4, "Bean", 33, "Austin"));

        ds.addRows(1, rows);

        assertEquals(5, ds.size());
        assertEquals(Arrays.asList(0, 1, 2, 3, 4), ds.getColumn("id"));
        assertEquals(Arrays.asList("Initial", "Array", "List", "Map", "Bean"), ds.getColumn("name"));
        assertEquals(Arrays.asList("Seattle", "New York", "Boston", "Chicago", "Austin"), ds.getColumn("city"));
    }

    @Test
    public void testAddRows_ExtractionFailureDoesNotPartiallyMutateColumns() {
        final RowDataset ds = new RowDataset(new ArrayList<>(Arrays.asList("id", "name")),
                new ArrayList<>(Arrays.asList(new ArrayList<>(Arrays.asList(1, 2)), new ArrayList<>(Arrays.asList("A", "B")))));
        final List<Object> throwingRow = new AbstractList<>() {
            @Override
            public Object get(final int index) {
                if (index == 1) {
                    throw new IllegalStateException("name extraction failed");
                }

                return 20;
            }

            @Override
            public int size() {
                return 2;
            }
        };

        assertThrows(IllegalStateException.class, () -> ds.addRows(Arrays.asList(Arrays.asList(10, "X"), throwingRow)));

        assertEquals(2, ds.size());
        assertEquals(Arrays.asList(1, 2), ds.getColumn("id"));
        assertEquals(Arrays.asList("A", "B"), ds.getColumn("name"));
    }

    @Test
    public void testAddRows_ObjectArrayCollectionWithShortRow() {
        final RowDataset ds = new RowDataset(new ArrayList<>(Arrays.asList("id", "name")),
                new ArrayList<>(Arrays.asList(new ArrayList<>(Arrays.asList(1)), new ArrayList<>(Arrays.asList("a")))));

        ds.addRows(1, Arrays.asList(new Object[] { 2, "b" }, new Object[] { 3, "c" }));

        assertEquals(3, ds.size());
        assertEquals(Arrays.asList(1, 2, 3), ds.getColumn("id"));
        assertEquals(Arrays.asList("a", "b", "c"), ds.getColumn("name"));
        assertThrows(IllegalArgumentException.class, () -> ds.addRows(Arrays.asList(new Object[] { 4 })));
    }

    @Test
    public void testAddRows_positionalRowMustMatchColumnCountExactly() {
        final Dataset ds = twoColumnDataset();

        assertThrows(IllegalArgumentException.class, () -> ds.addRows(Arrays.asList(new Object[] { 4, "d", "dropped" })));
        // The batch is normalized before anything is appended, so a bad row leaves the Dataset untouched.
        assertEquals(3, ds.size());

        // A valid row followed by an invalid one must not half-apply either.
        assertThrows(IllegalArgumentException.class, () -> ds.addRows(Arrays.asList(new Object[] { 4, "d" }, new Object[] { 5 })));
        assertEquals(3, ds.size());
    }

}
