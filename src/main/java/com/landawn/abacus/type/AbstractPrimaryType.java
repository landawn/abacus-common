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
 * The abstract base class for primary types in the type system.
 * <p>
 * Primary types are fundamental value types that can be directly converted from arbitrary objects.
 * This base implementation treats them as immutable and comparable. Specialized subclasses may
 * override those defaults when the represented values are mutable or not naturally comparable.
 * This includes primitive wrappers
 * ({@code Integer}, {@code Long}, {@code Double}, etc.),
 * {@code String}, {@code Character}, {@code Boolean}, and other basic value types.
 * </p>
 *
 * @param <T> the primary type (e.g., {@code Integer}, {@code String}, {@code Boolean}, {@code Character})
 */
public abstract class AbstractPrimaryType<T> extends AbstractType<T> {

    /**
     * Constructs an {@code AbstractPrimaryType} with the specified type name.
     *
     * @param typeName the name of the primary type (e.g., "Integer", "String", "Boolean")
     */
    protected AbstractPrimaryType(final String typeName) {
        super(typeName);
    }

    /**
     * Returns {@code true} for this base implementation. Specialized primary types may override this
     * when their represented values are mutable.
     *
     * @return {@code true} by default
     */
    @Override
    public boolean isImmutable() {
        return true;
    }

    /**
     * Returns {@code true} for this base implementation. Specialized primary types may override this
     * when their represented values are not naturally comparable.
     *
     * @return {@code true} by default
     */
    @Override
    public boolean isComparable() {
        return true;
    }

    /**
     * Converts an object to this primary type.
     * <p>
     * This method provides a generic way to convert an object to the primary type:
     * </p>
     * <ul>
     *   <li>Returns the default value if the input object is {@code null}.</li>
     *   <li>If the object is already a {@code String}, it is parsed directly via {@link #valueOf(String)}.</li>
     *   <li>Otherwise the object is first converted to a string representation using the type
     *       registered for the object's actual class, and that string is then parsed via
     *       {@link #valueOf(String)} to create the primary type value.</li>
     * </ul>
     *
     * @param obj the object to convert, may be {@code null}
     * @return the converted primary type value, or the default value if {@code obj} is {@code null}
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
