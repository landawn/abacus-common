package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.Throwables.LazyInitializer;

public class ThrowablesTest extends ThrowablesTestSupport {
    @Test
    public void testRun_Success() {
        AtomicBoolean executed = new AtomicBoolean(false);
        Throwables.run(() -> executed.set(true));
        assertTrue(executed.get());
    }

    @Test
    public void testRun_NullCommand() {
        assertThrows(IllegalArgumentException.class, () -> Throwables.run(null));
    }

    @Test
    public void testRun_WithErrorHandler_NullCommand() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> Throwables.run(null, e -> {
        }));
    }

    @Test
    public void testRun_WithErrorHandler_NullErrorHandler() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> Throwables.run(() -> {
        }, null));
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
    public void testRecoveryOverloadsRestoreInterruptedStatusBeforeRecovery() {
        final Throwables.Callable<String, InterruptedException> interruptedCall = () -> {
            throw new InterruptedException("stop");
        };

        try {
            Thread.interrupted();
            Throwables.run(() -> {
                throw new InterruptedException("stop");
            }, e -> assertTrue(Thread.currentThread().isInterrupted()));
            assertTrue(Thread.currentThread().isInterrupted());

            Thread.interrupted();
            assertEquals("function", Throwables.call(interruptedCall, (java.util.function.Function<Throwable, String>) e -> {
                assertTrue(Thread.currentThread().isInterrupted());
                return "function";
            }));

            Thread.interrupted();
            assertEquals("supplier", Throwables.call(interruptedCall, (java.util.function.Supplier<String>) () -> {
                assertTrue(Thread.currentThread().isInterrupted());
                return "supplier";
            }));

            Thread.interrupted();
            assertEquals("default", Throwables.call(interruptedCall, "default"));
            assertTrue(Thread.currentThread().isInterrupted());

            Thread.interrupted();
            assertEquals("conditional supplier", Throwables.call(interruptedCall, (java.util.function.Predicate<Throwable>) e -> {
                assertTrue(Thread.currentThread().isInterrupted());
                return true;
            }, (java.util.function.Supplier<String>) () -> "conditional supplier"));

            Thread.interrupted();
            assertEquals("conditional default", Throwables.call(interruptedCall, (java.util.function.Predicate<Throwable>) e -> {
                assertTrue(Thread.currentThread().isInterrupted());
                return true;
            }, "conditional default"));
        } finally {
            Thread.interrupted();
        }
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
    public void testCallable_Unchecked_Success() {
        Throwables.Callable<String, Exception> throwableCallable = () -> "result";

        com.landawn.abacus.util.function.Callable<String> unchecked = throwableCallable.unchecked();
        String result = unchecked.call();

        assertEquals("result", result);
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
    public void testRunnable_Unchecked_PreservesRuntimeIdentity() {
        final TestRuntimeException failure = new TestRuntimeException("same instance");
        final Throwables.Runnable<Exception> throwableRunnable = () -> {
            throw failure;
        };

        assertSame(failure, assertThrows(TestRuntimeException.class, throwableRunnable.unchecked()::run));
    }

    @Test
    public void testRunnable_Unchecked_RestoresInterruptedStatus() {
        final Throwables.Runnable<InterruptedException> throwableRunnable = () -> {
            throw new InterruptedException("interrupted");
        };

        try {
            final RuntimeException failure = assertThrows(RuntimeException.class, throwableRunnable.unchecked()::run);
            assertTrue(Thread.currentThread().isInterrupted());
            assertTrue(failure.getCause() instanceof InterruptedException);
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    public void testLazyInitializer_Of_NullSupplier() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> LazyInitializer.of(null));
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
    public void testLazyInitializer_Get_RecursiveInitializationFailsFastAndRetries() {
        final AtomicInteger attempts = new AtomicInteger();
        @SuppressWarnings("unchecked")
        final LazyInitializer<String, Exception>[] holder = new LazyInitializer[1];

        holder[0] = LazyInitializer.of(() -> {
            attempts.incrementAndGet();
            return holder[0].get();
        });

        final IllegalStateException first = assertThrows(IllegalStateException.class, holder[0]::get);
        final IllegalStateException second = assertThrows(IllegalStateException.class, holder[0]::get);

        assertEquals("Recursive initialization of deferred value", first.getMessage());
        assertEquals("Recursive initialization of deferred value", second.getMessage());
        assertNotSame(first, second);
        assertEquals(2, attempts.get());
    }

    @Test
    public void testLazyInitializer_Get_ReturnsSameInstance() throws Exception {
        LazyInitializer<Object, Exception> lazy = LazyInitializer.of(Object::new);

        Object first = lazy.get();
        Object second = lazy.get();

        assertSame(first, second, "Should return the same instance");
    }

    @Test
    public void testLazyInitializer_Get_ReleasesSupplierAfterSuccess() throws Exception {
        LazyInitializer<Object, Exception> lazy = LazyInitializer.of(Object::new);
        java.lang.reflect.Field supplierField = LazyInitializer.class.getDeclaredField("supplier");
        supplierField.setAccessible(true);
        assertNotNull(supplierField.get(lazy));

        lazy.get();

        assertNull(supplierField.get(lazy), "Successful initialization must not retain the supplier or its captured state");
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
    public void testNFunctions_AndThen_DoesNotValidateAfterEagerly() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> ((Throwables.BooleanNFunction<Integer, Exception>) args -> args.length).andThen(null));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> ((Throwables.CharNFunction<Integer, Exception>) args -> args.length).andThen(null));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> ((Throwables.ByteNFunction<Integer, Exception>) args -> args.length).andThen(null));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> ((Throwables.ShortNFunction<Integer, Exception>) args -> args.length).andThen(null));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> ((Throwables.IntNFunction<Integer, Exception>) args -> args.length).andThen(null));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> ((Throwables.LongNFunction<Integer, Exception>) args -> args.length).andThen(null));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> ((Throwables.FloatNFunction<Integer, Exception>) args -> args.length).andThen(null));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> ((Throwables.DoubleNFunction<Integer, Exception>) args -> args.length).andThen(null));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> ((Throwables.NFunction<String, Integer, Exception>) args -> args.length).andThen(null));
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
            for (int i : args) {
                sum += i;
            }
            return sum;
        };
        assertEquals(Integer.valueOf(10), sumFunc.apply(1, 2, 3, 4));
        assertEquals(Integer.valueOf(0), sumFunc.apply());

        Throwables.DoubleNFunction<Double, Exception> avgFunc = args -> {
            if (args.length == 0) {
                return 0.0;
            }
            double sum = 0;
            for (double d : args) {
                sum += d;
            }
            return sum / args.length;
        };
        assertEquals(2.5, avgFunc.apply(1.0, 2.0, 3.0, 4.0), 0.001);

        Throwables.IntNFunction<Integer, Exception> multiplyFunc = args -> {
            int product = 1;
            for (int i : args) {
                product *= i;
            }
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
    public void testStaticFactoryMethods() throws Exception {
        Throwables.IntObjConsumer<String, Exception> originalConsumer = (i, s) -> {
        };
        Throwables.IntObjConsumer<String, Exception> consumer = Throwables.IntObjConsumer.of(originalConsumer);
        assertSame(originalConsumer, consumer);

        Throwables.IntObjFunction<String, String, Exception> originalFunction = (i, s) -> s + i;
        Throwables.IntObjFunction<String, String, Exception> function = Throwables.IntObjFunction.of(originalFunction);
        assertSame(originalFunction, function);
        assertEquals("Test1", function.apply(1, "Test"));

        Throwables.IntObjPredicate<String, Exception> originalPredicate = (i, s) -> s.length() > i;
        Throwables.IntObjPredicate<String, Exception> predicate = Throwables.IntObjPredicate.of(originalPredicate);
        assertSame(originalPredicate, predicate);
        assertTrue(predicate.test(2, "Hello"));
        assertFalse(predicate.test(10, "Hi"));
    }

    @Test
    public void testStaticFactoryMethods_RejectNull() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> Throwables.IntObjConsumer.of(null));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> Throwables.IntObjFunction.of(null));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> Throwables.IntObjPredicate.of(null));
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

    @Test
    public void testTriPredicate() throws Exception {
        Throwables.TriPredicate<String, Integer, Boolean, Exception> triPred = (s, i, b) -> s.length() > i && b;
        assertTrue(triPred.test("Hello", 3, true));
        assertFalse(triPred.test("Hi", 3, true));
        assertFalse(triPred.test("Hello", 3, false));
    }

    @Test
    public void testQuadPredicate() throws Exception {
        Throwables.QuadPredicate<String, String, Integer, Boolean, Exception> quadPred = (a, b, i, flag) -> flag && (a.length() + b.length()) > i;
        assertTrue(quadPred.test("Hello", "World", 5, true));
        assertFalse(quadPred.test("Hi", "Go", 5, true));
        assertFalse(quadPred.test("Hello", "World", 5, false));
    }

    @Test
    public void testTriFunction() throws Exception {
        Throwables.TriFunction<String, Integer, Boolean, String, Exception> triFunc = (s, i, b) -> b ? s.substring(0, Math.min(i, s.length())) : s;
        assertEquals("Hel", triFunc.apply("Hello", 3, true));
        assertEquals("Hello", triFunc.apply("Hello", 3, false));
    }

    @Test
    public void testQuadFunction() throws Exception {
        Throwables.QuadFunction<String, String, String, String, String, Exception> quadFunc = (a, b, c, d) -> a + "-" + b + "-" + c + "-" + d;
        assertEquals("A-B-C-D", quadFunc.apply("A", "B", "C", "D"));
    }

    @Test
    public void testTriConsumer() throws Exception {
        List<String> results = new ArrayList<>();
        Throwables.TriConsumer<String, Integer, Boolean, Exception> triConsumer = (s, i, b) -> results.add(s + ":" + i + ":" + b);
        triConsumer.accept("val", 42, true);
        assertEquals("val:42:true", results.get(0));
    }

    @Test
    public void testQuadConsumer() throws Exception {
        List<String> results = new ArrayList<>();
        Throwables.QuadConsumer<String, String, Integer, Boolean, Exception> quadConsumer = (a, b, i, flag) -> results.add(a + b + i + flag);
        quadConsumer.accept("X", "Y", 1, true);
        assertEquals("XY1true", results.get(0));
    }

    @Test
    public void testByteConsumer() throws Exception {
        AtomicInteger captured = new AtomicInteger();
        Throwables.ByteConsumer<Exception> byteConsumer = b -> captured.set(b);
        byteConsumer.accept((byte) 42);
        assertEquals(42, captured.get());
    }

    @Test
    public void testShortConsumer() throws Exception {
        AtomicInteger captured = new AtomicInteger();
        Throwables.ShortConsumer<Exception> shortConsumer = s -> captured.set(s);
        shortConsumer.accept((short) 300);
        assertEquals(300, captured.get());
    }

    @Test
    public void testFloatConsumer() throws Exception {
        AtomicReference<Float> captured = new AtomicReference<>();
        Throwables.FloatConsumer<Exception> floatConsumer = captured::set;
        floatConsumer.accept(3.14f);
        assertEquals(3.14f, captured.get(), 0.001f);
    }

    @Test
    public void testBytePredicate() throws Exception {
        Throwables.BytePredicate<Exception> bytePred = b -> b > 0;
        assertTrue(bytePred.test((byte) 1));
        assertFalse(bytePred.test((byte) -1));
    }

    @Test
    public void testShortPredicate() throws Exception {
        Throwables.ShortPredicate<Exception> shortPred = s -> s > 100;
        assertTrue(shortPred.test((short) 200));
        assertFalse(shortPred.test((short) 50));
    }

    @Test
    public void testFloatPredicate() throws Exception {
        Throwables.FloatPredicate<Exception> floatPred = f -> f > 1.0f;
        assertTrue(floatPred.test(2.5f));
        assertFalse(floatPred.test(0.5f));
    }

    @Test
    public void testLongPredicate() throws Exception {
        Throwables.LongPredicate<Exception> longPred = l -> l > 1000L;
        assertTrue(longPred.test(2000L));
        assertFalse(longPred.test(500L));
    }

    @Test
    public void testCharFunction() throws Exception {
        Throwables.CharFunction<String, Exception> charFunc = c -> "Char: " + c;
        assertEquals("Char: A", charFunc.apply('A'));
    }

    @Test
    public void testByteFunction() throws Exception {
        Throwables.ByteFunction<String, Exception> byteFunc = b -> "Byte: " + b;
        assertEquals("Byte: 42", byteFunc.apply((byte) 42));
    }

    @Test
    public void testShortFunction() throws Exception {
        Throwables.ShortFunction<String, Exception> shortFunc = s -> "Short: " + s;
        assertEquals("Short: 300", shortFunc.apply((short) 300));
    }

    @Test
    public void testFloatFunction() throws Exception {
        Throwables.FloatFunction<String, Exception> floatFunc = f -> String.format("Float: %.1f", f);
        assertEquals("Float: 3.1", floatFunc.apply(3.14f));
    }

    @Test
    public void testLongFunction() throws Exception {
        Throwables.LongFunction<String, Exception> longFunc = l -> "Long: " + l;
        assertEquals("Long: 999", longFunc.apply(999L));
    }

    @Test
    public void testDoubleFunction() throws Exception {
        Throwables.DoubleFunction<String, Exception> doubleFunc = d -> String.format("Double: %.2f", d);
        assertEquals("Double: 3.14", doubleFunc.apply(3.14));
    }

    @Test
    public void testIntToDoubleFunction() throws Exception {
        Throwables.IntToDoubleFunction<Exception> intToDouble = i -> i * 1.5;
        assertEquals(7.5, intToDouble.applyAsDouble(5), 0.001);
    }

    @Test
    public void testLongToIntFunction() throws Exception {
        Throwables.LongToIntFunction<Exception> longToInt = l -> (int) (l % Integer.MAX_VALUE);
        assertEquals(42, longToInt.applyAsInt(42L));
    }

    @Test
    public void testLongToDoubleFunction() throws Exception {
        Throwables.LongToDoubleFunction<Exception> longToDouble = l -> l * 0.1;
        assertEquals(10.0, longToDouble.applyAsDouble(100L), 0.001);
    }

    @Test
    public void testFloatToIntFunction() throws Exception {
        Throwables.FloatToIntFunction<Exception> floatToInt = f -> Math.round(f);
        assertEquals(3, floatToInt.applyAsInt(3.14f));
        assertEquals(4, floatToInt.applyAsInt(3.7f));
    }

    @Test
    public void testFloatToLongFunction() throws Exception {
        Throwables.FloatToLongFunction<Exception> floatToLong = f -> (long) f;
        assertEquals(3L, floatToLong.applyAsLong(3.9f));
    }

    @Test
    public void testFloatToDoubleFunction() throws Exception {
        Throwables.FloatToDoubleFunction<Exception> floatToDouble = f -> f * 2.0;
        assertEquals(6.28, floatToDouble.applyAsDouble(3.14f), 0.01);
    }

    @Test
    public void testDoubleToLongFunction() throws Exception {
        Throwables.DoubleToLongFunction<Exception> doubleToLong = d -> Math.round(d);
        assertEquals(3L, doubleToLong.applyAsLong(3.14));
        assertEquals(4L, doubleToLong.applyAsLong(3.7));
    }

    @Test
    public void testToByteFunction() throws Exception {
        Throwables.ToByteFunction<String, Exception> toByte = s -> (byte) s.length();
        assertEquals((byte) 5, toByte.applyAsByte("Hello"));
    }

    @Test
    public void testToShortFunction() throws Exception {
        Throwables.ToShortFunction<String, Exception> toShort = s -> (short) s.length();
        assertEquals((short) 5, toShort.applyAsShort("Hello"));
    }

    @Test
    public void testToFloatFunction() throws Exception {
        Throwables.ToFloatFunction<String, Exception> toFloat = s -> Float.parseFloat(s);
        assertEquals(3.14f, toFloat.applyAsFloat("3.14"), 0.001f);
    }

    @Test
    public void testToLongFunction() throws Exception {
        Throwables.ToLongFunction<String, Exception> toLong = s -> Long.parseLong(s);
        assertEquals(12345L, toLong.applyAsLong("12345"));
    }

    @Test
    public void testByteUnaryOperator() throws Exception {
        Throwables.ByteUnaryOperator<Exception> byteOp = b -> (byte) (b * 2);
        assertEquals((byte) 10, byteOp.applyAsByte((byte) 5));
    }

    @Test
    public void testShortUnaryOperator() throws Exception {
        Throwables.ShortUnaryOperator<Exception> shortOp = s -> (short) (s + 1);
        assertEquals((short) 101, shortOp.applyAsShort((short) 100));
    }

    @Test
    public void testLongUnaryOperator() throws Exception {
        Throwables.LongUnaryOperator<Exception> longOp = l -> l * l;
        assertEquals(100L, longOp.applyAsLong(10L));
    }

    @Test
    public void testFloatUnaryOperator() throws Exception {
        Throwables.FloatUnaryOperator<Exception> floatOp = f -> f * 2.0f;
        assertEquals(6.28f, floatOp.applyAsFloat(3.14f), 0.001f);
    }

    @Test
    public void testCharBinaryOperator() throws Exception {
        Throwables.CharBinaryOperator<Exception> charOp = (a, b) -> (char) Math.max(a, b);
        assertEquals('Z', charOp.applyAsChar('A', 'Z'));
    }

    @Test
    public void testByteBinaryOperator() throws Exception {
        Throwables.ByteBinaryOperator<Exception> byteOp = (a, b) -> (byte) (a + b);
        assertEquals((byte) 7, byteOp.applyAsByte((byte) 3, (byte) 4));
    }

    @Test
    public void testShortBinaryOperator() throws Exception {
        Throwables.ShortBinaryOperator<Exception> shortOp = (a, b) -> (short) (a * b);
        assertEquals((short) 12, shortOp.applyAsShort((short) 3, (short) 4));
    }

    @Test
    public void testLongBinaryOperator() throws Exception {
        Throwables.LongBinaryOperator<Exception> longOp = (a, b) -> a + b;
        assertEquals(30L, longOp.applyAsLong(10L, 20L));
    }

    @Test
    public void testFloatBinaryOperator() throws Exception {
        Throwables.FloatBinaryOperator<Exception> floatOp = (a, b) -> a + b;
        assertEquals(5.5f, floatOp.applyAsFloat(2.5f, 3.0f), 0.001f);
    }

    @Test
    public void testCharTernaryOperator() throws Exception {
        Throwables.CharTernaryOperator<Exception> charOp = (a, b, c) -> (char) (Math.max(Math.max(a, b), c));
        assertEquals('C', charOp.applyAsChar('A', 'C', 'B'));
    }

    @Test
    public void testByteTernaryOperator() throws Exception {
        Throwables.ByteTernaryOperator<Exception> byteOp = (a, b, c) -> (byte) (a + b + c);
        assertEquals((byte) 6, byteOp.applyAsByte((byte) 1, (byte) 2, (byte) 3));
    }

    @Test
    public void testShortTernaryOperator() throws Exception {
        Throwables.ShortTernaryOperator<Exception> shortOp = (a, b, c) -> (short) (a + b + c);
        assertEquals((short) 60, shortOp.applyAsShort((short) 10, (short) 20, (short) 30));
    }

    @Test
    public void testLongTernaryOperator() throws Exception {
        Throwables.LongTernaryOperator<Exception> longOp = (a, b, c) -> a * b + c;
        assertEquals(23L, longOp.applyAsLong(4L, 5L, 3L));
    }

    @Test
    public void testFloatTernaryOperator() throws Exception {
        Throwables.FloatTernaryOperator<Exception> floatOp = (a, b, c) -> a + b + c;
        assertEquals(6.0f, floatOp.applyAsFloat(1.0f, 2.0f, 3.0f), 0.001f);
    }

    @Test
    public void testByteBiPredicate() throws Exception {
        Throwables.ByteBiPredicate<Exception> byteBiPred = (a, b) -> a + b > 10;
        assertTrue(byteBiPred.test((byte) 7, (byte) 5));
        assertFalse(byteBiPred.test((byte) 2, (byte) 3));
    }

    @Test
    public void testShortBiPredicate() throws Exception {
        Throwables.ShortBiPredicate<Exception> shortBiPred = (a, b) -> a > b;
        assertTrue(shortBiPred.test((short) 200, (short) 100));
        assertFalse(shortBiPred.test((short) 50, (short) 100));
    }

    @Test
    public void testLongBiPredicate() throws Exception {
        Throwables.LongBiPredicate<Exception> longBiPred = (a, b) -> a + b > 1000L;
        assertTrue(longBiPred.test(600L, 500L));
        assertFalse(longBiPred.test(200L, 300L));
    }

    @Test
    public void testFloatBiPredicate() throws Exception {
        Throwables.FloatBiPredicate<Exception> floatBiPred = (a, b) -> Math.abs(a - b) < 0.01f;
        assertTrue(floatBiPred.test(1.234f, 1.235f));
        assertFalse(floatBiPred.test(1.0f, 2.0f));
    }

    @Test
    public void testByteBiFunction() throws Exception {
        Throwables.ByteBiFunction<String, Exception> byteBiFunc = (a, b) -> "Sum: " + (a + b);
        assertEquals("Sum: 7", byteBiFunc.apply((byte) 3, (byte) 4));
    }

    @Test
    public void testShortBiFunction() throws Exception {
        Throwables.ShortBiFunction<String, Exception> shortBiFunc = (a, b) -> "Product: " + (a * b);
        assertEquals("Product: 600", shortBiFunc.apply((short) 20, (short) 30));
    }

    @Test
    public void testLongBiFunction() throws Exception {
        Throwables.LongBiFunction<String, Exception> longBiFunc = (a, b) -> "Max: " + Math.max(a, b);
        assertEquals("Max: 200", longBiFunc.apply(100L, 200L));
    }

    @Test
    public void testFloatBiFunction() throws Exception {
        Throwables.FloatBiFunction<String, Exception> floatBiFunc = (a, b) -> String.format("Avg: %.1f", (a + b) / 2.0f);
        assertEquals("Avg: 2.5", floatBiFunc.apply(2.0f, 3.0f));
    }

    @Test
    public void testFloatToIntFunction_ReturnsInt() throws Exception {
        Throwables.FloatToIntFunction<Exception> fn = f -> Math.round(f);
        int result = fn.applyAsInt(3.7f);
        assertEquals(4, result);
        // Verify the type is genuinely int via a strict equality check on a primitive.
        int boxed = fn.applyAsInt(3.14f);
        assertEquals(3, boxed);
    }

    @Test
    public void testFloatToLongFunction_ReturnsLong() throws Exception {
        Throwables.FloatToLongFunction<Exception> fn = f -> (long) f;
        long result = fn.applyAsLong(3.99f);
        assertEquals(3L, result);
        // Truncation of large float fits in long.
        long big = fn.applyAsLong(1e9f);
        assertEquals(1_000_000_000L, big);
    }

    @Test
    public void testNFunction_andThen_OrderIsThisThenAfter() throws Throwable {
        Throwables.NFunction<Integer, Integer, Exception> doubleFirst = args -> {
            int sum = 0;
            for (Integer i : args) {
                sum += i;
            }
            return sum * 2;
        };
        // After receives the doubled sum and adds 1.
        Throwables.NFunction<Integer, Integer, Exception> chained = doubleFirst.andThen(r -> r + 1);
        // sum(1,2,3)=6 -> *2 = 12 -> +1 = 13. If ordering were reversed (after first), 1+2+3=6, after(6)=7, doubled=14.
        assertEquals(13, chained.apply(1, 2, 3).intValue());
    }

    @Test
    public void testIntNFunction_andThen_OrderIsThisThenAfter() throws Throwable {
        Throwables.IntNFunction<Integer, Exception> sumFn = args -> {
            int s = 0;
            for (int i : args) {
                s += i;
            }
            return s;
        };
        Throwables.IntNFunction<String, Exception> chained = sumFn.andThen(r -> "=" + r);
        assertEquals("=10", chained.apply(1, 2, 3, 4));
    }

    @Test
    public void testPredicate_negate_PropagatesException() {
        Throwables.Predicate<Integer, IOException> throwing = i -> {
            throw new IOException("boom");
        };
        Throwables.Predicate<Integer, IOException> negated = throwing.negate();
        IOException ex = assertThrows(IOException.class, () -> negated.test(1));
        assertEquals("boom", ex.getMessage());
    }

    @Test
    public void testRunnable_unchecked_WrapsChecked() {
        Throwables.Runnable<IOException> throwing = () -> {
            throw new IOException("io-fail");
        };
        com.landawn.abacus.util.function.Runnable wrapped = throwing.unchecked();
        RuntimeException re = assertThrows(RuntimeException.class, wrapped::run);
        assertNotNull(re);
    }

    @Test
    public void testSupplier_unchecked_PropagatesValue() {
        Throwables.Supplier<String, IOException> ok = () -> "value";
        com.landawn.abacus.util.function.Supplier<String> wrapped = ok.unchecked();
        assertEquals("value", wrapped.get());
    }

    @Test
    public void testFunction_unchecked_AppliesCorrectly() {
        Throwables.Function<Integer, Integer, IOException> sq = i -> i * i;
        com.landawn.abacus.util.function.Function<Integer, Integer> wrapped = sq.unchecked();
        assertEquals(25, wrapped.apply(5).intValue());
    }

    @Test
    public void testConsumer_unchecked_SideEffects() {
        AtomicInteger sink = new AtomicInteger();
        Throwables.Consumer<Integer, IOException> c = sink::set;
        com.landawn.abacus.util.function.Consumer<Integer> wrapped = c.unchecked();
        wrapped.accept(42);
        assertEquals(42, sink.get());
    }

    @Test
    public void reviewFixes20260906_wrapperExceptionsArePeeledBeforeConversion() {
        // Documented (F6): run/call peel an ExecutionException / InvocationTargetException /
        // UndeclaredThrowableException down to its cause and convert THAT, so the wrapper is not in the cause
        // chain at all - the plain "a checked exception is wrapped in a RuntimeException" reading is wrong for
        // these three.
        final IOException io = new IOException("boom");

        for (final Throwable wrapper : new Throwable[] { new java.util.concurrent.ExecutionException(io), new java.lang.reflect.InvocationTargetException(io),
                new java.lang.reflect.UndeclaredThrowableException(io) }) {
            final RuntimeException thrown = assertThrows(RuntimeException.class, () -> Throwables.run(() -> {
                throw wrapper;
            }));
            assertSame(io, thrown.getCause(), wrapper.getClass().getSimpleName() + " must be peeled off");
            assertTrue(thrown instanceof com.landawn.abacus.exception.UncheckedIOException);

            final RuntimeException thrown2 = assertThrows(RuntimeException.class, () -> Throwables.call(() -> {
                throw wrapper;
            }));
            assertSame(io, thrown2.getCause());
        }

        // A wrapper around a RUNTIME exception is not wrapped at all - the cause comes out as itself.
        final IllegalStateException ise = new IllegalStateException("ise");
        assertSame(ise, assertThrows(IllegalStateException.class, () -> Throwables.run(() -> {
            throw new java.util.concurrent.ExecutionException(ise);
        })));

        // Nesting is peeled all the way down.
        assertSame(io, assertThrows(RuntimeException.class, () -> Throwables.run(() -> {
            throw new java.util.concurrent.ExecutionException(new java.lang.reflect.InvocationTargetException(io));
        })).getCause());

        // Control 1 - a wrapper with no cause has nothing to peel, so it is wrapped like any checked exception.
        final java.util.concurrent.ExecutionException causeless = new java.util.concurrent.ExecutionException("nocause", null);
        assertSame(causeless, assertThrows(RuntimeException.class, () -> Throwables.run(() -> {
            throw causeless;
        })).getCause());

        // Control 2 - a NON-wrapper checked exception is still wrapped, and a runtime exception still rethrown.
        assertSame(io, assertThrows(RuntimeException.class, () -> Throwables.run(() -> {
            throw io;
        })).getCause());
        final IllegalArgumentException iae = new IllegalArgumentException("iae");
        assertSame(iae, assertThrows(IllegalArgumentException.class, () -> Throwables.run(() -> {
            throw iae;
        })));
    }

    @Test
    public void reviewFixes20260906_handlerAndPredicateSeeTheUnpeeledException() {
        // Documented (F6): only the RETHROW path peels; an actionOnError/predicate argument gets the throwable
        // exactly as it was thrown.
        final IOException io = new IOException("boom");
        final java.util.concurrent.ExecutionException wrapper = new java.util.concurrent.ExecutionException(io);

        final AtomicReference<Throwable> seen = new AtomicReference<>();
        Throwables.run(() -> {
            throw wrapper;
        }, seen::set);
        assertSame(wrapper, seen.get());

        final AtomicReference<Throwable> seenByPredicate = new AtomicReference<>();
        final com.landawn.abacus.util.function.Supplier<String> fallback = () -> "fallback";
        assertEquals("fallback", Throwables.call(() -> {
            throw wrapper;
        }, e -> {
            seenByPredicate.set(e);
            return true;
        }, fallback));
        assertSame(wrapper, seenByPredicate.get());

        // ... but when that same predicate declines, the rethrow is the peeled conversion.
        seenByPredicate.set(null);
        assertSame(io, assertThrows(RuntimeException.class, () -> Throwables.call(() -> {
            throw wrapper;
        }, e -> {
            seenByPredicate.set(e);
            return false;
        }, fallback)).getCause());
        assertSame(wrapper, seenByPredicate.get());
    }

    @Test
    public void reviewFixes20260906_uncheckedAdaptersPeelWrappersToo() {
        // Documented (F6): the class-level note covers the unchecked() adapters as well, not just run/call.
        final IOException io = new IOException("boom");

        final com.landawn.abacus.util.function.Runnable r = ((Throwables.Runnable<Throwable>) () -> {
            throw new java.util.concurrent.ExecutionException(io);
        }).unchecked();
        assertSame(io, assertThrows(RuntimeException.class, r::run).getCause());

        final com.landawn.abacus.util.function.Function<String, String> f = ((Throwables.Function<String, String, Throwable>) s -> {
            throw new java.lang.reflect.InvocationTargetException(io);
        }).unchecked();
        assertSame(io, assertThrows(RuntimeException.class, () -> f.apply("x")).getCause());

        // Control - an unwrapped checked exception through the same adapter keeps its own instance as the cause.
        final com.landawn.abacus.util.function.Runnable r2 = ((Throwables.Runnable<Throwable>) () -> {
            throw io;
        }).unchecked();
        assertSame(io, assertThrows(RuntimeException.class, r2::run).getCause());
    }

    // FINDING R09-6 (the same defect com.landawn.abacus.util.LazyInitializer was fixed for): this object is what
    // Fnn.memoize(Throwables.Supplier) and N.lazyInitChecked(Throwables.Supplier) hand back to callers, so
    // initializing under `synchronized (this)` let caller code holding that monitor block the initialization.
    // Initialization now runs under a private lock.
    @Test
    public void reviewFixes20260908_getDoesNotSynchronizeOnTheReturnedInitializer() throws Exception {
        final AtomicInteger direct = new AtomicInteger();
        final LazyInitializer<Integer, Exception> lazy = LazyInitializer.of(direct::incrementAndGet);
        assertEquals(1, getWhileHoldingMonitorOf(lazy));
        assertEquals(1, direct.get());
        assertEquals(1, lazy.get());

        // Fnn.memoize(Throwables.Supplier) returns this same class.
        final AtomicInteger viaFnn = new AtomicInteger();
        final Throwables.Supplier<Integer, Exception> fnnSupplier = viaFnn::incrementAndGet;
        final Throwables.Supplier<Integer, Exception> memoized = Fnn.memoize(fnnSupplier);
        assertEquals(1, getWhileHoldingMonitorOf(memoized));
        assertEquals(1, viaFnn.get());

        // ... and so does N.lazyInitChecked(Throwables.Supplier).
        final AtomicInteger viaN = new AtomicInteger();
        final Throwables.Supplier<Integer, Exception> nSupplier = viaN::incrementAndGet;
        final Throwables.Supplier<Integer, Exception> lazyChecked = N.lazyInitChecked(nSupplier);
        assertEquals(1, getWhileHoldingMonitorOf(lazyChecked));
        assertEquals(1, viaN.get());
    }

    /**
     * Calls {@code get()} on another thread while this thread holds the supplier's own monitor, and returns
     * whatever that call produced. Fails if the call did not finish, i.e. if it was blocked by the caller-held
     * monitor.
     */
    private static Object getWhileHoldingMonitorOf(final Throwables.Supplier<?, ?> supplier) throws Exception {
        final CountDownLatch done = new CountDownLatch(1);
        final AtomicReference<Object> result = new AtomicReference<>();
        final Thread worker = new Thread(() -> {
            try {
                result.set(supplier.get());
            } catch (final Throwable e) { // NOSONAR - the failure has to reach the assertion in the caller
                result.set(e);
            } finally {
                done.countDown();
            }
        });
        worker.setDaemon(true);

        synchronized (supplier) {
            worker.start();
            assertTrue(done.await(10, TimeUnit.SECONDS), "get() blocked on the caller-visible monitor of the returned initializer");
        }

        return result.get();
    }

    @Test
    public void testRun_ErrorIsConvertedWithoutAFallbackAndAbsorbedWithOne() {
        final Throwables.Runnable<Exception> boom = () -> {
            throw new StackOverflowError("simulated");
        };

        final RuntimeException converted = assertThrows(RuntimeException.class, () -> Throwables.run(boom));
        assertTrue(converted.getCause() instanceof StackOverflowError);
        assertEquals("simulated", converted.getCause().getMessage());

        final AtomicReference<Throwable> seen = new AtomicReference<>();
        Throwables.run(boom, seen::set);
        assertTrue(seen.get() instanceof StackOverflowError);
        assertEquals("simulated", seen.get().getMessage());
    }

    @Test
    public void testRun_NoFallbackOverloadsRestoreInterruptedStatusBeforeThrowing() {
        final Throwables.Runnable<InterruptedException> interruptedRun = () -> {
            throw new InterruptedException("stop");
        };
        final Throwables.Callable<String, InterruptedException> interruptedCall = () -> {
            throw new InterruptedException("stop");
        };

        try {
            Thread.interrupted();
            final RuntimeException fromRun = assertThrows(RuntimeException.class, () -> Throwables.run(interruptedRun));
            assertTrue(Thread.currentThread().isInterrupted());
            assertTrue(fromRun.getCause() instanceof InterruptedException);

            Thread.interrupted();
            final RuntimeException fromCall = assertThrows(RuntimeException.class, () -> Throwables.call(interruptedCall));
            assertTrue(Thread.currentThread().isInterrupted());
            assertTrue(fromCall.getCause() instanceof InterruptedException);
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    public void testUnchecked_ConvertsAnErrorToARuntimeExceptionForEveryAdapter() {
        final AssertionError boom = new AssertionError("boom");

        final Throwables.Runnable<Exception> runnable = () -> {
            throw boom;
        };
        final Throwables.Callable<String, Exception> callable = () -> {
            throw boom;
        };
        final Throwables.Supplier<String, Exception> supplier = () -> {
            throw boom;
        };
        final Throwables.Predicate<String, Exception> predicate = t -> {
            throw boom;
        };
        final Throwables.BiPredicate<String, String, Exception> biPredicate = (t, u) -> {
            throw boom;
        };
        final Throwables.Function<String, String, Exception> function = t -> {
            throw boom;
        };
        final Throwables.BiFunction<String, String, String, Exception> biFunction = (t, u) -> {
            throw boom;
        };
        final Throwables.Consumer<String, Exception> consumer = t -> {
            throw boom;
        };
        final Throwables.BiConsumer<String, String, Exception> biConsumer = (t, u) -> {
            throw boom;
        };

        assertUncheckedWrapsError(boom, runnable.unchecked()::run);
        assertUncheckedWrapsError(boom, () -> callable.unchecked().call());
        assertUncheckedWrapsError(boom, () -> supplier.unchecked().get());
        assertUncheckedWrapsError(boom, () -> predicate.unchecked().test("a"));
        assertUncheckedWrapsError(boom, () -> biPredicate.unchecked().test("a", "b"));
        assertUncheckedWrapsError(boom, () -> function.unchecked().apply("a"));
        assertUncheckedWrapsError(boom, () -> biFunction.unchecked().apply("a", "b"));
        assertUncheckedWrapsError(boom, () -> consumer.unchecked().accept("a"));
        assertUncheckedWrapsError(boom, () -> biConsumer.unchecked().accept("a", "b"));
    }

    private static void assertUncheckedWrapsError(final AssertionError expected, final org.junit.jupiter.api.function.Executable invocation) {
        // assertThrows fails outright if the Error itself escapes, because an AssertionError is not a RuntimeException.
        final RuntimeException wrapper = assertThrows(RuntimeException.class, invocation);
        assertSame(expected, wrapper.getCause());
    }
}
