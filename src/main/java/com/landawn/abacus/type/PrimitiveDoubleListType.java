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
import com.landawn.abacus.util.DoubleList;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Strings;

/**
 * Type handler for DoubleList objects.
 * Provides functionality for serialization and deserialization of DoubleList instances
 * by delegating to the underlying double array type handler.
 */
@SuppressWarnings("java:S2160")
public final class PrimitiveDoubleListType extends AbstractPrimitiveListType<DoubleList> {

    public static final String DOUBLE_LIST = DoubleList.class.getSimpleName();

    private final Type<double[]> arrayType = N.typeOf(double[].class);

    private final Type<Double> elementType = N.typeOf(double.class);
    private final Type<Double>[] parameterTypes = new Type[] { elementType };

    protected PrimitiveDoubleListType() {
        super(DOUBLE_LIST);
    }

    /**
     * Returns the Class object representing the DoubleList type.
     *
     * @return the Class object for DoubleList
     */
    @Override
    public Class<DoubleList> clazz() {
        return DoubleList.class;
    }

    /**
     * Returns the Type object for the double element type.
     *
     * @return the Type object representing double elements
     */
    @Override
    public Type<Double> getElementType() {
        return elementType;
    }

    /**
     * Returns the parameter types associated with this array type.
     *
     * @return an array containing the Double Type that describes the elements of this array type
     * @see #getElementType()
     */
    @Override
    public Type<Double>[] getParameterTypes() {
        return parameterTypes;
    }

    /**
     * Converts a DoubleList to its string representation.
     * The list is first converted to a double array, then serialized using the array type handler.
     * Returns null if the input list is null.
     *
     * @param x the DoubleList to convert
     * @return the string representation of the list, or null if input is null
     */
    @Override
    public String stringOf(final DoubleList x) {
        return x == null ? null : arrayType.stringOf(x.toArray());
    }

    /**
     * Parses a string representation and creates a DoubleList.
     * The string is first parsed as a double array, then wrapped in a DoubleList.
     * Returns null if the input string is null or empty.
     *
     * @param str the string to parse
     * @return a DoubleList created from the parsed values, or null if input is null or empty
     */
    @Override
    public DoubleList valueOf(final String str) {
        return Strings.isEmpty(str) ? null : DoubleList.of(arrayType.valueOf(str));
    }

    /**
     * Appends the string representation of a DoubleList to an Appendable.
     * Delegates to the array type handler after converting the list to an array.
     * Appends "null" if the list is null.
     *
     * @param appendable the Appendable to write to
     * @param x the DoubleList to append
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void appendTo(final Appendable appendable, final DoubleList x) throws IOException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else {
            arrayType.appendTo(appendable, x.toArray());
        }
    }

    /**
     * Writes the character representation of a DoubleList to a CharacterWriter.
     * Delegates to the array type handler after converting the list to an array.
     * Writes "null" if the list is null.
     *
     * @param writer the CharacterWriter to write to
     * @param x the DoubleList to write
     * @param config the serialization configuration
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, final DoubleList x, final JSONXMLSerializationConfig<?> config) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            arrayType.writeCharacter(writer, x.toArray(), config);
        }
    }
}