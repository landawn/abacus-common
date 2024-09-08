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
public final class IndexedChar extends AbstractIndexed {

    private final char value;

    IndexedChar(final long index, final char value) {
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
    public static IndexedChar of(final char value, final int index) throws IllegalArgumentException {
        N.checkArgNotNegative(index, cs.index);

        return new IndexedChar(index, value);
    }

    /**
     *
     *
     * @param value
     * @param index
     * @return
     * @throws IllegalArgumentException
     */
    public static IndexedChar of(final char value, final long index) throws IllegalArgumentException {
        N.checkArgNotNegative(index, cs.index);

        return new IndexedChar(index, value);
    }

    /**
     *
     *
     * @return
     */
    public char value() {
        return value;
    }

    /**
     *
     *
     * @return
     */
    @Override
    public int hashCode() {
        return (int) index + value * 31;
    }

    /**
     *
     * @param obj
     * @return
     */
    @Override
    public boolean equals(final Object obj) {
        return obj instanceof IndexedChar && ((IndexedChar) obj).index == index && N.equals(((IndexedChar) obj).value, value);
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
