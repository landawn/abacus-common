package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.type.Type;

@Tag("unit")
class DeepReview20261004XmlElementSchemaTest extends TestBase {

    static Stream<Arguments> backends() {
        return Stream.of(Arguments.of(false, XmlParserType.StAX), Arguments.of(false, XmlParserType.DOM), Arguments.of(true, XmlParserType.StAX),
                Arguments.of(true, XmlParserType.DOM), Arguments.of(true, XmlParserType.SAX));
    }

    private static XmlParser parser(final boolean abacus, final XmlParserType backend) {
        return abacus ? new AbacusXmlParserImpl(backend) : new XmlParserImpl(backend);
    }

    @ParameterizedTest
    @MethodSource("backends")
    void configuredRootComponentWinsForTextAndChildElements(final boolean abacus, final XmlParserType backend) {
        final XmlParser parser = parser(abacus, backend);
        final XmlDeserConfig config = XmlDeserConfig.create().setElementType(Long.class);

        for (final String body : List.of("[\"1\",\"2\"]", "<e>1</e><e>2</e>")) {
            for (final String metadata : List.of("", " type=\"ArrayList&lt;Integer&gt;\"")) {
                final List<?> values = parser.deserialize("<list" + metadata + ">" + body + "</list>", config, Type.of("List<Integer>"));
                assertEquals(List.of(1L, 2L), values);
                assertEquals(Long.class, values.getFirst().getClass());
            }

            final Long[] values = parser.deserialize("<array>" + body + "</array>", config, Type.of("Long[]"));
            assertArrayEquals(new Long[] { 1L, 2L }, values);

            if (!abacus) {
                final Object[] openValues = parser.deserialize("<array>" + body + "</array>", XmlDeserConfig.create().setElementType(Long.class),
                        Type.of("Object[]"));
                assertArrayEquals(new Object[] { 1L, 2L }, openValues);
            }
        }
    }

    @ParameterizedTest
    @MethodSource("backends")
    void configuredComponentWinsForAnOriginallyOpenMapValue(final boolean abacus, final XmlParserType backend) {
        final XmlParser parser = parser(abacus, backend);
        final XmlDeserConfig config = XmlDeserConfig.create().setElementType(Long.class);
        final String metadata = " type=\"ArrayList&lt;Integer&gt;\"";

        for (final String body : List.of("[\"1\",\"2\"]", "<e>1</e><e>2</e>")) {
            final String xml = abacus ? "<map><entry><key>k</key><value" + metadata + "><list>" + body + "</list></value></entry></map>"
                    : "<map><k" + metadata + ">" + body + "</k></map>";
            final Map<?, ?> values = parser.deserialize(xml, config, Type.of("Map<String,List<Object>>"));
            assertEquals(List.of(1L, 2L), values.get("k"));

            final Map<?, ?> wrapped = parser.deserialize(xml, config, Type.of("Map<String,java.util.Optional<List<Object>>>"));
            assertEquals(Optional.of(List.of(1L, 2L)), wrapped.get("k"));
        }
    }

    @ParameterizedTest
    @MethodSource("backends")
    void arrayTextRetainsConfiguredNestedTypeArguments(final boolean abacus, final XmlParserType backend) {
        final XmlParser parser = parser(abacus, backend);
        final XmlDeserConfig listConfig = XmlDeserConfig.create().setElementType(Type.of("List<Long>"));
        for (final String body : List.of("[[\"1\",\"2\"]]", "<list>[\"1\",\"2\"]</list>")) {
            final Object[] values = parser.deserialize("<array>" + body + "</array>", listConfig, Type.of("Object[]"));
            assertArrayEquals(new Object[] { List.of(1L, 2L) }, values);
        }

        final XmlDeserConfig mapConfig = XmlDeserConfig.create().setElementType(Type.of("Map<String,List<Long>>"));
        final String mapElement = abacus ? "<e><map><entry><key>k</key><value>[\"1\",\"2\"]</value></entry></map></e>" : "<map><k>[\"1\",\"2\"]</k></map>";
        for (final String body : List.of("[{\"k\":[\"1\",\"2\"]}]", mapElement)) {
            final Object[] values = parser.deserialize("<array>" + body + "</array>", mapConfig, Type.of("Object[]"));
            assertArrayEquals(new Object[] { Map.of("k", List.of(1L, 2L)) }, values);
        }
    }
}
