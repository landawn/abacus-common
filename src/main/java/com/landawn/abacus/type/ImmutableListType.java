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
import java.io.Writer;
import java.util.List;

import com.landawn.abacus.parser.SerializationConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.ImmutableList;
import com.landawn.abacus.util.WD;

/**
 *
 * @author Haiyang Li
 * @param <E>
 * @param <T>
 * @since 0.8
 */
@SuppressWarnings("java:S2160")
public class ImmutableListType<E> extends AbstractType<ImmutableList<E>> {

    private final String declaringName;

    private final Class<ImmutableList<E>> typeClass;

    private final Type<E>[] parameterTypes;

    private final Type<E> elementType;

    private final Type<List<E>> listType;

    @SuppressWarnings("rawtypes")
    ImmutableListType(String parameterTypeName) {
        super(getTypeName(ImmutableList.class, parameterTypeName, false));

        typeClass = (Class) ImmutableList.class;
        this.declaringName = getTypeName(ImmutableList.class, parameterTypeName, true);
        this.parameterTypes = new Type[] { TypeFactory.getType(parameterTypeName) };
        this.elementType = parameterTypes[0];
        this.listType = TypeFactory.getType("List<" + parameterTypeName + ">");
    }

    @Override
    public String declaringName() {
        return declaringName;
    }

    @Override
    public Class<ImmutableList<E>> clazz() {
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

    /**
     * Checks if is list.
     *
     * @return true, if is list
     */
    @Override
    public boolean isList() {
        return true;
    }

    /**
     * Checks if is sets the.
     *
     * @return true, if is sets the
     */
    @Override
    public boolean isSet() {
        return false;
    }

    /**
     * Checks if is collection.
     *
     * @return true, if is collection
     */
    @Override
    public boolean isCollection() {
        return true;
    }

    /**
     * Checks if is generic type.
     *
     * @return true, if is generic type
     */
    @Override
    public boolean isGenericType() {
        return true;
    }

    /**
     * Checks if is serializable.
     *
     * @return true, if is serializable
     */
    @Override
    public boolean isSerializable() {
        return listType.isSerializable();
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
    public String stringOf(ImmutableList<E> x) {
        return listType.stringOf(x);
    }

    /**
     *
     * @param str
     * @return
     */
    @SuppressWarnings("deprecation")
    @Override
    public ImmutableList<E> valueOf(String str) {
        return ImmutableList.wrap(listType.valueOf(str));
    }

    /**
     *
     * @param writer
     * @param x
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public void write(Writer writer, ImmutableList<E> x) throws IOException {
        listType.write(writer, x);
    }

    /**
     *
     * @param writer
     * @param x
     * @param config
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public void writeCharacter(CharacterWriter writer, ImmutableList<E> x, SerializationConfig<?> config) throws IOException {
        listType.writeCharacter(writer, x, config);
    }

    /**
     * Gets the type name.
     *
     * @param typeClass
     * @param parameterTypeName
     * @param isDeclaringName
     * @return
     */
    protected static String getTypeName(Class<?> typeClass, String parameterTypeName, boolean isDeclaringName) {
        if (isDeclaringName) {
            return ClassUtil.getSimpleClassName(typeClass) + WD.LESS_THAN + TypeFactory.getType(parameterTypeName).declaringName() + WD.GREATER_THAN;
        } else {
            return ClassUtil.getCanonicalClassName(typeClass) + WD.LESS_THAN + TypeFactory.getType(parameterTypeName).name() + WD.GREATER_THAN;
        }
    }
}
