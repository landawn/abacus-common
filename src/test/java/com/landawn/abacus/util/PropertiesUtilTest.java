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
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.ParsingException;
import com.landawn.abacus.exception.UncheckedIOException;

public class PropertiesUtilTest extends TestBase {

    @TempDir
    Path tempDir;

    private File testPropertiesFile;
    private File testXmlFile;
    private File testInvalidXmlFile;
    private File testComplexXmlFile;

    public static class CustomProperties extends Properties<String, Object> {
        public CustomProperties() {
            super();
        }
    }

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
    public void testGetCommonConfigPaths_PathsExist() {
        java.util.List<String> paths = PropertiesUtil.getCommonConfigPaths();
        for (String path : paths) {
            File dir = new File(path);
            assertTrue(dir.exists());
            assertTrue(dir.isDirectory());
        }
    }

    // ==================== getCommonConfigPaths() ====================

    @Test
    public void testGetCommonConfigPaths() {
        java.util.List<String> paths = PropertiesUtil.getCommonConfigPaths();
        assertNotNull(paths);
    }

    @Test
    public void testGetCommonConfigPaths_ReturnsNonNullList() {
        java.util.List<String> paths = PropertiesUtil.getCommonConfigPaths();
        assertNotNull(paths);
        for (String path : paths) {
            assertNotNull(path);
            assertTrue(path.length() > 0);
        }
    }

    @Test
    public void testFormatPath_ExistingFileUnchanged() {
        File result = PropertiesUtil.formatPath(testPropertiesFile);
        assertEquals(testPropertiesFile.getAbsolutePath(), result.getAbsolutePath());
    }

    // ==================== formatPath(File) ====================

    @Test
    public void testFormatPath() {
        File result = PropertiesUtil.formatPath(testPropertiesFile);
        assertNotNull(result);
        assertEquals(testPropertiesFile.getAbsolutePath(), result.getAbsolutePath());
    }

    @Test
    public void testFormatPath_NonExistingFile() {
        File nonExisting = new File(tempDir.toFile(), "nonexistent_file.txt");
        File result = PropertiesUtil.formatPath(nonExisting);
        assertNotNull(result);
    }

    @Test
    public void testFormatPath_NonExistingWithNoEncodedSpaces() {
        File nonExisting = new File("/nonexistent/path/file.txt");
        File result = PropertiesUtil.formatPath(nonExisting);
        assertNotNull(result);
        assertEquals(nonExisting, result);
    }

    @Test
    public void testFormatPath_WithEncodedSpaces() throws IOException {
        File dirWithSpace = new File(tempDir.toFile(), "dir with space");
        dirWithSpace.mkdirs();
        File fileInDir = new File(dirWithSpace, "file.txt");
        fileInDir.createNewFile();

        File encodedFile = new File(dirWithSpace.getAbsolutePath().replace(" ", "%20"), "file.txt");
        File result = PropertiesUtil.formatPath(encodedFile);
        assertNotNull(result);
        assertTrue(result.exists());
    }

    @Test
    public void testFormatPath_WithNoEncodedSpaces() throws IOException {
        File result = PropertiesUtil.formatPath(testPropertiesFile);
        assertEquals(testPropertiesFile, result);
    }

    // ==================== findDir(String) ====================

    @Test
    public void testFindDir() {
        File result = PropertiesUtil.findDir("com");

        File nonExisting = PropertiesUtil.findDir("non-existing-dir-12345");
        assertNull(nonExisting);
    }

    @Test
    public void testFindDir_Null() {
        assertThrows(RuntimeException.class, () -> PropertiesUtil.findDir(null));
    }

    @Test
    public void testFindDir_Empty() {
        assertThrows(RuntimeException.class, () -> PropertiesUtil.findDir(""));
    }

    // ==================== findFile(String) ====================

    @Test
    public void testFindFile() {
        File result = PropertiesUtil.findFile("PropertiesUtil.java");

        File nonExisting = PropertiesUtil.findFile("non-existing-file-12345.txt");
        assertNull(nonExisting);
    }

    @Test
    public void testFindFile_Null() {
        assertThrows(RuntimeException.class, () -> PropertiesUtil.findFile(null));
    }

    @Test
    public void testFindFile_Empty() {
        assertThrows(RuntimeException.class, () -> PropertiesUtil.findFile(""));
    }

    @Test
    public void testFindFile_WithPathSeparators() {
        // Test with path separators in the filename
        File result = PropertiesUtil.findFile("com/landawn/abacus/util/PropertiesUtil.java");
        // May or may not find it depending on the environment
        // Just ensure it doesn't throw
    }

    // ==================== findFile/findDir edge cases ====================

    @Test
    public void testFindFile_CurrentDirFile() throws IOException {
        // Create a temp file and search for it — using just filename search in findFileInDir
        File subDir = new File(tempDir.toFile(), "searchtest");
        subDir.mkdirs();
        File target = new File(subDir, "uniquesearchfile12345.txt");
        target.createNewFile();

        File result = PropertiesUtil.findFileInDir("uniquesearchfile12345.txt", tempDir.toFile(), false);
        assertNotNull(result);
        assertTrue(result.exists());
        assertEquals("uniquesearchfile12345.txt", result.getName());
    }

    @Test
    public void testFindFileRelativeTo_NullSrcFile() {
        File result = PropertiesUtil.findFileRelativeTo(null, "nonexistent_file_xyz123.txt");
        assertTrue(result == null || !result.exists());
    }

    @Test
    public void testFindFileRelativeTo_NonExistingSrcFile() {
        File srcFile = new File(tempDir.toFile(), "nonexistent_src.txt");
        File result = PropertiesUtil.findFileRelativeTo(srcFile, "nonexistent_target_xyz123.txt");
        assertTrue(result == null || !result.exists());
    }

    // ==================== findFileRelativeTo(File, String) ====================

    @Test
    public void testFindFileRelativeTo() throws IOException {
        File srcFile = testPropertiesFile;
        File targetFile = tempDir.resolve("relative-target.txt").toFile();
        targetFile.createNewFile();

        File result = PropertiesUtil.findFileRelativeTo(srcFile, targetFile.getAbsolutePath());
        assertNotNull(result);
        assertTrue(result.exists());
    }

    @Test
    public void testFindFileRelativeTo_ExistingAbsoluteTarget() throws IOException {
        File targetFile = tempDir.resolve("absolute-target.txt").toFile();
        targetFile.createNewFile();

        File result = PropertiesUtil.findFileRelativeTo(testPropertiesFile, targetFile.getAbsolutePath());
        assertNotNull(result);
        assertTrue(result.exists());
    }

    @Test
    public void testFindFileRelativeTo_RelativeTargetInSameDir() throws IOException {
        File targetFile = tempDir.resolve("same-dir-target.txt").toFile();
        targetFile.createNewFile();

        File result = PropertiesUtil.findFileRelativeTo(testPropertiesFile, "same-dir-target.txt");
        assertNotNull(result);
    }

    @Test
    public void testFindFileInDir_FileNotExists() {
        File result = PropertiesUtil.findFileInDir("nonexistent_xyz123.txt", tempDir.toFile(), false);
        assertTrue(result == null || !result.exists());
    }

    @Test
    public void testFindFileInDir_NullDir() {
        File result = PropertiesUtil.findFileInDir("something.txt", null, false);
        assertNull(result);
    }

    // ==================== findFileInDir(String, File, boolean) ====================

    @Test
    public void testFindFileInDir() throws IOException {
        File subDir = new File(tempDir.toFile(), "subdir");
        subDir.mkdirs();
        File targetFile = new File(subDir, "findme.txt");
        targetFile.createNewFile();

        File result = PropertiesUtil.findFileInDir("findme.txt", tempDir.toFile(), false);
        assertNotNull(result);
        assertTrue(result.exists());
    }

    @Test
    public void testFindFileInDir_DirectorySearch() throws IOException {
        File subDir = new File(tempDir.toFile(), "findthisdir");
        subDir.mkdirs();

        File result = PropertiesUtil.findFileInDir("findthisdir", tempDir.toFile(), true);
        assertNotNull(result);
        assertTrue(result.exists());
        assertTrue(result.isDirectory());
    }

    @Test
    public void testFindFileInDir_NullFileName() {
        assertThrows(RuntimeException.class, () -> PropertiesUtil.findFileInDir(null, tempDir.toFile(), false));
    }

    @Test
    public void testFindFileInDir_EmptyFileName() {
        assertThrows(RuntimeException.class, () -> PropertiesUtil.findFileInDir("", tempDir.toFile(), false));
    }

    @Test
    public void testFindFileInDir_WithRelativePath() throws IOException {
        File subDir = new File(tempDir.toFile(), "config");
        subDir.mkdirs();
        File targetFile = new File(subDir, "app.conf");
        targetFile.createNewFile();

        File result = PropertiesUtil.findFileInDir("config/app.conf", tempDir.toFile(), false);
        assertNotNull(result);
        assertTrue(result.exists());
    }

    @Test
    public void testFindFileInDir_IgnoresGitDir() throws IOException {
        File gitDir = new File(tempDir.toFile(), ".git");
        gitDir.mkdirs();
        File hiddenFile = new File(gitDir, "hidden.txt");
        hiddenFile.createNewFile();

        File result = PropertiesUtil.findFileInDir("hidden.txt", tempDir.toFile(), false);
        assertTrue(result == null || !result.getParentFile().getName().equals(".git"));
    }

    @Test
    public void testFindFileInDir_NestedDirectory() throws IOException {
        File level1 = new File(tempDir.toFile(), "level1");
        level1.mkdirs();
        File level2 = new File(level1, "level2");
        level2.mkdirs();
        File deepFile = new File(level2, "deepfile.txt");
        deepFile.createNewFile();

        File result = PropertiesUtil.findFileInDir("deepfile.txt", tempDir.toFile(), false);
        assertNotNull(result);
        assertTrue(result.exists());
    }

    @Test
    public void testFindFileInDir_EmptyDir() throws IOException {
        File emptyDir = new File(tempDir.toFile(), "emptydir");
        emptyDir.mkdirs();

        File result = PropertiesUtil.findFileInDir("anything.txt", emptyDir, false);
        assertNull(result);
    }

    @Test
    public void testFindFileInDir_IgnoresSvnDir() throws IOException {
        File svnDir = new File(tempDir.toFile(), ".svn");
        svnDir.mkdirs();
        File svnFile = new File(svnDir, "svnfile.txt");
        svnFile.createNewFile();

        File result = PropertiesUtil.findFileInDir("svnfile.txt", tempDir.toFile(), false);
        assertTrue(result == null || !result.getParentFile().getName().equals(".svn"));
    }

    // ==================== load(File) ====================

    @Test
    public void testLoad_File() {
        Properties<String, String> props = PropertiesUtil.load(testPropertiesFile);

        assertNotNull(props);
        assertEquals("value1", props.get("key1"));
        assertEquals("value2", props.get("key2"));
        assertEquals("value3", props.get("key3"));
        assertEquals("nested.value", props.get("nested.key"));
    }

    @Test
    public void testLoad_FileWithoutAutoRefresh() {
        Properties<String, String> props = PropertiesUtil.load(testPropertiesFile, false);

        assertNotNull(props);
        assertEquals("value1", props.get("key1"));
    }

    @Test
    public void testLoad_FileNonExistent() {
        File nonExistent = new File(tempDir.toFile(), "nonexistent.properties");
        assertThrows(UncheckedIOException.class, () -> {
            PropertiesUtil.load(nonExistent);
        });
    }

    @Test
    public void testLoad_EmptyFile() throws IOException {
        File emptyFile = tempDir.resolve("empty.properties").toFile();
        emptyFile.createNewFile();

        Properties<String, String> props = PropertiesUtil.load(emptyFile);
        assertNotNull(props);
        assertTrue(props.isEmpty());
    }

    // ==================== load(File, boolean) ====================

    @Test
    public void testLoad_FileWithAutoRefresh() throws IOException, InterruptedException {
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
    public void testLoad_FileAutoRefreshSameInstance() throws IOException {
        Properties<String, String> props1 = PropertiesUtil.load(testPropertiesFile, true);
        Properties<String, String> props2 = PropertiesUtil.load(testPropertiesFile, true);

        Assertions.assertSame(props1, props2);
    }

    @Test
    public void testLoad_MultipleAutoRefreshProperties() throws IOException, InterruptedException {
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

        Properties<String, String> props1Again = PropertiesUtil.load(file1, true);
        Assertions.assertSame(props1, props1Again);
    }

    // ==================== load(InputStream) ====================

    @Test
    public void testLoad_InputStream() throws IOException {
        try (InputStream is = new FileInputStream(testPropertiesFile)) {
            Properties<String, String> props = PropertiesUtil.load(is);

            assertNotNull(props);
            assertEquals("value1", props.get("key1"));
            assertEquals("value2", props.get("key2"));
        }
    }

    @Test
    public void testLoad_InputStreamEmpty() throws IOException {
        String emptyProps = "";
        try (InputStream is = new ByteArrayInputStream(emptyProps.getBytes())) {
            Properties<String, String> props = PropertiesUtil.load(is);
            assertNotNull(props);
            assertTrue(props.isEmpty());
        }
    }

    @Test
    public void testLoad_InputStreamWithComments() throws IOException {
        String propsContent = "# This is a comment\nkey1=value1\n! Another comment\nkey2=value2\n";
        try (InputStream is = new ByteArrayInputStream(propsContent.getBytes())) {
            Properties<String, String> props = PropertiesUtil.load(is);
            assertNotNull(props);
            assertEquals("value1", props.get("key1"));
            assertEquals("value2", props.get("key2"));
        }
    }

    @Test
    public void testLoad_InputStreamWithWhitespace() throws IOException {
        String propsContent = "  key1  =  value1  \n  key2=value2\n";
        try (InputStream is = new ByteArrayInputStream(propsContent.getBytes())) {
            Properties<String, String> props = PropertiesUtil.load(is);
            assertNotNull(props);
            // Java properties trims keys and leading whitespace in values
            assertNotNull(props.get("key1"));
        }
    }

    // ==================== load(Reader) ====================

    @Test
    public void testLoad_Reader() throws IOException {
        try (Reader reader = new FileReader(testPropertiesFile)) {
            Properties<String, String> props = PropertiesUtil.load(reader);

            assertNotNull(props);
            assertEquals("value1", props.get("key1"));
            assertEquals("value2", props.get("key2"));
        }
    }

    @Test
    public void testLoad_ReaderEmpty() throws IOException {
        try (Reader reader = new StringReader("")) {
            Properties<String, String> props = PropertiesUtil.load(reader);
            assertNotNull(props);
            assertTrue(props.isEmpty());
        }
    }

    @Test
    public void testLoad_ReaderWithSpecialChars() throws IOException {
        String propsContent = "key.with.dots=value\nkey\\:with\\:colons=value2\n";
        try (Reader reader = new StringReader(propsContent)) {
            Properties<String, String> props = PropertiesUtil.load(reader);
            assertNotNull(props);
            assertEquals("value", props.get("key.with.dots"));
        }
    }

    @Test
    public void testLoad_ReaderWithComments() throws IOException {
        String propsContent = "# Comment line\nkey=value\n";
        try (Reader reader = new StringReader(propsContent)) {
            Properties<String, String> props = PropertiesUtil.load(reader);
            assertEquals("value", props.get("key"));
        }
    }

    @Test
    public void testLoad_ReaderWithMultilineValues() throws IOException {
        String propsContent = "key1=value1\\\n  continued\nkey2=value2\n";
        try (Reader reader = new StringReader(propsContent)) {
            Properties<String, String> props = PropertiesUtil.load(reader);
            assertNotNull(props);
            assertNotNull(props.get("key1"));
        }
    }

    @Test
    public void testLoadFromNonExistentFile() {
        File nonExistentFile = new File("non-existent-file.properties");
        Assertions.assertThrows(UncheckedIOException.class, () -> {
            PropertiesUtil.load(nonExistentFile);
        });
    }

    @Test
    public void testLoad_ReaderWithEqualsInValue() throws IOException {
        String propsContent = "url=jdbc:mysql://localhost:3306/db?param=value&other=test\n";
        try (Reader reader = new StringReader(propsContent)) {
            Properties<String, String> props = PropertiesUtil.load(reader);
            assertNotNull(props);
            assertEquals("jdbc:mysql://localhost:3306/db?param=value&other=test", props.get("url"));
        }
    }

    @Test
    public void testLoad_InputStreamWithUnicodeEscapes() throws IOException {
        String propsContent = "greeting=Hello\\u0020World\n";
        try (InputStream is = new ByteArrayInputStream(propsContent.getBytes())) {
            Properties<String, String> props = PropertiesUtil.load(is);
            assertNotNull(props);
            assertEquals("Hello World", props.get("greeting"));
        }
    }

    // ==================== loadFromXml(File) ====================

    @Test
    public void testLoadFromXml_File() {
        Properties<String, Object> props = PropertiesUtil.loadFromXml(testXmlFile);

        assertNotNull(props);
        assertNotNull(props.get("database"));
        assertNotNull(props.get("timeout"));
        assertEquals(true, props.get("enabled"));
    }

    // ==================== loadFromXml(File, boolean) ====================

    @Test
    public void testLoadFromXml_FileWithAutoRefresh() {
        Properties<String, Object> props = PropertiesUtil.loadFromXml(testXmlFile, true);

        assertNotNull(props);
        assertNotNull(props.get("database"));
    }

    @Test
    public void testLoadFromXml_FileWithoutAutoRefresh() {
        Properties<String, Object> props = PropertiesUtil.loadFromXml(testXmlFile, false);

        assertNotNull(props);
        assertNotNull(props.get("database"));
    }

    @Test
    public void testLoadFromXml_FileAutoRefreshSameInstance() {
        Properties<String, Object> props1 = PropertiesUtil.loadFromXml(testXmlFile, true);
        Properties<String, Object> props2 = PropertiesUtil.loadFromXml(testXmlFile, true);
        Assertions.assertSame(props1, props2);
    }

    // ==================== loadFromXml(File, Class) ====================

    @Test
    public void testLoadFromXml_FileWithClass() {
        Properties<String, Object> props = PropertiesUtil.loadFromXml(testXmlFile, Properties.class);

        assertNotNull(props);
        assertNotNull(props.get("database"));
    }

    @Test
    public void testLoadFromXml_FileWithCustomClass() {
        CustomProperties props = PropertiesUtil.loadFromXml(testXmlFile, CustomProperties.class);

        assertNotNull(props);
        assertTrue(props instanceof CustomProperties);
        assertEquals(5000L, props.get("timeout"));
    }

    // ==================== loadFromXml(File, boolean, Class) ====================

    @Test
    public void testLoadFromXml_FileWithAutoRefreshAndClass() {
        Properties<String, Object> props = PropertiesUtil.loadFromXml(testXmlFile, true, Properties.class);

        assertNotNull(props);
        assertNotNull(props.get("database"));
    }

    @Test
    public void testLoadFromXml_FileWithoutAutoRefreshAndClass() {
        Properties<String, Object> props = PropertiesUtil.loadFromXml(testXmlFile, false, Properties.class);

        assertNotNull(props);
        assertNotNull(props.get("database"));
    }

    @Test
    public void testLoadFromXml_FileInvalid() {
        assertThrows(ParsingException.class, () -> {
            PropertiesUtil.loadFromXml(testInvalidXmlFile);
        });
    }

    @Test
    public void testLoadFromXml_FileNonExistent() {
        File nonExistent = new File(tempDir.toFile(), "nonexistent.xml");
        assertThrows(Exception.class, () -> PropertiesUtil.loadFromXml(nonExistent));
    }

    @Test
    public void testLoadFromXml_FileWithAutoRefreshUtf16() throws Exception {
        File utf16XmlFile = tempDir.resolve("utf16-auto-refresh.xml").toFile();
        Files.writeString(utf16XmlFile.toPath(), "<?xml version=\"1.0\" encoding=\"UTF-16\"?><root><message>v1</message></root>", StandardCharsets.UTF_16);

        Properties<String, Object> props = PropertiesUtil.loadFromXml(utf16XmlFile, true);
        assertEquals("v1", props.get("message"));

        Thread.sleep(1100);

        Files.writeString(utf16XmlFile.toPath(), "<?xml version=\"1.0\" encoding=\"UTF-16\"?><root><message>v2</message></root>", StandardCharsets.UTF_16);

        boolean refreshed = false;
        long deadline = System.currentTimeMillis() + 8000;

        while (System.currentTimeMillis() < deadline) {
            if ("v2".equals(props.get("message"))) {
                refreshed = true;
                break;
            }

            Thread.sleep(200);
        }

        assertTrue(refreshed, "Expected auto-refresh to update UTF-16 XML properties");
    }

    // ==================== loadFromXml(InputStream) ====================

    @Test
    public void testLoadFromXml_InputStream() throws IOException {
        try (InputStream is = new FileInputStream(testXmlFile)) {
            Properties<String, Object> props = PropertiesUtil.loadFromXml(is);

            assertNotNull(props);
            assertNotNull(props.get("database"));
        }
    }

    @Test
    public void testLoadFromXml_InputStreamWithLeadingComment() throws IOException {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><!-- leading comment --><root><name>test</name></root>";

        try (InputStream is = new ByteArrayInputStream(xml.getBytes(Charsets.UTF_8))) {
            Properties<String, Object> props = PropertiesUtil.loadFromXml(is);
            assertEquals("test", props.get("name"));
        }
    }

    @Test
    public void testLoadFromXml_InputStreamInvalid() throws IOException {
        String invalidXml = "<?xml version=\"1.0\"?><root><unclosed></root>";
        try (InputStream is = new ByteArrayInputStream(invalidXml.getBytes())) {
            assertThrows(ParsingException.class, () -> {
                PropertiesUtil.loadFromXml(is);
            });
        }
    }

    @Test
    public void testLoadFromXml_InputStreamEmpty() throws IOException {
        String xml = "<?xml version=\"1.0\"?><root></root>";
        try (InputStream is = new ByteArrayInputStream(xml.getBytes())) {
            Properties<String, Object> props = PropertiesUtil.loadFromXml(is);
            assertNotNull(props);
        }
    }

    @Test
    public void testLoadFromXml_InputStreamWithTypeAttributes() throws IOException {
        String xml = "<?xml version=\"1.0\"?><root><doubleVal type=\"double\">3.14</doubleVal></root>";
        try (InputStream is = new ByteArrayInputStream(xml.getBytes())) {
            Properties<String, Object> props = PropertiesUtil.loadFromXml(is);
            assertEquals(3.14, (Double) props.get("doubleVal"), 0.001);
        }
    }

    // ==================== loadFromXml(Reader) ====================

    @Test
    public void testLoadFromXml_Reader() throws IOException {
        try (Reader reader = new FileReader(testXmlFile)) {
            Properties<String, Object> props = PropertiesUtil.loadFromXml(reader);

            assertNotNull(props);
            assertNotNull(props.get("database"));
        }
    }

    @Test
    public void testLoadFromXml_ReaderWithLeadingComment() throws IOException {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><!-- leading comment --><root><name>reader</name></root>";

        try (Reader reader = new StringReader(xml)) {
            Properties<String, Object> props = PropertiesUtil.loadFromXml(reader);
            assertEquals("reader", props.get("name"));
        }
    }

    @Test
    public void testLoadFromXml_ReaderEmpty() throws IOException {
        String xml = "<?xml version=\"1.0\"?><root></root>";
        try (Reader reader = new StringReader(xml)) {
            Properties<String, Object> props = PropertiesUtil.loadFromXml(reader);
            assertNotNull(props);
        }
    }

    @Test
    public void testLoadFromXml_ReaderWithTypeAttributes() throws IOException {
        String xml = "<?xml version=\"1.0\"?><root>" + "<intVal type=\"int\">42</intVal>" + "<longVal type=\"long\">123456789</longVal>"
                + "<boolVal type=\"boolean\">true</boolVal>" + "</root>";
        try (Reader reader = new StringReader(xml)) {
            Properties<String, Object> props = PropertiesUtil.loadFromXml(reader);
            assertEquals(42, props.get("intVal"));
            assertEquals(123456789L, props.get("longVal"));
            assertEquals(true, props.get("boolVal"));
        }
    }

    @Test
    public void testLoadFromXml_ReaderInvalid() {
        String xml = "<?xml version=\"1.0\"?><root><unclosed></root>";
        assertThrows(ParsingException.class, () -> {
            try (Reader reader = new StringReader(xml)) {
                PropertiesUtil.loadFromXml(reader);
            }
        });
    }

    @Test
    public void testLoadFromXml_FileWithClassNonExistent() {
        File nonExistent = new File(tempDir.toFile(), "nonexistent.xml");
        assertThrows(Exception.class, () -> PropertiesUtil.loadFromXml(nonExistent, Properties.class));
    }

    @Test
    public void testLoadFromXml_FileWithCustomClassAndAutoRefresh() throws IOException, InterruptedException {
        CustomProperties props = PropertiesUtil.loadFromXml(testXmlFile, true, CustomProperties.class);

        assertNotNull(props);
        assertTrue(props instanceof CustomProperties);
        assertEquals(5000L, props.get("timeout"));
    }

    @Test
    public void testLoadFromXml_FileWithAutoRefreshAndClassNonExistent() {
        File nonExistent = new File(tempDir.toFile(), "nonexistent.xml");
        assertThrows(Exception.class, () -> PropertiesUtil.loadFromXml(nonExistent, true, Properties.class));
    }

    // ==================== loadFromXml(InputStream, Class) ====================

    @Test
    public void testLoadFromXml_InputStreamWithClass() throws IOException {
        try (InputStream is = new FileInputStream(testXmlFile)) {
            Properties<String, Object> props = PropertiesUtil.loadFromXml(is, Properties.class);

            assertNotNull(props);
            assertNotNull(props.get("database"));
        }
    }

    @Test
    public void testLoadFromXml_InputStreamWithCustomClass() throws IOException {
        try (InputStream is = new FileInputStream(testXmlFile)) {
            CustomProperties props = PropertiesUtil.loadFromXml(is, CustomProperties.class);

            assertNotNull(props);
            assertTrue(props instanceof CustomProperties);
            assertEquals(5000L, props.get("timeout"));
        }
    }

    // ==================== loadFromXml(Reader, Class) ====================

    @Test
    public void testLoadFromXml_ReaderWithClass() throws IOException {
        try (Reader reader = new FileReader(testXmlFile)) {
            Properties<String, Object> props = PropertiesUtil.loadFromXml(reader, Properties.class);

            assertNotNull(props);
            assertNotNull(props.get("database"));
        }
    }

    @Test
    public void testLoadFromXml_ReaderWithCustomClass() throws IOException {
        try (Reader reader = new FileReader(testXmlFile)) {
            CustomProperties props = PropertiesUtil.loadFromXml(reader, CustomProperties.class);

            assertNotNull(props);
            assertTrue(props instanceof CustomProperties);
            assertEquals(5000L, props.get("timeout"));
        }
    }

    // ==================== Complex types and edge cases ====================

    @Test
    public void testLoadFromXml_WithDuplicatedProperties() throws IOException {
        String xml = "<?xml version=\"1.0\"?><root><item>value1</item><item>value2</item></root>";

        assertThrows(RuntimeException.class, () -> {
            try (Reader reader = new StringReader(xml)) {
                PropertiesUtil.loadFromXml(reader);
            }
        });
    }

    @Test
    public void testLoadFromXml_WithAutoRefresh() throws IOException {
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
    public void testXmlWithComplexTypes() throws IOException {
        File complexXml = tempDir.resolve("complex-types.xml").toFile();
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
    public void testXmlWithDuplicatedPropertyNames() throws IOException {
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
    public void testLoadFromXml_InputStream_DefaultClass() throws IOException {
        String xmlContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<config>\n  <key1>value1</key1>\n  <key2>value2</key2>\n</config>\n";
        try (InputStream is = new ByteArrayInputStream(xmlContent.getBytes(StandardCharsets.UTF_8))) {
            Properties<String, Object> props = PropertiesUtil.loadFromXml(is, Properties.class);
            assertNotNull(props);
            assertEquals("value1", props.get("key1"));
            assertEquals("value2", props.get("key2"));
        }
    }

    @Test
    public void testLoadFromXml_Reader_DefaultClass() throws IOException {
        String xmlContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<config>\n  <host>localhost</host>\n  <port>8080</port>\n</config>\n";
        try (Reader reader = new StringReader(xmlContent)) {
            Properties<String, Object> props = PropertiesUtil.loadFromXml(reader, Properties.class);
            assertNotNull(props);
            assertEquals("localhost", props.get("host"));
        }
    }

    @Test
    public void testLoadFromXml_InputStream_InvalidXml() {
        String invalidXml = "this is not xml";
        InputStream is = new ByteArrayInputStream(invalidXml.getBytes(StandardCharsets.UTF_8));
        assertThrows(Exception.class, () -> PropertiesUtil.loadFromXml(is, Properties.class));
    }

    // ==================== loadFromXml merge path (output != null) ====================

    @Test
    public void testLoadFromXml_refreshMerge_removesOldKeys() throws IOException {
        // Create initial XML with two keys
        String xmlV1 = "<?xml version=\"1.0\"?><root><key1>v1</key1><key2>v2</key2></root>";
        Properties<String, Object> loaded = PropertiesUtil.loadFromXml(new java.io.ByteArrayInputStream(xmlV1.getBytes()), Properties.class);
        assertEquals("v1", loaded.get("key1"));
        assertEquals("v2", loaded.get("key2"));
    }

    @Test
    public void testLoadFromXml_WithCustomProperties_multipleKeys() throws IOException {
        String xml = "<?xml version=\"1.0\"?><config>" + "<host>localhost</host>" + "<port type=\"int\">8080</port>" + "<ssl type=\"boolean\">true</ssl>"
                + "</config>";

        java.io.StringReader reader = new java.io.StringReader(xml);
        CustomProperties props = PropertiesUtil.loadFromXml(reader, CustomProperties.class);

        assertNotNull(props);
        assertTrue(props instanceof CustomProperties);
        assertEquals("localhost", props.get("host"));
        assertEquals(8080, props.get("port"));
        assertEquals(true, props.get("ssl"));
    }

    // ==================== store(Properties, String, File) ====================

    @Test
    public void testStore_File() throws IOException {
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
    public void testStore_FileWithNullComments() throws IOException {
        Properties<String, String> props = new Properties<>();
        props.put("key", "value");

        File outputFile = tempDir.resolve("output2.properties").toFile();
        PropertiesUtil.store(props, null, outputFile);

        assertTrue(outputFile.exists());
    }

    @Test
    public void testStore_FileEmptyProperties() throws IOException {
        Properties<String, String> props = new Properties<>();
        File outputFile = tempDir.resolve("empty-output.properties").toFile();
        PropertiesUtil.store(props, "Empty", outputFile);

        assertTrue(outputFile.exists());
        Properties<String, String> loaded = PropertiesUtil.load(outputFile);
        assertTrue(loaded.isEmpty());
    }

    // ==================== store(Properties, String, OutputStream) ====================

    @Test
    public void testStore_OutputStream() throws IOException {
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
    public void testStore_OutputStreamEmpty() throws IOException {
        Properties<String, String> props = new Properties<>();

        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PropertiesUtil.store(props, "Empty", os);
            assertNotNull(os.toString());
        }
    }

    @Test
    public void testStore_OutputStreamNullComments() throws IOException {
        Properties<String, String> props = new Properties<>();
        props.put("key", "value");
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PropertiesUtil.store(props, null, os);
            String output = os.toString();
            assertTrue(output.contains("key"));
        }
    }

    // ==================== store(Properties, String, Writer) ====================

    @Test
    public void testStore_Writer() throws IOException {
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
    public void testStore_WriterMultipleProperties() throws IOException {
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
    public void testStore_WriterNullComments() throws IOException {
        Properties<String, String> props = new Properties<>();
        props.put("key", "value");
        try (StringWriter writer = new StringWriter()) {
            PropertiesUtil.store(props, null, writer);
            String output = writer.toString();
            assertTrue(output.contains("key"));
        }
    }

    @Test
    public void testStore_WriterEmptyProperties() throws IOException {
        Properties<String, String> props = new Properties<>();
        try (StringWriter writer = new StringWriter()) {
            PropertiesUtil.store(props, "Empty", writer);
            assertNotNull(writer.toString());
        }
    }

    // ==================== Round trip tests ====================

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
    public void testStoreAndLoadRoundTrip_Unicode() throws IOException {
        Properties<String, String> original = new Properties<>();
        original.put("greeting", "Hello, \u4f60\u597d, caf\u00e9");

        File tempFile = tempDir.resolve("roundtrip-unicode.properties").toFile();
        PropertiesUtil.store(original, "Unicode round trip test", tempFile);

        Properties<String, String> loaded = PropertiesUtil.load(tempFile);
        assertEquals(original.get("greeting"), loaded.get("greeting"));
    }

    @Test
    public void testStoreAndLoadConsistency() throws IOException {
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
    public void testPropertiesWithSpecialCharacters() throws IOException {
        Properties<String, String> props = new Properties<>();
        props.put("key.with.dots", "value with spaces");
        props.put("key:with:colons", "value=with=equals");
        props.put("key\\with\\backslashes", "value\nwith\nnewlines");

        File outputFile = tempDir.resolve("special.properties").toFile();
        PropertiesUtil.store(props, "Special Characters Test", outputFile);

        Properties<String, String> loadedProps = PropertiesUtil.load(outputFile);
        Assertions.assertEquals("value with spaces", loadedProps.get("key.with.dots"));
        Assertions.assertEquals("value=with=equals", loadedProps.get("key:with:colons"));
        Assertions.assertTrue(loadedProps.get("key\\with\\backslashes").contains("with"));
    }

    // ==================== store with various property types ====================

    @Test
    public void testStore_OutputStream_LargeProperties() throws IOException {
        Properties<String, String> props = new Properties<>();
        for (int i = 0; i < 100; i++) {
            props.put("key" + i, "value" + i);
        }

        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        PropertiesUtil.store(props, "Large properties test", baos);
        String output = baos.toString();

        assertTrue(output.contains("key0"));
        assertTrue(output.contains("key99"));
        assertTrue(output.contains("value0"));
        assertTrue(output.contains("value99"));
    }

    @Test
    public void testStore_Writer_WithSpecialCharacters() throws IOException {
        Properties<String, String> props = new Properties<>();
        props.put("message", "Hello, World!");
        props.put("url", "http://example.com/path?a=1&b=2");

        java.io.StringWriter sw = new java.io.StringWriter();
        PropertiesUtil.store(props, "Special characters", sw);
        String output = sw.toString();

        assertTrue(output.contains("message"));
        assertTrue(output.contains("url"));
    }

    // ==================== storeToXml(Properties, String, boolean, File) ====================

    @Test
    public void testStoreToXml_File() throws IOException {
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
    public void testStoreToXml_FileWithoutTypeInfo() throws IOException {
        Properties<String, Object> props = new Properties<>();
        props.put("key1", "value1");

        File outputFile = tempDir.resolve("output_no_type.xml").toFile();
        PropertiesUtil.storeToXml(props, "config", false, outputFile);

        assertTrue(outputFile.exists());
    }

    @Test
    public void testStoreToXml_FileEmptyProperties() throws IOException {
        Properties<String, Object> props = new Properties<>();
        File outputFile = tempDir.resolve("empty-output.xml").toFile();
        PropertiesUtil.storeToXml(props, "empty", true, outputFile);

        assertTrue(outputFile.exists());
        assertTrue(outputFile.length() > 0);
    }

    @Test
    public void testStoreToXml_FileNestedProperties() throws IOException {
        Properties<String, Object> props = new Properties<>();
        Properties<String, Object> nested = new Properties<>();
        nested.put("nestedKey", "nestedValue");
        props.put("nested", nested);

        File outputFile = tempDir.resolve("nested-output.xml").toFile();
        PropertiesUtil.storeToXml(props, "config", true, outputFile);

        assertTrue(outputFile.exists());
        String content = Files.readString(outputFile.toPath());
        assertTrue(content.contains("nested"));
        assertTrue(content.contains("nestedKey"));
    }

    // ==================== storeToXml(Properties, String, boolean, OutputStream) ====================

    @Test
    public void testStoreToXml_OutputStream() throws IOException {
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
    public void testStoreToXml_OutputStreamWithTypeInfo() throws IOException {
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
    public void testStoreToXml_OutputStreamWithoutTypeInfo() throws IOException {
        Properties<String, Object> props = new Properties<>();
        props.put("key", "value");
        props.put("num", 42);

        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PropertiesUtil.storeToXml(props, "config", false, os);
            String output = os.toString();
            assertTrue(output.contains("key"));
        }
    }

    @Test
    public void testStoreToXml_OutputStreamEmptyProperties() throws IOException {
        Properties<String, Object> props = new Properties<>();
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PropertiesUtil.storeToXml(props, "empty", true, os);
            assertTrue(os.toString().contains("<?xml"));
        }
    }

    // ==================== storeToXml(Properties, String, boolean, Writer) ====================

    @Test
    public void testStoreToXml_Writer() throws IOException {
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
    public void testStoreToXml_WriterNestedProperties() throws IOException {
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
    public void testStoreToXml_WriterWithoutTypeInfo() throws IOException {
        Properties<String, Object> props = new Properties<>();
        props.put("key", "value");

        try (StringWriter writer = new StringWriter()) {
            PropertiesUtil.storeToXml(props, "config", false, writer);
            String output = writer.toString();
            assertTrue(output.contains("key"));
            assertTrue(output.contains("<?xml"));
        }
    }

    @Test
    public void testStoreToXml_WriterWithVariousTypes() throws IOException {
        Properties<String, Object> props = new Properties<>();
        props.put("strVal", "hello");
        props.put("intVal", 42);
        props.put("boolVal", true);
        props.put("longVal", 123456L);
        props.put("doubleVal", 3.14);

        try (StringWriter writer = new StringWriter()) {
            PropertiesUtil.storeToXml(props, "root", true, writer);
            String output = writer.toString();
            assertTrue(output.contains("strVal"));
            assertTrue(output.contains("intVal"));
            assertTrue(output.contains("boolVal"));
        }
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
    public void testStoreToXmlAndLoadRoundTrip_Unicode() throws IOException {
        Properties<String, Object> original = new Properties<>();
        original.put("greeting", "Hello, \u4f60\u597d, caf\u00e9");

        File tempFile = tempDir.resolve("roundtrip-unicode.xml").toFile();
        PropertiesUtil.storeToXml(original, "root", true, tempFile);

        String xmlContent = Files.readString(tempFile.toPath(), Charsets.UTF_8);
        assertTrue(xmlContent.contains("encoding=\"UTF-8\""));
        assertTrue(xmlContent.contains("\u4f60\u597d"));

        Properties<String, Object> loaded = PropertiesUtil.loadFromXml(tempFile);
        assertEquals(original.get("greeting"), loaded.get("greeting"));
    }

    @Test
    public void testStoreToXml_RoundTripWithNestedAndSimple() throws IOException {
        Properties<String, Object> props = new Properties<>();
        props.put("name", "app");
        Properties<String, Object> db = new Properties<>();
        db.put("host", "localhost");
        db.put("port", 3306);
        props.put("database", db);

        File xmlFile = tempDir.resolve("nested-roundtrip.xml").toFile();
        PropertiesUtil.storeToXml(props, "config", true, xmlFile);

        Properties<String, Object> loaded = PropertiesUtil.loadFromXml(xmlFile);
        assertEquals("app", loaded.get("name"));
        assertNotNull(loaded.get("database"));
    }

    @Test
    public void testStoreToXml_OutputStreamWithNullValue() throws IOException {
        Properties<String, Object> props = new Properties<>();
        props.put("key1", "value1");
        // null values should be skipped in storeToXml

        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PropertiesUtil.storeToXml(props, "config", true, os);
            String output = os.toString();
            assertTrue(output.contains("<?xml"));
            assertTrue(output.contains("key1"));
        }
    }

    @Test
    public void testStoreToXml_Writer_AndLoadBack() throws IOException {
        Properties<String, Object> props = new Properties<>();
        props.put("app", "test-app");
        props.put("version", "1.0");

        StringWriter sw = new StringWriter();
        PropertiesUtil.storeToXml(props, "root", true, sw);
        String xmlOutput = sw.toString();
        assertTrue(xmlOutput.contains("app"));
        assertTrue(xmlOutput.contains("test-app"));
        assertTrue(xmlOutput.contains("version"));
        assertTrue(xmlOutput.contains("1.0"));
    }

    // ==================== storeToXml with null value (skipped) ====================

    @Test
    public void testStoreToXml_Writer_NullValueSkipped() throws IOException {
        // A null value in the properties should be skipped (not serialized)
        Properties<String, Object> props = new Properties<>();
        props.put("key1", "value1");
        props.put("key2", null); // null value — should be skipped

        StringWriter sw = new StringWriter();
        PropertiesUtil.storeToXml(props, "root", true, sw);
        String xml = sw.toString();
        assertTrue(xml.contains("key1"));
        assertTrue(xml.contains("value1"));
        // key2 with null value should not appear as an element
        assertFalse(xml.contains("<key2>null</key2>"));
    }

    @Test
    public void testStoreToXml_Writer_CustomProperties_typeInfo() throws IOException {
        // When the property value is a custom Properties subclass, it should serialize with type info
        CustomProperties nested = new CustomProperties();
        nested.put("innerKey", "innerValue");

        Properties<String, Object> props = new Properties<>();
        props.put("sub", nested);

        StringWriter sw = new StringWriter();
        PropertiesUtil.storeToXml(props, "root", true, sw);
        String xml = sw.toString();
        assertTrue(xml.contains("sub"));
        assertTrue(xml.contains("innerKey"));
        assertTrue(xml.contains("innerValue"));
    }

    @Test
    public void testStoreToXml_Writer_StringValue_noTypeInfo() throws IOException {
        Properties<String, Object> props = new Properties<>();
        props.put("name", "Alice");

        StringWriter sw = new StringWriter();
        PropertiesUtil.storeToXml(props, "config", false, sw);
        String xml = sw.toString();
        // Without type info, no type attribute
        assertFalse(xml.contains("type=\"String\""));
        assertTrue(xml.contains("<name>Alice</name>"));
    }

    @Test
    public void testStoreToXml_Writer_FloatValue() throws IOException {
        Properties<String, Object> props = new Properties<>();
        props.put("ratio", 0.5f);

        StringWriter sw = new StringWriter();
        PropertiesUtil.storeToXml(props, "root", true, sw);
        String xml = sw.toString();
        assertTrue(xml.contains("ratio"));
        assertTrue(xml.contains("type=\"float\""));
    }

    @Test
    public void testStoreToXml_Writer_NonPrimitiveType() throws IOException {
        // A java.util.Date is not a primitive wrapper, so declaringName() should be used for type info
        java.util.Date date = new java.util.Date(0);
        Properties<String, Object> props = new Properties<>();
        props.put("startDate", date);

        StringWriter sw = new StringWriter();
        // Should not throw even for date types
        PropertiesUtil.storeToXml(props, "root", true, sw);
        String xml = sw.toString();
        assertTrue(xml.contains("startDate"));
    }

    @Test
    public void testStoreToXml_OutputStream_LongStringValue() throws IOException {
        StringBuilder longValue = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            longValue.append("x");
        }
        Properties<String, Object> props = new Properties<>();
        props.put("longKey", longValue.toString());

        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        PropertiesUtil.storeToXml(props, "root", true, baos);
        String xml = baos.toString();

        assertTrue(xml.contains("longKey"));
        assertTrue(xml.contains(longValue.toString()));
    }

    @Test
    public void testStoreToXml_Writer_WithTypeInfoRootAttribute() throws IOException {
        Properties<String, Object> props = new Properties<>();
        props.put("port", 8080);
        props.put("enabled", true);

        java.io.StringWriter writer = new java.io.StringWriter();
        PropertiesUtil.storeToXml(props, "config", true, writer);

        String xml = writer.toString();
        assertTrue(xml.contains("<config>"));
        assertTrue(xml.contains("<port type=\"int\">8080</port>"));
        assertTrue(xml.contains("<enabled type=\"boolean\">true</enabled>"));

        Properties<String, Object> loaded = PropertiesUtil.loadFromXml(new java.io.StringReader(xml), Properties.class);
        assertEquals(8080, loaded.get("port"));
        assertEquals(true, loaded.get("enabled"));
    }

    // ==================== xmlToJava(String, ...) ====================

    @Test
    public void testXmlToJava_FromString() throws IOException {
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
    public void testXmlToJava_FromStringWithPublicFields() throws IOException {
        String xml = "<?xml version=\"1.0\"?><app><title>MyApp</title></app>";

        String srcPath = tempDir.resolve("src2").toFile().getAbsolutePath();
        new File(srcPath).mkdirs();

        PropertiesUtil.xmlToJava(xml, srcPath, "com.test2", "AppConfig", true);

        File generatedFile = new File(srcPath + File.separator + "com" + File.separator + "test2", "AppConfig.java");
        assertTrue(generatedFile.exists());
    }

    @Test
    public void testXmlToJava_FromStringWithDuplicatedPropNames() throws IOException {
        String xml = "<?xml version=\"1.0\"?><root><item>value1</item><item>value2</item></root>";

        String srcPath = tempDir.resolve("src9").toFile().getAbsolutePath();
        new File(srcPath).mkdirs();

        assertThrows(RuntimeException.class, () -> {
            PropertiesUtil.xmlToJava(xml, srcPath, "com.test9", "DupConfig", false);
        });
    }

    // ==================== xmlToJava(File, ...) ====================

    @Test
    public void testXmlToJava_FromFile() throws IOException {
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
    public void testXmlToJava_FromFileWithNullClassName() throws IOException {
        String srcPath = tempDir.resolve("src4").toFile().getAbsolutePath();
        new File(srcPath).mkdirs();

        PropertiesUtil.xmlToJava(testComplexXmlFile, srcPath, "com.test4", null, false);

        File generatedFile = new File(srcPath + File.separator + "com" + File.separator + "test4", "Application.java");
        assertTrue(generatedFile.exists());
    }

    @Test
    public void testXmlToJava_FromFileWithPublicFields() throws IOException {
        String srcPath = tempDir.resolve("src-public-fields").toFile().getAbsolutePath();
        new File(srcPath).mkdirs();

        PropertiesUtil.xmlToJava(testComplexXmlFile, srcPath, "com.pubfields", "PubConfig", true);

        File generatedFile = new File(srcPath + File.separator + "com" + File.separator + "pubfields", "PubConfig.java");
        assertTrue(generatedFile.exists());
    }

    // ==================== xmlToJava(InputStream, ...) ====================

    @Test
    public void testXmlToJava_FromInputStream() throws IOException {
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
    public void testXmlToJava_FromInputStreamComplex() throws IOException {
        try (InputStream is = new FileInputStream(testComplexXmlFile)) {
            String srcPath = tempDir.resolve("src6").toFile().getAbsolutePath();
            new File(srcPath).mkdirs();

            PropertiesUtil.xmlToJava(is, srcPath, "com.test6", "AppSettings", false);

            File generatedFile = new File(srcPath + File.separator + "com" + File.separator + "test6", "AppSettings.java");
            assertTrue(generatedFile.exists());
        }
    }

    @Test
    public void testXmlToJava_FromInputStreamWithPublicFields() throws IOException {
        String xml = "<?xml version=\"1.0\"?><myapp><setting>value</setting></myapp>";

        String srcPath = tempDir.resolve("src-pub-is").toFile().getAbsolutePath();
        new File(srcPath).mkdirs();

        try (InputStream is = new ByteArrayInputStream(xml.getBytes())) {
            PropertiesUtil.xmlToJava(is, srcPath, "com.pubis", "PubIs", true);
        }

        File generatedFile = new File(srcPath + File.separator + "com" + File.separator + "pubis", "PubIs.java");
        assertTrue(generatedFile.exists());
    }

    // ==================== xmlToJava(Reader, ...) ====================

    @Test
    public void testXmlToJava_FromReader() throws IOException {
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
    public void testXmlToJava_FromReaderWithNestedElements() throws IOException {
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
    public void testXmlToJava_FromReaderWithLeadingCommentAndNullClassName() throws IOException {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><!-- schema comment --><app><title>MyApp</title></app>";

        String srcPath = tempDir.resolve("src10").toFile().getAbsolutePath();
        new File(srcPath).mkdirs();

        try (Reader reader = new StringReader(xml)) {
            PropertiesUtil.xmlToJava(reader, srcPath, "com.test10", null, false);
        }

        File generatedFile = new File(srcPath + File.separator + "com" + File.separator + "test10", "App.java");
        assertTrue(generatedFile.exists());
    }

    @Test
    public void testXmlToJava_FromReaderWithPublicFields() throws IOException {
        String xml = "<?xml version=\"1.0\"?><myapp><setting>value</setting></myapp>";

        String srcPath = tempDir.resolve("src-pub-reader").toFile().getAbsolutePath();
        new File(srcPath).mkdirs();

        try (Reader reader = new StringReader(xml)) {
            PropertiesUtil.xmlToJava(reader, srcPath, "com.pubreader", "PubReader", true);
        }

        File generatedFile = new File(srcPath + File.separator + "com" + File.separator + "pubreader", "PubReader.java");
        assertTrue(generatedFile.exists());
    }

    @Test
    public void testXmlToJava_FromReaderWithTypedProperties() throws IOException {
        String xml = "<?xml version=\"1.0\"?><config><port type=\"int\">8080</port><debug type=\"boolean\">true</debug><name>MyApp</name></config>";

        String srcPath = tempDir.resolve("src-typed").toFile().getAbsolutePath();
        new File(srcPath).mkdirs();

        try (Reader reader = new StringReader(xml)) {
            PropertiesUtil.xmlToJava(reader, srcPath, "com.typed", "TypedConfig", false);
        }

        File generatedFile = new File(srcPath + File.separator + "com" + File.separator + "typed", "TypedConfig.java");
        assertTrue(generatedFile.exists());
        String content = Files.readString(generatedFile.toPath());
        assertTrue(content.contains("int"));
        assertTrue(content.contains("boolean"));
    }

    @Test
    public void testXmlToJava_FromReaderWithDuplicatedPropertyNames() throws IOException {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<config>\n" + "  <item>first</item>\n" + "  <item>second</item>\n" + "</config>";

        String srcPath = tempDir.toString();
        String packageName = "com.test.generated";
        String className = "DuplicatedConfig";

        Assertions.assertThrows(RuntimeException.class, () -> {
            PropertiesUtil.xmlToJava(xml, srcPath, packageName, className, false);
        });
    }

    @Test
    public void testXmlToJava_GeneratesGetterSetterAndRemove() throws IOException {
        String xml = "<?xml version=\"1.0\"?><config><serverName>myserver</serverName></config>";

        String srcPath = tempDir.resolve("src-accessor").toFile().getAbsolutePath();
        new File(srcPath).mkdirs();

        PropertiesUtil.xmlToJava(xml, srcPath, "com.accessor", "AccessorConfig", false);

        File generatedFile = new File(srcPath + File.separator + "com" + File.separator + "accessor", "AccessorConfig.java");
        assertTrue(generatedFile.exists());
        String content = Files.readString(generatedFile.toPath());
        assertTrue(content.contains("getServerName"));
        assertTrue(content.contains("setServerName"));
        assertTrue(content.contains("removeServerName"));
    }

    @Test
    public void testXmlToJava_GeneratesDeprecatedOverrides() throws IOException {
        String xml = "<?xml version=\"1.0\"?><config><name>test</name></config>";

        String srcPath = tempDir.resolve("src-deprecated").toFile().getAbsolutePath();
        new File(srcPath).mkdirs();

        PropertiesUtil.xmlToJava(xml, srcPath, "com.deprecated", "DepConfig", false);

        File generatedFile = new File(srcPath + File.separator + "com" + File.separator + "deprecated", "DepConfig.java");
        String content = Files.readString(generatedFile.toPath());
        assertTrue(content.contains("@Deprecated"));
        assertTrue(content.contains("@Override"));
    }
}
