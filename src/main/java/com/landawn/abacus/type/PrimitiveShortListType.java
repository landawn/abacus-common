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
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.ShortList;
import com.landawn.abacus.util.Strings;

@SuppressWarnings("java:S2160")
public final class PrimitiveShortListType extends AbstractPrimitiveListType<ShortList> {

    public static final String SHORT_LIST = ShortList.class.getSimpleName();

    private final Type<short[]> arrayType = N.typeOf(short[].class);

    private final Type<Short> elementType = N.typeOf(short.class);
    private final Type<Short>[] parameterTypes = new Type[] { elementType };

    protected PrimitiveShortListType() {
        super(SHORT_LIST);
    }

    /**
     * Returns the Class object representing the ShortList type.
     *
     * @return the Class object for ShortList.class
     */
    @Override
    public Class<ShortList> clazz() {
        return ShortList.class;
    }

    /**
     * Returns the Type instance for the element type of this list, which is primitive short.
     * This method provides access to the Type representation of individual list elements.
     *
     * @return the Type instance representing short type for list elements
     */
    @Override
    public Type<Short> getElementType() {
        return elementType;
    }

    /**
     * Returns the parameter types associated with this array type.
     *
     * @return an array containing the Short Type that describes the elements of this array type
     * @see #getElementType()
     */
    @Override
    public Type<Short>[] getParameterTypes() {
        return parameterTypes;
    }

    /**
     * Converts a ShortList to its string representation.
     * The list is converted to an array first, then formatted as comma-separated values 
     * enclosed in square brackets. For example, a ShortList containing {1, 2, 3} 
     * becomes "[1, 2, 3]".
     * 
     * @param x the ShortList to convert to string
     * @return the string representation of the list, or null if the input list is null
     */
    @Override
    public String stringOf(final ShortList x) {
        return x == null ? null : arrayType.stringOf(x.toArray());
    }

    /**
     * Parses a string representation of a short list and returns the corresponding ShortList.
     * The string should contain comma-separated short values enclosed in square brackets.
     * For example, "[1, 2, 3]" will be parsed to a ShortList containing {1, 2, 3}.
     * 
     * @param str the string to parse, expected format is "[value1, value2, ...]"
     * @return the parsed ShortList, or null if the input string is null or empty
     * @throws NumberFormatException if any element in the string cannot be parsed as a short
     */
    @Override
    public ShortList valueOf(final String str) {
        return Strings.isEmpty(str) ? null : ShortList.of(arrayType.valueOf(str));
    }

    /**
     * Appends the string representation of a ShortList to the given Appendable.
     * The list is formatted as comma-separated values enclosed in square brackets.
     * If the list is null, appends "null".
     * 
     * @param appendable the Appendable to write to (e.g., StringBuilder, Writer)
     * @param x the ShortList to append
     * @throws IOException if an I/O error occurs during the append operation
     */
    @Override
    public void appendTo(final Appendable appendable, final ShortList x) throws IOException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else {
            arrayType.appendTo(appendable, x.toArray());
        }
    }

    /**
     * Writes the character representation of a ShortList to the given CharacterWriter.
     * This method is optimized for performance when writing to character-based outputs.
     * The list is converted to an array and then written as comma-separated values 
     * enclosed in square brackets.
     * 
     * @param writer the CharacterWriter to write to
     * @param x the ShortList to write
     * @param config the serialization configuration (passed through to the array type writer)
     * @throws IOException if an I/O error occurs during the write operation
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, final ShortList x, final JSONXMLSerializationConfig<?> config) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            arrayType.writeCharacter(writer, x.toArray(), config);
        }
    }
}