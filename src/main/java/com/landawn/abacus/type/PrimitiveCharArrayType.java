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
import java.util.List;

import com.landawn.abacus.annotation.SuppressFBWarnings;
import com.landawn.abacus.exception.UncheckedSQLException;
import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.SK;
import com.landawn.abacus.util.Strings;

/**
 * Type handler for primitive {@code char[]} arrays.
 * Provides functionality for serialization, deserialization, database operations,
 * and conversion between char arrays and their various representations including {@link Clob} objects.
 * The {@link #stringOf(char[])} representation single-quotes each element (for example {@code ['a', 'b', 'c']}),
 * while {@link #appendTo(Appendable, char[])} and {@link #toString(char[])} emit the elements unquoted.
 */
@SuppressWarnings("java:S2160")
public final class PrimitiveCharArrayType extends AbstractPrimitiveArrayType<char[]> {

    /** The type name constant for the char array type, equal to {@code "char[]"}. */
    public static final String CHAR_ARRAY = char[].class.getSimpleName();

    private final Type<Character> elementType;
    private final List<Type<?>> parameterTypes;

    /**
     * Constructs a new PrimitiveCharArrayType instance.
     * This constructor is package-private and intended to be called only by the TypeFactory.
     */
    PrimitiveCharArrayType() {
        super(CHAR_ARRAY);

        elementType = TypeFactory.getType(char.class);
        parameterTypes = List.of(elementType);
    }

    /**
     * Returns the Class object representing the char array type.
     *
     * @return the Class object for char[]
     */
    @Override
    public Class<char[]> javaType() {
        return char[].class;
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
     * Returns the parameter types associated with this array type.
     *
     * @return an immutable list containing the Character Type that describes the elements of this array type
     * @see #elementType()
     */
    @Override
    public List<Type<?>> parameterTypes() {
        return parameterTypes;
    }

    /**
     * Converts a char array to its string representation.
     * The format is: ['a', 'b', 'c'] with each character quoted and separated by commas.
     * Returns {@code null} if the input array is {@code null}, or "[]" if the array is empty.
     *
     * <p>The returned string is a serializable representation designed to be parsed back into an equivalent value
     * via {@link #valueOf(String)}; {@code stringOf} and {@code valueOf} are inverse operations that round-trip. This
     * is the key distinction from {@link Object#toString()}, whose result is not guaranteed to be convertible back
     * into the original value.</p>
     *
     * @param x the char array to convert
     * @return the string representation of the array, or {@code null} if input is null
     * @see #valueOf(String)
     * @see #valueOf(Object)
     */
    @Override
    public String stringOf(final char[] x) {
        if (x == null) {
            return null; // NOSONAR
        } else if (x.length == 0) {
            return STR_FOR_EMPTY_ARRAY;
        }

        final StringBuilder sb = Objectory.createStringBuilder(calculateBufferSize(x.length, 5));

        sb.append(SK._BRACKET_L);

        for (int i = 0, len = x.length; i < len; i++) {
            if (i > 0) {
                sb.append(ELEMENT_SEPARATOR);
            }

            sb.append(SK.SINGLE_QUOTE);
            sb.append(x[i]);
            sb.append(SK.SINGLE_QUOTE);
        }

        sb.append(SK._BRACKET_R);

        final String str = sb.toString();

        Objectory.recycle(sb);

        return str;
    }

    /**
     * Parses a string representation and creates a char array.
     * Expected format: ['a', 'b', 'c'] with quoted characters or [a, b, c] without quotes.
     * Automatically detects whether characters are quoted and handles both formats.
     * Returns {@code null} if input is {@code null}, empty, or blank, or an empty array if input is {@code "[]"}.
     *
     * <p>This method is the inverse of {@code stringOf} and round-trips with it: it parses the string produced by
     * {@code stringOf} back into a value of this type. Strings produced by {@link Object#toString()} are not
     * guaranteed to be parseable in this way.</p>
     *
     * @param str the string to parse
     * @return the parsed char array, or {@code null} if input is {@code null}, empty, or blank
     * @see #valueOf(Object)
     * @see #stringOf(char[])
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

        // Detect quoting per element (mirroring CharacterArrayType): a single isQuoted flag derived
        // from strs[0] mis-parses mixed-quoting input such as "[a, 'b']".
        for (int i = 0; i < len; i++) {
            final String element = strs[i];

            if (element.length() >= 2) {
                final char quoteChar = element.charAt(0);

                if ((quoteChar == SK._SINGLE_QUOTE || quoteChar == SK._DOUBLE_QUOTE) && element.charAt(element.length() - 1) == quoteChar) {
                    a[i] = elementType.valueOf(element.substring(1, element.length() - 1));
                    continue;
                }
            }

            a[i] = elementType.valueOf(element);
        }

        return a;
    }

    /**
     * Converts an object to a char array.
     * Handles {@link Reader} (reads all characters) and {@link Clob} objects (extracts character content and frees the Clob)
     * as special cases. For other object types, converts to string first then parses as char array.
     * Returns {@code null} if input is {@code null}.
     *
     * @param obj the object to convert (can be a {@link Reader}, {@link Clob}, or any other type)
     * @return the char array representation of the object, or {@code null} if input is null
     * @throws UncheckedSQLException if a database access error occurs while reading or freeing a Clob
     * @throws UnsupportedOperationException if the Clob length exceeds {@link Integer#MAX_VALUE}
     */
    @SuppressFBWarnings
    @Override
    public char[] valueOf(final Object obj) {
        if (obj == null) {
            return null; // NOSONAR
        } else if (obj instanceof Reader reader) {
            return IOUtil.readAllChars(reader);
        } else if (obj instanceof Clob clob) {
            RuntimeException primaryException = null;

            try {
                final long len = clob.length();
                if (len > Integer.MAX_VALUE) {
                    throw new UnsupportedOperationException("Clob too large to convert to char[]: " + len + " characters");
                }
                return clob.getSubString(1, (int) len).toCharArray();
            } catch (final SQLException e) {
                primaryException = new UncheckedSQLException(e);
                throw primaryException;
            } catch (final RuntimeException e) {
                primaryException = e;
                throw primaryException;
            } finally {
                try {
                    clob.free();
                } catch (final SQLException e) {
                    final UncheckedSQLException freeException = new UncheckedSQLException(e);
                    if (primaryException != null) {
                        primaryException.addSuppressed(freeException);
                    } else {
                        throw freeException; //NOSONAR
                    }
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
     * <p>
     * <b>appendTo vs. serializeTo:</b> {@code appendTo} produces a plain, {@code toString()}-style rendering with no
     * JSON/XML quoting or escaping (for general text output), whereas {@code serializeTo} produces the JSON/XML
     * serialized form (applying string quotation and character escaping per the serialization config) and is used by the
     * JSON/XML serializers.
     *
     * @param appendable the Appendable to write to
     * @param x the char array to append
     * @throws IOException if an I/O error occurs
     * @implNote
     * This method appends a string representation of {@code x} to {@code appendable} (the literal {@code "null"} for a
     * {@code null} value). Conceptually this is the human-readable form produced by {@code toString()}, <i>not</i> the
     * value returned by {@code stringOf}, which is a formatted, serializable representation (typically a JSON string)
     * that {@link #valueOf(String)} can convert back into an equivalent value. For values whose nested structure makes
     * the two forms differ (collections, maps, arrays), {@code appendTo} emits the unquoted, {@code toString()}-style
     * form; it is therefore not, in the general contract, a plain
     * {@code appendable.append(x == null ? NULL_STRING : stringOf(x))}. (For value types whose human-readable and
     * serialized forms coincide, the appended text is naturally identical to {@code stringOf(x)}.)
     */
    @Override
    public void appendTo(final Appendable appendable, final char[] x) throws IOException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else {
            appendable.append(SK._BRACKET_L);

            for (int i = 0, len = x.length; i < len; i++) {
                if (i > 0) {
                    appendable.append(ELEMENT_SEPARATOR);
                }

                appendable.append(x[i]);
            }

            appendable.append(SK._BRACKET_R);
        }
    }

    /**
     * Writes the character representation of a char array to a CharacterWriter.
     * If a character quotation is specified in the config, characters are quoted.
     * Single quotes within characters are escaped when using single quote quotation.
     * Writes "null" if the array is {@code null}.
     * <p>
     * This method is specifically designed for JSON/XML serialization: it writes the serialized form of {@code x} to the
     * {@code CharacterWriter}, applying string quotation and character escaping according to the supplied serialization
     * config (a {@code null} config means no surrounding quotation). It is the streaming counterpart of {@code stringOf}
     * and is invoked by the JSON/XML serializers.
     * <p>
     * <b>serializeTo vs. appendTo:</b> {@code serializeTo} produces machine-readable JSON/XML (quoted and escaped),
     * whereas {@code appendTo} produces a plain, human-readable {@code toString()}-style rendering without JSON/XML
     * quoting or escaping.
     *
     * @param writer the CharacterWriter to write to
     * @param x the char array to write
     * @param config the serialization configuration that may specify character quotation
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void serializeTo(final CharacterWriter writer, final char[] x, final JsonXmlSerConfig<?> config) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            writer.write(SK._BRACKET_L);

            final char charQuotation = (config == null) ? SK.CHAR_ZERO : config.getCharQuotation();

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

            writer.write(SK._BRACKET_R);
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
     * @param x the char array to convert
     * @param output the Collection to add the boxed Character values to
     */
    @Override
    public void arrayToCollection(final char[] x, final Collection<?> output) {
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

        return Strings.join(x, 0, x.length, ELEMENT_SEPARATOR, SK.BRACKET_L, SK.BRACKET_R);
    }
}
