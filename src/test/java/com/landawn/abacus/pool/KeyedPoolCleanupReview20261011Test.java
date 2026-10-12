package com.landawn.abacus.pool;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.landawn.abacus.TestBase;

@Tag("unit")
class KeyedPoolCleanupReview20261011Test extends TestBase {

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void clearDoesNotRehashKeysAndResetsMemory(final boolean measured) {
        final Key key = new Key();
        final Resource value = new Resource();

        try (GenericKeyedObjectPool<Key, Resource> pool = newPool(measured)) {
            assertTrue(pool.put(key, value));
            final int callsAfterAdmission = key.hashCalls;
            key.failHash = true;

            assertDoesNotThrow(pool::clear);
            assertEquals(callsAfterAdmission, key.hashCalls);
            assertEquals(0, pool.size());
            assertEquals(measured ? 0 : -1, pool.stats().dataSize());
            assertEquals(0, pool.stats().evictionCount());
            assertEquals(1, value.destroyCount);
            assertSame(Poolable.Caller.REMOVE_REPLACE_CLEAR, value.caller);

            assertTrue(pool.put(new Key(), new Resource()));
            assertEquals(measured ? 10 : -1, pool.stats().dataSize());
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void closeReleasesResourcesWithoutInvokingKeys(final boolean measured) {
        final Key key = new Key();
        final Resource value = new Resource();
        final GenericKeyedObjectPool<Key, Resource> pool = newPool(measured);

        try {
            assertTrue(pool.put(key, value));
            final int callsAfterAdmission = key.hashCalls;
            key.failHash = true;

            assertDoesNotThrow(pool::close);
            assertTrue(pool.isClosed());
            assertEquals(callsAfterAdmission, key.hashCalls);
            assertEquals(1, value.destroyCount);
            assertSame(Poolable.Caller.CLOSE, value.caller);
            assertEquals(0, pool.totalDataSize.get());
            assertTrue(pool.pool.isEmpty());
            pool.close();
            assertEquals(1, value.destroyCount);
        } finally {
            key.failHash = false;
            pool.close();
        }
    }

    @Test
    void clearAccountsBeforeUnlockedCallbacksCanAdmitNewValues() {
        try (GenericKeyedObjectPool<Key, Resource> pool = newPool(true)) {
            final Resource value = new Resource();
            final Resource replacement = new Resource();
            final AtomicBoolean callbackHeldLock = new AtomicBoolean(true);
            final AtomicBoolean admitted = new AtomicBoolean();
            value.onDestroy = () -> {
                callbackHeldLock.set(pool.lock.isHeldByCurrentThread());
                admitted.set(pool.put(new Key(), replacement));
            };
            assertTrue(pool.put(new Key(), value));

            pool.clear();

            assertFalse(callbackHeldLock.get());
            assertTrue(admitted.get());
            assertEquals(1, pool.size());
            assertEquals(10, pool.stats().dataSize());
            assertEquals(0, replacement.destroyCount);
        }
    }

    @Test
    void clearPreservesOneDestructionPerMapping() {
        final Resource value = new Resource();
        try (GenericKeyedObjectPool<Key, Resource> pool = newPool(false)) {
            assertTrue(pool.put(new Key(), value));
            assertTrue(pool.put(new Key(), value));
            pool.clear();
            assertEquals(2, value.destroyCount);
            assertEquals(0, pool.size());
        }
    }

    private static GenericKeyedObjectPool<Key, Resource> newPool(final boolean measured) {
        return new GenericKeyedObjectPool<>(2, 0, EvictionPolicy.FIFO, false, 0.5f, measured ? 10 : 0, measured ? (key, value) -> 10 : null);
    }

    private static final class Key {
        private int hashCalls;
        private boolean failHash;

        @Override
        public int hashCode() {
            hashCalls++;
            if (failHash) {
                throw new IllegalStateException("Key is no longer available for hashing");
            }
            return System.identityHashCode(this);
        }
    }

    private static final class Resource extends AbstractPoolable {
        private int destroyCount;
        private Caller caller;
        private Runnable onDestroy = () -> {
        };

        private Resource() {
            super(Long.MAX_VALUE, Long.MAX_VALUE);
        }

        @Override
        public void destroy(final Caller caller) {
            destroyCount++;
            this.caller = caller;
            onDestroy.run();
        }
    }
}
