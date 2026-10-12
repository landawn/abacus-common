package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.ParsingException;
import com.landawn.abacus.type.Type;

@Tag("unit")
public class XmlImmutableSchemaRegressionTest extends TestBase {
    enum Shape {
        LIST, MAP;

        String schema(final String element) {
            return this == LIST ? "List<" + element + ">" : "Map<String," + element + ">";
        }

        String metadata() {
            return implementations().getFirst().getName() + (this == LIST ? "<Integer>" : "<String,Integer>");
        }

        List<Class<?>> implementations() {
            return this == LIST
                    ? List.of(List.of(1, 2).getClass(), List.of(1, 2, 3).getClass(), Collections.singletonList(1).getClass(),
                            Collections.unmodifiableList(new ArrayList<>()).getClass())
                    : List.of(Map.of("a", 1).getClass(), Map.of("a", 1, "b", 2).getClass(), Collections.singletonMap("a", 1).getClass(),
                            Collections.unmodifiableMap(new LinkedHashMap<>()).getClass());
        }

        Object expected(final boolean longs) {
            return this == LIST ? (longs ? List.of(1L, 2L) : List.of(1, 2)) : (longs ? Map.of("a", 1L, "b", 2L) : Map.of("a", 1, "b", 2));
        }

        String body(final boolean abacus, final boolean text) {
            if (text) {
                // Quoted numbers distinguish generic refinement from JSON's numeric inference.
                return this == LIST ? "[\"1\",\"2\"]" : "{\"a\":\"1\",\"b\":\"2\"}";
            }
            return this == LIST ? "<e>1</e><e>2</e>" : entry(abacus, "a", "", "1") + entry(abacus, "b", "", "2");
        }

        String element(final String metadata, final String body) {
            final String name = this == LIST ? "list" : "map";
            return "<" + name + attribute(metadata) + ">" + body + "</" + name + ">";
        }
    }

    enum Placement {
        PROPERTY, CHILD, ENCLOSING
    }

    static Stream<Arguments> containers() {
        return Stream.of(false, true)
                .flatMap(abacus -> Stream.of(XmlParserType.values())
                        .filter(backend -> abacus || backend != XmlParserType.SAX)
                        .flatMap(backend -> Stream.of(Shape.values())
                                .flatMap(shape -> Stream.of(false, true).map(text -> Arguments.of(abacus, backend, shape, text)))));
    }

    static Stream<Arguments> backends() {
        return XmlWrappedObjectRegressionTest.backends();
    }

    static Stream<XmlParserType> abacusBackends() {
        return Stream.of(XmlParserType.StAX, XmlParserType.DOM, XmlParserType.SAX);
    }

    static Stream<Arguments> roots() {
        // Maps are written as XML entries at the root; embedded JSON maps are property values.
        return containers().filter(args -> args.get()[2] == Shape.LIST || !(boolean) args.get()[3]);
    }

    static Stream<Arguments> properties() {
        return containers().flatMap(args -> Stream.of(Placement.values())
                .filter(placement -> placement != Placement.CHILD || args.get()[2] == Shape.LIST || !(boolean) args.get()[3])
                .filter(placement -> placement != Placement.CHILD || (boolean) args.get()[0] || args.get()[2] != Shape.LIST)
                .map(placement -> Arguments.of(args.get()[0], args.get()[1], args.get()[2], args.get()[3], placement)));
    }

    static Stream<Arguments> wrappers() {
        return properties().flatMap(args -> Stream.of(XmlWrappedObjectRegressionTest.Wrapper.values())
                .map(wrapper -> Arguments.of(args.get()[0], args.get()[1], args.get()[2], args.get()[3], args.get()[4], wrapper)));
    }

    private static XmlParser parser(final boolean abacus, final XmlParserType backend) {
        return abacus ? new AbacusXmlParserImpl(backend) : new XmlParserImpl(backend);
    }

    private static String attribute(final String metadata) {
        return metadata == null ? "" : " type=\"" + metadata.replace("<", "&lt;").replace(">", "&gt;") + "\"";
    }

    private static String entry(final boolean abacus, final String key, final String metadata, final String body) {
        return abacus ? "<entry><key>" + key + "</key><value" + metadata + ">" + body + "</value></entry>"
                : "<" + key + metadata + ">" + body + "</" + key + ">";
    }

    private static String propertyDocument(final boolean abacus, final Shape shape, final boolean text, final Placement placement, final String metadata) {
        final String body = shape.body(abacus, text);
        // XmlParser writes collection items directly in the property; Abacus gives them a separate list child.
        final String payload = (!text || placement == Placement.CHILD) && (abacus || shape == Shape.MAP)
                ? shape.element(placement == Placement.CHILD ? metadata : null, body)
                : body;
        final String rootType = placement == Placement.ENCLOSING ? "LinkedHashMap<String," + metadata + ">" : "LinkedHashMap";
        // DOM's established enclosing-type policy still applies when metadata is carried only by the child map.
        final String propertyType = placement == Placement.PROPERTY ? metadata : placement == Placement.CHILD && !abacus ? shape.schema("Object") : null;
        return "<map" + attribute(rootType) + ">" + entry(abacus, "k", attribute(propertyType), payload) + "</map>";
    }

    @SuppressWarnings("unchecked")
    private static void assertMutableValue(final Object value, final Shape shape, final boolean longs) {
        assertEquals(shape.expected(longs), value);
        if (shape == Shape.LIST) {
            final List<Object> list = assertInstanceOf(List.class, value);
            list.add(null);
            list.removeLast();
        } else {
            final Map<Object, Object> map = assertInstanceOf(Map.class, value);
            map.put("extra", null);
            map.remove("extra");
        }
    }

    @ParameterizedTest
    @MethodSource("roots")
    void rootMetadataRefinesOpenArgumentsOnMutableFallback(final boolean abacus, final XmlParserType backend, final Shape shape, final boolean text) {
        final XmlParser parser = parser(abacus, backend);
        for (final Class<?> implementation : shape.implementations()) {
            final String metadata = implementation.getName() + (shape == Shape.LIST ? "<Integer>" : "<String,Integer>");
            final String xml = shape.element(metadata, shape.body(abacus, text));
            assertMutableValue(parser.deserialize(xml, Type.of(shape.schema("Object"))), shape, false);
            assertMutableValue(parser.deserialize(xml, Type.of(shape.schema("Long"))), shape, true);
        }
    }

    @ParameterizedTest
    @MethodSource("properties")
    void nestedMetadataRefinesOpenArgumentsAtEveryPlacement(final boolean abacus, final XmlParserType backend, final Shape shape, final boolean text,
            final Placement placement) {
        final XmlParser parser = parser(abacus, backend);
        final String xml = propertyDocument(abacus, shape, text, placement, shape.metadata());
        for (final String argument : List.of("Object", "Long")) {
            final Map<?, ?> result = parser.deserialize(xml, Type.of("Map<String," + shape.schema(argument) + ">"));
            assertMutableValue(result.get("k"), shape, argument.equals("Long"));
        }
    }

    @ParameterizedTest
    @MethodSource("wrappers")
    void wrapperKeepsRefinedMutablePayload(final boolean abacus, final XmlParserType backend, final Shape shape, final boolean text, final Placement placement,
            final XmlWrappedObjectRegressionTest.Wrapper wrapper) {
        final String xml = propertyDocument(abacus, shape, text, placement, shape.metadata());
        final XmlParser parser = parser(abacus, backend);
        for (final String argument : List.of("Object", "Long")) {
            final Map<?, ?> result = parser.deserialize(xml, Type.of("Map<String," + wrapper.typeName + "<" + shape.schema(argument) + ">>"));
            assertMutableValue(wrapper.unwrap(result.get("k")), shape, argument.equals("Long"));
        }
    }

    @ParameterizedTest
    @MethodSource("containers")
    void configuredSpecificTypesAndImplementationsStillWin(final boolean abacus, final XmlParserType backend, final Shape shape, final boolean text) {
        final XmlParser parser = parser(abacus, backend);
        final String xml = propertyDocument(abacus, shape, text, Placement.PROPERTY, shape.metadata());
        final String configured = shape == Shape.LIST ? "LinkedList<Long>" : "LinkedHashMap<String,Long>";
        final XmlDeserConfig config = XmlDeserConfig.create().setValueType("k", Type.of(configured));
        final Map<?, ?> result = parser.deserialize(xml, config, Type.of("Map<String," + shape.schema("Object") + ">"));
        assertMutableValue(result.get("k"), shape, true);
        assertEquals(shape == Shape.LIST ? LinkedList.class : LinkedHashMap.class, result.get("k").getClass());
    }

    @ParameterizedTest
    @MethodSource("containers")
    void incompatibleMetadataCannotRefineConfiguredOpenArguments(final boolean abacus, final XmlParserType backend, final Shape shape, final boolean text) {
        final XmlParser parser = parser(abacus, backend);
        final String xml = propertyDocument(abacus, shape, text, Placement.PROPERTY, shape.metadata());
        final String configured = shape == Shape.LIST ? "LinkedList<Object>" : "LinkedHashMap<String,Object>";
        final XmlDeserConfig config = XmlDeserConfig.create().setValueType("k", Type.of(configured));
        final Map<?, ?> result = parser.deserialize(xml, config, Type.of("Map<String," + shape.schema("Object") + ">"));
        // A private List/Map implementation is incompatible with a specifically declared sibling implementation.
        assertEquals(shape == Shape.LIST ? LinkedList.class : LinkedHashMap.class, result.get("k").getClass());
        assertEquals(shape == Shape.LIST ? List.of("1", "2") : Map.of("a", "1", "b", "2"), result.get("k"));
    }

    @ParameterizedTest
    @MethodSource("containers")
    void compatibleConfiguredOpenArgumentsCanUseMetadata(final boolean abacus, final XmlParserType backend, final Shape shape, final boolean text) {
        final XmlParser parser = parser(abacus, backend);
        final String xml = propertyDocument(abacus, shape, text, Placement.PROPERTY, shape.metadata());
        final XmlDeserConfig config = XmlDeserConfig.create().setValueType("k", Type.of(shape.schema("Object")));
        final Map<?, ?> result = parser.deserialize(xml, config, Type.of("Map<String,Object>"));
        assertMutableValue(result.get("k"), shape, false);
    }

    @ParameterizedTest
    @MethodSource("roots")
    void globalConfiguredArgumentsKeepPrecedence(final boolean abacus, final XmlParserType backend, final Shape shape, final boolean text) {
        final XmlParser parser = parser(abacus, backend);
        final String xml = shape.element(shape.metadata(), shape.body(abacus, text));
        final XmlDeserConfig config = shape == Shape.LIST ? XmlDeserConfig.create().setElementType(Long.class)
                : XmlDeserConfig.create().setMapValueType(Long.class);
        assertMutableValue(parser.deserialize(xml, config, Type.of(shape.schema("Object"))), shape, true);
    }

    @ParameterizedTest
    @MethodSource("backends")
    void immutableMapMetadataRefinesKeysIndependentlyOfValues(final boolean abacus, final XmlParserType backend) {
        final XmlParser parser = parser(abacus, backend);
        final String metadata = Map.of(1L, 2).getClass().getName() + "<Long,Integer>";
        final String body = abacus ? entry(true, "1", "", "2") : "<e name=\"1\">2</e>";
        final String xml = Shape.MAP.element(metadata, body);
        assertEquals(Map.of(1L, 2), parser.deserialize(xml, Type.of("Map<Object,Object>")));
        assertEquals(Map.of("1", 2L), parser.deserialize(xml, Type.of("Map<String,Long>")));
    }

    @ParameterizedTest
    @MethodSource("abacusBackends")
    void immutableMetadataCanRefineContainerMapKeys(final XmlParserType backend) {
        final XmlParser parser = parser(true, backend);
        for (final Shape shape : Shape.values()) {
            for (final boolean text : new boolean[] { false, true }) {
                final String body = text ? shape.body(true, true) : shape.element(null, shape.body(true, false));
                final String xml = "<map><entry><key" + attribute(shape.metadata()) + ">" + body + "</key><value>kept</value></entry></map>";
                final Map<?, ?> result = parser.deserialize(xml, Type.of("Map<" + shape.schema("Object") + ",String>"));
                assertEquals(Map.of(shape.expected(false), "kept"), result);
            }
        }
    }

    @ParameterizedTest
    @MethodSource("backends")
    void configuredParameterizedElementsSurviveTextConversion(final boolean abacus, final XmlParserType backend) {
        final String metadata = List.of(1, 2).getClass().getName() + "<List<Integer>>";
        final String xml = Shape.LIST.element(metadata, "[[\"1\",\"2\"]]");
        final XmlDeserConfig config = XmlDeserConfig.create().setElementType(Type.of("List<Long>"));
        assertEquals(List.of(List.of(1L, 2L)), parser(abacus, backend).deserialize(xml, config, Type.of("List<Object>")));
    }

    @ParameterizedTest
    @MethodSource("containers")
    void beanPropertiesUseTheSameMutableSchemaFallback(final boolean abacus, final XmlParserType backend, final Shape shape, final boolean text) {
        final XmlParser parser = abacus ? new AbacusXmlParserImpl(backend, null, null, Set.of(Holder.class))
                : new XmlParserImpl(backend, null, null, Set.of(Holder.class));
        final String name = shape == Shape.LIST ? "items" : "values";
        final String content = shape.body(abacus, text);
        final String payload = !text && (abacus || shape == Shape.MAP) ? shape.element(null, content) : content;
        final String xml = "<Holder" + attribute(Holder.class.getCanonicalName()) + "><" + name + attribute(shape.metadata()) + ">" + payload + "</" + name
                + "></Holder>";
        final Holder result = parser.deserialize(xml, Holder.class);
        assertMutableValue(shape == Shape.LIST ? result.getItems() : result.getValues(), shape, false);
    }

    @ParameterizedTest
    @MethodSource("properties")
    void unapprovedNestedArgumentIsRejectedDespiteMutableFallback(final boolean abacus, final XmlParserType backend, final Shape shape, final boolean text,
            final Placement placement) {
        final String xml = propertyDocument(abacus, shape, text, placement, shape.metadata().replace("Integer", "not.approved.Payload"));
        assertThrows(ParsingException.class, () -> parser(abacus, backend).deserialize(xml, Type.of("Map<String," + shape.schema("Object") + ">")));
    }

    public static class Holder {
        private List<Object> items;
        private Map<String, Object> values;

        public List<Object> getItems() {
            return items;
        }

        public void setItems(final List<Object> items) {
            this.items = items;
        }

        public Map<String, Object> getValues() {
            return values;
        }

        public void setValues(final Map<String, Object> values) {
            this.values = values;
        }
    }
}
