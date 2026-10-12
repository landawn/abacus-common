package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.MapEntity;

@Tag("unit")
class XmlPerNodeTypeDeepReviewTest extends TestBase {
    @Test
    void typedMapValuesSurviveAnUntypedNullFirstEntry() {
        final Map<String, Object> original = new LinkedHashMap<>();
        original.put("missing", null);
        original.put("count", 42);
        original.put("enabled", true);

        for (final XmlParserType backend : new XmlParserType[] { XmlParserType.DOM, XmlParserType.StAX }) {
            final XmlParser parser = new XmlParserImpl(backend);
            final String xml = parser.serialize(original, XmlSerConfig.create().setWriteTypeInfo(true));
            final Map<?, ?> result = parser.deserialize(xml, Map.class);
            assertNull(result.get("missing"), xml);
            assertEquals(42, result.get("count"), xml);
            assertEquals(true, result.get("enabled"), xml);
        }
    }

    @Test
    void typedMapEntityValuesSurviveAnUntypedNullFirstProperty() {
        final String xml = "<Result><missing isNull=\"true\"/><count type=\"Integer\">42</count>" + "<enabled type=\"Boolean\">true</enabled></Result>";

        for (final XmlParserType backend : new XmlParserType[] { XmlParserType.DOM, XmlParserType.StAX }) {
            final XmlParser parser = new XmlParserImpl(backend);
            final MapEntity result = parser.deserialize(xml, MapEntity.class);
            assertNull(result.get("missing"), xml);
            assertEquals(Integer.valueOf(42), (Object) result.get("count"), xml);
            assertEquals(Boolean.TRUE, (Object) result.get("enabled"), xml);
        }
    }

    // DOM only: the StAX reader reads a bean element in an Object-typed array/collection as a map whatever its type attribute
    // (a recorded design decision), and that result does not depend on the order of the elements.
    // (A typed Object property inside a bean element WITHOUT a type attribute is not covered: DOM then ignores the type attributes
    // below that bean element by design, and the writer never produces that shape.)
    @Test
    void typedContainerElementsSurviveAnUntypedNullFirstElement() {
        final Payload payload = new Payload();
        payload.setNumber(42);

        final XmlParser parser = new XmlParserImpl(XmlParserType.DOM, null, null, Set.of(Payload.class));
        final XmlSerConfig config = XmlSerConfig.create().setWriteTypeInfo(true);
        final String arrayXml = parser.serialize(new Object[] { null, payload }, config);
        final Object[] array = parser.deserialize(arrayXml, Object[].class);
        assertNull(array[0], arrayXml);
        assertEquals(Payload.class, array[1].getClass(), arrayXml);
        assertEquals(42, ((Payload) array[1]).getNumber(), arrayXml);

        final String listXml = parser.serialize(Arrays.asList(null, payload), config);
        final List<?> list = parser.deserialize(listXml, List.class);
        assertNull(list.get(0), listXml);
        assertEquals(Payload.class, list.get(1).getClass(), listXml);
        assertEquals(42, ((Payload) list.get(1)).getNumber(), listXml);
    }

    public static class Payload {
        private int number;

        public int getNumber() {
            return number;
        }

        public void setNumber(final int number) {
            this.number = number;
        }
    }
}
