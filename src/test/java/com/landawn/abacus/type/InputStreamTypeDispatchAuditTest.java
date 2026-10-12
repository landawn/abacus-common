package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JsonSerConfig;
import com.landawn.abacus.util.BufferedJsonWriter;
import com.landawn.abacus.util.Objectory;

@Tag("unit")
class InputStreamTypeDispatchAuditTest extends TestBase {
    @Test
    void inheritedSerializationUsesTheSubclassTextConversionExactlyOnce() throws IOException {
        for (final char quotation : new char[] { 0, '"', '\'' }) {
            final AtomicInteger conversions = new AtomicInteger();
            final String replacement = "custom '\" \\ \n \uD83D\uDE00";
            final InputStreamType custom = new InputStreamType("AuditCustomStreamText") {
                @Override
                public String stringOf(final InputStream input) {
                    conversions.incrementAndGet();
                    return replacement;
                }
            };
            final TrackingInput source = new TrackingInput("unconverted input");
            final JsonSerConfig config = new JsonSerConfig().setStringQuotation(quotation);

            assertEquals(render(Type.of(String.class), replacement, config), render(custom, source, config));
            assertEquals(1, conversions.get(), "the inherited writer must honor the conversion hook once");
            assertEquals("unconverted input".length(), source.available(), "the conversion hook owns stream consumption");
            assertFalse(source.closed);
        }
    }

    @Test
    void nullConfigurationAndNullStreamKeepTheirOriginalDispatch() throws IOException {
        final AtomicInteger conversions = new AtomicInteger();
        final InputStreamType custom = new InputStreamType("AuditNullableStreamText") {
            @Override
            public String stringOf(final InputStream input) {
                conversions.incrementAndGet();
                return "override";
            }
        };
        assertEquals("override", render(custom, new TrackingInput("raw"), null));
        assertEquals(1, conversions.get());
        assertEquals("null", render(custom, null, new JsonSerConfig()));
        assertEquals(1, conversions.get(), "null values bypass stringOf as before");
    }

    @Test
    void customConversionFailureKeepsItsIdentityAndLeavesTheStreamOwnedByTheCaller() {
        final IllegalStateException failure = new IllegalStateException("custom stream conversion failed");
        final InputStreamType custom = new InputStreamType("AuditFailingStreamText") {
            @Override
            public String stringOf(final InputStream input) {
                throw failure;
            }
        };
        final TrackingInput source = new TrackingInput("raw");
        assertSame(failure, assertThrows(IllegalStateException.class, () -> render(custom, source, new JsonSerConfig())));
        assertEquals(3, source.available());
        assertFalse(source.closed);
    }

    @Test
    void customConversionRetainsQuotationReadsBeforeAndAfterItsCallback() throws IOException {
        final List<String> calls = new ArrayList<>();
        final JsonSerConfig config = new JsonSerConfig() {
            @Override
            public char getStringQuotation() {
                calls.add("quotation");
                return super.getStringQuotation();
            }
        }.setStringQuotation('"');
        final InputStreamType custom = new InputStreamType("AuditMutatingStreamText") {
            @Override
            public String stringOf(final InputStream input) {
                calls.add("conversion");
                config.setStringQuotation('\'');
                return "a'b";
            }
        };

        // The legacy callback may change the content/closing quotation after the opening quote is written.
        assertEquals("\"a\\'b'", render(custom, new TrackingInput("raw"), config));
        assertEquals(List.of("quotation", "quotation", "conversion", "quotation", "quotation"), calls);
    }

    private static <T> String render(final Type<T> type, final T value, final JsonSerConfig config) throws IOException {
        final StringWriter output = new StringWriter();
        final BufferedJsonWriter writer = Objectory.createBufferedJsonWriter(output);
        try {
            type.serializeTo(writer, value, config);
            writer.flush();
            return output.toString();
        } finally {
            Objectory.recycle(writer);
        }
    }

    private static final class TrackingInput extends ByteArrayInputStream {
        private boolean closed;

        private TrackingInput(final String value) {
            super(value.getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public void close() {
            closed = true;
        }
    }
}
