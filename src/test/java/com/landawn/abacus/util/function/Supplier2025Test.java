package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class Supplier2025Test extends TestBase {

    @Test
    public void testGet() {
        Supplier<String> supplier = () -> "hello";
        String result = supplier.get();
        assertEquals("hello", result);
    }

    @Test
    public void testGet_WithAnonymousClass() {
        Supplier<Integer> supplier = new Supplier<Integer>() {
            @Override
            public Integer get() {
                return 42;
            }
        };

        Integer result = supplier.get();
        assertEquals(42, result);
    }

    @Test
    public void testGet_MultipleInvocations() {
        Supplier<String> supplier = () -> "constant";
        assertEquals("constant", supplier.get());
        assertEquals("constant", supplier.get());
        assertEquals("constant", supplier.get());
    }

    @Test
    public void testGet_WithState() {
        final int[] counter = {0};
        Supplier<Integer> supplier = () -> ++counter[0];

        assertEquals(1, supplier.get());
        assertEquals(2, supplier.get());
        assertEquals(3, supplier.get());
    }

    @Test
    public void testToThrowable() {
        Supplier<String> supplier = () -> "test";
        com.landawn.abacus.util.Throwables.Supplier<String, ?> throwableSupplier = supplier.toThrowable();
        assertNotNull(throwableSupplier);
    }

    @Test
    public void testJavaUtilFunctionCompatibility() {
        Supplier<String> supplier = () -> "hello";
        java.util.function.Supplier<String> javaSupplier = supplier;

        String result = javaSupplier.get();
        assertEquals("hello", result);
    }

    @Test
    public void testFunctionalInterfaceContract() {
        Supplier<String> lambda = () -> "test";
        assertNotNull(lambda);
        assertDoesNotThrow(() -> lambda.get());
    }
}
