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
import com.landawn.abacus.util.LongList;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Strings;

@SuppressWarnings("java:S2160")
public final class PrimitiveLongListType extends AbstractPrimitiveListType<LongList> {

    public static final String LONG_LIST = LongList.class.getSimpleName();

    private final Type<long[]> arrayType = N.typeOf(long[].class);

    private final Type<Long> elementType = N.typeOf(long.class);
    private final Type<Long>[] parameterTypes = new Type[] { elementType };

    protected PrimitiveLongListType() {
        super(LONG_LIST);
    }

    /**
     * Returns the Class object representing the LongList type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * PrimitiveLongListType type = new PrimitiveLongListType();
     * Class<LongList> clazz = type.clazz();
     * System.out.println(clazz.getName()); // Output: com.landawn.abacus.util.LongList
     * }</pre>
     *
     * @return the Class object for LongList.class
     */
    @Override
    public Class<LongList> clazz() {
        return LongList.class;
    }

    /**
     * Returns the Type instance for the element type of this list, which is primitive long.
     * This method provides access to the Type representation of individual list elements.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * PrimitiveLongListType type = new PrimitiveLongListType();
     * Type<?> elementType = type.getElementType();
     * System.out.println(elementType.name()); // Output: long
     * }</pre>
     *
     * @return the Type instance representing long type for list elements
     */
    @Override
    public Type<?> getElementType() {
        return elementType;
    }

    /**
     * Returns the parameter types associated with this list type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * PrimitiveLongListType type = new PrimitiveLongListType();
     * Type<Long>[] paramTypes = type.getParameterTypes();
     * System.out.println(paramTypes.length); // Output: 1
     * System.out.println(paramTypes[0].name()); // Output: long
     * }</pre>
     *
     * @return an array containing the Long Type that describes the elements of this list type
     * @see #getElementType()
     */
    @Override
    public Type<Long>[] getParameterTypes() {
        return parameterTypes;
    }

    /**
     * Converts a LongList to its string representation.
     * The list is converted to an array first, then formatted as comma-separated values
     * enclosed in square brackets.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * PrimitiveLongListType type = new PrimitiveLongListType();
     * LongList list = LongList.of(1L, 2L, 3L);
     * String str = type.stringOf(list);
     * System.out.println(str); // Output: [1, 2, 3]
     *
     * String nullStr = type.stringOf(null);
     * System.out.println(nullStr); // Output: null
     * }</pre>
     *
     * @param x the LongList to convert to string
     * @return the string representation of the list, or null if the input list is null
     */
    @Override
    public String stringOf(final LongList x) {
        return x == null ? null : arrayType.stringOf(x.toArray());
    }

    /**
     * Parses a string representation of a long list and returns the corresponding LongList.
     * The string should contain comma-separated long values enclosed in square brackets.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * PrimitiveLongListType type = new PrimitiveLongListType();
     * LongList list = type.valueOf("[1, 2, 3]");
     * System.out.println(list.size()); // Output: 3
     * System.out.println(list.get(0)); // Output: 1
     *
     * LongList emptyList = type.valueOf("[]");
     * System.out.println(emptyList.isEmpty()); // Output: true
     *
     * LongList nullList = type.valueOf(null);
     * System.out.println(nullList); // Output: null
     * }</pre>
     *
     * @param str the string to parse, expected format is "[value1, value2, ...]"
     * @return the parsed LongList, or null if the input string is null or empty
     * @throws NumberFormatException if any element in the string cannot be parsed as a long
     */
    @Override
    public LongList valueOf(final String str) {
        return Strings.isEmpty(str) ? null : LongList.of(arrayType.valueOf(str));
    }

    /**
     * Appends the string representation of a LongList to the given Appendable.
     * The list is formatted as comma-separated values enclosed in square brackets.
     * If the list is null, appends "null".
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * PrimitiveLongListType type = new PrimitiveLongListType();
     * LongList list = LongList.of(10L, 20L, 30L);
     * StringBuilder sb = new StringBuilder("Values: ");
     * type.appendTo(sb, list);
     * System.out.println(sb.toString()); // Output: Values: [10, 20, 30]
     *
     * StringBuilder nullSb = new StringBuilder();
     * type.appendTo(nullSb, null);
     * System.out.println(nullSb.toString()); // Output: null
     * }</pre>
     *
     * @param appendable the Appendable to write to (e.g., StringBuilder, Writer)
     * @param x the LongList to append
     * @throws IOException if an I/O error occurs during the append operation
     */
    @Override
    public void appendTo(final Appendable appendable, final LongList x) throws IOException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else {
            arrayType.appendTo(appendable, x.toArray());
        }
    }

    /**
     * Writes the character representation of a LongList to the given CharacterWriter.
     * This method is optimized for performance when writing to character-based outputs.
     * The list is converted to an array and then written as comma-separated values
     * enclosed in square brackets.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * PrimitiveLongListType type = new PrimitiveLongListType();
     * LongList list = LongList.of(100L, 200L, 300L);
     * CharacterWriter writer = new CharacterWriter();
     * JSONXMLSerializationConfig<?> config = new JSONXMLSerializationConfig<>();
     * type.writeCharacter(writer, list, config);
     * System.out.println(writer.toString()); // Output: [100, 200, 300]
     *
     * CharacterWriter nullWriter = new CharacterWriter();
     * type.writeCharacter(nullWriter, null, config);
     * System.out.println(nullWriter.toString()); // Output: null
     * }</pre>
     *
     * @param writer the CharacterWriter to write to
     * @param x the LongList to write
     * @param config the serialization configuration (passed through to the array type writer)
     * @throws IOException if an I/O error occurs during the write operation
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, final LongList x, final JSONXMLSerializationConfig<?> config) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            arrayType.writeCharacter(writer, x.toArray(), config);
        }
    }
}