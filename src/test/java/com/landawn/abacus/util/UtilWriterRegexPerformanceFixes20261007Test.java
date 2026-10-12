package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.Writer;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Spliterator;
import java.util.concurrent.TimeUnit;
import java.util.function.IntConsumer;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("unit")
class UtilWriterRegexPerformanceFixes20261007Test extends TestBase {
    @Test
    void decimalWritersPreserveAllShortsIntegerBoundariesAndRandomLongs() throws Exception {
        final StringBuilder expected = new StringBuilder();
        try (final BufferedWriter writer = new BufferedWriter()) {
            for (int value = Short.MIN_VALUE; value <= Short.MAX_VALUE; value++) {
                writer.write((short) value);
                writer.write(',');
                expected.append(value).append(',');
            }
            for (final int value : new int[] { Integer.MIN_VALUE, Integer.MAX_VALUE, -1001, -1000, 10000, 10001 }) {
                writer.writeInt(value);
                writer.write(',');
                expected.append(value).append(',');
            }
            final Random random = new Random(719);
            for (int i = 0; i < 2000; i++) {
                final long value = i == 0 ? Long.MIN_VALUE : i == 1 ? Long.MAX_VALUE : random.nextLong();
                writer.write(value);
                writer.write(',');
                expected.append(value).append(',');
            }
            assertEquals(expected.toString(), writer.toString());
        }
    }

    @Test
    void numericIoHelpersUseTheSameTextAndStillRejectClosedWriters() throws Exception {
        try (final BufferedJsonWriter writer = new BufferedJsonWriter()) {
            IOUtil.write(1234567, writer);
            writer.write(',');
            IOUtil.write(Long.MIN_VALUE, writer);
            writer.write(',');
            IOUtil.write(Short.MIN_VALUE, writer);
            assertEquals("1234567,-9223372036854775808,-32768", writer.toString());
            writer.close();
            assertThrows(IOException.class, () -> writer.writeInt(1));
            assertThrows(IOException.class, () -> writer.writeInt(Integer.MIN_VALUE));
            assertThrows(IOException.class, () -> writer.write(Long.MIN_VALUE));
            assertThrows(IOException.class, () -> writer.writeCharacter('\ud800'));
        }
    }

    @Test
    void scalarBufferSpillPreservesDestinationCallbacksAndFailureIdentity() throws Exception {
        final List<String> events = new ArrayList<>();
        final IOException failure = new IOException("scalar write failed");
        final Writer destination = new Writer() {
            @Override
            public void write(final char[] chars, final int offset, final int length) {
                events.add("chars:" + length);
            }

            @Override
            public void write(final String text, final int offset, final int length) throws IOException {
                events.add(text.substring(offset, offset + length));
                throw failure;
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
        try (final BufferedJsonWriter writer = new BufferedJsonWriter(destination)) {
            writer.write("x".repeat(Objectory.BUFFER_SIZE - 2));
            assertSame(failure, assertThrows(IOException.class, () -> writer.writeInt(1234567)));
            assertEquals(List.of("chars:" + (Objectory.BUFFER_SIZE - 2), "1234567"), events);
            events.clear();
            writer.write("x".repeat(Objectory.BUFFER_SIZE - 2));
            assertSame(failure, assertThrows(IOException.class, () -> writer.writeCharacter('\ud800')));
            assertEquals(List.of("chars:" + (Objectory.BUFFER_SIZE - 2), "\\ud800"), events);
        }
    }

    @Test
    void isolatedSurrogatesAndSplitPairsPreserveAllWriterOverloads() throws Exception {
        final StringBuilder expected = new StringBuilder();
        try (final BufferedJsonWriter writer = new BufferedJsonWriter()) {
            for (int ch = Character.MIN_SURROGATE; ch <= Character.MAX_SURROGATE; ch++) {
                writer.writeCharacter((char) ch);
                expected.append(String.format("\\u%04x", ch));
            }
            assertEquals(expected.toString(), writer.toString());
        }

        final String input = "x\ud83d\ude00\ud800y\udc00z";
        final String escaped = "x\ud83d\ude00\\ud800y\\udc00z";
        for (final boolean array : new boolean[] { false, true }) {
            final java.io.StringWriter destination = new java.io.StringWriter();
            try (final BufferedJsonWriter writer = new BufferedJsonWriter(destination)) {
                if (array) {
                    writer.writeCharacter(input.toCharArray());
                    writer.writeCharacter(input.toCharArray(), 1, 1);
                    writer.writeCharacter(input.toCharArray(), 2, 1);
                } else {
                    writer.writeCharacter(input);
                    writer.writeCharacter(input, 1, 1);
                    writer.writeCharacter(input, 2, 1);
                }
            }
            assertEquals(escaped + "\\ud83d\\ude00", destination.toString());
        }
    }

    @Test
    void regexIndicesMatchJdkForEmptyZeroWidthUnicodeAndParallelTraversal() {
        final String[] inputs = { "", "abc123def45", "a\na\r\nb", "\ud83d\ude00a\ud83d\ude00", "aaaa", "\ud800x\udc00" };
        final String[] expressions = { "", "a", "a*", "(?=a)", "^|$", "(?m)^|$", ".", "(a)|(b)", "(?<=a)a", "\\d+" };
        for (final String input : inputs) {
            for (final String expression : expressions) {
                final Pattern pattern = Pattern.compile(expression);
                final int[] expected = pattern.matcher(input).results().mapToInt(r -> r.start()).toArray();
                assertArrayEquals(expected, RegExUtil.matchIndices(input, pattern).toArray());
                assertArrayEquals(expected, RegExUtil.matchIndices(input, pattern).parallel().toArray());
                assertEquals(expected.length == 0 ? -1 : expected[0], RegExUtil.matchIndices(input, pattern).findFirst().orElse(-1));
            }
        }
        final String many = "a".repeat(8192);
        assertArrayEquals(Pattern.compile("a").matcher(many).results().mapToInt(r -> r.start()).toArray(),
                RegExUtil.matchIndices(many, Pattern.compile("a")).parallel().toArray());
        assertThrows(IllegalArgumentException.class, () -> RegExUtil.matchIndices(null, (Pattern) null));
        assertEquals(0, RegExUtil.matchIndices(null, Pattern.compile("a")).count());
    }

    @Test
    void regexSpliteratorPreservesValidationExhaustionAndConsumerFailureProgress() {
        final Spliterator.OfInt split = RegExUtil.matchIndices("aa", Pattern.compile("a")).spliterator();
        assertThrows(NullPointerException.class, () -> split.tryAdvance((IntConsumer) null));
        final RuntimeException failure = new RuntimeException("consumer failed");
        assertSame(failure, assertThrows(RuntimeException.class, () -> split.tryAdvance((int index) -> {
            throw failure;
        })));
        final int[] last = { -1 };
        assertTrue(split.tryAdvance((int index) -> last[0] = index));
        assertEquals(1, last[0]);
        assertFalse(split.tryAdvance((int index) -> last[0] = index));
        assertFalse(split.tryAdvance((int index) -> last[0] = index));
        assertThrows(NullPointerException.class, () -> split.tryAdvance((IntConsumer) null));
    }

    @Test
    void hotNumericAndEscapeWritesAvoidPerValueFormattedStrings() throws Exception {
        allocationCounter(); // Preserve the existing capability check before starting the child.
        // Other type tests mock BufferedJsonWriter. Mockito's inline instrumentation remains installed
        // even after clearing mocks and adds dispatch allocations to real writers. Measure in a fresh VM.
        final String executable = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        final String classpath = System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"));
        final Path output = Files.createTempFile("scalar-writer-allocation-", ".log");
        Process process = null;

        try {
            process = new ProcessBuilder(executable, "-ea", "-Xms64m", "-Xmx256m", "-XX:+UseSerialGC", "-Dfile.encoding=UTF-8", "-cp", classpath,
                    ScalarWriterAllocationProcess.class.getName()).redirectErrorStream(true).redirectOutput(output.toFile()).start();
            assertTrue(process.waitFor(30, TimeUnit.SECONDS), "scalar writer allocation probe did not terminate");
            assertEquals(0, process.exitValue(), Files.readString(output));
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
                process.waitFor(5, TimeUnit.SECONDS);
            }
            Files.deleteIfExists(output);
        }
    }

    public static final class ScalarWriterAllocationProcess {
        public static void main(final String[] args) throws Exception {
            verifyHotNumericAndEscapeWritesAllocation();
        }
    }

    private static void verifyHotNumericAndEscapeWritesAllocation() throws Exception {
        final com.sun.management.ThreadMXBean allocations = allocationCounter();
        final long thread = Thread.currentThread().threadId();
        try (final BufferedJsonWriter writer = new BufferedJsonWriter(Writer.nullWriter())) {
            for (int i = 0; i < 20_000; i++) {
                writer.writeInt(1234567);
                writer.writeCharacter('\ud800');
            }
            final long before = allocations.getThreadAllocatedBytes(thread);
            for (int i = 0; i < 10_000; i++) {
                writer.writeInt(1234567);
                writer.writeCharacter('\ud800');
            }
            final long bytes = allocations.getThreadAllocatedBytes(thread) - before;
            assertTrue(bytes < 100_000, "Buffered scalar writes allocated " + bytes + " bytes");
        }
    }

    @Test
    void matchIndexTraversalDoesNotAllocateOneMatchSnapshotPerResult() {
        final com.sun.management.ThreadMXBean allocations = allocationCounter();
        final long thread = Thread.currentThread().threadId();
        final String input = "a".repeat(4096);
        final Pattern pattern = Pattern.compile("(a)");
        for (int i = 0; i < 30; i++) {
            assertEquals(8_386_560L, RegExUtil.matchIndices(input, pattern).asLongStream().sum());
        }
        final long before = allocations.getThreadAllocatedBytes(thread);
        long checksum = 0;
        for (int i = 0; i < 30; i++) {
            checksum += RegExUtil.matchIndices(input, pattern).asLongStream().sum();
        }
        final long bytes = allocations.getThreadAllocatedBytes(thread) - before;
        assertEquals(30 * 8_386_560L, checksum);
        assertTrue(bytes < 1_000_000, "Index-only matches allocated " + bytes + " bytes");
    }

    private static com.sun.management.ThreadMXBean allocationCounter() {
        final var bean = ManagementFactory.getThreadMXBean();
        Assumptions.assumeTrue(bean instanceof com.sun.management.ThreadMXBean);
        final var allocations = (com.sun.management.ThreadMXBean) bean;
        Assumptions.assumeTrue(allocations.isThreadAllocatedMemorySupported() && allocations.isThreadAllocatedMemoryEnabled());
        return allocations;
    }
}
