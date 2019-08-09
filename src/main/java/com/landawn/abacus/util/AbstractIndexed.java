/*
 * Copyright (C) 2016 HaiYang Li
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

package com.landawn.abacus.util;

// TODO: Auto-generated Javadoc
/**
 * The Class AbstractIndexed.
 *
 * @author Haiyang Li
 * @since 0.8
 */
abstract class AbstractIndexed {

    /** The index. */
    protected final long index;

    /**
     * Instantiates a new abstract indexed.
     *
     * @param index the index
     */
    protected AbstractIndexed(long index) {
        this.index = index;
    }

    /**
     * Index.
     *
     * @return the int
     */
    public int index() {
        return (int) index;
    }

    /**
     * Long index.
     *
     * @return the long
     */
    public long longIndex() {
        return index;
    }
}
