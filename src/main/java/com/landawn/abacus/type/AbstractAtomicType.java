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
 * <p>
 * Atomic types represent thread-safe, mutable single-value containers from
 * {@code java.util.concurrent.atomic} (e.g., {@code AtomicInteger}, {@code AtomicLong},
 * {@code AtomicBoolean}, {@code AtomicReference}). Values are self-delimiting numeric or boolean
 * quantities that do not require quoting in CSV format.
 * </p>
 *
 * @param <T> the atomic wrapper type handled by this class
 *            (e.g., {@link java.util.concurrent.atomic.AtomicInteger},
 *            {@link java.util.concurrent.atomic.AtomicLong},
 *            {@link java.util.concurrent.atomic.AtomicBoolean})
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
     * Returns {@code false} because atomic types hold numeric or boolean values
     * that are self-delimiting and do not require quotation marks in CSV format.
     *
     * @return {@code false}
     */
    @Override
    public boolean isCsvQuoteRequired() {
        return false;
    }
}
