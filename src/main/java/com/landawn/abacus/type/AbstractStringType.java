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
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

import com.landawn.abacus.annotation.SuppressFBWarnings;
import com.landawn.abacus.exception.UncheckedSQLException;
import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.Strings;

/**
 * The Abstract base class for {@code String} type handling in the type system.
 * <p>
 * This class provides the foundation for {@code String} serialization/deserialization,
 * database operations, and character-based I/O operations.
 * </p>
 */
public abstract class AbstractStringType extends AbstractCharSequenceType<String> {

    /**
     * Constructs an {@code AbstractStringType} with the specified type name.
     *
     * @param typeName the name of the string type (e.g., "String")
     */
    protected AbstractStringType(final String typeName) {
        super(typeName);
    }

    /**
     * Returns the {@code Class} object representing the {@code String} class.
     *
     * <p>Usage Examples:</p>
     * <pre>{@code
     * Type<String> type = TypeFactory.getType(String.class);
     * Class<?> clazz = type.javaType();   // returns String.class
     * }</pre>
     *
     * @return the {@code Class} object for {@code String.class}
     */
    @Override
    public Class<String> javaType() {
        return String.class;
    }

    /**
     * Determines whether this type represents a {@code String} type.
     * <p>
     * Always returns {@code true} for {@code AbstractStringType} implementations.
     * </p>
     *
     * <p>Usage Examples:</p>
     * <pre>{@code
     * Type<String> type = TypeFactory.getType(String.class);
     * boolean isString = type.isString();   // returns true
     * }</pre>
     *
     * @return {@code true} indicating this is a string type
     */
    @Override
    public boolean isString() {
        return true;
    }

    /**
     * Converts a {@code String} value to its string representation.
     * <p>
     * Since the input is already a {@code String}, this method simply returns the input value unchanged.
     * </p>
     *
     * <p>Usage Examples:</p>
     * <pre>{@code
     * Type<String> type = TypeFactory.getType(String.class);
     * String result = type.stringOf("hello");    // returns "hello"
     * String nullResult = type.stringOf(null);   // returns null
     * }</pre>
     *
     * @param str the {@code String} value to convert, may be {@code null}
     * @return the same {@code String} value passed as input, or {@code null} if input is {@code null}
     */
    @Override
    public String stringOf(final String str) {
        return str;
    }

    /**
     * Converts a string representation to a {@code String} value.
     * <p>
     * Since the input is already a {@code String}, this method simply returns the input value unchanged.
     * </p>
     *
     * <p>Usage Examples:</p>
     * <pre>{@code
     * Type<String> type = TypeFactory.getType(String.class);
     * String result = type.valueOf("hello");    // returns "hello"
     * String nullResult = type.valueOf(null);   // returns null
     * }</pre>
     *
     * @param str the string representation to convert, may be {@code null}
     * @return the same {@code String} value passed as input, or {@code null} if input is {@code null}
     */
    @Override
    public String valueOf(final String str) {
        return str;
    }

    /**
     * Creates a {@code String} from a character array subset.
     * <p>
     * Constructs a new {@code String} from the specified subset of the character array.
     * </p>
     *
     * <p>Usage Examples:</p>
     * <pre>{@code
     * Type<String> type = TypeFactory.getType(String.class);
     * char[] chars = {'h', 'e', 'l', 'l', 'o'};
     * String result = type.valueOf(chars, 0, 5);    // returns "hello"
     * String partial = type.valueOf(chars, 0, 3);   // returns "hel"
     * }</pre>
     *
     * @param cbuf the character array containing the characters to convert, may be {@code null}
     * @param offset the starting position in the character array (0-based)
     * @param len the number of characters to include
     * @return a new {@code String} created from the specified characters, or {@code null} if {@code cbuf} is {@code null},
     *         or an empty string if {@code cbuf} is empty or {@code len} is 0
     */
    @Override
    public String valueOf(final char[] cbuf, final int offset, final int len) {
        return cbuf == null ? null : ((cbuf.length == 0 || len == 0) ? Strings.EMPTY : String.valueOf(cbuf, offset, len));
    }

    /**
     * Converts an {@code Object} to a {@code String} value.
     * <p>
     * This method handles special cases including {@code Clob} objects and uses type-specific
     * string conversion for other object types.
     * </p>
     *
     * <p>Usage Examples:</p>
     * <pre>{@code
     * Type<String> type = TypeFactory.getType(String.class);
     * String result1 = type.valueOf(Integer.valueOf(123));   // returns "123"
     * String result2 = type.valueOf("hello");                // returns "hello"
     * String nullResult = type.valueOf(null);                // returns null
     * }</pre>
     *
     * @param obj the object to convert to {@code String}, may be {@code null}
     * @return the {@code String} representation of the object, or {@code null} if {@code obj} is {@code null}.
     *         For {@code Clob} objects, extracts and returns the character data.
     *         For {@code Reader} objects, reads all content and returns as {@code String}.
     *         For other objects, uses their type-specific string conversion.
     * @throws UncheckedSQLException if there's an error reading from a {@code Clob} or freeing {@code Clob} resources
     */
    @SuppressFBWarnings
    @Override
    public String valueOf(final Object obj) {
        if (obj == null) {
            return null; // NOSONAR
        } else if (obj instanceof Reader reader) {
            return IOUtil.readAllToString(reader);
        } else if (obj instanceof Clob clob) {
            try {
                final long len = clob.length();
                if (len > Integer.MAX_VALUE) {
                    throw new UnsupportedOperationException("Clob too large to convert to String: " + len + " characters");
                }
                return clob.getSubString(1, (int) len);
            } catch (final SQLException e) {
                throw new UncheckedSQLException(e);
            } finally {
                try {
                    clob.free();
                } catch (final SQLException e) {
                    // Log and ignore - don't mask the original exception
                    // Freeing resources should not override the main exception
                }
            }
        } else {
            return valueOf(Type.<Object> of(obj.getClass()).stringOf(obj));
        }
    }

    /**
     * Retrieves a {@code String} value from a {@code ResultSet} at the specified column index.
     *
     * <p>Usage Examples:</p>
     * <pre>{@code
     * Type<String> type = TypeFactory.getType(String.class);
     * ResultSet rs = org.mockito.Mockito.mock(ResultSet.class);
     * String name = type.get(rs, 1);   // retrieves String from column 1
     * }</pre>
     *
     * @param rs the {@code ResultSet} to retrieve the value from, must not be {@code null}
     * @param columnIndex the column index (1-based) of the value to retrieve
     * @return the {@code String} value at the specified column, or {@code null} if the value is SQL {@code NULL}
     * @throws SQLException if a database access error occurs or the {@code columnIndex} is invalid
     */
    @Override
    public String get(final ResultSet rs, final int columnIndex) throws SQLException {
        return rs.getString(columnIndex);
    }

    /**
     * Retrieves a {@code String} value from a {@code ResultSet} using the specified column label.
     *
     * <p>Usage Examples:</p>
     * <pre>{@code
     * Type<String> type = TypeFactory.getType(String.class);
     * ResultSet rs = org.mockito.Mockito.mock(ResultSet.class);
     * String name = type.get(rs, "name");   // retrieves String from "name" column
     * }</pre>
     *
     * @param rs the {@code ResultSet} to retrieve the value from, must not be {@code null}
     * @param columnName the label for the column specified with the SQL AS clause,
     *                    or the column name if no AS clause was specified, must not be {@code null}
     * @return the {@code String} value in the specified column, or {@code null} if the value is SQL {@code NULL}
     * @throws SQLException if a database access error occurs or the {@code columnName} is invalid
     */
    @Override
    public String get(final ResultSet rs, final String columnName) throws SQLException {
        return rs.getString(columnName);
    }

    /**
     * Sets a {@code String} parameter in a {@code PreparedStatement} at the specified position.
     *
     * <p>Usage Examples:</p>
     * <pre>{@code
     * Type<String> type = TypeFactory.getType(String.class);
     * PreparedStatement stmt = conn.prepareStatement("UPDATE users SET name = ? WHERE id = ?");
     * type.set(stmt, 1, "John Doe");   // sets parameter 1 to "John Doe"
     * }</pre>
     *
     * @param stmt the {@code PreparedStatement} to set the parameter on, must not be {@code null}
     * @param columnIndex the parameter index (1-based) to set
     * @param x the {@code String} value to set, may be {@code null}
     * @throws SQLException if a database access error occurs or the {@code columnIndex} is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final String x) throws SQLException {
        stmt.setString(columnIndex, x);
    }

    /**
     * Sets a named {@code String} parameter in a {@code CallableStatement}.
     *
     * <p>Usage Examples:</p>
     * <pre>{@code
     * Type<String> type = TypeFactory.getType(String.class);
     * CallableStatement stmt = conn.prepareCall("{call updateUser(?, ?)}");
     * type.set(stmt, "name", "John Doe");   // sets named parameter "name"
     * }</pre>
     *
     * @param stmt the {@code CallableStatement} to set the parameter on, must not be {@code null}
     * @param parameterName the name of the parameter to set, must not be {@code null}
     * @param x the {@code String} value to set, may be {@code null}
     * @throws SQLException if a database access error occurs or the parameter name is invalid
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final String x) throws SQLException {
        stmt.setString(parameterName, x);
    }

    /**
     * Appends a {@code String} value to an {@code Appendable} object.
     * <p>
     * If the {@code String} is {@code null}, appends the string "null" instead.
     * </p>
     *
     * <p>Usage Examples:</p>
     * <pre>{@code
     * Type<String> type = TypeFactory.getType(String.class);
     * StringBuilder sb = new StringBuilder();
     * type.appendTo(sb, "hello");   // appends "hello" to StringBuilder
     * type.appendTo(sb, null);      // appends "null" to StringBuilder
     * }</pre>
     *
     * @param appendable the {@code Appendable} object to append to, must not be {@code null}
     * @param x the {@code String} value to append, may be {@code null}
     * @throws IOException if an I/O error occurs during the append operation
     */
    @Override
    public void appendTo(final Appendable appendable, final String x) throws IOException {
        appendable.append(Objects.requireNonNullElse(x, NULL_STRING));
    }

    /**
     * Writes a {@code String} value to a {@code CharacterWriter} with optional quotation based on configuration.
     * <p>
     * This method handles {@code null} values and applies string quotation marks if specified in the configuration.
     * </p>
     *
     * <p>Usage Examples:</p>
     * <pre>{@code
     * Type<String> type = TypeFactory.getType(String.class);
     * CharacterWriter writer = new CharacterWriter();
     * type.writeCharacter(writer, "hello", null);   // writes "hello"
     * type.writeCharacter(writer, null, null);      // writes "null"
     * }</pre>
     *
     * @param writer the {@code CharacterWriter} to write to, must not be {@code null}
     * @param x the {@code String} value to write, may be {@code null}
     * @param config the serialization configuration that may specify string quotation preferences
     *               and {@code null} string handling options, may be {@code null}
     * @throws IOException if an I/O error occurs during the write operation
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, String x, final JsonXmlSerConfig<?> config) throws IOException {
        x = x == null && config != null && config.isWriteNullStringAsEmpty() ? Strings.EMPTY : x;

        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            final char ch = config == null ? 0 : config.getStringQuotation();

            if (ch == 0) {
                writer.writeCharacter(x);
            } else {
                writer.write(ch);
                writer.writeCharacter(x);
                writer.write(ch);
            }
        }
    }
}
