package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/** Performance and memory regression contracts for URLEncodedUtil. */
@Tag("slow-test")
public class URLEncodedUtilPerformanceMemoryTest extends TestBase {

    // ---- perf review 2026-10-08 G072 end ----
    // ---- perf review 2026-10-08 G075 begin ----
    @Nested
    class G075 {
        private final java.nio.charset.Charset utf8 = java.nio.charset.StandardCharsets.UTF_8;

        // Records every call made on it; throws an IOException on call number failAtCall (never if it is negative).
        private final class RecordingAppendable implements Appendable {
            private final List<String> calls = new ArrayList<>();
            private final int failAtCall;

            RecordingAppendable(final int failAtCall) {
                this.failAtCall = failAtCall;
            }

            private void record(final String call) throws java.io.IOException {
                if (calls.size() + 1 == failAtCall) {
                    throw new java.io.IOException("call " + failAtCall);
                }

                calls.add(call);
            }

            @Override
            public Appendable append(final CharSequence csq) throws java.io.IOException {
                record("seq:" + csq);
                return this;
            }

            @Override
            public Appendable append(final CharSequence csq, final int start, final int end) throws java.io.IOException {
                record("range:" + csq.subSequence(start, end));
                return this;
            }

            @Override
            public Appendable append(final char c) throws java.io.IOException {
                record("char:" + c);
                return this;
            }
        }

        private String percentEscaped(final int... bytes) {
            final StringBuilder sb = new StringBuilder();

            for (final int b : bytes) {
                sb.append('%')
                        .append(Character.toUpperCase(Character.forDigit((b >> 4) & 0xF, 16)))
                        .append(Character.toUpperCase(Character.forDigit(b & 0xF, 16)));
            }

            return sb.toString();
        }

        private byte[] toBytes(final int... values) {
            final byte[] bytes = new byte[values.length];

            for (int i = 0; i < values.length; i++) {
                bytes[i] = (byte) values[i];
            }

            return bytes;
        }

        // The reporting JDK UTF-8 decoder, the reference for strict decoding of one escape run: its exception, or null if it accepts the bytes.
        private java.nio.charset.CharacterCodingException jdkStrictFailure(final byte[] bytes) {
            try {
                utf8.newDecoder()
                        .onMalformedInput(java.nio.charset.CodingErrorAction.REPORT)
                        .onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT)
                        .decode(java.nio.ByteBuffer.wrap(bytes));
                return null;
            } catch (final java.nio.charset.CharacterCodingException e) {
                return e;
            }
        }

        // One escape run decoded strictly and leniently must equal what the JDK decoder / new String(bytes, UTF_8) give for it.
        private void assertRunDecodedLikeJdk(final int... values) {
            final byte[] bytes = toBytes(values);
            final String query = "k=" + percentEscaped(values);
            final java.nio.charset.CharacterCodingException expectedFailure = jdkStrictFailure(bytes);

            if (expectedFailure == null) {
                assertEquals(new String(bytes, utf8), URLEncodedUtil.decode(query).get("k"), query);
            } else {
                final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> URLEncodedUtil.decode(query), query);
                assertEquals("Invalid percent-encoded byte sequence at index 0 for charset UTF-8", e.getMessage(), query);
                assertEquals(expectedFailure.getClass(), e.getCause().getClass(), query);
                assertEquals(expectedFailure.getMessage(), e.getCause().getMessage(), query);
            }

            assertEquals(new String(bytes, utf8), URLEncodedUtil.decodeLenient(query).get("k"), query);
        }

        // G075-01: every BMP character (alone, between safe characters, and inside a run with ASCII punctuation) and a spread of supplementary characters encode exactly as java.net.URLEncoder encodes them
        @Test
        void testEncode_utf8RunsMatchUrlEncoder() {
            for (int c = 0; c <= 0xFFFF; c++) {
                if (Character.isSurrogate((char) c)) {
                    continue;
                }

                final String ch = String.valueOf((char) c);

                for (final String value : new String[] { ch, "a" + ch + "b", ch + "/" + ch + ch + ":" }) {
                    assertEquals("k=" + java.net.URLEncoder.encode(value, utf8), URLEncodedUtil.encode(new Object[] { "k", value }), value);
                }
            }

            for (int codePoint = Character.MIN_SUPPLEMENTARY_CODE_POINT; codePoint <= Character.MAX_CODE_POINT; codePoint += 0xFF) {
                final String supplementary = new String(Character.toChars(codePoint));

                for (final String value : new String[] { supplementary, "x" + supplementary + "\u00e9 /" + supplementary + supplementary }) {
                    assertEquals("k=" + java.net.URLEncoder.encode(value, utf8), URLEncodedUtil.encode(new Object[] { "k", value }), value);
                }
            }

            assertEquals("k=%F4%8F%BF%BF%F0%90%80%80", URLEncodedUtil.encode(new Object[] { "k", "\udbff\udfff\ud800\udc00" }));
            assertEquals("k=%C2%80%DF%BF%E0%A0%80%EF%BF%BF", URLEncodedUtil.encode(new Object[] { "k", "\u0080\u07ff\u0800\uffff" }));
            assertEquals("a%C3%A9%20b/%E2%82%AC", encodePath("a\u00e9 b/\u20ac"));
        }

        private String encodePath(final String value) {
            final StringBuilder sb = new StringBuilder();

            try {
                URLEncodedUtil.encPath(value, utf8, sb);
            } catch (final java.io.IOException e) {
                throw new AssertionError(e);
            }

            return sb.toString();
        }

        // G075-01: a caller-supplied Appendable still receives one append(char) per output character, and a failure in the middle of a multi-byte run leaves the same prefix
        @Test
        void testEncode_utf8AppendableCallsUnchanged() {
            final String value = "x\u00e9/\u4e2d \ud83d\ude00y";
            final String encodedValue = "x%C3%A9%2F%E4%B8%AD+%F0%9F%98%80y";
            final List<String> expectedCalls = new ArrayList<>();
            expectedCalls.add("char:k");
            expectedCalls.add("seq:=");

            for (final char c : encodedValue.toCharArray()) {
                expectedCalls.add("char:" + c);
            }

            final RecordingAppendable complete = new RecordingAppendable(-1);
            URLEncodedUtil.encode(new Object[] { "k", value }, utf8, complete);
            assertEquals(expectedCalls, complete.calls);

            for (int failAt = 1; failAt <= expectedCalls.size(); failAt++) {
                final RecordingAppendable failing = new RecordingAppendable(failAt);
                final com.landawn.abacus.exception.UncheckedIOException e = assertThrows(com.landawn.abacus.exception.UncheckedIOException.class,
                        () -> URLEncodedUtil.encode(new Object[] { "k", value }, utf8, failing));
                assertEquals("call " + failAt, e.getCause().getMessage());
                assertEquals(expectedCalls.subList(0, failAt - 1), failing.calls);
            }
        }

        // G075-01: runs holding an unpaired surrogate still go through the reporting encoder - same exception, message, cause, and output written before the run
        @Test
        void testEncode_utf8MalformedRunsRejectedAsBefore() {
            final Object[][] cases = { { "ab\ud800", 2 }, { "a\ud800b", 1 }, { "\udc00", 0 }, { "x\u00e9\ud800", 1 }, { "x\ud800\u00e9", 1 },
                    { "\ud83d\ude00\udc00", 0 }, { "\ude00\ud83d", 0 }, { "ok \ud83d \ude00", 3 }, { "\u4e2d/\ud800:", 0 }, { "a\u00e9b\ud800\ud800", 3 } };

            for (final Object[] testCase : cases) {
                final String value = (String) testCase[0];
                final int runStart = (Integer) testCase[1];
                final StringBuilder sb = new StringBuilder();
                final IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                        () -> URLEncodedUtil.encode(new Object[] { "k", value }, utf8, sb), value);
                assertEquals("Input contains malformed UTF-16 or a character not representable in UTF-8 in the run beginning at index " + runStart,
                        e.getMessage(), value);
                assertEquals(java.nio.charset.MalformedInputException.class, e.getCause().getClass(), value);
                assertEquals("Input length = 1", e.getCause().getMessage(), value);
                assertEquals("k=" + java.net.URLEncoder.encode(value.substring(0, runStart), utf8), sb.toString(), value);
                assertThrows(IllegalArgumentException.class, () -> URLEncodedUtil.encode(new Object[] { "k", value }), value);
            }
        }

        // G075-02: every one- and two-byte escape run decodes (strict and lenient) exactly as the JDK UTF-8 decoder / new String do, including the rejection and its cause
        @Test
        void testDecode_utf8AllOneAndTwoByteRunsMatchJdk() {
            for (int b1 = 0; b1 < 256; b1++) {
                assertRunDecodedLikeJdk(b1);

                for (int b2 = 0; b2 < 256; b2++) {
                    assertRunDecodedLikeJdk(b1, b2);
                }
            }
        }

        // G075-02: boundary three- and four-byte runs (overlong forms, encoded surrogates, U+FFFF, U+10000, U+10FFFF and above, invalid leads, truncation, BOM) match the JDK
        @Test
        void testDecode_utf8BoundaryRunsMatchJdk() {
            final int[][] runs = { { 0xE0, 0x80, 0x80 }, { 0xE0, 0x9F, 0xBF }, { 0xE0, 0xA0, 0x80 }, { 0xE1, 0x80, 0x80 }, { 0xEC, 0xBF, 0xBF },
                    { 0xED, 0x80, 0x80 }, { 0xED, 0x9F, 0xBF }, { 0xED, 0xA0, 0x80 }, { 0xED, 0xAF, 0xBF }, { 0xED, 0xB0, 0x80 }, { 0xED, 0xBF, 0xBF },
                    { 0xEE, 0x80, 0x80 }, { 0xEF, 0xBF, 0xBF }, { 0xEF, 0xBB, 0xBF }, { 0xE4, 0xB8 }, { 0xE4, 0xB8, 0x41 }, { 0xE4, 0x41, 0xAD },
                    { 0xE4, 0xB8, 0xC0 }, { 0xF0, 0x80, 0x80, 0x80 }, { 0xF0, 0x8F, 0xBF, 0xBF }, { 0xF0, 0x90, 0x80, 0x80 }, { 0xF0, 0x9F, 0x98, 0x80 },
                    { 0xF3, 0xBF, 0xBF, 0xBF }, { 0xF4, 0x80, 0x80, 0x80 }, { 0xF4, 0x8F, 0xBF, 0xBF }, { 0xF4, 0x90, 0x80, 0x80 }, { 0xF5, 0x80, 0x80, 0x80 },
                    { 0xF7, 0xBF, 0xBF, 0xBF }, { 0xF8, 0x88, 0x80, 0x80, 0x80 }, { 0xFC, 0x84, 0x80, 0x80, 0x80, 0x80 }, { 0xF0, 0x9F, 0x98 }, { 0xF0, 0x9F },
                    { 0xF0, 0x9F, 0x98, 0x41 }, { 0xF0, 0x9F, 0x98, 0x80, 0x80 }, { 0xC3, 0xA9, 0xA9 },
                    { 0x41, 0xC3, 0xA9, 0xE4, 0xB8, 0xAD, 0xF0, 0x9F, 0x98, 0x80, 0x7F, 0x00 }, { 0xC3, 0xA9, 0xE4, 0xB8, 0xAD, 0xC3 } };

            for (final int[] run : runs) {
                assertRunDecodedLikeJdk(run);
            }

            for (int b1 = 0xE0; b1 <= 0xF7; b1++) {
                for (int b2 = 0; b2 < 256; b2 += 0x0F) {
                    assertRunDecodedLikeJdk(b1, b2, 0x80);
                    assertRunDecodedLikeJdk(b1, b2, 0xBF, 0x80);
                }
            }
        }

        // G075-02: several runs in one token, separated by literals and '+': the first malformed run is reported at its own start index with the JDK cause; lenient replaces only that run
        @Test
        void testDecode_utf8MultipleRunsPerToken() {
            assertEquals("\u00e9 \u4e2d+x\ud83d\ude00", URLEncodedUtil.decode("k=%C3%A9+%E4%B8%AD%2Bx%F0%9F%98%80").get("k"));
            assertEquals("\u00e9 \u4e2d+x\ud83d\ude00", URLEncodedUtil.decodeLenient("k=%C3%A9+%E4%B8%AD%2Bx%F0%9F%98%80").get("k"));
            assertEquals("\ufeffbom", URLEncodedUtil.decode("k=%EF%BB%BFbom").get("k"));

            final String query = "k=%C3%A9x%ED%A0%80y%E4%B8%AD";
            final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> URLEncodedUtil.decode(query));
            assertEquals("Invalid percent-encoded byte sequence at index 7 for charset UTF-8", e.getMessage());
            final java.nio.charset.CharacterCodingException expectedCause = jdkStrictFailure(toBytes(0xED, 0xA0, 0x80));
            assertNotNull(expectedCause);
            assertEquals(expectedCause.getClass(), e.getCause().getClass());
            assertEquals(expectedCause.getMessage(), e.getCause().getMessage());
            assertEquals("\u00e9x" + new String(toBytes(0xED, 0xA0, 0x80), utf8) + "y\u4e2d", URLEncodedUtil.decodeLenient(query).get("k"));

            final Map<String, String> pairs = URLEncodedUtil.decodeLenient("%C3%A9=%C3&a=%E4%B8%AD%E4%B8;%F0%9F%98%80=%C0%80%41");
            final Map<String, String> expected = new LinkedHashMap<>();
            expected.put("\u00e9", new String(toBytes(0xC3), utf8));
            expected.put("a", new String(toBytes(0xE4, 0xB8, 0xAD, 0xE4, 0xB8), utf8));
            expected.put("\ud83d\ude00", new String(toBytes(0xC0, 0x80, 0x41), utf8));
            assertEquals(expected, pairs);
        }

        // G075-02: random escape runs (mostly well-formed UTF-8, some corrupted, some arbitrary bytes) match the JDK in both modes
        @Test
        void testDecode_utf8RandomRunsMatchJdk() {
            final Random random = new Random(75);

            for (int round = 0; round < 20_000; round++) {
                final int length = 1 + random.nextInt(10);
                final byte[] bytes;

                if (random.nextBoolean()) {
                    final StringBuilder sb = new StringBuilder();

                    for (int i = 0; i < length; i++) {
                        final int kind = random.nextInt(4);
                        sb.appendCodePoint(kind == 0 ? random.nextInt(0x80)
                                : kind == 1 ? 0x80 + random.nextInt(0x780) : kind == 2 ? 0xE000 + random.nextInt(0x2000) : 0x10000 + random.nextInt(0x100000));
                    }

                    bytes = sb.toString().getBytes(utf8);

                    if (random.nextInt(3) == 0) {
                        bytes[random.nextInt(bytes.length)] = (byte) random.nextInt(256);
                    }
                } else {
                    bytes = new byte[length];
                    random.nextBytes(bytes);
                }

                final int[] values = new int[bytes.length];

                for (int i = 0; i < bytes.length; i++) {
                    values[i] = bytes[i] & 0xFF;
                }

                assertRunDecodedLikeJdk(values);
            }
        }
    }
}
