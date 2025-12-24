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

import com.landawn.abacus.parser.JSONXMLSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.FloatList;
import com.landawn.abacus.util.Strings;

/**
 * Type handler for FloatList objects.
 * Provides functionality for serialization and deserialization of FloatList instances
 * by delegating to the underlying float array type handler.
 */
@SuppressWarnings("java:S2160")
public final class PrimitiveFloatListType extends AbstractPrimitiveListType<FloatList> {

    public static final String FLOAT_LIST = FloatList.class.getSimpleName();

    private final Type<float[]> arrayType = Type.of(float[].class);

    private final Type<Float> elementType = Type.of(float.class);
    private final Type<Float>[] parameterTypes = new Type[] { elementType };

    protected PrimitiveFloatListType() {
        super(FLOAT_LIST);
    }

    /**
     * Returns the Class object representing the FloatList type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<FloatList> type = TypeFactory.getType(FloatList.class);
     * Class<FloatList> clazz = type.clazz();
     * // Returns: FloatList.class
     * }</pre>
     *
     * @return the Class object for FloatList
     */
    @Override
    public Class<FloatList> clazz() {
        return FloatList.class;
    }

    /**
     * Returns the Type object for the float element type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<FloatList> type = TypeFactory.getType(FloatList.class);
     * Type<Float> elementType = type.getElementType();
     * // Returns: Type instance for float
     * }</pre>
     *
     * @return the Type object representing float elements
     */
    @Override
    public Type<Float> getElementType() {
        return elementType;
    }

    /**
     * Returns the parameter types associated with this array type.
     *
     * @return an array containing the Float Type that describes the elements of this array type
     * @see #getElementType()
     */
    @Override
    public Type<Float>[] getParameterTypes() {
        return parameterTypes;
    }

    /**
     * Converts a FloatList to its string representation.
     * The list is first converted to a float array, then serialized using the array type handler.
     * Returns {@code null} if the input list is {@code null}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<FloatList> type = TypeFactory.getType(FloatList.class);
     * FloatList list = FloatList.of(1.5f, 2.7f, 3.14f);
     * String result = type.stringOf(list);
     * // Returns: "[1.5, 2.7, 3.14]"
     * }</pre>
     *
     * @param x the FloatList to convert
     * @return the string representation of the list, or {@code null} if input is null
     */
    @Override
    public String stringOf(final FloatList x) {
        return x == null ? null : arrayType.stringOf(x.toArray());
    }

    /**
     * Parses a string representation and creates a FloatList.
     * The string is first parsed as a float array, then wrapped in a FloatList.
     * Returns {@code null} if the input string is {@code null} or empty.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<FloatList> type = TypeFactory.getType(FloatList.class);
     * FloatList result = type.valueOf("[1.5, 2.7, 3.14]");
     * // Returns: FloatList containing {1.5f, 2.7f, 3.14f}
     *
     * FloatList empty = type.valueOf("[]");
     * // Returns: empty FloatList
     * }</pre>
     *
     * @param str the string to parse
     * @return a FloatList created from the parsed values, or {@code null} if input is {@code null} or empty
     */
    @Override
    public FloatList valueOf(final String str) {
        if (Strings.isEmpty(str)) {
            return null;
        }

        final float[] array = arrayType.valueOf(str);
        return array == null ? null : FloatList.of(array);
    }

    /**
     * Appends the string representation of a FloatList to an Appendable.
     * Delegates to the array type handler after converting the list to an array.
     * Appends "null" if the list is {@code null}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<FloatList> type = TypeFactory.getType(FloatList.class);
     * StringBuilder sb = new StringBuilder();
     * FloatList list = FloatList.of(1.5f, 2.7f, 3.14f);
     * type.appendTo(sb, list);
     * // sb now contains: "[1.5, 2.7, 3.14]"
     * }</pre>
     *
     * @param appendable the Appendable to write to
     * @param x the FloatList to append
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void appendTo(final Appendable appendable, final FloatList x) throws IOException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else {
            arrayType.appendTo(appendable, x.toArray());
        }
    }

    /**
     * Writes the character representation of a FloatList to a CharacterWriter.
     * Delegates to the array type handler after converting the list to an array.
     * Writes "null" if the list is {@code null}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<FloatList> type = TypeFactory.getType(FloatList.class);
     * CharacterWriter writer = new CharacterWriter();
     * FloatList list = FloatList.of(1.5f, 2.7f, 3.14f);
     * type.writeCharacter(writer, list, null);
     * // Writer contains: "[1.5, 2.7, 3.14]"
     * }</pre>
     *
     * @param writer the CharacterWriter to write to
     * @param x the FloatList to write
     * @param config the serialization configuration
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, final FloatList x, final JSONXMLSerializationConfig<?> config) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            arrayType.writeCharacter(writer, x.toArray(), config);
        }
    }
}
