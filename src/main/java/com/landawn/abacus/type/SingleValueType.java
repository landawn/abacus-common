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

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.landawn.abacus.annotation.JsonXmlCreator;
import com.landawn.abacus.annotation.JsonXmlValue;
import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.parser.JSONXMLSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.TypeAttrParser;

/**
 *
 * @author Haiyang Li
 * @param <T>
 * @since 0.8
 */
abstract class SingleValueType<T> extends AbstractType<T> { //NOSONAR

    final Class<T> typeClass;
    final boolean isGenericType;
    final Type<?>[] parameterTypes;

    final Field jsonValueField;
    final Method jsonValueMethod;
    final Method jsonCreatorMethod;

    final Type<Object> jsonValueType;
    final boolean isSerializable;

    protected SingleValueType(final Class<T> cls) {
        this(ClassUtil.getCanonicalClassName(cls), cls);
    }

    @SuppressWarnings("null")
    protected SingleValueType(final String typeName, final Class<T> cls) {
        super(typeName);
        typeClass = cls;

        isGenericType = typeName.indexOf('<') > 0 && typeName.indexOf('>') > 0; //NOSONAR
        final TypeAttrParser attrs = TypeAttrParser.parse(typeName);
        parameterTypes = new Type<?>[attrs.getTypeParameters().length];
        for (int i = 0, len = parameterTypes.length; i < len; i++) {
            parameterTypes[i] = TypeFactory.getType(attrs.getTypeParameters()[i]);
        }

        Field localJsonValueField = null;
        Method localJsonValueMethod = null;
        Method localJsonCreatorMethod = null;
        Class<?> localJsonValueType = null;

        final Method[] methods = cls.getDeclaredMethods();

        for (final Method m : methods) {
            if (m.isAnnotationPresent(JsonXmlCreator.class)) {
                localJsonCreatorMethod = m;
            } else if (m.isAnnotationPresent(JsonXmlValue.class)) {
                localJsonValueMethod = m;
            } else {
                try {
                    if (m.isAnnotationPresent(com.fasterxml.jackson.annotation.JsonCreator.class)) {
                        localJsonCreatorMethod = m;
                    }
                } catch (final Throwable e) {
                    // ignore
                }

                try {
                    if (m.isAnnotationPresent(com.fasterxml.jackson.annotation.JsonValue.class)
                            && m.getAnnotation(com.fasterxml.jackson.annotation.JsonValue.class).value()) {
                        localJsonValueMethod = m;
                    }
                } catch (final Throwable e) {
                    // ignore
                }
            }

            if (localJsonCreatorMethod != null && localJsonValueMethod != null) {
                break;
            }
        }

        if (localJsonValueMethod == null) {
            for (final Field field : cls.getDeclaredFields()) {
                if (field.isAnnotationPresent(JsonXmlValue.class)) {
                    localJsonValueField = field;
                } else {
                    try {
                        if (field.isAnnotationPresent(com.fasterxml.jackson.annotation.JsonValue.class)
                                && field.getAnnotation(com.fasterxml.jackson.annotation.JsonValue.class).value()) {
                            localJsonValueField = field;
                        }
                    } catch (final Throwable e) {
                        // ignore
                    }
                }

                if (localJsonValueField != null) {
                    break;
                }
            }
        }

        if ((localJsonValueField != null || localJsonValueMethod != null) == (localJsonCreatorMethod == null)) {
            throw new RuntimeException("Json annotation 'JsonValue' and 'JsonCreator' are not added in pair in class: " + cls);
        }

        if (localJsonCreatorMethod != null) {
            localJsonValueType = localJsonValueMethod == null ? localJsonValueField.getType() : localJsonValueMethod.getReturnType();

            if (!cls.isAssignableFrom(localJsonCreatorMethod.getReturnType())) {
                throw new RuntimeException("The result type of 'JsonCreator' method: " + localJsonCreatorMethod + " is not assigned to target class: " + cls);
            }

            if (!Modifier.isStatic(localJsonCreatorMethod.getModifiers())) {
                throw new RuntimeException("The 'JsonCreator' method: " + localJsonCreatorMethod + " is not static in class: " + cls);
            }

            if (N.len(localJsonCreatorMethod.getParameterTypes()) != 1 && localJsonCreatorMethod.getParameterTypes()[0].isAssignableFrom(localJsonValueType)) {
                throw new RuntimeException("The parameter type of 'JsonCreator' method: " + localJsonCreatorMethod
                        + " is not assigned from the return type of 'JsonValue' in class " + cls);
            }
        }

        jsonValueField = localJsonValueField;
        jsonValueMethod = localJsonValueMethod;
        jsonCreatorMethod = localJsonCreatorMethod;

        if (jsonValueField != null) {
            ClassUtil.setAccessibleQuietly(jsonValueField, true);
        }

        if (jsonValueMethod != null) {
            ClassUtil.setAccessibleQuietly(jsonValueMethod, true);
        }

        if (jsonCreatorMethod != null) {
            ClassUtil.setAccessibleQuietly(jsonCreatorMethod, true);
        }

        jsonValueType = localJsonValueType != null ? TypeFactory.getType(localJsonValueType) : null;
        isSerializable = jsonValueType == null ? false : jsonValueType.isSerializable();
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

    /**
     *
     *
     * @return
     */
    @Override
    public boolean isGenericType() {
        return isGenericType;
    }

    /**
     *
     *
     * @return
     */
    @Override
    public Type<?>[] getParameterTypes() {
        return parameterTypes;
    }

    /**
     *
     *
     * @return
     */
    @Override
    public boolean isSerializable() {
        return isSerializable;
    }

    /**
     *
     *
     * @param x
     * @return {@code null} if {@code (x == null)}. (auto-generated java doc for return)
     */
    @MayReturnNull
    @Override
    public String stringOf(final T x) {
        if (x == null) {
            return null; // NOSONAR
        }

        if (jsonValueType == null) {
            final Type<Object> realType = TypeFactory.getType(x.getClass());

            return realType instanceof ObjectType ? x.toString() : realType.stringOf(x);
        } else {
            try {
                if (jsonValueField != null) {
                    return jsonValueType.stringOf(jsonValueField.get(x));
                } else {
                    return jsonValueType.stringOf(jsonValueMethod.invoke(x));
                }
            } catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     *
     *
     * @param str
     * @return
     */
    @Override
    public T valueOf(final String str) {
        // throw new UnsupportedOperationException();

        if (jsonValueType == null) {
            return (T) str;
        } else {
            try {
                return (T) jsonCreatorMethod.invoke(null, jsonValueType.valueOf(str));
            } catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     *
     *
     * @param rs
     * @param columnIndex
     * @return
     * @throws SQLException
     */
    @Override
    public T get(final ResultSet rs, final int columnIndex) throws SQLException {
        if (jsonValueType == null) {
            final Object obj = rs.getObject(columnIndex);

            return obj == null || typeClass.isAssignableFrom(obj.getClass()) ? (T) obj : N.convert(obj, typeClass);
        } else {
            try {
                return (T) jsonCreatorMethod.invoke(null, jsonValueType.get(rs, columnIndex));
            } catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     *
     *
     * @param rs
     * @param columnLabel
     * @return
     * @throws SQLException
     */
    @Override
    public T get(final ResultSet rs, final String columnLabel) throws SQLException {
        if (jsonValueType == null) {
            final Object obj = rs.getObject(columnLabel);

            return obj == null || typeClass.isAssignableFrom(obj.getClass()) ? (T) obj : N.convert(obj, typeClass);
        } else {
            try {
                return (T) jsonCreatorMethod.invoke(null, jsonValueType.get(rs, columnLabel));
            } catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     *
     *
     * @param stmt
     * @param columnIndex
     * @param x
     * @throws SQLException
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final T x) throws SQLException {
        if (jsonValueType == null) {
            stmt.setObject(columnIndex, x);
        } else {
            try {
                if (jsonValueField != null) {
                    jsonValueType.set(stmt, columnIndex, jsonValueField.get(x));
                } else {
                    jsonValueType.set(stmt, columnIndex, jsonValueMethod.invoke(x));
                }
            } catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     *
     *
     * @param stmt
     * @param parameterName
     * @param x
     * @throws SQLException
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final T x) throws SQLException {
        if (jsonValueType == null) {
            stmt.setObject(parameterName, x);
        } else {
            try {
                if (jsonValueField != null) {
                    jsonValueType.set(stmt, parameterName, jsonValueField.get(x));
                } else {
                    jsonValueType.set(stmt, parameterName, jsonValueMethod.invoke(x));
                }
            } catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     *
     *
     * @param stmt
     * @param columnIndex
     * @param x
     * @param sqlTypeOrLength
     * @throws SQLException
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final T x, final int sqlTypeOrLength) throws SQLException {
        if (jsonValueType == null) {
            stmt.setObject(columnIndex, x, sqlTypeOrLength);
        } else {
            try {
                if (jsonValueField != null) {
                    jsonValueType.set(stmt, columnIndex, jsonValueField.get(x), sqlTypeOrLength);
                } else {
                    jsonValueType.set(stmt, columnIndex, jsonValueMethod.invoke(x), sqlTypeOrLength);
                }
            } catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     *
     *
     * @param stmt
     * @param parameterName
     * @param x
     * @param sqlTypeOrLength
     * @throws SQLException
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final T x, final int sqlTypeOrLength) throws SQLException {
        if (jsonValueType == null) {
            stmt.setObject(parameterName, x, sqlTypeOrLength);
        } else {
            try {
                if (jsonValueField != null) {
                    jsonValueType.set(stmt, parameterName, jsonValueField.get(x), sqlTypeOrLength);
                } else {
                    jsonValueType.set(stmt, parameterName, jsonValueMethod.invoke(x), sqlTypeOrLength);
                }
            } catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     *
     *
     * @param writer
     * @param x
     * @param config
     * @throws IOException
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, final T x, final JSONXMLSerializationConfig<?> config) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            if (jsonValueType == null) {
                final char ch = config == null ? 0 : config.getStringQuotation();

                if (ch == 0) {
                    writer.writeCharacter(stringOf(x));
                } else {
                    writer.write(ch);
                    writer.writeCharacter(stringOf(x));
                    writer.write(ch);
                }
            } else {
                try {
                    if (jsonValueField != null) {
                        jsonValueType.writeCharacter(writer, jsonValueField.get(x), config);
                    } else {
                        jsonValueType.writeCharacter(writer, jsonValueMethod.invoke(x), config);
                    }
                } catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
