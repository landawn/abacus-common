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

/**
 *
 * @author Haiyang Li
 * @since 0.8
 */
public final class IndexedLong extends AbstractIndexed {

    private final long value;

    IndexedLong(long index, long value) {
        super(index);
        this.value = value;
    }

    /**
     * 
     *
     * @param value 
     * @param index 
     * @return 
     * @throws IllegalArgumentException 
     */
    public static IndexedLong of(long value, int index) throws IllegalArgumentException {
        N.checkArgNotNegative(index, "index");

        return new IndexedLong(index, value);
    }

    /**
     * 
     *
     * @param value 
     * @param index 
     * @return 
     * @throws IllegalArgumentException 
     */
    public static IndexedLong of(long value, long index) throws IllegalArgumentException {
        N.checkArgNotNegative(index, "index");

        return new IndexedLong(index, value);
    }

    /**
     * 
     *
     * @return 
     */
    public long value() {
        return value;
    }

    /**
     * 
     *
     * @return 
     */
    @Override
    public int hashCode() {
        return (int) index + (int) (value * 31);
    }

    /**
     *
     * @param obj
     * @return
     */
    @Override
    public boolean equals(Object obj) {
        return obj instanceof IndexedLong && ((IndexedLong) obj).index == index && N.equals(((IndexedLong) obj).value, value);
    }

    /**
     * 
     *
     * @return 
     */
    @Override
    public String toString() {
        return "[" + index + "]=" + value;
    }

}
