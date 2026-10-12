package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.stream.Stream;

@Tag("unit")
class DeepReview20261004ZeroElementSelectionTest extends TestBase {

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void zeroElementsDoesNotEvaluateFilteredSource(final boolean first) {
        try (Stream<Integer> source = Stream.of(1, 2).filter(value -> {
            throw new AssertionError("A zero-element request must not evaluate the source");
        })) {
            final Iterator<Integer> iterator = source.iterator();

            assertEquals(List.of(), first ? CommonUtil.firstElements(iterator, 0) : CommonUtil.lastElements(iterator, 0));
        }
    }
}
