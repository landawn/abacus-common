/*
 * Copyright (C) 2016 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.landawn.abacus.util;

import java.util.function.BiPredicate;
import java.util.function.Predicate;

import com.landawn.abacus.annotation.SuppressFBWarnings;
import com.landawn.abacus.exception.RetryExhaustedException;
import com.landawn.abacus.logging.Logger;
import com.landawn.abacus.logging.LoggerFactory;

/**
 * Provides a mechanism to retry operations when exceptions are thrown or when returned results
 * are unsatisfactory. The number of retry attempts, the delay between retries, and the conditions
 * under which a retry is triggered are all configurable.
 *
 * <p>Use the {@link #withFixedDelay(int, long, Predicate)} factory method for void operations or
 * the {@link #withFixedDelay(int, long, BiPredicate)} overload for operations that return a value.</p>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Retry a void operation up to 3 times on IOException
 * Retry<Void> retry = Retry.withFixedDelay(3, 1000, e -> e instanceof java.io.IOException);
 * retry.run(() -> sendNetworkRequest());
 *
 * // Retry a value-returning operation if result is null or a timeout occurs
 * Retry<String> retry2 = Retry.withFixedDelay(3, 500,
 *     (result, ex) -> result == null || ex instanceof java.util.concurrent.TimeoutException);
 * String data = retry2.call(() -> fetchDataFromServer());
 * }</pre>
 *
 * <p><b>Thread safety:</b> a {@code Retry} is immutable and holds no per-execution state, so one instance may
 * be shared as a constant and used concurrently. Each {@code run}/{@code call} keeps its own attempt counters
 * on the stack. Whether concurrent execution is <i>safe</i> beyond that depends entirely on the supplied
 * operation and predicates.</p>
 *
 * @param <R> the type of the result returned by the operation to be retried;
 *            use {@code Void} for operations that do not return a value
 * @see Predicate
 * @see BiPredicate
 * @see RetryExhaustedException
 */
@SuppressWarnings("java:S1192")
public final class Retry<R> {

    private static final Logger logger = LoggerFactory.getLogger(Retry.class);

    private final int retryTimes;

    private final long retryIntervalInMillis;

    private final Predicate<? super Exception> retryCondition;

    private final BiPredicate<? super R, ? super Exception> retryCondition2;

    /**
     * Creates a retry policy. Exactly one retry predicate is normally non-{@code null}.
     *
     * @param retryTimes the maximum number of additional attempts
     * @param retryIntervalInMillis the fixed delay between attempts, in milliseconds
     * @param retryCondition the exception-only predicate, or {@code null}
     * @param retryCondition2 the result/exception predicate, or {@code null}
     */
    Retry(final int retryTimes, final long retryIntervalInMillis, final Predicate<? super Exception> retryCondition,
            final BiPredicate<? super R, ? super Exception> retryCondition2) {

        this.retryTimes = retryTimes;
        this.retryIntervalInMillis = retryIntervalInMillis;
        this.retryCondition = retryCondition;
        this.retryCondition2 = retryCondition2;
    }

    /**
     * Creates a new {@code Retry} with the specified retry times, retry interval, and exception-based retry condition.
     *
     * <p>The retry logic is triggered only when an exception is thrown and the {@code retryCondition} predicate
     * evaluates to {@code true} for that exception. Results are never inspected, so a policy built here retries on
     * failure only - to reject an unsatisfactory <i>result</i> as well, use
     * {@link #withFixedDelay(int, long, BiPredicate)} instead.</p>
     *
     * <p>{@code T} is unconstrained here precisely because this predicate never looks at a result: use
     * {@code Retry<Void>} for {@link #run(Throwables.Runnable)}, or let {@code T} be inferred from the target type
     * when the same failure-only policy is applied to a value-returning {@link #call(java.util.concurrent.Callable)}.</p>
     *
     * <p>Between retry attempts, the thread will sleep for the specified interval. If {@code retryIntervalInMillis} is 0,
     * retries will be executed immediately without delay.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Retry up to 3 times with 1 second interval on IOException or TimeoutException
     * Retry<Void> retry = Retry.withFixedDelay(3, 1000, e -> e instanceof java.io.IOException || e instanceof java.util.concurrent.TimeoutException);
     * retry.run(() -> sendNotification());
     *
     * // The same failure-only policy applied to a value-returning call
     * Retry<String> reader = Retry.withFixedDelay(3, 1000, e -> e instanceof java.io.IOException);
     * String content = reader.call(() -> java.nio.file.Files.readString(path));
     * }</pre>
     *
     * @param <T> the type of the result returned by the operation to be retried; use {@code Void} for operations
     *            that do not return a value. It is not referenced by {@code retryCondition}.
     * @param retryTimes the maximum number of times to retry the operation if it fails. Must be non-negative. A value of 0 means no retries.
     * @param retryIntervalInMillis the interval in milliseconds to wait between retries. Must be non-negative. A value of 0 means no delay between retries.
     * @param retryCondition a predicate that tests the thrown exception. If it returns {@code true}, the operation will be retried.
     * @return a new {@code Retry} instance configured with the specified parameters.
     * @throws IllegalArgumentException if {@code retryTimes} or {@code retryIntervalInMillis} is negative, or if
     *         {@code retryCondition} is {@code null}.
     */
    public static <T> Retry<T> withFixedDelay(final int retryTimes, final long retryIntervalInMillis, final Predicate<? super Exception> retryCondition)
            throws IllegalArgumentException {
        N.checkArgNotNegative(retryTimes, cs.retryTimes);
        N.checkArgNotNegative(retryIntervalInMillis, cs.retryIntervalInMillis);
        N.checkArgNotNull(retryCondition, cs.retryCondition);

        return new Retry<>(retryTimes, retryIntervalInMillis, retryCondition, null);
    }

    /**
     * Creates a new instance of {@code Retry<T>} with the specified retry times, retry interval, and result/exception-based retry condition.
     *
     * <p>This factory method is designed for operations that return a value of type {@code T}. The retry logic
     * will be triggered when:</p>
     * <ul>
     *   <li>An exception is thrown and the {@code retryCondition} bi-predicate evaluates to {@code true} for {@code null} result and the exception.</li>
     *   <li>A result is returned but the {@code retryCondition} bi-predicate evaluates to {@code true} for the result and {@code null} exception.</li>
     * </ul>
     * <p>This allows retrying based on both exceptional outcomes and unsatisfactory results.</p>
     *
     * <p>Between retry attempts, the thread will sleep for the specified interval. If {@code retryIntervalInMillis} is 0,
     * retries will be executed immediately without delay.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Retry up to 3 times with 500ms interval on 503 status or SocketTimeoutException
     * Retry<Response> retry = Retry.withFixedDelay(3, 500, (result, ex) ->
     *     (result != null && result.getStatusCode() == 503) || ex instanceof java.net.SocketTimeoutException);
     * Response response = retry.call(() -> httpClient.get(url));
     * }</pre>
     *
     * @param <T> the type of the result returned by the operation to be retried.
     * @param retryTimes the maximum number of times to retry the operation if it fails or returns an unsatisfactory result. Must be non-negative. A value of 0 means no retries.
     * @param retryIntervalInMillis the interval in milliseconds to wait between retries. Must be non-negative. A value of 0 means no delay between retries.
     * @param retryCondition a bi-predicate tested with {@code (result, exception)} after each attempt;
     *        returns {@code true} to retry. On success the exception argument is {@code null}; on failure
     *        the result argument is {@code null}. Must not be {@code null}.
     * @return a new {@code Retry<T>} instance configured with the specified parameters.
     * @throws IllegalArgumentException if {@code retryTimes} or {@code retryIntervalInMillis} is negative, or if
     *         {@code retryCondition} is {@code null}.
     */
    public static <T> Retry<T> withFixedDelay(final int retryTimes, final long retryIntervalInMillis,
            final BiPredicate<? super T, ? super Exception> retryCondition) throws IllegalArgumentException {
        N.checkArgNotNegative(retryTimes, cs.retryTimes);
        N.checkArgNotNegative(retryIntervalInMillis, cs.retryIntervalInMillis);
        N.checkArgNotNull(retryCondition, cs.retryCondition);

        return new Retry<>(retryTimes, retryIntervalInMillis, null, retryCondition);
    }

    /**
     * Executes the specified runnable operation and retries it if it fails according to the
     * configured retry conditions.
     *
     * <p>On the first failure, the exception is tested against the retry condition(s):</p>
     * <ul>
     *   <li>If {@code retryCondition} (set via {@link #withFixedDelay(int, long, Predicate)}) is
     *       present and returns {@code true} for the thrown exception, the operation is retried.</li>
     *   <li>If {@code retryCondition2} (set via {@link #withFixedDelay(int, long, BiPredicate)}) is
     *       present and returns {@code true} for {@code (null, exception)}, the operation is
     *       retried.</li>
     * </ul>
     * <p>If the condition is not satisfied the exception is rethrown immediately. Otherwise the
     * thread sleeps for {@code retryIntervalInMillis} milliseconds (if positive) and the operation
     * is attempted again, up to {@code retryTimes} additional times. If all retries are exhausted,
     * the last exception is rethrown.</p>
     *
     * <p>The retry predicate is consulted only when another attempt could actually follow, so for an
     * operation that always fails it is evaluated at most {@code retryTimes} times, not
     * {@code retryTimes + 1}.</p>
     *
     * <p><b>Interruption:</b> the delay between attempts is interruptible. If the calling thread is
     * interrupted before or during a delay, no further attempt is started, the thread's interrupted
     * status is left set, and the last exception thrown by {@code cmd} is rethrown with the
     * {@link InterruptedException} attached to it as a suppressed exception. A cancelled caller
     * therefore never has to wait out the remaining {@code retryTimes * retryIntervalInMillis}.</p>
     *
     * <p>If {@code retryTimes} is 0, the operation is executed exactly once without any retries,
     * and the retry predicates are not evaluated.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Retry up to 3 times with 1 second interval on IOException
     * Retry<Void> retry = Retry.withFixedDelay(3, 1000, e -> e instanceof java.io.IOException);
     * retry.run(() -> performNetworkOperation());
     * }</pre>
     *
     * @param cmd the runnable operation to execute
     * @throws IllegalArgumentException if {@code cmd} is {@code null}.
     * @throws RuntimeException if a configured retry predicate throws a runtime exception; predicate
     *         failures are propagated immediately, with the operation's own exception attached to them as a
     *         suppressed exception so it is not lost
     * @throws Exception the exception thrown by {@code cmd} if the retry condition is not satisfied,
     *                   or the last exception thrown if all retry attempts are exhausted (or if the
     *                   thread was interrupted before the retries were exhausted).
     */
    public void run(final Throwables.Runnable<? extends Exception> cmd) throws IllegalArgumentException, RuntimeException, Exception {
        N.checkArgNotNull(cmd, cs.cmd);

        if (retryTimes > 0) {
            try {
                cmd.run();
            } catch (final Exception e) {
                int retriedTimes = 0;
                Exception ex = e;

                while (retriedTimes < retryTimes && shouldRetryAfter(ex)) {

                    final InterruptedException interrupted = awaitBeforeRetry();

                    if (interrupted != null) {
                        ex.addSuppressed(interrupted);
                        break;
                    }

                    retriedTimes++;

                    try {
                        logger.debug("Starting retry attempt {} of {}", retriedTimes, retryTimes);

                        cmd.run();
                        return;
                    } catch (final Exception e2) {
                        ex = e2;
                    }
                }

                throw ex;
            }
        } else {
            cmd.run();
        }
    }

    /**
     * Asks the configured exception predicate whether {@code operationFailure} is worth another attempt.
     *
     * <p>A predicate that throws is a defect in the retry <i>configuration</i>, not in the operation, so its
     * failure is what the caller must fix and it stays the primary exception - as this class has always
     * documented. But it must not erase the operation's own failure: with the predicate call inlined into the
     * {@code catch} block, a throwing predicate propagated with an empty cause and an empty suppressed list,
     * and the {@code IOException} (or whatever the operation actually threw) simply vanished. Attaching it as a
     * suppressed exception keeps both.</p>
     *
     * <p>Note this is the opposite nesting from {@link Try}'s final-action handling, which keeps the
     * <i>operation</i> failure primary. That is deliberate: a final action is cleanup running alongside the
     * operation, whereas a retry predicate is the control logic deciding whether the operation runs again -
     * when it is broken, nothing about the retry policy can be trusted.</p>
     *
     * @param operationFailure the exception thrown by the most recent attempt
     * @return {@code true} if a configured predicate accepted the failure for retry
     * @throws RuntimeException if a retry predicate throws; the operation failure, when present, is attached as a suppressed exception
     */
    private boolean shouldRetryAfter(final Exception operationFailure) throws RuntimeException {
        try {
            return (retryCondition != null && retryCondition.test(operationFailure))
                    || (retryCondition2 != null && retryCondition2.test(null, operationFailure));
        } catch (final RuntimeException | Error predicateFailure) {
            if (predicateFailure != operationFailure) {
                predicateFailure.addSuppressed(operationFailure);
            }

            throw predicateFailure;
        }
    }

    /**
     * Asks the configured result predicate whether {@code result} must be rejected and retried.
     *
     * <p>This is the result-side twin of {@link #shouldRetryAfter(Exception)} and exists for the same
     * reason: a predicate that throws must not erase the operation's own failure. An earlier attempt may
     * well have thrown before a later one returned the result now being judged, and that failure is worth
     * exactly as much here as it is two branches later, where {@code RetryExhaustedException} already
     * carries it as a suppressed exception. Inlined at the call site, the predicate's failure propagated
     * with an empty cause and an empty suppressed list, and {@code lastException} simply vanished.</p>
     *
     * @param result the value returned by the most recent attempt
     * @param lastException the exception thrown by an earlier attempt, or {@code null} if none has thrown
     * @return {@code true} if the configured result predicate rejected {@code result}
     * @throws RuntimeException if a retry predicate throws; the operation failure, when present, is attached as a suppressed exception
     */
    private boolean shouldRetryResult(final R result, final Exception lastException) throws RuntimeException {
        try {
            return retryCondition2 != null && retryCondition2.test(result, null);
        } catch (final RuntimeException | Error predicateFailure) {
            // Self-suppression throws: a predicate is free to rethrow the operation's own exception.
            if (lastException != null && predicateFailure != lastException) {
                predicateFailure.addSuppressed(lastException);
            }

            throw predicateFailure;
        }
    }

    /**
     * Waits {@code retryIntervalInMillis} before the next attempt, and reports whether the retry loop
     * must stop because the calling thread was interrupted.
     *
     * <p>This deliberately does <i>not</i> use an uninterruptible sleep. Swallowing the interrupt made a
     * retry loop uncancellable: an already-interrupted thread still slept out every remaining attempt,
     * so cancelling a task with {@code retryTimes = 5, retryIntervalInMillis = 2000} left the caller
     * waiting ten seconds for the task to notice. The interrupted status is always left set for the
     * caller, whether it was already set on entry or raised by {@link Thread#sleep(long)} (which clears
     * it before throwing).</p>
     *
     * <p>The status is also checked when the interval is 0, so that a zero-delay retry loop is
     * cancellable too.</p>
     *
     * @return the {@link InterruptedException} that ended the wait, or {@code null} if the caller
     *         should proceed with the next attempt
     */
    private InterruptedException awaitBeforeRetry() {
        if (Thread.currentThread().isInterrupted()) {
            // Already interrupted: do not start another attempt. isInterrupted() does not clear the flag,
            // so nothing needs restoring here.
            return new InterruptedException("Retry abandoned: the thread was already interrupted");
        }

        if (retryIntervalInMillis > 0) {
            try {
                Thread.sleep(retryIntervalInMillis);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt(); // Thread.sleep clears the flag; put it back for the caller.
                return e;
            }
        }

        return null;
    }

    /**
     * Executes the specified callable operation and retries it if it fails or returns an
     * unsatisfactory result according to the configured retry conditions.
     *
     * <p>The operation is retried when any of the following is {@code true} after an invocation:</p>
     * <ul>
     *   <li>An exception is thrown and {@code retryCondition} (a {@link Predicate}) is present
     *       and returns {@code true} for that exception.</li>
     *   <li>An exception is thrown and {@code retryCondition2} (a {@link BiPredicate}) is present
     *       and returns {@code true} for {@code (null, exception)}.</li>
     *   <li>A result is returned and {@code retryCondition2} is present and returns {@code true}
     *       for {@code (result, null)}.</li>
     * </ul>
     * <p>The <i>exception</i> predicate is consulted only when another attempt could actually follow,
     * so the exception thrown by the final attempt is rethrown without being tested; for an operation
     * that always throws it is therefore evaluated at most {@code retryTimes} times, matching
     * {@link #run(Throwables.Runnable)}. The <i>result</i> predicate is evaluated after every
     * successful invocation, including the final one, because its answer decides whether that result
     * is returned or rejected.</p>
     * <p>If the condition is not satisfied the exception is rethrown (or the result returned)
     * immediately. Otherwise the thread sleeps for {@code retryIntervalInMillis} milliseconds
     * (if positive) and the operation is attempted again, up to {@code retryTimes} additional
     * times.</p>
     *
     * <p>After all retries are exhausted:</p>
     * <ul>
     *   <li>If the last attempt threw an exception, that exception is rethrown.</li>
     *   <li>If the last attempt returned a result that still satisfies {@code retryCondition2}, a
     *       {@link RetryExhaustedException} is thrown carrying how many attempts were made. The rejected
     *       result itself is deliberately <b>not</b> included in the exception or its message, because it may
     *       be large or hold sensitive data that would then be copied into logs. The most recent operation
     *       exception, if any, is attached as a <i>suppressed</i> exception - not as the cause, since an
     *       earlier failure did not cause the final attempt's result to be rejected.</li>
     * </ul>
     *
     * <p><b>Interruption:</b> the delay between attempts is interruptible; see
     * {@link #run(Throwables.Runnable)} for the details. The last exception thrown by
     * {@code callable} is rethrown with the {@link InterruptedException} attached as a suppressed
     * exception, and the thread's interrupted status is left set.</p>
     *
     * <p>If {@code retryTimes} is 0, the operation is executed exactly once without any retries,
     * and its result is still checked by the result predicate. A rejected result throws RetryExhaustedException; an operation
     * exception is rethrown directly without evaluating an exception predicate.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Retry up to 3 times with 1 second interval if result is null or TimeoutException occurs
     * Retry<String> retry = Retry.withFixedDelay(3, 1000,
     *     (result, ex) -> result == null || ex instanceof java.util.concurrent.TimeoutException);
     * String result = retry.call(() -> fetchDataFromServer());
     * }</pre>
     *
     * @param callable the callable operation to execute
     * @return the result of the first invocation whose outcome does not satisfy the retry condition.
     * @throws IllegalArgumentException if {@code callable} is {@code null}.
     * @throws InterruptedException if the thread is interrupted between attempts and no operation
     *                              exception is available to carry the interruption as a suppressed exception
     * @throws RetryExhaustedException if all retry attempts are exhausted and the final invocation returned a
     *                          result that still satisfies {@code retryCondition2}
     * @throws RuntimeException if a configured retry predicate itself throws a runtime exception. Predicate failures
     *                          are propagated immediately and do not trigger another operation attempt; the
     *                          operation's own exception, if any, is attached to them as a suppressed exception.
     *                          That attachment mutates the instance the predicate threw, so a predicate must
     *                          throw a fresh exception rather than a shared or cached one, whose suppressed
     *                          list would otherwise grow by one entry on every call.
     * @throws Exception the exception thrown by {@code callable} if the retry condition is not
     *                   satisfied, or the last exception thrown if all retry attempts are exhausted
     *                   (or if the thread was interrupted before the retries were exhausted).
     */
    @SuppressFBWarnings("RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE")
    public R call(final java.util.concurrent.Callable<? extends R> callable)
            throws IllegalArgumentException, InterruptedException, RetryExhaustedException, RuntimeException, Exception {
        N.checkArgNotNull(callable, cs.callable);

        R result = null;
        Exception lastException = null;

        for (int retriedTimes = 0; retriedTimes <= retryTimes; retriedTimes++) {
            if (retriedTimes > 0) {
                final InterruptedException interrupted = awaitBeforeRetry();

                if (interrupted != null) {
                    // Cancelled mid-loop. Report the operation's own failure (that is what the caller
                    // asked this method to produce) but attach the interruption so the cause of the
                    // early exit is not lost; the interrupted status itself is already set again.
                    if (lastException != null) {
                        lastException.addSuppressed(interrupted);
                        throw lastException;
                    }

                    throw interrupted;
                }

                logger.debug("Starting retry attempt {} of {}", retriedTimes, retryTimes);
            }

            try {
                result = callable.call();
            } catch (final Exception e) {
                lastException = e;

                // Terminal attempt: this exception is being rethrown no matter what the predicate says,
                // so do not ask it. Asking anyway made `call` invoke the exception predicate once more
                // than `run` does for the same configuration, and let a predicate that throws on the
                // final attempt replace the operation's own exception with its own.
                if (retriedTimes == retryTimes) {
                    throw e;
                }

                if (!shouldRetryAfter(e)) {
                    throw e;
                }

                continue;
            }

            // Evaluate outside the callable's catch block. A predicate failure is a configuration
            // failure, not an operation failure, and must not trigger another callable invocation.
            // Unlike the exception predicate above, this one must run even on the terminal attempt:
            // its answer decides whether the final result is returned or rejected.
            if (!shouldRetryResult(result, lastException)) {
                return result;
            }

            if (retriedTimes == retryTimes) {
                // The rejected result is intentionally omitted: it may be large or carry sensitive
                // data, and this message routinely ends up in logs.
                final RetryExhaustedException exhausted = new RetryExhaustedException(
                        "The final result still matched the retry condition after " + (retryTimes + 1L) + " attempts (" + retryTimes + " retries)",
                        retryTimes + 1L, retryTimes);

                // An earlier attempt's failure is history, not the reason this result was rejected, so it is
                // suppressed rather than set as the cause. (It was the cause before, which read as though the
                // exception had propagated - even though the final attempt returned normally.)
                if (lastException != null) {
                    exhausted.addSuppressed(lastException);
                }

                throw exhausted;
            }
        }

        throw new AssertionError("Unreachable retry state");

    }

}
