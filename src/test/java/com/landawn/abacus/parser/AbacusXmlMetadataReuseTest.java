package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.ParsingException;
import com.landawn.abacus.type.Type;

@Tag("unit")
public class AbacusXmlMetadataReuseTest extends TestBase {
    private static final String UNAPPROVED = "not.approved.Type";

    static Stream<Arguments> itemCases() {
        return Stream.of(Arguments.of("list", ""), Arguments.of("array", ""), Arguments.of("list", "ignored<![CDATA[text]]>"),
                Arguments.of("array", "ignored<![CDATA[text]]>"));
    }

    static Stream<Arguments> wrappedItemCases() {
        final List<Arguments> cases = new ArrayList<>();
        for (final String container : List.of("list", "array")) {
            for (final String prefix : List.of("", "ignored<![CDATA[text]]>")) {
                for (final XmlWrappedObjectRegressionTest.Wrapper wrapper : XmlWrappedObjectRegressionTest.Wrapper.values()) {
                    cases.add(Arguments.of(container, prefix, wrapper));
                }
            }
        }
        return cases.stream();
    }

    private static XmlParser parser() {
        return new AbacusXmlParserImpl(XmlParserType.StAX, null, null, Set.of());
    }

    private static Type<?> containerType(final String container, final String elementType) {
        return Type.of(container.equals("list") ? "List<" + elementType + ">" : elementType + "[]");
    }

    private static List<?> elements(final Object value) {
        return value instanceof List<?> list ? list : Arrays.asList((Object[]) value);
    }

    // Mixed text reaches an item through a different StAX event branch; both branches must retain the same schema.
    @ParameterizedTest
    @MethodSource("itemCases")
    void itemsKeepDeclaredTypesAndNulls(final String container, final String prefix) {
        final String xml = "<" + container + ">" + prefix + "<e type=\"String\">1</e><e type=\"Integer\" isNull=\"true\"/><e type=\"Integer\">3</e></"
                + container + ">";
        assertEquals(Arrays.asList(1, null, 3), elements(parser().deserialize(xml, containerType(container, "Integer"))), xml);
    }

    // Reusing metadata must not bypass approval when the declaration wins or a null marker bypasses conversion.
    @ParameterizedTest
    @MethodSource("itemCases")
    void itemsRejectUnapprovedMetadata(final String container, final String prefix) {
        final XmlParser parser = parser();
        for (final String nullMarker : List.of("", " isNull=\"true\"")) {
            final String xml = "<" + container + ">" + prefix + "<e type=\"" + UNAPPROVED + "\"" + nullMarker + ">1</e></" + container + ">";
            assertThrows(ParsingException.class, () -> parser.deserialize(xml, containerType(container, "Integer")), xml);
            final XmlDeserConfig config = XmlDeserConfig.create().setElementType(Integer.class);
            assertThrows(ParsingException.class, () -> parser.deserialize(xml, config, containerType(container, "Object")), xml);
        }
    }

    @ParameterizedTest
    @MethodSource("wrappedItemCases")
    void wrappedItemsKeepApprovedPayloadTypes(final String container, final String prefix, final XmlWrappedObjectRegressionTest.Wrapper wrapper) {
        final String xml = "<" + container + ">" + prefix + "<e type=\"Integer\">7</e><e type=\"Long\">8</e></" + container + ">";
        final Type<?> target = containerType(container, wrapper.typeName + "<Object>");
        assertEquals(List.of(wrapper.wrap(7), wrapper.wrap(8L)), elements(parser().deserialize(xml, target)), xml);
    }

    @ParameterizedTest
    @EnumSource(XmlWrappedObjectRegressionTest.Wrapper.class)
    void mapWrappersKeepApprovedPayloadTypes(final XmlWrappedObjectRegressionTest.Wrapper wrapper) {
        final XmlParser parser = parser();
        final String xml = "<map><entry><key type=\"Integer\">7</key><value type=\"Long\">8</value></entry></map>";
        final Type<?> target = Type.of("Map<" + wrapper.typeName + "<Object>, " + wrapper.typeName + "<Object>>");
        assertEquals(Map.of(wrapper.wrap(7), wrapper.wrap(8L)), parser.deserialize(xml, target), xml);
        assertThrows(ParsingException.class, () -> parser.deserialize(xml.replace("Integer", UNAPPROVED), target));
        assertThrows(ParsingException.class, () -> parser.deserialize(xml.replace("Long", UNAPPROVED), target));

        final String configuredXml = "<map><entry><key type=\"String\">k</key><value type=\"Integer\">7</value></entry></map>";
        final XmlDeserConfig config = XmlDeserConfig.create().setValueType("k", Type.of(wrapper.typeName + "<Object>"));
        assertEquals(Map.of("k", wrapper.wrap(7)), parser.deserialize(configuredXml, config, Map.class), configuredXml);
        assertThrows(ParsingException.class, () -> parser.deserialize(configuredXml.replace("Integer", UNAPPROVED), config, Map.class));
        assertThrows(ParsingException.class,
                () -> parser.deserialize(configuredXml.replace("<value ", "<value isNull=\"true\" ").replace("Integer", UNAPPROVED), config, Map.class));
    }
}
