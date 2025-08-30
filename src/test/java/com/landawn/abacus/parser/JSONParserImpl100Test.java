package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.annotation.JsonXmlField;
import com.landawn.abacus.parser.JSONDeserializationConfig.JDC;
import com.landawn.abacus.parser.JSONSerializationConfig.JSC;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.Dataset;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.MapEntity;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Seid;
import com.landawn.abacus.util.Sheet;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class JSONParserImpl100Test extends TestBase {

    private JSONParserImpl parser;

    @TempDir
    Path tempDir;

    @BeforeEach
    public void setUp() {
        parser = new JSONParserImpl();
    }

    @Builder
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Person {

        public Person(String name, int age) {
            this.name = name;
            this.age = age;
        }

        private String name;
        private int age;
        private List<String> hobbies;
    }

    @Builder
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class XBean {
        private Person rawJsonBean;
        private List<String> rawJsonList;
    }

    @Builder
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class XBean2 {
        @JsonXmlField(isJsonRawValue = true)
        private String rawJsonBean;

        @JsonXmlField(isJsonRawValue = true)
        private String rawJsonList;
    }

    public static class PersonWithTransient {
        private String name;
        private transient String secret;
        @JsonXmlField(expose = JsonXmlField.Expose.SERIALIZE_ONLY)
        private String writeOnly;
        @JsonXmlField(expose = JsonXmlField.Expose.DESERIALIZE_ONLY)
        private String readOnly;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public String getWriteOnly() {
            return writeOnly;
        }

        public void setWriteOnly(String writeOnly) {
            this.writeOnly = writeOnly;
        }

        public String getReadOnly() {
            return readOnly;
        }

        public void setReadOnly(String readOnly) {
            this.readOnly = readOnly;
        }
    }

    @Test
    public void testReadStringToObject() {
        String json = "{\"name\": \"John\",\"age\":30}";
        Person person = parser.readString(json, null, Person.class);

        assertEquals("John", person.getName());
        assertEquals(30, person.getAge());
    }

    @Test
    public void testReadStringToArray() {
        String json = "[1,2,3,4,5]";
        Integer[] array = new Integer[5];
        parser.readString(json, null, array);

        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, array);
    }

    @Test
    public void testReadStringToCollection() {
        String json = "[\"a\",\"b\",\"c\"]";
        List<String> list = new ArrayList<>();
        parser.readString(json, null, list);

        assertEquals(Arrays.asList("a", "b", "c"), list);
    }

    @Test
    public void testReadStringToMap() {
        String json = "{\"key1\": \"value1\",\"key2\": \"value2\"}";
        Map<String, String> map = new HashMap<>();
        parser.readString(json, null, map);

        assertEquals("value1", map.get("key1"));
        assertEquals("value2", map.get("key2"));
    }

    @Test
    public void testSerializeObject() {
        Person person = new Person("Jane", 25);
        String json = parser.serialize(person);

        assertTrue(json.contains("\"name\": \"Jane\""));
        assertTrue(json.contains("\"age\": 25"));
    }

    @Test
    public void testSerializeNull() {
        String result = parser.serialize(null);
        assertNull(result);
    }

    @Test
    public void testSerializeToFile() throws IOException {
        Person person = new Person("Bob", 35);
        File file = tempDir.resolve("person.json").toFile();

        parser.serialize(person, null, file);

        assertTrue(file.exists());
        String content = IOUtil.readAllToString(file);
        assertTrue(content.contains("\"name\": \"Bob\""));
    }

    @Test
    public void testSerializeToOutputStream() throws IOException {
        Person person = new Person("Alice", 28);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        parser.serialize(person, null, baos);

        String json = baos.toString();
        assertTrue(json.contains("\"name\": \"Alice\""));
    }

    @Test
    public void testSerializeToWriter() throws IOException {
        Person person = new Person("Charlie", 40);
        StringWriter writer = new StringWriter();

        parser.serialize(person, null, writer);

        String json = writer.toString();
        assertTrue(json.contains("\"name\": \"Charlie\""));
    }

    @Test
    public void testDeserializeFromString() {
        String json = "{\"name\": \"David\",\"age\":45}";
        Person person = parser.deserialize(json, null, Person.class);

        assertEquals("David", person.getName());
        assertEquals(45, person.getAge());
    }

    @Test
    public void testDeserializeFromSubstring() {
        String json = "prefix{\"name\": \"Eve\",\"age\":22}suffix";
        Person person = parser.deserialize(json, 6, json.length() - 6, null, Person.class);

        assertEquals("Eve", person.getName());
        assertEquals(22, person.getAge());
    }

    @Test
    public void testDeserializeFromFile() throws IOException {
        String json = "{\"name\": \"Frank\",\"age\":50}";
        File file = tempDir.resolve("test.json").toFile();
        IOUtil.write(json, file);

        Person person = parser.deserialize(file, null, Person.class);

        assertEquals("Frank", person.getName());
        assertEquals(50, person.getAge());
    }

    @Test
    public void testDeserializeFromInputStream() throws IOException {
        String json = "{\"name\": \"Grace\",\"age\":33}";
        ByteArrayInputStream bais = new ByteArrayInputStream(json.getBytes());

        Person person = parser.deserialize(bais, null, Person.class);

        assertEquals("Grace", person.getName());
        assertEquals(33, person.getAge());
    }

    @Test
    public void testDeserializeFromReader() throws IOException {
        String json = "{\"name\": \"Henry\",\"age\":27}";
        StringReader reader = new StringReader(json);

        Person person = parser.deserialize(reader, null, Person.class);

        assertEquals("Henry", person.getName());
        assertEquals(27, person.getAge());
    }

    @Test
    public void testStreamFromString() {
        String json = "[{\"name\": \"A\",\"age\":1},{\"name\": \"B\",\"age\":2},{\"name\": \"C\",\"age\":3}]";

        List<Person> people = parser.stream(json, null, Type.of(Person.class)).toList();

        assertEquals(3, people.size());
        assertEquals("A", people.get(0).getName());
        assertEquals("B", people.get(1).getName());
        assertEquals("C", people.get(2).getName());
    }

    @Test
    public void testStreamFromFile() throws IOException {
        String json = "[{\"name\": \"X\",\"age\":10},{\"name\": \"Y\",\"age\":20}]";
        File file = tempDir.resolve("stream.json").toFile();
        IOUtil.write(json, file);

        List<Person> people = parser.stream(file, null, Type.of(Person.class)).toList();

        assertEquals(2, people.size());
        assertEquals("X", people.get(0).getName());
        assertEquals("Y", people.get(1).getName());
    }

    @Test
    public void testStreamFromInputStream() throws IOException {
        String json = "[{\"name\": \"M\",\"age\":15},{\"name\": \"N\",\"age\":25}]";
        ByteArrayInputStream bais = new ByteArrayInputStream(json.getBytes());

        List<Person> people = parser.stream(bais, null, true, Type.of(Person.class)).toList();

        assertEquals(2, people.size());
        assertEquals("M", people.get(0).getName());
        assertEquals("N", people.get(1).getName());
    }

    @Test
    public void testStreamFromReader() throws IOException {
        String json = "[{\"name\": \"P\",\"age\":30},{\"name\": \"Q\",\"age\":40}]";
        StringReader reader = new StringReader(json);

        List<Person> people = parser.stream(reader, null, true, Type.of(Person.class)).toList();

        assertEquals(2, people.size());
        assertEquals("P", people.get(0).getName());
        assertEquals("Q", people.get(1).getName());
    }

    @Test
    public void testSerializationConfig() {
        Person person = new Person("Test", 100);

        // Test pretty format
        JSONSerializationConfig config = JSC.create().prettyFormat(true).setIndentation("  ");

        String json = parser.serialize(person, config);
        assertTrue(json.contains("\n"));
        assertTrue(json.contains("  "));
    }

    @Test
    public void testSerializeWithNullHandling() {
        Map<String, Object> map = new HashMap<>();
        map.put("string", null);
        map.put("number", null);
        map.put("boolean", null);

        JSONSerializationConfig config = JSC.create().writeNullStringAsEmpty(true).writeNullNumberAsZero(true).writeNullBooleanAsFalse(true);

        String json = parser.serialize(map, config);
        assertTrue(json.contains("\"string\": null"));
        assertTrue(json.contains("\"number\": null"));
        assertTrue(json.contains("\"boolean\": null"));
    }

    @Test
    public void testDeserializationConfig() {
        String json = "{\"name\": \"Test\",\"unknownField\": \"value\"}";

        JSONDeserializationConfig config = JDC.create().ignoreUnmatchedProperty(true);

        Person person = parser.deserialize(json, config, Person.class);
        assertEquals("Test", person.getName());
    }

    @Test
    public void testSerializeCollection() {
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);
        String json = parser.serialize(list);

        assertEquals("[1, 2, 3, 4, 5]", json);
    }

    @Test
    public void testSerializeMap() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("one", 1);
        map.put("two", 2);

        String json = parser.serialize(map);
        assertEquals("{\"one\": 1, \"two\": 2}", json);
    }

    @Test
    public void testSerializeArray() {
        int[] array = { 1, 2, 3 };
        String json = parser.serialize(array);

        assertEquals("[1, 2, 3]", json);
    }

    @Test
    public void testSerializeDataset() {
        List<String> columnNames = Arrays.asList("col1", "col2");
        List<List<Object>> columnList = new ArrayList<>();
        columnList.add(Arrays.asList("a", "b", "c"));
        columnList.add(Arrays.asList(1, 2, 3));

        Dataset ds = N.newDataset(columnNames, columnList);
        String json = parser.serialize(ds);

        assertTrue(json.contains("columnNames"));
        assertTrue(json.contains("columns"));

        Dataset ds2 = parser.deserialize(json, Dataset.class);
        assertEquals(ds, ds2);
    }

    @Test
    public void testSerializeDataset_2() {
        List<String> columnNames = Arrays.asList("col1", "col2");
        List<List<Object>> columnList = new ArrayList<>();
        columnList.add(Arrays.asList("a", "b", "c"));
        columnList.add(Arrays.asList(1, 2, 3));

        Dataset ds = N.newDataset(columnNames, columnList);
        ds.freeze();
        String json = parser.serialize(ds, JSC.create().writeColumnType(true).prettyFormat(true).quotePropName(true));

        assertTrue(json.contains("columnNames"));
        assertTrue(json.contains("columns"));

        Dataset ds2 = parser.deserialize(json, Dataset.class);
        assertEquals(ds, ds2);
    }

    @Test
    public void testSerializeSheet() {
        List<String> rowKeys = Arrays.asList("R1", "R2", "R3");
        List<String> columnKeys = Arrays.asList("C1", "C2", "C3");
        Object[][] data = { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } };
        Sheet<String, String, Integer> sheet = new Sheet<>(rowKeys, columnKeys, data);

        sheet.println();

        sheet.freeze();
        String json = parser.serialize(sheet, JSC.create().writeColumnType(true).writeRowColumnKeyType(true).prettyFormat(true).quotePropName(true));

        Sheet<String, String, Integer> sheet2 = parser.deserialize(json, Sheet.class);
        assertEquals(sheet, sheet2);
    }

    @Test
    public void testSerializeMapEntity() {
        MapEntity entity = new MapEntity("TestEntity");
        entity.set("prop1", "value1");
        entity.set("prop2", 123);

        String json = parser.serialize(entity);
        assertTrue(json.contains("TestEntity"));
        assertTrue(json.contains("prop1"));
        assertTrue(json.contains("value1"));
    }

    @Test
    public void testSerializeEntityId() {
        Seid entityId = Seid.of("TestId");
        entityId.set("id", 123);
        entityId.set("type", "test");

        String json = parser.serialize(entityId);
        assertTrue(json.contains("TestId"));
        assertTrue(json.contains("\"id\": 123"));
    }

    @Test
    public void testComplexNestedObject() {
        Person person = new Person("John", 30);
        person.setHobbies(Arrays.asList("reading", "gaming", "coding"));

        String json = parser.serialize(person);
        Person deserialized = parser.deserialize(json, null, Person.class);

        assertEquals(person, deserialized);
    }

    @Test
    public void testTransientFields() {
        PersonWithTransient person = new PersonWithTransient();
        person.setName("Public");
        person.setSecret("Secret");
        person.setWriteOnly("WriteOnly");
        person.setReadOnly("ReadOnly");

        JSONSerializationConfig config = JSC.create().skipTransientField(true);
        String json = parser.serialize(person, config);

        assertFalse(json.contains("secret"));
        assertTrue(json.contains("writeOnly"));
        assertFalse(json.contains("readOnly"));
    }

    @Test
    public void testEmptyJson() {
        assertEquals(new HashMap<>(), parser.deserialize("{}", null, Map.class));
        assertEquals(new ArrayList<>(), parser.deserialize("[]", null, List.class));
    }

    @Test
    public void testSpecialValues() {
        String json = "{\"nullValue\":null,\"trueValue\":true,\"falseValue\":false}";
        Map<String, Object> map = parser.deserialize(json, null, Map.class);

        assertNull(map.get("nullValue"));
        assertEquals(true, map.get("trueValue"));
        assertEquals(false, map.get("falseValue"));
    }

    @Test
    public void testNumbers() {
        String json = "{\"int\":123,\"long\":123456789012345,\"float\":123.45,\"double\":123.456789}";
        Map<String, Object> map = parser.deserialize(json, null, Map.class);

        assertTrue(map.get("int") instanceof Integer);
        assertTrue(map.get("long") instanceof Long);
        assertTrue(map.get("double") instanceof Double);
    }

    @Test
    public void testEscapedCharacters() {
        String json = "{\"text\":\"Line1\\nLine2\\tTabbed\\\"Quoted\\\"\"}";
        Map<String, String> map = parser.deserialize(json, null, Map.class);

        assertEquals("Line1\nLine2\tTabbed\"Quoted\"", map.get("text"));
    }

    @Test
    public void testRawJson() {
        XBean xBean = new XBean();
        xBean.setRawJsonBean(new Person("RawBean", 99));
        xBean.setRawJsonList(Arrays.asList("item1", "item2", "item3"));
        String json = parser.serialize(xBean);
        N.println(json);

        XBean2 xBean2 = parser.deserialize(json, null, XBean2.class);
        assertEquals("{\"name\": \"RawBean\", \"age\": 99}", xBean2.getRawJsonBean());
        assertEquals("[\"item1\", \"item2\", \"item3\"]", xBean2.getRawJsonList());
        // assertEquals(xBean, xBean2);
    }

}
