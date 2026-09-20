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

import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.annotation.JsonXmlCreator;
import com.landawn.abacus.annotation.JsonXmlValue;
import com.landawn.abacus.annotation.SuppressFBWarnings;
import com.landawn.abacus.parser.JsonSerConfig;
import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.ExceptionUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.Tuple;
import com.landawn.abacus.util.Tuple.Tuple3;
import com.landawn.abacus.util.TypeAttrParser;
import com.landawn.abacus.util.cs;

/**
 * Abstract base class for type handlers that wrap a single value. This class provides
 * serialization and deserialization support for types that contain a single wrapped value,
 * with support for JSON/XML annotations and automatic value extraction/creation.
 *
 * <p>The wrapped value is discovered, in order of precedence:
 * <ol>
 *   <li>Via the framework's {@link com.landawn.abacus.annotation.JsonXmlValue}/
 *       {@link com.landawn.abacus.annotation.JsonXmlCreator} annotations (or the equivalent
 *       Jackson {@code @JsonValue}/{@code @JsonCreator} annotations). The framework annotations must
 *       always be declared as a pair. A Jackson {@code @JsonValue} member without a {@code @JsonCreator}
 *       is accepted on an <b>enum</b> only (Jackson's canonical enum form): the value is written through the
 *       annotated member and read back through the reverse mapping of the constants' values built by
 *       {@link EnumType}. On any other class a lone value member or a lone creator is rejected.</li>
 *   <li>By scanning the class for a single non-static, non-final, non-transient, non-synthetic field whose
 *       type is accepted by a public single-arg constructor or static factory method, plus a public getter
 *       whose name is derived from the field ({@code getXxx}, {@code isXxx}, {@code xxx}, {@code xxxValue})
 *       or one of the conventional accessors ({@code value()}, {@code getValue()}, {@code get()}), or a
 *       publicly accessible field for value extraction.</li>
 * </ol>
 * If neither pattern is detected and the type is not an enum, the handler falls back to a generic
 * object handling mode in which {@link #stringOf(Object)} delegates to the value's runtime type.
 * That fallback has no way to reconstruct the declared type: {@link #valueOf(String)} returns the
 * input string through an unchecked cast, so callers must not assume round-trip conversion.
 * JDK classes whose only mutable fields are transient caches, such as {@link java.util.Locale} and
 * {@link java.net.InetAddress}, are therefore <b>not</b> single-value types: they are handled in object mode
 * (their {@code toString()} form is written and cannot be read back into the declared type).
 *
 * <p>Detected value members retain their declared generic arguments. Class variables are resolved against
 * this handler's parameters; factory variables are inferred from its generic return type and value argument.
 * Unbound variables and wildcards are parsed using their upper bound. A broader creator parameter does not
 * erase the value member's type. Contradictory concrete arguments are rejected, including those inherited through a creator's superclass or interface.</p>
 * <p>Nested JSON values are passed through {@link #valueOf(String)} with their numeric tokens intact;
 * that conversion uses the value handler's parsing defaults, not the enclosing parser's property configuration.</p>
 *
 * @param <T> the type being handled
 */
abstract class SingleValueType<T> extends AbstractType<T> { //NOSONAR

    /** The class handled by this type. */
    final Class<T> typeClass;

    /** {@code true} if the type name carries generic parameters (i.e. contains {@code '<'} and {@code '>'}). */
    final boolean isGenericType;

    /** Immutable list of the type parameters parsed from the type name; empty when the type is not generic. */
    final List<Type<?>> parameterTypes;

    /** The {@code @JsonXmlValue}/{@code @JsonValue} annotated field, or {@code null} if none was found. */
    final Field jsonValueField;

    /** The {@code @JsonXmlValue}/{@code @JsonValue} annotated accessor method, or {@code null} if none was found. */
    final Method jsonValueMethod;

    /**
     * The static {@code @JsonXmlCreator}/{@code @JsonCreator} factory method, or {@code null} if none was found.
     * May be {@code null} while {@link #jsonValueType} is not, only for an enum carrying a lone Jackson
     * {@code @JsonValue}; {@link EnumType} then reads values back through its reverse map.
     */
    final Method jsonCreatorMethod;

    /**
     * The type handler for the annotated JSON value (the field or method return type),
     * or {@code null} when no value member was found.
     */
    final Type<Object> jsonValueType;

    /** Cached result of {@link #isSerializable()}: {@code true} if the wrapped value's type is serializable. */
    final boolean isSerializable;

    /** The type handler for the auto-detected single value field, or {@code null} if none was detected. */
    final Type<Object> valueType;

    /**
     * Auto-detected factory that builds an instance from its string form, or {@code null} when no
     * factory method or single-argument constructor was found.
     */
    final Function<String, T> creator;

    /** Auto-detected accessor that extracts the wrapped value, or {@code null} when none was found. */
    final Function<T, Object> valueExtractor;

    /** Cached result of {@link #isObject()}: {@code true} when neither annotations nor a value pattern were detected. */
    final boolean isObjectType;

    /**
     * Constructs a {@code SingleValueType} using the canonical class name as the type name.
     *
     * @param typeClass the class of the type to handle
     * @throws IllegalArgumentException if {@code typeClass} is {@code null}.
     */
    protected SingleValueType(final Class<T> typeClass) throws IllegalArgumentException {
        this(ClassUtil.getCanonicalClassName(typeClass), typeClass);
    }

    /**
     * Constructs a {@code SingleValueType} with an explicit type name.
     * Inspects the class for {@code @JsonXmlValue}/{@code @JsonXmlCreator} (or Jackson equivalent)
     * annotations and, if absent, attempts to auto-detect a single-field value pattern
     * by scanning constructors, factory methods, and getter methods.
     * Creator compatibility includes inherited generic arguments: a {@code List<BigDecimal>} value can be
     * passed to a {@code Collection<BigDecimal>} creator, but not to a {@code Collection<String>} creator.
     *
     * @param typeName the type name string (may include generic parameters)
     * @param typeClass the class of the type to handle
     * @throws IllegalArgumentException if {@code typeName} or {@code typeClass} is {@code null}, or if only one side of the
     *         {@code @JsonXmlValue} /{@code @JsonXmlCreator} pair is
     *         present (a lone Jackson {@code @JsonValue} is tolerated on an enum only), if multiple annotated members
     *         are present for either role, or if an annotated member violates its signature constraints.
     */
    @SuppressWarnings("null")
    protected SingleValueType(final String typeName, final Class<T> typeClass) throws IllegalArgumentException {
        super(typeName);
        N.checkArgNotNull(typeClass, cs.typeClass);

        this.typeClass = typeClass;

        final TypeAttrParser attrs = TypeAttrParser.parse(typeName);
        isGenericType = typeName.indexOf('<') > 0 && typeName.indexOf('>') > 0; //NOSONAR
        final String[] paramTypeNames = attrs.getTypeParameters();
        final Type<?>[] paramTypeArr = new Type<?>[paramTypeNames.length];

        for (int i = 0, len = paramTypeArr.length; i < len; i++) {
            paramTypeArr[i] = TypeFactory.getType(paramTypeNames[i]);
        }

        parameterTypes = List.of(paramTypeArr);
        final ValueTypeResolver resolver = new ValueTypeResolver(typeClass, parameterTypes);

        Field localJsonValueField = null;
        Method localJsonValueMethod = null;
        Method localJsonCreatorMethod = null;
        Class<?> localJsonValueType = null;
        java.lang.reflect.Type localJsonValueMetadata = null;

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

        final boolean hasValueMember = localJsonValueField != null || localJsonValueMethod != null;

        if (localJsonCreatorMethod != null && !hasValueMember) {
            throw new IllegalArgumentException("Json annotations 'JsonValue' and 'JsonCreator' must be declared as a pair in class: " + typeClass);
        }

        // A creator-less value member is Jackson's canonical enum form (@JsonValue alone; the constants' values are
        // the reverse mapping), so it is tolerated for an enum annotated with the JACKSON annotation only. The
        // framework's @JsonXmlValue documents the pair as mandatory, and a non-enum has no reverse mapping at all.
        if (hasValueMember && localJsonCreatorMethod == null) {
            final boolean isFrameworkValue = localJsonValueField != null ? localJsonValueField.isAnnotationPresent(JsonXmlValue.class)
                    : localJsonValueMethod.isAnnotationPresent(JsonXmlValue.class);

            if (!typeClass.isEnum() || isFrameworkValue) {
                throw new IllegalArgumentException("Json annotations 'JsonValue' and 'JsonCreator' must be declared as a pair in class: " + typeClass);
            }
        }

        if (localJsonValueField != null && Modifier.isStatic(localJsonValueField.getModifiers())) {
            throw new IllegalArgumentException("The 'JsonValue' field must not be static in class: " + typeClass);
        }

        if (localJsonValueMethod != null && (Modifier.isStatic(localJsonValueMethod.getModifiers()) || localJsonValueMethod.getParameterCount() != 0
                || localJsonValueMethod.getReturnType() == void.class)) {
            throw new IllegalArgumentException(
                    "The 'JsonValue' method must be a non-static, no-argument method with a value return type in class: " + typeClass);
        }

        if (hasValueMember) {
            localJsonValueType = localJsonValueMethod == null ? localJsonValueField.getType() : localJsonValueMethod.getReturnType();
            localJsonValueMetadata = localJsonValueMethod == null ? localJsonValueField.getGenericType() : localJsonValueMethod.getGenericReturnType();
        }

        if (localJsonCreatorMethod != null) {
            if (!typeClass.isAssignableFrom(localJsonCreatorMethod.getReturnType())) {
                throw new IllegalArgumentException(
                        "The return type of 'JsonCreator' method " + localJsonCreatorMethod + " is not assignable to target class: " + typeClass.getName());
            }

            if (!Modifier.isStatic(localJsonCreatorMethod.getModifiers())) {
                throw new IllegalArgumentException("The 'JsonCreator' method must be static: " + localJsonCreatorMethod);
            }

            if (N.len(localJsonCreatorMethod.getParameterTypes()) != 1) {
                throw new IllegalArgumentException("The 'JsonCreator' method must take exactly one parameter compatible with the 'JsonValue' type "
                        + localJsonValueType.getName() + ": " + localJsonCreatorMethod);
            }
            resolver.checkCreator(localJsonValueMetadata, localJsonCreatorMethod.getGenericParameterTypes()[0], localJsonCreatorMethod);
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

        jsonValueType = localJsonValueMetadata != null ? resolver.valueType(localJsonValueMetadata) : null;

        Tuple3<Type<Object>, Function<String, T>, Function<T, Object>> creatorAndValueExtractor = null;

        if (jsonValueType == null && !typeClass.isEnum()) {
            creatorAndValueExtractor = getCreatorAndValueExtractor(typeClass, resolver);
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
     * <p>When an annotated or auto-detected creator/extractor pair is available, this representation is designed
     * to round-trip through {@link #valueOf(String)}. Generic object-mode values have no creator and therefore do
     * not have that guarantee.</p>
     *
     * @param x the object to convert
     * @return the string representation, or {@code null} if {@code x} is {@code null}
     * @see #valueOf(String)
     * @see #valueOf(Object)
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
                // Unwrap the reflective wrapper so the creator's/accessor's own exception (e.g. IAE) reaches the caller.
                throw ExceptionUtil.toRuntimeException(e, true);
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
     * <p>This method is the inverse of {@code stringOf} when an annotated or auto-detected creator/extractor pair
     * is available. In generic object mode no creator exists, so the input string is returned through an unchecked
     * cast and is not a reconstructed instance of the declared type.</p>
     *
     * @param str the string to parse; may be {@code null}
     * @return an instance of type T, {@code null} when {@code str} is {@code null}, or the string itself
     *         (cast to {@code T}) if no creator is available
     * @throws RuntimeException whatever the annotated creator throws, propagated unwrapped (an
     *         {@code IllegalArgumentException} thrown by the creator surfaces as that exception)
     * @throws UnsupportedOperationException if a value member is annotated but no creator exists (only an
     *         enum can be in that state, and {@link EnumType} overrides this method)
     * @see #valueOf(Object)
     * @see #stringOf(Object)
     */
    @MayReturnNull
    @Override
    public T valueOf(final String str) throws RuntimeException, UnsupportedOperationException {
        // throw new UnsupportedOperationException();

        if (str == null) {
            return null;
        }

        if (jsonValueType != null) {
            checkCreatorAvailable();

            try {
                return (T) jsonCreatorMethod.invoke(null, jsonValueType.valueOf(str));
            } catch (IllegalAccessException | InvocationTargetException e) {
                // Unwrap the reflective wrapper so the creator's/accessor's own exception (e.g. IAE) reaches the caller.
                throw ExceptionUtil.toRuntimeException(e, true);
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
     * @throws UnsupportedOperationException if a value member is annotated but no creator exists (only an
     *         enum can be in that state, and {@link EnumType} overrides this method)
     * @throws NullPointerException if {@code rs} is null when this method or the selected value type accesses the JDBC resource
     * @throws SQLException if a database access error occurs
     */
    @Override
    public T get(final ResultSet rs, final int columnIndex) throws UnsupportedOperationException, NullPointerException, SQLException {
        if (jsonValueType != null) {
            checkCreatorAvailable();

            try {
                final Object value = jsonValueType.get(rs, columnIndex);

                return value == null || rs.wasNull() ? null : (T) jsonCreatorMethod.invoke(null, value);
            } catch (IllegalAccessException | InvocationTargetException e) {
                // Unwrap the reflective wrapper so the creator's/accessor's own exception (e.g. IAE) reaches the caller.
                throw ExceptionUtil.toRuntimeException(e, true);
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
     * @throws UnsupportedOperationException if a value member is annotated but no creator exists (only an
     *         enum can be in that state, and {@link EnumType} overrides this method)
     * @throws NullPointerException if {@code rs} is null when this method or the selected value type accesses the JDBC resource
     * @throws SQLException if a database access error occurs
     */
    @Override
    public T get(final ResultSet rs, final String columnName) throws UnsupportedOperationException, NullPointerException, SQLException {
        if (jsonValueType != null) {
            checkCreatorAvailable();

            try {
                final Object value = jsonValueType.get(rs, columnName);

                return value == null || rs.wasNull() ? null : (T) jsonCreatorMethod.invoke(null, value);
            } catch (IllegalAccessException | InvocationTargetException e) {
                // Unwrap the reflective wrapper so the creator's/accessor's own exception (e.g. IAE) reaches the caller.
                throw ExceptionUtil.toRuntimeException(e, true);
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
     * @throws NullPointerException if {@code stmt} is null when this method or the selected value type accesses the JDBC resource
     * @throws SQLException if a database access error occurs
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final T x) throws NullPointerException, SQLException {
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
                // Unwrap the reflective wrapper so the creator's/accessor's own exception (e.g. IAE) reaches the caller.
                throw ExceptionUtil.toRuntimeException(e, true);
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
     * @throws NullPointerException if {@code stmt} is null when this method or the selected value type accesses the JDBC resource
     * @throws SQLException if a database access error occurs
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final T x) throws NullPointerException, SQLException {
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
                // Unwrap the reflective wrapper so the creator's/accessor's own exception (e.g. IAE) reaches the caller.
                throw ExceptionUtil.toRuntimeException(e, true);
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
     * @throws NullPointerException if {@code stmt} is null when this method or the selected value type accesses the JDBC resource
     * @throws SQLException if a database access error occurs
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final T x, final int sqlTypeOrLength) throws NullPointerException, SQLException {
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
                // Unwrap the reflective wrapper so the creator's/accessor's own exception (e.g. IAE) reaches the caller.
                throw ExceptionUtil.toRuntimeException(e, true);
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
     * @throws NullPointerException if {@code stmt} is null when this method or the selected value type accesses the JDBC resource
     * @throws SQLException if a database access error occurs
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final T x, final int sqlTypeOrLength) throws NullPointerException, SQLException {
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
                // Unwrap the reflective wrapper so the creator's/accessor's own exception (e.g. IAE) reaches the caller.
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        } else if (valueType != null && valueExtractor != null) {
            valueType.set(stmt, parameterName, valueExtractor.apply(x), sqlTypeOrLength);
        } else {
            stmt.setObject(parameterName, x, sqlTypeOrLength);
        }
    }

    /**
     * Writes the character representation of a value to the given CharacterWriter for JSON/XML serialization.
     * Extracts and writes the wrapped value if JSON annotations or value extractors are available. In generic
     * object mode the value is dispatched on its runtime {@link Type}, exactly as {@link #stringOf(Object)} does:
     * a serializable runtime type (numbers, booleans, dates, primitive arrays, ...) writes its own JSON/XML form
     * honouring {@code config} (so {@code 5} is written as {@code 5}, not {@code "5"}); a structured runtime type
     * (map, bean, collection, {@code Object[]}) is written as structural JSON when {@code config} is a
     * {@link com.landawn.abacus.parser.JsonSerConfig} and as its escaped {@code stringOf} text otherwise; a value
     * whose runtime type is a plain {@link ObjectType} is written as its quoted, escaped {@code toString()}, and a
     * handler with no serialization category at all (the JDBC locators) writes through its own {@code serializeTo}.
     * A {@code null} value writes the literal {@code null}.
     * <p>
     * This method is specifically designed for JSON/XML serialization: it writes the serialized form of {@code x} to the
     * {@code CharacterWriter}, applying string quotation and character escaping according to the supplied serialization
     * config (a {@code null} config means no surrounding quotation). It is the streaming counterpart of {@code stringOf}
     * and is invoked by the JSON/XML serializers.
     * <p>
     * <b>serializeTo vs. appendTo:</b> {@code serializeTo} produces machine-readable JSON/XML (quoted and escaped),
     * whereas {@code appendTo} produces a plain, human-readable {@code toString()}-style rendering without JSON/XML
     * quoting or escaping.
     * <p>
     * In object mode, a value whose runtime handler has no serializable form (a map, bean or collection) is written
     * as embedded JSON. That embedded write is always compact: {@code prettyFormat} is deliberately not propagated
     * to it, because this handler is not told the caller's current indentation and a pretty embedded structure
     * would restart at the left margin.
     *
     * @param writer the CharacterWriter to write to
     * @param x the value to write, may be null
     * @param config the serialization configuration for formatting options
     * @throws NullPointerException if {@code writer} is {@code null}.
     * @throws IOException if writing the null literal or the selected wrapped/runtime value representation to {@code writer} fails
     */
    @Override
    public void serializeTo(final CharacterWriter writer, final T x, final JsonXmlSerConfig<?> config) throws NullPointerException, IOException {
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
                    // Unwrap the reflective wrapper so the accessor's own exception reaches the caller.
                    throw ExceptionUtil.toRuntimeException(e, true);
                }
            } else if (valueType != null && valueExtractor != null) {
                valueType.serializeTo(writer, valueExtractor.apply(x), config);
            } else {
                // Object mode: dispatch on the runtime type like stringOf() does, so an Integer/Date/int[] held in an
                // Object slot keeps its JSON shape and config. `instanceof ObjectType` (not isObject()) is the
                // recursion guard: an object-mode runtime ObjectType would otherwise re-enter this branch forever.
                final Type<Object> realType = TypeFactory.getType(x.getClass());

                if (realType instanceof ObjectType) {
                    final String str = x.toString();

                    if (str == null) {
                        writer.write(NULL_CHAR_ARRAY);
                        return;
                    }

                    final char ch = config == null ? 0 : config.getStringQuotation();

                    if (ch == 0) {
                        Utils.writeStringContent(writer, str, ch);
                    } else {
                        writer.write(ch);
                        Utils.writeStringContent(writer, str, ch);
                        writer.write(ch);
                    }
                } else if (realType.isSerializable() || realType.serializationType() == SerializationType.UNKNOWN) {
                    // Same rule as AbstractTupleType.serializeSlot: a handler with no serialization category (the
                    // JDBC locators - Blob/Ref/RowId/SQLXML/Array) has no JSON shape either, so it keeps its own
                    // form (and its own descriptive exception) instead of the parser's "Unsupported class".
                    realType.serializeTo(writer, x, config);
                } else if (config instanceof JsonSerConfig jsc) {
                    // Map/bean/collection: only the JSON parser can write the structural form. Pretty format is
                    // deliberately not propagated to that embedded write: this handler is not told the caller's
                    // current indentation, so a pretty embedded structure would restart at the left margin and
                    // mis-align every one of its lines. Same rule as AbstractTupleType.serializeSlot,
                    // CollectionType.serializeTo and ObjectArrayType.serializeTo.
                    Utils.jsonParser.serialize(x, jsc.isPrettyFormat() ? jsc.copy().setPrettyFormat(false) : jsc, writer);
                } else {
                    writer.writeCharacter(realType.stringOf(x));
                }
            }
        }
    }

    /**
     * Guards the creator-dependent read paths: a value member without a creator is only legal on an enum,
     * and {@link EnumType} serves those reads from its reverse map without reaching this class.
     * @throws UnsupportedOperationException if the wrapped class has no supported JsonCreator method or constructor for reading values
     */
    private void checkCreatorAvailable() throws UnsupportedOperationException {
        if (jsonCreatorMethod == null) {
            throw new UnsupportedOperationException(
                    "No 'JsonCreator' method is declared in class " + typeClass.getName() + "; values can be written but not read back");
        }
    }

    /**
     * Analyzes a class to extract creator and value extractor functions for single-value types.
     * Searches for factory methods, constructors, and getter methods following common naming patterns.
     * Only non-static, non-final, non-transient, non-synthetic fields are candidates (a transient field is a
     * cache, not the wrapped value; a synthetic field is the compiler's, e.g. an inner class's {@code this$0}).
     * The conventional value getters ({@code value()}, {@code getValue()}, {@code get()}) take precedence; otherwise
     * only a public no-arg method whose name is derived from the field ({@code getXxx}, {@code isXxx}, {@code xxx},
     * {@code xxxValue}) is accepted as the extractor, and primitive field types accept their boxed getter return types.
     *
     * @param <T> the type to analyze
     * @param typeClass the class to analyze for value extraction patterns
     * @return a tuple containing the value type, creator function, and value extractor function,
     *         or a tuple of {@code (null, null, null)} if no suitable pattern is found
     */
    @SuppressFBWarnings("REC_CATCH_EXCEPTION")
    static <T> Tuple3<Type<Object>, Function<String, T>, Function<T, Object>> getCreatorAndValueExtractor(final Class<T> typeClass) {
        return getCreatorAndValueExtractor(typeClass, new ValueTypeResolver(typeClass, List.of()));
    }

    @SuppressFBWarnings("REC_CATCH_EXCEPTION")
    private static <T> Tuple3<Type<Object>, Function<String, T>, Function<T, Object>> getCreatorAndValueExtractor(final Class<T> typeClass,
            final ValueTypeResolver resolver) {
        final Field[] fields = typeClass.getDeclaredFields();
        final Constructor<?>[] constructors = typeClass.getDeclaredConstructors();
        final Method[] methods = typeClass.getDeclaredMethods();

        List<Field> matchedFields = null;

        try {
            // transient = a cache (java.util.Locale's languageTag), synthetic = compiler-owned (this$0): neither is the value.
            matchedFields = N.filter(fields, f -> !Modifier.isStatic(f.getModifiers()) && !Modifier.isFinal(f.getModifiers())//
                    && !Modifier.isTransient(f.getModifiers()) && !f.isSynthetic() //
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
                        && ClassUtil.wrap(valueType).isAssignableFrom(ClassUtil.wrap(getMethod.getReturnType()))) {
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
                // Only a getter named after the field is the value accessor. "Any public no-arg method with an
                // assignable return type" picked java.util.Locale.getLanguage() (dropping the country) and depends
                // on getDeclaredMethods() order. The Object contract methods can never match a derived name.
                final String fieldName = valueField.getName();
                final String capitalized = Strings.capitalize(fieldName);
                final List<String> derivedNames = List.of("get" + capitalized, "is" + capitalized, fieldName, fieldName + "Value");

                getMethod = N.findFirst(methods, it -> Modifier.isPublic(it.getModifiers()) //
                        && !Modifier.isStatic(it.getModifiers()) //
                        && derivedNames.contains(it.getName()) //
                        && ClassUtil.wrap(valueType).isAssignableFrom(ClassUtil.wrap(it.getReturnType())) //
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
        resolver.checkCreator(valueField.getGenericType(), fm != null ? fm.getGenericParameterTypes()[0] : cons.getGenericParameterTypes()[0], fm);
        // Parse the declared value before calling a potentially broader Object/Collection creator.
        final Type<Object> parameterType = resolver.valueType(valueField.getGenericType());

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

        return Tuple.of(parameterType, creator, valueExtractor);
    }
}
