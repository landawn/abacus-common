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

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.landawn.abacus.annotation.MayReturnNull;

/**
 * A pool that manages objects associated with keys, similar to a Map but with pooling capabilities.
 * This interface extends Pool to provide key-based storage and retrieval of poolable objects.
 *
 * <p>Timed operations include initial lock acquisition in their waiting budget and respond to
 * interruption while acquiring that lock or waiting on a condition. Zero or negative timeouts
 * make an immediate attempt. The timeout does not bound user callbacks, processing, or the
 * lock reacquisition required when a condition wait finishes. With automatic destruction enabled,
 * a failed put also waits to check whether the value remains pooled before deciding whether to
 * destroy it; this ownership check and cleanup can finish after the timeout or interruption.</p>
 *
 * <p>KeyedObjectPool is useful when you need to pool objects by category or type, such as:
 * <ul>
 *   <li>Database connections per schema or server</li>
 *   <li>Thread pools per task type</li>
 *   <li>Cached computations by input parameters</li>
 *   <li>Resources grouped by tenant or user</li>
 * </ul>
 *
 * <p>Key features:
 * <ul>
 *   <li>Map-like interface for key-based access</li>
 *   <li>Automatic expiration of entries based on age or inactivity</li>
 *   <li>Memory-based capacity constraints</li>
 *   <li>Thread-safe operations</li>
 * </ul>
 *
 * <p><b>API Design Note:</b> KeyedObjectPool follows {@link java.util.Map} naming conventions
 * ({@code put}/{@code get}/{@code remove}/{@code peek}/{@code containsKey}) because each pooled
 * object is associated with a unique key — callers store and retrieve objects by identity. This is
 * in contrast to {@link ObjectPool}, which follows {@link java.util.concurrent.BlockingQueue} naming
 * conventions ({@code add}/{@code poll}/{@code contains}) because it models an unkeyed collection
 * of interchangeable objects.</p>
 *
 * <table>
 *   <caption>Method naming comparison between ObjectPool and KeyedObjectPool</caption>
 *   <tr><th>Operation</th><th>ObjectPool (Queue-style)</th><th>KeyedObjectPool (Map-style)</th></tr>
 *   <tr><td>Insert</td><td>{@code add(E)}</td><td>{@code put(K, E)}</td></tr>
 *   <tr><td>Retrieve-and-remove</td><td>{@code poll()}</td><td>{@code remove(K)}</td></tr>
 *   <tr><td>Retrieve-without-removing</td><td>(no analog)</td><td>{@code get(K)}</td></tr>
 *   <tr><td>Check</td><td>{@code contains(E)}</td><td>{@code containsKey(K)}</td></tr>
 * </table>
 *
 * <p>Note: {@link ObjectPool#poll()} <em>removes</em> the object from the pool, so its true keyed
 * mirror is {@link #remove(Object)} (which also removes). {@link #get(Object)} returns the value
 * <em>without</em> removing it and therefore has no unkeyed analog. One difference remains:
 * {@code poll()} destroys (with {@code EVICT}) and skips expired objects, whereas {@code remove(K)}
 * does not check expiry - an expired mapping is returned to the caller undestroyed. Likewise
 * {@link #containsKey(Object)}, {@link #keySet()}, {@link #values()} and {@link #size()} report
 * expired mappings that have not yet been evicted, while {@code get}/{@code peek} would return
 * {@code null} for them.</p>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * KeyedObjectPool<String, DBConnection> pool = PoolFactory.createKeyedObjectPool(100);
 *
 * // Store connection by database name
 * DBConnection conn = new DBConnection("server1");
 * pool.put("database1", conn);
 *
 * // Remove the connection to take exclusive ownership while using it
 * DBConnection borrowed = pool.remove("database1");
 * if (borrowed != null) {
 *     try {
 *         // use connection
 *     } finally {
 *         pool.put("database1", borrowed, true);   // return it, or destroy it if rejected
 *     }
 * }
 * }</pre>
 *
 * @param <K> the type of keys maintained by this pool
 * @param <E> the type of pooled values, must implement Poolable
 * @see Pool
 * @see ObjectPool
 * @see Poolable
 * @see PoolFactory
 */
public interface KeyedObjectPool<K, E extends Poolable> extends Pool {

    /**
     * Associates the specified poolable element with the specified key in this pool.
     * If the pool previously contained an element for the key, the old element is removed and
     * accounted (with {@link Poolable.Caller#REMOVE_REPLACE_CLEAR}) <em>before</em> the new value
     * is inserted, and it is destroyed as part of the put; its destruction callback may run after
     * the new mapping is already visible. The removal happens even when the subsequent insertion
     * of the new value fails. A {@code value} that is already expired on entry is rejected before
     * any previous mapping for {@code key} is removed; a value that expires afterwards (during
     * memory measurement or lock acquisition) is rejected in-lock after the previous mapping has
     * been detached, exactly like a capacity or memory rejection. The old element is not
     * destroyed when it is the same instance as {@code value}.
     *
     * <p>The put operation returns {@code false} (does not insert) if:</p>
     * <ul>
     *   <li>The value has already expired</li>
     *   <li>The pool is at capacity and either auto-balancing is disabled, or balancing did not free a slot</li>
     *   <li>The value would exceed memory constraints (when a memory measure is configured) and balancing did not free enough memory</li>
     *   <li>The memory measure returns a negative size or throws an exception</li>
     * </ul>
     *
     * @param key the key with which the specified value is to be associated, must not be {@code null}
     * @param value the value to be associated with the specified key, must not be {@code null}
     * @return {@code true} if the value was successfully added, {@code false} otherwise
     * @throws IllegalStateException if the pool has been closed
     * @throws IllegalArgumentException if the key or value is null.
     */
    boolean put(K key, E value) throws IllegalStateException, IllegalArgumentException;

    /**
     * Associates the specified element with the specified key in this pool,
     * with optional automatic destruction on failure.
     *
     * <p>This is a convenience method that ensures proper cleanup if the object cannot be pooled.
     *
     * <p><b>Execution Order:</b></p>
     * <ol>
     *   <li>Attempts to add the object to the pool using {@link #put(Object, Poolable)}</li>
     *   <li>If put fails and {@code autoDestroyOnFailedToPut} is {@code true}, destroys the non-null value unless still pooled by identity</li>
     *   <li>Returns the success status of the put operation</li>
     * </ol>
     *
     * <p>Cleanup is attempted in a finally block even if an exception occurs. A null value is
     * ignored, and an instance still retained under any key at the cleanup check is not destroyed.
     * Callers must coordinate concurrent ownership transfers of the same instance; the check does
     * not prevent a later admission by another thread.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Assume DBConnection extends AbstractPoolable and implements close logic in destroy()
     * DBConnection conn = new DBConnection("server1");
     * // Connection will be destroyed if it can't be added to pool
     * pool.put("db1", conn, true);
     * }</pre>
     *
     * @param key the key with which the specified value is to be associated, must not be {@code null}
     * @param value the value to be associated with the specified key, must not be {@code null}
     * @param autoDestroyOnFailedToPut if {@code true}, destroys a rejected non-null value unless that
     *        same instance remains pooled at the cleanup check
     * @return {@code true} if the value was successfully added, {@code false} otherwise
     * @throws IllegalStateException if the pool has been closed
     * @throws IllegalArgumentException if the key or value is null.
     */
    boolean put(K key, E value, boolean autoDestroyOnFailedToPut) throws IllegalStateException, IllegalArgumentException;

    /**
     * Attempts to associate the specified element with the specified key, waiting if necessary for
     * a capacity slot to become available. This is the keyed mirror of
     * {@link ObjectPool#add(Poolable, long, TimeUnit)}: when the pool is at capacity and
     * auto-balancing is disabled, this method blocks until a slot frees up (because another thread
     * removes/evicts an entry), the timeout expires, or the thread is interrupted.
     * Rejection of one waiting candidate allows other waiting candidates to proceed when space is available.
     *
     * <p>When auto-balancing is enabled (the {@link PoolFactory} default), a full pool is first
     * balanced under the lock - a balance-factor share of the existing mappings is detached and
     * destroyed with {@link Poolable.Caller#VACATE} - and the value is inserted without waiting.
     * Waiting for a slot occurs only when auto-balancing is disabled (or the capacity is {@code 0}).</p>
     *
     * <p>All other {@code put} semantics of {@link #put(Object, Poolable)} still apply: the
     * previous mapping for {@code key} (if any, and a different instance) is removed and destroyed
     * before insertion, an already-expired value is rejected ({@code false}), and a configured
     * memory measure is consulted. The method returns {@code false} if the timeout elapses before a
     * slot becomes available.</p>
     *
     * <p>If another thread inserts this key while the call waits, that mapping can be replaced
     * even when the pool is full. The same replacement, memory-accounting and destruction rules
     * apply to mappings discovered after a wait.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DBConnection conn = new DBConnection("server1");
     * if (pool.put("database1", conn, 5, TimeUnit.SECONDS)) {
     *     // Successfully pooled
     * } else {
     *     // Timed out waiting for a slot - handle the connection
     *     conn.destroy(Poolable.Caller.PUT_ADD_FAILURE);
     * }
     * }</pre>
     *
     * @param key the key with which the specified value is to be associated, must not be {@code null}
     * @param value the value to be associated with the specified key, must not be {@code null}
     * @param timeout the maximum time to wait for a capacity slot to become available
     * @param unit the time unit of the timeout argument, must not be {@code null}
     * @return {@code true} if the value was successfully added, {@code false} if the timeout elapsed
     *         (or the value was already/became expired, or memory constraints rejected it)
     * @throws IllegalStateException if the pool has been closed
     * @throws IllegalArgumentException if the key, value, or unit is null.
     * @throws InterruptedException if interrupted while waiting
     */
    boolean put(K key, E value, long timeout, TimeUnit unit) throws IllegalStateException, IllegalArgumentException, InterruptedException;

    /**
     * Attempts to associate the specified element with the specified key within the given timeout,
     * with optional automatic destruction on failure. This combines the timed-wait behavior of
     * {@link #put(Object, Poolable, long, TimeUnit)} with the cleanup convenience of
     * {@link #put(Object, Poolable, boolean)}.
     *
     * <p>If the put fails (timeout elapsed, capacity could not be freed, the value expired, or
     * memory constraints rejected it) and {@code autoDestroyOnFailedToPut} is {@code true}, the
     * pool calls {@code value.destroy(PUT_ADD_FAILURE)} in a finally block even if an exception is
     * thrown, unless the value is null or the same instance remains pooled at the cleanup check.</p>
     *
     * @param key the key with which the specified value is to be associated, must not be {@code null}
     * @param value the value to be associated with the specified key, must not be {@code null}
     * @param timeout the maximum time to wait for a capacity slot to become available
     * @param unit the time unit of the timeout argument, must not be {@code null}
     * @param autoDestroyOnFailedToPut if {@code true}, destroys a rejected non-null value unless that
     *        same instance remains pooled at the cleanup check
     * @return {@code true} if the value was successfully added, {@code false} if the timeout elapsed or put failed
     * @throws IllegalStateException if the pool has been closed
     * @throws IllegalArgumentException if the key, value, or unit is null.
     * @throws InterruptedException if interrupted while waiting
     */
    boolean put(K key, E value, long timeout, TimeUnit unit, boolean autoDestroyOnFailedToPut)
            throws IllegalStateException, IllegalArgumentException, InterruptedException;

    /**
     * Returns the element associated with the specified key, or {@code null} if no mapping exists.
     * The element's activity print is updated (last access time and access count) to reflect this access.
     *
     * <p>If the retrieved element has expired, it is removed from the pool, destroyed, and {@code null} is returned.
     *
     * <p><b>Contrast with {@link ObjectPool}:</b> {@code get(K)} <em>retrieves without removing</em> the
     * element (cache semantics), unlike {@link ObjectPool#poll()} which retrieves <em>and removes</em>.
     * The removing counterpart here is {@link #remove(Object)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * E cached = pool.get("myKey");
     * if (cached != null) {
     *     // Use cached object
     * } else {
     *     // Create new object and add to pool
     *     E newObj = createObject();
     *     pool.put("myKey", newObj);
     * }
     * }</pre>
     *
     * @param key the key whose associated element is to be returned; a {@code null} key matches no
     *        mapping ({@code null} is returned and a miss is recorded)
     * @return the element associated with the key, or {@code null} if no mapping exists or the element has expired
     * @throws IllegalStateException if the pool has been closed
     */
    @MayReturnNull
    E get(K key) throws IllegalStateException;

    /**
     * Returns the element associated with the specified key, waiting if necessary until a non-expired
     * mapping for the key becomes available, the timeout expires, or the thread is interrupted. This
     * is the keyed mirror of {@link ObjectPool#poll(long, TimeUnit)}: a keyed pool retrieval is keyed
     * on a specific {@code key}, so this method blocks until <em>that</em> key is populated (typically
     * by another thread's {@code put}) rather than for any arbitrary object.
     *
     * <p>Like {@link #get(Object)}, on success the element's activity print is updated (last access
     * time and access count), the element is <em>not</em> removed (it stays in the pool), and an
     * expired mapping encountered for the key is removed and destroyed (the method then keeps waiting
     * for a fresh mapping until the timeout). Returns {@code null} if the timeout elapses before a
     * valid mapping for the key is available.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DBConnection conn = pool.get("database1", 10, TimeUnit.SECONDS);
     * if (conn != null) {
     *     // use connection (still pooled under "database1")
     * } else {
     *     // timed out - no connection became available for that key
     * }
     * }</pre>
     *
     * @param key the key whose associated element is to be returned; a {@code null} key matches no
     *        mapping (the call waits out the timeout, returns {@code null} and records a miss)
     * @param timeout the maximum time to wait for a valid mapping for the key to become available
     * @param unit the time unit of the timeout argument, must not be {@code null}
     * @return the element associated with the key, or {@code null} if the timeout elapsed before a
     *         valid mapping was available
     * @throws IllegalStateException if the pool has been closed
     * @throws IllegalArgumentException if the unit is null.
     * @throws InterruptedException if interrupted while waiting
     */
    @MayReturnNull
    E get(K key, long timeout, TimeUnit unit) throws IllegalStateException, IllegalArgumentException, InterruptedException;

    /**
     * Removes and returns the element associated with the specified key.
     * The element's activity print is updated (last access time and access count)
     * to reflect this access.
     *
     * <p>Unlike {@link #get(Object)}, this method removes the element from the pool,
     * so it will not be available for future requests unless re-added.
     *
     * <p>The pool does <em>not</em> invoke {@link Poolable#destroy(Poolable.Caller)} on the
     * returned element — the caller takes ownership and is responsible for either returning
     * the element to a pool or destroying it when no longer needed.
     *
     * <p>Unlike {@link #get(Object)}, {@link #peek(Object)} and {@link ObjectPool#poll()}, the
     * expiry of the mapping is <em>not</em> checked: an expired element that has not yet been
     * evicted is returned (ownership transferred) and is not destroyed by the pool. Borrowers that
     * care should test {@code element.activityPrint().isExpired()} before using it.
     *
     * @param key the key whose mapping is to be removed from the pool; a {@code null} key matches no
     *        mapping ({@code null} is returned)
     * @return the element previously associated with the key, or {@code null} if no mapping exists
     * @throws IllegalStateException if the pool has been closed
     */
    @MayReturnNull
    E remove(K key) throws IllegalStateException;

    /**
     * Returns the element associated with the specified key without updating access statistics.
     * This method is useful for monitoring or administrative purposes where you want to check
     * the pool's contents without affecting eviction behavior.
     *
     * <p><b>Important Side Effects:</b></p>
     * <ul>
     *   <li><b>Does NOT update</b> last access time - element's access time remains unchanged</li>
     *   <li><b>Does NOT update</b> access count - element's access counter remains unchanged</li>
     *   <li><b>DOES remove and destroy</b> expired elements - if the element has expired, it will be
     *       destroyed and removed from the pool, and {@code null} will be returned</li>
     *   <li><b>Does NOT remove</b> the element from the pool (if valid) - the element remains available
     *       for future requests</li>
     * </ul>
     *
     * <p>Use this method when you need to inspect pool contents for monitoring, debugging, or
     * administrative purposes without affecting the element's eviction priority. If you need to
     * use the element for regular operations, use {@link #get(Object)} instead.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Check if an element exists without affecting its access statistics
     * DBConnection conn = pool.peek("database1");
     * if (conn != null) {
     *     System.out.println("Connection available: " + conn.isActive());
     *     // Connection remains in pool with unchanged access statistics
     * }
     * }</pre>
     *
     * @param key the key whose associated element is to be returned; a {@code null} key matches no
     *        mapping ({@code null} is returned)
     * @return the element associated with the key, or {@code null} if no mapping exists or element expired
     * @throws IllegalStateException if the pool has been closed
     */
    @MayReturnNull
    E peek(K key) throws IllegalStateException;

    /**
     * Returns a Set containing a snapshot of the keys contained in this pool.
     * The returned set will not reflect subsequent changes to the pool. Keys of expired mappings
     * that have not yet been evicted are included.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Set<String> keys = pool.keySet();
     * for (String key : keys) {
     *     System.out.println("Pool contains key: " + key);
     * }
     * }</pre>
     *
     * @return a set containing all keys currently in the pool
     * @throws IllegalStateException if the pool has been closed
     */
    Set<K> keySet() throws IllegalStateException;

    /**
     * Returns a Collection containing a snapshot of the values contained in this pool.
     * The returned collection will not reflect subsequent changes to the pool. Values of expired
     * mappings that have not yet been evicted are included.
     *
     * @return a collection containing all values currently in the pool
     * @throws IllegalStateException if the pool has been closed
     */
    Collection<E> values() throws IllegalStateException;

    /**
     * Checks if this pool contains a mapping for the specified key.
     *
     * <p>An expired mapping counts as present until it is evicted (by the scheduled sweep,
     * {@link #evict()}, or a {@code get}/{@code peek} that detects the expiry), so
     * {@code containsKey(key)} may return {@code true} while a following {@code get(key)} returns
     * {@code null}.
     *
     * @param key the key whose presence in this pool is to be tested; a {@code null} key matches no
     *        mapping ({@code false} is returned)
     * @return {@code true} if this pool contains a mapping for the specified key, {@code false} otherwise
     * @throws IllegalStateException if the pool has been closed
     */
    boolean containsKey(K key) throws IllegalStateException;

    /**
     * Interface for measuring the memory size of key-value pairs in the pool.
     * This allows the pool to enforce memory-based capacity limits.
     *
     * <p><b>Serialization:</b> the measure is stored in a non-transient field of the pool, and every
     * {@link Pool} is {@link java.io.Serializable}. A pool is therefore serializable only if every
     * pooled key and value and the configured measure are {@code Serializable}; a lambda measure must
     * be declared with an intersection cast such as
     * {@code (KeyedObjectPool.MemoryMeasure<K, E> & Serializable) (k, v) -> v.size()}, otherwise
     * serializing the pool fails with {@link java.io.NotSerializableException}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * KeyedObjectPool.MemoryMeasure<String, CachedData> measure = (key, data) ->
     *     key.length() * 2 + data.getDataSize();   // uses 2 bytes per char for UTF-16 encoding
     *
     * KeyedObjectPool<String, CachedData> pool = PoolFactory.createKeyedObjectPool(
     *     1000, 3000, EvictionPolicy.LAST_ACCESS_TIME,
     *     1024 * 1024 * 500, // 500MB max
     *     measure
     * );
     * }</pre>
     *
     * @param <K> the type of keys
     * @param <E> the type of values being measured
     */
    interface MemoryMeasure<K, E> {

        /**
         * Calculates the memory size of the given key-value pair in bytes.
         * The returned value is used to track total memory usage and enforce memory limits.
         * The pool retains the charge for each successful admission and subtracts that charge
         * on removal without measuring again. Mutating a pooled value does not change its charge;
         * putting it again measures it again. Totals must remain representable as a {@code long},
         * even when no memory limit is configured.
         *
         * <p>Each eligible put attempt measures once before it acquires the pool lock or detaches
         * an existing mapping, including attempts later rejected for capacity or timeout. Timed
         * puts retain this sample while waiting. Measures must support concurrent calls; this
         * admission does not hold the pool lock while measuring (unless its caller already holds it).</p>
         *
         * @param key the key part of the pair, never {@code null} when called by the pool
         * @param value the value part of the pair, never {@code null} when called by the pool
         * @return the combined size of the key-value pair in bytes, should be non-negative
         */
        long sizeOf(K key, E value);
    }
}
