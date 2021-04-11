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

package com.landawn.abacus.type;

/**
 *
 * @author Haiyang Li
 * @param <T>
 * @since 0.8
 */
public abstract class AbstractPrimitiveArrayType<T> extends AbstractArrayType<T> {

    protected AbstractPrimitiveArrayType(String typeName) {
        super(typeName);
    }

    /**
     * Checks if is primitive array.
     *
     * @return true, if is primitive array
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
    public int deepHashCode(T x) {
        return hashCode(x);
    }

    /**
     *
     * @param x
     * @param y
     * @return true, if successful
     */
    @Override
    public boolean deepEquals(T x, T y) {
        return equals(x, y);
    }

    /**
     *
     * @param x
     * @return
     */
    @Override
    public String toString(T x) {
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
    public String deepToString(T x) {
        return toString(x);
    }
}
