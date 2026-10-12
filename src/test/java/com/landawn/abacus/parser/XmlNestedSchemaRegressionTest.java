package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.type.Type;

@Tag("unit")
public class XmlNestedSchemaRegressionTest extends TestBase {
    static Stream<Arguments> backends() {
        return XmlWrappedObjectRegressionTest.backends();
    }

    static Stream<XmlParserType> xmlBackends() {
        return XmlWrappedObjectRegressionTest.xmlBackends();
    }

    static Stream<Arguments> abacusWrapperCases() {
        return Stream.of(XmlParserType.values())
                .flatMap(backend -> Stream.of(XmlWrappedObjectRegressionTest.Wrapper.values())
                        .flatMap(wrapper -> Stream.of(false, true).map(typedRoot -> Arguments.of(backend, wrapper, typedRoot))));
    }

    private static XmlParser parser(final boolean abacus, final XmlParserType backend) {
        return abacus ? new AbacusXmlParserImpl(backend, null, null, Set.of(StringKeyMap.class, ReversedMap.class, Bean.class, BeanValue.class))
                : new XmlParserImpl(backend, null, null, Set.of(StringKeyMap.class, ReversedMap.class, Bean.class, BeanValue.class));
    }

    // A concrete attribute supplies construction, never a replacement for the caller's Integer schema.
    @ParameterizedTest
    @MethodSource("backends")
    void parameterizedRootMetadataKeepsDeclaredSchema(final boolean abacus, final XmlParserType backend) {
        final XmlParser parser = parser(abacus, backend);
        for (final String body : List.of("[1,2]", "<e>1</e><e>2</e>")) {
            final String xml = "<list type=\"LinkedList&lt;String&gt;\">" + body + "</list>";
            final List<?> actual = parser.deserialize(xml, Type.of("List<Integer>"));
            assertEquals(List.of(1, 2), actual, xml);
            assertInstanceOf(LinkedList.class, actual, xml);
        }
        final String body = abacus ? "<entry><key>k</key><value>[1,2]</value></entry>" : "<k>[1,2]</k>";
        final String xml = "<map type=\"TreeMap&lt;String,String&gt;\">" + body + "</map>";
        final Map<?, ?> actual = parser.deserialize(xml, Type.of("Map<String,List<Integer>>"));
        assertEquals(Map.of("k", List.of(1, 2)), actual, xml);
        assertInstanceOf(TreeMap.class, actual, xml);
    }

    @ParameterizedTest
    @MethodSource("xmlBackends")
    void xmlPropertyContainersKeepImplementationAndSchema(final XmlParserType backend) {
        final XmlParser parser = parser(false, backend);
        for (final String body : List.of("[1,2]", "<e>1</e><e>2</e>")) {
            final String xml = "<map type=\"LinkedHashMap\"><k type=\"LinkedList&lt;String&gt;\">" + body + "</k></map>";
            final Map<?, ?> actual = parser.deserialize(xml, Type.of("Map<String,List<Integer>>"));
            assertEquals(List.of(1, 2), actual.get("k"), xml);
            assertInstanceOf(LinkedList.class, actual.get("k"), xml);

            final Bean bean = parser.deserialize(xml.replace("<map type=\"LinkedHashMap\">", "<bean type=\"" + Bean.class.getCanonicalName() + "\">")
                    .replace("</map>", "</bean>")
                    .replace("<k ", "<values ")
                    .replace("</k>", "</values>"), Bean.class);
            assertEquals(List.of(1, 2), bean.getValues());
            assertInstanceOf(LinkedList.class, bean.getValues());
        }
    }

    // These subclasses have a different parameter count or order from Map<K,V>; transplanting argument text is invalid.
    @ParameterizedTest
    @MethodSource("backends")
    void nestedMapUsesDeclaredSchemaWithCustomImplementation(final boolean abacus, final XmlParserType backend) {
        final XmlParser parser = parser(abacus, backend);
        for (final Class<?> implementation : List.of(StringKeyMap.class, ReversedMap.class)) {
            final String contents = abacus ? "<entry><key>a</key><value>[1,2]</value></entry>" : "<a>[1,2]</a>";
            final String payload = "<map>" + contents + "</map>";
            final String value = abacus ? "<entry><key>k</key><value type=\"%s\">%s</value></entry>" : "<k type=\"%s\">%s</k>";
            final String xml = "<map type=\"LinkedHashMap\">" + value.formatted(implementation.getCanonicalName(), payload) + "</map>";
            final Map<?, ?> actual = parser.deserialize(xml, Type.of("Map<String,Map<String,List<Integer>>>"));
            assertEquals(Map.of("a", List.of(1, 2)), actual.get("k"), xml);
            assertInstanceOf(implementation, actual.get("k"), xml);
        }
    }

    // The wrapper and its single child can each describe the payload; Optional is still supplied by the declaration.
    @ParameterizedTest
    @MethodSource("abacusWrapperCases")
    void abacusAcceptsPayloadMetadataOnWrapperOrChild(final XmlParserType backend, final XmlWrappedObjectRegressionTest.Wrapper wrapper,
            final boolean typedRoot) {
        final XmlParser parser = parser(true, backend);
        final String attr = " type=\"TreeMap&lt;String,Integer&gt;\"";
        for (int placement = 0; placement < 3; placement++) {
            final String xml = "<map" + (typedRoot ? " type=\"LinkedHashMap\"" : "") + "><entry><key>k</key><value" + (placement != 1 ? attr : "") + "><map"
                    + (placement != 0 ? attr : "") + "><entry><key>a</key><value>1</value></entry></map></value></entry></map>";
            final Map<?, ?> actual = parser.deserialize(xml, Type.of("LinkedHashMap<String," + wrapper.typeName + "<Object>>"));
            final Object held = wrapper.unwrap(actual.get("k"));
            assertEquals(Map.of("a", 1), held, xml);
            assertInstanceOf(TreeMap.class, held, xml);
        }
    }

    // A constructible map can contain private immutable implementations several levels below the Object slot.
    @ParameterizedTest
    @MethodSource("abacusWrapperCases")
    void nestedImmutableContainersRetainFallback(final XmlParserType backend, final XmlWrappedObjectRegressionTest.Wrapper wrapper, final boolean typedRoot) {
        final XmlParser parser = parser(true, backend);
        final List<Object> values = List.of(List.of(1, 2), Map.of("a", 1), Collections.singletonList(1),
                Collections.unmodifiableList(new LinkedList<>(List.of(1, 2))), Map.of("a", List.of(1, 2)));
        for (final Object value : values) {
            final Map<String, Object> inner = new LinkedHashMap<>();
            inner.put("a", value);
            final Map<String, Object> source = new LinkedHashMap<>();
            source.put("k", wrapper.wrap(inner));
            source.put("tail", wrapper.wrap("end"));
            final String xml = parser.serialize(source, XmlSerConfig.create().setWriteTypeInfo(true));
            final Map<?, ?> actual = parser.deserialize(xml, Type.of((typedRoot ? "LinkedHashMap" : "Map") + "<String," + wrapper.typeName + "<Object>>"));
            assertEquals(inner, wrapper.unwrap(actual.get("k")), xml);
            assertEquals(wrapper.wrap("end"), actual.get("tail"), xml);
        }
    }

    @ParameterizedTest
    @MethodSource("backends")
    void nestedPlainObjectRetainsImmutableFallback(final boolean abacus, final XmlParserType backend) {
        if (!abacus) {
            return; // XmlParser's untyped Object shapes are a separate compatibility policy.
        }
        final XmlParser parser = parser(true, backend);
        final Map<String, Object> inner = new LinkedHashMap<>();
        inner.put("a", List.of(1, 2));
        final Map<String, Object> source = new LinkedHashMap<>();
        source.put("k", inner);
        final String xml = parser.serialize(source, XmlSerConfig.create().setWriteTypeInfo(true));
        assertEquals(source, parser.deserialize(xml, Type.of("LinkedHashMap<String,Object>")), xml);
    }

    static Stream<XmlParserType> abacusBackends() {
        return Stream.of(XmlParserType.values());
    }

    @ParameterizedTest
    @MethodSource("abacusBackends")
    void abacusKeysAndItemsKeepSchemaAndImplementation(final XmlParserType backend) {
        final XmlParser parser = parser(true, backend);
        for (final Class<?> implementation : List.of(StringKeyMap.class, ReversedMap.class)) {
            final String metadata = " type=\"" + implementation.getCanonicalName() + "\"";
            final String payload = "<map><entry><key>a</key><value>[1,2]</value></entry></map>";
            final Map<?, ?> keyed = parser.deserialize("<map><entry><key" + metadata + ">" + payload + "</key><value>end</value></entry></map>",
                    Type.of("Map<Map<String,List<Integer>>,String>"));
            final Object key = keyed.keySet().iterator().next();
            assertEquals(Map.of("a", List.of(1, 2)), key);
            assertInstanceOf(implementation, key);
            for (final String outer : List.of("list", "array")) {
                final Object result = parser.deserialize("<" + outer + "><e" + metadata + ">" + payload + "</e></" + outer + ">",
                        Type.of(outer.equals("list") ? "List<Map<String,List<Integer>>>" : "Map<String,List<Integer>>[]"));
                final Object item = result instanceof List<?> list ? list.get(0) : ((Object[]) result)[0];
                assertEquals(Map.of("a", List.of(1, 2)), item);
                assertInstanceOf(implementation, item);
            }
        }
    }

    @ParameterizedTest
    @MethodSource("abacusBackends")
    void abacusTextContainerSlotsKeepRuntimeClass(final XmlParserType backend) {
        final XmlParser parser = parser(true, backend);
        final String metadata = " type=\"LinkedList&lt;String&gt;\"";
        final Map<?, ?> keyed = parser.deserialize("<map><entry><key" + metadata + ">[1,2]</key><value" + metadata + ">[3,4]</value></entry></map>",
                Type.of("Map<List<Integer>,List<Integer>>"));
        final Object key = keyed.keySet().iterator().next();
        assertEquals(List.of(1, 2), key);
        assertInstanceOf(LinkedList.class, key);
        assertEquals(List.of(3, 4), keyed.get(key));
        assertInstanceOf(LinkedList.class, keyed.get(key));
        final List<?> items = parser.deserialize("<list><e" + metadata + ">[1,2]</e></list>", Type.of("List<List<Integer>>"));
        assertEquals(List.of(List.of(1, 2)), items);
        assertInstanceOf(LinkedList.class, items.get(0));
    }

    @ParameterizedTest
    @MethodSource("abacusBackends")
    void abacusBeanPropertyRetainsImplementationAndSchema(final XmlParserType backend) {
        final XmlParser parser = parser(true, backend);
        for (final String contents : List.of("[1,2]", "<list><e>1</e><e>2</e></list>")) {
            final String xml = "<bean type=\"" + Bean.class.getCanonicalName() + "\"><values type=\"LinkedList&lt;String&gt;\">" + contents
                    + "</values></bean>";
            final Bean actual = parser.deserialize(xml, Bean.class);
            assertEquals(List.of(1, 2), actual.getValues(), xml);
            assertInstanceOf(LinkedList.class, actual.getValues(), xml);
        }
        final String xml = "<bean type=\"" + Bean.class.getCanonicalName() + "\"><wrapped type=\"LinkedList&lt;String&gt;\">"
                + "<list><e>1</e><e>2</e></list></wrapped></bean>";
        final Bean actual = parser.deserialize(xml, Bean.class);
        assertEquals(List.of(1, 2), actual.getWrapped().orElseThrow(), xml);
        assertInstanceOf(LinkedList.class, actual.getWrapped().orElseThrow(), xml);
    }

    @ParameterizedTest
    @MethodSource("abacusWrapperCases")
    void abacusParameterizedWrapperRetainsSchemaAtEitherMetadataLocation(final XmlParserType backend, final XmlWrappedObjectRegressionTest.Wrapper wrapper,
            final boolean typedRoot) {
        final XmlParser parser = parser(true, backend);
        for (int placement = 0; placement < 3; placement++) {
            final String metadata = " type=\"LinkedList&lt;String&gt;\"";
            final String xml = "<map" + (typedRoot ? " type=\"LinkedHashMap\"" : "") + "><entry><key>k</key><value" + (placement != 1 ? metadata : "")
                    + "><list" + (placement != 0 ? metadata : "") + "><e>1</e><e>2</e></list></value></entry></map>";
            final Map<?, ?> actual = parser.deserialize(xml, Type.of("Map<String," + wrapper.typeName + "<List<Integer>>>"));
            final Object held = wrapper.unwrap(actual.get("k"));
            assertEquals(List.of(1, 2), held, xml);
            assertInstanceOf(LinkedList.class, held, xml);
        }
    }

    @Test
    void abacusStaxPrimitiveMetadataStillBoxesObjectMapSlots() {
        final XmlParser parser = parser(true, XmlParserType.StAX);
        final String xml = "<map><entry><key type=\"int\">1</key><value type=\"long\">2</value></entry></map>";
        assertEquals(Map.of(1, 2L), parser.deserialize(xml, Type.of("Map<Object,Object>")));
        assertEquals(Map.of(1, 2L), parser.deserialize(xml, Type.of("Map<Integer,Long>")));
    }

    // An interface also uses an Object-like type handler, but a TreeMap cannot refine that declared bean slot.
    @ParameterizedTest
    @MethodSource("abacusWrapperCases")
    void abacusWrapperMetadataMustFitDeclaredInterface(final XmlParserType backend, final XmlWrappedObjectRegressionTest.Wrapper wrapper,
            final boolean typedRoot) {
        final XmlParser parser = parser(true, backend);
        final String xml = "<map" + (typedRoot ? " type=\"LinkedHashMap\"" : "") + "><entry><key>k</key><value type=\"TreeMap\">" + "<bean type=\""
                + Bean.class.getCanonicalName() + "\"><values>[1,2]</values></bean></value></entry></map>";
        final Map<?, ?> result = parser.deserialize(xml, Type.of("Map<String," + wrapper.typeName + "<" + BeanValue.class.getCanonicalName() + ">>"));
        final Bean value = assertInstanceOf(Bean.class, wrapper.unwrap(result.get("k")), xml);
        assertEquals(List.of(1, 2), value.getValues(), xml);
    }

    public static class StringKeyMap<V> extends LinkedHashMap<String, V> {
        private static final long serialVersionUID = 1L;
    }

    public static class ReversedMap<V, K> extends LinkedHashMap<K, V> {
        private static final long serialVersionUID = 1L;
    }

    public interface BeanValue {
    }

    public static class Bean implements BeanValue {
        private List<Integer> values;
        private java.util.Optional<List<Integer>> wrapped;

        public List<Integer> getValues() {
            return values;
        }

        public void setValues(final List<Integer> values) {
            this.values = values;
        }

        public java.util.Optional<List<Integer>> getWrapped() {
            return wrapped;
        }

        public void setWrapped(final java.util.Optional<List<Integer>> wrapped) {
            this.wrapped = wrapped;
        }
    }
}
