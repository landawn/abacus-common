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
 * Type handler for the primitive {@code char} type, as opposed to the {@link Character} wrapper class.
 * It provides type information and default value handling for {@code char} primitives.
 *
 * <p>The key distinction from {@code CharacterType} (which handles {@link Character}) is the default value:
 * {@link #defaultValue()} returns {@code '\u0000'} (matching the JLS default for {@code char}),
 * whereas the wrapper type's default is {@code null}.</p>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * Type<Character> type = TypeFactory.getType(char.class);
 *
 * // Convert string to char
 * Character value = type.valueOf("A");   // Returns 'A'
 * Character value2 = type.valueOf("9");   // Returns '9'
 *
 * // Get default value
 * Character defaultVal = type.defaultValue();   // Returns '\0' (null character)
 *
 * // Read from database
 * try (ResultSet rs = stmt.executeQuery("SELECT grade FROM grades")) {
 *     if (rs.next()) {
 *         Character grade = type.get(rs, 1);
 *     }
 * }
 * }</pre>
 */
@SuppressWarnings("java:S2160")
public final class PrimitiveCharType extends AbstractCharacterType {

    /** The type name constant for the primitive {@code char} type, equal to {@code "char"}. */
    public static final String CHAR = char.class.getSimpleName();

    private static final Character DEFAULT_VALUE = 0;

    /**
     * Constructs a new PrimitiveCharType instance.
     * This constructor is package-private and intended to be called only by the TypeFactory.
     */
    PrimitiveCharType() {
        super(CHAR);
    }

    /**
     * Returns the Class object representing the primitive char type.
     * Note that this returns char.class, not Character.class.
     *
     * @return the Class object for the primitive char type
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public Class javaType() {
        return char.class;
    }

    /**
     * Indicates whether this type represents a primitive type.
     * Always returns {@code true} for PrimitiveCharType.
     *
     * @return {@code true}, as this type handler is for primitive char
     */
    @Override
    public boolean isPrimitive() {
        return true;
    }

    /**
     * Returns the default value for the primitive char type.
     * The default value for char primitives is the {@code null} character (Unicode 0).
     *
     * @return Character value of 0 ('\0') as the default value for primitive char
     */
    @Override
    public Character defaultValue() {
        return DEFAULT_VALUE;
    }
}
