package com.landawn.abacus.pool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class Poolable2025Test extends TestBase {

    @Test
    public void testWrapWithDefaultValues() {
        String testObject = "test-object";
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
        assertThrows(IllegalArgumentException.class, () -> Poolable.wrap("test", -100, 5000));
    }

    @Test
    public void testWrapWithInvalidMaxIdleTime() {
        assertThrows(IllegalArgumentException.class, () -> Poolable.wrap("test", 5000, 0));
        assertThrows(IllegalArgumentException.class, () -> Poolable.wrap("test", 5000, -1));
        assertThrows(IllegalArgumentException.class, () -> Poolable.wrap("test", 5000, -100));
    }

    @Test
    public void testWrapWithBothInvalidTimes() {
        assertThrows(IllegalArgumentException.class, () -> Poolable.wrap("test", 0, 0));
        assertThrows(IllegalArgumentException.class, () -> Poolable.wrap("test", -1, -1));
    }

    @Test
    public void testCallerEnumValues() {
        Poolable.Caller[] callers = Poolable.Caller.values();

        assertNotNull(callers);
        assertEquals(6, callers.length);
    }

    @Test
    public void testCallerEnumValueOf() {
        assertEquals(Poolable.Caller.CLOSE, Poolable.Caller.valueOf("CLOSE"));
        assertEquals(Poolable.Caller.EVICT, Poolable.Caller.valueOf("EVICT"));
        assertEquals(Poolable.Caller.VACATE, Poolable.Caller.valueOf("VACATE"));
        assertEquals(Poolable.Caller.REMOVE_REPLACE_CLEAR, Poolable.Caller.valueOf("REMOVE_REPLACE_CLEAR"));
        assertEquals(Poolable.Caller.PUT_ADD_FAILURE, Poolable.Caller.valueOf("PUT_ADD_FAILURE"));
        assertEquals(Poolable.Caller.OTHER_EXTERNAL, Poolable.Caller.valueOf("OTHER_EXTERNAL"));
    }

    @Test
    public void testCallerValue() {
        assertEquals(0, Poolable.Caller.CLOSE.value());
        assertEquals(1, Poolable.Caller.EVICT.value());
        assertEquals(2, Poolable.Caller.VACATE.value());
        assertEquals(3, Poolable.Caller.REMOVE_REPLACE_CLEAR.value());
        assertEquals(4, Poolable.Caller.PUT_ADD_FAILURE.value());
        assertEquals(5, Poolable.Caller.OTHER_EXTERNAL.value());
    }

    @Test
    public void testCallerOrdinal() {
        assertEquals(0, Poolable.Caller.CLOSE.ordinal());
        assertEquals(1, Poolable.Caller.EVICT.ordinal());
        assertEquals(2, Poolable.Caller.VACATE.ordinal());
        assertEquals(3, Poolable.Caller.REMOVE_REPLACE_CLEAR.ordinal());
        assertEquals(4, Poolable.Caller.PUT_ADD_FAILURE.ordinal());
        assertEquals(5, Poolable.Caller.OTHER_EXTERNAL.ordinal());
    }

    @Test
    public void testCallerClose() {
        Poolable.Caller caller = Poolable.Caller.CLOSE;

        assertNotNull(caller);
        assertEquals("CLOSE", caller.name());
        assertEquals(0, caller.value());
    }

    @Test
    public void testCallerEvict() {
        Poolable.Caller caller = Poolable.Caller.EVICT;

        assertNotNull(caller);
        assertEquals("EVICT", caller.name());
        assertEquals(1, caller.value());
    }

    @Test
    public void testCallerVacate() {
        Poolable.Caller caller = Poolable.Caller.VACATE;

        assertNotNull(caller);
        assertEquals("VACATE", caller.name());
        assertEquals(2, caller.value());
    }

    @Test
    public void testCallerRemoveReplaceClear() {
        Poolable.Caller caller = Poolable.Caller.REMOVE_REPLACE_CLEAR;

        assertNotNull(caller);
        assertEquals("REMOVE_REPLACE_CLEAR", caller.name());
        assertEquals(3, caller.value());
    }

    @Test
    public void testCallerPutAddFailure() {
        Poolable.Caller caller = Poolable.Caller.PUT_ADD_FAILURE;

        assertNotNull(caller);
        assertEquals("PUT_ADD_FAILURE", caller.name());
        assertEquals(4, caller.value());
    }

    @Test
    public void testCallerOtherOuter() {
        Poolable.Caller caller = Poolable.Caller.OTHER_EXTERNAL;

        assertNotNull(caller);
        assertEquals("OTHER_EXTERNAL", caller.name());
        assertEquals(5, caller.value());
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

    @Test
    public void testPoolableDestroyWithDifferentCallers() {
        for (Poolable.Caller caller : Poolable.Caller.values()) {
            TestPoolable poolable = new TestPoolable(10000, 5000);

            poolable.destroy(caller);

            assertTrue(poolable.isDestroyed());
            assertEquals(caller, poolable.getDestroyedByCaller());
        }
    }

    @Test
    public void testActivityPrintIsUpdated() {
        TestPoolable poolable = new TestPoolable(10000, 5000);
        ActivityPrint print = poolable.activityPrint();

        long initialAccessTime = print.getLastAccessTime();
        int initialAccessCount = print.getAccessCount();

        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            // ignore
        }

        print.updateLastAccessTime();
        print.updateAccessCount();

        assertTrue(print.getLastAccessTime() >= initialAccessTime);
        assertEquals(initialAccessCount + 1, print.getAccessCount());
    }
}
