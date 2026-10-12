package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("unit")
class SeqBufferedClosePass2Review20261010Test extends TestBase {
    @Test
    void actualNullValuesRemainInTheBufferedOutput() {
        final AtomicInteger closes = new AtomicInteger();
        final Seq<Integer, RuntimeException> source = Seq.<Integer, RuntimeException> of(1, null, 2, null).onClose(closes::incrementAndGet);
        assertEquals(Arrays.asList(1, null, 2, null), source.buffered(2).toList());
        assertEquals(1, closes.get());
        source.close();
        assertEquals(1, closes.get());
    }

    @Test
    void queuedPrefixPrecedesTheOriginalCheckedFailureAndCloseSuppression() {
        final IOException failure = new IOException("producer");
        final IllegalStateException closeFailure = new IllegalStateException("close");
        final AtomicInteger closes = new AtomicInteger();
        final List<String> received = new ArrayList<>();
        final Seq<String, IOException> source = Seq.of(new Throwables.Iterator<String, IOException>() {
            private int count;

            @Override
            public boolean hasNext() throws IOException {
                if (count == 3) {
                    throw failure;
                }
                return true;
            }

            @Override
            public String next() {
                return count++ == 1 ? "value" : null;
            }
        }).onClose(() -> {
            closes.incrementAndGet();
            throw closeFailure;
        });

        assertSame(failure, assertThrows(IOException.class, () -> source.buffered(4).forEach(received::add)));
        assertEquals(Arrays.asList(null, "value", null), received);
        assertEquals(List.of(closeFailure), Arrays.asList(failure.getSuppressed()));
        assertEquals(1, closes.get());
        source.close();
        assertEquals(1, closes.get());
    }

}
