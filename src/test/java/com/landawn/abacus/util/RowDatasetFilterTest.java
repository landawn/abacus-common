package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.NoCachingNoUpdating.DisposableObjArray;
import com.landawn.abacus.util.function.BiPredicate;
import com.landawn.abacus.util.function.Predicate;
import com.landawn.abacus.util.function.TriPredicate;

public class RowDatasetFilterTest extends RowDatasetTestSupport {
    @Test
    public void testFilterWithPredicate() {
        final RowDataset dataset = createFiveRowCityDataset();
        Predicate<DisposableObjArray> filter = arr -> ((Integer) arr.get(2)) > 30;
        Dataset filtered = dataset.filter(filter);

        assertNotNull(filtered);
        assertEquals(2, filtered.size());
    }

    @Test
    public void testFilterWithPredicateAndMax() {
        final RowDataset dataset = createFiveRowCityDataset();
        Predicate<DisposableObjArray> filter = arr -> ((Integer) arr.get(2)) > 25;
        Dataset filtered = dataset.filter(filter, 2);

        assertNotNull(filtered);
        assertEquals(2, filtered.size());
    }

    @Test
    public void testFilterByColumn() {
        final RowDataset dataset = createFiveRowCityDataset();
        Predicate<Object> filter = age -> ((Integer) age) >= 30;
        Dataset filtered = dataset.filter("age", filter);

        assertNotNull(filtered);
        assertEquals(3, filtered.size());
    }

    @Test
    public void testFilterByColumnWithMax() {
        final RowDataset dataset = createFiveRowCityDataset();
        Predicate<Object> filter = name -> ((String) name).startsWith("J");
        Dataset filtered = dataset.filter("name", filter, 1);

        assertNotNull(filtered);
        assertEquals(1, filtered.size());
    }

    @Test
    public void testFilterByMultipleColumns() {
        final RowDataset dataset = createFiveRowCityDataset();
        Predicate<DisposableObjArray> filter = arr -> "NYC".equals(arr.get(0)) && ((Integer) arr.get(1)) > 30;
        Dataset filtered = dataset.filter(CommonUtil.toList("city", "age"), filter);

        assertNotNull(filtered);
        assertEquals(1, filtered.size());
    }

    @Test
    public void testFilterByMultipleColumnsWithMax() {
        final RowDataset dataset = createFiveRowCityDataset();
        Predicate<DisposableObjArray> filter = arr -> arr.get(0) != null;
        Dataset filtered = dataset.filter(CommonUtil.toList("name", "city"), filter, 3);

        assertNotNull(filtered);
        assertEquals(3, filtered.size());
    }

    @Test
    public void testFilterByTuple2() {
        final RowDataset dataset = createFiveRowCityDataset();
        BiPredicate<Object, Object> filter = (name, age) -> ((Integer) age) > 30;
        Dataset filtered = dataset.filter(Tuple.of("name", "age"), filter);

        assertNotNull(filtered);
        assertEquals(2, filtered.size());
    }

    @Test
    public void testFilterByTuple2WithMax() {
        final RowDataset dataset = createFiveRowCityDataset();
        BiPredicate<Object, Object> filter = (id, name) -> ((Integer) id) <= 3;
        Dataset filtered = dataset.filter(Tuple.of("id", "name"), filter, 2);

        assertNotNull(filtered);
        assertEquals(2, filtered.size());
    }

    @Test
    public void testFilterByTuple3() {
        final RowDataset dataset = createFiveRowCityDataset();
        TriPredicate<Object, Object, Object> filter = (id, name, age) -> ((Integer) age) < 30;
        Dataset filtered = dataset.filter(Tuple.of("id", "name", "age"), filter);

        assertNotNull(filtered);
        assertEquals(2, filtered.size());
    }

    @Test
    public void testFilterByTuple3WithMax() {
        final RowDataset dataset = createFiveRowCityDataset();
        TriPredicate<Object, Object, Object> filter = (name, age, city) -> true;
        Dataset filtered = dataset.filter(Tuple.of("name", "age", "city"), filter, 2);

        assertNotNull(filtered);
        assertEquals(2, filtered.size());
    }

    @Test
    public void testFilterByTuple2_NegativeMax_ThrowsIllegalArgumentException() {
        final RowDataset dataset = createFiveRowCityDataset();
        final BiPredicate<Object, Object> filter = (name, age) -> true;

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> dataset.filter(Tuple.of("name", "age"), filter, -1));
        assertTrue(ex.getMessage().contains("max"));

        ex = assertThrows(IllegalArgumentException.class, () -> dataset.filter(0, dataset.size(), Tuple.of("name", "age"), filter, -1));
        assertTrue(ex.getMessage().contains("max"));
    }

    @Test
    public void testFilterByTuple3_NegativeMax_ThrowsIllegalArgumentException() {
        final RowDataset dataset = createFiveRowCityDataset();
        final TriPredicate<Object, Object, Object> filter = (name, age, city) -> true;

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> dataset.filter(Tuple.of("name", "age", "city"), filter, -1));
        assertTrue(ex.getMessage().contains("max"));

        ex = assertThrows(IllegalArgumentException.class, () -> dataset.filter(0, dataset.size(), Tuple.of("name", "age", "city"), filter, -1));
        assertTrue(ex.getMessage().contains("max"));
    }

    @Test
    public void testFilterResolvesColumnSelectionBeforeRowRange() {
        // Both Collection overloads checked the row range first, so the same pair of bad arguments reported
        // IndexOutOfBoundsException there and IllegalArgumentException from the String/Tuple2/Tuple3
        // overloads and from forEach(int, int, Collection, ...). The argument precedence is now: filter,
        // then the column selection, then the row range, then max.
        final RowDataset ds = createFiveRowCityDataset();
        final int badTo = ds.size() + 99;
        final Predicate<DisposableObjArray> any = row -> true;

        assertThrows(IllegalArgumentException.class, () -> ds.filter(0, badTo, "nope", (Predicate<Object>) v -> true));
        assertThrows(IllegalArgumentException.class, () -> ds.filter(0, badTo, Tuple.of("nope", "name"), (BiPredicate<Object, Object>) (a, b) -> true));

        // an unknown column name beats the bad row range, in the 4-arg and the 5-arg overload alike
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ds.filter(0, badTo, Arrays.asList("nope"), any));
        assertTrue(ex.getMessage().contains("nope"), ex.getMessage());

        ex = assertThrows(IllegalArgumentException.class, () -> ds.filter(0, badTo, Arrays.asList("nope"), any, 1));
        assertTrue(ex.getMessage().contains("nope"), ex.getMessage());

        // so does a null or an empty columnNames
        for (final Collection<String> noColumns : Arrays.<Collection<String>> asList(null, new ArrayList<>())) {
            ex = assertThrows(IllegalArgumentException.class, () -> ds.filter(0, badTo, noColumns, any));
            assertTrue(ex.getMessage().contains("columnNames"), ex.getMessage());

            ex = assertThrows(IllegalArgumentException.class, () -> ds.filter(0, badTo, noColumns, any, 1));
            assertTrue(ex.getMessage().contains("columnNames"), ex.getMessage());
        }

        // a valid column selection with a bad row range still reports the range
        assertThrows(IndexOutOfBoundsException.class, () -> ds.filter(0, badTo, Arrays.asList("name"), any));
        assertThrows(IndexOutOfBoundsException.class, () -> ds.filter(0, badTo, Arrays.asList("name"), any, 1));

        // A null filter now beats the bad row range in the 5-arg overload too: only the 4-arg overload used
        // to check 'filter' before the range, and the 5-arg one reported the range here. So it is the
        // null-filter message - not the unknown-column one - that both overloads must report.
        ex = assertThrows(IllegalArgumentException.class, () -> ds.filter(0, badTo, Arrays.asList("name"), (Predicate<DisposableObjArray>) null, 1));
        assertTrue(ex.getMessage().contains("filter"), ex.getMessage());

        ex = assertThrows(IllegalArgumentException.class, () -> ds.filter(0, badTo, Arrays.asList("nope"), (Predicate<DisposableObjArray>) null, 1));
        assertTrue(ex.getMessage().contains("filter"), ex.getMessage());
    }

    // ========== filter - empty dataset ==========

    @Test
    public void testFilter_Tuple2_EmptyDataset_ReturnsEmpty() {
        RowDataset ds = new RowDataset(new ArrayList<>(Arrays.asList("id", "name")), new ArrayList<>(Arrays.asList(new ArrayList<>(), new ArrayList<>())));
        Dataset result = ds.filter(Tuple.of("id", "name"), (BiPredicate<Integer, String>) (id, name) -> id > 0);
        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    public void testFilter_Tuple3_EmptyDataset_ReturnsEmpty() {
        RowDataset ds = new RowDataset(new ArrayList<>(Arrays.asList("id", "name", "age")),
                new ArrayList<>(Arrays.asList(new ArrayList<>(), new ArrayList<>(), new ArrayList<>())));
        Dataset result = ds.filter(Tuple.of("id", "name", "age"), (TriPredicate<Integer, String, Integer>) (id, name, age) -> id > 0);
        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    public void testFilter_SingleCol_EmptyDataset_ReturnsEmpty() {
        RowDataset ds = new RowDataset(new ArrayList<>(Arrays.asList("id")), new ArrayList<>(Arrays.asList(new ArrayList<>())));
        Dataset result = ds.filter("id", (Predicate<Integer>) v -> v > 0);
        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    public void testFilter_MultiCols_EmptyDataset_ReturnsEmpty() {
        RowDataset ds = new RowDataset(new ArrayList<>(Arrays.asList("id", "name")), new ArrayList<>(Arrays.asList(new ArrayList<>(), new ArrayList<>())));
        Dataset result = ds.filter(Arrays.asList("id", "name"), (Predicate<DisposableObjArray>) arr -> true);
        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    public void testFilterWithRowRange() {
        final RowDataset dataset = createFiveRowCityDataset();
        Predicate<DisposableObjArray> filter = arr -> true;
        Dataset filtered = dataset.filter(1, 4, filter);

        assertNotNull(filtered);
        assertEquals(3, filtered.size());
    }

    @Test
    public void testFilterWithRowRangeAndMax() {
        final RowDataset dataset = createFiveRowCityDataset();
        Predicate<DisposableObjArray> filter = arr -> true;
        Dataset filtered = dataset.filter(0, 5, filter, 3);

        assertNotNull(filtered);
        assertEquals(3, filtered.size());
    }

    @Test
    public void testFilterByColumnWithRowRange() {
        final RowDataset dataset = createFiveRowCityDataset();
        Predicate<Object> filter = city -> "NYC".equals(city);
        Dataset filtered = dataset.filter(0, 5, "city", filter);

        assertNotNull(filtered);
        assertEquals(2, filtered.size());
    }

    @Test
    public void testFilterByColumnWithRowRangeAndMax() {
        final RowDataset dataset = createFiveRowCityDataset();
        Predicate<Object> filter = city -> city != null;
        Dataset filtered = dataset.filter(1, 4, "city", filter, 2);

        assertNotNull(filtered);
        assertEquals(2, filtered.size());
    }

    @Test
    public void testFilterByMultipleColumnsWithRowRange() {
        final RowDataset dataset = createFiveRowCityDataset();
        Predicate<DisposableObjArray> filter = arr -> true;
        Dataset filtered = dataset.filter(1, 3, CommonUtil.toList("name", "age"), filter);

        assertNotNull(filtered);
        assertEquals(2, filtered.size());
    }

    @Test
    public void testFilterByMultipleColumnsWithRowRangeAndMax() {
        final RowDataset dataset = createFiveRowCityDataset();
        Predicate<DisposableObjArray> filter = arr -> true;
        Dataset filtered = dataset.filter(0, 5, CommonUtil.toList("id", "name"), filter, 4);

        assertNotNull(filtered);
        assertEquals(4, filtered.size());
    }

    @Test
    public void testFilterByTuple2WithRowRange() {
        final RowDataset dataset = createFiveRowCityDataset();
        BiPredicate<Object, Object> filter = (name, city) -> "LA".equals(city);
        Dataset filtered = dataset.filter(1, 5, Tuple.of("name", "city"), filter);

        assertNotNull(filtered);
        assertEquals(2, filtered.size());
    }

    @Test
    public void testFilterByTuple2WithRowRangeAndMax() {
        final RowDataset dataset = createFiveRowCityDataset();
        BiPredicate<Object, Object> filter = (id, age) -> true;
        Dataset filtered = dataset.filter(0, 5, Tuple.of("id", "age"), filter, 3);

        assertNotNull(filtered);
        assertEquals(3, filtered.size());
    }

    @Test
    public void testFilterByTuple3WithRowRange() {
        final RowDataset dataset = createFiveRowCityDataset();
        TriPredicate<Object, Object, Object> filter = (id, name, age) -> ((Integer) id) > 2;
        Dataset filtered = dataset.filter(1, 5, Tuple.of("id", "name", "age"), filter);

        assertNotNull(filtered);
        assertEquals(3, filtered.size());
    }

    @Test
    public void testFilterByTuple3WithRowRangeAndMax() {
        final RowDataset dataset = createFiveRowCityDataset();
        TriPredicate<Object, Object, Object> filter = (id, age, city) -> true;
        Dataset filtered = dataset.filter(0, 4, Tuple.of("id", "age", "city"), filter, 3);

        assertNotNull(filtered);
        assertEquals(3, filtered.size());
    }

}
