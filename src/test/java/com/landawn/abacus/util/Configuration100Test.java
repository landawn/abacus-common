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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.ParseException;

@Tag("new-test")
public class Configuration100Test extends TestBase {

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
            setAttribute("defaultAttr", "defaultValue");
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
        tempDir = new File(System.getProperty("java.io.tmpdir"), "configtest_" + System.currentTimeMillis());
        tempDir.mkdirs();
        tempConfigFile = new File(tempDir, "test.xml");
    }

    @AfterEach
    public void tearDown() throws Exception {
        if (tempConfigFile != null && tempConfigFile.exists()) {
            tempConfigFile.delete();
        }
        if (tempDir != null && tempDir.exists()) {
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
    public void testDefaultConstructor() {
        TestConfiguration config = new TestConfiguration();
        Assertions.assertNotNull(config.getAttributes());
        Assertions.assertEquals("defaultValue", config.getAttribute("defaultAttr"));
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
        Assertions.assertEquals("defaultValue", config.getAttribute("defaultAttr"));
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
    public void testGetSourceCodeLocation() {
        String location = Configuration.getSourceCodeLocation(Configuration.class);
        Assertions.assertNotNull(location);
        Assertions.assertTrue(location.length() > 0);
    }

    @Test
    public void testGetCommonConfigPath() {
        List<String> paths = Configuration.getCommonConfigPath();
        Assertions.assertNotNull(paths);
        Assertions.assertFalse(paths.isEmpty());
    }

    @Test
    public void testFindDir() throws Exception {
        File testDir = new File(tempDir, "testdir");
        testDir.mkdirs();

        File result = Configuration.findDir("nonexistent");
    }

    @Test
    public void testFindFile() throws Exception {
        createConfigFile("<?xml version=\"1.0\"?><config/>");

        File found = Configuration.findFile(tempConfigFile.getAbsolutePath());
        Assertions.assertNotNull(found);
        Assertions.assertEquals(tempConfigFile.getAbsolutePath(), found.getAbsolutePath());

        File notFound = Configuration.findFile("nonexistent.xml");
    }

    @Test
    public void testFindFileWithEmptyName() {
        Assertions.assertThrows(RuntimeException.class, () -> Configuration.findFile(""));
        Assertions.assertThrows(RuntimeException.class, () -> Configuration.findFile(null));
    }

    @Test
    public void testFindFileByFile() throws Exception {
        createConfigFile("<?xml version=\"1.0\"?><config/>");
        File srcFile = new File(tempDir, "source.txt");
        srcFile.createNewFile();

        File targetFile = new File(tempDir, "test.xml");
        File found = Configuration.findFileByFile(srcFile, targetFile.getName());
        Assertions.assertNotNull(found);
        Assertions.assertEquals(targetFile.getName(), found.getName());

        found = Configuration.findFileByFile(null, "test.xml");
    }

    @Test
    public void testFindFileInDir() throws Exception {
        createConfigFile("<?xml version=\"1.0\"?><config/>");

        File found = Configuration.findFileInDir(tempConfigFile.getName(), tempDir, false);
        Assertions.assertNotNull(found);
        Assertions.assertEquals(tempConfigFile.getName(), found.getName());

        File subDir = new File(tempDir, "subdir");
        subDir.mkdirs();
        found = Configuration.findFileInDir("subdir", tempDir, true);
        Assertions.assertNotNull(found);
        Assertions.assertTrue(found.isDirectory());

        Assertions.assertThrows(RuntimeException.class, () -> Configuration.findFileInDir("", tempDir, false));
    }

    @Test
    public void testParse() throws Exception {
        String xmlContent = "<?xml version=\"1.0\"?><root><element>value</element></root>";
        createConfigFile(xmlContent);

        Document doc = Configuration.parse(tempConfigFile);
        Assertions.assertNotNull(doc);
        Assertions.assertEquals("root", doc.getDocumentElement().getNodeName());
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
    public void testParseInvalidXml() throws Exception {
        createConfigFile("invalid xml content");

        Assertions.assertThrows(ParseException.class, () -> Configuration.parse(tempConfigFile));
    }

    @Test
    public void testFormatPath() {
        File file = new File("/path/with%20space/file.txt");
        File formatted = Configuration.formatPath(file);
        Assertions.assertNotNull(formatted);
        Assertions.assertFalse(formatted.getAbsolutePath().contains("%20"));
    }

    @Test
    public void testReadTimeInMillis() {
        Assertions.assertEquals(1000L, Configuration.readTimeInMillis("1000"));
        Assertions.assertEquals(1000L, Configuration.readTimeInMillis("1000ms"));
        Assertions.assertEquals(1000L, Configuration.readTimeInMillis("1000MS"));
        Assertions.assertEquals(1000L, Configuration.readTimeInMillis("1s"));
        Assertions.assertEquals(1000L, Configuration.readTimeInMillis("1S"));
        Assertions.assertEquals(60000L, Configuration.readTimeInMillis("1m"));
        Assertions.assertEquals(60000L, Configuration.readTimeInMillis("1M"));
        Assertions.assertEquals(3600000L, Configuration.readTimeInMillis("1h"));
        Assertions.assertEquals(3600000L, Configuration.readTimeInMillis("1H"));
        Assertions.assertEquals(86400000L, Configuration.readTimeInMillis("1d"));
        Assertions.assertEquals(86400000L, Configuration.readTimeInMillis("1D"));
        Assertions.assertEquals(604800000L, Configuration.readTimeInMillis("1w"));
        Assertions.assertEquals(604800000L, Configuration.readTimeInMillis("1W"));

        Assertions.assertEquals(1000L, Configuration.readTimeInMillis("1000l"));
        Assertions.assertEquals(1000L, Configuration.readTimeInMillis("1000L"));

        Assertions.assertEquals(6000L, Configuration.readTimeInMillis("2 * 3 * 1000"));
        Assertions.assertEquals(120000L, Configuration.readTimeInMillis("2 * 60s"));

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
        Assertions.assertTrue(names.contains("defaultAttr"));
    }

    @Test
    public void testGetAttribute() {
        TestConfiguration config = new TestConfiguration();
        config.setAttribute("testAttr", "testValue");

        Assertions.assertEquals("testValue", config.getAttribute("testAttr"));
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
    public void testSetAttribute() {
        TestConfiguration config = new TestConfiguration();

        String old = config.setAttribute("newAttr", "newValue");
        Assertions.assertNull(old);
        Assertions.assertEquals("newValue", config.getAttribute("newAttr"));

        old = config.setAttribute("newAttr", "updatedValue");
        Assertions.assertEquals("newValue", old);
        Assertions.assertEquals("updatedValue", config.getAttribute("newAttr"));

        config.setAttribute("nullAttr", null);
        Assertions.assertEquals("", config.getAttribute("nullAttr"));

        config.setAttribute("trimAttr", "  trimmed  ");
        Assertions.assertEquals("trimmed", config.getAttribute("trimAttr"));
    }

    @Test
    public void testRemoveAttribute() {
        TestConfiguration config = new TestConfiguration();
        config.setAttribute("toRemove", "value");

        Assertions.assertEquals("value", config.getAttribute("toRemove"));
        String removed = config.removeAttribute("toRemove");
        Assertions.assertEquals("value", removed);
        Assertions.assertNull(config.getAttribute("toRemove"));

        removed = config.removeAttribute("nonExistent");
        Assertions.assertNull(removed);
    }

    @Test
    public void testString2Array() {
        TestConfiguration config = new TestConfiguration();

        String[] result = config.string2Array("one,two,three");
        Assertions.assertArrayEquals(new String[] { "one", "two", "three" }, result);

        result = config.string2Array(" one , two , three ");
        Assertions.assertArrayEquals(new String[] { "one", "two", "three" }, result);

        result = config.string2Array("");
        Assertions.assertEquals(1, result.length);
        Assertions.assertEquals("", result[0]);
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
    public void testString2Set() {
        TestConfiguration config = new TestConfiguration();

        Set<String> result = config.string2Set("one,two,three,two");
        Assertions.assertEquals(3, result.size());
        Assertions.assertTrue(result.contains("one"));
        Assertions.assertTrue(result.contains("two"));
        Assertions.assertTrue(result.contains("three"));
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
    public void testEquals() {
        TestConfiguration config1 = new TestConfiguration();
        config1.setAttribute("attr", "value");

        TestConfiguration config2 = new TestConfiguration();
        config2.setAttribute("attr", "value");

        TestConfiguration config3 = new TestConfiguration();
        config3.setAttribute("attr", "different");

        Assertions.assertEquals(config1, config1);
        Assertions.assertEquals(config1, config2);
        Assertions.assertNotEquals(config1, config3);
        Assertions.assertNotEquals(config1, null);
        Assertions.assertNotEquals(config1, "not a configuration");
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
    public void testComplexElement2Attr() {
        Configuration config = new Configuration() {
            @Override
            protected void complexElement2Attr(Element element) {
                super.complexElement2Attr(element);
            }
        };

        Assertions.assertThrows(RuntimeException.class, () -> {
            Element element = parseXmlString("<unknownElement/>");
            config.complexElement2Attr(element);
        });
    }

    @Test
    public void testNestedElements() throws Exception {
        String xml = "<config>" + "<level1>value1</level1>" + "<nested>" + "  <level2>value2</level2>" + "</nested>" + "</config>";
        Element element = parseXmlString(xml);

        Configuration config = new Configuration(element, null) {
            @Override
            protected void complexElement2Attr(Element element) {
                if ("nested".equals(element.getNodeName())) {
                    return;
                }
                super.complexElement2Attr(element);
            }
        };

        Assertions.assertEquals("value1", config.getAttribute("level1"));
    }
}
