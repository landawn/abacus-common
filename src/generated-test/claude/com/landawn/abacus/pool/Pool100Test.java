package com.landawn.abacus.pool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;


public class Pool100Test extends TestBase {

    private TestPool pool;

    private static class TestPoolable extends AbstractPoolable {
        TestPoolable() {
            super(10000, 5000);
        }

        @Override
        public void destroy(Poolable.Caller caller) {
            // No-op
        }
    }

    private static class TestPool implements Pool {
        private boolean locked = false;
        private int capacity = 10;
        private int size = 0;
        private boolean closed = false;

        @Override
        public void lock() {
            locked = true;
        }

        @Override
        public void unlock() {
            if (!locked) {
                throw new IllegalMonitorStateException();
            }
            locked = false;
        }

        @Override
        public int capacity() {
            return capacity;
        }

        @Override
        public int size() {
            return size;
        }

        @Override
        public boolean isEmpty() {
            return size == 0;
        }

        @Override
        public void vacate() {
            if (closed) {
                throw new IllegalStateException("Pool is closed");
            }
            size = Math.max(0, size - 2); // Remove some elements
        }

        @Override
        public void clear() {
            if (closed) {
                throw new IllegalStateException("Pool is closed");
            }
            size = 0;
        }

        @Override
        public PoolStats stats() {
            return new PoolStats(capacity, size, 100, 80, 60, 20, 10, -1, -1);
        }

        @Override
        public void close() {
            closed = true;
            size = 0;
        }

        @Override
        public boolean isClosed() {
            return closed;
        }

        public void setSize(int size) {
            this.size = size;
        }

        public void setCapacity(int capacity) {
            this.capacity = capacity;
        }

        boolean isLocked() {
            return locked;
        }
    }

    @BeforeEach
    public void setUp() {
        pool = new TestPool();
    }

    @Test
    public void testLock() {
        assertFalse(pool.isLocked());
        pool.lock();
        assertTrue(pool.isLocked());
    }

    @Test
    public void testUnlock() {
        pool.lock();
        assertTrue(pool.isLocked());
        pool.unlock();
        assertFalse(pool.isLocked());
    }

    @Test
    public void testUnlockWithoutLock() {
        assertThrows(IllegalMonitorStateException.class, () -> pool.unlock());
    }

    @Test
    public void testCapacity() {
        assertEquals(10, pool.capacity());
        pool.setCapacity(20);
        assertEquals(20, pool.capacity());
    }

    @Test
    public void testSize() {
        assertEquals(0, pool.size());
        pool.setSize(5);
        assertEquals(5, pool.size());
    }

    @Test
    public void testIsEmpty() {
        assertTrue(pool.isEmpty());
        pool.setSize(1);
        assertFalse(pool.isEmpty());
        pool.setSize(0);
        assertTrue(pool.isEmpty());
    }

    @Test
    public void testVacate() {
        pool.setSize(5);
        assertEquals(5, pool.size());
        pool.vacate();
        assertEquals(3, pool.size()); // Should remove 2 elements
        pool.vacate();
        assertEquals(1, pool.size());
        pool.vacate();
        assertEquals(0, pool.size()); // Should not go below 0
    }

    @Test
    public void testVacateOnClosedPool() {
        pool.close();
        assertThrows(IllegalStateException.class, () -> pool.vacate());
    }

    @Test
    public void testClear() {
        pool.setSize(5);
        assertEquals(5, pool.size());
        pool.clear();
        assertEquals(0, pool.size());
    }

    @Test
    public void testClearOnClosedPool() {
        pool.close();
        assertThrows(IllegalStateException.class, () -> pool.clear());
    }

    @Test
    public void testStats() {
        PoolStats stats = pool.stats();
        assertNotNull(stats);
        assertEquals(10, stats.capacity());
        assertEquals(0, stats.size());
        assertEquals(100, stats.putCount());
        assertEquals(80, stats.getCount());
        assertEquals(60, stats.hitCount());
        assertEquals(20, stats.missCount());
        assertEquals(10, stats.evictionCount());
        assertEquals(-1, stats.maxMemory());
        assertEquals(-1, stats.dataSize());
    }

    @Test
    public void testClose() {
        assertFalse(pool.isClosed());
        pool.setSize(5);
        pool.close();
        assertTrue(pool.isClosed());
        assertEquals(0, pool.size()); // Should clear on close
    }

    @Test
    public void testIsClosed() {
        assertFalse(pool.isClosed());
        pool.close();
        assertTrue(pool.isClosed());
    }

    @Test
    public void testMultipleLockUnlock() {
        // Test multiple lock/unlock cycles
        for (int i = 0; i < 5; i++) {
            assertFalse(pool.isLocked());
            pool.lock();
            assertTrue(pool.isLocked());
            pool.unlock();
            assertFalse(pool.isLocked());
        }
    }

    @Test
    public void testPoolOperationsSequence() {
        // Test a typical sequence of pool operations
        assertTrue(pool.isEmpty());
        assertEquals(10, pool.capacity());
        
        pool.setSize(8);
        assertFalse(pool.isEmpty());
        assertEquals(8, pool.size());
        
        pool.vacate();
        assertEquals(6, pool.size());
        
        PoolStats stats = pool.stats();
        assertEquals(6, stats.size());
        
        pool.clear();
        assertTrue(pool.isEmpty());
        
        assertFalse(pool.isClosed());
        pool.close();
        assertTrue(pool.isClosed());
    }
}
