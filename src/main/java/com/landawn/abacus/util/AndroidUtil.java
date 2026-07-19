/*
 * Copyright (C) 2019 HaiYang Li
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

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import com.landawn.abacus.annotation.Internal;

/**
 * Utility class providing access to Android-specific executors or fallback executors
 * for non-Android environments.
 *
 * <p>This class automatically detects whether it is running on Android and provides
 * access to Android's {@code AsyncTask} executors if available. On non-Android platforms,
 * it provides equivalent fallback executors using standard Java concurrency utilities:
 * a single-threaded executor for serial execution and a fixed-size thread pool
 * (sized to {@link IOUtil#CPU_CORES CPU_CORES}) for parallel execution. The fallback
 * executors use daemon worker threads so that idle framework executors do not keep an
 * otherwise-finished JVM alive.</p>
 *
 * <p>On non-Android platforms a JVM shutdown hook is registered to gracefully
 * terminate the created executors on JVM exit.</p>
 *
 * <p>This class is marked {@link Internal} and is intended for framework use only.
 * It cannot be instantiated.</p>
 */
@Internal
public final class AndroidUtil {

    private static final Executor SERIAL_EXECUTOR;

    private static final Executor TP_EXECUTOR;

    private static final ThreadFactory DAEMON_THREAD_FACTORY = runnable -> {
        final Thread thread = Executors.defaultThreadFactory().newThread(runnable);
        thread.setDaemon(true);
        return thread;
    };

    static {
        if (IOUtil.IS_PLATFORM_ANDROID) {
            Class<?> asyncTaskClass;
            try {
                asyncTaskClass = Class.forName("android.os.AsyncTask");
                SERIAL_EXECUTOR = (Executor) asyncTaskClass.getField("SERIAL_EXECUTOR").get(null); // android.os.AsyncTask.SERIAL_EXECUTOR;
                TP_EXECUTOR = (Executor) asyncTaskClass.getField("THREAD_POOL_EXECUTOR").get(null); // android.os.AsyncTask.THREAD_POOL_EXECUTOR;
            } catch (ClassNotFoundException | IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        } else {
            final ExecutorService serialExecutor = Executors.newSingleThreadExecutor(DAEMON_THREAD_FACTORY);
            final ExecutorService tpExecutor = Executors.newFixedThreadPool(IOUtil.CPU_CORES, DAEMON_THREAD_FACTORY);

            SERIAL_EXECUTOR = serialExecutor;
            TP_EXECUTOR = tpExecutor;

            // Register shutdown hook to properly cleanup executors
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                if (serialExecutor != null) {
                    serialExecutor.shutdown();
                    try {
                        if (!serialExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                            serialExecutor.shutdownNow();
                        }
                    } catch (final InterruptedException e) {
                        Thread.currentThread().interrupt();
                        serialExecutor.shutdownNow();
                    }
                }

                if (tpExecutor != null) {
                    tpExecutor.shutdown();
                    try {
                        if (!tpExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                            tpExecutor.shutdownNow();
                        }
                    } catch (final InterruptedException e) {
                        Thread.currentThread().interrupt();
                        tpExecutor.shutdownNow();
                    }
                }
            }));
        }
    }

    /**
     * Private constructor. This class is not instantiable.
     */
    private AndroidUtil() {
        // utility class;
    }

    /**
     * Returns a serial executor that executes tasks one at a time in serial order.
     *
     * <p>On Android platforms, this returns {@code AsyncTask.SERIAL_EXECUTOR}.
     * On non-Android platforms, this returns a single-threaded executor whose worker is a daemon thread.</p>
     *
     * <p>This executor is useful when tasks must be executed sequentially and
     * thread safety is a concern.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Executor serialExecutor = AndroidUtil.getSerialExecutor();
     * serialExecutor.execute(() -> {
     *     // Task executed serially
     * });
     * }</pre>
     *
     * @return a serial executor instance
     */
    @Internal
    public static Executor getSerialExecutor() {
        return SERIAL_EXECUTOR;
    }

    /**
     * Returns a thread pool executor suitable for parallel task execution.
     *
     * <p>On Android platforms, this returns {@code AsyncTask.THREAD_POOL_EXECUTOR}.
     * On non-Android platforms, this returns a fixed thread pool with size equal
     * to the number of available CPU cores and whose workers are daemon threads.</p>
     *
     * <p>This executor is suitable for CPU-bound parallel tasks that can benefit
     * from multi-core execution.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Executor threadPool = AndroidUtil.getThreadPoolExecutor();
     * threadPool.execute(() -> {
     *     // Task executed in parallel thread pool
     * });
     * }</pre>
     *
     * @return a thread pool executor instance
     */
    @Internal
    public static Executor getThreadPoolExecutor() {
        return TP_EXECUTOR;
    }

}
