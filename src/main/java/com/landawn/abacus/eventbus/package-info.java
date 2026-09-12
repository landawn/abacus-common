/*
 * Copyright (C) 2015 HaiYang Li
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

/**
 * A lightweight, thread-safe publish-subscribe event system for loosely coupled components.
 *
 * <p>{@link EventBus} delivers posted events to registered {@link Subscriber} instances and to
 * public instance methods marked with {@link Subscribe}. Obtain the process-wide bus with
 * {@link EventBus#getDefault()} or construct a dedicated instance.</p>
 *
 * <p>Supported features:</p>
 * <ul>
 *   <li>Delivery by event type, including inherited types. Parameterized event types match on their raw class.</li>
 *   <li>Optional event identifiers so a post targets a subset of subscribers.</li>
 *   <li>Sticky events that are retained and replayed to newly registered subscribers.</li>
 *   <li>{@link com.landawn.abacus.util.ThreadMode} control for synchronous or executor-based delivery.</li>
 *   <li>Interval throttling, consecutive-event deduplication, sticky delivery, and strict event-type
 *       matching &mdash; configured only on {@link Subscribe}, not on lambda {@link Subscriber} registration.</li>
 * </ul>
 *
 * <p>Accepted callbacks may run concurrently when events are posted from multiple threads. Exceptions
 * thrown by a subscriber are caught and logged; they do not abort delivery to other subscribers.</p>
 *
 * @see EventBus
 * @see Subscriber
 * @see Subscribe
 */
package com.landawn.abacus.eventbus;
