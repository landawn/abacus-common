package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.stream.Collectors;

/**
 * Regression tests for cycle 1 of the 2026-09-02 iterative review of {@code Dataset}, {@code RowDataset} and
 * {@code Sheet} (ledger {@code scripts/cross_review/Dataset_RowDataset_Sheet_ledger_2026-09-02.md}, C-035 .. C-040).
 *
 * <p>One nested class per finding. Unless its javadoc says it only pins already-correct behaviour, every test
 * here fails against the pre-fix classes.</p>
 */
public class DatasetSheetRegressionBTest extends TestBase {

    /** A bean with two properties, so that a column named {@code extra} has no matching property. */
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

    private static Dataset twoRows() {
        return Dataset.rows(Arrays.asList("a", "b"), new Object[][] { { 1, 2 }, { 3, 4 } });
    }

    @Nested
    public class C035_DivideColumnCollectionOverloadsRejectNullOrEmptyNames {

        private Dataset single() {
            return Dataset.rows(Arrays.asList("v"), new Object[][] { { "a-b" }, { "c-d" } });
        }

        @Test
        public void functionOverloadRejectsANullName() {
            final Dataset ds = single();
            final IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                    () -> ds.divideColumn("v", Arrays.asList("x", null), (final Object v) -> Arrays.asList(1, 2)));
            assertTrue(e.getMessage().contains("null or empty"), e.getMessage());
            assertEquals(Arrays.asList("v"), ds.columnNames()); // untouched
            assertFalse(ds.containsColumn(null));
        }

        @Test
        public void functionOverloadRejectsAnEmptyName() {
            final Dataset ds = single();
            assertThrows(IllegalArgumentException.class, () -> ds.divideColumn("v", Arrays.asList("", "y"), (final Object v) -> Arrays.asList(1, 2)));
            assertEquals(Arrays.asList("v"), ds.columnNames());
        }

        @Test
        public void biConsumerOverloadRejectsNullAndEmptyNames() {
            final Dataset ds = single();
            assertThrows(IllegalArgumentException.class,
                    () -> ds.divideColumn("v", Arrays.asList(null, "y"), (final Object v, final Object[] out) -> out[0] = v));
            assertThrows(IllegalArgumentException.class,
                    () -> ds.divideColumn("v", Arrays.asList("x", ""), (final Object v, final Object[] out) -> out[0] = v));
            assertEquals(Arrays.asList("v"), ds.columnNames());
        }

        /** Pins the previously working shape, including non-ASCII names. */
        @Test
        public void validNamesStillWork() {
            final Dataset ds = single();
            ds.divideColumn("v", Arrays.asList("左", "右"), (final Object v) -> Arrays.asList(((String) v).split("-")));
            assertEquals(Arrays.asList("左", "右"), ds.columnNames());
            assertEquals(Arrays.asList("a", "b"), ds.getRow(0));

            final Dataset ds2 = single();
            ds2.divideColumn("v", Arrays.asList("x", "y"), (final Object v, final Object[] out) -> {
                out[0] = ((String) v).charAt(0);
                out[1] = ((String) v).charAt(2);
            });
            assertEquals(Arrays.asList('c', 'd'), ds2.getRow(1));
        }

        @Test
        public void nullOrEmptyCollectionAndExistingOrDuplicateNamesAreStillRejected() {
            final Dataset ds = single();
            assertThrows(IllegalArgumentException.class, () -> ds.divideColumn("v", (Collection<String>) null, (final Object v) -> Arrays.asList(1)));
            assertThrows(IllegalArgumentException.class, () -> ds.divideColumn("v", Collections.<String> emptyList(), (final Object v) -> Arrays.asList(1)));
            assertThrows(IllegalArgumentException.class, () -> ds.divideColumn("v", Arrays.asList("v", "y"), (final Object v) -> Arrays.asList(1, 2)));
            assertThrows(IllegalArgumentException.class, () -> ds.divideColumn("v", Arrays.asList("x", "x"), (final Object v) -> Arrays.asList(1, 2)));
        }
    }

    @Nested
    public class C036_FilterToListRejectsAFilterThatMatchesNothing {

        @Test
        public void allFourOverloadsThrowIllegalArgumentNamingTheFilter() {
            final Dataset ds = twoRows();

            final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> ds.toList(0, 2, n -> false, Fn.identity(), Map.class));
            assertTrue(e.getMessage().contains("columnNameFilter"), e.getMessage());
            assertTrue(e.getMessage().contains("[a, b]"), e.getMessage());

            assertThrows(IllegalArgumentException.class, () -> ds.toList(n -> false, Fn.identity(), Map.class));
            assertThrows(IllegalArgumentException.class, () -> ds.toList(n -> false, Fn.identity(), size -> new Object[size]));
            assertThrows(IllegalArgumentException.class, () -> ds.toList(1, 2, n -> false, Fn.identity(), size -> new ArrayList<>(size)));
        }

        @Test
        public void aMatchingFilterStillConvertsTheSelectedColumnsOnly() {
            final Dataset ds = twoRows();

            final List<Map> rows = ds.toList(n -> "b".equals(n), n -> n.toUpperCase(), Map.class);
            assertEquals(2, rows.size());
            assertEquals(CommonUtil.asMap("B", 2), rows.get(0));

            final List<Object[]> arrays = ds.toList(1, 2, n -> true, Fn.identity(), size -> new Object[size]);
            assertEquals(1, arrays.size());
            assertEquals(Arrays.asList(3, 4), Arrays.asList(arrays.get(0)));
        }

        /** A zero-column Dataset has no columns for the filter to match; the empty selection is its whole column set. */
        @Test
        public void zeroColumnDatasetIsNotAnError() {
            final Dataset empty = CommonUtil.newEmptyDataset();
            assertEquals(Collections.emptyList(), empty.toList(n -> false, Fn.identity(), Map.class));
            assertEquals(Collections.emptyList(), empty.toList(0, 0, n -> true, Fn.identity(), size -> new ArrayList<>(size)));
        }

        /** Pins the documented lenient bean conversion through the filter overloads. */
        @Test
        public void beanConversionThroughTheFilterIsLenient() {
            final Dataset ds = Dataset.rows(Arrays.asList("id", "name", "extra"), new Object[][] { { 1, "a", "x" } });

            final List<Person> people = ds.toList(n -> true, Fn.identity(), Person.class);
            assertEquals("a", people.get(0).getName());
            assertEquals(1, people.get(0).getId());
        }
    }

    @Nested
    public class C037_ZeroColumnDatasetConvertsToAnEmptyBeanList {

        @Test
        public void toListAndToEntitiesReturnEmptyLikeTheOtherRowTypes() {
            final Dataset empty = CommonUtil.newEmptyDataset();

            assertEquals(Collections.emptyList(), empty.toList(Person.class));
            assertEquals(Collections.emptyList(), empty.toList(0, 0, Person.class));
            assertEquals(Collections.emptyList(), empty.toEntities(Collections.<String, String> emptyMap(), Person.class));
            assertEquals(Collections.emptyList(), empty.toList(Map.class));
            assertEquals(Collections.emptyList(), empty.toList());
            assertEquals(Collections.emptyList(), empty.stream(Person.class).toList());
        }

        @Test
        public void explicitEmptySelectionOnADatasetWithColumnsIsStillRejected() {
            final Dataset ds = twoRows();
            assertThrows(IllegalArgumentException.class, () -> ds.toList(Collections.<String> emptyList(), Person.class));
            assertThrows(IllegalArgumentException.class, () -> ds.toList((Collection<String>) null, Person.class));
            assertThrows(IllegalArgumentException.class, () -> ds.toEntities(Collections.<String> emptyList(), null, Person.class));
        }

        @Test
        public void toMergedEntitiesStillRequiresTheIdColumns() {
            final Dataset empty = CommonUtil.newEmptyDataset();
            assertThrows(IllegalArgumentException.class, () -> empty.toMergedEntities("id", Person.class));
        }
    }

    @Nested
    public class C038_GroupByValidatesTheResultColumnNameEagerly {

        private final Dataset ds = Dataset.rows(Arrays.asList("k", "k2", "v"), new Object[][] { { "a", 1, 10 }, { "a", 1, 20 }, { "b", 2, 30 } });

        private void assertNamesParameter(final org.junit.jupiter.api.function.Executable call) {
            final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, call);
            assertTrue(e.getMessage().contains("aggregateResultColumnName"), e.getMessage());
        }

        @Test
        public void singleKeyOverloads() {
            assertNamesParameter(() -> ds.groupBy("k", "v", null, Collectors.counting()));
            assertNamesParameter(() -> ds.groupBy("k", "v", "", Collectors.counting()));
            assertNamesParameter(() -> ds.groupBy("k", Arrays.asList("v"), null, Object[].class));
            assertNamesParameter(() -> ds.groupBy("k", Arrays.asList("v"), "", Collectors.toList()));
            assertNamesParameter(() -> ds.groupBy("k", Fn.identity(), "v", null, Collectors.counting()));
            assertNamesParameter(() -> ds.groupBy("k", Fn.identity(), Arrays.asList("v"), "", Object[].class));
            assertNamesParameter(() -> ds.groupBy("k", Fn.identity(), Arrays.asList("v"), null, Collectors.toList()));
            assertNamesParameter(() -> ds.groupBy("k", Fn.identity(), Arrays.asList("v"), "", r -> r.get(0), Collectors.toList()));
        }

        @Test
        public void multiKeyOverloads() {
            final List<String> keys = Arrays.asList("k", "k2");
            assertNamesParameter(() -> ds.groupBy(keys, "v", null, Collectors.counting()));
            assertNamesParameter(() -> ds.groupBy(keys, Arrays.asList("v"), "", Object[].class));
            assertNamesParameter(() -> ds.groupBy(keys, Arrays.asList("v"), null, Collectors.toList()));
            assertNamesParameter(() -> ds.groupBy(keys, Arrays.asList("v"), "", r -> r.get(0), Collectors.toList()));
            assertNamesParameter(() -> ds.groupBy(keys, r -> r.join("-"), "v", null, Collectors.counting()));
            assertNamesParameter(() -> ds.groupBy(keys, r -> r.join("-"), Arrays.asList("v"), "", Object[].class));
            assertNamesParameter(() -> ds.groupBy(keys, r -> r.join("-"), Arrays.asList("v"), null, Collectors.toList()));
            assertNamesParameter(() -> ds.groupBy(keys, r -> r.join("-"), Arrays.asList("v"), "", r -> r.get(0), Collectors.toList()));
        }

        /** Pins the results themselves and the collision rule the check must not disturb. */
        @Test
        public void validCallsAndCollisionsAreUnchanged() {
            final Dataset g = ds.groupBy("k", "v", "total", Collectors.summingInt(o -> ((Number) o).intValue()));
            assertEquals(Arrays.asList("k", "total"), g.columnNames());
            assertEquals(Arrays.asList(30, 30), g.getColumn("total"));

            final Dataset g2 = ds.groupBy(Arrays.asList("k", "k2"), r -> r.join("-"), "v", "cnt", Collectors.counting());
            assertEquals(Arrays.asList(2L, 1L), g2.getColumn("cnt"));

            assertThrows(IllegalArgumentException.class, () -> ds.groupBy("k", "v", "k", Collectors.counting()));
            assertThrows(IllegalArgumentException.class, () -> ds.groupBy(Arrays.asList("k", "k2"), "v", "k2", Collectors.counting()));
        }
    }

    /** Pins that the Sheet constructors and factories still deliver working key maps after the redundant rebuilds were removed. */
    @Nested
    public class C040_SheetFactoriesAndCopiesKeepWorkingKeyMaps {

        @Test
        public void everyConstructionPathResolvesKeys() {
            final Sheet<String, String, Integer> viaRows = Sheet.rows(Arrays.asList("r1", "r2"), Arrays.asList("c1"), new Integer[][] { { 1 }, { 2 } });
            assertEquals(Integer.valueOf(2), viaRows.get("r2", "c1"));

            final Sheet<String, String, Integer> viaRowLists = Sheet.rows(Arrays.asList("r1"), Arrays.asList("c1", "c2"), Arrays.asList(Arrays.asList(1, 2)));
            assertEquals(Integer.valueOf(2), viaRowLists.get("r1", "c2"));

            final Sheet<String, String, Integer> viaColumns = Sheet.columns(Arrays.asList("r1", "r2"), Arrays.asList("c1"), new Integer[][] { { 1, 2 } });
            assertEquals(Integer.valueOf(2), viaColumns.get("r2", "c1"));

            final Sheet<String, String, Integer> viaColumnLists = Sheet.columns(Arrays.asList("r1"), Arrays.asList("c1", "c2"),
                    Arrays.asList(Arrays.asList(1), Arrays.asList(2)));
            assertEquals(Integer.valueOf(2), viaColumnLists.get("r1", "c2"));

            final Sheet<String, String, Integer> copied = viaRows.copy();
            copied.addRow("r3", Arrays.asList(3));
            assertEquals(Integer.valueOf(3), copied.get("r3", "c1"));
            assertEquals(Arrays.asList(1, 2), viaRows.columnValues("c1"));

            final Sheet<String, String, Integer> sub = viaRows.copy(Arrays.asList("r2"), Arrays.asList("c1"));
            assertEquals(Integer.valueOf(2), sub.get("r2", "c1"));
            assertThrows(IllegalArgumentException.class, () -> sub.get("r1", "c1"));

            final Sheet<String, String, Integer> transposed = viaRows.transposed();
            assertEquals(Integer.valueOf(2), transposed.get("c1", "r2"));
            transposed.addColumn("r3", Arrays.asList(3));
            assertEquals(Integer.valueOf(3), transposed.get("c1", "r3"));
        }
    }

    /** Documentation claims added this cycle, executed. All pass on the pre-fix classes too. */
    @Nested
    public class DocClaims {

        /** C-039: keys compare with Objects.equals semantics, so null matches null (unlike SQL). */
        @Test
        public void nullKeysMatchNullKeysInJoinsSetOperationsAndGroupBy() {
            final Dataset left = Dataset.rows(Arrays.asList("k", "l"), new Object[][] { { null, "L1" }, { 1, "L2" } });
            final Dataset right = Dataset.rows(Arrays.asList("k", "r"), new Object[][] { { null, "R1" }, { 1, "R2" }, { null, "R3" } });

            assertEquals(3, left.innerJoin(right, "k", "k").size());
            assertEquals(3, left.leftJoin(right, "k", "k").size());
            assertEquals(3, left.rightJoin(right, "k", "k").size());
            assertEquals(3, left.fullJoin(right, "k", "k").size());

            final Dataset l2 = Dataset.rows(Arrays.asList("a", "b", "l"), new Object[][] { { null, 1, "L1" } });
            final Dataset r2 = Dataset.rows(Arrays.asList("a", "b", "r"), new Object[][] { { null, 1, "R1" } });
            assertEquals(1, l2.innerJoin(r2, CommonUtil.asMap("a", "a", "b", "b")).size());

            assertEquals(2, left.unionBy(right, Arrays.asList("k")).size());
            assertEquals(2, left.intersectBy(right, Arrays.asList("k")).size());
            assertEquals(0, left.exceptBy(right, Arrays.asList("k")).size());

            final Dataset grouped = right.groupBy("k", "r", "cnt", Collectors.counting());
            assertEquals(Arrays.asList(null, 1), grouped.getColumn("k"));
            assertEquals(Arrays.asList(2L, 1L), grouped.getColumn("cnt"));
        }
    }
}
