package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Strings;

@Tag("2025")
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
    public void testDeserializeFromString() {
        String xml = "<bean><name>David</name><age>45</age></bean>";

        Person person = staxParser.deserialize(xml, Person.class);
        assertNotNull(person);
        assertEquals("David", person.getName());
        assertEquals(45, person.getAge());
    }

    @Test
    public void testDeserializeWithConfig() {
        String xml = "<person><name>Emma</name><age>33</age></person>";

        XmlDeserConfig config = new XmlDeserConfig().setIgnoreUnmatchedProperty(true);

        Person person = staxParser.deserialize(xml, config, Person.class);
        assertNotNull(person);
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
    public void testSerializeNull() {
        String xml = staxParser.serialize(null);
        assertEquals("", xml);
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
    public void testDeserializeFromStringWithType() {
        String xml = "<bean><name>TypeTest</name><age>55</age></bean>";

        Person person = staxParser.deserialize(xml, null, Type.of(Person.class));
        assertNotNull(person);
        assertEquals("TypeTest", person.getName());
        assertEquals(55, person.getAge());
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
    public void testSerializeWithPrettyFormat() {
        Person person = new Person("Pretty", 25);

        XmlSerConfig config = new XmlSerConfig().setPrettyFormat(true);
        String xml = staxParser.serialize(person, config);
        assertNotNull(xml);
        assertTrue(xml.contains("Pretty"));
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
    public void testDeserialize_NodeTypesWithNameAttribute_ThrowsNullPointerException() {
        String xml = "<entry name=\"person\"><name>AttrRoot</name><age>64</age></entry>";
        Map<String, Type<?>> nodeTypes = Map.of("person", Type.of(Person.class));

        assertThrows(NullPointerException.class,
                () -> staxParser.deserialize(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)), new XmlDeserConfig(), nodeTypes));
    }

}
