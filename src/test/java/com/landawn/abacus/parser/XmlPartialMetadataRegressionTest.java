package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.ParsingException;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.MapEntity;

@Tag("unit")
public class XmlPartialMetadataRegressionTest extends TestBase {
    static Stream<Arguments> backends() {
        return XmlWrappedObjectRegressionTest.backends();
    }

    static Stream<Arguments> wrappers() {
        return Stream.of(false, true)
                .flatMap(abacus -> Stream.of(XmlParserType.values())
                        .filter(backend -> abacus || backend != XmlParserType.SAX)
                        .flatMap(backend -> Stream.of(XmlWrappedObjectRegressionTest.Wrapper.values()).map(wrapper -> Arguments.of(abacus, backend, wrapper))));
    }

    static Stream<Arguments> xmlWrappers() {
        return XmlWrappedObjectRegressionTest.xmlWrappedCases();
    }

    private static XmlParser parser(final boolean abacus, final XmlParserType backend) {
        final Set<Class<?>> approved = Set.of(XmlNestedSchemaRegressionTest.StringKeyMap.class, XmlNestedSchemaRegressionTest.ReversedMap.class,
                PartialBean.class, org.springframework.util.LinkedMultiValueMap.class);
        return abacus ? new AbacusXmlParserImpl(backend, null, null, approved) : new XmlParserImpl(backend, null, null, approved);
    }

    private static String entry(final boolean abacus, final String key, final String metadata, final String body) {
        return abacus ? "<entry><key>" + key + "</key><value" + metadata + ">" + body + "</value></entry>"
                : "<e name=\"" + key + "\"" + metadata + ">" + body + "</e>";
    }

    // A String key is a constraint on that slot only; it must not prevent refinement of the Object value.
    @ParameterizedTest
    @MethodSource("backends")
    void rootMetadataRefinesOnlyUnconstrainedArguments(final boolean abacus, final XmlParserType backend) {
        final XmlParser parser = parser(abacus, backend);
        final String xml = "<map type=\"LinkedHashMap&lt;String,Integer&gt;\">" + entry(abacus, "k", "", "1") + "</map>";
        for (final String root : List.of("Map", "LinkedHashMap")) {
            final Map<?, ?> actual = parser.deserialize(xml, Type.of(root + "<String,Object>"));
            assertEquals(Map.of("k", 1), actual, xml);
            assertInstanceOf(LinkedHashMap.class, actual);
            assertEquals(Map.of("k", 1L), parser.deserialize(xml, Type.of(root + "<String,Long>")), xml);
        }
        final String keyXml = "<map type=\"LinkedHashMap&lt;Long,Integer&gt;\">" + entry(abacus, "5", "", "1") + "</map>";
        assertEquals(Map.of(5L, "1"), parser.deserialize(keyXml, Type.of("Map<Object,String>")), keyXml);
    }

    @ParameterizedTest
    @MethodSource("backends")
    void primitiveMetadataArgumentsRefineToBoxedObjectValues(final boolean abacus, final XmlParserType backend) {
        final XmlParser parser = parser(abacus, backend);
        final String map = "<map type=\"LinkedHashMap&lt;String,int&gt;\">" + entry(abacus, "k", "", "1") + "</map>";
        assertEquals(Map.of("k", 1), parser.deserialize(map, Type.of("Map<String,Object>")), map);
        final String list = "<list type=\"LinkedList&lt;long&gt;\"><e>1</e></list>";
        assertEquals(List.of(1L), parser.deserialize(list, Type.of("List<Object>")), list);
    }

    @ParameterizedTest
    @MethodSource("backends")
    void nestedMapRefinesObjectValuesInTextAndElementForms(final boolean abacus, final XmlParserType backend) {
        final XmlParser parser = parser(abacus, backend);
        for (final String body : List.of("{\"a\":\"1\"}", "<map>" + entry(abacus, "a", "", "1") + "</map>")) {
            final String xml = "<map type=\"LinkedHashMap\">" + entry(abacus, "k", " type=\"TreeMap&lt;Integer,Integer&gt;\"", body) + "</map>";
            final Map<?, ?> actual = parser.deserialize(xml, Type.of("Map<String,Map<String,Object>>"));
            assertEquals(Map.of("a", 1), actual.get("k"), xml);
            assertInstanceOf(TreeMap.class, actual.get("k"));
        }
    }

    @ParameterizedTest
    @MethodSource("backends")
    void refinementReachesNestedContainersAndArrays(final boolean abacus, final XmlParserType backend) {
        final XmlParser parser = parser(abacus, backend);
        for (final String valueType : List.of("List<Object>", "Object[]")) {
            final String metadataValue = valueType.equals("Object[]") ? "Long[]" : "List&lt;Long&gt;";
            final String xml = "<map type=\"LinkedHashMap&lt;Integer," + metadataValue + "&gt;\">" + entry(abacus, "k", "", "[1,2]") + "</map>";
            final Map<?, ?> actual = parser.deserialize(xml, Type.of("Map<String," + valueType + ">"));
            if (valueType.equals("Object[]")) {
                assertArrayEquals(new Long[] { 1L, 2L }, (Object[]) actual.get("k"), xml);
            } else {
                assertEquals(List.of(1L, 2L), actual.get("k"), xml);
            }
        }
        final String list = "<list type=\"LinkedList&lt;Integer&gt;\"><e>1</e><e>2</e></list>";
        final List<?> actual = parser.deserialize(list, Type.of("List<Object>"));
        assertEquals(List.of(1, 2), actual, list);
        assertInstanceOf(LinkedList.class, actual);
    }

    // MapType exposes K/V schema slots even for Java subclasses with fewer or reversed variables.
    @ParameterizedTest
    @MethodSource("backends")
    void customImplementationsKeepContainerSlotMeaning(final boolean abacus, final XmlParserType backend) {
        final XmlParser parser = parser(abacus, backend);
        for (final Class<?> implementation : List.of(XmlNestedSchemaRegressionTest.StringKeyMap.class, XmlNestedSchemaRegressionTest.ReversedMap.class)) {
            final String xml = "<map type=\"" + implementation.getCanonicalName() + "&lt;Integer,Long&gt;\">" + entry(abacus, "k", "", "1") + "</map>";
            for (final String declaration : List.of("Map", implementation.getCanonicalName())) {
                final Map<?, ?> actual = parser.deserialize(xml, Type.of(declaration + "<String,Object>"));
                assertEquals(Map.of("k", 1L), actual, xml);
                assertInstanceOf(implementation, actual);
            }
        }
    }

    @ParameterizedTest
    @MethodSource("wrappers")
    void partialSchemaSurvivesInsideWrappers(final boolean abacus, final XmlParserType backend, final XmlWrappedObjectRegressionTest.Wrapper wrapper) {
        final XmlParser parser = parser(abacus, backend);
        for (final String body : List.of("{\"a\":\"1\"}", "<map>" + entry(abacus, "a", "", "1") + "</map>")) {
            final String xml = "<map type=\"LinkedHashMap\">" + entry(abacus, "k", " type=\"TreeMap&lt;Integer,Integer&gt;\"", body) + "</map>";
            final Map<?, ?> actual = parser.deserialize(xml, Type.of("Map<String," + wrapper.typeName + "<Map<String,Object>>>"));
            assertEquals(wrapper.wrap(Map.of("a", 1)), actual.get("k"), xml);
            assertInstanceOf(TreeMap.class, wrapper.unwrap(actual.get("k")));
        }
    }

    @ParameterizedTest
    @MethodSource("wrappers")
    void refiningAPayloadDoesNotEraseItsDeclaredWrapper(final boolean abacus, final XmlParserType backend,
            final XmlWrappedObjectRegressionTest.Wrapper wrapper) {
        final XmlParser parser = parser(abacus, backend);
        final String xml = "<map type=\"LinkedHashMap&lt;String,Integer&gt;\">" + entry(abacus, "k", "", "1") + "</map>";
        assertEquals(Map.of("k", wrapper.wrap(1)), parser.deserialize(xml, Type.of("Map<String," + wrapper.typeName + "<Object>>")), xml);
        assertEquals(Map.of("k", wrapper.wrap(1L)), parser.deserialize(xml, Type.of("Map<String," + wrapper.typeName + "<Long>>")), xml);
    }

    @ParameterizedTest
    @MethodSource("backends")
    void rawIncompatibleAndUnapprovedMetadataCannotReplaceConstraints(final boolean abacus, final XmlParserType backend) {
        final XmlParser parser = parser(abacus, backend);
        for (final String metadata : List.of("LinkedHashMap", "LinkedHashMap&lt;Object,Object&gt;", "LinkedList&lt;Integer&gt;")) {
            final String xml = "<map type=\"" + metadata + "\">" + entry(abacus, "k", "", "1") + "</map>";
            assertEquals(Map.of("k", "1"), parser.deserialize(xml, Type.of("Map<String,Object>")), xml);
        }
        final String invalid = "<map type=\"LinkedHashMap&lt;String,not.approved.Type&gt;\">" + entry(abacus, "k", "", "1") + "</map>";
        assertThrows(ParsingException.class, () -> parser.deserialize(invalid, Type.of("Map<String,Object>")));
    }

    @ParameterizedTest
    @MethodSource("backends")
    void beanPropertiesRefineObjectSlotsAndKeepTheirImplementation(final boolean abacus, final XmlParserType backend) {
        final XmlParser parser = parser(abacus, backend);
        for (final String body : List.of("{\"a\":\"1\"}", "<map>" + entry(abacus, "a", "", "1") + "</map>")) {
            final String xml = "<bean type=\"" + PartialBean.class.getCanonicalName() + "\"><values type=\"TreeMap&lt;String,Integer&gt;\">" + body
                    + "</values><tail>done</tail></bean>";
            final PartialBean actual = parser.deserialize(xml, PartialBean.class);
            assertEquals(Map.of("a", 1), actual.getValues(), xml);
            assertInstanceOf(TreeMap.class, actual.getValues());
            assertEquals("done", actual.getTail());
            final XmlDeserConfig config = XmlDeserConfig.create().setValueType("values", Type.of("Map<String,Object>"));
            assertEquals(actual.getValues(), parser.<PartialBean> deserialize(xml, config, PartialBean.class).getValues());
        }
    }

    @ParameterizedTest
    @MethodSource("backends")
    void configuredValueTypeStillTakesPrecedence(final boolean abacus, final XmlParserType backend) {
        final XmlParser parser = parser(abacus, backend);
        final String xml = "<map type=\"LinkedHashMap&lt;String,Integer&gt;\">" + entry(abacus, "k", "", "1") + "</map>";
        final XmlDeserConfig config = XmlDeserConfig.create().setMapValueType(Long.class);
        assertEquals(Map.of("k", 1L), parser.deserialize(xml, config, Type.of("Map<String,Object>")), xml);
    }

    @ParameterizedTest
    @MethodSource("backends")
    void configuredPartialContainerSchemaCanRefineObjectSlots(final boolean abacus, final XmlParserType backend) {
        final XmlParser parser = parser(abacus, backend);
        for (final String body : List.of("{\"a\":\"1\"}", "<map>" + entry(abacus, "a", "", "1") + "</map>")) {
            final String xml = "<map type=\"LinkedHashMap\">" + entry(abacus, "k", " type=\"TreeMap&lt;Integer,Integer&gt;\"", body) + "</map>";
            for (final String valueType : List.of("Object", "Long")) {
                final XmlDeserConfig config = XmlDeserConfig.create().setValueType("k", Type.of("Map<String," + valueType + ">"));
                final Map<?, ?> actual = parser.deserialize(xml, config, Type.of("Map<String,Object>"));
                assertEquals(Map.of("a", valueType.equals("Long") ? (Object) 1L : 1), actual.get("k"), xml);
                assertInstanceOf(TreeMap.class, actual.get("k"));
                if (!abacus) {
                    final String entityXml = "<values><k type=\"TreeMap&lt;Integer,Integer&gt;\">" + body + "</k><tail>end</tail></values>";
                    final MapEntity entity = parser.deserialize(entityXml, config, MapEntity.class);
                    assertEquals(actual.get("k"), entity.get("k"), entityXml);
                    assertInstanceOf(TreeMap.class, entity.get("k"));
                    assertEquals("end", entity.get("tail"));
                }
            }
        }
    }

    @ParameterizedTest
    @MethodSource("backends")
    void multiValueMapNamesRetainTheirElementArgument(final boolean abacus, final XmlParserType backend) {
        final XmlParser parser = parser(abacus, backend);
        final String xml = "<map type=\"org.springframework.util.LinkedMultiValueMap&lt;String,Integer&gt;\">" + entry(abacus, "k", "", "[\"1\",\"2\"]")
                + "</map>";
        for (final String target : List.of("Map<String,List<Object>>", "org.springframework.util.MultiValueMap<String,Object>",
                "org.springframework.util.LinkedMultiValueMap<String,Object>")) {
            final Map<?, ?> actual = parser.deserialize(xml, Type.of(target));
            assertEquals(Map.of("k", List.of(1, 2)), actual, xml);
            assertInstanceOf(org.springframework.util.LinkedMultiValueMap.class, actual);
        }
    }

    // Keep the established MapEntity distinction: a single Object child of another name uses its enclosing entity context, on
    // both readers (2026-10-05: the DOM reader used to keep a one-element list of a map); collection/array elements use the
    // same ordinary map fallback with or without an Optional/Nullable.
    @ParameterizedTest
    @MethodSource("xmlWrappers")
    void mapEntityWrappersKeepThePlainCollectionElementFallback(final XmlParserType backend, final XmlWrappedObjectRegressionTest.Wrapper wrapper) {
        final XmlParser parser = parser(false, backend);
        final String xml = "<values><k><item><x>1</x></item></k><tail>end</tail></values>";
        for (final String payload : List.of("List<Object>", "Object[]")) {
            final MapEntity plain = parser.deserialize(xml, XmlDeserConfig.create().setValueType("k", Type.of(payload)), MapEntity.class);
            final MapEntity wrapped = parser.deserialize(xml, XmlDeserConfig.create().setValueType("k", Type.of(wrapper.typeName + "<" + payload + ">")),
                    MapEntity.class);
            final Object value = wrapper.unwrap(wrapped.get("k"));
            final Object first = value instanceof List<?> list ? list.get(0) : ((Object[]) value)[0];
            assertInstanceOf(HashMap.class, first);
            assertEquals(Map.of("x", "1"), first);
            if (value instanceof Object[] array) {
                assertArrayEquals((Object[]) plain.get("k"), array);
            } else {
                assertEquals(plain.get("k"), value);
            }
            assertEquals("end", wrapped.get("tail"));
        }
        final MapEntity single = parser.deserialize(xml, XmlDeserConfig.create().setValueType("k", Type.of(wrapper.typeName + "<Object>")), MapEntity.class);
        final Object value = wrapper.unwrap(single.get("k"));
        assertEquals("1", assertInstanceOf(MapEntity.class, value, backend.toString()).get("x"));
    }

    public static class PartialBean {
        private Map<String, Object> values;
        private String tail;

        public Map<String, Object> getValues() {
            return values;
        }

        public void setValues(final Map<String, Object> values) {
            this.values = values;
        }

        public String getTail() {
            return tail;
        }

        public void setTail(final String tail) {
            this.tail = tail;
        }
    }
}
