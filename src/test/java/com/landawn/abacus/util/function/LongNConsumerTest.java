package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class LongNConsumerTest extends TestBase {
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
