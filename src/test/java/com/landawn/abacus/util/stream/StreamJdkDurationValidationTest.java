package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

class StreamJdkDurationValidationTest extends TestBase {
    @Test
    void closedEntryStreamRejectsStateBeforeDurationConversion() {
        EntryStream<String, Integer> stream = EntryStream.of(Map.of("a", 1));
        stream.close();

        assertThrows(IllegalStateException.class, () -> stream.delay(Duration.ofSeconds(Long.MAX_VALUE)));
        assertThrows(IllegalStateException.class, () -> stream.delay((Duration) null));
    }

    @Test
    void nullDurationClosesTheStreamWithoutTraversingIt() {
        AtomicInteger traversed = new AtomicInteger();
        AtomicInteger closed = new AtomicInteger();
        Stream<Integer> stream = Stream.of(1).onEach(value -> traversed.incrementAndGet()).onClose(closed::incrementAndGet);

        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class, () -> stream.delay((Duration) null));

        assertTrue(failure.getMessage().contains("duration"));
        assertEquals(0, traversed.get());
        assertEquals(1, closed.get());
    }

    @Test
    void entryStreamPreservesOverflowAndValidDelayBehavior() {
        try (EntryStream<String, Integer> stream = EntryStream.of(Map.of("a", 1))) {
            assertThrows(ArithmeticException.class, () -> stream.delay(Duration.ofSeconds(Long.MAX_VALUE)));
        }
        assertEquals(Map.of("a", 1), EntryStream.of(Map.of("a", 1)).delay(Duration.ZERO).toMap());
    }
}
