package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.exception.ParsingException;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.type.AbstractCharSequenceType;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.BufferedXmlWriter;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.Objectory;

@Tag("unit")
class ParserStateReuseTest {
    public static class TextBean {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    @Test
    void repeatedFailuresRestoreDepthAndKeepPropertyDiagnostics() {
        JsonParser json = new JsonParserImpl();
        XmlParser xml = new XmlParserImpl(XmlParserType.StAX);
        TextBean bean = new TextBean();
        bean.setName("a\u0001b");
        List<Object> cycle = new ArrayList<>();
        cycle.add(cycle);
        // More calls than either serialization depth budget: even a one-step leak is observable.
        for (int i = 0; i < 300; i++) {
            assertThrows(ParsingException.class, () -> json.serialize(cycle));
            assertThrows(ParsingException.class, () -> xml.serialize(cycle));
            ParsingException invalid = assertThrows(ParsingException.class, () -> xml.serialize(bean));
            assertEquals("Property 'name' contains U+0001, which cannot be represented in XML 1.0", invalid.getMessage());
            assertThrows(ParsingException.class, () -> json.deserialize("{\"a\":[{\"b\":1} {\"b\":2}]}", Map.class));
            assertThrows(ParsingException.class, () -> xml.deserialize("<map><a><map><b></map></a></map>", Map.class));
            assertEquals(List.of(Map.of("x", 1)), json.deserialize("[{\"x\":1}]", List.class));
            assertEquals("ok", xml.deserialize("<textBean><name>ok</name></textBean>", TextBean.class).getName());
        }
    }

    @Test
    void failedWriterDoesNotPoisonTheNextSerialization() {
        JsonParser json = new JsonParserImpl();
        XmlParser xml = new XmlParserImpl(XmlParserType.StAX);
        Writer failing = new Writer() {
            @Override
            public void write(char[] value, int offset, int length) throws IOException {
                throw new IOException("test write failure");
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
        Map<String, Object> value = Map.of("nested", Map.of("text", "x".repeat(20_000)));
        for (int i = 0; i < 300; i++) {
            assertThrows(UncheckedIOException.class, () -> json.serialize(value, null, failing));
            assertThrows(UncheckedIOException.class, () -> xml.serialize(value, null, failing));
        }
        assertEquals(Map.of("x", 1), json.deserialize(json.serialize(Map.of("x", 1)), Map.class));
        assertTrue(xml.serialize(Map.of("x", 1)).contains(">1</x>"));
    }

    @Test
    void embeddedSettingsCanAlternateWithoutLeakingAcrossCallsOrThreads() throws Exception {
        AbstractXmlParser xml = new XmlParserImpl(XmlParserType.StAX);
        XmlSerConfig defaults = new XmlSerConfig();
        XmlSerConfig custom = new XmlSerConfig().setWriteLongAsString(true).setWriteBigDecimalAsPlain(true);
        List<Object> values = List.of(123L, new BigDecimal("1E+3"));
        String ordinary = xml.serializeEmbeddedJson(values, defaults);
        String configured = xml.serializeEmbeddedJson(values, custom);
        assertEquals("[123, 1E+3]", ordinary);
        assertEquals("[\"123\", 1000]", configured);
        try (var pool = Executors.newFixedThreadPool(4)) {
            var tasks = new ArrayList<java.util.concurrent.Callable<Void>>();
            for (int thread = 0; thread < 4; thread++) {
                tasks.add(() -> {
                    for (int i = 0; i < 300; i++) {
                        assertEquals(configured, xml.serializeEmbeddedJson(values, custom));
                        assertEquals(ordinary, xml.serializeEmbeddedJson(values, defaults));
                    }
                    return null;
                });
            }
            for (var future : pool.invokeAll(tasks)) {
                future.get();
            }
        }
        custom.setWriteLongAsString(false).setWriteBigDecimalAsPlain(false);
        assertEquals(ordinary, xml.serializeEmbeddedJson(values, custom));
    }

    @Test
    void scalarValidationPreservesCharactersAndMutableCharacterSequences() throws Exception {
        BufferedXmlWriter writer = Objectory.createBufferedXmlWriter();
        try {
            for (Object value : List.of('a', '\t', '\n', '\r', new StringBuilder("a & 😀"), new StringBuffer("b < 😀"))) {
                AbstractXmlParser.writeXmlScalar(writer, Type.of(value.getClass()), value, new XmlSerConfig(), "Value");
            }
            for (Object value : List.of('\u0000', '\u0001', '\uD800', '\uDC00', '\uFFFE', '\uFFFF', new StringBuilder("a\u0001"),
                    new StringBuffer("\uD800"))) {
                assertThrows(ParsingException.class,
                        () -> AbstractXmlParser.writeXmlScalar(writer, Type.of(value.getClass()), value, new XmlSerConfig(), "Value"));
            }
            assertTrue(writer.toString().contains("&amp;"));
        } finally {
            Objectory.recycle(writer);
        }
    }

    @Test
    @SuppressWarnings({ "deprecation", "unchecked" })
    void mutableTextSnapshotKeepsTheBuiltInQuotingAndEscaping() throws Exception {
        for (Object value : List.of(new StringBuilder("<tag>&\"' 😀\n"), new StringBuffer("<tag>&\"' 😀\n"))) {
            Type<Object> type = (Type<Object>) (Type<?>) Type.of(value.getClass());
            for (char quotation : new char[] { 0, '\'', '"' }) {
                XmlSerConfig config = new XmlSerConfig().setStringQuotation(quotation);
                BufferedXmlWriter expected = Objectory.createBufferedXmlWriter();
                BufferedXmlWriter actual = Objectory.createBufferedXmlWriter();
                try {
                    type.serializeTo(expected, value, config);
                    AbstractXmlParser.writeXmlScalar(actual, type, value, config, "Value");
                    assertEquals(expected.toString(), actual.toString());
                } finally {
                    Objectory.recycle(actual);
                    Objectory.recycle(expected);
                }
            }
        }
    }

    @Test
    void mutableTextKeepsCustomTypeSerialization() throws Exception {
        Type<CharSequence> custom = new AbstractCharSequenceType<CharSequence>("CustomMutableText") {
            @Override
            public Class<CharSequence> javaType() {
                return CharSequence.class;
            }

            @Override
            public String stringOf(CharSequence value) {
                return value.toString();
            }

            @Override
            public CharSequence valueOf(String value) {
                return value;
            }

            @Override
            public void serializeTo(CharacterWriter writer, CharSequence value, JsonXmlSerConfig<?> config) throws IOException {
                writer.write("custom:");
                writer.writeCharacter(value.toString());
            }
        };
        for (CharSequence value : List.of(new StringBuilder("text"), new StringBuffer("text"))) {
            BufferedXmlWriter writer = Objectory.createBufferedXmlWriter();
            try {
                AbstractXmlParser.writeXmlScalar(writer, custom, value, new XmlSerConfig(), "Value");
                assertEquals("custom:text", writer.toString());
            } finally {
                Objectory.recycle(writer);
            }
        }
    }
}
