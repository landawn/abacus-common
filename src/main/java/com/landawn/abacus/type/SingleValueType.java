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
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.function.Function;

import com.landawn.abacus.annotation.JsonXmlCreator;
import com.landawn.abacus.annotation.JsonXmlValue;
import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.annotation.SuppressFBWarnings;
import com.landawn.abacus.parser.JSONXMLSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Tuple;
import com.landawn.abacus.util.Tuple.Tuple3;
import com.landawn.abacus.util.TypeAttrParser;

/**
 *
 * @param <T>
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

    final Type<Object> valueType;
    final Function<String, T> creator;
    final Function<T, Object> valueExtractor;
    final boolean isObjectType;

    protected SingleValueType(final Class<T> typeClass) {
        this(ClassUtil.getCanonicalClassName(typeClass), typeClass);
    }

    @SuppressWarnings("null")
    protected SingleValueType(final String typeName, final Class<T> typeClass) {
        super(typeName);
        this.typeClass = typeClass;

        final TypeAttrParser attrs = TypeAttrParser.parse(typeName);
        isGenericType = typeName.indexOf('<') > 0 && typeName.indexOf('>') > 0; //NOSONAR
        parameterTypes = new Type<?>[attrs.getTypeParameters().length];

        for (int i = 0, len = parameterTypes.length; i < len; i++) {
            parameterTypes[i] = TypeFactory.getType(attrs.getTypeParameters()[i]);
        }

        Field localJsonValueField = null;
        Method localJsonValueMethod = null;
        Method localJsonCreatorMethod = null;
        Class<?> localJsonValueType = null;

        final Method[] methods = typeClass.getDeclaredMethods();

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
            for (final Field field : typeClass.getDeclaredFields()) {
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
            throw new RuntimeException("Json annotation 'JsonValue' and 'JsonCreator' are not added in pair in class: " + typeClass);
        }

        if (localJsonCreatorMethod != null) {
            localJsonValueType = localJsonValueMethod == null ? localJsonValueField.getType() : localJsonValueMethod.getReturnType();

            if (!typeClass.isAssignableFrom(localJsonCreatorMethod.getReturnType())) {
                throw new RuntimeException(
                        "The result type of 'JsonCreator' method: " + localJsonCreatorMethod + " is not assigned to target class: " + typeClass);
            }

            if (!Modifier.isStatic(localJsonCreatorMethod.getModifiers())) {
                throw new RuntimeException("The 'JsonCreator' method: " + localJsonCreatorMethod + " is not static in class: " + typeClass);
            }

            if (N.len(localJsonCreatorMethod.getParameterTypes()) != 1 && localJsonCreatorMethod.getParameterTypes()[0].isAssignableFrom(localJsonValueType)) {
                throw new RuntimeException("The parameter type of 'JsonCreator' method: " + localJsonCreatorMethod
                        + " is not assigned from the return type of 'JsonValue' in class " + typeClass);
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

        Tuple3<Type<Object>, Function<String, T>, Function<T, Object>> creatorAndValueExtractor = null;

        if (jsonValueType == null && !typeClass.isEnum()) {
            creatorAndValueExtractor = getCreatorAndValueExtractor(typeClass);
        }

        valueType = creatorAndValueExtractor == null ? null : creatorAndValueExtractor._1;
        creator = creatorAndValueExtractor == null ? null : creatorAndValueExtractor._2;
        valueExtractor = creatorAndValueExtractor == null ? null : creatorAndValueExtractor._3;

        isSerializable = jsonValueType != null ? jsonValueType.isSerializable() : (valueType != null && valueExtractor != null && valueType.isSerializable());

        isObjectType = jsonValueType == null && valueType == null && valueExtractor == null && !typeClass.isEnum();
    }

    @Override
    public Class<T> clazz() {
        return typeClass;
    }

    @Override
    public boolean isGenericType() {
        return isGenericType;
    }

    @Override
    public Type<?>[] getParameterTypes() {
        return parameterTypes;
    }

    @Override
    public boolean isObjectType() {
        return isObjectType;
    }

    @Override
    public boolean isSerializable() {
        return isSerializable;
    }

    /**
     *
     * @param x
     * @return
     */
    @MayReturnNull
    @Override
    public String stringOf(final T x) {
        if (x == null) {
            return null; // NOSONAR
        }

        if (jsonValueType != null) {
            try {
                if (jsonValueField != null) {
                    return jsonValueType.stringOf(jsonValueField.get(x));
                } else {
                    return jsonValueType.stringOf(jsonValueMethod.invoke(x));
                }
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        } else if (valueType != null && valueExtractor != null) {
            return valueType.stringOf(valueExtractor.apply(x));
        } else {
            final Type<Object> realType = TypeFactory.getType(x.getClass());

            return realType instanceof ObjectType ? x.toString() : realType.stringOf(x);
        }
    }

    /**
     *
     * @param str
     * @return
     */
    @Override
    public T valueOf(final String str) {
        // throw new UnsupportedOperationException();

        if (jsonValueType != null) {
            try {
                return (T) jsonCreatorMethod.invoke(null, jsonValueType.valueOf(str));
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        } else if (creator != null) {
            return creator.apply(str);
        } else {
            return (T) str;
        }
    }

    /**
     *
     * @param rs
     * @param columnIndex
     * @return
     * @throws SQLException
     */
    @Override
    public T get(final ResultSet rs, final int columnIndex) throws SQLException {
        if (jsonValueType != null) {
            try {
                return (T) jsonCreatorMethod.invoke(null, jsonValueType.get(rs, columnIndex));
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        } else if (creator != null) {
            return creator.apply(rs.getString(columnIndex));
        } else {
            final Object obj = rs.getObject(columnIndex);

            return obj == null || typeClass.isAssignableFrom(obj.getClass()) ? (T) obj : N.convert(obj, typeClass);
        }
    }

    /**
     *
     * @param rs
     * @param columnLabel
     * @return
     * @throws SQLException
     */
    @Override
    public T get(final ResultSet rs, final String columnLabel) throws SQLException {
        if (jsonValueType != null) {
            try {
                return (T) jsonCreatorMethod.invoke(null, jsonValueType.get(rs, columnLabel));
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        } else if (creator != null) {
            return creator.apply(rs.getString(columnLabel));
        } else {
            final Object obj = rs.getObject(columnLabel);

            return obj == null || typeClass.isAssignableFrom(obj.getClass()) ? (T) obj : N.convert(obj, typeClass);
        }
    }

    /**
     *
     * @param stmt
     * @param columnIndex
     * @param x
     * @throws SQLException
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final T x) throws SQLException {
        if (jsonValueType != null) {
            try {
                if (jsonValueField != null) {
                    jsonValueType.set(stmt, columnIndex, jsonValueField.get(x));
                } else {
                    jsonValueType.set(stmt, columnIndex, jsonValueMethod.invoke(x));
                }
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        } else if (valueType != null && valueExtractor != null) {
            valueType.set(stmt, columnIndex, valueExtractor.apply(x));
        } else {
            stmt.setObject(columnIndex, x);
        }
    }

    /**
     *
     * @param stmt
     * @param parameterName
     * @param x
     * @throws SQLException
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final T x) throws SQLException {
        if (jsonValueType != null) {
            try {
                if (jsonValueField != null) {
                    jsonValueType.set(stmt, parameterName, jsonValueField.get(x));
                } else {
                    jsonValueType.set(stmt, parameterName, jsonValueMethod.invoke(x));
                }
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        } else if (valueType != null && valueExtractor != null) {
            valueType.set(stmt, parameterName, valueExtractor.apply(x));
        } else {
            stmt.setObject(parameterName, x);
        }
    }

    /**
     *
     * @param stmt
     * @param columnIndex
     * @param x
     * @param sqlTypeOrLength
     * @throws SQLException
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final T x, final int sqlTypeOrLength) throws SQLException {
        if (jsonValueType != null) {
            try {
                if (jsonValueField != null) {
                    jsonValueType.set(stmt, columnIndex, jsonValueField.get(x), sqlTypeOrLength);
                } else {
                    jsonValueType.set(stmt, columnIndex, jsonValueMethod.invoke(x), sqlTypeOrLength);
                }
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        } else if (valueType != null && valueExtractor != null) {
            valueType.set(stmt, columnIndex, valueExtractor.apply(x), sqlTypeOrLength);
        } else {
            stmt.setObject(columnIndex, x, sqlTypeOrLength);
        }
    }

    /**
     *
     * @param stmt
     * @param parameterName
     * @param x
     * @param sqlTypeOrLength
     * @throws SQLException
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final T x, final int sqlTypeOrLength) throws SQLException {
        if (jsonValueType != null) {
            try {
                if (jsonValueField != null) {
                    jsonValueType.set(stmt, parameterName, jsonValueField.get(x), sqlTypeOrLength);
                } else {
                    jsonValueType.set(stmt, parameterName, jsonValueMethod.invoke(x), sqlTypeOrLength);
                }
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        } else if (valueType != null && valueExtractor != null) {
            valueType.set(stmt, parameterName, valueExtractor.apply(x), sqlTypeOrLength);
        } else {
            stmt.setObject(parameterName, x, sqlTypeOrLength);
        }
    }

    /**
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
            if (jsonValueType != null) {
                try {
                    if (jsonValueField != null) {
                        jsonValueType.writeCharacter(writer, jsonValueField.get(x), config);
                    } else {
                        jsonValueType.writeCharacter(writer, jsonValueMethod.invoke(x), config);
                    }
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new RuntimeException(e);
                }
            } else if (valueType != null && valueExtractor != null) {
                valueType.writeCharacter(writer, valueExtractor.apply(x), config);
            } else {
                final char ch = config == null ? 0 : config.getStringQuotation();

                if (ch == 0) {
                    writer.writeCharacter(stringOf(x));
                } else {
                    writer.write(ch);
                    writer.writeCharacter(stringOf(x));
                    writer.write(ch);
                }
            }
        }
    }

    @SuppressFBWarnings("REC_CATCH_EXCEPTION")
    static <T> Tuple3<Type<Object>, Function<String, T>, Function<T, Object>> getCreatorAndValueExtractor(final Class<T> typeClass) {
        final Field[] fields = typeClass.getDeclaredFields();
        final Constructor<?>[] constructors = typeClass.getDeclaredConstructors();
        final Method[] methods = typeClass.getDeclaredMethods();

        List<Field> matchedFields = null;

        try {
            matchedFields = N.filter(fields, f -> !Modifier.isStatic(f.getModifiers())//
                    && N.anyMatch(constructors, c -> Modifier.isPublic(c.getModifiers()) //
                            && c.getParameterCount() == 1 //
                            && f.getType().isAssignableFrom(c.getParameterTypes()[0])));
        } catch (final Exception e) {
            // ignore
        }

        final Field valueField = N.findFirst(matchedFields, f -> valueFieldNames.contains(f.getName())).orElse(N.firstElement(matchedFields).orElseNull());

        final Class<?> valueType = valueField == null ? null : valueField.getType();

        Method factoryMethod = null;

        for (final String methodName : factoryMethodNames) {
            try {
                factoryMethod = typeClass.getMethod(methodName, String.class);

                if (Modifier.isPublic(factoryMethod.getModifiers()) && Modifier.isStatic(factoryMethod.getModifiers())
                        && typeClass.isAssignableFrom(factoryMethod.getReturnType())) {
                    break;
                } else {
                    factoryMethod = null;
                }
            } catch (final Exception e) {
                // ignore
            }
        }

        if (factoryMethod == null && valueType != null) {
            try {
                factoryMethod = N.findFirst(methods, it -> Modifier.isPublic(it.getModifiers()) //
                        && Modifier.isStatic(it.getModifiers()) //
                        && typeClass.isAssignableFrom(it.getReturnType()) //
                        && it.getParameterCount() == 1 //
                        && (ClassUtil.wrap(valueType).isAssignableFrom(ClassUtil.wrap(it.getParameterTypes()[0])))).orElseNull();
            } catch (final Exception e) {
                // ignore
            }
        }

        Constructor<?> constructor = null;

        if (factoryMethod == null) {
            try {
                constructor = typeClass.getConstructor(String.class);
                if (!Modifier.isPublic(constructor.getModifiers())) {
                    constructor = null;
                }
            } catch (final Exception e) {
                // ignore
            }

            if (constructor == null && valueType != null) {
                try {
                    constructor = N.findFirst(constructors, it -> Modifier.isPublic(it.getModifiers()) //
                            && it.getParameterCount() == 1 //
                            && (valueType.isAssignableFrom(it.getParameterTypes()[0])))
                            .or(() -> N.findFirst(constructors, it -> Modifier.isPublic(it.getModifiers()) //
                                    && it.getParameterCount() == 1 //
                                    && (ClassUtil.wrap(valueType).isAssignableFrom(ClassUtil.wrap(it.getParameterTypes()[0])))))
                            .orElseNull();
                } catch (final Exception e) {
                    // ignore
                }
            }
        }

        Method getMethod = null;

        for (final String methodName : getValueMethodNames) {
            try {
                getMethod = typeClass.getMethod(methodName);

                if (Modifier.isPublic(getMethod.getModifiers()) && !Modifier.isStatic(getMethod.getModifiers())
                        && (valueType == null || valueType.isAssignableFrom(getMethod.getReturnType()))) {
                    break;
                } else {
                    getMethod = null;
                }
            } catch (final Exception e) {
                // ignore
            }
        }

        if (getMethod == null && valueType != null) {
            try {
                getMethod = N.findFirst(methods, it -> Modifier.isPublic(it.getModifiers()) //
                        && !Modifier.isStatic(it.getModifiers()) //
                        && valueType.isAssignableFrom(it.getReturnType()) //
                        && it.getParameterCount() == 0).orElseNull();
            } catch (final Exception e) {
                // ignore
            }
        }

        if (getMethod == null && valueField != null && !Modifier.isPublic(valueField.getModifiers())) {
            ClassUtil.setAccessibleQuietly(valueField, true);
        }

        final Method fm = factoryMethod;
        final Constructor<?> cons = constructor;
        final Type<?> parameterType = fm != null ? Type.of(fm.getParameterTypes()[0])
                : (constructor != null ? Type.of(constructor.getParameterTypes()[0]) : null);

        final Function<String, T> creator = fm != null ? str -> (T) ClassUtil.invokeMethod(fm, parameterType == null ? str : parameterType.valueOf(str)) //
                : (cons != null ? str -> (T) ClassUtil.invokeConstructor(cons, parameterType == null ? str : parameterType.valueOf(str)) //
                        : null);

        final Method getter = getMethod;

        final Function<T, Object> valueExtractor = getter != null ? x -> ClassUtil.invokeMethod(x, getter) : (valueField != null ? x -> {
            try {
                return valueField.get(x);
            } catch (final IllegalAccessException e) {
                throw N.toRuntimeException(e);
            }
        } : null);

        return Tuple.of(valueType == null ? null : Type.of(valueType), creator, valueExtractor);
    }
}
