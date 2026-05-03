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

import com.landawn.abacus.util.PrimitiveList;

/**
 * The abstract base class for primitive list types in the type system.
 * <p>
 * This class provides common functionality for handling specialized list implementations
 * that store primitive values directly without boxing overhead.
 * Examples include {@code IntList}, {@code DoubleList}, {@code BooleanList}, etc.
 * </p>
 *
 * @param <T> the primitive list type (e.g., {@code IntList}, {@code DoubleList}, {@code BooleanList})
 */
public abstract class AbstractPrimitiveListType<T extends PrimitiveList<?, ?, ?>> extends AbstractType<T> {

    /**
     * Constructs an {@code AbstractPrimitiveListType} with the specified type name.
     *
     * @param typeName the name of the primitive list type (e.g., "IntList", "DoubleList", "BooleanList")
     */
    protected AbstractPrimitiveListType(final String typeName) {
        super(typeName);
    }

    /**
     * Returns {@code true} because this type represents a primitive list — a specialized
     * list implementation that stores primitive values directly without boxing overhead.
     *
     * @return {@code true}
     */
    @Override
    public boolean isPrimitiveList() {
        return true;
    }
}
