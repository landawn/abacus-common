package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.ParsingException;
import com.landawn.abacus.type.Type;

public class XmlNestedNullReview20261003Test extends TestBase {
    @ParameterizedTest
    @EnumSource(XmlParserType.class)
    public void nestedNullCollectionRetainsFollowingItems(final XmlParserType mode) {
        final XmlParser parser = new AbacusXmlParserImpl(mode);
        for (final String value : List.of("<list isNull=\"true\"/>", "<list isNull=\"true\"><e>ignored</e></list>")) {
            final String xml = "<list><e>" + value + "</e><e><list><e>kept</e></list></e></list>";
            assertEquals(Arrays.asList(null, List.of("kept")), parser.deserialize(xml, Type.of("List<List<String>>")), xml);
        }
    }

    @ParameterizedTest
    @EnumSource(XmlParserType.class)
    public void nestedNullArrayAndMapRetainFollowingItems(final XmlParserType mode) {
        final XmlParser parser = new AbacusXmlParserImpl(mode);
        for (final String value : List.of("<array isNull=\"true\"/>", "<array isNull=\"true\"><e>1</e></array>")) {
            final int[][] result = parser.deserialize("<array><e>" + value + "</e><e><array><e>7</e></array></e></array>", int[][].class);
            assertEquals(2, result.length);
            assertNull(result[0]);
            assertArrayEquals(new int[] { 7 }, result[1]);
        }
        for (final String value : List.of("<map isNull=\"true\"/>", "<map isNull=\"true\"><entry><key>ignored</key><value>1</value></entry></map>")) {
            final String xml = "<list><e>" + value + "</e><e><map><entry><key>kept</key><value>2</value></entry></map></e></list>";
            assertEquals(Arrays.asList(null, Map.of("kept", "2")), parser.deserialize(xml, Type.of("List<Map<String,String>>")), xml);
        }
    }

    @ParameterizedTest
    @EnumSource(XmlParserType.class)
    public void nullContainerWrapperOverridesNestedValue(final XmlParserType mode) {
        final XmlParser parser = new AbacusXmlParserImpl(mode);
        final String listXml = "<list><e isNull=\"true\"><list><e>ignored</e></list></e><e><list><e>kept</e></list></e></list>";
        assertEquals(Arrays.asList(null, List.of("kept")), parser.deserialize(listXml, Type.of("List<List<String>>")));

        final String mapXml = "<map><entry><key isNull=\"true\"><list><e>ignored</e></list></key>"
                + "<value isNull=\"true\"><list><e>ignored</e></list></value></entry>"
                + "<entry><key><list><e>kept</e></list></key><value><list><e>2</e></list></value></entry></map>";
        final Map<List<String>, List<String>> result = parser.deserialize(mapXml, Type.of("Map<List<String>,List<String>>"));
        assertEquals(2, result.size());
        assertTrue(result.containsKey(null));
        assertNull(result.get(null));
        assertEquals(List.of("2"), result.get(List.of("kept")));
    }

    @ParameterizedTest
    @EnumSource(XmlParserType.class)
    public void nullBeanAndPropertyWrappersRetainFollowingProperty(final XmlParserType mode) {
        final XmlParser parser = new AbacusXmlParserImpl(mode);
        for (final String value : List.of("<child isNull=\"true\"/>", "<child isNull=\"true\"><value>ignored</value></child>")) {
            final Parent result = parser.deserialize("<parent><child>" + value + "</child><tail>kept</tail></parent>", Parent.class);
            assertNull(result.getChild());
            assertEquals("kept", result.getTail());
        }
        final Parent markedProperty = parser
                .deserialize("<parent><child isNull=\"true\"><child><value>ignored</value></child></child><tail>kept</tail></parent>", Parent.class);
        assertNull(markedProperty.getChild());
        assertEquals("kept", markedProperty.getTail());
    }

    @ParameterizedTest
    @EnumSource(XmlParserType.class)
    public void falseAndOrdinaryMarkersPreserveNestedValues(final XmlParserType mode) {
        final XmlParser parser = new AbacusXmlParserImpl(mode);
        for (final String marker : List.of("", " isNull=\"false\"")) {
            final String xml = "<list><e" + marker + "><list" + marker + "><e>first</e></list></e><e><list><e>kept</e></list></e></list>";
            assertEquals(List.of(List.of("first"), List.of("kept")), parser.deserialize(xml, Type.of("List<List<String>>")));
        }
        // The writer's ordinary null-item representation remains accepted by every reader.
        final List<List<String>> value = Arrays.asList(null, List.of("kept"));
        assertEquals(value, parser.deserialize(parser.serialize(value), Type.of("List<List<String>>")));
        assertEquals(Map.of("kept", "2"),
                parser.deserialize("<map><entry isNull=\"true\"><key>kept</key><value>2</value></entry></map>", Type.of("Map<String,String>")));
        assertThrows(ParsingException.class, () -> parser.deserialize("<list><e><entry isNull=\"true\"/></e></list>", Type.of("List<List<String>>")));
    }

    @ParameterizedTest
    @EnumSource(XmlParserType.class)
    public void optionalNullPayloadIsDistinctFromNullWrapper(final XmlParserType mode) {
        final XmlParser parser = new AbacusXmlParserImpl(mode);
        final String xml = "<list><e><list isNull=\"true\"><e>ignored</e></list></e>"
                + "<e isNull=\"true\"><list><e>ignored</e></list></e><e><list><e>kept</e></list></e></list>";
        final List<Optional<List<String>>> expected = Arrays.asList(Optional.empty(), null, Optional.of(List.of("kept")));
        assertEquals(expected, parser.deserialize(xml, Type.of("List<java.util.Optional<List<String>>>")));
    }

    @ParameterizedTest
    @EnumSource(XmlParserType.class)
    public void nullMarkersKeepOwnTypeApprovalAndParserReuse(final XmlParserType mode) {
        final XmlParser parser = new AbacusXmlParserImpl(mode);
        for (final String value : List.of("<e isNull=\"true\" type=\"unapproved.NotAType\"/>", "<e><list isNull=\"true\" type=\"unapproved.NotAType\"/></e>",
                "<e isNull=\"true\"><list></e>")) {
            assertThrows(ParsingException.class, () -> parser.deserialize("<list>" + value + "</list>", Type.of("List<List<String>>")));
            assertEquals(List.of(List.of("kept")), parser.deserialize("<list><e><list><e>kept</e></list></e></list>", Type.of("List<List<String>>")));
        }
    }

    public static final class Parent {
        private Child child;
        private String tail;

        public Child getChild() {
            return child;
        }

        public void setChild(final Child child) {
            this.child = child;
        }

        public String getTail() {
            return tail;
        }

        public void setTail(final String tail) {
            this.tail = tail;
        }
    }

    public static final class Child {
        private String value;

        public String getValue() {
            return value;
        }

        public void setValue(final String value) {
            this.value = value;
        }
    }
}
