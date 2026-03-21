/*
 * Copyright (C) 2019 HaiYang Li
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
 * The abstract base class for {@code CharSequence} types in the type system.
 * This class provides common functionality for handling {@code CharSequence} implementations
 * such as {@code String}, {@code StringBuilder}, {@code StringBuffer}, and other custom {@code CharSequence} types.
 * {@code CharSequence} types are treated as primary types that can be directly serialized
 * and compared.
 *
 * @param <T> the specific {@code CharSequence} type (e.g., {@code String}, {@code StringBuilder}, {@code StringBuffer})
 */
public abstract class AbstractCharSequenceType<T extends CharSequence> extends AbstractPrimaryType<T> {

    /**
     * Constructs a new {@code AbstractCharSequenceType} with the specified type name.
     *
     * @param typeName the name of the {@code CharSequence} type (e.g., "String", "StringBuilder")
     */
    protected AbstractCharSequenceType(final String typeName) {
        super(typeName);
    }

    /**
     * Returns {@code true} because this type represents a {@code CharSequence} type.
     *
     * @return {@code true}
     */
    @Override
    public boolean isCharSequence() {
        return true;
    }
}
