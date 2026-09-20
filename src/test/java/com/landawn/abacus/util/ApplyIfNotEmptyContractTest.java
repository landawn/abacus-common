package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.Optional;

@Tag("unit")
public class ApplyIfNotEmptyContractTest extends TestBase {
    @FunctionalInterface
    interface Apply {
        Optional<String> apply(Throwables.Function<Object, String, IOException> function) throws IOException;
    }

    record Fixture(Object input, Apply operation) {
    }

    enum Kind {
        LIST_MULTIMAP, SET_MULTIMAP, MULTISET, DATASET, SHEET;

        Fixture create(final boolean nonEmpty) {
            switch (this) {
                case LIST_MULTIMAP: {
                    final ListMultimap<String, Integer> map = new ListMultimap<>();
                    if (nonEmpty) {
                        map.put("key", null);
                    }
                    return new Fixture(map, map::applyIfNotEmpty);
                }
                case SET_MULTIMAP: {
                    final SetMultimap<String, Integer> map = new SetMultimap<>();
                    if (nonEmpty) {
                        map.put("key", null);
                    }
                    return new Fixture(map, map::applyIfNotEmpty);
                }
                case MULTISET: {
                    final Multiset<Integer> multiset = new Multiset<>();
                    if (nonEmpty) {
                        multiset.add(null);
                    }
                    return new Fixture(multiset, multiset::applyIfNotEmpty);
                }
                case DATASET: {
                    final Dataset dataset = new RowDataset(List.of("value"), List.of(nonEmpty ? Collections.singletonList(null) : List.of()));
                    return new Fixture(dataset, dataset::applyIfNotEmpty);
                }
                case SHEET: {
                    final Sheet<String, String, Integer> sheet = nonEmpty ? new Sheet<>(List.of("row"), List.of("column")) : new Sheet<>();
                    return new Fixture(sheet, sheet::applyIfNotEmpty);
                }
                default:
                    throw new AssertionError(this);
            }
        }
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    void nullResultThrowsAfterExactlyOneCallback(final Kind kind) {
        final Fixture fixture = kind.create(true);
        final AtomicInteger calls = new AtomicInteger();
        assertThrows(NullPointerException.class, () -> fixture.operation().apply(input -> {
            assertSame(fixture.input(), input);
            calls.incrementAndGet();
            return null;
        }));
        assertEquals(1, calls.get());
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    void emptyInputSkipsCallback(final Kind kind) throws IOException {
        assertFalse(kind.create(false).operation().apply(input -> {
            throw new AssertionError("empty input must not invoke the callback");
        }).isPresent());
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    void nonNullResultIsRetainedForNullValuedContents(final Kind kind) throws IOException {
        final Fixture fixture = kind.create(true);
        final AtomicInteger calls = new AtomicInteger();
        assertEquals(Optional.of("present"), fixture.operation().apply(input -> {
            assertSame(fixture.input(), input);
            calls.incrementAndGet();
            return "present";
        }));
        assertEquals(1, calls.get());
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    void nullCallbackIsRejectedEvenOnEmptyInput(final Kind kind) {
        assertThrows(IllegalArgumentException.class, () -> kind.create(false).operation().apply(null));
        assertThrows(IllegalArgumentException.class, () -> kind.create(true).operation().apply(null));
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    void callbackFailuresRetainTheirIdentity(final Kind kind) {
        final Fixture fixture = kind.create(true);
        final IOException checkedFailure = new IOException("callback failed");
        assertSame(checkedFailure, assertThrows(IOException.class, () -> fixture.operation().apply(input -> {
            throw checkedFailure;
        })));
        final AssertionError error = new AssertionError("callback error");
        assertSame(error, assertThrows(AssertionError.class, () -> fixture.operation().apply(input -> {
            throw error;
        })));
    }

    @Test
    void sheetSkipsCallbackWhenEitherAxisIsEmpty() {
        final Sheet<String, String, Integer> noRows = new Sheet<>(List.of(), List.of("column"));
        final Sheet<String, String, Integer> noColumns = new Sheet<>(List.of("row"), List.of());
        for (final Sheet<String, String, Integer> sheet : List.of(noRows, noColumns)) {
            assertFalse(sheet.applyIfNotEmpty(input -> {
                throw new AssertionError("an empty axis must skip the callback");
            }).isPresent());
        }
    }
}
