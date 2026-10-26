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

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Struct;

import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.exception.UncheckedSQLException;
import com.landawn.abacus.util.N;

/**
 * Type handler for {@link Struct} values and their driver-specific implementations.
 * SQL structured values are read through {@link ResultSet#getObject(int)} or {@link ResultSet#getObject(String)}
 * and bound through {@link PreparedStatement#setObject(int, Object)} or {@link CallableStatement#setObject(String, Object)}.
 * The result-set value must be a {@code Struct} or SQL {@code NULL}; a connection type map that produces
 * a different Java representation is not converted by this handler.
 *
 * <p>{@link TypeFactory#getType(Class)} supports both {@code Struct.class} and implementation classes.
 * {@link #javaType()} retains the requested class, while {@link #name()} remains {@code "Struct"}.
 * Selecting an implementation class does not construct or convert structured values; JDBC operations
 * transfer the supplied or retrieved instances unchanged.</p>
 *
 * <p>{@link #stringOf(Struct)} reads the attributes with {@link Struct#getAttributes()} and formats them
 * with {@link N#stringOf(Object)}, wrapping SQL failures in {@link UncheckedSQLException}.
 * {@link #isSerializable()} returns {@code false}; the formatted attributes do not recreate the SQL structured value.
 * {@link #valueOf(String)} always throws {@link UnsupportedOperationException}, including for {@code null} arguments.
 * {@link #valueOf(Object)} preserves existing {@code Struct} values, maps {@code null} to {@code null},
 * and rejects other values without reading or serializing them.</p>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * Struct address = connection.createStruct("ADDRESS_TYPE", new Object[] { "Main St", "Springfield" });
 * Type<Struct> type = TypeFactory.getType(address.getClass());
 * Class<Struct> implementationClass = type.javaType();   // Returns address.getClass()
 * type.set(statement, 1, address);
 * Struct storedAddress = type.get(resultSet, "address");
 * }</pre>
 */
public class StructType extends AbstractType<Struct> {

    /** The type name identifier for SQL Struct values. */
    public static final String STRUCT = Struct.class.getSimpleName();

    /** The {@link Struct} interface or implementation class represented by this handler. */
    private final Class<Struct> clazz;

    /**
     * Constructs a handler for the standard Struct interface, for use by {@link TypeFactory}.
     */
    StructType() {
        this(Struct.class);
    }

    /**
     * Constructs a handler for a specific SQL Struct interface or implementation class.
     * The class is retained as metadata; the handler name remains {@code "Struct"}.
     *
     * @param targetClass the Struct interface or implementation class managed by this handler
     */
    StructType(final Class<? extends Struct> targetClass) {
        super(STRUCT);
        this.clazz = (Class<Struct>) targetClass;
    }

    /**
     * Returns the Struct interface or implementation class selected for this handler.
     *
     * @return the Struct interface or implementation class managed by this handler
     */
    @Override
    public Class<Struct> javaType() {
        return clazz;
    }

    /**
     * Indicates that formatted attributes cannot recreate a database-specific SQL structured value.
     *
     * @return {@code false}
     */
    @Override
    public boolean isSerializable() {
        return false;
    }

    /**
     * Reads the attributes with {@link Struct#getAttributes()} and formats them with {@link N#stringOf(Object)}.
     * The attribute array is formatted in its existing order, using the corresponding type handlers for
     * its elements. The SQL type name is not included, and {@link #valueOf(String)} cannot reconstruct the Struct.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Struct> type = TypeFactory.getType(Struct.class);
     * String attributes = type.stringOf(address);   // For attributes {"Main St", 123}: "[\"Main St\", 123]"
     * }</pre>
     *
     * @param x the Struct whose attributes are to be read; may be {@code null}
     * @return the formatted attributes, or {@code null} if {@code x} or the attribute array is null
     * @throws UncheckedSQLException if reading the attributes fails; the original SQLException is retained as the cause
     */
    @Override
    public String stringOf(final Struct x) throws UncheckedSQLException {
        if (x == null) {
            return null;
        }

        try {
            return N.stringOf(x.getAttributes());
        } catch (final SQLException e) {
            throw new UncheckedSQLException(e);
        }
    }

    /**
     * Creating SQL structured values from strings is unsupported.
     *
     * @param str the string to convert; may be {@code null}
     * @return never returns normally
     * @throws UnsupportedOperationException always
     */
    @Override
    public Struct valueOf(final String str) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("Struct cannot be created from string representation");
    }

    /**
     * Preserves existing Struct values without reading their attributes. Non-Struct values are rejected
     * without calling their {@code toString()} method or otherwise serializing them.
     *
     * @param obj the object to convert; may be {@code null}
     * @return the same Struct instance, or {@code null} if the input is {@code null}
     * @throws UnsupportedOperationException if the input is non-null and is not a Struct
     */
    @MayReturnNull
    @Override
    public Struct valueOf(final Object obj) throws UnsupportedOperationException {
        if (obj == null) {
            return null; // NOSONAR
        } else if (obj instanceof Struct value) {
            return value;
        }

        throw new UnsupportedOperationException("Struct cannot be created from " + obj.getClass().getName());
    }

    /**
     * Retrieves a SQL structured value by column index.
     *
     * @param rs the ResultSet to read from
     * @param columnIndex the 1-based column index
     * @return the Struct value, or {@code null} for SQL NULL
     * @throws NullPointerException if {@code rs} is null
     * @throws SQLException if a database access error occurs
     * @throws ClassCastException if the column does not contain a Struct value
     */
    @Override
    public Struct get(final ResultSet rs, final int columnIndex) throws NullPointerException, SQLException {
        return (Struct) rs.getObject(columnIndex);
    }

    /**
     * Retrieves a SQL structured value by column label.
     *
     * @param rs the ResultSet to read from
     * @param columnName the column label
     * @return the Struct value, or {@code null} for SQL NULL
     * @throws NullPointerException if {@code rs} is null
     * @throws SQLException if a database access error occurs
     * @throws ClassCastException if the column does not contain a Struct value
     */
    @Override
    public Struct get(final ResultSet rs, final String columnName) throws NullPointerException, SQLException {
        return (Struct) rs.getObject(columnName);
    }

    /**
     * Binds a SQL structured value by parameter index.
     *
     * @param statement the PreparedStatement to bind
     * @param columnIndex the 1-based parameter index
     * @param x the Struct value; may be {@code null}
     * @throws NullPointerException if {@code statement} is null
     * @throws SQLException if a database access error occurs
     */
    @Override
    public void set(final PreparedStatement statement, final int columnIndex, final Struct x) throws NullPointerException, SQLException {
        statement.setObject(columnIndex, x);
    }

    /**
     * Binds a SQL structured value by parameter name.
     *
     * @param statement the CallableStatement to bind
     * @param parameterName the parameter name
     * @param x the Struct value; may be {@code null}
     * @throws NullPointerException if {@code statement} is null
     * @throws SQLException if a database access error occurs
     */
    @Override
    public void set(final CallableStatement statement, final String parameterName, final Struct x) throws NullPointerException, SQLException {
        statement.setObject(parameterName, x);
    }
}
