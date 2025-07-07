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
import java.io.StringReader;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.annotation.SuppressFBWarnings;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.exception.UncheckedSQLException;
import com.landawn.abacus.parser.JSONXMLSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Objectory;

@SuppressWarnings("java:S2160")
public class ReaderType extends AbstractType<Reader> {

    public static final String READER = Reader.class.getSimpleName();

    private final Class<Reader> typeClass;

    private final Constructor<?> stringConstructor;

    private final Constructor<?> readerConstructor;

    ReaderType() {
        this(READER);
    }

    ReaderType(final String typeName) {
        super(typeName);

        typeClass = Reader.class;

        stringConstructor = null;
        readerConstructor = null;
    }

    ReaderType(final Class<Reader> cls) {
        super(ClassUtil.getSimpleClassName(cls));

        typeClass = cls;

        if (Modifier.isAbstract(cls.getModifiers())) {
            stringConstructor = null;
            readerConstructor = null;
        } else {
            stringConstructor = ClassUtil.getDeclaredConstructor(cls, String.class);
            readerConstructor = ClassUtil.getDeclaredConstructor(cls, Reader.class);
        }
    }

    /**
     * Returns the Class object representing the Reader type or its subclass.
     *
     * @return the Class object for Reader.class or the specific Reader subclass
     */
    @Override
    public Class<Reader> clazz() {
        return typeClass;
    }

    /**
     * Indicates whether this type represents a Reader.
     * For ReaderType, this always returns true.
     *
     * @return true, indicating this is a Reader type
     */
    @Override
    public boolean isReader() {
        return true;
    }

    /**
     * Converts a Reader to its string representation by reading all content from the Reader.
     * The Reader is fully consumed and closed after this operation.
     *
     * @param x the Reader to convert to string
     * @return the string containing all content read from the Reader, or null if the input is null
     * @throws UncheckedIOException if an I/O error occurs while reading from the Reader
     */
    @Override
    public String stringOf(final Reader x) {
        return x == null ? null : IOUtil.readAllToString(x);
    }

    /**
     * Creates a Reader instance from a string value.
     * If the concrete Reader class has a constructor accepting String or Reader,
     * it will be used. Otherwise, a StringReader is returned.
     *
     * @param str the string to create a Reader from
     * @return a Reader containing the string content, or null if the input string is null
     * @throws RuntimeException if the constructor invocation fails
     */
    @MayReturnNull
    @Override
    public Reader valueOf(final String str) {
        if (str == null) {
            return null; // NOSONAR
        }

        if (stringConstructor != null) {
            return (Reader) ClassUtil.invokeConstructor(stringConstructor, str);
        } else if (readerConstructor != null) {
            return (Reader) ClassUtil.invokeConstructor(readerConstructor, new StringReader(str));
        } else {
            return new StringReader(str);
        }
    }

    /**
     * Creates a Reader from various object types.
     * If the object is a Clob, its character stream is returned.
     * Otherwise, the object is converted to string and then to a Reader.
     *
     * @param obj the object to convert to a Reader
     * @return a Reader representation of the object, or null if the input is null
     * @throws UncheckedSQLException if accessing the Clob's character stream fails
     */
    @MayReturnNull
    @SuppressFBWarnings
    @Override
    public Reader valueOf(final Object obj) {
        if (obj == null) {
            return null; // NOSONAR
        } else if (obj instanceof Clob clob) {
            try {
                return clob.getCharacterStream();
            } catch (final SQLException e) {
                throw new UncheckedSQLException(e);
            }
        } else {
            return valueOf(N.typeOf(obj.getClass()).stringOf(obj));
        }
    }

    /**
     * Retrieves a character stream (Reader) from the specified column in the ResultSet.
     *
     * @param rs the ResultSet to read from
     * @param columnIndex the 1-based index of the column to retrieve
     * @return the Reader for the specified column, or null if the column value is SQL NULL
     * @throws SQLException if a database access error occurs or the column index is invalid
     */
    @Override
    public Reader get(final ResultSet rs, final int columnIndex) throws SQLException {
        return rs.getCharacterStream(columnIndex);
    }

    /**
     * Retrieves a character stream (Reader) from the specified column in the ResultSet.
     *
     * @param rs the ResultSet to read from
     * @param columnLabel the label of the column to retrieve (column name or alias)
     * @return the Reader for the specified column, or null if the column value is SQL NULL
     * @throws SQLException if a database access error occurs or the column label is not found
     */
    @Override
    public Reader get(final ResultSet rs, final String columnLabel) throws SQLException {
        return rs.getCharacterStream(columnLabel);
    }

    /**
     * Sets a Reader parameter in a PreparedStatement.
     * The Reader will be used to provide character stream data to the database.
     *
     * @param stmt the PreparedStatement to set the parameter on
     * @param columnIndex the 1-based index of the parameter to set
     * @param x the Reader to set as the parameter value
     * @throws SQLException if a database access error occurs or the parameter index is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final Reader x) throws SQLException {
        stmt.setCharacterStream(columnIndex, x);
    }

    /**
     * Sets a Reader parameter in a CallableStatement.
     * The Reader will be used to provide character stream data to the database.
     *
     * @param stmt the CallableStatement to set the parameter on
     * @param parameterName the name of the parameter to set
     * @param x the Reader to set as the parameter value
     * @throws SQLException if a database access error occurs or the parameter name is not found
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final Reader x) throws SQLException {
        stmt.setCharacterStream(parameterName, x);
    }

    /**
     * Sets a Reader parameter in a PreparedStatement with a specified length.
     * The Reader will be used to provide character stream data to the database.
     *
     * @param stmt the PreparedStatement to set the parameter on
     * @param columnIndex the 1-based index of the parameter to set
     * @param x the Reader to set as the parameter value
     * @param sqlTypeOrLength the length of the stream in characters
     * @throws SQLException if a database access error occurs or the parameter index is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final Reader x, final int sqlTypeOrLength) throws SQLException {
        stmt.setCharacterStream(columnIndex, x, sqlTypeOrLength);
    }

    /**
     * Sets a Reader parameter in a CallableStatement with a specified length.
     * The Reader will be used to provide character stream data to the database.
     *
     * @param stmt the CallableStatement to set the parameter on
     * @param parameterName the name of the parameter to set
     * @param x the Reader to set as the parameter value
     * @param sqlTypeOrLength the length of the stream in characters
     * @throws SQLException if a database access error occurs or the parameter name is not found
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final Reader x, final int sqlTypeOrLength) throws SQLException {
        stmt.setCharacterStream(parameterName, x, sqlTypeOrLength);
    }

    /**
     * Appends the content of a Reader to the given Appendable.
     * If the Appendable is a Writer, the content is copied directly.
     * Otherwise, the Reader content is read as a string and appended.
     * The Reader is fully consumed after this operation.
     *
     * @param appendable the Appendable to write to (e.g., StringBuilder, Writer)
     * @param x the Reader whose content should be appended
     * @throws IOException if an I/O error occurs during the append operation
     */
    @Override
    public void appendTo(final Appendable appendable, final Reader x) throws IOException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else {
            if (appendable instanceof Writer writer) {
                IOUtil.write(x, writer);
            } else {
                appendable.append(IOUtil.readAllToString(x));
            }
        }
    }

    /**
     * Writes the content of a Reader to the given CharacterWriter.
     * The content is optionally quoted based on the serialization configuration.
     * The Reader is fully consumed after this operation.
     *
     * @param writer the CharacterWriter to write to
     * @param x the Reader whose content should be written
     * @param config the serialization configuration that determines string quotation
     * @throws IOException if an I/O error occurs during the write operation
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, final Reader x, final JSONXMLSerializationConfig<?> config) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            if ((config != null) && (config.getStringQuotation() != 0)) {
                writer.write(config.getStringQuotation());
            }

            final char[] buf = Objectory.createCharArrayBuffer();

            try {
                int count = 0;

                while (IOUtil.EOF != (count = IOUtil.read(x, buf, 0, buf.length))) {
                    writer.writeCharacter(buf, 0, count);
                }
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            } finally {
                Objectory.recycle(buf);
            }

            if ((config != null) && (config.getStringQuotation() != 0)) {
                writer.write(config.getStringQuotation());
            }
        }
    }
}