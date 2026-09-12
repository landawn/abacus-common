package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Comparator;

import org.junit.jupiter.api.Test;

@org.junit.jupiter.api.Tag("unit")
public class MedianNullPresenceTest {
    @Test
    void upperNullRemainsPresentForEveryEvenSizeAcrossArrayAndCollectionOverloads() {
        for (int length = 1; length <= 8; length++) {
            final String[] values = new String[length];
            for (final Comparator<String> order : java.util.List.of(Comparator.<String> nullsFirst(Comparator.naturalOrder()),
                    Comparator.<String> nullsLast(Comparator.naturalOrder()))) {
                final var result = Median.of(values, order);
                assertNull(result.left());
                assertEquals(length % 2 == 0, result.right().isPresent());
                if (result.right().isPresent()) {
                    assertNull(result.right().get());
                }
                assertEquals(result, Median.of(Arrays.asList(values), order));
                assertEquals(result, Median.of(values, 0, values.length, order));
                assertEquals(result, Median.of(Arrays.asList(values), 0, values.length, order));
            }
        }
    }

    @Test
    void oddAbsenceAndEvenNullAreDifferentAndInputsRemainUnchanged() {
        final var order = Comparator.<String> nullsLast(Comparator.naturalOrder());
        final String[] values = { "\uD83D\uDE00", null };
        final var odd = Median.of(new String[] { "\uD83D\uDE00" }, order);
        final var even = Median.of(values, order);
        assertEquals(odd.left(), even.left());
        assertTrue(odd.right().isEmpty());
        assertTrue(even.right().isPresent());
        assertNull(even.right().get());
        assertNotEquals(odd, even);
        assertArrayEquals(new String[] { "\uD83D\uDE00", null }, values);
        assertThrows(IllegalArgumentException.class, () -> Median.of(new String[0]));
        assertThrows(IllegalArgumentException.class, () -> Median.of((String[]) null));
    }
}
