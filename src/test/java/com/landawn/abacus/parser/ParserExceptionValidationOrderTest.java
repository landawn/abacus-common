package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.avro.Schema;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.landawn.abacus.TestBase;
import com.esotericsoftware.kryo.KryoException;
import com.landawn.abacus.exception.ParsingException;
import com.landawn.abacus.poi.ExcelUtil;
import com.landawn.abacus.pool.ObjectPool;
import com.landawn.abacus.pool.PoolFactory;
import com.landawn.abacus.pool.Poolable;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.type.TypeFactory;

import jakarta.xml.bind.annotation.XmlRootElement;

class ParserExceptionValidationOrderTest extends TestBase {
    @TempDir
    Path directory;

    @Test
    void sourceValidationPrecedesTargetResolution() {
        for (XmlParser parser : List.of(ParserFactory.createXmlParser(), ParserFactory.createAbacusXmlParser())) {
            assertTrue(
                    assertThrows(IllegalArgumentException.class, () -> parser.deserialize((File) null, null, (Class<?>) null)).getMessage().contains("source"));
            assertTrue(
                    assertThrows(IllegalArgumentException.class, () -> parser.deserialize((File) null, null, (Type<?>) null)).getMessage().contains("source"));
            assertNull(parser.deserialize((String) null, null, String.class));
        }
    }

    @Test
    void jsonRangeValidationPrecedesTargetValidation() {
        JsonParser parser = ParserFactory.createJsonParser();
        assertThrows(IndexOutOfBoundsException.class, () -> parser.deserialize("{}", -1, 2, null, (Class<?>) null));
        assertThrows(IndexOutOfBoundsException.class, () -> parser.deserialize("{}", -1, 2, null, (Type<?>) null));
    }

    @Test
    void avroValidatesTargetBeforeOpeningAFileOrReturningAnEmptyDefault() {
        AvroParser parser = ParserFactory.createAvroParser();
        assertThrows(IllegalArgumentException.class, () -> parser.deserialize(directory.resolve("missing.avro").toFile(), null, (Class<?>) null));
        assertThrows(IllegalArgumentException.class, () -> parser.deserialize("", null, (Class<?>) null));
        assertNull(parser.deserialize("", null, String.class));
    }

    @Test
    void avroReportsMalformedBase64AndUnsupportedTargets() {
        AvroParser parser = ParserFactory.createAvroParser();
        assertThrows(IllegalArgumentException.class, () -> parser.deserialize("%%%", null, Type.of(Map.class)));
        Schema schema = new Schema.Parser().parse("{\"type\":\"record\",\"name\":\"Value\",\"fields\":[{\"name\":\"value\",\"type\":\"int\"}]}");
        String encoded = parser.serialize(Map.of("value", 7), new AvroSerConfig().setSchema(schema));
        AvroDeserConfig config = new AvroDeserConfig().setSchema(schema);
        assertThrows(IllegalArgumentException.class, () -> parser.deserialize(encoded, config, String.class));
        assertEquals(Map.of("value", 7), parser.deserialize(encoded, config, Map.class));
    }

    @Test
    void jaxbChecksConfigurationBeforeTargetAndBeforeFileAccess() {
        XmlParser parser = ParserFactory.createJaxbParser();
        XmlDeserConfig config = new XmlDeserConfig().setIgnoredPropNames(Map.of(String.class, Set.of("value")));
        assertThrows(ParsingException.class, () -> parser.deserialize("", config, (Class<?>) null));
        assertThrows(ParsingException.class, () -> parser.deserialize(directory.resolve("missing.xml").toFile(), config, String.class));
        assertThrows(IllegalArgumentException.class, () -> parser.deserialize("", null, (Class<?>) null));
    }

    @Test
    void kryoRejectsMissingResourcesAndPreservesClassAndObjectMode() {
        KryoParser parser = ParserFactory.createKryoParser();
        assertThrows(IllegalArgumentException.class, () -> parser.serialize("value", null, (OutputStream) null));
        assertThrows(IllegalArgumentException.class, () -> parser.deserialize((ByteArrayInputStream) null, null, (Class<?>) null));
        assertThrows(KryoException.class, () -> parser.deserialize("", null, String.class));
        assertNull(parser.deserialize(parser.serialize(null), null, (Class<?>) null));
    }

    @Test
    void excelValidatesInSignatureOrderWithoutWriting() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        assertTrue(assertThrows(IllegalArgumentException.class, () -> ExcelUtil.writeRowsToSheet(null, null, null, sheet -> {
        }, output, null)).getMessage().contains("sheetName"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> ExcelUtil.writeRowsToSheet("Data", List.of(), List.of(), null, (Path) null)).getMessage()
                .contains("sheetSetter"));
        assertEquals(0, output.size());
    }

    @Test
    void typeRegistrationValidatesFirstParameterFirst() {
        assertTrue(assertThrows(IllegalArgumentException.class, () -> TypeFactory.registerType("   ", (Type<?>) null)).getMessage().contains("typeName"));
    }

    @Test
    void poolValidatesConfigurationInOrderAndClosedStateBeforeArguments() {
        assertTrue(assertThrows(IllegalArgumentException.class, () -> PoolFactory.<Poolable> createObjectPool(-1, -1, null, -1, null)).getMessage()
                .contains("capacity"));
        assertTrue(
                assertThrows(IllegalArgumentException.class, () -> PoolFactory.<Poolable> createObjectPool(1, 0, null, true, Float.NaN, -1, null)).getMessage()
                        .contains("balanceFactor"));
        ObjectPool<Poolable> pool = PoolFactory.createObjectPool(1, 0, null);
        pool.close();
        assertThrows(IllegalStateException.class, () -> pool.add(null));
    }

    @Test
    void jaxbForwardingOverloadsPreserveResourcesAndCanRetryAfterValidationFailure() {
        XmlParser parser = ParserFactory.createJaxbParser();
        XmlSerConfig invalidConfig = new XmlSerConfig().setIgnoredPropNames(Map.of(XmlValue.class, Set.of("text")));
        class TrackingOutput extends ByteArrayOutputStream {
            boolean closed;

            @Override
            public void close() {
                closed = true;
            }
        }
        TrackingOutput output = new TrackingOutput();
        XmlValue value = new XmlValue();
        value.text = "a<&>b";
        assertThrows(ParsingException.class, () -> parser.serialize(value, invalidConfig, output));
        assertEquals(0, output.size());
        parser.serialize(value, null, output);
        assertFalse(output.closed);

        class TrackingInput extends ByteArrayInputStream {
            boolean closed;

            TrackingInput(byte[] bytes) {
                super(bytes);
            }

            @Override
            public void close() {
                closed = true;
            }
        }
        TrackingInput input = new TrackingInput(output.toByteArray());
        int available = input.available();
        assertThrows(IllegalArgumentException.class, () -> parser.deserialize(input, null, (Class<?>) null));
        assertEquals(available, input.available());
        assertEquals(value.text, parser.deserialize(input, null, XmlValue.class).text);
        assertFalse(input.closed);

        StringReader reader = new StringReader(output.toString(java.nio.charset.StandardCharsets.UTF_8));
        assertThrows(IllegalArgumentException.class, () -> parser.deserialize(reader, null, (Class<?>) null));
        assertEquals(value.text, parser.deserialize(reader, null, XmlValue.class).text);
    }

    @Test
    void classTargetsAreValidatedBeforeReadingAcrossTextParsers() {
        for (Parser<?, ?> parser : List.of(ParserFactory.createJsonParser(), ParserFactory.createXmlParser(), ParserFactory.createAbacusXmlParser(),
                new XmlParserImpl(XmlParserType.DOM), ParserFactory.createJaxbParser(), ParserFactory.createAvroParser())) {
            InputStream source = new InputStream() {
                @Override
                public int read() {
                    throw new AssertionError("Invalid target must not consume the source");
                }

                @Override
                public void close() {
                    throw new AssertionError("Validation must preserve caller ownership");
                }
            };
            assertThrows(IllegalArgumentException.class, () -> parser.deserialize(source, null, (Class<?>) null));
        }
    }

    @XmlRootElement
    public static class XmlValue {
        public String text;
    }
}
