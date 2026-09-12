package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.landawn.abacus.parser.JsonDeserConfig;
import com.landawn.abacus.parser.XmlDeserConfig;
import com.landawn.abacus.type.Type;

public class NFromTest extends NTestSupport {

    @Test
    public void testFromJson_string() {
        String json = getExpectedJsonForSampleBean(false);
        assertEquals(createSampleBean(), N.fromJson(json, TestBean.class));
        assertEquals(createSampleBean(), N.fromJson(json, new TypeReference<TestBean>() {
        }.type()));
        assertEquals(createSampleBean(), N.fromJson(json, JsonDeserConfig.create(), TestBean.class));
        assertEquals(createSampleBean(), N.fromJson(json, JsonDeserConfig.create(), Type.of(TestBean.class)));

        String padded = "###" + json + "@@@";
        assertEquals(createSampleBean(), N.fromJson(padded, 3, padded.length() - 3, TestBean.class));
        assertEquals(createSampleBean(), N.fromJson(padded, 3, padded.length() - 3, Type.of(TestBean.class)));
        assertEquals(createSampleBean(), N.fromJson(padded, 3, padded.length() - 3, JsonDeserConfig.create(), TestBean.class));
        assertThrows(IndexOutOfBoundsException.class, () -> N.fromJson("{}", 0, 10, TestBean.class));

        TestBean defaultBean = new TestBean("default", 0, null, null);
        assertEquals(defaultBean, N.fromJson((String) null, defaultBean, TestBean.class));
        assertEquals(createSampleBean(), N.fromJson(json, defaultBean, TestBean.class));
        assertEquals(defaultBean, N.fromJson(null, defaultBean, Type.of(TestBean.class)));

        TestPerson person = N.fromJson(TEST_JSON, TestPerson.class);
        assertEquals("John", person.getName());
        assertEquals(30, person.getAge());
        assertEquals(Arrays.asList(1, 2, 3), N.fromJson("[1,2,3]", new TypeReference<List<Integer>>() {
        }.type()));
        assertThrows(Exception.class, () -> N.fromJson("invalid json", TestPerson.class));
    }

    @Test
    public void testFromJson_fileStreamReader(@TempDir Path jsonTempDir) throws IOException {
        String json = getExpectedJsonForSampleBean(false);
        File inputFile = jsonTempDir.resolve("input.json").toFile();
        try (FileWriter writer = new FileWriter(inputFile)) {
            writer.write(json);
        }
        assertEquals(createSampleBean(), N.fromJson(inputFile, TestBean.class));
        assertEquals(createSampleBean(), N.fromJson(inputFile, Type.of(TestBean.class)));
        assertEquals(createSampleBean(), N.fromJson(inputFile, JsonDeserConfig.create(), TestBean.class));
        assertEquals(createSampleBean(), N.fromJson(inputFile, JsonDeserConfig.create(), Type.of(TestBean.class)));

        try (InputStream in = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8))) {
            assertEquals(createSampleBean(), N.fromJson(in, TestBean.class));
        }
        try (InputStream in = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8))) {
            assertEquals(createSampleBean(), N.fromJson(in, Type.of(TestBean.class)));
        }
        try (InputStream in = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8))) {
            assertEquals(createSampleBean(), N.fromJson(in, JsonDeserConfig.create(), TestBean.class));
        }
        try (Reader reader = new StringReader(json)) {
            assertEquals(createSampleBean(), N.fromJson(reader, TestBean.class));
        }
        try (Reader reader = new StringReader(json)) {
            assertEquals(createSampleBean(), N.fromJson(reader, JsonDeserConfig.create(), Type.of(TestBean.class)));
        }
    }

    @Test
    public void testFromXml(@TempDir Path xmlTempDir) throws IOException {
        TestBean bean = createSampleBean();
        String xml = N.toXml(bean);
        assertEquals(bean, N.fromXml(xml, TestBean.class));
        assertEquals(bean, N.fromXml(xml, XmlDeserConfig.create(), TestBean.class));
        assertEquals(bean, N.fromXml(xml, XmlDeserConfig.create(), Type.of(TestBean.class)));

        TestPerson person = N.fromXml(TEST_XML, TestPerson.class);
        assertEquals("John", person.getName());
        assertEquals(30, person.getAge());
        assertThrows(Exception.class, () -> N.fromXml("invalid xml", TestPerson.class));

        File xmlFile = xmlTempDir.resolve("bean.xml").toFile();
        N.toXml(bean, xmlFile);
        assertEquals(bean, N.fromXml(xmlFile, TestBean.class));
        assertEquals(bean, N.fromXml(xmlFile, Type.of(TestBean.class)));

        try (InputStream in = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))) {
            assertEquals(bean, N.fromXml(in, TestBean.class));
        }
        try (InputStream in = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))) {
            assertEquals(bean, N.fromXml(in, Type.of(TestBean.class)));
        }
    }
}
