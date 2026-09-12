package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class TryTest extends TestBase {

    private static class TestCloseable implements AutoCloseable {
        private boolean closed;

        public boolean isClosed() {
            return closed;
        }

        @Override
        public void close() {
            closed = true;
        }
    }

    private static class ThrowOnClose implements AutoCloseable {
        private final RuntimeException onClose;
        boolean closeCalled;

        ThrowOnClose(RuntimeException onClose) {
            this.onClose = onClose;
        }

        @Override
        public void close() {
            closeCalled = true;
            if (onClose != null) {
                throw onClose;
            }
        }
    }

    @Test
    public void testWith() {
        TestCloseable c = new TestCloseable();
        Try.with(c).run(r -> assertFalse(r.isClosed()));
        assertTrue(c.isClosed());

        AtomicBoolean finalAction = new AtomicBoolean();
        assertEquals("ok", Try.with(new ByteArrayInputStream("t".getBytes()), () -> finalAction.set(true)).call(s -> "ok"));
        assertTrue(finalAction.get());

        Throwables.Supplier<InputStream, Exception> supplier = () -> new ByteArrayInputStream("t".getBytes());
        AtomicBoolean ran = new AtomicBoolean();
        Try.with(supplier).run(s -> ran.set(true));
        assertTrue(ran.get());
        Try.with(supplier, () -> finalAction.set(true)).run(s -> {
        });
        assertTrue(finalAction.get());

        assertThrows(IllegalArgumentException.class, () -> Try.with((InputStream) null));
        assertThrows(IllegalArgumentException.class, () -> Try.with((InputStream) null, () -> {
        }));
        assertThrows(IllegalArgumentException.class, () -> Try.with(new ByteArrayInputStream("t".getBytes()), (Runnable) null));
        assertThrows(IllegalArgumentException.class, () -> Try.with((Throwables.Supplier<InputStream, Exception>) null));
        assertThrows(IllegalArgumentException.class, () -> Try.with((Throwables.Supplier<InputStream, Exception>) null, () -> {
        }));
        assertThrows(IllegalArgumentException.class, () -> Try.with(supplier, (Runnable) null));
        assertThrows(RuntimeException.class, () -> Try.with((Throwables.Supplier<InputStream, Exception>) () -> {
            throw new IOException("supplier");
        }).run(s -> fail("must not run")));

        AtomicBoolean bodyRan = new AtomicBoolean();
        AtomicBoolean finalRan = new AtomicBoolean();
        assertThrows(IllegalArgumentException.class,
                () -> Try.with((Throwables.Supplier<AutoCloseable, Exception>) () -> null, () -> finalRan.set(true)).run(x -> bodyRan.set(true)));
        assertFalse(bodyRan.get());
        assertTrue(finalRan.get());
    }

    // Kept in its own test so the only assertion in this class that can go red against an unpatched build is
    // not hidden behind a dozen unrelated assertThrows calls. @MayReturnNull says "this method may return
    // null", which is meaningless on a void method and was rendered into the published signature; the
    // annotation is RUNTIME-retained, so its absence is observable by reflection.
    @Test
    public void testVoidRunOverloadsAreNotAnnotatedMayReturnNull() throws Exception {
        assertNull(Try.class.getMethod("run", Throwables.Consumer.class)
                .getAnnotation(com.landawn.abacus.annotation.MayReturnNull.class));
        assertNull(Try.class.getMethod("run", Throwables.Consumer.class, java.util.function.Consumer.class)
                .getAnnotation(com.landawn.abacus.annotation.MayReturnNull.class));
        // both really are void, which is why the annotation could never have been meaningful
        assertSame(void.class, Try.class.getMethod("run", Throwables.Consumer.class).getReturnType());
        assertSame(void.class, Try.class.getMethod("run", Throwables.Consumer.class, java.util.function.Consumer.class).getReturnType());
    }

    @Test
    public void testInstanceRunAndCall() {
        TestCloseable c = new TestCloseable();
        Try<TestCloseable> t = Try.with(c);
        Throwables.Function<TestCloseable, String, Exception> cmd = r -> "result";
        assertThrows(IllegalArgumentException.class, () -> t.run((Throwables.Consumer<TestCloseable, Exception>) null));
        assertThrows(IllegalArgumentException.class, () -> t.run(r -> {
        }, (java.util.function.Consumer<Exception>) null));
        assertThrows(IllegalArgumentException.class, () -> t.call((Throwables.Function<TestCloseable, String, Exception>) null));
        assertThrows(IllegalArgumentException.class, () -> t.call(cmd, (java.util.function.Function<Exception, String>) null));
        assertThrows(IllegalArgumentException.class, () -> t.call(cmd, (java.util.function.Supplier<String>) null));
        assertThrows(IllegalArgumentException.class, () -> t.call((Throwables.Function<TestCloseable, String, Exception>) null, "d"));
        assertThrows(IllegalArgumentException.class, () -> t.call(cmd, (Predicate<Exception>) null, (java.util.function.Supplier<String>) () -> "d"));
        assertThrows(IllegalArgumentException.class, () -> t.call(cmd, e -> true, (java.util.function.Supplier<String>) null));
        assertThrows(IllegalArgumentException.class, () -> t.call(cmd, (Predicate<Exception>) null, "d"));
        assertFalse(c.isClosed());

        AtomicBoolean executed = new AtomicBoolean();
        Try.with(new ByteArrayInputStream("t".getBytes())).run(s -> executed.set(true));
        assertTrue(executed.get());
        assertThrows(RuntimeException.class, () -> Try.with(new ByteArrayInputStream("t".getBytes())).run(s -> {
            throw new IOException("boom");
        }));

        AtomicBoolean handled = new AtomicBoolean();
        Try.with(new ByteArrayInputStream("t".getBytes())).run(s -> executed.set(true), e -> handled.set(true));
        assertFalse(handled.get());
        Try.with(new ByteArrayInputStream("t".getBytes())).run(s -> {
            throw new IOException("boom");
        }, e -> handled.set(true));
        assertTrue(handled.get());
        TestCloseable closedOnError = new TestCloseable();
        Try.with(closedOnError).run(r -> {
            throw new IOException("boom");
        }, e -> {
        });
        assertTrue(closedOnError.isClosed());

        assertEquals("result", Try.with(new ByteArrayInputStream("t".getBytes())).call(s -> "result"));
        assertEquals("result", Try.with(new ByteArrayInputStream("t".getBytes())).call(s -> "result", () -> "d"));
        assertEquals("d", Try.with(new ByteArrayInputStream("t".getBytes())).call(s -> {
            throw new IOException("boom");
        }, () -> "d"));
        assertEquals("result", Try.with(new ByteArrayInputStream("t".getBytes())).call(s -> "result", "d"));
        assertEquals("d", Try.with(new ByteArrayInputStream("t".getBytes())).call(s -> {
            throw new IOException("boom");
        }, "d"));
        assertEquals("error", Try.with(new ByteArrayInputStream("t".getBytes())).call(s -> {
            throw new IOException("boom");
        }, e -> "error"));
        assertEquals("result", Try.with(new ByteArrayInputStream("t".getBytes())).call(s -> "result", e -> e instanceof IOException, () -> "d"));
        assertEquals("d", Try.with(new ByteArrayInputStream("t".getBytes())).call(s -> {
            throw new IOException("boom");
        }, e -> e instanceof IOException, () -> "d"));
        assertThrows(RuntimeException.class, () -> Try.with(new ByteArrayInputStream("t".getBytes())).call(s -> {
            throw new IOException("boom");
        }, e -> e instanceof FileNotFoundException, () -> "d"));
        assertEquals("d", Try.with(new ByteArrayInputStream("t".getBytes())).call(s -> {
            throw new IOException("boom");
        }, e -> e instanceof IOException, "d"));

        Throwables.Supplier<InputStream, Exception> supplier = () -> new ByteArrayInputStream("t".getBytes());
        assertEquals("result", Try.with(supplier).call(s -> "result"));
        assertEquals("error", Try.with(supplier).call(s -> {
            throw new IOException("boom");
        }, e -> "error"));
        TestCloseable supplierClosed = new TestCloseable();
        Try.with((Throwables.Supplier<TestCloseable, Exception>) () -> supplierClosed).call(r -> {
            throw new IOException("boom");
        }, () -> "d");
        assertTrue(supplierClosed.isClosed());
    }

    @Test
    public void testStaticRunAndCall() {
        AtomicBoolean ran = new AtomicBoolean();
        Try.run(() -> ran.set(true));
        assertTrue(ran.get());
        assertThrows(RuntimeException.class, () -> Try.run(() -> {
            throw new IOException("boom");
        }));
        assertThrows(IllegalArgumentException.class, () -> Try.run((Throwables.Runnable<Exception>) null));

        AtomicBoolean handled = new AtomicBoolean();
        Try.run(() -> ran.set(true), e -> handled.set(true));
        assertFalse(handled.get());
        AtomicReference<Exception> caught = new AtomicReference<>();
        Try.run(() -> {
            throw new IOException("boom");
        }, e -> {
            handled.set(true);
            caught.set(e);
        });
        assertTrue(handled.get());
        assertTrue(caught.get() instanceof IOException);
        assertThrows(IllegalArgumentException.class, () -> Try.run((Throwables.Runnable<Exception>) null, e -> {
        }));
        assertThrows(IllegalArgumentException.class, () -> Try.run(() -> {
        }, null));

        assertEquals("ok", Try.call(() -> "ok"));
        assertThrows(RuntimeException.class, () -> Try.call(() -> {
            throw new IOException("boom");
        }));
        assertThrows(IllegalArgumentException.class, () -> Try.call((java.util.concurrent.Callable<String>) null));
        assertEquals("ok", Try.call(() -> "ok", e -> "err"));
        assertEquals("err", Try.call(() -> {
            throw new IOException("boom");
        }, e -> "err"));
        assertEquals("ok", Try.call(() -> "ok", () -> "d"));
        assertEquals("d", Try.call(() -> {
            throw new IOException("boom");
        }, () -> "d"));
        assertEquals("ok", Try.call(() -> "ok", "d"));
        assertEquals("d", Try.call(() -> {
            throw new IOException("boom");
        }, "d"));
        assertNull(Try.call(() -> {
            throw new IOException("boom");
        }, (String) null));
        assertEquals("ok", Try.call(() -> "ok", e -> e instanceof IOException, () -> "d"));
        assertEquals("d", Try.call(() -> {
            throw new IOException("boom");
        }, e -> e instanceof IOException, () -> "d"));
        assertThrows(RuntimeException.class, () -> Try.call(() -> {
            throw new IOException("boom");
        }, e -> e instanceof FileNotFoundException, () -> "d"));
        assertEquals("d", Try.call(() -> {
            throw new IOException("boom");
        }, e -> e instanceof IOException, "d"));
        assertThrows(RuntimeException.class, () -> Try.call(() -> {
            throw new IOException("boom");
        }, e -> e instanceof FileNotFoundException, "d"));
        assertThrows(IllegalArgumentException.class, () -> Try.call((java.util.concurrent.Callable<String>) null, e -> "d"));
        assertThrows(IllegalArgumentException.class, () -> Try.call(() -> "ok", (java.util.function.Function<Exception, String>) null));
        assertThrows(IllegalArgumentException.class, () -> Try.call((java.util.concurrent.Callable<String>) null, () -> "d"));
        assertThrows(IllegalArgumentException.class, () -> Try.call(() -> "ok", (java.util.function.Supplier<String>) null));
        assertThrows(IllegalArgumentException.class, () -> Try.call((java.util.concurrent.Callable<String>) null, "d"));
        assertThrows(IllegalArgumentException.class, () -> Try.call((java.util.concurrent.Callable<String>) null, e -> true, () -> "d"));
        assertThrows(IllegalArgumentException.class, () -> Try.call(() -> "ok", (Predicate<Exception>) null, () -> "d"));
        assertThrows(IllegalArgumentException.class, () -> Try.call(() -> "ok", e -> true, (java.util.function.Supplier<String>) null));
        assertThrows(IllegalArgumentException.class, () -> Try.call((java.util.concurrent.Callable<String>) null, e -> true, "d"));
        assertThrows(IllegalArgumentException.class, () -> Try.call(() -> "ok", (Predicate<Exception>) null, "d"));
    }

    @Test
    public void testInterruptedStatusRestored() {
        Thread.interrupted();
        try {
            assertThrows(RuntimeException.class, () -> Try.run(() -> {
                throw new IOException("outer", new InterruptedException("nested"));
            }));
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }

        Thread.interrupted();
        try {
            AtomicBoolean saw = new AtomicBoolean();
            Try.run(() -> {
                throw new InterruptedException("stop");
            }, e -> saw.set(Thread.currentThread().isInterrupted()));
            assertTrue(saw.get());
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }

        Thread.interrupted();
        try {
            AtomicBoolean saw = new AtomicBoolean();
            assertEquals("fallback", Try.with(new TestCloseable()).call(c -> {
                throw new InterruptedException("stop");
            }, (java.util.function.Function<Exception, String>) e -> {
                saw.set(Thread.currentThread().isInterrupted());
                return "fallback";
            }));
            assertTrue(saw.get());
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }

        Thread.interrupted();
        try {
            AutoCloseable closeable = () -> {
                throw new InterruptedException("close");
            };
            AtomicBoolean saw = new AtomicBoolean();
            assertEquals("fallback", Try.with(closeable).call(r -> {
                throw new IOException("primary");
            }, (java.util.function.Function<Exception, String>) e -> {
                assertEquals(1, e.getSuppressed().length);
                assertTrue(e.getSuppressed()[0] instanceof InterruptedException);
                saw.set(Thread.currentThread().isInterrupted());
                return "fallback";
            }));
            assertTrue(saw.get());
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }

        Thread.interrupted();
        try {
            AutoCloseable closeable = () -> {
                throw new InterruptedException("close");
            };
            AssertionError failure = assertThrows(AssertionError.class, () -> Try.with(closeable).run(r -> {
                throw new AssertionError("primary");
            }));
            assertEquals(1, failure.getSuppressed().length);
            assertTrue(failure.getSuppressed()[0] instanceof InterruptedException);
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }

        Thread.interrupted();
        try {
            IOException failure = new IOException("outer");
            failure.addSuppressed(new InterruptedException("suppressed"));
            assertThrows(RuntimeException.class, () -> Try.call(() -> {
                throw failure;
            }));
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    public void testAnotherThreadsInterruptionIsNotRestoredOnThisThread() {
        // Future.get()/CompletableFuture.join() report the *task's* failure. An InterruptedException under
        // one of those wrappers belongs to the task's thread and must leave this thread alone.
        Thread.interrupted();
        try {
            assertEquals("fallback", Try.call(() -> {
                throw new ExecutionException(new InterruptedException("task"));
            }, "fallback"));
            assertFalse(Thread.currentThread().isInterrupted());

            assertEquals("fallback", Try.call(() -> {
                throw new CompletionException(new InterruptedException("task"));
            }, "fallback"));
            assertFalse(Thread.currentThread().isInterrupted());

            assertThrows(RuntimeException.class, () -> Try.run(() -> {
                throw new ExecutionException(new InterruptedException("task"));
            }));
            assertFalse(Thread.currentThread().isInterrupted());

            // The resource overloads convert through ExceptionUtil.toRuntimeException(..), which unwraps the
            // wrapper: the decision must already have been taken on the original exception.
            assertThrows(RuntimeException.class, () -> Try.with(new TestCloseable()).run(c -> {
                throw new ExecutionException(new InterruptedException("task"));
            }));
            assertFalse(Thread.currentThread().isInterrupted());

            assertThrows(RuntimeException.class, () -> Try.with(new TestCloseable()).call(c -> {
                throw new ExecutionException(new InterruptedException("task"));
            }));
            assertFalse(Thread.currentThread().isInterrupted());

            // The rejected branch of the predicate overloads throws through executeWithFinalAction too.
            assertThrows(RuntimeException.class, () -> Try.with(new TestCloseable()).call(c -> {
                throw new ExecutionException(new InterruptedException("task"));
            }, (Predicate<Exception>) e -> false, "fallback"));
            assertFalse(Thread.currentThread().isInterrupted());

            assertEquals("fallback", Try.with(new TestCloseable()).call(c -> {
                throw new ExecutionException(new InterruptedException("task"));
            }, (Predicate<Exception>) e -> true, "fallback"));
            assertFalse(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }

        // What is suppressed on the wrapper was attached on this thread, so it still restores the status.
        Thread.interrupted();
        try {
            final ExecutionException wrapper = new ExecutionException(new InterruptedException("task"));
            wrapper.addSuppressed(new InterruptedException("close"));
            assertEquals("fallback", Try.call(() -> {
                throw wrapper;
            }, "fallback"));
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }

        // The other half of the same call: future.get() throws InterruptedException *unwrapped* when this
        // thread was interrupted while waiting, and that one must still restore the status.
        Thread.interrupted();
        try {
            assertEquals("fallback", Try.call(() -> {
                throw new InterruptedException("this thread was interrupted while waiting");
            }, "fallback"));
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    public void testFinalActionAndSuppressed() {
        AtomicBoolean ran = new AtomicBoolean();
        TestCloseable c = new TestCloseable();
        Try.with(c, () -> ran.set(true)).run(x -> {
        });
        assertTrue(c.isClosed());
        assertTrue(ran.get());

        ran.set(false);
        TestCloseable c2 = new TestCloseable();
        assertThrows(RuntimeException.class, () -> Try.with(c2, () -> ran.set(true)).run(x -> {
            throw new RuntimeException("boom");
        }));
        assertTrue(c2.isClosed());
        assertTrue(ran.get());

        AtomicBoolean executed = new AtomicBoolean();
        assertThrows(RuntimeException.class, () -> Try.with(new ByteArrayInputStream("t".getBytes()), () -> {
            throw new RuntimeException("final");
        }).run(s -> executed.set(true)));
        assertTrue(executed.get());

        RuntimeException body = new RuntimeException("body");
        RuntimeException closeEx = new RuntimeException("close");
        ThrowOnClose closeable = new ThrowOnClose(closeEx);
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> Try.with(closeable).run(x -> {
            throw body;
        }));
        assertTrue(closeable.closeCalled);
        boolean foundClose = false;
        for (Throwable t = thrown; t != null && !foundClose; t = t.getCause()) {
            for (Throwable s : t.getSuppressed()) {
                if (s == closeEx) {
                    foundClose = true;
                    break;
                }
            }
        }
        assertTrue(foundClose);

        RuntimeException bodyFailure = new RuntimeException("body");
        RuntimeException finalFailure = new RuntimeException("final");
        RuntimeException both = assertThrows(RuntimeException.class, () -> Try.with(new TestCloseable(), () -> {
            throw finalFailure;
        }).run(r -> {
            throw bodyFailure;
        }));
        assertSame(bodyFailure, both);
        assertEquals(1, both.getSuppressed().length);
        assertSame(finalFailure, both.getSuppressed()[0]);

        AtomicInteger count = new AtomicInteger();
        assertEquals("handled", Try.call(() -> {
            count.incrementAndGet();
            throw new IOException("first");
        }, e -> {
            count.incrementAndGet();
            return "handled";
        }));
        assertEquals(2, count.get());
    }

    // Pins the class-javadoc claim that a failing final action is NOT routed to a fallback: with no other
    // failure left to propagate it reaches the caller and the fallback overload's result is discarded.
    // Its own test so a regression names the contract it broke.
    @Test
    public void testFinalActionFailureIsNotRoutedToAFallback() {
        IllegalStateException finalBoom = new IllegalStateException("finalBoom");
        IllegalStateException escaped = assertThrows(IllegalStateException.class, () -> Try.with(new TestCloseable(), () -> {
            throw finalBoom;
        }).call(x -> "body", "fallback"));
        assertSame(finalBoom, escaped);
        assertEquals(0, escaped.getSuppressed().length);

        // ... and the same when the body itself failed and the fallback already absorbed that failure.
        IllegalStateException finalBoom2 = new IllegalStateException("finalBoom2");
        assertSame(finalBoom2, assertThrows(IllegalStateException.class, () -> Try.with(new TestCloseable(), () -> {
            throw finalBoom2;
        }).call(x -> {
            throw new IOException("body");
        }, "fallback")));
    }

    // Pins the corrected class-javadoc sentence about a supplier that returns null: the IllegalArgumentException
    // is an acquisition failure like any other, so only run(cmd) and call(cmd) let it reach the caller - every
    // overload with error handling absorbs it.
    @Test
    public void testNullSuppliedResourceIsAnAcquisitionFailureLikeAnyOther() {
        final Throwables.Supplier<AutoCloseable, Exception> nullSupplier = () -> null;
        final Throwables.Function<AutoCloseable, String, Exception> body = x -> "bodyResult";

        // no error handling -> the caller sees it
        assertThrows(IllegalArgumentException.class, () -> Try.with(nullSupplier).run(x -> fail("body must not run")));
        assertThrows(IllegalArgumentException.class, () -> Try.with(nullSupplier).call(body));

        // every overload that offers error handling absorbs it instead
        final AtomicReference<Exception> seen = new AtomicReference<>();
        Try.with(nullSupplier).run(x -> fail("body must not run"), seen::set);
        assertTrue(seen.get() instanceof IllegalArgumentException);

        assertEquals("fb", Try.with(nullSupplier).call(body, "fb"));
        assertEquals("fbSup", Try.with(nullSupplier).call(body, () -> "fbSup"));
        assertEquals("fbFn", Try.with(nullSupplier).call(body, e -> {
            assertTrue(e instanceof IllegalArgumentException);
            return "fbFn";
        }));
        assertEquals("fbPred", Try.with(nullSupplier).call(body, e -> true, "fbPred"));
    }
}
