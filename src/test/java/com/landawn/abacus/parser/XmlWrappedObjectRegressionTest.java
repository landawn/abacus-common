package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.ParsingException;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.MapEntity;
import com.landawn.abacus.util.u;

@Tag("unit")
public class XmlWrappedObjectRegressionTest extends TestBase {
    enum Wrapper {
        JDK("java.util.Optional"), OPTIONAL("com.landawn.abacus.util.u.Optional"), NULLABLE("com.landawn.abacus.util.u.Nullable");

        final String typeName;

        Wrapper(final String typeName) {
            this.typeName = typeName;
        }

        Object wrap(final Object value) {
            return switch (this) {
                case JDK -> java.util.Optional.ofNullable(value);
                case OPTIONAL -> u.Optional.ofNullable(value);
                case NULLABLE -> u.Nullable.of(value);
            };
        }

        Object unwrap(final Object value) {
            return switch (this) {
                case JDK -> ((java.util.Optional<?>) value).get();
                case OPTIONAL -> ((u.Optional<?>) value).get();
                case NULLABLE -> ((u.Nullable<?>) value).get();
            };
        }
    }

    static Stream<Arguments> wrappedCases() {
        final List<Arguments> cases = new ArrayList<>();
        for (final boolean abacus : new boolean[] { false, true }) {
            for (final XmlParserType backend : XmlParserType.values()) {
                if (!abacus && backend == XmlParserType.SAX) {
                    continue;
                }
                for (final Wrapper wrapper : Wrapper.values()) {
                    for (final boolean typeInfo : new boolean[] { false, true }) {
                        for (final String root : List.of("Map", "LinkedHashMap")) {
                            cases.add(Arguments.of(abacus, backend, wrapper, typeInfo, root));
                        }
                    }
                }
            }
        }
        return cases.stream();
    }

    static Stream<Arguments> backends() {
        return Stream.of(Arguments.of(false, XmlParserType.StAX), Arguments.of(false, XmlParserType.DOM), Arguments.of(true, XmlParserType.StAX),
                Arguments.of(true, XmlParserType.DOM), Arguments.of(true, XmlParserType.SAX));
    }

    static Stream<XmlParserType> xmlBackends() {
        return Stream.of(XmlParserType.StAX, XmlParserType.DOM);
    }

    static Stream<Arguments> xmlWrappedCases() {
        return xmlBackends().flatMap(backend -> Stream.of(Wrapper.values()).map(wrapper -> Arguments.of(backend, wrapper)));
    }

    private static XmlParser parser(final boolean abacus, final XmlParserType backend) {
        return abacus ? new AbacusXmlParserImpl(backend, null, null, Set.of(StringKeyMap.class, WrappedBean.class))
                : new XmlParserImpl(backend, null, null, Set.of(StringKeyMap.class, WrappedBean.class));
    }

    // Runtime metadata describes the held value, not its Optional/Nullable declaration. Without type information both standard
    // readers take a single <map> child as the nested map (2026-10-05: the DOM reader used to keep a one-element list), and a
    // wrapped multi-child value must never lose later siblings.
    @ParameterizedTest
    @MethodSource("wrappedCases")
    void wrappedObjectPreservesValueAndEverySibling(final boolean abacus, final XmlParserType backend, final Wrapper wrapper, final boolean typeInfo,
            final String root) {
        final XmlParser parser = parser(abacus, backend);
        final Map<String, String> first = new LinkedHashMap<>(Map.of("x", "1", "z", "3"));
        final Map<String, String> second = new LinkedHashMap<>(Map.of("y", "2"));
        final List<Object> values = List.of(first, new ArrayList<>(List.of(first)), new ArrayList<>(List.of(first, second)));
        final Type<Map<String, Object>> target = Type.of(root + "<String, " + wrapper.typeName + "<Object>>");

        for (int shape = 0; shape < values.size(); shape++) {
            for (final boolean pretty : new boolean[] { false, true }) {
                final Map<String, Object> source = new LinkedHashMap<>();
                source.put("k", wrapper.wrap(values.get(shape)));
                source.put("tail", wrapper.wrap(second));
                final String xml = parser.serialize(source, XmlSerConfig.create().setWriteTypeInfo(typeInfo).setPrettyFormat(pretty));
                final Map<String, Object> result = parser.deserialize(xml, target);
                Object expected = values.get(shape);
                Object expectedTail = second;
                if (!abacus && !typeInfo && shape == 1) {
                    // A one-element list of maps is written as a single <map> child, which reads back as the map.
                    expected = first;
                }
                assertEquals(wrapper.wrap(expected), result.get("k"), xml);
                assertEquals(wrapper.wrap(expectedTail), result.get("tail"), xml);
                if (typeInfo) {
                    assertInstanceOf(LinkedHashMap.class, result, xml);
                }
            }
        }
    }

    @ParameterizedTest
    @MethodSource("wrappedCases")
    void wrappedParameterizedValueKeepsNestedConstraints(final boolean abacus, final XmlParserType backend, final Wrapper wrapper, final boolean typeInfo,
            final String root) {
        final XmlParser parser = parser(abacus, backend);
        final Object value = new ArrayList<>(List.of(new LinkedHashMap<>(Map.of("x", 1)), new LinkedHashMap<>(Map.of("y", 2))));
        final Map<String, Object> source = new LinkedHashMap<>(Map.of("k", wrapper.wrap(value)));
        final Type<Map<String, Object>> target = Type.of(root + "<String, " + wrapper.typeName + "<List<Map<String, Integer>>>>");
        final String xml = parser.serialize(source, XmlSerConfig.create().setWriteTypeInfo(typeInfo));
        assertEquals(source, parser.deserialize(xml, target), xml);
    }

    @ParameterizedTest
    @MethodSource("wrappedCases")
    void scalarMetadataAppliesInsideTheWrapper(final boolean abacus, final XmlParserType backend, final Wrapper wrapper, final boolean typeInfo,
            final String root) {
        final XmlParser parser = parser(abacus, backend);
        final Map<String, Object> source = new LinkedHashMap<>();
        source.put("number", wrapper.wrap(42));
        source.put("text", wrapper.wrap("null"));
        final String xml = parser.serialize(source, XmlSerConfig.create().setWriteTypeInfo(typeInfo));
        final Type<Map<String, Object>> target = Type.of(root + "<String, " + wrapper.typeName + "<Object>>");
        final Map<String, Object> actual = parser.deserialize(xml, target);
        assertEquals(wrapper.wrap(typeInfo ? (Object) 42 : "42"), actual.get("number"), xml);
        assertEquals(wrapper.wrap("null"), actual.get("text"), xml);
    }

    @ParameterizedTest
    @MethodSource("wrappedCases")
    void wrappedArraysKeepParameterizedComponents(final boolean abacus, final XmlParserType backend, final Wrapper wrapper, final boolean typeInfo,
            final String root) {
        final XmlParser parser = parser(abacus, backend);
        final Map<?, ?>[] value = { new LinkedHashMap<>(Map.of("x", 1)), null, new LinkedHashMap<>(Map.of("y", 2)) };
        final String xml = parser.serialize(new LinkedHashMap<>(Map.of("k", wrapper.wrap(value))), XmlSerConfig.create().setWriteTypeInfo(typeInfo));
        final Type<Map<String, Object>> target = Type.of(root + "<String, " + wrapper.typeName + "<Map<String, Integer>[]>>");
        final Map<String, Object> actual = parser.deserialize(xml, target);
        assertArrayEquals(value, (Object[]) wrapper.unwrap(actual.get("k")), xml);
    }

    @ParameterizedTest
    @MethodSource("backends")
    void beanPropertiesRetainWrappersAndEmptySemantics(final boolean abacus, final XmlParserType backend) {
        final XmlParser parser = parser(abacus, backend);
        for (final boolean typeInfo : new boolean[] { false, true }) {
            for (final boolean tagByName : new boolean[] { false, true }) {
                final XmlSerConfig config = XmlSerConfig.create().setWriteTypeInfo(typeInfo).setTagByPropertyName(tagByName).setExclusion(Exclusion.NONE);
                final WrappedBean empty = new WrappedBean();
                final String emptyXml = parser.serialize(empty, config);
                assertEquals(empty, parser.deserialize(emptyXml, WrappedBean.class), emptyXml);

                final List<Map<String, String>> value = new ArrayList<>(List.of(new LinkedHashMap<>(Map.of("x", "1")), new LinkedHashMap<>(Map.of("y", "2"))));
                final WrappedBean bean = new WrappedBean();
                bean.setJdk(java.util.Optional.of(value));
                bean.setOptional(u.Optional.of(value));
                bean.setNullable(u.Nullable.of(value));
                final String xml = parser.serialize(bean, config);
                final WrappedBean actual = parser.deserialize(xml, WrappedBean.class);
                // An Object-valued bean property is written as JSON text by its declared wrapper codec.
                // Without payload metadata that text remains a String, as for a plain Object property.
                final String text = "[{\"x\": \"1\"}, {\"y\": \"2\"}]";
                bean.setJdk(java.util.Optional.of(text));
                bean.setOptional(u.Optional.of((Object) text));
                bean.setNullable(u.Nullable.of((Object) text));
                assertEquals(bean, actual, xml);
            }
        }
    }

    @ParameterizedTest
    @MethodSource("backends")
    void configuredWrapperAndDeclaredScalarKeepPrecedence(final boolean abacus, final XmlParserType backend) {
        final XmlParser parser = parser(abacus, backend);
        final String xml = parser.serialize(new LinkedHashMap<>(Map.of("k", 42)), XmlSerConfig.create().setWriteTypeInfo(true));
        final Type<?> valueType = Type.of("java.util.Optional<Long>");
        final XmlDeserConfig config = XmlDeserConfig.create().setMapValueType(valueType);
        final Map<?, ?> actual = parser.deserialize(xml, config, Type.of("Map<String, Object>"));
        assertEquals(java.util.Optional.of(42L), actual.get("k"), xml);
        final String invalid = xml.replace("type=\"Integer\"", "type=\"not.approved.Type\"");
        assertThrows(ParsingException.class, () -> parser.deserialize(invalid, config, Type.of("Map<String, Object>")));
    }

    @ParameterizedTest
    @MethodSource("backends")
    void mixedWrappedValuesKeepNullPositionsAndReaderAlignment(final boolean abacus, final XmlParserType backend) {
        final XmlParser parser = parser(abacus, backend);
        final List<Object> value = new ArrayList<>(Arrays.asList(null, "", "text", new LinkedHashMap<>(Map.of("x", "1")), null));
        final Map<String, Object> source = new LinkedHashMap<>();
        source.put("k", java.util.Optional.of(value));
        source.put("tail", java.util.Optional.of("end"));
        for (final boolean typeInfo : new boolean[] { false, true }) {
            final String xml = parser.serialize(source, XmlSerConfig.create().setWriteTypeInfo(typeInfo).setPrettyFormat(true));
            assertEquals(source, parser.deserialize(xml, Type.of("Map<String, java.util.Optional<Object>>")), xml);
        }
        if (!abacus) {
            final MapEntity entity = new MapEntity("values");
            entity.set("k", java.util.Optional.of(value));
            final XmlDeserConfig config = XmlDeserConfig.create().setValueType("k", Type.of("java.util.Optional<Object>"));
            final String xml = parser.serialize(entity, XmlSerConfig.create().setWriteTypeInfo(true));
            final MapEntity actual = parser.deserialize(xml, config, MapEntity.class);
            assertEquals(entity, actual, xml);

            entity.set("k", java.util.Optional.of(new LinkedHashMap<>(Map.of("x", "1"))));
            final String untyped = parser.serialize(entity);
            final MapEntity untypedResult = parser.deserialize(untyped, config, MapEntity.class);
            // The untyped <map> child is the map the writer named, on both readers (2026-10-05: StAX used to read it as a
            // MapEntity named "map", DOM as a one-element list).
            assertEquals(java.util.Optional.of(Map.of("x", "1")), untypedResult.get("k"), untyped);
        }
    }

    // A runtime implementation selects construction; the declared Map<K,V> continues to supply the parsing schema.
    // StringKeyMap has only ONE Java type variable, so appending Map's two arguments to that class is not a valid solution.
    @ParameterizedTest
    @MethodSource("backends")
    void rootRuntimeClassKeepsDeclaredMapSchema(final boolean abacus, final XmlParserType backend) {
        final XmlParser parser = parser(abacus, backend);
        final Map<String, List<Integer>> source = new LinkedHashMap<>(Map.of("k", new ArrayList<>(List.of(1, 2))));
        final String xml = parser.serialize(source, XmlSerConfig.create().setWriteTypeInfo(true));
        final Type<Map<String, List<Integer>>> target = Type.of("Map<String, List<Integer>>");
        final Map<String, List<Integer>> result = parser.deserialize(xml, target);
        assertEquals(source, result, xml);
        assertInstanceOf(LinkedHashMap.class, result, xml);

        final int start = xml.indexOf("type=\"") + 6;
        final int end = xml.indexOf('"', start);
        final String customXml = xml.substring(0, start) + StringKeyMap.class.getCanonicalName() + xml.substring(end);
        final Map<String, List<Integer>> custom = parser.deserialize(customXml, target);
        assertEquals(source, custom, customXml);
        assertInstanceOf(StringKeyMap.class, custom, customXml);
        assertThrows(ParsingException.class, () -> parser.deserialize(xml.substring(0, start) + "not.approved.Type" + xml.substring(end), target));
        final String incompatible = xml.substring(0, start) + "ArrayList" + xml.substring(end);
        assertEquals(source, parser.deserialize(incompatible, target), incompatible);
    }

    @ParameterizedTest
    @MethodSource("backends")
    void rootRuntimeClassKeepsDeclaredCollectionSchema(final boolean abacus, final XmlParserType backend) {
        final XmlParser parser = parser(abacus, backend);
        final List<Map<String, java.util.Optional<Integer>>> source = new LinkedList<>();
        source.add(new LinkedHashMap<>(Map.of("x", java.util.Optional.of(1))));
        source.add(new LinkedHashMap<>(Map.of("y", java.util.Optional.of(2))));
        final String xml = parser.serialize(source, XmlSerConfig.create().setWriteTypeInfo(true));
        final List<?> actual = parser.deserialize(xml, Type.of("List<Map<String, java.util.Optional<Integer>>>"));
        assertEquals(source, actual, xml);
        assertInstanceOf(LinkedList.class, actual, xml);
    }

    @ParameterizedTest
    @MethodSource("xmlBackends")
    void typedSingleValueRejectsExtraChildrenAndReaderRecovers(final XmlParserType backend) {
        final XmlParser parser = parser(false, backend);
        final Map<String, String> first = new LinkedHashMap<>(Map.of("x", "1", "z", "3"));
        final String invalid = parser
                .serialize(new LinkedHashMap<>(Map.of("k", java.util.Optional.of(new ArrayList<>(List.of(first, new LinkedHashMap<>(Map.of("y", "2"))))))));
        final Type<Map<String, java.util.Optional<Map<String, String>>>> target = Type.of("Map<String, java.util.Optional<Map<String, String>>>");
        assertThrows(ParsingException.class, () -> parser.deserialize(invalid, target));
        final Map<String, java.util.Optional<Map<String, String>>> valid = new LinkedHashMap<>(Map.of("k", java.util.Optional.of(first)));
        assertEquals(valid, parser.deserialize(parser.serialize(valid), target));
    }

    // Text after a structured child has historically been ignored by the plain StAX reader and DOM.
    // Wrapped values must do the same without skipping later children or consuming the following property.
    @ParameterizedTest
    @MethodSource("xmlWrappedCases")
    void wrappedMapIgnoresTrailingText(final XmlParserType backend, final Wrapper wrapper) {
        final XmlParser parser = parser(false, backend);
        final Type<Map<String, Object>> target = Type.of("Map<String, " + wrapper.typeName + "<Object>>");
        final Object held = Map.of("x", "1");
        final Map<String, Object> expected = Map.of("k", wrapper.wrap(held), "tail", wrapper.wrap("end"));
        for (final String text : List.of(" junk ", "<![CDATA[junk < &]]><!-- comment --><?test instruction?> more ", " \n\t ")) {
            final String xml = "<map><k><map><x>1</x></map>" + text + "</k><tail>end</tail></map>";
            assertEquals(expected, parser.deserialize(xml, target), xml);
        }
    }

    @ParameterizedTest
    @MethodSource("xmlWrappedCases")
    void wrappedObjectKeepsSiblingsSeparatedByText(final XmlParserType backend, final Wrapper wrapper) {
        final XmlParser parser = parser(false, backend);
        final String xml = "<map><k><map><x>1</x><z>3</z></map> between <![CDATA[more text]]>"
                + "<!-- comment --><?test instruction?><map><y>2</y></map> after </k><tail>end</tail></map>";
        final Type<Map<String, Object>> target = Type.of("Map<String, " + wrapper.typeName + "<Object>>");
        assertEquals(Map.of("k", wrapper.wrap(List.of(Map.of("x", "1", "z", "3"), Map.of("y", "2"))), "tail", wrapper.wrap("end")),
                parser.deserialize(xml, target), xml);
    }

    @ParameterizedTest
    @MethodSource("xmlWrappedCases")
    void wrappedSequencesIgnoreTextOutsideTheirElements(final XmlParserType backend, final Wrapper wrapper) {
        final XmlParser parser = parser(false, backend);
        final String elements = "<e>1</e> between <e isNull=\"true\"/> <![CDATA[more]]><e>2</e> after ";
        for (final String heldType : List.of("List<Integer>", "Integer[]")) {
            final String xml = "<map><k>" + elements + "</k><tail><e>3</e></tail></map>";
            final Type<Map<String, Object>> target = Type.of("Map<String, " + wrapper.typeName + "<" + heldType + ">>");
            final Map<String, Object> actual = parser.deserialize(xml, target);
            if (heldType.endsWith("[]")) {
                assertArrayEquals(new Integer[] { 1, null, 2 }, (Object[]) wrapper.unwrap(actual.get("k")), xml);
                assertArrayEquals(new Integer[] { 3 }, (Object[]) wrapper.unwrap(actual.get("tail")), xml);
            } else {
                assertEquals(wrapper.wrap(Arrays.asList(1, null, 2)), actual.get("k"), xml);
                assertEquals(wrapper.wrap(List.of(3)), actual.get("tail"), xml);
            }
        }
        // Runtime metadata can select the sequence shape inside an Object-valued wrapper as well.
        final String typed = "<map><k type=\"LinkedList&lt;Integer&gt;\">" + elements + "</k><tail>end</tail></map>";
        final Map<String, Object> actual = parser.deserialize(typed, Type.of("Map<String, " + wrapper.typeName + "<Object>>"));
        assertEquals(wrapper.wrap(Arrays.asList(1, null, 2)), actual.get("k"), typed);
        assertInstanceOf(LinkedList.class, wrapper.unwrap(actual.get("k")), typed);
        assertEquals(wrapper.wrap("end"), actual.get("tail"), typed);
    }

    @ParameterizedTest
    @MethodSource("xmlWrappedCases")
    void wrappedScalarChildKeepsItsOwnText(final XmlParserType backend, final Wrapper wrapper) {
        final XmlParser parser = parser(false, backend);
        final Type<Map<String, Object>> target = Type.of("Map<String, " + wrapper.typeName + "<Object>>");
        final String clean = "<map><k><e> inside <![CDATA[< &]]> text </e></k><tail>end</tail></map>";
        final Map<String, Object> expected = parser.deserialize(clean, target);
        if (backend == XmlParserType.StAX) {
            assertEquals(wrapper.wrap(" inside < & text "), expected.get("k"));
        }
        assertEquals(expected, parser.deserialize(clean.replace("</e></k>", "</e> ignored <![CDATA[outside]]></k>"), target));
    }

    @ParameterizedTest
    @MethodSource("xmlWrappedCases")
    void interstitialTextDoesNotHideAnExtraTypedChild(final XmlParserType backend, final Wrapper wrapper) {
        final XmlParser parser = parser(false, backend);
        for (final boolean metadata : new boolean[] { false, true }) {
            final Type<Map<String, Object>> target = Type.of("Map<String, " + wrapper.typeName + "<" + (metadata ? "Object" : "Map<String, String>") + ">>");
            final String prefix = metadata ? "<map><k type=\"Map&lt;String, String&gt;\">" : "<map><k>";
            final String first = prefix + "<map><x>1</x></map> junk ";
            final ParsingException error = assertThrows(ParsingException.class, () -> parser.deserialize(first + "<map><y>2</y></map></k></map>", target));
            assertTrue(error.getMessage().contains("Only one child element"), error::getMessage);
            // A valid subsequent document still parses after the rejected second child.
            assertEquals(Map.of("k", wrapper.wrap(Map.of("x", "1"))), parser.deserialize(first + "</k></map>", target));
        }
    }

    @ParameterizedTest
    @MethodSource("xmlWrappedCases")
    void interstitialTextDoesNotHideUnapprovedMetadata(final XmlParserType backend, final Wrapper wrapper) {
        final XmlParser parser = parser(false, backend);
        for (final String heldType : List.of("Object", "List<Object>")) {
            final Type<Map<String, Object>> target = Type.of("Map<String, " + wrapper.typeName + "<" + heldType + ">>");
            for (final String child : List.of("<map type=\"not.approved.Type\"><y>2</y></map>", "<e type=\"not.approved.Type\" isNull=\"true\"/>")) {
                final String xml = "<map><k><map><x>1</x></map> junk <![CDATA[more]]>" + child + "</k></map>";
                final ParsingException error = assertThrows(ParsingException.class, () -> parser.deserialize(xml, target), xml);
                assertTrue(error.getMessage().contains("not.approved.Type"), error::getMessage);
            }
        }
    }

    @ParameterizedTest
    @MethodSource("xmlBackends")
    void beanAndMapEntityWrappersIgnoreTextBetweenChildren(final XmlParserType backend) {
        final XmlParser parser = parser(false, backend);
        final String children = "<map><x>1</x></map><map><y>2</y></map>";
        final String decorated = children.replace("</map>", "</map> junk <![CDATA[more]]>");
        final String bean = "<WrappedBean><jdk>" + children + "</jdk><optional>" + children + "</optional><nullable>" + children
                + "</nullable><tail>finished</tail></WrappedBean>";
        final WrappedBean expected = parser.deserialize(bean, WrappedBean.class);
        assertEquals(java.util.Optional.of(List.of(Map.of("x", "1"), Map.of("y", "2"))), expected.getJdk());
        assertEquals(expected, parser.deserialize(bean.replace(children, decorated), WrappedBean.class));
        assertEquals("finished", expected.getTail());

        for (final Wrapper wrapper : Wrapper.values()) {
            final XmlDeserConfig config = XmlDeserConfig.create().setValueType("k", Type.of(wrapper.typeName + "<Object>"));
            final String entity = "<values><k>" + children + "</k><tail>finished</tail></values>";
            final MapEntity expectedEntity = parser.deserialize(entity, config, MapEntity.class);
            assertEquals(expectedEntity, parser.deserialize(entity.replace(children, decorated), config, MapEntity.class));
            assertEquals("finished", expectedEntity.get("tail"));
        }
    }

    @ParameterizedTest
    @MethodSource("xmlBackends")
    void trailingTextDoesNotAllowMalformedWrapperXml(final XmlParserType backend) {
        final XmlParser parser = parser(false, backend);
        final Type<?> target = Type.of("Map<String, java.util.Optional<Object>>");
        for (final String xml : List.of("<map><k><map><x>1</x></map> junk", "<map><k><map><x>1</x></map> junk </wrong></map>")) {
            assertThrows(ParsingException.class, () -> parser.deserialize(xml, target), xml);
        }
    }

    @ParameterizedTest
    @MethodSource("backends")
    void nullChildInsideWrapperStillRequiresTypeApproval(final boolean abacus, final XmlParserType backend) {
        final XmlParser parser = parser(abacus, backend);
        final List<Object> value = new ArrayList<>(Arrays.asList(null, new LinkedHashMap<>(Map.of("x", "1"))));
        final String xml = parser.serialize(new LinkedHashMap<>(Map.of("k", java.util.Optional.of(value))));
        final String invalid = xml.replace("isNull=\"true\"", "type=\"not.approved.Type\" isNull=\"true\"");
        assertThrows(ParsingException.class, () -> parser.deserialize(invalid, Type.of("Map<String, java.util.Optional<List<Map<String, String>>>>")));
    }

    @Test
    void containerRefinementDoesNotChangeGenericBeanPropertyExclusions() {
        final XmlParser parser = new XmlParserImpl(XmlParserType.StAX, null, null, Set.of(StringBean.class, GenericBean.class));
        final StringBean source = new StringBean();
        source.setPayload("ignored");
        final String xml = parser.serialize(source, XmlSerConfig.create().setWriteTypeInfo(true));
        final XmlDeserConfig config = XmlDeserConfig.create().setIgnoredPropNames(GenericBean.class, Set.of("payload"));
        final GenericBean<String> actual = parser.deserialize(xml, config, Type.of(GenericBean.class.getCanonicalName() + "<String>"));
        assertNull(actual.getPayload(), xml);
    }

    public static class StringKeyMap<V> extends LinkedHashMap<String, V> {
        private static final long serialVersionUID = 1L;
    }

    @lombok.Data
    public static class WrappedBean {
        private java.util.Optional<Object> jdk = java.util.Optional.empty();
        private u.Optional<Object> optional = u.Optional.empty();
        private u.Nullable<Object> nullable = u.Nullable.empty();
        private String tail = "end";
    }

    @lombok.Data
    public static class GenericBean<T> {
        private T payload;
    }

    public static class StringBean extends GenericBean<String> {
    }
}
