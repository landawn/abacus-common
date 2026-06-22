package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.annotation.JsonXmlField;
import com.landawn.abacus.parser.entity.RecordB;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Strings;

public class AbacusXmlParserImplTest extends TestBase {

    @TempDir
    Path tempDir;

    private XmlParser staxParser;
    private XmlParser domParser;

    public static class Person {
        private String name;
        private int age;

        public Person() {
        }

        public Person(final String name, final int age) {
            this.name = name;
            this.age = age;
        }

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(final int age) {
            this.age = age;
        }
    }

    public static class RawXmlBean {
        private String name;

        @JsonXmlField(isJsonRawValue = true)
        private String payload;

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public String getPayload() {
            return payload;
        }

        public void setPayload(final String payload) {
            this.payload = payload;
        }
    }

    public static class CircularRefBean {
        private String name;
        private CircularRefBean reference;

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public CircularRefBean getReference() {
            return reference;
        }

        public void setReference(final CircularRefBean reference) {
            this.reference = reference;
        }
    }

    @BeforeEach
    public void setUp() {
        if (ParserFactory.isAbacusXmlParserAvailable()) {
            staxParser = new AbacusXmlParserImpl(XmlParserType.StAX);
            domParser = new AbacusXmlParserImpl(XmlParserType.DOM);
        }
    }

    public static class Utf16Bean {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }
    }

    public static class RecordWrapper {
        private String name;
        private RecordB rec;

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public RecordB getRec() {
            return rec;
        }

        public void setRec(final RecordB rec) {
            this.rec = rec;
        }
    }

    @Test
    public void bugFix_sax_nestedImmutableRecord_isBound() {
        if (!ParserFactory.isAbacusXmlParserAvailable()) {
            return;
        }

        final RecordWrapper outer = new RecordWrapper();
        outer.setName("OUTER");
        outer.setRec(new RecordB(7, "first", "last"));

        final String xml = staxParser.serialize(outer);
        final XmlDeserConfig xdc = new XmlDeserConfig();

        final XmlParser saxParser = new AbacusXmlParserImpl(XmlParserType.SAX);
        final RecordWrapper fromSax = saxParser.deserialize(xml, xdc, RecordWrapper.class);

        // The nested immutable record must be the finished record, not the raw constructor-arg intermediate.
        assertNotNull(fromSax.getRec());
        assertEquals(new RecordB(7, "first", "last"), fromSax.getRec());
        assertEquals("OUTER", fromSax.getName());

        // And SAX must agree with the reference StAX/DOM parsers.
        final RecordWrapper fromStax = staxParser.deserialize(xml, xdc, RecordWrapper.class);
        final RecordWrapper fromDom = domParser.deserialize(xml, xdc, RecordWrapper.class);
        assertEquals(fromStax.getRec(), fromSax.getRec());
        assertEquals(fromDom.getRec(), fromSax.getRec());
    }

    @Test
    public void test_constructor_default() {
        if (ParserFactory.isAbacusXmlParserAvailable()) {
            XmlParser parser = new AbacusXmlParserImpl(XmlParserType.StAX);
            assertNotNull(parser);
        }
    }

    @Test
    public void test_constructor_withConfig() {
        if (ParserFactory.isAbacusXmlParserAvailable()) {
            XmlSerConfig xsc = new XmlSerConfig();
            XmlDeserConfig xdc = new XmlDeserConfig();
            XmlParser parser = new AbacusXmlParserImpl(XmlParserType.StAX, xsc, xdc);
            assertNotNull(parser);
        }
    }

    @Test
    public void test_serialize_string() {
        if (ParserFactory.isAbacusXmlParserAvailable()) {
            XmlParser parser = new AbacusXmlParserImpl(XmlParserType.StAX);
            String xml = parser.serialize("test");
            assertNotNull(xml);
        }
    }

    @Test
    public void testSerializeJsonRawValueWritesPayloadVerbatim() {
        if (ParserFactory.isAbacusXmlParserAvailable()) {
            final RawXmlBean bean = new RawXmlBean();
            bean.setName("doc");
            bean.setPayload("{\"k\":\"v\"}");

            String xml = staxParser.serialize(bean);
            assertTrue(xml.contains("{\"k\":\"v\"}"), xml);
            assertTrue(!xml.contains("\\\"k\\\""), xml);

            xml = domParser.serialize(bean);
            assertTrue(xml.contains("{\"k\":\"v\"}"), xml);
            assertTrue(!xml.contains("\\\"k\\\""), xml);
        }
    }

    @Test
    public void testSerializeToString() {
        Person person = new Person();
        person.setName("John");
        person.setAge(30);

        String xml = staxParser.serialize(person);
        assertNotNull(xml);
        assertTrue(xml.contains("John"));
        assertTrue(xml.contains("30"));
    }

    @Test
    public void testSerializeWithConfig() {
        Person person = new Person();
        person.setName("Jane");
        person.setAge(25);

        XmlSerConfig config = new XmlSerConfig().setPrettyFormat(true).setTagByPropertyName(true);

        String xml = staxParser.serialize(person, config);
        assertNotNull(xml);
        assertTrue(xml.contains("Jane"));
        assertTrue(xml.contains("25"));
    }

    @Test
    public void testSerializeNull() {
        String xml = staxParser.serialize(null);
        assertEquals("", xml);
    }

    @Test
    public void testSerializePrimitiveArray() {
        if (ParserFactory.isAbacusXmlParserAvailable()) {
            final int[] values = { 1, 2, 3 };

            String xml = staxParser.serialize(values);
            assertNotNull(xml);
            assertTrue(xml.contains("1"));
            assertTrue(xml.contains("2"));
            assertTrue(xml.contains("3"));

            xml = domParser.serialize(values);
            assertNotNull(xml);
            assertTrue(xml.contains("1"));
            assertTrue(xml.contains("2"));
            assertTrue(xml.contains("3"));
        }
    }

    @Test
    public void testSerializeCollection() {
        List<String> list = Arrays.asList("one", "two", "three");

        String xml = staxParser.serialize(list);
        assertNotNull(xml);
        assertTrue(xml.contains("one"));
        assertTrue(xml.contains("two"));
        assertTrue(xml.contains("three"));
    }

    @Test
    public void testSerializeMap() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);

        String xml = staxParser.serialize(map);
        assertNotNull(xml);
        assertTrue(xml.contains("a"));
        assertTrue(xml.contains("1"));
        assertTrue(xml.contains("b"));
        assertTrue(xml.contains("2"));
    }

    @Test
    public void testSerializeArray() {
        String[] array = { "x", "y", "z" };

        String xml = staxParser.serialize(array);
        assertNotNull(xml);
        assertTrue(xml.contains("x"));
        assertTrue(xml.contains("y"));
        assertTrue(xml.contains("z"));
    }

    @Test
    public void testSerializeWithPrettyFormat() {
        Person person = new Person("Pretty", 25);

        XmlSerConfig config = new XmlSerConfig().setPrettyFormat(true);
        String xml = staxParser.serialize(person, config);
        assertNotNull(xml);
        assertTrue(xml.contains("Pretty"));
    }

    @Test
    public void testSerializeUsesDefaultConfigForCircularReference() {
        if (!ParserFactory.isAbacusXmlParserAvailable()) {
            return;
        }

        XmlSerConfig config = new XmlSerConfig().setSupportCircularReference(true);
        XmlParser parser = new AbacusXmlParserImpl(XmlParserType.StAX, config, null);
        CircularRefBean bean = new CircularRefBean();
        bean.setName("cycle");
        bean.setReference(bean);

        String xml = parser.serialize(bean);

        assertNotNull(xml);
        assertTrue(xml.contains("cycle"));
    }

    @Test
    public void testSerializeBigValues() {
        String longString = Strings.repeat(Strings.uuid(), 1000);

        {
            String[] array = { "a", "b", longString };

            String xml = staxParser.serialize(array, XmlSerConfig.create().setWriteTypeInfo(true));
            assertNotNull(xml);
            N.println(xml);

            String[] array2 = staxParser.deserialize(xml, String[].class);
            assertArrayEquals(array, array2);
        }

        {
            List<String> coll = N.toList("a", "b", longString);

            String xml = staxParser.serialize(coll, XmlSerConfig.create().setWriteTypeInfo(true));
            assertNotNull(xml);
            N.println(xml);

            List<String> coll2 = staxParser.deserialize(xml, List.class);
            assertEquals(coll, coll2);
        }

        {
            Set<String> coll = N.toSet("a", "b", longString);

            String xml = staxParser.serialize(coll, XmlSerConfig.create().setWriteTypeInfo(true));
            assertNotNull(xml);
            N.println(xml);

            Set<String> coll2 = staxParser.deserialize(xml, Set.class);
            assertEquals(coll, coll2);
        }

        {
            Queue<String> coll = N.toQueue("a", "b", longString);

            String xml = staxParser.serialize(coll, XmlSerConfig.create().setWriteTypeInfo(true));
            assertNotNull(xml);
            N.println(xml);

            Queue<String> coll2 = staxParser.deserialize(xml, ArrayDeque.class);
            assertHaveSameElements(coll, coll2);
        }

        {
            Map<String, Object> map = N.asMap("a", 1, "b", "2", "c", longString, "map", map = N.asMap("a", 1, "b", "2", "c", longString), "list",
                    N.toList("a", "b", longString), "listMap", N.asMap("a", N.toList("1", "2", longString), "b", N.toList("3", "4")));

            String xml = staxParser.serialize(map, XmlSerConfig.create().setWriteTypeInfo(true));
            assertNotNull(xml);
            N.println(xml);

            Map<String, String> map2 = staxParser.deserialize(xml, Map.class);
            assertEquals(map, map2);
        }

    }

    @Test
    public void testSerializeWriteTypeInfoAndTagByPropertyName() {
        if (!ParserFactory.isAbacusXmlParserAvailable()) {
            return;
        }
        Person person = new Person("WriteTypeProp", 91);
        XmlSerConfig config = new XmlSerConfig().setWriteTypeInfo(true).setTagByPropertyName(true);

        String xml = staxParser.serialize(person, config);
        assertNotNull(xml);
        assertTrue(xml.contains("WriteTypeProp"));
        assertTrue(xml.contains("91"));
    }

    @Test
    public void testRoundTrip_domParser() {
        if (!ParserFactory.isAbacusXmlParserAvailable()) {
            return;
        }
        Person original = new Person("DomRoundTrip", 101);
        String xml = domParser.serialize(original);
        Person restored = domParser.deserialize(xml, Person.class);
        assertNotNull(restored);
        assertEquals("DomRoundTrip", restored.getName());
        assertEquals(101, restored.getAge());
    }

    @Test
    public void testSerializeToFile() throws IOException {
        Person person = new Person();
        person.setName("Bob");
        person.setAge(35);

        File tempFile = File.createTempFile("test", ".xml");
        tempFile.deleteOnExit();

        staxParser.serialize(person, tempFile);

        assertTrue(tempFile.exists());
        assertTrue(tempFile.length() > 0);
    }

    @Test
    public void testSerializeToOutputStream() throws IOException {
        Person person = new Person();
        person.setName("Alice");
        person.setAge(28);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        staxParser.serialize(person, baos);

        String xml = baos.toString();
        assertNotNull(xml);
        assertTrue(xml.contains("Alice"));
    }

    @Test
    public void testSerializeToWriter() throws IOException {
        Person person = new Person();
        person.setName("Charlie");
        person.setAge(40);

        StringWriter writer = new StringWriter();
        staxParser.serialize(person, writer);

        String xml = writer.toString();
        assertNotNull(xml);
        assertTrue(xml.contains("Charlie"));
    }

    @Test
    public void testSerializeWithConfigToOutputStream() throws IOException {
        Person person = new Person("ConfigOS", 30);

        XmlSerConfig config = new XmlSerConfig().setTagByPropertyName(true);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        staxParser.serialize(person, config, baos);

        String xml = baos.toString();
        assertNotNull(xml);
        assertTrue(xml.contains("ConfigOS"));
    }

    @Test
    public void testSerializeWithConfigToWriter() throws IOException {
        Person person = new Person("ConfigWriter", 35);

        XmlSerConfig config = new XmlSerConfig().setTagByPropertyName(true);
        StringWriter writer = new StringWriter();
        staxParser.serialize(person, config, writer);

        String xml = writer.toString();
        assertNotNull(xml);
        assertTrue(xml.contains("ConfigWriter"));
    }

    @Test
    public void testSerializeWithConfigToFile() throws IOException {
        Person person = new Person("ConfigFile", 40);

        XmlSerConfig config = new XmlSerConfig().setTagByPropertyName(true);
        File tempFile = File.createTempFile("test-cfg", ".xml");
        tempFile.deleteOnExit();

        staxParser.serialize(person, config, tempFile);
        assertTrue(tempFile.exists());
        assertTrue(tempFile.length() > 0);
    }

    @Test
    public void testDeserializeFromString() {
        String xml = "<bean><name>David</name><age>45</age></bean>";

        Person person = staxParser.deserialize(xml, Person.class);
        assertNotNull(person);
        assertEquals("David", person.getName());
        assertEquals(45, person.getAge());
    }

    @Test
    public void testDeserializeCdataBeanProperty_staxPath() {
        // Regression: the StAX deserialization path must treat CDATA events as text.
        // Because the StAX reader is not coalescing, <![CDATA[...]]> arrives as a
        // separate CDATA event; if only CHARACTERS were recognized the value was
        // silently dropped (deserialized to null / empty).
        final String xml = "<bean><name><![CDATA[hello & <world>]]></name><age>7</age></bean>";

        final Person person = staxParser.deserialize(xml, Person.class);
        assertNotNull(person);
        assertEquals("hello & <world>", person.getName());
        assertEquals(7, person.getAge());
    }

    @Test
    public void testDeserializeMixedCdataAndCharacters_staxPath() {
        // Regression: a mixed text node where plain CHARACTERS and CDATA are interleaved
        // must be concatenated across the CHARACTERS/CDATA event boundaries.
        final String xml = "<bean><name>pre<![CDATA[mid]]>post</name><age>9</age></bean>";

        final Person person = staxParser.deserialize(xml, Person.class);
        assertNotNull(person);
        assertEquals("premidpost", person.getName());
        assertEquals(9, person.getAge());
    }

    @Test
    public void testDeserializeWithConfig() {
        String xml = "<person><name>Emma</name><age>33</age></person>";

        XmlDeserConfig config = new XmlDeserConfig().setIgnoreUnmatchedProperty(true);

        Person person = staxParser.deserialize(xml, config, Person.class);
        assertNotNull(person);
    }

    @Test
    public void testDeserializeFromStringWithType() {
        String xml = "<bean><name>TypeTest</name><age>55</age></bean>";

        Person person = staxParser.deserialize(xml, null, Type.of(Person.class));
        assertNotNull(person);
        assertEquals("TypeTest", person.getName());
        assertEquals(55, person.getAge());
    }

    @Test
    public void test_deserialize_file_honors_xml_encoding_utf16() throws Exception {
        if (ParserFactory.isAbacusXmlParserAvailable()) {
            XmlParser parser = new AbacusXmlParserImpl(XmlParserType.StAX);
            File file = tempDir.resolve("abacus-utf16.xml").toFile();
            Files.writeString(file.toPath(), "<?xml version=\"1.0\" encoding=\"UTF-16\"?><bean><name>\u4f60\u597d</name></bean>", StandardCharsets.UTF_16);

            Utf16Bean bean = parser.deserialize(file, Utf16Bean.class);
            assertEquals("\u4f60\u597d", bean.getName());
        }
    }

    @Test
    public void test_deserialize_file_with_node_types_honors_xml_encoding_utf16() throws Exception {
        if (ParserFactory.isAbacusXmlParserAvailable()) {
            XmlParser parser = new AbacusXmlParserImpl(XmlParserType.StAX);
            File file = tempDir.resolve("abacus-utf16-node-types.xml").toFile();
            Files.writeString(file.toPath(), "<?xml version=\"1.0\" encoding=\"UTF-16\"?><bean><name>\u4e16\u754c</name></bean>", StandardCharsets.UTF_16);

            Map<String, Type<?>> nodeTypes = new HashMap<>();
            nodeTypes.put("bean", Type.of(Utf16Bean.class));

            Utf16Bean bean = parser.deserialize(file, null, nodeTypes);
            assertEquals("\u4e16\u754c", bean.getName());
        }
    }

    @Test
    public void testDeserializeFromFile() throws IOException {
        String xml = "<bean><name>Frank</name><age>50</age></bean>";

        File tempFile = File.createTempFile("test", ".xml");
        tempFile.deleteOnExit();

        try (FileWriter fw = new FileWriter(tempFile)) {
            fw.write(xml);
        }

        Person person = staxParser.deserialize(tempFile, Person.class);
        assertNotNull(person);
        assertEquals("Frank", person.getName());
        assertEquals(50, person.getAge());
    }

    @Test
    public void testDeserializeFromInputStream() throws IOException {
        String xml = "<bean><name>Grace</name><age>27</age></bean>";
        ByteArrayInputStream bais = new ByteArrayInputStream(xml.getBytes());

        Person person = staxParser.deserialize(bais, Person.class);
        assertNotNull(person);
        assertEquals("Grace", person.getName());
        assertEquals(27, person.getAge());
    }

    @Test
    public void testDeserializeFromReader() throws IOException {
        String xml = "<bean><name>Henry</name><age>38</age></bean>";
        StringReader reader = new StringReader(xml);

        Person person = staxParser.deserialize(reader, Person.class);
        assertNotNull(person);
        assertEquals("Henry", person.getName());
        assertEquals(38, person.getAge());
    }

    @Test
    public void testDeserializeFromNode() throws Exception {
        String xml = "<bean><name>Henry</name><age>38</age></bean>";

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(xml.getBytes()));
        Node node = doc.getFirstChild();

        Person person = domParser.deserialize(node, Person.class);
        assertNotNull(person);
        assertEquals("Henry", person.getName());
        assertEquals(38, person.getAge());
    }

    @Test
    public void testDeserializeWithNodeClasses() throws IOException {
        String xml = "<person><name>Jack</name><age>31</age></person>";

        Map<String, Type<?>> nodeClasses = Map.of("person", Type.of(Person.class));

        ByteArrayInputStream bais = new ByteArrayInputStream(xml.getBytes());
        Person person = staxParser.deserialize(bais, new XmlDeserConfig(), nodeClasses);

        assertNotNull(person);
        assertEquals("Jack", person.getName());
        assertEquals(31, person.getAge());
    }

    @Test
    public void testDeserializeFromFileWithType() throws IOException {
        String xml = "<bean><name>FileType</name><age>66</age></bean>";

        File tempFile = File.createTempFile("test-type", ".xml");
        tempFile.deleteOnExit();

        try (FileWriter fw = new FileWriter(tempFile)) {
            fw.write(xml);
        }

        Person person = staxParser.deserialize(tempFile, null, Type.of(Person.class));
        assertNotNull(person);
        assertEquals("FileType", person.getName());
        assertEquals(66, person.getAge());
    }

    @Test
    public void testDeserializeFromInputStreamWithType() throws IOException {
        String xml = "<bean><name>StreamType</name><age>77</age></bean>";
        ByteArrayInputStream bais = new ByteArrayInputStream(xml.getBytes());

        Person person = staxParser.deserialize(bais, null, Type.of(Person.class));
        assertNotNull(person);
        assertEquals("StreamType", person.getName());
        assertEquals(77, person.getAge());
    }

    @Test
    public void testDeserializeFromReaderWithType() throws IOException {
        String xml = "<bean><name>ReaderType</name><age>88</age></bean>";
        StringReader reader = new StringReader(xml);

        Person person = staxParser.deserialize(reader, null, Type.of(Person.class));
        assertNotNull(person);
        assertEquals("ReaderType", person.getName());
        assertEquals(88, person.getAge());
    }

    @Test
    public void testDeserializeFromNodeWithType() throws Exception {
        String xml = "<bean><name>NodeType</name><age>99</age></bean>";

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(xml.getBytes()));
        Node node = doc.getFirstChild();

        Person person = domParser.deserialize(node, Type.of(Person.class));
        assertNotNull(person);
        assertEquals("NodeType", person.getName());
        assertEquals(99, person.getAge());
    }

    @Test
    public void testDeserializeFromReaderWithNodeTypes() throws IOException {
        String xml = "<person><name>ReaderNode</name><age>50</age></person>";
        StringReader reader = new StringReader(xml);

        Map<String, Type<?>> nodeTypes = Map.of("person", Type.of(Person.class));
        Person person = staxParser.deserialize(reader, new XmlDeserConfig(), nodeTypes);

        assertNotNull(person);
        assertEquals("ReaderNode", person.getName());
        assertEquals(50, person.getAge());
    }

    @Test
    public void testDeserializeFromNodeWithConfig() throws Exception {
        String xml = "<bean><name>NodeConfig</name><age>60</age></bean>";

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(xml.getBytes()));
        Node node = doc.getFirstChild();

        XmlDeserConfig config = new XmlDeserConfig().setIgnoreUnmatchedProperty(true);
        Person person = domParser.deserialize(node, config, Person.class);
        assertNotNull(person);
        assertEquals("NodeConfig", person.getName());
        assertEquals(60, person.getAge());
    }

    @Test
    public void testDeserializeFromNodeWithNodeTypes() throws Exception {
        String xml = "<person><name>NodeTypes</name><age>70</age></person>";

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(xml.getBytes()));
        Node node = doc.getFirstChild();

        Map<String, Type<?>> nodeTypes = Map.of("person", Type.of(Person.class));
        Person person = domParser.deserialize(node, new XmlDeserConfig(), nodeTypes);
        assertNotNull(person);
        assertEquals("NodeTypes", person.getName());
        assertEquals(70, person.getAge());
    }

    @Test
    public void testDeserializeFromFileWithNodeTypes() throws IOException {
        String xml = "<person><name>FileNode</name><age>80</age></person>";

        File tempFile = File.createTempFile("test-node", ".xml");
        tempFile.deleteOnExit();

        try (FileWriter fw = new FileWriter(tempFile)) {
            fw.write(xml);
        }

        Map<String, Type<?>> nodeTypes = Map.of("person", Type.of(Person.class));
        Person person = staxParser.deserialize(tempFile, new XmlDeserConfig(), nodeTypes);

        assertNotNull(person);
        assertEquals("FileNode", person.getName());
        assertEquals(80, person.getAge());
    }

    @Test
    public void testDeserialize_NodeTypesWithNameAttribute_ThrowsNullPointerException() {
        String xml = "<entry name=\"person\"><name>AttrRoot</name><age>64</age></entry>";
        Map<String, Type<?>> nodeTypes = Map.of("person", Type.of(Person.class));

        assertThrows(NullPointerException.class,
                () -> staxParser.deserialize(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)), new XmlDeserConfig(), nodeTypes));
    }

    // TODO: testReadByDOMParser_mapType requires specific XML format for Map deserialization

    @Test
    public void testDeserializeInputStream_withNullConfig() throws IOException {
        if (!ParserFactory.isAbacusXmlParserAvailable()) {
            return;
        }
        String xml = "<bean><name>NullCfg</name><age>41</age></bean>";
        ByteArrayInputStream bais = new ByteArrayInputStream(xml.getBytes());

        Person result = staxParser.deserialize(bais, (XmlDeserConfig) null, Person.class);
        assertNotNull(result);
        assertEquals("NullCfg", result.getName());
        assertEquals(41, result.getAge());
    }

    @Test
    public void testDeserializeReader_withNullConfig() throws IOException {
        if (!ParserFactory.isAbacusXmlParserAvailable()) {
            return;
        }
        String xml = "<bean><name>ReaderNullCfg</name><age>51</age></bean>";
        StringReader reader = new StringReader(xml);

        Person result = staxParser.deserialize(reader, (XmlDeserConfig) null, Person.class);
        assertNotNull(result);
        assertEquals("ReaderNullCfg", result.getName());
        assertEquals(51, result.getAge());
    }

    @Test
    public void testDeserializeInputStream_withNodeTypes_dom() throws IOException {
        if (!ParserFactory.isAbacusXmlParserAvailable()) {
            return;
        }
        String xml = "<person><name>DomNodeTypes</name><age>61</age></person>";
        Map<String, Type<?>> nodeTypes = Map.of("person", Type.of(Person.class));
        ByteArrayInputStream bais = new ByteArrayInputStream(xml.getBytes());

        Person result = domParser.deserialize(bais, new XmlDeserConfig(), nodeTypes);
        assertNotNull(result);
        assertEquals("DomNodeTypes", result.getName());
        assertEquals(61, result.getAge());
    }

    @Test
    public void testDeserializeReader_withNodeTypes_dom() throws IOException {
        if (!ParserFactory.isAbacusXmlParserAvailable()) {
            return;
        }
        String xml = "<person><name>ReaderDom</name><age>71</age></person>";
        Map<String, Type<?>> nodeTypes = Map.of("person", Type.of(Person.class));
        StringReader reader = new StringReader(xml);

        Person result = domParser.deserialize(reader, new XmlDeserConfig(), nodeTypes);
        assertNotNull(result);
        assertEquals("ReaderDom", result.getName());
        assertEquals(71, result.getAge());
    }

    @Test
    public void testDeserializeWithNodeTypes_withTypeInfo() throws IOException {
        if (!ParserFactory.isAbacusXmlParserAvailable()) {
            return;
        }
        // Serialize with type info then deserialize
        Person p = new Person("WithType", 81);
        String xml = staxParser.serialize(p, XmlSerConfig.create().setWriteTypeInfo(true));
        assertNotNull(xml);

        Person result = staxParser.deserialize(xml, Person.class);
        assertNotNull(result);
        assertEquals("WithType", result.getName());
        assertEquals(81, result.getAge());
    }

    @Test
    public void testReadByStreamParser_beanType() throws Exception {
        if (!ParserFactory.isAbacusXmlParserAvailable()) {
            return;
        }
        AbacusXmlParserImpl impl = (AbacusXmlParserImpl) staxParser;
        String xml = "<bean><name>StreamParse</name><age>21</age></bean>";

        javax.xml.stream.XMLStreamReader xmlReader = javax.xml.stream.XMLInputFactory.newFactory().createXMLStreamReader(new StringReader(xml));
        while (xmlReader.hasNext() && xmlReader.getEventType() != javax.xml.stream.XMLStreamConstants.START_ELEMENT) {
            xmlReader.next();
        }

        Object result = impl.readByStreamParser(xmlReader, new XmlDeserConfig(), Type.of(Person.class));
        assertNotNull(result);
        assertTrue(result instanceof Person);
        assertEquals("StreamParse", ((Person) result).getName());
        assertEquals(21, ((Person) result).getAge());
    }

    // TODO: testReadByStreamParser_mapType requires careful positioning of XMLStreamReader

    @Test
    public void testReadByDOMParser_beanType() throws Exception {
        if (!ParserFactory.isAbacusXmlParserAvailable()) {
            return;
        }
        AbacusXmlParserImpl impl = (AbacusXmlParserImpl) domParser;
        String xml = "<bean><name>DOMParsed</name><age>31</age></bean>";

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(xml.getBytes()));
        Node node = doc.getFirstChild();

        Object result = impl.readByDOMParser(node, new XmlDeserConfig(), Type.of(Person.class));
        assertNotNull(result);
        assertTrue(result instanceof Person);
        assertEquals("DOMParsed", ((Person) result).getName());
        assertEquals(31, ((Person) result).getAge());
    }

    @Test
    public void testDeserializeInputStream_withNodeTypes_stax() throws IOException {
        if (!ParserFactory.isAbacusXmlParserAvailable()) {
            return;
        }
        String xml = "<person><name>InputStreamNode</name><age>72</age></person>";
        Map<String, Type<?>> nodeTypes = new HashMap<>();
        nodeTypes.put("person", Type.of(Person.class));

        ByteArrayInputStream bais = new ByteArrayInputStream(xml.getBytes());
        Person result = staxParser.deserialize(bais, new XmlDeserConfig(), nodeTypes);

        assertNotNull(result);
        assertEquals("InputStreamNode", result.getName());
        assertEquals(72, result.getAge());
    }

    // TODO: Remaining AbacusXmlParserImpl.XmlSAXHandler gaps are in internal recursive SAX startElement/endElement branches that are not practical to isolate without purpose-built parser fixtures.

    // -----------------------------------------------------------------------
    // Bug-fix regression tests – Bug 2: stale StringBuilder drops first CHARACTERS chunk
    // -----------------------------------------------------------------------

    /**
     * Bug fix: the StAX readByStreamParserBody() method accumulates split CHARACTERS
     * events in a shared StringBuilder {@code sb}.  After each accumulation the
     * builder is cleared with {@code sb.setLength(0)}, but ONLY when
     * {@code sb.length() > text.length()}.  When that condition is false {@code sb}
     * retains stale content.  On the next property whose value also triggers
     * multi-CHARACTERS accumulation the {@code else if (sb.isEmpty())} branch was
     * skipped (because sb was non-empty), so the initial {@code text} fragment was
     * silently dropped.
     *
     * The fix restructures the branch so {@code text} is always prepended when
     * {@code sb} is empty, regardless of whether it was explicitly cleared or was
     * freshly allocated.
     *
     * This test validates that a bean with two String properties round-trips
     * correctly via StAX – if either property's first text-chunk is lost the
     * assertion will fail.
     */
    @Test
    public void bugFix_staleStringBuilder_multipleStringProps_roundTripsCorrectly() {
        if (!ParserFactory.isAbacusXmlParserAvailable()) {
            return;
        }
        // Use a bean with two distinct non-empty string fields so any silent
        // truncation of either field is immediately detectable.
        Person original = new Person("GivenName", 42);
        XmlParser parser = new AbacusXmlParserImpl(XmlParserType.StAX);

        String xml = parser.serialize(original, new XmlSerConfig().setTagByPropertyName(true));
        assertNotNull(xml);

        Person restored = parser.deserialize(xml, new XmlDeserConfig(), Person.class);
        assertNotNull(restored);
        assertEquals(original.getName(), restored.getName(), "name must survive the StAX round-trip; stale-sb bug would truncate it");
        assertEquals(original.getAge(), restored.getAge(), "age must survive the StAX round-trip");
    }

    /**
     * Same StAX round-trip for a list of strings to exercise the
     * COLLECTION/CHARACTERS accumulation path which had the identical stale-sb bug.
     */
    @Test
    public void bugFix_staleStringBuilder_collectionOfStrings_roundTripsCorrectly() {
        if (!ParserFactory.isAbacusXmlParserAvailable()) {
            return;
        }
        XmlParser parser = new AbacusXmlParserImpl(XmlParserType.StAX);
        List<String> original = Arrays.asList("firstElement_withSomeLengthToExerciseSbAccumulation", "secondElement_alsoWithSomeLengthToExerciseSbAccumulation",
                "thirdElement");

        String xml = parser.serialize(original, new XmlSerConfig().setTagByPropertyName(true).setWriteTypeInfo(true));
        assertNotNull(xml);

        @SuppressWarnings("unchecked")
        List<String> restored = parser.deserialize(xml, new XmlDeserConfig().setElementType(String.class), List.class);

        assertNotNull(restored);
        assertEquals(original.size(), restored.size(), "Restored list must have same size");
        for (int i = 0; i < original.size(); i++) {
            assertEquals(original.get(i), restored.get(i), "Element " + i + " must survive the StAX round-trip");
        }
    }

    /**
     * Verify the InputStream StAX path also round-trips correctly.
     */
    @Test
    public void bugFix_staleStringBuilder_staxFromInputStream_roundTripsCorrectly() {
        if (!ParserFactory.isAbacusXmlParserAvailable()) {
            return;
        }
        XmlParser parser = new AbacusXmlParserImpl(XmlParserType.StAX);
        Person original = new Person("InputFirst", 99);

        String xml = parser.serialize(original, new XmlSerConfig().setTagByPropertyName(true));
        assertNotNull(xml);

        ByteArrayInputStream bais = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
        Person restored = parser.deserialize(bais, new XmlDeserConfig(), Person.class);
        assertNotNull(restored);
        assertEquals(original.getName(), restored.getName());
        assertEquals(original.getAge(), restored.getAge());
    }

    /**
     * Verify the Reader StAX path also round-trips correctly.
     */
    @Test
    public void bugFix_staleStringBuilder_staxFromReader_roundTripsCorrectly() {
        if (!ParserFactory.isAbacusXmlParserAvailable()) {
            return;
        }
        XmlParser parser = new AbacusXmlParserImpl(XmlParserType.StAX);
        Person original = new Person("ReaderFirst", 55);

        String xml = parser.serialize(original, new XmlSerConfig().setTagByPropertyName(true));
        assertNotNull(xml);

        StringReader reader = new StringReader(xml);
        Person restored = parser.deserialize(reader, new XmlDeserConfig(), Person.class);
        assertNotNull(restored);
        assertEquals(original.getName(), restored.getName());
        assertEquals(original.getAge(), restored.getAge());
    }

    // ==================================================================================
    // Regression tests: ignored-property / ignored-key handling in the SAX parser.
    //
    // Before the fix, when deserializing with XmlDeserConfig.setIgnoredPropNames(...):
    //   (1) a nested bean with two or more ignored properties serialized with
    //       tagByPropertyName=false left a stale entry on the internal property-name
    //       stack, so the enclosing bean's following properties were bound under the
    //       wrong name (the nested bean was dropped, sibling properties got garbage);
    //   (2) an ignored map key left the internal "ignore" counter stuck at 1, which
    //       silently swallowed every entry that followed the first ignored key.
    // In both cases the SAX result diverged from the (correct) StAX/DOM result.
    // ==================================================================================

    public static class IgnInner {
        private String keep;
        private String drop1;
        private String drop2;
        private String after;

        public String getKeep() {
            return keep;
        }

        public void setKeep(final String keep) {
            this.keep = keep;
        }

        public String getDrop1() {
            return drop1;
        }

        public void setDrop1(final String drop1) {
            this.drop1 = drop1;
        }

        public String getDrop2() {
            return drop2;
        }

        public void setDrop2(final String drop2) {
            this.drop2 = drop2;
        }

        public String getAfter() {
            return after;
        }

        public void setAfter(final String after) {
            this.after = after;
        }
    }

    public static class IgnOuter {
        private String name;
        private IgnInner inner;
        private String tail;

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public IgnInner getInner() {
            return inner;
        }

        public void setInner(final IgnInner inner) {
            this.inner = inner;
        }

        public String getTail() {
            return tail;
        }

        public void setTail(final String tail) {
            this.tail = tail;
        }
    }

    @Test
    public void bugFix_sax_nestedBean_twoIgnoredProps_tagByValue() {
        if (!ParserFactory.isAbacusXmlParserAvailable()) {
            return;
        }

        final IgnInner inner = new IgnInner();
        inner.setKeep("K");
        inner.setDrop1("D1");
        inner.setDrop2("D2");
        inner.setAfter("A");
        final IgnOuter outer = new IgnOuter();
        outer.setName("OUTER");
        outer.setInner(inner);
        outer.setTail("TAIL");

        // tagByPropertyName=false -> <property name="...">value</property> form.
        final String xml = staxParser.serialize(outer, new XmlSerConfig().setTagByPropertyName(false));

        final XmlDeserConfig xdc = new XmlDeserConfig().setIgnoredPropNames(IgnInner.class, Set.of("drop1", "drop2"));

        final XmlParser saxParser = new AbacusXmlParserImpl(XmlParserType.SAX);
        final IgnOuter fromSax = saxParser.deserialize(xml, xdc, IgnOuter.class);

        // The nested bean must still be bound, with the two ignored props left null and the rest intact.
        assertNotNull(fromSax.getInner());
        assertEquals("OUTER", fromSax.getName());
        assertEquals("TAIL", fromSax.getTail());
        assertEquals("K", fromSax.getInner().getKeep());
        assertEquals("A", fromSax.getInner().getAfter());
        assertNull(fromSax.getInner().getDrop1());
        assertNull(fromSax.getInner().getDrop2());

        // And SAX must agree with the reference StAX/DOM parsers.
        final IgnOuter fromStax = staxParser.deserialize(xml, xdc, IgnOuter.class);
        final IgnOuter fromDom = domParser.deserialize(xml, xdc, IgnOuter.class);
        assertEquals(fromStax.getName(), fromSax.getName());
        assertEquals(fromStax.getTail(), fromSax.getTail());
        assertEquals(fromStax.getInner().getKeep(), fromSax.getInner().getKeep());
        assertEquals(fromStax.getInner().getAfter(), fromSax.getInner().getAfter());
        assertEquals(fromDom.getInner().getKeep(), fromSax.getInner().getKeep());
    }

    @Test
    public void bugFix_sax_flatMap_ignoredKeys_keepsLaterEntries() {
        if (!ParserFactory.isAbacusXmlParserAvailable()) {
            return;
        }

        final Map<String, Object> map = new LinkedHashMap<>();
        map.put("a", "1");
        map.put("drop1", "X");
        map.put("drop2", "Y");
        map.put("b", "2");

        final String xml = staxParser.serialize(map);
        final XmlDeserConfig xdc = new XmlDeserConfig().setIgnoredPropNames(Map.class, Set.of("drop1", "drop2"));

        final XmlParser saxParser = new AbacusXmlParserImpl(XmlParserType.SAX);
        final Map<String, Object> fromSax = saxParser.deserialize(xml, xdc, Map.class);

        // The entry after the ignored keys must survive (previously "b" was swallowed).
        assertEquals(N.asMap("a", "1", "b", "2"), fromSax);
        assertEquals(staxParser.deserialize(xml, xdc, Map.class), fromSax);
        assertEquals(domParser.deserialize(xml, xdc, Map.class), fromSax);
    }

    @Test
    public void bugFix_sax_nestedMap_ignoredKeys_keepsLaterEntries() {
        if (!ParserFactory.isAbacusXmlParserAvailable()) {
            return;
        }

        final Map<String, Object> innerMap = new LinkedHashMap<>();
        innerMap.put("a", "1");
        innerMap.put("drop1", "X");
        innerMap.put("drop2", "Y");
        innerMap.put("b", "2");
        final Map<String, Object> outerMap = new LinkedHashMap<>();
        outerMap.put("inner", innerMap);
        outerMap.put("tail", "TAIL");

        final String xml = staxParser.serialize(outerMap);
        final XmlDeserConfig xdc = new XmlDeserConfig().setIgnoredPropNames(Map.class, Set.of("drop1", "drop2"));

        final XmlParser saxParser = new AbacusXmlParserImpl(XmlParserType.SAX);
        final Map<String, Object> fromSax = saxParser.deserialize(xml, xdc, Map.class);

        assertEquals(N.asMap("inner", N.asMap("a", "1", "b", "2"), "tail", "TAIL"), fromSax);
        assertEquals(staxParser.deserialize(xml, xdc, Map.class), fromSax);
        assertEquals(domParser.deserialize(xml, xdc, Map.class), fromSax);
    }

    // --- regression tests for 2026-06-11 deep-review fixes ---

    @Test
    public void testSharedReferenceIsNotMisdetectedAsCycle() {
        // regression: write() never removed objects from serializedObjects, so two SIBLING
        // references to the same object (a DAG, not a cycle) were misidentified as circular and
        // the second one was silently emitted as an empty element
        final Map<String, Object> shared = N.asMap("name", "x");
        final Map<String, Object> root = new java.util.LinkedHashMap<>();
        root.put("a1", shared);
        root.put("a2", shared);

        final com.landawn.abacus.parser.XmlSerConfig xsc = new com.landawn.abacus.parser.XmlSerConfig().setSupportCircularReference(true);
        final String xml = new AbacusXmlParserImpl(XmlParserType.StAX).serialize(root, xsc);

        final int first = xml.indexOf("name");
        final int second = xml.indexOf("name", first + 1);
        assertEquals(true, first >= 0 && second > first, "both sibling references must be fully serialized: " + xml);
    }

    @Test
    public void testDeserialize_IOExceptionFromReader() {
        final java.io.Reader failingReader = new java.io.Reader() {
            @Override
            public int read(final char[] cbuf, final int off, final int len) throws IOException {
                throw new IOException("read failed");
            }

            @Override
            public void close() throws IOException {
                // no-op
            }
        };

        final com.landawn.abacus.exception.UncheckedIOException ex = assertThrows(com.landawn.abacus.exception.UncheckedIOException.class,
                () -> new AbacusXmlParserImpl(XmlParserType.StAX).deserialize(failingReader, new XmlDeserConfig(), Map.class));

        assertTrue(ex.getCause() instanceof IOException);
    }

}
