package com.landawn.abacus.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Builder.DatasetBuilder;
import com.landawn.abacus.util.NoCachingNoUpdating.DisposableObjArray;
import com.landawn.abacus.util.Tuple.Tuple2;
import com.landawn.abacus.util.Tuple.Tuple3;
import com.landawn.abacus.util.function.IntBiObjFunction;
import com.landawn.abacus.util.function.TriFunction;

@Tag("new-test")
public class Builder103Test extends TestBase {
    private Dataset dataset;
    private DatasetBuilder builder;

    @BeforeEach
    public void setUp() {
        List<String> columnNames = Arrays.asList("name", "age", "salary", "department");
        List<List<Object>> data = new ArrayList<>();
        data.add(Arrays.asList("John", 30, 50000.0, "IT"));
        data.add(Arrays.asList("Jane", 25, 45000.0, "HR"));
        data.add(Arrays.asList("Bob", 35, 60000.0, "IT"));

        dataset = Dataset.rows(columnNames, data);
        builder = new DatasetBuilder(dataset);
    }

    @Test
    public void testRenameColumn() {
        DatasetBuilder result = builder.renameColumn("name", "employee_name");

        Assertions.assertSame(builder, result);
        Assertions.assertTrue(dataset.columnNames().contains("employee_name"));
        Assertions.assertFalse(dataset.columnNames().contains("name"));
    }

    @Test
    public void testRenameColumnsWithMap() {
        Map<String, String> oldNewNames = new HashMap<>();
        oldNewNames.put("name", "employee_name");
        oldNewNames.put("age", "employee_age");

        DatasetBuilder result = builder.renameColumns(oldNewNames);

        Assertions.assertSame(builder, result);
        Assertions.assertTrue(dataset.columnNames().contains("employee_name"));
        Assertions.assertTrue(dataset.columnNames().contains("employee_age"));
        Assertions.assertFalse(dataset.columnNames().contains("name"));
        Assertions.assertFalse(dataset.columnNames().contains("age"));
    }

    @Test
    public void testRenameColumnsWithCollectionAndFunction() {
        Collection<String> columnNames = Arrays.asList("name", "department");
        Function<String, String> func = name -> name.toUpperCase();

        DatasetBuilder result = builder.renameColumns(columnNames, func);

        Assertions.assertSame(builder, result);
        Assertions.assertTrue(dataset.columnNames().contains("NAME"));
        Assertions.assertTrue(dataset.columnNames().contains("DEPARTMENT"));
        Assertions.assertTrue(dataset.columnNames().contains("age"));
        Assertions.assertTrue(dataset.columnNames().contains("salary"));
    }

    @Test
    public void testRenameColumnsWithFunction() {
        Function<String, String> func = name -> "prefix_" + name;

        DatasetBuilder result = builder.renameColumns(func);

        Assertions.assertSame(builder, result);
        Assertions.assertTrue(dataset.columnNames().contains("prefix_name"));
        Assertions.assertTrue(dataset.columnNames().contains("prefix_age"));
        Assertions.assertTrue(dataset.columnNames().contains("prefix_salary"));
        Assertions.assertTrue(dataset.columnNames().contains("prefix_department"));
    }

    @Test
    public void testAddColumnWithList() {
        List<String> cities = Arrays.asList("New York", "Boston", "Chicago");

        DatasetBuilder result = builder.addColumn("city", cities);

        Assertions.assertSame(builder, result);
        Assertions.assertTrue(dataset.columnNames().contains("city"));
        Assertions.assertEquals(5, dataset.columnCount());
        Assertions.assertEquals("New York", dataset.getColumn("city").get(0));
    }

    @Test
    public void testAddColumnAtIndex() {
        List<String> cities = Arrays.asList("New York", "Boston", "Chicago");

        DatasetBuilder result = builder.addColumn(1, "city", cities);

        Assertions.assertSame(builder, result);
        Assertions.assertTrue(dataset.columnNames().contains("city"));
        Assertions.assertEquals(1, dataset.getColumnIndex("city"));
        Assertions.assertEquals(5, dataset.columnCount());
    }

    @Test
    public void testAddColumnWithFunctionFromSingleColumn() {
        Function<Integer, Integer> func = age -> age * 2;

        DatasetBuilder result = builder.addColumn("double_age", "age", func);

        Assertions.assertSame(builder, result);
        Assertions.assertTrue(dataset.columnNames().contains("double_age"));
        Assertions.assertEquals(60, dataset.getColumn("double_age").get(0));
        Assertions.assertEquals(50, dataset.getColumn("double_age").get(1));
    }

    @Test
    public void testAddColumnAtIndexWithFunctionFromSingleColumn() {
        Function<Double, Double> func = salary -> salary * 1.1;

        DatasetBuilder result = builder.addColumn(2, "new_salary", "salary", func);

        Assertions.assertSame(builder, result);
        Assertions.assertTrue(dataset.columnNames().contains("new_salary"));
        Assertions.assertEquals(2, dataset.getColumnIndex("new_salary"));
        Assertions.assertEquals(55000.0, (Double) dataset.getColumn("new_salary").get(0), 0.000001d);
    }

    @Test
    public void testAddColumnWithFunctionFromMultipleColumns() {
        Collection<String> fromColumns = Arrays.asList("name", "department");
        Function<DisposableObjArray, String> func = arr -> arr.get(0) + " - " + arr.get(1);

        DatasetBuilder result = builder.addColumn("name_dept", fromColumns, func);

        Assertions.assertSame(builder, result);
        Assertions.assertTrue(dataset.columnNames().contains("name_dept"));
        Assertions.assertEquals("John - IT", dataset.getColumn("name_dept").get(0));
    }

    @Test
    public void testAddColumnAtIndexWithFunctionFromMultipleColumns() {
        Collection<String> fromColumns = Arrays.asList("age", "salary");
        Function<DisposableObjArray, Double> func = arr -> ((Integer) arr.get(0)) * ((Double) arr.get(1)) / 1000;

        DatasetBuilder result = builder.addColumn(3, "age_salary_factor", fromColumns, func);

        Assertions.assertSame(builder, result);
        Assertions.assertTrue(dataset.columnNames().contains("age_salary_factor"));
        Assertions.assertEquals(3, dataset.getColumnIndex("age_salary_factor"));
    }

    @Test
    public void testAddColumnWithBiFunction() {
        Tuple2<String, String> fromColumns = Tuple.of("name", "department");
        BiFunction<String, String, String> func = (name, dept) -> name + "@" + dept;

        DatasetBuilder result = builder.addColumn("email", fromColumns, func);

        Assertions.assertSame(builder, result);
        Assertions.assertTrue(dataset.columnNames().contains("email"));
        Assertions.assertEquals("John@IT", dataset.getColumn("email").get(0));
    }

    @Test
    public void testAddColumnAtIndexWithBiFunction() {
        Tuple2<String, String> fromColumns = Tuple.of("age", "salary");
        BiFunction<Integer, Double, Double> func = (age, salary) -> salary / age;

        DatasetBuilder result = builder.addColumn(4, "salary_per_year", fromColumns, func);

        Assertions.assertSame(builder, result);
        Assertions.assertTrue(dataset.columnNames().contains("salary_per_year"));
        Assertions.assertEquals(4, dataset.getColumnIndex("salary_per_year"));
    }

    @Test
    public void testAddColumnWithTriFunction() {
        builder.addColumn("bonus", Arrays.asList(5000.0, 3000.0, 7000.0));

        Tuple3<String, String, String> fromColumns = Tuple.of("name", "salary", "bonus");
        TriFunction<String, Double, Double, String> func = (name, salary, bonus) -> name + " total: " + (salary + bonus);

        DatasetBuilder result = builder.addColumn("summary", fromColumns, func);

        Assertions.assertSame(builder, result);
        Assertions.assertTrue(dataset.columnNames().contains("summary"));
        Assertions.assertEquals("John total: 55000.0", dataset.getColumn("summary").get(0));
    }

    @Test
    public void testAddColumnAtIndexWithTriFunction() {
        builder.addColumn("bonus", Arrays.asList(5000.0, 3000.0, 7000.0));

        Tuple3<String, String, String> fromColumns = Tuple.of("age", "salary", "bonus");
        TriFunction<Integer, Double, Double, Double> func = (age, salary, bonus) -> (salary + bonus) / age;

        DatasetBuilder result = builder.addColumn(2, "total_per_year", fromColumns, func);

        Assertions.assertSame(builder, result);
        Assertions.assertTrue(dataset.columnNames().contains("total_per_year"));
        Assertions.assertEquals(2, dataset.getColumnIndex("total_per_year"));
    }

    @Test
    public void testRemoveColumn() {
        DatasetBuilder result = builder.removeColumn("department");

        Assertions.assertSame(builder, result);
        Assertions.assertFalse(dataset.columnNames().contains("department"));
        Assertions.assertEquals(3, dataset.columnCount());
    }

    @Test
    public void testRemoveColumns() {
        Collection<String> columnsToRemove = Arrays.asList("age", "department");

        DatasetBuilder result = builder.removeColumns(columnsToRemove);

        Assertions.assertSame(builder, result);
        Assertions.assertFalse(dataset.columnNames().contains("age"));
        Assertions.assertFalse(dataset.columnNames().contains("department"));
        Assertions.assertEquals(2, dataset.columnCount());
    }

    @Test
    public void testRemoveColumnsWithPredicate() {
        Predicate<String> filter = name -> name.length() > 4;

        DatasetBuilder result = builder.removeColumns(filter);

        Assertions.assertSame(builder, result);
        Assertions.assertFalse(dataset.columnNames().contains("salary"));
        Assertions.assertFalse(dataset.columnNames().contains("department"));
        Assertions.assertTrue(dataset.columnNames().contains("name"));
        Assertions.assertTrue(dataset.columnNames().contains("age"));
    }

    @Test
    public void testUpdateColumn() {
        Function<Integer, Integer> func = age -> age + 5;

        DatasetBuilder result = builder.updateColumn("age", func);

        Assertions.assertSame(builder, result);
        Assertions.assertEquals(35, dataset.getColumn("age").get(0));
        Assertions.assertEquals(30, dataset.getColumn("age").get(1));
        Assertions.assertEquals(40, dataset.getColumn("age").get(2));
    }

    @Test
    public void testUpdateColumns() {
        Collection<String> columnsToUpdate = Arrays.asList("age", "salary");
        IntBiObjFunction<String, Number, Number> func = (i, c, v) -> {
            if (v instanceof Integer) {
                return ((Integer) v) * 2;
            } else if (v instanceof Double) {
                return ((Double) v) * 1.5;
            }
            return v;
        };

        DatasetBuilder result = builder.updateColumns(columnsToUpdate, func);

        Assertions.assertSame(builder, result);
        Assertions.assertEquals(60, dataset.getColumn("age").get(0));
        Assertions.assertEquals(75000.0, dataset.getColumn("salary").get(0));
    }

    @Test
    public void testConvertColumn() {
        DatasetBuilder result = builder.convertColumn("age", String.class);

        Assertions.assertSame(builder, result);
        Assertions.assertEquals("30", dataset.getColumn("age").get(0));
        Assertions.assertTrue(dataset.getColumn("age").get(0) instanceof String);
    }

    @Test
    public void testConvertColumns() {
        Map<String, Class<?>> columnTargetTypes = new HashMap<>();
        columnTargetTypes.put("age", String.class);
        columnTargetTypes.put("salary", String.class);

        DatasetBuilder result = builder.convertColumns(columnTargetTypes);

        Assertions.assertSame(builder, result);
        Assertions.assertTrue(dataset.getColumn("age").get(0) instanceof String);
        Assertions.assertTrue(dataset.getColumn("salary").get(0) instanceof String);
    }

    @Test
    public void testCombineColumnsWithClass() {
        Collection<String> columnsToCombine = Arrays.asList("name", "department");

        DatasetBuilder result = builder.combineColumns(columnsToCombine, "combined", a -> a.join(", "));

        Assertions.assertSame(builder, result);
        Assertions.assertTrue(dataset.columnNames().contains("combined"));
        Assertions.assertFalse(dataset.columnNames().contains("name"));
        Assertions.assertFalse(dataset.columnNames().contains("department"));
    }

    @Test
    public void testCombineColumnsWithFunction() {
        Collection<String> columnsToCombine = Arrays.asList("name", "age");
        Function<DisposableObjArray, String> combineFunc = arr -> arr.get(0) + " (age: " + arr.get(1) + ")";

        DatasetBuilder result = builder.combineColumns(columnsToCombine, "person_info", combineFunc);

        Assertions.assertSame(builder, result);
        Assertions.assertTrue(dataset.columnNames().contains("person_info"));
        Assertions.assertEquals("John (age: 30)", dataset.getColumn("person_info").get(0));
    }

    @Test
    public void testCombineColumnsWithBiFunction() {
        Tuple2<String, String> columnsToCombine = Tuple.of("name", "department");
        BiFunction<String, String, String> combineFunc = (name, dept) -> name + " in " + dept;

        DatasetBuilder result = builder.combineColumns(columnsToCombine, "name_in_dept", combineFunc);

        Assertions.assertSame(builder, result);
        Assertions.assertTrue(dataset.columnNames().contains("name_in_dept"));
        Assertions.assertEquals("John in IT", dataset.getColumn("name_in_dept").get(0));
    }

    @Test
    public void testCombineColumnsWithTriFunction() {
        builder.addColumn("bonus", Arrays.asList(5000.0, 3000.0, 7000.0));

        Tuple3<String, String, String> columnsToCombine = Tuple.of("name", "salary", "bonus");
        TriFunction<String, Double, Double, String> combineFunc = (name, salary, bonus) -> name + ": $" + (salary + bonus);

        DatasetBuilder result = builder.combineColumns(columnsToCombine, "total_comp", combineFunc);

        Assertions.assertSame(builder, result);
        Assertions.assertTrue(dataset.columnNames().contains("total_comp"));
        Assertions.assertEquals("John: $55000.0", dataset.getColumn("total_comp").get(0));
    }

    @Test
    public void testDivideColumnWithFunction() {
        builder.addColumn("full_name", Arrays.asList("John Doe", "Jane Smith", "Bob Johnson"));

        Collection<String> newColumns = Arrays.asList("first_name", "last_name");
        Function<String, List<String>> divideFunc = fullName -> Arrays.asList(fullName.split(" "));

        DatasetBuilder result = builder.divideColumn("full_name", newColumns, divideFunc);

        Assertions.assertSame(builder, result);
        Assertions.assertTrue(dataset.columnNames().contains("first_name"));
        Assertions.assertTrue(dataset.columnNames().contains("last_name"));
        Assertions.assertFalse(dataset.columnNames().contains("full_name"));
        Assertions.assertEquals("John", dataset.getColumn("first_name").get(0));
        Assertions.assertEquals("Doe", dataset.getColumn("last_name").get(0));
    }

    @Test
    public void testDivideColumnWithBiConsumer() {
        builder.addColumn("coordinates", Arrays.asList("10.5,20.3", "15.7,25.1", "30.2,40.8"));

        Collection<String> newColumns = Arrays.asList("x", "y");
        BiConsumer<String, Object[]> output = (coords, arr) -> {
            String[] parts = coords.split(",");
            arr[0] = Double.parseDouble(parts[0]);
            arr[1] = Double.parseDouble(parts[1]);
        };

        DatasetBuilder result = builder.divideColumn("coordinates", newColumns, output);

        Assertions.assertSame(builder, result);
        Assertions.assertTrue(dataset.columnNames().contains("x"));
        Assertions.assertTrue(dataset.columnNames().contains("y"));
        Assertions.assertEquals(10.5, dataset.getColumn("x").get(0));
        Assertions.assertEquals(20.3, dataset.getColumn("y").get(0));
    }

    @Test
    public void testDivideColumnIntoPair() {
        builder.addColumn("key_value", Arrays.asList("name:John", "age:30", "city:NYC"));

        Tuple2<String, String> newColumns = Tuple.of("key", "value");
        BiConsumer<String, Pair<Object, Object>> output = (kv, pair) -> {
            String[] parts = kv.split(":");
            pair.setLeft(parts[0]);
            pair.setRight(parts[1]);
        };

        DatasetBuilder result = builder.divideColumn("key_value", newColumns, output);

        Assertions.assertSame(builder, result);
        Assertions.assertTrue(dataset.columnNames().contains("key"));
        Assertions.assertTrue(dataset.columnNames().contains("value"));
        Assertions.assertEquals("name", dataset.getColumn("key").get(0));
        Assertions.assertEquals("John", dataset.getColumn("value").get(0));
    }

    @Test
    public void testDivideColumnIntoTriple() {
        builder.addColumn("rgb", Arrays.asList("255,0,0", "0,255,0", "0,0,255"));

        Tuple3<String, String, String> newColumns = Tuple.of("red", "green", "blue");
        BiConsumer<String, Triple<Object, Object, Object>> output = (rgb, triple) -> {
            String[] parts = rgb.split(",");
            triple.setLeft(Integer.parseInt(parts[0]));
            triple.setMiddle(Integer.parseInt(parts[1]));
            triple.setRight(Integer.parseInt(parts[2]));
        };

        DatasetBuilder result = builder.divideColumn("rgb", newColumns, output);

        Assertions.assertSame(builder, result);
        Assertions.assertTrue(dataset.columnNames().contains("red"));
        Assertions.assertTrue(dataset.columnNames().contains("green"));
        Assertions.assertTrue(dataset.columnNames().contains("blue"));
        Assertions.assertEquals(255, dataset.getColumn("red").get(0));
        Assertions.assertEquals(0, dataset.getColumn("green").get(0));
        Assertions.assertEquals(0, dataset.getColumn("blue").get(0));
    }

    @Test
    public void testUpdateAll() {
        Function<Object, Object> func = value -> {
            if (value == null)
                return "N/A";
            if (value instanceof String)
                return ((String) value).toUpperCase();
            return value;
        };

        DatasetBuilder result = builder.updateAll(func);

        Assertions.assertSame(builder, result);
        Assertions.assertEquals("JOHN", dataset.getColumn("name").get(0));
        Assertions.assertEquals("IT", dataset.getColumn("department").get(0));
        Assertions.assertEquals(30, dataset.getColumn("age").get(0));
    }

    @Test
    public void testReplaceIf() {
        List<String> columnWithNulls = Arrays.asList("Active", null, "Active");
        builder.addColumn("status", columnWithNulls);

        Predicate<Object> predicate = value -> value == null;

        DatasetBuilder result = builder.replaceIf(predicate, "Inactive");

        Assertions.assertSame(builder, result);
        Assertions.assertEquals("Active", dataset.getColumn("status").get(0));
        Assertions.assertEquals("Inactive", dataset.getColumn("status").get(1));
        Assertions.assertEquals("Active", dataset.getColumn("status").get(2));
    }

    @Test
    public void testPrepend() {
        List<String> columnNames = Arrays.asList("name", "age", "salary", "department");
        List<List<Object>> newData = new ArrayList<>();
        newData.add(Arrays.asList("Alice", 28, 55000.0, "Finance"));

        Dataset otherDataset = Dataset.rows(columnNames, newData);

        DatasetBuilder result = builder.prepend(otherDataset);

        Assertions.assertSame(builder, result);
        Assertions.assertEquals(4, dataset.size());
        Assertions.assertEquals("Alice", dataset.getColumn("name").get(0));
        Assertions.assertEquals("John", dataset.getColumn("name").get(1));
    }

    @Test
    public void testAppend() {
        List<String> columnNames = Arrays.asList("name", "age", "salary", "department");
        List<List<Object>> newData = new ArrayList<>();
        newData.add(Arrays.asList("Charlie", 40, 70000.0, "Sales"));

        Dataset otherDataset = Dataset.rows(columnNames, newData);

        DatasetBuilder result = builder.append(otherDataset);

        Assertions.assertSame(builder, result);
        Assertions.assertEquals(4, dataset.size());
        Assertions.assertEquals("Bob", dataset.getColumn("name").get(2));
        Assertions.assertEquals("Charlie", dataset.getColumn("name").get(3));
    }

    @Test
    public void testMethodChaining() {
        DatasetBuilder result = builder.renameColumn("name", "employee")
                .addColumn("bonus", Arrays.asList(5000.0, 3000.0, 7000.0))
                .updateColumn("age", (Integer age) -> age + 1)
                .removeColumn("department");

        Assertions.assertSame(builder, result);
        Assertions.assertTrue(dataset.columnNames().contains("employee"));
        Assertions.assertTrue(dataset.columnNames().contains("bonus"));
        Assertions.assertFalse(dataset.columnNames().contains("department"));
        Assertions.assertEquals(31, dataset.getColumn("age").get(0));
    }

    @Test
    public void testComplexScenario() {
        builder.addColumn("full_name", Arrays.asList("name", "department"), arr -> arr.get(0) + " from " + arr.get(1))
                .removeColumns(Arrays.asList("name", "department"))
                .updateColumn("salary", (Double s) -> s * 1.15)
                .addColumn("category", "age", (Integer age) -> age < 30 ? "Junior" : "Senior")
                .renameColumn("full_name", "employee_info");

        Assertions.assertTrue(dataset.columnNames().contains("employee_info"));
        Assertions.assertTrue(dataset.columnNames().contains("category"));
        Assertions.assertFalse(dataset.columnNames().contains("name"));
        Assertions.assertFalse(dataset.columnNames().contains("department"));
        Assertions.assertEquals("John from IT", dataset.getColumn("employee_info").get(0));
        Assertions.assertEquals("Senior", dataset.getColumn("category").get(0));
        Assertions.assertEquals("Junior", dataset.getColumn("category").get(1));
        Assertions.assertEquals(57500.0, (Double) dataset.getColumn("salary").get(0), 0.000001d);
    }
}
