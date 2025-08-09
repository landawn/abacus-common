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
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.XMLSerializationConfig.XSC;
import com.landawn.abacus.util.MapEntity;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Strings;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class XMLParserImpl100Test extends TestBase {

    private XMLParser staxParser;
    private XMLParser domParser;

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
        staxParser = new XMLParserImpl(XMLParserType.StAX);
        domParser = new XMLParserImpl(XMLParserType.DOM);
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

        XMLSerializationConfig config = new XMLSerializationConfig().prettyFormat(true).tagByPropertyName(true);

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

        XMLDeserializationConfig config = new XMLDeserializationConfig().ignoreUnmatchedProperty(true);

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
        String xml = "<bean><property name=\"name\">Ivy</property><property name=\"age\">29</property></bean>";

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(xml.getBytes()));
        Node node = doc.getFirstChild();

        Person person = domParser.deserialize(node, Person.class);
        assertNotNull(person);
        assertEquals("Ivy", person.getName());
        assertEquals(29, person.getAge());
    }

    @Test
    public void testDeserializeWithNodeClasses() throws IOException {
        String xml = "<person><name>Jack</name><age>31</age></person>";

        Map<String, Class<?>> nodeClasses = new HashMap<>();
        nodeClasses.put("person", Person.class);

        ByteArrayInputStream bais = new ByteArrayInputStream(xml.getBytes());
        Person person = staxParser.deserialize(bais, new XMLDeserializationConfig(), nodeClasses);

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
    public void testSerializeMapEntity() {
        String longString = Strings.repeat("a", 1000);
        MapEntity mapEntity = new MapEntity("Account");
        mapEntity.set("id", 122);
        mapEntity.set("nil", null);
        mapEntity.set("strValue", longString);
        mapEntity.set("byteValue", (byte) 1);
        mapEntity.set("shortValue", (short) 2);
        mapEntity.set("mapValue", N.asMap("key1", 1.0f, "key2", 2.0f));
        mapEntity.set("listValue", N.asList(1L, 2L, 3));
        mapEntity.set("arrayValue", new String[] { "a", "b", longString });
        mapEntity.set("mapEntityValue", N.asMap("key1", 1.0d, "key2", 2.0d));
        mapEntity.set("beanValue", new Person("Alice", 30));
        mapEntity.set("collectionValues", N.asList(new Person(longString, 30), new Person("Bob", 35)));

        String xml = staxParser.serialize(mapEntity, XSC.create().writeTypeInfo(true));
        assertNotNull(xml);
        N.println(xml);

        MapEntity mapEntity2 = staxParser.deserialize(xml, MapEntity.class);
        N.println(mapEntity2);

        assertEquals((byte) 1, (Byte) mapEntity2.get("byteValue"));
        assertEquals((short) 2, (Short) mapEntity2.get("shortValue"));
        assertEquals(longString, ((String[]) mapEntity2.get("arrayValue"))[2]);

        // assertEquals(mapEntity, mapEntity2);
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

        // assertEquals(mapEntity, mapEntity2);
    }
}

// ParserConfigTest.java
