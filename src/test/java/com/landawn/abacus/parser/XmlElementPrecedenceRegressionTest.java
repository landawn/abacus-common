package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Stream;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.type.Type;

@Tag("unit")
public class XmlElementPrecedenceRegressionTest extends TestBase {
    static Stream<Arguments> backends() {
        return XmlWrappedObjectRegressionTest.backends();
    }

    static Stream<Arguments> nestedCases() {
        return Stream.of(XmlParserType.StAX, XmlParserType.DOM, XmlParserType.SAX)
                .flatMap(backend -> Stream.of(false, true).flatMap(child -> Stream.of(false, true).map(text -> Arguments.of(backend, child, text))));
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

    @ParameterizedTest
    @MethodSource("backends")
    void rootElementConfigurationOverridesGenericDeclaration(final boolean abacus, final XmlParserType backend) {
        final XmlParser parser = parser(abacus, backend);
        final XmlDeserConfig config = XmlDeserConfig.create().setElementType(Integer.class);
        // Include an unchanged class, a runtime implementation, and an unusable implementation with a mutable fallback.
        for (final String metadata : List.of("", "List<String>", "ArrayList<Integer>", "LinkedList<Integer>", immutableList("Integer"))) {
            final String declaration = metadata.startsWith("ArrayList") ? "ArrayList<Long>" : "List<Long>";
            final String xml = "<list" + attribute(metadata) + ">[\"1\",\"2\"]</list>";
            final List<?> actual = parser.deserialize(xml, config, Type.of(declaration));
            assertEquals(List.of(1, 2), actual, xml);
            assertEquals(Integer.class, actual.getFirst().getClass(), xml);
            if (metadata.startsWith("LinkedList")) {
                assertEquals(LinkedList.class, actual.getClass(), xml);
            }
        }
    }

    @ParameterizedTest
    @MethodSource("backends")
    void rootTextUsesConfigurationAndPreservesRuntimeMembership(final boolean abacus, final XmlParserType backend) {
        final String xml = "<list" + attribute("TreeSet<Integer>") + ">[\"2\",\"1\",\"2\"]</list>";
        final Collection<?> actual = parser(abacus, backend).deserialize(xml, XmlDeserConfig.create().setElementType(Integer.class),
                Type.of("Collection<Long>"));
        assertEquals(TreeSet.class, actual.getClass());
        assertEquals(List.of(1, 2), List.copyOf(actual));
    }

    @ParameterizedTest
    @MethodSource("backends")
    void nestedCollectionSchemaIsNotReplacedByScalarConfiguration(final boolean abacus, final XmlParserType backend) {
        final XmlDeserConfig config = XmlDeserConfig.create().setElementType(Integer.class);
        for (final String metadata : List.of("", immutableList("List<Integer>"))) {
            final String xml = nestedValueDocument(abacus, metadata, "[[\"1\",\"2\"]]");
            assertEquals(Map.of("k", List.of(List.of(1L, 2L))), parser(abacus, backend).deserialize(xml, config, Type.of("Map<String,List<List<Long>>>")), xml);
        }
    }

    @ParameterizedTest
    @MethodSource("backends")
    void originalObjectRootElementsStillUseConfiguration(final boolean abacus, final XmlParserType backend) {
        final XmlDeserConfig config = XmlDeserConfig.create().setElementType(Long.class);
        for (final String metadata : List.of("", immutableList("Integer"))) {
            final String xml = "<list" + attribute(metadata) + ">[\"1\",\"2\"]</list>";
            assertEquals(List.of(1L, 2L), parser(abacus, backend).deserialize(xml, config, Type.of("List<Object>")), xml);
        }
    }

    @ParameterizedTest
    @MethodSource("backends")
    void nestedConstraintKeepsRefinementsInsideItsOpenSlots(final boolean abacus, final XmlParserType backend) {
        final String xml = nestedValueDocument(abacus, immutableList("LinkedList<Integer>"), "[[\"1\",\"2\"]]");
        final Map<String, List<List<Object>>> actual = parser(abacus, backend).deserialize(xml, XmlDeserConfig.create().setElementType(String.class),
                Type.of("Map<String,List<List<Object>>>"));
        // The outer List element is explicitly declared, so scalar configuration cannot replace it. Its Object slot is still refinable.
        assertEquals(Map.of("k", List.of(List.of(1, 2))), actual);
        assertEquals(LinkedList.class, actual.get("k").getFirst().getClass());
    }

    private static String nestedValueDocument(final boolean abacus, final String metadata, final String text) {
        return abacus ? "<map><entry><key>k</key><value" + attribute(metadata) + "><list>" + text + "</list></value></entry></map>"
                : "<map><k" + attribute(metadata) + ">" + text + "</k></map>";
    }

    @ParameterizedTest
    @MethodSource("nestedCases")
    void nestedValueKeepsDeclarationSeparateFromMetadata(final XmlParserType backend, final boolean childMetadata, final boolean text) {
        final XmlParser parser = parser(true, backend);
        final String metadata = attribute(immutableList("Integer"));
        final String body = text ? "[\"1\",\"2\"]" : "<e>1</e><e>2</e>";
        final String xml = "<map><entry><key>k</key><value" + (childMetadata ? "" : metadata) + "><list" + (childMetadata ? metadata : "") + ">" + body
                + "</list></value></entry><entry><key>tail</key><value><list><e>3</e></list></value></entry></map>";
        for (final boolean constrained : new boolean[] { false, true }) {
            final String payload = constrained ? "List<Long>" : "List<Object>";
            final XmlDeserConfig config = XmlDeserConfig.create().setElementType(constrained ? Integer.class : Long.class);
            final Map<?, ?> actual = parser.deserialize(xml, config, Type.of("Map<String," + payload + ">"));
            assertEquals(Map.of("k", List.of(1L, 2L), "tail", List.of(3L)), actual, xml);
            for (final XmlWrappedObjectRegressionTest.Wrapper wrapper : XmlWrappedObjectRegressionTest.Wrapper.values()) {
                final Map<?, ?> wrapped = parser.deserialize(xml, config, Type.of("Map<String," + wrapper.typeName + "<" + payload + ">>"));
                assertEquals(wrapper.wrap(List.of(1L, 2L)), wrapped.get("k"), xml);
                assertEquals(wrapper.wrap(List.of(3L)), wrapped.get("tail"), xml);
            }
            final XmlDeserConfig configuredValue = XmlDeserConfig.create().setElementType(Integer.class).setValueType("k", Type.of("List<Long>"));
            final Map<?, ?> configured = parser.deserialize(xml, configuredValue, Type.of("Map<String,List<Object>>"));
            assertEquals(List.of(1L, 2L), configured.get("k"), xml);
            assertEquals(List.of(3), configured.get("tail"), xml);
        }
    }

    @ParameterizedTest
    @MethodSource("nestedCases")
    void nestedKeyRetainsConfiguredElements(final XmlParserType backend, final boolean childMetadata, final boolean text) {
        final String metadata = attribute(immutableList("Integer"));
        final String body = text ? "[\"1\",\"2\"]" : "<e>1</e><e>2</e>";
        final String xml = "<map><entry><key" + (childMetadata ? "" : metadata) + "><list" + (childMetadata ? metadata : "") + ">" + body
                + "</list></key><value>kept</value></entry></map>";
        final XmlDeserConfig config = XmlDeserConfig.create().setElementType(Long.class);
        assertEquals(Map.of(List.of(1L, 2L), "kept"), parser(true, backend).deserialize(xml, config, Type.of("Map<List<Object>,String>")), xml);
    }
}
