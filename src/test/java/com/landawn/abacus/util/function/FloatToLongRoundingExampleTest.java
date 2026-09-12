package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@Tag("unit")
class FloatToLongRoundingExampleTest {
    @ParameterizedTest
    @CsvSource({ "3.7, 4", "-3.7, -4", "0.5, 1", "-0.5, 0", "0.0, 0", "-0.0, 0",
            "3000000000, 3000000000", "-3000000000, -3000000000", "2147483648, 2147483648",
            "-2147483904, -2147483904", "NaN, 0", "Infinity, 9223372036854775807",
            "-Infinity, -9223372036854775808", "3.4028235E38, 9223372036854775807", "-3.4028235E38, -9223372036854775808" })
    void roundingUsesTheLongResultRange(float input, long expected) {
        FloatToLongFunction round = value -> Math.round((double) value);
        assertEquals(expected, round.applyAsLong(input));
    }
}
