package com.landawn.abacus.util.stream;

import static org.assertj.core.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.ByteIterator;

/** ByteStream general contracts. */
public class ByteStreamTest extends ByteStreamTestSupport {

    // defaultIfEmpty

    @Test
    public void testDefaultIfEmpty() {
        ByteStream empty = ByteStream.empty();
        ByteStream result1 = empty.defaultIfEmpty(() -> createByteStream((byte) 1, (byte) 2));
        assertArrayEquals(new byte[] { 1, 2 }, result1.toArray());

        ByteStream nonEmpty = createByteStream((byte) 3);
        ByteStream result2 = nonEmpty.defaultIfEmpty(() -> createByteStream((byte) 1, (byte) 2));
        assertArrayEquals(new byte[] { 3 }, result2.toArray());
    }

    @Test
    public void testDefaultIfEmpty_OnEmpty() {
        ByteStream stream = ByteStream.empty();
        byte[] result = stream.defaultIfEmpty(() -> ByteStream.of((byte) 99)).toArray();
        assertArrayEquals(new byte[] { 99 }, result);
    }

    @Test
    public void testDefaultIfEmpty_OnNonEmpty() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2);
        byte[] result = stream.defaultIfEmpty(() -> ByteStream.of((byte) 99)).toArray();
        assertArrayEquals(new byte[] { 1, 2 }, result);
    }

    @Test
    public void testDefaultIfEmptyOnEmpty() {
        byte[] result = ByteStream.empty().defaultIfEmpty(() -> ByteStream.of((byte) 99)).toArray();
        assertArrayEquals(new byte[] { 99 }, result);
    }

    @Test
    public void testDefaultIfEmptyOnNonEmpty() {
        byte[] result = createByteStream((byte) 1, (byte) 2).defaultIfEmpty(() -> ByteStream.of((byte) 99)).toArray();
        assertArrayEquals(new byte[] { 1, 2 }, result);
    }

    @Test
    public void testFiniteFactoryIteratorsRemainExhaustedAfterFailedNext() {
        final ByteIteratorEx[] iterators = { ByteStream.range((byte) 1, (byte) 3).iteratorEx(), ByteStream.range((byte) 1, (byte) 5, (byte) 2).iteratorEx(),
                ByteStream.rangeClosed((byte) 1, (byte) 3).iteratorEx(), ByteStream.rangeClosed((byte) 1, (byte) 5, (byte) 2).iteratorEx(),
                ByteStream.repeat((byte) 7, 10).iteratorEx() };

        for (final ByteIteratorEx iter : iterators) {
            iter.advance(Long.MAX_VALUE);
            assertThrows(NoSuchElementException.class, iter::nextByte);
            assertEquals(0, iter.count());
            assertArrayEquals(new byte[0], iter.toArray());
        }
    }

    // ifEmpty

    @Test
    public void testIfEmpty() {
        AtomicBoolean actionExecuted = new AtomicBoolean(false);
        byteStream = createByteStream(new byte[] { 1, 2 });
        byteStream.ifEmpty(() -> actionExecuted.set(true)).toList();
        assertFalse(actionExecuted.get());

        actionExecuted.set(false);
        byteStream = createByteStream(new byte[] {});
        byteStream.ifEmpty(() -> actionExecuted.set(true)).toList();
        assertTrue(actionExecuted.get());
    }

    @Test
    public void testIfEmpty_Empty() {
        ByteStream stream = ByteStream.empty();
        AtomicBoolean called = new AtomicBoolean(false);
        ByteStream result = stream.ifEmpty(() -> called.set(true));
        result.count(); // terminal op to trigger
        assertTrue(called.get());
    }

    @Test
    public void testIfEmpty_NonEmpty() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2);
        AtomicBoolean called = new AtomicBoolean(false);
        byte[] result = stream.ifEmpty(() -> called.set(true)).toArray();
        assertFalse(called.get());
        assertArrayEquals(new byte[] { 1, 2 }, result);
    }

    @Test
    public void testIsParallel() {
        byteStream = createByteStream(new byte[] { 1, 2, 3 });
        assertFalse(byteStream.isParallel());
        assertTrue(byteStream.parallel().isParallel());
    }

    @Test
    public void testBooleanSupplierIteratorsStayExhausted() {
        final AtomicInteger conditionCalls = new AtomicInteger();
        ByteIterator iter = ByteStream.iterate(() -> conditionCalls.getAndIncrement() > 0, () -> (byte) 1).iterator();

        assertFalse(iter.hasNext());
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, iter::nextByte);
        assertEquals(1, conditionCalls.get());

        conditionCalls.set(0);
        iter = ByteStream.iterate((byte) 1, () -> conditionCalls.getAndIncrement() > 0, value -> (byte) (value + 1)).iterator();

        assertFalse(iter.hasNext());
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, iter::nextByte);
        assertEquals(1, conditionCalls.get());
    }

    @Test
    public void testIterator() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2, (byte) 3);
        ByteIterator iter = stream.iterator();
        assertTrue(iter.hasNext());
        assertEquals(1, iter.nextByte());
        assertTrue(iter.hasNext());
        assertEquals(2, iter.nextByte());
        assertTrue(iter.hasNext());
        assertEquals(3, iter.nextByte());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testPrintln() {
        assertDoesNotThrow(() -> {
            byteStream = createByteStream(new byte[] { 1, 2, 3 });
            byteStream.println();
        });
    }

    @Test
    public void testPrintln_EmptyStream() {
        assertDoesNotThrow(() -> ByteStream.empty().println());
    }

    @Test
    public void testPsp() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);

        ByteStream result = stream.parallel().psp(s -> s.filter(b -> b % 2 == 0));

        assertTrue(result.isParallel());
        assertArrayEquals(new byte[] { 2, 4 }, result.sorted().toArray());
    }

    @Test
    public void testPspFunction() {
        byteStream = createByteStream(new byte[] { 3, 1, 2 }).parallel();
        List<Byte> result = byteStream.psp(s -> s.sorted()).toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3), result);
        assertTrue(byteStream.isParallel());
    }

    @Test
    public void testStreamReuseThrowsException() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3);
        stream.toArray(); // consume
        assertThrows(IllegalStateException.class, () -> stream.toArray());
    }

    // throwIfEmpty

    @Test
    public void testThrowIfEmpty() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2, (byte) 3);
        assertArrayEquals(new byte[] { 1, 2, 3 }, stream.throwIfEmpty().toArray());

        ByteStream emptyStream = ByteStream.empty();
        assertThrows(NoSuchElementException.class, () -> emptyStream.throwIfEmpty().toArray());
    }

    @Test
    public void testThrowIfEmpty_Empty() {
        ByteStream stream = ByteStream.empty();
        assertThrows(NoSuchElementException.class, () -> stream.throwIfEmpty().toArray());
    }

    @Test
    public void testThrowIfEmpty_NonEmpty() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2);
        byte[] result = stream.throwIfEmpty().toArray();
        assertArrayEquals(new byte[] { 1, 2 }, result);
    }

    @Test
    public void testThrowIfEmptyWithCustomException() {
        byteStream = createByteStream(new byte[] {});
        RuntimeException customException = new RuntimeException("Stream is empty!");
        try {
            byteStream.throwIfEmpty(() -> customException).toList();
            fail("Should throw custom exception");
        } catch (RuntimeException e) {
            assertSame(customException, e);
        }
    }

    @Test
    public void testThrowIfEmptyWithSupplier() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2);
        assertArrayEquals(new byte[] { 1, 2 }, stream.throwIfEmpty(() -> new RuntimeException("Should not throw")).toArray());

        ByteStream empty = ByteStream.empty();
        assertThrows(IllegalStateException.class, () -> empty.throwIfEmpty(() -> new IllegalStateException("Empty stream")).toArray());
    }

}
