package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.ParsingException;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.MapEntity;

public class XmlStandardNullReview20261003Test extends TestBase {
    private static <T> T read(final XmlParser parser, final boolean reader, final String xml, final Class<T> type) {
        return reader ? parser.deserialize(new StringReader(xml), type) : parser.deserialize(xml, type);
    }

    @ParameterizedTest
    @EnumSource(value = XmlParserType.class, names = { "StAX", "DOM" })
    public void nullScalarPayloadDoesNotDropFollowingItems(final XmlParserType backend) {
        final XmlParser parser = new XmlParserImpl(backend);
        for (final boolean reader : new boolean[] { false, true }) {
            for (final String payload : List.of("ignored", "<![CDATA[ignored]]>", "before<inner>ignored</inner>after")) {
                final String elements = "<e isNull=\"true\">" + payload + "</e><e>kept</e>";
                assertEquals(Arrays.asList(null, "kept"), read(parser, reader, "<list>" + elements + "</list>", List.class));
                assertArrayEquals(new Object[] { null, "kept" }, read(parser, reader, "<array>" + elements + "</array>", Object[].class));
            }
        }
    }

    @ParameterizedTest
    @EnumSource(value = XmlParserType.class, names = { "StAX", "DOM" })
    public void nullContainerPayloadDoesNotDropFollowingItems(final XmlParserType backend) {
        final XmlParser parser = new XmlParserImpl(backend);
        for (final boolean reader : new boolean[] { false, true }) {
            final String elements = "<list isNull=\"true\"><e><inner>ignored</inner></e></list><e>kept</e>";
            assertEquals(Arrays.asList(null, "kept"), read(parser, reader, "<list>" + elements + "</list>", List.class));
            assertArrayEquals(new Object[] { null, "kept" }, read(parser, reader, "<array>" + elements + "</array>", Object[].class));
        }
    }

    @ParameterizedTest
    @EnumSource(value = XmlParserType.class, names = { "StAX", "DOM" })
    public void markedPropertiesIgnoreNestedPayloadAndRetainFollowingProperty(final XmlParserType backend) {
        final XmlParser parser = new XmlParserImpl(backend);
        for (final boolean reader : new boolean[] { false, true }) {
            final String properties = "<items isNull=\"true\"><e>ignored</e></items><tail>kept</tail>";
            final Bean bean = read(parser, reader, "<bean>" + properties + "</bean>", Bean.class);
            assertNull(bean.getItems());
            assertEquals("kept", bean.getTail());
            final Map<?, ?> map = read(parser, reader, "<map>" + properties + "</map>", Map.class);
            assertTrue(map.containsKey("items"));
            assertNull(map.get("items"));
            assertEquals("kept", map.get("tail"));
            final MapEntity entity = read(parser, reader, "<entity>" + properties + "</entity>", MapEntity.class);
            assertTrue(entity.containsKey("items"));
            assertNull(entity.get("items"));
            assertEquals("kept", entity.get("tail"));

            final String nested = "<child><child isNull=\"true\"><value>ignored</value></child></child>"
                    + "<map><map isNull=\"true\"><ignored>1</ignored></map></map><tail>kept</tail>";
            final Bean nestedBean = read(parser, reader, "<bean>" + nested + "</bean>", Bean.class);
            assertNull(nestedBean.getChild());
            assertNull(nestedBean.getMap());
            assertEquals("kept", nestedBean.getTail());
        }
    }

    @ParameterizedTest
    @EnumSource(value = XmlParserType.class, names = { "StAX", "DOM" })
    public void propertyCollectionAndWrappedCollectionKeepNullItemPositions(final XmlParserType backend) {
        final XmlParser parser = new XmlParserImpl(backend);
        for (final boolean reader : new boolean[] { false, true }) {
            final String elements = "<list isNull=\"true\"><e>ignored</e></list><e>kept</e>";
            final String properties = "<items>" + elements + "</items><wrapped>" + elements + "</wrapped><tail>kept</tail>";
            final Bean bean = read(parser, reader, "<bean>" + properties + "</bean>", Bean.class);
            assertEquals(Arrays.asList(null, "kept"), bean.getItems());
            assertEquals(Optional.of(Arrays.asList(null, "kept")), bean.getWrapped());
            assertEquals("kept", bean.getTail());

            final XmlDeserConfig config = XmlDeserConfig.create().setValueType("items", Type.of("List<Object>"));
            final String xml = "<map><items>" + elements + "</items><tail>kept</tail></map>";
            final Map<?, ?> map = reader ? parser.deserialize(new StringReader(xml), config, Map.class) : parser.deserialize(xml, config, Map.class);
            assertEquals(Arrays.asList(null, "kept"), map.get("items"));
            assertEquals("kept", map.get("tail"));
            final MapEntity entity = reader ? parser.deserialize(new StringReader(xml), config, MapEntity.class)
                    : parser.deserialize(xml, config, MapEntity.class);
            assertEquals(Arrays.asList(null, "kept"), entity.get("items"));
            assertEquals("kept", entity.get("tail"));
        }
    }

    @ParameterizedTest
    @EnumSource(value = XmlParserType.class, names = { "StAX", "DOM" })
    public void trueMarkersUseTheEstablishedCaseInsensitiveBooleanRule(final XmlParserType backend) {
        final XmlParser parser = new XmlParserImpl(backend);
        for (final String marker : List.of("true", "TRUE", "True")) {
            assertEquals(Arrays.asList(null, "kept"), parser.deserialize("<list><e isNull=\"" + marker + "\">ignored</e><e>kept</e></list>", List.class));
        }
    }

    @ParameterizedTest
    @EnumSource(value = XmlParserType.class, names = { "StAX", "DOM" })
    public void nullMarkersStillRequireApprovedOwnTypeMetadata(final XmlParserType backend) {
        final XmlParser parser = new XmlParserImpl(backend);
        for (final String item : List.of("<e isNull=\"true\" type=\"unapproved.NotAType\">ignored</e>",
                "<list isNull=\"true\" type=\"unapproved.NotAType\"><e>ignored</e></list>")) {
            assertThrows(ParsingException.class, () -> parser.deserialize("<list>" + item + "</list>", List.class));
        }
        final String properties = "<items isNull=\"true\" type=\"unapproved.NotAType\"><e>ignored</e></items>";
        assertThrows(ParsingException.class, () -> parser.deserialize("<bean>" + properties + "</bean>", Bean.class));
        assertThrows(ParsingException.class, () -> parser.deserialize("<map>" + properties + "</map>", Map.class));
        assertThrows(ParsingException.class, () -> parser.deserialize("<entity>" + properties + "</entity>", MapEntity.class));
    }

    @ParameterizedTest
    @EnumSource(value = XmlParserType.class, names = { "StAX", "DOM" })
    public void emptyNullMarkersAndOrdinaryValuesRemainSupported(final XmlParserType backend) {
        final XmlParser parser = new XmlParserImpl(backend);
        assertEquals(Arrays.asList(null, "kept"), parser.deserialize("<list><null isNull=\"true\"/><e>kept</e></list>", List.class));
        final Bean bean = parser.deserialize("<bean><items isNull=\"true\"/><tail>kept</tail></bean>", Bean.class);
        assertNull(bean.getItems());
        assertEquals("kept", bean.getTail());
        for (final String marker : List.of("", " isNull=\"false\"")) {
            assertEquals(List.of("first", "kept"), parser.deserialize("<list><e" + marker + ">first</e><e>kept</e></list>", List.class));
            final Bean ordinary = parser.deserialize("<bean><items" + marker + "><e>first</e></items><tail>kept</tail></bean>", Bean.class);
            assertEquals(List.of("first"), ordinary.getItems());
            assertEquals("kept", ordinary.getTail());
        }
    }

    @ParameterizedTest
    @EnumSource(value = XmlParserType.class, names = { "StAX", "DOM" })
    public void malformedNullSubtreesStillFailAndReaderStateIsReusable(final XmlParserType backend) {
        final XmlParser parser = new XmlParserImpl(backend);
        for (final String xml : List.of("<list><list isNull=\"true\"><e></list></list>", "<bean><items isNull=\"true\"><e></items></bean>")) {
            assertThrows(ParsingException.class, () -> parser.deserialize(xml, xml.startsWith("<bean>") ? Bean.class : List.class));
            assertEquals(List.of("kept"), parser.deserialize("<list><e>kept</e></list>", List.class));
        }
    }

    public static class Bean {
        private List<Object> items = List.of("sentinel");
        private Optional<List<Object>> wrapped;
        private String tail;
        private Child child = new Child();
        private Map<String, String> map = Map.of("sentinel", "value");

        public List<Object> getItems() {
            return items;
        }

        public void setItems(final List<Object> items) {
            this.items = items;
        }

        public Optional<List<Object>> getWrapped() {
            return wrapped;
        }

        public void setWrapped(final Optional<List<Object>> wrapped) {
            this.wrapped = wrapped;
        }

        public String getTail() {
            return tail;
        }

        public void setTail(final String tail) {
            this.tail = tail;
        }

        public Child getChild() {
            return child;
        }

        public void setChild(final Child child) {
            this.child = child;
        }

        public Map<String, String> getMap() {
            return map;
        }

        public void setMap(final Map<String, String> map) {
            this.map = map;
        }
    }

    public static class Child {
        private String value;

        public String getValue() {
            return value;
        }

        public void setValue(final String value) {
            this.value = value;
        }
    }
}
