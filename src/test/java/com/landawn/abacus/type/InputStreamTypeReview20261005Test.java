package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.parser.JsonSerConfig;
import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.parser.XmlSerConfig;
import com.landawn.abacus.util.BufferedJsonWriter;
import com.landawn.abacus.util.BufferedXmlWriter;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.Objectory;

/**
 * Code review 2026-10-05: {@code InputStreamType.serializeTo} now streams the content in buffer-sized chunks instead of
 * first reading it into one String. JSON/XML writers escape a surrogate they see unpaired, so a surrogate pair whose high
 * half ended one chunk was written as two escaped halves; its spelling depended on where the chunk boundary fell. A
 * trailing high surrogate is now carried into the next chunk, and the output is again exactly what writing the whole
 * value at once produces.
 */
@Tag("unit")
class InputStreamTypeReview20261005Test extends TestBase {
    private static final String EMOJI = "😀";

    private final Type<InputStream> type = Type.of(InputStream.class);

    private static int chunkSize() throws ReflectiveOperationException {
        // serializeTo reads into Objectory.createCharArrayBuffer(), whose length is Objectory.BUFFER_SIZE.
        final Field field = Objectory.class.getDeclaredField("BUFFER_SIZE");
        field.setAccessible(true);
        return field.getInt(null);
    }

    private interface WriterFactory {
        CharacterWriter create();
    }

    private static final WriterFactory JSON = Objectory::createBufferedJsonWriter;
    private static final WriterFactory XML = Objectory::createBufferedXmlWriter;

    private String streamed(final WriterFactory factory, final String content, final JsonXmlSerConfig<?> config) throws IOException {
        final CharacterWriter writer = factory.create();

        try {
            type.serializeTo(writer, new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)), config);
            return writer.toString();
        } finally {
            recycle(writer);
        }
    }

    /** The value written in one piece, as serializeTo did before it streamed: the reference output. */
    private static String atOnce(final WriterFactory factory, final String content, final JsonXmlSerConfig<?> config) throws IOException {
        final CharacterWriter writer = factory.create();

        try {
            final char quotation = config == null ? 0 : config.getStringQuotation();

            if (quotation == 0) {
                writer.writeCharacter(content);
            } else {
                writer.write(quotation);
                Utils.writeStringContent(writer, content, quotation);
                writer.write(quotation);
            }

            return writer.toString();
        } finally {
            recycle(writer);
        }
    }

    private static void recycle(final CharacterWriter writer) {
        if (writer instanceof BufferedJsonWriter json) {
            Objectory.recycle(json);
        } else if (writer instanceof BufferedXmlWriter xml) {
            Objectory.recycle(xml);
        }
    }

    @SuppressWarnings("deprecation") // setStringQuotation selects the single-quote spelling under test
    private void assertSameAsWrittenAtOnce(final String content, final String what) throws IOException {
        final JsonXmlSerConfig<?>[] configs = { null, JsonSerConfig.create(), JsonSerConfig.create().setStringQuotation('\''), XmlSerConfig.create() };

        for (final JsonXmlSerConfig<?> config : configs) {
            final WriterFactory factory = config instanceof XmlSerConfig ? XML : JSON;
            final String expected = atOnce(factory, content, config);
            final String actual = streamed(factory, content, config);

            assertEquals(expected.length(), actual.length(), what + " length, quotation=" + (config == null ? "none" : (int) config.getStringQuotation()));
            assertEquals(expected, actual, what + ", quotation=" + (config == null ? "none" : (int) config.getStringQuotation()));
        }
    }

    @Test
    void surrogatePairEndingTheFirstChunkIsWrittenAsOneCharacter() throws Exception {
        final int chunk = chunkSize();
        final String content = "x".repeat(chunk - 1) + EMOJI + "y";

        assertTrue(Character.isHighSurrogate(content.charAt(chunk - 1)), "the pair must straddle the first chunk boundary");
        assertSameAsWrittenAtOnce(content, "pair straddling the first boundary");

        final String json = streamed(JSON, content, JsonSerConfig.create());
        assertTrue(json.contains(EMOJI), "the pair must be written raw, not as two escaped halves");
    }

    @Test
    void denseSurrogatePairsMatchForEveryBoundaryAlignment() throws Exception {
        final int chunk = chunkSize();
        final String pairs = EMOJI.repeat(chunk + chunk / 2 + 3);

        // Both parities, so pairs straddle boundaries whichever chunk offsets the carried character produces.
        assertSameAsWrittenAtOnce(pairs, "even-aligned pairs");
        assertSameAsWrittenAtOnce("a" + pairs, "odd-aligned pairs");
        assertSameAsWrittenAtOnce("'" + pairs + "'" + pairs, "pairs around single quotes");
    }

    @Test
    void contentWithoutSurrogatesAndSmallValuesAreUnchanged() throws Exception {
        final int chunk = chunkSize();

        assertSameAsWrittenAtOnce("", "empty");
        assertSameAsWrittenAtOnce(EMOJI, "single pair");
        assertSameAsWrittenAtOnce("plain \"quoted\" 'single' \\ \n\t <xml> & text", "escapes");
        assertSameAsWrittenAtOnce("z".repeat(chunk * 2 + 7), "multi-chunk ASCII");
        assertSameAsWrittenAtOnce("z".repeat(chunk) + EMOJI, "pair starting the second chunk");
    }

    @Test
    void readFailureIsReportedAsUncheckedIOException() {
        final InputStream failing = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("read failure");
            }
        };

        final BufferedJsonWriter writer = Objectory.createBufferedJsonWriter();

        try {
            final UncheckedIOException e = assertThrows(UncheckedIOException.class, () -> type.serializeTo(writer, failing, JsonSerConfig.create()));
            assertInstanceOf(IOException.class, e.getCause());
        } finally {
            Objectory.recycle(writer);
        }
    }

    @Test
    void readFailureAfterAChunkLeavesThePartialOutputDocumentedOnSerializeTo() throws Exception {
        final int chunk = chunkSize();
        final byte[] content = "x".repeat(chunk + 10).getBytes(StandardCharsets.UTF_8);

        // Delivers one full chunk and a little more, then fails: the first chunk is already written when the read fails.
        final InputStream failsAfterContent = new InputStream() {
            private int position;

            @Override
            public int read() throws IOException {
                final byte[] one = new byte[1];
                return read(one, 0, 1) < 0 ? -1 : one[0] & 0xFF;
            }

            @Override
            public int read(final byte[] b, final int off, final int len) throws IOException {
                if (position >= content.length) {
                    throw new IOException("connection reset");
                }

                final int n = Math.min(len, content.length - position);
                System.arraycopy(content, position, b, off, n);
                position += n;
                return n;
            }
        };

        final BufferedJsonWriter writer = Objectory.createBufferedJsonWriter();

        try {
            assertThrows(UncheckedIOException.class, () -> type.serializeTo(writer, failsAfterContent, JsonSerConfig.create()));

            final String partial = writer.toString();
            assertEquals("\"" + "x".repeat(chunk), partial, "the opening quotation and the first chunk stay written; the value is not closed");
        } finally {
            Objectory.recycle(writer);
        }
    }
}
