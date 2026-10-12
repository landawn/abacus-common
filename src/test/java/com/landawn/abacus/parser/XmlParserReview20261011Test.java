package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.stream.Stream;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.ParsingException;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.type.Type;

@Tag("unit")
class XmlParserReview20261011Test extends TestBase {
    static Stream<Arguments> parsers() {
        return Stream.of(Arguments.of("standard DOM", new XmlParserImpl(XmlParserType.DOM)),
                Arguments.of("standard StAX", new XmlParserImpl(XmlParserType.StAX)), Arguments.of("Abacus DOM", new AbacusXmlParserImpl(XmlParserType.DOM)),
                Arguments.of("Abacus SAX", new AbacusXmlParserImpl(XmlParserType.SAX)),
                Arguments.of("Abacus StAX", new AbacusXmlParserImpl(XmlParserType.StAX)));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("parsers")
    void foreignMetadataDoesNotChangeValues(final String backend, final XmlParser parser) {
        for (final String attribute : List.of("x:isNull='true'", "x:type='Long'", "x:type='unapproved.Payload'")) {
            final String xml = "<list xmlns:x='urn:foreign'><e " + attribute + ">7</e></list>";
            assertEquals(List.of("7"), parser.deserialize(xml, List.class));
        }
        assertEquals(List.of("7"), parser.deserialize("<list xmlns:x='urn:foreign' x:type='unapproved.Payload'><e>7</e></list>", List.class));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("parsers")
    void unqualifiedMetadataWinsRegardlessOfAttributeOrder(final String backend, final XmlParser parser) {
        for (final String attribute : List.of("x:type='Long' type='String'", "type='String' x:type='Long'", "x:isNull='true' isNull='false'",
                "isNull='false' x:isNull='true'")) {
            assertEquals(List.of("7"), parser.deserialize("<list xmlns:x='urn:foreign'><e " + attribute + ">7</e></list>", List.class));
        }
        assertEquals(Arrays.asList(null, 8L), parser.deserialize("<list><e isNull='true'>discarded</e><e type='Long'>8</e></list>", List.class));
        assertThrows(ParsingException.class,
                () -> parser.deserialize("<list xmlns:x='urn:foreign'><e x:type='String' type='unapproved.Payload'>7</e></list>", List.class));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("parsers")
    void foreignRootNameDoesNotRedirectNodeTypeLookup(final String backend, final XmlParser parser) {
        final Map<String, Type<?>> nodeTypes = Map.of("list", Type.of(List.class));
        final String xml = "<list xmlns:x='urn:foreign' x:name='different'><e>kept</e></list>";
        assertEquals(List.of("kept"), parser.deserialize(new StringReader(xml), null, nodeTypes));
        assertEquals(List.of("kept"), parser.deserialize(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)), null, nodeTypes));
        for (final String attributes : List.of("x:name='different' name='list'", "name='list' x:name='different'")) {
            assertEquals(List.of("kept"),
                    parser.deserialize(new StringReader("<list xmlns:x='urn:foreign' " + attributes + "><e>kept</e></list>"), null, nodeTypes));
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("parsers")
    void foreignPropertyNameDoesNotRedirectBeanAssignment(final String backend, final XmlParser parser) {
        for (final String attributes : List.of("x:name='different' name='value'", "name='value' x:name='different'")) {
            final String xml = "<bean xmlns:x='urn:foreign' name='valueBean'><property " + attributes + ">kept</property></bean>";
            assertEquals("kept", parser.deserialize(xml, ValueBean.class).getValue());
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("parsers")
    void nullSerializationFlushesCallerOutputs(final String backend, final XmlParser parser) {
        final CountingWriter writer = new CountingWriter();
        parser.serialize(null, null, writer);
        assertTrue(writer.flushes > 0);
        assertEquals("", writer.toString());
        final CountingOutputStream output = new CountingOutputStream();
        parser.serialize(null, null, output);
        assertTrue(output.flushes > 0);
        assertEquals(0, output.size());
        assertThrows(UncheckedIOException.class, () -> parser.serialize(null, null, new StringWriter() {
            @Override
            public void flush() {
                throw new UncheckedIOException(new IOException("flush failed"));
            }
        }));
    }

    @Test
    void pooledSaxHandlerReleasesDeclaredTypeAfterFailedPropertyConversion() throws Exception {
        final XmlParser parser = new AbacusXmlParserImpl(XmlParserType.SAX);
        assertThrows(RuntimeException.class, () -> parser.deserialize("<nestedBean><payload>this is not a bean</payload></nestedBean>", NestedBean.class));

        final Field poolField = AbacusXmlParserImpl.class.getDeclaredField("xmlSAXHandlerPool");
        poolField.setAccessible(true);
        final Queue<?> pool = (Queue<?>) poolField.get(null);
        assertTrue(!pool.isEmpty());
        final Field slotField = AbacusXmlParserImpl.XmlSAXHandler.class.getDeclaredField("declaredSlotType");
        slotField.setAccessible(true);
        for (final Object handler : pool) {
            assertNull(slotField.get(handler), "Pooled handler must not retain a failed property's type and its class loader");
        }
        assertEquals("kept", parser.deserialize("<valueBean><value>kept</value></valueBean>", ValueBean.class).getValue());
    }

    private static final class CountingWriter extends StringWriter {
        int flushes;

        @Override
        public void flush() {
            flushes++;
        }
    }

    private static final class CountingOutputStream extends ByteArrayOutputStream {
        int flushes;

        @Override
        public void flush() {
            flushes++;
        }
    }

    public static class ValueBean {
        private String value;

        public String getValue() {
            return value;
        }

        public void setValue(final String value) {
            this.value = value;
        }
    }

    public static class NestedBean {
        private ValueBean payload;

        public ValueBean getPayload() {
            return payload;
        }

        public void setPayload(final ValueBean payload) {
            this.payload = payload;
        }
    }
}
