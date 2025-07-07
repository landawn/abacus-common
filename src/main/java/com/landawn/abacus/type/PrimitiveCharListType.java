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
import com.landawn.abacus.util.CharList;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Strings;

/**
 * Type handler for CharList objects.
 * Provides functionality for serialization and deserialization of CharList instances
 * by delegating to the underlying char array type handler.
 */
@SuppressWarnings("java:S2160")
public final class PrimitiveCharListType extends AbstractPrimitiveListType<CharList> {

    public static final String CHAR_LIST = CharList.class.getSimpleName();

    private final Type<char[]> arrayType = N.typeOf(char[].class);

    private final Type<Character> elementType = N.typeOf(char.class);
    private final Type<Character>[] parameterTypes = new Type[] { elementType };

    protected PrimitiveCharListType() {
        super(CHAR_LIST);
    }

    /**
     * Returns the Class object representing the CharList type.
     *
     * @return the Class object for CharList
     */
    @Override
    public Class<CharList> clazz() {
        return CharList.class;
    }

    /**
     * Returns the Type object for the char element type.
     *
     * @return the Type object representing char elements
     */
    @Override
    public Type<Character> getElementType() {
        return elementType;
    }

    /**
     * Returns the parameter types associated with this array type.
     *
     * @return an array containing the Character Type that describes the elements of this array type
     * @see #getElementType()
     */
    @Override
    public Type<Character>[] getParameterTypes() {
        return parameterTypes;
    }

    /**
     * Converts a CharList to its string representation.
     * The list is first converted to a char array, then serialized using the array type handler.
     * Returns null if the input list is null.
     *
     * @param x the CharList to convert
     * @return the string representation of the list, or null if input is null
     */
    @Override
    public String stringOf(final CharList x) {
        return x == null ? null : arrayType.stringOf(x.toArray());
    }

    /**
     * Parses a string representation and creates a CharList.
     * The string is first parsed as a char array, then wrapped in a CharList.
     * Returns null if the input string is null or empty.
     *
     * @param str the string to parse
     * @return a CharList created from the parsed values, or null if input is null or empty
     */
    @Override
    public CharList valueOf(final String str) {
        return Strings.isEmpty(str) ? null : CharList.of(arrayType.valueOf(str));
    }

    /**
     * Appends the string representation of a CharList to an Appendable.
     * Delegates to the array type handler after converting the list to an array.
     * Appends "null" if the list is null.
     *
     * @param appendable the Appendable to write to
     * @param x the CharList to append
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void appendTo(final Appendable appendable, final CharList x) throws IOException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else {
            arrayType.appendTo(appendable, x.toArray());
        }
    }

    /**
     * Writes the character representation of a CharList to a CharacterWriter.
     * Delegates to the array type handler after converting the list to an array.
     * Writes "null" if the list is null.
     *
     * @param writer the CharacterWriter to write to
     * @param x the CharList to write
     * @param config the serialization configuration
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, final CharList x, final JSONXMLSerializationConfig<?> config) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            arrayType.writeCharacter(writer, x.toArray(), config);
        }
    }
}