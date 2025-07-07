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
 * An abstract base class for objects that have an associated index value.
 * This class provides a foundation for indexed data structures where elements
 * need to maintain their position or index information.
 * 
 * <p>The class is immutable and stores a long index value that can be retrieved
 * as either an int or long value.</p>
 * 
 * @see Immutable
 * @since 1.0
 */
@com.landawn.abacus.annotation.Immutable
abstract class AbstractIndexed implements Immutable {

    /** The index value associated with this object */
    protected final long index;

    /**
     * Constructs an AbstractIndexed object with the specified index.
     * 
     * @param index the index value to associate with this object
     */
    protected AbstractIndexed(final long index) {
        this.index = index;
    }

    /**
     * Returns the index value as an int.
     * 
     * <p>Note: This method performs a narrowing conversion from long to int,
     * which may result in loss of precision if the index value exceeds
     * Integer.MAX_VALUE or is less than Integer.MIN_VALUE.</p>
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * int idx = indexedObject.index();
     * System.out.println("Index: " + idx);
     * }</pre>
     * 
     * @return the index value as an int
     */
    public int index() {
        return (int) index;
    }

    /**
     * Returns the index value as a long.
     * 
     * <p>This method returns the full precision index value without any
     * conversion or potential loss of data.</p>
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * long idx = indexedObject.longIndex();
     * System.out.println("Long index: " + idx);
     * }</pre>
     * 
     * @return the index value as a long
     */
    public long longIndex() {
        return index;
    }
}