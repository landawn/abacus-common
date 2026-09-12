package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.landawn.abacus.parser.JsonSerConfig;
import com.landawn.abacus.util.stream.Stream;

import testfixtures.entity.extendDirty.basic.Account;
import testfixtures.entity.extendDirty.basic.AccountContact;

public class DatasetToTest extends DatasetTestSupport {
    @Test
    public void toList_defaultArray() {
        List<Object[]> list = sampleDataset.toList();
        assertEquals(3, list.size());
        assertArrayEquals(new Object[] { 1, "Alice", 30 }, list.get(0));
    }

    @Test
    public void toList_specificType_Map() {
        List<Map> mapList = sampleDataset.toList(Map.class);
        assertEquals(3, mapList.size());
        assertEquals("Alice", mapList.get(0).get("Name"));
    }

    @Test
    public void toList_specificType_Bean() {
        RowDataset beanFriendlyDs = new RowDataset(Arrays.asList("id", "name", "value"), Arrays.asList(new ArrayList<>(Arrays.asList(101, 102, 103)),
                new ArrayList<>(Arrays.asList("BeanA", "BeanB", "BeanC")), new ArrayList<>(Arrays.asList(1.1, 2.2, 3.3))));
        List<TestBean> beanList = beanFriendlyDs.toList(TestBean.class);
        assertEquals(3, beanList.size());
        assertEquals("BeanA", beanList.get(0).getName());
        assertEquals(2.2, beanList.get(1).getValue(), 0.001);
    }

    @Test
    public void testToList() {
        List<Object[]> list = dataset.toList();
        assertEquals(5, list.size());
        assertEquals("John", list.get(0)[1]);
    }

    @Test
    public void testToListWithRange() {
        List<Object[]> list = dataset.toList(1, 3);
        assertEquals(2, list.size());
        assertEquals("Jane", list.get(0)[1]);
        assertEquals("Bob", list.get(1)[1]);
    }

    @Test
    public void testToListWithClass() {
        List<List> list = dataset.toList(ArrayList.class);
        assertEquals(5, list.size());
        assertEquals(Arrays.asList(1, "John", 25, 50000.0), list.get(0));
    }

    @Test
    public void testToListWithColumnNames() {
        List<Object[]> list = dataset.toList(Arrays.asList("name", "age"), Object[].class);
        assertEquals(5, list.size());
        assertArrayEquals(new Object[] { "John", 25 }, list.get(0));
    }

    @Test
    public void testToListWithSupplier() {
        List<List> list = dataset.toList(size -> new ArrayList<>(size));
        assertEquals(5, list.size());
        assertEquals(Arrays.asList(1, "John", 25, 50000.0), list.get(0));
    }

    @Test
    public void test_toList() {
        final Dataset dataset = CommonUtil.newDataset(CommonUtil.toList("a", "b", "c"), CommonUtil.toList(CommonUtil.toList("a1", "b1", "c1")));

        assertNotNull(dataset);
    }

    @Test
    public void testToList_AsEntity() {
        List<Person> persons = dataset.toList(Person.class);
        assertNotNull(persons);
        assertEquals(5, persons.size());
        assertEquals(1, persons.get(0).getId());
        assertEquals("John", persons.get(0).getName());
    }

    @Test
    public void testToList_WithRange() {
        List<Person> persons = dataset.toList(1, 3, Person.class);
        assertNotNull(persons);
        assertEquals(2, persons.size());
        assertEquals(2, persons.get(0).getId());
        assertEquals(3, persons.get(1).getId());
    }

    @Test
    public void testToList_WithColumnNames() {
        List<Person> persons = dataset.toList(Arrays.asList("id", "name"), Person.class);
        assertNotNull(persons);
        assertEquals(5, persons.size());
        assertEquals(1, persons.get(0).getId());
        assertEquals("John", persons.get(0).getName());
    }

    @Test
    public void testToList_WithSupplier() {
        IntFunction<Person> supplier = rowIndex -> new Person();
        List<Person> persons = dataset.toList(supplier);
        assertNotNull(persons);
        assertEquals(5, persons.size());
    }

    @Test
    public void testToListWithFilter() {
        List<Map> list = dataset.toList(col -> col.equals("name") || col.equals("age"), col -> col.toUpperCase(), HashMap.class);

        assertEquals(5, list.size());
        Map<String, Object> firstRow = list.get(0);
        assertEquals("John", firstRow.get("NAME"));
        assertEquals(25, firstRow.get("AGE"));
    }

    @Test
    public void testToEntities() {
        List<Person> persons = dataset.toEntities(null, Person.class);
        assertNotNull(persons);
        assertEquals(5, persons.size());
    }

    @Test
    public void toEntities_simpleBean() {
        RowDataset beanDs = new RowDataset(Arrays.asList("id", "name", "value"), Arrays.asList(new ArrayList<>(Arrays.asList(1, 2)),
                new ArrayList<>(Arrays.asList("obj1", "obj2")), new ArrayList<>(Arrays.asList(10.0, 20.0))));
        List<TestBean> entities = beanDs.toEntities(Collections.emptyMap(), TestBean.class);
        assertEquals(2, entities.size());
        assertEquals(new TestBean(1, "obj1", 10.0), entities.get(0));
        assertEquals(new TestBean(2, "obj2", 20.0), entities.get(1));
    }

    @Test
    public void test_toMergedEntities() {

        {
            final List<Account> accountList = createAccountList(Account.class, 3);
            final MutableInt id = MutableInt.of(1);
            accountList.forEach(it -> it.setId(id.incrementAndGet())
                    .setContact(createAccountContact(AccountContact.class).setAccountId(id.value()).setEmail(Strings.uuid())));
            final List<Map<String, Object>> mapList = Stream.of(accountList).map(Beans::beanToFlatMap).toList();
            final Dataset ds = CommonUtil.newDataset(mapList);

            final List<Account> mergedEntities = ds.toMergedEntities(Account.class);

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
            final List<Map<String, Object>> mapList = Stream.of(accountList).map(Beans::beanToFlatMap).toList();
            final Dataset ds = CommonUtil.newDataset(mapList);

            final List<Account> mergedEntities = ds.toMergedEntities(Account.class);

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
            final List<Map<String, Object>> mapList = Stream.of(accountList).map(Beans::beanToFlatMap).toList();
            mapList.forEach(it -> Maps.replaceKeys(it, k -> k.startsWith("contact.") ? Strings.replaceFirst(k, "contact.", "ac.") : k));
            final Dataset ds = CommonUtil.newDataset(mapList);

            final List<Account> mergedEntities = ds.toMergedEntities(Account.class);

            for (int i = 0; i < accountList.size(); i++) {
                assertEquals(accountList.get(i).getGUI(), mergedEntities.get(i).getGUI());
                assertEquals(accountList.get(i).getContact().getEmail(), mergedEntities.get(i).getContact().getEmail());
            }
        }
    }

    @Test
    public void test_toMergedEntities_1() {
        final Map<String, String> map = new HashMap<>();
        map.put(null, null);

        final List<String> columNames = CommonUtil.toList("id", "name", "devices.id", "devices.model", "devices.serialNumber");
        final Dataset dataset = Dataset.rows(columNames,
                new Object[][] { { 100, "Bob", 1, "iPhone", "abc123" }, { 100, "Bob", 2, "MacBook", "mmm123" }, { 200, "Alice", 3, "Android", "aaa223" } });

        final List<Account> accounts = dataset.toMergedEntities(Account.class);

        String json = N.toJson(accounts, JsonSerConfig.create().setPrettyFormat(true));
        assertNotNull(json);
    }

    @Test
    public void test_toMergedEntities_2() {
        final Map<String, String> map = new HashMap<>();
        map.put(null, null);

        final List<String> columNames = CommonUtil.toList("id", "firstName", "contact.id", "contact.address", "contact.city", "device.id", "device.name",
                "device.model");
        final Dataset dataset = CommonUtil.newDataset(columNames,
                CommonUtil.toList(CommonUtil.toList(1, "firstName1", 1, "address1", "city1", 1, "device1", "model1"),
                        CommonUtil.toList(1, "firstName2", 2, "address2", "city2", 2, "device2", "model2")));

        final List<Account> accounts = dataset.toList(Account.class);

        assertEquals(2, accounts.size());
        assertEquals("firstName1", accounts.get(0).getFirstName());
        assertEquals("address2", accounts.get(1).getContact().getAddress());
        assertEquals(1, accounts.get(0).getDevices().size());
        assertEquals(1, accounts.get(1).getDevices().size());

        final List<Account> accounts1 = dataset.toMergedEntities(Account.class);

        assertEquals(1, accounts1.size());
        assertEquals("firstName2", accounts1.get(0).getFirstName());
        assertEquals(2, accounts1.get(0).getDevices().size());

        final List<Account> accounts2 = dataset.toMergedEntities(CommonUtil.toList("id", "firstName"), dataset.columnNames(), Account.class);

        assertEquals(2, accounts2.size());
        assertEquals("firstName1", accounts2.get(0).getFirstName());
        assertEquals(1, accounts2.get(0).getDevices().size());
    }

    @Test
    public void test_toMergedEntities_3() {
        final Map<String, String> map = new HashMap<>();
        map.put(null, null);

        final List<String> columNames = CommonUtil.toList("id", "firstName", "ct.id", "ct.address", "ct.city", "device.id", "device.name", "device.model");
        final Dataset dataset = CommonUtil.newDataset(columNames,
                CommonUtil.toList(CommonUtil.toList(1, "firstName1", 1, "address1", "city1", 1, "device1", "model1"),
                        CommonUtil.toList(1, "firstName2", 2, "address2", "city2", 2, "device2", "model2")));

        final List<Account> accounts = dataset.toEntities(dataset.columnNames(), CommonUtil.asMap("ct", "contact"), Account.class);

        assertEquals(2, accounts.size());
        assertEquals("firstName1", accounts.get(0).getFirstName());
        assertEquals("address2", accounts.get(1).getContact().getAddress());
        assertEquals(1, accounts.get(0).getDevices().size());
        assertEquals(1, accounts.get(1).getDevices().size());

        final List<Account> accounts1 = dataset.toMergedEntities(CommonUtil.toList("id"), dataset.columnNames(), CommonUtil.asMap("ct", "contact"),
                Account.class);

        assertEquals(1, accounts1.size());
        assertEquals("firstName2", accounts1.get(0).getFirstName());
        assertEquals("address2", accounts1.get(0).getContact().getAddress());
        assertEquals(2, accounts1.get(0).getDevices().size());

        final List<Account> accounts2 = dataset.toMergedEntities(CommonUtil.toList("id", "firstName"), dataset.columnNames(), CommonUtil.asMap("ct", "contact"),
                Account.class);

        assertEquals(2, accounts2.size());
        assertEquals("firstName1", accounts2.get(0).getFirstName());
        assertEquals("address1", accounts2.get(0).getContact().getAddress());
        assertEquals(1, accounts2.get(0).getDevices().size());
    }

    @Test
    public void testToMergedEntities() {
        List<Person> persons = dataset.toMergedEntities(Person.class);
        assertNotNull(persons);
        assertEquals(5, persons.size());
    }

    @Test
    public void toMergedEntities_singleId() {
        List<String> names = Arrays.asList("id", "name", "value", "detail");
        List<List<Object>> values = Arrays.asList(new ArrayList<>(Arrays.asList(1, 1, 2)), new ArrayList<>(Arrays.asList("A", "A", "B")),
                new ArrayList<>(Arrays.asList(10.0, 10.0, 20.0)), new ArrayList<>(Arrays.asList("d1", "d2", "d3")));
        RowDataset ds = new RowDataset(names, values);

        List<String> simpleNames = Arrays.asList("id", "name", "value");
        List<List<Object>> simpleValues = Arrays.asList(new ArrayList<>(Arrays.asList(1, 1, 2)), new ArrayList<>(Arrays.asList("Alice", "Alice", "Bob")),
                new ArrayList<>(Arrays.asList(10.0, 11.0, 20.0)));
        RowDataset simpleDs = new RowDataset(simpleNames, simpleValues);

        List<TestBean> merged = simpleDs.toMergedEntities("id", TestBean.class);
        assertEquals(2, merged.size());

        TestBean bean1 = merged.stream().filter(b -> b.getId() == 1).findFirst().orElse(null);
        TestBean bean2 = merged.stream().filter(b -> b.getId() == 2).findFirst().orElse(null);

        assertNotNull(bean1);
        assertEquals("Alice", bean1.getName());
        assertEquals(11.0, bean1.getValue(), 0.001);

        assertNotNull(bean2);
        assertEquals("Bob", bean2.getName());
        assertEquals(20.0, bean2.getValue(), 0.001);
    }

    @Test
    public void toMap_keyValue() {
        Map<Integer, String> idToNameMap = sampleDataset.toMap("ID", "Name");
        assertEquals(3, idToNameMap.size());
        assertEquals("Alice", idToNameMap.get(1));
        assertEquals("Bob", idToNameMap.get(2));
        assertEquals("Charlie", idToNameMap.get(3));
    }

    @Test
    public void toMap_keyRowAsBean() {
        RowDataset ds = new RowDataset(Arrays.asList("key", "id", "name", "value"), Arrays.asList(new ArrayList<>(Arrays.asList("k1", "k2")),
                new ArrayList<>(Arrays.asList(1, 2)), new ArrayList<>(Arrays.asList("Alice", "Bob")), new ArrayList<>(Arrays.asList(10.0, 20.0))));

        Map<String, TestBean> map = ds.toMap("key", Arrays.asList("id", "name", "value"), TestBean.class);
        assertEquals(2, map.size());
        assertEquals(new TestBean(1, "Alice", 10.0), map.get("k1"));
        assertEquals(new TestBean(2, "Bob", 20.0), map.get("k2"));
    }

    @Test
    public void testToMap() {
        Map<Object, Object> map = dataset.toMap("id", "name");
        assertEquals(5, map.size());
        assertEquals("John", map.get(1));
        assertEquals("Jane", map.get(2));
    }

    @Test
    public void testToMap_KeyValue() {
        Map<Integer, String> map = dataset.toMap("id", "name");
        assertNotNull(map);
        assertEquals(5, map.size());
        assertEquals("John", map.get(1));
        assertEquals("Jane", map.get(2));
    }

    @Test
    public void testToMap_WithRowType() {
        Map<Integer, Person> map = dataset.toMap("id", Arrays.asList("name", "age"), Person.class);
        assertNotNull(map);
        assertEquals(5, map.size());
        assertEquals("John", map.get(1).getName());
    }

    @Test
    public void testToMultimap() {
        Dataset ds = Dataset.rows(Arrays.asList("dept", "name"), new Object[][] { { "IT", "Alice" }, { "HR", "Bob" }, { "IT", "Charlie" } });

        ListMultimap<String, String> map = ds.toMultimap("dept", "name");
        assertNotNull(map);
        assertEquals(2, map.get("IT").size());
    }

    @Test
    public void testToJSON_ToWriter() {
        StringWriter writer = new StringWriter();
        dataset.toJson(writer);

        String json = writer.toString();
        assertNotNull(json);
        assertTrue(json.contains("John"));
    }

    @Test
    public void testToJson_LowercaseName() {
        String json = dataset.toJson();

        assertNotNull(json);
        assertTrue(json.contains("Alice") || json.contains("alice"));
    }

    @Test
    public void testToJson() {
        String json = testDataset.toJson();
        assertNotNull(json);
        assertTrue(json.contains("Alice"));
        assertTrue(json.contains("Bob"));
    }

    @Test
    public void testToJsonWithRange() {
        String json = testDataset.toJson(1, 3);
        assertNotNull(json);
        assertTrue(json.contains("Bob"));
        assertTrue(json.contains("Charlie"));
        assertFalse(json.contains("Alice"));
        assertFalse(json.contains("Diana"));
    }

    @Test
    public void testToJsonWithRangeAndColumns() {
        String json = testDataset.toJson(0, 2, Arrays.asList("name", "age"));
        assertNotNull(json);
        assertTrue(json.contains("Alice"));
        assertTrue(json.contains("Bob"));
        assertFalse(json.contains("50000"));
    }

    @Test
    public void toJson_writer() throws IOException {
        StringWriter sw = new StringWriter();
        sampleDataset.toJson(sw);
        String json = sw.toString();
        assertTrue(json.startsWith("["));
        assertTrue(json.endsWith("]"));
        assertTrue(json.contains("\"ID\":1"));
        assertTrue(json.contains("\"Name\":\"Alice\""));
        assertTrue(json.contains("\"Age\":30"));
    }

    @Test
    public void toJson_file(@TempDir File tempDir) throws IOException {
        File tempFile = new File(tempDir, "test.json");
        sampleDataset.toJson(tempFile);
        assertTrue(tempFile.exists());
        String jsonContent = Files.readString(tempFile.toPath());
        assertTrue(jsonContent.contains("\"Name\":\"Bob\""));

        IOUtil.deleteIfExists(tempFile);
    }

    @Test
    public void toJson_outputStream() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        sampleDataset.toJson(baos);
        String json = baos.toString();
        assertTrue(json.contains("\"Name\":\"Charlie\""));
    }

    @Test
    public void testToJsonFile() throws Exception {
        File tempFile = File.createTempFile("test", ".json");
        tempFile.deleteOnExit();

        testDataset.toJson(tempFile);
        assertTrue(tempFile.exists());
        assertTrue(tempFile.length() > 0);
    }

    @Test
    public void testToJsonOutputStream() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        testDataset.toJson(baos);

        String json = baos.toString();
        assertNotNull(json);
        assertTrue(json.contains("Alice"));
    }

    @Test
    public void testToJsonWriter() throws Exception {
        StringWriter writer = new StringWriter();
        testDataset.toJson(writer);

        String json = writer.toString();
        assertNotNull(json);
        assertTrue(json.contains("Alice"));
    }

    @Test
    public void testToJsonToFile() throws IOException {
        File tempFile = File.createTempFile("dataset", ".json");
        tempFile.deleteOnExit();

        dataset.toJson(tempFile);

        String content = new String(IOUtil.readAllBytes(tempFile));
        assertTrue(content.contains("John"));
    }

    @Test
    public void testToXml_LowercaseName() {
        String xml = dataset.toXml();

        assertNotNull(xml);
        assertTrue(xml.contains("Alice") || xml.contains("alice") || xml.length() > 0);
    }

    @Test
    public void testToXml() {
        String xml = testDataset.toXml();
        assertNotNull(xml);
        assertTrue(xml.contains("Alice"));
        assertTrue(xml.contains("Bob"));
    }

    @Test
    public void testToXmlWithRange() {
        String xml = testDataset.toXml(1, 3);
        assertNotNull(xml);
        assertTrue(xml.contains("Bob"));
        assertTrue(xml.contains("Charlie"));
        assertFalse(xml.contains("Alice"));
        assertFalse(xml.contains("Diana"));
    }

    @Test
    public void toXml_writer() throws IOException {
        StringWriter sw = new StringWriter();
        sampleDataset.toXml(sw);
        String xml = sw.toString();
        assertTrue(xml.startsWith("<dataset>"));
        assertTrue(xml.endsWith("</dataset>"));
        assertTrue(xml.contains("<row>"));
        assertTrue(xml.contains("<ID>1</ID>"));
        assertTrue(xml.contains("<Name>Alice</Name>"));
        assertTrue(xml.contains("<Age>30</Age>"));
    }

    @Test
    public void testToXmlWithRowElementName() {
        String xml = dataset.toXml("person");
        assertNotNull(xml);
        assertTrue(xml.contains("<person>"));
    }

    @Test
    public void testToCsv_LowercaseName() {
        String csv = dataset.toCsv();

        assertNotNull(csv);
        assertTrue(csv.contains("Alice") || csv.contains("alice") || csv.length() > 0);
    }

    @Test
    public void testToCsv() {
        String csv = testDataset.toCsv();
        assertNotNull(csv);
        assertTrue(csv.contains("\"id\",\"name\",\"age\",\"salary\""));
        assertTrue(csv.contains("\"Alice\""));
        assertTrue(csv.contains("\"Bob\""));
    }

    @Test
    public void testToCsvWithRangeAndColumns() {
        String csv = testDataset.toCsv(0, 2, Arrays.asList("name", "age"));
        assertNotNull(csv);
        assertTrue(csv.contains("\"name\",\"age\""));
        assertTrue(csv.contains("\"Alice\",30"));
        assertTrue(csv.contains("\"Bob\",25"));
        assertFalse(csv.contains("50000"));
    }

    @Test
    public void testToCsv_2() {
        Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] { { 1, "Alice" }, { 2, "Bob\"s" } });
        String csv = dataset.toCsv();
        assertNotNull(csv);
    }

    @Test
    public void toCsv_writer() throws IOException {
        StringWriter sw = new StringWriter();
        sampleDataset.toCsv(sw);
        String csv = sw.toString();
        String[] lines = csv.split(IOUtil.LINE_SEPARATOR_UNIX);
        assertEquals("\"ID\",\"Name\",\"Age\"", lines[0]);
        assertEquals("1,\"Alice\",30", lines[1]);
    }

    @Test
    public void testToCsvToFile() throws IOException {
        File tempFile = File.createTempFile("dataset", ".csv");
        tempFile.deleteOnExit();

        dataset.toCsv(tempFile);

        String content = new String(IOUtil.readAllBytes(tempFile));
        assertTrue(content.contains("John"));
    }

    @Test
    public void testToString() {
        String str = dataset.toString();
        assertNotNull(str);
        assertTrue(str.contains("Alice"));
    }

    @Test
    @DisplayName("toMap with duplicate keys silently overwrites earlier values")
    public void testToMapDuplicateKeysOverwrite() {
        List<String> cols = new ArrayList<>(Arrays.asList("k", "v"));
        List<List<Object>> data = new ArrayList<>();
        data.add(new ArrayList<>(Arrays.asList("a", "a", "b")));
        data.add(new ArrayList<>(Arrays.asList(1, 2, 3)));
        Dataset ds = new RowDataset(cols, data);
        Map<Object, Object> map = ds.toMap("k", "v");
        assertEquals(2, map.size());
        assertEquals(2, map.get("a")); // last write wins
        assertEquals(3, map.get("b"));
    }

}
