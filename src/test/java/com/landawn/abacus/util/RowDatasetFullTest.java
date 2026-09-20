package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.If.OrElse;
import com.landawn.abacus.util.NoCachingNoUpdating.DisposableObjArray;
import com.landawn.abacus.util.Tuple.Tuple2;
import com.landawn.abacus.util.Tuple.Tuple3;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.function.BiFunction;
import com.landawn.abacus.util.function.Function;
import com.landawn.abacus.util.function.IntFunction;
import com.landawn.abacus.util.function.IntObjFunction;
import com.landawn.abacus.util.function.Predicate;
import com.landawn.abacus.util.function.TriFunction;
import com.landawn.abacus.util.stream.ObjIteratorEx;
import com.landawn.abacus.util.stream.Stream;

public class RowDatasetFullTest extends RowDatasetTestSupport {
    @Test
    public void testSize() {
        assertEquals(5, dataset.size());
        assertEquals(0, emptyDataset.size());
    }

    /**
     * C-014: the positional {@code addColumns} accepts {@code columnCount()} (append) and seeds a Dataset that has
     * no columns yet, neither of which its javadoc described.
     */
    @Test
    public void test20260906_addColumnsPositionalAppendsAtColumnCountAndSeedsAnEmptyDataset() {
        final Dataset appended = Dataset.rows(CommonUtil.asList("id"), new Object[][] { { 1 } });
        appended.addColumns(appended.columnCount(), CommonUtil.asList("z"), CommonUtil.asList(CommonUtil.asList(9)));
        assertEquals(CommonUtil.asList("id", "z"), appended.columnNames());

        final Dataset seeded = CommonUtil.newEmptyDataset();
        seeded.addColumns(0, CommonUtil.asList("a", "b"), CommonUtil.asList(CommonUtil.asList(1, 2), CommonUtil.asList("x", "y")));
        assertEquals(CommonUtil.asList("a", "b"), seeded.columnNames());
        assertEquals(2, seeded.size());

        assertThrows(IndexOutOfBoundsException.class, () -> Dataset.rows(CommonUtil.asList("id"), new Object[][] { { 1 } })
                .addColumns(2, CommonUtil.asList("z"), CommonUtil.asList(CommonUtil.asList(9))));
    }

    /**
     * C-012: {@code renameColumns(Function)} validates the <i>resulting</i> name list, not the current one - its
     * javadoc claimed an exception for any new name that already existed, which no input can trigger.
     */
    @Test
    public void test20260906_renameColumnsFunctionValidatesTheResultingNameList() {
        final Dataset permuted = Dataset.rows(CommonUtil.asList("a", "b"), new Object[][] { { 1, 2 } });
        permuted.renameColumns(name -> "a".equals(name) ? "b" : "a");
        assertEquals(CommonUtil.asList("b", "a"), permuted.columnNames());
        assertEquals(Integer.valueOf(1), permuted.<Integer> get(0, 0)); // the data does not move with the names

        final Dataset prefixed = Dataset.rows(CommonUtil.asList("a", "za"), new Object[][] { { 1, 2 } });
        prefixed.renameColumns(name -> "z" + name); // "za" already exists and is still accepted
        assertEquals(CommonUtil.asList("za", "zza"), prefixed.columnNames());

        final Dataset collide = Dataset.rows(CommonUtil.asList("a", "b"), new Object[][] { { 1, 2 } });
        assertThrows(IllegalArgumentException.class, () -> collide.renameColumns(name -> "x"));
        assertThrows(IllegalArgumentException.class, () -> collide.renameColumns((java.util.function.Function<String, String>) null));
        assertThrows(IllegalArgumentException.class, () -> collide.renameColumns(name -> null));
    }

    @Test
    @SuppressWarnings("rawtypes")
    public void testEmptyDatasetRejectsNullCallbacks() {
        final Function<DisposableObjArray, Comparable> nullKeyExtractor = null;
        final IntObjFunction<DisposableObjArray, Object> nullRowMapper = null;
        final BiFunction<Object, Object, Object> nullBiMapper = null;
        final TriFunction<Object, Object, Object, Object> nullTriMapper = null;

        assertThrows(IllegalArgumentException.class, () -> emptyDs.sortBy(emptyDs.columnNames(), nullKeyExtractor));
        assertThrows(IllegalArgumentException.class, () -> emptyDs.topBy(emptyDs.columnNames(), 1, nullKeyExtractor));
        assertThrows(IllegalArgumentException.class, () -> emptyDs.stream(emptyDs.columnNames(), nullRowMapper));
        assertThrows(IllegalArgumentException.class, () -> emptyDs.stream(new Tuple2<>("col1", "col2"), nullBiMapper));
        assertThrows(IllegalArgumentException.class, () -> emptyDs.stream(new Tuple3<>("col1", "col2", "col1"), nullTriMapper));
    }

    @Test
    public void testCursorAccessorsOnEmptyDatasetReportNoCurrentRow() {
        final Dataset ds = twoColumnDataset();
        ds.moveToRow(2);
        ds.removeRows(0, 3);

        assertEquals(0, ds.size());
        assertEquals(0, ds.currentRowIndex());

        final IndexOutOfBoundsException e = assertThrows(IndexOutOfBoundsException.class, () -> ds.get("id"));
        assertTrue(e.getMessage().contains("no rows"));

        assertThrows(IndexOutOfBoundsException.class, () -> ds.get(0));
        assertThrows(IndexOutOfBoundsException.class, () -> ds.isNull("id"));
        assertThrows(IndexOutOfBoundsException.class, () -> ds.set("id", 1));
        assertThrows(IndexOutOfBoundsException.class, () -> ds.getInt("id"));
    }

    @Test
    public void testJoinReportsMissingRightColumnAgainstTheRightDataset() {
        final Dataset left = Dataset.rows(CommonUtil.asList("id", "k", "name"), new Object[][] { { 1, 9, "a" } });
        final Dataset right = Dataset.rows(CommonUtil.asList("rid", "rk"), new Object[][] { { 1, 9 } });

        final Map<String, String> on = new LinkedHashMap<>();
        on.put("id", "rid");
        on.put("k", "NOPE");

        final List<Runnable> joins = Arrays.asList(() -> left.innerJoin(right, on), () -> left.leftJoin(right, on), () -> left.rightJoin(right, on),
                () -> left.fullJoin(right, on));

        for (final Runnable join : joins) {
            final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, join::run);
            assertTrue(e.getMessage().contains("right Dataset"), e.getMessage());
            assertTrue(e.getMessage().contains("NOPE"), e.getMessage());
        }
    }

    /**
     * C-027/C-028: an <i>empty</i> column selection is "nothing to do" for the three column mutators and "no
     * columns" for {@code println} — while {@code null} is still rejected by the mutators — and a {@code null}
     * or empty collection in {@code addColumns} adds an all-{@code null} column. The class-level
     * column-selection paragraph now enumerates all of this.
     */
    @Test
    public void test20260906c2_emptyColumnSelectionAndNullColumnsInAddColumns() throws Exception {
        final Dataset ds = Dataset.rows(CommonUtil.asList("id", "name"), new Object[][] { { 1, "a" }, { 2, "b" } });

        // C-027 - empty is a no-op, null is still an error
        ds.updateColumns(CommonUtil.emptyList(), (i, c, v) -> v);
        ds.moveColumns(CommonUtil.<String> emptyList(), 99);
        ds.removeColumns(CommonUtil.emptyList());
        assertEquals(CommonUtil.asList("id", "name"), ds.columnNames());
        assertThrows(IllegalArgumentException.class, () -> ds.updateColumns(null, (i, c, v) -> v));

        final StringBuilder rendered = new StringBuilder();
        ds.println(0, 1, (java.util.Collection<String>) null, rendered);
        assertTrue(rendered.length() > 0, "println with no columns still prints an empty box");
        assertFalse(rendered.toString().contains("id"), rendered.toString());

        // C-028 - a null or empty column becomes an all-null column of the right length
        final Dataset padded = Dataset.rows(CommonUtil.asList("id"), new Object[][] { { 1 }, { 2 } });
        padded.addColumns(CommonUtil.asList("a", "b"), CommonUtil.asList(null, CommonUtil.emptyList()));
        assertEquals(CommonUtil.asList("id", "a", "b"), padded.columnNames());
        assertEquals(2, padded.size());
        assertNull(padded.get(0, padded.getColumnIndex("a")));
        assertNull(padded.get(1, padded.getColumnIndex("b")));

        // ... and the two rejections the javadoc now lists
        assertThrows(IllegalArgumentException.class, () -> Dataset.rows(CommonUtil.asList("id"), new Object[][] { { 1 }, { 2 } })
                .addColumns(CommonUtil.asList("c", "c"), CommonUtil.asList(CommonUtil.asList(1, 2), CommonUtil.asList(3, 4))));
        assertThrows(IllegalArgumentException.class, () -> Dataset.rows(CommonUtil.asList("id"), new Object[][] { { 1 }, { 2 } })
                .addColumns(CommonUtil.asList(""), CommonUtil.asList(CommonUtil.asList(1, 2))));
    }

    @Test
    public void testCombineColumnsWithLiveColumnNameView() {
        // regression: passing the live columnNames() view destroyed the dataset — addColumn mutated
        // the view, so removeColumns then also removed the freshly combined column
        final RowDataset ds = new RowDataset(new ArrayList<>(Arrays.asList("a", "b")),
                new ArrayList<>(Arrays.asList(new ArrayList<>(Arrays.asList(1, 2)), new ArrayList<>(Arrays.asList(10, 20)))));

        ds.combineColumns(ds.columnNames(), "c", arr -> (Integer) arr.get(0) + (Integer) arr.get(1));

        assertEquals(Arrays.asList("c"), ds.columnNames());
        assertEquals(Arrays.asList(11, 22), ds.getColumn("c"));
    }

    @Test
    public void testCombineColumnsValidatesBeforeMappingRows() {
        final int[] mapperCalls = { 0 };

        assertThrows(IllegalArgumentException.class, () -> dataset.combineColumns((Collection<String>) null, "combined", arr -> mapperCalls[0]++));
        assertThrows(IllegalArgumentException.class, () -> dataset.combineColumns(Arrays.asList("id", "age"), "", arr -> mapperCalls[0]++));
        assertEquals(0, mapperCalls[0]);
    }

    /**
     * C-018b: the {@code Tuple3} form of {@code combineColumns} enforces the same four rejections as the
     * {@code Tuple2} form, which only the latter's {@code @throws} listed.
     */
    @Test
    public void test20260906c2_combineColumnsTuple3Validations() {
        assertThrows(IllegalArgumentException.class, () -> newAbcDataset().combineColumns(Tuple.of("a", "a", "b"), "z", (x, y, z) -> x));
        assertThrows(IllegalArgumentException.class, () -> newAbcDataset().combineColumns(Tuple.of("a", "b", "c"), "z", null));
        assertThrows(IllegalArgumentException.class, () -> newAbcDataset().combineColumns(Tuple.of("a", "b", "c"), "", (x, y, z) -> x));
        assertThrows(IllegalArgumentException.class, () -> newAbcDataset().combineColumns(Tuple.of("a", "b", "c"), "a", (x, y, z) -> x));

        // the valid call still works
        final Dataset ok = newAbcDataset();
        ok.combineColumns(Tuple.of("a", "b", "c"), "z", (x, y, z) -> "" + x + y + z);
        assertEquals(CommonUtil.asList("z"), ok.columnNames());
    }

    @Test
    public void testDivideColumn_DuplicateNewColumnNames_Tuple2() {
        final List<String> cols = new ArrayList<>(Arrays.asList("id", "val"));
        final List<List<Object>> data = new ArrayList<>();
        data.add(new ArrayList<>(Arrays.asList(1, 2)));
        data.add(new ArrayList<>(Arrays.asList("a-b", "c-d")));
        final RowDataset ds = new RowDataset(cols, data);

        assertThrows(IllegalArgumentException.class,
                () -> ds.divideColumn("val", new Tuple2<>("dup", "dup"), (BiConsumer<Object, Pair<Object, Object>>) (v, p) -> {
                    final String[] parts = ((String) v).split("-");
                    p.set(parts[0], parts[1]);
                }));

        // dataset must not be mutated by the failed call
        assertEquals(2, ds.columnCount());
        assertEquals(2, ds.size());
        assertTrue(ds.containsColumn("val"));
    }

    @Test
    public void testDivideColumn_DuplicateNewColumnNames_Tuple3() {
        final List<String> cols = new ArrayList<>(Arrays.asList("id", "val"));
        final List<List<Object>> data = new ArrayList<>();
        data.add(new ArrayList<>(Arrays.asList(1, 2)));
        data.add(new ArrayList<>(Arrays.asList("a-b-c", "d-e-f")));
        final RowDataset ds = new RowDataset(cols, data);

        assertThrows(IllegalArgumentException.class,
                () -> ds.divideColumn("val", new Tuple3<>("x", "y", "x"), (BiConsumer<Object, Triple<Object, Object, Object>>) (v, t) -> {
                    final String[] parts = ((String) v).split("-");
                    t.set(parts[0], parts[1], parts[2]);
                }));

        assertEquals(2, ds.columnCount());
        assertTrue(ds.containsColumn("val"));
    }

    @Test
    public void testDivideColumn_DuplicateNewColumnNames_Collection_Function() {
        final List<String> cols = new ArrayList<>(Arrays.asList("id", "val"));
        final List<List<Object>> data = new ArrayList<>();
        data.add(new ArrayList<>(Arrays.asList(1, 2)));
        data.add(new ArrayList<>(Arrays.asList("a-b", "c-d")));
        final RowDataset ds = new RowDataset(cols, data);

        assertThrows(IllegalArgumentException.class,
                () -> ds.divideColumn("val", Arrays.asList("dup", "dup"), (Function<Object, ? extends List<?>>) v -> Arrays.asList(((String) v).split("-"))));

        assertEquals(2, ds.columnCount());
        assertTrue(ds.containsColumn("val"));
    }

    @Test
    public void testDivideColumn_DuplicateNewColumnNames_Collection_BiConsumer() {
        final List<String> cols = new ArrayList<>(Arrays.asList("id", "val"));
        final List<List<Object>> data = new ArrayList<>();
        data.add(new ArrayList<>(Arrays.asList(1, 2)));
        data.add(new ArrayList<>(Arrays.asList("a-b", "c-d")));
        final RowDataset ds = new RowDataset(cols, data);

        assertThrows(IllegalArgumentException.class, () -> ds.divideColumn("val", Arrays.asList("dup", "dup"), (BiConsumer<Object, Object[]>) (v, out) -> {
            final String[] parts = ((String) v).split("-");
            out[0] = parts[0];
            out[1] = parts[1];
        }));

        assertEquals(2, ds.columnCount());
        assertTrue(ds.containsColumn("val"));
    }

    /**
     * C-013: the Tuple overloads of {@code divideColumn} hand the same {@code Pair}/{@code Triple} to every row,
     * and reject duplicate/empty new names - none of which their javadoc said.
     */
    @Test
    public void test20260906_divideColumnTupleOverloadsReuseOneBufferAndRejectDuplicateNames() {
        final Dataset ds = Dataset.rows(CommonUtil.asList("v"), new Object[][] { { "a" }, { "b" } });
        final List<Object> handedOut = new ArrayList<>();
        ds.divideColumn("v", Tuple.of("x", "y"), (v, pair) -> {
            pair.set(v, v);
            handedOut.add(pair);
        });
        Assertions.assertSame(handedOut.get(0), handedOut.get(1));

        final Dataset other = Dataset.rows(CommonUtil.asList("v"), new Object[][] { { "a" } });
        assertThrows(IllegalArgumentException.class, () -> other.divideColumn("v", Tuple.of("x", "x"), (v, pair) -> {
        }));
        assertThrows(IllegalArgumentException.class, () -> other.divideColumn("v", Tuple.of("x", ""), (v, pair) -> {
        }));
        assertThrows(IllegalArgumentException.class, () -> other.divideColumn("v", Tuple.of("x", "y", "x"), (v, triple) -> {
        }));
    }

    @Test
    public void testSingleColumnStreamIsFailFastOnStructuralChange() {
        final Dataset ds = twoColumnDataset();
        final com.landawn.abacus.util.stream.Stream<Object> stream = ds.stream("id");
        ds.removeRow(0);

        assertThrows(ConcurrentModificationException.class, stream::toList);
    }

    @Test
    public void testSingleColumnStreamSeesValueWritesAndStillWorks() {
        final Dataset ds = twoColumnDataset();
        final com.landawn.abacus.util.stream.Stream<Object> stream = ds.stream("id");
        ds.set(0, 0, 77);

        assertEquals(Arrays.asList(77, 2, 3), stream.toList());

        // The row-range overload and the count()/skip() shortcuts must agree with plain iteration.
        assertEquals(2, ds.stream(1, 3, "id").count());
        assertEquals(Arrays.asList(3), ds.stream(1, 3, "id").skip(1).toList());
        assertEquals(Arrays.asList(2, 3), ds.stream(1, 3, "id").toList());
    }

    @Test
    public void testSingleColumnStreamBulkOperationsMatchElementWiseIteration() {
        // toArray/toList are overridden on the fail-fast iterator so they still copy the backing column in one
        // shot; they must agree with plain iteration for every cursor position.
        assertEquals(Arrays.asList(1, 2, 3), twoColumnDataset().stream("id").toList());
        assertEquals(Arrays.asList(2, 3), twoColumnDataset().stream(1, 3, "id").toList());
        assertArrayEquals(new Object[] { 1, 2, 3 }, twoColumnDataset().stream("id").toArray());
        assertArrayEquals(new Object[] { 2, 3 }, twoColumnDataset().stream(1, 3, "id").toArray());

        // ...including after the cursor has already been advanced by skip().
        assertEquals(Arrays.asList(2, 3), twoColumnDataset().stream("id").skip(1).toList());
        assertArrayEquals(new Object[] { 3 }, twoColumnDataset().stream("id").skip(2).toArray());
        assertTrue(twoColumnDataset().stream("id").skip(99).toList().isEmpty());

        assertEquals(3, twoColumnDataset().stream("id").count());
        assertEquals(2, twoColumnDataset().stream("id").skip(1).count());
        assertEquals(Arrays.asList(1, 2), twoColumnDataset().stream("id").limit(2).toList());
        assertTrue(twoColumnDataset().stream(1, 1, "id").toList().isEmpty());
        assertTrue(Dataset.rows(CommonUtil.asList("id"), new Object[0][]).stream("id").toList().isEmpty());
    }

    @Test
    public void testSingleColumnStreamToListIsAnIndependentMutableSnapshot() {
        // The override must not hand back the backing column, nor a live subList view of it.
        final Dataset ds = twoColumnDataset();
        final List<Object> collected = ds.stream("id").toList();

        collected.add(99);
        assertEquals(4, collected.size());
        assertEquals(3, ds.size());

        ds.set(0, 0, 777);
        assertEquals(Integer.valueOf(1), collected.get(0));
    }

    @Test
    public void testSingleColumnStreamBulkOperationsAreAlsoFailFast() {
        final Dataset d1 = twoColumnDataset();
        final com.landawn.abacus.util.stream.Stream<Object> toList = d1.stream("id");
        d1.addRow(new Object[] { 4, "d" });
        assertThrows(ConcurrentModificationException.class, toList::toList);

        final Dataset d2 = twoColumnDataset();
        final com.landawn.abacus.util.stream.Stream<Object> toArray = d2.stream("id");
        d2.removeRow(0);
        assertThrows(ConcurrentModificationException.class, toArray::toArray);

        final Dataset d3 = twoColumnDataset();
        final com.landawn.abacus.util.stream.Stream<Object> count = d3.stream("id");
        d3.renameColumn("name", "label");
        assertThrows(ConcurrentModificationException.class, count::count);
    }

    @Test
    public void testRowStreamOptimizedCountAndToArrayExhaustIterators() throws ReflectiveOperationException {
        final RowDataset ds = createThreeRowScoreDataset();

        final ObjIteratorEx<TestBean> beanRows = iteratorEx(ds.stream(Arrays.asList("name", "age"), TestBean.class));
        assertNotNull(beanRows.next());
        assertEquals(2, beanRows.count());
        assertFalse(beanRows.hasNext());

        final ObjIteratorEx<String> mappedRows = iteratorEx(
                ds.stream(Arrays.asList("name"), (int rowIndex, DisposableObjArray row) -> rowIndex + ":" + row.get(0)));
        assertEquals(3, mappedRows.count());
        assertFalse(mappedRows.hasNext());

        final ObjIteratorEx<String> pairRows = iteratorEx(ds.stream(Tuple.of("name", "age"), (BiFunction<String, Integer, String>) (name, age) -> name + age));
        assertEquals(3, pairRows.count());
        assertFalse(pairRows.hasNext());

        final ObjIteratorEx<String> tripleRows = iteratorEx(
                ds.stream(Tuple.of("id", "name", "age"), (TriFunction<Integer, String, Integer, String>) (id, name, age) -> id + name + age));
        assertEquals(3, tripleRows.count());
        assertFalse(tripleRows.hasNext());

        final ObjIteratorEx<TestBean> arrayRows = iteratorEx(ds.stream(Arrays.asList("name", "age"), TestBean.class));
        assertEquals(3, arrayRows.toArray(new TestBean[0]).length);
        assertFalse(arrayRows.hasNext());
    }

    @Test
    public void testRowRemovalClampsOrResetsCurrentRowIndex() {
        dataset.moveToRow(4);
        dataset.removeRow(4);

        assertEquals(3, dataset.currentRowIndex());
        assertEquals(4, dataset.<Integer> get("id"));

        dataset.removeRows(0, dataset.size());
        assertEquals(0, dataset.currentRowIndex());
        assertEquals(0, dataset.size());

        dataset.addRow(new Object[] { 6, "Frank", 40, 80000.0 });
        assertEquals(6, dataset.<Integer> get("id"));

        dataset.clear();
        assertEquals(0, dataset.currentRowIndex());

        final RowDataset oneColumn = new RowDataset(new ArrayList<>(Arrays.asList("value")),
                new ArrayList<>(Arrays.asList(new ArrayList<>(Arrays.asList(1, 2)))));
        oneColumn.moveToRow(1);
        assertThrows(IllegalArgumentException.class, () -> oneColumn.removeColumn("value"));
        assertEquals(2, oneColumn.size());
        oneColumn.clear();
        oneColumn.removeColumn("value");
        assertEquals(0, oneColumn.size());
        assertEquals(0, oneColumn.currentRowIndex());
    }

    @Test
    public void testRowSuppliersRejectNullAndShortResults() {
        assertThrows(IllegalArgumentException.class, () -> dataset.getRow(0, (IntFunction<Object[]>) columnCount -> null));
        assertThrows(IllegalArgumentException.class, () -> dataset.getRow(0, (IntFunction<Object[]>) columnCount -> new Object[columnCount - 1]));

        final int[] listSupplierCalls = { 0 };
        final IntFunction<Object[]> listRowSupplier = columnCount -> listSupplierCalls[0]++ == 0 ? new Object[columnCount] : null;
        assertThrows(IllegalArgumentException.class, () -> dataset.toList(listRowSupplier));

        final int[] streamSupplierCalls = { 0 };
        final IntFunction<Object[]> streamRowSupplier = columnCount -> streamSupplierCalls[0]++ == 0 ? new Object[columnCount] : null;
        assertThrows(IllegalArgumentException.class, () -> dataset.stream(streamRowSupplier).toList());
    }

    @Test
    public void testRowAccessorReadsAndWritesWithoutTheCursor() {
        final Dataset ds = twoColumnDataset();
        ds.moveToRow(0);

        final Dataset.Row row = ds.row(1);

        assertEquals(1, row.rowIndex());
        assertEquals(2, row.columnCount());
        assertEquals(Integer.valueOf(2), row.get("id"));
        assertEquals("b", row.get(1));
        assertEquals(2, row.getInt("id"));
        assertEquals(2L, row.getLong(0));
        assertEquals(2.0, row.getDouble("id"), 0.0);
        assertFalse(row.isNull("id"));
        assertEquals(1, row.columnIndex("name"));
        assertArrayEquals(new Object[] { 2, "b" }, row.toArray());

        // Reading through the accessor must not have moved the shared cursor.
        assertEquals(0, ds.currentRowIndex());

        row.set("name", "B");
        assertEquals("B", ds.get(1, 1));
        row.set(0, 20);
        assertEquals(Integer.valueOf(20), ds.get(1, 0));

        // ...and writing did not move it either.
        assertEquals(0, ds.currentRowIndex());
    }

    @Test
    public void testRowAccessorTypedGettersOnNullAndValidation() {
        final Dataset ds = Dataset.rows(CommonUtil.asList("flag", "ch", "num"), new Object[][] { { null, null, null } });
        final Dataset.Row row = ds.row(0);

        assertTrue(row.isNull("flag"));
        assertFalse(row.getBoolean("flag"));
        assertEquals(0, row.getChar("ch"));
        assertEquals(0, row.getByte("num"));
        assertEquals(0, row.getShort("num"));
        assertEquals(0, row.getInt("num"));
        assertEquals(0L, row.getLong("num"));
        assertEquals(0f, row.getFloat("num"), 0f);
        assertEquals(0d, row.getDouble("num"), 0d);

        assertThrows(IllegalArgumentException.class, () -> row.get("nope"));
        assertThrows(IndexOutOfBoundsException.class, () -> row.get(3));
        assertThrows(IndexOutOfBoundsException.class, () -> ds.row(1));
    }

    @Test
    public void testRowAccessorIsPositionalAndRespectsFrozen() {
        final Dataset ds = twoColumnDataset();
        final Dataset.Row row = ds.row(0);

        // Structural changes invalidate a row accessor; reacquire the row at its new position.
        ds.sortBy("id", Comparator.reverseOrder());
        assertThrows(ConcurrentModificationException.class, () -> row.get("id"));
        final Dataset.Row current = ds.row(0);
        assertEquals(Integer.valueOf(3), current.get("id"));

        ds.freeze();
        assertThrows(IllegalStateException.class, () -> current.set("id", 9));
    }

    @Test
    public void testCursorAccessorsValidateColumnIndex() {
        final Dataset ds = twoColumnDataset();
        ds.moveToRow(1);

        assertEquals(Integer.valueOf(2), ds.get(0));
        assertThrows(IndexOutOfBoundsException.class, () -> ds.get(2));
        assertThrows(IndexOutOfBoundsException.class, () -> ds.set(2, "x"));
    }

    @Test
    public void testFirstAndLastRowValidateArgumentsWhenEmpty() {
        final IntFunction<Object[]> nullRowSupplier = null;

        assertThrows(IllegalArgumentException.class, () -> emptyDs.firstRow(null, Object[].class));
        assertThrows(IllegalArgumentException.class, () -> emptyDs.firstRow(Arrays.asList("col1"), String.class));
        assertThrows(IllegalArgumentException.class, () -> emptyDs.firstRow(Arrays.asList("col1"), nullRowSupplier));

        assertThrows(IllegalArgumentException.class, () -> emptyDs.lastRow(null, Object[].class));
        assertThrows(IllegalArgumentException.class, () -> emptyDs.lastRow(Arrays.asList("col1"), String.class));
        assertThrows(IllegalArgumentException.class, () -> emptyDs.lastRow(Arrays.asList("col1"), nullRowSupplier));
    }

    @Test
    public void testValueWritesDoNotInvalidateLiveIterators() {
        final Dataset ds = twoColumnDataset();
        final BiIterator<Object, Object> iter = ds.iterator("id", "name");

        ds.set(0, 0, 99);
        ds.updateAll(v -> v);
        ds.replaceIf(v -> false, "unused");
        ds.updateColumn("name", v -> v);
        ds.updateRow(0, v -> v);

        // None of the above changes the row count, the column set or any ordering, so the iterator stays valid.
        final List<Object> ids = new ArrayList<>();
        iter.forEachRemaining((a, b) -> ids.add(a));
        assertEquals(Arrays.asList(99, 2, 3), ids);
    }

    @Test
    public void testStructuralChangesStillInvalidateLiveIterators() {
        final Dataset ds = twoColumnDataset();
        final BiIterator<Object, Object> afterAddRow = ds.iterator("id", "name");
        ds.addRow(new Object[] { 4, "d" });
        assertThrows(ConcurrentModificationException.class, () -> afterAddRow.forEachRemaining((a, b) -> {
        }));

        final Dataset ds2 = twoColumnDataset();
        final BiIterator<Object, Object> afterSort = ds2.iterator("id", "name");
        ds2.sortBy("id", Comparator.reverseOrder());
        assertThrows(ConcurrentModificationException.class, () -> afterSort.forEachRemaining((a, b) -> {
        }));

        final Dataset ds3 = twoColumnDataset();
        final BiIterator<Object, Object> afterRename = ds3.iterator("id", "name");
        ds3.renameColumn("name", "label");
        assertThrows(ConcurrentModificationException.class, () -> afterRename.forEachRemaining((a, b) -> {
        }));
    }

    /**
     * C-009: the aggregate column selection and the row type were only resolved by the {@code toList(..)} call
     * that the {@code size == 0} early return skips, so a mistyped aggregate column or an unsupported row type
     * was silently accepted whenever the Dataset happened to have no rows - the shape a query result often has.
     */
    @Test
    public void test20260906_groupByValidatesAggregateArgumentsOnAnEmptyDataset() {
        final Dataset noRows = Dataset.rows(CommonUtil.asList("a", "b", "c"), new Object[0][]);
        final Dataset oneRow = Dataset.rows(CommonUtil.asList("a", "b", "c"), new Object[][] { { 1, 2, 3 } });

        for (final Dataset ds : new Dataset[] { noRows, oneRow }) {
            assertThrows(IllegalArgumentException.class, () -> ds.groupBy(CommonUtil.asList("a", "b"), CommonUtil.asList("nope"), "agg", Object[].class));
            assertThrows(IllegalArgumentException.class, () -> ds.groupBy(CommonUtil.asList("a", "b"), CommonUtil.asList("c"), "agg", String.class));
            // Fn.identity() routes straight back to the plain overload above, so a NON-identity extractor is
            // needed to reach the second (textually identical) check site
            assertThrows(IllegalArgumentException.class, () -> ds.groupBy(CommonUtil.asList("a", "b"), (final DisposableObjArray row) -> row.join("-"),
                    CommonUtil.asList("nope"), "agg", Object[].class));
            assertThrows(IllegalArgumentException.class, () -> ds.groupBy(CommonUtil.asList("a", "b"), (final DisposableObjArray row) -> row.join("-"),
                    CommonUtil.asList("c"), "agg", String.class));
            assertThrows(IllegalArgumentException.class,
                    () -> ds.groupBy(CommonUtil.asList("a", "b"), Fn.<DisposableObjArray> identity(), CommonUtil.asList("nope"), "agg", Object[].class));
        }

        final Dataset grouped = noRows.groupBy(CommonUtil.asList("a", "b"), CommonUtil.asList("c"), "agg", Object[].class);
        assertEquals(CommonUtil.asList("a", "b", "agg"), grouped.columnNames());
        assertEquals(0, grouped.size());
    }

    /**
     * C-010: rollup/cube document "arguments are validated when this method is called, before the Stream is
     * returned", but a result column name colliding with a key column was left to the {@code groupBy} inside the
     * lazy {@code map(..)}, so it surfaced at an arbitrary later consumption point.
     */
    @Test
    public void test20260906_rollupAndCubeRejectAResultNameCollidingWithAKeyColumnEagerly() {
        final Dataset ds = Dataset.rows(CommonUtil.asList("a", "b", "c"), new Object[][] { { 1, 2, 3 } });
        final List<String> keys = CommonUtil.asList("a", "b");
        final List<String> aggregateOn = CommonUtil.asList("c");

        assertThrows(IllegalArgumentException.class, () -> ds.rollup(keys, "c", "a", com.landawn.abacus.util.stream.Collectors.countingToInt()));
        assertThrows(IllegalArgumentException.class, () -> ds.rollup(keys, aggregateOn, "a", Object[].class));
        assertThrows(IllegalArgumentException.class, () -> ds.rollup(keys, aggregateOn, "a", com.landawn.abacus.util.stream.Collectors.toList()));
        assertThrows(IllegalArgumentException.class,
                () -> ds.rollup(keys, Fn.<DisposableObjArray> identity(), "c", "a", com.landawn.abacus.util.stream.Collectors.countingToInt()));
        assertThrows(IllegalArgumentException.class, () -> ds.rollup(keys, Fn.<DisposableObjArray> identity(), aggregateOn, "a", Object[].class));

        assertThrows(IllegalArgumentException.class, () -> ds.cube(keys, "c", "b", com.landawn.abacus.util.stream.Collectors.countingToInt()));
        assertThrows(IllegalArgumentException.class, () -> ds.cube(keys, aggregateOn, "b", Object[].class));
        assertThrows(IllegalArgumentException.class, () -> ds.cube(keys, aggregateOn, "b", com.landawn.abacus.util.stream.Collectors.toList()));
        assertThrows(IllegalArgumentException.class,
                () -> ds.cube(keys, Fn.<DisposableObjArray> identity(), "c", "b", com.landawn.abacus.util.stream.Collectors.countingToInt()));
        assertThrows(IllegalArgumentException.class, () -> ds.cube(keys, Fn.<DisposableObjArray> identity(), aggregateOn, "b", Object[].class));

        // a free result name is unaffected, and so are the no-aggregate overloads (which generate a free name)
        assertEquals(3, ds.rollup(keys, "c", "total", com.landawn.abacus.util.stream.Collectors.countingToInt()).toList().size());
        assertEquals(4, ds.cube(keys, "c", "total", com.landawn.abacus.util.stream.Collectors.countingToInt()).toList().size());
        assertEquals(3, ds.rollup(keys).toList().size());

        // The check is the same three lines pasted into twelve methods, so covering a sample of them would not
        // catch the likely failure - one paste missing. Every overload that takes an explicit result name is
        // called here, with a colliding name and then with a free one.
        final List<java.util.function.Supplier<Stream<Dataset>>> colliding = rollupAndCubeStreams(ds, "a");
        assertEquals(12, colliding.size());

        for (int i = 0; i < colliding.size(); i++) {
            final int index = i;
            assertThrows(IllegalArgumentException.class, () -> colliding.get(index).get(),
                    () -> "overload #" + index + " accepted a result name that collides with a key column");
        }

        for (final java.util.function.Supplier<Stream<Dataset>> free : rollupAndCubeStreams(ds, "total")) {
            assertNotNull(free.get().toList());
        }
    }

    /**
     * C-036: the {@code rollup}/{@code cube} Streams read the <i>live</i> Dataset as each level is pulled, and
     * were the only lazy sources in the class without a {@code modCount} guard - every other one
     * ({@code split}, {@code stream}, {@code iterator}, {@code columns}, {@code paginate}) is fail-fast. A
     * structural change between the call and the consumption therefore produced levels computed from different
     * data (the grand total not reconciling with the detail rows, silently), or reported a since-removed key
     * column as a bad argument, contradicting the documented "arguments are validated when this method is
     * called".
     */
    @Test
    public void test20260906c3_rollupAndCubeStreamsAreFailFast() {
        // a row added after the call invalidates the not-yet-produced levels
        assertThrows(ConcurrentModificationException.class, () -> {
            final Dataset ds = salesForRollup();
            final Stream<Dataset> levels = ds.rollup(CommonUtil.asList("region", "product"));
            ds.addRow(new Object[] { "East", "widget", 400 });
            levels.toList();
        });
        assertThrows(ConcurrentModificationException.class, () -> {
            final Dataset ds = salesForRollup();
            final Stream<Dataset> levels = ds.cube(CommonUtil.asList("region", "product"));
            ds.addRow(new Object[] { "East", "widget", 400 });
            levels.toList();
        });

        // ... and so does removing a column, which used to surface as an IllegalArgumentException naming a
        // column that was present when the arguments were checked
        assertThrows(ConcurrentModificationException.class, () -> {
            final Dataset ds = salesForRollup();
            final Stream<Dataset> levels = ds.rollup(CommonUtil.asList("region", "product"));
            ds.removeColumn("product");
            levels.toList();
        });

        // the aggregate-bearing overloads too
        assertThrows(ConcurrentModificationException.class, () -> {
            final Dataset ds = salesForRollup();
            final Stream<Dataset> levels = ds.rollup(CommonUtil.asList("region", "product"), "amount", "total",
                    com.landawn.abacus.util.stream.Collectors.countingToInt());
            ds.addRow(new Object[] { "East", "widget", 400 });
            levels.toList();
        });
        assertThrows(ConcurrentModificationException.class, () -> {
            final Dataset ds = salesForRollup();
            final Stream<Dataset> levels = ds.cube(CommonUtil.asList("region", "product"), CommonUtil.asList("amount"), "agg", Object[].class);
            ds.addRow(new Object[] { "East", "widget", 400 });
            levels.toList();
        });

        // the change is detected level by level, not only up front
        assertThrows(ConcurrentModificationException.class, () -> {
            final Dataset ds = salesForRollup();
            final java.util.Iterator<Dataset> it = ds.rollup(CommonUtil.asList("region", "product")).iterator();
            assertEquals(3, it.next().size()); // level 1 over the original 3 rows
            ds.addRow(new Object[] { "East", "widget", 400 });
            it.next(); // level 2 would otherwise have been computed over 4 rows
        });

        // Same reasoning as the collision test: twelve identical guards, so call every one of them rather
        // than a sample. Each stream is built first, then the Dataset is mutated, then it is consumed.
        final Dataset shared = Dataset.rows(CommonUtil.asList("a", "b", "c"), new Object[][] { { 1, 2, 3 }, { 4, 5, 6 } });
        final List<java.util.function.Supplier<Stream<Dataset>>> guarded = rollupAndCubeStreams(shared, "total");
        assertEquals(12, guarded.size());

        for (int i = 0; i < guarded.size(); i++) {
            final int index = i;
            final Stream<Dataset> levels = guarded.get(i).get();
            shared.addRow(new Object[] { 7, 8, 9 });
            assertThrows(ConcurrentModificationException.class, levels::toList, () -> "overload #" + index + " consumed a Dataset that changed after the call");
        }

        // an unmutated Dataset is unaffected
        final Dataset untouched = salesForRollup();
        assertEquals(3, untouched.rollup(CommonUtil.asList("region", "product")).toList().size());
        assertEquals(4, untouched.cube(CommonUtil.asList("region", "product")).toList().size());
        assertEquals(3,
                untouched.rollup(CommonUtil.asList("region", "product"), "amount", "total", com.landawn.abacus.util.stream.Collectors.countingToInt())
                        .toList()
                        .size());
    }

    @Test
    public void testSortByComparatorReceivesArraysItMayRetain() {
        final Dataset ds = Dataset.rows(CommonUtil.asList("a", "b"), new Object[][] { { 2, "x" }, { 1, "y" }, { 3, "z" } });
        final List<Object[]> captured = new ArrayList<>();

        ds.sortBy(CommonUtil.asList("a", "b"), (x, y) -> {
            captured.add(x);
            captured.add(y);
            return ((Integer) x[0]).compareTo((Integer) y[0]);
        });

        assertEquals(Arrays.asList(1, 2, 3), ds.<Integer> getColumn("a"));

        // The arrays handed to the comparator used to come from Objectory and were zero-filled on the way out.
        for (final Object[] keyRow : captured) {
            assertNotNull(keyRow[0]);
            assertNotNull(keyRow[1]);
        }
    }

    @Test
    public void testSortByAppliesMultiCyclePermutationsCorrectly() {
        // 6 rows whose sort order decomposes into more than one cycle.
        final Dataset ds = Dataset.rows(CommonUtil.asList("k", "v"), new Object[][] { { 5, "f" }, { 3, "d" }, { 1, "b" }, { 0, "a" }, { 4, "e" }, { 2, "c" } });

        ds.sortBy("k", Comparator.<Integer> naturalOrder());

        assertEquals(Arrays.asList(0, 1, 2, 3, 4, 5), ds.<Integer> getColumn("k"));
        // The paired column must have moved with its row.
        assertEquals(Arrays.asList("a", "b", "c", "d", "e", "f"), ds.<String> getColumn("v"));
    }

    @Test
    public void testSortByIsStableForEqualKeys() {
        final Dataset ds = Dataset.rows(CommonUtil.asList("k", "v"), new Object[][] { { 1, "first" }, { 0, "x" }, { 1, "second" }, { 1, "third" } });

        ds.sortBy("k", Comparator.<Integer> naturalOrder());

        assertEquals(Arrays.asList(0, 1, 1, 1), ds.<Integer> getColumn("k"));
        assertEquals(Arrays.asList("x", "first", "second", "third"), ds.<String> getColumn("v"));
    }

    @Test
    public void testTopByComparatorReceivesArraysItMayRetain() {
        final Dataset ds = Dataset.rows(CommonUtil.asList("a", "b"), new Object[][] { { 2, "x" }, { 1, "y" }, { 3, "z" }, { 4, "w" } });
        final List<Object[]> captured = new ArrayList<>();

        final Dataset top = ds.topBy(CommonUtil.asList("a", "b"), 2, (x, y) -> {
            captured.add(x);
            captured.add(y);
            return ((Integer) x[0]).compareTo((Integer) y[0]);
        });

        assertEquals(2, top.size());
        assertFalse(captured.isEmpty());

        for (final Object[] keyRow : captured) {
            assertNotNull(keyRow[0]);
            assertNotNull(keyRow[1]);
        }
    }

    /**
     * C-011: {@code topBy}/{@code distinctBy} returned {@code this.copy()} when the operation could not shrink the
     * input, and a copy carries the missing-property policy over. The policy of a transformation result therefore
     * depended on whether {@code n} happened to reach the row count.
     */
    @Test
    public void test20260906_topByAndDistinctByDoNotCarryTheMissingPropertyPolicy() {
        final Dataset ds = Dataset.rows(CommonUtil.asList("id", "extra"), new Object[][] { { 1, "x" }, { 2, "y" } });
        final Dataset strict = ds.withMissingPropertyPolicy(Dataset.MissingPropertyPolicy.ERROR);

        // documented to retain it
        assertThrows(IllegalArgumentException.class, () -> strict.copy().toList(OnlyIdBean20260906.class));
        assertThrows(IllegalArgumentException.class, () -> strict.slice(0, 2).toList(OnlyIdBean20260906.class));
        assertThrows(IllegalArgumentException.class, () -> strict.toList(OnlyIdBean20260906.class));

        // transformation results are not, and the two branches of the same method must agree
        assertEquals(2, strict.topBy("id", 5).toList(OnlyIdBean20260906.class).size());
        assertEquals(1, strict.topBy("id", 1).toList(OnlyIdBean20260906.class).size());

        // the other three early returns of the same shape - one per topBy overload
        assertEquals(2, strict.topBy(CommonUtil.asList("id"), 5).toList(OnlyIdBean20260906.class).size());
        assertEquals(2, strict.topBy(CommonUtil.asList("id"), 5, Comparators.OBJECT_ARRAY_COMPARATOR).toList(OnlyIdBean20260906.class).size());
        assertEquals(2,
                strict.topBy(CommonUtil.asList("id"), 5, (final DisposableObjArray row) -> (Comparable<?>) row.get(0)).toList(OnlyIdBean20260906.class).size());

        // distinctBy's early return is only reachable on a 0-row source, and a 0-row result converts no rows,
        // so the policy only becomes observable once the result has a row. A single-column selection with
        // Fn.identity() delegates to distinctBy(String, Function), which builds a fresh Dataset and never
        // reaches the changed call site - so neither of those shapes discriminates.
        final Dataset strictEmpty = Dataset.rows(CommonUtil.asList("id", "extra"), new Object[0][])
                .withMissingPropertyPolicy(Dataset.MissingPropertyPolicy.ERROR);

        final Dataset viaMultiColumn = strictEmpty.distinctBy(CommonUtil.asList("id", "extra"), Fn.identity());
        viaMultiColumn.addRow(new Object[] { 1, "x" });
        assertEquals(1, viaMultiColumn.toList(OnlyIdBean20260906.class).size());

        final Dataset viaNonIdentity = strictEmpty.distinctBy(CommonUtil.asList("id"), (final DisposableObjArray row) -> row.get(0));
        viaNonIdentity.addRow(new Object[] { 2, "y" });
        assertEquals(1, viaNonIdentity.toList(OnlyIdBean20260906.class).size());

        // C-030: unionAll built its result with the private copy(..), which carries the policy - so it was the
        // only set operation whose result differed from its siblings'
        final Dataset other = Dataset.rows(CommonUtil.asList("id", "extra"), new Object[][] { { 3, "z" } });
        assertEquals(3, strict.unionAll(other).toList(OnlyIdBean20260906.class).size());
        assertEquals(3, strict.union(other).toList(OnlyIdBean20260906.class).size());
        assertEquals(0, strict.intersect(other).toList(OnlyIdBean20260906.class).size());
    }

    @Test
    public void testComplexFilterPredicates() {
        final RowDataset dataset = createFiveRowCityDataset();
        Predicate<DisposableObjArray> complexPredicate = arr -> {
            Integer age = (Integer) arr.get(2);
            String city = (String) arr.get(3);
            return age > 25 && age < 35 && ("NYC".equals(city) || "LA".equals(city));
        };

        Dataset filtered = dataset.filter(complexPredicate);
        assertNotNull(filtered);
        assertTrue(filtered.size() > 0);

        Predicate<DisposableObjArray> subsetPredicate = arr -> {
            String name = (String) arr.get(0);
            Integer age = (Integer) arr.get(1);
            return name.length() > 3 && age > 30;
        };

        Dataset subsetFiltered = dataset.filter(CommonUtil.toList("name", "age"), subsetPredicate);
        assertNotNull(subsetFiltered);
    }

    /**
     * C-025/C-026/C-029: contracts the cycle-2 javadoc pass added — the twentieth {@code filter} overload's
     * null-filter rejection, the stale-row-index {@code IndexOutOfBoundsException} that {@code Row}'s four core
     * accessors and {@code toArray()} raise, and the {@code null}-row rejection of the array {@code rows}
     * factory (whose {@code Collection} twin accepts one).
     */
    @Test
    public void test20260906c2_filterRowAndRowsFactoryContracts() {
        final Dataset ds = Dataset.rows(CommonUtil.asList("id", "name"), new Object[][] { { 1, "a" }, { 2, "b" } });

        // C-025 - the one filter overload whose javadoc had not said so
        assertThrows(IllegalArgumentException.class, () -> ds.filter(0, 1, CommonUtil.asList("id"), null, 1));

        // C-026 - a structural change invalidates every accessor, even when an index remains in range.
        final Dataset shrinking = Dataset.rows(CommonUtil.asList("id", "name"), new Object[][] { { 1, "a" }, { 2, "b" } });
        final Dataset.Row stale = shrinking.row(1);
        assertEquals(Integer.valueOf(2), stale.<Integer> get(0)); // fine while the row exists
        shrinking.removeRow(1);
        assertThrows(ConcurrentModificationException.class, () -> stale.get(0));
        assertThrows(ConcurrentModificationException.class, () -> stale.get("id"));
        assertThrows(ConcurrentModificationException.class, () -> stale.set(0, 9));
        assertThrows(ConcurrentModificationException.class, () -> stale.set("id", 9));
        assertThrows(ConcurrentModificationException.class, stale::toArray);
        assertEquals("Row[1]=<invalidated>", stale.toString());

        // C-029 - the array factory rejects a null row; the Collection factory turns one into an all-null row
        assertThrows(IllegalArgumentException.class, () -> Dataset.rows(CommonUtil.asList("id"), new Object[][] { { 1 }, null }));
        final Dataset fromCollections = Dataset.rows(CommonUtil.asList("id"), CommonUtil.asList(CommonUtil.asList(1), null));
        assertEquals(2, fromCollections.size());
        assertNull(fromCollections.get(1, 0));
    }

    @Test
    public void testFullJoinWithMap() {
        Map<String, String> onColumnNames = new HashMap<>();
        onColumnNames.put("id", "id");

        Dataset result = ds1.fullJoin(ds2, onColumnNames);

        assertNotNull(result);
        assertEquals(4, result.size());
    }

    @Test
    public void testFullJoinWithNewColumn() {
        Map<String, String> onColumnNames = new HashMap<>();
        onColumnNames.put("id", "id");

        Dataset result = ds1.fullJoin(ds2, onColumnNames, "mergedData", Map.class);

        assertNotNull(result);
        assertEquals(4, result.size());
        assertTrue(result.containsColumn("mergedData"));
    }

    @Test
    public void testFullJoinWithCollectionSupplier() {
        Map<String, String> onColumnNames = new HashMap<>();
        onColumnNames.put("id", "id");

        Dataset result = ds1.fullJoin(ds2, onColumnNames, "dataList", List.class, ArrayList::new);

        assertNotNull(result);
        assertEquals(4, result.size());
        assertTrue(result.containsColumn("dataList"));
    }

    @Test
    public void testFullJoinUnmatchedRightRowsKeepRightOrder() {
        // regression: the single-key fullJoin used a HashMap, emitting unmatched right rows in
        // hash-bucket order instead of right-dataset order (the multi-key path uses LinkedHashMap)
        final RowDataset left = new RowDataset(new ArrayList<>(Arrays.asList("id")), new ArrayList<>(Arrays.asList(new ArrayList<>(Arrays.asList(1)))));
        final RowDataset right = new RowDataset(new ArrayList<>(Arrays.asList("rid", "score")),
                new ArrayList<>(Arrays.asList(new ArrayList<>(Arrays.asList(18, 16)), new ArrayList<>(Arrays.asList("a", "b")))));

        final Dataset joined = left.fullJoin(right, CommonUtil.asMap("id", "rid"));

        assertEquals(Arrays.asList(null, 18, 16), joined.getColumn("rid"));
    }

    @Test
    public void testFullJoinUnmatchedRightRowsWithDuplicateKeysKeepRightOrder() {
        // The unmatched right rows used to be appended grouped by join key, so a key occurring twice in the
        // right Dataset dragged its second row forward past the rows of other keys. rightJoin emits them in
        // right-dataset order and fullJoin must agree. The key 1 below straddles the key 2, which is what the
        // distinct-key regression above cannot detect.
        final Dataset left = Dataset.rows(CommonUtil.asList("id", "lv"), new Object[][] { { 9, "L9" } });
        final Dataset right = Dataset.rows(CommonUtil.asList("rid", "rv"), new Object[][] { { 1, "R1" }, { 2, "R2" }, { 1, "R1b" }, { 3, "R3" } });
        final List<String> expected = Arrays.asList(null, "R1", "R2", "R1b", "R3");

        assertEquals(expected, left.fullJoin(right, CommonUtil.asMap("id", "rid")).getColumn("rv"));
        assertEquals(expected, left.fullJoin(right, "id", "rid").getColumn("rv"));
        assertEquals(Arrays.asList("R1", "R2", "R1b", "R3"), left.rightJoin(right, CommonUtil.asMap("id", "rid")).getColumn("rv"));

        final Dataset withNewColumn = left.fullJoin(right, CommonUtil.asMap("id", "rid"), "r", Map.class);
        assertEquals(5, withNewColumn.size());
        assertEquals(expected, rvOfMapColumn(withNewColumn));

        // two join columns take the Wrapper<Object[]> path
        final Dataset left2 = Dataset.rows(CommonUtil.asList("id", "k", "lv"), new Object[][] { { 9, "z", "L9" } });
        final Dataset right2 = Dataset.rows(CommonUtil.asList("rid", "rk", "rv"),
                new Object[][] { { 1, "a", "R1" }, { 2, "a", "R2" }, { 1, "a", "R1b" }, { 3, "a", "R3" } });
        final Map<String, String> on2 = new LinkedHashMap<>();
        on2.put("id", "rid");
        on2.put("k", "rk");

        assertEquals(expected, left2.fullJoin(right2, on2).getColumn("rv"));

        final Dataset withNewColumn2 = left2.fullJoin(right2, on2, "r", Map.class);
        assertEquals(5, withNewColumn2.size());
        assertEquals(expected, rvOfMapColumn(withNewColumn2));

        // the collSupplier overloads deliberately emit ONE row per unmatched KEY, holding a collection of
        // that key's right rows, exactly as their rightJoin twins do - they must keep that row count.
        final Dataset collJoined = left.fullJoin(right, CommonUtil.asMap("id", "rid"), "r", Map.class, IntFunctions.ofList());
        assertEquals(4, collJoined.size());
        assertEquals(2, ((Collection<?>) collJoined.getColumn("r").get(1)).size());
        assertEquals(4, left2.fullJoin(right2, on2, "r", Map.class, IntFunctions.ofList()).size());
    }

    private static List<Object> rvOfMapColumn(final Dataset ds) {
        final List<Object> result = new ArrayList<>();

        for (final Map<String, Object> row : ds.<Map<String, Object>> getColumn("r")) {
            result.add(row == null ? null : row.get("rv"));
        }

        return result;
    }

    @Test
    public void testUnion() {
        assertThrows(IllegalArgumentException.class, () -> ds1.union(ds2));
        Dataset result = ds1.unionBy(ds2, List.of("id"));

        assertNotNull(result);
        assertTrue(result.size() <= ds1.size() + ds2.size());
        assertTrue(result.containsColumn("id"));
        assertTrue(result.containsColumn("name"));
        assertTrue(result.containsColumn("age"));
        assertTrue(result.containsColumn("city"));
        assertTrue(result.containsColumn("salary"));
    }

    @Test
    public void testUnionWithSameColumnsRequired() {
        List<String> columnNames = CommonUtil.toList("id", "name", "age");
        List<List<Object>> columns = new ArrayList<>();
        columns.add(CommonUtil.toList(4, 5));
        columns.add(CommonUtil.toList("David", "Eve"));
        columns.add(CommonUtil.toList(40, 45));
        Dataset ds3 = new RowDataset(columnNames, columns);

        Dataset result = ds1.union(ds3, true);

        assertNotNull(result);
        assertEquals(5, result.size());
    }

    @Test
    public void testUnionWithKeyColumns() {
        Collection<String> keyColumns = CommonUtil.toList("id");
        Dataset result = ds1.unionBy(ds2, keyColumns);

        assertNotNull(result);
        assertTrue(result.containsColumn("id"));
    }

    @Test
    public void testUnionWithKeyColumnsAndRequireSameColumns() {
        RowDataset ds1Local = new RowDataset(new ArrayList<>(Arrays.asList("id", "name")),
                new ArrayList<>(Arrays.asList(new ArrayList<>(Arrays.asList(1, 2)), new ArrayList<>(Arrays.asList("A", "B")))));
        RowDataset ds2Local = new RowDataset(new ArrayList<>(Arrays.asList("id", "name")),
                new ArrayList<>(Arrays.asList(new ArrayList<>(Arrays.asList(2, 3)), new ArrayList<>(Arrays.asList("B", "C")))));

        Dataset result = ds1Local.unionBy(ds2Local, Arrays.asList("id"), true);
        assertNotNull(result);
        assertTrue(result.size() >= 2);
    }

    @Test
    public void testUnion_WithKeyColumnsAndRequireSameColumns_MultiColumnKey() {
        // unionBy(Dataset, Collection, boolean) with multi-column key
        RowDataset left = new RowDataset(new ArrayList<>(Arrays.asList("id", "dept", "name")),
                new ArrayList<>(Arrays.asList(new ArrayList<>(Arrays.asList(1, 2, 1)), new ArrayList<>(Arrays.asList("eng", "eng", "eng")),
                        new ArrayList<>(Arrays.asList("Alice", "Bob", "Alice2")))));
        RowDataset right = new RowDataset(new ArrayList<>(Arrays.asList("id", "dept", "name")), new ArrayList<>(Arrays
                .asList(new ArrayList<>(Arrays.asList(1, 3)), new ArrayList<>(Arrays.asList("eng", "hr")), new ArrayList<>(Arrays.asList("Alice3", "Carol")))));

        Dataset result = left.unionBy(right, Arrays.asList("id", "dept"), true);
        assertNotNull(result);
        // rows with (1, eng) from left + (3, hr) from right = 3 unique
        assertEquals(3, result.size());
    }

    @Test
    public void testUnion_WithKeyColumnsAndRequireSameColumns_EmptyOther() {
        // union where other dataset is empty
        RowDataset other = new RowDataset(new ArrayList<>(Arrays.asList("id", "name")), new ArrayList<>(Arrays.asList(new ArrayList<>(), new ArrayList<>())));
        RowDataset source = new RowDataset(new ArrayList<>(Arrays.asList("id", "name")),
                new ArrayList<>(Arrays.asList(new ArrayList<>(Arrays.asList(1, 2)), new ArrayList<>(Arrays.asList("A", "B")))));

        Dataset result = source.unionBy(other, Arrays.asList("id"), true);
        assertEquals(2, result.size());
    }

    @Test
    public void testUnion_WithKeyColumnsAndRequireSameColumns_EmptySource() {
        // union where source dataset is empty
        RowDataset other = new RowDataset(new ArrayList<>(Arrays.asList("id", "name")),
                new ArrayList<>(Arrays.asList(new ArrayList<>(Arrays.asList(1, 2)), new ArrayList<>(Arrays.asList("A", "B")))));
        RowDataset source = new RowDataset(new ArrayList<>(Arrays.asList("id", "name")), new ArrayList<>(Arrays.asList(new ArrayList<>(), new ArrayList<>())));

        Dataset result = source.unionBy(other, Arrays.asList("id"), true);
        assertEquals(2, result.size());
    }

    @Test
    public void testUnion_DifferentColumns_WithKeyColumn() {
        // union(Dataset, Collection, boolean) with different columns (requiresSameColumns = false)
        // ds1 has id, name, age; ds2 has id, city, salary
        Dataset result = ds1.unionBy(ds2, Arrays.asList("id"), false);
        assertNotNull(result);
        assertTrue(result.containsColumn("id"));
        assertTrue(result.containsColumn("name"));
        assertTrue(result.containsColumn("city"));
    }

    @Test
    public void testUnion_NullOther_ThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> ds1.union(null));
        assertThrows(IllegalArgumentException.class, () -> ds1.union(null, true));
        assertThrows(IllegalArgumentException.class, () -> ds1.unionBy(null, Arrays.asList("id")));
        assertThrows(IllegalArgumentException.class, () -> ds1.unionBy(null, Arrays.asList("id"), false));
    }

    @Test
    public void testUnionAll() {
        assertThrows(IllegalArgumentException.class, () -> ds1.unionAll(ds2));
        Dataset result = ds1.unionAll(ds2, false);

        assertNotNull(result);
        assertTrue(result.containsColumn("id"));
    }

    @Test
    public void testUnionAll_NullOther_ThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> ds1.unionAll(null));
        assertThrows(IllegalArgumentException.class, () -> ds1.unionAll(null, false));
    }

    @Test
    public void testIntersect() {
        assertThrows(IllegalArgumentException.class, () -> ds1.intersect(ds2));
        Dataset result = ds1.intersectBy(ds2, List.of("id"));

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    public void testIntersectWithKeyColumns() {
        Collection<String> keyColumns = CommonUtil.toList("id");
        Dataset result = ds1.intersectBy(ds2, keyColumns);

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    public void testIntersectWithKeyColumnsAndRequireSameColumns() {
        RowDataset ds1Local = new RowDataset(new ArrayList<>(Arrays.asList("id", "name")),
                new ArrayList<>(Arrays.asList(new ArrayList<>(Arrays.asList(1, 2, 3)), new ArrayList<>(Arrays.asList("A", "B", "C")))));
        RowDataset ds2Local = new RowDataset(new ArrayList<>(Arrays.asList("id", "name")),
                new ArrayList<>(Arrays.asList(new ArrayList<>(Arrays.asList(2, 3, 4)), new ArrayList<>(Arrays.asList("B", "C", "D")))));

        Dataset result = ds1Local.intersectBy(ds2Local, Arrays.asList("id"), true);
        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    public void testIntersect_NullOther_ThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> ds1.intersect(null));
        assertThrows(IllegalArgumentException.class, () -> ds1.intersect(null, true));
        assertThrows(IllegalArgumentException.class, () -> ds1.intersectBy(null, Arrays.asList("id")));
        assertThrows(IllegalArgumentException.class, () -> ds1.intersectBy(null, Arrays.asList("id"), false));
    }

    @Test
    public void testIntersectAll() {
        assertThrows(IllegalArgumentException.class, () -> ds1.intersectAll(ds2));
        Dataset result = ds1.intersectAllBy(ds2, List.of("id"));

        assertNotNull(result);
        assertTrue(result.size() <= Math.min(ds1.size(), ds2.size()));
    }

    @Test
    public void testIntersectAllWithKeyColumns() {
        RowDataset ds1Local = new RowDataset(new ArrayList<>(Arrays.asList("id", "name")),
                new ArrayList<>(Arrays.asList(new ArrayList<>(Arrays.asList(1, 2, 2, 3)), new ArrayList<>(Arrays.asList("A", "B", "B2", "C")))));
        RowDataset ds2Local = new RowDataset(new ArrayList<>(Arrays.asList("id", "name")),
                new ArrayList<>(Arrays.asList(new ArrayList<>(Arrays.asList(2, 3, 4)), new ArrayList<>(Arrays.asList("B", "C", "D")))));

        Dataset result = ds1Local.intersectAllBy(ds2Local, Arrays.asList("id"));
        assertNotNull(result);
        assertTrue(result.size() >= 2);
    }

    @Test
    public void testIntersectAllWithKeyColumnsAndRequireSameColumns() {
        RowDataset ds1Local = new RowDataset(new ArrayList<>(Arrays.asList("id", "name")),
                new ArrayList<>(Arrays.asList(new ArrayList<>(Arrays.asList(1, 2, 3)), new ArrayList<>(Arrays.asList("A", "B", "C")))));
        RowDataset ds2Local = new RowDataset(new ArrayList<>(Arrays.asList("id", "name")),
                new ArrayList<>(Arrays.asList(new ArrayList<>(Arrays.asList(2, 3)), new ArrayList<>(Arrays.asList("B", "C")))));

        Dataset result = ds1Local.intersectAllBy(ds2Local, Arrays.asList("id"), true);
        assertNotNull(result);
    }

    @Test
    public void testIntersectAll_NullOther_ThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> ds1.intersectAll(null));
        assertThrows(IllegalArgumentException.class, () -> ds1.intersectAll(null, true));
        assertThrows(IllegalArgumentException.class, () -> ds1.intersectAllBy(null, Arrays.asList("id")));
        assertThrows(IllegalArgumentException.class, () -> ds1.intersectAllBy(null, Arrays.asList("id"), false));
    }

    @Test
    public void testExcept() {
        assertThrows(IllegalArgumentException.class, () -> ds1.except(ds2));
        Dataset result = ds1.exceptBy(ds2, List.of("id"));

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1, (Integer) result.moveToRow(0).get("id"));
    }

    @Test
    public void testExceptWithKeyColumns() {
        Collection<String> keyColumns = CommonUtil.toList("id");
        Dataset result = ds1.exceptBy(ds2, keyColumns);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    public void testExceptWithKeyColumnsAndRequireSameColumns() {
        RowDataset ds1Local = new RowDataset(new ArrayList<>(Arrays.asList("id", "name")),
                new ArrayList<>(Arrays.asList(new ArrayList<>(Arrays.asList(1, 2, 3)), new ArrayList<>(Arrays.asList("A", "B", "C")))));
        RowDataset ds2Local = new RowDataset(new ArrayList<>(Arrays.asList("id", "name")),
                new ArrayList<>(Arrays.asList(new ArrayList<>(Arrays.asList(2, 3)), new ArrayList<>(Arrays.asList("B", "C")))));

        Dataset result = ds1Local.exceptBy(ds2Local, Arrays.asList("id"), true);
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    public void testExcept_NullOther_ThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> ds1.except(null));
        assertThrows(IllegalArgumentException.class, () -> ds1.except(null, true));
        assertThrows(IllegalArgumentException.class, () -> ds1.exceptBy(null, Arrays.asList("id")));
        assertThrows(IllegalArgumentException.class, () -> ds1.exceptBy(null, Arrays.asList("id"), false));
    }

    @Test
    public void testConstructor_NullFirstColumnThrowsIllegalArgumentException() {
        // The first column in columnList is null. Per the constructor contract this must be
        // reported as IllegalArgumentException (not NullPointerException from columnList.get(0).size()).
        final List<String> cols = new ArrayList<>(Arrays.asList("a", "b"));
        final List<List<Object>> data = new ArrayList<>();
        data.add(null);
        data.add(new ArrayList<>(Arrays.asList(1, 2, 3)));

        assertThrows(IllegalArgumentException.class, () -> new RowDataset(cols, data));
    }

    @Test
    public void testJoinNullRightThrowsIllegalArgumentException() {
        // regression: a null right dataset failed with a raw NPE; the documented contract is IAE
        final RowDataset left = new RowDataset(new ArrayList<>(Arrays.asList("id")), new ArrayList<>(Arrays.asList(new ArrayList<>(Arrays.asList(1)))));

        assertThrows(IllegalArgumentException.class, () -> left.innerJoin(null, CommonUtil.asMap("id", "id")));
        assertThrows(IllegalArgumentException.class, () -> left.leftJoin(null, CommonUtil.asMap("id", "id")));
        assertThrows(IllegalArgumentException.class, () -> left.rightJoin(null, CommonUtil.asMap("id", "id")));
        assertThrows(IllegalArgumentException.class, () -> left.fullJoin(null, CommonUtil.asMap("id", "id")));
    }

    @Test
    public void testExceptAll() {
        assertThrows(IllegalArgumentException.class, () -> ds1.exceptAll(ds2));
        Dataset result = ds1.exceptAllBy(ds2, List.of("id"));

        assertNotNull(result);
        assertTrue(result.size() <= ds1.size());
    }

    @Test
    public void testExceptAllWithKeyColumns() {
        RowDataset ds1Local = new RowDataset(new ArrayList<>(Arrays.asList("id", "name")),
                new ArrayList<>(Arrays.asList(new ArrayList<>(Arrays.asList(1, 2, 2, 3)), new ArrayList<>(Arrays.asList("A", "B", "B2", "C")))));
        RowDataset ds2Local = new RowDataset(new ArrayList<>(Arrays.asList("id", "name")),
                new ArrayList<>(Arrays.asList(new ArrayList<>(Arrays.asList(2)), new ArrayList<>(Arrays.asList("B")))));

        Dataset result = ds1Local.exceptAllBy(ds2Local, Arrays.asList("id"));
        assertNotNull(result);
    }

    @Test
    public void testExceptAllWithKeyColumnsAndRequireSameColumns() {
        RowDataset ds1Local = new RowDataset(new ArrayList<>(Arrays.asList("id", "name")),
                new ArrayList<>(Arrays.asList(new ArrayList<>(Arrays.asList(1, 2, 3)), new ArrayList<>(Arrays.asList("A", "B", "C")))));
        RowDataset ds2Local = new RowDataset(new ArrayList<>(Arrays.asList("id", "name")),
                new ArrayList<>(Arrays.asList(new ArrayList<>(Arrays.asList(2)), new ArrayList<>(Arrays.asList("B")))));

        Dataset result = ds1Local.exceptAllBy(ds2Local, Arrays.asList("id"), true);
        assertNotNull(result);
    }

    @Test
    public void testExceptAll_NullOther_ThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> ds1.exceptAll(null));
        assertThrows(IllegalArgumentException.class, () -> ds1.exceptAll(null, true));
        assertThrows(IllegalArgumentException.class, () -> ds1.exceptAllBy(null, Arrays.asList("id")));
        assertThrows(IllegalArgumentException.class, () -> ds1.exceptAllBy(null, Arrays.asList("id"), false));
    }

    @Test
    public void testSemiAndAntiJoinsDifferFromBagOperations() {
        final Dataset left = Dataset.rows(CommonUtil.asList("id"), new Object[][] { { 1 }, { 1 }, { 2 } });
        final Dataset right = Dataset.rows(CommonUtil.asList("id"), new Object[][] { { 1 } });

        assertEquals(1, left.intersectAll(right).size());
        assertEquals(2, left.exceptAll(right).size());

        assertEquals(1, left.intersectAllBy(right, CommonUtil.asList("id")).size());
        assertEquals(2, left.exceptAllBy(right, CommonUtil.asList("id")).size());

        // Joins preserve every matching/nonmatching left occurrence.
        assertEquals(2, left.semiJoin(right).size());
        assertEquals(1, left.antiJoin(right).size());
    }

    @Test
    public void testCartesianProduct() {
        List<String> columnNames1 = CommonUtil.toList("a", "b");
        List<List<Object>> columns1 = new ArrayList<>();
        columns1.add(CommonUtil.toList(1, 2));
        columns1.add(CommonUtil.toList("X", "Y"));
        Dataset ds1New = new RowDataset(columnNames1, columns1);

        List<String> columnNames2 = CommonUtil.toList("c", "d");
        List<List<Object>> columns2 = new ArrayList<>();
        columns2.add(CommonUtil.toList(10, 20));
        columns2.add(CommonUtil.toList("P", "Q"));
        Dataset ds2New = new RowDataset(columnNames2, columns2);

        Dataset result = ds1New.cartesianProduct(ds2New);

        assertNotNull(result);
        assertEquals(4, result.size());
        assertTrue(result.containsColumn("a"));
        assertTrue(result.containsColumn("b"));
        assertTrue(result.containsColumn("c"));
        assertTrue(result.containsColumn("d"));
    }

    // ========== cartesianProduct - empty dataset ==========

    @Test
    public void testCartesianProduct_WithEmptyOther_ReturnsEmpty() {
        RowDataset ds = new RowDataset(new ArrayList<>(Arrays.asList("id")), new ArrayList<>(Arrays.asList(new ArrayList<>(Arrays.asList(1, 2)))));
        RowDataset emptyOther = new RowDataset(new ArrayList<>(Arrays.asList("val")), new ArrayList<>(Arrays.asList(new ArrayList<>())));
        Dataset result = ds.cartesianProduct(emptyOther);
        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    public void testCartesianProductWithCommonColumns() {
        assertThrows(IllegalArgumentException.class, () -> {
            ds1.cartesianProduct(ds2);
        });
    }

    @Test
    public void testCartesianProduct_NullOther_ThrowsIllegalArgumentException() {
        // regression: a null other dataset failed with a raw NPE; the documented contract is IAE
        assertThrows(IllegalArgumentException.class, () -> ds1.cartesianProduct(null));
    }

    @Test
    public void testJoinsAndCartesianProductStillReadTheCorrectCells() {
        // get(int, int) validates both axes now, so the join/cartesian loops read through an unchecked
        // private accessor instead. This pins the values they produce.
        final Dataset left = Dataset.rows(CommonUtil.asList("id", "v"), new Object[][] { { 1, "L1" }, { 2, "L2" }, { 3, "L3" } });
        final Dataset right = Dataset.rows(CommonUtil.asList("rid", "w"), new Object[][] { { 2, "R2" }, { 3, "R3" }, { 3, "R3b" } });
        final Map<String, String> on = new LinkedHashMap<>();
        on.put("id", "rid");

        assertEquals(Arrays.asList("L2", "L3", "L3"), left.innerJoin(right, on).<String> getColumn("v"));
        assertEquals(Arrays.asList("R2", "R3", "R3b"), left.innerJoin(right, on).<String> getColumn("w"));

        assertEquals(4, left.leftJoin(right, on).size());
        assertTrue(left.leftJoin(right, on).<String> getColumn("v").contains("L1"));
        assertEquals(3, left.rightJoin(right, on).size());
        assertEquals(4, left.fullJoin(right, on).size());

        final Dataset a = Dataset.rows(CommonUtil.asList("a"), new Object[][] { { 1 }, { 2 } });
        final Dataset b = Dataset.rows(CommonUtil.asList("b"), new Object[][] { { "x" }, { "y" }, { "z" } });
        final Dataset product = a.cartesianProduct(b);

        assertEquals(6, product.size());
        assertEquals(Arrays.asList(1, 1, 1, 2, 2, 2), product.<Integer> getColumn("a"));
        assertEquals(Arrays.asList("x", "y", "z", "x", "y", "z"), product.<String> getColumn("b"));
    }

    @Test
    public void testSplit() {
        Stream<Dataset> splitStream = ds1.split(2);
        List<Dataset> splits = splitStream.toList();

        assertEquals(2, splits.size());
        assertEquals(2, splits.get(0).size());
        assertEquals(1, splits.get(1).size());
    }

    @Test
    public void testSplitWithColumns() {
        Collection<String> columnNames = CommonUtil.toList("id", "name");
        Stream<Dataset> splitStream = ds1.split(2, columnNames);
        List<Dataset> splits = splitStream.toList();

        assertEquals(2, splits.size());
        assertTrue(splits.get(0).containsColumn("id"));
        assertTrue(splits.get(0).containsColumn("name"));
        assertFalse(splits.get(0).containsColumn("age"));
    }

    @Test
    public void testSplitToList() {
        List<Dataset> splits = ds1.splitToList(2);

        assertEquals(2, splits.size());
        assertEquals(2, splits.get(0).size());
        assertEquals(1, splits.get(1).size());
    }

    @Test
    public void testSplitToListWithColumns() {
        Collection<String> columnNames = CommonUtil.toList("id", "age");
        List<Dataset> splits = ds1.splitToList(2, columnNames);

        assertEquals(2, splits.size());
        assertTrue(splits.get(0).containsColumn("id"));
        assertTrue(splits.get(0).containsColumn("age"));
        assertFalse(splits.get(0).containsColumn("name"));
    }

    @Test
    public void testSliceWithColumns() {
        Dataset sliced = dataset.slice(Arrays.asList("id", "name"));
        assertEquals(2, sliced.columnCount());
        assertEquals(dataset.size(), sliced.size());
    }

    @Test
    public void testSlice() {
        Collection<String> columnNames = CommonUtil.toList("id", "name");
        Dataset result = ds1.slice(columnNames);

        assertNotNull(result);
        assertEquals(ds1.size(), result.size());
        assertEquals(2, result.columnCount());
        assertTrue(result.containsColumn("id"));
        assertTrue(result.containsColumn("name"));
        assertFalse(result.containsColumn("age"));
    }

    // ========== slice - empty column names ==========

    @Test
    public void testSlice_EmptyColumnNamesRequireAnEmptyRowRange() {
        assertThrows(IllegalArgumentException.class, () -> dataset.slice(0, dataset.size(), new ArrayList<>()));
        Dataset sliced = dataset.slice(0, 0, new ArrayList<>());
        assertNotNull(sliced);
        assertEquals(0, sliced.columnCount());
        assertTrue(sliced.isFrozen());
    }

    @Test
    public void testSliceWithRowRange() {
        Dataset result = ds1.slice(1, 3);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(ds1.columnCount(), result.columnCount());
    }

    @Test
    public void testSliceWithRowRangeAndColumns() {
        Collection<String> columnNames = CommonUtil.toList("name", "age");
        Dataset result = ds1.slice(0, 2, columnNames);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(2, result.columnCount());
        assertTrue(result.containsColumn("name"));
        assertTrue(result.containsColumn("age"));
    }

    @Test
    public void testSliceIsInvalidatedByRowChangesInTheParent() {
        final Dataset parent = twoColumnDataset();
        final Dataset fullRange = parent.slice(CommonUtil.asList("id", "name"));
        final Dataset partial = parent.slice(0, 2);

        assertTrue(fullRange.isFrozen());
        assertEquals(3, fullRange.size());
        assertEquals(2, partial.size());

        parent.addRow(new Object[] { 4, "d" });

        // Both shapes now fail the same way. The full-range slice used to silently grow to 4 instead.
        assertThrows(ConcurrentModificationException.class, fullRange::size);
        assertThrows(ConcurrentModificationException.class, partial::size);
    }

    @Test
    public void testSliceSeesValueUpdatesAndSurvivesColumnChanges() {
        final Dataset parent = twoColumnDataset();
        final Dataset slice = parent.slice(0, 2, CommonUtil.asList("id"));

        assertEquals(Integer.valueOf(1), slice.get(0, 0));

        // A value write is not a structural change, so the view stays valid and shows the new value.
        parent.set(0, 0, 42);
        assertEquals(Integer.valueOf(42), slice.get(0, 0));

        // Column-level changes leave the captured columns intact.
        parent.removeColumn("name");
        assertEquals(Integer.valueOf(42), slice.get(0, 0));
        assertEquals(2, slice.size());
    }

    @Test
    public void testSliceIsFrozen() {
        final Dataset slice = twoColumnDataset().slice(0, 2);

        assertTrue(slice.isFrozen());
        assertThrows(IllegalStateException.class, () -> slice.set(0, 0, 9));
        assertThrows(IllegalStateException.class, () -> slice.addRow(new Object[] { 9, "z" }));
    }

    @Test
    public void testPaginateGetPage() {
        Paginated<Dataset> paginated = dataset.paginate(2);
        assertEquals(3, paginated.totalPages());
        assertEquals(2, paginated.pageSize());

        Dataset page0 = paginated.getPage(0);
        assertEquals(2, page0.size());

        Dataset page2 = paginated.getPage(2);
        assertEquals(1, page2.size());
    }

    @Test
    public void testPaginateFirstAndLastPage() {
        Paginated<Dataset> paginated = dataset.paginate(2);
        Optional<Dataset> first = paginated.firstPage();
        assertTrue(first.isPresent());
        assertEquals(2, first.get().size());

        Optional<Dataset> last = paginated.lastPage();
        assertTrue(last.isPresent());
        assertEquals(1, last.get().size());
    }

    @Test
    public void testPaginateStream() {
        Paginated<Dataset> paginated = dataset.paginate(2);
        List<Dataset> pages = paginated.stream().toList();
        assertEquals(3, pages.size());
    }

    @Test
    public void testPaginate() {
        Paginated<Dataset> paginated = ds1.paginate(2);

        assertNotNull(paginated);
        assertEquals(2, paginated.pageSize());
        assertEquals(2, paginated.totalPages());

        Optional<Dataset> firstPage = paginated.firstPage();
        assertTrue(firstPage.isPresent());
        assertEquals(2, firstPage.get().size());

        Optional<Dataset> lastPage = paginated.lastPage();
        assertTrue(lastPage.isPresent());
        assertEquals(1, lastPage.get().size());
    }

    @Test
    public void testPaginateWithColumns() {
        Collection<String> columnNames = CommonUtil.toList("id", "name");
        Paginated<Dataset> paginated = ds1.paginate(columnNames, 2);

        assertNotNull(paginated);
        assertEquals(2, paginated.totalPages());

        Dataset page = paginated.getPage(0);
        assertEquals(2, page.columnCount());
        assertTrue(page.containsColumn("id"));
        assertTrue(page.containsColumn("name"));
    }

    @Test
    public void testPaginateIterator() {
        Paginated<Dataset> paginated = dataset.paginate(3);
        int count = 0;
        for (Dataset page : paginated) {
            assertNotNull(page);
            count++;
        }
        assertEquals(2, count);
    }

    @Test
    public void testApply() {
        Integer result = ds1.apply(ds -> ds.size());
        assertEquals(3, result);
    }

    @Test
    public void testApplyIfNotEmpty() {
        Optional<Integer> result = ds1.applyIfNotEmpty(ds -> ds.size());
        assertTrue(result.isPresent());
        assertEquals(3, result.get().intValue());

        Optional<Integer> emptyResult = emptyDs.applyIfNotEmpty(ds -> ds.size());
        assertFalse(emptyResult.isPresent());
    }

    @Test
    public void testAccept() {
        List<String> names = new ArrayList<>();
        ds1.accept(ds -> {
            for (int i = 0; i < ds.size(); i++) {
                names.add((String) ds.moveToRow(i).get("name"));
            }
        });

        assertEquals(3, names.size());
        assertEquals(CommonUtil.toList("Alice", "Bob", "Charlie"), names);
    }

    @Test
    public void testAcceptIfNotEmpty() {
        List<String> names = new ArrayList<>();
        OrElse result = ds1.acceptIfNotEmpty(ds -> {
            names.add("processed");
        });

        assertEquals(OrElse.TRUE, result);
        assertEquals(1, names.size());

        OrElse emptyResult = emptyDs.acceptIfNotEmpty(ds -> {
            names.add("should not be added");
        });

        assertEquals(OrElse.FALSE, emptyResult);
        assertEquals(1, names.size());
    }

    @Test
    public void testFreeze() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        assertFalse(ds.isFrozen());
        ds.freeze();
        assertTrue(ds.isFrozen());
    }

    @Test
    public void testFreezeIdempotent() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        ds.freeze();
        ds.freeze();
        assertTrue(ds.isFrozen());
    }

    @Test
    public void testIsFrozen() {
        assertFalse(dataset.isFrozen());
        dataset.freeze();
        assertTrue(dataset.isFrozen());
    }

    @Test
    public void testIsEmpty() {
        assertFalse(dataset.isEmpty());
        assertTrue(emptyDataset.isEmpty());
    }

    @Test
    public void testTrimToSize() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        ds.removeRow(0);
        ds.trimToSize();
        assertEquals(4, ds.size());
    }

    @Test
    public void testTrimToSize_FullCoverage() {
        // trimToSize on a dataset with some capacity
        RowDataset ds = new RowDataset(new ArrayList<>(Arrays.asList("id", "name")),
                new ArrayList<>(Arrays.asList(new ArrayList<>(Arrays.asList(1, 2, 3)), new ArrayList<>(Arrays.asList("A", "B", "C")))));
        ds.trimToSize();
        assertEquals(3, ds.size());
    }

    @Test
    public void testClear() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        assertFalse(ds.isEmpty());
        ds.clear();
        assertTrue(ds.isEmpty());
        assertEquals(0, ds.size());
        assertEquals(4, ds.columnCount());
    }

    @Test
    public void testClearFrozen() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        ds.freeze();
        assertThrows(IllegalStateException.class, () -> {
            ds.clear();
        });
    }

    @Test
    public void testSetProperties() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        Map<String, Object> props = new HashMap<>();
        props.put("key1", "value1");
        props.put("key2", 123);
        ds.setProperties(props);
        assertEquals("value1", ds.getProperties().get("key1"));
        assertEquals(123, ds.getProperties().get("key2"));
    }

    @Test
    public void testSetPropertiesNull() {
        RowDataset ds = new RowDataset(columnNames, copyColumnList());
        ds.setProperties(null);
        assertNotNull(ds.getProperties());
        assertTrue(ds.getProperties().isEmpty());
    }

    @Test
    public void testPrintlnWithRangeAndColumns() {
        Collection<String> columnNames = CommonUtil.toList("id", "name");
        ds1.println(0, 2, columnNames);
        assertNotNull(columnNames);
    }

    @Test
    public void testPrintlnWithWriter() {
        StringWriter writer = new StringWriter();
        ds1.println(writer);

        String output = writer.toString();
        assertNotNull(output);
        assertTrue(output.contains("id"));
        assertTrue(output.contains("name"));
        assertTrue(output.contains("age"));
    }

    @Test
    public void testPrintlnWithRangeColumnsAndWriter() {
        StringWriter writer = new StringWriter();
        Collection<String> columnNames = CommonUtil.toList("id", "name");
        ds1.println(1, 3, columnNames, writer);

        String output = writer.toString();
        assertNotNull(output);
        assertTrue(output.contains("Bob"));
        assertTrue(output.contains("Charlie"));
    }

    @Test
    public void testPrintln() {
        assertDoesNotThrow(() -> {
            ds1.println();
        });
    }

    @Test
    public void testPrintlnWithRange() {
        assertDoesNotThrow(() -> {
            ds1.println(0, 2);
        });
    }

    @Test
    public void testPrintlnWithPrefix() {
        assertDoesNotThrow(() -> dataset.println("PREFIX: "));
    }

    @Test
    public void testPrintlnWithRangeAndColumnsAndPrefix() {
        StringWriter sw = new StringWriter();
        assertDoesNotThrow(() -> dataset.println(0, 2, Arrays.asList("id", "name"), "= ", sw));
        String output = sw.toString();
        assertNotNull(output);
        assertTrue(output.length() > 0);
    }

    @Test
    public void testPrintlnToAppendable() throws IOException {
        StringBuilder sb = new StringBuilder();
        dataset.println(sb);
        assertTrue(sb.length() > 0);
        assertTrue(sb.toString().contains("Alice"));
    }

    @Test
    public void testPrintlnWithRangeColumnsAndPrefixToAppendable() throws IOException {
        StringBuilder sb = new StringBuilder();
        dataset.println(0, 2, Arrays.asList("id", "name"), "PREFIX: ", sb);
        String result = sb.toString();
        assertTrue(result.contains("PREFIX:"));
        assertTrue(result.contains("Alice"));
        assertFalse(result.contains("Charlie"));
    }

    @Test
    public void testHashCode() {
        int hash1 = dataset.hashCode();
        int hash2 = dataset.hashCode();
        assertEquals(hash1, hash2);
    }

    @Test
    public void testEquals_DifferentContent() {
        RowDataset different = new RowDataset(new ArrayList<>(Arrays.asList("id")), new ArrayList<>(Arrays.asList(new ArrayList<>(Arrays.asList(99)))));
        assertFalse(dataset.equals(different));
    }

    @Test
    public void testEquals() {
        Dataset copy = dataset.copy();
        boolean result = dataset.equals(copy);
        assertNotNull(result);
    }

    @Test
    public void testEquals_SameContent() {
        RowDataset ds1Copy = new RowDataset(new ArrayList<>(Arrays.asList("id", "name", "age", "salary")),
                new ArrayList<>(Arrays.asList(new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5)),
                        new ArrayList<>(Arrays.asList("Alice", "Bob", "Charlie", "Diana", "Eve")), new ArrayList<>(Arrays.asList(25, 30, 35, 28, 22)),
                        new ArrayList<>(Arrays.asList(50000.0, 60000.0, 70000.0, 55000.0, 45000.0)))));

        assertTrue(dataset.equals(ds1Copy));
        assertEquals(dataset.hashCode(), ds1Copy.hashCode());
    }

    @Test
    public void testEquals_NonDataset() {
        assertFalse(dataset.equals("not a dataset"));
        assertFalse(dataset.equals(null));
        assertTrue(dataset.equals(dataset)); // same reference
    }

    @Test
    public void testEqualsAndHashCodeUseDeepEqualityForArrayCells() {
        final Dataset a = Dataset.rows(CommonUtil.asList("k"), new Object[][] { { new int[] { 1, 2 } } });
        final Dataset b = Dataset.rows(CommonUtil.asList("k"), new Object[][] { { new int[] { 1, 2 } } });
        final Dataset c = Dataset.rows(CommonUtil.asList("k"), new Object[][] { { new int[] { 1, 3 } } });

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);

        // equals must now agree with distinct(), which has always hashed array cells by content.
        final Dataset dups = Dataset.rows(CommonUtil.asList("k"), new Object[][] { { new int[] { 1 } }, { new int[] { 1 } } });
        assertEquals(1, dups.distinct().size());

        // Object arrays too.
        final Dataset o1 = Dataset.rows(CommonUtil.asList("k"), new Object[][] { { new String[] { "x" } } });
        final Dataset o2 = Dataset.rows(CommonUtil.asList("k"), new Object[][] { { new String[] { "x" } } });
        assertEquals(o1, o2);
        assertEquals(o1.hashCode(), o2.hashCode());
    }

    @Test
    public void testEqualsUnchangedForNonArrayCells() {
        final Dataset a = twoColumnDataset();
        final Dataset b = twoColumnDataset();

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());

        b.set(0, 0, 99);
        assertNotEquals(a, b);

        // Differing shapes stay unequal.
        assertNotEquals(a, Dataset.rows(CommonUtil.asList("id"), new Object[][] { { 1 }, { 2 }, { 3 } }));
        assertNotEquals(a, Dataset.rows(CommonUtil.asList("id", "name"), new Object[][] { { 1, "a" } }));
        assertNotEquals(a, "not a Dataset");
        assertEquals(a, a);
    }

    @Test
    public void testPaginationEdgeCases() {
        Paginated<Dataset> paginated1 = ds1.paginate(3);
        assertEquals(1, paginated1.totalPages());

        Paginated<Dataset> paginated2 = ds1.paginate(10);
        assertEquals(1, paginated2.totalPages());

        assertThrows(IllegalArgumentException.class, () -> {
            paginated1.getPage(-1);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            paginated1.getPage(5);
        });
    }

    @Test
    public void testProperties() {
        Map<String, Object> props = ds1.getProperties();
        assertNotNull(props);
    }

    @Test
    public void testMapAndMultimapSuppliersRejectNullResults() {
        final IntFunction<Map<Integer, String>> nullMapSupplier = ignored -> null;
        final IntFunction<ListMultimap<Integer, String>> nullMultimapSupplier = ignored -> null;

        assertThrows(IllegalArgumentException.class, () -> dataset.toMap("id", "name", nullMapSupplier));
        assertThrows(IllegalArgumentException.class, () -> dataset.toMultimap("id", "name", nullMultimapSupplier));
    }

    @Test
    @SuppressWarnings("rawtypes")
    public void testJoinCollectionSuppliersRejectNullResults() {
        final RowDataset left = new RowDataset(new ArrayList<>(Arrays.asList("id")), new ArrayList<>(Arrays.asList(new ArrayList<>(Arrays.asList(1)))));
        final RowDataset right = new RowDataset(new ArrayList<>(Arrays.asList("rid")), new ArrayList<>(Arrays.asList(new ArrayList<>(Arrays.asList(1)))));
        final Map<String, String> joinColumns = CommonUtil.asMap("id", "rid");
        final IntFunction<Collection> nullCollectionSupplier = ignored -> null;

        assertThrows(IllegalArgumentException.class, () -> left.innerJoin(right, joinColumns, "matches", Object[].class, nullCollectionSupplier));
        assertThrows(IllegalArgumentException.class, () -> left.rightJoin(right, joinColumns, "matches", Object[].class, nullCollectionSupplier));
        assertThrows(IllegalArgumentException.class, () -> left.fullJoin(right, joinColumns, "matches", Object[].class, nullCollectionSupplier));
    }

    @Test
    public void testTwoCoordinateAccessorsValidateBothAxes() {
        final Dataset ds = twoColumnDataset();

        assertThrows(IndexOutOfBoundsException.class, () -> ds.get(0, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> ds.get(0, -1));
        assertThrows(IndexOutOfBoundsException.class, () -> ds.get(3, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> ds.get(-1, 0));

        assertThrows(IndexOutOfBoundsException.class, () -> ds.set(0, 2, "x"));
        assertThrows(IndexOutOfBoundsException.class, () -> ds.set(3, 0, "x"));

        assertThrows(IndexOutOfBoundsException.class, () -> ds.isNull(0, 2));

        // The message must name the axis that was wrong, not just the offending number.
        assertTrue(assertThrows(IndexOutOfBoundsException.class, () -> ds.get(0, 9)).getMessage().contains("column index"));
        assertTrue(assertThrows(IndexOutOfBoundsException.class, () -> ds.get(9, 0)).getMessage().contains("row index"));
    }

    @Test
    public void testDeepEqualityIsScopedToArrayCellsOnly() {
        // A List holding arrays must NOT become deep-compared - the change is scoped to array cells.
        final Dataset l1 = Dataset.rows(CommonUtil.asList("k"), new Object[][] { { new ArrayList<>(Arrays.asList((Object) new int[] { 1 })) } });
        final Dataset l2 = Dataset.rows(CommonUtil.asList("k"), new Object[][] { { new ArrayList<>(Arrays.asList((Object) new int[] { 1 })) } });
        assertNotEquals(l1, l2);

        // Nested arrays inside an Object[] cell are compared and hashed consistently.
        final Dataset n1 = Dataset.rows(CommonUtil.asList("k"), new Object[][] { { new Object[] { new int[] { 9 } } } });
        final Dataset n2 = Dataset.rows(CommonUtil.asList("k"), new Object[][] { { new Object[] { new int[] { 9 } } } });
        assertEquals(n1, n2);
        assertEquals(n1.hashCode(), n2.hashCode());

        // Different primitive array types must not be equal even with equal contents.
        final Dataset p1 = Dataset.rows(CommonUtil.asList("k"), new Object[][] { { new int[] { 1 } } });
        final Dataset p2 = Dataset.rows(CommonUtil.asList("k"), new Object[][] { { new Integer[] { 1 } } });
        assertNotEquals(p1, p2);

        // Zero-row datasets with the same columns are equal and hash alike.
        assertEquals(Dataset.rows(CommonUtil.asList("a"), new Object[0][]), Dataset.rows(CommonUtil.asList("a"), new Object[0][]));
        assertEquals(Dataset.rows(CommonUtil.asList("a"), new Object[0][]).hashCode(), Dataset.rows(CommonUtil.asList("a"), new Object[0][]).hashCode());
        assertNotEquals(Dataset.rows(CommonUtil.asList("a"), new Object[0][]), Dataset.empty());
    }

}
