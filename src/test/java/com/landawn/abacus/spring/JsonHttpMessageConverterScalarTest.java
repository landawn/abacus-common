package com.landawn.abacus.spring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.ParsingException;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.parser.JsonDeserConfig;
import com.landawn.abacus.parser.JsonSerConfig;

public class JsonHttpMessageConverterScalarTest extends TestBase {
    private final JsonHttpMessageConverter converter = new JsonHttpMessageConverter();

    private Object read(final String json, final Class<?> type) {
        return converter.readInternal(type, new StringReader(json));
    }

    private String write(final Object value) {
        final StringWriter writer = new StringWriter();
        converter.writeInternal(value, value == null ? Object.class : value.getClass(), writer);
        return writer.toString();
    }

    @Test
    public void stringsUseJsonEscapingAndRoundTripUnicode() {
        final String value = "\u540D\uD83D\uDE80\"\\/\b\f\n\r\t\u0000";
        final String json = write(value);
        assertTrue(json.startsWith("\""));
        assertTrue(json.endsWith("\""));
        assertFalse(json.contains("\n"));
        assertEquals(value, read(json, String.class));
        assertEquals(value, read(json, Object.class));
        assertEquals("\u540D\uD83D\uDE80", read(" \t\"\\u540D\\uD83D\\uDE80\"\r\n", String.class));
        assertEquals("/", read("\"\\/\"", String.class));
    }

    @Test
    public void nullAndEmptyStringAreDistinct() {
        assertEquals("null", write(null));
        assertEquals("\"\"", write(""));
        assertNull(read("null", String.class));
        assertNull(read("null", Object.class));
        assertNull(read("null", Integer.class));
        assertEquals(0, read("null", int.class));
        assertEquals("", read("\"\"", String.class));
        assertNull(read("null", Map.class));
        assertNull(read("null", List.class));
        assertNull(read("null", int[].class));
    }

    @Test
    public void scalarConfigurationIsHonoredWithoutMutatingIt() {
        final JsonDeserConfig config = new JsonDeserConfig().setIgnoreNullOrEmpty(true).setReadNullToEmpty(true).setElementType(String.class);
        final JsonHttpMessageConverter custom = new JsonHttpMessageConverter(new JsonSerConfig(), config);
        assertEquals("", custom.readInternal(String.class, new StringReader("null")));
        assertEquals("", custom.readInternal(String.class, new StringReader("\"\"")));
        assertInstanceOf(Number.class, custom.readInternal(Object.class, new StringReader("123")));
        assertTrue(config.isIgnoreNullOrEmpty());
        assertTrue(config.isReadNullToEmpty());
        assertEquals(String.class, config.getElementType().javaType());
        assertEquals(List.of("1"), custom.readInternal(Object.class, new StringReader("[null,1]")));
    }

    @Test
    public void numericBoundariesAndSignedZeroRoundTrip() {
        for (final Object value : List.of(Integer.MIN_VALUE, Long.MIN_VALUE, Long.MAX_VALUE, new BigInteger("1234567890123456789012345678901234567890"),
                new BigDecimal("1.2345678901234567890123456789E+400"), Double.MIN_VALUE, Double.MAX_VALUE, -0.0d, -0.0f)) {
            assertEquals(value, read(write(value), value.getClass()));
        }
        assertEquals(Double.doubleToRawLongBits(-0.0d), Double.doubleToRawLongBits((Double) read("-0.0", Double.class)));
        assertEquals(true, read("true", boolean.class));
        assertEquals(false, read("false", Object.class));
        assertEquals("true", write(true));
    }

    @Test
    public void stringLikeScalarsUseQuotedJson() {
        for (final Object value : List.of('"', LocalDate.of(2026, 9, 5), java.time.DayOfWeek.MONDAY, UUID.fromString("12345678-1234-1234-1234-123456789abc"))) {
            final String json = write(value);
            assertTrue(json.startsWith("\""));
            assertEquals(value, read(json, value.getClass()));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { "", " \t\r\n", "hello", "'hello'", "\"a\" \"b\"", "true false", "1 2", "1,", ",1", "1]true", "\"bad\\q\"", "\"bad\\u12xz\"",
            "\"bad\nline\"", "\"unterminated", "\"escaped\\\"", "01", "+1", "1.", ".1", "1e", "NaN", "Infinity", "[]", "{}", "\u0001true", "true\u0001" })
    public void malformedScalarInputIsRejected(final String json) {
        assertThrows(ParsingException.class, () -> read(json, String.class));
    }

    @Test
    public void nonfiniteNumbersFailBeforeWritingAnything() {
        for (final Number number : List.of(Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Float.NaN, Float.POSITIVE_INFINITY)) {
            final StringWriter writer = new StringWriter();
            assertThrows(ParsingException.class, () -> converter.writeInternal(number, number.getClass(), writer));
            assertEquals("", writer.toString());
        }
    }

    @Test
    public void largeStringsDoNotUseRecursiveValidation() {
        final String value = "x\\\"\uD83D\uDE80".repeat(20000);
        assertEquals(value, read(write(value), String.class));
    }

    @Test
    public void scalarReadAndWriteLeaveCallerResourcesOpen() throws IOException {
        final boolean[] closed = { false, false };
        final Reader reader = new StringReader("\"value\"") {
            @Override
            public void close() {
                closed[0] = true;
            }
        };
        final Writer writer = new StringWriter() {
            @Override
            public void close() {
                closed[1] = true;
            }
        };
        assertEquals("value", converter.readInternal(String.class, reader));
        converter.writeInternal("value", String.class, writer);
        assertFalse(closed[0]);
        assertFalse(closed[1]);
        reader.close();
        writer.close();
        assertTrue(closed[0]);
        assertTrue(closed[1]);
    }

    @Test
    public void writeFailurePreservesIoCauseAndDoesNotCloseWriter() {
        final IOException cause = new IOException("write failure");
        final boolean[] closed = { false };
        final Writer writer = new Writer() {
            @Override
            public void write(final char[] chars, final int offset, final int length) throws IOException {
                throw cause;
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
                closed[0] = true;
            }
        };
        final UncheckedIOException failure = assertThrows(UncheckedIOException.class, () -> converter.writeInternal("value", String.class, writer));
        assertEquals(cause, failure.getCause());
        assertFalse(closed[0]);
    }

    @Test
    public void structuredRootsStillUseTheConfiguredParser() {
        assertEquals(Map.of("value", List.of(1, 2)), read("{\"value\":[1,2]}", Object.class));
        assertEquals(List.of(Map.of("value", 1)), read("[{\"value\":1}]", Object.class));
        assertEquals(List.of(1, 2), read("[1,2]", List.class));
    }

    @Test
    public void publicHttpPathUsesJsonScalarsAndSpringExceptions() throws IOException {
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        final HttpHeaders headers = new HttpHeaders();
        final HttpOutputMessage message = new HttpOutputMessage() {
            @Override
            public ByteArrayOutputStream getBody() {
                return output;
            }

            @Override
            public HttpHeaders getHeaders() {
                return headers;
            }
        };
        converter.write("\u540D\uD83D\uDE80", MediaType.APPLICATION_JSON, message);
        assertEquals("\"\u540D\uD83D\uDE80\"", output.toString(StandardCharsets.UTF_8));
        final HttpInputMessage input = new HttpInputMessage() {
            @Override
            public ByteArrayInputStream getBody() {
                return new ByteArrayInputStream(output.toByteArray());
            }

            @Override
            public HttpHeaders getHeaders() {
                return headers;
            }
        };
        assertEquals("\u540D\uD83D\uDE80", converter.read(String.class, input));
        output.reset();
        output.writeBytes("NaN".getBytes(StandardCharsets.UTF_8));
        assertThrows(HttpMessageNotReadableException.class, () -> converter.read(Double.class, input));
        assertThrows(HttpMessageNotWritableException.class, () -> converter.write(Double.NaN, MediaType.APPLICATION_JSON, message));
    }
}
