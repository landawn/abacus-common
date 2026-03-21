package com.landawn.abacus.util;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.landawn.abacus.TestBase;

import lombok.Data;

public class XmlMappersTest extends TestBase {

    @TempDir
    File tempDir;

    @Data
    public static class Person {
        private String name = "";
        private int age = 0;
        private String city = "";

        public Person() {
        }

        public Person(String name, int age) {
            this.name = name;
            this.age = age;
        }

        public Person(String name, int age, String city) {
            this.name = name;
            this.age = age;
            this.city = city;
        }
    }

    @Test
    public void test_toXml_object() {
        Person person = new Person("John", 30);
        String xml = XmlMappers.toXml(person);
        assertNotNull(xml);
        assertTrue(xml.contains("John"));
        assertTrue(xml.contains("30"));
    }

    @Test
    public void test_toXml_object_prettyFormat() {
        Person person = new Person("Jane", 25);
        String xml = XmlMappers.toXml(person, true);
        assertNotNull(xml);
        assertTrue(xml.contains("Jane"));
        assertTrue(xml.contains("25"));

        String xmlNonPretty = XmlMappers.toXml(person, false);
        assertNotNull(xmlNonPretty);
        assertTrue(xmlNonPretty.contains("Jane"));
    }

    @Test
    public void test_toXml_object_serializationFeatures() {
        Person person = new Person("Bob", 40);
        String xml = XmlMappers.toXml(person, SerializationFeature.INDENT_OUTPUT);
        assertNotNull(xml);
        assertTrue(xml.contains("Bob"));
        assertTrue(xml.contains("40"));
    }

    @Test
    public void test_toXml_object_serializationConfig() {
        Person person = new Person("Alice", 35);
        SerializationConfig config = XmlMappers.createSerializationConfig().with(SerializationFeature.INDENT_OUTPUT);
        String xml = XmlMappers.toXml(person, config);
        assertNotNull(xml);
        assertTrue(xml.contains("Alice"));
        assertTrue(xml.contains("35"));
    }

    @Test
    public void test_toXml_null_config() {
        Person person = new Person("Test", 25);
        String xml = XmlMappers.toXml(person, (SerializationConfig) null);
        assertNotNull(xml);
        assertTrue(xml.contains("Test"));
    }

    @Test
    public void test_complex_nested_object() {
        Map<String, List<Person>> data = new HashMap<>();
        List<Person> persons = new ArrayList<>();
        persons.add(new Person("Alice", 30, "NYC"));
        persons.add(new Person("Bob", 25, "LA"));
        data.put("people", persons);

        String xml = XmlMappers.toXml(data);
        assertNotNull(xml);
        assertTrue(xml.contains("Alice"));
        assertTrue(xml.contains("Bob"));
    }

    @Test
    public void testToXmlWithPrettyFormat() {
        Person person = new Person("John", 30);

        String prettyXml = XmlMappers.toXml(person, true);
        String compactXml = XmlMappers.toXml(person, false);

        Assertions.assertNotNull(prettyXml);
        Assertions.assertNotNull(compactXml);
        Assertions.assertTrue(prettyXml.contains("\n"));
        Assertions.assertFalse(compactXml.contains("\n"));
    }

    @Test
    public void testToXmlWithSerializationFeatures() {
        Person person = new Person("John", 0, null);

        String xml = XmlMappers.toXml(person, SerializationFeature.WRITE_NULL_MAP_VALUES, SerializationFeature.INDENT_OUTPUT);

        Assertions.assertNotNull(xml);
        Assertions.assertTrue(xml.contains("John"));
    }

    @Test
    public void testToXmlWithSerializationConfig() {
        Person person = new Person("John", 30);
        SerializationConfig config = XmlMappers.createSerializationConfig().with(SerializationFeature.WRAP_ROOT_VALUE).with(SerializationFeature.INDENT_OUTPUT);

        String xml = XmlMappers.toXml(person, config);

        Assertions.assertNotNull(xml);
        Assertions.assertTrue(xml.contains("John"));
    }

    @Test
    public void test_toXml_object_file() throws IOException {
        Person person = new Person("Charlie", 28);
        File file = new File(tempDir, "test_toXml.xml");
        XmlMappers.toXml(person, file);
        assertTrue(file.exists());
        assertTrue(file.length() > 0);
    }

    @Test
    public void test_toXml_object_file_config() throws IOException {
        Person person = new Person("David", 33);
        File file = new File(tempDir, "test_toXml_config.xml");
        SerializationConfig config = XmlMappers.createSerializationConfig();
        XmlMappers.toXml(person, file, config);
        assertTrue(file.exists());
        assertTrue(file.length() > 0);
    }

    @Test
    public void test_toXml_object_outputStream() throws IOException {
        Person person = new Person("Eve", 27);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        XmlMappers.toXml(person, baos);
        assertTrue(baos.size() > 0);
        String xml = baos.toString();
        assertTrue(xml.contains("Eve"));
    }

    @Test
    public void test_toXml_object_outputStream_config() throws IOException {
        Person person = new Person("Frank", 45);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        SerializationConfig config = XmlMappers.createSerializationConfig();
        XmlMappers.toXml(person, baos, config);
        assertTrue(baos.size() > 0);
    }

    @Test
    public void test_toXml_object_writer() throws IOException {
        Person person = new Person("Grace", 29);
        StringWriter writer = new StringWriter();
        XmlMappers.toXml(person, writer);
        String xml = writer.toString();
        assertTrue(xml.contains("Grace"));
        assertTrue(xml.contains("29"));
    }

    @Test
    public void test_toXml_object_writer_config() throws IOException {
        Person person = new Person("Henry", 38);
        StringWriter writer = new StringWriter();
        SerializationConfig config = XmlMappers.createSerializationConfig();
        XmlMappers.toXml(person, writer, config);
        String xml = writer.toString();
        assertTrue(xml.contains("Henry"));
    }

    @Test
    public void test_toXml_object_dataOutput() throws IOException {
        Person person = new Person("Ivy", 26);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutput dos = new DataOutputStream(baos);
        XmlMappers.toXml(person, dos);
        assertTrue(baos.size() > 0);
    }

    @Test
    public void test_toXml_object_dataOutput_config() throws IOException {
        Person person = new Person("Jack", 42);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutput dos = new DataOutputStream(baos);
        SerializationConfig config = XmlMappers.createSerializationConfig();
        XmlMappers.toXml(person, dos, config);
        assertTrue(baos.size() > 0);
    }

    @Test
    public void testToXmlToFile() throws Exception {
        Person person = new Person("John", 30);
        File tempFile = File.createTempFile("test", ".xml");
        tempFile.deleteOnExit();

        XmlMappers.toXml(person, tempFile);

        Assertions.assertTrue(tempFile.exists());
        Assertions.assertTrue(tempFile.length() > 0);
    }

    @Test
    public void testToXmlToFileWithConfig() throws Exception {
        Person person = new Person("John", 30);
        File tempFile = File.createTempFile("test", ".xml");
        tempFile.deleteOnExit();
        SerializationConfig config = XmlMappers.createSerializationConfig().with(SerializationFeature.INDENT_OUTPUT);

        XmlMappers.toXml(person, tempFile, config);

        Assertions.assertTrue(tempFile.exists());
        Assertions.assertTrue(tempFile.length() > 0);
    }

    @Test
    public void testToXmlToOutputStream() throws Exception {
        Person person = new Person("John", 30);
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        XmlMappers.toXml(person, os);

        String xml = os.toString();
        Assertions.assertTrue(xml.contains("John"));
        Assertions.assertTrue(xml.contains("30"));
    }

    @Test
    public void testToXmlToOutputStreamWithConfig() throws Exception {
        Person person = new Person("John", 30);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        SerializationConfig config = XmlMappers.createSerializationConfig().with(SerializationFeature.INDENT_OUTPUT);

        XmlMappers.toXml(person, os, config);

        String xml = os.toString();
        Assertions.assertTrue(xml.contains("John"));
    }

    @Test
    public void testToXmlToWriter() throws Exception {
        Person person = new Person("John", 30);
        StringWriter writer = new StringWriter();

        XmlMappers.toXml(person, writer);

        String xml = writer.toString();
        Assertions.assertTrue(xml.contains("John"));
        Assertions.assertTrue(xml.contains("30"));
    }

    @Test
    public void testToXmlToWriterWithConfig() throws Exception {
        Person person = new Person("John", 30);
        StringWriter writer = new StringWriter();
        SerializationConfig config = XmlMappers.createSerializationConfig().with(SerializationFeature.INDENT_OUTPUT);

        XmlMappers.toXml(person, writer, config);

        String xml = writer.toString();
        Assertions.assertTrue(xml.contains("John"));
    }

    @Test
    public void testToXmlToDataOutput() throws Exception {
        Person person = new Person("John", 30);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        XmlMappers.toXml(person, (DataOutput) dos);

        String xml = baos.toString();
        Assertions.assertTrue(xml.contains("John"));
    }

    @Test
    public void testToXmlToDataOutputWithConfig() throws Exception {
        Person person = new Person("John", 30);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        SerializationConfig config = XmlMappers.createSerializationConfig().with(SerializationFeature.INDENT_OUTPUT);

        XmlMappers.toXml(person, (DataOutput) dos, config);

        String xml = baos.toString();
        Assertions.assertTrue(xml.contains("John"));
    }

    @Test
    public void test_fromXml_bytes_class() {
        Person person = new Person("Kate", 31);
        String xml = XmlMappers.toXml(person);
        byte[] bytes = xml.getBytes();
        Person result = XmlMappers.fromXml(bytes, Person.class);
        assertEquals(person, result);
    }

    @Test
    public void test_fromXml_bytes_offset_len_class() {
        Person person = new Person("Leo", 34);
        String xml = XmlMappers.toXml(person);
        byte[] bytes = xml.getBytes();
        Person result = XmlMappers.fromXml(bytes, 0, bytes.length, Person.class);
        assertEquals(person, result);
    }

    @Test
    public void test_fromXml_string_class() {
        Person person = new Person("Mary", 29);
        String xml = XmlMappers.toXml(person);
        Person result = XmlMappers.fromXml(xml, Person.class);
        assertEquals(person, result);
    }

    @Test
    public void test_fromXml_string_class_deserializationFeatures() {
        Person person = new Person("Nancy", 36);
        String xml = XmlMappers.toXml(person);
        Person result = XmlMappers.fromXml(xml, Person.class, DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        assertEquals(person, result);
    }

    @Test
    public void test_fromXml_bytes_typeReference() {
        List<String> list = CommonUtil.toList("apple", "banana", "cherry");
        String xml = XmlMappers.toXml(list);
        byte[] bytes = xml.getBytes();
        List<String> result = XmlMappers.fromXml(bytes, new TypeReference<List<String>>() {
        });
        assertEquals(list.size(), result.size());
    }

    @Test
    public void test_fromXml_bytes_offset_len_typeReference() {
        List<String> list = CommonUtil.toList("dog", "cat", "bird");
        String xml = XmlMappers.toXml(list);
        byte[] bytes = xml.getBytes();
        List<String> result = XmlMappers.fromXml(bytes, 0, bytes.length, new TypeReference<List<String>>() {
        });
        assertEquals(list.size(), result.size());
    }

    @Test
    public void test_fromXml_string_typeReference_config() {
        List<Integer> list = CommonUtil.toList(10, 20, 30);
        String xml = XmlMappers.toXml(list);
        DeserializationConfig config = XmlMappers.createDeserializationConfig();
        List<Integer> result = XmlMappers.fromXml(xml, new TypeReference<List<Integer>>() {
        }, config);
        assertEquals(list.size(), result.size());
    }

    @Test
    public void test_fromXml_string_class_config() {
        Person person = new Person("Oscar", 41);
        String xml = XmlMappers.toXml(person);
        DeserializationConfig config = XmlMappers.createDeserializationConfig().with(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);
        Person result = XmlMappers.fromXml(xml, Person.class, config);
        assertEquals(person, result);
    }

    @Test
    public void test_fromXml_string_typeReference() {
        Map<String, Integer> map = new HashMap<>();
        map.put("one", 1);
        map.put("two", 2);
        String xml = XmlMappers.toXml(map);
        Map<String, Integer> result = XmlMappers.fromXml(xml, new TypeReference<Map<String, Integer>>() {
        });
        assertNotNull(result);
    }

    @Test
    public void test_fromXml_string_typeReference_features() {
        List<String> list = CommonUtil.toList("red", "green", "blue");
        String xml = XmlMappers.toXml(list);
        List<String> result = XmlMappers.fromXml(xml, new TypeReference<List<String>>() {
        }, DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
        assertEquals(list.size(), result.size());
    }

    @Test
    public void test_fromXml_null_config() {
        Person person = new Person("Test", 25);
        String xml = XmlMappers.toXml(person);
        Person result = XmlMappers.fromXml(xml, Person.class, (DeserializationConfig) null);
        assertEquals(person, result);
    }

    @Test
    public void testFromXmlByteArray() {
        byte[] xmlBytes = "<Person><name>John</name><age>30</age></Person>".getBytes();

        Person person = XmlMappers.fromXml(xmlBytes, Person.class);

        Assertions.assertNotNull(person);
        Assertions.assertEquals("John", person.getName());
        Assertions.assertEquals(30, person.getAge());
    }

    @Test
    public void testFromXmlByteArrayPartial() {
        byte[] xmlBytes = "xxx<Person><name>John</name><age>30</age></Person>yyy".getBytes();

        Person person = XmlMappers.fromXml(xmlBytes, 3, xmlBytes.length - 6, Person.class);

        Assertions.assertNotNull(person);
        Assertions.assertEquals("John", person.getName());
        Assertions.assertEquals(30, person.getAge());
    }

    @Test
    public void testFromXmlString() {
        String xml = "<Person><name>John</name><age>30</age></Person>";

        Person person = XmlMappers.fromXml(xml, Person.class);

        Assertions.assertNotNull(person);
        Assertions.assertEquals("John", person.getName());
        Assertions.assertEquals(30, person.getAge());
    }

    @Test
    public void testFromXmlStringWithConfig() {
        String xml = "<Person><name>John</name><age>30</age></Person>";
        DeserializationConfig config = XmlMappers.createDeserializationConfig().without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        Person person = XmlMappers.fromXml(xml, Person.class, config);

        Assertions.assertNotNull(person);
        Assertions.assertEquals("John", person.getName());
    }

    @Test
    public void testFromXmlWithTypeReferenceByteArray() {
        String xml = "<ArrayList><item>a</item><item>b</item><item>c</item></ArrayList>";
        byte[] xmlBytes = xml.getBytes();

        List<String> list = XmlMappers.fromXml(xmlBytes, new TypeReference<List<String>>() {
        });

        Assertions.assertNotNull(list);
        Assertions.assertEquals(3, list.size());
        Assertions.assertEquals("a", list.get(0));
    }

    @Test
    public void testFromXmlWithTypeReferenceByteArrayPartial() {
        String xml = "xxx<ArrayList><item>a</item><item>b</item></ArrayList>yyy";
        byte[] xmlBytes = xml.getBytes();

        List<String> list = XmlMappers.fromXml(xmlBytes, 3, xmlBytes.length - 6, new TypeReference<List<String>>() {
        });

        Assertions.assertNotNull(list);
        Assertions.assertEquals(2, list.size());
    }

    @Test
    public void testFromXmlWithTypeReferenceString() {
        String xml = "<LinkedHashMap><key1>value1</key1><key2>value2</key2></LinkedHashMap>";

        Map<String, String> map = XmlMappers.fromXml(xml, new TypeReference<Map<String, String>>() {
        });

        Assertions.assertNotNull(map);
        Assertions.assertEquals("value1", map.get("key1"));
        Assertions.assertEquals("value2", map.get("key2"));
    }

    @Test
    public void testFromXmlWithTypeReferenceStringWithFeatures() {
        String xml = "<ArrayList><item>a</item><item>b</item></ArrayList>";

        List<String> list = XmlMappers.fromXml(xml, new TypeReference<List<String>>() {
        }, DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);

        Assertions.assertNotNull(list);
        Assertions.assertEquals(2, list.size());
    }

    @Test
    public void testFromXmlWithTypeReferenceStringWithConfig() {
        String xml = "<ArrayList><item>a</item><item>b</item></ArrayList>";
        DeserializationConfig config = XmlMappers.createDeserializationConfig();

        List<String> list = XmlMappers.fromXml(xml, new TypeReference<List<String>>() {
        }, config);

        Assertions.assertNotNull(list);
        Assertions.assertEquals(2, list.size());
    }

    @Test
    public void test_fromXml_file_class() throws IOException {
        Person person = new Person("Paul", 37);
        File file = new File(tempDir, "test_fromXml.xml");
        XmlMappers.toXml(person, file);
        Person result = XmlMappers.fromXml(file, Person.class);
        assertEquals(person, result);
    }

    @Test
    public void test_fromXml_file_class_config() throws IOException {
        Person person = new Person("Quinn", 39);
        File file = new File(tempDir, "test_fromXml_config.xml");
        XmlMappers.toXml(person, file);
        DeserializationConfig config = XmlMappers.createDeserializationConfig();
        Person result = XmlMappers.fromXml(file, Person.class, config);
        assertEquals(person, result);
    }

    @Test
    public void test_fromXml_inputStream_class() throws IOException {
        Person person = new Person("Rachel", 32);
        String xml = XmlMappers.toXml(person);
        ByteArrayInputStream bais = new ByteArrayInputStream(xml.getBytes());
        Person result = XmlMappers.fromXml(bais, Person.class);
        assertEquals(person, result);
    }

    @Test
    public void test_fromXml_inputStream_class_config() throws IOException {
        Person person = new Person("Sam", 43);
        String xml = XmlMappers.toXml(person);
        ByteArrayInputStream bais = new ByteArrayInputStream(xml.getBytes());
        DeserializationConfig config = XmlMappers.createDeserializationConfig();
        Person result = XmlMappers.fromXml(bais, Person.class, config);
        assertEquals(person, result);
    }

    @Test
    public void test_fromXml_reader_class() throws IOException {
        Person person = new Person("Tina", 28);
        String xml = XmlMappers.toXml(person);
        StringReader reader = new StringReader(xml);
        Person result = XmlMappers.fromXml(reader, Person.class);
        assertEquals(person, result);
    }

    @Test
    public void test_fromXml_reader_class_config() throws IOException {
        Person person = new Person("Uma", 35);
        String xml = XmlMappers.toXml(person);
        StringReader reader = new StringReader(xml);
        DeserializationConfig config = XmlMappers.createDeserializationConfig();
        Person result = XmlMappers.fromXml(reader, Person.class, config);
        assertEquals(person, result);
    }

    @Test
    public void test_fromXml_url_class() throws IOException {
        Person person = new Person("Victor", 44);
        File file = new File(tempDir, "test_url.xml");
        XmlMappers.toXml(person, file);
        URL url = file.toURI().toURL();
        Person result = XmlMappers.fromXml(url, Person.class);
        assertEquals(person, result);
    }

    @Test
    public void test_fromXml_url_class_config() throws IOException {
        Person person = new Person("Wendy", 30);
        File file = new File(tempDir, "test_url_config.xml");
        XmlMappers.toXml(person, file);
        URL url = file.toURI().toURL();
        DeserializationConfig config = XmlMappers.createDeserializationConfig();
        Person result = XmlMappers.fromXml(url, Person.class, config);
        assertEquals(person, result);
    }

    @Test
    public void test_fromXml_dataInput_class() throws IOException {
        Person person = new Person("Victor", 44);
        String xml = XmlMappers.toXml(person);
        ByteArrayInputStream bais = new ByteArrayInputStream(xml.getBytes());
        DataInput dis = new DataInputStream(bais);
        assertThrows(UnsupportedOperationException.class, () -> XmlMappers.fromXml(dis, Person.class));
    }

    @Test
    public void test_fromXml_dataInput_class_config() throws IOException {
        Person person = new Person("Wendy", 30);
        String xml = XmlMappers.toXml(person);
        ByteArrayInputStream bais = new ByteArrayInputStream(xml.getBytes());
        DataInput dis = new DataInputStream(bais);
        DeserializationConfig config = XmlMappers.createDeserializationConfig();
        assertThrows(UnsupportedOperationException.class, () -> XmlMappers.fromXml(dis, Person.class, config));
    }

    @Test
    public void test_fromXml_file_typeReference() throws IOException {
        List<String> list = CommonUtil.toList("alpha", "beta", "gamma");
        File file = new File(tempDir, "test_typeRef.xml");
        XmlMappers.toXml(list, file);
        List<String> result = XmlMappers.fromXml(file, new TypeReference<List<String>>() {
        });
        assertEquals(list.size(), result.size());
    }

    @Test
    public void test_fromXml_file_typeReference_config() throws IOException {
        List<String> list = CommonUtil.toList("x", "y", "z");
        File file = new File(tempDir, "test_typeRef_config.xml");
        XmlMappers.toXml(list, file);
        DeserializationConfig config = XmlMappers.createDeserializationConfig();
        List<String> result = XmlMappers.fromXml(file, new TypeReference<List<String>>() {
        }, config);
        assertEquals(list.size(), result.size());
    }

    @Test
    public void test_fromXml_inputStream_typeReference() throws IOException {
        List<String> list = CommonUtil.toList("sun", "moon", "stars");
        String xml = XmlMappers.toXml(list);
        ByteArrayInputStream bais = new ByteArrayInputStream(xml.getBytes());
        List<String> result = XmlMappers.fromXml(bais, new TypeReference<List<String>>() {
        });
        assertEquals(list.size(), result.size());
    }

    @Test
    public void test_fromXml_inputStream_typeReference_config() throws IOException {
        List<String> list = CommonUtil.toList("earth", "mars", "venus");
        String xml = XmlMappers.toXml(list);
        ByteArrayInputStream bais = new ByteArrayInputStream(xml.getBytes());
        DeserializationConfig config = XmlMappers.createDeserializationConfig();
        List<String> result = XmlMappers.fromXml(bais, new TypeReference<List<String>>() {
        }, config);
        assertEquals(list.size(), result.size());
    }

    @Test
    public void test_fromXml_reader_typeReference() throws IOException {
        List<String> list = CommonUtil.toList("north", "south", "east", "west");
        String xml = XmlMappers.toXml(list);
        StringReader reader = new StringReader(xml);
        List<String> result = XmlMappers.fromXml(reader, new TypeReference<List<String>>() {
        });
        assertEquals(list.size(), result.size());
    }

    @Test
    public void test_fromXml_reader_typeReference_config() throws IOException {
        List<String> list = CommonUtil.toList("spring", "summer", "fall", "winter");
        String xml = XmlMappers.toXml(list);
        StringReader reader = new StringReader(xml);
        DeserializationConfig config = XmlMappers.createDeserializationConfig();
        List<String> result = XmlMappers.fromXml(reader, new TypeReference<List<String>>() {
        }, config);
        assertEquals(list.size(), result.size());
    }

    @Test
    public void test_fromXml_url_typeReference() throws IOException {
        List<String> list = CommonUtil.toList("url", "test", "data");
        File file = new File(tempDir, "test_url_typeref.xml");
        XmlMappers.toXml(list, file);
        URL url = file.toURI().toURL();
        List<String> result = XmlMappers.fromXml(url, new TypeReference<List<String>>() {
        });
        assertEquals(list.size(), result.size());
    }

    @Test
    public void test_fromXml_url_typeReference_config() throws IOException {
        List<String> list = CommonUtil.toList("url", "config", "test");
        File file = new File(tempDir, "test_url_typeref_config.xml");
        XmlMappers.toXml(list, file);
        URL url = file.toURI().toURL();
        DeserializationConfig config = XmlMappers.createDeserializationConfig();
        List<String> result = XmlMappers.fromXml(url, new TypeReference<List<String>>() {
        }, config);
        assertEquals(list.size(), result.size());
    }

    @Test
    public void test_fromXml_dataInput_typeReference() throws IOException {
        List<String> list = CommonUtil.toList("monday", "tuesday", "wednesday");
        String xml = XmlMappers.toXml(list);
        ByteArrayInputStream bais = new ByteArrayInputStream(xml.getBytes());
        DataInput dis = new DataInputStream(bais);
        assertThrows(UnsupportedOperationException.class, () -> XmlMappers.fromXml(dis, new TypeReference<List<String>>() {
        }));
    }

    @Test
    public void test_fromXml_dataInput_typeReference_config() throws IOException {
        List<String> list = CommonUtil.toList("january", "february", "march");
        String xml = XmlMappers.toXml(list);
        ByteArrayInputStream bais = new ByteArrayInputStream(xml.getBytes());
        DataInput dis = new DataInputStream(bais);
        DeserializationConfig config = XmlMappers.createDeserializationConfig();
        assertThrows(UnsupportedOperationException.class, () -> XmlMappers.fromXml(dis, new TypeReference<List<String>>() {
        }, config));
    }

    @Test
    public void testFromXmlStringWithFeatures() {
        String xml = "<Person><name>John</name><unknownField>value</unknownField><age>30</age></Person>";

        DeserializationFeature failOnUnknownProperties = DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
        Assertions.assertThrows(RuntimeException.class, () -> {
            XmlMappers.fromXml(xml, Person.class, failOnUnknownProperties);
        });
    }

    @Test
    public void testFromXmlFile() throws Exception {
        File tempFile = File.createTempFile("test", ".xml");
        tempFile.deleteOnExit();
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write("<Person><name>John</name><age>30</age></Person>");
        }

        Person person = XmlMappers.fromXml(tempFile, Person.class);

        Assertions.assertNotNull(person);
        Assertions.assertEquals("John", person.getName());
    }

    @Test
    public void testFromXmlFileWithConfig() throws Exception {
        File tempFile = File.createTempFile("test", ".xml");
        tempFile.deleteOnExit();
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write("<Person><name>John</name><age>30</age></Person>");
        }
        DeserializationConfig config = XmlMappers.createDeserializationConfig();

        Person person = XmlMappers.fromXml(tempFile, Person.class, config);

        Assertions.assertNotNull(person);
        Assertions.assertEquals("John", person.getName());
    }

    @Test
    public void testFromXmlInputStream() throws Exception {
        String xml = "<Person><name>John</name><age>30</age></Person>";
        ByteArrayInputStream is = new ByteArrayInputStream(xml.getBytes());

        Person person = XmlMappers.fromXml(is, Person.class);

        Assertions.assertNotNull(person);
        Assertions.assertEquals("John", person.getName());
    }

    @Test
    public void testFromXmlInputStreamWithConfig() throws Exception {
        String xml = "<Person><name>John</name><age>30</age></Person>";
        ByteArrayInputStream is = new ByteArrayInputStream(xml.getBytes());
        DeserializationConfig config = XmlMappers.createDeserializationConfig();

        Person person = XmlMappers.fromXml(is, Person.class, config);

        Assertions.assertNotNull(person);
        Assertions.assertEquals("John", person.getName());
    }

    @Test
    public void testFromXmlReader() throws Exception {
        String xml = "<Person><name>John</name><age>30</age></Person>";
        StringReader reader = new StringReader(xml);

        Person person = XmlMappers.fromXml(reader, Person.class);

        Assertions.assertNotNull(person);
        Assertions.assertEquals("John", person.getName());
    }

    @Test
    public void testFromXmlReaderWithConfig() throws Exception {
        String xml = "<Person><name>John</name><age>30</age></Person>";
        StringReader reader = new StringReader(xml);
        DeserializationConfig config = XmlMappers.createDeserializationConfig();

        Person person = XmlMappers.fromXml(reader, Person.class, config);

        Assertions.assertNotNull(person);
        Assertions.assertEquals("John", person.getName());
    }

    @Test
    public void testFromXmlURL() throws Exception {
        File tempFile = File.createTempFile("test", ".xml");
        tempFile.deleteOnExit();
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write("<Person><name>John</name><age>30</age></Person>");
        }
        URL url = tempFile.toURI().toURL();

        Person person = XmlMappers.fromXml(url, Person.class);

        Assertions.assertNotNull(person);
        Assertions.assertEquals("John", person.getName());
    }

    @Test
    public void testFromXmlURLWithConfig() throws Exception {
        File tempFile = File.createTempFile("test", ".xml");
        tempFile.deleteOnExit();
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write("<Person><name>John</name><age>30</age></Person>");
        }
        URL url = tempFile.toURI().toURL();
        DeserializationConfig config = XmlMappers.createDeserializationConfig();

        Person person = XmlMappers.fromXml(url, Person.class, config);

        Assertions.assertNotNull(person);
        Assertions.assertEquals("John", person.getName());
    }

    @Test
    public void testFromXmlWithTypeReferenceFile() throws Exception {
        File tempFile = File.createTempFile("test", ".xml");
        tempFile.deleteOnExit();
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write("<ArrayList><item>a</item><item>b</item></ArrayList>");
        }

        List<String> list = XmlMappers.fromXml(tempFile, new TypeReference<List<String>>() {
        });

        Assertions.assertNotNull(list);
        Assertions.assertEquals(2, list.size());
    }

    @Test
    public void testFromXmlWithTypeReferenceFileWithConfig() throws Exception {
        File tempFile = File.createTempFile("test", ".xml");
        tempFile.deleteOnExit();
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write("<ArrayList><item>a</item><item>b</item></ArrayList>");
        }
        DeserializationConfig config = XmlMappers.createDeserializationConfig();

        List<String> list = XmlMappers.fromXml(tempFile, new TypeReference<List<String>>() {
        }, config);

        Assertions.assertNotNull(list);
        Assertions.assertEquals(2, list.size());
    }

    @Test
    public void testFromXmlWithTypeReferenceInputStream() throws Exception {
        String xml = "<ArrayList><item>a</item><item>b</item></ArrayList>";
        ByteArrayInputStream is = new ByteArrayInputStream(xml.getBytes());

        List<String> list = XmlMappers.fromXml(is, new TypeReference<List<String>>() {
        });

        Assertions.assertNotNull(list);
        Assertions.assertEquals(2, list.size());
    }

    @Test
    public void testFromXmlWithTypeReferenceInputStreamWithConfig() throws Exception {
        String xml = "<ArrayList><item>a</item><item>b</item></ArrayList>";
        ByteArrayInputStream is = new ByteArrayInputStream(xml.getBytes());
        DeserializationConfig config = XmlMappers.createDeserializationConfig();

        List<String> list = XmlMappers.fromXml(is, new TypeReference<List<String>>() {
        }, config);

        Assertions.assertNotNull(list);
        Assertions.assertEquals(2, list.size());
    }

    @Test
    public void testFromXmlWithTypeReferenceReader() throws Exception {
        String xml = "<ArrayList><item>a</item><item>b</item></ArrayList>";
        StringReader reader = new StringReader(xml);

        List<String> list = XmlMappers.fromXml(reader, new TypeReference<List<String>>() {
        });

        Assertions.assertNotNull(list);
        Assertions.assertEquals(2, list.size());
    }

    @Test
    public void testFromXmlWithTypeReferenceReaderWithConfig() throws Exception {
        String xml = "<ArrayList><item>a</item><item>b</item></ArrayList>";
        StringReader reader = new StringReader(xml);
        DeserializationConfig config = XmlMappers.createDeserializationConfig();

        List<String> list = XmlMappers.fromXml(reader, new TypeReference<List<String>>() {
        }, config);

        Assertions.assertNotNull(list);
        Assertions.assertEquals(2, list.size());
    }

    @Test
    public void testFromXmlWithTypeReferenceURL() throws Exception {
        File tempFile = File.createTempFile("test", ".xml");
        tempFile.deleteOnExit();
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write("<ArrayList><item>a</item><item>b</item></ArrayList>");
        }
        URL url = tempFile.toURI().toURL();

        List<String> list = XmlMappers.fromXml(url, new TypeReference<List<String>>() {
        });

        Assertions.assertNotNull(list);
        Assertions.assertEquals(2, list.size());
    }

    @Test
    public void testFromXmlWithTypeReferenceURLWithConfig() throws Exception {
        File tempFile = File.createTempFile("test", ".xml");
        tempFile.deleteOnExit();
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write("<ArrayList><item>a</item><item>b</item></ArrayList>");
        }
        URL url = tempFile.toURI().toURL();
        DeserializationConfig config = XmlMappers.createDeserializationConfig();

        List<String> list = XmlMappers.fromXml(url, new TypeReference<List<String>>() {
        }, config);

        Assertions.assertNotNull(list);
        Assertions.assertEquals(2, list.size());
    }

    @Test
    public void test_createSerializationConfig() {
        SerializationConfig config = XmlMappers.createSerializationConfig();
        assertNotNull(config);
    }

    @Test
    public void test_createDeserializationConfig() {
        DeserializationConfig config = XmlMappers.createDeserializationConfig();
        assertNotNull(config);
    }

    @Test
    public void test_One_fromXml_bytes_class() {
        XmlMapper mapper = new XmlMapper();
        XmlMappers.One wrapper = XmlMappers.wrap(mapper);
        Person person = new Person("Eve", 27);
        String xml = wrapper.toXml(person);
        byte[] bytes = xml.getBytes();
        Person result = wrapper.fromXml(bytes, Person.class);
        assertEquals(person, result);
    }

    @Test
    public void test_One_fromXml_bytes_offset_len_class() {
        XmlMapper mapper = new XmlMapper();
        XmlMappers.One wrapper = XmlMappers.wrap(mapper);
        Person person = new Person("Frank", 45);
        String xml = wrapper.toXml(person);
        byte[] bytes = xml.getBytes();
        Person result = wrapper.fromXml(bytes, 0, bytes.length, Person.class);
        assertEquals(person, result);
    }

    @Test
    public void test_One_fromXml_string_class() {
        XmlMapper mapper = new XmlMapper();
        XmlMappers.One wrapper = XmlMappers.wrap(mapper);
        Person person = new Person("Grace", 29);
        String xml = wrapper.toXml(person);
        Person result = wrapper.fromXml(xml, Person.class);
        assertEquals(person, result);
    }

    @Test
    public void test_One_fromXml_bytes_typeReference() {
        XmlMapper mapper = new XmlMapper();
        XmlMappers.One wrapper = XmlMappers.wrap(mapper);
        List<String> list = CommonUtil.toList("a", "b", "c");
        String xml = wrapper.toXml(list);
        byte[] bytes = xml.getBytes();
        List<String> result = wrapper.fromXml(bytes, new TypeReference<List<String>>() {
        });
        assertEquals(list.size(), result.size());
    }

    @Test
    public void test_One_fromXml_bytes_offset_len_typeReference() {
        XmlMapper mapper = new XmlMapper();
        XmlMappers.One wrapper = XmlMappers.wrap(mapper);
        List<String> list = CommonUtil.toList("d", "e", "f");
        String xml = wrapper.toXml(list);
        byte[] bytes = xml.getBytes();
        List<String> result = wrapper.fromXml(bytes, 0, bytes.length, new TypeReference<List<String>>() {
        });
        assertEquals(list.size(), result.size());
    }

    @Test
    public void test_One_fromXml_string_typeReference() {
        XmlMapper mapper = new XmlMapper();
        XmlMappers.One wrapper = XmlMappers.wrap(mapper);
        List<String> list = CommonUtil.toList("g", "h", "i");
        String xml = wrapper.toXml(list);
        List<String> result = wrapper.fromXml(xml, new TypeReference<List<String>>() {
        });
        assertEquals(list.size(), result.size());
    }

    @Test
    public void test_wrap() {
        XmlMapper mapper = new XmlMapper();
        XmlMappers.One wrapper = XmlMappers.wrap(mapper);
        assertNotNull(wrapper);
    }

    @Test
    public void test_One_toXml_object() {
        XmlMapper mapper = new XmlMapper();
        XmlMappers.One wrapper = XmlMappers.wrap(mapper);
        Person person = new Person("John", 30);
        String xml = wrapper.toXml(person);
        assertNotNull(xml);
        assertTrue(xml.contains("John"));
    }

    @Test
    public void test_One_toXml_object_prettyFormat() {
        XmlMapper mapper = new XmlMapper();
        XmlMappers.One wrapper = XmlMappers.wrap(mapper);
        Person person = new Person("Jane", 25);
        String xml = wrapper.toXml(person, true);
        assertNotNull(xml);
        assertTrue(xml.contains("Jane"));

        String xmlNonPretty = wrapper.toXml(person, false);
        assertNotNull(xmlNonPretty);
    }

    @Test
    public void testOneToXmlPretty() {
        XmlMapper mapper = new XmlMapper();
        XmlMappers.One wrapper = XmlMappers.wrap(mapper);
        Person person = new Person("John", 30);

        String prettyXml = wrapper.toXml(person, true);
        String compactXml = wrapper.toXml(person, false);

        Assertions.assertNotNull(prettyXml);
        Assertions.assertNotNull(compactXml);
        Assertions.assertTrue(prettyXml.contains("\n"));
        Assertions.assertFalse(compactXml.contains("\n"));
    }

    @Test
    public void testOneFromXmlByteArray() {
        XmlMapper mapper = new XmlMapper();
        XmlMappers.One wrapper = XmlMappers.wrap(mapper);
        byte[] xmlBytes = "<Person><name>John</name><age>30</age></Person>".getBytes();

        Person person = wrapper.fromXml(xmlBytes, Person.class);

        Assertions.assertNotNull(person);
        Assertions.assertEquals("John", person.getName());
    }

    @Test
    public void testOneFromXmlByteArrayPartial() {
        XmlMapper mapper = new XmlMapper();
        XmlMappers.One wrapper = XmlMappers.wrap(mapper);
        byte[] xmlBytes = "xxx<Person><name>John</name><age>30</age></Person>yyy".getBytes();

        Person person = wrapper.fromXml(xmlBytes, 3, xmlBytes.length - 6, Person.class);

        Assertions.assertNotNull(person);
        Assertions.assertEquals("John", person.getName());
    }

    @Test
    public void testOneFromXmlString() {
        XmlMapper mapper = new XmlMapper();
        XmlMappers.One wrapper = XmlMappers.wrap(mapper);
        String xml = "<Person><name>John</name><age>30</age></Person>";

        Person person = wrapper.fromXml(xml, Person.class);

        Assertions.assertNotNull(person);
        Assertions.assertEquals("John", person.getName());
    }

    @Test
    public void testOneFromXmlWithTypeReferenceByteArray() {
        XmlMapper mapper = new XmlMapper();
        XmlMappers.One wrapper = XmlMappers.wrap(mapper);
        String xml = "<ArrayList><item>a</item><item>b</item></ArrayList>";
        byte[] xmlBytes = xml.getBytes();

        List<String> list = wrapper.fromXml(xmlBytes, new TypeReference<List<String>>() {
        });

        Assertions.assertNotNull(list);
        Assertions.assertEquals(2, list.size());
    }

    @Test
    public void testOneFromXmlWithTypeReferenceByteArrayPartial() {
        XmlMapper mapper = new XmlMapper();
        XmlMappers.One wrapper = XmlMappers.wrap(mapper);
        String xml = "xxx<ArrayList><item>a</item><item>b</item></ArrayList>yyy";
        byte[] xmlBytes = xml.getBytes();

        List<String> list = wrapper.fromXml(xmlBytes, 3, xmlBytes.length - 6, new TypeReference<List<String>>() {
        });

        Assertions.assertNotNull(list);
        Assertions.assertEquals(2, list.size());
    }

    @Test
    public void testOneFromXmlWithTypeReferenceString() {
        XmlMapper mapper = new XmlMapper();
        XmlMappers.One wrapper = XmlMappers.wrap(mapper);
        String xml = "<ArrayList><item>a</item><item>b</item></ArrayList>";

        List<String> list = wrapper.fromXml(xml, new TypeReference<List<String>>() {
        });

        Assertions.assertNotNull(list);
        Assertions.assertEquals(2, list.size());
    }

    @Test
    public void testOneToXml() {
        XmlMapper mapper = new XmlMapper();
        XmlMappers.One wrapper = XmlMappers.wrap(mapper);
        Person person = new Person("Alice", 28);
        String xml = wrapper.toXml(person);
        Assertions.assertNotNull(xml);
        Assertions.assertTrue(xml.contains("Alice"));
        Assertions.assertTrue(xml.contains("28"));
    }

    @Test
    public void testOneToXml_PrettyFormat() {
        XmlMappers.One wrapper = XmlMappers.wrap(new XmlMapper());
        String xml = wrapper.toXml(new Person("Pretty", 31), true);

        Assertions.assertNotNull(xml);
        Assertions.assertTrue(xml.contains("Pretty"));
        Assertions.assertTrue(xml.contains("\n"));
    }

    @Test
    public void test_One_toXml_object_file() throws IOException {
        XmlMapper mapper = new XmlMapper();
        XmlMappers.One wrapper = XmlMappers.wrap(mapper);
        Person person = new Person("Bob", 40);
        File file = new File(tempDir, "test_One_toXml.xml");
        wrapper.toXml(person, file);
        assertTrue(file.exists());
        assertTrue(file.length() > 0);
    }

    @Test
    public void test_One_toXml_object_outputStream() throws IOException {
        XmlMapper mapper = new XmlMapper();
        XmlMappers.One wrapper = XmlMappers.wrap(mapper);
        Person person = new Person("Alice", 35);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        wrapper.toXml(person, baos);
        assertTrue(baos.size() > 0);
    }

    @Test
    public void test_One_toXml_object_writer() throws IOException {
        XmlMapper mapper = new XmlMapper();
        XmlMappers.One wrapper = XmlMappers.wrap(mapper);
        Person person = new Person("Charlie", 28);
        StringWriter writer = new StringWriter();
        wrapper.toXml(person, writer);
        String xml = writer.toString();
        assertTrue(xml.contains("Charlie"));
    }

    @Test
    public void test_One_toXml_object_dataOutput() throws IOException {
        XmlMapper mapper = new XmlMapper();
        XmlMappers.One wrapper = XmlMappers.wrap(mapper);
        Person person = new Person("David", 33);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutput dos = new DataOutputStream(baos);
        wrapper.toXml(person, dos);
        assertTrue(baos.size() > 0);
    }

    @Test
    public void test_One_fromXml_file_class() throws IOException {
        XmlMapper mapper = new XmlMapper();
        XmlMappers.One wrapper = XmlMappers.wrap(mapper);
        Person person = new Person("Henry", 38);
        File file = new File(tempDir, "test_One_fromXml.xml");
        wrapper.toXml(person, file);
        Person result = wrapper.fromXml(file, Person.class);
        assertEquals(person, result);
    }

    @Test
    public void test_One_fromXml_inputStream_class() throws IOException {
        XmlMapper mapper = new XmlMapper();
        XmlMappers.One wrapper = XmlMappers.wrap(mapper);
        Person person = new Person("Ivy", 26);
        String xml = wrapper.toXml(person);
        ByteArrayInputStream bais = new ByteArrayInputStream(xml.getBytes());
        Person result = wrapper.fromXml(bais, Person.class);
        assertEquals(person, result);
    }

    @Test
    public void test_One_fromXml_reader_class() throws IOException {
        XmlMapper mapper = new XmlMapper();
        XmlMappers.One wrapper = XmlMappers.wrap(mapper);
        Person person = new Person("Jack", 42);
        String xml = wrapper.toXml(person);
        StringReader reader = new StringReader(xml);
        Person result = wrapper.fromXml(reader, Person.class);
        assertEquals(person, result);
    }

    @Test
    public void test_One_fromXml_url_class() throws IOException {
        XmlMapper mapper = new XmlMapper();
        XmlMappers.One wrapper = XmlMappers.wrap(mapper);
        Person person = new Person("UrlTest", 50);
        File file = new File(tempDir, "test_One_url.xml");
        wrapper.toXml(person, file);
        URL url = file.toURI().toURL();
        Person result = wrapper.fromXml(url, Person.class);
        assertEquals(person, result);
    }

    @Test
    public void test_One_fromXml_dataInput_class() throws IOException {
        XmlMapper mapper = new XmlMapper();
        XmlMappers.One wrapper = XmlMappers.wrap(mapper);
        Person person = new Person("Kate", 31);
        String xml = wrapper.toXml(person);
        ByteArrayInputStream bais = new ByteArrayInputStream(xml.getBytes());
        DataInput dis = new DataInputStream(bais);
        assertThrows(UnsupportedOperationException.class, () -> wrapper.fromXml(dis, Person.class));
    }

    @Test
    public void test_One_fromXml_file_typeReference() throws IOException {
        XmlMapper mapper = new XmlMapper();
        XmlMappers.One wrapper = XmlMappers.wrap(mapper);
        List<String> list = CommonUtil.toList("j", "k", "l");
        File file = new File(tempDir, "test_One_typeRef.xml");
        wrapper.toXml(list, file);
        List<String> result = wrapper.fromXml(file, new TypeReference<List<String>>() {
        });
        assertEquals(list.size(), result.size());
    }

    @Test
    public void test_One_fromXml_inputStream_typeReference() throws IOException {
        XmlMapper mapper = new XmlMapper();
        XmlMappers.One wrapper = XmlMappers.wrap(mapper);
        List<String> list = CommonUtil.toList("m", "n", "o");
        String xml = wrapper.toXml(list);
        ByteArrayInputStream bais = new ByteArrayInputStream(xml.getBytes());
        List<String> result = wrapper.fromXml(bais, new TypeReference<List<String>>() {
        });
        assertEquals(list.size(), result.size());
    }

    @Test
    public void test_One_fromXml_reader_typeReference() throws IOException {
        XmlMapper mapper = new XmlMapper();
        XmlMappers.One wrapper = XmlMappers.wrap(mapper);
        List<String> list = CommonUtil.toList("p", "q", "r");
        String xml = wrapper.toXml(list);
        StringReader reader = new StringReader(xml);
        List<String> result = wrapper.fromXml(reader, new TypeReference<List<String>>() {
        });
        assertEquals(list.size(), result.size());
    }

    @Test
    public void test_One_fromXml_url_typeReference() throws IOException {
        XmlMapper mapper = new XmlMapper();
        XmlMappers.One wrapper = XmlMappers.wrap(mapper);
        List<String> list = CommonUtil.toList("one", "url", "test");
        File file = new File(tempDir, "test_One_url_typeref.xml");
        wrapper.toXml(list, file);
        URL url = file.toURI().toURL();
        List<String> result = wrapper.fromXml(url, new TypeReference<List<String>>() {
        });
        assertEquals(list.size(), result.size());
    }

    @Test
    public void test_One_fromXml_dataInput_typeReference() throws IOException {
        XmlMapper mapper = new XmlMapper();
        XmlMappers.One wrapper = XmlMappers.wrap(mapper);
        List<String> list = CommonUtil.toList("s", "t", "u");
        String xml = wrapper.toXml(list);
        ByteArrayInputStream bais = new ByteArrayInputStream(xml.getBytes());
        DataInput dis = new DataInputStream(bais);
        assertThrows(UnsupportedOperationException.class, () -> wrapper.fromXml(dis, new TypeReference<List<String>>() {
        }));
    }

    @Test
    public void testOneToXmlFile() throws Exception {
        XmlMapper mapper = new XmlMapper();
        XmlMappers.One wrapper = XmlMappers.wrap(mapper);
        Person person = new Person("John", 30);
        File tempFile = File.createTempFile("test", ".xml");
        tempFile.deleteOnExit();

        wrapper.toXml(person, tempFile);

        Assertions.assertTrue(tempFile.exists());
        Assertions.assertTrue(tempFile.length() > 0);
    }

    @Test
    public void testOneToXmlOutputStream() throws Exception {
        XmlMapper mapper = new XmlMapper();
        XmlMappers.One wrapper = XmlMappers.wrap(mapper);
        Person person = new Person("John", 30);
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        wrapper.toXml(person, os);

        String xml = os.toString();
        Assertions.assertTrue(xml.contains("John"));
    }

    @Test
    public void testOneToXmlWriter() throws Exception {
        XmlMapper mapper = new XmlMapper();
        XmlMappers.One wrapper = XmlMappers.wrap(mapper);
        Person person = new Person("John", 30);
        StringWriter writer = new StringWriter();

        wrapper.toXml(person, writer);

        String xml = writer.toString();
        Assertions.assertTrue(xml.contains("John"));
    }

    @Test
    public void testOneToXmlDataOutput() throws Exception {
        XmlMapper mapper = new XmlMapper();
        XmlMappers.One wrapper = XmlMappers.wrap(mapper);
        Person person = new Person("John", 30);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        wrapper.toXml(person, (DataOutput) dos);

        String xml = baos.toString();
        Assertions.assertTrue(xml.contains("John"));
    }

    @Test
    public void testOneFromXmlFile() throws Exception {
        XmlMapper mapper = new XmlMapper();
        XmlMappers.One wrapper = XmlMappers.wrap(mapper);
        File tempFile = File.createTempFile("test", ".xml");
        tempFile.deleteOnExit();
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write("<Person><name>John</name><age>30</age></Person>");
        }

        Person person = wrapper.fromXml(tempFile, Person.class);

        Assertions.assertNotNull(person);
        Assertions.assertEquals("John", person.getName());
    }

    @Test
    public void testOneFromXmlInputStream() throws Exception {
        XmlMapper mapper = new XmlMapper();
        XmlMappers.One wrapper = XmlMappers.wrap(mapper);
        String xml = "<Person><name>John</name><age>30</age></Person>";
        ByteArrayInputStream is = new ByteArrayInputStream(xml.getBytes());

        Person person = wrapper.fromXml(is, Person.class);

        Assertions.assertNotNull(person);
        Assertions.assertEquals("John", person.getName());
    }

    @Test
    public void testOneFromXmlReader() throws Exception {
        XmlMapper mapper = new XmlMapper();
        XmlMappers.One wrapper = XmlMappers.wrap(mapper);
        String xml = "<Person><name>John</name><age>30</age></Person>";
        StringReader reader = new StringReader(xml);

        Person person = wrapper.fromXml(reader, Person.class);

        Assertions.assertNotNull(person);
        Assertions.assertEquals("John", person.getName());
    }

    @Test
    public void testOneFromXmlURL() throws Exception {
        XmlMapper mapper = new XmlMapper();
        XmlMappers.One wrapper = XmlMappers.wrap(mapper);
        File tempFile = File.createTempFile("test", ".xml");
        tempFile.deleteOnExit();
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write("<Person><name>John</name><age>30</age></Person>");
        }
        URL url = tempFile.toURI().toURL();

        Person person = wrapper.fromXml(url, Person.class);

        Assertions.assertNotNull(person);
        Assertions.assertEquals("John", person.getName());
    }

    @Test
    public void testOneFromXmlWithTypeReferenceFile() throws Exception {
        XmlMapper mapper = new XmlMapper();
        XmlMappers.One wrapper = XmlMappers.wrap(mapper);
        File tempFile = File.createTempFile("test", ".xml");
        tempFile.deleteOnExit();
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write("<ArrayList><item>a</item><item>b</item></ArrayList>");
        }

        List<String> list = wrapper.fromXml(tempFile, new TypeReference<List<String>>() {
        });

        Assertions.assertNotNull(list);
        Assertions.assertEquals(2, list.size());
    }

    @Test
    public void testOneFromXmlWithTypeReferenceInputStream() throws Exception {
        XmlMapper mapper = new XmlMapper();
        XmlMappers.One wrapper = XmlMappers.wrap(mapper);
        String xml = "<ArrayList><item>a</item><item>b</item></ArrayList>";
        ByteArrayInputStream is = new ByteArrayInputStream(xml.getBytes());

        List<String> list = wrapper.fromXml(is, new TypeReference<List<String>>() {
        });

        Assertions.assertNotNull(list);
        Assertions.assertEquals(2, list.size());
    }

    @Test
    public void testOneFromXmlWithTypeReferenceReader() throws Exception {
        XmlMapper mapper = new XmlMapper();
        XmlMappers.One wrapper = XmlMappers.wrap(mapper);
        String xml = "<ArrayList><item>a</item><item>b</item></ArrayList>";
        StringReader reader = new StringReader(xml);

        List<String> list = wrapper.fromXml(reader, new TypeReference<List<String>>() {
        });

        Assertions.assertNotNull(list);
        Assertions.assertEquals(2, list.size());
    }

    @Test
    public void testOneFromXmlWithTypeReferenceURL() throws Exception {
        XmlMapper mapper = new XmlMapper();
        XmlMappers.One wrapper = XmlMappers.wrap(mapper);
        File tempFile = File.createTempFile("test", ".xml");
        tempFile.deleteOnExit();
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write("<ArrayList><item>a</item><item>b</item></ArrayList>");
        }
        URL url = tempFile.toURI().toURL();

        List<String> list = wrapper.fromXml(url, new TypeReference<List<String>>() {
        });

        Assertions.assertNotNull(list);
        Assertions.assertEquals(2, list.size());
    }

    @Test
    public void testOneFromXmlDataInput() throws Exception {
        XmlMapper mapper = new XmlMapper();
        XmlMappers.One wrapper = XmlMappers.wrap(mapper);
        Person person = new Person("Bob", 35);
        String xml = wrapper.toXml(person);
        ByteArrayInputStream bais = new ByteArrayInputStream(xml.getBytes());
        DataInput dis = new DataInputStream(bais);
        assertThrows(UnsupportedOperationException.class, () -> wrapper.fromXml(dis, Person.class));
    }

    @Test
    public void testOneFromXmlWithTypeReferenceDataInput() throws Exception {
        XmlMapper mapper = new XmlMapper();
        XmlMappers.One wrapper = XmlMappers.wrap(mapper);
        List<String> list = new ArrayList<>();
        list.add("x");
        list.add("y");
        String xml = wrapper.toXml(list);
        ByteArrayInputStream bais = new ByteArrayInputStream(xml.getBytes());
        DataInput dis = new DataInputStream(bais);
        assertThrows(UnsupportedOperationException.class, () -> wrapper.fromXml(dis, new TypeReference<List<String>>() {
        }));
    }

    @Test
    public void testOneToXml_DataOutput() throws Exception {
        XmlMappers.One wrapper = XmlMappers.wrap(new XmlMapper());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutput output = new DataOutputStream(baos);

        wrapper.toXml(new Person("DataOutput", 29), output);

        String xml = baos.toString();
        Assertions.assertTrue(xml.contains("DataOutput"));
    }

}
