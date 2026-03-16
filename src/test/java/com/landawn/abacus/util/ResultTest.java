package com.landawn.abacus.util;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Tuple.Tuple2;
import com.landawn.abacus.util.u.Optional;

@Tag("2025")
public class ResultTest extends TestBase {

    @Test
    public void testSuccess() {
        Result<String, Exception> result = Result.success("hello");
        Assertions.assertTrue(result.isSuccess());
        Assertions.assertFalse(result.isFailure());
        Assertions.assertEquals("hello", result.orElseIfFailure("default"));
        Assertions.assertNull(result.getException());

        Result<String, Exception> nullSuccess = Result.success(null);
        Assertions.assertTrue(nullSuccess.isSuccess());
        Assertions.assertNull(nullSuccess.orElseIfFailure("default"));
    }

    @Test
    public void testFailure() {
        IOException ex = new IOException("fail");
        Result<String, IOException> result = Result.failure(ex);
        Assertions.assertTrue(result.isFailure());
        Assertions.assertFalse(result.isSuccess());
        Assertions.assertSame(ex, result.getException());
        Assertions.assertEquals("default", result.orElseIfFailure("default"));

        Assertions.assertThrows(IOException.class, () -> result.orElseThrow());
    }

    @Test
    public void test_of() {
        Result<String, Exception> successResult = Result.of("success", null);
        Assertions.assertTrue(successResult.isSuccess());
        Assertions.assertFalse(successResult.isFailure());

        Exception exception = new Exception("error");
        Result<String, Exception> failureResult = Result.of(null, exception);
        Assertions.assertFalse(failureResult.isSuccess());
        Assertions.assertTrue(failureResult.isFailure());

        Result<String, Exception> bothNull = Result.of(null, null);
        Assertions.assertTrue(bothNull.isSuccess());
        Assertions.assertFalse(bothNull.isFailure());

        Result<String, Exception> bothNonNull = Result.of("value", new Exception());
        Assertions.assertFalse(bothNonNull.isSuccess());
        Assertions.assertTrue(bothNonNull.isFailure());
    }

    @Test
    public void test_isFailure() {
        Result<String, Exception> success = Result.of("value", null);
        Assertions.assertFalse(success.isFailure());

        Result<String, Exception> failure = Result.of(null, new Exception());
        Assertions.assertTrue(failure.isFailure());

        Result<String, Exception> nullSuccess = Result.of(null, null);
        Assertions.assertFalse(nullSuccess.isFailure());
    }

    @Test
    public void test_isSuccess() {
        Result<String, Exception> success = Result.of("value", null);
        Assertions.assertTrue(success.isSuccess());

        Result<String, Exception> failure = Result.of(null, new Exception());
        Assertions.assertFalse(failure.isSuccess());

        Result<String, Exception> nullSuccess = Result.of(null, null);
        Assertions.assertTrue(nullSuccess.isSuccess());
    }

    @Test
    public void testIfFailure_NullAction() {
        Result<String, Exception> failure = Result.of(null, new Exception("error"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> failure.ifFailure(null));

        Result<String, Exception> success = Result.of("value", null);
        Assertions.assertThrows(IllegalArgumentException.class, () -> success.ifFailure(null));
    }

    @Test
    public void testIfSuccess_NullAction() {
        Result<String, Exception> success = Result.of("value", null);
        Assertions.assertThrows(IllegalArgumentException.class, () -> success.ifSuccess(null));

        Result<String, Exception> failure = Result.of(null, new Exception("error"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> failure.ifSuccess(null));
    }

    @Test
    public void test_ifFailure() {
        AtomicBoolean called = new AtomicBoolean(false);
        AtomicReference<Exception> capturedEx = new AtomicReference<>();

        Exception ex = new Exception("test error");
        Result<String, Exception> failure = Result.of(null, ex);

        failure.ifFailure(e -> {
            called.set(true);
            capturedEx.set(e);
        });

        Assertions.assertTrue(called.get());
        Assertions.assertSame(ex, capturedEx.get());

        called.set(false);
        Result<String, Exception> success = Result.of("value", null);
        success.ifFailure(e -> called.set(true));
        Assertions.assertFalse(called.get());
    }

    @Test
    public void test_ifFailureOrElse() {
        AtomicReference<String> result = new AtomicReference<>();

        Exception ex = new Exception("error");
        Result<String, Exception> failure = Result.of("ignored", ex);

        failure.ifFailureOrElse(e -> result.set("failure: " + e.getMessage()), v -> result.set("success: " + v));

        Assertions.assertEquals("failure: error", result.get());

        Result<String, Exception> success = Result.of("value", null);

        success.ifFailureOrElse(e -> result.set("failure"), v -> result.set("success: " + v));

        Assertions.assertEquals("success: value", result.get());

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            success.ifFailureOrElse(null, v -> {
            });
        });

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            success.ifFailureOrElse(e -> {
            }, null);
        });
    }

    @Test
    public void test_ifSuccess() {
        AtomicBoolean called = new AtomicBoolean(false);
        AtomicReference<String> capturedValue = new AtomicReference<>();

        Result<String, Exception> success = Result.of("test value", null);

        success.ifSuccess(v -> {
            called.set(true);
            capturedValue.set(v);
        });

        Assertions.assertTrue(called.get());
        Assertions.assertEquals("test value", capturedValue.get());

        called.set(false);
        Result<String, Exception> failure = Result.of(null, new Exception());
        failure.ifSuccess(v -> called.set(true));
        Assertions.assertFalse(called.get());
    }

    @Test
    public void test_ifSuccessOrElse() {
        AtomicReference<String> result = new AtomicReference<>();

        Result<String, Exception> success = Result.of("value", null);

        success.ifSuccessOrElse(v -> result.set("success: " + v), e -> result.set("failure: " + e.getMessage()));

        Assertions.assertEquals("success: value", result.get());

        Exception ex = new Exception("error");
        Result<String, Exception> failure = Result.of(null, ex);

        failure.ifSuccessOrElse(v -> result.set("success"), e -> result.set("failure: " + e.getMessage()));

        Assertions.assertEquals("failure: error", result.get());

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            success.ifSuccessOrElse(null, e -> {
            });
        });

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            success.ifSuccessOrElse(v -> {
            }, null);
        });
    }

    @Test
    public void test_orElseIfFailure() {
        Result<String, Exception> success = Result.of("original", null);
        String value1 = success.orElseIfFailure("default");
        Assertions.assertEquals("original", value1);

        Result<String, Exception> failure = Result.of(null, new Exception());
        String value2 = failure.orElseIfFailure("default");
        Assertions.assertEquals("default", value2);

        String value3 = failure.orElseIfFailure(null);
        Assertions.assertNull(value3);

        Result<String, Exception> nullSuccess = Result.of(null, null);
        String value4 = nullSuccess.orElseIfFailure("default");
        Assertions.assertNull(value4);
    }

    @Test
    public void test_orElseGetIfFailure() {
        Result<String, Exception> success = Result.of("original", null);
        String value1 = success.orElseGetIfFailure(() -> "default");
        Assertions.assertEquals("original", value1);

        Result<String, Exception> failure = Result.of(null, new Exception());
        String value2 = failure.orElseGetIfFailure(() -> "default");
        Assertions.assertEquals("default", value2);

        AtomicBoolean supplierCalled = new AtomicBoolean(false);
        success.orElseGetIfFailure(() -> {
            supplierCalled.set(true);
            return "not used";
        });
        Assertions.assertFalse(supplierCalled.get());

        failure.orElseGetIfFailure(() -> {
            supplierCalled.set(true);
            return "used";
        });
        Assertions.assertTrue(supplierCalled.get());

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            success.orElseGetIfFailure(null);
        });
    }

    @Test
    public void test_orElseThrow_noArgs() {
        Result<String, IOException> success = Result.of("value", null);

        Assertions.assertDoesNotThrow(() -> {
            String value = success.orElseThrow();
            Assertions.assertEquals("value", value);
        });

        IOException ex = new IOException("test error");
        Result<String, IOException> failure = Result.of(null, ex);

        IOException thrown = Assertions.assertThrows(IOException.class, () -> {
            failure.orElseThrow();
        });

        Assertions.assertSame(ex, thrown);
    }

    @Test
    public void test_orElseThrow_withFunction() {
        Result<String, IOException> success = Result.of("value", null);

        Function<IOException, RuntimeException> mapper = e -> new RuntimeException("Wrapped: " + e.getMessage());

        Assertions.assertDoesNotThrow(() -> {
            String value = success.orElseThrow(mapper);
            Assertions.assertEquals("value", value);
        });

        IOException ex = new IOException("original error");
        Result<String, IOException> failure = Result.of(null, ex);

        RuntimeException thrown = Assertions.assertThrows(RuntimeException.class, () -> {
            failure.orElseThrow(mapper);
        });

        Assertions.assertEquals("Wrapped: original error", thrown.getMessage());

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            success.orElseThrow((Function<IOException, RuntimeException>) null);
        });
    }

    @Test
    public void test_orElseThrow_withSupplier() {
        Result<String, Exception> success = Result.of("value", null);

        Supplier<RuntimeException> supplier = () -> new RuntimeException("Custom exception");

        Assertions.assertDoesNotThrow(() -> {
            String value = success.orElseThrow(supplier);
            Assertions.assertEquals("value", value);
        });

        Result<String, Exception> failure = Result.of(null, new Exception());

        RuntimeException thrown = Assertions.assertThrows(RuntimeException.class, () -> {
            failure.orElseThrow(supplier);
        });

        Assertions.assertEquals("Custom exception", thrown.getMessage());

        AtomicBoolean supplierCalled = new AtomicBoolean(false);
        success.orElseThrow((Supplier<RuntimeException>) () -> {
            supplierCalled.set(true);
            return new RuntimeException();
        });
        Assertions.assertFalse(supplierCalled.get());

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            success.orElseThrow((Supplier<RuntimeException>) null);
        });
    }

    @Test
    @SuppressWarnings("deprecation")
    public void test_orElseThrow_withException() {
        Result<String, Exception> success = Result.of("value", null);

        RuntimeException ex = new RuntimeException("Custom exception");

        Assertions.assertDoesNotThrow(() -> {
            String value = success.orElseThrow(ex);
            Assertions.assertEquals("value", value);
        });

        Result<String, Exception> failure = Result.of(null, new Exception());

        RuntimeException thrown = Assertions.assertThrows(RuntimeException.class, () -> {
            failure.orElseThrow(ex);
        });

        Assertions.assertSame(ex, thrown);
    }

    @Test
    public void test_getException() {
        Exception ex = new Exception("test");
        Result<String, Exception> failure = Result.of(null, ex);

        Assertions.assertSame(ex, failure.getException());

        Result<String, Exception> success = Result.of("value", null);
        Assertions.assertNull(success.getException());
    }

    @Test
    @SuppressWarnings("deprecation")
    public void test_getExceptionIfPresent() {
        Exception ex = new Exception("test");
        Result<String, Exception> failure = Result.of(null, ex);

        Optional<Exception> optionalEx = failure.getExceptionIfPresent();
        Assertions.assertTrue(optionalEx.isPresent());
        Assertions.assertSame(ex, optionalEx.get());

        Result<String, Exception> success = Result.of("value", null);
        Optional<Exception> optionalNull = success.getExceptionIfPresent();
        Assertions.assertFalse(optionalNull.isPresent());
    }

    @Test
    public void test_toPair() {
        Result<String, Exception> success = Result.of("value", null);
        Pair<String, Exception> pair1 = success.toPair();

        Assertions.assertEquals("value", pair1.left());
        Assertions.assertNull(pair1.right());

        Exception ex = new Exception("error");
        Result<String, Exception> failure = Result.of(null, ex);
        Pair<String, Exception> pair2 = failure.toPair();

        Assertions.assertNull(pair2.left());
        Assertions.assertSame(ex, pair2.right());

        Result<String, Exception> bothNull = Result.of(null, null);
        Pair<String, Exception> pair3 = bothNull.toPair();
        Assertions.assertNull(pair3.left());
        Assertions.assertNull(pair3.right());

        Result<String, Exception> bothNonNull = Result.of("value", new Exception());
        Pair<String, Exception> pair4 = bothNonNull.toPair();
        Assertions.assertEquals("value", pair4.left());
        Assertions.assertNotNull(pair4.right());
    }

    @Test
    public void test_toTuple() {
        Result<String, Exception> success = Result.of("value", null);
        Tuple2<String, Exception> tuple1 = success.toTuple();

        Assertions.assertEquals("value", tuple1._1);
        Assertions.assertNull(tuple1._2);

        Exception ex = new Exception("error");
        Result<String, Exception> failure = Result.of(null, ex);
        Tuple2<String, Exception> tuple2 = failure.toTuple();

        Assertions.assertNull(tuple2._1);
        Assertions.assertSame(ex, tuple2._2);

        Result<String, Exception> bothNull = Result.of(null, null);
        Tuple2<String, Exception> tuple3 = bothNull.toTuple();
        Assertions.assertNull(tuple3._1);
        Assertions.assertNull(tuple3._2);
    }

    @Test
    public void test_hashCode() {
        Result<String, Exception> success1 = Result.of("value", null);
        Result<String, Exception> success2 = Result.of("value", null);
        Result<String, Exception> success3 = Result.of("different", null);

        Assertions.assertEquals(success1.hashCode(), success2.hashCode());

        Assertions.assertNotNull(success3.hashCode());

        Exception ex1 = new Exception("error");
        Result<String, Exception> failure1 = Result.of(null, ex1);
        Result<String, Exception> failure2 = Result.of(null, ex1);

        Assertions.assertEquals(failure1.hashCode(), failure2.hashCode());

        Result<String, Exception> nullResult = Result.of(null, null);
        Assertions.assertNotNull(nullResult.hashCode());
    }

    @Test
    public void test_equals() {
        Result<String, Exception> success1 = Result.of("value", null);
        Result<String, Exception> success2 = Result.of("value", null);
        Result<String, Exception> success3 = Result.of("different", null);

        Assertions.assertEquals(success1, success1);

        Assertions.assertEquals(success1, success2);
        Assertions.assertEquals(success2, success1);

        Assertions.assertNotEquals(success1, success3);

        Assertions.assertNotEquals(success1, null);

        Assertions.assertNotEquals(success1, "string");

        Exception ex1 = new Exception("error");
        Exception ex2 = new Exception("error");
        Result<String, Exception> failure1 = Result.of(null, ex1);
        Result<String, Exception> failure2 = Result.of(null, ex1);
        Result<String, Exception> failure3 = Result.of(null, ex2);

        Assertions.assertEquals(failure1, failure2);

        Assertions.assertNotEquals(failure1, failure3);

        Assertions.assertNotEquals(success1, failure1);

        Result<String, Exception> nullResult1 = Result.of(null, null);
        Result<String, Exception> nullResult2 = Result.of(null, null);
        Assertions.assertEquals(nullResult1, nullResult2);
    }

    @Test
    public void test_toString() {
        Result<String, Exception> success = Result.of("value", null);
        String str1 = success.toString();
        Assertions.assertTrue(str1.contains("value"));
        Assertions.assertTrue(str1.contains("null"));

        Exception ex = new Exception("error");
        Result<String, Exception> failure = Result.of(null, ex);
        String str2 = failure.toString();
        Assertions.assertTrue(str2.contains("null"));
        Assertions.assertTrue(str2.contains("Exception"));

        Result<String, Exception> bothNull = Result.of(null, null);
        String str3 = bothNull.toString();
        Assertions.assertTrue(str3.contains("null"));
    }

    @Test
    public void test_RR_of() {
        Result.RR<String> successResult = Result.RR.of("success", null);
        Assertions.assertTrue(successResult.isSuccess());
        Assertions.assertFalse(successResult.isFailure());

        RuntimeException exception = new RuntimeException("error");
        Result.RR<String> failureResult = Result.RR.of(null, exception);
        Assertions.assertFalse(failureResult.isSuccess());
        Assertions.assertTrue(failureResult.isFailure());

        Result.RR<String> bothNull = Result.RR.of(null, null);
        Assertions.assertTrue(bothNull.isSuccess());
        Assertions.assertFalse(bothNull.isFailure());

        Result.RR<String> bothNonNull = Result.RR.of("value", new RuntimeException());
        Assertions.assertFalse(bothNonNull.isSuccess());
        Assertions.assertTrue(bothNonNull.isFailure());
    }

    @Test
    public void test_RR_operations() {
        Result.RR<String> success = Result.RR.of("value", null);

        Assertions.assertEquals("value", success.orElseThrow());
        Assertions.assertEquals("value", success.orElseIfFailure("default"));

        RuntimeException ex = new RuntimeException("error");
        Result.RR<String> failure = Result.RR.of(null, ex);

        Assertions.assertEquals("default", failure.orElseIfFailure("default"));
        Assertions.assertSame(ex, failure.getException());

        RuntimeException thrown = Assertions.assertThrows(RuntimeException.class, () -> {
            failure.orElseThrow();
        });
        Assertions.assertSame(ex, thrown);
    }

    @Test
    public void test_RR_ifFailureAndIfSuccess() {
        AtomicBoolean failureCalled = new AtomicBoolean(false);
        AtomicBoolean successCalled = new AtomicBoolean(false);

        RuntimeException ex = new RuntimeException("error");
        Result.RR<String> failure = Result.RR.of(null, ex);
        failure.ifFailure(e -> failureCalled.set(true));
        Assertions.assertTrue(failureCalled.get());

        failure.ifSuccess(v -> successCalled.set(true));
        Assertions.assertFalse(successCalled.get());

        Result.RR<String> success = Result.RR.of("value", null);
        AtomicReference<String> capturedValue = new AtomicReference<>();
        success.ifSuccess(v -> capturedValue.set(v));
        Assertions.assertEquals("value", capturedValue.get());
    }

    @Test
    public void test_RR_toPairAndToTuple() {
        Result.RR<String> success = Result.RR.of("value", null);
        Pair<String, RuntimeException> pair = success.toPair();
        Assertions.assertEquals("value", pair.left());
        Assertions.assertNull(pair.right());

        Tuple2<String, RuntimeException> tuple = success.toTuple();
        Assertions.assertEquals("value", tuple._1);
        Assertions.assertNull(tuple._2);

        RuntimeException ex = new RuntimeException("error");
        Result.RR<String> failure = Result.RR.of(null, ex);
        Pair<String, RuntimeException> failurePair = failure.toPair();
        Assertions.assertNull(failurePair.left());
        Assertions.assertSame(ex, failurePair.right());
    }

    @Test
    public void test_RR_equalsAndHashCode() {
        Result.RR<String> r1 = Result.RR.of("value", null);
        Result.RR<String> r2 = Result.RR.of("value", null);
        Result.RR<String> r3 = Result.RR.of("other", null);

        Assertions.assertEquals(r1, r2);
        Assertions.assertEquals(r1.hashCode(), r2.hashCode());
        Assertions.assertNotEquals(r1, r3);
    }

    @Test
    public void test_RR_toString() {
        Result.RR<String> success = Result.RR.of("value", null);
        String str = success.toString();
        Assertions.assertTrue(str.contains("value"));

        RuntimeException ex = new RuntimeException("error");
        Result.RR<String> failure = Result.RR.of(null, ex);
        String failStr = failure.toString();
        Assertions.assertTrue(failStr.contains("null"));
    }

    @Test
    public void test_RR_orElseGetIfFailure() {
        Result.RR<String> success = Result.RR.of("value", null);
        Assertions.assertEquals("value", success.orElseGetIfFailure(() -> "fallback"));

        Result.RR<String> failure = Result.RR.of(null, new RuntimeException("error"));
        Assertions.assertEquals("fallback", failure.orElseGetIfFailure(() -> "fallback"));
    }

    @Test
    public void test_edgeCases_nullValue() throws Exception {
        Result<String, Exception> nullValueSuccess = Result.of(null, null);
        Assertions.assertTrue(nullValueSuccess.isSuccess());
        Assertions.assertNull(nullValueSuccess.orElseThrow());

        AtomicReference<String> capturedValue = new AtomicReference<>("not set");
        nullValueSuccess.ifSuccess(v -> capturedValue.set(v));
        Assertions.assertNull(capturedValue.get());
    }

    @Test
    public void test_edgeCases_exceptionsInCallbacks() {
        Result<String, Exception> success = Result.of("value", null);

        Assertions.assertThrows(RuntimeException.class, () -> {
            success.ifSuccess(v -> {
                throw new RuntimeException("Callback error");
            });
        });

        Result<String, Exception> failure = Result.of(null, new Exception());

        Assertions.assertThrows(RuntimeException.class, () -> {
            failure.ifFailure(e -> {
                throw new RuntimeException("Callback error");
            });
        });
    }

    @Test
    public void test_typeCompatibility() throws IOException {
        Result<Integer, IOException> ioResult = Result.of(42, null);
        Assertions.assertEquals(42, ioResult.orElseThrow());

        Result<Integer, IllegalArgumentException> illegalArgResult = Result.of(null, new IllegalArgumentException());
        Assertions.assertTrue(illegalArgResult.isFailure());

        Result<String, Exception> genericResult = Result.of("test", null);
        Assertions.assertEquals("test", genericResult.orElseIfFailure("fallback"));
    }

    @Test
    public void testIsFailure() {
        Result<String, Exception> success = Result.of("value", null);
        Assertions.assertFalse(success.isFailure());

        Result<String, Exception> failure = Result.of(null, new Exception());
        Assertions.assertTrue(failure.isFailure());
    }

    @Test
    public void testIsSuccess() {
        Result<String, Exception> success = Result.of("value", null);
        Assertions.assertTrue(success.isSuccess());

        Result<String, Exception> failure = Result.of(null, new Exception());
        Assertions.assertFalse(failure.isSuccess());
    }

    @Test
    public void testIfFailureOrElse() {
        AtomicReference<String> result = new AtomicReference<>();

        Exception ex = new Exception("error");
        Result<String, Exception> failure = Result.of("ignored", ex);

        failure.ifFailureOrElse(e -> result.set("failure: " + e.getMessage()), v -> result.set("success: " + v));

        Assertions.assertEquals("failure: error", result.get());

        Result<String, Exception> success = Result.of("value", null);

        success.ifFailureOrElse(e -> result.set("failure"), v -> result.set("success: " + v));

        Assertions.assertEquals("success: value", result.get());
    }

    @Test
    public void testIfSuccessOrElse() {
        AtomicReference<String> result = new AtomicReference<>();

        Result<String, Exception> success = Result.of("value", null);

        success.ifSuccessOrElse(v -> result.set("success: " + v), e -> result.set("failure: " + e.getMessage()));

        Assertions.assertEquals("success: value", result.get());

        Exception ex = new Exception("error");
        Result<String, Exception> failure = Result.of(null, ex);

        failure.ifSuccessOrElse(v -> result.set("success"), e -> result.set("failure: " + e.getMessage()));

        Assertions.assertEquals("failure: error", result.get());
    }

    @Test
    public void testOrDefaultIfFailure() {
        Result<String, Exception> success = Result.of("success value", null);
        Assertions.assertEquals("success value", success.orElseIfFailure("default"));

        Result<String, Exception> failure = Result.of(null, new Exception());
        Assertions.assertEquals("default", failure.orElseIfFailure("default"));

        Assertions.assertNull(failure.orElseIfFailure(null));
    }

    @Test
    public void testOrElseGetIfFailure() {
        Result<String, Exception> success = Result.of("success value", null);
        Assertions.assertEquals("success value", success.orElseGetIfFailure(() -> "default"));

        Result<String, Exception> failure = Result.of(null, new Exception());
        Assertions.assertEquals("default", failure.orElseGetIfFailure(() -> "default"));

        AtomicBoolean supplierCalled = new AtomicBoolean(false);
        success.orElseGetIfFailure(() -> {
            supplierCalled.set(true);
            return "not used";
        });
        Assertions.assertFalse(supplierCalled.get());
    }

    @Test
    public void testOrElseGetIfFailureNullSupplier() {
        Result<String, Exception> failure = Result.of(null, new Exception());
        Assertions.assertThrows(IllegalArgumentException.class, () -> failure.orElseGetIfFailure(null));
    }

    @Test
    public void testOrElseThrow() throws Exception {
        Result<String, Exception> success = Result.of("value", null);
        Assertions.assertEquals("value", success.orElseThrow());

        Exception ex = new Exception("test error");
        Result<String, Exception> failure = Result.of(null, ex);

        Exception thrown = Assertions.assertThrows(Exception.class, () -> failure.orElseThrow());
        Assertions.assertSame(ex, thrown);
    }

    @Test
    public void testOrElseThrowWithMapper() {
        Result<String, Exception> success = Result.of("value", null);
        Assertions.assertEquals("value", success.orElseThrow(Fn.f(e -> new RuntimeException("mapped: " + e.getMessage()))));

        Exception ex = new Exception("original");
        Result<String, Exception> failure = Result.of(null, ex);

        RuntimeException thrown = Assertions.assertThrows(RuntimeException.class,
                () -> failure.orElseThrow(Fn.f(e -> new RuntimeException("mapped: " + e.getMessage()))));
        Assertions.assertEquals("mapped: original", thrown.getMessage());
    }

    @Test
    public void testOrElseThrowWithMapperNull() {
        Result<String, Exception> failure = Result.of(null, new Exception());
        Assertions.assertThrows(IllegalArgumentException.class, () -> failure.orElseThrow((java.util.function.Function<Exception, RuntimeException>) null));
    }

    @Test
    public void testOrElseThrowWithSupplier() {
        Result<String, Exception> success = Result.of("value", null);
        Assertions.assertEquals("value", success.orElseThrow((Supplier<? extends RuntimeException>) () -> new RuntimeException("should not be thrown")));

        Result<String, Exception> failure = Result.of(null, new Exception("original"));

        RuntimeException thrown = Assertions.assertThrows(RuntimeException.class,
                () -> failure.orElseThrow((Supplier<? extends RuntimeException>) () -> new RuntimeException("custom error")));
        Assertions.assertEquals("custom error", thrown.getMessage());
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testOrElseThrowDeprecated() {
        Result<String, Exception> success = Result.of("value", null);
        RuntimeException customEx = new RuntimeException("custom");
        Assertions.assertEquals("value", success.orElseThrow(customEx));

        Result<String, Exception> failure = Result.of(null, new Exception());

        RuntimeException thrown = Assertions.assertThrows(RuntimeException.class, () -> failure.orElseThrow(customEx));
        Assertions.assertSame(customEx, thrown);
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testGetExceptionIfPresent() {
        Exception ex = new Exception("test");
        Result<String, Exception> failure = Result.of(null, ex);
        Optional<Exception> opt = failure.getExceptionIfPresent();
        Assertions.assertTrue(opt.isPresent());
        Assertions.assertSame(ex, opt.get());

        Result<String, Exception> success = Result.of("value", null);
        Assertions.assertFalse(success.getExceptionIfPresent().isPresent());
    }

    @Test
    public void testToPair() {
        Result<String, Exception> success = Result.of("value", null);
        Pair<String, Exception> pair = success.toPair();
        Assertions.assertEquals("value", pair.left());
        Assertions.assertNull(pair.right());

        Exception ex = new Exception("error");
        Result<String, Exception> failure = Result.of(null, ex);
        pair = failure.toPair();
        Assertions.assertNull(pair.left());
        Assertions.assertSame(ex, pair.right());
    }

    @Test
    public void testToTuple() {
        Result<String, Exception> success = Result.of("value", null);
        Tuple2<String, Exception> tuple = success.toTuple();
        Assertions.assertEquals("value", tuple._1);
        Assertions.assertNull(tuple._2);

        Exception ex = new Exception("error");
        Result<String, Exception> failure = Result.of(null, ex);
        tuple = failure.toTuple();
        Assertions.assertNull(tuple._1);
        Assertions.assertSame(ex, tuple._2);
    }

    @Test
    public void testHashCode() {
        Result<String, Exception> result1 = Result.of("value", null);
        Result<String, Exception> result2 = Result.of("value", null);
        Assertions.assertEquals(result1.hashCode(), result2.hashCode());

        Exception ex = new Exception("error");
        Result<String, Exception> failure1 = Result.of(null, ex);
        Result<String, Exception> failure2 = Result.of(null, ex);
        Assertions.assertEquals(failure1.hashCode(), failure2.hashCode());

        Result<String, Exception> different = Result.of("different", null);
        Assertions.assertNotEquals(result1.hashCode(), different.hashCode());
    }

    @Test
    public void testEquals() {
        Result<String, Exception> result1 = Result.of("value", null);
        Result<String, Exception> result2 = Result.of("value", null);
        Result<String, Exception> result3 = Result.of("different", null);

        Assertions.assertEquals(result1, result1);

        Assertions.assertEquals(result1, result2);
        Assertions.assertEquals(result2, result1);

        Assertions.assertNotEquals(result1, result3);

        Exception ex1 = new Exception("error");
        Exception ex2 = new Exception("error");
        Result<String, Exception> failure1 = Result.of(null, ex1);
        Result<String, Exception> failure2 = Result.of(null, ex1);
        Result<String, Exception> failure3 = Result.of(null, ex2);

        Assertions.assertEquals(failure1, failure2);
        Assertions.assertNotEquals(failure1, failure3);

        Assertions.assertNotEquals(result1, null);
        Assertions.assertNotEquals(result1, "not a result");

        Result<String, Exception> both = Result.of("value", ex1);
        Result<String, Exception> both2 = Result.of("value", ex1);
        Assertions.assertEquals(both, both2);
    }

    @Test
    public void testToString() {
        Result<String, Exception> success = Result.of("test value", null);
        String str = success.toString();
        Assertions.assertTrue(str.contains("value=test value"));
        Assertions.assertTrue(str.contains("exception=null"));

        Exception ex = new Exception("test error");
        Result<String, Exception> failure = Result.of(null, ex);
        str = failure.toString();
        Assertions.assertTrue(str.contains("value=null"));
        Assertions.assertTrue(str.contains("test error"));
    }

    @Test
    public void testRR() {
        Result.RR<String> success = Result.RR.of("value", null);
        Assertions.assertTrue(success.isSuccess());
        Assertions.assertEquals("value", success.orElseThrow());

        RuntimeException ex = new RuntimeException("runtime error");
        Result.RR<String> failure = Result.RR.of(null, ex);
        Assertions.assertTrue(failure.isFailure());

        RuntimeException thrown = Assertions.assertThrows(RuntimeException.class, () -> failure.orElseThrow());
        Assertions.assertSame(ex, thrown);
    }

    @Test
    public void testNullArgumentValidation() {
        Result<String, Exception> result = Result.of("value", null);

        Assertions.assertThrows(IllegalArgumentException.class, () -> result.ifFailureOrElse(null, v -> {
        }));

        Assertions.assertThrows(IllegalArgumentException.class, () -> result.ifFailureOrElse(e -> {
        }, null));

        Assertions.assertThrows(IllegalArgumentException.class, () -> result.ifSuccessOrElse(null, e -> {
        }));

        Assertions.assertThrows(IllegalArgumentException.class, () -> result.ifSuccessOrElse(v -> {
        }, null));
    }

    @Test
    public void testWithNullValue() throws Exception {
        Result<String, Exception> nullSuccess = Result.of(null, null);
        Assertions.assertTrue(nullSuccess.isSuccess());
        Assertions.assertNull(nullSuccess.orElseIfFailure("default"));
        Assertions.assertNull(nullSuccess.orElseThrow());

        AtomicBoolean called = new AtomicBoolean(false);
        nullSuccess.ifSuccess(v -> {
            called.set(true);
            Assertions.assertNull(v);
        });
        Assertions.assertTrue(called.get());
    }

}
