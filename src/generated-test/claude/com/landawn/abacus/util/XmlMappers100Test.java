package com.landawn.abacus.util;

import java.io.ByteArrayInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.StringReader;
import java.net.URL;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.landawn.abacus.TestBase;

public class XmlMappers100Test extends TestBase {

    @Test
    public void testToXml() {
        Person person = new Person("John", 30);

        String xml = XmlMappers.toXml(person);

        Assertions.assertNotNull(xml);
        Assertions.assertTrue(xml.contains("John"));
        Assertions.assertTrue(xml.contains("30"));
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
        Person person = new Person("John", null);

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
    public void testFromXmlStringWithFeatures() {
        String xml = "<Person><name>John</name><unknownField>value</unknownField><age>30</age></Person>";

        DeserializationFeature failOnUnknownProperties = DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
        // Should throw exception due to unknown field
        Assertions.assertThrows(RuntimeException.class, () -> {
            XmlMappers.fromXml(xml, Person.class, failOnUnknownProperties);
        });
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

    //    @Test
    //    public void testFromXmlDataInput() throws Exception {
    //        String xml = "<Person><name>John</name><age>30</age></Person>";
    //        ByteArrayInputStream bais = new ByteArrayInputStream(xml.getBytes());
    //        DataInputStream dis = new DataInputStream(bais);
    //
    //        Person person = XmlMappers.fromXml((DataInput) dis, Person.class);
    //
    //        Assertions.assertNotNull(person);
    //        Assertions.assertEquals("John", person.getName());
    //    }

    //    @Test
    //    public void testFromXmlDataInputWithConfig() throws Exception {
    //        String xml = "<Person><name>John</name><age>30</age></Person>";
    //        ByteArrayInputStream bais = new ByteArrayInputStream(xml.getBytes());
    //        DataInputStream dis = new DataInputStream(bais);
    //        DeserializationConfig config = XmlMappers.createDeserializationConfig();
    //
    //        Person person = XmlMappers.fromXml((DataInput) dis, Person.class, config);
    //
    //        Assertions.assertNotNull(person);
    //        Assertions.assertEquals("John", person.getName());
    //    }

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

    //    @Test
    //    public void testFromXmlWithTypeReferenceDataInput() throws Exception {
    //        String xml = "<ArrayList><item>a</item><item>b</item></ArrayList>";
    //        ByteArrayInputStream bais = new ByteArrayInputStream(xml.getBytes());
    //        DataInputStream dis = new DataInputStream(bais);
    //
    //        List<String> list = XmlMappers.fromXml((DataInput) dis, new TypeReference<List<String>>() {
    //        });
    //
    //        Assertions.assertNotNull(list);
    //        Assertions.assertEquals(2, list.size());
    //    }
    //
    //    @Test
    //    public void testFromXmlWithTypeReferenceDataInputWithConfig() throws Exception {
    //        String xml = "<ArrayList><item>a</item><item>b</item></ArrayList>";
    //        ByteArrayInputStream bais = new ByteArrayInputStream(xml.getBytes());
    //        DataInputStream dis = new DataInputStream(bais);
    //        DeserializationConfig config = XmlMappers.createDeserializationConfig();
    //
    //        List<String> list = XmlMappers.fromXml((DataInput) dis, new TypeReference<List<String>>() {
    //        }, config);
    //
    //        Assertions.assertNotNull(list);
    //        Assertions.assertEquals(2, list.size());
    //    }

    @Test
    public void testCreateSerializationConfig() {
        SerializationConfig config = XmlMappers.createSerializationConfig();

        Assertions.assertNotNull(config);
    }

    @Test
    public void testCreateDeserializationConfig() {
        DeserializationConfig config = XmlMappers.createDeserializationConfig();

        Assertions.assertNotNull(config);
    }

    @Test
    public void testWrap() {
        XmlMapper mapper = new XmlMapper();
        XmlMappers.One wrapper = XmlMappers.wrap(mapper);

        Assertions.assertNotNull(wrapper);
    }

    @Test
    public void testOneToXml() {
        XmlMapper mapper = new XmlMapper();
        XmlMappers.One wrapper = XmlMappers.wrap(mapper);
        Person person = new Person("John", 30);

        String xml = wrapper.toXml(person);

        Assertions.assertNotNull(xml);
        Assertions.assertTrue(xml.contains("John"));
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

    //    @Test
    //    public void testOneFromXmlDataInput() throws Exception {
    //        XmlMapper mapper = new XmlMapper();
    //        XmlMappers.One wrapper = XmlMappers.wrap(mapper);
    //        String xml = "<Person><name>John</name><age>30</age></Person>";
    //        ByteArrayInputStream bais = new ByteArrayInputStream(xml.getBytes());
    //        DataInputStream dis = new DataInputStream(bais);
    //
    //        Person person = wrapper.fromXml((DataInput) dis, Person.class);
    //
    //        Assertions.assertNotNull(person);
    //        Assertions.assertEquals("John", person.getName());
    //    }

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

    //    @Test
    //    public void testOneFromXmlWithTypeReferenceDataInput() throws Exception {
    //        XmlMapper mapper = new XmlMapper();
    //        XmlMappers.One wrapper = XmlMappers.wrap(mapper);
    //        String xml = "<ArrayList><item>a</item><item>b</item></ArrayList>";
    //        ByteArrayInputStream bais = new ByteArrayInputStream(xml.getBytes());
    //        DataInputStream dis = new DataInputStream(bais);
    //
    //        List<String> list = wrapper.fromXml((DataInput) dis, new TypeReference<List<String>>() {
    //        });
    //
    //        Assertions.assertNotNull(list);
    //        Assertions.assertEquals(2, list.size());
    //    }

    // Test bean class
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