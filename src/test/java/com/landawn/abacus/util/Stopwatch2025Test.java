package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class Stopwatch2025Test extends TestBase {

    @Test
    public void test_createUnstarted_initialState() {
        Stopwatch stopwatch = Stopwatch.createUnstarted();
        assertNotNull(stopwatch);
        assertFalse(stopwatch.isRunning());
        assertEquals(0, stopwatch.elapsed(TimeUnit.NANOSECONDS));
        assertEquals(0, stopwatch.elapsed(TimeUnit.MILLISECONDS));
    }

    @Test
    public void test_createUnstarted_elapsedIsZero() {
        Stopwatch stopwatch = Stopwatch.createUnstarted();
        Duration duration = stopwatch.elapsed();
        assertEquals(0, duration.toNanos());
        assertEquals(0, duration.toMillis());
    }

    @Test
    public void test_createUnstarted_canBeStartedLater() {
        Stopwatch stopwatch = Stopwatch.createUnstarted();
        assertFalse(stopwatch.isRunning());
        stopwatch.start();
        assertTrue(stopwatch.isRunning());
    }

    @Test
    public void test_createUnstarted_withTicker_initialState() {
        MockTicker ticker = new MockTicker();
        Stopwatch stopwatch = Stopwatch.createUnstarted(ticker);
        assertNotNull(stopwatch);
        assertFalse(stopwatch.isRunning());
        assertEquals(0, stopwatch.elapsed(TimeUnit.NANOSECONDS));
    }

    @Test
    public void test_createUnstarted_withNullTicker_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            Stopwatch.createUnstarted(null);
        });
    }

    @Test
    public void test_createUnstarted_withTicker_usesTicker() {
        MockTicker ticker = new MockTicker();
        Stopwatch stopwatch = Stopwatch.createUnstarted(ticker);
        stopwatch.start();
        ticker.advance(5000000);
        assertEquals(5, stopwatch.elapsed(TimeUnit.MILLISECONDS));
    }

    @Test
    public void test_createStarted_isRunning() {
        Stopwatch stopwatch = Stopwatch.createStarted();
        assertTrue(stopwatch.isRunning());
    }

    @Test
    public void test_createStarted_elapsedIncreases() throws InterruptedException {
        Stopwatch stopwatch = Stopwatch.createStarted();
        Thread.sleep(10);
        assertTrue(stopwatch.elapsed(TimeUnit.NANOSECONDS) > 0);
        assertTrue(stopwatch.elapsed(TimeUnit.MILLISECONDS) >= 0);
    }

    @Test
    public void test_createStarted_cannotStartAgain() {
        Stopwatch stopwatch = Stopwatch.createStarted();
        assertThrows(IllegalStateException.class, () -> {
            stopwatch.start();
        });
    }

    @Test
    public void test_createStarted_withTicker_isRunning() {
        MockTicker ticker = new MockTicker();
        Stopwatch stopwatch = Stopwatch.createStarted(ticker);
        assertTrue(stopwatch.isRunning());
    }

    @Test
    public void test_createStarted_withTicker_usesTickerImmediately() {
        MockTicker ticker = new MockTicker();
        Stopwatch stopwatch = Stopwatch.createStarted(ticker);
        ticker.advance(10000000);
        assertEquals(10, stopwatch.elapsed(TimeUnit.MILLISECONDS));
    }

    @Test
    public void test_createStarted_withNullTicker_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            Stopwatch.createStarted(null);
        });
    }

    @Test
    public void test_isRunning_unstarted() {
        Stopwatch stopwatch = Stopwatch.createUnstarted();
        assertFalse(stopwatch.isRunning());
    }

    @Test
    public void test_isRunning_started() {
        Stopwatch stopwatch = Stopwatch.createUnstarted();
        stopwatch.start();
        assertTrue(stopwatch.isRunning());
    }

    @Test
    public void test_isRunning_stopped() {
        Stopwatch stopwatch = Stopwatch.createStarted();
        stopwatch.stop();
        assertFalse(stopwatch.isRunning());
    }

    @Test
    public void test_isRunning_reset() {
        Stopwatch stopwatch = Stopwatch.createStarted();
        stopwatch.reset();
        assertFalse(stopwatch.isRunning());
    }

    @Test
    public void test_isRunning_multipleStates() {
        Stopwatch stopwatch = Stopwatch.createUnstarted();
        assertFalse(stopwatch.isRunning());

        stopwatch.start();
        assertTrue(stopwatch.isRunning());

        stopwatch.stop();
        assertFalse(stopwatch.isRunning());

        stopwatch.start();
        assertTrue(stopwatch.isRunning());

        stopwatch.reset();
        assertFalse(stopwatch.isRunning());
    }

    @Test
    public void test_start_setsRunningState() {
        Stopwatch stopwatch = Stopwatch.createUnstarted();
        Stopwatch result = stopwatch.start();
        assertTrue(stopwatch.isRunning());
        assertSame(stopwatch, result);
    }

    @Test
    public void test_start_beginsTimeMeasurement() {
        MockTicker ticker = new MockTicker();
        Stopwatch stopwatch = Stopwatch.createUnstarted(ticker);

        stopwatch.start();
        ticker.advance(7000000);
        assertEquals(7, stopwatch.elapsed(TimeUnit.MILLISECONDS));
    }

    @Test
    public void test_start_alreadyRunning_throwsException() {
        Stopwatch stopwatch = Stopwatch.createStarted();
        assertThrows(IllegalStateException.class, () -> {
            stopwatch.start();
        });
    }

    @Test
    public void test_start_afterStop_accumulates() {
        MockTicker ticker = new MockTicker();
        Stopwatch stopwatch = Stopwatch.createUnstarted(ticker);

        stopwatch.start();
        ticker.advance(5000000);
        stopwatch.stop();

        stopwatch.start();
        ticker.advance(3000000);
        stopwatch.stop();

        assertEquals(8, stopwatch.elapsed(TimeUnit.MILLISECONDS));
    }

    @Test
    public void test_start_methodChaining() {
        Stopwatch stopwatch = Stopwatch.createUnstarted();
        Stopwatch same = stopwatch.start();
        assertSame(stopwatch, same);
    }

    @Test
    public void test_stop_stopsTimeMeasurement() {
        MockTicker ticker = new MockTicker();
        Stopwatch stopwatch = Stopwatch.createStarted(ticker);

        ticker.advance(10000000);
        stopwatch.stop();

        long elapsed = stopwatch.elapsed(TimeUnit.MILLISECONDS);
        ticker.advance(5000000);

        assertEquals(elapsed, stopwatch.elapsed(TimeUnit.MILLISECONDS));
    }

    @Test
    public void test_stop_setsRunningToFalse() {
        Stopwatch stopwatch = Stopwatch.createStarted();
        stopwatch.stop();
        assertFalse(stopwatch.isRunning());
    }

    @Test
    public void test_stop_notRunning_throwsException() {
        Stopwatch stopwatch = Stopwatch.createUnstarted();
        assertThrows(IllegalStateException.class, () -> {
            stopwatch.stop();
        });
    }

    @Test
    public void test_stop_alreadyStopped_throwsException() {
        Stopwatch stopwatch = Stopwatch.createStarted();
        stopwatch.stop();
        assertThrows(IllegalStateException.class, () -> {
            stopwatch.stop();
        });
    }

    @Test
    public void test_stop_preservesElapsedTime() {
        MockTicker ticker = new MockTicker();
        Stopwatch stopwatch = Stopwatch.createStarted(ticker);

        ticker.advance(15000000);
        stopwatch.stop();

        long elapsed1 = stopwatch.elapsed(TimeUnit.MILLISECONDS);
        long elapsed2 = stopwatch.elapsed(TimeUnit.MILLISECONDS);

        assertEquals(15, elapsed1);
        assertEquals(elapsed1, elapsed2);
    }

    @Test
    public void test_stop_methodChaining() {
        Stopwatch stopwatch = Stopwatch.createStarted();
        Stopwatch same = stopwatch.stop();
        assertSame(stopwatch, same);
    }

    @Test
    public void test_reset_clearsElapsedTime() {
        MockTicker ticker = new MockTicker();
        Stopwatch stopwatch = Stopwatch.createStarted(ticker);

        ticker.advance(10000000);
        stopwatch.reset();

        assertEquals(0, stopwatch.elapsed(TimeUnit.NANOSECONDS));
    }

    @Test
    public void test_reset_stopsStopwatch() {
        Stopwatch stopwatch = Stopwatch.createStarted();
        stopwatch.reset();
        assertFalse(stopwatch.isRunning());
    }

    @Test
    public void test_reset_whileRunning() {
        MockTicker ticker = new MockTicker();
        Stopwatch stopwatch = Stopwatch.createStarted(ticker);

        ticker.advance(20000000);
        stopwatch.reset();

        assertFalse(stopwatch.isRunning());
        assertEquals(0, stopwatch.elapsed(TimeUnit.NANOSECONDS));
    }

    @Test
    public void test_reset_whileStopped() {
        MockTicker ticker = new MockTicker();
        Stopwatch stopwatch = Stopwatch.createStarted(ticker);

        ticker.advance(10000000);
        stopwatch.stop();
        stopwatch.reset();

        assertEquals(0, stopwatch.elapsed(TimeUnit.NANOSECONDS));
    }

    @Test
    public void test_reset_canStartAfter() {
        MockTicker ticker = new MockTicker();
        Stopwatch stopwatch = Stopwatch.createStarted(ticker);

        ticker.advance(5000000);
        stopwatch.reset();
        stopwatch.start();
        ticker.advance(3000000);

        assertEquals(3, stopwatch.elapsed(TimeUnit.MILLISECONDS));
    }

    @Test
    public void test_reset_methodChaining() {
        Stopwatch stopwatch = Stopwatch.createStarted();
        Stopwatch same = stopwatch.reset();
        assertSame(stopwatch, same);
    }

    @Test
    public void test_reset_methodChaining_startImmediately() {
        MockTicker ticker = new MockTicker();
        Stopwatch stopwatch = Stopwatch.createStarted(ticker);

        ticker.advance(10000000);
        stopwatch.reset().start();
        ticker.advance(5000000);

        assertEquals(5, stopwatch.elapsed(TimeUnit.MILLISECONDS));
    }

    @Test
    public void test_elapsed_timeUnit_nanoseconds() {
        MockTicker ticker = new MockTicker();
        Stopwatch stopwatch = Stopwatch.createStarted(ticker);

        ticker.advance(1234567890L);
        assertEquals(1234567890L, stopwatch.elapsed(TimeUnit.NANOSECONDS));
    }

    @Test
    public void test_elapsed_timeUnit_microseconds() {
        MockTicker ticker = new MockTicker();
        Stopwatch stopwatch = Stopwatch.createStarted(ticker);

        ticker.advance(1234567890L);
        assertEquals(1234567L, stopwatch.elapsed(TimeUnit.MICROSECONDS));
    }

    @Test
    public void test_elapsed_timeUnit_milliseconds() {
        MockTicker ticker = new MockTicker();
        Stopwatch stopwatch = Stopwatch.createStarted(ticker);

        ticker.advance(1234567890L);
        assertEquals(1234L, stopwatch.elapsed(TimeUnit.MILLISECONDS));
    }

    @Test
    public void test_elapsed_timeUnit_seconds() {
        MockTicker ticker = new MockTicker();
        Stopwatch stopwatch = Stopwatch.createStarted(ticker);

        ticker.advance(5500000000L);
        assertEquals(5L, stopwatch.elapsed(TimeUnit.SECONDS));
    }

    @Test
    public void test_elapsed_timeUnit_minutes() {
        MockTicker ticker = new MockTicker();
        Stopwatch stopwatch = Stopwatch.createStarted(ticker);

        ticker.advance(150000000000L);
        assertEquals(2L, stopwatch.elapsed(TimeUnit.MINUTES));
    }

    @Test
    public void test_elapsed_timeUnit_hours() {
        MockTicker ticker = new MockTicker();
        Stopwatch stopwatch = Stopwatch.createStarted(ticker);

        ticker.advance(7200000000000L);
        assertEquals(2L, stopwatch.elapsed(TimeUnit.HOURS));
    }

    @Test
    public void test_elapsed_timeUnit_days() {
        MockTicker ticker = new MockTicker();
        Stopwatch stopwatch = Stopwatch.createStarted(ticker);

        ticker.advance(172800000000000L);
        assertEquals(2L, stopwatch.elapsed(TimeUnit.DAYS));
    }

    @Test
    public void test_elapsed_timeUnit_roundsDown() {
        MockTicker ticker = new MockTicker();
        Stopwatch stopwatch = Stopwatch.createStarted(ticker);

        ticker.advance(1999999);
        assertEquals(1L, stopwatch.elapsed(TimeUnit.MILLISECONDS));
    }

    @Test
    public void test_elapsed_timeUnit_whileRunning() {
        MockTicker ticker = new MockTicker();
        Stopwatch stopwatch = Stopwatch.createStarted(ticker);

        ticker.advance(5000000);
        long elapsed1 = stopwatch.elapsed(TimeUnit.MILLISECONDS);

        ticker.advance(3000000);
        long elapsed2 = stopwatch.elapsed(TimeUnit.MILLISECONDS);

        assertEquals(5L, elapsed1);
        assertEquals(8L, elapsed2);
    }

    @Test
    public void test_elapsed_timeUnit_whileStopped() {
        MockTicker ticker = new MockTicker();
        Stopwatch stopwatch = Stopwatch.createStarted(ticker);

        ticker.advance(10000000);
        stopwatch.stop();

        long elapsed1 = stopwatch.elapsed(TimeUnit.MILLISECONDS);
        ticker.advance(5000000);
        long elapsed2 = stopwatch.elapsed(TimeUnit.MILLISECONDS);

        assertEquals(10L, elapsed1);
        assertEquals(10L, elapsed2);
    }

    @Test
    public void test_elapsed_timeUnit_accumulatesAcrossStartStop() {
        MockTicker ticker = new MockTicker();
        Stopwatch stopwatch = Stopwatch.createUnstarted(ticker);

        stopwatch.start();
        ticker.advance(7000000);
        stopwatch.stop();

        stopwatch.start();
        ticker.advance(5000000);
        stopwatch.stop();

        assertEquals(12L, stopwatch.elapsed(TimeUnit.MILLISECONDS));
    }

    @Test
    public void test_elapsed_duration_returnsCorrectValue() {
        MockTicker ticker = new MockTicker();
        Stopwatch stopwatch = Stopwatch.createStarted(ticker);

        ticker.advance(5000000000L);
        Duration duration = stopwatch.elapsed();

        assertNotNull(duration);
        assertEquals(5000000000L, duration.toNanos());
        assertEquals(5000L, duration.toMillis());
        assertEquals(5L, duration.getSeconds());
    }

    @Test
    public void test_elapsed_duration_zero() {
        Stopwatch stopwatch = Stopwatch.createUnstarted();
        Duration duration = stopwatch.elapsed();

        assertEquals(0, duration.toNanos());
        assertEquals(Duration.ZERO, duration);
    }

    @Test
    public void test_elapsed_duration_preservesPrecision() {
        MockTicker ticker = new MockTicker();
        Stopwatch stopwatch = Stopwatch.createStarted(ticker);

        ticker.advance(1234567890L);
        Duration duration = stopwatch.elapsed();

        assertEquals(1234567890L, duration.toNanos());
    }

    @Test
    public void test_elapsed_duration_whileRunning() {
        MockTicker ticker = new MockTicker();
        Stopwatch stopwatch = Stopwatch.createStarted(ticker);

        ticker.advance(2000000000L);
        Duration duration1 = stopwatch.elapsed();

        ticker.advance(1000000000L);
        Duration duration2 = stopwatch.elapsed();

        assertEquals(2L, duration1.getSeconds());
        assertEquals(3L, duration2.getSeconds());
    }

    @Test
    public void test_elapsed_duration_whileStopped() {
        MockTicker ticker = new MockTicker();
        Stopwatch stopwatch = Stopwatch.createStarted(ticker);

        ticker.advance(3000000000L);
        stopwatch.stop();

        Duration duration = stopwatch.elapsed();
        assertEquals(3L, duration.getSeconds());
    }

    @Test
    public void test_toString_nanoseconds() {
        MockTicker ticker = new MockTicker();
        Stopwatch stopwatch = Stopwatch.createStarted(ticker);

        ticker.advance(38);
        String str = stopwatch.toString();
        assertTrue(str.contains("ns"));
        assertTrue(str.contains("38"));
    }

    @Test
    public void test_toString_microseconds() {
        MockTicker ticker = new MockTicker();
        Stopwatch stopwatch = Stopwatch.createStarted(ticker);

        ticker.advance(1234);
        String str = stopwatch.toString();
        assertTrue(str.contains("μs") || str.contains("us"));
    }

    @Test
    public void test_toString_milliseconds() {
        MockTicker ticker = new MockTicker();
        Stopwatch stopwatch = Stopwatch.createStarted(ticker);

        ticker.advance(5678123);
        String str = stopwatch.toString();
        assertTrue(str.contains("ms"));
    }

    @Test
    public void test_toString_seconds() {
        MockTicker ticker = new MockTicker();
        Stopwatch stopwatch = Stopwatch.createStarted(ticker);

        ticker.advance(1234000000L);
        String str = stopwatch.toString();
        assertTrue(str.contains("s"));
        assertFalse(str.contains("ms"));
    }

    @Test
    public void test_toString_minutes() {
        MockTicker ticker = new MockTicker();
        Stopwatch stopwatch = Stopwatch.createStarted(ticker);

        ticker.advance(90L * 1000000000L);
        String str = stopwatch.toString();
        assertTrue(str.contains("min"));
    }

    @Test
    public void test_toString_hours() {
        MockTicker ticker = new MockTicker();
        Stopwatch stopwatch = Stopwatch.createStarted(ticker);

        ticker.advance(5400L * 1000000000L);
        String str = stopwatch.toString();
        assertTrue(str.contains("h"));
        assertFalse(str.contains("min"));
    }

    @Test
    public void test_toString_days() {
        MockTicker ticker = new MockTicker();
        Stopwatch stopwatch = Stopwatch.createStarted(ticker);

        ticker.advance(48L * 60 * 60 * 1000000000L);
        String str = stopwatch.toString();
        assertTrue(str.contains("d"));
    }

    @Test
    public void test_toString_zero() {
        Stopwatch stopwatch = Stopwatch.createUnstarted();
        String str = stopwatch.toString();
        assertNotNull(str);
        assertTrue(str.contains("0") || str.contains("ns"));
    }

    @Test
    public void test_toString_formatCompact4Digits() {
        MockTicker ticker = new MockTicker();
        Stopwatch stopwatch = Stopwatch.createStarted(ticker);

        ticker.advance(1234000000L);
        String str = stopwatch.toString();
        assertTrue(str.contains("1.234"));
    }

    @Test
    public void test_formatCompact4Digits_smallDecimals() {
        assertEquals("1.234", Stopwatch.formatCompact4Digits(1.234));
    }

    @Test
    public void test_formatCompact4Digits_wholeNumbers() {
        assertEquals("1234", Stopwatch.formatCompact4Digits(1234.0));
    }

    @Test
    public void test_formatCompact4Digits_largeNumbers() {
        assertEquals("1.234e+06", Stopwatch.formatCompact4Digits(1234000.0));
    }

    @Test
    public void test_formatCompact4Digits_verySmallNumbers() {
        assertEquals("0.001234", Stopwatch.formatCompact4Digits(0.001234));
    }

    @Test
    public void test_formatCompact4Digits_zero() {
        assertEquals("0.000", Stopwatch.formatCompact4Digits(0.0));
    }

    @Test
    public void test_formatCompact4Digits_negativeNumbers() {
        assertEquals("-1.234", Stopwatch.formatCompact4Digits(-1.234));
    }

    @Test
    public void test_formatCompact4Digits_scientificNotation() {
        String result = Stopwatch.formatCompact4Digits(1.234e10);
        assertTrue(result.contains("e+10") || result.contains("1.234e+10"));
    }

    @Test
    public void test_formatCompact4Digits_fourSignificantFigures() {
        assertEquals("12.34", Stopwatch.formatCompact4Digits(12.34));
        assertEquals("123.4", Stopwatch.formatCompact4Digits(123.4));
    }

    @Test
    public void test_multipleStartStopCycles() {
        MockTicker ticker = new MockTicker();
        Stopwatch stopwatch = Stopwatch.createUnstarted(ticker);

        stopwatch.start();
        ticker.advance(10000000);
        stopwatch.stop();
        assertEquals(10, stopwatch.elapsed(TimeUnit.MILLISECONDS));

        stopwatch.start();
        ticker.advance(5000000);
        stopwatch.stop();
        assertEquals(15, stopwatch.elapsed(TimeUnit.MILLISECONDS));

        stopwatch.start();
        ticker.advance(7000000);
        stopwatch.stop();
        assertEquals(22, stopwatch.elapsed(TimeUnit.MILLISECONDS));
    }

    @Test
    public void test_resetAndReuse() {
        MockTicker ticker = new MockTicker();
        Stopwatch stopwatch = Stopwatch.createStarted(ticker);

        ticker.advance(10000000);
        stopwatch.stop();
        assertEquals(10, stopwatch.elapsed(TimeUnit.MILLISECONDS));

        stopwatch.reset();
        assertEquals(0, stopwatch.elapsed(TimeUnit.MILLISECONDS));

        stopwatch.start();
        ticker.advance(5000000);
        assertEquals(5, stopwatch.elapsed(TimeUnit.MILLISECONDS));
    }

    @Test
    public void test_elapsedWhileRunningAndStopped() {
        MockTicker ticker = new MockTicker();
        Stopwatch stopwatch = Stopwatch.createStarted(ticker);

        ticker.advance(5000000);
        long elapsedRunning = stopwatch.elapsed(TimeUnit.MILLISECONDS);

        stopwatch.stop();
        long elapsedStopped = stopwatch.elapsed(TimeUnit.MILLISECONDS);

        assertEquals(5, elapsedRunning);
        assertEquals(5, elapsedStopped);
    }

    @Test
    public void test_veryLargeElapsedTime() {
        MockTicker ticker = new MockTicker();
        Stopwatch stopwatch = Stopwatch.createStarted(ticker);

        ticker.advance(Long.MAX_VALUE / 2);
        assertTrue(stopwatch.elapsed(TimeUnit.NANOSECONDS) > 0);
        assertTrue(stopwatch.elapsed(TimeUnit.DAYS) > 0);
    }

    @Test
    public void test_zeroElapsedTime() {
        MockTicker ticker = new MockTicker();
        Stopwatch stopwatch = Stopwatch.createStarted(ticker);

        assertEquals(0, stopwatch.elapsed(TimeUnit.NANOSECONDS));
        assertEquals(0, stopwatch.elapsed(TimeUnit.MILLISECONDS));
    }

    @Test
    public void test_methodChainingComplex() {
        MockTicker ticker = new MockTicker();
        Stopwatch stopwatch = Stopwatch.createUnstarted(ticker);

        stopwatch.start().stop().reset().start();
        ticker.advance(5000000);

        assertEquals(5, stopwatch.elapsed(TimeUnit.MILLISECONDS));
        assertTrue(stopwatch.isRunning());
    }

    @Test
    public void test_durationAndTimeUnitConsistency() {
        MockTicker ticker = new MockTicker();
        Stopwatch stopwatch = Stopwatch.createStarted(ticker);

        ticker.advance(3500000000L);

        Duration duration = stopwatch.elapsed();
        long millis = stopwatch.elapsed(TimeUnit.MILLISECONDS);
        long nanos = stopwatch.elapsed(TimeUnit.NANOSECONDS);

        assertEquals(3500, millis);
        assertEquals(3500000000L, nanos);
        assertEquals(nanos, duration.toNanos());
        assertEquals(millis, duration.toMillis());
    }

    @Test
    public void test_realTimeStopwatch() throws InterruptedException {
        Stopwatch stopwatch = Stopwatch.createStarted();

        Thread.sleep(50);
        stopwatch.stop();

        long elapsedMillis = stopwatch.elapsed(TimeUnit.MILLISECONDS);
        assertTrue(elapsedMillis >= 40 && elapsedMillis <= 200, "Expected elapsed time ~50ms but was " + elapsedMillis + "ms");
    }

    @Test
    public void test_stoppedStopwatchDoesNotAdvance() {
        MockTicker ticker = new MockTicker();
        Stopwatch stopwatch = Stopwatch.createStarted(ticker);

        ticker.advance(10000000);
        stopwatch.stop();

        long elapsed1 = stopwatch.elapsed(TimeUnit.MILLISECONDS);

        ticker.advance(100000000);

        long elapsed2 = stopwatch.elapsed(TimeUnit.MILLISECONDS);

        assertEquals(elapsed1, elapsed2);
    }

    @Test
    public void test_toStringConsistency() {
        MockTicker ticker = new MockTicker();
        Stopwatch stopwatch = Stopwatch.createStarted(ticker);

        ticker.advance(1500000000L);

        String str1 = stopwatch.toString();
        String str2 = stopwatch.toString();

        assertEquals(str1, str2);
    }

    @Test
    public void test_unstartedStopwatchToString() {
        Stopwatch stopwatch = Stopwatch.createUnstarted();
        String str = stopwatch.toString();
        assertNotNull(str);
        assertTrue(str.length() > 0);
    }

    private static class MockTicker extends Ticker {
        private long nanos = 0;

        @Override
        public long read() {
            return nanos;
        }

        public void advance(long nanosToAdvance) {
            nanos += nanosToAdvance;
        }
    }
}
