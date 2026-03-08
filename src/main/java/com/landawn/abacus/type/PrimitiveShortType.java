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
 * Type handler for primitive {@code short} values, providing conversion between short and its string representation.
 */
@SuppressWarnings("java:S2160")
public final class PrimitiveShortType extends AbstractShortType {

    public static final String SHORT = short.class.getSimpleName();

    private static final Short DEFAULT_VALUE = 0;

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
