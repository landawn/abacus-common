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

import net.spy.memcached.CachedData;
import net.spy.memcached.transcoders.Transcoder;

import com.landawn.abacus.parser.KryoParser;
import com.landawn.abacus.parser.ParserFactory;

/**
 *
 * @author Haiyang Li
 * @param <T>
 * @since 0.8
 */
public class KryoTranscoder<T> implements Transcoder<T> {

    private static final KryoParser kryoParser = ParserFactory.createKryoParser();

    private final int maxSize;

    public KryoTranscoder() {
        this(CachedData.MAX_SIZE);
    }

    public KryoTranscoder(int maxSize) {
        this.maxSize = maxSize;
    }

    /**
     *
     * @param d
     * @return true, if successful
     */
    @Override
    public boolean asyncDecode(CachedData d) {
        return false;
    }

    /**
     *
     * @param o
     * @return
     */
    @Override
    public CachedData encode(T o) {
        return new CachedData(0, kryoParser.encode(o), maxSize);
    }

    /**
     *
     * @param d
     * @return
     */
    @Override
    public T decode(CachedData d) {
        return kryoParser.decode(d.getData());
    }

    /**
     * Gets the max size.
     *
     * @return
     */
    @Override
    public int getMaxSize() {
        return maxSize;
    }
}
