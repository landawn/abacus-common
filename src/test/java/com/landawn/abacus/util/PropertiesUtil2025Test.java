package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.ParsingException;
import com.landawn.abacus.exception.UncheckedIOException;

@Tag("2025")
public class PropertiesUtil2025Test extends TestBase {

    @TempDir
    Path tempDir;

    private File testPropertiesFile;
    private File testXmlFile;
    private File testInvalidXmlFile;
    private File testComplexXmlFile;

    @BeforeEach
    public void setUp() throws IOException {
        testPropertiesFile = tempDir.resolve("test.properties").toFile();
        try (FileWriter writer = new FileWriter(testPropertiesFile)) {
            writer.write("key1=value1\n");
            writer.write("key2=value2\n");
            writer.write("key3=value3\n");
            writer.write("nested.key=nested.value\n");
        }

        testXmlFile = tempDir.resolve("test.xml").toFile();
        try (FileWriter writer = new FileWriter(testXmlFile)) {
            writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            writer.write("<config>\n");
            writer.write("  <database>\n");
            writer.write("    <url>jdbc:mysql://localhost/test</url>\n");
            writer.write("    <username>admin</username>\n");
            writer.write("    <port type=\"int\">3306</port>\n");
            writer.write("  </database>\n");
            writer.write("  <timeout type=\"long\">5000</timeout>\n");
            writer.write("  <enabled type=\"boolean\">true</enabled>\n");
            writer.write("</config>\n");
        }

        testInvalidXmlFile = tempDir.resolve("invalid.xml").toFile();
        try (FileWriter writer = new FileWriter(testInvalidXmlFile)) {
            writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            writer.write("<config>\n");
            writer.write("  <unclosed>\n");
            writer.write("</config>\n");
        }

        testComplexXmlFile = tempDir.resolve("complex.xml").toFile();
        try (FileWriter writer = new FileWriter(testComplexXmlFile)) {
            writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            writer.write("<application>\n");
            writer.write("  <name>TestApp</name>\n");
            writer.write("  <version type=\"int\">1</version>\n");
            writer.write("  <settings>\n");
            writer.write("    <debug type=\"boolean\">true</debug>\n");
            writer.write("    <maxConnections type=\"int\">100</maxConnections>\n");
            writer.write("  </settings>\n");
            writer.write("</application>\n");
        }
    }

    @AfterEach
    public void tearDown() {
    }

    @Test
    public void testFindFile() {
        File result = PropertiesUtil.findFile("PropertiesUtil.java");

        File nonExisting = PropertiesUtil.findFile("non-existing-file-12345.txt");
    }

    @Test
    public void testFindFileNull() {
        assertThrows(RuntimeException.class, () -> PropertiesUtil.findFile(null));
    }

    @Test
    public void testFindDir() {
        File result = PropertiesUtil.findDir("com");

        File nonExisting = PropertiesUtil.findDir("non-existing-dir-12345");
    }

    @Test
    public void testFindDirNull() {
        assertThrows(RuntimeException.class, () -> PropertiesUtil.findDir(null));
    }

    @Test
    public void testLoadFromFile() {
        Properties<String, String> props = PropertiesUtil.load(testPropertiesFile);

        assertNotNull(props);
        assertEquals("value1", props.get("key1"));
        assertEquals("value2", props.get("key2"));
        assertEquals("value3", props.get("key3"));
        assertEquals("nested.value", props.get("nested.key"));
    }

    @Test
    public void testLoadFromFileNonExistent() {
        File nonExistent = new File(tempDir.toFile(), "nonexistent.properties");
        assertThrows(UncheckedIOException.class, () -> {
            PropertiesUtil.load(nonExistent);
        });
    }

    @Test
    public void testLoadFromFileWithAutoRefresh() throws IOException, InterruptedException {
        Properties<String, String> props = PropertiesUtil.load(testPropertiesFile, true);

        assertNotNull(props);
        assertEquals("value1", props.get("key1"));

        Thread.sleep(100);
        try (FileWriter writer = new FileWriter(testPropertiesFile)) {
            writer.write("key1=newValue1\n");
            writer.write("key2=value2\n");
        }

        Thread.sleep(3000);

    }

    @Test
    public void testLoadFromFileWithoutAutoRefresh() {
        Properties<String, String> props = PropertiesUtil.load(testPropertiesFile, false);

        assertNotNull(props);
        assertEquals("value1", props.get("key1"));
    }

    @Test
    public void testLoadFromInputStream() throws IOException {
        try (InputStream is = new FileInputStream(testPropertiesFile)) {
            Properties<String, String> props = PropertiesUtil.load(is);

            assertNotNull(props);
            assertEquals("value1", props.get("key1"));
            assertEquals("value2", props.get("key2"));
        }
    }

    @Test
    public void testLoadFromInputStreamEmpty() throws IOException {
        String emptyProps = "";
        try (InputStream is = new ByteArrayInputStream(emptyProps.getBytes())) {
            Properties<String, String> props = PropertiesUtil.load(is);
            assertNotNull(props);
            assertTrue(props.isEmpty());
        }
    }

    @Test
    public void testLoadFromReader() throws IOException {
        try (Reader reader = new FileReader(testPropertiesFile)) {
            Properties<String, String> props = PropertiesUtil.load(reader);

            assertNotNull(props);
            assertEquals("value1", props.get("key1"));
            assertEquals("value2", props.get("key2"));
        }
    }

    @Test
    public void testLoadFromReaderWithSpecialChars() throws IOException {
        String propsContent = "key.with.dots=value\nkey\\:with\\:colons=value2\n";
        try (Reader reader = new StringReader(propsContent)) {
            Properties<String, String> props = PropertiesUtil.load(reader);
            assertNotNull(props);
            assertEquals("value", props.get("key.with.dots"));
        }
    }

    @Test
    public void testLoadFromXmlFile() {
        Properties<String, Object> props = PropertiesUtil.loadFromXml(testXmlFile);

        assertNotNull(props);
        assertNotNull(props.get("database"));
        assertNotNull(props.get("timeout"));
        assertEquals(true, props.get("enabled"));
    }

    @Test
    public void testLoadFromXmlFileInvalid() {
        assertThrows(ParsingException.class, () -> {
            PropertiesUtil.loadFromXml(testInvalidXmlFile);
        });
    }

    @Test
    public void testLoadFromXmlFileWithAutoRefresh() {
        Properties<String, Object> props = PropertiesUtil.loadFromXml(testXmlFile, true);

        assertNotNull(props);
        assertNotNull(props.get("database"));
    }

    @Test
    public void testLoadFromXmlFileWithoutAutoRefresh() {
        Properties<String, Object> props = PropertiesUtil.loadFromXml(testXmlFile, false);

        assertNotNull(props);
        assertNotNull(props.get("database"));
    }

    @Test
    public void testLoadFromXmlInputStream() throws IOException {
        try (InputStream is = new FileInputStream(testXmlFile)) {
            Properties<String, Object> props = PropertiesUtil.loadFromXml(is);

            assertNotNull(props);
            assertNotNull(props.get("database"));
        }
    }

    @Test
    public void testLoadFromXmlInputStreamInvalid() throws IOException {
        String invalidXml = "<?xml version=\"1.0\"?><root><unclosed></root>";
        try (InputStream is = new ByteArrayInputStream(invalidXml.getBytes())) {
            assertThrows(ParsingException.class, () -> {
                PropertiesUtil.loadFromXml(is);
            });
        }
    }

    @Test
    public void testLoadFromXmlReader() throws IOException {
        try (Reader reader = new FileReader(testXmlFile)) {
            Properties<String, Object> props = PropertiesUtil.loadFromXml(reader);

            assertNotNull(props);
            assertNotNull(props.get("database"));
        }
    }

    @Test
    public void testLoadFromXmlReaderEmpty() throws IOException {
        String xml = "<?xml version=\"1.0\"?><root></root>";
        try (Reader reader = new StringReader(xml)) {
            Properties<String, Object> props = PropertiesUtil.loadFromXml(reader);
            assertNotNull(props);
        }
    }

    @Test
    public void testLoadFromXmlFileWithClass() {
        Properties<String, Object> props = PropertiesUtil.loadFromXml(testXmlFile, Properties.class);

        assertNotNull(props);
        assertNotNull(props.get("database"));
    }

    @Test
    public void testLoadFromXmlFileWithAutoRefreshAndClass() {
        Properties<String, Object> props = PropertiesUtil.loadFromXml(testXmlFile, true, Properties.class);

        assertNotNull(props);
        assertNotNull(props.get("database"));
    }

    @Test
    public void testLoadFromXmlFileWithoutAutoRefreshAndClass() {
        Properties<String, Object> props = PropertiesUtil.loadFromXml(testXmlFile, false, Properties.class);

        assertNotNull(props);
        assertNotNull(props.get("database"));
    }

    @Test
    public void testLoadFromXmlInputStreamWithClass() throws IOException {
        try (InputStream is = new FileInputStream(testXmlFile)) {
            Properties<String, Object> props = PropertiesUtil.loadFromXml(is, Properties.class);

            assertNotNull(props);
            assertNotNull(props.get("database"));
        }
    }

    @Test
    public void testLoadFromXmlReaderWithClass() throws IOException {
        try (Reader reader = new FileReader(testXmlFile)) {
            Properties<String, Object> props = PropertiesUtil.loadFromXml(reader, Properties.class);

            assertNotNull(props);
            assertNotNull(props.get("database"));
        }
    }

    @Test
    public void testStoreToFile() throws IOException {
        Properties<String, String> props = new Properties<>();
        props.put("testKey1", "testValue1");
        props.put("testKey2", "testValue2");

        File outputFile = tempDir.resolve("output.properties").toFile();
        PropertiesUtil.store(props, "Test Properties", outputFile);

        assertTrue(outputFile.exists());

        Properties<String, String> loaded = PropertiesUtil.load(outputFile);
        assertEquals("testValue1", loaded.get("testKey1"));
        assertEquals("testValue2", loaded.get("testKey2"));
    }

    @Test
    public void testStoreToFileWithNullComments() throws IOException {
        Properties<String, String> props = new Properties<>();
        props.put("key", "value");

        File outputFile = tempDir.resolve("output2.properties").toFile();
        PropertiesUtil.store(props, null, outputFile);

        assertTrue(outputFile.exists());
    }

    @Test
    public void testStoreToOutputStream() throws IOException {
        Properties<String, String> props = new Properties<>();
        props.put("testKey", "testValue");

        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PropertiesUtil.store(props, "Test", os);

            String output = os.toString();
            assertTrue(output.contains("testKey"));
            assertTrue(output.contains("testValue"));
        }
    }

    @Test
    public void testStoreToOutputStreamEmpty() throws IOException {
        Properties<String, String> props = new Properties<>();

        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PropertiesUtil.store(props, "Empty", os);
            assertNotNull(os.toString());
        }
    }

    @Test
    public void testStoreToWriter() throws IOException {
        Properties<String, String> props = new Properties<>();
        props.put("testKey", "testValue");

        try (StringWriter writer = new StringWriter()) {
            PropertiesUtil.store(props, "Test", writer);

            String output = writer.toString();
            assertTrue(output.contains("testKey"));
            assertTrue(output.contains("testValue"));
        }
    }

    @Test
    public void testStoreToWriterMultipleProperties() throws IOException {
        Properties<String, String> props = new Properties<>();
        props.put("key1", "value1");
        props.put("key2", "value2");
        props.put("key3", "value3");

        try (StringWriter writer = new StringWriter()) {
            PropertiesUtil.store(props, "Multiple", writer);

            String output = writer.toString();
            assertTrue(output.contains("key1"));
            assertTrue(output.contains("key2"));
            assertTrue(output.contains("key3"));
        }
    }

    @Test
    public void testStoreToXmlFile() throws IOException {
        Properties<String, Object> props = new Properties<>();
        props.put("key1", "value1");
        props.put("key2", 123);

        File outputFile = tempDir.resolve("output.xml").toFile();
        PropertiesUtil.storeToXml(props, "config", true, outputFile);

        assertTrue(outputFile.exists());

        Properties<String, Object> loaded = PropertiesUtil.loadFromXml(outputFile);
        assertEquals("value1", loaded.get("key1"));
    }

    @Test
    public void testStoreToXmlFileWithoutTypeInfo() throws IOException {
        Properties<String, Object> props = new Properties<>();
        props.put("key1", "value1");

        File outputFile = tempDir.resolve("output_no_type.xml").toFile();
        PropertiesUtil.storeToXml(props, "config", false, outputFile);

        assertTrue(outputFile.exists());
    }

    @Test
    public void testStoreToXmlOutputStream() throws IOException {
        Properties<String, Object> props = new Properties<>();
        props.put("key1", "value1");
        props.put("key2", true);

        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PropertiesUtil.storeToXml(props, "config", true, os);

            String output = os.toString();
            assertTrue(output.contains("key1"));
            assertTrue(output.contains("value1"));
        }
    }

    @Test
    public void testStoreToXmlOutputStreamWithTypeInfo() throws IOException {
        Properties<String, Object> props = new Properties<>();
        props.put("port", 8080);

        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PropertiesUtil.storeToXml(props, "config", true, os);

            String output = os.toString();
            assertTrue(output.contains("port"));
            assertTrue(output.contains("type="));
        }
    }

    @Test
    public void testStoreToXmlWriter() throws IOException {
        Properties<String, Object> props = new Properties<>();
        props.put("key1", "value1");

        try (StringWriter writer = new StringWriter()) {
            PropertiesUtil.storeToXml(props, "config", true, writer);

            String output = writer.toString();
            assertTrue(output.contains("key1"));
            assertTrue(output.contains("value1"));
            assertTrue(output.contains("<?xml"));
        }
    }

    @Test
    public void testStoreToXmlWriterNestedProperties() throws IOException {
        Properties<String, Object> props = new Properties<>();
        Properties<String, Object> nested = new Properties<>();
        nested.put("nestedKey", "nestedValue");
        props.put("nested", nested);

        try (StringWriter writer = new StringWriter()) {
            PropertiesUtil.storeToXml(props, "config", true, writer);

            String output = writer.toString();
            assertTrue(output.contains("nested"));
        }
    }

    @Test
    public void testXml2JavaFromString() throws IOException {
        String xml = "<?xml version=\"1.0\"?><config><name>Test</name><version type=\"int\">1</version></config>";

        String srcPath = tempDir.resolve("src").toFile().getAbsolutePath();
        new File(srcPath).mkdirs();

        PropertiesUtil.xmlToJava(xml, srcPath, "com.test", "TestConfig", false);

        File generatedFile = new File(srcPath + File.separator + "com" + File.separator + "test", "TestConfig.java");
        assertTrue(generatedFile.exists());

        String content = new String(Files.readAllBytes(generatedFile.toPath()));
        assertTrue(content.contains("package com.test;"));
        assertTrue(content.contains("class TestConfig"));
    }

    @Test
    public void testXml2JavaFromStringWithPublicFields() throws IOException {
        String xml = "<?xml version=\"1.0\"?><app><title>MyApp</title></app>";

        String srcPath = tempDir.resolve("src2").toFile().getAbsolutePath();
        new File(srcPath).mkdirs();

        PropertiesUtil.xmlToJava(xml, srcPath, "com.test2", "AppConfig", true);

        File generatedFile = new File(srcPath + File.separator + "com" + File.separator + "test2", "AppConfig.java");
        assertTrue(generatedFile.exists());
    }

    @Test
    public void testXml2JavaFromFile() throws IOException {
        String srcPath = tempDir.resolve("src3").toFile().getAbsolutePath();
        new File(srcPath).mkdirs();

        PropertiesUtil.xmlToJava(testComplexXmlFile, srcPath, "com.test3", "ComplexConfig", false);

        File generatedFile = new File(srcPath + File.separator + "com" + File.separator + "test3", "ComplexConfig.java");
        assertTrue(generatedFile.exists());

        String content = new String(Files.readAllBytes(generatedFile.toPath()));
        assertTrue(content.contains("package com.test3;"));
        assertTrue(content.contains("class ComplexConfig"));
    }

    @Test
    public void testXml2JavaFromFileWithNullClassName() throws IOException {
        String srcPath = tempDir.resolve("src4").toFile().getAbsolutePath();
        new File(srcPath).mkdirs();

        PropertiesUtil.xmlToJava(testComplexXmlFile, srcPath, "com.test4", null, false);

        File generatedFile = new File(srcPath + File.separator + "com" + File.separator + "test4", "Application.java");
        assertTrue(generatedFile.exists());
    }

    @Test
    public void testXml2JavaFromInputStream() throws IOException {
        String xml = "<?xml version=\"1.0\"?><myapp><setting>value</setting></myapp>";

        String srcPath = tempDir.resolve("src5").toFile().getAbsolutePath();
        new File(srcPath).mkdirs();

        try (InputStream is = new ByteArrayInputStream(xml.getBytes())) {
            PropertiesUtil.xmlToJava(is, srcPath, "com.test5", "MyApp", false);
        }

        File generatedFile = new File(srcPath + File.separator + "com" + File.separator + "test5", "MyApp.java");
        assertTrue(generatedFile.exists());
    }

    @Test
    public void testXml2JavaFromInputStreamComplex() throws IOException {
        try (InputStream is = new FileInputStream(testComplexXmlFile)) {
            String srcPath = tempDir.resolve("src6").toFile().getAbsolutePath();
            new File(srcPath).mkdirs();

            PropertiesUtil.xmlToJava(is, srcPath, "com.test6", "AppSettings", false);

            File generatedFile = new File(srcPath + File.separator + "com" + File.separator + "test6", "AppSettings.java");
            assertTrue(generatedFile.exists());
        }
    }

    @Test
    public void testXml2JavaFromReader() throws IOException {
        String xml = "<?xml version=\"1.0\"?><system><param>test</param></system>";

        String srcPath = tempDir.resolve("src7").toFile().getAbsolutePath();
        new File(srcPath).mkdirs();

        try (Reader reader = new StringReader(xml)) {
            PropertiesUtil.xmlToJava(reader, srcPath, "com.test7", "SystemConfig", false);
        }

        File generatedFile = new File(srcPath + File.separator + "com" + File.separator + "test7", "SystemConfig.java");
        assertTrue(generatedFile.exists());

        String content = new String(Files.readAllBytes(generatedFile.toPath()));
        assertTrue(content.contains("package com.test7;"));
    }

    @Test
    public void testXml2JavaFromReaderWithNestedElements() throws IOException {
        String xml = "<?xml version=\"1.0\"?><root><nested><value>test</value></nested></root>";

        String srcPath = tempDir.resolve("src8").toFile().getAbsolutePath();
        new File(srcPath).mkdirs();

        try (Reader reader = new StringReader(xml)) {
            PropertiesUtil.xmlToJava(reader, srcPath, "com.test8", "RootConfig", false);
        }

        File generatedFile = new File(srcPath + File.separator + "com" + File.separator + "test8", "RootConfig.java");
        assertTrue(generatedFile.exists());
    }

    @Test
    public void testStoreAndLoadRoundTrip() throws IOException {
        Properties<String, String> original = new Properties<>();
        original.put("key1", "value1");
        original.put("key2", "value2");
        original.put("key3", "value3");

        File tempFile = tempDir.resolve("roundtrip.properties").toFile();
        PropertiesUtil.store(original, "Round trip test", tempFile);

        Properties<String, String> loaded = PropertiesUtil.load(tempFile);

        assertEquals(original.size(), loaded.size());
        assertEquals(original.get("key1"), loaded.get("key1"));
        assertEquals(original.get("key2"), loaded.get("key2"));
        assertEquals(original.get("key3"), loaded.get("key3"));
    }

    @Test
    public void testStoreToXmlAndLoadRoundTrip() throws IOException {
        Properties<String, Object> original = new Properties<>();
        original.put("stringKey", "stringValue");
        original.put("intKey", 42);
        original.put("boolKey", true);

        File tempFile = tempDir.resolve("roundtrip.xml").toFile();
        PropertiesUtil.storeToXml(original, "root", true, tempFile);

        Properties<String, Object> loaded = PropertiesUtil.loadFromXml(tempFile);

        assertNotNull(loaded);
        assertEquals("stringValue", loaded.get("stringKey"));
    }

    @Test
    public void testLoadFromEmptyPropertiesFile() throws IOException {
        File emptyFile = tempDir.resolve("empty.properties").toFile();
        emptyFile.createNewFile();

        Properties<String, String> props = PropertiesUtil.load(emptyFile);
        assertNotNull(props);
        assertTrue(props.isEmpty());
    }

    @Test
    public void testXml2JavaWithDuplicatedPropNames() throws IOException {
        String xml = "<?xml version=\"1.0\"?><root><item>value1</item><item>value2</item></root>";

        String srcPath = tempDir.resolve("src9").toFile().getAbsolutePath();
        new File(srcPath).mkdirs();

        assertThrows(RuntimeException.class, () -> {
            PropertiesUtil.xmlToJava(xml, srcPath, "com.test9", "DupConfig", false);
        });
    }

    @Test
    public void testLoadFromXmlWithDuplicatedProperties() throws IOException {
        String xml = "<?xml version=\"1.0\"?><root><item>value1</item><item>value2</item></root>";

        assertThrows(RuntimeException.class, () -> {
            try (Reader reader = new StringReader(xml)) {
                PropertiesUtil.loadFromXml(reader);
            }
        });
    }
}
