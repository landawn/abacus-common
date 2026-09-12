package com.landawn.abacus.parser;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.annotation.JsonXmlField;
import com.landawn.abacus.exception.ParsingException;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.BufferedXmlWriter;
import com.landawn.abacus.util.DateTimeFormat;
import com.landawn.abacus.util.MapEntity;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.NamingPolicy;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.Tuple;
import com.landawn.abacus.util.Tuple.Tuple2;
import com.landawn.abacus.util.TypeReference;
import com.landawn.abacus.util.u.Nullable;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalInt;

import lombok.Data;
import lombok.NoArgsConstructor;

public class XmlParserImplTest extends TestBase {

    private XmlParserImpl staxParser;
    private XmlParserImpl domParser;

    @TempDir
    Path tempDir;

    @BeforeEach
    public void setUp() {
        staxParser = new XmlParserImpl(XmlParserType.StAX);
        domParser = new XmlParserImpl(XmlParserType.DOM);
    }

    @Data
    @NoArgsConstructor
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

        public TestBean(String name, int age) {
            this.name = name;
            this.age = age;
        }
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

    public static class EmptyBean {
    }

    /** Review fixes 2026-09-06: a bean whose properties are all {@code null} by default (P5-01). */
    @Data
    public static class NullableOnlyBean {
        private String a;
        private String b;
    }

    /** Review fixes 2026-09-06: outer bean for the empty-nested-bean round trip (P5-01/P5-02). */
    @Data
    public static class OuterBean {
        private String name;
        private NullableOnlyBean inner;
        private List<NullableOnlyBean> inners;
        private Map<String, Integer> map;
        private List<String> strs;
        private String tail;
    }

    /** Review fixes 2026-09-06: mixed (bean + scalar) array and collection properties (P5-05). */
    @Data
    public static class MixedBean {
        private Object[] objs;
        private List<Object> los;
    }

    /** Review fixes 2026-09-06: a map property whose keys are not valid XML element names (P5-08). */
    @Data
    public static class MapKeyBean {
        private Map<Integer, String> byId;
    }

    /** Review fixes 2026-09-06: optional/nullable and tuple-like properties (T6-02). */
    @Data
    public static class OptionalBean {
        private Optional<String> os;
        private Optional<Integer> oi;
        private OptionalInt oi2;
        private Nullable<String> n;
        private Pair<String, Integer> p;
        private Tuple2<String, Integer> t;
    }

    @Data
    public static class PrimitiveBean {
        private byte byteVal;
        private short shortVal;
        private int intVal;
        private long longVal;
        private float floatVal;
        private double doubleVal;
        private boolean booleanVal;
        private char charVal;
    }

    @SuppressWarnings("unchecked")
    private static Type<Object> runtimeType(final Object value) {
        return (Type<Object>) (Type<?>) N.typeOf(value.getClass());
    }

    private static Document document(final String xml) throws Exception {
        return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes()));
    }

    @Test
    public void testStaxDocumentWithoutRootElementReportsParsingException() {
        Assertions.assertThrows(ParsingException.class, () -> staxParser.deserialize("<?xml version=\"1.0\"?>", Map.class));
    }

    @Test
    public void testIgnoredMapTextIsNotConvertedToConfiguredValueType() {
        XmlDeserConfig config = XmlDeserConfig.create().setMapValueType(Integer.class).setIgnoredPropNames(Map.class, N.asSet("skip"));
        for (String xml : new String[] { "<map><skip>not-an-integer</skip><keep>7</keep></map>",
                "<map><keep>7</keep><skip type=\"not-a-java-type\"><![CDATA[invalid]]></skip></map>",
                "<map><skip type=\"not-a-java-type\"><nested>invalid</nested></skip><keep>7</keep></map>" }) {
            for (XmlParserImpl parser : new XmlParserImpl[] { staxParser, domParser }) {
                Assertions.assertEquals(Map.of("keep", 7), parser.deserialize(xml, config, Map.class));
                MapEntity entity = parser.deserialize(xml, config, MapEntity.class);
                Assertions.assertEquals(Integer.valueOf(7), entity.<Integer> get("keep"));
                Assertions.assertFalse(entity.containsKey("skip"));
            }
        }
    }

    @Test
    public void testRootCollectionUsesGenericTargetElementType() {
        final Type<List<TestBean>> targetType = Type.of(new TypeReference<List<TestBean>>() {
        });
        final String xml = "<list><testBean><name>A</name><age>1</age></testBean><testBean><name>B</name><age>2</age></testBean></list>";
        for (final XmlParserImpl parser : new XmlParserImpl[] { staxParser, domParser }) {
            final List<TestBean> result = parser.deserialize(xml, null, targetType);
            Assertions.assertEquals(2, result.size());
            Assertions.assertEquals("A", result.get(0).getName());
            Assertions.assertEquals(1, result.get(0).getAge());
            Assertions.assertEquals("B", result.get(1).getName());
            Assertions.assertEquals(2, result.get(1).getAge());
        }
    }

    @Test
    public void testRootMapUsesGenericTargetKeyAndValueTypes() {
        final Type<Map<DayOfWeek, Integer>> targetType = Type.of(new TypeReference<Map<DayOfWeek, Integer>>() {
        });
        for (final XmlParserImpl parser : new XmlParserImpl[] { staxParser, domParser }) {
            Assertions.assertEquals(Map.of(DayOfWeek.MONDAY, 7, DayOfWeek.FRIDAY, 9),
                    parser.deserialize("<map><MONDAY>7</MONDAY><FRIDAY>9</FRIDAY></map>", null, targetType));
        }
    }

    @Test
    public void testRootGenericTypesPreserveExplicitConfigAndNestedValues() {
        for (final XmlParserImpl parser : new XmlParserImpl[] { staxParser, domParser }) {
            final Type<List<Integer>> integers = Type.of("List<Integer>");
            Assertions.assertEquals(List.of(Map.of("name", "A")), parser.deserialize("<list><testBean><name>A</name></testBean></list>",
                    XmlDeserConfig.create().setElementType(Map.class), Type.of(new TypeReference<List<TestBean>>() {
                    })));
            Assertions.assertEquals(List.of(), parser.deserialize("<list/>", null, integers));

            final Type<Map<Integer, Integer>> mapType = Type.of("Map<Integer,Integer>");
            Assertions.assertEquals(Map.of(true, "2"), parser.deserialize("<map><true>2</true></map>",
                    XmlDeserConfig.create().setMapKeyType(Boolean.class).setMapValueType(String.class), mapType));

            final String xml = parser.serialize(Map.of("values", List.of("3", "4")), XmlSerConfig.create().setWriteTypeInfo(false));
            Assertions.assertEquals(Map.of("values", List.of(3, 4)), parser.deserialize(xml, null, Type.of("Map<String,List<Integer>>")));
        }
    }

    @Test
    public void testConstructor() {
        Assertions.assertNotNull(new XmlParserImpl(XmlParserType.StAX));
        Assertions.assertNotNull(new XmlParserImpl(XmlParserType.DOM));
        XmlSerConfig xsc = new XmlSerConfig();
        XmlDeserConfig xdc = new XmlDeserConfig();
        Assertions.assertNotNull(new XmlParserImpl(XmlParserType.StAX, xsc, xdc));
        Assertions.assertNotNull(new XmlParserImpl(XmlParserType.DOM, xsc, xdc));
    }

    @Test
    public void testSerializeNull() throws IOException {
        Assertions.assertEquals("", staxParser.serialize(null));
        Assertions.assertEquals("", domParser.serialize(null));

        final AtomicInteger flushCount = new AtomicInteger();
        final StringWriter writer = new StringWriter() {
            @Override
            public void flush() {
                flushCount.incrementAndGet();
            }
        };
        staxParser.serialize(null, null, writer);
        Assertions.assertEquals(1, flushCount.get());
    }

    @Test
    public void testSerializeBean() {
        TestBean bean = new TestBean("John", 30);
        bean.setActive(true);
        String xml = staxParser.serialize(bean);
        Assertions.assertTrue(xml.contains("John") && xml.contains("30") && xml.contains("true"));

        bean.setTags(Arrays.asList("tag1", "tag2", "tag3"));
        bean.setAttributes(N.asMap("key1", "value1", "key2", "value2"));
        xml = staxParser.serialize(bean);
        Assertions.assertTrue(xml.contains("tag1") && xml.contains("tag3") && xml.contains("key1") && xml.contains("value1"));

        TestBean parent = new TestBean("Parent", 40);
        parent.setTags(Arrays.asList("tag1", "tag2"));
        parent.setNested(new TestBean("Child", 10));
        parent.setMoreNested(Arrays.asList(new TestBean("Child", 10), new TestBean("Child2", 10)));
        xml = staxParser.serialize(parent);
        Assertions.assertTrue(xml.contains("Parent") && xml.contains("Child"));

        bean.setIgnoredField("This should be ignored");
        bean.setRenamedField("Renamed Value");
        xml = staxParser.serialize(bean);
        Assertions.assertFalse(xml.contains("This should be ignored"));
        Assertions.assertTrue(xml.contains("customName") && xml.contains("Renamed Value"));

        PrimitiveBean primitives = new PrimitiveBean();
        primitives.setByteVal((byte) 127);
        primitives.setShortVal((short) 32767);
        primitives.setIntVal(2147483647);
        primitives.setLongVal(9223372036854775807L);
        primitives.setFloatVal(3.14f);
        primitives.setDoubleVal(2.71828);
        primitives.setBooleanVal(true);
        primitives.setCharVal('A');
        xml = staxParser.serialize(primitives);
        Assertions.assertTrue(xml.contains("127") && xml.contains("32767") && xml.contains("2147483647") && xml.contains("9223372036854775807")
                && xml.contains("3.14") && xml.contains("2.71828") && xml.contains("true") && xml.contains("A"));

        TestBean nulls = new TestBean();
        xml = staxParser.serialize(nulls);
        Assertions.assertNotNull(xml);
        xml = staxParser.serialize(nulls, new XmlSerConfig().setExclusion(Exclusion.NONE));
        Assertions.assertTrue(xml.contains("<tags"));
    }

    @Test
    public void testSerializeJsonRawValueWritesPayloadVerbatim() {
        final RawXmlBean bean = new RawXmlBean();
        bean.setName("doc");
        bean.setPayload("{\"k\":\"v\"}");
        for (XmlParserImpl parser : new XmlParserImpl[] { staxParser, domParser }) {
            String xml = parser.serialize(bean);
            Assertions.assertTrue(xml.contains("{\"k\":\"v\"}"), xml);
            Assertions.assertFalse(xml.contains("\\\"k\\\""), xml);
        }
    }

    @Test
    public void testSerializeCollections() {
        int[] intArray = { 1, 2, 3, 4, 5 };
        Assertions.assertTrue(staxParser.serialize(intArray).contains("1") && staxParser.serialize(intArray).contains("5"));
        Assertions.assertTrue(domParser.serialize(intArray).contains("1") && domParser.serialize(intArray).contains("5"));
        String xml = staxParser.serialize(new String[] { "one", "two", "three" });
        Assertions.assertTrue(xml.contains("one") && xml.contains("two") && xml.contains("three"));
        xml = staxParser.serialize(Arrays.asList("apple", "banana", "cherry"));
        Assertions.assertTrue(xml.contains("apple") && xml.contains("banana") && xml.contains("cherry"));
        xml = staxParser.serialize(N.asMap("one", 1, "two", 2, "three", 3));
        Assertions.assertTrue(xml.contains("one") && xml.contains("two") && xml.contains("three"));

        final Map<Object, String> ignored = new HashMap<>();
        ignored.put(new StringBuilder("skip"), "hidden");
        ignored.put(new StringBuilder("keep"), "visible");
        xml = staxParser.serialize(ignored, new XmlSerConfig().setIgnoredPropNames(Map.class, N.asSet("skip")));
        Assertions.assertFalse(xml.contains("skip"), xml);
        Assertions.assertTrue(xml.contains("keep"), xml);

        MapEntity entity = new MapEntity("TestEntity");
        entity.set("prop1", "value1");
        entity.set("prop2", 123);
        entity.set("prop3", true);
        xml = staxParser.serialize(entity);
        Assertions.assertTrue(xml.contains("TestEntity") && xml.contains("prop1") && xml.contains("value1"));
    }

    @Test
    public void testSerializeConfig() {
        TestBean bean = new TestBean("Test", 20);
        Assertions.assertTrue(staxParser.serialize(bean, new XmlSerConfig().setPrettyFormat(true)).contains("\n"));
        Assertions.assertNotNull(staxParser.serialize(bean, new XmlSerConfig().setWriteTypeInfo(true)));
        Assertions.assertNotNull(staxParser.serialize(bean, new XmlSerConfig().setTagByPropertyName(true)));
        String xml = staxParser.serialize(bean, new XmlSerConfig().setPropNamingPolicy(NamingPolicy.SCREAMING_SNAKE_CASE));
        Assertions.assertTrue(xml.contains("NAME") || xml.contains("name"));

        CircularRefBean cycle = new CircularRefBean();
        cycle.setName("cycle");
        cycle.setReference(cycle);
        xml = new XmlParserImpl(XmlParserType.StAX, new XmlSerConfig().setCircularReferenceSupported(true), null).serialize(cycle);
        Assertions.assertTrue(xml.contains("cycle"));

        CircularRefBean bean1 = new CircularRefBean();
        bean1.setName("Bean1");
        CircularRefBean bean2 = new CircularRefBean();
        bean2.setName("Bean2");
        bean1.setReference(bean2);
        bean2.setReference(bean1);
        // review fix 2026-09-06 (P5-15): a cycle without circular-reference support is now reported as a
        // ParsingException by the serialization depth guard instead of unwinding in a StackOverflowError.
        final ParsingException cycleException = Assertions.assertThrows(ParsingException.class, () -> staxParser.serialize(bean1));
        Assertions.assertTrue(cycleException.getMessage().contains("Serialization nesting depth exceeded " + XmlParserImpl.MAX_SERIALIZATION_DEPTH),
                cycleException.getMessage());
        Assertions.assertNotNull(staxParser.serialize(bean1, new XmlSerConfig().setCircularReferenceSupported(true)));

        Assertions.assertThrows(ParsingException.class, () -> staxParser.serialize(new EmptyBean()));
        XmlSerConfig failOff = new XmlSerConfig().setFailOnEmptyBean(false);
        Assertions.assertEquals("", staxParser.serialize(new EmptyBean(), failOff));
        Assertions.assertEquals("", new XmlParserImpl(XmlParserType.StAX, failOff, null).serialize(new EmptyBean()));
    }

    @Test
    public void testRoundTrip() {
        TestBean original = new TestBean("RoundTrip", 45);
        original.setActive(true);
        original.setTags(Arrays.asList("tag1", "tag2"));
        original.setAttributes(N.asMap("key", "value"));
        TestBean restored = staxParser.deserialize(staxParser.serialize(original), null, TestBean.class);
        Assertions.assertEquals(original.getName(), restored.getName());
        Assertions.assertEquals(original.getAge(), restored.getAge());
        Assertions.assertEquals(original.isActive(), restored.isActive());
        Assertions.assertEquals(original.getTags().size(), restored.getTags().size());
        Assertions.assertEquals("value", restored.getAttributes().get("key"));

        original = new TestBean("DOMTest", 55);
        original.setActive(false);
        restored = domParser.deserialize(domParser.serialize(original), null, TestBean.class);
        Assertions.assertEquals("DOMTest", restored.getName());
        Assertions.assertEquals(55, restored.getAge());
        Assertions.assertEquals(false, restored.isActive());

        original = new TestBean("DOMColl", 50);
        original.setTags(Arrays.asList("x", "y", "z"));
        restored = domParser.deserialize(domParser.serialize(original), null, TestBean.class);
        Assertions.assertEquals(3, restored.getTags().size());

        TestBean large = new TestBean("LargeData", 100);
        List<String> largeTags = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            largeTags.add("tag" + i);
        }
        large.setTags(largeTags);
        Map<String, String> largeAttrs = new HashMap<>();
        for (int i = 0; i < 100; i++) {
            largeAttrs.put("key" + i, "value" + i);
        }
        large.setAttributes(largeAttrs);
        String xml = staxParser.serialize(large);
        Assertions.assertTrue(xml.length() > 10000);
        restored = staxParser.deserialize(xml, null, TestBean.class);
        Assertions.assertEquals(1000, restored.getTags().size());
        Assertions.assertEquals(100, restored.getAttributes().size());
    }

    @Test
    public void testSerializeToDestinations() throws IOException {
        TestBean bean = new TestBean("FileTest", 25);
        File file = tempDir.resolve("test.xml").toFile();
        staxParser.serialize(bean, null, file);
        String content = new String(java.nio.file.Files.readAllBytes(file.toPath()));
        Assertions.assertTrue(content.contains("FileTest") && content.contains("25"));

        File nested = tempDir.resolve("subdir/test.xml").toFile();
        staxParser.serialize(bean, null, nested);
        Assertions.assertTrue(nested.exists());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        staxParser.serialize(new TestBean("StreamTest", 30), null, baos);
        Assertions.assertTrue(baos.toString().contains("StreamTest") && baos.toString().contains("30"));

        StringWriter writer = new StringWriter();
        staxParser.serialize(new TestBean("WriterTest", 35), null, writer);
        Assertions.assertTrue(writer.toString().contains("WriterTest") && writer.toString().contains("35"));

        BufferedXmlWriter bw = Objectory.createBufferedXmlWriter();
        try {
            staxParser.serialize(new TestBean("BufferedTest", 40), null, bw);
            Assertions.assertTrue(bw.toString().contains("BufferedTest") && bw.toString().contains("40"));
        } finally {
            Objectory.recycle(bw);
        }
    }

    @Test
    public void testWrite() throws IOException {
        BufferedXmlWriter bw = Objectory.createBufferedXmlWriter();
        try {
            TestBean bean = new TestBean("WriteTest", 50);
            staxParser.writeBean(bean, XmlSerConfig.create(), null, null, runtimeType(bean), bw);
            Assertions.assertTrue(bw.toString().contains("WriteTest") && bw.toString().contains("50"));
        } finally {
            Objectory.recycle(bw);
        }

        bw = Objectory.createBufferedXmlWriter();
        try {
            TestBean bean = new TestBean("PropTest", 77);
            bean.setActive(true);
            staxParser.writeProperties(bean, XmlSerConfig.create().setPrettyFormat(true).setWriteTypeInfo(true), "  ", null, runtimeType(bean), bw);
            Assertions.assertTrue(bw.toString().contains("PropTest") && bw.toString().contains("77"));
        } finally {
            Objectory.recycle(bw);
        }

        bw = Objectory.createBufferedXmlWriter();
        try {
            TestBean bean = new TestBean("TagProp", 88);
            staxParser.writeProperties(bean, XmlSerConfig.create().setTagByPropertyName(true), null, null, runtimeType(bean), bw);
            Assertions.assertTrue(bw.toString().contains("TagProp") && bw.toString().contains("88"));
        } finally {
            Objectory.recycle(bw);
        }

        bw = Objectory.createBufferedXmlWriter();
        try {
            Map<String, String> map = N.asMap("key1", "value1", "key2", "value2");
            staxParser.writeMap(map, XmlSerConfig.create(), null, null, runtimeType(map), bw);
            Assertions.assertTrue(bw.toString().contains("key1") && bw.toString().contains("value2"));
        } finally {
            Objectory.recycle(bw);
        }

        bw = Objectory.createBufferedXmlWriter();
        try {
            MapEntity entity = new MapEntity("TestEntity");
            entity.set("prop1", "value1");
            entity.set("prop2", 123);
            staxParser.writeMapEntity(entity, XmlSerConfig.create(), null, null, runtimeType(entity), bw);
            Assertions.assertTrue(bw.toString().contains("TestEntity") && bw.toString().contains("value1"));
        } finally {
            Objectory.recycle(bw);
        }

        bw = Objectory.createBufferedXmlWriter();
        try {
            MapEntity entity = new MapEntity("PrettyEntity");
            entity.set("field1", "value1");
            entity.set("field2", null);
            staxParser.writeMapEntity(entity, XmlSerConfig.create().setPrettyFormat(true).setWriteTypeInfo(true), "  ", null, runtimeType(entity), bw);
            Assertions.assertTrue(bw.toString().contains("PrettyEntity") && bw.toString().contains("value1"));
        } finally {
            Objectory.recycle(bw);
        }

        bw = Objectory.createBufferedXmlWriter();
        try {
            String[] array = { "one", "two", "three" };
            staxParser.writeArray(array, XmlSerConfig.create(), null, null, runtimeType(array), bw);
            Assertions.assertTrue(bw.toString().contains("one") && bw.toString().contains("three"));
        } finally {
            Objectory.recycle(bw);
        }

        bw = Objectory.createBufferedXmlWriter();
        try {
            TestBean[] array = { new TestBean("X", 10), new TestBean("Y", 20) };
            staxParser.writeArray(array, XmlSerConfig.create(), null, null, runtimeType(array), bw);
            Assertions.assertNotNull(bw.toString());
        } finally {
            Objectory.recycle(bw);
        }

        bw = Objectory.createBufferedXmlWriter();
        try {
            List<String> list = Arrays.asList("apple", "banana", "cherry");
            staxParser.writeCollection(list, XmlSerConfig.create(), null, null, runtimeType(list), bw);
            Assertions.assertTrue(bw.toString().contains("apple") && bw.toString().contains("cherry"));
        } finally {
            Objectory.recycle(bw);
        }

        bw = Objectory.createBufferedXmlWriter();
        try {
            List<TestBean> list = Arrays.asList(new TestBean("A", 1), new TestBean("B", 2));
            staxParser.writeCollection(list, XmlSerConfig.create(), null, null, runtimeType(list), bw);
            Assertions.assertTrue(bw.toString().contains("A") || bw.toString().length() > 0);
        } finally {
            Objectory.recycle(bw);
        }
    }

    @Test
    public void testIsSerializableByJson() {
        Assertions.assertTrue(staxParser.isSerializableByJson(new Integer[] { 1, 2, 3 }));
        Assertions.assertTrue(staxParser.isSerializableByJson(new Object[] { "string", 123, true }));
        Assertions.assertFalse(staxParser.isSerializableByJson(new Object[] { new TestBean(), new TestBean() }));
        Assertions.assertTrue(staxParser.isSerializableByJson(Arrays.asList("a", "b", "c")));
        Assertions.assertFalse(staxParser.isSerializableByJson(Arrays.asList(new TestBean(), new TestBean())));
        Assertions.assertFalse(staxParser.isSerializableByJson(Arrays.asList("string", new TestBean())));
    }

    @Test
    public void testDeserialize() {
        Assertions.assertNull(staxParser.deserialize("", null, TestBean.class));
        Assertions.assertNull(domParser.deserialize("", null, TestBean.class));
        Assertions.assertNull(staxParser.deserialize((String) null, null, TestBean.class));
        Assertions.assertNull(domParser.deserialize((String) null, null, TestBean.class));

        String xml = "<TestBean><name>John</name><age>30</age><active>true</active></TestBean>";
        for (XmlParserImpl parser : new XmlParserImpl[] { staxParser, domParser }) {
            TestBean result = parser.deserialize(xml, null, TestBean.class);
            Assertions.assertEquals("John", result.getName());
            Assertions.assertEquals(30, result.getAge());
            Assertions.assertTrue(result.isActive());
        }

        xml = "<TestBean><name>Jane</name><age>25</age>" + "<tags>[\"tag1\",\"tag2\",\"tag3\"]</tags>"
                + "<attributes><map><key1>value1</key1><key2>value2</key2></map></attributes></TestBean>";
        TestBean result = staxParser.deserialize(xml, null, TestBean.class);
        Assertions.assertEquals("Jane", result.getName());
        Assertions.assertEquals(3, result.getTags().size());
        Assertions.assertEquals("value1", result.getAttributes().get("key1"));

        result = domParser.deserialize(
                "<testBean><name>Parent</name><age>40</age><nested><testBean><name>Child</name><age>10</age></testBean></nested></testBean>", null,
                TestBean.class);
        Assertions.assertEquals("Parent", result.getName());
        Assertions.assertEquals("Child", result.getNested().getName());
        Assertions.assertEquals(10, result.getNested().getAge());

        Integer[] array = staxParser.deserialize("<array>[1,2,3,4,5]</array>", null, Integer[].class);
        Assertions.assertEquals(5, array.length);
        Assertions.assertEquals(1, array[0]);
        Assertions.assertEquals(5, array[4]);

        List<String> list = staxParser.deserialize("<list>[\"apple\",\"banana\",\"cherry\"]</list>", null, List.class);
        Assertions.assertEquals(3, list.size());
        Assertions.assertTrue(list.contains("apple"));

        Map<String, Integer> map = staxParser.deserialize("<map><one>1</one><two>2</two><three>3</three></map>", null, Map.class);
        Assertions.assertEquals("1", map.get("one"));
        Assertions.assertEquals("3", map.get("three"));

        MapEntity entity = staxParser.deserialize("<TestEntity><prop1>value1</prop1><prop2>123</prop2><prop3>true</prop3></TestEntity>", null, MapEntity.class);
        Assertions.assertEquals("TestEntity", entity.entityName());
        Assertions.assertEquals("value1", entity.get("prop1"));

        PrimitiveBean primitives = staxParser.deserialize("<PrimitiveBean>" + "<byteVal>127</byteVal>" + "<shortVal>32767</shortVal>"
                + "<intVal>2147483647</intVal>" + "<longVal>9223372036854775807</longVal>" + "<floatVal>3.14</floatVal>" + "<doubleVal>2.71828</doubleVal>"
                + "<booleanVal>true</booleanVal>" + "<charVal>A</charVal>" + "</PrimitiveBean>", null, PrimitiveBean.class);
        Assertions.assertEquals((byte) 127, primitives.getByteVal());
        Assertions.assertEquals(2147483647, primitives.getIntVal());
        Assertions.assertEquals(9223372036854775807L, primitives.getLongVal());
        Assertions.assertEquals(3.14f, primitives.getFloatVal(), 0.001);
        Assertions.assertEquals(2.71828, primitives.getDoubleVal(), 0.00001);
        Assertions.assertTrue(primitives.isBooleanVal());
        Assertions.assertEquals('A', primitives.getCharVal());

        result = staxParser.deserialize("<TestBean><name>TypeAnnot</name><age>99</age></TestBean>", null, TestBean.class);
        Assertions.assertEquals("TypeAnnot", result.getName());

        final String unmatchedXml = "<TestBean><name>John</name><age>30</age><unknownProp>value</unknownProp></TestBean>";
        Assertions.assertThrows(ParsingException.class,
                () -> staxParser.deserialize(unmatchedXml, XmlDeserConfig.create().setIgnoreUnmatchedProperty(false), TestBean.class));
        result = staxParser.deserialize(unmatchedXml, XmlDeserConfig.create().setIgnoreUnmatchedProperty(true), TestBean.class);
        Assertions.assertEquals("John", result.getName());

        Assertions.assertThrows(Exception.class, () -> staxParser.deserialize("<TestBean><name>Unclosed", null, TestBean.class));
        Assertions.assertThrows(Exception.class, () -> domParser.deserialize("<TestBean><name>Unclosed", null, TestBean.class));
        Assertions.assertThrows(ParsingException.class, () -> staxParser.deserialize("<AtomicInteger>123</AtomicInteger>", null, AtomicInteger.class));
    }

    @Test
    public void testDeserializeFromSources() throws Exception {
        String xml = "<TestBean><name>FileTest</name><age>25</age></TestBean>";
        File file = tempDir.resolve("input.xml").toFile();
        java.nio.file.Files.write(file.toPath(), xml.getBytes());
        for (XmlParserImpl parser : new XmlParserImpl[] { staxParser, domParser }) {
            TestBean result = parser.deserialize(file, null, TestBean.class);
            Assertions.assertEquals("FileTest", result.getName());
            Assertions.assertEquals(25, result.getAge());
        }

        xml = "<TestBean><name>StreamTest</name><age>30</age></TestBean>";
        ByteArrayInputStream bais = new ByteArrayInputStream(xml.getBytes());
        TestBean result = staxParser.deserialize(bais, null, TestBean.class);
        Assertions.assertEquals("StreamTest", result.getName());
        bais.reset();
        result = domParser.deserialize(bais, null, TestBean.class);
        Assertions.assertEquals("StreamTest", result.getName());

        xml = "<TestBean><name>ReaderTest</name><age>35</age></TestBean>";
        result = staxParser.deserialize(new StringReader(xml), null, TestBean.class);
        Assertions.assertEquals("ReaderTest", result.getName());
        result = domParser.deserialize(new StringReader(xml), null, TestBean.class);
        Assertions.assertEquals("ReaderTest", result.getName());

        xml = "<TestBean><name>NodeTest</name><age>40</age></TestBean>";
        Node node = document(xml).getDocumentElement();
        result = staxParser.deserialize(node, null, TestBean.class);
        Assertions.assertEquals("NodeTest", result.getName());
        result = domParser.deserialize(node, null, TestBean.class);
        Assertions.assertEquals("NodeTest", result.getName());
    }

    @Test
    public void testDeserializeNodeClasses() throws Exception {
        String xml = "<bean><name>Test</name><age>25</age></bean>";
        Map<String, Type<?>> nodeClasses = new HashMap<>();
        nodeClasses.put("bean", Type.of(TestBean.class));

        TestBean result = staxParser.deserialize(new ByteArrayInputStream(xml.getBytes()), null, nodeClasses);
        Assertions.assertEquals("Test", result.getName());
        result = staxParser.deserialize(new StringReader(xml), null, nodeClasses);
        Assertions.assertEquals("Test", result.getName());
        result = staxParser.deserialize(document(xml).getDocumentElement(), null, nodeClasses);
        Assertions.assertEquals("Test", result.getName());

        Assertions.assertThrows(ParsingException.class,
                () -> staxParser.deserialize(new ByteArrayInputStream("<unknown><name>Test</name><age>25</age></unknown>".getBytes()), null, nodeClasses));
        final String beanXml = xml;
        Assertions.assertThrows(ParsingException.class, () -> staxParser.deserialize(new ByteArrayInputStream(beanXml.getBytes()), null, new HashMap<>()));

        xml = "<testBean><name>NodeClassTest</name><age>22</age></testBean>";
        Map<String, Type<?>> testBeanNodes = N.asMap("testBean", Type.of(TestBean.class));
        result = staxParser.deserialize(new ByteArrayInputStream(xml.getBytes()), null, testBeanNodes);
        Assertions.assertEquals("NodeClassTest", result.getName());
        result = staxParser.deserialize(new ByteArrayInputStream("<testBean><name>DomNodeClass</name><age>33</age></testBean>".getBytes()), null,
                testBeanNodes);
        Assertions.assertEquals("DomNodeClass", result.getName());
        result = staxParser.deserialize(new ByteArrayInputStream("<testBean><name>StaxNodeClass</name><age>36</age></testBean>".getBytes()), null,
                testBeanNodes);
        Assertions.assertEquals("StaxNodeClass", result.getName());
        result = domParser.deserialize(new ByteArrayInputStream("<testBean><name>DomNodeClass</name><age>33</age></testBean>".getBytes()), null, testBeanNodes);
        Assertions.assertEquals("DomNodeClass", result.getName());
        result = domParser.deserialize(new StringReader("<testBean><name>ReaderDomNode</name><age>44</age></testBean>"), null, testBeanNodes);
        Assertions.assertEquals("ReaderDomNode", result.getName());
    }

    @Test
    public void testDeserializeObject_UsesConfiguredRootValueType() throws Exception {
        String xml = "<bean><name>TypedRoot</name><age>42</age></bean>";
        XmlDeserConfig config = new XmlDeserConfig().setValueType("bean", TestBean.class);
        XMLStreamReader xmlReader = XMLInputFactory.newFactory().createXMLStreamReader(new StringReader(xml));
        while (xmlReader.hasNext() && xmlReader.getEventType() != XMLStreamConstants.START_ELEMENT) {
            xmlReader.next();
        }
        Object staxResult = staxParser.readByStreamParser(xmlReader, config, Type.of(Object.class));
        Object domResult = domParser.readByDOMParser(document(xml).getFirstChild(), config, Type.of(Object.class));
        Assertions.assertTrue(staxResult instanceof TestBean);
        Assertions.assertTrue(domResult instanceof MapEntity);
        Assertions.assertEquals("TypedRoot", ((TestBean) staxResult).getName());
        Assertions.assertEquals("42", ((MapEntity) domResult).get("age"));
    }

    @Test
    public void testReadByStreamParser() throws Exception {
        XMLStreamReader xmlReader = XMLInputFactory.newFactory()
                .createXMLStreamReader(new StringReader("<TestBean><name>StreamBean</name><age>55</age><active>true</active></TestBean>"));
        while (xmlReader.hasNext() && xmlReader.getEventType() != XMLStreamConstants.START_ELEMENT) {
            xmlReader.next();
        }
        TestBean bean = staxParser.readByStreamParser(xmlReader, new XmlDeserConfig(), Type.of(TestBean.class));
        Assertions.assertEquals("StreamBean", bean.getName());
        Assertions.assertEquals(55, bean.getAge());
        Assertions.assertTrue(bean.isActive());

        xmlReader = XMLInputFactory.newFactory().createXMLStreamReader(new StringReader("<map><key1>val1</key1><key2>val2</key2></map>"));
        while (xmlReader.hasNext() && xmlReader.getEventType() != XMLStreamConstants.START_ELEMENT) {
            xmlReader.next();
        }
        Map result = staxParser.readByStreamParser(xmlReader, new XmlDeserConfig(), Type.of(Map.class));
        Assertions.assertEquals("val1", result.get("key1"));
        Assertions.assertEquals("val2", result.get("key2"));
    }

    @Test
    public void testReadByDOMParser() throws Exception {
        TestBean bean = domParser.readByDOMParser(document("<TestBean><name>DOMBean</name><age>66</age></TestBean>").getDocumentElement(), new XmlDeserConfig(),
                Type.of(TestBean.class));
        Assertions.assertEquals("DOMBean", bean.getName());
        Assertions.assertEquals(66, bean.getAge());

        bean = domParser.deserialize("<TestBean><?before value?><name>DOMBean</name><!-- between --><age>66</age></TestBean>", TestBean.class);
        Assertions.assertEquals("DOMBean", bean.getName());
        Assertions.assertEquals(66, bean.getAge());

        // A <property> element without the `name` attribute falls back to its own element name (G01-18), so it
        // is an unmatched property: skipped by default, reported under setIgnoreUnmatchedProperty(false).
        final String namelessProperty = "<bean name=\"TestBean\"><property>DOMBean</property><property name=\"age\">66</property></bean>";
        bean = domParser.deserialize(namelessProperty, new XmlDeserConfig().setIgnoreUnmatchedProperty(true), TestBean.class);
        Assertions.assertNull(bean.getName());
        Assertions.assertEquals(66, bean.getAge());

        ParsingException exception = Assertions.assertThrows(ParsingException.class,
                () -> domParser.deserialize(namelessProperty, new XmlDeserConfig().setIgnoreUnmatchedProperty(false), TestBean.class));
        Assertions.assertTrue(exception.getMessage().contains("Unknown property element: property"), exception.getMessage());

        Map result = domParser.readByDOMParser(document("<map><alpha>1</alpha><beta>2</beta></map>").getDocumentElement(), new XmlDeserConfig(),
                Type.of(Map.class));
        Assertions.assertEquals("1", result.get("alpha"));
        Assertions.assertEquals("2", result.get("beta"));
    }

    @Test
    public void testIgnoredProps_EdgeCase() {
        XmlDeserConfig cfg = new XmlDeserConfig().setIgnoredPropNames(TestBean.class, N.asSet("name"));
        TestBean result = staxParser.deserialize("<testBean><name>abc</name><age>30</age></testBean>", cfg, TestBean.class);
        Assertions.assertNull(result.getName());
        Assertions.assertEquals(30, result.getAge());
        TestBean domResult = domParser.deserialize("<testBean><name>abc</name><age>30</age></testBean>", cfg, TestBean.class);
        Assertions.assertNull(domResult.getName());
        Assertions.assertEquals(30, domResult.getAge());

        result = staxParser.deserialize("<testBean><age>30</age><name>abc</name><renamedField>keep</renamedField></testBean>", cfg, TestBean.class);
        Assertions.assertNull(result.getName());
        Assertions.assertEquals(30, result.getAge());
    }

    @Test
    public void testNullElements_EdgeCase() {
        List<TestBean> beans = new ArrayList<>();
        beans.add(new TestBean("a", 1));
        beans.add(null);
        beans.add(new TestBean("b", 2));
        List<TestBean> result = staxParser.deserialize(staxParser.serialize(beans), new XmlDeserConfig().setElementType(TestBean.class), List.class);
        Assertions.assertEquals(3, result.size());
        Assertions.assertEquals("a", result.get(0).getName());
        Assertions.assertNull(result.get(1));
        Assertions.assertEquals("b", result.get(2).getName());

        TestBean[] array = staxParser.deserialize(staxParser.serialize(new TestBean[] { new TestBean("a", 1), null, new TestBean("b", 2) }),
                new XmlDeserConfig().setElementType(TestBean.class), TestBean[].class);
        Assertions.assertEquals(3, array.length);
        Assertions.assertEquals("a", array[0].getName());
        Assertions.assertNull(array[1]);
        Assertions.assertEquals("b", array[2].getName());
    }

    @Test
    public void testStrayTextAndCdata_EdgeCase() {
        Map<String, Object> result = staxParser.deserialize("<map><a>1</a>tail</map>", Map.class);
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals("1", result.get("a"));
        Assertions.assertTrue(staxParser.deserialize("<map>abc</map>", Map.class).isEmpty());

        MapEntity entity = staxParser.deserialize("<m><a>1</a>tail</m>", MapEntity.class);
        Assertions.assertEquals("1", entity.get("a"));
        Assertions.assertTrue(staxParser.deserialize("<x>abc</x>", MapEntity.class).keySet().isEmpty());

        String xml = "<testBean><name><![CDATA[John & Jane]]></name><age>5</age></testBean>";
        TestBean bean = staxParser.deserialize(xml, TestBean.class);
        Assertions.assertEquals("John & Jane", bean.getName());
        Assertions.assertEquals(5, bean.getAge());
        bean = domParser.deserialize(xml, TestBean.class);
        Assertions.assertEquals("John & Jane", bean.getName());
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
        final com.landawn.abacus.exception.UncheckedIOException ex = Assertions.assertThrows(com.landawn.abacus.exception.UncheckedIOException.class,
                () -> staxParser.deserialize(failingReader, new XmlDeserConfig(), Map.class));
        Assertions.assertTrue(ex.getCause() instanceof IOException);
    }

    @Test
    public void testSharedReferenceIsNotMisdetectedAsCycle() {
        final Map<String, Object> shared = N.asMap("name", "x");
        final Map<String, Object> root = new java.util.LinkedHashMap<>();
        root.put("a1", shared);
        root.put("a2", shared);
        final String xml = staxParser.serialize(root, new XmlSerConfig().setCircularReferenceSupported(true));
        Assertions.assertTrue(xml.contains("<a1><map><name>x</name></map></a1>"), xml);
        Assertions.assertTrue(xml.contains("<a2><map><name>x</name></map></a2>"), xml);
    }

    @Test
    public void testValueFormattingPropagatesToJsonBackedCollections() {
        final String xml = staxParser.serialize(Arrays.asList(new Date(0), new BigDecimal("1E+3")),
                new XmlSerConfig().setDateTimeFormat(DateTimeFormat.ISO_8601_DATE_TIME).setWriteBigDecimalAsPlain(true));
        Assertions.assertTrue(xml.contains("T"), xml);
        Assertions.assertFalse(xml.contains("[0,"), xml);
        Assertions.assertTrue(xml.contains("1000"), xml);
        Assertions.assertFalse(xml.contains("1E+3"), xml);
    }

    private static OuterBean newOuterBean() {
        final OuterBean bean = new OuterBean();
        bean.setName("n");
        bean.setInner(new NullableOnlyBean());
        final NullableOnlyBean filled = new NullableOnlyBean();
        filled.setA("x");
        bean.setInners(Arrays.asList(filled));
        bean.setMap(N.asMap("k", 1));
        bean.setStrs(Arrays.asList("a", "b"));
        bean.setTail("t");
        return bean;
    }

    /**
     * P5-01: a pretty-printed bean holding a nested bean with no written property must round trip. The
     * whitespace between the nested start and end tags used to be treated as scalar text of the OUTER
     * property, so the outer PropInfo received the nested instance and threw ClassCastException.
     */
    @Test
    public void reviewFixes20260906_prettyEmptyNestedBeanRoundTrips() {
        final OuterBean original = newOuterBean();

        for (final Exclusion exclusion : new Exclusion[] { Exclusion.NULL, Exclusion.DEFAULT, Exclusion.NONE }) {
            for (final boolean pretty : new boolean[] { false, true }) {
                final XmlSerConfig config = XmlSerConfig.create().setPrettyFormat(pretty).setExclusion(exclusion);
                final String xml = staxParser.serialize(original, config);

                for (final XmlParserImpl parser : new XmlParserImpl[] { staxParser, domParser }) {
                    final String label = exclusion + "/pretty=" + pretty + "/" + parser + " " + xml;
                    final OuterBean back = parser.deserialize(xml, null, OuterBean.class);
                    Assertions.assertEquals("n", back.getName(), label);
                    Assertions.assertNotNull(back.getInner(), label);
                    Assertions.assertNull(back.getInner().getA(), label);
                    Assertions.assertNull(back.getInner().getB(), label);
                    Assertions.assertEquals("t", back.getTail(), label);
                    Assertions.assertEquals(1, back.getInners().size(), label);
                    Assertions.assertEquals("x", back.getInners().get(0).getA(), label);
                    Assertions.assertEquals(1, back.getMap().get("k"), label);
                    Assertions.assertEquals(Arrays.asList("a", "b"), back.getStrs(), label);
                }
            }
        }

        // Hand-written equivalents of the whitespace-only nested element, plus a CDATA variant.
        for (final String body : new String[] { "<nullableOnlyBean>\n  </nullableOnlyBean>", "<nullableOnlyBean><![CDATA[  ]]></nullableOnlyBean>",
                "<nullableOnlyBean></nullableOnlyBean>", "<nullableOnlyBean/>" }) {
            final String xml = "<outerBean><name>n</name><inner>" + body + "</inner><tail>t</tail></outerBean>";

            for (final XmlParserImpl parser : new XmlParserImpl[] { staxParser, domParser }) {
                final OuterBean back = parser.deserialize(xml, null, OuterBean.class);
                Assertions.assertNotNull(back.getInner(), body);
                Assertions.assertNull(back.getInner().getA(), body);
                Assertions.assertEquals("t", back.getTail(), body);
            }
        }

        // Text inside the nested bean element is not a property value; it is ignored, as the DOM backend does.
        final String junk = "<outerBean><name>n</name><inner><nullableOnlyBean>junk</nullableOnlyBean></inner><tail>t</tail></outerBean>";
        for (final XmlParserImpl parser : new XmlParserImpl[] { staxParser, domParser }) {
            final OuterBean back = parser.deserialize(junk, null, OuterBean.class);
            Assertions.assertNotNull(back.getInner());
            Assertions.assertEquals("t", back.getTail());
        }
    }

    /**
     * P5-02/P5-03: {@code tagByPropertyName=false} output must be readable again. The StAX reader ignored the
     * {@code name} attribute (every property was silently dropped) and the DOM reader demanded one on map
     * entries ("Missing 'name' attribute on XML element: k").
     */
    @Test
    public void reviewFixes20260906_tagByPropertyNameFalseRoundTrips() {
        final OuterBean original = newOuterBean();

        for (final boolean typeInfo : new boolean[] { false, true }) {
            for (final boolean pretty : new boolean[] { false, true }) {
                final XmlSerConfig config = XmlSerConfig.create().setTagByPropertyName(false).setWriteTypeInfo(typeInfo).setPrettyFormat(pretty);

                for (final XmlParserImpl writer : new XmlParserImpl[] { staxParser, domParser }) {
                    final String xml = writer.serialize(original, config);
                    Assertions.assertTrue(xml.contains("<property name=\"name\""), xml);

                    for (final XmlParserImpl parser : new XmlParserImpl[] { staxParser, domParser }) {
                        final String label = "typeInfo=" + typeInfo + "/pretty=" + pretty + " " + xml;
                        final OuterBean back = parser.deserialize(xml, null, OuterBean.class);
                        Assertions.assertEquals("n", back.getName(), label);
                        Assertions.assertEquals("t", back.getTail(), label);
                        Assertions.assertNotNull(back.getInner(), label);
                        Assertions.assertEquals(1, back.getInners().size(), label);
                        Assertions.assertEquals("x", back.getInners().get(0).getA(), label);
                        Assertions.assertEquals(1, back.getMap().get("k"), label);
                        Assertions.assertEquals(Arrays.asList("a", "b"), back.getStrs(), label);
                    }
                }
            }
        }

        // An unmatched <property name="..."> element is still reported when unmatched properties are rejected.
        final String unknown = "<bean name=\"outerBean\"><property name=\"nope\">1</property></bean>";
        Assertions.assertThrows(ParsingException.class,
                () -> staxParser.deserialize(unknown, XmlDeserConfig.create().setIgnoreUnmatchedProperty(false), OuterBean.class));
        Assertions.assertNull(staxParser.deserialize(unknown, XmlDeserConfig.create().setIgnoreUnmatchedProperty(true), OuterBean.class).getName());

        // Untyped read: the map entity keeps the real property names, not the literal element name "property".
        final String xml = staxParser.serialize(original, XmlSerConfig.create().setTagByPropertyName(false));
        for (final XmlParserImpl parser : new XmlParserImpl[] { staxParser, domParser }) {
            final MapEntity entity = parser.deserialize(xml, null, MapEntity.class);
            Assertions.assertTrue(entity.containsKey("name"), entity.keySet().toString());
            Assertions.assertEquals("n", entity.get("name"));
            Assertions.assertFalse(entity.containsKey("property"), entity.keySet().toString());
        }
    }

    /**
     * P5-04: with {@code writeTypeInfo=true} the DOM backend read a {@code List<Bean>} property back as a list
     * of HashMaps, because the resolved {@code type} attribute was reduced to a raw Class.
     */
    @Test
    public void reviewFixes20260906_writeTypeInfoKeepsGenericElementTypes() {
        final TestBean original = new TestBean("outer", 1);
        final TestBean child = new TestBean("child", 2);
        original.setNested(child);
        original.setMoreNested(Arrays.asList(child));

        for (final boolean pretty : new boolean[] { false, true }) {
            final XmlSerConfig config = XmlSerConfig.create().setWriteTypeInfo(true).setPrettyFormat(pretty);

            for (final XmlParserImpl writer : new XmlParserImpl[] { staxParser, domParser }) {
                final String xml = writer.serialize(original, config);

                for (final XmlParserImpl parser : new XmlParserImpl[] { staxParser, domParser }) {
                    final TestBean back = parser.deserialize(xml, null, TestBean.class);
                    Assertions.assertEquals(1, back.getMoreNested().size(), xml);
                    Assertions.assertEquals(TestBean.class, back.getMoreNested().get(0).getClass(), xml);
                    Assertions.assertEquals("child", back.getMoreNested().get(0).getName(), xml);
                    Assertions.assertEquals(2, back.getMoreNested().get(0).getAge(), xml);
                    Assertions.assertEquals("child", back.getNested().getName(), xml);
                }
            }
        }

        // A type attribute without type arguments must not replace the declared List<TestBean>.
        final String rawTypeXml = "<testBean><name>outer</name><moreNested type=\"ArrayList\">"
                + "<testBean><name>child</name><age>2</age></testBean></moreNested></testBean>";
        for (final XmlParserImpl parser : new XmlParserImpl[] { staxParser, domParser }) {
            final TestBean back = parser.deserialize(rawTypeXml, null, TestBean.class);
            Assertions.assertEquals(TestBean.class, back.getMoreNested().get(0).getClass());
            Assertions.assertEquals("child", back.getMoreNested().get(0).getName());
        }
    }

    /**
     * P5-05: scalar elements of a mixed (bean + scalar) array or collection used to be written as bare text, so
     * {@code "s"} and {@code 1} fused into the single token {@code s1} and were lost on read. They are wrapped
     * in {@code <e>} now.
     */
    @Test
    public void reviewFixes20260906_mixedArrayAndCollectionElementsAreDelimited() {
        final TestBean inner = new TestBean("i", 3);
        final MixedBean bean = new MixedBean();
        bean.setObjs(new Object[] { inner, "s", 1, null });
        bean.setLos(Arrays.asList(inner, "s", 2));

        for (final boolean pretty : new boolean[] { false, true }) {
            for (final XmlParserImpl writer : new XmlParserImpl[] { staxParser, domParser }) {
                final String xml = writer.serialize(bean, XmlSerConfig.create().setPrettyFormat(pretty));
                Assertions.assertTrue(xml.contains("<e>s</e>"), xml);
                Assertions.assertTrue(xml.contains("<e>1</e>"), xml);
                Assertions.assertFalse(xml.contains("s1"), xml);

                for (final XmlParserImpl parser : new XmlParserImpl[] { staxParser, domParser }) {
                    final MixedBean back = parser.deserialize(xml, null, MixedBean.class);
                    Assertions.assertEquals(4, back.getObjs().length, xml);
                    Assertions.assertInstanceOf(Map.class, back.getObjs()[0], xml);
                    Assertions.assertEquals("i", ((Map<?, ?>) back.getObjs()[0]).get("name"), xml);
                    Assertions.assertEquals("s", back.getObjs()[1], xml);
                    Assertions.assertEquals("1", back.getObjs()[2], xml);
                    Assertions.assertNull(back.getObjs()[3], xml);
                    Assertions.assertEquals(3, back.getLos().size(), xml);
                    Assertions.assertEquals("s", back.getLos().get(1), xml);
                    Assertions.assertEquals("2", back.getLos().get(2), xml);
                }
            }
        }

        // With type information each <e> carries its own type, so the scalars keep their Java types.
        final String typedXml = staxParser.serialize(bean, XmlSerConfig.create().setWriteTypeInfo(true));
        Assertions.assertTrue(typedXml.contains("<e type=\"String\">s</e>"), typedXml);
        Assertions.assertTrue(typedXml.contains("<e type=\"Integer\">1</e>"), typedXml);
        for (final XmlParserImpl parser : new XmlParserImpl[] { staxParser, domParser }) {
            final MixedBean back = parser.deserialize(typedXml, null, MixedBean.class);
            Assertions.assertEquals("s", back.getObjs()[1], typedXml);
            Assertions.assertEquals(1, back.getObjs()[2], typedXml);
            Assertions.assertNull(back.getObjs()[3], typedXml);
        }

        // Root-level mixed array and collection.
        for (final boolean typeInfo : new boolean[] { false, true }) {
            final XmlSerConfig config = XmlSerConfig.create().setWriteTypeInfo(typeInfo);
            final String arrayXml = staxParser.serialize(new Object[] { inner, "s", 1, null }, config);
            final String listXml = staxParser.serialize(Arrays.asList(inner, "s", 2), config);

            for (final XmlParserImpl parser : new XmlParserImpl[] { staxParser, domParser }) {
                final Object[] backArray = parser.deserialize(arrayXml, null, Object[].class);
                Assertions.assertEquals(4, backArray.length, arrayXml);
                Assertions.assertNotNull(backArray[0], arrayXml);
                Assertions.assertEquals("s", backArray[1], arrayXml);
                Assertions.assertEquals(typeInfo ? (Object) 1 : (Object) "1", backArray[2], arrayXml);
                Assertions.assertNull(backArray[3], arrayXml);

                final List<?> backList = parser.deserialize(listXml, null, List.class);
                Assertions.assertEquals(3, backList.size(), listXml);
                Assertions.assertEquals("s", backList.get(1), listXml);
                Assertions.assertEquals(typeInfo ? (Object) 2 : (Object) "2", backList.get(2), listXml);
            }
        }

        // An all-serializable collection is still written as one JSON payload, not as <e> elements.
        final String jsonXml = staxParser.serialize(Arrays.asList("a", "b"));
        Assertions.assertFalse(jsonXml.contains("<e>"), jsonXml);
        Assertions.assertEquals(Arrays.asList("a", "b"), staxParser.deserialize(jsonXml, null, List.class));

        // Hand-written <e> elements are read as scalars by both backends: as Strings without type information,
        // and as the named type with it.
        for (final XmlParserImpl parser : new XmlParserImpl[] { staxParser, domParser }) {
            final String untyped = "<list><e>s</e><e>2</e></list>";
            Assertions.assertEquals(Arrays.asList("s", "2"), parser.deserialize(untyped, null, List.class), untyped);

            final String typed = "<list><e type=\"String\">s</e><e type=\"Integer\">2</e></list>";
            Assertions.assertEquals(Arrays.asList("s", 2), parser.deserialize(typed, null, List.class), typed);
        }
    }

    /**
     * G05-108: the two backends applied different rules for recognising the {@code <e>} scalar wrapper - the DOM
     * reader also required the element to have no children, which a stream reader cannot test - so a hand-written
     * {@code <e>} with child elements read as text through StAX and as a nested container through DOM. The DOM
     * reader now applies the documented name-based rule everywhere the StAX reader does, including the root
     * array/collection path, which had no {@code <e>} handling of its own at all.
     */
    @Test
    public void reviewFixes20260908_scalarEleWrapperIsRecognisedAlikeByBothBackends() {
        for (final XmlParserImpl parser : new XmlParserImpl[] { staxParser, domParser }) {
            final String mixed = "<list><e><sub>1</sub></e></list>";
            Assertions.assertEquals(Arrays.asList("1"), parser.deserialize(mixed, null, List.class), mixed);

            final String mixedArray = "<array><e><sub>1</sub></e><e>2</e></array>";
            Assertions.assertArrayEquals(new Object[] { "1", "2" }, parser.deserialize(mixedArray, null, Object[].class), mixedArray);

            // a declared element type that is a bean, map or map entity still wins over the name
            final String beanElement = "<list><e><name>i</name></e></list>";
            final List<?> asBeans = parser.deserialize(beanElement, XmlDeserConfig.create().setElementType(TestBean.class), List.class);
            Assertions.assertEquals("i", ((TestBean) asBeans.get(0)).getName(), beanElement);

            // scalar wrappers keep reading as before, with and without a type attribute
            Assertions.assertEquals(Arrays.asList("s", "2"), parser.deserialize("<list><e>s</e><e>2</e></list>", null, List.class));
            Assertions.assertEquals(Arrays.asList("s", 2),
                    parser.deserialize("<list><e type=\"String\">s</e><e type=\"Integer\">2</e></list>", null, List.class));
            // ... including a primitive type name, which the DOM reader used to drop back to String
            Assertions.assertEquals(Arrays.asList(1, 2L), parser.deserialize("<list><e type=\"int\">1</e><e type=\"long\">2</e></list>", null, List.class));
            // the null marker still wins over the wrapper
            Assertions.assertEquals(Arrays.asList(null, "1"), parser.deserialize("<list><e isNull=\"true\"/><e>1</e></list>", null, List.class));
        }
    }

    /**
     * P5-06: an {@code isJsonRawValue} payload was written without any escaping, so any JSON holding
     * {@code <}, {@code &} or {@code ]]>} produced a document neither backend could read.
     */
    @Test
    public void reviewFixes20260906_rawJsonPayloadStaysWellFormed() {
        final String euro = "{\"k\":\"" + (char) 0x20AC + "\"}";

        for (final String payload : new String[] { "{\"k\":\"v\"}", "{\"k\":\"a<b\"}", "{\"k\":\"a&b\"}", "{\"k\":\"]]>\"}", "{\"k\":\"&amp;\"}", euro }) {
            final RawXmlBean bean = new RawXmlBean();
            bean.setName("doc");
            bean.setPayload(payload);

            for (final XmlParserImpl writer : new XmlParserImpl[] { staxParser, domParser }) {
                final String xml = writer.serialize(bean);

                for (final XmlParserImpl parser : new XmlParserImpl[] { staxParser, domParser }) {
                    Assertions.assertEquals(payload, parser.deserialize(xml, null, RawXmlBean.class).getPayload(), xml);
                }
            }
        }

        // Quotes stay verbatim (only the three markup characters are escaped).
        final RawXmlBean bean = new RawXmlBean();
        bean.setPayload("{\"k\":\"a<b&c\"}");
        final String xml = staxParser.serialize(bean);
        Assertions.assertTrue(xml.contains("{\"k\":\"a&lt;b&amp;c\"}"), xml);
        Assertions.assertFalse(xml.contains("&quot;"), xml);
    }

    /**
     * P5-07: text XML 1.0 cannot carry was written as a numeric character reference (or raw, for surrogates),
     * producing a document that no XML reader accepts. It is now rejected while serializing.
     */
    @Test
    public void reviewFixes20260906_xmlIllegalCharactersAreRejected() {
        final String[] illegal = { "ctl" + (char) 1 + "x", "nul" + (char) 0 + "x", "ff" + (char) 12 + "x", "lone" + (char) 0xD800 + "sur",
                "fffe" + (char) 0xFFFE, "ffff" + (char) 0xFFFF };

        for (final String value : illegal) {
            final TestBean bean = new TestBean(value, 1);

            for (final XmlParserImpl parser : new XmlParserImpl[] { staxParser, domParser }) {
                final ParsingException exception = Assertions.assertThrows(ParsingException.class, () -> parser.serialize(bean));
                Assertions.assertTrue(exception.getMessage().startsWith("Property 'name' contains U+"), exception.getMessage());
                Assertions.assertTrue(exception.getMessage().endsWith("which cannot be represented in XML 1.0"), exception.getMessage());
            }
        }

        // Legal text still round trips, including the characters that look suspicious.
        final String[] legal = { "ok\t\n\r", "del" + (char) 0x7F, "nel" + (char) 0x85, "sep" + (char) 0x2028, "bom" + (char) 0xFEFF,
                "emoji" + new String(Character.toChars(0x1F600)), "plain" };

        for (final String value : legal) {
            final TestBean bean = new TestBean(value, 1);

            for (final XmlParserImpl parser : new XmlParserImpl[] { staxParser, domParser }) {
                Assertions.assertEquals(value, parser.deserialize(parser.serialize(bean), null, TestBean.class).getName(), value);
            }
        }

        // A map value, an array element and a collection element are checked as well.
        Assertions.assertThrows(ParsingException.class, () -> staxParser.serialize(N.asMap("k", "v" + (char) 1)));
        Assertions.assertThrows(ParsingException.class, () -> staxParser.serialize(new Object[] { new TestBean("a", 1), "v" + (char) 1 }));
        Assertions.assertThrows(ParsingException.class, () -> staxParser.serialize(Arrays.asList(new TestBean("a", 1), "v" + (char) 1)));

        // An unset char has no XML representation either; it is written as an empty element and read back as 0.
        final PrimitiveBean primitives = new PrimitiveBean();
        final String xml = staxParser.serialize(primitives, XmlSerConfig.create().setExclusion(Exclusion.NONE));
        Assertions.assertTrue(xml.contains("<charVal></charVal>"), xml);
        for (final XmlParserImpl parser : new XmlParserImpl[] { staxParser, domParser }) {
            Assertions.assertEquals((char) 0, parser.deserialize(xml, null, PrimitiveBean.class).getCharVal(), xml);
        }
    }

    /**
     * P5-08: a map key becomes an element name, and XML has no escaping in name position, so a key that is not
     * an NCName silently produced an unreadable document ({@code <map><1>a</1></map>}).
     */
    @Test
    public void reviewFixes20260906_mapKeysMustBeValidXmlNames() {
        for (final XmlParserImpl parser : new XmlParserImpl[] { staxParser, domParser }) {
            final ParsingException exception = Assertions.assertThrows(ParsingException.class, () -> parser.serialize(N.asMap(1, "a")));
            Assertions.assertEquals("Map key '1' is not a valid XML element name", exception.getMessage());

            for (final String badKey : new String[] { "a b", "", "a<b", "a&b", "1x", "x:y", ".dot", "-dash" }) {
                final ParsingException failure = Assertions.assertThrows(ParsingException.class, () -> parser.serialize(N.asMap(badKey, "v")), badKey);
                Assertions.assertEquals("Map key '" + badKey + "' is not a valid XML element name", failure.getMessage());
            }

            // As a bean property, too.
            final MapKeyBean bean = new MapKeyBean();
            bean.setById(N.asMap(1, "a"));
            Assertions.assertThrows(ParsingException.class, () -> parser.serialize(bean));

            // A MapEntity property name is validated after the naming policy has been applied.
            final MapEntity entity = new MapEntity("Sample");
            entity.set("1bad", "v");
            final ParsingException entityFailure = Assertions.assertThrows(ParsingException.class, () -> parser.serialize(entity));
            Assertions.assertEquals("MapEntity property name '1bad' is not a valid XML element name", entityFailure.getMessage());
        }

        // Valid NCNames keep working, including non-ASCII letters and a decomposed accent.
        final String unicodeKey = "un" + (char) 0x00EF + "code";
        final String combiningKey = "e" + (char) 0x0301 + "cole";
        final Map<String, String> ok = new java.util.LinkedHashMap<>();
        ok.put("_x", "1");
        ok.put("a-b", "2");
        ok.put("a.b", "3");
        ok.put("k9", "4");
        ok.put(unicodeKey, "5");
        ok.put(combiningKey, "6");

        for (final XmlParserImpl parser : new XmlParserImpl[] { staxParser, domParser }) {
            final String xml = parser.serialize(ok);
            final Map<String, String> back = parser.deserialize(xml, null, Map.class);
            Assertions.assertEquals(ok, back, xml);
        }

        // A null key is written as <null> and read back as the String "null" (unchanged, deliberate).
        final Map<String, String> withNullKey = new java.util.HashMap<>();
        withNullKey.put(null, "v");
        final String nullKeyXml = staxParser.serialize(withNullKey);
        Assertions.assertEquals("<map><null>v</null></map>", nullKeyXml);
        Assertions.assertEquals("v", staxParser.deserialize(nullKeyXml, null, Map.class).get("null"));

        // An ignored key is filtered before the check, so it does not fail the whole document.
        final Map<Object, String> ignored = new java.util.LinkedHashMap<>();
        ignored.put("keep", "1");
        ignored.put("a b", "2");
        final String filtered = staxParser.serialize(ignored, new XmlSerConfig().setIgnoredPropNames(Map.class, N.asSet("a b")));
        Assertions.assertEquals("<map><keep>1</keep></map>", filtered);
    }

    /**
     * P5-10: a processing instruction is not character data, so the text around it must be coalesced. The StAX
     * reader stopped at the instruction and dropped everything before it.
     */
    @Test
    public void reviewFixes20260906_processingInstructionKeepsSurroundingText() {
        for (final XmlParserImpl parser : new XmlParserImpl[] { staxParser, domParser }) {
            Assertions.assertEquals("ab", parser.deserialize("<testBean><name>a<?pi x?>b</name></testBean>", null, TestBean.class).getName());
            Assertions.assertEquals("ab", parser.deserialize("<testBean><name>a<!-- c -->b</name></testBean>", null, TestBean.class).getName());

            // ... and an instruction between two properties changes nothing.
            final TestBean between = parser.deserialize("<testBean><name>a</name><?pi x?><age>7</age></testBean>", null, TestBean.class);
            Assertions.assertEquals("a", between.getName());
            Assertions.assertEquals(7, between.getAge());

            final Map<?, ?> map = parser.deserialize("<map><k>a<?pi x?>b</k></map>", null, Map.class);
            Assertions.assertEquals("ab", map.get("k"));
        }
    }

    /**
     * P5-12: a String or a file is one whole document, so a second root element or trailing text must be
     * reported. A caller-supplied stream may carry further documents and is deliberately not drained.
     */
    @Test
    public void reviewFixes20260906_contentAfterRootElementIsRejectedForBoundedSources() throws IOException {
        final String valid = "<testBean><name>a</name></testBean>";

        for (final String trailing : new String[] { "<testBean/>", "junk", "<![CDATA[x]]>" }) {
            final String xml = valid + trailing;

            for (final XmlParserImpl parser : new XmlParserImpl[] { staxParser, domParser }) {
                Assertions.assertThrows(ParsingException.class, () -> parser.deserialize(xml, null, TestBean.class), xml);

                final File file = tempDir.resolve("trailing" + trailing.hashCode() + ".xml").toFile();
                java.nio.file.Files.write(file.toPath(), xml.getBytes());
                Assertions.assertThrows(ParsingException.class, () -> parser.deserialize(file, null, TestBean.class), xml);
            }
        }

        // Whitespace, comments and processing instructions are legal after the root element.
        for (final String trailing : new String[] { "   ", "<!-- c -->", "<?pi x?>", "\n" }) {
            for (final XmlParserImpl parser : new XmlParserImpl[] { staxParser, domParser }) {
                Assertions.assertEquals("a", parser.deserialize(valid + trailing, null, TestBean.class).getName(), trailing);
            }
        }

        // A caller-supplied stream or reader is not VALIDATED past the root element, so the first document is
        // still returned. It is not left intact: the StAX reader buffers ahead, so these small sources are
        // read to EOF and a large one would be left mid-token -- a second document cannot be read from it.
        Assertions.assertEquals("a", staxParser.deserialize(new StringReader(valid + "<testBean/>"), null, TestBean.class).getName());
        Assertions.assertEquals("a",
                staxParser.deserialize(new ByteArrayInputStream((valid + "<testBean/>").getBytes()), null, TestBean.class).getName());
    }

    /**
     * P5-13: {@code writeTypeInfo=true} makes both writers emit {@code type="MapEntity"}, which the DOM backend
     * rejected as an unsafe type attribute -- it could not read this parser's own output.
     */
    @Test
    public void reviewFixes20260906_mapEntityTypeAttributeIsAccepted() {
        final MapEntity entity = new MapEntity("Account");
        entity.set("name", "x");
        entity.set("id", 1);
        entity.set("nul", null);

        for (final XmlParserImpl writer : new XmlParserImpl[] { staxParser, domParser }) {
            final String xml = writer.serialize(entity, XmlSerConfig.create().setWriteTypeInfo(true));
            Assertions.assertTrue(xml.contains("type=\"MapEntity\""), xml);

            for (final XmlParserImpl parser : new XmlParserImpl[] { staxParser, domParser }) {
                final MapEntity back = parser.deserialize(xml, null, MapEntity.class);
                Assertions.assertEquals("Account", back.entityName(), xml);
                Assertions.assertEquals("x", back.get("name"), xml);
                // Compared as text: the DOM backend decides whether to honour type attributes from the FIRST
                // child element, which is the isNull marker here, so it converts the value with the map's
                // default value type. Unchanged by this fix.
                Assertions.assertEquals("1", String.valueOf((Object) back.get("id")), xml);
                Assertions.assertNull(back.get("nul"), xml);
            }
        }

        // G05-109: both spellings the writers emit are accepted by the shared allowlist itself now - the parser
        // no longer carries a two-name special case for MapEntity.
        for (final String typeAttr : new String[] { "MapEntity", "com.landawn.abacus.util.MapEntity" }) {
            final String xml = "<Account type=\"" + typeAttr + "\"><name>x</name></Account>";

            for (final XmlParserImpl parser : new XmlParserImpl[] { staxParser, domParser }) {
                Assertions.assertEquals("x", parser.deserialize(xml, null, MapEntity.class).get("name"), xml);
            }
        }

        // A type attribute that is not on the allowlist is still rejected.
        Assertions.assertThrows(ParsingException.class,
                () -> domParser.deserialize("<Account type=\"com.example.NotRegistered\"><name>x</name></Account>", null, MapEntity.class));
    }

    /**
     * T6-02: an empty {@code Optional}/{@code Nullable} property was written as the text {@code null}, which no
     * numeric or bean element type can read back; a tuple-like property was written with unquoted String slots,
     * so a comma inside a slot broke the value.
     */
    @Test
    public void reviewFixes20260906_optionalAndTupleProperties() {
        final OptionalBean empty = new OptionalBean();
        empty.setOs(Optional.empty());
        empty.setOi(Optional.empty());
        empty.setOi2(OptionalInt.empty());
        empty.setN(Nullable.of(null));
        empty.setP(Pair.of("a,b", 1));
        empty.setT(Tuple.of("x,y", 2));

        final String emptyXml = staxParser.serialize(empty, XmlSerConfig.create().setExclusion(Exclusion.NONE));
        Assertions.assertTrue(emptyXml.contains("<os isNull=\"true\" />"), emptyXml);
        Assertions.assertTrue(emptyXml.contains("<oi isNull=\"true\" />"), emptyXml);
        Assertions.assertTrue(emptyXml.contains("<oi2 isNull=\"true\" />"), emptyXml);
        Assertions.assertTrue(emptyXml.contains("<n isNull=\"true\" />"), emptyXml);
        Assertions.assertFalse(emptyXml.contains(">null<"), emptyXml);

        for (final XmlParserImpl parser : new XmlParserImpl[] { staxParser, domParser }) {
            final OptionalBean back = parser.deserialize(emptyXml, null, OptionalBean.class);
            Assertions.assertFalse(back.getOs().isPresent(), emptyXml);
            Assertions.assertFalse(back.getOi().isPresent(), emptyXml);
            Assertions.assertFalse(back.getOi2().isPresent(), emptyXml);
            Assertions.assertFalse(back.getN().isPresent(), emptyXml);
            Assertions.assertEquals(Pair.of("a,b", 1), back.getP(), emptyXml);
            Assertions.assertEquals(Tuple.of("x,y", 2), back.getT(), emptyXml);
        }

        // Exclusion.NULL drops an empty optional exactly like a null property.
        final String excluded = staxParser.serialize(empty);
        Assertions.assertFalse(excluded.contains("<os"), excluded);
        Assertions.assertFalse(excluded.contains("<oi2"), excluded);

        final OptionalBean present = new OptionalBean();
        present.setOs(Optional.of("null"));
        present.setOi(Optional.of(7));
        present.setOi2(OptionalInt.of(8));
        present.setN(Nullable.of("v"));
        present.setP(Pair.of("a,b", 1));
        present.setT(Tuple.of("x,y", 2));

        for (final boolean typeInfo : new boolean[] { false, true }) {
            final String xml = staxParser.serialize(present, XmlSerConfig.create().setExclusion(Exclusion.NONE).setWriteTypeInfo(typeInfo));
            Assertions.assertTrue(xml.contains("<os>null</os>"), xml);
            Assertions.assertTrue(xml.contains("<oi>7</oi>"), xml);

            for (final XmlParserImpl parser : new XmlParserImpl[] { staxParser, domParser }) {
                final OptionalBean back = parser.deserialize(xml, null, OptionalBean.class);
                // The text "null" is a value here, not the empty marker: it must come back as a present Optional.
                Assertions.assertEquals("null", back.getOs().get(), xml);
                Assertions.assertEquals(7, back.getOi().get(), xml);
                Assertions.assertEquals(8, back.getOi2().get(), xml);
                Assertions.assertEquals("v", back.getN().get(), xml);
                Assertions.assertEquals("a,b", back.getP().left(), xml);
                Assertions.assertEquals(1, back.getP().right(), xml);
                Assertions.assertEquals("x,y", back.getT()._1, xml);
                Assertions.assertEquals(2, back.getT()._2, xml);
            }
        }
    }

    /**
     * P5-15: a cyclic graph is rejected by the serialization depth guard instead of unwinding in a
     * StackOverflowError; a deep but finite graph still serializes.
     */
    @Test
    public void reviewFixes20260906_serializationDepthGuard() {
        CircularRefBean deep = new CircularRefBean();
        deep.setName("leaf");

        for (int i = 0; i < 100; i++) {
            final CircularRefBean parent = new CircularRefBean();
            parent.setName("level" + i);
            parent.setReference(deep);
            deep = parent;
        }

        Assertions.assertTrue(staxParser.serialize(deep).contains("leaf"));

        for (int i = 0; i < 200; i++) {
            final CircularRefBean parent = new CircularRefBean();
            parent.setName("level" + i);
            parent.setReference(deep);
            deep = parent;
        }

        final CircularRefBean tooDeep = deep;
        for (final XmlParserImpl parser : new XmlParserImpl[] { staxParser, domParser }) {
            final ParsingException exception = Assertions.assertThrows(ParsingException.class, () -> parser.serialize(tooDeep));
            Assertions.assertTrue(exception.getMessage().contains("Serialization nesting depth exceeded " + XmlParserImpl.MAX_SERIALIZATION_DEPTH),
                    exception.getMessage());
        }

        // The counter is per thread and released again, so the next serialization starts from zero.
        Assertions.assertTrue(staxParser.serialize(new TestBean("after", 1)).contains("after"));
    }

    /**
     * P5-09 (documented, not changed): an element with no text yields the property default with the StAX parser
     * type and an empty String with the DOM parser type. Pinned so the divergence cannot change unnoticed.
     */
    @Test
    public void reviewFixes20260906_emptyElementConventionIsBackendSpecific() {
        for (final String xml : new String[] { "<testBean><name/></testBean>", "<testBean><name></name></testBean>",
                "<testBean><name isNull=\"false\"/></testBean>" }) {
            Assertions.assertNull(staxParser.deserialize(xml, null, TestBean.class).getName(), xml);
            Assertions.assertEquals("", domParser.deserialize(xml, null, TestBean.class).getName(), xml);
        }

        // The explicit null marker means null on both.
        final String nullMarker = "<testBean><name isNull=\"true\"/></testBean>";
        Assertions.assertNull(staxParser.deserialize(nullMarker, null, TestBean.class).getName());
        Assertions.assertNull(domParser.deserialize(nullMarker, null, TestBean.class).getName());
    }
    /**
     * Self-review R2 of P5-02: the StAX MAP branch resolved a key from the element's local name only, so a
     * {@code tagByPropertyName=false} document read into a {@code Map} (directly, or as an untyped nested value
     * of a mixed array or collection) came back with the literal key {@code "property"} for every entry, while
     * the DOM backend returned the real names.
     */
    @Test
    public void reviewFixes20260907_tagByPropertyNameFalseReadAsMap() {
        final OuterBean original = newOuterBean();
        final String xml = staxParser.serialize(original, XmlSerConfig.create().setTagByPropertyName(false));

        for (final XmlParserImpl parser : new XmlParserImpl[] { staxParser, domParser }) {
            final Map<?, ?> back = parser.deserialize(xml, null, Map.class);

            Assertions.assertEquals("n", back.get("name"), back.keySet().toString());
            Assertions.assertEquals("t", back.get("tail"), back.keySet().toString());
            Assertions.assertFalse(back.containsKey("property"), back.keySet().toString());
        }

        // The same through the mixed array/collection path, where the element is read with the map type.
        final MixedBean mixed = new MixedBean();
        mixed.setObjs(new Object[] { newOuterBean(), "s" });
        final String mixedXml = staxParser.serialize(mixed, XmlSerConfig.create().setTagByPropertyName(false));

        for (final XmlParserImpl parser : new XmlParserImpl[] { staxParser, domParser }) {
            final Object[] objs = parser.deserialize(mixedXml, null, MixedBean.class).getObjs();

            Assertions.assertEquals(2, objs.length, mixedXml);
            Assertions.assertEquals("n", ((Map<?, ?>) objs[0]).get("name"), mixedXml);
            Assertions.assertEquals("s", objs[1], mixedXml);
        }

        // A map written by this parser has no `name` attribute, so plain map reading is unchanged.
        final Map<String, Object> plain = new LinkedHashMap<>();
        plain.put("name", "v");
        plain.put("k2", "w");
        final String plainXml = staxParser.serialize(plain);

        for (final XmlParserImpl parser : new XmlParserImpl[] { staxParser, domParser }) {
            final Map<?, ?> back = parser.deserialize(plainXml, null, Map.class);

            Assertions.assertEquals("v", back.get("name"), plainXml);
            Assertions.assertEquals("w", back.get("k2"), plainXml);
        }
    }

    /**
     * Self-review R2 of T6-02: an empty {@code Optional}/{@code Nullable} must produce the {@code isNull} form
     * and a present one its unwrapped value for MAP values and MAP ENTITY values too, not only for bean
     * properties. Before the fix a map value was written as an empty element ({@code <k></k>}, read back as
     * {@code null} on StAX and {@code ""} on DOM) and, with type information on, carried the WRAPPER's name in
     * the {@code type} attribute -- which this parser's own DOM backend rejects
     * ({@code XML type attribute is not allowed: Nullable<Object>}).
     */
    @Test
    public void reviewFixes20260907_optionalMapValuesUseTheIsNullForm() {
        // A present entry comes first on purpose: with an isNull element first the DOM backend samples it for the
        // whole map and then ignores every type attribute (a pre-existing quirk, reproducible with a plain null
        // value and unchanged by this fix), which would hide what is asserted here.
        final Map<String, Object> map = new LinkedHashMap<>();
        map.put("present", Optional.of("v"));
        map.put("empty", Optional.empty());
        map.put("emptyInt", OptionalInt.empty());
        map.put("presentInt", OptionalInt.of(3));
        map.put("nullableNull", Nullable.of(null));

        for (final boolean typeInfo : new boolean[] { false, true }) {
            final String xml = staxParser.serialize(map, XmlSerConfig.create().setWriteTypeInfo(typeInfo));

            Assertions.assertTrue(xml.contains("<empty isNull=\"true\" />"), xml);
            Assertions.assertTrue(xml.contains("<emptyInt isNull=\"true\" />"), xml);
            Assertions.assertTrue(xml.contains("<nullableNull isNull=\"true\" />"), xml);
            Assertions.assertFalse(xml.contains("Optional"), xml);
            Assertions.assertFalse(xml.contains("Nullable"), xml);

            for (final XmlParserImpl parser : new XmlParserImpl[] { staxParser, domParser }) {
                final Map<?, ?> back = parser.deserialize(xml, null, Map.class);

                Assertions.assertNull(back.get("empty"), xml);
                Assertions.assertNull(back.get("emptyInt"), xml);
                Assertions.assertNull(back.get("nullableNull"), xml);
                Assertions.assertEquals("v", back.get("present"), xml);
                Assertions.assertEquals(typeInfo ? (Object) 3 : (Object) "3", back.get("presentInt"), xml);
            }
        }

        final MapEntity mapEntity = new MapEntity("acc");
        mapEntity.set("present", Optional.of("v"));
        mapEntity.set("empty", Optional.empty());

        for (final boolean typeInfo : new boolean[] { false, true }) {
            final String xml = staxParser.serialize(mapEntity, XmlSerConfig.create().setWriteTypeInfo(typeInfo));

            Assertions.assertTrue(xml.contains("<empty isNull=\"true\" />"), xml);
            Assertions.assertFalse(xml.contains("Optional"), xml);

            for (final XmlParserImpl parser : new XmlParserImpl[] { staxParser, domParser }) {
                final MapEntity back = parser.deserialize(xml, null, MapEntity.class);

                Assertions.assertNull(back.get("empty"), xml);
                Assertions.assertEquals("v", back.get("present"), xml);
            }
        }
    }

    /**
     * Self-review R2 of T6-02 / P5-05: the same rule for an ARRAY or COLLECTION element of a mixed value. An
     * empty wrapper now produces the {@code <null isNull="true" />} marker both backends read as {@code null}
     * (it used to produce {@code <e></e>}: {@code null} on StAX, {@code ""} on DOM), and a present one carries
     * the UNWRAPPED value's type, so it survives a {@code writeTypeInfo} round trip.
     */
    @Test
    public void reviewFixes20260907_optionalCollectionElementsUseTheNullMarker() {
        final TestBean bean = new TestBean("b", 1);
        final MixedBean mixed = new MixedBean();
        mixed.setObjs(new Object[] { bean, Optional.empty(), Optional.of(7), Nullable.of(null) });
        mixed.setLos(new ArrayList<>(Arrays.asList(bean, Optional.empty(), OptionalInt.of(9))));

        for (final boolean typeInfo : new boolean[] { false, true }) {
            final String xml = staxParser.serialize(mixed, XmlSerConfig.create().setWriteTypeInfo(typeInfo));

            Assertions.assertFalse(xml.contains("<e></e>"), xml);
            Assertions.assertFalse(xml.contains("Optional"), xml);
            Assertions.assertFalse(xml.contains("Nullable"), xml);
            Assertions.assertTrue(xml.contains("<null isNull=\"true\" />"), xml);

            for (final XmlParserImpl parser : new XmlParserImpl[] { staxParser, domParser }) {
                final MixedBean back = parser.deserialize(xml, null, MixedBean.class);

                Assertions.assertEquals(4, back.getObjs().length, xml);
                Assertions.assertNull(back.getObjs()[1], xml);
                Assertions.assertEquals(typeInfo ? (Object) 7 : (Object) "7", back.getObjs()[2], xml);
                Assertions.assertNull(back.getObjs()[3], xml);

                Assertions.assertEquals(3, back.getLos().size(), xml);
                Assertions.assertNull(back.getLos().get(1), xml);
                Assertions.assertEquals(typeInfo ? (Object) 9 : (Object) "9", back.getLos().get(2), xml);
            }
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Fix pass 2026-09-08 (G01)
    // ---------------------------------------------------------------------------------------------------------

    /** A recognized bean (getter + setter) whose only property is excluded from serialization. */
    public static class AllPropsIgnoredBean {
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
    // JSON writer already honoured the flag.
    @Test
    public void fixG01_emptyBeanIsWrittenAsAnEmptyElementWhenFailOnEmptyBeanIsOff() {
        final XmlSerConfig lenient = XmlSerConfig.create().setFailOnEmptyBean(false);
        final AllPropsIgnoredBean bean = new AllPropsIgnoredBean();
        bean.setHidden("h");

        for (final XmlParserImpl parser : new XmlParserImpl[] { staxParser, domParser }) {
            final String xml = parser.serialize(bean, lenient);
            Assertions.assertEquals("<allPropsIgnoredBean></allPropsIgnoredBean>", xml);

            final AllPropsIgnoredBean back = parser.deserialize(xml, null, AllPropsIgnoredBean.class);
            Assertions.assertNotNull(back, xml);
            Assertions.assertNull(back.getHidden(), xml);

            // the flag on (the default) still rejects it, with the empty-bean message
            final ParsingException ex = Assertions.assertThrows(ParsingException.class, () -> parser.serialize(bean));
            Assertions.assertTrue(ex.getMessage().startsWith("No serializable property is found in class:"), ex.getMessage());
            Assertions.assertThrows(ParsingException.class, () -> parser.serialize(bean, XmlSerConfig.create().setFailOnEmptyBean(true)));
        }
    }

    // G01-16: the MapEntity's own entity name goes into element-name position too, but only its property names
    // were checked, so new MapEntity("a b") emitted <a b>...</a b>.
    @Test
    public void fixG01_mapEntityNameIsCheckedAsAnXmlElementName() {
        for (final XmlParserImpl parser : new XmlParserImpl[] { staxParser, domParser }) {
            for (final String badName : new String[] { "a b", "", "1bad", "a<b", "a&b", "x:y", ".dot", "-dash" }) {
                final MapEntity entity = new MapEntity(badName);
                entity.set("p", "v");

                final ParsingException failure = Assertions.assertThrows(ParsingException.class, () -> parser.serialize(entity), badName);
                Assertions.assertEquals("MapEntity name '" + badName + "' is not a valid XML element name", failure.getMessage());
            }

            // a valid name is untouched, and the property names keep their own check
            final MapEntity ok = new MapEntity("Sample");
            ok.set("p", "v");
            Assertions.assertEquals("<Sample><p>v</p></Sample>", parser.serialize(ok));
        }

        // A supplementary-plane character stays rejected on purpose: XML 1.0 fifth edition allows it in a name,
        // but the JDK readers this parser runs on implement the fourth-edition tables and cannot read such an
        // element back, which is exactly the unreadable document this check exists to prevent.
        final String supplementary = new String(Character.toChars(0x10400)) + "x";
        Assertions.assertThrows(ParsingException.class, () -> staxParser.serialize(N.asMap(supplementary, "v")));
        Assertions.assertThrows(ParsingException.class, () -> staxParser.serialize(new MapEntity(supplementary)));
    }

    // G01-17: writeMap/writeMapEntity emitted a tuple-like value's OWN type name under writeTypeInfo, unlike
    // writeProperties and writeElement - a name the shared allowlist rejects, so the DOM backend could not read
    // this parser's own output.
    @Test
    public void fixG01_tupleMapAndMapEntityValuesCarryNoTypeAttribute() {
        final XmlSerConfig typeInfo = XmlSerConfig.create().setWriteTypeInfo(true);

        // "plain" comes first on purpose: the DOM backend samples the FIRST child for the type-attribute mode, so
        // a leading element without one would make it skip every type attribute and hide the defect.
        final Map<String, Object> map = new LinkedHashMap<>();
        map.put("plain", "s");
        map.put("p", Pair.of("a,b", 1));
        map.put("t", Tuple.of(1, "x"));

        final String mapXml = staxParser.serialize(map, typeInfo);
        Assertions.assertFalse(mapXml.contains("type=\"Pair"), mapXml);
        Assertions.assertFalse(mapXml.contains("type=\"Tuple"), mapXml);
        Assertions.assertTrue(mapXml.contains("<plain type=\"String\">s</plain>"), mapXml);
        Assertions.assertTrue(mapXml.contains("<p>[&quot;a,b&quot;, 1]</p>"), mapXml);

        final MapEntity entity = new MapEntity("Account");
        entity.set("plain", "s");
        entity.set("p", Pair.of("a,b", 1));

        final String entityXml = staxParser.serialize(entity, typeInfo);
        Assertions.assertFalse(entityXml.contains("type=\"Pair"), entityXml);
        Assertions.assertTrue(entityXml.contains("<p>[&quot;a,b&quot;, 1]</p>"), entityXml);

        for (final XmlParserImpl parser : new XmlParserImpl[] { staxParser, domParser }) {
            final Map<?, ?> backMap = parser.deserialize(mapXml, null, Map.class);
            Assertions.assertEquals("s", backMap.get("plain"), mapXml);
            Assertions.assertEquals("[\"a,b\", 1]", String.valueOf(backMap.get("p")), mapXml);

            final MapEntity backEntity = parser.deserialize(entityXml, null, MapEntity.class);
            // MapEntity.get is <T> T, so an un-witnessed call binds String.valueOf(char[]) rather than
            // String.valueOf(Object) and fails with a ClassCastException at run time.
            Assertions.assertEquals("[\"a,b\", 1]", String.valueOf(backEntity.<Object> get("p")), entityXml);
        }
    }

    // G01-18: the DOM ENTITY branch threw when a child element had no `name` attribute, while the MAP and
    // MAP_ENTITY branches - and the whole StAX reader - fall back to the element's own name.
    @Test
    public void fixG01_domEntityFallsBackToTheElementNameLikeItsSiblings() {
        // The mode flag is taken from the OUTERMOST element, so this bean-style document puts a child without a
        // `name` attribute next to one that has it.
        final String mixed = "<bean name=\"testBean\"><name>A</name><property name=\"age\">3</property></bean>";

        for (final XmlParserImpl parser : new XmlParserImpl[] { staxParser, domParser }) {
            final TestBean back = parser.deserialize(mixed, null, TestBean.class);

            Assertions.assertEquals("A", back.getName(), mixed);
            Assertions.assertEquals(3, back.getAge(), mixed);

            // a name attribute still wins over the element name when the document is written that way
            final TestBean named = parser.deserialize("<bean name=\"testBean\"><property name=\"name\">B</property></bean>", null, TestBean.class);
            Assertions.assertEquals("B", named.getName());

            // and an element that matches no property is still reported, not silently accepted
            final XmlDeserConfig strict = XmlDeserConfig.create().setIgnoreUnmatchedProperty(false);
            final ParsingException unknown = Assertions.assertThrows(ParsingException.class,
                    () -> parser.deserialize("<bean name=\"testBean\"><nosuch>x</nosuch></bean>", strict, TestBean.class));
            Assertions.assertTrue(unknown.getMessage().contains("nosuch"), unknown.getMessage());
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Fix pass 2026-09-08 (G04)
    // ---------------------------------------------------------------------------------------------------------

    // G04-85: the DOM reader decided "properties are named by a `name` attribute" from ANY attribute on the bean
    // element, so an ordinary <person name="John"> document was read in the generic <bean name=".."> shape.
    @Test
    public void fixG04_domTakesTheGenericShapeFromTheBeanElementItself() {
        // a hand-written root that merely carries a `name` attribute: its children are named by their elements
        final TestBean handWritten = domParser.deserialize("<testBean name=\"John\"><age name=\"name\">30</age></testBean>", null, TestBean.class);
        Assertions.assertEquals(30, handWritten.getAge());
        Assertions.assertNull(handWritten.getName());

        final TestBean plain = domParser.deserialize("<testBean name=\"John\"><age>30</age></testBean>", null, TestBean.class);
        Assertions.assertEquals(30, plain.getAge());

        // the generic shape this parser writes is still read by the `name` attribute
        final TestBean generic = domParser.deserialize("<bean name=\"testBean\"><property name=\"age\">30</property></bean>", null, TestBean.class);
        Assertions.assertEquals(30, generic.getAge());

        // R02: this is the ONE document shape on which the two backends now answer differently, and the only
        // new test here that cannot loop over both. The StAX reader resolves each element's name on its own
        // (resolveElementName: a non-empty `name` attribute always wins), so it reads the child as the property
        // "name"; before this fix the DOM reader agreed, because it took the mode from any `name` attribute on
        // the ROOT. That divergence is deliberate and is documented on resolveElementName - "the two agree on
        // every document either writer produces, but not on a hand-written one that mixes the two shapes" -
        // and it is pinned here so a change to either backend has to face it.
        final TestBean viaStax = staxParser.deserialize("<testBean name=\"John\"><age name=\"name\">30</age></testBean>", null, TestBean.class);
        Assertions.assertEquals("30", viaStax.getName());
        Assertions.assertEquals(0, viaStax.getAge());

        // every shape either writer actually produces still reads alike through both backends
        for (final XmlParserImpl parser : new XmlParserImpl[] { staxParser, domParser }) {
            Assertions.assertEquals(30, parser.<TestBean> deserialize("<testBean name=\"John\"><age>30</age></testBean>", null, TestBean.class).getAge());
            Assertions.assertEquals(30,
                    parser.<TestBean> deserialize("<bean name=\"testBean\"><property name=\"age\">30</property></bean>", null, TestBean.class).getAge());
        }
    }

    // G04-85: the shape belongs to the bean element, not to the document. Decided once from the outermost
    // element, a bean nested in a map entry inherited the mode from the entry element (<p1>), which never
    // carries a `name` attribute, and the DOM reader then keyed every <property name=".."> child as "property"
    // and dropped it - silently, since an unmatched property is ignored by default.
    @Test
    public void fixG04_beanNestedInAMapEntryKeepsItsPropertiesInTheGenericShape() {
        final Map<String, TestBean> map = new LinkedHashMap<>();
        map.put("p1", new TestBean("John", 30));

        final XmlDeserConfig asBeans = XmlDeserConfig.create().setMapValueType(TestBean.class);

        for (final XmlParserImpl parser : new XmlParserImpl[] { staxParser, domParser }) {
            final String xml = parser.serialize(map, XmlSerConfig.create().setTagByPropertyName(false));
            Assertions.assertTrue(xml.contains("<bean name=\"testBean\">"), xml);

            final Map<String, TestBean> back = parser.deserialize(xml, asBeans, Map.class);
            Assertions.assertEquals("John", back.get("p1").getName(), xml);
            Assertions.assertEquals(30, back.get("p1").getAge(), xml);
        }
    }

    // G04-85: DocumentBuilder.parse hands back a Document, which is not an element - deserialize(Node, ..) read
    // it as null (and the nodeTypes overload looked its target up under "#document").
    @Test
    public void fixG04_aDocumentIsReadAsItsDocumentElement() throws Exception {
        final Document doc = document("<testBean><name>NodeTest</name><age>40</age></testBean>");
        final Map<String, Type<?>> nodeTypes = N.asMap("testBean", Type.of(TestBean.class));

        for (final XmlParserImpl parser : new XmlParserImpl[] { staxParser, domParser }) {
            final TestBean byType = parser.deserialize(doc, null, Type.of(TestBean.class));
            Assertions.assertEquals("NodeTest", byType.getName());
            Assertions.assertEquals(40, byType.getAge());

            final TestBean byClass = parser.deserialize(doc, null, TestBean.class);
            Assertions.assertEquals("NodeTest", byClass.getName());

            final TestBean byNodeTypes = parser.deserialize(doc, null, nodeTypes);
            Assertions.assertEquals("NodeTest", byNodeTypes.getName());

            // the document element itself still reads the same way
            final TestBean fromElement = parser.deserialize(doc.getDocumentElement(), null, nodeTypes);
            Assertions.assertEquals("NodeTest", fromElement.getName());
        }

        // a document without a document element has no element to read
        final Document noRoot = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        Assertions.assertNull(domParser.deserialize(noRoot, null, TestBean.class));
    }

    // G04-86: a null map key is written as the element <null>, but the ignore filter tested the bare null key,
    // so setIgnoredPropNames(Map.class, N.asSet("null")) did not filter it while the reader did - and a Set that
    // rejects a null argument threw from contains(null).
    @Test
    public void fixG04_aNullMapKeyIsIgnoredUnderTheNameItIsWrittenWith() {
        final Map<Object, String> withNullKey = new LinkedHashMap<>();
        withNullKey.put(null, "v1");
        withNullKey.put("k", "v2");

        for (final XmlParserImpl parser : new XmlParserImpl[] { staxParser, domParser }) {
            Assertions.assertEquals("<map><null>v1</null><k>v2</k></map>", parser.serialize(withNullKey));

            // filtered under the name the reader gives it back as
            Assertions.assertEquals("<map><k>v2</k></map>",
                    parser.serialize(withNullKey, new XmlSerConfig().setIgnoredPropNames(Map.class, N.asSet("null"))));

            // a Set that rejects a null argument - the shape ParserConfig's own javadoc uses - no longer throws
            Assertions.assertEquals("<map><null>v1</null></map>",
                    parser.serialize(withNullKey, new XmlSerConfig().setIgnoredPropNames(Map.class, java.util.Set.of("k"))));
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Review R02 of the 2026-09-08 fix pass
    // ---------------------------------------------------------------------------------------------------------

    /** A bean whose custom property name cannot be written in element-name position. */
    public static class CustomXmlPropNameBean {
        @JsonXmlField(name = "a b")
        public String value;
    }

    /** A bean whose custom property name is escapable in attribute position but not writable as an element name. */
    public static class MarkupXmlPropNameBean {
        @JsonXmlField(name = "a&b")
        public String value;
    }

    /** A bean whose custom property name needs no escaping and is a valid element name. */
    public static class PlainCustomXmlPropNameBean {
        @JsonXmlField(name = "user_id")
        public String value;
    }

    // R02: the map-key / MapEntity element-name check was never extended to a bean property, although
    // ParserUtil.XmlNameTag escapes such a name only for the <property name=".."> style and its own comment
    // says the element-name check "belongs to the writers". @JsonProperty("a b") therefore wrote
    // <a b>v</a b>, exactly the document-no-reader-accepts this family of checks exists to prevent.
    @Test
    public void reviewR02_beanPropertyCustomNameIsCheckedAsAnXmlElementName() {
        final CustomXmlPropNameBean bean = new CustomXmlPropNameBean();
        bean.value = "v";

        for (final XmlParserImpl parser : new XmlParserImpl[] { staxParser, domParser }) {
            final ParsingException failure = Assertions.assertThrows(ParsingException.class, () -> parser.serialize(bean));
            Assertions.assertEquals("Property name 'a b' is not a valid XML element name", failure.getMessage());

            // the generic <bean name=".."><property name=".."> shape puts the name in attribute position,
            // where XmlNameTag escapes it - that document is well-formed, so it must keep working
            Assertions.assertEquals("<bean name=\"customXmlPropNameBean\"><property name=\"a b\">v</property></bean>",
                    parser.serialize(bean, XmlSerConfig.create().setTagByPropertyName(false)));

            // the same holds for a name XmlNameTag CAN escape in attribute position: escaping is not
            // available in element-name position, so the two styles must answer differently
            // (ParserUtilTest.reviewFixes20260908_xmlNameTagEscapesTheAnnotationNameInAttributePosition
            // covers the attribute half of exactly this bean)
            final MarkupXmlPropNameBean markup = new MarkupXmlPropNameBean();
            markup.value = "v";
            Assertions.assertEquals("Property name 'a&b' is not a valid XML element name",
                    Assertions.assertThrows(ParsingException.class, () -> parser.serialize(markup)).getMessage());
            Assertions.assertEquals("<bean name=\"markupXmlPropNameBean\"><property name=\"a&amp;b\">v</property></bean>",
                    parser.serialize(markup, XmlSerConfig.create().setTagByPropertyName(false)));

            // a property that is not written cannot make serialization fail, as for an ignored map key
            Assertions.assertEquals("<customXmlPropNameBean></customXmlPropNameBean>",
                    parser.serialize(bean, XmlSerConfig.create().setIgnoredPropNames(CustomXmlPropNameBean.class, N.asSet("value"))));
            Assertions.assertEquals("<customXmlPropNameBean></customXmlPropNameBean>", parser.serialize(new CustomXmlPropNameBean()));

            // and an ordinary or valid custom name is written byte-for-byte as before
            final PlainCustomXmlPropNameBean plain = new PlainCustomXmlPropNameBean();
            plain.value = "v";
            Assertions.assertEquals("<plainCustomXmlPropNameBean><user_id>v</user_id></plainCustomXmlPropNameBean>", parser.serialize(plain));

            final TestBean ordinary = new TestBean();
            ordinary.setName("n");
            Assertions.assertTrue(parser.serialize(ordinary).contains("<name>n</name>"));
        }
    }

    /** A custom property name that attribute escaping turns into a character reference XML 1.0 forbids. */
    public static class ControlCharXmlPropNameBean {
        @JsonXmlField(name = "a" + (char) 1 + "b")
        public String value;
    }

    /** A custom property name holding an isolated surrogate, which no XML document can carry. */
    public static class LoneSurrogateXmlPropNameBean {
        @JsonXmlField(name = "a" + (char) 0xD800 + "b")
        public String value;
    }

    // X03/R03-8, the second half: the element-name check above covers tagByPropertyName=true, but with
    // tagByPropertyName=false the name goes in ATTRIBUTE position, where XmlNameTag escapes it - and escaping a
    // character XML 1.0 has no representation for produces <property name="a&#x1;b">, which this parser's own
    // reader rejects with "Illegal character entity". An isolated surrogate went out raw. The same code unit in
    // a property VALUE was already rejected up front, so the two positions disagreed.
    @Test
    public void reviewX03_R03_8_attributeStyleRejectsAPropertyNameXmlCannotRepresent() {
        final XmlSerConfig generic = XmlSerConfig.create().setTagByPropertyName(false);

        final ControlCharXmlPropNameBean ctrl = new ControlCharXmlPropNameBean();
        ctrl.value = "v";
        final LoneSurrogateXmlPropNameBean surrogate = new LoneSurrogateXmlPropNameBean();
        surrogate.value = "v";

        for (final XmlParserImpl parser : new XmlParserImpl[] { staxParser, domParser }) {
            Assertions.assertEquals("Property name 'a" + (char) 1 + "b' contains U+0001, which cannot be represented in XML 1.0",
                    Assertions.assertThrows(ParsingException.class, () -> parser.serialize(ctrl, generic)).getMessage());
            Assertions.assertEquals("Property name 'a" + (char) 0xD800 + "b' contains U+D800, which cannot be represented in XML 1.0",
                    Assertions.assertThrows(ParsingException.class, () -> parser.serialize(surrogate, generic)).getMessage());

            // element-name position rejects the same two names, as it did before, with its own message
            Assertions.assertEquals("Property name 'a" + (char) 1 + "b' is not a valid XML element name",
                    Assertions.assertThrows(ParsingException.class, () -> parser.serialize(ctrl)).getMessage());
            Assertions.assertEquals("Property name 'a" + (char) 0xD800 + "b' is not a valid XML element name",
                    Assertions.assertThrows(ParsingException.class, () -> parser.serialize(surrogate)).getMessage());

            // a property that is not written still cannot make serialization fail
            Assertions.assertEquals("<bean name=\"controlCharXmlPropNameBean\"></bean>",
                    parser.serialize(new ControlCharXmlPropNameBean(), generic));

            // and a name that only needs ESCAPING is still written, and still round-trips
            final MarkupXmlPropNameBean markup = new MarkupXmlPropNameBean();
            markup.value = "v";
            final String xml = parser.serialize(markup, generic);
            Assertions.assertEquals("<bean name=\"markupXmlPropNameBean\"><property name=\"a&amp;b\">v</property></bean>", xml);
            Assertions.assertEquals("v", parser.deserialize(xml, MarkupXmlPropNameBean.class).value);
        }
    }

    // X03/R03-9: writeMap took key.toString() for an Optional/Nullable key, so a PRESENT wrapper key failed
    // ("Map key 'Optional[k]' is not a valid XML element name") and an EMPTY one was written as the element
    // <Optional.empty>, which reads back as the String key "Optional.empty". JsonParserImpl and
    // AbacusXmlParserImpl.writeMap both unwrap a key; this writer was the one left out.
    @Test
    public void reviewX03_R03_9_wrapperMapKeyIsUnwrappedBeforeItBecomesTheElementName() {
        for (final XmlParserImpl parser : new XmlParserImpl[] { staxParser, domParser }) {
            Assertions.assertEquals("<map><k>c</k></map>", parser.serialize(oneEntryMap(Optional.of("k"), "c")));
            Assertions.assertEquals("<map><k>c</k></map>", parser.serialize(oneEntryMap(Nullable.of("k"), "c")));
            Assertions.assertEquals("<map><k>c</k></map>", parser.serialize(oneEntryMap(java.util.Optional.of("k"), "c")));

            // an empty wrapper key is the null key, exactly as the value side treats an empty wrapper
            Assertions.assertEquals("<map><null>c</null></map>", parser.serialize(oneEntryMap(Optional.empty(), "c")));
            Assertions.assertEquals("<map><null>c</null></map>", parser.serialize(oneEntryMap(Nullable.empty(), "c")));
            Assertions.assertEquals("<map><null>c</null></map>", parser.serialize(oneEntryMap(java.util.Optional.empty(), "c")));

            // ... and the document reads back, which it could not before
            Assertions.assertEquals(oneEntryMap("k", "c"), parser.deserialize(parser.serialize(oneEntryMap(Optional.of("k"), "c")), Map.class));

            // the ignoredPropNames filter sees the key the entry is written under, not the wrapper's toString
            Assertions.assertEquals("<map></map>",
                    parser.serialize(oneEntryMap(Optional.of("k"), "c"), XmlSerConfig.create().setIgnoredPropNames(Map.class, N.asSet("k"))));
            Assertions.assertEquals("<map></map>",
                    parser.serialize(oneEntryMap(Optional.empty(), "c"), XmlSerConfig.create().setIgnoredPropNames(Map.class, N.asSet("null"))));

            // unchanged: a plain key, a null key, and a wrapper VALUE
            Assertions.assertEquals("<map><k>c</k></map>", parser.serialize(oneEntryMap("k", "c")));
            Assertions.assertEquals("<map><null>c</null></map>", parser.serialize(oneEntryMap(null, "c")));
            Assertions.assertEquals("<map><k>c</k></map>", parser.serialize(oneEntryMap("k", Optional.of("c"))));
            Assertions.assertEquals("<map><k isNull=\"true\" /></map>", parser.serialize(oneEntryMap("k", Optional.empty())));
        }
    }

    private static Map<Object, Object> oneEntryMap(final Object key, final Object value) {
        final Map<Object, Object> map = new LinkedHashMap<>();
        map.put(key, value);
        return map;
    }
}
