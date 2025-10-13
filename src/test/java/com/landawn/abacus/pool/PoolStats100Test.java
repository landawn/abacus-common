package com.landawn.abacus.pool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class PoolStats100Test extends TestBase {

    @Test
    public void testConstructorAndGetters() {
        int capacity = 100;
        int size = 50;
        long putCount = 1000L;
        long getCount = 800L;
        long hitCount = 600L;
        long missCount = 200L;
        long evictionCount = 100L;
        long maxMemory = 1024 * 1024L;
        long dataSize = 512 * 1024L;

        PoolStats stats = new PoolStats(capacity, size, putCount, getCount, hitCount, missCount, evictionCount, maxMemory, dataSize);

        assertEquals(capacity, stats.capacity());
        assertEquals(size, stats.size());
        assertEquals(putCount, stats.putCount());
        assertEquals(getCount, stats.getCount());
        assertEquals(hitCount, stats.hitCount());
        assertEquals(missCount, stats.missCount());
        assertEquals(evictionCount, stats.evictionCount());
        assertEquals(maxMemory, stats.maxMemory());
        assertEquals(dataSize, stats.dataSize());
    }

    @Test
    public void testWithNegativeMemoryValues() {
        PoolStats stats = new PoolStats(50, 25, 100, 80, 60, 20, 10, -1, -1);

        assertEquals(50, stats.capacity());
        assertEquals(25, stats.size());
        assertEquals(100, stats.putCount());
        assertEquals(80, stats.getCount());
        assertEquals(60, stats.hitCount());
        assertEquals(20, stats.missCount());
        assertEquals(10, stats.evictionCount());
        assertEquals(-1, stats.maxMemory());
        assertEquals(-1, stats.dataSize());
    }

    @Test
    public void testWithZeroValues() {
        PoolStats stats = new PoolStats(0, 0, 0, 0, 0, 0, 0, 0, 0);

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
    public void testWithMaxValues() {
        PoolStats stats = new PoolStats(Integer.MAX_VALUE, Integer.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE,
                Long.MAX_VALUE, Long.MAX_VALUE);

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
        PoolStats stats1 = new PoolStats(100, 50, 1000, 800, 600, 200, 100, 1024, 512);
        PoolStats stats2 = new PoolStats(100, 50, 1000, 800, 600, 200, 100, 1024, 512);
        PoolStats stats3 = new PoolStats(100, 50, 1000, 800, 600, 200, 100, 1024, 513);

        assertEquals(stats1, stats2);
        assertNotEquals(stats1, stats3);
    }

    @Test
    public void testRecordHashCode() {
        PoolStats stats1 = new PoolStats(100, 50, 1000, 800, 600, 200, 100, 1024, 512);
        PoolStats stats2 = new PoolStats(100, 50, 1000, 800, 600, 200, 100, 1024, 512);

        assertEquals(stats1.hashCode(), stats2.hashCode());
    }

    @Test
    public void testRecordToString() {
        PoolStats stats = new PoolStats(100, 50, 1000, 800, 600, 200, 100, 1024, 512);
        String str = stats.toString();

        assertNotNull(str);
        assertTrue(str.contains("capacity=100"));
        assertTrue(str.contains("size=50"));
        assertTrue(str.contains("putCount=1000"));
        assertTrue(str.contains("getCount=800"));
        assertTrue(str.contains("hitCount=600"));
        assertTrue(str.contains("missCount=200"));
        assertTrue(str.contains("evictionCount=100"));
        assertTrue(str.contains("maxMemory=1024"));
        assertTrue(str.contains("dataSize=512"));
    }

    @Test
    public void testHitRateCalculation() {
        PoolStats stats = new PoolStats(100, 50, 1000, 800, 600, 200, 100, -1, -1);

        double hitRate = stats.getCount() > 0 ? (double) stats.hitCount() / stats.getCount() : 0.0;
        assertEquals(0.75, hitRate, 0.001);

        double missRate = stats.getCount() > 0 ? (double) stats.missCount() / stats.getCount() : 0.0;
        assertEquals(0.25, missRate, 0.001);
    }

    @Test
    public void testUtilizationCalculation() {
        PoolStats stats = new PoolStats(100, 75, 1000, 800, 600, 200, 100, -1, -1);

        double utilization = (double) stats.size() / stats.capacity();
        assertEquals(0.75, utilization, 0.001);
    }

    @Test
    public void testMemoryUtilizationCalculation() {
        PoolStats stats = new PoolStats(100, 50, 1000, 800, 600, 200, 100, 1024 * 1024, 768 * 1024);

        double memoryUtilization = (double) stats.dataSize() / stats.maxMemory();
        assertEquals(0.75, memoryUtilization, 0.001);
    }
}
