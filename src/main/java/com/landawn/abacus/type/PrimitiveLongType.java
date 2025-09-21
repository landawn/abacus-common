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
public final class PrimitiveLongType extends AbstractLongType {

    public static final String LONG = long.class.getSimpleName();

    private static final Long DEFAULT_VALUE = 0L;

    PrimitiveLongType() {
        super(LONG);
    }

    /**
     * Returns the Class object representing the primitive long type.
     * 
     * @return the Class object for long.class
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public Class clazz() {
        return long.class;
    }

    /**
     * Indicates whether this type represents a primitive type.
     * For PrimitiveLongType, this always returns {@code true} since it represents the primitive long type.
     *
     * @return true, indicating this is a primitive type
     */
    @Override
    public boolean isPrimitiveType() {
        return true;
    }

    /**
     * Returns the default value for the primitive long type.
     * The default value for primitive long is 0L.
     *
     * @return Long object containing the value 0L
     */
    @Override
    public Long defaultValue() {
        return DEFAULT_VALUE;
    }
}