package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Tuple.Tuple2;

public class ResultTest extends TestBase {

    @Test
    public void testOfSuccessAndFailure() throws Exception {
        Result<String, Exception> success = Result.success("hello");
        assertTrue(success.isSuccess());
        assertFalse(success.isFailure());
        assertEquals("hello", success.orElseIfFailure("default"));
        assertEquals("hello", success.orElseThrow());
        assertNull(success.getException());

        Result<String, Exception> nullSuccess = Result.success(null);
        assertTrue(nullSuccess.isSuccess());
        assertFalse(nullSuccess.isFailure());
        assertNull(nullSuccess.orElseIfFailure("default"));
        assertNull(nullSuccess.orElseThrow());

        IOException ex = new IOException("fail");
        Result<String, IOException> failure = Result.failure(ex);
        assertTrue(failure.isFailure());
        assertFalse(failure.isSuccess());
        assertSame(ex, failure.getException());
        assertEquals("default", failure.orElseIfFailure("default"));
        assertSame(ex, assertThrows(IOException.class, failure::orElseThrow));
        assertThrows(IllegalArgumentException.class, () -> Result.failure(null));

        Result<String, Exception> ofSuccess = Result.of("success", null);
        assertTrue(ofSuccess.isSuccess());
        assertFalse(ofSuccess.isFailure());
        Result<String, Exception> ofFailure = Result.of(null, new Exception("error"));
        assertFalse(ofFailure.isSuccess());
        assertTrue(ofFailure.isFailure());
        Result<String, Exception> bothNull = Result.of(null, null);
        assertTrue(bothNull.isSuccess());
        Result<String, Exception> bothNonNull = Result.of("value", new Exception());
        assertTrue(bothNonNull.isFailure());

        Result<Integer, IOException> typed = Result.of(42, null);
        assertEquals(42, typed.orElseThrow());
        assertTrue(Result.of(null, new IllegalArgumentException()).isFailure());
    }

    @Test
    public void testIfSuccessAndIfFailure() {
        AtomicBoolean called = new AtomicBoolean(false);
        AtomicReference<String> captured = new AtomicReference<>();
        Result<String, Exception> success = Result.of("test value", null);
        success.ifSuccess(v -> {
            called.set(true);
            captured.set(v);
        });
        assertTrue(called.get());
        assertEquals("test value", captured.get());
        called.set(false);
        Result.of(null, new Exception()).ifSuccess(v -> called.set(true));
        assertFalse(called.get());
        assertThrows(IllegalArgumentException.class, () -> success.ifSuccess(null));
        assertThrows(RuntimeException.class, () -> success.ifSuccess(v -> {
            throw new RuntimeException("Callback error");
        }));

        AtomicReference<Exception> capturedEx = new AtomicReference<>();
        Exception ex = new Exception("test error");
        Result<String, Exception> failure = Result.of(null, ex);
        failure.ifFailure(e -> {
            called.set(true);
            capturedEx.set(e);
        });
        assertTrue(called.get());
        assertSame(ex, capturedEx.get());
        called.set(false);
        success.ifFailure(e -> called.set(true));
        assertFalse(called.get());
        assertThrows(IllegalArgumentException.class, () -> failure.ifFailure(null));
        assertThrows(RuntimeException.class, () -> failure.ifFailure(e -> {
            throw new RuntimeException("Callback error");
        }));

        AtomicReference<String> branch = new AtomicReference<>();
        success.ifSuccessOrElse(v -> branch.set("success: " + v), e -> branch.set("failure"));
        assertEquals("success: test value", branch.get());
        failure.ifSuccessOrElse(v -> branch.set("success"), e -> branch.set("failure: " + e.getMessage()));
        assertEquals("failure: test error", branch.get());
        failure.ifFailureOrElse(e -> branch.set("failure: " + e.getMessage()), v -> branch.set("success: " + v));
        assertEquals("failure: test error", branch.get());
        success.ifFailureOrElse(e -> branch.set("failure"), v -> branch.set("success: " + v));
        assertEquals("success: test value", branch.get());
        assertThrows(IllegalArgumentException.class, () -> success.ifSuccessOrElse(null, e -> {
        }));
        assertThrows(IllegalArgumentException.class, () -> success.ifSuccessOrElse(v -> {
        }, null));
        assertThrows(IllegalArgumentException.class, () -> success.ifFailureOrElse(null, v -> {
        }));
        assertThrows(IllegalArgumentException.class, () -> success.ifFailureOrElse(e -> {
        }, null));

        AtomicReference<String> nullValue = new AtomicReference<>("not set");
        Result.<String, Exception> of(null, null).ifSuccess(nullValue::set);
        assertNull(nullValue.get());
    }

    @Test
    public void testOrElseIfFailure() {
        Result<String, Exception> success = Result.of("original", null);
        Result<String, Exception> failure = Result.of(null, new Exception());
        Result<String, Exception> nullSuccess = Result.of(null, null);
        assertEquals("original", success.orElseIfFailure("default"));
        assertEquals("default", failure.orElseIfFailure("default"));
        assertNull(failure.orElseIfFailure(null));
        assertNull(nullSuccess.orElseIfFailure("default"));

        assertEquals("original", success.orElseGetIfFailure(() -> "default"));
        assertEquals("default", failure.orElseGetIfFailure(() -> "default"));
        AtomicBoolean supplierCalled = new AtomicBoolean(false);
        success.orElseGetIfFailure(() -> {
            supplierCalled.set(true);
            return "not used";
        });
        assertFalse(supplierCalled.get());
        failure.orElseGetIfFailure(() -> {
            supplierCalled.set(true);
            return "used";
        });
        assertTrue(supplierCalled.get());
        assertThrows(IllegalArgumentException.class, () -> success.orElseGetIfFailure(null));
        assertThrows(IllegalArgumentException.class, () -> failure.orElseGetIfFailure(null));
    }

    @Test
    public void testOrElseThrow() throws Exception {
        Result<String, IOException> success = Result.of("value", null);
        assertEquals("value", success.orElseThrow());
        IOException ex = new IOException("test error");
        assertSame(ex, assertThrows(IOException.class, () -> Result.of(null, ex).orElseThrow()));

        Function<IOException, RuntimeException> mapper = e -> new RuntimeException("Wrapped: " + e.getMessage());
        assertEquals("value", success.orElseThrow(mapper));
        assertEquals("value", Result.of("value", (Exception) null).orElseThrow(Fn.f(e -> new RuntimeException("mapped: " + e.getMessage()))));
        assertEquals("Wrapped: original error",
                assertThrows(RuntimeException.class, () -> Result.of(null, new IOException("original error")).orElseThrow(mapper)).getMessage());
        assertEquals("mapped: original",
                assertThrows(RuntimeException.class,
                        () -> Result.of(null, new Exception("original")).orElseThrow(Fn.f(e -> new RuntimeException("mapped: " + e.getMessage()))))
                                .getMessage());
        assertThrows(IllegalArgumentException.class, () -> success.orElseThrow((Function<IOException, RuntimeException>) null));
        assertThrows(IllegalArgumentException.class, () -> Result.of(null, new Exception()).orElseThrow((Function<Exception, RuntimeException>) null));

        Supplier<RuntimeException> supplier = () -> new RuntimeException("Custom exception");
        assertEquals("value", Result.of("value", (Exception) null).orElseThrow(supplier));
        assertEquals("Custom exception", assertThrows(RuntimeException.class, () -> Result.of(null, new Exception()).orElseThrow(supplier)).getMessage());
        AtomicBoolean supplierCalled = new AtomicBoolean(false);
        Result.of("value", (Exception) null).orElseThrow((Supplier<RuntimeException>) () -> {
            supplierCalled.set(true);
            return new RuntimeException();
        });
        assertFalse(supplierCalled.get());
        assertThrows(IllegalArgumentException.class, () -> Result.of("value", (Exception) null).orElseThrow((Supplier<RuntimeException>) null));
    }

    @Test
    public void testExceptionAccessAndConvert() {
        Exception ex = new Exception("test");
        assertSame(ex, Result.of(null, ex).getException());
        assertNull(Result.of("value", (Exception) null).getException());

        Pair<String, Exception> successPair = Result.<String, Exception> of("value", null).toPair();
        assertEquals("value", successPair.left());
        assertNull(successPair.right());
        Pair<String, Exception> failurePair = Result.<String, Exception> of(null, ex).toPair();
        assertNull(failurePair.left());
        assertSame(ex, failurePair.right());
        Pair<String, Exception> bothNull = Result.<String, Exception> of(null, null).toPair();
        assertNull(bothNull.left());
        assertNull(bothNull.right());
        Pair<String, Exception> both = Result.<String, Exception> of("value", new Exception()).toPair();
        assertEquals("value", both.left());
        assertNotNull(both.right());

        Tuple2<String, Exception> successTuple = Result.<String, Exception> of("value", null).toTuple();
        assertEquals("value", successTuple._1);
        assertNull(successTuple._2);
        Tuple2<String, Exception> failureTuple = Result.<String, Exception> of(null, ex).toTuple();
        assertNull(failureTuple._1);
        assertSame(ex, failureTuple._2);
        Tuple2<String, Exception> nullTuple = Result.<String, Exception> of(null, null).toTuple();
        assertNull(nullTuple._1);
        assertNull(nullTuple._2);
    }

    @Test
    public void testRR() {
        Result.RR<String> success = Result.RR.of("value", null);
        assertTrue(success.isSuccess());
        assertFalse(success.isFailure());
        assertEquals("value", success.orElseThrow());
        assertEquals("value", success.orElseIfFailure("default"));
        assertEquals("value", success.orElseGetIfFailure(() -> "fallback"));
        AtomicReference<String> captured = new AtomicReference<>();
        success.ifSuccess(captured::set);
        assertEquals("value", captured.get());
        AtomicBoolean failureCalled = new AtomicBoolean(false);
        success.ifFailure(e -> failureCalled.set(true));
        assertFalse(failureCalled.get());

        RuntimeException ex = new RuntimeException("runtime error");
        Result.RR<String> failure = Result.RR.of(null, ex);
        assertTrue(failure.isFailure());
        assertFalse(failure.isSuccess());
        assertSame(ex, failure.getException());
        assertEquals("default", failure.orElseIfFailure("default"));
        assertEquals("fallback", failure.orElseGetIfFailure(() -> "fallback"));
        assertSame(ex, assertThrows(RuntimeException.class, failure::orElseThrow));
        failure.ifFailure(e -> failureCalled.set(true));
        assertTrue(failureCalled.get());
        failure.ifSuccess(v -> captured.set("no"));
        assertEquals("value", captured.get());

        Result.RR<String> bothNull = Result.RR.of(null, null);
        assertTrue(bothNull.isSuccess());
        Result.RR<String> bothNonNull = Result.RR.of("value", new RuntimeException());
        assertTrue(bothNonNull.isFailure());

        Pair<String, RuntimeException> pair = success.toPair();
        assertEquals("value", pair.left());
        assertNull(pair.right());
        Tuple2<String, RuntimeException> tuple = success.toTuple();
        assertEquals("value", tuple._1);
        assertNull(tuple._2);
        Pair<String, RuntimeException> failurePair = failure.toPair();
        assertNull(failurePair.left());
        assertSame(ex, failurePair.right());
    }

    @Test
    public void testEqualsHashCodeToString() {
        Result<String, Exception> success1 = Result.of("value", null);
        Result<String, Exception> success2 = Result.of("value", null);
        Result<String, Exception> success3 = Result.of("different", null);
        assertEquals(success1, success1);
        assertEquals(success1, success2);
        assertEquals(success1.hashCode(), success2.hashCode());
        assertNotEquals(success1, success3);
        assertNotEquals(success1.hashCode(), success3.hashCode());
        assertNotEquals(success1, null);
        assertNotEquals(success1, "not a result");

        Exception ex1 = new Exception("error");
        Exception ex2 = new Exception("error");
        Result<String, Exception> failure1 = Result.of(null, ex1);
        Result<String, Exception> failure2 = Result.of(null, ex1);
        Result<String, Exception> failure3 = Result.of(null, ex2);
        assertEquals(failure1, failure2);
        assertEquals(failure1.hashCode(), failure2.hashCode());
        assertNotEquals(failure1, failure3);
        assertNotEquals(success1, failure1);
        assertEquals(Result.of(null, (Exception) null), Result.of(null, (Exception) null));
        assertEquals(Result.of("value", ex1), Result.of("value", ex1));
        assertNotNull(Result.of(null, (Exception) null).hashCode());

        Result.RR<String> r1 = Result.RR.of("value", null);
        Result.RR<String> r2 = Result.RR.of("value", null);
        assertEquals(r1, r2);
        assertEquals(r1.hashCode(), r2.hashCode());
        assertNotEquals(r1, Result.RR.of("other", null));

        String successStr = success1.toString();
        assertTrue(successStr.contains("value=value"));
        assertTrue(successStr.contains("exception=null"));
        String failStr = Result.of(null, new Exception("test error")).toString();
        assertTrue(failStr.contains("value=null"));
        assertTrue(failStr.contains("test error"));
        assertTrue(Result.of(null, (Exception) null).toString().contains("null"));
        assertTrue(Result.RR.of("value", null).toString().contains("value"));
        assertTrue(Result.RR.of(null, new RuntimeException("error")).toString().contains("null"));

        Result<List<Integer>, RuntimeException> listResult = Result.success(List.of(1, 2, 3));
        Result<List<Integer>, RuntimeException> equalListResult = Result.success(List.of(1, 2, 3));
        assertEquals(listResult, equalListResult);
        assertEquals(listResult.hashCode(), equalListResult.hashCode());
        assertNotEquals(listResult, Result.success(List.of(1, 2, 4)));
        assertEquals("{value=[1, 2, 3], exception=null}", listResult.toString());
    }
}
