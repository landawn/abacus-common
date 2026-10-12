package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Spliterator;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.landawn.abacus.TestBase;

@Tag("unit")
class DatasetNameSpliteratorReview20261003Test extends TestBase {
    private static Dataset data() {
        return Dataset.rows(List.of("a", "b"), new Object[][] { { 1, 2 } });
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 1, 2 })
    void rejectsNullBeforeAdvancingAtEveryPosition(final int consumed) {
        final Spliterator<String> iterator = data().columnNames().spliterator();
        for (int i = 0; i < consumed; i++) {
            assertTrue(iterator.tryAdvance(ignored -> {
            }));
        }
        assertThrows(NullPointerException.class, () -> iterator.tryAdvance(null));
        final List<String> remaining = new ArrayList<>();
        iterator.forEachRemaining(remaining::add);
        assertEquals(List.of("a", "b").subList(consumed, 2), remaining);
    }

    @Test
    void emptyNameViewRejectsNullAction() {
        final Dataset empty = new RowDataset(List.of(), List.of());
        assertThrows(NullPointerException.class, () -> empty.columnNames().spliterator().tryAdvance(null));
    }

    @Test
    void invalidCallbackDoesNotBindTheLiveView() {
        final Dataset data = data();
        final Spliterator<String> iterator = data.columnNames().spliterator();
        assertThrows(NullPointerException.class, () -> iterator.tryAdvance(null));
        data.addColumn("c", List.of(3));
        final List<String> values = new ArrayList<>();
        iterator.forEachRemaining(values::add);
        assertEquals(List.of("a", "b", "c"), values);
    }

    @Test
    void successfulTraversalBindsAndDetectsStructuralChanges() {
        final Dataset data = data();
        final Spliterator<String> iterator = data.columnNames().spliterator();
        assertTrue(iterator.tryAdvance(value -> assertEquals("a", value)));
        data.addColumn("c", List.of(3));
        assertThrows(ConcurrentModificationException.class, () -> iterator.tryAdvance(ignored -> {
        }));
    }

    @Test
    void splitAndBulkTraversalKeepOrderAndNullValidation() {
        final Spliterator<String> iterator = data().columnNames().spliterator();
        final Spliterator<String> prefix = iterator.trySplit();
        assertNotNull(prefix);
        final List<String> values = new ArrayList<>();
        prefix.forEachRemaining(values::add);
        iterator.forEachRemaining(values::add);
        assertEquals(List.of("a", "b"), values);
        assertThrows(NullPointerException.class, () -> iterator.forEachRemaining(null));
    }
}
