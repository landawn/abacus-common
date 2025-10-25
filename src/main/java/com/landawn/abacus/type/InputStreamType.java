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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.annotation.SuppressFBWarnings;
import com.landawn.abacus.exception.UncheckedSQLException;
import com.landawn.abacus.parser.JSONXMLSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.N;

/**
 * Type handler for InputStream and its subclasses.
 * This class provides serialization, deserialization, and database access capabilities for InputStream instances.
 * InputStreams are serialized as base64-encoded strings for text-based storage and transmission.
 * Note that the InputStream content is consumed during serialization.
 */
@SuppressWarnings("java:S2160")
public class InputStreamType extends AbstractType<InputStream> {

    public static final String INPUT_STREAM = InputStream.class.getSimpleName();

    private final Class<InputStream> typeClass;

    private final Constructor<?> bytesConstructor;

    private final Constructor<?> streamConstructor;

    InputStreamType() {
        this(INPUT_STREAM);
    }

    InputStreamType(final String typeName) {
        super(typeName);

        typeClass = InputStream.class;

        bytesConstructor = null;
        streamConstructor = null;
    }

    InputStreamType(final Class<InputStream> cls) {
        super(ClassUtil.getSimpleClassName(cls));

        typeClass = cls;

        if (Modifier.isAbstract(cls.getModifiers())) {
            bytesConstructor = null;
            streamConstructor = null;
        } else {
            bytesConstructor = ClassUtil.getDeclaredConstructor(cls, byte[].class);
            streamConstructor = ClassUtil.getDeclaredConstructor(cls, InputStream.class);
        }
    }

    /**
     * Returns the Class object representing the InputStream type handled by this type handler.
     *
     * @return the Class object for InputStream or its subclass
     */
    @Override
    public Class<InputStream> clazz() {
        return typeClass;
    }

    /**
     * Indicates whether this type represents an InputStream.
     *
     * @return true, as this type handler specifically handles InputStreams
     */
    @Override
    public boolean isInputStream() {
        return true;
    }

    /**
     * Converts an InputStream to its string representation.
     * The stream is read completely and its contents are encoded as a base64 string.
     * Note that this operation consumes the stream.
     *
     * @param x the InputStream to convert to string
     * @return the base64-encoded string representation of the stream contents, or null if the input is null
     */
    @Override
    public String stringOf(final InputStream x) {
        // return x == null ? null : Strings.base64Encode(IOUtil.readAllBytes(x));

        return x == null ? null : IOUtil.readAllToString(x);
    }

    /**
     * Converts a base64-encoded string back to an InputStream instance.
     * Creates the appropriate InputStream subclass based on the configured constructors.
     * If no specific constructors are available, returns a ByteArrayInputStream.
     *
     * @param str the base64-encoded string to decode
     * @return a new InputStream containing the decoded bytes, or null if the input is null
     */
    @MayReturnNull
    @Override
    public InputStream valueOf(final String str) {
        if (str == null) {
            return null; // NOSONAR
        }

        if (bytesConstructor != null) {
            //noinspection PrimitiveArrayArgumentToVarargsMethod
            return (InputStream) ClassUtil.invokeConstructor(bytesConstructor, str.getBytes()); //NOSONAR
        } else if (streamConstructor != null) {
            return (InputStream) ClassUtil.invokeConstructor(streamConstructor, new ByteArrayInputStream(str.getBytes()));
        } else {
            return new ByteArrayInputStream(str.getBytes());
        }
    }

    /**
     * Converts various object types to an InputStream.
     * Handles Blob objects by extracting their binary stream.
     * Other objects are converted to string first, then to InputStream.
     *
     * @param obj the object to convert to InputStream
     * @return an InputStream representation of the object, or null if the input is null
     * @throws UncheckedSQLException if a SQLException occurs while reading from a Blob
     */
    @MayReturnNull
    @SuppressFBWarnings
    @Override
    public InputStream valueOf(final Object obj) {
        if (obj == null) {
            return null; // NOSONAR
        } else if (obj instanceof Blob blob) {
            try {
                return blob.getBinaryStream();
            } catch (final SQLException e) {
                throw new UncheckedSQLException(e);
            }
        } else {
            return valueOf(N.typeOf(obj.getClass()).stringOf(obj));
        }
    }

    /**
     * Retrieves an InputStream from the specified column in a ResultSet.
     * Uses the getBinaryStream method to read binary data as a stream.
     *
     * @param rs the ResultSet to read from
     * @param columnIndex the index of the column to read (1-based)
     * @return the InputStream from the column, or null if the column value is SQL NULL
     * @throws SQLException if a database access error occurs or the columnIndex is invalid
     */
    @Override
    public InputStream get(final ResultSet rs, final int columnIndex) throws SQLException {
        return rs.getBinaryStream(columnIndex);
    }

    /**
     * Retrieves an InputStream from the specified column in a ResultSet using the column label.
     * Uses the getBinaryStream method to read binary data as a stream.
     *
     * @param rs the ResultSet to read from
     * @param columnLabel the label of the column to read
     * @return the InputStream from the column, or null if the column value is SQL NULL
     * @throws SQLException if a database access error occurs or the columnLabel is not found
     */
    @Override
    public InputStream get(final ResultSet rs, final String columnLabel) throws SQLException {
        return rs.getBinaryStream(columnLabel);
    }

    /**
     * Sets an InputStream parameter in a PreparedStatement.
     * The stream will be read when the statement is executed.
     *
     * @param stmt the PreparedStatement to set the parameter on
     * @param columnIndex the index of the parameter to set (1-based)
     * @param x the InputStream to set, or null
     * @throws SQLException if a database access error occurs
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final InputStream x) throws SQLException {
        stmt.setBinaryStream(columnIndex, x);
    }

    /**
     * Sets an InputStream parameter in a CallableStatement using a parameter name.
     * The stream will be read when the statement is executed.
     *
     * @param stmt the CallableStatement to set the parameter on
     * @param parameterName the name of the parameter to set
     * @param x the InputStream to set, or null
     * @throws SQLException if a database access error occurs
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final InputStream x) throws SQLException {
        stmt.setBinaryStream(parameterName, x);
    }

    /**
     * Sets an InputStream parameter in a PreparedStatement with a specified length.
     * The stream will be read when the statement is executed, up to the specified length.
     *
     * @param stmt the PreparedStatement to set the parameter on
     * @param columnIndex the index of the parameter to set (1-based)
     * @param x the InputStream to set, or null
     * @param sqlTypeOrLength the length of the stream in bytes
     * @throws SQLException if a database access error occurs
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final InputStream x, final int sqlTypeOrLength) throws SQLException {
        stmt.setBinaryStream(columnIndex, x, sqlTypeOrLength);
    }

    /**
     * Sets an InputStream parameter in a CallableStatement with a specified length.
     * The stream will be read when the statement is executed, up to the specified length.
     *
     * @param stmt the CallableStatement to set the parameter on
     * @param parameterName the name of the parameter to set
     * @param x the InputStream to set, or null
     * @param sqlTypeOrLength the length of the stream in bytes
     * @throws SQLException if a database access error occurs
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final InputStream x, final int sqlTypeOrLength) throws SQLException {
        stmt.setBinaryStream(parameterName, x, sqlTypeOrLength);
    }

    /**
     * Appends the content of an InputStream to an Appendable.
     * If the Appendable is a Writer, the stream is efficiently copied using character encoding.
     * Otherwise, the entire stream is read into a string first.
     * Note that this operation consumes the stream.
     *
     * @param appendable the Appendable to write to
     * @param x the InputStream to read from
     * @throws IOException if an I/O error occurs during reading or writing
     */
    @Override
    public void appendTo(final Appendable appendable, final InputStream x) throws IOException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else {
            if (appendable instanceof Writer writer) {
                IOUtil.write(new InputStreamReader(x), writer);
            } else {
                appendable.append(IOUtil.readAllToString(x));
            }
        }
    }

    /**
     * Writes the character representation of an InputStream to a CharacterWriter.
     * The stream is read completely and encoded as base64 before writing.
     * Handles quotation marks if specified in the configuration.
     * Note that this operation consumes the stream.
     *
     * @param writer the CharacterWriter to write to
     * @param t the InputStream to write
     * @param config the serialization configuration to use
     * @throws IOException if an I/O error occurs during reading or writing
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, final InputStream t, final JSONXMLSerializationConfig<?> config) throws IOException {
        if (t == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            if ((config == null) || (config.getStringQuotation() == 0)) {
                writer.write(stringOf(t));
            } else {
                writer.write(config.getStringQuotation());
                writer.write(stringOf(t));
                writer.write(config.getStringQuotation());
            }
        }
    }
}