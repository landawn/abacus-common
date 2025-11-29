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

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Array;

import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.parser.JSONXMLSerializationConfig;
import com.landawn.abacus.util.BufferedJSONWriter;
import com.landawn.abacus.util.BufferedXMLWriter;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.Range;
import com.landawn.abacus.util.Range.BoundType;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.WD;

/**
 * Type handler for Range objects containing comparable values.
 * This class handles serialization and deserialization of Range instances.
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Create a RangeType for Integer
 * Type<Range<Integer>> type = TypeFactory.getType("Range<Integer>");
 *
 * // Serialize a Range to string
 * Range<Integer> closedRange = Range.closed(1, 10);  // [1, 10]
 * String str1 = type.stringOf(closedRange);  // Returns "[1, 10]"
 *
 * Range<Integer> openRange = Range.open(1, 10);  // (1, 10)
 * String str2 = type.stringOf(openRange);  // Returns "(1, 10)"
 *
 * // Deserialize string to Range
 * Range<Integer> restored = type.valueOf("[1, 10]");
 * boolean contains5 = restored.contains(5);  // true
 * }</pre>
 *
 * @param <T> the type of values in the range, must be Comparable
 */
@SuppressWarnings("java:S2160")
public class RangeType<T extends Comparable<? super T>> extends AbstractType<Range<T>> {

    static final Type<String> strType = TypeFactory.getType(String.class);

    public static final String RANGE = Range.class.getSimpleName();

    private final String declaringName;

    private final Class<Range<T>> typeClass;

    private final Type<T>[] parameterTypes;

    private final Type<T> elementType;

    @SuppressWarnings("rawtypes")
    RangeType(final String parameterTypeName) {
        super(RANGE + WD.LESS_THAN + TypeFactory.getType(parameterTypeName).name() + WD.GREATER_THAN);

        declaringName = RANGE + WD.LESS_THAN + TypeFactory.getType(parameterTypeName).declaringName() + WD.GREATER_THAN;
        typeClass = (Class) Range.class;
        parameterTypes = new Type[] { TypeFactory.getType(parameterTypeName) };
        elementType = parameterTypes[0];
    }

    /**
     * Returns the declaring name of this type, which includes the Range class name
     * and its parameterized type in angle brackets.
     *
     * @return the declaring name in the format "Range&lt;ElementType&gt;"
     */
    @Override
    public String declaringName() {
        return declaringName;
    }

    /**
     * Returns the Class object representing the Range type.
     *
     * @return the Class object for Range.class
     */
    @Override
    public Class<Range<T>> clazz() {
        return typeClass;
    }

    /**
     * Returns the Type instance for the element type of this Range.
     * This represents the type of values that the Range can contain.
     *
     * @return the Type instance for the element type
     */
    @Override
    public Type<T> getElementType() {
        return elementType;
    }

    /**
     * Returns an array containing the Type instances for the parameter types of this Range.
     * For Range, this returns a single-element array containing the element type.
     *
     * @return an array with one Type instance representing the element type of the Range
     */
    @Override
    public Type<T>[] getParameterTypes() {
        return parameterTypes;
    }

    /**
     * Indicates whether this type is a generic type.
     * RangeType is always generic as it is parameterized with a type T.
     *
     * @return {@code true}, indicating this is a generic type
     */
    @Override
    public boolean isGenericType() {
        return true;
    }

    /**
     * Converts a Range object to its string representation.
     * The format depends on the bound type:
     * - Open-Open: "(lower, upper)"
     * - Open-Closed: "(lower, upper]"
     * - Closed-Open: "[lower, upper)"
     * - Closed-Closed: "[lower, upper]"
     *
     * @param x the Range to convert to string
     * @return the string representation of the Range, or {@code null} if the input is null
     */
    @Override
    public String stringOf(final Range<T> x) {
        if (x == null) {
            return null; // NOSONAR
        }

        final BoundType boundType = x.boundType();
        final String prefix = (boundType == BoundType.OPEN_OPEN || boundType == BoundType.OPEN_CLOSED) ? "(" : "[";
        final String postfix = (boundType == BoundType.OPEN_OPEN || boundType == BoundType.CLOSED_OPEN) ? ")" : "]";
        Type<T> type = elementType;

        if (x.lowerEndpoint() != null) {
            type = TypeFactory.getType(x.lowerEndpoint().getClass());
        } else if (x.upperEndpoint() != null) {
            type = TypeFactory.getType(x.upperEndpoint().getClass());
        }

        return prefix + type.stringOf(x.lowerEndpoint()) + ELEMENT_SEPARATOR + type.stringOf(x.upperEndpoint()) + postfix;
    }

    /**
     * Parses a string representation of a Range and returns the corresponding Range object.
     * The string should be in one of the following formats:
     * - "(lower, upper)" for open-open range
     * - "(lower, upper]" for open-closed range
     * - "[lower, upper)" for closed-open range
     * - "[lower, upper]" for closed-closed range
     *
     * @param str the string to parse
     * @return the parsed Range object, or {@code null} if the input string is {@code null} or empty
     * @throws IllegalArgumentException if the string format is invalid
     */
    @Override
    public Range<T> valueOf(String str) {
        str = Strings.trim(str);

        if (Strings.isEmpty(str)) {
            return null; // NOSONAR
        }

        final String prefix = str.substring(0, 1);
        final String postfix = str.substring(str.length() - 1);

        final T[] tmp = (T[]) Utils.jsonParser.deserialize(str, 1, str.length() - 1, Utils.jdc, Array.newInstance(elementType.clazz(), 0).getClass());

        if ("(".equals(prefix)) {
            return ")".equals(postfix) ? Range.open(tmp[0], tmp[1]) : Range.openClosed(tmp[0], tmp[1]);
        } else {
            return ")".equals(postfix) ? Range.closedOpen(tmp[0], tmp[1]) : Range.closed(tmp[0], tmp[1]);
        }
    }

    /**
     * Appends the string representation of a Range to the given Appendable.
     * The format depends on the bound type and includes the appropriate brackets/parentheses.
     * If the Range is {@code null}, appends "null".
     *
     * @param appendable the Appendable to write to (e.g., StringBuilder, Writer)
     * @param x the Range to append
     * @throws IOException if an I/O error occurs during the append operation
     */
    @Override
    public void appendTo(final Appendable appendable, final Range<T> x) throws IOException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else {
            final BoundType boundType = x.boundType();
            final String prefix = (boundType == BoundType.OPEN_OPEN || boundType == BoundType.OPEN_CLOSED) ? "(" : "[";
            final String postfix = (boundType == BoundType.OPEN_OPEN || boundType == BoundType.CLOSED_OPEN) ? ")" : "]";
            Type<T> type = elementType;

            if (x.lowerEndpoint() != null) {
                type = TypeFactory.getType(x.lowerEndpoint().getClass());
            } else if (x.upperEndpoint() != null) {
                type = TypeFactory.getType(x.upperEndpoint().getClass());
            }

            if (appendable instanceof Writer writer) {
                final boolean isBufferedWriter = IOUtil.isBufferedWriter(writer);
                final Writer bw = isBufferedWriter ? writer : Objectory.createBufferedWriter(writer); //NOSONAR

                try {
                    bw.write(prefix);

                    type.appendTo(bw, x.lowerEndpoint());
                    bw.write(ELEMENT_SEPARATOR_CHAR_ARRAY);
                    type.appendTo(bw, x.upperEndpoint());

                    bw.write(postfix);

                    if (!isBufferedWriter) {
                        bw.flush();
                    }
                } catch (final IOException e) {
                    throw new UncheckedIOException(e);
                } finally {
                    if (!isBufferedWriter) {
                        Objectory.recycle((BufferedWriter) bw);
                    }
                }
            } else {
                appendable.append(prefix);

                type.appendTo(appendable, x.lowerEndpoint());
                appendable.append(ELEMENT_SEPARATOR);
                type.appendTo(appendable, x.upperEndpoint());

                appendable.append(postfix);
            }
        }
    }

    /**
     * Writes the character representation of a Range to the given CharacterWriter.
     * The Range is first converted to a string representation, then written as a quoted string
     * according to the serialization configuration.
     *
     * @param writer the CharacterWriter to write to
     * @param x the Range to write
     * @param config the serialization configuration that determines string quotation
     * @throws IOException if an I/O error occurs during the write operation
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, final Range<T> x, final JSONXMLSerializationConfig<?> config) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            final BoundType boundType = x.boundType();
            final String prefix = (boundType == BoundType.OPEN_OPEN || boundType == BoundType.OPEN_CLOSED) ? "(" : "[";
            final String postfix = (boundType == BoundType.OPEN_OPEN || boundType == BoundType.CLOSED_OPEN) ? ")" : "]";
            Type<T> type = elementType;

            if (x.lowerEndpoint() != null) {
                type = TypeFactory.getType(x.lowerEndpoint().getClass());
            } else if (x.upperEndpoint() != null) {
                type = TypeFactory.getType(x.upperEndpoint().getClass());
            }

            final boolean isBufferedJSONWriter = writer instanceof BufferedJSONWriter;
            final CharacterWriter tmpWriter = isBufferedJSONWriter ? Objectory.createBufferedJSONWriter() : Objectory.createBufferedXMLWriter();

            try {
                tmpWriter.write(prefix);
                type.writeCharacter(tmpWriter, x.lowerEndpoint(), config);
                tmpWriter.write(ELEMENT_SEPARATOR_CHAR_ARRAY);
                type.writeCharacter(tmpWriter, x.upperEndpoint(), config);
                tmpWriter.write(postfix);

                strType.writeCharacter(writer, tmpWriter.toString(), config);
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            } finally {
                if (isBufferedJSONWriter) {
                    Objectory.recycle((BufferedJSONWriter) tmpWriter);
                } else {
                    Objectory.recycle((BufferedXMLWriter) tmpWriter);
                }
            }
        }
    }
}
