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
 * Type handler for primitive double values.
 * This class handles the primitive double type specifically, as opposed to the Double wrapper class.
 * It provides type information and default value handling for double primitives.
 */
@SuppressWarnings("java:S2160")
public final class PrimitiveDoubleType extends AbstractDoubleType {

    public static final String DOUBLE = double.class.getSimpleName();

    private static final Double DEFAULT_VALUE = 0d;

    PrimitiveDoubleType() {
        super(DOUBLE);
    }

    /**
     * Returns the Class object representing the primitive double type.
     * Note that this returns double.class, not Double.class.
     *
     * @return the Class object for the primitive double type
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public Class clazz() {
        return double.class;
    }

    /**
     * Indicates whether this type represents a primitive type.
     * Always returns {@code true} for PrimitiveDoubleType.
     *
     * @return true, as this type handler is for primitive double
     */
    @Override
    public boolean isPrimitiveType() {
        return true;
    }

    /**
     * Returns the default value for the primitive double type.
     * The default value for double primitives is 0.0.
     *
     * @return Double value of 0.0 as the default value for primitive double
     */
    @Override
    public Double defaultValue() {
        return DEFAULT_VALUE;
    }
}