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
import java.util.Set;

import com.landawn.abacus.parser.JSONXMLSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.ImmutableSet;
import com.landawn.abacus.util.WD;

/**
 *
 * @param <E>
 */
@SuppressWarnings("java:S2160")
public class ImmutableSetType<E> extends AbstractType<ImmutableSet<E>> {

    private final String declaringName;

    private final Class<ImmutableSet<E>> typeClass;

    private final Type<E>[] parameterTypes;

    private final Type<E> elementType;

    private final Type<Set<E>> setType;

    @SuppressWarnings("rawtypes")
    ImmutableSetType(final String parameterTypeName) {
        super(getTypeName(ImmutableSet.class, parameterTypeName, false));

        typeClass = (Class) ImmutableSet.class;
        declaringName = getTypeName(ImmutableSet.class, parameterTypeName, true);
        parameterTypes = new Type[] { TypeFactory.getType(parameterTypeName) };
        elementType = parameterTypes[0];
        setType = TypeFactory.getType("Set<" + parameterTypeName + ">");
    }

    @Override
    public String declaringName() {
        return declaringName;
    }

    @Override
    public Class<ImmutableSet<E>> clazz() {
        return typeClass;
    }

    /**
     * Gets the element type.
     *
     * @return
     */
    @Override
    public Type<E> getElementType() {
        return elementType;
    }

    /**
     * Gets the parameter types.
     *
     * @return
     */
    @Override
    public Type<E>[] getParameterTypes() {
        return parameterTypes;
    }

    @Override
    public boolean isSet() {
        return true;
    }

    @Override
    public boolean isCollection() {
        return true;
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
        return setType.isSerializable();
    }

    /**
     * Gets the serialization type.
     *
     * @return
     */
    @Override
    public SerializationType getSerializationType() {
        return isSerializable() ? SerializationType.SERIALIZABLE : SerializationType.COLLECTION;
    }

    /**
     *
     * @param x
     * @return
     */
    @Override
    public String stringOf(final ImmutableSet<E> x) {
        return setType.stringOf(x);
    }

    /**
     *
     * @param str
     * @return
     */
    @Override
    public ImmutableSet<E> valueOf(final String str) {
        return ImmutableSet.wrap(setType.valueOf(str));
    }

    /**
     *
     * @param writer
     * @param x
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public void appendTo(final Appendable writer, final ImmutableSet<E> x) throws IOException {
        setType.appendTo(writer, x);
    }

    /**
     *
     * @param writer
     * @param x
     * @param config
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, final ImmutableSet<E> x, final JSONXMLSerializationConfig<?> config) throws IOException {
        setType.writeCharacter(writer, x, config);
    }

    /**
     * Gets the type name.
     *
     * @param typeClass
     * @param parameterTypeName
     * @param isDeclaringName
     * @return
     */
    protected static String getTypeName(final Class<?> typeClass, final String parameterTypeName, final boolean isDeclaringName) {
        if (isDeclaringName) {
            return ClassUtil.getSimpleClassName(typeClass) + WD.LESS_THAN + TypeFactory.getType(parameterTypeName).declaringName() + WD.GREATER_THAN;
        } else {
            return ClassUtil.getCanonicalClassName(typeClass) + WD.LESS_THAN + TypeFactory.getType(parameterTypeName).name() + WD.GREATER_THAN;
        }
    }
}
