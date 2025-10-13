package com.landawn.abacus.util;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class Try100Test extends TestBase {

    public static class TestAutoCloseable implements AutoCloseable {
        boolean closed = false;
        boolean throwOnClose = false;

        @Override
        public void close() throws Exception {
            closed = true;
            if (throwOnClose) {
                throw new Exception("Close failed");
            }
        }
    }

    @Test
    public void testWith_Resource() {
        TestAutoCloseable resource = new TestAutoCloseable();
        Try<TestAutoCloseable> tryInstance = Try.with(resource);
        Assertions.assertNotNull(tryInstance);
    }

    @Test
    public void testWith_ResourceAndFinalAction() {
        TestAutoCloseable resource = new TestAutoCloseable();
        boolean[] finalActionCalled = { false };
        Try<TestAutoCloseable> tryInstance = Try.with(resource, () -> finalActionCalled[0] = true);
        Assertions.assertNotNull(tryInstance);
    }

    @Test
    public void testWith_ResourceSupplier() {
        Try<TestAutoCloseable> tryInstance = Try.with(Fnn.s(() -> new TestAutoCloseable()));
        Assertions.assertNotNull(tryInstance);
    }

    @Test
    public void testWith_ResourceSupplierAndFinalAction() {
        boolean[] finalActionCalled = { false };
        Try<TestAutoCloseable> tryInstance = Try.with(Fnn.s(() -> new TestAutoCloseable()), () -> finalActionCalled[0] = true);
        Assertions.assertNotNull(tryInstance);
    }

    @Test
    public void testStatic_Run_Success() {
        boolean[] executed = { false };
        Try.run(() -> {
            executed[0] = true;
        });
        Assertions.assertTrue(executed[0]);
    }

    @Test
    public void testStatic_Run_Exception() {
        Assertions.assertThrows(RuntimeException.class, () -> {
            Try.run(() -> {
                throw new IOException("Test exception");
            });
        });
    }

    @Test
    public void testStatic_Run_WithErrorHandler() {
        boolean[] errorHandled = { false };
        Try.run(() -> {
            throw new IOException("Test exception");
        }, e -> {
            errorHandled[0] = true;
        });
        Assertions.assertTrue(errorHandled[0]);
    }

    @Test
    public void testStatic_Call_Success() {
        String result = Try.call(() -> "success");
        Assertions.assertEquals("success", result);
    }

    @Test
    public void testStatic_Call_Exception() {
        Assertions.assertThrows(RuntimeException.class, () -> {
            Try.call(() -> {
                throw new IOException("Test exception");
            });
        });
    }

    @Test
    public void testStatic_Call_WithErrorFunction() {
        Callable<String> cmd = () -> {
            throw new IOException("Test exception");
        };
        String result = Try.call(cmd, Fn.f(e -> "error handled"));
        Assertions.assertEquals("error handled", result);
    }

    @Test
    public void testStatic_Call_WithSupplier() {
        String result = Try.call(() -> {
            throw new IOException("Test exception");
        }, Fn.s(() -> "default value"));
        Assertions.assertEquals("default value", result);
    }

    @Test
    public void testStatic_Call_WithDefaultValue() {
        String result = Try.call(() -> {
            throw new IOException("Test exception");
        }, "default");
        Assertions.assertEquals("default", result);
    }

    @Test
    public void testStatic_Call_WithPredicateAndSupplier_PredicateTrue() {
        String result = Try.call(() -> {
            throw new IOException("Test exception");
        }, e -> e instanceof IOException, Fn.s(() -> "handled"));
        Assertions.assertEquals("handled", result);
    }

    @Test
    public void testStatic_Call_WithPredicateAndSupplier_PredicateFalse() {
        Assertions.assertThrows(RuntimeException.class, () -> {
            Try.call((Callable<String>) () -> {
                throw new IOException("Test exception");
            }, (Predicate<? super Exception>) e -> e instanceof IllegalArgumentException, Fn.s(() -> "handled"));
        });
    }

    @Test
    public void testStatic_Call_WithPredicateAndDefaultValue_PredicateTrue() {
        String result = Try.call(() -> {
            throw new IOException("Test exception");
        }, e -> e instanceof IOException, "default");
        Assertions.assertEquals("default", result);
    }

    @Test
    public void testStatic_Call_WithPredicateAndDefaultValue_PredicateFalse() {
        Assertions.assertThrows(RuntimeException.class, () -> {
            Try.call(() -> {
                throw new IOException("Test exception");
            }, e -> e instanceof IllegalArgumentException, "default");
        });
    }

    @Test
    public void testInstance_Run_Success() {
        TestAutoCloseable resource = new TestAutoCloseable();
        boolean[] executed = { false };

        Try.with(resource).run(r -> {
            executed[0] = true;
            Assertions.assertFalse(r.closed);
        });

        Assertions.assertTrue(executed[0]);
        Assertions.assertTrue(resource.closed);
    }

    @Test
    public void testInstance_Run_WithFinalAction() {
        TestAutoCloseable resource = new TestAutoCloseable();
        boolean[] finalActionCalled = { false };

        Try.with(resource, () -> finalActionCalled[0] = true).run(r -> {
        });

        Assertions.assertTrue(resource.closed);
        Assertions.assertTrue(finalActionCalled[0]);
    }

    @Test
    public void testInstance_Run_WithErrorHandler() {
        TestAutoCloseable resource = new TestAutoCloseable();
        boolean[] errorHandled = { false };

        Try.with(resource).run(r -> {
            throw new IOException("Test exception");
        }, e -> {
            errorHandled[0] = true;
        });

        Assertions.assertTrue(errorHandled[0]);
        Assertions.assertTrue(resource.closed);
    }

    @Test
    public void testInstance_Call_Success() {
        TestAutoCloseable resource = new TestAutoCloseable();

        String result = Try.with(resource).call(r -> {
            Assertions.assertFalse(r.closed);
            return "success";
        });

        Assertions.assertEquals("success", result);
        Assertions.assertTrue(resource.closed);
    }

    @Test
    public void testInstance_Call_WithErrorFunction() {
        TestAutoCloseable resource = new TestAutoCloseable();

        String result = Try.with(resource).call(r -> {
            throw new IOException("Test exception");
        }, Fn.f(e -> "error handled"));

        Assertions.assertEquals("error handled", result);
        Assertions.assertTrue(resource.closed);
    }

    @Test
    public void testInstance_Call_WithSupplier() {
        TestAutoCloseable resource = new TestAutoCloseable();

        String result = Try.with(resource).call(r -> {
            throw new IOException("Test exception");
        }, Fn.s(() -> "default value"));

        Assertions.assertEquals("default value", result);
        Assertions.assertTrue(resource.closed);
    }

    @Test
    public void testInstance_Call_WithDefaultValue() {
        TestAutoCloseable resource = new TestAutoCloseable();

        String result = Try.with(resource).call(r -> {
            throw new IOException("Test exception");
        }, "default");

        Assertions.assertEquals("default", result);
        Assertions.assertTrue(resource.closed);
    }

    @Test
    public void testInstance_Call_WithPredicateAndSupplier() {
        TestAutoCloseable resource = new TestAutoCloseable();

        String result = Try.with(resource).call(r -> {
            throw new IOException("Test exception");
        }, e -> e instanceof IOException, Fn.s(() -> "handled"));

        Assertions.assertEquals("handled", result);
        Assertions.assertTrue(resource.closed);
    }

    @Test
    public void testInstance_Call_WithPredicateAndDefaultValue() {
        TestAutoCloseable resource = new TestAutoCloseable();

        String result = Try.with(resource).call(r -> {
            throw new IOException("Test exception");
        }, e -> e instanceof IOException, "default");

        Assertions.assertEquals("default", result);
        Assertions.assertTrue(resource.closed);
    }

    @Test
    public void testResourceSupplier_LazyInit() {
        boolean[] resourceCreated = { false };

        Try<TestAutoCloseable> tryInstance = Try.with((Throwables.Supplier<TestAutoCloseable, ? extends Exception>) () -> {
            resourceCreated[0] = true;
            return new TestAutoCloseable();
        });

        Assertions.assertFalse(resourceCreated[0]);

        tryInstance.run(r -> {
        });

        Assertions.assertTrue(resourceCreated[0]);
    }

    @Test
    public void testNullResource() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> Try.with((TestAutoCloseable) null));
    }

    @Test
    public void testNullResourceSupplier() {
        Try<TestAutoCloseable> tryInstance = Try.with((Throwables.Supplier<TestAutoCloseable, ? extends Exception>) () -> null);
        tryInstance.run(r -> {
            Assertions.assertNull(r);
        });
    }

    @Test
    public void testCloseException() {
        TestAutoCloseable resource = new TestAutoCloseable();
        resource.throwOnClose = true;

        Try.with(resource).run(r -> {
            resource.throwOnClose = false;
        });

        Assertions.assertTrue(resource.closed);
    }

    @Test
    public void testFinalActionAlwaysRuns() {
        TestAutoCloseable resource = new TestAutoCloseable();
        boolean[] finalActionCalled = { false };

        try {
            Try.with(resource, () -> finalActionCalled[0] = true).run(r -> {
                throw new RuntimeException("Test exception");
            });
        } catch (RuntimeException e) {
        }

        Assertions.assertTrue(resource.closed);
        Assertions.assertTrue(finalActionCalled[0]);
    }

    @Test
    public void testNullCmd() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Throwables.Runnable<? extends Exception> cmd = null;
            Try.run(cmd);
        });
    }

    @Test
    public void testNullActionOnError() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Try.run(() -> {
            }, null);
        });
    }

    @Test
    public void testNullCallCmd() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Try.call((Callable<String>) null);
        });
    }

    @Test
    public void testNullPredicate() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Try.call(() -> "test", null, Fn.s(() -> "default"));
        });
    }

    @Test
    public void testNullSupplier() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Callable<String> jc = Fn.jc(() -> "test");
            Try.call(jc, (Supplier<String>) null);
        });
    }
}
