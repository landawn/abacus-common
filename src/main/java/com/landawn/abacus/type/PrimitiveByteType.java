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
 * Type handler for the primitive {@code byte} type, as opposed to the {@link Byte} wrapper class.
 * It provides type information and default value handling for {@code byte} primitives,
 * which map to the SQL {@code TINYINT} type.
 *
 * <p>The key distinction from {@code ByteType} (which handles {@link Byte}) is the default value:
 * {@link #defaultValue()} returns {@code (byte) 0} (matching the JLS default for {@code byte}),
 * whereas the wrapper type's default is {@code null}.</p>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * Type<Byte> type = TypeFactory.getType(byte.class);
 *
 * // Convert string to byte
 * Byte value = type.valueOf("42");    // Returns (byte) 42
 * Byte value2 = type.valueOf("-128"); // Returns (byte) -128
 *
 * // Get default value
 * Byte defaultVal = type.defaultValue();   // Returns 0
 *
 * // Read from database
 * try (ResultSet rs = stmt.executeQuery("SELECT status_code FROM statuses")) {
 *     if (rs.next()) {
 *         Byte statusCode = type.get(rs, 1);
 *     }
 * }
 * }</pre>
 *
 */
@SuppressWarnings("java:S2160")
public final class PrimitiveByteType extends AbstractByteType {

    /** The type name constant for the primitive {@code byte} type, equal to {@code "byte"}. */
    public static final String BYTE = byte.class.getSimpleName();

    private static final Byte DEFAULT_VALUE = 0;

    /**
     * Constructs a new PrimitiveByteType instance.
     * This constructor is package-private and intended to be called only by the TypeFactory.
     */
    PrimitiveByteType() {
        super(BYTE);
    }

    /**
     * Returns the Class object representing the primitive byte type.
     * Note that this returns byte.class, not Byte.class.
     *
     * @return the Class object for the primitive byte type
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public Class javaType() {
        return byte.class;
    }

    /**
     * Indicates whether this type represents a primitive type.
     * Always returns {@code true} for PrimitiveByteType.
     *
     * @return {@code true}, as this type handler is for primitive byte
     */
    @Override
    public boolean isPrimitive() {
        return true;
    }

    /**
     * Returns the default value for the primitive byte type.
     * The default value for byte primitives is 0.
     *
     * @return Byte value of 0 as the default value for primitive byte
     */
    @Override
    public Byte defaultValue() {
        return DEFAULT_VALUE;
    }
}
