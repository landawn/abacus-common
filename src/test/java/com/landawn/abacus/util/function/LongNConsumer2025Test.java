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
public class LongNConsumer2025Test extends TestBase {
    private void processValues(final long... values) {
        // Process values (empty for testing)
    }

    @Test
    public void testAccept_sideEffects() {
        final long[] array = new long[3];
        final LongNConsumer consumer = args -> {
            for (int i = 0; i < args.length && i < array.length; i++) {
                array[i] = args[i];
            }
        };

        consumer.accept(10L, 20L, 30L);

        assertEquals(10L, array[0]);
        assertEquals(20L, array[1]);
        assertEquals(30L, array[2]);
    }
}
