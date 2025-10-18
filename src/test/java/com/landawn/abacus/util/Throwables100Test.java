package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Throwables.Callable;
import com.landawn.abacus.util.function.Function;

@Tag("new-test")
public class Throwables100Test extends TestBase {

    private static class TestException extends Exception {
        public TestException(String message) {
            super(message);
        }
    }

    private static class TestRuntimeException extends RuntimeException {
        public TestRuntimeException(String message) {
            super(message);
        }
    }

    @Test
    public void testRun_Success() {
        AtomicBoolean executed = new AtomicBoolean(false);
        Throwables.run(() -> executed.set(true));
        assertTrue(executed.get(), "Runnable should have been executed");
    }

    @Test
    public void testRun_ThrowsException() {
        assertThrows(RuntimeException.class, () -> Throwables.run(() -> {
            throw new TestException("Test exception");
        }));
    }

    @Test
    public void testRun_ThrowsRuntimeException() {
        try {
            Throwables.run(() -> {
                throw new TestRuntimeException("Test runtime exception");
            });
            fail("Should have thrown RuntimeException");
        } catch (TestRuntimeException e) {
            assertEquals("Test runtime exception", e.getMessage());
        }
    }

    @Test
    public void testRun_WithErrorHandler_Success() {
        AtomicBoolean executed = new AtomicBoolean(false);
        AtomicBoolean errorHandled = new AtomicBoolean(false);

        Throwables.run(() -> executed.set(true), e -> errorHandled.set(true));

        assertTrue(executed.get(), "Runnable should have been executed");
        assertFalse(errorHandled.get(), "Error handler should not have been called");
    }

    @Test
    public void testRun_WithErrorHandler_HandlesException() {
        AtomicReference<Throwable> capturedError = new AtomicReference<>();

        Throwables.run(() -> {
            throw new TestException("Test exception");
        }, capturedError::set);

        assertNotNull(capturedError.get(), "Error should have been captured");
        assertEquals("Test exception", capturedError.get().getMessage());
    }

    @Test
    public void testCall_Success() {
        String result = Throwables.call(() -> "Success");
        assertEquals("Success", result);
    }

    @Test
    public void testCall_ThrowsException() {
        assertThrows(RuntimeException.class, () -> Throwables.call(() -> {
            throw new TestException("Test exception");
        }));
    }

    @Test
    public void testCall_ReturnsNull() {
        String result = Throwables.call(() -> null);
        assertNull(result);
    }

    @Test
    public void testCall_WithErrorFunction_Success() {
        Callable<String, Throwable> cmd = () -> "Success";
        Function<Throwable, String> f = Fn.f(e -> "Error: " + e.getMessage());
        String result = Throwables.call(cmd, f);
        assertEquals("Success", result);
    }

    @Test
    public void testCall_WithErrorFunction_HandlesException() {
        Callable<String, Throwable> cmd = () -> {
            throw new TestException("Test error");
        };
        String result = Throwables.call(cmd, Fn.f(e -> "Handled: " + e.getMessage()));

        assertEquals("Handled: Test error", result);
    }

    @Test
    public void testCall_WithSupplier_Success() {
        String result = Throwables.call(() -> "Success", Fn.s(() -> "Default"));
        assertEquals("Success", result);
    }

    @Test
    public void testCall_WithSupplier_ReturnsSupplierValueOnError() {
        String result = Throwables.call(() -> {
            throw new TestException("Error");
        }, Fn.s(() -> "Default value"));

        assertEquals("Default value", result);
    }

    @Test
    public void testCall_WithDefaultValue_Success() {
        String result = Throwables.call(() -> "Success", "Default");
        assertEquals("Success", result);
    }

    @Test
    public void testCall_WithDefaultValue_ReturnsDefaultOnError() {
        String result = Throwables.call(() -> {
            throw new TestException("Error");
        }, "Default value");

        assertEquals("Default value", result);
    }

    @Test
    public void testCall_WithNullDefaultValue_ReturnsNullOnError() {
        String result = Throwables.call(() -> {
            throw new TestException("Error");
        }, (String) null);

        assertNull(result);
    }

    @Test
    public void testCall_WithPredicateSupplier_Success() {
        String result = Throwables.call(() -> "Success", e -> e instanceof TestException, Fn.s(() -> "Handled"));
        assertEquals("Success", result);
    }

    @Test
    public void testCall_WithPredicateSupplier_PredicateTrue() {
        String result = Throwables.call(() -> {
            throw new TestException("Error");
        }, e -> e instanceof TestException, Fn.s(() -> "Handled by predicate"));
        assertEquals("Handled by predicate", result);
    }

    @Test
    public void testCall_WithPredicateSupplier_PredicateFalse() {
        assertThrows(RuntimeException.class, () -> Throwables.call(() -> {
            throw new TestException("Error");
        }, e -> e instanceof IOException, Fn.s(() -> "Should not reach here")));
    }

    @Test
    public void testCall_WithPredicateDefault_Success() {
        String result = Throwables.call(() -> "Success", e -> e instanceof TestException, "Default");
        assertEquals("Success", result);
    }

    @Test
    public void testCall_WithPredicateDefault_PredicateTrue() {
        String result = Throwables.call(() -> {
            throw new TestException("Error");
        }, e -> e instanceof TestException, "Default value");
        assertEquals("Default value", result);
    }

    @Test
    public void testCall_WithPredicateDefault_PredicateFalse() {
        assertThrows(RuntimeException.class, () -> Throwables.call(() -> {
            throw new TestException("Error");
        }, e -> e instanceof IOException, "Should not reach here"));
    }

    @Test
    public void testIterator_Empty() throws Exception {
        Throwables.Iterator<String, Exception> iter = Throwables.Iterator.empty();
        assertFalse(iter.hasNext());
    }

    @Test
    public void testIterator_Empty_NextThrowsException() throws Exception {
        Throwables.Iterator<String, Exception> iter = Throwables.Iterator.empty();
        assertThrows(NoSuchElementException.class, () -> iter.next());
    }

    @Test
    public void testIterator_Just() throws Exception {
        Throwables.Iterator<String, Exception> iter = Throwables.Iterator.just("Hello");
        assertTrue(iter.hasNext());
        assertEquals("Hello", iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testIterator_Just_SecondNextThrowsException() throws Exception {
        Throwables.Iterator<String, Exception> iter = Throwables.Iterator.just("Hello");
        iter.next();
        assertThrows(NoSuchElementException.class, () -> iter.next());
    }

    @Test
    public void testIterator_Just_WithNull() throws Exception {
        Throwables.Iterator<String, Exception> iter = Throwables.Iterator.just(null);
        assertTrue(iter.hasNext());
        assertNull(iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testIterator_OfArray_Empty() throws Exception {
        Throwables.Iterator<String, Exception> iter = Throwables.Iterator.of();
        assertFalse(iter.hasNext());
    }

    @Test
    public void testIterator_OfArray_SingleElement() throws Exception {
        Throwables.Iterator<String, Exception> iter = Throwables.Iterator.of("One");
        assertTrue(iter.hasNext());
        assertEquals("One", iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testIterator_OfArray_MultipleElements() throws Exception {
        Throwables.Iterator<String, Exception> iter = Throwables.Iterator.of("One", "Two", "Three");
        assertEquals("One", iter.next());
        assertEquals("Two", iter.next());
        assertEquals("Three", iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testIterator_OfArray_WithNulls() throws Exception {
        Throwables.Iterator<String, Exception> iter = Throwables.Iterator.of("One", null, "Three");
        assertEquals("One", iter.next());
        assertNull(iter.next());
        assertEquals("Three", iter.next());
    }

    @Test
    public void testIterator_OfArrayRange_FullRange() throws Exception {
        String[] array = { "A", "B", "C", "D" };
        Throwables.Iterator<String, Exception> iter = Throwables.Iterator.of(array, 0, 4);
        assertEquals("A", iter.next());
        assertEquals("B", iter.next());
        assertEquals("C", iter.next());
        assertEquals("D", iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testIterator_OfArrayRange_PartialRange() throws Exception {
        String[] array = { "A", "B", "C", "D" };
        Throwables.Iterator<String, Exception> iter = Throwables.Iterator.of(array, 1, 3);
        assertEquals("B", iter.next());
        assertEquals("C", iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testIterator_OfArrayRange_EmptyRange() throws Exception {
        String[] array = { "A", "B", "C", "D" };
        Throwables.Iterator<String, Exception> iter = Throwables.Iterator.of(array, 2, 2);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testIterator_OfArrayRange_InvalidRange() throws Exception {
        String[] array = { "A", "B", "C" };
        assertThrows(IndexOutOfBoundsException.class, () -> Throwables.Iterator.of(array, 1, 5));
    }

    @Test
    public void testIterator_OfIterable_List() throws Exception {
        List<String> list = Arrays.asList("One", "Two", "Three");
        Throwables.Iterator<String, Exception> iter = Throwables.Iterator.of(list);
        assertEquals("One", iter.next());
        assertEquals("Two", iter.next());
        assertEquals("Three", iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testIterator_OfIterable_EmptyList() throws Exception {
        List<String> list = Collections.emptyList();
        Throwables.Iterator<String, Exception> iter = Throwables.Iterator.of(list);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testIterator_OfIterable_Null() throws Exception {
        Throwables.Iterator<String, Exception> iter = Throwables.Iterator.of((Iterable<String>) null);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testIterator_OfJavaIterator() throws Exception {
        List<String> list = Arrays.asList("A", "B", "C");
        Throwables.Iterator<String, Exception> iter = Throwables.Iterator.of(list.iterator());
        assertEquals("A", iter.next());
        assertEquals("B", iter.next());
        assertEquals("C", iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testIterator_OfJavaIterator_Null() throws Exception {
        Throwables.Iterator<String, Exception> iter = Throwables.Iterator.of((java.util.Iterator<String>) null);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testIterator_Defer() throws Exception {
        AtomicBoolean supplierCalled = new AtomicBoolean(false);
        Throwables.Iterator<String, Exception> iter = Throwables.Iterator.defer(() -> {
            supplierCalled.set(true);
            return Throwables.Iterator.of("Deferred");
        });

        assertFalse(supplierCalled.get(), "Supplier should not be called yet");
        assertTrue(iter.hasNext());
        assertTrue(supplierCalled.get(), "Supplier should be called now");
        assertEquals("Deferred", iter.next());
    }

    @Test
    public void testIterator_Defer_CalledOnNext() throws Exception {
        AtomicBoolean supplierCalled = new AtomicBoolean(false);
        Throwables.Iterator<String, Exception> iter = Throwables.Iterator.defer(() -> {
            supplierCalled.set(true);
            return Throwables.Iterator.of("Value");
        });

        assertFalse(supplierCalled.get(), "Supplier should not be called yet");
        assertEquals("Value", iter.next());
        assertTrue(supplierCalled.get(), "Supplier should be called now");
    }

    @Test
    public void testIterator_Concat_Arrays() throws Exception {
        Throwables.Iterator<String, Exception> iter1 = Throwables.Iterator.of("A", "B");
        Throwables.Iterator<String, Exception> iter2 = Throwables.Iterator.of("C", "D");
        Throwables.Iterator<String, Exception> iter3 = Throwables.Iterator.of("E");

        Throwables.Iterator<String, Exception> concat = Throwables.Iterator.concat(iter1, iter2, iter3);

        assertEquals("A", concat.next());
        assertEquals("B", concat.next());
        assertEquals("C", concat.next());
        assertEquals("D", concat.next());
        assertEquals("E", concat.next());
        assertFalse(concat.hasNext());
    }

    @Test
    public void testIterator_Concat_WithEmpty() throws Exception {
        Throwables.Iterator<String, Exception> iter1 = Throwables.Iterator.of("A");
        Throwables.Iterator<String, Exception> iter2 = Throwables.Iterator.empty();
        Throwables.Iterator<String, Exception> iter3 = Throwables.Iterator.of("B");

        Throwables.Iterator<String, Exception> concat = Throwables.Iterator.concat(iter1, iter2, iter3);

        assertEquals("A", concat.next());
        assertEquals("B", concat.next());
        assertFalse(concat.hasNext());
    }

    @Test
    public void testIterator_Concat_Collection() throws Exception {
        List<Throwables.Iterator<String, Exception>> iterators = Arrays.asList(Throwables.Iterator.of("1", "2"), Throwables.Iterator.of("3"),
                Throwables.Iterator.of("4", "5"));

        Throwables.Iterator<String, Exception> concat = Throwables.Iterator.concat(iterators);

        assertEquals("1", concat.next());
        assertEquals("2", concat.next());
        assertEquals("3", concat.next());
        assertEquals("4", concat.next());
        assertEquals("5", concat.next());
        assertFalse(concat.hasNext());
    }

    @Test
    public void testIterator_Concat_EmptyCollection() throws Exception {
        List<Throwables.Iterator<String, Exception>> empty = Collections.emptyList();
        Throwables.Iterator<String, Exception> concat = Throwables.Iterator.concat(empty);
        assertFalse(concat.hasNext());
    }

    @Test
    public void testIterator_OfLines_SingleLine() throws IOException {
        StringReader reader = new StringReader("Hello World");
        Throwables.Iterator<String, IOException> iter = Throwables.Iterator.ofLines(reader);

        assertTrue(iter.hasNext());
        assertEquals("Hello World", iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testIterator_OfLines_MultipleLines() throws IOException {
        StringReader reader = new StringReader("Line 1\nLine 2\nLine 3");
        Throwables.Iterator<String, IOException> iter = Throwables.Iterator.ofLines(reader);

        assertEquals("Line 1", iter.next());
        assertEquals("Line 2", iter.next());
        assertEquals("Line 3", iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testIterator_OfLines_EmptyLines() throws IOException {
        StringReader reader = new StringReader("\n\n");
        Throwables.Iterator<String, IOException> iter = Throwables.Iterator.ofLines(reader);

        assertEquals("", iter.next());
        assertEquals("", iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testIterator_OfLines_NullReader() throws IOException {
        Throwables.Iterator<String, IOException> iter = Throwables.Iterator.ofLines(null);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testIterator_OfLines_NextAfterEnd() throws IOException {
        StringReader reader = new StringReader("Single line");
        Throwables.Iterator<String, IOException> iter = Throwables.Iterator.ofLines(reader);
        iter.next();
        assertThrows(NoSuchElementException.class, () -> iter.next());
    }

    @Test
    public void testIterator_Advance() throws Exception {
        Throwables.Iterator<String, Exception> iter = Throwables.Iterator.of("A", "B", "C", "D", "E");
        iter.advance(2);
        assertEquals("C", iter.next());
    }

    @Test
    public void testIterator_Advance_Zero() throws Exception {
        Throwables.Iterator<String, Exception> iter = Throwables.Iterator.of("A", "B", "C");
        iter.advance(0);
        assertEquals("A", iter.next());
    }

    @Test
    public void testIterator_Advance_BeyondEnd() throws Exception {
        Throwables.Iterator<String, Exception> iter = Throwables.Iterator.of("A", "B");
        iter.advance(5);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testIterator_Advance_Negative() throws Exception {
        Throwables.Iterator<String, Exception> iter = Throwables.Iterator.of("A");
        assertTrue(iter.hasNext());
    }

    @Test
    public void testIterator_Count_Empty() throws Exception {
        Throwables.Iterator<String, Exception> iter = Throwables.Iterator.empty();
        assertEquals(0, iter.count());
    }

    @Test
    public void testIterator_Count_SingleElement() throws Exception {
        Throwables.Iterator<String, Exception> iter = Throwables.Iterator.just("One");
        assertEquals(1, iter.count());
    }

    @Test
    public void testIterator_Count_MultipleElements() throws Exception {
        Throwables.Iterator<String, Exception> iter = Throwables.Iterator.of("A", "B", "C", "D");
        assertEquals(4, iter.count());
    }

    @Test
    public void testIterator_Count_AfterAdvance() throws Exception {
        Throwables.Iterator<String, Exception> iter = Throwables.Iterator.of("A", "B", "C", "D", "E");
        iter.advance(2);
        assertEquals(3, iter.count());
    }

    @Test
    public void testIterator_Filter() throws Exception {
        Throwables.Iterator<Integer, Exception> iter = Throwables.Iterator.of(1, 2, 3, 4, 5, 6);
        Throwables.Iterator<Integer, Exception> filtered = iter.filter(n -> n % 2 == 0);

        assertEquals(Integer.valueOf(2), filtered.next());
        assertEquals(Integer.valueOf(4), filtered.next());
        assertEquals(Integer.valueOf(6), filtered.next());
        assertFalse(filtered.hasNext());
    }

    @Test
    public void testIterator_Filter_None() throws Exception {
        Throwables.Iterator<Integer, Exception> iter = Throwables.Iterator.of(1, 3, 5);
        Throwables.Iterator<Integer, Exception> filtered = iter.filter(n -> n % 2 == 0);

        assertFalse(filtered.hasNext());
    }

    @Test
    public void testIterator_Filter_All() throws Exception {
        Throwables.Iterator<Integer, Exception> iter = Throwables.Iterator.of(2, 4, 6);
        Throwables.Iterator<Integer, Exception> filtered = iter.filter(n -> n % 2 == 0);

        assertEquals(Integer.valueOf(2), filtered.next());
        assertEquals(Integer.valueOf(4), filtered.next());
        assertEquals(Integer.valueOf(6), filtered.next());
        assertFalse(filtered.hasNext());
    }

    @Test
    public void testIterator_Map() throws Exception {
        Throwables.Iterator<Integer, Exception> iter = Throwables.Iterator.of(1, 2, 3);
        Throwables.Iterator<String, Exception> mapped = iter.map(n -> "Value: " + n);

        assertEquals("Value: 1", mapped.next());
        assertEquals("Value: 2", mapped.next());
        assertEquals("Value: 3", mapped.next());
        assertFalse(mapped.hasNext());
    }

    @Test
    public void testIterator_Map_Empty() throws Exception {
        Throwables.Iterator<Integer, Exception> iter = Throwables.Iterator.empty();
        Throwables.Iterator<String, Exception> mapped = iter.map(n -> "Value: " + n);

        assertFalse(mapped.hasNext());
    }

    @Test
    public void testIterator_Map_ToNull() throws Exception {
        Throwables.Iterator<String, Exception> iter = Throwables.Iterator.of("A", "B");
        Throwables.Iterator<String, Exception> mapped = iter.map(s -> null);

        assertNull(mapped.next());
        assertNull(mapped.next());
        assertFalse(mapped.hasNext());
    }

    @Test
    public void testIterator_First() throws Exception {
        Throwables.Iterator<String, Exception> iter = Throwables.Iterator.of("First", "Second");
        assertEquals("First", iter.first().orElse(null));
    }

    @Test
    public void testIterator_First_Empty() throws Exception {
        Throwables.Iterator<String, Exception> iter = Throwables.Iterator.empty();
        assertFalse(iter.first().isPresent());
    }

    @Test
    public void testIterator_First_SingleElement() throws Exception {
        Throwables.Iterator<String, Exception> iter = Throwables.Iterator.just("Only");
        assertEquals("Only", iter.first().orElse(null));
    }

    @Test
    public void testIterator_FirstNonNull() throws Exception {
        Throwables.Iterator<String, Exception> iter = Throwables.Iterator.of(null, null, "First Non-Null", "Second");
        assertEquals("First Non-Null", iter.firstNonNull().orElse(null));
    }

    @Test
    public void testIterator_FirstNonNull_AllNull() throws Exception {
        Throwables.Iterator<String, Exception> iter = Throwables.Iterator.of(null, null, null);
        assertFalse(iter.firstNonNull().isPresent());
    }

    @Test
    public void testIterator_FirstNonNull_Empty() throws Exception {
        Throwables.Iterator<String, Exception> iter = Throwables.Iterator.empty();
        assertFalse(iter.firstNonNull().isPresent());
    }

    @Test
    public void testIterator_Last() throws Exception {
        Throwables.Iterator<String, Exception> iter = Throwables.Iterator.of("First", "Middle", "Last");
        assertEquals("Last", iter.last().orElse(null));
    }

    @Test
    public void testIterator_Last_Empty() throws Exception {
        Throwables.Iterator<String, Exception> iter = Throwables.Iterator.empty();
        assertFalse(iter.last().isPresent());
    }

    @Test
    public void testIterator_Last_SingleElement() throws Exception {
        Throwables.Iterator<String, Exception> iter = Throwables.Iterator.just("Only");
        assertEquals("Only", iter.last().orElse(null));
    }

    @Test
    public void testIterator_ToArray() throws Exception {
        Throwables.Iterator<String, Exception> iter = Throwables.Iterator.of("A", "B", "C");
        Object[] array = iter.toArray();

        assertEquals(3, array.length);
        assertEquals("A", array[0]);
        assertEquals("B", array[1]);
        assertEquals("C", array[2]);
    }

    @Test
    public void testIterator_ToArray_Empty() throws Exception {
        Throwables.Iterator<String, Exception> iter = Throwables.Iterator.empty();
        Object[] array = iter.toArray();
        assertEquals(0, array.length);
    }

    @Test
    public void testIterator_ToArray_TypedArray() throws Exception {
        Throwables.Iterator<String, Exception> iter = Throwables.Iterator.of("X", "Y", "Z");
        String[] array = iter.toArray(new String[0]);

        assertEquals(3, array.length);
        assertEquals("X", array[0]);
        assertEquals("Y", array[1]);
        assertEquals("Z", array[2]);
    }

    @Test
    public void testIterator_ToArray_TypedArray_PreSized() throws Exception {
        Throwables.Iterator<String, Exception> iter = Throwables.Iterator.of("A", "B");
        String[] array = iter.toArray(new String[5]);

        assertEquals(5, array.length);
        assertEquals("A", array[0]);
        assertEquals("B", array[1]);
        assertNull(array[2]);
    }

    @Test
    public void testIterator_ToList() throws Exception {
        Throwables.Iterator<String, Exception> iter = Throwables.Iterator.of("One", "Two", "Three");
        List<String> list = iter.toList();

        assertEquals(3, list.size());
        assertEquals("One", list.get(0));
        assertEquals("Two", list.get(1));
        assertEquals("Three", list.get(2));
    }

    @Test
    public void testIterator_ToList_Empty() throws Exception {
        Throwables.Iterator<String, Exception> iter = Throwables.Iterator.empty();
        List<String> list = iter.toList();
        assertTrue(list.isEmpty());
    }

    @Test
    public void testIterator_ForEachRemaining() throws Exception {
        Throwables.Iterator<String, Exception> iter = Throwables.Iterator.of("A", "B", "C");
        List<String> collected = new ArrayList<>();

        iter.forEachRemaining(collected::add);

        assertEquals(Arrays.asList("A", "B", "C"), collected);
    }

    @Test
    public void testIterator_ForEachRemaining_PartiallyConsumed() throws Exception {
        Throwables.Iterator<String, Exception> iter = Throwables.Iterator.of("A", "B", "C", "D");
        iter.next();

        List<String> collected = new ArrayList<>();
        iter.forEachRemaining(collected::add);

        assertEquals(Arrays.asList("B", "C", "D"), collected);
    }

    @Test
    public void testIterator_ForeachRemaining() throws Exception {
        Throwables.Iterator<String, Exception> iter = Throwables.Iterator.of("X", "Y", "Z");
        List<String> collected = new ArrayList<>();

        iter.foreachRemaining(s -> collected.add(s.toLowerCase()));

        assertEquals(Arrays.asList("x", "y", "z"), collected);
    }

    @Test
    public void testIterator_ForeachIndexed() throws Exception {
        Throwables.Iterator<String, Exception> iter = Throwables.Iterator.of("A", "B", "C");
        List<String> collected = new ArrayList<>();

        iter.foreachIndexed((idx, value) -> collected.add(idx + ":" + value));

        assertEquals(Arrays.asList("0:A", "1:B", "2:C"), collected);
    }

    @Test
    public void testIterator_ForeachIndexed_Empty() throws Exception {
        Throwables.Iterator<String, Exception> iter = Throwables.Iterator.empty();
        AtomicInteger callCount = new AtomicInteger(0);

        iter.foreachIndexed((idx, value) -> callCount.incrementAndGet());

        assertEquals(0, callCount.get());
    }

    @Test
    public void testIterator_Close() throws Exception {
        AtomicBoolean resourceClosed = new AtomicBoolean(false);

        Throwables.Iterator<String, Exception> iter = new Throwables.Iterator<>() {
            private boolean hasMore = true;

            @Override
            public boolean hasNext() {
                return hasMore;
            }

            @Override
            public String next() {
                hasMore = false;
                return "Value";
            }

            @Override
            protected void closeResource() {
                resourceClosed.set(true);
            }
        };

        iter.close();
        assertTrue(resourceClosed.get(), "Resource should be closed");
    }

    @Test
    public void testIterator_Close_MultipleCalls() throws Exception {
        AtomicInteger closeCount = new AtomicInteger(0);

        Throwables.Iterator<String, Exception> iter = new Throwables.Iterator<>() {
            @Override
            public boolean hasNext() {
                return false;
            }

            @Override
            public String next() {
                throw new NoSuchElementException();
            }

            @Override
            protected void closeResource() {
                closeCount.incrementAndGet();
            }
        };

        iter.close();
        iter.close();
        iter.close();

        assertEquals(1, closeCount.get(), "closeResource should only be called once");
    }

    @Test
    public void testLazyInitializer_Basic() throws Exception {
        AtomicInteger callCount = new AtomicInteger(0);

        Throwables.Supplier<String, Exception> lazy = N.lazyInitialize(() -> {
            callCount.incrementAndGet();
            return "Initialized Value";
        });

        assertEquals(0, callCount.get());
        assertEquals("Initialized Value", lazy.get());
        assertEquals(1, callCount.get());
        assertEquals("Initialized Value", lazy.get());
        assertEquals(1, callCount.get());
    }

    @Test
    public void testLazyInitializer_WithNull() throws Exception {

        Throwables.Supplier<String, Exception> lazy = N.lazyInitialize(() -> null);

        assertNull(lazy.get());
        assertNull(lazy.get());
    }

    @Test
    public void testLazyInitializer_WithException() throws Exception {

        Throwables.Supplier<String, Exception> lazy = N.lazyInitialize(() -> {
            throw new TestException("Initialization failed");
        });

        try {
            lazy.get();
            fail("Should have thrown exception");
        } catch (TestException e) {
            assertEquals("Initialization failed", e.getMessage());
        }
    }

    @Test
    public void testLazyInitializer_OfAlreadyLazy() throws Exception {

        Throwables.Supplier<String, Exception> lazy1 = N.lazyInitialize(() -> "Value");

        Throwables.Supplier<String, Exception> lazy2 = N.lazyInitialize(lazy1);

        assertSame(lazy1, lazy2, "Should return the same instance");
    }

    @Test
    public void testRunnable_Unchecked_Success() {
        Throwables.Runnable<TestException> throwingRunnable = () -> {
        };

        com.landawn.abacus.util.function.Runnable unchecked = throwingRunnable.unchecked();
        unchecked.run();
    }

    @Test
    public void testRunnable_Unchecked_Exception() {
        Throwables.Runnable<TestException> throwingRunnable = () -> {
            throw new TestException("Test");
        };

        com.landawn.abacus.util.function.Runnable unchecked = throwingRunnable.unchecked();
        assertThrows(RuntimeException.class, () -> unchecked.run());
    }

    @Test
    public void testCallable_Unchecked_Success() {
        Throwables.Callable<String, TestException> throwingCallable = () -> "Success";

        com.landawn.abacus.util.function.Callable<String> unchecked = throwingCallable.unchecked();
        assertEquals("Success", unchecked.call());
    }

    @Test
    public void testCallable_Unchecked_Exception() {
        Throwables.Callable<String, TestException> throwingCallable = () -> {
            throw new TestException("Test");
        };

        com.landawn.abacus.util.function.Callable<String> unchecked = throwingCallable.unchecked();
        assertThrows(RuntimeException.class, () -> unchecked.call());
    }

    @Test
    public void testSupplier_Unchecked_Success() {
        Throwables.Supplier<String, TestException> throwingSupplier = () -> "Value";

        com.landawn.abacus.util.function.Supplier<String> unchecked = throwingSupplier.unchecked();
        assertEquals("Value", unchecked.get());
    }

    @Test
    public void testPredicate_Unchecked_Success() {
        Throwables.Predicate<String, TestException> throwingPredicate = s -> s.length() > 5;

        com.landawn.abacus.util.function.Predicate<String> unchecked = throwingPredicate.unchecked();
        assertTrue(unchecked.test("Hello World"));
        assertFalse(unchecked.test("Hi"));
    }

    @Test
    public void testPredicate_Negate() throws TestException {
        Throwables.Predicate<Integer, TestException> predicate = n -> n > 5;
        Throwables.Predicate<Integer, TestException> negated = predicate.negate();

        assertTrue(predicate.test(10));
        assertFalse(negated.test(10));

        assertFalse(predicate.test(3));
        assertTrue(negated.test(3));
    }

    @Test
    public void testFunction_Unchecked_Success() {
        Throwables.Function<String, Integer, TestException> throwingFunction = String::length;

        com.landawn.abacus.util.function.Function<String, Integer> unchecked = throwingFunction.unchecked();
        assertEquals(Integer.valueOf(5), unchecked.apply("Hello"));
    }

    @Test
    public void testBiFunction_Unchecked_Success() {
        Throwables.BiFunction<String, String, String, TestException> throwingBiFunction = (a, b) -> a + b;

        com.landawn.abacus.util.function.BiFunction<String, String, String> unchecked = throwingBiFunction.unchecked();
        assertEquals("HelloWorld", unchecked.apply("Hello", "World"));
    }

    @Test
    public void testConsumer_Unchecked_Success() {
        AtomicReference<String> result = new AtomicReference<>();
        Throwables.Consumer<String, TestException> throwingConsumer = result::set;

        com.landawn.abacus.util.function.Consumer<String> unchecked = throwingConsumer.unchecked();
        unchecked.accept("Test");
        assertEquals("Test", result.get());
    }

    @Test
    public void testBiConsumer_Unchecked_Success() {
        AtomicReference<String> result = new AtomicReference<>();
        Throwables.BiConsumer<String, String, TestException> throwingBiConsumer = (a, b) -> result.set(a + b);

        com.landawn.abacus.util.function.BiConsumer<String, String> unchecked = throwingBiConsumer.unchecked();
        unchecked.accept("Hello", "World");
        assertEquals("HelloWorld", result.get());
    }

    @Test
    public void testBiPredicate_Unchecked_Success() {
        Throwables.BiPredicate<String, String, TestException> throwingBiPredicate = (a, b) -> a.length() > b.length();

        com.landawn.abacus.util.function.BiPredicate<String, String> unchecked = throwingBiPredicate.unchecked();
        assertTrue(unchecked.test("Hello", "Hi"));
        assertFalse(unchecked.test("Hi", "Hello"));
    }

    @Test
    public void testPrimitiveFunctionalInterfaces() throws Exception {
        Throwables.BooleanSupplier<Exception> boolSupplier = () -> true;
        assertTrue(boolSupplier.getAsBoolean());

        Throwables.CharSupplier<Exception> charSupplier = () -> 'A';
        assertEquals('A', charSupplier.getAsChar());

        Throwables.ByteSupplier<Exception> byteSupplier = () -> (byte) 42;
        assertEquals(42, byteSupplier.getAsByte());

        Throwables.ShortSupplier<Exception> shortSupplier = () -> (short) 100;
        assertEquals(100, shortSupplier.getAsShort());

        Throwables.IntSupplier<Exception> intSupplier = () -> 999;
        assertEquals(999, intSupplier.getAsInt());

        Throwables.LongSupplier<Exception> longSupplier = () -> 123456789L;
        assertEquals(123456789L, longSupplier.getAsLong());

        Throwables.FloatSupplier<Exception> floatSupplier = () -> 3.14f;
        assertEquals(3.14f, floatSupplier.getAsFloat(), 0.001);

        Throwables.DoubleSupplier<Exception> doubleSupplier = () -> 2.71828;
        assertEquals(2.71828, doubleSupplier.getAsDouble(), 0.00001);
    }

    @Test
    public void testPrimitiveConsumers() throws Exception {
        AtomicBoolean boolResult = new AtomicBoolean();
        Throwables.BooleanConsumer<Exception> boolConsumer = boolResult::set;
        boolConsumer.accept(true);
        assertTrue(boolResult.get());

        AtomicReference<Character> charResult = new AtomicReference<>();
        Throwables.CharConsumer<Exception> charConsumer = charResult::set;
        charConsumer.accept('Z');
        assertEquals(Character.valueOf('Z'), charResult.get());

        AtomicInteger intResult = new AtomicInteger();
        Throwables.IntConsumer<Exception> intConsumer = intResult::set;
        intConsumer.accept(42);
        assertEquals(42, intResult.get());

        AtomicReference<Long> longResult = new AtomicReference<>();
        Throwables.LongConsumer<Exception> longConsumer = longResult::set;
        longConsumer.accept(999L);
        assertEquals(Long.valueOf(999L), longResult.get());

        AtomicReference<Double> doubleResult = new AtomicReference<>();
        Throwables.DoubleConsumer<Exception> doubleConsumer = doubleResult::set;
        doubleConsumer.accept(1.23);
        assertEquals(Double.valueOf(1.23), doubleResult.get());
    }

    @Test
    public void testPrimitivePredicates() throws Exception {
        Throwables.BooleanPredicate<Exception> boolPredicate = b -> b;
        assertTrue(boolPredicate.test(true));
        assertFalse(boolPredicate.test(false));

        Throwables.CharPredicate<Exception> charPredicate = c -> c >= 'A' && c <= 'Z';
        assertTrue(charPredicate.test('B'));
        assertFalse(charPredicate.test('a'));

        Throwables.IntPredicate<Exception> intPredicate = i -> i > 0;
        assertTrue(intPredicate.test(5));
        assertFalse(intPredicate.test(-1));

        Throwables.DoublePredicate<Exception> doublePredicate = d -> d > 0.5;
        assertTrue(doublePredicate.test(0.7));
        assertFalse(doublePredicate.test(0.3));
    }

    @Test
    public void testPrimitiveFunctions() throws Exception {
        Throwables.BooleanFunction<String, Exception> boolFunction = b -> b ? "yes" : "no";
        assertEquals("yes", boolFunction.apply(true));
        assertEquals("no", boolFunction.apply(false));

        Throwables.IntFunction<String, Exception> intFunction = i -> "Number: " + i;
        assertEquals("Number: 42", intFunction.apply(42));

        Throwables.IntToLongFunction<Exception> intToLong = i -> i * 1000L;
        assertEquals(5000L, intToLong.applyAsLong(5));

        Throwables.DoubleToIntFunction<Exception> doubleToInt = d -> (int) Math.round(d);
        assertEquals(3, doubleToInt.applyAsInt(3.14));
        assertEquals(4, doubleToInt.applyAsInt(3.7));
    }

    @Test
    public void testToXFunctions() throws Exception {
        Throwables.ToBooleanFunction<String, Exception> toBool = s -> s.equalsIgnoreCase("true");
        assertTrue(toBool.applyAsBoolean("TRUE"));
        assertFalse(toBool.applyAsBoolean("false"));

        Throwables.ToCharFunction<String, Exception> toChar = s -> s.charAt(0);
        assertEquals('H', toChar.applyAsChar("Hello"));

        Throwables.ToIntFunction<String, Exception> toInt = s -> s.length();
        assertEquals(5, toInt.applyAsInt("Hello"));

        Throwables.ToDoubleFunction<String, Exception> toDouble = Double::parseDouble;
        assertEquals(3.14, toDouble.applyAsDouble("3.14"), 0.001);
    }

    @Test
    public void testBinaryOperators() throws Exception {
        Throwables.BooleanBinaryOperator<Exception> boolOp = (a, b) -> a && b;
        assertTrue(boolOp.applyAsBoolean(true, true));
        assertFalse(boolOp.applyAsBoolean(true, false));

        Throwables.IntBinaryOperator<Exception> intOp = (a, b) -> a + b;
        assertEquals(7, intOp.applyAsInt(3, 4));

        Throwables.DoubleBinaryOperator<Exception> doubleOp = (a, b) -> a * b;
        assertEquals(6.0, doubleOp.applyAsDouble(2.0, 3.0), 0.001);
    }

    @Test
    public void testTernaryOperators() throws Exception {
        Throwables.TernaryOperator<String, Exception> ternaryOp = (a, b, c) -> a + b + c;
        assertEquals("ABC", ternaryOp.apply("A", "B", "C"));

        Throwables.IntTernaryOperator<Exception> intTernaryOp = (a, b, c) -> a + b + c;
        assertEquals(6, intTernaryOp.applyAsInt(1, 2, 3));

        Throwables.DoubleTernaryOperator<Exception> doubleTernaryOp = (a, b, c) -> a * b * c;
        assertEquals(24.0, doubleTernaryOp.applyAsDouble(2.0, 3.0, 4.0), 0.001);
    }

    @Test
    public void testObjPrimitiveConsumers() throws Exception {
        List<String> results = new ArrayList<>();
        Throwables.ObjIntConsumer<String, Exception> objIntConsumer = (s, i) -> results.add(s + ":" + i);
        objIntConsumer.accept("Value", 42);
        assertEquals("Value:42", results.get(0));

        results.clear();
        Throwables.ObjLongConsumer<String, Exception> objLongConsumer = (s, l) -> results.add(s + ":" + l);
        objLongConsumer.accept("Long", 999L);
        assertEquals("Long:999", results.get(0));

        results.clear();
        Throwables.ObjDoubleConsumer<String, Exception> objDoubleConsumer = (s, d) -> results.add(s + ":" + d);
        objDoubleConsumer.accept("Pi", 3.14);
        assertEquals("Pi:3.14", results.get(0));
    }

    @Test
    public void testObjPrimitiveFunctions() throws Exception {
        Throwables.ObjIntFunction<String, String, Exception> objIntFunc = (s, i) -> s + " times " + i;
        assertEquals("Hello times 3", objIntFunc.apply("Hello", 3));

        Throwables.ObjLongFunction<String, String, Exception> objLongFunc = (s, l) -> s + " at " + l;
        assertEquals("Event at 12345", objLongFunc.apply("Event", 12345L));

        Throwables.ObjDoubleFunction<String, String, Exception> objDoubleFunc = (s, d) -> String.format("%s: %.2f", s, d);
        assertEquals("Price: 9.99", objDoubleFunc.apply("Price", 9.99));
    }

    @Test
    public void testObjPrimitivePredicates() throws Exception {
        Throwables.ObjIntPredicate<String, Exception> objIntPred = (s, i) -> s.length() == i;
        assertTrue(objIntPred.test("Hello", 5));
        assertFalse(objIntPred.test("Hi", 5));

        Throwables.ObjLongPredicate<String, Exception> objLongPred = (s, l) -> s.hashCode() == l;
        String test = "test";
        assertTrue(objLongPred.test(test, test.hashCode()));

        Throwables.ObjDoublePredicate<String, Exception> objDoublePred = (s, d) -> Double.parseDouble(s) == d;
        assertTrue(objDoublePred.test("3.14", 3.14));
    }

    @Test
    public void testBiObjIntFunctions() throws Exception {
        List<String> results = new ArrayList<>();
        Throwables.BiObjIntConsumer<String, String, Exception> biObjIntConsumer = (s1, s2, i) -> results.add(s1 + "-" + s2 + ":" + i);
        biObjIntConsumer.accept("A", "B", 1);
        assertEquals("A-B:1", results.get(0));

        Throwables.BiObjIntFunction<String, String, String, Exception> biObjIntFunc = (s1, s2, i) -> s1 + s2 + i;
        assertEquals("Hello5", biObjIntFunc.apply("He", "llo", 5));

        Throwables.BiObjIntPredicate<String, String, Exception> biObjIntPred = (s1, s2, i) -> (s1.length() + s2.length()) == i;
        assertTrue(biObjIntPred.test("Hi", "Bye", 5));
        assertFalse(biObjIntPred.test("Hello", "World", 5));
    }

    @Test
    public void testIntObjFunctions() throws Exception {
        List<String> results = new ArrayList<>();
        Throwables.IntObjConsumer<String, Exception> intObjConsumer = (i, s) -> results.add(i + ":" + s);
        intObjConsumer.accept(1, "First");
        assertEquals("1:First", results.get(0));

        Throwables.IntObjFunction<String, String, Exception> intObjFunc = (i, s) -> s.substring(0, Math.min(i, s.length()));
        assertEquals("Hel", intObjFunc.apply(3, "Hello"));

        Throwables.IntObjPredicate<String, Exception> intObjPred = (i, s) -> s.length() > i;
        assertTrue(intObjPred.test(3, "Hello"));
        assertFalse(intObjPred.test(10, "Hello"));
    }

    @Test
    public void testNFunctions() throws Exception {
        Throwables.IntNFunction<Integer, Exception> sumFunc = args -> {
            int sum = 0;
            for (int i : args)
                sum += i;
            return sum;
        };
        assertEquals(Integer.valueOf(10), sumFunc.apply(1, 2, 3, 4));
        assertEquals(Integer.valueOf(0), sumFunc.apply());

        Throwables.DoubleNFunction<Double, Exception> avgFunc = args -> {
            if (args.length == 0)
                return 0.0;
            double sum = 0;
            for (double d : args)
                sum += d;
            return sum / args.length;
        };
        assertEquals(2.5, avgFunc.apply(1.0, 2.0, 3.0, 4.0), 0.001);

        Throwables.IntNFunction<Integer, Exception> multiplyFunc = args -> {
            int product = 1;
            for (int i : args)
                product *= i;
            return product;
        };
        Throwables.IntNFunction<String, Exception> composed = multiplyFunc.andThen(i -> "Result: " + i);
        assertEquals("Result: 24", composed.apply(2, 3, 4));
    }

    @Test
    public void testIndexedConsumers() throws Exception {
        List<String> results = new ArrayList<>();
        Throwables.IntCharConsumer<Exception> intCharConsumer = (idx, c) -> results.add(idx + ":" + c);
        intCharConsumer.accept(0, 'A');
        intCharConsumer.accept(1, 'B');
        assertEquals("0:A", results.get(0));
        assertEquals("1:B", results.get(1));

        results.clear();
        Throwables.IntIntConsumer<Exception> intIntConsumer = (idx, val) -> results.add("Index " + idx + " = " + val);
        intIntConsumer.accept(0, 100);
        assertEquals("Index 0 = 100", results.get(0));

        results.clear();
        Throwables.IntDoubleConsumer<Exception> intDoubleConsumer = (idx, d) -> results.add(String.format("%d: %.2f", idx, d));
        intDoubleConsumer.accept(0, 3.14159);
        assertEquals("0: 3.14", results.get(0));
    }

    @Test
    public void testEEFunctionalInterfaces() throws Exception {
        AtomicBoolean executed = new AtomicBoolean(false);
        Throwables.EE.Runnable<TestException, IOException> eeRunnable = () -> executed.set(true);
        eeRunnable.run();
        assertTrue(executed.get());

        Throwables.EE.Callable<String, TestException, IOException> eeCallable = () -> "EE Result";
        assertEquals("EE Result", eeCallable.call());

        Throwables.EE.Supplier<Integer, TestException, IOException> eeSupplier = () -> 42;
        assertEquals(Integer.valueOf(42), eeSupplier.get());

        Throwables.EE.Function<String, Integer, TestException, IOException> eeFunction = String::length;
        assertEquals(Integer.valueOf(5), eeFunction.apply("Hello"));

        Throwables.EE.BiFunction<String, String, String, TestException, IOException> eeBiFunction = (a, b) -> a + b;
        assertEquals("AB", eeBiFunction.apply("A", "B"));

        AtomicReference<String> result = new AtomicReference<>();
        Throwables.EE.Consumer<String, TestException, IOException> eeConsumer = result::set;
        eeConsumer.accept("Test");
        assertEquals("Test", result.get());

        Throwables.EE.Predicate<Integer, TestException, IOException> eePredicate = n -> n > 0;
        assertTrue(eePredicate.test(5));
        assertFalse(eePredicate.test(-1));
    }

    @Test
    public void testEEEFunctionalInterfaces() throws Exception {
        AtomicBoolean executed = new AtomicBoolean(false);
        Throwables.EEE.Runnable<TestException, IOException, RuntimeException> eeeRunnable = () -> executed.set(true);
        eeeRunnable.run();
        assertTrue(executed.get());

        Throwables.EEE.Callable<String, TestException, IOException, RuntimeException> eeeCallable = () -> "EEE Result";
        assertEquals("EEE Result", eeeCallable.call());

        Throwables.EEE.Function<String, Integer, TestException, IOException, RuntimeException> eeeFunction = s -> s.length();
        assertEquals(Integer.valueOf(7), eeeFunction.apply("Testing"));

        Throwables.EEE.TriFunction<String, String, String, String, TestException, IOException, RuntimeException> eeeTriFunction = (a, b, c) -> a + b + c;
        assertEquals("ABC", eeeTriFunction.apply("A", "B", "C"));

        List<String> results = new ArrayList<>();
        Throwables.EEE.Consumer<String, TestException, IOException, RuntimeException> eeeConsumer = results::add;
        eeeConsumer.accept("Item1");
        assertEquals(1, results.size());
        assertEquals("Item1", results.get(0));

        Throwables.EEE.BiPredicate<String, Integer, TestException, IOException, RuntimeException> eeeBiPredicate = (s, i) -> s.length() == i;
        assertTrue(eeeBiPredicate.test("Hello", 5));
        assertFalse(eeeBiPredicate.test("Hi", 5));
    }

    @Test
    public void testIterator_OfArrayRange_WithOptimizedCount() throws Exception {
        String[] array = { "A", "B", "C", "D", "E" };
        Throwables.Iterator<String, Exception> iter = Throwables.Iterator.of(array, 1, 4);

        assertEquals(3, iter.count());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testIterator_OfArrayRange_WithOptimizedAdvance() throws Exception {
        String[] array = { "A", "B", "C", "D", "E" };
        Throwables.Iterator<String, Exception> iter = Throwables.Iterator.of(array, 0, 5);

        iter.advance(2);
        assertEquals("C", iter.next());

        iter.advance(10);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testIterator_Defer_WithAdvanceAndCount() throws Exception {
        Throwables.Iterator<String, Exception> deferred = Throwables.Iterator.defer(() -> Throwables.Iterator.of("A", "B", "C", "D"));

        deferred.advance(1);
        assertEquals("B", deferred.next());

        assertEquals(2, deferred.count());
    }

    @Test
    public void testIterator_Defer_Close() throws Exception {
        AtomicBoolean closeCalled = new AtomicBoolean(false);

        Throwables.Iterator<String, Exception> innerIter = new Throwables.Iterator<>() {
            @Override
            public boolean hasNext() {
                return false;
            }

            @Override
            public String next() {
                throw new NoSuchElementException();
            }

            @Override
            protected void closeResource() {
                closeCalled.set(true);
            }
        };

        Throwables.Iterator<String, Exception> deferred = Throwables.Iterator.defer(() -> innerIter);
        deferred.close();

        assertFalse(closeCalled.get(), "Inner iterator should be closed");
    }

    @Test
    public void testStaticFactoryMethods() throws Exception {
        Throwables.IntObjConsumer<String, Exception> consumer = Throwables.IntObjConsumer.of((i, s) -> {
        });
        assertNotNull(consumer);

        Throwables.IntObjFunction<String, String, Exception> function = Throwables.IntObjFunction.of((i, s) -> s + i);
        assertEquals("Test1", function.apply(1, "Test"));

        Throwables.IntObjPredicate<String, Exception> predicate = Throwables.IntObjPredicate.of((i, s) -> s.length() > i);
        assertTrue(predicate.test(2, "Hello"));
        assertFalse(predicate.test(10, "Hi"));
    }

    @Test
    public void testLazyInitializer_ThreadSafety() throws Exception {
        final int threadCount = 100;
        final AtomicInteger initCount = new AtomicInteger(0);

        Throwables.Supplier<String, Exception> lazy = N.lazyInitialize(() -> {
            initCount.incrementAndGet();
            Thread.sleep(10);
            return "Initialized";
        });

        Thread[] threads = new Thread[threadCount];
        final String[] results = new String[threadCount];

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                try {
                    results[index] = lazy.get();
                } catch (Exception e) {
                    results[index] = "ERROR";
                }
            });
        }

        for (Thread t : threads) {
            t.start();
        }

        for (Thread t : threads) {
            t.join();
        }

        assertEquals(1, initCount.get(), "Initialization should happen only once");

        for (String result : results) {
            assertEquals("Initialized", result);
        }
    }

    @Test
    public void testPrimitiveBiPredicates() throws Exception {
        Throwables.BooleanBiPredicate<Exception> boolBiPred = (a, b) -> a || b;
        assertTrue(boolBiPred.test(true, false));
        assertTrue(boolBiPred.test(false, true));
        assertFalse(boolBiPred.test(false, false));

        Throwables.CharBiPredicate<Exception> charBiPred = (a, b) -> a < b;
        assertTrue(charBiPred.test('A', 'B'));
        assertFalse(charBiPred.test('Z', 'A'));

        Throwables.IntBiPredicate<Exception> intBiPred = (a, b) -> (a + b) > 10;
        assertTrue(intBiPred.test(7, 5));
        assertFalse(intBiPred.test(2, 3));

        Throwables.DoubleBiPredicate<Exception> doubleBiPred = (a, b) -> Math.abs(a - b) < 0.01;
        assertTrue(doubleBiPred.test(1.234, 1.235));
        assertFalse(doubleBiPred.test(1.0, 2.0));
    }

    @Test
    public void testPrimitiveTriPredicates() throws Exception {
        Throwables.BooleanTriPredicate<Exception> boolTriPred = (a, b, c) -> a && b && c;
        assertTrue(boolTriPred.test(true, true, true));
        assertFalse(boolTriPred.test(true, true, false));

        Throwables.IntTriPredicate<Exception> intTriPred = (a, b, c) -> (a + b + c) % 2 == 0;
        assertTrue(intTriPred.test(1, 2, 3));
        assertFalse(intTriPred.test(1, 2, 2));

        Throwables.DoubleTriPredicate<Exception> doubleTriPred = (a, b, c) -> (a * b * c) > 100.0;
        assertTrue(doubleTriPred.test(5.0, 5.0, 5.0));
        assertFalse(doubleTriPred.test(2.0, 3.0, 4.0));
    }

    @Test
    public void testPrimitiveBiFunctions() throws Exception {
        Throwables.BooleanBiFunction<String, Exception> boolBiFunc = (a, b) -> String.format("%s AND %s = %s", a, b, a && b);
        assertEquals("true AND false = false", boolBiFunc.apply(true, false));

        Throwables.CharBiFunction<String, Exception> charBiFunc = (a, b) -> "" + a + b;
        assertEquals("AB", charBiFunc.apply('A', 'B'));

        Throwables.IntBiFunction<String, Exception> intBiFunc = (a, b) -> "Sum: " + (a + b);
        assertEquals("Sum: 15", intBiFunc.apply(7, 8));

        Throwables.DoubleBiFunction<Double, Exception> doubleBiFunc = (a, b) -> Math.sqrt(a * a + b * b);
        assertEquals(5.0, doubleBiFunc.apply(3.0, 4.0), 0.001);
    }

    @Test
    public void testPrimitiveTriFunctions() throws Exception {
        Throwables.BooleanTriFunction<String, Exception> boolTriFunc = (a, b, c) -> String.format("(%s OR %s) AND %s", a, b, c);
        assertEquals("(true OR false) AND true", boolTriFunc.apply(true, false, true));

        Throwables.IntTriFunction<Integer, Exception> intTriFunc = (a, b, c) -> a * b + c;
        assertEquals(Integer.valueOf(23), intTriFunc.apply(4, 5, 3));

        Throwables.DoubleTriFunction<Double, Exception> doubleTriFunc = (a, b, c) -> (a + b + c) / 3.0;
        assertEquals(20.0, doubleTriFunc.apply(10.0, 20.0, 30.0), 0.001);
    }

    @Test
    public void testPrimitiveBiConsumers() throws Exception {
        List<String> results = new ArrayList<>();
        Throwables.BooleanBiConsumer<Exception> boolBiConsumer = (a, b) -> results.add(a + " XOR " + b + " = " + (a ^ b));
        boolBiConsumer.accept(true, false);
        assertEquals("true XOR false = true", results.get(0));

        results.clear();
        Throwables.CharBiConsumer<Exception> charBiConsumer = (a, b) -> results.add("Chars: " + a + ", " + b);
        charBiConsumer.accept('X', 'Y');
        assertEquals("Chars: X, Y", results.get(0));

        results.clear();
        Throwables.IntBiConsumer<Exception> intBiConsumer = (a, b) -> results.add("Product: " + (a * b));
        intBiConsumer.accept(6, 7);
        assertEquals("Product: 42", results.get(0));
    }

    @Test
    public void testPrimitiveTriConsumers() throws Exception {
        List<String> results = new ArrayList<>();
        Throwables.BooleanTriConsumer<Exception> boolTriConsumer = (a, b, c) -> results.add(String.format("%s, %s, %s", a, b, c));
        boolTriConsumer.accept(true, false, true);
        assertEquals("true, false, true", results.get(0));

        AtomicInteger sum = new AtomicInteger();
        Throwables.IntTriConsumer<Exception> intTriConsumer = (a, b, c) -> sum.set(a + b + c);
        intTriConsumer.accept(10, 20, 30);
        assertEquals(60, sum.get());

        AtomicReference<Double> product = new AtomicReference<>();
        Throwables.DoubleTriConsumer<Exception> doubleTriConsumer = (a, b, c) -> product.set(a * b * c);
        doubleTriConsumer.accept(2.0, 3.0, 4.0);
        assertEquals(24.0, product.get(), 0.001);
    }

    @Test
    public void testUnaryOperators() throws Exception {
        Throwables.UnaryOperator<String, Exception> stringOp = s -> s.toUpperCase();
        assertEquals("HELLO", stringOp.apply("hello"));

        Throwables.BooleanUnaryOperator<Exception> boolOp = b -> !b;
        assertTrue(boolOp.applyAsBoolean(false));
        assertFalse(boolOp.applyAsBoolean(true));

        Throwables.CharUnaryOperator<Exception> charOp = c -> Character.toLowerCase(c);
        assertEquals('a', charOp.applyAsChar('A'));

        Throwables.IntUnaryOperator<Exception> intOp = i -> i * i;
        assertEquals(25, intOp.applyAsInt(5));

        Throwables.DoubleUnaryOperator<Exception> doubleOp = d -> Math.sqrt(d);
        assertEquals(3.0, doubleOp.applyAsDouble(9.0), 0.001);
    }

    @Test
    public void testToBiFunctions() throws Exception {
        Throwables.ToIntBiFunction<String, String, Exception> toIntBiFunc = (a, b) -> a.length() + b.length();
        assertEquals(10, toIntBiFunc.applyAsInt("Hello", "World"));

        Throwables.ToLongBiFunction<String, Integer, Exception> toLongBiFunc = (s, i) -> s.hashCode() + i;
        assertEquals("Test".hashCode() + 100L, toLongBiFunc.applyAsLong("Test", 100));

        Throwables.ToDoubleBiFunction<Integer, Integer, Exception> toDoubleBiFunc = (a, b) -> (double) a / b;
        assertEquals(2.5, toDoubleBiFunc.applyAsDouble(5, 2), 0.001);
    }

    @Test
    public void testToTriFunctions() throws Exception {
        Throwables.ToIntTriFunction<String, String, String, Exception> toIntTriFunc = (a, b, c) -> a.length() + b.length() + c.length();
        assertEquals(10, toIntTriFunc.applyAsInt("Hi", "Hello", "Bye"));

        Throwables.ToLongTriFunction<Integer, Integer, Integer, Exception> toLongTriFunc = (a, b, c) -> (long) a * b * c;
        assertEquals(60L, toLongTriFunc.applyAsLong(3, 4, 5));

        Throwables.ToDoubleTriFunction<Double, Double, Double, Exception> toDoubleTriFunc = (a, b, c) -> (a + b + c) / 3.0;
        assertEquals(2.0, toDoubleTriFunc.applyAsDouble(1.0, 2.0, 3.0), 0.001);
    }

    @Test
    public void testObjBiIntFunctions() throws Exception {
        List<String> results = new ArrayList<>();
        Throwables.ObjBiIntConsumer<String, Exception> objBiIntConsumer = (s, i, j) -> results.add(s + "[" + i + "," + j + "]");
        objBiIntConsumer.accept("Array", 0, 5);
        assertEquals("Array[0,5]", results.get(0));

        Throwables.ObjBiIntFunction<String, String, Exception> objBiIntFunc = (s, i, j) -> s.substring(i, Math.min(j, s.length()));
        assertEquals("llo", objBiIntFunc.apply("Hello", 2, 5));

        Throwables.ObjBiIntPredicate<String, Exception> objBiIntPred = (s, i, j) -> s.length() >= i && s.length() <= j;
        assertTrue(objBiIntPred.test("Test", 3, 5));
        assertFalse(objBiIntPred.test("VeryLongString", 3, 5));
    }

    @Test
    public void testIntBiObjFunctions() throws Exception {
        List<String> results = new ArrayList<>();
        Throwables.IntBiObjConsumer<String, String, Exception> intBiObjConsumer = (i, s1, s2) -> results.add(i + ": " + s1 + " + " + s2);
        intBiObjConsumer.accept(1, "Hello", "World");
        assertEquals("1: Hello + World", results.get(0));

        Throwables.IntBiObjFunction<String, String, String, Exception> intBiObjFunc = (i, s1, s2) -> i + ": " + s1 + s2;
        assertEquals("42: AB", intBiObjFunc.apply(42, "A", "B"));

        Throwables.IntBiObjPredicate<String, String, Exception> intBiObjPred = (i, s1, s2) -> (s1.length() + s2.length()) == i;
        assertTrue(intBiObjPred.test(7, "Hi", "World"));
        assertFalse(intBiObjPred.test(10, "Hi", "World"));
    }

    @Test
    public void testBiIntObjFunctions() throws Exception {
        List<String> results = new ArrayList<>();
        Throwables.BiIntObjConsumer<String, Exception> biIntObjConsumer = (i, j, s) -> results.add("(" + i + "," + j + ") -> " + s);
        biIntObjConsumer.accept(3, 4, "Coordinates");
        assertEquals("(3,4) -> Coordinates", results.get(0));

        Throwables.BiIntObjFunction<String, String, Exception> biIntObjFunc = (i, j, s) -> s + " from " + i + " to " + j;
        assertEquals("Range from 1 to 10", biIntObjFunc.apply(1, 10, "Range"));

        Throwables.BiIntObjPredicate<int[], Exception> biIntObjPred = (i, j, arr) -> arr != null && i >= 0 && j < arr.length;
        assertTrue(biIntObjPred.test(0, 2, new int[] { 1, 2, 3 }));
        assertFalse(biIntObjPred.test(0, 5, new int[] { 1, 2, 3 }));
    }

    @Test
    public void testLongObjFunctions() throws Exception {
        AtomicReference<String> result = new AtomicReference<>();
        Throwables.LongObjConsumer<String, Exception> longObjConsumer = (l, s) -> result.set(s + " at " + l);
        longObjConsumer.accept(12345L, "Event");
        assertEquals("Event at 12345", result.get());

        Throwables.LongObjFunction<String, String, Exception> longObjFunc = (l, s) -> s + "_" + l;
        assertEquals("ID_999", longObjFunc.apply(999L, "ID"));

        Throwables.LongObjPredicate<String, Exception> longObjPred = (l, s) -> s.hashCode() == l;
        String test = "test";
        assertTrue(longObjPred.test(test.hashCode(), test));
    }

    @Test
    public void testDoubleObjFunctions() throws Exception {
        List<String> results = new ArrayList<>();
        Throwables.DoubleObjConsumer<String, Exception> doubleObjConsumer = (d, s) -> results.add(s + ": $" + String.format("%.2f", d));
        doubleObjConsumer.accept(19.99, "Price");
        assertEquals("Price: $19.99", results.get(0));

        Throwables.DoubleObjFunction<String, String, Exception> doubleObjFunc = (d, s) -> s + " * " + d + " = " + (d * s.length());
        assertEquals("Hello * 2.5 = 12.5", doubleObjFunc.apply(2.5, "Hello"));

        Throwables.DoubleObjPredicate<Double, Exception> doubleObjPred = (d1, d2) -> Math.abs(d1 - d2) < 0.001;
        assertTrue(doubleObjPred.test(3.14159, 3.14160));
        assertFalse(doubleObjPred.test(3.14, 3.15));
    }

    @Test
    public void testIntObjOperator() throws Exception {
        Throwables.IntObjOperator<List<Integer>, Exception> intObjOp = (val, list) -> {
            list.add(val);
            return list.size();
        };

        List<Integer> list = new ArrayList<>();
        assertEquals(1, intObjOp.applyAsInt(10, list));
        assertEquals(2, intObjOp.applyAsInt(20, list));
        assertEquals(Arrays.asList(10, 20), list);
    }

    @Test
    public void testExceptionWrapping() {
        try {
            Throwables.run(() -> {
                throw new IllegalArgumentException("Should not be wrapped");
            });
            fail("Should have thrown exception");
        } catch (IllegalArgumentException e) {
            assertEquals("Should not be wrapped", e.getMessage());
        }

        try {
            Throwables.run(() -> {
                throw new IOException("Should be wrapped");
            });
            fail("Should have thrown exception");
        } catch (RuntimeException e) {
            assertTrue(e.getCause() instanceof IOException);
            assertEquals("Should be wrapped", e.getCause().getMessage());
        }
    }

    @Test
    public void testNullHandling() throws Exception {

        Throwables.Iterator<String, Exception> iter = Throwables.Iterator.of("A", null, "C");
        assertEquals("A", iter.next());
        assertNull(iter.next());
        assertEquals("C", iter.next());

        iter = Throwables.Iterator.of("A", null, "B");
        Throwables.Iterator<String, Exception> filtered = iter.filter(s -> s == null || s.equals("A"));
        assertEquals("A", filtered.next());
        assertNull(filtered.next());
        assertFalse(filtered.hasNext());

        iter = Throwables.Iterator.of("Test");
        Throwables.Iterator<String, Exception> mapped = iter.map(s -> null);
        assertNull(mapped.next());
    }

    @Test
    public void testComplexChaining() throws Exception {
        Throwables.Iterator<Integer, Exception> iter = Throwables.Iterator.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

        List<String> result = iter.filter(n -> n % 2 == 0).map(n -> n * n).filter(n -> n > 10).map(n -> "Value: " + n).toList();

        assertEquals(Arrays.asList("Value: 16", "Value: 36", "Value: 64", "Value: 100"), result);
    }

    @Test
    public void testMemoryEfficiency() throws Exception {
        AtomicInteger mapCount = new AtomicInteger(0);

        Throwables.Iterator<Integer, Exception> iter = Throwables.Iterator.of(1, 2, 3, 4, 5);
        Throwables.Iterator<Integer, Exception> mapped = iter.map(n -> {
            mapCount.incrementAndGet();
            return n * 2;
        });

        assertEquals(0, mapCount.get());

        mapped.next();
        mapped.next();

        assertEquals(2, mapCount.get());
    }
}
