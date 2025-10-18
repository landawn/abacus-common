package com.landawn.abacus.pool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class PoolStats2025Test extends TestBase {

    @Test
    public void testRecordConstruction() {
        PoolStats stats = new PoolStats(100, 50, 1000, 500, 400, 100, 25, 1024 * 1024, 512 * 1024);

        assertNotNull(stats);
        assertEquals(100, stats.capacity());
        assertEquals(50, stats.size());
        assertEquals(1000, stats.putCount());
        assertEquals(500, stats.getCount());
        assertEquals(400, stats.hitCount());
        assertEquals(100, stats.missCount());
        assertEquals(25, stats.evictionCount());
        assertEquals(1024 * 1024, stats.maxMemory());
        assertEquals(512 * 1024, stats.dataSize());
    }

    @Test
    public void testRecordWithZeroValues() {
        PoolStats stats = new PoolStats(0, 0, 0, 0, 0, 0, 0, 0, 0);

        assertNotNull(stats);
        assertEquals(0, stats.capacity());
        assertEquals(0, stats.size());
        assertEquals(0, stats.putCount());
        assertEquals(0, stats.getCount());
        assertEquals(0, stats.hitCount());
        assertEquals(0, stats.missCount());
        assertEquals(0, stats.evictionCount());
        assertEquals(0, stats.maxMemory());
        assertEquals(0, stats.dataSize());
    }

    @Test
    public void testRecordWithNoMemoryLimit() {
        PoolStats stats = new PoolStats(100, 50, 1000, 500, 400, 100, 25, -1, -1);

        assertNotNull(stats);
        assertEquals(100, stats.capacity());
        assertEquals(50, stats.size());
        assertEquals(-1, stats.maxMemory());
        assertEquals(-1, stats.dataSize());
    }

    @Test
    public void testRecordWithMaxValues() {
        PoolStats stats = new PoolStats(Integer.MAX_VALUE, Integer.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE,
                Long.MAX_VALUE, Long.MAX_VALUE);

        assertNotNull(stats);
        assertEquals(Integer.MAX_VALUE, stats.capacity());
        assertEquals(Integer.MAX_VALUE, stats.size());
        assertEquals(Long.MAX_VALUE, stats.putCount());
        assertEquals(Long.MAX_VALUE, stats.getCount());
        assertEquals(Long.MAX_VALUE, stats.hitCount());
        assertEquals(Long.MAX_VALUE, stats.missCount());
        assertEquals(Long.MAX_VALUE, stats.evictionCount());
        assertEquals(Long.MAX_VALUE, stats.maxMemory());
        assertEquals(Long.MAX_VALUE, stats.dataSize());
    }

    @Test
    public void testRecordEquality() {
        PoolStats stats1 = new PoolStats(100, 50, 1000, 500, 400, 100, 25, 1024, 512);
        PoolStats stats2 = new PoolStats(100, 50, 1000, 500, 400, 100, 25, 1024, 512);

        assertEquals(stats1, stats2);
        assertEquals(stats1.hashCode(), stats2.hashCode());
    }

    @Test
    public void testRecordToString() {
        PoolStats stats = new PoolStats(100, 50, 1000, 500, 400, 100, 25, 1024, 512);

        String str = stats.toString();
        assertNotNull(str);
    }
}
