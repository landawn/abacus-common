package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.AbstractTest;
import com.landawn.abacus.entity.extendDirty.basic.Account;
import com.landawn.abacus.entity.extendDirty.basic.AccountContact;
import com.landawn.abacus.entity.extendDirty.basic.DataType;
import com.landawn.abacus.parser.JSONDeserializationConfig.JDC;
import com.landawn.abacus.parser.JSONSerializationConfig.JSC;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.NoCachingNoUpdating.DisposableObjArray;
import com.landawn.abacus.util.function.BiConsumer;
import com.landawn.abacus.util.function.Function;
import com.landawn.abacus.util.stream.Collectors;
import com.landawn.abacus.util.stream.IntStream.IntStreamEx;
import com.landawn.abacus.util.stream.Stream;

@Tag("old-test")
public class DatasetTest extends AbstractTest {
    final int threadNum = 100;
    final int recordCount = 10000;
    final int pageSize = 200;
    final int pageCount = recordCount / pageSize;

    @Test
    public void test_convertColumn() {

        final Dataset dataset = N.newDataset(N.asList("column1", "column2"), N.asList(N.asList("ab", "cd"), N.asList("ef", "gh")));

        assertThrows(IllegalArgumentException.class, () -> dataset.convertColumn("column1", Long.class));
    }

    @Test
    public void test_toMergedEntities() {

        {
            final List<Account> accountList = createAccountList(Account.class, 3);
            final MutableInt id = MutableInt.of(1);
            accountList.forEach(it -> it.setId(id.incrementAndGet())
                    .setContact(createAccountContact(AccountContact.class).setAccountId(id.value()).setEmail(Strings.uuid())));
            final List<Map<String, Object>> mapList = Stream.of(accountList).map(Beans::bean2FlatMap).toList();
            final Dataset ds = N.newDataset(mapList);

            ds.println();

            final List<Account> mergedEntities = ds.toMergedEntities(Account.class);
            mergedEntities.forEach(Fn.println());

            for (int i = 0; i < accountList.size(); i++) {
                assertEquals(accountList.get(i).getGUI(), mergedEntities.get(i).getGUI());
                assertEquals(accountList.get(i).getContact().getEmail(), mergedEntities.get(i).getContact().getEmail());
            }
        }

        {
            final List<Account> accountList = createAccountList(Account.class, 3);
            final MutableInt id = MutableInt.of(1);
            accountList.forEach(it -> it.setId(id.incrementAndGet())
                    .setContact(createAccountContact(AccountContact.class).setId(id.value() + 100).setAccountId(id.value()).setEmail(Strings.uuid())));
            final List<Map<String, Object>> mapList = Stream.of(accountList).map(Beans::bean2FlatMap).toList();
            final Dataset ds = N.newDataset(mapList);

            ds.println();

            final List<Account> mergedEntities = ds.toMergedEntities(Account.class);
            mergedEntities.forEach(Fn.println());

            for (int i = 0; i < accountList.size(); i++) {
                assertEquals(accountList.get(i).getGUI(), mergedEntities.get(i).getGUI());
                assertEquals(accountList.get(i).getContact().getEmail(), mergedEntities.get(i).getContact().getEmail());
            }
        }

        {
            final List<Account> accountList = createAccountList(Account.class, 3);
            final MutableInt id = MutableInt.of(1);
            accountList.forEach(it -> it.setId(id.incrementAndGet())
                    .setContact(createAccountContact(AccountContact.class).setId(id.value() + 100).setAccountId(id.value()).setEmail(Strings.uuid())));
            final List<Map<String, Object>> mapList = Stream.of(accountList).map(Beans::bean2FlatMap).toList();
            mapList.forEach(it -> Maps.replaceKeys(it, k -> k.startsWith("contact.") ? Strings.replaceFirst(k, "contact.", "ac.") : k));
            final Dataset ds = N.newDataset(mapList);

            ds.println();

            final List<Account> mergedEntities = ds.toMergedEntities(Account.class);
            mergedEntities.forEach(Fn.println());

            for (int i = 0; i < accountList.size(); i++) {
                assertEquals(accountList.get(i).getGUI(), mergedEntities.get(i).getGUI());
                assertEquals(accountList.get(i).getContact().getEmail(), mergedEntities.get(i).getContact().getEmail());
            }
        }
    }

    @Test
    public void test_json() {
        final List<Account> accountList = createAccountList(Account.class, 3);
        final Dataset ds = N.newDataset(accountList);
        ds.println();

        String json = N.toJson(ds, JSC.create().writeDatasetByRow(true));
        N.println(json);

        json = N.toJson(ds, JSC.create().writeDatasetByRow(true).prettyFormat(true));
        N.println(json);

        final Dataset ds2 = N.fromJson(json, Dataset.class);
        N.println(ds2);
        ds2.println();
    }

    @Test
    public void test_toString() {

        final List<Account> accountList = createAccountList(Account.class, 3);
        Dataset ds = N.newDataset(accountList);
        ds.println();

        String json = N.toJson(ds);
        N.println(json);

        ds = N.fromJson(json, Dataset.class);
        ds.println();

        json = N.toJson(ds, true);
        N.println(json);

        json = N.toJson(N.newEmptyDataset());
        N.println(json);

        json = N.toJson(N.newEmptyDataset(), true);
        N.println(json);

        json = N.toJson(N.newEmptyDataset(N.asList("a", "b", "c")));
        N.println(json);

        json = N.toJson(N.newEmptyDataset(N.asList("a", "b", "c")), true);
        N.println(json);

        N.println(ds.toString());

        final List<String> columnNames = N.asList("id", "name");
        final List<List<Object>> columns = N.asList(N.asList(1, 2, 3), N.asList("a", "b", "c"));
        final Map<String, Object> props = N.asProps("prop1", 123, "prop2", "abc");

        ds = new RowDataset(columnNames, columns, props);
        ds.println();
        N.println(N.toJson(ds));
    }

    @Test
    public void test_join_empty() {

        final List<Account> accountList = createAccountList(Account.class, 3);
        final Dataset ds = N.newDataset(accountList);
        Dataset emptyDS = N.newDataset(N.asList("id"), N.emptyList());
        emptyDS.println();

        {

            N.println("===========innerJoin=============");
            ds.innerJoin(emptyDS, "id", "id").println();

            N.println("===========leftJoin=============");
            ds.leftJoin(emptyDS, "id", "id").println();

            N.println("===========rightJoin=============");
            ds.rightJoin(emptyDS, "id", "id").println();

            N.println("===========fullJoin=============");
            ds.fullJoin(emptyDS, "id", "id").println();
        }

        {

            N.println("===========innerJoin2=============");
            emptyDS.innerJoin(ds, "id", "id").println();

            N.println("===========leftJoin2=============");
            emptyDS.leftJoin(ds, "id", "id").println();

            N.println("===========rightJoin2=============");
            emptyDS.rightJoin(ds, "id", "id").println();

            N.println("===========fullJoin2=============");
            emptyDS.fullJoin(ds, "id", "id").println();
        }

        {

            N.println("===========union=============");
            ds.union(emptyDS).println();

            N.println("===========unionAll=============");
            ds.unionAll(emptyDS).println();

            N.println("===========except=============");
            ds.except(emptyDS).println();

            N.println("===========exceptAll=============");
            ds.exceptAll(emptyDS).println();

            N.println("===========intersect=============");
            ds.intersect(emptyDS).println();

            N.println("===========intersectAll=============");
            ds.intersectAll(emptyDS).println();
        }

        {
            emptyDS = N.newDataset(N.asList("id", "gui"), N.emptyList());
            emptyDS.println();

            N.println("===========union=============");
            ds.union(emptyDS).println();

            N.println("===========unionAll=============");
            ds.unionAll(emptyDS).println();

            N.println("===========except=============");
            ds.except(emptyDS).println();

            N.println("===========exceptAll=============");
            ds.exceptAll(emptyDS).println();

            N.println("===========intersect=============");
            ds.intersect(emptyDS).println();

            N.println("===========intersectAll=============");
            ds.intersectAll(emptyDS).println();
            ds.exceptAll(emptyDS).println();

            N.println("===========intersection=============");
            ds.intersection(emptyDS).println();

            N.println("===========difference=============");
            ds.difference(emptyDS).println();

            N.println("===========symmetricDifference=============");
            ds.symmetricDifference(emptyDS).println();
        }
    }

    @Test
    public void test_rollup() throws Exception {
        final List<Account> accountList = createAccountList(Account.class, 9);
        accountList.forEach(it -> it.setId(2));
        final Dataset ds = N.newDataset(accountList);
        ds.rollup(ds.columnNameList()).forEach(Dataset::println);
    }

    @Test
    public void test_cube() throws Exception {
        final List<Account> accountList = createAccountList(Account.class, 9);
        accountList.forEach(it -> it.setId(2));
        final Dataset ds = N.newDataset(accountList);
        ds.cube(ds.columnNameList()).forEach(Dataset::println);
    }

    @Test
    public void test_cube_2() throws Exception {
        final Object[][] rowList = { { "Banana", 1000, "USA" }, { "Carrots", 1500, "USA" }, { "Beans", 1600, "USA" }, { "Orange", 2000, "USA" },
                { "Orange", 2000, "USA" }, { "Banana", 400, "China" }, { "Carrots", 1200, "China" }, { "Beans", 1500, "China" }, { "Orange", 4000, "China" },
                { "Banana", 2000, "Canada" }, { "Carrots", 2000, "Canada" }, { "Beans", 2000, "Mexico" } };

        final Dataset dataset = N.newDataset(N.asList("Product", "Amount", "Country"), rowList);

        dataset.println();

        dataset.cube(N.asList("Product", "Country"), N.asList("Amount"), "result", List.class).forEach(Dataset::println);
    }

    @Test
    public void test_csv_01() throws Exception {
        final Object[][] rowList = { { "Banana", 1000, "USA" }, { "Carrots", 1500, "USA" }, { "Beans", 1600, "USA" }, { "Orange", 2000, "USA" },
                { "Orange", 2000, "USA" }, { "Banana", 400, "China" }, { "Carrots", 1200, "China" }, { "Beans", 1500, "China" }, { "Orange", 4000, "China" },
                { "Banana", 2000, "Canada" }, { "Carrots", 2000, "Canada" }, { "Beans", 2000, "Mexico" } };

        final Dataset dataset = N.newDataset(N.asList("Prod\"^@&\\'skdf'''\\\\\\uct", "\\\"^@&\\\\'skdf'''\\\\\\\\\\\\uct", "Country"), rowList);

        dataset.println();

        N.println(dataset.toCsv());
    }

    @Test
    public void test_sortBy() throws Exception {

        final List<List<Object>> rowList = IntStreamEx.range(0, 10)
                .mapToObj(it -> N.<Object> asList(it, ((char) ('a' + it)), it + "_" + ((char) ('a' + it))))
                .toList();
        N.shuffle(rowList);

        Dataset dataset = N.newDataset(N.asList("int", "char", "str"), rowList);
        dataset.println();

        dataset.sortBy("int");
        dataset.println();

        dataset = N.newDataset(N.asList("int", "char", "str"), rowList);
        dataset.sortBy(N.asList("char", "int"), Comparators.OBJECT_ARRAY_COMPARATOR);
        dataset.println();

        dataset = N.newDataset(N.asList("int", "char", "str"), rowList);
        dataset.sortBy(N.asList("char", "int"), Comparators.comparingObjArray(Comparators.reverseOrder()));
        dataset.println();
    }

    @Test
    public void test_pivot() throws Exception {
        final Object[][] rowList = { { "Banana", 1000, "USA" }, { "Carrots", 1500, "USA" }, { "Beans", 1600, "USA" }, { "Orange", 2000, "USA" },
                { "Orange", 2000, "USA" }, { "Banana", 400, "China" }, { "Carrots", 1200, "China" }, { "Beans", 1500, "China" }, { "Orange", 4000, "China" },
                { "Banana", 2000, "Canada" }, { "Carrots", 2000, "Canada" }, { "Beans", 2000, "Mexico" } };

        final Dataset dataset = N.newDataset(N.asList("Product", "Amount", "Country"), rowList);

        dataset.println();

        final Dataset ds2 = dataset.groupBy(N.asList("Product", "Country"), "Amount", "sum(Amount)", Collectors.summingLong(it -> ((Number) it).longValue()));
        ds2.sortBy("Product");
        ds2.println();
    }

    @Test
    public void test_pivot_2() throws Exception {
        final Object[][] rowList = { { "Banana", 1000, "USA" }, { "Carrots", 1500, "USA" }, { "Beans", 1600, "USA" }, { "Orange", 2000, "USA" },
                { "Orange", 2000, "USA" }, { "Banana", 400, "China" }, { "Carrots", 1200, "China" }, { "Beans", 1500, "China" }, { "Orange", 4000, "China" },
                { "Banana", 2000, "Canada" }, { "Carrots", 2000, "Canada" }, { "Beans", 2000, "Mexico" } };

        final Dataset dataset = N.newDataset(N.asList("Product", "Amount", "Country"), rowList);

        dataset.println();

        Sheet<String, String, Double> sheet = dataset.pivot("Product", "Country", "Amount", Collectors.summingDouble(Number::doubleValue));
        sheet.println();

        sheet = dataset.pivot("Country", "Product", "Amount", Collectors.summingDouble(Number::doubleValue));
        sheet.println();

        dataset.pivot("Country", "Product", N.asList("Amount", "Country"), it -> it.join("_"), Collectors.toList()).println();
        dataset.pivot("Country", "Product", N.asList("Amount", "Country"), N::toString, Collectors.toList()).println();
        dataset.pivot("Country", "Product", N.asList("Amount", "Country"), Collectors.mappingToList(N::toString)).println();

        Sheet<String, Integer, List<String>> sheet2 = dataset.pivot("Country", "Amount", N.asList("Product", "Country"), Collectors.mappingToList(N::toString));
        sheet2.println();

        dataset.sortBy("Amount");
        sheet2 = dataset.pivot("Country", "Amount", N.asList("Product", "Country"), Collectors.mappingToList(N::toString));
        sheet2.println();

        dataset.sortBy(N.asList("Country", "Amount"));
        sheet2 = dataset.pivot("Country", "Amount", N.asList("Product", "Country"), Collectors.mappingToList(N::toString));
        sheet2.println();

        sheet2.sortByColumnKey();
        sheet2.println();
    }

    @Test
    public void test_sheet_sortBy() throws Exception {
        final Object[][] rowList = { { "Banana", 1000, "USA" }, { "Carrots", 1500, "USA" }, { "Beans", 1600, "USA" }, { "Orange", 2000, "USA" },
                { "Orange", 2000, "USA" }, { "Banana", 400, "China" }, { "Carrots", 1200, "China" }, { "Beans", 1500, "China" }, { "Orange", 4000, "China" },
                { "Banana", 2000, "Canada" }, { "Carrots", 2000, "Canada" }, { "Beans", 2000, "Mexico" } };

        final Dataset dataset = N.newDataset(N.asList("Product", "Amount", "Country"), rowList);

        dataset.println();

        final Sheet<String, Integer, List<String>> sheet = dataset.pivot("Country", "Amount", N.asList("Product", "Country"),
                Collectors.mappingToList(N::toString));
        sheet.println();

        Sheet<String, Integer, List<String>> copy = sheet.copy();
        copy.sortByColumnKey();
        copy.println();

        copy = sheet.copy();
        copy.sortByRowKey();
        copy.println();

        copy = sheet.copy();
        copy.sortByRow("China", Comparators.comparingCollection());
        copy.println();

        copy = sheet.copy();
        copy.sortByRow("China", Comparators.<List<String>> comparingCollection().reversed());
        copy.println();

        copy = sheet.copy();
        copy.sortByColumn(1500, Comparators.comparingCollection());
        copy.println();

        copy = sheet.copy();
        copy.sortByColumn(1500, Comparators.<List<String>> comparingCollection().reversed());
        copy.println();

        N.println("sortByColumns" + Strings.repeat("=", 80));
        copy = sheet.copy();
        copy.println();
        copy.sortByColumns(N.asList(1500, 2000), Comparators.comparingObjArray(Comparators.comparingCollection()));
        copy.println();
        copy = sheet.copy();
        copy.sortByColumns(N.asList(1500, 2000), Comparators.comparingObjArray(Comparators.comparingCollection()).reversed());
        copy.println();
        N.println(Strings.repeat("=", 80));

        N.println("sortByRows" + Strings.repeat("=", 80));
        copy = sheet.copy();
        copy.println();
        copy.sortByRows(N.asList("China", "USA"), Comparators.comparingObjArray(Comparators.comparingCollection()));
        copy.println();
        copy = sheet.copy();
        copy.sortByRows(N.asList("China", "USA"), Comparators.comparingObjArray(Comparators.comparingCollection()).reversed());
        copy.println();
        N.println(Strings.repeat("=", 80));

        copy = sheet.copy();
        copy.transpose().println();

    }

    @Test
    public void test_toDataset() throws Exception {
        final Object[][] rowList = { { "Banana", 1000, "USA" }, { "Carrots", 1500, "USA" }, { "Beans", 1600, "USA" }, { "Orange", 2000, "USA" },
                { "Orange", 2000, "USA" }, { "Banana", 400, "China" }, { "Carrots", 1200, "China" }, { "Beans", 1500, "China" }, { "Orange", 4000, "China" },
                { "Banana", 2000, "Canada" }, { "Carrots", 2000, "Canada" }, { "Beans", 2000, "Mexico" } };

        final Dataset dataset = N.newDataset(N.asList("Product", "Amount", "Country"), rowList);

        final Sheet<String, Integer, List<String>> sheet = dataset.pivot("Country", "Amount", N.asList("Product", "Country"),
                Collectors.mappingToList(N::toString));
        sheet.println();

        sheet.toDatasetH().println();
        sheet.toDatasetV().println();

        N.println(Strings.repeat("=", 80));
        N.forEach(sheet.toArrayH(), Fn.println());
        N.println(Strings.repeat("=", 80));
        N.forEach(sheet.toArrayV(), Fn.println());
        N.println(Strings.repeat("=", 80));

    }

    @Test
    public void test_json_xml() throws Exception {
        final List<Account> accountList = createAccountList(Account.class, 9);
        final Dataset ds = N.newDataset(accountList, N.asMap("prop1", 1, "key2", "val2"));
        N.println(ds.toJson());

        N.println(ds.toXml());

        N.println(N.toJson(ds));
        N.println(N.toJson(ds, true));

    }

    @Test
    public void test_json_2() throws Exception {
        final List<Account> accountList = createAccountList(Account.class, 9);
        final Dataset ds = N.newDataset(accountList, N.asMap("prop1", 1, "key2", "val2"));
        N.println(Strings.repeat("=", 80));
        ds.println();

        final String json = ds.toJson();
        N.println(Strings.repeat("=", 80));
        N.println(json);

        final Dataset ds2 = N.fromJson(json, JDC.create().setValueTypesByBeanClass(Account.class), Dataset.class);
        N.println(Strings.repeat("=", 80));
        ds2.println();

        assertEquals(ds, ds2);

        final Map<Object, Object> map = N.asMap("key", accountList);

        final Map<String, Dataset> map2 = N.fromJson(N.toJson(map), JDC.create().setValueTypesByBeanClass(Account.class),
                Type.ofMap(String.class, Dataset.class));
        map2.entrySet().iterator().next().getValue().println();

        final List<Dataset> list = N.fromJson(N.toJson(N.asList(accountList)), JDC.create().setValueTypesByBeanClass(Account.class),
                Type.ofList(Dataset.class));
        list.get(0).println();

    }

    @Test
    public void test_json_3() throws Exception {
        final List<Account> accountList = createAccountList(Account.class, 9);

        final List<Map<String, Object>> mapList = Stream.of(accountList).map(Beans::bean2Map).toList();

        mapList.get(0).remove("id");

        mapList.get(mapList.size() / 2).remove("emailAddress");

        mapList.get(mapList.size() - 1).remove("createdTime");

        String json = N.toJson(mapList, true);
        N.println(Strings.repeat("=", 80));
        N.println(json);

        Dataset ds2 = N.fromJson(json, JDC.create().setValueTypesByBeanClass(Account.class), Dataset.class);
        N.println(Strings.repeat("=", 80));
        ds2.println();

        json = N.toJson(N.asList(accountList.get(0)));

        ds2 = N.fromJson(json, JDC.create().setValueTypesByBeanClass(Account.class), Dataset.class);
        N.println(Strings.repeat("=", 80));
        ds2.println();

        ds2 = N.fromJson("[]", JDC.create().setValueTypesByBeanClass(Account.class), Dataset.class);
        N.println(Strings.repeat("=", 80));
        ds2.println();

    }

    @Test
    public void test_groupBy() throws Exception {
        final List<Account> accountList = createAccountList(Account.class, 9);
        final MutableInt idVal = MutableInt.of(accountList.size());
        accountList.forEach(it -> it.setId(idVal.incrementAndGet() % 3));
        final Dataset ds = N.newDataset(accountList);
        ds.groupBy("id", ds.columnNameList(), "account", Account.class).println();

        ds.groupBy(N.asList("id", "firstName"), ds.columnNameList(), "account", Account.class).println();

        ds.groupBy(N.asList("id", "firstName"), N.asList("lastName", "firstName"), "account", it -> it.join(":"), Collectors.toList()).println();
    }

    @Test
    public void test_toList() {
        final Dataset dataset = N.newDataset(N.asList("a", "b", "c"), N.asList(N.asList("a1", "b1", "c1")));
        dataset.toList(dataset.columnNameList().subList(1, 3), Object[].class).forEach(Fn.println());
    }

    @Test
    public void test_toMergedEntities_1() {
        final Map<String, String> map = new HashMap<>();
        map.put(null, null);

        final List<String> columNames = N.asList("id", "name", "devices.id", "devices.model", "devices.serialNumber");
        final Dataset dataset = Dataset.rows(columNames,
                new Object[][] { { 100, "Bob", 1, "iPhone", "abc123" }, { 100, "Bob", 2, "MacBook", "mmm123" }, { 200, "Alice", 3, "Android", "aaa223" } });

        dataset.println("     * # ");

        dataset.toEntities(Map.of("d", "devices"), Account.class).forEach(e -> System.out.println(N.toJson(e)));

        dataset.toMergedEntities(Account.class).forEach(e -> System.out.println(N.toJson(e)));

        final List<Account> accounts = dataset.toMergedEntities(Account.class);

        String json = N.toJson(accounts, JSC.create().prettyFormat(true));
        N.println(json);
    }

    @Test
    public void test_toMergedEntities_2() {
        final Map<String, String> map = new HashMap<>();
        map.put(null, null);

        final List<String> columNames = N.asList("id", "firstName", "contact.id", "contact.address", "contact.city", "device.id", "device.name",
                "device.model");
        final Dataset dataset = N.newDataset(columNames, N.asList(N.asList(1, "firstName1", 1, "address1", "city1", 1, "device1", "model1"),
                N.asList(1, "firstName2", 2, "address2", "city2", 2, "device2", "model2")));

        dataset.toList(Account.class).stream().map(N::toJson).forEach(Fn.println());

        final List<Account> accounts = dataset.toList(Account.class);
        accounts.stream().map(N::toJson).forEach(Fn.println());
        assertEquals(2, accounts.size());
        assertEquals("firstName1", accounts.get(0).getFirstName());
        assertEquals("address2", accounts.get(1).getContact().getAddress());
        assertEquals(1, accounts.get(0).getDevices().size());
        assertEquals(1, accounts.get(1).getDevices().size());

        final List<Account> accounts1 = dataset.toMergedEntities(Account.class);
        accounts1.stream().map(N::toJson).forEach(Fn.println());
        assertEquals(1, accounts1.size());
        assertEquals("firstName2", accounts1.get(0).getFirstName());
        assertEquals(2, accounts1.get(0).getDevices().size());

        final List<Account> accounts2 = dataset.toMergedEntities(N.asList("id", "firstName"), dataset.columnNameList(), Account.class);
        accounts2.stream().map(N::toJson).forEach(Fn.println());
        assertEquals(2, accounts2.size());
        assertEquals("firstName1", accounts2.get(0).getFirstName());
        assertEquals(1, accounts2.get(0).getDevices().size());
    }

    @Test
    public void test_toMergedEntities_3() {
        final Map<String, String> map = new HashMap<>();
        map.put(null, null);

        final List<String> columNames = N.asList("id", "firstName", "ct.id", "ct.address", "ct.city", "device.id", "device.name", "device.model");
        final Dataset dataset = N.newDataset(columNames, N.asList(N.asList(1, "firstName1", 1, "address1", "city1", 1, "device1", "model1"),
                N.asList(1, "firstName2", 2, "address2", "city2", 2, "device2", "model2")));

        dataset.toList(Account.class).stream().map(N::toJson).forEach(Fn.println());

        final List<Account> accounts = dataset.toEntities(dataset.columnNameList(), N.asMap("ct", "contact"), Account.class);
        accounts.stream().map(N::toJson).forEach(Fn.println());
        assertEquals(2, accounts.size());
        assertEquals("firstName1", accounts.get(0).getFirstName());
        assertEquals("address2", accounts.get(1).getContact().getAddress());
        assertEquals(1, accounts.get(0).getDevices().size());
        assertEquals(1, accounts.get(1).getDevices().size());

        final List<Account> accounts1 = dataset.toMergedEntities(N.asList("id"), dataset.columnNameList(), N.asMap("ct", "contact"), Account.class);
        accounts1.stream().map(N::toJson).forEach(Fn.println());
        assertEquals(1, accounts1.size());
        assertEquals("firstName2", accounts1.get(0).getFirstName());
        assertEquals("address2", accounts1.get(0).getContact().getAddress());
        assertEquals(2, accounts1.get(0).getDevices().size());

        final List<Account> accounts2 = dataset.toMergedEntities(N.asList("id", "firstName"), dataset.columnNameList(), N.asMap("ct", "contact"),
                Account.class);
        accounts2.stream().map(N::toJson).forEach(Fn.println());
        assertEquals(2, accounts2.size());
        assertEquals("firstName1", accounts2.get(0).getFirstName());
        assertEquals("address1", accounts2.get(0).getContact().getAddress());
        assertEquals(1, accounts2.get(0).getDevices().size());
    }

    @Test
    public void test_emptyDataset() {
        N.newEmptyDataset().println();
        N.newEmptyDataset(N.asList("co1", "col2")).println();

        N.println(N.newEmptyDataset().toJson());
        N.println(N.newEmptyDataset().toXml());
        N.println(N.newEmptyDataset().toCsv());

        N.println(N.newEmptyDataset(N.asList("firstName", "lastName")).toJson());
        N.println(N.newEmptyDataset(N.asList("firstName", "lastName")).toXml());
        N.println(N.newEmptyDataset(N.asList("firstName", "lastName")).toCsv());
        N.println(N.newEmptyDataset(N.asList("firstName", "lastName")).toCsv());

        N.println(N.toJson(N.newEmptyDataset()));
        N.println(N.toJson(N.newEmptyDataset(N.asList("firstName", "lastName"))));

        N.println(N.fromJson(N.toJson(N.newEmptyDataset(N.asList("firstName", "lastName"))), Dataset.class));
        N.println(N.fromJson(N.toJson(N.newEmptyDataset()), Dataset.class));
    }

    @Test
    public void test_cartesianProduct() {
        final Dataset a = N.newDataset(N.asLinkedHashMap("col1", N.asList(1, 2), "col2", N.asList(3, 4)));
        final Dataset b = N.newDataset(N.asLinkedHashMap("col3", N.asList("a", "b"), "col4", N.asList("c", "d")));
        a.cartesianProduct(b).println();

        N.newEmptyDataset().cartesianProduct(b).println();
        N.newEmptyDataset(N.asList("co1", "col2")).cartesianProduct(b).println();
        N.newEmptyDataset(N.asList("co1", "col2")).cartesianProduct(N.newEmptyDataset()).println();

        N.newEmptyDataset().cartesianProduct(N.newEmptyDataset()).println();

        try {
            N.newEmptyDataset(N.asList("col1", "col2")).cartesianProduct(N.newEmptyDataset(N.asList("col1", "a"))).println();
            fail("Should throw: IllegalArgumentException");
        } catch (final IllegalArgumentException e) {

        }
    }

    @Test
    public void test_print() {
        final Dataset ds = N.newDataset(N.asList("a", "blafjiawfj;lkasjf23 i2qfja;lsfjoiaslf", "c"),
                N.asList(N.asList(1, "n1kafjeoiwajf", "c1"), N.asList(2, "n2", "c2las83292rfjioa"), N.asList(3, "n3", "c3")));
        ds.println();

        StringWriter outputWriter = new StringWriter();
        ds.println(outputWriter);
        N.println(outputWriter.toString());

        ds.println(0, 2, N.asList("c"));
        ds.println(1, 3, N.asList("a", "c"));

        ds.clear();
        ds.println();

        outputWriter = new StringWriter();
        ds.println(outputWriter);
        N.println(outputWriter.toString());

        ds.removeColumns(ds.columnNameList());
        ds.println();
    }

    @Test
    public void test_rename() {
        final Dataset ds1 = N.newDataset(N.asList("a", "b", "c"), N.asList(N.asList(1, "n1", "c1"), N.asList(2, "n2", "c2"), N.asList(3, "n3", "c3")));
        ds1.renameColumns(N.asMap("a", "a", "c", "d"));
        ds1.println();
        N.println(N.toJson(ds1));

        ds1.slice(0, 2, N.asList("a")).println();
    }

    @Test
    public void test_union_all() {
        Dataset ds1 = N.newDataset(N.asList("id", "name", "city"), N.asList(N.asList(1, "n1", "c1"), N.asList(2, "n2", "c2"), N.asList(3, "n3", "c3")));
        Dataset ds2 = N.newDataset(N.asList("id", "address2", "state"), N.asList(N.asList(1, "n1", "c1"), N.asList(2, "n2", "c2"), N.asList(2, "n22", "c22")));

        N.println("============================== union ===========================");
        ds1.union(ds2).println();

        N.println("============================== unionAll ===========================");
        ds1.unionAll(ds2).println();

        ds1 = N.newDataset(N.asList("id", "name", "city"), N.asList(N.asList(1, "n1", "c1"), N.asList(2, "n2", "c2"), N.asList(3, "n3", "c3")));
        ds2 = N.newDataset(N.asList("id", "name", "city"), N.asList(N.asList(2, "n2", "c2"), N.asList(2, "n2", "c2"), N.asList(3, "n4", "c4")));

        N.println("============================== union ===========================");
        ds1.union(ds2).println();

        N.println("============================== unionAll ===========================");
        ds1.unionAll(ds2).println();

        N.println("============================== intersection ===========================");
        ds1.intersection(ds2).println();

        N.println("============================== intersectAll ===========================");
        ds1.intersectAll(ds2).println();

        ds1 = N.newDataset(N.asList("id", "name", "city"),
                N.asList(N.asList(1, "n1", "c1"), N.asList(2, "n2", "c2"), N.asList(2, "n2", "c2"), N.asList(3, "n3", "c3")));
        ds2 = N.newDataset(N.asList("id", "name", "city", "state"),
                N.asList(N.asList(2, "n2", "c2", "CA"), N.asList(2, "n2", "c2", "CA"), N.asList(3, "n4", "c4", "CA")));

        N.println("============================== intersectAll ===========================");
        ds1.intersectAll(ds2).println();

        N.println("============================== intersectAll ===========================");
        ds2.intersectAll(ds1).println();

    }

    @Test
    public void test_join_all() {
        final Dataset ds1 = N.newDataset(N.asList("id", "name", "city"), N.asList(N.asList(1, "n1", "c1"), N.asList(2, "n2", "c2"), N.asList(3, "n3", "c3")));
        final Dataset ds2 = N.newDataset(N.asList("id", "address2", "state"),
                N.asList(N.asList(1, "n1", "c1"), N.asList(2, "n2", "c2"), N.asList(2, "n22", "c22"), N.asList(4, "n4", "c4")));

        N.println("============================== ds1/2 ===========================");

        ds1.println();
        ds2.println();

        N.println("============================== innerJoin ===========================");
        ds1.innerJoin(ds2, "id", "id").println();
        ds1.innerJoin(ds2, N.asMap("id", "id"), "newAddress", List.class).println();
        ds1.innerJoin(ds2, N.asMap("id", "id"), "newAddress", List.class, IntFunctions.ofList()).println();
        ds1.innerJoin(ds2, N.asMap("id", "id", "name", "address2")).println();
        ds1.innerJoin(ds2, N.asMap("id", "id", "name", "address2"), "newAddress", List.class).println();
        ds1.innerJoin(ds2, N.asMap("id", "id", "name", "address2"), "newAddress", List.class, IntFunctions.ofSet()).println();

        N.println("============================== left join ===========================");
        ds1.leftJoin(ds2, "id", "id").println();
        ds1.leftJoin(ds2, N.asMap("id", "id"), "newAddress", List.class).println();
        ds1.leftJoin(ds2, N.asMap("id", "id"), "newAddress", List.class, IntFunctions.ofList()).println();
        ds1.leftJoin(ds2, N.asMap("id", "id", "name", "address2")).println();
        ds1.leftJoin(ds2, N.asMap("id", "id", "name", "address2"), "newAddress", List.class).println();
        ds1.leftJoin(ds2, N.asMap("id", "id", "name", "address2"), "newAddress", List.class, IntFunctions.ofSet()).println();

        N.println("============================== right join ===========================");
        ds1.rightJoin(ds2, "id", "id").println();
        ds1.rightJoin(ds2, N.asMap("id", "id"), "newAddress", List.class).println();
        ds1.rightJoin(ds2, N.asMap("id", "id"), "newAddress", List.class, IntFunctions.ofList()).println();
        ds1.rightJoin(ds2, N.asMap("id", "id", "name", "address2")).println();
        ds1.rightJoin(ds2, N.asMap("id", "id", "name", "address2"), "newAddress", List.class).println();
        ds1.rightJoin(ds2, N.asMap("id", "id", "name", "address2"), "newAddress", List.class, IntFunctions.ofSet()).println();

        N.println("============================== full join ===========================");
        ds1.fullJoin(ds2, "id", "id").println();
        ds1.fullJoin(ds2, N.asMap("id", "id"), "newAddress", List.class).println();
        ds1.fullJoin(ds2, N.asMap("id", "id"), "newAddress", List.class, IntFunctions.ofList()).println();
        ds1.fullJoin(ds2, N.asMap("id", "id", "name", "address2")).println();
        ds1.fullJoin(ds2, N.asMap("id", "id", "name", "address2"), "newAddress", List.class).println();
        ds1.fullJoin(ds2, N.asMap("id", "id", "name", "address2"), "newAddress", List.class, IntFunctions.ofSet()).println();
    }

    @Test
    public void test_removeRowRange() {
        final Dataset ds = N.newDataset(N.asList(createAccount(Account.class), createAccount(Account.class), createAccount(Account.class),
                createAccount(Account.class), createAccount(Account.class), createAccount(Account.class)));

        final Dataset ds1 = ds.copy();
        ds1.println();

        ds1.removeRows(1, 5);
        ds1.println();

        final Dataset ds2 = ds.copy();
        ds2.println();

        ds2.removeMultiRows(1, 3, 5);
        ds2.println();

        final Dataset ds3 = ds.copy();
        ds3.println();

        ds3.removeMultiRows(0, 2, 4, 5);
        ds3.println();
    }

    @Test
    public void test_combine_divide() throws Exception {
        final Dataset ds1 = N.newDataset(N.asList(createAccount(Account.class), createAccount(Account.class), createAccount(Account.class)));
        ds1.removeColumns(N.asList("gui", "emailAddress", "lastUpdateTime", "createdTime"));
        ds1.updateRow(0, t -> t instanceof String ? t + "__0" : t);

        Dataset ds2 = ds1.copy();
        ds2.combineColumns(N.asList("firstName", "lastName"), "name", Map.class);
        ds2.println();

        ds2 = ds1.copy();
        ds2.combineColumns(N.asList("firstName", "lastName"), "name", (Function<DisposableObjArray, String>) t -> Strings.join(t.copy(), "-"));
        ds2.println();

        ds2.moveColumn("name", 0);
        ds2.println();

        ds2.divideColumn("name", N.asList("firstName", "lastName"), (BiConsumer<String, Object[]>) (t, a) -> {
            final String[] strs = Splitter.with("-").splitToArray(t);
            N.copy(strs, 0, a, 0, a.length);
        });

        ds2.println();
    }

    @Test
    public void test_swap() throws Exception {
        final Dataset ds1 = N.newDataset(N.asList(createAccount(Account.class), createAccount(Account.class), createAccount(Account.class)));
        ds1.removeColumns(N.asList("gui", "emailAddress", "lastUpdateTime", "createdTime"));
        ds1.println();

        ds1.updateRow(0, t -> t instanceof String ? t + "___" : t);

        ds1.println();

        ds1.swapColumnPosition("firstName", "lastName");
        ds1.println();

        ds1.swapRowPosition(1, 0);
        ds1.println();

        ds1.clone().println();
    }

    @Test
    public void test_update() throws Exception {
        final Account account = createAccount(Account.class);
        final Dataset ds1 = N.newDataset(N.asList(account, account));

        ds1.updateRow(0, t -> t instanceof String ? t + "___" : t);

        ds1.updateRows(Array.of(1, 0), (i, c, v) -> v instanceof String ? v + "___" : v);

        ds1.println();

        ds1.updateColumn("firstName", t -> t instanceof String ? t + "###" : t);

        ds1.println();

        ds1.updateColumns(N.asList("lastName", "firstName"), (i, c, v) -> v instanceof String ? v + "###" : v);

        ds1.println();

        ds1.updateAll(t -> t instanceof String ? t + "+++" : t);

        ds1.println();
    }

    @Test
    public void test_clone() throws Exception {
        final Account account = createAccount(Account.class);
        final Dataset ds1 = N.newDataset(N.asList(account, account));

        assertEquals(ds1, ds1.copy());
        assertEquals(ds1, ds1.clone());
        assertEquals(ds1.copy(), ds1.clone());
        assertEquals(ds1.clone(), ds1.clone());

        assertFalse(ds1.copy().isFrozen());
        assertFalse(ds1.clone().isFrozen());
        assertFalse(ds1.clone(false).isFrozen());
        assertTrue(ds1.clone(true).isFrozen());

        ds1.freeze();

        assertFalse(ds1.copy().isFrozen());
        assertTrue(ds1.clone().isFrozen());
        assertFalse(ds1.clone(false).isFrozen());
        assertTrue(ds1.clone(true).isFrozen());
    }

    @Test
    public void test_addColumn() throws Exception {
        final Account account = createAccount(Account.class);
        final Dataset ds1 = N.newDataset(N.asList(account, account));

        ds1.addColumn("firstName2", "firstName", (Function<String, String>) t -> "**********" + t);

        ds1.println();

        ds1.addColumn(0, "firstName3", N.asList("firstName", "lastName"), (Function<DisposableObjArray, String>) a -> a.get(0) + "**********" + a.get(1));

        ds1.println();
    }

    @Test
    public void test_intersection() throws Exception {
        final Account account = createAccount(Account.class);
        final Dataset ds1 = N.newDataset(N.asList(account));
        final Dataset ds2 = N.newDataset(N.asList(account, account));
        final Dataset ds3 = N.newDataset(N.asList(account, account, account));

        assertEquals(0, ds2.except(ds1).size());
        assertEquals(1, ds2.difference(ds1).size());
        assertEquals(0, ds3.except(ds1).size());
        assertEquals(2, ds3.difference(ds1).size());

        assertEquals(1, ds2.intersection(ds1).size());
        assertEquals(2, ds2.intersectAll(ds1).size());
        assertEquals(2, ds3.intersection(ds2).size());
        assertEquals(3, ds3.intersectAll(ds2).size());
    }

    @Test
    public void test_lift() throws Exception {
        final List<Account> accountList = createAccountList(Account.class, 7);

        final Dataset ds = N.newDataset(accountList);

        N.println(ds.toMap("firstName", "lastName"));

    }

    @Test
    public void test_multiset() throws Exception {
        final List<Account> accountList = createAccountList(Account.class, 7);
        final Dataset ds = N.newDataset(accountList);
        N.println(ds.stream("gui").toMultiset());
        N.println(ds.stream("firstName").toMultiset());
        N.println(ds.stream(N.asList("firstName", "lastName"), String[].class).toMultiset());
        N.println(ds.stream(N.asList("firstName", "lastName"), List.class).toMultiset());
        N.println(ds.stream(N.asList("firstName", "lastName"), Set.class).toMultiset());
        N.println(ds.stream(N.asList("firstName", "lastName"), Account.class).toMultiset());
    }

    @Test
    public void test_top() throws Exception {
        final List<Account> accountList = createAccountList(Account.class, 7);
        final MutableInt idx = MutableInt.of(100);
        final Dataset ds = N.newDataset(accountList);
        ds.updateColumns(N.asList("firstName", "lastName"), (i, c, v) -> (String) v + idx.getAndIncrement());
        ds.println();

        ds.topBy("lastName", 3).println();
        ds.topBy(N.asList("lastName", "gui"), 3).println();

        ds.topBy("lastName", 3, (Comparator<String>) (o1, o2) -> o2.compareTo(o1)).println();

        ds.topBy(N.asList("lastName", "gui"), 3, (Comparator<Object[]>) (o1, o2) -> ((String) o2[0]).compareTo((String) o1[0])).println();
    }

    @Test
    public void test_renameColumn_2() throws Exception {
        final List<Account> accountList = createAccountList(Account.class, 7);

        final Dataset ds = N.newDataset(accountList);
        ds.renameColumns(ds.columnNameList(), t -> t + "2");
        ds.println();

        ds.updateColumns(ds.columnNameList(), (i, c, v) -> N.toString(v));
        ds.println();
    }

    @Test
    public void test_containsColumn() throws Exception {
        final List<Account> accountList = createAccountList(Account.class, 7);

        final Dataset ds = N.newDataset(accountList);

        assertTrue(ds.containsColumn("firstName"));
        assertTrue(ds.containsAllColumns(N.asList("firstName", "lastName")));

        assertFalse(ds.containsColumn("Account.firstName"));
        assertFalse(ds.containsAllColumns(N.asList("firstName", "Account.lastName")));
    }

    @Test
    public void test_join() throws Exception {
        final List<Account> accountList = createAccountList(Account.class, 5);
        for (int i = 0; i < accountList.size(); i++) {
            accountList.get(i).setLastName("lastName" + i);
            accountList.get(i).setFirstName("firstName" + i);
        }

        accountList.add(accountList.get(1));
        accountList.add(accountList.get(3));

        final List<Account> accountList2 = createAccountList(Account.class, 5);
        for (int i = 0; i < accountList2.size(); i++) {
            accountList2.get(i).setLastName("lastName" + i);
            accountList2.get(i).setFirstName("firstName" + (i + 2));
        }

        accountList2.add(accountList2.get(1));
        accountList2.add(accountList2.get(3));

        final Dataset ds = N.newDataset(accountList);

        Dataset ds2 = N.newDataset(accountList2);
        ds2.removeColumn("gui");
        final Map<String, String> oldNewNames = new HashMap<>();
        for (final String columnName : ds2.columnNameList()) {
            oldNewNames.put(columnName, "right" + Strings.capitalize(columnName));
        }

        ds2.renameColumns(oldNewNames);

        N.println("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
        ds.println();

        ds2.println();

        N.println("++++++++++++++++++++++++++join+++++++++++++++++++++++++++++++++++++++++++");

        Dataset ds3 = ds.innerJoin(ds2, "firstName", "rightFirstName");
        ds3.println();
        assertEquals(6, ds3.size());

        N.println("+++++++++++++++++++++++++++leftJoin++++++++++++++++++++++++++++++++++++++++++");

        ds3 = ds.leftJoin(ds2, "firstName", "rightFirstName");
        ds3.println();
        assertEquals(9, ds3.size());

        N.println("+++++++++++++++++++++++++++++rightJoin++++++++++++++++++++++++++++++++++++++++");

        ds3 = ds.rightJoin(ds2, "firstName", "rightFirstName");
        ds3.println();
        assertEquals(9, ds3.size());

        N.println("+++++++++++++++++++++++++++++++fullJoin++++++++++++++++++++++++++++++++++++++");

        ds3 = ds.fullJoin(ds2, "firstName", "rightFirstName");
        ds3.println();
        assertEquals(12, ds3.size());

        Map<String, String> onColumnNames = N.asMap("firstName", "firstName");
        ds2 = N.newDataset(accountList2);

        N.println("+++++++++++++++++++++++++++++join++++++++++++++++++++++++++++++++++++++++");

        ds3 = ds.innerJoin(ds2, onColumnNames, "rigthtAccount", Account.class);
        ds3.println();
        assertEquals(6, ds3.size());

        ds3 = ds.innerJoin(ds2, onColumnNames, "rigthtAccount", Account.class, IntFunctions.ofList());
        ds3.println();
        assertEquals(4, ds3.size());

        N.println("++++++++++++++++++++++++++++++leftJoin+++++++++++++++++++++++++++++++++++++++");

        ds3 = ds.leftJoin(ds2, onColumnNames, "rigthtAccount", Account.class);
        ds3.println();
        assertEquals(9, ds3.size());

        ds3 = ds.leftJoin(ds2, onColumnNames, "rigthtAccount", Account.class, IntFunctions.ofList());
        ds3.println();
        assertEquals(7, ds3.size());

        N.println("+++++++++++++++++++++++++++rightJoin++++++++++++++++++++++++++++++++++++++++++");

        ds3 = ds.rightJoin(ds2, onColumnNames, "rigthtAccount", Account.class);
        ds3.println();
        assertEquals(9, ds3.size());

        ds3 = ds.rightJoin(ds2, onColumnNames, "rigthtAccount", Account.class, IntFunctions.ofList());
        ds3.println();
        assertEquals(6, ds3.size());

        N.println("++++++++++++++++++++++++++fullJoin+++++++++++++++++++++++++++++++++++++++++++");

        ds3 = ds.fullJoin(ds2, onColumnNames, "rigthtAccount", Account.class);
        ds3.println();
        assertEquals(12, ds3.size());

        ds3 = ds.fullJoin(ds2, onColumnNames, "rigthtAccount", Account.class, IntFunctions.ofList());
        ds3.println();
        assertEquals(9, ds3.size());

        N.println("++++++++++++++++++++++++++fullJoin+++++++++++++++++++++++++++++++++++++++++++");

        onColumnNames = N.asMap("firstName", "firstName", "middleName", "middleName");
        ds2 = N.newDataset(accountList2);

        N.println("+++++++++++++++++++++++++++++join++++++++++++++++++++++++++++++++++++++++");

        ds3 = ds.innerJoin(ds2, onColumnNames, "rigthtAccount", Account.class);
        ds3.println();
        assertEquals(6, ds3.size());

        ds3 = ds.innerJoin(ds2, onColumnNames, "rigthtAccount", Account.class, IntFunctions.ofList());
        ds3.println();
        assertEquals(4, ds3.size());

        N.println("++++++++++++++++++++++++++++++leftJoin+++++++++++++++++++++++++++++++++++++++");

        ds3 = ds.leftJoin(ds2, onColumnNames, "rigthtAccount", Account.class);
        ds3.println();
        assertEquals(9, ds3.size());

        ds3 = ds.leftJoin(ds2, onColumnNames, "rigthtAccount", Account.class, IntFunctions.ofList());
        ds3.println();
        assertEquals(7, ds3.size());

        N.println("+++++++++++++++++++++++++++rightJoin++++++++++++++++++++++++++++++++++++++++++");

        ds3 = ds.rightJoin(ds2, onColumnNames, "rigthtAccount", Account.class);
        ds3.println();
        assertEquals(9, ds3.size());

        ds3 = ds.rightJoin(ds2, onColumnNames, "rigthtAccount", Account.class, IntFunctions.ofList());
        ds3.println();
        assertEquals(6, ds3.size());

        N.println("++++++++++++++++++++++++++fullJoin+++++++++++++++++++++++++++++++++++++++++++");

        ds3 = ds.fullJoin(ds2, onColumnNames, "rigthtAccount", Account.class);
        ds3.println();
        assertEquals(12, ds3.size());

        ds3 = ds.fullJoin(ds2, onColumnNames, "rigthtAccount", Account.class, IntFunctions.ofList());
        ds3.println();
        assertEquals(9, ds3.size());
    }

    @Test
    public void test_leftJoin() throws Exception {
        final List<Account> accountList = createAccountList(Account.class, 7);
        for (int i = 0; i < accountList.size(); i++) {
            accountList.get(i).setFirstName("firstName" + i);
        }

        final List<Account> accountList2 = createAccountList(Account.class, 9);
        for (int i = 0; i < accountList2.size(); i++) {
            accountList2.get(i).setFirstName("firstName" + i);
        }

        final Dataset ds = N.newDataset(accountList);

        final Dataset ds2 = N.newDataset(accountList2);
        ds2.removeColumn("gui");
        final Map<String, String> oldNewNames = new HashMap<>();
        for (final String columnName : ds2.columnNameList()) {
            oldNewNames.put(columnName, "right" + Strings.capitalize(columnName));
        }

        ds2.renameColumns(oldNewNames);

        ds.println();
        ds2.println();

        Dataset joinedDataset = ds.leftJoin(ds2, "firstName", "rightFirstName");

        joinedDataset.println();

        {

            try {
                ds.leftJoin(ds2, "Account.firstName11", "Account.firstName");
                fail("SHould throw IllegalArgumentException");
            } catch (final IllegalArgumentException e) {

            }

            try {
                ds.leftJoin(ds2, "Account.firstName", "Account.firstName11");
                fail("SHould throw IllegalArgumentException");
            } catch (final IllegalArgumentException e) {

            }
        }

        Map<String, String> onColumnNames = N.asMap("firstName", "rightFirstName");
        joinedDataset = ds.leftJoin(ds2, onColumnNames);

        joinedDataset.println();

        ds2.renameColumn("rightFirstName", "firstName");

        joinedDataset = ds.leftJoin(ds2, "firstName", "firstName");

        joinedDataset.println();

        ds2.renameColumn("firstName", "rightFirstName");

        joinedDataset = ds.leftJoin(ds2, "firstName", "rightFirstName");

        joinedDataset.println();

        onColumnNames = N.asMap("firstName", "rightFirstName", "lastName", "rightLastName");

        joinedDataset = ds.leftJoin(ds2, onColumnNames);

        joinedDataset.println();

        joinedDataset = ds.leftJoin(ds2, onColumnNames, "account", Account.class);

        joinedDataset.println();

        try {
            joinedDataset = ds.leftJoin(ds2, (Map<String, String>) null, "account", Account.class);
            fail("SHould throw IllegalArgumentException");
        } catch (final IllegalArgumentException e) {

        }

        try {
            joinedDataset = ds.leftJoin(ds2, onColumnNames, "firstName", Account.class);
            fail("SHould throw IllegalArgumentException");
        } catch (final IllegalArgumentException e) {

        }

        joinedDataset = ds.leftJoin(ds2, onColumnNames, "account", Account.class, IntFunctions.ofList());

        joinedDataset.println();

        joinedDataset = ds.leftJoin(ds2, onColumnNames, "account", Account.class, IntFunctions.ofSet());

        joinedDataset.println();

        try {
            joinedDataset = ds.leftJoin(ds2, (Map<String, String>) null, "account", Account.class, IntFunctions.ofList());
            fail("SHould throw IllegalArgumentException");
        } catch (final IllegalArgumentException e) {

        }

        try {
            joinedDataset = ds.leftJoin(ds2, onColumnNames, "firstName", Account.class, IntFunctions.ofList());
            fail("SHould throw IllegalArgumentException");
        } catch (final IllegalArgumentException e) {

        }
    }

    @Test
    public void test_union() throws Exception {
        final List<Account> accountList = createAccountList(Account.class, 9);
        final Dataset ds = N.newDataset(accountList);

        Dataset ds2 = ds.copy();

        Dataset ds3 = ds.union(ds2);

        ds3.println();

        assertEquals(ds.size(), ds3.size());

        ds3 = ds.unionAll(ds2);

        ds3.println();

        assertEquals(ds.size() * 2, ds3.size());

        ds2 = N.newDataset(createAccountList(Account.class, 9));

        ds3 = ds.union(ds2);

        ds3.println();

        assertEquals(ds.size() * 2, ds3.size());

        ds3 = ds.unionAll(ds2);

        ds3.println();

        assertEquals(ds.size() * 2, ds3.size());
    }

    @Test
    public void test_union_2() throws Exception {
        final List<Account> accountList = createAccountList(Account.class, 9);
        final Dataset ds = N.newDataset(accountList);

        Dataset ds2 = ds.copy();

        ds.removeColumn(ds.getColumnName(2));

        Dataset ds3 = ds.union(ds2);

        ds3.println();

        assertEquals(ds.size(), ds3.size());

        ds3 = ds.unionAll(ds2);

        ds3.println();

        assertEquals(ds.size() * 2, ds3.size());

        ds2 = N.newDataset(createAccountList(Account.class, 9));

        ds3 = ds.union(ds2);

        ds3.println();

        assertEquals(ds.size() * 2, ds3.size());

        ds3 = ds.unionAll(ds2);

        ds3.println();

        assertEquals(ds.size() * 2, ds3.size());
    }

    @Test
    public void test_union_3() throws Exception {
        final List<Account> accountList = createAccountList(Account.class, 9);
        final Dataset ds = N.newDataset(accountList);

        Dataset ds2 = ds.copy();

        ds2.removeColumn(ds2.getColumnName(2));

        Dataset ds3 = ds.union(ds2);

        ds3.println();

        assertEquals(ds.size(), ds3.size());

        ds3 = ds.unionAll(ds2);

        ds3.println();

        assertEquals(ds.size() * 2, ds3.size());

        ds2 = N.newDataset(createAccountList(Account.class, 9));

        ds2.removeColumn(ds2.getColumnName(2));

        ds.println();
        ds2.println();
        ds3 = ds.union(ds2);

        ds3.println();

        assertEquals(ds.size() * 2, ds3.size());

        ds3 = ds.unionAll(ds2);

        ds3.println();

        assertEquals(ds.size() * 2, ds3.size());
    }

    @Test
    public void test_interset_difference() throws Exception {
        final List<Account> accountList = createAccountList(Account.class, 9);
        final Dataset ds = N.newDataset(accountList);

        final Dataset ds2 = ds.copy();

        Dataset ds3 = ds.intersection(ds2);

        ds.println();
        ds3.println();

        assertEquals(ds, ds3);

        ds3 = ds.difference(ds2);

        ds.println();
        ds3.println();

        assertEquals(0, ds3.size());

        ds2.clear();

        ds3 = ds.intersection(ds2);

        ds.println();
        ds3.println();

        assertEquals(0, ds3.size());

        ds3 = ds.difference(ds2);

        ds.println();
        ds3.println();

        assertEquals(ds, ds3);

        ds.clear();

        ds3 = ds.intersection(ds2);

        ds.println();
        ds3.println();

        assertEquals(0, ds3.size());

        ds3 = ds.difference(ds2);

        ds.println();
        ds3.println();

        assertEquals(ds, ds3);
    }

    @Test
    public void test_interset_except_2() throws Exception {
        final List<Account> accountList = createAccountList(Account.class, 9);
        final Dataset ds = N.newDataset(accountList);

        final Dataset ds2 = ds.copy();

        ds.removeColumn(ds.getColumnName(2));

        Dataset ds3 = ds.intersection(ds2);

        ds.println();
        ds3.println();

        assertEquals(ds, ds3);

        ds3 = ds.difference(ds2);

        ds.println();
        ds3.println();

        assertEquals(0, ds3.size());

        ds2.clear();

        ds3 = ds.intersection(ds2);

        ds.println();
        ds3.println();

        assertEquals(0, ds3.size());

        ds3 = ds.difference(ds2);

        ds.println();
        ds3.println();

        assertEquals(ds, ds3);

        ds.clear();

        ds3 = ds.intersection(ds2);

        ds.println();
        ds3.println();

        assertEquals(0, ds3.size());

        ds3 = ds.difference(ds2);

        ds.println();
        ds3.println();

        assertEquals(ds, ds3);
    }

    @Test
    public void test_interset_except_3() throws Exception {
        final List<Account> accountList = createAccountList(Account.class, 9);
        final Dataset ds = N.newDataset(accountList);

        final Dataset ds2 = ds.copy();

        ds2.removeColumn(ds2.getColumnName(2));

        Dataset ds3 = ds.intersection(ds2);

        ds.println();
        ds3.println();

        assertEquals(ds, ds3);

        ds3 = ds.difference(ds2);

        ds.println();
        ds3.println();

        assertEquals(0, ds3.size());

        ds2.clear();

        ds3 = ds.intersection(ds2);

        ds.println();
        ds3.println();

        assertEquals(0, ds3.size());

        ds3 = ds.difference(ds2);

        ds.println();
        ds3.println();

        assertEquals(ds, ds3);

        ds.clear();

        ds3 = ds.intersection(ds2);

        ds.println();
        ds3.println();

        assertEquals(0, ds3.size());

        ds3 = ds.difference(ds2);

        ds.println();
        ds3.println();

        assertEquals(ds, ds3);
    }

    @Test
    public void test_first_last_row() throws Exception {
        final List<Account> accountList = createAccountList(Account.class, 9);
        final Dataset ds = N.newDataset(accountList);

        N.println(ds.firstRow());
        N.println(ds.lastRow());

        ds.clear();

        assertFalse(ds.firstRow().isPresent());
        assertFalse(ds.lastRow().isPresent());
    }

    @Test
    public void test_get_set() throws Exception {
        final List<Account> accountList = createAccountList(Account.class, 9);
        final Dataset ds = N.newDataset(accountList);

        N.println(ds.get(1, 1));

        final String newValue = "abc123";

        ds.set(1, 1, newValue);

        assertEquals(newValue, ds.get(1, 1));

        assertFalse(ds.absolute(1).isNull(1));
        assertFalse(ds.absolute(1).isNull(ds.getColumnName(1)));

        ds.set(1, 1, null);

        assertTrue(ds.absolute(1).isNull(1));
        assertTrue(ds.absolute(1).isNull(ds.getColumnName(1)));
    }

    @Test
    public void test_combineColumn() throws Exception {
        final List<Account> accountList = createAccountList(Account.class, 9);
        final Dataset ds = N.newDataset(accountList);

        ds.println();

        ds.combineColumns(N.asList("firstName", "lastName"), "name", Object[].class);

        ds.println();

        assertTrue(ds.getColumnIndex("name") >= 0);
        assertFalse(ds.containsColumn("firstName"));
        assertFalse(ds.containsColumn("lastName"));
    }

    @Test
    public void test_removeColumn() throws Exception {
        final List<Account> accountList = createAccountList(Account.class, 9);
        final Dataset ds = N.newDataset(accountList);

        ds.println();

        assertTrue(ds.getColumnIndex("firstName") >= 0);
        assertTrue(ds.getColumnIndex("lastName") >= 0);
        assertTrue(ds.getColumnIndex("birthDate") >= 0);

        ds.removeColumns(N.asList("firstName", "lastName", "birthDate"));

        ds.println();

        assertFalse(ds.containsColumn("firstName"));
        assertFalse(ds.containsColumn("lastName"));
    }

    @Test
    public void test_asDataset() throws Exception {
        final List<String> columnNameList = new ArrayList<>(Beans.getPropNameList(Account.class));

        final List<Account> accountList = createAccountList(Account.class, 1000);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < 3; i++) {
            final Dataset dataset = N.newDataset(columnNameList, accountList);
            assertEquals(accountList.size(), dataset.size());
        }

        N.println("It took " + (System.currentTimeMillis() - startTime) + " to convert " + accountList.size() + " bean to Dataset");

        final Dataset dataset = N.newDataset(columnNameList, accountList);

        startTime = System.currentTimeMillis();

        for (int i = 0; i < 3; i++) {
            final List<?> list = dataset.toList(Map.class);

            assertEquals(accountList.size(), list.size());
        }

        N.println("It took " + (System.currentTimeMillis() - startTime) + " to convert " + dataset.size() + " Dataset to account");

        startTime = System.currentTimeMillis();

        for (int i = 0; i < 3; i++) {
            dataset.sortBy("gui");
        }

        N.println("It took " + (System.currentTimeMillis() - startTime) + " to sort " + dataset.size() + " Dataset");

        final Map<String, Object> props = createAccountProps();
        final Dataset ds = N.newDataset("propName", "propValue", props);
        ds.println();
    }

    @Test
    public void test_asDataset_3() throws Exception {
        final long startTime = System.currentTimeMillis();

        final List<Map<String, Object>> propsList = createAccountPropsList(1000);
        N.println(System.currentTimeMillis() - startTime);

        final Dataset ds = N.newDataset(new ArrayList<>(propsList.get(0).keySet()), propsList);
        N.println(System.currentTimeMillis() - startTime);

        ds.groupBy(N.asList("Account.firstName", "Account.lastName"));
        N.println(System.currentTimeMillis() - startTime);
    }

    @Test
    public void test_distinct() throws Exception {
        final List<Account> accountList = createAccountList(Account.class, 1000);
        final Dataset ds = N.newDataset(accountList);
        Dataset ds2 = ds.distinct();
        ds2.println();
        assertEquals(accountList.size(), ds2.size());

        ds2 = ds.distinct();
        ds2.println();
        assertEquals(accountList.size(), ds2.size());

        ds2 = ds.distinctBy("gui");
        ds2.println();
        assertEquals(accountList.size(), ds2.size());

        ds2 = ds.distinctBy("gui", (Function<String, Object>) t -> t.substring(0, 2));
        ds2.println();

        ds2 = ds.distinctBy(N.asList("firstName", "lastName"), (Function<DisposableObjArray, Object>) DisposableObjArray::length);
        ds2.println();
        assertEquals(1, ds2.size());

        ds2 = ds.groupBy(N.asList("firstName", "lastName"), (Function<DisposableObjArray, Object>) DisposableObjArray::length);
        ds2.println();
        assertEquals(1, ds2.size());

        ds2 = ds.groupBy("gui", (Function<String, Object>) t -> t.substring(0, 2), "gui", "*", Collectors.counting());
        ds2.println();

        ds2 = ds.groupBy("gui", (Function<String, Object>) t -> t.substring(0, 2), N.asList("gui"), "*", Collectors.counting());
        ds2.println();

        ds2 = ds.groupBy(N.asList("firstName", "lastName"), (Function<DisposableObjArray, Object>) DisposableObjArray::length, "gui", "*",
                Collectors.counting());

        ds2.println();
        assertEquals(1, ds2.size());

        ds2 = ds.groupBy(N.asList("firstName", "lastName"), (Function<DisposableObjArray, Object>) DisposableObjArray::length, N.asList("gui"), "*",
                Collectors.counting());

        ds2 = ds.groupBy(N.asList("firstName", "lastName"), (Function<DisposableObjArray, Object>) DisposableObjArray::length,
                N.asList("firstName", "lastName"), "*", Collectors.counting());

        ds2.println();
        assertEquals(1, ds2.size());

        ds2 = ds.groupBy(N.asList("firstName", "lastName"), (Function<DisposableObjArray, Object>) DisposableObjArray::length,
                N.asList("firstName", "lastName"), "*", Collectors.counting());

        ds2.println();
        assertEquals(1, ds2.size());

        ds2 = ds.distinctBy(N.asList("gui"));
        ds2.println();
        assertEquals(accountList.size(), ds2.size());

        ds2 = ds.distinctBy(N.asList("firstName", "lastName", "gui"));
        ds2.println();
        assertEquals(accountList.size(), ds2.size());

        ds2 = ds.distinctBy(N.asList("firstName", "lastName", "gui"));
        ds2.println();
        assertEquals(accountList.size(), ds2.size());

        ds2 = ds.distinctBy(N.asList("firstName", "lastName"));
        ds2.println();
        assertEquals(1, ds2.size());

        ds2 = ds.distinctBy(N.asList("firstName", "lastName"));
        ds2.println();
        assertEquals(1, ds2.size());
    }

    @Test
    public void test_sort_perf() throws Exception {
        final List<String> columnNameList = new ArrayList<>(Beans.getPropNameList(Account.class));
        final Dataset dataset = N.newDataset(columnNameList, createAccountList(Account.class, 999));

        Profiler.run(8, 10, 1, () -> {
            final Dataset copy = dataset.copy();

            copy.sortBy(N.asList(Account.GUI, Account.FIRST_NAME));

        }).printResult();

    }

    static final AtomicInteger counter = new AtomicInteger();

    void addDataType() {
        final DataType dataType = new DataType();
        dataType.setByteType((byte) 1);
        dataType.setCharType((char) 50);
        dataType.setBooleanType(true);
        dataType.setShortType(Short.MAX_VALUE);
        dataType.setIntType(counter.getAndIncrement());
        dataType.setLongType(0);
        dataType.setFloatType(0.00000000f);
        dataType.setDoubleType(000000000000000000000000);
        dataType.setStringType("String");

        final ArrayList<String> stringArrayList = new ArrayList<>();
        stringArrayList.add("aa");
        stringArrayList.add("黎");
        stringArrayList.add("cc");
        dataType.setStringArrayListType(stringArrayList);

        final LinkedList<Boolean> booleanLinkedList = new LinkedList<>();
        booleanLinkedList.add(false);
        booleanLinkedList.add(false);
        booleanLinkedList.add(true);
        dataType.setBooleanLinkedListType(booleanLinkedList);

        final Vector<String> stringVector = new Vector<>();
        stringVector.add("false");
        dataType.setStringVectorType(stringVector);

        final Map<BigDecimal, String> bigDecimalHashMap = new HashMap<>();

        bigDecimalHashMap.put(BigDecimal.valueOf(3993.000), "3993.000");
        bigDecimalHashMap.put(BigDecimal.valueOf(3993.001), "3993.001");

        final HashMap<Timestamp, Float> timestampHashMap = new HashMap<>();

        timestampHashMap.put(new Timestamp(System.currentTimeMillis()), 3993.000f);
        timestampHashMap.put(new Timestamp(System.currentTimeMillis()), 3993.001f);
        dataType.setTimestampHashMapType(timestampHashMap);

        final ConcurrentHashMap<BigDecimal, String> StringConcurrentHashMap = new ConcurrentHashMap<>();
        StringConcurrentHashMap.put(BigDecimal.valueOf(3993.000), "3993.000");
        StringConcurrentHashMap.put(BigDecimal.valueOf(3993.001), "3993.001");
        dataType.setStringConcurrentHashMapType(StringConcurrentHashMap);

        dataType.setByteArrayType(new byte[] { 1, 2, 3 });
        dataType.setDateType(new Date(System.currentTimeMillis()));
        dataType.setTimeType(new Time(System.currentTimeMillis()));
        dataType.setTimestampType(new Timestamp(System.currentTimeMillis()));
    }

}
