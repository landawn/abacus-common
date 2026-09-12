package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class StopwatchTest extends TestBase {

    private static class MockTicker extends Ticker {
        private long nanos;

        @Override
        public long read() {
            return nanos;
        }

        public void advance(long nanosToAdvance) {
            nanos += nanosToAdvance;
        }
    }

    @Test
    public void testCreate() {
        Stopwatch unstarted = Stopwatch.createUnstarted();
        assertFalse(unstarted.isRunning());
        assertEquals(0, unstarted.elapsed(TimeUnit.NANOSECONDS));
        assertEquals(Duration.ZERO, unstarted.elapsed());
        assertSame(unstarted, unstarted.start());
        assertTrue(unstarted.isRunning());

        MockTicker ticker = new MockTicker();
        Stopwatch withTicker = Stopwatch.createUnstarted(ticker);
        assertFalse(withTicker.isRunning());
        withTicker.start();
        ticker.advance(5_000_000);
        assertEquals(5, withTicker.elapsed(TimeUnit.MILLISECONDS));

        Stopwatch started = Stopwatch.createStarted(ticker);
        assertTrue(started.isRunning());
        ticker.advance(10_000_000);
        assertEquals(10, started.elapsed(TimeUnit.MILLISECONDS));

        assertThrows(IllegalArgumentException.class, () -> Stopwatch.createUnstarted(null));
        assertThrows(IllegalArgumentException.class, () -> Stopwatch.createStarted(null));
    }

    @Test
    public void testStartStopReset() {
        MockTicker ticker = new MockTicker();
        Stopwatch sw = Stopwatch.createUnstarted(ticker);
        assertSame(sw, sw.start());
        ticker.advance(7_000_000);
        assertEquals(7, sw.elapsed(TimeUnit.MILLISECONDS));
        assertThrows(IllegalStateException.class, sw::start);

        assertSame(sw, sw.stop());
        assertFalse(sw.isRunning());
        ticker.advance(5_000_000);
        assertEquals(7, sw.elapsed(TimeUnit.MILLISECONDS));
        assertThrows(IllegalStateException.class, sw::stop);
        assertThrows(IllegalStateException.class, Stopwatch.createUnstarted()::stop);

        sw.start();
        ticker.advance(3_000_000);
        sw.stop();
        assertEquals(10, sw.elapsed(TimeUnit.MILLISECONDS));

        assertSame(sw, sw.reset());
        assertFalse(sw.isRunning());
        assertEquals(0, sw.elapsed(TimeUnit.NANOSECONDS));
        sw.start();
        ticker.advance(5_000_000);
        assertEquals(5, sw.elapsed(TimeUnit.MILLISECONDS));

        sw.reset().start();
        ticker.advance(4_000_000);
        assertEquals(4, sw.elapsed(TimeUnit.MILLISECONDS));
        assertTrue(sw.isRunning());
    }

    @Test
    public void testTickerFailuresAndOverflow() {
        RuntimeException failure = new RuntimeException("ticker failure");
        Ticker failing = new Ticker() {
            @Override
            public long read() {
                throw failure;
            }
        };
        Stopwatch stopped = Stopwatch.createUnstarted(failing);
        assertSame(failure, assertThrows(RuntimeException.class, stopped::start));
        assertFalse(stopped.isRunning());
        assertEquals(0, stopped.elapsed(TimeUnit.NANOSECONDS));

        int[] reads = { 0 };
        Ticker counting = new Ticker() {
            @Override
            public long read() {
                reads[0]++;
                return 0;
            }
        };
        assertThrows(IllegalStateException.class, Stopwatch.createUnstarted(counting)::stop);
        assertEquals(0, reads[0]);

        int[] stopReads = { 0 };
        Ticker failOnStop = new Ticker() {
            @Override
            public long read() {
                if (stopReads[0]++ == 0) {
                    return 10;
                }
                throw failure;
            }
        };
        Stopwatch running = Stopwatch.createStarted(failOnStop);
        assertSame(failure, assertThrows(RuntimeException.class, running::stop));
        assertTrue(running.isRunning());

        MockTicker ticker = new MockTicker();
        Stopwatch overflow = Stopwatch.createStarted(ticker);
        ticker.advance(Long.MAX_VALUE);
        overflow.stop();
        assertEquals(Long.MAX_VALUE, overflow.elapsed(TimeUnit.NANOSECONDS));
        overflow.start();
        ticker.advance(1);
        assertThrows(ArithmeticException.class, () -> overflow.elapsed(TimeUnit.NANOSECONDS));
        assertThrows(ArithmeticException.class, overflow::stop);
        assertTrue(overflow.isRunning());
        overflow.reset();
        assertFalse(overflow.isRunning());
        assertEquals(0, overflow.elapsed(TimeUnit.NANOSECONDS));
    }

    @Test
    public void testElapsed() {
        MockTicker ticker = new MockTicker();
        Stopwatch sw = Stopwatch.createStarted(ticker);
        assertEquals(0, sw.elapsed(TimeUnit.NANOSECONDS));

        ticker.advance(1_234_567_890L);
        assertEquals(1_234_567_890L, sw.elapsed(TimeUnit.NANOSECONDS));
        assertEquals(1_234_567L, sw.elapsed(TimeUnit.MICROSECONDS));
        assertEquals(1234L, sw.elapsed(TimeUnit.MILLISECONDS));
        assertEquals(1L, sw.elapsed(TimeUnit.SECONDS));
        assertEquals(0L, sw.elapsed(TimeUnit.MINUTES));
        assertEquals(1_234_567_890L, sw.elapsed().toNanos());

        MockTicker round = new MockTicker();
        Stopwatch rounded = Stopwatch.createStarted(round);
        round.advance(1_999_999);
        assertEquals(1L, rounded.elapsed(TimeUnit.MILLISECONDS));

        MockTicker units = new MockTicker();
        Stopwatch timed = Stopwatch.createStarted(units);
        units.advance(150_000_000_000L);
        assertEquals(2L, timed.elapsed(TimeUnit.MINUTES));
        units.advance(7_050_000_000_000L);
        assertEquals(2L, timed.elapsed(TimeUnit.HOURS));
        units.advance(165_600_000_000_000L);
        assertEquals(2L, timed.elapsed(TimeUnit.DAYS));

        MockTicker running = new MockTicker();
        Stopwatch live = Stopwatch.createStarted(running);
        running.advance(5_000_000);
        assertEquals(5, live.elapsed(TimeUnit.MILLISECONDS));
        running.advance(3_000_000);
        assertEquals(8, live.elapsed(TimeUnit.MILLISECONDS));
        live.stop();
        running.advance(100_000_000);
        assertEquals(8, live.elapsed(TimeUnit.MILLISECONDS));
        assertEquals(8_000_000, live.elapsed().toNanos());

        MockTicker durationTicker = new MockTicker();
        Stopwatch durationSw = Stopwatch.createStarted(durationTicker);
        durationTicker.advance(3_500_000_000L);
        Duration duration = durationSw.elapsed();
        assertEquals(3500, durationSw.elapsed(TimeUnit.MILLISECONDS));
        assertEquals(duration.toNanos(), durationSw.elapsed(TimeUnit.NANOSECONDS));
        assertEquals(duration.toMillis(), durationSw.elapsed(TimeUnit.MILLISECONDS));
    }

    @Test
    public void testToStringAndFormat() {
        MockTicker ticker = new MockTicker();
        Stopwatch sw = Stopwatch.createStarted(ticker);
        ticker.advance(38);
        assertTrue(sw.toString().contains("ns"));
        sw.reset().start();
        ticker.advance(1234);
        assertTrue(sw.toString().contains("μs") || sw.toString().contains("us"));
        sw.reset().start();
        ticker.advance(5_678_123);
        assertTrue(sw.toString().contains("ms"));
        sw.reset().start();
        ticker.advance(1_234_000_000L);
        assertTrue(sw.toString().contains("1.234"));
        assertTrue(sw.toString().contains("s"));
        assertFalse(sw.toString().contains("ms"));
        assertEquals(sw.toString(), sw.toString());
        sw.reset().start();
        ticker.advance(90L * 1_000_000_000L);
        assertTrue(sw.toString().contains("min"));
        sw.reset().start();
        ticker.advance(5_400L * 1_000_000_000L);
        assertTrue(sw.toString().contains("h"));
        sw.reset().start();
        ticker.advance(48L * 60 * 60 * 1_000_000_000L);
        assertTrue(sw.toString().contains("d"));
        assertTrue(Stopwatch.createUnstarted().toString().contains("0") || Stopwatch.createUnstarted().toString().contains("ns"));

        assertEquals("1.234", Stopwatch.formatCompact4Digits(1.234));
        assertEquals("12.34", Stopwatch.formatCompact4Digits(12.34));
        assertEquals("123.4", Stopwatch.formatCompact4Digits(123.4));
        assertEquals("1234", Stopwatch.formatCompact4Digits(1234.0));
        assertEquals("1.234e+06", Stopwatch.formatCompact4Digits(1_234_000.0));
        assertEquals("0.001234", Stopwatch.formatCompact4Digits(0.001234));
        assertEquals("0.000", Stopwatch.formatCompact4Digits(0.0));
        assertEquals("-1.234", Stopwatch.formatCompact4Digits(-1.234));
        assertTrue(Stopwatch.formatCompact4Digits(1.234e10).contains("e+10"));
    }

    @Test
    public void testChaining() {
        MockTicker ticker = new MockTicker();
        Stopwatch sw = Stopwatch.createUnstarted(ticker);
        sw.start().stop().reset().start();
        ticker.advance(5_000_000);
        assertEquals(5, sw.elapsed(TimeUnit.MILLISECONDS));
        assertTrue(sw.isRunning());
        assertSame(sw, sw.stop());
        assertSame(sw, sw.reset());
    }
}
