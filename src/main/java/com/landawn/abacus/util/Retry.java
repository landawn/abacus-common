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
 * This class provides a mechanism to retry operations in case of exceptions.
 * It allows specifying the number of retry attempts, the interval between retries, and conditions for retry.
 * The conditions can be specified as predicates that test the exceptions thrown.
 * The class is parameterized by the type {@code T} which is the type of the result of the operation to be retried.
 *
 * @param <T> The type of the result of the operation to be retried.
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
     * <p><b>Usage Example:</b></p>
     * <pre>{@code
     * Retry<Void> retry = Retry.of(3, 1000, e -> e instanceof IOException || e instanceof TimeoutException);
     * retry.run(() -> sendNotification());
     * }</pre>
     *
     * @param retryTimes The maximum number of times to retry the operation if it fails. Must be non-negative. A value of 0 means no retries.
     * @param retryIntervalInMillis The interval in milliseconds to wait between retries. Must be non-negative. A value of 0 means no delay between retries.
     * @param retryCondition A predicate that tests the thrown exception. If it returns {@code true}, the operation will be retried. Must not be {@code null}.
     * @return A new {@code Retry<Void>} instance configured with the specified parameters.
     * @throws IllegalArgumentException if {@code retryTimes} is negative, {@code retryIntervalInMillis} is negative, or {@code retryCondition} is {@code null}.
     */
    public static Retry<Void> of(final int retryTimes, final long retryIntervalInMillis, final Predicate<? super Exception> retryCondition)
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
     * <p><b>Usage Example:</b></p>
     * <pre>{@code
     * Retry<Response> retry = Retry.of(3, 500, (result, ex) ->
     *     (result != null && result.getStatusCode() == 503) || ex instanceof SocketTimeoutException);
     * Response response = retry.call(() -> httpClient.get(url));
     * }</pre>
     *
     * @param <T> The type of the result returned by the operation to be retried.
     * @param retryTimes The maximum number of times to retry the operation if it fails or returns an unsatisfactory result. Must be non-negative. A value of 0 means no retries.
     * @param retryIntervalInMillis The interval in milliseconds to wait between retries. Must be non-negative. A value of 0 means no delay between retries.
     * @param retryCondition A bi-predicate that tests both the result and the exception. The first parameter is the result (or {@code null} if an exception occurred),
     *                       and the second parameter is the exception (or {@code null} if no exception occurred). If it returns {@code true}, the operation will be retried.
     *                       Must not be {@code null}.
     * @return A new {@code Retry<T>} instance configured with the specified parameters.
     * @throws IllegalArgumentException if {@code retryTimes} is negative, {@code retryIntervalInMillis} is negative, or {@code retryCondition} is {@code null}.
     */
    public static <T> Retry<T> of(final int retryTimes, final long retryIntervalInMillis, final BiPredicate<? super T, ? super Exception> retryCondition)
            throws IllegalArgumentException {
        N.checkArgNotNegative(retryTimes, "retryTimes");
        N.checkArgNotNegative(retryIntervalInMillis, "retryIntervalInMillis");
        N.checkArgNotNull(retryCondition, "retryCondition");

        return new Retry<>(retryTimes, retryIntervalInMillis, null, retryCondition);
    }

    /**
     * Executes the specified runnable operation and retries it if it fails according to the configured retry conditions.
     *
     * <p>This method will execute the provided {@code cmd} and retry it up to {@code retryTimes} times if an exception
     * is thrown and the retry condition is satisfied. Between retries, the thread will sleep for {@code retryIntervalInMillis}
     * milliseconds if configured. If all retry attempts fail, the last exception thrown will be propagated.</p>
     *
     * <p>The retry logic is controlled by the retry conditions specified during construction:
     * <ul>
     *   <li>If {@code retryCondition} is set, the operation is retried when the predicate returns {@code true} for the thrown exception.</li>
     *   <li>If {@code retryCondition2} is set, the operation is retried when the bi-predicate returns {@code true} for {@code null} result and the thrown exception.</li>
     * </ul>
     *
     * <p><b>Usage Example:</b></p>
     * <pre>{@code
     * Retry<Void> retry = Retry.of(3, 1000, e -> e instanceof IOException);
     * retry.run(() -> performNetworkOperation());
     * }</pre>
     *
     * @param cmd The runnable operation to execute, which may throw an exception.
     * @throws Exception If the operation fails and either no retry condition is met, or all retry attempts are exhausted.
     *                   The exception thrown will be the last exception encountered during execution.
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
     * Executes the specified callable operation and retries it if it fails or returns an unsatisfactory result according to the configured retry conditions.
     *
     * <p>This method will execute the provided {@code callable} and retry it up to {@code retryTimes} times if:
     * <ul>
     *   <li>An exception is thrown and the retry condition (based on the exception) is satisfied.</li>
     *   <li>The result is returned but {@code retryCondition2} evaluates to {@code true} for the result.</li>
     * </ul>
     * Between retries, the thread will sleep for {@code retryIntervalInMillis} milliseconds if configured.
     *
     * <p>The retry logic is controlled by the retry conditions specified during construction:
     * <ul>
     *   <li>If {@code retryCondition} is set, the operation is retried when the predicate returns {@code true} for the thrown exception.</li>
     *   <li>If {@code retryCondition2} is set, the operation is retried when the bi-predicate returns {@code true} for the result and/or exception.</li>
     * </ul>
     *
     * <p><b>Usage Example:</b></p>
     * <pre>{@code
     * Retry<String> retry = Retry.of(3, 1000, (result, ex) -> result == null || ex instanceof TimeoutException);
     * String result = retry.call(() -> fetchDataFromServer());
     * }</pre>
     *
     * @param callable The callable operation to execute, which may throw an exception or return a result.
     * @return The result of the successful execution of {@code callable}.
     * @throws Exception If the operation fails and either no retry condition is met, or all retry attempts are exhausted.
     *                   The exception thrown will be the last exception encountered during execution.
     * @throws RuntimeException If all retry attempts are exhausted and the result still does not satisfy {@code retryCondition2}.
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
