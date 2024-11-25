/*
 * Copyright (C) 2016 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.landawn.abacus.util;

import java.util.Iterator;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import com.landawn.abacus.logging.Logger;
import com.landawn.abacus.logging.LoggerFactory;

/**
 * This class provides a mechanism to retry operations in case of exceptions.
 * It allows specifying the number of retry attempts, the interval between retries, and conditions for retry.
 * The conditions can be specified as predicates that test the exceptions thrown.
 * The class is parameterized by the type {@code T} which is the type of the result of the operation to be retried.
 *
 * @param <T> The type of the result of the operation to be retried.
 *
 * @param <T>
 */
@SuppressWarnings("java:S1192")
public final class Retry<T> {

    private static final Logger logger = LoggerFactory.getLogger(Retry.class);

    private final int retryTimes;

    private final long retryIntervallInMillis;

    private final Predicate<? super Exception> retryCondition;

    /** The retry condition 2. */
    private final BiPredicate<? super T, ? super Exception> retryCondition2;

    Retry(final int retryTimes, final long retryIntervallInMillis, final Predicate<? super Exception> retryCondition,
            final BiPredicate<? super T, ? super Exception> retryCondition2) {

        this.retryTimes = retryTimes;
        this.retryIntervallInMillis = retryIntervallInMillis;
        this.retryCondition = retryCondition;
        this.retryCondition2 = retryCondition2;
    }

    /**
     * Creates a new instance of Retry<Void> with the specified retry times, retry interval, and retry condition.
     *
     * @param retryTimes The number of times to retry the operation if it fails. Must be non-negative.
     * @param retryIntervallInMillis The interval in milliseconds between retries. Must be non-negative.
     * @param retryCondition The condition to test the exceptions thrown. If the condition returns {@code true}, the operation is retried.
     * @return A new instance of Retry<Void> with the specified retry times, retry interval, and retry condition.
     * @throws IllegalArgumentException if the provided {@code retryTimes} or {@code retryIntervallInMillis} is negative, or {@code retryCondition} is {@code null}.
     */
    public static Retry<Void> of(final int retryTimes, final long retryIntervallInMillis, final Predicate<? super Exception> retryCondition)
            throws IllegalArgumentException {
        N.checkArgNotNegative(retryTimes, cs.retryTimes);
        N.checkArgNotNegative(retryIntervallInMillis, cs.retryIntervallInMillis);
        N.checkArgNotNull(retryCondition);

        return new Retry<>(retryTimes, retryIntervallInMillis, retryCondition, null);
    }

    /**
     * Creates a new instance of Retry<T> with the specified retry times, retry interval, and retry condition.
     *
     * @param <T> The type of the result of the operation to be retried.
     * @param retryTimes The number of times to retry the operation if it fails. Must be non-negative.
     * @param retryIntervallInMillis The interval in milliseconds between retries. Must be non-negative.
     * @param retryCondition The condition to test the result and the exceptions thrown. If the condition returns {@code true}, the operation is retried.
     * @return A new instance of Retry<T> with the specified retry times, retry interval, and retry condition.
     * @throws IllegalArgumentException if the provided {@code retryTimes} or {@code retryIntervallInMillis} is negative, or {@code retryCondition} is {@code null}.
     */
    public static <T> Retry<T> of(final int retryTimes, final long retryIntervallInMillis, final BiPredicate<? super T, ? super Exception> retryCondition)
            throws IllegalArgumentException {
        N.checkArgNotNegative(retryTimes, cs.retryTimes);
        N.checkArgNotNegative(retryIntervallInMillis, cs.retryIntervallInMillis);
        N.checkArgNotNull(retryCondition);

        return new Retry<>(retryTimes, retryIntervallInMillis, null, retryCondition);
    }

    /**
     * Executes the specified operation and retries it if it fails.
     *
     * @param cmd
     * @throws Exception the exception
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
                        if (retryIntervallInMillis > 0) {
                            N.sleepUninterruptibly(retryIntervallInMillis);
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
     * Executes the specified operation and retries it if it fails.
     *
     * @param callable
     * @return
     * @throws Exception the exception
     */
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

                    if (retryIntervallInMillis > 0) {
                        N.sleepUninterruptibly(retryIntervallInMillis);
                    }

                    logger.info("Start " + retriedTimes + " retry");

                    result = callable.call();

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
                        if (retryIntervallInMillis > 0) {
                            N.sleepUninterruptibly(retryIntervallInMillis);
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

            if (retryTimes > 0 && (retryCondition2 != null && retryCondition2.test(result, null))) {
                throw new RuntimeException("Still failed after retried " + retryTimes + " times for result: " + N.toString(result));
            }

            return result;
        } else {
            return callable.call();
        }
    }

    public static final class R<T> {

        @SuppressWarnings("hiding")
        private static final Logger logger = LoggerFactory.getLogger(R.class);

        private final int retryTimes;

        private final long retryIntervallInMillis;

        private final Predicate<? super RuntimeException> retryCondition;

        /** The retry condition 2. */
        private final BiPredicate<? super T, ? super RuntimeException> retryCondition2;

        R(final int retryTimes, final long retryIntervallInMillis, final Predicate<? super RuntimeException> retryCondition,
                final BiPredicate<? super T, ? super RuntimeException> retryCondition2) {

            this.retryTimes = retryTimes;
            this.retryIntervallInMillis = retryIntervallInMillis;
            this.retryCondition = retryCondition;
            this.retryCondition2 = retryCondition2;
        }

        /**
         *
         * @param retryTimes
         * @param retryIntervallInMillis
         * @param retryCondition
         * @return
         * @throws IllegalArgumentException
         */
        public static R<Void> of(final int retryTimes, final long retryIntervallInMillis, final Predicate<? super RuntimeException> retryCondition)
                throws IllegalArgumentException {
            N.checkArgNotNegative(retryTimes, cs.retryTimes);
            N.checkArgNotNegative(retryIntervallInMillis, cs.retryIntervallInMillis);
            N.checkArgNotNull(retryCondition);

            return new R<>(retryTimes, retryIntervallInMillis, retryCondition, null);
        }

        /**
         *
         * @param <T>
         * @param retryTimes
         * @param retryIntervallInMillis
         * @param retryCondition
         * @return
         * @throws IllegalArgumentException
         */
        public static <T> R<T> of(final int retryTimes, final long retryIntervallInMillis,
                final BiPredicate<? super T, ? super RuntimeException> retryCondition) throws IllegalArgumentException {
            N.checkArgNotNegative(retryTimes, cs.retryTimes);
            N.checkArgNotNegative(retryIntervallInMillis, cs.retryIntervallInMillis);
            N.checkArgNotNull(retryCondition);

            return new R<>(retryTimes, retryIntervallInMillis, null, retryCondition);
        }

        /**
         * Executes the specified operation and retries it if it fails.
         *
         * @param cmd The runnable task that might throw a runtime exception.
         * @throws RuntimeException if an exception occurs during the execution of the {@code cmd} and the retry conditions are not met.
         */
        public void run(final Runnable cmd) {
            if (retryTimes > 0) {
                try {
                    cmd.run();
                } catch (final RuntimeException e) {
                    logger.error("Failed to run", e);

                    int retriedTimes = 0;
                    RuntimeException ex = e;

                    while (retriedTimes < retryTimes
                            && ((retryCondition != null && retryCondition.test(ex)) || (retryCondition2 != null && retryCondition2.test(null, ex)))) {

                        retriedTimes++;

                        try {
                            if (retryIntervallInMillis > 0) {
                                N.sleepUninterruptibly(retryIntervallInMillis);
                            }

                            logger.info("Start " + retriedTimes + " retry");

                            cmd.run();
                            return;
                        } catch (final RuntimeException e2) {
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
         * Executes the specified callable operation and retries it if it fails.
         *
         * @param callable The callable task that might throw a runtime exception.
         * @return The result of the {@code callable}.
         * @throws RuntimeException if an exception occurs during the execution of the {@code callable} and the retry conditions are not met.
         */
        public T call(final Throwables.Callable<T, RuntimeException> callable) {
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

                        if (retryIntervallInMillis > 0) {
                            N.sleepUninterruptibly(retryIntervallInMillis);
                        }

                        logger.info("Start " + retriedTimes + " retry");

                        result = callable.call();

                        if (retryCondition2 == null || !retryCondition2.test(result, null)) {
                            return result;
                        }
                    }
                } catch (final RuntimeException e) {
                    logger.error("Failed to call", e);

                    RuntimeException ex = e;

                    if (!((retryCondition != null && retryCondition.test(ex)) || (retryCondition2 != null && retryCondition2.test(null, ex)))) {
                        throw ex;
                    }

                    while (retriedTimes < retryTimes) {
                        retriedTimes++;

                        try {
                            if (retryIntervallInMillis > 0) {
                                N.sleepUninterruptibly(retryIntervallInMillis);
                            }

                            logger.info("Start " + retriedTimes + " retry");

                            result = callable.call();

                            if (retryCondition2 == null || !retryCondition2.test(result, null)) {
                                return result;
                            }
                        } catch (final RuntimeException e2) {
                            logger.error("Retried: " + retriedTimes, e2);

                            ex = e2;

                            if (!((retryCondition != null && retryCondition.test(ex)) || (retryCondition2 != null && retryCondition2.test(null, ex)))) {
                                throw ex;
                            }
                        }
                    }

                    throw ex;
                }

                if (retryTimes > 0 && (retryCondition2 != null && retryCondition2.test(result, null))) {
                    throw new RuntimeException("Still failed after retried " + retryTimes + " times for result: " + N.toString(result));
                }

                return result;
            } else {
                return callable.call();
            }
        }

        /**
         * Creates an iterator that retries the specified iterator's operations if they fail.
         *
         * @param <E> The type of elements returned by the iterator.
         * @param iter The iterator whose operations might throw a runtime exception.
         * @param totalRetryTimes The total number of times to retry the iterator's operations if they fail.
         * @return An iterator that retries the specified iterator's operations if they fail.
         * @throws IllegalArgumentException if the provided {@code totalRetryTimes} is not positive.
         */
        public <E> Iterator<E> iterate(final Iterator<E> iter, final int totalRetryTimes) throws IllegalArgumentException {
            N.checkArgPositive(totalRetryTimes, cs.totalRetryTimes);

            return new Iterator<>() {
                private int totalRetriedTimes = 0;

                @Override
                public boolean hasNext() {
                    try {
                        return iter.hasNext();
                    } catch (final RuntimeException e) {
                        logger.error("Failed to hasNext()", e);

                        int retriedTimes = 0;
                        RuntimeException ex = e;

                        while (totalRetriedTimes < totalRetryTimes && retriedTimes < retryTimes
                                && ((retryCondition != null && retryCondition.test(ex)) || (retryCondition2 != null && retryCondition2.test(null, ex)))) {

                            totalRetriedTimes++;
                            retriedTimes++;

                            try {
                                if (retryIntervallInMillis > 0) {
                                    N.sleepUninterruptibly(retryIntervallInMillis);
                                }

                                logger.info("Start " + retriedTimes + " retry in hasNext()");

                                return iter.hasNext();
                            } catch (final RuntimeException e2) {
                                logger.error("Retried: " + retriedTimes + " in hasNext()", e2);

                                ex = e2;
                            }
                        }

                        throw ex;
                    }
                }

                @Override
                public E next() {
                    try {
                        return iter.next();
                    } catch (final RuntimeException e) {
                        logger.error("Failed to next()", e);

                        int retriedTimes = 0;
                        RuntimeException ex = e;

                        while (totalRetriedTimes < totalRetryTimes && retriedTimes < retryTimes
                                && ((retryCondition != null && retryCondition.test(ex)) || (retryCondition2 != null && retryCondition2.test(null, ex)))) {

                            totalRetriedTimes++;
                            retriedTimes++;

                            try {
                                if (retryIntervallInMillis > 0) {
                                    N.sleepUninterruptibly(retryIntervallInMillis);
                                }

                                logger.info("Start " + retriedTimes + " retry in next()");

                                return iter.next();
                            } catch (final RuntimeException e2) {
                                logger.error("Retried: " + retriedTimes + " in next()", e2);

                                ex = e2;
                            }
                        }

                        throw ex;
                    }
                }

                @Override
                public void remove() {
                    try {
                        iter.remove();
                    } catch (final RuntimeException e) {
                        logger.error("Failed to remove()", e);

                        int retriedTimes = 0;
                        RuntimeException ex = e;

                        while (totalRetriedTimes < totalRetryTimes && retriedTimes < retryTimes
                                && ((retryCondition != null && retryCondition.test(ex)) || (retryCondition2 != null && retryCondition2.test(null, ex)))) {

                            totalRetriedTimes++;
                            retriedTimes++;

                            try {
                                if (retryIntervallInMillis > 0) {
                                    N.sleepUninterruptibly(retryIntervallInMillis);
                                }

                                logger.info("Start " + retriedTimes + " retry in remove()");

                                iter.remove();
                            } catch (final RuntimeException e2) {
                                logger.error("Retried: " + retriedTimes + " in remove()", e2);

                                ex = e2;
                            }
                        }

                        throw ex;
                    }
                }
            };
        }
    }
}
