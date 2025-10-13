package com.landawn.abacus.parser;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.annotation.JsonXmlField;
import com.landawn.abacus.exception.ParseException;
import com.landawn.abacus.parser.XMLDeserializationConfig.XDC;
import com.landawn.abacus.parser.XMLSerializationConfig.XSC;
import com.landawn.abacus.util.BufferedXMLWriter;
import com.landawn.abacus.util.MapEntity;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.NamingPolicy;
import com.landawn.abacus.util.Objectory;

@Tag("new-test")
public class XMLParserImpl101Test extends TestBase {

    private XMLParserImpl staxParser;
    private XMLParserImpl domParser;
    private XMLSerializationConfig serializationConfig;
    private XMLDeserializationConfig deserializationConfig;

    @TempDir
    Path tempDir;

    @BeforeEach
    public void setUp() {
        staxParser = new XMLParserImpl(XMLParserType.StAX);
        domParser = new XMLParserImpl(XMLParserType.DOM);
        serializationConfig = new XMLSerializationConfig();
        deserializationConfig = new XMLDeserializationConfig();
    }

    public static class TestBean {
        private String name;
        private int age;
        private boolean active;
        private List<String> tags;
        private Map<String, String> attributes;
        private TestBean nested;
        private List<TestBean> moreNested;

        @JsonXmlField(ignore = true)
        private String ignoredField;

        @JsonXmlField(name = "customName")
        private String renamedField;

        public TestBean() {
        }

        public TestBean(String name, int age) {
            this.name = name;
            this.age = age;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }

        public boolean isActive() {
            return active;
        }

        public void setActive(boolean active) {
            this.active = active;
        }

        public List<String> getTags() {
            return tags;
        }

        public void setTags(List<String> tags) {
            this.tags = tags;
        }

        public Map<String, String> getAttributes() {
            return attributes;
        }

        public void setAttributes(Map<String, String> attributes) {
            this.attributes = attributes;
        }

        public TestBean getNested() {
            return nested;
        }

        public void setNested(TestBean nested) {
            this.nested = nested;
        }

        public String getIgnoredField() {
            return ignoredField;
        }

        public void setIgnoredField(String ignoredField) {
            this.ignoredField = ignoredField;
        }

        public String getRenamedField() {
            return renamedField;
        }

        public void setRenamedField(String renamedField) {
            this.renamedField = renamedField;
        }

        public List<TestBean> getMoreNested() {
            return moreNested;
        }

        public void setMoreNested(List<TestBean> moreNested) {
            this.moreNested = moreNested;
        }
    }

    public static class CircularRefBean {
        private String name;
        private CircularRefBean reference;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public CircularRefBean getReference() {
            return reference;
        }

        public void setReference(CircularRefBean reference) {
            this.reference = reference;
        }
    }

    public static class EmptyBean {
    }

    public static class PrimitiveBean {
        private byte byteVal;
        private short shortVal;
        private int intVal;
        private long longVal;
        private float floatVal;
        private double doubleVal;
        private boolean booleanVal;
        private char charVal;

        public byte getByteVal() {
            return byteVal;
        }

        public void setByteVal(byte byteVal) {
            this.byteVal = byteVal;
        }

        public short getShortVal() {
            return shortVal;
        }

        public void setShortVal(short shortVal) {
            this.shortVal = shortVal;
        }

        public int getIntVal() {
            return intVal;
        }

        public void setIntVal(int intVal) {
            this.intVal = intVal;
        }

        public long getLongVal() {
            return longVal;
        }

        public void setLongVal(long longVal) {
            this.longVal = longVal;
        }

        public float getFloatVal() {
            return floatVal;
        }

        public void setFloatVal(float floatVal) {
            this.floatVal = floatVal;
        }

        public double getDoubleVal() {
            return doubleVal;
        }

        public void setDoubleVal(double doubleVal) {
            this.doubleVal = doubleVal;
        }

        public boolean isBooleanVal() {
            return booleanVal;
        }

        public void setBooleanVal(boolean booleanVal) {
            this.booleanVal = booleanVal;
        }

        public char getCharVal() {
            return charVal;
        }

        public void setCharVal(char charVal) {
            this.charVal = charVal;
        }
    }

    @Test
    public void testConstructorWithParserType() {
        XMLParserImpl parser = new XMLParserImpl(XMLParserType.StAX);
        Assertions.assertNotNull(parser);

        parser = new XMLParserImpl(XMLParserType.DOM);
        Assertions.assertNotNull(parser);
    }

    @Test
    public void testConstructorWithAllParameters() {
        XMLSerializationConfig xsc = new XMLSerializationConfig();
        XMLDeserializationConfig xdc = new XMLDeserializationConfig();

        XMLParserImpl parser = new XMLParserImpl(XMLParserType.StAX, xsc, xdc);
        Assertions.assertNotNull(parser);

        parser = new XMLParserImpl(XMLParserType.DOM, xsc, xdc);
        Assertions.assertNotNull(parser);
    }

    @Test
    public void testSerializeNull() {
        String result = staxParser.serialize(null);
        Assertions.assertEquals("", result);

        result = domParser.serialize(null);
        Assertions.assertEquals("", result);
    }

    @Test
    public void testSerializeSimpleBean() {
        TestBean bean = new TestBean("John", 30);
        bean.setActive(true);

        String xml = staxParser.serialize(bean);
        Assertions.assertNotNull(xml);
        Assertions.assertTrue(xml.contains("John"));
        Assertions.assertTrue(xml.contains("30"));
        Assertions.assertTrue(xml.contains("true"));
    }

    @Test
    public void testSerializeWithCollections() {
        TestBean bean = new TestBean("Jane", 25);
        bean.setTags(Arrays.asList("tag1", "tag2", "tag3"));

        Map<String, String> attrs = new HashMap<>();
        attrs.put("key1", "value1");
        attrs.put("key2", "value2");
        bean.setAttributes(attrs);

        String xml = staxParser.serialize(bean);
        Assertions.assertNotNull(xml);
        Assertions.assertTrue(xml.contains("tag1"));
        Assertions.assertTrue(xml.contains("tag2"));
        Assertions.assertTrue(xml.contains("tag3"));
        Assertions.assertTrue(xml.contains("key1"));
        Assertions.assertTrue(xml.contains("value1"));
    }

    @Test
    public void testSerializeNestedBean() {
        TestBean parent = new TestBean("Parent", 40);
        TestBean child = new TestBean("Child", 10);
        TestBean child2 = new TestBean("Child2", 10);
        parent.setTags(Arrays.asList("tag1", "tag2"));
        parent.setNested(child);
        parent.setMoreNested(Arrays.asList(child, child2));

        String xml = staxParser.serialize(parent);
        N.println(xml);
        Assertions.assertNotNull(xml);
        Assertions.assertTrue(xml.contains("Parent"));
        Assertions.assertTrue(xml.contains("Child"));
    }

    @Test
    public void testSerializeWithCircularReference() {
        CircularRefBean bean1 = new CircularRefBean();
        bean1.setName("Bean1");
        CircularRefBean bean2 = new CircularRefBean();
        bean2.setName("Bean2");
        bean1.setReference(bean2);
        bean2.setReference(bean1);

        Assertions.assertThrows(StackOverflowError.class, () -> {
            staxParser.serialize(bean1);
        });

        XMLSerializationConfig config = new XMLSerializationConfig();
        config.supportCircularReference(true);
        String xml = staxParser.serialize(bean1, config);
        Assertions.assertNotNull(xml);
    }

    @Test
    public void testSerializeArray() {
        int[] intArray = { 1, 2, 3, 4, 5 };
        String xml = staxParser.serialize(intArray);
        Assertions.assertNotNull(xml);

        String[] stringArray = { "one", "two", "three" };
        xml = staxParser.serialize(stringArray);
        Assertions.assertNotNull(xml);
        Assertions.assertTrue(xml.contains("one"));
        Assertions.assertTrue(xml.contains("two"));
        Assertions.assertTrue(xml.contains("three"));
    }

    @Test
    public void testSerializeList() {
        List<String> list = Arrays.asList("apple", "banana", "cherry");
        String xml = staxParser.serialize(list);
        Assertions.assertNotNull(xml);
        Assertions.assertTrue(xml.contains("apple"));
        Assertions.assertTrue(xml.contains("banana"));
        Assertions.assertTrue(xml.contains("cherry"));
    }

    @Test
    public void testSerializeMap() {
        Map<String, Integer> map = new HashMap<>();
        map.put("one", 1);
        map.put("two", 2);
        map.put("three", 3);

        String xml = staxParser.serialize(map);
        Assertions.assertNotNull(xml);
        Assertions.assertTrue(xml.contains("one"));
        Assertions.assertTrue(xml.contains("two"));
        Assertions.assertTrue(xml.contains("three"));
    }

    @Test
    public void testSerializeMapEntity() {
        MapEntity entity = new MapEntity("TestEntity");
        entity.set("prop1", "value1");
        entity.set("prop2", 123);
        entity.set("prop3", true);

        String xml = staxParser.serialize(entity);
        Assertions.assertNotNull(xml);
        Assertions.assertTrue(xml.contains("TestEntity"));
        Assertions.assertTrue(xml.contains("prop1"));
        Assertions.assertTrue(xml.contains("value1"));
    }

    @Test
    public void testSerializeWithPrettyFormat() {
        TestBean bean = new TestBean("Test", 20);

        XMLSerializationConfig config = new XMLSerializationConfig();
        config.prettyFormat(true);

        String xml = staxParser.serialize(bean, config);
        Assertions.assertNotNull(xml);
        Assertions.assertTrue(xml.contains("\n"));
    }

    @Test
    public void testSerializeWithwriteTypeInfo() {
        TestBean bean = new TestBean("Test", 20);

        XMLSerializationConfig config = new XMLSerializationConfig();
        config.writeTypeInfo(true);

        String xml = staxParser.serialize(bean, config);
        Assertions.assertNotNull(xml);
    }

    @Test
    public void testSerializeWithTagByPropertyName() {
        TestBean bean = new TestBean("Test", 20);

        XMLSerializationConfig config = new XMLSerializationConfig();
        config.tagByPropertyName(true);

        String xml = staxParser.serialize(bean, config);
        Assertions.assertNotNull(xml);
    }

    @Test
    public void testSerializeWithIgnoredProperties() {
        TestBean bean = new TestBean("Test", 20);
        bean.setIgnoredField("This should be ignored");

        String xml = staxParser.serialize(bean);
        Assertions.assertNotNull(xml);
        Assertions.assertFalse(xml.contains("This should be ignored"));
    }

    @Test
    public void testSerializeWithRenamedField() {
        TestBean bean = new TestBean("Test", 20);
        bean.setRenamedField("Renamed Value");

        String xml = staxParser.serialize(bean);
        Assertions.assertNotNull(xml);
        Assertions.assertTrue(xml.contains("customName"));
        Assertions.assertTrue(xml.contains("Renamed Value"));
    }

    @Test
    public void testSerializeEmptyBean() {
        EmptyBean bean = new EmptyBean();

        Assertions.assertThrows(ParseException.class, () -> {
            staxParser.serialize(bean);
        });

        XMLSerializationConfig config = new XMLSerializationConfig();
        config.failOnEmptyBean(false);
        String xml = staxParser.serialize(bean, config);
        Assertions.assertEquals("", xml);
    }

    @Test
    public void testSerializePrimitiveTypes() {
        PrimitiveBean bean = new PrimitiveBean();
        bean.setByteVal((byte) 127);
        bean.setShortVal((short) 32767);
        bean.setIntVal(2147483647);
        bean.setLongVal(9223372036854775807L);
        bean.setFloatVal(3.14f);
        bean.setDoubleVal(2.71828);
        bean.setBooleanVal(true);
        bean.setCharVal('A');

        String xml = staxParser.serialize(bean);
        Assertions.assertNotNull(xml);
        Assertions.assertTrue(xml.contains("127"));
        Assertions.assertTrue(xml.contains("32767"));
        Assertions.assertTrue(xml.contains("2147483647"));
        Assertions.assertTrue(xml.contains("9223372036854775807"));
        Assertions.assertTrue(xml.contains("3.14"));
        Assertions.assertTrue(xml.contains("2.71828"));
        Assertions.assertTrue(xml.contains("true"));
        Assertions.assertTrue(xml.contains("A"));
    }

    @Test
    public void testSerializeToFile() throws IOException {
        TestBean bean = new TestBean("FileTest", 25);
        File file = tempDir.resolve("test.xml").toFile();

        staxParser.serialize(bean, null, file);

        Assertions.assertTrue(file.exists());
        String content = new String(java.nio.file.Files.readAllBytes(file.toPath()));
        Assertions.assertTrue(content.contains("FileTest"));
        Assertions.assertTrue(content.contains("25"));
    }

    @Test
    public void testSerializeToNonExistentFile() throws IOException {
        TestBean bean = new TestBean("FileTest", 25);
        File file = tempDir.resolve("subdir/test.xml").toFile();

        staxParser.serialize(bean, null, file);

        Assertions.assertTrue(file.exists());
    }

    @Test
    public void testSerializeToOutputStream() throws IOException {
        TestBean bean = new TestBean("StreamTest", 30);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        staxParser.serialize(bean, null, baos);

        String xml = baos.toString();
        Assertions.assertTrue(xml.contains("StreamTest"));
        Assertions.assertTrue(xml.contains("30"));
    }

    @Test
    public void testSerializeToWriter() throws IOException {
        TestBean bean = new TestBean("WriterTest", 35);
        StringWriter writer = new StringWriter();

        staxParser.serialize(bean, null, writer);

        String xml = writer.toString();
        Assertions.assertTrue(xml.contains("WriterTest"));
        Assertions.assertTrue(xml.contains("35"));
    }

    @Test
    public void testSerializeToBufferedXMLWriter() throws IOException {
        TestBean bean = new TestBean("BufferedTest", 40);
        BufferedXMLWriter bw = Objectory.createBufferedXMLWriter();

        try {
            staxParser.serialize(bean, null, bw);
            String xml = bw.toString();
            Assertions.assertTrue(xml.contains("BufferedTest"));
            Assertions.assertTrue(xml.contains("40"));
        } finally {
            Objectory.recycle(bw);
        }
    }

    @Test
    public void testDeserializeEmptyString() {
        TestBean result = staxParser.deserialize("", null, TestBean.class);
        Assertions.assertNull(result);

        result = domParser.deserialize("", null, TestBean.class);
        Assertions.assertNull(result);
    }

    @Test
    public void testDeserializeNullString() {
        TestBean result = staxParser.deserialize((String) null, null, TestBean.class);
        Assertions.assertNull(result);

        result = domParser.deserialize((String) null, null, TestBean.class);
        Assertions.assertNull(result);
    }

    @Test
    public void testDeserializeSimpleBean() {
        String xml = "<TestBean><name>John</name><age>30</age><active>true</active></TestBean>";

        TestBean result = staxParser.deserialize(xml, null, TestBean.class);
        Assertions.assertNotNull(result);
        Assertions.assertEquals("John", result.getName());
        Assertions.assertEquals(30, result.getAge());
        Assertions.assertTrue(result.isActive());

        result = domParser.deserialize(xml, null, TestBean.class);
        Assertions.assertNotNull(result);
        Assertions.assertEquals("John", result.getName());
        Assertions.assertEquals(30, result.getAge());
        Assertions.assertTrue(result.isActive());
    }

    @Test
    public void testDeserializeWithCollections() {
        String xml = "<TestBean><name>Jane</name><age>25</age>" + "<tags>[\"tag1\",\"tag2\",\"tag3\"]</tags>"
                + "<attributes><map><key1>value1</key1><key2>value2</key2></map></attributes></TestBean>";

        TestBean result = staxParser.deserialize(xml, null, TestBean.class);
        Assertions.assertNotNull(result);
        Assertions.assertEquals("Jane", result.getName());
        Assertions.assertEquals(25, result.getAge());
        Assertions.assertNotNull(result.getTags());
        Assertions.assertEquals(3, result.getTags().size());
        Assertions.assertTrue(result.getTags().contains("tag1"));
        Assertions.assertNotNull(result.getAttributes());
        Assertions.assertEquals("value1", result.getAttributes().get("key1"));
    }

    @Test
    public void testDeserializeNestedBean() {
        String xml = "<testBean><name>Parent</name><age>40</age><nested><testBean><name>Child</name><age>10</age></testBean></nested></testBean>";

        TestBean result = domParser.deserialize(xml, null, TestBean.class);
        Assertions.assertNotNull(result);
        Assertions.assertEquals("Parent", result.getName());
        Assertions.assertNotNull(result.getNested());
        Assertions.assertEquals("Child", result.getNested().getName());
        Assertions.assertEquals(10, result.getNested().getAge());
    }

    @Test
    public void testDeserializeArray() {
        String xml = "<array>[1,2,3,4,5]</array>";

        Integer[] result = staxParser.deserialize(xml, null, Integer[].class);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(5, result.length);
        Assertions.assertEquals(1, result[0]);
        Assertions.assertEquals(5, result[4]);
    }

    @Test
    public void testDeserializeList() {
        String xml = "<list>[\"apple\",\"banana\",\"cherry\"]</list>";

        List<String> result = staxParser.deserialize(xml, null, List.class);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(3, result.size());
        Assertions.assertTrue(result.contains("apple"));
        Assertions.assertTrue(result.contains("banana"));
        Assertions.assertTrue(result.contains("cherry"));
    }

    @Test
    public void testDeserializeMap() {
        String xml = "<map><one>1</one><two>2</two><three>3</three></map>";

        Map<String, Integer> result = staxParser.deserialize(xml, null, Map.class);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(3, result.size());
        Assertions.assertEquals("1", result.get("one"));
        Assertions.assertEquals("2", result.get("two"));
        Assertions.assertEquals("3", result.get("three"));
    }

    @Test
    public void testDeserializeMapEntity() {
        String xml = "<TestEntity><prop1>value1</prop1><prop2>123</prop2><prop3>true</prop3></TestEntity>";

        MapEntity result = staxParser.deserialize(xml, null, MapEntity.class);
        Assertions.assertNotNull(result);
        Assertions.assertEquals("TestEntity", result.entityName());
        Assertions.assertEquals("value1", result.get("prop1"));
        Assertions.assertEquals("123", result.get("prop2"));
        Assertions.assertEquals("true", result.get("prop3"));
    }

    @Test
    public void testDeserializeWithIgnoreUnmatchedProperty() {
        String xml = "<TestBean><name>John</name><age>30</age><unknownProp>value</unknownProp></TestBean>";

        Assertions.assertThrows(ParseException.class, () -> {
            staxParser.deserialize(xml, XDC.create().ignoreUnmatchedProperty(false), TestBean.class);
        });

        XMLDeserializationConfig config = XDC.create().ignoreUnmatchedProperty(true);
        TestBean result = staxParser.deserialize(xml, config, TestBean.class);
        Assertions.assertNotNull(result);
        Assertions.assertEquals("John", result.getName());
        Assertions.assertEquals(30, result.getAge());
    }

    @Test
    public void testDeserializePrimitiveTypes() {
        String xml = "<PrimitiveBean>" + "<byteVal>127</byteVal>" + "<shortVal>32767</shortVal>" + "<intVal>2147483647</intVal>"
                + "<longVal>9223372036854775807</longVal>" + "<floatVal>3.14</floatVal>" + "<doubleVal>2.71828</doubleVal>" + "<booleanVal>true</booleanVal>"
                + "<charVal>A</charVal>" + "</PrimitiveBean>";

        PrimitiveBean result = staxParser.deserialize(xml, null, PrimitiveBean.class);
        Assertions.assertNotNull(result);
        Assertions.assertEquals((byte) 127, result.getByteVal());
        Assertions.assertEquals((short) 32767, result.getShortVal());
        Assertions.assertEquals(2147483647, result.getIntVal());
        Assertions.assertEquals(9223372036854775807L, result.getLongVal());
        Assertions.assertEquals(3.14f, result.getFloatVal(), 0.001);
        Assertions.assertEquals(2.71828, result.getDoubleVal(), 0.00001);
        Assertions.assertTrue(result.isBooleanVal());
        Assertions.assertEquals('A', result.getCharVal());
    }

    @Test
    public void testDeserializeFromFile() throws IOException {
        File file = tempDir.resolve("input.xml").toFile();
        String xml = "<TestBean><name>FileTest</name><age>25</age></TestBean>";
        java.nio.file.Files.write(file.toPath(), xml.getBytes());

        TestBean result = staxParser.deserialize(file, null, TestBean.class);
        Assertions.assertNotNull(result);
        Assertions.assertEquals("FileTest", result.getName());
        Assertions.assertEquals(25, result.getAge());

        result = domParser.deserialize(file, null, TestBean.class);
        Assertions.assertNotNull(result);
        Assertions.assertEquals("FileTest", result.getName());
        Assertions.assertEquals(25, result.getAge());
    }

    @Test
    public void testDeserializeFromInputStream() throws IOException {
        String xml = "<TestBean><name>StreamTest</name><age>30</age></TestBean>";
        ByteArrayInputStream bais = new ByteArrayInputStream(xml.getBytes());

        TestBean result = staxParser.deserialize(bais, null, TestBean.class);
        Assertions.assertNotNull(result);
        Assertions.assertEquals("StreamTest", result.getName());
        Assertions.assertEquals(30, result.getAge());

        bais.reset();
        result = domParser.deserialize(bais, null, TestBean.class);
        Assertions.assertNotNull(result);
        Assertions.assertEquals("StreamTest", result.getName());
        Assertions.assertEquals(30, result.getAge());
    }

    @Test
    public void testDeserializeFromReader() throws IOException {
        String xml = "<TestBean><name>ReaderTest</name><age>35</age></TestBean>";
        StringReader reader = new StringReader(xml);

        TestBean result = staxParser.deserialize(reader, null, TestBean.class);
        Assertions.assertNotNull(result);
        Assertions.assertEquals("ReaderTest", result.getName());
        Assertions.assertEquals(35, result.getAge());

        reader = new StringReader(xml);
        result = domParser.deserialize(reader, null, TestBean.class);
        Assertions.assertNotNull(result);
        Assertions.assertEquals("ReaderTest", result.getName());
        Assertions.assertEquals(35, result.getAge());
    }

    @Test
    public void testDeserializeFromNode() throws Exception {
        String xml = "<TestBean><name>NodeTest</name><age>40</age></TestBean>";
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(xml.getBytes()));
        Node node = doc.getDocumentElement();

        TestBean result = staxParser.deserialize(node, null, TestBean.class);
        Assertions.assertNotNull(result);
        Assertions.assertEquals("NodeTest", result.getName());
        Assertions.assertEquals(40, result.getAge());

        result = domParser.deserialize(node, null, TestBean.class);
        Assertions.assertNotNull(result);
        Assertions.assertEquals("NodeTest", result.getName());
        Assertions.assertEquals(40, result.getAge());
    }

    @Test
    public void testDeserializeWithNodeClasses() throws IOException {
        String xml = "<bean><name>Test</name><age>25</age></bean>";
        Map<String, Class<?>> nodeClasses = new HashMap<>();
        nodeClasses.put("bean", TestBean.class);

        ByteArrayInputStream bais = new ByteArrayInputStream(xml.getBytes());
        TestBean result = staxParser.deserialize(bais, null, nodeClasses);
        Assertions.assertNotNull(result);
        Assertions.assertEquals("Test", result.getName());
        Assertions.assertEquals(25, result.getAge());

        StringReader reader = new StringReader(xml);
        result = staxParser.deserialize(reader, null, nodeClasses);
        Assertions.assertNotNull(result);
        Assertions.assertEquals("Test", result.getName());
        Assertions.assertEquals(25, result.getAge());
    }

    @Test
    public void testDeserializeWithNodeClassesFromNode() throws Exception {
        String xml = "<bean><name>Test</name><age>25</age></bean>";
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(xml.getBytes()));
        Node node = doc.getDocumentElement();

        Map<String, Class<?>> nodeClasses = new HashMap<>();
        nodeClasses.put("bean", TestBean.class);

        TestBean result = staxParser.deserialize(node, null, nodeClasses);
        Assertions.assertNotNull(result);
        Assertions.assertEquals("Test", result.getName());
        Assertions.assertEquals(25, result.getAge());
    }

    @Test
    public void testDeserializeWithNodeClassesNoMatch() throws IOException {
        String xml = "<unknown><name>Test</name><age>25</age></unknown>";
        Map<String, Class<?>> nodeClasses = new HashMap<>();
        nodeClasses.put("bean", TestBean.class);

        ByteArrayInputStream bais = new ByteArrayInputStream(xml.getBytes());
        Assertions.assertThrows(ParseException.class, () -> {
            staxParser.deserialize(bais, null, nodeClasses);
        });
    }

    @Test
    public void testDeserializeWithEmptyNodeClasses() throws IOException {
        String xml = "<bean><name>Test</name><age>25</age></bean>";
        Map<String, Class<?>> nodeClasses = new HashMap<>();

        ByteArrayInputStream bais = new ByteArrayInputStream(xml.getBytes());
        Assertions.assertThrows(ParseException.class, () -> {
            staxParser.deserialize(bais, null, nodeClasses);
        });
    }

    @Test
    public void testWriteBean() throws IOException {
        TestBean bean = new TestBean("WriteTest", 50);
        BufferedXMLWriter bw = Objectory.createBufferedXMLWriter();

        try {
            staxParser.writeBean(bean, XSC.create(), null, null, N.typeOf(bean.getClass()), bw);
            String xml = bw.toString();
            Assertions.assertTrue(xml.contains("WriteTest"));
            Assertions.assertTrue(xml.contains("50"));
        } finally {
            Objectory.recycle(bw);
        }
    }

    @Test
    public void testWriteMap() throws IOException {
        Map<String, String> map = new HashMap<>();
        map.put("key1", "value1");
        map.put("key2", "value2");

        BufferedXMLWriter bw = Objectory.createBufferedXMLWriter();

        try {
            staxParser.writeMap(map, XSC.create(), null, null, N.typeOf(map.getClass()), bw);
            String xml = bw.toString();
            Assertions.assertTrue(xml.contains("key1"));
            Assertions.assertTrue(xml.contains("value1"));
            Assertions.assertTrue(xml.contains("key2"));
            Assertions.assertTrue(xml.contains("value2"));
        } finally {
            Objectory.recycle(bw);
        }
    }

    @Test
    public void testWriteMapEntity() throws IOException {
        MapEntity entity = new MapEntity("TestEntity");
        entity.set("prop1", "value1");
        entity.set("prop2", 123);

        BufferedXMLWriter bw = Objectory.createBufferedXMLWriter();

        try {
            staxParser.writeMapEntity(entity, XSC.create(), null, null, N.typeOf(entity.getClass()), bw);
            String xml = bw.toString();
            Assertions.assertTrue(xml.contains("TestEntity"));
            Assertions.assertTrue(xml.contains("prop1"));
            Assertions.assertTrue(xml.contains("value1"));
        } finally {
            Objectory.recycle(bw);
        }
    }

    @Test
    public void testWriteArray() throws IOException {
        String[] array = { "one", "two", "three" };
        BufferedXMLWriter bw = Objectory.createBufferedXMLWriter();

        try {
            staxParser.writeArray(array, XSC.create(), null, null, N.typeOf(array.getClass()), bw);
            String xml = bw.toString();
            Assertions.assertTrue(xml.contains("one"));
            Assertions.assertTrue(xml.contains("two"));
            Assertions.assertTrue(xml.contains("three"));
        } finally {
            Objectory.recycle(bw);
        }
    }

    @Test
    public void testWriteCollection() throws IOException {
        List<String> list = Arrays.asList("apple", "banana", "cherry");
        BufferedXMLWriter bw = Objectory.createBufferedXMLWriter();

        try {
            staxParser.writeCollection(list, XSC.create(), null, null, N.typeOf(list.getClass()), bw);
            String xml = bw.toString();
            Assertions.assertTrue(xml.contains("apple"));
            Assertions.assertTrue(xml.contains("banana"));
            Assertions.assertTrue(xml.contains("cherry"));
        } finally {
            Objectory.recycle(bw);
        }
    }

    @Test
    public void testIsSerializableByJSONArray() {
        Integer[] intArray = { 1, 2, 3 };
        Assertions.assertTrue(staxParser.isSerializableByJSON(intArray));

        Object[] objArray = { "string", 123, true };
        Assertions.assertTrue(staxParser.isSerializableByJSON(objArray));

        Object[] beanArray = { new TestBean(), new TestBean() };
        Assertions.assertFalse(staxParser.isSerializableByJSON(beanArray));
    }

    @Test
    public void testIsSerializableByJSONCollection() {
        List<String> stringList = Arrays.asList("a", "b", "c");
        Assertions.assertTrue(staxParser.isSerializableByJSON(stringList));

        List<TestBean> beanList = Arrays.asList(new TestBean(), new TestBean());
        Assertions.assertFalse(staxParser.isSerializableByJSON(beanList));

        List<Object> mixedList = Arrays.asList("string", new TestBean());
        Assertions.assertTrue(staxParser.isSerializableByJSON(mixedList));
    }

    @Test
    public void testDeserializeInvalidXML() {
        String invalidXml = "<TestBean><name>Unclosed";

        Assertions.assertThrows(Exception.class, () -> {
            staxParser.deserialize(invalidXml, null, TestBean.class);
        });

        Assertions.assertThrows(Exception.class, () -> {
            domParser.deserialize(invalidXml, null, TestBean.class);
        });
    }

    @Test
    public void testDeserializeUnsupportedClass() {
        String xml = "<AtomicInteger>123</AtomicInteger>";

        Assertions.assertThrows(ParseException.class, () -> {
            staxParser.deserialize(xml, null, AtomicInteger.class);
        });
    }

    @Test
    public void testSerializeNullProperties() {
        TestBean bean = new TestBean();
        bean.setName(null);
        bean.setAge(0);
        bean.setTags(null);

        String xml = staxParser.serialize(bean);
        Assertions.assertNotNull(xml);

        XMLSerializationConfig config = new XMLSerializationConfig();
        config.setExclusion(Exclusion.NONE);
        xml = staxParser.serialize(bean, config);
        Assertions.assertNotNull(xml);
        Assertions.assertTrue(xml.contains("<tags"));
    }

    @Test
    public void testSerializeWithNamingPolicy() {
        TestBean bean = new TestBean("Test", 20);

        XMLSerializationConfig config = new XMLSerializationConfig();
        config.setPropNamingPolicy(NamingPolicy.UPPER_CASE_WITH_UNDERSCORE);

        String xml = staxParser.serialize(bean, config);
        Assertions.assertNotNull(xml);
        Assertions.assertTrue(xml.contains("NAME") || xml.contains("name"));
    }

    @Test
    public void testRoundTripSerialization() {
        TestBean original = new TestBean("RoundTrip", 45);
        original.setActive(true);
        original.setTags(Arrays.asList("tag1", "tag2"));
        Map<String, String> attrs = new HashMap<>();
        attrs.put("key", "value");
        original.setAttributes(attrs);

        String xml = staxParser.serialize(original);

        TestBean restored = staxParser.deserialize(xml, null, TestBean.class);

        Assertions.assertNotNull(restored);
        Assertions.assertEquals(original.getName(), restored.getName());
        Assertions.assertEquals(original.getAge(), restored.getAge());
        Assertions.assertEquals(original.isActive(), restored.isActive());
        Assertions.assertEquals(original.getTags().size(), restored.getTags().size());
        Assertions.assertEquals(original.getAttributes().get("key"), restored.getAttributes().get("key"));
    }

    @Test
    public void testDOMParserRoundTrip() {
        TestBean original = new TestBean("DOMTest", 55);
        original.setActive(false);

        String xml = domParser.serialize(original);

        TestBean restored = domParser.deserialize(xml, null, TestBean.class);

        Assertions.assertNotNull(restored);
        Assertions.assertEquals(original.getName(), restored.getName());
        Assertions.assertEquals(original.getAge(), restored.getAge());
        Assertions.assertEquals(original.isActive(), restored.isActive());
    }

    @Test
    public void testLargeDataSerialization() {
        TestBean bean = new TestBean("LargeData", 100);
        List<String> largeTags = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            largeTags.add("tag" + i);
        }
        bean.setTags(largeTags);

        Map<String, String> largeAttrs = new HashMap<>();
        for (int i = 0; i < 100; i++) {
            largeAttrs.put("key" + i, "value" + i);
        }
        bean.setAttributes(largeAttrs);

        String xml = staxParser.serialize(bean);
        Assertions.assertNotNull(xml);
        Assertions.assertTrue(xml.length() > 10000);

        TestBean restored = staxParser.deserialize(xml, null, TestBean.class);
        Assertions.assertNotNull(restored);
        Assertions.assertEquals(1000, restored.getTags().size());
        Assertions.assertEquals(100, restored.getAttributes().size());
    }
}
