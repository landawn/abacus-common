package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Tuple.Tuple2;
import com.landawn.abacus.util.Tuple.Tuple3;
import com.landawn.abacus.util.stream.Collectors;
import com.landawn.abacus.util.stream.Stream;

/**
 * Regression tests for the 2026-09-02 review pass over {@code Dataset}, {@code RowDataset} and {@code Sheet}.
 *
 * <p>One nested class per finding, named after it. Unless its javadoc says it only pins already-correct
 * behaviour, every test here fails against the pre-fix classes.</p>
 */
public class DatasetSheetRegressionDTest extends TestBase {

    /** A plain bean with two properties, so that a Dataset column named {@code extra} has no matching property. */
    public static final class Person {
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

    private static Dataset ab(final Object[]... rows) {
        return Dataset.rows(Arrays.asList("a", "b"), rows);
    }

    @Nested
    public class B01_DivideColumnTupleOverloadsClearTheTuplePerRow extends TestBase {

        @Test
        public void tuple2ConsumerThatSetsNothingProducesNulls() {
            final Dataset ds = Dataset.rows(Arrays.asList("v"), new Object[][] { { "a" }, { "b" }, { "a" } });

            ds.divideColumn("v", Tuple.of("x", "y"), (final Object v, final Pair<Object, Object> pair) -> {
                if ("a".equals(v)) {
                    pair.set("L", "R");
                }
            });

            assertEquals(Arrays.asList("x", "y"), ds.columnNames());
            assertEquals(Arrays.asList("L", "R"), ds.getRow(0));
            assertEquals(Arrays.asList(null, null), ds.getRow(1)); // used to be [L, R], leaked from row 0
            assertEquals(Arrays.asList("L", "R"), ds.getRow(2));
        }

        @Test
        public void tuple2ConsumerThatSetsOnlyOneSideLeavesTheOtherNull() {
            final Dataset ds = Dataset.rows(Arrays.asList("v"), new Object[][] { { 1 }, { 2 } });

            ds.divideColumn("v", Tuple.of("x", "y"), (final Object v, final Pair<Object, Object> pair) -> {
                if ((Integer) v == 1) {
                    pair.set("both", "both");
                } else {
                    pair.setLeft("left-only");
                }
            });

            assertEquals(Arrays.asList("both", "both"), ds.getRow(0));
            assertEquals(Arrays.asList("left-only", null), ds.getRow(1));
        }

        @Test
        public void tuple3ConsumerThatSetsNothingProducesNulls() {
            final Dataset ds = Dataset.rows(Arrays.asList("v"), new Object[][] { { "a" }, { "b" } });

            ds.divideColumn("v", Tuple.of("x", "y", "z"), (final Object v, final Triple<Object, Object, Object> t) -> {
                if ("a".equals(v)) {
                    t.set(1, 2, 3);
                }
            });

            assertEquals(Arrays.asList(1, 2, 3), ds.getRow(0));
            assertEquals(Arrays.asList(null, null, null), ds.getRow(1));
        }

        /** Pins the sibling that was already correct. */
        @Test
        public void objectArrayOverloadStillClearsItsBuffer() {
            final Dataset ds = Dataset.rows(Arrays.asList("v"), new Object[][] { { "a" }, { "b" } });

            ds.divideColumn("v", Arrays.asList("x", "y"), (final Object v, final Object[] out) -> {
                if ("a".equals(v)) {
                    out[0] = "L";
                    out[1] = "R";
                }
            });

            assertEquals(Arrays.asList(null, null), ds.getRow(1));
        }
    }

    @Nested
    public class B02_SheetReadsAreSafeWithoutWrites extends TestBase {

        private Object indexMapOf(final Sheet<?, ?, ?> sheet, final String fieldName) throws Exception {
            final Field f = Sheet.class.getDeclaredField(fieldName);
            f.setAccessible(true);
            return f.get(sheet);
        }

        private int indexSizeOf(final Sheet<?, ?, ?> sheet, final String fieldName) throws Exception {
            final Object index = indexMapOf(sheet, fieldName);
            final java.lang.reflect.Method size = index.getClass().getDeclaredMethod("size");
            size.setAccessible(true);
            return (int) size.invoke(index);
        }

        @Test
        public void constructorsBuildTheIndexMapsEagerly() throws Exception {
            final Sheet<String, String, Integer> dataless = new Sheet<>(Arrays.asList("r1", "r2"), Arrays.asList("c1"));
            assertNotNull(indexMapOf(dataless, "_rowKeyIndexMap"));
            assertNotNull(indexMapOf(dataless, "_columnKeyIndexMap"));
            assertEquals(2, indexSizeOf(dataless, "_rowKeyIndexMap"));

            final Sheet<String, String, Integer> empty = new Sheet<>();
            assertNotNull(indexMapOf(empty, "_rowKeyIndexMap"));

            final Sheet<String, String, Integer> copied = dataless.copy();
            assertNotNull(indexMapOf(copied, "_rowKeyIndexMap"));

            final Sheet<String, String, Integer> transposed = dataless.transposed();
            assertNotNull(indexMapOf(transposed, "_rowKeyIndexMap"));
            assertEquals(1, indexSizeOf(transposed, "_rowKeyIndexMap"));
        }

        /**
         * The reproduction from the review: readers of a frozen Sheet that never received data raced the lazy
         * index-map build and saw a half-built map ("No row found by key" for a key that exists). With the
         * maps built in the constructor there is nothing left to race on.
         */
        @Test
        public void concurrentKeyedReadsOnAFrozenDatalessSheetNeverFail() throws Exception {
            final int rows = 200_000;
            final List<Integer> rowKeys = new ArrayList<>(rows);

            for (int i = 0; i < rows; i++) {
                rowKeys.add(i);
            }

            final AtomicInteger failedReads = new AtomicInteger();
            final AtomicReference<Throwable> sample = new AtomicReference<>();

            for (int trial = 0; trial < 5; trial++) {
                final Sheet<Integer, String, Integer> sheet = new Sheet<>(rowKeys, Arrays.asList("c"));
                sheet.freeze();

                final int threads = 4;
                final CountDownLatch start = new CountDownLatch(1);
                final CountDownLatch done = new CountDownLatch(threads);

                for (int t = 0; t < threads; t++) {
                    final int id = t;

                    new Thread(() -> {
                        try {
                            start.await();

                            if (id > 0) { // let thread 0 go first, so the others arrive while a lazy build would still be running
                                final long end = System.nanoTime() + 30_000L * id;

                                while (System.nanoTime() < end) {
                                    Thread.onSpinWait();
                                }
                            }

                            for (int k = 0; k < 200; k++) {
                                try {
                                    sheet.rowValues(rows - 1 - k).get(0);
                                    sheet.columnValues("c").get(rows - 1 - k);
                                } catch (final Throwable e) {
                                    failedReads.incrementAndGet();
                                    sample.compareAndSet(null, e);
                                }
                            }
                        } catch (final InterruptedException e) {
                            Thread.currentThread().interrupt();
                        } finally {
                            done.countDown();
                        }
                    }).start();
                }

                start.countDown();
                done.await();
            }

            assertEquals(0, failedReads.get(), () -> "concurrent reads failed, e.g. " + sample.get());
        }

        @Test
        public void keyedAccessStillWorksAfterEveryStructuralMutation() {
            final Sheet<String, String, Integer> sheet = new Sheet<>(Arrays.asList("r1", "r2"), Arrays.asList("c1", "c2"));

            sheet.addRow("r3", Arrays.asList(31, 32));
            sheet.addRow(0, "r0", Arrays.asList(1, 2));
            sheet.addColumn("c3", Arrays.asList(13, null, null, 33));
            sheet.removeRow("r1");
            sheet.moveRow("r3", 0);
            sheet.swapColumns("c1", "c3");
            sheet.renameRow("r0", "zero");
            sheet.sortByRowKey();

            assertEquals(Arrays.asList("r2", "r3", "zero"), new ArrayList<>(sheet.rowKeySet()));
            assertEquals(Arrays.asList("c3", "c2", "c1"), new ArrayList<>(sheet.columnKeySet()));
            assertEquals(Integer.valueOf(1), sheet.get("zero", "c1"));
            assertEquals(Integer.valueOf(33), sheet.get("r3", "c3"));
            assertEquals(Integer.valueOf(32), sheet.get("r3", "c2"));
            assertEquals(Arrays.asList(33, 32, 31), sheet.rowValues("r3"));
        }
    }

    @Nested
    public class B03_GetDoubleNarrowsWithDoubleValue extends TestBase {

        @Test
        public void floatCellReadsTheSameThroughCursorRowAndDoubleValue() {
            final Dataset ds = Dataset.rows(Arrays.asList("f"), new Object[][] { { 0.1f } });
            ds.moveToRow(0);

            final double expected = ((Number) 0.1f).doubleValue(); // 0.10000000149011612, not 0.1

            assertEquals(expected, ds.getDouble("f"));
            assertEquals(expected, ds.getDouble(0));
            assertEquals(expected, ds.row(0).getDouble("f"));
            assertEquals(ds.row(0).getDouble("f"), ds.getDouble("f"));
        }

        @Test
        public void otherNumberTypesAndNullAreUnchanged() {
            final Dataset ds = Dataset.rows(Arrays.asList("d", "i", "big", "n"), new Object[][] { { 0.1d, 7, new BigInteger("12345678901234567890"), null } });
            ds.moveToRow(0);

            assertEquals(0.1d, ds.getDouble("d"));
            assertEquals(7d, ds.getDouble("i"));
            assertEquals(new BigInteger("12345678901234567890").doubleValue(), ds.getDouble("big"));
            assertEquals(0d, ds.getDouble("n"));
            assertEquals(0.1f, ds.getFloat("d"));
            assertEquals(((Number) 0.1d).floatValue(), ds.getFloat("d"));
            assertEquals(0f, ds.getFloat("n"));
            assertEquals(ds.row(0).getFloat("d"), ds.getFloat("d"));
        }
    }

    @Nested
    public class B04_TopByKeepsTheEarliestTiedRows extends TestBase {

        private final Dataset ds = Dataset.rows(Arrays.asList("id", "v"), new Object[][] { { 1, 5 }, { 2, 5 }, { 3, 5 }, { 4, 9 } });

        @Test
        public void singleColumn() {
            assertEquals(Arrays.asList(1, 4), ds.topBy("v", 2).getColumn("id"));
            assertEquals(Arrays.asList(1, 2, 4), ds.topBy("v", 3).getColumn("id"));
        }

        @Test
        public void singleColumnWithComparator() {
            assertEquals(Arrays.asList(1, 4), ds.topBy("v", 2, Comparators.naturalOrder()).getColumn("id"));
            // reverse order: the "greatest" values are the 5s; the earliest two of them must survive
            assertEquals(Arrays.asList(1, 2), ds.topBy("v", 2, Comparators.reverseOrder()).getColumn("id"));
        }

        @Test
        public void multipleColumnsAndKeyExtractor() {
            assertEquals(Arrays.asList(1, 4), ds.topBy(Arrays.asList("v"), 2).getColumn("id"));
            assertEquals(Arrays.asList(1, 4), ds.topBy(Arrays.asList("v"), 2, Comparators.OBJECT_ARRAY_COMPARATOR).getColumn("id"));
            assertEquals(Arrays.asList(1, 4), ds.topBy(Arrays.asList("v"), 2, row -> (Integer) row.get(0)).getColumn("id"));
        }

        /** Pins behaviour that was already right: with every row tied, the earliest n survive. */
        @Test
        public void allTied() {
            final Dataset tied = Dataset.rows(Arrays.asList("id", "v"), new Object[][] { { 1, 5 }, { 2, 5 }, { 3, 5 } });
            assertEquals(Arrays.asList(1, 2), tied.topBy("v", 2).getColumn("id"));
        }

        @Test
        public void noTiesUnchanged() {
            final Dataset plain = Dataset.rows(Arrays.asList("id", "v"), new Object[][] { { 1, 3 }, { 2, 9 }, { 3, 1 }, { 4, 7 } });
            assertEquals(Arrays.asList(2, 4), plain.topBy("v", 2).getColumn("id"));
        }
    }

    @Nested
    public class B05_ColumnNamesViewIsRecognisedAsTheWholeColumnSet extends TestBase {

        @Test
        public void everyViewIsRecognisedButNotACopyOfIt() {
            final RowDataset ds = (RowDataset) ab(new Object[] { 1, 2 });

            final ImmutableList<String> view = ds.columnNames();
            assertTrue(ds.isColumnNameList(view));
            assertTrue(ds.isColumnNameList(ds.columnNames())); // a later view too
            assertEquals(view, ds.columnNames());
            assertFalse(ds.isColumnNameList(new ArrayList<>(view)));
            assertFalse(ds.isColumnNameList(ImmutableList.copyOf(view)));
            assertFalse(ds.isColumnNameList(ImmutableList.wrap(new ArrayList<>(view))));
            assertFalse(ds.isColumnNameList(Arrays.asList("a", "b")));
            assertFalse(ds.isColumnNameList(null));
            assertThrows(UnsupportedOperationException.class, () -> view.add("x"));
            assertThrows(UnsupportedOperationException.class, () -> view.iterator().remove());
            assertThrows(UnsupportedOperationException.class, () -> view.subList(0, 1).clear());
            assertThrows(UnsupportedOperationException.class, () -> view.listIterator().set("x"));
        }

        @Test
        public void theViewTracksInPlaceMutations() {
            final RowDataset ds = (RowDataset) ab(new Object[] { 1, 2 });
            final ImmutableList<String> view = ds.columnNames();

            ds.renameColumn("a", "A");
            ds.addColumn("c", Arrays.asList(3));
            ds.moveColumn("c", 0);
            ds.removeColumn("b");

            assertEquals(Arrays.asList("c", "A"), view);
            assertTrue(ds.isColumnNameList(view));
            assertEquals(view, ds.columnNames());
        }

        @Test
        public void copiesDoNotShareTheView() {
            final RowDataset ds = (RowDataset) ab(new Object[] { 1, 2 });
            final RowDataset copy = (RowDataset) ds.copy();

            assertNotSame(ds.columnNames(), copy.columnNames());
            assertFalse(copy.isColumnNameList(ds.columnNames()));
            assertTrue(copy.isColumnNameList(copy.columnNames()));
        }

        /**
         * Kryo deep copies ({@code clone()}, {@code Beans.deepCopy}) cannot instantiate the sealed ImmutableList classes,
         * so no view may be cached in a field: this pins that copying still works after {@code columnNames()} was
         * called, and that the copy's views are its own.
         */
        @Test
        public void kryoDeepCopiesStillWorkAndGetTheirOwnViews() {
            final RowDataset ds = (RowDataset) ab(new Object[] { 1, 2 });
            final ImmutableList<String> view = ds.columnNames();
            final RowDataset clone = (RowDataset) ds.clone();
            final RowDataset deepCopy = Beans.deepCopy(ds);

            assertEquals(Arrays.asList("a", "b"), clone.columnNames());
            assertEquals(Arrays.asList("a", "b"), deepCopy.columnNames());
            assertNotSame(view, clone.columnNames());
            assertTrue(clone.isColumnNameList(clone.columnNames()));
            assertTrue(deepCopy.isColumnNameList(deepCopy.columnNames()));
            assertFalse(clone.isColumnNameList(view));

            clone.renameColumn("a", "A");
            assertEquals(Arrays.asList("A", "b"), clone.columnNames());
            assertEquals(Arrays.asList("a", "b"), view); // the original's view is untouched
            assertEquals(Arrays.asList("a", "b"), ds.columnNames());
        }

        @Test
        public void beanConversionUsesAnExplicitMissingPropertyPolicy() {
            final Dataset ds = Dataset.rows(Arrays.asList("id", "name", "extra"), new Object[][] { { 1, "a", "x" } });

            // whole column set, however it is spelled: the unmatched "extra" column is skipped
            assertEquals("a", ds.toList(Person.class).get(0).getName());
            assertEquals("a", ds.<Person> toList(ds.columnNames(), Person.class).get(0).getName());
            assertEquals("a", ds.<Integer, Person> toMap("id", ds.columnNames(), Person.class).get(1).getName());
            assertEquals("a", ds.<Integer, Person> toMultimap("id", ds.columnNames(), Person.class).get(1).get(0).getName());
            assertEquals("a", ds.<Person> stream(ds.columnNames(), Person.class).first().get().getName());
            assertEquals("a", ds.<Person> getRow(0, ds.columnNames(), Person.class).getName());

            // Copying a selection does not change its policy; strictness is chosen explicitly.
            final Dataset strict = ds.withMissingPropertyPolicy(Dataset.MissingPropertyPolicy.ERROR);
            final List<String> explicit = Arrays.asList("id", "name", "extra");
            assertThrows(IllegalArgumentException.class, () -> strict.toList(explicit, Person.class));
            assertThrows(IllegalArgumentException.class, () -> strict.toMap("id", explicit, Person.class));
            assertThrows(IllegalArgumentException.class, () -> strict.toMultimap("id", explicit, Person.class));
            assertThrows(IllegalArgumentException.class, () -> strict.getRow(0, explicit, Person.class));
        }
    }

    @Nested
    public class B06_ColumnsStreamIsFailFast extends TestBase {

        @Test
        public void structuralChangeAfterCreationThrowsConcurrentModification() {
            final Dataset ds = ab(new Object[] { 1, 2 });
            final Stream<ImmutableList<Object>> columns = ds.columns();

            ds.removeColumn("b");

            assertThrows(ConcurrentModificationException.class, columns::toList);
        }

        @Test
        public void addingARowInvalidatesToo() {
            final Dataset ds = ab(new Object[] { 1, 2 });
            final Stream<ImmutableList<Object>> columns = ds.columns();

            ds.addRow(new Object[] { 3, 4 });

            assertThrows(ConcurrentModificationException.class, columns::toList);
        }

        @Test
        public void valueWritesDoNotInvalidateAndViewsStayLive() {
            final Dataset ds = ab(new Object[] { 1, 2 });
            final Stream<ImmutableList<Object>> columns = ds.columns();

            ds.set(0, 0, 10);
            ds.updateAll(v -> (Integer) v + 1);

            final List<ImmutableList<Object>> list = columns.toList();
            assertEquals(2, list.size());
            assertEquals(Arrays.asList(11), list.get(0));
            assertEquals(Arrays.asList(3), list.get(1));
        }
    }

    @Nested
    public class B07_PivotResultColumnNeverCollidesWithTheKeyOrPivotColumn extends TestBase {

        private final Dataset ds = Dataset.rows(Arrays.asList("region", "product", "sales"),
                new Object[][] { { "North", "A", 100 }, { "North", "B", 200 }, { "South", "A", 150 }, { "North", "A", 5 } });

        @Test
        public void aggregatingTheKeyColumnItselfWorks() {
            final Sheet<String, String, Long> counts = ds.pivot("region", "product", "region", Collectors.counting());

            assertEquals(Long.valueOf(2), counts.get("North", "A"));
            assertEquals(Long.valueOf(1), counts.get("North", "B"));
            assertEquals(Long.valueOf(1), counts.get("South", "A"));
            assertEquals(null, counts.get("South", "B"));
        }

        @Test
        public void aggregatingThePivotColumnAndCollectionOverloadsWork() {
            final Sheet<String, String, Long> byProduct = ds.pivot("region", "product", "product", Collectors.counting());
            assertEquals(Long.valueOf(2), byProduct.get("North", "A"));

            final Sheet<String, String, Integer> sums = ds.pivot("region", "product", Arrays.asList("region", "sales"),
                    Collectors.summingInt(arr -> ((Number) arr[1]).intValue()));
            assertEquals(Integer.valueOf(105), sums.get("North", "A"));

            final Sheet<String, String, Integer> mapped = ds.pivot("region", "product", Arrays.asList("product", "sales"),
                    row -> ((Number) row.get(1)).intValue(), Collectors.summingInt(Integer::intValue));
            assertEquals(Integer.valueOf(150), mapped.get("South", "A"));
        }

        /** Pins the previously working shape. */
        @Test
        public void ordinaryPivotUnchanged() {
            final Sheet<String, String, Integer> sums = ds.pivot("region", "product", "sales", Collectors.summingInt(o -> ((Number) o).intValue()));

            assertEquals(Arrays.asList("North", "South"), new ArrayList<>(sums.rowKeySet()));
            assertEquals(Arrays.asList("A", "B"), new ArrayList<>(sums.columnKeySet()));
            assertEquals(Integer.valueOf(105), sums.get("North", "A"));
            assertEquals(Integer.valueOf(200), sums.get("North", "B"));
            assertEquals(Integer.valueOf(150), sums.get("South", "A"));
            assertEquals(null, sums.get("South", "B"));
        }

        @Test
        public void nullKeyOrPivotValueIsReportedByColumnName() {
            final Dataset withNullKey = Dataset.rows(Arrays.asList("r", "c", "v"), new Object[][] { { null, "A", 1 } });
            final IllegalArgumentException e1 = assertThrows(IllegalArgumentException.class,
                    () -> withNullKey.pivot("r", "c", "v", Collectors.summingInt(o -> ((Number) o).intValue())));
            assertTrue(e1.getMessage().contains("'r'"), e1.getMessage());

            final Dataset withNullPivot = Dataset.rows(Arrays.asList("r", "c", "v"), new Object[][] { { "x", null, 1 } });
            final IllegalArgumentException e2 = assertThrows(IllegalArgumentException.class,
                    () -> withNullPivot.pivot("r", "c", "v", Collectors.summingInt(o -> ((Number) o).intValue())));
            assertTrue(e2.getMessage().contains("'c'"), e2.getMessage());
        }
    }

    @Nested
    public class B08_RollupAndCubeValidateAtTheCallSite extends TestBase {

        private final Dataset ds = Dataset.rows(Arrays.asList("region", "country", "sales"),
                new Object[][] { { "N", "US", 10 }, { "N", "CA", 5 }, { "S", "MX", 2 } });

        @Test
        public void unknownKeyColumnIsRejectedBeforeTheStreamIsReturned() {
            assertThrows(IllegalArgumentException.class, () -> ds.rollup(Arrays.asList("nope")));
            assertThrows(IllegalArgumentException.class, () -> ds.rollup(Arrays.asList("region", "nope"), "sales", "total", Collectors.counting()));
            assertThrows(IllegalArgumentException.class, () -> ds.rollup(Arrays.asList("nope"), Arrays.asList("sales"), "data", Object[].class));
            assertThrows(IllegalArgumentException.class, () -> ds.rollup(Arrays.asList("nope"), Arrays.asList("sales"), "data", Collectors.toList()));
            assertThrows(IllegalArgumentException.class, () -> ds.rollup(Arrays.asList("nope"), r -> r.join("-")));
            assertThrows(IllegalArgumentException.class, () -> ds.cube(Arrays.asList("nope")));
            assertThrows(IllegalArgumentException.class, () -> ds.cube(Arrays.asList("nope"), "sales", "total", Collectors.counting()));
            assertThrows(IllegalArgumentException.class, () -> ds.cube(Arrays.asList("nope"), Arrays.asList("sales"), "data", Object[].class));
            assertThrows(IllegalArgumentException.class, () -> ds.cube(Arrays.asList("nope"), r -> r.join("-"), "sales", "total", Collectors.counting()));
        }

        @Test
        public void emptyOrNullKeysGetTheirOwnMessage() {
            final IllegalArgumentException empty = assertThrows(IllegalArgumentException.class, () -> ds.rollup(Collections.<String> emptyList()));
            assertTrue(empty.getMessage().contains("keyColumnNames"), empty.getMessage());

            final IllegalArgumentException nul = assertThrows(IllegalArgumentException.class, () -> ds.cube((Collection<String>) null));
            assertTrue(nul.getMessage().contains("keyColumnNames"), nul.getMessage());
        }

        @Test
        public void aggregateArgumentsAreValidatedEagerlyToo() {
            assertThrows(IllegalArgumentException.class, () -> ds.rollup(Arrays.asList("region"), "nope", "total", Collectors.counting()));
            assertThrows(IllegalArgumentException.class, () -> ds.rollup(Arrays.asList("region"), "sales", "total", null));
            assertThrows(IllegalArgumentException.class, () -> ds.rollup(Arrays.asList("region"), "sales", null, Collectors.counting()));
            assertThrows(IllegalArgumentException.class, () -> ds.rollup(Arrays.asList("region"), Arrays.asList("sales"), "data", String.class));
            assertThrows(IllegalArgumentException.class, () -> ds.rollup(Arrays.asList("region"), Collections.<String> emptyList(), "data", Object[].class));
            assertThrows(IllegalArgumentException.class, () -> ds.cube(Arrays.asList("region"), "sales", "", Collectors.counting()));
            assertThrows(IllegalArgumentException.class, () -> ds.cube(Arrays.asList("region"), Arrays.asList("nope"), "data", Collectors.toList()));
        }

        /** Pins the results themselves: the validation must not change what a valid call produces. */
        @Test
        public void validCallsAreUnchanged() {
            final List<Dataset> levels = ds.rollup(Arrays.asList("region", "country")).toList();
            assertEquals(3, levels.size());
            assertEquals(Arrays.asList("region", "country", "count"), levels.get(0).columnNames());
            assertEquals(Arrays.asList("region", "count"), levels.get(1).columnNames());
            assertEquals(Arrays.asList("count"), levels.get(2).columnNames());
            assertEquals(Arrays.asList(3), levels.get(2).getColumn("count"));

            final List<Dataset> cube = ds.cube(Arrays.asList("region", "country"), "sales", "total", Collectors.summingInt(o -> ((Number) o).intValue()))
                    .toList();
            assertEquals(4, cube.size());
            assertEquals(Arrays.asList(17), cube.get(3).getColumn("total"));
        }
    }

    @Nested
    public class B09_ReverseForEachRangeIsValidatedExactly extends TestBase {

        private final Dataset ds = Dataset.rows(Arrays.asList("id", "n"), new Object[][] { { 1, "a" }, { 2, "b" }, { 3, "c" } });

        private List<Object> visit(final int from, final int to) {
            final List<Object> out = new ArrayList<>();
            ds.forEach(from, to, r -> out.add(r.get(0)));
            return out;
        }

        @Test
        public void fromEqualToSizeIsRejectedInReverseMode() {
            assertThrows(IndexOutOfBoundsException.class, () -> visit(3, -1));
            assertThrows(IndexOutOfBoundsException.class, () -> visit(3, 0));
            assertThrows(IndexOutOfBoundsException.class, () -> visit(4, -1));
            assertThrows(IndexOutOfBoundsException.class, () -> visit(2, -2));
            assertThrows(IndexOutOfBoundsException.class, () -> ds.forEach(3, -1, Tuple.of("id", "n"), (a, b) -> {
            }));
            assertThrows(IndexOutOfBoundsException.class, () -> ds.forEach(3, -1, Tuple.of("id", "n", "id"), (a, b, c) -> {
            }));
            assertThrows(IndexOutOfBoundsException.class, () -> ds.forEach(3, -1, Arrays.asList("id"), r -> {
            }));
        }

        @Test
        public void inRangeReverseAndForwardVisitsAreUnchanged() {
            assertEquals(Arrays.asList(3, 2, 1), visit(2, -1));
            assertEquals(Arrays.asList(3, 2), visit(2, 0));
            assertEquals(Arrays.asList(2, 1), visit(1, -1));
            assertEquals(Arrays.asList(1, 2, 3), visit(0, 3));
            assertEquals(Arrays.asList(), visit(3, 3));
            assertEquals(Arrays.asList(), visit(0, 0));

            final List<Object> tuple = new ArrayList<>();
            ds.forEach(2, -1, Tuple.of("id", "n"), (a, b) -> tuple.add(a));
            assertEquals(Arrays.asList(3, 2, 1), tuple);
        }

        @Test
        public void emptyDataset() {
            final Dataset empty = Dataset.rows(Arrays.asList("id"), new Object[0][]);
            final List<Object> out = new ArrayList<>();
            empty.forEach(0, 0, r -> out.add(r.get(0)));
            assertTrue(out.isEmpty());
            assertThrows(IndexOutOfBoundsException.class, () -> empty.forEach(0, -1, r -> out.add(r.get(0))));
        }
    }

    @Nested
    public class B10_NullArgumentsThrowIllegalArgumentException extends TestBase {

        private Dataset ds() {
            return ab(new Object[] { 1, 2 });
        }

        @Test
        public void methodsThatUsedToThrowNullPointerException() {
            assertThrows(IllegalArgumentException.class, () -> ds().renameColumns((Map<String, String>) null));
            assertThrows(IllegalArgumentException.class, () -> ds().containsAllColumns(null));
            assertThrows(IllegalArgumentException.class, () -> ds().updateRows(null, (i, c, v) -> v));
            assertThrows(IllegalArgumentException.class, () -> ds().removeRowsAt((int[]) null));
            assertThrows(IllegalArgumentException.class, () -> ds().forEach((Tuple2<String, String>) null, (x, y) -> {
            }));
            assertThrows(IllegalArgumentException.class, () -> ds().forEach((Tuple3<String, String, String>) null, (x, y, z) -> {
            }));
            assertThrows(IllegalArgumentException.class, () -> ds().filter((Tuple2<String, String>) null, (x, y) -> true));
            assertThrows(IllegalArgumentException.class, () -> ds().filter((Tuple3<String, String, String>) null, (x, y, z) -> true));
            assertThrows(IllegalArgumentException.class, () -> ds().combineColumns((Tuple2<String, String>) null, "n", (x, y) -> x));
            assertThrows(IllegalArgumentException.class, () -> ds().combineColumns((Tuple3<String, String, String>) null, "n", (x, y, z) -> x));
            assertThrows(IllegalArgumentException.class, () -> ds().addColumn("n", (Tuple2<String, String>) null, (x, y) -> x));
            assertThrows(IllegalArgumentException.class, () -> ds().addColumn("n", (Tuple3<String, String, String>) null, (x, y, z) -> x));
            assertThrows(IllegalArgumentException.class, () -> ds().divideColumn("a", (Tuple2<String, String>) null, (v, p) -> {
            }));
            assertThrows(IllegalArgumentException.class, () -> ds().divideColumn("a", (Tuple3<String, String, String>) null, (v, t) -> {
            }));
            assertThrows(IllegalArgumentException.class, () -> ds().mapColumns((Tuple2<String, String>) null, "n", Arrays.asList("a"), (x, y) -> x));
            assertThrows(IllegalArgumentException.class, () -> ds().mapColumns((Tuple3<String, String, String>) null, "n", Arrays.asList("a"), (x, y, z) -> x));
            assertThrows(IllegalArgumentException.class,
                    () -> ds().flatMapColumns((Tuple2<String, String>) null, "n", Arrays.asList("a"), (x, y) -> Arrays.asList(x)));
            assertThrows(IllegalArgumentException.class,
                    () -> ds().flatMapColumns((Tuple3<String, String, String>) null, "n", Arrays.asList("a"), (x, y, z) -> Arrays.asList(x)));
        }

        @Test
        public void methodsThatUsedToSilentlyAcceptNull() {
            assertThrows(IllegalArgumentException.class, () -> ds().moveColumns(null, 0));
            assertThrows(IllegalArgumentException.class, () -> ds().moveColumns(null, 99));
            assertThrows(IllegalArgumentException.class, () -> ds().removeColumns((Collection<String>) null));
            assertThrows(IllegalArgumentException.class, () -> ds().updateColumns(null, (i, c, v) -> v));
            assertThrows(IllegalArgumentException.class, () -> ds().convertColumns(null));
            assertThrows(IllegalArgumentException.class, () -> ds().addColumns(null, null));
            assertThrows(IllegalArgumentException.class, () -> ds().addColumns(Arrays.asList("c"), null));
        }

        /** An empty selection is still a harmless no-op for the mutators that act on "these columns". */
        @Test
        public void emptySelectionsRemainNoOps() {
            final Dataset ds = ds();
            final Stream<Object> live = ds.stream("a");

            ds.moveColumns(Collections.<String> emptyList(), 0);
            ds.removeColumns(Collections.<String> emptyList());
            ds.updateColumns(Collections.<String> emptyList(), (i, c, v) -> v);
            ds.convertColumns(Collections.<String, Class<?>> emptyMap());
            ds.addColumns(Collections.<String> emptyList(), Collections.<List<Object>> emptyList());

            assertEquals(Arrays.asList("a", "b"), ds.columnNames());
            assertEquals(Arrays.asList(1), live.toList()); // nothing invalidated the stream
        }
    }

    /** Pins behaviour that was correct but undocumented until this pass. */
    @Nested
    public class B11_ToXmlRejectsNamesThatAreNotXmlNames extends TestBase {

        @Test
        public void columnNameWithASpaceIsRejectedByToXmlButNotToJson() {
            final Dataset ds = Dataset.rows(Arrays.asList("first name", "1st"), new Object[][] { { "a", "b" } });

            final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, ds::toXml);
            assertTrue(e.getMessage().contains("first name"), e.getMessage());
            assertThrows(IllegalArgumentException.class, () -> Dataset.rows(Arrays.asList("ok"), new Object[][] { { "a" } }).toXml("row element"));

            assertEquals("[{\"first name\":\"a\", \"1st\":\"b\"}]", ds.toJson());
            assertEquals("\"first name\",\"1st\"\n\"a\",\"b\"", ds.toCsv());
        }
    }

    @Nested
    public class D07_MappedColumnNameIsValidatedUpFront extends TestBase {

        private final Dataset ds = ab(new Object[] { 1, 2 });

        @Test
        public void nullOrEmptyNewColumnName() {
            IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> ds.mapColumn("a", null, Arrays.asList("b"), v -> v));
            assertTrue(e.getMessage().contains("cannot be null or empty"), e.getMessage());
            e = assertThrows(IllegalArgumentException.class, () -> ds.mapColumn("a", "", "b", v -> v));
            assertTrue(e.getMessage().contains("cannot be null or empty"), e.getMessage());
            assertThrows(IllegalArgumentException.class, () -> ds.mapColumns(Tuple.of("a", "b"), "", Arrays.asList("a"), (x, y) -> x));
            assertThrows(IllegalArgumentException.class, () -> ds.mapColumns(Tuple.of("a", "b", "a"), null, Arrays.asList("a"), (x, y, z) -> x));
            assertThrows(IllegalArgumentException.class, () -> ds.mapColumns(Arrays.asList("a"), null, Arrays.asList("b"), r -> r.get(0)));
            assertThrows(IllegalArgumentException.class, () -> ds.flatMapColumn("a", "", "b", v -> Arrays.asList(v)));
            assertThrows(IllegalArgumentException.class, () -> ds.flatMapColumns(Tuple.of("a", "b"), null, Arrays.asList("a"), (x, y) -> Arrays.asList(x)));
            assertThrows(IllegalArgumentException.class,
                    () -> ds.flatMapColumns(Tuple.of("a", "b", "a"), "", Arrays.asList("a"), (x, y, z) -> Arrays.asList(x)));
            assertThrows(IllegalArgumentException.class, () -> ds.flatMapColumns(Arrays.asList("a"), null, Arrays.asList("b"), r -> Arrays.asList(r.get(0))));
        }

        @Test
        public void newColumnNameEqualToACopiedColumn() {
            final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> ds.mapColumn("a", "b", Arrays.asList("b"), v -> v));
            assertTrue(e.getMessage().contains("copied"), e.getMessage());
            assertThrows(IllegalArgumentException.class, () -> ds.flatMapColumn("a", "b", "b", v -> Arrays.asList(v)));
        }

        @Test
        public void newColumnMayReuseANonCopiedNameAndCopyingMayBeNullOrEmpty() {
            assertEquals(Arrays.asList("b", "a"), ds.mapColumn("a", "a", Arrays.asList("b"), v -> v).columnNames());
            assertEquals(Arrays.asList("x"), ds.mapColumn("a", "x", (Collection<String>) null, v -> v).columnNames());
            assertEquals(Arrays.asList("x"), ds.mapColumn("a", "x", Collections.<String> emptyList(), v -> v).columnNames());
            assertEquals(Arrays.asList("x"), ds.flatMapColumn("a", "x", (Collection<String>) null, v -> Arrays.asList(v)).columnNames());
        }
    }

    @Nested
    public class J07_ToMapRowTypeIsValidatedWithTheSharedMessage extends TestBase {

        @Test
        public void nullAndUnsupportedRowType() {
            final Dataset ds = Dataset.rows(Arrays.asList("id", "name"), new Object[][] { { 1, "a" } });

            IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> ds.toMap("id", Arrays.asList("name"), (Class<Object>) null));
            assertTrue(e.getMessage().contains("rowType"), e.getMessage());
            e = assertThrows(IllegalArgumentException.class, () -> ds.toMap("id", Arrays.asList("name"), String.class));
            assertTrue(e.getMessage().contains("Unsupported row type"), e.getMessage());
            e = assertThrows(IllegalArgumentException.class, () -> ds.toMultimap("id", Arrays.asList("name"), (Class<Object>) null));
            assertTrue(e.getMessage().contains("rowType"), e.getMessage());
            assertThrows(IllegalArgumentException.class, () -> ds.toMultimap("id", Arrays.asList("name"), Integer.class));
        }
    }

    /** Every claim rewritten in the javadoc this pass, executed. All of these pass on the pre-fix classes too. */
    @Nested
    public class DocClaims extends TestBase {

        @Test
        public void narrowingExamplesInTheGetterJavadocs() {
            final Dataset ds = Dataset.rows(Arrays.asList("i", "d", "big", "l"),
                    new Object[][] { { 300, 1.0e30, BigInteger.TWO.pow(64).add(BigInteger.valueOf(5)), 70000 } });
            ds.moveToRow(0);

            assertEquals((byte) 44, ds.getByte("i"));
            assertEquals((byte) -1, ds.getByte("d"));
            assertEquals((short) 4464, ds.getShort("l"));
            assertEquals((short) -1, ds.getShort("d"));
            assertEquals(5L, ds.getLong("big"));
            assertEquals(Long.MAX_VALUE, ds.getLong("d"));
            assertEquals(Integer.MAX_VALUE, ds.getInt("d"));
        }

        @Test
        public void rightJoinWithCollectionSupplierExample() {
            final Dataset left = Dataset.rows(Arrays.asList("id", "name"), new Object[][] { { 1, "Alice" }, { 1, "Alicia" } });
            final Dataset right = Dataset.rows(Arrays.asList("id", "score"), new Object[][] { { 1, 95 }, { 1, 88 }, { 2, 85 } });

            final Dataset joined = left.rightJoin(right, CommonUtil.asMap("id", "id"), "scores", Map.class, size -> new ArrayList<>(size));

            assertEquals(3, joined.size());
            assertEquals(Arrays.asList(1, 1, null), joined.getColumn("id"));
            assertEquals(Arrays.asList("Alice", "Alicia", null), joined.getColumn("name"));
            assertEquals(Arrays.asList(CommonUtil.asMap("id", 1, "score", 95), CommonUtil.asMap("id", 1, "score", 88)), joined.get(0, 2));
            assertEquals((Object) joined.get(0, 2), (Object) joined.get(1, 2));
            assertEquals(Arrays.asList(CommonUtil.asMap("id", 2, "score", 85)), joined.get(2, 2));
        }

        @Test
        public void groupByWithKeyExtractorKeepsTheFirstOriginalValue() {
            final Dataset ds = Dataset.rows(Arrays.asList("dept", "n"), new Object[][] { { "SALES", 1 }, { "sales", 2 }, { "it", 3 } });

            final Dataset g = ds.groupBy("dept", k -> ((String) k).toLowerCase(), "n", "total", Collectors.summingInt(o -> ((Number) o).intValue()));

            assertEquals(Arrays.asList("SALES", "it"), g.getColumn("dept"));
            assertEquals(Arrays.asList(3, 3), g.getColumn("total"));
        }

        @Test
        public void cubeKeyExtractorSeesADifferentWidthPerLevel() {
            final Dataset ds = Dataset.rows(Arrays.asList("r", "c", "v"), new Object[][] { { "x", "A", 1 }, { "y", "B", 2 } });
            final List<Integer> widths = new ArrayList<>();

            final List<Dataset> levels = ds.cube(Arrays.asList("r", "c"), row -> {
                widths.add(row.length());
                return row.join("-");
            }).toList();

            assertEquals(4, levels.size());
            assertTrue(widths.contains(2) && widths.contains(1), widths.toString());
        }

        @Test
        public void sortByPutsNullFirst() {
            final Dataset ds = Dataset.rows(Arrays.asList("v"), new Object[][] { { 2 }, { null }, { 1 } });
            ds.sortBy("v");
            assertEquals(Arrays.asList(null, 1, 2), ds.getColumn("v"));

            final Dataset ds2 = Dataset.rows(Arrays.asList("v"), new Object[][] { { 2 }, { null }, { 1 } });
            ds2.parallelSortBy(Arrays.asList("v"));
            assertEquals(Arrays.asList(null, 1, 2), ds2.getColumn("v"));
        }

        @Test
        public void cartesianProductUnchangedAfterHoistingTheRightColumns() {
            final Dataset a = Dataset.rows(Arrays.asList("id", "name"), new Object[][] { { 1, "Alice" }, { 2, "Bob" } });
            final Dataset b = Dataset.rows(Arrays.asList("category", "score"), new Object[][] { { "A", 95 }, { "B", 90 } });

            final Dataset p = a.cartesianProduct(b);

            assertEquals(Arrays.asList("id", "name", "category", "score"), p.columnNames());
            assertEquals(4, p.size());
            assertArrayEquals(new Object[] { 1, "Alice", "A", 95 }, p.getRow(0).toArray());
            assertArrayEquals(new Object[] { 1, "Alice", "B", 90 }, p.getRow(1).toArray());
            assertArrayEquals(new Object[] { 2, "Bob", "A", 95 }, p.getRow(2).toArray());
            assertArrayEquals(new Object[] { 2, "Bob", "B", 90 }, p.getRow(3).toArray());
        }
    }
}
