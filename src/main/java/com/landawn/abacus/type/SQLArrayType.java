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

import java.sql.Array;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.exception.ParsingException;
import com.landawn.abacus.exception.UncheckedSQLException;
import com.landawn.abacus.util.N;

/**
 * Type handler for {@link java.sql.Array} objects, used primarily for JDBC operations that read
 * SQL ARRAY values from a {@link ResultSet} and bind them to {@link PreparedStatement}/{@link CallableStatement}
 * parameters. {@link #stringOf(Array)} reads the Java array through {@link Array#getArray()} and formats
 * it with {@link N#stringOf(Object)}, wrapping SQL failures in {@link UncheckedSQLException}; an element that is itself
 * a JDBC value such as a {@code Struct} is rejected with {@link ParsingException} (see {@link #stringOf(Array)}).
 * {@link #valueOf(String)} remains unsupported and {@link #isSerializable()} returns {@code false}:
 * the formatted contents do not recreate a database-specific SQL Array. {@link #valueOf(Object)} returns an object that already is an
 * {@link Array} unchanged, maps {@code null} to {@code null}, and rejects everything else without touching it.
 *
 * <p>{@link TypeFactory#getType(Class)} supports both {@code Array.class} and implementation classes.
 * {@link #javaType()} retains the requested class, while {@link #name()} remains {@code "SQLArray"}.
 * Selecting an implementation class does not construct or convert array values; JDBC operations
 * transfer the supplied or retrieved instances unchanged.</p>
 *
 * <p>Database-backed {@link Array} instances may depend on the active connection. Callers are
 * responsible for invoking {@link Array#free()} when such an array is no longer needed; this type
 * handler does not manage the array lifecycle.
 */
public class SQLArrayType extends AbstractType<Array> {

    /**
     * The type name identifier for SQL Array type, equal to the constant string {@code "SQLArray"}.
     */
    static final String SQL_ARRAY = "SQLArray";

    /** The {@link Array} interface or implementation class represented by this handler. */
    private final Class<Array> clazz;

    /**
     * Constructs a handler for the standard Array interface, for use by {@link TypeFactory}.
     */
    SQLArrayType() {
        this(Array.class);
    }

    /**
     * Constructs a handler for a specific SQL Array interface or implementation class.
     * The class is retained as metadata; the handler name remains {@value #SQL_ARRAY}.
     *
     * @param targetClass the Array interface or implementation class managed by this handler
     */
    SQLArrayType(final Class<? extends Array> targetClass) {
        super(SQL_ARRAY);
        this.clazz = (Class<Array>) targetClass;
    }

    /**
     * Returns the Array interface or implementation class selected for this handler.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Array> type = TypeFactory.getType(Array.class);
     * Class<Array> clazz = type.javaType();   // Returns Array.class
     * Type<Array> implementationType = TypeFactory.getType(sqlArray.getClass());
     * Class<Array> implementationClass = implementationType.javaType();   // Returns sqlArray.getClass()
     * }</pre>
     *
     * @return the Array interface or implementation class managed by this handler
     */
    @Override
    public Class<Array> javaType() {
        return clazz;
    }

    /**
     * Indicates whether this type is serializable.
     * Formatting the array contents is supported, but recreating a database-specific SQL Array from that text is not.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Array> type = TypeFactory.getType(Array.class);
     * boolean serializable = type.isSerializable();   // Returns false
     * }</pre>
     *
     * @return {@code false}, indicating this type is not serializable
     */
    @Override
    public boolean isSerializable() {
        return false;
    }

    /**
     * Reads the Java array with {@link Array#getArray()} and formats it with {@link N#stringOf(Object)}.
     * Primitive and object arrays use their corresponding type handlers: an element with a serializable type handler
     * (a string, number, date, {@code Clob} and the like) is written by that handler, and a nested Java array, collection,
     * map or bean element is written element by element in the same way by the JSON serializer. Nested JDBC values are
     * not formatted: an element, or a value inside one, that is a {@link java.sql.Struct}, {@code Array}, {@link java.sql.Ref},
     * {@link java.sql.SQLXML} or {@link java.sql.RowId} makes this method throw {@link ParsingException} (also for a typed
     * payload such as {@code Struct[]}), although the handler of such a value formats it when it is passed directly; a
     * {@link java.sql.Blob} or any other value the JSON serializer does not support is rejected the same way. The result
     * contains the array contents, not the SQL type metadata, and cannot be converted back through
     * {@link #valueOf(String)}. This method does not free the SQL Array.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Array> type = TypeFactory.getType(Array.class);
     * String contents = type.stringOf(sqlArray);   // For an int[] payload {1, 2}: "[1, 2]"
     * type.stringOf(structArray);   // For a Struct[] payload: throws ParsingException (nested Struct)
     * }</pre>
     *
     * @param x the SQL Array to read; may be {@code null}
     * @return the formatted Java array, or {@code null} if {@code x} or the retrieved array is null
     * @throws UncheckedSQLException if reading the array fails; the original SQLException is retained as the cause
     * @throws ParsingException if a nested value cannot be formatted: an element, or a value inside one, that is a
     *         Struct, Array, Ref, SQLXML, RowId or Blob, or any other value the JSON serializer does not support
     */
    @Override
    public String stringOf(final Array x) throws UncheckedSQLException, ParsingException {
        if (x == null) {
            return null;
        }

        try {
            return N.stringOf(x.getArray());
        } catch (SQLException e) {
            throw new UncheckedSQLException(e);
        }
    }

    /**
     * Creates an Array object from a string representation.
     * This operation is not supported for SQL Array types as they are database-specific
     * and cannot be created from a string representation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Array> type = TypeFactory.getType(Array.class);
     * // This will throw UnsupportedOperationException
     * Array array = type.valueOf("[1, 2, 3]");
     * }</pre>
     *
     * @param str the string to convert
     * @return never returns normally
     * @throws UnsupportedOperationException always thrown as Array cannot be created from string
     */
    @Override
    public Array valueOf(final String str) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("SQL Array cannot be created from string representation");
    }

    /**
     * Returns {@code obj} unchanged if it already is an {@link Array}; otherwise the conversion is not supported.
     * Unlike the inherited default, this method never serializes the value: {@code AbstractType.valueOf(Object)}
     * renders {@code obj} with the handler registered for its own runtime class and feeds that text to
     * {@link #valueOf(String)}, which always throws - so the caller's locator would be read for nothing.
     *
     * @param obj the object to convert; may be {@code null}
     * @return the same {@link Array} instance if {@code obj} is an {@link Array}, or {@code null} if {@code obj} is
     *         {@code null}
     * @throws UnsupportedOperationException if {@code obj} is non-null and not an {@link Array}
     */
    @MayReturnNull
    @Override
    public Array valueOf(final Object obj) throws UnsupportedOperationException {
        if (obj == null) {
            return null; // NOSONAR
        } else if (obj instanceof Array value) {
            return value;
        }

        throw new UnsupportedOperationException("SQL Array cannot be created from " + obj.getClass().getName());
    }

    /**
     * Retrieves a SQL ARRAY value from the specified column in the ResultSet.
     * A SQL ARRAY represents an array value in the database.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Array> type = TypeFactory.getType(Array.class);
     * try (ResultSet rs = statement.executeQuery("SELECT tags FROM products")) {
     *     if (rs.next()) {
     *         Array tags = type.get(rs, 1);   // Get array from first column
     *     }
     * }
     * }</pre>
     *
     * @param rs the ResultSet to read from
     * @param columnIndex the 1-based index of the column to retrieve
     * @return the Array value from the specified column, or {@code null} if the column value is SQL NULL
     * @throws NullPointerException if {@code rs} is null when the JDBC operation is invoked
     * @throws SQLException if a database access error occurs or the column index is invalid
     */
    @Override
    public Array get(final ResultSet rs, final int columnIndex) throws NullPointerException, SQLException {
        return rs.getArray(columnIndex);
    }

    /**
     * Retrieves a SQL ARRAY value from the specified column in the ResultSet.
     * A SQL ARRAY represents an array value in the database.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Array> type = TypeFactory.getType(Array.class);
     * try (ResultSet rs = statement.executeQuery("SELECT tags FROM products")) {
     *     if (rs.next()) {
     *         Array tags = type.get(rs, "tags");   // Get array by column name
     *     }
     * }
     * }</pre>
     *
     * @param rs the ResultSet to read from
     * @param columnName the label of the column to retrieve (column name or alias)
     * @return the Array value from the specified column, or {@code null} if the column value is SQL NULL
     * @throws NullPointerException if {@code rs} is null when the JDBC operation is invoked
     * @throws SQLException if a database access error occurs or the column label is not found
     */
    @Override
    public Array get(final ResultSet rs, final String columnName) throws NullPointerException, SQLException {
        return rs.getArray(columnName);
    }

    /**
     * Sets an Array parameter in a PreparedStatement.
     * The Array represents a SQL ARRAY value.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Array> type = TypeFactory.getType(Array.class);
     * PreparedStatement stmt = conn.prepareStatement("INSERT INTO products (tags) VALUES (?)");
     * type.set(stmt, 1, tagsArray);   // Set array at parameter index 1
     * }</pre>
     *
     * @param statement the PreparedStatement to set the parameter on
     * @param columnIndex the 1-based index of the parameter to set
     * @param x the Array value to set as the parameter
     * @throws NullPointerException if {@code statement} is null when the JDBC operation is invoked
     * @throws SQLException if a database access error occurs or the parameter index is invalid
     */
    @Override
    public void set(final PreparedStatement statement, final int columnIndex, final Array x) throws NullPointerException, SQLException {
        statement.setArray(columnIndex, x);
    }

    /**
     * Sets an Array parameter in a CallableStatement.
     * The Array represents a SQL ARRAY value.
     * This method uses {@link CallableStatement#setObject(String, Object)} because JDBC provides
     * {@code setArray} only for positional parameters, not parameter names.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Array> type = TypeFactory.getType(Array.class);
     * CallableStatement stmt = conn.prepareCall("{call update_product_tags(?)}");
     * type.set(stmt, "tags_param", tagsArray);   // Set array by parameter name
     * }</pre>
     *
     * @param statement the CallableStatement to set the parameter on
     * @param parameterName the name of the parameter to set
     * @param x the Array value to set as the parameter
     * @throws NullPointerException if {@code statement} is null when the JDBC operation is invoked
     * @throws SQLException if a database access error occurs or the parameter name is not found
     */
    @Override
    public void set(final CallableStatement statement, final String parameterName, final Array x) throws NullPointerException, SQLException {
        // stmt.setArray(parameterName, x);

        statement.setObject(parameterName, x);
    }
}
