package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class TickerTest extends TestBase {

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
    public void testSystemTicker() {
        assertSame(Ticker.systemTicker(), Ticker.systemTicker());
    }

    @Test
    public void testSystemTickerRead() {
        Ticker ticker = Ticker.systemTicker();
        long time1 = ticker.read();
        long time2 = ticker.read();

        Assertions.assertTrue(time2 - time1 >= 0);
    }

}
