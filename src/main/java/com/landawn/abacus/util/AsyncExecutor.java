/*
 * Copyright (c) 2015, Haiyang Li.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.landawn.abacus.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import com.landawn.abacus.annotation.Internal;
import com.landawn.abacus.logging.Logger;
import com.landawn.abacus.logging.LoggerFactory;

/**
 *
 * @author Haiyang Li
 * @since 0.8
 */
public class AsyncExecutor {

    private static final Logger logger = LoggerFactory.getLogger(AsyncExecutor.class);

    private static final int DEFAULT_CORE_POOL_SIZE = Math.max(8, IOUtil.CPU_CORES);

    private static final int DEFAULT_MAX_THREAD_POOL_SIZE = Math.max(16, IOUtil.CPU_CORES);

    private final int coreThreadPoolSize;

    private final int maxThreadPoolSize;

    private final long keepAliveTime;

    private final TimeUnit unit;

    private volatile Executor executor;

    public AsyncExecutor() {
        this(DEFAULT_CORE_POOL_SIZE, DEFAULT_MAX_THREAD_POOL_SIZE, 180L, TimeUnit.SECONDS);
    }

    public AsyncExecutor(int coreThreadPoolSize, int maxThreadPoolSize, long keepAliveTime, TimeUnit unit) {
        N.checkArgNotNegative(coreThreadPoolSize, "coreThreadPoolSize");
        N.checkArgNotNegative(maxThreadPoolSize, "maxThreadPoolSize");
        N.checkArgNotNegative(keepAliveTime, "keepAliveTime");
        N.checkArgNotNull(unit, "unit");

        this.coreThreadPoolSize = coreThreadPoolSize;
        this.maxThreadPoolSize = Math.max(coreThreadPoolSize, maxThreadPoolSize);
        this.keepAliveTime = keepAliveTime;
        this.unit = unit;

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                shutdown();
            }
        });
    }

    public AsyncExecutor(final Executor executor) {
        this(getCorePoolSize(executor), getMaximumPoolSize(executor), getKeepAliveTime(executor), TimeUnit.MILLISECONDS);

        this.executor = executor;
    }

    private static int getCorePoolSize(final Executor executor) {
        return executor instanceof ThreadPoolExecutor ? ((ThreadPoolExecutor) executor).getCorePoolSize() : DEFAULT_CORE_POOL_SIZE;
    }

    private static int getMaximumPoolSize(final Executor executor) {
        return executor instanceof ThreadPoolExecutor ? ((ThreadPoolExecutor) executor).getMaximumPoolSize() : DEFAULT_CORE_POOL_SIZE;
    }

    private static long getKeepAliveTime(final Executor executor) {
        return executor instanceof ThreadPoolExecutor ? ((ThreadPoolExecutor) executor).getKeepAliveTime(TimeUnit.MILLISECONDS)
                : TimeUnit.SECONDS.toMillis(180);
    }

    /**
     *
     * @param command
     * @return
     */
    public ContinuableFuture<Void> execute(final Throwables.Runnable<? extends Exception> command) {
        return execute(new FutureTask<>(() -> {
            command.run();
            return null;
        }));
    }

    /**
     *
     * @param command
     * @param onComplete
     * @return
     */
    public ContinuableFuture<Void> execute(final Throwables.Runnable<? extends Exception> command, final java.lang.Runnable onComplete) {
        return execute(new FutureTask<>(() -> {
            try {
                command.run();
                return null;
            } finally {
                onComplete.run();
            }
        }));
    }

    /**
     *
     * @param commands
     * @return
     * @deprecated
     */
    @Deprecated
    @SafeVarargs
    public final List<ContinuableFuture<Void>> execute(final Throwables.Runnable<? extends Exception>... commands) {
        if (N.isNullOrEmpty(commands)) {
            return new ArrayList<>();
        }

        final List<ContinuableFuture<Void>> results = new ArrayList<>(commands.length);

        for (com.landawn.abacus.util.Throwables.Runnable<? extends Exception> command : commands) {
            results.add(execute(command));
        }

        return results;
    }

    /**
     *
     * @param commands
     * @return
     */
    public List<ContinuableFuture<Void>> execute(final List<? extends Throwables.Runnable<? extends Exception>> commands) {
        if (N.isNullOrEmpty(commands)) {
            return new ArrayList<>();
        }

        final List<ContinuableFuture<Void>> results = new ArrayList<>(commands.size());

        for (Throwables.Runnable<? extends Exception> cmd : commands) {
            results.add(execute(cmd));
        }

        return results;
    }

    /**
     *
     * @param <R>
     * @param command
     * @return
     */
    public <R> ContinuableFuture<R> execute(final Callable<R> command) {
        return execute(new FutureTask<>(command));
    }

    /**
     *
     * @param <R>
     * @param command
     * @param onComplete
     * @return
     */
    public <R> ContinuableFuture<R> execute(final Callable<R> command, final java.lang.Runnable onComplete) {
        return execute(new FutureTask<>(() -> {
            try {
                return command.call();
            } finally {
                onComplete.run();
            }
        }));
    }

    /**
     *
     * @param <R>
     * @param commands
     * @return
     * @deprecated
     */
    @Deprecated
    @SafeVarargs
    public final <R> List<ContinuableFuture<R>> execute(final Callable<R>... commands) {
        if (N.isNullOrEmpty(commands)) {
            return new ArrayList<>();
        }

        final List<ContinuableFuture<R>> results = new ArrayList<>(commands.length);

        for (Callable<R> command : commands) {
            results.add(execute(command));
        }

        return results;
    }

    /**
     *
     * @param <R>
     * @param commands
     * @return
     */
    public <R> List<ContinuableFuture<R>> execute(final Collection<? extends Callable<R>> commands) {
        if (N.isNullOrEmpty(commands)) {
            return new ArrayList<>();
        }

        final List<ContinuableFuture<R>> results = new ArrayList<>(commands.size());

        for (Callable<R> cmd : commands) {
            results.add(execute(cmd));
        }

        return results;
    }

    /**
     *
     * @param action
     * @param retryTimes
     * @param retryInterval
     * @param retryCondition
     * @return
     */
    public ContinuableFuture<Void> execute(final Throwables.Runnable<? extends Exception> action, final int retryTimes, final long retryInterval,
            final Predicate<? super Exception> retryCondition) {
        return execute((Callable<Void>) () -> {
            Retry.of(retryTimes, retryInterval, retryCondition).run(action);
            return null;
        });
    }

    /**
     *
     * @param <R>
     * @param action
     * @param retryTimes
     * @param retryInterval
     * @param retryCondition
     * @return
     */
    public <R> ContinuableFuture<R> execute(final Callable<R> action, final int retryTimes, final long retryInterval,
            final BiPredicate<? super R, ? super Exception> retryCondition) {
        return execute((Callable<R>) () -> {
            final Retry<R> retry = Retry.of(retryTimes, retryInterval, retryCondition);
            return retry.call(action);
        });
    }

    /**
     *
     * @param <R>
     * @param futureTask
     * @return
     */
    <R> ContinuableFuture<R> execute(final FutureTask<R> futureTask) {
        final Executor executor = getExecutor();

        executor.execute(futureTask);

        return new ContinuableFuture<>(futureTask, null, executor);
    }

    /**
     * Gets the executor.
     *
     * @return
     */
    @Internal
    public Executor getExecutor() {
        if (executor == null) {
            synchronized (this) {
                if (executor == null) {
                    final ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(coreThreadPoolSize, maxThreadPoolSize, keepAliveTime, unit,
                            new LinkedBlockingQueue<Runnable>());
                    threadPoolExecutor.allowCoreThreadTimeOut(true);
                    executor = threadPoolExecutor;
                }
            }
        }

        return executor;
    }

    @Override
    public String toString() {
        final String activeCount = executor instanceof ThreadPoolExecutor ? "" + ((ThreadPoolExecutor) executor).getActiveCount() : "?";

        return "{coreThreadPoolSize: " + coreThreadPoolSize + ", maxThreadPoolSize: " + maxThreadPoolSize + ", activeCount: " + activeCount
                + ", keepAliveTime: " + unit.toMillis(keepAliveTime) + "ms, Executor: " + N.toString(executor) + "}";
    }

    public synchronized void shutdown() {
        if (executor == null || !(executor instanceof ExecutorService executorService)) {
            return;
        }

        logger.warn("Starting to shutdown task in AsyncExecutor");

        try {
            executorService.shutdown();

            if (!executorService.isTerminated()) {
                executorService.awaitTermination(60, TimeUnit.SECONDS);
            }
        } catch (InterruptedException e) {
            logger.warn("Not all the requests/tasks executed in AsyncExecutor are completed successfully before shutdown.");
        } finally {
            executor = null;
            logger.warn("Completed to shutdown task in AsyncExecutor");
        }

    }

    @Override
    public void finalize() {
        shutdown();
    }
}
