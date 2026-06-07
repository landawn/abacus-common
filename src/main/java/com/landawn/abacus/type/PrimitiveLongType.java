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
 * Type handler for the primitive {@code long} type, as opposed to the {@link Long} wrapper class.
 * It provides type information and default value handling for {@code long} primitives.
 *
 * <p>The key distinction from {@code LongType} (which handles {@link Long}) is the default value:
 * {@link #defaultValue()} returns {@code 0L} (matching the JLS default for {@code long}),
 * whereas the wrapper type's default is {@code null}.</p>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * Type<Long> type = TypeFactory.getType(long.class);
 *
 * // Convert string to long
 * Long value = type.valueOf("123456789");   // Returns 123456789L
 *
 * // Get default value
 * Long defaultVal = type.defaultValue();   // Returns 0L
 *
 * // Read from database
 * try (ResultSet rs = stmt.executeQuery("SELECT user_id FROM users")) {
 *     if (rs.next()) {
 *         Long userId = type.get(rs, 1);
 *     }
 * }
 * }</pre>
 *
 */
@SuppressWarnings("java:S2160")
public final class PrimitiveLongType extends AbstractLongType {

    /** The type name constant for the primitive {@code long} type, equal to {@code "long"}. */
    public static final String LONG = long.class.getSimpleName();

    private static final Long DEFAULT_VALUE = 0L;

    /**
     * Constructs a new PrimitiveLongType instance.
     * This constructor is package-private and intended to be called only by the TypeFactory.
     */
    PrimitiveLongType() {
        super(LONG);
    }

    /**
     * Returns the Class object representing the primitive long type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Long> type = TypeFactory.getType(long.class);
     * Class clazz = type.javaType();
     * System.out.println(clazz.getName());       // Output: long
     * System.out.println(clazz.isPrimitive());   // Output: true
     * }</pre>
     *
     * @return the Class object for long.class
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public Class javaType() {
        return long.class;
    }

    /**
     * Indicates whether this type represents a primitive type.
     * For PrimitiveLongType, this always returns {@code true} since it represents the primitive long type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Long> type = TypeFactory.getType(long.class);
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
     * Returns the default value for the primitive long type.
     * The default value for primitive long is 0L.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Long> type = TypeFactory.getType(long.class);
     * Long defaultVal = type.defaultValue();
     * System.out.println(defaultVal);   // Output: 0
     * }</pre>
     *
     * @return Long object containing the value 0L
     */
    @Override
    public Long defaultValue() {
        return DEFAULT_VALUE;
    }
}
