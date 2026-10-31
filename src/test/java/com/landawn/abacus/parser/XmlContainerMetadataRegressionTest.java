package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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

    // ---- deep review 2026-10-27 G140 begin ----

    // JDK containers outside the public API with a no-argument constructor that reflection reaches only with
    // --add-opens java.base/java.util, which this suite passes. Metadata naming them must take the readers' fallback
    // in every JVM instead of producing the immutable instance (or a second instance of a JDK singleton).
    static Stream<Arguments> nonPublicJdkContainerCases() {
        final Map<String, Object> values = new LinkedHashMap<>();
        values.put("emptyList", Collections.emptyList());
        values.put("emptySet", Collections.emptySet());
        values.put("emptyMap", Collections.emptyMap());
        values.put("emptySortedSet", Collections.emptySortedSet());
        values.put("emptyNavigableSet", Collections.emptyNavigableSet());
        values.put("emptySortedMap", Collections.emptySortedMap());
        values.put("emptyNavigableMap", Collections.emptyNavigableMap());

        return backends().flatMap(
                backend -> values.entrySet().stream().map(entry -> Arguments.of(backend.get()[0], backend.get()[1], entry.getKey(), entry.getValue())));
    }

    public static class NonPublicJdkContainerBean {
        private Object obj;
        private Collection<Object> coll;
        private List<Object> list;
        private Set<Object> set;
        private Map<String, Object> map;

        public Object getObj() {
            return obj;
        }

        public void setObj(final Object obj) {
            this.obj = obj;
        }

        public Collection<Object> getColl() {
            return coll;
        }

        public void setColl(final Collection<Object> coll) {
            this.coll = coll;
        }

        public List<Object> getList() {
            return list;
        }

        public void setList(final List<Object> list) {
            this.list = list;
        }

        public Set<Object> getSet() {
            return set;
        }

        public void setSet(final Set<Object> set) {
            this.set = set;
        }

        public Map<String, Object> getMap() {
            return map;
        }

        public void setMap(final Map<String, Object> map) {
            this.map = map;
        }
    }

    private static XmlParser nonPublicJdkContainerParser(final boolean abacus, final XmlParserType backend) {
        final Set<Class<?>> approved = Set.of(NonPublicJdkContainerBean.class);
        return abacus ? new AbacusXmlParserImpl(backend, null, null, approved) : new XmlParserImpl(backend, null, null, approved);
    }

    // The reader's own fallback: a mutable public container with the same content, never the JDK class the metadata names.
    @SuppressWarnings("unchecked")
    private static void assertMutableFallback(final Object expected, final Object actual, final String xml) {
        assertNotNull(actual, xml);
        assertTrue(Modifier.isPublic(actual.getClass().getModifiers()), () -> actual.getClass().getName() + " read from " + xml);
        if (expected instanceof Map<?, ?> expectedMap) {
            final Map<Object, Object> map = assertInstanceOf(Map.class, actual, xml);
            assertEquals(expectedMap, map, xml);
            map.put("marker", "v");
            map.remove("marker");
        } else {
            final Collection<Object> collection = assertInstanceOf(Collection.class, actual, xml);
            assertEquals(new ArrayList<>((Collection<?>) expected), new ArrayList<>(collection), xml);
            collection.add("marker");
            collection.remove("marker");
        }
    }

    // The writer records the runtime class: the root, an Object map value and every bean property must read it into the
    // fallback, as when java.util is not opened (r108 returned the Collections$Empty* instance under --add-opens).
    @ParameterizedTest(name = "{0}, {1}, {2}")
    @MethodSource("nonPublicJdkContainerCases")
    @SuppressWarnings("unchecked")
    void nonPublicJdkContainerMetadataTakesTheFallbackRegardlessOfAddOpens(final boolean abacus, final XmlParserType backend, final String description,
            final Object value) {
        final XmlParser parser = nonPublicJdkContainerParser(abacus, backend);
        final XmlSerConfig withTypes = XmlSerConfig.create().setWriteTypeInfo(true);
        final Class<?> declared = value instanceof Map ? Map.class : value instanceof Set ? Set.class : List.class;

        final String rootXml = parser.serialize(value, withTypes);
        assertTrue(rootXml.contains(value.getClass().getCanonicalName()), rootXml);
        assertMutableFallback(value, parser.deserialize(rootXml, declared), rootXml);
        assertMutableFallback(value, parser.deserialize(rootXml, Object.class), rootXml);

        final Map<String, Object> source = new LinkedHashMap<>();
        source.put("k", value);
        source.put("tail", "end");
        final String entryXml = parser.serialize(source, withTypes);
        final Map<String, Object> entry = parser.deserialize(entryXml, Type.of("LinkedHashMap<String, Object>"));
        assertMutableFallback(value, entry.get("k"), entryXml);
        assertEquals("end", entry.get("tail"), entryXml);

        final NonPublicJdkContainerBean bean = new NonPublicJdkContainerBean();
        bean.setObj(value);
        if (value instanceof Map) {
            bean.setMap((Map<String, Object>) value);
        } else {
            bean.setColl((Collection<Object>) value);
            if (value instanceof List) {
                bean.setList((List<Object>) value);
            } else {
                bean.setSet((Set<Object>) value);
            }
        }
        final String beanXml = parser.serialize(bean, withTypes);
        final NonPublicJdkContainerBean back = parser.deserialize(beanXml, NonPublicJdkContainerBean.class);
        assertMutableFallback(value, back.getObj(), beanXml);
        if (value instanceof Map) {
            assertMutableFallback(value, back.getMap(), beanXml);
        } else {
            assertMutableFallback(value, back.getColl(), beanXml);
            assertMutableFallback(value, value instanceof List ? back.getList() : back.getSet(), beanXml);
        }
    }

    // Metadata naming such a class for a non-empty value: under --add-opens r108 constructed the immutable instance and
    // failed with UnsupportedOperationException at the first element; the fallback reads the content.
    @ParameterizedTest
    @MethodSource("backends")
    void nonPublicJdkContainerMetadataWithContentIsReadIntoTheFallback(final boolean abacus, final XmlParserType backend) {
        final XmlParser parser = nonPublicJdkContainerParser(abacus, backend);
        for (final String body : List.of("[\"a\",\"b\"]", "<e>a</e><e>b</e>")) {
            final String listXml = "<list type=\"java.util.Collections$EmptyList\">" + body + "</list>";
            assertEquals(List.of("a", "b"), assertInstanceOf(ArrayList.class, parser.deserialize(listXml, List.class), listXml), listXml);
            assertEquals(List.of("a", "b"), assertInstanceOf(ArrayList.class, parser.deserialize(listXml, Object.class), listXml), listXml);

            final String setXml = "<list type=\"java.util.Collections.EmptySet\">" + body + "</list>";
            assertEquals(Set.of("a", "b"), assertInstanceOf(HashSet.class, parser.deserialize(setXml, Set.class), setXml), setXml);
            assertEquals(Set.of("a", "b"), assertInstanceOf(LinkedHashSet.class, parser.deserialize(setXml, Object.class), setXml), setXml);
        }

        final String mapXml = "<map type=\"java.util.Collections$EmptyMap\">{\"a\":\"b\"}</map>";
        assertEquals(Map.of("a", "b"), assertInstanceOf(HashMap.class, parser.deserialize(mapXml, Map.class), mapXml), mapXml);
        assertEquals(Map.of("a", "b"), assertInstanceOf(LinkedHashMap.class, parser.deserialize(mapXml, Object.class), mapXml), mapXml);
    }

    // The shared helpers every reader uses for metadata: getConcreteClass drops the class and newPropInstance treats it
    // as a failed construction. Public JDK, Abacus and application containers keep their metadata class.
    @org.junit.jupiter.api.Test
    void sharedConstructionHelpersNeverConstructNonPublicJdkContainers() {
        for (final Object value : List.of(Collections.emptyList(), Collections.emptySet(), Collections.emptyMap(), Collections.emptyNavigableSet(),
                Collections.emptyNavigableMap(), new HashMap<>().keySet(), new HashMap<>().values())) {
            final Class<?> cls = value.getClass();
            final Class<?> declared = value instanceof Map ? Map.class : value instanceof Set ? Set.class : Collection.class;
            assertSame(declared, AbstractParser.getConcreteClass(cls, declared), cls.getName());
            assertSame(Object.class, AbstractParser.getConcreteClass(cls, Object.class), cls.getName());
            final Object created = AbstractParser.newPropInstance(declared, cls);
            assertTrue(Modifier.isPublic(created.getClass().getModifiers()), created.getClass().getName());
            assertThrows(ParsingException.class, () -> AbstractParser.newPropInstance(null, cls), cls.getName());
        }

        for (final Class<?> cls : List.of(ArrayList.class, LinkedList.class, HashSet.class, LinkedHashSet.class, TreeSet.class, java.util.ArrayDeque.class,
                java.util.Vector.class, HashMap.class, LinkedHashMap.class, TreeMap.class, IdentityHashMap.class, java.util.concurrent.ConcurrentHashMap.class,
                java.util.concurrent.CopyOnWriteArrayList.class, IntegerList.class, IdentityIntegerSet.class)) {
            final Class<?> declared = Map.class.isAssignableFrom(cls) ? Map.class : Collection.class;
            assertSame(cls, AbstractParser.getConcreteClass(cls, declared), cls.getName());
            assertSame(cls, AbstractParser.newPropInstance(declared, cls).getClass(), cls.getName());
        }
    }

    private static class PrivateApplicationList extends ArrayList<Object> {
        private static final long serialVersionUID = 1L;
    }

    @org.junit.jupiter.api.Test
    void nonPublicJdkContainerClassIsDecidedByModuleAndVisibilityNotByJvmFlags() {
        for (final Object value : List.of(Collections.emptyList(), Collections.emptySet(), Collections.emptyMap(), Collections.emptySortedSet(),
                Collections.emptyNavigableMap(), Collections.singletonList(1), Collections.singleton(1), Collections.singletonMap(1, 1),
                Collections.unmodifiableList(new ArrayList<>()), Collections.unmodifiableMap(new HashMap<>()), Collections.synchronizedList(new ArrayList<>()),
                Collections.checkedSet(new HashSet<>(), Object.class), Collections.nCopies(1, 1), List.of(), List.of(1), Set.of(1), Map.of(),
                java.util.Arrays.asList(1), new HashMap<>().keySet(), new HashMap<>().values(), new TreeMap<>().keySet())) {
            assertTrue(AbstractParser.isNonPublicJdkContainerClass(value.getClass()), value.getClass().getName());
        }

        for (final Class<?> cls : List.of(ArrayList.class, LinkedList.class, HashSet.class, LinkedHashSet.class, TreeSet.class, java.util.ArrayDeque.class,
                HashMap.class, LinkedHashMap.class, TreeMap.class, IdentityHashMap.class, java.util.Properties.class,
                java.util.concurrent.ConcurrentHashMap.class, java.util.concurrent.ConcurrentHashMap.newKeySet().getClass(),
                java.util.concurrent.CopyOnWriteArrayList.class, com.landawn.abacus.util.ImmutableList.of(1).getClass(), IntegerList.class,
                IdentityIntegerSet.class, PrivateApplicationList.class, new ArrayList<Object>() {
                    private static final long serialVersionUID = 1L;
                }.getClass(), List.class, java.util.AbstractList.class, String.class, Collections.class)) {
            assertFalse(AbstractParser.isNonPublicJdkContainerClass(cls), cls.getName());
        }
    }

    // ---- deep review 2026-10-27 G140 end ----
}
