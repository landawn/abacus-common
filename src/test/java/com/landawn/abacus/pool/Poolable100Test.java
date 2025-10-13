package com.landawn.abacus.pool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class Poolable100Test extends TestBase {

    @Test
    public void testWrapWithDefaultValues() {
        String testObject = "test";
        PoolableWrapper<String> wrapper = Poolable.wrap(testObject);

        assertNotNull(wrapper);
        assertEquals(testObject, wrapper.value());
        assertNotNull(wrapper.activityPrint());
        assertEquals(Long.MAX_VALUE, wrapper.activityPrint().getLiveTime());
        assertEquals(Long.MAX_VALUE, wrapper.activityPrint().getMaxIdleTime());
    }

    @Test
    public void testWrapWithCustomValues() {
        Integer testObject = 42;
        long liveTime = 10000;
        long maxIdleTime = 5000;

        PoolableWrapper<Integer> wrapper = Poolable.wrap(testObject, liveTime, maxIdleTime);

        assertNotNull(wrapper);
        assertEquals(testObject, wrapper.value());
        assertNotNull(wrapper.activityPrint());
        assertEquals(liveTime, wrapper.activityPrint().getLiveTime());
        assertEquals(maxIdleTime, wrapper.activityPrint().getMaxIdleTime());
    }

    @Test
    public void testWrapWithNullObject() {
        PoolableWrapper<Object> wrapper = Poolable.wrap(null);
        assertNotNull(wrapper);
        assertNull(wrapper.value());
        assertNotNull(wrapper.activityPrint());
    }

    @Test
    public void testWrapWithInvalidLiveTime() {
        assertThrows(IllegalArgumentException.class, () -> Poolable.wrap("test", 0, 5000));
        assertThrows(IllegalArgumentException.class, () -> Poolable.wrap("test", -1, 5000));
    }

    @Test
    public void testWrapWithInvalidMaxIdleTime() {
        assertThrows(IllegalArgumentException.class, () -> Poolable.wrap("test", 5000, 0));
        assertThrows(IllegalArgumentException.class, () -> Poolable.wrap("test", 5000, -1));
    }

    @Test
    public void testCallerEnum() {
        assertEquals(0, Poolable.Caller.CLOSE.value());
        assertEquals(1, Poolable.Caller.EVICT.value());
        assertEquals(2, Poolable.Caller.VACATE.value());
        assertEquals(3, Poolable.Caller.REMOVE_REPLACE_CLEAR.value());
        assertEquals(4, Poolable.Caller.PUT_ADD_FAILURE.value());
        assertEquals(5, Poolable.Caller.OTHER_OUTER.value());

        Poolable.Caller[] callers = Poolable.Caller.values();
        assertEquals(6, callers.length);

        assertEquals(Poolable.Caller.CLOSE, Poolable.Caller.valueOf("CLOSE"));
        assertEquals(Poolable.Caller.EVICT, Poolable.Caller.valueOf("EVICT"));
        assertEquals(Poolable.Caller.VACATE, Poolable.Caller.valueOf("VACATE"));
        assertEquals(Poolable.Caller.REMOVE_REPLACE_CLEAR, Poolable.Caller.valueOf("REMOVE_REPLACE_CLEAR"));
        assertEquals(Poolable.Caller.PUT_ADD_FAILURE, Poolable.Caller.valueOf("PUT_ADD_FAILURE"));
        assertEquals(Poolable.Caller.OTHER_OUTER, Poolable.Caller.valueOf("OTHER_OUTER"));
    }

    @Test
    public void testCallerValueMethod() {
        assertEquals(0, Poolable.Caller.CLOSE.value());
        assertEquals(1, Poolable.Caller.EVICT.value());
        assertEquals(2, Poolable.Caller.VACATE.value());
        assertEquals(3, Poolable.Caller.REMOVE_REPLACE_CLEAR.value());
        assertEquals(4, Poolable.Caller.PUT_ADD_FAILURE.value());
        assertEquals(5, Poolable.Caller.OTHER_OUTER.value());
    }

    private static class TestPoolable implements Poolable {
        private final ActivityPrint activityPrint;
        private boolean destroyed = false;
        private Poolable.Caller destroyedByCaller = null;

        TestPoolable(long liveTime, long maxIdleTime) {
            this.activityPrint = new ActivityPrint(liveTime, maxIdleTime);
        }

        @Override
        public ActivityPrint activityPrint() {
            return activityPrint;
        }

        @Override
        public void destroy(Caller caller) {
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
    public void testPoolableImplementation() {
        TestPoolable poolable = new TestPoolable(10000, 5000);

        assertNotNull(poolable.activityPrint());
        assertEquals(10000, poolable.activityPrint().getLiveTime());
        assertEquals(5000, poolable.activityPrint().getMaxIdleTime());

        assertFalse(poolable.isDestroyed());
        assertNull(poolable.getDestroyedByCaller());

        poolable.destroy(Poolable.Caller.EVICT);
        assertTrue(poolable.isDestroyed());
        assertEquals(Poolable.Caller.EVICT, poolable.getDestroyedByCaller());
    }
}
