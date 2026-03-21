package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.TypeReference;
import com.landawn.abacus.TestBase;

public class FastJsonTest extends TestBase {

    @TempDir
    Path tempDir;

    private TestPerson testPerson;
    private String expectedJson;
    private String expectedPrettyJson;

    public static class TestPerson {
        private String name;
        private int age;
        private String email;

        public TestPerson() {
        }

        public TestPerson(String name, int age) {
            this.name = name;
            this.age = age;
        }

        public TestPerson(String name, int age, String email) {
            this.name = name;
            this.age = age;
            this.email = email;
        }

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

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null || getClass() != obj.getClass())
                return false;
            TestPerson that = (TestPerson) obj;
            return age == that.age && java.util.Objects.equals(name, that.name) && java.util.Objects.equals(email, that.email);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(name, age, email);
        }
    }

    @BeforeEach
    public void setUp() {
        testPerson = new TestPerson("John", 30, "john@example.com");
        expectedJson = "{\"age\":30,\"email\":\"john@example.com\",\"name\":\"John\"}";
        expectedPrettyJson = "{\n\t\"age\":30,\n\t\"email\":\"john@example.com\",\n\t\"name\":\"John\"\n}";
    }

    @AfterEach
    public void tearDown() {
    }

    @Test
    public void testToJson_String() {
        String result = FastJson.toJson("test");
        assertEquals("\"test\"", result);
    }

    @Test
    public void testToJson_Number() {
        String result = FastJson.toJson(42);
        assertEquals("42", result);
    }

    @Test
    public void testToJson_List() {
        List<String> list = Arrays.asList("a", "b", "c");
        String result = FastJson.toJson(list);
        assertEquals("[\"a\",\"b\",\"c\"]", result);
    }

    @Test
    public void testToJson_Boolean() {
        assertEquals("true", FastJson.toJson(true));
        assertEquals("false", FastJson.toJson(false));
    }

    // ==================== Round trip tests ====================

    @Test
    public void testRoundTripSerialization() {
        String json = FastJson.toJson(testPerson);

        TestPerson result = FastJson.fromJson(json, TestPerson.class);

        assertEquals(testPerson, result);
    }

    // ==================== toJson(Object) ====================

    @Test
    public void testToJson() {
        String result = FastJson.toJson(testPerson);
        assertNotNull(result);
        assertTrue(result.contains("\"name\":\"John\""));
        assertTrue(result.contains("\"age\":30"));
        assertTrue(result.contains("\"email\":\"john@example.com\""));
    }

    @Test
    public void testToJson_Null() {
        String result = FastJson.toJson(null);
        assertEquals("null", result);
    }

    @Test
    public void testToJson_EmptyMap() {
        Map<String, Object> emptyMap = new HashMap<>();
        String result = FastJson.toJson(emptyMap);
        assertEquals("{}", result);
    }

    @Test
    public void testToJson_EmptyList() {
        List<Object> emptyList = Collections.emptyList();
        String result = FastJson.toJson(emptyList);
        assertEquals("[]", result);
    }

    // ==================== toJson(Object, boolean) ====================

    @Test
    public void testToJson_PrettyFormatTrue() {
        String result = FastJson.toJson(testPerson, true);
        assertNotNull(result);
        assertTrue(result.contains("\n"));
        assertTrue(result.contains("\t"));
        assertTrue(result.contains("\"name\":\"John\""));
    }

    @Test
    public void testToJson_PrettyFormatFalse() {
        String result = FastJson.toJson(testPerson, false);
        assertNotNull(result);
        assertFalse(result.contains("\n"));
        assertFalse(result.contains("\t"));
        assertTrue(result.contains("\"name\":\"John\""));
    }

    @Test
    public void testToJson_PrettyFormatNull() {
        String result = FastJson.toJson(null, true);
        assertEquals("null", result);
    }

    // ==================== toJson(Object, Feature...) ====================

    @Test
    public void testToJson_WithFeatures() {
        String result = FastJson.toJson(testPerson, JSONWriter.Feature.PrettyFormat);
        assertNotNull(result);
        assertTrue(result.contains("\n"));
        assertTrue(result.contains("\"name\":\"John\""));
    }

    @Test
    public void testToJson_WithMultipleFeatures() {
        TestPerson personWithNull = new TestPerson("Jane", 25, null);
        String result = FastJson.toJson(personWithNull, JSONWriter.Feature.WriteNulls, JSONWriter.Feature.PrettyFormat);
        assertNotNull(result);
        assertTrue(result.contains("\"email\":null"));
        assertTrue(result.contains("\n"));
    }

    @Test
    public void testToJson_WithFeaturesNull() {
        String result = FastJson.toJson(null, JSONWriter.Feature.PrettyFormat);
        assertEquals("null", result);
    }

    @Test
    public void testToJson_WithNoFeatures() {
        String result = FastJson.toJson(testPerson, new JSONWriter.Feature[0]);
        assertNotNull(result);
        assertTrue(result.contains("\"name\":\"John\""));
    }

    // ==================== toJson(Object, Context) ====================

    @Test
    public void testToJson_WithContext() {
        JSONWriter.Context context = new JSONWriter.Context();
        context.setDateFormat("yyyy-MM-dd");
        String result = FastJson.toJson(testPerson, context);
        assertNotNull(result);
        assertTrue(result.contains("\"name\":\"John\""));
    }

    @Test
    public void testToJson_WithContextNull() {
        JSONWriter.Context context = new JSONWriter.Context();
        String result = FastJson.toJson(null, context);
        assertEquals("null", result);
    }

    @Test
    public void testRoundTripSerializationWithList() {
        List<TestPerson> people = Arrays.asList(new TestPerson("John", 30), new TestPerson("Jane", 25), new TestPerson("Bob", 35));

        String json = FastJson.toJson(people);

        List<TestPerson> result = FastJson.fromJson(json, new TypeReference<List<TestPerson>>() {
        });

        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals(people.get(0), result.get(0));
        assertEquals(people.get(1), result.get(1));
        assertEquals(people.get(2), result.get(2));
    }

    @Test
    public void testRoundTripWithMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", "test");
        map.put("count", 42);

        String json = FastJson.toJson(map);
        Map<String, Object> result = FastJson.fromJson(json, new TypeReference<Map<String, Object>>() {
        });

        assertNotNull(result);
        assertEquals("test", result.get("name"));
        assertEquals(42, result.get("count"));
    }

    // ==================== toJson(Object, File) ====================

    @Test
    public void testToJson_ToFile() throws Exception {
        File outputFile = tempDir.resolve("test.json").toFile();

        FastJson.toJson(testPerson, outputFile);

        assertTrue(outputFile.exists());
        String content = Files.readString(outputFile.toPath());
        assertTrue(content.contains("\"name\":\"John\""));
        assertTrue(content.contains("\"age\":30"));
    }

    @Test
    public void testToJson_ToFileNull() throws Exception {
        File outputFile = tempDir.resolve("null.json").toFile();

        FastJson.toJson(null, outputFile);

        assertTrue(outputFile.exists());
        String content = Files.readString(outputFile.toPath());
        assertEquals("null", content);
    }

    @Test
    public void testToJson_ToFileIOException() {
        File invalidFile = new File("/invalid/path/test.json");

        assertThrows(RuntimeException.class, () -> {
            FastJson.toJson(testPerson, invalidFile);
        });
    }

    // ==================== toJson(Object, File, Feature...) ====================

    @Test
    public void testToJson_ToFileWithFeatures() throws Exception {
        File outputFile = tempDir.resolve("pretty.json").toFile();

        FastJson.toJson(testPerson, outputFile, JSONWriter.Feature.PrettyFormat);

        assertTrue(outputFile.exists());
        String content = Files.readString(outputFile.toPath());
        assertTrue(content.contains("\n"));
        assertTrue(content.contains("\"name\":\"John\""));
    }

    @Test
    public void testToJson_ToFileWithFeaturesNull() throws Exception {
        File outputFile = tempDir.resolve("null_pretty.json").toFile();

        FastJson.toJson(null, outputFile, JSONWriter.Feature.PrettyFormat);

        assertTrue(outputFile.exists());
        String content = Files.readString(outputFile.toPath());
        assertEquals("null", content);
    }

    @Test
    public void testToJson_ToFileWithFeaturesIOException() {
        File invalidFile = new File("/invalid/path/test.json");

        assertThrows(RuntimeException.class, () -> {
            FastJson.toJson(testPerson, invalidFile, JSONWriter.Feature.PrettyFormat);
        });
    }

    @Test
    public void testToJson_ToFileWithWriteNulls() throws Exception {
        File outputFile = tempDir.resolve("nulls.json").toFile();
        TestPerson personWithNull = new TestPerson("Jane", 25, null);

        FastJson.toJson(personWithNull, outputFile, JSONWriter.Feature.WriteNulls);

        String content = Files.readString(outputFile.toPath());
        assertTrue(content.contains("\"email\":null"));
    }

    // ==================== toJson(Object, File, Context) ====================

    @Test
    public void testToJson_ToFileWithContext() throws Exception {
        File outputFile = tempDir.resolve("context.json").toFile();
        JSONWriter.Context context = new JSONWriter.Context();
        context.setDateFormat("yyyy-MM-dd");

        FastJson.toJson(testPerson, outputFile, context);

        assertTrue(outputFile.exists());
        String content = Files.readString(outputFile.toPath());
        assertTrue(content.contains("\"name\":\"John\""));
    }

    @Test
    public void testToJson_ToFileWithContextIOException() {
        File invalidFile = new File("/invalid/path/test.json");
        JSONWriter.Context context = new JSONWriter.Context();

        assertThrows(RuntimeException.class, () -> {
            FastJson.toJson(testPerson, invalidFile, context);
        });
    }

    // ==================== toJson(Object, OutputStream) ====================

    @Test
    public void testToJson_ToOutputStream() throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        FastJson.toJson(testPerson, outputStream);

        String result = outputStream.toString();
        assertTrue(result.contains("\"name\":\"John\""));
        assertTrue(result.contains("\"age\":30"));
    }

    @Test
    public void testToJson_ToOutputStreamNull() throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        FastJson.toJson(null, outputStream);

        String result = outputStream.toString();
        assertEquals("null", result);
    }

    @Test
    public void testToJson_ToOutputStreamEmptyObject() throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        FastJson.toJson(new HashMap<>(), outputStream);

        String result = outputStream.toString();
        assertEquals("{}", result);
    }

    // ==================== toJson(Object, OutputStream, Feature...) ====================

    @Test
    public void testToJson_ToOutputStreamWithFeatures() throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        FastJson.toJson(testPerson, outputStream, JSONWriter.Feature.PrettyFormat);

        String result = outputStream.toString();
        assertTrue(result.contains("\n"));
        assertTrue(result.contains("\"name\":\"John\""));
    }

    @Test
    public void testToJson_ToOutputStreamWithWriteNulls() throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        TestPerson personWithNull = new TestPerson("Test", 10, null);

        FastJson.toJson(personWithNull, outputStream, JSONWriter.Feature.WriteNulls);

        String result = outputStream.toString();
        assertTrue(result.contains("\"email\":null"));
    }

    // ==================== toJson(Object, OutputStream, Context) ====================

    @Test
    public void testToJson_ToOutputStreamWithContext() throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        JSONWriter.Context context = new JSONWriter.Context();

        FastJson.toJson(testPerson, outputStream, context);

        String result = outputStream.toString();
        assertTrue(result.contains("\"name\":\"John\""));
    }

    @Test
    public void testToJson_ToOutputStreamWithContextNull() throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        JSONWriter.Context context = new JSONWriter.Context();

        FastJson.toJson(null, outputStream, context);

        String result = outputStream.toString();
        assertEquals("null", result);
    }

    // ==================== toJson(Object, Writer) ====================

    @Test
    public void testToJson_ToWriter() throws Exception {
        StringWriter writer = new StringWriter();

        FastJson.toJson(testPerson, writer);

        String result = writer.toString();
        assertTrue(result.contains("\"name\":\"John\""));
        assertTrue(result.contains("\"age\":30"));
    }

    @Test
    public void testToJson_ToWriterNull() throws Exception {
        StringWriter writer = new StringWriter();

        FastJson.toJson(null, writer);

        String result = writer.toString();
        assertEquals("null", result);
    }

    @Test
    public void testToJson_ToWriterIOException() throws Exception {
        Writer mockWriter = new Writer() {
            @Override
            public void write(char[] cbuf, int off, int len) throws IOException {
                throw new IOException("Test exception");
            }

            @Override
            public void flush() throws IOException {
            }

            @Override
            public void close() throws IOException {
            }
        };

        assertThrows(RuntimeException.class, () -> {
            FastJson.toJson(testPerson, mockWriter);
        });
    }

    // ==================== toJson(Object, Writer, Feature...) ====================

    @Test
    public void testToJson_ToWriterWithFeatures() throws Exception {
        StringWriter writer = new StringWriter();

        FastJson.toJson(testPerson, writer, JSONWriter.Feature.PrettyFormat);

        String result = writer.toString();
        assertTrue(result.contains("\n"));
        assertTrue(result.contains("\"name\":\"John\""));
    }

    @Test
    public void testToJson_ToWriterWithFeaturesIOException() throws Exception {
        Writer mockWriter = new Writer() {
            @Override
            public void write(char[] cbuf, int off, int len) throws IOException {
                throw new IOException("Test exception");
            }

            @Override
            public void flush() throws IOException {
            }

            @Override
            public void close() throws IOException {
            }
        };

        assertThrows(RuntimeException.class, () -> {
            FastJson.toJson(testPerson, mockWriter, JSONWriter.Feature.PrettyFormat);
        });
    }

    // ==================== toJson(Object, Writer, Context) ====================

    @Test
    public void testToJson_ToWriterWithContext() throws Exception {
        StringWriter writer = new StringWriter();
        JSONWriter.Context context = new JSONWriter.Context();

        FastJson.toJson(testPerson, writer, context);

        String result = writer.toString();
        assertTrue(result.contains("\"name\":\"John\""));
    }

    @Test
    public void testToJson_ToWriterWithContextIOException() throws Exception {
        Writer mockWriter = new Writer() {
            @Override
            public void write(char[] cbuf, int off, int len) throws IOException {
                throw new IOException("Test exception");
            }

            @Override
            public void flush() throws IOException {
            }

            @Override
            public void close() throws IOException {
            }
        };

        JSONWriter.Context context = new JSONWriter.Context();

        assertThrows(RuntimeException.class, () -> {
            FastJson.toJson(testPerson, mockWriter, context);
        });
    }

    @Test
    public void testFileRoundTrip() throws Exception {
        File jsonFile = tempDir.resolve("roundtrip.json").toFile();

        FastJson.toJson(testPerson, jsonFile);

        try (FileReader reader = new FileReader(jsonFile)) {
            TestPerson result = FastJson.fromJson(reader, TestPerson.class);
            assertEquals(testPerson, result);
        }
    }

    @Test
    public void testRoundTripWithOutputStream() throws Exception {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        FastJson.toJson(testPerson, os);

        byte[] bytes = os.toByteArray();
        TestPerson result = FastJson.fromJson(bytes, TestPerson.class);

        assertEquals(testPerson, result);
    }

    @Test
    public void testFromJson_StringToBoolean() {
        assertEquals(Boolean.TRUE, FastJson.fromJson("true", Boolean.class));
        assertEquals(Boolean.FALSE, FastJson.fromJson("false", Boolean.class));
    }

    // ==================== fromJson(byte[], Class) ====================

    @Test
    public void testFromJson_ByteArray() {
        String json = "{\"name\":\"John\",\"age\":30,\"email\":\"john@example.com\"}";
        byte[] jsonBytes = json.getBytes();

        TestPerson result = FastJson.fromJson(jsonBytes, TestPerson.class);

        assertNotNull(result);
        assertEquals("John", result.getName());
        assertEquals(30, result.getAge());
        assertEquals("john@example.com", result.getEmail());
    }

    @Test
    public void testFromJson_ByteArrayNull() {
        byte[] jsonBytes = "null".getBytes();

        TestPerson result = FastJson.fromJson(jsonBytes, TestPerson.class);

        assertNull(result);
    }

    @Test
    public void testFromJson_ByteArrayEmpty() {
        byte[] jsonBytes = "{}".getBytes();

        TestPerson result = FastJson.fromJson(jsonBytes, TestPerson.class);

        assertNotNull(result);
        assertNull(result.getName());
        assertEquals(0, result.getAge());
    }

    @Test
    public void testFromJson_ByteArrayToMap() {
        byte[] jsonBytes = "{\"key\":\"value\"}".getBytes();

        Map result = FastJson.fromJson(jsonBytes, Map.class);

        assertNotNull(result);
        assertEquals("value", result.get("key"));
    }

    // ==================== fromJson(byte[], int, int, Class) ====================

    @Test
    public void testFromJson_ByteArrayWithOffset() {
        String fullString = "prefix{\"name\":\"Jane\",\"age\":25}suffix";
        byte[] buffer = fullString.getBytes();
        int offset = 6;
        int length = 24;

        TestPerson result = FastJson.fromJson(buffer, offset, length, TestPerson.class);

        assertNotNull(result);
        assertEquals("Jane", result.getName());
        assertEquals(25, result.getAge());
    }

    @Test
    public void testFromJson_ByteArrayWithOffsetNull() {
        byte[] buffer = "prefixnullsuffix".getBytes();
        int offset = 6;
        int length = 4;

        TestPerson result = FastJson.fromJson(buffer, offset, length, TestPerson.class);

        assertNull(result);
    }

    @Test
    public void testFromJson_ByteArrayWithOffsetFullArray() {
        String json = "{\"name\":\"Bob\",\"age\":40}";
        byte[] buffer = json.getBytes();

        TestPerson result = FastJson.fromJson(buffer, 0, buffer.length, TestPerson.class);

        assertNotNull(result);
        assertEquals("Bob", result.getName());
        assertEquals(40, result.getAge());
    }

    // ==================== fromJson(String, Class) ====================

    @Test
    public void testFromJson_String() {
        String json = "{\"name\":\"John\",\"age\":30,\"email\":\"john@example.com\"}";

        TestPerson result = FastJson.fromJson(json, TestPerson.class);

        assertNotNull(result);
        assertEquals("John", result.getName());
        assertEquals(30, result.getAge());
        assertEquals("john@example.com", result.getEmail());
    }

    @Test
    public void testFromJson_StringNull() {
        String json = "null";

        TestPerson result = FastJson.fromJson(json, TestPerson.class);

        assertNull(result);
    }

    @Test
    public void testFromJson_StringPrimitive() {
        String json = "42";

        Integer result = FastJson.fromJson(json, Integer.class);

        assertNotNull(result);
        assertEquals(42, result.intValue());
    }

    @Test
    public void testFromJson_StringList() {
        String json = "[\"a\",\"b\",\"c\"]";

        List result = FastJson.fromJson(json, List.class);

        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("a", result.get(0));
    }

    @Test
    public void testFromJson_StringEmptyObject() {
        TestPerson result = FastJson.fromJson("{}", TestPerson.class);
        assertNotNull(result);
        assertNull(result.getName());
        assertEquals(0, result.getAge());
    }

    // ==================== fromJson(String, Class, Feature...) ====================

    @Test
    public void testFromJson_StringWithFeatures() {
        String json = "{\"name\":\"John\",\"age\":30}";

        TestPerson result = FastJson.fromJson(json, TestPerson.class, JSONReader.Feature.SupportSmartMatch);

        assertNotNull(result);
        assertEquals("John", result.getName());
        assertEquals(30, result.getAge());
    }

    @Test
    public void testFromJson_StringWithFeaturesNull() {
        String json = "null";

        TestPerson result = FastJson.fromJson(json, TestPerson.class, JSONReader.Feature.SupportSmartMatch);

        assertNull(result);
    }

    @Test
    public void testFromJson_StringWithNoFeatures() {
        String json = "{\"name\":\"John\",\"age\":30}";

        TestPerson result = FastJson.fromJson(json, TestPerson.class, new JSONReader.Feature[0]);

        assertNotNull(result);
        assertEquals("John", result.getName());
    }

    // ==================== fromJson(String, Class, Context) ====================

    @Test
    public void testFromJson_StringWithContext() {
        String json = "{\"name\":\"John\",\"age\":30}";
        JSONReader.Context context = new JSONReader.Context();

        TestPerson result = FastJson.fromJson(json, TestPerson.class, context);

        assertNotNull(result);
        assertEquals("John", result.getName());
        assertEquals(30, result.getAge());
    }

    @Test
    public void testFromJson_StringWithContextNull() {
        JSONReader.Context context = new JSONReader.Context();

        TestPerson result = FastJson.fromJson("null", TestPerson.class, context);

        assertNull(result);
    }

    // ==================== fromJson(String, Type) ====================

    @Test
    public void testFromJson_StringWithType() {
        String json = "[{\"name\":\"John\",\"age\":30},{\"name\":\"Jane\",\"age\":25}]";
        Type listType = new TypeReference<List<TestPerson>>() {
        }.getType();

        List<TestPerson> result = FastJson.fromJson(json, listType);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("John", result.get(0).getName());
        assertEquals("Jane", result.get(1).getName());
    }

    @Test
    public void testFromJson_StringWithTypeNull() {
        String json = "null";
        Type listType = new TypeReference<List<TestPerson>>() {
        }.getType();

        List<TestPerson> result = FastJson.fromJson(json, listType);

        assertNull(result);
    }

    @Test
    public void testFromJson_StringWithTypeMap() {
        String json = "{\"a\":1,\"b\":2}";
        Type mapType = new TypeReference<Map<String, Integer>>() {
        }.getType();

        Map<String, Integer> result = FastJson.fromJson(json, mapType);

        assertNotNull(result);
        assertEquals(1, result.get("a"));
        assertEquals(2, result.get("b"));
    }

    // ==================== fromJson(String, Type, Feature...) ====================

    @Test
    public void testFromJson_StringWithTypeAndFeatures() {
        String json = "[{\"name\":\"John\",\"age\":30}]";
        Type listType = new TypeReference<List<TestPerson>>() {
        }.getType();

        List<TestPerson> result = FastJson.fromJson(json, listType, JSONReader.Feature.SupportSmartMatch);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("John", result.get(0).getName());
    }

    @Test
    public void testFromJson_StringWithTypeAndFeaturesNull() {
        Type listType = new TypeReference<List<TestPerson>>() {
        }.getType();

        List<TestPerson> result = FastJson.fromJson("null", listType, JSONReader.Feature.SupportSmartMatch);

        assertNull(result);
    }

    // ==================== fromJson(String, Type, Context) ====================

    @Test
    public void testFromJson_StringWithTypeAndContext() {
        String json = "[{\"name\":\"John\",\"age\":30}]";
        Type listType = new TypeReference<List<TestPerson>>() {
        }.getType();
        JSONReader.Context context = new JSONReader.Context();

        List<TestPerson> result = FastJson.fromJson(json, listType, context);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("John", result.get(0).getName());
    }

    @Test
    public void testFromJson_StringWithTypeAndContextNull() {
        Type listType = new TypeReference<List<TestPerson>>() {
        }.getType();
        JSONReader.Context context = new JSONReader.Context();

        List<TestPerson> result = FastJson.fromJson("null", listType, context);

        assertNull(result);
    }

    // ==================== fromJson(String, TypeReference) ====================

    @Test
    public void testFromJson_StringWithTypeReference() {
        String json = "[{\"name\":\"John\",\"age\":30},{\"name\":\"Jane\",\"age\":25}]";

        List<TestPerson> result = FastJson.fromJson(json, new TypeReference<List<TestPerson>>() {
        });

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("John", result.get(0).getName());
        assertEquals("Jane", result.get(1).getName());
    }

    @Test
    public void testFromJson_StringWithTypeReferenceNull() {
        String json = "null";

        List<TestPerson> result = FastJson.fromJson(json, new TypeReference<List<TestPerson>>() {
        });

        assertNull(result);
    }

    @Test
    public void testFromJson_StringWithTypeReferenceMap() {
        String json = "{\"key1\":\"value1\",\"key2\":\"value2\"}";

        Map<String, String> result = FastJson.fromJson(json, new TypeReference<Map<String, String>>() {
        });

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("value1", result.get("key1"));
        assertEquals("value2", result.get("key2"));
    }

    @Test
    public void testFromJson_StringWithTypeReferenceNestedMap() {
        String json = "{\"outer\":{\"inner\":\"value\"}}";

        Map<String, Map<String, String>> result = FastJson.fromJson(json, new TypeReference<Map<String, Map<String, String>>>() {
        });

        assertNotNull(result);
        assertEquals("value", result.get("outer").get("inner"));
    }

    // ==================== fromJson(String, TypeReference, Feature...) ====================

    @Test
    public void testFromJson_StringWithTypeReferenceAndFeatures() {
        String json = "[{\"name\":\"John\",\"age\":30}]";

        List<TestPerson> result = FastJson.fromJson(json, new TypeReference<List<TestPerson>>() {
        }, JSONReader.Feature.SupportSmartMatch);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("John", result.get(0).getName());
    }

    @Test
    public void testFromJson_StringWithTypeReferenceAndFeaturesNull() {
        List<TestPerson> result = FastJson.fromJson("null", new TypeReference<List<TestPerson>>() {
        }, JSONReader.Feature.SupportSmartMatch);

        assertNull(result);
    }

    // ==================== fromJson(String, TypeReference, Context) ====================

    @Test
    public void testFromJson_StringWithTypeReferenceAndContext() {
        String json = "[{\"name\":\"John\",\"age\":30}]";
        JSONReader.Context context = new JSONReader.Context();

        List<TestPerson> result = FastJson.fromJson(json, new TypeReference<List<TestPerson>>() {
        }, context);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("John", result.get(0).getName());
    }

    @Test
    public void testFromJson_StringWithTypeReferenceAndContextNull() {
        JSONReader.Context context = new JSONReader.Context();

        List<TestPerson> result = FastJson.fromJson("null", new TypeReference<List<TestPerson>>() {
        }, context);

        assertNull(result);
    }

    // ==================== fromJson(Reader, Class) ====================

    @Test
    public void testFromJson_Reader() {
        String json = "{\"name\":\"John\",\"age\":30,\"email\":\"john@example.com\"}";
        Reader reader = new StringReader(json);

        TestPerson result = FastJson.fromJson(reader, TestPerson.class);

        assertNotNull(result);
        assertEquals("John", result.getName());
        assertEquals(30, result.getAge());
        assertEquals("john@example.com", result.getEmail());
    }

    @Test
    public void testFromJson_ReaderNull() {
        String json = "null";
        Reader reader = new StringReader(json);

        TestPerson result = FastJson.fromJson(reader, TestPerson.class);

        assertNull(result);
    }

    // ==================== fromJson(Reader, Class, Feature...) ====================

    @Test
    public void testFromJson_ReaderWithFeatures() {
        String json = "{\"name\":\"John\",\"age\":30}";
        Reader reader = new StringReader(json);

        TestPerson result = FastJson.fromJson(reader, TestPerson.class, JSONReader.Feature.SupportSmartMatch);

        assertNotNull(result);
        assertEquals("John", result.getName());
        assertEquals(30, result.getAge());
    }

    @Test
    public void testFromJson_ReaderWithFeaturesNull() {
        Reader reader = new StringReader("null");

        TestPerson result = FastJson.fromJson(reader, TestPerson.class, JSONReader.Feature.SupportSmartMatch);

        assertNull(result);
    }

    // ==================== fromJson(Reader, Class, Context) ====================

    @Test
    public void testFromJson_ReaderWithContext() {
        String json = "{\"name\":\"John\",\"age\":30}";
        Reader reader = new StringReader(json);
        JSONReader.Context context = new JSONReader.Context();

        TestPerson result = FastJson.fromJson(reader, TestPerson.class, context);

        assertNotNull(result);
        assertEquals("John", result.getName());
        assertEquals(30, result.getAge());
    }

    @Test
    public void testFromJson_ReaderWithContextNull() {
        Reader reader = new StringReader("null");
        JSONReader.Context context = new JSONReader.Context();

        TestPerson result = FastJson.fromJson(reader, TestPerson.class, context);

        assertNull(result);
    }

    // ==================== fromJson(Reader, Type) ====================

    @Test
    public void testFromJson_ReaderWithType() {
        String json = "[{\"name\":\"John\",\"age\":30}]";
        Reader reader = new StringReader(json);
        Type listType = new TypeReference<List<TestPerson>>() {
        }.getType();

        List<TestPerson> result = FastJson.fromJson(reader, listType);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("John", result.get(0).getName());
    }

    @Test
    public void testFromJson_ReaderWithTypeNull() {
        Reader reader = new StringReader("null");
        Type listType = new TypeReference<List<TestPerson>>() {
        }.getType();

        List<TestPerson> result = FastJson.fromJson(reader, listType);

        assertNull(result);
    }

    // ==================== fromJson(Reader, Type, Feature...) ====================

    @Test
    public void testFromJson_ReaderWithTypeAndFeatures() {
        String json = "[{\"name\":\"John\",\"age\":30}]";
        Reader reader = new StringReader(json);
        Type listType = new TypeReference<List<TestPerson>>() {
        }.getType();

        List<TestPerson> result = FastJson.fromJson(reader, listType, JSONReader.Feature.SupportSmartMatch);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("John", result.get(0).getName());
    }

    @Test
    public void testFromJson_ReaderWithTypeAndFeaturesNull() {
        Reader reader = new StringReader("null");
        Type listType = new TypeReference<List<TestPerson>>() {
        }.getType();

        List<TestPerson> result = FastJson.fromJson(reader, listType, JSONReader.Feature.SupportSmartMatch);

        assertNull(result);
    }

    // ==================== fromJson(Reader, Type, Context) ====================

    @Test
    public void testFromJson_ReaderWithTypeAndContext() {
        String json = "[{\"name\":\"John\",\"age\":30}]";
        Reader reader = new StringReader(json);
        Type listType = new TypeReference<List<TestPerson>>() {
        }.getType();
        JSONReader.Context context = new JSONReader.Context();

        List<TestPerson> result = FastJson.fromJson(reader, listType, context);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("John", result.get(0).getName());
    }

    @Test
    public void testFromJson_ReaderWithTypeAndContextNull() {
        Reader reader = new StringReader("null");
        Type listType = new TypeReference<List<TestPerson>>() {
        }.getType();
        JSONReader.Context context = new JSONReader.Context();

        List<TestPerson> result = FastJson.fromJson(reader, listType, context);

        assertNull(result);
    }

    @Test
    public void testFromJson_ReaderFromFile() throws Exception {
        File jsonFile = tempDir.resolve("person.json").toFile();
        Files.writeString(jsonFile.toPath(), "{\"name\":\"FileReader\",\"age\":35}");

        try (FileReader reader = new FileReader(jsonFile)) {
            TestPerson result = FastJson.fromJson(reader, TestPerson.class);

            assertNotNull(result);
            assertEquals("FileReader", result.getName());
            assertEquals(35, result.getAge());
        }
    }
}
