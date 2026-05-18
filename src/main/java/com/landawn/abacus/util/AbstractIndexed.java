/*
 * Copyright (C) 2016 HaiYang Li
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

package com.landawn.abacus.util;

/**
 * An abstract base class for objects that pair a value with an associated index position.
 * Subclasses such as {@link Indexed}, {@link IndexedBoolean}, {@link IndexedByte}, etc.
 * use this class to wrap a typed value together with its index.
 *
 * <p>The class is immutable once constructed. The index is stored as a {@code long} and
 * can be retrieved as either an {@code int} (via {@link #index()}, which throws
 * {@link ArithmeticException} on overflow) or as a {@code long} (via {@link #longIndex()}).</p>
 *
 * @see Indexed
 */
@com.landawn.abacus.annotation.Immutable
abstract class AbstractIndexed implements Immutable {

    /** The non-negative index position associated with this object, stored as a {@code long}. */
    protected final long index;

    /**
     * Constructs an {@code AbstractIndexed} instance with the specified index value.
     *
     * @param index the index position to associate with this object (must be non-negative)
     */
    protected AbstractIndexed(final long index) {
        this.index = index;
    }

    /**
     * Returns the index value as an {@code int}.
     *
     * <p>This method converts the internal {@code long} index to an {@code int}.
     * If the index exceeds {@link Integer#MAX_VALUE}, an {@link ArithmeticException}
     * is thrown to prevent silent data loss. Use {@link #longIndex()} when the index
     * may exceed the {@code int} range.</p>
     *
     * @return the index value as an {@code int}
     * @throws ArithmeticException if the index value overflows an {@code int}
     * @see #longIndex()
     * @see Math#toIntExact(long)
     */
    public int index() throws ArithmeticException {
        return Math.toIntExact(index);
    }

    /**
     * Returns the index value as a {@code long}.
     *
     * <p>Unlike {@link #index()}, this method never throws an exception and
     * safely represents any index value within the full {@code long} range.</p>
     *
     * @return the index value as a {@code long}
     * @see #index()
     */
    public long longIndex() {
        return index;
    }
}
