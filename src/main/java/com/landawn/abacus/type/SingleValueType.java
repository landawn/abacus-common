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
import com.landawn.abacus.annotation.SuppressFBWarnings;
import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.ExceptionUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Tuple;
import com.landawn.abacus.util.Tuple.Tuple3;
import com.landawn.abacus.util.TypeAttrParser;

/**
 * Abstract base class for type handlers that wrap a single value. This class provides
 * serialization and deserialization support for types that contain a single wrapped value,
 * with support for JSON/XML annotations and automatic value extraction/creation.
 *
 * <p>The wrapped value is discovered, in order of precedence:
 * <ol>
 *   <li>Via the framework's {@link com.landawn.abacus.annotation.JsonXmlValue}/
 *       {@link com.landawn.abacus.annotation.JsonXmlCreator} annotations (or the equivalent
 *       Jackson {@code @JsonValue}/{@code @JsonCreator} annotations).</li>
 *   <li>By scanning the class for a single non-static, non-final field whose type is accepted by
 *       a public single-arg constructor or static factory method, plus a matching public getter or
 *       publicly accessible field for value extraction.</li>
 * </ol>
 * If neither pattern is detected and the type is not an enum, the handler falls back to a generic
 * object handling mode in which {@link #stringOf(Object)} delegates to the value's runtime type
 * and {@link #valueOf(String)} returns the input string unchanged (cast to {@code T}).
 *
 * @param <T> the type being handled
 */
abstract class SingleValueType<T> extends AbstractType<T> { //NOSONAR

    final Class<T> typeClass;
    final boolean isGenericType;
    final List<Type<?>> parameterTypes;

    final Field jsonValueField;
    final Method jsonValueMethod;
    final Method jsonCreatorMethod;

    final Type<Object> jsonValueType;
    final boolean isSerializable;

    final Type<Object> valueType;
    final Function<String, T> creator;
    final Function<T, Object> valueExtractor;
    final boolean isObjectType;

    /**
     * Constructs a {@code SingleValueType} using the canonical class name as the type name.
     *
     * @param typeClass the class of the type to handle
     */
    protected SingleValueType(final Class<T> typeClass) {
        this(ClassUtil.getCanonicalClassName(typeClass), typeClass);
    }

    /**
     * Constructs a {@code SingleValueType} with an explicit type name.
     * Inspects the class for {@code @JsonXmlValue}/{@code @JsonXmlCreator} (or Jackson equivalent)
     * annotations and, if absent, attempts to auto-detect a single-field value pattern
     * by scanning constructors, factory methods, and getter methods.
     *
     * @param typeName the type name string (may include generic parameters)
     * @param typeClass the class of the type to handle
     * @throws IllegalArgumentException if only one side of the {@code @JsonXmlValue}/{@code @JsonXmlCreator}
     *                                  pair is present, if multiple annotated members are present for either role,
     *                                  or if an annotated member violates its signature constraints
     */
    @SuppressWarnings("null")
    protected SingleValueType(final String typeName, final Class<T> typeClass) {
        super(typeName);
        this.typeClass = typeClass;

        final TypeAttrParser attrs = TypeAttrParser.parse(typeName);
        isGenericType = typeName.indexOf('<') > 0 && typeName.indexOf('>') > 0; //NOSONAR
        final String[] paramTypeNames = attrs.getTypeParameters();
        final Type<?>[] paramTypeArr = new Type<?>[paramTypeNames.length];

        for (int i = 0, len = paramTypeArr.length; i < len; i++) {
            paramTypeArr[i] = TypeFactory.getType(paramTypeNames[i]);
        }

        parameterTypes = List.of(paramTypeArr);

        Field localJsonValueField = null;
        Method localJsonValueMethod = null;
        Method localJsonCreatorMethod = null;
        Class<?> localJsonValueType = null;

        final Method[] methods = typeClass.getDeclaredMethods();

        for (final Method m : methods) {
            boolean isCreator = m.isAnnotationPresent(JsonXmlCreator.class);
            boolean isValue = m.isAnnotationPresent(JsonXmlValue.class);

            try {
                final com.fasterxml.jackson.annotation.JsonCreator annotation = m.getAnnotation(com.fasterxml.jackson.annotation.JsonCreator.class);
                isCreator |= annotation != null && annotation.mode() != com.fasterxml.jackson.annotation.JsonCreator.Mode.DISABLED;
            } catch (final Throwable e) {
                // Jackson is optional.
            }

            try {
                isValue |= m.isAnnotationPresent(com.fasterxml.jackson.annotation.JsonValue.class)
                        && m.getAnnotation(com.fasterxml.jackson.annotation.JsonValue.class).value();
            } catch (final Throwable e) {
                // Jackson is optional.
            }

            if (isCreator) {
                if (localJsonCreatorMethod != null) {
                    throw new IllegalArgumentException("Multiple JsonCreator methods are defined in class: " + typeClass);
                }

                localJsonCreatorMethod = m;
            }

            if (isValue) {
                if (localJsonValueMethod != null) {
                    throw new IllegalArgumentException("Multiple JsonValue members are defined in class: " + typeClass);
                }

                localJsonValueMethod = m;
            }
        }

        for (final Field field : typeClass.getDeclaredFields()) {
            boolean isValue = field.isAnnotationPresent(JsonXmlValue.class);

            try {
                isValue |= field.isAnnotationPresent(com.fasterxml.jackson.annotation.JsonValue.class)
                        && field.getAnnotation(com.fasterxml.jackson.annotation.JsonValue.class).value();
            } catch (final Throwable e) {
                // Jackson is optional.
            }

            if (isValue) {
                if (localJsonValueField != null || localJsonValueMethod != null) {
                    throw new IllegalArgumentException("Multiple JsonValue members are defined in class: " + typeClass);
                }

                localJsonValueField = field;
            }
        }

        if ((localJsonValueField != null || localJsonValueMethod != null) == (localJsonCreatorMethod == null)) {
            throw new IllegalArgumentException("Json annotations 'JsonValue' and 'JsonCreator' must be declared as a pair in class: " + typeClass);
        }

        if (localJsonValueField != null && Modifier.isStatic(localJsonValueField.getModifiers())) {
            throw new IllegalArgumentException("The 'JsonValue' field must not be static in class: " + typeClass);
        }

        if (localJsonValueMethod != null && (Modifier.isStatic(localJsonValueMethod.getModifiers()) || localJsonValueMethod.getParameterCount() != 0
                || localJsonValueMethod.getReturnType() == void.class)) {
            throw new IllegalArgumentException(
                    "The 'JsonValue' method must be a non-static, no-argument method with a value return type in class: " + typeClass);
        }

        if (localJsonCreatorMethod != null) {
            localJsonValueType = localJsonValueMethod == null ? localJsonValueField.getType() : localJsonValueMethod.getReturnType();

            if (!typeClass.isAssignableFrom(localJsonCreatorMethod.getReturnType())) {
                throw new IllegalArgumentException(
                        "The return type of 'JsonCreator' method " + localJsonCreatorMethod + " is not assignable to target class: " + typeClass.getName());
            }

            if (!Modifier.isStatic(localJsonCreatorMethod.getModifiers())) {
                throw new IllegalArgumentException("The 'JsonCreator' method must be static: " + localJsonCreatorMethod);
            }

            if (N.len(localJsonCreatorMethod.getParameterTypes()) != 1
                    || !ClassUtil.wrap(localJsonCreatorMethod.getParameterTypes()[0]).isAssignableFrom(ClassUtil.wrap(localJsonValueType))) {
                throw new IllegalArgumentException("The 'JsonCreator' method must take exactly one parameter compatible with the 'JsonValue' type "
                        + localJsonValueType.getName() + ": " + localJsonCreatorMethod);
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

    /**
     * Returns the Class object representing the type handled by this type handler.
     *
     * @return the Class object for type T
     */
    @Override
    public Class<T> javaType() {
        return typeClass;
    }

    /**
     * Indicates whether this type is a generic type with type parameters.
     *
     * @return {@code true} if this is a generic type, {@code false} otherwise
     */
    @Override
    public boolean isParameterizedType() {
        return isGenericType;
    }

    /**
     * Returns the immutable list of parameter types for generic types.
     *
     * @return an immutable list of Type objects representing the type parameters, or an empty list if not generic
     */
    @Override
    public List<Type<?>> parameterTypes() {
        return parameterTypes;
    }

    /**
     * Indicates whether this type is treated as a general object type without specific value extraction.
     *
     * @return {@code true} if this is an object type, {@code false} otherwise
     */
    @Override
    public boolean isObject() {
        return isObjectType;
    }

    /**
     * {@inheritDoc}
     * <p>A single-value type is serializable if its underlying JSON value type or value type
     * (with a value extractor) is itself serializable.
     *
     * @return {@code true} if this type is serializable, {@code false} otherwise
     */
    @Override
    public boolean isSerializable() {
        return isSerializable;
    }

    /**
     * Converts an instance of type T to its string representation.
     * Uses JSON value annotations or value extractors if available; otherwise delegates to the
     * value's runtime {@link Type} (falling back to {@link Object#toString()} when that runtime
     * type resolves to a generic {@link ObjectType}).
     *
     * <p>The returned string is a serializable representation designed to be parsed back into an equivalent value
     * via {@link #valueOf(String)}; {@code stringOf} and {@code valueOf} are inverse operations that round-trip. This
     * is the key distinction from {@link Object#toString()}, whose result is not guaranteed to be convertible back
     * into the original value.</p>
     *
     * @param x the object to convert
     * @return the string representation, or {@code null} if {@code x} is {@code null}
     * @see #valueOf(String)
     * @see #valueOf(Object)
     */
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
     * Creates an instance of type T from its string representation.
     * Uses the JSON creator method (paired with the JSON value type) if available, otherwise the
     * auto-detected factory-method/constructor creator.
     *
     * <p>This method is the inverse of {@code stringOf} and round-trips with it: it parses the string produced by
     * {@code stringOf} back into a value of this type. Strings produced by {@link Object#toString()} are not
     * guaranteed to be parseable in this way.</p>
     *
     * @param str the string to parse; may be {@code null}
     * @return an instance of type T, {@code null} when {@code str} is {@code null}, or the string itself
     *         (cast to {@code T}) if no creator is available
     * @see #valueOf(Object)
     * @see #stringOf(Object)
     */
    @Override
    public T valueOf(final String str) {
        // throw new UnsupportedOperationException();

        if (str == null) {
            return null;
        }

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
     * Retrieves a value of type T from the specified column in the ResultSet.
     * Uses JSON creator if available, otherwise attempts string conversion or direct cast.
     *
     * @param rs the ResultSet containing the query results
     * @param columnIndex the index of the column to retrieve (1-based)
     * @return an instance of type T, or {@code null} if the database value is SQL {@code NULL}
     * @throws SQLException if a database access error occurs
     */
    @Override
    public T get(final ResultSet rs, final int columnIndex) throws SQLException {
        if (jsonValueType != null) {
            try {
                final Object value = jsonValueType.get(rs, columnIndex);

                return value == null ? null : (T) jsonCreatorMethod.invoke(null, value);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        } else if (creator != null) {
            final String value = rs.getString(columnIndex);

            return value == null ? null : creator.apply(value);
        } else {
            final Object obj = rs.getObject(columnIndex);

            return obj == null || typeClass.isAssignableFrom(obj.getClass()) ? (T) obj : N.convert(obj, typeClass);
        }
    }

    /**
     * Retrieves a value of type T from the specified column in the ResultSet.
     * Uses JSON creator if available, otherwise attempts string conversion or direct cast.
     *
     * @param rs the ResultSet containing the query results
     * @param columnName the label of the column to retrieve
     * @return an instance of type T, or {@code null} if the database value is SQL {@code NULL}
     * @throws SQLException if a database access error occurs
     */
    @Override
    public T get(final ResultSet rs, final String columnName) throws SQLException {
        if (jsonValueType != null) {
            try {
                final Object value = jsonValueType.get(rs, columnName);

                return value == null ? null : (T) jsonCreatorMethod.invoke(null, value);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        } else if (creator != null) {
            final String value = rs.getString(columnName);

            return value == null ? null : creator.apply(value);
        } else {
            final Object obj = rs.getObject(columnName);

            return obj == null || typeClass.isAssignableFrom(obj.getClass()) ? (T) obj : N.convert(obj, typeClass);
        }
    }

    /**
     * Sets a value of type T at the specified parameter index in the PreparedStatement.
     * Extracts the wrapped value if JSON annotations or value extractors are available.
     *
     * @param stmt the PreparedStatement to set the parameter on
     * @param columnIndex the index of the parameter to set (1-based)
     * @param x the value to set, may be null
     * @throws SQLException if a database access error occurs
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final T x) throws SQLException {
        if (x == null) {
            stmt.setObject(columnIndex, null);
        } else if (jsonValueType != null) {
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
     * Sets a value of type T for the specified parameter name in the CallableStatement.
     * Extracts the wrapped value if JSON annotations or value extractors are available.
     *
     * @param stmt the CallableStatement to set the parameter on
     * @param parameterName the name of the parameter to set
     * @param x the value to set, may be null
     * @throws SQLException if a database access error occurs
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final T x) throws SQLException {
        if (x == null) {
            stmt.setObject(parameterName, null);
        } else if (jsonValueType != null) {
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
     * Sets a value of type T at the specified parameter index in the PreparedStatement with SQL type information.
     * Extracts the wrapped value if JSON annotations or value extractors are available.
     *
     * @param stmt the PreparedStatement to set the parameter on
     * @param columnIndex the index of the parameter to set (1-based)
     * @param x the value to set, may be null
     * @param sqlTypeOrLength the SQL type code or length information
     * @throws SQLException if a database access error occurs
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final T x, final int sqlTypeOrLength) throws SQLException {
        if (x == null) {
            stmt.setObject(columnIndex, null, sqlTypeOrLength);
        } else if (jsonValueType != null) {
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
     * Sets a value of type T for the specified parameter name in the CallableStatement with SQL type information.
     * Extracts the wrapped value if JSON annotations or value extractors are available.
     *
     * @param stmt the CallableStatement to set the parameter on
     * @param parameterName the name of the parameter to set
     * @param x the value to set, may be null
     * @param sqlTypeOrLength the SQL type code or length information
     * @throws SQLException if a database access error occurs
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final T x, final int sqlTypeOrLength) throws SQLException {
        if (x == null) {
            stmt.setObject(parameterName, null, sqlTypeOrLength);
        } else if (jsonValueType != null) {
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
     * Writes the character representation of a value to the given CharacterWriter for JSON/XML serialization.
     * Extracts and writes the wrapped value if JSON annotations or value extractors are available.
     * <p>
     * This method is specifically designed for JSON/XML serialization: it writes the serialized form of {@code x} to the
     * {@code CharacterWriter}, applying string quotation and character escaping according to the supplied serialization
     * config (a {@code null} config means no surrounding quotation). It is the streaming counterpart of {@code stringOf}
     * and is invoked by the JSON/XML serializers.
     * <p>
     * <b>serializeTo vs. appendTo:</b> {@code serializeTo} produces machine-readable JSON/XML (quoted and escaped),
     * whereas {@code appendTo} produces a plain, human-readable {@code toString()}-style rendering without JSON/XML
     * quoting or escaping.
     *
     * @param writer the CharacterWriter to write to
     * @param x the value to write, may be null
     * @param config the serialization configuration for formatting options
     * @throws IOException if an I/O error occurs during writing
     */
    @Override
    public void serializeTo(final CharacterWriter writer, final T x, final JsonXmlSerConfig<?> config) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            if (jsonValueType != null) {
                try {
                    if (jsonValueField != null) {
                        jsonValueType.serializeTo(writer, jsonValueField.get(x), config);
                    } else {
                        jsonValueType.serializeTo(writer, jsonValueMethod.invoke(x), config);
                    }
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new RuntimeException(e);
                }
            } else if (valueType != null && valueExtractor != null) {
                valueType.serializeTo(writer, valueExtractor.apply(x), config);
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

    /**
     * Analyzes a class to extract creator and value extractor functions for single-value types.
     * Searches for factory methods, constructors, and getter methods following common naming patterns.
     *
     * @param <T> the type to analyze
     * @param typeClass the class to analyze for value extraction patterns
     * @return a tuple containing the value type, creator function, and value extractor function,
     *         or a tuple of {@code (null, null, null)} if no suitable pattern is found
     */
    @SuppressFBWarnings("REC_CATCH_EXCEPTION")
    static <T> Tuple3<Type<Object>, Function<String, T>, Function<T, Object>> getCreatorAndValueExtractor(final Class<T> typeClass) {
        final Field[] fields = typeClass.getDeclaredFields();
        final Constructor<?>[] constructors = typeClass.getDeclaredConstructors();
        final Method[] methods = typeClass.getDeclaredMethods();

        List<Field> matchedFields = null;

        try {
            matchedFields = N.filter(fields, f -> !Modifier.isStatic(f.getModifiers()) && !Modifier.isFinal(f.getModifiers())//
                    && (N.anyMatch(constructors, c -> Modifier.isPublic(c.getModifiers()) //
                            && c.getParameterCount() == 1 //
                            && ClassUtil.wrap(c.getParameterTypes()[0]).isAssignableFrom(ClassUtil.wrap(f.getType())))
                            || N.anyMatch(methods, m -> Modifier.isPublic(m.getModifiers()) && Modifier.isStatic(m.getModifiers())//
                                    && m.getParameterCount() == 1 //
                                    && ClassUtil.wrap(m.getParameterTypes()[0]).isAssignableFrom(ClassUtil.wrap(f.getType())))));
        } catch (final Exception e) {
            // ignore
        }

        // if no fields matched or more than one matched, return null.
        if (N.size(matchedFields) != 1) {
            return Tuple.of(null, null, null);
        }

        final Field valueField = matchedFields.get(0);
        final Class<?> valueType = valueField.getType();

        Method factoryMethod = null;

        for (final String methodName : factoryMethodNames) {
            try {
                factoryMethod = typeClass.getMethod(methodName, valueType);

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

        if (factoryMethod == null) {
            try {
                factoryMethod = N.findFirst(methods, it -> Modifier.isPublic(it.getModifiers()) //
                        && Modifier.isStatic(it.getModifiers()) //
                        && typeClass.isAssignableFrom(it.getReturnType()) //
                        && it.getParameterCount() == 1 //
                        && (ClassUtil.wrap(it.getParameterTypes()[0]).isAssignableFrom(ClassUtil.wrap(valueType)))).orElseNull();
            } catch (final Exception e) {
                // ignore
            }
        }

        Constructor<?> constructor = null;

        if (factoryMethod == null) {
            try {
                constructor = typeClass.getConstructor(valueType);
                if (!Modifier.isPublic(constructor.getModifiers())) {
                    constructor = null;
                }
            } catch (final Exception e) {
                // ignore
            }

            if (constructor == null) {
                try {
                    constructor = N.findFirst(constructors, it -> Modifier.isPublic(it.getModifiers()) //
                            && it.getParameterCount() == 1 //
                            && (it.getParameterTypes()[0].isAssignableFrom(valueType)))
                            .or(() -> N.findFirst(constructors, it -> Modifier.isPublic(it.getModifiers()) //
                                    && it.getParameterCount() == 1 //
                                    && (ClassUtil.wrap(it.getParameterTypes()[0]).isAssignableFrom(ClassUtil.wrap(valueType)))))
                            .orElseNull();
                } catch (final Exception e) {
                    // ignore
                }
            }
        }

        // The matchedFields pre-filter accepts a field when ANY public static one-arg method takes the
        // field's type, without checking that method's return type. If neither a real factory method nor
        // a one-arg constructor exists, the object cannot be reconstructed - advertising only a value
        // extractor would break the documented stringOf/valueOf round-trip (the value would serialize as
        // the single field but deserialize as a raw String). Treat such classes as plain object types.
        if (factoryMethod == null && constructor == null) {
            return Tuple.of(null, null, null);
        }

        Method getMethod = null;

        for (final String methodName : getValueMethodNames) {
            try {
                getMethod = typeClass.getMethod(methodName);

                if (Modifier.isPublic(getMethod.getModifiers()) && !Modifier.isStatic(getMethod.getModifiers())
                        && valueType.isAssignableFrom(getMethod.getReturnType())) {
                    break;
                } else {
                    getMethod = null;
                }
            } catch (final Exception e) {
                // ignore
            }
        }

        if (getMethod == null) {
            try {
                getMethod = N.findFirst(methods, it -> Modifier.isPublic(it.getModifiers()) //
                        && !Modifier.isStatic(it.getModifiers()) //
                        && valueType.isAssignableFrom(it.getReturnType()) //
                        && it.getParameterCount() == 0).orElseNull();
            } catch (final Exception e) {
                // ignore
            }
        }

        if (getMethod == null && !Modifier.isPublic(valueField.getModifiers())) {
            return Tuple.of(null, null, null);
        }

        final Method fm = factoryMethod;
        final Constructor<?> cons = constructor;
        final Type<?> parameterType = fm != null ? Type.of(fm.getParameterTypes()[0])
                : (constructor != null ? Type.of(constructor.getParameterTypes()[0]) : null);

        final Function<String, T> creator = fm != null ? str -> (T) ClassUtil.invokeMethod(fm, parameterType == null ? str : parameterType.valueOf(str)) //
                : (cons != null ? str -> (T) ClassUtil.invokeConstructor(cons, parameterType == null ? str : parameterType.valueOf(str)) //
                        : null);

        final Method getter = getMethod;

        final Function<T, Object> valueExtractor = getter != null ? x -> ClassUtil.invokeMethod(x, getter) : x -> {
            try {
                return valueField.get(x);
            } catch (final IllegalAccessException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        };

        return Tuple.of(Type.of(valueType), creator, valueExtractor);
    }
}
