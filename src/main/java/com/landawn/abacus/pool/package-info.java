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
 *   <li>{@link ObjectPool} &mdash; an <em>unkeyed</em> collection of interchangeable objects.
 *       Follows {@link java.util.concurrent.BlockingQueue} naming
 *       ({@code add} / {@code poll} / {@code contains}); {@code poll()} removes the returned object.</li>
 *   <li>{@link KeyedObjectPool} &mdash; objects associated with a unique key (e.g. connections per
 *       schema, resources per tenant). Follows {@link java.util.Map} naming
 *       ({@code put} / {@code get} / {@code remove} / {@code peek} / {@code containsKey});
 *       {@code get(key)} returns the value <em>without</em> removing it, whereas {@code remove(key)}
 *       hands ownership to the caller.</li>
 * </ul>
 *
 * <p>Instances are obtained from {@link PoolFactory}, whose overloads layer on eviction delay,
 * {@linkplain EvictionPolicy eviction policy}, auto-balancing, and optional memory-based capacity
 * limits. The concrete implementations are {@link GenericObjectPool} (LIFO storage) and
 * {@link GenericKeyedObjectPool} (insertion-ordered storage).</p>
 *
 * <h2>Poolable objects</h2>
 * <p>Every pooled value implements {@link Poolable}, exposing an {@link ActivityPrint} (creation
 * time, live time, max idle time, last access time, access count) and a
 * {@link Poolable#destroy destroy} callback that receives a {@code Poolable.Caller} explaining why
 * the object is being released. {@link AbstractPoolable} is a convenient base class.
 * {@link PoolableAdapter} wraps objects that do not implement {@code Poolable} directly
 * (or use {@link Poolable#wrap}).</p>
 *
 * <h2>Expiration vs. eviction</h2>
 * <p>These are two distinct mechanisms:</p>
 * <ul>
 *   <li><b>Expiration</b> is time-based: an object expires once it exceeds its live time or max idle
 *       time. Expired objects are reclaimed by a periodic background task (configured via the evict
 *       delay) and are also skipped and destroyed lazily when encountered during {@code poll}/{@code get}.
 *       The eviction policy has no bearing on expiration.</li>
 *   <li><b>Eviction / balancing</b> is capacity-based: when a full pool must make room, the configured
 *       {@link EvictionPolicy} decides the <em>order</em> in which live objects are shed (LRU, LFU,
 *       closest-to-expiration, oldest-created, or FIFO). This is driven by {@code autoBalance} plus a
 *       balance factor, or invoked explicitly via {@link Pool#evict()}.</li>
 * </ul>
 *
 * <h2>Lifecycle, thread-safety, and serialization</h2>
 * <p>All pools are thread-safe and support concurrent access. A pool progresses through creation,
 * use, and closure; once {@linkplain Pool#close() closed} it cannot be reopened. Operations
 * requiring an open pool throw {@link java.lang.IllegalStateException}; {@code capacity()},
 * {@code isClosed()}, and {@code close()} remain available, as do the generic implementations'
 * {@code equals}, {@code hashCode}, and {@code toString} methods. Pools are
 * {@link java.lang.AutoCloseable} and also register a JVM shutdown hook to destroy their contents
 * on exit. Runtime metrics are available as an immutable {@link PoolStats} snapshot from
 * {@link Pool#stats()}.</p>
 *
 * <p>Every {@link Pool} is {@link java.io.Serializable}. {@link AbstractPoolable} is not: it
 * represents a live pooled resource. Serializing a pool that still contains {@code AbstractPoolable}
 * or {@link PoolableAdapter} values fails with {@link java.io.NotSerializableException}. An empty
 * pool still serializes. If persistence is wanted, snapshot configuration or statistics and
 * reconstruct live resources from that snapshot.</p>
 *
 * <h2>Usage example</h2>
 * <pre>{@code
 * class MyResource extends AbstractPoolable {
 *     MyResource() {
 *         super(3_600_000, 600_000);
 *     }
 *
 *     void use() {
 *         System.out.println("resource in use");
 *     }
 *
 *     @Override
 *     public void destroy(Poolable.Caller caller) {
 *         System.out.println("destroyed by " + caller);
 *     }
 * }
 *
 * // A pool of up to 100 reusable resources, background eviction every 60s, LRU balancing.
 * try (ObjectPool<MyResource> pool = PoolFactory.createObjectPool(
 *         100, 60_000, EvictionPolicy.LAST_ACCESS_TIME)) {
 *
 *     MyResource resource = pool.poll();      // borrow (removes from pool), or null if empty
 *     if (resource == null) {
 *         resource = new MyResource();        // MyResource extends AbstractPoolable
 *     }
 *     try {
 *         resource.use();
 *     } finally {
 *         pool.add(resource, true);   // return it, or destroy it if rejected
 *     }
 * }   // close() destroys everything still pooled
 * }</pre>
 *
 * @see Pool
 * @see PoolFactory
 * @see Poolable
 */
package com.landawn.abacus.pool;
