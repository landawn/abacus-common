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
import java.util.concurrent.Executors;

import com.landawn.abacus.annotation.Internal;

@Internal
public final class AndroidUtil {

    private static final Executor SERIAL_EXECUTOR;

    private static final Executor TP_EXECUTOR;

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
            SERIAL_EXECUTOR = Executors.newSingleThreadExecutor();
            TP_EXECUTOR = Executors.newFixedThreadPool(IOUtil.CPU_CORES);
        }
    }

    private AndroidUtil() {
        // utility class;
    }

    /**
     * Gets the serial executor.
     *
     * @return
     */
    @Internal
    public static Executor getSerialExecutor() {
        return SERIAL_EXECUTOR;
    }

    /**
     * Gets the thread pool executor.
     *
     * @return
     */
    @Internal
    public static Executor getThreadPoolExecutor() {
        return TP_EXECUTOR;
    }

}
