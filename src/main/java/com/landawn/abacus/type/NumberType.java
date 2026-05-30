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
import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.N;

/**
 * Generic type handler for {@link Number} subclasses, providing common functionality
 * for numeric type handling including serialization, deserialization, and type conversions.
 * <p>
 * This handler is suitable for any {@link Number} subclass (including {@link java.math.BigDecimal}
 * and {@link java.math.BigInteger} as well as user-defined numeric types) that does not have a
 * dedicated {@link Type} implementation. It uses reflection to discover, in order:
 * <ol>
 *   <li>a public static factory method on the type class (such as {@code valueOf(String)}) that
 *       returns an instance of the type;</li>
 *   <li>a public static method taking a single numeric/{@code String} argument that returns the type;</li>
 *   <li>a public single-argument constructor accepting either a {@code String} or a numeric value.</li>
 * </ol>
 * If none of those is found, {@link #valueOf(String)} throws
 * {@link UnsupportedOperationException} when called with a non-empty input.
 *
 * @param <T> the specific {@code Number} subclass this type handler manages
 */
public class NumberType<T extends Number> extends AbstractPrimaryType<T> {

    private final Class<T> typeClass;
    private final Function<String, T> creator;

    /**
     * Constructs a {@link NumberType} with the specified type name and
     * {@link Number}.class as the underlying type class. The discovered factory method
     * or constructor will be looked up on {@link Number} itself, so this constructor is
     * typically only used by subclasses that override the conversion methods.
     *
     * @param typeName the registered name of this number type
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
     * Indicates whether values of this type require quoting in CSV format.
     * Numeric values do not require quotes in CSV.
     *
     * @return {@code false}, as numeric values do not require quoting in CSV format
     */
    @Override
    public boolean isCsvQuoteRequired() {
        return false;
    }

    /**
     * Returns the Java class type that this type handler manages.
     *
     * @return the class object for the specific Number subclass
     */
    @Override
    public Class<T> javaType() {
        return typeClass;
    }

    /**
     * Converts a string representation to an instance of the number type.
     * This method uses the discovered factory method or constructor to create instances.
     *
     * @param str the string to convert, may be {@code null} or empty
     * @return an instance of the number type, or {@code null} if the input string is {@code null} or empty
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
     * @param x the number object to convert, may be {@code null}
     * @return the string representation using {@code toString()}, or {@code null} if the input is {@code null}
     */
    @Override
    public String stringOf(final T x) {
        return (x == null) ? null : x.toString();
    }

    /**
     * Appends the string representation of a number to an {@link Appendable}.
     * Writes {@code "null"} when {@code x} is {@code null}; otherwise appends the result
     * of {@link #stringOf(Number)}.
     *
     * @param appendable the target to write to
     * @param x the number value to append, may be {@code null}
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
     * Writes the character representation of a number to a {@link CharacterWriter} by delegating
     * to {@link #appendTo(Appendable, Object)}.
     *
     * @param writer the {@code CharacterWriter} to write to
     * @param x the number value to write, may be {@code null}
     * @param config the serialization configuration (unused for number values)
     * @throws IOException if an I/O error occurs during the write operation
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, final T x, final JsonXmlSerConfig<?> config) throws IOException {
        appendTo(writer, x);
    }
}
