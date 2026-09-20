package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigInteger;
import java.util.List;
import java.util.Random;
import java.util.function.BiFunction;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@org.junit.jupiter.api.Tag("unit")
public class DurationCancellationTest extends TestBase {
    private record Operation(long unit, boolean subtract, BiFunction<Duration, Long, Duration> apply) {
    }

    private static final List<Operation> OPERATIONS = List.of(new Operation(1000, false, Duration::plusSeconds),
            new Operation(1000, true, Duration::minusSeconds), new Operation(60000, false, Duration::plusMinutes),
            new Operation(60000, true, Duration::minusMinutes), new Operation(3600000, false, Duration::plusHours),
            new Operation(3600000, true, Duration::minusHours), new Operation(86400000, false, Duration::plusDays),
            new Operation(86400000, true, Duration::minusDays));

    private static void check(final Operation operation, final long base, final long amount) {
        final BigInteger scaled = BigInteger.valueOf(amount).multiply(BigInteger.valueOf(operation.unit));
        final BigInteger exact = operation.subtract ? BigInteger.valueOf(base).subtract(scaled) : BigInteger.valueOf(base).add(scaled);
        final Duration original = Duration.ofMillis(base);
        if (exact.bitLength() > 63) {
            assertThrows(ArithmeticException.class, () -> operation.apply.apply(original, amount));
        } else {
            assertEquals(exact.longValueExact(), operation.apply.apply(original, amount).toMillis());
        }
        assertEquals(base, original.toMillis());
    }

    @Test
    void everyUnitAndSignChecksTheFinalResultAgainstAnExactOracle() {
        assertEquals(9223372036854775000L, Duration.ofMillis(-1000).plusSeconds(9223372036854776L).toMillis());
        for (final Operation operation : OPERATIONS) {
            for (final long base : new long[] { Long.MIN_VALUE, Long.MIN_VALUE + 1, -operation.unit, -1, 0, 1, operation.unit, Long.MAX_VALUE - 1,
                    Long.MAX_VALUE }) {
                for (final long amount : new long[] { Long.MIN_VALUE, Long.MAX_VALUE, Long.MAX_VALUE / operation.unit + 1, Long.MIN_VALUE / operation.unit - 1,
                        -1, 0, 1 }) {
                    check(operation, base, amount);
                }
            }
            final Random random = new Random(36);
            for (int i = 0; i < 100; i++) {
                check(operation, random.nextLong(), random.nextLong() / operation.unit);
            }
            final Duration original = Duration.ofMillis(7);
            assertSame(original, operation.apply.apply(original, 0L));
        }
    }
}
