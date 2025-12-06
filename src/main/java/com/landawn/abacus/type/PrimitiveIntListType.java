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
import com.landawn.abacus.util.IntList;
import com.landawn.abacus.util.Strings;

@SuppressWarnings("java:S2160")
public final class PrimitiveIntListType extends AbstractPrimitiveListType<IntList> {

    public static final String INT_LIST = IntList.class.getSimpleName();

    private final Type<int[]> arrayType = Type.of(int[].class);

    private final Type<Integer> elementType = Type.of(int.class);
    private final Type<Integer>[] parameterTypes = new Type[] { elementType };

    protected PrimitiveIntListType() {
        super(INT_LIST);
    }

    /**
     * Returns the Class object representing the IntList type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<IntList> type = TypeFactory.getType(IntList.class);
     * Class&lt;IntList&gt; clazz = type.clazz();
     * // clazz equals IntList.class
     * }</pre>
     *
     * @return the Class object for IntList.class
     */
    @Override
    public Class<IntList> clazz() {
        return IntList.class;
    }

    /**
     * Returns the Type instance for the element type of this list, which is primitive int.
     * This method provides access to the Type representation of individual list elements.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<IntList> type = TypeFactory.getType(IntList.class);
     * Type&lt;Integer&gt; elemType = type.getElementType();
     * // elemType can be used for element-level operations
     * }</pre>
     *
     * @return the Type instance representing int type for list elements
     */
    @Override
    public Type<Integer> getElementType() {
        return elementType;
    }

    /**
     * Returns the parameter types associated with this list type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<IntList> type = TypeFactory.getType(IntList.class);
     * Type&lt;Integer&gt;[] paramTypes = type.getParameterTypes();
     * // paramTypes[0] represents the element type
     * }</pre>
     *
     * @return an array containing the Integer Type that describes the elements of this list type
     * @see #getElementType()
     */
    @Override
    public Type<Integer>[] getParameterTypes() {
        return parameterTypes;
    }

    /**
     * Converts an IntList to its string representation.
     * The list is converted to an array first, then formatted as comma-separated values
     * enclosed in square brackets. For example, an IntList containing {1, 2, 3}
     * becomes "[1, 2, 3]".
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<IntList> type = TypeFactory.getType(IntList.class);
     * IntList list = IntList.of(1, 2, 3);
     * String str = type.stringOf(list);
     * // str equals "[1, 2, 3]"
     *
     * String nullStr = type.stringOf(null);
     * // nullStr equals null
     * }</pre>
     *
     * @param x the IntList to convert to string
     * @return the string representation of the list, or {@code null} if the input list is null
     */
    @Override
    public String stringOf(final IntList x) {
        return x == null ? null : arrayType.stringOf(x.toArray());
    }

    /**
     * Parses a string representation of an int list and returns the corresponding IntList.
     * The string should contain comma-separated integer values enclosed in square brackets.
     * For example, "[1, 2, 3]" will be parsed to an IntList containing {1, 2, 3}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<IntList> type = TypeFactory.getType(IntList.class);
     * IntList list = type.valueOf("[1, 2, 3]");
     * // list contains {1, 2, 3}
     *
     * IntList empty = type.valueOf("[]");
     * // empty.size() equals 0
     *
     * IntList nullList = type.valueOf(null);
     * // nullList equals null
     * }</pre>
     *
     * @param str the string to parse, expected format is "[value1, value2, ...]"
     * @return the parsed IntList, or {@code null} if the input string is {@code null} or empty
     * @throws NumberFormatException if any element in the string cannot be parsed as an integer
     */
    @Override
    public IntList valueOf(final String str) {
        return Strings.isEmpty(str) ? null : IntList.of(arrayType.valueOf(str));
    }

    /**
     * Appends the string representation of an IntList to the given Appendable.
     * The list is formatted as comma-separated values enclosed in square brackets.
     * If the list is {@code null}, appends "null".
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<IntList> type = TypeFactory.getType(IntList.class);
     * StringBuilder sb = new StringBuilder();
     * IntList list = IntList.of(10, 20, 30);
     * type.appendTo(sb, list);
     * // sb.toString() equals "[10, 20, 30]"
     *
     * StringBuilder sb2 = new StringBuilder();
     * type.appendTo(sb2, null);
     * // sb2.toString() equals "null"
     * }</pre>
     *
     * @param appendable the Appendable to write to (e.g., StringBuilder, Writer)
     * @param x the IntList to append
     * @throws IOException if an I/O error occurs during the append operation
     */
    @Override
    public void appendTo(final Appendable appendable, final IntList x) throws IOException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else {
            arrayType.appendTo(appendable, x.toArray());
        }
    }

    /**
     * Writes the character representation of an IntList to the given CharacterWriter.
     * This method is optimized for performance when writing to character-based outputs.
     * The list is converted to an array and then written as comma-separated values
     * enclosed in square brackets.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<IntList> type = TypeFactory.getType(IntList.class);
     * CharacterWriter writer = new CharacterWriter();
     * IntList list = IntList.of(5, 10, 15);
     * type.writeCharacter(writer, list, null);
     * // Writes: [5, 10, 15]
     *
     * type.writeCharacter(writer, null, null);
     * // Writes: null
     * }</pre>
     *
     * @param writer the CharacterWriter to write to
     * @param x the IntList to write
     * @param config the serialization configuration (passed through to the array type writer)
     * @throws IOException if an I/O error occurs during the write operation
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, final IntList x, final JSONXMLSerializationConfig<?> config) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            arrayType.writeCharacter(writer, x.toArray(), config);
        }
    }
}
