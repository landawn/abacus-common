package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class SeqLifecycleTest extends TestBase {

    @Test
    public void testFlatMapAndDeferRejectClosedChildrenWhenTraversalReachesThem() throws Exception {
        final Seq<Integer, Exception> child = materialized();
        child.close();
        assertThrows(IllegalStateException.class, () -> Seq.<Integer, Exception> of(0).flatMap(value -> child).toList());
        assertThrows(IllegalStateException.class, () -> Seq.<Integer, Exception> defer(() -> child).toList());
        final AtomicInteger calls = new AtomicInteger();
        final Seq<Integer, Exception> unused = Seq.defer(() -> {
            calls.incrementAndGet();
            return child;
        });
        unused.close();
        assertEquals(0, calls.get());
    }

    @Test
    public void testCompositeCloseEndsHandlerFreeChildLifecycles() throws Exception {
        for (int operation = 0; operation < 3; operation++) {
            final Seq<Integer, Exception> first = materialized();
            final Seq<Integer, Exception> second = materialized();
            final Seq<Integer, Exception> combined = switch (operation) {
                case 0 -> Seq.concat(first, second);
                case 1 -> Seq.zip(first, second, Integer::sum);
                default -> Seq.merge(first, second, (a, b) -> MergeResult.TAKE_FIRST);
            };
            combined.close();
            combined.close();
            assertThrows(IllegalStateException.class, first::toList);
            assertThrows(IllegalStateException.class, second::toList);
        }
        final Seq<Integer, Exception> empty = Seq.empty();
        Seq.concat(empty).close();
        assertThrows(IllegalStateException.class, empty::toList);
    }

    @Test
    public void testCompositeCloseStillClosesAllChildrenAfterAHandlerFails() throws Exception {
        final IllegalArgumentException failure = new IllegalArgumentException("first close");
        final Seq<Integer, Exception> first = materialized().onClose(() -> {
            throw failure;
        });
        final Seq<Integer, Exception> second = materialized();
        final AtomicInteger closes = new AtomicInteger();
        final Seq<Integer, Exception> third = materialized().onClose(closes::incrementAndGet);
        final Seq<Integer, Exception> combined = Seq.concat(first, second, third);
        assertSame(failure, assertThrows(IllegalArgumentException.class, combined::close));
        assertEquals(1, closes.get());
        assertThrows(IllegalStateException.class, second::toList);
        assertThrows(IllegalStateException.class, third::toList);
        combined.close();
        assertEquals(1, closes.get());
    }

    @Test
    public void testFlatMapPreservesNullEmptyUnicodeAndCloseOnceBehavior() throws Exception {
        final AtomicInteger closes = new AtomicInteger();
        final Seq<String, Exception> child = Seq.<String, Exception> of("\u65E5\u672C\uD83D\uDE00", "").onClose(closes::incrementAndGet);
        assertEquals(List.of("\u65E5\u672C\uD83D\uDE00", ""),
                Seq.<Integer, Exception> of(0, 1, 2).flatMap(value -> value == 0 ? null : value == 1 ? Seq.<String, Exception> empty() : child).toList());
        assertEquals(1, closes.get());
        child.close();
        assertEquals(1, closes.get());
        assertThrows(IllegalStateException.class, child::toList);
    }

    private static Seq<Integer, Exception> materialized() throws Exception {
        return Seq.<Integer, Exception> of(1, 2).splitAt(2).toList().getFirst();
    }
}
