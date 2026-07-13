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
 * A lightweight, thread-safe object-pooling framework for caching and reusing expensive-to-create
 * objects (database connections, threads, large buffers, pre-initialized instances, and the like).
 *
 * <h2>Choosing a pool type</h2>
 * <ul>
 *   <li>{@link com.landawn.abacus.pool.ObjectPool} &mdash; an <em>unkeyed</em> collection of
 *       interchangeable objects. Follows {@link java.util.concurrent.BlockingQueue} naming
 *       ({@code add} / {@code poll} / {@code contains}); {@code poll()} removes the returned object.</li>
 *   <li>{@link com.landawn.abacus.pool.KeyedObjectPool} &mdash; objects associated with a unique
 *       key (e.g. connections per schema, resources per tenant). Follows {@link java.util.Map}
 *       naming ({@code put} / {@code get} / {@code remove} / {@code peek} / {@code containsKey});
 *       {@code get(key)} returns the value <em>without</em> removing it, whereas {@code remove(key)}
 *       hands ownership to the caller.</li>
 * </ul>
 *
 * <p>Instances are obtained from {@link com.landawn.abacus.pool.PoolFactory}, whose overloads layer
 * on eviction delay, {@linkplain com.landawn.abacus.pool.EvictionPolicy eviction policy},
 * auto-balancing, and optional memory-based capacity limits. The concrete implementations are
 * {@link com.landawn.abacus.pool.GenericObjectPool} (LIFO storage) and
 * {@link com.landawn.abacus.pool.GenericKeyedObjectPool} (insertion-ordered storage).</p>
 *
 * <h2>Poolable objects</h2>
 * <p>Every pooled value implements {@link com.landawn.abacus.pool.Poolable}, exposing an
 * {@link com.landawn.abacus.pool.ActivityPrint} (creation time, live time, max idle time, last
 * access time, access count) and a {@link com.landawn.abacus.pool.Poolable#destroy destroy} callback
 * that receives a {@link com.landawn.abacus.pool.Poolable.Caller} explaining why the object is being
 * released. {@link com.landawn.abacus.pool.AbstractPoolable} is a convenient base class, and
 * {@link com.landawn.abacus.pool.PoolableAdapter} wraps objects that do not implement
 * {@code Poolable} directly (or use {@link com.landawn.abacus.pool.Poolable#wrap Poolable.wrap}).</p>
 *
 * <h2>Expiration vs. eviction</h2>
 * <p>These are two distinct mechanisms:</p>
 * <ul>
 *   <li><b>Expiration</b> is time-based: an object expires once it exceeds its live time or max idle
 *       time. Expired objects are reclaimed by a periodic background task (configured via the evict
 *       delay) and are also skipped and destroyed lazily when encountered during {@code poll}/{@code get}.
 *       The eviction policy has no bearing on expiration.</li>
 *   <li><b>Eviction / balancing</b> is capacity-based: when a full pool must make room, the configured
 *       {@link com.landawn.abacus.pool.EvictionPolicy} decides the <em>order</em> in which live
 *       objects are shed (LRU, LFU, closest-to-expiration, oldest-created, or FIFO). This is driven
 *       by {@code autoBalance} plus a balance factor, or invoked explicitly via
 *       {@link com.landawn.abacus.pool.Pool#evict()}.</li>
 * </ul>
 *
 * <h2>Lifecycle and thread-safety</h2>
 * <p>All pools are thread-safe and support concurrent access. A pool progresses through creation,
 * use, and closure; once {@linkplain com.landawn.abacus.pool.Pool#close() closed} it cannot be
 * reopened, and every operation except {@code capacity()}, {@code isClosed()}, and {@code close()}
 * throws {@link java.lang.IllegalStateException}. Pools are {@link java.lang.AutoCloseable} and also
 * register a JVM shutdown hook to destroy their contents on exit. Runtime metrics are available as an
 * immutable {@link com.landawn.abacus.pool.PoolStats} snapshot from
 * {@link com.landawn.abacus.pool.Pool#stats()}.</p>
 *
 * <h2>Usage example</h2>
 * <pre>{@code
 * // A pool of up to 100 reusable resources, background eviction every 60s, LRU balancing.
 * try (ObjectPool<MyResource> pool = PoolFactory.createObjectPool(
 *         100, 60_000, EvictionPolicy.LAST_ACCESS_TIME)) {
 *
 *     MyResource resource = pool.poll();      // borrow (removes from pool), or null if empty
 *     if (resource == null) {
 *         resource = new MyResource();        // MyResource extends AbstractPoolable
 *     }
 *     try {
 *         // ... use the resource ...
 *     } finally {
 *         pool.add(resource);                 // return it for reuse
 *     }
 * }   // close() destroys everything still pooled
 * }</pre>
 *
 * @see com.landawn.abacus.pool.Pool
 * @see com.landawn.abacus.pool.PoolFactory
 * @see com.landawn.abacus.pool.Poolable
 */
package com.landawn.abacus.pool;
