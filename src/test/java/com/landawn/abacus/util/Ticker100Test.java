package com.landawn.abacus.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class Ticker100Test extends TestBase {

    @Test
    public void testSystemTicker() {
        Ticker ticker = Ticker.systemTicker();
        Assertions.assertNotNull(ticker);

        Ticker ticker2 = Ticker.systemTicker();
        Assertions.assertSame(ticker, ticker2);
    }

    @Test
    public void testSystemTickerRead() {
        Ticker ticker = Ticker.systemTicker();

        long time1 = ticker.read();
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        long time2 = ticker.read();

        Assertions.assertTrue(time2 > time1);

        Assertions.assertTrue(time1 > 0);
        Assertions.assertTrue(time2 > 0);
    }

    @Test
    public void testSystemTickerMonotonicity() {
        Ticker ticker = Ticker.systemTicker();

        long previousTime = ticker.read();
        for (int i = 0; i < 100; i++) {
            long currentTime = ticker.read();
            Assertions.assertTrue(currentTime >= previousTime);
            previousTime = currentTime;
        }
    }

    @Test
    public void testSystemTickerElapsedTime() {
        Ticker ticker = Ticker.systemTicker();

        long startTime = ticker.read();

        int sum = 0;
        for (int i = 0; i < 1000000; i++) {
            sum += i;
        }

        long endTime = ticker.read();
        long elapsed = endTime - startTime;

        Assertions.assertTrue(elapsed > 0);

        Assertions.assertTrue(sum != 0);
    }

    @Test
    public void testCustomTicker() {
        Ticker customTicker = new Ticker() {
            private long time = 1000000L;

            @Override
            public long read() {
                long current = time;
                time += 1000000L;
                return current;
            }
        };

        long time1 = customTicker.read();
        long time2 = customTicker.read();
        long time3 = customTicker.read();

        Assertions.assertEquals(1000000L, time1);
        Assertions.assertEquals(2000000L, time2);
        Assertions.assertEquals(3000000L, time3);

        long elapsed = time3 - time1;
        Assertions.assertEquals(2000000L, elapsed);
    }

    @Test
    public void testTickerWithStopwatch() {
        Ticker ticker = Ticker.systemTicker();

        long start = ticker.read();

        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        long end = ticker.read();
        long elapsedNanos = end - start;

        Assertions.assertTrue(elapsedNanos >= 10_000_000L);

        Assertions.assertTrue(elapsedNanos < 100_000_000L);
    }

    @Test
    public void testTickerPrecision() {
        Ticker ticker = Ticker.systemTicker();

        long[] readings = new long[10];
        for (int i = 0; i < readings.length; i++) {
            readings[i] = ticker.read();
        }

        boolean foundDifferent = false;
        for (int i = 1; i < readings.length; i++) {
            if (readings[i] != readings[i - 1]) {
                foundDifferent = true;
                break;
            }
        }

        for (int i = 1; i < readings.length; i++) {
            Assertions.assertTrue(readings[i] >= readings[i - 1]);
        }
    }

    @Test
    public void testNegativeTicker() {
        Ticker negativeTicker = new Ticker() {
            private long time = -1000000L;

            @Override
            public long read() {
                return time++;
            }
        };

        long time1 = negativeTicker.read();
        long time2 = negativeTicker.read();

        Assertions.assertTrue(time1 < 0);
        Assertions.assertTrue(time2 > time1);

        long elapsed = time2 - time1;
        Assertions.assertEquals(1L, elapsed);
    }
}
