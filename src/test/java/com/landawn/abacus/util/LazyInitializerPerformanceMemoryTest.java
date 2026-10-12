package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/** Performance and memory regression contracts for LazyInitializer. */
@Tag("slow-test")
public class LazyInitializerPerformanceMemoryTest extends TestBase {

    // ---- perf review 2026-10-08 G041 end ----
    // ---- perf review 2026-10-08 G042 begin ----
    @Nested
    class G042 {
        private java.lang.reflect.Field lazyField(final String name) throws Exception {
            final java.lang.reflect.Field field = LazyInitializer.class.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        }

        // G042-02: an attempt rejected because the supplier swallowed the recursive-access failure must not stay referenced
        @Test
        public void testGet_rejectedRecursiveAttemptDoesNotRetainValue() throws Exception {
            final StringBuilder rejected = new StringBuilder("rejected value");
            final AtomicInteger attempts = new AtomicInteger();
            @SuppressWarnings("unchecked")
            final com.landawn.abacus.util.function.Supplier<Object>[] self = new com.landawn.abacus.util.function.Supplier[1];

            self[0] = N.lazyInit(() -> {
                if (attempts.incrementAndGet() == 1) {
                    try {
                        self[0].get();
                    } catch (final IllegalStateException swallowed) {
                        // the supplier swallows the recursive-access failure and returns normally
                    }

                    return rejected;
                }

                return "ok";
            });

            assertTrue(self[0] instanceof LazyInitializer);

            final IllegalStateException failure = assertThrows(IllegalStateException.class, self[0]::get);
            assertEquals("Recursive initialization of deferred value", failure.getMessage());
            assertEquals(1, attempts.get());

            assertNull(lazyField("value").get(self[0]), "the rejected value must not stay referenced by the initializer");
            assertNotNull(lazyField("supplier").get(self[0]), "a failed attempt keeps the supplier for the retry");

            assertEquals("ok", self[0].get());
            assertEquals("ok", self[0].get());
            assertEquals(2, attempts.get());
            assertEquals("ok", lazyField("value").get(self[0]));
            assertNull(lazyField("supplier").get(self[0]));
        }

        // G042-02: every attempt rejected (supplier always swallows the recursion): fresh ISE per get(), one supplier call each, nothing kept
        @Test
        public void testGet_repeatedRejectedAttemptsKeepNothingAndStayRetryable() throws Exception {
            final AtomicInteger attempts = new AtomicInteger();
            final List<Object> produced = new ArrayList<>();
            @SuppressWarnings("unchecked")
            final LazyInitializer<Object>[] self = new LazyInitializer[1];

            self[0] = LazyInitializer.of(() -> {
                attempts.incrementAndGet();

                try {
                    self[0].get();
                } catch (final IllegalStateException swallowed) {
                    // swallowed every time
                }

                final Object result = new Object();
                produced.add(result);
                return result;
            });

            IllegalStateException previous = null;

            for (int i = 1; i <= 3; i++) {
                final IllegalStateException failure = assertThrows(IllegalStateException.class, self[0]::get);
                assertEquals("Recursive initialization of deferred value", failure.getMessage());
                assertNotSame(previous, failure);
                previous = failure;

                assertEquals(i, attempts.get());
                assertNull(lazyField("value").get(self[0]), "attempt " + i);
                assertFalse((Boolean) lazyField("initialized").get(self[0]), "attempt " + i);
            }

            assertEquals(3, produced.size());
        }

        // G042-02: unchanged ordinary paths: a successful (also null) value is cached and the supplier released; a throwing supplier is retried
        @Test
        public void testGet_successAndThrowingSupplierPathsUnchanged() throws Exception {
            final AtomicInteger calls = new AtomicInteger();
            final LazyInitializer<String> nullValue = LazyInitializer.of(() -> {
                calls.incrementAndGet();
                return null;
            });

            assertNull(nullValue.get());
            assertNull(nullValue.get());
            assertEquals(1, calls.get());
            assertNull(lazyField("supplier").get(nullValue));
            assertTrue((Boolean) lazyField("initialized").get(nullValue));

            final AtomicInteger attempts = new AtomicInteger();
            final LazyInitializer<String> flaky = LazyInitializer.of(() -> {
                if (attempts.incrementAndGet() < 3) {
                    throw new UnsupportedOperationException("attempt " + attempts.get());
                }

                return "third";
            });

            assertEquals("attempt 1", assertThrows(UnsupportedOperationException.class, flaky::get).getMessage());
            assertNull(lazyField("value").get(flaky));
            assertEquals("attempt 2", assertThrows(UnsupportedOperationException.class, flaky::get).getMessage());
            assertEquals("third", flaky.get());
            assertEquals("third", flaky.get());
            assertEquals(3, attempts.get());
        }
    }
}
