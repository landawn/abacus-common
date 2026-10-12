package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.type.Type;

@Tag("unit")
class DeepReview20261004AbacusXmlSchemaTest extends TestBase {
    @ParameterizedTest
    @EnumSource(XmlParserType.class)
    void objectArrayTextAndItemsUseConfiguredComponent(final XmlParserType backend) {
        final XmlParser parser = new AbacusXmlParserImpl(backend);
        final XmlDeserConfig config = XmlDeserConfig.create().setElementType(Long.class);

        for (final String body : List.of("[\"1\",\"2\"]", "<e>1</e><e>2</e>")) {
            final Object[] array = parser.deserialize("<array>" + body + "</array>", config, Type.of("Object[]"));
            assertArrayEquals(new Object[] { 1L, 2L }, array);
        }
    }

    @ParameterizedTest
    @EnumSource(XmlParserType.class)
    void optionalObjectPayloadKeepsConfiguredComponentAfterMetadataRefinement(final XmlParserType backend) {
        final XmlParser parser = new AbacusXmlParserImpl(backend);
        final XmlDeserConfig config = XmlDeserConfig.create().setElementType(Long.class);
        final String metadata = List.of(1, 2).getClass().getName() + "&lt;Integer&gt;";

        for (final String body : List.of("[\"1\",\"2\"]", "<e>1</e><e>2</e>")) {
            final String xml = "<map><entry><key>k</key><value type=\"" + metadata + "\"><list>" + body + "</list></value></entry></map>";
            final Map<String, Optional<Object>> values = parser.deserialize(xml, config, Type.of("Map<String,java.util.Optional<Object>>"));
            assertEquals(List.of(1L, 2L), values.get("k").orElseThrow());
        }
    }
}
