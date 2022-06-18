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

import com.landawn.abacus.util.N;

/**
 *
 * @author Haiyang Li
 * @since 0.8
 */
@SuppressWarnings("rawtypes")
public class TypeType extends AbstractType<Type> {

    public static final String TYPE = "Type";

    TypeType() {
        super(TYPE);
    }

    TypeType(String typeName) {
        super(typeName);
    }

    @Override
    public Class<Type> clazz() {
        return Type.class;
    }

    /**
     * Checks if is immutable.
     *
     * @return true, if is immutable
     */
    @Override
    public boolean isImmutable() {
        return true;
    }

    /**
     *
     * @param x
     * @return
     */
    @Override
    public String stringOf(Type x) {
        return (x == null) ? null : x.name();
    }

    /**
     *
     * @param str
     * @return
     */
    @Override
    public Type valueOf(String str) {
        return (N.isNullOrEmpty(str)) ? null : TypeFactory.getType(str);
    }
}
