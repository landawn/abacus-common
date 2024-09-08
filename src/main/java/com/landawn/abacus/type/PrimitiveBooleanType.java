/*
 * Copyright (C) 2015 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.landawn.abacus.type;

/**
 *
 * @author Haiyang Li
 * @since 0.8
 */
public final class PrimitiveBooleanType extends AbstractBooleanType {

    public static final String BOOLEAN = boolean.class.getSimpleName();

    public static final String BOOL = "bool";

    PrimitiveBooleanType() {
        super(BOOLEAN);
    }

    /**
     *
     *
     * @return
     */
    @Override
    public Class<Boolean> clazz() {
        return boolean.class;
    }

    /**
     * Checks if is primitive type.
     *
     * @return true, if is primitive type
     */
    @Override
    public boolean isPrimitiveType() {
        return true;
    }

    /**
     *
     *
     * @return
     */
    @Override
    public Boolean defaultValue() {
        return Boolean.FALSE;
    }
}
