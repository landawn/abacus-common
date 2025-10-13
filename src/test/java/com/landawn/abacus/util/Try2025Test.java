package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class Try2025Test extends TestBase {

    @Test
    public void test_with_targetResource() {
        InputStream stream = new ByteArrayInputStream("test".getBytes());
        Try<InputStream> tryInstance = Try.with(stream);
        assertNotNull(tryInstance);
    }

    @Test
    public void test_with_targetResource_null() {
        assertThrows(IllegalArgumentException.class, () -> {
            Try.with((InputStream) null);
        });
    }

    @Test
    public void test_with_targetResource_finalAction() {
        InputStream stream = new ByteArrayInputStream("test".getBytes());
        AtomicBoolean finalActionRun = new AtomicBoolean(false);
        Try<InputStream> tryInstance = Try.with(stream, () -> finalActionRun.set(true));
        assertNotNull(tryInstance);

        tryInstance.run(s -> {
        });

        assertTrue(finalActionRun.get());
    }

    @Test
    public void test_with_targetResource_finalAction_null_resource() {
        assertThrows(IllegalArgumentException.class, () -> {
            Try.with((InputStream) null, () -> {
            });
        });
    }

    @Test
    public void test_with_targetResource_finalAction_null_action() {
        InputStream stream = new ByteArrayInputStream("test".getBytes());
        assertThrows(IllegalArgumentException.class, () -> {
            Try.with(stream, (Runnable) null);
        });
    }

    @Test
    public void test_with_targetResourceSupplier() {
        Throwables.Supplier<InputStream, Exception> supplier = () -> new ByteArrayInputStream("test".getBytes());
        Try<InputStream> tryInstance = Try.with(supplier);
        assertNotNull(tryInstance);
    }

    @Test
    public void test_with_targetResourceSupplier_null() {
        assertThrows(IllegalArgumentException.class, () -> {
            Try.with((Throwables.Supplier<InputStream, Exception>) null);
        });
    }

    @Test
    public void test_with_targetResourceSupplier_finalAction() {
        AtomicBoolean finalActionRun = new AtomicBoolean(false);
        Throwables.Supplier<InputStream, Exception> supplier = () -> new ByteArrayInputStream("test".getBytes());
        Try<InputStream> tryInstance = Try.with(supplier, () -> finalActionRun.set(true));
        assertNotNull(tryInstance);

        tryInstance.run(s -> {
        });

        assertTrue(finalActionRun.get());
    }

    @Test
    public void test_with_targetResourceSupplier_finalAction_null_supplier() {
        assertThrows(IllegalArgumentException.class, () -> {
            Try.with((Throwables.Supplier<InputStream, Exception>) null, () -> {
            });
        });
    }

    @Test
    public void test_with_targetResourceSupplier_finalAction_null_action() {
        Throwables.Supplier<InputStream, Exception> supplier = () -> new ByteArrayInputStream("test".getBytes());
        assertThrows(IllegalArgumentException.class, () -> {
            Try.with(supplier, (Runnable) null);
        });
    }

    @Test
    public void test_run_cmd_success() {
        AtomicBoolean executed = new AtomicBoolean(false);
        Try.run(() -> executed.set(true));
        assertTrue(executed.get());
    }

    @Test
    public void test_run_cmd_with_exception() {
        assertThrows(RuntimeException.class, () -> {
            Try.run(() -> {
                throw new IOException("Test exception");
            });
        });
    }

    @Test
    public void test_run_cmd_null() {
        assertThrows(IllegalArgumentException.class, () -> {
            Try.run((Throwables.Runnable<Exception>) null);
        });
    }

    @Test
    public void test_run_cmd_actionOnError_success() {
        AtomicBoolean executed = new AtomicBoolean(false);
        AtomicBoolean errorHandled = new AtomicBoolean(false);
        Try.run(() -> executed.set(true), ex -> errorHandled.set(true));
        assertTrue(executed.get());
        assertTrue(!errorHandled.get());
    }

    @Test
    public void test_run_cmd_actionOnError_with_exception() {
        AtomicBoolean errorHandled = new AtomicBoolean(false);
        AtomicReference<Exception> caughtException = new AtomicReference<>();
        Try.run(() -> {
            throw new IOException("Test exception");
        }, ex -> {
            errorHandled.set(true);
            caughtException.set(ex);
        });
        assertTrue(errorHandled.get());
        assertNotNull(caughtException.get());
        assertTrue(caughtException.get() instanceof IOException);
    }

    @Test
    public void test_run_cmd_actionOnError_null_cmd() {
        assertThrows(IllegalArgumentException.class, () -> {
            Try.run((Throwables.Runnable<Exception>) null, ex -> {
            });
        });
    }

    @Test
    public void test_run_cmd_actionOnError_null_action() {
        assertThrows(IllegalArgumentException.class, () -> {
            Try.run(() -> {
            }, null);
        });
    }

    @Test
    public void test_call_cmd_success() {
        String result = Try.call(() -> "test result");
        assertEquals("test result", result);
    }

    @Test
    public void test_call_cmd_with_exception() {
        assertThrows(RuntimeException.class, () -> {
            Try.call(() -> {
                throw new IOException("Test exception");
            });
        });
    }

    @Test
    public void test_call_cmd_null() {
        assertThrows(IllegalArgumentException.class, () -> {
            Try.call((java.util.concurrent.Callable<String>) null);
        });
    }

    @Test
    public void test_call_cmd_function_success() {
        java.util.function.Function<Exception, String> errorHandler = ex -> "error result";
        String result = Try.call(() -> "test result", errorHandler);
        assertEquals("test result", result);
    }

    @Test
    public void test_call_cmd_function_with_exception() {
        java.util.function.Function<Exception, String> errorHandler = ex -> "error result";
        String result = Try.call(() -> {
            throw new IOException("Test exception");
        }, errorHandler);
        assertEquals("error result", result);
    }

    @Test
    public void test_call_cmd_function_null_cmd() {
        java.util.function.Function<Exception, String> errorHandler = ex -> "error";
        assertThrows(IllegalArgumentException.class, () -> {
            Try.call((java.util.concurrent.Callable<String>) null, errorHandler);
        });
    }

    @Test
    public void test_call_cmd_function_null_function() {
        java.util.concurrent.Callable<String> cmd = () -> "test";
        assertThrows(IllegalArgumentException.class, () -> {
            Try.call(cmd, (java.util.function.Function<Exception, String>) null);
        });
    }

    @Test
    public void test_call_cmd_supplier_success() {
        java.util.function.Supplier<String> supplier = () -> "default result";
        String result = Try.call(() -> "test result", supplier);
        assertEquals("test result", result);
    }

    @Test
    public void test_call_cmd_supplier_with_exception() {
        java.util.function.Supplier<String> supplier = () -> "default result";
        String result = Try.call(() -> {
            throw new IOException("Test exception");
        }, supplier);
        assertEquals("default result", result);
    }

    @Test
    public void test_call_cmd_supplier_null_cmd() {
        java.util.function.Supplier<String> supplier = () -> "default";
        assertThrows(IllegalArgumentException.class, () -> {
            Try.call((java.util.concurrent.Callable<String>) null, supplier);
        });
    }

    @Test
    public void test_call_cmd_supplier_null_supplier() {
        java.util.concurrent.Callable<String> cmd = () -> "test";
        assertThrows(IllegalArgumentException.class, () -> {
            Try.call(cmd, (java.util.function.Supplier<String>) null);
        });
    }

    @Test
    public void test_call_cmd_defaultValue_success() {
        String result = Try.call(() -> "test result", "default result");
        assertEquals("test result", result);
    }

    @Test
    public void test_call_cmd_defaultValue_with_exception() {
        String result = Try.call(() -> {
            throw new IOException("Test exception");
        }, "default result");
        assertEquals("default result", result);
    }

    @Test
    public void test_call_cmd_defaultValue_null_cmd() {
        assertThrows(IllegalArgumentException.class, () -> {
            Try.call((java.util.concurrent.Callable<String>) null, "default");
        });
    }

    @Test
    public void test_call_cmd_defaultValue_null_default() {
        String result = Try.call(() -> {
            throw new IOException("Test exception");
        }, (String) null);
        assertNull(result);
    }

    @Test
    public void test_call_cmd_predicate_supplier_success() {
        java.util.function.Predicate<Exception> predicate = ex -> ex instanceof IOException;
        java.util.function.Supplier<String> supplier = () -> "default result";
        String result = Try.call(() -> "test result", predicate, supplier);
        assertEquals("test result", result);
    }

    @Test
    public void test_call_cmd_predicate_supplier_matching_exception() {
        java.util.function.Predicate<Exception> predicate = ex -> ex instanceof IOException;
        java.util.function.Supplier<String> supplier = () -> "default result";
        String result = Try.call(() -> {
            throw new IOException("Test exception");
        }, predicate, supplier);
        assertEquals("default result", result);
    }

    @Test
    public void test_call_cmd_predicate_supplier_non_matching_exception() {
        java.util.function.Predicate<Exception> predicate = ex -> ex instanceof FileNotFoundException;
        java.util.function.Supplier<String> supplier = () -> "default result";
        assertThrows(RuntimeException.class, () -> {
            Try.call(() -> {
                throw new IOException("Test exception");
            }, predicate, supplier);
        });
    }

    @Test
    public void test_call_cmd_predicate_supplier_null_cmd() {
        java.util.function.Predicate<Exception> predicate = ex -> true;
        java.util.function.Supplier<String> supplier = () -> "default";
        assertThrows(IllegalArgumentException.class, () -> {
            Try.call((java.util.concurrent.Callable<String>) null, predicate, supplier);
        });
    }

    @Test
    public void test_call_cmd_predicate_supplier_null_predicate() {
        java.util.concurrent.Callable<String> cmd = () -> "test";
        java.util.function.Supplier<String> supplier = () -> "default";
        assertThrows(IllegalArgumentException.class, () -> {
            Try.call(cmd, (java.util.function.Predicate<Exception>) null, supplier);
        });
    }

    @Test
    public void test_call_cmd_predicate_supplier_null_supplier() {
        java.util.concurrent.Callable<String> cmd = () -> "test";
        java.util.function.Predicate<Exception> predicate = ex -> true;
        assertThrows(IllegalArgumentException.class, () -> {
            Try.call(cmd, predicate, (java.util.function.Supplier<String>) null);
        });
    }

    @Test
    public void test_call_cmd_predicate_defaultValue_success() {
        String result = Try.call(() -> "test result", ex -> ex instanceof IOException, "default result");
        assertEquals("test result", result);
    }

    @Test
    public void test_call_cmd_predicate_defaultValue_matching_exception() {
        String result = Try.call(() -> {
            throw new IOException("Test exception");
        }, ex -> ex instanceof IOException, "default result");
        assertEquals("default result", result);
    }

    @Test
    public void test_call_cmd_predicate_defaultValue_non_matching_exception() {
        assertThrows(RuntimeException.class, () -> {
            Try.call(() -> {
                throw new IOException("Test exception");
            }, ex -> ex instanceof FileNotFoundException, "default result");
        });
    }

    @Test
    public void test_call_cmd_predicate_defaultValue_null_cmd() {
        java.util.function.Predicate<Exception> predicate = ex -> true;
        assertThrows(IllegalArgumentException.class, () -> {
            Try.call((java.util.concurrent.Callable<String>) null, predicate, "default");
        });
    }

    @Test
    public void test_call_cmd_predicate_defaultValue_null_predicate() {
        java.util.concurrent.Callable<String> cmd = () -> "test";
        assertThrows(IllegalArgumentException.class, () -> {
            Try.call(cmd, (java.util.function.Predicate<Exception>) null, "default");
        });
    }

    @Test
    public void test_instance_run_cmd_success() {
        AtomicBoolean executed = new AtomicBoolean(false);
        Try.with(new ByteArrayInputStream("test".getBytes())).run(stream -> {
            executed.set(true);
            assertNotNull(stream);
        });
        assertTrue(executed.get());
    }

    @Test
    public void test_instance_run_cmd_with_exception() {
        assertThrows(RuntimeException.class, () -> {
            Try.with(new ByteArrayInputStream("test".getBytes())).run(stream -> {
                throw new IOException("Test exception");
            });
        });
    }

    @Test
    public void test_instance_run_cmd_resource_closed() {
        TestCloseable closeable = new TestCloseable();
        Try.with(closeable).run(c -> {
            assertTrue(!c.isClosed());
        });
        assertTrue(closeable.isClosed());
    }

    @Test
    public void test_instance_run_cmd_with_supplier() {
        AtomicBoolean executed = new AtomicBoolean(false);
        Throwables.Supplier<InputStream, Exception> supplier = () -> new ByteArrayInputStream("test".getBytes());
        Try.with(supplier).run(stream -> {
            executed.set(true);
            assertNotNull(stream);
        });
        assertTrue(executed.get());
    }

    @Test
    public void test_instance_run_cmd_with_finalAction() {
        AtomicBoolean finalActionRun = new AtomicBoolean(false);
        Try.with(new ByteArrayInputStream("test".getBytes()), () -> finalActionRun.set(true)).run(stream -> {
        });
        assertTrue(finalActionRun.get());
    }

    @Test
    public void test_instance_run_cmd_actionOnError_success() {
        AtomicBoolean executed = new AtomicBoolean(false);
        AtomicBoolean errorHandled = new AtomicBoolean(false);
        Try.with(new ByteArrayInputStream("test".getBytes())).run(stream -> executed.set(true), ex -> errorHandled.set(true));
        assertTrue(executed.get());
        assertTrue(!errorHandled.get());
    }

    @Test
    public void test_instance_run_cmd_actionOnError_with_exception() {
        AtomicBoolean errorHandled = new AtomicBoolean(false);
        AtomicReference<Exception> caughtException = new AtomicReference<>();
        Try.with(new ByteArrayInputStream("test".getBytes())).run(stream -> {
            throw new IOException("Test exception");
        }, ex -> {
            errorHandled.set(true);
            caughtException.set(ex);
        });
        assertTrue(errorHandled.get());
        assertNotNull(caughtException.get());
        assertTrue(caughtException.get() instanceof IOException);
    }

    @Test
    public void test_instance_run_cmd_actionOnError_resource_closed() {
        TestCloseable closeable = new TestCloseable();
        Try.with(closeable).run(c -> {
            throw new IOException("Test exception");
        }, ex -> {
        });
        assertTrue(closeable.isClosed());
    }

    @Test
    public void test_instance_run_cmd_actionOnError_with_finalAction() {
        AtomicBoolean finalActionRun = new AtomicBoolean(false);
        Try.with(new ByteArrayInputStream("test".getBytes()), () -> finalActionRun.set(true)).run(stream -> {
            throw new IOException("Test exception");
        }, ex -> {
        });
        assertTrue(finalActionRun.get());
    }

    @Test
    public void test_instance_call_cmd_success() {
        String result = Try.with(new ByteArrayInputStream("test".getBytes())).call(stream -> "result");
        assertEquals("result", result);
    }

    @Test
    public void test_instance_call_cmd_with_exception() {
        assertThrows(RuntimeException.class, () -> {
            Try.with(new ByteArrayInputStream("test".getBytes())).call(stream -> {
                throw new IOException("Test exception");
            });
        });
    }

    @Test
    public void test_instance_call_cmd_resource_closed() {
        TestCloseable closeable = new TestCloseable();
        Try.with(closeable).call(c -> {
            assertTrue(!c.isClosed());
            return "result";
        });
        assertTrue(closeable.isClosed());
    }

    @Test
    public void test_instance_call_cmd_with_supplier() {
        Throwables.Supplier<InputStream, Exception> supplier = () -> new ByteArrayInputStream("test".getBytes());
        String result = Try.with(supplier).call(stream -> "result");
        assertEquals("result", result);
    }

    @Test
    public void test_instance_call_cmd_with_finalAction() {
        AtomicBoolean finalActionRun = new AtomicBoolean(false);
        String result = Try.with(new ByteArrayInputStream("test".getBytes()), () -> finalActionRun.set(true)).call(stream -> "result");
        assertEquals("result", result);
        assertTrue(finalActionRun.get());
    }

    @Test
    public void test_instance_call_cmd_function_success() {
        java.util.function.Function<Exception, String> errorHandler = ex -> "error";
        String result = Try.with(new ByteArrayInputStream("test".getBytes())).call(stream -> "result", errorHandler);
        assertEquals("result", result);
    }

    @Test
    public void test_instance_call_cmd_function_with_exception() {
        java.util.function.Function<Exception, String> errorHandler = ex -> "error";
        String result = Try.with(new ByteArrayInputStream("test".getBytes())).call(stream -> {
            throw new IOException("Test exception");
        }, errorHandler);
        assertEquals("error", result);
    }

    @Test
    public void test_instance_call_cmd_function_resource_closed() {
        TestCloseable closeable = new TestCloseable();
        java.util.function.Function<Exception, String> errorHandler = ex -> "error";
        Try.with(closeable).call(c -> {
            throw new IOException("Test exception");
        }, errorHandler);
        assertTrue(closeable.isClosed());
    }

    @Test
    public void test_instance_call_cmd_function_with_finalAction() {
        AtomicBoolean finalActionRun = new AtomicBoolean(false);
        java.util.function.Function<Exception, String> errorHandler = ex -> "error";
        String result = Try.with(new ByteArrayInputStream("test".getBytes()), () -> finalActionRun.set(true)).call(stream -> {
            throw new IOException("Test exception");
        }, errorHandler);
        assertEquals("error", result);
        assertTrue(finalActionRun.get());
    }

    @Test
    public void test_instance_call_cmd_supplier_success() {
        java.util.function.Supplier<String> supplier = () -> "default";
        String result = Try.with(new ByteArrayInputStream("test".getBytes())).call(stream -> "result", supplier);
        assertEquals("result", result);
    }

    @Test
    public void test_instance_call_cmd_supplier_with_exception() {
        java.util.function.Supplier<String> supplier = () -> "default";
        String result = Try.with(new ByteArrayInputStream("test".getBytes())).call(stream -> {
            throw new IOException("Test exception");
        }, supplier);
        assertEquals("default", result);
    }

    @Test
    public void test_instance_call_cmd_supplier_resource_closed() {
        TestCloseable closeable = new TestCloseable();
        java.util.function.Supplier<String> supplier = () -> "default";
        Try.with(closeable).call(c -> {
            throw new IOException("Test exception");
        }, supplier);
        assertTrue(closeable.isClosed());
    }

    @Test
    public void test_instance_call_cmd_supplier_with_finalAction() {
        AtomicBoolean finalActionRun = new AtomicBoolean(false);
        java.util.function.Supplier<String> supplier = () -> "default";
        String result = Try.with(new ByteArrayInputStream("test".getBytes()), () -> finalActionRun.set(true)).call(stream -> {
            throw new IOException("Test exception");
        }, supplier);
        assertEquals("default", result);
        assertTrue(finalActionRun.get());
    }

    @Test
    public void test_instance_call_cmd_defaultValue_success() {
        String result = Try.with(new ByteArrayInputStream("test".getBytes())).call(stream -> "result", "default");
        assertEquals("result", result);
    }

    @Test
    public void test_instance_call_cmd_defaultValue_with_exception() {
        String result = Try.with(new ByteArrayInputStream("test".getBytes())).call(stream -> {
            throw new IOException("Test exception");
        }, "default");
        assertEquals("default", result);
    }

    @Test
    public void test_instance_call_cmd_defaultValue_resource_closed() {
        TestCloseable closeable = new TestCloseable();
        Try.with(closeable).call(c -> {
            throw new IOException("Test exception");
        }, "default");
        assertTrue(closeable.isClosed());
    }

    @Test
    public void test_instance_call_cmd_defaultValue_with_finalAction() {
        AtomicBoolean finalActionRun = new AtomicBoolean(false);
        String result = Try.with(new ByteArrayInputStream("test".getBytes()), () -> finalActionRun.set(true)).call(stream -> {
            throw new IOException("Test exception");
        }, "default");
        assertEquals("default", result);
        assertTrue(finalActionRun.get());
    }

    @Test
    public void test_instance_call_cmd_predicate_supplier_success() {
        java.util.function.Predicate<Exception> predicate = ex -> ex instanceof IOException;
        java.util.function.Supplier<String> supplier = () -> "default";
        String result = Try.with(new ByteArrayInputStream("test".getBytes())).call(stream -> "result", predicate, supplier);
        assertEquals("result", result);
    }

    @Test
    public void test_instance_call_cmd_predicate_supplier_matching_exception() {
        java.util.function.Predicate<Exception> predicate = ex -> ex instanceof IOException;
        java.util.function.Supplier<String> supplier = () -> "default";
        String result = Try.with(new ByteArrayInputStream("test".getBytes())).call(stream -> {
            throw new IOException("Test exception");
        }, predicate, supplier);
        assertEquals("default", result);
    }

    @Test
    public void test_instance_call_cmd_predicate_supplier_non_matching_exception() {
        java.util.function.Predicate<Exception> predicate = ex -> ex instanceof FileNotFoundException;
        java.util.function.Supplier<String> supplier = () -> "default";
        assertThrows(RuntimeException.class, () -> {
            Try.with(new ByteArrayInputStream("test".getBytes())).call(stream -> {
                throw new IOException("Test exception");
            }, predicate, supplier);
        });
    }

    @Test
    public void test_instance_call_cmd_predicate_supplier_resource_closed() {
        TestCloseable closeable = new TestCloseable();
        java.util.function.Predicate<Exception> predicate = ex -> ex instanceof IOException;
        java.util.function.Supplier<String> supplier = () -> "default";
        Try.with(closeable).call(c -> {
            throw new IOException("Test exception");
        }, predicate, supplier);
        assertTrue(closeable.isClosed());
    }

    @Test
    public void test_instance_call_cmd_predicate_supplier_with_finalAction() {
        AtomicBoolean finalActionRun = new AtomicBoolean(false);
        java.util.function.Predicate<Exception> predicate = ex -> ex instanceof IOException;
        java.util.function.Supplier<String> supplier = () -> "default";
        String result = Try.with(new ByteArrayInputStream("test".getBytes()), () -> finalActionRun.set(true)).call(stream -> {
            throw new IOException("Test exception");
        }, predicate, supplier);
        assertEquals("default", result);
        assertTrue(finalActionRun.get());
    }

    @Test
    public void test_instance_call_cmd_predicate_defaultValue_success() {
        java.util.function.Predicate<Exception> predicate = ex -> ex instanceof IOException;
        String result = Try.with(new ByteArrayInputStream("test".getBytes())).call(stream -> "result", predicate, "default");
        assertEquals("result", result);
    }

    @Test
    public void test_instance_call_cmd_predicate_defaultValue_matching_exception() {
        java.util.function.Predicate<Exception> predicate = ex -> ex instanceof IOException;
        String result = Try.with(new ByteArrayInputStream("test".getBytes())).call(stream -> {
            throw new IOException("Test exception");
        }, predicate, "default");
        assertEquals("default", result);
    }

    @Test
    public void test_instance_call_cmd_predicate_defaultValue_non_matching_exception() {
        java.util.function.Predicate<Exception> predicate = ex -> ex instanceof FileNotFoundException;
        assertThrows(RuntimeException.class, () -> {
            Try.with(new ByteArrayInputStream("test".getBytes())).call(stream -> {
                throw new IOException("Test exception");
            }, predicate, "default");
        });
    }

    @Test
    public void test_instance_call_cmd_predicate_defaultValue_resource_closed() {
        TestCloseable closeable = new TestCloseable();
        java.util.function.Predicate<Exception> predicate = ex -> ex instanceof IOException;
        Try.with(closeable).call(c -> {
            throw new IOException("Test exception");
        }, predicate, "default");
        assertTrue(closeable.isClosed());
    }

    @Test
    public void test_instance_call_cmd_predicate_defaultValue_with_finalAction() {
        AtomicBoolean finalActionRun = new AtomicBoolean(false);
        java.util.function.Predicate<Exception> predicate = ex -> ex instanceof IOException;
        String result = Try.with(new ByteArrayInputStream("test".getBytes()), () -> finalActionRun.set(true)).call(stream -> {
            throw new IOException("Test exception");
        }, predicate, "default");
        assertEquals("default", result);
        assertTrue(finalActionRun.get());
    }

    @Test
    public void test_exception_in_finalAction() {
        AtomicBoolean executed = new AtomicBoolean(false);
        assertThrows(RuntimeException.class, () -> {
            Try.with(new ByteArrayInputStream("test".getBytes()), () -> {
                throw new RuntimeException("Final action exception");
            }).run(stream -> executed.set(true));
        });
        assertTrue(executed.get());
    }

    @Test
    public void test_resource_supplier_throws_exception() {
        Throwables.Supplier<InputStream, Exception> supplier = () -> {
            throw new IOException("Supplier exception");
        };
        assertThrows(RuntimeException.class, () -> {
            Try.with(supplier).run(stream -> {
                fail("Should not reach here");
            });
        });
    }

    @Test
    public void test_multiple_operations_same_try() {
        TestCloseable closeable1 = new TestCloseable();
        Try<TestCloseable> tryInstance = Try.with(closeable1);

        tryInstance.run(c -> {
            assertTrue(!c.isClosed());
        });
        assertTrue(closeable1.isClosed());
    }

    @Test
    public void test_complex_exception_handling() {
        AtomicInteger count = new AtomicInteger(0);

        java.util.function.Function<Exception, String> errorHandler = ex -> {
            if (ex instanceof IOException) {
                count.incrementAndGet();
                return "handled";
            }
            return "unhandled";
        };

        String result = Try.call(() -> {
            count.incrementAndGet();
            if (count.get() == 1) {
                throw new IOException("First attempt");
            }
            return "success";
        }, errorHandler);

        assertEquals("handled", result);
        assertEquals(2, count.get());
    }

    private static class TestCloseable implements AutoCloseable {
        private boolean closed = false;

        public boolean isClosed() {
            return closed;
        }

        @Override
        public void close() throws Exception {
            closed = true;
        }
    }
}
