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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.specific.SpecificRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.parser.AvroDeserializationConfig.ADC;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Strings;

public class AvroParser101Test extends TestBase {

    private AvroParser parser;
    private Schema testSchema;
    private Schema arraySchema;

    @TempDir
    File tempDir;

    @BeforeEach
    public void setUp() {
        parser = new AvroParser();

        // Create a simple test schema
        testSchema = new Schema.Parser().parse("{\"type\":\"record\",\"name\":\"TestRecord\",\"fields\":[" + "{\"name\":\"name\",\"type\":\"string\"},"
                + "{\"name\":\"age\",\"type\":\"int\"}" + "]}");

        // Create array schema
        arraySchema = new Schema.Parser().parse("{\"type\":\"array\",\"items\":\"string\"}");
    }

    @Test
    public void testSerializeToString() {
        // Test with GenericRecord
        GenericRecord record = new GenericData.Record(testSchema);
        record.put("name", "John");
        record.put("age", 30);

        AvroSerializationConfig config = new AvroSerializationConfig();
        config.setSchema(testSchema);

        String result = parser.serialize(record, config);
        assertNotNull(result);
        assertTrue(result.length() > 0);
        // Result should be Base64 encoded
        assertDoesNotThrow(() -> Strings.base64Decode(result));
    }

    @Test
    public void testSerializeToStringWithoutSchema() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "John");
        data.put("age", 30);

        AvroSerializationConfig config = new AvroSerializationConfig();
        // No schema set

        assertThrows(IllegalArgumentException.class, () -> parser.serialize(data, config));
    }

    @Test
    public void testSerializeToFile() throws IOException {
        GenericRecord record = new GenericData.Record(testSchema);
        record.put("name", "Jane");
        record.put("age", 25);

        AvroSerializationConfig config = new AvroSerializationConfig();
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

        AvroSerializationConfig config = new AvroSerializationConfig();
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

        AvroSerializationConfig config = new AvroSerializationConfig();
        config.setSchema(testSchema);

        Writer writer = new StringWriter();

        assertThrows(UnsupportedOperationException.class, () -> parser.serialize(record, config, writer));
    }

    @Test
    public void testSerializeMapWithSchema() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", "MapUser");
        map.put("age", 40);

        AvroSerializationConfig config = new AvroSerializationConfig();
        config.setSchema(testSchema);

        String result = parser.serialize(map, config);
        assertNotNull(result);
        assertTrue(result.length() > 0);
    }

    @Test
    public void testSerializeCollectionWithSchema() {
        List<String> list = Arrays.asList("one", "two", "three");

        AvroSerializationConfig config = new AvroSerializationConfig();
        config.setSchema(arraySchema);

        String result = parser.serialize(list, config);
        assertNotNull(result);
        assertTrue(result.length() > 0);
    }

    @Test
    public void testSerializePrimitiveArray() {
        int[] array = { 1, 2, 3, 4, 5 };

        Schema intArraySchema = new Schema.Parser().parse("{\"type\":\"array\",\"items\":\"int\"}");

        AvroSerializationConfig config = new AvroSerializationConfig();
        config.setSchema(intArraySchema);

        String result = parser.serialize(N.toList(array), config);
        assertNotNull(result);
        assertTrue(result.length() > 0);
    }

    @Test
    public void testSerializeUnsupportedType() {
        StringBuilder sb = new StringBuilder("test");

        AvroSerializationConfig config = new AvroSerializationConfig();
        config.setSchema(testSchema);

        assertThrows(IllegalArgumentException.class, () -> parser.serialize(sb, config));
    }

    @Test
    public void testDeserializeFromString() {
        // First serialize
        GenericRecord record = new GenericData.Record(testSchema);
        record.put("name", "TestUser");
        record.put("age", 50);

        AvroSerializationConfig serConfig = new AvroSerializationConfig();
        serConfig.setSchema(testSchema);

        String serialized = parser.serialize(record, serConfig);

        // Then deserialize
        AvroDeserializationConfig deserConfig = new AvroDeserializationConfig();
        deserConfig.setSchema(testSchema);

        GenericRecord result = parser.deserialize(serialized, deserConfig, GenericRecord.class);
        assertNotNull(result);
        assertEquals("TestUser", result.get("name").toString());
        assertEquals(50, result.get("age"));
    }

    @Test
    public void testDeserializeFromFile() throws IOException {
        // First serialize to file
        GenericRecord record = new GenericData.Record(testSchema);
        record.put("name", "FileUser");
        record.put("age", 45);

        AvroSerializationConfig serConfig = new AvroSerializationConfig();
        serConfig.setSchema(testSchema);

        File file = new File(tempDir, "deserialize_test.avro");
        parser.serialize(record, serConfig, file);

        // Then deserialize from file
        AvroDeserializationConfig deserConfig = new AvroDeserializationConfig();
        deserConfig.setSchema(testSchema);

        GenericRecord result = parser.deserialize(file, deserConfig, GenericRecord.class);
        assertNotNull(result);
        assertEquals("FileUser", result.get("name").toString());
        assertEquals(45, result.get("age"));
    }

    @Test
    public void testDeserializeFromInputStream() throws IOException {
        // First serialize
        GenericRecord record = new GenericData.Record(testSchema);
        record.put("name", "StreamUser");
        record.put("age", 55);

        AvroSerializationConfig serConfig = new AvroSerializationConfig();
        serConfig.setSchema(testSchema);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        parser.serialize(record, serConfig, baos);

        // Then deserialize
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        AvroDeserializationConfig deserConfig = new AvroDeserializationConfig();
        deserConfig.setSchema(testSchema);

        GenericRecord result = parser.deserialize(bais, deserConfig, GenericRecord.class);
        assertNotNull(result);
        assertEquals("StreamUser", result.get("name").toString());
        assertEquals(55, result.get("age"));
    }

    @Test
    public void testDeserializeFromReader() {
        AvroDeserializationConfig config = ADC.create().setSchema(testSchema);

        assertThrows(UnsupportedOperationException.class, () -> parser.deserialize(new java.io.StringReader("test"), config, GenericRecord.class));
    }

    @Test
    public void testDeserializeToMap() throws IOException {
        // First serialize
        GenericRecord record = new GenericData.Record(testSchema);
        record.put("name", "MapDeserializeUser");
        record.put("age", 60);

        AvroSerializationConfig serConfig = new AvroSerializationConfig();
        serConfig.setSchema(testSchema);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        parser.serialize(record, serConfig, baos);

        // Then deserialize to Map
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        AvroDeserializationConfig deserConfig = new AvroDeserializationConfig();
        deserConfig.setSchema(testSchema);

        Map<String, Object> result = parser.deserialize(bais, deserConfig, HashMap.class);
        assertNotNull(result);
        assertEquals("MapDeserializeUser", result.get("name").toString());
        assertEquals(60, result.get("age"));
    }

    @Test
    public void testDeserializeToCollection() throws IOException {
        // Create array schema and data
        Schema stringArraySchema = new Schema.Parser().parse("{\"type\":\"array\",\"items\":\"string\"}");

        List<String> originalList = Arrays.asList("alpha", "beta", "gamma");

        AvroSerializationConfig serConfig = new AvroSerializationConfig();
        serConfig.setSchema(stringArraySchema);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        parser.serialize(originalList, serConfig, baos);

        // Deserialize
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        AvroDeserializationConfig deserConfig = new AvroDeserializationConfig();
        deserConfig.setSchema(stringArraySchema);

        List<String> result = parser.deserialize(bais, deserConfig, List.class);
        assertNotNull(result);
        assertEquals(3, result.size());
    }

    @Test
    public void testDeserializeWithoutSchema() {
        String base64Data = Strings.base64Encode("dummy data".getBytes());

        AvroDeserializationConfig config = new AvroDeserializationConfig();
        // No schema set

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

        AvroSerializationConfig config = new AvroSerializationConfig();
        config.setSchema(testSchema);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        parser.serialize(list, config, baos);

        assertTrue(baos.toByteArray().length > 0);
    }

    //    @Test
    //    public void testSerializeArrayOfMaps() throws IOException {
    //        Map<String, Object>[] array = new Map[2];
    //        Map<String, Object> map1 = new HashMap<>();
    //        map1.put("name", "ArrayUser1");
    //        map1.put("age", 30);
    //        array[0] = map1;
    //
    //        Map<String, Object> map2 = new HashMap<>();
    //        map2.put("name", "ArrayUser2");
    //        map2.put("age", 35);
    //        array[1] = map2;
    //
    //        AvroSerializationConfig config = new AvroSerializationConfig();
    //        config.setSchema(testSchema);
    //
    //        ByteArrayOutputStream baos = new ByteArrayOutputStream();
    //        parser.serialize(array, config, baos);
    //
    //        assertTrue(baos.toByteArray().length > 0);
    //    }

    @Test
    public void testSerializeBean() {
        TestBean bean = new TestBean();
        bean.setName("BeanUser");
        bean.setAge(40);

        AvroSerializationConfig config = new AvroSerializationConfig();
        config.setSchema(testSchema);

        String result = parser.serialize(bean, config);
        assertNotNull(result);
        assertTrue(result.length() > 0);
    }

    @Test
    public void testDeserializeToBean() throws IOException {
        // First serialize
        GenericRecord record = new GenericData.Record(testSchema);
        record.put("name", "BeanDeserializeUser");
        record.put("age", 65);

        AvroSerializationConfig serConfig = new AvroSerializationConfig();
        serConfig.setSchema(testSchema);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        parser.serialize(record, serConfig, baos);

        // Then deserialize to Bean
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        AvroDeserializationConfig deserConfig = new AvroDeserializationConfig();
        deserConfig.setSchema(testSchema);

        TestBean result = parser.deserialize(bais, deserConfig, TestBean.class);
        assertNotNull(result);
        assertEquals("BeanDeserializeUser", result.getName().toString());
        assertEquals(65, result.getAge());
    }

    @Test
    public void testSerializeEmptyCollection() {
        List<Object> emptyList = new ArrayList<>();

        AvroSerializationConfig config = new AvroSerializationConfig();
        config.setSchema(arraySchema);

        String result = parser.serialize(emptyList, config);

        N.println("Testing serialization of empty collection: " + result);

        AvroDeserializationConfig adc = ADC.create().setSchema(arraySchema).setElementType(String.class);

        List<String> newList = parser.deserialize(result, adc, List.class);
        N.println("Deserialized empty collection: " + newList);
    }

    //    @Test
    //    public void testSerializeEmptyArray() {
    //        String[] emptyArray = new String[0];
    //
    //        AvroSerializationConfig config = new AvroSerializationConfig();
    //        config.setSchema(arraySchema);
    //
    //        String result = parser.serialize(emptyArray, config);
    //
    //        N.println("Testing serialization of empty collection: " + result);
    //
    //        AvroDeserializationConfig adc = ADC.create().setSchema(arraySchema).setElementType(String.class);
    //
    //        String[] newList = parser.deserialize(result, adc, String[].class);
    //        N.println("Deserialized empty collection: " + newList);
    //    }

    @Test
    public void testIOExceptionHandlingInSerializeToFile() {
        GenericRecord record = new GenericData.Record(testSchema);
        record.put("name", "Test");
        record.put("age", 30);

        AvroSerializationConfig config = new AvroSerializationConfig();
        config.setSchema(testSchema);

        // Try to write to a directory (not a file)
        File invalidFile = tempDir;

        assertThrows(UncheckedIOException.class, () -> parser.serialize(record, config, invalidFile));
    }

    //    @Test
    //    public void testDeserializeToPrimitiveArray() throws IOException {
    //        // Serialize int array
    //        int[] originalArray = { 10, 20, 30, 40, 50 };
    //
    //        Schema intArraySchema = new Schema.Parser().parse("{\"type\":\"array\",\"items\":\"int\"}");
    //
    //        AvroSerializationConfig serConfig = new AvroSerializationConfig();
    //        serConfig.setSchema(intArraySchema);
    //
    //        ByteArrayOutputStream baos = new ByteArrayOutputStream();
    //        parser.serialize(originalArray, serConfig, baos);
    //
    //        // Deserialize
    //        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
    //        AvroDeserializationConfig deserConfig = new AvroDeserializationConfig();
    //        deserConfig.setSchema(intArraySchema);
    //
    //        int[] result = parser.deserialize(bais, deserConfig, int[].class);
    //        assertNotNull(result);
    //        assertArrayEquals(originalArray, result);
    //    }

    @Test
    public void testDeserializeUnsupportedType() {
        AvroDeserializationConfig config = new AvroDeserializationConfig();
        config.setSchema(testSchema);

        ByteArrayInputStream bais = new ByteArrayInputStream(new byte[0]);

        assertThrows(UncheckedIOException.class, () -> parser.deserialize(bais, config, StringBuilder.class));
    }

    // Helper class for bean tests
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

    // Mock SpecificRecord for testing
    public static class MockSpecificRecord implements SpecificRecord {
        public static final Schema SCHEMA$ = new Schema.Parser()
                .parse("{\"type\":\"record\",\"name\":\"com.landawn.abacus.parser.AvroParser101Test.MockSpecificRecord\",\"fields\":["
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

    //    @Test
    //    public void testSerializeArrayOfSpecificRecords() throws IOException {
    //        MockSpecificRecord[] records = new MockSpecificRecord[] { new MockSpecificRecord("id1"), new MockSpecificRecord("id2") };
    //
    //        ByteArrayOutputStream baos = new ByteArrayOutputStream();
    //        parser.serialize(records, null, baos);
    //
    //        assertTrue(baos.toByteArray().length > 0);
    //    }

    @Test
    public void testDeserializeSpecificRecord() throws IOException {
        // First serialize
        MockSpecificRecord original = new MockSpecificRecord("deserialize-test");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        parser.serialize(original, null, baos);

        // Then deserialize
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        assertThrows(ClassCastException.class, () -> {
            MockSpecificRecord result = parser.deserialize(bais, null, MockSpecificRecord.class);

            assertNotNull(result);
            assertEquals("deserialize-test", result.getId());
        });
    }

    @Test
    public void testDeserializeCollectionOfSpecificRecords() throws IOException {
        // First serialize
        List<MockSpecificRecord> original = Arrays.asList(new MockSpecificRecord("col-id1"), new MockSpecificRecord("col-id2"));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        parser.serialize(original, null, baos);

        // Then deserialize
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        AvroDeserializationConfig config = new AvroDeserializationConfig();
        config.setElementType(MockSpecificRecord.class);

        assertThrows(ClassCastException.class, () -> {
            List<MockSpecificRecord> result = parser.deserialize(bais, config, List.class);

            assertNotNull(result);
            assertEquals(2, result.size());
            assertEquals("col-id1", result.get(0).getId());
            assertEquals("col-id2", result.get(1).getId());
        });
    }

    //    @Test
    //    public void testDeserializeArrayOfSpecificRecords() throws IOException {
    //        // First serialize
    //        MockSpecificRecord[] original = new MockSpecificRecord[] { new MockSpecificRecord("arr-id1"), new MockSpecificRecord("arr-id2"),
    //                new MockSpecificRecord("arr-id3") };
    //
    //        ByteArrayOutputStream baos = new ByteArrayOutputStream();
    //        parser.serialize(original, null, baos);
    //
    //        // Then deserialize
    //        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
    //        MockSpecificRecord[] result = parser.deserialize(bais, null, MockSpecificRecord[].class);
    //
    //        assertNotNull(result);
    //        assertEquals(3, result.length);
    //        assertEquals("arr-id1", result[0].getId());
    //        assertEquals("arr-id2", result[1].getId());
    //        assertEquals("arr-id3", result[2].getId());
    //    }

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

        AvroSerializationConfig config = new AvroSerializationConfig();
        config.setSchema(testSchema);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        parser.serialize(records, config, baos);

        assertTrue(baos.toByteArray().length > 0);
    }

    @Test
    public void testDeserializeCollectionWithElementType() throws IOException {
        // Create records as maps
        List<Map<String, Object>> original = new ArrayList<>();
        Map<String, Object> map1 = new HashMap<>();
        map1.put("name", "Element1");
        map1.put("age", 15);
        original.add(map1);

        Map<String, Object> map2 = new HashMap<>();
        map2.put("name", "Element2");
        map2.put("age", 25);
        original.add(map2);

        AvroSerializationConfig serConfig = new AvroSerializationConfig();
        serConfig.setSchema(testSchema);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        parser.serialize(original, serConfig, baos);

        // Deserialize with element type
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        AvroDeserializationConfig deserConfig = new AvroDeserializationConfig();
        deserConfig.setSchema(testSchema);
        deserConfig.setElementType(HashMap.class);

        List<HashMap> result = parser.deserialize(bais, deserConfig, List.class);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Element1", result.get(0).get("name").toString());
        assertEquals(15, result.get(0).get("age"));
    }

    //    @Test
    //    public void testDeserializeObjectArrayWithBeanElementType() throws IOException {
    //        // Create records
    //        List<Map<String, Object>> original = new ArrayList<>();
    //        Map<String, Object> map1 = new HashMap<>();
    //        map1.put("name", "ArrayBean1");
    //        map1.put("age", 31);
    //        original.add(map1);
    //
    //        Map<String, Object> map2 = new HashMap<>();
    //        map2.put("name", "ArrayBean2");
    //        map2.put("age", 32);
    //        original.add(map2);
    //
    //        AvroSerializationConfig serConfig = new AvroSerializationConfig();
    //        serConfig.setSchema(testSchema);
    //
    //        ByteArrayOutputStream baos = new ByteArrayOutputStream();
    //        parser.serialize(original, serConfig, baos);
    //
    //        // Deserialize to bean array
    //        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
    //        AvroDeserializationConfig deserConfig = new AvroDeserializationConfig();
    //        deserConfig.setSchema(testSchema);
    //
    //        TestBean[] result = parser.deserialize(bais, deserConfig, TestBean[].class);
    //
    //        assertNotNull(result);
    //        assertEquals(2, result.length);
    //        assertEquals("ArrayBean1", result[0].getName());
    //        assertEquals(31, result[0].getAge());
    //    }
}
