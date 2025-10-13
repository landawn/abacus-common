package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.io.TempDir;

import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.TypeReference;
import com.landawn.abacus.TestBase;

@Tag("new-test")
public class FastJson100Test extends TestBase {

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
    public void testToJsonObject() {
        String result = FastJson.toJson(testPerson);
        assertNotNull(result);
        assertTrue(result.contains("\"name\":\"John\""));
        assertTrue(result.contains("\"age\":30"));
        assertTrue(result.contains("\"email\":\"john@example.com\""));
    }

    @Test
    public void testToJsonObjectNull() {
        String result = FastJson.toJson(null);
        assertEquals("null", result);
    }

    @Test
    public void testToJsonObjectString() {
        String result = FastJson.toJson("test");
        assertEquals("\"test\"", result);
    }

    @Test
    public void testToJsonObjectNumber() {
        String result = FastJson.toJson(42);
        assertEquals("42", result);
    }

    @Test
    public void testToJsonObjectList() {
        List<String> list = Arrays.asList("a", "b", "c");
        String result = FastJson.toJson(list);
        assertEquals("[\"a\",\"b\",\"c\"]", result);
    }

    @Test
    public void testToJsonObjectPrettyFormatTrue() {
        String result = FastJson.toJson(testPerson, true);
        assertNotNull(result);
        assertTrue(result.contains("\n"));
        assertTrue(result.contains("\t"));
        assertTrue(result.contains("\"name\":\"John\""));
    }

    @Test
    public void testToJsonObjectPrettyFormatFalse() {
        String result = FastJson.toJson(testPerson, false);
        assertNotNull(result);
        assertTrue(!result.contains("\n"));
        assertTrue(!result.contains("\t"));
        assertTrue(result.contains("\"name\":\"John\""));
    }

    @Test
    public void testToJsonObjectPrettyFormatNull() {
        String result = FastJson.toJson(null, true);
        assertEquals("null", result);
    }

    @Test
    public void testToJsonObjectWithFeatures() {
        String result = FastJson.toJson(testPerson, JSONWriter.Feature.PrettyFormat);
        assertNotNull(result);
        assertTrue(result.contains("\n"));
        assertTrue(result.contains("\"name\":\"John\""));
    }

    @Test
    public void testToJsonObjectWithMultipleFeatures() {
        TestPerson personWithNull = new TestPerson("Jane", 25, null);
        String result = FastJson.toJson(personWithNull, JSONWriter.Feature.WriteNulls, JSONWriter.Feature.PrettyFormat);
        assertNotNull(result);
        assertTrue(result.contains("\"email\":null"));
        assertTrue(result.contains("\n"));
    }

    @Test
    public void testToJsonObjectWithFeaturesNull() {
        String result = FastJson.toJson(null, JSONWriter.Feature.PrettyFormat);
        assertEquals("null", result);
    }

    @Test
    public void testToJsonObjectWithContext() {
        JSONWriter.Context context = new JSONWriter.Context();
        context.setDateFormat("yyyy-MM-dd");
        String result = FastJson.toJson(testPerson, context);
        assertNotNull(result);
        assertTrue(result.contains("\"name\":\"John\""));
    }

    @Test
    public void testToJsonObjectWithContextNull() {
        JSONWriter.Context context = new JSONWriter.Context();
        String result = FastJson.toJson(null, context);
        assertEquals("null", result);
    }

    @Test
    public void testToJsonObjectToFile() throws Exception {
        File outputFile = tempDir.resolve("test.json").toFile();

        FastJson.toJson(testPerson, outputFile);

        assertTrue(outputFile.exists());
        String content = Files.readString(outputFile.toPath());
        assertTrue(content.contains("\"name\":\"John\""));
        assertTrue(content.contains("\"age\":30"));
    }

    @Test
    public void testToJsonObjectToFileNull() throws Exception {
        File outputFile = tempDir.resolve("null.json").toFile();

        FastJson.toJson(null, outputFile);

        assertTrue(outputFile.exists());
        String content = Files.readString(outputFile.toPath());
        assertEquals("null", content);
    }

    @Test
    public void testToJsonObjectToFileIOException() {
        File invalidFile = new File("/invalid/path/test.json");

        assertThrows(RuntimeException.class, () -> {
            FastJson.toJson(testPerson, invalidFile);
        });
    }

    @Test
    public void testToJsonObjectToFileWithFeatures() throws Exception {
        File outputFile = tempDir.resolve("pretty.json").toFile();

        FastJson.toJson(testPerson, outputFile, JSONWriter.Feature.PrettyFormat);

        assertTrue(outputFile.exists());
        String content = Files.readString(outputFile.toPath());
        assertTrue(content.contains("\n"));
        assertTrue(content.contains("\"name\":\"John\""));
    }

    @Test
    public void testToJsonObjectToFileWithFeaturesNull() throws Exception {
        File outputFile = tempDir.resolve("null_pretty.json").toFile();

        FastJson.toJson(null, outputFile, JSONWriter.Feature.PrettyFormat);

        assertTrue(outputFile.exists());
        String content = Files.readString(outputFile.toPath());
        assertEquals("null", content);
    }

    @Test
    public void testToJsonObjectToFileWithContext() throws Exception {
        File outputFile = tempDir.resolve("context.json").toFile();
        JSONWriter.Context context = new JSONWriter.Context();
        context.setDateFormat("yyyy-MM-dd");

        FastJson.toJson(testPerson, outputFile, context);

        assertTrue(outputFile.exists());
        String content = Files.readString(outputFile.toPath());
        assertTrue(content.contains("\"name\":\"John\""));
    }

    @Test
    public void testToJsonObjectToOutputStream() throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        FastJson.toJson(testPerson, outputStream);

        String result = outputStream.toString();
        assertTrue(result.contains("\"name\":\"John\""));
        assertTrue(result.contains("\"age\":30"));
    }

    @Test
    public void testToJsonObjectToOutputStreamNull() throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        FastJson.toJson(null, outputStream);

        String result = outputStream.toString();
        assertEquals("null", result);
    }

    @Test
    public void testToJsonObjectToOutputStreamWithFeatures() throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        FastJson.toJson(testPerson, outputStream, JSONWriter.Feature.PrettyFormat);

        String result = outputStream.toString();
        assertTrue(result.contains("\n"));
        assertTrue(result.contains("\"name\":\"John\""));
    }

    @Test
    public void testToJsonObjectToOutputStreamWithContext() throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        JSONWriter.Context context = new JSONWriter.Context();

        FastJson.toJson(testPerson, outputStream, context);

        String result = outputStream.toString();
        assertTrue(result.contains("\"name\":\"John\""));
    }

    @Test
    public void testToJsonObjectToWriter() throws Exception {
        StringWriter writer = new StringWriter();

        FastJson.toJson(testPerson, writer);

        String result = writer.toString();
        assertTrue(result.contains("\"name\":\"John\""));
        assertTrue(result.contains("\"age\":30"));
    }

    @Test
    public void testToJsonObjectToWriterNull() throws Exception {
        StringWriter writer = new StringWriter();

        FastJson.toJson(null, writer);

        String result = writer.toString();
        assertEquals("null", result);
    }

    @Test
    public void testToJsonObjectToWriterIOException() throws Exception {
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

    @Test
    public void testToJsonObjectToWriterWithFeatures() throws Exception {
        StringWriter writer = new StringWriter();

        FastJson.toJson(testPerson, writer, JSONWriter.Feature.PrettyFormat);

        String result = writer.toString();
        assertTrue(result.contains("\n"));
        assertTrue(result.contains("\"name\":\"John\""));
    }

    @Test
    public void testToJsonObjectToWriterWithContext() throws Exception {
        StringWriter writer = new StringWriter();
        JSONWriter.Context context = new JSONWriter.Context();

        FastJson.toJson(testPerson, writer, context);

        String result = writer.toString();
        assertTrue(result.contains("\"name\":\"John\""));
    }

    @Test
    public void testFromJsonByteArray() {
        String json = "{\"name\":\"John\",\"age\":30,\"email\":\"john@example.com\"}";
        byte[] jsonBytes = json.getBytes();

        TestPerson result = FastJson.fromJson(jsonBytes, TestPerson.class);

        assertNotNull(result);
        assertEquals("John", result.getName());
        assertEquals(30, result.getAge());
        assertEquals("john@example.com", result.getEmail());
    }

    @Test
    public void testFromJsonByteArrayNull() {
        byte[] jsonBytes = "null".getBytes();

        TestPerson result = FastJson.fromJson(jsonBytes, TestPerson.class);

        assertNull(result);
    }

    @Test
    public void testFromJsonByteArrayEmpty() {
        byte[] jsonBytes = "{}".getBytes();

        TestPerson result = FastJson.fromJson(jsonBytes, TestPerson.class);

        assertNotNull(result);
        assertNull(result.getName());
        assertEquals(0, result.getAge());
    }

    @Test
    public void testFromJsonByteArrayWithOffsetAndLength() {
        String fullString = "prefix{\"name\":\"Jane\",\"age\":25}suffix";
        byte[] buffer = fullString.getBytes();
        int offset = 6;
        int length = 24;

        N.println(new String(buffer, offset, length));
        TestPerson result = FastJson.fromJson(buffer, offset, length, TestPerson.class);

        assertNotNull(result);
        assertEquals("Jane", result.getName());
        assertEquals(25, result.getAge());
    }

    @Test
    public void testFromJsonByteArrayWithOffsetAndLengthNull() {
        byte[] buffer = "prefixnullsuffix".getBytes();
        int offset = 6;
        int length = 4;

        TestPerson result = FastJson.fromJson(buffer, offset, length, TestPerson.class);

        assertNull(result);
    }

    @Test
    public void testFromJsonString() {
        String json = "{\"name\":\"John\",\"age\":30,\"email\":\"john@example.com\"}";

        TestPerson result = FastJson.fromJson(json, TestPerson.class);

        assertNotNull(result);
        assertEquals("John", result.getName());
        assertEquals(30, result.getAge());
        assertEquals("john@example.com", result.getEmail());
    }

    @Test
    public void testFromJsonStringNull() {
        String json = "null";

        TestPerson result = FastJson.fromJson(json, TestPerson.class);

        assertNull(result);
    }

    @Test
    public void testFromJsonStringPrimitive() {
        String json = "42";

        Integer result = FastJson.fromJson(json, Integer.class);

        assertNotNull(result);
        assertEquals(42, result.intValue());
    }

    @Test
    public void testFromJsonStringList() {
        String json = "[\"a\",\"b\",\"c\"]";

        List result = FastJson.fromJson(json, List.class);

        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("a", result.get(0));
    }

    @Test
    public void testFromJsonStringWithFeatures() {
        String json = "{\"name\":\"John\",\"age\":30}";

        TestPerson result = FastJson.fromJson(json, TestPerson.class, JSONReader.Feature.SupportSmartMatch);

        assertNotNull(result);
        assertEquals("John", result.getName());
        assertEquals(30, result.getAge());
    }

    @Test
    public void testFromJsonStringWithFeaturesNull() {
        String json = "null";

        TestPerson result = FastJson.fromJson(json, TestPerson.class, JSONReader.Feature.SupportSmartMatch);

        assertNull(result);
    }

    @Test
    public void testFromJsonStringWithContext() {
        String json = "{\"name\":\"John\",\"age\":30}";
        JSONReader.Context context = new JSONReader.Context();

        TestPerson result = FastJson.fromJson(json, TestPerson.class, context);

        assertNotNull(result);
        assertEquals("John", result.getName());
        assertEquals(30, result.getAge());
    }

    @Test
    public void testFromJsonStringWithType() {
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
    public void testFromJsonStringWithTypeNull() {
        String json = "null";
        Type listType = new TypeReference<List<TestPerson>>() {
        }.getType();

        List<TestPerson> result = FastJson.fromJson(json, listType);

        assertNull(result);
    }

    @Test
    public void testFromJsonStringWithTypeAndFeatures() {
        String json = "[{\"name\":\"John\",\"age\":30}]";
        Type listType = new TypeReference<List<TestPerson>>() {
        }.getType();

        List<TestPerson> result = FastJson.fromJson(json, listType, JSONReader.Feature.SupportSmartMatch);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("John", result.get(0).getName());
    }

    @Test
    public void testFromJsonStringWithTypeAndContext() {
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
    public void testFromJsonStringWithTypeReference() {
        String json = "[{\"name\":\"John\",\"age\":30},{\"name\":\"Jane\",\"age\":25}]";

        List<TestPerson> result = FastJson.fromJson(json, new TypeReference<List<TestPerson>>() {
        });

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("John", result.get(0).getName());
        assertEquals("Jane", result.get(1).getName());
    }

    @Test
    public void testFromJsonStringWithTypeReferenceNull() {
        String json = "null";

        List<TestPerson> result = FastJson.fromJson(json, new TypeReference<List<TestPerson>>() {
        });

        assertNull(result);
    }

    @Test
    public void testFromJsonStringWithTypeReferenceMap() {
        String json = "{\"key1\":\"value1\",\"key2\":\"value2\"}";

        Map<String, String> result = FastJson.fromJson(json, new TypeReference<Map<String, String>>() {
        });

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("value1", result.get("key1"));
        assertEquals("value2", result.get("key2"));
    }

    @Test
    public void testFromJsonStringWithTypeReferenceAndFeatures() {
        String json = "[{\"name\":\"John\",\"age\":30}]";

        List<TestPerson> result = FastJson.fromJson(json, new TypeReference<List<TestPerson>>() {
        }, JSONReader.Feature.SupportSmartMatch);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("John", result.get(0).getName());
    }

    @Test
    public void testFromJsonStringWithTypeReferenceAndContext() {
        String json = "[{\"name\":\"John\",\"age\":30}]";
        JSONReader.Context context = new JSONReader.Context();

        List<TestPerson> result = FastJson.fromJson(json, new TypeReference<List<TestPerson>>() {
        }, context);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("John", result.get(0).getName());
    }

    @Test
    public void testFromJsonReader() {
        String json = "{\"name\":\"John\",\"age\":30,\"email\":\"john@example.com\"}";
        Reader reader = new StringReader(json);

        TestPerson result = FastJson.fromJson(reader, TestPerson.class);

        assertNotNull(result);
        assertEquals("John", result.getName());
        assertEquals(30, result.getAge());
        assertEquals("john@example.com", result.getEmail());
    }

    @Test
    public void testFromJsonReaderNull() {
        String json = "null";
        Reader reader = new StringReader(json);

        TestPerson result = FastJson.fromJson(reader, TestPerson.class);

        assertNull(result);
    }

    @Test
    public void testFromJsonReaderFromFile() throws Exception {
        File jsonFile = tempDir.resolve("person.json").toFile();
        Files.writeString(jsonFile.toPath(), "{\"name\":\"FileReader\",\"age\":35}");

        try (FileReader reader = new FileReader(jsonFile)) {
            TestPerson result = FastJson.fromJson(reader, TestPerson.class);

            assertNotNull(result);
            assertEquals("FileReader", result.getName());
            assertEquals(35, result.getAge());
        }
    }

    @Test
    public void testFromJsonReaderWithFeatures() {
        String json = "{\"name\":\"John\",\"age\":30}";
        Reader reader = new StringReader(json);

        TestPerson result = FastJson.fromJson(reader, TestPerson.class, JSONReader.Feature.SupportSmartMatch);

        assertNotNull(result);
        assertEquals("John", result.getName());
        assertEquals(30, result.getAge());
    }

    @Test
    public void testFromJsonReaderWithTypeAndContext() {
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
    public void testRoundTripSerialization() {
        String json = FastJson.toJson(testPerson);

        TestPerson result = FastJson.fromJson(json, TestPerson.class);

        assertEquals(testPerson, result);
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
    public void testFileRoundTrip() throws Exception {
        File jsonFile = tempDir.resolve("roundtrip.json").toFile();

        FastJson.toJson(testPerson, jsonFile);

        try (FileReader reader = new FileReader(jsonFile)) {
            TestPerson result = FastJson.fromJson(reader, TestPerson.class);
            assertEquals(testPerson, result);
        }
    }
}
