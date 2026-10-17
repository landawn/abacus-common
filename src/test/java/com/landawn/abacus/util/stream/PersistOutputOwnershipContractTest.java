package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class PersistOutputOwnershipContractTest extends TestBase {
    @Test
    void headerAndTailAreFlushedButCallerOutputStaysOpen() throws IOException {
        AtomicInteger sourceCloses = new AtomicInteger();
        AtomicInteger outputCloses = new AtomicInteger();
        ByteArrayOutputStream output = new ByteArrayOutputStream() {
            @Override
            public void close() {
                outputCloses.incrementAndGet();
            }
        };
        Stream<Integer> stream = Stream.of(1, 2).onClose(sourceCloses::incrementAndGet);
        assertEquals(2, stream.persist("header", "tail", value -> "row " + value, output));
        assertEquals("header\nrow 1\nrow 2\ntail\n", output.toString(StandardCharsets.UTF_8));
        assertEquals(1, sourceCloses.get());
        assertEquals(0, outputCloses.get());
        assertThrows(IllegalStateException.class, stream::count);
    }

    @Test
    void mapperFailureSurvivesOutputFlushAndSourceCloseFailures() {
        IllegalStateException mapperFailure = new IllegalStateException("mapper");
        IOException writeFailure = new IOException("output");
        IllegalArgumentException closeFailure = new IllegalArgumentException("source close");
        AtomicInteger sourceCloses = new AtomicInteger();
        AtomicInteger outputCloses = new AtomicInteger();
        OutputStream output = failingOutput(writeFailure, outputCloses);
        Stream<Integer> stream = Stream.of(1, 2).onClose(() -> {
            sourceCloses.incrementAndGet();
            throw closeFailure;
        });
        assertSame(mapperFailure, assertThrows(IllegalStateException.class, () -> stream.persist("header", "tail", value -> {
            throw mapperFailure;
        }, output)));
        assertTrue(List.of(mapperFailure.getSuppressed()).contains(writeFailure));
        assertTrue(List.of(mapperFailure.getSuppressed()).contains(closeFailure));
        assertEquals(1, sourceCloses.get());
        assertEquals(0, outputCloses.get());
        assertThrows(IllegalStateException.class, stream::count);
    }

    @Test
    void csvOutputFailureClosesTheSourceAndKeepsCallerOutputOpen() {
        IOException writeFailure = new IOException("CSV output");
        AtomicInteger sourceCloses = new AtomicInteger();
        AtomicInteger outputCloses = new AtomicInteger();
        Stream<List<Integer>> stream = Stream.of(List.of(1, 2), List.of(3, 4)).onClose(sourceCloses::incrementAndGet);
        assertSame(writeFailure, assertThrows(IOException.class, () -> stream.persistToCsv(List.of("a", "b"), failingOutput(writeFailure, outputCloses))));
        assertEquals(1, sourceCloses.get());
        assertEquals(0, outputCloses.get());
        assertThrows(IllegalStateException.class, stream::count);
    }

    private static OutputStream failingOutput(IOException failure, AtomicInteger closes) {
        return new OutputStream() {
            @Override
            public void write(int value) throws IOException {
                throw failure;
            }

            @Override
            public void close() {
                closes.incrementAndGet();
            }
        };
    }
}
