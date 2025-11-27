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
 * Abstract base class for primary types in the type system.
 * Primary types are fundamental, immutable types that can be directly compared
 * and converted from objects. This includes primitive wrappers (Integer, Double, etc.),
 * String, Character, and other basic value types.
 *
 * @param <T> the primary type (e.g., Integer, String, Boolean, Character)
 */
public abstract class AbstractPrimaryType<T> extends AbstractType<T> {

    /**
     * Constructs an AbstractPrimaryType with the specified type name.
     *
     * @param typeName the name of the primary type (e.g., "Integer", "String", "Boolean")
     */
    protected AbstractPrimaryType(final String typeName) {
        super(typeName);
    }

    /**
     * Checks if this type is immutable.
     * Primary types are always immutable, meaning their values cannot be changed
     * after creation. This includes all primitive wrappers and String.
     *
     * @return {@code true}, indicating that primary types are immutable
     */
    @Override
    public boolean isImmutable() {
        return true;
    }

    /**
     * Checks if this type is comparable.
     * Primary types implement natural ordering and can be compared with each other.
     * This allows them to be used in sorted collections and comparison operations.
     *
     * @return {@code true}, indicating that primary types support comparison
     */
    @Override
    public boolean isComparable() {
        return true;
    }

    /**
     * Converts an object to this primary type.
     * This method provides a generic way to convert any object to the primary type
     * by first converting it to a string representation using the object's actual type,
     * then parsing that string to create the primary type value.
     * Returns the default value if the input object is {@code null}.
     *
     * @param obj the object to convert
     * @return the converted primary type value, or default value if input is {@code null}
     */
    @Override
    public T valueOf(final Object obj) {
        if (obj == null) {
            return defaultValue();
        }

        if (obj instanceof String) {
            return valueOf((String) obj);
        }

        return valueOf(Type.<Object> of(obj.getClass()).stringOf(obj));
    }
}
