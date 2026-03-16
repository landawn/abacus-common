package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
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
import java.sql.SQLException;
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
public class ThrowablesTest extends TestBase {

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
        assertNotNull(iter);
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

    @Test
    public void testRun_withoutException() {
        final AtomicBoolean executed = new AtomicBoolean(false);
        Throwables.run(() -> executed.set(true));
        assertTrue(executed.get(), "Runnable should have been executed.");
    }

    @Test
    public void testRun_withCheckedException() {
        assertThrows(RuntimeException.class, () -> {
            Throwables.run(() -> {
                throw new IOException("Test Exception");
            });
        }, "A checked exception should be wrapped in a RuntimeException.");
    }

    @Test
    public void testRun_withRuntimeException() {
        assertThrows(IllegalArgumentException.class, () -> {
            Throwables.run(() -> {
                throw new IllegalArgumentException("Test Runtime Exception");
            });
        }, "A RuntimeException should be rethrown as is.");
    }

    @Test
    public void testRun_withActionOnError_noException() {
        final AtomicBoolean cmdExecuted = new AtomicBoolean(false);
        final AtomicBoolean errorActionExecuted = new AtomicBoolean(false);

        Throwables.run(() -> cmdExecuted.set(true), e -> errorActionExecuted.set(true));

        assertTrue(cmdExecuted.get(), "Command should have been executed.");
        assertFalse(errorActionExecuted.get(), "Error action should not have been executed.");
    }

    @Test
    public void testRun_withActionOnError_withException() {
        final AtomicBoolean errorActionExecuted = new AtomicBoolean(false);
        final Exception testException = new IOException("Test");

        Throwables.run(() -> {
            throw testException;
        }, e -> {
            errorActionExecuted.set(true);
            assertSame(testException, e, "The correct exception should be passed to the error action.");
        });

        assertTrue(errorActionExecuted.get(), "Error action should have been executed.");
    }

    @Test
    public void testCall_withoutException() {
        String result = Throwables.call(() -> "success");
        assertEquals("success", result, "The result of the callable should be returned.");
    }

    @Test
    public void testCall_withCheckedException() {
        assertThrows(RuntimeException.class, () -> {
            Throwables.call(() -> {
                if (true)
                    throw new IOException("Test Exception");
                return "failure";
            });
        }, "A checked exception should be wrapped in a RuntimeException.");
    }

    @Test
    public void testCall_withActionOnError() {
        String result = Throwables.call(() -> {
            throw new IOException("Test");
        }, Fn.s(() -> "handled"));
        assertEquals("handled", result, "The actionOnError function should provide the return value.");
    }

    @Test
    public void testCall_withSupplier() {
        String result = Throwables.call(() -> {
            throw new Exception("Test");
        }, Fn.s(() -> "supplied"));
        assertEquals("supplied", result, "The supplier should provide the return value on error.");
    }

    @Test
    public void testCall_withDefaultValue() {
        String result = Throwables.call(() -> {
            throw new Exception("Test");
        }, "default");
        assertEquals("default", result, "The default value should be returned on error.");
    }

    @Test
    public void testCall_withPredicateAndSupplier_predicateTrue() {
        String result = Throwables.call(() -> {
            throw new IOException("IO Test");
        }, e -> e instanceof IOException, Fn.s(() -> "supplied_on_io"));
        assertEquals("supplied_on_io", result, "Supplier should be used when predicate is true.");
    }

    @Test
    public void testCall_withPredicateAndSupplier_predicateFalse() {
        assertThrows(RuntimeException.class, () -> {
            Throwables.call(() -> {
                throw new IllegalArgumentException("Arg Test");
            }, e -> e instanceof IOException, Fn.s(() -> "supplied_on_io"));
        }, "Exception should be rethrown when predicate is false.");
    }

    @Test
    public void testCall_withPredicateAndDefaultValue_predicateTrue() {
        String result = Throwables.call(() -> {
            throw new IOException("IO Test");
        }, e -> e instanceof IOException, "default_on_io");
        assertEquals("default_on_io", result, "Default value should be used when predicate is true.");
    }

    @Test
    public void testCall_withPredicateAndDefaultValue_predicateFalse() {
        assertThrows(RuntimeException.class, () -> {
            Throwables.call(() -> {
                throw new IllegalArgumentException("Arg Test");
            }, e -> e instanceof IOException, "default_on_io");
        }, "Exception should be rethrown when predicate is false.");
    }

    @Test
    public void testIterator_empty() throws Throwable {
        Throwables.Iterator<Object, ?> emptyIterator = Throwables.Iterator.empty();
        assertFalse(emptyIterator.hasNext(), "empty iterator should not have next.");
        assertThrows(NoSuchElementException.class, emptyIterator::next);
        assertEquals(0, emptyIterator.count());
    }

    @Test
    public void testIterator_just() throws Throwable {
        Throwables.Iterator<String, ?> singleIterator = Throwables.Iterator.just("one");
        assertTrue(singleIterator.hasNext());
        assertEquals("one", singleIterator.next());
        assertFalse(singleIterator.hasNext());
        assertThrows(NoSuchElementException.class, singleIterator::next);
    }

    @Test
    public void testIterator_ofArray() throws Throwable {
        Throwables.Iterator<Integer, ?> iterator = Throwables.Iterator.of(1, 2, 3);
        assertEquals(Arrays.asList(1, 2, 3), iterator.toList());
    }

    @Test
    public void testIterator_ofArraySlice() throws Throwable {
        Integer[] source = { 0, 1, 2, 3, 4, 5 };
        Throwables.Iterator<Integer, ?> iterator = Throwables.Iterator.of(source, 2, 4);
        assertEquals(Arrays.asList(2, 3), iterator.toList());
    }

    @Test
    public void testIterator_ofIterable() throws Throwable {
        List<String> sourceList = Arrays.asList("a", "b", "c");
        Throwables.Iterator<String, ?> iterator = Throwables.Iterator.of(sourceList);
        assertEquals(sourceList, iterator.toList());
    }

    @Test
    public void testIterator_ofJavaIterator() throws Throwable {
        List<String> sourceList = Arrays.asList("a", "b", "c");
        Throwables.Iterator<String, ?> iterator = Throwables.Iterator.of(sourceList.iterator());
        assertEquals(sourceList, iterator.toList());
    }

    @Test
    public void testIterator_ofLines() throws IOException {
        StringReader reader = new StringReader("line 1\nline 2\nline 3");
        Throwables.Iterator<String, IOException> lineIterator = Throwables.Iterator.ofLines(reader);
        assertEquals(Arrays.asList("line 1", "line 2", "line 3"), lineIterator.toList());
    }

    @Test
    public void testIterator_defer() throws Throwable {
        AtomicInteger supplierCalls = new AtomicInteger(0);
        Throwables.Iterator<Integer, ?> deferred = Throwables.Iterator.defer(() -> {
            supplierCalls.incrementAndGet();
            return Throwables.Iterator.of(1, 2);
        });

        assertEquals(0, supplierCalls.get(), "Supplier should not be called on creation.");

        assertTrue(deferred.hasNext(), "hasNext should trigger initialization.");
        assertEquals(1, supplierCalls.get(), "Supplier should be called on first hasNext.");

        assertEquals(1, deferred.next());
        assertEquals(1, supplierCalls.get(), "Supplier should not be called again.");

        assertEquals(Arrays.asList(2), deferred.toList());
        assertEquals(1, supplierCalls.get(), "Supplier should not be called again during consumption.");
    }

    @Test
    public void testIterator_concat() throws Throwable {
        Throwables.Iterator<Integer, Exception> iter1 = Throwables.Iterator.of(1, 2);
        Throwables.Iterator<Integer, Exception> iter2 = Throwables.Iterator.empty();
        Throwables.Iterator<Integer, Exception> iter3 = Throwables.Iterator.of(3, 4);

        Throwables.Iterator<Integer, ?> concatenated = Throwables.Iterator.concat(iter1, iter2, iter3);

        assertEquals(Arrays.asList(1, 2, 3, 4), concatenated.toList());
    }

    @Test
    public void testIterator_close() {
        final AtomicBoolean closed = new AtomicBoolean(false);
        Throwables.Iterator<Integer, Exception> closableIterator = new Throwables.Iterator<>() {
            private final Throwables.Iterator<Integer, Exception> internal = Throwables.Iterator.of(1, 2, 3);

            @Override
            public boolean hasNext() throws Exception {
                return internal.hasNext();
            }

            @Override
            public Integer next() throws Exception {
                return internal.next();
            }

            @Override
            protected void closeResource() {
                closed.set(true);
            }
        };

        try (closableIterator) {
            assertEquals(3, closableIterator.count());
        } catch (Throwable e) {
            fail("Should not throw exception on close");
        }

        assertTrue(closed.get(), "closeResource should be called when iterator is closed.");
    }

    @Test
    public void testIterator_forEachRemaining() throws Throwable {
        Throwables.Iterator<String, ?> iterator = Throwables.Iterator.of("x", "y", "z");
        List<String> items = new ArrayList<>();
        iterator.forEachRemaining(items::add);
        assertEquals(Arrays.asList("x", "y", "z"), items);
    }

    @Test
    public void testIterator_foreachRemaining_throwable() throws Throwable {
        Throwables.Iterator<String, ?> iterator = Throwables.Iterator.of("x", "y", "z");
        List<String> items = new ArrayList<>();
        iterator.foreachRemaining(items::add);
        assertEquals(Arrays.asList("x", "y", "z"), items);
    }

    @Test
    public void testIterator_foreachRemaining_throwable_withException() {
        Throwables.Iterator<String, ?> iterator = Throwables.Iterator.of("x", "y", "z");
        assertThrows(SQLException.class, () -> {
            iterator.foreachRemaining(item -> {
                if (item.equals("y")) {
                    throw new SQLException("Test SQL Exception");
                }
            });
        });
    }

    @Test
    public void testIterator_instanceMethods() throws Throwable {
        Throwables.Iterator<Integer, Exception> original = Throwables.Iterator.of(1, 2, 3, 4, 5, null);

        Throwables.Iterator<Integer, ?> filtered = original.filter(i -> i != null && i % 2 != 0);
        assertEquals(Arrays.asList(1, 3, 5), filtered.toList());

        original = Throwables.Iterator.of(1, 2, 3);
        Throwables.Iterator<String, ?> mapped = original.map(i -> "v" + i);
        assertEquals(Arrays.asList("v1", "v2", "v3"), mapped.toList());

        original = Throwables.Iterator.of(1, 2, 3);
        assertEquals(Nullable.of(1), original.first());
        assertEquals(Nullable.empty(), Throwables.Iterator.empty().first());

        original = Throwables.Iterator.of(null, null, 1, 2);
        assertEquals(com.landawn.abacus.util.u.Optional.of(1), original.firstNonNull());

        original = Throwables.Iterator.of(1, 2, 3);
        assertEquals(Nullable.of(3), original.last());

        original = Throwables.Iterator.of(1, 2, 3);
        assertArrayEquals(new Object[] { 1, 2, 3 }, original.toArray());

        original = Throwables.Iterator.of(4, 5, 6);
        assertArrayEquals(new Integer[] { 4, 5, 6 }, original.toArray(new Integer[0]));

        original = Throwables.Iterator.of(1, 2, 3, 4, 5);
        original.advance(2);
        assertEquals(3, original.next());

        Throwables.Iterator<String, Exception> original2 = Throwables.Iterator.of("a", "b");
        final List<String> indexedItems = new ArrayList<>();
        original2.foreachIndexed((idx, item) -> indexedItems.add(idx + ":" + item));
        assertEquals(Arrays.asList("0:a", "1:b"), indexedItems);
    }

    @Test
    public void testRunnable_unchecked() {
        Throwables.Runnable<IOException> throwableRunnable = () -> {
            throw new IOException("Test");
        };
        com.landawn.abacus.util.function.Runnable uncheckedRunnable = throwableRunnable.unchecked();
        assertThrows(RuntimeException.class, uncheckedRunnable::run);
    }

    @Test
    public void testCallable_unchecked() {
        Throwables.Callable<String, IOException> throwableCallable = () -> {
            throw new IOException("Test");
        };
        com.landawn.abacus.util.function.Callable<String> uncheckedCallable = throwableCallable.unchecked();
        assertThrows(RuntimeException.class, uncheckedCallable::call);
    }

    @Test
    public void testFunction_unchecked() {
        Throwables.Function<String, Integer, IOException> throwableFunction = s -> {
            throw new IOException("Test");
        };
        com.landawn.abacus.util.function.Function<String, Integer> uncheckedFunction = throwableFunction.unchecked();
        assertThrows(RuntimeException.class, () -> uncheckedFunction.apply("test"));
    }

    @Test
    public void testBiFunction_unchecked() {
        Throwables.BiFunction<String, Integer, Boolean, IOException> throwableBiFunction = (s, i) -> {
            throw new IOException("Test");
        };
        com.landawn.abacus.util.function.BiFunction<String, Integer, Boolean> uncheckedBiFunction = throwableBiFunction.unchecked();
        assertThrows(RuntimeException.class, () -> uncheckedBiFunction.apply("test", 123));
    }

    @Test
    public void testPredicate_unchecked() {
        Throwables.Predicate<String, IOException> throwablePredicate = s -> {
            throw new IOException("Test");
        };
        com.landawn.abacus.util.function.Predicate<String> uncheckedPredicate = throwablePredicate.unchecked();
        assertThrows(RuntimeException.class, () -> uncheckedPredicate.test("test"));
    }

    @Test
    public void testPredicate_negate() throws Throwable {
        Throwables.Predicate<Integer, ?> isEven = i -> i % 2 == 0;
        Throwables.Predicate<Integer, ?> isOdd = isEven.negate();

        assertFalse(isEven.test(3));
        assertTrue(isOdd.test(3));

        assertTrue(isEven.test(4));
        assertFalse(isOdd.test(4));
    }

    @Test
    public void testConsumer_unchecked() {
        Throwables.Consumer<String, IOException> throwableConsumer = s -> {
            throw new IOException("Test");
        };
        com.landawn.abacus.util.function.Consumer<String> uncheckedConsumer = throwableConsumer.unchecked();
        assertThrows(RuntimeException.class, () -> uncheckedConsumer.accept("test"));
    }

    @Test
    public void testNFunction_andThen() throws Throwable {
        Throwables.NFunction<Integer, Integer, ?> sum = args -> {
            int total = 0;
            for (int i : args)
                total += i;
            return total;
        };

        Throwables.NFunction<Integer, String, ?> sumAndStringify = sum.andThen(result -> "Sum is " + result);

        String result = sumAndStringify.apply(1, 2, 3, 4);
        assertEquals("Sum is 10", result);
    }

    @Test
    public void testLazyInitializer() throws Throwable {
        final AtomicInteger supplierCalls = new AtomicInteger(0);
        final Throwables.Supplier<String, Exception> supplier = () -> {
            supplierCalls.incrementAndGet();
            return "initialized";
        };

        Throwables.Supplier<String, ?> lazy = N.lazyInitialize(supplier);

        assertEquals(0, supplierCalls.get(), "Supplier should not be called on creation.");

        String value1 = lazy.get();
        assertEquals("initialized", value1);
        assertEquals(1, supplierCalls.get(), "Supplier should be called on first get().");

        String value2 = lazy.get();
        assertEquals("initialized", value2);
        assertEquals(1, supplierCalls.get(), "Supplier should not be called on subsequent get() calls.");
        assertSame(value1, value2, "Should return the same cached instance.");
    }

    @Test
    public void testLazyInitializer_threadSafety() throws InterruptedException {
        final AtomicInteger supplierCalls = new AtomicInteger(0);
        final Throwables.Supplier<String, Exception> supplier = () -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                fail("Sleep interrupted");
            }
            supplierCalls.incrementAndGet();
            return "initialized";
        };

        final Throwables.Supplier<String, Exception> lazy = N.lazyInitialize(supplier);
        final int numThreads = 10;
        final Thread[] threads = new Thread[numThreads];

        for (int i = 0; i < numThreads; i++) {
            threads[i] = new Thread(() -> {
                try {
                    assertEquals("initialized", lazy.get());
                } catch (Throwable e) {
                    fail("Exception in thread", e);
                }
            });
        }

        for (Thread t : threads) {
            t.start();
        }

        for (Thread t : threads) {
            t.join();
        }

        assertEquals(1, supplierCalls.get(), "Supplier must be called exactly once in a multi-threaded environment.");
    }

    @Test
    public void testCall_ReturnsNull() {
        String result = Throwables.call(() -> null);
        assertNull(result);
    }

    @Test
    public void testCall_WithSupplier_ReturnsSupplierValueOnError() {
        String result = Throwables.call(() -> {
            throw new TestException("Error");
        }, Fn.s(() -> "Default value"));

        assertEquals("Default value", result);
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
    public void testIterator_OfJavaIterator() throws Exception {
        List<String> list = Arrays.asList("A", "B", "C");
        Throwables.Iterator<String, Exception> iter = Throwables.Iterator.of(list.iterator());
        assertEquals("A", iter.next());
        assertEquals("B", iter.next());
        assertEquals("C", iter.next());
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
    public void testIterator_Advance_BeyondEnd() throws Exception {
        Throwables.Iterator<String, Exception> iter = Throwables.Iterator.of("A", "B");
        iter.advance(5);
        assertFalse(iter.hasNext());
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
    public void testIterator_First() throws Exception {
        Throwables.Iterator<String, Exception> iter = Throwables.Iterator.of("First", "Second");
        assertEquals("First", iter.first().orElse(null));
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
    public void testIterator_Last() throws Exception {
        Throwables.Iterator<String, Exception> iter = Throwables.Iterator.of("First", "Middle", "Last");
        assertEquals("Last", iter.last().orElse(null));
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
    public void testRunnable_Unchecked_Exception() {
        Throwables.Runnable<TestException> throwingRunnable = () -> {
            throw new TestException("Test");
        };

        com.landawn.abacus.util.function.Runnable unchecked = throwingRunnable.unchecked();
        assertThrows(RuntimeException.class, () -> unchecked.run());
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
    public void testPredicate_Negate() throws TestException {
        Throwables.Predicate<Integer, TestException> predicate = n -> n > 5;
        Throwables.Predicate<Integer, TestException> negated = predicate.negate();

        assertTrue(predicate.test(10));
        assertFalse(negated.test(10));

        assertFalse(predicate.test(3));
        assertTrue(negated.test(3));
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

    // ========== Tests for 52 untested methods ==========

    // 1. TriPredicate (generic)
    @Test
    public void testTriPredicate() throws Exception {
        Throwables.TriPredicate<String, Integer, Boolean, Exception> triPred = (s, i, b) -> s.length() > i && b;
        assertTrue(triPred.test("Hello", 3, true));
        assertFalse(triPred.test("Hi", 3, true));
        assertFalse(triPred.test("Hello", 3, false));
    }

    // 2. QuadPredicate
    @Test
    public void testQuadPredicate() throws Exception {
        Throwables.QuadPredicate<String, String, Integer, Boolean, Exception> quadPred = (a, b, i, flag) -> flag && (a.length() + b.length()) > i;
        assertTrue(quadPred.test("Hello", "World", 5, true));
        assertFalse(quadPred.test("Hi", "Go", 5, true));
        assertFalse(quadPred.test("Hello", "World", 5, false));
    }

    // 3. TriFunction (generic)
    @Test
    public void testTriFunction() throws Exception {
        Throwables.TriFunction<String, Integer, Boolean, String, Exception> triFunc = (s, i, b) -> b ? s.substring(0, Math.min(i, s.length())) : s;
        assertEquals("Hel", triFunc.apply("Hello", 3, true));
        assertEquals("Hello", triFunc.apply("Hello", 3, false));
    }

    // 4. QuadFunction
    @Test
    public void testQuadFunction() throws Exception {
        Throwables.QuadFunction<String, String, String, String, String, Exception> quadFunc = (a, b, c, d) -> a + "-" + b + "-" + c + "-" + d;
        assertEquals("A-B-C-D", quadFunc.apply("A", "B", "C", "D"));
    }

    // 5. TriConsumer (generic)
    @Test
    public void testTriConsumer() throws Exception {
        List<String> results = new ArrayList<>();
        Throwables.TriConsumer<String, Integer, Boolean, Exception> triConsumer = (s, i, b) -> results.add(s + ":" + i + ":" + b);
        triConsumer.accept("val", 42, true);
        assertEquals("val:42:true", results.get(0));
    }

    // 6. QuadConsumer
    @Test
    public void testQuadConsumer() throws Exception {
        List<String> results = new ArrayList<>();
        Throwables.QuadConsumer<String, String, Integer, Boolean, Exception> quadConsumer = (a, b, i, flag) -> results.add(a + b + i + flag);
        quadConsumer.accept("X", "Y", 1, true);
        assertEquals("XY1true", results.get(0));
    }

    // 7. ByteConsumer
    @Test
    public void testByteConsumer() throws Exception {
        AtomicInteger captured = new AtomicInteger();
        Throwables.ByteConsumer<Exception> byteConsumer = b -> captured.set(b);
        byteConsumer.accept((byte) 42);
        assertEquals(42, captured.get());
    }

    // 8. ShortConsumer
    @Test
    public void testShortConsumer() throws Exception {
        AtomicInteger captured = new AtomicInteger();
        Throwables.ShortConsumer<Exception> shortConsumer = s -> captured.set(s);
        shortConsumer.accept((short) 300);
        assertEquals(300, captured.get());
    }

    // 9. FloatConsumer
    @Test
    public void testFloatConsumer() throws Exception {
        AtomicReference<Float> captured = new AtomicReference<>();
        Throwables.FloatConsumer<Exception> floatConsumer = captured::set;
        floatConsumer.accept(3.14f);
        assertEquals(3.14f, captured.get(), 0.001f);
    }

    // 10. BytePredicate
    @Test
    public void testBytePredicate() throws Exception {
        Throwables.BytePredicate<Exception> bytePred = b -> b > 0;
        assertTrue(bytePred.test((byte) 1));
        assertFalse(bytePred.test((byte) -1));
    }

    // 11. ShortPredicate
    @Test
    public void testShortPredicate() throws Exception {
        Throwables.ShortPredicate<Exception> shortPred = s -> s > 100;
        assertTrue(shortPred.test((short) 200));
        assertFalse(shortPred.test((short) 50));
    }

    // 12. FloatPredicate
    @Test
    public void testFloatPredicate() throws Exception {
        Throwables.FloatPredicate<Exception> floatPred = f -> f > 1.0f;
        assertTrue(floatPred.test(2.5f));
        assertFalse(floatPred.test(0.5f));
    }

    // 13. LongPredicate
    @Test
    public void testLongPredicate() throws Exception {
        Throwables.LongPredicate<Exception> longPred = l -> l > 1000L;
        assertTrue(longPred.test(2000L));
        assertFalse(longPred.test(500L));
    }

    // 14. CharFunction
    @Test
    public void testCharFunction() throws Exception {
        Throwables.CharFunction<String, Exception> charFunc = c -> "Char: " + c;
        assertEquals("Char: A", charFunc.apply('A'));
    }

    // 15. ByteFunction
    @Test
    public void testByteFunction() throws Exception {
        Throwables.ByteFunction<String, Exception> byteFunc = b -> "Byte: " + b;
        assertEquals("Byte: 42", byteFunc.apply((byte) 42));
    }

    // 16. ShortFunction
    @Test
    public void testShortFunction() throws Exception {
        Throwables.ShortFunction<String, Exception> shortFunc = s -> "Short: " + s;
        assertEquals("Short: 300", shortFunc.apply((short) 300));
    }

    // 17. FloatFunction
    @Test
    public void testFloatFunction() throws Exception {
        Throwables.FloatFunction<String, Exception> floatFunc = f -> String.format("Float: %.1f", f);
        assertEquals("Float: 3.1", floatFunc.apply(3.14f));
    }

    // 18. LongFunction
    @Test
    public void testLongFunction() throws Exception {
        Throwables.LongFunction<String, Exception> longFunc = l -> "Long: " + l;
        assertEquals("Long: 999", longFunc.apply(999L));
    }

    // 19. DoubleFunction
    @Test
    public void testDoubleFunction() throws Exception {
        Throwables.DoubleFunction<String, Exception> doubleFunc = d -> String.format("Double: %.2f", d);
        assertEquals("Double: 3.14", doubleFunc.apply(3.14));
    }

    // 20. IntToDoubleFunction
    @Test
    public void testIntToDoubleFunction() throws Exception {
        Throwables.IntToDoubleFunction<Exception> intToDouble = i -> i * 1.5;
        assertEquals(7.5, intToDouble.applyAsDouble(5), 0.001);
    }

    // 21. LongToIntFunction
    @Test
    public void testLongToIntFunction() throws Exception {
        Throwables.LongToIntFunction<Exception> longToInt = l -> (int) (l % Integer.MAX_VALUE);
        assertEquals(42, longToInt.applyAsInt(42L));
    }

    // 22. LongToDoubleFunction
    @Test
    public void testLongToDoubleFunction() throws Exception {
        Throwables.LongToDoubleFunction<Exception> longToDouble = l -> l * 0.1;
        assertEquals(10.0, longToDouble.applyAsDouble(100L), 0.001);
    }

    // 23. FloatToIntFunction
    @Test
    public void testFloatToIntFunction() throws Exception {
        Throwables.FloatToIntFunction<Exception> floatToInt = f -> Math.round(f);
        assertEquals(3, floatToInt.applyAsInt(3.14f));
        assertEquals(4, floatToInt.applyAsInt(3.7f));
    }

    // 24. FloatToLongFunction
    @Test
    public void testFloatToLongFunction() throws Exception {
        Throwables.FloatToLongFunction<Exception> floatToLong = f -> (long) f;
        assertEquals(3L, floatToLong.applyAsLong(3.9f));
    }

    // 25. FloatToDoubleFunction
    @Test
    public void testFloatToDoubleFunction() throws Exception {
        Throwables.FloatToDoubleFunction<Exception> floatToDouble = f -> f * 2.0;
        assertEquals(6.28, floatToDouble.applyAsDouble(3.14f), 0.01);
    }

    // 26. DoubleToLongFunction
    @Test
    public void testDoubleToLongFunction() throws Exception {
        Throwables.DoubleToLongFunction<Exception> doubleToLong = d -> Math.round(d);
        assertEquals(3L, doubleToLong.applyAsLong(3.14));
        assertEquals(4L, doubleToLong.applyAsLong(3.7));
    }

    // 27. ToByteFunction
    @Test
    public void testToByteFunction() throws Exception {
        Throwables.ToByteFunction<String, Exception> toByte = s -> (byte) s.length();
        assertEquals((byte) 5, toByte.applyAsByte("Hello"));
    }

    // 28. ToShortFunction
    @Test
    public void testToShortFunction() throws Exception {
        Throwables.ToShortFunction<String, Exception> toShort = s -> (short) s.length();
        assertEquals((short) 5, toShort.applyAsShort("Hello"));
    }

    // 29. ToFloatFunction
    @Test
    public void testToFloatFunction() throws Exception {
        Throwables.ToFloatFunction<String, Exception> toFloat = s -> Float.parseFloat(s);
        assertEquals(3.14f, toFloat.applyAsFloat("3.14"), 0.001f);
    }

    // 30. ToLongFunction
    @Test
    public void testToLongFunction() throws Exception {
        Throwables.ToLongFunction<String, Exception> toLong = s -> Long.parseLong(s);
        assertEquals(12345L, toLong.applyAsLong("12345"));
    }

    // 31. ByteUnaryOperator
    @Test
    public void testByteUnaryOperator() throws Exception {
        Throwables.ByteUnaryOperator<Exception> byteOp = b -> (byte) (b * 2);
        assertEquals((byte) 10, byteOp.applyAsByte((byte) 5));
    }

    // 32. ShortUnaryOperator
    @Test
    public void testShortUnaryOperator() throws Exception {
        Throwables.ShortUnaryOperator<Exception> shortOp = s -> (short) (s + 1);
        assertEquals((short) 101, shortOp.applyAsShort((short) 100));
    }

    // 33. LongUnaryOperator
    @Test
    public void testLongUnaryOperator() throws Exception {
        Throwables.LongUnaryOperator<Exception> longOp = l -> l * l;
        assertEquals(100L, longOp.applyAsLong(10L));
    }

    // 34. FloatUnaryOperator
    @Test
    public void testFloatUnaryOperator() throws Exception {
        Throwables.FloatUnaryOperator<Exception> floatOp = f -> f * 2.0f;
        assertEquals(6.28f, floatOp.applyAsFloat(3.14f), 0.001f);
    }

    // 35. CharBinaryOperator
    @Test
    public void testCharBinaryOperator() throws Exception {
        Throwables.CharBinaryOperator<Exception> charOp = (a, b) -> (char) Math.max(a, b);
        assertEquals('Z', charOp.applyAsChar('A', 'Z'));
    }

    // 36. ByteBinaryOperator
    @Test
    public void testByteBinaryOperator() throws Exception {
        Throwables.ByteBinaryOperator<Exception> byteOp = (a, b) -> (byte) (a + b);
        assertEquals((byte) 7, byteOp.applyAsByte((byte) 3, (byte) 4));
    }

    // 37. ShortBinaryOperator
    @Test
    public void testShortBinaryOperator() throws Exception {
        Throwables.ShortBinaryOperator<Exception> shortOp = (a, b) -> (short) (a * b);
        assertEquals((short) 12, shortOp.applyAsShort((short) 3, (short) 4));
    }

    // 38. LongBinaryOperator
    @Test
    public void testLongBinaryOperator() throws Exception {
        Throwables.LongBinaryOperator<Exception> longOp = (a, b) -> a + b;
        assertEquals(30L, longOp.applyAsLong(10L, 20L));
    }

    // 39. FloatBinaryOperator
    @Test
    public void testFloatBinaryOperator() throws Exception {
        Throwables.FloatBinaryOperator<Exception> floatOp = (a, b) -> a + b;
        assertEquals(5.5f, floatOp.applyAsFloat(2.5f, 3.0f), 0.001f);
    }

    // 40. CharTernaryOperator
    @Test
    public void testCharTernaryOperator() throws Exception {
        Throwables.CharTernaryOperator<Exception> charOp = (a, b, c) -> (char) (Math.max(Math.max(a, b), c));
        assertEquals('C', charOp.applyAsChar('A', 'C', 'B'));
    }

    // 41. ByteTernaryOperator
    @Test
    public void testByteTernaryOperator() throws Exception {
        Throwables.ByteTernaryOperator<Exception> byteOp = (a, b, c) -> (byte) (a + b + c);
        assertEquals((byte) 6, byteOp.applyAsByte((byte) 1, (byte) 2, (byte) 3));
    }

    // 42. ShortTernaryOperator
    @Test
    public void testShortTernaryOperator() throws Exception {
        Throwables.ShortTernaryOperator<Exception> shortOp = (a, b, c) -> (short) (a + b + c);
        assertEquals((short) 60, shortOp.applyAsShort((short) 10, (short) 20, (short) 30));
    }

    // 43. LongTernaryOperator
    @Test
    public void testLongTernaryOperator() throws Exception {
        Throwables.LongTernaryOperator<Exception> longOp = (a, b, c) -> a * b + c;
        assertEquals(23L, longOp.applyAsLong(4L, 5L, 3L));
    }

    // 44. FloatTernaryOperator
    @Test
    public void testFloatTernaryOperator() throws Exception {
        Throwables.FloatTernaryOperator<Exception> floatOp = (a, b, c) -> a + b + c;
        assertEquals(6.0f, floatOp.applyAsFloat(1.0f, 2.0f, 3.0f), 0.001f);
    }

    // 45. ByteBiPredicate
    @Test
    public void testByteBiPredicate() throws Exception {
        Throwables.ByteBiPredicate<Exception> byteBiPred = (a, b) -> a + b > 10;
        assertTrue(byteBiPred.test((byte) 7, (byte) 5));
        assertFalse(byteBiPred.test((byte) 2, (byte) 3));
    }

    // 46. ShortBiPredicate
    @Test
    public void testShortBiPredicate() throws Exception {
        Throwables.ShortBiPredicate<Exception> shortBiPred = (a, b) -> a > b;
        assertTrue(shortBiPred.test((short) 200, (short) 100));
        assertFalse(shortBiPred.test((short) 50, (short) 100));
    }

    // 47. LongBiPredicate
    @Test
    public void testLongBiPredicate() throws Exception {
        Throwables.LongBiPredicate<Exception> longBiPred = (a, b) -> a + b > 1000L;
        assertTrue(longBiPred.test(600L, 500L));
        assertFalse(longBiPred.test(200L, 300L));
    }

    // 48. FloatBiPredicate
    @Test
    public void testFloatBiPredicate() throws Exception {
        Throwables.FloatBiPredicate<Exception> floatBiPred = (a, b) -> Math.abs(a - b) < 0.01f;
        assertTrue(floatBiPred.test(1.234f, 1.235f));
        assertFalse(floatBiPred.test(1.0f, 2.0f));
    }

    // 49. ByteBiFunction
    @Test
    public void testByteBiFunction() throws Exception {
        Throwables.ByteBiFunction<String, Exception> byteBiFunc = (a, b) -> "Sum: " + (a + b);
        assertEquals("Sum: 7", byteBiFunc.apply((byte) 3, (byte) 4));
    }

    // 50. ShortBiFunction
    @Test
    public void testShortBiFunction() throws Exception {
        Throwables.ShortBiFunction<String, Exception> shortBiFunc = (a, b) -> "Product: " + (a * b);
        assertEquals("Product: 600", shortBiFunc.apply((short) 20, (short) 30));
    }

    // 51. LongBiFunction
    @Test
    public void testLongBiFunction() throws Exception {
        Throwables.LongBiFunction<String, Exception> longBiFunc = (a, b) -> "Max: " + Math.max(a, b);
        assertEquals("Max: 200", longBiFunc.apply(100L, 200L));
    }

    // 52. FloatBiFunction
    @Test
    public void testFloatBiFunction() throws Exception {
        Throwables.FloatBiFunction<String, Exception> floatBiFunc = (a, b) -> String.format("Avg: %.1f", (a + b) / 2.0f);
        assertEquals("Avg: 2.5", floatBiFunc.apply(2.0f, 3.0f));
    }

}
