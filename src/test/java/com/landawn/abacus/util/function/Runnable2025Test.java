package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class Runnable2025Test extends TestBase {

    @Test
    public void testRun() {
        final int[] counter = {0};
        Runnable runnable = () -> counter[0]++;

        runnable.run();

        assertEquals(1, counter[0]);
    }

    @Test
    public void testRun_WithAnonymousClass() {
        final StringBuilder builder = new StringBuilder();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                builder.append("executed");
            }
        };

        runnable.run();

        assertEquals("executed", builder.toString());
    }

    @Test
    public void testRun_MultipleInvocations() {
        final int[] counter = {0};
        Runnable runnable = () -> counter[0]++;

        runnable.run();
        runnable.run();
        runnable.run();

        assertEquals(3, counter[0]);
    }

    @Test
    public void testToCallable() {
        final int[] counter = {0};
        Runnable runnable = () -> counter[0]++;

        Callable<Void> callable = runnable.toCallable();
        Void result = callable.call();

        assertEquals(1, counter[0]);
        assertNull(result);
    }

    @Test
    public void testToThrowable() {
        Runnable runnable = () -> {};
        com.landawn.abacus.util.Throwables.Runnable<?> throwableRunnable = runnable.toThrowable();
        assertNotNull(throwableRunnable);
    }

    @Test
    public void testJavaLangCompatibility() {
        final int[] counter = {0};
        Runnable runnable = () -> counter[0]++;
        java.lang.Runnable javaRunnable = runnable;

        javaRunnable.run();

        assertEquals(1, counter[0]);
    }

    @Test
    public void testFunctionalInterfaceContract() {
        Runnable lambda = () -> {};
        assertNotNull(lambda);
        assertDoesNotThrow(() -> lambda.run());
    }
}
