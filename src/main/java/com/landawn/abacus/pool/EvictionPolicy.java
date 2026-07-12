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

package com.landawn.abacus.pool;

/**
 * Enumeration of eviction policies that determine which objects are selected for removal
 * during balancing/vacate operations when a pool reaches capacity.
 *
 * <p>The eviction policy controls only the <em>order</em> in which objects are selected for
 * removal during balancing/vacate operations when the pool needs to free capacity. Expired
 * objects are always removed by the periodic eviction task regardless of the configured
 * policy — the policy does not influence whether or how soon an expired object is reclaimed.
 *
 * <p>Each policy uses different criteria from the object's {@link ActivityPrint} to determine
 * eviction priority. Objects with lower values according to the policy's criteria are
 * evicted first. The sole exception is {@link #FIFO}, which ignores the {@code ActivityPrint}
 * entirely and selects victims purely by the order in which entries were added to the pool.
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // LRU-style pool - evicts least recently used objects
 * ObjectPool<Resource> lruPool = PoolFactory.createObjectPool(
 *     100, 60000, EvictionPolicy.LAST_ACCESS_TIME
 * );
 *
 * // LFU-style pool - evicts least frequently used objects
 * ObjectPool<Resource> lfuPool = PoolFactory.createObjectPool(
 *     100, 60000, EvictionPolicy.ACCESS_COUNT
 * );
 *
 * // Time-based pool - evicts objects closest to expiration
 * ObjectPool<Resource> timePool = PoolFactory.createObjectPool(
 *     100, 60000, EvictionPolicy.EXPIRATION_TIME
 * );
 * }</pre>
 *
 * @see AbstractPool
 * @see ActivityPrint
 * @see PoolFactory
 */
public enum EvictionPolicy {
    /**
     * Least Recently Used (LRU) eviction policy.
     * Objects are evicted based on their last access time, with the least recently
     * accessed objects being evicted first.
     *
     * <p>This policy is ideal for:
     * <ul>
     *   <li>General-purpose caching where recent access indicates future use</li>
     *   <li>Scenarios where temporal locality is important</li>
     *   <li>Default choice when unsure which policy to use</li>
     * </ul>
     *
     * <p>Objects with the oldest {@link ActivityPrint#getLastAccessTime()} are evicted first.
     */
    LAST_ACCESS_TIME,

    /**
     * Time-based eviction policy.
     * Objects are evicted based on their expiration time, with objects closest to
     * expiration being evicted first.
     *
     * <p>This policy is ideal for:
     * <ul>
     *   <li>Pools where objects have varying lifetimes</li>
     *   <li>Scenarios where you want to maximize object lifetime utilization</li>
     *   <li>Cases where objects become less valuable as they age</li>
     * </ul>
     *
     * <p>Objects with the earliest {@link ActivityPrint#getExpirationTime()} are evicted first.
     * This ensures objects are used for as much of their lifetime as possible.
     */
    EXPIRATION_TIME,

    /**
     * Least Frequently Used (LFU) eviction policy.
     * Objects are evicted based on their access count, with the least frequently
     * accessed objects being evicted first.
     *
     * <p>This policy is ideal for:
     * <ul>
     *   <li>Scenarios where popular objects should remain cached</li>
     *   <li>Pools where access frequency is a good predictor of future use</li>
     *   <li>Long-running applications with stable access patterns</li>
     * </ul>
     *
     * <p>Objects with the lowest {@link ActivityPrint#getAccessCount()} are evicted first.
     * Note that this policy may keep old but frequently accessed objects indefinitely.
     */
    ACCESS_COUNT,

    /**
     * Creation-time eviction policy.
     * Objects are evicted based on their creation time, with the oldest-created objects
     * being evicted first (regardless of how recently or how often they were accessed).
     *
     * <p>This policy is ideal for:
     * <ul>
     *   <li>Pools where freshness matters and long-lived objects should be recycled</li>
     *   <li>Scenarios where you want to bound how long any object can stay pooled</li>
     *   <li>Mitigating slow resource drift/leaks by periodically rotating old objects out</li>
     * </ul>
     *
     * <p>Objects with the earliest {@link ActivityPrint#getCreatedTime()} are evicted first.
     */
    CREATED_TIME,

    /**
     * First-In-First-Out (FIFO) eviction policy.
     * Objects are evicted in the order they entered the pool — the earliest-added object is
     * evicted first, independent of access recency or frequency.
     *
     * <p>This policy is ideal for:
     * <ul>
     *   <li>Fair, predictable round-robin recycling of pooled objects</li>
     *   <li>Workloads with no useful temporal/frequency locality</li>
     *   <li>Cases where every object should get a roughly equal lifetime in the pool</li>
     * </ul>
     *
     * <p>This policy is based on the order entries are added to the pool, not on the creation time
     * of the pooled object or its {@link ActivityPrint}.
     */
    FIFO
}
