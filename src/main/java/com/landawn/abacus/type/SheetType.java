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

import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.Sheet;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.WD;

// TODO: Auto-generated Javadoc
/**
 *
 * @param <R>
 * @param <C>
 * @param <E>
 */
@SuppressWarnings("java:S2160")
public class SheetType<R, C, E> extends AbstractType<Sheet<R, C, E>> {
    private final String declaringName;

    private final Class<Sheet<R, C, E>> typeClass;

    private final Type<?>[] parameterTypes;

    /**
     *
     *
     * @param rowKeyTypeName
     * @param columnKeyTypeName
     * @param elementTypeName
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public SheetType(final String rowKeyTypeName, final String columnKeyTypeName, final String elementTypeName) {
        super(getTypeName(Sheet.class, rowKeyTypeName, columnKeyTypeName, elementTypeName, false));

        declaringName = getTypeName(Sheet.class, rowKeyTypeName, columnKeyTypeName, elementTypeName, true);

        typeClass = (Class) Sheet.class;
        parameterTypes = new Type[] { TypeFactory.getType(rowKeyTypeName), TypeFactory.getType(columnKeyTypeName), TypeFactory.getType(elementTypeName) };
    }

    @Override
    public String declaringName() {
        return declaringName;
    }

    @Override
    public Class<Sheet<R, C, E>> clazz() {
        return typeClass;
    }

    /**
     * Gets the parameter types.
     *
     * @return
     */
    @Override
    public Type<?>[] getParameterTypes() {
        return parameterTypes;
    }

    /**
     * Checks if is generic type.
     *
     * @return {@code true}, if is generic type
     */
    @Override
    public boolean isGenericType() {
        return true;
    }

    /**
     * Checks if is serializable.
     *
     * @return {@code true}, if is serializable
     */
    @Override
    public boolean isSerializable() {
        return false;
    }

    /**
     * Gets the serialization type.
     *
     * @return
     */
    @Override
    public SerializationType getSerializationType() {
        return SerializationType.SHEET;
    }

    /**
     *
     * @param x
     * @return
     */
    @SuppressWarnings("rawtypes")
    @Override
    public String stringOf(final Sheet x) {
        return (x == null) ? null : Utils.jsonParser.serialize(x, Utils.jsc);
    }

    /**
     *
     * @param str
     * @return
     */
    @SuppressWarnings("rawtypes")
    @Override
    public Sheet valueOf(final String str) {
        return (Strings.isEmpty(str)) ? null : (Sheet) Utils.jsonParser.deserialize(str, typeClass);
    }

    /**
     * Gets the type name.
     *
     * @param typeClass
     * @param rowKeyTypeName
     * @param columnKeyTypeName
     * @param elementTypeName
     * @param isDeclaringName
     * @return
     */
    protected static String getTypeName(final Class<?> typeClass, final String rowKeyTypeName, final String columnKeyTypeName, final String elementTypeName,
            final boolean isDeclaringName) {
        if (isDeclaringName) {
            return ClassUtil.getSimpleClassName(typeClass) + WD.LESS_THAN + TypeFactory.getType(rowKeyTypeName).declaringName() + WD.COMMA_SPACE
                    + TypeFactory.getType(columnKeyTypeName).declaringName() + WD.COMMA_SPACE + TypeFactory.getType(elementTypeName).declaringName()
                    + WD.GREATER_THAN;
        } else {
            return ClassUtil.getCanonicalClassName(typeClass) + WD.LESS_THAN + TypeFactory.getType(rowKeyTypeName).name() + WD.COMMA_SPACE
                    + TypeFactory.getType(columnKeyTypeName).name() + WD.COMMA_SPACE + TypeFactory.getType(elementTypeName).name() + WD.GREATER_THAN;

        }
    }
}
