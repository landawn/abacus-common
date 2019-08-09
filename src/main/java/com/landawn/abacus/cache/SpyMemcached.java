/*
 * Copyright (C) 2015 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.landawn.abacus.cache;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.logging.Logger;
import com.landawn.abacus.logging.LoggerFactory;
import com.landawn.abacus.util.AddrUtil;
import com.landawn.abacus.util.N;

import net.spy.memcached.ConnectionFactory;
import net.spy.memcached.DefaultConnectionFactory;
import net.spy.memcached.MemcachedClient;
import net.spy.memcached.transcoders.Transcoder;

// TODO: Auto-generated Javadoc
/**
 * The Class SpyMemcached.
 *
 * @author Haiyang Li
 * @param <T> the generic type
 * @since 0.8
 */
public class SpyMemcached<T> extends AbstractDistributedCacheClient<T> {

    /** The Constant logger. */
    static final Logger logger = LoggerFactory.getLogger(SpyMemcached.class);

    /** The mc. */
    private MemcachedClient mc;

    /**
     * Instantiates a new spy memcached.
     *
     * @param serverUrl the server url
     */
    public SpyMemcached(final String serverUrl) {
        this(serverUrl, DEFAULT_TIMEOUT);
    }

    /**
     * Instantiates a new spy memcached.
     *
     * @param serverUrl the server url
     * @param timeout the timeout
     */
    public SpyMemcached(final String serverUrl, final long timeout) {
        this(serverUrl, timeout, null);

    }

    /**
     * Instantiates a new spy memcached.
     *
     * @param serverUrl the server url
     * @param timeout the timeout
     * @param transcoder the transcoder
     */
    public SpyMemcached(final String serverUrl, final long timeout, final Transcoder<Object> transcoder) {
        super(serverUrl);

        ConnectionFactory connFactory = new DefaultConnectionFactory() {
            @Override
            public long getOperationTimeout() {
                return timeout;
            }

            @Override
            public Transcoder<Object> getDefaultTranscoder() {
                if (transcoder != null) {
                    return transcoder;
                } else {
                    return super.getDefaultTranscoder();
                }
            }
        };

        mc = createSpyMemcachedClient(serverUrl, connFactory);
    }

    /**
     * Gets the.
     *
     * @param key the key
     * @return the t
     */
    @SuppressWarnings("unchecked")
    @Override
    public T get(String key) {
        return (T) mc.get(key);
    }

    /**
     * Async get.
     *
     * @param key the key
     * @return the future
     */
    @SuppressWarnings("unchecked")
    public Future<T> asyncGet(String key) {
        return (Future<T>) mc.asyncGet(key);
    }

    /**
     * Gets the bulk.
     *
     * @param keys the keys
     * @return the bulk
     */
    @SuppressWarnings("unchecked")
    @Override
    @SafeVarargs
    public final Map<String, T> getBulk(String... keys) {
        return (Map<String, T>) mc.getBulk(keys);
    }

    /**
     * Async get bulk.
     *
     * @param keys the keys
     * @return the future
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @SafeVarargs
    public final Future<Map<String, T>> asyncGetBulk(String... keys) {
        return (Future) mc.asyncGetBulk(keys);
    }

    /**
     * Gets the bulk.
     *
     * @param keys the keys
     * @return the bulk
     */
    @SuppressWarnings("unchecked")
    @Override
    public Map<String, T> getBulk(Collection<String> keys) {
        return (Map<String, T>) mc.getBulk(keys);
    }

    /**
     * Async get bulk.
     *
     * @param keys the keys
     * @return the future
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Future<Map<String, T>> asyncGetBulk(Collection<String> keys) {
        return (Future) mc.asyncGetBulk(keys);
    }

    /**
     * Sets the.
     *
     * @param key the key
     * @param obj the obj
     * @param liveTime the live time
     * @return true, if successful
     */
    @Override
    public boolean set(String key, T obj, long liveTime) {
        return resultOf(mc.set(key, toSeconds(liveTime), obj));
    }

    /**
     * Async set.
     *
     * @param key the key
     * @param obj the obj
     * @param liveTime the live time
     * @return the future
     */
    public Future<Boolean> asyncSet(String key, T obj, long liveTime) {
        return mc.set(key, toSeconds(liveTime), obj);
    }

    /**
     * Adds the.
     *
     * @param key the key
     * @param obj the obj
     * @param liveTime the live time
     * @return true, if successful
     */
    public boolean add(String key, T obj, long liveTime) {
        return resultOf(mc.add(key, toSeconds(liveTime), obj));
    }

    /**
     * Async add.
     *
     * @param key the key
     * @param obj the obj
     * @param liveTime the live time
     * @return the future
     */
    public Future<Boolean> asyncAdd(String key, T obj, long liveTime) {
        return mc.add(key, toSeconds(liveTime), obj);
    }

    /**
     * Replace.
     *
     * @param key the key
     * @param obj the obj
     * @param liveTime the live time
     * @return true, if successful
     */
    public boolean replace(String key, T obj, long liveTime) {
        return resultOf(mc.replace(key, toSeconds(liveTime), obj));
    }

    /**
     * Async replace.
     *
     * @param key the key
     * @param obj the obj
     * @param liveTime the live time
     * @return the future
     */
    public Future<Boolean> asyncReplace(String key, T obj, long liveTime) {
        return mc.replace(key, toSeconds(liveTime), obj);
    }

    /**
     * Delete.
     *
     * @param key the key
     * @return true, if successful
     */
    @Override
    public boolean delete(String key) {
        return resultOf(mc.delete(key));
    }

    /**
     * Async delete.
     *
     * @param key the key
     * @return the future
     */
    public Future<Boolean> asyncDelete(String key) {
        return mc.delete(key);
    }

    /**
     * Incr.
     *
     * @param key the key
     * @return the long
     */
    @Override
    public long incr(String key) {
        return mc.incr(key, 1);
    }

    /**
     * Incr.
     *
     * @param key the key
     * @param deta the deta
     * @return the long
     */
    @Override
    public long incr(String key, int deta) {
        return mc.incr(key, deta);
    }

    /**
     * Incr.
     *
     * @param key the key
     * @param deta the deta
     * @param defaultValue the default value
     * @return the long
     */
    public long incr(String key, int deta, long defaultValue) {
        return mc.incr(key, deta, defaultValue, -1);
    }

    /**
     * Incr.
     *
     * @param key the key
     * @param deta the deta
     * @param defaultValue the default value
     * @param liveTime the live time
     * @return the long
     */
    public long incr(String key, int deta, long defaultValue, long liveTime) {
        return mc.incr(key, deta, defaultValue, toSeconds(liveTime));
    }

    /**
     * Decr.
     *
     * @param key the key
     * @return the long
     */
    @Override
    public long decr(String key) {
        return mc.decr(key, 1);
    }

    /**
     * Decr.
     *
     * @param key the key
     * @param deta the deta
     * @return the long
     */
    @Override
    public long decr(String key, int deta) {
        return mc.decr(key, deta);
    }

    /**
     * Decr.
     *
     * @param key the key
     * @param deta the deta
     * @param defaultValue the default value
     * @return the long
     */
    public long decr(String key, int deta, long defaultValue) {
        return mc.decr(key, deta, defaultValue, -1);
    }

    /**
     * Decr.
     *
     * @param key the key
     * @param deta the deta
     * @param defaultValue the default value
     * @param liveTime the live time
     * @return the long
     */
    public long decr(String key, int deta, long defaultValue, long liveTime) {
        return mc.decr(key, deta, defaultValue, toSeconds(liveTime));
    }

    /**
     * Flush all.
     */
    @Override
    public void flushAll() {
        resultOf(mc.flush());
    }

    /**
     * Async flush all.
     *
     * @return the future
     */
    public Future<Boolean> asyncFlushAll() {
        return mc.flush();
    }

    /**
     * Flush all.
     *
     * @param delay the delay
     * @return true, if successful
     */
    public boolean flushAll(long delay) {
        return resultOf(mc.flush(toSeconds(delay)));
    }

    /**
     * Async flush all.
     *
     * @param delay the delay
     * @return the future
     */
    public Future<Boolean> asyncFlushAll(long delay) {
        return mc.flush(toSeconds(delay));
    }

    /**
     * Disconnect.
     */
    @Override
    public void disconnect() {
        mc.shutdown();
    }

    /**
     * Disconnect.
     *
     * @param timeout the timeout
     */
    public void disconnect(long timeout) {
        mc.shutdown(timeout, TimeUnit.MICROSECONDS);
    }

    /**
     * Result of.
     *
     * @param <R> the generic type
     * @param future the future
     * @return the r
     */
    protected <R> R resultOf(Future<R> future) {
        try {
            return future.get();
        } catch (InterruptedException e) {
            throw N.toRuntimeException(e);
        } catch (ExecutionException e) {
            throw N.toRuntimeException(e);
        }
    }

    /**
     * Creates the spy memcached client.
     *
     * @param serverUrl the server url
     * @param connFactory the conn factory
     * @return the net.spy.memcached. memcached client
     * @throws UncheckedIOException the unchecked IO exception
     */
    protected net.spy.memcached.MemcachedClient createSpyMemcachedClient(String serverUrl, ConnectionFactory connFactory) throws UncheckedIOException {
        try {
            return new net.spy.memcached.MemcachedClient(connFactory, AddrUtil.getAddressList(serverUrl));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to create Memcached client.", e);
        }
    }
}
