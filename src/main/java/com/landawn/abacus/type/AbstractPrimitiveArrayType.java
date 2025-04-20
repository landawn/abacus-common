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

package com.landawn.abacus.type;

/**
 *
 * @param <T>
 */
public abstract class AbstractPrimitiveArrayType<T> extends AbstractArrayType<T> {

    protected AbstractPrimitiveArrayType(final String typeName) {
        super(typeName);
    }

    /**
     * Checks if is a primitive array.
     *
     * @return {@code true}, if is primitive array
     */
    @Override
    public boolean isPrimitiveArray() {
        return true;
    }

    /**
     * Deep hash code.
     *
     * @param x
     * @return
     */
    @Override
    public int deepHashCode(final T x) {
        return hashCode(x);
    }

    /**
     *
     * @param x
     * @param y
     * @return {@code true}, if successful
     */
    @Override
    public boolean deepEquals(final T x, final T y) {
        return equals(x, y);
    }

    /**
     *
     * @param x
     * @return
     */
    @Override
    public String toString(final T x) {
        if (x == null) {
            return NULL_STRING;
        }

        return stringOf(x);
    }

    /**
     * Deep to string.
     *
     * @param x
     * @return
     */
    @Override
    public String deepToString(final T x) {
        return toString(x);
    }
}
