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
 * Enumeration representing the threading mode used for event delivery in the
 * {@link com.landawn.abacus.eventbus.EventBus}. The mode controls whether subscriber
 * methods are invoked on the posting thread or dispatched to a background thread pool.
 *
 * <p><b>Usage Example:</b></p>
 * <pre>{@code
 * // Deliver events on the posting thread (default)
 * @Subscribe(threadMode = ThreadMode.DEFAULT)
 * public void onEvent(MyEvent event) { ... }
 *
 * // Deliver events asynchronously on a background thread pool
 * @Subscribe(threadMode = ThreadMode.THREAD_POOL_EXECUTOR)
 * public void onEvent(MyEvent event) { ... }
 * }</pre>
 *
 * @see com.landawn.abacus.eventbus.EventBus
 * @see com.landawn.abacus.eventbus.Subscribe
 */
public enum ThreadMode {

    /**
     * Default threading mode. Events are delivered synchronously on the same thread
     * that posted the event. No additional threads are created and no executor is involved.
     *
     * <p>Use this mode when:</p>
     * <ul>
     *   <li>Event handlers are lightweight and complete quickly</li>
     *   <li>The handler does not perform blocking operations</li>
     *   <li>Preserving the posting thread's context is required</li>
     * </ul>
     */
    DEFAULT,

    /**
     * Thread-pool executor mode. Events are delivered asynchronously on a background thread
     * from the {@code EventBus}'s configured executor, decoupling the subscriber from the
     * posting thread.
     *
     * <p>Use this mode when:</p>
     * <ul>
     *   <li>Event handlers perform time-consuming or blocking operations</li>
     *   <li>Parallel execution of independent event handlers is desired</li>
     *   <li>The posting thread must not be blocked by subscriber processing</li>
     * </ul>
     */
    THREAD_POOL_EXECUTOR
}
