package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.TooManyElementsException;
import com.landawn.abacus.util.stream.StreamOptionalResultContractTest.Source;
import com.landawn.abacus.util.u.Optional;

@Tag("unit")
public class EntryStreamOptionalResultContractTest extends TestBase {
    private static final Comparator<Entry<String, Integer>> NULLS_FIRST = Comparator.nullsFirst(
            Entry.comparingByKey(Comparator.nullsFirst(Comparator.naturalOrder())));
    private static final Comparator<Entry<String, Integer>> NULLS_LAST = Comparator.nullsLast(
            Entry.comparingByKey(Comparator.nullsLast(Comparator.naturalOrder())));

    enum Terminal {
        FIRST(EntryStream::first),
        LAST(EntryStream::last),
        FIND_FIRST(EntryStream::findFirst),
        FIND_ANY(EntryStream::findAny),
        ELEMENT_AT(s -> s.elementAt(0)),
        ONLY_ONE(EntryStream::onlyOne),
        FIND_FIRST_PREDICATE(s -> s.findFirst(entry -> true)),
        FIND_ANY_PREDICATE(s -> s.findAny(entry -> true)),
        FIND_LAST_PREDICATE(s -> s.findLast(entry -> true)),
        FIND_FIRST_BIPREDICATE(s -> s.findFirst((key, value) -> true)),
        FIND_ANY_BIPREDICATE(s -> s.findAny((key, value) -> true)),
        FIND_LAST_BIPREDICATE(s -> s.findLast((key, value) -> true)),
        MIN(s -> s.min(NULLS_FIRST)),
        MAX(s -> s.max(NULLS_LAST)),
        MIN_BY_KEY(s -> s.minByKey(Comparator.nullsFirst(Comparator.naturalOrder()))),
        MAX_BY_KEY(s -> s.maxByKey(Comparator.nullsLast(Comparator.naturalOrder()))),
        MIN_BY_VALUE(s -> s.minByValue(Comparator.nullsFirst(Comparator.naturalOrder()))),
        MAX_BY_VALUE(s -> s.maxByValue(Comparator.nullsLast(Comparator.naturalOrder()))),
        MIN_BY(s -> s.minBy(entry -> entry == null ? null : entry.getKey())),
        MAX_BY(s -> s.maxBy(entry -> entry == null ? null : entry.getKey())),
        REDUCE(s -> s.reduce((left, right) -> left));

        final Function<EntryStream<String, Integer>, Optional<Entry<String, Integer>>> operation;

        Terminal(final Function<EntryStream<String, Integer>, Optional<Entry<String, Integer>>> operation) {
            this.operation = operation;
        }
    }

    static java.util.stream.Stream<Arguments> sourcesAndTerminals() {
        return Arrays.stream(Source.values()).flatMap(source -> Arrays.stream(Terminal.values()).map(terminal -> Arguments.of(source, terminal)));
    }

    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource("sourcesAndTerminals")
    void nullEntriesThrowWhileEmptyAndNullFieldsRemainDistinct(final Source source, final Terminal terminal) {
        final AtomicInteger closes = new AtomicInteger();
        try (final EntryStream<String, Integer> stream = entries(source, (Entry<String, Integer>) null)) {
            stream.onClose(closes::incrementAndGet);
            assertThrows(NullPointerException.class, () -> terminal.operation.apply(stream));
            assertEquals(1, closes.get());
        }
        assertEquals(1, closes.get());
        assertFalse(terminal.operation.apply(entries(source)).isPresent());

        // Optional wraps the entry itself; null fields do not make that entry absent.
        final Entry<String, Integer> nullFields = new SimpleImmutableEntry<>(null, null);
        assertEquals(Optional.of(nullFields), terminal.operation.apply(entries(source, nullFields)));
    }

    @ParameterizedTest
    @EnumSource(Source.class)
    void biPredicatesDereferenceNullEntriesBeforeInvokingTheCallback(final Source source) {
        assertThrows(NullPointerException.class, () -> entries(source, (Entry<String, Integer>) null).findFirst((key, value) -> {
            throw new AssertionError("a null entry must fail before invoking the bi-predicate");
        }));
        assertThrows(NullPointerException.class, () -> entries(source, (Entry<String, Integer>) null).findAny((key, value) -> {
            throw new AssertionError("a null entry must fail before invoking the bi-predicate");
        }));
        assertThrows(NullPointerException.class, () -> entries(source, (Entry<String, Integer>) null).findLast((key, value) -> {
            throw new AssertionError("a null entry must fail before invoking the bi-predicate");
        }));

        // Entry predicates can reject a null entry without dereferencing it or selecting it.
        assertFalse(entries(source, (Entry<String, Integer>) null).findFirst(entry -> entry != null).isPresent());
        assertFalse(entries(source, (Entry<String, Integer>) null).findAny(entry -> entry != null).isPresent());
        assertFalse(entries(source, (Entry<String, Integer>) null).findLast(entry -> entry != null).isPresent());
    }

    @ParameterizedTest
    @EnumSource(Source.class)
    void mixedEntriesOnlyRejectNullWhenItIsSelected(final Source source) {
        final Entry<String, Integer> value = new SimpleImmutableEntry<>("key", 1);
        assertEquals(Optional.of(value), entries(source, null, value).findFirst(entry -> entry != null));
        assertEquals(Optional.of(value), entries(source, null, value).findAny(entry -> entry != null));
        assertEquals(Optional.of(value), entries(source, value, null).findLast(entry -> entry != null));
        assertEquals(Optional.of(value), entries(source, null, value).min(NULLS_LAST));
        assertEquals(Optional.of(value), entries(source, null, value).max(NULLS_FIRST));
        assertThrows(NullPointerException.class, () -> entries(source, value, null).min(NULLS_FIRST));
        assertThrows(NullPointerException.class, () -> entries(source, value, null).max(NULLS_LAST));
        assertThrows(NullPointerException.class, () -> entries(source, value, value).reduce((left, right) -> null));
        assertThrows(TooManyElementsException.class, () -> entries(source, null, value).onlyOne());
        assertEquals(Optional.of(value), entries(source, null, value).elementAt(1));
        assertFalse(entries(source, (Entry<String, Integer>) null).elementAt(1).isPresent());
    }

    @ParameterizedTest
    @EnumSource(Source.class)
    void keyExtractorIsAppliedToEveryEntryIncludingNulls(final Source source) {
        final Entry<String, Integer> value = new SimpleImmutableEntry<>("key", 1);
        // The extractor sees every entry, so it must tolerate a null entry even when that entry is not the winner.
        assertThrows(NullPointerException.class, () -> entries(source, value, null).minBy(Entry::getKey));
        assertThrows(NullPointerException.class, () -> entries(source, value, null).maxBy(Entry::getKey));

        // A null-tolerant extractor still returns the non-null winner.
        assertEquals(Optional.of(value), entries(source, value, null).minBy(entry -> entry == null ? "zz" : entry.getKey()));
        assertEquals(Optional.of(value), entries(source, value, null).maxBy(entry -> entry == null ? "aa" : entry.getKey()));
    }

    @SafeVarargs
    private static EntryStream<String, Integer> entries(final Source source, final Entry<String, Integer>... values) {
        return EntryStream.of(source.streamOf(values));
    }
}
