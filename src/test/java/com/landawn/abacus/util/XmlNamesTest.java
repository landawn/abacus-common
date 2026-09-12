package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.ctc.wstx.api.WstxOutputProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;
import com.landawn.abacus.TestBase;

public class XmlNamesTest extends TestBase {

    @Test
    void ownedMappersRejectMalformedAndLossyMapKeys() {
        final SerializationConfig config = XmlMappers.createSerializationConfig();
        for (final String name : new String[] { "", "1abc", "a b", "a<b", "a>b", "x ", "x\t", "a:b", "xml:lang" }) {
            final Map<String, String> value = Collections.singletonMap(name, "value");
            assertThrows(RuntimeException.class, () -> XmlMappers.toXml(value));
            assertThrows(RuntimeException.class, () -> XmlMappers.toXml(value, true));
            assertThrows(RuntimeException.class, () -> XmlMappers.toXml(value, SerializationFeature.INDENT_OUTPUT));
            for (int i = 0; i < 2; i++) {
                assertThrows(RuntimeException.class, () -> XmlMappers.toXml(value, config));
            }
            assertThrows(RuntimeException.class, () -> XmlMappers.toXml(value, new StringWriter(), config));
            assertThrows(RuntimeException.class, () -> XmlMappers.toXml(value, new ByteArrayOutputStream(), config));
        }
    }

    @Test
    void annotationAndConfiguredRootNamesAreValidated() {
        assertThrows(RuntimeException.class, () -> XmlMappers.toXml(new InvalidRoot()));
        assertThrows(RuntimeException.class, () -> XmlMappers.toXml(new InvalidProperty()));
        assertThrows(RuntimeException.class, () -> XmlMappers.toXml(new InvalidAttribute()));
        final SerializationConfig config = XmlMappers.createSerializationConfig().withRootName("bad root");
        assertThrows(RuntimeException.class, () -> XmlMappers.toXml(Map.of("x", "value"), config));
    }

    @Test
    void validUnicodeNamesRoundTripExactly() {
        final SerializationConfig config = XmlMappers.createSerializationConfig();
        for (final String name : new String[] { "normal", "_name", "a-b.c", "a\u00B7b", "caf\u00E9", "\u65E5\u672C\u8A9E", "a\u0301", "\uD800\uDC00name",
                "xmlData" }) {
            final Map<String, String> value = Map.of(name, "\u65E5\u672C\uD83D\uDE00<&>");
            assertEquals(value, XmlMappers.fromXml(XmlMappers.toXml(value), Map.class));
            assertEquals(value, XmlMappers.fromXml(XmlMappers.toXml(value, true), Map.class));
            assertEquals(value, XmlMappers.fromXml(XmlMappers.toXml(value, config), Map.class));
        }
        assertDoesNotThrow(() -> XmlMappers.toXml(null));
        assertDoesNotThrow(() -> XmlMappers.toXml(Map.of()));
    }

    @Test
    void namespacesAttributesAndTextRemainSupported() {
        final Namespaced source = new Namespaced();
        source.language = "fr";
        source.item = "other";
        final String xml = XmlMappers.toXml(source);
        final Namespaced restored = XmlMappers.fromXml(xml, Namespaced.class);
        assertEquals(source.language, restored.language);
        assertEquals(source.item, restored.item);
        final Text sourceText = new Text();
        sourceText.id = "id2";
        sourceText.text = "bonjour";
        final Text restoredText = XmlMappers.fromXml(XmlMappers.toXml(sourceText), Text.class);
        assertEquals(sourceText.id, restoredText.id);
        assertEquals(sourceText.text, restoredText.text);
    }

    @Test
    void wrappingRetainsTheCallersNameValidationPolicy() throws Exception {
        final XmlMapper mapper = new XmlMapper();
        mapper.getFactory().getXMLOutputFactory().setProperty(WstxOutputProperties.P_OUTPUT_VALIDATE_NAMES, false);
        final Map<String, String> value = Map.of("x ", "value");
        final String expected = mapper.writeValueAsString(value);
        assertEquals(expected, XmlMappers.wrap(mapper).toXml(value));
        assertEquals(Boolean.FALSE, mapper.getFactory().getXMLOutputFactory().getProperty(WstxOutputProperties.P_OUTPUT_VALIDATE_NAMES));
    }

    @JacksonXmlRootElement(localName = "1bad")
    public static class InvalidRoot {
        public String value = "x";
    }

    public static class InvalidProperty {
        @JsonProperty("a b")
        public String value = "x";
    }

    public static class InvalidAttribute {
        @JacksonXmlProperty(isAttribute = true, localName = "a b")
        public String value = "x";
    }

    @JacksonXmlRootElement(localName = "root", namespace = "urn:test")
    public static class Namespaced {
        @JacksonXmlProperty(isAttribute = true, localName = "lang", namespace = "http://www.w3.org/XML/1998/namespace")
        public String language = "en";
        @JacksonXmlProperty(localName = "item", namespace = "urn:other")
        public String item = "value";
    }

    public static class Text {
        @JacksonXmlProperty(isAttribute = true)
        public String id = "x";
        @JacksonXmlText
        public String text = "hello";
    }
}
