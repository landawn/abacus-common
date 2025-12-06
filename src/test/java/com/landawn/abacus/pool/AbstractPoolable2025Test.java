package com.landawn.abacus.pool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.pool.Poolable.Caller;

@Tag("2025")
public class AbstractPoolable2025Test extends TestBase {

    private static class TestPoolable extends AbstractPoolable {
        private boolean destroyed = false;
        private Caller destroyedBy = null;

        TestPoolable(long liveTime, long maxIdleTime) {
            super(liveTime, maxIdleTime);
        }

        @Override
        public void destroy(Caller caller) {
            destroyed = true;
            destroyedBy = caller;
        }

        boolean isDestroyed() {
            return destroyed;
        }

        Caller getDestroyedBy() {
            return destroyedBy;
        }
    }

    @Test
    public void testConstructorWithValidValues() {
        TestPoolable poolable = new TestPoolable(10000, 5000);

        assertNotNull(poolable);
        assertNotNull(poolable.activityPrint());
        assertEquals(10000, poolable.activityPrint().getLiveTime());
        assertEquals(5000, poolable.activityPrint().getMaxIdleTime());
    }

    @Test
    public void testConstructorWithMaxValues() {
        TestPoolable poolable = new TestPoolable(Long.MAX_VALUE, Long.MAX_VALUE);

        assertNotNull(poolable);
        assertEquals(Long.MAX_VALUE, poolable.activityPrint().getLiveTime());
        assertEquals(Long.MAX_VALUE, poolable.activityPrint().getMaxIdleTime());
    }

    @Test
    public void testConstructorWithInvalidLiveTime() {
        assertThrows(IllegalArgumentException.class, () -> new TestPoolable(0, 5000));
        assertThrows(IllegalArgumentException.class, () -> new TestPoolable(-1, 5000));
        assertThrows(IllegalArgumentException.class, () -> new TestPoolable(-100, 5000));
    }

    @Test
    public void testConstructorWithInvalidMaxIdleTime() {
        assertThrows(IllegalArgumentException.class, () -> new TestPoolable(10000, 0));
        assertThrows(IllegalArgumentException.class, () -> new TestPoolable(10000, -1));
        assertThrows(IllegalArgumentException.class, () -> new TestPoolable(10000, -100));
    }

    @Test
    public void testActivityPrintReturnsNonNull() {
        TestPoolable poolable = new TestPoolable(10000, 5000);

        ActivityPrint activityPrint = poolable.activityPrint();

        assertNotNull(activityPrint);
    }

    @Test
    public void testActivityPrintReturnsSameInstance() {
        TestPoolable poolable = new TestPoolable(10000, 5000);

        ActivityPrint firstCall = poolable.activityPrint();
        ActivityPrint secondCall = poolable.activityPrint();

        assertEquals(firstCall, secondCall);
        assertTrue(firstCall == secondCall);   // same instance
    }

    @Test
    public void testActivityPrintIsInitializedCorrectly() {
        long liveTime = 12345;
        long maxIdleTime = 6789;
        TestPoolable poolable = new TestPoolable(liveTime, maxIdleTime);

        ActivityPrint print = poolable.activityPrint();

        assertEquals(liveTime, print.getLiveTime());
        assertEquals(maxIdleTime, print.getMaxIdleTime());
        assertEquals(0, print.getAccessCount());
        assertTrue(print.getCreatedTime() > 0);
    }

    @Test
    public void testDestroyMethod() {
        TestPoolable poolable = new TestPoolable(10000, 5000);

        assertFalse(poolable.isDestroyed());

        poolable.destroy(Caller.CLOSE);

        assertTrue(poolable.isDestroyed());
        assertEquals(Caller.CLOSE, poolable.getDestroyedBy());
    }

    @Test
    public void testDestroyWithDifferentCallers() {
        for (Caller caller : Caller.values()) {
            TestPoolable poolable = new TestPoolable(10000, 5000);

            poolable.destroy(caller);

            assertTrue(poolable.isDestroyed());
            assertEquals(caller, poolable.getDestroyedBy());
        }
    }

    @Test
    public void testDestroyWithEvictCaller() {
        TestPoolable poolable = new TestPoolable(10000, 5000);

        poolable.destroy(Caller.EVICT);

        assertTrue(poolable.isDestroyed());
        assertEquals(Caller.EVICT, poolable.getDestroyedBy());
    }

    @Test
    public void testDestroyWithVacateCaller() {
        TestPoolable poolable = new TestPoolable(10000, 5000);

        poolable.destroy(Caller.VACATE);

        assertTrue(poolable.isDestroyed());
        assertEquals(Caller.VACATE, poolable.getDestroyedBy());
    }

    @Test
    public void testDestroyWithRemoveReplaceClearCaller() {
        TestPoolable poolable = new TestPoolable(10000, 5000);

        poolable.destroy(Caller.REMOVE_REPLACE_CLEAR);

        assertTrue(poolable.isDestroyed());
        assertEquals(Caller.REMOVE_REPLACE_CLEAR, poolable.getDestroyedBy());
    }

    @Test
    public void testDestroyWithPutAddFailureCaller() {
        TestPoolable poolable = new TestPoolable(10000, 5000);

        poolable.destroy(Caller.PUT_ADD_FAILURE);

        assertTrue(poolable.isDestroyed());
        assertEquals(Caller.PUT_ADD_FAILURE, poolable.getDestroyedBy());
    }

    @Test
    public void testDestroyWithOtherOuterCaller() {
        TestPoolable poolable = new TestPoolable(10000, 5000);

        poolable.destroy(Caller.OTHER_OUTER);

        assertTrue(poolable.isDestroyed());
        assertEquals(Caller.OTHER_OUTER, poolable.getDestroyedBy());
    }

    @Test
    public void testMultipleDestroyCalls() {
        TestPoolable poolable = new TestPoolable(10000, 5000);

        poolable.destroy(Caller.CLOSE);
        poolable.destroy(Caller.EVICT);

        assertTrue(poolable.isDestroyed());
        // Last caller should be EVICT
        assertEquals(Caller.EVICT, poolable.getDestroyedBy());
    }

    @Test
    public void testActivityPrintCanBeUpdated() {
        TestPoolable poolable = new TestPoolable(10000, 5000);
        ActivityPrint print = poolable.activityPrint();

        int initialAccessCount = print.getAccessCount();
        print.updateAccessCount();

        assertEquals(initialAccessCount + 1, print.getAccessCount());
    }

    @Test
    public void testActivityPrintExpirationDetection() throws InterruptedException {
        TestPoolable poolable = new TestPoolable(1, 1);

        assertFalse(poolable.activityPrint().isExpired());

        Thread.sleep(10);

        assertTrue(poolable.activityPrint().isExpired());
    }

    @Test
    public void testImplementsPoolableInterface() {
        TestPoolable poolable = new TestPoolable(10000, 5000);

        assertTrue(poolable instanceof Poolable);
    }

    @Test
    public void testActivityPrintFieldIsFinal() {
        TestPoolable poolable = new TestPoolable(10000, 5000);
        ActivityPrint firstRef = poolable.activityPrint();

        poolable.activityPrint().updateAccessCount();

        ActivityPrint secondRef = poolable.activityPrint();
        assertEquals(firstRef, secondRef);
    }

    private static class ConcretePoolable extends AbstractPoolable {
        ConcretePoolable(long liveTime, long maxIdleTime) {
            super(liveTime, maxIdleTime);
        }

        @Override
        public void destroy(Caller caller) {
            // no-op for testing
        }
    }

    @Test
    public void testMinimalConcreteImplementation() {
        ConcretePoolable poolable = new ConcretePoolable(10000, 5000);

        assertNotNull(poolable);
        assertNotNull(poolable.activityPrint());
        assertEquals(10000, poolable.activityPrint().getLiveTime());
        assertEquals(5000, poolable.activityPrint().getMaxIdleTime());

        // Should not throw
        poolable.destroy(Caller.CLOSE);
    }

    @Test
    public void testShortLivedPoolable() {
        TestPoolable poolable = new TestPoolable(100, 50);

        assertNotNull(poolable.activityPrint());
        assertFalse(poolable.activityPrint().isExpired());
    }

    @Test
    public void testLongLivedPoolable() {
        TestPoolable poolable = new TestPoolable(Long.MAX_VALUE, Long.MAX_VALUE);

        assertNotNull(poolable.activityPrint());
        assertFalse(poolable.activityPrint().isExpired());
    }
}
