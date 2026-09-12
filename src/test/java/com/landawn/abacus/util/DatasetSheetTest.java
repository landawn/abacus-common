package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.stream.Stream;

/**
 * Regression tests for the second 2026-09-01 review pass over {@code Dataset}, {@code RowDataset} and
 * {@code Sheet}.
 *
 * <p>One nested class per reported finding, named after it. Every test in here fails on the pre-fix classes
 * unless its javadoc says it only pins already-correct behaviour.</p>
 */
public class DatasetSheetTest extends TestBase {

    private static Dataset ds(final int rows, final int columns) {
        final List<String> names = new ArrayList<>(columns);
        final List<List<Object>> data = new ArrayList<>(columns);

        for (int c = 0; c < columns; c++) {
            names.add("c" + c);
            final List<Object> column = new ArrayList<>(rows);

            for (int r = 0; r < rows; r++) {
                column.add(r);
            }

            data.add(column);
        }

        return Dataset.columns(names, data);
    }

    /** A bean whose second getter throws, so serializing it fails after a partial value has been written. */
    public static final class HalfSerializable {
        private String a = "aaa";
        private String b = "bbb";

        public String getA() {
            return a;
        }

        public void setA(final String a) {
            this.a = a;
        }

        public String getB() {
            throw new IllegalStateException("boom");
        }

        public void setB(final String b) {
            this.b = b;
        }

        @Override
        public String toString() {
            return "Boom!";
        }
    }

    /** An insertion-ordered column -&gt; target-type map, so the conversion order under test is deterministic. */
    private static Map<String, Class<?>> linkedTypes(final String n1, final Class<?> t1, final String n2, final Class<?> t2) {
        final Map<String, Class<?>> map = new LinkedHashMap<>();
        map.put(n1, t1);
        map.put(n2, t2);
        return map;
    }

    /** Minimal bean with an id, for the {@code toMergedEntities} null-id pin. */
    public static class MergeBean {
        private Integer id;
        private String name;

        public Integer getId() {
            return id;
        }

        public void setId(final Integer id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }
    }

    // ------------------------------------------------------------------------------------------------
    // B1 - filter(...) sized its result columns from the whole Dataset instead of the scanned range.
    // ------------------------------------------------------------------------------------------------
    @Nested
    public class B1_FilterCapacityIsBoundedByTheScannedRange {

        /**
         * The capacity is not observable through the public API, so this measures the allocation instead: on
         * the pre-fix code a 10-row scan over a 200,000-row x 8-column Dataset reserved ~0.8 x 200,000 slots
         * per column (~5 MB); it is now bounded by the 10 rows actually scanned.
         */
        @Test
        public void filterOverASmallRangeDoesNotAllocateWholeDatasetColumns() {
            final Dataset large = ds(200_000, 8);
            large.filter(0, 10, row -> false); // warm up

            final long before = allocatedBytes();
            final Dataset result = large.filter(0, 10, row -> false);
            final long allocated = allocatedBytes() - before;

            assertEquals(0, result.size());
            assertTrue(allocated < 1_000_000L, "filter(0, 10, ...) allocated " + allocated + " bytes; expected well under 1 MB");
        }

        @Test
        public void filterStillReturnsTheCorrectRowsForEveryOverload() {
            final Dataset d = Dataset.rows(CommonUtil.asList("a", "b", "c"), new Object[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });

            assertEquals(CommonUtil.asList(1, 7), d.filter(0, 3, "a", v -> !CommonUtil.equals(v, 4)).getColumn("a"));
            assertEquals(CommonUtil.asList(4), d.filter(1, 2, row -> true).getColumn("a"));
            assertEquals(CommonUtil.asList(1, 4), d.filter(0, 2, CommonUtil.asList("a", "b"), row -> true).getColumn("a"));
            assertEquals(CommonUtil.asList(4, 7), d.filter(1, 3, Tuple.of("a", "b"), (x, y) -> true).getColumn("a"));
            assertEquals(CommonUtil.asList(7), d.filter(2, 3, Tuple.of("a", "b", "c"), (x, y, z) -> true).getColumn("a"));
            // max still caps the result
            assertEquals(CommonUtil.asList(1), d.filter(0, 3, row -> true, 1).getColumn("a"));
            assertEquals(0, d.filter(0, 3, row -> true, 0).size());
        }

        private long allocatedBytes() {
            return ((com.sun.management.ThreadMXBean) java.lang.management.ManagementFactory.getThreadMXBean())
                    .getThreadAllocatedBytes(Thread.currentThread().getId());
        }
    }

    // ------------------------------------------------------------------------------------------------
    // B2 - a cell whose serialization failed part way left the partial value in the output and then
    //      appended the toString() fallback on top of it, producing unparseable JSON/XML.
    // ------------------------------------------------------------------------------------------------
    @Nested
    public class B2_PartialSerializationDoesNotCorruptTheOutput {

        @Test
        public void toJsonStaysParseableWhenACellFailsToSerialize() {
            final Dataset d = Dataset.rows(CommonUtil.asList("x", "y"), new Object[][] { { new HalfSerializable(), 1 } });

            final String json = d.toJson();

            // pre-fix: [{"x":{"a": "aaa""Boom!", "y":1}]  -> ParsingException on read-back
            final List<?> parsed = N.fromJson(json, List.class);
            assertEquals(1, parsed.size());
            final Map<?, ?> row = (Map<?, ?>) parsed.get(0);
            assertEquals("Boom!", row.get("x"));
            assertEquals(1, ((Number) row.get("y")).intValue());
        }

        @Test
        public void toXmlStaysWellFormedWhenACellFailsToSerialize() {
            final Dataset d = Dataset.rows(CommonUtil.asList("x", "y"), new Object[][] { { new HalfSerializable(), 1 } });

            final String xml = d.toXml();

            // pre-fix: <x><boom><a>aaa</a>Boom!</x> -- <boom> never closed
            assertFalse(xml.contains("<a>aaa</a>"), "partial inner value leaked into the output: " + xml);
            assertTrue(xml.contains("<x>Boom!</x>"), xml);
            assertTrue(xml.contains("<y>1</y>"), xml);
        }

        @Test
        public void aCellThatSerializesNormallyIsStillWrittenAsAnObject() {
            final Dataset d = Dataset.rows(CommonUtil.asList("x"), new Object[][] { { CommonUtil.asMap("k", "v") } });

            assertEquals("[{\"x\":{\"k\": \"v\"}}]", d.toJson());
        }
    }

    // ------------------------------------------------------------------------------------------------
    // B3 - mutators that changed nothing still bumped modCount and invalidated live lazy sources.
    // ------------------------------------------------------------------------------------------------
    @Nested
    public class B3_NoOpMutatorsAreNotStructuralModifications {

        @Test
        public void moveRowsWithAnEmptyRangeLeavesStreamsValid() {
            final Dataset d = Dataset.rows(CommonUtil.asList("id"), new Object[][] { { 1 }, { 2 }, { 3 } });
            final Stream<Object> s = d.stream("id");

            d.moveRows(1, 1, 0);

            assertEquals(CommonUtil.asList(1, 2, 3), s.toList());
        }

        @Test
        public void moveRowsStillValidatesTheNewPositionForAnEmptyRange() {
            final Dataset d = Dataset.rows(CommonUtil.asList("id"), new Object[][] { { 1 }, { 2 }, { 3 } });

            assertThrows(IndexOutOfBoundsException.class, () -> d.moveRows(1, 1, -1));
            assertThrows(IndexOutOfBoundsException.class, () -> d.moveRows(1, 1, 4));
            assertThrows(IndexOutOfBoundsException.class, () -> d.moveRows(-1, -1, 0));
        }

        @Test
        public void moveRowsStillMovesWhenTheRangeIsNotEmpty() {
            final Dataset d = Dataset.rows(CommonUtil.asList("id"), new Object[][] { { 1 }, { 2 }, { 3 } });

            d.moveRows(0, 1, 2);

            assertEquals(CommonUtil.asList(2, 3, 1), d.getColumn("id"));
        }

        @Test
        public void removeDuplicateRowsByLeavesStreamsValidWhenNothingIsDuplicated() {
            final Dataset single = Dataset.rows(CommonUtil.asList("id"), new Object[][] { { 1 }, { 2 }, { 3 } });
            final Stream<Object> s1 = single.stream("id");
            single.removeDuplicateRowsBy("id");
            assertEquals(CommonUtil.asList(1, 2, 3), s1.toList());

            final Dataset multi = Dataset.rows(CommonUtil.asList("a", "b"), new Object[][] { { 1, 1 }, { 1, 2 }, { 2, 1 } });
            final Stream<Object> s2 = multi.stream("a");
            multi.removeDuplicateRowsBy(CommonUtil.asList("a", "b"));
            assertEquals(CommonUtil.asList(1, 1, 2), s2.toList());
        }

        @Test
        public void removeDuplicateRowsByStillRemovesAndStillInvalidates() {
            final Dataset single = Dataset.rows(CommonUtil.asList("id"), new Object[][] { { 1 }, { 1 }, { 2 } });
            final Stream<Object> s1 = single.stream("id");
            single.removeDuplicateRowsBy("id");
            assertEquals(CommonUtil.asList(1, 2), single.getColumn("id"));
            assertThrows(ConcurrentModificationException.class, s1::toList);

            final Dataset multi = Dataset.rows(CommonUtil.asList("a", "b"), new Object[][] { { 1, 1 }, { 1, 1 }, { 2, 1 } });
            final Stream<Object> s2 = multi.stream("a");
            multi.removeDuplicateRowsBy(CommonUtil.asList("a", "b"));
            assertEquals(CommonUtil.asList(1, 2), multi.getColumn("a"));
            assertThrows(ConcurrentModificationException.class, s2::toList);
        }

        @Test
        public void clearOnAnAlreadyEmptyDatasetLeavesStreamsValid() {
            final Dataset d = Dataset.rows(CommonUtil.asList("id"), new Object[0][]);
            final Stream<Object> s = d.stream("id");

            d.clear();

            assertEquals(0, s.toList().size());
        }

        @Test
        public void clearStillClearsAndStillInvalidates() {
            final Dataset d = Dataset.rows(CommonUtil.asList("id"), new Object[][] { { 1 } });
            final Stream<Object> s = d.stream("id");

            d.clear();

            assertEquals(0, d.size());
            assertEquals(1, d.columnCount());
            assertThrows(ConcurrentModificationException.class, s::toList);
        }

        @Test
        public void clearOnAFrozenDatasetStillThrowsEvenWhenEmpty() {
            final Dataset d = Dataset.rows(CommonUtil.asList("id"), new Object[0][]);
            d.freeze();

            assertThrows(IllegalStateException.class, d::clear);
        }
    }

    // ------------------------------------------------------------------------------------------------
    // B4 - a failed conversion left the Dataset half converted.
    // ------------------------------------------------------------------------------------------------
    @Nested
    public class B4_ColumnConversionIsAllOrNothing {

        @Test
        public void convertColumnLeavesTheColumnUntouchedWhenAValueCannotBeConverted() {
            final Dataset d = Dataset.rows(CommonUtil.asList("v"), new Object[][] { { "1" }, { "x" }, { "3" } });

            assertThrows(NumberFormatException.class, () -> d.convertColumn("v", Integer.class));

            // pre-fix: [1, x, 3] -- the first value had already been replaced by an Integer
            assertEquals(CommonUtil.asList("1", "x", "3"), d.getColumn("v"));
        }

        @Test
        public void convertColumnsLeavesEveryColumnUntouchedWhenOneFails() {
            final Dataset d = Dataset.rows(CommonUtil.asList("a", "b"), new Object[][] { { "1", "x" }, { "2", "y" } });

            assertThrows(NumberFormatException.class, () -> d.convertColumns(linkedTypes("a", Integer.class, "b", Integer.class)));

            // pre-fix: column "a" was already converted to [1, 2] before "b" failed
            assertEquals(CommonUtil.asList("1", "2"), d.getColumn("a"));
            assertEquals(CommonUtil.asList("x", "y"), d.getColumn("b"));
        }

        @Test
        public void aSuccessfulConversionStillWritesThroughToExistingColumnViews() {
            final Dataset d = Dataset.rows(CommonUtil.asList("v"), new Object[][] { { "1" }, { "2" } });
            final ImmutableList<Object> view = d.getColumn("v");

            d.convertColumn("v", Integer.class);

            assertEquals(CommonUtil.asList(1, 2), d.getColumn("v"));
            assertEquals(CommonUtil.asList(1, 2), view, "the column view must still be attached after conversion");
        }

        @Test
        public void convertColumnsStillConvertsEveryColumnOnSuccess() {
            final Dataset d = Dataset.rows(CommonUtil.asList("a", "b"), new Object[][] { { "1", "2.5" }, { "3", "4.5" } });

            d.convertColumns(linkedTypes("a", Integer.class, "b", Double.class));

            assertEquals(CommonUtil.asList(1, 3), d.getColumn("a"));
            assertEquals(CommonUtil.asList(2.5d, 4.5d), d.getColumn("b"));
        }
    }

    // ------------------------------------------------------------------------------------------------
    // B5 - moveRow/moveColumn discarded the key-index map instead of repairing it.
    // ------------------------------------------------------------------------------------------------
    @Nested
    public class B5_SheetMoveRepairsTheKeyIndexMap {

        @Test
        public void aLiveCellStreamSurvivesAMoveRow() {
            final Sheet<String, String, Integer> sh = Sheet.rows(CommonUtil.asList("r1", "r2", "r3"), CommonUtil.asList("c1"),
                    new Integer[][] { { 1 }, { 2 }, { 3 } });
            final Stream<Sheet.Cell<String, String, Integer>> s = sh.rowMajorCells();

            sh.moveRow("r1", 2);

            // pre-fix: NullPointerException, because _rowKeyIndexMap had been set to null. Asserting the
            // rendered cells, not just the count, also pins that the repaired map maps every index to the
            // key that now sits there: r1 moved to the end, so row-major order is r2, r3, r1.
            assertEquals(CommonUtil.asList("r2/c1/2", "r3/c1/3", "r1/c1/1"), s.map(c -> c.rowKey() + "/" + c.columnKey() + "/" + c.value()).toList());
        }

        @Test
        public void aLiveCellStreamSurvivesAMoveColumn() {
            final Sheet<String, String, Integer> sh = Sheet.rows(CommonUtil.asList("r1"), CommonUtil.asList("c1", "c2", "c3"), new Integer[][] { { 1, 2, 3 } });
            final Stream<Sheet.Cell<String, String, Integer>> s = sh.columnMajorCells();

            sh.moveColumn("c1", 2);

            assertEquals(CommonUtil.asList("r1/c2/2", "r1/c3/3", "r1/c1/1"), s.map(c -> c.rowKey() + "/" + c.columnKey() + "/" + c.value()).toList());
        }

        /** Exhaustively checks the repaired map against a freshly built one, for every move on sizes 1..7. */
        @Test
        public void theRepairedRowIndexMapMatchesAFreshlyBuiltOneForEveryMove() {
            final List<String> columnKeys = CommonUtil.asList("c0", "c1");

            for (int n = 1; n <= 7; n++) {
                final List<String> rowKeys = new ArrayList<>();
                final Object[][] values = new Object[n][2];

                for (int i = 0; i < n; i++) {
                    rowKeys.add("r" + i);
                    values[i][0] = i;
                    values[i][1] = i * 10;
                }

                for (int from = 0; from < n; from++) {
                    for (int to = 0; to < n; to++) {
                        final String where = "n=" + n + " from=" + from + " to=" + to;

                        final Sheet<String, String, Object> moved = Sheet.rows(rowKeys, columnKeys, values);
                        moved.get("r0", "c0"); // force the index map to be built BEFORE the move
                        moved.moveRow("r" + from, to);

                        // Same move on a Sheet whose index map is only built afterwards, i.e. from scratch.
                        final Sheet<String, String, Object> rebuilt = Sheet.rows(rowKeys, columnKeys, values);
                        rebuilt.moveRow("r" + from, to);

                        assertEquals(new ArrayList<>(rebuilt.rowKeySet()), new ArrayList<>(moved.rowKeySet()), where);

                        for (final String rowKey : rebuilt.rowKeySet()) {
                            assertEquals(rebuilt.get(rowKey, "c0"), moved.get(rowKey, "c0"), where + " key=" + rowKey);
                            assertEquals(rebuilt.get(rowKey, "c1"), moved.get(rowKey, "c1"), where + " key=" + rowKey);
                        }

                        // Every index must resolve back to the right key through the repaired inverse map.
                        final List<String> keysInOrder = new ArrayList<>();

                        for (final Sheet.Cell<String, String, Object> cell : moved.rowMajorCells().toList()) {
                            if (CommonUtil.equals(cell.columnKey(), "c0")) {
                                keysInOrder.add(cell.rowKey());
                            }
                        }

                        assertEquals(new ArrayList<>(moved.rowKeySet()), keysInOrder, where);
                    }
                }
            }
        }

        /** The column twin of the exhaustive row check. */
        @Test
        public void theRepairedColumnIndexMapMatchesAFreshlyBuiltOneForEveryMove() {
            final List<String> rowKeys = CommonUtil.asList("r0", "r1");

            for (int n = 1; n <= 7; n++) {
                final List<String> columnKeys = new ArrayList<>();
                final Object[][] values = new Object[2][n];

                for (int i = 0; i < n; i++) {
                    columnKeys.add("c" + i);
                    values[0][i] = i;
                    values[1][i] = i * 10;
                }

                for (int from = 0; from < n; from++) {
                    for (int to = 0; to < n; to++) {
                        final String where = "n=" + n + " from=" + from + " to=" + to;

                        final Sheet<String, String, Object> moved = Sheet.rows(rowKeys, columnKeys, values);
                        moved.get("r0", "c0");
                        moved.moveColumn("c" + from, to);

                        final Sheet<String, String, Object> rebuilt = Sheet.rows(rowKeys, columnKeys, values);
                        rebuilt.moveColumn("c" + from, to);

                        assertEquals(new ArrayList<>(rebuilt.columnKeySet()), new ArrayList<>(moved.columnKeySet()), where);

                        for (final String columnKey : rebuilt.columnKeySet()) {
                            assertEquals(rebuilt.get("r0", columnKey), moved.get("r0", columnKey), where + " key=" + columnKey);
                            assertEquals(rebuilt.get("r1", columnKey), moved.get("r1", columnKey), where + " key=" + columnKey);
                        }

                        final List<String> keysInOrder = new ArrayList<>();

                        for (final Sheet.Cell<String, String, Object> cell : moved.columnMajorCells().toList()) {
                            if (CommonUtil.equals(cell.rowKey(), "r0")) {
                                keysInOrder.add(cell.columnKey());
                            }
                        }

                        assertEquals(new ArrayList<>(moved.columnKeySet()), keysInOrder, where);
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------------------------------------
    // B6 - toDataset()'s key-collision diagnostic came from the RowDataset constructor.
    // ------------------------------------------------------------------------------------------------
    @Nested
    public class B6_ToDatasetNamesTheOffendingKeys {

        @Test
        public void collidingColumnKeysAreReportedWithBothKeys() {
            final Sheet<String, Object, Integer> sh = Sheet.rows(CommonUtil.asList("r"), CommonUtil.asList((Object) Integer.valueOf(1), "1"),
                    new Integer[][] { { 10, 20 } });

            final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, sh::toDataset);

            assertTrue(e.getMessage().contains("Column keys"), e.getMessage());
            assertTrue(e.getMessage().contains("\"1\""), e.getMessage());
        }

        @Test
        public void anEmptyColumnKeyNameIsReportedAsSuch() {
            final Sheet<String, String, Integer> sh = Sheet.rows(CommonUtil.asList("r"), CommonUtil.asList(""), new Integer[][] { { 10 } });

            final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, sh::toDataset);

            assertTrue(e.getMessage().contains("empty Dataset column name"), e.getMessage());
        }

        @Test
        public void collidingRowKeysAreReportedByToTransposedDataset() {
            final Sheet<Object, String, Integer> sh = Sheet.rows(CommonUtil.asList((Object) Integer.valueOf(1), "1"), CommonUtil.asList("c"),
                    new Integer[][] { { 10 }, { 20 } });

            final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, sh::toTransposedDataset);

            assertTrue(e.getMessage().contains("Row keys"), e.getMessage());
        }

        @Test
        public void aNormalSheetStillConverts() {
            final Sheet<String, String, Integer> sh = Sheet.rows(CommonUtil.asList("r1", "r2"), CommonUtil.asList("c1", "c2"),
                    new Integer[][] { { 1, 2 }, { 3, 4 } });

            assertEquals(CommonUtil.asList("c1", "c2"), sh.toDataset().columnNames());
            assertEquals(CommonUtil.asList("r1", "r2"), sh.toTransposedDataset().columnNames());
            assertEquals(CommonUtil.asList(1, 3), sh.toDataset().getColumn("c1"));
        }
    }

    // ------------------------------------------------------------------------------------------------
    // B7 - rollup/cube hard-wired "count" and failed on a Dataset that had a key column of that name.
    // ------------------------------------------------------------------------------------------------
    @Nested
    public class B7_CountColumnNameAvoidsTheKeyColumns {

        @Test
        public void rollupWorksWhenAKeyColumnIsNamedCount() {
            final Dataset d = Dataset.rows(CommonUtil.asList("count", "x"), new Object[][] { { "a", 1 }, { "a", 2 }, { "b", 1 } });

            final List<Dataset> levels = d.rollup(CommonUtil.asList("count", "x")).toList();

            assertEquals(3, levels.size());
            assertEquals(CommonUtil.asList("count", "x", "count_2"), levels.get(0).columnNames());
            assertEquals(CommonUtil.asList("count", "count_2"), levels.get(1).columnNames());
            assertEquals(CommonUtil.asList("count_2"), levels.get(2).columnNames());
            assertEquals(CommonUtil.asList(3), levels.get(2).getColumn("count_2"));
        }

        @Test
        public void cubeWorksWhenAKeyColumnIsNamedCount() {
            final Dataset d = Dataset.rows(CommonUtil.asList("count", "x"), new Object[][] { { "a", 1 }, { "a", 2 } });

            final List<Dataset> levels = d.cube(CommonUtil.asList("count", "x")).toList();

            assertEquals(4, levels.size());
            assertTrue(levels.get(0).columnNames().contains("count_2"), levels.get(0).columnNames().toString());
        }

        @Test
        public void rollupWithAKeyExtractorWorksWhenAKeyColumnIsNamedCount() {
            final Dataset d = Dataset.rows(CommonUtil.asList("count", "x"), new Object[][] { { "a", 1 }, { "a", 2 } });

            final List<Dataset> levels = d.rollup(CommonUtil.asList("count", "x"), row -> row.join("-")).toList();

            assertEquals(3, levels.size());
            assertTrue(levels.get(0).columnNames().contains("count_2"), levels.get(0).columnNames().toString());
        }

        @Test
        public void cubeWithAKeyExtractorWorksWhenAKeyColumnIsNamedCount() {
            final Dataset d = Dataset.rows(CommonUtil.asList("count", "x"), new Object[][] { { "a", 1 }, { "a", 2 } });

            final List<Dataset> levels = d.cube(CommonUtil.asList("count", "x"), row -> row.join("-")).toList();

            assertEquals(4, levels.size());
            assertTrue(levels.get(0).columnNames().contains("count_2"), levels.get(0).columnNames().toString());
        }

        @Test
        public void theOrdinaryCaseStillUsesPlainCount() {
            final Dataset d = Dataset.rows(CommonUtil.asList("region", "city"), new Object[][] { { "N", "a" }, { "N", "b" } });

            final List<Dataset> levels = d.rollup(CommonUtil.asList("region", "city")).toList();

            assertEquals(CommonUtil.asList("region", "city", "count"), levels.get(0).columnNames());
            assertEquals(CommonUtil.asList(2), levels.get(2).getColumn("count"));
        }

        @Test
        public void theSuffixSkipsAnAlreadyTakenCount2() {
            final Dataset d = Dataset.rows(CommonUtil.asList("count", "count_2", "x"), new Object[][] { { "a", "b", 1 } });

            final List<Dataset> levels = d.rollup(CommonUtil.asList("count", "count_2", "x")).toList();

            // "count" and "count_2" are both key columns here, so only the exact list distinguishes the
            // aggregate column from a key that happens to share its name.
            assertEquals(CommonUtil.asList("count", "count_2", "x", "count_3"), levels.get(0).columnNames());
        }
    }

    // ------------------------------------------------------------------------------------------------
    // B8 - Paginated cached every page it ever produced.
    // ------------------------------------------------------------------------------------------------
    @Nested
    public class B8_PaginatedDoesNotRetainEveryPage {

        /**
         * Only the most recently returned page is kept, so re-reading an older page rebuilds it. On the
         * pre-fix code every page ever produced stayed in an unbounded map, and this returned the same
         * instance - which is exactly what made a full traversal retain every page.
         */
        @Test
        public void onlyTheMostRecentPageIsCached() {
            final Dataset d = Dataset.rows(CommonUtil.asList("a"), new Object[][] { { 1 }, { 2 }, { 3 }, { 4 } });
            final Paginated<Dataset> paginated = d.paginate(2);

            final Dataset page0First = paginated.getPage(0);
            paginated.getPage(1);
            final Dataset page0Again = paginated.getPage(0);

            assertNotSame(page0First, page0Again);
            assertEquals(CommonUtil.asList(1, 2), page0Again.getColumn("a"));
        }

        @Test
        public void aFullTraversalStillVisitsEveryRow() {
            final Dataset d = ds(5_000, 1);
            int rows = 0;

            for (final Dataset page : d.paginate(1)) {
                rows += page.size();
            }

            assertEquals(5_000, rows);
        }

        @Test
        public void rereadingTheSamePageStillReturnsTheCachedInstance() {
            final Dataset d = Dataset.rows(CommonUtil.asList("a"), new Object[][] { { 1 }, { 2 }, { 3 }, { 4 } });
            final Paginated<Dataset> paginated = d.paginate(2);

            assertSame(paginated.getPage(1), paginated.getPage(1));
            assertEquals(CommonUtil.asList(3, 4), paginated.getPage(1).getColumn("a"));
            assertEquals(CommonUtil.asList(1, 2), paginated.getPage(0).getColumn("a"));
            assertEquals(2, paginated.totalPages());
        }

        @Test
        public void pageBoundsAndConcurrentModificationStillBehave() {
            final Dataset d = Dataset.rows(CommonUtil.asList("a"), new Object[][] { { 1 }, { 2 }, { 3 } });
            final Paginated<Dataset> paginated = d.paginate(2);

            assertThrows(IllegalArgumentException.class, () -> paginated.getPage(-1));
            assertThrows(IllegalArgumentException.class, () -> paginated.getPage(2));
            assertEquals(CommonUtil.asList(3), paginated.lastPage().get().getColumn("a"));
            assertEquals(CommonUtil.asList(1, 2), paginated.firstPage().get().getColumn("a"));

            d.addRow(new Object[] { 4 });
            assertThrows(ConcurrentModificationException.class, () -> paginated.getPage(0));
        }
    }

    // ------------------------------------------------------------------------------------------------
    // B9 - a zero-row table printed as a single cell spanning every column.
    // ------------------------------------------------------------------------------------------------
    @Nested
    public class B9_ZeroRowPrintlnKeepsColumnSeparators {

        @Test
        public void datasetWithColumnsButNoRows() {
            final Dataset d = Dataset.rows(CommonUtil.asList("a", "bb"), new Object[0][]);
            final StringWriter writer = new StringWriter();

            d.println(0, 0, d.columnNames(), writer);

            assertEquals("+---+----+\n" //
                    + "| a | bb |\n" //
                    + "+---+----+\n" //
                    + "|   |    |\n" //
                    + "+---+----+\n", writer.toString());
        }

        @Test
        public void sheetWithColumnsButNoRows() {
            final Sheet<String, String, Integer> sh = new Sheet<>(new ArrayList<>(), CommonUtil.asList("c1", "c2"));
            final StringWriter writer = new StringWriter();

            sh.println(sh.rowKeySet(), sh.columnKeySet(), null, writer);

            // pre-fix the filler row was "|                 |" -- one cell spanning the row-key column and both
            // data columns; it now has the same framing as a data row would.
            //
            // The width-4 columns this used to assert came from an uninitialized Sheet reserving room for the
            // literal "null" even when it printed none: with no rows the fill is a no-op. That made a Sheet
            // emptied by removeRow and one built empty - which are equals() - render differently, and made this
            // Sheet case wider than the Dataset case above. The columns are now sized by their keys.
            assertEquals("    +----+----+\n" //
                    + "    | c1 | c2 |\n" //
                    + "+---+----+----+\n" //
                    + "|   |    |    |\n" //
                    + "+---+----+----+\n", writer.toString());
        }

        @Test
        public void aNonEmptyTableIsUnchanged() {
            final Dataset d = Dataset.rows(CommonUtil.asList("a", "bb"), new Object[][] { { 1, 2 } });
            final StringWriter writer = new StringWriter();

            d.println(0, 1, d.columnNames(), writer);

            assertEquals("+---+----+\n" //
                    + "| a | bb |\n" //
                    + "+---+----+\n" //
                    + "| 1 | 2  |\n" //
                    + "+---+----+\n", writer.toString());
        }
    }

    // ------------------------------------------------------------------------------------------------
    // J4 - the Class-taking groupBy overloads had no eager duplicate-result-name check, so a collision
    //      surfaced as the RowDataset constructor's "Duplicated column names found in: [k, k]".
    // ------------------------------------------------------------------------------------------------
    @Nested
    public class J4_GroupByReportsADuplicateResultNameLikeItsSiblings {

        @Test
        public void singleKeyColumnWithARowType() {
            final Dataset d = Dataset.rows(CommonUtil.asList("k", "v"), new Object[][] { { "a", 1 } });

            final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> d.groupBy("k", CommonUtil.asList("v"), "k", Object[].class));

            assertEquals("Duplicate property name: k", e.getMessage());
        }

        @Test
        public void multipleKeyColumnsWithARowType() {
            final Dataset d = Dataset.rows(CommonUtil.asList("k1", "k2", "v"), new Object[][] { { "a", "b", 1 } });

            final IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                    () -> d.groupBy(CommonUtil.asList("k1", "k2"), CommonUtil.asList("v"), "k2", Object[].class));

            assertEquals("Duplicate property name: k2", e.getMessage());
        }

        @Test
        public void withAKeyExtractorAndARowType() {
            final Dataset d = Dataset.rows(CommonUtil.asList("k", "v"), new Object[][] { { "a", 1 } });

            assertEquals("Duplicate property name: k",
                    assertThrows(IllegalArgumentException.class, () -> d.groupBy("k", Fn.identity(), CommonUtil.asList("v"), "k", Object[].class))
                            .getMessage());

            final Dataset d2 = Dataset.rows(CommonUtil.asList("k1", "k2", "v"), new Object[][] { { "a", "b", 1 } });

            assertEquals("Duplicate property name: k1", assertThrows(IllegalArgumentException.class,
                    () -> d2.groupBy(CommonUtil.asList("k1", "k2"), row -> row.join("-"), CommonUtil.asList("v"), "k1", Object[].class)).getMessage());
        }

        @Test
        public void aNonCollidingResultNameStillWorks() {
            final Dataset d = Dataset.rows(CommonUtil.asList("k", "v"), new Object[][] { { "a", 1 }, { "a", 2 }, { "b", 3 } });

            final Dataset grouped = d.groupBy("k", CommonUtil.asList("v"), "vals", Object[].class);

            assertEquals(CommonUtil.asList("k", "vals"), grouped.columnNames());
            assertEquals(2, grouped.size());
        }
    }

    // ------------------------------------------------------------------------------------------------
    // Documented-behaviour pins: these already passed before the fixes; they guard the new javadoc.
    // ------------------------------------------------------------------------------------------------
    @Nested
    public class DocumentedBehaviourPins {

        @Test
        public void unionAllConcatenatesAndNeverMatchesRows() {
            final Dataset a = Dataset.rows(CommonUtil.asList("id", "x"), new Object[][] { { 1, 9 }, { 1, 9 } });
            final Dataset b = Dataset.rows(CommonUtil.asList("id"), new Object[][] { { 1 } });

            assertThrows(IllegalArgumentException.class, () -> a.unionAll(b));
            final Dataset result = a.unionAll(b, false);

            assertEquals(3, result.size());
            assertEquals(CommonUtil.asList("id", "x"), result.columnNames());
            assertEquals(CommonUtil.asList(1, 1, 1), result.getColumn("id"));
            assertEquals(CommonUtil.asList(9, 9, null), result.getColumn("x"));
        }

        @Test
        public void aPropertiesViewTakenBeforeTheFirstMergeStaysEmpty() {
            final Dataset a = Dataset.rows(CommonUtil.asList("id"), new Object[][] { { 1 } });
            final Dataset b = Dataset.rows(CommonUtil.asList("id"), new Object[][] { { 2 } });
            b.setProperties(CommonUtil.asMap("k", (Object) "v"));

            final Map<String, Object> viewBefore = a.getProperties();
            a.append(b);

            assertTrue(viewBefore.isEmpty(), "documented: the empty view is replaced, not mutated");
            assertEquals("v", a.getProperties().get("k"));
        }

        @Test
        public void aPropertiesViewIsLiveOnceTheDatasetHasProperties() {
            final Dataset a = Dataset.rows(CommonUtil.asList("id"), new Object[][] { { 1 } });
            a.setProperties(CommonUtil.asMap("x", (Object) "1"));
            final Dataset b = Dataset.rows(CommonUtil.asList("id"), new Object[][] { { 2 } });
            b.setProperties(CommonUtil.asMap("k", (Object) "v"));

            final Map<String, Object> viewBefore = a.getProperties();
            a.append(b);

            assertEquals("v", viewBefore.get("k"), "documented: an existing map is merged into in place");
        }

        @Test
        public void primitiveGettersNarrowSilently() {
            final Dataset d = Dataset.rows(CommonUtil.asList("v"), new Object[][] { { 4294967301L } });
            d.moveToRow(0);

            assertEquals(5, d.getInt("v"));
            assertEquals((byte) 5, d.getByte("v"));
            assertEquals((short) 5, d.getShort("v"));

            final Dataset f = Dataset.rows(CommonUtil.asList("v"), new Object[][] { { 3.99d } });
            f.moveToRow(0);
            assertEquals(3, f.getInt("v"));
        }

        @Test
        public void toMapKeepsTheLastRowForADuplicateKey() {
            final Dataset d = Dataset.rows(CommonUtil.asList("k", "v"), new Object[][] { { "a", 1 }, { "a", 2 } });

            assertEquals(1, d.toMap("k", "v").size());
            assertEquals(2, d.toMap("k", "v").get("a"));
        }

        @Test
        public void sortByIsStable() {
            final Dataset d = Dataset.rows(CommonUtil.asList("k", "id"), new Object[][] { { "b", 1 }, { "a", 2 }, { "b", 3 }, { "a", 4 } });

            d.sortBy("k");

            assertEquals(CommonUtil.asList("a", "a", "b", "b"), d.getColumn("k"));
            assertEquals(CommonUtil.asList(2, 4, 1, 3), d.getColumn("id"));
        }

        @Test
        public void topByKeepsTheEarliestRowsOnATie() {
            final Dataset d = Dataset.rows(CommonUtil.asList("id", "v"), new Object[][] { { 1, 5 }, { 2, 5 }, { 3, 5 }, { 4, 1 } });

            assertEquals(CommonUtil.asList(1, 2), d.topBy("v", 2).getColumn("id"));
        }

        @Test
        public void toMergedEntitiesDropsRowsWhoseIdIsNull() {
            final Dataset d = Dataset.rows(CommonUtil.asList("id", "name"), new Object[][] { { 1, "a" }, { null, "b" }, { 2, "c" } });

            assertEquals(2, d.toMergedEntities("id", MergeBean.class).size());
        }

        @Test
        public void csvUsesUnixNewlinesAndNoTrailingNewline() {
            final Dataset d = Dataset.rows(CommonUtil.asList("a", "b"), new Object[][] { { 1, "x" }, { 2, "y" } });

            assertEquals("\"a\",\"b\"\n1,\"x\"\n2,\"y\"", d.toCsv());
        }

        @Test
        public void transposedIsAlwaysMutable() {
            final Sheet<String, String, Integer> sh = Sheet.rows(CommonUtil.asList("r"), CommonUtil.asList("c"), new Integer[][] { { 1 } });
            sh.freeze();

            assertFalse(sh.transposed().isFrozen());
            assertFalse(sh.copy().isFrozen());
        }
    }

    // ------------------------------------------------------------------------------------------------
    // Equivalence guards for the rewrites that were meant to change nothing observable. These pass on
    // both the pre-fix and the fixed code; they exist so a future edit cannot quietly change behaviour.
    // ------------------------------------------------------------------------------------------------
    @Nested
    public class RewritesThatMustNotChangeBehaviour {

        /** merge() now lets addColumn build the null padding instead of hand-rolling the list. */
        @Test
        public void mergeStillPadsMissingColumnsWithNulls() {
            final Dataset a = Dataset.rows(CommonUtil.asList("id", "age"), new Object[][] { { 1, 25 }, { 2, 30 } });
            final Dataset b = Dataset.rows(CommonUtil.asList("id", "score"), new Object[][] { { 3, 95 } });

            a.merge(b);

            assertEquals(CommonUtil.asList("id", "age", "score"), a.columnNames());
            assertEquals(CommonUtil.asList(1, 2, 3), a.getColumn("id"));
            assertEquals(CommonUtil.asList(25, 30, null), a.getColumn("age"));
            assertEquals(CommonUtil.asList(null, null, 95), a.getColumn("score"));
        }

        @Test
        public void mergeIntoAnEmptyDatasetStillEstablishesTheRowCount() {
            final Dataset empty = Dataset.rows(CommonUtil.asList("id"), new Object[0][]);
            final Dataset b = Dataset.rows(CommonUtil.asList("id", "score"), new Object[][] { { 1, 9 }, { 2, 8 } });

            empty.merge(b);

            assertEquals(2, empty.size());
            assertEquals(CommonUtil.asList("id", "score"), empty.columnNames());
            assertEquals(CommonUtil.asList(9, 8), empty.getColumn("score"));
        }

        @Test
        public void mergeOfARowRangeStillPadsTheUnselectedColumns() {
            final Dataset a = Dataset.rows(CommonUtil.asList("id", "age"), new Object[][] { { 1, 25 } });
            final Dataset b = Dataset.rows(CommonUtil.asList("id", "score"), new Object[][] { { 2, 8 }, { 3, 7 }, { 4, 6 } });

            a.merge(b, 1, 3, CommonUtil.asList("id", "score"));

            assertEquals(CommonUtil.asList(1, 3, 4), a.getColumn("id"));
            assertEquals(CommonUtil.asList(25, null, null), a.getColumn("age"));
            assertEquals(CommonUtil.asList(null, 7, 6), a.getColumn("score"));
        }

        /** copy() now passes _properties straight to the constructor, which copies it. */
        @Test
        public void copyStillGetsAnIndependentPropertiesMap() {
            final Dataset d = Dataset.rows(CommonUtil.asList("a"), new Object[][] { { 1 }, { 2 } });
            d.setProperties(CommonUtil.asMap("k", (Object) "v"));

            final Dataset copy = d.copy();
            final Dataset ranged = d.copy(0, 1);
            final Dataset columns = d.copy(CommonUtil.asList("a"));

            assertEquals("v", copy.getProperties().get("k"));
            assertEquals("v", ranged.getProperties().get("k"));
            assertEquals("v", columns.getProperties().get("k"));

            d.setProperties(CommonUtil.asMap("k", (Object) "CHANGED"));

            assertEquals("v", copy.getProperties().get("k"), "the copy must not alias the source map");
            assertEquals("v", ranged.getProperties().get("k"));
        }

        /** cartesianProduct's left-hand values are now appended directly instead of via a scratch array. */
        @Test
        public void cartesianProductStillRepeatsEachLeftRow() {
            final Dataset a = Dataset.rows(CommonUtil.asList("x"), new Object[][] { { 1 }, { 2 } });
            final Dataset b = Dataset.rows(CommonUtil.asList("y"), new Object[][] { { "p" }, { "q" }, { "r" } });

            final Dataset product = a.cartesianProduct(b);

            assertEquals(6, product.size());
            assertEquals(CommonUtil.asList(1, 1, 1, 2, 2, 2), product.getColumn("x"));
            assertEquals(CommonUtil.asList("p", "q", "r", "p", "q", "r"), product.getColumn("y"));
        }

        @Test
        public void cartesianProductWithAnEmptySideIsStillEmpty() {
            final Dataset a = Dataset.rows(CommonUtil.asList("x"), new Object[][] { { 1 } });
            final Dataset empty = Dataset.rows(CommonUtil.asList("y"), new Object[0][]);

            assertEquals(0, a.cartesianProduct(empty).size());
            assertEquals(0, empty.cartesianProduct(a).size());
        }

        /** The three identical join index-map helpers were collapsed into one. */
        @Test
        public void everyJoinShapeStillProducesTheSameColumnsAndRows() {
            final Dataset left = Dataset.rows(CommonUtil.asList("id", "k2", "name"), new Object[][] { { 1, "a", "L1" }, { 2, "b", "L2" }, { 3, "c", "L3" } });
            final Dataset right = Dataset.rows(CommonUtil.asList("id", "k2", "score"), new Object[][] { { 1, "a", 10 }, { 1, "a", 11 }, { 4, "d", 40 } });
            final Map<String, String> on2 = new LinkedHashMap<>();
            on2.put("id", "id");
            on2.put("k2", "k2");

            assertEquals(2, left.innerJoin(right, "id", "id").size());
            assertEquals(2, left.innerJoin(right, on2).size());
            assertEquals(4, left.leftJoin(right, on2).size());
            assertEquals(3, left.rightJoin(right, on2).size());
            assertEquals(5, left.fullJoin(right, on2).size());

            // right-side name collisions still get the _2 suffix
            assertEquals(CommonUtil.asList("id", "k2", "name", "id_2", "k2_2", "score"), left.innerJoin(right, on2).columnNames());
        }

        /** columnIndexMap() replaced three copies of the lazy initialiser. */
        @Test
        public void columnLookupsStillTrackEveryStructuralChange() {
            final Dataset d = Dataset.rows(CommonUtil.asList("a", "b", "c"), new Object[][] { { 1, 2, 3 } });

            assertEquals(1, d.getColumnIndex("b"));
            d.renameColumn("b", "bb");
            assertEquals(1, d.getColumnIndex("bb"));
            assertTrue(d.containsColumn("bb"));
            assertFalse(d.containsColumn("b"));

            d.swapColumns("a", "c");
            assertEquals(0, d.getColumnIndex("c"));
            assertEquals(2, d.getColumnIndex("a"));

            d.moveColumn("a", 0);
            assertEquals(0, d.getColumnIndex("a"));

            d.addColumn("d", CommonUtil.asList(9));
            assertEquals(3, d.getColumnIndex("d"));

            d.removeColumn("bb");
            assertFalse(d.containsColumn("bb"));
            assertArrayEquals(new int[] { 0, 1, 2 }, d.getColumnIndexes(d.columnNames()));
        }
    }

    // ------------------------------------------------------------------------------------------------
    // Edge cases the line-by-line review of the new code surfaced.
    // ------------------------------------------------------------------------------------------------
    @Nested
    public class EdgeCasesOfTheNewCode {

        /** Sheet's index-map repair must survive long chains of moves, not just a single one. */
        @Test
        public void chainedSheetMovesKeepBothMapDirectionsCorrect() {
            final java.util.Random random = new java.util.Random(20260901L);

            for (int trial = 0; trial < 40; trial++) {
                final int nRows = 1 + random.nextInt(6);
                final int nCols = 1 + random.nextInt(5);
                final List<String> rowKeys = new ArrayList<>();
                final List<String> columnKeys = new ArrayList<>();

                for (int i = 0; i < nRows; i++) {
                    rowKeys.add("r" + i);
                }

                for (int i = 0; i < nCols; i++) {
                    columnKeys.add("c" + i);
                }

                final Object[][] values = new Object[nRows][nCols];

                for (int i = 0; i < nRows; i++) {
                    for (int j = 0; j < nCols; j++) {
                        values[i][j] = i * 10 + j;
                    }
                }

                final Sheet<String, String, Object> repaired = Sheet.rows(rowKeys, columnKeys, values);
                repaired.get("r0", "c0"); // force the index maps to exist before the first move
                final Sheet<String, String, Object> rebuilt = Sheet.rows(rowKeys, columnKeys, values);

                for (int step = 0; step < 12; step++) {
                    final boolean row = random.nextBoolean();
                    final int from = random.nextInt(row ? nRows : nCols);
                    final int to = random.nextInt(row ? nRows : nCols);
                    final String where = "trial=" + trial + " step=" + step;

                    if (row) {
                        final String key = new ArrayList<>(repaired.rowKeySet()).get(from);
                        repaired.moveRow(key, to);
                        rebuilt.moveRow(key, to);
                    } else {
                        final String key = new ArrayList<>(repaired.columnKeySet()).get(from);
                        repaired.moveColumn(key, to);
                        rebuilt.moveColumn(key, to);
                    }

                    assertEquals(new ArrayList<>(rebuilt.rowKeySet()), new ArrayList<>(repaired.rowKeySet()), where);
                    assertEquals(new ArrayList<>(rebuilt.columnKeySet()), new ArrayList<>(repaired.columnKeySet()), where);

                    // forward direction: key -> index
                    for (final String rowKey : rebuilt.rowKeySet()) {
                        for (final String columnKey : rebuilt.columnKeySet()) {
                            assertEquals(rebuilt.get(rowKey, columnKey), repaired.get(rowKey, columnKey), where);
                        }
                    }

                    // inverse direction: index -> key, which is what the cell streams use
                    assertEquals(rebuilt.rowMajorCells().toList(), repaired.rowMajorCells().toList(), where);
                }
            }
        }

        /** countColumnName probes the key collection, which may be any Collection. */
        @Test
        public void rollupAcceptsASetOfKeyColumnNames() {
            final Dataset d = Dataset.rows(CommonUtil.asList("count", "x"), new Object[][] { { "a", 1 }, { "a", 2 } });
            final Set<String> keys = new LinkedHashSet<>(CommonUtil.asList("count", "x"));

            final List<Dataset> levels = d.rollup(keys).toList();

            assertEquals(3, levels.size());
            assertTrue(levels.get(0).columnNames().contains("count_2"), levels.get(0).columnNames().toString());
        }

        /**
         * The duplicate-result-name check must not probe a collection with {@code null}: a {@code TreeSet}
         * throws {@link NullPointerException} from {@code contains(null)}. Since C-038 (2026-09-02 ledger) a
         * {@code null} result name is rejected before that check, with a message naming the parameter; the
         * {@code TreeSet} keys make sure the rejection still happens without touching the collection.
         */
        @Test
        public void aNullResultColumnNameIsRejectedBeforeTheCollisionCheck() {
            final Dataset d = Dataset.rows(CommonUtil.asList("k1", "k2", "v"), new Object[][] { { "a", "b", 1 } });
            final Set<String> sortedKeys = new java.util.TreeSet<>(CommonUtil.asList("k1", "k2"));

            final IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                    () -> d.groupBy(sortedKeys, CommonUtil.asList("v"), null, Object[].class));

            assertTrue(e.getMessage().contains("aggregateResultColumnName"), e.getMessage());
        }

        /** convertColumns must be atomic whichever of the selected columns fails. */
        @Test
        public void convertColumnsIsAtomicWhicheverColumnFails() {
            final Dataset first = Dataset.rows(CommonUtil.asList("a", "b"), new Object[][] { { "x", "1" }, { "y", "2" } });
            assertThrows(NumberFormatException.class, () -> first.convertColumns(linkedTypes("a", Integer.class, "b", Integer.class)));
            assertEquals(CommonUtil.asList("x", "y"), first.getColumn("a"));
            assertEquals(CommonUtil.asList("1", "2"), first.getColumn("b"));

            final Dataset second = Dataset.rows(CommonUtil.asList("a", "b"), new Object[][] { { "1", "x" }, { "2", "y" } });
            assertThrows(NumberFormatException.class, () -> second.convertColumns(linkedTypes("a", Integer.class, "b", Integer.class)));
            assertEquals(CommonUtil.asList("1", "2"), second.getColumn("a"));
            assertEquals(CommonUtil.asList("x", "y"), second.getColumn("b"));
        }

        @Test
        public void convertColumnOnAnEmptyDatasetIsANoOp() {
            final Dataset d = Dataset.rows(CommonUtil.asList("a"), new Object[0][]);

            d.convertColumn("a", Integer.class);

            // size() was already 0 before the call, so assert the column survived and is still readable
            assertEquals(CommonUtil.asList("a"), d.columnNames());
            assertEquals(0, d.size());
            assertTrue(d.getColumn("a").isEmpty());
            d.addRow(new Object[] { 7 });
            assertEquals(CommonUtil.asList(7), d.getColumn("a"));
        }

        /** The scratch-writer fallback must not leak a pooled writer or disturb later rows. */
        @Test
        public void repeatedFailingSerializationsStayCorrect() {
            final Object[][] rows = new Object[50][2];

            for (int i = 0; i < 50; i++) {
                rows[i][0] = new HalfSerializable();
                rows[i][1] = i;
            }

            final Dataset d = Dataset.rows(CommonUtil.asList("x", "y"), rows);

            final List<?> parsed = N.fromJson(d.toJson(), List.class);

            assertEquals(50, parsed.size());
            assertEquals("Boom!", ((Map<?, ?>) parsed.get(49)).get("x"));
            assertEquals(49, ((Number) ((Map<?, ?>) parsed.get(49)).get("y")).intValue());
        }
    }

    // ------------------------------------------------------------------------------------------------
    // C-018 - append/prepend/merge bumped modCount even when the other Dataset was empty, so a no-op
    //         killed outstanding streams. Only an actual structural change may invalidate them.
    // ------------------------------------------------------------------------------------------------
    @Nested
    public class C018_EmptyAppendPrependMergeAreNotStructuralModifications {

        private Dataset base() {
            return Dataset.rows(CommonUtil.asList("a", "b"), new Object[][] { { 1, "x" }, { 2, "y" } });
        }

        private Dataset emptyLike() {
            return Dataset.rows(CommonUtil.asList("a", "b"), new Object[0][]);
        }

        @Test
        public void appendingAnEmptyDatasetLeavesStreamsValid() {
            final Dataset d = base();
            final Stream<Object[]> live = d.stream(Object[].class);

            d.append(emptyLike());

            assertEquals(2, live.toList().size());
            assertEquals(2, d.size());
        }

        @Test
        public void prependingAnEmptyDatasetLeavesStreamsValid() {
            final Dataset d = base();
            final Stream<Object[]> live = d.stream(Object[].class);

            d.prepend(emptyLike());

            assertEquals(2, live.toList().size());
            assertEquals(2, d.size());
        }

        @Test
        public void mergingAnEmptyDatasetWithTheSameColumnsLeavesStreamsValid() {
            final Dataset d = base();
            final Stream<Object[]> live = d.stream(Object[].class);

            d.merge(emptyLike());

            assertEquals(2, live.toList().size());
            assertEquals(CommonUtil.asList("a", "b"), d.columnNames());
        }

        /** A zero-row other that still contributes a COLUMN is a structural change and must invalidate. */
        @Test
        public void mergingAZeroRowDatasetThatAddsAColumnStillInvalidates() {
            final Dataset d = base();
            final Stream<Object[]> live = d.stream(Object[].class);

            d.merge(Dataset.rows(CommonUtil.asList("a", "b", "c"), new Object[0][]));

            assertEquals(CommonUtil.asList("a", "b", "c"), d.columnNames());
            assertThrows(ConcurrentModificationException.class, live::toList);
        }

        @Test
        public void appendingANonEmptyDatasetStillInvalidates() {
            final Dataset d = base();
            final Stream<Object[]> live = d.stream(Object[].class);

            d.append(Dataset.rows(CommonUtil.asList("a", "b"), new Object[][] { { 3, "z" } }));

            assertEquals(3, d.size());
            assertThrows(ConcurrentModificationException.class, live::toList);
        }

        @Test
        public void prependingANonEmptyDatasetStillInvalidates() {
            final Dataset d = base();
            final Stream<Object[]> live = d.stream(Object[].class);

            d.prepend(Dataset.rows(CommonUtil.asList("a", "b"), new Object[][] { { 0, "w" } }));

            assertEquals(CommonUtil.asList(0, 1, 2), d.getColumn("a"));
            assertThrows(ConcurrentModificationException.class, live::toList);
        }

        @Test
        public void mergingANonEmptyDatasetStillInvalidates() {
            final Dataset d = base();
            final Stream<Object[]> live = d.stream(Object[].class);

            d.merge(Dataset.rows(CommonUtil.asList("a", "b"), new Object[][] { { 3, "z" } }));

            assertEquals(3, d.size());
            assertThrows(ConcurrentModificationException.class, live::toList);
        }

        /** Properties are metadata, outside the structural rule: an empty other must still contribute them. */
        @Test
        public void anEmptyOtherStillMergesItsProperties() {
            final Dataset d = base();
            final Dataset other = emptyLike();
            other.setProperties(CommonUtil.asMap("k", (Object) "v"));

            d.append(other);

            assertEquals("v", d.getProperties().get("k"));
        }

        @Test
        public void appendRejectsNull() {
            assertThrows(IllegalArgumentException.class, () -> base().append(null));
            assertThrows(IllegalArgumentException.class, () -> base().prepend(null));
            assertThrows(IllegalArgumentException.class, () -> base().merge((Dataset) null));
        }

        /** Both sides empty is the boundary case: nothing anywhere, so nothing may be invalidated. */
        @Test
        public void appendingEmptyToEmptyIsAlsoANoOp() {
            final Dataset d = emptyLike();
            final Stream<Object[]> live = d.stream(Object[].class);

            d.append(emptyLike());

            assertEquals(0, live.toList().size());
            assertEquals(0, d.size());
        }

        /** Column names are matched by value, so a non-ASCII name must behave exactly like an ASCII one. */
        @Test
        public void unicodeColumnNamesBehaveTheSame() {
            final Dataset d = Dataset.rows(CommonUtil.asList("中文", "café"), new Object[][] { { 1, "x" } });
            final Stream<Object[]> live = d.stream(Object[].class);

            d.append(Dataset.rows(CommonUtil.asList("中文", "café"), new Object[0][]));

            assertEquals(1, live.toList().size());

            final Stream<Object[]> live2 = d.stream(Object[].class);
            d.append(Dataset.rows(CommonUtil.asList("中文", "café"), new Object[][] { { 2, "y" } }));
            assertThrows(ConcurrentModificationException.class, live2::toList);
        }
    }

}
