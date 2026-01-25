package com.landawn.abacus.util;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Tuple.Tuple2;
import com.landawn.abacus.util.u.Optional;

@Tag("new-test")
public class Result100Test extends TestBase {

    @Test
    public void testOf() {
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
    public void testIfFailure() {
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
    public void testIfSuccess() {
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
    public void testGetException() {
        Exception ex = new Exception("test");
        Result<String, Exception> failure = Result.of(null, ex);
        Assertions.assertSame(ex, failure.getException());

        Result<String, Exception> success = Result.of("value", null);
        Assertions.assertNull(success.getException());
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
