package com.landawn.abacus.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.ParseException;
import com.landawn.abacus.exception.UncheckedIOException;

public class PropertiesUtil100Test extends TestBase {

    @TempDir
    Path tempDir;

    private File testPropertiesFile;
    private File testXmlFile;
    private File testInvalidXmlFile;

    @BeforeEach
    public void setUp() throws IOException {
        // Create test properties file
        testPropertiesFile = tempDir.resolve("test.properties").toFile();
        try (FileWriter writer = new FileWriter(testPropertiesFile)) {
            writer.write("key1=value1\n");
            writer.write("key2=value2\n");
            writer.write("key3=value3\n");
            writer.write("nested.key=nested.value\n");
        }

        // Create test XML file
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

        // Create invalid XML file
        testInvalidXmlFile = tempDir.resolve("invalid.xml").toFile();
        try (FileWriter writer = new FileWriter(testInvalidXmlFile)) {
            writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            writer.write("<config>\n");
            writer.write("  <unclosed>\n");
            writer.write("</config>\n");
        }
    }

    @AfterEach
    public void tearDown() {
        // Clean up is handled by @TempDir
    }

    @Test
    public void testFindFile() {
        // Test finding existing file
        File configFile = PropertiesUtil.findFile("PropertiesUtil.java");
        // The file might or might not exist depending on the test environment
        // Just ensure the method doesn't throw exception

        // Test finding non-existing file
        File nonExistingFile = PropertiesUtil.findFile("non-existing-file.txt");
        // Method should return null or a non-existing file
    }

    @Test
    public void testFindDir() {
        // Test finding existing directory
        File configDir = PropertiesUtil.findDir("com");
        // The directory might or might not exist depending on the test environment

        // Test finding non-existing directory
        File nonExistingDir = PropertiesUtil.findDir("non-existing-directory");
        // Method should return null or a non-existing directory
    }

    @Test
    public void testLoadFromFile() {
        Properties<String, String> props = PropertiesUtil.load(testPropertiesFile);

        Assertions.assertNotNull(props);
        Assertions.assertEquals("value1", props.get("key1"));
        Assertions.assertEquals("value2", props.get("key2"));
        Assertions.assertEquals("value3", props.get("key3"));
        Assertions.assertEquals("nested.value", props.get("nested.key"));
    }

    @Test
    public void testLoadFromFileWithAutoRefresh() throws IOException, InterruptedException {
        Properties<String, String> props = PropertiesUtil.load(testPropertiesFile, true);

        Assertions.assertNotNull(props);
        Assertions.assertEquals("value1", props.get("key1"));

        // Modify the file
        Thread.sleep(1100); // Wait for auto-refresh to kick in
        try (FileWriter writer = new FileWriter(testPropertiesFile)) {
            writer.write("key1=modified_value1\n");
            writer.write("key2=value2\n");
            writer.write("newKey=newValue\n");
        }

        Thread.sleep(2000); // Wait for auto-refresh to process the change

        // Check if properties were refreshed
        Assertions.assertEquals("modified_value1", props.get("key1"));
        Assertions.assertEquals("value2", props.get("key2"));
        Assertions.assertEquals("newValue", props.get("newKey"));
        Assertions.assertNull(props.get("key3")); // Removed key
    }

    @Test
    public void testLoadFromInputStream() throws IOException {
        try (InputStream is = new FileInputStream(testPropertiesFile)) {
            Properties<String, String> props = PropertiesUtil.load(is);

            Assertions.assertNotNull(props);
            Assertions.assertEquals("value1", props.get("key1"));
            Assertions.assertEquals("value2", props.get("key2"));
        }
    }

    @Test
    public void testLoadFromReader() throws IOException {
        try (Reader reader = new FileReader(testPropertiesFile)) {
            Properties<String, String> props = PropertiesUtil.load(reader);

            Assertions.assertNotNull(props);
            Assertions.assertEquals("value1", props.get("key1"));
            Assertions.assertEquals("value2", props.get("key2"));
        }
    }

    @Test
    public void testLoadFromXmlFile() {
        Properties<String, Object> props = PropertiesUtil.loadFromXml(testXmlFile);

        Assertions.assertNotNull(props);
        Assertions.assertTrue(props.get("database") instanceof Properties);

        Properties<String, Object> dbProps = (Properties<String, Object>) props.get("database");
        Assertions.assertEquals("jdbc:mysql://localhost/test", dbProps.get("url"));
        Assertions.assertEquals("admin", dbProps.get("username"));
        Assertions.assertEquals(3306, dbProps.get("port"));

        Assertions.assertEquals(5000L, props.get("timeout"));
        Assertions.assertEquals(true, props.get("enabled"));
    }

    @Test
    public void testLoadFromXmlFileWithAutoRefresh() throws IOException, InterruptedException {
        Properties<String, Object> props = PropertiesUtil.loadFromXml(testXmlFile, true);

        Assertions.assertNotNull(props);
        Assertions.assertEquals(5000L, props.get("timeout"));

        // Modify the XML file
        Thread.sleep(1100);
        try (FileWriter writer = new FileWriter(testXmlFile)) {
            writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            writer.write("<config>\n");
            writer.write("  <timeout type=\"long\">10000</timeout>\n");
            writer.write("  <newProperty>newValue</newProperty>\n");
            writer.write("</config>\n");
        }

        Thread.sleep(2000); // Wait for auto-refresh

        Assertions.assertEquals(10000L, props.get("timeout"));
        Assertions.assertEquals("newValue", props.get("newProperty"));
        Assertions.assertNull(props.get("database")); // Removed property
    }

    @Test
    public void testLoadFromXmlInputStream() throws IOException {
        try (InputStream is = new FileInputStream(testXmlFile)) {
            Properties<String, Object> props = PropertiesUtil.loadFromXml(is);

            Assertions.assertNotNull(props);
            Assertions.assertEquals(5000L, props.get("timeout"));
            Assertions.assertEquals(true, props.get("enabled"));
        }
    }

    @Test
    public void testLoadFromXmlReader() throws IOException {
        try (Reader reader = new FileReader(testXmlFile)) {
            Properties<String, Object> props = PropertiesUtil.loadFromXml(reader);

            Assertions.assertNotNull(props);
            Assertions.assertEquals(5000L, props.get("timeout"));
            Assertions.assertEquals(true, props.get("enabled"));
        }
    }

    @Test
    public void testLoadFromXmlWithCustomClass() {
        CustomProperties props = PropertiesUtil.loadFromXml(testXmlFile, CustomProperties.class);

        Assertions.assertNotNull(props);
        Assertions.assertTrue(props instanceof CustomProperties);
        Assertions.assertEquals(5000L, props.get("timeout"));
    }

    @Test
    public void testLoadFromXmlWithCustomClassAndAutoRefresh() throws IOException, InterruptedException {

        CustomProperties props = PropertiesUtil.loadFromXml(testXmlFile, true, CustomProperties.class);

        Assertions.assertNotNull(props);
        Assertions.assertTrue(props instanceof CustomProperties);
        Assertions.assertEquals(5000L, props.get("timeout"));
    }

    public static class CustomProperties extends Properties<String, Object> {
        public CustomProperties() {
            super();
        }

        // Custom methods can be added here if needed
    }

    @Test
    public void testLoadFromXmlInputStreamWithCustomClass() throws IOException {

        try (InputStream is = new FileInputStream(testXmlFile)) {
            CustomProperties props = PropertiesUtil.loadFromXml(is, CustomProperties.class);

            Assertions.assertNotNull(props);
            Assertions.assertTrue(props instanceof CustomProperties);
            Assertions.assertEquals(5000L, props.get("timeout"));
        }
    }

    @Test
    public void testLoadFromXmlReaderWithCustomClass() throws IOException {
        try (Reader reader = new FileReader(testXmlFile)) {
            CustomProperties props = PropertiesUtil.loadFromXml(reader, CustomProperties.class);

            Assertions.assertNotNull(props);
            Assertions.assertTrue(props instanceof CustomProperties);
            Assertions.assertEquals(5000L, props.get("timeout"));
        }
    }

    @Test
    public void testStoreToFile() throws IOException {
        Properties<String, String> props = new Properties<>();
        props.put("test.key1", "test.value1");
        props.put("test.key2", "test.value2");

        File outputFile = tempDir.resolve("output.properties").toFile();
        PropertiesUtil.store(props, "Test Properties", outputFile);

        Assertions.assertTrue(outputFile.exists());

        // Load and verify
        Properties<String, String> loadedProps = PropertiesUtil.load(outputFile);
        Assertions.assertEquals("test.value1", loadedProps.get("test.key1"));
        Assertions.assertEquals("test.value2", loadedProps.get("test.key2"));
    }

    @Test
    public void testStoreToOutputStream() throws IOException {
        Properties<String, String> props = new Properties<>();
        props.put("test.key1", "test.value1");
        props.put("test.key2", "test.value2");

        File outputFile = tempDir.resolve("output-stream.properties").toFile();
        try (OutputStream os = new FileOutputStream(outputFile)) {
            PropertiesUtil.store(props, "Test Properties", os);
        }

        // Load and verify
        Properties<String, String> loadedProps = PropertiesUtil.load(outputFile);
        Assertions.assertEquals("test.value1", loadedProps.get("test.key1"));
        Assertions.assertEquals("test.value2", loadedProps.get("test.key2"));
    }

    @Test
    public void testStoreToWriter() throws IOException {
        Properties<String, String> props = new Properties<>();
        props.put("test.key1", "test.value1");
        props.put("test.key2", "test.value2");

        File outputFile = tempDir.resolve("output-writer.properties").toFile();
        try (Writer writer = new FileWriter(outputFile)) {
            PropertiesUtil.store(props, "Test Properties", writer);
        }

        // Load and verify
        Properties<String, String> loadedProps = PropertiesUtil.load(outputFile);
        Assertions.assertEquals("test.value1", loadedProps.get("test.key1"));
        Assertions.assertEquals("test.value2", loadedProps.get("test.key2"));
    }

    @Test
    public void testStoreToXmlFile() throws IOException {
        Properties<String, Object> props = new Properties<>();
        props.put("stringValue", "test");
        props.put("intValue", 123);
        props.put("booleanValue", true);

        Properties<String, Object> nestedProps = new Properties<>();
        nestedProps.put("nested1", "value1");
        nestedProps.put("nested2", 456);
        props.put("nestedProperties", nestedProps);

        File outputFile = tempDir.resolve("output.xml").toFile();
        PropertiesUtil.storeToXml(props, "testConfig", true, outputFile);

        Assertions.assertTrue(outputFile.exists());

        // Load and verify
        Properties<String, Object> loadedProps = PropertiesUtil.loadFromXml(outputFile);
        Assertions.assertEquals("test", loadedProps.get("stringValue"));
        Assertions.assertEquals(123, loadedProps.get("intValue"));
        Assertions.assertEquals(true, loadedProps.get("booleanValue"));

        Properties<String, Object> loadedNested = (Properties<String, Object>) loadedProps.get("nestedProperties");
        Assertions.assertEquals("value1", loadedNested.get("nested1"));
        Assertions.assertEquals(456, loadedNested.get("nested2"));
    }

    @Test
    public void testStoreToXmlOutputStream() throws IOException {
        Properties<String, Object> props = new Properties<>();
        props.put("key1", "value1");
        props.put("key2", 100);

        File outputFile = tempDir.resolve("output-stream.xml").toFile();
        try (OutputStream os = new FileOutputStream(outputFile)) {
            PropertiesUtil.storeToXml(props, "config", false, os);
        }

        // Load and verify
        Properties<String, Object> loadedProps = PropertiesUtil.loadFromXml(outputFile);
        Assertions.assertEquals("value1", loadedProps.get("key1"));
        Assertions.assertEquals("100", loadedProps.get("key2")); // Without type info, loaded as string
    }

    @Test
    public void testStoreToXmlWriter() throws IOException {
        Properties<String, Object> props = new Properties<>();
        props.put("key1", "value1");
        props.put("key2", 200L);

        File outputFile = tempDir.resolve("output-writer.xml").toFile();
        try (Writer writer = new FileWriter(outputFile)) {
            PropertiesUtil.storeToXml(props, "settings", true, writer);
        }

        // Load and verify
        Properties<String, Object> loadedProps = PropertiesUtil.loadFromXml(outputFile);
        Assertions.assertEquals("value1", loadedProps.get("key1"));
        Assertions.assertEquals(200L, loadedProps.get("key2"));
    }

    @Test
    public void testXml2JavaFromString() throws IOException {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<config>\n" + "  <database>\n" + "    <url>jdbc:mysql://localhost</url>\n"
                + "    <port type=\"int\">3306</port>\n" + "  </database>\n" + "  <timeout type=\"long\">5000</timeout>\n" + "</config>";

        String srcPath = tempDir.toString();
        String packageName = "com.test.generated";
        String className = "TestConfig";

        IOUtil.mkdirsIfNotExists(new File(srcPath + "/com/test/generated"));

        PropertiesUtil.xml2Java(xml, srcPath, packageName, className, false);

        // Verify the generated file exists
        File generatedFile = new File(srcPath + "/com/test/generated/TestConfig.java");
        Assertions.assertTrue(generatedFile.exists());

        // Read and verify basic structure
        String content = new String(Files.readAllBytes(generatedFile.toPath()));
        Assertions.assertTrue(content.contains("package com.test.generated;"));
        Assertions.assertTrue(content.contains("public class TestConfig extends Properties<String, Object>"));
        Assertions.assertTrue(content.contains("public static class Database extends Properties<String, Object>"));
        Assertions.assertTrue(content.contains("public Database getDatabase()"));
        Assertions.assertTrue(content.contains("public void setDatabase(Database database)"));
        Assertions.assertTrue(content.contains("public long getTimeout()"));
        Assertions.assertTrue(content.contains("public void setTimeout(long timeout)"));
    }

    @Test
    public void testXml2JavaFromFile() throws IOException {
        String srcPath = tempDir.toString();
        String packageName = "com.test.generated";
        String className = "ConfigFromFile";

        IOUtil.mkdirsIfNotExists(new File(srcPath + "/com/test/generated"));

        PropertiesUtil.xml2Java(testXmlFile, srcPath, packageName, className, true);

        // Verify the generated file exists
        File generatedFile = new File(srcPath + "/com/test/generated/ConfigFromFile.java");
        Assertions.assertTrue(generatedFile.exists());

        // Read and verify content
        String content = new String(Files.readAllBytes(generatedFile.toPath()));
        Assertions.assertTrue(content.contains("public class ConfigFromFile extends Properties<String, Object>"));
    }

    @Test
    public void testXml2JavaFromInputStream() throws IOException {
        String srcPath = tempDir.toString();
        String packageName = "com.test.generated";
        String className = "ConfigFromStream";

        IOUtil.mkdirsIfNotExists(new File(srcPath + "/com/test/generated"));

        try (InputStream is = new FileInputStream(testXmlFile)) {
            PropertiesUtil.xml2Java(is, srcPath, packageName, className, false);
        }

        // Verify the generated file exists
        File generatedFile = new File(srcPath + "/com/test/generated/ConfigFromStream.java");
        Assertions.assertTrue(generatedFile.exists());
    }

    @Test
    public void testXml2JavaFromReader() throws IOException {
        String srcPath = tempDir.toString();
        String packageName = "com.test.generated";

        IOUtil.mkdirsIfNotExists(new File(srcPath + "/com/test/generated"));

        try (Reader reader = new FileReader(testXmlFile)) {
            PropertiesUtil.xml2Java(reader, srcPath, packageName, null, false); // null className
        }

        // When className is null, it uses the root element name
        File generatedFile = new File(srcPath + "/com/test/generated/Config.java");
        Assertions.assertTrue(generatedFile.exists());
    }

    @Test
    public void testLoadFromInvalidXmlFile() {
        Assertions.assertThrows(ParseException.class, () -> {
            PropertiesUtil.loadFromXml(testInvalidXmlFile);
        });
    }

    @Test
    public void testLoadFromNonExistentFile() {
        File nonExistentFile = new File("non-existent-file.properties");
        Assertions.assertThrows(UncheckedIOException.class, () -> {
            PropertiesUtil.load(nonExistentFile);
        });
    }

    @Test
    public void testEmptyProperties() throws IOException {
        // Test with empty properties file
        File emptyFile = tempDir.resolve("empty.properties").toFile();
        emptyFile.createNewFile();

        Properties<String, String> props = PropertiesUtil.load(emptyFile);
        Assertions.assertNotNull(props);
        Assertions.assertTrue(props.isEmpty());
    }

    @Test
    public void testXmlWithComplexTypes() throws IOException {
        // Create XML with various types
        File complexXml = tempDir.resolve("complex.xml").toFile();
        try (FileWriter writer = new FileWriter(complexXml)) {
            writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            writer.write("<config>\n");
            writer.write("  <stringValue>test string</stringValue>\n");
            writer.write("  <intValue type=\"int\">42</intValue>\n");
            writer.write("  <longValue type=\"long\">1234567890123</longValue>\n");
            writer.write("  <doubleValue type=\"double\">3.14159</doubleValue>\n");
            writer.write("  <floatValue type=\"float\">2.718</floatValue>\n");
            writer.write("  <booleanValue type=\"boolean\">true</booleanValue>\n");
            writer.write("  <byteValue type=\"byte\">127</byteValue>\n");
            writer.write("  <shortValue type=\"short\">32767</shortValue>\n");
            writer.write("</config>\n");
        }

        Properties<String, Object> props = PropertiesUtil.loadFromXml(complexXml);

        Assertions.assertEquals("test string", props.get("stringValue"));
        Assertions.assertEquals(42, props.get("intValue"));
        Assertions.assertEquals(1234567890123L, props.get("longValue"));
        Assertions.assertEquals(3.14159, (Double) props.get("doubleValue"), 0.00001);
        Assertions.assertEquals(2.718f, (Float) props.get("floatValue"), 0.001f);
        Assertions.assertEquals(true, props.get("booleanValue"));
        Assertions.assertEquals((byte) 127, props.get("byteValue"));
        Assertions.assertEquals((short) 32767, props.get("shortValue"));
    }

    @Test
    public void testPropertiesWithSpecialCharacters() throws IOException {
        Properties<String, String> props = new Properties<>();
        props.put("key.with.dots", "value with spaces");
        props.put("key:with:colons", "value=with=equals");
        props.put("key\\with\\backslashes", "value\nwith\nnewlines");

        File outputFile = tempDir.resolve("special.properties").toFile();
        PropertiesUtil.store(props, "Special Characters Test", outputFile);

        // Load and verify
        Properties<String, String> loadedProps = PropertiesUtil.load(outputFile);
        Assertions.assertEquals("value with spaces", loadedProps.get("key.with.dots"));
        Assertions.assertEquals("value=with=equals", loadedProps.get("key:with:colons"));
        Assertions.assertTrue(loadedProps.get("key\\with\\backslashes").contains("with"));
    }

    @Test
    public void testXmlWithDuplicatedPropertyNames() throws IOException {
        // Create XML with duplicated property names (should throw exception)
        File duplicatedXml = tempDir.resolve("duplicated.xml").toFile();
        try (FileWriter writer = new FileWriter(duplicatedXml)) {
            writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            writer.write("<config>\n");
            writer.write("  <item>first</item>\n");
            writer.write("  <item>second</item>\n");
            writer.write("</config>\n");
        }

        Assertions.assertThrows(RuntimeException.class, () -> {
            PropertiesUtil.loadFromXml(duplicatedXml);
        });
    }

    @Test
    public void testXml2JavaWithDuplicatedPropertyNames() throws IOException {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<config>\n" + "  <item>first</item>\n" + "  <item>second</item>\n" + "</config>";

        String srcPath = tempDir.toString();
        String packageName = "com.test.generated";
        String className = "DuplicatedConfig";

        Assertions.assertThrows(RuntimeException.class, () -> {
            PropertiesUtil.xml2Java(xml, srcPath, packageName, className, false);
        });
    }

    @Test
    public void testStoreAndLoadConsistency() throws IOException {
        // Test that storing and loading maintains data consistency
        Properties<String, Object> originalProps = new Properties<>();
        originalProps.put("string", "value");
        originalProps.put("integer", 123);
        originalProps.put("long", 456L);
        originalProps.put("boolean", true);
        originalProps.put("double", 3.14);

        Properties<String, Object> nestedProps = new Properties<>();
        nestedProps.put("nested.string", "nested value");
        nestedProps.put("nested.int", 789);
        originalProps.put("nested", nestedProps);

        File xmlFile = tempDir.resolve("consistency.xml").toFile();
        PropertiesUtil.storeToXml(originalProps, "test", true, xmlFile);

        Properties<String, Object> loadedProps = PropertiesUtil.loadFromXml(xmlFile);

        Assertions.assertEquals(originalProps.get("string"), loadedProps.get("string"));
        Assertions.assertEquals(originalProps.get("integer"), loadedProps.get("integer"));
        Assertions.assertEquals(originalProps.get("long"), loadedProps.get("long"));
        Assertions.assertEquals(originalProps.get("boolean"), loadedProps.get("boolean"));
        Assertions.assertEquals(originalProps.get("double"), loadedProps.get("double"));

        Properties<String, Object> loadedNested = (Properties<String, Object>) loadedProps.get("nested");
        Properties<String, Object> originalNested = (Properties<String, Object>) originalProps.get("nested");
        Assertions.assertEquals(originalNested.get("nested.string"), loadedNested.get("nested.string"));
        Assertions.assertEquals(originalNested.get("nested.int"), loadedNested.get("nested.int"));
    }

    @Test
    public void testMultipleAutoRefreshProperties() throws IOException, InterruptedException {
        // Test multiple properties files with auto-refresh
        File file1 = tempDir.resolve("auto1.properties").toFile();
        File file2 = tempDir.resolve("auto2.properties").toFile();

        try (FileWriter writer = new FileWriter(file1)) {
            writer.write("file1.key=value1\n");
        }

        try (FileWriter writer = new FileWriter(file2)) {
            writer.write("file2.key=value2\n");
        }

        Properties<String, String> props1 = PropertiesUtil.load(file1, true);
        Properties<String, String> props2 = PropertiesUtil.load(file2, true);

        Assertions.assertEquals("value1", props1.get("file1.key"));
        Assertions.assertEquals("value2", props2.get("file2.key"));

        // Load same file again with auto-refresh should return same instance
        Properties<String, String> props1Again = PropertiesUtil.load(file1, true);
        Assertions.assertSame(props1, props1Again);
    }
}
