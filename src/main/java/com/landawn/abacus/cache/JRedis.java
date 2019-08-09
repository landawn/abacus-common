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

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.landawn.abacus.parser.KryoParser;
import com.landawn.abacus.parser.ParserFactory;
import com.landawn.abacus.util.AddrUtil;
import com.landawn.abacus.util.N;

import redis.clients.jedis.BinaryShardedJedis;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisShardInfo;

// TODO: Auto-generated Javadoc
/**
 * The Class JRedis.
 *
 * @author Haiyang Li
 * @param <T> the generic type
 * @since 0.8
 */
public class JRedis<T> extends AbstractDistributedCacheClient<T> {

    /** The Constant kryoParser. */
    private static final KryoParser kryoParser = ParserFactory.createKryoParser();

    /** The jedis. */
    private final BinaryShardedJedis jedis;

    /**
     * Instantiates a new j redis.
     *
     * @param serverUrl the server url
     */
    public JRedis(String serverUrl) {
        this(serverUrl, DEFAULT_TIMEOUT);
    }

    /**
     * Instantiates a new j redis.
     *
     * @param serverUrl the server url
     * @param timeout the timeout
     */
    public JRedis(final String serverUrl, final long timeout) {
        super(serverUrl);

        List<InetSocketAddress> addressList = AddrUtil.getAddressList(serverUrl);

        List<JedisShardInfo> jedisClusterNodes = new ArrayList<>();

        for (InetSocketAddress addr : addressList) {
            jedisClusterNodes.add(new JedisShardInfo(addr.getHostName(), addr.getPort(), (int) timeout));
        }

        jedis = new BinaryShardedJedis(jedisClusterNodes);
    }

    /**
     * Gets the.
     *
     * @param key the key
     * @return the t
     */
    @Override
    public T get(String key) {
        return decode(jedis.get(getKeyBytes(key)));
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
        jedis.setex(getKeyBytes(key), toSeconds(liveTime), encode(obj));

        return true;
    }

    /**
     * Delete.
     *
     * @param key the key
     * @return true, if successful
     */
    @Override
    public boolean delete(String key) {
        jedis.del(getKeyBytes(key));

        return true;
    }

    /**
     * Incr.
     *
     * @param key the key
     * @return the long
     */
    @Override
    public long incr(String key) {
        return jedis.incr(getKeyBytes(key));
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
        return jedis.incrBy(getKeyBytes(key), deta);
    }

    /**
     * Decr.
     *
     * @param key the key
     * @return the long
     */
    @Override
    public long decr(String key) {
        return jedis.decr(getKeyBytes(key));
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
        return jedis.decrBy(getKeyBytes(key), deta);
    }

    /**
     * Flush all.
     */
    @Override
    public void flushAll() {
        final Collection<Jedis> allShards = jedis.getAllShards();

        for (Jedis j : allShards) {
            j.flushAll();
        }
    }

    /**
     * Disconnect.
     */
    @Override
    public void disconnect() {
        jedis.disconnect();
    }

    /**
     * Gets the key bytes.
     *
     * @param key the key
     * @return the key bytes
     */
    protected byte[] getKeyBytes(String key) {
        return key.getBytes();
    }

    /**
     * Encode.
     *
     * @param obj the obj
     * @return the byte[]
     */
    protected byte[] encode(Object obj) {
        return obj == null ? N.EMPTY_BYTE_ARRAY : kryoParser.encode(obj);
    }

    /**
     * Decode.
     *
     * @param bytes the bytes
     * @return the t
     */
    protected T decode(byte[] bytes) {
        return (T) (N.isNullOrEmpty(bytes) ? null : kryoParser.decode(bytes));
    }
}
