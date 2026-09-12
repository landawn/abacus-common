package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.Optional;

public class FindCauseTest extends TestBase {

    @Test
    void predicateResultsExposeTheActualThrowableWithoutSubtypeAssumptions() {
        final IllegalStateException failure = new IllegalStateException("failure");
        final Optional<Throwable> found = ExceptionUtil.findCause(failure, cause -> true);
        assertSame(failure, found.get());
        assertTrue(ExceptionUtil.findCause(failure, cause -> cause instanceof IOException).isEmpty());
    }

    @Test
    void typedMigrationFindsTheFirstExceptionMatchingBothTypeAndPredicate() {
        final IOException matching = new IOException("\u65E5\u672C timeout \uD83D\uDE00");
        final IOException earlier = new IOException("unrelated", matching);
        final RuntimeException root = new RuntimeException("timeout", earlier);
        final Optional<IOException> found = ExceptionUtil.findCause(root, cause -> cause instanceof IOException && cause.getMessage().contains("timeout"))
                .map(IOException.class::cast);
        assertSame(matching, found.get());
        assertSame(earlier, ExceptionUtil.findCause(root, IOException.class).get());
    }

    @Test
    void predicateSearchHandlesNullCyclesAndPredicateFailure() {
        assertTrue(ExceptionUtil.findCause(null, cause -> {
            throw new AssertionError();
        }).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> ExceptionUtil.findCause(null, (Predicate<Throwable>) null));
        final RuntimeException first = new RuntimeException();
        final RuntimeException second = new RuntimeException("second", first);
        first.initCause(second);
        final List<Throwable> visited = new ArrayList<>();
        assertTrue(ExceptionUtil.findCause(first, cause -> {
            visited.add(cause);
            return false;
        }).isEmpty());
        assertEquals(List.of(first, second), visited);
        assertSame(second, ExceptionUtil.findCause(first, cause -> "second".equals(cause.getMessage())).get());
        final IllegalStateException failure = new IllegalStateException("predicate failure");
        assertSame(failure, assertThrows(IllegalStateException.class, () -> ExceptionUtil.findCause(first, cause -> {
            throw failure;
        })));
    }
}
