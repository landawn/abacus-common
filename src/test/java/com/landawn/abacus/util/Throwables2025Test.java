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
import com.landawn.abacus.util.Throwables.Iterator;
import com.landawn.abacus.util.Throwables.LazyInitializer;
import com.landawn.abacus.util.u.Nullable;

@Tag("2025")
public class Throwables2025Test extends TestBase {

    public static class TestException extends Exception {
        public TestException(String message) {
            super(message);
        }
    }

    public static class TestRuntimeException extends RuntimeException {
        public TestRuntimeException(String message) {
            super(message);
        }
    }

    @Test
    public void testRun_Success() {
        AtomicBoolean executed = new AtomicBoolean(false);
        Throwables.run(() -> executed.set(true));
        assertTrue(executed.get());
    }

    @Test
    public void testRun_ThrowsCheckedException() {
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
    public void testRun_NullCommand() {
        assertThrows(IllegalArgumentException.class, () -> Throwables.run(null));
    }

    @Test
    public void testRun_WithErrorHandler_Success() {
        AtomicBoolean executed = new AtomicBoolean(false);
        AtomicBoolean errorHandled = new AtomicBoolean(false);

        Throwables.run(() -> executed.set(true), e -> errorHandled.set(true));

        assertTrue(executed.get());
        assertFalse(errorHandled.get());
    }

    @Test
    public void testRun_WithErrorHandler_HandlesException() {
        AtomicReference<Throwable> capturedError = new AtomicReference<>();

        Throwables.run(() -> {
            throw new TestException("Test exception");
        }, capturedError::set);

        assertNotNull(capturedError.get());
        assertEquals("Test exception", capturedError.get().getMessage());
    }

    @Test
    public void testRun_WithErrorHandler_NullCommand() {
        assertThrows(IllegalArgumentException.class, () -> Throwables.run(null, e -> {
        }));
    }

    @Test
    public void testRun_WithErrorHandler_NullErrorHandler() {
        assertThrows(IllegalArgumentException.class, () -> Throwables.run(() -> {
        }, null));
    }

    @Test
    public void testCall_Success() {
        String result = Throwables.call(() -> "Success");
        assertEquals("Success", result);
    }

    @Test
    public void testCall_ThrowsCheckedException() {
        assertThrows(RuntimeException.class, () -> Throwables.call(() -> {
            throw new TestException("Test exception");
        }));
    }

    @Test
    public void testCall_NullCommand() {
        assertThrows(IllegalArgumentException.class, () -> Throwables.call(null));
    }

    @Test
    public void testCall_WithErrorFunction_Success() {
        String result = Throwables.call(() -> "Success", (java.util.function.Function<Throwable, String>) e -> "Error: " + e.getMessage());
        assertEquals("Success", result);
    }

    @Test
    public void testCall_WithErrorFunction_HandlesException() {
        String result = Throwables.call(() -> {
            throw new TestException("Test exception");
        }, (java.util.function.Function<Throwable, String>) e -> "Error: " + e.getMessage());

        assertEquals("Error: Test exception", result);
    }

    @Test
    public void testCall_WithErrorFunction_NullCommand() {
        assertThrows(IllegalArgumentException.class, () -> Throwables.call(null, (java.util.function.Function<Throwable, String>) e -> "Error"));
    }

    @Test
    public void testCall_WithErrorFunction_NullFunction() {
        assertThrows(IllegalArgumentException.class, () -> Throwables.call(() -> "Success", (java.util.function.Function<Throwable, String>) null));
    }

    @Test
    public void testCall_WithSupplier_Success() {
        String result = Throwables.call(() -> "Success", (java.util.function.Supplier<String>) () -> "Default");
        assertEquals("Success", result);
    }

    @Test
    public void testCall_WithSupplier_HandlesException() {
        String result = Throwables.call(() -> {
            throw new TestException("Test exception");
        }, (java.util.function.Supplier<String>) () -> "Default");

        assertEquals("Default", result);
    }

    @Test
    public void testCall_WithSupplier_NullCommand() {
        assertThrows(IllegalArgumentException.class, () -> Throwables.call(null, (java.util.function.Supplier<String>) () -> "Default"));
    }

    @Test
    public void testCall_WithSupplier_NullSupplier() {
        assertThrows(IllegalArgumentException.class, () -> Throwables.call(() -> "Success", (java.util.function.Supplier<String>) null));
    }

    @Test
    public void testCall_WithDefaultValue_Success() {
        Integer result = Throwables.call(() -> 42, 0);
        assertEquals(42, result);
    }

    @Test
    public void testCall_WithDefaultValue_HandlesException() {
        Integer result = Throwables.call(() -> {
            throw new TestException("Test exception");
        }, 0);

        assertEquals(0, result);
    }

    @Test
    public void testCall_WithDefaultValue_NullCommand() {
        assertThrows(IllegalArgumentException.class, () -> Throwables.call(null, 0));
    }

    @Test
    public void testCall_WithDefaultValue_NullDefaultValue() {
        Integer result = Throwables.call(() -> {
            throw new TestException("Test exception");
        }, (Integer) null);

        assertNull(result);
    }

    @Test
    public void testCall_WithPredicateAndSupplier_Success() {
        String result = Throwables.call(() -> "Success", e -> true, (java.util.function.Supplier<String>) () -> "Default");
        assertEquals("Success", result);
    }

    @Test
    public void testCall_WithPredicateAndSupplier_PredicateTrue() {
        String result = Throwables.call(() -> {
            throw new TestException("Test exception");
        }, e -> e instanceof TestException, (java.util.function.Supplier<String>) () -> "Handled");

        assertEquals("Handled", result);
    }

    @Test
    public void testCall_WithPredicateAndSupplier_PredicateFalse() {
        assertThrows(RuntimeException.class, () -> Throwables.call(() -> {
            throw new TestException("Test exception");
        }, e -> e instanceof IOException, (java.util.function.Supplier<String>) () -> "Handled"));
    }

    @Test
    public void testCall_WithPredicateAndSupplier_NullCommand() {
        assertThrows(IllegalArgumentException.class, () -> Throwables.call(null, e -> true, (java.util.function.Supplier<String>) () -> "Default"));
    }

    @Test
    public void testCall_WithPredicateAndSupplier_NullPredicate() {
        assertThrows(IllegalArgumentException.class, () -> Throwables.call(() -> "Success", null, (java.util.function.Supplier<String>) () -> "Default"));
    }

    @Test
    public void testCall_WithPredicateAndSupplier_NullSupplier() {
        assertThrows(IllegalArgumentException.class, () -> Throwables.call(() -> "Success", e -> true, (java.util.function.Supplier<String>) null));
    }

    @Test
    public void testCall_WithPredicateAndDefaultValue_Success() {
        Integer result = Throwables.call(() -> 42, e -> true, 0);
        assertEquals(42, result);
    }

    @Test
    public void testCall_WithPredicateAndDefaultValue_PredicateTrue() {
        Integer result = Throwables.call(() -> {
            throw new TestException("Test exception");
        }, e -> e instanceof TestException, 0);

        assertEquals(0, result);
    }

    @Test
    public void testCall_WithPredicateAndDefaultValue_PredicateFalse() {
        assertThrows(RuntimeException.class, () -> Throwables.call(() -> {
            throw new TestException("Test exception");
        }, e -> e instanceof IOException, 0));
    }

    @Test
    public void testCall_WithPredicateAndDefaultValue_NullCommand() {
        assertThrows(IllegalArgumentException.class, () -> Throwables.call(null, e -> true, 0));
    }

    @Test
    public void testCall_WithPredicateAndDefaultValue_NullPredicate() {
        assertThrows(IllegalArgumentException.class, () -> Throwables.call(() -> 42, null, 0));
    }

    @Test
    public void testIterator_Empty_HasNext() throws Exception {
        Iterator<String, Exception> iter = Iterator.empty();
        assertFalse(iter.hasNext());
    }

    @Test
    public void testIterator_Empty_Next() {
        Iterator<String, Exception> iter = Iterator.empty();
        assertThrows(NoSuchElementException.class, () -> iter.next());
    }

    @Test
    public void testIterator_Empty_Count() throws Exception {
        Iterator<String, Exception> iter = Iterator.empty();
        assertEquals(0, iter.count());
    }

    @Test
    public void testIterator_Empty_ToList() throws Exception {
        Iterator<String, Exception> iter = Iterator.empty();
        List<String> list = iter.toList();
        assertTrue(list.isEmpty());
    }

    @Test
    public void testIterator_Just_HasNext() throws Exception {
        Iterator<String, Exception> iter = Iterator.just("value");
        assertTrue(iter.hasNext());
        assertTrue(iter.hasNext());
    }

    @Test
    public void testIterator_Just_Next() throws Exception {
        Iterator<String, Exception> iter = Iterator.just("value");
        assertEquals("value", iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testIterator_Just_NextTwice() throws Exception {
        Iterator<String, Exception> iter = Iterator.just("value");
        iter.next();
        assertThrows(NoSuchElementException.class, () -> iter.next());
    }

    @Test
    public void testIterator_Just_NullValue() throws Exception {
        Iterator<String, Exception> iter = Iterator.just(null);
        assertTrue(iter.hasNext());
        assertNull(iter.next());
    }

    @Test
    public void testIterator_Just_Count() throws Exception {
        Iterator<String, Exception> iter = Iterator.just("value");
        assertEquals(1, iter.count());
    }

    @Test
    public void testIterator_OfVarargs_EmptyArray() throws Exception {
        Iterator<String, Exception> iter = Iterator.of();
        assertFalse(iter.hasNext());
    }

    @Test
    public void testIterator_OfVarargs_NullArray() throws Exception {
        Iterator<String, Exception> iter = Iterator.of((String[]) null);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testIterator_OfVarargs_SingleElement() throws Exception {
        Iterator<String, Exception> iter = Iterator.of("one");
        assertTrue(iter.hasNext());
        assertEquals("one", iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testIterator_OfVarargs_MultipleElements() throws Exception {
        Iterator<String, Exception> iter = Iterator.of("one", "two", "three");
        assertEquals("one", iter.next());
        assertEquals("two", iter.next());
        assertEquals("three", iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testIterator_OfVarargs_Count() throws Exception {
        Iterator<String, Exception> iter = Iterator.of("one", "two", "three");
        assertEquals(3, iter.count());
    }

    @Test
    public void testIterator_OfRange_ValidRange() throws Exception {
        String[] arr = { "zero", "one", "two", "three", "four" };
        Iterator<String, Exception> iter = Iterator.of(arr, 1, 4);
        assertEquals("one", iter.next());
        assertEquals("two", iter.next());
        assertEquals("three", iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testIterator_OfRange_EmptyRange() throws Exception {
        String[] arr = { "one", "two", "three" };
        Iterator<String, Exception> iter = Iterator.of(arr, 1, 1);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testIterator_OfRange_InvalidRange() {
        String[] arr = { "one", "two", "three" };
        assertThrows(IndexOutOfBoundsException.class, () -> Iterator.of(arr, 1, 5));
    }

    @Test
    public void testIterator_OfRange_NegativeFromIndex() {
        String[] arr = { "one", "two", "three" };
        assertThrows(IndexOutOfBoundsException.class, () -> Iterator.of(arr, -1, 2));
    }

    @Test
    public void testIterator_OfRange_FromGreaterThanTo() {
        String[] arr = { "one", "two", "three" };
        assertThrows(IndexOutOfBoundsException.class, () -> Iterator.of(arr, 2, 1));
    }

    @Test
    public void testIterator_OfRange_Advance() throws Exception {
        String[] arr = { "zero", "one", "two", "three", "four" };
        Iterator<String, Exception> iter = Iterator.of(arr, 0, 5);
        iter.advance(2);
        assertEquals("two", iter.next());
    }

    @Test
    public void testIterator_OfRange_AdvanceZero() throws Exception {
        String[] arr = { "zero", "one", "two" };
        Iterator<String, Exception> iter = Iterator.of(arr, 0, 3);
        iter.advance(0);
        assertEquals("zero", iter.next());
    }

    @Test
    public void testIterator_OfRange_AdvancePastEnd() throws Exception {
        String[] arr = { "zero", "one", "two" };
        Iterator<String, Exception> iter = Iterator.of(arr, 0, 3);
        iter.advance(10);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testIterator_OfRange_Count() throws Exception {
        String[] arr = { "zero", "one", "two", "three", "four" };
        Iterator<String, Exception> iter = Iterator.of(arr, 1, 4);
        assertEquals(3, iter.count());
    }

    @Test
    public void testIterator_OfIterable_Null() throws Exception {
        Iterator<String, Exception> iter = Iterator.of((Iterable<String>) null);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testIterator_OfIterable_EmptyList() throws Exception {
        Iterator<String, Exception> iter = Iterator.of(Collections.emptyList());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testIterator_OfIterable_NonEmptyList() throws Exception {
        List<String> list = Arrays.asList("one", "two", "three");
        Iterator<String, Exception> iter = Iterator.of(list);
        assertEquals("one", iter.next());
        assertEquals("two", iter.next());
        assertEquals("three", iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testIterator_OfJavaIterator_Null() throws Exception {
        Iterator<String, Exception> iter = Iterator.of((java.util.Iterator<String>) null);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testIterator_OfJavaIterator_EmptyIterator() throws Exception {
        Iterator<String, Exception> iter = Iterator.of(Collections.emptyIterator());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testIterator_OfJavaIterator_NonEmptyIterator() throws Exception {
        List<String> list = Arrays.asList("one", "two", "three");
        Iterator<String, Exception> iter = Iterator.of(list.iterator());
        assertEquals("one", iter.next());
        assertEquals("two", iter.next());
        assertEquals("three", iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testIterator_Defer_LazyInitialization() throws Exception {
        AtomicBoolean initialized = new AtomicBoolean(false);
        Iterator<String, Exception> iter = Iterator.defer(() -> {
            initialized.set(true);
            return Iterator.of("one", "two");
        });

        assertFalse(initialized.get(), "Should not be initialized until first access");
        assertTrue(iter.hasNext());
        assertTrue(initialized.get(), "Should be initialized after hasNext");
    }

    @Test
    public void testIterator_Defer_InitializedOnNext() throws Exception {
        AtomicBoolean initialized = new AtomicBoolean(false);
        Iterator<String, Exception> iter = Iterator.defer(() -> {
            initialized.set(true);
            return Iterator.of("one");
        });

        assertFalse(initialized.get());
        assertEquals("one", iter.next());
        assertTrue(initialized.get());
    }

    @Test
    public void testIterator_Defer_InitializedOnAdvance() throws Exception {
        AtomicBoolean initialized = new AtomicBoolean(false);
        Iterator<String, Exception> iter = Iterator.defer(() -> {
            initialized.set(true);
            return Iterator.of("one", "two", "three");
        });

        assertFalse(initialized.get());
        iter.advance(1);
        assertTrue(initialized.get());
    }

    @Test
    public void testIterator_Defer_InitializedOnCount() throws Exception {
        AtomicBoolean initialized = new AtomicBoolean(false);
        Iterator<String, Exception> iter = Iterator.defer(() -> {
            initialized.set(true);
            return Iterator.of("one", "two");
        });

        assertFalse(initialized.get());
        assertEquals(2, iter.count());
        assertTrue(initialized.get());
    }

    @Test
    public void testIterator_Defer_CloseWithoutInitialization() throws Exception {
        AtomicBoolean initialized = new AtomicBoolean(false);
        AtomicBoolean closed = new AtomicBoolean(false);

        Iterator<String, Exception> iter = Iterator.defer(() -> {
            initialized.set(true);
            return new Iterator<String, Exception>() {
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
                    closed.set(true);
                }
            };
        });

        iter.close();
        assertFalse(initialized.get(), "Should not initialize on close");
        assertFalse(closed.get(), "Underlying iterator should not be closed if not initialized");
    }

    @Test
    public void testIterator_Defer_CloseAfterInitialization() throws Exception {
        AtomicBoolean closed = new AtomicBoolean(false);

        Iterator<String, Exception> iter = Iterator.defer(() -> new Iterator<String, Exception>() {
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
                closed.set(true);
            }
        });

        iter.hasNext();
        iter.close();
        assertTrue(closed.get(), "Underlying iterator should be closed if initialized");
    }

    @Test
    public void testIterator_Defer_NullSupplier() {
        assertThrows(IllegalArgumentException.class, () -> Iterator.defer(null));
    }

    @Test
    public void testIterator_Defer_AdvanceZero() throws Exception {
        AtomicBoolean initialized = new AtomicBoolean(false);
        Iterator<String, Exception> iter = Iterator.defer(() -> {
            initialized.set(true);
            return Iterator.of("one");
        });

        iter.advance(0);
        assertFalse(initialized.get(), "Should not initialize on advance(0)");
    }

    @Test
    public void testIterator_ConcatVarargs_Empty() throws Exception {
        @SuppressWarnings("unchecked")
        Iterator<String, Exception> iter = Iterator.concat();
        assertFalse(iter.hasNext());
    }

    @Test
    public void testIterator_ConcatVarargs_SingleIterator() throws Exception {
        Iterator<String, Exception> iter = Iterator.concat(Iterator.of("one", "two"));
        assertEquals("one", iter.next());
        assertEquals("two", iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testIterator_ConcatVarargs_MultipleIterators() throws Exception {
        Iterator<String, Exception> iter = Iterator.concat(Iterator.of("one", "two"), Iterator.of("three"), Iterator.of("four", "five"));

        assertEquals("one", iter.next());
        assertEquals("two", iter.next());
        assertEquals("three", iter.next());
        assertEquals("four", iter.next());
        assertEquals("five", iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testIterator_ConcatVarargs_WithEmptyIterators() throws Exception {
        Iterator<String, Exception> iter = Iterator.concat(Iterator.empty(), Iterator.of("one"), Iterator.empty(), Iterator.of("two"), Iterator.empty());

        assertEquals("one", iter.next());
        assertEquals("two", iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testIterator_ConcatCollection_Null() throws Exception {
        Iterator<String, Exception> iter = Iterator.concat((List<Iterator<String, Exception>>) null);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testIterator_ConcatCollection_EmptyCollection() throws Exception {
        Iterator<String, Exception> iter = Iterator.concat(Collections.emptyList());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testIterator_ConcatCollection_MultipleIterators() throws Exception {
        List<Iterator<String, Exception>> iterators = Arrays.asList(Iterator.of("one", "two"), Iterator.of("three"), Iterator.of("four", "five"));

        Iterator<String, Exception> iter = Iterator.concat(iterators);

        assertEquals("one", iter.next());
        assertEquals("two", iter.next());
        assertEquals("three", iter.next());
        assertEquals("four", iter.next());
        assertEquals("five", iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testIterator_ConcatCollection_HasNextMultipleCalls() throws Exception {
        List<Iterator<String, Exception>> iterators = Arrays.asList(Iterator.of("one"), Iterator.of("two"));
        Iterator<String, Exception> iter = Iterator.concat(iterators);

        assertTrue(iter.hasNext());
        assertTrue(iter.hasNext());
        assertEquals("one", iter.next());
    }

    @Test
    public void testIterator_OfLines_Null() throws Exception {
        Iterator<String, IOException> iter = Iterator.ofLines(null);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testIterator_OfLines_EmptyReader() throws Exception {
        StringReader reader = new StringReader("");
        Iterator<String, IOException> iter = Iterator.ofLines(reader);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testIterator_OfLines_SingleLine() throws Exception {
        StringReader reader = new StringReader("line1");
        Iterator<String, IOException> iter = Iterator.ofLines(reader);
        assertTrue(iter.hasNext());
        assertEquals("line1", iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testIterator_OfLines_MultipleLines() throws Exception {
        StringReader reader = new StringReader("line1\nline2\nline3");
        Iterator<String, IOException> iter = Iterator.ofLines(reader);

        assertEquals("line1", iter.next());
        assertEquals("line2", iter.next());
        assertEquals("line3", iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testIterator_OfLines_HasNextMultipleCalls() throws Exception {
        StringReader reader = new StringReader("line1\nline2");
        Iterator<String, IOException> iter = Iterator.ofLines(reader);

        assertTrue(iter.hasNext());
        assertTrue(iter.hasNext());
        assertEquals("line1", iter.next());
    }

    @Test
    public void testIterator_OfLines_NextWithoutHasNext() throws Exception {
        StringReader reader = new StringReader("line1");
        Iterator<String, IOException> iter = Iterator.ofLines(reader);
        assertEquals("line1", iter.next());
    }

    @Test
    public void testIterator_OfLines_NextOnEmptyThrows() throws Exception {
        StringReader reader = new StringReader("");
        Iterator<String, IOException> iter = Iterator.ofLines(reader);
        assertThrows(NoSuchElementException.class, () -> iter.next());
    }

    @Test
    public void testIterator_OfLines_Close() throws Exception {
        StringReader reader = new StringReader("line1\nline2\nline3");
        Iterator<String, IOException> iter = Iterator.ofLines(reader);
        iter.next();
        iter.close();
        iter.close();
    }

    @Test
    public void testIterator_HasNext_MultipleCallsSameResult() throws Exception {
        Iterator<String, Exception> iter = Iterator.of("one", "two");
        assertTrue(iter.hasNext());
        assertTrue(iter.hasNext());
        assertTrue(iter.hasNext());
        assertEquals("one", iter.next());
    }

    @Test
    public void testIterator_Next_WithoutHasNext() throws Exception {
        Iterator<String, Exception> iter = Iterator.of("one");
        assertEquals("one", iter.next());
    }

    @Test
    public void testIterator_Next_AfterExhausted() throws Exception {
        Iterator<String, Exception> iter = Iterator.of("one");
        iter.next();
        assertThrows(NoSuchElementException.class, () -> iter.next());
    }

    @Test
    public void testIterator_Advance_PositiveN() throws Exception {
        Iterator<String, Exception> iter = Iterator.of("one", "two", "three", "four");
        iter.advance(2);
        assertEquals("three", iter.next());
    }

    @Test
    public void testIterator_Advance_Zero() throws Exception {
        Iterator<String, Exception> iter = Iterator.of("one", "two");
        iter.advance(0);
        assertEquals("one", iter.next());
    }

    @Test
    public void testIterator_Advance_Negative() throws Exception {
        Iterator<String, Exception> iter = Iterator.of("one", "two");
        iter.advance(-1);
        assertEquals("one", iter.next());
    }

    @Test
    public void testIterator_Advance_PastEnd() throws Exception {
        Iterator<String, Exception> iter = Iterator.of("one", "two");
        iter.advance(10);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testIterator_Advance_ExactlyToEnd() throws Exception {
        Iterator<String, Exception> iter = Iterator.of("one", "two", "three");
        iter.advance(3);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testIterator_Count_Empty() throws Exception {
        Iterator<String, Exception> iter = Iterator.empty();
        assertEquals(0, iter.count());
    }

    @Test
    public void testIterator_Count_NonEmpty() throws Exception {
        Iterator<String, Exception> iter = Iterator.of("one", "two", "three");
        assertEquals(3, iter.count());
    }

    @Test
    public void testIterator_Count_AfterPartialConsumption() throws Exception {
        Iterator<String, Exception> iter = Iterator.of("one", "two", "three", "four");
        iter.next();
        iter.next();
        assertEquals(2, iter.count());
    }

    @Test
    public void testIterator_Count_AfterFullConsumption() throws Exception {
        Iterator<String, Exception> iter = Iterator.of("one", "two");
        iter.count();
        assertEquals(0, iter.count());
    }

    @Test
    public void testIterator_Close_MultipleCalls() throws Exception {
        AtomicInteger closeCount = new AtomicInteger(0);

        Iterator<String, Exception> iter = new Iterator<>() {
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
    public void testIterator_Close_CallsCloseResource() throws Exception {
        AtomicBoolean closed = new AtomicBoolean(false);

        Iterator<String, Exception> iter = new Iterator<>() {
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
                closed.set(true);
            }
        };

        iter.close();
        assertTrue(closed.get());
    }

    @Test
    public void testIterator_Filter_AllMatch() throws Exception {
        Iterator<Integer, Exception> iter = Iterator.of(2, 4, 6, 8);
        Iterator<Integer, Exception> filtered = iter.filter(n -> n % 2 == 0);

        assertEquals(2, filtered.next());
        assertEquals(4, filtered.next());
        assertEquals(6, filtered.next());
        assertEquals(8, filtered.next());
        assertFalse(filtered.hasNext());
    }

    @Test
    public void testIterator_Filter_SomeMatch() throws Exception {
        Iterator<Integer, Exception> iter = Iterator.of(1, 2, 3, 4, 5, 6);
        Iterator<Integer, Exception> filtered = iter.filter(n -> n % 2 == 0);

        assertEquals(2, filtered.next());
        assertEquals(4, filtered.next());
        assertEquals(6, filtered.next());
        assertFalse(filtered.hasNext());
    }

    @Test
    public void testIterator_Filter_NoneMatch() throws Exception {
        Iterator<Integer, Exception> iter = Iterator.of(1, 3, 5, 7);
        Iterator<Integer, Exception> filtered = iter.filter(n -> n % 2 == 0);

        assertFalse(filtered.hasNext());
    }

    @Test
    public void testIterator_Filter_Empty() throws Exception {
        Iterator<Integer, Exception> iter = Iterator.empty();
        Iterator<Integer, Exception> filtered = iter.filter(n -> true);

        assertFalse(filtered.hasNext());
    }

    @Test
    public void testIterator_Filter_HasNextMultipleCalls() throws Exception {
        Iterator<Integer, Exception> iter = Iterator.of(1, 2, 3);
        Iterator<Integer, Exception> filtered = iter.filter(n -> n == 2);

        assertTrue(filtered.hasNext());
        assertTrue(filtered.hasNext());
        assertEquals(2, filtered.next());
    }

    @Test
    public void testIterator_Map_Transform() throws Exception {
        Iterator<Integer, Exception> iter = Iterator.of(1, 2, 3);
        Iterator<String, Exception> mapped = iter.map(n -> "num" + n);

        assertEquals("num1", mapped.next());
        assertEquals("num2", mapped.next());
        assertEquals("num3", mapped.next());
        assertFalse(mapped.hasNext());
    }

    @Test
    public void testIterator_Map_Empty() throws Exception {
        Iterator<Integer, Exception> iter = Iterator.empty();
        Iterator<String, Exception> mapped = iter.map(n -> "num" + n);

        assertFalse(mapped.hasNext());
    }

    @Test
    public void testIterator_Map_ToNull() throws Exception {
        Iterator<Integer, Exception> iter = Iterator.of(1, 2);
        Iterator<String, Exception> mapped = iter.map(n -> null);

        assertNull(mapped.next());
        assertNull(mapped.next());
        assertFalse(mapped.hasNext());
    }

    @Test
    public void testIterator_Map_ChangeType() throws Exception {
        Iterator<String, Exception> iter = Iterator.of("1", "2", "3");
        Iterator<Integer, Exception> mapped = iter.map(Integer::parseInt);

        assertEquals(1, mapped.next());
        assertEquals(2, mapped.next());
        assertEquals(3, mapped.next());
        assertFalse(mapped.hasNext());
    }

    @Test
    public void testIterator_First_NonEmpty() throws Exception {
        Iterator<String, Exception> iter = Iterator.of("one", "two", "three");
        Nullable<String> first = iter.first();

        assertTrue(first.isPresent());
        assertEquals("one", first.get());
    }

    @Test
    public void testIterator_First_Empty() throws Exception {
        Iterator<String, Exception> iter = Iterator.empty();
        Nullable<String> first = iter.first();

        assertFalse(first.isPresent());
    }

    @Test
    public void testIterator_First_NullElement() throws Exception {
        Iterator<String, Exception> iter = Iterator.just(null);
        Nullable<String> first = iter.first();

        assertTrue(first.isPresent());
        assertNull(first.get());
    }

    @Test
    public void testIterator_FirstNonNull_AllNonNull() throws Exception {
        Iterator<String, Exception> iter = Iterator.of("one", "two", "three");
        u.Optional<String> first = iter.firstNonNull();

        assertTrue(first.isPresent());
        assertEquals("one", first.get());
    }

    @Test
    public void testIterator_FirstNonNull_SomeNull() throws Exception {
        Iterator<String, Exception> iter = Iterator.of(null, null, "three", "four");
        u.Optional<String> first = iter.firstNonNull();

        assertTrue(first.isPresent());
        assertEquals("three", first.get());
    }

    @Test
    public void testIterator_FirstNonNull_AllNull() throws Exception {
        Iterator<String, Exception> iter = Iterator.of(null, null, null);
        u.Optional<String> first = iter.firstNonNull();

        assertFalse(first.isPresent());
    }

    @Test
    public void testIterator_FirstNonNull_Empty() throws Exception {
        Iterator<String, Exception> iter = Iterator.empty();
        u.Optional<String> first = iter.firstNonNull();

        assertFalse(first.isPresent());
    }

    @Test
    public void testIterator_Last_NonEmpty() throws Exception {
        Iterator<String, Exception> iter = Iterator.of("one", "two", "three");
        Nullable<String> last = iter.last();

        assertTrue(last.isPresent());
        assertEquals("three", last.get());
    }

    @Test
    public void testIterator_Last_Empty() throws Exception {
        Iterator<String, Exception> iter = Iterator.empty();
        Nullable<String> last = iter.last();

        assertFalse(last.isPresent());
    }

    @Test
    public void testIterator_Last_SingleElement() throws Exception {
        Iterator<String, Exception> iter = Iterator.just("one");
        Nullable<String> last = iter.last();

        assertTrue(last.isPresent());
        assertEquals("one", last.get());
    }

    @Test
    public void testIterator_Last_NullElement() throws Exception {
        Iterator<String, Exception> iter = Iterator.of("one", null);
        Nullable<String> last = iter.last();

        assertTrue(last.isPresent());
        assertNull(last.get());
    }

    @Test
    public void testIterator_ToArray_NoArg_Empty() throws Exception {
        Iterator<String, Exception> iter = Iterator.empty();
        Object[] array = iter.toArray();

        assertEquals(0, array.length);
    }

    @Test
    public void testIterator_ToArray_NoArg_NonEmpty() throws Exception {
        Iterator<String, Exception> iter = Iterator.of("one", "two", "three");
        Object[] array = iter.toArray();

        assertEquals(3, array.length);
        assertEquals("one", array[0]);
        assertEquals("two", array[1]);
        assertEquals("three", array[2]);
    }

    @Test
    public void testIterator_ToArray_WithArg_Empty() throws Exception {
        Iterator<String, Exception> iter = Iterator.empty();
        String[] array = iter.toArray(new String[0]);

        assertEquals(0, array.length);
    }

    @Test
    public void testIterator_ToArray_WithArg_NonEmpty() throws Exception {
        Iterator<String, Exception> iter = Iterator.of("one", "two", "three");
        String[] array = iter.toArray(new String[0]);

        assertEquals(3, array.length);
        assertEquals("one", array[0]);
        assertEquals("two", array[1]);
        assertEquals("three", array[2]);
    }

    @Test
    public void testIterator_ToArray_WithArg_LargerArray() throws Exception {
        Iterator<String, Exception> iter = Iterator.of("one", "two");
        String[] array = iter.toArray(new String[5]);

        assertEquals(5, array.length);
        assertEquals("one", array[0]);
        assertEquals("two", array[1]);
        assertNull(array[2]);
    }

    @Test
    public void testIterator_ToList_Empty() throws Exception {
        Iterator<String, Exception> iter = Iterator.empty();
        List<String> list = iter.toList();

        assertTrue(list.isEmpty());
    }

    @Test
    public void testIterator_ToList_NonEmpty() throws Exception {
        Iterator<String, Exception> iter = Iterator.of("one", "two", "three");
        List<String> list = iter.toList();

        assertEquals(3, list.size());
        assertEquals("one", list.get(0));
        assertEquals("two", list.get(1));
        assertEquals("three", list.get(2));
    }

    @Test
    public void testIterator_ToList_WithNullElements() throws Exception {
        Iterator<String, Exception> iter = Iterator.of("one", null, "three");
        List<String> list = iter.toList();

        assertEquals(3, list.size());
        assertEquals("one", list.get(0));
        assertNull(list.get(1));
        assertEquals("three", list.get(2));
    }

    @Test
    public void testIterator_ForEachRemaining_Empty() throws Exception {
        Iterator<String, Exception> iter = Iterator.empty();
        List<String> collected = new ArrayList<>();

        iter.forEachRemaining(collected::add);

        assertTrue(collected.isEmpty());
    }

    @Test
    public void testIterator_ForEachRemaining_NonEmpty() throws Exception {
        Iterator<String, Exception> iter = Iterator.of("one", "two", "three");
        List<String> collected = new ArrayList<>();

        iter.forEachRemaining(collected::add);

        assertEquals(Arrays.asList("one", "two", "three"), collected);
    }

    @Test
    public void testIterator_ForEachRemaining_PartiallyConsumed() throws Exception {
        Iterator<String, Exception> iter = Iterator.of("one", "two", "three");
        iter.next();

        List<String> collected = new ArrayList<>();
        iter.forEachRemaining(collected::add);

        assertEquals(Arrays.asList("two", "three"), collected);
    }

    @Test
    public void testIterator_ForeachRemaining_Empty() throws Exception {
        Iterator<String, Exception> iter = Iterator.empty();
        List<String> collected = new ArrayList<>();

        iter.foreachRemaining(collected::add);

        assertTrue(collected.isEmpty());
    }

    @Test
    public void testIterator_ForeachRemaining_NonEmpty() throws Exception {
        Iterator<String, Exception> iter = Iterator.of("one", "two", "three");
        List<String> collected = new ArrayList<>();

        iter.foreachRemaining(collected::add);

        assertEquals(Arrays.asList("one", "two", "three"), collected);
    }

    @Test
    public void testIterator_ForeachRemaining_CanThrowException() {
        Iterator<String, IOException> iter = Iterator.of("one", "two");

        assertThrows(IOException.class, () -> iter.foreachRemaining(s -> {
            throw new IOException("Test");
        }));
    }

    @Test
    public void testIterator_ForeachIndexed_Empty() throws Exception {
        Iterator<String, Exception> iter = Iterator.empty();
        List<Pair<Integer, String>> collected = new ArrayList<>();

        iter.foreachIndexed((idx, val) -> collected.add(Pair.of(idx, val)));

        assertTrue(collected.isEmpty());
    }

    @Test
    public void testIterator_ForeachIndexed_NonEmpty() throws Exception {
        Iterator<String, Exception> iter = Iterator.of("one", "two", "three");
        List<Pair<Integer, String>> collected = new ArrayList<>();

        iter.foreachIndexed((idx, val) -> collected.add(Pair.of(idx, val)));

        assertEquals(3, collected.size());
        assertEquals(0, collected.get(0).left());
        assertEquals("one", collected.get(0).right());
        assertEquals(1, collected.get(1).left());
        assertEquals("two", collected.get(1).right());
        assertEquals(2, collected.get(2).left());
        assertEquals("three", collected.get(2).right());
    }

    @Test
    public void testIterator_ForeachIndexed_PartiallyConsumed() throws Exception {
        Iterator<String, Exception> iter = Iterator.of("one", "two", "three");
        iter.next();

        List<Pair<Integer, String>> collected = new ArrayList<>();
        iter.foreachIndexed((idx, val) -> collected.add(Pair.of(idx, val)));

        assertEquals(2, collected.size());
        assertEquals(0, collected.get(0).left());
        assertEquals("two", collected.get(0).right());
        assertEquals(1, collected.get(1).left());
        assertEquals("three", collected.get(1).right());
    }

    @Test
    public void testRunnable_Unchecked_Success() {
        AtomicBoolean executed = new AtomicBoolean(false);
        Throwables.Runnable<Exception> throwableRunnable = () -> executed.set(true);

        com.landawn.abacus.util.function.Runnable unchecked = throwableRunnable.unchecked();
        unchecked.run();

        assertTrue(executed.get());
    }

    @Test
    public void testRunnable_Unchecked_ThrowsRuntimeException() {
        Throwables.Runnable<Exception> throwableRunnable = () -> {
            throw new TestException("Test exception");
        };

        com.landawn.abacus.util.function.Runnable unchecked = throwableRunnable.unchecked();

        assertThrows(RuntimeException.class, unchecked::run);
    }

    @Test
    public void testCallable_Unchecked_Success() {
        Throwables.Callable<String, Exception> throwableCallable = () -> "result";

        com.landawn.abacus.util.function.Callable<String> unchecked = throwableCallable.unchecked();
        String result = unchecked.call();

        assertEquals("result", result);
    }

    @Test
    public void testCallable_Unchecked_ThrowsRuntimeException() {
        Throwables.Callable<String, Exception> throwableCallable = () -> {
            throw new TestException("Test exception");
        };

        com.landawn.abacus.util.function.Callable<String> unchecked = throwableCallable.unchecked();

        assertThrows(RuntimeException.class, unchecked::call);
    }

    @Test
    public void testLazyInitializer_Of_NullSupplier() {
        assertThrows(IllegalArgumentException.class, () -> LazyInitializer.of(null));
    }

    @Test
    public void testLazyInitializer_Of_AlreadyLazyInitializer() throws Exception {
        LazyInitializer<String, Exception> original = LazyInitializer.of(() -> "value");
        LazyInitializer<String, Exception> wrapped = LazyInitializer.of(original);

        assertSame(original, wrapped);
    }

    @Test
    public void testLazyInitializer_Of_CreatesNewInstance() throws Exception {
        Throwables.Supplier<String, Exception> supplier = () -> "value";
        LazyInitializer<String, Exception> lazy = LazyInitializer.of(supplier);

        assertNotNull(lazy);
    }

    @Test
    public void testLazyInitializer_Get_CalledOnce() throws Exception {
        AtomicInteger callCount = new AtomicInteger(0);
        LazyInitializer<String, Exception> lazy = LazyInitializer.of(() -> {
            callCount.incrementAndGet();
            return "value";
        });

        assertEquals("value", lazy.get());
        assertEquals("value", lazy.get());
        assertEquals("value", lazy.get());

        assertEquals(1, callCount.get(), "Supplier should only be called once");
    }

    @Test
    public void testLazyInitializer_Get_ThreadSafe() throws Exception {
        AtomicInteger callCount = new AtomicInteger(0);
        LazyInitializer<String, Exception> lazy = LazyInitializer.of(() -> {
            callCount.incrementAndGet();
            Thread.sleep(10);
            return "value";
        });

        Thread t1 = new Thread(() -> {
            try {
                lazy.get();
            } catch (Exception e) {
                fail("Unexpected exception");
            }
        });
        Thread t2 = new Thread(() -> {
            try {
                lazy.get();
            } catch (Exception e) {
                fail("Unexpected exception");
            }
        });

        t1.start();
        t2.start();
        t1.join();
        t2.join();

        assertEquals(1, callCount.get(), "Supplier should only be called once even with concurrent access");
    }

    @Test
    public void testLazyInitializer_Get_ReturnsNull() throws Exception {
        LazyInitializer<String, Exception> lazy = LazyInitializer.of(() -> null);
        assertNull(lazy.get());
        assertNull(lazy.get());
    }

    @Test
    public void testLazyInitializer_Get_ThrowsException() {
        LazyInitializer<String, IOException> lazy = LazyInitializer.of(() -> {
            throw new IOException("Test exception");
        });

        assertThrows(IOException.class, lazy::get);
    }

    @Test
    public void testLazyInitializer_Get_ReturnsSameInstance() throws Exception {
        LazyInitializer<Object, Exception> lazy = LazyInitializer.of(Object::new);

        Object first = lazy.get();
        Object second = lazy.get();

        assertSame(first, second, "Should return the same instance");
    }

    @Test
    public void testSupplier_Unchecked_Success() {
        Throwables.Supplier<String, Exception> throwableSupplier = () -> "result";

        com.landawn.abacus.util.function.Supplier<String> unchecked = throwableSupplier.unchecked();
        String result = unchecked.get();

        assertEquals("result", result);
    }

    @Test
    public void testSupplier_Unchecked_ThrowsRuntimeException() {
        Throwables.Supplier<String, Exception> throwableSupplier = () -> {
            throw new TestException("Test exception");
        };

        com.landawn.abacus.util.function.Supplier<String> unchecked = throwableSupplier.unchecked();

        assertThrows(RuntimeException.class, unchecked::get);
    }

    @Test
    public void testPredicate_Negate_TrueBecomesFlase() throws Exception {
        Throwables.Predicate<Integer, Exception> isEven = n -> n % 2 == 0;
        Throwables.Predicate<Integer, Exception> isOdd = isEven.negate();

        assertTrue(isOdd.test(1));
        assertFalse(isOdd.test(2));
        assertTrue(isOdd.test(3));
        assertFalse(isOdd.test(4));
    }

    @Test
    public void testPredicate_Negate_FalseBecomesTrue() throws Exception {
        Throwables.Predicate<String, Exception> isEmpty = String::isEmpty;
        Throwables.Predicate<String, Exception> isNotEmpty = isEmpty.negate();

        assertFalse(isNotEmpty.test(""));
        assertTrue(isNotEmpty.test("hello"));
    }

    @Test
    public void testPredicate_Unchecked_Success() {
        Throwables.Predicate<Integer, Exception> throwablePredicate = n -> n > 0;

        com.landawn.abacus.util.function.Predicate<Integer> unchecked = throwablePredicate.unchecked();

        assertTrue(unchecked.test(5));
        assertFalse(unchecked.test(-1));
    }

    @Test
    public void testPredicate_Unchecked_ThrowsRuntimeException() {
        Throwables.Predicate<Integer, Exception> throwablePredicate = n -> {
            throw new TestException("Test exception");
        };

        com.landawn.abacus.util.function.Predicate<Integer> unchecked = throwablePredicate.unchecked();

        assertThrows(RuntimeException.class, () -> unchecked.test(1));
    }

    @Test
    public void testBiPredicate_Unchecked_Success() {
        Throwables.BiPredicate<Integer, Integer, Exception> throwableBiPredicate = (a, b) -> a > b;

        com.landawn.abacus.util.function.BiPredicate<Integer, Integer> unchecked = throwableBiPredicate.unchecked();

        assertTrue(unchecked.test(5, 3));
        assertFalse(unchecked.test(2, 4));
    }

    @Test
    public void testBiPredicate_Unchecked_ThrowsRuntimeException() {
        Throwables.BiPredicate<Integer, Integer, Exception> throwableBiPredicate = (a, b) -> {
            throw new TestException("Test exception");
        };

        com.landawn.abacus.util.function.BiPredicate<Integer, Integer> unchecked = throwableBiPredicate.unchecked();

        assertThrows(RuntimeException.class, () -> unchecked.test(1, 2));
    }

    @Test
    public void testFunction_Unchecked_Success() {
        Throwables.Function<Integer, String, Exception> throwableFunction = n -> "Number: " + n;

        com.landawn.abacus.util.function.Function<Integer, String> unchecked = throwableFunction.unchecked();
        String result = unchecked.apply(42);

        assertEquals("Number: 42", result);
    }

    @Test
    public void testFunction_Unchecked_ThrowsRuntimeException() {
        Throwables.Function<Integer, String, Exception> throwableFunction = n -> {
            throw new TestException("Test exception");
        };

        com.landawn.abacus.util.function.Function<Integer, String> unchecked = throwableFunction.unchecked();

        assertThrows(RuntimeException.class, () -> unchecked.apply(1));
    }

    @Test
    public void testBiFunction_Unchecked_Success() {
        Throwables.BiFunction<Integer, Integer, Integer, Exception> throwableBiFunction = (a, b) -> a + b;

        com.landawn.abacus.util.function.BiFunction<Integer, Integer, Integer> unchecked = throwableBiFunction.unchecked();
        Integer result = unchecked.apply(3, 4);

        assertEquals(7, result);
    }

    @Test
    public void testBiFunction_Unchecked_ThrowsRuntimeException() {
        Throwables.BiFunction<Integer, Integer, Integer, Exception> throwableBiFunction = (a, b) -> {
            throw new TestException("Test exception");
        };

        com.landawn.abacus.util.function.BiFunction<Integer, Integer, Integer> unchecked = throwableBiFunction.unchecked();

        assertThrows(RuntimeException.class, () -> unchecked.apply(1, 2));
    }

    @Test
    public void testConsumer_Unchecked_Success() {
        AtomicReference<String> captured = new AtomicReference<>();
        Throwables.Consumer<String, Exception> throwableConsumer = captured::set;

        com.landawn.abacus.util.function.Consumer<String> unchecked = throwableConsumer.unchecked();
        unchecked.accept("test");

        assertEquals("test", captured.get());
    }

    @Test
    public void testConsumer_Unchecked_ThrowsRuntimeException() {
        Throwables.Consumer<String, Exception> throwableConsumer = s -> {
            throw new TestException("Test exception");
        };

        com.landawn.abacus.util.function.Consumer<String> unchecked = throwableConsumer.unchecked();

        assertThrows(RuntimeException.class, () -> unchecked.accept("test"));
    }

    @Test
    public void testBiConsumer_Unchecked_Success() {
        AtomicReference<String> captured = new AtomicReference<>();
        Throwables.BiConsumer<String, String, Exception> throwableBiConsumer = (a, b) -> captured.set(a + b);

        com.landawn.abacus.util.function.BiConsumer<String, String> unchecked = throwableBiConsumer.unchecked();
        unchecked.accept("Hello", "World");

        assertEquals("HelloWorld", captured.get());
    }

    @Test
    public void testBiConsumer_Unchecked_ThrowsRuntimeException() {
        Throwables.BiConsumer<String, String, Exception> throwableBiConsumer = (a, b) -> {
            throw new TestException("Test exception");
        };

        com.landawn.abacus.util.function.BiConsumer<String, String> unchecked = throwableBiConsumer.unchecked();

        assertThrows(RuntimeException.class, () -> unchecked.accept("a", "b"));
    }

    @Test
    public void testBooleanNFunction_AndThen_Success() throws Exception {
        Throwables.BooleanNFunction<String, Exception> booleanNFunction = args -> String.valueOf(args.length);
        Throwables.BooleanNFunction<Integer, Exception> composed = booleanNFunction.andThen(Integer::parseInt);

        Integer result = composed.apply(true, false, true);
        assertEquals(3, result);
    }

    @Test
    public void testCharNFunction_AndThen_Success() throws Exception {
        Throwables.CharNFunction<String, Exception> charNFunction = args -> String.valueOf(args.length);
        Throwables.CharNFunction<Integer, Exception> composed = charNFunction.andThen(Integer::parseInt);

        Integer result = composed.apply('a', 'b', 'c');
        assertEquals(3, result);
    }

    @Test
    public void testByteNFunction_AndThen_Success() throws Exception {
        Throwables.ByteNFunction<String, Exception> byteNFunction = args -> String.valueOf(args.length);
        Throwables.ByteNFunction<Integer, Exception> composed = byteNFunction.andThen(Integer::parseInt);

        Integer result = composed.apply((byte) 1, (byte) 2);
        assertEquals(2, result);
    }

    @Test
    public void testShortNFunction_AndThen_Success() throws Exception {
        Throwables.ShortNFunction<String, Exception> shortNFunction = args -> String.valueOf(args.length);
        Throwables.ShortNFunction<Integer, Exception> composed = shortNFunction.andThen(Integer::parseInt);

        Integer result = composed.apply((short) 1, (short) 2, (short) 3, (short) 4);
        assertEquals(4, result);
    }

    @Test
    public void testIntNFunction_AndThen_Success() throws Exception {
        Throwables.IntNFunction<String, Exception> intNFunction = args -> String.valueOf(args.length);
        Throwables.IntNFunction<Integer, Exception> composed = intNFunction.andThen(Integer::parseInt);

        Integer result = composed.apply(1, 2, 3);
        assertEquals(3, result);
    }

    @Test
    public void testLongNFunction_AndThen_Success() throws Exception {
        Throwables.LongNFunction<String, Exception> longNFunction = args -> String.valueOf(args.length);
        Throwables.LongNFunction<Integer, Exception> composed = longNFunction.andThen(Integer::parseInt);

        Integer result = composed.apply(1L, 2L);
        assertEquals(2, result);
    }

    @Test
    public void testFloatNFunction_AndThen_Success() throws Exception {
        Throwables.FloatNFunction<String, Exception> floatNFunction = args -> String.valueOf(args.length);
        Throwables.FloatNFunction<Integer, Exception> composed = floatNFunction.andThen(Integer::parseInt);

        Integer result = composed.apply(1.0f, 2.0f, 3.0f);
        assertEquals(3, result);
    }

    @Test
    public void testDoubleNFunction_AndThen_Success() throws Exception {
        Throwables.DoubleNFunction<String, Exception> doubleNFunction = args -> String.valueOf(args.length);
        Throwables.DoubleNFunction<Integer, Exception> composed = doubleNFunction.andThen(Integer::parseInt);

        Integer result = composed.apply(1.0, 2.0);
        assertEquals(2, result);
    }

    @Test
    public void testNFunction_AndThen_Success() throws Exception {
        Throwables.NFunction<String, Integer, Exception> nFunction = args -> args.length;
        Throwables.NFunction<String, String, Exception> composed = nFunction.andThen(n -> "Count: " + n);

        String result = composed.apply("a", "b", "c");
        assertEquals("Count: 3", result);
    }
}
