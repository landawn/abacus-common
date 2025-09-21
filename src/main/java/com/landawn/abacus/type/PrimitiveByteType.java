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
 * Type handler for primitive byte values.
 * This class handles the primitive byte type specifically, as opposed to the Byte wrapper class.
 * It provides type information and default value handling for byte primitives.
 */
@SuppressWarnings("java:S2160")
public final class PrimitiveByteType extends AbstractByteType {

    public static final String BYTE = byte.class.getSimpleName();

    private static final Byte DEFAULT_VALUE = 0;

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
    public Class clazz() {
        return byte.class;
    }

    /**
     * Indicates whether this type represents a primitive type.
     * Always returns {@code true} for PrimitiveByteType.
     *
     * @return true, as this type handler is for primitive byte
     */
    @Override
    public boolean isPrimitiveType() {
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