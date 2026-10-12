package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.AbstractMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("unit")
public class DeepReviewMapsFailureRegressionTest extends TestBase {
    @Test
    public void replaceKeysPreservesReusedRefillAndRollbackFailure() {
        final RuntimeException failure = new IllegalStateException("map write failed");
        final FailingMap map = new FailingMap(failure, failure);

        assertSame(failure, assertThrows(RuntimeException.class, () -> Maps.replaceKeys(map, key -> "new-" + key)));
        assertEquals(0, failure.getSuppressed().length);
    }

    @Test
    public void replaceKeysWithMergePreservesReusedRefillAndRollbackFailure() {
        final RuntimeException failure = new IllegalStateException("map write failed");
        final FailingMap map = new FailingMap(failure, failure);

        assertSame(failure, assertThrows(RuntimeException.class, () -> Maps.replaceKeys(map, key -> "new-" + key, Integer::sum)));
        assertEquals(0, failure.getSuppressed().length);
    }

    @Test
    public void replaceKeysSuppressesDistinctRollbackFailure() {
        final RuntimeException failure = new IllegalStateException("refill failed");
        final RuntimeException rollbackFailure = new IllegalArgumentException("rollback failed");
        final FailingMap map = new FailingMap(failure, rollbackFailure);

        assertSame(failure, assertThrows(RuntimeException.class, () -> Maps.replaceKeys(map, key -> "new-" + key)));
        assertEquals(1, failure.getSuppressed().length);
        assertSame(rollbackFailure, failure.getSuppressed()[0]);
    }

    private static final class FailingMap extends AbstractMap<String, Integer> {
        private final Map<String, Integer> entries = new LinkedHashMap<>();
        private final RuntimeException refillFailure;
        private final RuntimeException rollbackFailure;

        FailingMap(final RuntimeException refillFailure, final RuntimeException rollbackFailure) {
            this.refillFailure = refillFailure;
            this.rollbackFailure = rollbackFailure;
            entries.put("original", 1);
        }

        @Override
        public Set<Entry<String, Integer>> entrySet() {
            return entries.entrySet();
        }

        @Override
        public Integer put(final String key, final Integer value) {
            throw key.startsWith("new-") ? refillFailure : rollbackFailure;
        }

        @Override
        public void clear() {
            entries.clear();
        }
    }
}
