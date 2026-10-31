package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
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

    private static java.util.Set<Class<?>> approvedTypes() {
        final java.util.Set<Class<?>> types = new java.util.HashSet<>(java.util.Arrays.asList(AbacusXmlParserImplTest.class.getDeclaredClasses()));
        types.add(java.time.DayOfWeek.class);
        types.add(PersonType.class);
        types.add(RecordB.class);
        return types;
    }

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
        staxParser = new AbacusXmlParserImpl(XmlParserType.StAX, null, null, approvedTypes());
        domParser = new AbacusXmlParserImpl(XmlParserType.DOM, null, null, approvedTypes());
    }

    private Node firstChild(final String xml) throws Exception {
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes()));
        return doc.getFirstChild();
    }

    // R4-01: an adopted type attribute naming a class whose codec cannot read text at all (Number) is a document error, reported as
    // ParsingException naming the attribute, the same on every backend and at every site (bean property, map value, map key, list
    // item under a typed root; a map value under an untyped root, where values adopt scalar attributes) instead of the codec's
    // UnsupportedOperationException. Value errors of working codecs keep the codec's own exception, as for declared types and in
    // JSON; abstract classes with a working codec (Calendar) and interfaces the Object handler reads (CharSequence) still convert
    @Test
    public void testAdoptedTypeAttribute_unreadableCodecIsParsingExceptionOnAllBackends() {
        final String holderXml = parserOf(XmlParserType.StAX).serialize(new R2BHolder(), XmlSerConfig.create().setWriteTypeInfo(true));
        final String holderTag = holderXml.substring(0, holderXml.indexOf('>') + 1);
        final String holderName = holderTag.substring(1, holderTag.indexOf(' '));
        final String holderEnd = "</" + holderName + ">";
        final Type<R2BHolder> holderType = Type.of(R2BHolder.class);
        final Type<Map<String, Object>> stringMapType = Type.of("Map<String, Object>");
        final Type<Map<Object, Object>> openMapType = Type.of("Map<Object, Object>");
        final Type<List<Object>> listType = Type.of("List<Object>");
        final String expectedMessage = "XML type attribute 'Number' names java.lang.Number, whose values cannot be read from element text";

        r4ParsingExceptionOnAllBackends(holderTag + "<open type=\"Number\">5</open>" + holderEnd, holderType, expectedMessage);
        r4ParsingExceptionOnAllBackends("<map type=\"HashMap&lt;String, Object&gt;\"><entry><key>k</key><value type=\"Number\">5</value></entry></map>",
                stringMapType, expectedMessage);
        r4ParsingExceptionOnAllBackends("<map type=\"HashMap&lt;Object, Object&gt;\"><entry><key type=\"Number\">5</key><value>v</value></entry></map>",
                openMapType, expectedMessage);
        r4ParsingExceptionOnAllBackends("<list type=\"ArrayList&lt;Object&gt;\"><e type=\"Number\">5</e></list>", listType, expectedMessage);
        r4ParsingExceptionOnAllBackends("<map><entry><key>k</key><value type=\"Number\">5</value></entry></map>", stringMapType, expectedMessage);

        // under an untyped root a bean property does not adopt the attribute: the text is the value
        assertEquals("5", r2bSameOnAllBackends("<" + holderName + "><open type=\"Number\">5</open>" + holderEnd, null, holderType).getOpen());

        // a working codec's own value errors propagate unchanged: a Long that is not a number, a date text that is ambiguous or unparsable
        for (final XmlParserType parserType : ALL_TYPES) {
            final XmlParser parser = parserOf(parserType);
            assertThrows(NumberFormatException.class, () -> parser.deserialize(holderTag + "<open type=\"Long\">garbage</open>" + holderEnd, null, holderType),
                    parserType.name());
            assertThrows(IllegalArgumentException.class,
                    () -> parser.deserialize(holderTag + "<open type=\"Timestamp\">5</open>" + holderEnd, null, holderType), parserType.name());
            assertThrows(IllegalArgumentException.class,
                    () -> parser.deserialize(holderTag + "<open type=\"Timestamp\">garbage</open>" + holderEnd, null, holderType), parserType.name());
        }

        assertEquals(java.sql.Timestamp.valueOf("2024-01-02 03:04:05"),
                r2bSameOnAllBackends(holderTag + "<open type=\"Timestamp\">2024-01-02 03:04:05</open>" + holderEnd, null, holderType).getOpen());
        assertInstanceOf(java.util.Calendar.class,
                r2bSameOnAllBackends(holderTag + "<open type=\"Calendar\">2024-01-02T03:04:05Z</open>" + holderEnd, null, holderType).getOpen());
        assertEquals("5", r2bSameOnAllBackends(holderTag + "<open type=\"CharSequence\">5</open>" + holderEnd, null, holderType).getOpen());
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

    // R5-02: a value type configured for a bean property types its text on every backend, as on the DOM reader (and the standard
    // readers): the configuration wins over a scalar attribute, with and without type information; an Object configuration keeps
    // scalar text, wrapper (Optional) metadata included, and adopts container metadata; a formatted property keeps its format. The
    // StAX and SAX readers read a configured scalar type with the declared type before.
    @Test
    public void testConfiguredValueTypeOfBeanProperty_sameOnAllBackends() {
        final String root = r5TypedRoot(new R2BHolder());
        final String end = r5EndTag(root);
        final String untyped = r5UntypedRoot(root);
        final Type<R2BHolder> type = Type.of(R2BHolder.class);
        final XmlDeserConfig longOpen = XmlDeserConfig.create().setValueType("open", Type.of(Long.class));
        final XmlDeserConfig objectOpen = XmlDeserConfig.create().setValueType("open", Type.of(Object.class));

        for (final String r : new String[] { root, untyped }) {
            assertEquals(5L, r2bSameOnAllBackends(r + "<open>5</open>" + end, longOpen, type).getOpen(), r);
            assertEquals(5L, r2bSameOnAllBackends(r + "<open type=\"Integer\">5</open>" + end, longOpen, type).getOpen(), r);
            assertEquals(5L, r2bSameOnAllBackends(r + "<open type=\"Optional&lt;Long&gt;\">5</open>" + end, longOpen, type).getOpen(), r);
            assertNull(r2bSameOnAllBackends(r + "<open></open>" + end, longOpen, type).getOpen(), r);
            assertEquals("5", r2bSameOnAllBackends(r + "<open type=\"Integer\">5</open>" + end, objectOpen, type).getOpen(), r);
            assertEquals("5", r2bSameOnAllBackends(r + "<open type=\"Optional&lt;Long&gt;\">5</open>" + end, objectOpen, type).getOpen(), r);
            assertEquals(List.of(1, 2), r2bSameOnAllBackends(r + "<open type=\"List&lt;Integer&gt;\">[1, 2]</open>" + end, objectOpen, type).getOpen(), r);
            for (final XmlParserType parserType : ALL_TYPES) {
                assertThrows(NumberFormatException.class, () -> parserOf(parserType).deserialize(r + "<open>x</open>" + end, longOpen, type),
                        parserType + " " + r);
            }
        }

        // a configured type on a formatted property: the format reads the text on every backend
        final XmlDeserConfig longWhen = XmlDeserConfig.create().setValueType("when", Type.of(Long.class));
        final java.util.Date when = r2bSameOnAllBackends(root + "<when type=\"JUDate\">2024-01-02</when>" + end, longWhen, type).getWhen();
        assertEquals("2024-01-02", new java.text.SimpleDateFormat("yyyy-MM-dd").format(when));
    }

    // R5-05: a nested element in a String key or value slot is read with the slot's container metadata on every backend, as in an
    // open slot (the StAX reader read String items, the DOM reader typed a list but not a map); bean metadata does not open the
    // slot on any backend, and text in the slot stays the declared String.
    @Test
    public void testContainerMetadataOnNestedElementInStringSlot_sameOnAllBackends() {
        final String childClass = R2BChild.class.getCanonicalName();
        final String childElement = parserOf(XmlParserType.StAX).serialize(r2bChild("n", 1));
        final String list = "<list><e>1</e><e>2</e></list>";
        final String map = "<map><entry><key>a</key><value>1</value></entry></map>";
        final Type<Map<String, Object>> stringKeys = Type.of("Map<String, Object>");
        final Type<Map<Object, String>> stringValues = Type.of("Map<Object, String>");

        for (final String root : new String[] { "<map type=\"HashMap&lt;Object, Object&gt;\">", "<map>" }) {
            for (final String attr : new String[] { "List&lt;Integer&gt;", "ImmutableList&lt;Integer&gt;" }) {
                assertEquals(Map.of(List.of(1, 2), "v"),
                        r2bSameOnAllBackends(root + "<entry><key type=\"" + attr + "\">" + list + "</key><value>v</value></entry></map>", null, stringKeys),
                        root);
                assertEquals(Map.of("k", List.of(1, 2)),
                        r2bSameOnAllBackends(root + "<entry><key>k</key><value type=\"" + attr + "\">" + list + "</value></entry></map>", null, stringValues),
                        root);
            }
            for (final String attr : new String[] { "Map&lt;String, Integer&gt;", "ImmutableMap&lt;String, Integer&gt;" }) {
                assertEquals(Map.of(Map.of("a", 1), "v"),
                        r2bSameOnAllBackends(root + "<entry><key type=\"" + attr + "\">" + map + "</key><value>v</value></entry></map>", null, stringKeys),
                        root);
                assertEquals(Map.of("k", Map.of("a", 1)),
                        r2bSameOnAllBackends(root + "<entry><key>k</key><value type=\"" + attr + "\">" + map + "</value></entry></map>", null, stringValues),
                        root);
            }
            // no metadata: the nested list keeps String items, as before on every backend; text stays the declared String
            assertEquals(Map.of("k", List.of("1", "2")),
                    r2bSameOnAllBackends(root + "<entry><key>k</key><value>" + list + "</value></entry></map>", null, stringValues), root);
            assertEquals(Map.of("k", "[1, 2]"),
                    r2bSameOnAllBackends(root + "<entry><key>k</key><value type=\"List&lt;Integer&gt;\">[1, 2]</value></entry></map>", null, stringValues),
                    root);
            // bean metadata does not open a String slot: the nested element is not read as a bean on any backend
            r5SameFailureOnAllBackends(root + "<entry><key>k</key><value type=\"" + childClass + "\">" + childElement + "</value></entry></map>", null,
                    stringValues, ParsingException.class, "No bean class found for node name: ");
        }
    }

    // R5-04: container metadata on a nested map key or value is the container on every backend: under an explicit Object override
    // (the StAX reader read the nested element through an open slot) and on an open key with an uninstantiable container (the DOM
    // reader read it through the open Object key), a nested element that is no item of the container is rejected as without the
    // override or with an instantiable container, and a nested list or map is read typed.
    @Test
    public void testContainerMetadataOnNestedKeyOrOverriddenValue_sameOnAllBackends() {
        final String childElement = parserOf(XmlParserType.StAX).serialize(r2bChild("n", 1));
        final XmlDeserConfig override = XmlDeserConfig.create().setValueType("k", Type.of(Object.class));
        final Type<Map<String, Object>> stringMapType = Type.of("Map<String, Object>");
        final Type<Map<Object, Object>> openMapType = Type.of("Map<Object, Object>");
        final String notAnItem = "No bean class found for node name: ";
        final String list = "<list><e>1</e><e>2</e></list>";
        final String map = "<map><entry><key>a</key><value>1</value></entry></map>";

        for (final String root : new String[] { "<map type=\"HashMap&lt;Object, Object&gt;\">", "<map>" }) {
            for (final String attr : new String[] { "List&lt;Integer&gt;", "ImmutableList&lt;Integer&gt;", "Object[]", "Map&lt;String, Integer&gt;",
                    "ImmutableMap&lt;String, Integer&gt;" }) {
                r5SameFailureOnAllBackends(root + "<entry><key>k</key><value type=\"" + attr + "\">" + childElement + "</value></entry></map>", override,
                        stringMapType, ParsingException.class, notAnItem);
                if (!attr.startsWith("Object")) {
                    r5SameFailureOnAllBackends(root + "<entry><key type=\"" + attr + "\">" + childElement + "</key><value>v</value></entry></map>", null,
                            openMapType, ParsingException.class, notAnItem);
                }
            }
            assertEquals(Map.of("k", List.of(1, 2)), r2bSameOnAllBackends(
                    root + "<entry><key>k</key><value type=\"List&lt;Integer&gt;\">" + list + "</value></entry></map>", override, stringMapType), root);
            assertEquals(Map.of("k", Map.of("a", 1)), r2bSameOnAllBackends(
                    root + "<entry><key>k</key><value type=\"Map&lt;String, Integer&gt;\">" + map + "</value></entry></map>", override, stringMapType), root);
            assertEquals(Map.of(List.of(1, 2), "v"), r2bSameOnAllBackends(
                    root + "<entry><key type=\"ImmutableList&lt;Integer&gt;\">" + list + "</key><value>v</value></entry></map>", null, openMapType), root);
            // without the override, the open value already agreed: the nested bean element is no item of the list
            r5SameFailureOnAllBackends(root + "<entry><key>k</key><value type=\"List&lt;Integer&gt;\">" + childElement + "</value></entry></map>", null,
                    stringMapType, ParsingException.class, notAnItem);
        }
    }

    // R5-01: a type attribute naming the declared class of a property whose codec cannot read text at all (Number) is the
    // ParsingException naming the attribute on every backend; it was that ParsingException on the DOM reader and the codec's
    // UnsupportedOperationException on the StAX and SAX readers, also for the writer's own typed output. Without the attribute
    // the declared codec's own exception stays (unchanged), a configured value type reads the text, an empty element is null.
    @Test
    public void testDeclaredNumberPropertyWithItsOwnTypeAttribute_sameOnAllBackends() {
        final R5NumberHolder holder = new R5NumberHolder();
        holder.setN(5);
        final String typed = parserOf(XmlParserType.StAX).serialize(holder, XmlSerConfig.create().setWriteTypeInfo(true));
        assertTrue(typed.contains("<n type=\"Number\">5</n>"), typed);
        final String root = r5TypedRoot(holder);
        final String end = r5EndTag(root);
        final Type<R5NumberHolder> type = Type.of(R5NumberHolder.class);
        final String unreadable = "XML type attribute 'Number' names java.lang.Number, whose values cannot be read from element text";

        r5SameFailureOnAllBackends(typed, null, type, ParsingException.class, unreadable);
        r5SameFailureOnAllBackends(root + "<n type=\"Number\">5</n>" + end, null, type, ParsingException.class, unreadable);
        r5SameFailureOnAllBackends(r5UntypedRoot(root) + "<n type=\"Number\">5</n>" + end, null, type, ParsingException.class, unreadable);
        for (final XmlParserType parserType : ALL_TYPES) {
            final ParsingException e = assertThrows(ParsingException.class, () -> parserOf(parserType).deserialize(typed, type), parserType.name());
            assertInstanceOf(UnsupportedOperationException.class, e.getCause(), parserType.name());
        }

        // no attribute: the declared codec's own failure, as before on every backend
        r5SameFailureOnAllBackends(root + "<n>5</n>" + end, null, type, UnsupportedOperationException.class, "No public static method or constructor found");
        // a configured value type types the text, the empty element and the null marker are null
        assertEquals(5L, r2bSameOnAllBackends(typed, XmlDeserConfig.create().setValueType("n", Type.of(Long.class)), type).getN());
        assertNull(r2bSameOnAllBackends(root + "<n type=\"Number\"></n>" + end, null, type).getN());
        assertNull(r2bSameOnAllBackends(root + "<n type=\"Number\" isNull=\"true\"></n>" + end, null, type).getN());
    }

    // deserialize

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

    // G144: DOM reads a bean with a type attribute in an untyped ARRAY item (the branch G143 changed for lists), and a list of an
    // INTERFACE element type reads its nested items as Object slots on every backend: DOM passed the interface, so it took the
    // node-name class instead of a misfitting type attribute and could not read a bean outside com.landawn.abacus.
    @Test
    public void testDeserialize_arrayItemsAndInterfaceTypedItemsParity() {
        final testfixtures.entity.AccountContact contact = new testfixtures.entity.AccountContact();
        contact.setMobile("m");
        final String bean = "<accountContact type=\"" + testfixtures.entity.AccountContact.class.getName() + "\"><mobile>m</mobile></accountContact>";
        final Type<Object> shapes = Type.of("List<" + G142Shape.class.getCanonicalName() + ">");
        final String misfit = "<list><e><g142Circle type=\"" + G142Other.class.getCanonicalName() + "\"><radius>1</radius></g142Circle></e></list>";
        final String foreign = "<list><e>" + bean + "</e></list>";
        final AbacusXmlParserImpl stax = new AbacusXmlParserImpl(XmlParserType.StAX, null, null, g144Approved());

        for (final XmlParserType parserType : ALL_TYPES) {
            final AbacusXmlParserImpl parser = new AbacusXmlParserImpl(parserType, null, null, g144Approved());

            assertArrayEquals(new Object[] { contact }, parser.deserialize("<array><e>" + bean + "</e></array>", Object[].class), parserType.name());

            for (final String xml : new String[] { misfit, foreign }) {
                assertEquals(g144Outcome(() -> stax.deserialize(xml, XmlDeserConfig.create(), shapes)),
                        g144Outcome(() -> parser.deserialize(xml, XmlDeserConfig.create(), shapes)), parserType.name() + ": " + xml);
            }
        }
    }

    // G141: a bean held in an Object property is read through its type attribute on every backend; StAX already used it, DOM and
    // SAX only tried node-name discovery, which searches the com.landawn.abacus packages: "No bean class found for node name:
    // accountContact" for a bean anywhere else. Object map values and List<Object> elements already worked (neighbours).
    @Test
    public void testDeserialize_beanInObjectSlotUsesTypeAttribute() {
        final testfixtures.entity.AccountContact contact = new testfixtures.entity.AccountContact();
        contact.setMobile("m");
        contact.setStatus(2);
        final G141Holder holder = new G141Holder();
        holder.setOther(contact);
        holder.setValues(new LinkedHashMap<>(Map.of("k", contact)));
        holder.setItems(List.of(contact));
        final XmlSerConfig withTypes = XmlSerConfig.create().setWriteTypeInfo(true);
        final Set<Class<?>> approved = new java.util.HashSet<>(approvedTypes());
        approved.add(testfixtures.entity.AccountContact.class);

        for (final XmlParserType parserType : ALL_TYPES) {
            final AbacusXmlParserImpl parser = new AbacusXmlParserImpl(parserType, null, null, approved);
            final G141Holder read = parser.deserialize(parser.serialize(holder, withTypes), G141Holder.class);

            assertEquals(contact, read.getOther(), parserType.name());
            assertEquals(contact, read.getValues().get("k"), parserType.name());
            assertEquals(List.of(contact), read.getItems(), parserType.name());

            // Without type information such a bean cannot be found by its node name: the open Object property reads it as a map
            // of its properties (2026-10-06; it used to fail with "No bean class found").
            final G141Holder plain = new G141Holder();
            plain.setOther(contact);
            final Object other = parser.deserialize(parser.serialize(plain), G141Holder.class).getOther();
            assertEquals("m", assertInstanceOf(Map.class, other, parserType.name()).get("mobile"), parserType.name());
        }
    }

    // G143: DOM reads a bean with a type attribute directly inside an untyped list item or map value (<e>, <value> without a
    // type attribute), as StAX and SAX do: it read such a slot as String, which no bean fits, and failed with "No bean class
    // found for node name". Root lists and maps as well.
    @Test
    public void testDeserialize_beanInUntypedItemOrValueOnEveryBackend() {
        final testfixtures.entity.AccountContact contact = new testfixtures.entity.AccountContact();
        contact.setMobile("m");
        final String bean = "<accountContact type=\"" + testfixtures.entity.AccountContact.class.getName() + "\"><mobile>m</mobile></accountContact>";
        final String holderStart = "<g141Holder type=\"" + G141Holder.class.getCanonicalName() + "\">";
        final Set<Class<?>> approved = new java.util.HashSet<>(approvedTypes());
        approved.add(testfixtures.entity.AccountContact.class);

        for (final XmlParserType parserType : ALL_TYPES) {
            final AbacusXmlParserImpl parser = new AbacusXmlParserImpl(parserType, null, null, approved);

            final G141Holder items = parser.deserialize(holderStart + "<items><list><e>" + bean + "</e></list></items></g141Holder>", G141Holder.class);
            assertEquals(List.of(contact), items.getItems(), parserType.name());

            final G141Holder values = parser.deserialize(
                    holderStart + "<values><map><entry><key>k</key><value>" + bean + "</value></entry></map></values></g141Holder>", G141Holder.class);
            assertEquals(Map.of("k", contact), values.getValues(), parserType.name());

            assertEquals(List.of(contact), parser.deserialize("<list><e>" + bean + "</e></list>", List.class), parserType.name());
            assertEquals(Map.of("k", contact), parser.deserialize("<map><entry><key>k</key><value>" + bean + "</value></entry></map>", Map.class),
                    parserType.name());
        }
    }

    // G006-01 neighbour: a bean in an Object property that is of the same class as the preceding list's element type (which SAX used
    // to get right only through the stale element type) is still read back on every backend, now by node-name discovery.
    @Test
    public void testDeserialize_beanOfListElementClassInObjectPropertyStillReadOnAllBackends() {
        final G006Alpha alpha = new G006Alpha();
        alpha.setAlphaName("a1");
        final G006Alpha other = new G006Alpha();
        other.setAlphaName("a2");
        final G006Holder holder = new G006Holder();
        holder.setAlphas(List.of(alpha));
        holder.setOther(other);

        final G006Carrier inner = new G006Carrier();
        inner.setCarrierName("inner");
        final G006Carrier outer = new G006Carrier();
        outer.setCarrierName("outer");
        outer.setPayload(inner);
        final G006Box box = new G006Box();
        box.setCarriers(List.of(outer));

        for (final XmlParserType parserType : XmlParserType.values()) {
            final AbacusXmlParserImpl parser = new AbacusXmlParserImpl(parserType);

            assertEquals(holder, parser.deserialize(parser.serialize(holder), G006Holder.class), parserType.name());
            assertEquals(box, parser.deserialize(parser.serialize(box), G006Box.class), parserType.name());
        }
    }

    // G121 neighbours: below the root the configured bean element type still types the items of a list/array root, and SAX still
    // discovers a bean nested in an Object property of such an item through the configured element type.
    @Test
    public void testDeserialize_configuredBeanElementTypeStillTypesNestedElements() {
        final G006Alpha alpha = new G006Alpha();
        alpha.setAlphaName("a");
        final G006Carrier inner = new G006Carrier();
        inner.setCarrierName("inner");
        final G006Carrier outer = new G006Carrier();
        outer.setCarrierName("outer");
        outer.setPayload(inner);
        final G006Box box = new G006Box();
        box.setCarriers(List.of(outer));

        for (final XmlParserType parserType : XmlParserType.values()) {
            final AbacusXmlParserImpl parser = new AbacusXmlParserImpl(parserType, null, null, approvedTypes());
            final XmlDeserConfig alphaElements = XmlDeserConfig.create().setElementType(G006Alpha.class);

            assertEquals(List.of(alpha), parser.deserialize(parser.serialize(List.of(alpha)), alphaElements, List.class), parserType.name());

            final Object[] array = parser.deserialize(parser.serialize(new G006Alpha[] { alpha }), alphaElements, Object[].class);
            assertEquals(1, array.length, parserType.name());
            assertEquals(alpha, array[0], parserType.name());
        }

        final AbacusXmlParserImpl saxParser = new AbacusXmlParserImpl(XmlParserType.SAX, null, null, approvedTypes());
        assertEquals(List.of(box), saxParser.deserialize(saxParser.serialize(List.of(box)), XmlDeserConfig.create().setElementType(G006Box.class), List.class));
    }

    // DOM (and StAX/SAX): a configured Map<String, Integer> value keeps its type arguments, while an unconfigured key of the
    // same map still reads with the default value type.
    @Test
    public void testDeserialize_configuredMapValueTypeArgumentsKeptOnAllBackends() {
        final String xml = "<map><entry><key>x</key><value><map><entry><key>a</key><value>1</value></entry></map></value></entry>"
                + "<entry><key>y</key><value>3</value></entry></map>";

        for (final XmlParserType parserType : ALL_TYPES) {
            final AbacusXmlParserImpl parser = new AbacusXmlParserImpl(parserType);
            final XmlDeserConfig config = new XmlDeserConfig().setValueType("x", Type.of("Map<String, Integer>"));
            final Map<String, Object> result = parser.deserialize(xml, config, Type.<Map<String, Object>> of("Map<String, Object>"));

            assertEquals(Map.of("a", 1), result.get("x"), parserType.toString());
            assertEquals("3", result.get("y"), parserType.toString());
        }
    }

    // G006-02: the type attribute of a <key>/<value> wrapper holding a nested element is approved on DOM also when it is not used (a
    // configured value type, or a bean read without type information), as on StAX and SAX; DOM silently accepted an unapproved name.
    @Test
    public void testDeserialize_domApprovesNestedMapKeyAndValueWrapperTypeAttributes() {
        final String nestedValue = "<map><entry><key>a</key><value type=\"not.approved.Type\"><list><e>1</e></list></value></entry></map>";
        final String nestedKey = "<map><entry><key type=\"not.approved.Type\"><list><e>1</e></list></key><value>v</value></entry></map>";
        final XmlDeserConfig configured = XmlDeserConfig.create().setValueType("a", "List<Integer>");
        final Type<Map<String, Object>> mapType = Type.of("Map<String, Object>");

        for (final XmlParserType parserType : ALL_TYPES) {
            final AbacusXmlParserImpl parser = new AbacusXmlParserImpl(parserType, null, null, approvedTypes());

            assertThrows(ParsingException.class, () -> parser.deserialize(nestedValue, configured, mapType), parserType.name());
            assertThrows(ParsingException.class, () -> parser.deserialize("<g115KeyLoop><attrs>" + nestedValue + "</attrs></g115KeyLoop>", G115KeyLoop.class),
                    parserType.name());
            assertThrows(ParsingException.class, () -> parser.deserialize("<g115KeyLoop><attrs>" + nestedKey + "</attrs></g115KeyLoop>", G115KeyLoop.class),
                    parserType.name());

            // neighbours: an approved attribute is accepted in the same positions
            final String approvedValue = nestedValue.replace("not.approved.Type", "ArrayList");
            assertEquals(Map.of("a", List.of(1)), parser.deserialize(approvedValue, configured, mapType), parserType.name());
            assertEquals(Map.of("a", List.of("1")),
                    parser.deserialize("<g115KeyLoop><attrs>" + approvedValue + "</attrs></g115KeyLoop>", G115KeyLoop.class).getAttrs(), parserType.name());
        }
    }

    // G006-01: the DOM reader dropped the type arguments of a configured map value type (List<Integer> read as List<String>).
    @Test
    public void testDeserialize_domMapValueKeepsConfiguredType() {
        final String xml = "<map><entry><key>x</key><value><list><e>1</e><e>2</e></list></value></entry></map>";

        for (final XmlParserType parserType : new XmlParserType[] { XmlParserType.StAX, XmlParserType.DOM, XmlParserType.SAX }) {
            final AbacusXmlParserImpl parser = new AbacusXmlParserImpl(parserType);
            final XmlDeserConfig config = new XmlDeserConfig().setValueType("x", Type.of("List<Integer>"));
            final Map<String, Object> result = parser.deserialize(xml, config, Type.<Map<String, Object>> of("Map<String, Object>"));

            assertEquals(Arrays.asList(1, 2), result.get("x"), parserType.toString());
        }
    }

    // G121: at the root a configured bean element type no longer turns a bean root into that bean for a List/Map/Object
    // target - DOM (List/Map/Object) and SAX (List/Map) returned a G006Alpha where a List was requested; StAX rejected it.
    // A Map target reads the bean root as a map of its properties on every backend (2026-10-06; it used to be rejected).
    @Test
    public void testDeserialize_domRootBeanIgnoresConfiguredElementType() {
        final G006Alpha alpha = new G006Alpha();
        alpha.setAlphaName("a");
        final String xml = staxParser.serialize(alpha);
        final XmlDeserConfig config = XmlDeserConfig.create().setElementType(G006Alpha.class);

        for (final XmlParserType parserType : new XmlParserType[] { XmlParserType.StAX, XmlParserType.DOM, XmlParserType.SAX }) {
            final AbacusXmlParserImpl parser = new AbacusXmlParserImpl(parserType, null, null, approvedTypes());
            assertThrows(ParsingException.class, () -> parser.deserialize(xml, config, List.class), parserType.name());
            assertEquals("a", assertInstanceOf(Map.class, parser.deserialize(xml, config, Map.class), parserType.name()).get("alphaName"), parserType.name());

            // unchanged: a bean target reads the bean, with or without the element type
            assertEquals("a", parser.deserialize(xml, config, G006Alpha.class).getAlphaName(), parserType.name());
            assertEquals("a", parser.deserialize(xml, G006Alpha.class).getAlphaName(), parserType.name());
        }
    }

    // G006-02: DOM started the root with the configured element type as the declared type, so a List<Integer> element type
    // was applied one level too deep: [[1, 2]] read back as [[[1], [2]]] (StAX and SAX read [[1, 2]]).
    @Test
    public void testDeserialize_domRootConfiguredContainerElementType() {
        final XmlDeserConfig config = XmlDeserConfig.create().setElementType(Type.of("List<Integer>"));

        for (final XmlParserType parserType : XmlParserType.values()) {
            final AbacusXmlParserImpl parser = new AbacusXmlParserImpl(parserType);

            final List<?> list = parser.deserialize("<list><e><list><e>1</e><e>2</e></list></e></list>", config, List.class);
            assertEquals(List.of(List.of(1, 2)), list, parserType.name());

            final Object[] array = parser.deserialize("<array><e><list><e>1</e><e>2</e></list></e></array>", config, Object[].class);
            assertEquals(1, array.length, parserType.name());
            assertEquals(List.of(1, 2), array[0], parserType.name());
        }
    }

    // G006-02 completed for a map root: DOM took the key/value types of a configured Map<K, V> element type for the root map itself,
    // overriding even an explicitly typed target (key "7" read as a String, or NumberFormatException on key "a").
    @Test
    public void testDeserialize_domRootMapIgnoresConfiguredMapElementType() {
        final String nested = "<map><entry><key>7</key><value><list><e><map><entry><key>x</key><value>1</value></entry></map></e></list></value></entry></map>";
        final String flat = "<map><entry><key>a</key><value>1</value></entry></map>";
        final XmlDeserConfig stringIntegerElements = XmlDeserConfig.create().setElementType(Type.of("Map<String, Integer>"));
        final XmlDeserConfig integerIntegerElements = XmlDeserConfig.create().setElementType(Type.of("Map<Integer, Integer>"));

        for (final XmlParserType parserType : XmlParserType.values()) {
            final AbacusXmlParserImpl parser = new AbacusXmlParserImpl(parserType);

            final Map<Integer, List<Map<String, Integer>>> typed = parser.deserialize(nested, stringIntegerElements,
                    Type.of("Map<Integer, List<Map<String, Integer>>>"));
            assertEquals(Map.of(7, List.of(Map.of("x", 1))), typed, parserType.name());

            assertEquals(Map.of("a", "1"), parser.deserialize(flat, stringIntegerElements, Map.class), parserType.name());
            assertEquals(Map.of("a", "1"), parser.deserialize(flat, integerIntegerElements, Map.class), parserType.name());
            // unchanged: without a configured element type the declared key/value types apply
            assertEquals(Map.of("a", 1), parser.deserialize(flat, Type.of("Map<String, Integer>")), parserType.name());
        }
    }

    // G144: an entry whose key is ignored (setIgnoredPropNames(Map.class, ..)) must still hold exactly one <value>: StAX and SAX
    // skipped the rest of it unchecked, where DOM reports it as malformed. Null values read the same on every backend.
    @Test
    public void testDeserialize_ignoredKeyEntriesValidatedAndNullValuesOnEveryBackend() {
        final String holderStart = "<g141Holder type=\"" + G141Holder.class.getCanonicalName() + "\"><values><map>";
        final String holderEnd = "</map></values></g141Holder>";
        final XmlDeserConfig ignoring = XmlDeserConfig.create().setIgnoredPropNames(Map.class, Set.of("ig"));
        final AbacusXmlParserImpl stax = new AbacusXmlParserImpl(XmlParserType.StAX, null, null, approvedTypes());
        final String nullValue = holderStart + "<entry><key>k</key><value isNull=\"true\"/></entry>" + holderEnd;

        for (final XmlParserType parserType : ALL_TYPES) {
            final AbacusXmlParserImpl parser = new AbacusXmlParserImpl(parserType, null, null, approvedTypes());

            for (final String entry : new String[] { "<entry><key>ig</key></entry>", "<entry><key>ig</key><value>1</value><value>2</value></entry>",
                    "<entry><key>ig</key><key>b</key><value>2</value></entry>", "<entry><key>ig</key><foo>1</foo></entry>" }) {
                assertThrows(ParsingException.class, () -> parser.deserialize(holderStart + entry + holderEnd, ignoring, G141Holder.class),
                        parserType.name() + ": " + entry);
            }

            final G141Holder ok = parser.deserialize(
                    holderStart + "<entry><key>ig</key><value><list><e>1</e></list></value></entry><entry><key>k</key><value>v</value></entry>" + holderEnd,
                    ignoring, G141Holder.class);
            assertEquals(Map.of("k", "v"), ok.getValues(), parserType.name());

            assertEquals(g144Outcome(() -> stax.deserialize(nullValue, G141Holder.class)), g144Outcome(() -> parser.deserialize(nullValue, G141Holder.class)),
                    parserType.name());
            assertTrue(parser.deserialize(nullValue, G141Holder.class).getValues().containsKey("k"), parserType.name());
        }
    }

    // G006-06: an ignored property was still bound when its element/name attribute used a case or snake-case variant that
    // BeanInfo.getPropInfo resolves to the same property (<firstname>, <first_name>).
    @Test
    public void testDeserialize_ignoredPropertyIsNotBoundThroughNameVariant() {
        final XmlDeserConfig config = new XmlDeserConfig();
        config.setIgnoredPropNames(G006NameBean.class, Set.of("firstName"));

        for (final XmlParserType parserType : ALL_TYPES) {
            final XmlParser parser = parserOf(parserType);

            for (final String name : new String[] { "firstName", "firstname", "first_name", "FIRST_NAME" }) {
                final String label = parserType + " <" + name + ">";
                final G006NameBean bean = parser.deserialize("<g006NameBean><" + name + ">x</" + name + "><lastName>y</lastName></g006NameBean>", config,
                        G006NameBean.class);
                assertNull(bean.getFirstName(), label);
                assertEquals("y", bean.getLastName(), label);
            }

            final G006NameBean nested = parser.deserialize("<g006NameBean><first_name><list><e>1</e></list></first_name><lastName>y</lastName></g006NameBean>",
                    config, G006NameBean.class);
            assertNull(nested.getFirstName(), parserType + " nested");
            assertEquals("y", nested.getLastName(), parserType + " nested");

            final G006NameBean byAttribute = parser.deserialize(
                    "<bean name=\"g006NameBean\"><property name=\"first_name\">x</property><property name=\"lastName\">y</property></bean>", config,
                    G006NameBean.class);
            assertNull(byAttribute.getFirstName(), parserType + " name attribute");
            assertEquals("y", byAttribute.getLastName(), parserType + " name attribute");
        }
    }

    // G145: every backend gives the StAX result for an item whose declared type is an interface - an array component type or the
    // item's own <e type=..> - and for a value wrapper's type attribute under a root map without one (DOM took a misfitting type
    // attribute; SAX ignored the <value type> because the root map had fixed the type-info decision for the whole document).
    @Test
    public void testDeserialize_interfaceItemTypesAndWrapperTypesParity() {
        final String misfitCircle = "<g142Circle type=\"" + G142Other.class.getCanonicalName() + "\"><radius>1</radius></g142Circle>";
        final String shape = G142Shape.class.getCanonicalName();
        final AbacusXmlParserImpl stax = new AbacusXmlParserImpl(XmlParserType.StAX, null, null, g144Approved());
        final Object[][] cases = { { "<array><e>" + misfitCircle + "</e></array>", G142Shape[].class },
                { "<list><e type=\"" + shape + "\">" + misfitCircle + "</e></list>", List.class },
                { "<array><e type=\"" + shape + "\">" + misfitCircle + "</e></array>", Object[].class },
                { "<map><entry><key>k</key><value type=\"" + shape + "\">" + misfitCircle + "</value></entry></map>", Map.class } };

        for (final XmlParserType parserType : ALL_TYPES) {
            final AbacusXmlParserImpl parser = new AbacusXmlParserImpl(parserType, null, null, g144Approved());

            for (final Object[] c : cases) {
                final String xml = (String) c[0];
                final Class<?> target = (Class<?>) c[1];
                assertEquals(g144Outcome(() -> show(stax.deserialize(xml, target))), g144Outcome(() -> show(parser.deserialize(xml, target))),
                        parserType.name() + ": " + xml);
            }
        }
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
                () -> new AbacusXmlParserImpl(XmlParserType.StAX, null, null, approvedTypes()).deserialize(failingReader, new XmlDeserConfig(), Map.class));
        assertTrue(ex.getCause() instanceof IOException);
    }

    // G143: SAX rejects an <entry> that does not hold exactly one <key> and one <value> - a bean directly inside it was built and
    // dropped (the entry silently lost), and an empty entry was ignored - as StAX and DOM do.
    @Test
    public void testDeserialize_malformedMapEntryRejectedOnEveryBackend() {
        final String bean = "<accountContact type=\"" + testfixtures.entity.AccountContact.class.getName() + "\"><mobile>m</mobile></accountContact>";
        final String holderStart = "<g141Holder type=\"" + G141Holder.class.getCanonicalName() + "\"><values><map>";
        final String holderEnd = "</map></values></g141Holder>";
        final Set<Class<?>> approved = new java.util.HashSet<>(approvedTypes());
        approved.add(testfixtures.entity.AccountContact.class);

        for (final XmlParserType parserType : ALL_TYPES) {
            final AbacusXmlParserImpl parser = new AbacusXmlParserImpl(parserType, null, null, approved);

            for (final String entry : new String[] { "<entry>" + bean + "</entry>", "<entry/>", "<entry><key>k</key>" + bean + "</entry>",
                    "<entry><key>a</key><value>1</value><key>b</key><value>2</value></entry>" }) {
                assertThrows(ParsingException.class, () -> parser.deserialize(holderStart + entry + holderEnd, G141Holder.class),
                        parserType.name() + ": " + entry);
            }

            final G141Holder ok = parser.deserialize(holderStart + "<entry><key>k</key><value>v</value></entry>" + holderEnd, G141Holder.class);
            assertEquals(Map.of("k", "v"), ok.getValues(), parserType.name());
        }
    }

    // G006-01 neighbours: a primitive type name fits its wrapper and is kept; one that does not fit the declared type is dropped on
    // every backend (StAX took <value type="long"> as a Long in a Map<String, Integer> and <key type="int"> as an Integer key).
    @Test
    public void testDeserialize_mapEntryPrimitiveTypeAttributeFitsOnlyItsWrapper() {
        final String longValue = "<map><entry><key>a</key><value type=\"long\">5</value></entry></map>";
        final String intKey = "<map><entry><key type=\"int\">7</key><value>v</value></entry></map>";

        for (final XmlParserType parserType : ALL_TYPES) {
            final AbacusXmlParserImpl parser = new AbacusXmlParserImpl(parserType);

            assertEquals(Map.of("a", 5L), parser.deserialize(longValue, Type.<Map<String, Long>> of("Map<String, Long>")), parserType.name());
            assertEquals(Map.of("a", 5), parser.deserialize(longValue, Type.<Map<String, Integer>> of("Map<String, Integer>")), parserType.name());
            assertEquals(Map.of(7, "v"), parser.deserialize(intKey, Type.<Map<Integer, String>> of("Map<Integer, String>")), parserType.name());
            assertEquals(Map.of(7L, "v"), parser.deserialize(intKey, Type.<Map<Long, String>> of("Map<Long, String>")), parserType.name());
        }
    }

    // G006-01: a map key/value type attribute that does not fit the declared key/value type is dropped on every backend, as the DOM
    // and SAX readers already drop it; StAX took it as is and returned a String key and a Long value in a Map<Integer, Integer>.
    @Test
    public void testDeserialize_mapEntryTypeAttributeNotFittingDeclaredTypeIsDropped() {
        final String misfit = "<map><entry><key type=\"String\">1</key><value type=\"Long\">5</value></entry></map>";
        final String fitting = "<map><entry><key type=\"String\">a</key><value type=\"Long\">5</value></entry></map>";

        for (final XmlParserType parserType : ALL_TYPES) {
            final AbacusXmlParserImpl parser = new AbacusXmlParserImpl(parserType, null, null, approvedTypes());

            assertEquals(Map.of(1, 5), parser.deserialize(misfit, Type.<Map<Integer, Integer>> of("Map<Integer, Integer>")), parserType.name());

            // neighbour: a runtime type that fits the declared type is still used
            assertEquals(Map.of("a", 5L), parser.deserialize(fitting, Type.<Map<String, Number>> of("Map<String, Number>")), parserType.name());
        }
    }

    // G145: an item wrapper <e> belongs directly in an array or collection; SAX added a nested one to the enclosing collection and
    // failed with NullPointerException. A pooled SAX handler is reused correctly after a failure inside an ignored entry.
    @Test
    public void testDeserialize_misplacedItemWrapperAndHandlerReuse() {
        for (final XmlParserType parserType : ALL_TYPES) {
            final AbacusXmlParserImpl parser = new AbacusXmlParserImpl(parserType, null, null, approvedTypes());

            assertThrows(ParsingException.class, () -> parser.deserialize("<list><e><e>x</e></e></list>", List.class), parserType.name());
            assertThrows(ParsingException.class, () -> parser.deserialize("<array><e><e>x</e></e></array>", String[].class), parserType.name());
        }

        final AbacusXmlParserImpl sax = new AbacusXmlParserImpl(XmlParserType.SAX, null, null, approvedTypes());
        final XmlDeserConfig ignoring = XmlDeserConfig.create().setIgnoredPropNames(Map.class, Set.of("ig"));
        final String holderStart = "<g141Holder type=\"" + G141Holder.class.getCanonicalName() + "\"><values><map>";
        final String holderEnd = "</map></values></g141Holder>";

        assertThrows(ParsingException.class, () -> sax.deserialize(holderStart + "<entry><key>ig</key></entry>" + holderEnd, ignoring, G141Holder.class));
        assertEquals(Map.of("k", "v"),
                sax.deserialize(holderStart + "<entry><key>k</key><value>v</value></entry>" + holderEnd, ignoring, G141Holder.class).getValues());
    }

    // G144: <entry> belongs directly in a map and <key>/<value> directly in an <entry>; SAX used the enclosing entry's state for
    // such elements nested in an item or value and failed with NullPointerException. Every backend rejects them.
    @Test
    public void testDeserialize_misplacedMapEntryPartsRejectedOnEveryBackend() {
        final String holderStart = "<g141Holder type=\"" + G141Holder.class.getCanonicalName() + "\"><values><map><entry><key>k</key><value>";
        final String holderEnd = "</value></entry></map></values></g141Holder>";

        for (final XmlParserType parserType : ALL_TYPES) {
            final AbacusXmlParserImpl parser = new AbacusXmlParserImpl(parserType, null, null, approvedTypes());

            for (final String nested : new String[] { "<list><e><value>z</value></e></list>",
                    "<list><e><entry><key>a</key><value>b</value></entry></e></list>" }) {
                assertThrows(ParsingException.class, () -> parser.deserialize(holderStart + nested + holderEnd, G141Holder.class),
                        parserType.name() + ": " + nested);
            }
        }
    }

    // G115-03: an Optional map value nested two levels deep (Optional<Map<String, Optional<Bean>>>) is re-wrapped at both levels on every
    // backend, and a malformed wrapper (two child elements) fails without leaving per-frame state behind: the next document read by the
    // same parser (the SAX handler is pooled) is read correctly.
    @Test
    public void testDeserialize_nestedOptionalMapValueAndRecoveryAfterMalformedWrapper() {
        final Map<String, java.util.Optional<G006Alpha>> inner = new LinkedHashMap<>();
        inner.put("i", java.util.Optional.of(g115Alpha("x")));
        inner.put("k", java.util.Optional.of(g115Alpha("y")));
        final G119DeepOptionalHolder bean = new G119DeepOptionalHolder();
        bean.setDeep(new LinkedHashMap<>(Map.of("d", java.util.Optional.of(inner))));

        for (final XmlParserType parserType : ALL_TYPES) {
            final AbacusXmlParserImpl parser = new AbacusXmlParserImpl(parserType, null, null, approvedTypes());

            for (final XmlSerConfig config : new XmlSerConfig[] { XmlSerConfig.create(), XmlSerConfig.create().setPrettyFormat(true) }) {
                final String xml = parser.serialize(bean, config);
                final int valueEnd = xml.indexOf("</value>");
                final String malformed = xml.substring(0, valueEnd) + "<g006Alpha></g006Alpha>" + xml.substring(valueEnd);

                assertThrows(ParsingException.class, () -> parser.deserialize(malformed, G119DeepOptionalHolder.class), parserType + " " + malformed);
                assertEquals(bean, parser.deserialize(xml, G119DeepOptionalHolder.class), parserType + " " + xml);
            }
        }
    }

    // G143: the root check also applies through the nodeTypes overloads (the root element's mapped type).
    @Test
    public void testDeserialize_nodeTypesRootOfAnotherKindRejected() {
        for (final XmlParserType parserType : ALL_TYPES) {
            final AbacusXmlParserImpl parser = new AbacusXmlParserImpl(parserType, null, null, approvedTypes());
            final String listXml = parser.serialize(List.of("a"));

            assertThrows(ParsingException.class, () -> parser.deserialize(new StringReader(listXml), null, Map.of("list", Type.of(String[].class))),
                    parserType.name());
            assertEquals(List.of("a"), parser.deserialize(new StringReader(listXml), null, Map.<String, Type<?>> of("list", Type.of(List.class))),
                    parserType.name());
        }
    }

    // G134-01/02: Optional/Nullable items, keys and explicitly parameterized values: a List<Optional<Object>> root lost its wrappers (DOM,
    // SAX: root erasure) and its items' types (all readers: <e type="Integer">5</e> became Optional["5"]); an Optional key ignored its type
    // attribute; a declared Optional<List<Map<String, Long>>> value lost its wrapper under an interface root. Nothing changes without type
    // information (the values are Strings there, as for an unwrapped Object value).
    @Test
    public void testDeserialize_optionalItemsKeysAndParameterizedValuesOnAllBackends() {
        final XmlSerConfig typeInfo = XmlSerConfig.create().setWriteTypeInfo(true);
        final List<Object> items = new java.util.ArrayList<>(
                List.of(java.util.Optional.of(5), java.util.Optional.of(g134Map("x", 1)), java.util.Optional.of("s")));
        final Map<Object, Object> keys = new LinkedHashMap<>();
        keys.put(java.util.Optional.of(5), "v");
        final Map<String, Object> parameterized = g134Map("k", java.util.Optional.of(new java.util.ArrayList<>(List.of(g134Map("x", 1), g134Map("y", 2)))));

        for (final XmlParserType parserType : ALL_TYPES) {
            final XmlParser parser = parserOf(parserType);

            final String itemsXml = parser.serialize(items, typeInfo);
            assertEquals(items, parser.deserialize(itemsXml, Type.of("List<java.util.Optional<Object>>")), parserType + " " + itemsXml);
            assertEquals(List.of(java.util.Optional.of("5"), java.util.Optional.of(Map.of("x", "1")), java.util.Optional.of("s")),
                    parser.deserialize(parser.serialize(items), Type.of("List<java.util.Optional<Object>>")), parserType.name());

            final String keysXml = parser.serialize(keys, typeInfo);
            assertEquals(Map.of(java.util.Optional.of(5), "v"), parser.deserialize(keysXml, Type.of("LinkedHashMap<java.util.Optional<Object>, String>")),
                    parserType + " " + keysXml);

            for (final XmlSerConfig config : new XmlSerConfig[] { XmlSerConfig.create(), typeInfo }) {
                final String xml = parser.serialize(parameterized, config);

                for (final String wrapper : G134_WRAPPERS) {
                    for (final String rootType : new String[] { "Map", "LinkedHashMap" }) {
                        final Type<Map<String, Object>> type = Type.of(rootType + "<String, " + wrapper + "<List<Map<String, Long>>>>");

                        assertEquals(g134Wrap(wrapper, List.of(Map.of("x", 1L), Map.of("y", 2L))), parser.<Map<String, Object>> deserialize(xml, type).get("k"),
                                parserType + " " + type.name() + " " + xml);
                    }
                }
            }
        }
    }

    // G115-03: writeMap/writeCollection write a present Optional/Nullable key, value or item as the value it holds, so a bean, map,
    // list or array arrives as a nested element. All three readers read it with the wrapper type itself: the slot got a bare bean or
    // an ArrayList instead of Optional[...] (Map<String, Optional<List<String>>> gave [a, b]). Same defect as G010-01 in XmlParserImpl.
    @Test
    public void testDeserialize_optionalMapKeyValueAndItemHoldingStructuredValue() {
        final G115OptionalValueHolder bean = new G115OptionalValueHolder();
        bean.setBeans(new LinkedHashMap<>(Map.of("b", java.util.Optional.of(g115Alpha("x")))));
        bean.setLists(new LinkedHashMap<>(Map.of("l", u.Optional.of(new java.util.ArrayList<>(List.of(g115Alpha("y"), g115Alpha("z")))))));
        bean.setMaps(new LinkedHashMap<>(Map.of("m", u.Nullable.of(new LinkedHashMap<>(Map.of("k", g115Alpha("w")))))));
        bean.setStrings(new LinkedHashMap<>(Map.of("s", java.util.Optional.of(new java.util.ArrayList<>(List.of("a", "b"))))));
        bean.setKeys(new LinkedHashMap<>(Map.of(java.util.Optional.of(g115Alpha("key")), "v")));

        final List<java.util.Optional<G006Alpha>> rootList = new java.util.ArrayList<>(List.of(java.util.Optional.of(g115Alpha("item"))));
        final Type<List<java.util.Optional<G006Alpha>>> rootListType = Type.of(new TypeReference<List<java.util.Optional<G006Alpha>>>() {
        });

        final G115JdkOptionalValueHolder typed = new G115JdkOptionalValueHolder();
        typed.setBeans(bean.getBeans());
        typed.setStrings(bean.getStrings());

        for (final XmlParserType parserType : ALL_TYPES) {
            final AbacusXmlParserImpl parser = new AbacusXmlParserImpl(parserType);

            for (final XmlSerConfig config : new XmlSerConfig[] { XmlSerConfig.create(), XmlSerConfig.create().setPrettyFormat(true) }) {
                final String xml = parser.serialize(bean, config);
                assertEquals(bean, parser.deserialize(xml, G115OptionalValueHolder.class), parserType + " " + xml);

                final String listXml = parser.serialize(rootList, config);
                assertEquals(rootList, parser.deserialize(listXml, rootListType), parserType + " " + listXml);
            }

            // with type information too (the value wrapper then names the unwrapped value's class)
            final String typedXml = parser.serialize(typed, XmlSerConfig.create().setWriteTypeInfo(true));
            assertEquals(typed, parser.deserialize(typedXml, G115JdkOptionalValueHolder.class), parserType + " " + typedXml);
        }
    }

    // G134 neighbours (unchanged): without type information every wrapper keeps its value's shape (Strings, as for an unwrapped Object
    // value); an empty wrapper (the isNull marker) reads as null (the open G010-03 decision); a configured value type takes precedence over
    // the value's own type attribute, which is still approved; unapproved type attributes are rejected inside a wrapper.
    @Test
    public void testDeserialize_optionalWrapperNeighboursUnchangedOnAllBackends() {
        final List<Object> untyped = List.of(Map.of("x", "1"), List.of(Map.of("x", "1")), List.of(Map.of("x", "1"), Map.of("y", "2")),
                Map.of("x", "1", "y", "2"), "5", "s", List.of("1", "2"));
        final XmlDeserConfig configuredWrapper = XmlDeserConfig.create().setValueType("k", Type.of("java.util.Optional<String>"));

        for (final XmlParserType parserType : ALL_TYPES) {
            final XmlParser parser = parserOf(parserType);

            for (final String wrapper : G134_WRAPPERS) {
                final Type<Map<String, Object>> type = Type.of("Map<String, " + wrapper + "<Object>>");
                final List<Object> values = g134Values();

                for (int i = 0; i < values.size(); i++) {
                    final String xml = parser.serialize(g134Map("k", g134Wrap(wrapper, values.get(i))));
                    assertEquals(g134Wrap(wrapper, untyped.get(i)), parser.<Map<String, Object>> deserialize(xml, type).get("k"), parserType + " " + xml);
                }
            }

            for (final XmlSerConfig config : new XmlSerConfig[] { XmlSerConfig.create(), XmlSerConfig.create().setWriteTypeInfo(true) }) {
                final Map<String, Object> root = new LinkedHashMap<>();
                root.put("k", java.util.Optional.empty());
                final String xml = parser.serialize(root, config);
                final Map<String, Object> back = parser.deserialize(xml, Type.of("LinkedHashMap<String, java.util.Optional<Object>>"));

                assertTrue(back.containsKey("k"), xml);
                assertNull(back.get("k"), parserType + " " + xml);
            }

            final String typed = "<map><entry><key>k</key><value type=\"Integer\">5</value></entry></map>";
            assertEquals(java.util.Optional.of("5"),
                    parser.<Map<String, Object>> deserialize(typed, configuredWrapper, Type.of("Map<String, java.util.Optional<Object>>")).get("k"),
                    parserType.name());

            for (final String rejected : new String[] { "<map><entry><key>k</key><value type=\"not.approved.Type\">5</value></entry></map>",
                    "<map><entry><key>k</key><value type=\"java.lang.Runtime\"><map /></value></entry></map>" }) {
                assertThrows(ParsingException.class, () -> parser.deserialize(rejected, Type.of("Map<String, java.util.Optional<Object>>")), rejected);
                assertThrows(ParsingException.class,
                        () -> parser.deserialize(rejected, configuredWrapper, Type.of("LinkedHashMap<String, java.util.Optional<Object>>")), rejected);
            }

            assertThrows(ParsingException.class,
                    () -> parser.deserialize("<list><e type=\"not.approved.Type\">5</e></list>", Type.of("List<java.util.Optional<Object>>")));
        }
    }

    @Test
    public void testDeserializeIgnoredJsonKeyDoesNotBecomeCompactMapContent() {
        final String ignoredKey = "{\"x\":1}";
        final XmlDeserConfig config = XmlDeserConfig.create().setIgnoredPropNames(Map.class, Set.of(ignoredKey));
        final String entry = "<entry><key>" + ignoredKey + "</key><value>discard</value></entry>";

        for (final XmlParserType type : ALL_TYPES) {
            final XmlParser parser = parserOf(type);
            for (final String separator : new String[] { "", "\n  " }) {
                final String ignoredMap = "<map>" + separator + entry + separator + "</map>";
                assertEquals(Map.of(), parser.deserialize(ignoredMap, config, Map.class), type.toString());
                assertEquals(Map.of("outer", Map.of()),
                        parser.deserialize("<map><entry><key>outer</key><value>" + ignoredMap + "</value></entry></map>", config, Map.class), type.toString());
            }
            assertEquals(Map.of("x", 1), parser.deserialize("<map>" + ignoredKey + "</map>", config, Map.class), type.toString());
        }
    }

    // G149: a pretty-printed entry whose key is ignored (whitespace between </key>, <value> and </entry>) is skipped on every
    // backend, and is still validated: exactly one <value>.
    @Test
    public void testDeserialize_prettyPrintedIgnoredKeyEntry() {
        final String holderStart = "<g141Holder type=\"" + G141Holder.class.getCanonicalName() + "\">\n  <values>\n    <map>\n";
        final String holderEnd = "    </map>\n  </values>\n</g141Holder>\n";
        final XmlDeserConfig ignoring = XmlDeserConfig.create().setIgnoredPropNames(Map.class, Set.of("ig"));
        final String ignored = "      <entry>\n        <key>ig</key>\n        <value>\n          <list>\n            <e>1</e>\n          </list>\n"
                + "        </value>\n      </entry>\n";
        final String kept = "      <entry>\n        <key>k</key>\n        <value>v</value>\n      </entry>\n";

        for (final XmlParserType parserType : ALL_TYPES) {
            final AbacusXmlParserImpl parser = new AbacusXmlParserImpl(parserType, null, null, approvedTypes());

            assertEquals(Map.of("k", "v"), parser.deserialize(holderStart + ignored + kept + holderEnd, ignoring, G141Holder.class).getValues(),
                    parserType.name());
            assertEquals(Map.of("k", "v"), parser.deserialize(holderStart + kept + ignored + holderEnd, ignoring, G141Holder.class).getValues(),
                    parserType.name());

            for (final String bad : new String[] { "      <entry>\n        <key>ig</key>\n      </entry>\n",
                    "      <entry>\n        <key>ig</key>\n        <value>1</value>\n        <value>2</value>\n      </entry>\n" }) {
                assertThrows(ParsingException.class, () -> parser.deserialize(holderStart + bad + holderEnd, ignoring, G141Holder.class),
                        parserType.name() + ": " + bad);
            }
        }
    }

    // G121 for array targets: a bean root with a configured bean element type was returned as that bean for an Object[] or
    // G006Alpha[] target (DOM and SAX); every backend now rejects it, as StAX did.
    @Test
    public void testDeserialize_rootBeanForArrayTargetIgnoresConfiguredElementType() {
        final G006Alpha alpha = new G006Alpha();
        alpha.setAlphaName("a");
        final String xml = staxParser.serialize(alpha);
        final XmlDeserConfig config = XmlDeserConfig.create().setElementType(G006Alpha.class);

        for (final XmlParserType parserType : XmlParserType.values()) {
            final AbacusXmlParserImpl parser = new AbacusXmlParserImpl(parserType, null, null, approvedTypes());
            assertThrows(ParsingException.class, () -> parser.deserialize(xml, config, Object[].class), parserType.name());
            assertThrows(ParsingException.class, () -> parser.deserialize(xml, config, G006Alpha[].class), parserType.name());
        }
    }

    // G142: the root check also covers InputStream and Node sources (separate SAX/DOM entry points), and a bean root read with a
    // configured bean element type for an Object target is never that bean (a bean outside com.landawn.abacus, so that node-name
    // discovery cannot find it): it is read as a map of its properties on every backend (2026-10-06; it used to be rejected).
    @Test
    public void testDeserialize_rootChecksForStreamAndNodeSourcesAndObjectTarget() throws Exception {
        final G141Omega omega = new G141Omega();
        omega.setOmegaName("o");
        final testfixtures.entity.AccountContact contact = new testfixtures.entity.AccountContact();
        contact.setMobile("m");
        final Set<Class<?>> approved = new java.util.HashSet<>(approvedTypes());
        approved.add(testfixtures.entity.AccountContact.class);

        for (final XmlParserType parserType : ALL_TYPES) {
            final AbacusXmlParserImpl parser = new AbacusXmlParserImpl(parserType, null, null, approved);
            final byte[] arrayXml = parser.serialize(new G141Omega[] { omega }).getBytes(StandardCharsets.UTF_8);

            assertThrows(ParsingException.class, () -> parser.deserialize(new ByteArrayInputStream(arrayXml), null, G141Omega.class), parserType.name());
            assertArrayEquals(new G141Omega[] { omega }, parser.deserialize(new ByteArrayInputStream(arrayXml), null, G141Omega[].class), parserType.name());

            final org.w3c.dom.Element root = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder()
                    .parse(new ByteArrayInputStream(arrayXml))
                    .getDocumentElement();
            assertThrows(ParsingException.class, () -> parser.deserialize(root, null, G141Omega.class), parserType.name());

            final String contactXml = parser.serialize(contact);
            final XmlDeserConfig contactElements = XmlDeserConfig.create().setElementType(testfixtures.entity.AccountContact.class);
            final Object contactValue = parser.deserialize(contactXml, contactElements, Object.class);
            assertEquals("m", assertInstanceOf(Map.class, contactValue, parserType.name()).get("mobile"), parserType.name());
        }
    }

    // G141: the kind of the root element no longer overrides the requested type: an <array>, <list> or <map> root was returned as
    // an array, list or map for a bean (or String) target - deserialize(xml, G141Omega.class) returned a G141Omega[] and failed
    // in the caller with ClassCastException. Every backend now throws ParsingException; matching targets are unchanged.
    @Test
    public void testDeserialize_rootOfAnotherKindRejected() {
        final G141Omega omega = new G141Omega();
        omega.setOmegaName("o");
        final XmlDeserConfig omegaElements = XmlDeserConfig.create().setElementType(G141Omega.class);

        for (final XmlParserType parserType : ALL_TYPES) {
            final AbacusXmlParserImpl parser = new AbacusXmlParserImpl(parserType, null, null, approvedTypes());
            final String arrayXml = parser.serialize(new G141Omega[] { omega });
            final String listXml = parser.serialize(List.of(omega));
            final String mapXml = parser.serialize(Map.of("k", "v"));

            for (final XmlDeserConfig config : new XmlDeserConfig[] { XmlDeserConfig.create(), omegaElements }) {
                assertThrows(ParsingException.class, () -> parser.deserialize(arrayXml, config, G141Omega.class), parserType.name());
                assertThrows(ParsingException.class, () -> parser.deserialize(listXml, config, G141Omega.class), parserType.name());
                assertThrows(ParsingException.class, () -> parser.deserialize(mapXml, config, G141Omega.class), parserType.name());
                assertThrows(ParsingException.class, () -> parser.deserialize(mapXml, config, String.class), parserType.name());
                assertThrows(ParsingException.class, () -> parser.deserialize(new StringReader(arrayXml), config, G141Omega.class), parserType.name());
            }

            assertThrows(ParsingException.class, () -> parser.deserialize(listXml, omegaElements, G141Omega[].class), parserType.name());

            // unchanged: a root of the requested kind
            assertArrayEquals(new G141Omega[] { omega }, parser.deserialize(arrayXml, G141Omega[].class), parserType.name());
            assertEquals(List.of(omega), parser.deserialize(listXml, omegaElements, List.class), parserType.name());
            assertEquals(Map.of("k", "v"), parser.deserialize(mapXml, Map.class), parserType.name());
            assertEquals(Map.of("k", "v"), parser.deserialize(mapXml, Object.class), parserType.name());
            assertEquals(omega, parser.deserialize(parser.serialize(omega), G141Omega.class), parserType.name());
        }
    }

    // G134-01: a root whose type attribute names a subclass with another parameter layout (G134StringKeyMap<V> extends HashMap<String, V>,
    // written as type="...G134StringKeyMap<Object, Object>"): the declared Map<String, X> is the schema and the subclass is only
    // instantiated; no type argument is transplanted onto it.
    @Test
    public void testDeserialize_rootSubclassWithOtherParameterLayoutKeepsDeclaredSchemaOnAllBackends() {
        final XmlSerConfig typeInfo = XmlSerConfig.create().setWriteTypeInfo(true);
        final G134StringKeyMap<Object> lists = new G134StringKeyMap<>();
        lists.put("a", new java.util.ArrayList<>(List.of(1, 2)));
        final G134StringKeyMap<Object> optionals = new G134StringKeyMap<>();
        optionals.put("k", java.util.Optional.of(new java.util.ArrayList<>(List.of(g134Map("x", 1), g134Map("y", 2)))));

        for (final XmlParserType parserType : ALL_TYPES) {
            final XmlParser parser = parserOf(parserType);
            final String listsXml = parser.serialize(lists, typeInfo);
            final String optionalsXml = parser.serialize(optionals, typeInfo);

            final Map<String, Object> sets = parser.deserialize(listsXml, Type.of("Map<String, Set<Integer>>"));
            assertEquals(Map.of("a", Set.of(1, 2)), sets, parserType + " " + listsXml);
            assertTrue(sets instanceof G134StringKeyMap, parserType + " " + sets.getClass().getName());

            final Map<String, Object> wrapped = parser.deserialize(optionalsXml, Type.of("Map<String, java.util.Optional<Object>>"));
            assertEquals(Map.of("k", java.util.Optional.of(List.of(Map.of("x", 1), Map.of("y", 2)))), wrapped, parserType + " " + optionalsXml);
            assertTrue(wrapped instanceof G134StringKeyMap, parserType + " " + wrapped.getClass().getName());
        }
    }

    // G134-01: all three readers let the root element's type attribute - the RAW runtime class, type="LinkedHashMap<Object, Object>" -
    // replace the declared root type, so with type information a declared Map<String, Optional<Object>> lost every wrapper. The declared
    // type stays the schema; the attribute's class is only the instance to fill (still a LinkedHashMap). G134-02: a scalar held by the
    // wrapper is converted with its own type attribute (<value type="Integer">5</value>: Optional[5], was Optional["5"]), as an unwrapped
    // Object value is. The concrete root type (no G134-01 there) isolates G134-02.
    @Test
    public void testDeserialize_rootTypeAttributeKeepsDeclaredOptionalOnAllBackends() {
        final XmlSerConfig typeInfo = XmlSerConfig.create().setWriteTypeInfo(true);

        for (final XmlParserType parserType : ALL_TYPES) {
            final XmlParser parser = parserOf(parserType);

            for (final String wrapper : G134_WRAPPERS) {
                for (final Object value : g134Values()) {
                    final String xml = parser.serialize(g134Map("k", g134Wrap(wrapper, value)), typeInfo);

                    for (final String rootType : new String[] { "Map", "LinkedHashMap" }) {
                        final Type<Map<String, Object>> type = Type.of(rootType + "<String, " + wrapper + "<Object>>");
                        final Map<String, Object> back = parser.deserialize(xml, type);

                        assertEquals(g134Wrap(wrapper, value), back.get("k"), parserType + " " + type.name() + " " + xml);
                        assertTrue(back instanceof LinkedHashMap, back.getClass().getName());
                    }
                }
            }
        }
    }

    // G134-01: the same root erasure dropped every other declared type argument below the root on all three readers (Map<String, Set<Integer>>
    // read lists, HashMap<String, List<Long>> Integers on StAX, a configured value type was not applied on DOM because the keys became Object),
    // and a JSON text root collection was read with the attribute's element type (Strings on DOM and SAX). The attribute's class is still the
    // instance to fill (TreeMap, LinkedList); a raw declaration still takes the attribute's type (neighbour). (On DOM and SAX a map VALUE's own
    // type attribute, ArrayList, still replaces a declared List<Long> value type below the root - a separate, pre-existing item.)
    @Test
    public void testDeserialize_rootTypeAttributeKeepsDeclaredTypeArgumentsOnAllBackends() {
        final XmlSerConfig typeInfo = XmlSerConfig.create().setWriteTypeInfo(true);
        final Map<String, Object> lists = new java.util.TreeMap<>(
                g134Map("a", new java.util.ArrayList<>(List.of(1, 2)), "b", new java.util.ArrayList<>(List.of(3))));
        final XmlDeserConfig configured = XmlDeserConfig.create().setValueType("a", Type.of("List<Long>"));
        final String linkedListXml = "<list type=\"LinkedList&lt;Object&gt;\">[1, 2]</list>";

        for (final XmlParserType parserType : ALL_TYPES) {
            final XmlParser parser = parserOf(parserType);
            final String xml = parser.serialize(lists, typeInfo);

            assertEquals(g134Map("a", List.of(1, 2), "b", List.of(3)), parser.deserialize(xml, Type.of("Map<String, List<Integer>>")), parserType + " " + xml);

            final Map<String, Object> sets = parser.deserialize(xml, Type.of("Map<String, Set<Integer>>"));
            assertEquals(g134Map("a", Set.of(1, 2), "b", Set.of(3)), sets, parserType + " " + xml);
            assertTrue(sets instanceof java.util.TreeMap, parserType + " " + sets.getClass().getName());

            if (parserType == XmlParserType.StAX) {
                assertEquals(g134Map("a", List.of(1L, 2L), "b", List.of(3L)), parser.deserialize(xml, Type.of("HashMap<String, List<Long>>")), xml);
            }

            assertEquals(List.of(1L, 2L), parser.<Map<String, Object>> deserialize(xml, configured, Type.of("Map<String, Object>")).get("a"),
                    parserType + " " + xml);

            final List<Object> linked = parser.deserialize(linkedListXml, Type.of("List<Long>"));
            assertEquals(List.of(1L, 2L), linked, parserType.name());
            assertTrue(linked instanceof java.util.LinkedList, parserType + " " + linked.getClass().getName());

            // neighbour (unchanged): a raw declaration takes the attribute's type
            final Map<?, ?> raw = parser.deserialize(xml, Map.class);
            assertEquals(g134Map("a", List.of(1, 2), "b", List.of(3)), raw, parserType + " " + xml);
            assertTrue(raw instanceof java.util.TreeMap, parserType + " " + raw.getClass().getName());
        }
    }

    // G006-01: the same leak inside a list item: the bean in an Object property of a List<G006Carrier> item was read as a G006Carrier.
    @Test
    public void testDeserialize_saxBeanInObjectPropertyOfListItemReadsItsOwnBean() {
        final G006Carrier carrier = new G006Carrier();
        carrier.setCarrierName("c1");
        carrier.setPayload(g006Omega());
        final G006Box box = new G006Box();
        box.setCarriers(List.of(carrier));

        for (final XmlParserType parserType : XmlParserType.values()) {
            final AbacusXmlParserImpl parser = new AbacusXmlParserImpl(parserType);

            assertEquals(box, parser.deserialize(parser.serialize(box), G006Box.class), parserType.name());
        }
    }

    // G006-01: after a closed List<Set<G006Alpha>> / List<G006Alpha[]> property, SAX typed the list/array held by the following Object
    // property with that element type (anyList read as a Set of G006Alpha, anyArray as a G006Alpha[]).
    @Test
    public void testDeserialize_saxContainerInObjectPropertyAfterContainerListKeepsItsOwnType() {
        final G006Alpha alpha = new G006Alpha();
        alpha.setAlphaName("a1");
        final G115ContainerHolder holder = new G115ContainerHolder();
        holder.setSets(List.of(Set.of(alpha)));
        holder.setAnyList(new java.util.ArrayList<>(List.of("3", "3")));
        holder.setArrays(List.<G006Alpha[]> of(new G006Alpha[] { alpha }));
        holder.setAnyArray(new String[] { "p" });

        for (final XmlParserType parserType : XmlParserType.values()) {
            final AbacusXmlParserImpl parser = new AbacusXmlParserImpl(parserType);
            final String xml = parser.serialize(holder);
            final G115ContainerHolder result = parser.deserialize(xml, G115ContainerHolder.class);

            assertEquals(List.of(Set.of(alpha)), result.getSets(), parserType.name());
            assertEquals(List.of("3", "3"), result.getAnyList(), parserType.name());
            assertEquals(N.deepToString(holder.getArrays().toArray()), N.deepToString(result.getArrays().toArray()), parserType.name());
            assertEquals(String[].class, result.getAnyArray().getClass(), parserType.name());
            assertEquals("[p]", N.deepToString(result.getAnyArray()), parserType.name());
        }
    }

    // G006-01: a map nested in an Object value of a List<Map<Integer, Object>> item was typed as Map<Integer, Object> by SAX
    // and failed with NumberFormatException on its String key.
    @Test
    public void testDeserialize_saxMapNestedInListOfMapsKeepsItsOwnKeyType() {
        final Map<Integer, Object> item = new LinkedHashMap<>();
        item.put(1, new LinkedHashMap<>(Map.of("x", "abc")));
        final List<Map<Integer, Object>> list = List.of(item);

        for (final XmlParserType parserType : XmlParserType.values()) {
            final AbacusXmlParserImpl parser = new AbacusXmlParserImpl(parserType);

            assertEquals(list, parser.deserialize(parser.serialize(list), Type.of("List<Map<Integer, Object>>")), parserType.name());
        }
    }

    // G006-02: the SAX reader did not ignore a null map key under the name it is written as ("null").
    @Test
    public void testDeserialize_saxNullMapKeyIgnoredByNullName() {
        final Map<Object, Object> source = new LinkedHashMap<>();
        source.put(null, 1);
        source.put("a", 2);

        for (final XmlParserType parserType : new XmlParserType[] { XmlParserType.StAX, XmlParserType.DOM, XmlParserType.SAX }) {
            final AbacusXmlParserImpl parser = new AbacusXmlParserImpl(parserType);
            final String xml = parser.serialize(source);
            final XmlDeserConfig config = new XmlDeserConfig().setIgnoredPropNames(Map.class, Set.of("null"));
            final Map<String, Object> result = parser.deserialize(xml, config, Type.<Map<String, Object>> of("Map<String, Object>"));

            assertEquals(Map.of("a", "2"), result, parserType.toString());
        }
    }

    // SAX: an ignored null key also skips a structured value, and a null key is kept when "null" is not an ignored name.
    @Test
    public void testDeserialize_saxNullMapKeyIgnoredWithStructuredValueAndKeptOtherwise() {
        final Map<Object, Object> source = new LinkedHashMap<>();
        source.put("b", 0);
        source.put(null, Arrays.asList(1, 2));
        source.put("z", 9);

        for (final XmlParserType parserType : ALL_TYPES) {
            final AbacusXmlParserImpl parser = new AbacusXmlParserImpl(parserType);
            final String xml = parser.serialize(source);

            final Map<String, Object> ignored = parser.deserialize(xml, new XmlDeserConfig().setIgnoredPropNames(Map.class, Set.of("null")),
                    Type.<Map<String, Object>> of("Map<String, Object>"));
            assertEquals(Map.of("b", "0", "z", "9"), ignored, parserType.toString());

            final Map<String, Object> kept = parser.deserialize(xml, new XmlDeserConfig().setIgnoredPropNames(Map.class, Set.of("b")),
                    Type.<Map<String, Object>> of("Map<String, Object>"));
            assertEquals(2, kept.size(), parserType.toString());
            assertEquals(Arrays.asList("1", "2"), kept.get(null), parserType.toString());
        }
    }

    // G006-01: SAX kept the element type of a closed List<G006Alpha> property, so the bean held by the following Object
    // property was read as a G006Alpha (fields silently dropped, or "Unknown property" in strict mode).
    @Test
    public void testDeserialize_saxObjectPropertyAfterBeanListReadsItsOwnBean() {
        final G006Alpha alpha = new G006Alpha();
        alpha.setAlphaName("a1");
        final G006Holder holder = new G006Holder();
        holder.setAlphas(List.of(alpha));
        holder.setOther(g006Omega());

        for (final XmlParserType parserType : XmlParserType.values()) {
            final AbacusXmlParserImpl parser = new AbacusXmlParserImpl(parserType);
            final String xml = parser.serialize(holder);

            assertEquals(holder, parser.deserialize(xml, G006Holder.class), parserType.name());
            assertEquals(holder, parser.deserialize(xml, XmlDeserConfig.create().setIgnoreUnmatchedProperty(false), G006Holder.class), parserType.name());
        }
    }

    // G149: SAX checks a bean's type attribute in an Object slot once (it was resolved twice); the approval policy still applies.
    @Test
    public void testDeserialize_saxObjectSlotTypeAttributeStillApproved() {
        final String doc = "<list><e><g142Circle type=\"" + G142Circle.class.getCanonicalName() + "\"><radius>1</radius></g142Circle></e></list>";

        for (final XmlParserType parserType : ALL_TYPES) {
            final AbacusXmlParserImpl approving = new AbacusXmlParserImpl(parserType, null, null, approvedTypes());
            final List<?> result = approving.deserialize(doc, List.class);
            assertEquals(1, result.size(), parserType.name());
            final G142Circle circle = org.junit.jupiter.api.Assertions.assertInstanceOf(G142Circle.class, result.get(0), parserType.name());
            assertEquals(1, circle.getRadius(), parserType.name());

            final AbacusXmlParserImpl strict = new AbacusXmlParserImpl(parserType, null, null, Set.of());
            assertThrows(ParsingException.class, () -> strict.deserialize(doc, List.class), parserType.name());
        }
    }

    // G006-02: SAX classified a non-<e> child of <list>/<array> by its name, so it dropped, misread or crashed on items
    // that the StAX and DOM readers read as item wrappers.
    @Test
    public void testDeserialize_saxReadsAnyListOrArrayChildAsItemWrapper() {
        final Type<List<Integer>> intListType = Type.of(new TypeReference<List<Integer>>() {
        });
        final Type<List<List<Integer>>> intListListType = Type.of(new TypeReference<List<List<Integer>>>() {
        });
        final Type<List<Person>> personListType = Type.of(new TypeReference<List<Person>>() {
        });

        for (final XmlParserType parserType : ALL_TYPES) {
            final XmlParser parser = parserOf(parserType);

            assertEquals(Arrays.asList(1, 2), parser.deserialize("<list><item>1</item><e>2</e></list>", null, intListType), parserType.name());
            assertArrayEquals(new int[] { 1, 2 }, parser.deserialize("<array><item>1</item><e>2</e></array>", int[].class), parserType.name());
            assertEquals(Arrays.asList((Person) null), parser.deserialize("<list><person isNull=\"true\"/></list>", null, personListType), parserType.name());
            // SAX read the inner list's text into the OUTER list: [1] instead of [[1]].
            assertEquals(Arrays.asList(Arrays.asList(1)), parser.deserialize("<list><list>[1]</list></list>", null, intListListType), parserType.name());
            assertThrows(ParsingException.class, () -> parser.deserialize("<list><person><name>x</name></person></list>", null, personListType),
                    parserType.name());
            assertThrows(ParsingException.class, () -> parser.deserialize("<array><person><name>x</name></person></array>", Person[].class), parserType.name());
        }
    }

    // G145: sibling items of a root container each decide whether type attributes are used, as on StAX/DOM: SAX kept the first
    // item's decision for the document, so a later item's type attribute was ignored.
    @Test
    public void testDeserialize_siblingItemsDecideTypeInfoOnEveryBackend() {
        final testfixtures.entity.AccountContact contact = new testfixtures.entity.AccountContact();
        contact.setMobile("m");
        final G141Omega omega = new G141Omega();
        omega.setOmegaName("o");
        final String xml = "<list><e><g141Omega><omegaName>o</omegaName></g141Omega></e><e><accountContact type=\""
                + testfixtures.entity.AccountContact.class.getName() + "\"><mobile>m</mobile></accountContact></e></list>";

        // the first item has no type attribute: its class comes from a configured value type
        final XmlDeserConfig config = XmlDeserConfig.create().setValueType("g141Omega", G141Omega.class);

        for (final XmlParserType parserType : ALL_TYPES) {
            final AbacusXmlParserImpl parser = new AbacusXmlParserImpl(parserType, null, null, g144Approved());
            assertEquals(List.of(omega, contact), parser.deserialize(xml, config, List.class), parserType.name());
        }
    }

    // G146: items of a root container written in the name-attribute format (<bean name=..><property name=..>) next to items in the
    // tag format each decide their own format, as on StAX/DOM: SAX kept the first item's decision ("Missing 'name' attribute").
    @Test
    public void testDeserialize_siblingItemsInNameAndTagFormat() {
        final String circle = G142Circle.class.getCanonicalName();
        final String named = "<bean name=\"g142Circle\" type=\"" + circle + "\"><property name=\"radius\">1</property></bean>";
        final String tagged = "<g142Circle type=\"" + circle + "\"><radius>2</radius></g142Circle>";
        final AbacusXmlParserImpl stax = new AbacusXmlParserImpl(XmlParserType.StAX, null, null, approvedTypes());
        final Object[][] cases = { { "<list><e>" + named + "</e><e>" + tagged + "</e></list>", List.class },
                { "<list><e><list><e>" + named + "</e></list></e><e>" + tagged + "</e></list>", List.class },
                { "<map><entry><key>a</key><value>" + named + "</value></entry><entry><key>b</key><value>" + tagged + "</value></entry></map>", Map.class } };

        for (final XmlParserType parserType : ALL_TYPES) {
            final AbacusXmlParserImpl parser = new AbacusXmlParserImpl(parserType, null, null, approvedTypes());

            for (final Object[] c : cases) {
                final String xml = (String) c[0];
                final Class<?> target = (Class<?>) c[1];
                final String expected = g144Outcome(() -> stax.deserialize(xml, target));
                assertTrue(expected.contains("radius=1") && expected.contains("radius=2"), expected);
                assertEquals(expected, g144Outcome(() -> parser.deserialize(xml, target)), parserType.name() + ": " + xml);
            }
        }
    }

    // G142: DOM and SAX use a bean's type attribute only in an Object (or untyped) slot, as StAX does. For a typed slot whose
    // attribute names a class that does not fit (G142Other in a G142Shape property, a bean inside a Map property) they took the
    // attribute: IllegalArgumentException "Conversion returned java.lang.String" escaped deserialize, or the bean silently became
    // a map. Every backend now gives the StAX result; a fitting attribute still works everywhere.
    @Test
    public void testDeserialize_typeAttributeThatDoesNotFitTheSlot() {
        final String holderStart = "<g142ShapeHolder type=\"" + G142ShapeHolder.class.getCanonicalName() + "\">";
        final String fitting = holderStart + "<shape type=\"" + G142Shape.class.getCanonicalName() + "\"><g142Circle type=\""
                + G142Circle.class.getCanonicalName() + "\"><radius>1</radius></g142Circle></shape></g142ShapeHolder>";
        final String misfit = fitting.replace(G142Circle.class.getCanonicalName(), G142Other.class.getCanonicalName());
        final String inMap = holderStart + "<m><g142Circle type=\"" + G142Other.class.getCanonicalName()
                + "\"><radius>1</radius></g142Circle></m></g142ShapeHolder>";
        final G142Circle circle = new G142Circle();
        circle.setRadius(1);
        final AbacusXmlParserImpl stax = new AbacusXmlParserImpl(XmlParserType.StAX, null, null, approvedTypes());

        for (final XmlParserType parserType : ALL_TYPES) {
            final AbacusXmlParserImpl parser = new AbacusXmlParserImpl(parserType, null, null, approvedTypes());

            assertEquals(circle, parser.deserialize(fitting, G142ShapeHolder.class).getShape(), parserType.name());

            for (final String xml : new String[] { misfit, inMap }) {
                final String outcome = g142Outcome(parser, xml);
                assertEquals(g142Outcome(stax, xml), outcome, parserType.name() + ": " + xml);
                assertTrue(!"IllegalArgumentException".equals(outcome), parserType.name());
            }
        }
    }

    // G119-01: with writeTypeInfo a java.time.Duration property or map value is written with its handler's name (type="JdkDuration"),
    // which every backend rejected - the same defect as the JdkOptional names of G115-02.
    @Test
    public void testDeserialize_writeTypeInfoJdkDurationTypeAttributeAcceptedOnAllBackends() {
        final G119DurationHolder bean = new G119DurationHolder();
        bean.setDuration(java.time.Duration.ofSeconds(90));
        bean.setDurations(new LinkedHashMap<>(Map.of("k", java.time.Duration.ofMillis(1500))));

        for (final XmlParserType parserType : ALL_TYPES) {
            for (final AbacusXmlParserImpl parser : new AbacusXmlParserImpl[] { new AbacusXmlParserImpl(parserType),
                    new AbacusXmlParserImpl(parserType, null, null, approvedTypes()) }) {
                final String xml = parser.serialize(bean, XmlSerConfig.create().setWriteTypeInfo(true));
                assertTrue(xml.contains("type=\"JdkDuration\""), xml);
                assertEquals(bean, parser.deserialize(xml, G119DurationHolder.class), parserType + " " + xml);

                // neighbours: the output without type information reads as before; an unapproved name there is still rejected
                assertEquals(bean, parser.deserialize(parser.serialize(bean), G119DurationHolder.class), parserType.name());
                final String unapproved = xml.replace("type=\"JdkDuration\"", "type=\"not.approved.Type\"");
                assertThrows(ParsingException.class, () -> parser.deserialize(unapproved, G119DurationHolder.class), parserType.name());
            }
        }
    }

    // G115-02: with writeTypeInfo the property type attribute names the java.util optional handlers (type="Map<String,
    // JdkOptional<String>>"), which every backend rejected; those names of already approved classes are accepted now.
    @Test
    public void testDeserialize_writeTypeInfoJdkOptionalTypeAttributeAcceptedOnAllBackends() {
        final G115JdkOptionalScalarHolder bean = new G115JdkOptionalScalarHolder();
        bean.setOptionals(new LinkedHashMap<>(Map.of("x", java.util.Optional.of("a,b"))));
        bean.setOptionalLongs(new LinkedHashMap<>(Map.of("l", java.util.OptionalLong.of(7L))));

        for (final XmlParserType parserType : ALL_TYPES) {
            for (final AbacusXmlParserImpl parser : new AbacusXmlParserImpl[] { new AbacusXmlParserImpl(parserType),
                    new AbacusXmlParserImpl(parserType, null, null, approvedTypes()) }) {
                final String xml = parser.serialize(bean, XmlSerConfig.create().setWriteTypeInfo(true));
                assertTrue(xml.contains("JdkOptional&lt;String&gt;"), xml);
                assertEquals(bean, parser.deserialize(xml, G115JdkOptionalScalarHolder.class), parserType + " " + xml);

                // neighbour: an unapproved name in the same position is still rejected
                final String unapproved = xml.replace("JdkOptional&lt;String&gt;", "not.approved.Type");
                assertThrows(ParsingException.class, () -> parser.deserialize(unapproved, G115JdkOptionalScalarHolder.class), parserType.name());
            }
        }
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
    public void testDeserializeNodeTypesNullFileSourceNamesSourceParameter() {
        final Map<String, Type<?>> nodeTypes = Map.of("value", Type.of(String.class));

        for (final XmlParserType parserType : XmlParserType.values()) {
            final AbacusXmlParserImpl parser = new AbacusXmlParserImpl(parserType);
            final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> parser.deserialize((File) null, null, nodeTypes));
            assertEquals("'source' cannot be null", e.getMessage());
        }
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
    public void testDomMapSkipsNonElementsAndRequiresExactlyKeyAndValue() {
        final Map<?, ?> result = domParser
                .deserialize("<map><!-- before --><?entry value?><entry><!-- key --><key>a</key><?value next?><value>1</value></entry></map>", Map.class);
        assertEquals("1", result.get("a"));
        assertThrows(ParsingException.class, () -> domParser.deserialize("<map><entry><key>a</key></entry></map>", Map.class));
        assertThrows(ParsingException.class, () -> domParser.deserialize("<map><entry><key>a</key><value>1</value><value>2</value></entry></map>", Map.class));
    }

    @Test
    public void testDuplicateItemAndMapValueWrappersAreRejected() {
        // Empty structured values count too, including at an outer depth after nested values finish.
        final Object[][] malformed = { { "<list><e><list/><list/></e></list>", Type.of("List<List<String>>") },
                { "<array><e><array/><array/></e></array>", Type.of(String[][].class) },
                { "<map><entry><key><list/><list/></key><value>x</value></entry></map>", Type.of("Map<List<String>, String>") },
                { "<map><entry><key>x</key><value><map/><map/></value></entry></map>", Type.of("Map<String, Map<String, String>>") },
                { "<list><e><list><e><list><e>1</e></list></e></list><list/></e></list>", Type.of("List<List<List<Integer>>>") } };

        for (final XmlParserType type : ALL_TYPES) {
            final XmlParser parser = parserOf(type);

            for (final Object[] entry : malformed) {
                final String xml = (String) entry[0];
                final Type<?> targetType = (Type<?>) entry[1];
                assertThrows(ParsingException.class, () -> parser.deserialize(xml, null, targetType), type + " " + xml);
            }
        }
    }

    @Test
    public void testDuplicatePropertyValueWrappersAreRejected() {
        final String[] malformed = { "<family><friend><person/><person/></friend></family>",
                "<family><friend><unknown><name>b</name></unknown><person/></friend></family>", "<family><friend><person/><list/></friend></family>",
                "<family><tags><list/><list/></tags></family>", "<family><nums><array/><array/></nums></family>", "<family><m><map/><map/></m></family>",
                "<family name=\"family\"><property name=\"friend\"><person name=\"friend\"/><person name=\"friend\"/></property></family>",
                "<p:family xmlns:p=\"urn:test\"><p:friend><p:person/><p:person/></p:friend></p:family>" };

        for (final XmlParserType type : ALL_TYPES) {
            final XmlParser parser = parserOf(type);

            for (final String xml : malformed) {
                assertThrows(ParsingException.class, () -> parser.deserialize(xml, Family.class), type + " " + xml);

                // A failed parse must not leave child-presence state in a pooled handler.
                final Family back = parser.deserialize("<family><friend><person><name>ok</name><age>2</age></person></friend><age>5</age></family>",
                        Family.class);
                assertEquals(new Person("ok", 2), back.getFriend(), type.toString());
                assertEquals(5, back.getAge(), type.toString());
            }
        }
    }

    // R5-03: an explicit Object value-type override keeps the text of a map value on every backend when the value's type attribute
    // names a bean class (the SAX reader converted the text with the bean class: "Can't parse", a bean read from JSON text, null for
    // empty text); a nested element carrying the bean class is still read as that bean.
    @Test
    public void testExplicitObjectOverrideWithBeanAttributeKeepsText_sameOnAllBackends() {
        final String childClass = R2BChild.class.getCanonicalName();
        final String childElement = parserOf(XmlParserType.StAX).serialize(r2bChild("n", 1));
        final XmlDeserConfig override = XmlDeserConfig.create().setValueType("k", Type.of(Object.class));
        final Type<Map<String, Object>> stringMapType = Type.of("Map<String, Object>");

        for (final String root : new String[] { "<map type=\"HashMap&lt;String, Object&gt;\">", "<map>" }) {
            for (final String text : new String[] { "5", "{\"name\": \"x\"}", "[1, 2]", "", " " }) {
                assertEquals(Map.of("k", text),
                        r2bSameOnAllBackends(root + "<entry><key>k</key><value type=\"" + childClass + "\">" + text + "</value></entry></map>", override,
                                stringMapType),
                        root + text);
            }
            assertEquals(Map.of("k", r2bChild("n", 1)), r2bSameOnAllBackends(
                    root + "<entry><key>k</key><value type=\"" + childClass + "\">" + childElement + "</value></entry></map>", override, stringMapType), root);
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

        final ParsingException fromReader = assertThrows(ParsingException.class, () -> domParser.deserialize(new StringReader(prefixed), null, nodeTypes));
        assertTrue(fromReader.getMessage().contains("xml node: order"), fromReader.getMessage());
        assertTrue(fromReader.getMessage().contains("<ns:order>"), fromReader.getMessage());

        // an unprefixed root is named once: the key and the element are the same string
        final ParsingException plain = assertThrows(ParsingException.class,
                () -> domParser.deserialize(new StringReader("<order><id>1</id></order>"), null, nodeTypes));
        assertEquals("No target class is specified for xml node: order", plain.getMessage());
    }

    @Test
    public void testRootCollectionUsesGenericTargetElementType() {
        final Type<List<Person>> targetType = Type.of(new TypeReference<List<Person>>() {
        });
        final String xml = "<list><e><person><name>A</name><age>1</age></person></e><e><person><name>B</name><age>2</age></person></e></list>";
        for (final XmlParserType parserType : new XmlParserType[] { XmlParserType.StAX, XmlParserType.DOM, XmlParserType.SAX }) {
            final List<Person> result = new AbacusXmlParserImpl(parserType, null, null, approvedTypes()).deserialize(xml, null, targetType);
            assertEquals(2, result.size());
            assertEquals("A", result.get(0).getName());
            assertEquals(1, result.get(0).getAge());
            assertEquals("B", result.get(1).getName());
            assertEquals(2, result.get(1).getAge());
        }
    }

    // G006-03: without circular-reference support no identity set is consulted; with it, cycles and shared references keep their output.
    @Test
    public void testHasCircularReference_outputIsPinned() {
        final CircularRefBean cycle = new CircularRefBean();
        cycle.setName("c");
        cycle.setReference(cycle);

        final XmlSerConfig circular = XmlSerConfig.create().setCircularReferenceSupported(true);
        final XmlParser parser = parserOf(XmlParserType.StAX);
        assertEquals("<circularRefBean><name>c</name><reference></reference></circularRefBean>", parser.serialize(cycle, circular));

        final CircularRefBean leaf = new CircularRefBean();
        leaf.setName("leaf");
        final List<CircularRefBean> shared = Arrays.asList(leaf, leaf);
        final String expectedShared = "<list><e><circularRefBean><name>leaf</name></circularRefBean></e><e><circularRefBean><name>leaf</name></circularRefBean></e></list>";
        assertEquals(expectedShared, parser.serialize(shared, circular));
        assertEquals(expectedShared, parser.serialize(shared));

        final ParsingException ex = assertThrows(ParsingException.class, () -> parser.serialize(cycle));
        assertTrue(ex.getMessage().startsWith("Serialization nesting depth exceeded"), ex.getMessage());
    }

    @Test
    public void testIgnoredMapValueSkipsTypeResolutionAndConversion() {
        XmlDeserConfig config = XmlDeserConfig.create().setMapValueType(Integer.class).setIgnoredPropNames(Map.class, Set.of("skip"));
        for (String value : new String[] { "<value>not-an-integer</value>", "<value type=\"not-a-java-type\"><![CDATA[invalid]]></value>",
                "<value type=\"not-a-java-type\"><map><entry><key>nested</key><value>invalid</value></entry></map></value>" }) {
            String xml = "<map><entry><key>skip</key>" + value + "</entry><entry><key>keep</key><value>7</value></entry></map>";
            for (XmlParserType parserType : new XmlParserType[] { XmlParserType.StAX, XmlParserType.DOM, XmlParserType.SAX }) {
                assertEquals(Map.of("keep", 7), new AbacusXmlParserImpl(parserType, null, null, approvedTypes()).deserialize(xml, config, Map.class));
            }
        }
    }

    @Test
    public void testMapEntryTypeAttributeApprovalAcrossBackends() {
        for (final XmlParserType parserType : ALL_TYPES) {
            final AbacusXmlParserImpl parser = new AbacusXmlParserImpl(parserType, null, null, approvedTypes());
            for (final String attr : new String[] { "", " type=\"\"", " type=\"String\"" }) {
                assertEquals(Map.of("k", "v"), parser.deserialize("<map><entry" + attr + "><key>k</key><value>v</value></entry></map>", Map.class),
                        parserType.name() + attr);
            }
            final String unapproved = "<map><entry type=\"not.approved.Type\"><key>k</key><value>v</value></entry></map>";
            assertThrows(ParsingException.class, () -> parser.deserialize(unapproved, Map.class), parserType.name());
            final XmlDeserConfig ignoredKey = XmlDeserConfig.create().setIgnoredPropNames(Map.class, Set.of("k"));
            assertThrows(ParsingException.class, () -> parser.deserialize(unapproved, ignoredKey, Map.class), parserType.name());
        }
    }

    // R5-06: an Object[] property item holding a nested array, read under a String element configuration, keeps the nested
    // Object[] on every backend (the StAX and DOM readers built a String[], or failed with "Unsupported class type" on the StAX
    // reader under the attribute name style).
    @Test
    public void testNestedArrayItemUnderStringElementConfiguration_sameOnAllBackends() {
        final R2BHolder holder = new R2BHolder();
        holder.setArr(new Object[] { "x", new Object[] { "deep" } });
        final XmlDeserConfig strings = XmlDeserConfig.create().setElementType(Type.of(String.class));
        final Type<R2BHolder> type = Type.of(R2BHolder.class);

        for (final XmlSerConfig writeConfig : new XmlSerConfig[] { XmlSerConfig.create().setWriteTypeInfo(true),
                XmlSerConfig.create().setWriteTypeInfo(true).setPrettyFormat(true),
                XmlSerConfig.create().setWriteTypeInfo(true).setTagByPropertyName(false) }) {
            final String xml = parserOf(XmlParserType.StAX).serialize(holder, writeConfig);
            final Object[] arr = r2bSameOnAllBackends(xml, strings, type).getArr();
            assertEquals("x", arr[0], xml);
            assertArrayEquals(new Object[] { "deep" }, assertInstanceOf(Object[].class, arr[1], xml), xml);
            assertEquals(Object[].class, arr[1].getClass(), xml);
        }
    }

    /**
     * The ignored-name filter tested a null map key by passing the bare {@code null} to
     * {@code ignoredPropNames.contains(..)}, which throws {@code NullPointerException} on a
     * {@code Set.of(..)} - the very shape {@code ParserConfig.setIgnoredPropNames}' own javadoc examples use.
     * A null key is written as the element {@code <null>}, so it must be filtered under the name "null",
     * exactly as {@code XmlParserImpl} already does (see its
     * {@code fixG04_aNullMapKeyIsIgnoredUnderTheNameItIsWrittenWith}). This covers the write path and both
     * read paths (StAX and DOM).
     */
    @Test
    public void test_nullMapKeyAgainstNullHostileIgnoredNames_regression_20260918() {
        final Map<Object, String> withNullKey = new LinkedHashMap<>();
        withNullKey.put(null, "v1");
        withNullKey.put("k", "v2");

        for (final XmlParserType type : new XmlParserType[] { XmlParserType.StAX, XmlParserType.DOM }) {
            final AbacusXmlParserImpl parser = new AbacusXmlParserImpl(type, null, null, approvedTypes());

            // A Set that rejects a null argument must not blow up the writer.
            // Before the fix this threw NullPointerException from Set.of(..).contains(null).
            final String written = parser.serialize(withNullKey, new XmlSerConfig().setIgnoredPropNames(Map.class, Set.of("k")));
            assertTrue(written.contains("v1"), type + " -> " + written);
            assertTrue(!written.contains("v2"), type + " -> " + written);

            // The null key is filtered under the name it is written as ("null"), not skipped.
            final String filtered = parser.serialize(withNullKey, new XmlSerConfig().setIgnoredPropNames(Map.class, N.asSet("null")));
            assertTrue(filtered.contains("v2"), type + " -> " + filtered);
            assertTrue(!filtered.contains("v1"), type + " -> " + filtered);

            // And the read paths must survive the same null-hostile set.
            final String xmlWithNullKey = parser.serialize(withNullKey);
            final XmlDeserConfig dc = new XmlDeserConfig().setIgnoredPropNames(Map.class, Set.of("k"));
            final Map<?, ?> readBack = parser.deserialize(xmlWithNullKey, dc, Map.class);
            assertNotNull(readBack, type.toString());
        }
    }

    // R5-07: a bean property element without content is read as empty text on the StAX reader too, with the same type selection
    // as text (it read the attribute's type even where text ignores it, ignored the property's format and the Optional payload
    // refinement): an Object property keeps "" under a primitive or an ignored scalar attribute, an Optional<Object> property
    // under scalar metadata is the payload's empty value, a formatted number fails as for any text it cannot read.
    @Test
    public void testPropertyElementWithoutContent_sameOnAllBackends() {
        final String root = r5TypedRoot(new R5EmptyHolder());
        final String end = r5EndTag(root);
        final String untyped = r5UntypedRoot(root);
        final Type<R5EmptyHolder> type = Type.of(R5EmptyHolder.class);

        for (final String body : new String[] { "", "<!-- c -->" }) {
            assertEquals("", r2bSameOnAllBackends(untyped + "<open type=\"Long\">" + body + "</open>" + end, null, type).getOpen(), body);
            assertEquals("", r2bSameOnAllBackends(untyped + "<open type=\"int\">" + body + "</open>" + end, null, type).getOpen(), body);
            assertEquals("", r2bSameOnAllBackends(root + "<open type=\"int\">" + body + "</open>" + end, null, type).getOpen(), body);
            assertNull(r2bSameOnAllBackends(root + "<open type=\"Long\">" + body + "</open>" + end, null, type).getOpen(), body);
            assertEquals(java.util.Optional.empty(), r2bSameOnAllBackends(root + "<opt type=\"Long\">" + body + "</opt>" + end, null, type).getOpt(), body);
            assertEquals(java.util.Optional.of(0), r2bSameOnAllBackends(root + "<opt type=\"int\">" + body + "</opt>" + end, null, type).getOpt(), body);
            assertNull(r2bSameOnAllBackends(root + "<when>" + body + "</when>" + end, null, type).getWhen(), body);
            assertEquals(0, r2bSameOnAllBackends(root + "<count>" + body + "</count>" + end, null, type).getCount(), body);
            assertEquals("", r2bSameOnAllBackends(root + "<text>" + body + "</text>" + end, null, type).getText(), body);
            assertNull(r2bSameOnAllBackends(root + "<list>" + body + "</list>" + end, null, type).getList(), body);
            r5SameFailureOnAllBackends(root + "<amount>" + body + "</amount>" + end, null, type, ParsingException.class,
                    "Failed to parse complete number value");
        }
        // the writer's own output for a formatted number still reads back
        final R5EmptyHolder holder = new R5EmptyHolder();
        holder.setAmount(1234);
        assertEquals(1234, r2bSameOnAllBackends(parserOf(XmlParserType.StAX).serialize(holder), null, type).getAmount());
    }

    // G11-01: the empty element of a property-less class is pinned on every backend for the first write (the name tags
    // are built) and the later ones (the tags are cached per class): root, list, map value and bean property, both tag
    // styles, with type info, pretty format, and every way of choosing the naming policy
    @Test
    public void testWriteEmptyObject_pinnedOutputPerBackendAndConfig() {
        final XmlSerConfig lenient = XmlSerConfig.create().setFailOnEmptyBean(false);
        final String typeName = Type.of(PerfReviewMarker.class).name();
        final PerfReviewMarkerHolder holder = new PerfReviewMarkerHolder();
        holder.setFirst(new PerfReviewMarker());
        holder.setTag("t");
        final Map<String, Object> map = new LinkedHashMap<>();
        map.put("m", new PerfReviewMarker());
        map.put("n", "v");

        for (final XmlParserType type : ALL_TYPES) {
            final XmlParser parser = parserOf(type);
            final String label = type.toString();
            for (int round = 0; round < 3; round++) {
                assertEquals("<perfReviewMarker></perfReviewMarker>", parser.serialize(new PerfReviewMarker(), lenient), label);
                assertEquals("<list><e><perfReviewMarker></perfReviewMarker></e><e><perfReviewMarker></perfReviewMarker></e></list>",
                        parser.serialize(N.asList(new PerfReviewMarker(), new PerfReviewMarker()), lenient), label);
                assertEquals(
                        "<map><entry><key>m</key><value><perfReviewMarker></perfReviewMarker></value></entry><entry><key>n</key><value>v</value></entry></map>",
                        parser.serialize(map, lenient), label);
                assertEquals("<perfReviewMarkerHolder><first><perfReviewMarker></perfReviewMarker></first><tag>t</tag></perfReviewMarkerHolder>",
                        parser.serialize(holder, lenient), label);
                assertEquals("<perfReviewMarker type=\"" + typeName + "\"></perfReviewMarker>",
                        parser.serialize(new PerfReviewMarker(), XmlSerConfig.create().setFailOnEmptyBean(false).setWriteTypeInfo(true)), label);
                assertEquals("<bean name=\"perfReviewMarker\"></bean>",
                        parser.serialize(new PerfReviewMarker(), XmlSerConfig.create().setFailOnEmptyBean(false).setTagByPropertyName(false)), label);
                assertEquals("<bean name=\"perfReviewMarker\" type=\"" + typeName + "\"></bean>", parser.serialize(new PerfReviewMarker(),
                        XmlSerConfig.create().setFailOnEmptyBean(false).setTagByPropertyName(false).setWriteTypeInfo(true)), label);
                assertEquals(
                        "<list>\n    <e>\n        <perfReviewMarker></perfReviewMarker>\n    </e>\n    <e>\n        <perfReviewMarker></perfReviewMarker>\n    </e>\n</list>",
                        parser.serialize(N.asList(new PerfReviewMarker(), new PerfReviewMarker()),
                                XmlSerConfig.create().setFailOnEmptyBean(false).setPrettyFormat(true)),
                        label);
                assertEquals(
                        "<perfReviewMarkerHolder>\n    <first>\n        <perfReviewMarker></perfReviewMarker>\n    </first>\n    <tag>t</tag>\n</perfReviewMarkerHolder>",
                        parser.serialize(holder, XmlSerConfig.create().setFailOnEmptyBean(false).setPrettyFormat(true)), label);
                // the naming policy of the configuration, of the annotation, and the configuration over the annotation
                assertEquals("<PERF_REVIEW_MARKER></PERF_REVIEW_MARKER>",
                        parser.serialize(new PerfReviewMarker(),
                                XmlSerConfig.create().setFailOnEmptyBean(false).setPropNamingPolicy(com.landawn.abacus.util.NamingPolicy.SCREAMING_SNAKE_CASE)),
                        label);
                assertEquals("<perf-review-marker></perf-review-marker>", parser.serialize(new PerfReviewMarker(),
                        XmlSerConfig.create().setFailOnEmptyBean(false).setPropNamingPolicy(com.landawn.abacus.util.NamingPolicy.KEBAB_CASE)), label);
                assertEquals("<PerfReviewMarker></PerfReviewMarker>",
                        parser.serialize(new PerfReviewMarker(),
                                XmlSerConfig.create().setFailOnEmptyBean(false).setPropNamingPolicy(com.landawn.abacus.util.NamingPolicy.UPPER_CAMEL_CASE)),
                        label);
                assertEquals("<perf_review_annotated_marker></perf_review_annotated_marker>", parser.serialize(new PerfReviewAnnotatedMarker(), lenient),
                        label);
                assertEquals("<PerfReviewAnnotatedMarker></PerfReviewAnnotatedMarker>",
                        parser.serialize(new PerfReviewAnnotatedMarker(),
                                XmlSerConfig.create().setFailOnEmptyBean(false).setPropNamingPolicy(com.landawn.abacus.util.NamingPolicy.UPPER_CAMEL_CASE)),
                        label);
                // the strict default still rejects the class, with the same message
                final ParsingException strict = assertThrows(ParsingException.class, () -> parser.serialize(new PerfReviewMarker()));
                assertTrue(strict.getMessage().startsWith("Unsupported class: "), strict.getMessage());
            }
        }
    }

    // R3-03: a bean-shaped element in an open slot that holds a bean-shaped element of its own reads as nested maps on every
    // backend, in a list item, an Object property and a map value (the SAX handler took the inner element for a map node when it
    // closed: it popped the enclosing map's key/value types, or failed on an empty queue inside a list item or an Object property).
    // Properties without type attributes only: attribute adoption inside such an element still differs per backend (reported)
    @Test
    public void testReadBeanShapedElement_nestedBeanShapedElementOnAllBackends() {
        // element names that no bean class in the test tree carries: under a bean root the readers discover a bean class by the
        // nested element's name (an <inner> element found BeansFieldHeadReview20261003Test.Inner), and that is not what is tested here
        final String thing = "<r3Thing><a>1</a><d><r3Inner><x>4</x><y>5</y></r3Inner></d><b>2</b></r3Thing>";
        // a property element holds its nested value in one wrapper child (<d> holds <r3Inner>'s map, <deep> holds <r3Deeper>'s):
        // the readers' standing shape rule; two or more text children directly under a property are rejected on every backend
        final String deeper = "<r3Thing><a>1</a><d><r3Inner><deep><r3Deeper><z>6</z><w>7</w></r3Deeper></deep><x>4</x></r3Inner></d></r3Thing>";
        final Map<String, Object> inner = new LinkedHashMap<>();
        inner.put("x", "4");
        inner.put("y", "5");
        final Map<String, Object> expected = new LinkedHashMap<>();
        expected.put("a", "1");
        expected.put("d", inner);
        expected.put("b", "2");
        final Map<String, Object> expectedDeeper = Map.of("a", "1", "d", Map.of("deep", Map.of("z", "6", "w", "7"), "x", "4"));
        final Type<List<Object>> listType = Type.of("List<Object>");

        for (final String root : new String[] { "<list>", "<list type=\"ArrayList&lt;Object&gt;\">" }) {
            assertEquals(List.of(expected), r2bSameOnAllBackends(root + "<e>" + thing + "</e></list>", null, listType), root);
            assertEquals(List.of(expected, expected, "s"),
                    r2bSameOnAllBackends(root + "<e>" + thing + "</e><e>" + thing + "</e><e>s</e></list>", null, listType), root);
            assertEquals(List.of(expectedDeeper), r2bSameOnAllBackends(root + "<e>" + deeper + "</e></list>", null, listType), root);
        }
        assertEquals(Map.of("k", expected, "l", expectedDeeper),
                r2bSameOnAllBackends(
                        "<map><entry><key>k</key><value>" + thing + "</value></entry><entry><key>l</key><value>" + deeper + "</value></entry></map>", null,
                        Type.of("Map<String, Object>")));

        final String holderXml = parserOf(XmlParserType.StAX).serialize(new R2BHolder(), XmlSerConfig.create().setWriteTypeInfo(true));
        final String holderTag = holderXml.substring(0, holderXml.indexOf('>') + 1);
        final String holderEnd = "</" + holderTag.substring(1, holderTag.indexOf(' ')) + ">";
        final R2BHolder holder = r2bSameOnAllBackends(holderTag + "<open>" + thing + "</open>" + holderEnd, null, Type.of(R2BHolder.class));
        assertEquals(expected, holder.getOpen());
        assertEquals(expectedDeeper, r2bSameOnAllBackends(holderTag + "<open>" + deeper + "</open>" + holderEnd, null, Type.of(R2BHolder.class)).getOpen());
    }

    // G05-01: bean properties whose element carries a type attribute on the DOM backend (the approval result now feeds getConcreteClass
    // instead of a second read and resolution of the same attribute): the declared class, a subclass, an incompatible class, a
    // container implementation, an open slot and a rejected attribute all read as on the StAX and SAX backends.
    @Test
    public void testReadByDOMParser_beanPropertyTypeAttributeReused_pins() {
        final G05Sub sub = new G05Sub();
        sub.setName("n");
        sub.setExtra(7);
        final G05Holder holder = new G05Holder();
        holder.setChild(sub);
        holder.setTags(new java.util.ArrayList<>(List.of(1, 2)));
        holder.setCounts(new LinkedHashMap<>(Map.of("a", 1)));
        holder.setOpen(5);

        final String typed = parserOf(XmlParserType.StAX).serialize(holder, XmlSerConfig.create().setWriteTypeInfo(true));
        // the writer puts the DECLARED property type on the property element and the runtime class on the nested bean element
        final String childTag = "<child type=\"com.landawn.abacus.parser.AbacusXmlParserImplTest.G05Base\">";
        final String tagsTag = "<tags type=\"List&lt;Integer&gt;\">";
        final String countsTag = "<counts type=\"Map&lt;String, Integer&gt;\">";
        final String openTag = "<open type=\"Object\">5</open>";
        assertTrue(typed.contains(childTag) && typed.contains(tagsTag) && typed.contains(countsTag) && typed.contains(openTag), typed);

        final Type<G05Holder> type = Type.of(G05Holder.class);

        final G05Holder back = g05SameOnAllBackends(typed, type);
        assertEquals(sub, back.getChild());
        assertEquals(List.of(1, 2), back.getTags());
        assertEquals(Map.of("a", 1), back.getCounts());
        assertEquals(sub,
                g05SameOnAllBackends(typed.replace(childTag, "<child type=\"com.landawn.abacus.parser.AbacusXmlParserImplTest.G05Sub\">"), type).getChild());
        g05SameOnAllBackends(typed.replace(childTag, "<child type=\"Integer\">"), type);
        g05SameOnAllBackends(typed.replace(childTag, "<child type=\"\">"), type);
        g05SameOnAllBackends(typed.replace(childTag, "<child>"), type);

        final G05Holder linked = g05SameOnAllBackends(typed.replace(tagsTag, "<tags type=\"LinkedList&lt;Integer&gt;\">"), type);
        assertInstanceOf(java.util.LinkedList.class, linked.getTags());
        assertEquals(List.of(1, 2), linked.getTags());
        assertEquals(List.of(1, 2), g05SameOnAllBackends(typed.replace(tagsTag, "<tags type=\"HashMap&lt;String, Integer&gt;\">"), type).getTags());
        assertEquals(List.of(1, 2), g05SameOnAllBackends(typed.replace(tagsTag, "<tags type=\"ImmutableList&lt;Integer&gt;\">"), type).getTags());
        assertEquals(List.of(1, 2), g05SameOnAllBackends(typed.replace(tagsTag, "<tags>"), type).getTags());

        final G05Holder sorted = g05SameOnAllBackends(typed.replace(countsTag, "<counts type=\"TreeMap&lt;String, Integer&gt;\">"), type);
        assertEquals(Map.of("a", 1), sorted.getCounts());
        assertEquals(Map.of("a", 1), g05SameOnAllBackends(typed.replace(countsTag, "<counts type=\"ArrayList&lt;Integer&gt;\">"), type).getCounts());

        assertEquals(List.of(1, 2), g05SameOnAllBackends(typed.replace(openTag, "<open type=\"List&lt;Integer&gt;\">[1, 2]</open>"), type).getOpen());
        assertEquals("5", g05SameOnAllBackends(typed.replace(openTag, "<open>5</open>"), type).getOpen());
        assertEquals("5", g05SameOnAllBackends(typed.replace(openTag, "<open type=\"int\">5</open>"), type).getOpen());

        // Scalar metadata on an Object property is adopted on every backend, as the DOM reader adopted it (owner ruling on G05-02,
        // round 2 R2A; the StAX and SAX readers used to read the declared Object's text).
        assertEquals(5L, g05SameOnAllBackends(typed.replace(openTag, "<open type=\"Long\">5</open>"), type).getOpen());
        assertEquals(5, g05SameOnAllBackends(typed.replace(openTag, "<open type=\"Integer\">5</open>"), type).getOpen());

        for (final XmlParserType parserType : ALL_TYPES) {
            final XmlParser parser = parserOf(parserType);

            assertThrows(ParsingException.class, () -> parser.deserialize(typed.replace(childTag, "<child type=\"not.approved.X\">"), type), parserType.name());
            assertThrows(ParsingException.class, () -> parser.deserialize(typed.replace(tagsTag, "<tags type=\"java.lang.Runtime\">"), type),
                    parserType.name());
            assertThrows(ParsingException.class, () -> parser.deserialize(typed.replace(openTag, "<open type=\"not.approved.X\">5</open>"), type),
                    parserType.name());
        }
    }

    // G05-01: list/array items and the open map key/value fallback with type attributes on the DOM backend (resolveItemType(Node, Type)
    // now reads and approves the attribute once): compatible metadata is adopted, incompatible metadata and a blank attribute keep the
    // declared type, a primitive name fits its wrapper, an open item adopts scalars and containers (an uninstantiable container as an
    // open mutable schema), and a rejected attribute still fails on every backend.
    @Test
    public void testReadByDOMParser_itemTypeAttributeResolvedOnce_pins() {
        final String items = "<e type=\"Integer\">1</e><e type=\"Long\">2</e><e>3</e><e type=\"int\">4</e><e type=\"\">5</e><e type=\"String\">6</e>";
        final String list = "<list>" + items + "</list>";
        final String array = "<array>" + items + "</array>";

        assertEquals(List.of(1, 2, 3, 4, 5, 6), g05SameOnAllBackends(list, Type.of("List<Integer>")));
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5, 6 }, g05SameOnAllBackends(array, Type.of(Integer[].class)));
        assertEquals(List.of(1, 2L, "3", 4, "5", "6"), g05SameOnAllBackends(list, Type.of("List<Object>")));
        assertArrayEquals(new Object[] { 1, 2L, "3", 4, "5", "6" }, g05SameOnAllBackends(array, Type.of(Object[].class)));
        assertArrayEquals(new String[] { "1", "2", "3", "4", "5", "6" }, g05SameOnAllBackends(array, Type.of(String[].class)));
        g05SameOnAllBackends(list, Type.of("List<String>"));

        // an abstract scalar component adopts the scalar the metadata names
        final String numbers = "<list><e type=\"Integer\">1</e><e type=\"Long\">2</e><e type=\"Double\">3.5</e></list>";
        assertEquals(List.of(1, 2L, 3.5), g05SameOnAllBackends(numbers, Type.of("List<Number>")));
        assertArrayEquals(new Number[] { 1, 2L, 3.5 }, g05SameOnAllBackends(numbers.replace("list>", "array>"), Type.of(Number[].class)));

        final String containers = "<list><e type=\"List&lt;Integer&gt;\">[1, 2]</e><e type=\"Map&lt;String, Integer&gt;\">{\"a\": 1}</e>"
                + "<e type=\"HashSet&lt;Integer&gt;\">[3]</e><e type=\"ImmutableList&lt;Integer&gt;\">[4, 5]</e><e type=\"ArrayList&lt;Integer&gt;\"></e></list>";
        final List<Object> adopted = g05SameOnAllBackends(containers, Type.of("List<Object>"));
        assertEquals(List.of(1, 2), adopted.get(0));
        assertEquals(Map.of("a", 1), adopted.get(1));
        assertEquals(Set.of(3), adopted.get(2));
        assertEquals(List.of(4, 5), adopted.get(3));
        // an empty wrapper reads as null on every backend, whatever its type attribute
        assertNull(adopted.get(4));

        // a declared container item refined by its metadata (implementation, uninstantiable container, no attribute)
        final String collections = "<list><e type=\"List&lt;Integer&gt;\">[1, 2]</e><e type=\"HashSet&lt;Integer&gt;\">[3]</e>"
                + "<e type=\"ImmutableList&lt;Integer&gt;\">[4, 5]</e><e>[6]</e></list>";
        assertEquals(List.of(List.of(1, 2), Set.of(3), List.of(4, 5), List.of(6)), g05SameOnAllBackends(collections, Type.of("List<Collection<Integer>>")));

        final String map = "<map><entry><key type=\"ImmutableList&lt;Integer&gt;\">[1, 2]</key><value type=\"ImmutableMap&lt;String, Integer&gt;\">{\"a\": 1}</value></entry>"
                + "<entry><key type=\"Integer\">3</key><value type=\"ImmutableSet&lt;Integer&gt;\">[4]</value></entry><entry><key>5</key><value>6</value></entry></map>";
        assertEquals(Map.of(List.of(1, 2), Map.of("a", 1), 3, Set.of(4), "5", "6"), g05SameOnAllBackends(map, Type.of("Map<Object, Object>")));

        for (final XmlParserType parserType : ALL_TYPES) {
            final XmlParser parser = parserOf(parserType);

            assertThrows(ParsingException.class, () -> parser.deserialize("<list><e type=\"java.lang.Runtime\">1</e></list>", Type.of("List<Object>")),
                    parserType.name());
            assertThrows(ParsingException.class, () -> parser.deserialize("<array><e type=\"not.approved.X\">1</e></array>", Type.of(Object[].class)),
                    parserType.name());
            assertThrows(ParsingException.class,
                    () -> parser.deserialize("<map><entry><key type=\"not.approved.X\">1</key><value>2</value></entry></map>", Type.of("Map<Object, Object>")),
                    parserType.name());
            assertThrows(ParsingException.class,
                    () -> parser.deserialize("<map><entry><key>1</key><value type=\"not.approved.X\">2</value></entry></map>", Type.of("Map<Object, Object>")),
                    parserType.name());
        }
    }

    // R2B-02: the Abacus DOM reader reads and approves a node's type attribute once (bean property, list / array item, map key and
    // value) and hands the result to the text and nested value readers, which used to resolve it again. Pins the values and the
    // rejections of every site: typed and untyped roots, scalar, container, bean and wrapper properties, null markers, blank and
    // primitive-named attributes, a formatted property, configured value types, items, keys, values and a bean-shaped element in
    // an open slot, equal on the StAX, DOM and SAX backends and through the DOM Node overload.
    @Test
    public void testReadByDOMParser_typeAttributeResolvedOncePerNode_pins() {
        final R2BHolder holder = new R2BHolder();
        holder.setId(1);
        holder.setName("h");
        holder.setOpen(5);
        holder.setTags(new java.util.ArrayList<>(List.of(1, 2)));
        holder.setCounts(new LinkedHashMap<>(Map.of("a", 1)));
        holder.setChild(r2bChild("c", 2));
        holder.setItems(new java.util.ArrayList<>(List.of(1, "s", 2L, r2bChild("c", 2))));
        final Map<Object, Object> keyed = new LinkedHashMap<>();
        keyed.put(3, "three");
        keyed.put("k", r2bChild("c", 2));
        holder.setKeyed(keyed);
        holder.setArr(new Object[] { 1, "x" });
        holder.setNumbers(new java.util.ArrayList<>(List.of(1, 2L, 3.5)));
        holder.setWrapped(java.util.Optional.of(7));
        holder.setWhen(new java.util.Date(1700000000000L));

        final XmlParser writer = parserOf(XmlParserType.StAX);
        final String typed = writer.serialize(holder, XmlSerConfig.create().setWriteTypeInfo(true));
        final String untyped = writer.serialize(holder);
        final String childElement = writer.serialize(r2bChild("o", 1));
        final String childName = childElement.substring(1, childElement.indexOf('>'));
        final String childClass = R2BChild.class.getCanonicalName();
        final Type<R2BHolder> holderType = Type.of(R2BHolder.class);
        final java.text.SimpleDateFormat dayFormat = new java.text.SimpleDateFormat("yyyy-MM-dd");

        final String openTag = "<open type=\"Object\">5</open>";
        final String childTag = "<child type=\"" + childClass + "\"><" + childName + " type=\"" + childClass + "\">";
        final String tagsTag = "<tags type=\"List&lt;Integer&gt;\">[1, 2]</tags>";
        assertTrue(typed.contains(openTag) && typed.contains(childTag) && typed.contains(tagsTag) && typed.contains("<when type=\"JUDate\">")
                && typed.contains("<wrapped>7</wrapped>") && untyped.contains("<open>5</open>") && untyped.contains("<when>"), typed + "\n" + untyped);

        final R2BHolder back = r2bSameOnAllBackends(typed, null, holderType);
        assertEquals(1, back.getId());
        assertEquals("h", back.getName());
        assertEquals("5", back.getOpen());
        assertEquals(List.of(1, 2), back.getTags());
        assertEquals(Map.of("a", 1), back.getCounts());
        assertEquals(r2bChild("c", 2), back.getChild());
        assertEquals(List.of(1, "s", 2L, r2bChild("c", 2)), back.getItems());
        assertEquals(Map.of(3, "three", "k", r2bChild("c", 2)), back.getKeyed());
        assertArrayEquals(new Object[] { 1, "x" }, back.getArr());
        assertEquals(List.of(1, 2, 3.5), back.getNumbers());
        assertEquals(java.util.Optional.of("7"), back.getWrapped());
        assertEquals(dayFormat.format(holder.getWhen()), dayFormat.format(back.getWhen()));
        final R2BHolder untypedBack = r2bSameOnAllBackends(untyped, null, holderType);
        assertEquals("5", untypedBack.getOpen());
        assertEquals(List.of(1, 2), untypedBack.getTags());
        assertEquals(Map.of("a", 1), untypedBack.getCounts());
        assertEquals(r2bChild("c", 2), untypedBack.getChild());
        // the writer-named nested bean element is found by its node name even without type attributes
        assertEquals(List.of("1", "s", "2", r2bChild("c", 2)), untypedBack.getItems());
        assertEquals(Map.of("3", "three", "k", r2bChild("c", 2)), untypedBack.getKeyed());
        assertArrayEquals(new Object[] { "1", "x" }, untypedBack.getArr());
        assertEquals(List.of(1, 2, 3.5), untypedBack.getNumbers());
        assertEquals(java.util.Optional.of("7"), untypedBack.getWrapped());
        assertEquals(dayFormat.format(holder.getWhen()), dayFormat.format(untypedBack.getWhen()));

        // an Object property: container metadata (instantiable or not) is adopted, a null marker wins, a blank or primitive-named
        // attribute keeps the text, a nested bean element is read through its attribute
        assertEquals(List.of(1, 2),
                r2bSameOnAllBackends(typed.replace(openTag, "<open type=\"List&lt;Integer&gt;\">[1, 2]</open>"), null, holderType).getOpen());
        assertEquals(List.of(1, 2),
                r2bSameOnAllBackends(typed.replace(openTag, "<open type=\"ImmutableList&lt;Integer&gt;\">[1, 2]</open>"), null, holderType).getOpen());
        assertNull(r2bSameOnAllBackends(typed.replace(openTag, "<open type=\"Long\" isNull=\"true\"></open>"), null, holderType).getOpen());
        assertEquals("5", r2bSameOnAllBackends(typed.replace(openTag, "<open type=\"\">5</open>"), null, holderType).getOpen());
        assertEquals("5", r2bSameOnAllBackends(typed.replace(openTag, "<open type=\"int\">5</open>"), null, holderType).getOpen());
        assertEquals(r2bChild("o", null),
                r2bSameOnAllBackends(typed.replace(openTag, "<open type=\"" + childClass + "\"><" + childName + "><name>o</name></" + childName + "></open>"),
                        null, holderType).getOpen());
        assertEquals(List.of(1, 2),
                r2bSameOnAllBackends(untyped.replace("<open>5</open>", "<open type=\"List&lt;Integer&gt;\">[1, 2]</open>"), null, holderType).getOpen());
        assertNull(r2bSameOnAllBackends(untyped.replace("<open>5</open>", "<open type=\"Long\" isNull=\"true\"></open>"), null, holderType).getOpen());

        // a bean property: an incompatible, blank or absent attribute keeps the declared bean, a null marker wins
        assertEquals(r2bChild("c", 2),
                r2bSameOnAllBackends(typed.replace(childTag, "<child type=\"Integer\"><" + childName + " type=\"" + childClass + "\">"), null, holderType)
                        .getChild());
        assertEquals(r2bChild("c", 2),
                r2bSameOnAllBackends(typed.replace(childTag, "<child><" + childName + " type=\"" + childClass + "\">"), null, holderType).getChild());
        assertEquals(r2bChild("c", 2),
                r2bSameOnAllBackends(typed.replace(childTag, "<child type=\"\"><" + childName + " type=\"" + childClass + "\">"), null, holderType).getChild());
        assertNull(r2bSameOnAllBackends(
                typed.replace(childTag, "<child type=\"" + childClass + "\" isNull=\"true\"><" + childName + " type=\"" + childClass + "\">"), null, holderType)
                        .getChild());

        // a formatted property (readPropValue path) with and without its attribute and with a null marker; a wrapper property
        // refined by scalar or wrapper metadata or null-marked
        assertEquals(dayFormat.format(holder.getWhen()),
                dayFormat.format(r2bSameOnAllBackends(typed.replace("<when type=\"JUDate\">", "<when>"), null, holderType).getWhen()));
        assertNull(r2bSameOnAllBackends(typed.replace("<when type=\"JUDate\">", "<when type=\"JUDate\" isNull=\"true\">"), null, holderType).getWhen());
        assertEquals(java.util.Optional.of(7L),
                r2bSameOnAllBackends(typed.replace("<wrapped>", "<wrapped type=\"java.util.Optional&lt;Long&gt;\">"), null, holderType).getWrapped());
        assertEquals(java.util.Optional.of(7L), r2bSameOnAllBackends(typed.replace("<wrapped>", "<wrapped type=\"Long\">"), null, holderType).getWrapped());
        assertEquals(java.util.Optional.empty(), r2bSameOnAllBackends(typed.replace("<wrapped>", "<wrapped isNull=\"true\">"), null, holderType).getWrapped());

        // container property items in child-element form: compatible, incompatible-but-fitting and absent item attributes
        assertEquals(List.of(1, 2, 3),
                r2bSameOnAllBackends(
                        typed.replace(tagsTag,
                                "<tags type=\"List&lt;Integer&gt;\"><list><e type=\"Integer\">1</e><e type=\"Long\">2</e><e>3</e></list></tags>"),
                        null, holderType).getTags());

        // list and array items: scalars, null markers, blank attributes, containers, a nested bean, an untyped nested element
        final String items = "<e type=\"Integer\">1</e><e type=\"Long\">2</e><e>3</e><e type=\"int\">4</e><e type=\"\">5</e><e type=\"String\">6</e>"
                + "<e type=\"Integer\" isNull=\"true\"></e><e isNull=\"true\"></e><e type=\"List&lt;Integer&gt;\">[7]</e>"
                + "<e type=\"ImmutableList&lt;Integer&gt;\">[8]</e><e type=\"" + childClass + "\"><" + childName
                + "><name>n</name><count type=\"Integer\">1</count></" + childName + "></e><e type=\"" + childClass
                + "\" isNull=\"true\"></e><e><thing><name>u</name></thing></e>" + "<e type=\"Double\">9.5</e>";
        final List<Object> expectedItems = new java.util.ArrayList<>(List.of(1, 2L, "3", 4, "5", "6"));
        expectedItems.add(null);
        expectedItems.add(null);
        expectedItems.addAll(List.of(List.of(7), List.of(8), r2bChild("n", 1)));
        expectedItems.add(null);
        expectedItems.addAll(List.of(Map.of("name", "u"), 9.5));
        assertEquals(expectedItems, r2bSameOnAllBackends("<list>" + items + "</list>", null, Type.of("List<Object>")));
        assertArrayEquals(expectedItems.toArray(), r2bSameOnAllBackends("<array>" + items + "</array>", null, Type.of("Object[]")));
        assertEquals(List.of(1, 2L, 3.5),
                r2bSameOnAllBackends("<list><e type=\"Integer\">1</e><e type=\"Long\">2</e><e type=\"Double\">3.5</e></list>", null, Type.of("List<Number>")));
        final List<Object> expectedContainers = new java.util.ArrayList<>(List.of(List.of(1, 2), Set.of(3), List.of(4, 5), List.of(6)));
        expectedContainers.add(null);
        expectedContainers.add(List.of(7));
        assertEquals(expectedContainers,
                r2bSameOnAllBackends(
                        "<list><e type=\"List&lt;Integer&gt;\">[1, 2]</e><e type=\"HashSet&lt;Integer&gt;\">[3]</e>"
                                + "<e type=\"ImmutableList&lt;Integer&gt;\">[4, 5]</e><e>[6]</e><e type=\"ArrayList&lt;Integer&gt;\"></e>"
                                + "<e type=\"List&lt;Integer&gt;\"><list><e type=\"Integer\">7</e></list></e></list>",
                        null, Type.of("List<Collection<Integer>>")));

        // map keys and values: text and nested, typed and untyped, blank attributes, null markers, uninstantiable container
        // metadata on open keys and values
        final String entries = "<entry><key type=\"String\">a</key><value type=\"Integer\">1</value></entry><entry><key>b</key><value>2</value></entry>"
                + "<entry><key type=\"Integer\">3</key><value type=\"Long\">4</value></entry><entry><key type=\"\">c</key><value type=\"\">5</value></entry>"
                + "<entry><key type=\"ImmutableList&lt;Integer&gt;\">[1, 2]</key><value type=\"ImmutableMap&lt;String, Integer&gt;\">{\"a\": 1}</value></entry>"
                + "<entry><key type=\"List&lt;Integer&gt;\">[9]</key><value type=\"ImmutableSet&lt;Integer&gt;\">[4]</value></entry>"
                + "<entry><key>d</key><value isNull=\"true\"></value></entry><entry><key>e</key><value type=\"Integer\" isNull=\"true\"></value></entry>"
                + "<entry><key>f</key><value type=\"" + childClass + "\"><" + childName + "><name>n</name><count type=\"Integer\">1</count></" + childName
                + "></value></entry><entry><key>g</key><value><thing><name>u</name></thing></value></entry>"
                + "<entry><key>h</key><value type=\"List&lt;Integer&gt;\"><list><e type=\"Integer\">1</e></list></value></entry>"
                + "<entry><key>i</key><value type=\"" + childClass + "\" isNull=\"true\"></value></entry>"
                + "<entry><key><thing><name>k2</name></thing></key><value>nestedKey2</value></entry>"
                + "<entry><key type=\"Integer\" isNull=\"true\"></key><value>nullKey</value></entry><entry><key>j</key><value type=\"Double\">1.5</value></entry>";
        final Map<Object, Object> expectedEntries = new HashMap<>();
        expectedEntries.put("a", 1);
        expectedEntries.put("b", "2");
        expectedEntries.put(3, 4L);
        expectedEntries.put("c", "5");
        expectedEntries.put(List.of(1, 2), Map.of("a", 1));
        expectedEntries.put(List.of(9), Set.of(4));
        expectedEntries.put("d", null);
        expectedEntries.put("e", null);
        expectedEntries.put("f", r2bChild("n", 1));
        expectedEntries.put("g", Map.of("name", "u"));
        expectedEntries.put("h", List.of(1));
        expectedEntries.put("i", null);
        expectedEntries.put(Map.of("name", "k2"), "nestedKey2");
        expectedEntries.put(null, "nullKey");
        expectedEntries.put("j", 1.5);
        final Type<Map<Object, Object>> openMapType = Type.of("Map<Object, Object>");
        assertEquals(expectedEntries, r2bSameOnAllBackends("<map>" + entries + "</map>", null, openMapType));
        // a nested KEY carrying a bean type attribute is read as that bean on every backend (round 3: the DOM reader used to hand
        // the declared Object key type to its nested reader and read the bean as a map)
        final String nestedKey = "<entry><key type=\"" + childClass + "\"><" + childName + "><name>k</name></" + childName
                + "></key><value>nestedKey</value></entry>";
        assertEquals(Map.of(r2bChild("k", null), "nestedKey"), r2bSameOnAllBackends("<map>" + nestedKey + "</map>", null, openMapType));

        // configured value types: a scalar type converts, an explicit Object override keeps the text on every backend (round 3: the
        // SAX reader used to adopt the scalar attribute) and adopts container metadata, a configured container reads a nested
        // element or JSON text
        final String configured = "<map><entry><key>k1</key><value type=\"Integer\">1</value></entry><entry><key>k2</key><value type=\"Integer\">2</value></entry>"
                + "<entry><key>k3</key><value type=\"ImmutableList&lt;Integer&gt;\">[3]</value></entry>"
                + "<entry><key>k4</key><value type=\"Integer\"><list><e>4</e></list></value></entry>"
                + "<entry><key>k5</key><value type=\"List&lt;Integer&gt;\">[5]</value></entry></map>";
        final XmlDeserConfig valueTypes = XmlDeserConfig.create()
                .setValueType("k1", Type.of(Long.class))
                .setValueType("k2", Type.of(Object.class))
                .setValueType("k3", Type.of(Object.class))
                .setValueType("k4", Type.of("List<Integer>"))
                .setValueType("k5", Type.of("Object[]"));
        final Type<Map<String, Object>> stringMapType = Type.of("Map<String, Object>");
        for (final XmlParserType parserType : ALL_TYPES) {
            final Map<String, Object> values = parserOf(parserType).deserialize(configured, valueTypes, stringMapType);
            assertEquals(1L, values.get("k1"), parserType.name());
            assertEquals(List.of(3), values.get("k3"), parserType.name());
            assertEquals(List.of(4), values.get("k4"), parserType.name());
            assertArrayEquals(new Object[] { 5 }, (Object[]) values.get("k5"), parserType.name());
            assertEquals("2", values.get("k2"), parserType.name());
        }
        assertEquals("2", parserOf(XmlParserType.StAX).deserialize(r2bDocument(configured), valueTypes, stringMapType).get("k2"));

        // a bean-shaped element in an open slot, with a bean-shaped element nested in it, reads as a map of its properties on
        // every backend (round 3: the SAX reader failed with IndexOutOfBoundsException on the nested one inside a list item); the
        // properties carry no type attributes here - the backends still disagree on attribute adoption inside such an element
        final String beanShaped = "<list><e><thing><a>1</a><b>2</b><c>3</c><d><inner><x>4</x></inner></d></thing></e></list>";
        assertEquals(List.of(Map.of("a", "1", "b", "2", "c", "3", "d", Map.of("x", "4"))), r2bSameOnAllBackends(beanShaped, null, Type.of("List<Object>")));

        // a disallowed attribute is rejected at every site, with the same message, whether the node is text, nested or null-marked,
        // on a typed or untyped root, under a configured value type, and inside a bean-shaped element
        r2bRejectedOnAllBackends(typed.replace(openTag, "<open type=\"not.approved.X\">5</open>"), null, holderType);
        r2bRejectedOnAllBackends(typed.replace(openTag, "<open type=\"not.approved.X\" isNull=\"true\"></open>"), null, holderType);
        r2bRejectedOnAllBackends(typed.replace(openTag, "<open type=\"not.approved.X\"><" + childName + "><name>o</name></" + childName + "></open>"), null,
                holderType);
        r2bRejectedOnAllBackends(untyped.replace("<open>5</open>", "<open type=\"not.approved.X\">5</open>"), null, holderType);
        r2bRejectedOnAllBackends(untyped.replace("<open>5</open>", "<open type=\"not.approved.X\" isNull=\"true\"></open>"), null, holderType);
        r2bRejectedOnAllBackends(
                untyped.replace("<open>5</open>", "<open type=\"not.approved.X\"><" + childName + "><name>o</name></" + childName + "></open>"), null,
                holderType);
        r2bRejectedOnAllBackends(typed.replace(childTag, "<child type=\"not.approved.X\"><" + childName + " type=\"" + childClass + "\">"), null, holderType);
        r2bRejectedOnAllBackends(typed.replace(childTag, "<child type=\"" + childClass + "\"><" + childName + " type=\"not.approved.X\">"), null, holderType);
        r2bRejectedOnAllBackends(typed.replace("<when type=\"JUDate\">", "<when type=\"not.approved.X\">"), null, holderType);
        r2bRejectedOnAllBackends(typed.replace("<wrapped>", "<wrapped type=\"not.approved.X\">"), null, holderType);
        r2bRejectedOnAllBackends(typed.replace(tagsTag, "<tags type=\"not.approved.X\">[1, 2]</tags>"), null, holderType);
        r2bRejectedOnAllBackends(
                typed.replace(tagsTag, "<tags type=\"List&lt;Integer&gt;\"><list><e type=\"Integer\">1</e><e type=\"not.approved.X\">2</e></list></tags>"),
                null, holderType);
        r2bRejectedOnAllBackends("<list><e type=\"Integer\">1</e><e type=\"not.approved.X\">2</e></list>", null, Type.of("List<Object>"));
        r2bRejectedOnAllBackends("<list><e type=\"Integer\">1</e><e type=\"not.approved.X\">2</e></list>", null, Type.of("List<Integer>"));
        r2bRejectedOnAllBackends("<list><e type=\"not.approved.X\"><" + childName + "><name>n</name></" + childName + "></e></list>", null,
                Type.of("List<Object>"));
        r2bRejectedOnAllBackends("<list><e type=\"not.approved.X\" isNull=\"true\"></e></list>", null, Type.of("List<Object>"));
        r2bRejectedOnAllBackends("<array><e type=\"not.approved.X\">1</e></array>", null, Type.of("Object[]"));
        for (final String entry : new String[] { "<entry type=\"not.approved.X\"><key>a</key><value>1</value></entry>",
                "<entry><key type=\"not.approved.X\">a</key><value>1</value></entry>",
                "<entry><key type=\"not.approved.X\"><" + childName + "><name>k</name></" + childName + "></key><value>1</value></entry>",
                "<entry><key type=\"not.approved.X\" isNull=\"true\"></key><value>1</value></entry>",
                "<entry><key>a</key><value type=\"not.approved.X\">1</value></entry>",
                "<entry><key>a</key><value type=\"not.approved.X\"><" + childName + "><name>v</name></" + childName + "></value></entry>",
                "<entry><key>a</key><value type=\"not.approved.X\" isNull=\"true\"></value></entry>",
                "<entry><key>a</key><value>1</value></entry><entry><key>b</key><value type=\"not.approved.X\">2</value></entry>" }) {
            r2bRejectedOnAllBackends("<map>" + entry + "</map>", null, openMapType);
            r2bRejectedOnAllBackends("<map>" + entry + "</map>", null, Type.of("Map<String, Integer>"));
        }
        r2bRejectedOnAllBackends("<map><entry><key>k1</key><value type=\"not.approved.X\">1</value></entry></map>", valueTypes, stringMapType);
        r2bRejectedOnAllBackends("<map><entry><key>k2</key><value type=\"not.approved.X\"><list><e>4</e></list></value></entry></map>", valueTypes,
                stringMapType);
        r2bRejectedOnAllBackends("<map><entry><key>k4</key><value type=\"not.approved.X\"><list><e>4</e></list></value></entry></map>", valueTypes,
                stringMapType);
        for (final XmlParserType parserType : new XmlParserType[] { XmlParserType.DOM, XmlParserType.StAX }) {
            final XmlParser parser = parserOf(parserType);
            assertEquals(R2B_REJECTED,
                    assertThrows(ParsingException.class,
                            () -> parser.deserialize("<list><e><thing><a type=\"Integer\">1</a><c type=\"not.approved.X\">3</c></thing></e></list>", null,
                                    Type.of("List<Object>"))).getMessage(),
                    parserType.name());
            assertEquals(R2B_REJECTED,
                    assertThrows(ParsingException.class,
                            () -> parser.deserialize("<list><e><thing><d type=\"not.approved.X\"><inner><x>4</x></inner></d></thing></e></list>", null,
                                    Type.of("List<Object>"))).getMessage(),
                    parserType.name());
        }
        r2bRejectedOnAllBackends(typed.replace("<" + typed.substring(1, typed.indexOf(' ')) + " type=\"" + R2BHolder.class.getCanonicalName() + "\">",
                "<" + typed.substring(1, typed.indexOf(' ')) + " type=\"not.approved.X\">"), null, holderType);
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

    // R2B-01: the Abacus StAX reader skips refineSlot for a scalar, bean or Object property (refineSlot hands such a schema back
    // unchanged, see AbstractXmlParser.isRefinableSlot) and still refines container, array and wrapper slots. Pins every slot kind
    // of the bean-property path (JSON-text and child-element form, null markers, empty text, the writer's output with and without
    // type attributes) with and without global element / map types and per-key value types, and the same slots as map entries,
    // equal on the StAX, DOM and SAX backends and through the DOM Node overload.
    @Test
    public void testReadByStreamParser_scalarAndBeanSlotsSkipRefineSlot_pins() {
        final XmlParser writer = parserOf(XmlParserType.StAX);
        final String childC = writer.serialize(r2bSlotChild("c"));
        final String childW = writer.serialize(r2bSlotChild("w"));
        assertTrue(childC.contains("<name>c</name>"), childC);

        final R2BSlotBean populated = new R2BSlotBean();
        populated.setText("5");
        populated.setNumber(7);
        populated.setOpen(9);
        populated.setOpenList(new java.util.ArrayList<>(List.of(1, "two", 3L)));
        populated.setOpenArray(new Object[] { 3, "x" });
        final Map<String, Object> openMap = new LinkedHashMap<>();
        openMap.put("k", 4);
        openMap.put("s", "v");
        populated.setOpenMap(openMap);
        populated.setWrappedList(java.util.Optional.of(new java.util.ArrayList<>(List.of(6))));
        populated.setChild(r2bSlotChild("c"));
        populated.setTypedList(new java.util.ArrayList<>(List.of(8L)));
        populated.setWrappedChild(java.util.Optional.of(r2bSlotChild("w")));
        populated.setTypedArray(new Integer[] { 10, 11 });
        final String compact = writer.serialize(populated);
        final String typed = writer.serialize(populated, XmlSerConfig.create().setWriteTypeInfo(true));
        final String rootTag = compact.substring(1, compact.indexOf('>'));
        final Type<R2BSlotBean> beanType = Type.of(R2BSlotBean.class);

        final String body = "<text>5</text><number>7</number><open>9</open><openList>[1, 2]</openList><openArray>[3]</openArray>"
                + "<openMap>{\"k\": 4}</openMap><wrappedList>[6]</wrappedList><child>" + childC + "</child><typedList>[8]</typedList><wrappedChild>" + childW
                + "</wrappedChild><typedArray>[10, 11]</typedArray>";
        final String beanXml = "<" + rootTag + ">" + body + "</" + rootTag + ">";

        for (final boolean global : new boolean[] { false, true }) {
            for (final XmlDeserConfig config : new XmlDeserConfig[] { global ? r2bGlobal(XmlDeserConfig.create()) : null,
                    global ? r2bGlobal(r2bPerKey()) : r2bPerKey() }) {
                final R2BSlotBean bean = r2bSameOnAllBackends(beanXml, config, beanType);
                assertEquals("5", bean.getText());
                assertEquals(7, bean.getNumber());
                assertEquals("9", bean.getOpen());
                assertEquals(global ? List.of(1L, 2L) : List.of(1, 2), bean.getOpenList());
                assertArrayEquals(global ? new Object[] { 3L } : new Object[] { 3 }, bean.getOpenArray());
                assertEquals(global ? Map.of("k", 4L) : Map.of("k", 4), bean.getOpenMap());
                assertEquals(java.util.Optional.of(global ? List.of(6L) : List.of(6)), bean.getWrappedList());
                assertEquals(r2bSlotChild("c"), bean.getChild());
                assertEquals(List.of(8L), bean.getTypedList());
                assertEquals(java.util.Optional.of(r2bSlotChild("w")), bean.getWrappedChild());
                assertArrayEquals(new Integer[] { 10, 11 }, bean.getTypedArray());
            }
        }

        // the writer's own output: child-element items of an open list are text without type attributes and typed with them
        final R2BSlotBean fromCompact = r2bSameOnAllBackends(compact, null, beanType);
        assertEquals("5", fromCompact.getText());
        assertEquals(7, fromCompact.getNumber());
        assertEquals("9", fromCompact.getOpen());
        assertEquals(List.of("1", "two", "3"), fromCompact.getOpenList());
        assertArrayEquals(new Object[] { "3", "x" }, fromCompact.getOpenArray());
        assertEquals(Map.of("k", "4", "s", "v"), fromCompact.getOpenMap());
        assertEquals(java.util.Optional.of(List.of(6)), fromCompact.getWrappedList());
        assertEquals(r2bSlotChild("c"), fromCompact.getChild());
        assertEquals(List.of(8L), fromCompact.getTypedList());
        assertEquals(java.util.Optional.of(r2bSlotChild("w")), fromCompact.getWrappedChild());
        assertArrayEquals(new Integer[] { 10, 11 }, fromCompact.getTypedArray());
        final R2BSlotBean fromTyped = r2bSameOnAllBackends(typed, null, beanType);
        assertEquals("9", fromTyped.getOpen());
        assertEquals(List.of(1, "two", 3L), fromTyped.getOpenList());
        assertArrayEquals(new Object[] { 3, "x" }, fromTyped.getOpenArray());
        assertEquals(Map.of("k", 4, "s", "v"), fromTyped.getOpenMap());
        assertEquals(java.util.Optional.of(List.of(6)), fromTyped.getWrappedList());
        assertEquals(r2bSlotChild("c"), fromTyped.getChild());
        assertEquals(List.of(8L), fromTyped.getTypedList());
        assertEquals(java.util.Optional.of(r2bSlotChild("w")), fromTyped.getWrappedChild());
        assertArrayEquals(new Integer[] { 10, 11 }, fromTyped.getTypedArray());

        // null markers and empty text on scalar, Object, container, bean and wrapper slots
        final String nulls = "<" + rootTag
                + "><text isNull=\"true\"></text><number>7</number><open isNull=\"true\"></open><openList isNull=\"true\"></openList>"
                + "<child isNull=\"true\"></child><wrappedChild isNull=\"true\"></wrappedChild><typedList>[]</typedList></" + rootTag + ">";
        final R2BSlotBean fromNulls = r2bSameOnAllBackends(nulls, null, beanType);
        assertNull(fromNulls.getText());
        assertEquals(7, fromNulls.getNumber());
        assertNull(fromNulls.getOpen());
        assertNull(fromNulls.getOpenList());
        assertNull(fromNulls.getChild());
        assertEquals(java.util.Optional.empty(), fromNulls.getWrappedChild());
        assertEquals(List.of(), fromNulls.getTypedList());
        assertNull(fromNulls.getTypedArray());
        assertNull(fromNulls.getWrappedList());
        final String empty = "<" + rootTag
                + "><text></text><open></open><openList></openList><openArray></openArray><openMap></openMap><typedList></typedList></" + rootTag + ">";
        final R2BSlotBean fromEmpty = r2bSameOnAllBackends(empty, null, beanType);
        assertEquals("", fromEmpty.getText());
        assertEquals(0, fromEmpty.getNumber());
        assertEquals("", fromEmpty.getOpen());
        assertNull(fromEmpty.getOpenList());
        assertNull(fromEmpty.getOpenArray());
        assertNull(fromEmpty.getOpenMap());
        assertNull(fromEmpty.getTypedList());

        // the same slots as map entries: text without configuration, per-key value types, and a global map value type that
        // cannot hold container text
        final String mapXml = "<map><entry><key>text</key><value>5</value></entry><entry><key>number</key><value>7</value></entry>"
                + "<entry><key>open</key><value>9</value></entry><entry><key>openList</key><value>[1, 2]</value></entry>"
                + "<entry><key>openArray</key><value>[3]</value></entry><entry><key>openMap</key><value>{\"k\": 4}</value></entry>"
                + "<entry><key>wrappedList</key><value>[6]</value></entry><entry><key>child</key><value>" + childC + "</value></entry>"
                + "<entry><key>typedList</key><value>[8]</value></entry></map>";
        final Type<Map<String, Object>> mapType = Type.of("Map<String, Object>");
        final Map<String, Object> plain = r2bSameOnAllBackends(mapXml, null, mapType);
        assertEquals("5", plain.get("text"));
        assertEquals("7", plain.get("number"));
        assertEquals("9", plain.get("open"));
        assertEquals("[1, 2]", plain.get("openList"));
        assertEquals("[3]", plain.get("openArray"));
        assertEquals("{\"k\": 4}", plain.get("openMap"));
        assertEquals("[6]", plain.get("wrappedList"));
        assertEquals(Map.of("name", "c"), plain.get("child"));
        assertEquals("[8]", plain.get("typedList"));
        for (final XmlParserType parserType : ALL_TYPES) {
            final XmlParser parser = parserOf(parserType);
            final Map<String, Object> perKey = parser.deserialize(mapXml, r2bPerKey(), mapType);
            assertEquals("5", perKey.get("text"), parserType.name());
            assertEquals(7, perKey.get("number"), parserType.name());
            assertEquals("9", perKey.get("open"), parserType.name());
            assertEquals(List.of(1, 2), perKey.get("openList"), parserType.name());
            assertArrayEquals(new Object[] { 3 }, (Object[]) perKey.get("openArray"), parserType.name());
            assertEquals(Map.of("k", 4), perKey.get("openMap"), parserType.name());
            assertEquals(java.util.Optional.of(List.of(6)), perKey.get("wrappedList"), parserType.name());
            assertEquals(r2bSlotChild("c"), perKey.get("child"), parserType.name());
            assertEquals(List.of(8L), perKey.get("typedList"), parserType.name());
            assertThrows(NumberFormatException.class, () -> parser.deserialize(mapXml, r2bGlobal(XmlDeserConfig.create()), mapType), parserType.name());
        }
    }

    // R3-02: an explicit Object value-type override is an open slot of its own on every backend: a scalar attribute does not type
    // its text (the SAX reader adopted it), container metadata is adopted, and a nested element carrying an approved bean class
    // is read as that bean (the DOM reader read it as a map of its properties); without the override the scalar attribute types
    // the value, and a configured scalar type converts
    @Test
    public void testReadMapEntry_explicitObjectOverrideOnAllBackends() {
        final String childElement = parserOf(XmlParserType.StAX).serialize(r2bChild("n", 1));
        final String childName = childElement.substring(1, childElement.indexOf('>'));
        final String childClass = R2BChild.class.getCanonicalName();
        final Type<Map<String, Object>> stringMapType = Type.of("Map<String, Object>");
        final XmlDeserConfig override = XmlDeserConfig.create().setValueType("k", Type.of(Object.class));
        final XmlDeserConfig longValue = XmlDeserConfig.create().setValueType("k", Type.of(Long.class));
        final String beanChild = "<" + childName + "><name>n</name></" + childName + ">";

        for (final String root : new String[] { "<map type=\"HashMap&lt;String, Object&gt;\">", "<map>" }) {
            assertEquals(Map.of("k", "2"),
                    r2bSameOnAllBackends(root + "<entry><key>k</key><value type=\"Integer\">2</value></entry></map>", override, stringMapType), root);
            assertEquals(Map.of("k", "2"),
                    r2bSameOnAllBackends(root + "<entry><key>k</key><value type=\"Long\">2</value></entry></map>", override, stringMapType), root);
            assertEquals(Map.of("k", "2"), r2bSameOnAllBackends(root + "<entry><key>k</key><value>2</value></entry></map>", override, stringMapType), root);
            assertEquals(Map.of("k", List.of(1, 2)),
                    r2bSameOnAllBackends(root + "<entry><key>k</key><value type=\"List&lt;Integer&gt;\">[1, 2]</value></entry></map>", override, stringMapType),
                    root);
            assertEquals(Map.of("k", List.of(1)),
                    r2bSameOnAllBackends(root + "<entry><key>k</key><value type=\"List&lt;Integer&gt;\"><list><e>1</e></list></value></entry></map>", override,
                            stringMapType),
                    root);
            assertEquals(Map.of("k", r2bChild("n", null)), r2bSameOnAllBackends(
                    root + "<entry><key>k</key><value type=\"" + childClass + "\">" + beanChild + "</value></entry></map>", override, stringMapType), root);
            assertEquals(Map.of("k", Map.of("name", "n")),
                    r2bSameOnAllBackends(root + "<entry><key>k</key><value>" + beanChild + "</value></entry></map>", override, stringMapType), root);
            assertEquals(Map.of("k", 2), r2bSameOnAllBackends(root + "<entry><key>k</key><value type=\"Integer\">2</value></entry></map>", null, stringMapType),
                    root);
            assertEquals(Map.of("k", 2L),
                    r2bSameOnAllBackends(root + "<entry><key>k</key><value type=\"Integer\">2</value></entry></map>", longValue, stringMapType), root);
        }
    }

    // R3-01: a nested map KEY whose type attribute names a bean class is read as that bean on every backend, under a typed and an
    // untyped root (the DOM reader handed its nested reader the declared Object key type and read the bean as a map of its
    // properties); keys with container metadata, without metadata, with an Object attribute and text keys keep their results
    @Test
    public void testReadMapEntry_nestedKeyWithBeanAttributeAdoptedOnAllBackends() {
        final String childElement = parserOf(XmlParserType.StAX).serialize(r2bChild("k", 1));
        final String childName = childElement.substring(1, childElement.indexOf('>'));
        final String childClass = R2BChild.class.getCanonicalName();
        final Type<Map<Object, Object>> openMapType = Type.of("Map<Object, Object>");
        final String beanChild = "<" + childName + "><name>k</name><count>1</count></" + childName + ">";

        for (final String root : new String[] { "<map type=\"HashMap&lt;Object, Object&gt;\">", "<map>" }) {
            assertEquals(Map.of(r2bChild("k", 1), "v"), r2bSameOnAllBackends(
                    root + "<entry><key type=\"" + childClass + "\">" + beanChild + "</key><value>v</value></entry></map>", null, openMapType), root);
            assertEquals(Map.of(Map.of("name", "k", "count", "1"), "v"),
                    r2bSameOnAllBackends(root + "<entry><key>" + beanChild + "</key><value>v</value></entry></map>", null, openMapType), root);
            assertEquals(Map.of(Map.of("name", "k", "count", "1"), "v"),
                    r2bSameOnAllBackends(root + "<entry><key type=\"Object\">" + beanChild + "</key><value>v</value></entry></map>", null, openMapType), root);
            assertEquals(Map.of(List.of(1, 2), "v"),
                    r2bSameOnAllBackends(root + "<entry><key type=\"List&lt;Integer&gt;\"><list><e>1</e><e>2</e></list></key><value>v</value></entry></map>",
                            null, openMapType),
                    root);
            assertEquals(Map.of(List.of(1, 2), "v"),
                    r2bSameOnAllBackends(
                            root + "<entry><key type=\"ImmutableList&lt;Integer&gt;\"><list><e>1</e><e>2</e></list></key><value>v</value></entry></map>", null,
                            openMapType),
                    root);
            assertEquals(Map.of(Map.of("a", 1), "v"), r2bSameOnAllBackends(root
                    + "<entry><key type=\"HashMap&lt;String, Integer&gt;\"><map><entry><key>a</key><value>1</value></entry></map></key><value>v</value></entry></map>",
                    null, openMapType), root);
            assertEquals(Map.of(5L, "v"), r2bSameOnAllBackends(root + "<entry><key type=\"Long\">5</key><value>v</value></entry></map>", null, openMapType),
                    root);
            assertEquals(Map.of("5", "v"), r2bSameOnAllBackends(root + "<entry><key>5</key><value>v</value></entry></map>", null, openMapType), root);
            // the same bean class on a nested VALUE was already read as the bean everywhere
            assertEquals(Map.of("k", r2bChild("k", 1)), r2bSameOnAllBackends(
                    root + "<entry><key>k</key><value type=\"" + childClass + "\">" + beanChild + "</value></entry></map>", null, openMapType), root);
        }
    }

    // R2A-03: an attribute class that is not approved still fails on every backend, with and without type information; a property
    // declared as a concrete scalar (int, String) keeps its own codec whatever scalar the attribute names; an interface-declared
    // property is read by the Object handler and adopts the attribute like an Object property (a primitive name does not fit it).
    @Test
    public void testReadObjectProperty_rejectedAttributeAndConcreteScalarsPinned() {
        final String typed = r2aDocument(true);
        final String untyped = r2aDocument(false);

        for (final XmlParserType parserType : ALL_TYPES) {
            final XmlParser parser = parserOf(parserType);

            for (final String open : new String[] { "<open type=\"not.approved.X\">5</open>", "<open type=\"java.lang.Runtime\">5</open>" }) {
                assertThrows(ParsingException.class, () -> parser.deserialize(typed.replace(R2A_OPEN, open), R2AHolder.class), parserType.name());
                assertThrows(ParsingException.class, () -> parser.deserialize(untyped.replace(R2A_OPEN_UNTYPED, open), R2AHolder.class), parserType.name());
            }

            for (final String count : new String[] { "<count type=\"Long\">7</count>", "<count type=\"String\">7</count>" }) {
                assertEquals(7, parser.deserialize(typed.replace("<count type=\"int\">1</count>", count), R2AHolder.class).getCount(), parserType.name());
            }

            for (final String text : new String[] { "<text type=\"Long\">7</text>", "<text type=\"Integer\">7</text>" }) {
                assertEquals("7", parser.deserialize(typed.replace("<text type=\"String\">t</text>", text), R2AHolder.class).getText(), parserType.name());
            }
        }

        final R2AInterfaceHolder holder = new R2AInterfaceHolder();
        holder.setPlain("s");
        final String typedInterface = r2aParserOf(XmlParserType.StAX).serialize(holder, XmlSerConfig.create().setWriteTypeInfo(true));
        final String plainTag = "<plain type=\"java.io.Serializable\">s</plain>";
        assertTrue(typedInterface.contains(plainTag), typedInterface);

        for (final XmlParserType parserType : ALL_TYPES) {
            final XmlParser parser = r2aParserOf(parserType);

            assertEquals("s", parser.deserialize(typedInterface, R2AInterfaceHolder.class).getPlain(), parserType.name());
            assertEquals(7L, parser.deserialize(typedInterface.replace(plainTag, "<plain type=\"Long\">7</plain>"), R2AInterfaceHolder.class).getPlain(),
                    parserType.name());
            assertEquals(7, parser.deserialize(typedInterface.replace(plainTag, "<plain type=\"Integer\">7</plain>"), R2AInterfaceHolder.class).getPlain(),
                    parserType.name());
            assertEquals("7", parser.deserialize(typedInterface.replace(plainTag, "<plain type=\"int\">7</plain>"), R2AInterfaceHolder.class).getPlain(),
                    parserType.name());
        }
    }

    // R2A-01: with type information honoured, an approved scalar type attribute on an Object property names the value's type on every
    // backend (RED on base for StAX and SAX, which read the declared Object's text); text the adopted type cannot read fails on every
    // backend as it failed on the DOM reader; the properties beside the adopted one keep their own codecs.
    @Test
    public void testReadObjectProperty_scalarTypeAttributeAdoptedOnAllBackends() {
        final String typed = r2aDocument(true);

        r2aAssertOpen(5L, typed.replace(R2A_OPEN, "<open type=\"Long\">5</open>"));
        r2aAssertOpen(5, typed.replace(R2A_OPEN, "<open type=\"Integer\">5</open>"));
        r2aAssertOpen(5.0d, typed.replace(R2A_OPEN, "<open type=\"Double\">5</open>"));
        r2aAssertOpen(Boolean.TRUE, typed.replace(R2A_OPEN, "<open type=\"Boolean\">true</open>"));
        r2aAssertOpen("5", typed.replace(R2A_OPEN, "<open type=\"String\">5</open>"));
        r2aAssertOpen('c', typed.replace(R2A_OPEN, "<open type=\"Character\">c</open>"));
        r2aAssertOpen(new java.math.BigDecimal("1.50"), typed.replace(R2A_OPEN, "<open type=\"BigDecimal\">1.50</open>"));
        r2aAssertOpen(java.time.LocalDate.of(2024, 1, 2), typed.replace(R2A_OPEN, "<open type=\"LocalDate\">2024-01-02</open>"));
        r2aAssertOpen(new java.util.Date(1700000000000L), typed.replace(R2A_OPEN, "<open type=\"java.util.Date\">1700000000000</open>"));
        // an empty typed element without a null marker is the adopted type's value of empty text: null for Long
        r2aAssertOpen(null, typed.replace(R2A_OPEN, "<open type=\"Long\"></open>"));

        for (final XmlParserType parserType : ALL_TYPES) {
            final XmlParser parser = parserOf(parserType);

            assertThrows(NumberFormatException.class, () -> parser.deserialize(typed.replace(R2A_OPEN, "<open type=\"Long\">x</open>"), R2AHolder.class),
                    parserType.name());
            assertThrows(NumberFormatException.class, () -> parser.deserialize(typed.replace(R2A_OPEN, "<open type=\"Long\"> 5 </open>"), R2AHolder.class),
                    parserType.name());

            final R2AHolder back = parser.deserialize(typed.replace(R2A_OPEN, "<open type=\"Long\">5</open>"), R2AHolder.class);
            assertEquals(5L, back.getOpen(), parserType.name());
            assertEquals(1, back.getCount(), parserType.name());
            assertEquals("t", back.getText(), parserType.name());
        }
    }

    // R2A-02: the shapes the DOM reader does not adopt stay the declared Object's text on every backend: a primitive name, the Object
    // name, no or a blank attribute, and any scalar attribute while type information is ignored (root without a type attribute); a
    // null marker wins over the attribute and any text; container metadata is still adopted as before.
    @Test
    public void testReadObjectProperty_unadoptedScalarAttributeShapesPinned() {
        final String typed = r2aDocument(true);

        r2aAssertOpen("5", typed.replace(R2A_OPEN, "<open type=\"int\">5</open>"));
        r2aAssertOpen("5", typed.replace(R2A_OPEN, "<open type=\"long\">5</open>"));
        r2aAssertOpen("5", typed.replace(R2A_OPEN, "<open type=\"Object\">5</open>"));
        r2aAssertOpen("5", typed.replace(R2A_OPEN, "<open>5</open>"));
        r2aAssertOpen("5", typed.replace(R2A_OPEN, "<open type=\"\">5</open>"));
        r2aAssertOpen(null, typed.replace(R2A_OPEN, "<open type=\"Long\" isNull=\"true\"></open>"));
        r2aAssertOpen(null, typed.replace(R2A_OPEN, "<open type=\"Long\" isNull=\"true\">5</open>"));
        r2aAssertOpen(new java.util.ArrayList<>(List.of(1, 2)), typed.replace(R2A_OPEN, "<open type=\"List&lt;Integer&gt;\">[1, 2]</open>"));

        final String untyped = r2aDocument(false);

        r2aAssertOpen("5", untyped.replace(R2A_OPEN_UNTYPED, "<open type=\"Long\">5</open>"));
        r2aAssertOpen("5", untyped.replace(R2A_OPEN_UNTYPED, "<open type=\"Integer\">5</open>"));
        r2aAssertOpen("true", untyped.replace(R2A_OPEN_UNTYPED, "<open type=\"Boolean\">true</open>"));
        r2aAssertOpen(null, untyped.replace(R2A_OPEN_UNTYPED, "<open type=\"Long\" isNull=\"true\"></open>"));
    }

    // G006-02: a StAX item/key/value wrapper keeps a single text fragment without copying it; joined fragments stay the same.
    @Test
    public void testReadWrappedValue_textFragmentsNullsAndMixedContent() {
        final XmlParser parser = parserOf(XmlParserType.StAX);
        final Type<List<String>> listType = Type.of("List<String>");
        final Type<Map<String, Integer>> mapType = Type.of("Map<String, Integer>");

        final List<String> list = parser
                .deserialize("<list><e>a<![CDATA[b]]>c</e><e>x&amp;y</e><e></e><e/><e>  </e><e isNull=\"true\">q</e><e>plain</e></list>", listType);
        assertEquals(Arrays.asList("abc", "x&y", "", "", "  ", null, "plain"), list);

        final Map<String, Integer> map = parser
                .deserialize("<map><entry><key>k<![CDATA[1]]></key><value>1<![CDATA[2]]>3</value></entry><entry><key>k2</key><value>7</value></entry>"
                        + "<entry><key>k3</key><value isNull=\"true\"/></entry></map>", mapType);
        final Map<String, Integer> expected = new LinkedHashMap<>();
        expected.put("k1", 123);
        expected.put("k2", 7);
        expected.put("k3", null);
        assertEquals(expected, map);

        final String[] array = parser.deserialize("<array><e>p<![CDATA[q]]></e><e>r</e></array>", String[].class);
        assertArrayEquals(new String[] { "pq", "r" }, array);

        // whitespace (one or several fragments) before a nested value is structural
        final Type<List<List<String>>> nestedType = Type.of("List<List<String>>");
        assertEquals(Arrays.asList(Arrays.asList("1", "2")),
                parser.deserialize("<list><e> <![CDATA[ ]]>\n<list><e>1</e><e>2</e></list></e></list>", nestedType));
        assertEquals(Arrays.asList(Arrays.asList("3")), parser.deserialize("<list><e>\n  <list><e>3</e></list>\n</e></list>", nestedType));

        // non-blank text before a nested value is rejected, whether it came in one fragment or several
        for (final String xml : new String[] { "<list><e>t<list><e>1</e></list></e></list>", "<list><e> <![CDATA[t]]><list><e>1</e></list></e></list>" }) {
            final ParsingException ex = assertThrows(ParsingException.class, () -> parser.deserialize(xml, nestedType), xml);
            assertEquals("Mixed scalar and nested XML values are not supported", ex.getMessage());
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

    // P4-12: Dataset is documented as unsupported.
    @Test
    public void reviewFixes20260906_datasetIsUnsupported() {
        final Dataset dataset = Dataset.rows(N.asList("c1", "c2"), new Object[][] { { 1, null }, { null, "x" } });
        final XmlParser parser = parserOf(XmlParserType.StAX);

        assertThrows(ParsingException.class, () -> parser.serialize(dataset));
        assertThrows(ParsingException.class, () -> parser.serialize(N.asMap("d", dataset)));

        assertThrows(ParsingException.class, () -> parser.serialize(dataset, XmlSerConfig.create().setFailOnEmptyBean(false)));
    }

    @Test
    public void reviewFixes20260906_failOnEmptyBeanAllowsPropertylessObjects() {
        final EmptyHolder holder = new EmptyHolder();
        holder.setE(new Empty());
        holder.setS("x");
        for (XmlParserType type : ALL_TYPES) {
            for (Object value : new Object[] { new Empty(), N.asMap("e", new Empty()), holder, N.asList(new Empty()) }) {
                assertThrows(ParsingException.class, () -> parserOf(type).serialize(value));
                assertTrue(!parserOf(type).serialize(value, XmlSerConfig.create().setFailOnEmptyBean(false)).isEmpty());
            }
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

    @Test
    public void reviewFixes20260906_nulCharPropertyIsRejected() {
        final CharBean bean = new CharBean();
        bean.setI(7);
        bean.setBoxed((char) 0);
        for (XmlParserType type : ALL_TYPES) {
            assertThrows(ParsingException.class, () -> parserOf(type).serialize(bean));
        }
        bean.setBoxed(null);
        final String omitted = parserOf(XmlParserType.StAX).serialize(bean, XmlSerConfig.create().setExclusion(Exclusion.DEFAULT));
        assertTrue(!omitted.contains("<c>"), omitted);
        bean.setC('x');
        bean.setBoxed('y');
        for (XmlParserType type : ALL_TYPES) {
            final CharBean back = parserOf(type).deserialize(parserOf(type).serialize(bean), CharBean.class);
            assertEquals('x', back.getC());
            assertEquals(Character.valueOf('y'), back.getBoxed());
        }
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
        assertThrows(ParsingException.class, () -> parserOf(XmlParserType.StAX).serialize(present));
        present.setN(u.Nullable.empty());
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
                assertEquals(u.Nullable.empty(), back.getN(), type + " " + presentXml); // Empty Nullable remains empty.
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

            assertEquals(0, parserOf(type).deserialize("<array>\n  </array>", Object[].class).length, type.toString());
            assertEquals(0, parserOf(type).deserialize("<array> </array>", int[].class).length, type.toString());
            assertEquals(0, parserOf(type).deserialize("<array>\n</array>", String[].class).length, type.toString());
            assertEquals(0, parserOf(type).deserialize("<array>\n</array>", Person[].class).length, type.toString());
            assertEquals(0, parserOf(type).deserialize("<array></array>", Object[].class).length, type.toString());

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
        for (final XmlParser parser : new XmlParser[] { staxParser, domParser, new AbacusXmlParserImpl(XmlParserType.SAX, null, null, approvedTypes()) }) {
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
                new AbacusXmlParserImpl(XmlParserType.SAX, null, null, approvedTypes()).deserialize(xml, List.class);
            } catch (final Throwable t) {
                thrown[0] = t;
            }
        });
        caller.start();
        caller.join();

        assertTrue(thrown[0] instanceof ParsingException, "expected ParsingException, got " + thrown[0]);
        // Any child of <list> is an item wrapper (as in StAX/DOM), so the unresolvable node is its child <a>.
        assertTrue(thrown[0].getMessage().contains("node name: a "), thrown[0].getMessage());

        // the StAX and DOM readers, which have no such frames, already reported it this way
        for (final XmlParserType type : new XmlParserType[] { XmlParserType.StAX, XmlParserType.DOM }) {
            assertThrows(ParsingException.class, () -> new AbacusXmlParserImpl(type, null, null, approvedTypes()).deserialize(xml, List.class),
                    type.toString());
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
            assertThrows(ParsingException.class, () -> parser.deserialize("<map><pair><key>a</key><value>1</value></pair></map>", Map.class), type.toString());
            assertThrows(ParsingException.class, () -> parser.deserialize("<map><foo>a</foo></map>", Map.class), type.toString());
            assertThrows(ParsingException.class, () -> parser.deserialize("<map><entry><key>a</key><value>1</value></entry><foo/></map>", Map.class),
                    type.toString());

            // the well-formed shapes are untouched, including the namespace-prefixed one
            assertEquals(Map.of("a", "1"), parser.deserialize("<map><entry><key>a</key><value>1</value></entry></map>", Map.class), type.toString());
            assertEquals(Map.of("a", "1"),
                    parser.deserialize("<p:map xmlns:p=\"urn:x\"><p:entry><p:key>a</p:key><p:value>1</p:value></p:entry></p:map>", Map.class), type.toString());
        }
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

    @Test
    public void testRootGenericTypesPreserveExplicitConfigAndNestedValues() {
        for (final XmlParserType parserType : new XmlParserType[] { XmlParserType.StAX, XmlParserType.DOM, XmlParserType.SAX }) {
            final XmlParser parser = new AbacusXmlParserImpl(parserType, null, null, approvedTypes());
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
    public void testRootMapUsesGenericTargetKeyAndValueTypes() {
        final Type<Map<DayOfWeek, Integer>> targetType = Type.of(new TypeReference<Map<DayOfWeek, Integer>>() {
        });
        final String xml = "<map><entry><key>MONDAY</key><value>7</value></entry><entry><key>FRIDAY</key><value>9</value></entry></map>";
        for (final XmlParserType parserType : new XmlParserType[] { XmlParserType.StAX, XmlParserType.DOM, XmlParserType.SAX }) {
            assertEquals(Map.of(DayOfWeek.MONDAY, 7, DayOfWeek.FRIDAY, 9),
                    new AbacusXmlParserImpl(parserType, null, null, approvedTypes()).deserialize(xml, null, targetType));
        }
    }

    // G149: reset() clears the per-parse SAX state a failure can leave behind - the ignored-entry, entry and type-info flags -
    // so a pooled handler starts clean. (A reuse through the parser cannot observe it: the cleared ignored-element count alone
    // already gates the entry flags.)
    @Test
    public void testSaxHandlerResetClearsEntryAndTypeInfoState() throws Exception {
        final AbacusXmlParserImpl.XmlSAXHandler<Object> handler = new AbacusXmlParserImpl.XmlSAXHandler<>();
        final Class<?> cls = handler.getClass();
        final String[] flags = { "inIgnoredEntry", "ignoredEntryValueSeen", "checkedPropNameTag", "checkedTypeInfo", "isTagByPropertyName", "ignoreTypeInfo",
                "typeAttributeChecked" };

        for (final String flag : flags) {
            final java.lang.reflect.Field field = cls.getDeclaredField(flag);
            field.setAccessible(true);
            field.setBoolean(handler, true);
        }

        final java.lang.reflect.Field depth = cls.getDeclaredField("typeInfoDecisionDepth");
        depth.setAccessible(true);
        depth.setInt(handler, 3);
        final java.lang.reflect.Field keyMarks = cls.getDeclaredField("entryKeyMarkQueue");
        keyMarks.setAccessible(true);
        ((List<Integer>) keyMarks.get(handler)).add(1);
        final java.lang.reflect.Field valueSeen = cls.getDeclaredField("entryValueSeenQueue");
        valueSeen.setAccessible(true);
        ((List<Boolean>) valueSeen.get(handler)).add(Boolean.TRUE);

        final java.lang.reflect.Method reset = cls.getDeclaredMethod("reset");
        reset.setAccessible(true);
        reset.invoke(handler);

        for (final String flag : flags) {
            final java.lang.reflect.Field field = cls.getDeclaredField(flag);
            field.setAccessible(true);
            org.junit.jupiter.api.Assertions.assertFalse(field.getBoolean(handler), flag);
        }

        assertEquals(-1, depth.getInt(handler));
        assertTrue(((List<?>) keyMarks.get(handler)).isEmpty());
        assertTrue(((List<?>) valueSeen.get(handler)).isEmpty());
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
        final XmlParser saxParser = new AbacusXmlParserImpl(XmlParserType.SAX, null, null, approvedTypes());
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
    public void testSaxNestedImmutableRecord() {
        final RecordWrapper outer = new RecordWrapper();
        outer.setName("OUTER");
        outer.setRec(new RecordB(7, "first", "last"));
        final String xml = staxParser.serialize(outer);
        final XmlDeserConfig xdc = new XmlDeserConfig();
        final RecordWrapper fromSax = new AbacusXmlParserImpl(XmlParserType.SAX, null, null, approvedTypes()).deserialize(xml, xdc, RecordWrapper.class);
        assertEquals(new RecordB(7, "first", "last"), fromSax.getRec());
        assertEquals("OUTER", fromSax.getName());
        assertEquals(staxParser.deserialize(xml, xdc, RecordWrapper.class).getRec(), fromSax.getRec());
        assertEquals(domParser.deserialize(xml, xdc, RecordWrapper.class).getRec(), fromSax.getRec());
    }

    // G06-01: the SAX reader resolves an element's type attribute once per element (concrete class, item type and the
    // final approval check share it). Pins the values read and the rejections, which must match the StAX and DOM readers.
    @Test
    public void testSaxStartElement_typeAttributeResolvedOncePinned() {
        final XmlParser sax = parserOf(XmlParserType.SAX);
        final XmlParser stax = parserOf(XmlParserType.StAX);
        final XmlParser dom = parserOf(XmlParserType.DOM);

        final G06TypedBean bean = new G06TypedBean();
        bean.setId(7L);
        bean.setName("n");
        bean.setValue(g06Child("v", 1));
        bean.setChild(g06Child("c", 2));
        bean.setItems(new java.util.ArrayList<>(Arrays.asList(1, "two", g06Child("three", 3), null, 4.5d)));
        final Map<String, Object> props = new LinkedHashMap<>();
        props.put("a", 1);
        props.put("b", g06Child("b", 2));
        props.put("c", null);
        bean.setProps(props);
        final Map<Object, Object> keyed = new LinkedHashMap<>();
        keyed.put(1, "one");
        keyed.put("two", 2L);
        bean.setKeyed(keyed);

        for (final XmlSerConfig xsc : new XmlSerConfig[] { XmlSerConfig.create().setWriteTypeInfo(true), XmlSerConfig.create() }) {
            final String xml = sax.serialize(bean, xsc);
            final G06TypedBean fromSax = sax.deserialize(xml, G06TypedBean.class);

            assertEquals(stax.deserialize(xml, G06TypedBean.class), fromSax, xml);
            assertEquals(dom.deserialize(xml, G06TypedBean.class), fromSax, xml);

            if (xsc.isWriteTypeInfo()) {
                assertEquals(bean, fromSax, xml);
            }
        }

        // A blank attribute is no metadata: the declared types read the text.
        final G06TypedBean blank = sax.deserialize("<g06TypedBean><id type=\"  \">5</id><name type=\" \">n</name></g06TypedBean>", G06TypedBean.class);
        assertEquals(5L, blank.getId());
        assertEquals("n", blank.getName());

        // A disallowed attribute is rejected at every site that resolves it: property, bean in a typed slot, null-marked
        // bean in an Object slot, item, key, value and bean in an Object slot.
        final String notAllowed = "XML type attribute is not allowed: not.approved.Type";

        for (final String rejected : new String[] { "<g06TypedBean><name type=\"not.approved.Type\">x</name></g06TypedBean>",
                "<g06TypedBean><child><g06Child type=\"not.approved.Type\"><label>x</label></g06Child></child></g06TypedBean>",
                "<g06TypedBean><value><g06Child isNull=\"true\" type=\"not.approved.Type\" /></value></g06TypedBean>",
                "<g06TypedBean><items><list><e type=\"not.approved.Type\">1</e></list></items></g06TypedBean>",
                "<g06TypedBean><keyed><map><entry><key type=\"not.approved.Type\">1</key><value>v</value></entry></map></keyed></g06TypedBean>",
                "<g06TypedBean><keyed><map><entry><key>1</key><value type=\"not.approved.Type\">v</value></entry></map></keyed></g06TypedBean>",
                "<g06TypedBean><value><g06Child type=\"not.approved.Type\"><label>x</label></g06Child></value></g06TypedBean>" }) {
            final ParsingException e = assertThrows(ParsingException.class, () -> sax.deserialize(rejected, G06TypedBean.class), rejected);
            assertEquals(notAllowed, e.getMessage(), rejected);
            assertThrows(ParsingException.class, () -> stax.deserialize(rejected, G06TypedBean.class), rejected);
            assertThrows(ParsingException.class, () -> dom.deserialize(rejected, G06TypedBean.class), rejected);
        }

        // An unknown property, ignored by default, is skipped before its attribute is looked at; when unknown properties
        // are rejected, the rejection names the property, still ahead of its attribute.
        final String unknownTyped = "<g06TypedBean><unknown type=\"not.approved.Type\">1</unknown><name>n</name></g06TypedBean>";
        assertEquals("n", sax.deserialize(unknownTyped, G06TypedBean.class).getName());
        assertEquals("n", sax.deserialize(unknownTyped, XmlDeserConfig.create().setIgnoreUnmatchedProperty(true), G06TypedBean.class).getName());
        final ParsingException unknown = assertThrows(ParsingException.class,
                () -> sax.deserialize(unknownTyped, XmlDeserConfig.create().setIgnoreUnmatchedProperty(false), G06TypedBean.class));
        assertTrue(unknown.getMessage().startsWith("Unknown property element: unknown"), unknown.getMessage());
    }

    @Test
    public void testSaxValueWrapperTrackingSkipsIgnoredContentAndResetsAfterFailure() {
        final XmlParser parser = parserOf(XmlParserType.SAX);
        final String duplicate = "<family><friend><person/><person/></friend></family>";
        final XmlDeserConfig ignoredFriend = XmlDeserConfig.create().setIgnoredPropNames(Family.class, Set.of("friend"));
        final XmlDeserConfig ignoredKey = XmlDeserConfig.create().setIgnoredPropNames(Map.class, Set.of("skip"));

        for (int i = 0; i < 3; i++) {
            assertThrows(ParsingException.class, () -> parser.deserialize(duplicate, Family.class));

            // Skipped subtrees never push frames; their closing events must not pop tracked frames either.
            final Family ignored = parser.deserialize("<family><friend><person/><person/></friend><age>5</age></family>", ignoredFriend, Family.class);
            assertNull(ignored.getFriend());
            assertEquals(5, ignored.getAge());
            final Family unmatched = parser.deserialize("<family><unknown><person/><person/></unknown><name>ok</name></family>",
                    XmlDeserConfig.create().setIgnoreUnmatchedProperty(true), Family.class);
            assertEquals("ok", unmatched.getName());
            assertEquals(Map.of("keep", "ok"),
                    parser.deserialize(
                            "<map><entry><key>skip</key><value><list/><list/></value></entry>" + "<entry><key>keep</key><value>ok</value></entry></map>",
                            ignoredKey, Map.class));

            assertNull(parser.deserialize("<family isNull=\"true\"><friend><person/><person/></friend></family>", Family.class));
            final Family valid = parser.deserialize("<family><friend isNull=\"true\"/><tags><list><e>a</e><e>b</e></list></tags></family>", Family.class);
            assertNull(valid.getFriend());
            assertEquals(List.of("a", "b"), valid.getTags());
        }
    }

    // serialize

    @Test
    public void testConstructorAndSerialize() {
        assertNotNull(new AbacusXmlParserImpl(XmlParserType.StAX, null, null, approvedTypes()));
        assertNotNull(new AbacusXmlParserImpl(XmlParserType.StAX, new XmlSerConfig(), new XmlDeserConfig()));
        assertNotNull(new AbacusXmlParserImpl(XmlParserType.StAX, null, null, approvedTypes()).serialize("test"));
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

        // A direct NUL Character has no XML representation; an empty element would lose the token.
        assertThrows(ParsingException.class, () -> parser.serialize(new Object[] { '\0' }));

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

    // G115-01: a Map.Entry value was written with unquoted String slots ({"a,b":c,d}) and its own output did not read back on any
    // backend; with writeTypeInfo its handler name became a rejected type attribute. It is now written as JSON text like Pair/Tuple.
    @Test
    public void testSerialize_mapEntryPropertyRoundTripsOnAllBackends() {
        final G115EntryCarrier bean = new G115EntryCarrier();
        bean.setEntry(new java.util.AbstractMap.SimpleEntry<>("a,b", "c,d"));
        bean.setOptionalEntry(java.util.Optional.of(new java.util.AbstractMap.SimpleEntry<>("k", "v,w")));
        final Map<String, Map.Entry<String, String>> entries = new LinkedHashMap<>(Map.of("e", new java.util.AbstractMap.SimpleEntry<>("a b", "c,d")));
        final Type<Map<String, Map.Entry<String, String>>> entriesType = Type.of("Map<String, Map.Entry<String, String>>");

        for (final XmlParserType parserType : ALL_TYPES) {
            final AbacusXmlParserImpl parser = new AbacusXmlParserImpl(parserType);

            for (final XmlSerConfig config : new XmlSerConfig[] { XmlSerConfig.create(), XmlSerConfig.create().setWriteTypeInfo(true) }) {
                final String xml = parser.serialize(bean, config);
                final G115EntryCarrier back = parser.deserialize(xml, G115EntryCarrier.class);

                assertEquals(Map.entry("a,b", "c,d"), back.getEntry(), parserType + " " + xml);
                assertEquals(java.util.Optional.of(Map.entry("k", "v,w")), back.getOptionalEntry(), parserType + " " + xml);

                // a typed root map is read without type information here (a root type attribute replaces the declared root type)
                final String entriesXml = parser.serialize(entries, config.isWriteTypeInfo() ? XmlSerConfig.create() : config);
                assertEquals(Map.of("e", Map.entry("a b", "c,d")), parser.deserialize(entriesXml, entriesType), parserType + " " + entriesXml);
            }
        }
    }

    // G006-03, map-key site: a repeated object used as a map KEY is written as an empty <key></key> in pretty format too, and every
    // backend reads the pretty form back like the compact form.
    @Test
    public void testSerialize_prettyCircularReferenceAsMapKeyWritesEmptyKey() {
        final G115KeyLoop bean = new G115KeyLoop();
        bean.setAttrs(new LinkedHashMap<>());
        bean.getAttrs().put(bean, "v");

        final XmlSerConfig compact = new XmlSerConfig().setCircularReferenceSupported(true);
        final XmlSerConfig pretty = new XmlSerConfig().setCircularReferenceSupported(true).setPrettyFormat(true);

        for (final XmlParserType parserType : XmlParserType.values()) {
            final AbacusXmlParserImpl parser = new AbacusXmlParserImpl(parserType);
            final String prettyXml = parser.serialize(bean, pretty);
            assertTrue(prettyXml.contains("<key></key>"), prettyXml);

            final G115KeyLoop fromCompact = parser.deserialize(parser.serialize(bean, compact), G115KeyLoop.class);
            final G115KeyLoop fromPretty = parser.deserialize(prettyXml, G115KeyLoop.class);

            assertEquals(fromCompact.getAttrs(), fromPretty.getAttrs(), parserType.name());
            assertEquals(1, fromPretty.getAttrs().size(), parserType.name());
        }
    }

    // G006-03: with circularReferenceSupported a repeated object is written as an empty element, but pretty format put the
    // closing line break + indentation into it, so an Object-typed value read back as that whitespace (compact reads "").
    @Test
    public void testSerialize_prettyCircularReferenceWritesEmptyElement() {
        final G006Loop bean = new G006Loop();
        bean.setName("n");
        bean.setSelf(bean);
        bean.setAttrs(new LinkedHashMap<>());
        bean.getAttrs().put("loop", bean.getAttrs());
        bean.getAttrs().put("k", "v");
        bean.setItems(new java.util.ArrayList<>());
        bean.getItems().add(bean.getItems());
        bean.getItems().add("x");
        final Object[] array = new Object[2];
        array[0] = array;
        array[1] = "y";
        bean.setArray(array);

        final XmlSerConfig compact = new XmlSerConfig().setCircularReferenceSupported(true);
        final XmlSerConfig pretty = new XmlSerConfig().setCircularReferenceSupported(true).setPrettyFormat(true);

        for (final XmlParserType parserType : XmlParserType.values()) {
            final AbacusXmlParserImpl parser = new AbacusXmlParserImpl(parserType);
            final String prettyXml = parser.serialize(bean, pretty);
            assertTrue(prettyXml.contains("<self></self>"), prettyXml);

            final G006Loop fromCompact = parser.deserialize(parser.serialize(bean, compact), G006Loop.class);
            final G006Loop fromPretty = parser.deserialize(prettyXml, G006Loop.class);

            assertEquals(fromCompact.getSelf(), fromPretty.getSelf(), parserType.name());
            assertEquals(fromCompact.getAttrs(), fromPretty.getAttrs(), parserType.name());
            assertEquals(fromCompact.getItems(), fromPretty.getItems(), parserType.name());
            assertEquals(N.deepToString((Object[]) fromCompact.getArray()), N.deepToString((Object[]) fromPretty.getArray()), parserType.name());
            assertEquals("v", fromPretty.getAttrs().get("k"), parserType.name());
        }
    }

    // pretty format: an Object property reads back on every backend exactly as the compact form does, for scalars
    // (including surrounding blanks and the empty String) and for structured values alike.
    @Test
    public void testSerialize_prettyObjectPropertyReadsLikeCompactOnAllBackends() {
        final Object[] values = { " a b ", "", 5, true, DayOfWeek.MONDAY, Arrays.asList("p", "q"), N.asMap("k", "v") };

        for (final boolean tagByPropertyName : new boolean[] { true, false }) {
            for (final Object value : values) {
                final StrBean bean = new StrBean();
                bean.setName("n");
                bean.setAny(value);

                final String compactXml = parserOf(XmlParserType.StAX).serialize(bean, XmlSerConfig.create().setTagByPropertyName(tagByPropertyName));
                final String prettyXml = parserOf(XmlParserType.StAX).serialize(bean,
                        XmlSerConfig.create().setPrettyFormat(true).setTagByPropertyName(tagByPropertyName));

                for (final XmlParserType parserType : ALL_TYPES) {
                    final Object expected = parserOf(parserType).deserialize(compactXml, StrBean.class).getAny();
                    assertEquals(expected, parserOf(parserType).deserialize(prettyXml, StrBean.class).getAny(), parserType + " " + prettyXml);
                }
            }
        }
    }

    // G114-03: with prettyFormat an Object-declared property holding a scalar got the closing line break and
    // indentation inside its element (<any>abc\n    </any>), so it read back as "abc\n    ".
    @Test
    public void testSerialize_prettyObjectPropertyScalarHasNoTrailingWhitespace() {
        final StrBean bean = new StrBean();
        bean.setName("n");
        bean.setAny("abc");

        for (final XmlSerConfig xsc : new XmlSerConfig[] { XmlSerConfig.create().setPrettyFormat(true),
                XmlSerConfig.create().setPrettyFormat(true).setTagByPropertyName(false) }) {
            final String xml = parserOf(XmlParserType.StAX).serialize(bean, xsc);
            assertTrue(xml.contains(">abc</"), xml);

            for (final XmlParserType parserType : ALL_TYPES) {
                assertEquals("abc", parserOf(parserType).deserialize(xml, StrBean.class).getAny(), parserType + " " + xml);
            }
        }

        // a structured value held by the Object property keeps its closing indentation
        bean.setAny(N.asMap("k", "v"));
        final String mapXml = parserOf(XmlParserType.StAX).serialize(bean, XmlSerConfig.create().setPrettyFormat(true));
        assertTrue(mapXml.contains("</map>\n"), mapXml);
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
    public void testSharedReferenceIsNotMisdetectedAsCycle() {
        final Map<String, Object> shared = N.asMap("name", "x");
        final Map<String, Object> root = new LinkedHashMap<>();
        root.put("a1", shared);
        root.put("a2", shared);
        final String xml = new AbacusXmlParserImpl(XmlParserType.StAX, null, null, approvedTypes()).serialize(root,
                new XmlSerConfig().setCircularReferenceSupported(true));
        final int first = xml.indexOf("name");
        final int second = xml.indexOf("name", first + 1);
        assertTrue(first >= 0 && second > first, xml);
    }

    @Test
    public void testSingleValueWrappersAllowNestedContainersAndSiblingItems() {
        final String nestedLists = "<list><e><list><e><list><e>1</e><e>2</e></list></e><e><list/></e></list></e>"
                + "<e isNull=\"true\"/><e><list><e><list><e>3</e></list></e></list></e></list>";
        final String structuredKeysAndValues = "<map><entry><key><list><e>a</e><e>b</e></list></key>" + "<value><list><e>1</e><e>2</e></list></value></entry>"
                + "<entry><key><list/></key><value><list/></value></entry></map>";

        for (final XmlParserType type : ALL_TYPES) {
            final XmlParser parser = parserOf(type);
            final List<List<List<Integer>>> lists = parser.deserialize(nestedLists, null, Type.of("List<List<List<Integer>>>"));
            assertEquals(Arrays.asList(List.of(List.of(1, 2), List.of()), null, List.of(List.of(3))), lists, type.toString());

            final Map<List<String>, List<Integer>> map = parser.deserialize(structuredKeysAndValues, null, Type.of("Map<List<String>, List<Integer>>"));
            assertEquals(Map.of(List.of("a", "b"), List.of(1, 2), List.of(), List.of()), map, type.toString());

            final int[][] arrays = parser.deserialize("<array><e><array><e>1</e><e>2</e></array></e><e><array/></e></array>", int[][].class);
            assertEquals(2, arrays.length, type.toString());
            assertArrayEquals(new int[] { 1, 2 }, arrays[0], type.toString());
            assertArrayEquals(new int[0], arrays[1], type.toString());
        }
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
    public void testStaxDocumentWithoutRootElementReportsParsingException() {
        assertThrows(ParsingException.class, () -> staxParser.deserialize("<?xml version=\"1.0\"?>", Map.class));
    }

    @Test
    public void testStaxEmptyCdataFragmentDoesNotLeakIntoNextPropertyText() {
        final XmlParser parser = new AbacusXmlParserImpl(XmlParserType.StAX);
        final String xml = "<bean><keep>abc<![CDATA[]]></keep><drop1>x<![CDATA[y]]></drop1><drop2>p<!--c-->q</drop2><after>z</after></bean>";

        final AbacusXmlParserImplTest.IgnInner bean = parser.deserialize(xml, AbacusXmlParserImplTest.IgnInner.class);

        assertEquals("abc", bean.getKeep());
        assertEquals("xy", bean.getDrop1());
        assertEquals("pq", bean.getDrop2());
        assertEquals("z", bean.getAfter());

        // The DOM reader agrees.
        final AbacusXmlParserImplTest.IgnInner viaDom = new AbacusXmlParserImpl(XmlParserType.DOM).deserialize(xml, AbacusXmlParserImplTest.IgnInner.class);
        assertEquals("xy", viaDom.getDrop1());
        assertEquals("pq", viaDom.getDrop2());
    }

    @Test
    public void testTypeApprovalIsResetAfterObjectSlotBean() {
        final String prefix = "<map><entry><key>shape</key><value><g142Circle type=\"" + G142Circle.class.getCanonicalName()
                + "\"><radius>1</radius></g142Circle></value></entry>";
        final String suffix = "<key>later</key><value>ok</value></entry></map>";
        final String valid = prefix + "<entry type=\"String\">" + suffix;
        final String invalid = prefix + "<entry type=\"not.approved.Type\">" + suffix;

        for (final XmlParserType parserType : ALL_TYPES) {
            final AbacusXmlParserImpl parser = new AbacusXmlParserImpl(parserType, null, null, approvedTypes());
            final Map<?, ?> result = parser.deserialize(valid, Map.class);
            assertEquals(1, org.junit.jupiter.api.Assertions.assertInstanceOf(G142Circle.class, result.get("shape")).getRadius());
            assertEquals("ok", result.get("later"));
            // Without SAX's per-element reset, the previous bean's successful approval skips the later entry's check.
            assertThrows(ParsingException.class, () -> parser.deserialize(invalid, Map.class), parserType.name());
            assertEquals("ok", parser.deserialize(valid, Map.class).get("later"), parserType.name());
        }
    }

    // G04-01: writeArray indexes an Object[] directly instead of calling the reflective Array.get per element; the output of
    // every array shape (nulls, wrappers, tuple-likes, nested arrays, beans, primitive arrays, cycles) is pinned on base and fix.
    @Test
    public void testWriteArray_objectArrayShapesPinned() {
        final XmlParser parser = new AbacusXmlParserImpl(XmlParserType.StAX);
        final XmlSerConfig typed = new XmlSerConfig().setWriteTypeInfo(true);
        final XmlSerConfig pretty = new XmlSerConfig().setPrettyFormat(true);
        final XmlSerConfig both = new XmlSerConfig().setWriteTypeInfo(true).setPrettyFormat(true);

        final String[] strings = { "a", null, "b<c&d>" };
        assertEquals("<array><e>a</e><e isNull=\"true\" /><e>b&lt;c&amp;d&gt;</e></array>", parser.serialize(strings));
        assertEquals("<array type=\"String[]\"><e type=\"String\">a</e><e isNull=\"true\" /><e type=\"String\">b&lt;c&amp;d&gt;</e></array>",
                parser.serialize(strings, typed));
        assertEquals(g04Lines("<array>", "    <e>a</e>", "    <e isNull=\"true\" />", "    <e>b&lt;c&amp;d&gt;</e>", "</array>"),
                parser.serialize(strings, pretty));
        assertEquals(g04Lines("<array type=\"String[]\">", "    <e type=\"String\">a</e>", "    <e isNull=\"true\" />",
                "    <e type=\"String\">b&lt;c&amp;d&gt;</e>", "</array>"), parser.serialize(strings, both));

        final Integer[] integers = { 1, null, -2 };
        assertEquals("<array><e>1</e><e isNull=\"true\" /><e>-2</e></array>", parser.serialize(integers));
        assertEquals("<array type=\"Integer[]\"><e type=\"Integer\">1</e><e isNull=\"true\" /><e type=\"Integer\">-2</e></array>",
                parser.serialize(integers, typed));

        assertEquals("<array></array>", parser.serialize(new Object[0]));
        assertEquals("<array type=\"Object[]\"></array>", parser.serialize(new Object[0], typed));
        assertEquals("<array>\n</array>", parser.serialize(new Object[0], pretty));
        assertEquals("<array type=\"Object[]\">\n</array>", parser.serialize(new Object[0], both));
        assertEquals("<array><e isNull=\"true\" /></array>", parser.serialize(new Object[] { null }));
        assertEquals("<array>\n    <e isNull=\"true\" />\n</array>", parser.serialize(new Object[] { null }, pretty));

        final Object[] wrappers = { u.Optional.of("x"), u.Optional.empty(), java.util.Optional.of(7), u.OptionalInt.empty(), u.Nullable.of("y") };
        assertEquals("<array><e>x</e><e isNull=\"true\" /><e>7</e><e isNull=\"true\" /><e>y</e></array>", parser.serialize(wrappers));
        assertEquals("<array type=\"Object[]\"><e type=\"String\">x</e><e isNull=\"true\" /><e type=\"Integer\">7</e><e isNull=\"true\" />"
                + "<e type=\"String\">y</e></array>", parser.serialize(wrappers, typed));

        final Object[] mixed = { Pair.of("p", 1), N.asList(1, 2), new String[] { "n1", "n2" }, new int[] { 3, 4 }, N.asMap("k", "v") };
        assertEquals("<array><e>[&quot;p&quot;, 1]</e><e><list><e>1</e><e>2</e></list></e><e>[&quot;n1&quot;, &quot;n2&quot;]</e><e>[3, 4]</e>"
                + "<e><map><entry><key>k</key><value>v</value></entry></map></e></array>", parser.serialize(mixed));
        assertEquals("<array type=\"Object[]\"><e>[&quot;p&quot;, 1]</e><e type=\"ImmutableList&lt;Object&gt;\">"
                + "<list type=\"ImmutableList&lt;Object&gt;\"><e type=\"Integer\">1</e><e type=\"Integer\">2</e></list></e>"
                + "<e type=\"String[]\">[&quot;n1&quot;, &quot;n2&quot;]</e><e type=\"int[]\">[3, 4]</e>"
                + "<e type=\"ImmutableMap&lt;Object, Object&gt;\"><map type=\"ImmutableMap&lt;Object, Object&gt;\">"
                + "<entry><key type=\"String\">k</key><value type=\"String\">v</value></entry></map></e></array>", parser.serialize(mixed, typed));
        assertEquals(g04Lines("<array>", "    <e>[&quot;p&quot;, 1]</e>", "    <e>", "        <list>", "            <e>1</e>", "            <e>2</e>",
                "        </list>", "    </e>", "    <e>[&quot;n1&quot;, &quot;n2&quot;]</e>", "    <e>[3, 4]</e>", "    <e>", "        <map>",
                "            <entry>", "                <key>k</key>", "                <value>v</value>", "            </entry>", "        </map>", "    </e>",
                "</array>"), parser.serialize(mixed, pretty));

        final ItemBean[] beans = { new ItemBean("bob", 3), null };
        assertEquals("<array><e><itemBean><name>bob</name><age>3</age></itemBean></e><e isNull=\"true\" /></array>", parser.serialize(beans));
        assertEquals(
                "<array type=\"com.landawn.abacus.parser.AbacusXmlParserImplTest.ItemBean[]\"><e type=\"com.landawn.abacus.parser.AbacusXmlParserImplTest.ItemBean\">"
                        + "<itemBean type=\"com.landawn.abacus.parser.AbacusXmlParserImplTest.ItemBean\"><name type=\"String\">bob</name><age type=\"int\">3</age></itemBean></e>"
                        + "<e isNull=\"true\" /></array>",
                parser.serialize(beans, typed));
        assertEquals(g04Lines("<array>", "    <e>", "        <itemBean>", "            <name>bob</name>", "            <age>3</age>", "        </itemBean>",
                "    </e>", "    <e isNull=\"true\" />", "</array>"), parser.serialize(beans, pretty));

        final Object[] nested = { new Object[] { "deep", new Object[] { 1 } } };
        assertEquals("<array><e><array><e>deep</e><e><array><e>1</e></array></e></array></e></array>", parser.serialize(nested));
        assertEquals("<array type=\"Object[]\"><e type=\"Object[]\"><array type=\"Object[]\"><e type=\"String\">deep</e><e type=\"Object[]\">"
                + "<array type=\"Object[]\"><e type=\"Integer\">1</e></array></e></array></e></array>", parser.serialize(nested, typed));
        assertEquals(
                g04Lines("<array>", "    <e>", "        <array>", "            <e>deep</e>", "            <e>", "                <array>",
                        "                    <e>1</e>", "                </array>", "            </e>", "        </array>", "    </e>", "</array>"),
                parser.serialize(nested, pretty));

        assertEquals("<array><e>c</e><e>&amp;</e></array>", parser.serialize(new Character[] { 'c', '&' }));
        assertEquals("<array type=\"Character[]\"><e type=\"Character\">c</e><e type=\"Character\">&amp;</e></array>",
                parser.serialize(new Character[] { 'c', '&' }, typed));
        assertEquals("<array><e>1.5</e><e>-0.0</e><e>NaN</e></array>", parser.serialize(new Double[] { 1.5, -0.0, Double.NaN }));
        assertEquals("<array type=\"Double[]\"><e type=\"Double\">1.5</e><e type=\"Double\">-0.0</e><e type=\"Double\">NaN</e></array>",
                parser.serialize(new Double[] { 1.5, -0.0, Double.NaN }, typed));

        // Primitive arrays never reach writeArray: text form at the root and as an element.
        assertEquals("[1, 2]", parser.serialize(new int[] { 1, 2 }));
        assertEquals("<array type=\"Object[]\"><e type=\"int[]\">[1, 2]</e><e type=\"long[]\">[]</e></array>",
                parser.serialize(new Object[] { new int[] { 1, 2 }, new long[0] }, typed));

        // Arrays nested in a collection element and in a map value.
        final ItemBean[] inContainer = { new ItemBean("in", 1) };
        assertEquals("<list><e><array><e><itemBean><name>in</name><age>1</age></itemBean></e></array></e></list>",
                parser.serialize(N.asList((Object) inContainer)));
        assertEquals(
                g04Lines("<map>", "    <entry>", "        <key>k</key>", "        <value>", "            <array>", "                <e>",
                        "                    <itemBean>", "                        <name>in</name>", "                        <age>1</age>",
                        "                    </itemBean>", "                </e>", "            </array>", "        </value>", "    </entry>", "</map>"),
                parser.serialize(N.asMap("k", inContainer), pretty));

        // Self reference: an empty element with circular-reference support, the nesting-depth failure without it;
        // a shared (non-circular) element is written twice either way.
        final XmlSerConfig circular = new XmlSerConfig().setCircularReferenceSupported(true);
        final Object[] self = new Object[3];
        self[0] = "x";
        self[1] = self;
        self[2] = new Object[] { self, "y" };
        assertEquals("<array><e>x</e><e></e><e><array><e></e><e>y</e></array></e></array>", parser.serialize(self, circular));
        assertEquals(
                g04Lines("<array>", "    <e>x</e>", "    <e></e>", "    <e>", "        <array>", "            <e></e>", "            <e>y</e>",
                        "        </array>", "    </e>", "</array>"),
                parser.serialize(self, new XmlSerConfig().setCircularReferenceSupported(true).setPrettyFormat(true)));
        final ParsingException cycle = assertThrows(ParsingException.class, () -> parser.serialize(self));
        assertTrue(cycle.getMessage().startsWith("Serialization nesting depth exceeded 256 while writing [Ljava.lang.Object;"), cycle.getMessage());
        final Object[] shared = { "s" };
        assertEquals("<array><e><array><e>s</e></array></e><e><array><e>s</e></array></e></array>",
                parser.serialize(new Object[] { shared, shared }, circular));
        assertEquals("<array><e><array><e>s</e></array></e><e><array><e>s</e></array></e></array>", parser.serialize(new Object[] { shared, shared }));
    }

    // G11-01: two property-less classes written alternately each keep their own element name
    @Test
    public void testWriteEmptyObject_distinctClassesKeepTheirOwnTags() {
        final XmlSerConfig lenient = XmlSerConfig.create().setFailOnEmptyBean(false);

        for (final XmlParserType type : ALL_TYPES) {
            final XmlParser parser = parserOf(type);
            final String label = type.toString();
            for (int round = 0; round < 3; round++) {
                assertEquals("<perfReviewMarker></perfReviewMarker>", parser.serialize(new PerfReviewMarker(), lenient), label);
                assertEquals("<perfReviewOtherMarker></perfReviewOtherMarker>", parser.serialize(new PerfReviewOtherMarker(), lenient), label);
                assertEquals("<list><e><perfReviewMarker></perfReviewMarker></e><e><perfReviewOtherMarker></perfReviewOtherMarker></e></list>",
                        parser.serialize(N.asList(new PerfReviewMarker(), new PerfReviewOtherMarker()), lenient), label);
            }
        }
    }

    // G11-01: an anonymous class has an empty simple name: the element-name check fails on every write (a cached tag
    // never suppresses the failure), while the attribute style still writes the empty name as before
    @Test
    public void testWriteEmptyObject_invalidNameIsRejectedOnEveryWrite() {
        final XmlSerConfig lenient = XmlSerConfig.create().setFailOnEmptyBean(false);
        final XmlSerConfig lenientAttributeStyle = XmlSerConfig.create().setFailOnEmptyBean(false).setTagByPropertyName(false);
        final Object anonymous = new Object() {
        };

        for (final XmlParserType type : ALL_TYPES) {
            final XmlParser parser = parserOf(type);
            final String label = type.toString();
            for (int round = 0; round < 3; round++) {
                final ParsingException ex = assertThrows(ParsingException.class, () -> parser.serialize(anonymous, lenient), label);
                assertEquals("Class name '' is not a valid XML element name", ex.getMessage(), label);
                final ParsingException nested = assertThrows(ParsingException.class, () -> parser.serialize(N.asList(anonymous), lenient), label);
                assertEquals("Class name '' is not a valid XML element name", nested.getMessage(), label);
                assertEquals("<bean name=\"\"></bean>", parser.serialize(anonymous, lenientAttributeStyle), label);
            }
        }
    }

    // G006-01: the property description is built only on failure; the messages stay exactly the same.
    @Test
    public void testWriteProperties_illegalCharacterMessagesArePinned() {
        final XmlParser parser = parserOf(XmlParserType.StAX);
        final String expectedName = "Property 'name' contains U+0001, which cannot be represented in XML 1.0";
        final String expectedAny = "Property 'any' contains U+0001, which cannot be represented in XML 1.0";

        final StrBean named = new StrBean();
        named.setName("a\u0001b");
        assertEquals(expectedName, assertThrows(ParsingException.class, () -> parser.serialize(named)).getMessage());

        final StrBean anyString = new StrBean();
        anyString.setAny("a\u0001b");
        assertEquals(expectedAny, assertThrows(ParsingException.class, () -> parser.serialize(anyString)).getMessage());

        final StrBean anyBuilder = new StrBean();
        anyBuilder.setAny(new StringBuilder("a\u0001b"));
        assertEquals(expectedAny, assertThrows(ParsingException.class, () -> parser.serialize(anyBuilder)).getMessage());

        final StrBean anyOptional = new StrBean();
        anyOptional.setAny(u.Optional.of(u.Optional.of("a\u0001b")));
        assertEquals(expectedAny, assertThrows(ParsingException.class, () -> parser.serialize(anyOptional)).getMessage());

        final OptBean optional = new OptBean();
        optional.setOs(u.Optional.of("a\u0001b"));
        assertEquals("Property 'os' contains U+0001, which cannot be represented in XML 1.0",
                assertThrows(ParsingException.class, () -> parser.serialize(optional)).getMessage());

        final OptBean jdkOptional = new OptBean();
        jdkOptional.setJos(java.util.Optional.of("\uFFFF"));
        assertEquals("Property 'jos' contains U+FFFF, which cannot be represented in XML 1.0",
                assertThrows(ParsingException.class, () -> parser.serialize(jdkOptional)).getMessage());

        // the non-property descriptions are unchanged
        assertEquals("Map value contains U+0001, which cannot be represented in XML 1.0",
                assertThrows(ParsingException.class, () -> parser.serialize(N.asMap("k", "a\u0001b"))).getMessage());

        // legal values are written exactly as before
        final StrBean legal = new StrBean();
        legal.setName("x&<y>");
        legal.setAny(u.Optional.of("z"));
        assertEquals("<strBean><name>x&amp;&lt;y&gt;</name><any>z</any></strBean>", parser.serialize(legal));
    }

    // G04-03: writeProperties builds the indentation of nested structured properties lazily (never for a bean of scalars) and
    // G04-04: it reuses the BeanInfo writeBean looked up; pretty output of scalar-only, nested, custom-indentation, typed,
    // container-nested and circular beans is pinned on base and fix.
    @Test
    public void testWriteProperties_prettyNestedIndentationPinned() {
        final XmlParser parser = new AbacusXmlParserImpl(XmlParserType.StAX);
        final XmlSerConfig pretty = new XmlSerConfig().setPrettyFormat(true);
        final XmlSerConfig prettyTab = new XmlSerConfig().setPrettyFormat(true).setIndentation("\t");
        final XmlSerConfig both = new XmlSerConfig().setWriteTypeInfo(true).setPrettyFormat(true);

        final OuterBean scalarsOnly = new OuterBean();
        scalarsOnly.setFirst("f");
        scalarsOnly.setLast("l");
        final String scalarsPretty = g04Lines("<outerBean>", "    <first>f</first>", "    <last>l</last>", "</outerBean>");
        assertEquals(scalarsPretty, parser.serialize(scalarsOnly, pretty));
        assertEquals(g04Lines("<outerBean>", "\t<first>f</first>", "\t<last>l</last>", "</outerBean>"), parser.serialize(scalarsOnly, prettyTab));
        assertEquals("<outerBean><first>f</first><last>l</last></outerBean>", parser.serialize(scalarsOnly));

        final OuterBean full = new OuterBean();
        full.setFirst("f");
        full.setNested(new ItemBean("n", 1));
        full.setBeans(N.asList(new ItemBean("b1", 2), new ItemBean("b2", 3)));
        full.setBeanMap(N.asMap("k", new ItemBean("m", 4)));
        full.setNumbers(N.asList(5, 6));
        full.setLast("l");
        final String fullPretty = g04Lines("<outerBean>", "    <first>f</first>", "    <nested>", "        <itemBean>", "            <name>n</name>",
                "            <age>1</age>", "        </itemBean>", "    </nested>", "    <beans>", "        <list>", "            <e>",
                "                <itemBean>", "                    <name>b1</name>", "                    <age>2</age>", "                </itemBean>",
                "            </e>", "            <e>", "                <itemBean>", "                    <name>b2</name>", "                    <age>3</age>",
                "                </itemBean>", "            </e>", "        </list>", "    </beans>", "    <beanMap>", "        <map>", "            <entry>",
                "                <key>k</key>", "                <value>", "                    <itemBean>", "                        <name>m</name>",
                "                        <age>4</age>", "                    </itemBean>", "                </value>", "            </entry>", "        </map>",
                "    </beanMap>", "    <numbers>[5, 6]</numbers>", "    <last>l</last>", "</outerBean>");
        assertEquals(fullPretty, parser.serialize(full, pretty));
        assertEquals(fullPretty.replace("    ", "\t"), parser.serialize(full, prettyTab));
        assertEquals("<outerBean><first>f</first><nested><itemBean><name>n</name><age>1</age></itemBean></nested><beans><list>"
                + "<e><itemBean><name>b1</name><age>2</age></itemBean></e><e><itemBean><name>b2</name><age>3</age></itemBean></e></list></beans>"
                + "<beanMap><map><entry><key>k</key><value><itemBean><name>m</name><age>4</age></itemBean></value></entry></map></beanMap>"
                + "<numbers>[5, 6]</numbers><last>l</last></outerBean>", parser.serialize(full));
        assertEquals(g04Lines("<outerBean type=\"com.landawn.abacus.parser.AbacusXmlParserImplTest.OuterBean\">", "    <first type=\"String\">f</first>",
                "    <nested type=\"com.landawn.abacus.parser.AbacusXmlParserImplTest.ItemBean\">",
                "        <itemBean type=\"com.landawn.abacus.parser.AbacusXmlParserImplTest.ItemBean\">", "            <name type=\"String\">n</name>",
                "            <age type=\"int\">1</age>", "        </itemBean>", "    </nested>",
                "    <beans type=\"List&lt;com.landawn.abacus.parser.AbacusXmlParserImplTest.ItemBean&gt;\">",
                "        <list type=\"ImmutableList&lt;Object&gt;\">", "            <e type=\"com.landawn.abacus.parser.AbacusXmlParserImplTest.ItemBean\">",
                "                <itemBean type=\"com.landawn.abacus.parser.AbacusXmlParserImplTest.ItemBean\">",
                "                    <name type=\"String\">b1</name>", "                    <age type=\"int\">2</age>", "                </itemBean>",
                "            </e>", "            <e type=\"com.landawn.abacus.parser.AbacusXmlParserImplTest.ItemBean\">",
                "                <itemBean type=\"com.landawn.abacus.parser.AbacusXmlParserImplTest.ItemBean\">",
                "                    <name type=\"String\">b2</name>", "                    <age type=\"int\">3</age>", "                </itemBean>",
                "            </e>", "        </list>", "    </beans>",
                "    <beanMap type=\"Map&lt;String, com.landawn.abacus.parser.AbacusXmlParserImplTest.ItemBean&gt;\">",
                "        <map type=\"ImmutableMap&lt;Object, Object&gt;\">", "            <entry>", "                <key type=\"String\">k</key>",
                "                <value type=\"com.landawn.abacus.parser.AbacusXmlParserImplTest.ItemBean\">",
                "                    <itemBean type=\"com.landawn.abacus.parser.AbacusXmlParserImplTest.ItemBean\">",
                "                        <name type=\"String\">m</name>", "                        <age type=\"int\">4</age>",
                "                    </itemBean>", "                </value>", "            </entry>", "        </map>", "    </beanMap>",
                "    <numbers type=\"List&lt;Integer&gt;\">[5, 6]</numbers>", "    <last type=\"String\">l</last>", "</outerBean>"),
                parser.serialize(full, both));

        // The first structured property comes after scalars, and two entries share one nested indentation.
        final OuterBean structuredLater = new OuterBean();
        structuredLater.setFirst("f");
        structuredLater.setBeanMap(new LinkedHashMap<>(N.asMap("k", new ItemBean("m", 4), "k2", new ItemBean("m2", 5))));
        structuredLater.setLast("l");
        final String laterPretty = g04Lines("<outerBean>", "    <first>f</first>", "    <beanMap>", "        <map>", "            <entry>",
                "                <key>k</key>", "                <value>", "                    <itemBean>", "                        <name>m</name>",
                "                        <age>4</age>", "                    </itemBean>", "                </value>", "            </entry>",
                "            <entry>", "                <key>k2</key>", "                <value>", "                    <itemBean>",
                "                        <name>m2</name>", "                        <age>5</age>", "                    </itemBean>",
                "                </value>", "            </entry>", "        </map>", "    </beanMap>", "    <last>l</last>", "</outerBean>");
        assertEquals(laterPretty, parser.serialize(structuredLater, pretty));

        // Beans nested in a list and in an array get the enclosing element's indentation.
        assertEquals(g04Lines("<list>", "    <e>", g04Indent(scalarsPretty, "        "), "    </e>", "    <e>", g04Indent(fullPretty, "        "), "    </e>",
                "</list>"), parser.serialize(N.asList(scalarsOnly, full), pretty));
        assertEquals(g04Lines("<array>", "    <e>", g04Indent(scalarsPretty, "        "), "    </e>", "    <e>", g04Indent(laterPretty, "        "), "    </e>",
                "</array>"), parser.serialize(new OuterBean[] { scalarsOnly, structuredLater }, pretty));

        // Empty nested containers and a repeated (circular) list element keep their empty-element form.
        final OuterBean selfRef = new OuterBean();
        selfRef.setFirst("self");
        selfRef.setBeans(new java.util.ArrayList<>());
        selfRef.setBeanMap(new LinkedHashMap<>());
        final List<Object> cyclic = new java.util.ArrayList<>();
        cyclic.add(selfRef);
        cyclic.add(cyclic);
        assertEquals(
                g04Lines("<list>", "    <e>", "        <outerBean>", "            <first>self</first>", "            <beans>", "                <list>",
                        "                </list>", "            </beans>", "            <beanMap>", "                <map>", "                </map>",
                        "            </beanMap>", "        </outerBean>", "    </e>", "    <e></e>", "</list>"),
                parser.serialize(cyclic, new XmlSerConfig().setPrettyFormat(true).setCircularReferenceSupported(true)));
    }

    // Review fixes 2026-09-06 (P4-01..P4-12, P8-02, R-P04, P5-06, T6-02). Every case runs on StAX, DOM and SAX.
    private static final XmlParserType[] ALL_TYPES = { XmlParserType.StAX, XmlParserType.DOM, XmlParserType.SAX };

    private static XmlParser parserOf(final XmlParserType type) {
        return new AbacusXmlParserImpl(type, null, null, approvedTypes());
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

    // Self-review 2026-09-07 (R3): the two halves of the 2026-09-06 fixes that stopped short.
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

    // Fix pass 2026-09-08 (G01)

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

    @Data
    public static class G006NameBean {
        private String firstName;
        private String lastName;
    }

    @Data
    public static class G006Alpha {
        private String alphaName;
    }

    @Data
    public static class G006Omega {
        private String omegaName;
        private int omegaCount;
    }

    @Data
    public static class G006Holder {
        private List<G006Alpha> alphas;
        private Object other;
    }

    @Data
    public static class G006Carrier {
        private String carrierName;
        private Object payload;
    }

    @Data
    public static class G006Box {
        private List<G006Carrier> carriers;
    }

    private static G006Omega g006Omega() {
        final G006Omega omega = new G006Omega();
        omega.setOmegaName("o1");
        omega.setOmegaCount(7);
        return omega;
    }

    @Data
    public static class G006Loop {
        private String name;
        private Object self;
        private Map<String, Object> attrs;
        private List<Object> items;
        private Object array;
    }

    @Data
    public static class G115ContainerHolder {
        private List<Set<G006Alpha>> sets;
        private Object anyList;
        private List<G006Alpha[]> arrays;
        private Object anyArray;
    }

    // Plain bean (identity equals/hashCode) so it can be a key of its own map.
    public static class G115KeyLoop {
        private Map<Object, Object> attrs;

        public Map<Object, Object> getAttrs() {
            return attrs;
        }

        public void setAttrs(final Map<Object, Object> attrs) {
            this.attrs = attrs;
        }
    }

    @Data
    @NoArgsConstructor
    public static class G141Omega {
        private String omegaName;
    }

    @Data
    @NoArgsConstructor
    public static class G141Holder {
        private Object other;
        private Map<String, Object> values;
        private List<Object> items;
    }

    public interface G142Shape {
    }

    @Data
    @NoArgsConstructor
    public static class G142Circle implements G142Shape {
        private int radius;
    }

    @Data
    @NoArgsConstructor
    public static class G142Other {
        private int radius;
    }

    @Data
    @NoArgsConstructor
    public static class G142ShapeHolder {
        private G142Shape shape;
        private Map<String, Object> m;
    }

    private static String g142Outcome(final XmlParser parser, final String xml) {
        try {
            return String.valueOf(parser.deserialize(xml, G142ShapeHolder.class));
        } catch (final RuntimeException e) {
            return e.getClass().getSimpleName();
        }
    }

    private static String g144Outcome(final java.util.concurrent.Callable<Object> call) {
        try {
            return String.valueOf(call.call());
        } catch (final Exception e) {
            return e.getClass().getSimpleName();
        }
    }

    private static Set<Class<?>> g144Approved() {
        final Set<Class<?>> approved = new java.util.HashSet<>(approvedTypes());
        approved.add(testfixtures.entity.AccountContact.class);
        return approved;
    }

    private static String show(final Object value) {
        return value instanceof Object[] a ? Arrays.toString(a) : String.valueOf(value);
    }

    @Data
    public static class G115EntryCarrier {
        private Map.Entry<String, String> entry;
        private java.util.Optional<Map.Entry<String, String>> optionalEntry;
    }

    @Data
    public static class G115OptionalValueHolder {
        private Map<String, java.util.Optional<G006Alpha>> beans;
        private Map<String, u.Optional<List<G006Alpha>>> lists;
        private Map<String, u.Nullable<Map<String, G006Alpha>>> maps;
        private Map<String, java.util.Optional<List<String>>> strings;
        private Map<java.util.Optional<G006Alpha>, String> keys;
    }

    @Data
    public static class G115JdkOptionalValueHolder {
        private Map<String, java.util.Optional<G006Alpha>> beans;
        private Map<String, java.util.Optional<List<String>>> strings;
    }

    private static G006Alpha g115Alpha(final String name) {
        final G006Alpha alpha = new G006Alpha();
        alpha.setAlphaName(name);
        return alpha;
    }

    @Data
    public static class G115JdkOptionalScalarHolder {
        private Map<String, java.util.Optional<String>> optionals;
        private Map<String, java.util.OptionalLong> optionalLongs;
    }

    @Data
    public static class G119DurationHolder {
        private java.time.Duration duration;
        private Map<String, java.time.Duration> durations;
    }

    @Data
    public static class G119DeepOptionalHolder {
        private Map<String, java.util.Optional<Map<String, java.util.Optional<G006Alpha>>>> deep;
    }

    // A map subclass with another parameter layout than Map<K, V>: its key type is fixed, its only parameter is the value type.
    public static class G134StringKeyMap<V> extends HashMap<String, V> {
        private static final long serialVersionUID = 1L;
    }

    private static final String[] G134_WRAPPERS = { "java.util.Optional", "com.landawn.abacus.util.u.Optional", "com.landawn.abacus.util.u.Nullable" };

    // Wraps a value in the wrapper named by its type name (one of G134_WRAPPERS).
    private static Object g134Wrap(final String wrapper, final Object value) {
        return G134_WRAPPERS[0].equals(wrapper) ? java.util.Optional.of(value) : G134_WRAPPERS[1].equals(wrapper) ? u.Optional.of(value) : u.Nullable.of(value);
    }

    private static Map<String, Object> g134Map(final Object... keysAndValues) {
        final Map<String, Object> map = new LinkedHashMap<>();

        for (int i = 0; i < keysAndValues.length; i += 2) {
            map.put((String) keysAndValues[i], keysAndValues[i + 1]);
        }

        return map;
    }

    // mutable values: an immutable JDK container is written with its own class name, which no reader can instantiate
    private static List<Object> g134Values() {
        return List.of(g134Map("x", 1), new java.util.ArrayList<>(List.of(g134Map("x", 1))),
                new java.util.ArrayList<>(List.of(g134Map("x", 1), g134Map("y", 2))), g134Map("x", 1, "y", 2), 5, "s",
                new java.util.ArrayList<>(List.of(1, 2)));
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemBean {
        private String name;
        private int age;
    }

    @Data
    public static class OuterBean {
        private String first;
        private ItemBean nested;
        private List<ItemBean> beans;
        private Map<String, ItemBean> beanMap;
        private List<Integer> numbers;
        private String last;
    }

    private static String g04Lines(final String... lines) {
        return String.join("\n", lines);
    }

    private static String g04Indent(final String text, final String prefix) {
        return prefix + text.replace("\n", "\n" + prefix);
    }

    // Beans for the DOM type-attribute pins (G05-01): a property whose declared type is a bean, a container or Object reaches
    // getConcreteClass in the DOM bean reader; list and array items and the open map key/value fallbacks reach
    // resolveItemType(Node, Type). Both used to read and resolve the element's type attribute twice.
    @Data
    public static class G05Base {
        private String name;
    }

    @Data
    @lombok.EqualsAndHashCode(callSuper = true)
    public static class G05Sub extends G05Base {
        private int extra;
    }

    @Data
    public static class G05Holder {
        private G05Base child;
        private List<Integer> tags;
        private Map<String, Integer> counts;
        private Object open;
    }

    // Reads the document on the three backends: the DOM result must stay what the StAX and SAX backends read (value and class).
    private static <T> T g05SameOnAllBackends(final String xml, final Type<T> type) {
        final T expected = parserOf(XmlParserType.StAX).deserialize(xml, type);

        for (final XmlParserType parserType : ALL_TYPES) {
            final T actual = parserOf(parserType).deserialize(xml, type);

            if (expected != null && expected.getClass().isArray()) {
                assertArrayEquals((Object[]) expected, (Object[]) actual, parserType + " " + xml);
            } else {
                assertEquals(expected, actual, parserType + " " + xml);
            }

            assertEquals(expected == null ? null : expected.getClass(), actual == null ? null : actual.getClass(), parserType + " " + xml);
        }

        return expected;
    }

    @Data
    public static class G06Child {
        private String label;
        private Integer count;
    }

    @Data
    public static class G06TypedBean {
        private long id;
        private String name;
        private Object value;
        private G06Child child;
        private List<Object> items;
        private Map<String, Object> props;
        private Map<Object, Object> keyed;
    }

    private static G06Child g06Child(final String label, final int count) {
        final G06Child child = new G06Child();
        child.setLabel(label);
        child.setCount(count);
        return child;
    }

    /** G11-01: a property-less class (an ObjectType, not a bean) written by AbstractXmlParser.writeEmptyObject. */
    public static class PerfReviewMarker {
    }

    /** G11-01: a property-less class whose annotation selects the naming policy of its element name. */
    @com.landawn.abacus.annotation.JsonXmlConfig(namingPolicy = com.landawn.abacus.util.NamingPolicy.SNAKE_CASE)
    public static class PerfReviewAnnotatedMarker {
    }

    /** G11-01: a second property-less class, interleaved with the first one. */
    public static class PerfReviewOtherMarker {
    }

    /** G11-01: a bean holding property-less values. */
    public static class PerfReviewMarkerHolder {
        private PerfReviewMarker first;
        private PerfReviewMarker second;
        private String tag;

        public PerfReviewMarker getFirst() {
            return first;
        }

        public void setFirst(final PerfReviewMarker first) {
            this.first = first;
        }

        public PerfReviewMarker getSecond() {
            return second;
        }

        public void setSecond(final PerfReviewMarker second) {
            this.second = second;
        }

        public String getTag() {
            return tag;
        }

        public void setTag(final String tag) {
            this.tag = tag;
        }
    }

    // R2A (owner ruling on G05-02): a scalar type attribute on an Object bean property is adopted by the Abacus StAX and SAX readers
    // the way the DOM reader adopts it. The beans: an Object property beside concrete scalars, and an interface-declared property
    // (read by the Object handler, so the same rule applies to it).
    @Data
    public static class R2AHolder {
        private Object open;
        private int count;
        private String text;
    }

    @Data
    public static class R2AInterfaceHolder {
        private java.io.Serializable plain;
    }

    private static final String R2A_OPEN = "<open type=\"Object\">5</open>";

    private static final String R2A_OPEN_UNTYPED = "<open>5</open>";

    // The interface the writer names on the interface-declared property must be approved to read the typed document back.
    private static XmlParser r2aParserOf(final XmlParserType type) {
        final java.util.Set<Class<?>> approved = new java.util.HashSet<>(approvedTypes());
        approved.add(java.io.Serializable.class);
        return new AbacusXmlParserImpl(type, null, null, approved);
    }

    // The documents the writer produces for the holder: with type information (the DECLARED Object type on the property) and without.
    private static String r2aDocument(final boolean withTypeInfo) {
        final R2AHolder holder = new R2AHolder();
        holder.setOpen(5);
        holder.setCount(1);
        holder.setText("t");
        final String xml = withTypeInfo ? parserOf(XmlParserType.StAX).serialize(holder, XmlSerConfig.create().setWriteTypeInfo(true))
                : parserOf(XmlParserType.StAX).serialize(holder);
        assertTrue(withTypeInfo ? xml.contains(R2A_OPEN) && xml.contains("<count type=\"int\">1</count>") && xml.contains("<text type=\"String\">t</text>")
                : xml.contains(R2A_OPEN_UNTYPED) && xml.contains("<count>1</count>") && !xml.contains("type="), xml);
        return xml;
    }

    // Asserts the value AND the class of the open property on the three backends.
    private static void r2aAssertOpen(final Object expected, final String xml) {
        for (final XmlParserType parserType : ALL_TYPES) {
            final Object actual = parserOf(parserType).deserialize(xml, R2AHolder.class).getOpen();

            assertEquals(expected, actual, parserType + " " + xml);
            assertEquals(expected == null ? null : expected.getClass(), actual == null ? null : actual.getClass(), parserType + " " + xml);
        }
    }

    @Data
    public static class R2BSlotChild {
        private String name;
    }

    @Data
    public static class R2BSlotBean {
        private String text;
        private int number;
        private Object open;
        private List<Object> openList;
        private Object[] openArray;
        private Map<String, Object> openMap;
        private java.util.Optional<List<Object>> wrappedList;
        private R2BSlotChild child;
        private List<Long> typedList;
        private java.util.Optional<R2BSlotChild> wrappedChild;
        private Integer[] typedArray;
    }

    @Data
    public static class R2BChild {
        private String name;
        private Integer count;
    }

    @Data
    public static class R2BHolder {
        private int id;
        private String name;
        private Object open;
        private List<Integer> tags;
        private Map<String, Integer> counts;
        private R2BChild child;
        private List<Object> items;
        private Map<Object, Object> keyed;
        private Object[] arr;
        private List<Number> numbers;
        private java.util.Optional<Object> wrapped;
        @JsonXmlField(dateFormat = "yyyy-MM-dd")
        private java.util.Date when;
    }

    private static final String R2B_REJECTED = "XML type attribute is not allowed: not.approved.X";

    private static R2BSlotChild r2bSlotChild(final String name) {
        final R2BSlotChild child = new R2BSlotChild();
        child.setName(name);
        return child;
    }

    private static R2BChild r2bChild(final String name, final Integer count) {
        final R2BChild child = new R2BChild();
        child.setName(name);
        child.setCount(count);
        return child;
    }

    private static XmlDeserConfig r2bGlobal(final XmlDeserConfig config) {
        return config.setElementType(Type.of(Long.class)).setMapKeyType(Type.of(String.class)).setMapValueType(Type.of(Long.class));
    }

    private static XmlDeserConfig r2bPerKey() {
        return XmlDeserConfig.create()
                .setValueType("number", Type.of(Integer.class))
                .setValueType("openList", Type.of("List<Object>"))
                .setValueType("openArray", Type.of("Object[]"))
                .setValueType("openMap", Type.of("Map<String, Object>"))
                .setValueType("wrappedList", Type.of("java.util.Optional<List<Object>>"))
                .setValueType("child", Type.of(R2BSlotChild.class))
                .setValueType("typedList", Type.of("List<Long>"))
                .setValueType("wrappedChild", Type.of("java.util.Optional<" + R2BSlotChild.class.getName() + ">"))
                .setValueType("typedArray", Type.of("Integer[]"));
    }

    private static Document r2bDocument(final String xml) {
        try {
            return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new org.xml.sax.InputSource(new StringReader(xml)));
        } catch (final Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static void r2bAssertSame(final Object expected, final Object actual, final String message) {
        if (expected != null && expected.getClass().isArray()) {
            assertArrayEquals((Object[]) expected, (Object[]) actual, message);
        } else {
            assertEquals(expected, actual, message);
        }
    }

    // Reads the document on the three backends and through the DOM Node overload: all four results must be equal.
    private static <T> T r2bSameOnAllBackends(final String xml, final XmlDeserConfig config, final Type<T> type) {
        final T expected = parserOf(XmlParserType.StAX).deserialize(xml, config, type);

        for (final XmlParserType parserType : ALL_TYPES) {
            r2bAssertSame(expected, parserOf(parserType).deserialize(xml, config, type), parserType + " " + xml);
        }
        r2bAssertSame(expected, parserOf(XmlParserType.StAX).deserialize(r2bDocument(xml), config, type), "Node " + xml);

        return expected;
    }

    // Every backend and the DOM Node overload reject the document with the message of the disallowed attribute.
    private static void r2bRejectedOnAllBackends(final String xml, final XmlDeserConfig config, final Type<?> type) {
        for (final XmlParserType parserType : ALL_TYPES) {
            final XmlParser parser = parserOf(parserType);
            final ParsingException e = assertThrows(ParsingException.class, () -> parser.deserialize(xml, config, type), parserType + " " + xml);
            assertEquals(R2B_REJECTED, e.getMessage(), parserType + " " + xml);
        }
        final XmlParser parser = parserOf(XmlParserType.StAX);
        final Document document = r2bDocument(xml);
        final ParsingException e = assertThrows(ParsingException.class, () -> parser.deserialize(document, config, type), "Node " + xml);
        assertEquals(R2B_REJECTED, e.getMessage(), "Node " + xml);
    }

    private static void r4ParsingExceptionOnAllBackends(final String xml, final Type<?> type, final String expectedMessage) {
        for (final XmlParserType parserType : ALL_TYPES) {
            final XmlParser parser = parserOf(parserType);
            final ParsingException e = assertThrows(ParsingException.class, () -> parser.deserialize(xml, null, type), parserType + " " + xml);
            assertEquals(expectedMessage, e.getMessage(), parserType + " " + xml);
            assertInstanceOf(UnsupportedOperationException.class, e.getCause(), parserType + " " + xml);
        }
        final XmlParser parser = parserOf(XmlParserType.StAX);
        final Document document = r2bDocument(xml);
        final ParsingException e = assertThrows(ParsingException.class, () -> parser.deserialize(document, null, type), "Node " + xml);
        assertEquals(expectedMessage, e.getMessage(), "Node " + xml);
    }

    // Backend agreement of the Abacus readers after the recheck of rounds 3 and 4: every document below was read differently by
    // one of the three readers (or by the StAX reader for an element without content); each test pins the agreed result on the
    // StAX, DOM and SAX readers and through the DOM Node overload.

    /** R5-01: a property declared as an abstract scalar the codec cannot read from text. */
    @Data
    public static class R5NumberHolder {
        private Number n;
        private String tag;
    }

    /** R5-07: the property kinds whose element without content the StAX reader read with its own fallback. */
    @Data
    public static class R5EmptyHolder {
        @JsonXmlField(dateFormat = "yyyy-MM-dd")
        private java.util.Date when;
        @JsonXmlField(numberFormat = "#,##0")
        private Integer amount;
        private int count;
        private String text;
        private List<Integer> list;
        private java.util.Optional<Object> opt;
        private Object open;
    }

    // The typed root element of a bean as the writer opens it, e.g. <r5NumberHolder type="...R5NumberHolder">.
    private static String r5TypedRoot(final Object bean) {
        final String xml = parserOf(XmlParserType.StAX).serialize(bean, XmlSerConfig.create().setWriteTypeInfo(true));
        return xml.substring(0, xml.indexOf('>') + 1);
    }

    private static String r5EndTag(final String typedRoot) {
        return "</" + typedRoot.substring(1, typedRoot.indexOf(' ')) + ">";
    }

    private static String r5UntypedRoot(final String typedRoot) {
        return typedRoot.substring(0, typedRoot.indexOf(' ')) + ">";
    }

    // Every backend and the DOM Node overload fail with the same exception class and a message with the same start.
    private static void r5SameFailureOnAllBackends(final String xml, final XmlDeserConfig config, final Type<?> type, final Class<? extends Throwable> expected,
            final String messageStart) {
        for (final XmlParserType parserType : ALL_TYPES) {
            final XmlParser parser = parserOf(parserType);
            final Throwable e = assertThrows(expected, () -> parser.deserialize(xml, config, type), parserType + " " + xml);
            assertTrue(String.valueOf(e.getMessage()).startsWith(messageStart), parserType + " " + e.getMessage());
        }
        final Document document = r2bDocument(xml);
        final Throwable e = assertThrows(expected, () -> parserOf(XmlParserType.StAX).deserialize(document, config, type), "Node " + xml);
        assertTrue(String.valueOf(e.getMessage()).startsWith(messageStart), "Node " + e.getMessage());
    }

    // ---- review 2026-10-07 recheck R5 end ----

    // ---- deep review 2026-10-16 G007 begin ----

    // G007-01: an untyped <array> (no declared array type) whose <e> items the configured element type types used to keep the
    // String[] placeholder as its destination on StAX and DOM, so the configured items failed with "cannot be stored in String[]";
    // SAX built an array of the configured class. All three now agree, at the root, in an Object map value and in a List<Object> item.
    @Test
    public void testDeserialize_untypedArrayChildrenUnderConfiguredElementTypeOnAllBackends() {
        final XmlDeserConfig integers = XmlDeserConfig.create().setElementType(Integer.class);

        for (final XmlParserType parserType : XmlParserType.values()) {
            final XmlParser parser = new AbacusXmlParserImpl(parserType, null, null, approvedTypes());

            final Object root = parser.deserialize("<array><e>1</e><e>2</e></array>", integers, Object.class);
            assertArrayEquals(new Integer[] { 1, 2 }, assertInstanceOf(Integer[].class, root), parserType.name());

            final Map<String, Object> map = parser.deserialize("<map><entry><key>a</key><value><array><e>3</e></array></value></entry></map>", integers,
                    Type.<Map<String, Object>> of("Map<String, Object>"));
            assertArrayEquals(new Integer[] { 3 }, assertInstanceOf(Integer[].class, map.get("a")), parserType.name());

            final List<Object> list = parser.deserialize("<list><e><array><e>4</e><e isNull=\"true\"/></array></e></list>", integers,
                    Type.<List<Object>> of("List<Object>"));
            assertArrayEquals(new Integer[] { 4, null }, assertInstanceOf(Integer[].class, list.get(0)), parserType.name());

            // Unchanged: without configuration the untyped array keeps its String items.
            assertArrayEquals(new String[] { "1", "2" }, assertInstanceOf(String[].class, parser.deserialize("<array><e>1</e><e>2</e></array>", Object.class)),
                    parserType.name());
        }
    }

    // ---- deep review 2026-10-16 G007 end ----

    // ---- deep review 2026-10-16 verify G118 begin ----

    // G118-01: a configured untyped <array> is an array of the CONFIGURED class on all three backends (SAX's rule), not of the
    // items' runtime class (ArrayList[] / HashMap[] / Object[] for mixed items).
    @Test
    public void testDeserialize_configuredUntypedArrayUsesTheConfiguredClassOnAllBackends() {
        for (final XmlParserType parserType : XmlParserType.values()) {
            final XmlParser parser = new AbacusXmlParserImpl(parserType, null, null, approvedTypes());

            final Object lists = parser.deserialize("<array><e>1</e><e>2</e></array>", XmlDeserConfig.create().setElementType("List<Long>"), Object.class);
            assertEquals(List[].class, lists.getClass(), parserType.name());
            assertEquals(Arrays.asList(1L), ((List<?>[]) lists)[0], parserType.name());

            final Object numbers = parser.deserialize("<array><e type=\"Long\">1</e><e type=\"Integer\">2</e></array>",
                    XmlDeserConfig.create().setElementType(Number.class), Object.class);
            assertArrayEquals(new Number[] { 1L, 2 }, assertInstanceOf(Number[].class, numbers), parserType.name());
            assertEquals(Number[].class, numbers.getClass(), parserType.name());

            final List<Object> maps = parser.deserialize("<list><e><array><e><map><entry><key>k</key><value>1</value></entry></map></e></array></e></list>",
                    XmlDeserConfig.create().setElementType("Map<String, Integer>"), Type.<List<Object>> of("List<Object>"));
            assertEquals(Map[].class, maps.get(0).getClass(), parserType.name());
            assertEquals(1, ((Map<?, ?>[]) maps.get(0))[0].get("k"), parserType.name());
        }
    }

    // G118-01: an item the configured class cannot hold (a nested <array> under a scalar configuration) is a parse error on all
    // three backends.
    @Test
    public void testDeserialize_configuredUntypedArrayRejectsMismatchedItemsOnAllBackends() {
        final XmlDeserConfig integers = XmlDeserConfig.create().setElementType(Integer.class);

        for (final XmlParserType parserType : XmlParserType.values()) {
            final XmlParser parser = new AbacusXmlParserImpl(parserType, null, null, approvedTypes());

            assertThrows(ParsingException.class, () -> parser.deserialize("<array><e><array><e>1</e></array></e></array>", integers, Object.class),
                    parserType.name());
        }
    }

    // G007-01 neighbours that must not change: text form, declared array types, type attribute, String configuration, and the
    // StAX/DOM String[] results for an empty or all-null configured untyped array.
    @Test
    public void testDeserialize_configuredUntypedArrayNeighboursAreUnchanged() {
        final XmlDeserConfig integers = XmlDeserConfig.create().setElementType(Integer.class);

        for (final XmlParserType parserType : XmlParserType.values()) {
            final XmlParser parser = new AbacusXmlParserImpl(parserType, null, null, approvedTypes());
            final String name = parserType.name();

            assertArrayEquals(new String[] { "1", "2" }, assertInstanceOf(String[].class, parser.deserialize("<array>[1, 2]</array>", integers, Object.class)),
                    name);
            assertArrayEquals(new Integer[] { 1, 2 }, parser.deserialize("<array><e>1</e><e>2</e></array>", integers, Integer[].class), name);
            assertEquals(Object[].class, parser.deserialize("<array><e>1</e><e>2</e></array>", integers, Object[].class).getClass(), name);
            final Object typed = parser.deserialize("<array type=\"Object[]\"><e>1</e><e>2</e></array>", integers, Object.class);
            assertEquals(Object[].class, typed.getClass(), name);
            assertArrayEquals(new Object[] { 1, 2 }, (Object[]) typed, name);
            final Object strings = parser.deserialize("<array><e>1</e><e>2</e></array>", XmlDeserConfig.create().setElementType(String.class), Object.class);
            assertEquals(String[].class, strings.getClass(), name);
            assertArrayEquals(new String[] { "1", "2" }, (String[]) strings, name);

            if (parserType != XmlParserType.SAX) {
                // SAX answers Integer[0] / Integer[]{null} here (its placeholder); StAX and DOM keep their String[] results.
                assertEquals(String[].class, parser.deserialize("<array></array>", integers, Object.class).getClass(), name);
                final Object nulls = parser.deserialize("<array><e isNull=\"true\"/></array>", integers, Object.class);
                assertEquals(String[].class, nulls.getClass(), name);
                assertArrayEquals(new String[] { null }, (String[]) nulls, name);
            }
        }
    }

    // ---- deep review 2026-10-16 verify G118 end ----

    // ---- deep review 2026-10-16 G145 begin ----

    // G145 (G118-02): an explicit null element in a primitive array reads as the primitive default (as the JSON parser reads
    // [1, null, 3] into int[]), on all three backends, instead of escaping as a raw NullPointerException.
    @Test
    public void testDeserialize_nullElementInPrimitiveArrayReadsAsDefault() {
        final String xml = "<array><e>1</e><e isNull=\"true\"/><e>3</e></array>";

        for (final XmlParserType parserType : XmlParserType.values()) {
            final XmlParser parser = new AbacusXmlParserImpl(parserType, null, null, approvedTypes());
            final String name = parserType.name();

            assertArrayEquals(new int[] { 1, 0, 3 }, parser.deserialize(xml, null, int[].class), name);
            assertArrayEquals(new long[] { 1L, 0L, 3L }, parser.deserialize(xml, null, long[].class), name);
            assertArrayEquals(new double[] { 1d, 0d, 3d }, parser.deserialize(xml, null, double[].class), name);
            assertArrayEquals(new boolean[] { true, false }, parser.deserialize("<array><e>true</e><e isNull=\"true\"/></array>", null, boolean[].class), name);

            final Object configured = parser.deserialize(xml, XmlDeserConfig.create().setElementType(int.class), Object.class);
            assertArrayEquals(new int[] { 1, 0, 3 }, assertInstanceOf(int[].class, configured), name);
        }
    }

    // G145 neighbours (unchanged): all-non-null primitive arrays, and a null element in a wrapper array stays null.
    @Test
    public void testDeserialize_primitiveArrayWithoutNullAndWrapperArrayNullUnchanged() {
        for (final XmlParserType parserType : XmlParserType.values()) {
            final XmlParser parser = new AbacusXmlParserImpl(parserType, null, null, approvedTypes());
            final String name = parserType.name();

            assertArrayEquals(new int[] { 1, 2 }, parser.deserialize("<array><e>1</e><e>2</e></array>", null, int[].class), name);
            assertArrayEquals(new Integer[] { 1, null, 3 }, parser.deserialize("<array><e>1</e><e isNull=\"true\"/><e>3</e></array>", null, Integer[].class),
                    name);
        }
    }

    // ---- deep review 2026-10-16 G145 end ----

    // ---- deep review 2026-10-16 G153 begin ----

    // G153-01 (G145 coverage): the shared collectionToArray maps a null element to the primitive default for every primitive array type
    // (leading, inner and all-null), and leaves null-free, empty, nested (int[][]) and wrapper arrays as before; a NullPointerException
    // that does not come from a null element is rethrown unchanged, and a mistyped non-null element fails with ParsingException.
    @Test
    public void testCollectionToArray_nullElementReadsAsPrimitiveDefaultForEveryPrimitiveType() {
        assertArrayEquals(new boolean[] { false, true, false },
                (boolean[]) AbstractParser.collectionToArray(Arrays.asList(null, true, null), Type.of(boolean[].class)));
        assertArrayEquals(new char[] { 'a', (char) 0 }, (char[]) AbstractParser.collectionToArray(Arrays.asList('a', null), Type.of(char[].class)));
        assertArrayEquals(new byte[] { 1, 0, 3 }, (byte[]) AbstractParser.collectionToArray(Arrays.asList((byte) 1, null, (byte) 3), Type.of(byte[].class)));
        assertArrayEquals(new short[] { 0, 2 }, (short[]) AbstractParser.collectionToArray(Arrays.asList(null, (short) 2), Type.of(short[].class)));
        assertArrayEquals(new int[] { 1, 0, 3 }, (int[]) AbstractParser.collectionToArray(Arrays.asList(1, null, 3), Type.of(int[].class)));
        assertArrayEquals(new long[] { 1L, 0L }, (long[]) AbstractParser.collectionToArray(Arrays.asList(1L, null), Type.of(long[].class)));
        assertArrayEquals(new float[] { 0f, 1.5f }, (float[]) AbstractParser.collectionToArray(Arrays.asList(null, 1.5f), Type.of(float[].class)));
        assertArrayEquals(new double[] { 1.5d, 0d }, (double[]) AbstractParser.collectionToArray(Arrays.asList(1.5d, null), Type.of(double[].class)));
        assertArrayEquals(new int[] { 0, 0 }, (int[]) AbstractParser.collectionToArray(Arrays.asList(null, null), Type.of(int[].class)));

        // Unchanged neighbours.
        assertArrayEquals(new int[] { 1, 2 }, (int[]) AbstractParser.collectionToArray(Arrays.asList(1, 2), Type.of(int[].class)));
        assertEquals(0, ((char[]) AbstractParser.collectionToArray(new java.util.ArrayList<>(), Type.of(char[].class))).length);
        final int[][] nested = AbstractParser.collectionToArray(Arrays.asList(new int[] { 1 }, null), Type.of(int[][].class));
        assertArrayEquals(new int[] { 1 }, nested[0]);
        assertNull(nested[1]);
        assertArrayEquals(new Integer[] { 1, null }, (Integer[]) AbstractParser.collectionToArray(Arrays.asList(1, null), Type.of(Integer[].class)));
        // A mistyped non-null element is a ParsingException (G157), see testCollectionToArray_mistypedElementOfPrimitiveArrayIsParsingException.
        assertThrows(ParsingException.class, () -> AbstractParser.collectionToArray(Arrays.asList(1L), Type.of(int[].class)));

        final NullPointerException failure = new NullPointerException("iterator failure");
        final java.util.Collection<Object> failing = new java.util.AbstractCollection<>() {
            @Override
            public java.util.Iterator<Object> iterator() {
                return new java.util.Iterator<>() {
                    private int count;

                    @Override
                    public boolean hasNext() {
                        return count < 2;
                    }

                    @Override
                    public Object next() {
                        if (count++ == 1) {
                            throw failure;
                        }

                        return 1;
                    }
                };
            }

            @Override
            public int size() {
                return 2;
            }
        };

        org.junit.jupiter.api.Assertions.assertSame(failure,
                assertThrows(NullPointerException.class, () -> AbstractParser.collectionToArray(failing, Type.of(int[].class))));
    }

    // G153-02 (G145 coverage): null markers in primitive arrays on all three backends beyond int[]/long[]/double[]/boolean[]: char[], an all-null
    // array, a nested int[][] (a null inner element reads 0, a null row stays null), a bean's int[] property, a type="int[]" attribute and a
    // configured char component; an empty primitive array stays empty.
    @Test
    public void testDeserialize_nullMarkerInPrimitiveArrayShapesOnAllBackends() {
        for (final XmlParserType parserType : XmlParserType.values()) {
            final XmlParser parser = new AbacusXmlParserImpl(parserType, null, null, approvedTypes());
            final String name = parserType.name();

            assertArrayEquals(new char[] { 'a', (char) 0 }, parser.deserialize("<array><e>a</e><e isNull=\"true\"/></array>", null, char[].class), name);
            assertArrayEquals(new int[] { 0, 0 }, parser.deserialize("<array><e isNull=\"true\"/><e isNull=\"true\"/></array>", null, int[].class), name);
            assertEquals(0, parser.deserialize("<array></array>", null, int[].class).length, name);

            final int[][] nested = parser.deserialize("<array><e><array><e>1</e><e isNull=\"true\"/></array></e><e isNull=\"true\"/></array>", null,
                    int[][].class);
            assertEquals(2, nested.length, name);
            assertArrayEquals(new int[] { 1, 0 }, nested[0], name);
            assertNull(nested[1], name);

            final Family family = parser.deserialize("<family><nums><array><e>7</e><e isNull=\"true\"/></array></nums></family>", null, Family.class);
            assertArrayEquals(new int[] { 7, 0 }, family.getNums(), name);

            assertArrayEquals(new int[] { 1, 0 },
                    assertInstanceOf(int[].class, parser.deserialize("<array type=\"int[]\"><e>1</e><e isNull=\"true\"/></array>", null, Object.class)), name);

            final Object chars = parser.deserialize("<array><e>x</e><e isNull=\"true\"/></array>", XmlDeserConfig.create().setElementType(char.class),
                    Object.class);
            assertArrayEquals(new char[] { 'x', (char) 0 }, assertInstanceOf(char[].class, chars), name);
        }
    }

    // ---- deep review 2026-10-16 G153 end ----

    // ---- deep review 2026-10-16 G157 begin ----

    // G157 (G153-05): a non-null element that a primitive array cannot hold (a nested array/map/list item, a wrong wrapper type) is
    // a ParsingException naming the element, like the object-array check, not a raw ClassCastException from the unboxing cast.
    @Test
    public void testCollectionToArray_mistypedElementOfPrimitiveArrayIsParsingException() {
        ParsingException e = assertThrows(ParsingException.class,
                () -> AbstractParser.collectionToArray(Arrays.asList(1, new int[] { 2 }), Type.of(int[].class)));
        assertTrue(e.getMessage().contains("index 1"), e.getMessage());
        assertTrue(e.getMessage().contains("int[]"), e.getMessage());
        assertInstanceOf(ClassCastException.class, e.getCause());

        // A null before the mistyped element takes the null-to-default path first and still ends as a ParsingException.
        e = assertThrows(ParsingException.class, () -> AbstractParser.collectionToArray(Arrays.asList(null, "x"), Type.of(long[].class)));
        assertTrue(e.getMessage().contains("index 1"), e.getMessage());
        assertThrows(ParsingException.class, () -> AbstractParser.collectionToArray(Arrays.asList(1), Type.of(char[].class)));
        assertThrows(ParsingException.class, () -> AbstractParser.collectionToArray(Arrays.asList(true, java.util.Map.of()), Type.of(boolean[].class)));

        // Unchanged neighbours: correctly typed elements, nulls only, and a ClassCastException that no element explains (rethrown as is).
        assertArrayEquals(new long[] { 1L, 0L }, (long[]) AbstractParser.collectionToArray(Arrays.asList(1L, null), Type.of(long[].class)));
        final ClassCastException failure = new ClassCastException("iterator failure");
        final java.util.Collection<Object> failing = new java.util.AbstractCollection<>() {
            @Override
            public java.util.Iterator<Object> iterator() {
                return new java.util.Iterator<>() {
                    private int count;

                    @Override
                    public boolean hasNext() {
                        return count < 2;
                    }

                    @Override
                    public Object next() {
                        if (count++ == 1) {
                            throw failure;
                        }

                        return 1;
                    }
                };
            }

            @Override
            public int size() {
                return 2;
            }
        };

        org.junit.jupiter.api.Assertions.assertSame(failure,
                assertThrows(ClassCastException.class, () -> AbstractParser.collectionToArray(failing, Type.of(int[].class))));
    }

    // G157 (G153-05): with a primitive configured element type, a nested <array>, <map> or <list> item is a ParsingException on every
    // backend (r89: ParsingException on StAX/DOM, ClassCastException on SAX); a declared int[] target with such an item too (r89:
    // ClassCastException on all backends). Well-formed arrays are unchanged.
    @Test
    public void testDeserialize_nestedItemIntoPrimitiveArrayIsParsingExceptionOnAllBackends() {
        final XmlDeserConfig intElements = XmlDeserConfig.create().setElementType(int.class);

        for (final XmlParserType parserType : XmlParserType.values()) {
            final XmlParser parser = new AbacusXmlParserImpl(parserType, null, null, approvedTypes());
            final String name = parserType.name();

            assertThrows(ParsingException.class, () -> parser.deserialize("<array><e><array><e>1</e></array></e></array>", intElements, Object.class), name);
            assertThrows(ParsingException.class, () -> parser.deserialize("<array><e><map><a>1</a></map></e></array>", intElements, Object.class), name);
            assertThrows(ParsingException.class, () -> parser.deserialize("<array><e><list><e>1</e></list></e></array>", intElements, Object.class), name);
            assertThrows(ParsingException.class, () -> parser.deserialize("<array><e><array><e>1</e></array></e></array>", null, int[].class), name);

            assertArrayEquals(new int[] { 1, 2 },
                    assertInstanceOf(int[].class, parser.deserialize("<array><e>1</e><e>2</e></array>", intElements, Object.class)), name);
            assertArrayEquals(new int[] { 1, 2 }, parser.deserialize("<array><e>1</e><e>2</e></array>", null, int[].class), name);
        }
    }

    // ---- deep review 2026-10-16 G157 end ----

    // ---- deep review 2026-10-26 G007 begin ----

    // G007-01: an unknown (ignoreUnmatchedProperty) or ignored bean property whose content mixes text with a child element is
    // skipped whole on every backend. StAX stepped into the child element and ended the bean at its end, silently dropping
    // every following property (r107: name=null, age=0). A known property with such content is the mixed-content
    // ParsingException of the StAX item/key/value wrappers (r107: the text became the value and the bean also ended early).
    @Test
    public void testDeserialize_mixedContentInSkippedPropertyKeepsFollowingProperties() {
        final XmlDeserConfig ignoreUnmatched = new XmlDeserConfig().setIgnoreUnmatchedProperty(true);
        final XmlDeserConfig ignoreName = new XmlDeserConfig().setIgnoredPropNames(Person.class, Set.of("name"));
        final String unknown = "<person><desc>Some <i>bold</i> text</desc><name>n</name><age>3</age></person>";
        final String unknownByNameAttribute = "<bean name=\"person\"><property name=\"desc\">Some <i>bold</i> text</property>"
                + "<property name=\"name\">n</property><property name=\"age\">3</property></bean>";

        for (final XmlParserType type : ALL_TYPES) {
            final XmlParser parser = parserOf(type);
            final String name = type.name();

            assertEquals(new Person("n", 3), parser.deserialize(unknown, ignoreUnmatched, Person.class), name);
            assertEquals(new Person("n", 3), parser.deserialize(new StringReader(unknown), ignoreUnmatched, Person.class), name);
            assertEquals(new Person("n", 3), parser.deserialize(unknownByNameAttribute, ignoreUnmatched, Person.class), name);
            assertEquals(new Person(null, 3), parser.deserialize("<person><name>Some <i>x</i> text</name><age>3</age></person>", ignoreName, Person.class),
                    name);

            // Unchanged neighbours: indentation before a nested value, and text-only properties.
            assertEquals(new Person(" n", 3),
                    parser.deserialize("<person><desc>\n  <i>bold</i>\n</desc><name> n</name><age>3</age></person>", ignoreUnmatched, Person.class), name);
        }

        final XmlParser stax = parserOf(XmlParserType.StAX);
        final ParsingException e = assertThrows(ParsingException.class,
                () -> stax.deserialize("<person><name>ab<zqx>1</zqx></name><age>3</age></person>", null, Person.class));
        assertEquals("Mixed scalar and nested XML values are not supported", e.getMessage());

        for (final XmlParserType type : ALL_TYPES) {
            assertThrows(ParsingException.class,
                    () -> parserOf(type).deserialize(new StringReader("<person><age>3</age><name>ab<zqx/></name></person>"), null, Person.class), type.name());
        }
    }

    @Data
    public static class E {
        private String name;
    }

    @Data
    public static class Key {
        private String name;
    }

    @Data
    public static class Value {
        private String name;
    }

    @Data
    public static class G007WrapperNamedBeans {
        private Value value;
        private Map<String, Value> values;
        private List<E> items;
        private Map<Key, String> keyed;
        private Value[] array;
        private String after;
    }

    // G007-02: the writer names a bean element after its class, so a bean class named E, Entry, Key or Value is written as
    // <e>, <entry>, <key> or <value>. In a declared bean slot (the root, a bean property, a typed item, key or value) every
    // backend now reads it as that bean; r107 classified it as the wrapper of that name and could not read the parser's own
    // output back (StAX/DOM "Unsupported class type", SAX "only allowed inside a map element"; SAX read the name-attribute form).
    @Test
    public void testDeserialize_beanClassNamedLikeWrapperElementReadsBack() {
        final Value value = new Value();
        value.setName("va");
        final E item = new E();
        item.setName("it");
        final Key key = new Key();
        key.setName("ke");

        final G007WrapperNamedBeans beans = new G007WrapperNamedBeans();
        beans.setValue(value);
        beans.setValues(new LinkedHashMap<>(Map.of("v", value)));
        beans.setItems(Arrays.asList(item, null));
        beans.setKeyed(new LinkedHashMap<>(Map.of(key, "k")));
        beans.setArray(new Value[] { value });
        beans.setAfter("z");

        for (final XmlParserType type : ALL_TYPES) {
            final XmlParser parser = parserOf(type);

            for (final boolean tagByPropertyName : new boolean[] { true, false }) {
                final XmlSerConfig config = new XmlSerConfig().setTagByPropertyName(tagByPropertyName);
                final String name = type.name() + " tagByPropertyName=" + tagByPropertyName;

                assertEquals(beans, parser.deserialize(parser.serialize(beans, config), G007WrapperNamedBeans.class), name);
                assertEquals(item, parser.deserialize(parser.serialize(item, config), E.class), name);
                assertEquals(key, parser.deserialize(parser.serialize(key, config), Key.class), name);
                assertEquals(value, parser.deserialize(parser.serialize(value, config), Value.class), name);
            }

            // Unchanged neighbours: the wrappers keep their structural meaning in a map and an untyped list.
            assertEquals(Map.of("k", "v"), parser.deserialize("<map><entry><key>k</key><value>v</value></entry></map>", Map.class), type.name());
            assertEquals(List.of("x"), parser.deserialize("<list><e>x</e></list>", List.class), type.name());
        }
    }

    // ---- deep review 2026-10-26 G007 end ----

    // ---- deep review 2026-10-26 verify G118 begin ----

    // G007-01 in a nested bean: an unknown property with mixed content is skipped whole and both beans keep their following
    // properties on every backend (r107 StAX: "Unexpected element <keep> after the value of property 'inner'"); an indented
    // known bean property still reads its nested value.
    @Test
    public void testDeserialize_mixedContentInNestedBeanPropertyIsSkippedOnEveryBackend() {
        final XmlDeserConfig ignoreUnmatched = new XmlDeserConfig().setIgnoreUnmatchedProperty(true);
        final String nested = "<ignOuter><name>n</name><inner><ignInner><desc>a<i>b</i>c</desc><keep>k</keep></ignInner></inner><tail>t</tail></ignOuter>";
        final String indented = "<ignOuter><inner>\n  <ignInner><keep>k</keep></ignInner>\n</inner><tail>t</tail></ignOuter>";

        for (final XmlParserType type : ALL_TYPES) {
            final XmlParser parser = parserOf(type);

            for (final String xml : new String[] { nested, indented }) {
                final IgnOuter outer = parser.deserialize(new StringReader(xml), ignoreUnmatched, IgnOuter.class);
                assertEquals(nested.equals(xml) ? "n" : null, outer.getName(), type.name());
                assertEquals("k", outer.getInner().getKeep(), type.name());
                assertEquals("t", outer.getTail(), type.name());
            }
        }
    }

    // G007-01 for a known property: StAX rejects text in a CDATA fragment before an empty child element as mixed content (r107
    // StAX kept "ab", took the child's end for the property end and ended the bean there: age lost), and the same content in an
    // ignored property is skipped and the following property is read on every backend. DOM and SAX reject THIS input only
    // because zqx names no class; with a resolvable child they ignore the text (see the G129 test below and package-info L17).
    @Test
    public void testDeserialize_knownPropertyMixedContentIsRejectedByStAX() {
        final String xml = "<person><name><![CDATA[ab]]><zqx/></name><age>3</age></person>";
        final XmlDeserConfig ignoreName = new XmlDeserConfig().setIgnoredPropNames(Person.class, Set.of("name"));

        for (final XmlParserType type : ALL_TYPES) {
            final XmlParser parser = parserOf(type);

            assertThrows(ParsingException.class, () -> parser.deserialize(xml, Person.class), type.name());
            assertEquals(new Person(null, 3), parser.deserialize(xml, ignoreName, Person.class), type.name());
        }

        final ParsingException e = assertThrows(ParsingException.class, () -> parserOf(XmlParserType.StAX).deserialize(xml, Person.class));
        assertEquals("Mixed scalar and nested XML values are not supported", e.getMessage());
    }

    // G007-02 with writeTypeInfo and a null marker: the typed output of bean classes named Value, E and Key reads back on every
    // backend (r107: StAX/DOM "Unsupported class type", SAX "only allowed inside a map element"), a null-marked element named
    // like the wrapper in a declared bean slot is null, and the wrappers keep their structural meaning in an Object root.
    @Test
    public void testDeserialize_beanClassNamedLikeWrapperElementWithTypeInfoAndNullMarker() {
        final Value value = new Value();
        value.setName("va");
        final E item = new E();
        item.setName("it");
        final Key key = new Key();
        key.setName("ke");

        final G007WrapperNamedBeans beans = new G007WrapperNamedBeans();
        beans.setValue(value);
        beans.setValues(new LinkedHashMap<>(Map.of("v", value)));
        beans.setItems(Arrays.asList(item, null));
        beans.setKeyed(new LinkedHashMap<>(Map.of(key, "k")));
        beans.setArray(new Value[] { value });
        beans.setAfter("z");

        for (final XmlParserType type : ALL_TYPES) {
            final XmlParser parser = parserOf(type);

            for (final boolean tagByPropertyName : new boolean[] { true, false }) {
                final XmlSerConfig config = new XmlSerConfig().setWriteTypeInfo(true).setTagByPropertyName(tagByPropertyName);
                final String name = type.name() + " tagByPropertyName=" + tagByPropertyName;

                assertEquals(beans, parser.deserialize(parser.serialize(beans, config), G007WrapperNamedBeans.class), name);
                assertEquals(value, parser.deserialize(parser.serialize(value, config), Value.class), name);
            }

            final G007WrapperNamedBeans nullValue = parser.deserialize(
                    "<g007WrapperNamedBeans><value><value isNull=\"true\"/></value><after>z</after></g007WrapperNamedBeans>", G007WrapperNamedBeans.class);
            assertNull(nullValue.getValue(), type.name());
            assertEquals("z", nullValue.getAfter(), type.name());

            // Unchanged neighbours: real wrappers in an open (Object) root.
            assertEquals(Map.of("k", "v"), parser.deserialize("<map><entry><key>k</key><value>v</value></entry></map>", Object.class), type.name());
            assertEquals(List.of("x"), parser.deserialize("<list><e>x</e></list>", Object.class), type.name());
        }
    }

    @Data
    public static class G118ObjectHolder {
        private String name;
        private Object obj;
        private Person friend;
        private String after;
    }

    // G118-03: name-attribute form, a bean element without a name attribute in an Object property is looked up by its local name
    // (here one no class has, so it is read as a bean-shaped map) on every backend (r107 SAX: NullPointerException from the node-name
    // cache; StAX/DOM read the map). Not <bean>: discovery would find a test class named Bean.
    @Test
    public void testDeserialize_nameAttributeFormAnonymousBeanInObjectProperty() {
        final String xml = "<bean name=\"g118ObjectHolder\"><property name=\"obj\"><zqxNoName><property name=\"y\">1</property></zqxNoName></property>"
                + "<property name=\"after\">z</property></bean>";

        for (final XmlParserType type : ALL_TYPES) {
            final G118ObjectHolder holder = parserOf(type).deserialize(xml, G118ObjectHolder.class);

            assertEquals(Map.of("y", "1"), holder.getObj(), type.name());
            assertEquals("z", holder.getAfter(), type.name());
        }
    }

    // G118-03: name-attribute form, a nameless child element of a String property is the ParsingException StAX and DOM give
    // ("No bean class found for node name: ..."); r107 SAX threw a NullPointerException.
    @Test
    public void testDeserialize_nameAttributeFormNamelessChildOfScalarProperty() {
        final String xml = "<bean name=\"g118ObjectHolder\"><property name=\"name\"><zqx/></property><property name=\"after\">z</property></bean>";

        for (final XmlParserType type : ALL_TYPES) {
            final ParsingException e = assertThrows(ParsingException.class, () -> parserOf(type).deserialize(xml, G118ObjectHolder.class), type.name());
            assertTrue(e.getMessage().startsWith("No bean class found for node name: zqx"), type.name() + ": " + e.getMessage());
        }
    }

    // G118-03 neighbours (unchanged on every backend): a child WITH a name attribute in an Object property, and a nameless bean
    // element in a declared bean slot.
    @Test
    public void testDeserialize_nameAttributeFormNamedChildAndDeclaredBeanSlotUnchanged() {
        final String named = "<bean name=\"g118ObjectHolder\"><property name=\"obj\"><bean name=\"zqxBean\"><property name=\"y\">1</property></bean></property>"
                + "<property name=\"after\">z</property></bean>";
        final String declared = "<bean name=\"g118ObjectHolder\"><property name=\"friend\"><bean><property name=\"name\">p</property>"
                + "<property name=\"age\">3</property></bean></property><property name=\"after\">z</property></bean>";

        for (final XmlParserType type : ALL_TYPES) {
            final XmlParser parser = parserOf(type);

            final G118ObjectHolder namedHolder = parser.deserialize(named, G118ObjectHolder.class);
            assertEquals(Map.of("y", "1"), namedHolder.getObj(), type.name());
            assertEquals("z", namedHolder.getAfter(), type.name());

            final G118ObjectHolder declaredHolder = parser.deserialize(declared, G118ObjectHolder.class);
            assertEquals(new Person("p", 3), declaredHolder.getFriend(), type.name());
            assertEquals("z", declaredHolder.getAfter(), type.name());
        }
    }

    // ---- deep review 2026-10-26 verify G118 end ----

    // ---- deep review 2026-10-27 G129 begin ----

    public static class G129AChild {
        private String v;

        public String getV() {
            return v;
        }

        public void setV(final String v) {
            this.v = v;
        }
    }

    public static class G129AHolder {
        private String name;
        private G129AChild friend;
        private String after;

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public G129AChild getFriend() {
            return friend;
        }

        public void setFriend(final G129AChild friend) {
            this.friend = friend;
        }

        public String getAfter() {
            return after;
        }

        public void setAfter(final String after) {
            this.after = after;
        }
    }

    // G129-02 (package-info L17): a known property whose non-blank text precedes a nested element that resolves is rejected by
    // StAX as mixed content, while DOM and SAX ignore the text and read the nested element (a bean slot gets the child bean).
    @Test
    public void testDeserialize_knownPropertyMixedContentWithResolvableChildDiffersPerBackend() {
        final String beanSlot = "<g129AHolder><friend>ab<g129AChild><v>x</v></g129AChild></friend><after>z</after></g129AHolder>";
        final String stringSlot = "<g129AHolder><name>ab<g129AChild><v>x</v></g129AChild></name><after>z</after></g129AHolder>";

        for (final XmlParserType type : ALL_TYPES) {
            final XmlParser parser = parserOf(type);

            if (type == XmlParserType.StAX) {
                for (final String xml : new String[] { beanSlot, stringSlot }) {
                    final ParsingException e = assertThrows(ParsingException.class, () -> parser.deserialize(xml, G129AHolder.class), xml);
                    assertEquals("Mixed scalar and nested XML values are not supported", e.getMessage(), xml);
                }
            } else {
                final G129AHolder holder = parser.deserialize(beanSlot, G129AHolder.class);
                assertEquals("x", holder.getFriend().getV(), type.name());
                assertEquals("z", holder.getAfter(), type.name());

                assertEquals("z", parser.deserialize(stringSlot, G129AHolder.class).getAfter(), type.name());
            }
        }
    }

    // ---- deep review 2026-10-27 G129 end ----

    // ---- deep review 2026-10-27 G131 begin ----

    public static class G131AChild {
        private String v;

        public String getV() {
            return v;
        }

        public void setV(final String v) {
            this.v = v;
        }
    }

    public static class G131AHolder {
        private String name;
        @JsonXmlField(direction = JsonXmlField.Direction.SERIALIZE_ONLY)
        private String computed;
        @JsonXmlField(direction = JsonXmlField.Direction.SERIALIZE_ONLY)
        private int count;
        private String after;

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public String getComputed() {
            return computed;
        }

        public void setComputed(final String computed) {
            this.computed = computed;
        }

        public int getCount() {
            return count;
        }

        public void setCount(final int count) {
            this.count = count;
        }

        public String getAfter() {
            return after;
        }

        public void setAfter(final String after) {
            this.after = after;
        }
    }

    // G131: a SERIALIZE_ONLY property is ignored when reading, so StAX skips its mixed content whole as for an ignored property
    // (r107 lost the following property, r108 rejected it as mixed content). DOM and SAX read the resolvable child and drop it.
    @Test
    public void testDeserialize_serializeOnlyPropertyMixedContentIsSkipped() {
        final String resolvable = "<g131AHolder><computed>ab<g131AChild><v>x</v></g131AChild></computed><after>z</after></g131AHolder>";

        for (final XmlParserType type : ALL_TYPES) {
            final G131AHolder holder = parserOf(type).deserialize(resolvable, G131AHolder.class);
            assertNull(holder.getComputed(), type.name());
            assertEquals("z", holder.getAfter(), type.name());
        }

        final XmlParser stax = parserOf(XmlParserType.StAX);

        for (final String xml : new String[] { "<g131AHolder><computed>ab<zqx/></computed><after>z</after></g131AHolder>",
                "<g131AHolder><computed>ab<i>b</i>c</computed><after>z</after></g131AHolder>",
                "<g131AHolder><after>z</after><computed>ab<zqx><q>1</q></zqx></computed></g131AHolder>" }) {
            final G131AHolder holder = stax.deserialize(xml, G131AHolder.class);
            assertNull(holder.getComputed(), xml);
            assertEquals("z", holder.getAfter(), xml);
        }
    }

    // G131 neighbours on StAX: plain text of a SERIALIZE_ONLY property is still decoded (and discarded), and an ordinary
    // property with mixed content is still rejected.
    @Test
    public void testDeserialize_serializeOnlyNeighboursUnchangedOnStAX() {
        final XmlParser stax = parserOf(XmlParserType.StAX);

        final G131AHolder plain = stax.deserialize("<g131AHolder><computed>ab</computed><count>7</count><after>z</after></g131AHolder>", G131AHolder.class);
        assertNull(plain.getComputed());
        assertEquals(0, plain.getCount());
        assertEquals("z", plain.getAfter());

        assertThrows(NumberFormatException.class, () -> stax.deserialize("<g131AHolder><count>abc</count><after>z</after></g131AHolder>", G131AHolder.class));

        final ParsingException e = assertThrows(ParsingException.class,
                () -> stax.deserialize("<g131AHolder><name>ab<zqx/></name><after>z</after></g131AHolder>", G131AHolder.class));
        assertEquals("Mixed scalar and nested XML values are not supported", e.getMessage());
    }

    // ---- deep review 2026-10-27 G131 end ----

    // ---- deep review 2026-10-27 G136 begin ----

    public static class G136EntryBean {
        private java.util.AbstractMap.SimpleEntry<String, Integer> entry;
        private List<java.util.AbstractMap.SimpleEntry<String, Integer>> entries;
        private Map<String, java.util.AbstractMap.SimpleEntry<String, Integer>> entryMap;
        private java.util.AbstractMap.SimpleImmutableEntry<String, Integer> immutableEntry;
        private List<Map.Entry<String, Integer>> mapEntries;
        private Map<String, Integer> map;
        private List<Map<String, Integer>> maps;

        public java.util.AbstractMap.SimpleEntry<String, Integer> getEntry() {
            return entry;
        }

        public void setEntry(final java.util.AbstractMap.SimpleEntry<String, Integer> entry) {
            this.entry = entry;
        }

        public List<java.util.AbstractMap.SimpleEntry<String, Integer>> getEntries() {
            return entries;
        }

        public void setEntries(final List<java.util.AbstractMap.SimpleEntry<String, Integer>> entries) {
            this.entries = entries;
        }

        public Map<String, java.util.AbstractMap.SimpleEntry<String, Integer>> getEntryMap() {
            return entryMap;
        }

        public void setEntryMap(final Map<String, java.util.AbstractMap.SimpleEntry<String, Integer>> entryMap) {
            this.entryMap = entryMap;
        }

        public java.util.AbstractMap.SimpleImmutableEntry<String, Integer> getImmutableEntry() {
            return immutableEntry;
        }

        public void setImmutableEntry(final java.util.AbstractMap.SimpleImmutableEntry<String, Integer> immutableEntry) {
            this.immutableEntry = immutableEntry;
        }

        public List<Map.Entry<String, Integer>> getMapEntries() {
            return mapEntries;
        }

        public void setMapEntries(final List<Map.Entry<String, Integer>> mapEntries) {
            this.mapEntries = mapEntries;
        }

        public Map<String, Integer> getMap() {
            return map;
        }

        public void setMap(final Map<String, Integer> map) {
            this.map = map;
        }

        public List<Map<String, Integer>> getMaps() {
            return maps;
        }

        public void setMaps(final List<Map<String, Integer>> maps) {
            this.maps = maps;
        }
    }

    // G009-06: an AbstractMap.SimpleEntry bean property, list element and map value read back as AbstractMap.SimpleEntry on the
    // SAX, StAX and DOM backends. The direct property used to receive a java.util.HashMap$Node (IllegalArgumentException "Can not
    // set java.util.AbstractMap$SimpleEntry field ..."), and list elements were HashMap$Node instances in a SimpleEntry list.
    @Test
    public void testDeserialize_simpleEntryPropertyElementAndMapValueRoundTripG136() {
        final G136EntryBean bean = new G136EntryBean();
        bean.setEntry(new java.util.AbstractMap.SimpleEntry<>("a", 1));
        bean.setEntries(new java.util.ArrayList<>(List.of(new java.util.AbstractMap.SimpleEntry<>("b", 2))));
        bean.setEntryMap(new LinkedHashMap<>(Map.of("k", new java.util.AbstractMap.SimpleEntry<>("c", 3))));
        bean.setImmutableEntry(new java.util.AbstractMap.SimpleImmutableEntry<>("d", 4));
        bean.setMapEntries(new java.util.ArrayList<>(List.of(new java.util.AbstractMap.SimpleEntry<>("e", 5))));

        for (final XmlParserType parserType : XmlParserType.values()) {
            final XmlParser xmlParser = parserOf(parserType);
            final G136EntryBean back = xmlParser.deserialize(xmlParser.serialize(bean), G136EntryBean.class);

            assertEquals(bean.getEntry(), back.getEntry(), parserType.toString());
            assertEquals(java.util.AbstractMap.SimpleEntry.class, back.getEntry().getClass(), parserType.toString());
            assertEquals(bean.getEntries(), back.getEntries(), parserType.toString());
            assertEquals(java.util.AbstractMap.SimpleEntry.class, ((Object) back.getEntries().get(0)).getClass(), parserType.toString());
            assertEquals(bean.getEntryMap(), back.getEntryMap(), parserType.toString());
            assertEquals(java.util.AbstractMap.SimpleEntry.class, ((Object) back.getEntryMap().get("k")).getClass(), parserType.toString());
            assertEquals(bean.getImmutableEntry(), back.getImmutableEntry(), parserType.toString());
            assertEquals(bean.getMapEntries(), back.getMapEntries(), parserType.toString());
        }
    }

    // G009-05: a Map.Entry list element is compact JSON read by the JSON reader; an object with two entries there now fails like
    // the direct Map.Entry property always did, instead of silently keeping the first entry.
    @Test
    public void testDeserialize_multiEntryObjectInMapEntryListElementFailsLikePropertyG136() {
        final G136EntryBean bean = new G136EntryBean();
        bean.setMapEntries(new java.util.ArrayList<>(List.of(new java.util.AbstractMap.SimpleEntry<>("m", 1))));
        final G136EntryBean property = new G136EntryBean();
        property.setEntry(new java.util.AbstractMap.SimpleEntry<>("m", 1));

        for (final XmlParserType parserType : XmlParserType.values()) {
            final XmlParser xmlParser = parserOf(parserType);

            for (final G136EntryBean source : new G136EntryBean[] { bean, property }) {
                final String xml = xmlParser.serialize(source);
                final String twoEntries = xml.contains("m&quot;:1}") ? xml.replace("m&quot;:1}", "m&quot;:1, &quot;n&quot;:2}")
                        : xml.replace("m\":1}", "m\":1, \"n\":2}");
                assertTrue(!xml.equals(twoEntries), parserType.toString());

                final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> xmlParser.deserialize(twoEntries, G136EntryBean.class),
                        parserType.toString());
                assertEquals("Map.Entry JSON must contain exactly one entry, but got: 2", e.getMessage(), parserType.toString());
            }
        }
    }

    // G009-04: compact text whose element is a JSON array in a List<Map> property fails like the same array in the Map property
    // itself (the compact text is read by the JSON reader). The list used to hold the ArrayList.
    @Test
    public void testDeserialize_jsonArrayInCompactMapListElementFailsLikeMapPropertyG136() {
        for (final XmlParserType parserType : XmlParserType.values()) {
            final XmlParser xmlParser = parserOf(parserType);
            final String root = xmlParser.serialize(new G136EntryBean()).replaceAll("^<([A-Za-z0-9_]+).*$", "$1");
            final ParsingException expected = assertThrows(ParsingException.class,
                    () -> xmlParser.deserialize("<" + root + "><map>[1, 2]</map></" + root + ">", G136EntryBean.class), parserType.toString());

            final ParsingException e = assertThrows(ParsingException.class,
                    () -> xmlParser.deserialize("<" + root + "><maps>[[1, 2]]</maps></" + root + ">", G136EntryBean.class), parserType.toString());
            assertEquals(expected.getMessage(), e.getMessage(), parserType.toString());

            final G136EntryBean bean = xmlParser.deserialize("<" + root + "><maps>[{&quot;a&quot;: 1}]</maps></" + root + ">", G136EntryBean.class);
            assertEquals(List.of(Map.of("a", 1)), bean.getMaps(), parserType.toString());
        }
    }

    // ---- deep review 2026-10-27 G136 end ----
}
