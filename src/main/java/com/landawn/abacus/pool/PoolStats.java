/*
 * Copyright (C) 2025 HaiYang Li
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
package com.landawn.abacus.pool;

/**
 * An immutable record containing statistics about a pool's current state and historical performance.
 * This record provides a snapshot of various metrics that can be used to monitor pool health,
 * performance, and resource utilization.
 *
 * <p>The statistics include:
 * <ul>
 *   <li>Capacity and size information</li>
 *   <li>Operation counts (puts, gets)</li>
 *   <li>Cache performance metrics (hits, misses)</li>
 *   <li>Eviction statistics</li>
 *   <li>Memory usage (when applicable)</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * PoolStats stats = pool.stats();
 *
 * // Calculate hit rate
 * double hitRate = stats.getCount() > 0 ?
 *     (double) stats.hitCount() / stats.getCount() : 0.0;
 * System.out.println("Cache hit rate: " + (hitRate * 100) + "%");
 *
 * // Monitor pool utilization
 * double utilization = (double) stats.size() / stats.capacity();
 * System.out.println("Pool utilization: " + (utilization * 100) + "%");
 *
 * // Check memory usage
 * if (stats.maxMemory() > 0) {
 *     double memoryUsage = (double) stats.dataSize() / stats.maxMemory();
 *     System.out.println("Memory usage: " + (memoryUsage * 100) + "%");
 * }
 *
 * // Analyze pool efficiency
 * System.out.println("Total operations: " + (stats.putCount() + stats.getCount()));
 * System.out.println("Objects evicted: " + stats.evictionCount());
 * System.out.println("Miss rate: " +
 *     (stats.getCount() > 0 ? (double) stats.missCount() / stats.getCount() * 100 : 0) + "%");
 * }</pre>
 *
 * <p><b>Accessor naming:</b> Because {@code PoolStats} is a {@code record}, its accessors are the
 * compiler-generated bare-noun methods ({@code capacity()}, {@code size()}, {@code hitCount()}, …)
 * — record syntax forces this convention; {@code getXxx} accessors are not available. This
 * intentionally differs from {@link ActivityPrint}, a hand-written mutable class in the same package
 * that uses JavaBean {@code getXxx} accessors. The split is by design (record-forced vs. JavaBean),
 * not an oversight; the bare-noun and {@code getXxx} forms are kept as-is rather than mass-renamed.</p>
 *
 * @param capacity the maximum number of objects the pool can hold
 * @param size the current number of objects in the pool
 * @param putCount the total number of put/add operations performed
 * @param getCount the total number of get/take operations performed
 * @param hitCount the number of successful get/take operations (object was found)
 * @param missCount the number of unsuccessful get/take operations (object not found or pool empty)
 * @param evictionCount the total number of objects evicted from the pool
 * @param maxMemory the maximum memory size in bytes (-1 if no memory limit)
 * @param dataSize the current total size of data in bytes (-1 if memory tracking is disabled)
 *
 * @see Pool#stats()
 * @see ObjectPool
 * @see KeyedObjectPool
 */
public record PoolStats(int capacity, int size, long putCount, long getCount, long hitCount, long missCount, long evictionCount, long maxMemory,
        long dataSize) {

    /**
     * Returns the cache hit rate as a fraction in {@code [0.0, 1.0]}: {@code hitCount / getCount}.
     * This is a pure computed value (no state is read or modified).
     *
     * <p>Returns {@code 0.0} when {@link #getCount()} is {@code 0} (no get/take operations have
     * been performed), avoiding the division-by-zero guard callers would otherwise write.</p>
     *
     * @return the fraction of get/take operations that found an object, or {@code 0.0} if there
     *         have been no get/take operations
     */
    public double hitRate() {
        return getCount <= 0 ? 0.0 : (double) hitCount / getCount;
    }

    /**
     * Returns the cache miss rate as a fraction in {@code [0.0, 1.0]}: {@code missCount / getCount}.
     * This is a pure computed value (no state is read or modified).
     *
     * <p>Returns {@code 0.0} when {@link #getCount()} is {@code 0} (no get/take operations have
     * been performed). Note {@code hitRate() + missRate() == 1.0} whenever {@code getCount > 0}.</p>
     *
     * @return the fraction of get/take operations that did not find an object, or {@code 0.0} if
     *         there have been no get/take operations
     */
    public double missRate() {
        return getCount <= 0 ? 0.0 : (double) missCount / getCount;
    }

    /**
     * Returns the pool utilization as a fraction in {@code [0.0, 1.0]}: {@code size / capacity}.
     * This is a pure computed value (no state is read or modified).
     *
     * <p>Returns {@code 0.0} when {@link #capacity()} is {@code 0} (an unbounded/zero-capacity pool
     * configuration), avoiding the division-by-zero guard callers would otherwise write.</p>
     *
     * @return the fraction of capacity currently occupied, or {@code 0.0} if capacity is {@code 0}
     */
    public double utilization() {
        return capacity <= 0 ? 0.0 : (double) size / capacity;
    }
}
