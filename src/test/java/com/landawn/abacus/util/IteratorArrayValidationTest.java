package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@org.junit.jupiter.api.Tag("unit")
public class IteratorArrayValidationTest extends TestBase {
    @Test
    void generatedObjectIteratorRejectsNullBeforeAnyCallback() {
        final AtomicInteger calls = new AtomicInteger();
        final var iter = ObjIterator.generate(() -> calls.get() < 3, calls::getAndIncrement);
        assertThrows(NullPointerException.class, () -> iter.toArray((Integer[]) null));
        assertEquals(0, calls.get());
        assertArrayEquals(new Integer[] { 0, 1, 2 }, iter.toArray(new Integer[0]));
        assertEquals(3, calls.get());
        assertThrows(NullPointerException.class, () -> iter.toArray((Integer[]) null));
    }

    @Test
    void generatedPairAndTripleIteratorsRejectNullBeforeAnyCallback() {
        final AtomicInteger calls = new AtomicInteger();
        final var pairs = BiIterator.<Integer, Integer> generate(0, 3, (i, out) -> {
            calls.incrementAndGet();
            out.set(i, -i);
        });
        assertThrows(NullPointerException.class, () -> pairs.toArray((Object[]) null));
        assertEquals(0, calls.get());
        assertEquals(3, pairs.toArray(new Object[0]).length);
        calls.set(0);
        final var triples = TriIterator.<Integer, Integer, Integer> generate(0, 3, (i, out) -> {
            calls.incrementAndGet();
            out.set(i, -i, i);
        });
        assertThrows(NullPointerException.class, () -> triples.toArray((Object[]) null));
        assertEquals(0, calls.get());
        final Object[] destination = new Object[5];
        java.util.Arrays.fill(destination, "sentinel");
        assertSame(destination, triples.toArray(destination));
        assertEquals(Triple.of(0, 0, 0), destination[0]);
        assertNull(destination[3]);
        assertEquals("sentinel", destination[4]);
        assertEquals(3, calls.get());
    }

    @Test
    void emptyAndUnicodeInputsRetainArraySemantics() {
        assertThrows(NullPointerException.class, () -> ObjIterator.empty().toArray((Object[]) null));
        assertThrows(NullPointerException.class, () -> BiIterator.empty().toArray((Object[]) null));
        assertThrows(NullPointerException.class, () -> TriIterator.empty().toArray((Object[]) null));
        assertArrayEquals(new String[] { null, "", "\uD83D\uDE00" }, ObjIterator.of((String) null, "", "\uD83D\uDE00").toArray(new String[0]));
    }
}
