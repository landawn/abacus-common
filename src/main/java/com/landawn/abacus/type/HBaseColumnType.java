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

import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.HBaseColumn;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.WD;

/**
 *
 * @param <T>
 */
@SuppressWarnings("java:S2160")
public class HBaseColumnType<T> extends AbstractType<HBaseColumn<T>> {

    private static final String SEPERATOR = ":";

    private static final char _SEPERATOR = ':';

    public static final String HBASE_COLUMN = "HBaseColumn";

    private final String declaringName;

    private final Class<HBaseColumn<T>> typeClass;

    private final Type<T>[] parameterTypes;

    private final Type<T> elementType;

    HBaseColumnType(final Class<HBaseColumn<T>> typeClass, final String parameterTypeName) {
        super(getTypeName(typeClass, parameterTypeName, false));

        declaringName = getTypeName(typeClass, parameterTypeName, true);

        this.typeClass = typeClass;
        parameterTypes = new Type[] { TypeFactory.getType(parameterTypeName) };
        elementType = parameterTypes[0];
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
    public Class<HBaseColumn<T>> clazz() {
        return typeClass;
    }

    /**
     * Gets the element type.
     *
     * @return
     */
    @Override
    public Type<T> getElementType() {
        return elementType;
    }

    /**
     * Gets the parameter types.
     *
     * @return
     */
    @Override
    public Type<T>[] getParameterTypes() {
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
     *
     * @param x
     * @return
     */
    @Override
    public String stringOf(final HBaseColumn<T> x) {
        return x == null ? null : x.version() + SEPERATOR + elementType.stringOf(x.value());
    }

    /**
     *
     * @param str
     * @return
     */
    @MayReturnNull
    @Override
    public HBaseColumn<T> valueOf(final String str) {
        if (Strings.isEmpty(str)) {
            return null; // NOSONAR
        }

        final int index = str.indexOf(_SEPERATOR);

        final long version = Long.parseLong(str.substring(0, index));
        final T value = elementType.valueOf(str.substring(index + 1));

        return new HBaseColumn<>(value, version);
    }

    /**
     * Gets the type name.
     *
     * @param typeClass
     * @param parameterTypeName
     * @param isDeclaringName
     * @return
     */
    @SuppressWarnings("unused")
    protected static String getTypeName(final Class<?> typeClass, final String parameterTypeName, final boolean isDeclaringName) {
        if (isDeclaringName) {
            return ClassUtil.getSimpleClassName(HBaseColumn.class) + WD.LESS_THAN + TypeFactory.getType(parameterTypeName).declaringName() + WD.GREATER_THAN;
        } else {
            return ClassUtil.getCanonicalClassName(HBaseColumn.class) + WD.LESS_THAN + TypeFactory.getType(parameterTypeName).name() + WD.GREATER_THAN;
        }
    }
}
