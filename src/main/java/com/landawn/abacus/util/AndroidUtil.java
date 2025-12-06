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
import java.util.concurrent.TimeUnit;

import com.landawn.abacus.annotation.Internal;

/**
 * Utility class providing access to Android-specific executors or fallback executors
 * for non-Android environments.
 * 
 * <p>This class automatically detects whether it's running on Android and provides
 * access to Android's AsyncTask executors if available. On non-Android platforms,
 * it provides equivalent fallback executors using standard Java concurrency utilities.</p>
 * 
 * <p>The class is marked as {@code @Internal} and is intended for framework use only.</p>
 * 
 */
@Internal
public final class AndroidUtil {

    private static final Executor SERIAL_EXECUTOR;

    private static final Executor TP_EXECUTOR;

    static {
        if (IOUtil.IS_PLATFORM_ANDROID) {
            Class<?> asyncTaskClass;
            try {
                asyncTaskClass = Class.forName("android.os.AsyncTask");
                SERIAL_EXECUTOR = (Executor) asyncTaskClass.getField("SERIAL_EXECUTOR").get(null);   // android.os.AsyncTask.SERIAL_EXECUTOR;
                TP_EXECUTOR = (Executor) asyncTaskClass.getField("THREAD_POOL_EXECUTOR").get(null);   // android.os.AsyncTask.THREAD_POOL_EXECUTOR;
            } catch (ClassNotFoundException | IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        } else {
            final ExecutorService serialExecutor = Executors.newSingleThreadExecutor();
            final ExecutorService tpExecutor = Executors.newFixedThreadPool(IOUtil.CPU_CORES);

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

    private AndroidUtil() {
        // utility class;
    }

    /**
     * Returns a serial executor that executes tasks one at a time in serial order.
     * 
     * <p>On Android platforms, this returns {@code AsyncTask.SERIAL_EXECUTOR}.
     * On non-Android platforms, this returns a single-threaded executor.</p>
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
     * to the number of available CPU cores.</p>
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
