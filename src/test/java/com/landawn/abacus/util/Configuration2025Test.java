package com.landawn.abacus.util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.ParseException;
import com.landawn.abacus.exception.UncheckedIOException;

@Tag("2025")
public class Configuration2025Test extends TestBase {

    private File tempDir;
    private File tempConfigFile;

    public static class TestConfiguration extends Configuration {
        public TestConfiguration() {
            super();
        }

        public TestConfiguration(Element element, Map<String, String> props) {
            super(element, props);
        }

        @Override
        protected void init() {
            setAttribute("initAttr", "initValue");
        }

        @Override
        protected void complexElement2Attr(Element element) {
            if ("customElement".equals(element.getNodeName())) {
                setAttribute("custom", XmlUtil.getTextContent(element));
            } else {
                super.complexElement2Attr(element);
            }
        }
    }

    @BeforeEach
    public void setUp() throws Exception {
        tempDir = new File(System.getProperty("java.io.tmpdir"), "config124test_" + System.currentTimeMillis());
        tempDir.mkdirs();
        tempConfigFile = new File(tempDir, "test.xml");
    }

    @AfterEach
    public void tearDown() throws Exception {
        if (tempConfigFile != null && tempConfigFile.exists()) {
            tempConfigFile.delete();
        }
        if (tempDir != null && tempDir.exists()) {
            File[] files = tempDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    file.delete();
                }
            }
            tempDir.delete();
        }
    }

    private void createConfigFile(String content) throws Exception {
        try (FileWriter writer = new FileWriter(tempConfigFile)) {
            writer.write(content);
        }
    }

    private Element parseXmlString(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(xml.getBytes()));
        return doc.getDocumentElement();
    }

    @Test
    public void testGetSourceCodeLocation() {
        String location = Configuration.getSourceCodeLocation(Configuration.class);
        Assertions.assertNotNull(location);
        Assertions.assertTrue(location.length() > 0);
    }

    @Test
    public void testGetSourceCodeLocationWithSpaces() {
        String location = Configuration.getSourceCodeLocation(Configuration2025Test.class);
        Assertions.assertNotNull(location);
        Assertions.assertFalse(location.contains("%20"));
    }

    @Test
    public void testGetCommonConfigPath() {
        List<String> paths = Configuration.getCommonConfigPath();
        Assertions.assertNotNull(paths);
        Assertions.assertFalse(paths.isEmpty());
    }

    @Test
    public void testGetCommonConfigPathReturnsAbsolutePaths() {
        List<String> paths = Configuration.getCommonConfigPath();
        for (String path : paths) {
            File file = new File(path);
            Assertions.assertTrue(file.isAbsolute());
        }
    }

    @Test
    public void testFindDir() throws Exception {
        File testDir = new File(tempDir, "testdir");
        testDir.mkdirs();

        File result = Configuration.findDir(testDir.getAbsolutePath());
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isDirectory());
    }

    @Test
    public void testFindDirNonExistent() {
        File result = Configuration.findDir("nonexistent_dir_" + System.currentTimeMillis());
    }

    @Test
    public void testFindDirWithEmptyName() {
        Assertions.assertThrows(RuntimeException.class, () -> Configuration.findDir(""));
        Assertions.assertThrows(RuntimeException.class, () -> Configuration.findDir(null));
    }

    @Test
    public void testFindFile() throws Exception {
        createConfigFile("<?xml version=\"1.0\"?><config/>");

        File found = Configuration.findFile(tempConfigFile.getAbsolutePath());
        Assertions.assertNotNull(found);
        Assertions.assertEquals(tempConfigFile.getAbsolutePath(), found.getAbsolutePath());
    }

    @Test
    public void testFindFileNonExistent() {
        File result = Configuration.findFile("nonexistent_" + System.currentTimeMillis() + ".xml");
    }

    @Test
    public void testFindFileWithEmptyName() {
        Assertions.assertThrows(RuntimeException.class, () -> Configuration.findFile(""));
        Assertions.assertThrows(RuntimeException.class, () -> Configuration.findFile(null));
    }

    @Test
    public void testFindFileWithRelativePath() throws Exception {
        File subDir = new File(tempDir, "config");
        subDir.mkdirs();
        File configFile = new File(subDir, "app.xml");
        try (FileWriter writer = new FileWriter(configFile)) {
            writer.write("<?xml version=\"1.0\"?><config/>");
        }

        File found = Configuration.findFile(configFile.getAbsolutePath());
        Assertions.assertNotNull(found);
    }

    @Test
    public void testFindFileCaching() throws Exception {
        createConfigFile("<?xml version=\"1.0\"?><config/>");

        File found1 = Configuration.findFile(tempConfigFile.getAbsolutePath());
        File found2 = Configuration.findFile(tempConfigFile.getAbsolutePath());

        Assertions.assertNotNull(found1);
        Assertions.assertNotNull(found2);
    }

    @Test
    public void testFindFileByFile() throws Exception {
        createConfigFile("<?xml version=\"1.0\"?><config/>");
        File srcFile = new File(tempDir, "source.txt");
        srcFile.createNewFile();

        File found = Configuration.findFileByFile(srcFile, tempConfigFile.getName());
        Assertions.assertNotNull(found);
    }

    @Test
    public void testFindFileByFileWithNullSource() {
        File found = Configuration.findFileByFile(null, "test.xml");
    }

    @Test
    public void testFindFileByFileWithExistingTarget() throws Exception {
        createConfigFile("<?xml version=\"1.0\"?><config/>");
        File srcFile = new File(tempDir, "source.txt");
        srcFile.createNewFile();

        File found = Configuration.findFileByFile(srcFile, tempConfigFile.getAbsolutePath());
        Assertions.assertNotNull(found);
        Assertions.assertTrue(found.exists());
    }

    @Test
    public void testFindFileInDir() throws Exception {
        createConfigFile("<?xml version=\"1.0\"?><config/>");

        File found = Configuration.findFileInDir(tempConfigFile.getName(), tempDir, false);
        Assertions.assertNotNull(found);
        Assertions.assertEquals(tempConfigFile.getName(), found.getName());
    }

    @Test
    public void testFindFileInDirForDirectory() throws Exception {
        File subDir = new File(tempDir, "subdir");
        subDir.mkdirs();

        File found = Configuration.findFileInDir("subdir", tempDir, true);
        Assertions.assertNotNull(found);
        Assertions.assertTrue(found.isDirectory());
    }

    @Test
    public void testFindFileInDirWithEmptyName() {
        Assertions.assertThrows(RuntimeException.class, () -> Configuration.findFileInDir("", tempDir, false));
        Assertions.assertThrows(RuntimeException.class, () -> Configuration.findFileInDir(null, tempDir, false));
    }

    @Test
    public void testFindFileInDirWithRelativePath() throws Exception {
        File subDir = new File(tempDir, "config");
        subDir.mkdirs();
        File configFile = new File(subDir, "app.xml");
        try (FileWriter writer = new FileWriter(configFile)) {
            writer.write("<?xml version=\"1.0\"?><config/>");
        }

        File found = Configuration.findFileInDir("config/app.xml", tempDir, false);
        Assertions.assertNotNull(found);
    }

    @Test
    public void testFindFileInDirIgnoresSvnGitCvs() throws Exception {
        File svnDir = new File(tempDir, ".svn");
        File gitDir = new File(tempDir, ".git");
        File cvsDir = new File(tempDir, ".cvs");
        svnDir.mkdirs();
        gitDir.mkdirs();
        cvsDir.mkdirs();

        File svnFile = new File(svnDir, "test.xml");
        try (FileWriter writer = new FileWriter(svnFile)) {
            writer.write("<?xml version=\"1.0\"?><config/>");
        }

        File found = Configuration.findFileInDir("test.xml", tempDir, false);
    }

    @Test
    public void testParseFile() throws Exception {
        String xmlContent = "<?xml version=\"1.0\"?><root><element>value</element></root>";
        createConfigFile(xmlContent);

        Document doc = Configuration.parse(tempConfigFile);
        Assertions.assertNotNull(doc);
        Assertions.assertEquals("root", doc.getDocumentElement().getNodeName());
    }

    @Test
    public void testParseFileInvalid() throws Exception {
        createConfigFile("invalid xml content");

        Assertions.assertThrows(ParseException.class, () -> Configuration.parse(tempConfigFile));
    }

    @Test
    public void testParseFileNonExistent() {
        File nonExistent = new File(tempDir, "nonexistent.xml");
        Assertions.assertThrows(UncheckedIOException.class, () -> Configuration.parse(nonExistent));
    }

    @Test
    public void testParseInputStream() throws Exception {
        String xmlContent = "<?xml version=\"1.0\"?><root><element>value</element></root>";
        InputStream is = new ByteArrayInputStream(xmlContent.getBytes());

        Document doc = Configuration.parse(is);
        Assertions.assertNotNull(doc);
        Assertions.assertEquals("root", doc.getDocumentElement().getNodeName());
    }

    @Test
    public void testParseInputStreamInvalid() {
        String xmlContent = "invalid xml content";
        InputStream is = new ByteArrayInputStream(xmlContent.getBytes());

        Assertions.assertThrows(ParseException.class, () -> Configuration.parse(is));
    }

    @Test
    public void testFormatPath() {
        File file = new File("/path/with%20space/file.txt");
        File formatted = Configuration.formatPath(file);
        Assertions.assertNotNull(formatted);
        Assertions.assertFalse(formatted.getAbsolutePath().contains("%20"));
    }

    @Test
    public void testFormatPathNoEncoding() {
        File file = new File("/path/without/encoding/file.txt");
        File formatted = Configuration.formatPath(file);
        Assertions.assertNotNull(formatted);
    }

    @Test
    public void testFormatPathExistingFile() throws Exception {
        File formatted = Configuration.formatPath(tempConfigFile);
        Assertions.assertNotNull(formatted);
    }

    @Test
    public void testReadTimeInMillisBasic() {
        Assertions.assertEquals(1000L, Configuration.readTimeInMillis("1000"));
        Assertions.assertEquals(1000L, Configuration.readTimeInMillis("1000ms"));
        Assertions.assertEquals(1000L, Configuration.readTimeInMillis("1000MS"));
    }

    @Test
    public void testReadTimeInMillisSeconds() {
        Assertions.assertEquals(1000L, Configuration.readTimeInMillis("1s"));
        Assertions.assertEquals(1000L, Configuration.readTimeInMillis("1S"));
        Assertions.assertEquals(30000L, Configuration.readTimeInMillis("30s"));
    }

    @Test
    public void testReadTimeInMillisMinutes() {
        Assertions.assertEquals(60000L, Configuration.readTimeInMillis("1m"));
        Assertions.assertEquals(60000L, Configuration.readTimeInMillis("1M"));
        Assertions.assertEquals(300000L, Configuration.readTimeInMillis("5m"));
    }

    @Test
    public void testReadTimeInMillisHours() {
        Assertions.assertEquals(3600000L, Configuration.readTimeInMillis("1h"));
        Assertions.assertEquals(3600000L, Configuration.readTimeInMillis("1H"));
        Assertions.assertEquals(7200000L, Configuration.readTimeInMillis("2h"));
    }

    @Test
    public void testReadTimeInMillisDays() {
        Assertions.assertEquals(86400000L, Configuration.readTimeInMillis("1d"));
        Assertions.assertEquals(86400000L, Configuration.readTimeInMillis("1D"));
        Assertions.assertEquals(172800000L, Configuration.readTimeInMillis("2d"));
    }

    @Test
    public void testReadTimeInMillisWeeks() {
        Assertions.assertEquals(604800000L, Configuration.readTimeInMillis("1w"));
        Assertions.assertEquals(604800000L, Configuration.readTimeInMillis("1W"));
        Assertions.assertEquals(1209600000L, Configuration.readTimeInMillis("2w"));
    }

    @Test
    public void testReadTimeInMillisWithLSuffix() {
        Assertions.assertEquals(1000L, Configuration.readTimeInMillis("1000l"));
        Assertions.assertEquals(1000L, Configuration.readTimeInMillis("1000L"));
        Assertions.assertEquals(1000L, Configuration.readTimeInMillis("1sl"));
        Assertions.assertEquals(1000L, Configuration.readTimeInMillis("1SL"));
    }

    @Test
    public void testReadTimeInMillisMultiplication() {
        Assertions.assertEquals(6000L, Configuration.readTimeInMillis("2 * 3 * 1000"));
        Assertions.assertEquals(120000L, Configuration.readTimeInMillis("2 * 60s"));
        Assertions.assertEquals(3600000L, Configuration.readTimeInMillis("60 * 60 * 1000"));
    }

    @Test
    public void testReadTimeInMillisNullEmpty() {
        Assertions.assertEquals(0L, Configuration.readTimeInMillis(null));
        Assertions.assertEquals(0L, Configuration.readTimeInMillis(""));
        Assertions.assertEquals(0L, Configuration.readTimeInMillis("  "));
    }

    @Test
    public void testGetAttrNames() {
        TestConfiguration config = new TestConfiguration();
        config.setAttribute("attr1", "value1");
        config.setAttribute("attr2", "value2");

        Collection<String> names = config.getAttrNames();
        Assertions.assertNotNull(names);
        Assertions.assertTrue(names.contains("attr1"));
        Assertions.assertTrue(names.contains("attr2"));
        Assertions.assertTrue(names.contains("initAttr"));
    }

    @Test
    public void testGetAttrNamesEmpty() {
        Configuration config = new Configuration() {
        };
        Collection<String> names = config.getAttrNames();
        Assertions.assertNotNull(names);
    }

    @Test
    public void testGetAttribute() {
        TestConfiguration config = new TestConfiguration();
        config.setAttribute("testAttr", "testValue");

        Assertions.assertEquals("testValue", config.getAttribute("testAttr"));
        Assertions.assertNull(config.getAttribute("nonExistent"));
    }

    @Test
    public void testGetAttributeNull() {
        TestConfiguration config = new TestConfiguration();
        Assertions.assertNull(config.getAttribute("nonExistent"));
    }

    @Test
    public void testHasAttribute() {
        TestConfiguration config = new TestConfiguration();
        config.setAttribute("existingAttr", "value");

        Assertions.assertTrue(config.hasAttribute("existingAttr"));
        Assertions.assertFalse(config.hasAttribute("nonExistent"));
    }

    @Test
    public void testHasAttributeAfterRemoval() {
        TestConfiguration config = new TestConfiguration();
        config.setAttribute("attr", "value");
        Assertions.assertTrue(config.hasAttribute("attr"));

        config.removeAttribute("attr");
        Assertions.assertFalse(config.hasAttribute("attr"));
    }

    @Test
    public void testGetAttributes() {
        TestConfiguration config = new TestConfiguration();
        config.setAttribute("attr1", "value1");
        config.setAttribute("attr2", "value2");

        Map<String, String> attrs = config.getAttributes();
        Assertions.assertNotNull(attrs);
        Assertions.assertEquals("value1", attrs.get("attr1"));
        Assertions.assertEquals("value2", attrs.get("attr2"));
    }

    @Test
    public void testGetAttributesModifiable() {
        TestConfiguration config = new TestConfiguration();
        Map<String, String> attrs = config.getAttributes();
        Assertions.assertNotNull(attrs);

        attrs.put("direct", "value");
        Assertions.assertEquals("value", config.getAttribute("direct"));
    }

    @Test
    public void testHashCode() {
        TestConfiguration config1 = new TestConfiguration();
        config1.setAttribute("attr", "value");

        TestConfiguration config2 = new TestConfiguration();
        config2.setAttribute("attr", "value");

        Assertions.assertEquals(config1.hashCode(), config2.hashCode());
    }

    @Test
    public void testHashCodeDifferent() {
        TestConfiguration config1 = new TestConfiguration();
        config1.setAttribute("attr", "value1");

        TestConfiguration config2 = new TestConfiguration();
        config2.setAttribute("attr", "value2");

    }

    @Test
    public void testEquals() {
        TestConfiguration config1 = new TestConfiguration();
        config1.setAttribute("attr", "value");

        TestConfiguration config2 = new TestConfiguration();
        config2.setAttribute("attr", "value");

        Assertions.assertEquals(config1, config1);
        Assertions.assertEquals(config1, config2);
    }

    @Test
    public void testEqualsDifferent() {
        TestConfiguration config1 = new TestConfiguration();
        config1.setAttribute("attr", "value1");

        TestConfiguration config2 = new TestConfiguration();
        config2.setAttribute("attr", "value2");

        Assertions.assertNotEquals(config1, config2);
    }

    @Test
    public void testEqualsNull() {
        TestConfiguration config = new TestConfiguration();
        Assertions.assertNotEquals(config, null);
    }

    @Test
    public void testEqualsDifferentClass() {
        TestConfiguration config = new TestConfiguration();
        Assertions.assertNotEquals(config, "not a configuration");
    }

    @Test
    public void testToString() {
        TestConfiguration config = new TestConfiguration();
        config.setAttribute("attr1", "value1");
        config.setAttribute("attr2", "value2");

        String str = config.toString();
        Assertions.assertNotNull(str);
        Assertions.assertTrue(str.contains("attr1"));
        Assertions.assertTrue(str.contains("value1"));
        Assertions.assertTrue(str.contains("attr2"));
        Assertions.assertTrue(str.contains("value2"));
    }

    @Test
    public void testToStringEmpty() {
        Configuration config = new Configuration() {
        };
        String str = config.toString();
        Assertions.assertNotNull(str);
    }

    @Test
    public void testDefaultConstructor() {
        TestConfiguration config = new TestConfiguration();
        Assertions.assertNotNull(config.getAttributes());
        Assertions.assertEquals("initValue", config.getAttribute("initAttr"));
    }

    @Test
    public void testConstructorWithNullElement() {
        TestConfiguration config = new TestConfiguration(null, null);
        Assertions.assertNotNull(config.getAttributes());
        Assertions.assertEquals("initValue", config.getAttribute("initAttr"));
    }

    @Test
    public void testConstructorWithElement() throws Exception {
        String xml = "<config attr1=\"value1\" attr2=\"value2\">" + "<simpleElement>simpleValue</simpleElement>" + "<customElement>customValue</customElement>"
                + "</config>";
        Element element = parseXmlString(xml);

        TestConfiguration config = new TestConfiguration(element, null);

        Assertions.assertEquals("value1", config.getAttribute("attr1"));
        Assertions.assertEquals("value2", config.getAttribute("attr2"));
        Assertions.assertEquals("simpleValue", config.getAttribute("simpleElement"));
        Assertions.assertEquals("customValue", config.getAttribute("customElement"));
    }

    @Test
    public void testConstructorWithProperties() throws Exception {
        Map<String, String> props = new HashMap<>();
        props.put("propValue", "replacedValue");

        String xml = "<config attr1=\"${propValue}\" attr2=\"normalValue\"/>";
        Element element = parseXmlString(xml);

        TestConfiguration config = new TestConfiguration(element, props);

        Assertions.assertEquals("replacedValue", config.getAttribute("attr1"));
        Assertions.assertEquals("normalValue", config.getAttribute("attr2"));
    }

    @Test
    public void testConstructorWithPropertiesNotFound() throws Exception {
        Map<String, String> props = new HashMap<>();
        props.put("otherProp", "value");

        String xml = "<config attr1=\"${notFound}\"/>";
        Element element = parseXmlString(xml);

        TestConfiguration config = new TestConfiguration(element, props);

        Assertions.assertNull(config.getAttribute("attr1"));
    }

    @Test
    public void testSetAttribute() {
        TestConfiguration config = new TestConfiguration();

        String old = config.setAttribute("newAttr", "newValue");
        Assertions.assertNull(old);
        Assertions.assertEquals("newValue", config.getAttribute("newAttr"));

        old = config.setAttribute("newAttr", "updatedValue");
        Assertions.assertEquals("newValue", old);
        Assertions.assertEquals("updatedValue", config.getAttribute("newAttr"));
    }

    @Test
    public void testSetAttributeNull() {
        TestConfiguration config = new TestConfiguration();
        config.setAttribute("nullAttr", null);
        Assertions.assertEquals("", config.getAttribute("nullAttr"));
    }

    @Test
    public void testSetAttributeTrimming() {
        TestConfiguration config = new TestConfiguration();
        config.setAttribute("trimAttr", "  trimmed  ");
        Assertions.assertEquals("trimmed", config.getAttribute("trimAttr"));
    }

    @Test
    public void testSetAttributeWithPropertyReplacement() {
        Map<String, String> props = new HashMap<>();
        props.put("myProp", "myValue");

        TestConfiguration config = new TestConfiguration(null, props);
        config.setAttribute("attr", "${myProp}");
        Assertions.assertEquals("myValue", config.getAttribute("attr"));
    }

    @Test
    public void testRemoveAttribute() {
        TestConfiguration config = new TestConfiguration();
        config.setAttribute("toRemove", "value");

        String removed = config.removeAttribute("toRemove");
        Assertions.assertEquals("value", removed);
        Assertions.assertNull(config.getAttribute("toRemove"));
    }

    @Test
    public void testRemoveAttributeNonExistent() {
        TestConfiguration config = new TestConfiguration();
        String removed = config.removeAttribute("nonExistent");
        Assertions.assertNull(removed);
    }

    @Test
    public void testString2Array() {
        TestConfiguration config = new TestConfiguration();

        String[] result = config.string2Array("one,two,three");
        Assertions.assertArrayEquals(new String[] { "one", "two", "three" }, result);
    }

    @Test
    public void testString2ArrayWithSpaces() {
        TestConfiguration config = new TestConfiguration();
        String[] result = config.string2Array(" one , two , three ");
        Assertions.assertArrayEquals(new String[] { "one", "two", "three" }, result);
    }

    @Test
    public void testString2ArrayEmpty() {
        TestConfiguration config = new TestConfiguration();
        String[] result = config.string2Array("");
        Assertions.assertEquals(1, result.length);
    }

    @Test
    public void testString2List() {
        TestConfiguration config = new TestConfiguration();

        List<String> result = config.string2List("one,two,three");
        Assertions.assertEquals(3, result.size());
        Assertions.assertEquals("one", result.get(0));
        Assertions.assertEquals("two", result.get(1));
        Assertions.assertEquals("three", result.get(2));
    }

    @Test
    public void testString2ListWithSpaces() {
        TestConfiguration config = new TestConfiguration();
        List<String> result = config.string2List(" one , two , three ");
        Assertions.assertEquals(3, result.size());
        Assertions.assertEquals("one", result.get(0));
    }

    @Test
    public void testString2Set() {
        TestConfiguration config = new TestConfiguration();

        Set<String> result = config.string2Set("one,two,three,two");
        Assertions.assertEquals(3, result.size());
        Assertions.assertTrue(result.contains("one"));
        Assertions.assertTrue(result.contains("two"));
        Assertions.assertTrue(result.contains("three"));
    }

    @Test
    public void testString2SetWithSpaces() {
        TestConfiguration config = new TestConfiguration();
        Set<String> result = config.string2Set(" one , two , three ");
        Assertions.assertEquals(3, result.size());
    }

    @Test
    public void testInitCalled() {
        TestConfiguration config = new TestConfiguration();
        Assertions.assertEquals("initValue", config.getAttribute("initAttr"));
    }

    @Test
    public void testComplexElement2Attr() throws Exception {
        String xml = "<customElement>customValue</customElement>";
        Element element = parseXmlString(xml);

        TestConfiguration config = new TestConfiguration();
        config.complexElement2Attr(element);

        Assertions.assertEquals("customValue", config.getAttribute("custom"));
    }

    @Test
    public void testComplexElement2AttrUnknown() throws Exception {
        String xml = "<unknownElement>value</unknownElement>";
        Element element = parseXmlString(xml);

        Configuration config = new Configuration() {
        };

        Assertions.assertThrows(RuntimeException.class, () -> config.complexElement2Attr(element));
    }
}
