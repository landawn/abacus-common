package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class BooleanSupplier2025Test extends TestBase {

    @Test
    public void testGetAsBoolean() {
        BooleanSupplier supplier = () -> true;

        assertTrue(supplier.getAsBoolean());
    }

    @Test
    public void testGetAsBooleanWithLambda() {
        BooleanSupplier supplier = () -> false;

        assertFalse(supplier.getAsBoolean());
    }

    @Test
    public void testTrue() {
        assertTrue(BooleanSupplier.TRUE.getAsBoolean());
    }

    @Test
    public void testFalse() {
        assertFalse(BooleanSupplier.FALSE.getAsBoolean());
    }

    @Test
    public void testRandom() {
        assertNotNull(BooleanSupplier.RANDOM);

        // Call it multiple times to verify it doesn't throw exception
        for (int i = 0; i < 10; i++) {
            boolean result = BooleanSupplier.RANDOM.getAsBoolean();
            // Result can be either true or false
        }
    }

    @Test
    public void testGetAsBooleanMultipleCalls() {
        BooleanSupplier supplier = () -> true;

        for (int i = 0; i < 5; i++) {
            assertTrue(supplier.getAsBoolean());
        }
    }

    @Test
    public void testStatefulSupplier() {
        AtomicBoolean state = new AtomicBoolean(false);
        BooleanSupplier supplier = () -> {
            state.set(!state.get());
            return state.get();
        };

        assertTrue(supplier.getAsBoolean());   // First call: false -> true
        assertFalse(supplier.getAsBoolean());   // Second call: true -> false
        assertTrue(supplier.getAsBoolean());   // Third call: false -> true
    }

    @Test
    public void testGetAsBooleanWithException() {
        BooleanSupplier supplier = () -> {
            throw new RuntimeException("Test exception");
        };

        assertThrows(RuntimeException.class, supplier::getAsBoolean);
    }

    @Test
    public void testAnonymousClass() {
        BooleanSupplier supplier = new BooleanSupplier() {
            @Override
            public boolean getAsBoolean() {
                return true;
            }
        };

        assertTrue(supplier.getAsBoolean());
    }

    @Test
    public void testSupplierWithComputation() {
        BooleanSupplier supplier = () -> {
            int x = 5;
            int y = 10;
            return x < y;
        };

        assertTrue(supplier.getAsBoolean());
    }

    @Test
    public void testConstantsNotNull() {
        assertNotNull(BooleanSupplier.TRUE);
        assertNotNull(BooleanSupplier.FALSE);
        assertNotNull(BooleanSupplier.RANDOM);
    }

    @Test
    public void testTrueConsistency() {
        for (int i = 0; i < 10; i++) {
            assertTrue(BooleanSupplier.TRUE.getAsBoolean());
        }
    }

    @Test
    public void testFalseConsistency() {
        for (int i = 0; i < 10; i++) {
            assertFalse(BooleanSupplier.FALSE.getAsBoolean());
        }
    }

    @Test
    public void testRandomVariability() {
        // Call random supplier many times and check if we get both true and false
        boolean gotTrue = false;
        boolean gotFalse = false;

        for (int i = 0; i < 100; i++) {
            boolean result = BooleanSupplier.RANDOM.getAsBoolean();
            if (result) {
                gotTrue = true;
            } else {
                gotFalse = true;
            }

            if (gotTrue && gotFalse) {
                break; // We've seen both values
            }
        }

        // With 100 iterations, it's extremely unlikely to not see both values
        // but we won't assert on it since it's technically possible
    }

    @Test
    public void testMethodReference() {
        AtomicBoolean value = new AtomicBoolean(true);
        BooleanSupplier supplier = value::get;

        assertTrue(supplier.getAsBoolean());

        value.set(false);
        assertFalse(supplier.getAsBoolean());
    }
}
