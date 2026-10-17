package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.landawn.abacus.exception.ParsingException;

@Tag("unit")
class AbacusXmlUntypedArrayTest {
    private static final String ITEM = "<e><reviewUnknownArrayItem><value>kept</value></reviewUnknownArrayItem></e>";
    private static final List<String> PREFIXES = List.of("", " ", "stray", "ignored<![CDATA[text]]>", "ignored<!-- comment -->", "ignored<?item next?>");

    @ParameterizedTest
    @EnumSource(XmlParserType.class)
    void rootArrayRetainsStructuredItemsAfterMixedText(final XmlParserType backend) {
        final XmlParser parser = new AbacusXmlParserImpl(backend);

        for (final String prefix : PREFIXES) {
            final String xml = "<array>" + prefix + ITEM + "<e isNull=\"true\"/></array>";
            assertItems(parser.deserialize(xml, Object.class));
            assertItems(parser.deserialize(new StringReader(xml), Object.class));
            assertItems(parser.deserialize(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)), Object.class));
        }
    }

    @ParameterizedTest
    @EnumSource(XmlParserType.class)
    void nestedArrayRetainsStructuredItemsAfterMixedText(final XmlParserType backend) {
        final XmlParser parser = new AbacusXmlParserImpl(backend);

        for (final String prefix : PREFIXES) {
            final String xml = "<list><e><array>" + prefix + ITEM + "<e isNull=\"true\"/></array></e><e>tail</e></list>";
            final List<?> result = parser.deserialize(xml, List.class);
            assertEquals(2, result.size());
            assertItems(result.get(0));
            assertEquals("tail", result.get(1));
        }
    }

    @ParameterizedTest
    @EnumSource(XmlParserType.class)
    void declaredArrayComponentRemainsEffectiveAfterMixedText(final XmlParserType backend) {
        final XmlParser parser = new AbacusXmlParserImpl(backend);

        for (final String prefix : PREFIXES) {
            assertArrayEquals(new Integer[] { 7, null, 8 },
                    parser.deserialize("<array>" + prefix + "<e>7</e><e isNull=\"true\"/><e>8</e></array>", Integer[].class));
        }
    }

    @ParameterizedTest
    @EnumSource(XmlParserType.class)
    void mixedTextBetweenItemsRetainsScalarsNullsAndNestedContainers(final XmlParserType backend) {
        final XmlParser parser = new AbacusXmlParserImpl(backend);

        for (final String text : PREFIXES) {
            final String xml = "<array><e>before</e>" + text + ITEM + text + "<e isNull=\"true\"/>" + text + "<e><list><e>3</e></list></e>" + text
                    + "<e><array><e>4</e></array></e>" + text + "<e>after</e>" + text + "</array>";
            final Object[] expected = { "before", Map.of("value", "kept"), null, List.of("3"), new String[] { "4" }, "after" };
            assertArrayEquals(expected, assertInstanceOf(Object[].class, parser.deserialize(xml, Object.class)), text);
            assertArrayEquals(expected, parser.deserialize(xml, Object[].class), text);
        }
    }

    @ParameterizedTest
    @EnumSource(XmlParserType.class)
    void configuredItemsKeepTheirOwnContainerElementSchemaAfterMixedText(final XmlParserType backend) {
        final XmlParser parser = new AbacusXmlParserImpl(backend);
        final XmlDeserConfig config = XmlDeserConfig.create().setElementType("List<Long>");

        for (final String text : PREFIXES) {
            final String xml = "<array>" + text + "<e><list><e>7</e></list></e>" + text + "<e><list>[8]</list></e></array>";
            final Object[] result = parser.deserialize(xml, config, Object[].class);
            assertEquals(Object[].class, result.getClass());
            assertArrayEquals(new Object[] { List.of(7L), List.of(8L) }, result, text);
        }
    }

    @ParameterizedTest
    @EnumSource(XmlParserType.class)
    void mixedTextStillHonorsItemMetadataAndValidatesNullItemMetadata(final XmlParserType backend) {
        final XmlParser parser = new AbacusXmlParserImpl(backend);

        for (final String text : PREFIXES) {
            final String xml = "<array>" + text + "<e type=\"List&lt;Long&gt;\"><list><e>7</e></list></e>" + text + "<e type=\"Long\">8</e></array>";
            assertArrayEquals(new Object[] { List.of(7L), 8L }, assertInstanceOf(Object[].class, parser.deserialize(xml, Object.class)), text);
            assertThrows(ParsingException.class,
                    () -> parser.deserialize("<array>" + text + "<e isNull=\"true\" type=\"not.approved.ArrayItem\"/></array>", Object.class), text);
        }
    }

    @ParameterizedTest
    @EnumSource(XmlParserType.class)
    void mixedTextDoesNotPermitMultipleValuesInsideOneItem(final XmlParserType backend) {
        final XmlParser parser = new AbacusXmlParserImpl(backend);
        assertThrows(ParsingException.class, () -> parser.deserialize("<array>ignored<e><list/><list/></e></array>", Object.class));
    }

    private static void assertItems(final Object value) {
        final Object[] items = assertInstanceOf(Object[].class, value);
        assertArrayEquals(new Object[] { Map.of("value", "kept"), null }, items);
    }
}
