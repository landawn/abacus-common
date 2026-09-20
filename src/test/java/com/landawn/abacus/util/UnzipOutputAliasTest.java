package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@org.junit.jupiter.api.Tag("unit")
public class UnzipOutputAliasTest extends TestBase {
    @Test
    void pairOutputsRejectAliasesWithoutConsumingOrMutatingCollections() {
        final AtomicInteger calls = new AtomicInteger();
        final var source = BiIterator.<Integer, Integer> generate(0, 2, (i, out) -> {
            calls.incrementAndGet();
            out.set(i, -i);
        });
        final var list = new ArrayList<>(List.of(7));
        final var set = new LinkedHashSet<Integer>();
        assertThrows(IllegalArgumentException.class, () -> source.unzipToLists(() -> list));
        assertThrows(IllegalArgumentException.class, () -> source.unzipToSets(() -> set));
        assertThrows(IllegalArgumentException.class, () -> source.unzipToCollections(() -> list, () -> list));
        assertEquals(0, calls.get());
        assertEquals(List.of(7), list);
        final var result = source.unzipToCollections(ArrayList::new, ArrayList::new);
        assertEquals(List.of(0, 1), result.left());
        assertEquals(List.of(0, -1), result.right());
    }

    @Test
    void triplesRejectEveryAliasPairAndSharedListOrSetSuppliers() {
        for (int pair = 0; pair < 3; pair++) {
            final AtomicInteger calls = new AtomicInteger();
            final var source = TriIterator.<Integer, Integer, Integer> generate(0, 2, (i, out) -> {
                calls.incrementAndGet();
                out.set(i, -i, 10 + i);
            });
            final var first = new ArrayList<Integer>();
            final var second = pair == 0 ? first : new ArrayList<Integer>();
            final var third = pair == 1 ? first : pair == 2 ? second : new ArrayList<Integer>();
            assertThrows(IllegalArgumentException.class, () -> source.unzipToCollections(() -> first, () -> second, () -> third));
            assertThrows(IllegalArgumentException.class, () -> source.unzipToLists(() -> first));
            final var set = new LinkedHashSet<Integer>();
            assertThrows(IllegalArgumentException.class, () -> source.unzipToSets(() -> set));
            assertEquals(0, calls.get());
            assertTrue(first.isEmpty());
            assertEquals(List.of(10, 11), source.unzipToCollections(ArrayList::new, ArrayList::new, ArrayList::new).right());
        }
    }

    @Test
    void emptySourcesStillRejectAliasedOutputsAndKeepNullValidation() {
        final var shared = new ArrayList<>();
        assertThrows(IllegalArgumentException.class, () -> BiIterator.empty().unzipToLists(() -> shared));
        assertThrows(IllegalArgumentException.class, () -> TriIterator.empty().unzipToLists(() -> shared));
        assertThrows(IllegalArgumentException.class, () -> BiIterator.empty().unzipToLists(null));
        assertThrows(IllegalArgumentException.class, () -> TriIterator.empty().unzipToLists(() -> null));
    }
}
