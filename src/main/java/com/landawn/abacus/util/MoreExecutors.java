/*
 * Copyright (C) 2007 The Guava Authors
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
 * <p>This class provides utilities for creating executor services that automatically shut down
 * when the JVM exits, preventing threads from keeping the JVM alive unnecessarily. The executors
 * created by this class use daemon threads and register shutdown hooks for graceful termination.</p>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * ThreadPoolExecutor executor = new ThreadPoolExecutor(...);
 * ExecutorService exitingExecutor = MoreExecutors.getExitingExecutorService(executor);
 * // The executor will now shut down automatically when the JVM exits
 * }</pre>
 * 
 * <p>Note: This class is adapted from Google Guava.</p>
 * 
 * @author Eric Fellheimer
 * @author Kyle Littlefield
 * @author Justin Mahoney
 */
public final class MoreExecutors {

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private MoreExecutors() {
        // utility class
    }

    /**
     * Converts the given ThreadPoolExecutor into an ExecutorService that exits when the JVM exits.
     * Uses a default termination timeout of 120 seconds.
     * 
     * <p>This method performs the following actions:</p>
     * <ul>
     *   <li>Configures the executor to use daemon threads</li>
     *   <li>Wraps it in an unconfigurable executor service</li>
     *   <li>Registers a shutdown hook to terminate the executor on JVM exit</li>
     * </ul>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ThreadPoolExecutor threadPool = new ThreadPoolExecutor(
     *     5, 10, 60L, TimeUnit.SECONDS,
     *     new LinkedBlockingQueue<>()
     * );
     * ExecutorService exitingService = MoreExecutors.getExitingExecutorService(threadPool);
     * }</pre>
     * 
     * @param executor the executor to modify
     * @return an unconfigurable ExecutorService that will shut down on JVM exit
     * @see #getExitingExecutorService(ThreadPoolExecutor, long, TimeUnit)
     */
    public static ExecutorService getExitingExecutorService(final ThreadPoolExecutor executor) {
        return getExitingExecutorService(executor, 120, TimeUnit.SECONDS);
    }

    /**
     * Converts the given ThreadPoolExecutor into an ExecutorService that exits when the JVM exits,
     * with a custom termination timeout.
     * 
     * <p>This method performs the following actions:</p>
     * <ul>
     *   <li>Configures the executor to use daemon threads</li>
     *   <li>Wraps it in an unconfigurable executor service</li>
     *   <li>Registers a shutdown hook to terminate the executor on JVM exit</li>
     * </ul>
     * 
     * <p>The shutdown hook will call {@code shutdown()} on the executor and then wait up to
     * the specified timeout for termination. If the executor doesn't terminate within
     * the timeout, the shutdown hook will exit anyway.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ThreadPoolExecutor threadPool = new ThreadPoolExecutor(...);
     * ExecutorService exitingService = MoreExecutors.getExitingExecutorService(
     *     threadPool, 30, TimeUnit.SECONDS
     * );
     * }</pre>
     * 
     * @param executor the executor to modify
     * @param terminationTimeout the maximum time to wait for the executor to terminate
     * @param timeUnit the time unit for the termination timeout
     * @return an unconfigurable ExecutorService that will shut down on JVM exit
     */
    public static ExecutorService getExitingExecutorService(final ThreadPoolExecutor executor, final long terminationTimeout, final TimeUnit timeUnit) {
        useDaemonThreadFactory(executor);
        final ExecutorService service = Executors.unconfigurableExecutorService(executor);
        addDelayedShutdownHook(service, terminationTimeout, timeUnit);
        return service;
    }

    /**
     * Converts the given ScheduledThreadPoolExecutor into a ScheduledExecutorService that exits
     * when the JVM exits. Uses a default termination timeout of 120 seconds.
     * 
     * <p>This method performs the following actions:</p>
     * <ul>
     *   <li>Configures the executor to use daemon threads</li>
     *   <li>Wraps it in an unconfigurable scheduled executor service</li>
     *   <li>Registers a shutdown hook to terminate the executor on JVM exit</li>
     * </ul>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ScheduledThreadPoolExecutor scheduler = new ScheduledThreadPoolExecutor(3);
     * ScheduledExecutorService exitingScheduler = 
     *     MoreExecutors.getExitingScheduledExecutorService(scheduler);
     * }</pre>
     * 
     * @param executor the scheduled executor to modify
     * @return an unconfigurable ScheduledExecutorService that will shut down on JVM exit
     * @see #getExitingScheduledExecutorService(ScheduledThreadPoolExecutor, long, TimeUnit)
     */
    public static ScheduledExecutorService getExitingScheduledExecutorService(final ScheduledThreadPoolExecutor executor) {
        return getExitingScheduledExecutorService(executor, 120, TimeUnit.SECONDS);
    }

    /**
     * Converts the given ScheduledThreadPoolExecutor into a ScheduledExecutorService that exits
     * when the JVM exits, with a custom termination timeout.
     * 
     * <p>This method performs the following actions:</p>
     * <ul>
     *   <li>Configures the executor to use daemon threads</li>
     *   <li>Wraps it in an unconfigurable scheduled executor service</li>
     *   <li>Registers a shutdown hook to terminate the executor on JVM exit</li>
     * </ul>
     * 
     * <p>The shutdown hook will call {@code shutdown()} on the executor and then wait up to
     * the specified timeout for termination. If the executor doesn't terminate within
     * the timeout, the shutdown hook will exit anyway.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ScheduledThreadPoolExecutor scheduler = new ScheduledThreadPoolExecutor(3);
     * ScheduledExecutorService exitingScheduler = 
     *     MoreExecutors.getExitingScheduledExecutorService(scheduler, 30, TimeUnit.SECONDS);
     * }</pre>
     * 
     * @param executor the scheduled executor to modify
     * @param terminationTimeout the maximum time to wait for the executor to terminate
     * @param timeUnit the time unit for the termination timeout
     * @return an unconfigurable ScheduledExecutorService that will shut down on JVM exit
     */
    public static ScheduledExecutorService getExitingScheduledExecutorService(final ScheduledThreadPoolExecutor executor, final long terminationTimeout,
            final TimeUnit timeUnit) {
        useDaemonThreadFactory(executor);
        final ScheduledExecutorService service = Executors.unconfigurableScheduledExecutorService(executor);
        addDelayedShutdownHook(service, terminationTimeout, timeUnit);
        return service;
    }

    /**
     * Adds a shutdown hook that will attempt to shut down the given ExecutorService when the JVM exits.
     * 
     * <p>The shutdown hook will:</p>
     * <ol>
     *   <li>Call {@code shutdown()} on the executor service</li>
     *   <li>Wait up to the specified timeout for the service to terminate</li>
     *   <li>Exit regardless of whether termination completed</li>
     * </ol>
     * 
     * <p>Note: Logging behavior is undefined in shutdown hooks because the logging system
     * may install its own shutdown hooks.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ExecutorService service = Executors.newFixedThreadPool(5);
     * MoreExecutors.addDelayedShutdownHook(service, 60, TimeUnit.SECONDS);
     * }</pre>
     * 
     * @param service the executor service to shut down on JVM exit
     * @param terminationTimeout the maximum time to wait for the executor to terminate
     * @param timeUnit the time unit for the termination timeout
     * @throws IllegalArgumentException if service or timeUnit is null
     */
    public static void addDelayedShutdownHook(final ExecutorService service, final long terminationTimeout, final TimeUnit timeUnit)
            throws IllegalArgumentException {
        N.checkArgNotNull(service);
        N.checkArgNotNull(timeUnit);
        addShutdownHook(MoreExecutors.newThread("DelayedShutdownHook-for-" + service, () -> {
            try {
                // We'd like to log progress and failures that may arise in the
                // following code, but unfortunately, the behavior of logging
                // is undefined in shutdown hooks.
                // This is because the logging code installs a shutdown hook of its
                // own. See Cleaner class inside {@link LogManager}.
                service.shutdown();
                //noinspection ResultOfMethodCallIgnored
                service.awaitTermination(terminationTimeout, timeUnit);
            } catch (final InterruptedException ignored) {
                // We're shutting down anyway, so ignore.
            }
        }));
    }

    /**
     * Adds a shutdown hook thread to the runtime.
     * This is a package-private utility method.
     * 
     * @param hook the thread to add as a shutdown hook
     */
    static void addShutdownHook(final Thread hook) {
        Runtime.getRuntime().addShutdownHook(hook);
    }

    /**
     * Configures the given ThreadPoolExecutor to use daemon threads.
     * This ensures that threads created by the executor won't prevent JVM shutdown.
     * 
     * <p>The method wraps the executor's existing thread factory to set the daemon flag
     * on all newly created threads.</p>
     * 
     * @param executor the executor to configure with daemon threads
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
     * Creates a new thread with the specified name and runnable.
     * This is a package-private utility method used for creating shutdown hook threads.
     * 
     * <p>The method attempts to set the thread name but silently ignores any
     * SecurityException if the operation is not permitted.</p>
     * 
     * @param name the desired name for the thread
     * @param runnable the runnable to execute in the thread
     * @return a new thread configured with the given name and runnable
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