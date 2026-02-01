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
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.function.Function;

import com.landawn.abacus.annotation.SuppressFBWarnings;
import com.landawn.abacus.parser.JsonXmlSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.N;

/**
 * Abstract base type handler for {@link Number} subclasses, providing common functionality
 * for numeric type handling including serialization, deserialization, and type conversions.
 * This class uses reflection to automatically discover factory methods or constructors
 * for creating instances from strings.
 *
 * @param <T> the specific Number subclass this type handler manages
 */
public class NumberType<T extends Number> extends AbstractPrimaryType<T> {

    private final Class<T> typeClass;
    private final Function<String, T> creator;

    /**
     * Constructs a NumberType with the specified type name.
     * Uses the generic Number class as the type class.
     *
     * @param typeName the name of the number type
     */
    protected NumberType(final String typeName) {
        this(typeName, Number.class);
    }

    /**
     * Constructs a NumberType for the specified type class.
     * The type name is derived from the canonical class name.
     *
     * @param typeClass the class of the number type
     */
    protected NumberType(final Class<?> typeClass) {
        this(ClassUtil.getCanonicalClassName(typeClass), typeClass);
    }

    /**
     * Constructs a NumberType with the specified type name and type class.
     * This constructor automatically discovers factory methods or constructors
     * for creating instances from strings using reflection.
     *
     * @param typeName the name of the number type
     * @param typeClass the class of the number type
     */
    @SuppressFBWarnings({ "REC_CATCH_EXCEPTION", "DE_MIGHT_IGNORE" })
    protected NumberType(final String typeName, final Class<?> typeClass) {
        super(typeName);
        this.typeClass = (Class<T>) typeClass;

        final Field[] fields = typeClass.getDeclaredFields();

        final Class<Number> valueType = N.findFirst(fields, it -> Number.class.isAssignableFrom(ClassUtil.wrap(it.getType())) //
                && !Modifier.isStatic(it.getModifiers()) //
                && valueFieldNames.contains(it.getName()))
                .or(() -> N.findFirst(fields, it -> Number.class.isAssignableFrom(ClassUtil.wrap(it.getType())) //
                        && !Modifier.isStatic(it.getModifiers())))
                .map(it -> (Class<Number>) it.getType())
                .orElse(Number.class);

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

        if (factoryMethod == null) {
            try {
                final Method[] methods = typeClass.getDeclaredMethods();

                factoryMethod = N.findFirst(methods, it -> Modifier.isPublic(it.getModifiers()) //
                        && Modifier.isStatic(it.getModifiers()) //
                        && typeClass.isAssignableFrom(it.getReturnType()) //
                        && it.getParameterCount() == 1 //
                        && (valueType.isAssignableFrom(it.getParameterTypes()[0]))).orElse(null);
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

            if (constructor == null) {
                try {
                    final Constructor<?>[] constructors = typeClass.getDeclaredConstructors();

                    constructor = N.findFirst(constructors, it -> Modifier.isPublic(it.getModifiers()) //
                            && it.getParameterCount() == 1 //
                            && (valueType.isAssignableFrom(it.getParameterTypes()[0])))
                            .or(() -> N.findFirst(constructors, it -> Modifier.isPublic(it.getModifiers()) //
                                    && it.getParameterCount() == 1 //
                                    && (Number.class.isAssignableFrom(ClassUtil.wrap(it.getParameterTypes()[0])))))
                            .orElse(null);
                } catch (final Exception e) {
                    // ignore
                }
            }
        }

        final Method fm = factoryMethod;
        final Constructor<?> cons = constructor;
        final Type<?> parameterType = fm != null ? Type.of(fm.getParameterTypes()[0])
                : (constructor != null ? Type.of(constructor.getParameterTypes()[0]) : null);

        creator = fm != null ? str -> (T) ClassUtil.invokeMethod(fm, parameterType == null ? str : parameterType.valueOf(str)) //
                : (cons != null ? str -> (T) ClassUtil.invokeConstructor(cons, parameterType == null ? str : parameterType.valueOf(str)) //
                        : str -> {
                            throw new UnsupportedOperationException(
                                    "No public static method or constructor found in " + typeClass.getName() + " to create to value by String");
                        });
    }

    /**
     * Indicates whether this type represents a numeric value.
     *
     * @return {@code true}, as this is a number type
     */
    @Override
    public boolean isNumber() {
        return true;
    }

    /**
     * Indicates whether values of this type should be quoted when written to CSV format.
     * Numeric values typically don't require quotes in CSV.
     *
     * @return {@code true}, indicating numeric values don't need quotes in CSV
     */
    @Override
    public boolean isNonQuotableCsvType() {
        return true;
    }

    /**
     * Returns the Java class type that this type handler manages.
     *
     * @return the class object for the specific Number subclass
     */
    @Override
    public Class<T> clazz() {
        return typeClass;
    }

    /**
     * Converts a string representation to an instance of the number type.
     * This method uses the discovered factory method or constructor to create instances.
     *
     * @param str the string to convert
     * @return an instance of the number type, or {@code null} if the input string is empty or null
     * @throws NumberFormatException if the string cannot be parsed as the target number type
     * @throws UnsupportedOperationException if no suitable factory method or constructor was found
     */
    @Override
    public T valueOf(final String str) {
        return N.isEmpty(str) ? null : creator.apply(str);
    }

    /**
     * Converts a number object to its string representation.
     *
     * @param x the number object to convert
     * @return the string representation using toString(), or {@code null} if the input is null
     */
    @Override
    public String stringOf(final T x) {
        return (x == null) ? null : x.toString();
    }

    /**
     * Appends the string representation of a number to an Appendable.
     * <p>
     * If the number is {@code null}, appends the string "null". Otherwise, appends
     * the number's string representation as returned by {@link #stringOf(Number)}.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * StringBuilder sb = new StringBuilder();
     * NumberType<Integer> type = new NumberType<>(Integer.class);
     * type.appendTo(sb, 42);     // Appends "42"
     * type.appendTo(sb, null);   // Appends "null"
     * }</pre>
     *
     * @param appendable the Appendable to write to
     * @param x the number value to append
     * @throws IOException if an I/O error occurs during the append operation
     */
    @Override
    public void appendTo(final Appendable appendable, final T x) throws IOException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else {
            appendable.append(stringOf(x));
        }
    }

    /**
     * Writes the character representation of a number to a CharacterWriter.
     * This method delegates to the appendTo method and is typically used for JSON/XML serialization.
     *
     * @param writer the CharacterWriter to write to
     * @param x the number value to write
     * @param config the serialization configuration
     * @throws IOException if an I/O error occurs during the write operation
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, final T x, final JsonXmlSerializationConfig<?> config) throws IOException {
        appendTo(writer, x);
    }
}