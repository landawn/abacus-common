package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.ParseException;
import com.landawn.abacus.type.Type;

@Tag("2025")
public class XMLParser2025Test extends TestBase {

    private XMLParser parser;

    public static class Person {
        private String name;
        private int age;
        private String email;

        public Person() {
        }

        public Person(String name, int age, String email) {
            this.name = name;
            this.age = age;
            this.email = email;
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

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null || getClass() != obj.getClass())
                return false;
            Person person = (Person) obj;
            return age == person.age && Objects.equals(name, person.name) && Objects.equals(email, person.email);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, age, email);
        }
    }

    public static class Company {
        private String companyName;
        private int employees;

        public Company() {
        }

        public Company(String companyName, int employees) {
            this.companyName = companyName;
            this.employees = employees;
        }

        public String getCompanyName() {
            return companyName;
        }

        public void setCompanyName(String companyName) {
            this.companyName = companyName;
        }

        public int getEmployees() {
            return employees;
        }

        public void setEmployees(int employees) {
            this.employees = employees;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null || getClass() != obj.getClass())
                return false;
            Company company = (Company) obj;
            return employees == company.employees && Objects.equals(companyName, company.companyName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(companyName, employees);
        }
    }

    public static class Department {
        private String name;
        private List<Person> employees;

        public Department() {
        }

        public Department(String name, List<Person> employees) {
            this.name = name;
            this.employees = employees;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public List<Person> getEmployees() {
            return employees;
        }

        public void setEmployees(List<Person> employees) {
            this.employees = employees;
        }
    }

    @BeforeEach
    public void setUp() {
        parser = ParserFactory.createXMLParser();
    }

    @Test
    public void testDeserializeFromNode_SimpleObject() throws Exception {
        String xml = "<person><name>John</name><age>30</age><email>john@example.com</email></person>";
        Node node = parseXmlToNode(xml);

        Person result = parser.deserialize(node, Person.class);

        assertNotNull(result);
        assertEquals("John", result.getName());
        assertEquals(30, result.getAge());
        assertEquals("john@example.com", result.getEmail());
    }

    @Test
    public void testDeserializeFromNode_EmptyValues() throws Exception {
        String xml = "<person><name></name><age>0</age><email></email></person>";
        Node node = parseXmlToNode(xml);

        Person result = parser.deserialize(node, Person.class);

        assertNotNull(result);
        assertTrue(result.getName() == null || result.getName().isEmpty());
        assertEquals(0, result.getAge());
    }

    @Test
    public void testDeserializeFromNode_NullNode() {
        assertThrows(Exception.class, () -> {
            parser.deserialize((Node) null, Person.class);
        });
    }

    @Test
    public void testDeserializeFromNode_NullTargetClass() throws Exception {
        String xml = "<person><name>John</name></person>";
        Node node = parseXmlToNode(xml);

        assertThrows(Exception.class, () -> {
            parser.deserialize(node, (Class<Person>) null);
        });
    }

    @Test
    public void testDeserializeFromNodeWithConfig_SimpleObject() throws Exception {
        String xml = "<person><name>Jane</name><age>25</age><email>jane@example.com</email></person>";
        Node node = parseXmlToNode(xml);
        XMLDeserializationConfig config = new XMLDeserializationConfig();

        Person result = parser.deserialize(node, config, Person.class);

        assertNotNull(result);
        assertEquals("Jane", result.getName());
        assertEquals(25, result.getAge());
        assertEquals("jane@example.com", result.getEmail());
    }

    @Test
    public void testDeserializeFromNodeWithConfig_NullConfig() throws Exception {
        String xml = "<person><name>Bob</name><age>40</age><email>bob@example.com</email></person>";
        Node node = parseXmlToNode(xml);

        Person result = parser.deserialize(node, null, Person.class);

        assertNotNull(result);
        assertEquals("Bob", result.getName());
        assertEquals(40, result.getAge());
    }

    @Test
    public void testDeserializeFromNodeWithConfig_IgnoreUnknownProperties() throws Exception {
        String xml = "<person><name>Alice</name><age>35</age><unknownField>value</unknownField></person>";
        Node node = parseXmlToNode(xml);
        XMLDeserializationConfig config = new XMLDeserializationConfig().ignoreUnmatchedProperty(true);

        Person result = parser.deserialize(node, config, Person.class);

        assertNotNull(result);
        assertEquals("Alice", result.getName());
        assertEquals(35, result.getAge());
    }

    @Test
    public void testDeserializeFromNodeWithConfig_ComplexObject() throws Exception {
        String xml = "<department><name>Engineering</name><employees>" + "<person><name>John</name><age>30</age></person>"
                + "<person><name>Jane</name><age>25</age></person>" + "</employees></department>";
        Node node = parseXmlToNode(xml);
        XMLDeserializationConfig config = new XMLDeserializationConfig();

        Department result = parser.deserialize(node, config, Department.class);

        assertNotNull(result);
        assertEquals("Engineering", result.getName());
        assertNotNull(result.getEmployees());
    }

    @Test
    public void testDeserializeFromInputStream_WithNodeClasses() {
        String xml = "<person><name>Charlie</name><age>28</age><email>charlie@example.com</email></person>";
        InputStream inputStream = new ByteArrayInputStream(xml.getBytes());

        Map<String, Type<?>> nodeClasses = new HashMap<>();
        nodeClasses.put("person", Type.of(Person.class));

        XMLDeserializationConfig config = new XMLDeserializationConfig();

        Person result = parser.deserialize(inputStream, config, nodeClasses);

        assertNotNull(result);
        assertEquals("Charlie", result.getName());
        assertEquals(28, result.getAge());
    }

    @Test
    public void testDeserializeFromInputStream_PolymorphicDeserialization() {
        String xml = "<company><companyName>TechCorp</companyName><employees>500</employees></company>";
        InputStream inputStream = new ByteArrayInputStream(xml.getBytes());

        Map<String, Type<?>> nodeClasses = new HashMap<>();
        nodeClasses.put("person", Type.of(Person.class));
        nodeClasses.put("company", Type.of(Company.class));

        XMLDeserializationConfig config = new XMLDeserializationConfig();

        Company result = parser.deserialize(inputStream, config, nodeClasses);

        assertNotNull(result);
        assertEquals("TechCorp", result.getCompanyName());
        assertEquals(500, result.getEmployees());
    }

    @Test
    public void testDeserializeFromInputStream_NullConfig() {
        String xml = "<person><name>David</name><age>45</age></person>";
        InputStream inputStream = new ByteArrayInputStream(xml.getBytes());

        Map<String, Type<?>> nodeClasses = new HashMap<>();
        nodeClasses.put("person", Type.of(Person.class));

        Person result = parser.deserialize(inputStream, null, nodeClasses);

        assertNotNull(result);
        assertEquals("David", result.getName());
    }

    @Test
    public void testDeserializeFromInputStream_NullNodeClasses() {
        String xml = "<person><name>Eve</name><age>32</age></person>";
        InputStream inputStream = new ByteArrayInputStream(xml.getBytes());
        XMLDeserializationConfig config = new XMLDeserializationConfig();

        assertThrows(Exception.class, () -> {
            parser.deserialize(inputStream, config, (Map<String, Type<?>>) null);
        });
    }

    @Test
    public void testDeserializeFromInputStream_EmptyNodeClasses() {
        String xml = "<person><name>Frank</name><age>55</age></person>";
        InputStream inputStream = new ByteArrayInputStream(xml.getBytes());

        Map<String, Type<?>> nodeClasses = new HashMap<>();
        XMLDeserializationConfig config = new XMLDeserializationConfig();

        assertThrows(ParseException.class, () -> parser.deserialize(inputStream, config, nodeClasses));
    }

    @Test
    public void testDeserializeFromInputStream_NullSource() {
        Map<String, Type<?>> nodeClasses = new HashMap<>();
        nodeClasses.put("person", Type.of(Person.class));
        XMLDeserializationConfig config = new XMLDeserializationConfig();

        assertThrows(Exception.class, () -> {
            parser.deserialize((InputStream) null, config, nodeClasses);
        });
    }

    @Test
    public void testDeserializeFromReader_WithNodeClasses() {
        String xml = "<person><name>Grace</name><age>29</age><email>grace@example.com</email></person>";
        Reader reader = new StringReader(xml);

        Map<String, Type<?>> nodeClasses = new HashMap<>();
        nodeClasses.put("person", Type.of(Person.class));

        XMLDeserializationConfig config = new XMLDeserializationConfig();

        Person result = parser.deserialize(reader, config, nodeClasses);

        assertNotNull(result);
        assertEquals("Grace", result.getName());
        assertEquals(29, result.getAge());
    }

    @Test
    public void testDeserializeFromReader_MultipleNodeTypes() {
        String xml = "<company><companyName>StartupInc</companyName><employees>50</employees></company>";
        Reader reader = new StringReader(xml);

        Map<String, Type<?>> nodeClasses = new HashMap<>();
        nodeClasses.put("person", Type.of(Person.class));
        nodeClasses.put("company", Type.of(Company.class));

        XMLDeserializationConfig config = new XMLDeserializationConfig();

        Company result = parser.deserialize(reader, config, nodeClasses);

        assertNotNull(result);
        assertEquals("StartupInc", result.getCompanyName());
        assertEquals(50, result.getEmployees());
    }

    @Test
    public void testDeserializeFromReader_NullConfig() {
        String xml = "<person><name>Henry</name><age>38</age></person>";
        Reader reader = new StringReader(xml);

        Map<String, Type<?>> nodeClasses = new HashMap<>();
        nodeClasses.put("person", Type.of(Person.class));

        Person result = parser.deserialize(reader, null, nodeClasses);

        assertNotNull(result);
        assertEquals("Henry", result.getName());
        assertEquals(38, result.getAge());
    }

    @Test
    public void testDeserializeFromReader_NullNodeClasses() {
        String xml = "<person><name>Ivy</name><age>27</age></person>";
        Reader reader = new StringReader(xml);
        XMLDeserializationConfig config = new XMLDeserializationConfig();

        assertThrows(Exception.class, () -> {
            parser.deserialize(reader, config, (Map<String, Type<?>>) null);
        });
    }

    @Test
    public void testDeserializeFromReader_NullSource() {
        Map<String, Type<?>> nodeClasses = new HashMap<>();
        nodeClasses.put("person", Type.of(Person.class));
        XMLDeserializationConfig config = new XMLDeserializationConfig();

        assertThrows(Exception.class, () -> {
            parser.deserialize((Reader) null, config, nodeClasses);
        });
    }

    @Test
    public void testDeserializeFromReader_EmptyXml() {
        String xml = "";
        Reader reader = new StringReader(xml);

        Map<String, Type<?>> nodeClasses = new HashMap<>();
        nodeClasses.put("person", Type.of(Person.class));
        XMLDeserializationConfig config = new XMLDeserializationConfig();

        try {
            Object result = parser.deserialize(reader, config, nodeClasses);
        } catch (Exception e) {
            assertNotNull(e);
        }
    }

    @Test
    public void testDeserializeFromNodeWithNodeClasses_SimpleMapping() throws Exception {
        String xml = "<person><name>Jack</name><age>33</age><email>jack@example.com</email></person>";
        Node node = parseXmlToNode(xml);

        Map<String, Type<?>> nodeClasses = new HashMap<>();
        nodeClasses.put("person", Type.of(Person.class));

        XMLDeserializationConfig config = new XMLDeserializationConfig();

        Person result = parser.deserialize(node, config, nodeClasses);

        assertNotNull(result);
        assertEquals("Jack", result.getName());
        assertEquals(33, result.getAge());
    }

    @Test
    public void testDeserializeFromNodeWithNodeClasses_PolymorphicByNodeName() throws Exception {
        String xml = "<company><companyName>BigCorp</companyName><employees>1000</employees></company>";
        Node node = parseXmlToNode(xml);

        Map<String, Type<?>> nodeClasses = new HashMap<>();
        nodeClasses.put("person", Type.of(Person.class));
        nodeClasses.put("company", Type.of(Company.class));

        XMLDeserializationConfig config = new XMLDeserializationConfig();

        Company result = parser.deserialize(node, config, nodeClasses);

        assertNotNull(result);
        assertEquals("BigCorp", result.getCompanyName());
        assertEquals(1000, result.getEmployees());
    }

    @Test
    public void testDeserializeFromNodeWithNodeClasses_NullConfig() throws Exception {
        String xml = "<person><name>Karen</name><age>31</age></person>";
        Node node = parseXmlToNode(xml);

        Map<String, Type<?>> nodeClasses = new HashMap<>();
        nodeClasses.put("person", Type.of(Person.class));

        Person result = parser.deserialize(node, null, nodeClasses);

        assertNotNull(result);
        assertEquals("Karen", result.getName());
        assertEquals(31, result.getAge());
    }

    @Test
    public void testDeserializeFromNodeWithNodeClasses_NullNodeClasses() throws Exception {
        String xml = "<person><name>Leo</name><age>42</age></person>";
        Node node = parseXmlToNode(xml);
        XMLDeserializationConfig config = new XMLDeserializationConfig();

        assertThrows(Exception.class, () -> {
            parser.deserialize(node, config, (Map<String, Type<?>>) null);
        });
    }

    @Test
    public void testDeserializeFromNodeWithNodeClasses_NullSource() {
        Map<String, Type<?>> nodeClasses = new HashMap<>();
        nodeClasses.put("person", Type.of(Person.class));
        XMLDeserializationConfig config = new XMLDeserializationConfig();

        assertThrows(Exception.class, () -> {
            parser.deserialize((Node) null, config, nodeClasses);
        });
    }

    @Test
    public void testDeserializeFromNodeWithNodeClasses_EmptyNodeClasses() throws Exception {
        String xml = "<person><name>Mike</name><age>36</age></person>";
        Node node = parseXmlToNode(xml);

        Map<String, Type<?>> nodeClasses = new HashMap<>();
        XMLDeserializationConfig config = new XMLDeserializationConfig();

        assertThrows(ParseException.class, () -> parser.deserialize(node, config, nodeClasses));
    }

    @Test
    public void testDeserializeFromNodeWithNodeClasses_NameAttribute() throws Exception {
        String xml = "<entity name=\"person\"><name>Nancy</name><age>28</age></entity>";
        Node node = parseXmlToNode(xml);

        Map<String, Type<?>> nodeClasses = new HashMap<>();
        nodeClasses.put("person", Type.of(Person.class));

        XMLDeserializationConfig config = new XMLDeserializationConfig();

        Object result = parser.deserialize(node, config, nodeClasses);

        assertNotNull(result);
    }

    @Test
    public void testDeserializeFromNodeWithNodeClasses_MultipleClasses() throws Exception {
        String xml = "<person><name>Oliver</name><age>44</age></person>";
        Node node = parseXmlToNode(xml);

        Map<String, Type<?>> nodeClasses = new HashMap<>();
        nodeClasses.put("person", Type.of(Person.class));
        nodeClasses.put("company", Type.of(Company.class));
        nodeClasses.put("department", Type.of(Department.class));

        XMLDeserializationConfig config = new XMLDeserializationConfig();

        Person result = parser.deserialize(node, config, nodeClasses);

        assertNotNull(result);
        assertEquals("Oliver", result.getName());
        assertEquals(44, result.getAge());
    }

    private Node parseXmlToNode(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(xml.getBytes()));
        return doc.getDocumentElement();
    }
}
