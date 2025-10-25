package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class Callable2025Test extends TestBase {

    @Test
    public void testCall() {
        Callable<String> callable = () -> "result";
        String result = callable.call();
        assertEquals("result", result);
    }

    @Test
    public void testCall_WithAnonymousClass() {
        Callable<Integer> callable = new Callable<Integer>() {
            @Override
            public Integer call() {
                return 42;
            }
        };

        Integer result = callable.call();
        assertEquals(42, result);
    }

    @Test
    public void testCall_WithComputation() {
        Callable<Integer> callable = () -> {
            int sum = 0;
            for (int i = 1; i <= 10; i++) {
                sum += i;
            }
            return sum;
        };

        Integer result = callable.call();
        assertEquals(55, result);
    }

    @Test
    public void testToRunnable() {
        final int[] counter = { 0 };
        Callable<String> callable = () -> {
            counter[0]++;
            return "executed";
        };

        Runnable runnable = callable.toRunnable();
        runnable.run();

        assertEquals(1, counter[0]);
    }

    @Test
    public void testToThrowable() {
        Callable<String> callable = () -> "test";
        com.landawn.abacus.util.Throwables.Callable<String, ?> throwableCallable = callable.toThrowable();
        assertNotNull(throwableCallable);
    }

    @Test
    public void testJavaUtilConcurrentCompatibility() throws Exception {
        Callable<String> callable = () -> "result";
        java.util.concurrent.Callable<String> javaCallable = callable;

        String result = javaCallable.call();
        assertEquals("result", result);
    }

    @Test
    public void testFunctionalInterfaceContract() {
        Callable<String> lambda = () -> "test";
        assertNotNull(lambda);
        assertDoesNotThrow(() -> lambda.call());
    }
}
