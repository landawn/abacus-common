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
public final class IndexedFloat extends AbstractIndexed {

    private final float value;

    IndexedFloat(long index, float value) {
        super(index);
        this.value = value;
    }

    /**
     *
     * @param value
     * @param index
     * @return
     */
    public static IndexedFloat of(float value, int index) {
        N.checkArgNotNegative(index, "index");

        return new IndexedFloat(index, value);
    }

    /**
     *
     * @param value
     * @param index
     * @return
     */
    public static IndexedFloat of(float value, long index) {
        N.checkArgNotNegative(index, "index");

        return new IndexedFloat(index, value);
    }

    /**
     *
     *
     * @return
     */
    public float value() {
        return value;
    }

    /**
     *
     *
     * @return
     */
    @Override
    public int hashCode() {
        return (int) index + (int) (value * 31); // NOSONAR
    }

    /**
     *
     * @param obj
     * @return
     */
    @Override
    public boolean equals(Object obj) {
        return obj instanceof IndexedFloat && ((IndexedFloat) obj).index == index && N.equals(((IndexedFloat) obj).value, value);
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
