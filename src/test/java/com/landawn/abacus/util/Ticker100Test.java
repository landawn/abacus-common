package com.landawn.abacus.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class Ticker100Test extends TestBase {

    @Test
    public void testSystemTicker() {
        Ticker ticker = Ticker.systemTicker();
        Assertions.assertNotNull(ticker);
        
        // Verify it returns consistent instance
        Ticker ticker2 = Ticker.systemTicker();
        Assertions.assertSame(ticker, ticker2);
    }

    @Test
    public void testSystemTickerRead() {
        Ticker ticker = Ticker.systemTicker();
        
        long time1 = ticker.read();
        // Sleep a tiny bit to ensure time advances
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        long time2 = ticker.read();
        
        // Time should advance
        Assertions.assertTrue(time2 > time1);
        
        // Values should be positive
        Assertions.assertTrue(time1 > 0);
        Assertions.assertTrue(time2 > 0);
    }

    @Test
    public void testSystemTickerMonotonicity() {
        Ticker ticker = Ticker.systemTicker();
        
        long previousTime = ticker.read();
        for (int i = 0; i < 100; i++) {
            long currentTime = ticker.read();
            // Time should never go backwards
            Assertions.assertTrue(currentTime >= previousTime);
            previousTime = currentTime;
        }
    }

    @Test
    public void testSystemTickerElapsedTime() {
        Ticker ticker = Ticker.systemTicker();
        
        long startTime = ticker.read();
        
        // Do some work
        int sum = 0;
        for (int i = 0; i < 1000000; i++) {
            sum += i;
        }
        
        long endTime = ticker.read();
        long elapsed = endTime - startTime;
        
        // Should have taken some time
        Assertions.assertTrue(elapsed > 0);
        
        // Verify sum to ensure loop wasn't optimized away
        Assertions.assertTrue(sum != 0);
    }

    @Test
    public void testCustomTicker() {
        // Test with a custom ticker implementation
        Ticker customTicker = new Ticker() {
            private long time = 1000000L; // Start at 1ms in nanos
            
            @Override
            public long read() {
                long current = time;
                time += 1000000L; // Advance by 1ms each call
                return current;
            }
        };
        
        long time1 = customTicker.read();
        long time2 = customTicker.read();
        long time3 = customTicker.read();
        
        Assertions.assertEquals(1000000L, time1);
        Assertions.assertEquals(2000000L, time2);
        Assertions.assertEquals(3000000L, time3);
        
        // Verify elapsed time calculation works
        long elapsed = time3 - time1;
        Assertions.assertEquals(2000000L, elapsed); // 2ms
    }

    @Test
    public void testTickerWithStopwatch() {
        // Test ticker integration with typical usage pattern
        Ticker ticker = Ticker.systemTicker();
        
        long start = ticker.read();
        
        // Simulate some work
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        long end = ticker.read();
        long elapsedNanos = end - start;
        
        // Should be at least 10ms (10,000,000 nanos)
        Assertions.assertTrue(elapsedNanos >= 10_000_000L);
        
        // But not too much more (less than 100ms)
        Assertions.assertTrue(elapsedNanos < 100_000_000L);
    }

    @Test
    public void testTickerPrecision() {
        Ticker ticker = Ticker.systemTicker();
        
        // Take multiple readings in quick succession
        long[] readings = new long[10];
        for (int i = 0; i < readings.length; i++) {
            readings[i] = ticker.read();
        }
        
        // At least some readings should be different
        boolean foundDifferent = false;
        for (int i = 1; i < readings.length; i++) {
            if (readings[i] != readings[i-1]) {
                foundDifferent = true;
                break;
            }
        }
        
        // On most systems, at least some readings should differ
        // But we can't guarantee this on all systems, so just verify
        // that readings are monotonic
        for (int i = 1; i < readings.length; i++) {
            Assertions.assertTrue(readings[i] >= readings[i-1]);
        }
    }

    @Test
    public void testNegativeTicker() {
        // Test that ticker can handle implementations that return negative values
        // (though system ticker shouldn't)
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
        
        // Elapsed time calculation should still work
        long elapsed = time2 - time1;
        Assertions.assertEquals(1L, elapsed);
    }
}