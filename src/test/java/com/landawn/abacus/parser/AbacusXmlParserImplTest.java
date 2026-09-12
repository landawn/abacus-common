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
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.annotation.JsonXmlField;
import com.landawn.abacus.exception.ParsingException;
import com.landawn.abacus.parser.entity.PersonType;
import com.landawn.abacus.parser.entity.RecordB;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.Dataset;
import com.landawn.abacus.util.Indexed;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.Timed;
import com.landawn.abacus.util.Tuple;
import com.landawn.abacus.util.Tuple.Tuple2;
import com.landawn.abacus.util.TypeReference;
import com.landawn.abacus.util.u;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class AbacusXmlParserImplTest extends TestBase {

    @TempDir
    Path tempDir;

    private XmlParser staxParser;
    private XmlParser domParser;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Person {
        private String name;
        private int age;
    }

    @Data
    public static class RawXmlBean {
        private String name;

        @JsonXmlField(isJsonRawValue = true)
        private String payload;
    }

    @Data
    public static class CircularRefBean {
        private String name;
        private CircularRefBean reference;
    }

    @Data
    public static class Utf16Bean {
        private String name;
    }

    @Data
    public static class RecordWrapper {
        private String name;
        private RecordB rec;
    }

    @Data
    public static class IgnInner {
        private String keep;
        private String drop1;
        private String drop2;
        private String after;
    }

    @Data
    public static class IgnOuter {
        private String name;
        private IgnInner inner;
        private String tail;
    }

    @BeforeEach
    public void setUp() {
        Assumptions.assumeTrue(ParserFactory.isAbacusXmlParserAvailable());
        staxParser = new AbacusXmlParserImpl(XmlParserType.StAX);
        domParser = new AbacusXmlParserImpl(XmlParserType.DOM);
    }

    private Node firstChild(final String xml) throws Exception {
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes()));
        return doc.getFirstChild();
    }

    @Test
    public void testIgnoredMapValueSkipsTypeResolutionAndConversion() {
        XmlDeserConfig config = XmlDeserConfig.create().setMapValueType(Integer.class).setIgnoredPropNames(Map.class, Set.of("skip"));
        for (String value : new String[] { "<value>not-an-integer</value>", "<value type=\"not-a-java-type\"><![CDATA[invalid]]></value>",
                "<value type=\"not-a-java-type\"><map><entry><key>nested</key><value>invalid</value></entry></map></value>" }) {
            String xml = "<map><entry><key>skip</key>" + value + "</entry><entry><key>keep</key><value>7</value></entry></map>";
            for (XmlParserType parserType : new XmlParserType[] { XmlParserType.StAX, XmlParserType.DOM, XmlParserType.SAX }) {
                assertEquals(Map.of("keep", 7), new AbacusXmlParserImpl(parserType).deserialize(xml, config, Map.class));
            }
        }
    }

    @Test
    public void testStaxDocumentWithoutRootElementReportsParsingException() {
        assertThrows(ParsingException.class, () -> staxParser.deserialize("<?xml version=\"1.0\"?>", Map.class));
    }

    @Test
    public void testRootCollectionUsesGenericTargetElementType() {
        final Type<List<Person>> targetType = Type.of(new TypeReference<List<Person>>() {
        });
        final String xml = "<list><e><person><name>A</name><age>1</age></person></e><e><person><name>B</name><age>2</age></person></e></list>";
        for (final XmlParserType parserType : new XmlParserType[] { XmlParserType.StAX, XmlParserType.DOM, XmlParserType.SAX }) {
            final List<Person> result = new AbacusXmlParserImpl(parserType).deserialize(xml, null, targetType);
            assertEquals(2, result.size());
            assertEquals("A", result.get(0).getName());
            assertEquals(1, result.get(0).getAge());
            assertEquals("B", result.get(1).getName());
            assertEquals(2, result.get(1).getAge());
        }
    }

    @Test
    public void testRootMapUsesGenericTargetKeyAndValueTypes() {
        final Type<Map<DayOfWeek, Integer>> targetType = Type.of(new TypeReference<Map<DayOfWeek, Integer>>() {
        });
        final String xml = "<map><entry><key>MONDAY</key><value>7</value></entry><entry><key>FRIDAY</key><value>9</value></entry></map>";
        for (final XmlParserType parserType : new XmlParserType[] { XmlParserType.StAX, XmlParserType.DOM, XmlParserType.SAX }) {
            assertEquals(Map.of(DayOfWeek.MONDAY, 7, DayOfWeek.FRIDAY, 9), new AbacusXmlParserImpl(parserType).deserialize(xml, null, targetType));
        }
    }

    @Test
    public void testRootGenericTypesPreserveExplicitConfigAndNestedValues() {
        for (final XmlParserType parserType : new XmlParserType[] { XmlParserType.StAX, XmlParserType.DOM, XmlParserType.SAX }) {
            final XmlParser parser = new AbacusXmlParserImpl(parserType);
            final Type<List<Integer>> integers = Type.of("List<Integer>");
            assertEquals(List.of(1L), parser.deserialize("<list><e>1</e></list>", XmlDeserConfig.create().setElementType(Long.class), integers));
            assertEquals(List.of(), parser.deserialize("<list/>", null, integers));

            final Type<Map<Integer, Integer>> mapType = Type.of("Map<Integer,Integer>");
            assertEquals(Map.of(1L, "2"), parser.deserialize("<map><entry><key>1</key><value>2</value></entry></map>",
                    XmlDeserConfig.create().setMapKeyType(Long.class).setMapValueType(String.class), mapType));

            final String xml = parser.serialize(Map.of("values", List.of("3", "4")), XmlSerConfig.create().setWriteTypeInfo(false));
            assertEquals(Map.of("values", List.of(3, 4)), parser.deserialize(xml, null, Type.of("Map<String,List<Integer>>")));
        }
    }

    @Test
    public void testConstructorAndSerialize() {
        assertNotNull(new AbacusXmlParserImpl(XmlParserType.StAX));
        assertNotNull(new AbacusXmlParserImpl(XmlParserType.StAX, new XmlSerConfig(), new XmlDeserConfig()));
        assertNotNull(new AbacusXmlParserImpl(XmlParserType.StAX).serialize("test"));
        assertEquals("", staxParser.serialize(null));

        Person person = new Person("John", 30);
        String xml = staxParser.serialize(person);
        assertTrue(xml.contains("John") && xml.contains("30"));
        xml = staxParser.serialize(person, new XmlSerConfig().setPrettyFormat(true).setTagByPropertyName(true));
        assertTrue(xml.contains("John") && xml.contains("30"));
        xml = staxParser.serialize(new Person("Pretty", 25), new XmlSerConfig().setPrettyFormat(true));
        assertTrue(xml.contains("Pretty"));
        xml = staxParser.serialize(new Person("WriteTypeProp", 91), new XmlSerConfig().setWriteTypeInfo(true).setTagByPropertyName(true));
        assertTrue(xml.contains("WriteTypeProp") && xml.contains("91"));

        final RawXmlBean bean = new RawXmlBean();
        bean.setName("doc");
        bean.setPayload("{\"k\":\"v\"}");
        for (XmlParser parser : new XmlParser[] { staxParser, domParser }) {
            xml = parser.serialize(bean);
            assertTrue(xml.contains("{\"k\":\"v\"}"), xml);
            assertTrue(!xml.contains("\\\"k\\\""), xml);
        }

        int[] values = { 1, 2, 3 };
        assertTrue(staxParser.serialize(values).contains("1") && staxParser.serialize(values).contains("3"));
        assertTrue(domParser.serialize(values).contains("1") && domParser.serialize(values).contains("3"));
        xml = staxParser.serialize(Arrays.asList("one", "two", "three"));
        assertTrue(xml.contains("one") && xml.contains("three"));
        xml = staxParser.serialize(N.asMap("a", 1, "b", 2));
        assertTrue(xml.contains("a") && xml.contains("1") && xml.contains("b"));
        xml = staxParser.serialize(new String[] { "x", "y", "z" });
        assertTrue(xml.contains("x") && xml.contains("z"));

        final Map<Object, String> ignored = new LinkedHashMap<>();
        ignored.put(new StringBuilder("skip"), "hidden");
        ignored.put(new StringBuilder("keep"), "visible");
        xml = staxParser.serialize(ignored, new XmlSerConfig().setIgnoredPropNames(Map.class, Set.of("skip")));
        assertTrue(!xml.contains("skip"), xml);
        assertTrue(xml.contains("keep"), xml);

        CircularRefBean cycle = new CircularRefBean();
        cycle.setName("cycle");
        cycle.setReference(cycle);
        xml = new AbacusXmlParserImpl(XmlParserType.StAX, new XmlSerConfig().setCircularReferenceSupported(true), null).serialize(cycle);
        assertTrue(xml.contains("cycle"));

        Person original = new Person("DomRoundTrip", 101);
        Person restored = domParser.deserialize(domParser.serialize(original), Person.class);
        assertEquals("DomRoundTrip", restored.getName());
        assertEquals(101, restored.getAge());
    }

    @Test
    public void testSerializeBigValues() {
        String longString = Strings.repeat(Strings.uuid(), 1000);
        String[] array = { "a", "b", longString };
        String xml = staxParser.serialize(array, XmlSerConfig.create().setWriteTypeInfo(true));
        assertTrue(xml.contains(longString));
        assertArrayEquals(array, staxParser.deserialize(xml, String[].class));

        List<String> coll = N.toList("a", "b", longString);
        xml = staxParser.serialize(coll, XmlSerConfig.create().setWriteTypeInfo(true));
        assertEquals(coll, staxParser.deserialize(xml, List.class));

        Set<String> set = N.toSet("a", "b", longString);
        xml = staxParser.serialize(set, XmlSerConfig.create().setWriteTypeInfo(true));
        assertEquals(set, staxParser.deserialize(xml, Set.class));

        Queue<String> queue = N.toQueue("a", "b", longString);
        xml = staxParser.serialize(queue, XmlSerConfig.create().setWriteTypeInfo(true));
        assertHaveSameElements(queue, staxParser.deserialize(xml, ArrayDeque.class));

        Map<String, Object> map = N.asMap("a", 1, "b", "2", "c", longString, "map", N.asMap("a", 1, "b", "2", "c", longString), "list",
                N.toList("a", "b", longString), "listMap", N.asMap("a", N.toList("1", "2", longString), "b", N.toList("3", "4")));
        xml = staxParser.serialize(map, XmlSerConfig.create().setWriteTypeInfo(true));
        assertTrue(xml.contains(longString));
        assertEquals(map, staxParser.deserialize(xml, Map.class));
    }

    @Test
    public void testSerializeToDestinations() throws IOException {
        Person person = new Person("Bob", 35);
        File tempFile = File.createTempFile("test", ".xml");
        tempFile.deleteOnExit();
        staxParser.serialize(person, tempFile);
        assertTrue(tempFile.length() > 0);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        staxParser.serialize(new Person("Alice", 28), baos);
        assertTrue(baos.toString().contains("Alice"));

        StringWriter writer = new StringWriter();
        staxParser.serialize(new Person("Charlie", 40), writer);
        assertTrue(writer.toString().contains("Charlie"));

        XmlSerConfig config = new XmlSerConfig().setTagByPropertyName(true);
        baos.reset();
        staxParser.serialize(new Person("ConfigOS", 30), config, baos);
        assertTrue(baos.toString().contains("ConfigOS"));

        writer = new StringWriter();
        staxParser.serialize(new Person("ConfigWriter", 35), config, writer);
        assertTrue(writer.toString().contains("ConfigWriter"));

        File cfgFile = File.createTempFile("test-cfg", ".xml");
        cfgFile.deleteOnExit();
        staxParser.serialize(new Person("ConfigFile", 40), config, cfgFile);
        assertTrue(cfgFile.length() > 0);
    }

    @Test
    public void testDeserialize() throws Exception {
        Person person = staxParser.deserialize("<bean><name>David</name><age>45</age></bean>", Person.class);
        assertEquals("David", person.getName());
        assertEquals(45, person.getAge());

        person = staxParser.deserialize("<person><name>Emma</name><age>33</age></person>", new XmlDeserConfig().setIgnoreUnmatchedProperty(true), Person.class);
        assertNotNull(person);

        person = staxParser.deserialize("<bean><name>TypeTest</name><age>55</age></bean>", null, Type.of(Person.class));
        assertEquals("TypeTest", person.getName());

        String xml = "<bean><name>Frank</name><age>50</age></bean>";
        File tempFile = File.createTempFile("test", ".xml");
        tempFile.deleteOnExit();
        Files.writeString(tempFile.toPath(), xml);
        person = staxParser.deserialize(tempFile, Person.class);
        assertEquals("Frank", person.getName());
        person = staxParser.deserialize(tempFile, null, Type.of(Person.class));
        assertEquals("Frank", person.getName());

        person = staxParser.deserialize(new ByteArrayInputStream("<bean><name>Grace</name><age>27</age></bean>".getBytes()), Person.class);
        assertEquals("Grace", person.getName());
        person = staxParser.deserialize(new ByteArrayInputStream("<bean><name>StreamType</name><age>77</age></bean>".getBytes()), null, Type.of(Person.class));
        assertEquals("StreamType", person.getName());
        person = staxParser.deserialize(new ByteArrayInputStream("<bean><name>NullCfg</name><age>41</age></bean>".getBytes()), (XmlDeserConfig) null,
                Person.class);
        assertEquals("NullCfg", person.getName());

        person = staxParser.deserialize(new StringReader("<bean><name>Henry</name><age>38</age></bean>"), Person.class);
        assertEquals("Henry", person.getName());
        person = staxParser.deserialize(new StringReader("<bean><name>ReaderType</name><age>88</age></bean>"), null, Type.of(Person.class));
        assertEquals("ReaderType", person.getName());
        person = staxParser.deserialize(new StringReader("<bean><name>ReaderNullCfg</name><age>51</age></bean>"), (XmlDeserConfig) null, Person.class);
        assertEquals("ReaderNullCfg", person.getName());

        person = domParser.deserialize(firstChild("<bean><name>Henry</name><age>38</age></bean>"), Person.class);
        assertEquals("Henry", person.getName());
        person = domParser.deserialize(firstChild("<bean><name>NodeType</name><age>99</age></bean>"), Type.of(Person.class));
        assertEquals("NodeType", person.getName());
        person = domParser.deserialize(firstChild("<bean><name>NodeConfig</name><age>60</age></bean>"), new XmlDeserConfig().setIgnoreUnmatchedProperty(true),
                Person.class);
        assertEquals("NodeConfig", person.getName());

        Person typed = new Person("WithType", 81);
        Person restored = staxParser.deserialize(staxParser.serialize(typed, XmlSerConfig.create().setWriteTypeInfo(true)), Person.class);
        assertEquals("WithType", restored.getName());
        assertEquals(81, restored.getAge());
    }

    @Test
    public void testDeserializeCdataAndStrayText() {
        Person person = staxParser.deserialize("<bean><name><![CDATA[hello & <world>]]></name><age>7</age></bean>", Person.class);
        assertEquals("hello & <world>", person.getName());
        assertEquals(7, person.getAge());

        person = staxParser.deserialize("<bean><name>pre<![CDATA[mid]]>post</name><age>9</age></bean>", Person.class);
        assertEquals("premidpost", person.getName());

        person = staxParser.deserialize("<bean>ignored<name>Pretty</name>also ignored<age>27</age></bean>", Person.class);
        assertEquals("Pretty", person.getName());
        assertEquals(27, person.getAge());
    }

    @Test
    public void testDeserializeUtf16() throws Exception {
        File file = tempDir.resolve("abacus-utf16.xml").toFile();
        Files.writeString(file.toPath(), "<?xml version=\"1.0\" encoding=\"UTF-16\"?><bean><name>\u4f60\u597d</name></bean>", StandardCharsets.UTF_16);
        assertEquals("\u4f60\u597d", staxParser.deserialize(file, Utf16Bean.class).getName());

        file = tempDir.resolve("abacus-utf16-node-types.xml").toFile();
        Files.writeString(file.toPath(), "<?xml version=\"1.0\" encoding=\"UTF-16\"?><bean><name>\u4e16\u754c</name></bean>", StandardCharsets.UTF_16);
        Map<String, Type<?>> nodeTypes = new HashMap<>();
        nodeTypes.put("bean", Type.of(Utf16Bean.class));
        Utf16Bean utf16 = staxParser.deserialize(file, null, nodeTypes);
        assertEquals("\u4e16\u754c", utf16.getName());
    }

    @Test
    public void testDeserializeNodeTypes() throws Exception {
        Map<String, Type<?>> nodeClasses = Map.of("person", Type.of(Person.class));
        Person person = staxParser.deserialize(new ByteArrayInputStream("<person><name>Jack</name><age>31</age></person>".getBytes()), new XmlDeserConfig(),
                nodeClasses);
        assertEquals("Jack", person.getName());

        person = staxParser.deserialize(new StringReader("<person><name>ReaderNode</name><age>50</age></person>"), new XmlDeserConfig(), nodeClasses);
        assertEquals("ReaderNode", person.getName());

        person = staxParser.deserialize(new ByteArrayInputStream("<person><name>InputStreamNode</name><age>72</age></person>".getBytes()), new XmlDeserConfig(),
                nodeClasses);
        assertEquals("InputStreamNode", person.getName());

        person = domParser.deserialize(firstChild("<person><name>NodeTypes</name><age>70</age></person>"), new XmlDeserConfig(), nodeClasses);
        assertEquals("NodeTypes", person.getName());

        File tempFile = File.createTempFile("test-node", ".xml");
        tempFile.deleteOnExit();
        Files.writeString(tempFile.toPath(), "<person><name>FileNode</name><age>80</age></person>");
        person = staxParser.deserialize(tempFile, new XmlDeserConfig(), nodeClasses);
        assertEquals("FileNode", person.getName());

        person = domParser.deserialize(new ByteArrayInputStream("<person><name>DomNodeTypes</name><age>61</age></person>".getBytes()), new XmlDeserConfig(),
                nodeClasses);
        assertEquals("DomNodeTypes", person.getName());
        person = domParser.deserialize(new StringReader("<person><name>ReaderDom</name><age>71</age></person>"), new XmlDeserConfig(), nodeClasses);
        assertEquals("ReaderDom", person.getName());

        ParsingException ex = assertThrows(ParsingException.class,
                () -> staxParser.deserialize(
                        new ByteArrayInputStream("<entry name=\"person\"><name>AttrRoot</name><age>64</age></entry>".getBytes(StandardCharsets.UTF_8)),
                        new XmlDeserConfig(), nodeClasses));
        assertTrue(ex.getMessage().contains("Missing 'name' attribute"));
    }

    @Test
    public void testReadByStreamAndDomParser() throws Exception {
        AbacusXmlParserImpl impl = (AbacusXmlParserImpl) staxParser;
        javax.xml.stream.XMLStreamReader xmlReader = javax.xml.stream.XMLInputFactory.newFactory()
                .createXMLStreamReader(new StringReader("<bean><name>StreamParse</name><age>21</age></bean>"));
        while (xmlReader.hasNext() && xmlReader.getEventType() != javax.xml.stream.XMLStreamConstants.START_ELEMENT) {
            xmlReader.next();
        }
        Object result = impl.readByStreamParser(xmlReader, new XmlDeserConfig(), Type.of(Person.class));
        assertTrue(result instanceof Person);
        assertEquals("StreamParse", ((Person) result).getName());
        assertEquals(21, ((Person) result).getAge());

        impl = (AbacusXmlParserImpl) domParser;
        result = impl.readByDOMParser(firstChild("<bean><name>DOMParsed</name><age>31</age></bean>"), new XmlDeserConfig(), Type.of(Person.class));
        assertTrue(result instanceof Person);
        assertEquals("DOMParsed", ((Person) result).getName());
        assertEquals(31, ((Person) result).getAge());
    }

    @Test
    public void testDomMapSkipsNonElementsAndRequiresExactlyKeyAndValue() {
        final Map<?, ?> result = domParser
                .deserialize("<map><!-- before --><?entry value?><entry><!-- key --><key>a</key><?value next?><value>1</value></entry></map>", Map.class);
        assertEquals("1", result.get("a"));
        assertThrows(ParsingException.class, () -> domParser.deserialize("<map><entry><key>a</key></entry></map>", Map.class));
        assertThrows(ParsingException.class, () -> domParser.deserialize("<map><entry><key>a</key><value>1</value><value>2</value></entry></map>", Map.class));
    }

    @Test
    public void testSaxNestedImmutableRecord() {
        final RecordWrapper outer = new RecordWrapper();
        outer.setName("OUTER");
        outer.setRec(new RecordB(7, "first", "last"));
        final String xml = staxParser.serialize(outer);
        final XmlDeserConfig xdc = new XmlDeserConfig();
        final RecordWrapper fromSax = new AbacusXmlParserImpl(XmlParserType.SAX).deserialize(xml, xdc, RecordWrapper.class);
        assertEquals(new RecordB(7, "first", "last"), fromSax.getRec());
        assertEquals("OUTER", fromSax.getName());
        assertEquals(staxParser.deserialize(xml, xdc, RecordWrapper.class).getRec(), fromSax.getRec());
        assertEquals(domParser.deserialize(xml, xdc, RecordWrapper.class).getRec(), fromSax.getRec());
    }

    @Test
    public void testStaleStringBuilderRoundTrip() throws IOException {
        Person original = new Person("GivenName", 42);
        XmlSerConfig ser = new XmlSerConfig().setTagByPropertyName(true);
        String xml = staxParser.serialize(original, ser);
        Person restored = staxParser.deserialize(xml, new XmlDeserConfig(), Person.class);
        assertEquals(original.getName(), restored.getName());
        assertEquals(original.getAge(), restored.getAge());

        restored = staxParser.deserialize(new ByteArrayInputStream(staxParser.serialize(new Person("InputFirst", 99), ser).getBytes(StandardCharsets.UTF_8)),
                new XmlDeserConfig(), Person.class);
        assertEquals("InputFirst", restored.getName());
        restored = staxParser.deserialize(new StringReader(staxParser.serialize(new Person("ReaderFirst", 55), ser)), new XmlDeserConfig(), Person.class);
        assertEquals("ReaderFirst", restored.getName());

        List<String> list = Arrays.asList("firstElement_withSomeLengthToExerciseSbAccumulation", "secondElement_alsoWithSomeLengthToExerciseSbAccumulation",
                "thirdElement");
        xml = staxParser.serialize(list, new XmlSerConfig().setTagByPropertyName(true).setWriteTypeInfo(true));
        assertEquals(list, staxParser.deserialize(xml, new XmlDeserConfig().setElementType(String.class), List.class));
    }

    @Test
    public void testSaxIgnoredProps() {
        final IgnInner inner = new IgnInner();
        inner.setKeep("K");
        inner.setDrop1("D1");
        inner.setDrop2("D2");
        inner.setAfter("A");
        final IgnOuter outer = new IgnOuter();
        outer.setName("OUTER");
        outer.setInner(inner);
        outer.setTail("TAIL");
        final String xml = staxParser.serialize(outer, new XmlSerConfig().setTagByPropertyName(false));
        final XmlDeserConfig xdc = new XmlDeserConfig().setIgnoredPropNames(IgnInner.class, Set.of("drop1", "drop2"));
        final XmlParser saxParser = new AbacusXmlParserImpl(XmlParserType.SAX);
        final IgnOuter fromSax = saxParser.deserialize(xml, xdc, IgnOuter.class);
        assertEquals("OUTER", fromSax.getName());
        assertEquals("TAIL", fromSax.getTail());
        assertEquals("K", fromSax.getInner().getKeep());
        assertEquals("A", fromSax.getInner().getAfter());
        assertNull(fromSax.getInner().getDrop1());
        assertNull(fromSax.getInner().getDrop2());
        final IgnOuter fromStax = staxParser.deserialize(xml, xdc, IgnOuter.class);
        assertEquals(fromStax.getInner().getKeep(), fromSax.getInner().getKeep());
        assertEquals(domParser.deserialize(xml, xdc, IgnOuter.class).getInner().getKeep(), fromSax.getInner().getKeep());

        final Map<String, Object> map = new LinkedHashMap<>();
        map.put("a", "1");
        map.put("drop1", "X");
        map.put("drop2", "Y");
        map.put("b", "2");
        final String mapXml = staxParser.serialize(map);
        final XmlDeserConfig mapCfg = new XmlDeserConfig().setIgnoredPropNames(Map.class, Set.of("drop1", "drop2"));
        assertEquals(N.asMap("a", "1", "b", "2"), saxParser.deserialize(mapXml, mapCfg, Map.class));
        assertEquals(staxParser.deserialize(mapXml, mapCfg, Map.class), saxParser.deserialize(mapXml, mapCfg, Map.class));
        assertEquals(domParser.deserialize(mapXml, mapCfg, Map.class), saxParser.deserialize(mapXml, mapCfg, Map.class));

        final Map<String, Object> innerMap = new LinkedHashMap<>();
        innerMap.put("a", "1");
        innerMap.put("drop1", "X");
        innerMap.put("drop2", "Y");
        innerMap.put("b", "2");
        final Map<String, Object> outerMap = new LinkedHashMap<>();
        outerMap.put("inner", innerMap);
        outerMap.put("tail", "TAIL");
        final String nestedXml = staxParser.serialize(outerMap);
        assertEquals(N.asMap("inner", N.asMap("a", "1", "b", "2"), "tail", "TAIL"), saxParser.deserialize(nestedXml, mapCfg, Map.class));
        assertEquals(staxParser.deserialize(nestedXml, mapCfg, Map.class), saxParser.deserialize(nestedXml, mapCfg, Map.class));
        assertEquals(domParser.deserialize(nestedXml, mapCfg, Map.class), saxParser.deserialize(nestedXml, mapCfg, Map.class));
    }

    @Test
    public void testSharedReferenceIsNotMisdetectedAsCycle() {
        final Map<String, Object> shared = N.asMap("name", "x");
        final Map<String, Object> root = new LinkedHashMap<>();
        root.put("a1", shared);
        root.put("a2", shared);
        final String xml = new AbacusXmlParserImpl(XmlParserType.StAX).serialize(root, new XmlSerConfig().setCircularReferenceSupported(true));
        final int first = xml.indexOf("name");
        final int second = xml.indexOf("name", first + 1);
        assertTrue(first >= 0 && second > first, xml);
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
            }
        };
        final com.landawn.abacus.exception.UncheckedIOException ex = assertThrows(com.landawn.abacus.exception.UncheckedIOException.class,
                () -> new AbacusXmlParserImpl(XmlParserType.StAX).deserialize(failingReader, new XmlDeserConfig(), Map.class));
        assertTrue(ex.getCause() instanceof IOException);
    }

    // ---------------------------------------------------------------------------------------------------------
    // Review fixes 2026-09-06 (P4-01..P4-12, P8-02, R-P04, P5-06, T6-02). Every case runs on StAX, DOM and SAX.
    // ---------------------------------------------------------------------------------------------------------

    private static final XmlParserType[] ALL_TYPES = { XmlParserType.StAX, XmlParserType.DOM, XmlParserType.SAX };

    private static XmlParser parserOf(final XmlParserType type) {
        return new AbacusXmlParserImpl(type);
    }

    public static class CharBean {
        private char c;
        private int i;
        private Character boxed;

        public char getC() {
            return c;
        }

        public void setC(final char c) {
            this.c = c;
        }

        public int getI() {
            return i;
        }

        public void setI(final int i) {
            this.i = i;
        }

        public Character getBoxed() {
            return boxed;
        }

        public void setBoxed(final Character boxed) {
            this.boxed = boxed;
        }
    }

    public static class StrBean {
        private String name;
        private Object any;
        private List<String> tags;

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public Object getAny() {
            return any;
        }

        public void setAny(final Object any) {
            this.any = any;
        }

        public List<String> getTags() {
            return tags;
        }

        public void setTags(final List<String> tags) {
            this.tags = tags;
        }
    }

    public static class Family {
        private String name;
        private int age;
        private Person friend;
        private Object[] objs;
        private Person[] kids;
        private int[] nums;
        private List<String> tags;
        private Map<String, String> m;
        private List<Object> lo;

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

        public Person getFriend() {
            return friend;
        }

        public void setFriend(final Person friend) {
            this.friend = friend;
        }

        public Object[] getObjs() {
            return objs;
        }

        public void setObjs(final Object[] objs) {
            this.objs = objs;
        }

        public Person[] getKids() {
            return kids;
        }

        public void setKids(final Person[] kids) {
            this.kids = kids;
        }

        public int[] getNums() {
            return nums;
        }

        public void setNums(final int[] nums) {
            this.nums = nums;
        }

        public List<String> getTags() {
            return tags;
        }

        public void setTags(final List<String> tags) {
            this.tags = tags;
        }

        public Map<String, String> getM() {
            return m;
        }

        public void setM(final Map<String, String> m) {
            this.m = m;
        }

        public List<Object> getLo() {
            return lo;
        }

        public void setLo(final List<Object> lo) {
            this.lo = lo;
        }
    }

    public static class Empty {
    }

    public static class EmptyHolder {
        private Empty e;
        private String s;

        public Empty getE() {
            return e;
        }

        public void setE(final Empty e) {
            this.e = e;
        }

        public String getS() {
            return s;
        }

        public void setS(final String s) {
            this.s = s;
        }
    }

    public static class OptBean {
        private u.OptionalInt oi = u.OptionalInt.empty();
        private u.Optional<Integer> og = u.Optional.empty();
        private u.Optional<String> os = u.Optional.empty();
        private java.util.OptionalInt joi = java.util.OptionalInt.empty();
        private java.util.Optional<String> jos = java.util.Optional.empty();
        private u.Nullable<String> n = u.Nullable.empty();
        private Pair<Integer, String> p;
        private Tuple2<String, Integer> t;
        private u.Optional<List<String>> ol = u.Optional.empty();
        private Timed<String> tm;
        private Indexed<String> ix;
        private u.Optional<Person> ob = u.Optional.empty();

        public u.OptionalInt getOi() {
            return oi;
        }

        public void setOi(final u.OptionalInt oi) {
            this.oi = oi;
        }

        public u.Optional<Integer> getOg() {
            return og;
        }

        public void setOg(final u.Optional<Integer> og) {
            this.og = og;
        }

        public u.Optional<String> getOs() {
            return os;
        }

        public void setOs(final u.Optional<String> os) {
            this.os = os;
        }

        public java.util.OptionalInt getJoi() {
            return joi;
        }

        public void setJoi(final java.util.OptionalInt joi) {
            this.joi = joi;
        }

        public java.util.Optional<String> getJos() {
            return jos;
        }

        public void setJos(final java.util.Optional<String> jos) {
            this.jos = jos;
        }

        public u.Nullable<String> getN() {
            return n;
        }

        public void setN(final u.Nullable<String> n) {
            this.n = n;
        }

        public Pair<Integer, String> getP() {
            return p;
        }

        public void setP(final Pair<Integer, String> p) {
            this.p = p;
        }

        public Tuple2<String, Integer> getT() {
            return t;
        }

        public void setT(final Tuple2<String, Integer> t) {
            this.t = t;
        }

        public u.Optional<List<String>> getOl() {
            return ol;
        }

        public void setOl(final u.Optional<List<String>> ol) {
            this.ol = ol;
        }

        public Timed<String> getTm() {
            return tm;
        }

        public void setTm(final Timed<String> tm) {
            this.tm = tm;
        }

        public Indexed<String> getIx() {
            return ix;
        }

        public void setIx(final Indexed<String> ix) {
            this.ix = ix;
        }

        public u.Optional<Person> getOb() {
            return ob;
        }

        public void setOb(final u.Optional<Person> ob) {
            this.ob = ob;
        }
    }

    // P4-01 / P5-07 (b): an unset char is written as an empty element and reads back as '\0'.
    @Test
    public void reviewFixes20260906_nulCharPropertyWritesEmptyElement() {
        final CharBean bean = new CharBean();
        bean.setI(7);
        bean.setBoxed('\0');

        final String xml = parserOf(XmlParserType.StAX).serialize(bean);
        assertTrue(xml.contains("<c></c>"), xml);
        assertTrue(xml.contains("<boxed></boxed>"), xml);
        assertTrue(!xml.contains("&#x0;"), xml);

        for (final XmlParserType type : ALL_TYPES) {
            final CharBean back = parserOf(type).deserialize(xml, CharBean.class);
            assertEquals('\0', back.getC(), type.toString());
            assertEquals(7, back.getI(), type.toString());
            assertNull(back.getBoxed(), type.toString()); // documented: a boxed Character reads back as null
        }

        // Exclusion.DEFAULT still omits the default char.
        final String xml2 = parserOf(XmlParserType.StAX).serialize(bean, XmlSerConfig.create().setExclusion(Exclusion.DEFAULT));
        assertTrue(!xml2.contains("<c>"), xml2);

        // A legal char still round-trips through the same path.
        bean.setC('x');
        bean.setBoxed('y');

        for (final XmlParserType type : ALL_TYPES) {
            final CharBean back = parserOf(type).deserialize(parserOf(type).serialize(bean), CharBean.class);
            assertEquals('x', back.getC(), type.toString());
            assertEquals(Character.valueOf('y'), back.getBoxed(), type.toString());
        }
    }

    // P4-01 / P5-07 (a): XML-1.0-illegal code units throw at serialize time, on every scalar path.
    @Test
    public void reviewFixes20260906_illegalXmlCharactersThrowAtSerializeTime() {
        final XmlParser parser = parserOf(XmlParserType.StAX);
        final String[] illegal = { "a\u0001b", "a\u0000b", "\u000C", "\uD800", "a\uDC00b", "\uFFFE", "\uFFFF" };

        for (final String value : illegal) {
            final StrBean bean = new StrBean();
            bean.setName(value);
            ParsingException ex = assertThrows(ParsingException.class, () -> parser.serialize(bean), value);
            assertTrue(ex.getMessage().contains("Property 'name'") && ex.getMessage().contains("U+"), ex.getMessage());

            final StrBean objBean = new StrBean();
            objBean.setAny(value);
            ex = assertThrows(ParsingException.class, () -> parser.serialize(objBean), value);
            assertTrue(ex.getMessage().contains("Property 'any'"), ex.getMessage());

            ex = assertThrows(ParsingException.class, () -> parser.serialize(N.asMap("k", value)), value);
            assertTrue(ex.getMessage().contains("Map value"), ex.getMessage());

            ex = assertThrows(ParsingException.class, () -> parser.serialize(N.asMap(value, "v")), value);
            assertTrue(ex.getMessage().contains("Map key"), ex.getMessage());

            assertThrows(ParsingException.class, () -> parser.serialize(N.asList((Object) 1, value)), value);
            assertThrows(ParsingException.class, () -> parser.serialize(new Object[] { value }), value);
        }

        // A NUL Character element is the one lossless case: an empty <e></e>.
        final String nulElement = parser.serialize(new Object[] { '\0' });
        assertEquals("<array><e></e></array>", nulElement);

        // Legal edge values round-trip on all three readers.
        final String[] legal = { "\t\n\r", "", "  ", "\u007F", "\u0085", "\u2028", "\uD7FF", "\uE000", "\uFFFD", "\uFEFF", "😀", "<a&b>\"'" };

        for (final String value : legal) {
            final StrBean bean = new StrBean();
            bean.setName(value);
            bean.setTags(N.asList(value, "\u0000")); // embedded-JSON path keeps escaping NUL
            final String xml = parser.serialize(bean);

            for (final XmlParserType type : ALL_TYPES) {
                final StrBean back = parserOf(type).deserialize(xml, StrBean.class);
                assertEquals(value, back.getName(), type + " " + xml);
                assertEquals(N.asList(value, "\u0000"), back.getTags(), type + " " + xml);
            }
        }
    }

    // P4-02: pretty-printed EMPTY array (non-serializable element type) on SAX and DOM.
    @Test
    public void reviewFixes20260906_prettyPrintedEmptyArrayReadsBackEmpty() {
        final Family f = new Family();
        f.setName("n");
        f.setObjs(new Object[0]);
        f.setKids(new Person[0]);
        f.setNums(new int[0]);

        final String pretty = parserOf(XmlParserType.StAX).serialize(f, XmlSerConfig.create().setPrettyFormat(true));
        assertTrue(pretty.contains("<array>\n"), pretty);

        for (final XmlParserType type : ALL_TYPES) {
            final Family back = parserOf(type).deserialize(pretty, Family.class);
            assertNotNull(back.getObjs(), type.toString());
            assertEquals(0, back.getObjs().length, type.toString());
            assertNotNull(back.getKids(), type.toString());
            assertEquals(0, back.getKids().length, type.toString());
            assertEquals(0, back.getNums().length, type.toString());

            assertEquals(0, ((Object[]) parserOf(type).deserialize("<array>\n  </array>", Object[].class)).length, type.toString());
            assertEquals(0, ((int[]) parserOf(type).deserialize("<array> </array>", int[].class)).length, type.toString());
            assertEquals(0, ((String[]) parserOf(type).deserialize("<array>\n</array>", String[].class)).length, type.toString());
            assertEquals(0, ((Person[]) parserOf(type).deserialize("<array>\n</array>", Person[].class)).length, type.toString());
            assertEquals(0, ((Object[]) parserOf(type).deserialize("<array></array>", Object[].class)).length, type.toString());

            // regression guards: JSON text forms and element forms are unchanged
            assertArrayEquals(new int[] { 1, 2 }, parserOf(type).deserialize("<array>[1, 2]</array>", int[].class), type.toString());
            assertArrayEquals(new int[] { 1, 2 }, parserOf(type).deserialize("<array> [1, 2] </array>", int[].class), type.toString());
            assertArrayEquals(new Integer[] { 1 }, parserOf(type).deserialize("<array><e>1</e></array>", Integer[].class), type.toString());

            final List<Object[]> nested = parserOf(type).deserialize("<list><e><array> </array></e></list>", null, Type.of("List<Object[]>"));
            assertEquals(1, nested.size(), type.toString());
            assertEquals(0, nested.get(0).length, type.toString());

            // pretty NON-empty arrays with a null element still round-trip
            f.setObjs(new Object[] { 1, "a", null });
            f.setKids(new Person[] { new Person("k", 1), null });
            final String pretty2 = parserOf(type).serialize(f, XmlSerConfig.create().setPrettyFormat(true));
            final Family back2 = parserOf(type).deserialize(pretty2, Family.class);
            assertEquals(3, back2.getObjs().length, type.toString());
            assertEquals("a", back2.getObjs()[1], type.toString());
            assertNull(back2.getObjs()[2], type.toString());
            assertEquals(2, back2.getKids().length, type.toString());
            assertEquals("k", back2.getKids()[0].getName(), type.toString());
            assertNull(back2.getKids()[1], type.toString());
            f.setObjs(new Object[0]);
            f.setKids(new Person[0]);
        }
    }

    // P4-03: an unwrapped nested bean is rejected instead of silently read as an empty bean.
    @Test
    public void reviewFixes20260906_unwrappedNestedBeanIsRejected() {
        final String twoChildren = "<family><name>a</name><friend><name>b</name><age>2</age></friend><age>5</age></family>";
        final String oneChild = "<family><name>a</name><friend><name>b</name></friend><age>5</age></family>";
        final String unknownChild = "<family><friend><nosuch>b</nosuch></friend></family>";
        final String trailingSibling = "<family><friend><person><name>b</name></person><extra>1</extra></friend></family>";
        final String twoWrappers = "<family><friend><person><name>b</name></person><person><name>c</name></person></friend></family>";

        for (final XmlParserType type : ALL_TYPES) {
            final XmlParser parser = parserOf(type);

            for (final String xml : new String[] { twoChildren, oneChild, unknownChild, trailingSibling, twoWrappers }) {
                assertThrows(ParsingException.class, () -> parser.deserialize(xml, Family.class), type + " " + xml);
            }

            final ParsingException ex = assertThrows(ParsingException.class, () -> parser.deserialize(oneChild, Family.class));
            assertTrue(ex.getMessage().contains("<name>"), type + " " + ex.getMessage());

            // the serializer's own wrapper forms still round-trip: compact, pretty, tagByPropertyName=false, writeTypeInfo
            final Family f = new Family();
            f.setName("a");
            f.setAge(5);
            f.setFriend(new Person("b", 2));
            f.setM(N.asMap("k", "v"));
            f.setLo(N.asList((Object) "x", 1));
            f.setObjs(new Object[] { "o" });

            for (final XmlSerConfig xsc : new XmlSerConfig[] { XmlSerConfig.create(), XmlSerConfig.create().setPrettyFormat(true),
                    XmlSerConfig.create().setTagByPropertyName(false), XmlSerConfig.create().setWriteTypeInfo(true),
                    XmlSerConfig.create().setTagByPropertyName(false).setWriteTypeInfo(true).setPrettyFormat(true) }) {
                final String xml = parser.serialize(f, xsc);
                final Family back = parser.deserialize(xml, Family.class);
                assertEquals("b", back.getFriend().getName(), type + " " + xml);
                assertEquals(2, back.getFriend().getAge(), type + " " + xml);
                assertEquals(5, back.getAge(), type + " " + xml);
                assertEquals(N.asMap("k", "v"), back.getM(), type + " " + xml);
                assertEquals(2, back.getLo().size(), type + " " + xml);
                assertEquals("o", back.getObjs()[0], type + " " + xml);
            }

            // an explicit null marker still yields null; an unknown wrapper name holding properties is still accepted (legacy)
            assertNull(parser.deserialize("<family><friend isNull=\"true\" /></family>", Family.class).getFriend(), type.toString());
            assertEquals("b", parser.deserialize("<family><friend><unknown><name>b</name></unknown></friend></family>", Family.class).getFriend().getName(),
                    type.toString());
            // trailing text after the wrapper is ignored, as before
            assertEquals("b", parser.deserialize("<family><friend><person><name>b</name></person>junk</friend></family>", Family.class).getFriend().getName(),
                    type.toString());
        }
    }

    // P4-04: a root element marked isNull="true" is null on every reader.
    @Test
    public void reviewFixes20260906_rootIsNullYieldsNull() {
        for (final XmlParserType type : ALL_TYPES) {
            final XmlParser parser = parserOf(type);
            assertNull(parser.deserialize("<person isNull=\"true\"/>", Person.class), type.toString());
            assertNull(parser.deserialize("<bean name=\"person\" isNull=\"true\"></bean>", Person.class), type.toString());
            assertNull(parser.deserialize("<list isNull=\"true\"/>", List.class), type.toString());
            assertNull(parser.deserialize("<map isNull=\"true\"><entry><key>a</key><value>1</value></entry></map>", Map.class), type.toString());
            assertNull(parser.deserialize("<array isNull=\"true\"/>", String[].class), type.toString());

            // and without the marker the same documents produce instances
            assertNotNull(parser.deserialize("<person/>", Person.class), type.toString());
            assertEquals(List.of(), parser.deserialize("<list/>", List.class), type.toString());
        }
    }

    // P4-05: the JSON text form of a collection is read by SAX and DOM as it is by StAX.
    @Test
    public void reviewFixes20260906_collectionJsonTextForm() {
        for (final XmlParserType type : ALL_TYPES) {
            final XmlParser parser = parserOf(type);
            assertEquals(List.of(1, 2, 3), parser.deserialize("<list>[1, 2, 3]</list>", null, Type.of("List<Integer>")), type.toString());
            assertEquals(Set.of("a"), parser.deserialize("<set>[\"a\"]</set>", null, Type.of("Set<String>")), type.toString());
            assertEquals(List.of(), parser.deserialize("<list></list>", null, Type.of("List<Integer>")), type.toString());
            assertEquals(List.of(), parser.deserialize("<list>  \n </list>", null, Type.of("List<Integer>")), type.toString());
            assertEquals(List.of("a", "b"), parser.deserialize("<list>[\"a\", \"b\"]</list>", List.class), type.toString());

            final Family back = parser.deserialize("<family><tags><list>[\"x\", \"y\"]</list></tags></family>", Family.class);
            assertEquals(List.of("x", "y"), back.getTags(), type.toString());
        }
    }

    // P4-06: SAX and DOM close the source; StAX leaves it open.
    @Test
    public void reviewFixes20260906_saxAndDomCloseTheSourceStaxDoesNot() {
        final String xml = "<person><name>a</name></person>";

        for (final XmlParserType type : ALL_TYPES) {
            final boolean[] closed = { false, false };
            final InputStream is = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)) {
                @Override
                public void close() throws IOException {
                    closed[0] = true;
                    super.close();
                }
            };
            final Reader reader = new StringReader(xml) {
                @Override
                public void close() {
                    closed[1] = true;
                    super.close();
                }
            };

            assertEquals("a", parserOf(type).deserialize(is, Person.class).getName(), type.toString());
            assertEquals("a", parserOf(type).deserialize(reader, Person.class).getName(), type.toString());

            final boolean expectClosed = type != XmlParserType.StAX;
            assertEquals(expectClosed, closed[0], type + " InputStream closed");
            assertEquals(expectClosed, closed[1], type + " Reader closed");
        }
    }

    // P4-07: malformed map entries / stray elements are ParsingExceptions everywhere; mixed text is skipped everywhere.
    @Test
    public void reviewFixes20260906_malformedMapEntriesThrowParsingException() {
        final String[] malformed = { "<family><tags><e>a</e><e>b</e></tags></family>", "<map><entry><key>a</key><value>1</value><value>2</value></entry></map>",
                "<map><entry><key>a</key></entry></map>", "<map><entry><key>a</key><key>b</key><value>1</value></entry></map>", "<map><value>1</value></map>",
                "<map><entry><value>1</value></entry></map>" };

        for (final XmlParserType type : ALL_TYPES) {
            final XmlParser parser = parserOf(type);

            for (final String xml : malformed) {
                final Class<?> target = xml.startsWith("<family") ? Family.class : Map.class;
                assertThrows(ParsingException.class, () -> parser.deserialize(xml, target), type + " " + xml);
            }

            assertEquals(Map.of("a", "1"), parser.deserialize("<map><entry>x<key>a</key>y<value>1</value>z</entry></map>", Map.class), type.toString());
            assertEquals(List.of(1), parser.deserialize("<list>junk<e>1</e></list>", null, Type.of("List<Integer>")), type.toString());
            assertEquals(List.of(1), parser.deserialize("<list><e>1</e>junk</list>", null, Type.of("List<Integer>")), type.toString());
            assertArrayEquals(new Integer[] { 1 }, parser.deserialize("<array>junk<e>1</e></array>", Integer[].class), type.toString());

            // nested maps keep working with the per-entry bookkeeping
            final Map<String, Map<String, Integer>> nested = parser.deserialize(
                    "<map><entry><key>o</key><value><map><entry><key>i</key><value>1</value></entry></map></value></entry></map>", null,
                    Type.of("Map<String, Map<String, Integer>>"));
            assertEquals(Map.of("o", Map.of("i", 1)), nested, type.toString());
        }
    }

    // P4-08: namespace-prefixed documents match on the local name in all three readers.
    @Test
    public void reviewFixes20260906_namespacePrefixedDocument() {
        final String xml = "<p:person xmlns:p=\"urn:x\"><p:name>a</p:name><p:age>3</p:age></p:person>";
        final String nodeTypesXml = "<p:bean xmlns:p=\"urn:x\"><p:name>a</p:name></p:bean>";
        final Map<String, Type<?>> nodeTypes = N.asMap("bean", Type.of(Person.class));

        for (final XmlParserType type : ALL_TYPES) {
            final Person person = parserOf(type).deserialize(xml, Person.class);
            assertEquals("a", person.getName(), type.toString());
            assertEquals(3, person.getAge(), type.toString());

            final Person viaNodeTypes = parserOf(type).deserialize(new StringReader(nodeTypesXml), null, nodeTypes);
            assertEquals("a", viaNodeTypes.getName(), type.toString());
        }
    }

    // P4-09: nodeTypes lookup falls back to the tag name when the name attribute is present but empty.
    @Test
    public void reviewFixes20260906_nodeTypesEmptyNameAttributeFallsBackToTagName() {
        final Map<String, Type<?>> nodeTypes = N.asMap("bean", Type.of(Person.class));

        for (final XmlParserType type : ALL_TYPES) {
            final XmlParser parser = parserOf(type);
            final String emptyName = "<bean name=\"\"><name>a</name></bean>";

            Person p = parser.deserialize(new StringReader(emptyName), null, nodeTypes);
            assertEquals("a", p.getName(), type.toString());
            p = parser.deserialize(new ByteArrayInputStream(emptyName.getBytes(StandardCharsets.UTF_8)), null, nodeTypes);
            assertEquals("a", p.getName(), type.toString());

            p = parser.deserialize(new StringReader("<bean><name>a</name></bean>"), null, nodeTypes);
            assertEquals("a", p.getName(), type.toString());

            // a non-empty name that is not in the map must NOT fall back
            assertThrows(ParsingException.class, () -> parser.deserialize(new StringReader("<bean name=\"other\"><name>a</name></bean>"), null, nodeTypes),
                    type.toString());
        }
    }

    // P4-10: isNull="true" wins over text on every reader.
    @Test
    public void reviewFixes20260906_isNullAttributeWinsOverText() {
        for (final XmlParserType type : ALL_TYPES) {
            final XmlParser parser = parserOf(type);
            assertNull(parser.deserialize("<person><name isNull=\"true\">a</name></person>", Person.class).getName(), type.toString());
            assertNull(parser.deserialize("<person><name isNull=\"TRUE\">a</name></person>", Person.class).getName(), type.toString());
            assertEquals(0, parser.deserialize("<person><age isNull=\"true\">7</age></person>", Person.class).getAge(), type.toString());
            assertNull(parser.deserialize("<person><name isNull=\"true\"/></person>", Person.class).getName(), type.toString());
            assertEquals("a", parser.deserialize("<person><name isNull=\"false\">a</name></person>", Person.class).getName(), type.toString());
            assertEquals("a", parser.deserialize("<person><name>a</name></person>", Person.class).getName(), type.toString());

            final Map<String, String> m = parser.deserialize("<map><entry><key>k</key><value isNull=\"true\">x</value></entry></map>", null,
                    Type.of("Map<String, String>"));
            assertTrue(m.containsKey("k") && m.get("k") == null, type + " " + m);

            final List<String> l = parser.deserialize("<list><e isNull=\"true\">x</e><e>y</e></list>", null, Type.of("List<String>"));
            assertEquals(Arrays.asList(null, "y"), l, type.toString());
        }
    }

    // P4-11: root scalar contract pinned (documented, not changed).
    @Test
    public void reviewFixes20260906_rootScalarContractPinned() {
        final XmlParser parser = parserOf(XmlParserType.StAX);
        assertEquals("[1, 2]", parser.serialize(new int[] { 1, 2 }));
        assertEquals("<array><e>1</e><e>2</e></array>", parser.serialize(new Integer[] { 1, 2 }));

        for (final XmlParserType type : ALL_TYPES) {
            assertThrows(ParsingException.class, () -> parserOf(type).deserialize("[1, 2]", int[].class), type.toString());
            assertArrayEquals(new int[] { 1, 2 }, parserOf(type).deserialize("<array><e>1</e><e>2</e></array>", int[].class), type.toString());
        }
    }

    // P4-12: Dataset is documented as unsupported.
    @Test
    public void reviewFixes20260906_datasetIsUnsupported() {
        final Dataset dataset = Dataset.rows(N.asList("c1", "c2"), new Object[][] { { 1, null }, { null, "x" } });
        final XmlParser parser = parserOf(XmlParserType.StAX);

        assertThrows(ParsingException.class, () -> parser.serialize(dataset));
        assertThrows(ParsingException.class, () -> parser.serialize(N.asMap("d", dataset)));

        // ... unless failOnEmptyBean is off, which turns every unsupported class into an empty element
        assertEquals("", parser.serialize(dataset, XmlSerConfig.create().setFailOnEmptyBean(false)));
    }

    // P8-02: failOnEmptyBean=false is honoured (writes nothing), the default still throws.
    @Test
    public void reviewFixes20260906_failOnEmptyBeanFalseWritesNothing() {
        final XmlSerConfig lenient = XmlSerConfig.create().setFailOnEmptyBean(false);
        final EmptyHolder holder = new EmptyHolder();
        holder.setE(new Empty());
        holder.setS("x");

        for (final XmlParserType type : ALL_TYPES) {
            final XmlParser parser = parserOf(type);

            assertEquals("", parser.serialize(new Empty(), lenient), type.toString());
            assertEquals("<map><entry><key>e</key><value></value></entry></map>", parser.serialize(N.asMap("e", new Empty()), lenient), type.toString());
            assertEquals("<emptyHolder><e></e><s>x</s></emptyHolder>", parser.serialize(holder, lenient), type.toString());
            assertEquals("<list><e></e></list>", parser.serialize(N.asList(new Empty()), lenient), type.toString());
            assertEquals(Map.of("e", ""), parser.deserialize(parser.serialize(N.asMap("e", new Empty()), lenient), Map.class), type.toString());

            for (final Object obj : new Object[] { new Empty(), N.asMap("e", new Empty()), holder, N.asList(new Empty()) }) {
                final ParsingException ex = assertThrows(ParsingException.class, () -> parser.serialize(obj), type.toString());
                assertTrue(ex.getMessage().startsWith("Unsupported class:"), ex.getMessage());
                assertThrows(ParsingException.class, () -> parser.serialize(obj, XmlSerConfig.create().setFailOnEmptyBean(true)), type.toString());
            }

            // a parser constructed with a lenient default config
            assertEquals("", new AbacusXmlParserImpl(type, XmlSerConfig.create().setFailOnEmptyBean(false), null).serialize(new Empty()), type.toString());
        }
    }

    // R-P04 / P5-12: content after the root element is rejected for bounded sources; open sources are not drained.
    @Test
    public void reviewFixes20260906_trailingContentAfterRootIsRejected() throws IOException {
        final String doc = "<person><name>a</name></person>";
        final String[] bad = { doc + "<person/>", doc + "junk", doc + "<![CDATA[x]]>", doc + " x", "<?xml version=\"1.0\"?>" + doc + "<x/>" };
        final String[] good = { doc, doc + "  \n ", doc + "<!-- c -->", doc + "<?pi x?>", "<?xml version=\"1.0\"?>\n" + doc + "\n" };

        for (final XmlParserType type : ALL_TYPES) {
            final XmlParser parser = parserOf(type);

            for (final String xml : bad) {
                assertThrows(ParsingException.class, () -> parser.deserialize(xml, Person.class), type + " " + xml);

                final Path file = tempDir.resolve("trailing-" + type + "-" + Math.abs(xml.hashCode()) + ".xml");
                Files.writeString(file, xml, StandardCharsets.UTF_8);
                assertThrows(ParsingException.class, () -> parser.deserialize(file.toFile(), Person.class), type + " file " + xml);
            }

            for (final String xml : good) {
                assertEquals("a", parser.deserialize(xml, Person.class).getName(), type + " " + xml);
            }
        }

        // StAX + open Reader: reading stops at the root end element, the second document is neither validated nor rejected.
        final Reader twoDocs = new StringReader(doc + "<person><name>b</name></person>");
        assertEquals("a", parserOf(XmlParserType.StAX).deserialize(twoDocs, Person.class).getName());
        assertTrue(twoDocs.ready(), "the reader is left open");
    }

    // P5-06: raw JSON payloads are XML-escaped for & < > only and decode back verbatim.
    @Test
    public void reviewFixes20260906_rawJsonValueIsXmlEscaped() {
        final String[] payloads = { "{\"k\":\"a<b\"}", "{\"k\":\"a&b\"}", "{\"k\":\"]]>\"}", "{\"k\":\"é😀\"}", "{\"k\":\"v\"}", "{\"k\":\"a>b\"}" };

        for (final String payload : payloads) {
            final RawXmlBean bean = new RawXmlBean();
            bean.setName("doc");
            bean.setPayload(payload);

            final String xml = parserOf(XmlParserType.StAX).serialize(bean);
            assertTrue(!xml.contains("\\\""), xml); // quotes stay verbatim
            assertTrue(xml.contains(payload.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")), xml);

            for (final XmlParserType type : ALL_TYPES) {
                assertEquals(payload, parserOf(type).deserialize(xml, RawXmlBean.class).getPayload(), type + " " + xml);
            }
        }

        final RawXmlBean control = new RawXmlBean();
        control.setPayload("{\"k\":\"\u0001\"}");
        assertThrows(ParsingException.class, () -> parserOf(XmlParserType.StAX).serialize(control));
    }

    // T6-02: empty optionals are written as the null form, present ones as their element, tuple-likes as JSON text.
    @Test
    public void reviewFixes20260906_optionalAndTupleProperties() {
        final OptBean empty = new OptBean();
        final XmlParser writer = parserOf(XmlParserType.StAX);

        // Under the default exclusion an empty wrapper is treated exactly like a null property (omitted or the
        // isNull form, depending on the configured default); with Exclusion.NONE it is the isNull form.
        assertTrue(!writer.serialize(empty).contains(">null<"), writer.serialize(empty));
        final String emptyXml = writer.serialize(empty, XmlSerConfig.create().setExclusion(Exclusion.NONE));
        assertTrue(!emptyXml.contains(">null<"), emptyXml);
        assertTrue(emptyXml.contains("<oi isNull=\"true\" />"), emptyXml);
        assertTrue(emptyXml.contains("<joi isNull=\"true\" />"), emptyXml);
        assertTrue(emptyXml.contains("<n isNull=\"true\" />"), emptyXml);
        assertTrue(emptyXml.contains("<ob isNull=\"true\" />"), emptyXml);

        final OptBean present = new OptBean();
        present.setOi(u.OptionalInt.of(3));
        present.setOg(u.Optional.of(4));
        present.setOs(u.Optional.of("null"));
        present.setJoi(java.util.OptionalInt.of(5));
        present.setJos(java.util.Optional.of("j"));
        present.setN(u.Nullable.of(null));
        present.setP(Pair.of(1, "a,b"));
        present.setT(Tuple.of("x,y", 2));
        present.setOl(u.Optional.of(N.asList("a,b", "c")));
        present.setTm(Timed.of("v", 5L));
        present.setIx(Indexed.of("v", 7));
        present.setOb(u.Optional.of(new Person("pb", 9)));

        for (final XmlParserType type : ALL_TYPES) {
            final XmlParser parser = parserOf(type);

            for (final XmlSerConfig xsc : new XmlSerConfig[] { XmlSerConfig.create(), XmlSerConfig.create().setPrettyFormat(true),
                    XmlSerConfig.create().setWriteTypeInfo(true), XmlSerConfig.create().setExclusion(Exclusion.NULL) }) {
                final OptBean backEmpty = parser.deserialize(parser.serialize(empty, xsc), OptBean.class);
                assertEquals(u.OptionalInt.empty(), backEmpty.getOi(), type.toString());
                assertEquals(u.Optional.empty(), backEmpty.getOg(), type.toString());
                assertEquals(u.Optional.empty(), backEmpty.getOs(), type.toString());
                assertEquals(java.util.OptionalInt.empty(), backEmpty.getJoi(), type.toString());
                assertEquals(java.util.Optional.empty(), backEmpty.getJos(), type.toString());
                assertEquals(u.Nullable.empty(), backEmpty.getN(), type.toString());
                assertNull(backEmpty.getP(), type.toString());
                assertEquals(u.Optional.empty(), backEmpty.getOl(), type.toString());
                assertEquals(u.Optional.empty(), backEmpty.getOb(), type.toString());

                final String presentXml = parser.serialize(present, xsc);
                final OptBean back = parser.deserialize(presentXml, OptBean.class);
                assertEquals(u.OptionalInt.of(3), back.getOi(), type + " " + presentXml);
                assertEquals(u.Optional.of(4), back.getOg(), type + " " + presentXml);
                assertEquals(u.Optional.of("null"), back.getOs(), type + " " + presentXml); // present "null" survives
                assertEquals(java.util.OptionalInt.of(5), back.getJoi(), type + " " + presentXml);
                assertEquals(java.util.Optional.of("j"), back.getJos(), type + " " + presentXml);
                assertEquals(u.Nullable.empty(), back.getN(), type + " " + presentXml); // documented: Nullable.of(null) -> empty
                assertEquals(Pair.of(1, "a,b"), back.getP(), type + " " + presentXml);
                assertEquals(Tuple.of("x,y", 2), back.getT(), type + " " + presentXml);
                assertEquals(u.Optional.of(N.asList("a,b", "c")), back.getOl(), type + " " + presentXml);
                assertEquals(Timed.of("v", 5L), back.getTm(), type + " " + presentXml);
                assertEquals(Indexed.of("v", 7), back.getIx(), type + " " + presentXml);
                assertTrue(back.getOb().isPresent(), type + " " + presentXml);
                assertEquals("pb", back.getOb().get().getName(), type + " " + presentXml);
                assertEquals(9, back.getOb().get().getAge(), type + " " + presentXml);
            }

            // Exclusion.NULL drops the empty optional element like a null property
            final String excluded = parser.serialize(empty, XmlSerConfig.create().setExclusion(Exclusion.NULL));
            assertTrue(!excluded.contains("<oi"), excluded);
            assertTrue(!excluded.contains("<n"), excluded);

            // the isNull form reads back as empty on every reader
            assertEquals(u.OptionalInt.empty(), parser.deserialize("<optBean><oi isNull=\"true\"/></optBean>", OptBean.class).getOi(), type.toString());

            // wrappers inside containers: an empty one is the null form, a present one its element
            final Map<String, Object> wrappers = new LinkedHashMap<>();
            wrappers.put("a", u.Optional.empty());
            wrappers.put("b", u.OptionalInt.of(5));
            final String mapXml = parser.serialize(wrappers);
            assertEquals("<map><entry><key>a</key><value isNull=\"true\" /></entry><entry><key>b</key><value>5</value></entry></map>", mapXml, type.toString());
            final String listXml = parser.serialize(N.asList(u.Optional.empty(), u.Optional.of("x"), Pair.of("l,r", 1)));
            assertEquals("<list><e isNull=\"true\" /><e>x</e><e>[&quot;l,r&quot;, 1]</e></list>", listXml, type.toString());
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Self-review 2026-09-07 (R3): the two halves of the 2026-09-06 fixes that stopped short.
    // ---------------------------------------------------------------------------------------------------------

    public static class SelfRef {
        private String name;
        private SelfRef next;

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public SelfRef getNext() {
            return next;
        }

        public void setNext(final SelfRef next) {
            this.next = next;
        }
    }

    public static class TupleContainerBean {
        private Map<String, Pair<Integer, String>> mp;
        private List<Pair<Integer, String>> lp;
        private Pair<Integer, String>[] ap;

        public Map<String, Pair<Integer, String>> getMp() {
            return mp;
        }

        public void setMp(final Map<String, Pair<Integer, String>> mp) {
            this.mp = mp;
        }

        public List<Pair<Integer, String>> getLp() {
            return lp;
        }

        public void setLp(final List<Pair<Integer, String>> lp) {
            this.lp = lp;
        }

        public Pair<Integer, String>[] getAp() {
            return ap;
        }

        public void setAp(final Pair<Integer, String>[] ap) {
            this.ap = ap;
        }
    }

    // X-01, container half: a tuple-like map key/value, collection element or array element is written as its
    // JSON text, so it must NOT also carry a type attribute naming the tuple - the readers reject that name and
    // the parser's own output could not be read back.
    @Test
    public void reviewFixes20260907_tupleValuesInContainersCarryNoTypeAttribute() {
        final XmlSerConfig typeInfo = XmlSerConfig.create().setWriteTypeInfo(true);
        final XmlParser writer = parserOf(XmlParserType.StAX);

        final Map<Object, Object> mapValue = new LinkedHashMap<>();
        mapValue.put("pair", Pair.of(1, "a,b"));
        mapValue.put("tim", Timed.of("v", 5L));
        mapValue.put("plain", "s");
        final String mapValueXml = writer.serialize(mapValue, typeInfo);
        assertTrue(!mapValueXml.contains("type=\"Pair"), mapValueXml);
        assertTrue(!mapValueXml.contains("Timed"), mapValueXml);
        assertTrue(mapValueXml.contains("<value type=\"String\">s</value>"), mapValueXml); // a plain value keeps its type

        final Map<Object, Object> mapKey = new LinkedHashMap<>();
        mapKey.put(Pair.of(1, "a,b"), "v");
        final String mapKeyXml = writer.serialize(mapKey, typeInfo);
        assertTrue(!mapKeyXml.contains("type=\"Pair"), mapKeyXml);

        final String listXml = writer.serialize(N.asList((Object) Pair.of(1, "a,b"), Indexed.of("v", 7), "s"), typeInfo);
        assertTrue(!listXml.contains("type=\"Pair"), listXml);
        assertTrue(!listXml.contains("Indexed"), listXml);
        assertTrue(listXml.contains("<e type=\"String\">s</e>"), listXml);

        final String arrayXml = writer.serialize(new Object[] { Pair.of(1, "a,b"), "s" }, typeInfo);
        assertTrue(!arrayXml.contains("type=\"Pair"), arrayXml);
        assertTrue(arrayXml.contains("<e type=\"String\">s</e>"), arrayXml);

        for (final XmlParserType type : ALL_TYPES) {
            final XmlParser parser = parserOf(type);

            final Map<String, String> expectedMapValue = new LinkedHashMap<>();
            expectedMapValue.put("pair", "[1, \"a,b\"]");
            expectedMapValue.put("tim", "[5, \"v\"]");
            expectedMapValue.put("plain", "s");
            assertEquals(expectedMapValue, parser.deserialize(mapValueXml, Map.class), type + " " + mapValueXml);
            assertEquals(Map.of("[1, \"a,b\"]", "v"), parser.deserialize(mapKeyXml, Map.class), type + " " + mapKeyXml);
            assertEquals(List.of("[1, \"a,b\"]", "[7, \"v\"]", "s"), parser.deserialize(listXml, List.class), type + " " + listXml);
            assertArrayEquals(new Object[] { "[1, \"a,b\"]", "s" }, parser.deserialize(arrayXml, Object[].class), type + " " + arrayXml);
        }

        // with a declared element type the suppressed attribute lets the tuples come back as tuples
        final TupleContainerBean bean = new TupleContainerBean();
        final Map<String, Pair<Integer, String>> mp = new LinkedHashMap<>();
        mp.put("k", Pair.of(1, "a,b"));
        bean.setMp(mp);
        bean.setLp(N.asList(Pair.of(2, "c,d")));
        bean.setAp(new Pair[] { Pair.of(3, "e,f") });

        // the tuple-carrying elements themselves carry no type attribute even under writeTypeInfo
        final String beanTypeInfoXml = parserOf(XmlParserType.StAX).serialize(bean, typeInfo);
        assertTrue(beanTypeInfoXml.contains("<value>[1, &quot;a,b&quot;]</value>"), beanTypeInfoXml);
        assertTrue(!beanTypeInfoXml.contains("<value type="), beanTypeInfoXml);

        for (final XmlSerConfig xsc : new XmlSerConfig[] { XmlSerConfig.create(), XmlSerConfig.create().setPrettyFormat(true) }) {
            final String xml = parserOf(XmlParserType.StAX).serialize(bean, xsc);

            for (final XmlParserType type : ALL_TYPES) {
                final TupleContainerBean back = parserOf(type).deserialize(xml, TupleContainerBean.class);
                assertEquals(Pair.of(1, "a,b"), back.getMp().get("k"), type + " " + xml);
                assertEquals(List.of(Pair.of(2, "c,d")), back.getLp(), type + " " + xml);
                assertArrayEquals(new Pair[] { Pair.of(3, "e,f") }, back.getAp(), type + " " + xml);
            }
        }
    }

    // R-P01 / P5-15 for this parser: the default configuration tracks no object identity, so a cyclic graph used
    // to unwind in StackOverflowError here while JsonParserImpl and XmlParserImpl already raised ParsingException.
    @Test
    public void reviewFixes20260907_cyclicGraphRaisesParsingExceptionNotStackOverflow() {
        final SelfRef self = new SelfRef();
        self.setName("a");
        self.setNext(self);

        final XmlParser parser = parserOf(XmlParserType.StAX);
        final ParsingException ex = assertThrows(ParsingException.class, () -> parser.serialize(self));
        assertTrue(ex.getMessage().contains("Serialization nesting depth exceeded 256"), ex.getMessage());

        // the same graph inside a map, a list and an array property
        assertThrows(ParsingException.class, () -> parser.serialize(N.asMap("k", self)));
        assertThrows(ParsingException.class, () -> parser.serialize(N.asList(self)));
        assertThrows(ParsingException.class, () -> parser.serialize(new Object[] { self }));

        // a deep but finite graph below the bound still serializes, and round-trips
        SelfRef head = new SelfRef();
        head.setName("n0");
        SelfRef cur = head;

        for (int i = 1; i < 100; i++) {
            final SelfRef n = new SelfRef();
            n.setName("n" + i);
            n.setNext(null);
            cur.setNext(n);
            cur = n;
        }

        final String deepXml = parser.serialize(head);
        assertEquals("n0", parser.deserialize(deepXml, SelfRef.class).getName());

        // a graph deeper than the bound is rejected rather than overflowing the stack
        SelfRef tooDeep = new SelfRef();
        tooDeep.setName("d0");
        cur = tooDeep;

        for (int i = 1; i < 300; i++) {
            final SelfRef n = new SelfRef();
            n.setName("d" + i);
            cur.setNext(n);
            cur = n;
        }

        assertThrows(ParsingException.class, () -> parser.serialize(tooDeep));

        // circularReferenceSupported=true keeps the documented behaviour: repeated objects become empty elements
        final String circular = parser.serialize(self, XmlSerConfig.create().setCircularReferenceSupported(true));
        assertTrue(circular.startsWith("<selfRef>"), circular);
        assertTrue(circular.contains("<name>a</name>"), circular);

        // the counter is released, so a later serialization on the same thread is unaffected
        final SelfRef plain = new SelfRef();
        plain.setName("a");
        assertEquals("<selfRef><name>a</name></selfRef>", parser.serialize(plain));
    }

    // writeUnwrappedValue routes a present wrapper through the same XML-1.0 guard as a plain property.
    @Test
    public void reviewFixes20260907_illegalCharactersInsideAWrapperAreRejected() {
        final XmlParser parser = parserOf(XmlParserType.StAX);

        for (final String value : new String[] { "a\u0001b", "a\u0000b", "\uD800", "\uFFFF" }) {
            final OptBean bean = new OptBean();
            bean.setOs(u.Optional.of(value));
            final ParsingException ex = assertThrows(ParsingException.class, () -> parser.serialize(bean), value);
            assertTrue(ex.getMessage().contains("Property 'os'") && ex.getMessage().contains("U+"), ex.getMessage());
        }

        // legal values, including a supplementary character, still round-trip through the wrapper
        for (final String value : new String[] { "\t\n\r", "", "\uD83D\uDE00", "<a&b>" }) {
            final OptBean bean = new OptBean();
            bean.setOs(u.Optional.of(value));
            final String xml = parserOf(XmlParserType.StAX).serialize(bean);

            for (final XmlParserType type : ALL_TYPES) {
                // "" is the empty ELEMENT, which the String handler reads back as "" (C-023), not as an empty wrapper
                assertEquals(u.Optional.of(value), parserOf(type).deserialize(xml, OptBean.class).getOs(), type + " " + xml);
            }
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Fix pass 2026-09-08 (G01)
    // ---------------------------------------------------------------------------------------------------------

    /** A recognized bean (getter + setter) whose only property is excluded from serialization. */
    public static class AllPropsIgnored {
        @JsonXmlField(ignore = true)
        private String hidden;

        public String getHidden() {
            return hidden;
        }

        public void setHidden(final String hidden) {
            this.hidden = hidden;
        }
    }

    // G01-02: writeBean rejected a bean with no serializable property whatever failOnEmptyBean said, while the
    // JSON writer already honoured the flag - and serialize's own javadoc promised this one did too.
    @Test
    public void fixG01_emptyBeanIsWrittenAsAnEmptyElementWhenFailOnEmptyBeanIsOff() {
        final XmlSerConfig lenient = XmlSerConfig.create().setFailOnEmptyBean(false);
        final AllPropsIgnored bean = new AllPropsIgnored();
        bean.setHidden("h");

        for (final XmlParserType type : ALL_TYPES) {
            final XmlParser parser = parserOf(type);

            final String xml = parser.serialize(bean, lenient);
            assertEquals("<allPropsIgnored></allPropsIgnored>", xml, type.toString());

            final AllPropsIgnored back = parser.deserialize(xml, AllPropsIgnored.class);
            assertNotNull(back, type.toString());
            assertNull(back.getHidden(), type.toString());

            // and nested, where the enclosing element must stay well formed
            assertEquals("<map><entry><key>b</key><value><allPropsIgnored></allPropsIgnored></value></entry></map>",
                    parser.serialize(N.asMap("b", bean), lenient), type.toString());

            // the flag on (the default) still rejects it, with the empty-bean message
            final ParsingException ex = assertThrows(ParsingException.class, () -> parser.serialize(bean), type.toString());
            assertTrue(ex.getMessage().startsWith("No serializable property is found in class:"), ex.getMessage());
            assertThrows(ParsingException.class, () -> parser.serialize(bean, XmlSerConfig.create().setFailOnEmptyBean(true)), type.toString());
        }
    }

    // G01-08: a wrapper in map-KEY position was the one place left unwrapped, so an empty wrapper key wrote the
    // JSON literal "null" and, under writeTypeInfo, a type attribute the readers reject.
    @Test
    public void fixG01_optionalMapKeysAreUnwrappedLikeEveryOtherPosition() {
        final Map<Object, Object> wrapperKeys = new LinkedHashMap<>();
        wrapperKeys.put(u.Optional.empty(), "a");
        wrapperKeys.put(u.OptionalInt.of(5), "b");
        wrapperKeys.put(u.Nullable.of("k"), "c");

        for (final XmlParserType type : ALL_TYPES) {
            final XmlParser parser = parserOf(type);

            final String plain = parser.serialize(wrapperKeys);
            assertEquals("<map><entry><key isNull=\"true\" /><value>a</value></entry>" //
                    + "<entry><key>5</key><value>b</value></entry>" //
                    + "<entry><key>k</key><value>c</value></entry></map>", plain, type.toString());

            final String typed = parser.serialize(wrapperKeys, XmlSerConfig.create().setWriteTypeInfo(true));
            assertTrue(typed.contains("<key isNull=\"true\" />"), typed);
            assertTrue(typed.contains("<key type=\"Integer\">5</key>"), typed);
            assertTrue(typed.contains("<key type=\"String\">k</key>"), typed);
            assertTrue(!typed.contains("Optional"), typed);
            assertTrue(!typed.contains("Nullable"), typed);

            // the wrapper never reaches the writer, so the parser can read its own typed output back instead of
            // rejecting the wrapper's name ("XML type attribute is not allowed: ...")
            assertNotNull(parser.deserialize(typed, Map.class), typed);
            assertNotNull(parser.deserialize(plain, Map.class), plain);
        }
    }

    // G02-49: the child elements of a map entry are matched by name, not by position, in every reader. Read
    // positionally, StAX and DOM turned <key>a</key><key>b</key> into {a=b} and <value>1</value><key>k</key>
    // into {1=k}, both of which the SAX reader already rejected.
    @Test
    public void reviewFixes20260908_mapEntryChildElementsAreMatchedByName() {
        final String[] malformed = { "<map><entry><key>a</key><key>b</key></entry></map>", "<map><entry><value>1</value><key>k</key></entry></map>",
                "<map><entry><value>1</value><value>2</value></entry></map>" };

        for (final XmlParserType type : ALL_TYPES) {
            final XmlParser parser = parserOf(type);

            for (final String xml : malformed) {
                assertThrows(ParsingException.class, () -> parser.deserialize(xml, Map.class), type + " " + xml);
            }

            // The entry wrapper itself must be named <entry> in every reader. R03: the SAX reader was carved
            // out here and it did NOT reject an unknown wrapper - it classified it as a bean node, built the
            // bean and threw it away, so the entry was silently dropped and an empty map came back. The element
            // type is what makes that reproducible: it anchors the node-name scan at com.landawn.abacus, where
            // <personType> resolves. Without it the scan root is taken from the call stack, so whether the bean
            // path wins depends on who called.
            final XmlDeserConfig scanAnchored = new XmlDeserConfig().setElementType(PersonType.class);
            assertThrows(ParsingException.class,
                    () -> parser.deserialize("<map><personType><key>a</key><value>1</value></personType></map>", scanAnchored, Map.class), type.toString());
            assertThrows(ParsingException.class, () -> parser.deserialize("<map><pair><key>a</key><value>1</value></pair></map>", Map.class),
                    type.toString());
            assertThrows(ParsingException.class, () -> parser.deserialize("<map><foo>a</foo></map>", Map.class), type.toString());
            assertThrows(ParsingException.class, () -> parser.deserialize("<map><entry><key>a</key><value>1</value></entry><foo/></map>", Map.class),
                    type.toString());

            // the well-formed shapes are untouched, including the namespace-prefixed one
            assertEquals(Map.of("a", "1"), parser.deserialize("<map><entry><key>a</key><value>1</value></entry></map>", Map.class), type.toString());
            assertEquals(Map.of("a", "1"),
                    parser.deserialize("<p:map xmlns:p=\"urn:x\"><p:entry><p:key>a</p:key><p:value>1</p:value></p:entry></p:map>", Map.class),
                    type.toString());
        }
    }


    // G04-104: the DOM branches of read(..) look the target type up under the element's LOCAL name (or its
    // `name` attribute) but reported node.getNodeName() when nothing matched, naming a key they never searched
    // for - exactly the prefixed-root case the local-name lookup exists for.
    @Test
    public void fixG04_theDomFailureNamesTheKeyItLookedUp() {
        final String prefixed = "<ns:order xmlns:ns=\"urn:x\"><id>1</id></ns:order>";
        final Map<String, Type<?>> nodeTypes = N.asMap("nosuch", Type.of(Map.class));

        final ParsingException fromStream = assertThrows(ParsingException.class,
                () -> domParser.deserialize(new ByteArrayInputStream(prefixed.getBytes(StandardCharsets.UTF_8)), null, nodeTypes));
        assertTrue(fromStream.getMessage().contains("xml node: order"), fromStream.getMessage());
        assertTrue(fromStream.getMessage().contains("<ns:order>"), fromStream.getMessage());

        final ParsingException fromReader = assertThrows(ParsingException.class,
                () -> domParser.deserialize(new StringReader(prefixed), null, nodeTypes));
        assertTrue(fromReader.getMessage().contains("xml node: order"), fromReader.getMessage());
        assertTrue(fromReader.getMessage().contains("<ns:order>"), fromReader.getMessage());

        // an unprefixed root is named once: the key and the element are the same string
        final ParsingException plain = assertThrows(ParsingException.class,
                () -> domParser.deserialize(new StringReader("<order><id>1</id></order>"), null, nodeTypes));
        assertEquals("No target class is specified for xml node: order", plain.getMessage());
    }

    // R03 review of G04-104: the StAX branches look the target type up under the root's `name` attribute,
    // falling back to its local name, but reported getLocalName() - naming a key they never searched for
    // whenever a name attribute is present. The same defect the DOM branches were corrected for.
    @Test
    public void reviewFixes20260908_theStaxFailureNamesTheKeyItLookedUp() {
        final String named = "<bean name=\"order\"><id>1</id></bean>";
        final Map<String, Type<?>> nodeTypes = N.asMap("nosuch", Type.of(Map.class));

        final ParsingException fromStream = assertThrows(ParsingException.class,
                () -> staxParser.deserialize(new ByteArrayInputStream(named.getBytes(StandardCharsets.UTF_8)), null, nodeTypes));
        assertTrue(fromStream.getMessage().contains("xml node: order"), fromStream.getMessage());
        assertTrue(fromStream.getMessage().contains("<bean>"), fromStream.getMessage());

        final ParsingException fromReader = assertThrows(ParsingException.class, () -> staxParser.deserialize(new StringReader(named), null, nodeTypes));
        assertTrue(fromReader.getMessage().contains("xml node: order"), fromReader.getMessage());
        assertTrue(fromReader.getMessage().contains("<bean>"), fromReader.getMessage());

        // a root with no name attribute is named once: the key and the element are the same string
        final ParsingException plainStax = assertThrows(ParsingException.class,
                () -> staxParser.deserialize(new StringReader("<order><id>1</id></order>"), null, nodeTypes));
        assertEquals("No target type is specified for xml node: order", plainStax.getMessage());

        // and a prefixed root is looked up - and reported - under its local name
        final ParsingException prefixedStax = assertThrows(ParsingException.class,
                () -> staxParser.deserialize(new StringReader("<ns:order xmlns:ns=\"urn:x\"><id>1</id></ns:order>"), null, nodeTypes));
        assertEquals("No target type is specified for xml node: order", prefixedStax.getMessage());
    }

    /** A bean whose custom property name is escapable in attribute position but not writable as an element name. */
    public static class MarkupXmlPropNameBean {
        @JsonXmlField(name = "a&b")
        public String value;
    }

    /** A custom property name that attribute escaping turns into a character reference XML 1.0 forbids. */
    public static class ControlCharXmlPropNameBean {
        @JsonXmlField(name = "a" + (char) 1 + "b")
        public String value;
    }

    /** A custom property name that needs no escaping and is a valid element name. */
    public static class PlainCustomXmlPropNameBean {
        @JsonXmlField(name = "user_id")
        public String value;
    }

    // X03/R03-8: the r9506 escaping fix covered only the <property name=".."> (attribute) style, and the
    // follow-up element-name check landed in XmlParserImpl only. This writer still interpolated the annotation
    // name straight into element-name position under the DEFAULT tagByPropertyName=true, so
    // @JsonXmlField(name = "a&b") wrote <bean><a&b>v</a&b></bean> - a document neither backend can read back.
    // Attribute position is escaped, but escaping cannot rescue a character XML 1.0 has no representation for:
    // name="a&#x1;b" is rejected by this parser's own reader. Both checks now match XmlParserImpl exactly.
    @Test
    public void reviewX03_R03_8_beanPropertyCustomNameIsCheckedInBothStyles() {
        final XmlSerConfig generic = XmlSerConfig.create().setTagByPropertyName(false);

        final MarkupXmlPropNameBean markup = new MarkupXmlPropNameBean();
        markup.value = "v";
        final ControlCharXmlPropNameBean ctrl = new ControlCharXmlPropNameBean();
        ctrl.value = "v";

        for (final XmlParser parser : new XmlParser[] { staxParser, domParser }) {
            // element-name position: the same message XmlParserImpl reports
            assertEquals("Property name 'a&b' is not a valid XML element name",
                    assertThrows(ParsingException.class, () -> parser.serialize(markup)).getMessage());
            assertEquals("Property name 'a" + (char) 1 + "b' is not a valid XML element name",
                    assertThrows(ParsingException.class, () -> parser.serialize(ctrl)).getMessage());

            // attribute position: markup is escapable and still written; U+0001 is not representable at all
            assertEquals("<bean name=\"markupXmlPropNameBean\"><property name=\"a&amp;b\">v</property></bean>", parser.serialize(markup, generic));
            assertEquals("Property name 'a" + (char) 1 + "b' contains U+0001, which cannot be represented in XML 1.0",
                    assertThrows(ParsingException.class, () -> parser.serialize(ctrl, generic)).getMessage());

            // a property that is not written cannot make serialization fail, as for a bean property in the
            // sibling writer: the check sits after the ignore/exclusion filters
            assertEquals("<markupXmlPropNameBean></markupXmlPropNameBean>", parser.serialize(new MarkupXmlPropNameBean()));
            assertEquals("<markupXmlPropNameBean></markupXmlPropNameBean>",
                    parser.serialize(markup, XmlSerConfig.create().setIgnoredPropNames(MarkupXmlPropNameBean.class, Set.of("value"))));

            // and a valid custom name is written - and read back - byte-for-byte as before
            final PlainCustomXmlPropNameBean plain = new PlainCustomXmlPropNameBean();
            plain.value = "v";
            assertEquals("<plainCustomXmlPropNameBean><user_id>v</user_id></plainCustomXmlPropNameBean>", parser.serialize(plain));
            assertEquals("v", parser.deserialize(parser.serialize(plain), PlainCustomXmlPropNameBean.class).value);
            assertEquals("<bean name=\"plainCustomXmlPropNameBean\"><property name=\"user_id\">v</property></bean>", parser.serialize(plain, generic));
        }
    }
    // R02-3 (2026-09-08): the G04-85 fix added toElementNode to XmlParserImpl so that an org.w3c.dom.Document -
    // what DocumentBuilder.parse returns, and what most callers hand to deserialize(Node, ..) - is read as its
    // document element. AbacusXmlParserImpl, the sibling DOM reader with the identical overloads, was not
    // converted: it answered null for the Type/Class forms and threw
    // "No target class is specified for xml node: #document" for the nodeTypes form.
    @Test
    public void reviewFixes20260908_aDocumentIsReadAsItsDocumentElement() throws Exception {
        final Document doc = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(new ByteArrayInputStream("<person><name>NodeTest</name><age>40</age></person>".getBytes(StandardCharsets.UTF_8)));
        final Map<String, Type<?>> nodeTypes = N.asMap("person", Type.of(Person.class));

        // deserialize(Node, ..) always reads through the DOM reader, whichever backend the parser was built with
        for (final XmlParser parser : new XmlParser[] { staxParser, domParser, new AbacusXmlParserImpl(XmlParserType.SAX) }) {
            final Person byType = parser.deserialize(doc, null, Type.of(Person.class));
            assertEquals("NodeTest", byType.getName());
            assertEquals(40, byType.getAge());

            assertEquals("NodeTest", parser.deserialize(doc, null, Person.class).getName());
            assertEquals("NodeTest", parser.deserialize(doc, Type.of(Person.class)).getName());
            assertEquals("NodeTest", parser.deserialize(doc, Person.class).getName());
            assertEquals("NodeTest", ((Person) parser.deserialize(doc, null, nodeTypes)).getName());

            // the document element itself still reads the same way
            assertEquals("NodeTest", parser.deserialize(doc.getDocumentElement(), null, Person.class).getName());
            assertEquals("NodeTest", ((Person) parser.deserialize(doc.getDocumentElement(), null, nodeTypes)).getName());
        }

        // a document without a document element has no element to read
        final Document noRoot = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        assertNull(domParser.deserialize(noRoot, null, Person.class));
    }

    // R03-11 (2026-09-08): when the anchor class is a JDK class (a Map/List target), node-name discovery guesses
    // the package to scan from the call stack. Under the SAX backend the frames below the parser belong to the XML
    // implementation itself, so a caller inside com.landawn.abacus.parser resolves com.sun.org and
    // ClassUtil.findClassesInPackage threw "No resource found for package: com.sun.org" - an internal
    // IllegalArgumentException escaping a public deserialize call instead of the descriptive ParsingException.
    // Run on a dedicated thread so the frame below the parser is fixed by this test rather than by the runner.
    @Test
    public void reviewFixes20260908_anUnscannableGuessedPackageStillReportsParsingException() throws Exception {
        final String xml = "<list><zzR0311Unresolvable><a>1</a></zzR0311Unresolvable></list>";
        final Throwable[] thrown = new Throwable[1];

        final Thread caller = new Thread(() -> {
            try {
                new AbacusXmlParserImpl(XmlParserType.SAX).deserialize(xml, List.class);
            } catch (final Throwable t) {
                thrown[0] = t;
            }
        });
        caller.start();
        caller.join();

        assertTrue(thrown[0] instanceof ParsingException, "expected ParsingException, got " + thrown[0]);
        assertTrue(thrown[0].getMessage().contains("zzR0311Unresolvable"), thrown[0].getMessage());

        // the StAX and DOM readers, which have no such frames, already reported it this way
        for (final XmlParserType type : new XmlParserType[] { XmlParserType.StAX, XmlParserType.DOM }) {
            assertThrows(ParsingException.class, () -> new AbacusXmlParserImpl(type).deserialize(xml, List.class), type.toString());
        }
    }
}
