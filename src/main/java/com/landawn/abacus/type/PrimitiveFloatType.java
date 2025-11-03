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
 * Type handler for primitive float values.
 * This class handles the primitive float type specifically, as opposed to the Float wrapper class.
 * It provides type information and default value handling for float primitives.
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * Type<Float> type = TypeFactory.getType(float.class);
 *
 * // Convert string to float
 * Float value = type.valueOf("3.14"); // Returns 3.14f
 * Float value2 = type.valueOf("-0.5"); // Returns -0.5f
 *
 * // Get default value
 * Float defaultVal = type.defaultValue(); // Returns 0.0f
 *
 * // Read from database
 * try (ResultSet rs = stmt.executeQuery("SELECT price FROM products")) {
 *     if (rs.next()) {
 *         Float price = type.get(rs, 1);
 *     }
 * }
 * }</pre>
 */
@SuppressWarnings("java:S2160")
final class PrimitiveFloatType extends AbstractFloatType {

    public static final String FLOAT = float.class.getSimpleName();

    private static final Float DEFAULT_VALUE = 0f;

    PrimitiveFloatType() {
        super(FLOAT);
    }

    /**
     * Returns the Class object representing the primitive float type.
     * Note that this returns float.class, not Float.class.
     *
     * @return the Class object for the primitive float type
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public Class clazz() {
        return float.class;
    }

    /**
     * Indicates whether this type represents a primitive type.
     * Always returns {@code true} for PrimitiveFloatType.
     *
     * @return {@code true}, as this type handler is for primitive float
     */
    @Override
    public boolean isPrimitiveType() {
        return true;
    }

    /**
     * Returns the default value for the primitive float type.
     * The default value for float primitives is 0.0f.
     *
     * @return Float value of 0.0f as the default value for primitive float
     */
    @Override
    public Float defaultValue() {
        return DEFAULT_VALUE;
    }
}
