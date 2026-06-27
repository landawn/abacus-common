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
import java.io.InputStream;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

import com.landawn.abacus.annotation.SuppressFBWarnings;
import com.landawn.abacus.exception.UncheckedSQLException;
import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.SK;
import com.landawn.abacus.util.Strings;

/**
 * Type handler for primitive {@code byte[]} arrays, providing serialization, deserialization,
 * database operations, and conversion between byte arrays and various representations,
 * including {@link java.sql.Blob} objects.
 * String representations use the format {@code [1, 2, 3]} with comma-separated numeric byte values
 * enclosed in square brackets.
 */
@SuppressWarnings("java:S2160")
public final class PrimitiveByteArrayType extends AbstractPrimitiveArrayType<byte[]> {

    /** The type name constant for the byte array type, equal to {@code "byte[]"}. */
    public static final String BYTE_ARRAY = byte[].class.getSimpleName();

    private final Type<Byte> elementType;
    private final List<Type<?>> parameterTypes;

    /**
     * Constructs a new PrimitiveByteArrayType instance.
     * This constructor is package-private and intended to be called only by the TypeFactory.
     */
    PrimitiveByteArrayType() {
        super(BYTE_ARRAY);

        elementType = TypeFactory.getType(byte.class);
        parameterTypes = List.of(elementType);
    }

    /**
     * Returns the Class object representing the byte array type.
     *
     * @return the Class object for byte[]
     */
    @Override
    public Class<byte[]> javaType() {
        return byte[].class;
    }

    /**
     * Returns the Type object for the byte element type.
     *
     * @return the Type object representing Byte/byte elements
     */
    @Override
    public Type<Byte> elementType() {
        return elementType;
    }

    /**
     * Returns the parameter types associated with this array type.
     *
     * @return an immutable list containing the Byte Type that describes the elements of this array type
     * @see #elementType()
     */
    @Override
    public List<Type<?>> parameterTypes() {
        return parameterTypes;
    }

    /**
     * Indicates whether this type represents a primitive byte array.
     * Always returns {@code true} for PrimitiveByteArrayType.
     *
     * @return {@code true}, as this type handler is specifically for byte arrays
     */
    @Override
    public boolean isPrimitiveByteArray() {
        return true;
    }

    /**
     * Converts a byte array to its string representation.
     * The format is: [1, 2, 3] with elements separated by commas.
     * Returns {@code null} if the input array is {@code null}, or "[]" if the array is empty.
     *
     * <p>The returned string is a serializable representation designed to be parsed back into an equivalent value
     * via {@link #valueOf(String)}; {@code stringOf} and {@code valueOf} are inverse operations that round-trip. This
     * is the key distinction from {@link Object#toString()}, whose result is not guaranteed to be convertible back
     * into the original value.</p>
     *
     * @param x the byte array to convert
     * @return the string representation of the array, or {@code null} if input is null
     * @see #valueOf(String)
     * @see #valueOf(Object)
     */
    @Override
    public String stringOf(final byte[] x) {
        if (x == null) {
            return null; // NOSONAR
        } else if (x.length == 0) {
            return STR_FOR_EMPTY_ARRAY;
        }

        return Strings.join(x, 0, x.length, ELEMENT_SEPARATOR, SK.BRACKET_L, SK.BRACKET_R);
    }

    /**
     * Parses a string representation and creates a byte array.
     * Expected format: [1, 2, 3] or similar numeric value representations.
     * Returns {@code null} if input is {@code null}, empty, or blank, or an empty array if input is {@code "[]"}.
     *
     * <p>This method is the inverse of {@code stringOf} and round-trips with it: it parses the string produced by
     * {@code stringOf} back into a value of this type. Strings produced by {@link Object#toString()} are not
     * guaranteed to be parseable in this way.</p>
     *
     * @param str the string to parse
     * @return the parsed byte array; {@code null} if input is {@code null}, empty, or blank;
     *         or an empty array if input is {@code "[]"}
     * @see #valueOf(Object)
     * @see #stringOf(byte[])
     */
    @Override
    public byte[] valueOf(final String str) {
        if (Strings.isEmpty(str) || Strings.isBlank(str)) {
            return null; // NOSONAR
        } else if (STR_FOR_EMPTY_ARRAY.equals(str)) {
            return N.EMPTY_BYTE_ARRAY;
        }

        final String[] strs = split(str);
        final int len = strs.length;
        final byte[] a = new byte[len];

        if (len > 0) {
            for (int i = 0; i < len; i++) {
                a[i] = elementType.valueOf(strs[i]);
            }
        }

        return a;
    }

    /**
     * Converts an object to a byte array.
     * Handles {@link InputStream} (reads all bytes) and {@link Blob} objects (extracts bytes and frees the Blob)
     * as special cases. For other object types, converts the object to its string representation first
     * and then parses that string as a byte array.
     * Returns {@code null} if input is {@code null}.
     *
     * @param obj the object to convert (can be an {@link InputStream}, {@link Blob}, or any other type)
     * @return the byte array representation of the object, or {@code null} if input is null
     * @throws UnsupportedOperationException if the input is a {@link Blob} whose length exceeds {@link Integer#MAX_VALUE}
     * @throws UncheckedSQLException if a database access error occurs while reading or freeing a Blob
     */
    @SuppressFBWarnings
    @Override
    public byte[] valueOf(final Object obj) {
        if (obj == null) {
            return null; // NOSONAR
        } else if (obj instanceof InputStream is) {
            return IOUtil.readAllBytes(is);
        } else if (obj instanceof Blob blob) {
            RuntimeException primaryException = null;

            try {
                final long len = blob.length();

                if (len > Integer.MAX_VALUE) {
                    throw new UnsupportedOperationException("Blob too large to convert to byte[]: " + len + " bytes");
                }

                return blob.getBytes(1, (int) len);
            } catch (final SQLException e) {
                primaryException = new UncheckedSQLException(e);
                throw primaryException;
            } catch (final RuntimeException e) {
                primaryException = e;
                throw primaryException;
            } finally {
                try {
                    blob.free();
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
     * Retrieves a byte array from a ResultSet at the specified column index.
     *
     * @param rs the ResultSet to read from
     * @param columnIndex the column index (1-based)
     * @return the byte array from the database
     * @throws SQLException if a database access error occurs
     */
    @Override
    public byte[] get(final ResultSet rs, final int columnIndex) throws SQLException {
        return rs.getBytes(columnIndex);
    }

    /**
     * Retrieves a byte array from a ResultSet using the specified column label.
     *
     * @param rs the ResultSet to read from
     * @param columnName the column label/name
     * @return the byte array from the database
     * @throws SQLException if a database access error occurs
     */
    @Override
    public byte[] get(final ResultSet rs, final String columnName) throws SQLException {
        return rs.getBytes(columnName);
    }

    /**
     * Sets a byte array value in a PreparedStatement at the specified parameter index.
     *
     * @param stmt the PreparedStatement to set the parameter on
     * @param columnIndex the parameter index (1-based)
     * @param x the byte array to set
     * @throws SQLException if a database access error occurs
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final byte[] x) throws SQLException {
        stmt.setBytes(columnIndex, x);
    }

    /**
     * Sets a byte array value in a CallableStatement using the specified parameter name.
     *
     * @param stmt the CallableStatement to set the parameter on
     * @param parameterName the name of the parameter
     * @param x the byte array to set
     * @throws SQLException if a database access error occurs
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final byte[] x) throws SQLException {
        stmt.setBytes(parameterName, x);
    }

    /**
     * Sets a byte array value in a PreparedStatement with SQL type information.
     * The sqlTypeOrLength parameter is ignored as byte arrays have their own specific SQL type.
     *
     * @param stmt the PreparedStatement to set the parameter on
     * @param columnIndex the parameter index (1-based)
     * @param x the byte array to set
     * @param sqlTypeOrLength the SQL type or length (ignored for byte arrays)
     * @throws SQLException if a database access error occurs
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final byte[] x, final int sqlTypeOrLength) throws SQLException {
        stmt.setBytes(columnIndex, x);
    }

    /**
     * Sets a byte array value in a CallableStatement with SQL type information.
     * The sqlTypeOrLength parameter is ignored as byte arrays have their own specific SQL type.
     *
     * @param stmt the CallableStatement to set the parameter on
     * @param parameterName the name of the parameter
     * @param x the byte array to set
     * @param sqlTypeOrLength the SQL type or length (ignored for byte arrays)
     * @throws SQLException if a database access error occurs
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final byte[] x, final int sqlTypeOrLength) throws SQLException {
        stmt.setBytes(parameterName, x);
    }

    /**
     * Appends the string representation of a byte array to an Appendable.
     * The format is: [1, 2, 3] with proper element separation.
     * Appends "null" if the array is {@code null}.
     * <p>
     * <b>appendTo vs. serializeTo:</b> {@code appendTo} produces a plain, {@code toString()}-style rendering with no
     * JSON/XML quoting or escaping (for general text output), whereas {@code serializeTo} produces the JSON/XML
     * serialized form (applying string quotation and character escaping per the serialization config) and is used by the
     * JSON/XML serializers.
     *
     * @param appendable the Appendable to write to
     * @param x the byte array to append
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
    public void appendTo(final Appendable appendable, final byte[] x) throws IOException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else {
            appendable.append(SK._BRACKET_L);

            for (int i = 0, len = x.length; i < len; i++) {
                if (i > 0) {
                    appendable.append(ELEMENT_SEPARATOR);
                }

                appendable.append(N.stringOf(x[i]));
            }

            appendable.append(SK._BRACKET_R);
        }
    }

    /**
     * Writes the character representation of a byte array to a CharacterWriter.
     * Uses optimized write methods for better performance.
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
     * @param x the byte array to write
     * @param config the serialization configuration (currently unused for byte arrays)
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void serializeTo(final CharacterWriter writer, final byte[] x, final JsonXmlSerConfig<?> config) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            writer.write(SK._BRACKET_L);

            for (int i = 0, len = x.length; i < len; i++) {
                if (i > 0) {
                    writer.write(ELEMENT_SEPARATOR);
                }

                writer.write(x[i]);
            }

            writer.write(SK._BRACKET_R);
        }
    }

    /**
     * Converts a Collection of Byte objects to a primitive byte array.
     * Each element in the collection is unboxed to its primitive byte value.
     * Returns {@code null} if the input collection is {@code null}.
     *
     * @param c the Collection of Byte objects to convert
     * @return a byte array containing the unboxed values, or {@code null} if input is null
     */
    @Override
    public byte[] collectionToArray(final Collection<?> c) {
        if (c == null) {
            return null; // NOSONAR
        }

        final byte[] a = new byte[c.size()];

        int i = 0;

        for (final Object e : c) {
            a[i++] = (Byte) e;
        }

        return a;
    }

    /**
     * Converts a byte array to a Collection.
     * Each primitive byte value is boxed to a Byte object and added to the output collection.
     * Does nothing if the input array is {@code null} or empty.
     *
     * @param x the byte array to convert
     * @param output the Collection to add the boxed Byte values to
     */
    @Override
    public void arrayToCollection(final byte[] x, final Collection<?> output) {
        if (N.notEmpty(x)) {
            final Collection<Object> c = (Collection<Object>) output;

            for (final byte element : x) {
                c.add(element);
            }
        }
    }

    /**
     * Calculates the hash code for a byte array.
     * Uses the standard Arrays.hashCode algorithm for consistency.
     *
     * @param x the byte array to hash
     * @return the hash code of the array
     */
    @Override
    public int hashCode(final byte[] x) {
        return N.hashCode(x);
    }

    /**
     * Compares two byte arrays for equality.
     * Arrays are considered equal if they have the same length and all corresponding elements are equal.
     * Two {@code null} arrays are considered equal.
     *
     * @param x the first byte array
     * @param y the second byte array
     * @return {@code true} if the arrays are equal, {@code false} otherwise
     */
    @Override
    public boolean equals(final byte[] x, final byte[] y) {
        return N.equals(x, y);
    }
}
