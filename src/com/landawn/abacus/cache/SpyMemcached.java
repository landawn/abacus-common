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

/**
 * 
 * @since 0.8
 * 
 * @author Haiyang Li
 */
public class SpyMemcached<T> extends AbstractDistributedCacheClient<T> {
    static final Logger logger = LoggerFactory.getLogger(SpyMemcached.class);
    private MemcachedClient mc;

    public SpyMemcached(final String serverUrl) {
        this(serverUrl, DEFAULT_TIMEOUT);
    }

    public SpyMemcached(final String serverUrl, final long timeout) {
        this(serverUrl, timeout, null);

    }

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

    @SuppressWarnings("unchecked")
    @Override
    public T get(String key) {
        return (T) mc.get(key);
    }

    @SuppressWarnings("unchecked")
    public Future<T> asyncGet(String key) {
        return (Future<T>) mc.asyncGet(key);
    }

    @SuppressWarnings("unchecked")
    @Override
    @SafeVarargs
    public final Map<String, T> getBulk(String... keys) {
        return (Map<String, T>) mc.getBulk(keys);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @SafeVarargs
    public final Future<Map<String, T>> asyncGetBulk(String... keys) {
        return (Future) mc.asyncGetBulk(keys);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, T> getBulk(Collection<String> keys) {
        return (Map<String, T>) mc.getBulk(keys);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Future<Map<String, T>> asyncGetBulk(Collection<String> keys) {
        return (Future) mc.asyncGetBulk(keys);
    }

    @Override
    public boolean set(String key, T obj, long liveTime) {
        return resultOf(mc.set(key, toSeconds(liveTime), obj));
    }

    public Future<Boolean> asyncSet(String key, T obj, long liveTime) {
        return mc.set(key, toSeconds(liveTime), obj);
    }

    public boolean add(String key, T obj, long liveTime) {
        return resultOf(mc.add(key, toSeconds(liveTime), obj));
    }

    public Future<Boolean> asyncAdd(String key, T obj, long liveTime) {
        return mc.add(key, toSeconds(liveTime), obj);
    }

    public boolean replace(String key, T obj, long liveTime) {
        return resultOf(mc.replace(key, toSeconds(liveTime), obj));
    }

    public Future<Boolean> asyncReplace(String key, T obj, long liveTime) {
        return mc.replace(key, toSeconds(liveTime), obj);
    }

    @Override
    public boolean delete(String key) {
        return resultOf(mc.delete(key));
    }

    public Future<Boolean> asyncDelete(String key) {
        return mc.delete(key);
    }

    @Override
    public long incr(String key) {
        return mc.incr(key, 1);
    }

    @Override
    public long incr(String key, int deta) {
        return mc.incr(key, deta);
    }

    public long incr(String key, int deta, long defaultValue) {
        return mc.incr(key, deta, defaultValue, -1);
    }

    public long incr(String key, int deta, long defaultValue, long liveTime) {
        return mc.incr(key, deta, defaultValue, toSeconds(liveTime));
    }

    @Override
    public long decr(String key) {
        return mc.decr(key, 1);
    }

    @Override
    public long decr(String key, int deta) {
        return mc.decr(key, deta);
    }

    public long decr(String key, int deta, long defaultValue) {
        return mc.decr(key, deta, defaultValue, -1);
    }

    public long decr(String key, int deta, long defaultValue, long liveTime) {
        return mc.decr(key, deta, defaultValue, toSeconds(liveTime));
    }

    @Override
    public void flushAll() {
        resultOf(mc.flush());
    }

    public Future<Boolean> asyncFlushAll() {
        return mc.flush();
    }

    public boolean flushAll(long delay) {
        return resultOf(mc.flush(toSeconds(delay)));
    }

    public Future<Boolean> asyncFlushAll(long delay) {
        return mc.flush(toSeconds(delay));
    }

    @Override
    public void disconnect() {
        mc.shutdown();
    }

    public void disconnect(long timeout) {
        mc.shutdown(timeout, TimeUnit.MICROSECONDS);
    }

    protected <R> R resultOf(Future<R> future) {
        try {
            return future.get();
        } catch (InterruptedException e) {
            throw N.toRuntimeException(e);
        } catch (ExecutionException e) {
            throw N.toRuntimeException(e);
        }
    }

    protected net.spy.memcached.MemcachedClient createSpyMemcachedClient(String serverUrl, ConnectionFactory connFactory) throws UncheckedIOException {
        try {
            return new net.spy.memcached.MemcachedClient(connFactory, AddrUtil.getAddressList(serverUrl));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to create Memcached client.", e);
        }
    }
}
