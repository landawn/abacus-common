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

import java.util.Collection;
import java.util.Map;

/**
 *
 * @author Haiyang Li
 * @param <T>
 * @since 0.8
 */
public abstract class AbstractDistributedCacheClient<T> implements DistributedCacheClient<T> {

    private final String serverUrl;

    public AbstractDistributedCacheClient(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    @Override
    public String serverUrl() {
        return serverUrl;
    }

    /**
     * Gets the bulk.
     *
     * @param keys
     * @return
     */
    @Override
    public Map<String, T> getBulk(String... keys) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Gets the bulk.
     *
     * @param keys
     * @return
     */
    @Override
    public Map<String, T> getBulk(Collection<String> keys) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Flush all.
     */
    @Override
    public void flushAll() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     *
     * @param liveTime
     * @return
     */
    protected int toSeconds(long liveTime) {
        return (int) ((liveTime % 1000 == 0) ? (liveTime / 1000) : (liveTime / 1000) + 1);
    }
}
