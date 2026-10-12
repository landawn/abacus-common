package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("unit")
class DeepReview20261004IteratorsTest extends TestBase {

    @Test
    void anIteratorEqualsItselfWithoutAdvancingTwice() {
        final Iterator<Integer> single = List.of(1).iterator();
        assertTrue(Iterators.equalsInOrder(single, single));
        assertEquals(1, single.next());

        final Iterator<Integer> multiple = List.of(1, 2, 3).iterator();
        multiple.next();
        assertTrue(Iterators.equalsInOrder(multiple, multiple));
        assertEquals(2, multiple.next());
    }
}
