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
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.function.Function;

import com.landawn.abacus.annotation.SuppressFBWarnings;
import com.landawn.abacus.parser.JSONXMLSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.N;

/**
 *
 * @param <T>
 */
public class NumberType<T extends Number> extends AbstractPrimaryType<T> {

    private final Class<T> typeClass;
    private final Function<String, T> creator;

    protected NumberType(final String typeName) {
        this(typeName, Number.class);
    }

    protected NumberType(final Class<?> typeClass) {
        this(ClassUtil.getCanonicalClassName(typeClass), typeClass);
    }

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
                        && (valueType.isAssignableFrom(it.getParameterTypes()[0]))).orElseNull();
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
                            .orElseNull();
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
     * Checks if is number.
     *
     * @return {@code true}, if is number
     */
    @Override
    public boolean isNumber() {
        return true;
    }

    @Override
    public Class<T> clazz() {
        return typeClass;
    }

    @Override
    public T valueOf(final String str) {
        return N.isEmpty(str) ? null : creator.apply(str);
    }

    /**
     *
     * @param x
     * @return
     */
    @Override
    public String stringOf(final T x) {
        return (x == null) ? null : x.toString();
    }

    /**
     *
     * @param appendable
     * @param x
     * @throws IOException Signals that an I/O exception has occurred.
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
     *
     * @param writer
     * @param x
     * @param config
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, final T x, final JSONXMLSerializationConfig<?> config) throws IOException {
        appendTo(writer, x);
    }
}
