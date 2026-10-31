package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.ParsingException;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.MapEntity;
import com.landawn.abacus.util.XmlUtil;

public class XmlEmptyRootCollectionTest extends TestBase {
    private static final List<XmlParser> PARSERS = List.of(new XmlParserImpl(XmlParserType.DOM), new XmlParserImpl(XmlParserType.StAX),
            new AbacusXmlParserImpl(XmlParserType.DOM), new AbacusXmlParserImpl(XmlParserType.StAX), new AbacusXmlParserImpl(XmlParserType.SAX));

    public static class NullRootBean {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }
    }

    public static class EmptyItem {
        private String name;

        public EmptyItem() {
        }

        EmptyItem(final String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        @Override
        public boolean equals(final Object obj) {
            return obj instanceof EmptyItem other && java.util.Objects.equals(name, other.name);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hashCode(name);
        }

        @Override
        public String toString() {
            return "EmptyItem{" + name + "}";
        }
    }

    public static class WrapperBean {
        private Map<String, String> props;
        private List<String> tags;
        private List<Map<String, String>> maps;

        public Map<String, String> getProps() {
            return props;
        }

        public void setProps(final Map<String, String> props) {
            this.props = props;
        }

        public List<String> getTags() {
            return tags;
        }

        public void setTags(final List<String> tags) {
            this.tags = tags;
        }

        public List<Map<String, String>> getMaps() {
            return maps;
        }

        public void setMaps(final List<Map<String, String>> maps) {
            this.maps = maps;
        }
    }

    public static class MapEntityItemsHolder {
        private List<MapEntity> items;
        private MapEntity[] array;

        public List<MapEntity> getItems() {
            return items;
        }

        public void setItems(final List<MapEntity> items) {
            this.items = items;
        }

        public MapEntity[] getArray() {
            return array;
        }

        public void setArray(final MapEntity[] array) {
            this.array = array;
        }
    }

    private static Node element(final String xml) throws Exception {
        return XmlUtil.createDOMParser().parse(new InputSource(new StringReader(xml))).getDocumentElement();
    }

    /** Reads {@code xml} through String, Reader, InputStream and DOM Node inputs. */
    private static List<Object> readAllInputs(final XmlParser parser, final String xml, final Type<?> type) throws Exception {
        final List<Object> results = new ArrayList<>();
        results.add(parser.deserialize(xml, type));
        results.add(parser.deserialize(new StringReader(xml), type));
        results.add(parser.deserialize(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)), type));
        results.add(parser.deserialize(element(xml), type));
        return results;
    }

    // Bug fix: Unicode-only blank roots must follow the same empty-collection contract as ASCII whitespace.
    @Test
    public void blankRootCollectionsAreEmptyAcrossBackendsAndElementTypes() throws Exception {
        for (final XmlParser parser : PARSERS) {
            for (final String name : new String[] { "List<String>", "List<Object>", "List<Integer>", "Set<String>", "ImmutableList<String>",
                    "java.util.concurrent.CopyOnWriteArrayList<String>" }) {
                final Type<?> type = Type.of(name);
                for (final String xml : new String[] { "<list/>", "<list></list>", "<list> \n\t </list>", "<list><![CDATA[ ]]></list>", "<list>&#x2003;</list>",
                        "<list><![CDATA[\u3000]]></list>", "<list><!-- empty --></list>", "<list>[]</list>" }) {
                    for (final Object result : readAllInputs(parser, xml, type)) {
                        assertNotNull(result, name + " " + xml);
                        assertTrue(type.javaType().isInstance(result), name + " " + result.getClass());
                        assertTrue(((Collection<?>) result).isEmpty(), name + " " + xml);
                    }
                }
            }

            // The standard DOM reader used to return null for a blank root of a concrete collection class.
            assertSame(CopyOnWriteArrayList.class, parser.deserialize("<list/>", Type.of("java.util.concurrent.CopyOnWriteArrayList<String>")).getClass());
        }
    }

    @Test
    public void explicitNullCollectionsRemainDistinctFromEmptyCollectionsAndNullItems() throws Exception {
        for (final XmlParser parser : PARSERS) {
            final Type<List<String>> type = Type.of("List<String>");
            for (final String xml : List.of("<list isNull='true'/>", "<list isNull='true'>ignored</list>", "<list isNull='true'><e>ignored</e></list>")) {
                for (final Object result : readAllInputs(parser, xml, type)) {
                    assertNull(result, xml);
                }
            }
            assertEquals(List.of(), parser.deserialize("<list isNull='false'/>", type));
            assertEquals(Arrays.asList("a", null, "b"), parser.deserialize("<list><e>a</e><null isNull='true'/><e>b</e></list>", type));
            assertEquals(List.of("a", "b"), parser.deserialize("<list>[\"a\",\"b\"]</list>", type));
        }
    }

    // Bug fix: the standard readers honored isNull="true" on a collection root only. A bean, map or array root marked so
    // was read as an empty (or populated) value, while every Abacus reader returned null.
    @Test
    public void explicitNullRootsOfEveryTypeAreNullOnEveryReader() throws Exception {
        final String bean = NullRootBean.class.getName();
        final String[][] cases = { { bean, "<bean isNull='true'/>" }, { bean, "<bean isNull='true'><name>x</name></bean>" },
                { "Map<String, String>", "<map isNull='true'/>" }, { "Map<String, String>", "<map isNull='true'><a>1</a></map>" },
                { "String[]", "<array isNull='true'/>" }, { "String[]", "<array isNull='true'><e>x</e></array>" }, { "Object[]", "<array isNull='true'/>" },
                { "int[]", "<array isNull='true'/>" }, { "Object", "<map isNull='true'/>" }, { "Object", "<array type='String[]' isNull='true'/>" },
                { "Object", "<map type='Map&lt;String, String&gt;' isNull='true'/>" }, { "String", "<s isNull='true'/>" },
                { "List<String>", "<list isNull='TRUE'/>" } };

        for (final XmlParser parser : PARSERS) {
            for (final String[] c : cases) {
                for (final Object result : readAllInputs(parser, c[1], Type.of(c[0]))) {
                    assertNull(result, parser + " " + c[0] + " " + c[1]);
                }
            }

            // Not marked, or marked false: read as usual.
            final NullRootBean read = parser.deserialize("<bean isNull='false'><name>x</name></bean>", NullRootBean.class);
            assertEquals("x", read.getName());
            final Map<String, String> map = parser.deserialize(
                    parser instanceof AbacusXmlParserImpl ? "<map><entry><key>a</key><value>1</value></entry></map>" : "<map><a>1</a></map>",
                    Type.of("Map<String, String>"));
            assertEquals(Map.of("a", "1"), map);
            assertArrayEquals(new String[] { "x" }, parser.deserialize("<array><e>x</e></array>", String[].class));
        }
    }

    @Test
    public void nullCollectionRootsStillValidateTypeMetadata() {
        for (final XmlParserType backend : new XmlParserType[] { XmlParserType.DOM, XmlParserType.StAX }) {
            final XmlParser parser = new XmlParserImpl(backend, null, null, Set.of());
            assertThrows(ParsingException.class, () -> parser.deserialize("<list type='unapproved.Payload' isNull='true'/>", Type.of("List<String>")));
        }
    }

    // The null marker on a root of any type is honored only after its type attribute passed the allowlist.
    @Test
    public void nullRootsOfEveryTypeStillValidateTypeMetadata() {
        for (final XmlParserType backend : new XmlParserType[] { XmlParserType.DOM, XmlParserType.StAX }) {
            final XmlParser parser = new XmlParserImpl(backend, null, null, Set.of());
            assertThrows(ParsingException.class, () -> parser.deserialize("<bean type='unapproved.Payload' isNull='true'/>", NullRootBean.class));
            assertThrows(ParsingException.class, () -> parser.deserialize("<map type='unapproved.Payload' isNull='true'/>", Type.of("Map<String, String>")));
            assertThrows(ParsingException.class, () -> parser.deserialize("<array type='unapproved.Payload' isNull='true'/>", String[].class));
            assertThrows(ParsingException.class, () -> parser.deserialize("<x type='unapproved.Payload' isNull='true'/>", Object.class));
        }
    }

    // Blank nested list or array items written by hand stay backend-specific (see XmlParser). What the writers produce -
    // [] for an empty nested list or array, an element without content for an empty map or bean, isNull="true" for null -
    // reads back alike on every reader of the parser's format.
    @Test
    public void writtenEmptyAndNullNestedContainersRoundTripOnEveryReader() {
        final List<List<String>> lists = Arrays.asList(new ArrayList<>(), null, new ArrayList<>(List.of("a")));
        final List<Map<String, String>> maps = Arrays.asList(new LinkedHashMap<>(), null, new LinkedHashMap<>(Map.of("k", "v")));
        final List<EmptyItem> beans = Arrays.asList(new EmptyItem(), null, new EmptyItem("x"));
        final String[][] arrays = { {}, null, { "a" } };
        @SuppressWarnings("unchecked")
        final Map<String, String>[] mapArray = new Map[] { new LinkedHashMap<>(), null };
        final EmptyItem[] beanArray = { new EmptyItem(), new EmptyItem("x") };
        final List<Object> open = Arrays.asList(new LinkedHashMap<>(), new ArrayList<>());

        for (final List<XmlParser> family : List.of(PARSERS.subList(0, 2), PARSERS.subList(2, 5))) {
            final XmlParser writer = family.get(0);
            final String listXml = writer.serialize(lists);
            final String mapXml = writer.serialize(maps);
            final String beanXml = writer.serialize(beans);
            final String arrayXml = writer.serialize(arrays);
            final String mapArrayXml = writer.serialize(mapArray);
            final String beanArrayXml = writer.serialize(beanArray);
            final String openXml = writer.serialize(open);

            for (final XmlParser reader : family) {
                assertEquals(lists, reader.deserialize(listXml, Type.of("List<List<String>>")), reader + " " + listXml);
                assertEquals(maps, reader.deserialize(mapXml, Type.of("List<Map<String, String>>")), reader + " " + mapXml);
                assertEquals(beans, reader.deserialize(beanXml, Type.of("List<" + EmptyItem.class.getName() + ">")), reader + " " + beanXml);
                assertArrayEquals(arrays, reader.deserialize(arrayXml, String[][].class), reader + " " + arrayXml);
                assertArrayEquals(mapArray, reader.deserialize(mapArrayXml, Type.of("Map<String, String>[]")), reader + " " + mapArrayXml);
                assertArrayEquals(beanArray, reader.deserialize(beanArrayXml, EmptyItem[].class), reader + " " + beanArrayXml);
                assertEquals(open, reader.deserialize(openXml, Type.of("List<Object>")), reader + " " + openXml);
            }
        }
    }

    // Bug fix: the standard writer emits an empty map or bean item as an element without content. The standard DOM reader
    // returned null for such an item of a root array or collection, while its StAX reader read the empty map or bean.
    @Test
    public void blankMapAndBeanItemsAreEmptyOnBothStandardReaders() throws Exception {
        final Type<List<EmptyItem>> beanList = Type.of("List<" + EmptyItem.class.getName() + ">");

        for (final XmlParser parser : PARSERS.subList(0, 2)) {
            for (final Object result : readAllInputs(parser, "<list><item></item><null isNull='true'/><item><name>x</name></item></list>", beanList)) {
                assertEquals(Arrays.asList(new EmptyItem(), null, new EmptyItem("x")), result, parser.toString());
            }
            for (final Object result : readAllInputs(parser, "<array><item/><item> </item></array>", Type.of(EmptyItem[].class))) {
                assertArrayEquals(new EmptyItem[] { new EmptyItem(), new EmptyItem() }, (EmptyItem[]) result);
            }

            for (final Object result : readAllInputs(parser, "<list><map><a>1</a></map><map></map><map> </map><map isNull='true'/></list>",
                    Type.of("List<Map<String, String>>"))) {
                assertEquals(Arrays.asList(Map.of("a", "1"), Map.of(), Map.of(), null), result);
            }
            final Map<String, String>[] mapArray = parser.deserialize("<array><map/></array>", Type.of("Map<String, String>[]"));
            assertEquals(Map.of(), mapArray[0]);

            // The item type's class is constructed, including immutable and sorted maps and a typed open item.
            assertSame(java.util.TreeMap.class,
                    parser.<List<Object>> deserialize("<list><map/></list>", Type.of("List<java.util.TreeMap<String, String>>")).get(0).getClass());
            assertTrue(parser.<List<Object>> deserialize("<list><map/></list>", Type.of("List<com.landawn.abacus.util.ImmutableMap<String, String>>"))
                    .get(0) instanceof com.landawn.abacus.util.ImmutableMap<?, ?> immutable && immutable.isEmpty());
            final List<Object> typed = parser.deserialize(
                    "<list type='ArrayList&lt;Object&gt;'><map type='java.util.LinkedHashMap&lt;Object, Object&gt;'></map></list>", Type.of("List<Object>"));
            assertSame(LinkedHashMap.class, typed.get(0).getClass());
            assertEquals(List.of(Map.of()), parser.deserialize("<list><map/></list>", Type.of("List<Object>")));

            // Controls: compact map text still reads; a blank property wrapper keeps the property text rule (null) on
            // every reader, while a blank map item inside a property's collection was already the empty map.
            assertEquals(List.of(Map.of("a", "1")), parser.deserialize("<list><map>{\"a\": \"1\"}</map></list>", Type.of("List<Map<String, String>>")));
            final WrapperBean wrapper = parser.deserialize("<wrapperBean><props/><tags/><maps><map/></maps></wrapperBean>", WrapperBean.class);
            assertNull(wrapper.getProps());
            assertNull(wrapper.getTags());
            assertEquals(List.of(Map.of()), wrapper.getMaps());
        }

        for (final XmlParser parser : PARSERS.subList(2, 5)) {
            final WrapperBean wrapper = parser.deserialize("<wrapperBean><props/><tags/></wrapperBean>", WrapperBean.class);
            assertNull(wrapper.getProps(), parser.toString());
            assertNull(wrapper.getTags(), parser.toString());
        }
    }

    // Bug fix: MapEntity has its own type category, so the blank map/bean item path omitted it and lost its name and value.
    @Test
    public void mapEntityRootItemsPreserveEmptyEntitiesAndNulls() throws Exception {
        final List<MapEntity> expected = Arrays.asList(new MapEntity("emptyEntity"), null, new MapEntity("populatedEntity").set("a", "1"));
        final MapEntity[] expectedArray = expected.toArray(new MapEntity[0]);
        final XmlParser writer = PARSERS.get(0);

        for (final XmlParser reader : PARSERS.subList(0, 2)) {
            for (final Object result : readAllInputs(reader, writer.serialize(expected), Type.of("List<MapEntity>"))) {
                assertMapEntities(expected, (List<?>) result);
            }
            for (final Object result : readAllInputs(reader, writer.serialize(expectedArray), Type.of(MapEntity[].class))) {
                assertSame(MapEntity[].class, result.getClass());
                assertMapEntities(expected, Arrays.asList((MapEntity[]) result));
            }
            for (final String blank : List.of("", " ", "&#x2003;", "<![CDATA[\u3000]]>", "<!-- empty -->")) {
                for (final Object result : readAllInputs(reader, "<list><emptyEntity>" + blank + "</emptyEntity><null isNull='true'/></list>",
                        Type.of("List<MapEntity>"))) {
                    assertMapEntities(expected.subList(0, 2), (List<?>) result);
                }
            }
        }
    }

    // Bug fix: a declared MapEntity component fell back to Map for collection/array properties on both standard readers.
    @Test
    public void mapEntityPropertiesPreserveCollectionAndArrayElementTypes() throws Exception {
        final List<MapEntity> expected = Arrays.asList(new MapEntity("emptyEntity"), null, new MapEntity("populatedEntity").set("a", "1"));
        final MapEntityItemsHolder value = new MapEntityItemsHolder();
        value.setItems(expected);
        value.setArray(expected.toArray(new MapEntity[0]));
        final String xml = PARSERS.get(0).serialize(value);

        for (final XmlParser reader : PARSERS.subList(0, 2)) {
            for (final Object result : readAllInputs(reader, xml, Type.of(MapEntityItemsHolder.class))) {
                final MapEntityItemsHolder holder = (MapEntityItemsHolder) result;
                assertMapEntities(expected, holder.getItems());
                assertSame(MapEntity[].class, holder.getArray().getClass());
                assertMapEntities(expected, Arrays.asList(holder.getArray()));
            }
            // Blank property wrappers retain the property text rule; they are not empty entity items.
            final MapEntityItemsHolder blank = reader.deserialize("<holder><items/><array/></holder>", MapEntityItemsHolder.class);
            assertNull(blank.getItems());
            assertNull(blank.getArray());
        }
    }

    private static void assertMapEntities(final List<MapEntity> expected, final List<?> actual) {
        assertEquals(expected.size(), actual.size());
        for (int i = 0; i < expected.size(); i++) {
            final MapEntity entity = expected.get(i);
            if (entity == null) {
                assertNull(actual.get(i));
            } else {
                assertNotNull(actual.get(i));
                assertSame(MapEntity.class, actual.get(i).getClass());
                final MapEntity read = (MapEntity) actual.get(i);
                assertEquals(entity.entityName(), read.entityName());
                assertEquals(entity, read);
                assertEquals(entity.isEmpty(), read.isEmpty());
            }
        }
    }
}
