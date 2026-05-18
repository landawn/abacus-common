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
 * Retry<Void> retry = Retry.withFixedDelay(3, 1000, e -> e instanceof IOException);
 * retry.run(() -> sendNetworkRequest());
 *
 * // Retry a value-returning operation if result is null or a timeout occurs
 * Retry<String> retry2 = Retry.withFixedDelay(3, 500,
 *     (result, ex) -> result == null || ex instanceof TimeoutException);
 * String data = retry2.call(() -> fetchDataFromServer());
 * }</pre>
 *
 * @param <T> the type of the result returned by the operation to be retried;
 *            use {@code Void} for operations that do not return a value
 * @see Predicate
 * @see BiPredicate
 */
@SuppressWarnings("java:S1192")
public final class Retry<T> {

    private static final Logger logger = LoggerFactory.getLogger(Retry.class);

    private final int retryTimes;

    private final long retryIntervalInMillis;

    private final Predicate<? super Exception> retryCondition;

    private final BiPredicate<? super T, ? super Exception> retryCondition2;

    Retry(final int retryTimes, final long retryIntervalInMillis, final Predicate<? super Exception> retryCondition,
            final BiPredicate<? super T, ? super Exception> retryCondition2) {

        this.retryTimes = retryTimes;
        this.retryIntervalInMillis = retryIntervalInMillis;
        this.retryCondition = retryCondition;
        this.retryCondition2 = retryCondition2;
    }

    /**
     * Creates a new instance of {@code Retry<Void>} with the specified retry times, retry interval, and exception-based retry condition.
     *
     * <p>This factory method is designed for operations that do not return a value (void operations). The retry logic
     * will be triggered only when an exception is thrown and the {@code retryCondition} predicate evaluates to {@code true}
     * for that exception.</p>
     *
     * <p>Between retry attempts, the thread will sleep for the specified interval. If {@code retryIntervalInMillis} is 0,
     * retries will be executed immediately without delay.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Retry up to 3 times with 1 second interval on IOException or TimeoutException
     * Retry<Void> retry = Retry.withFixedDelay(3, 1000, e -> e instanceof java.io.IOException || e instanceof java.util.concurrent.TimeoutException);
     * retry.run(() -> sendNotification());
     * }</pre>
     *
     * @param retryTimes the maximum number of times to retry the operation if it fails. Must be non-negative. A value of 0 means no retries.
     * @param retryIntervalInMillis the interval in milliseconds to wait between retries. Must be non-negative. A value of 0 means no delay between retries.
     * @param retryCondition a predicate that tests the thrown exception. If it returns {@code true}, the operation will be retried. Must not be {@code null}.
     * @return a new {@code Retry<Void>} instance configured with the specified parameters.
     * @throws IllegalArgumentException if {@code retryTimes} is negative, {@code retryIntervalInMillis} is negative, or {@code retryCondition} is {@code null}.
     */
    public static Retry<Void> withFixedDelay(final int retryTimes, final long retryIntervalInMillis, final Predicate<? super Exception> retryCondition)
            throws IllegalArgumentException {
        N.checkArgNotNegative(retryTimes, "retryTimes");
        N.checkArgNotNegative(retryIntervalInMillis, "retryIntervalInMillis");
        N.checkArgNotNull(retryCondition, "retryCondition");

        return new Retry<>(retryTimes, retryIntervalInMillis, retryCondition, null);
    }

    /**
     * Creates a new instance of {@code Retry<T>} with the specified retry times, retry interval, and result/exception-based retry condition.
     *
     * <p>This factory method is designed for operations that return a value of type {@code T}. The retry logic
     * will be triggered when:
     * <ul>
     *   <li>An exception is thrown and the {@code retryCondition} bi-predicate evaluates to {@code true} for {@code null} result and the exception.</li>
     *   <li>A result is returned but the {@code retryCondition} bi-predicate evaluates to {@code true} for the result and {@code null} exception.</li>
     * </ul>
     * This allows retrying based on both exceptional outcomes and unsatisfactory results.
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
     * @param retryCondition a bi-predicate that tests both the result and the exception. The first parameter is the result (or {@code null} if an exception occurred),
     *                       and the second parameter is the exception (or {@code null} if no exception occurred). If it returns {@code true}, the operation will be retried.
     *                       Must not be {@code null}.
     * @return a new {@code Retry<T>} instance configured with the specified parameters.
     * @throws IllegalArgumentException if {@code retryTimes} is negative, {@code retryIntervalInMillis} is negative, or {@code retryCondition} is {@code null}.
     */
    public static <T> Retry<T> withFixedDelay(final int retryTimes, final long retryIntervalInMillis,
            final BiPredicate<? super T, ? super Exception> retryCondition) throws IllegalArgumentException {
        N.checkArgNotNegative(retryTimes, "retryTimes");
        N.checkArgNotNegative(retryIntervalInMillis, "retryIntervalInMillis");
        N.checkArgNotNull(retryCondition, "retryCondition");

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
     * <p>If {@code retryTimes} is 0, the operation is executed exactly once without any retries.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Retry up to 3 times with 1 second interval on IOException
     * Retry<Void> retry = Retry.withFixedDelay(3, 1000, e -> e instanceof java.io.IOException);
     * retry.run(() -> performNetworkOperation());
     * }</pre>
     *
     * @param cmd the runnable operation to execute; must not be {@code null}.
     * @throws Exception the exception thrown by {@code cmd} if the retry condition is not satisfied,
     *                   or the last exception thrown if all retry attempts are exhausted.
     */
    public void run(final Throwables.Runnable<? extends Exception> cmd) throws Exception {
        if (retryTimes > 0) {
            try {
                cmd.run();
            } catch (final Exception e) {
                logger.error("Failed to run", e);

                int retriedTimes = 0;
                Exception ex = e;

                while (retriedTimes < retryTimes
                        && ((retryCondition != null && retryCondition.test(ex)) || (retryCondition2 != null && retryCondition2.test(null, ex)))) {

                    retriedTimes++;

                    try {
                        if (retryIntervalInMillis > 0) {
                            N.sleepUninterruptibly(retryIntervalInMillis);
                        }

                        logger.info("Start " + retriedTimes + " retry");

                        cmd.run();
                        return;
                    } catch (final Exception e2) {
                        logger.error("Retried: " + retriedTimes, e2);

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
     * Executes the specified callable operation and retries it if it fails or returns an
     * unsatisfactory result according to the configured retry conditions.
     *
     * <p>The operation is retried when any of the following is true after an invocation:</p>
     * <ul>
     *   <li>An exception is thrown and {@code retryCondition} (a {@link Predicate}) is present
     *       and returns {@code true} for that exception.</li>
     *   <li>An exception is thrown and {@code retryCondition2} (a {@link BiPredicate}) is present
     *       and returns {@code true} for {@code (null, exception)}.</li>
     *   <li>A result is returned and {@code retryCondition2} is present and returns {@code true}
     *       for {@code (result, null)}.</li>
     * </ul>
     * <p>If the condition is not satisfied the exception is rethrown (or the result returned)
     * immediately. Otherwise the thread sleeps for {@code retryIntervalInMillis} milliseconds
     * (if positive) and the operation is attempted again, up to {@code retryTimes} additional
     * times.</p>
     *
     * <p>After all retries are exhausted:</p>
     * <ul>
     *   <li>If the last attempt threw an exception, that exception is rethrown.</li>
     *   <li>If the last attempt returned a result that still satisfies {@code retryCondition2},
     *       a {@link RuntimeException} is thrown describing the persistent failure.</li>
     * </ul>
     *
     * <p>If {@code retryTimes} is 0, the operation is executed exactly once without any retries.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Retry up to 3 times with 1 second interval if result is null or TimeoutException occurs
     * Retry<String> retry = Retry.withFixedDelay(3, 1000,
     *     (result, ex) -> result == null || ex instanceof java.util.concurrent.TimeoutException);
     * String result = retry.call(() -> fetchDataFromServer());
     * }</pre>
     *
     * @param callable the callable operation to execute; must not be {@code null}.
     * @return the result of the first invocation whose outcome does not satisfy the retry condition.
     * @throws RuntimeException if all retry attempts are exhausted and the final invocation returned
     *                          a result that still satisfies {@code retryCondition2}.
     * @throws Exception the exception thrown by {@code callable} if the retry condition is not
     *                   satisfied, or the last exception thrown if all retry attempts are exhausted.
     */
    @SuppressFBWarnings("RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE")
    public T call(final java.util.concurrent.Callable<T> callable) throws Exception {
        if (retryTimes > 0) {
            T result = null;
            int retriedTimes = 0;

            try {
                result = callable.call();

                if (retryCondition2 == null || !retryCondition2.test(result, null)) {
                    return result;
                }

                while (retriedTimes < retryTimes) {
                    retriedTimes++;

                    if (retryIntervalInMillis > 0) {
                        N.sleepUninterruptibly(retryIntervalInMillis);
                    }

                    logger.info("Start " + retriedTimes + " retry");

                    result = callable.call();

                    //noinspection ConstantValue
                    if (retryCondition2 == null || !retryCondition2.test(result, null)) {
                        return result;
                    }
                }
            } catch (final Exception e) {
                logger.error("Failed to call", e);

                Exception ex = e;

                if (!((retryCondition != null && retryCondition.test(ex)) || (retryCondition2 != null && retryCondition2.test(null, ex)))) {
                    throw ex;
                }

                while (retriedTimes < retryTimes) {
                    retriedTimes++;

                    try {
                        if (retryIntervalInMillis > 0) {
                            N.sleepUninterruptibly(retryIntervalInMillis);
                        }

                        logger.info("Start " + retriedTimes + " retry");

                        result = callable.call();

                        if (retryCondition2 == null || !retryCondition2.test(result, null)) {
                            return result;
                        }
                    } catch (final Exception e2) {
                        logger.error("Retried: " + retriedTimes, e2);

                        ex = e2;

                        if (!((retryCondition != null && retryCondition.test(ex)) || (retryCondition2 != null && retryCondition2.test(null, ex)))) {
                            throw ex;
                        }
                    }
                }

                throw ex;
            }

            //noinspection ConstantValue
            if (retryTimes > 0 && (retryCondition2 != null && retryCondition2.test(result, null))) {
                throw new RuntimeException("Still failed after retried " + retryTimes + " times for result: " + N.toString(result));
            }

            return result;
        } else {
            return callable.call();
        }
    }

}
