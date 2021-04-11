/*
 * Copyright (C) 2020 HaiYang Li
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

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.landawn.abacus.condition.Condition;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.TypeAttrParser;

/**
 * A special type.
 *
 * @author Haiyang Li
 * @param <T>
 * @since 0.8
 */
public final class ConditionType<T extends Condition> extends AbstractType<T> {
    private final Class<T> typeClass;

    private final boolean isGenericType;

    private final Type<?>[] parameterTypes;

    protected ConditionType(Class<T> cls) {
        this(ClassUtil.getCanonicalClassName(cls), cls);
    }

    protected ConditionType(String typeName, Class<T> cls) {
        super(typeName);

        this.typeClass = cls;
        this.isGenericType = typeName.indexOf('<') > 0 && typeName.indexOf('>') > 0;
        final TypeAttrParser attrs = TypeAttrParser.parse(typeName);
        parameterTypes = new Type<?>[attrs.getTypeParameters().length];
        for (int i = 0, len = parameterTypes.length; i < len; i++) {
            parameterTypes[i] = TypeFactory.getType(attrs.getTypeParameters()[i]);
        }
    }

    @Override
    public Class<T> clazz() {
        return typeClass;
    }

    /**
     * Checks if is generic type.
     *
     * @return true, if is generic type
     */
    @Override
    public boolean isGenericType() {
        return isGenericType;
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
     * Checks if is serializable.
     *
     * @return true, if is serializable
     */
    @Override
    public boolean isSerializable() {
        return true;
    }

    /**
     *
     * @param x
     * @return
     */
    @Override
    public String stringOf(T x) {
        if (x == null) {
            return null;
        }

        final Type<Object> realType = TypeFactory.getType(x.getClass());

        return realType instanceof ConditionType ? x.toString() : realType.stringOf(x);
    }

    /**
     *
     * @param str
     * @return
     */
    @Override
    public T valueOf(String str) {
        throw new UnsupportedOperationException();
    }

    /**
     *
     * @param rs
     * @param columnIndex
     * @return
     * @throws SQLException the SQL exception
     */
    @Override
    public T get(ResultSet rs, int columnIndex) throws SQLException {
        final Object obj = rs.getObject(columnIndex);

        return obj == null || typeClass.isAssignableFrom(obj.getClass()) ? (T) obj : N.convert(obj, typeClass);
    }

    /**
     *
     * @param rs
     * @param columnLabel
     * @return
     * @throws SQLException the SQL exception
     */
    @Override
    public T get(ResultSet rs, String columnLabel) throws SQLException {
        final Object obj = rs.getObject(columnLabel);

        return obj == null || typeClass.isAssignableFrom(obj.getClass()) ? (T) obj : N.convert(obj, typeClass);
    }

    /**
     *
     * @param stmt
     * @param columnIndex
     * @param x
     * @throws SQLException the SQL exception
     */
    @Override
    public void set(PreparedStatement stmt, int columnIndex, T x) throws SQLException {
        stmt.setObject(columnIndex, x);
    }

    /**
     *
     * @param stmt
     * @param parameterName
     * @param x
     * @throws SQLException the SQL exception
     */
    @Override
    public void set(CallableStatement stmt, String parameterName, T x) throws SQLException {
        stmt.setObject(parameterName, x);
    }

    /**
     *
     * @param stmt
     * @param columnIndex
     * @param x
     * @param sqlTypeOrLength
     * @throws SQLException the SQL exception
     */
    @Override
    public void set(PreparedStatement stmt, int columnIndex, T x, int sqlTypeOrLength) throws SQLException {
        stmt.setObject(columnIndex, x, sqlTypeOrLength);
    }

    /**
     *
     * @param stmt
     * @param parameterName
     * @param x
     * @param sqlTypeOrLength
     * @throws SQLException the SQL exception
     */
    @Override
    public void set(CallableStatement stmt, String parameterName, T x, int sqlTypeOrLength) throws SQLException {
        stmt.setObject(parameterName, x, sqlTypeOrLength);
    }
}
