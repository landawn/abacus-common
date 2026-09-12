package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.NoCachingNoUpdating.DisposableObjArray;
import com.landawn.abacus.util.Tuple.Tuple2;
import com.landawn.abacus.util.Tuple.Tuple3;
import com.landawn.abacus.util.function.BiFunction;
import com.landawn.abacus.util.function.IntFunction;
import com.landawn.abacus.util.function.TriFunction;
import com.landawn.abacus.util.stream.Stream;

public class RowDatasetStreamTest extends RowDatasetTestSupport {
    @Test
    public void testStreamByColumnName() {
        Stream<Integer> idStream = ds1.stream("id");
        List<Integer> ids = idStream.toList();

        assertEquals(3, ids.size());
        assertEquals(CommonUtil.toList(1, 2, 3), ids);
    }

    @Test
    public void testStreamByColumnNameWithRange() {
        Stream<String> nameStream = ds1.stream(1, 3, "name");
        List<String> names = nameStream.toList();

        assertEquals(2, names.size());
        assertEquals(CommonUtil.toList("Bob", "Charlie"), names);
    }

    @Test
    public void testStreamWithPrefixAndFieldNameMap() {
        Map<String, String> prefixMap = new HashMap<>();
        prefixMap.put("", "");

        Stream<Person> rowStream = ds1.stream(prefixMap, Person.class);
        List<Person> rows = rowStream.toList();

        assertEquals(3, rows.size());
    }

    @Test
    public void testStreamWithTuple2() {
        Tuple2<String, String> columnNames = Tuple.of("id", "name");
        Stream<String> stream = ds1.stream(columnNames, (id, name) -> id + "-" + name);
        List<String> results = stream.toList();

        assertEquals(3, results.size());
        assertEquals("1-Alice", results.get(0));
        assertEquals("2-Bob", results.get(1));
        assertEquals("3-Charlie", results.get(2));
    }

    @Test
    public void testStreamWithTuple3() {
        Tuple3<String, String, String> columnNames = Tuple.of("id", "name", "age");
        Stream<String> stream = ds1.stream(columnNames, (id, name, age) -> id + "-" + name + "-" + age);
        List<String> results = stream.toList();

        assertEquals(3, results.size());
        assertEquals("1-Alice-25", results.get(0));
    }

    @Test
    public void testStreamWithRangeAndColumnsAndRowType() {
        final RowDataset ds = createThreeRowScoreDataset();
        List<TestBean> list = ds.stream(1, 3, Arrays.asList("name", "age"), TestBean.class).toList();
        assertEquals(2, list.size());
        assertEquals("Jane", list.get(0).name);
        assertEquals(35, list.get(1).age);
    }

    @Test
    public void testStreamWithRangeAndColumnsAndSupplier() {
        final RowDataset ds = createThreeRowScoreDataset();
        List<Map<String, Object>> list = ds.stream(0, 2, Arrays.asList("name", "age"), (IntFunction<Map<String, Object>>) size -> new HashMap<>()).toList();
        assertEquals(2, list.size());
        assertEquals("John", list.get(0).get("name"));
    }

    @Test
    public void testStreamWithColumnsAndRowMapper() {
        final RowDataset ds = createThreeRowScoreDataset();
        List<String> list = ds.stream(Arrays.asList("name", "age"), (int rowIndex, DisposableObjArray arr) -> arr.get(0) + "-" + arr.get(1)).toList();
        assertEquals(3, list.size());
        assertEquals("John-25", list.get(0));
    }

    @Test
    public void testStreamWithRangeAndRowMapper() {
        final RowDataset ds = createThreeRowScoreDataset();
        List<String> list = ds.stream(1, 3, (int rowIndex, DisposableObjArray arr) -> arr.get(1) + "=" + arr.get(2)).toList();
        assertEquals(2, list.size());
        assertEquals("Jane=30", list.get(0));
    }

    @Test
    public void testStreamWithRangeAndColumnsAndRowMapper() {
        final RowDataset ds = createThreeRowScoreDataset();
        List<String> list = ds.stream(0, 2, Arrays.asList("name"), (int rowIndex, DisposableObjArray arr) -> "row" + rowIndex + ":" + arr.get(0)).toList();
        assertEquals(2, list.size());
        assertEquals("row0:John", list.get(0));
        assertEquals("row1:Jane", list.get(1));
    }

    @Test
    public void testStreamWithRangeAndTuple2() {
        final RowDataset ds = createThreeRowScoreDataset();
        List<String> list = ds.stream(1, 3, Tuple.of("name", "age"), (BiFunction<String, Integer, String>) (name, age) -> name + "(" + age + ")").toList();
        assertEquals(2, list.size());
        assertEquals("Jane(30)", list.get(0));
        assertEquals("Bob(35)", list.get(1));
    }

    @Test
    public void testStreamWithRangeAndTuple3() {
        final RowDataset ds = createThreeRowScoreDataset();
        List<String> list = ds
                .stream(0, 2, new Tuple3<>("id", "name", "age"), (TriFunction<Integer, String, Integer, String>) (id, name, age) -> id + ":" + name + ":" + age)
                .toList();
        assertEquals(2, list.size());
        assertEquals("1:John:25", list.get(0));
        assertEquals("2:Jane:30", list.get(1));
    }

    @Test
    public void testStreamWithColumnsAndPrefixMap() {
        final RowDataset ds = createThreeRowScoreDataset();
        Map<String, String> prefixMap = new HashMap<>();
        List<TestBean> list = ds.stream(Arrays.asList("name", "age"), prefixMap, TestBean.class).toList();
        assertEquals(3, list.size());
        assertEquals("John", list.get(0).name);
    }

    @Test
    public void testStreamWithRangeAndColumnsAndPrefixMap() {
        final RowDataset ds = createThreeRowScoreDataset();
        Map<String, String> prefixMap = new HashMap<>();
        List<TestBean> list = ds.stream(1, 3, Arrays.asList("name", "age"), prefixMap, TestBean.class).toList();
        assertEquals(2, list.size());
        assertEquals("Jane", list.get(0).name);
    }

    @Test
    public void testStreamWithColumnsAndRowType() {
        final RowDataset ds = createThreeRowScoreDataset();
        List<TestBean> list = ds.stream(Arrays.asList("name", "age"), TestBean.class).toList();
        assertEquals(3, list.size());
        assertEquals("John", list.get(0).name);
    }

    @Test
    public void testStreamWithColumnsAndRowSupplier() {
        final RowDataset ds = createThreeRowScoreDataset();
        List<Map<String, Object>> list = ds.stream(Arrays.asList("name", "age"), (IntFunction<Map<String, Object>>) size -> new HashMap<>()).toList();
        assertEquals(3, list.size());
        assertEquals("John", list.get(0).get("name"));
    }

    @Test
    public void testStreamWithPrefixMapAndRange() {
        final RowDataset ds = createThreeRowScoreDataset();
        Map<String, String> prefixMap = new HashMap<>();
        List<TestBean> list = ds.stream(0, 2, prefixMap, TestBean.class).toList();
        assertEquals(2, list.size());
        assertEquals("John", list.get(0).name);
    }

    // ========== stream - advance/count internal methods ==========

    @Test
    public void testStream_Tuple2_AdvanceAndCount() {
        List<String> names = dataset.stream(Tuple.of("id", "name"), (BiFunction<Integer, String, String>) (id, name) -> id + ":" + name).skip(2).toList();
        assertEquals(3, names.size());
    }

    @Test
    public void testStream_Tuple3_AdvanceAndCount() {
        List<String> names = dataset
                .stream(Tuple.of("id", "name", "age"), (TriFunction<Integer, String, Integer, String>) (id, name, age) -> id + ":" + name + ":" + age)
                .skip(2)
                .toList();
        assertEquals(3, names.size());
    }

    @Test
    public void testStream_WithIntObjFunction_AdvanceAndCount() {
        List<String> result = dataset
                .stream(Arrays.asList("id", "name"),
                        (com.landawn.abacus.util.function.IntObjFunction<DisposableObjArray, String>) (idx, arr) -> idx + ":" + arr.get(0))
                .skip(2)
                .toList();
        assertEquals(3, result.size());
    }

    @Test
    public void testStreamOperationsWithEmptyDataset() {
        Stream<Object> stream = emptyDs.stream("col1");
        assertEquals(0, stream.count());

        Stream<Object[]> rowStream = emptyDs.stream(Object[].class);
        assertEquals(0, rowStream.count());
    }

    @Test
    public void testStreamWithRowType() {
        Stream<Object[]> rowStream = ds1.stream(Object[].class);
        List<Object[]> rows = rowStream.toList();

        assertEquals(3, rows.size());
        assertEquals(3, rows.get(0).length);
    }

    @Test
    public void testStreamWithRowSupplier() {
        Stream<List> rowStream = ds1.stream(size -> new ArrayList<>(size));
        List<List> rows = rowStream.toList();

        assertEquals(3, rows.size());
    }

    @Test
    public void testStreamWithRowMapper() {
        Stream<String> stream = ds1.stream((rowIndex, array) -> "Row " + rowIndex + ": " + Arrays.toString(array.copy()));
        List<String> results = stream.toList();

        assertEquals(3, results.size());
        assertTrue(results.get(0).startsWith("Row 0:"));
    }

    @Test
    public void testStreamWithRowTypeAndRange() {
        final RowDataset ds = createThreeRowScoreDataset();
        List<TestBean> list = ds.stream(1, 3, TestBean.class).toList();
        assertEquals(2, list.size());
        assertEquals("Jane", list.get(0).name);
    }

    @Test
    public void testStreamWithRowSupplierAndRange() {
        final RowDataset ds = createThreeRowScoreDataset();
        List<Map<String, Object>> list = ds.stream(1, 3, (IntFunction<Map<String, Object>>) size -> new HashMap<>()).toList();
        assertEquals(2, list.size());
        assertEquals("Jane", list.get(0).get("name"));
    }

}
