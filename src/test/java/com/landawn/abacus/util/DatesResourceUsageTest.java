package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.SimpleTimeZone;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.UncheckedIOException;

/** Regression coverage for formatter ownership, bounded retention, and allocation-saving paths. */
public class DatesResourceUsageTest extends TestBase {

    private static final String LEGACY_PATTERN = "yyyy/MM/dd HH:mm:ss";
    private static final DateTimeFormatter REFERENCE = DateTimeFormatter.ofPattern("uuuu/MM/dd HH:mm:ss", Locale.ROOT);
    private static final Instant INSTANT = Instant.parse("2025-01-15T10:30:45Z");

    // SimpleTimeZone.clone() shares subclass fields, just as a caller's zone can retain application state.
    private static final class PayloadZone extends SimpleTimeZone {
        private final Object payload = new Object();

        PayloadZone(final int offset) {
            super(offset, "payload-zone");
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<Object, Object> cache(final Class<?> owner, final String name) throws Exception {
        final Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        return (Map<Object, Object>) field.get(null);
    }

    private static void restore(final Map<Object, Object> cache, final Map<Object, Object> snapshot) {
        synchronized (cache) {
            cache.clear();
            cache.putAll(snapshot);
        }
    }

    private static DateFormat onlyIdleFormatter(final Map<Object, Object> pool) {
        assertEquals(1, pool.size());
        final Queue<?> queue = (Queue<?>) pool.values().iterator().next();
        assertEquals(1, queue.size());
        final DateFormat formatter = (DateFormat) queue.element();
        assertFalse(formatter.getTimeZone() instanceof PayloadZone, "an idle formatter must release the caller's zone graph");
        return formatter;
    }

    @Test
    public void pooledFormattersReleaseCallerZonesOnSuccessAndFailure() throws Exception {
        final Map<Object, Object> pool = cache(Dates.class, "dfPool");
        final Map<Object, Object> before = new LinkedHashMap<>(pool);
        final PayloadZone zone = new PayloadZone(5 * 3600_000);
        assertSame(zone.payload, ((PayloadZone) zone.clone()).payload);

        try {
            pool.clear();
            assertEquals("2025/01/15 15:30:45", Dates.format(Date.from(INSTANT), LEGACY_PATTERN, zone));
            final DateFormat formatter = onlyIdleFormatter(pool);

            assertEquals(INSTANT.toEpochMilli(), Dates.parseToJUDate("2025/01/15 15:30:45", LEGACY_PATTERN, zone).getTime());
            assertSame(formatter, onlyIdleFormatter(pool));

            assertThrows(IllegalArgumentException.class, () -> Dates.parseToJUDate("2025/02/30 15:30:45", LEGACY_PATTERN, zone));
            assertSame(formatter, onlyIdleFormatter(pool));

            final IOException failure = new IOException("output failed");
            final Writer failingWriter = new Writer() {
                @Override
                public void write(final char[] text, final int offset, final int length) throws IOException {
                    throw failure;
                }

                @Override
                public void flush() {
                }

                @Override
                public void close() {
                }
            };
            final UncheckedIOException thrown = assertThrows(UncheckedIOException.class,
                    () -> Dates.formatTo(Date.from(INSTANT), LEGACY_PATTERN, zone, failingWriter));
            assertSame(failure, thrown.getCause());
            assertSame(formatter, onlyIdleFormatter(pool));

            // Reusing the same formatter must install the new zone, without modifying either caller.
            final PayloadZone nextZone = new PayloadZone(-7 * 3600_000);
            assertEquals("2025/01/15 03:30:45", Dates.format(Date.from(INSTANT), LEGACY_PATTERN, nextZone));
            assertSame(formatter, onlyIdleFormatter(pool));
            assertEquals(5 * 3600_000, zone.getRawOffset());
            assertEquals(-7 * 3600_000, nextZone.getRawOffset());
        } finally {
            restore(pool, before);
        }
    }

    @Test
    public void concurrentBorrowersKeepIndependentZonesAndBoundIdleRetention() throws Exception {
        final Map<Object, Object> pool = cache(Dates.class, "dfPool");
        final Map<Object, Object> before = new LinkedHashMap<>(pool);
        final int borrowers = 96;
        final CountDownLatch formatted = new CountDownLatch(borrowers);
        final CountDownLatch release = new CountDownLatch(1);
        final int[] offsets = { -8 * 3600, 0, 5 * 3600 + 30 * 60, 9 * 3600 + 45 * 60 };

        try {
            pool.clear();

            try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
                final List<Future<?>> futures = new ArrayList<>();

                for (int i = 0; i < borrowers; i++) {
                    final int offset = offsets[i % offsets.length];
                    futures.add(executor.submit(() -> {
                        final String expected = REFERENCE.format(INSTANT.atOffset(ZoneOffset.ofTotalSeconds(offset)));
                        final Appendable output = new Appendable() {
                            @Override
                            public Appendable append(final CharSequence text) throws IOException {
                                assertEquals(expected, text.toString());
                                formatted.countDown();

                                try {
                                    if (!release.await(30, TimeUnit.SECONDS)) {
                                        throw new IOException("timed out waiting for concurrent formatters");
                                    }
                                } catch (final InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                    throw new IOException(e);
                                }

                                return this;
                            }

                            @Override
                            public Appendable append(final CharSequence text, final int start, final int end) throws IOException {
                                return append(text.subSequence(start, end));
                            }

                            @Override
                            public Appendable append(final char value) throws IOException {
                                return append(String.valueOf(value));
                            }
                        };
                        Dates.formatTo(Date.from(INSTANT), LEGACY_PATTERN, new PayloadZone(offset * 1000), output);
                    }));
                }

                try {
                    assertTrue(formatted.await(20, TimeUnit.SECONDS), "all borrowers must hold their formatter before any are recycled");
                    assertEquals(1, pool.size());
                    assertTrue(((Queue<?>) pool.values().iterator().next()).isEmpty());
                } finally {
                    release.countDown();
                }

                for (final Future<?> future : futures) {
                    future.get(20, TimeUnit.SECONDS);
                }
            }

            final ArrayBlockingQueue<?> idle = (ArrayBlockingQueue<?>) pool.values().iterator().next();
            assertEquals(64, idle.size(), "a burst must retain at most 64 idle formatters per pattern");
            assertEquals(0, idle.remainingCapacity(), "the backing array must obey the same retention budget");

            for (final Object entry : idle) {
                assertFalse(((DateFormat) entry).getTimeZone() instanceof PayloadZone);
            }

            assertEquals("2025/01/15 12:30:45", Dates.format(Date.from(INSTANT), LEGACY_PATTERN, new PayloadZone(2 * 3600_000)));
            assertEquals(64, idle.size());
        } finally {
            release.countDown();
            restore(pool, before);
        }
    }

    @Test
    public void builtInTimestampGrammarsRemainStrictWhenCustomPatternCacheIsFull() throws Exception {
        final Map<Object, Object> patterns = cache(Dates.DTF.class, "patternCache");
        final Map<Object, Object> before = new LinkedHashMap<>(patterns);

        try {
            patterns.clear();

            for (int i = 0; i < 128; i++) {
                Dates.DTF.of("uuuu/MM/dd 'resource-test-" + i + "'");
            }

            final Map<Object, Object> fullCache = new LinkedHashMap<>(patterns);
            assertEquals(128, fullCache.size());

            for (final String local : new String[] { "0001-01-01T00:00:00.000", "1582-10-10T12:00:00.500", "1969-12-31T23:59:59.999", "2024-02-29T10:30:45.123",
                    "9999-12-31T23:59:59.999" }) {
                final LocalDateTime expected = LocalDateTime.parse(local);
                assertEquals(expected, Dates.parseToLocalDateTime(local, Dates.ISO_LOCAL_TIMESTAMP_FORMAT));
                assertEquals(expected, Dates.parseToLocalDateTime(local.replace('T', ' '), Dates.LOCAL_TIMESTAMP_FORMAT));
            }

            for (final String fraction : new String[] { "1", "12", "1234", "123456789" }) {
                final String local = "2025-01-15T10:30:45." + fraction;
                assertThrows(IllegalArgumentException.class, () -> Dates.parseToLocalDateTime(local, Dates.ISO_LOCAL_TIMESTAMP_FORMAT));
                assertThrows(IllegalArgumentException.class, () -> Dates.parseToLocalDateTime(local.replace('T', ' '), Dates.LOCAL_TIMESTAMP_FORMAT));
                assertEquals(LocalDateTime.parse(local), Dates.parseToLocalDateTime(local));
                assertEquals(LocalDateTime.parse(local), Dates.parseToLocalDateTime(local.replace('T', ' ')));
            }

            assertThrows(IllegalArgumentException.class, () -> Dates.parseToLocalDateTime("2025-02-29T10:30:45.123", Dates.ISO_LOCAL_TIMESTAMP_FORMAT));
            assertEquals(fullCache, patterns, "built-in parsing must not displace the caller's cached patterns");
        } finally {
            restore(patterns, before);
        }
    }

    @Test
    public void defaultBuilderFormattingPreservesPrefixesPrecisionAndFailureAtomicity() {
        final DateTimeFormatter seconds = DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss'Z'", Locale.ROOT).withZone(ZoneOffset.UTC);
        final DateTimeFormatter millis = DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ROOT).withZone(ZoneOffset.UTC);

        for (final long epochMillis : new long[] { -62_135_596_800_000L, -12_219_292_800_001L, -1001L, -1L, 0L, 1L, 1736937045123L, 253_402_300_799_999L }) {
            for (final String prefix : new String[] { "prefix:", "\u03a9 prefix:" }) {
                final StringBuilder builder = new StringBuilder(prefix);
                Dates.formatTo(new Date(epochMillis), builder);
                Dates.formatTo(new Timestamp(epochMillis), builder);
                Dates.formatTo(new java.sql.Date(epochMillis), builder);
                Dates.formatTo(new java.sql.Time(epochMillis), builder);
                final Instant instant = Instant.ofEpochMilli(epochMillis);
                assertEquals(prefix + seconds.format(instant) + millis.format(instant).repeat(3), builder.toString());
            }
        }

        for (final long epochMillis : new long[] { Long.MIN_VALUE, -62_135_596_800_001L, 253_402_300_800_000L, Long.MAX_VALUE }) {
            final StringBuilder builder = new StringBuilder("unchanged");
            assertThrows(IllegalArgumentException.class, () -> Dates.formatTo(new Date(epochMillis), builder));
            assertEquals("unchanged", builder.toString());
        }
    }
}
