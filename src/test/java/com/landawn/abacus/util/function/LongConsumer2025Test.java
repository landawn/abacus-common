package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class LongConsumer2025Test extends TestBase {
    private void addToList(final long value) {
        // Intentionally empty for testing
    }

    @Test
    public void testAccept_sideEffects() {
        final long[] array = new long[1];
        final LongConsumer consumer = val -> array[0] = val;

        consumer.accept(99L);

        assertEquals(99L, array[0]);
    }

    @Test
    public void testAccept_multipleInvocations() {
        final AtomicLong counter = new AtomicLong(0);
        final LongConsumer consumer = val -> counter.addAndGet(val);

        consumer.accept(10L);
        consumer.accept(20L);
        consumer.accept(30L);

        assertEquals(60L, counter.get());
    }

    @Test
    public void testCompatibilityWithJavaUtilFunction() {
        final List<Long> results = new ArrayList<>();
        final java.util.function.LongConsumer javaConsumer = results::add;
        final LongConsumer abacusConsumer = javaConsumer::accept;

        abacusConsumer.accept(42L);

        assertEquals(1, results.size());
        assertEquals(42L, results.get(0));
    }
}
