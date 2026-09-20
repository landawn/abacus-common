package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.xml.datatype.XMLGregorianCalendar;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.annotation.AccessFieldByMethod;

class UtilANFollowupValidationTest extends TestBase {
    static Stream<Method> arrayIoOverloads() {
        return Arrays.stream(IOUtil.class.getMethods()).filter(method -> {
            final Class<?>[] p = method.getParameterTypes();
            return (method.getName().equals("write") || method.getName().equals("append")) && p.length >= 4 && (p[0] == byte[].class || p[0] == char[].class)
                    && p[1] == int.class && p[2] == int.class;
        }).sorted(Comparator.comparing(Method::toString));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("arrayIoOverloads")
    void everyArrayIoOverloadValidatesBeforeWritingAndRetainsEmptyBehavior(final Method method, @TempDir final Path dir) throws Throwable {
        final Object source = method.getParameterTypes()[0] == byte[].class ? new byte[] { 'a', 'b', 'c' } : new char[] { 'a', 'b', 'c' };
        final File target = dir.resolve("result.txt").toFile();
        final CountingOutput output = new CountingOutput();
        final CountingWriter writer = new CountingWriter();

        for (final int[] range : new int[][] { { -1, 0 }, { 0, -1 }, { 4, 0 }, { 2, 2 }, { Integer.MAX_VALUE, 1 } }) {
            final Class<? extends Throwable> expected = range[0] < 0 || range[1] < 0 ? IllegalArgumentException.class : IndexOutOfBoundsException.class;
            assertThrows(expected, () -> invokeIo(method, source, range[0], range[1], null, null, null));
            assertThrows(expected, () -> invokeIo(method, source, range[0], range[1], target, output, writer));
        }
        assertThrows(IndexOutOfBoundsException.class, () -> invokeIo(method, null, 0, 1, target, output, writer));
        assertFalse(target.exists());
        assertEquals(0, output.size());
        assertEquals(0, writer.getBuffer().length());
        assertEquals(0, output.flushes + writer.flushes);

        Files.writeString(target.toPath(), "old");
        invokeIo(method, null, 0, 0, target, output, writer);
        final boolean fileDestination = Arrays.asList(method.getParameterTypes()).contains(File.class);
        final boolean flush = method.getParameterTypes()[method.getParameterCount() - 1] == boolean.class;
        assertEquals(fileDestination && method.getName().equals("write") ? "" : "old", Files.readString(target.toPath()));
        assertEquals(flush ? 1 : 0, output.flushes + writer.flushes);

        invokeIo(method, source, 1, 1, target, output, writer);
        if (fileDestination) {
            assertEquals(method.getName().equals("append") ? "oldb" : "b", Files.readString(target.toPath()));
        } else {
            assertEquals("b", output.toString(StandardCharsets.UTF_8) + writer);
        }
        assertEquals(0, output.closes + writer.closes, "Caller-owned destinations stay open");
    }

    static Stream<Method> streamIoOverloads() {
        return Arrays.stream(IOUtil.class.getMethods()).filter(method -> {
            final Class<?>[] p = method.getParameterTypes();
            return (method.getName().equals("write") || method.getName().equals("append")) && p.length >= 4
                    && (p[0] == File.class || p[0] == InputStream.class || p[0] == Reader.class) && p[1] == long.class && p[2] == long.class;
        }).sorted(Comparator.comparing(Method::toString));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("streamIoOverloads")
    void everyStreamIoOverloadRejectsArgumentsBeforeConsumingOrOpening(final Method method, @TempDir final Path dir) throws Throwable {
        final File sourceFile = dir.resolve("source.txt").toFile();
        Files.writeString(sourceFile.toPath(), "abc");
        final ByteArrayInputStream input = new ByteArrayInputStream(new byte[] { 'a', 'b', 'c' });
        final StringReader reader = new StringReader("abc");
        final Class<?> sourceType = method.getParameterTypes()[0];
        final Object source = sourceType == File.class ? sourceFile : sourceType == InputStream.class ? input : reader;
        final File target = dir.resolve("target.txt").toFile();
        final CountingOutput output = new CountingOutput();
        final CountingWriter writer = new CountingWriter();
        assertTrue(assertThrows(IllegalArgumentException.class, () -> invokeIo(method, null, -1L, -1L, null, null, null)).getMessage().contains("source"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> invokeIo(method, source, -1L, -1L, null, null, null)).getMessage().contains("offset"));
        assertTrue(
                assertThrows(IllegalArgumentException.class, () -> invokeIo(method, source, 0L, -1L, target, output, writer)).getMessage().contains("count"));
        assertFalse(target.exists());
        assertEquals(0, output.flushes + writer.flushes);

        // A zero count must not skip even when the offset lies beyond the source's end.
        assertEquals(0L, invokeIo(method, source, 100L, 0L, target, output, writer));
        assertEquals(2L, invokeIo(method, source, 1L, 2L, target, output, writer));
        final boolean fileDestination = Arrays.stream(method.getParameterTypes()).skip(3).anyMatch(type -> type == File.class);
        assertEquals("bc", fileDestination ? Files.readString(target.toPath()) : output.toString(StandardCharsets.UTF_8) + writer);
        assertEquals(0, output.closes + writer.closes);
        assertEquals(-1, sourceType == InputStream.class ? input.read() : sourceType == Reader.class ? reader.read() : -1);
    }

    private static Object invokeIo(final Method method, final Object source, final Number offset, final Number count, final File file,
            final OutputStream output, final Writer writer) throws Throwable {
        final Class<?>[] types = method.getParameterTypes();
        final Object[] args = new Object[types.length];
        args[0] = source;
        args[1] = offset;
        args[2] = count;
        for (int i = 3; i < types.length; i++) {
            args[i] = types[i] == File.class ? file
                    : types[i] == OutputStream.class ? output : types[i] == Writer.class ? writer : types[i] == Charset.class ? null : Boolean.TRUE;
        }
        try {
            return method.invoke(null, args);
        } catch (final InvocationTargetException e) {
            throw e.getCause();
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void beanComparisonsValidateEveryNameBeforeReadingProperties(final boolean ordering) {
        final ComparisonBean first = new ComparisonBean(1);
        final ComparisonBean second = new ComparisonBean(2);
        for (final List<String> names : Arrays.asList(List.of("value", "missing"), Arrays.asList("value", null))) {
            assertThrows(IllegalArgumentException.class, () -> compareBeans(ordering, first, second, names));
            assertEquals(0, first.reads + second.reads);
        }
        compareBeans(ordering, first, second, List.of("value", "failure"));
        assertEquals(2, first.reads + second.reads, "A valid comparison still stops at the first unequal value");
        assertThrows(RuntimeException.class, () -> compareBeans(ordering, first, second, List.of("failure")));
        assertThrows(IllegalArgumentException.class, () -> compareBeans(ordering, first, new ValueOnlyBean(), List.of("value", "failure")));
        assertEquals(1, first.reads, "A property missing from the second bean is rejected before the first getter is invoked");
    }

    private static void compareBeans(final boolean ordering, final Object first, final Object second, final Collection<String> properties) {
        if (ordering) {
            assertTrue(N.compareByProps(first, second, properties) < 0);
        } else {
            assertFalse(N.equalsByProps(first, second, properties));
        }
    }

    @AccessFieldByMethod
    public static class ComparisonBean {
        private int value;
        private int failure;
        int reads;

        public ComparisonBean() {
            this(0);
        }

        ComparisonBean(final int value) {
            this.value = value;
        }

        public int getValue() {
            reads++;
            return value;
        }

        public void setValue(final int value) {
            this.value = value;
        }

        public int getFailure() {
            throw new IllegalStateException("getter failed");
        }

        public void setFailure(final int failure) {
            this.failure = failure;
        }
    }

    public static class ValueOnlyBean {
        private int value = 2;

        public int getValue() {
            return value;
        }

        public void setValue(final int value) {
            this.value = value;
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { "array", "arrayList", "linkedList" })
    void groupingValidatesCallbacksBeforeSuppliersAndPreservesEncounterOrder(final String kind) {
        final AtomicInteger supplied = new AtomicInteger();
        final Supplier<Map<Integer, List<String>>> supplier = () -> {
            supplied.incrementAndGet();
            return new LinkedHashMap<>();
        };
        final String[] values = { "aa", "b", "cc" };
        final List<String> list = kind.equals("linkedList") ? new LinkedList<>(Arrays.asList(values)) : Arrays.asList(values);
        if (kind.equals("array")) {
            assertThrows(IndexOutOfBoundsException.class, () -> N.groupBy(values, -1, 0, null, supplier));
            assertThrows(IllegalArgumentException.class, () -> N.groupBy(values, 0, 0, null, supplier));
            assertEquals(0, supplied.get());
            assertEquals(Map.of(2, List.of("aa", "cc"), 1, List.of("b")), N.groupBy(values, 0, 3, String::length, supplier));
        } else {
            assertThrows(IndexOutOfBoundsException.class, () -> N.groupBy(list, -1, 0, null, supplier));
            assertThrows(IllegalArgumentException.class, () -> N.groupBy(list, 0, 0, null, supplier));
            assertEquals(0, supplied.get());
            assertEquals(Map.of(2, List.of("aa", "cc"), 1, List.of("b")), N.groupBy(list, 0, 3, String::length, supplier));
        }
        assertEquals(1, supplied.get());
    }

    @ParameterizedTest
    @ValueSource(ints = { 2, 3 })
    void iteratorWindowsValidateBeforeInspectingTheIteratorAndPropagateCallbacks(final int width) {
        final Iterator<Integer> untouched = new Iterator<>() {
            @Override
            public boolean hasNext() {
                throw new AssertionError("Validation must precede iteration");
            }

            @Override
            public Integer next() {
                throw new AssertionError("Validation must precede iteration");
            }
        };
        if (width == 2) {
            assertTrue(assertThrows(IllegalArgumentException.class, () -> N.forEachPair(untouched, 0, null)).getMessage().contains("increment"));
            assertThrows(IllegalArgumentException.class, () -> N.forEachPair(untouched, 1, null));
            assertThrows(IllegalArgumentException.class, () -> N.forEachPair((Iterator<Integer>) null, 1, null));
            final IOException failure = new IOException("pair");
            assertSame(failure, assertThrows(IOException.class, () -> N.forEachPair(List.of(1).iterator(), 1, (a, b) -> {
                throw failure;
            })));
        } else {
            assertTrue(assertThrows(IllegalArgumentException.class, () -> N.forEachTriple(untouched, 0, null)).getMessage().contains("increment"));
            assertThrows(IllegalArgumentException.class, () -> N.forEachTriple(untouched, 1, null));
            assertThrows(IllegalArgumentException.class, () -> N.forEachTriple((Iterator<Integer>) null, 1, null));
            final IOException failure = new IOException("triple");
            assertSame(failure, assertThrows(IOException.class, () -> N.forEachTriple(List.of(1).iterator(), 1, (a, b, c) -> {
                throw failure;
            })));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { "date", "calendar", "xml" })
    void explicitLocaleIsValidatedBeforeTheDestinationEvenForNullDates(final String kind) {
        assertTrue(assertThrows(IllegalArgumentException.class, () -> formatNull(kind, null, null)).getMessage().contains("locale"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> formatNull(kind, Locale.US, null)).getMessage().contains("appendable"));
        final StringBuilder text = new StringBuilder();
        formatNull(kind, Locale.US, text);
        assertEquals("null", text.toString());
    }

    private static void formatNull(final String kind, final Locale locale, final Appendable output) {
        switch (kind) {
            case "date" -> Dates.formatTo((java.util.Date) null, null, null, locale, output);
            case "calendar" -> Dates.formatTo((Calendar) null, null, null, locale, output);
            default -> Dates.formatTo((XMLGregorianCalendar) null, null, null, locale, output);
        }
    }

    @Test
    void executorValidationDoesNotChangeTheFactoryOrShutDownTheService() {
        final ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
        final ScheduledThreadPoolExecutor scheduled = new ScheduledThreadPoolExecutor(1);
        try {
            final ThreadFactory original = executor.getThreadFactory();
            final ThreadFactory originalScheduled = scheduled.getThreadFactory();
            assertTrue(assertThrows(IllegalArgumentException.class, () -> MoreExecutors.getExitingExecutorService(executor, -1, null)).getMessage()
                    .contains("terminationTimeout"));
            assertTrue(assertThrows(IllegalArgumentException.class, () -> MoreExecutors.getExitingScheduledExecutorService(scheduled, -1, null)).getMessage()
                    .contains("terminationTimeout"));
            assertTrue(assertThrows(IllegalArgumentException.class, () -> MoreExecutors.addDelayedShutdownHook(executor, -1, null)).getMessage()
                    .contains("terminationTimeout"));
            assertTrue(assertThrows(IllegalArgumentException.class, () -> Fn.shutdown(executor, -1, null)).getMessage().contains("terminationTimeout"));
            assertSame(original, executor.getThreadFactory());
            assertSame(originalScheduled, scheduled.getThreadFactory());
            assertFalse(executor.isShutdown());
            assertFalse(scheduled.isShutdown());
            Fn.shutdown(executor, 0, TimeUnit.SECONDS).run();
            assertTrue(executor.isShutdown());
        } finally {
            executor.shutdownNow();
            scheduled.shutdownNow();
        }
    }

    @ParameterizedTest
    @EnumSource(RoundingMode.class)
    void bigIntegerDivisionKeepsArithmeticAndRoundingBehavior(final RoundingMode mode) {
        assertEquals(BigInteger.valueOf(3), Numbers.divide(BigInteger.valueOf(9), BigInteger.valueOf(3), mode));
        assertThrows(ArithmeticException.class, () -> Numbers.divide(BigInteger.ONE, BigInteger.ZERO, mode));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> Numbers.divide(BigInteger.ONE, null, null)).getMessage().contains("q"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> Numbers.divide(BigInteger.ONE, BigInteger.ZERO, null)).getMessage().contains("mode"));
    }

    @ParameterizedTest
    @ValueSource(ints = { 1, 2, 3, 4, 5, 6 })
    void medianComparatorBranchesRetainNullHandlingAndDoNotMutateSources(final int length) {
        final Integer[] values = new Integer[length];
        for (int i = 1; i < length; i++) {
            values[i] = length - i;
        }
        final Integer[] original = values.clone();
        final Comparator<Integer> comparator = Comparator.nullsFirst(Comparator.naturalOrder());
        final Pair<Integer, ?> expected = Median.of(values, 0, length, comparator);
        assertEquals(expected, Median.of(new LinkedList<>(Arrays.asList(values)), 0, length, comparator));
        assertArrayEquals(original, values);
        if (length > 1) {
            assertThrows(NullPointerException.class, () -> Median.of(values, 0, length, Comparator.naturalOrder()));
            assertThrows(NullPointerException.class, () -> Median.of(Arrays.asList(values), Comparator.naturalOrder()));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { "plain", "json", "xml" })
    void bufferedWritersCheckStateThenNullThenBoundsAndRecoverAfterInvalidArguments(final String kind) throws IOException {
        final StringWriter destination = new StringWriter();
        final BufferedWriter writer = switch (kind) {
            case "json" -> new BufferedJsonWriter(destination);
            case "xml" -> new BufferedXmlWriter(destination);
            default -> new BufferedWriter(destination);
        };
        try {
            assertThrows(NullPointerException.class, () -> writeCharacters(writer, null, -1, -1));
            for (final int[] range : new int[][] { { -1, 0 }, { 0, -1 }, { 2, 0 }, { Integer.MAX_VALUE, 1 } }) {
                assertThrows(IndexOutOfBoundsException.class, () -> writeCharacters(writer, new char[1], range[0], range[1]));
            }
            writeCharacters(writer, new char[0], 0, 0);
            writeCharacters(writer, new char[] { 'A', '&', 'B' }, 0, 3);
            writer.flush();
            assertEquals(kind.equals("xml") ? "A&amp;B" : "A&B", destination.toString());
        } finally {
            writer.close();
        }
        assertThrows(IOException.class, () -> writeCharacters(writer, null, -1, -1));
        assertThrows(IOException.class, () -> writeCharacters(writer, new char[0], 0, 0));
    }

    private static void writeCharacters(final BufferedWriter writer, final char[] value, final int offset, final int length) throws IOException {
        if (writer instanceof CharacterWriter escaping) {
            escaping.writeCharacter(value, offset, length);
        } else {
            writer.write(value, offset, length);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void batchCleanupFailurePreservesTheExecutionOutcome(final boolean executionFails) throws SQLException {
        final SQLException executionFailure = new SQLException("execution failed");
        final AtomicInteger clears = new AtomicInteger();
        final int[] result = { 1, 2 };
        final Statement statement = (Statement) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { Statement.class },
                (proxy, method, arguments) -> {
                    if (method.getName().equals("executeBatch")) {
                        if (executionFails) {
                            throw executionFailure;
                        }
                        return result;
                    }
                    if (method.getName().equals("clearBatch")) {
                        clears.incrementAndGet();
                        throw new SQLException("cleanup failed");
                    }
                    throw new AssertionError("Unexpected Statement operation: " + method.getName());
                });
        if (executionFails) {
            assertSame(executionFailure, assertThrows(SQLException.class, () -> DataSourceUtil.executeBatch(statement)));
        } else {
            assertSame(result, DataSourceUtil.executeBatch(statement));
        }
        assertEquals(1, clears.get());
    }

    private static class CountingOutput extends ByteArrayOutputStream {
        int flushes;
        int closes;

        @Override
        public void flush() {
            flushes++;
        }

        @Override
        public void close() {
            closes++;
        }
    }

    private static class CountingWriter extends StringWriter {
        int flushes;
        int closes;

        @Override
        public void flush() {
            flushes++;
        }

        @Override
        public void close() {
            closes++;
        }
    }
}
