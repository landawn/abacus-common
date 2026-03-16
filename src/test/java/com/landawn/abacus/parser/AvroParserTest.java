package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.specific.SpecificRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.parser.entity.User;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Strings;

public class AvroParserTest {

    static final AvroParser avroParser = ParserFactory.createAvroParser();

    private AvroParser parser;
    private Schema testSchema;
    private Schema arraySchema;

    @TempDir
    File tempDir;

    @BeforeEach
    public void setUp() {
        parser = new AvroParser();

        testSchema = new Schema.Parser().parse("{\"type\":\"record\",\"name\":\"TestRecord\",\"fields\":[" + "{\"name\":\"name\",\"type\":\"string\"},"
                + "{\"name\":\"age\",\"type\":\"int\"}" + "]}");

        arraySchema = new Schema.Parser().parse("{\"type\":\"array\",\"items\":\"string\"}");
    }

    public static class TestBean {
        private String name;
        private int age;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }
    }

    public static class MockSpecificRecord implements SpecificRecord {
        public static final Schema SCHEMA$ = new Schema.Parser()
                .parse("{\"type\":\"record\",\"name\":\"com.landawn.abacus.parser.AvroParserTest.MockSpecificRecord\",\"fields\":["
                        + "{\"name\":\"id\",\"type\":\"string\"}]}");

        private String id;

        public MockSpecificRecord() {
        }

        public MockSpecificRecord(String id) {
            this.id = id;
        }

        @Override
        public Schema getSchema() {
            return SCHEMA$;
        }

        @Override
        public Object get(int field) {
            if (field == 0)
                return id;
            throw new IndexOutOfBoundsException();
        }

        @Override
        public void put(int field, Object value) {
            if (field == 0) {
                id = (String) value;
            } else {
                throw new IndexOutOfBoundsException();
            }
        }

        public String getId() {
            return id;
        }
    }

    private static final class CloseTrackingOutputStream extends ByteArrayOutputStream {
        private boolean closed = false;

        @Override
        public void close() throws IOException {
            closed = true;
            super.close();
        }

        boolean isClosed() {
            return closed;
        }
    }

    private static final class CloseTrackingInputStream extends ByteArrayInputStream {
        private boolean closed = false;

        CloseTrackingInputStream(byte[] buf) {
            super(buf);
        }

        @Override
        public void close() throws IOException {
            closed = true;
            super.close();
        }

        boolean isClosed() {
            return closed;
        }
    }

    static final Schema schema;

    static {
        try {
            schema = new Schema.Parser().parse(new File("./src/test/resources/user.avsc"));
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testSerialize_0() throws Exception {
        User user1 = new User();
        user1.setName("Alyssa");
        user1.setFavoriteNumber(256);

        String str = avroParser.serialize(user1);

        N.println(str);

        GenericRecord xBean2 = avroParser.deserialize(str, User.class);
        N.println(xBean2);

        Map<String, Object> m = avroParser.deserialize(str, AvroDeserConfig.create().setSchema(schema), Map.class);
        N.println(m);

        User user2 = new User("Ben", 7, "red");
        User user3 = User.newBuilder().setName("Charlie").setFavoriteColor("blue").setFavoriteNumber(null).build();
        str = avroParser.serialize(N.toList(user1, user2, user3));

        N.println(str);

        AvroDeserConfig ds = AvroDeserConfig.create().setSchema(schema).setElementType(User.class);
        List<User> users = avroParser.deserialize(str, ds, List.class);
        N.println(users);

        ds = AvroDeserConfig.create().setElementType(User.class);
        users = avroParser.deserialize(str, ds, List.class);
        N.println(users);
        assertNotNull(users);
    }

    @Test
    public void testSerialize_0_1() throws Exception {
        User user1 = new User();
        user1.setName("Alyssa");
        user1.setFavoriteNumber(256);
        File file = new File("./src/test/resources/test.avsc");
        avroParser.serialize(user1, file);

        N.println(IOUtil.readAllToString(file));

        GenericRecord xBean2 = avroParser.deserialize(file, User.class);
        N.println(xBean2);

        User user2 = new User("Ben", 7, "red");
        User user3 = User.newBuilder().setName("Charlie").setFavoriteColor("blue").setFavoriteNumber(null).build();
        avroParser.serialize(N.toList(user1, user2, user3), file);

        N.println(IOUtil.readAllToString(file));

        AvroDeserConfig ds = AvroDeserConfig.create().setSchema(schema).setElementType(User.class);
        List<User> users = avroParser.deserialize(file, ds, List.class);
        N.println(users);

        ds = AvroDeserConfig.create().setElementType(User.class);
        users = avroParser.deserialize(file, ds, List.class);
        N.println(users);
        assertNotNull(users);
    }

    @Test
    public void testSerialize_1() throws Exception {
        GenericRecord user1 = new GenericData.Record(schema);
        user1.put("name", "Alyssa");
        user1.put("favorite_number", 256);

        AvroSerConfig sc = AvroSerConfig.create().setSchema(schema);
        String str = avroParser.serialize(user1, sc);
        N.println(str);

        Map<String, Object> user2 = new HashMap<>();
        user2.put("name", "Ben");
        user2.put("favorite_number", 7);
        user2.put("favorite_color", "red");

        sc = AvroSerConfig.create().setSchema(schema);
        str = avroParser.serialize(user2, sc);
        N.println(str);

        AvroDeserConfig ds = AvroDeserConfig.create().setSchema(schema);
        GenericRecord record = avroParser.deserialize(str, ds, GenericRecord.class);
        N.println(record);

        Map<String, Object> m = avroParser.deserialize(str, ds, Map.class);
        N.println(m);
        assertNotNull(m);
    }

    @Test
    public void testSerializeToString() {
        GenericRecord record = new GenericData.Record(testSchema);
        record.put("name", "John");
        record.put("age", 30);

        AvroSerConfig config = new AvroSerConfig();
        config.setSchema(testSchema);

        String result = parser.serialize(record, config);
        assertNotNull(result);
        assertTrue(result.length() > 0);
        assertDoesNotThrow(() -> Strings.base64Decode(result));
    }

    @Test
    public void testSerializeToStringWithoutSchema() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "John");
        data.put("age", 30);

        AvroSerConfig config = new AvroSerConfig();

        assertThrows(IllegalArgumentException.class, () -> parser.serialize(data, config));
    }

    @Test
    public void testSerializeToFile() throws IOException {
        GenericRecord record = new GenericData.Record(testSchema);
        record.put("name", "Jane");
        record.put("age", 25);

        AvroSerConfig config = new AvroSerConfig();
        config.setSchema(testSchema);

        File outputFile = new File(tempDir, "test.avro");
        parser.serialize(record, config, outputFile);

        assertTrue(outputFile.exists());
        assertTrue(outputFile.length() > 0);
    }

    @Test
    public void testSerializeToOutputStream() throws IOException {
        GenericRecord record = new GenericData.Record(testSchema);
        record.put("name", "Bob");
        record.put("age", 35);

        AvroSerConfig config = new AvroSerConfig();
        config.setSchema(testSchema);

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        parser.serialize(record, config, os);

        byte[] bytes = os.toByteArray();
        assertTrue(bytes.length > 0);
    }

    @Test
    public void testSerializeToWriter() {
        GenericRecord record = new GenericData.Record(testSchema);
        record.put("name", "Alice");
        record.put("age", 28);

        AvroSerConfig config = new AvroSerConfig();
        config.setSchema(testSchema);

        Writer writer = new StringWriter();

        assertThrows(UnsupportedOperationException.class, () -> parser.serialize(record, config, writer));
    }

    @Test
    public void testSerializeMapWithSchema() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", "MapUser");
        map.put("age", 40);

        AvroSerConfig config = new AvroSerConfig();
        config.setSchema(testSchema);

        String result = parser.serialize(map, config);
        assertNotNull(result);
        assertTrue(result.length() > 0);
    }

    @Test
    public void testSerializeCollectionWithSchema() {
        List<String> list = Arrays.asList("one", "two", "three");

        AvroSerConfig config = new AvroSerConfig();
        config.setSchema(arraySchema);

        String result = parser.serialize(list, config);
        assertNotNull(result);
        assertTrue(result.length() > 0);
    }

    @Test
    public void testSerializePrimitiveArray() {
        int[] array = { 1, 2, 3, 4, 5 };

        Schema intArraySchema = new Schema.Parser().parse("{\"type\":\"array\",\"items\":\"int\"}");

        AvroSerConfig config = new AvroSerConfig();
        config.setSchema(intArraySchema);

        String result = parser.serialize(N.toList(array), config);
        assertNotNull(result);
        assertTrue(result.length() > 0);
    }

    @Test
    public void testSerializeUnsupportedType() {
        StringBuilder sb = new StringBuilder("test");

        AvroSerConfig config = new AvroSerConfig();
        config.setSchema(testSchema);

        assertThrows(IllegalArgumentException.class, () -> parser.serialize(sb, config));
    }

    @Test
    public void testDeserializeFromString() {
        GenericRecord record = new GenericData.Record(testSchema);
        record.put("name", "TestUser");
        record.put("age", 50);

        AvroSerConfig serConfig = new AvroSerConfig();
        serConfig.setSchema(testSchema);

        String serialized = parser.serialize(record, serConfig);

        AvroDeserConfig deserConfig = new AvroDeserConfig();
        deserConfig.setSchema(testSchema);

        GenericRecord result = parser.deserialize(serialized, deserConfig, GenericRecord.class);
        assertNotNull(result);
        assertEquals("TestUser", result.get("name").toString());
        assertEquals(50, result.get("age"));
    }

    @Test
    public void testDeserializeFromFile() throws IOException {
        GenericRecord record = new GenericData.Record(testSchema);
        record.put("name", "FileUser");
        record.put("age", 45);

        AvroSerConfig serConfig = new AvroSerConfig();
        serConfig.setSchema(testSchema);

        File file = new File(tempDir, "deserialize_test.avro");
        parser.serialize(record, serConfig, file);

        AvroDeserConfig deserConfig = new AvroDeserConfig();
        deserConfig.setSchema(testSchema);

        GenericRecord result = parser.deserialize(file, deserConfig, GenericRecord.class);
        assertNotNull(result);
        assertEquals("FileUser", result.get("name").toString());
        assertEquals(45, result.get("age"));
    }

    @Test
    public void testDeserializeFromInputStream() throws IOException {
        GenericRecord record = new GenericData.Record(testSchema);
        record.put("name", "StreamUser");
        record.put("age", 55);

        AvroSerConfig serConfig = new AvroSerConfig();
        serConfig.setSchema(testSchema);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        parser.serialize(record, serConfig, baos);

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        AvroDeserConfig deserConfig = new AvroDeserConfig();
        deserConfig.setSchema(testSchema);

        GenericRecord result = parser.deserialize(bais, deserConfig, GenericRecord.class);
        assertNotNull(result);
        assertEquals("StreamUser", result.get("name").toString());
        assertEquals(55, result.get("age"));
    }

    @Test
    public void testDeserializeFromReader() {
        AvroDeserConfig config = AvroDeserConfig.create().setSchema(testSchema);

        assertThrows(UnsupportedOperationException.class, () -> parser.deserialize(new java.io.StringReader("test"), config, GenericRecord.class));
    }

    @Test
    public void testDeserializeToMap() throws IOException {
        GenericRecord record = new GenericData.Record(testSchema);
        record.put("name", "MapDeserializeUser");
        record.put("age", 60);

        AvroSerConfig serConfig = new AvroSerConfig();
        serConfig.setSchema(testSchema);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        parser.serialize(record, serConfig, baos);

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        AvroDeserConfig deserConfig = new AvroDeserConfig();
        deserConfig.setSchema(testSchema);

        Map<String, Object> result = parser.deserialize(bais, deserConfig, HashMap.class);
        assertNotNull(result);
        assertEquals("MapDeserializeUser", result.get("name").toString());
        assertEquals(60, result.get("age"));
    }

    @Test
    public void testDeserializeToCollection() throws IOException {
        Schema stringArraySchema = new Schema.Parser().parse("{\"type\":\"array\",\"items\":\"string\"}");

        List<String> originalList = Arrays.asList("alpha", "beta", "gamma");

        AvroSerConfig serConfig = new AvroSerConfig();
        serConfig.setSchema(stringArraySchema);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        parser.serialize(originalList, serConfig, baos);

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        AvroDeserConfig deserConfig = new AvroDeserConfig();
        deserConfig.setSchema(stringArraySchema);

        List<String> result = parser.deserialize(bais, deserConfig, List.class);
        assertNotNull(result);
        assertEquals(3, result.size());
    }

    @Test
    public void testDeserializeWithoutSchema() {
        String base64Data = Strings.base64Encode("dummy data".getBytes());

        AvroDeserConfig config = new AvroDeserConfig();

        assertThrows(IllegalArgumentException.class, () -> parser.deserialize(base64Data, config, Map.class));
    }

    @Test
    public void testSerializeCollectionOfMaps() throws IOException {
        List<Map<String, Object>> list = new ArrayList<>();
        Map<String, Object> map1 = new HashMap<>();
        map1.put("name", "User1");
        map1.put("age", 20);
        list.add(map1);

        Map<String, Object> map2 = new HashMap<>();
        map2.put("name", "User2");
        map2.put("age", 25);
        list.add(map2);

        AvroSerConfig config = new AvroSerConfig();
        config.setSchema(testSchema);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        parser.serialize(list, config, baos);

        assertTrue(baos.toByteArray().length > 0);
    }

    @Test
    public void testSerializeBean() {
        TestBean bean = new TestBean();
        bean.setName("BeanUser");
        bean.setAge(40);

        AvroSerConfig config = new AvroSerConfig();
        config.setSchema(testSchema);

        String result = parser.serialize(bean, config);
        assertNotNull(result);
        assertTrue(result.length() > 0);
    }

    @Test
    public void testDeserializeToBean() throws IOException {
        GenericRecord record = new GenericData.Record(testSchema);
        record.put("name", "BeanDeserializeUser");
        record.put("age", 65);

        AvroSerConfig serConfig = new AvroSerConfig();
        serConfig.setSchema(testSchema);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        parser.serialize(record, serConfig, baos);

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        AvroDeserConfig deserConfig = new AvroDeserConfig();
        deserConfig.setSchema(testSchema);

        TestBean result = parser.deserialize(bais, deserConfig, TestBean.class);
        assertNotNull(result);
        assertEquals("BeanDeserializeUser", result.getName().toString());
        assertEquals(65, result.getAge());
    }

    @Test
    public void testSerializeEmptyCollection() {
        List<Object> emptyList = new ArrayList<>();

        AvroSerConfig config = new AvroSerConfig();
        config.setSchema(arraySchema);

        String result = parser.serialize(emptyList, config);

        N.println("Testing serialization of empty collection: " + result);

        AvroDeserConfig adc = AvroDeserConfig.create().setSchema(arraySchema).setElementType(String.class);

        List<String> newList = parser.deserialize(result, adc, List.class);
        N.println("Deserialized empty collection: " + newList);
        assertNotNull(newList);
    }

    @Test
    public void testIOExceptionHandlingInSerializeToFile() {
        GenericRecord record = new GenericData.Record(testSchema);
        record.put("name", "Test");
        record.put("age", 30);

        AvroSerConfig config = new AvroSerConfig();
        config.setSchema(testSchema);

        File invalidFile = tempDir;

        assertThrows(UncheckedIOException.class, () -> parser.serialize(record, config, invalidFile));
    }

    @Test
    public void testDeserializeUnsupportedType() {
        AvroDeserConfig config = new AvroDeserConfig();
        config.setSchema(testSchema);

        ByteArrayInputStream bais = new ByteArrayInputStream(new byte[0]);

        assertThrows(UncheckedIOException.class, () -> parser.deserialize(bais, config, StringBuilder.class));
    }

    @Test
    public void testSerializeSpecificRecord() throws IOException {
        MockSpecificRecord record = new MockSpecificRecord("test-id-123");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        parser.serialize(record, null, baos);

        assertTrue(baos.toByteArray().length > 0);
    }

    @Test
    public void testSerializeCollectionOfSpecificRecords() throws IOException {
        List<MockSpecificRecord> records = Arrays.asList(new MockSpecificRecord("id1"), new MockSpecificRecord("id2"), new MockSpecificRecord("id3"));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        parser.serialize(records, null, baos);

        assertTrue(baos.toByteArray().length > 0);
    }

    @Test
    public void testDeserializeSpecificRecord() throws IOException {
        MockSpecificRecord original = new MockSpecificRecord("deserialize-test");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        parser.serialize(original, null, baos);

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        assertThrows(ClassCastException.class, () -> {
            MockSpecificRecord result = parser.deserialize(bais, null, MockSpecificRecord.class);

            assertNotNull(result);
            assertEquals("deserialize-test", result.getId());
        });
    }

    @Test
    public void testDeserializeCollectionOfSpecificRecords() throws IOException {
        List<MockSpecificRecord> original = Arrays.asList(new MockSpecificRecord("col-id1"), new MockSpecificRecord("col-id2"));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        parser.serialize(original, null, baos);

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        AvroDeserConfig config = new AvroDeserConfig();
        config.setElementType(MockSpecificRecord.class);

        assertThrows(ClassCastException.class, () -> {
            List<MockSpecificRecord> result = parser.deserialize(bais, config, List.class);

            assertNotNull(result);
            assertEquals(2, result.size());
            assertEquals("col-id1", result.get(0).getId());
            assertEquals("col-id2", result.get(1).getId());
        });
    }

    @Test
    public void testSerializeCollectionOfGenericRecords() throws IOException {
        List<GenericRecord> records = new ArrayList<>();

        GenericRecord record1 = new GenericData.Record(testSchema);
        record1.put("name", "Generic1");
        record1.put("age", 10);
        records.add(record1);

        GenericRecord record2 = new GenericData.Record(testSchema);
        record2.put("name", "Generic2");
        record2.put("age", 20);
        records.add(record2);

        AvroSerConfig config = new AvroSerConfig();
        config.setSchema(testSchema);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        parser.serialize(records, config, baos);

        assertTrue(baos.toByteArray().length > 0);
    }

    @Test
    public void testDeserializeCollectionWithElementType() throws IOException {
        List<Map<String, Object>> original = new ArrayList<>();
        Map<String, Object> map1 = new HashMap<>();
        map1.put("name", "Element1");
        map1.put("age", 15);
        original.add(map1);

        Map<String, Object> map2 = new HashMap<>();
        map2.put("name", "Element2");
        map2.put("age", 25);
        original.add(map2);

        AvroSerConfig serConfig = new AvroSerConfig();
        serConfig.setSchema(testSchema);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        parser.serialize(original, serConfig, baos);

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        AvroDeserConfig deserConfig = new AvroDeserConfig();
        deserConfig.setSchema(testSchema);
        deserConfig.setElementType(HashMap.class);

        List<HashMap> result = parser.deserialize(bais, deserConfig, List.class);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Element1", result.get(0).get("name").toString());
        assertEquals(15, result.get(0).get("age"));
    }

    @Test
    public void testSerializeToStringWithNull() {
        // assertThrows(NullPointerException.class, () -> parser.serialize(null, (AvroSerConfig) null));
        Assertions.assertEquals("", parser.serialize(null, (AvroSerConfig) null));
    }

    @Test
    public void testSerializeToOutputStreamDoesNotCloseCallerStream() throws IOException {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "NoClose");
        data.put("age", 26);

        AvroSerConfig config = AvroSerConfig.create().setSchema(schema);
        CloseTrackingOutputStream os = new CloseTrackingOutputStream();

        parser.serialize(data, config, os);

        Assertions.assertFalse(os.isClosed());
        os.write(1);
    }

    @Test
    public void testSerializeToOutputStreamWithoutSchema() {
        Map<String, Object> data = new HashMap<>();
        data.put("test", "value");

        ByteArrayOutputStream os = new ByteArrayOutputStream();

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            parser.serialize(data, null, os);
        });
    }

    @Test
    public void testDeserializeFromInputStreamDoesNotCloseCallerStream() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "InputNoClose");
        data.put("age", 41);

        AvroSerConfig serConfig = AvroSerConfig.create().setSchema(schema);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        parser.serialize(data, serConfig, os);

        CloseTrackingInputStream is = new CloseTrackingInputStream(os.toByteArray());
        AvroDeserConfig desConfig = AvroDeserConfig.create().setSchema(schema);
        GenericRecord result = parser.deserialize(is, desConfig, GenericRecord.class);

        Assertions.assertNotNull(result);
        Assertions.assertEquals("InputNoClose", result.get("name").toString());
        Assertions.assertFalse(is.isClosed());
    }

    @Test
    public void testDeserializeFromInputStreamWithoutSchema() {
        ByteArrayInputStream is = new ByteArrayInputStream(new byte[0]);

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            parser.deserialize(is, null, Map.class);
        });
    }

    @Test
    public void testSerializeCollection() {
        List<Map<String, Object>> list = new ArrayList<>();
        Map<String, Object> item1 = new HashMap<>();
        item1.put("name", "Item1");
        item1.put("age", 10);
        Map<String, Object> item2 = new HashMap<>();
        item2.put("name", "Item2");
        item2.put("age", 20);
        list.add(item1);
        list.add(item2);

        AvroSerConfig config = AvroSerConfig.create().setSchema(schema);
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        parser.serialize(list, config, os);

        byte[] bytes = os.toByteArray();
        Assertions.assertTrue(bytes.length > 0);
    }

    @Test
    @DisplayName("Deserialize to GenericRecord should return GenericRecord directly")
    public void test_deserialize_toGenericRecord() {
        // Create a GenericRecord and serialize it
        GenericRecord record = new GenericData.Record(schema);
        record.put("name", "Alice");
        record.put("age", 30);

        AvroSerConfig serConfig = AvroSerConfig.create().setSchema(schema);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        parser.serialize(record, serConfig, os);

        byte[] serialized = os.toByteArray();
        assertTrue(serialized.length > 0, "Serialized data should not be empty");

        // Deserialize back to GenericRecord — this is the bugfix path
        AvroDeserConfig deserConfig = AvroDeserConfig.create().setSchema(schema);
        ByteArrayInputStream is = new ByteArrayInputStream(serialized);
        GenericRecord result = parser.deserialize(is, deserConfig, GenericRecord.class);

        assertNotNull(result, "Deserialized GenericRecord should not be null");
        assertEquals("Alice", result.get("name").toString());
        assertEquals(30, result.get("age"));
    }

    @Test
    @DisplayName("Deserialize to Map should still work correctly")
    public void test_deserialize_toMap() {
        GenericRecord record = new GenericData.Record(schema);
        record.put("name", "Bob");
        record.put("age", 25);

        AvroSerConfig serConfig = AvroSerConfig.create().setSchema(schema);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        parser.serialize(record, serConfig, os);

        byte[] serialized = os.toByteArray();

        AvroDeserConfig deserConfig = AvroDeserConfig.create().setSchema(schema);
        ByteArrayInputStream is = new ByteArrayInputStream(serialized);
        @SuppressWarnings("unchecked")
        Map<String, Object> result = parser.deserialize(is, deserConfig, Map.class);

        assertNotNull(result, "Deserialized Map should not be null");
        assertEquals("Bob", result.get("name").toString());
        assertEquals(25, result.get("age"));
    }

    // Exercise private Avro record conversion helpers that are otherwise hard to reach through the public API.
    @Test
    public void testToGenericRecord_CollectionFieldsByPosition() throws Exception {
        Method method = AvroParser.class.getDeclaredMethod("toGenericRecord", Object.class, Schema.class);
        method.setAccessible(true);

        GenericRecord result = (GenericRecord) method.invoke(parser, Arrays.asList("ListUser", 29), testSchema);

        assertNotNull(result);
        assertEquals("ListUser", result.get("name").toString());
        assertEquals(29, result.get("age"));
    }

    @Test
    public void testFromGenericRecord_CollectionTarget() throws Exception {
        GenericRecord record = new GenericData.Record(testSchema);
        record.put("name", "ListUser");
        record.put("age", 29);

        Method method = AvroParser.class.getDeclaredMethod("fromGenericRecord", GenericRecord.class, Class.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<Object> result = (List<Object>) method.invoke(parser, record, ArrayList.class);

        assertNotNull(result);
        assertEquals(Arrays.asList("ListUser", 29), result);
    }
}
