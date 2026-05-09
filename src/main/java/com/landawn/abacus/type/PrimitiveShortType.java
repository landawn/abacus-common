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
 * Type handler for the primitive {@code short} type, as opposed to the {@link Short} wrapper class.
 * It provides type information and default value handling for {@code short} primitives.
 *
 * <p>The key distinction from {@code ShortType} (which handles {@link Short}) is the default value:
 * {@link #defaultValue()} returns {@code (short) 0} (matching the JLS default for {@code short}),
 * whereas the wrapper type's default is {@code null}.</p>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * Type<Short> type = TypeFactory.getType(short.class);
 *
 * // Convert string to short
 * Short value = type.valueOf("100");   // Returns (short) 100
 * Short value2 = type.valueOf("-128");   // Returns (short) -128
 *
 * // Get default value
 * Short defaultVal = type.defaultValue();   // Returns 0
 *
 * // Read from database
 * try (ResultSet rs = stmt.executeQuery("SELECT quantity FROM inventory")) {
 *     if (rs.next()) {
 *         Short quantity = type.get(rs, 1);
 *     }
 * }
 * }</pre>
 */
@SuppressWarnings("java:S2160")
public final class PrimitiveShortType extends AbstractShortType {

    public static final String SHORT = short.class.getSimpleName();

    private static final Short DEFAULT_VALUE = 0;

    /**
     * Constructs a new PrimitiveShortType instance.
     * This constructor is package-private and intended to be called only by the TypeFactory.
     */
    PrimitiveShortType() {
        super(SHORT);
    }

    /**
     * Returns the Class object representing the primitive short type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Short> type = TypeFactory.getType(short.class);
     * Class clazz = type.javaType();
     * System.out.println(clazz.getName());       // Output: short
     * System.out.println(clazz.isPrimitive());   // Output: true
     * }</pre>
     *
     * @return the Class object for short.class
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public Class javaType() {
        return short.class;
    }

    /**
     * Indicates whether this type represents a primitive type.
     * For PrimitiveShortType, this always returns {@code true} since it represents the primitive short type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Short> type = TypeFactory.getType(short.class);
     * boolean isPrimitive = type.isPrimitive();
     * System.out.println(isPrimitive);   // Output: true
     * }</pre>
     *
     * @return {@code true}, indicating this is a primitive type
     */
    @Override
    public boolean isPrimitive() {
        return true;
    }

    /**
     * Returns the default value for the primitive short type.
     * The default value for primitive short is 0.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Short> type = TypeFactory.getType(short.class);
     * Short defaultVal = type.defaultValue();
     * System.out.println(defaultVal);   // Output: 0
     * }</pre>
     *
     * @return Short object containing the value 0
     */
    @Override
    public Short defaultValue() {
        return DEFAULT_VALUE;
    }
}
