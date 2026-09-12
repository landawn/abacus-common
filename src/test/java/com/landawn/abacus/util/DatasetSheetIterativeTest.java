package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.stream.Collectors;

/** Regressions for the 2026-09-05 Dataset/RowDataset/Sheet review cycle (C-014, C-015, C-017). */
public class DatasetSheetIterativeTest extends TestBase {

    // ------------------------------------------------------------------------------------------------
    // C-014 - Dataset.columns(...) must reject a null column instead of reading it as an empty one.

    @Test
    void columnsArray_nullFirstColumn_isRejectedNotEmptied() {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> Dataset.columns(List.of("a"), new Object[][] { null }));
        assertTrue(e.getMessage().contains("columns[0] is null"), e.getMessage());
    }

    @Test
    void columnsArray_nullLaterColumn_isReportedByPosition() {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> Dataset.columns(List.of("a", "b"), new Object[][] { { 1 }, null }));
        assertTrue(e.getMessage().contains("columns[1] is null"), e.getMessage());

        final IllegalArgumentException e2 = assertThrows(IllegalArgumentException.class,
                () -> Dataset.columns(List.of("a", "b", "c"), new Object[][] { null, { 1 }, { 2 } }));
        assertTrue(e2.getMessage().contains("columns[0] is null"), e2.getMessage());
    }

    @Test
    void columnsArray_lengthMismatch_namesBothLengths() {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> Dataset.columns(List.of("a", "b"), new Object[][] { { 1, 2 }, { 3 } }));
        assertTrue(e.getMessage().contains("columns[1] has length 1") && e.getMessage().contains("length 2"), e.getMessage());
    }

    @Test
    void columnsArray_nullColumnNames_isRejected() {
        assertThrows(IllegalArgumentException.class, () -> Dataset.columns((Collection<String>) null, (Object[][]) null));
        assertThrows(IllegalArgumentException.class, () -> Dataset.columns((Collection<String>) null, new Object[][] { { 1 } }));
    }

    @Test
    void columnsArray_countMismatch_isReported() {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> Dataset.columns(List.of("a"), (Object[][]) null));
        assertTrue(e.getMessage().contains("'columnNames'(1)"), e.getMessage());
        assertThrows(IllegalArgumentException.class, () -> Dataset.columns(List.of("a"), new Object[][] { { 1 }, { 2 } }));
    }

    @Test
    void columnsArray_validShapesStillWork() {
        final Dataset empty = Dataset.columns(List.of(), (Object[][]) null);
        assertEquals(0, empty.columnCount());
        assertEquals(0, empty.size());
        assertEquals(0, Dataset.columns(new ArrayList<>(), new Object[0][]).columnCount());

        final Dataset zeroRows = Dataset.columns(List.of("a", "b"), new Object[][] { {}, {} });
        assertEquals(List.of("a", "b"), zeroRows.columnNames());
        assertEquals(0, zeroRows.size());

        final Dataset ds = Dataset.columns(List.of("海", "b"), new Object[][] { { 1, null }, { "😀", "x" } });
        assertEquals(2, ds.size());
        assertEquals(1, (Integer) ds.get(0, 0));
        assertNull(ds.get(1, 0));
        assertEquals("😀", ds.get(0, 1));
        ds.addRow(new Object[] { 2, "y" }); // the copied columns stay growable
        assertEquals(3, ds.size());
    }

    @Test
    void columnsCollection_nullColumn_isRejectedByPosition() {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> Dataset.columns(List.of("a"), Arrays.asList((Collection<?>) null)));
        assertTrue(e.getMessage().contains("columns[0] is null"), e.getMessage());

        final IllegalArgumentException e2 = assertThrows(IllegalArgumentException.class,
                () -> Dataset.columns(List.of("a", "b"), Arrays.asList(List.of(1), null)));
        assertTrue(e2.getMessage().contains("columns[1] is null"), e2.getMessage());
    }

    @Test
    void columnsCollection_sizeMismatch_namesBothSizes() {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> Dataset.columns(List.of("a", "b"), List.of(List.of(1), List.of(2, 3))));
        assertTrue(e.getMessage().contains("columns[1] has size 2") && e.getMessage().contains("size 1"), e.getMessage());
    }

    @Test
    void columnsCollection_nullNamesAndCountMismatch_areRejected() {
        assertThrows(IllegalArgumentException.class, () -> Dataset.columns((Collection<String>) null, (Collection<Collection<?>>) null));
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> Dataset.columns(List.of("a"), List.of()));
        assertTrue(e.getMessage().contains("'columnNames'(1)") && e.getMessage().contains("'columns'(0)"), e.getMessage());
    }

    @Test
    void columnsCollection_validShapesStillWork() {
        assertEquals(0, Dataset.columns(List.of(), (Collection<Collection<?>>) null).columnCount());
        final Dataset ds = Dataset.columns(List.of("a", "b"), List.of(Arrays.asList(1, null), List.of("x", "海")));
        assertEquals(2, ds.size());
        assertNull(ds.get(1, 0));
        assertEquals("海", ds.get(1, 1));
        final Dataset zeroRows = Dataset.columns(List.of("a"), List.of(List.of()));
        assertEquals(0, zeroRows.size());
        assertEquals(1, zeroRows.columnCount());
    }

    @Test
    void rowsCollection_nullRow_becomesAllNullRow_asDocumented() {
        final Dataset ds = Dataset.rows(List.of("a", "b"), Arrays.asList(List.of(1, 2), null));
        assertEquals(2, ds.size());
        assertNull(ds.get(1, 0));
        assertNull(ds.get(1, 1));
        assertThrows(IllegalArgumentException.class, () -> Dataset.rows(List.of("a"), new Object[][] { null }));
    }

    // ------------------------------------------------------------------------------------------------
    // C-017 - accessor checks: getColumnName on an invalidated slice, getColumn(int) bounds message.

    @Test
    void getColumnName_onInvalidatedSlice_failsFast() {
        final Dataset root = Dataset.rows(List.of("a", "b"), new Object[][] { { 1, 2 }, { 3, 4 } });
        final Dataset view = root.slice(0, 1);
        assertEquals("b", view.getColumnName(1));
        root.addRow(new Object[] { 5, 6 });
        assertThrows(ConcurrentModificationException.class, () -> view.getColumnName(0));
        assertThrows(ConcurrentModificationException.class, view::columnCount);
    }

    @Test
    void getColumnByIndex_outOfRange_namesTheColumnIndex() {
        final Dataset ds = Dataset.rows(List.of("a"), new Object[][] { { 1 } });
        final IndexOutOfBoundsException low = assertThrows(IndexOutOfBoundsException.class, () -> ds.getColumn(-1));
        assertTrue(low.getMessage().contains("column index: -1"), low.getMessage());
        final IndexOutOfBoundsException high = assertThrows(IndexOutOfBoundsException.class, () -> ds.getColumn(1));
        assertTrue(high.getMessage().contains("column index: 1"), high.getMessage());
        assertEquals(List.of(1), ds.getColumn(0));
        assertThrows(IndexOutOfBoundsException.class, () -> Dataset.empty().getColumn(0));
    }

    // ------------------------------------------------------------------------------------------------
    // C-015 - a column selection that names the same column twice is rejected up front, everywhere.

    private static Dataset abc() {
        return Dataset.rows(List.of("a", "b", "\u6D77"), new Object[][] { { 1, "x", "\uD83D\uDE00" }, { 2, "y", null } });
    }

    private static void assertDuplicateRejected(final String what, final Executable call) {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, call, what);
        assertTrue(e.getMessage().contains("Duplicated column names in the selection") && e.getMessage().contains("(a is listed more than once)"),
                what + " -> " + e.getMessage());
        assertTrue(!e.getMessage().contains("cnt") && !e.getMessage().contains("count") && !e.getMessage().contains("__pivot"), what + " -> " + e.getMessage());
    }

    @Test
    void updateColumns_duplicateSelection_isRejectedAndAppliesNothing() {
        final Dataset ds = abc();
        assertDuplicateRejected("updateColumns", () -> ds.updateColumns(List.of("a", "a"), (i, n, v) -> (Integer) v + 1));
        assertDuplicateRejected("updateColumns spaced", () -> ds.updateColumns(List.of("a", "b", "a"), (i, n, v) -> n + ":" + v));
        assertEquals(1, (Integer) ds.get(0, 0)); // untouched, not incremented twice
        assertEquals("x", ds.get(0, 1));
        ds.updateColumns(List.of("a", "b"), (i, n, v) -> n + ":" + v); // a distinct selection still works, once
        assertEquals("a:1", ds.get(0, 0));
    }

    @Test
    void exports_duplicateSelection_areRejected() {
        final Dataset ds = abc();
        assertDuplicateRejected("toJson", () -> ds.toJson(0, 2, List.of("a", "a")));
        assertDuplicateRejected("toXml", () -> ds.toXml(0, 2, List.of("a", "a")));
        assertDuplicateRejected("toCsv", () -> ds.toCsv(0, 2, List.of("a", "a")));
        assertDuplicateRejected("println", () -> ds.println(0, 2, List.of("a", "a")));
        assertEquals("[{\"a\":1, \"\u6D77\":\"\uD83D\uDE00\"}, {\"a\":2, \"\u6D77\":null}]", ds.toJson(0, 2, List.of("a", "\u6D77")));
    }

    @Test
    void derivedDatasets_duplicateSelection_failFastWithoutInternalNames() {
        final Dataset ds = abc();
        assertDuplicateRejected("copy", () -> ds.copy(List.of("a", "a")));
        assertDuplicateRejected("slice", () -> ds.slice(List.of("a", "a")));
        assertDuplicateRejected("split", () -> ds.split(1, List.of("a", "a")));
        assertDuplicateRejected("splitToList", () -> ds.splitToList(1, List.of("a", "a")));
        assertDuplicateRejected("paginate", () -> ds.paginate(List.of("a", "a"), 1));
        assertDuplicateRejected("groupBy keys", () -> ds.groupBy(List.of("a", "a")));
        assertDuplicateRejected("groupBy count", () -> ds.groupBy(List.of("a", "a"), "b", "cnt", Collectors.counting()));
        assertDuplicateRejected("rollup", () -> ds.rollup(List.of("a", "a")));
        assertDuplicateRejected("cube", () -> ds.cube(List.of("a", "a")));
        assertDuplicateRejected("mapColumns copied", () -> ds.mapColumns(List.of("a"), "out", List.of("a", "a"), arr -> arr.length()));
        assertDuplicateRejected("mapColumns from", () -> ds.mapColumns(List.of("a", "a"), "out", List.of("b"), arr -> arr.length()));
        assertDuplicateRejected("flatMapColumns", () -> ds.flatMapColumns(List.of("a", "a"), "out", List.of("b"), arr -> List.of(1)));
        assertDuplicateRejected("filter", () -> ds.filter(List.of("a", "a"), arr -> true));
        assertDuplicateRejected("distinctBy", () -> ds.distinctBy(List.of("a", "a")));
        assertDuplicateRejected("sortBy", () -> ds.sortBy(List.of("a", "a")));
        assertDuplicateRejected("topBy", () -> ds.topBy(List.of("a", "a"), 1));
    }

    @Test
    void projections_duplicateSelection_areRejected() {
        final Dataset ds = abc();
        assertDuplicateRejected("toList", () -> ds.toList(List.of("a", "a"), Object[].class));
        assertDuplicateRejected("toList supplier", () -> ds.toList(List.of("a", "a"), n -> new Object[n]));
        assertDuplicateRejected("stream", () -> ds.stream(List.of("a", "a"), Object[].class));
        assertDuplicateRejected("stream mapper", () -> ds.stream(List.of("a", "a"), (i, arr) -> arr.length()));
        assertDuplicateRejected("forEach", () -> ds.forEach(List.of("a", "a"), arr -> {
        }));
        assertDuplicateRejected("toMap", () -> ds.toMap("b", List.of("a", "a"), Object[].class));
        assertDuplicateRejected("toMultimap", () -> ds.toMultimap("b", List.of("a", "a"), Object[].class));
        assertDuplicateRejected("getRow", () -> ds.getRow(0, List.of("a", "a"), Object[].class));
        assertDuplicateRejected("firstRow", () -> ds.firstRow(List.of("a", "a"), Object[].class));
    }

    @Test
    void mutatorsAndSetOps_duplicateSelection_areRejected() {
        final Dataset ds = abc();
        assertDuplicateRejected("addColumn from", () -> ds.addColumn("out", List.of("a", "a"), arr -> arr.length()));
        assertDuplicateRejected("combineColumns", () -> ds.combineColumns(List.of("a", "a"), "out", Object[].class));
        assertDuplicateRejected("renameColumns", () -> ds.renameColumns(List.of("a", "a"), n -> n + "2"));
        assertDuplicateRejected("removeDuplicateRowsBy", () -> ds.removeDuplicateRowsBy(List.of("a", "a")));
        assertDuplicateRejected("unionBy", () -> ds.unionBy(abc(), List.of("a", "a")));
        assertDuplicateRejected("intersectBy", () -> ds.intersectBy(abc(), List.of("a", "a")));
        assertDuplicateRejected("exceptBy", () -> ds.exceptBy(abc(), List.of("a", "a")));
        assertDuplicateRejected("intersectAllBy", () -> ds.intersectAllBy(abc(), List.of("a", "a")));
        assertDuplicateRejected("exceptAllBy", () -> ds.exceptAllBy(abc(), List.of("a", "a")));
        assertDuplicateRejected("semiJoin", () -> ds.semiJoin(abc(), List.of("a", "a")));
        assertDuplicateRejected("antiJoin", () -> ds.antiJoin(abc(), List.of("a", "a")));
        assertEquals(List.of("a", "b", "\u6D77"), ds.columnNames());
        assertEquals(2, ds.size());
    }

    @Test
    void moveColumns_duplicateSelection_isRejectedRegardlessOfPosition() {
        final Dataset ds = abc();
        // Previously the bound check ran first, so a repeated name at an "impossible" position surfaced as
        // IndexOutOfBoundsException instead of the duplicate complaint.
        assertDuplicateRejected("moveColumns pos 0", () -> ds.moveColumns(List.of("a", "a"), 0));
        assertDuplicateRejected("moveColumns pos 2", () -> ds.moveColumns(List.of("a", "a"), 2));
        assertDuplicateRejected("moveColumns 3 names", () -> ds.moveColumns(List.of("b", "a", "a"), 0));
        assertThrows(IndexOutOfBoundsException.class, () -> ds.moveColumns(List.of("a", "b"), 2));
        ds.moveColumns(List.of("\u6D77", "a"), 0);
        assertEquals(List.of("\u6D77", "a", "b"), ds.columnNames());
    }

    @Test
    void removeColumns_stillTreatsARepeatedNameAsOne() {
        final Dataset ds = abc();
        ds.removeColumns(List.of("b", "b"));
        assertEquals(List.of("a", "\u6D77"), ds.columnNames());
        assertEquals(2, ds.size());
        assertEquals("\uD83D\uDE00", ds.get(0, 1));
        assertThrows(IllegalArgumentException.class, () -> ds.removeColumns(List.of("a", "nope")));
    }

    @Test
    void lookupAndIdentitySelections_areUnaffected() {
        final Dataset ds = abc();
        assertEquals("[0, 0]", Arrays.toString(ds.getColumnIndexes(List.of("a", "a"))));
        assertEquals(2, ds.toList(ds.columnNames(), Object[].class).size()); // identity fast path
        assertEquals(2, ds.copy(ds.columnNames()).size());
        assertEquals(1, ds.copy(List.of("\u6D77")).columnCount());
        assertEquals(0, Dataset.empty().toList(List.of(), Object[].class).size()); // empty selection on a zero-column Dataset
        assertThrows(IllegalArgumentException.class, () -> ds.toList(List.of(), Object[].class));
        assertThrows(IllegalArgumentException.class, () -> ds.toList((Collection<String>) null, Object[].class));
        assertThrows(IllegalArgumentException.class, () -> ds.toList(List.of("a", "nope"), Object[].class));
    }

    // ------------------------------------------------------------------------------------------------
    // C-018 - toCsv renders a null cell as the unquoted text null (now documented).

    @Test
    void toCsv_nullCell_isWrittenAsUnquotedNullText() {
        final Dataset ds = Dataset.rows(List.of("a", "b"), new Object[][] { { null, "x" }, { 1, null } });
        assertEquals("\"a\",\"b\"\nnull,\"x\"\n1,null", ds.toCsv());
    }
}
