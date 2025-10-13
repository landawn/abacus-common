package com.landawn.abacus.util;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Tuple.Tuple2;
import com.landawn.abacus.util.u.Optional;

@Tag("2025")
public class Result2025Test extends TestBase {

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
}
