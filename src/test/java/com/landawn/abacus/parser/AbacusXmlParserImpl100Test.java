package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
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
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.XmlSerializationConfig.XSC;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Strings;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Tag("new-test")
public class AbacusXmlParserImpl100Test extends TestBase {

    private XmlParser staxParser;
    private XmlParser domParser;

    @Builder
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Person {
        private String name;
        private int age;

    }

    @BeforeEach
    public void setUp() {
        staxParser = new AbacusXmlParserImpl(XmlParserType.StAX);
        domParser = new AbacusXmlParserImpl(XmlParserType.DOM);
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

        XmlSerializationConfig config = new XmlSerializationConfig().prettyFormat(true).tagByPropertyName(true);

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

        XmlDeserializationConfig config = new XmlDeserializationConfig().ignoreUnmatchedProperty(true);

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
        Person person = staxParser.deserialize(bais, new XmlDeserializationConfig(), nodeClasses);

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
    public void testSerializeBigValues() {
        String longString = Strings.repeat(Strings.uuid(), 1000);

        {
            String[] array = { "a", "b", longString };

            String xml = staxParser.serialize(array, XSC.create().writeTypeInfo(true));
            assertNotNull(xml);
            N.println(xml);

            String[] array2 = staxParser.deserialize(xml, String[].class);
            assertArrayEquals(array, array2);
        }

        {
            List<String> coll = N.asList("a", "b", longString);

            String xml = staxParser.serialize(coll, XSC.create().writeTypeInfo(true));
            assertNotNull(xml);
            N.println(xml);

            List<String> coll2 = staxParser.deserialize(xml, List.class);
            assertEquals(coll, coll2);
        }

        {
            Set<String> coll = N.asSet("a", "b", longString);

            String xml = staxParser.serialize(coll, XSC.create().writeTypeInfo(true));
            assertNotNull(xml);
            N.println(xml);

            Set<String> coll2 = staxParser.deserialize(xml, Set.class);
            assertEquals(coll, coll2);
        }

        {
            Queue<String> coll = N.asQueue("a", "b", longString);

            String xml = staxParser.serialize(coll, XSC.create().writeTypeInfo(true));
            assertNotNull(xml);
            N.println(xml);

            Queue<String> coll2 = staxParser.deserialize(xml, ArrayDeque.class);
            assertHaveSameElements(coll, coll2);
        }

        {
            Map<String, Object> map = N.asMap("a", 1, "b", "2", "c", longString, "map", map = N.asMap("a", 1, "b", "2", "c", longString), "list",
                    N.asList("a", "b", longString), "listMap", N.asMap("a", N.asList("1", "2", longString), "b", N.asList("3", "4")));

            String xml = staxParser.serialize(map, XSC.create().writeTypeInfo(true));
            assertNotNull(xml);
            N.println(xml);

            Map<String, String> map2 = staxParser.deserialize(xml, Map.class);
            assertEquals(map, map2);
        }

    }
}
