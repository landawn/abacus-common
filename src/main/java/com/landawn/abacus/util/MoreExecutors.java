/*
 * Copyright (C) 2007 The Guava Authors
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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Factory and utility methods for {@link java.util.concurrent.Executor}, {@link ExecutorService},
 * and {@link ThreadFactory}.
 *
 * @author Eric Fellheimer
 * @author Kyle Littlefield
 * @author Justin Mahoney
 */
public final class MoreExecutors {

    private MoreExecutors() {
        // utility class
    }

    /**
     * Gets the exiting executor service.
     *
     * @param executor
     * @return
     */
    public static ExecutorService getExitingExecutorService(final ThreadPoolExecutor executor) {
        return getExitingExecutorService(executor, 120, TimeUnit.SECONDS);
    }

    /**
     * Gets the exiting executor service.
     *
     * @param executor
     * @param terminationTimeout
     * @param timeUnit
     * @return
     */
    public static ExecutorService getExitingExecutorService(final ThreadPoolExecutor executor, final long terminationTimeout, final TimeUnit timeUnit) {
        useDaemonThreadFactory(executor);
        final ExecutorService service = Executors.unconfigurableExecutorService(executor);
        addDelayedShutdownHook(service, terminationTimeout, timeUnit);
        return service;
    }

    /**
     * Gets the exiting scheduled executor service.
     *
     * @param executor
     * @return
     */
    public static ScheduledExecutorService getExitingScheduledExecutorService(final ScheduledThreadPoolExecutor executor) {
        return getExitingScheduledExecutorService(executor, 120, TimeUnit.SECONDS);
    }

    /**
     * Gets the exiting scheduled executor service.
     *
     * @param executor
     * @param terminationTimeout
     * @param timeUnit
     * @return
     */
    public static ScheduledExecutorService getExitingScheduledExecutorService(final ScheduledThreadPoolExecutor executor, final long terminationTimeout,
            final TimeUnit timeUnit) {
        useDaemonThreadFactory(executor);
        final ScheduledExecutorService service = Executors.unconfigurableScheduledExecutorService(executor);
        addDelayedShutdownHook(service, terminationTimeout, timeUnit);
        return service;
    }

    /**
     * Adds the delayed shutdown hook.
     *
     * @param service
     * @param terminationTimeout
     * @param timeUnit
     * @throws IllegalArgumentException
     */
    public static void addDelayedShutdownHook(final ExecutorService service, final long terminationTimeout, final TimeUnit timeUnit)
            throws IllegalArgumentException {
        N.checkArgNotNull(service);
        N.checkArgNotNull(timeUnit);
        addShutdownHook(MoreExecutors.newThread("DelayedShutdownHook-for-" + service, () -> {
            try {
                // We'd like to log progress and failures that may arise in the
                // following code, but unfortunately the behavior of logging
                // is undefined in shutdown hooks.
                // This is because the logging code installs a shutdown hook of its
                // own. See Cleaner class inside {@link LogManager}.
                service.shutdown();
                //noinspection ResultOfMethodCallIgnored
                service.awaitTermination(terminationTimeout, timeUnit);
            } catch (final InterruptedException ignored) {
                // We're shutting down anyway, so just ignore.
            }
        }));
    }

    /**
     * Adds the shutdown hook.
     *
     * @param hook
     */
    static void addShutdownHook(final Thread hook) {
        Runtime.getRuntime().addShutdownHook(hook);
    }

    /**
     * Use daemon thread factory.
     *
     * @param executor
     */
    private static void useDaemonThreadFactory(final ThreadPoolExecutor executor) {
        executor.setThreadFactory(new ThreadFactory() {
            private final ThreadFactory impl = executor.getThreadFactory();

            @Override
            public Thread newThread(final Runnable r) {
                final Thread res = impl.newThread(r);
                res.setDaemon(true);
                return res;
            }
        });
    }

    /**
     *
     * @param name
     * @param runnable
     * @return
     */
    static Thread newThread(final String name, final Runnable runnable) {
        N.checkArgNotNull(name);
        N.checkArgNotNull(runnable);

        final Thread result = Executors.defaultThreadFactory().newThread(runnable);
        try {
            result.setName(name);
        } catch (final SecurityException e) {
            // OK if we can't set the name in this environment.
        }
        return result;
    }
}
