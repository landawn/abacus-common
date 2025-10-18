package com.landawn.abacus.util;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class Stopwatch100Test extends TestBase {

    @Test
    public void testCreateUnstarted() {
        Stopwatch stopwatch = Stopwatch.createUnstarted();
        Assertions.assertFalse(stopwatch.isRunning());
        Assertions.assertEquals(0, stopwatch.elapsed(TimeUnit.NANOSECONDS));
    }

    @Test
    public void testCreateStarted() {
        Stopwatch stopwatch = Stopwatch.createStarted();
        Assertions.assertTrue(stopwatch.isRunning());

        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        Assertions.assertTrue(stopwatch.elapsed(TimeUnit.NANOSECONDS) > 0);
    }

    @Test
    public void testCreateUnstartedWithTicker() {
        MockTicker ticker = new MockTicker();
        Stopwatch stopwatch = Stopwatch.createUnstarted(ticker);
        Assertions.assertFalse(stopwatch.isRunning());
        Assertions.assertEquals(0, stopwatch.elapsed(TimeUnit.NANOSECONDS));
    }

    @Test
    public void testCreateStartedWithTicker() {
        MockTicker ticker = new MockTicker();
        Stopwatch stopwatch = Stopwatch.createStarted(ticker);
        Assertions.assertTrue(stopwatch.isRunning());

        ticker.advance(1000000);
        Assertions.assertEquals(1, stopwatch.elapsed(TimeUnit.MILLISECONDS));
    }

    @Test
    public void testCreateWithNullTicker() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> Stopwatch.createUnstarted(null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Stopwatch.createStarted(null));
    }

    @Test
    public void testIsRunning() {
        Stopwatch stopwatch = Stopwatch.createUnstarted();
        Assertions.assertFalse(stopwatch.isRunning());

        stopwatch.start();
        Assertions.assertTrue(stopwatch.isRunning());

        stopwatch.stop();
        Assertions.assertFalse(stopwatch.isRunning());
    }

    @Test
    public void testStart() {
        MockTicker ticker = new MockTicker();
        Stopwatch stopwatch = Stopwatch.createUnstarted(ticker);

        stopwatch.start();
        Assertions.assertTrue(stopwatch.isRunning());

        ticker.advance(5000000);
        Assertions.assertEquals(5, stopwatch.elapsed(TimeUnit.MILLISECONDS));
    }

    @Test
    public void testStartAlreadyRunning() {
        Stopwatch stopwatch = Stopwatch.createStarted();
        Assertions.assertThrows(IllegalStateException.class, () -> stopwatch.start());
    }

    @Test
    public void testStop() {
        MockTicker ticker = new MockTicker();
        Stopwatch stopwatch = Stopwatch.createStarted(ticker);

        ticker.advance(10000000);
        stopwatch.stop();

        Assertions.assertFalse(stopwatch.isRunning());
        Assertions.assertEquals(10, stopwatch.elapsed(TimeUnit.MILLISECONDS));

        ticker.advance(5000000);
        Assertions.assertEquals(10, stopwatch.elapsed(TimeUnit.MILLISECONDS));
    }

    @Test
    public void testStopNotRunning() {
        Stopwatch stopwatch = Stopwatch.createUnstarted();
        Assertions.assertThrows(IllegalStateException.class, () -> stopwatch.stop());
    }

    @Test
    public void testReset() {
        MockTicker ticker = new MockTicker();
        Stopwatch stopwatch = Stopwatch.createStarted(ticker);

        ticker.advance(10000000);
        stopwatch.stop();

        stopwatch.reset();
        Assertions.assertFalse(stopwatch.isRunning());
        Assertions.assertEquals(0, stopwatch.elapsed(TimeUnit.NANOSECONDS));
    }

    @Test
    public void testResetWhileRunning() {
        MockTicker ticker = new MockTicker();
        Stopwatch stopwatch = Stopwatch.createStarted(ticker);

        ticker.advance(10000000);

        stopwatch.reset();
        Assertions.assertFalse(stopwatch.isRunning());
        Assertions.assertEquals(0, stopwatch.elapsed(TimeUnit.NANOSECONDS));
    }

    @Test
    public void testElapsedTimeUnit() {
        MockTicker ticker = new MockTicker();
        Stopwatch stopwatch = Stopwatch.createStarted(ticker);

        ticker.advance(1234567890L);

        Assertions.assertEquals(1234567890L, stopwatch.elapsed(TimeUnit.NANOSECONDS));
        Assertions.assertEquals(1234567L, stopwatch.elapsed(TimeUnit.MICROSECONDS));
        Assertions.assertEquals(1234L, stopwatch.elapsed(TimeUnit.MILLISECONDS));
        Assertions.assertEquals(1L, stopwatch.elapsed(TimeUnit.SECONDS));
        Assertions.assertEquals(0L, stopwatch.elapsed(TimeUnit.MINUTES));
    }

    @Test
    public void testElapsedDuration() {
        MockTicker ticker = new MockTicker();
        Stopwatch stopwatch = Stopwatch.createStarted(ticker);

        ticker.advance(5000000000L);

        Duration duration = stopwatch.elapsed();
        Assertions.assertEquals(5000000000L, duration.toNanos());
        Assertions.assertEquals(5L, duration.getSeconds());
    }

    @Test
    public void testElapsedWhileStopped() {
        MockTicker ticker = new MockTicker();
        Stopwatch stopwatch = Stopwatch.createStarted(ticker);

        ticker.advance(10000000);
        stopwatch.stop();

        long elapsed = stopwatch.elapsed(TimeUnit.MILLISECONDS);

        ticker.advance(5000000);

        Assertions.assertEquals(elapsed, stopwatch.elapsed(TimeUnit.MILLISECONDS));
    }

    @Test
    public void testMultipleStartStop() {
        MockTicker ticker = new MockTicker();
        Stopwatch stopwatch = Stopwatch.createUnstarted(ticker);

        stopwatch.start();
        ticker.advance(10000000);
        stopwatch.stop();

        stopwatch.start();
        ticker.advance(5000000);
        stopwatch.stop();

        Assertions.assertEquals(15, stopwatch.elapsed(TimeUnit.MILLISECONDS));
    }

    @Test
    public void testToString() {
        MockTicker ticker = new MockTicker();
        Stopwatch stopwatch = Stopwatch.createStarted(ticker);

        ticker.advance(38);
        Assertions.assertTrue(stopwatch.toString().contains("ns"));

        stopwatch.reset().start();
        ticker.advance(1234);
        Assertions.assertTrue(stopwatch.toString().contains("μs"));

        stopwatch.reset().start();
        ticker.advance(5678123);
        Assertions.assertTrue(stopwatch.toString().contains("ms"));

        stopwatch.reset().start();
        ticker.advance(1234000000L);
        Assertions.assertTrue(stopwatch.toString().contains("s"));

        stopwatch.reset().start();
        ticker.advance(90L * 1000000000L);
        Assertions.assertTrue(stopwatch.toString().contains("min"));

        stopwatch.reset().start();
        ticker.advance(90L * 60 * 1000000000L);
        Assertions.assertTrue(stopwatch.toString().contains("h"));

        stopwatch.reset().start();
        ticker.advance(48L * 60 * 60 * 1000000000L);
        Assertions.assertTrue(stopwatch.toString().contains("d"));
    }

    @Test
    public void testFormatCompact4Digits() {
        Assertions.assertEquals("1.234", Stopwatch.formatCompact4Digits(1.234));
        Assertions.assertEquals("1234", Stopwatch.formatCompact4Digits(1234.0));
        Assertions.assertEquals("1.234e+06", Stopwatch.formatCompact4Digits(1234000.0));
        Assertions.assertEquals("0.001234", Stopwatch.formatCompact4Digits(0.001234));
        Assertions.assertEquals("0.000", Stopwatch.formatCompact4Digits(0.0));
    }

    @Test
    public void testMethodChaining() {
        MockTicker ticker = new MockTicker();
        Stopwatch stopwatch = Stopwatch.createUnstarted(ticker);

        Stopwatch same = stopwatch.start();
        Assertions.assertSame(stopwatch, same);

        ticker.advance(1000000);
        same = stopwatch.stop();
        Assertions.assertSame(stopwatch, same);

        same = stopwatch.reset();
        Assertions.assertSame(stopwatch, same);
    }

    @Test
    public void testRealTimeElapsed() throws InterruptedException {
        Stopwatch stopwatch = Stopwatch.createStarted();

        Thread.sleep(50);

        long elapsedMillis = stopwatch.elapsed(TimeUnit.MILLISECONDS);
        Assertions.assertTrue(elapsedMillis >= 40 && elapsedMillis <= 200, "Elapsed time should be approximately 50ms but was " + elapsedMillis);
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
