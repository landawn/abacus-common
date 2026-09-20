package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.Test;

/**
 * Group N9 (N.java 41899-47093) exception-review tests.
 * The four *_reportedBeforeNullRetryCondition tests pin the rule-3 validation order (retryTimes / retryIntervalInMillis
 * are now rejected before retryCondition); they are RED on the r9600+ baseline. The rest are green on both.
 */
public class ExcReviewN9Test extends com.landawn.abacus.TestBase {

    @Test
    public void runWithRetry_negativeRetryTimes_reportedBeforeNullRetryCondition() {
        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> N.runWithRetry(() -> {
        }, -1, 0L, null));
        assertTrue(ex.getMessage().contains("retryTimes"), ex.getMessage());
    }

    @Test
    public void runWithRetry_negativeInterval_reportedBeforeNullRetryCondition() {
        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> N.runWithRetry(() -> {
        }, 0, -1L, null));
        assertTrue(ex.getMessage().contains("retryIntervalInMillis"), ex.getMessage());
    }

    @Test
    public void callWithRetry_negativeRetryTimes_reportedBeforeNullRetryCondition() {
        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> N.callWithRetry(() -> "x", -1, 0L, null));
        assertTrue(ex.getMessage().contains("retryTimes"), ex.getMessage());
    }

    @Test
    public void callWithRetry_negativeInterval_reportedBeforeNullRetryCondition() {
        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> N.callWithRetry(() -> "x", 0, -1L, null));
        assertTrue(ex.getMessage().contains("retryIntervalInMillis"), ex.getMessage());
    }

    // ---- behaviour preserved: the same exceptions as before for single-fault inputs (green on baseline and patched)

    @Test
    public void runWithRetry_negativeRetryTimes_isIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> N.runWithRetry(() -> {
        }, -1, 0L, e -> true));
    }

    @Test
    public void runWithRetry_nullRetryCondition_isIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> N.runWithRetry(() -> {
        }, 1, 0L, null));
    }

    @Test
    public void callWithRetry_negativeInterval_isIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> N.callWithRetry(() -> "x", 0, -1L, (r, e) -> false));
    }

    @Test
    public void callWithRetry_nullRetryCondition_isIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> N.callWithRetry(() -> "x", 1, 0L, null));
    }

    @Test
    public void toRuntimeException_null_isIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> N.toRuntimeException((Exception) null));
        assertThrows(IllegalArgumentException.class, () -> N.toRuntimeException((Exception) null, true));
        assertThrows(IllegalArgumentException.class, () -> N.toRuntimeException((Throwable) null));
        assertThrows(IllegalArgumentException.class, () -> N.toRuntimeException((Throwable) null, true));
    }

    @Test
    public void callInParallel_checkedFailure_isWrappedRuntimeException() {
        assertThrows(RuntimeException.class, () -> N.callInParallel(() -> {
            throw new IOException("boom");
        }, () -> 1));
    }

    @Test
    public void runInParallel_checkedFailure_isWrappedRuntimeException() {
        assertThrows(RuntimeException.class, () -> N.runInParallel(() -> {
            throw new IOException("boom");
        }, () -> {
        }));
    }
}
