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
 * Abstract base class for CharSequence types in the type system.
 * This class provides common functionality for handling CharSequence implementations
 * such as String, StringBuilder, StringBuffer, and other custom CharSequence types.
 * CharSequence types are treated as primary types that can be directly serialized
 * and compared.
 *
 * @param <T> the specific CharSequence type (e.g., String, StringBuilder, StringBuffer)
 */
public abstract class AbstractCharSequenceType<T extends CharSequence> extends AbstractPrimaryType<T> {

    /**
     * Constructs an AbstractCharSequenceType with the specified type name.
     *
     * @param typeName the name of the CharSequence type (e.g., "String", "StringBuilder")
     */
    protected AbstractCharSequenceType(String typeName) {
        super(typeName);
    }

    /**
     * Checks if this type represents a CharSequence type.
     * This method always returns {@code true} for CharSequence types,
     * indicating that the type handles objects that implement the CharSequence interface.
     *
     * @return {@code true}, indicating this is a CharSequence type
     */
    @Override
    public boolean isCharSequence() {
        return true;
    }

}
