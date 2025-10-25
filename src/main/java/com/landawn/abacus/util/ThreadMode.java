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

/**
 * Enumeration representing different threading modes for concurrent operations.
 * This enum is used to specify how operations should be executed in a multi-threaded context.
 * 
 * <p>The threading mode determines whether operations run in the default thread context
 * or are submitted to a thread pool for concurrent execution.
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Execute operation in default thread mode
 * processor.execute(task, ThreadMode.DEFAULT);
 * 
 * // Execute operation using thread pool
 * processor.execute(task, ThreadMode.THREAD_POOL_EXECUTOR);
 * }</pre>
 * 
 * @since 1.0
 */
public enum ThreadMode {

    /**
     * Default threading mode where operations are executed in the current thread context.
     * This mode does not create additional threads and executes operations synchronously
     * in the calling thread.
     * 
     * <p>Use this mode when:
     * <ul>
     *   <li>Thread safety is not a concern</li>
     *   <li>Operations are lightweight and don't require parallelization</li>
     *   <li>You want to maintain the current thread context</li>
     * </ul>
     */
    DEFAULT,

    /**
     * Thread pool executor mode where operations are submitted to a thread pool for execution.
     * This mode enables concurrent execution of operations across multiple threads,
     * potentially improving performance for CPU-intensive or I/O-bound tasks.
     * 
     * <p>Use this mode when:
     * <ul>
     *   <li>Operations can benefit from parallel execution</li>
     *   <li>Tasks are independent and can run concurrently</li>
     *   <li>You want to leverage multi-core processors</li>
     *   <li>Operations involve blocking I/O that would benefit from async execution</li>
     * </ul>
     */
    THREAD_POOL_EXECUTOR
}