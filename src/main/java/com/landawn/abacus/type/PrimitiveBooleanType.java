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
 * Type handler for the primitive {@code boolean} type, as opposed to the {@link Boolean} wrapper class.
 * It provides type information and default value handling for {@code boolean} primitives.
 *
 * <p>The key distinction from {@code BooleanType} (which handles {@link Boolean}) is the default value:
 * {@link #defaultValue()} returns {@link Boolean#FALSE} (matching the JLS default for {@code boolean}),
 * whereas the wrapper type's default is {@code null}.</p>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * Type<Boolean> type = TypeFactory.getType(boolean.class);
 *
 * // Convert string to boolean
 * Boolean value = type.valueOf("true");   // Returns true
 * Boolean value2 = type.valueOf("false");   // Returns false
 *
 * // Get default value
 * Boolean defaultVal = type.defaultValue();   // Returns false
 *
 * // Read from database
 * try (ResultSet rs = stmt.executeQuery("SELECT active FROM users")) {
 *     if (rs.next()) {
 *         Boolean active = type.get(rs, 1);
 *     }
 * }
 * }</pre>
 */
public final class PrimitiveBooleanType extends AbstractBooleanType {

    /** The primary type name constant for the primitive {@code boolean} type, equal to {@code "boolean"}. */
    public static final String BOOLEAN = boolean.class.getSimpleName();

    /** The alternate alias type name constant for the primitive {@code boolean} type, equal to {@code "bool"}. */
    public static final String BOOL = "bool";

    /**
     * Constructs a new PrimitiveBooleanType instance.
     * This constructor is package-private and intended to be called only by the TypeFactory.
     */
    PrimitiveBooleanType() {
        super(BOOLEAN);
    }

    /**
     * Returns the Class object representing the primitive boolean type.
     * Note that this returns boolean.class, not Boolean.class.
     *
     * @return the Class object for the primitive boolean type
     */
    @Override
    public Class<Boolean> javaType() {
        return boolean.class;
    }

    /**
     * Indicates whether this type represents a primitive type.
     * Always returns {@code true} for PrimitiveBooleanType.
     *
     * @return {@code true}, as this type handler is for primitive boolean
     */
    @Override
    public boolean isPrimitive() {
        return true;
    }

    /**
     * Returns the default value for the primitive boolean type.
     * The default value for boolean primitives is {@code false}.
     *
     * @return Boolean.FALSE as the default value for primitive boolean
     */
    @Override
    public Boolean defaultValue() {
        return Boolean.FALSE;
    }
}
