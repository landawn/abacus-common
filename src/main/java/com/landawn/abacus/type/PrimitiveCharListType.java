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
import java.util.List;

import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.util.CharList;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.Strings;

/**
 * Type handler for {@link CharList} objects, which is a primitive-backed list of {@code char} values.
 * Provides serialization and deserialization of {@link CharList} instances by delegating to the
 * underlying {@code char[]} array type handler.
 * String representations use the format {@code ['a', 'b', 'c']} with single-quoted, comma-separated
 * characters enclosed in square brackets.
 */
@SuppressWarnings("java:S2160")
public final class PrimitiveCharListType extends AbstractPrimitiveListType<CharList> {

    /** The type name constant for {@link CharList} type identification, equal to {@code "CharList"}. */
    public static final String CHAR_LIST = CharList.class.getSimpleName();

    private final Type<char[]> arrayType = Type.of(char[].class);

    private final Type<Character> elementType = Type.of(char.class);
    private final List<Type<?>> parameterTypes = List.of(elementType);

    /**
     * Constructs a new PrimitiveCharListType instance.
     * This constructor is protected to allow subclassing while maintaining controlled instantiation
     * through the TypeFactory.
     */
    protected PrimitiveCharListType() {
        super(CHAR_LIST);
    }

    /**
     * Returns the Class object representing the CharList type.
     *
     * @return the Class object for CharList
     */
    @Override
    public Class<CharList> javaType() {
        return CharList.class;
    }

    /**
     * Returns the Type object for the char element type.
     *
     * @return the Type object representing Character/char elements
     */
    @Override
    public Type<Character> elementType() {
        return elementType;
    }

    /**
     * Returns the parameter types associated with this list type.
     *
     * @return an immutable list containing the Character Type that describes the elements of this list type
     * @see #elementType()
     */
    @Override
    public List<Type<?>> parameterTypes() {
        return parameterTypes;
    }

    /**
     * Converts a CharList to its string representation.
     * The list is first converted to a char array, then serialized using the array type handler.
     * Returns {@code null} if the input list is {@code null}.
     *
     * @param x the CharList to convert
     * @return the string representation of the list, or {@code null} if input is null
     */
    @Override
    public String stringOf(final CharList x) {
        return x == null ? null : arrayType.stringOf(x.toArray());
    }

    /**
     * Parses a string representation and creates a CharList.
     * The string is first parsed as a char array, then wrapped in a CharList.
     * Returns {@code null} if the input string is {@code null} or empty.
     *
     * @param str the string to parse
     * @return a CharList created from the parsed values, or {@code null} if input is {@code null} or empty
     */
    @Override
    public CharList valueOf(final String str) {
        if (Strings.isEmpty(str)) {
            return null;
        }

        final char[] array = arrayType.valueOf(str);
        return array == null ? null : CharList.of(array);
    }

    /**
     * Appends the string representation of a CharList to an Appendable.
     * Delegates to the array type handler after converting the list to an array.
     * Appends "null" if the list is {@code null}.
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
     * Writes "null" if the list is {@code null}.
     *
     * @param writer the CharacterWriter to write to
     * @param x the CharList to write
     * @param config the serialization configuration
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, final CharList x, final JsonXmlSerConfig<?> config) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            arrayType.writeCharacter(writer, x.toArray(), config);
        }
    }
}
