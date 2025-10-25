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
 * Abstract base class for atomic types in the type system.
 * Atomic types are types that represent single, indivisible values that can be
 * safely used in concurrent operations without additional synchronization.
 * Examples include AtomicInteger, AtomicLong, AtomicBoolean, etc.
 *
 * @param <T> the atomic type (e.g., AtomicInteger, AtomicLong, AtomicBoolean)
 */
public abstract class AbstractAtomicType<T> extends AbstractType<T> {

    protected AbstractAtomicType(final String typeName) {
        super(typeName);
    }

    /**
     * Checks if this type represents values that should not be quoted in CSV format.
     * For atomic types, this method always returns {@code true}, indicating that
     * atomic values should be written without quotes in CSV files.
     * This is because atomic types typically represent numeric or boolean values
     * that are self-delimiting and don't require quotation marks.
     *
     * @return {@code true}, indicating that atomic type values should not be quoted in CSV format
     */
    @Override
    public boolean isNonQuotableCsvType() {
        return true;
    }
}