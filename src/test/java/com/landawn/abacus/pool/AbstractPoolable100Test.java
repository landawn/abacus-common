package com.landawn.abacus.pool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;


public class AbstractPoolable100Test extends TestBase {

    private static class TestAbstractPoolable extends AbstractPoolable {
        private boolean destroyed = false;
        private Poolable.Caller destroyedByCaller = null;

        TestAbstractPoolable(long liveTime, long maxIdleTime) {
            super(liveTime, maxIdleTime);
        }

        @Override
        public void destroy(Poolable.Caller caller) {
            destroyed = true;
            destroyedByCaller = caller;
        }

        boolean isDestroyed() {
            return destroyed;
        }

        Poolable.Caller getDestroyedByCaller() {
            return destroyedByCaller;
        }
    }

    @Test
    public void testConstructor() {
        long liveTime = 10000;
        long maxIdleTime = 5000;
        TestAbstractPoolable poolable = new TestAbstractPoolable(liveTime, maxIdleTime);
        
        assertNotNull(poolable);
        assertNotNull(poolable.activityPrint());
        assertEquals(liveTime, poolable.activityPrint().getLiveTime());
        assertEquals(maxIdleTime, poolable.activityPrint().getMaxIdleTime());
    }

    @Test
    public void testConstructorWithInvalidLiveTime() {
        assertThrows(IllegalArgumentException.class, () -> new TestAbstractPoolable(0, 5000));
        assertThrows(IllegalArgumentException.class, () -> new TestAbstractPoolable(-1, 5000));
    }

    @Test
    public void testConstructorWithInvalidMaxIdleTime() {
        assertThrows(IllegalArgumentException.class, () -> new TestAbstractPoolable(10000, 0));
        assertThrows(IllegalArgumentException.class, () -> new TestAbstractPoolable(10000, -1));
    }

    @Test
    public void testActivityPrint() {
        TestAbstractPoolable poolable = new TestAbstractPoolable(10000, 5000);
        ActivityPrint activityPrint = poolable.activityPrint();
        
        assertNotNull(activityPrint);
        assertEquals(10000, activityPrint.getLiveTime());
        assertEquals(5000, activityPrint.getMaxIdleTime());
        
        // Test that same instance is returned
        assertSame(activityPrint, poolable.activityPrint());
    }

    @Test
    public void testDestroy() {
        TestAbstractPoolable poolable = new TestAbstractPoolable(10000, 5000);
        
        assertFalse(poolable.isDestroyed());
        assertNull(poolable.getDestroyedByCaller());
        
        poolable.destroy(Poolable.Caller.CLOSE);
        assertTrue(poolable.isDestroyed());
        assertEquals(Poolable.Caller.CLOSE, poolable.getDestroyedByCaller());
    }

    @Test
    public void testDestroyWithDifferentCallers() {
        Poolable.Caller[] callers = Poolable.Caller.values();
        
        for (Poolable.Caller caller : callers) {
            TestAbstractPoolable poolable = new TestAbstractPoolable(10000, 5000);
            poolable.destroy(caller);
            assertTrue(poolable.isDestroyed());
            assertEquals(caller, poolable.getDestroyedByCaller());
        }
    }

    @Test
    public void testActivityPrintOperations() {
        TestAbstractPoolable poolable = new TestAbstractPoolable(10000, 5000);
        ActivityPrint activityPrint = poolable.activityPrint();
        
        // Test initial state
        assertEquals(0, activityPrint.getAccessCount());
        long initialLastAccessTime = activityPrint.getLastAccessTime();
        
        // Test update operations
        activityPrint.updateAccessCount();
        assertEquals(1, activityPrint.getAccessCount());
        
        try {
            Thread.sleep(10); // Ensure time difference
        } catch (InterruptedException e) {
            // Ignore
        }
        
        activityPrint.updateLastAccessTime();
        assertTrue(activityPrint.getLastAccessTime() > initialLastAccessTime);
    }

    @Test
    public void testExpiration() throws InterruptedException {
        TestAbstractPoolable poolable = new TestAbstractPoolable(50, 100);
        assertFalse(poolable.activityPrint().isExpired());
        
        Thread.sleep(60);
        assertTrue(poolable.activityPrint().isExpired());
    }
}
