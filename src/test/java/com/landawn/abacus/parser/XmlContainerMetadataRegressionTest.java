package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Stream;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.ParsingException;
import com.landawn.abacus.type.Type;

@Tag("unit")
public class XmlContainerMetadataRegressionTest extends TestBase {
    static Stream<Arguments> backends() {
        return XmlWrappedObjectRegressionTest.backends();
    }

    static Stream<XmlParserType> xmlBackends() {
        return XmlWrappedObjectRegressionTest.xmlBackends();
    }

    static Stream<Arguments> wrappedContainerFallbackCases() {
        final Map<String, Object> values = new LinkedHashMap<>();
        values.put("empty List.of", List.of());
        values.put("singleton List.of", List.of("a"));
        values.put("two-element List.of", List.of("a", "b"));
        values.put("three-element List.of", List.of("a", "b", "c"));
        values.put("empty Map.of", Map.of());
        values.put("singleton Map.of", Map.of("a", "b"));
        values.put("two-entry Map.of", Map.of("a", "b", "c", "d"));
        values.put("empty Set.of", Set.of());
        values.put("singleton Set.of", Set.of("a"));
        values.put("two-element Set.of", Set.of("a", "b"));
        values.put("emptyList", Collections.emptyList());
        values.put("emptyMap", Collections.emptyMap());
        values.put("emptySet", Collections.emptySet());
        values.put("singletonList", Collections.singletonList("a"));
        values.put("singletonMap", Collections.singletonMap("a", "b"));
        values.put("singletonSet", Collections.singleton("a"));
        values.put("unmodifiable random-access list", Collections.unmodifiableList(new ArrayList<>(List.of("a", "b"))));
        values.put("unmodifiable sequential list", Collections.unmodifiableList(new LinkedList<>(List.of("a", "b"))));
        values.put("unmodifiable map", Collections.unmodifiableMap(new LinkedHashMap<>(Map.of("a", "b"))));
        values.put("unmodifiable set", Collections.unmodifiableSet(new LinkedHashSet<>(List.of("a", "b"))));
        values.put("unmodifiable sorted map", Collections.unmodifiableSortedMap(new TreeMap<>(Map.of("a", "b"))));
        values.put("unmodifiable sorted set", Collections.unmodifiableSortedSet(new TreeSet<>(List.of("a", "b"))));
        values.put("fixed-size list", java.util.Arrays.asList("a", "b"));

        final List<Arguments> cases = new ArrayList<>();
        for (final XmlParserType backend : XmlParserType.values()) {
            for (final XmlWrappedObjectRegressionTest.Wrapper wrapper : XmlWrappedObjectRegressionTest.Wrapper.values()) {
                values.forEach((name, value) -> cases.add(Arguments.of(backend, wrapper, name, value)));
            }
        }
        return cases.stream();
    }

    private static XmlParser parser(final boolean abacus, final XmlParserType backend) {
        return abacus ? new AbacusXmlParserImpl(backend, null, null, Set.of(IntegerList.class, IdentityIntegerSet.class))
                : new XmlParserImpl(backend, null, null, Set.of(IntegerList.class, IdentityIntegerSet.class));
    }

    // Runtime implementation metadata must remain a hint when the reader cannot construct that implementation.
    @ParameterizedTest(name = "{0}, {1}, {2}")
    @MethodSource("wrappedContainerFallbackCases")
    void wrappedObjectRetainsMutableContainerFallback(final XmlParserType backend, final XmlWrappedObjectRegressionTest.Wrapper wrapper,
            final String description, final Object value) {
        final XmlParser parser = parser(true, backend);
        final Map<String, Object> source = new LinkedHashMap<>();
        source.put("k", wrapper.wrap(value));
        source.put("tail", wrapper.wrap("end"));
        final Type<Map<String, Object>> target = Type.of("LinkedHashMap<String, " + wrapper.typeName + "<Object>>");

        for (final boolean typeInfo : new boolean[] { false, true }) {
            final String xml = parser.serialize(source, XmlSerConfig.create().setWriteTypeInfo(typeInfo));
            final Map<String, Object> actual = parser.deserialize(xml, target);
            final Object held = wrapper.unwrap(actual.get("k"));
            if (value instanceof Map<?, ?> expected) {
                assertEquals(expected, assertInstanceOf(Map.class, held, xml), xml);
            } else {
                assertEquals(new ArrayList<>((Collection<?>) value), new ArrayList<>(assertInstanceOf(Collection.class, held, xml)), xml);
                // Approved metadata preserves the collection kind even when its immutable implementation
                // cannot be constructed. Without metadata the XML sequence retains its list fallback.
                if (typeInfo && value instanceof Set<?>) {
                    assertInstanceOf(LinkedHashSet.class, held, xml);
                } else {
                    assertInstanceOf(ArrayList.class, held, xml);
                }
                @SuppressWarnings("unchecked")
                final Collection<Object> mutable = (Collection<Object>) held;
                final Object marker = new Object();
                mutable.add(marker);
                mutable.remove(marker);
            }
            assertEquals(wrapper.wrap("end"), actual.get("tail"), xml);
        }
    }

    // Payload metadata may choose a compatible implementation, but its String argument cannot replace Integer.
    @ParameterizedTest
    @MethodSource("xmlBackends")
    void wrapperRetainsDeclaredArgumentsForTextAndElementForms(final XmlParserType backend) {
        final XmlParser parser = parser(false, backend);
        for (final XmlWrappedObjectRegressionTest.Wrapper wrapper : XmlWrappedObjectRegressionTest.Wrapper.values()) {
            final Type<Map<String, Object>> target = Type.of("Map<String, " + wrapper.typeName + "<List<Integer>>>");
            for (final String content : List.of("[1,2]", "<e>1</e><e>2</e>")) {
                final String xml = "<map><k type=\"LinkedList&lt;String&gt;\">" + content + "</k></map>";
                final Map<String, Object> actual = parser.deserialize(xml, target);
                final List<?> held = (List<?>) wrapper.unwrap(actual.get("k"));
                assertEquals(List.of(1, 2), held, xml);
                assertInstanceOf(LinkedList.class, held, xml);

                final XmlDeserConfig configured = XmlDeserConfig.create().setValueType("k", target.parameterTypes().get(1));
                final Map<?, ?> configuredResult = parser.deserialize(xml, configured, Map.class);
                assertEquals(wrapper.wrap(List.of(1, 2)), configuredResult.get("k"), xml);
                assertThrows(ParsingException.class, () -> parser.deserialize(xml.replace("String", "not.approved.Type"), target));
            }
        }
    }

    @ParameterizedTest
    @MethodSource("xmlBackends")
    void wrapperRetainsDeeperArgumentsAndMapImplementation(final XmlParserType backend) {
        final XmlParser parser = parser(false, backend);
        final String listXml = "<map><k type=\"LinkedList&lt;Map&lt;String, String&gt;&gt;\">[{\"x\":\"1\"}]</k></map>";
        final Map<String, java.util.Optional<List<Map<String, Integer>>>> listResult = parser.deserialize(listXml,
                Type.of("Map<String, java.util.Optional<List<Map<String, Integer>>>>"));
        assertEquals(List.of(Map.of("x", 1)), listResult.get("k").orElseThrow(), listXml);
        assertInstanceOf(LinkedList.class, listResult.get("k").orElseThrow(), listXml);

        final String mapXml = "<map><k type=\"TreeMap&lt;String, String&gt;\">{\"x\":\"1\"}</k></map>";
        final Map<String, java.util.Optional<Map<String, Integer>>> mapResult = parser.deserialize(mapXml,
                Type.of("Map<String, java.util.Optional<Map<String, Integer>>>"));
        assertEquals(Map.of("x", 1), mapResult.get("k").orElseThrow(), mapXml);
        assertInstanceOf(java.util.TreeMap.class, mapResult.get("k").orElseThrow(), mapXml);
    }

    // A JSON-text body must populate the same implementation that element-by-element reading would construct.
    @ParameterizedTest
    @MethodSource("backends")
    void textCollectionKeepsRuntimeClassAndElementSchema(final boolean abacus, final XmlParserType backend) {
        final XmlParser parser = parser(abacus, backend);
        for (final String body : List.of("[3,1,3]", "[]")) {
            final String xml = "<list type=\"LinkedList&lt;Object&gt;\">" + body + "</list>";
            final List<?> actual = parser.deserialize(xml, Type.of("List<Integer>"));
            assertEquals(body.equals("[]") ? List.of() : List.of(3, 1, 3), actual, xml);
            assertInstanceOf(LinkedList.class, actual, xml);
        }

        final LinkedList<Integer> source = new LinkedList<>(List.of(3, 1, 3));
        final String written = parser.serialize(source, XmlSerConfig.create().setWriteTypeInfo(true));
        final List<?> roundTrip = parser.deserialize(written, Type.of("List<Integer>"));
        assertEquals(source, roundTrip, written);
        assertInstanceOf(LinkedList.class, roundTrip, written);

        final String nested = "<list type=\"LinkedList&lt;Object&gt;\">[{\"x\":\"1\"}]</list>";
        final List<?> nestedResult = parser.deserialize(nested, Type.of("List<Map<String, Integer>>"));
        assertEquals(List.of(Map.of("x", 1)), nestedResult, nested);
        assertInstanceOf(LinkedList.class, nestedResult, nested);
    }

    @ParameterizedTest
    @MethodSource("backends")
    void textCollectionKeepsSortingAndDuplicateRemoval(final boolean abacus, final XmlParserType backend) {
        final XmlParser parser = parser(abacus, backend);
        final String xml = "<set type=\"TreeSet&lt;Object&gt;\">[3,1,3,2]</set>";
        final Collection<?> actual = parser.deserialize(xml, Type.of("Collection<Integer>"));
        assertInstanceOf(TreeSet.class, actual, xml);
        assertEquals(List.of(1, 2, 3), new ArrayList<>(actual), xml);
    }

    // This implementation has no type variables: the declared List<Integer> schema cannot be appended to its name.
    @ParameterizedTest
    @MethodSource("backends")
    void textCollectionSupportsFixedArgumentSubclass(final boolean abacus, final XmlParserType backend) {
        final XmlParser parser = parser(abacus, backend);
        final String xml = "<list type=\"" + IntegerList.class.getCanonicalName() + "\">[3,1,3]</list>";
        final List<?> actual = parser.deserialize(xml, Type.of("List<Integer>"));
        assertEquals(List.of(3, 1, 3), actual, xml);
        assertInstanceOf(IntegerList.class, actual, xml);
    }

    // Decoding through HashSet before copying would collapse these distinct, equal Integer instances.
    @ParameterizedTest
    @MethodSource("backends")
    void textCollectionUsesRuntimeMembershipWhileParsing(final boolean abacus, final XmlParserType backend) {
        final XmlParser parser = parser(abacus, backend);
        final String xml = "<set type=\"" + IdentityIntegerSet.class.getCanonicalName() + "\">[1000,1000]</set>";
        final Set<?> actual = parser.deserialize(xml, Type.of("Set<Integer>"));
        assertInstanceOf(IdentityIntegerSet.class, actual, xml);
        assertEquals(2, actual.size(), xml);
        actual.forEach(value -> assertEquals(1000, value));
    }

    @ParameterizedTest
    @MethodSource("xmlBackends")
    void wrappedCollectionUsesRuntimeMembershipWhileParsing(final XmlParserType backend) {
        final XmlParser parser = parser(false, backend);
        for (final String content : List.of("[1000,1000]", "<e>1000</e><e>1000</e>")) {
            final String xml = "<map><k type=\"" + IdentityIntegerSet.class.getCanonicalName() + "\">" + content + "</k></map>";
            final Map<String, java.util.Optional<Set<Integer>>> actual = parser.deserialize(xml, Type.of("Map<String, java.util.Optional<Set<Integer>>>"));
            final Set<Integer> held = actual.get("k").orElseThrow();
            assertInstanceOf(IdentityIntegerSet.class, held, xml);
            assertEquals(2, held.size(), xml);
        }
    }

    @ParameterizedTest
    @MethodSource("xmlBackends")
    void wrappedMapUsesRuntimeMembershipWhileParsing(final XmlParserType backend) {
        final XmlParser parser = parser(false, backend);
        for (final String content : List.of("{\"1000\":1,\"1000\":2}", "<map><property name=\"1000\">1</property><property name=\"1000\">2</property></map>")) {
            final String xml = "<map><k type=\"IdentityHashMap&lt;Integer,Integer&gt;\">" + content + "</k></map>";
            final Map<String, java.util.Optional<Map<Integer, Integer>>> actual = parser.deserialize(xml,
                    Type.of("Map<String, java.util.Optional<Map<Integer, Integer>>>"));
            final Map<Integer, Integer> held = actual.get("k").orElseThrow();
            assertInstanceOf(IdentityHashMap.class, held, xml);
            assertEquals(2, held.size(), xml);
            assertEquals(Set.of(1, 2), Set.copyOf(held.values()), xml);
        }
    }

    @ParameterizedTest
    @MethodSource("backends")
    void incompatibleAndUnapprovedCollectionMetadataKeepExistingRules(final boolean abacus, final XmlParserType backend) {
        final XmlParser parser = parser(abacus, backend);
        final String xml = "<list type=\"TreeSet&lt;Object&gt;\">[3,1,3]</list>";
        final List<?> actual = parser.deserialize(xml, Type.of("List<Integer>"));
        assertEquals(List.of(3, 1, 3), actual, xml);
        assertThrows(ParsingException.class, () -> parser.deserialize(xml.replace("TreeSet", "not.approved.Type"), Type.of("List<Integer>")));
    }

    public static class IntegerList extends LinkedList<Integer> {
        private static final long serialVersionUID = 1L;
    }

    public static class IdentityIntegerSet extends AbstractSet<Integer> {
        private final Set<Integer> values = Collections.newSetFromMap(new IdentityHashMap<>());

        @Override
        public Iterator<Integer> iterator() {
            return values.iterator();
        }

        @Override
        public int size() {
            return values.size();
        }

        @Override
        public boolean add(final Integer value) {
            return values.add(value);
        }
    }
}
