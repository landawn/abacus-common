package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.Test;

public class ThrowablesCallTest extends ThrowablesTestSupport {
    @Test
    public void testCall_Success() {
        String result = Throwables.call(() -> "Success");
        assertEquals("Success", result);
    }

    @Test
    public void testCall_ThrowsCheckedException() {
        assertThrows(RuntimeException.class, () -> Throwables.call(() -> {
            throw new TestException("Test exception");
        }));
    }

    @Test
    public void testCall_NullCommand() {
        assertThrows(IllegalArgumentException.class, () -> Throwables.call(null));
    }

    @Test
    public void testCall_WithErrorFunction_Success() {
        String result = Throwables.call(() -> "Success", (java.util.function.Function<Throwable, String>) e -> "Error: " + e.getMessage());
        assertEquals("Success", result);
    }

    @Test
    public void testCall_WithErrorFunction_HandlesException() {
        String result = Throwables.call(() -> {
            throw new TestException("Test exception");
        }, (java.util.function.Function<Throwable, String>) e -> "Error: " + e.getMessage());

        assertEquals("Error: Test exception", result);
    }

    @Test
    public void testCall_WithErrorFunction_NullCommand() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> Throwables.call(null, (java.util.function.Function<Throwable, String>) e -> "Error"));
    }

    @Test
    public void testCall_WithErrorFunction_NullFunction() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> Throwables.call(() -> "Success", (java.util.function.Function<Throwable, String>) null));
    }

    @Test
    public void testCall_WithSupplier_Success() {
        String result = Throwables.call(() -> "Success", (java.util.function.Supplier<String>) () -> "Default");
        assertEquals("Success", result);
    }

    @Test
    public void testCall_WithSupplier_HandlesException() {
        String result = Throwables.call(() -> {
            throw new TestException("Test exception");
        }, (java.util.function.Supplier<String>) () -> "Default");

        assertEquals("Default", result);
    }

    @Test
    public void testCall_WithSupplier_NullCommand() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> Throwables.call(null, (java.util.function.Supplier<String>) () -> "Default"));
    }

    @Test
    public void testCall_WithSupplier_NullSupplier() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> Throwables.call(() -> "Success", (java.util.function.Supplier<String>) null));
    }

    @Test
    public void testCall_WithDefaultValue_Success() {
        Integer result = Throwables.call(() -> 42, 0);
        assertEquals(42, result);
    }

    @Test
    public void testCall_WithDefaultValue_HandlesException() {
        Integer result = Throwables.call(() -> {
            throw new TestException("Test exception");
        }, 0);

        assertEquals(0, result);
    }

    @Test
    public void testCall_WithDefaultValue_NullCommand() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> Throwables.call(null, 0));
    }

    @Test
    public void testCall_WithDefaultValue_NullDefaultValue() {
        Integer result = Throwables.call(() -> {
            throw new TestException("Test exception");
        }, (Integer) null);

        assertNull(result);
    }

    @Test
    public void testCall_WithPredicateAndSupplier_Success() {
        String result = Throwables.call(() -> "Success", e -> true, (java.util.function.Supplier<String>) () -> "Default");
        assertEquals("Success", result);
    }

    @Test
    public void testCall_WithPredicateAndSupplier_PredicateTrue() {
        String result = Throwables.call(() -> {
            throw new TestException("Test exception");
        }, e -> e instanceof TestException, (java.util.function.Supplier<String>) () -> "Handled");

        assertEquals("Handled", result);
    }

    @Test
    public void testCall_WithPredicateAndSupplier_PredicateFalse() {
        assertThrows(RuntimeException.class, () -> Throwables.call(() -> {
            throw new TestException("Test exception");
        }, e -> e instanceof IOException, (java.util.function.Supplier<String>) () -> "Handled"));
    }

    @Test
    public void testCall_WithPredicateAndSupplier_NullCommand() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> Throwables.call(null, e -> true, (java.util.function.Supplier<String>) () -> "Default"));
    }

    @Test
    public void testCall_WithPredicateAndSupplier_NullPredicate() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> Throwables.call(() -> "Success", null, (java.util.function.Supplier<String>) () -> "Default"));
    }

    @Test
    public void testCall_WithPredicateAndSupplier_NullSupplier() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> Throwables.call(() -> "Success", e -> true, (java.util.function.Supplier<String>) null));
    }

    @Test
    public void testCall_WithPredicateAndDefaultValue_Success() {
        Integer result = Throwables.call(() -> 42, e -> true, 0);
        assertEquals(42, result);
    }

    @Test
    public void testCall_WithPredicateAndDefaultValue_PredicateTrue() {
        Integer result = Throwables.call(() -> {
            throw new TestException("Test exception");
        }, e -> e instanceof TestException, 0);

        assertEquals(0, result);
    }

    @Test
    public void testCall_WithPredicateAndDefaultValue_PredicateFalse() {
        assertThrows(RuntimeException.class, () -> Throwables.call(() -> {
            throw new TestException("Test exception");
        }, e -> e instanceof IOException, 0));
    }

    @Test
    public void testCall_WithPredicateAndDefaultValue_NullCommand() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> Throwables.call(null, e -> true, 0));
    }

    @Test
    public void testCall_WithPredicateAndDefaultValue_NullPredicate() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> Throwables.call(() -> 42, null, 0));
    }

    @Test
    public void testCall_withoutException() {
        String result = Throwables.call(() -> "success");
        assertEquals("success", result, "The result of the callable should be returned.");
    }

    @Test
    public void testCall_withCheckedException() {
        assertThrows(RuntimeException.class, () -> {
            Throwables.call(() -> {
                if (true) {
                    throw new IOException("Test Exception");
                }
                return "failure";
            });
        }, "A checked exception should be wrapped in a RuntimeException.");
    }

    @Test
    public void testCall_withActionOnError() {
        String result = Throwables.call(() -> {
            throw new IOException("Test");
        }, Fn.s(() -> "handled"));
        assertEquals("handled", result, "The actionOnError function should provide the return value.");
    }

    @Test
    public void testCall_withSupplier() {
        String result = Throwables.call(() -> {
            throw new Exception("Test");
        }, Fn.s(() -> "supplied"));
        assertEquals("supplied", result, "The supplier should provide the return value on error.");
    }

    @Test
    public void testCall_withDefaultValue() {
        String result = Throwables.call(() -> {
            throw new Exception("Test");
        }, "default");
        assertEquals("default", result, "The default value should be returned on error.");
    }

    @Test
    public void testCall_withPredicateAndSupplier_predicateTrue() {
        String result = Throwables.call(() -> {
            throw new IOException("IO Test");
        }, e -> e instanceof IOException, Fn.s(() -> "supplied_on_io"));
        assertEquals("supplied_on_io", result, "Supplier should be used when predicate is true.");
    }

    @Test
    public void testCall_withPredicateAndSupplier_predicateFalse() {
        assertThrows(RuntimeException.class, () -> {
            Throwables.call(() -> {
                throw new IllegalArgumentException("Arg Test");
            }, e -> e instanceof IOException, Fn.s(() -> "supplied_on_io"));
        }, "Exception should be rethrown when predicate is false.");
    }

    @Test
    public void testCall_withPredicateAndDefaultValue_predicateTrue() {
        String result = Throwables.call(() -> {
            throw new IOException("IO Test");
        }, e -> e instanceof IOException, "default_on_io");
        assertEquals("default_on_io", result, "Default value should be used when predicate is true.");
    }

    @Test
    public void testCall_withPredicateAndDefaultValue_predicateFalse() {
        assertThrows(RuntimeException.class, () -> {
            Throwables.call(() -> {
                throw new IllegalArgumentException("Arg Test");
            }, e -> e instanceof IOException, "default_on_io");
        }, "Exception should be rethrown when predicate is false.");
    }

    @Test
    public void testCall_ReturnsNull() {
        String result = Throwables.call(() -> null);
        assertNull(result);
    }

    @Test
    public void testCall_WithSupplier_ReturnsSupplierValueOnError() {
        String result = Throwables.call(() -> {
            throw new TestException("Error");
        }, Fn.s(() -> "Default value"));

        assertEquals("Default value", result);
    }

    @Test
    public void testCall_WithDefaultValue_ReturnsDefaultOnError() {
        String result = Throwables.call(() -> {
            throw new TestException("Error");
        }, "Default value");

        assertEquals("Default value", result);
    }

    @Test
    public void testCall_WithNullDefaultValue_ReturnsNullOnError() {
        String result = Throwables.call(() -> {
            throw new TestException("Error");
        }, (String) null);

        assertNull(result);
    }

    @Test
    public void testCall_WithPredicateSupplier_Success() {
        String result = Throwables.call(() -> "Success", e -> e instanceof TestException, Fn.s(() -> "Handled"));
        assertEquals("Success", result);
    }

    @Test
    public void testCall_WithPredicateSupplier_PredicateTrue() {
        String result = Throwables.call(() -> {
            throw new TestException("Error");
        }, e -> e instanceof TestException, Fn.s(() -> "Handled by predicate"));
        assertEquals("Handled by predicate", result);
    }

    @Test
    public void testCall_WithPredicateSupplier_PredicateFalse() {
        assertThrows(RuntimeException.class, () -> Throwables.call(() -> {
            throw new TestException("Error");
        }, e -> e instanceof IOException, Fn.s(() -> "Should not reach here")));
    }

    @Test
    public void testCall_WithPredicateDefault_Success() {
        String result = Throwables.call(() -> "Success", e -> e instanceof TestException, "Default");
        assertEquals("Success", result);
    }

    @Test
    public void testCall_WithPredicateDefault_PredicateTrue() {
        String result = Throwables.call(() -> {
            throw new TestException("Error");
        }, e -> e instanceof TestException, "Default value");
        assertEquals("Default value", result);
    }

    @Test
    public void testCall_WithPredicateDefault_PredicateFalse() {
        assertThrows(RuntimeException.class, () -> Throwables.call(() -> {
            throw new TestException("Error");
        }, e -> e instanceof IOException, "Should not reach here"));
    }

    @Test
    public void testCall_ErrorIsAbsorbedByEveryFallbackOverload() {
        final Throwables.Callable<String, Exception> boom = () -> {
            throw new StackOverflowError("simulated");
        };

        assertEquals("default", Throwables.call(boom, "default"));
        assertEquals("from-supplier", Throwables.call(boom, (java.util.function.Supplier<String>) () -> "from-supplier"));
        assertEquals("handled:simulated", Throwables.call(boom, (java.util.function.Function<Throwable, String>) e -> "handled:" + e.getMessage()));
        assertEquals("accepted", Throwables.call(boom, e -> e instanceof StackOverflowError, "accepted"));
        assertEquals("accepted", Throwables.call(boom, e -> e instanceof StackOverflowError, (java.util.function.Supplier<String>) () -> "accepted"));
    }

    @Test
    public void testCall_ErrorIsConvertedWithNoFallbackOrWhenThePredicateRejectsIt() {
        final Throwables.Callable<String, Exception> boom = () -> {
            throw new StackOverflowError("simulated");
        };

        final RuntimeException noFallback = assertThrows(RuntimeException.class, () -> Throwables.call(boom));
        assertTrue(noFallback.getCause() instanceof StackOverflowError);

        final RuntimeException supplierPredicateRejects = assertThrows(RuntimeException.class,
                () -> Throwables.call(boom, e -> false, (java.util.function.Supplier<String>) () -> "unused"));
        assertTrue(supplierPredicateRejects.getCause() instanceof StackOverflowError);

        final RuntimeException defaultPredicateRejects = assertThrows(RuntimeException.class, () -> Throwables.call(boom, e -> false, "unused"));
        assertTrue(defaultPredicateRejects.getCause() instanceof StackOverflowError);
    }
}
