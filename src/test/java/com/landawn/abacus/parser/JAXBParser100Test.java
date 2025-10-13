package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Node;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.ParseException;
import com.landawn.abacus.util.IOUtil;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

@Tag("new-test")
public class JAXBParser100Test extends TestBase {

    private JAXBParser parser;

    @TempDir
    Path tempDir;

    @BeforeEach
    public void setUp() {
        parser = new JAXBParser();
    }

    @XmlRootElement
    public static class Person {
        private String name;
        private int age;

        public Person() {
        }

        public Person(String name, int age) {
            this.name = name;
            this.age = age;
        }

        @XmlElement
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        @XmlElement
        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            Person person = (Person) o;
            return age == person.age && Objects.equals(name, person.name);
        }
    }

    @XmlRootElement
    public static class Book {
        private String title;
        private String isbn;

        @XmlAttribute
        public String getIsbn() {
            return isbn;
        }

        public void setIsbn(String isbn) {
            this.isbn = isbn;
        }

        @XmlElement
        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }
    }

    @Test
    public void testSerializeToString() {
        Person person = new Person("John", 30);
        String xml = parser.serialize(person, (XMLSerializationConfig) null);

        assertNotNull(xml);
        assertTrue(xml.contains("<name>John</name>"));
        assertTrue(xml.contains("<age>30</age>"));
    }

    @Test
    public void testSerializeNull() {
        String result = parser.serialize(null, (XMLSerializationConfig) null);
        assertEquals("", result);
    }

    @Test
    public void testSerializeToFile() throws IOException {
        Person person = new Person("Alice", 25);
        File file = tempDir.resolve("person.xml").toFile();

        parser.serialize(person, null, file);

        assertTrue(file.exists());
        String content = IOUtil.readAllToString(file);
        assertTrue(content.contains("<name>Alice</name>"));
        assertTrue(content.contains("<age>25</age>"));
    }

    @Test
    public void testSerializeToOutputStream() throws IOException {
        Person person = new Person("Bob", 35);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        parser.serialize(person, null, baos);

        String xml = baos.toString();
        assertTrue(xml.contains("<name>Bob</name>"));
        assertTrue(xml.contains("<age>35</age>"));
    }

    @Test
    public void testSerializeToWriter() throws IOException {
        Person person = new Person("Charlie", 40);
        StringWriter writer = new StringWriter();

        parser.serialize(person, null, writer);

        String xml = writer.toString();
        assertTrue(xml.contains("<name>Charlie</name>"));
        assertTrue(xml.contains("<age>40</age>"));
    }

    @Test
    public void testDeserializeFromString() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" + "<person><name>David</name><age>45</age></person>";

        Person person = parser.deserialize(xml, null, Person.class);

        assertEquals("David", person.getName());
        assertEquals(45, person.getAge());
    }

    @Test
    public void testDeserializeEmptyString() {
        Person person = parser.deserialize("", null, Person.class);
        assertNull(person);
    }

    @Test
    public void testDeserializeFromFile() throws IOException {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" + "<person><name>Eve</name><age>22</age></person>";
        File file = tempDir.resolve("test.xml").toFile();
        IOUtil.write(xml, file);

        Person person = parser.deserialize(file, null, Person.class);

        assertEquals("Eve", person.getName());
        assertEquals(22, person.getAge());
    }

    @Test
    public void testDeserializeFromInputStream() throws IOException {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" + "<person><name>Frank</name><age>50</age></person>";
        ByteArrayInputStream bais = new ByteArrayInputStream(xml.getBytes());

        Person person = parser.deserialize(bais, null, Person.class);

        assertEquals("Frank", person.getName());
        assertEquals(50, person.getAge());
    }

    @Test
    public void testDeserializeFromReader() throws IOException {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" + "<person><name>Grace</name><age>28</age></person>";
        StringReader reader = new StringReader(xml);

        Person person = parser.deserialize(reader, null, Person.class);

        assertEquals("Grace", person.getName());
        assertEquals(28, person.getAge());
    }

    @Test
    public void testRoundTrip() {
        Person original = new Person("Henry", 33);
        String xml = parser.serialize(original, (XMLSerializationConfig) null);
        Person deserialized = parser.deserialize(xml, null, Person.class);

        assertEquals(original, deserialized);
    }

    @Test
    public void testSerializeWithAttributes() {
        Book book = new Book();
        book.setTitle("Java Programming");
        book.setIsbn("123-456-789");

        String xml = parser.serialize(book, (XMLSerializationConfig) null);

        assertTrue(xml.contains("isbn=\"123-456-789\""));
        assertTrue(xml.contains("<title>Java Programming</title>"));
    }

    @Test
    public void testSerializeWithIgnoredPropNames() {
        Person person = new Person("Test", 100);
        XMLSerializationConfig config = new XMLSerializationConfig();
        Map<Class<?>, Set<String>> ignoredProps = new HashMap<>();
        ignoredProps.put(Person.class, new HashSet<>(Arrays.asList("age")));
        config.setIgnoredPropNames(ignoredProps);

        assertThrows(ParseException.class, () -> parser.serialize(person, config));
    }

    @Test
    public void testDeserializeWithIgnoredPropNames() {
        String xml = "<person><name>Test</name><age>100</age></person>";
        XMLDeserializationConfig config = new XMLDeserializationConfig();
        Map<Class<?>, Set<String>> ignoredProps = new HashMap<>();
        ignoredProps.put(Person.class, new HashSet<>(Arrays.asList("age")));
        config.setIgnoredPropNames(ignoredProps);

        assertThrows(ParseException.class, () -> parser.deserialize(xml, config, Person.class));
    }

    @Test
    public void testDeserializeFromNode() {
        assertThrows(UnsupportedOperationException.class, () -> parser.deserialize((Node) null, null, Person.class));
    }

    @Test
    public void testDeserializeFromInputStreamWithNodeClasses() {
        ByteArrayInputStream bais = new ByteArrayInputStream("test".getBytes());
        Map<String, Class<?>> nodeClasses = new HashMap<>();

        assertThrows(UnsupportedOperationException.class, () -> parser.deserialize(bais, null, nodeClasses));
    }

    @Test
    public void testDeserializeFromReaderWithNodeClasses() {
        StringReader reader = new StringReader("test");
        Map<String, Class<?>> nodeClasses = new HashMap<>();

        assertThrows(UnsupportedOperationException.class, () -> parser.deserialize(reader, null, nodeClasses));
    }

    @Test
    public void testDeserializeFromNodeWithNodeClasses() {
        Map<String, Class<?>> nodeClasses = new HashMap<>();

        assertThrows(UnsupportedOperationException.class, () -> parser.deserialize((Node) null, null, nodeClasses));
    }

    @Test
    public void testSerializeNullToFile() throws IOException {
        File file = tempDir.resolve("null.xml").toFile();
        parser.serialize(null, null, file);

        assertTrue(file.exists());
        assertEquals("", IOUtil.readAllToString(file));
    }

    @Test
    public void testSerializeNullToOutputStream() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        parser.serialize(null, null, baos);

        assertEquals("", baos.toString());
    }

    @Test
    public void testSerializeNullToWriter() throws IOException {
        StringWriter writer = new StringWriter();
        parser.serialize(null, null, writer);

        assertEquals("", writer.toString());
    }

    @Test
    public void testConstructorWithConfigs() {
        XMLSerializationConfig xsc = new XMLSerializationConfig();
        XMLDeserializationConfig xdc = new XMLDeserializationConfig();

        JAXBParser parserWithConfig = new JAXBParser(xsc, xdc);
        assertNotNull(parserWithConfig);

        Person person = new Person("Test", 30);
        String xml = parserWithConfig.serialize(person, (XMLSerializationConfig) null);
        Person deserialized = parserWithConfig.deserialize(xml, null, Person.class);
        assertEquals(person, deserialized);
    }
}
