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

@SuppressWarnings("java:S2160")
public final class PrimitiveIntType extends AbstractIntegerType {

    public static final String INT = int.class.getSimpleName();

    private static final Integer DEFAULT_VALUE = 0;

    PrimitiveIntType() {
        super(INT);
    }

    /**
     * Returns the Class object representing the primitive int type.
     * 
     * @return the Class object for int.class
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public Class clazz() {
        return int.class;
    }

    /**
     * Indicates whether this type represents a primitive type.
     * For PrimitiveIntType, this always returns true since it represents the primitive int type.
     *
     * @return true, indicating this is a primitive type
     */
    @Override
    public boolean isPrimitiveType() {
        return true;
    }

    /**
     * Returns the default value for the primitive int type.
     * The default value for primitive int is 0.
     *
     * @return Integer object containing the value 0
     */
    @Override
    public Integer defaultValue() {
        return DEFAULT_VALUE;
    }
}