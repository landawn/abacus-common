package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.BufferedJsonWriter;
import com.landawn.abacus.util.Objectory;

public class AsciiStreamCharsetTest extends TestBase {
    private static final String[] ASCII_TYPES = { "AsciiStream", "ClobAsciiStream" };

    @Test
    public void nullAndEmptyRemainDistinct() throws Exception {
        for (String name : ASCII_TYPES) {
            Type<InputStream> type = TypeFactory.getType(name);
            assertNull(type.valueOf((String) null));
            assertTextPaths(type, null, null);
            assertArrayEquals(new byte[0], type.valueOf("").readAllBytes());
            assertTextPaths(type, new byte[0], "");
        }
    }

    @Test
    public void everyAsciiByteRoundTripsAcrossTextPaths() throws Exception {
        byte[] bytes = new byte[128];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) i;
        }
        String text = new String(bytes, StandardCharsets.US_ASCII);
        for (String name : ASCII_TYPES) {
            Type<InputStream> type = TypeFactory.getType(name);
            assertArrayEquals(bytes, type.valueOf(text).readAllBytes());
            assertTextPaths(type, bytes, text);
        }
    }

    @Test
    public void nonAsciiAndMalformedSurrogatesUseEncoderReplacement() throws Exception {
        for (String name : ASCII_TYPES) {
            Type<InputStream> type = TypeFactory.getType(name);
            for (String text : new String[] { "\u00E9", "\uD83D\uDE00", "\uD800", "\uDC00", "\uFFFF" }) {
                byte[] bytes = type.valueOf(text).readAllBytes();
                assertArrayEquals(new byte[] { '?' }, bytes);
                assertTextPaths(type, bytes, "?");
            }
        }
    }

    @Test
    public void nonAsciiBytesUseDecoderReplacementConsistently() throws Exception {
        byte[] bytes = { 0, 127, (byte) 128, (byte) 255, (byte) 0xC3, (byte) 0xA9 };
        for (String name : ASCII_TYPES) {
            assertTextPaths(TypeFactory.getType(name), bytes, "\u0000\u007F\uFFFD\uFFFD\uFFFD\uFFFD");
        }
    }

    @Test
    public void otherStreamHandlersRetainUtf8() throws Exception {
        String text = "\u00E9\uD83D\uDE00\u0000\r\n";
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        for (String name : new String[] { "InputStream", "BinaryStream", "BlobInputStream", "java.io.ByteArrayInputStream" }) {
            Type<InputStream> type = TypeFactory.getType(name);
            assertArrayEquals(bytes, type.valueOf(text).readAllBytes());
            assertTextPaths(type, bytes, text);
        }
    }

    private static void assertTextPaths(Type<InputStream> type, byte[] bytes, String expected) throws Exception {
        assertEquals(expected, type.stringOf(stream(bytes)));
        StringBuilder builder = new StringBuilder();
        type.appendTo(builder, stream(bytes));
        assertEquals(expected == null ? "null" : expected, builder.toString());
        StringWriter writer = new StringWriter();
        type.appendTo(writer, stream(bytes));
        assertEquals(expected == null ? "null" : expected, writer.toString());
        BufferedJsonWriter json = Objectory.createBufferedJsonWriter();
        try {
            type.serializeTo(json, stream(bytes), Utils.jsc);
            assertEquals(Utils.jsonParser.serialize(Collections.singletonList(expected), Utils.jsc), "[" + json + "]");
        } finally {
            Objectory.recycle(json);
        }
    }

    private static InputStream stream(byte[] bytes) {
        return bytes == null ? null : new ByteArrayInputStream(bytes);
    }
}
