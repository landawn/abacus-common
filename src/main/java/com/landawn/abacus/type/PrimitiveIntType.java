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
 * Type handler for the primitive {@code int} type, as opposed to the {@link Integer} wrapper class.
 * It provides type information and default value handling for {@code int} primitives.
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * Type<Integer> type = TypeFactory.getType(int.class);
 *
 * // Convert string to int
 * Integer value = type.valueOf("42");   // Returns 42
 *
 * // Get default value
 * Integer defaultVal = type.defaultValue();   // Returns 0
 *
 * // Read from database
 * try (ResultSet rs = stmt.executeQuery("SELECT age FROM users")) {
 *     if (rs.next()) {
 *         Integer age = type.get(rs, 1);
 *     }
 * }
 * }</pre>
 */
@SuppressWarnings("java:S2160")
public final class PrimitiveIntType extends AbstractIntegerType {

    public static final String INT = int.class.getSimpleName();

    private static final Integer DEFAULT_VALUE = 0;

    /**
     * Constructs a new PrimitiveIntType instance.
     * This constructor is package-private and intended to be called only by the TypeFactory.
     */
    PrimitiveIntType() {
        super(INT);
    }

    /**
     * Returns the Class object representing the primitive int type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Integer> type = TypeFactory.getType(int.class);
     * Class<?> clazz = type.javaType();
     * // clazz equals int.class
     * }</pre>
     *
     * @return the Class object for int.class
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public Class javaType() {
        return int.class;
    }

    /**
     * Indicates whether this type represents a primitive type.
     * For PrimitiveIntType, this always returns {@code true} since it represents the primitive int type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Integer> type = TypeFactory.getType(int.class);
     * boolean isPrimitive = type.isPrimitive();
     * // isPrimitive is true
     * }</pre>
     *
     * @return {@code true}, indicating this is a primitive type
     */
    @Override
    public boolean isPrimitive() {
        return true;
    }

    /**
     * Returns the default value for the primitive int type.
     * The default value for primitive int is 0.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Integer> type = TypeFactory.getType(int.class);
     * Integer defaultVal = type.defaultValue();
     * // defaultVal equals 0
     * }</pre>
     *
     * @return Integer object containing the value 0
     */
    @Override
    public Integer defaultValue() {
        return DEFAULT_VALUE;
    }
}
