package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/**
 * Regression tests for the 2026-09-01 review of {@code Dataset}, {@code RowDataset}, {@code Sheet},
 * {@code SetMultimap} and {@code BiMap}.
 *
 * <p>Each nested class corresponds to one reported finding and is named after it. Every behavioural test here
 * fails on the pre-fix code; the tests that merely pin already-correct behaviour say so.</p>
 */
public class DatasetSheetRegressionTest extends TestBase {

    private static Sheet<String, String, Integer> sheet3x2() {
        return Sheet.rows(CommonUtil.asList("r1", "r2", "r3"), CommonUtil.asList("c1", "c2"), new Integer[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });
    }

    private static Dataset dataset3Rows() {
        return Dataset.rows(CommonUtil.asList("a", "b"), new Object[][] { { 1, "x" }, { 2, "y" }, { 3, "z" } });
    }

    // ------------------------------------------------------------------------------------------------
    // B2' - a Sheet move/swap onto a key's own position must not touch the Sheet or its live Streams.
    // ------------------------------------------------------------------------------------------------
    @Nested
    public class SheetSamePositionMoveIsNoOp {

        @Test
        public void moveRowToOwnPositionLeavesLiveStreamsValid() {
            final Sheet<String, String, Integer> sheet = sheet3x2();
            sheet.get("r1", "c1"); // materialize the index maps

            final Iterator<Sheet.Cell<String, String, Integer>> iter = sheet.rowMajorCells().iterator();

            sheet.moveRow("r1", 0);

            // Before the fix this threw NullPointerException: the no-op move discarded _rowKeyIndexMap.
            assertEquals(Sheet.Cell.of("r1", "c1", 1), iter.next());
            assertEquals(Sheet.Cell.of("r1", "c2", 2), iter.next());
            assertEquals(Sheet.Cell.of("r2", "c1", 3), iter.next());
        }

        @Test
        public void moveColumnToOwnPositionLeavesLiveStreamsValid() {
            final Sheet<String, String, Integer> sheet = sheet3x2();
            sheet.get("r1", "c1");

            final Iterator<Sheet.Cell<String, String, Integer>> iter = sheet.columnMajorCells().iterator();

            sheet.moveColumn("c1", 0);

            assertEquals(Sheet.Cell.of("r1", "c1", 1), iter.next());
            assertEquals(Sheet.Cell.of("r2", "c1", 3), iter.next());
        }

        @Test
        public void selfSwapLeavesLiveStreamsValid() {
            final Sheet<String, String, Integer> sheet = sheet3x2();
            sheet.get("r1", "c1");

            final Iterator<Sheet.Cell<String, String, Integer>> iter = sheet.rowMajorCells().iterator();

            sheet.swapRows("r2", "r2");
            sheet.swapColumns("c2", "c2");

            assertEquals(Sheet.Cell.of("r1", "c1", 1), iter.next());
        }

        @Test
        public void samePositionOperationsStillProduceTheSameContents() {
            final Sheet<String, String, Integer> sheet = sheet3x2();

            sheet.moveRow("r2", 1);
            sheet.moveColumn("c2", 1);
            sheet.swapRows("r3", "r3");
            sheet.swapColumns("c1", "c1");

            assertEquals(CommonUtil.asList("r1", "r2", "r3"), new ArrayList<>(sheet.rowKeySet()));
            assertEquals(CommonUtil.asList("c1", "c2"), new ArrayList<>(sheet.columnKeySet()));
            assertEquals(CommonUtil.asList(3, 4), new ArrayList<>(sheet.rowValues("r2")));
            assertEquals(CommonUtil.asList(1, 3, 5), new ArrayList<>(sheet.columnValues("c1")));
        }

        @Test
        public void realMovesStillWork() {
            final Sheet<String, String, Integer> sheet = sheet3x2();

            sheet.moveRow("r1", 2);
            assertEquals(CommonUtil.asList("r2", "r3", "r1"), new ArrayList<>(sheet.rowKeySet()));
            assertEquals(CommonUtil.asList(1, 2), new ArrayList<>(sheet.rowValues("r1")));

            sheet.moveColumn("c1", 1);
            assertEquals(CommonUtil.asList("c2", "c1"), new ArrayList<>(sheet.columnKeySet()));
            assertEquals(CommonUtil.asList(3, 5, 1), new ArrayList<>(sheet.columnValues("c1")));

            sheet.swapRows("r2", "r1");
            assertEquals(CommonUtil.asList("r1", "r3", "r2"), new ArrayList<>(sheet.rowKeySet()));

            sheet.swapColumns("c1", "c2");
            assertEquals(CommonUtil.asList("c1", "c2"), new ArrayList<>(sheet.columnKeySet()));
        }

        @Test
        public void frozenSheetStillRejectsSamePositionMoves() {
            final Sheet<String, String, Integer> sheet = sheet3x2();
            sheet.freeze();

            assertThrows(IllegalStateException.class, () -> sheet.moveRow("r1", 0));
            assertThrows(IllegalStateException.class, () -> sheet.moveColumn("c1", 0));
            assertThrows(IllegalStateException.class, () -> sheet.swapRows("r1", "r1"));
            assertThrows(IllegalStateException.class, () -> sheet.swapColumns("c1", "c1"));
        }
    }

    // ------------------------------------------------------------------------------------------------
    // B7 - moveRow and moveColumn resolve the key before validating the index.
    // ------------------------------------------------------------------------------------------------
    @Nested
    public class SheetMoveValidationOrder {

        @Test
        public void bothOverloadsReportTheUnknownKeyFirst() {
            final Sheet<String, String, Integer> sheet = sheet3x2();

            // Before the fix moveRow reported IndexOutOfBoundsException here while moveColumn reported
            // IllegalArgumentException for exactly the same shape of call.
            assertThrows(IllegalArgumentException.class, () -> sheet.moveRow("bogus", 99));
            assertThrows(IllegalArgumentException.class, () -> sheet.moveColumn("bogus", 99));
        }

        @Test
        public void aBadIndexWithAKnownKeyIsStillOutOfBounds() {
            final Sheet<String, String, Integer> sheet = sheet3x2();

            assertThrows(IndexOutOfBoundsException.class, () -> sheet.moveRow("r1", -1));
            assertThrows(IndexOutOfBoundsException.class, () -> sheet.moveRow("r1", 3));
            assertThrows(IndexOutOfBoundsException.class, () -> sheet.moveColumn("c1", -1));
            assertThrows(IndexOutOfBoundsException.class, () -> sheet.moveColumn("c1", 2));
        }
    }

    // ------------------------------------------------------------------------------------------------
    // B1 - Sheet.columns(from, to, mapper) must never hand the mapper another column's values.
    // ------------------------------------------------------------------------------------------------
    @Nested
    public class SheetColumnsMapperDoesNotReuseStaleData {

        @Test
        public void columnsMapperReadsEachColumnCorrectly() {
            final Sheet<String, String, Integer> sheet = sheet3x2();

            final List<Pair<String, String>> joined = sheet.columns((i, arr) -> arr.join(",")).toList();

            assertEquals(2, joined.size());
            assertEquals(Pair.of("c1", "1,3,5"), joined.get(0));
            assertEquals(Pair.of("c2", "2,4,6"), joined.get(1));
        }

        @Test
        public void rowAddedMidStreamDoesNotYieldThePreviousColumnsValues() {
            final Sheet<String, String, Integer> sheet = sheet3x2();
            final Iterator<Pair<String, String>> iter = sheet.columns(0, 2, (i, arr) -> arr.join(",")).iterator();

            assertEquals(Pair.of("c1", "1,3,5"), iter.next());

            sheet.addRow("r4", CommonUtil.asList(7, 8));

            // Consuming a Stream after a structural change is undefined, but it must not silently report
            // c1's values under c2's key, which is what discarding List.toArray(T[])'s result used to do.
            final Pair<String, String> second = iter.next();
            assertEquals("c2", second.left());
            assertEquals("2,4,6", second.right());
        }

        @Test
        public void rowRemovedMidStreamDoesNotLeaveAStaleTail() {
            final Sheet<String, String, Integer> sheet = sheet3x2();
            final Iterator<Pair<String, String>> iter = sheet.columns(0, 2, (i, arr) -> arr.join(",")).iterator();

            assertEquals(Pair.of("c1", "1,3,5"), iter.next());

            sheet.removeRow("r3");

            // Used to yield "2,4,null": toArray wrote only two cells and nulled the sentinel slot.
            assertThrows(IndexOutOfBoundsException.class, iter::next);
        }

        @Test
        public void emptySheetAndSingleColumnStillWork() {
            final Sheet<String, String, Integer> noRows = new Sheet<>(CommonUtil.asList(), CommonUtil.asList("c1"));
            assertEquals(CommonUtil.asList(Pair.of("c1", "")), noRows.columns((i, arr) -> arr.join(",")).toList());

            final Sheet<String, String, Integer> noColumns = new Sheet<>(CommonUtil.asList("r1"), CommonUtil.asList());
            assertTrue(noColumns.columns((i, arr) -> arr.join(",")).toList().isEmpty());

            final Sheet<String, String, Integer> uninitialized = new Sheet<>(CommonUtil.asList("r1", "r2"), CommonUtil.asList("c1"));
            assertEquals(CommonUtil.asList(Pair.of("c1", "null,null")), uninitialized.columns((i, arr) -> arr.join(",")).toList());
        }
    }

    // ------------------------------------------------------------------------------------------------
    // D4 / B8 - Sheet.putAll writes through the unchecked accessors and validates its source.
    // ------------------------------------------------------------------------------------------------
    @Nested
    public class SheetPutAll {

        @Test
        public void putAllCopiesByKeyRegardlessOfAxisOrder() {
            final Sheet<String, String, Integer> target = Sheet.rows(CommonUtil.asList("r1", "r2"), CommonUtil.asList("c1", "c2", "c3"),
                    new Integer[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            // Source axes deliberately in a different order from the target's.
            final Sheet<String, String, Integer> source = Sheet.rows(CommonUtil.asList("r2", "r1"), CommonUtil.asList("c3", "c1"),
                    new Integer[][] { { 60, 40 }, { 30, 10 } });

            target.putAll(source);

            assertEquals(CommonUtil.asList(10, 2, 30), new ArrayList<>(target.rowValues("r1")));
            assertEquals(CommonUtil.asList(40, 5, 60), new ArrayList<>(target.rowValues("r2")));
        }

        @Test
        public void putAllWithMergeFunctionCombinesByKey() {
            final Sheet<String, String, Integer> target = Sheet.rows(CommonUtil.asList("r1", "r2"), CommonUtil.asList("c1", "c2"),
                    new Integer[][] { { 1, 2 }, { 3, 4 } });
            final Sheet<String, String, Integer> source = Sheet.rows(CommonUtil.asList("r2", "r1"), CommonUtil.asList("c2", "c1"),
                    new Integer[][] { { 80, 70 }, { 60, 50 } });

            target.putAll(source, (a, b) -> (a == null ? 0 : a) + (b == null ? 0 : b));

            assertEquals(CommonUtil.asList(51, 62), new ArrayList<>(target.rowValues("r1")));
            assertEquals(CommonUtil.asList(73, 84), new ArrayList<>(target.rowValues("r2")));
        }

        @Test
        public void putAllOntoAnUninitializedSheetAllocatesStorage() {
            final Sheet<String, String, Integer> target = new Sheet<>(CommonUtil.asList("r1", "r2"), CommonUtil.asList("c1", "c2"));
            final Sheet<String, String, Integer> source = Sheet.rows(CommonUtil.asList("r1"), CommonUtil.asList("c2"), new Integer[][] { { 9 } });

            target.putAll(source);

            assertEquals(CommonUtil.asList(null, 9), new ArrayList<>(target.rowValues("r1")));
            assertEquals(Arrays.asList(null, null), new ArrayList<>(target.rowValues("r2")));
        }

        @Test
        public void putAllWithAnEmptySourceIsANoOp() {
            final Sheet<String, String, Integer> target = sheet3x2();
            final Sheet<String, String, Integer> emptyRows = new Sheet<>(CommonUtil.asList(), CommonUtil.asList("c1"));

            target.putAll(emptyRows);

            assertEquals(CommonUtil.asList(1, 2), new ArrayList<>(target.rowValues("r1")));
        }

        @Test
        public void putAllRejectsNullAndUnknownKeys() {
            final Sheet<String, String, Integer> target = sheet3x2();

            assertThrows(IllegalArgumentException.class, () -> target.putAll(null));
            assertThrows(IllegalArgumentException.class, () -> target.putAll(null, (a, b) -> a));
            assertThrows(IllegalArgumentException.class, () -> target.putAll(sheet3x2(), null));
            assertThrows(IllegalArgumentException.class,
                    () -> target.putAll(Sheet.rows(CommonUtil.asList("rX"), CommonUtil.asList("c1"), new Integer[][] { { 1 } })));
            assertThrows(IllegalArgumentException.class,
                    () -> target.putAll(Sheet.rows(CommonUtil.asList("r1"), CommonUtil.asList("cX"), new Integer[][] { { 1 } })));
        }

        @Test
        public void putAllOnAFrozenSheetThrows() {
            final Sheet<String, String, Integer> target = sheet3x2();
            target.freeze();

            assertThrows(IllegalStateException.class, () -> target.putAll(sheet3x2()));
        }

        @Test
        public void mergeRejectsANullOtherSheet() {
            assertThrows(IllegalArgumentException.class, () -> sheet3x2().merge(null, (a, b) -> a));
        }
    }

    // ------------------------------------------------------------------------------------------------
    // O1 / D9 - Sheet.hashCode's fused pass and the index-map sizing must not change any result.
    // ------------------------------------------------------------------------------------------------
    @Nested
    public class SheetHashCodeAndIndexMaps {

        @Test
        public void uninitializedAndAllNullSheetsAgree() {
            final Sheet<String, String, Integer> uninitialized = new Sheet<>(CommonUtil.asList("r1", "r2"), CommonUtil.asList("c1", "c2"));
            final Sheet<String, String, Integer> allNull = new Sheet<>(CommonUtil.asList("r1", "r2"), CommonUtil.asList("c1", "c2"));
            allNull.set("r1", "c1", 1);
            allNull.set("r1", "c1", null);

            assertEquals(uninitialized, allNull);
            assertEquals(uninitialized.hashCode(), allNull.hashCode());
        }

        @Test
        public void equalSheetsHashEqualAndDifferentOnesDiffer() {
            final Sheet<String, String, Integer> a = sheet3x2();
            final Sheet<String, String, Integer> b = sheet3x2();
            assertEquals(a.hashCode(), b.hashCode());

            b.set("r1", "c1", 99);
            assertFalse(a.equals(b));
            assertFalse(a.hashCode() == b.hashCode());
        }

        @Test
        public void largeSheetsResolveEveryKeyCorrectly() {
            // Exercises the resized index maps across the sizes that used to rehash mid-fill.
            for (final int n : new int[] { 1, 7, 12, 13, 16, 17, 25, 32, 33, 64 }) {
                final List<String> rowKeys = new ArrayList<>(n);
                final List<String> columnKeys = new ArrayList<>(n);

                for (int i = 0; i < n; i++) {
                    rowKeys.add("r" + i);
                    columnKeys.add("c" + i);
                }

                final Sheet<String, String, Integer> sheet = new Sheet<>(rowKeys, columnKeys);

                for (int i = 0; i < n; i++) {
                    sheet.set("r" + i, "c" + i, i);
                }

                for (int i = 0; i < n; i++) {
                    assertEquals(Integer.valueOf(i), sheet.get("r" + i, "c" + i), "n=" + n + " i=" + i);
                }
            }
        }
    }

    // ------------------------------------------------------------------------------------------------
    // D8 - setColumn writes straight through; behaviour must be unchanged.
    // ------------------------------------------------------------------------------------------------
    @Nested
    public class SheetSetColumn {

        @Test
        public void setColumnReplacesValuesInPlaceKeepingViewsLive() {
            final Sheet<String, String, Integer> sheet = sheet3x2();
            final ImmutableList<Integer> view = sheet.columnValues("c1");

            sheet.setColumn("c1", CommonUtil.asList(7, 8, 9));
            assertEquals(CommonUtil.asList(7, 8, 9), new ArrayList<>(view));

            sheet.setColumn("c1", CommonUtil.emptyList());
            assertEquals(Arrays.asList(null, null, null), new ArrayList<>(view));
        }

        @Test
        public void setColumnRejectsAMismatchedSize() {
            final Sheet<String, String, Integer> sheet = sheet3x2();
            assertThrows(IllegalArgumentException.class, () -> sheet.setColumn("c1", CommonUtil.asList(1, 2)));
        }
    }

    // ------------------------------------------------------------------------------------------------
    // B4 - Dataset mutators that change nothing must not invalidate live lazy sources.
    // ------------------------------------------------------------------------------------------------
    @Nested
    public class DatasetNoOpMutatorsDoNotBumpModCount {

        @Test
        public void emptyRemoveRowsAtKeepsStreamsValid() {
            final Dataset ds = dataset3Rows();
            final com.landawn.abacus.util.stream.Stream<Object> stream = ds.stream("a");

            ds.removeRowsAt();

            assertEquals(CommonUtil.asList(1, 2, 3), stream.toList());
            assertEquals(3, ds.size());
        }

        @Test
        public void emptyRemoveRowsRangeKeepsStreamsValid() {
            final Dataset ds = dataset3Rows();
            final com.landawn.abacus.util.stream.Stream<Object> stream = ds.stream("a");

            ds.removeRows(1, 1);

            assertEquals(CommonUtil.asList(1, 2, 3), stream.toList());
            assertEquals(3, ds.size());
        }

        @Test
        public void alreadySortedSortByKeepsStreamsValid() {
            final Dataset ds = dataset3Rows();
            final com.landawn.abacus.util.stream.Stream<Object> stream = ds.stream("a");

            ds.sortBy("a");

            assertEquals(CommonUtil.asList(1, 2, 3), stream.toList());
        }

        @Test
        public void identityRenameColumnsKeepsStreamsValid() {
            final Dataset ds = dataset3Rows();
            final com.landawn.abacus.util.stream.Stream<Object> stream = ds.stream("a");

            ds.renameColumns(CommonUtil.asMap("a", "a", "b", "b"));

            assertEquals(CommonUtil.asList(1, 2, 3), stream.toList());
        }

        @Test
        public void aRealRemovalStillInvalidatesStreams() {
            final Dataset ds = dataset3Rows();
            final com.landawn.abacus.util.stream.Stream<Object> byIndex = ds.stream("a");
            ds.removeRowsAt(0);
            assertThrows(ConcurrentModificationException.class, byIndex::toList);

            final Dataset ds2 = dataset3Rows();
            final com.landawn.abacus.util.stream.Stream<Object> byRange = ds2.stream("a");
            ds2.removeRows(0, 1);
            assertThrows(ConcurrentModificationException.class, byRange::toList);

            final Dataset ds3 = Dataset.rows(CommonUtil.asList("a"), new Object[][] { { 3 }, { 1 }, { 2 } });
            final com.landawn.abacus.util.stream.Stream<Object> bySort = ds3.stream("a");
            ds3.sortBy("a");
            assertThrows(ConcurrentModificationException.class, bySort::toList);
        }
    }

    // ------------------------------------------------------------------------------------------------
    // O2 - removeRowsAt's single-pass compaction must match the old semantics exactly.
    // ------------------------------------------------------------------------------------------------
    @Nested
    public class DatasetRemoveRowsAt {

        @Test
        public void removesTheRequestedRowsOnly() {
            final Dataset ds = Dataset.rows(CommonUtil.asList("a", "b"), new Object[][] { { 0, "p" }, { 1, "q" }, { 2, "r" }, { 3, "s" }, { 4, "t" } });

            ds.removeRowsAt(1, 3);

            assertEquals(CommonUtil.asList(0, 2, 4), ds.getColumn("a"));
            assertEquals(CommonUtil.asList("p", "r", "t"), ds.getColumn("b"));
        }

        @Test
        public void handlesUnsortedAndDuplicateIndexes() {
            final Dataset ds = Dataset.rows(CommonUtil.asList("a"), new Object[][] { { 0 }, { 1 }, { 2 }, { 3 }, { 4 } });

            ds.removeRowsAt(3, 0, 3);

            assertEquals(CommonUtil.asList(1, 2, 4), ds.getColumn("a"));
        }

        @Test
        public void doesNotMutateTheCallersIndexArray() {
            final Dataset ds = Dataset.rows(CommonUtil.asList("a"), new Object[][] { { 0 }, { 1 }, { 2 } });
            final int[] indexes = { 2, 0 };

            ds.removeRowsAt(indexes);

            assertArrayEquals(new int[] { 2, 0 }, indexes);
            assertEquals(CommonUtil.asList(1), ds.getColumn("a"));
        }

        @Test
        public void removingEveryRowAndTheFirstAndLastRowsWork() {
            final Dataset all = Dataset.rows(CommonUtil.asList("a"), new Object[][] { { 0 }, { 1 }, { 2 } });
            all.removeRowsAt(0, 1, 2);
            assertEquals(0, all.size());
            assertEquals(1, all.columnCount());

            final Dataset ends = Dataset.rows(CommonUtil.asList("a"), new Object[][] { { 0 }, { 1 }, { 2 } });
            ends.removeRowsAt(0, 2);
            assertEquals(CommonUtil.asList(1), ends.getColumn("a"));
        }

        @Test
        public void rejectsOutOfBoundsIndexesBeforeMutating() {
            final Dataset ds = Dataset.rows(CommonUtil.asList("a"), new Object[][] { { 0 }, { 1 } });

            assertThrows(IndexOutOfBoundsException.class, () -> ds.removeRowsAt(0, 5));
            assertEquals(CommonUtil.asList(0, 1), ds.getColumn("a"));
        }
    }

    // ------------------------------------------------------------------------------------------------
    // B5 - property maps are copied with newTargetMap, so a SortedMap keeps its comparator.
    // ------------------------------------------------------------------------------------------------
    @Nested
    public class DatasetPropertyMapType {

        @Test
        public void aSortedPropertiesMapStaysSorted() {
            final Map<String, Object> props = new TreeMap<>(Comparator.reverseOrder());
            props.put("a", 1);
            props.put("c", 3);

            final Dataset ds = new RowDataset(new ArrayList<>(CommonUtil.asList("x")),
                    new ArrayList<>(CommonUtil.asList(new ArrayList<>(CommonUtil.asList((Object) 1)))), props);

            final Dataset other = new RowDataset(new ArrayList<>(CommonUtil.asList("x")),
                    new ArrayList<>(CommonUtil.asList(new ArrayList<>(CommonUtil.asList((Object) 2)))), CommonUtil.asMap("b", 2));
            ds.append(other);

            // Before the fix newOrderingMap produced a LinkedHashMap, so "b" landed at the end instead of
            // being sorted into place by the source map's comparator.
            assertEquals(CommonUtil.asList("c", "b", "a"), new ArrayList<>(ds.getProperties().keySet()));
        }

        @Test
        public void anUnorderedPropertiesMapStillRoundTrips() {
            final Map<String, Object> props = new HashMap<>();
            props.put("src", "x");

            final Dataset ds = new RowDataset(new ArrayList<>(CommonUtil.asList("x")),
                    new ArrayList<>(CommonUtil.asList(new ArrayList<>(CommonUtil.asList((Object) 1)))), props);

            assertEquals(props, ds.getProperties());
            props.put("mutatedAfterwards", true);
            assertFalse(ds.getProperties().containsKey("mutatedAfterwards"));
        }
    }

    // ------------------------------------------------------------------------------------------------
    // D2 - the documented properties-propagation rule.
    // ------------------------------------------------------------------------------------------------
    @Nested
    public class DatasetPropertiesPropagation {

        private Dataset withProperties() {
            return new RowDataset(new ArrayList<>(CommonUtil.asList("a", "b")), new ArrayList<>(CommonUtil
                    .asList(new ArrayList<>(CommonUtil.asList((Object) 1, (Object) 2)), new ArrayList<>(CommonUtil.asList((Object) "p", (Object) "q")))),
                    CommonUtil.asMap("src", "x"));
        }

        @Test
        public void rowSelectionsAndColumnSubsetsKeepProperties() {
            final Dataset ds = withProperties();
            final Map<String, Object> expected = CommonUtil.asMap("src", "x");

            assertEquals(expected, ds.copy().getProperties());
            assertEquals(expected, ds.copy(CommonUtil.asList("a")).getProperties());
            assertEquals(expected, ds.slice(0, 1).getProperties());
            assertEquals(expected, ds.slice(CommonUtil.asList("a")).getProperties());
            assertEquals(expected, ds.filter(r -> true).getProperties());
            assertEquals(expected, ds.distinct().getProperties());
            assertEquals(expected, ds.distinctBy("a").getProperties());
            assertEquals(expected, ds.topBy("a", 1).getProperties());
            assertEquals(expected, ds.splitToList(1).get(0).getProperties());
            assertEquals(expected, ds.split(1).first().get().getProperties());
            assertEquals(expected, ds.paginate(1).firstPage().get().getProperties());
        }

        @Test
        public void reshapingAndBinaryOperationsDropProperties() {
            final Dataset ds = withProperties();

            assertTrue(ds.groupBy(CommonUtil.asList("a")).getProperties().isEmpty());
            assertTrue(ds.mapColumn("a", "a2", CommonUtil.asList("b"), v -> v).getProperties().isEmpty());
            assertTrue(ds.union(withProperties()).getProperties().isEmpty());
            assertTrue(ds.unionAll(withProperties()).getProperties().isEmpty());
            assertTrue(ds.intersect(withProperties()).getProperties().isEmpty());
            assertTrue(ds.intersectAll(withProperties()).getProperties().isEmpty());
            assertTrue(ds.except(withProperties()).getProperties().isEmpty());
            assertTrue(ds.exceptAll(withProperties()).getProperties().isEmpty());
            assertTrue(ds.cartesianProduct(Dataset.rows(CommonUtil.asList("z"), new Object[][] { { 1 } })).getProperties().isEmpty());
            assertTrue(ds.innerJoin(Dataset.rows(CommonUtil.asList("k"), new Object[][] { { 1 } }), CommonUtil.asMap("a", "k")).getProperties().isEmpty());
        }

        @Test
        public void unionAllStillProducesTheRightRows() {
            final Dataset a = Dataset.rows(CommonUtil.asList("a"), new Object[][] { { 1 }, { 2 } });
            final Dataset b = Dataset.rows(CommonUtil.asList("a"), new Object[][] { { 2 }, { 3 } });

            assertEquals(CommonUtil.asList(1, 2, 2, 3), a.unionAll(b).getColumn("a"));
            assertEquals(CommonUtil.asList(1, 2), a.getColumn("a"));
        }

        @Test
        public void appendMergesPropertiesIntoAPreviouslyReturnedView() {
            final Dataset a = withProperties();
            final Map<String, Object> view = a.getProperties();

            final Dataset b = new RowDataset(new ArrayList<>(CommonUtil.asList("a", "b")),
                    new ArrayList<>(CommonUtil.asList(new ArrayList<>(CommonUtil.asList((Object) 3)), new ArrayList<>(CommonUtil.asList((Object) "r")))),
                    CommonUtil.asMap("extra", 1));
            a.append(b);

            // Documented: getProperties() is a view, so an in-place merge is visible through it.
            assertEquals(1, view.get("extra"));
        }

        @Test
        public void setPropertiesDetachesAPreviouslyReturnedView() {
            final Dataset a = withProperties();
            final Map<String, Object> view = a.getProperties();

            a.setProperties(CommonUtil.asMap("fresh", 1));

            assertEquals("x", view.get("src"));
            assertFalse(view.containsKey("fresh"));
            assertEquals(CommonUtil.asMap("fresh", 1), a.getProperties());
        }
    }

    // ------------------------------------------------------------------------------------------------
    // B3 - the first column of a zero-column Dataset establishes the row count.
    // ------------------------------------------------------------------------------------------------
    @Nested
    public class DatasetAddColumnToEmptyDataset {

        @Test
        public void addColumnEstablishesTheRowCount() {
            final Dataset ds = CommonUtil.newEmptyDataset();

            ds.addColumn("a", CommonUtil.asList(1, 2, 3));

            assertEquals(3, ds.size());
            assertEquals(CommonUtil.asList(1, 2, 3), ds.getColumn("a"));

            ds.addColumn("b", CommonUtil.asList("x", "y", "z"));
            assertEquals(CommonUtil.asList("x", "y", "z"), ds.getColumn("b"));

            // ...and the size check is back in force once a column exists.
            assertThrows(IllegalArgumentException.class, () -> ds.addColumn("c", CommonUtil.asList(1)));
        }

        @Test
        public void addColumnsEstablishesTheRowCountAndStillCrossChecks() {
            final Dataset ds = CommonUtil.newEmptyDataset();

            ds.addColumns(CommonUtil.asList("a", "b"), CommonUtil.asList(CommonUtil.asList(1, 2), CommonUtil.asList("x", "y")));

            assertEquals(2, ds.size());
            assertEquals(CommonUtil.asList(1, 2), ds.getColumn("a"));
            assertEquals(CommonUtil.asList("x", "y"), ds.getColumn("b"));

            final Dataset mismatched = CommonUtil.newEmptyDataset();
            assertThrows(IllegalArgumentException.class,
                    () -> mismatched.addColumns(CommonUtil.asList("a", "b"), CommonUtil.asList(CommonUtil.asList(1, 2), CommonUtil.asList("x", "y", "z"))));
        }

        @Test
        public void anEmptyColumnOnAnEmptyDatasetIsStillEmpty() {
            final Dataset ds = CommonUtil.newEmptyDataset();
            ds.addColumn("a", CommonUtil.emptyList());

            assertEquals(0, ds.size());
            assertEquals(1, ds.columnCount());
        }

        @Test
        public void rowsCanThenBeAdded() {
            final Dataset ds = CommonUtil.newEmptyDataset();
            ds.addColumn("a", CommonUtil.asList(1, 2));
            ds.addRow(new Object[] { 3 });

            assertEquals(CommonUtil.asList(1, 2, 3), ds.getColumn("a"));
        }
    }

    // ------------------------------------------------------------------------------------------------
    // D3 - renameColumns validates the resulting name list, so permutations are accepted.
    // ------------------------------------------------------------------------------------------------
    @Nested
    public class DatasetRenameColumnsPermutation {

        @Test
        public void namesCanBeSwapped() {
            final Dataset ds = Dataset.rows(CommonUtil.asList("a", "b"), new Object[][] { { 1, "x" } });

            ds.renameColumns(CommonUtil.asMap("a", "b", "b", "a"));

            assertEquals(CommonUtil.asList("b", "a"), ds.columnNames());
            // The DATA does not move; only the names do.
            assertEquals(CommonUtil.asList(1), ds.getColumn("b"));
            assertEquals(CommonUtil.asList("x"), ds.getColumn("a"));
        }

        @Test
        public void namesCanBeRotatedAndShifted() {
            final Dataset rotated = Dataset.rows(CommonUtil.asList("a", "b", "c"), new Object[][] { { 1, 2, 3 } });
            final Map<String, String> rotation = new LinkedHashMap<>();
            rotation.put("a", "b");
            rotation.put("b", "c");
            rotation.put("c", "a");
            rotated.renameColumns(rotation);
            assertEquals(CommonUtil.asList("b", "c", "a"), rotated.columnNames());

            final Dataset shifted = Dataset.rows(CommonUtil.asList("a", "b"), new Object[][] { { 1, 2 } });
            shifted.renameColumns(CommonUtil.asMap("a", "x", "b", "a"));
            assertEquals(CommonUtil.asList("x", "a"), shifted.columnNames());
        }

        @Test
        public void lookupsFollowTheNewNames() {
            final Dataset ds = Dataset.rows(CommonUtil.asList("a", "b"), new Object[][] { { 1, "x" } });
            ds.getColumnIndex("a"); // warm the name -> index cache before the rename

            ds.renameColumns(CommonUtil.asMap("a", "b", "b", "a"));

            assertEquals(0, ds.getColumnIndex("b"));
            assertEquals(1, ds.getColumnIndex("a"));
            assertTrue(ds.containsColumn("a"));
            assertTrue(ds.containsColumn("b"));
        }

        @Test
        public void genuineCollisionsAreStillRejectedWithoutMutating() {
            final Dataset ds = Dataset.rows(CommonUtil.asList("a", "b", "c"), new Object[][] { { 1, 2, 3 } });

            // "c" is not being renamed, so renaming "a" onto it really is a duplicate.
            assertThrows(IllegalArgumentException.class, () -> ds.renameColumns(CommonUtil.asMap("a", "c")));
            assertEquals(CommonUtil.asList("a", "b", "c"), ds.columnNames());

            // Two entries mapping to the same new name.
            final Map<String, String> dup = new LinkedHashMap<>();
            dup.put("a", "z");
            dup.put("b", "z");
            assertThrows(IllegalArgumentException.class, () -> ds.renameColumns(dup));
            assertEquals(CommonUtil.asList("a", "b", "c"), ds.columnNames());

            // An unknown source column, and an empty new name.
            assertThrows(IllegalArgumentException.class, () -> ds.renameColumns(CommonUtil.asMap("nope", "z")));
            assertThrows(IllegalArgumentException.class, () -> ds.renameColumns(CommonUtil.asMap("a", "")));
            assertEquals(CommonUtil.asList("a", "b", "c"), ds.columnNames());
        }

        @Test
        public void collectionAndFunctionOverloadCanSwapToo() {
            final Dataset ds = Dataset.rows(CommonUtil.asList("a", "b"), new Object[][] { { 1, 2 } });

            ds.renameColumns(CommonUtil.asList("a", "b"), name -> "a".equals(name) ? "b" : "a");

            assertEquals(CommonUtil.asList("b", "a"), ds.columnNames());
        }

        @Test
        public void aFrozenDatasetStillRejectsRenames() {
            final Dataset ds = Dataset.rows(CommonUtil.asList("a", "b"), new Object[][] { { 1, 2 } });
            ds.freeze();

            assertThrows(IllegalStateException.class, () -> ds.renameColumns(CommonUtil.asMap("a", "b", "b", "a")));
        }
    }

    // ------------------------------------------------------------------------------------------------
    // D7 - hoisting the right-hand column views must not change any join result.
    // ------------------------------------------------------------------------------------------------
    @Nested
    public class DatasetJoinResults {

        private final Dataset left = Dataset.rows(N.asList("id", "name"), new Object[][] { { 1, "a" }, { 2, "b" }, { 3, "c" } });
        private final Dataset right = Dataset.rows(N.asList("id", "tag"), new Object[][] { { 1, "t1" }, { 1, "t2" }, { 2, "t3" } });

        @Test
        public void innerJoinOnOneColumn() {
            final Dataset joined = left.innerJoin(right, "id", "id");

            assertEquals(CommonUtil.asList("id", "name", "id_2", "tag"), joined.columnNames());
            assertEquals(CommonUtil.asList(1, 1, 2), joined.getColumn("id"));
            assertEquals(CommonUtil.asList("t1", "t2", "t3"), joined.getColumn("tag"));
        }

        @Test
        public void leftJoinOnOneColumnPadsUnmatchedRows() {
            final Dataset joined = left.leftJoin(right, "id", "id");

            assertEquals(CommonUtil.asList(1, 1, 2, 3), joined.getColumn("id"));
            assertEquals(Arrays.asList("t1", "t2", "t3", null), joined.getColumn("tag"));
        }

        @Test
        public void joinOnTwoColumns() {
            final Dataset l = Dataset.rows(CommonUtil.asList("k1", "k2", "v"), new Object[][] { { 1, "a", "L1" }, { 2, "b", "L2" } });
            final Dataset r = Dataset.rows(CommonUtil.asList("k1", "k2", "w"), new Object[][] { { 1, "a", "R1" }, { 1, "a", "R2" }, { 9, "z", "R3" } });

            final Dataset joined = l.innerJoin(r, CommonUtil.asMap("k1", "k1", "k2", "k2"));

            assertEquals(CommonUtil.asList("L1", "L1"), joined.getColumn("v"));
            assertEquals(CommonUtil.asList("R1", "R2"), joined.getColumn("w"));

            final Dataset leftJoined = l.leftJoin(r, CommonUtil.asMap("k1", "k1", "k2", "k2"));
            assertEquals(CommonUtil.asList("L1", "L1", "L2"), leftJoined.getColumn("v"));
            assertEquals(Arrays.asList("R1", "R2", null), leftJoined.getColumn("w"));
        }
    }

    // ------------------------------------------------------------------------------------------------
    // B6 - BiMap's backing-map equality requirement (documented; these pin the equals-consistent case).
    // ------------------------------------------------------------------------------------------------
    @Nested
    public class BiMapContracts {

        @Test
        public void valueSideViewsAgreeWithEqualsConsistentBackingMaps() {
            final BiMap<String, String> map = new BiMap<>(LinkedHashMap::new, LinkedHashMap::new);
            map.put("k", "ABC");

            assertTrue(map.containsValue("ABC"));
            assertFalse(map.containsValue("abc"));
            assertTrue(map.values().contains("ABC"));
            assertFalse(map.values().contains("abc"));
            assertTrue(map.containsEntry("k", "ABC"));
            assertFalse(map.containsEntry("k", "abc"));
            assertTrue(map.entrySet().contains(CommonUtil.newEntry("k", "ABC")));
        }

        @Test
        public void factoriesAndBuilderStillBehave() {
            final BiMap<String, Integer> map = BiMap.of("a", 1, "b", 2, "c", 3, "d", 4, "e", 5, "f", 6, "g", 7, "h", 8, "i", 9, "j", 10);

            assertEquals(10, map.size());
            assertEquals(Integer.valueOf(7), map.get("g"));
            assertEquals("j", map.getByValue(10));
            assertThrows(IllegalArgumentException.class, () -> map.put("k", 1));

            assertEquals(1, BiMap.of("a", 1).size());
            assertEquals(2, BiMap.of("a", 1, "b", 2).size());
        }

        @Test
        public void inverseAndCopyStillShareOrKeepTheirBackingTypes() {
            final BiMap<String, Integer> map = new BiMap<>(LinkedHashMap::new, TreeMap::new);
            map.put("b", 2);
            map.put("a", 1);

            assertSame(map, map.inverse().inverse());
            assertEquals(CommonUtil.asList("b", "a"), new ArrayList<>(map.keySet()));
            assertEquals(CommonUtil.asList(1, 2), new ArrayList<>(map.inverse().keySet()));

            final BiMap<String, Integer> copy = map.copy();
            assertNotSame(map, copy);
            assertEquals(CommonUtil.asList("b", "a"), new ArrayList<>(copy.keySet()));
            assertEquals(map, copy);
        }
    }

    // ------------------------------------------------------------------------------------------------
    // O3 - SetMultimap.wrap names the offending key instead of rendering the whole map.
    // ------------------------------------------------------------------------------------------------
    @Nested
    public class SetMultimapWrapDiagnostics {

        @Test
        public void wrapNamesTheOffendingKeyAndNotTheWholeMap() {
            final Map<String, java.util.Set<Integer>> map = new LinkedHashMap<>();

            for (int i = 0; i < 200; i++) {
                map.put("k" + i, CommonUtil.asSet(i));
            }

            map.put("bad", new java.util.HashSet<>());

            final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> SetMultimap.wrap(map));

            assertTrue(ex.getMessage().contains("bad"), ex.getMessage());
            assertFalse(ex.getMessage().contains("k100"), ex.getMessage());
        }

        @Test
        public void wrapWithASupplierRejectsTheSameInputAndStillWraps() {
            final Map<String, java.util.TreeSet<Integer>> bad = new LinkedHashMap<>();
            bad.put("x", new java.util.TreeSet<>());
            assertThrows(IllegalArgumentException.class, () -> SetMultimap.wrap(bad, java.util.TreeSet::new));

            final Map<String, java.util.TreeSet<Integer>> good = new LinkedHashMap<>();
            good.put("x", new java.util.TreeSet<>(CommonUtil.asList(3, 1)));
            final SetMultimap<String, Integer> wrapped = SetMultimap.wrap(good, java.util.TreeSet::new);
            assertEquals(CommonUtil.asList(1, 3), new ArrayList<>(wrapped.get("x")));

            wrapped.put("y", 5);
            assertTrue(good.containsKey("y"));
        }

        @Test
        public void wrapStillRejectsANullMapAndAcceptsAValidOne() {
            assertThrows(IllegalArgumentException.class, () -> SetMultimap.wrap((Map<String, java.util.Set<Integer>>) null));

            final Map<String, java.util.Set<Integer>> map = new LinkedHashMap<>();
            map.put("a", CommonUtil.asSet(1, 2));
            final SetMultimap<String, Integer> wrapped = SetMultimap.wrap(map);
            assertEquals(CommonUtil.asSet(1, 2), wrapped.get("a"));
        }

        @Test
        public void sizedConstructorViaFactoriesStillWorks() {
            final SetMultimap<String, Integer> mm = SetMultimap.of("a", 1, "b", 2, "c", 3, "d", 4, "e", 5, "f", 6, "g", 7);
            assertEquals(7, mm.keyCount());
            assertEquals(CommonUtil.asSet(7), mm.get("g"));
        }
    }

    // ------------------------------------------------------------------------------------------------
    // D1 - the documented (deliberate) shallow-vs-deep equality split between Sheet and Dataset.
    // ------------------------------------------------------------------------------------------------
    @Nested
    public class SheetAndDatasetEqualityAreDocumentedToDiffer {

        @Test
        public void sheetIsShallowAndDatasetIsDeepForArrayCells() {
            final Sheet<String, String, int[]> s1 = new Sheet<>(CommonUtil.asList("r"), CommonUtil.asList("c"));
            final Sheet<String, String, int[]> s2 = new Sheet<>(CommonUtil.asList("r"), CommonUtil.asList("c"));
            s1.set("r", "c", new int[] { 1, 2 });
            s2.set("r", "c", new int[] { 1, 2 });

            assertFalse(s1.equals(s2), "Sheet documents plain equals on cells");
            assertFalse(s1.containsValue(new int[] { 1, 2 }), "containsValue matches Sheet.equals");

            assertTrue(s1.toDataset().equals(s2.toDataset()), "Dataset documents deep cell equality");
        }

        @Test
        public void identicalArrayInstancesAreEqualOnBothSides() {
            final int[] shared = { 1, 2 };
            final Sheet<String, String, int[]> s1 = new Sheet<>(CommonUtil.asList("r"), CommonUtil.asList("c"));
            final Sheet<String, String, int[]> s2 = new Sheet<>(CommonUtil.asList("r"), CommonUtil.asList("c"));
            s1.set("r", "c", shared);
            s2.set("r", "c", shared);

            assertTrue(s1.equals(s2));
            assertEquals(s1.hashCode(), s2.hashCode());
            assertTrue(s1.containsValue(shared));
        }
    }

    // ------------------------------------------------------------------------------------------------
    // A broad structural cross-check: the Sheet mutators must keep the key sets, the index maps and the
    // cell grid mutually consistent, whatever sequence of no-op and real operations is applied.
    // ------------------------------------------------------------------------------------------------
    @Nested
    public class SheetMutatorInvariants {

        @Test
        public void randomMutationSequencesKeepEveryKeyResolvable() {
            final java.util.Random random = new java.util.Random(20260901L);

            for (int trial = 0; trial < 100; trial++) {
                final Sheet<String, String, String> sheet = new Sheet<>(CommonUtil.asList("r0", "r1", "r2", "r3"), CommonUtil.asList("c0", "c1", "c2"));

                for (final String r : sheet.rowKeySet()) {
                    for (final String c : sheet.columnKeySet()) {
                        sheet.set(r, c, r + "/" + c);
                    }
                }

                for (int step = 0; step < 12; step++) {
                    final List<String> rowKeys = new ArrayList<>(sheet.rowKeySet());
                    final List<String> columnKeys = new ArrayList<>(sheet.columnKeySet());

                    switch (random.nextInt(4)) {
                        case 0 -> sheet.moveRow(rowKeys.get(random.nextInt(rowKeys.size())), random.nextInt(rowKeys.size()));
                        case 1 -> sheet.moveColumn(columnKeys.get(random.nextInt(columnKeys.size())), random.nextInt(columnKeys.size()));
                        case 2 -> sheet.swapRows(rowKeys.get(random.nextInt(rowKeys.size())), rowKeys.get(random.nextInt(rowKeys.size())));
                        default -> sheet.swapColumns(columnKeys.get(random.nextInt(columnKeys.size())), columnKeys.get(random.nextInt(columnKeys.size())));
                    }

                    // Every cell must still be reachable by key, and by the index its key resolves to.
                    final List<String> rowsNow = new ArrayList<>(sheet.rowKeySet());
                    final List<String> columnsNow = new ArrayList<>(sheet.columnKeySet());

                    assertEquals(4, rowsNow.size());
                    assertEquals(3, columnsNow.size());

                    for (int ri = 0; ri < rowsNow.size(); ri++) {
                        for (int ci = 0; ci < columnsNow.size(); ci++) {
                            final String expected = rowsNow.get(ri) + "/" + columnsNow.get(ci);
                            assertEquals(expected, sheet.get(rowsNow.get(ri), columnsNow.get(ci)));
                            assertEquals(expected, sheet.getAt(ri, ci));
                        }
                    }
                }
            }
        }

        @Test
        public void sortsStillReorderRowsAndColumnsCorrectly() {
            final Sheet<String, String, Integer> sheet = Sheet.rows(CommonUtil.asList("c", "a", "b"), CommonUtil.asList("z", "x", "y"),
                    new Integer[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });

            sheet.sortByRowKey();
            assertEquals(CommonUtil.asList("a", "b", "c"), new ArrayList<>(sheet.rowKeySet()));
            assertEquals(CommonUtil.asList(4, 5, 6), new ArrayList<>(sheet.rowValues("a")));

            sheet.sortByColumnKey();
            assertEquals(CommonUtil.asList("x", "y", "z"), new ArrayList<>(sheet.columnKeySet()));
            assertEquals(CommonUtil.asList(5, 6, 4), new ArrayList<>(sheet.rowValues("a")));
        }
    }

    // ------------------------------------------------------------------------------------------------
    // Dataset structural invariants around the changed mutators.
    // ------------------------------------------------------------------------------------------------
    @Nested
    public class DatasetMutatorInvariants {

        @Test
        public void columnCachesStayConsistentAcrossRenamesAndStructuralEdits() {
            final Dataset ds = Dataset.rows(CommonUtil.asList("a", "b", "c"), new Object[][] { { 1, 2, 3 }, { 4, 5, 6 } });

            ds.getColumnIndex("a");
            ds.renameColumns(CommonUtil.asMap("a", "b", "b", "a"));
            assertEquals(CommonUtil.asList("b", "a", "c"), ds.columnNames());
            assertEquals(CommonUtil.asList(1, 4), ds.getColumn("b"));

            ds.moveColumn("c", 0);
            assertEquals(CommonUtil.asList("c", "b", "a"), ds.columnNames());
            assertEquals(CommonUtil.asList(3, 6), ds.getColumn("c"));

            ds.removeColumn("b");
            assertEquals(CommonUtil.asList("c", "a"), ds.columnNames());
            assertEquals(CommonUtil.asList(2, 5), ds.getColumn("a"));

            ds.addColumn("d", CommonUtil.asList(7, 8));
            assertEquals(CommonUtil.asList("c", "a", "d"), ds.columnNames());
            assertEquals(2, ds.getColumnIndex("d"));
        }

        @Test
        public void sortsStillReorderRows() {
            final Dataset ds = Dataset.rows(CommonUtil.asList("a", "b"), new Object[][] { { 3, "c" }, { 1, "a" }, { 2, "b" } });

            ds.sortBy("a");

            assertEquals(CommonUtil.asList(1, 2, 3), ds.getColumn("a"));
            assertEquals(CommonUtil.asList("a", "b", "c"), ds.getColumn("b"));
        }

        @Test
        public void parallelSortAndMultiColumnSortStillWork() {
            final Dataset ds = Dataset.rows(CommonUtil.asList("a", "b"), new Object[][] { { 2, 5 }, { 1, 3 }, { 1, 4 } });

            ds.sortBy(CommonUtil.asList("a", "b"));
            assertEquals(CommonUtil.asList(1, 1, 2), ds.getColumn("a"));
            assertEquals(CommonUtil.asList(3, 4, 5), ds.getColumn("b"));

            ds.parallelSortBy("a", Comparator.<Object> comparingInt(v -> -((Integer) v)));
            assertEquals(CommonUtil.asList(2, 1, 1), ds.getColumn("a"));
        }
    }

    // ------------------------------------------------------------------------------------------------
    // Sheet.copy(Collection, Collection) rejects duplicate keys (now documented).
    // ------------------------------------------------------------------------------------------------
    @Nested
    public class SheetCopyKeySelection {

        @Test
        public void duplicateKeysAreRejected() {
            final Sheet<String, String, Integer> sheet = sheet3x2();

            assertThrows(IllegalArgumentException.class, () -> sheet.copy(CommonUtil.asList("r1", "r1"), CommonUtil.asList("c1")));
            assertThrows(IllegalArgumentException.class, () -> sheet.copy(CommonUtil.asList("r1"), CommonUtil.asList("c1", "c1")));
        }

        @Test
        public void aValidSubsetStillCopies() {
            final Sheet<String, String, Integer> sheet = sheet3x2();
            final Sheet<String, String, Integer> sub = sheet.copy(CommonUtil.asList("r3", "r1"), CommonUtil.asList("c2"));

            assertEquals(CommonUtil.asList("r3", "r1"), new ArrayList<>(sub.rowKeySet()));
            assertEquals(CommonUtil.asList(6), new ArrayList<>(sub.rowValues("r3")));
            assertEquals(CommonUtil.asList(2), new ArrayList<>(sub.rowValues("r1")));
            assertFalse(sub.isFrozen());
        }
    }

    // ------------------------------------------------------------------------------------------------
    // J2 - the all-null short-circuit in the value-based sorts.
    // ------------------------------------------------------------------------------------------------
    @Nested
    public class SheetValueSortsOnAllNullKeys {

        @Test
        public void anAllNullKeyColumnOrRowLeavesTheOrderUnchanged() {
            final Sheet<String, String, Integer> byColumn = Sheet.rows(CommonUtil.asList("r1", "r2"), CommonUtil.asList("c1"),
                    new Integer[][] { { null }, { null } });
            assertDoesNotThrow(() -> byColumn.sortRowsByColumnValues("c1", (Comparator<Integer>) Integer::compareTo));
            assertEquals(CommonUtil.asList("r1", "r2"), new ArrayList<>(byColumn.rowKeySet()));

            final Sheet<String, String, Integer> byRow = Sheet.rows(CommonUtil.asList("r1"), CommonUtil.asList("c1", "c2"), new Integer[][] { { null, null } });
            assertDoesNotThrow(() -> byRow.sortColumnsByRowValues("r1", (Comparator<Integer>) Integer::compareTo));
            assertEquals(CommonUtil.asList("c1", "c2"), new ArrayList<>(byRow.columnKeySet()));
        }

        @Test
        public void oneNonNullValueBringsTheComparatorBack() {
            final Sheet<String, String, Integer> sheet = Sheet.rows(CommonUtil.asList("r1", "r2"), CommonUtil.asList("c1"),
                    new Integer[][] { { null }, { 1 } });

            assertThrows(NullPointerException.class, () -> sheet.sortRowsByColumnValues("c1", (Comparator<Integer>) Integer::compareTo));

            final Sheet<String, String, Integer> nullSafe = Sheet.rows(CommonUtil.asList("r1", "r2"), CommonUtil.asList("c1"),
                    new Integer[][] { { 2 }, { 1 } });
            nullSafe.sortRowsByColumnValues("c1", Comparators.nullsFirst());
            assertEquals(CommonUtil.asList("r2", "r1"), new ArrayList<>(nullSafe.rowKeySet()));
        }
    }

    // ------------------------------------------------------------------------------------------------
    // Collection-level smoke checks for the classes touched only by Javadoc/diagnostics changes.
    // ------------------------------------------------------------------------------------------------
    @Nested
    public class UntouchedBehaviourStillHolds {

        @Test
        public void setMultimapCoreOperations() {
            final SetMultimap<String, Integer> mm = CommonUtil.newSetMultimap();
            mm.put("a", 1);
            mm.put("a", 2);
            mm.put("a", 1);
            mm.put("b", 1);

            assertEquals(CommonUtil.asSet(1, 2), mm.get("a"));
            assertEquals(2, mm.keyCount());
            assertEquals(CommonUtil.asSet("a", "b"), mm.invert().get(1));
            assertEquals(mm, mm.copy());
            assertEquals(CommonUtil.asSet(1, 2), mm.toImmutableMap().get("a"));
        }

        @Test
        public void datasetSliceStillFailsFastOnAStructuralParentEdit() {
            final Dataset ds = dataset3Rows();
            final Dataset slice = ds.slice(0, 2);

            ds.addRow(new Object[] { 4, "w" });

            assertThrows(ConcurrentModificationException.class, () -> slice.getColumn("a").size());
        }

        @Test
        public void datasetValueWritesStillLeaveStreamsValid() {
            final Dataset ds = dataset3Rows();
            final com.landawn.abacus.util.stream.Stream<Object> stream = ds.stream("a");

            ds.set(0, 0, 99);
            ds.updateAll(v -> v);
            ds.replaceIf(v -> false, null);

            assertEquals(CommonUtil.asList(99, 2, 3), stream.toList());
        }

        @Test
        public void sheetCopyAndTransposeAreUnaffected() {
            final Sheet<String, String, Integer> sheet = sheet3x2();
            final Sheet<String, String, Integer> copy = sheet.copy();

            copy.set("r1", "c1", 99);
            assertEquals(Integer.valueOf(1), sheet.get("r1", "c1"));

            final Sheet<String, String, Integer> transposed = sheet.transposed();
            assertEquals(CommonUtil.asList("c1", "c2"), new ArrayList<>(transposed.rowKeySet()));
            assertEquals(Integer.valueOf(5), transposed.get("c1", "r3"));
        }

        @Test
        public void collectionsOfKeysAreStillRejectedWhenNullOrEmpty() {
            final Sheet<String, String, Integer> sheet = sheet3x2();
            final Collection<String> nullKeys = null;

            assertThrows(IllegalArgumentException.class, () -> sheet.copy(nullKeys, CommonUtil.asList("c1")));
            assertThrows(IllegalArgumentException.class, () -> sheet.copy(new ArrayList<>(), CommonUtil.asList("c1")));
            assertThrows(IllegalArgumentException.class, () -> sheet.sortRowsByColumnValues(new ArrayList<>(), (a, b) -> 0));
        }
    }
}
