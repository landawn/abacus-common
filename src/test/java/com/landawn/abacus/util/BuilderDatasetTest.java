package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.Builder.DatasetBuilder;

public class BuilderDatasetTest extends BuilderTestSupport {
    @Test
    public void testDatasetBuilder_renameColumn_returnsBuilder() {
        Dataset ds = CommonUtil.newDataset(Arrays.asList("col1"), Arrays.asList(Arrays.asList(1)));
        DatasetBuilder b = Builder.of(ds);
        assertSame(b, b.renameColumn("col1", "col1_new"));
    }

    @Test
    public void testDatasetBuilder_renameColumn() {
        Dataset dataset = CommonUtil.newDataset(Arrays.asList("col1", "col2"), Arrays.asList(Arrays.asList(1, 2)));
        DatasetBuilder builder = Builder.of(dataset);
        builder.renameColumn("col1", "newCol1");
        assertTrue(builder.val().columnNames().contains("newCol1"));
    }

    @Test
    public void testDatasetBuilder_renameColumnsMap() {
        Dataset dataset = CommonUtil.newDataset(Arrays.asList("col1", "col2"), Arrays.asList(Arrays.asList(1, 2)));
        Map<String, String> renames = new HashMap<>();
        renames.put("col1", "newCol1");
        renames.put("col2", "newCol2");
        DatasetBuilder builder = Builder.of(dataset);
        builder.renameColumns(renames);
        assertTrue(builder.val().columnNames().contains("newCol1"));
        assertTrue(builder.val().columnNames().contains("newCol2"));
    }

    @Test
    public void testDatasetBuilder_renameColumnsFunction() {
        Dataset dataset = CommonUtil.newDataset(Arrays.asList("col1", "col2"), Arrays.asList(Arrays.asList(1, 2)));
        DatasetBuilder builder = Builder.of(dataset);
        builder.renameColumns(name -> "prefix_" + name);
        assertTrue(builder.val().columnNames().contains("prefix_col1"));
    }

    @Test
    public void testDatasetBuilder_addColumn() {
        Dataset dataset = CommonUtil.newDataset(Arrays.asList("col1"), Arrays.asList(Arrays.asList(1), Arrays.asList(2)));
        DatasetBuilder builder = Builder.of(dataset);
        builder.addColumn("col2", Arrays.asList(3, 4));
        assertEquals(2, builder.val().columnNames().size());
    }

    @Test
    public void testDatasetBuilder_addColumnAtIndex() {
        Dataset dataset = CommonUtil.newDataset(Arrays.asList("col1"), Arrays.asList(Arrays.asList(1), Arrays.asList(2)));
        DatasetBuilder builder = Builder.of(dataset);
        builder.addColumn(0, "col0", Arrays.asList(0, 0));
        assertEquals("col0", builder.val().columnNames().get(0));
    }

    @Test
    public void testDatasetBuilder_removeColumn() {
        Dataset dataset = CommonUtil.newDataset(Arrays.asList("col1", "col2"), Arrays.asList(Arrays.asList(1, 2)));
        DatasetBuilder builder = Builder.of(dataset);
        builder.removeColumn("col2");
        assertEquals(1, builder.val().columnNames().size());
    }

    @Test
    public void testDatasetBuilder_removeColumnsCollection() {
        Dataset dataset = CommonUtil.newDataset(Arrays.asList("col1", "col2", "col3"), Arrays.asList(Arrays.asList(1, 2, 3)));
        DatasetBuilder builder = Builder.of(dataset);
        builder.removeColumns(Arrays.asList("col2", "col3"));
        assertEquals(1, builder.val().columnNames().size());
    }

    @Test
    public void testDatasetBuilder_removeColumnsPredicate() {
        Dataset dataset = CommonUtil.newDataset(Arrays.asList("col1", "temp1", "temp2"), Arrays.asList(Arrays.asList(1, 2, 3)));
        DatasetBuilder builder = Builder.of(dataset);
        builder.removeColumns(name -> name.startsWith("temp"));
        assertEquals(1, builder.val().columnNames().size());
    }

    @Test
    public void testDatasetBuilder_prepend() {
        Dataset dataset1 = CommonUtil.newDataset(Arrays.asList("col1"), Arrays.asList(Arrays.asList(1)));
        Dataset dataset2 = CommonUtil.newDataset(Arrays.asList("col1"), Arrays.asList(Arrays.asList(2)));
        DatasetBuilder builder = Builder.of(dataset1);
        builder.prepend(dataset2);
        assertEquals(2, builder.val().size());
    }

    @Test
    public void testDatasetBuilder_append() {
        Dataset dataset1 = CommonUtil.newDataset(Arrays.asList("col1"), Arrays.asList(Arrays.asList(1)));
        Dataset dataset2 = CommonUtil.newDataset(Arrays.asList("col1"), Arrays.asList(Arrays.asList(2)));
        DatasetBuilder builder = Builder.of(dataset1);
        builder.append(dataset2);
        assertEquals(2, builder.val().size());
    }

    @Test
    public void testDatasetBuilder_addColumn_withFunction() {
        Dataset dataset = CommonUtil.newDataset(Arrays.asList("col1"), Arrays.asList(Arrays.asList(1), Arrays.asList(2)));
        DatasetBuilder builder = Builder.of(dataset);
        builder.addColumn("col2", "col1", (Integer val) -> val * 2);
        assertEquals(2, builder.val().columnNames().size());
    }

    @Test
    public void testDatasetBuilder_addColumn_atIndex_withFunction() {
        Dataset dataset = CommonUtil.newDataset(Arrays.asList("col1"), Arrays.asList(Arrays.asList(1), Arrays.asList(2)));
        DatasetBuilder builder = Builder.of(dataset);
        builder.addColumn(0, "col0", "col1", (Integer val) -> val * 10);
        assertEquals("col0", builder.val().columnNames().get(0));
    }

    @Test
    public void testDatasetBuilder_addColumn_withMultiColumnFunction() {
        Dataset dataset = CommonUtil.newDataset(Arrays.asList("col1", "col2"), Arrays.asList(Arrays.asList(1, 2), Arrays.asList(3, 4)));
        DatasetBuilder builder = Builder.of(dataset);
        builder.addColumn("sum", Arrays.asList("col1", "col2"), arr -> {
            return ((Integer) arr.get(0)) + ((Integer) arr.get(1));
        });
        assertEquals(3, builder.val().columnNames().size());
    }

    @Test
    public void testDatasetBuilder_addColumn_atIndex_withMultiColumnFunction() {
        Dataset dataset = CommonUtil.newDataset(Arrays.asList("col1", "col2"), Arrays.asList(Arrays.asList(1, 2), Arrays.asList(3, 4)));
        DatasetBuilder builder = Builder.of(dataset);
        builder.addColumn(0, "sum", Arrays.asList("col1", "col2"), arr -> {
            return ((Integer) arr.get(0)) + ((Integer) arr.get(1));
        });
        assertEquals("sum", builder.val().columnNames().get(0));
    }

    @Test
    public void testDatasetBuilder_addColumn_withBiFunction() {
        Dataset dataset = CommonUtil.newDataset(Arrays.asList("col1", "col2"), Arrays.asList(Arrays.asList(1, 2), Arrays.asList(3, 4)));
        DatasetBuilder builder = Builder.of(dataset);
        builder.addColumn("product", Tuple.of("col1", "col2"), (Integer a, Integer b) -> a * b);
        assertEquals(3, builder.val().columnNames().size());
    }

    @Test
    public void testDatasetBuilder_addColumn_atIndex_withBiFunction() {
        Dataset dataset = CommonUtil.newDataset(Arrays.asList("col1", "col2"), Arrays.asList(Arrays.asList(1, 2), Arrays.asList(3, 4)));
        DatasetBuilder builder = Builder.of(dataset);
        builder.addColumn(0, "product", Tuple.of("col1", "col2"), (Integer a, Integer b) -> a * b);
        assertEquals("product", builder.val().columnNames().get(0));
    }

    @Test
    public void testDatasetBuilder_addColumn_withTriFunction() {
        Dataset dataset = CommonUtil.newDataset(Arrays.asList("a", "b", "c"), Arrays.asList(Arrays.asList(1, 2, 3), Arrays.asList(4, 5, 6)));
        DatasetBuilder builder = Builder.of(dataset);
        builder.addColumn("sum", Tuple.of("a", "b", "c"), (Integer x, Integer y, Integer z) -> x + y + z);
        assertEquals(4, builder.val().columnNames().size());
    }

    @Test
    public void testDatasetBuilder_addColumn_atIndex_withTriFunction() {
        Dataset dataset = CommonUtil.newDataset(Arrays.asList("a", "b", "c"), Arrays.asList(Arrays.asList(1, 2, 3), Arrays.asList(4, 5, 6)));
        DatasetBuilder builder = Builder.of(dataset);
        builder.addColumn(0, "sum", Tuple.of("a", "b", "c"), (Integer x, Integer y, Integer z) -> x + y + z);
        assertEquals("sum", builder.val().columnNames().get(0));
    }

    @Test
    public void testDatasetBuilder_updateColumn() {
        Dataset dataset = CommonUtil.newDataset(Arrays.asList("col1"), Arrays.asList(Arrays.asList(1), Arrays.asList(2)));
        DatasetBuilder builder = Builder.of(dataset);
        builder.updateColumn("col1", (Integer val) -> val * 2);
        assertEquals(2, (Integer) builder.val().moveToRow(0).get("col1"));
    }

    @Test
    public void testDatasetBuilder_updateColumns() {
        Dataset dataset = CommonUtil.newDataset(Arrays.asList("col1", "col2"), Arrays.asList(Arrays.asList(1, 2), Arrays.asList(3, 4)));
        DatasetBuilder builder = Builder.of(dataset);
        builder.updateColumns(Arrays.asList("col1", "col2"), (rowIdx, colName, val) -> ((Integer) val) * 10);
        assertEquals(10, (Integer) builder.val().moveToRow(0).get("col1"));
    }

    @Test
    public void testDatasetBuilder_convertColumn() {
        Dataset dataset = CommonUtil.newDataset(Arrays.asList("col1"), Arrays.asList(Arrays.asList("123")));
        DatasetBuilder builder = Builder.of(dataset);
        builder.convertColumn("col1", Integer.class);
        assertTrue(builder.val().moveToRow(0).get("col1") instanceof Integer);
    }

    @Test
    public void testDatasetBuilder_convertColumns() {
        Dataset dataset = CommonUtil.newDataset(Arrays.asList("col1", "col2"), Arrays.asList(Arrays.asList("123", "456")));
        Map<String, Class<?>> conversions = new HashMap<>();
        conversions.put("col1", Integer.class);
        conversions.put("col2", Integer.class);
        DatasetBuilder builder = Builder.of(dataset);
        builder.convertColumns(conversions);
        assertTrue(builder.val().moveToRow(0).get("col1") instanceof Integer);
    }

    @Test
    public void testDatasetBuilder_combineColumns() {
        Dataset dataset = CommonUtil.newDataset(Arrays.asList("a", "b"), Arrays.asList(Arrays.asList(1, 2)));
        DatasetBuilder builder = Builder.of(dataset);
        builder.combineColumns(Arrays.asList("a", "b"), "sum", arr -> {
            return ((Integer) arr.get(0)) + ((Integer) arr.get(1));
        });
        assertTrue(builder.val().columnNames().contains("sum"));
    }

    @Test
    public void testDatasetBuilder_combineColumns_withBiFunction() {
        Dataset dataset = CommonUtil.newDataset(Arrays.asList("a", "b"), Arrays.asList(Arrays.asList(1, 2)));
        DatasetBuilder builder = Builder.of(dataset);
        builder.combineColumns(Tuple.of("a", "b"), "sum", (Integer x, Integer y) -> x + y);
        assertTrue(builder.val().columnNames().contains("sum"));
    }

    @Test
    public void testDatasetBuilder_combineColumns_withTriFunction() {
        Dataset dataset = CommonUtil.newDataset(Arrays.asList("a", "b", "c"), Arrays.asList(Arrays.asList(1, 2, 3)));
        DatasetBuilder builder = Builder.of(dataset);
        builder.combineColumns(Tuple.of("a", "b", "c"), "sum", (Integer x, Integer y, Integer z) -> x + y + z);
        assertTrue(builder.val().columnNames().contains("sum"));
    }

    @Test
    public void testDatasetBuilder_divideColumn() {
        Dataset dataset = CommonUtil.newDataset(Arrays.asList("full"), Arrays.asList(Arrays.asList("a,b")));
        DatasetBuilder builder = Builder.of(dataset);
        builder.divideColumn("full", Arrays.asList("col1", "col2"), (String val) -> Arrays.asList(val.split(",")));
        assertTrue(builder.val().columnNames().contains("col1"));
        assertTrue(builder.val().columnNames().contains("col2"));
    }

    @Test
    public void testDatasetBuilder_divideColumn_withBiConsumer() {
        Dataset dataset = CommonUtil.newDataset(Arrays.asList("full"), Arrays.asList(Arrays.asList("a,b")));
        DatasetBuilder builder = Builder.of(dataset);
        builder.divideColumn("full", Arrays.asList("col1", "col2"), (String val, Object[] output) -> {
            String[] parts = val.split(",");
            output[0] = parts[0];
            output[1] = parts[1];
        });
        assertTrue(builder.val().columnNames().contains("col1"));
    }

    @Test
    public void testDatasetBuilder_divideColumn_withPair() {
        Dataset dataset = CommonUtil.newDataset(Arrays.asList("full"), Arrays.asList(Arrays.asList("a,b")));
        DatasetBuilder builder = Builder.of(dataset);
        builder.divideColumn("full", Tuple.of("col1", "col2"), (String val, Pair<Object, Object> output) -> {
            String[] parts = val.split(",");
            output.setLeft(parts[0]);
            output.setRight(parts[1]);
        });
        assertTrue(builder.val().columnNames().contains("col1"));
    }

    @Test
    public void testDatasetBuilder_divideColumn_withTriple() {
        Dataset dataset = CommonUtil.newDataset(Arrays.asList("full"), Arrays.asList(Arrays.asList("a,b,c")));
        DatasetBuilder builder = Builder.of(dataset);
        builder.divideColumn("full", Tuple.of("col1", "col2", "col3"), (String val, Triple<Object, Object, Object> output) -> {
            String[] parts = val.split(",");
            output.setLeft(parts[0]);
            output.setMiddle(parts[1]);
            output.setRight(parts[2]);
        });
        assertTrue(builder.val().columnNames().contains("col1"));
    }

    @Test
    public void testDatasetBuilder_updateAll() {
        Dataset dataset = CommonUtil.newDataset(Arrays.asList("col1"), Arrays.asList(Arrays.asList(1), Arrays.asList(2)));
        DatasetBuilder builder = Builder.of(dataset);
        builder.updateAll((Integer val) -> val * 2);
        assertEquals(2, (Integer) builder.val().moveToRow(0).get("col1"));
    }

    @Test
    public void testDatasetBuilder_replaceIf() {
        Dataset dataset = CommonUtil.newDataset(Arrays.asList("col1"), Arrays.asList(Arrays.asList(1), Arrays.asList(2)));
        DatasetBuilder builder = Builder.of(dataset);
        builder.replaceIf((Integer val) -> val == 2, 0);
        assertEquals(0, (Integer) builder.val().moveToRow(1).get("col1"));
    }

    @Test
    public void testDatasetBuilder_replaceIf_withPredicate() {
        Dataset dataset = CommonUtil.newDataset(Arrays.asList("col1"), Arrays.asList(Arrays.asList(1), Arrays.asList(2)));
        DatasetBuilder builder = Builder.of(dataset);
        builder.replaceIf((rowIdx, colName, val) -> ((Integer) val) > 1, 99);
        assertEquals(99, (Integer) builder.val().moveToRow(1).get("col1"));
    }

    @Test
    public void testDatasetBuilder() {
        List<String> columnNames = new ArrayList<>(Arrays.asList("ID", "Name"));
        List<List<?>> columns = new ArrayList<>();
        columns.add(new ArrayList<>(Arrays.asList(1, 2)));
        columns.add(new ArrayList<>(Arrays.asList("John", "Jane")));
        Dataset dataset = Dataset.columns(columnNames, columns);
        DatasetBuilder builder = Builder.of(dataset);

        builder.renameColumn("Name", "FullName");
        assertTrue(dataset.columnNames().contains("FullName"));
        assertFalse(dataset.columnNames().contains("Name"));

        builder.addColumn("Age", Arrays.asList(30, 25));
        assertEquals(3, dataset.columnCount());
        assertEquals(Arrays.asList(30, 25), dataset.getColumn("Age"));

        builder.removeColumn("ID");
        assertEquals(2, dataset.columnCount());

        builder.updateColumn("Age", (Integer age) -> age + 1);
        assertEquals(Arrays.asList(31, 26), dataset.getColumn("Age"));
    }

    @Test
    public void testDatasetBuilderRenameColumn() {
        Dataset ds = new RowDataset(Arrays.asList("oldName"), Arrays.asList(Arrays.asList("value")));
        Builder.DatasetBuilder builder = Builder.of(ds);

        builder.renameColumn("oldName", "newName");
        Assertions.assertTrue(ds.columnNames().contains("newName"));
        Assertions.assertFalse(ds.columnNames().contains("oldName"));
    }

    @Test
    public void testDatasetBuilderAddColumn() {
        Dataset ds = new RowDataset(CommonUtil.toList("col1"), CommonUtil.toList(Arrays.asList("val1")));
        Builder.DatasetBuilder builder = Builder.of(ds);

        builder.addColumn("col2", CommonUtil.toList("val2"));
        Assertions.assertEquals(2, ds.columnCount());
        Assertions.assertTrue(ds.columnNames().contains("col2"));
    }

    @Test
    public void testDatasetBuilderRemoveColumn() {
        Dataset ds = new RowDataset(CommonUtil.toList("col1", "col2"), CommonUtil.toList(CommonUtil.toList("val1"), CommonUtil.toList("val2")));
        Builder.DatasetBuilder builder = Builder.of(ds);

        builder.removeColumn("col2");
        Assertions.assertEquals(1, ds.columnCount());
        Assertions.assertFalse(ds.columnNames().contains("col2"));
    }

    @Test
    public void testDatasetBuilderDivideColumn() {
        testDataset.addColumn("fullName", Arrays.asList("John Doe", "Jane Smith", "Bob Jones"));

        DatasetBuilder builder = Builder.of(testDataset);

        Function<Object, List<String>> splitFunc = fullName -> Arrays.asList(fullName.toString().split(" "));

        builder.divideColumn("fullName", Arrays.asList("firstName", "lastName"), splitFunc);

        assertFalse(testDataset.columnNames().contains("fullName"));
        assertTrue(testDataset.columnNames().contains("firstName"));
        assertTrue(testDataset.columnNames().contains("lastName"));
    }

    @Test
    public void testDatasetBuilder_combineColumns_withClass() {
        Dataset dataset = CommonUtil.newDataset(Arrays.asList("a", "b"), Arrays.asList(Arrays.asList("x", "y")));
        DatasetBuilder builder = Builder.of(dataset);
        builder.combineColumns(Arrays.asList("a", "b"), "combined", Object[].class);
        assertTrue(builder.val().columnNames().contains("combined"));
    }

    @Test
    public void testDatasetBuilder_of() {
        Dataset dataset = CommonUtil.newDataset(Arrays.asList("col1", "col2"), Arrays.asList(Arrays.asList(1, 2), Arrays.asList(3, 4)));
        DatasetBuilder builder = Builder.of(dataset);
        assertNotNull(builder);
        assertEquals(2, builder.val().columnNames().size());
    }

    @Test
    public void testDatasetBuilder_renameColumnsCollectionFunction() {
        Dataset dataset = CommonUtil.newDataset(Arrays.asList("col1", "col2"), Arrays.asList(Arrays.asList(1, 2)));
        DatasetBuilder builder = Builder.of(dataset);
        builder.renameColumns(Arrays.asList("col1"), name -> name.toUpperCase());
        assertTrue(builder.val().columnNames().contains("COL1"));
        assertTrue(builder.val().columnNames().contains("col2"));
    }

    @Test
    public void testDatasetBuilder_renameColumnsMap_returnsBuilder() {
        Dataset ds = CommonUtil.newDataset(Arrays.asList("col1"), Arrays.asList(Arrays.asList(1)));
        DatasetBuilder b = Builder.of(ds);
        Map<String, String> renames = new HashMap<>();
        renames.put("col1", "col1_new");
        assertSame(b, b.renameColumns(renames));
    }

    @Test
    public void testDatasetBuilder_renameColumnsCollectionFunction_returnsBuilder() {
        Dataset ds = CommonUtil.newDataset(Arrays.asList("col1"), Arrays.asList(Arrays.asList(1)));
        DatasetBuilder b = Builder.of(ds);
        assertSame(b, b.renameColumns(Arrays.asList("col1"), name -> "new_" + name));
    }

    @Test
    public void testDatasetBuilder_renameColumnsFunction_returnsBuilder() {
        Dataset ds = CommonUtil.newDataset(Arrays.asList("col1"), Arrays.asList(Arrays.asList(1)));
        DatasetBuilder b = Builder.of(ds);
        assertSame(b, b.renameColumns(name -> "prefix_" + name));
    }

    @Test
    public void testDatasetBuilder_addColumn_returnsBuilder() {
        Dataset ds = CommonUtil.newDataset(Arrays.asList("col1"), Arrays.asList(Arrays.asList(1)));
        DatasetBuilder b = Builder.of(ds);
        assertSame(b, b.addColumn("col2", Arrays.asList(2)));
    }

    @Test
    public void testDatasetBuilder_addColumnAtIndex_returnsBuilder() {
        Dataset ds = CommonUtil.newDataset(Arrays.asList("col1"), Arrays.asList(Arrays.asList(1)));
        DatasetBuilder b = Builder.of(ds);
        assertSame(b, b.addColumn(0, "col0", Arrays.asList(0)));
    }

    @Test
    public void testDatasetBuilder_removeColumn_returnsBuilder() {
        Dataset ds = CommonUtil.newDataset(Arrays.asList("col1", "col2"), Arrays.asList(Arrays.asList(1, 2)));
        DatasetBuilder b = Builder.of(ds);
        assertSame(b, b.removeColumn("col2"));
    }

    @Test
    public void testDatasetBuilder_removeColumnsCollection_returnsBuilder() {
        Dataset ds = CommonUtil.newDataset(Arrays.asList("col1", "col2"), Arrays.asList(Arrays.asList(1, 2)));
        DatasetBuilder b = Builder.of(ds);
        assertSame(b, b.removeColumns(Arrays.asList("col2")));
    }

    @Test
    public void testDatasetBuilder_removeColumnsPredicate_returnsBuilder() {
        Dataset ds = CommonUtil.newDataset(Arrays.asList("col1", "temp"), Arrays.asList(Arrays.asList(1, 2)));
        DatasetBuilder b = Builder.of(ds);
        assertSame(b, b.removeColumns(name -> name.startsWith("temp")));
    }

    @Test
    public void testDatasetBuilder_updateColumn_returnsBuilder() {
        Dataset ds = CommonUtil.newDataset(Arrays.asList("col1"), Arrays.asList(Arrays.asList(1)));
        DatasetBuilder b = Builder.of(ds);
        assertSame(b, b.updateColumn("col1", (Integer v) -> v * 2));
    }

    @Test
    public void testDatasetBuilder_convertColumn_returnsBuilder() {
        Dataset ds = CommonUtil.newDataset(Arrays.asList("col1"), Arrays.asList(Arrays.asList("123")));
        DatasetBuilder b = Builder.of(ds);
        assertSame(b, b.convertColumn("col1", Integer.class));
    }

    @Test
    public void testDatasetBuilder_prepend_returnsBuilder() {
        Dataset ds1 = CommonUtil.newDataset(Arrays.asList("col1"), Arrays.asList(Arrays.asList(1)));
        Dataset ds2 = CommonUtil.newDataset(Arrays.asList("col1"), Arrays.asList(Arrays.asList(2)));
        DatasetBuilder b = Builder.of(ds1);
        assertSame(b, b.prepend(ds2));
    }

    @Test
    public void testDatasetBuilder_append_returnsBuilder() {
        Dataset ds1 = CommonUtil.newDataset(Arrays.asList("col1"), Arrays.asList(Arrays.asList(1)));
        Dataset ds2 = CommonUtil.newDataset(Arrays.asList("col1"), Arrays.asList(Arrays.asList(2)));
        DatasetBuilder b = Builder.of(ds1);
        assertSame(b, b.append(ds2));
    }

    @Test
    public void testDatasetBuilder_updateAll_returnsBuilder() {
        Dataset ds = CommonUtil.newDataset(Arrays.asList("col1"), Arrays.asList(Arrays.asList(1)));
        DatasetBuilder b = Builder.of(ds);
        assertSame(b, b.updateAll((Integer v) -> v * 2));
    }

    @Test
    public void testDatasetBuilder_replaceIf_returnsBuilder() {
        Dataset ds = CommonUtil.newDataset(Arrays.asList("col1"), Arrays.asList(Arrays.asList(1)));
        DatasetBuilder b = Builder.of(ds);
        assertSame(b, b.replaceIf((Integer v) -> v == 1, 99));
    }

    @Test
    public void testDatasetBuilder_updateAll_withRowIndexAndColumnName() {
        Dataset dataset = CommonUtil.newDataset(Arrays.asList("col1"), Arrays.asList(Arrays.asList(1), Arrays.asList(2)));
        DatasetBuilder builder = Builder.of(dataset);
        builder.updateAll((rowIdx, colName, val) -> ((Integer) val) + rowIdx);
        assertEquals(1, (Integer) builder.val().moveToRow(0).get("col1"));
        assertEquals(3, (Integer) builder.val().moveToRow(1).get("col1"));
    }

    // ---- G28-001: what addColumn(String, Collection) / addColumn(int, String, Collection) really enforce ----

    @Test
    public void testDatasetBuilder_addColumn_nullOrEmptyColumn_fillsNulls() {
        // A null or empty column is accepted (NOT an IllegalArgumentException) and fills the column with nulls.
        Dataset ds = CommonUtil.newDataset(Arrays.asList("id"), Arrays.asList(Arrays.asList(1), Arrays.asList(2), Arrays.asList(3)));
        Builder.of(ds).addColumn("x", Collections.emptyList()).addColumn("y", (Collection<?>) null).addColumn(1, "z", Collections.emptyList());

        // "z" went in at index 1, so it sits between "id" and the two appended columns
        assertEquals(Arrays.asList("id", "z", "x", "y"), new ArrayList<>(ds.columnNames()));
        assertEquals(3, ds.size());
        for (final String name : Arrays.asList("x", "y", "z")) {
            assertEquals(Arrays.asList(null, null, null), ds.getColumn(name));
        }
        assertNull(ds.getColumn("x").get(0));
    }

    @Test
    public void testDatasetBuilder_addColumn_nullOrEmptyName_throws() {
        final Dataset ds = CommonUtil.newDataset(Arrays.asList("id"), Arrays.asList(Arrays.asList(1), Arrays.asList(2), Arrays.asList(3)));
        assertThrows(IllegalArgumentException.class, () -> Builder.of(ds).addColumn("", Arrays.asList(1, 2, 3)));
        assertThrows(IllegalArgumentException.class, () -> Builder.of(ds).addColumn((String) null, Arrays.asList(1, 2, 3)));
        assertThrows(IllegalArgumentException.class, () -> Builder.of(ds).addColumn(0, "", Arrays.asList(1, 2, 3)));
        // control: a non-empty column of the wrong size IS rejected
        assertThrows(IllegalArgumentException.class, () -> Builder.of(ds).addColumn("w", Arrays.asList(1, 2)));
    }

    @Test
    public void testDatasetBuilder_addColumn_onColumnlessDataset_establishesRowCount() {
        // The size check is skipped while columnCount() == 0, so the first column sets the row count.
        final Dataset empty = CommonUtil.newEmptyDataset();
        assertEquals(0, empty.columnCount());
        assertEquals(0, empty.size());

        Builder.of(empty).addColumn("first", Arrays.asList(1, 2, 3, 4, 5));
        assertEquals(5, empty.size());
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), empty.getColumn("first"));

        final Dataset empty2 = CommonUtil.newEmptyDataset();
        Builder.of(empty2).addColumn(0, "a", Arrays.asList(1, 2, 3));
        assertEquals(3, empty2.size());
    }

    // ---- G28-006: the column-selection IllegalArgumentExceptions the javadoc now names ----

    @Test
    public void testDatasetBuilder_columnSelection_throwsIllegalArgumentException() {
        final Dataset ds = CommonUtil.newDataset(Arrays.asList("id", "name"), Arrays.asList(Arrays.asList(1, "a"), Arrays.asList(2, "b")));

        assertThrows(IllegalArgumentException.class, () -> Builder.of(ds).renameColumns((Collection<String>) null, s -> s + "!"));
        assertThrows(IllegalArgumentException.class, () -> Builder.of(ds).renameColumns(Collections.<String> emptyList(), s -> s + "!"));
        assertThrows(IllegalArgumentException.class, () -> Builder.of(ds).renameColumns(Arrays.asList("nope"), s -> s + "!"));
        assertThrows(IllegalArgumentException.class, () -> Builder.of(ds).renameColumns((String s) -> "same"));
        assertThrows(IllegalArgumentException.class, () -> Builder.of(ds).updateColumn("nope", (Object v) -> v));
        assertThrows(IllegalArgumentException.class, () -> Builder.of(ds).updateColumns((Collection<String>) null, (i, c, v) -> v));
        assertThrows(IllegalArgumentException.class, () -> Builder.of(ds).updateColumns(Arrays.asList("nope"), (i, c, v) -> v));
        assertThrows(IllegalArgumentException.class, () -> Builder.of(ds).addColumn("n", "nope", (Object v) -> v));
        assertThrows(IllegalArgumentException.class, () -> Builder.of(ds).addColumn("", "id", (Object v) -> v));
        assertThrows(IllegalArgumentException.class, () -> Builder.of(ds).combineColumns(Arrays.asList("nope"), "n", arr -> arr.get(0)));
        assertThrows(IllegalArgumentException.class, () -> Builder.of(ds).combineColumns(Arrays.asList("id"), "name", arr -> arr.get(0)));
        assertThrows(IllegalArgumentException.class, () -> Builder.of(ds).divideColumn("nope", Arrays.asList("a", "b"), (Object v) -> Arrays.asList(1, 2)));
        assertThrows(IllegalArgumentException.class, () -> Builder.of(ds).divideColumn("id", (Collection<String>) null, (Object v) -> Arrays.asList(1, 2)));
        assertThrows(IllegalArgumentException.class, () -> Builder.of(ds).divideColumn("id", Arrays.asList("name"), (Object v) -> Arrays.asList(1)));

        // S12-006: "or lists the same column twice" - newly documented on eight methods, previously unpinned.
        assertThrows(IllegalArgumentException.class, () -> Builder.of(ds).renameColumns(Arrays.asList("id", "id"), s -> s + "!"));
        assertThrows(IllegalArgumentException.class, () -> Builder.of(ds).updateColumns(Arrays.asList("id", "id"), (i, c, v) -> v));
        assertThrows(IllegalArgumentException.class, () -> Builder.of(ds).addColumn("n", Arrays.asList("id", "id"), arr -> arr.get(0)));
        assertThrows(IllegalArgumentException.class, () -> Builder.of(ds).addColumn(0, "n", Arrays.asList("id", "id"), arr -> arr.get(0)));
        assertThrows(IllegalArgumentException.class, () -> Builder.of(ds).combineColumns(Arrays.asList("id", "id"), "n", arr -> arr.get(0)));
        assertThrows(IllegalArgumentException.class, () -> Builder.of(ds).combineColumns(Arrays.asList("id", "id"), "n", Integer.class));
        assertThrows(IllegalArgumentException.class, () -> Builder.of(ds).combineColumns(Tuple.of("id", "id"), "n", (Object x, Object y) -> x));
        assertThrows(IllegalArgumentException.class,
                () -> Builder.of(ds).combineColumns(Tuple.of("id", "name", "id"), "n", (Object x, Object y, Object z) -> x));
    }

    @Test
    public void testDatasetBuilder_divideColumn_duplicateNewColumnNames_throws() {
        // The counterpart to the eight "same column twice" selections above: divideColumn's Tuple overloads
        // document that newColumnNames must not hold a duplicate. Its check is a separate one from
        // checkColumnNames, and reports differently, so the message is pinned rather than just the type.
        final Dataset ds = CommonUtil.newDataset(Arrays.asList("id", "name"), Arrays.asList(Arrays.asList(1, "a")));

        assertEquals("Duplicated new column names found in: (x, x)",
                assertThrows(IllegalArgumentException.class,
                        () -> Builder.of(ds).divideColumn("id", Tuple.of("x", "x"), (Object v, Pair<Object, Object> out) -> out.setLeft(v)))
                                .getMessage());
        assertEquals("Duplicated new column names found in: (x, x, x)",
                assertThrows(IllegalArgumentException.class,
                        () -> Builder.of(ds).divideColumn("id", Tuple.of("x", "x", "x"),
                                (Object v, Triple<Object, Object, Object> out) -> out.setLeft(v))).getMessage());

        // the Collection overloads report through checkColumnNames instead
        assertThrows(IllegalArgumentException.class,
                () -> Builder.of(ds).divideColumn("id", Arrays.asList("x", "x"), (Object v) -> Arrays.asList(1, 2)));

        // nothing was divided
        assertEquals(Arrays.asList("id", "name"), new ArrayList<>(ds.columnNames()));
    }

    @Test
    public void testDatasetBuilder_updateColumns_emptyColumnNames_isAccepted() {
        // updateColumns tolerates an empty selection (unlike renameColumns(Collection, Function)).
        final Dataset ds = CommonUtil.newDataset(Arrays.asList("id"), Arrays.asList(Arrays.asList(1), Arrays.asList(2)));
        Builder.of(ds).updateColumns(Collections.<String> emptyList(), (i, c, v) -> 99);
        assertEquals(Arrays.asList(1, 2), ds.getColumn("id"));
    }

    @Test
    public void testDatasetBuilder_addColumnAtIndex_withFunction_outOfBounds() {
        final Dataset ds = CommonUtil.newDataset(Arrays.asList("id", "name"), Arrays.asList(Arrays.asList(1, "a"), Arrays.asList(2, "b")));
        assertThrows(IndexOutOfBoundsException.class, () -> Builder.of(ds).addColumn(9, "n", "id", (Object v) -> v));
        assertThrows(IndexOutOfBoundsException.class, () -> Builder.of(ds).addColumn(-1, "n", Tuple.of("id", "name"), (Object x, Object y) -> x));
        // S12-006: the remaining two indexed overloads the pass newly tagged with @throws IndexOutOfBoundsException
        assertThrows(IndexOutOfBoundsException.class, () -> Builder.of(ds).addColumn(9, "n", Arrays.asList("id"), arr -> arr.get(0)));
        assertThrows(IndexOutOfBoundsException.class,
                () -> Builder.of(ds).addColumn(9, "n", Tuple.of("id", "name", "id"), (Object x, Object y, Object z) -> x));
        // an index equal to the column count is accepted (append)
        Builder.of(ds).addColumn(2, "appended", Arrays.asList("id"), arr -> arr.get(0));
        assertEquals(Arrays.asList("id", "name", "appended"), new ArrayList<>(ds.columnNames()));
    }

    // ---- S12-001: an EMPTY column selection is accepted on a Dataset that has no columns ----

    @Test
    public void testDatasetBuilder_emptyColumnSelection_isAcceptedOnColumnlessDataset() {
        // RowDataset.checkColumnNames deliberately lets an empty selection through when the Dataset has no
        // columns (there the empty list IS the full column set), so these three do NOT throw.
        Builder.of(CommonUtil.newEmptyDataset()).renameColumns(Collections.<String> emptyList(), s -> s + "!");
        Builder.of(CommonUtil.newEmptyDataset()).addColumn("n", Collections.<String> emptyList(), arr -> 1);

        final Dataset empty = CommonUtil.newEmptyDataset();
        Builder.of(empty).addColumn(0, "n", Collections.<String> emptyList(), arr -> 1);
        assertEquals(Arrays.asList("n"), new ArrayList<>(empty.columnNames()));

        // control: on a Dataset that HAS columns the same empty selection is rejected
        final Dataset ds = CommonUtil.newDataset(Arrays.asList("id"), Arrays.asList(Arrays.asList(1)));
        assertThrows(IllegalArgumentException.class, () -> Builder.of(ds).renameColumns(Collections.<String> emptyList(), s -> s + "!"));
        assertThrows(IllegalArgumentException.class, () -> Builder.of(ds).addColumn("n", Collections.<String> emptyList(), arr -> 1));
        assertThrows(IllegalArgumentException.class, () -> Builder.of(ds).addColumn(0, "n", Collections.<String> emptyList(), arr -> 1));

        // combineColumns has its own N.isEmpty guard, so it rejects an empty selection even with no columns
        assertThrows(IllegalArgumentException.class,
                () -> Builder.of(CommonUtil.newEmptyDataset()).combineColumns(Collections.<String> emptyList(), "c", arr -> 1));
    }

    // ---- S12-004: renameColumns rejects a null/empty name produced by func ----

    @Test
    public void testDatasetBuilder_renameColumns_funcReturningNullOrEmptyName_throws() {
        final Dataset ds = CommonUtil.newDataset(Arrays.asList("id", "name"), Arrays.asList(Arrays.asList(1, "a")));
        assertThrows(IllegalArgumentException.class, () -> Builder.of(ds).renameColumns(Arrays.asList("id"), s -> ""));
        assertThrows(IllegalArgumentException.class, () -> Builder.of(ds).renameColumns(Arrays.asList("id"), s -> null));

        // the all-columns overload too (one column, so the empty name is reported rather than a duplicate)
        final Dataset one = CommonUtil.newDataset(Arrays.asList("id"), Arrays.asList(Arrays.asList(1)));
        assertThrows(IllegalArgumentException.class, () -> Builder.of(one).renameColumns((String s) -> ""));
        assertThrows(IllegalArgumentException.class, () -> Builder.of(one).renameColumns((String s) -> null));

        // nothing was renamed
        assertEquals(Arrays.asList("id", "name"), new ArrayList<>(ds.columnNames()));
    }
}
