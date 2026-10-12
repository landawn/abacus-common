package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
public class XmlNestedConstructionRegressionTest extends TestBase {
    private static final String DUPLICATE_KEYS = "{\"a\":1,\"a\":2}";

    static Stream<Arguments> backends() {
        return XmlWrappedObjectRegressionTest.backends();
    }

    static Stream<Arguments> wrappers() {
        return XmlPartialMetadataRegressionTest.wrappers();
    }

    static Stream<XmlParserType> abacusBackends() {
        return Stream.of(XmlParserType.StAX, XmlParserType.DOM, XmlParserType.SAX);
    }

    private static XmlParser parser(final boolean abacus, final XmlParserType backend) {
        final Set<Class<?>> approved = Set.of(org.springframework.util.LinkedMultiValueMap.class, XmlNestedSchemaRegressionTest.ReversedMap.class,
                XmlNestedSchemaRegressionTest.StringKeyMap.class);
        return abacus ? new AbacusXmlParserImpl(backend, null, null, approved) : new XmlParserImpl(backend, null, null, approved);
    }

    private static String map(final boolean abacus, final String valueMetadata, final String body) {
        final String entry = abacus ? "<entry><key>k</key><value>" + body + "</value></entry>" : "<k>" + body + "</k>";
        return "<map type=\"" + ("LinkedHashMap<String," + valueMetadata + ">").replace("<", "&lt;").replace(">", "&gt;") + "\">" + entry + "</map>";
    }

    private static void assertIdentityEntries(final Object actual) {
        final Map<?, ?> map = assertInstanceOf(IdentityHashMap.class, actual);
        assertEquals(2, map.size(), "Equal keys must remain distinct until inserted into the identity map");
        assertEquals(List.of(1, 2), map.values().stream().map(v -> assertInstanceOf(Integer.class, v)).sorted().toList());
        assertEquals(List.of("a", "a"), new ArrayList<>(map.keySet()));
    }

    // Constructing a HashMap and copying it later loses the first equal key permanently.
    @ParameterizedTest
    @MethodSource("backends")
    void nestedIdentityMapKeepsEveryEntryAndDeclaredValues(final boolean abacus, final XmlParserType backend) {
        final XmlParser parser = parser(abacus, backend);
        for (final String metadataValue : List.of("Integer", "String", "Object")) {
            final String xml = map(abacus, "IdentityHashMap<String," + metadataValue + ">", DUPLICATE_KEYS);
            final Map<?, ?> actual = parser.deserialize(xml, Type.of("Map<String,Map<String,Integer>>"));
            assertIdentityEntries(actual.get("k"));
        }
    }

    @ParameterizedTest
    @MethodSource("backends")
    void nestedCollectionsKeepOrderingMembershipAndDeclaredElements(final boolean abacus, final XmlParserType backend) {
        final XmlParser parser = parser(abacus, backend);
        final Map<?, ?> sorted = parser.deserialize(map(abacus, "TreeSet<String>", "[3,1,3]"), Type.of("Map<String,Collection<Integer>>"));
        assertEquals(List.of(1, 3), new ArrayList<>(assertInstanceOf(TreeSet.class, sorted.get("k"))));
        final Map<?, ?> linked = parser.deserialize(map(abacus, "LinkedList<String>", "[3,1,3]"), Type.of("Map<String,List<Integer>>"));
        assertEquals(List.of(3, 1, 3), assertInstanceOf(LinkedList.class, linked.get("k")));
    }

    @ParameterizedTest
    @MethodSource("backends")
    void constructionHintsSurviveMultipleLevelsAndArrayComponents(final boolean abacus, final XmlParserType backend) {
        final XmlParser parser = parser(abacus, backend);
        for (final boolean array : new boolean[] { false, true }) {
            final String metadata = array ? "IdentityHashMap<String,String>[]" : "LinkedList<IdentityHashMap<String,String>>";
            final String declared = array ? "Map<String,Integer>[]" : "List<Map<String,Integer>>";
            final String xml = map(abacus, metadata, "[" + DUPLICATE_KEYS + "]");
            final Map<?, ?> actual = parser.deserialize(xml, Type.of("Map<String," + declared + ">"));
            if (array) {
                assertIdentityEntries(((Object[]) actual.get("k"))[0]);
            } else {
                assertIdentityEntries(assertInstanceOf(LinkedList.class, actual.get("k")).getFirst());
            }
        }
    }

    @ParameterizedTest
    @MethodSource("wrappers")
    void nestedWrappersKeepPayloadConstructionAndConstraints(final boolean abacus, final XmlParserType backend,
            final XmlWrappedObjectRegressionTest.Wrapper wrapper) {
        final XmlParser parser = parser(abacus, backend);
        final String xml = map(abacus, "IdentityHashMap<String,String>", DUPLICATE_KEYS);
        final Map<?, ?> actual = parser.deserialize(xml, Type.of("Map<String," + wrapper.typeName + "<Map<String,Integer>>>"));
        assertIdentityEntries(wrapper.unwrap(actual.get("k")));

        final String multiValueXml = map(abacus, "org.springframework.util.LinkedMultiValueMap<String,String>", "{\"a\":[\"1\",\"2\"]}");
        final Map<?, ?> multiValue = parser.deserialize(multiValueXml, Type.of("Map<String," + wrapper.typeName + "<Map<String,List<Integer>>>>"));
        final Object held = wrapper.unwrap(multiValue.get("k"));
        assertEquals(Map.of("a", List.of(1, 2)), held);
        assertInstanceOf(org.springframework.util.LinkedMultiValueMap.class, held);
    }

    @ParameterizedTest
    @MethodSource("abacusBackends")
    void nestedMapKeysKeepTheirConstructionAndGenericConstraints(final XmlParserType backend) {
        final String xml = "<map type=\"LinkedHashMap&lt;IdentityHashMap&lt;String,String&gt;,String&gt;\"><entry><key>" + DUPLICATE_KEYS
                + "</key><value>kept</value></entry></map>";
        final Map<?, ?> actual = parser(true, backend).deserialize(xml, Type.of("Map<Map<String,Integer>,String>"));
        assertEquals(1, actual.size());
        assertEquals("kept", actual.values().iterator().next());
        assertIdentityEntries(actual.keySet().iterator().next());
    }

    @ParameterizedTest
    @MethodSource("backends")
    void rootCollectionsKeepNestedClassesAndFullGenericSchema(final boolean abacus, final XmlParserType backend) {
        final XmlParser parser = parser(abacus, backend);
        final String xml = "<list type=\"LinkedList&lt;IdentityHashMap&lt;String,String&gt;&gt;\">[{\"a\":\"1\",\"a\":\"2\"}]</list>";
        final List<?> actual = parser.deserialize(xml, Type.of("List<Map<String,Integer>>"));
        assertInstanceOf(LinkedList.class, actual);
        assertIdentityEntries(actual.getFirst());
    }

    @ParameterizedTest
    @MethodSource("backends")
    void nestedImplementationSupportsStructuredValuesToo(final boolean abacus, final XmlParserType backend) {
        final XmlParser parser = parser(abacus, backend);
        final String entries = abacus ? "<entry><key>a</key><value>1</value></entry><entry><key>b</key><value>2</value></entry>" : "<a>1</a><b>2</b>";
        final String xml = map(abacus, "IdentityHashMap<String,String>", "<map>" + entries + "</map>");
        final Map<?, ?> actual = parser.deserialize(xml, Type.of("Map<String,Map<String,Integer>>"));
        final Map<?, ?> held = assertInstanceOf(IdentityHashMap.class, actual.get("k"));
        assertEquals(2, held.size());
        assertEquals(List.of(1, 2), held.values().stream().map(v -> assertInstanceOf(Integer.class, v)).sorted().toList());
    }

    @ParameterizedTest
    @MethodSource("backends")
    void incompatibleAndUninstantiableNestedClassesKeepDeclaredFallback(final boolean abacus, final XmlParserType backend) {
        final XmlParser parser = parser(abacus, backend);
        final Map<?, ?> incompatible = parser.deserialize(map(abacus, "TreeSet<Integer>", "[3,1,3]"), Type.of("Map<String,List<Integer>>"));
        assertEquals(List.of(3, 1, 3), incompatible.get("k"));
        final String privateList = List.of(1, 2).getClass().getName() + "<String>";
        final Map<?, ?> fallback = parser.deserialize(map(abacus, privateList, "[1,2]"), Type.of("Map<String,List<Integer>>"));
        assertEquals(List.of(1, 2), fallback.get("k"));
        assertInstanceOf(ArrayList.class, fallback.get("k"));
        assertThrows(ParsingException.class,
                () -> parser.deserialize(map(abacus, "not.approved.Type<String,Integer>", DUPLICATE_KEYS), Type.of("Map<String,Map<String,Integer>>")));
    }

    @ParameterizedTest
    @MethodSource("backends")
    void subtypeAndMultiValueMapSchemasAreNotReinterpretedAsJavaVariables(final boolean abacus, final XmlParserType backend) {
        final XmlParser parser = parser(abacus, backend);
        for (final Class<?> implementation : List.of(XmlNestedSchemaRegressionTest.StringKeyMap.class, XmlNestedSchemaRegressionTest.ReversedMap.class)) {
            final String xml = map(abacus, implementation.getCanonicalName() + "<Integer,String>", "{\"a\":\"1\"}");
            final Map<?, ?> actual = parser.deserialize(xml, Type.of("Map<String,Map<String,Integer>>"));
            assertEquals(Map.of("a", 1), actual.get("k"));
            assertInstanceOf(implementation, actual.get("k"));
        }
        final String xml = map(abacus, "org.springframework.util.LinkedMultiValueMap<String,String>", "{\"a\":[\"1\",\"2\"]}");
        final Map<?, ?> actual = parser.deserialize(xml, Type.of("Map<String,Map<String,List<Integer>>>"));
        assertEquals(Map.of("a", List.of(1, 2)), actual.get("k"));
        assertInstanceOf(org.springframework.util.LinkedMultiValueMap.class, actual.get("k"));
    }

    @ParameterizedTest
    @MethodSource("backends")
    void specializedMapCannotReplaceAnIncompatibleDeclaredValueShape(final boolean abacus, final XmlParserType backend) {
        final XmlParser parser = parser(abacus, backend);
        final String metadata = "org.springframework.util.LinkedMultiValueMap<String,String>";
        final Map<?, ?> scalar = parser.deserialize(map(abacus, metadata, "{\"a\":\"1\"}"), Type.of("Map<String,Map<String,Integer>>"));
        assertEquals(Map.of("a", 1), scalar.get("k"));
        final Map<?, ?> concrete = parser.deserialize(map(abacus, metadata, "{\"a\":[\"1\",\"2\"]}"), Type.of("Map<String,Map<String,LinkedList<Integer>>>"));
        final Map<?, ?> held = assertInstanceOf(Map.class, concrete.get("k"));
        assertEquals(List.of(1, 2), assertInstanceOf(LinkedList.class, held.get("a")));
    }

    @ParameterizedTest
    @MethodSource("backends")
    void matchingSpecializedMetadataStillKeepsDeclaredKeyConstraints(final boolean abacus, final XmlParserType backend) {
        final XmlParser parser = parser(abacus, backend);
        for (final String keyType : List.of("String", "Integer")) {
            final String xml = map(abacus, "org.springframework.util.LinkedMultiValueMap<" + keyType + ",Integer>", "{\"a\":[\"1\",\"2\"]}");
            final Map<?, ?> actual = parser.deserialize(xml, Type.of("Map<String,Map<String,List<Integer>>>"));
            assertEquals(Map.of("a", List.of(1, 2)), actual.get("k"));
            assertInstanceOf(org.springframework.util.LinkedMultiValueMap.class, actual.get("k"));
        }
    }

    @ParameterizedTest
    @MethodSource("backends")
    void collectionNamesKeepNestedElementConstraintsAndImplementations(final boolean abacus, final XmlParserType backend) {
        final XmlParser parser = parser(abacus, backend);
        final String xml = map(abacus, "LinkedList<TreeSet<String>>", "[[3,1,3],[2,2]]");
        final Map<?, ?> actual = parser.deserialize(xml, Type.of("Map<String,List<Collection<Integer>>>"));
        final List<?> held = assertInstanceOf(LinkedList.class, actual.get("k"));
        assertEquals(2, held.size());
        assertEquals(List.of(1, 3), new ArrayList<>(assertInstanceOf(TreeSet.class, held.getFirst())));
        assertEquals(List.of(2), new ArrayList<>(assertInstanceOf(TreeSet.class, held.getLast())));
    }
}
