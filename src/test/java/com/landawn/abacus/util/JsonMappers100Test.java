package com.landawn.abacus.util;

import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.StringReader;
import java.net.URL;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.landawn.abacus.TestBase;

@Tag("new-test")
public class JsonMappers100Test extends TestBase {

    @Test
    public void testToJson() {
        Person person = new Person("John", 30);

        String json = JsonMappers.toJson(person);

        Assertions.assertNotNull(json);
        Assertions.assertTrue(json.contains("\"name\":\"John\""));
        Assertions.assertTrue(json.contains("\"age\":30"));
    }

    @Test
    public void testToJsonWithPrettyFormat() {
        Person person = new Person("John", 30);

        String prettyJson = JsonMappers.toJson(person, true);
        String compactJson = JsonMappers.toJson(person, false);

        Assertions.assertNotNull(prettyJson);
        Assertions.assertNotNull(compactJson);
        Assertions.assertTrue(prettyJson.contains("\n"));
        Assertions.assertFalse(compactJson.contains("\n"));
    }

    @Test
    public void testToJsonWithSerializationFeatures() {
        Person person = new Person("John", null);

        String json = JsonMappers.toJson(person, SerializationFeature.WRITE_NULL_MAP_VALUES, SerializationFeature.INDENT_OUTPUT);

        Assertions.assertNotNull(json);
        Assertions.assertTrue(json.contains("John"));
    }

    @Test
    public void testToJsonWithSerializationConfig() {
        Person person = new Person("John", 30);
        SerializationConfig config = JsonMappers.createSerializationConfig().with(SerializationFeature.INDENT_OUTPUT);

        String json = JsonMappers.toJson(person, config);

        Assertions.assertNotNull(json);
        Assertions.assertTrue(json.contains("John"));
    }

    @Test
    public void testToJsonToFile() throws Exception {
        Person person = new Person("John", 30);
        File tempFile = File.createTempFile("test", ".json");
        tempFile.deleteOnExit();

        JsonMappers.toJson(person, tempFile);

        Assertions.assertTrue(tempFile.exists());
        Assertions.assertTrue(tempFile.length() > 0);
    }

    @Test
    public void testToJsonToFileWithConfig() throws Exception {
        Person person = new Person("John", 30);
        File tempFile = File.createTempFile("test", ".json");
        tempFile.deleteOnExit();
        SerializationConfig config = JsonMappers.createSerializationConfig().with(SerializationFeature.INDENT_OUTPUT);

        JsonMappers.toJson(person, tempFile, config);

        Assertions.assertTrue(tempFile.exists());
        Assertions.assertTrue(tempFile.length() > 0);
    }

    @Test
    public void testToJsonToOutputStream() throws Exception {
        Person person = new Person("John", 30);
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        JsonMappers.toJson(person, os);

        String json = os.toString();
        Assertions.assertTrue(json.contains("John"));
        Assertions.assertTrue(json.contains("30"));
    }

    @Test
    public void testToJsonToOutputStreamWithConfig() throws Exception {
        Person person = new Person("John", 30);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        SerializationConfig config = JsonMappers.createSerializationConfig().with(SerializationFeature.INDENT_OUTPUT);

        JsonMappers.toJson(person, os, config);

        String json = os.toString();
        Assertions.assertTrue(json.contains("John"));
    }

    @Test
    public void testToJsonToWriter() throws Exception {
        Person person = new Person("John", 30);
        StringWriter writer = new StringWriter();

        JsonMappers.toJson(person, writer);

        String json = writer.toString();
        Assertions.assertTrue(json.contains("John"));
        Assertions.assertTrue(json.contains("30"));
    }

    @Test
    public void testToJsonToWriterWithConfig() throws Exception {
        Person person = new Person("John", 30);
        StringWriter writer = new StringWriter();
        SerializationConfig config = JsonMappers.createSerializationConfig().with(SerializationFeature.INDENT_OUTPUT);

        JsonMappers.toJson(person, writer, config);

        String json = writer.toString();
        Assertions.assertTrue(json.contains("John"));
    }

    @Test
    public void testToJsonToDataOutput() throws Exception {
        Person person = new Person("John", 30);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        JsonMappers.toJson(person, (DataOutput) dos);

        String json = baos.toString();
        Assertions.assertTrue(json.contains("John"));
    }

    @Test
    public void testToJsonToDataOutputWithConfig() throws Exception {
        Person person = new Person("John", 30);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        SerializationConfig config = JsonMappers.createSerializationConfig().with(SerializationFeature.INDENT_OUTPUT);

        JsonMappers.toJson(person, (DataOutput) dos, config);

        String json = baos.toString();
        Assertions.assertTrue(json.contains("John"));
    }

    @Test
    public void testFromJsonByteArray() {
        byte[] jsonBytes = "{\"name\":\"John\",\"age\":30}".getBytes();

        Person person = JsonMappers.fromJson(jsonBytes, Person.class);

        Assertions.assertNotNull(person);
        Assertions.assertEquals("John", person.getName());
        Assertions.assertEquals(30, person.getAge());
    }

    @Test
    public void testFromJsonByteArrayPartial() {
        byte[] jsonBytes = "xxx{\"name\":\"John\",\"age\":30}yyy".getBytes();

        Person person = JsonMappers.fromJson(jsonBytes, 3, jsonBytes.length - 6, Person.class);

        Assertions.assertNotNull(person);
        Assertions.assertEquals("John", person.getName());
        Assertions.assertEquals(30, person.getAge());
    }

    @Test
    public void testFromJsonString() {
        String json = "{\"name\":\"John\",\"age\":30}";

        Person person = JsonMappers.fromJson(json, Person.class);

        Assertions.assertNotNull(person);
        Assertions.assertEquals("John", person.getName());
        Assertions.assertEquals(30, person.getAge());
    }

    @Test
    public void testFromJsonStringWithFeatures() {
        String json = "{\"name\":\"John\",\"age\":30}";

        Person person = JsonMappers.fromJson(json, Person.class, DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS,
                DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);

        Assertions.assertNotNull(person);
        Assertions.assertEquals("John", person.getName());
    }

    @Test
    public void testFromJsonStringWithConfig() {
        String json = "{\"name\":\"John\",\"age\":30}";
        DeserializationConfig config = JsonMappers.createDeserializationConfig().without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        Person person = JsonMappers.fromJson(json, Person.class, config);

        Assertions.assertNotNull(person);
        Assertions.assertEquals("John", person.getName());
    }

    @Test
    public void testFromJsonFile() throws Exception {
        File tempFile = File.createTempFile("test", ".json");
        tempFile.deleteOnExit();
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write("{\"name\":\"John\",\"age\":30}");
        }

        Person person = JsonMappers.fromJson(tempFile, Person.class);

        Assertions.assertNotNull(person);
        Assertions.assertEquals("John", person.getName());
    }

    @Test
    public void testFromJsonFileWithConfig() throws Exception {
        File tempFile = File.createTempFile("test", ".json");
        tempFile.deleteOnExit();
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write("{\"name\":\"John\",\"age\":30}");
        }
        DeserializationConfig config = JsonMappers.createDeserializationConfig();

        Person person = JsonMappers.fromJson(tempFile, Person.class, config);

        Assertions.assertNotNull(person);
        Assertions.assertEquals("John", person.getName());
    }

    @Test
    public void testFromJsonInputStream() throws Exception {
        String json = "{\"name\":\"John\",\"age\":30}";
        ByteArrayInputStream is = new ByteArrayInputStream(json.getBytes());

        Person person = JsonMappers.fromJson(is, Person.class);

        Assertions.assertNotNull(person);
        Assertions.assertEquals("John", person.getName());
    }

    @Test
    public void testFromJsonInputStreamWithConfig() throws Exception {
        String json = "{\"name\":\"John\",\"age\":30}";
        ByteArrayInputStream is = new ByteArrayInputStream(json.getBytes());
        DeserializationConfig config = JsonMappers.createDeserializationConfig();

        Person person = JsonMappers.fromJson(is, Person.class, config);

        Assertions.assertNotNull(person);
        Assertions.assertEquals("John", person.getName());
    }

    @Test
    public void testFromJsonReader() throws Exception {
        String json = "{\"name\":\"John\",\"age\":30}";
        StringReader reader = new StringReader(json);

        Person person = JsonMappers.fromJson(reader, Person.class);

        Assertions.assertNotNull(person);
        Assertions.assertEquals("John", person.getName());
    }

    @Test
    public void testFromJsonReaderWithConfig() throws Exception {
        String json = "{\"name\":\"John\",\"age\":30}";
        StringReader reader = new StringReader(json);
        DeserializationConfig config = JsonMappers.createDeserializationConfig();

        Person person = JsonMappers.fromJson(reader, Person.class, config);

        Assertions.assertNotNull(person);
        Assertions.assertEquals("John", person.getName());
    }

    @Test
    public void testFromJsonURL() throws Exception {
        File tempFile = File.createTempFile("test", ".json");
        tempFile.deleteOnExit();
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write("{\"name\":\"John\",\"age\":30}");
        }
        URL url = tempFile.toURI().toURL();

        Person person = JsonMappers.fromJson(url, Person.class);

        Assertions.assertNotNull(person);
        Assertions.assertEquals("John", person.getName());
    }

    @Test
    public void testFromJsonURLWithConfig() throws Exception {
        File tempFile = File.createTempFile("test", ".json");
        tempFile.deleteOnExit();
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write("{\"name\":\"John\",\"age\":30}");
        }
        URL url = tempFile.toURI().toURL();
        DeserializationConfig config = JsonMappers.createDeserializationConfig();

        Person person = JsonMappers.fromJson(url, Person.class, config);

        Assertions.assertNotNull(person);
        Assertions.assertEquals("John", person.getName());
    }

    @Test
    public void testFromJsonDataInput() throws Exception {
        String json = "{\"name\":\"John\",\"age\":30}";
        ByteArrayInputStream bais = new ByteArrayInputStream(json.getBytes());
        DataInputStream dis = new DataInputStream(bais);

        Person person = JsonMappers.fromJson((DataInput) dis, Person.class);

        Assertions.assertNotNull(person);
        Assertions.assertEquals("John", person.getName());
    }

    @Test
    public void testFromJsonDataInputWithConfig() throws Exception {
        String json = "{\"name\":\"John\",\"age\":30}";
        ByteArrayInputStream bais = new ByteArrayInputStream(json.getBytes());
        DataInputStream dis = new DataInputStream(bais);
        DeserializationConfig config = JsonMappers.createDeserializationConfig();

        Person person = JsonMappers.fromJson((DataInput) dis, Person.class, config);

        Assertions.assertNotNull(person);
        Assertions.assertEquals("John", person.getName());
    }

    @Test
    public void testFromJsonWithTypeReferenceByteArray() {
        String json = "[\"a\",\"b\",\"c\"]";
        byte[] jsonBytes = json.getBytes();

        List<String> list = JsonMappers.fromJson(jsonBytes, new TypeReference<List<String>>() {
        });

        Assertions.assertNotNull(list);
        Assertions.assertEquals(3, list.size());
        Assertions.assertEquals("a", list.get(0));
    }

    @Test
    public void testFromJsonWithTypeReferenceByteArrayPartial() {
        String json = "xxx[\"a\",\"b\"]yyy";
        byte[] jsonBytes = json.getBytes();

        List<String> list = JsonMappers.fromJson(jsonBytes, 3, jsonBytes.length - 6, new TypeReference<List<String>>() {
        });

        Assertions.assertNotNull(list);
        Assertions.assertEquals(2, list.size());
    }

    @Test
    public void testFromJsonWithTypeReferenceString() {
        String json = "{\"key1\":\"value1\",\"key2\":\"value2\"}";

        Map<String, String> map = JsonMappers.fromJson(json, new TypeReference<Map<String, String>>() {
        });

        Assertions.assertNotNull(map);
        Assertions.assertEquals("value1", map.get("key1"));
        Assertions.assertEquals("value2", map.get("key2"));
    }

    @Test
    public void testFromJsonWithTypeReferenceStringWithFeatures() {
        String json = "[\"a\",\"b\"]";

        List<String> list = JsonMappers.fromJson(json, new TypeReference<List<String>>() {
        }, DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);

        Assertions.assertNotNull(list);
        Assertions.assertEquals(2, list.size());
    }

    @Test
    public void testFromJsonWithTypeReferenceStringWithConfig() {
        String json = "[\"a\",\"b\"]";
        DeserializationConfig config = JsonMappers.createDeserializationConfig();

        List<String> list = JsonMappers.fromJson(json, new TypeReference<List<String>>() {
        }, config);

        Assertions.assertNotNull(list);
        Assertions.assertEquals(2, list.size());
    }

    @Test
    public void testFromJsonWithTypeReferenceFile() throws Exception {
        File tempFile = File.createTempFile("test", ".json");
        tempFile.deleteOnExit();
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write("[\"a\",\"b\"]");
        }

        List<String> list = JsonMappers.fromJson(tempFile, new TypeReference<List<String>>() {
        });

        Assertions.assertNotNull(list);
        Assertions.assertEquals(2, list.size());
    }

    @Test
    public void testFromJsonWithTypeReferenceFileWithConfig() throws Exception {
        File tempFile = File.createTempFile("test", ".json");
        tempFile.deleteOnExit();
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write("[\"a\",\"b\"]");
        }
        DeserializationConfig config = JsonMappers.createDeserializationConfig();

        List<String> list = JsonMappers.fromJson(tempFile, new TypeReference<List<String>>() {
        }, config);

        Assertions.assertNotNull(list);
        Assertions.assertEquals(2, list.size());
    }

    @Test
    public void testFromJsonWithTypeReferenceInputStream() throws Exception {
        String json = "[\"a\",\"b\"]";
        ByteArrayInputStream is = new ByteArrayInputStream(json.getBytes());

        List<String> list = JsonMappers.fromJson(is, new TypeReference<List<String>>() {
        });

        Assertions.assertNotNull(list);
        Assertions.assertEquals(2, list.size());
    }

    @Test
    public void testFromJsonWithTypeReferenceInputStreamWithConfig() throws Exception {
        String json = "[\"a\",\"b\"]";
        ByteArrayInputStream is = new ByteArrayInputStream(json.getBytes());
        DeserializationConfig config = JsonMappers.createDeserializationConfig();

        List<String> list = JsonMappers.fromJson(is, new TypeReference<List<String>>() {
        }, config);

        Assertions.assertNotNull(list);
        Assertions.assertEquals(2, list.size());
    }

    @Test
    public void testFromJsonWithTypeReferenceReader() throws Exception {
        String json = "[\"a\",\"b\"]";
        StringReader reader = new StringReader(json);

        List<String> list = JsonMappers.fromJson(reader, new TypeReference<List<String>>() {
        });

        Assertions.assertNotNull(list);
        Assertions.assertEquals(2, list.size());
    }

    @Test
    public void testFromJsonWithTypeReferenceReaderWithConfig() throws Exception {
        String json = "[\"a\",\"b\"]";
        StringReader reader = new StringReader(json);
        DeserializationConfig config = JsonMappers.createDeserializationConfig();

        List<String> list = JsonMappers.fromJson(reader, new TypeReference<List<String>>() {
        }, config);

        Assertions.assertNotNull(list);
        Assertions.assertEquals(2, list.size());
    }

    @Test
    public void testFromJsonWithTypeReferenceURL() throws Exception {
        File tempFile = File.createTempFile("test", ".json");
        tempFile.deleteOnExit();
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write("[\"a\",\"b\"]");
        }
        URL url = tempFile.toURI().toURL();

        List<String> list = JsonMappers.fromJson(url, new TypeReference<List<String>>() {
        });

        Assertions.assertNotNull(list);
        Assertions.assertEquals(2, list.size());
    }

    @Test
    public void testFromJsonWithTypeReferenceURLWithConfig() throws Exception {
        File tempFile = File.createTempFile("test", ".json");
        tempFile.deleteOnExit();
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write("[\"a\",\"b\"]");
        }
        URL url = tempFile.toURI().toURL();
        DeserializationConfig config = JsonMappers.createDeserializationConfig();

        List<String> list = JsonMappers.fromJson(url, new TypeReference<List<String>>() {
        }, config);

        Assertions.assertNotNull(list);
        Assertions.assertEquals(2, list.size());
    }

    @Test
    public void testFromJsonWithTypeReferenceDataInput() throws Exception {
        String json = "[\"a\",\"b\"]";
        ByteArrayInputStream bais = new ByteArrayInputStream(json.getBytes());
        DataInputStream dis = new DataInputStream(bais);

        List<String> list = JsonMappers.fromJson((DataInput) dis, new TypeReference<List<String>>() {
        });

        Assertions.assertNotNull(list);
        Assertions.assertEquals(2, list.size());
    }

    @Test
    public void testFromJsonWithTypeReferenceDataInputWithConfig() throws Exception {
        String json = "[\"a\",\"b\"]";
        ByteArrayInputStream bais = new ByteArrayInputStream(json.getBytes());
        DataInputStream dis = new DataInputStream(bais);
        DeserializationConfig config = JsonMappers.createDeserializationConfig();

        List<String> list = JsonMappers.fromJson((DataInput) dis, new TypeReference<List<String>>() {
        }, config);

        Assertions.assertNotNull(list);
        Assertions.assertEquals(2, list.size());
    }

    @Test
    public void testCreateSerializationConfig() {
        SerializationConfig config = JsonMappers.createSerializationConfig();

        Assertions.assertNotNull(config);
    }

    @Test
    public void testCreateDeserializationConfig() {
        DeserializationConfig config = JsonMappers.createDeserializationConfig();

        Assertions.assertNotNull(config);
    }

    @Test
    public void testWrap() {
        ObjectMapper mapper = new ObjectMapper();
        JsonMappers.One wrapper = JsonMappers.wrap(mapper);

        Assertions.assertNotNull(wrapper);
    }

    @Test
    public void testOneToJson() {
        ObjectMapper mapper = new ObjectMapper();
        JsonMappers.One wrapper = JsonMappers.wrap(mapper);
        Person person = new Person("John", 30);

        String json = wrapper.toJson(person);

        Assertions.assertNotNull(json);
        Assertions.assertTrue(json.contains("John"));
    }

    @Test
    public void testOneToJsonPretty() {
        ObjectMapper mapper = new ObjectMapper();
        JsonMappers.One wrapper = JsonMappers.wrap(mapper);
        Person person = new Person("John", 30);

        String prettyJson = wrapper.toJson(person, true);
        String compactJson = wrapper.toJson(person, false);

        Assertions.assertNotNull(prettyJson);
        Assertions.assertNotNull(compactJson);
        Assertions.assertTrue(prettyJson.contains("\n"));
        Assertions.assertFalse(compactJson.contains("\n"));
    }

    @Test
    public void testOneToJsonFile() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonMappers.One wrapper = JsonMappers.wrap(mapper);
        Person person = new Person("John", 30);
        File tempFile = File.createTempFile("test", ".json");
        tempFile.deleteOnExit();

        wrapper.toJson(person, tempFile);

        Assertions.assertTrue(tempFile.exists());
        Assertions.assertTrue(tempFile.length() > 0);
    }

    @Test
    public void testOneToJsonOutputStream() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonMappers.One wrapper = JsonMappers.wrap(mapper);
        Person person = new Person("John", 30);
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        wrapper.toJson(person, os);

        String json = os.toString();
        Assertions.assertTrue(json.contains("John"));
    }

    @Test
    public void testOneToJsonWriter() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonMappers.One wrapper = JsonMappers.wrap(mapper);
        Person person = new Person("John", 30);
        StringWriter writer = new StringWriter();

        wrapper.toJson(person, writer);

        String json = writer.toString();
        Assertions.assertTrue(json.contains("John"));
    }

    @Test
    public void testOneToJsonDataOutput() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonMappers.One wrapper = JsonMappers.wrap(mapper);
        Person person = new Person("John", 30);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        wrapper.toJson(person, (DataOutput) dos);

        String json = baos.toString();
        Assertions.assertTrue(json.contains("John"));
    }

    @Test
    public void testOneFromJsonByteArray() {
        ObjectMapper mapper = new ObjectMapper();
        JsonMappers.One wrapper = JsonMappers.wrap(mapper);
        byte[] jsonBytes = "{\"name\":\"John\",\"age\":30}".getBytes();

        Person person = wrapper.fromJson(jsonBytes, Person.class);

        Assertions.assertNotNull(person);
        Assertions.assertEquals("John", person.getName());
    }

    @Test
    public void testOneFromJsonByteArrayPartial() {
        ObjectMapper mapper = new ObjectMapper();
        JsonMappers.One wrapper = JsonMappers.wrap(mapper);
        byte[] jsonBytes = "xxx{\"name\":\"John\",\"age\":30}yyy".getBytes();

        Person person = wrapper.fromJson(jsonBytes, 3, jsonBytes.length - 6, Person.class);

        Assertions.assertNotNull(person);
        Assertions.assertEquals("John", person.getName());
    }

    @Test
    public void testOneFromJsonString() {
        ObjectMapper mapper = new ObjectMapper();
        JsonMappers.One wrapper = JsonMappers.wrap(mapper);
        String json = "{\"name\":\"John\",\"age\":30}";

        Person person = wrapper.fromJson(json, Person.class);

        Assertions.assertNotNull(person);
        Assertions.assertEquals("John", person.getName());
    }

    @Test
    public void testOneFromJsonFile() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonMappers.One wrapper = JsonMappers.wrap(mapper);
        File tempFile = File.createTempFile("test", ".json");
        tempFile.deleteOnExit();
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write("{\"name\":\"John\",\"age\":30}");
        }

        Person person = wrapper.fromJson(tempFile, Person.class);

        Assertions.assertNotNull(person);
        Assertions.assertEquals("John", person.getName());
    }

    @Test
    public void testOneFromJsonInputStream() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonMappers.One wrapper = JsonMappers.wrap(mapper);
        String json = "{\"name\":\"John\",\"age\":30}";
        ByteArrayInputStream is = new ByteArrayInputStream(json.getBytes());

        Person person = wrapper.fromJson(is, Person.class);

        Assertions.assertNotNull(person);
        Assertions.assertEquals("John", person.getName());
    }

    @Test
    public void testOneFromJsonReader() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonMappers.One wrapper = JsonMappers.wrap(mapper);
        String json = "{\"name\":\"John\",\"age\":30}";
        StringReader reader = new StringReader(json);

        Person person = wrapper.fromJson(reader, Person.class);

        Assertions.assertNotNull(person);
        Assertions.assertEquals("John", person.getName());
    }

    @Test
    public void testOneFromJsonURL() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonMappers.One wrapper = JsonMappers.wrap(mapper);
        File tempFile = File.createTempFile("test", ".json");
        tempFile.deleteOnExit();
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write("{\"name\":\"John\",\"age\":30}");
        }
        URL url = tempFile.toURI().toURL();

        Person person = wrapper.fromJson(url, Person.class);

        Assertions.assertNotNull(person);
        Assertions.assertEquals("John", person.getName());
    }

    @Test
    public void testOneFromJsonDataInput() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonMappers.One wrapper = JsonMappers.wrap(mapper);
        String json = "{\"name\":\"John\",\"age\":30}";
        ByteArrayInputStream bais = new ByteArrayInputStream(json.getBytes());
        DataInputStream dis = new DataInputStream(bais);

        Person person = wrapper.fromJson((DataInput) dis, Person.class);

        Assertions.assertNotNull(person);
        Assertions.assertEquals("John", person.getName());
    }

    @Test
    public void testOneFromJsonWithTypeReferenceByteArray() {
        ObjectMapper mapper = new ObjectMapper();
        JsonMappers.One wrapper = JsonMappers.wrap(mapper);
        String json = "[\"a\",\"b\"]";
        byte[] jsonBytes = json.getBytes();

        List<String> list = wrapper.fromJson(jsonBytes, new TypeReference<List<String>>() {
        });

        Assertions.assertNotNull(list);
        Assertions.assertEquals(2, list.size());
    }

    @Test
    public void testOneFromJsonWithTypeReferenceByteArrayPartial() {
        ObjectMapper mapper = new ObjectMapper();
        JsonMappers.One wrapper = JsonMappers.wrap(mapper);
        String json = "xxx[\"a\",\"b\"]yyy";
        byte[] jsonBytes = json.getBytes();

        List<String> list = wrapper.fromJson(jsonBytes, 3, jsonBytes.length - 6, new TypeReference<List<String>>() {
        });

        Assertions.assertNotNull(list);
        Assertions.assertEquals(2, list.size());
    }

    @Test
    public void testOneFromJsonWithTypeReferenceString() {
        ObjectMapper mapper = new ObjectMapper();
        JsonMappers.One wrapper = JsonMappers.wrap(mapper);
        String json = "[\"a\",\"b\"]";

        List<String> list = wrapper.fromJson(json, new TypeReference<List<String>>() {
        });

        Assertions.assertNotNull(list);
        Assertions.assertEquals(2, list.size());
    }

    @Test
    public void testOneFromJsonWithTypeReferenceFile() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonMappers.One wrapper = JsonMappers.wrap(mapper);
        File tempFile = File.createTempFile("test", ".json");
        tempFile.deleteOnExit();
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write("[\"a\",\"b\"]");
        }

        List<String> list = wrapper.fromJson(tempFile, new TypeReference<List<String>>() {
        });

        Assertions.assertNotNull(list);
        Assertions.assertEquals(2, list.size());
    }

    @Test
    public void testOneFromJsonWithTypeReferenceInputStream() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonMappers.One wrapper = JsonMappers.wrap(mapper);
        String json = "[\"a\",\"b\"]";
        ByteArrayInputStream is = new ByteArrayInputStream(json.getBytes());

        List<String> list = wrapper.fromJson(is, new TypeReference<List<String>>() {
        });

        Assertions.assertNotNull(list);
        Assertions.assertEquals(2, list.size());
    }

    @Test
    public void testOneFromJsonWithTypeReferenceReader() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonMappers.One wrapper = JsonMappers.wrap(mapper);
        String json = "[\"a\",\"b\"]";
        StringReader reader = new StringReader(json);

        List<String> list = wrapper.fromJson(reader, new TypeReference<List<String>>() {
        });

        Assertions.assertNotNull(list);
        Assertions.assertEquals(2, list.size());
    }

    @Test
    public void testOneFromJsonWithTypeReferenceURL() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonMappers.One wrapper = JsonMappers.wrap(mapper);
        File tempFile = File.createTempFile("test", ".json");
        tempFile.deleteOnExit();
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write("[\"a\",\"b\"]");
        }
        URL url = tempFile.toURI().toURL();

        List<String> list = wrapper.fromJson(url, new TypeReference<List<String>>() {
        });

        Assertions.assertNotNull(list);
        Assertions.assertEquals(2, list.size());
    }

    @Test
    public void testOneFromJsonWithTypeReferenceDataInput() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonMappers.One wrapper = JsonMappers.wrap(mapper);
        String json = "[\"a\",\"b\"]";
        ByteArrayInputStream bais = new ByteArrayInputStream(json.getBytes());
        DataInputStream dis = new DataInputStream(bais);

        List<String> list = wrapper.fromJson((DataInput) dis, new TypeReference<List<String>>() {
        });

        Assertions.assertNotNull(list);
        Assertions.assertEquals(2, list.size());
    }

    public static class Person {
        private String name;
        private Integer age;

        public Person() {
        }

        public Person(String name, Integer age) {
            this.name = name;
            this.age = age;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getAge() {
            return age;
        }

        public void setAge(Integer age) {
            this.age = age;
        }
    }
}
