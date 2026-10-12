package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.apache.avro.Schema;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.util.ByteArrayOutputStream;

// Extends TestBase and is tagged "slow-test": performance/memory-tuning regressions stay out of
// the regular suite (AbacusCoreTestSuite excludes "slow-test"). Run this class directly when changing that code.
@Tag("slow-test")
class ExternalParserPerformanceRegressionTest extends TestBase {
    @Test
    void base64UsesOnlyTheValidPayloadAndPreservesPaddingAcrossBoundaries() throws IOException {
        for (final int length : new int[] { 0, 1, 2, 3, 1023, 1024, 1025, 8191, 8192, 8193, 16384, 16385 }) {
            final byte[] bytes = new byte[length];
            for (int i = 0; i < length; i++) {
                bytes[i] = (byte) (i * 31);
            }
            final ByteArrayOutputStream source = new ByteArrayOutputStream(length + 177);
            source.write(bytes, 0, length);
            final String expected = Base64.getEncoder().encodeToString(bytes);
            assertEquals(expected, BinaryParserSupport.encodeBase64(source));
            final TrackingWriter output = new TrackingWriter();
            BinaryParserSupport.writeBase64(source, output);
            assertEquals(expected, output.text.toString());
            assertFalse(output.closed);
            assertEquals(0, output.flushes);
            if (length >= 8192) {
                assertTrue(output.maxWrite <= 8192);
            }
        }
    }

    @Test
    void kryoWriterPreservesTextFlushOwnershipAndFailurePropagation() {
        final KryoParser parser = new KryoParser();
        final String value = "large binary payload \u03b1".repeat(1000);
        final TrackingWriter output = new TrackingWriter();
        parser.serialize(value, null, output);
        assertEquals(parser.serialize(value), output.text.toString());
        assertEquals(value, parser.deserialize(output.text.toString(), null, String.class));
        assertEquals(1, output.flushes);
        assertFalse(output.closed);

        final TrackingWriter failure = new TrackingWriter();
        failure.fail = true;
        assertThrows(UncheckedIOException.class, () -> parser.serialize(value, null, failure));
        assertFalse(failure.closed);
        assertEquals(0, failure.flushes);
    }

    @Test
    void borrowedKryoInputReusesLargePayloadWrappersWithoutRetainingPayloadOrScratch() throws Exception {
        final List<Input> otherInputs = new ArrayList<>();
        Input borrowed = KryoParser.createInput();
        while (borrowed.getClass() == Input.class) {
            otherInputs.add(borrowed);
            borrowed = KryoParser.createInput();
        }
        final String value = "\u03b1".repeat(20_000);
        final Output encoded = new Output(128, -1);
        encoded.writeString(value);
        final byte[] payload = encoded.toBytes();
        final Field chars = Input.class.getDeclaredField("chars");
        chars.setAccessible(true);
        Input recycled = null;
        try {
            borrowed.setBuffer(payload);
            assertEquals(value, borrowed.readString());
            assertTrue(((char[]) chars.get(borrowed)).length >= value.length());
            KryoParser.recycle(borrowed);
            recycled = KryoParser.createInput();
            assertSame(borrowed, recycled);
            assertNotSame(payload, recycled.getBuffer());
            assertEquals(0, recycled.getBuffer().length);
            assertNull(recycled.getInputStream());
            assertTrue(((char[]) chars.get(recycled)).length <= 8192);
        } finally {
            KryoParser.recycle(recycled == null ? borrowed : recycled);
            for (final Input input : otherInputs) {
                KryoParser.recycle(input);
            }
        }
    }

    @Test
    void avroReadsAllBeanGettersBeforeProjectionValidationOrNumericConversion() {
        final AvroParser parser = new AvroParser();
        final ObservedBean bean = new ObservedBean();
        final List<String> propertyOrder = ParserUtil.getBeanInfo(ObservedBean.class).propInfoList.stream().map(p -> p.name).toList();
        final Schema empty = new Schema.Parser().parse("{\"type\":\"record\",\"name\":\"EmptyProjection\",\"fields\":[]}");
        ObservedBean.calls.clear();
        assertThrows(IllegalArgumentException.class, () -> parser.serialize(bean, new AvroSerConfig().setSchema(empty)));
        assertEquals(propertyOrder, ObservedBean.calls);

        final Schema numeric = new Schema.Parser().parse("""
                {"type":"record","name":"NumberProjection","fields":[{"name":"a","type":"int"}]}
                """);
        ObservedBean.calls.clear();
        assertThrows(ArithmeticException.class, () -> parser.serialize(bean, new AvroSerConfig().setSchema(numeric).setIgnoreUnknownFields(true)));
        assertEquals(propertyOrder, ObservedBean.calls);

        ObservedBean.calls.clear();
        assertNotNull(parser.serialize(bean, new AvroSerConfig().setSchema(empty).setIgnoreUnknownFields(true)));
        assertEquals(propertyOrder, ObservedBean.calls);
    }

    public static final class ObservedBean {
        static final List<String> calls = new ArrayList<>();

        public long getA() {
            calls.add("a");
            return Long.MAX_VALUE;
        }

        public void setA(final long value) {
        }

        public String getB() {
            calls.add("b");
            return null;
        }

        public void setB(final String value) {
        }

        public String getC() {
            calls.add("c");
            return "value";
        }

        public void setC(final String value) {
        }
    }

    private static final class TrackingWriter extends Writer {
        final StringWriter text = new StringWriter();
        boolean closed;
        boolean fail;
        int flushes;
        int maxWrite;

        @Override
        public void write(final char[] chars, final int offset, final int length) throws IOException {
            if (fail) {
                throw new IOException("writer failure");
            }
            maxWrite = Math.max(maxWrite, length);
            text.write(chars, offset, length);
        }

        @Override
        public void flush() {
            flushes++;
        }

        @Override
        public void close() {
            closed = true;
        }
    }
}
