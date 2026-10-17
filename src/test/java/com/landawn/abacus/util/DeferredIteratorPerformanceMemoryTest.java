package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class DeferredIteratorPerformanceMemoryTest extends TestBase {

    @Test
    public void testBooleanDeferReleasesRejectedSource() throws Exception {
        DeferredIteratorPerformanceMemoryTest.<BooleanIterator> verifyRejectedSourceReleased(BooleanIterator::defer, BooleanIterator.of(true));
    }

    @Test
    public void testByteDeferReleasesRejectedSource() throws Exception {
        DeferredIteratorPerformanceMemoryTest.<ByteIterator> verifyRejectedSourceReleased(ByteIterator::defer, ByteIterator.of((byte) 1));
    }

    @Test
    public void testCharDeferReleasesRejectedSource() throws Exception {
        DeferredIteratorPerformanceMemoryTest.<CharIterator> verifyRejectedSourceReleased(CharIterator::defer, CharIterator.of('a'));
    }

    @Test
    public void testShortDeferReleasesRejectedSource() throws Exception {
        DeferredIteratorPerformanceMemoryTest.<ShortIterator> verifyRejectedSourceReleased(ShortIterator::defer, ShortIterator.of((short) 1));
    }

    @Test
    public void testIntDeferReleasesRejectedSource() throws Exception {
        DeferredIteratorPerformanceMemoryTest.<IntIterator> verifyRejectedSourceReleased(IntIterator::defer, IntIterator.of(1));
    }

    @Test
    public void testLongDeferReleasesRejectedSource() throws Exception {
        DeferredIteratorPerformanceMemoryTest.<LongIterator> verifyRejectedSourceReleased(LongIterator::defer, LongIterator.of(1L));
    }

    @Test
    public void testFloatDeferReleasesRejectedSource() throws Exception {
        DeferredIteratorPerformanceMemoryTest.<FloatIterator> verifyRejectedSourceReleased(FloatIterator::defer, FloatIterator.of(1f));
    }

    @Test
    public void testDoubleDeferReleasesRejectedSource() throws Exception {
        DeferredIteratorPerformanceMemoryTest.<DoubleIterator> verifyRejectedSourceReleased(DoubleIterator::defer, DoubleIterator.of(1d));
    }

    @Test
    public void testObjectDeferReleasesRejectedSource() throws Exception {
        verifyRejectedSourceReleased(ObjIterator::defer, ObjIterator.of(new Object()));
    }

    private static <I extends ImmutableIterator<?>> void verifyRejectedSourceReleased(final DeferredFactory<I> factory, final I source) throws Exception {
        for (int mode = 0; mode < 2; mode++) {
            final AtomicReference<I> self = new AtomicReference<>();
            final AtomicReference<IllegalStateException> recursiveFailure = new AtomicReference<>();
            final AtomicInteger supplierCalls = new AtomicInteger();
            final I deferred = factory.create(() -> {
                supplierCalls.incrementAndGet();
                recursiveFailure.set(assertThrows(IllegalStateException.class, self.get()::hasNext));
                return source;
            });
            self.set(deferred);

            final IllegalStateException failure = mode == 0 ? assertThrows(IllegalStateException.class, deferred::hasNext)
                    : assertThrows(IllegalStateException.class, deferred::next);
            assertSame(recursiveFailure.get(), failure);
            assertSame(failure, assertThrows(IllegalStateException.class, deferred::hasNext));
            assertSame(failure, assertThrows(IllegalStateException.class, deferred::next));
            assertEquals(1, supplierCalls.get());
            assertTrue(source.hasNext(), "A rejected source must not be traversed");

            // Inspect the retained reference directly so this memory regression does not depend on GC timing.
            final Field sourceField = deferred.getClass().getDeclaredField("iter");
            sourceField.setAccessible(true);
            assertNull(sourceField.get(deferred), "A permanently failed iterator must release the rejected source and its backing data");
        }
    }

    @FunctionalInterface
    private interface DeferredFactory<I extends ImmutableIterator<?>> {
        I create(Supplier<? extends I> supplier);
    }
}
