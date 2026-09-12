package com.landawn.abacus.pool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.pool.Poolable.Caller;

public class AbstractPoolableTest extends TestBase {

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
    public void testActivityPrintCanBeUpdated() {
        TestPoolable poolable = new TestPoolable(10000, 5000);
        ActivityPrint print = poolable.activityPrint();

        int initialAccessCount = print.getAccessCount();
        print.updateAccessCount();

        assertEquals(initialAccessCount + 1, print.getAccessCount());
    }

    @Test
    public void testActivityPrintFieldIsFinal() {
        TestPoolable poolable = new TestPoolable(10000, 5000);
        ActivityPrint firstRef = poolable.activityPrint();

        poolable.activityPrint().updateAccessCount();

        ActivityPrint secondRef = poolable.activityPrint();
        assertEquals(firstRef, secondRef);
    }

    @Test
    public void testConstructorWithValidValues() {
        TestPoolable poolable = new TestPoolable(10000, 5000);

        assertNotNull(poolable);
        assertNotNull(poolable.activityPrint());
        assertEquals(10000, poolable.activityPrint().getMaxLiveTime());
        assertEquals(5000, poolable.activityPrint().getMaxIdleTime());
    }

    @Test
    public void testConstructorWithMaxValues() {
        TestPoolable poolable = new TestPoolable(Long.MAX_VALUE, Long.MAX_VALUE);

        assertNotNull(poolable);
        assertEquals(Long.MAX_VALUE, poolable.activityPrint().getMaxLiveTime());
        assertEquals(Long.MAX_VALUE, poolable.activityPrint().getMaxIdleTime());
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
        assertTrue(firstCall == secondCall); // same instance
    }

    @Test
    public void testActivityPrintIsInitializedCorrectly() {
        long liveTime = 12345;
        long maxIdleTime = 6789;
        TestPoolable poolable = new TestPoolable(liveTime, maxIdleTime);

        ActivityPrint print = poolable.activityPrint();

        assertEquals(liveTime, print.getMaxLiveTime());
        assertEquals(maxIdleTime, print.getMaxIdleTime());
        assertEquals(0, print.getAccessCount());
        assertTrue(print.getCreatedTime() > 0);
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

    @Test
    public void testActivityPrintExpirationDetection() throws InterruptedException {
        TestPoolable poolable = new TestPoolable(1, 1);

        assertFalse(poolable.activityPrint().isExpired());

        Thread.sleep(10);

        assertTrue(poolable.activityPrint().isExpired());
    }

    @Test
    public void testMinimalConcreteImplementation() {
        ConcretePoolable poolable = new ConcretePoolable(10000, 5000);

        assertNotNull(poolable);
        assertNotNull(poolable.activityPrint());
        assertEquals(10000, poolable.activityPrint().getMaxLiveTime());
        assertEquals(5000, poolable.activityPrint().getMaxIdleTime());

        // Should not throw
        poolable.destroy(Caller.CLOSE);
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

        poolable.destroy(Caller.OTHER_EXTERNAL);

        assertTrue(poolable.isDestroyed());
        assertEquals(Caller.OTHER_EXTERNAL, poolable.getDestroyedBy());
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
    public void testImplementsPoolableInterface() {
        TestPoolable poolable = new TestPoolable(10000, 5000);

        assertTrue(poolable instanceof Poolable);
    }

    private static byte[] serialize(final Object obj) throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(obj);
        }

        return baos.toByteArray();
    }

    @SuppressWarnings("unchecked")
    private static <T> T deserialize(final byte[] bytes) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            return (T) ois.readObject();
        }
    }

    /** A user subclass with its own serializable state. */
    private static class SerializablePoolable extends AbstractPoolable {
        private final String name;
        private boolean destroyed;

        SerializablePoolable(final String name, final long liveTime, final long maxIdleTime) {
            super(liveTime, maxIdleTime);
            this.name = name;
        }

        @Override
        public void destroy(final Caller caller) {
            destroyed = true;
        }
    }

    /** A user subclass whose own state is NOT serializable. */
    private static class NonSerializableStatePoolable extends AbstractPoolable {
        @SuppressWarnings("unused")
        private final Object handle = new Object();

        NonSerializableStatePoolable() {
            super(10000, 5000);
        }

        @Override
        public void destroy(final Caller caller) {
            // no-op
        }
    }

    @Test
    public void testIsSerializable() {
        // Pool extends Serializable and serializes its elements, so the recommended base class must be
        // Serializable too; before this, a Serializable subclass could be written but never read back
        // ("no valid constructor") because AbstractPoolable had no no-arg constructor.
        assertTrue(new TestPoolable(10000, 5000) instanceof Serializable);
    }

    @Test
    public void testSerializationRoundTripPreservesActivityPrintAndSubclassState() throws Exception {
        final SerializablePoolable original = new SerializablePoolable("resource-ü", 600_000, 60_000);
        original.activityPrint().updateAccessCount();
        original.activityPrint().updateAccessCount();
        original.activityPrint().updateAccessCount();
        original.activityPrint().updateLastAccessTime();
        final ActivityPrint before = original.activityPrint();

        final SerializablePoolable copy = deserialize(serialize(original));

        assertNotNull(copy);
        assertEquals("resource-ü", copy.name);
        assertFalse(copy.destroyed);
        assertNotNull(copy.activityPrint());
        assertEquals(before.getCreatedTime(), copy.activityPrint().getCreatedTime());
        assertEquals(before.getMaxLiveTime(), copy.activityPrint().getMaxLiveTime());
        assertEquals(before.getMaxIdleTime(), copy.activityPrint().getMaxIdleTime());
        assertEquals(before.getLastAccessTime(), copy.activityPrint().getLastAccessTime());
        assertEquals(3, copy.activityPrint().getAccessCount());
        assertFalse(copy.activityPrint().isExpired());

        // The copy has its own ActivityPrint instance (independent mutation), still tracked correctly.
        copy.activityPrint().updateAccessCount();
        assertEquals(4, copy.activityPrint().getAccessCount());
        assertEquals(3, original.activityPrint().getAccessCount());
    }

    @Test
    public void testSerializationRoundTripPreservesDestroyedState() throws Exception {
        final SerializablePoolable original = new SerializablePoolable("r", 10000, 5000);
        original.destroy(Caller.CLOSE);

        final SerializablePoolable copy = deserialize(serialize(original));

        assertTrue(copy.destroyed);
    }

    @Test
    public void testSerializationOfSubclassWithNonSerializableStateFails() {
        // Documented: a subclass is serializable only if all of its own state is.
        assertThrows(NotSerializableException.class, () -> serialize(new NonSerializableStatePoolable()));
    }

}
