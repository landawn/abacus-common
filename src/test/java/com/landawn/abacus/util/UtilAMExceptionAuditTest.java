package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiPredicate;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

class UtilAMExceptionAuditTest extends TestBase {
    @Test
    void closedJoinerRejectsConfigurationAndNoOpAppendsBeforeArguments() {
        final Joiner joiner = Joiner.with(",").skipNulls().append("saved");
        joiner.close();

        final List<Runnable> operations = List.of(() -> joiner.setEmptyValue(null), joiner::trimBeforeAppend, joiner::stripBeforeAppend, joiner::skipNulls,
                () -> joiner.useForNull(null), () -> joiner.append((String) null), () -> joiner.append((StringBuilder) null),
                () -> joiner.appendIf(false, null), () -> joiner.appendAll((int[]) null), () -> joiner.appendAll(new int[0], -1, 0),
                () -> joiner.appendAll(Collections.emptyList()), () -> joiner.appendAll(Collections.emptyIterator()),
                () -> joiner.appendEntries(Collections.emptyMap()));
        for (final Runnable operation : operations) {
            assertThrows(IllegalStateException.class, operation::run);
        }

        assertEquals("saved", joiner.toString());
        assertEquals(5, joiner.length());
        assertEquals("saved", joiner.map(value -> value));
    }

    @Test
    void closedJoinerDoesNotInspectAnIterableBeforeRejectingAppend() {
        final AtomicBoolean inspected = new AtomicBoolean();
        final Iterable<Object> source = () -> {
            inspected.set(true);
            return Collections.emptyIterator();
        };
        final Joiner joiner = Joiner.with(",");
        joiner.close();

        assertThrows(IllegalStateException.class, () -> joiner.appendAll(source));
        assertFalse(inspected.get());
    }

    @Test
    void joinerChecksBeanBeforeFilterAndBeforeEmptySelection() {
        final Joiner joiner = Joiner.with(",");
        assertTrue(assertThrows(IllegalArgumentException.class, () -> joiner.appendBean(new Object(), (BiPredicate<String, Object>) null)).getMessage()
                .contains("bean"));
        assertThrows(IllegalArgumentException.class, () -> joiner.appendBean(new Object(), Collections.emptyList()));
        assertSame(joiner, joiner.appendBean(null, Collections.emptyList()));
    }

    @Test
    void memoizedSuppliersValidateDurationBeforeUnitWithoutCallingSupplier() {
        final AtomicBoolean called = new AtomicBoolean();
        assertTrue(assertThrows(IllegalArgumentException.class, () -> Fn.memoizeWithExpiration(() -> {
            called.set(true);
            return "value";
        }, 0, null)).getMessage().contains("duration"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> Fnn.memoizeWithExpiration(() -> {
            called.set(true);
            return "value";
        }, 0, null)).getMessage().contains("duration"));
        assertFalse(called.get());
    }

    @Test
    void iteratorSlicesValidateOffsetBeforeCallbacksForBothSourceForms() {
        final Iterator<Object> iterator = Collections.emptyIterator();
        final List<Iterator<Object>> iterators = Collections.emptyList();
        assertTrue(assertThrows(IllegalArgumentException.class, () -> Iterators.forEach(iterator, -1, 0, null)).getMessage().contains("offset"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> Iterators.forEach(iterator, -1, 0, null, null)).getMessage().contains("offset"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> Iterators.forEach(iterators, -1, 0, null)).getMessage().contains("offset"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> Iterators.forEach(iterators, -1, 0, null, null)).getMessage().contains("offset"));
    }
}
