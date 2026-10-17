package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.landawn.abacus.TestBase;

/** Validation must preserve the original failure and release the source exactly once. */
public class StreamValidationContractTest extends TestBase {
    @FunctionalInterface
    interface Check {
        void accept(Stream<Integer> stream, boolean valid);
    }

    record Validation(String name, Check check, Class<? extends RuntimeException> failure) {
        @Override
        public String toString() {
            return name;
        }
    }

    static List<Validation> validations() {
        return List.of(new Validation("index", (s, ok) -> s.checkIndex(ok ? 0 : -1, 1), IndexOutOfBoundsException.class),
                new Validation("range", (s, ok) -> s.checkFromToIndex(0, ok ? 1 : 2, 1), IndexOutOfBoundsException.class),
                new Validation("size", (s, ok) -> s.checkFromIndexSize(0, ok ? 1 : 2, 1), IndexOutOfBoundsException.class),
                new Validation("positiveInt", (s, ok) -> s.checkArgPositive(ok ? 1 : 0, "size"), IllegalArgumentException.class),
                new Validation("positiveLong", (s, ok) -> s.checkArgPositive(ok ? 1L : 0L, "size"), IllegalArgumentException.class),
                new Validation("nonnegativeInt", (s, ok) -> s.checkArgNotNegative(ok ? 0 : -1, "size"), IllegalArgumentException.class),
                new Validation("nonnegativeLong", (s, ok) -> s.checkArgNotNegative(ok ? 0L : -1L, "size"), IllegalArgumentException.class),
                new Validation("nonnull", (s, ok) -> s.checkArgNotNull(ok ? "value" : null), IllegalArgumentException.class),
                new Validation("nonnullMessage", (s, ok) -> s.checkArgNotNull(ok ? "value" : null, "value"), IllegalArgumentException.class),
                new Validation("argumentMessage", (s, ok) -> s.checkArgument(ok, "invalid"), IllegalArgumentException.class),
                new Validation("argumentInt", (s, ok) -> s.checkArgument(ok, "invalid {}", 1), IllegalArgumentException.class),
                new Validation("argumentLong", (s, ok) -> s.checkArgument(ok, "invalid {}", 1L), IllegalArgumentException.class),
                new Validation("argumentObject", (s, ok) -> s.checkArgument(ok, "invalid {}", "a"), IllegalArgumentException.class),
                new Validation("argumentTwoInts", (s, ok) -> s.checkArgument(ok, "invalid {} {}", 1, 2), IllegalArgumentException.class),
                new Validation("argumentTwoLongs", (s, ok) -> s.checkArgument(ok, "invalid {} {}", 1L, 2L), IllegalArgumentException.class),
                new Validation("argumentTwoObjects", (s, ok) -> s.checkArgument(ok, "invalid {} {}", "a", "b"), IllegalArgumentException.class),
                new Validation("argumentThreeObjects", (s, ok) -> s.checkArgument(ok, "invalid {} {} {}", "a", "b", "c"), IllegalArgumentException.class),
                new Validation("state", (s, ok) -> s.checkState(ok), IllegalStateException.class),
                new Validation("stateMessage", (s, ok) -> s.checkState(ok, "invalid"), IllegalStateException.class),
                new Validation("stateInt", (s, ok) -> s.checkState(ok, "invalid {}", 1), IllegalStateException.class),
                new Validation("stateLong", (s, ok) -> s.checkState(ok, "invalid {}", 1L), IllegalStateException.class),
                new Validation("stateObject", (s, ok) -> s.checkState(ok, "invalid {}", "a"), IllegalStateException.class),
                new Validation("stateTwoInts", (s, ok) -> s.checkState(ok, "invalid {} {}", 1, 2), IllegalStateException.class),
                new Validation("stateTwoLongs", (s, ok) -> s.checkState(ok, "invalid {} {}", 1L, 2L), IllegalStateException.class),
                new Validation("stateTwoObjects", (s, ok) -> s.checkState(ok, "invalid {} {}", "a", "b"), IllegalStateException.class),
                new Validation("stateThreeObjects", (s, ok) -> s.checkState(ok, "invalid {} {} {}", "a", "b", "c"), IllegalStateException.class));
    }

    @ParameterizedTest
    @MethodSource("validations")
    void validArgumentsKeepTheSourceUsable(Validation validation) {
        AtomicInteger closes = new AtomicInteger();
        try (Stream<Integer> stream = Stream.of(1, 2).onClose(closes::incrementAndGet)) {
            validation.check.accept(stream, true);
            assertEquals(0, closes.get());
            assertEquals(List.of(1, 2), stream.toList());
        }
        assertEquals(1, closes.get());
    }

    @ParameterizedTest
    @MethodSource("validations")
    void invalidArgumentsCloseTheSourceAndSuppressCleanupFailures(Validation validation) {
        for (boolean failOnClose : new boolean[] { false, true }) {
            AtomicInteger closes = new AtomicInteger();
            IllegalStateException closeFailure = new IllegalStateException("close failed");
            try (Stream<Integer> stream = Stream.of(1, 2).onClose(() -> {
                closes.incrementAndGet();
                if (failOnClose) {
                    throw closeFailure;
                }
            })) {
                RuntimeException actual = assertThrows(validation.failure, () -> validation.check.accept(stream, false));
                assertNotSame(closeFailure, actual);
                assertArrayEquals(failOnClose ? new Throwable[] { closeFailure } : new Throwable[0], actual.getSuppressed());
                assertEquals(1, closes.get());
                assertThrows(IllegalStateException.class, stream::count);
            }
            assertEquals(1, closes.get());
        }
    }
}
