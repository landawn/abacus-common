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
 * Type handler for primitive boolean values.
 * This class handles the primitive boolean type specifically, as opposed to the Boolean wrapper class.
 * It provides type information and default value handling for boolean primitives.
 */
public final class PrimitiveBooleanType extends AbstractBooleanType {

    public static final String BOOLEAN = boolean.class.getSimpleName();

    public static final String BOOL = "bool";

    PrimitiveBooleanType() {
        super(BOOLEAN);
    }

    /**
     * Returns the Class object representing the primitive boolean type.
     * Note that this returns boolean.class, not Boolean.class.
     *
     * @return the Class object for the primitive boolean type
     */
    @Override
    public Class<Boolean> clazz() {
        return boolean.class;
    }

    /**
     * Indicates whether this type represents a primitive type.
     * Always returns {@code true} for PrimitiveBooleanType.
     *
     * @return {@code true}, as this type handler is for primitive boolean
     */
    @Override
    public boolean isPrimitiveType() {
        return true;
    }

    /**
     * Returns the default value for the primitive boolean type.
     * The default value for boolean primitives is {@code false}.
     *
     * @return Boolean.FALSE as the default value for primitive boolean
     */
    @Override
    public Boolean defaultValue() {
        return Boolean.FALSE;
    }
}
