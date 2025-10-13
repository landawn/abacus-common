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
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Tag;
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

@Tag("2025")
public class XmlMappers2025Test extends TestBase {

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
    public void test_fromXml_string_class_config() {
        Person person = new Person("Oscar", 41);
        String xml = XmlMappers.toXml(person);
        DeserializationConfig config = XmlMappers.createDeserializationConfig().with(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);
        Person result = XmlMappers.fromXml(xml, Person.class, config);
        assertEquals(person, result);
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
    public void test_fromXml_bytes_typeReference() {
        List<String> list = N.asList("apple", "banana", "cherry");
        String xml = XmlMappers.toXml(list);
        byte[] bytes = xml.getBytes();
        List<String> result = XmlMappers.fromXml(bytes, new TypeReference<List<String>>() {
        });
        assertEquals(list.size(), result.size());
    }

    @Test
    public void test_fromXml_bytes_offset_len_typeReference() {
        List<String> list = N.asList("dog", "cat", "bird");
        String xml = XmlMappers.toXml(list);
        byte[] bytes = xml.getBytes();
        List<String> result = XmlMappers.fromXml(bytes, 0, bytes.length, new TypeReference<List<String>>() {
        });
        assertEquals(list.size(), result.size());
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
        List<String> list = N.asList("red", "green", "blue");
        String xml = XmlMappers.toXml(list);
        List<String> result = XmlMappers.fromXml(xml, new TypeReference<List<String>>() {
        }, DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
        assertEquals(list.size(), result.size());
    }

    @Test
    public void test_fromXml_string_typeReference_config() {
        List<Integer> list = N.asList(10, 20, 30);
        String xml = XmlMappers.toXml(list);
        DeserializationConfig config = XmlMappers.createDeserializationConfig();
        List<Integer> result = XmlMappers.fromXml(xml, new TypeReference<List<Integer>>() {
        }, config);
        assertEquals(list.size(), result.size());
    }

    @Test
    public void test_fromXml_file_typeReference() throws IOException {
        List<String> list = N.asList("alpha", "beta", "gamma");
        File file = new File(tempDir, "test_typeRef.xml");
        XmlMappers.toXml(list, file);
        List<String> result = XmlMappers.fromXml(file, new TypeReference<List<String>>() {
        });
        assertEquals(list.size(), result.size());
    }

    @Test
    public void test_fromXml_file_typeReference_config() throws IOException {
        List<String> list = N.asList("x", "y", "z");
        File file = new File(tempDir, "test_typeRef_config.xml");
        XmlMappers.toXml(list, file);
        DeserializationConfig config = XmlMappers.createDeserializationConfig();
        List<String> result = XmlMappers.fromXml(file, new TypeReference<List<String>>() {
        }, config);
        assertEquals(list.size(), result.size());
    }

    @Test
    public void test_fromXml_inputStream_typeReference() throws IOException {
        List<String> list = N.asList("sun", "moon", "stars");
        String xml = XmlMappers.toXml(list);
        ByteArrayInputStream bais = new ByteArrayInputStream(xml.getBytes());
        List<String> result = XmlMappers.fromXml(bais, new TypeReference<List<String>>() {
        });
        assertEquals(list.size(), result.size());
    }

    @Test
    public void test_fromXml_inputStream_typeReference_config() throws IOException {
        List<String> list = N.asList("earth", "mars", "venus");
        String xml = XmlMappers.toXml(list);
        ByteArrayInputStream bais = new ByteArrayInputStream(xml.getBytes());
        DeserializationConfig config = XmlMappers.createDeserializationConfig();
        List<String> result = XmlMappers.fromXml(bais, new TypeReference<List<String>>() {
        }, config);
        assertEquals(list.size(), result.size());
    }

    @Test
    public void test_fromXml_reader_typeReference() throws IOException {
        List<String> list = N.asList("north", "south", "east", "west");
        String xml = XmlMappers.toXml(list);
        StringReader reader = new StringReader(xml);
        List<String> result = XmlMappers.fromXml(reader, new TypeReference<List<String>>() {
        });
        assertEquals(list.size(), result.size());
    }

    @Test
    public void test_fromXml_reader_typeReference_config() throws IOException {
        List<String> list = N.asList("spring", "summer", "fall", "winter");
        String xml = XmlMappers.toXml(list);
        StringReader reader = new StringReader(xml);
        DeserializationConfig config = XmlMappers.createDeserializationConfig();
        List<String> result = XmlMappers.fromXml(reader, new TypeReference<List<String>>() {
        }, config);
        assertEquals(list.size(), result.size());
    }

    @Test
    public void test_fromXml_url_typeReference() throws IOException {
        List<String> list = N.asList("url", "test", "data");
        File file = new File(tempDir, "test_url_typeref.xml");
        XmlMappers.toXml(list, file);
        URL url = file.toURI().toURL();
        List<String> result = XmlMappers.fromXml(url, new TypeReference<List<String>>() {
        });
        assertEquals(list.size(), result.size());
    }

    @Test
    public void test_fromXml_url_typeReference_config() throws IOException {
        List<String> list = N.asList("url", "config", "test");
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
        List<String> list = N.asList("monday", "tuesday", "wednesday");
        String xml = XmlMappers.toXml(list);
        ByteArrayInputStream bais = new ByteArrayInputStream(xml.getBytes());
        DataInput dis = new DataInputStream(bais);
        assertThrows(UnsupportedOperationException.class, () -> XmlMappers.fromXml(dis, new TypeReference<List<String>>() {
        }));
    }

    @Test
    public void test_fromXml_dataInput_typeReference_config() throws IOException {
        List<String> list = N.asList("january", "february", "march");
        String xml = XmlMappers.toXml(list);
        ByteArrayInputStream bais = new ByteArrayInputStream(xml.getBytes());
        DataInput dis = new DataInputStream(bais);
        DeserializationConfig config = XmlMappers.createDeserializationConfig();
        assertThrows(UnsupportedOperationException.class, () -> XmlMappers.fromXml(dis, new TypeReference<List<String>>() {
        }, config));
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
    public void test_One_fromXml_bytes_typeReference() {
        XmlMapper mapper = new XmlMapper();
        XmlMappers.One wrapper = XmlMappers.wrap(mapper);
        List<String> list = N.asList("a", "b", "c");
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
        List<String> list = N.asList("d", "e", "f");
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
        List<String> list = N.asList("g", "h", "i");
        String xml = wrapper.toXml(list);
        List<String> result = wrapper.fromXml(xml, new TypeReference<List<String>>() {
        });
        assertEquals(list.size(), result.size());
    }

    @Test
    public void test_One_fromXml_file_typeReference() throws IOException {
        XmlMapper mapper = new XmlMapper();
        XmlMappers.One wrapper = XmlMappers.wrap(mapper);
        List<String> list = N.asList("j", "k", "l");
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
        List<String> list = N.asList("m", "n", "o");
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
        List<String> list = N.asList("p", "q", "r");
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
        List<String> list = N.asList("one", "url", "test");
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
        List<String> list = N.asList("s", "t", "u");
        String xml = wrapper.toXml(list);
        ByteArrayInputStream bais = new ByteArrayInputStream(xml.getBytes());
        DataInput dis = new DataInputStream(bais);
        assertThrows(UnsupportedOperationException.class, () -> wrapper.fromXml(dis, new TypeReference<List<String>>() {
        }));
    }

    @Test
    public void test_toXml_null_config() {
        Person person = new Person("Test", 25);
        String xml = XmlMappers.toXml(person, (SerializationConfig) null);
        assertNotNull(xml);
        assertTrue(xml.contains("Test"));
    }

    @Test
    public void test_fromXml_null_config() {
        Person person = new Person("Test", 25);
        String xml = XmlMappers.toXml(person);
        Person result = XmlMappers.fromXml(xml, Person.class, (DeserializationConfig) null);
        assertEquals(person, result);
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
}
