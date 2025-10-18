package com.landawn.abacus.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.If.OrElse;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.stream.Stream;

@Tag("new-test")
public class Seq105Test extends TestBase {

    @TempDir
    File tempDir;

    private File tempFile;
    private StringWriter stringWriter;
    private ByteArrayOutputStream byteArrayOutputStream;

    @BeforeEach
    public void setUp() throws IOException {
        tempFile = new File(tempDir, "test.txt");
        stringWriter = new StringWriter();
        byteArrayOutputStream = new ByteArrayOutputStream();
    }

    @AfterEach
    public void tearDown() {
        IOUtil.close(stringWriter);
        IOUtil.close(byteArrayOutputStream);
    }

    @Test
    public void testPrintln() throws Exception {
        PrintStream originalOut = System.out;
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));

        try {
            List<String> data = Arrays.asList("A", "B", "C");
            Seq<String, Exception> seq = Seq.of(data);

            seq.println();

            String output = outContent.toString();
            assertTrue(output.contains("[A, B, C]"));
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    public void testCast() throws Exception {
        List<String> data = Arrays.asList("a", "b", "c");
        Seq<String, IOException> seq = Seq.of(data.iterator(), IOException.class);

        Seq<String, Exception> castedSeq = seq.cast();

        assertNotNull(castedSeq);
        List<String> result = castedSeq.toList();
        assertEquals(data, result);
    }

    @Test
    public void testCastWithClosedSequence() throws Exception {
        Seq<String, IOException> seq = Seq.of(Arrays.asList("a", "b", "c").iterator(), IOException.class);
        seq.close();

        assertThrows(IllegalStateException.class, () -> seq.cast());
    }

    @Test
    public void testStream() throws Exception {
        List<Integer> data = Arrays.asList(1, 2, 3, 4, 5);
        Seq<Integer, Exception> seq = Seq.of(data);

        Stream<Integer> stream = seq.stream();

        assertNotNull(stream);
        List<Integer> result = stream.toList();
        assertEquals(data, result);
    }

    @Test
    public void testStreamWithCloseHandlers() throws Exception {
        AtomicBoolean closeHandlerCalled = new AtomicBoolean(false);
        List<String> data = Arrays.asList("x", "y", "z");

        Seq<String, Exception> seq = Seq.of(data).onClose(() -> closeHandlerCalled.set(true));

        Stream<String> stream = seq.stream();
        List<String> result = stream.toList();

        assertEquals(data, result);
        assertTrue(closeHandlerCalled.get());
    }

    @Test
    public void testStreamClosedSequence() throws Exception {
        Seq<String, Exception> seq = Seq.of(Arrays.asList("a", "b", "c"));
        seq.close();

        assertThrows(IllegalStateException.class, () -> seq.stream());
    }

    @Test
    public void testTransform() throws Exception {
        List<Integer> data = Arrays.asList(1, 2, 3, 4, 5);
        Seq<Integer, Exception> seq = Seq.of(data);

        Seq<String, Exception> transformed = seq.transform(s -> s.map(i -> "Number: " + i));

        List<String> result = transformed.toList();
        assertEquals(5, result.size());
        assertEquals("Number: 1", result.get(0));
        assertEquals("Number: 5", result.get(4));
    }

    @Test
    public void testTransformWithFilter() throws Exception {
        List<Integer> data = Arrays.asList(1, 2, 3, 4, 5, 6);
        Seq<Integer, Exception> seq = Seq.of(data);

        Seq<Integer, Exception> transformed = seq.transform(s -> s.filter(i -> i % 2 == 0));

        List<Integer> result = transformed.toList();
        assertEquals(Arrays.asList(2, 4, 6), result);
    }

    @Test
    public void testTransformNullFunction() throws Exception {
        Seq<String, Exception> seq = Seq.of(Arrays.asList("a", "b"));

        assertThrows(IllegalArgumentException.class, () -> seq.transform(null));
    }

    @Test
    public void testTransformB() throws Exception {
        List<String> data = Arrays.asList("apple", "banana", "cherry");
        Seq<String, Exception> seq = Seq.of(data);

        Seq<Integer, Exception> transformed = seq.transformB(stream -> stream.map(String::length));

        List<Integer> result = transformed.toList();
        assertEquals(Arrays.asList(5, 6, 6), result);
    }

    @Test
    public void testTransformBWithDeferred() throws Exception {
        List<Integer> data = Arrays.asList(1, 2, 3, 4, 5);
        AtomicBoolean transformExecuted = new AtomicBoolean(false);

        Seq<Integer, Exception> seq = Seq.of(data);

        Seq<Integer, Exception> transformed = seq.transformB(stream -> {
            transformExecuted.set(true);
            return stream.filter(i -> i > 2);
        }, true);

        assertFalse(transformExecuted.get());

        List<Integer> result = transformed.toList();
        assertTrue(transformExecuted.get());
        assertEquals(Arrays.asList(3, 4, 5), result);
    }

    @Test
    public void testTransformBImmediate() throws Exception {
        List<String> data = Arrays.asList("a", "b", "c");
        Seq<String, Exception> seq = Seq.of(data);

        Seq<String, Exception> transformed = seq.transformB(stream -> stream.map(String::toUpperCase), false);

        List<String> result = transformed.toList();
        assertEquals(Arrays.asList("A", "B", "C"), result);
    }

    @Test
    public void testSps() throws Exception {
        List<Integer> data = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        Seq<Integer, Exception> seq = Seq.of(data);

        Seq<Integer, Exception> result = seq.sps(stream -> stream.filter(i -> i % 2 == 0).map(i -> i * 2));

        List<Integer> resultList = result.toList();
        assertHaveSameElements(Arrays.asList(4, 8, 12, 16, 20), resultList);
    }

    @Test
    public void testSpsWithMaxThreadNum() throws Exception {
        List<Integer> data = new ArrayList<>();
        for (int i = 1; i <= 100; i++) {
            data.add(i);
        }

        Seq<Integer, Exception> seq = Seq.of(data);

        Seq<Integer, Exception> result = seq.sps(4, stream -> stream.filter(i -> i > 50));

        assertEquals(50, result.count());
    }

    @Test
    public void testSpsInvalidThreadNum() throws Exception {

        assertThrows(IllegalArgumentException.class, () -> Seq.empty().sps(0, stream -> stream));

        assertThrows(IllegalArgumentException.class, () -> Seq.empty().sps(-1, stream -> stream));
    }

    @Test
    public void testAsyncRun() throws Exception {
        List<String> data = Arrays.asList("a", "b", "c");
        List<String> collectedData = new ArrayList<>();

        Seq<String, Exception> seq = Seq.of(data);

        ContinuableFuture<Void> future = seq.asyncRun(s -> s.forEach(collectedData::add));

        future.get();
        assertEquals(data, collectedData);
    }

    @Test
    public void testAsyncRunWithExecutor() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            List<Integer> data = Arrays.asList(1, 2, 3, 4, 5);
            AtomicInteger sum = new AtomicInteger(0);

            Seq<Integer, Exception> seq = Seq.of(data);

            ContinuableFuture<Void> future = seq.asyncRun(s -> s.forEach(sum::addAndGet), executor);

            future.get();
            assertEquals(15, sum.get());
        } finally {
            executor.shutdown();
        }
    }

    @Test
    public void testAsyncRunNullAction() throws Exception {
        Seq<String, Exception> seq = Seq.of(Arrays.asList("a", "b"));

        assertThrows(IllegalArgumentException.class, () -> seq.asyncRun(null));
    }

    @Test
    public void testAsyncCall() throws Exception {
        List<Integer> data = Arrays.asList(1, 2, 3, 4, 5);
        Seq<Integer, Exception> seq = Seq.of(data);

        ContinuableFuture<Long> future = seq.asyncCall(s -> s.sumInt(e -> e));

        Long result = future.get();
        assertEquals(15, result);
    }

    @Test
    public void testAsyncCallWithExecutor() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            List<String> data = Arrays.asList("hello", "world");
            Seq<String, Exception> seq = Seq.of(data);

            ContinuableFuture<String> future = seq.asyncCall(s -> s.join(" "), executor);

            String result = future.get();
            assertEquals("hello world", result);
        } finally {
            executor.shutdown();
        }
    }

    @Test
    public void testAsyncCallReturningList() throws Exception {
        List<Integer> data = Arrays.asList(1, 2, 3, 4, 5);
        Seq<Integer, Exception> seq = Seq.of(data);

        ContinuableFuture<List<Integer>> future = seq.asyncCall(s -> s.filter(i -> i > 2).toList());

        List<Integer> result = future.get();
        assertEquals(Arrays.asList(3, 4, 5), result);
    }

    @Test
    public void testApplyIfNotEmpty() throws Exception {
        List<Integer> data = Arrays.asList(10, 20, 30);
        Seq<Integer, Exception> seq = Seq.of(data);

        u.Optional<Double> result = seq.applyIfNotEmpty(s -> s.averageDouble(e -> e).orElse(0.0));

        assertTrue(result.isPresent());
        assertEquals(20.0, result.get(), 0.001);
    }

    @Test
    public void testApplyIfNotEmptyWithEmptySeq() throws Exception {
        Seq<String, Exception> seq = Seq.empty();

        Optional<String> result = seq.applyIfNotEmpty(s -> s.join(", "));

        assertFalse(result.isPresent());
    }

    @Test
    public void testApplyIfNotEmptyReturningNull() throws Exception {
        List<String> data = Arrays.asList("a", "b", "c");
        Seq<String, Exception> seq = Seq.of(data);

        Optional<String> result = seq.applyIfNotEmpty(s -> null);

        assertFalse(result.isPresent());
    }

    @Test
    public void testApplyIfNotEmptyNullFunction() throws Exception {
        Seq<String, Exception> seq = Seq.of(Arrays.asList("a", "b"));

        assertThrows(IllegalArgumentException.class, () -> seq.applyIfNotEmpty(null));
    }

    @Test
    public void testAcceptIfNotEmpty() throws Exception {
        List<String> data = Arrays.asList("x", "y", "z");
        List<String> collectedData = new ArrayList<>();

        Seq<String, Exception> seq = Seq.of(data);

        OrElse result = seq.acceptIfNotEmpty(s -> s.forEach(collectedData::add));

        assertEquals(data, collectedData);
    }

    @Test
    public void testAcceptIfNotEmptyWithEmptySeq() throws Exception {
        AtomicBoolean actionExecuted = new AtomicBoolean(false);
        Seq<Integer, Exception> seq = Seq.empty();

        OrElse result = seq.acceptIfNotEmpty(s -> {
            actionExecuted.set(true);
            s.forEach(System.out::println);
        });

        assertFalse(actionExecuted.get());
    }

    @Test
    public void testAcceptIfNotEmptyWithOrElse() throws Exception {
        AtomicBoolean primaryExecuted = new AtomicBoolean(false);
        AtomicBoolean elseExecuted = new AtomicBoolean(false);

        Seq<String, Exception> seq = Seq.empty();

        seq.acceptIfNotEmpty(s -> {
            primaryExecuted.set(true);
        }).orElse(() -> {
            elseExecuted.set(true);
        });

        assertFalse(primaryExecuted.get());
        assertTrue(elseExecuted.get());
    }

    @Test
    public void testOnClose() throws Exception {
        AtomicInteger closeCount = new AtomicInteger(0);
        List<String> data = Arrays.asList("a", "b", "c");

        Seq<String, Exception> seq = Seq.of(data).onClose(() -> closeCount.incrementAndGet());

        List<String> result = seq.toList();

        assertEquals(data, result);
        assertEquals(1, closeCount.get());
    }

    @Test
    public void testOnCloseMultipleHandlers() throws Exception {
        List<Integer> closeOrder = new ArrayList<>();

        Seq<Integer, Exception> seq = Seq.of(Arrays.asList(1, 2, 3))
                .onClose(() -> closeOrder.add(1))
                .onClose(() -> closeOrder.add(2))
                .onClose(() -> closeOrder.add(3));

        seq.count();

        assertEquals(Arrays.asList(1, 2, 3), closeOrder);
    }

    @Test
    public void testOnCloseWithNullHandler() throws Exception {
        Seq<String, Exception> seq = Seq.of(Arrays.asList("a", "b"));

        assertThrows(IllegalArgumentException.class, () -> seq.onClose(null));
    }

    @Test
    public void testOnCloseNotCalledTwice() throws Exception {
        AtomicInteger closeCount = new AtomicInteger(0);

        Seq<String, Exception> seq = Seq.of(Arrays.asList("x", "y")).onClose(() -> closeCount.incrementAndGet());

        seq.close();
        seq.close();

        assertEquals(1, closeCount.get());
    }

    @Test
    public void testClose() throws Exception {
        AtomicBoolean isClosed = new AtomicBoolean(false);

        Seq<String, Exception> seq = Seq.of(Arrays.asList("a", "b", "c")).onClose(() -> isClosed.set(true));

        assertFalse(isClosed.get());

        seq.close();

        assertTrue(isClosed.get());

        assertThrows(IllegalStateException.class, () -> seq.toList());
    }

    @Test
    public void testCloseIdempotent() throws Exception {
        AtomicInteger closeCount = new AtomicInteger(0);

        Seq<Integer, Exception> seq = Seq.of(Arrays.asList(1, 2, 3)).onClose(() -> closeCount.incrementAndGet());

        seq.close();
        seq.close();
        seq.close();

        assertEquals(1, closeCount.get());
    }

    @Test
    public void testAutoCloseOnTerminalOperation() throws Exception {
        AtomicBoolean closeHandlerCalled = new AtomicBoolean(false);

        Seq<String, Exception> seq = Seq.of(Arrays.asList("a", "b", "c")).onClose(() -> closeHandlerCalled.set(true));

        long count = seq.count();

        assertEquals(3, count);
        assertTrue(closeHandlerCalled.get());
    }

    @Test
    public void testTryWithResources() throws Exception {
        AtomicBoolean closeHandlerCalled = new AtomicBoolean(false);
        List<String> result;

        try (Seq<String, Exception> seq = Seq.of(Arrays.asList("x", "y", "z")).onClose(() -> closeHandlerCalled.set(true))) {
            result = seq.toList();
        }

        assertEquals(Arrays.asList("x", "y", "z"), result);
        assertTrue(closeHandlerCalled.get());
    }

    @Test
    public void testCloseWithExceptionInHandler() throws Exception {
        AtomicBoolean firstHandlerCalled = new AtomicBoolean(false);
        AtomicBoolean secondHandlerCalled = new AtomicBoolean(false);

        Seq<Integer, Exception> seq = Seq.of(Arrays.asList(1, 2, 3)).onClose(() -> {
            firstHandlerCalled.set(true);
            throw new RuntimeException("First handler exception");
        }).onClose(() -> {
            secondHandlerCalled.set(true);
        });

        assertThrows(RuntimeException.class, () -> seq.close());

        assertTrue(firstHandlerCalled.get());
        assertTrue(secondHandlerCalled.get());
    }

    private static class User {
        private final String name;
        private final int age;

        public User(String name, int age) {
            this.name = name;
            this.age = age;
        }

        public String getName() {
            return name;
        }

        public int getAge() {
            return age;
        }
    }
}
