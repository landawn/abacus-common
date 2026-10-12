package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.ByteIterator;

/** ByteStream lifecycle contracts. */
public class ByteStreamLifecycleTest extends ByteStreamTestSupport {

    @Test
    public void testAppendIfEmptySupplierNullResultAndLazyClose() {
        final AtomicInteger calls = new AtomicInteger();
        final ByteStream fromNull = ByteStream.of(new byte[0]).appendIfEmpty(() -> {
            calls.incrementAndGet();
            return null;
        });

        org.junit.jupiter.api.Assertions.assertEquals(0, calls.get());
        org.junit.jupiter.api.Assertions.assertEquals(0, fromNull.toArray().length);
        org.junit.jupiter.api.Assertions.assertEquals(1, calls.get());

        final AtomicBoolean suppliedClosed = new AtomicBoolean(false);
        final ByteStream s = ByteStream.of(new byte[0]).appendIfEmpty(() -> ByteStream.of((byte) 7).onClose(() -> suppliedClosed.set(true)));
        final ByteIterator iter = s.iterator();

        org.junit.jupiter.api.Assertions.assertEquals((byte) 7, iter.nextByte());
        org.junit.jupiter.api.Assertions.assertFalse(iter.hasNext());
        org.junit.jupiter.api.Assertions.assertFalse(suppliedClosed.get());

        s.close();

        org.junit.jupiter.api.Assertions.assertTrue(suppliedClosed.get());
    }

    // close

    @Test
    public void testClose() {
        AtomicInteger closed = new AtomicInteger(0);
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3).onClose(() -> closed.incrementAndGet());

        assertEquals(0, closed.get());
        stream.close();
        assertEquals(1, closed.get());

        stream.close();
        assertEquals(1, closed.get());
    }

    @Test
    public void testCloseIdempotent() {
        AtomicBoolean closed = new AtomicBoolean(false);
        byteStream = createByteStream(new byte[] { 1, 2, 3 }).onClose(() -> closed.set(true));
        byteStream.close();
        assertTrue(closed.get());
        closed.set(false);
        byteStream.close();
        assertFalse(closed.get());
    }

    @Test
    public void testOfInputStreamWithCloseFlag() throws IOException {
        byte[] data = { 10, 20, 30 };
        InputStream is = new ByteArrayInputStream(data);
        ByteStream stream = ByteStream.of(is, true);
        assertArrayEquals(data, stream.toArray());
        stream.close();
    }

    @Test
    public void testOfInputStreamWithCloseFlagFalse() throws IOException {
        byte[] data = { 1, 2, 3 };
        InputStream is = new ByteArrayInputStream(data);
        ByteStream stream = ByteStream.of(is, false);
        assertArrayEquals(data, stream.toArray());
        stream.close();
    }

    @Test
    public void testWithCloseHandlerInConstructor() {
        AtomicInteger closeCount = new AtomicInteger(0);
        List<Runnable> closeHandlers = Arrays.asList(() -> closeCount.incrementAndGet(), () -> closeCount.addAndGet(10));

        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3).onClose(closeHandlers.get(0)).onClose(closeHandlers.get(1));

        stream.close();
        assertEquals(11, closeCount.get());
    }

    @Test
    public void testOnClose() {
        AtomicBoolean closed = new AtomicBoolean(false);
        byteStream = createByteStream(new byte[] { 1, 2, 3 }).onClose(() -> closed.set(true));
        byteStream.toList();
        assertTrue(closed.get());

        closed.set(false);
        byteStream = createByteStream(new byte[] { 1, 2, 3 }).onClose(() -> closed.set(true));
        byteStream.close();
        assertTrue(closed.get());
    }

    @Test
    public void testOnClose_MultipleHandlers() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2);
        List<String> actions = new ArrayList<>();
        stream.onClose(() -> actions.add("first")).onClose(() -> actions.add("second"));
        stream.close();
        assertEquals(2, actions.size());
        assertEquals("first", actions.get(0));
        assertEquals("second", actions.get(1));
    }

    // parallel

    @Test
    public void testParallel_WithExecutor() {
        Executor executor = Executors.newFixedThreadPool(2);
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3);
        ByteStream result = stream.parallel(executor);
        assertNotNull(result);
        assertTrue(result.isParallel());
    }

    @Test
    public void testParallel_WithMaxThreadNumAndExecutor() {
        Executor executor = Executors.newFixedThreadPool(2);
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3);
        ByteStream result = stream.parallel(2, executor);
        assertNotNull(result);
        assertTrue(result.isParallel());
    }

    @Test
    public void testParallelExecutor() {
        byteStream = createByteStream(new byte[] { 1, 2, 3 });
        ByteStream parallelStream = byteStream.parallel(Runnable::run);
        assertTrue(parallelStream.isParallel());
    }

    @Test
    public void testParallelMaxThreadNumExecutor() {
        byteStream = createByteStream(new byte[] { 1, 2, 3 });
        ByteStream parallelStream = byteStream.parallel(2, Runnable::run);
        assertTrue(parallelStream.isParallel());
    }

    @Test
    public void testParallelWithExecutor() {
        Executor executor = Executors.newFixedThreadPool(2);
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        ByteStream parallel = stream.parallel(executor);

        assertTrue(parallel.isParallel());
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, parallel.toArray());

        ((java.util.concurrent.ExecutorService) executor).shutdown();
    }

    @Test
    public void testParallelWithMaxThreadNumAndExecutor() {
        Executor executor = Executors.newFixedThreadPool(3);
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        ByteStream parallel = stream.parallel(2, executor);

        assertTrue(parallel.isParallel());
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, parallel.toArray());

        ((java.util.concurrent.ExecutorService) executor).shutdown();
    }

    // rangeClosed

    @Test
    public void testRangeClosed() {
        ByteStream stream = ByteStream.rangeClosed((byte) 1, (byte) 5);
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, stream.toArray());

        ByteStream singleStream = ByteStream.rangeClosed((byte) 5, (byte) 5);
        assertArrayEquals(new byte[] { 5 }, singleStream.toArray());
    }

    @Test
    public void testRangeClosed_Basic() {
        byte[] result = ByteStream.rangeClosed((byte) 1, (byte) 5).toArray();
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, result);
    }

    @Test
    public void testRangeClosed_SingleElement() {
        byte[] result = ByteStream.rangeClosed((byte) 3, (byte) 3).toArray();
        assertArrayEquals(new byte[] { 3 }, result);
    }

    @Test
    public void testRangeClosedBy() {
        List<Byte> result = ByteStream.rangeClosed((byte) 1, (byte) 10, (byte) 2).toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 3, (byte) 5, (byte) 7, (byte) 9), result);

        result = ByteStream.rangeClosed((byte) 1, (byte) 9, (byte) 2).toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 3, (byte) 5, (byte) 7, (byte) 9), result);

        result = ByteStream.rangeClosed((byte) 10, (byte) 1, (byte) -2).toList();
        assertEquals(Arrays.asList((byte) 10, (byte) 8, (byte) 6, (byte) 4, (byte) 2), result);
    }

    @Test
    public void testRangeClosedSingleElement() {
        assertArrayEquals(new byte[] { 5 }, ByteStream.rangeClosed((byte) 5, (byte) 5).toArray());
    }

    @Test
    public void testRangeClosedWithNegativeStep() {
        ByteStream stream = ByteStream.rangeClosed((byte) 10, (byte) 1, (byte) -2);
        assertArrayEquals(new byte[] { 10, 8, 6, 4, 2 }, stream.toArray());
    }

    @Test
    public void testRangeClosedWithStep() {
        ByteStream stream = ByteStream.rangeClosed((byte) 1, (byte) 10, (byte) 2);
        assertArrayEquals(new byte[] { 1, 3, 5, 7, 9 }, stream.toArray());

        ByteStream stream2 = ByteStream.rangeClosed((byte) 0, (byte) 10, (byte) 5);
        assertArrayEquals(new byte[] { 0, 5, 10 }, stream2.toArray());
    }

    @Test
    public void testSpsIntExecutorFunction() {
        byteStream = createByteStream(new byte[] { 1, 2, 3 });
        List<Integer> result = byteStream.sps(2, Runnable::run, s -> s.mapToInt(b -> b * 10)).toList();
        assertEquals(Arrays.asList(10, 20, 30), result);
        assertFalse(byteStream.isParallel());
    }

}
