package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.type.Type;

/**
 * Locks the remaining schema gaps: a declared Object adopts an uninstantiable container, and element configuration
 * follows the same precedence for JSON text, direct child elements, arrays, and text on a map value or value wrapper.
 */
@Tag("unit")
public class XmlReportedRemainderRegressionTest extends TestBase {
    static Stream<Arguments> backends() {
        return XmlWrappedObjectRegressionTest.backends();
    }

    private static XmlParser parser(final boolean abacus, final XmlParserType backend) {
        return abacus ? new AbacusXmlParserImpl(backend) : new XmlParserImpl(backend);
    }

    private static String attribute(final String type) {
        return type.isEmpty() ? "" : " type=\"" + type.replace("<", "&lt;").replace(">", "&gt;") + "\"";
    }

    private static String immutableList(final String element) {
        return List.of(1, 2).getClass().getName() + "<" + element + ">";
    }

    private static String element(final String name, final String metadata, final String body) {
        return "<" + name + attribute(metadata) + ">" + body + "</" + name + ">";
    }

    private static String valueDocument(final boolean abacus, final String metadata, final String body) {
        return abacus ? "<map><entry><key>k</key><value" + attribute(metadata) + ">" + body + "</value></entry></map>"
                : "<map><k" + attribute(metadata) + ">" + body + "</k></map>";
    }

    private static List<String> objectValueDocuments(final boolean abacus, final String metadata) {
        final List<String> documents = new ArrayList<>();
        documents.add(valueDocument(abacus, metadata, "[\"1\",\"2\"]"));
        if (abacus) {
            documents.add(valueDocument(true, metadata, "<list>[\"1\",\"2\"]</list>"));
            documents.add(valueDocument(true, metadata, "<list><e>1</e><e>2</e></list>"));
            documents.add(valueDocument(true, "", "<list" + attribute(metadata) + ">[\"1\",\"2\"]</list>"));
            documents.add(valueDocument(true, "", "<list" + attribute(metadata) + "><e>1</e><e>2</e></list>"));
        } else {
            documents.add(valueDocument(false, metadata, "<e>1</e><e>2</e>"));
        }
        return documents;
    }

    @SuppressWarnings("unchecked")
    private static void assertMutableList(final Object value, final boolean longs) {
        final List<Object> expected = longs ? List.of(1L, 2L) : List.of(1, 2);
        assertEquals(expected, value);
        final List<Object> list = assertInstanceOf(List.class, value);
        assertFalse(List.of(1).getClass().isInstance(list));
        assertEquals(longs ? Long.class : Integer.class, list.getFirst().getClass());
        list.add(null);
        list.remove(list.size() - 1);
    }

    @SuppressWarnings("unchecked")
    private static void assertMutableSet(final Object value, final boolean longs) {
        assertEquals(longs ? Set.of(1L, 2L) : Set.of(1, 2), value);
        final Set<Object> set = assertInstanceOf(Set.class, value);
        assertFalse(Set.of(1).getClass().isInstance(set));
        assertEquals(longs ? Long.class : Integer.class, set.iterator().next().getClass());
        final Object extra = longs ? 9L : 9;
        set.add(extra);
        set.remove(extra);
    }

    @SuppressWarnings("unchecked")
    private static void assertMutableIntegerMap(final Object value) {
        assertEquals(Map.of("a", 1), value);
        final Map<Object, Object> map = assertInstanceOf(Map.class, value);
        assertFalse(Map.of("a", 1).getClass().isInstance(map));
        assertEquals(Integer.class, map.get("a").getClass());
        map.put("extra", null);
        map.remove("extra");
    }

    @ParameterizedTest
    @MethodSource("backends")
    void declaredObjectAdoptsUninstantiableList(final boolean abacus, final XmlParserType backend) {
        final XmlParser parser = parser(abacus, backend);
        final String metadata = immutableList("Integer");
        for (final String body : List.of("[\"1\",\"2\"]", "<e>1</e><e>2</e>")) {
            assertMutableList(parser.deserialize(element("list", metadata, body), Type.of("Object")), false);
            if (!abacus) {
                assertMutableList(parser.deserialize(element("v", metadata, body), Type.of("Object")), false);
            }
        }
        for (final String xml : objectValueDocuments(abacus, metadata)) {
            final Map<?, ?> actual = parser.deserialize(xml, Type.of("Map<String,Object>"));
            assertMutableList(actual.get("k"), false);
        }
    }

    @ParameterizedTest
    @MethodSource("backends")
    void declaredObjectHonorsElementConfiguration(final boolean abacus, final XmlParserType backend) {
        final XmlParser parser = parser(abacus, backend);
        final XmlDeserConfig config = XmlDeserConfig.create().setElementType(Long.class);
        final String metadata = immutableList("Integer");
        for (final String body : List.of("[\"1\",\"2\"]", "<e>1</e><e>2</e>")) {
            assertMutableList(parser.deserialize(element("list", metadata, body), config, Type.of("Object")), true);
        }
        for (final String xml : objectValueDocuments(abacus, metadata)) {
            final Map<?, ?> actual = parser.deserialize(xml, config, Type.of("Map<String,Object>"));
            assertMutableList(actual.get("k"), true);
        }
    }

    @ParameterizedTest
    @MethodSource("backends")
    void optionalObjectAdoptsUninstantiableList(final boolean abacus, final XmlParserType backend) {
        final XmlParser parser = parser(abacus, backend);
        final String metadata = immutableList("Integer");
        for (final XmlWrappedObjectRegressionTest.Wrapper wrapper : XmlWrappedObjectRegressionTest.Wrapper.values()) {
            final String schema = "Map<String," + wrapper.typeName + "<Object>>";
            for (final boolean longs : new boolean[] { false, true }) {
                final XmlDeserConfig config = longs ? XmlDeserConfig.create().setElementType(Long.class) : XmlDeserConfig.create();
                for (final String xml : objectValueDocuments(abacus, metadata)) {
                    final Map<?, ?> actual = parser.deserialize(xml, config, Type.of(schema));
                    assertMutableList(wrapper.unwrap(actual.get("k")), longs);
                }
            }
        }
    }

    @ParameterizedTest
    @MethodSource("backends")
    void rootCollectionConfigurationOverridesTheGenericComponent(final boolean abacus, final XmlParserType backend) {
        final XmlParser parser = parser(abacus, backend);
        final XmlDeserConfig config = XmlDeserConfig.create().setElementType(Integer.class);
        for (final String metadata : List.of("", immutableList("Integer"))) {
            for (final String body : List.of("[\"1\",\"2\"]", "<e>1</e><e>2</e>")) {
                assertMutableList(parser.deserialize(element("list", metadata, body), config, Type.of("List<Long>")), false);
                final Long[] array = parser.deserialize(element("array", metadata, body), XmlDeserConfig.create().setElementType(Long.class),
                        Type.of("Long[]"));
                assertEquals(2, array.length);
                assertEquals(Long.class, array[0].getClass());
                assertEquals(1L, array[0]);
                assertEquals(2L, array[1]);
            }
        }
    }

    @ParameterizedTest
    @MethodSource("backends")
    void openMapValueHonorsElementConfiguration(final boolean abacus, final XmlParserType backend) {
        final XmlParser parser = parser(abacus, backend);
        final XmlDeserConfig config = XmlDeserConfig.create().setElementType(Long.class);
        final String metadata = immutableList("Integer");
        for (final String xml : objectValueDocuments(abacus, metadata)) {
            final Map<?, ?> lists = parser.deserialize(xml, config, Type.of("Map<String,List<Object>>"));
            assertMutableList(lists.get("k"), true);
            for (final XmlWrappedObjectRegressionTest.Wrapper wrapper : XmlWrappedObjectRegressionTest.Wrapper.values()) {
                final Map<?, ?> wrapped = parser.deserialize(xml, config, Type.of("Map<String," + wrapper.typeName + "<List<Object>>>"));
                assertMutableList(wrapper.unwrap(wrapped.get("k")), true);
            }
        }
    }

    @ParameterizedTest
    @MethodSource("backends")
    void declaredMapValueComponentBeatsElementConfiguration(final boolean abacus, final XmlParserType backend) {
        final XmlParser parser = parser(abacus, backend);
        final XmlDeserConfig config = XmlDeserConfig.create().setElementType(Integer.class);
        final String metadata = immutableList("Integer");
        for (final String xml : objectValueDocuments(abacus, metadata)) {
            final Map<?, ?> lists = parser.deserialize(xml, config, Type.of("Map<String,List<Long>>"));
            assertMutableList(lists.get("k"), true);
            for (final XmlWrappedObjectRegressionTest.Wrapper wrapper : XmlWrappedObjectRegressionTest.Wrapper.values()) {
                final Map<?, ?> wrapped = parser.deserialize(xml, config, Type.of("Map<String," + wrapper.typeName + "<List<Long>>>"));
                assertMutableList(wrapper.unwrap(wrapped.get("k")), true);
            }
        }
    }

    @ParameterizedTest
    @MethodSource("backends")
    void declaredObjectAdoptsUninstantiableSetAndMap(final boolean abacus, final XmlParserType backend) {
        final XmlParser parser = parser(abacus, backend);
        final String setMetadata = Set.of(1).getClass().getName() + "<Integer>";
        assertMutableSet(parser.deserialize(element("set", setMetadata, "[\"1\",\"2\"]"), Type.of("Object")), false);
        assertMutableSet(
                parser.deserialize(element("set", setMetadata, "[\"1\",\"2\"]"), XmlDeserConfig.create().setElementType(Long.class), Type.of("Object")), true);

        final String mapMetadata = Map.of("a", 1).getClass().getName() + "<String,Integer>";
        final Map<?, ?> actual = parser.deserialize(valueDocument(abacus, mapMetadata, "{\"a\":\"1\"}"), Type.of("Map<String,Object>"));
        assertMutableIntegerMap(actual.get("k"));
    }
}
