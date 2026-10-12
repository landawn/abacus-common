package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.IntList;
import com.landawn.abacus.util.LongList;

@Tag("unit")
class JsonParserReview20261011Test extends TestBase {
    private final JsonParser parser = new JsonParserImpl();

    @Test
    void unwrappedPrimitiveListKeepsItsSnapshotWhenWriterMutatesTheSource() {
        for (final boolean pretty : new boolean[] { false, true }) {
            final int[] elements = new int[100_000];
            Arrays.fill(elements, 1);
            final IntList values = IntList.of(elements);
            final JsonSerConfig config = JsonSerConfig.create().setBracketRootValue(false).setPrettyFormat(pretty);
            final String expected = parser.serialize(values, config);
            final StringBuilder output = new StringBuilder();
            final Writer destination = new Writer() {
                @Override
                public void write(final char[] buffer, final int offset, final int length) {
                    values.set(values.size() - 1, 99);
                    output.append(buffer, offset, length);
                }

                @Override
                public void flush() {
                }

                @Override
                public void close() {
                }
            };

            parser.serialize(values, config, destination);

            assertEquals(99, values.get(values.size() - 1));
            assertEquals(expected, output.toString());
        }
    }

    @Test
    void unwrappedPrimitiveListKeepsItsSnapshotWhenOutputStreamMutatesTheSource() {
        final long[] elements = new long[100_000];
        Arrays.fill(elements, 1);
        final LongList values = LongList.of(elements);
        final JsonSerConfig config = JsonSerConfig.create().setBracketRootValue(false).setWriteLongAsString(true);
        final String expected = parser.serialize(values, config);
        final ByteArrayOutputStream destination = new ByteArrayOutputStream() {
            @Override
            public synchronized void write(final byte[] buffer, final int offset, final int length) {
                values.set(values.size() - 1, 99);
                super.write(buffer, offset, length);
            }
        };

        parser.serialize(values, config, destination);

        assertEquals(99L, values.get(values.size() - 1));
        assertEquals(expected, destination.toString(StandardCharsets.UTF_8));
    }

    @Test
    void advancingPastACompleteStringDoesNotCopyItsTextIntoTheScratchBuffer() {
        final String value = "x".repeat(4094);
        for (final String suffix : new String[] { "", ",1" }) {
            final char[] scratch = new char[0];
            final JsonStreamReader reader = new JsonStreamReader(new StringReader('"' + value + '"' + suffix), new char[4096], scratch);

            assertEquals(JsonReader.START_DOUBLE_QUOTE, reader.nextToken());
            assertEquals(JsonReader.END_DOUBLE_QUOTE, reader.nextToken());
            assertEquals(value, reader.getText());
            assertSame(scratch, reader.cbuf);
            assertEquals(suffix.isEmpty() ? JsonReader.EOF : JsonReader.COMMA, reader.nextToken());
            assertFalse(reader.hasText());
            assertSame(scratch, reader.cbuf, "a completed token must not allocate a scratch copy while advancing");
        }
    }

    @Test
    void tinyReadBuffersKeepTokenTextAndValuesAcrossRefills() {
        final String source = "[\"\",\"text\",'single',\"a\\n\\u0042\\\\\\\"z\",true,false,null,1234567890123456789,-0.0,1.2300,1e-10]";
        final Type<Object> objectType = Type.of(Object.class);
        for (int bufferSize = 1; bufferSize <= 32; bufferSize++) {
            final JsonReader expected = JsonStringReader.parse(source, new char[0]);
            final JsonReader actual = JsonStreamReader.parse(new StringReader(source), new char[bufferSize], new char[0]);
            int token;
            do {
                token = expected.nextToken();
                assertEquals(token, actual.nextToken(), "buffer size " + bufferSize);
                assertEquals(expected.lastToken(), actual.lastToken());
                assertEquals(expected.hasText(), actual.hasText());
                assertEquals(expected.getText(), actual.getText());
                assertEquals(expected.readValue(objectType), actual.readValue(objectType));
            } while (token != JsonReader.EOF);
            assertFalse(actual.hasText());
        }
    }
}
