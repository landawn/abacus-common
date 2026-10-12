package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("unit")
public class NUtilReview20261011Test extends TestBase {
    @Test
    public void setAllGeneratorSeesEarlierWritesInCopyOnWriteLists() {
        final List<Integer> values = new CopyOnWriteArrayList<>(List.of(1, 1, 1));
        // Publishing only once would hide earlier writes from later generator calls.
        N.setAll(values, index -> index == 0 ? 2 : values.get(index - 1) + 1);
        assertEquals(List.of(2, 3, 4), values);
    }

    @Test
    public void setAllRetainsEarlierWritesWhenGeneratorThrows() {
        final List<Integer> values = new CopyOnWriteArrayList<>(List.of(1, 1, 1));
        final RuntimeException failure = new IllegalStateException("generator failed");
        assertSame(failure, assertThrows(RuntimeException.class, () -> N.setAll(values, index -> {
            if (index == 1) {
                throw failure;
            }
            return 2;
        })));
        assertEquals(List.of(2, 1, 1), values);
    }

    @Test
    public void setAllPreservesListOrderNullsAndValidation() {
        for (final List<String> values : List.of(new ArrayList<>(Arrays.asList("a", null, "c")), new LinkedList<>(Arrays.asList("a", null, "c")))) {
            N.setAll(values, index -> index == 1 ? null : "value" + index);
            assertEquals(Arrays.asList("value0", null, "value2"), values);
            assertThrows(IllegalArgumentException.class, () -> N.setAll(values, (java.util.function.IntFunction<String>) null));
        }
        N.setAll(List.of(), index -> {
            throw new AssertionError("An empty list must not call its generator");
        });
    }
}
