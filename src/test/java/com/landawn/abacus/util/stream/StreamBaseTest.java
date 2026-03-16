package com.landawn.abacus.util.stream;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.AsyncExecutor;
import com.landawn.abacus.util.BooleanList;
import com.landawn.abacus.util.CharList;
import com.landawn.abacus.util.ContinuableFuture;
import com.landawn.abacus.util.Fn;
import com.landawn.abacus.util.Holder;
import com.landawn.abacus.util.ImmutableList;
import com.landawn.abacus.util.ImmutableSet;
import com.landawn.abacus.util.IntList;
import com.landawn.abacus.util.ListMultimap;
import com.landawn.abacus.util.MutableBoolean;
import com.landawn.abacus.util.Multiset;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Wrapper;
import com.landawn.abacus.util.stream.BaseStream.ParallelSettings;
import com.landawn.abacus.util.stream.BaseStream.ParallelSettings.PS;
import com.landawn.abacus.util.stream.BaseStream.Splitor;
import com.landawn.abacus.util.stream.StreamBase.LocalArrayDeque;
import com.landawn.abacus.util.stream.StreamBase.LocalRunnable;
import com.landawn.abacus.util.If.OrElse;

@Tag("new-test")
public class StreamBaseTest extends TestBase {

    @SafeVarargs
    private final <T> Stream<T> createStream(T... elements) {
        return Stream.of(elements);
    }

    private <T> Stream<T> createStream(Collection<T> elements) {
        return Stream.of(elements);
    }

    @Test
    public void testSumCharArray() {
        Assertions.assertEquals(0, StreamBase.sum((char[]) null));
        Assertions.assertEquals(0, StreamBase.sum(new char[0]));

        char[] arr = { 'a', 'b', 'c' };
        Assertions.assertEquals(294, StreamBase.sum(arr));

        Assertions.assertEquals(195, StreamBase.sum(arr, 0, 2));
        Assertions.assertEquals(99, StreamBase.sum(arr, 2, 3));
        Assertions.assertEquals(0, StreamBase.sum(arr, 1, 1));
    }

    @Test
    public void testSumByteArray() {
        Assertions.assertEquals(0, StreamBase.sum((byte[]) null));
        Assertions.assertEquals(0, StreamBase.sum(new byte[0]));

        byte[] arr = { 1, 2, 3, 4, 5 };
        Assertions.assertEquals(15, StreamBase.sum(arr));

        Assertions.assertEquals(6, StreamBase.sum(arr, 0, 3));
        Assertions.assertEquals(9, StreamBase.sum(arr, 3, 5));

        byte[] negArr = { -1, -2, 3 };
        Assertions.assertEquals(0, StreamBase.sum(negArr));
    }

    @Test
    public void testSumShortArray() {
        Assertions.assertEquals(0, StreamBase.sum((short[]) null));
        Assertions.assertEquals(0, StreamBase.sum(new short[0]));

        short[] arr = { 100, 200, 300, 400, 500 };
        Assertions.assertEquals(1500, StreamBase.sum(arr));

        Assertions.assertEquals(600, StreamBase.sum(arr, 0, 3));
        Assertions.assertEquals(900, StreamBase.sum(arr, 3, 5));
    }

    @Test
    public void testSumIntArray() {
        Assertions.assertEquals(0, StreamBase.sum((int[]) null));
        Assertions.assertEquals(0, StreamBase.sum(new int[0]));

        int[] arr = { 1000, 2000, 3000, 4000, 5000 };
        Assertions.assertEquals(15000, StreamBase.sum(arr));

        Assertions.assertEquals(6000, StreamBase.sum(arr, 0, 3));
        Assertions.assertEquals(9000, StreamBase.sum(arr, 3, 5));

        int[] largeArr = { Integer.MAX_VALUE, 1 };
        Assertions.assertThrows(ArithmeticException.class, () -> StreamBase.sum(largeArr));
    }

    @Test
    public void testSumLongArray() {
        Assertions.assertEquals(0L, StreamBase.sum((long[]) null));
        Assertions.assertEquals(0L, StreamBase.sum(new long[0]));

        long[] arr = { 1000L, 2000L, 3000L, 4000L, 5000L };
        Assertions.assertEquals(15000L, StreamBase.sum(arr));

        Assertions.assertEquals(6000L, StreamBase.sum(arr, 0, 3));
        Assertions.assertEquals(9000L, StreamBase.sum(arr, 3, 5));

        long[] largeArr = { Long.MAX_VALUE / 2, Long.MAX_VALUE / 2 };
        Assertions.assertEquals(Long.MAX_VALUE - 1, StreamBase.sum(largeArr));
    }

    @Test
    public void testSumFloatArray() {
        Assertions.assertEquals(0.0, StreamBase.sum((float[]) null));
        Assertions.assertEquals(0.0, StreamBase.sum(new float[0]));

        float[] arr = { 1.5f, 2.5f, 3.5f };
        Assertions.assertEquals(7.5, StreamBase.sum(arr), 0.001);

        Assertions.assertEquals(4.0, StreamBase.sum(arr, 0, 2), 0.001);
        Assertions.assertEquals(3.5, StreamBase.sum(arr, 2, 3), 0.001);
    }

    @Test
    public void testSumDoubleArray() {
        Assertions.assertEquals(0.0, StreamBase.sum((double[]) null));
        Assertions.assertEquals(0.0, StreamBase.sum(new double[0]));

        double[] arr = { 1.5, 2.5, 3.5, 4.5 };
        Assertions.assertEquals(12.0, StreamBase.sum(arr), 0.001);

        Assertions.assertEquals(4.0, StreamBase.sum(arr, 0, 2), 0.001);
        Assertions.assertEquals(8.0, StreamBase.sum(arr, 2, 4), 0.001);
    }

    @Test
    public void testIterateMethods() {
        CharStream charStream = CharStream.of('a', 'b', 'c');
        CharIteratorEx charIter = StreamBase.iterate(charStream);
        Assertions.assertTrue(charIter.hasNext());
        Assertions.assertEquals('a', charIter.nextChar());

        CharIteratorEx emptyCharIter = StreamBase.iterate((CharStream) null);
        Assertions.assertFalse(emptyCharIter.hasNext());

        ByteStream byteStream = ByteStream.of((byte) 1, (byte) 2, (byte) 3);
        ByteIteratorEx byteIter = StreamBase.iterate(byteStream);
        Assertions.assertTrue(byteIter.hasNext());
        Assertions.assertEquals((byte) 1, byteIter.nextByte());

        IntStream intStream = IntStream.of(1, 2, 3);
        IntIteratorEx intIter = StreamBase.iterate(intStream);
        Assertions.assertTrue(intIter.hasNext());
        Assertions.assertEquals(1, intIter.nextInt());

        Stream<String> objStream = Stream.of("a", "b", "c");
        ObjIteratorEx<String> objIter = StreamBase.iterate(objStream);
        Assertions.assertTrue(objIter.hasNext());
        Assertions.assertEquals("a", objIter.next());
    }

    @Test
    public void testIterateAll() {
        List<Stream<Integer>> streams = Arrays.asList(Stream.of(1, 2, 3), Stream.of(4, 5, 6), Stream.of(7, 8, 9));

        List<ObjIteratorEx<Integer>> iterators = StreamBase.iterateAll(streams);
        Assertions.assertEquals(3, iterators.size());

        for (int i = 0; i < 3; i++) {
            Assertions.assertTrue(iterators.get(i).hasNext());
            Assertions.assertEquals(i * 3 + 1, iterators.get(i).next());
        }

        List<ObjIteratorEx<Integer>> emptyIterators = StreamBase.iterateAll(null);
        Assertions.assertEquals(0, emptyIterators.size());

        List<ObjIteratorEx<Integer>> emptyIterators2 = StreamBase.iterateAll(new ArrayList<>());
        Assertions.assertEquals(0, emptyIterators2.size());

        List<Stream<Integer>> streamsWithNull = Arrays.asList(Stream.of(1, 2, 3), null, Stream.of(7, 8, 9));

        List<ObjIteratorEx<Integer>> iteratorsWithNull = StreamBase.iterateAll(streamsWithNull);
        Assertions.assertEquals(3, iteratorsWithNull.size());
        Assertions.assertFalse(iteratorsWithNull.get(1).hasNext());
    }

    @Test
    public void testPrimitiveIteratorConversions() {
        ObjIteratorEx<Character> charObjIter = ObjIteratorEx.of('a', 'b', 'c');
        CharIteratorEx charIter = StreamBase.charIterator(charObjIter);
        Assertions.assertEquals('a', charIter.nextChar());

        ObjIteratorEx<Byte> byteObjIter = ObjIteratorEx.of((byte) 1, (byte) 2, (byte) 3);
        ByteIteratorEx byteIter = StreamBase.byteIterator(byteObjIter);
        Assertions.assertEquals((byte) 1, byteIter.nextByte());

        ObjIteratorEx<Integer> intObjIter = ObjIteratorEx.of(1, 2, 3);
        IntIteratorEx intIter = StreamBase.intIterator(intObjIter);
        Assertions.assertEquals(1, intIter.nextInt());

        ObjIteratorEx<Long> longObjIter = ObjIteratorEx.of(1L, 2L, 3L);
        LongIteratorEx longIter = StreamBase.longIterator(longObjIter);
        Assertions.assertEquals(1L, longIter.nextLong());

        ObjIteratorEx<Float> floatObjIter = ObjIteratorEx.of(1.0f, 2.0f, 3.0f);
        FloatIteratorEx floatIter = StreamBase.floatIterator(floatObjIter);
        Assertions.assertEquals(1.0f, floatIter.nextFloat(), 0.001f);

        ObjIteratorEx<Double> doubleObjIter = ObjIteratorEx.of(1.0, 2.0, 3.0);
        DoubleIteratorEx doubleIter = StreamBase.doubleIterator(doubleObjIter);
        Assertions.assertEquals(1.0, doubleIter.nextDouble(), 0.001);
    }

    @Test
    public void testHashKey() {
        Assertions.assertEquals(StreamBase.NONE, StreamBase.hashKey(null));

        String str = "test";
        Assertions.assertEquals(str, StreamBase.hashKey(str));

        Integer num = 42;
        Assertions.assertEquals(num, StreamBase.hashKey(num));

        int[] arr = { 1, 2, 3 };
        Object hashKey = StreamBase.hashKey(arr);
        Assertions.assertTrue(hashKey instanceof Wrapper);

        Object hashKey2 = StreamBase.hashKey(arr);
        Assertions.assertEquals(hashKey, hashKey2);

        int[] arr2 = { 1, 2, 3 };
        Object hashKey3 = StreamBase.hashKey(arr2);
        Assertions.assertEquals(hashKey, hashKey3);
    }

    @Test
    public void testToInt() {
        Assertions.assertEquals(100, StreamBase.toInt(100L));
        Assertions.assertEquals(Integer.MAX_VALUE, StreamBase.toInt(Integer.MAX_VALUE));
        Assertions.assertEquals(0, StreamBase.toInt(0L));
        Assertions.assertEquals(-100, StreamBase.toInt(-100L));

        Assertions.assertEquals(Integer.MAX_VALUE, StreamBase.toInt(Long.MAX_VALUE));
        Assertions.assertEquals(Integer.MAX_VALUE, StreamBase.toInt((long) Integer.MAX_VALUE + 1));
    }

    @Test
    public void testCalculateBufferedSize() {
        Assertions.assertEquals(64, StreamBase.calculateBufferedSize(1, 1));
        Assertions.assertEquals(640, StreamBase.calculateBufferedSize(10, 1));
        Assertions.assertEquals(6400, StreamBase.calculateBufferedSize(100, 1));

        Assertions.assertEquals(StreamBase.MAX_BUFFERED_SIZE, StreamBase.calculateBufferedSize(1000000, 1));

        Assertions.assertEquals(256, StreamBase.calculateBufferedSize(1, 16));
        Assertions.assertEquals(1024, StreamBase.calculateBufferedSize(10, 64));
    }

    @Test
    public void testIsSameComparator() {
        Comparator<Integer> comp1 = Integer::compare;
        Assertions.assertTrue(StreamBase.isSameComparator(comp1, comp1));

        Assertions.assertTrue(StreamBase.isSameComparator(null, null));

        Assertions.assertTrue(StreamBase.isSameComparator(null, StreamBase.INT_COMPARATOR));
        Assertions.assertTrue(StreamBase.isSameComparator(StreamBase.INT_COMPARATOR, null));

        Assertions.assertTrue(StreamBase.isSameComparator(StreamBase.NATURAL_COMPARATOR, StreamBase.INT_COMPARATOR));
        Assertions.assertTrue(StreamBase.isSameComparator(StreamBase.INT_COMPARATOR, StreamBase.NATURAL_COMPARATOR));

        Assertions.assertFalse(StreamBase.isSameComparator(Comparator.nullsFirst(Integer::compare), Comparator.nullsFirst(String::compareTo)));
    }

    @Test
    public void testIsEmptyCloseHandler() {
        Assertions.assertTrue(StreamBase.isEmptyCloseHandler(null));

        Assertions.assertTrue(StreamBase.isEmptyCloseHandler(StreamBase.EMPTY_CLOSE_HANDLER));

        Runnable handler = () -> System.out.println("close");
        Assertions.assertFalse(StreamBase.isEmptyCloseHandler(handler));
    }

    @Test
    public void testIsEmptyCloseHandlers() {
        Assertions.assertTrue(StreamBase.isEmptyCloseHandlers(null));

        Assertions.assertTrue(StreamBase.isEmptyCloseHandlers(new ArrayList<>()));

        List<Runnable> handlers = new ArrayList<>();
        handlers.add(StreamBase.EMPTY_CLOSE_HANDLER);
        Assertions.assertTrue(StreamBase.isEmptyCloseHandlers(handlers));

        handlers.add(() -> System.out.println("close"));
        Assertions.assertFalse(StreamBase.isEmptyCloseHandlers(handlers));
    }

    @Test
    public void testNewCloseHandler() {
        LocalRunnable handler1 = StreamBase.newCloseHandler((Runnable) null);
        Assertions.assertEquals(StreamBase.EMPTY_CLOSE_HANDLER, handler1);

        LocalRunnable localRunnable = LocalRunnable.wrap(Fn.jr(() -> {
        }));
        LocalRunnable handler2 = StreamBase.newCloseHandler(localRunnable);
        Assertions.assertSame(localRunnable, handler2);

        AtomicBoolean called = new AtomicBoolean(false);
        Runnable runnable = () -> called.set(true);
        LocalRunnable handler3 = StreamBase.newCloseHandler(runnable);
        handler3.run();
        Assertions.assertTrue(called.get());

        AtomicBoolean closed = new AtomicBoolean(false);
        AutoCloseable closeable = () -> closed.set(true);
        LocalRunnable handler4 = StreamBase.newCloseHandler(closeable);
        handler4.run();
        Assertions.assertTrue(closed.get());
    }

    @Test
    public void testMergeCloseHandlers() {
        Deque<LocalRunnable> handlers = new ArrayDeque<>();
        handlers.add(LocalRunnable.wrap(Fn.jr(() -> {
        })));

        Deque<LocalRunnable> merged1 = StreamBase.mergeCloseHandlers(StreamBase.EMPTY_CLOSE_HANDLER, handlers);
        Assertions.assertSame(handlers, merged1);

        AtomicBoolean called = new AtomicBoolean(false);
        Runnable newHandler = () -> called.set(true);
        Deque<LocalRunnable> merged2 = StreamBase.mergeCloseHandlers(newHandler, null);
        Assertions.assertEquals(1, merged2.size());

        Deque<LocalRunnable> handlersA = new StreamBase.LocalArrayDeque<>();
        handlersA.add(LocalRunnable.wrap(Fn.jr(() -> {
        })));

        Deque<LocalRunnable> handlersB = new StreamBase.LocalArrayDeque<>();
        handlersB.add(LocalRunnable.wrap(Fn.jr(() -> {
        })));

        Deque<LocalRunnable> merged3 = StreamBase.mergeCloseHandlers(handlersA, handlersB);
        Assertions.assertEquals(2, merged3.size());
    }

    @Test
    public void testSetError() {
        Holder<Throwable> errorHolder = Holder.of(null);
        Exception error1 = new Exception("Error 1");
        StreamBase.setError(errorHolder, error1);
        Assertions.assertSame(error1, errorHolder.value());

        Exception error2 = new Exception("Error 2");
        StreamBase.setError(errorHolder, error2);
        Assertions.assertSame(error1, errorHolder.value());
        Assertions.assertEquals(1, errorHolder.value().getSuppressed().length);
        Assertions.assertSame(error2, errorHolder.value().getSuppressed()[0]);
    }

    @Test
    public void testCompleteAndShutdownTempExecutor() {
        List<ContinuableFuture<Void>> futures = new ArrayList<>();
        Holder<Throwable> errorHolder = Holder.of(null);
        AsyncExecutor asyncExecutor = new AsyncExecutor(Executors.newFixedThreadPool(2));

        futures.add(asyncExecutor.execute(() -> {
        }));
        futures.add(asyncExecutor.execute(() -> {
        }));

        Assertions.assertDoesNotThrow(() -> StreamBase.complete(futures, errorHolder));

        errorHolder.setValue(new RuntimeException("Test error"));
        Assertions.assertThrows(RuntimeException.class, () -> StreamBase.complete(futures, errorHolder));
    }

    @Test
    public void testCanBeSequential() {
        Assertions.assertTrue(StreamBase.canBeSequential(0));
        Assertions.assertTrue(StreamBase.canBeSequential(1));
        Assertions.assertFalse(StreamBase.canBeSequential(2));
        Assertions.assertFalse(StreamBase.canBeSequential(10));

        Assertions.assertTrue(StreamBase.canBeSequential(2, 0, 1));
        Assertions.assertFalse(StreamBase.canBeSequential(2, 0, 2));
        Assertions.assertTrue(StreamBase.canBeSequential(1, 0, 10));
    }

    @Test
    public void testSubList() {
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);

        List<Integer> sub1 = StreamBase.subList(list, 1, 3);
        Assertions.assertEquals(Arrays.asList(2, 3), sub1);

        List<Integer> sub2 = StreamBase.subList(list, 2, 10);
        Assertions.assertEquals(Arrays.asList(3, 4, 5), sub2);

        List<Integer> sub3 = StreamBase.subList(list, 3, 3);
        Assertions.assertTrue(sub3.isEmpty());
    }

    @Test
    public void testSlice() {
        Integer[] arr = { 1, 2, 3, 4, 5 };
        List<Integer> slice1 = StreamBase.slice(arr, 1, 4);
        Assertions.assertEquals(Arrays.asList(2, 3, 4), slice1);

        Integer[] emptyArr = {};
        List<Integer> slice2 = StreamBase.slice(emptyArr, 0, 0);
        Assertions.assertTrue(slice2.isEmpty());

        List<Integer> slice3 = StreamBase.slice(null, 0, 0);
        Assertions.assertTrue(slice3.isEmpty());
    }

    @Test
    public void testCheckAsyncExecutor() {
        AsyncExecutor executor1 = StreamBase.checkAsyncExecutor(null, 2);
        Assertions.assertNotNull(executor1);

        AsyncExecutor executor2 = StreamBase.checkAsyncExecutor(StreamBase.DEFAULT_ASYNC_EXECUTOR, 2);
        Assertions.assertNotNull(executor2);

        AsyncExecutor customExecutor = new AsyncExecutor(Executors.newFixedThreadPool(2));
        AsyncExecutor executor3 = StreamBase.checkAsyncExecutor(customExecutor, 2);
        Assertions.assertSame(customExecutor, executor3);
        customExecutor.shutdown();
    }

    @Test
    public void testLocalRunnable() {
        LocalRunnable wrapped1 = LocalRunnable.wrap((Runnable) null);
        Assertions.assertSame(StreamBase.EMPTY_CLOSE_HANDLER, wrapped1);

        LocalRunnable localRunnable = LocalRunnable.wrap(Fn.jr(() -> {
        }));
        LocalRunnable wrapped2 = LocalRunnable.wrap(localRunnable);
        Assertions.assertSame(localRunnable, wrapped2);

        AtomicBoolean called = new AtomicBoolean(false);
        Runnable runnable = () -> called.set(true);
        LocalRunnable wrapped3 = LocalRunnable.wrap(runnable);
        wrapped3.run();
        Assertions.assertTrue(called.get());

        called.set(false);
        wrapped3.run();
        Assertions.assertFalse(called.get());

        AtomicBoolean closed = new AtomicBoolean(false);
        AutoCloseable closeable = () -> closed.set(true);
        LocalRunnable wrapped4 = LocalRunnable.wrap(closeable);
        wrapped4.run();
        Assertions.assertTrue(closed.get());
    }

    @Test
    public void testLocalArrayDeque() {
        LocalArrayDeque<String> deque1 = new LocalArrayDeque<>();
        Assertions.assertTrue(deque1.isEmpty());

        LocalArrayDeque<String> deque2 = new LocalArrayDeque<>(10);
        Assertions.assertTrue(deque2.isEmpty());

        List<String> list = Arrays.asList("a", "b", "c");
        LocalArrayDeque<String> deque3 = new LocalArrayDeque<>(list);
        Assertions.assertEquals(3, deque3.size());
        Assertions.assertEquals("a", deque3.getFirst());
        Assertions.assertEquals("c", deque3.getLast());
    }

    @Test
    public void testToRuntimeException() {
        RuntimeException re = new RuntimeException("test");
        RuntimeException result1 = StreamBase.toRuntimeException(re);
        Assertions.assertSame(re, result1);

        Exception e = new Exception("test");
        RuntimeException result2 = StreamBase.toRuntimeException(e);
        Assertions.assertTrue(result2 instanceof RuntimeException);
        Assertions.assertEquals(e, result2.getCause());

        Error error = new Error("test");
        Assertions.assertThrows(Error.class, () -> StreamBase.toRuntimeException(error, true));

        RuntimeException result3 = StreamBase.toRuntimeException(error, false);
        Assertions.assertTrue(result3 instanceof RuntimeException);
    }

    @Test
    public void testCloseIterators() {
        List<IteratorEx<?>> iterators = new ArrayList<>();
        AtomicInteger closeCount = new AtomicInteger(0);

        for (int i = 0; i < 3; i++) {
            iterators.add(new IteratorEx<Integer>() {
                @Override
                public boolean hasNext() {
                    return false;
                }

                @Override
                public Integer next() {
                    throw new NoSuchElementException();
                }

                @Override
                public void close() {
                    closeCount.incrementAndGet();
                }
            });
        }

        StreamBase.closeIterators(iterators);
        Assertions.assertEquals(3, closeCount.get());

        List<IteratorEx<?>> iteratorsWithError = new ArrayList<>();
        iteratorsWithError.add(new IteratorEx<Integer>() {
            @Override
            public boolean hasNext() {
                return false;
            }

            @Override
            public Integer next() {
                throw new NoSuchElementException();
            }

            @Override
            public void close() {
                throw new RuntimeException("Close error");
            }
        });

        Assertions.assertThrows(RuntimeException.class, () -> StreamBase.closeIterators(iteratorsWithError));
    }

    @Test
    public void testExecuteMethods() {
        AsyncExecutor executor = new AsyncExecutor(Executors.newFixedThreadPool(2));
        List<ContinuableFuture<Void>> futures = new ArrayList<>();

        try {
            AtomicBoolean ran1 = new AtomicBoolean(false);
            AsyncExecutor result1 = StreamBase.execute(executor, 2, 0, () -> ran1.set(true));
            Assertions.assertNotNull(result1);
            Thread.sleep(100);
            Assertions.assertTrue(ran1.get());

            AsyncExecutor result2 = StreamBase.execute(executor, 2, 0, () -> "test");
            Assertions.assertNotNull(result2);

            AtomicBoolean ran3 = new AtomicBoolean(false);
            StreamBase.execute(executor, 2, 0, futures, () -> ran3.set(true));
            Assertions.assertEquals(1, futures.size());
            futures.get(0).get();
            Assertions.assertTrue(ran3.get());

        } catch (Exception e) {
            Assertions.fail("Unexpected exception: " + e);
        } finally {
            executor.shutdown();
        }
    }

    @Test
    public void testCheckMaxThreadNum() {
        int result1 = StreamBase.checkMaxThreadNum(0, null);
        Assertions.assertEquals(StreamBase.DEFAULT_MAX_THREAD_NUM, result1);

        int result2 = StreamBase.checkMaxThreadNum(4, null);
        Assertions.assertEquals(4, result2);

        int result3 = StreamBase.checkMaxThreadNum(1000, StreamBase.DEFAULT_ASYNC_EXECUTOR);
        Assertions.assertEquals(StreamBase.MAX_THREAD_NUM_PER_OPERATION, result3);

        AsyncExecutor customExecutor = new AsyncExecutor(Executors.newFixedThreadPool(2));
        int result4 = StreamBase.checkMaxThreadNum(1000, customExecutor);
        Assertions.assertEquals(1000, result4);
        customExecutor.shutdown();
    }

    @Test
    public void testCompleteToClose() {
        MutableBoolean onGoing = MutableBoolean.of(true);
        Holder<AsyncExecutor> executorHolder = Holder.of(null);

        Deque<LocalRunnable> closeHandlers = StreamBase.completeToClose(onGoing, executorHolder);
        Assertions.assertEquals(1, closeHandlers.size());

        closeHandlers.getFirst().run();
        Assertions.assertFalse(onGoing.value());

        AsyncExecutor executor = new AsyncExecutor(Executors.newFixedThreadPool(2));
        executorHolder.setValue(executor);
        onGoing.setTrue();

        Deque<LocalRunnable> closeHandlers2 = StreamBase.completeToClose(onGoing, executorHolder);
        closeHandlers2.getFirst().run();
        Assertions.assertFalse(onGoing.value());
    }

    @Test
    public void testSetStopFlagAndThrowException() {
        MutableBoolean onGoing = MutableBoolean.of(true);
        Holder<Throwable> errorHolder = Holder.of(null);

        Assertions.assertDoesNotThrow(() -> StreamBase.setStopFlagAndThrowException(errorHolder, onGoing));
        Assertions.assertFalse(onGoing.value());

        onGoing.setTrue();
        errorHolder.setValue(new RuntimeException("Test error"));
        Assertions.assertThrows(RuntimeException.class, () -> StreamBase.setStopFlagAndThrowException(errorHolder, onGoing));
        Assertions.assertFalse(onGoing.value());
        Assertions.assertNull(errorHolder.value());
    }

    @Test
    public void testThrowException() {
        Holder<Throwable> errorHolder = Holder.of(new RuntimeException("Test"));
        Assertions.assertThrows(RuntimeException.class, () -> StreamBase.throwException(errorHolder, null));

        errorHolder.setValue(new Exception("Test"));
        Assertions.assertThrows(Exception.class, () -> StreamBase.throwException(errorHolder, null));

        errorHolder.setValue(new Error("Test"));
        Assertions.assertThrows(Error.class, () -> StreamBase.throwException(errorHolder, null));
    }

    @Test
    public void testStaticFields() {
        Assertions.assertNotNull(StreamBase.NONE);
        Assertions.assertNotNull(StreamBase.RAND);
        Assertions.assertNotNull(StreamBase.NULL_CHAR_ARRAY);
        Assertions.assertArrayEquals("null".toCharArray(), StreamBase.NULL_CHAR_ARRAY);
        Assertions.assertNotNull(StreamBase.ELEMENT_SEPARATOR_CHAR_ARRAY);

        Assertions.assertTrue(StreamBase.MAX_WAIT_TIME_FOR_QUEUE_OFFER > 0);
        Assertions.assertTrue(StreamBase.MAX_WAIT_TIME_FOR_QUEUE_POLL > 0);
        Assertions.assertTrue(StreamBase.MAX_BUFFERED_SIZE > 0);
        Assertions.assertTrue(StreamBase.DEFAULT_BUFFERED_SIZE_PER_ITERATOR > 0);
        Assertions.assertTrue(StreamBase.BATCH_SIZE_FOR_FLUSH > 0);

        Assertions.assertNotNull(StreamBase.NULL_MIN_COMPARATOR);
        Assertions.assertNotNull(StreamBase.NULL_MAX_COMPARATOR);
        Assertions.assertNotNull(StreamBase.NATURAL_COMPARATOR);
        Assertions.assertNotNull(StreamBase.REVERSED_COMPARATOR);

        Assertions.assertEquals(-1, StreamBase.CHAR_COMPARATOR.compare('a', 'b'));
        Assertions.assertEquals(-1, StreamBase.BYTE_COMPARATOR.compare((byte) 1, (byte) 2));
        Assertions.assertEquals(-1, StreamBase.INT_COMPARATOR.compare(1, 2));
        Assertions.assertEquals(-1, StreamBase.LONG_COMPARATOR.compare(1L, 2L));
        Assertions.assertEquals(-1, StreamBase.FLOAT_COMPARATOR.compare(1.0f, 2.0f));
        Assertions.assertEquals(-1, StreamBase.DOUBLE_COMPARATOR.compare(1.0, 2.0));
    }

    @Test
    public void testCollectingCombiner() {
        BiConsumer<Object, Object> combiner = StreamBase.collectingCombiner;

        List<Integer> list1 = new ArrayList<>(Arrays.asList(1, 2, 3));
        List<Integer> list2 = new ArrayList<>(Arrays.asList(4, 5, 6));
        combiner.accept(list1, list2);
        Assertions.assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), list1);

        Map<String, Integer> map1 = new HashMap<>();
        map1.put("a", 1);
        Map<String, Integer> map2 = new HashMap<>();
        map2.put("b", 2);
        combiner.accept(map1, map2);
        Assertions.assertEquals(2, map1.size());
        Assertions.assertEquals(1, map1.get("a"));
        Assertions.assertEquals(2, map1.get("b"));

        StringBuilder sb1 = new StringBuilder("Hello");
        StringBuilder sb2 = new StringBuilder(" World");
        combiner.accept(sb1, sb2);
        Assertions.assertEquals("Hello World", sb1.toString());

        BooleanList boolList1 = BooleanList.of(true, false);
        BooleanList boolList2 = BooleanList.of(false, true);
        combiner.accept(boolList1, boolList2);
        Assertions.assertEquals(4, boolList1.size());

        IntList intList1 = IntList.of(1, 2);
        IntList intList2 = IntList.of(3, 4);
        combiner.accept(intList1, intList2);
        Assertions.assertEquals(4, intList1.size());
    }

    @Test
    public void testCollectingCombiner_MultisetAndMultimap() {
        BiConsumer<Object, Object> combiner = StreamBase.collectingCombiner;

        Multiset<String> multiset1 = Multiset.of("a", "a");
        Multiset<String> multiset2 = Multiset.of("a", "b");
        combiner.accept(multiset1, multiset2);
        Assertions.assertEquals(3, multiset1.count("a"));
        Assertions.assertEquals(1, multiset1.count("b"));

        ListMultimap<String, Integer> multimap1 = N.newListMultimap();
        multimap1.put("x", 1);
        ListMultimap<String, Integer> multimap2 = N.newListMultimap();
        multimap2.put("x", 2);
        multimap2.put("y", 3);
        combiner.accept(multimap1, multimap2);
        Assertions.assertEquals(Arrays.asList(1, 2), multimap1.get("x"));
        Assertions.assertEquals(Arrays.asList(3), multimap1.get("y"));

        CharList charList1 = CharList.of('a');
        CharList charList2 = CharList.of('b', 'c');
        combiner.accept(charList1, charList2);
        Assertions.assertEquals(3, charList1.size());
    }

    @Test
    public void testCompleteAndCollectResult_MergesResultsAndClosesStream() {
        AsyncExecutor executor = new AsyncExecutor(Executors.newFixedThreadPool(2));
        List<ContinuableFuture<List<Integer>>> futures = new ArrayList<>();
        futures.add(executor.execute(() -> new ArrayList<>(Arrays.asList(1, 2))));
        futures.add(executor.execute(() -> new ArrayList<>(Arrays.asList(3, 4))));

        Stream<Integer> stream = createStream(9, 8, 7);

        try {
            List<Integer> result = StreamBase.completeAndCollectResult(futures, Holder.of(null), ArrayList::new, List::addAll, stream, executor, executor);
            Assertions.assertEquals(Arrays.asList(1, 2, 3, 4), result);
            Assertions.assertThrows(IllegalStateException.class, stream::count);
        } finally {
            executor.shutdown();
        }
    }

    @Test
    public void testNewStreamCharRange_SequentialAndParallel() {
        Stream<Integer> sequentialBase = createStream(1, 2, 3);
        CharStream sequential = sequentialBase.newStream(new char[] { 'a', 'b', 'c' }, 1, 3, true);
        Assertions.assertFalse(sequential.isParallel());
        Assertions.assertArrayEquals(new char[] { 'b', 'c' }, sequential.toArray());

        Stream<Integer> parallelBase = createStream(1, 2, 3).parallel(PS.create(Splitor.ARRAY).maxThreadNum(2));
        CharStream parallel = parallelBase.newStream(new char[] { 'x', 'y', 'z' }, 0, 2, true);
        Assertions.assertTrue(parallel.isParallel());
        Assertions.assertArrayEquals(new char[] { 'x', 'y' }, parallel.toArray());
    }

    @Test
    public void testToArray_HelperCopiesOnlyElements() {
        ArrayList<String> list = new ArrayList<>(8);
        list.add("alpha");
        list.add("beta");

        Object[] array = StreamBase.toArray(list);
        Assertions.assertArrayEquals(new Object[] { "alpha", "beta" }, array);
        Assertions.assertEquals(2, array.length);
    }

    @Test
    public void testShuffled() {
        Stream<Integer> stream = createStream(1, 2, 3, 4, 5);
        List<Integer> shuffled = stream.shuffled().toList();

        Assertions.assertEquals(5, shuffled.size());
        Assertions.assertTrue(shuffled.containsAll(Arrays.asList(1, 2, 3, 4, 5)));
    }

    @Test
    public void testElementAt() {
        Stream<String> stream1 = createStream("a", "b", "c", "d", "e");
        Assertions.assertEquals("a", stream1.elementAt(0).get());

        Stream<String> stream2 = createStream("a", "b", "c", "d", "e");
        Assertions.assertEquals("c", stream2.elementAt(2).get());

        Stream<String> stream3 = createStream("a", "b", "c", "d", "e");
        Assertions.assertEquals("e", stream3.elementAt(4).get());

        Stream<String> stream4 = createStream("a", "b", "c");
        Assertions.assertThrows(IllegalArgumentException.class, () -> stream4.elementAt(-1));

        Stream<String> stream5 = createStream("a", "b", "c");
        Assertions.assertThrows(NoSuchElementException.class, () -> stream5.elementAt(5).get());

        Stream<String> stream6 = createStream();
        Assertions.assertThrows(NoSuchElementException.class, () -> stream6.elementAt(0).get());
    }

    @Test
    public void testToImmutableList() {
        Stream<Integer> stream = createStream(1, 2, 3, 4, 5);
        ImmutableList<Integer> immutableList = stream.toImmutableList();

        Assertions.assertEquals(5, immutableList.size());
        Assertions.assertEquals(Arrays.asList(1, 2, 3, 4, 5), new ArrayList<>(immutableList));

        Stream<Integer> emptyStream = createStream();
        ImmutableList<Integer> emptyList = emptyStream.toImmutableList();
        Assertions.assertTrue(emptyList.isEmpty());
    }

    @Test
    public void testToImmutableSet() {
        Stream<Integer> stream = createStream(1, 2, 3, 3, 4, 5);
        ImmutableSet<Integer> immutableSet = stream.toImmutableSet();

        Assertions.assertEquals(5, immutableSet.size());
        Assertions.assertTrue(immutableSet.containsAll(Arrays.asList(1, 2, 3, 4, 5)));

        Stream<Integer> emptyStream = createStream();
        ImmutableSet<Integer> emptySet = emptyStream.toImmutableSet();
        Assertions.assertTrue(emptySet.isEmpty());
    }

    @Test
    public void testToArray() {
        Stream<String> stream = createStream("a", "b", "c");
        Object[] array = stream.toArray();

        Assertions.assertArrayEquals(new String[] { "a", "b", "c" }, array);

        Stream<String> emptyStream = createStream();
        Object[] emptyArray = emptyStream.toArray();
        Assertions.assertEquals(0, emptyArray.length);
    }

    @Test
    public void testThrowIfEmpty() {
        Stream<Integer> stream1 = createStream(1, 2, 3);
        Assertions.assertDoesNotThrow(() -> stream1.throwIfEmpty());

        Stream<Integer> stream2 = createStream();
        Assertions.assertThrows(NoSuchElementException.class, () -> stream2.throwIfEmpty().count());
    }

    @Test
    public void testThrowIfEmptyWithSupplier() {
        Stream<Integer> stream1 = createStream(1, 2, 3);
        Assertions.assertDoesNotThrow(() -> stream1.throwIfEmpty(() -> new IllegalStateException("Empty!")));

        Stream<Integer> stream2 = createStream();
        Assertions.assertThrows(IllegalStateException.class, () -> stream2.throwIfEmpty(() -> new IllegalStateException("Custom empty message")).count());

        Stream<Integer> stream3 = createStream();
        Assertions.assertThrows(IllegalArgumentException.class, () -> stream3.throwIfEmpty(null));
    }

    @Test
    public void testPrintln() {
        Stream<Integer> stream = createStream(1, 2, 3);
        Assertions.assertDoesNotThrow(() -> stream.println());

        Stream<Integer> emptyStream = createStream();
        Assertions.assertDoesNotThrow(() -> emptyStream.println());
    }

    @Test
    public void testIsParallel() {
        Stream<Integer> stream = createStream(1, 2, 3);
        Assertions.assertFalse(stream.isParallel());
    }

    @Test
    public void testSequential() {
        Stream<Integer> stream = createStream(1, 2, 3);
        Stream<Integer> sequential = stream.sequential();

        Assertions.assertSame(stream, sequential);
        Assertions.assertFalse(sequential.isParallel());
    }

    @Test
    public void testParallel() {
        Stream<Integer> stream = createStream(1, 2, 3);
        Stream<Integer> parallel = stream.parallel();

        Assertions.assertTrue(parallel.isParallel());
    }

    @Test
    public void testParallelWithMaxThreadNum() {
        Stream<Integer> stream = createStream(1, 2, 3);

        Stream<Integer> parallel1 = stream.parallel(4);
        Assertions.assertTrue(parallel1.isParallel());

        Stream<Integer> stream2 = createStream(1, 2, 3);
        Stream<Integer> parallel2 = stream2.parallel(0);
        Assertions.assertTrue(parallel2.isParallel());

        Stream<Integer> stream3 = createStream(1, 2, 3);
        Assertions.assertThrows(IllegalArgumentException.class, () -> stream3.parallel(-1));
    }

    @Test
    public void testParallelWithExecutor() {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Stream<Integer> stream = createStream(1, 2, 3);
            Stream<Integer> parallel = stream.parallel(executor);

            Assertions.assertTrue(parallel.isParallel());
        } finally {
            executor.shutdown();
        }
    }

    @Test
    public void testParallelWithMaxThreadNumAndExecutor() {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Stream<Integer> stream = createStream(1, 2, 3);
            Stream<Integer> parallel = stream.parallel(4, executor);

            Assertions.assertTrue(parallel.isParallel());

            Stream<Integer> stream2 = createStream(1, 2, 3);
            Assertions.assertThrows(IllegalArgumentException.class, () -> stream2.parallel(-1, executor));
        } finally {
            executor.shutdown();
        }
    }

    @Test
    public void testParallelWithParallelSettings() {
        ParallelSettings ps = PS.create(Splitor.ARRAY);

        Stream<Integer> stream = createStream(1, 2, 3);
        Stream<Integer> parallel = stream.parallel(ps);

        Assertions.assertTrue(parallel.isParallel());

        Stream<Integer> stream2 = createStream(1, 2, 3);
        Assertions.assertThrows(IllegalArgumentException.class, () -> stream2.parallel((ParallelSettings) null));

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            ParallelSettings customPs = new ParallelSettings().maxThreadNum(4).executor(executor).splitor(Splitor.ARRAY);
            Stream<Integer> stream3 = createStream(1, 2, 3);
            Stream<Integer> parallel3 = stream3.parallel(customPs);
            Assertions.assertTrue(parallel3.isParallel());
        } finally {
            executor.shutdown();
        }
    }

    @Test
    public void testSps() {
        Stream<Integer> stream = createStream(1, 2, 3);
        Stream<Integer> result = stream.sps(s -> s.map(x -> x * 2));

        Assertions.assertFalse(result.isParallel());
        assertHaveSameElements(Arrays.asList(2, 4, 6), result.toList());

        Stream<Integer> closedStream = createStream(1, 2, 3);
        closedStream.close();
        Assertions.assertThrows(IllegalStateException.class, () -> closedStream.sps(s -> s));
    }

    @Test
    public void testSpsWithMaxThreadNum() {
        Stream<Integer> stream = createStream(1, 2, 3);
        Stream<Integer> result = stream.sps(4, s -> s.map(x -> x * 2));

        Assertions.assertFalse(result.isParallel());
        assertHaveSameElements(Arrays.asList(2, 4, 6), result.toList());
    }

    @Test
    public void testSpsWithMaxThreadNumAndExecutor() {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Stream<Integer> stream = createStream(1, 2, 3);
            Stream<Integer> result = stream.sps(4, executor, s -> s.map(x -> x * 2));

            Assertions.assertFalse(result.isParallel());
            assertHaveSameElements(Arrays.asList(2, 4, 6), result.toList());
        } finally {
            executor.shutdown();
        }
    }

    @Test
    public void testPsp() {
        Stream<Integer> stream = createStream(1, 2, 3);
        Stream<Integer> result = stream.psp(s -> s.map(x -> x * 2));

        Assertions.assertTrue(result.isParallel());
        List<Integer> resultList = result.toList();
        Collections.sort(resultList);
        Assertions.assertEquals(Arrays.asList(2, 4, 6), resultList);

        Stream<Integer> closedStream = createStream(1, 2, 3);
        closedStream.close();
        Assertions.assertThrows(IllegalStateException.class, () -> closedStream.psp(s -> s));
    }

    @Test
    public void testTransform() {
        Stream<Integer> stream = createStream(1, 2, 3);
        List<Integer> result = stream.transform(s -> s.map(x -> x * 2)).toList();

        Assertions.assertEquals(Arrays.asList(2, 4, 6), result);

        Stream<Integer> stream2 = createStream(1, 2, 3);
        Assertions.assertThrows(IllegalArgumentException.class, () -> stream2.transform(null));

        Stream<Integer> closedStream = createStream(1, 2, 3);
        closedStream.close();
        Assertions.assertThrows(IllegalStateException.class, () -> closedStream.transform(s -> s));
    }

    @Test
    public void testClose() {
        Stream<Integer> stream = createStream(1, 2, 3);
        Assertions.assertDoesNotThrow(() -> stream.close());

        Assertions.assertDoesNotThrow(() -> stream.close());

        Assertions.assertThrows(IllegalStateException.class, () -> stream.map(x -> x * 2));
    }

    @Test
    public void testCloseWithHandlers() {
        List<String> closeOrder = new ArrayList<>();

        Stream<Integer> stream = createStream(1, 2, 3).onClose(() -> closeOrder.add("handler1")).onClose(() -> closeOrder.add("handler2"));

        stream.close();

        Assertions.assertEquals(2, closeOrder.size());
        Assertions.assertTrue(closeOrder.contains("handler1"));
        Assertions.assertTrue(closeOrder.contains("handler2"));
    }

    @Test
    public void testCloseWithException() {
        Stream<Integer> stream = createStream(1, 2, 3).onClose(() -> {
            throw new RuntimeException("Close error");
        });

        Assertions.assertThrows(RuntimeException.class, () -> stream.close());
    }

    @Test
    public void testMultipleCloseHandlersWithExceptions() {
        List<String> closeOrder = new ArrayList<>();

        Stream<Integer> stream = createStream(1, 2, 3).onClose(() -> {
            closeOrder.add("handler1");
            throw new RuntimeException("Error 1");
        }).onClose(() -> {
            closeOrder.add("handler2");
            throw new RuntimeException("Error 2");
        }).onClose(() -> closeOrder.add("handler3"));

        RuntimeException thrown = Assertions.assertThrows(RuntimeException.class, () -> stream.close());

        Assertions.assertEquals(3, closeOrder.size());

        Assertions.assertTrue(thrown.getSuppressed().length > 0);
    }

    @Test
    public void testAssertNotClosed() {
        Stream<Integer> stream = createStream(1, 2, 3);

        Assertions.assertDoesNotThrow(() -> stream.map(x -> x * 2).toList());

        stream.close();

        Assertions.assertThrows(IllegalStateException.class, () -> stream.map(x -> x * 2));
    }

    @Test
    public void testOnClose() {
        AtomicBoolean handlerCalled = new AtomicBoolean(false);
        createStream(1, 2, 3).onClose(() -> handlerCalled.set(true)).close();
        Assertions.assertTrue(handlerCalled.get());
    }

    @Test
    public void testMultipleOnClose() {
        {
            List<Integer> callOrder = new ArrayList<>();

            Stream<Integer> stream = createStream(1, 2, 3);
            stream = stream.onClose(() -> callOrder.add(1)).onClose(() -> callOrder.add(2)).onClose(() -> callOrder.add(3));

            stream.close();

            Assertions.assertEquals(Arrays.asList(1, 2, 3), callOrder);
        }
        {
            List<Integer> callOrder = new ArrayList<>();
            java.util.stream.Stream<Integer> javaStream = java.util.stream.Stream.of(1, 2, 3);
            javaStream.onClose(() -> callOrder.add(1)).onClose(() -> callOrder.add(2)).onClose(() -> callOrder.add(3));
            javaStream.close();
            Assertions.assertEquals(Arrays.asList(1, 2, 3), callOrder);
        }
    }

    @Test
    public void testLimitWithOffset() {
        Stream<Integer> stream1 = createStream(1, 2, 3, 4, 5);
        List<Integer> result1 = stream1.limit(2, 2).toList();
        Assertions.assertEquals(Arrays.asList(3, 4), result1);

        Stream<Integer> stream2 = createStream(1, 2, 3, 4, 5);
        List<Integer> result2 = stream2.limit(0, 3).toList();
        Assertions.assertEquals(Arrays.asList(1, 2, 3), result2);

        Stream<Integer> stream3 = createStream(1, 2, 3, 4, 5);
        List<Integer> result3 = stream3.limit(0, Long.MAX_VALUE).toList();
        Assertions.assertEquals(Arrays.asList(1, 2, 3, 4, 5), result3);

        Stream<Integer> stream4 = createStream(1, 2, 3, 4, 5);
        List<Integer> result4 = stream4.limit(3, Long.MAX_VALUE).toList();
        Assertions.assertEquals(Arrays.asList(4, 5), result4);

        Stream<Integer> stream5 = createStream(1, 2, 3);
        Assertions.assertThrows(IllegalArgumentException.class, () -> stream5.limit(-1, 2));
    }

    @Test
    public void testApplyIfNotEmpty() {
        Stream<Integer> stream1 = createStream(1, 2, 3);
        com.landawn.abacus.util.u.Optional<List<Integer>> result1 = stream1.applyIfNotEmpty(s -> s.toList());
        Assertions.assertTrue(result1.isPresent());
        Assertions.assertEquals(Arrays.asList(1, 2, 3), result1.get());

        Stream<Integer> stream2 = createStream();
        com.landawn.abacus.util.u.Optional<List<Integer>> result2 = stream2.applyIfNotEmpty(s -> s.toList());
        Assertions.assertFalse(result2.isPresent());

        // Verify stream is closed after applyIfNotEmpty
        Stream<Integer> stream3 = createStream(1, 2, 3);
        stream3.applyIfNotEmpty(s -> s.toList());
        Assertions.assertThrows(IllegalStateException.class, () -> stream3.toList());
    }

    @Test
    public void testAcceptIfNotEmpty() {
        List<Integer> collected = new ArrayList<>();
        Stream<Integer> stream1 = createStream(1, 2, 3);
        OrElse result1 = stream1.acceptIfNotEmpty(s -> collected.addAll(s.toList()));
        Assertions.assertEquals(Arrays.asList(1, 2, 3), collected);

        Stream<Integer> stream2 = createStream();
        AtomicBoolean called = new AtomicBoolean(false);
        stream2.acceptIfNotEmpty(s -> called.set(true));
        Assertions.assertFalse(called.get());

        // Verify stream is closed after acceptIfNotEmpty
        Stream<Integer> stream3 = createStream(1, 2, 3);
        stream3.acceptIfNotEmpty(s -> s.toList());
        Assertions.assertThrows(IllegalStateException.class, () -> stream3.toList());
    }

    @Test
    public void testIsEmptyRange() {
        Assertions.assertTrue(StreamBase.isEmptyRange(5, 2, 2));
        Assertions.assertFalse(StreamBase.isEmptyRange(5, 0, 3));
        Assertions.assertTrue(StreamBase.isEmptyRange(5, 0, 0));

        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> StreamBase.isEmptyRange(5, 3, 2));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> StreamBase.isEmptyRange(5, -1, 2));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> StreamBase.isEmptyRange(5, 0, 6));
    }

    @Test
    public void testIterateShortStream() {
        ShortStream shortStream = ShortStream.of((short) 10, (short) 20, (short) 30);
        ShortIteratorEx shortIter = StreamBase.iterate(shortStream);
        Assertions.assertTrue(shortIter.hasNext());
        Assertions.assertEquals((short) 10, shortIter.nextShort());

        ShortIteratorEx emptyShortIter = StreamBase.iterate((ShortStream) null);
        Assertions.assertFalse(emptyShortIter.hasNext());
    }

    @Test
    public void testIterateLongStream() {
        LongStream longStream = LongStream.of(100L, 200L, 300L);
        LongIteratorEx longIter = StreamBase.iterate(longStream);
        Assertions.assertTrue(longIter.hasNext());
        Assertions.assertEquals(100L, longIter.nextLong());

        LongIteratorEx emptyLongIter = StreamBase.iterate((LongStream) null);
        Assertions.assertFalse(emptyLongIter.hasNext());
    }

    @Test
    public void testIterateFloatDoubleStream() {
        FloatStream floatStream = FloatStream.of(1.5f, 2.5f);
        FloatIteratorEx floatIter = StreamBase.iterate(floatStream);
        Assertions.assertTrue(floatIter.hasNext());
        Assertions.assertEquals(1.5f, floatIter.nextFloat(), 0.001f);

        FloatIteratorEx emptyFloatIter = StreamBase.iterate((FloatStream) null);
        Assertions.assertFalse(emptyFloatIter.hasNext());

        DoubleStream doubleStream = DoubleStream.of(1.5, 2.5);
        DoubleIteratorEx doubleIter = StreamBase.iterate(doubleStream);
        Assertions.assertTrue(doubleIter.hasNext());
        Assertions.assertEquals(1.5, doubleIter.nextDouble(), 0.001);

        DoubleIteratorEx emptyDoubleIter = StreamBase.iterate((DoubleStream) null);
        Assertions.assertFalse(emptyDoubleIter.hasNext());
    }

}
