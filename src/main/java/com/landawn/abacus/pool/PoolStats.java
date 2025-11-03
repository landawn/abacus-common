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
 * @param capacity the maximum number of objects the pool can hold
 * @param size the current number of objects in the pool
 * @param putCount the total number of put/add operations performed
 * @param getCount the total number of get/take operations performed  
 * @param hitCount the number of successful get operations (object was found)
 * @param missCount the number of unsuccessful get operations (object not found or pool empty)
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

}
