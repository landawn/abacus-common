package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class Ticker2025Test extends TestBase {

    @Test
    public void testSystemTicker() {
        Ticker ticker = Ticker.systemTicker();
        assertNotNull(ticker);
    }

    @Test
    public void testRead() {
        Ticker ticker = Ticker.systemTicker();
        long time = ticker.read();
        assertTrue(time > 0);
    }

    @Test
    public void testRead_monotonic() throws InterruptedException {
        Ticker ticker = Ticker.systemTicker();
        long time1 = ticker.read();
        Thread.sleep(1);
        long time2 = ticker.read();
        assertTrue(time2 >= time1);
    }

    @Test
    public void testRead_multipleReads() {
        Ticker ticker = Ticker.systemTicker();
        for (int i = 0; i < 10; i++) {
            long time = ticker.read();
            assertTrue(time > 0);
        }
    }
}
