package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.landawn.abacus.TestBase;

@Tag("unit")
class EmptySuppliedArrayReview20261003Test extends TestBase {
    static Stream<Arguments> emptyInputs() {
        return Stream.<Collection<String>> of(null, new ArrayList<>(), new LinkedList<>(), new LinkedHashSet<>())
                .flatMap(input -> Stream.of(Arguments.of(input, false), Arguments.of(input, true)));
    }

    @ParameterizedTest
    @MethodSource("emptyInputs")
    void oversizedSuppliedArrayIsTerminatedForEmptyInput(Collection<String> input, boolean range) {
        String[] supplied = { "stale", "untouched" };
        AtomicInteger calls = new AtomicInteger();
        com.landawn.abacus.util.function.IntFunction<String[]> supplier = size -> {
            assertEquals(0, size);
            calls.incrementAndGet();
            return supplied;
        };
        String[] result = range ? CommonUtil.toArray(input, 0, 0, supplier) : CommonUtil.toArray(input, supplier);
        assertSame(supplied, result);
        assertArrayEquals(new String[] { null, "untouched" }, result);
        assertEquals(1, calls.get());
    }

    @Test
    void zeroLengthSuppliedArrayRetainsIdentity() {
        String[] supplied = {};
        assertSame(supplied, CommonUtil.toArray(List.<String> of(), ignored -> supplied));
        assertSame(supplied, CommonUtil.toArray((Collection<String>) null, 0, 0, ignored -> supplied));
    }

    @Test
    void nullResultStillThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> CommonUtil.toArray(List.<String> of(), ignored -> (String[]) null));
        assertThrows(NullPointerException.class, () -> CommonUtil.toArray((Collection<String>) null, 0, 0, ignored -> (String[]) null));
    }

    @Test
    void invalidRangeIsRejectedBeforeCallingSupplier() {
        AtomicInteger calls = new AtomicInteger();
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.toArray(List.<String> of(), 0, 1, size -> {
            calls.incrementAndGet();
            return new String[size];
        }));
        assertEquals(0, calls.get());
    }

    @Test
    void nonemptyInputWithEmptyRangeAndNonemptyRangesKeepArrayContract() {
        assertArrayEquals(new String[] { null, "keep" }, CommonUtil.toArray(List.of("a"), 0, 0, size -> new String[] { "stale", "keep" }));
        assertArrayEquals(new String[] { "b", null, "keep" },
                CommonUtil.toArray(new LinkedHashSet<>(List.of("a", "b", "c")), 1, 2, size -> new String[] { "stale", "stale", "keep" }));
        assertArrayEquals(new String[] { "a", "b" }, CommonUtil.toArray(List.of("a", "b"), size -> new String[0]));
    }
}
