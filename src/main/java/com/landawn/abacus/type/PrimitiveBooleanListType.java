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
import com.landawn.abacus.util.BooleanList;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Strings;

/**
 * Type handler for BooleanList objects.
 * Provides functionality for serialization and deserialization of BooleanList instances
 * by delegating to the underlying boolean array type handler.
 */
@SuppressWarnings("java:S2160")
public final class PrimitiveBooleanListType extends AbstractPrimitiveListType<BooleanList> {

    public static final String BOOLEAN_LIST = BooleanList.class.getSimpleName();

    private final Type<boolean[]> arrayType = N.typeOf(boolean[].class);

    private final Type<Boolean> elementType = N.typeOf(boolean.class);
    private final Type<Boolean>[] parameterTypes = new Type[] { elementType };

    protected PrimitiveBooleanListType() {
        super(BOOLEAN_LIST);
    }

    /**
     * Returns the Class object representing the BooleanList type.
     *
     * @return the Class object for BooleanList
     */
    @Override
    public Class<BooleanList> clazz() {
        return BooleanList.class;
    }

    /**
     * Returns the Type object for the boolean element type.
     *
     * @return the Type object representing boolean elements
     */
    @Override
    public Type<?> getElementType() {
        return elementType;
    }

    /**
     * Returns the parameter types associated with this array type.
     *
     * @return an array containing the Boolean Type that describes the elements of this array type
     * @see #getElementType()
     */
    @Override
    public Type<Boolean>[] getParameterTypes() {
        return parameterTypes;
    }

    /**
     * Converts a BooleanList to its string representation.
     * The list is first converted to a boolean array, then serialized using the array type handler.
     * Returns {@code null} if the input list is {@code null}.
     *
     * @param x the BooleanList to convert
     * @return the string representation of the list, or {@code null} if input is null
     @MayReturnNull
     */
    @Override
    public String stringOf(final BooleanList x) {
        return x == null ? null : arrayType.stringOf(x.toArray());
    }

    /**
     * Parses a string representation and creates a BooleanList.
     * The string is first parsed as a boolean array, then wrapped in a BooleanList.
     * Returns {@code null} if the input string is {@code null} or empty.
     *
     * @param str the string to parse
     * @return a BooleanList created from the parsed values, or {@code null} if input is {@code null} or empty
     @MayReturnNull
     */
    @Override
    public BooleanList valueOf(final String str) {
        return Strings.isEmpty(str) ? null : BooleanList.of(arrayType.valueOf(str));
    }

    /**
     * Appends the string representation of a BooleanList to an Appendable.
     * Delegates to the array type handler after converting the list to an array.
     * Appends "null" if the list is {@code null}.
     *
     * @param appendable the Appendable to write to
     * @param x the BooleanList to append
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void appendTo(final Appendable appendable, final BooleanList x) throws IOException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else {
            arrayType.appendTo(appendable, x.toArray());
        }
    }

    /**
     * Writes the character representation of a BooleanList to a CharacterWriter.
     * Delegates to the array type handler after converting the list to an array.
     * Writes "null" if the list is {@code null}.
     *
     * @param writer the CharacterWriter to write to
     * @param x the BooleanList to write
     * @param config the serialization configuration
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, final BooleanList x, final JSONXMLSerializationConfig<?> config) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            arrayType.writeCharacter(writer, x.toArray(), config);
        }
    }
}
