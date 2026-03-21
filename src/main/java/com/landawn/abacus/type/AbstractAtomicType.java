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
 * The abstract base class for atomic types in the type system.
 * Atomic types are types that represent single, indivisible values that can be
 * safely used in concurrent operations without additional synchronization.
 * Examples include {@code AtomicInteger}, {@code AtomicLong}, {@code AtomicBoolean}, etc.
 *
 * @param <T> the atomic type (e.g., {@code AtomicInteger}, {@code AtomicLong}, {@code AtomicBoolean})
 */
public abstract class AbstractAtomicType<T> extends AbstractType<T> {

    /**
     * Constructs a new {@code AbstractAtomicType} with the specified type name.
     *
     * @param typeName the name of the atomic type (e.g., "AtomicInteger", "AtomicLong", "AtomicBoolean")
     */
    protected AbstractAtomicType(final String typeName) {
        super(typeName);
    }

    /**
     * Returns {@code false} because atomic types represent numeric or boolean values that are self-delimiting
     * and do not require quotation marks in CSV format.
     *
     * @return {@code false}
     */
    @Override
    public boolean isCsvQuoteRequired() {
        return false;
    }
}
