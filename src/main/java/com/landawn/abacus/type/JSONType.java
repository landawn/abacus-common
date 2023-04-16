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

import java.util.List;
import java.util.Map;

import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.WD;

/**
 *
 * @author Haiyang Li
 * @param <T>
 * @since 0.8
 */
@SuppressWarnings("java:S2160")
public class JSONType<T> extends AbstractType<T> {

    public static final String JSON = "JSON";

    private final String declaringName;

    private final Class<T> typeClass;
    //    private final Type<T>[] parameterTypes;
    //    private final Type<T> elementType;

    @SuppressWarnings("unchecked")
    JSONType(String clsName) {
        super(JSON + WD.LESS_THAN + TypeFactory.getType(clsName).name() + WD.GREATER_THAN);

        this.declaringName = JSON + WD.LESS_THAN + TypeFactory.getType(clsName).declaringName() + WD.GREATER_THAN;
        this.typeClass = (Class<T>) ("Map".equalsIgnoreCase(clsName) ? Map.class
                : ("List".equalsIgnoreCase(clsName) ? List.class : ClassUtil.forClass(clsName)));
        //        this.parameterTypes = new Type[] { TypeFactory.getType(clsName) };
        //        this.elementType = parameterTypes[0];
    }

    /**
     * 
     *
     * @return 
     */
    @Override
    public String declaringName() {
        return declaringName;
    }

    /**
     * 
     *
     * @return 
     */
    @Override
    public Class<T> clazz() {
        return typeClass;
    }

    //    @Override
    //    public Type<T> getElementType() {
    //        return elementType;
    //    }
    //
    //    @Override
    //    public Type<T>[] getParameterTypes() {
    //        return parameterTypes;
    //    }
    //
    //    @Override
    //    public boolean isGenericType() {
    //        return true;
    //    }

    /**
     *
     * @param x
     * @return
     */
    @Override
    public String stringOf(T x) {
        return (x == null) ? null : Utils.jsonParser.serialize(x, Utils.jsc);
    }

    /**
     *
     * @param str
     * @return
     */
    @Override
    public T valueOf(String str) {
        return N.isNullOrEmpty(str) ? null : Utils.jsonParser.deserialize(typeClass, str);
    }
}
