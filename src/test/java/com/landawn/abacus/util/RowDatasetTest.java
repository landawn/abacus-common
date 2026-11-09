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
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.Dataset.MissingPropertyPolicy;
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
    public void testAppend() {
        RowDataset ds1 = new RowDataset(columnNames, copyColumnList());
        RowDataset ds2 = new RowDataset(columnNames, copyColumnList());
        int originalSize = ds1.size();
        ds1.append(ds2);
        assertEquals(originalSize * 2, ds1.size());
    }

    @Test
    public void testColumnCount() {
        final RowDataset dataset = createThreeRowScoreDataset();
        Assertions.assertEquals(4, dataset.columnCount());
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
    public void testColumnNamesImmutable() {
        ImmutableList<String> names = dataset.columnNames();
        assertNotNull(names);
        assertEquals(4, names.size());
        assertEquals("id", names.get(0));
    }

    @Test
    public void testColumns() {
        Stream<ImmutableList<Object>> columnStream = dataset.columns();
        assertNotNull(columnStream);
        assertEquals(4, columnStream.count());
    }

    // combineColumns

    @Test
    public void testCombineColumnsNullNewColumnTypeNamesNewColumnTypeParameter() {
        final Dataset ds = Dataset.rows(Arrays.asList("a", "b", "c"), new Object[][] { { 1, "x", 2 } });

        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> ds.combineColumns(Arrays.asList("a", "b"), "ab", (Class<?>) null));
        assertTrue(e.getMessage().contains("'newColumnType'"), e.getMessage());
        assertEquals(Arrays.asList("a", "b", "c"), ds.columnNames());
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
    public void testCombineColumnsWithFunction() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        ds.combineColumns(Arrays.asList("name", "age"), "info", row -> row.get(0) + " is " + row.get(1));
        assertTrue(ds.containsColumn("info"));
        assertEquals("Alice is 25", ds.get(0, ds.getColumnIndex("info")));
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
    public void testConstructorBasic() {
        RowDataset ds = new RowDataset(columnNames, columnList);
        assertNotNull(ds);
        assertEquals(5, ds.size());
        assertEquals(4, ds.columnCount());
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
    public void testConstructorWithDuplicateColumnNames() {
        List<String> dupNames = Arrays.asList("id", "name", "id");
        List<List<Object>> cols = Arrays.asList(Arrays.asList(1, 2), Arrays.asList("A", "B"), Arrays.asList(10, 20));
        assertThrows(IllegalArgumentException.class, () -> {
            new RowDataset(dupNames, cols);
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
    public void testConstructorWithInconsistentColumnSizes() {
        List<List<Object>> badColumns = new ArrayList<>();
        badColumns.add(Arrays.asList(1, 2));
        badColumns.add(Arrays.asList("John", "Jane", "Bob"));

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            new RowDataset(Arrays.asList("id", "name"), badColumns);
        });
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
    public void testConstructorWithMismatchedSizes() {
        final List<List<Object>> columns = createThreeRowScoreColumns();
        List<String> shortColumnNames = Arrays.asList("id", "name");
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            new RowDataset(shortColumnNames, columns);
        });
    }

    @Test
    public void testConstructorWithNullColumnList() {
        assertThrows(Exception.class, () -> {
            new RowDataset(columnNames, null);
        });
    }

    @Test
    public void testConstructorWithNullColumnNames() {
        assertThrows(Exception.class, () -> {
            new RowDataset(null, columnList);
        });
    }

    @Test
    public void testConstructorWithNullColumns() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            new RowDataset(columnNames, null);
        });
    }

    @Test
    public void testConstructorWithNullProperties() {
        RowDataset ds = new RowDataset(columnNames, columnList, null);
        assertNotNull(ds);
        assertNotNull(ds.getProperties());
        assertTrue(ds.getProperties().isEmpty());
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
    public void testContainsAllColumns() {
        assertTrue(dataset.containsAllColumns(Arrays.asList("id", "name")));
        assertTrue(dataset.containsAllColumns(Arrays.asList()));
        assertFalse(dataset.containsAllColumns(Arrays.asList("id", "nonexistent")));
    }

    @Test
    public void testContainsColumn() {
        assertTrue(dataset.containsColumn("id"));
        assertTrue(dataset.containsColumn("name"));
        assertFalse(dataset.containsColumn("nonexistent"));
        assertFalse(dataset.containsColumn("ID"));
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

    @Test
    public void testConvertColumns_EmptyMap_ReturnsEarly() {
        RowDataset ds = new RowDataset(new ArrayList<>(Arrays.asList("id")), new ArrayList<>(Arrays.asList(new ArrayList<>(Arrays.asList(1, 2)))));
        assertDoesNotThrow(() -> ds.convertColumns(new HashMap<>()));
        assertEquals(1, (int) ds.get(0, 0));
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

    // cube

    @Test
    public void testCube() {
        final RowDataset dataset = createFiveRowCityDataset();
        List<Dataset> cubes = dataset.cube(CommonUtil.toList("city", "name")).toList();

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
    public void testCubeWithCollector() {
        final RowDataset dataset = createFiveRowCityDataset();
        Collector<Object, ?, Long> countCollector = Collectors.counting();
        List<Dataset> cubes = dataset.cube(CommonUtil.toList("city"), "name", "count", countCollector).toList();

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
    public void testCurrentRowIndex() {
        assertEquals(0, dataset.currentRowIndex());
    }

    // difference

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

    // divideColumn

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

    // firstRow

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
    public void testFirstRowEmpty() {
        Optional<Object[]> firstRow = emptyDataset.firstRow();
        assertFalse(firstRow.isPresent());
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
    public void testFirstRowWithColumnsAndSupplier() {
        final RowDataset ds = createThreeRowScoreDataset();
        Optional<Map<String, Object>> row = ds.firstRow(Arrays.asList("name", "age"), (IntFunction<Map<String, Object>>) size -> new HashMap<>());
        assertTrue(row.isPresent());
        assertEquals("John", row.get().get("name"));
    }

    @Test
    public void testFirstRowWithSupplier() {
        final RowDataset dataset = createThreeRowScoreDataset();
        Optional<List<Object>> firstRow = dataset.firstRow((IntFunction<List<Object>>) ArrayList::new);
        Assertions.assertTrue(firstRow.isPresent());
        Assertions.assertEquals(4, firstRow.get().size());
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

    // forEach

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

    // G066-01: multi-key groupBy/distinctBy keep their groups (duplicates and null keys) with plain key arrays.
    @Test
    public void testGroupBy_multiKeyIdentityPaths_pinned() {
        final Dataset ds = perfG066Left();
        final List<String> keys = Arrays.asList("k1", "k2");

        final Dataset keysOnly = ds.groupBy(keys);
        assertEquals(Arrays.asList("a", "a", "b", null), keysOnly.copyColumn("k1"));
        assertEquals(Arrays.asList(1, 2, 1, null), keysOnly.copyColumn("k2"));

        final Dataset collected = ds.groupBy(keys, "v", "vs", Collectors.toList());
        assertEquals(Arrays.asList("a", "a", "b", null), collected.copyColumn("k1"));
        assertEquals(Arrays.asList(Arrays.asList("x", "w"), Arrays.asList("y"), Arrays.asList("z"), Arrays.asList("n", "m")), collected.copyColumn("vs"));

        final Dataset mapped = ds.groupBy(keys, Arrays.asList("id", "v"), "rows", (Function<DisposableObjArray, String>) a -> a.get(0) + ":" + a.get(1),
                Collectors.toList());
        assertEquals(Arrays.asList(Arrays.asList("1:x", "4:w"), Arrays.asList("2:y"), Arrays.asList("3:z"), Arrays.asList("5:n", "6:m")),
                mapped.copyColumn("rows"));

        final Dataset typed = ds.groupBy(keys, Arrays.asList("id", "v"), "rows", List.class);
        assertEquals(Arrays.asList(Arrays.asList(Arrays.asList(1, "x"), Arrays.asList(4, "w")), Arrays.asList(Arrays.asList(2, "y")),
                Arrays.asList(Arrays.asList(3, "z")), Arrays.asList(Arrays.asList(5, "n"), Arrays.asList(6, "m"))), typed.copyColumn("rows"));

        assertEquals(Arrays.asList(1, 2, 3, 5), ds.distinctBy(keys).copyColumn("id"));

        final Dataset union = ds.unionBy(perfG066Right(), keys);
        assertEquals(Arrays.asList("id", "k1", "k2", "v", "r"), union.columnNames());
        assertEquals(Arrays.asList(1, 2, 3, 5, null), union.copyColumn("id"));
        assertEquals(Arrays.asList("a", "a", "b", null, "c"), union.copyColumn("k1"));
        assertEquals(Arrays.asList(null, null, null, null, "R3"), union.copyColumn("r"));

        // The same results a second time: nothing was left in a shared pool that could leak between calls.
        assertEquals(keysOnly, ds.groupBy(keys));
        assertEquals(collected, ds.groupBy(keys, "v", "vs", Collectors.toList()));
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

    // innerJoin

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
    public void testInPlaceUpdatesAllowCellWritesFromCallback() {
        final Dataset ds = Dataset.rows(Arrays.asList("a", "b"), new Object[][] { { 1, "x" }, { 2, "y" }, { 3, "z" } });

        // Writing a cell is not a structural modification, so the update still completes.
        ds.updateColumn("a", v -> {
            ds.set(0, 1, "touched");
            return ((Integer) v) + 1;
        });

        assertEquals(Arrays.asList(2, 3, 4), ds.copyColumn("a"));
        assertEquals("touched", ds.get(0, 1));
    }

    // intersection

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

    // isNull

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
    public void testIsNullByRowAndColumn() {
        List<List<Object>> cols = copyColumnList();
        cols.get(1).set(0, null);
        RowDataset ds = new RowDataset(columnNames, cols);
        assertTrue(ds.isNull(0, 1));
        assertFalse(ds.isNull(1, 1));
    }

    @Test
    public void testIsNullWithRowAndColumn() {
        final RowDataset dataset = createThreeRowScoreDataset();
        dataset.set(0, 0, null);
        Assertions.assertTrue(dataset.isNull(0, 0));
        Assertions.assertFalse(dataset.isNull(0, 1));
    }

    // iterator

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
    public void testBiIteratorNoRange() {
        final RowDataset ds = createThreeRowScoreDataset();
        BiIterator<Integer, String> iter = ds.iterator("id", "name");
        List<String> results = new ArrayList<>();
        iter.forEachRemaining((id, name) -> results.add(id + ":" + name));
        assertEquals(3, results.size());
        assertEquals("1:John", results.get(0));
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
    public void testTriIteratorNoRange() {
        final RowDataset ds = createThreeRowScoreDataset();
        TriIterator<Integer, String, Integer> iter = ds.iterator("id", "name", "age");
        List<String> results = new ArrayList<>();
        iter.forEachRemaining((id, name, age) -> results.add(id + ":" + name + ":" + age));
        assertEquals(3, results.size());
        assertEquals("1:John:25", results.get(0));
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

    // G066-01/G066-02: multi-key and single-key joins, with duplicate, null and unmatched keys on both sides.
    @Test
    public void testJoins_multiKeyAndRightColumnPaths_pinned() {
        final Dataset left = perfG066Left();
        final Dataset right = perfG066Right();
        final Map<String, String> on2 = perfG066On("k1", "k2");
        final Map<String, String> on1 = perfG066On("k1");

        final Dataset inner = left.innerJoin(right, on2);
        assertEquals(Arrays.asList("id", "k1", "k2", "v", "k1_2", "k2_2", "r"), inner.columnNames());
        assertEquals(Arrays.asList(1, 1, 4, 4, 5, 6), inner.copyColumn("id"));
        assertEquals(Arrays.asList("R1", "R2", "R1", "R2", "R4", "R4"), inner.copyColumn("r"));

        final Dataset leftJoined = left.leftJoin(right, on2);
        assertEquals(Arrays.asList(1, 1, 2, 3, 4, 4, 5, 6), leftJoined.copyColumn("id"));
        assertEquals(Arrays.asList("R1", "R2", null, null, "R1", "R2", "R4", "R4"), leftJoined.copyColumn("r"));

        final Dataset rightJoined = left.rightJoin(right, on2);
        assertEquals(Arrays.asList(1, 4, 1, 4, null, 5, 6), rightJoined.copyColumn("id"));
        assertEquals(Arrays.asList("R1", "R1", "R2", "R2", "R3", "R4", "R4"), rightJoined.copyColumn("r"));
        assertEquals(Arrays.asList("a", "a", "a", "a", "c", null, null), rightJoined.copyColumn("k1_2"));

        final Dataset rightJoined1 = left.rightJoin(right, on1);
        assertEquals(Arrays.asList(1, 2, 4, 1, 2, 4, null, 5, 6), rightJoined1.copyColumn("id"));
        assertEquals(Arrays.asList("R1", "R1", "R1", "R2", "R2", "R2", "R3", "R4", "R4"), rightJoined1.copyColumn("r"));
        assertEquals(Arrays.asList(1, 1, 1, 1, 1, 1, 9, null, null), rightJoined1.copyColumn("k2_2"));

        final Dataset full = left.fullJoin(right, on2);
        assertEquals(Arrays.asList(1, 1, 2, 3, 4, 4, 5, 6, null), full.copyColumn("id"));
        assertEquals(Arrays.asList("R1", "R2", null, null, "R1", "R2", "R4", "R4", "R3"), full.copyColumn("r"));
        assertEquals(Arrays.asList(1, 1, null, null, 1, 1, null, null, 9), full.copyColumn("k2_2"));

        final Dataset full1 = left.fullJoin(right, on1);
        assertEquals(Arrays.asList(1, 1, 2, 2, 3, 4, 4, 5, 6, null), full1.copyColumn("id"));
        assertEquals(Arrays.asList("R1", "R2", "R1", "R2", null, "R1", "R2", "R4", "R4", "R3"), full1.copyColumn("r"));

        // Self join through the resolved right-hand columns.
        final Dataset selfFull = left.fullJoin(left, perfG066On("id"));
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), selfFull.copyColumn("id_2"));
        assertEquals(left.copyColumn("v"), selfFull.copyColumn("v_2"));
    }

    // G066-01: multi-key joins that build a new row-typed column, with and without a collection per left row.
    @Test
    public void testJoins_multiKeyNewColumnPaths_pinned() {
        final Dataset left = perfG066Left();
        final Dataset right = perfG066Right();
        final Map<String, String> on2 = perfG066On("k1", "k2");
        final List<Object> r1 = Arrays.asList("a", 1, "R1");
        final List<Object> r2 = Arrays.asList("a", 1, "R2");
        final List<Object> r3 = Arrays.asList("c", 9, "R3");
        final List<Object> r4 = Arrays.asList(null, null, "R4");

        final Dataset inner = left.innerJoin(right, on2, "rr", List.class);
        assertEquals(Arrays.asList(1, 1, 4, 4, 5, 6), inner.copyColumn("id"));
        assertEquals(Arrays.asList(r1, r2, r1, r2, r4, r4), inner.copyColumn("rr"));

        final Dataset leftJoined = left.leftJoin(right, on2, "rr", List.class);
        assertEquals(Arrays.asList(1, 1, 2, 3, 4, 4, 5, 6), leftJoined.copyColumn("id"));
        assertEquals(Arrays.asList(r1, r2, null, null, r1, r2, r4, r4), leftJoined.copyColumn("rr"));

        final Dataset rightJoined = left.rightJoin(right, on2, "rr", List.class);
        assertEquals(Arrays.asList(1, 4, 1, 4, null, 5, 6), rightJoined.copyColumn("id"));
        assertEquals(Arrays.asList(r1, r1, r2, r2, r3, r4, r4), rightJoined.copyColumn("rr"));

        final Dataset full = left.fullJoin(right, on2, "rr", List.class);
        assertEquals(Arrays.asList(1, 1, 2, 3, 4, 4, 5, 6, null), full.copyColumn("id"));
        assertEquals(Arrays.asList(r1, r2, null, null, r1, r2, r4, r4, r3), full.copyColumn("rr"));

        final IntFunction<List<Object>> supplier = ArrayList::new;

        final Dataset innerColl = left.innerJoin(right, on2, "rr", List.class, supplier);
        assertEquals(Arrays.asList(1, 4, 5, 6), innerColl.copyColumn("id"));
        assertEquals(Arrays.asList(Arrays.asList(r1, r2), Arrays.asList(r1, r2), Arrays.asList(r4), Arrays.asList(r4)), innerColl.copyColumn("rr"));

        final Dataset leftColl = left.leftJoin(right, on2, "rr", List.class, supplier);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), leftColl.copyColumn("id"));
        assertEquals(Arrays.asList(Arrays.asList(r1, r2), null, null, Arrays.asList(r1, r2), Arrays.asList(r4), Arrays.asList(r4)), leftColl.copyColumn("rr"));

        final Dataset rightColl = left.rightJoin(right, on2, "rr", List.class, supplier);
        assertEquals(Arrays.asList(1, 4, null, 5, 6), rightColl.copyColumn("id"));
        assertEquals(Arrays.asList(Arrays.asList(r1, r2), Arrays.asList(r1, r2), Arrays.asList(r3), Arrays.asList(r4), Arrays.asList(r4)),
                rightColl.copyColumn("rr"));

        final Dataset fullColl = left.fullJoin(right, on2, "rr", List.class, supplier);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, null), fullColl.copyColumn("id"));
        assertEquals(Arrays.asList(Arrays.asList(r1, r2), null, null, Arrays.asList(r1, r2), Arrays.asList(r4), Arrays.asList(r4), Arrays.asList(r3)),
                fullColl.copyColumn("rr"));
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

    // lastRow

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
    public void testLastRowEmpty() {
        Optional<Object[]> lastRow = emptyDataset.lastRow();
        assertFalse(lastRow.isPresent());
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
    public void testLastRowWithColumnsAndSupplier() {
        final RowDataset ds = createThreeRowScoreDataset();
        Optional<Map<String, Object>> row = ds.lastRow(Arrays.asList("name", "age"), (IntFunction<Map<String, Object>>) size -> new HashMap<>());
        assertTrue(row.isPresent());
        assertEquals("Bob", row.get().get("name"));
    }

    @Test
    public void testLastRowWithSupplier() {
        final RowDataset dataset = createThreeRowScoreDataset();
        Optional<List<Object>> lastRow = dataset.lastRow((IntFunction<List<Object>>) ArrayList::new);
        Assertions.assertTrue(lastRow.isPresent());
        Assertions.assertEquals(4, lastRow.get().size());
    }

    // leftJoin

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

    // merge

    @Test
    public void testMerge() {
        RowDataset ds1 = new RowDataset(columnNames, copyColumnList());
        RowDataset ds2 = new RowDataset(columnNames, copyColumnList());
        int originalSize = ds1.size();
        ds1.merge(ds2);
        assertEquals(originalSize * 2, ds1.size());
    }

    @Test
    public void testMergeMultipleDatasets() {
        Collection<Dataset> ds = CommonUtil.toList(ds1, ds2, emptyDs);
        Dataset result = CommonUtil.merge(ds);

        assertNotNull(result);
        assertEquals(ds1.size() + ds2.size() + emptyDs.size(), result.size());
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
    public void testMergeWithNullOrEmptySelectColumnNamesThrowsIllegalArgument() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        RowDataset other = new RowDataset(new ArrayList<>(Arrays.asList("id")), new ArrayList<>(Arrays.asList(new ArrayList<>(Arrays.asList(9)))));

        assertThrows(IllegalArgumentException.class, () -> ds.merge(other, (Collection<String>) null));
        assertThrows(IllegalArgumentException.class, () -> ds.merge(other, new ArrayList<>()));
        assertThrows(IllegalArgumentException.class, () -> ds.merge(other, 0, 1, null));
        assertThrows(IllegalArgumentException.class, () -> ds.merge(other, 0, 1, new ArrayList<>()));
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

    // G066-01: parallelSortBy shares the sort path; a keyExtractor that removes a row failed with an IndexOutOfBoundsException.
    @Test
    public void testParallelSortBy_keyExtractorRemovingRowThrowsCme() {
        final Dataset ds = Dataset.rows(Arrays.asList("id"), new Object[][] { { 3 }, { 1 }, { 2 } });
        final boolean[] removed = { false };

        assertThrows(java.util.ConcurrentModificationException.class, () -> ds.parallelSortBy(Arrays.asList("id"), row -> {
            if (!removed[0]) {
                removed[0] = true;
                ds.removeRow(0);
            }

            return (Integer) row.get(0);
        }));

        assertEquals(Arrays.asList(1, 2), ds.copyColumn("id"));
    }

    // pivot

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
    public void testPrepend() {
        RowDataset ds1 = new RowDataset(columnNames, copyColumnList());
        RowDataset ds2 = new RowDataset(columnNames, copyColumnList());
        Object firstValue = ds1.get(0, 0);
        ds1.prepend(ds2);
        assertNotNull(ds1.get(ds2.size(), 0));
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

    // G113-03: multi-key removeDuplicateRowsBy keeps the first row of each key (deep array equality, nulls, many keys).
    @Test
    public void testRemoveDuplicateRowsBy_multiKeyIdentityPath_pinned() {
        // int[] and BigInteger cells are not "simple" keys, so the Wrapper key-array path runs.
        final Dataset ds = Dataset.rows(Arrays.asList("id", "k1", "k2"),
                new Object[][] { { 1, new int[] { 1 }, java.math.BigInteger.ONE }, { 2, new int[] { 1 }, java.math.BigInteger.ONE },
                        { 3, new int[] { 1 }, java.math.BigInteger.TEN }, { 4, null, null }, { 5, new int[] { 2 }, java.math.BigInteger.ONE },
                        { 6, null, null }, { 7, new int[] { 1 }, java.math.BigInteger.TEN } });
        ds.removeDuplicateRowsBy(Arrays.asList("k1", "k2"));
        assertEquals(Arrays.asList(1, 3, 4, 5), ds.copyColumn("id"));

        // no duplicates: every row stays
        final Dataset distinct = Dataset.rows(Arrays.asList("id", "k1", "k2"),
                new Object[][] { { 1, java.math.BigInteger.ONE, "a" }, { 2, java.math.BigInteger.ONE, "b" }, { 3, java.math.BigInteger.TEN, "a" } });
        distinct.removeDuplicateRowsBy(Arrays.asList("k1", "k2"));
        assertEquals(Arrays.asList(1, 2, 3), distinct.copyColumn("id"));

        // more distinct keys than the object-array pool holds, each duplicated once
        final Object[][] rows = new Object[400][];

        for (int i = 0; i < rows.length; i++) {
            rows[i] = new Object[] { i, java.math.BigInteger.valueOf(i % 200), i % 200 };
        }

        final Dataset many = Dataset.rows(Arrays.asList("id", "k1", "k2"), rows);
        many.removeDuplicateRowsBy(Arrays.asList("k1", "k2"));
        assertEquals(200, many.size());
        assertEquals(0, (Integer) many.copyColumn("id").get(0));
        assertEquals(199, (Integer) many.copyColumn("id").get(199));

        // the same again on a fresh copy gives the same result
        final Dataset again = Dataset.rows(Arrays.asList("id", "k1", "k2"), rows);
        again.removeDuplicateRowsBy(Arrays.asList("k1", "k2"));
        assertEquals(many, again);

        // non-identity key extractor (scratch array handed to the callback) is unchanged
        final Dataset extracted = Dataset.rows(Arrays.asList("id", "k1", "k2"), new Object[][] { { 1, java.math.BigInteger.ONE, "a" },
                { 2, java.math.BigInteger.ONE, "A" }, { 3, java.math.BigInteger.TEN, "a" }, { 4, java.math.BigInteger.ONE, "b" } });
        extracted.removeDuplicateRowsBy(Arrays.asList("k1", "k2"), a -> a.get(0) + "|" + ((String) a.get(1)).toLowerCase());
        assertEquals(Arrays.asList(1, 3, 4), extracted.copyColumn("id"));
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

    // renameColumn

    @Test
    public void testRenameColumn() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        ds.renameColumn("id", "identifier");
        assertEquals("identifier", ds.getColumnName(0));
        assertTrue(ds.containsColumn("identifier"));
        assertFalse(ds.containsColumn("id"));
    }

    @Test
    public void testRenameColumnNonExistent() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        assertThrows(IllegalArgumentException.class, () -> {
            ds.renameColumn("nonexistent", "newName");
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
    public void testRenameColumnsFrozen() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        ds.freeze();
        assertThrows(IllegalStateException.class, () -> {
            ds.renameColumn("id", "identifier");
        });
    }

    @Test
    public void testRenameColumnToExistingName() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        assertThrows(IllegalArgumentException.class, () -> {
            ds.renameColumn("id", "name");
        });
    }

    @Test
    public void testRenameColumnToSameName() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        ds.renameColumn("id", "id");
        assertEquals("id", ds.getColumnName(0));
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
    public void testRenameColumnWithSameName() {
        final RowDataset dataset = createThreeRowScoreDataset();
        dataset.renameColumn("id", "id");
        Assertions.assertTrue(dataset.containsColumn("id"));
    }

    // renameColumns

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
    public void testRenameColumnsValidatesAllNewNamesBeforeMutation() {
        final RowDataset dataset = createThreeRowScoreDataset();
        final Map<String, String> renameMap = new LinkedHashMap<>();
        renameMap.put("id", "identifier");
        renameMap.put("name", "");

        assertThrows(IllegalArgumentException.class, () -> dataset.renameColumns(renameMap));
        assertEquals(Arrays.asList("id", "name", "age", "score"), dataset.columnNames());
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
    public void testRenameColumnsWithMap() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        Map<String, String> renameMap = new LinkedHashMap<>();
        renameMap.put("id", "identifier");
        renameMap.put("name", "fullName");
        ds.renameColumns(renameMap);
        assertEquals("identifier", ds.getColumnName(0));
        assertEquals("fullName", ds.getColumnName(1));
    }

    // replaceIf

    @Test
    public void testReplaceIf() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        ds.replaceIf(val -> val instanceof Integer && (Integer) val > 30, 999);
        assertEquals(25, (Integer) ds.get(0, 2));
        assertEquals(999, (Integer) ds.get(2, 2));
    }

    @Test
    public void testReplaceIfFailsFastWhenPredicateAddsRow() {
        final Dataset ds = Dataset.rows(Arrays.asList("a", "b"), new Object[][] { { 1, "x" }, { 2, "y" }, { 3, "z" } });

        assertThrows(java.util.ConcurrentModificationException.class, () -> ds.replaceIf(v -> {
            ds.addRow(0, new Object[] { 99, "new" });
            return true;
        }, 0));

        assertEquals(Arrays.asList(99, 1, 2, 3), ds.copyColumn("a"));
    }

    @Test
    public void testReplaceIfIndexedFailsFastWhenPredicateAddsRow() {
        final Dataset ds = Dataset.rows(Arrays.asList("a", "b"), new Object[][] { { 1, "x" }, { 2, "y" }, { 3, "z" } });

        assertThrows(java.util.ConcurrentModificationException.class, () -> ds.replaceIf((i, c, v) -> {
            ds.addRow(0, new Object[] { 99, "new" });
            return true;
        }, 0));

        assertEquals(Arrays.asList(99, 1, 2, 3), ds.copyColumn("a"));
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

    // rightJoin

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
    public void testRightJoinWithEmptyRightDataset() {
        Dataset result = ds1.rightJoin(emptyDs, "id", "col1");

        assertNotNull(result);
        assertEquals(0, result.size());
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

    // rollup

    @Test
    public void testRollup() {
        final RowDataset dataset = createFiveRowCityDataset();
        List<Dataset> rollups = dataset.rollup(CommonUtil.toList("city", "name")).toList();

        assertNotNull(rollups);
        assertTrue(rollups.size() > 0);
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
    public void testRollupWithKeyExtractor() {
        final RowDataset dataset = createFiveRowCityDataset();
        Function<DisposableObjArray, String> keyExtractor = arr -> arr.get(0).toString();
        List<Dataset> rollups = dataset.rollup(CommonUtil.toList("city"), keyExtractor).toList();

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
    public void testRollupWithKeyExtractorAndCollector() {
        final RowDataset dataset = createFiveRowCityDataset();
        Function<DisposableObjArray, String> keyExtractor = arr -> arr.get(0).toString();
        Collector<Object, ?, Long> countCollector = Collectors.counting();
        List<Dataset> rollups = dataset.rollup(CommonUtil.toList("city"), keyExtractor, "name", "count", countCollector).toList();

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
    public void testRollupWithKeyExtractorAndRowType() {
        final RowDataset dataset = createFiveRowCityDataset();
        Function<DisposableObjArray, String> keyExtractor = arr -> arr.get(0).toString();
        List<Dataset> rollups = dataset.rollup(CommonUtil.toList("city"), keyExtractor, CommonUtil.toList("name"), "names", List.class).toList();

        assertNotNull(rollups);
        assertTrue(rollups.size() > 0);
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

    // set

    @Test
    public void testSet() {
        final RowDataset dataset = createThreeRowScoreDataset();
        dataset.set(0, 0, 100);
        Assertions.assertEquals(100, (Integer) dataset.get(0, 0));

        dataset.set(1, 1, "Updated");
        Assertions.assertEquals("Updated", dataset.get(1, 1));
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
    public void testSetByRowAndColumn() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        ds.set(0, 1, "NewName");
        assertEquals("NewName", ds.get(0, 1));
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
    public void testSetNull() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        ds.set(0, 1, null);
        assertNull(ds.get(0, 1));
        assertTrue(ds.isNull(0, 1));
    }

    // sortBy

    // G066-01: a sortBy comparator that removes a row failed part way through the row permutation with an
    // IndexOutOfBoundsException after it had already moved rows - losing one row and duplicating another.
    @Test
    public void testSortBy_comparatorRemovingRowThrowsCmeWithoutScramblingRows() {
        final Dataset ds = Dataset.rows(Arrays.asList("id"), new Object[][] { { 3 }, { 1 }, { 2 } });
        final boolean[] removed = { false };

        assertThrows(java.util.ConcurrentModificationException.class, () -> ds.sortBy("id", (Comparator<Integer>) (a, b) -> {
            if (!removed[0]) {
                removed[0] = true;
                ds.removeRow(2);
            }

            return Integer.compare(a, b);
        }));

        assertEquals(Arrays.asList(3, 1), ds.copyColumn("id"));
    }

    // G066-01: a same-size structural change (a nested sortBy) from the comparator went undetected, and the stale
    // permutation was applied to the re-ordered rows, silently leaving them in the wrong order.
    @Test
    public void testSortBy_comparatorResortingDatasetThrowsCme() {
        final Dataset ds = Dataset.rows(Arrays.asList("id", "name"), new Object[][] { { 3, "c" }, { 1, "a" }, { 2, "b" } });
        final boolean[] resorted = { false };

        assertThrows(java.util.ConcurrentModificationException.class, () -> ds.sortBy(Arrays.asList("id", "name"), (a, b) -> {
            if (!resorted[0]) {
                resorted[0] = true;
                ds.sortBy("name", Comparator.<String> reverseOrder());
            }

            return Integer.compare((Integer) a[0], (Integer) b[0]);
        }));

        assertEquals(Arrays.asList(3, 2, 1), ds.copyColumn("id"));
        assertEquals(Arrays.asList("c", "b", "a"), ds.copyColumn("name"));
    }

    // G066-01: writing cell values from a comparator stays allowed (not a structural modification).
    @Test
    public void testSortBy_comparatorWritingCellDoesNotThrow() {
        final Dataset ds = Dataset.rows(Arrays.asList("id", "name"), new Object[][] { { 2, "b" }, { 1, "a" } });

        ds.sortBy("id", (Comparator<Integer>) (a, b) -> {
            ds.set(0, 1, "w");
            return Integer.compare(a, b);
        });

        assertEquals(Arrays.asList(1, 2), ds.copyColumn("id"));
        assertEquals(Arrays.asList("a", "w"), ds.copyColumn("name"));
    }

    // G066-01: a sortBy keyExtractor that adds a row failed with ArrayIndexOutOfBoundsException.
    @Test
    public void testSortBy_keyExtractorAddingRowThrowsCme() {
        final Dataset ds = Dataset.rows(Arrays.asList("id"), new Object[][] { { 3 }, { 1 }, { 2 } });
        final boolean[] added = { false };

        assertThrows(java.util.ConcurrentModificationException.class, () -> ds.sortBy(Arrays.asList("id"), row -> {
            if (!added[0]) {
                added[0] = true;
                ds.addRow(new Object[] { 0 });
            }

            return (Integer) row.get(0);
        }));

        assertEquals(Arrays.asList(3, 1, 2, 0), ds.copyColumn("id"));
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
    public void testSwapColumnPosition() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        ds.swapColumns("id", "salary");
        assertEquals("salary", ds.getColumnName(0));
        assertEquals("name", ds.getColumnName(1));
        assertEquals("age", ds.getColumnName(2));
        assertEquals("id", ds.getColumnName(3));
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
    public void testSwapColumnPositionSame() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        ds.swapColumns("id", "id");
        assertEquals("id", ds.getColumnName(0));
    }

    @Test
    public void testSwapColumnPositionWithSameColumn() {
        final RowDataset dataset = createThreeRowScoreDataset();
        dataset.swapColumns("id", "id");
        List<String> names = dataset.columnNames();
        Assertions.assertEquals("id", names.get(0));
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
    public void testSwapRowPositionSame() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        Object value = ds.get(0, 1);
        ds.swapRows(0, 0);
        assertEquals(value, ds.get(0, 1));
    }

    @Test
    public void testSwapRowPositionWithSameRow() {
        final RowDataset dataset = createThreeRowScoreDataset();
        Object row0Name = dataset.get(0, 1);
        dataset.swapRows(0, 0);
        Assertions.assertEquals(row0Name, dataset.get(0, 1));
    }

    // symmetricDifference

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

    // G065-01: an empty column selection wrote "[]" without flushing the caller's Writer (the non-empty path flushes).
    @Test
    public void testToJson_emptySelectionFlushesWriter() {
        final Dataset ds = Dataset.rows(Arrays.asList("a", "b"), new Object[][] { { 1, "x" } });
        final java.io.StringWriter target = new java.io.StringWriter();
        final java.io.BufferedWriter writer = new java.io.BufferedWriter(target);

        ds.toJson(0, 1, Collections.<String> emptyList(), writer);

        assertEquals("[]", target.toString());
    }

    @Test
    public void testToJsonToXmlNullOutputNamesOutputParameter() {
        final Dataset ds = Dataset.rows(Arrays.asList("a"), new Object[][] { { 1 } });

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> ds.toJson((java.io.OutputStream) null));
        assertTrue(e.getMessage().contains("'output'"), e.getMessage());

        e = assertThrows(IllegalArgumentException.class, () -> ds.toJson((java.io.Writer) null));
        assertTrue(e.getMessage().contains("'output'"), e.getMessage());

        e = assertThrows(IllegalArgumentException.class, () -> ds.toXml((java.io.OutputStream) null));
        assertTrue(e.getMessage().contains("'output'"), e.getMessage());

        e = assertThrows(IllegalArgumentException.class, () -> ds.toXml((java.io.Writer) null));
        assertTrue(e.getMessage().contains("'output'"), e.getMessage());

        e = assertThrows(IllegalArgumentException.class, () -> ds.toXml("r", (java.io.Writer) null));
        assertTrue(e.getMessage().contains("'output'"), e.getMessage());

        // an empty column selection already named 'output'
        e = assertThrows(IllegalArgumentException.class, () -> ds.toJson(0, 1, new ArrayList<>(), (java.io.Writer) null));
        assertTrue(e.getMessage().contains("'output'"), e.getMessage());
    }

    // toList

    // G065-01: immutable (record) beans still go through the finishing pass.
    @Test
    public void testToListBean_immutableRecordsStillFinished() {
        final Dataset ds = new RowDataset(Arrays.asList("x", "label"),
                Arrays.asList(new ArrayList<>(Arrays.asList(1, 2, null)), new ArrayList<>(Arrays.asList("a", "b", null))));

        assertEquals(Arrays.asList(new G065Point(1, "a"), new G065Point(2, "b"), new G065Point(0, null)), ds.toList(G065Point.class));
        assertEquals(Arrays.asList(new G065Point(2, "b")), ds.toList(1, 2, G065Point.class));
    }

    // G065-01: toEntities skips the identity-finishing pass for mutable beans; pins values, fresh distinct instances and a mutable result.
    @Test
    public void testToListBean_mutableBeansDistinctAndMutableResult() {
        final Dataset ds = new RowDataset(Arrays.asList("id", "name", "age", "city"), Arrays.asList(new ArrayList<>(Arrays.asList(1, 2, 2)),
                new ArrayList<>(Arrays.asList("a", "b", "b")), new ArrayList<>(Arrays.asList(10, 20, 20)), new ArrayList<>(Arrays.asList("x", null, null))));

        final List<Person> persons = ds.toList(Person.class);

        assertEquals(3, persons.size());
        assertEquals(Arrays.asList(1, 2, 2), persons.stream().map(Person::getId).toList());
        assertEquals(Arrays.asList("a", "b", "b"), persons.stream().map(Person::getName).toList());
        assertEquals(Arrays.asList(10, 20, 20), persons.stream().map(Person::getAge).toList());
        assertEquals(Arrays.asList("x", null, null), persons.stream().map(Person::getCity).toList());
        Assertions.assertNotSame(persons.get(1), persons.get(2));
        Assertions.assertNotSame(persons.get(0), persons.get(1));

        persons.add(new Person());
        assertEquals(4, persons.size());

        final List<Person> sub = ds.toList(1, 2, Arrays.asList("id", "name"), Person.class);
        assertEquals(1, sub.size());
        assertEquals(2, sub.get(0).getId());
        assertEquals("b", sub.get(0).getName());
        assertNull(sub.get(0).getCity());

        assertTrue(ds.toList(1, 1, Person.class).isEmpty());
        assertEquals(2, ds.toEntities(0, 2, null, Person.class).size());
    }

    // G065-01: nested mutable beans (single child and child collection), with and without merging, keep their shape and identities.
    @Test
    public void testToListBean_nestedMutableBeansAndMergedEntities() {
        final Dataset ds = new RowDataset(Arrays.asList("id", "child.id", "child.name"), Arrays.asList(new ArrayList<>(Arrays.asList(1, 1, 2)),
                new ArrayList<>(Arrays.asList(10, 10, 20)), new ArrayList<>(Arrays.asList("c10", "c10", "c20"))));

        final List<G065Parent> parents = ds.toList(G065Parent.class);
        assertEquals(3, parents.size());
        assertEquals(Arrays.asList(10, 10, 20), parents.stream().map(it -> it.getChild().getId()).toList());
        assertEquals(Arrays.asList("c10", "c10", "c20"), parents.stream().map(it -> it.getChild().getName()).toList());
        Assertions.assertNotSame(parents.get(0), parents.get(1));
        Assertions.assertNotSame(parents.get(0).getChild(), parents.get(1).getChild());

        final Dataset rows = new RowDataset(Arrays.asList("id", "children.id", "children.name"), Arrays.asList(new ArrayList<>(Arrays.asList(1, 1, 1, 2)),
                new ArrayList<>(Arrays.asList(10, 20, 10, 10)), new ArrayList<>(Arrays.asList("a", "b", "a2", "c"))));

        final List<G065Parent> merged = rows.toMergedEntities("id", G065Parent.class);
        assertEquals(2, merged.size());
        assertEquals(1, merged.get(0).getId());
        assertEquals(Arrays.asList(10, 20), merged.get(0).getChildren().stream().map(G065Child::getId).toList());
        assertEquals("a2", merged.get(0).getChildren().get(0).getName());
        assertEquals(Arrays.asList(10), merged.get(1).getChildren().stream().map(G065Child::getId).toList());
        Assertions.assertNotSame(merged.get(0).getChildren().get(0), merged.get(1).getChildren().get(0));

        final List<G065Parent> unmerged = rows.toList(G065Parent.class);
        assertEquals(4, unmerged.size());
        assertEquals(Arrays.asList(1, 1, 1, 2), unmerged.stream().map(G065Parent::getId).toList());
        assertEquals(Arrays.asList(10, 20, 10, 10), unmerged.stream().map(it -> it.getChildren().get(0).getId()).toList());
        assertTrue(unmerged.stream().allMatch(it -> it.getChildren().size() == 1));
    }

    @Test
    public void testToListNullRowTypeNamesRowTypeParameter() {
        final Dataset ds = Dataset.rows(Arrays.asList("a", "b"), new Object[][] { { 1, "x" } });

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> ds.toList((Class<Object>) null));
        assertTrue(e.getMessage().contains("'rowType'"), e.getMessage());

        e = assertThrows(IllegalArgumentException.class, () -> ds.toList(0, 1, Arrays.asList("a"), (Class<Object>) null));
        assertTrue(e.getMessage().contains("'rowType'"), e.getMessage());

        // an empty Dataset rejects a null row type too
        final Dataset empty = Dataset.rows(Arrays.asList("a"), new Object[0][]);
        e = assertThrows(IllegalArgumentException.class, () -> empty.toList((Class<Object>) null));
        assertTrue(e.getMessage().contains("'rowType'"), e.getMessage());

        // the column selection is still validated first
        e = assertThrows(IllegalArgumentException.class, () -> ds.toList(0, 1, Arrays.asList("nope"), (Class<Object>) null));
        assertTrue(e.getMessage().contains("nope"), e.getMessage());
    }

    // toMergedEntities

    // G067-01: toMergedEntities read a nested collection back from a builder-based immutable bean's BUILDER through the bean's
    // property accessor -> ClassCastException (records and mutable beans were fine; toList on the same bean was fine).
    @Test
    public void testToMergedEntities_builderBeanWithNestedCollection() {
        final Dataset ds = Dataset.rows(Arrays.asList("id", "name", "children.id", "children.name"),
                new Object[][] { { 1, "p1", 10, "c10" }, { 1, "p1", 11, "c11" }, { 2, "p2", 20, "c20" }, { 1, "p1", 10, "c10" } });

        final List<G067BuilderParent> merged = ds.toMergedEntities(G067BuilderParent.class);

        assertEquals(2, merged.size());
        assertEquals(1, merged.get(0).getId());
        assertEquals("p1", merged.get(0).getName());
        assertEquals(Arrays.asList(10, 11), merged.get(0).getChildren().stream().map(G067Child::getId).toList());
        assertEquals(Arrays.asList("c10", "c11"), merged.get(0).getChildren().stream().map(G067Child::getName).toList());
        assertEquals(2, merged.get(1).getId());
        assertEquals(Arrays.asList(20), merged.get(1).getChildren().stream().map(G067Child::getId).toList());

        // toList is unchanged: one parent per row, each holding only its own row's child
        final List<G067BuilderParent> perRow = ds.toList(G067BuilderParent.class);
        assertEquals(4, perRow.size());
        assertEquals(Arrays.asList(11), perRow.get(1).getChildren().stream().map(G067Child::getId).toList());
    }

    // G120 verify of G067-01: a builder may copy (or append) the collection it is given, so the merged collection is handed to
    // each builder once, complete; setting it empty and filling it afterwards left such beans with empty lists.
    @Test
    public void testToMergedEntities_builderCopyingOrAppendingItsCollection() {
        final Dataset ds = Dataset.rows(Arrays.asList("id", "children.id", "children.name"),
                new Object[][] { { 1, 10, "c10" }, { 1, 11, "c11" }, { 2, 20, "c20" } });

        final List<G120VCopyingParent> copied = ds.toMergedEntities(G120VCopyingParent.class);
        assertEquals(2, copied.size());
        assertEquals(Arrays.asList(10, 11), copied.get(0).getChildren().stream().map(G067Child::getId).toList());
        assertEquals(Arrays.asList(20), copied.get(1).getChildren().stream().map(G067Child::getId).toList());

        final List<G120VAppendingParent> appended = ds.toMergedEntities(G120VAppendingParent.class);
        assertEquals(2, appended.size());
        assertEquals(Arrays.asList(10, 11), appended.get(0).getChildren().stream().map(G067Child::getId).toList());
        assertEquals(Arrays.asList(20), appended.get(1).getChildren().stream().map(G067Child::getId).toList());
    }

    // toMergedEntities of a record parent (Object[] intermediate, read back directly, not deferred like a builder) still merges
    // its nested collection.
    @Test
    public void testToMergedEntities_recordParentWithNestedCollection() {
        final Dataset ds = Dataset.rows(Arrays.asList("id", "children.id", "children.name"),
                new Object[][] { { 1, 10, "c10" }, { 1, 11, "c11" }, { 2, 20, "c20" }, { 1, 10, "c10" } });

        final List<G130RecordParent> merged = ds.toMergedEntities(G130RecordParent.class);

        assertEquals(2, merged.size());
        assertEquals(Arrays.asList(10, 11), merged.get(0).children().stream().map(G067Child::getId).toList());
        assertEquals(Arrays.asList(20), merged.get(1).children().stream().map(G067Child::getId).toList());
    }

    @Test
    public void testToMergedEntitiesNullIdPropNameNamesIdPropNameParameter() {
        final Dataset ds = Dataset.rows(Arrays.asList("id", "name"), new Object[][] { { 1, "x" } });

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> ds.toMergedEntities((String) null, RowDatasetTestSupport.Person.class));
        assertTrue(e.getMessage().contains("'idPropName'"), e.getMessage());

        e = assertThrows(IllegalArgumentException.class,
                () -> ds.toMergedEntities((String) null, Arrays.asList("id", "name"), RowDatasetTestSupport.Person.class));
        assertTrue(e.getMessage().contains("'idPropName'"), e.getMessage());

        e = assertThrows(IllegalArgumentException.class, () -> ds.toMergedEntities((String) null, new HashMap<>(), RowDatasetTestSupport.Person.class));
        assertTrue(e.getMessage().contains("'idPropName'"), e.getMessage());
    }

    // G065-01: an empty column selection wrote "<dataset></dataset>" without flushing the caller's Writer.
    @Test
    public void testToXml_emptySelectionFlushesWriter() {
        final Dataset ds = Dataset.rows(Arrays.asList("a", "b"), new Object[][] { { 1, "x" } });
        final java.io.StringWriter target = new java.io.StringWriter();
        final java.io.BufferedWriter writer = new java.io.BufferedWriter(target);

        ds.toXml(0, 1, Collections.<String> emptyList(), "row", writer);

        assertEquals("<dataset></dataset>", target.toString());
    }

    // updateAll

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
    public void testUpdateAllFailsFastWhenFuncAddsRow() {
        final Dataset ds = Dataset.rows(Arrays.asList("a", "b"), new Object[][] { { 1, "x" }, { 2, "y" }, { 3, "z" } });

        assertThrows(java.util.ConcurrentModificationException.class, () -> ds.updateAll(v -> {
            ds.addRow(0, new Object[] { 99, "new" });
            return v;
        }));

        assertEquals(Arrays.asList(99, 1, 2, 3), ds.copyColumn("a"));
    }

    @Test
    public void testUpdateAllIndexedFailsFastWhenFuncAddsRow() {
        final Dataset ds = Dataset.rows(Arrays.asList("a", "b"), new Object[][] { { 1, "x" }, { 2, "y" }, { 3, "z" } });

        assertThrows(java.util.ConcurrentModificationException.class, () -> ds.updateAll((i, c, v) -> {
            ds.addRow(0, new Object[] { 99, "new" });
            return v;
        }));

        assertEquals(Arrays.asList(99, 1, 2, 3), ds.copyColumn("a"));
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

    // updateColumn

    @Test
    public void testUpdateColumn() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        ds.updateColumn("age", (Integer age) -> age + 1);
        assertEquals(26, (Integer) ds.get(0, 2));
        assertEquals(31, (Integer) ds.get(1, 2));
    }

    @Test
    public void testUpdateColumnFailsFastWhenFuncAddsRow() {
        // In-place updates fail fast, like List.replaceAll, when the callback structurally modifies the Dataset:
        // the loop otherwise wrote each result into a shifted row and left the remaining rows un-updated.
        final Dataset ds = Dataset.rows(Arrays.asList("a", "b"), new Object[][] { { 1, "x" }, { 2, "y" }, { 3, "z" } });

        assertThrows(java.util.ConcurrentModificationException.class, () -> ds.updateColumn("a", v -> {
            if (Integer.valueOf(1).equals(v)) {
                ds.addRow(0, new Object[] { 99, "new" });
            }
            return ((Integer) v) * 10;
        }));

        // The row added by the callback keeps its value instead of being overwritten by the stale result.
        assertEquals(Arrays.asList(99, 1, 2, 3), ds.copyColumn("a"));
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

    // updateColumns

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
    public void testUpdateColumnsFailsFastWhenFuncRemovesRow() {
        final Dataset ds = Dataset.rows(Arrays.asList("a", "b"), new Object[][] { { 1, "x" }, { 2, "y" }, { 3, "z" } });

        assertThrows(java.util.ConcurrentModificationException.class, () -> ds.updateColumns(Arrays.asList("a"), (i, c, v) -> {
            if (i == 0) {
                ds.removeRow(0);
            }
            return ((Integer) v) * 10;
        }));

        assertEquals(Arrays.asList(2, 3), ds.copyColumn("a"));
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
    public void testUpdateRowFailsFastWhenFuncAddsRow() {
        final Dataset ds = Dataset.rows(Arrays.asList("a", "b"), new Object[][] { { 1, "x" }, { 2, "y" }, { 3, "z" } });

        assertThrows(java.util.ConcurrentModificationException.class, () -> ds.updateRow(0, v -> {
            ds.addRow(0, new Object[] { 99, "new" });
            return v;
        }));

        assertEquals(4, ds.size());
        assertEquals(99, (Integer) ds.get(0, 0));
    }

    // updateRows

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
    public void testUpdateRowsFailsFastWhenFuncAddsRow() {
        final Dataset ds = Dataset.rows(Arrays.asList("a", "b"), new Object[][] { { 1, "x" }, { 2, "y" }, { 3, "z" } });

        assertThrows(java.util.ConcurrentModificationException.class, () -> ds.updateRows(new int[] { 0, 1 }, (i, c, v) -> {
            ds.addRow(0, new Object[] { 99, "new" });
            return v;
        }));

        assertEquals(Arrays.asList(99, 1, 2, 3), ds.copyColumn("a"));
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

    public static class G065Child {
        private int id;
        private String name;

        public int getId() {
            return id;
        }

        public void setId(final int id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }
    }

    public static class G065Parent {
        private int id;
        private G065Child child;
        private List<G065Child> children;

        public int getId() {
            return id;
        }

        public void setId(final int id) {
            this.id = id;
        }

        public G065Child getChild() {
            return child;
        }

        public void setChild(final G065Child child) {
            this.child = child;
        }

        public List<G065Child> getChildren() {
            return children;
        }

        public void setChildren(final List<G065Child> children) {
            this.children = children;
        }
    }

    public record G065Point(int x, String label) {
    }

    private static Dataset perfG066Left() {
        return Dataset.rows(Arrays.asList("id", "k1", "k2", "v"), new Object[][] { { 1, "a", 1, "x" }, { 2, "a", 2, "y" }, { 3, "b", 1, "z" },
                { 4, "a", 1, "w" }, { 5, null, null, "n" }, { 6, null, null, "m" } });
    }

    private static Dataset perfG066Right() {
        return Dataset.rows(Arrays.asList("k1", "k2", "r"), new Object[][] { { "a", 1, "R1" }, { "a", 1, "R2" }, { "c", 9, "R3" }, { null, null, "R4" } });
    }

    private static Map<String, String> perfG066On(final String... names) {
        final Map<String, String> on = new LinkedHashMap<>();

        for (final String name : names) {
            on.put(name, name);
        }

        return on;
    }

    public static final class G067Child {
        @com.landawn.abacus.annotation.Id
        private int id;
        private String name;

        public int getId() {
            return id;
        }

        public void setId(final int id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }
    }

    public static final class G067BuilderParent {
        @com.landawn.abacus.annotation.Id
        private final int id;
        private final String name;
        private final List<G067Child> children;

        private G067BuilderParent(final int id, final String name, final List<G067Child> children) {
            this.id = id;
            this.name = name;
            this.children = children;
        }

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public List<G067Child> getChildren() {
            return children;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            private int id;
            private String name;
            private List<G067Child> children;

            public Builder id(final int id) {
                this.id = id;
                return this;
            }

            public Builder name(final String name) {
                this.name = name;
                return this;
            }

            public Builder children(final List<G067Child> children) {
                this.children = children;
                return this;
            }

            public G067BuilderParent build() {
                return new G067BuilderParent(id, name, children);
            }
        }
    }

    public static final class G120VCopyingParent {
        @com.landawn.abacus.annotation.Id
        private final int id;
        private final List<G067Child> children;

        private G120VCopyingParent(final int id, final List<G067Child> children) {
            this.id = id;
            this.children = children;
        }

        public int getId() {
            return id;
        }

        public List<G067Child> getChildren() {
            return children;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            private int id;
            private List<G067Child> children = new ArrayList<>();

            public Builder id(final int id) {
                this.id = id;
                return this;
            }

            public Builder children(final List<G067Child> children) {
                this.children = new ArrayList<>(children);
                return this;
            }

            public G120VCopyingParent build() {
                return new G120VCopyingParent(id, children);
            }
        }
    }

    public static final class G120VAppendingParent {
        @com.landawn.abacus.annotation.Id
        private final int id;
        private final List<G067Child> children;

        private G120VAppendingParent(final int id, final List<G067Child> children) {
            this.id = id;
            this.children = children;
        }

        public int getId() {
            return id;
        }

        public List<G067Child> getChildren() {
            return children;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            private int id;
            private final List<G067Child> children = new ArrayList<>();

            public Builder id(final int id) {
                this.id = id;
                return this;
            }

            public Builder children(final List<G067Child> children) {
                this.children.addAll(children);
                return this;
            }

            public G120VAppendingParent build() {
                return new G120VAppendingParent(id, children);
            }
        }
    }

    public record G130RecordParent(@com.landawn.abacus.annotation.Id int id, List<G067Child> children) {
    }

    // ---- bug review 2026-10-02 final G130 end ----

    // ---- deep review 2026-10-26 G071 begin ----

    public static class G071OnlyId {
        private int id;

        public int getId() {
            return id;
        }

        public void setId(final int id) {
            this.id = id;
        }
    }

    // G071-01: stream(.., beanClass).toArray() on an empty remainder must not fail where toList()/count() return no rows
    @Test
    public void testStreamBeanToArray_emptyRemainderUnderErrorPolicy() {
        final Dataset strict = Dataset.rows(Arrays.asList("id", "extra"), new Object[][] { { 1, "x" }, { 2, "y" } })
                .withMissingPropertyPolicy(Dataset.MissingPropertyPolicy.ERROR);

        assertEquals(0, strict.stream(1, 1, G071OnlyId.class).toList().size());
        assertEquals(0, strict.stream(1, 1, G071OnlyId.class).toArray().length);
        assertEquals(0, strict.stream(2, 2, G071OnlyId.class).toArray(G071OnlyId[]::new).length);
        // A remainder that has rows to convert still reports the unmapped column.
        assertThrows(IllegalArgumentException.class, () -> strict.stream(1, 2, G071OnlyId.class).toArray());
    }

    // ---- deep review 2026-10-26 G071 end ----

    // ---- deep review 2026-10-26 verify G124 begin ----

    // G071-01 (verify): the other paths that reach the same bulk shortcut on an empty remainder - intermediate ops that buffer
    // through it (sorted/reversed), skip past the end, the rowSupplier overload with a bean row and a slice - report no rows.
    @Test
    public void testStreamBeanToArray_emptyRemainderThroughBufferingOpsSupplierAndSlice() {
        final Dataset strict = Dataset.rows(Arrays.asList("id", "extra"), new Object[][] { { 1, "x" }, { 2, "y" } })
                .withMissingPropertyPolicy(Dataset.MissingPropertyPolicy.ERROR);

        assertEquals(0, strict.stream(1, 1, G071OnlyId.class).sorted(Comparator.comparingInt(G071OnlyId::getId)).toList().size());
        assertEquals(0, strict.stream(2, 2, G071OnlyId.class).reversed().toList().size());
        assertEquals(0, strict.stream(1, 1, G071OnlyId.class).skip(1).toArray().length);
        assertEquals(0, strict.stream(0, 0, columnCount -> new G071OnlyId()).toArray().length);
        assertEquals(0, strict.slice(1, 2).stream(1, 1, G071OnlyId.class).toArray().length);
    }

    // G071-01 (verify): unchanged neighbours - rows to convert still fail under ERROR (also through sorted), the default policy
    // still converts, and an empty remainder of non-bean rows is still empty.
    @Test
    public void testStreamBeanToArray_nonEmptyRemainderAndDefaultPolicyUnchanged() {
        final Dataset plain = Dataset.rows(Arrays.asList("id", "extra"), new Object[][] { { 1, "x" }, { 2, "y" } });
        final Dataset strict = plain.withMissingPropertyPolicy(Dataset.MissingPropertyPolicy.ERROR);

        assertThrows(IllegalArgumentException.class, () -> strict.stream(0, 1, G071OnlyId.class).sorted(Comparator.comparingInt(G071OnlyId::getId)).toList());
        assertThrows(IllegalArgumentException.class, () -> strict.stream(0, 2, G071OnlyId.class).toArray(G071OnlyId[]::new));

        final G071OnlyId[] converted = plain.stream(0, 2, G071OnlyId.class).toArray(G071OnlyId[]::new);
        assertEquals(2, converted.length);
        assertEquals(1, converted[0].getId());
        assertEquals(2, converted[1].getId());
        assertEquals(0, plain.stream(1, 1, G071OnlyId.class).toArray().length);
        assertEquals(0, strict.stream(2, 2, Object[].class).toArray().length);
        assertEquals(0, strict.stream(0, 2, G071OnlyId.class).limit(0).toArray().length);
    }

    // ---- deep review 2026-10-26 verify G124 end ----

    // ---- deep review 2026-11-02 G070 begin ----

    public static class G070Address {
        private String city;

        public String getCity() {
            return city;
        }

        public void setCity(final String city) {
            this.city = city;
        }
    }

    public static class G070Person {
        private int id;
        private String name;
        private G070Address address;

        public G070Person() {
        }

        G070Person(final int id, final String name, final String city) {
            this.id = id;
            this.name = name;

            if (city != null) {
                address = new G070Address();
                address.setCity(city);
            }
        }

        public int getId() {
            return id;
        }

        public void setId(final int id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public G070Address getAddress() {
            return address;
        }

        public void setAddress(final G070Address address) {
            this.address = address;
        }
    }

    public static class G070Other {
        private int id;
        private String name;
        private String extra;

        public G070Other() {
        }

        G070Other(final int id, final String name) {
            this.id = id;
            this.name = name;
            this.extra = "x" + id;
        }

        public int getId() {
            return id;
        }

        public void setId(final int id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public String getExtra() {
            return extra;
        }

        public void setExtra(final String extra) {
            this.extra = extra;
        }
    }

    static final List<String> G070_GETTER_CALLS = new ArrayList<>();

    @com.landawn.abacus.annotation.AccessFieldByMethod
    public static class G070Failing {
        private int id;
        private String name;

        public G070Failing() {
        }

        G070Failing(final int id, final String name) {
            this.id = id;
            this.name = name;
        }

        public int getId() {
            G070_GETTER_CALLS.add("id " + id);

            if (id < 0) {
                throw new IllegalStateException("bad id " + id);
            }

            return id;
        }

        public void setId(final int id) {
            this.id = id;
        }

        public String getName() {
            G070_GETTER_CALLS.add("name " + name);
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }
    }

    private static List<List<Object>> g070Rows(final Dataset dataset) {
        final List<List<Object>> result = new ArrayList<>();

        for (final Object[] row : dataset.toList()) {
            result.add(Arrays.asList(row));
        }

        return result;
    }

    // G070-01 (pin): addRows reuses the bean-row accessors resolved for the previous row of the same class. Rows of
    // different bean classes, nested columns (null link -> null), and array/map/collection rows interleaved with beans
    // must still land in the right columns.
    @Test
    public void testAddRows_beanRowAccessorsAcrossClassesAndRowKinds() {
        final Dataset nested = Dataset.rows(Arrays.asList("name", "id", "address.city"), new ArrayList<List<Object>>());
        final Map<String, Object> mapRow = new LinkedHashMap<>();
        mapRow.put("id", 7);
        mapRow.put("address.city", "cm");
        mapRow.put("name", "m");

        nested.addRows(Arrays.asList(new G070Person(1, "a", "c1"), new G070Person(2, "b", null), new Object[] { "x", 9, "cx" }, mapRow,
                Arrays.asList("l", 8, "cl"), new G070Person(3, null, "c3")));

        assertEquals(Arrays.asList(Arrays.asList("a", 1, "c1"), Arrays.asList("b", 2, null), Arrays.asList("x", 9, "cx"), Arrays.asList("m", 7, "cm"),
                Arrays.asList("l", 8, "cl"), Arrays.asList(null, 3, "c3")), g070Rows(nested));

        final Dataset flat = Dataset.rows(Arrays.asList("id", "name"), new ArrayList<List<Object>>());
        flat.addRow(new Object[] { 0, "first" });
        flat.addRows(1, Arrays.asList(new G070Person(1, "p1", null), new G070Other(2, "o2"), new G070Person(3, "p3", "c"), new G070Other(4, "o4"),
                new G070Other(5, "o5")));

        assertEquals(Arrays.asList(Arrays.asList(0, "first"), Arrays.asList(1, "p1"), Arrays.asList(2, "o2"), Arrays.asList(3, "p3"), Arrays.asList(4, "o4"),
                Arrays.asList(5, "o5")), g070Rows(flat));

        // Single-row and one-row-batch paths agree with the batch.
        final Dataset single = Dataset.rows(Arrays.asList("name", "id", "address.city"), new ArrayList<List<Object>>());
        single.addRow(new G070Person(1, "a", "c1"));
        single.addRows(Collections.singletonList(new G070Person(2, "b", null)));
        assertEquals(Arrays.asList(Arrays.asList("a", 1, "c1"), Arrays.asList("b", 2, null)), g070Rows(single));
    }

    // G070-01 (pin): a column that a later bean class lacks still fails the whole batch with the same message, and an
    // invalid nested chain is rejected; the dataset is left unchanged.
    @Test
    public void testAddRows_beanRowAccessorsMissingColumnInLaterClass() {
        final Dataset dataset = Dataset.rows(Arrays.asList("id", "extra"), new ArrayList<List<Object>>());
        dataset.addRow(new Object[] { 0, "e0" });

        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> dataset.addRows(Arrays.asList(new G070Other(1, "a"), new G070Other(2, "b"), new G070Person(3, "c", null))));
        assertTrue(e.getMessage().startsWith("Column (extra) is not found in bean"), e.getMessage());
        assertEquals(Arrays.asList(Arrays.asList(0, "e0")), g070Rows(dataset));

        final Dataset chain = Dataset.rows(Arrays.asList("id", "address.zip"), new ArrayList<List<Object>>());
        assertThrows(IllegalArgumentException.class, () -> chain.addRows(Arrays.asList(new G070Person(1, "a", "c"))));
        assertEquals(0, chain.size());
    }

    // G070-01 (pin): accessors are resolved lazily in column order, so a failing accessor-backed getter surfaces at the
    // same row and column, after the same getter calls, and the batch adds nothing.
    @Test
    public void testAddRows_beanRowAccessorsGetterFailureTiming() {
        final Dataset dataset = Dataset.rows(Arrays.asList("name", "id"), new ArrayList<List<Object>>());
        G070_GETTER_CALLS.clear();

        final IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> dataset.addRows(Arrays.asList(new G070Failing(1, "a"), new G070Failing(-2, "b"), new G070Failing(3, "c"))));
        assertEquals("bad id -2", e.getMessage());
        assertEquals(Arrays.asList("name a", "id 1", "name b", "id -2"), G070_GETTER_CALLS);
        assertEquals(0, dataset.size());

        G070_GETTER_CALLS.clear();
        dataset.addRows(Arrays.asList(new G070Failing(1, "a"), new G070Failing(2, "b")));
        assertEquals(Arrays.asList(Arrays.asList("a", 1), Arrays.asList("b", 2)), g070Rows(dataset));
    }

    // G070-01 (pin): the resolved accessors belong to one call; a later batch after the columns were moved or renamed
    // resolves them again.
    @Test
    public void testAddRows_beanRowAccessorsNotReusedAcrossCalls() {
        final Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new ArrayList<List<Object>>());
        dataset.addRows(Arrays.asList(new G070Person(1, "a", null), new G070Person(2, "b", null)));

        dataset.moveColumn("name", 0);
        dataset.addRows(Arrays.asList(new G070Person(3, "c", null), new G070Person(4, "d", null)));

        assertEquals(Arrays.asList(Arrays.asList("a", 1), Arrays.asList("b", 2), Arrays.asList("c", 3), Arrays.asList("d", 4)), g070Rows(dataset));

        dataset.renameColumn("name", "extra");
        assertThrows(IllegalArgumentException.class, () -> dataset.addRows(Arrays.asList(new G070Person(5, "e", null))));
        dataset.addRows(Arrays.asList(new G070Other(6, "f")));
        assertEquals(Arrays.asList("x6", 6), g070Rows(dataset).get(4));
    }

    // ---- deep review 2026-11-02 G070 end ----

    // ---- deep review 2026-11-02 G129 begin ----

    static Dataset g129Target;
    static int g129HookId;
    static String g129RenameFrom;
    static String g129RenameTo;

    @com.landawn.abacus.annotation.AccessFieldByMethod
    public static class G129Hooked {
        private int id;
        private String name;
        private String extra;
        private G070Address address;

        public G129Hooked() {
        }

        G129Hooked(final int id, final String name, final String extra, final String city) {
            this.id = id;
            this.name = name;
            this.extra = extra;

            if (city != null) {
                address = new G070Address();
                address.setCity(city);
            }
        }

        public int getId() {
            // Renames a column of the dataset being filled while this row is read.
            if (id == g129HookId && g129Target != null) {
                g129Target.renameColumn(g129RenameFrom, g129RenameTo);
            }

            return id;
        }

        public void setId(final int id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public String getExtra() {
            return extra;
        }

        public void setExtra(final String extra) {
            this.extra = extra;
        }

        public G070Address getAddress() {
            return address;
        }

        public void setAddress(final G070Address address) {
            this.address = address;
        }
    }

    private static Dataset g129Dataset(final String... columnNames) {
        return Dataset.rows(Arrays.asList(columnNames), new ArrayList<List<Object>>());
    }

    private static void g129Hook(final Dataset target, final int hookId, final String from, final String to) {
        g129Target = target;
        g129HookId = hookId;
        g129RenameFrom = from;
        g129RenameTo = to;
    }

    private static void g129Unhook() {
        g129Target = null;
        g129HookId = 0;
        g129RenameFrom = null;
        g129RenameTo = null;
    }

    // G129-01: a getter of a later row renames a column to a name the bean lacks. Every remaining column of that row
    // must be resolved by its current name, so addRows fails like addRow does (the cached accessor used to read the
    // old property and commit the batch).
    @Test
    public void testAddRows_getterRenamesColumnToMissingProperty() {
        try {
            final Dataset batch = g129Dataset("id", "name");
            g129Hook(batch, 2, "name", "zzz");
            final IllegalArgumentException batchError = assertThrows(IllegalArgumentException.class,
                    () -> batch.addRows(Arrays.asList(new G129Hooked(1, "a", "x1", null), new G129Hooked(2, "b", "x2", null))));
            assertTrue(batchError.getMessage().startsWith("Column (zzz) is not found in bean"), batchError.getMessage());
            assertEquals(0, batch.size());

            final Dataset single = g129Dataset("id", "name");
            single.addRow(new G129Hooked(1, "a", "x1", null));
            g129Hook(single, 2, "name", "zzz");
            final IllegalArgumentException singleError = assertThrows(IllegalArgumentException.class, () -> single.addRow(new G129Hooked(2, "b", "x2", null)));
            assertEquals(singleError.getMessage(), batchError.getMessage());
        } finally {
            g129Unhook();
        }
    }

    // G129-01: the same rename to a name the bean has - the rest of the row is read through the new column name.
    @Test
    public void testAddRows_getterRenamesColumnToExistingProperty() {
        try {
            final Dataset dataset = g129Dataset("id", "name");
            g129Hook(dataset, 2, "name", "extra");
            dataset.addRows(Arrays.asList(new G129Hooked(1, "a", "x1", null), new G129Hooked(2, "b", "x2", null), new G129Hooked(3, "c", "x3", null)));

            assertEquals(Arrays.asList("id", "extra"), dataset.columnNames());
            assertEquals(Arrays.asList(Arrays.asList(1, "a"), Arrays.asList(2, "x2"), Arrays.asList(3, "x3")), g070Rows(dataset));
        } finally {
            g129Unhook();
        }
    }

    // G129-01: a nested-chain column renamed by a getter of a later row is resolved again as well.
    @Test
    public void testAddRows_getterRenamesNestedChainColumn() {
        try {
            final Dataset dataset = g129Dataset("id", "address.city");
            g129Hook(dataset, 2, "address.city", "extra");
            dataset.addRows(Arrays.asList(new G129Hooked(1, "a", "x1", "c1"), new G129Hooked(2, "b", "x2", "c2"), new G129Hooked(3, "c", "x3", "c3")));

            assertEquals(Arrays.asList(Arrays.asList(1, "c1"), Arrays.asList(2, "x2"), Arrays.asList(3, "x3")), g070Rows(dataset));
        } finally {
            g129Unhook();
        }
    }

    // G129-01 (pin): a rename during the first row, while the accessors are being resolved, leaves later rows reading
    // the current columns.
    @Test
    public void testAddRows_getterRenamesColumnDuringFirstRow() {
        try {
            final Dataset dataset = g129Dataset("id", "name");
            g129Hook(dataset, 1, "name", "extra");
            dataset.addRows(Arrays.asList(new G129Hooked(1, "a", "x1", null), new G129Hooked(2, "b", "x2", null)));

            assertEquals(Arrays.asList(Arrays.asList(1, "x1"), Arrays.asList(2, "x2")), g070Rows(dataset));
        } finally {
            g129Unhook();
        }
    }

    static String g129StructuralProperty;
    static int g129StructuralRowId;
    static java.util.function.Consumer<Dataset> g129StructuralAction;

    // A bean whose getter for g129StructuralProperty runs g129StructuralAction on g129Target while row g129StructuralRowId
    // is read.
    @com.landawn.abacus.annotation.AccessFieldByMethod
    public static class G129Structural {
        private int id;
        private String name;
        private String extra;

        public G129Structural() {
        }

        G129Structural(final int id, final String name, final String extra) {
            this.id = id;
            this.name = name;
            this.extra = extra;
        }

        private void fire(final String property) {
            if (id == g129StructuralRowId && property.equals(g129StructuralProperty) && g129Target != null) {
                final Dataset target = g129Target;
                g129Target = null; // once
                g129StructuralAction.accept(target);
            }
        }

        public int getId() {
            fire("id");
            return id;
        }

        public void setId(final int id) {
            this.id = id;
        }

        public String getName() {
            fire("name");
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public String getExtra() {
            fire("extra");
            return extra;
        }

        public void setExtra(final String extra) {
            this.extra = extra;
        }
    }

    private static void g129StructuralHook(final Dataset target, final int rowId, final String property, final java.util.function.Consumer<Dataset> action) {
        g129Target = target;
        g129StructuralRowId = rowId;
        g129StructuralProperty = property;
        g129StructuralAction = action;
    }

    private static Dataset g129OneRowDataset() {
        final Dataset dataset = g129Dataset("id", "name", "extra");
        dataset.addRow(new Object[] { 0, "n0", "e0" });
        return dataset;
    }

    // Nothing of the failed call is in the dataset: still one row, every remaining column holds exactly that row.
    private static void g129AssertOnlyFirstRow(final Dataset dataset) {
        assertEquals(1, dataset.size());

        for (final String columnName : dataset.columnNames()) {
            assertEquals(1, dataset.getColumn(columnName).size(), columnName);
        }
    }

    // G129-02: a getter that adds or removes a column while addRows / addRow reads the rows fails fast with a
    // ConcurrentModificationException and adds nothing. addRows used to commit values under shifted columns or overflow its
    // scratch row; addRow could add the row to some columns and then throw IndexOutOfBoundsException.
    @Test
    public void testAddRows_getterAddsOrRemovesColumnFailsFast() {
        final Object[][] cases = { // property whose getter fires, row id, action
                { "id", 2, (java.util.function.Consumer<Dataset>) ds -> ds.removeColumn("extra") }, // mid-row, a later column
                { "extra", 2, (java.util.function.Consumer<Dataset>) ds -> ds.removeColumn("id") }, // last cell of a row
                { "name", 1, (java.util.function.Consumer<Dataset>) ds -> ds.removeColumn("name") }, // first row
                { "id", 2, (java.util.function.Consumer<Dataset>) ds -> ds.addColumn("added", Arrays.asList("a0")) }, // add
                { "extra", 3, (java.util.function.Consumer<Dataset>) ds -> ds.addColumn(0, "added", Arrays.asList("a0")) } }; // last row

        try {
            for (final Object[] entry : cases) {
                @SuppressWarnings("unchecked")
                final java.util.function.Consumer<Dataset> action = (java.util.function.Consumer<Dataset>) entry[2];
                final String label = entry[0] + "@" + entry[1];

                final Dataset batch = g129OneRowDataset();
                g129StructuralHook(batch, (Integer) entry[1], (String) entry[0], action);
                final java.util.ConcurrentModificationException batchError = assertThrows(java.util.ConcurrentModificationException.class, () -> batch
                        .addRows(Arrays.asList(new G129Structural(1, "n1", "e1"), new G129Structural(2, "n2", "e2"), new G129Structural(3, "n3", "e3"))),
                        label);
                assertTrue(batchError.getMessage().contains("no row was added"), batchError.getMessage());
                g129AssertOnlyFirstRow(batch);

                // The single-row path, for the rows whose getter fires.
                final Dataset single = g129OneRowDataset();
                g129StructuralHook(single, (Integer) entry[1], (String) entry[0], action);
                assertThrows(java.util.ConcurrentModificationException.class, () -> single.addRow(new G129Structural((Integer) entry[1], "n", "e")), label);
                g129AssertOnlyFirstRow(single);
            }
        } finally {
            g129Unhook();
        }
    }

    // G129-02: the rows collection's own iterator removing a column between two rows fails fast as well.
    @Test
    public void testAddRows_rowsIteratorChangesColumnCountFailsFast() {
        final Dataset dataset = g129OneRowDataset();
        final List<Object> source = Arrays.asList(new Object[] { 1, "n1", "e1" }, new Object[] { 2, "n2", "e2" });
        final Collection<Object> rows = new java.util.AbstractCollection<>() {
            @Override
            public java.util.Iterator<Object> iterator() {
                final java.util.Iterator<Object> iterator = source.iterator();

                return new java.util.Iterator<>() {
                    private int returned = 0;

                    @Override
                    public boolean hasNext() {
                        return iterator.hasNext();
                    }

                    @Override
                    public Object next() {
                        if (returned++ == 1) {
                            dataset.removeColumn("extra");
                        }

                        return iterator.next();
                    }
                };
            }

            @Override
            public int size() {
                return source.size();
            }
        };

        assertThrows(java.util.ConcurrentModificationException.class, () -> dataset.addRows(rows));
        g129AssertOnlyFirstRow(dataset);
    }

    // ---- deep review 2026-11-02 G129 end ----

    // ---- deep review 2026-11-02b G070 begin ----

    // A map row whose get(key) runs action on target the first time key is read.
    private static Map<String, Object> g070bHookedMapRow(final Dataset target, final String key, final java.util.function.Consumer<Dataset> action) {
        final Map<String, Object> row = new HashMap<>() {
            private static final long serialVersionUID = 1L;
            private boolean fired = false;

            @Override
            public Object get(final Object k) {
                if (!fired && key.equals(k)) {
                    fired = true;
                    action.accept(target);
                }

                return super.get(k);
            }
        };

        row.put("id", 1);
        row.put("name", "n1");
        row.put("extra", "e1");
        return row;
    }

    // G070-01: a map row whose get() adds or removes a column fails with the documented ConcurrentModificationException and
    // adds nothing. The map branch read the column names at shifted positions instead: IndexOutOfBoundsException for a
    // removed column, IllegalArgumentException "Column (added) is not found in map" for a column inserted mid-row.
    @Test
    public void testAddRow_mapRowGetAddsOrRemovesColumnFailsFast_r116() {
        final Object[][] cases = { // key whose get() fires, action
                { "id", (java.util.function.Consumer<Dataset>) ds -> ds.removeColumn("extra") }, // was IndexOutOfBoundsException
                { "id", (java.util.function.Consumer<Dataset>) ds -> ds.removeColumn("id") }, // was IndexOutOfBoundsException
                { "name", (java.util.function.Consumer<Dataset>) ds -> ds.removeColumn("id") }, // was IndexOutOfBoundsException
                { "id", (java.util.function.Consumer<Dataset>) ds -> ds.addColumn(1, "added", Arrays.asList("a0")) }, // was IAE
                { "id", (java.util.function.Consumer<Dataset>) ds -> ds.addColumn("added", Arrays.asList("a0")) }, // already CME
                { "extra", (java.util.function.Consumer<Dataset>) ds -> ds.removeColumn("name") } }; // last cell: already CME

        for (final Object[] entry : cases) {
            final String key = (String) entry[0];
            @SuppressWarnings("unchecked")
            final java.util.function.Consumer<Dataset> action = (java.util.function.Consumer<Dataset>) entry[1];

            final Dataset single = g129OneRowDataset();
            final java.util.ConcurrentModificationException singleError = assertThrows(java.util.ConcurrentModificationException.class,
                    () -> single.addRow(g070bHookedMapRow(single, key, action)), key);
            assertTrue(singleError.getMessage().contains("no row was added"), singleError.getMessage());
            g129AssertOnlyFirstRow(single);

            final Dataset batch = g129OneRowDataset();
            assertThrows(java.util.ConcurrentModificationException.class,
                    () -> batch.addRows(Arrays.asList(new Object[] { 2, "n2", "e2" }, g070bHookedMapRow(batch, key, action))), key);
            g129AssertOnlyFirstRow(batch);
        }
    }

    // G070-01 (pin): a map row whose get() renames a column (the count is unchanged) still reads the rest of the row by the
    // current column names, and a map row that changes nothing is added as before.
    @Test
    public void testAddRow_mapRowGetRenamesColumnStillReadsCurrentNames_r116() {
        final Dataset dataset = g129OneRowDataset();
        final Map<String, Object> row = g070bHookedMapRow(dataset, "id", ds -> ds.renameColumn("name", "nm"));
        row.put("nm", "renamed");
        dataset.addRow(row);

        assertEquals(Arrays.asList("id", "nm", "extra"), new ArrayList<>(dataset.columnNames()));
        assertEquals(Arrays.asList(0, 1), new ArrayList<>(dataset.getColumn("id")));
        assertEquals(Arrays.asList("n0", "renamed"), new ArrayList<>(dataset.getColumn("nm")));
        assertEquals(Arrays.asList("e0", "e1"), new ArrayList<>(dataset.getColumn("extra")));

        final Map<String, Object> plain = new HashMap<>();
        plain.put("id", 2);
        plain.put("nm", "n2");
        plain.put("extra", null);
        dataset.addRows(Arrays.asList(plain, Arrays.asList(3, "n3", "e3")));
        assertEquals(4, dataset.size());
        assertEquals(Arrays.asList(0, 1, 2, 3), new ArrayList<>(dataset.getColumn("id")));
        assertNull(dataset.get(2, 2));
    }

    // G070-02: the rows iterator's final hasNext() (the call that returns false) adding or removing a column fails with
    // ConcurrentModificationException before anything is committed. It used to commit the rows under shifted columns and
    // throw IndexOutOfBoundsException without rowsChanged() (a removed column), or leave the columns at different lengths
    // with no exception at all (an added column).
    @Test
    public void testAddRows_rowsIteratorFinalHasNextChangesColumnCountFailsFast_r116() {
        final List<java.util.function.Consumer<Dataset>> actions = Arrays.asList(ds -> ds.removeColumn("extra"), ds -> ds.removeColumn("id"),
                ds -> ds.addColumn("added", Arrays.asList("a0")), ds -> ds.addColumn(0, "added", Arrays.asList("a0")));

        for (int i = 0; i < actions.size(); i++) {
            final java.util.function.Consumer<Dataset> action = actions.get(i);
            final Dataset dataset = g129OneRowDataset();
            final List<Object> source = Arrays.asList(new Object[] { 1, "n1", "e1" }, new Object[] { 2, "n2", "e2" });
            final Collection<Object> rows = new java.util.AbstractCollection<>() {
                @Override
                public java.util.Iterator<Object> iterator() {
                    final java.util.Iterator<Object> iterator = source.iterator();

                    return new java.util.Iterator<>() {
                        private boolean fired = false;

                        @Override
                        public boolean hasNext() {
                            final boolean hasNext = iterator.hasNext();

                            if (!hasNext && !fired) {
                                fired = true;
                                action.accept(dataset);
                            }

                            return hasNext;
                        }

                        @Override
                        public Object next() {
                            return iterator.next();
                        }
                    };
                }

                @Override
                public int size() {
                    return source.size();
                }
            };

            assertThrows(java.util.ConcurrentModificationException.class, () -> dataset.addRows(rows), "action " + i);
            g129AssertOnlyFirstRow(dataset);
        }
    }

    // ---- deep review 2026-11-02b G070 end ----

    // ---- deep review 2026-11-03 review P2 begin ----

    // Column layout changes that keep the column count (so the former count-only check let them through).
    private static List<Object[]> p2NetZeroLayoutChanges() {
        return Arrays.asList(new Object[] { "remove and re-add id", (java.util.function.Consumer<Dataset>) ds -> {
            final List<Object> ids = new ArrayList<>(ds.getColumn("id"));
            ds.removeColumn("id");
            ds.addColumn("id", ids); // same name, same count, now the last column
        } }, new Object[] { "move id to the end", (java.util.function.Consumer<Dataset>) ds -> ds.moveColumn("id", 2) },
                new Object[] { "swap id and extra", (java.util.function.Consumer<Dataset>) ds -> ds.swapColumns("id", "extra") },
                new Object[] { "replace name by another column", (java.util.function.Consumer<Dataset>) ds -> {
                    ds.removeColumn("name");
                    ds.addColumn(1, "other", Arrays.asList("o0"));
                } });
    }

    // The first row is intact under every column name it still has.
    private static void p2AssertFirstRowIntact(final Dataset dataset) {
        g129AssertOnlyFirstRow(dataset);
        final Map<String, Object> expected = new HashMap<>();
        expected.put("id", 0);
        expected.put("name", "n0");
        expected.put("extra", "e0");
        expected.put("other", "o0");

        for (final String columnName : dataset.columnNames()) {
            assertEquals(expected.get(columnName), dataset.getColumn(columnName).get(0), columnName);
        }
    }

    // Review P2: a map row whose get() removes a column and adds it back (or moves, swaps or replaces one) keeps the column
    // count, which was all the check compared: the row was committed under shifted columns (name=1 instead of "n1"), and in
    // addRows the rows read before it were scrambled as well. It now fails with ConcurrentModificationException and adds
    // nothing, like an added or removed column.
    @Test
    public void testAddRow_mapRowGetNetZeroColumnLayoutChangeFailsFast_r116() {
        for (final Object[] change : p2NetZeroLayoutChanges()) {
            final String label = (String) change[0];
            @SuppressWarnings("unchecked")
            final java.util.function.Consumer<Dataset> action = (java.util.function.Consumer<Dataset>) change[1];

            for (final String key : Arrays.asList("id", "name", "extra")) {
                final Dataset single = g129OneRowDataset();
                final java.util.ConcurrentModificationException singleError = assertThrows(java.util.ConcurrentModificationException.class,
                        () -> single.addRow(g070bHookedMapRow(single, key, action)), label + " @" + key);
                assertTrue(singleError.getMessage().contains("no row was added"), singleError.getMessage());
                p2AssertFirstRowIntact(single);

                final Dataset batch = g129OneRowDataset();
                assertThrows(java.util.ConcurrentModificationException.class,
                        () -> batch
                                .addRows(Arrays.asList(new Object[] { 2, "n2", "e2" }, g070bHookedMapRow(batch, key, action), new Object[] { 3, "n3", "e3" })),
                        label + " @" + key);
                p2AssertFirstRowIntact(batch);
            }
        }
    }

    // Review P2: the same for a bean row whose getter changes the column layout (single row and batch, first or later row).
    @Test
    public void testAddRows_getterNetZeroColumnLayoutChangeFailsFast_r116() {
        try {
            for (final Object[] change : p2NetZeroLayoutChanges()) {
                final String label = (String) change[0];
                @SuppressWarnings("unchecked")
                final java.util.function.Consumer<Dataset> action = (java.util.function.Consumer<Dataset>) change[1];

                for (final Object[] hook : new Object[][] { { "id", 1 }, { "extra", 1 }, { "name", 2 }, { "extra", 3 } }) {
                    final String hookLabel = label + " @" + hook[0] + "#" + hook[1];

                    final Dataset batch = g129OneRowDataset();
                    g129StructuralHook(batch, (Integer) hook[1], (String) hook[0], action);
                    assertThrows(java.util.ConcurrentModificationException.class,
                            () -> batch.addRows(
                                    Arrays.asList(new G129Structural(1, "n1", "e1"), new G129Structural(2, "n2", "e2"), new G129Structural(3, "n3", "e3"))),
                            hookLabel);
                    p2AssertFirstRowIntact(batch);

                    final Dataset single = g129OneRowDataset();
                    g129StructuralHook(single, (Integer) hook[1], (String) hook[0], action);
                    assertThrows(java.util.ConcurrentModificationException.class, () -> single.addRow(new G129Structural((Integer) hook[1], "n", "e")),
                            hookLabel);
                    p2AssertFirstRowIntact(single);
                }
            }
        } finally {
            g129Unhook();
        }
    }

    // Review P2: the rows iterator's final hasNext() making a count-preserving layout change fails before the commit too;
    // the rows read before it were aligned to the old positions.
    @Test
    public void testAddRows_rowsIteratorFinalHasNextNetZeroLayoutChangeFailsFast_r116() {
        for (final Object[] change : p2NetZeroLayoutChanges()) {
            @SuppressWarnings("unchecked")
            final java.util.function.Consumer<Dataset> action = (java.util.function.Consumer<Dataset>) change[1];
            final Dataset dataset = g129OneRowDataset();
            final List<Object> source = Arrays.asList(new Object[] { 1, "n1", "e1" }, new Object[] { 2, "n2", "e2" });
            final Collection<Object> rows = new java.util.AbstractCollection<>() {
                @Override
                public java.util.Iterator<Object> iterator() {
                    final java.util.Iterator<Object> iterator = source.iterator();

                    return new java.util.Iterator<>() {
                        private boolean fired = false;

                        @Override
                        public boolean hasNext() {
                            final boolean hasNext = iterator.hasNext();

                            if (!hasNext && !fired) {
                                fired = true;
                                action.accept(dataset);
                            }

                            return hasNext;
                        }

                        @Override
                        public Object next() {
                            return iterator.next();
                        }
                    };
                }

                @Override
                public int size() {
                    return source.size();
                }
            };

            assertThrows(java.util.ConcurrentModificationException.class, () -> dataset.addRows(rows), (String) change[0]);
            p2AssertFirstRowIntact(dataset);
        }
    }

    // Review P2 (pin): a getter that renames a column keeps the layout (same storage lists), so a batch is still added and
    // read by the current names; a column moved before the call does not matter.
    @Test
    public void testAddRows_renameDuringReadAndLayoutChangeBeforeCallStillAdd_r116() {
        final Dataset dataset = g129OneRowDataset();
        dataset.moveColumn("id", 2);
        dataset.addRows(Arrays.asList(Map.of("id", 1, "name", "n1", "extra", "e1"), new Object[] { "n2", "e2", 2 }));
        assertEquals(Arrays.asList(0, 1, 2), new ArrayList<>(dataset.getColumn("id")));
        assertEquals(Arrays.asList("n0", "n1", "n2"), new ArrayList<>(dataset.getColumn("name")));

        final Dataset renamed = g129OneRowDataset();
        final Map<String, Object> row = g070bHookedMapRow(renamed, "id", ds -> ds.renameColumn("extra", "ex"));
        row.put("ex", "renamed");
        renamed.addRows(Arrays.asList(row, new Object[] { 2, "n2", "e2" }));
        assertEquals(Arrays.asList("e0", "renamed", "e2"), new ArrayList<>(renamed.getColumn("ex")));
        assertEquals(Arrays.asList("n0", "n1", "n2"), new ArrayList<>(renamed.getColumn("name")));
    }

    // A rows collection whose size() runs action on target the first time it is called. isEmpty() does not call size(), so
    // the action runs from addRows' own rows.size() call, after its emptiness check.
    private static Collection<Object> p2SizeHookedRows(final Dataset target, final java.util.function.Consumer<Dataset> action) {
        final List<Object> source = Arrays.asList(new Object[] { 1, "n1", "e1" }, new Object[] { 2, "n2", "e2" });

        return new java.util.AbstractCollection<>() {
            private boolean fired = false;

            @Override
            public boolean isEmpty() {
                return source.isEmpty();
            }

            @Override
            public int size() {
                if (!fired) {
                    fired = true;
                    action.accept(target);
                }

                return source.size();
            }

            @Override
            public java.util.Iterator<Object> iterator() {
                return source.iterator();
            }
        };
    }

    // Review P2 follow-up: the layout baseline was taken after addRows called rows.size(), so a column change made by that
    // size() became the baseline. A column added there was then committed with only its original row (the other columns
    // got two more), and a removed, moved or re-added column was not detected. The baseline is now taken before size():
    // every such change fails with ConcurrentModificationException and adds nothing, at the end or at a position.
    @Test
    public void testAddRows_rowsSizeChangesColumnLayoutFailsFast_r116() {
        final List<Object[]> changes = new ArrayList<>(p2NetZeroLayoutChanges());
        changes.add(new Object[] { "add a column at the end", (java.util.function.Consumer<Dataset>) ds -> ds.addColumn("added", Arrays.asList("a0")) });
        changes.add(new Object[] { "add a column first", (java.util.function.Consumer<Dataset>) ds -> ds.addColumn(0, "added", Arrays.asList("a0")) });
        changes.add(new Object[] { "remove a column", (java.util.function.Consumer<Dataset>) ds -> ds.removeColumn("extra") });

        for (final Object[] change : changes) {
            @SuppressWarnings("unchecked")
            final java.util.function.Consumer<Dataset> action = (java.util.function.Consumer<Dataset>) change[1];

            for (final int position : new int[] { -1, 0, 1 }) {
                final String label = change[0] + " / position " + position;
                final Dataset dataset = g129OneRowDataset();
                final Collection<Object> rows = p2SizeHookedRows(dataset, action);
                final java.util.ConcurrentModificationException error = assertThrows(java.util.ConcurrentModificationException.class, () -> {
                    if (position < 0) {
                        dataset.addRows(rows);
                    } else {
                        dataset.addRows(position, rows);
                    }
                }, label);
                assertTrue(error.getMessage().contains("no row was added"), error.getMessage());

                // Still one row in every column, and the original cells are where their names say.
                g129AssertOnlyFirstRow(dataset);
                final Map<String, Object> expected = new HashMap<>();
                expected.put("id", 0);
                expected.put("name", "n0");
                expected.put("extra", "e0");
                expected.put("other", "o0");
                expected.put("added", "a0");

                for (final String columnName : dataset.columnNames()) {
                    assertEquals(expected.get(columnName), dataset.getColumn(columnName).get(0), label + ": " + columnName);
                }
            }
        }
    }

    // Review P2 follow-up (pin): a size() that only renames a column keeps the layout, so the rows are still added.
    @Test
    public void testAddRows_rowsSizeRenamesColumnStillAdds_r116() {
        final Dataset dataset = g129OneRowDataset();
        dataset.addRows(p2SizeHookedRows(dataset, ds -> ds.renameColumn("extra", "ex")));

        assertEquals(Arrays.asList("id", "name", "ex"), new ArrayList<>(dataset.columnNames()));
        assertEquals(Arrays.asList(0, 1, 2), new ArrayList<>(dataset.getColumn("id")));
        assertEquals(Arrays.asList("e0", "e1", "e2"), new ArrayList<>(dataset.getColumn("ex")));
    }

    // ---- deep review 2026-11-03 review P2 end ----

    public static class Emp {
        private int id;
        private String firstName;

        public int getId() {
            return id;
        }

        public void setId(final int id) {
            this.id = id;
        }

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(final String firstName) {
            this.firstName = firstName;
        }
    }

    public static class Addr {
        private String city;
        private String zip;

        public String getCity() {
            return city;
        }

        public void setCity(final String city) {
            this.city = city;
        }

        public String getZip() {
            return zip;
        }

        public void setZip(final String zip) {
            this.zip = zip;
        }
    }

    public static class Holder {
        private int id;
        private Addr home;
        private Addr work;

        public int getId() {
            return id;
        }

        public void setId(final int id) {
            this.id = id;
        }

        public Addr getHome() {
            return home;
        }

        public void setHome(final Addr home) {
            this.home = home;
        }

        public Addr getWork() {
            return work;
        }

        public void setWork(final Addr work) {
            this.work = work;
        }
    }

    private static RowDataset ds3(final List<String> names, final Object[]... rows) {
        final List<List<Object>> columns = new ArrayList<>();
        for (int c = 0; c < names.size(); c++) {
            final List<Object> col = new ArrayList<>();
            for (final Object[] row : rows) {
                col.add(row[c]);
            }
            columns.add(col);
        }
        return new RowDataset(new ArrayList<>(names), columns);
    }

    private static RowDataset sample() {
        return ds3(Arrays.asList("id", "name", "age"), new Object[] { 1, "a", 10 }, new Object[] { 2, "b", 20 }, new Object[] { 3, "c", 30 });
    }

    private static RowDataset empty2(final String... names) {
        final List<List<Object>> columns = new ArrayList<>();
        for (int i = 0; i < names.length; i++) {
            columns.add(new ArrayList<>());
        }
        return new RowDataset(new ArrayList<>(Arrays.asList(names)), columns);
    }

    @Test
    public void testColumnNamesListIterator() {
        final RowDataset ds3 = sample();
        final ListIterator<String> iter = ds3.columnNames().listIterator();
        assertFalse(iter.hasPrevious());
        assertEquals(0, iter.nextIndex());
        assertEquals(-1, iter.previousIndex());
        assertEquals("id", iter.next());
        assertTrue(iter.hasPrevious());
        assertEquals(1, iter.nextIndex());
        assertEquals(0, iter.previousIndex());
        assertThrows(UnsupportedOperationException.class, () -> iter.remove());
        assertThrows(UnsupportedOperationException.class, () -> iter.set("x"));
        assertThrows(UnsupportedOperationException.class, () -> iter.add("x"));

        final List<String> seen = new ArrayList<>();
        ds3.columnNames().forEach(seen::add);
        assertEquals(Arrays.asList("id", "name", "age"), seen);
    }

    @Test
    public void testLastRowEmpty2() {
        final RowDataset ds3 = empty2("id", "name");
        assertFalse(ds3.lastRow(Arrays.asList("id"), n -> new Object[n]).isPresent());
    }

    @Test
    public void testForEachTuple3Reverse() throws Exception {
        final RowDataset ds3 = sample();
        final List<Object> seen = new ArrayList<>();
        ds3.forEach(2, -1, Tuple.of("id", "name", "age"), (a, b, c) -> seen.add(a));
        assertEquals(Arrays.asList(3, 2, 1), seen);
    }

    @Test
    public void testToListIdentityFilter() {
        final RowDataset ds3 = sample();
        final List<Object[]> list = ds3.toList(0, 2, Fn.alwaysTrue(), Fn.identity(), Object[].class);
        assertEquals(2, list.size());
        assertEquals(Arrays.asList(1, "a", 10), Arrays.asList(list.get(0)));
    }

    @Test
    public void testToStringWithProperties() {
        final Map<String, Object> props = new HashMap<>();
        props.put("k", "v");
        final RowDataset ds3 = new RowDataset(new ArrayList<>(Arrays.asList("id")), new ArrayList<>(Arrays.asList(new ArrayList<>(Arrays.asList(1)))), props);
        assertTrue(ds3.toString().contains("properties={k=v}"));
    }

    @Test
    public void testPaginatePageCount() {
        final RowDataset ds3 = sample();
        @SuppressWarnings("deprecation")
        final int pages = ds3.paginate(2).pageCount();
        assertEquals(2, pages);
    }

    @Test
    public void testTopByInvalidN() {
        final RowDataset ds3 = sample();
        assertThrows(IllegalArgumentException.class, () -> ds3.topBy("id", 0, (a, b) -> 0));
        assertThrows(IllegalArgumentException.class, () -> ds3.topBy(Arrays.asList("id"), 0, (Object[] a, Object[] b) -> 0));
        assertThrows(IllegalArgumentException.class,
                () -> ds3.topBy(Arrays.asList("id"), 0, (java.util.function.Function<NoCachingNoUpdating.DisposableObjArray, Comparable>) a -> 1));
    }

    @Test
    public void testSortOnEmpty() {
        final RowDataset ds3 = empty2("id", "name");
        ds3.sortBy("id", (a, b) -> 0);
        ds3.sortBy(Arrays.asList("id"), (Object[] a, Object[] b) -> 0);
        ds3.sortBy(Arrays.asList("id"), (java.util.function.Function<NoCachingNoUpdating.DisposableObjArray, Comparable>) a -> (Comparable) a.get(0));
        assertEquals(0, ds3.size());
    }

    @Test
    public void testParallelSortByKeyExtractor() {
        final RowDataset ds3 = ds3(Arrays.asList("id", "name"), new Object[] { 3, "c" }, new Object[] { 1, "a" }, new Object[] { 2, "b" });
        ds3.parallelSortBy(Arrays.asList("id"), (java.util.function.Function<NoCachingNoUpdating.DisposableObjArray, Comparable>) a -> (Comparable) a.get(0));
        assertEquals(Arrays.asList(1, 2, 3), ds3.getColumn("id"));
    }

    @Test
    public void testGroupByIdentityKeyExtractor() {
        final RowDataset ds3 = ds3(Arrays.asList("k", "k2", "v"), new Object[] { 1, "x", 10 }, new Object[] { 1, "x", 20 }, new Object[] { 2, "y", 30 });
        final Dataset g1 = ds3.groupBy(Arrays.asList("k"), Fn.identity(), Arrays.asList("v"), "vs", List.class);
        assertEquals(2, g1.size());
        final Dataset g2 = ds3.groupBy(Arrays.asList("k", "k2"), Fn.identity(), Arrays.asList("v"), "vs", List.class);
        assertEquals(2, g2.size());
        assertEquals(Arrays.asList("k", "k2", "vs"), new ArrayList<>(g2.columnNames()));
    }

    @Test
    public void testGroupByDuplicateResultName() {
        final RowDataset ds3 = sample();
        assertThrows(IllegalArgumentException.class,
                () -> ds3.groupBy("id", Fn.identity(), Arrays.asList("age"), "id", a -> a.get(0), com.landawn.abacus.util.stream.Collectors.toList()));
        assertThrows(IllegalArgumentException.class, () -> ds3.groupBy(Arrays.asList("id"), a -> a.get(0), Arrays.asList("age"), "id", a -> a.get(0),
                com.landawn.abacus.util.stream.Collectors.toList()));
    }

    @Test
    public void testGroupBySingleKeyExtractorOnEmpty() {
        final RowDataset ds3 = empty2("id", "name");
        final Dataset g = ds3.groupBy(Arrays.asList("id"), a -> a.get(0));
        assertEquals(0, g.size());
        assertEquals(Arrays.asList("id"), new ArrayList<>(g.columnNames()));
    }

    @Test
    public void testPivotResultNameClash() {
        final RowDataset ds3 = ds3(Arrays.asList("__pivot_result", "p", "v"), new Object[] { "r1", "c1", 1 }, new Object[] { "r1", "c2", 2 });
        final Sheet<Object, Object, Object> sheet = ds3.pivot("__pivot_result", "p", "v",
                com.landawn.abacus.util.stream.Collectors.summingInt(x -> (Integer) x));
        assertEquals(1, sheet.get("r1", "c1"));
        assertEquals(2, sheet.get("r1", "c2"));
    }

    @Test
    public void testFlatMapColumnsNoCopying() {
        final RowDataset ds3 = sample();
        final Dataset r1 = ds3.flatMapColumns(Tuple.of("id", "age"), "x", null, (a, b) -> Arrays.asList(a, b));
        assertEquals(Arrays.asList(1, 10, 2, 20, 3, 30), r1.getColumn("x"));

        final Dataset r2 = ds3.flatMapColumns(Arrays.asList("id", "age"), "x", null, a -> (Integer) a.get(0) == 2 ? null : Arrays.asList(a.get(1)));
        assertEquals(Arrays.asList(10, 30), r2.getColumn("x"));
    }

    @Test
    public void testJoinsWithEmptySides() {
        final RowDataset left = empty2("id", "name");
        final RowDataset right = ds3(Arrays.asList("rid", "rname"), new Object[] { 1, "x" }, new Object[] { 1, "y" });
        final Map<String, String> on1 = new LinkedHashMap<>();
        on1.put("id", "rid");
        final Map<String, String> on2 = new LinkedHashMap<>();
        on2.put("id", "rid");
        on2.put("name", "rname");

        assertEquals(0, left.innerJoin(right, on2).size());
        assertEquals(0, left.leftJoin(right, on1).size());
        assertEquals(0, left.innerJoin(right, on1, "nc", Object[].class).size());
        assertEquals(0, left.innerJoin(right, on2, "nc", Object[].class).size());
        assertEquals(0, left.innerJoin(right, on1, "nc", Object[].class, n -> new ArrayList<>()).size());
        assertEquals(0, left.innerJoin(right, on2, "nc", Object[].class, n -> new ArrayList<>()).size());

        final RowDataset emptyRight = empty2("rid", "rname");
        final RowDataset full = sample();
        final Map<String, String> onF1 = new LinkedHashMap<>();
        onF1.put("id", "rid");
        final Map<String, String> onF2 = new LinkedHashMap<>();
        onF2.put("id", "rid");
        onF2.put("name", "rname");
        assertEquals(0, full.rightJoin(emptyRight, onF1).size());
        assertEquals(0, full.rightJoin(emptyRight, onF1, "nc", Object[].class).size());
        assertEquals(0, full.rightJoin(emptyRight, onF2, "nc", Object[].class).size());
        assertEquals(0, full.rightJoin(emptyRight, onF1, "nc", Object[].class, n -> new ArrayList<>()).size());
        assertEquals(0, full.rightJoin(emptyRight, onF2, "nc", Object[].class, n -> new ArrayList<>()).size());
    }

    @Test
    public void testMultiKeyJoinDuplicateRightKeys() {
        final RowDataset left = ds3(Arrays.asList("id", "name"), new Object[] { 1, "x" });
        final RowDataset right = ds3(Arrays.asList("rid", "rname", "v"), new Object[] { 1, "x", "p" }, new Object[] { 1, "x", "q" });
        final Map<String, String> on2 = new LinkedHashMap<>();
        on2.put("id", "rid");
        on2.put("name", "rname");
        final Dataset r = left.innerJoin(right, on2);
        assertEquals(2, r.size());
        assertEquals(Arrays.asList("p", "q"), r.getColumn("v"));
    }

    @Test
    public void testCombineColumnsEmptyNames() {
        final RowDataset ds3 = sample();
        assertThrows(IllegalArgumentException.class, () -> ds3.combineColumns(new ArrayList<String>(), "x", Object[].class));
        assertThrows(IllegalArgumentException.class, () -> ds3.combineColumns(new ArrayList<String>(), "x", (NoCachingNoUpdating.DisposableObjArray a) -> 1));
    }

    @Test
    public void testToMergedEntitiesIdNameResolution() {
        final RowDataset ds3 = ds3(Arrays.asList("ID", "first_name"), new Object[] { 1, "a" }, new Object[] { 1, "a" }, new Object[] { 2, "b" });
        final List<Emp> l1 = ds3.toMergedEntities(Arrays.asList("id"), Arrays.asList("ID", "first_name"), null, Emp.class);
        assertEquals(2, l1.size());
        assertEquals(1, l1.get(0).getId());
        assertEquals("a", l1.get(0).getFirstName());

        final List<Emp> l2 = ds3.toMergedEntities(Arrays.asList("firstName"), Arrays.asList("ID", "first_name"), null, Emp.class);
        assertEquals(2, l2.size());
        assertEquals("b", l2.get(1).getFirstName());

        final RowDataset ds2 = ds3(Arrays.asList("id", "FIRSTNAME"), new Object[] { 1, "a" });
        final List<Emp> l3 = ds2.toMergedEntities(Arrays.asList("id", "firstName"), Arrays.asList("id", "FIRSTNAME"), null, Emp.class);
        assertEquals(1, l3.size());
    }

    @Test
    public void testStrictPolicyUnknownPrefixedProperty() {
        final RowDataset ds3 = ds3(Arrays.asList("id", "zzz.q"), new Object[] { 1, "a" });
        final Dataset strict = ds3.withMissingPropertyPolicy(MissingPropertyPolicy.ERROR);
        assertThrows(IllegalArgumentException.class, () -> strict.toList(Emp.class));
        assertThrows(IllegalArgumentException.class, () -> strict.getRow(0, Emp.class));

        final List<Emp> lenient = ds3.toList(Emp.class);
        assertEquals(1, lenient.get(0).getId());
        assertEquals(1, ds3.getRow(0, Emp.class).getId());
    }

    @Test
    public void testToXmlNonAsciiElementNameAndBeanValue() {
        final RowDataset ds3 = ds3(Arrays.asList("id"), new Object[] { 1 });
        final String xml = ds3.toXml("élément");
        assertTrue(xml.contains("<élément>"));

        final Emp e = new Emp();
        e.setId(7);
        e.setFirstName("z");
        final RowDataset beanDs = ds3(Arrays.asList("emp"), new Object[] { e });
        final String xml2 = beanDs.toXml();
        assertTrue(xml2.contains("7"));
        assertTrue(xml2.contains("z"));
    }

    @Test
    public void testGetRowInterleavedNestedColumns() {
        final RowDataset ds3 = ds3(Arrays.asList("home.city", "work.city", "home.zip"), new Object[] { "A", "B", "Z" });
        final Holder h = ds3.getRow(0, Holder.class);
        assertEquals("A", h.getHome().getCity());
        assertEquals("Z", h.getHome().getZip());
        assertEquals("B", h.getWork().getCity());
    }

    @Test
    public void testGetRowPrefixOnNonBeanProperty() {
        final RowDataset ds3 = ds3(Arrays.asList("id.x"), new Object[] { 1 });
        assertThrows(UnsupportedOperationException.class, () -> ds3.getRow(0, Holder.class));
    }

    @Test
    public void testIteratorsNextActionWhenExhausted() {
        final RowDataset ds3 = sample();
        final BiIterator<Object, Object> bi = ds3.iterator("id", "name");
        while (bi.hasNext()) {
            bi.next();
        }
        assertThrows(java.util.NoSuchElementException.class, () -> bi.next());
        assertThrows(java.util.NoSuchElementException.class, () -> bi.next((a, b) -> {
        }));

        final TriIterator<Object, Object, Object> tri = ds3.iterator("id", "name", "age");
        while (tri.hasNext()) {
            tri.next();
        }
        assertThrows(java.util.NoSuchElementException.class, () -> tri.next());
        assertThrows(java.util.NoSuchElementException.class, () -> tri.next((a, b, c) -> {
        }));
    }

    @Test
    public void testPaginateIteratorExhausted() {
        final RowDataset ds3 = sample();
        final java.util.Iterator<Dataset> it = ds3.paginate(2).iterator();
        assertEquals(2, it.next().size());
        assertEquals(1, it.next().size());
        assertFalse(it.hasNext());
        assertThrows(java.util.NoSuchElementException.class, () -> it.next());
    }

    @Test
    public void testGroupByIdentitySingleKeyOnEmpty() {
        final RowDataset ds3 = empty2("id", "name");
        final Dataset g = ds3.groupBy(Arrays.asList("id"), Fn.identity());
        assertEquals(0, g.size());
        assertEquals(Arrays.asList("id"), new ArrayList<>(g.columnNames()));
    }

    @Test
    public void testRightJoinMultiKeyEmptyRight() {
        final RowDataset emptyRight = empty2("rid", "rname");
        final Map<String, String> on2 = new LinkedHashMap<>();
        on2.put("id", "rid");
        on2.put("name", "rname");
        final Dataset r = sample().rightJoin(emptyRight, on2);
        assertEquals(0, r.size());
        assertEquals(5, r.columnCount());
    }

    @Test
    public void testRemoveColumnsByFilterNoColumns() {
        final RowDataset ds3 = new RowDataset(new ArrayList<>(), new ArrayList<>());
        ds3.removeColumns(name -> true);
        assertEquals(0, ds3.columnCount());
    }
}
