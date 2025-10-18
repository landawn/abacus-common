package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class DoubleTriFunction2025Test extends TestBase {
    @Test
    public void testApply_CalculatingVolume() {
        DoubleTriFunction<Double> function = (length, width, height) -> length * width * height;
        Double result = function.apply(2.0, 3.0, 4.0);
        assertEquals(24.0, result, 0.0001);
    }

    @Test
    public void testFunctionalInterfaceContract() {
        DoubleTriFunction<String> lambda = (a, b, c) -> "test";
        assertNotNull(lambda);
        assertDoesNotThrow(() -> lambda.apply(1.0, 2.0, 3.0));
    }
}
