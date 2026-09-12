package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.type.Type;

public class NullValidationRestartParserTest extends TestBase {
    @Test
    public void typeDescriptorsAreRejectedBeforeSourcesAreOpenedOrRead() {
        for (final Parser<?, ?> parser : new Parser<?, ?>[] { ParserFactory.createJaxbParser(), ParserFactory.createKryoParser(),
                ParserFactory.createXmlParser(), ParserFactory.createAbacusXmlParser(), new XmlParserImpl(XmlParserType.DOM) }) {
            final InputStream unreadableStream = new InputStream() {
                @Override
                public int read() {
                    throw new AssertionError("Source must not be read when targetType is null");
                }
            };
            final Reader unreadableReader = new Reader() {
                @Override
                public int read(final char[] buffer, final int offset, final int length) {
                    throw new AssertionError("Source must not be read when targetType is null");
                }

                @Override
                public void close() {
                    throw new AssertionError("Source must not be closed when targetType is null");
                }
            };

            assertArgument("targetType", () -> parser.deserialize("unused", null, (Type<String>) null));
            assertArgument("targetType", () -> parser.deserialize((File) null, null, (Type<String>) null));
            assertArgument("targetType", () -> parser.deserialize(unreadableStream, null, (Type<String>) null));
            assertArgument("targetType", () -> parser.deserialize(unreadableReader, null, (Type<String>) null));
            assertArgument("targetType", () -> parser.deserialize("unused", (Type<String>) null));
            assertArgument("targetType", () -> parser.deserialize((File) null, (Type<String>) null));
            assertArgument("targetType", () -> parser.deserialize(unreadableStream, (Type<String>) null));
            assertArgument("targetType", () -> parser.deserialize(unreadableReader, (Type<String>) null));
        }
    }

    @Test
    public void xmlSourcesAndOptionalStringDefaultsRemainConsistent() {
        for (final XmlParser parser : new XmlParser[] { ParserFactory.createXmlParser(), ParserFactory.createAbacusXmlParser(),
                new XmlParserImpl(XmlParserType.DOM) }) {
            assertArgument("source", () -> parser.deserialize((InputStream) null, Type.of(String.class)));
            assertArgument("source", () -> parser.deserialize((Reader) null, Type.of(String.class)));
            assertArgument("source", () -> parser.deserialize((File) null, Type.of(String.class)));
            assertNull(parser.deserialize((String) null, Type.of(String.class)));
            assertNull(parser.deserialize("", Type.of(String.class)));
        }
    }

    @Test
    public void avroRequiredTypesAndSourcesUseArgumentValidation() {
        final AvroParser parser = ParserFactory.createAvroParser();
        final InputStream unreadable = new InputStream() {
            @Override public int read() { throw new AssertionError("Invalid target must be rejected before reading"); }
        };
        assertArgument("targetType", () -> parser.deserialize("", (Type<String>) null));
        assertArgument("targetType", () -> parser.deserialize((File) null, (Type<String>) null));
        assertArgument("targetType", () -> parser.deserialize(unreadable, (Type<String>) null));
        assertArgument("source", () -> parser.deserialize((InputStream) null, Type.of(String.class)));
        assertNull(parser.deserialize("", Type.of(String.class)));
        assertThrowsExactly(UnsupportedOperationException.class, () -> parser.deserialize((Reader) null, (Type<String>) null));
    }

    @Test
    public void jaxbSourcesAreValidatedForClassAndTypeOverloads() {
        final XmlParser parser = ParserFactory.createJaxbParser();
        assertArgument("source", () -> parser.deserialize((InputStream) null, null, String.class));
        assertArgument("source", () -> parser.deserialize((Reader) null, null, String.class));
        assertArgument("source", () -> parser.deserialize((InputStream) null, null, Type.of(String.class)));
        assertArgument("source", () -> parser.deserialize((Reader) null, null, Type.of(String.class)));
        assertArgument("source", () -> parser.deserialize((InputStream) null, String.class));
        assertArgument("source", () -> parser.deserialize((Reader) null, Type.of(String.class)));
    }

    @Test
    public void jaxbNullStringSourceStillReturnsTargetDefault() {
        final XmlParser parser = ParserFactory.createJaxbParser();
        assertNull(parser.deserialize((String) null, String.class));
        assertNull(parser.deserialize((String) null, Type.of(String.class)));
    }

    @Test
    public void kryoNullClassStillReadsEmbeddedClassMetadata() {
        final KryoParser parser = ParserFactory.createKryoParser();
        final byte[] encoded = parser.encode("embedded");
        final String decoded = parser.deserialize(new ByteArrayInputStream(encoded), null, (Class<String>) null);
        assertEquals("embedded", decoded);
    }

    @Test
    public void kryoWriterRejectsNullAndRetainsValidSerialization() {
        final KryoParser parser = ParserFactory.createKryoParser();
        assertArgument("output", () -> parser.serialize("value", null, (Writer) null));
        assertArgument("output", () -> parser.serialize(null, (Writer) null));
        final StringWriter writer = new StringWriter();
        parser.serialize("value", null, writer);
        assertEquals("value", parser.deserialize(new StringReader(writer.toString()), null, String.class));
    }

    private static void assertArgument(final String parameter, final Executable action) {
        assertTrue(assertThrowsExactly(IllegalArgumentException.class, action).getMessage().contains(parameter));
    }
}
