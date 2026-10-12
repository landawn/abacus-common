package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.UnaryOperator;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("unit")
public class SeqUtilReview20261011Test extends TestBase {
    @Test
    public void lookaheadPreservesRepeatedHasNextAndTraversal() {
        final List<UnaryOperator<Seq<Item, RuntimeException>>> operations = List.of(seq -> seq.filter(item -> true), seq -> seq.takeWhile(item -> true),
                seq -> seq.dropWhile(item -> false));

        for (final UnaryOperator<Seq<Item, RuntimeException>> operation : operations) {
            try (final Seq<Item, RuntimeException> seq = operation.apply(items(2))) {
                final Throwables.Iterator<Item, RuntimeException> iterator = seq.iteratorEx();
                for (int id = 0; id < 2; id++) {
                    assertTrue(iterator.hasNext());
                    assertTrue(iterator.hasNext());
                    assertEquals(new Item(id), iterator.next());
                }
                assertFalse(iterator.hasNext());
                assertThrows(NoSuchElementException.class, iterator::next);
            }
        }
    }

    @Test
    public void lookaheadExhaustsWhenNoValuesAreSelected() {
        final List<UnaryOperator<Seq<Item, RuntimeException>>> operations = List.of(seq -> seq.filter(item -> false), seq -> seq.takeWhile(item -> false),
                seq -> seq.dropWhile(item -> true));

        for (final UnaryOperator<Seq<Item, RuntimeException>> operation : operations) {
            try (final Seq<Item, RuntimeException> seq = operation.apply(items(2))) {
                final Throwables.Iterator<Item, RuntimeException> iterator = seq.iteratorEx();
                assertFalse(iterator.hasNext());
                assertFalse(iterator.hasNext());
                assertThrows(NoSuchElementException.class, iterator::next);
            }
        }
    }

    @Test
    public void lookaheadContinuesAfterPredicateFailure() {
        final List<UnaryOperator<Seq<Item, RuntimeException>>> operations = List.of(seq -> seq.filter(SeqUtilReview20261011Test::acceptAfterFailure),
                seq -> seq.takeWhile(SeqUtilReview20261011Test::acceptAfterFailure), seq -> seq.dropWhile(item -> !acceptAfterFailure(item)));

        for (final UnaryOperator<Seq<Item, RuntimeException>> operation : operations) {
            try (final Seq<Item, RuntimeException> seq = operation.apply(items(2))) {
                final Throwables.Iterator<Item, RuntimeException> iterator = seq.iteratorEx();
                assertThrows(IllegalStateException.class, iterator::hasNext);
                assertEquals(new Item(1), iterator.next());
                assertFalse(iterator.hasNext());
                assertThrows(NoSuchElementException.class, iterator::next);
            }
        }
    }

    @Test
    public void nullValuesRemainValidLookaheadResults() {
        final List<UnaryOperator<Seq<String, RuntimeException>>> operations = List.of(seq -> seq.filter(item -> true), seq -> seq.takeWhile(item -> true),
                seq -> seq.dropWhile(item -> false));
        for (final UnaryOperator<Seq<String, RuntimeException>> operation : operations) {
            try (final Seq<String, RuntimeException> seq = operation.apply(Seq.of(new String[] { null, "value" }))) {
                final Throwables.Iterator<String, RuntimeException> iterator = seq.iteratorEx();
                assertTrue(iterator.hasNext());
                assertNull(iterator.next());
                assertEquals("value", iterator.next());
                assertFalse(iterator.hasNext());
            }
        }
    }

    private static boolean acceptAfterFailure(final Item item) {
        if (item.id() == 0) {
            throw new IllegalStateException("predicate failed");
        }
        return true;
    }

    private static Seq<Item, RuntimeException> items(final int count) {
        return Seq.of(new Throwables.Iterator<Item, RuntimeException>() {
            private int cursor;

            @Override
            public boolean hasNext() {
                return cursor < count;
            }

            @Override
            public Item next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                return new Item(cursor++);
            }
        });
    }

    private record Item(int id) {
    }
}
