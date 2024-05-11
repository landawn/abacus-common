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
 *
 * @author Haiyang Li
 * @param <T>
 * @since 0.8
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
     *
     * @param retryTimes
     * @param retryIntervallInMillis
     * @param retryCondition
     * @return
     */
    public static Retry<Void> of(final int retryTimes, final long retryIntervallInMillis, final Predicate<? super Exception> retryCondition) {
        N.checkArgNotNegative(retryTimes, "retryTimes");
        N.checkArgNotNegative(retryIntervallInMillis, "retryIntervallInMillis");
        N.checkArgNotNull(retryCondition);

        return new Retry<>(retryTimes, retryIntervallInMillis, retryCondition, null);
    }

    /**
     *
     * @param <T>
     * @param retryTimes
     * @param retryIntervallInMillis
     * @param retryCondition
     * @return
     */
    public static <T> Retry<T> of(final int retryTimes, final long retryIntervallInMillis, final BiPredicate<? super T, ? super Exception> retryCondition) {
        N.checkArgNotNegative(retryTimes, "retryTimes");
        N.checkArgNotNegative(retryIntervallInMillis, "retryIntervallInMillis");
        N.checkArgNotNull(retryCondition);

        return new Retry<>(retryTimes, retryIntervallInMillis, null, retryCondition);
    }

    /**
     *
     * @param cmd
     * @throws Exception the exception
     */
    public void run(final Throwables.Runnable<? extends Exception> cmd) throws Exception {
        if (retryTimes > 0) {
            try {
                cmd.run();
            } catch (Exception e) {
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
                    } catch (Exception e2) {
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
            } catch (Exception e) {
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
                    } catch (Exception e2) {
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
         */
        public static R<Void> of(final int retryTimes, final long retryIntervallInMillis, final Predicate<? super RuntimeException> retryCondition) {
            N.checkArgNotNegative(retryTimes, "retryTimes");
            N.checkArgNotNegative(retryIntervallInMillis, "retryIntervallInMillis");
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
         */
        public static <T> R<T> of(final int retryTimes, final long retryIntervallInMillis,
                final BiPredicate<? super T, ? super RuntimeException> retryCondition) {
            N.checkArgNotNegative(retryTimes, "retryTimes");
            N.checkArgNotNegative(retryIntervallInMillis, "retryIntervallInMillis");
            N.checkArgNotNull(retryCondition);

            return new R<>(retryTimes, retryIntervallInMillis, null, retryCondition);
        }

        /**
         * 
         *
         * @param cmd 
         */
        public void run(final Runnable cmd) {
            if (retryTimes > 0) {
                try {
                    cmd.run();
                } catch (RuntimeException e) {
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
                        } catch (RuntimeException e2) {
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
         *
         * @param callable
         * @return
         * @throws RuntimeException the exception
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
                } catch (RuntimeException e) {
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
                        } catch (RuntimeException e2) {
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
         *
         * @param <E>
         * @param iter
         * @return
         */
        public <E> Iterator<E> iterate(final Iterator<E> iter) {
            return iterate(iter, Integer.MAX_VALUE);
        }

        /**
         *
         * @param <E>
         * @param iter
         * @param totalRetryTimes
         * @return
         */
        public <E> Iterator<E> iterate(final Iterator<E> iter, final int totalRetryTimes) {
            N.checkArgPositive(totalRetryTimes, "totalRetryTimes");

            return new Iterator<>() {
                private int totalRetriedTimes = 0;

                @Override
                public boolean hasNext() {
                    try {
                        return iter.hasNext();
                    } catch (RuntimeException e) {
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
                            } catch (RuntimeException e2) {
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
                    } catch (RuntimeException e) {
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
                            } catch (RuntimeException e2) {
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
                    } catch (RuntimeException e) {
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
                            } catch (RuntimeException e2) {
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
