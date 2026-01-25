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

import com.landawn.abacus.annotation.SuppressFBWarnings;
import com.landawn.abacus.exception.UncheckedSQLException;
import com.landawn.abacus.parser.JsonXmlSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.WD;

/**
 * Type handler for primitive byte arrays (byte[]).
 * Provides functionality for serialization, deserialization, database operations,
 * and conversion between byte arrays and their various representations including Blob objects.
 */
@SuppressWarnings("java:S2160")
public final class PrimitiveByteArrayType extends AbstractPrimitiveArrayType<byte[]> {

    public static final String BYTE_ARRAY = byte[].class.getSimpleName();

    private final Type<Byte> elementType;
    private final Type<Byte>[] parameterTypes;

    /**
     * Constructs a new PrimitiveByteArrayType instance.
     * This constructor is package-private and intended to be called only by the TypeFactory.
     */
    PrimitiveByteArrayType() {
        super(BYTE_ARRAY);

        elementType = TypeFactory.getType(byte.class);
        parameterTypes = new Type[] { elementType };
    }

    /**
     * Returns the Class object representing the byte array type.
     *
     * @return the Class object for byte[]
     */
    @Override
    public Class<byte[]> clazz() {
        return byte[].class;
    }

    /**
     * Returns the Type object for the byte element type.
     *
     * @return the Type object representing Byte/byte elements
     */
    @Override
    public Type<Byte> getElementType() {
        return elementType;
    }

    /**
     * Returns the parameter types associated with this array type.
     *
     * @return an array containing the Byte Type that describes the elements of this array type
     * @see #getElementType()
     */
    @Override
    public Type<Byte>[] getParameterTypes() {
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
     * @param x the byte array to convert
     * @return the string representation of the array, or {@code null} if input is null
     */
    @Override
    public String stringOf(final byte[] x) {
        if (x == null) {
            return null; // NOSONAR
        } else if (x.length == 0) {
            return STR_FOR_EMPTY_ARRAY;
        }

        return Strings.join(x, 0, x.length, ELEMENT_SEPARATOR, WD.BRACKET_L, WD.BRACKET_R);
    }

    /**
     * Parses a string representation and creates a byte array.
     * Expected format: [1, 2, 3] or similar numeric value representations.
     * Returns {@code null} if input is {@code null}, empty array if input is empty or "[]".
     *
     * @param str the string to parse
     * @return the parsed byte array, or {@code null} if input is null
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
     * Handles special case of Blob objects by extracting their byte content.
     * For other object types, converts to string first then parses as byte array.
     * Returns {@code null} if input is {@code null}.
     *
     * @param obj the object to convert (can be a Blob or other type)
     * @return the byte array representation of the object, or {@code null} if input is null
     */
    @SuppressFBWarnings
    @Override
    public byte[] valueOf(final Object obj) {
        if (obj == null) {
            return null; // NOSONAR
        } else if (obj instanceof InputStream is) {
            return IOUtil.readAllBytes(is);
        } else if (obj instanceof Blob blob) {
            try {
                return blob.getBytes(1, (int) blob.length());
            } catch (final SQLException e) {
                throw new UncheckedSQLException(e);
            } finally {
                try {
                    blob.free();
                } catch (final SQLException e) {
                    throw new UncheckedSQLException(e); //NOSONAR
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
     * @param columnLabel the column label/name
     * @return the byte array from the database
     * @throws SQLException if a database access error occurs
     */
    @Override
    public byte[] get(final ResultSet rs, final String columnLabel) throws SQLException {
        return rs.getBytes(columnLabel);
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
     *
     * @param appendable the Appendable to write to
     * @param x the byte array to append
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void appendTo(final Appendable appendable, final byte[] x) throws IOException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else {
            appendable.append(WD._BRACKET_L);

            for (int i = 0, len = x.length; i < len; i++) {
                if (i > 0) {
                    appendable.append(ELEMENT_SEPARATOR);
                }

                appendable.append(N.stringOf(x[i]));
            }

            appendable.append(WD._BRACKET_R);
        }
    }

    /**
     * Writes the character representation of a byte array to a CharacterWriter.
     * Uses optimized write methods for better performance.
     * Writes "null" if the array is {@code null}.
     *
     * @param writer the CharacterWriter to write to
     * @param x the byte array to write
     * @param config the serialization configuration (currently unused for byte arrays)
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, final byte[] x, final JsonXmlSerializationConfig<?> config) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            writer.write(WD._BRACKET_L);

            for (int i = 0, len = x.length; i < len; i++) {
                if (i > 0) {
                    writer.write(ELEMENT_SEPARATOR);
                }

                writer.write(x[i]);
            }

            writer.write(WD._BRACKET_R);
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
     * @param <E> the type of elements in the output collection
     * @param x the byte array to convert
     * @param output the Collection to add the boxed Byte values to
     */
    @Override
    public <E> void arrayToCollection(final byte[] x, final Collection<E> output) {
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
