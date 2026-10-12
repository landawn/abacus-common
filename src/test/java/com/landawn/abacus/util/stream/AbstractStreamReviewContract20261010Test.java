package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Function;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.function.TriPredicate;

@Tag("unit")
class AbstractStreamReviewContract20261010Test extends TestBase {
    @Test
    void rangeMapPreservesBoundsAcrossRepeatedHasNextCalls() {
        final List<Token> produced = new ArrayList<>();

        try (Stream<List<Token>> stream = freshTokens(5, produced).rangeMap((left, right) -> left.id() / 2 == right.id() / 2, List::of)) {
            final ObjIteratorEx<List<Token>> iterator = stream.iteratorEx();

            for (int group = 0; group < 3; group++) {
                assertTrue(iterator.hasNext());
                assertTrue(iterator.hasNext());
                final List<Token> bounds = iterator.next();
                assertEquals(List.of(new Token(group * 2), new Token(Math.min(group * 2 + 1, 4))), bounds);
            }

            assertFalse(iterator.hasNext());
            assertFalse(iterator.hasNext());
            assertEquals(5, produced.size());
        }
    }

    @Test
    void collapseOverloadsPreserveGroupsAndPendingBoundaries() {
        final BiPredicate<Token, Token> adjacent = (left, right) -> left.id() / 2 == right.id() / 2;
        final TriPredicate<Token, Token, Token> inRange = (first, previous, next) -> first.id() / 2 == next.id() / 2;
        final List<?> groups = List.of(List.of(new Token(0), new Token(1)), List.of(new Token(2), new Token(3)), List.of(new Token(4)));
        final List<?> merged = List.of(new Token(1), new Token(3), new Token(4));
        final List<CollapseCase> cases = List.of(new CollapseCase(source -> source.collapse(adjacent), groups),
                new CollapseCase(source -> source.collapse(adjacent, ArrayList::new), groups),
                new CollapseCase(source -> source.collapse(adjacent, (left, right) -> right), merged),
                new CollapseCase(source -> source.collapse(adjacent, 0, (count, token) -> count + 1), List.of(2, 2, 1)),
                new CollapseCase(source -> source.collapse(adjacent, Collectors.counting()), List.of(2L, 2L, 1L)),
                new CollapseCase(source -> source.collapse(inRange), groups), new CollapseCase(source -> source.collapse(inRange, ArrayList::new), groups),
                new CollapseCase(source -> source.collapse(inRange, (left, right) -> right), merged),
                new CollapseCase(source -> source.collapse(inRange, 0, (count, token) -> count + 1), List.of(2, 2, 1)),
                new CollapseCase(source -> source.collapse(inRange, Collectors.counting()), List.of(2L, 2L, 1L)));

        for (final CollapseCase testCase : cases) {
            final List<Token> produced = new ArrayList<>();

            try (Stream<?> stream = testCase.operation().apply(freshTokens(5, produced))) {
                final ObjIteratorEx<?> iterator = stream.iteratorEx();

                for (int group = 0; group < 3; group++) {
                    assertTrue(iterator.hasNext());
                    assertTrue(iterator.hasNext());
                    assertEquals(testCase.expected().get(group), iterator.next());
                }

                assertFalse(iterator.hasNext());
                assertFalse(iterator.hasNext());
                assertEquals(5, produced.size());
            }
        }
    }

    @Test
    void rangeAndCollapsePreserveEmptyAndNullGroups() {
        final List<Integer> input = Arrays.asList(null, null, 1, 1, null);
        final List<List<Integer>> expected = Arrays.asList(Arrays.asList(null, null), List.of(1, 1), Arrays.asList((Integer) null));
        final BiPredicate<Integer, Integer> same = Objects::equals;
        final TriPredicate<Integer, Integer, Integer> sameRange = (first, previous, next) -> Objects.equals(previous, next);
        assertEquals(List.of(), Stream.<Integer> empty().rangeMap(same, Arrays::asList).toList());
        assertEquals(List.of(), Stream.<Integer> empty().collapse(same).toList());
        assertEquals(List.of(), Stream.<Integer> empty().collapse(sameRange).toList());
        assertEquals(Arrays.asList(Arrays.asList(null, null), List.of(1, 1), Arrays.asList(null, null)),
                Stream.of(input).rangeMap(same, Arrays::asList).toList());
        assertEquals(expected, Stream.of(input).collapse(same).toList());
        assertEquals(expected, Stream.of(input).collapse(sameRange).toList());
    }

    @Test
    void predicateSplitAtPreservesPrefixAndTailBoundary() {
        final List<Token> produced = new ArrayList<>();

        try (Stream<Stream<Token>> parts = freshTokens(4, produced).splitAt(token -> token.id() == 2)) {
            final ObjIteratorEx<Stream<Token>> iterator = parts.iteratorEx();
            assertEquals(List.of(new Token(0), new Token(1)), iterator.next().toList());

            try (Stream<Token> tail = iterator.next()) {
                final ObjIteratorEx<Token> tailIterator = tail.iteratorEx();
                assertTrue(tailIterator.hasNext());
                assertEquals(new Token(2), tailIterator.next());
                assertEquals(new Token(3), tailIterator.next());
                assertFalse(tailIterator.hasNext());
            }

            assertFalse(iterator.hasNext());
        }
    }

    @Test
    void predicateSplitAtReturnsAllInputsInPrefixWhenUnmatched() {
        final List<Token> produced = new ArrayList<>();

        try (Stream<Stream<Token>> parts = freshTokens(3, produced).splitAt(token -> false)) {
            final ObjIteratorEx<Stream<Token>> iterator = parts.iteratorEx();
            assertEquals(List.of(new Token(0), new Token(1), new Token(2)), iterator.next().toList());
            assertEquals(List.of(), iterator.next().toList());
        }
    }

    @Test
    void predicateSplitAtSkipsAndCountsUnreturnedHalves() {
        for (final boolean count : new boolean[] { false, true }) {
            final List<Token> produced = new ArrayList<>();

            try (Stream<Stream<Token>> parts = freshTokens(4, produced).splitAt(token -> token.id() == 2)) {
                final ObjIteratorEx<Stream<Token>> iterator = parts.iteratorEx();
                iterator.advance(1);
                assertEquals(3, produced.size());

                if (count) {
                    assertEquals(1, iterator.count());
                } else {
                    iterator.advance(Long.MAX_VALUE);
                }

                assertFalse(iterator.hasNext());
            }
        }
    }

    @Test
    void predicateSplitAtKeepsALiveTailWhenOuterIteratorIsExhausted() {
        try (Stream<Stream<Integer>> parts = Stream.of(Arrays.asList(1, 2, null, 3)).splitAt(Objects::isNull)) {
            final ObjIteratorEx<Stream<Integer>> iterator = parts.iteratorEx();
            iterator.advance(1);

            try (Stream<Integer> tail = iterator.next()) {
                assertEquals(0, iterator.count());
                iterator.advance(Long.MAX_VALUE);
                assertEquals(Arrays.asList(null, 3), tail.toList());
            }
        }
    }

    private static Stream<Token> freshTokens(final int count, final List<Token> produced) {
        return Stream.of(new Iterator<Token>() {
            private int cursor;

            @Override
            public boolean hasNext() {
                return cursor < count;
            }

            @Override
            public Token next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }

                final Token result = new Token(cursor++);
                produced.add(result);
                return result;
            }
        });
    }

    private record Token(int id) {
    }

    private record CollapseCase(Function<Stream<Token>, Stream<?>> operation, List<?> expected) {
    }
}
