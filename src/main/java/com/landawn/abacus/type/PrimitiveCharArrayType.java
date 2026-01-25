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
import java.io.Reader;
import java.sql.Clob;
import java.sql.SQLException;
import java.util.Collection;

import com.landawn.abacus.annotation.SuppressFBWarnings;
import com.landawn.abacus.exception.UncheckedSQLException;
import com.landawn.abacus.parser.JsonXmlSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.WD;

/**
 * Type handler for primitive char arrays (char[]).
 * Provides functionality for serialization, deserialization, database operations,
 * and conversion between char arrays and their various representations including Clob objects.
 * Character elements are quoted in string representations to handle special characters properly.
 */
@SuppressWarnings("java:S2160")
public final class PrimitiveCharArrayType extends AbstractPrimitiveArrayType<char[]> {

    public static final String CHAR_ARRAY = char[].class.getSimpleName();

    private final Type<Character> elementType;
    private final Type<Character>[] parameterTypes;

    PrimitiveCharArrayType() {
        super(CHAR_ARRAY);

        elementType = TypeFactory.getType(char.class);
        parameterTypes = new Type[] { elementType };
    }

    /**
     * Returns the Class object representing the char array type.
     *
     * @return the Class object for char[]
     */
    @Override
    public Class<char[]> clazz() {
        return char[].class;
    }

    /**
     * Returns the Type object for the char element type.
     *
     * @return the Type object representing Character/char elements
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
     * Converts a char array to its string representation.
     * The format is: ['a', 'b', 'c'] with each character quoted and separated by commas.
     * Returns {@code null} if the input array is {@code null}, or "[]" if the array is empty.
     *
     * @param x the char array to convert
     * @return the string representation of the array, or {@code null} if input is null
     */
    @Override
    public String stringOf(final char[] x) {
        if (x == null) {
            return null; // NOSONAR
        } else if (x.length == 0) {
            return STR_FOR_EMPTY_ARRAY;
        }

        final StringBuilder sb = Objectory.createStringBuilder(calculateBufferSize(x.length, 5));

        sb.append(WD._BRACKET_L);

        for (int i = 0, len = x.length; i < len; i++) {
            if (i > 0) {
                sb.append(ELEMENT_SEPARATOR);
            }

            sb.append(WD.QUOTATION_S);
            sb.append(x[i]);
            sb.append(WD.QUOTATION_S);
        }

        sb.append(WD._BRACKET_R);

        final String str = sb.toString();

        Objectory.recycle(sb);

        return str;
    }

    /**
     * Parses a string representation and creates a char array.
     * Expected format: ['a', 'b', 'c'] with quoted characters or [a, b, c] without quotes.
     * Automatically detects whether characters are quoted and handles both formats.
     * Returns {@code null} if input is {@code null}, empty array if input is empty or "[]".
     *
     * @param str the string to parse
     * @return the parsed char array, or {@code null} if input is null
     */
    @Override
    public char[] valueOf(final String str) {
        if (Strings.isEmpty(str) || Strings.isBlank(str)) {
            return null; // NOSONAR
        } else if (STR_FOR_EMPTY_ARRAY.equals(str)) {
            return N.EMPTY_CHAR_ARRAY;
        }

        final String[] strs = split(str);
        final int len = strs.length;
        final char[] a = new char[len];

        if (len > 0) {
            boolean isQuoted = (strs[0].length() > 1) && ((strs[0].charAt(0) == WD._QUOTATION_S) || (strs[0].charAt(0) == WD._QUOTATION_D));

            if (isQuoted) {
                for (int i = 0; i < len; i++) {
                    if (strs[i].length() >= 2) {
                        a[i] = elementType.valueOf(strs[i].substring(1, strs[i].length() - 1));
                    } else {
                        a[i] = elementType.valueOf(strs[i]);
                    }
                }
            } else {
                for (int i = 0; i < len; i++) {
                    a[i] = elementType.valueOf(strs[i]);
                }
            }
        }

        return a;
    }

    /**
     * Converts an object to a char array.
     * Handles special case of Clob objects by extracting their character content.
     * For other object types, converts to string first then parses as char array.
     * Returns {@code null} if input is {@code null}.
     *
     * @param obj the object to convert (can be a Clob or other type)
     * @return the char array representation of the object, or {@code null} if input is null
     */
    @SuppressFBWarnings
    @Override
    public char[] valueOf(final Object obj) {
        if (obj == null) {
            return null; // NOSONAR
        } else if (obj instanceof Reader reader) {
            return IOUtil.readAllChars(reader);
        } else if (obj instanceof Clob clob) {
            try {
                final long len = clob.length();
                if (len > Integer.MAX_VALUE) {
                    throw new UnsupportedOperationException("Clob too large to convert to char[]: " + len + " characters");
                }
                return clob.getSubString(1, (int) len).toCharArray();
            } catch (final SQLException e) {
                throw new UncheckedSQLException(e);
            } finally {
                try {
                    clob.free();
                } catch (final SQLException e) {
                    throw new UncheckedSQLException(e); //NOSONAR
                }
            }
        } else {
            return valueOf(Type.<Object> of(obj.getClass()).stringOf(obj));
        }
    }

    /**
     * Appends the string representation of a char array to an Appendable.
     * The format is: [a, b, c] without quotes around the characters.
     * Appends "null" if the array is {@code null}.
     *
     * @param appendable the Appendable to write to
     * @param x the char array to append
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void appendTo(final Appendable appendable, final char[] x) throws IOException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else {
            appendable.append(WD._BRACKET_L);

            for (int i = 0, len = x.length; i < len; i++) {
                if (i > 0) {
                    appendable.append(ELEMENT_SEPARATOR);
                }

                appendable.append(x[i]);
            }

            appendable.append(WD._BRACKET_R);
        }
    }

    /**
     * Writes the character representation of a char array to a CharacterWriter.
     * If a character quotation is specified in the config, characters are quoted.
     * Single quotes within characters are escaped when using single quote quotation.
     * Writes "null" if the array is {@code null}.
     *
     * @param writer the CharacterWriter to write to
     * @param x the char array to write
     * @param config the serialization configuration that may specify character quotation
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, final char[] x, final JsonXmlSerializationConfig<?> config) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            writer.write(WD._BRACKET_L);

            final char charQuotation = (config == null) ? WD.CHAR_ZERO : config.getCharQuotation();

            if (charQuotation > 0) {
                for (int i = 0, len = x.length; i < len; i++) {
                    if (i > 0) {
                        writer.write(ELEMENT_SEPARATOR);
                    }

                    writer.write(charQuotation);

                    if (x[i] == '\'' && charQuotation == '\'') {
                        writer.write('\\');
                    }

                    writer.writeCharacter(x[i]);
                    writer.write(charQuotation);
                }
            } else {
                for (int i = 0, len = x.length; i < len; i++) {
                    if (i > 0) {
                        writer.write(ELEMENT_SEPARATOR);
                    }

                    writer.writeCharacter(x[i]);
                }
            }

            writer.write(WD._BRACKET_R);
        }
    }

    /**
     * Converts a Collection of Character objects to a primitive char array.
     * Each element in the collection is unboxed to its primitive char value.
     * Returns {@code null} if the input collection is {@code null}.
     *
     * @param c the Collection of Character objects to convert
     * @return a char array containing the unboxed values, or {@code null} if input is null
     */
    @Override
    public char[] collectionToArray(final Collection<?> c) {
        if (c == null) {
            return null; // NOSONAR
        }

        final char[] a = new char[c.size()];

        int i = 0;

        for (final Object e : c) {
            a[i++] = (Character) e;
        }

        return a;
    }

    /**
     * Converts a char array to a Collection.
     * Each primitive char value is boxed to a Character object and added to the output collection.
     * Does nothing if the input array is {@code null} or empty.
     *
     * @param <E> the type of elements in the output collection
     * @param x the char array to convert
     * @param output the Collection to add the boxed Character values to
     */
    @Override
    public <E> void arrayToCollection(final char[] x, final Collection<E> output) {
        if (N.notEmpty(x)) {
            final Collection<Object> c = (Collection<Object>) output;

            for (final char element : x) {
                c.add(element);
            }
        }
    }

    /**
     * Calculates the hash code for a char array.
     * Uses the standard Arrays.hashCode algorithm for consistency.
     *
     * @param x the char array to hash
     * @return the hash code of the array
     */
    @Override
    public int hashCode(final char[] x) {
        return N.hashCode(x);
    }

    /**
     * Compares two char arrays for equality.
     * Arrays are considered equal if they have the same length and all corresponding elements are equal.
     * Two {@code null} arrays are considered equal.
     *
     * @param x the first char array
     * @param y the second char array
     * @return {@code true} if the arrays are equal, {@code false} otherwise
     */
    @Override
    public boolean equals(final char[] x, final char[] y) {
        return N.equals(x, y);
    }

    /**
     * Converts a char array to a readable string representation.
     * The format is: [a, b, c] with elements separated by commas but without quotes.
     * Returns "null" if the array is {@code null}.
     *
     * @param x the char array to convert
     * @return a string representation suitable for display
     */
    @Override
    public String toString(final char[] x) {
        if (x == null) {
            return NULL_STRING;
        }

        return Strings.join(x, 0, x.length, ELEMENT_SEPARATOR, WD.BRACKET_L, WD.BRACKET_R);
    }
}
