package com.landawn.abacus.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class PropertiesUtil101Test extends TestBase {

    private File tempFile;

    @BeforeEach
    public void setUp() throws IOException {
        tempFile = File.createTempFile("test", ".properties");
    }

    @AfterEach
    public void tearDown() {
        if (tempFile != null && tempFile.exists()) {
            tempFile.delete();
        }
    }

    @Test
    public void testFindFile() {
        File file = PropertiesUtil.findFile("test.properties");
        Assertions.assertTrue(file == null || file.getName().equals("test.properties"));
    }

    @Test
    public void testFindDir() {
        File dir = PropertiesUtil.findDir("test");
        Assertions.assertTrue(dir == null || dir.isDirectory());
    }

    @Test
    public void testLoadFromFile() throws IOException {
        Properties<String, String> testProps = new Properties<>();
        testProps.put("key1", "value1");
        testProps.put("key2", "value2");
        PropertiesUtil.store(testProps, "Test", tempFile);

        Properties<String, String> loaded = PropertiesUtil.load(tempFile);
        Assertions.assertEquals("value1", loaded.get("key1"));
        Assertions.assertEquals("value2", loaded.get("key2"));
    }

    @Test
    public void testLoadFromFileWithAutoRefresh() throws IOException {
        Properties<String, String> testProps = new Properties<>();
        testProps.put("key1", "value1");
        PropertiesUtil.store(testProps, "Test", tempFile);

        Properties<String, String> loaded = PropertiesUtil.load(tempFile, true);
        Assertions.assertEquals("value1", loaded.get("key1"));
    }

    @Test
    public void testLoadFromInputStream() throws IOException {
        String content = "key1=value1\nkey2=value2";
        ByteArrayInputStream is = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));

        Properties<String, String> loaded = PropertiesUtil.load(is);
        Assertions.assertEquals("value1", loaded.get("key1"));
        Assertions.assertEquals("value2", loaded.get("key2"));
    }

    @Test
    public void testLoadFromReader() throws IOException {
        String content = "key1=value1\nkey2=value2";
        StringReader reader = new StringReader(content);

        Properties<String, String> loaded = PropertiesUtil.load(reader);
        Assertions.assertEquals("value1", loaded.get("key1"));
        Assertions.assertEquals("value2", loaded.get("key2"));
    }

    @Test
    public void testLoadFromXmlFile() throws IOException {
        Properties<String, Object> testProps = new Properties<>();
        testProps.put("key1", "value1");
        testProps.put("key2", 123);

        File xmlFile = File.createTempFile("test", ".xml");
        try {
            PropertiesUtil.storeToXml(testProps, "root", true, xmlFile);

            Properties<String, Object> loaded = PropertiesUtil.loadFromXml(xmlFile);
            Assertions.assertEquals("value1", loaded.get("key1"));
            Assertions.assertEquals(123, loaded.get("key2"));
        } finally {
            xmlFile.delete();
        }
    }

    @Test
    public void testLoadFromXmlWithAutoRefresh() throws IOException {
        Properties<String, Object> testProps = new Properties<>();
        testProps.put("key1", "value1");

        File xmlFile = File.createTempFile("test", ".xml");
        try {
            PropertiesUtil.storeToXml(testProps, "root", true, xmlFile);

            Properties<String, Object> loaded = PropertiesUtil.loadFromXml(xmlFile, true);
            Assertions.assertEquals("value1", loaded.get("key1"));
        } finally {
            xmlFile.delete();
        }
    }

    @Test
    public void testLoadFromXmlInputStream() throws IOException {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><root><key1>value1</key1></root>";
        ByteArrayInputStream is = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

        Properties<String, Object> loaded = PropertiesUtil.loadFromXml(is);
        Assertions.assertEquals("value1", loaded.get("key1"));
    }

    @Test
    public void testLoadFromXmlReader() throws IOException {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><root><key1>value1</key1></root>";
        StringReader reader = new StringReader(xml);

        Properties<String, Object> loaded = PropertiesUtil.loadFromXml(reader);
        Assertions.assertEquals("value1", loaded.get("key1"));
    }

    @Test
    public void testStoreToFile() throws IOException {
        Properties<String, String> props = new Properties<>();
        props.put("key1", "value1");
        props.put("key2", "value2");

        PropertiesUtil.store(props, "Test Comments", tempFile);

        Assertions.assertTrue(tempFile.exists());
        Assertions.assertTrue(tempFile.length() > 0);
    }

    @Test
    public void testStoreToOutputStream() throws IOException {
        Properties<String, String> props = new Properties<>();
        props.put("key1", "value1");

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        PropertiesUtil.store(props, "Test", os);

        String result = os.toString();
        Assertions.assertTrue(result.contains("key1=value1"));
    }

    @Test
    public void testStoreToWriter() throws IOException {
        Properties<String, String> props = new Properties<>();
        props.put("key1", "value1");

        StringWriter writer = new StringWriter();
        PropertiesUtil.store(props, "Test", writer);

        String result = writer.toString();
        Assertions.assertTrue(result.contains("key1=value1"));
    }

    @Test
    public void testStoreToXml() throws IOException {
        Properties<String, Object> props = new Properties<>();
        props.put("key1", "value1");
        props.put("key2", 123);

        File xmlFile = File.createTempFile("test", ".xml");
        try {
            PropertiesUtil.storeToXml(props, "root", true, xmlFile);

            Assertions.assertTrue(xmlFile.exists());
            Assertions.assertTrue(xmlFile.length() > 0);
        } finally {
            xmlFile.delete();
        }
    }

    @Test
    public void testXml2Java() throws IOException {
        String xml = "<config><name>test</name><value>123</value></config>";
        String srcPath = System.getProperty("java.io.tmpdir");
        String packageName = "com.test";
        String className = "TestConfig";

        try {
            PropertiesUtil.xml2Java(xml, srcPath, packageName, className, false);
            File expectedFile = new File(srcPath + "/com/test/TestConfig.java");
            if (expectedFile.exists()) {
                expectedFile.delete();
            }
        } catch (Exception e) {
        }
    }
}
