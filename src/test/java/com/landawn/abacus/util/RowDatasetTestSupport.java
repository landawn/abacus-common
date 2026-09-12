package com.landawn.abacus.util;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.NoCachingNoUpdating.DisposableObjArray;
import com.landawn.abacus.util.stream.ObjIteratorEx;
import com.landawn.abacus.util.stream.Stream;

public abstract class RowDatasetTestSupport extends TestBase {

    @SuppressWarnings("unchecked")
    protected static <T> ObjIteratorEx<T> iteratorEx(final Stream<T> stream) throws ReflectiveOperationException {
        final Method method = Stream.class.getDeclaredMethod("iteratorEx");
        method.setAccessible(true);
        return (ObjIteratorEx<T>) method.invoke(stream);
    }

    protected RowDataset dataset;
    protected RowDataset emptyDataset;
    protected Dataset ds1;
    protected Dataset ds2;
    protected Dataset emptyDs;
    protected List<String> columnNames;
    protected List<List<Object>> columnList;
    protected List<List<Object>> columns;

    @BeforeEach
    public void setUp() {
        columnNames = new ArrayList<>(Arrays.asList("id", "name", "age", "salary"));

        columnList = new ArrayList<>();
        columnList.add(new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5)));
        columnList.add(new ArrayList<>(Arrays.asList("Alice", "Bob", "Charlie", "Diana", "Eve")));
        columnList.add(new ArrayList<>(Arrays.asList(25, 30, 35, 28, 22)));
        columnList.add(new ArrayList<>(Arrays.asList(50000.0, 60000.0, 70000.0, 55000.0, 45000.0)));

        dataset = new RowDataset(columnNames, columnList);
        emptyDataset = new RowDataset(new ArrayList<>(), new ArrayList<>());
        columns = copyColumnList();

        List<String> leftColumnNames = CommonUtil.toList("id", "name", "age");
        List<List<Object>> leftColumns = new ArrayList<>();
        leftColumns.add(CommonUtil.toList(1, 2, 3));
        leftColumns.add(CommonUtil.toList("Alice", "Bob", "Charlie"));
        leftColumns.add(CommonUtil.toList(25, 30, 35));
        ds1 = new RowDataset(leftColumnNames, leftColumns);

        List<String> rightColumnNames = CommonUtil.toList("id", "city", "salary");
        List<List<Object>> rightColumns = new ArrayList<>();
        rightColumns.add(CommonUtil.toList(2, 3, 4));
        rightColumns.add(CommonUtil.toList("New York", "London", "Tokyo"));
        rightColumns.add(CommonUtil.toList(50000, 60000, 70000));
        ds2 = new RowDataset(rightColumnNames, rightColumns);

        List<String> emptyColumnNames = CommonUtil.toList("col1", "col2");
        List<List<Object>> emptyColumns = new ArrayList<>();
        emptyColumns.add(new ArrayList<>());
        emptyColumns.add(new ArrayList<>());
        emptyDs = new RowDataset(emptyColumnNames, emptyColumns);
    }

    protected List<List<Object>> copyColumnList() {
        List<List<Object>> copy = new ArrayList<>();
        for (List<Object> column : columnList) {
            copy.add(new ArrayList<>(column));
        }
        return copy;
    }

    protected RowDataset createFourRowCityDataset() {
        final List<String> localColumnNames = new ArrayList<>(Arrays.asList("id", "name", "age", "city"));
        final List<List<Object>> localColumns = new ArrayList<>();
        localColumns.add(new ArrayList<>(Arrays.asList(1, 2, 3, 4)));
        localColumns.add(new ArrayList<>(Arrays.asList("John", "Jane", "Bob", "Alice")));
        localColumns.add(new ArrayList<>(Arrays.asList(25, 30, 35, 28)));
        localColumns.add(new ArrayList<>(Arrays.asList("NYC", "LA", "Chicago", "Miami")));

        return new RowDataset(localColumnNames, localColumns);
    }

    protected RowDataset createFiveRowCityDataset() {
        final List<String> localColumnNames = new ArrayList<>(Arrays.asList("id", "name", "age", "city"));
        final List<List<Object>> localColumns = new ArrayList<>();
        localColumns.add(new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5)));
        localColumns.add(new ArrayList<>(Arrays.asList("John", "Jane", "Bob", "Alice", "Eve")));
        localColumns.add(new ArrayList<>(Arrays.asList(25, 30, 35, 40, 28)));
        localColumns.add(new ArrayList<>(Arrays.asList("NYC", "LA", "Chicago", "NYC", "LA")));

        return new RowDataset(localColumnNames, localColumns);
    }

    protected RowDataset createThreeRowScoreDataset() {
        final List<String> localColumnNames = new ArrayList<>(Arrays.asList("id", "name", "age", "score"));
        final List<List<Object>> localColumns = new ArrayList<>();
        localColumns.add(new ArrayList<>(Arrays.asList(1, 2, 3)));
        localColumns.add(new ArrayList<>(Arrays.asList("John", "Jane", "Bob")));
        localColumns.add(new ArrayList<>(Arrays.asList(25, 30, 35)));
        localColumns.add(new ArrayList<>(Arrays.asList(85.5, 90.0, 88.0)));

        return new RowDataset(localColumnNames, localColumns);
    }

    protected List<List<Object>> createThreeRowScoreColumns() {
        final List<List<Object>> localColumns = new ArrayList<>();
        localColumns.add(new ArrayList<>(Arrays.asList(1, 2, 3)));
        localColumns.add(new ArrayList<>(Arrays.asList("John", "Jane", "Bob")));
        localColumns.add(new ArrayList<>(Arrays.asList(25, 30, 35)));
        localColumns.add(new ArrayList<>(Arrays.asList(85.5, 90.0, 88.0)));

        return localColumns;
    }

    public static class Person {
        protected int id;
        protected String name;
        protected int age;
        protected String city;

        public Person() {
        }

        public Person(final int id, final String name, final int age, final String city) {
            this.id = id;
            this.name = name;
            this.age = age;
            this.city = city;
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

        public int getAge() {
            return age;
        }

        public void setAge(final int age) {
            this.age = age;
        }

        public String getCity() {
            return city;
        }

        public void setCity(final String city) {
            this.city = city;
        }
    }

    public static class PersonSkill {
        protected int id;
        protected String name;
        protected String skill;

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

        public String getSkill() {
            return skill;
        }

        public void setSkill(final String skill) {
            this.skill = skill;
        }
    }

    public static class TestBean {
        public int id;
        public String name;
        public int age;
        public double score;

        public TestBean() {
        }
    }

    public static class SalaryRowBean {
        protected int id;
        protected String name;
        protected int age;
        protected double salary;

        public SalaryRowBean() {
        }

        public SalaryRowBean(final int id, final String name, final int age, final double salary) {
            this.id = id;
            this.name = name;
            this.age = age;
            this.salary = salary;
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

        public int getAge() {
            return age;
        }

        public void setAge(final int age) {
            this.age = age;
        }

        public double getSalary() {
            return salary;
        }

        public void setSalary(final double salary) {
            this.salary = salary;
        }
    }

    // --- New tests for previously untested methods ---

    // --- regression tests for 2026-06-10 deep-review fixes ---

    // ==================================================================================================
    // Fixes from the 2026-08-31 Dataset/RowDataset/Sheet/Array review.
    // ==================================================================================================

    protected static Dataset twoColumnDataset() {
        return Dataset.rows(CommonUtil.asList("id", "name"), new Object[][] { { 1, "a" }, { 2, "b" }, { 3, "c" } });
    }

    // -------- B3: a positional row must match the column count exactly (no silent truncation) --------

    // -------- B5: both coordinates are validated; the cursor reports an empty Dataset clearly --------

    // -------- B6: modCount tracks structural change only --------

    // -------- B7: array cell values compare and hash by content --------

    // -------- B8: comparator key rows are not pooled arrays --------

    // -------- B2: slice is a frozen view with one uniform failure mode --------

    // -------- D1: cursor-free row accessor --------

    // -------- D2: semiJoin / antiJoin membership --------

    // -------- J2: the join reports a missing right-hand column against the right Dataset --------

    // -------- D6: the extracted row permutation handles multi-cycle orderings --------

    // -------- D4: documented view semantics for getColumn / getRow --------

    // -------- B6 (cont.): the single-column stream's bulk terminal operations --------

    // -------- Guard for the unchecked internal accessor used by the join/cartesian inner loops --------

    // -------- B7 (cont.): the equals/hashCode contract holds across cell shapes --------

    // ------------------------------------------------------------------------------------------------------
    // Regressions for the 2026-09-06 review of Dataset/RowDataset (C-009..C-014).
    // ------------------------------------------------------------------------------------------------------

    /**
     * The twelve {@code rollup}/{@code cube} overloads that take an explicit aggregate result column name - the
     * exact set carrying the repeated eager-validation and {@code modCount} guards. Returned unevaluated so a
     * caller can assert on the call itself.
     */
    protected static List<java.util.function.Supplier<Stream<Dataset>>> rollupAndCubeStreams(final Dataset ds, final String resultName) {
        final List<String> keys = CommonUtil.asList("a", "b");
        final List<String> aggregateOn = CommonUtil.asList("c");
        final java.util.function.Function<DisposableObjArray, Object> keyExtractor = row -> row.join("-");

        return CommonUtil.asList( //
                () -> ds.rollup(keys, "c", resultName, com.landawn.abacus.util.stream.Collectors.countingToInt()),
                () -> ds.rollup(keys, aggregateOn, resultName, Object[].class),
                () -> ds.rollup(keys, aggregateOn, resultName, com.landawn.abacus.util.stream.Collectors.toList()),
                () -> ds.rollup(keys, keyExtractor, "c", resultName, com.landawn.abacus.util.stream.Collectors.countingToInt()),
                () -> ds.rollup(keys, keyExtractor, aggregateOn, resultName, Object[].class),
                () -> ds.rollup(keys, keyExtractor, aggregateOn, resultName, com.landawn.abacus.util.stream.Collectors.toList()),
                () -> ds.cube(keys, "c", resultName, com.landawn.abacus.util.stream.Collectors.countingToInt()),
                () -> ds.cube(keys, aggregateOn, resultName, Object[].class),
                () -> ds.cube(keys, aggregateOn, resultName, com.landawn.abacus.util.stream.Collectors.toList()),
                () -> ds.cube(keys, keyExtractor, "c", resultName, com.landawn.abacus.util.stream.Collectors.countingToInt()),
                () -> ds.cube(keys, keyExtractor, aggregateOn, resultName, Object[].class),
                () -> ds.cube(keys, keyExtractor, aggregateOn, resultName, com.landawn.abacus.util.stream.Collectors.toList()));
    }

    public static class OnlyIdBean20260906 {
        protected int id;

        public int getId() {
            return id;
        }

        public void setId(final int id) {
            this.id = id;
        }
    }

    protected static Dataset newAbcDataset() {
        return Dataset.rows(CommonUtil.asList("a", "b", "c"), new Object[][] { { 1, 2, 3 } });
    }

    protected static Dataset salesForRollup() {
        return Dataset.rows(CommonUtil.asList("region", "product", "amount"),
                new Object[][] { { "North", "widget", 100 }, { "North", "gadget", 200 }, { "South", "widget", 300 } });
    }
}
