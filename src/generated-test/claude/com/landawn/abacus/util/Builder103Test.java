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
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Builder.DataSetBuilder;
import com.landawn.abacus.util.NoCachingNoUpdating.DisposableObjArray;
import com.landawn.abacus.util.Tuple.Tuple2;
import com.landawn.abacus.util.Tuple.Tuple3;
import com.landawn.abacus.util.function.TriFunction;

public class Builder103Test extends TestBase {
    private DataSet dataSet;
    private DataSetBuilder builder;

    @BeforeEach
    public void setUp() {
        // Create a sample DataSet with test data
        List<String> columnNames = Arrays.asList("name", "age", "salary", "department");
        List<List<Object>> data = new ArrayList<>();
        data.add(Arrays.asList("John", 30, 50000.0, "IT"));
        data.add(Arrays.asList("Jane", 25, 45000.0, "HR"));
        data.add(Arrays.asList("Bob", 35, 60000.0, "IT"));

        dataSet = DataSet.rows(columnNames, data);
        builder = new DataSetBuilder(dataSet);
    }

    @Test
    public void testRenameColumn() {
        // Test renaming a single column
        DataSetBuilder result = builder.renameColumn("name", "employee_name");

        Assertions.assertSame(builder, result);
        Assertions.assertTrue(dataSet.columnNameList().contains("employee_name"));
        Assertions.assertFalse(dataSet.columnNameList().contains("name"));
    }

    @Test
    public void testRenameColumnsWithMap() {
        // Test renaming multiple columns with a map
        Map<String, String> oldNewNames = new HashMap<>();
        oldNewNames.put("name", "employee_name");
        oldNewNames.put("age", "employee_age");

        DataSetBuilder result = builder.renameColumns(oldNewNames);

        Assertions.assertSame(builder, result);
        Assertions.assertTrue(dataSet.columnNameList().contains("employee_name"));
        Assertions.assertTrue(dataSet.columnNameList().contains("employee_age"));
        Assertions.assertFalse(dataSet.columnNameList().contains("name"));
        Assertions.assertFalse(dataSet.columnNameList().contains("age"));
    }

    @Test
    public void testRenameColumnsWithCollectionAndFunction() {
        // Test renaming specified columns with a function
        Collection<String> columnNames = Arrays.asList("name", "department");
        Function<String, String> func = name -> name.toUpperCase();

        DataSetBuilder result = builder.renameColumns(columnNames, func);

        Assertions.assertSame(builder, result);
        Assertions.assertTrue(dataSet.columnNameList().contains("NAME"));
        Assertions.assertTrue(dataSet.columnNameList().contains("DEPARTMENT"));
        Assertions.assertTrue(dataSet.columnNameList().contains("age")); // unchanged
        Assertions.assertTrue(dataSet.columnNameList().contains("salary")); // unchanged
    }

    @Test
    public void testRenameColumnsWithFunction() {
        // Test renaming all columns with a function
        Function<String, String> func = name -> "prefix_" + name;

        DataSetBuilder result = builder.renameColumns(func);

        Assertions.assertSame(builder, result);
        Assertions.assertTrue(dataSet.columnNameList().contains("prefix_name"));
        Assertions.assertTrue(dataSet.columnNameList().contains("prefix_age"));
        Assertions.assertTrue(dataSet.columnNameList().contains("prefix_salary"));
        Assertions.assertTrue(dataSet.columnNameList().contains("prefix_department"));
    }

    @Test
    public void testAddColumnWithList() {
        // Test adding a column at the end
        List<String> cities = Arrays.asList("New York", "Boston", "Chicago");

        DataSetBuilder result = builder.addColumn("city", cities);

        Assertions.assertSame(builder, result);
        Assertions.assertTrue(dataSet.columnNameList().contains("city"));
        Assertions.assertEquals(5, dataSet.columnCount());
        Assertions.assertEquals("New York", dataSet.getColumn("city").get(0));
    }

    @Test
    public void testAddColumnAtIndex() {
        // Test adding a column at a specific index
        List<String> cities = Arrays.asList("New York", "Boston", "Chicago");

        DataSetBuilder result = builder.addColumn(1, "city", cities);

        Assertions.assertSame(builder, result);
        Assertions.assertTrue(dataSet.columnNameList().contains("city"));
        Assertions.assertEquals(1, dataSet.getColumnIndex("city"));
        Assertions.assertEquals(5, dataSet.columnCount());
    }

    @Test
    public void testAddColumnWithFunctionFromSingleColumn() {
        // Test adding a column by transforming another column
        Function<Integer, Integer> func = age -> age * 2;

        DataSetBuilder result = builder.addColumn("double_age", "age", func);

        Assertions.assertSame(builder, result);
        Assertions.assertTrue(dataSet.columnNameList().contains("double_age"));
        Assertions.assertEquals(60, dataSet.getColumn("double_age").get(0));
        Assertions.assertEquals(50, dataSet.getColumn("double_age").get(1));
    }

    @Test
    public void testAddColumnAtIndexWithFunctionFromSingleColumn() {
        // Test adding a column at index by transforming another column
        Function<Double, Double> func = salary -> salary * 1.1;

        DataSetBuilder result = builder.addColumn(2, "new_salary", "salary", func);

        Assertions.assertSame(builder, result);
        Assertions.assertTrue(dataSet.columnNameList().contains("new_salary"));
        Assertions.assertEquals(2, dataSet.getColumnIndex("new_salary"));
        Assertions.assertEquals(55000.0, (Double) dataSet.getColumn("new_salary").get(0), 0.000001d);
    }

    @Test
    public void testAddColumnWithFunctionFromMultipleColumns() {
        // Test adding a column by combining multiple columns
        Collection<String> fromColumns = Arrays.asList("name", "department");
        Function<DisposableObjArray, String> func = arr -> arr.get(0) + " - " + arr.get(1);

        DataSetBuilder result = builder.addColumn("name_dept", fromColumns, func);

        Assertions.assertSame(builder, result);
        Assertions.assertTrue(dataSet.columnNameList().contains("name_dept"));
        Assertions.assertEquals("John - IT", dataSet.getColumn("name_dept").get(0));
    }

    @Test
    public void testAddColumnAtIndexWithFunctionFromMultipleColumns() {
        // Test adding a column at index by combining multiple columns
        Collection<String> fromColumns = Arrays.asList("age", "salary");
        Function<DisposableObjArray, Double> func = arr -> ((Integer) arr.get(0)) * ((Double) arr.get(1)) / 1000;

        DataSetBuilder result = builder.addColumn(3, "age_salary_factor", fromColumns, func);

        Assertions.assertSame(builder, result);
        Assertions.assertTrue(dataSet.columnNameList().contains("age_salary_factor"));
        Assertions.assertEquals(3, dataSet.getColumnIndex("age_salary_factor"));
    }

    @Test
    public void testAddColumnWithBiFunction() {
        // Test adding a column using BiFunction with two columns
        Tuple2<String, String> fromColumns = Tuple.of("name", "department");
        BiFunction<String, String, String> func = (name, dept) -> name + "@" + dept;

        DataSetBuilder result = builder.addColumn("email", fromColumns, func);

        Assertions.assertSame(builder, result);
        Assertions.assertTrue(dataSet.columnNameList().contains("email"));
        Assertions.assertEquals("John@IT", dataSet.getColumn("email").get(0));
    }

    @Test
    public void testAddColumnAtIndexWithBiFunction() {
        // Test adding a column at index using BiFunction
        Tuple2<String, String> fromColumns = Tuple.of("age", "salary");
        BiFunction<Integer, Double, Double> func = (age, salary) -> salary / age;

        DataSetBuilder result = builder.addColumn(4, "salary_per_year", fromColumns, func);

        Assertions.assertSame(builder, result);
        Assertions.assertTrue(dataSet.columnNameList().contains("salary_per_year"));
        Assertions.assertEquals(4, dataSet.getColumnIndex("salary_per_year"));
    }

    @Test
    public void testAddColumnWithTriFunction() {
        // Test adding a column using TriFunction with three columns
        // First add a test column
        builder.addColumn("bonus", Arrays.asList(5000.0, 3000.0, 7000.0));

        Tuple3<String, String, String> fromColumns = Tuple.of("name", "salary", "bonus");
        TriFunction<String, Double, Double, String> func = (name, salary, bonus) -> name + " total: " + (salary + bonus);

        DataSetBuilder result = builder.addColumn("summary", fromColumns, func);

        Assertions.assertSame(builder, result);
        Assertions.assertTrue(dataSet.columnNameList().contains("summary"));
        Assertions.assertEquals("John total: 55000.0", dataSet.getColumn("summary").get(0));
    }

    @Test
    public void testAddColumnAtIndexWithTriFunction() {
        // Test adding a column at index using TriFunction
        builder.addColumn("bonus", Arrays.asList(5000.0, 3000.0, 7000.0));

        Tuple3<String, String, String> fromColumns = Tuple.of("age", "salary", "bonus");
        TriFunction<Integer, Double, Double, Double> func = (age, salary, bonus) -> (salary + bonus) / age;

        DataSetBuilder result = builder.addColumn(2, "total_per_year", fromColumns, func);

        Assertions.assertSame(builder, result);
        Assertions.assertTrue(dataSet.columnNameList().contains("total_per_year"));
        Assertions.assertEquals(2, dataSet.getColumnIndex("total_per_year"));
    }

    @Test
    public void testRemoveColumn() {
        // Test removing a single column
        DataSetBuilder result = builder.removeColumn("department");

        Assertions.assertSame(builder, result);
        Assertions.assertFalse(dataSet.columnNameList().contains("department"));
        Assertions.assertEquals(3, dataSet.columnCount());
    }

    @Test
    public void testRemoveColumns() {
        // Test removing multiple columns
        Collection<String> columnsToRemove = Arrays.asList("age", "department");

        DataSetBuilder result = builder.removeColumns(columnsToRemove);

        Assertions.assertSame(builder, result);
        Assertions.assertFalse(dataSet.columnNameList().contains("age"));
        Assertions.assertFalse(dataSet.columnNameList().contains("department"));
        Assertions.assertEquals(2, dataSet.columnCount());
    }

    @Test
    public void testRemoveColumnsWithPredicate() {
        // Test removing columns that match a predicate
        Predicate<String> filter = name -> name.length() > 4;

        DataSetBuilder result = builder.removeColumns(filter);

        Assertions.assertSame(builder, result);
        Assertions.assertFalse(dataSet.columnNameList().contains("salary"));
        Assertions.assertFalse(dataSet.columnNameList().contains("department"));
        Assertions.assertTrue(dataSet.columnNameList().contains("name"));
        Assertions.assertTrue(dataSet.columnNameList().contains("age"));
    }

    @Test
    public void testUpdateColumn() {
        // Test updating values in a column
        Function<Integer, Integer> func = age -> age + 5;

        DataSetBuilder result = builder.updateColumn("age", func);

        Assertions.assertSame(builder, result);
        Assertions.assertEquals(35, dataSet.getColumn("age").get(0));
        Assertions.assertEquals(30, dataSet.getColumn("age").get(1));
        Assertions.assertEquals(40, dataSet.getColumn("age").get(2));
    }

    @Test
    public void testUpdateColumns() {
        // Test updating values in multiple columns
        Collection<String> columnsToUpdate = Arrays.asList("age", "salary");
        Function<Number, Number> func = num -> {
            if (num instanceof Integer) {
                return ((Integer) num) * 2;
            } else if (num instanceof Double) {
                return ((Double) num) * 1.5;
            }
            return num;
        };

        DataSetBuilder result = builder.updateColumns(columnsToUpdate, func);

        Assertions.assertSame(builder, result);
        Assertions.assertEquals(60, dataSet.getColumn("age").get(0));
        Assertions.assertEquals(75000.0, dataSet.getColumn("salary").get(0));
    }

    @Test
    public void testConvertColumn() {
        // Test converting column type
        DataSetBuilder result = builder.convertColumn("age", String.class);

        Assertions.assertSame(builder, result);
        Assertions.assertEquals("30", dataSet.getColumn("age").get(0));
        Assertions.assertTrue(dataSet.getColumn("age").get(0) instanceof String);
    }

    @Test
    public void testConvertColumns() {
        // Test converting multiple columns
        Map<String, Class<?>> columnTargetTypes = new HashMap<>();
        columnTargetTypes.put("age", String.class);
        columnTargetTypes.put("salary", String.class);

        DataSetBuilder result = builder.convertColumns(columnTargetTypes);

        Assertions.assertSame(builder, result);
        Assertions.assertTrue(dataSet.getColumn("age").get(0) instanceof String);
        Assertions.assertTrue(dataSet.getColumn("salary").get(0) instanceof String);
    }

    @Test
    public void testCombineColumnsWithClass() {
        // Test combining columns into a new column of specified type
        Collection<String> columnsToCombine = Arrays.asList("name", "department");

        DataSetBuilder result = builder.combineColumns(columnsToCombine, "combined", a -> a.join(", "));

        Assertions.assertSame(builder, result);
        Assertions.assertTrue(dataSet.columnNameList().contains("combined"));
        Assertions.assertFalse(dataSet.columnNameList().contains("name"));
        Assertions.assertFalse(dataSet.columnNameList().contains("department"));
    }

    @Test
    public void testCombineColumnsWithFunction() {
        // Test combining columns with custom function
        Collection<String> columnsToCombine = Arrays.asList("name", "age");
        Function<DisposableObjArray, String> combineFunc = arr -> arr.get(0) + " (age: " + arr.get(1) + ")";

        DataSetBuilder result = builder.combineColumns(columnsToCombine, "person_info", combineFunc);

        Assertions.assertSame(builder, result);
        Assertions.assertTrue(dataSet.columnNameList().contains("person_info"));
        Assertions.assertEquals("John (age: 30)", dataSet.getColumn("person_info").get(0));
    }

    @Test
    public void testCombineColumnsWithBiFunction() {
        // Test combining two columns with BiFunction
        Tuple2<String, String> columnsToCombine = Tuple.of("name", "department");
        BiFunction<String, String, String> combineFunc = (name, dept) -> name + " in " + dept;

        DataSetBuilder result = builder.combineColumns(columnsToCombine, "name_in_dept", combineFunc);

        Assertions.assertSame(builder, result);
        Assertions.assertTrue(dataSet.columnNameList().contains("name_in_dept"));
        Assertions.assertEquals("John in IT", dataSet.getColumn("name_in_dept").get(0));
    }

    @Test
    public void testCombineColumnsWithTriFunction() {
        // Test combining three columns with TriFunction
        builder.addColumn("bonus", Arrays.asList(5000.0, 3000.0, 7000.0));

        Tuple3<String, String, String> columnsToCombine = Tuple.of("name", "salary", "bonus");
        TriFunction<String, Double, Double, String> combineFunc = (name, salary, bonus) -> name + ": $" + (salary + bonus);

        DataSetBuilder result = builder.combineColumns(columnsToCombine, "total_comp", combineFunc);

        Assertions.assertSame(builder, result);
        Assertions.assertTrue(dataSet.columnNameList().contains("total_comp"));
        Assertions.assertEquals("John: $55000.0", dataSet.getColumn("total_comp").get(0));
    }

    @Test
    public void testCombineColumnsWithPredicateAndClass() {
        // Test combining columns matching predicate
        builder.addColumn("score1", Arrays.asList(85, 90, 88));
        builder.addColumn("score2", Arrays.asList(92, 87, 95));

        Predicate<String> columnFilter = name -> name.startsWith("score");

        DataSetBuilder result = builder.combineColumns(columnFilter, "total_score", a -> a.join(" + "));

        Assertions.assertSame(builder, result);
        Assertions.assertTrue(dataSet.columnNameList().contains("total_score"));
        Assertions.assertFalse(dataSet.columnNameList().contains("score1"));
        Assertions.assertFalse(dataSet.columnNameList().contains("score2"));
    }

    @Test
    public void testCombineColumnsWithPredicateAndFunction() {
        // Test combining columns matching predicate with function
        builder.addColumn("metric1", Arrays.asList(10.5, 20.3, 15.7));
        builder.addColumn("metric2", Arrays.asList(5.2, 8.1, 6.9));

        Predicate<String> columnFilter = name -> name.startsWith("metric");
        Function<DisposableObjArray, Double> combineFunc = arr -> {
            double sum = 0;
            for (int i = 0; i < arr.length(); i++) {
                sum += (Double) arr.get(i);
            }
            return sum;
        };

        DataSetBuilder result = builder.combineColumns(columnFilter, "total_metric", combineFunc);

        Assertions.assertSame(builder, result);
        Assertions.assertTrue(dataSet.columnNameList().contains("total_metric"));
        Assertions.assertEquals(15.7, dataSet.getColumn("total_metric").get(0));
    }

    @Test
    public void testDivideColumnWithFunction() {
        // Test dividing a column with function returning list
        builder.addColumn("full_name", Arrays.asList("John Doe", "Jane Smith", "Bob Johnson"));

        Collection<String> newColumns = Arrays.asList("first_name", "last_name");
        Function<String, List<String>> divideFunc = fullName -> Arrays.asList(fullName.split(" "));

        DataSetBuilder result = builder.divideColumn("full_name", newColumns, divideFunc);

        Assertions.assertSame(builder, result);
        Assertions.assertTrue(dataSet.columnNameList().contains("first_name"));
        Assertions.assertTrue(dataSet.columnNameList().contains("last_name"));
        Assertions.assertFalse(dataSet.columnNameList().contains("full_name"));
        Assertions.assertEquals("John", dataSet.getColumn("first_name").get(0));
        Assertions.assertEquals("Doe", dataSet.getColumn("last_name").get(0));
    }

    @Test
    public void testDivideColumnWithBiConsumer() {
        // Test dividing a column with BiConsumer
        builder.addColumn("coordinates", Arrays.asList("10.5,20.3", "15.7,25.1", "30.2,40.8"));

        Collection<String> newColumns = Arrays.asList("x", "y");
        BiConsumer<String, Object[]> output = (coords, arr) -> {
            String[] parts = coords.split(",");
            arr[0] = Double.parseDouble(parts[0]);
            arr[1] = Double.parseDouble(parts[1]);
        };

        DataSetBuilder result = builder.divideColumn("coordinates", newColumns, output);

        Assertions.assertSame(builder, result);
        Assertions.assertTrue(dataSet.columnNameList().contains("x"));
        Assertions.assertTrue(dataSet.columnNameList().contains("y"));
        Assertions.assertEquals(10.5, dataSet.getColumn("x").get(0));
        Assertions.assertEquals(20.3, dataSet.getColumn("y").get(0));
    }

    @Test
    public void testDivideColumnIntoPair() {
        // Test dividing a column into pair
        builder.addColumn("key_value", Arrays.asList("name:John", "age:30", "city:NYC"));

        Tuple2<String, String> newColumns = Tuple.of("key", "value");
        BiConsumer<String, Pair<Object, Object>> output = (kv, pair) -> {
            String[] parts = kv.split(":");
            pair.setLeft(parts[0]);
            pair.setRight(parts[1]);
        };

        DataSetBuilder result = builder.divideColumn("key_value", newColumns, output);

        Assertions.assertSame(builder, result);
        Assertions.assertTrue(dataSet.columnNameList().contains("key"));
        Assertions.assertTrue(dataSet.columnNameList().contains("value"));
        Assertions.assertEquals("name", dataSet.getColumn("key").get(0));
        Assertions.assertEquals("John", dataSet.getColumn("value").get(0));
    }

    @Test
    public void testDivideColumnIntoTriple() {
        // Test dividing a column into triple
        builder.addColumn("rgb", Arrays.asList("255,0,0", "0,255,0", "0,0,255"));

        Tuple3<String, String, String> newColumns = Tuple.of("red", "green", "blue");
        BiConsumer<String, Triple<Object, Object, Object>> output = (rgb, triple) -> {
            String[] parts = rgb.split(",");
            triple.setLeft(Integer.parseInt(parts[0]));
            triple.setMiddle(Integer.parseInt(parts[1]));
            triple.setRight(Integer.parseInt(parts[2]));
        };

        DataSetBuilder result = builder.divideColumn("rgb", newColumns, output);

        Assertions.assertSame(builder, result);
        Assertions.assertTrue(dataSet.columnNameList().contains("red"));
        Assertions.assertTrue(dataSet.columnNameList().contains("green"));
        Assertions.assertTrue(dataSet.columnNameList().contains("blue"));
        Assertions.assertEquals(255, dataSet.getColumn("red").get(0));
        Assertions.assertEquals(0, dataSet.getColumn("green").get(0));
        Assertions.assertEquals(0, dataSet.getColumn("blue").get(0));
    }

    @Test
    public void testUpdateAll() {
        // Test updating all values in dataset
        Function<Object, Object> func = value -> {
            if (value == null)
                return "N/A";
            if (value instanceof String)
                return ((String) value).toUpperCase();
            return value;
        };

        DataSetBuilder result = builder.updateAll(func);

        Assertions.assertSame(builder, result);
        Assertions.assertEquals("JOHN", dataSet.getColumn("name").get(0));
        Assertions.assertEquals("IT", dataSet.getColumn("department").get(0));
        // Numbers remain unchanged
        Assertions.assertEquals(30, dataSet.getColumn("age").get(0));
    }

    @Test
    public void testReplaceIf() {
        // Test replacing values based on predicate
        // First, add some null values
        List<String> columnWithNulls = Arrays.asList("Active", null, "Active");
        builder.addColumn("status", columnWithNulls);

        Predicate<Object> predicate = value -> value == null;

        DataSetBuilder result = builder.replaceIf(predicate, "Inactive");

        Assertions.assertSame(builder, result);
        Assertions.assertEquals("Active", dataSet.getColumn("status").get(0));
        Assertions.assertEquals("Inactive", dataSet.getColumn("status").get(1));
        Assertions.assertEquals("Active", dataSet.getColumn("status").get(2));
    }

    @Test
    public void testPrepend() {
        // Test prepending another dataset
        List<String> columnNames = Arrays.asList("name", "age", "salary", "department");
        List<List<Object>> newData = new ArrayList<>();
        newData.add(Arrays.asList("Alice", 28, 55000.0, "Finance"));

        DataSet otherDataSet = DataSet.rows(columnNames, newData);

        DataSetBuilder result = builder.prepend(otherDataSet);

        Assertions.assertSame(builder, result);
        Assertions.assertEquals(4, dataSet.size());
        Assertions.assertEquals("Alice", dataSet.getColumn("name").get(0));
        Assertions.assertEquals("John", dataSet.getColumn("name").get(1));
    }

    @Test
    public void testAppend() {
        // Test appending another dataset
        List<String> columnNames = Arrays.asList("name", "age", "salary", "department");
        List<List<Object>> newData = new ArrayList<>();
        newData.add(Arrays.asList("Charlie", 40, 70000.0, "Sales"));

        DataSet otherDataSet = DataSet.rows(columnNames, newData);

        DataSetBuilder result = builder.append(otherDataSet);

        Assertions.assertSame(builder, result);
        Assertions.assertEquals(4, dataSet.size());
        Assertions.assertEquals("Bob", dataSet.getColumn("name").get(2));
        Assertions.assertEquals("Charlie", dataSet.getColumn("name").get(3));
    }

    @Test
    public void testMethodChaining() {
        // Test that methods can be chained together
        DataSetBuilder result = builder.renameColumn("name", "employee")
                .addColumn("bonus", Arrays.asList(5000.0, 3000.0, 7000.0))
                .updateColumn("age", (Integer age) -> age + 1)
                .removeColumn("department");

        Assertions.assertSame(builder, result);
        Assertions.assertTrue(dataSet.columnNameList().contains("employee"));
        Assertions.assertTrue(dataSet.columnNameList().contains("bonus"));
        Assertions.assertFalse(dataSet.columnNameList().contains("department"));
        Assertions.assertEquals(31, dataSet.getColumn("age").get(0));
    }

    @Test
    public void testComplexScenario() {
        // Test a complex scenario with multiple operations
        builder.addColumn("full_name", Arrays.asList("name", "department"), arr -> arr.get(0) + " from " + arr.get(1))
                .removeColumns(Arrays.asList("name", "department"))
                .updateColumn("salary", (Double s) -> s * 1.15)
                .addColumn("category", "age", (Integer age) -> age < 30 ? "Junior" : "Senior")
                .renameColumn("full_name", "employee_info");

        Assertions.assertTrue(dataSet.columnNameList().contains("employee_info"));
        Assertions.assertTrue(dataSet.columnNameList().contains("category"));
        Assertions.assertFalse(dataSet.columnNameList().contains("name"));
        Assertions.assertFalse(dataSet.columnNameList().contains("department"));
        Assertions.assertEquals("John from IT", dataSet.getColumn("employee_info").get(0));
        Assertions.assertEquals("Senior", dataSet.getColumn("category").get(0));
        Assertions.assertEquals("Junior", dataSet.getColumn("category").get(1));
        Assertions.assertEquals(57500.0, (Double) dataSet.getColumn("salary").get(0), 0.000001d);
    }
}