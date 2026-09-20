package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.landawn.abacus.TestBase;

class UtilANValidationOrderTest extends TestBase {
    @Test
    void bufferedResourcesValidateStateThenTheArrayThenItsRange() throws IOException {
        final BufferedJsonWriter writer = new BufferedJsonWriter();
        assertThrows(NullPointerException.class, () -> writer.writeCharacter((char[]) null, -1, -1));
        writer.close();
        assertThrows(IOException.class, () -> writer.writeCharacter((char[]) null));
        assertThrows(IOException.class, () -> writer.writeCharacter((char[]) null, -1, -1));

        final BufferedReader reader = new BufferedReader("data");
        assertThrows(NullPointerException.class, () -> reader.read(null, -1, -1));
        reader.close();
        assertThrows(IOException.class, () -> reader.read(null, -1, -1));
    }

    @Test
    void earlierParametersWinWhenMultipleArgumentsAreInvalid() {
        assertTrue(assertThrows(IllegalArgumentException.class, () -> Array.repeat("x", -1, null)).getMessage().contains("'n'"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> Numbers.divide((BigInteger) null, null, null)).getMessage().contains("'p'"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> NoCachingNoUpdating.DisposableArray.create(null, -1)).getMessage()
                .contains("componentType"));
        assertThrows(IndexOutOfBoundsException.class, () -> N.toArray(List.of("x"), -1, 1, (Class<String[]>) null));
        assertThrows(IndexOutOfBoundsException.class, () -> Median.of(new Integer[] { 1 }, -1, 1, (Comparator<Integer>) null));
    }

    @Test
    void groupingValidatesTheRangeAndCallbacksBeforeInvokingSuppliers() {
        final AtomicInteger invocations = new AtomicInteger();
        assertThrows(IndexOutOfBoundsException.class, () -> N.groupBy(new String[] { "x" }, -1, 1, value -> value, () -> {
            invocations.incrementAndGet();
            return new java.util.HashMap<>();
        }));
        assertThrows(IllegalArgumentException.class, () -> N.groupBy(new String[0], 0, 0, null, () -> {
            invocations.incrementAndGet();
            return new java.util.HashMap<>();
        }));
        assertEquals(0, invocations.get());
        assertThrows(IndexOutOfBoundsException.class, () -> N.skipRange(List.of("x"), -1, 0, null));
    }

    @Test
    void ioRangesAreValidatedBeforeDestinationsAndFlushes() throws IOException {
        assertThrows(IndexOutOfBoundsException.class, () -> IOUtil.write(new byte[0], 1, 0, (OutputStream) null));
        assertThrows(IndexOutOfBoundsException.class, () -> IOUtil.write(new char[0], 1, 0, (java.io.Writer) null));
        final AtomicInteger flushes = new AtomicInteger();
        final OutputStream output = new OutputStream() {
            @Override
            public void write(final int value) {
                throw new AssertionError("An empty write must not write data");
            }

            @Override
            public void flush() {
                flushes.incrementAndGet();
            }
        };
        assertThrows(IndexOutOfBoundsException.class, () -> IOUtil.write(new byte[0], 1, 0, output, true));
        assertEquals(0, flushes.get());
        IOUtil.write((byte[]) null, 0, 0, output, true);
        assertEquals(1, flushes.get());
    }

    @Test
    void invalidAppendRangesDoNotCreateFiles(@TempDir final Path directory) throws IOException {
        final Path target = directory.resolve("append.txt");
        assertThrows(IndexOutOfBoundsException.class, () -> IOUtil.append(new char[0], 1, 0, target.toFile()));
        assertFalse(Files.exists(target));
        IOUtil.append(new char[0], 0, 0, target.toFile());
        assertTrue(Files.exists(target));
        assertEquals(0, Files.size(target));
    }
}
