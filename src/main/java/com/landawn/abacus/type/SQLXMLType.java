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
import java.sql.SQLXML;

import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.exception.UncheckedSQLException;

/**
 * Type handler for {@link java.sql.SQLXML} objects. Provides JDBC support for reading SQLXML values
 * from a {@link ResultSet} and binding them to {@link PreparedStatement}/{@link CallableStatement} parameters.
 * {@link #stringOf(SQLXML)} reads the XML text through {@link SQLXML#getString()} and wraps SQL failures
 * in {@link UncheckedSQLException}. {@link #valueOf(String)} remains unsupported, and
 * {@link #isSerializable()} returns {@code false}: extracting text does not recreate a JDBC SQLXML instance.
 * {@link #valueOf(Object)} returns an object that already is a {@link SQLXML} unchanged, maps
 * {@code null} to {@code null}, and rejects everything else without touching it.
 * Callers retain ownership of retrieved values and must invoke {@link SQLXML#free()} when the value is no longer
 * needed; this handler does not release a value passed to or returned from a JDBC operation.
 *
 * <p>{@link TypeFactory#getType(Class)} supports both {@code SQLXML.class} and implementation classes.
 * {@link #javaType()} retains the requested class, while {@link #name()} remains {@code "SQLXML"}.
 * Selecting an implementation class does not construct or convert SQLXML values; JDBC operations
 * transfer the supplied or retrieved instances unchanged.</p>
 */
public class SQLXMLType extends AbstractType<SQLXML> {

    /**
     * The type name identifier for SQLXML type, equal to the simple class name {@code "SQLXML"}.
     */
    public static final String SQL_XML = SQLXML.class.getSimpleName();

    /** The {@link SQLXML} interface or implementation class represented by this handler. */
    private final Class<SQLXML> clazz;

    /**
     * Constructs a handler for the standard SQLXML interface, for use by {@link TypeFactory}.
     */
    SQLXMLType() {
        this(SQLXML.class);
    }

    /**
     * Constructs a handler for a specific SQLXML interface or implementation class.
     * The class is retained as metadata; the handler name remains {@code "SQLXML"}.
     *
     * @param targetClass the SQLXML interface or implementation class represented by this handler
     */
    SQLXMLType(final Class<? extends SQLXML> targetClass) {
        super(SQL_XML);
        this.clazz = (Class<SQLXML>) targetClass;
    }

    /**
     * Returns the SQLXML interface or implementation class selected for this handler.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<SQLXML> type = TypeFactory.getType(SQLXML.class);
     * Class<SQLXML> clazz = type.javaType();   // Returns SQLXML.class
     * Type<SQLXML> implementationType = TypeFactory.getType(sqlXml.getClass());
     * Class<SQLXML> implementationClass = implementationType.javaType();   // Returns sqlXml.getClass()
     * }</pre>
     *
     * @return the SQLXML interface or implementation class represented by this handler
     */
    @Override
    public Class<SQLXML> javaType() {
        return clazz;
    }

    /**
     * Indicates whether this type is serializable.
     * Extracting XML text is supported, but recreating a database-specific SQLXML value from that text is not.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<SQLXML> type = TypeFactory.getType(SQLXML.class);
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
     * Reads the XML text using {@link SQLXML#getString()} without freeing the SQLXML value.
     * The text is returned unchanged; no additional quoting or escaping is applied.
     * The driver determines whether the value is still readable. This operation does not make
     * {@link #valueOf(String)} supported or change {@link #isSerializable()}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<SQLXML> type = TypeFactory.getType(SQLXML.class);
     * String xmlText = type.stringOf(sqlXml);
     * }</pre>
     *
     * @param x the SQLXML value to read; may be {@code null}
     * @return the XML text, or {@code null} if {@code x} is null or the driver returns null
     * @throws UncheckedSQLException if reading the XML text fails; the original SQLException is retained as the cause
     */
    @Override
    public String stringOf(final SQLXML x) throws UncheckedSQLException {
        if (x == null) {
            return null;
        }

        try {
            return x.getString();
        } catch (SQLException e) {
            throw new UncheckedSQLException(e);
        }
    }

    /**
     * Creates a SQLXML object from a string representation.
     * This operation is not supported for SQL XML types as they must be created
     * by the database connection and cannot be instantiated from a string.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<SQLXML> type = TypeFactory.getType(SQLXML.class);
     * // This will throw UnsupportedOperationException
     * SQLXML xml = type.valueOf("<root>data</root>");
     * }</pre>
     *
     * @param str the string to convert
     * @return never returns normally
     * @throws UnsupportedOperationException always thrown as SQLXML cannot be created from string
     */
    @Override
    public SQLXML valueOf(final String str) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("SQLXML cannot be created from string representation");
    }

    /**
     * Returns {@code obj} unchanged if it already is a {@link SQLXML}; otherwise the conversion is not supported.
     * Unlike the inherited default, this method never serializes the value: {@code AbstractType.valueOf(Object)}
     * renders {@code obj} with the handler registered for its own runtime class and feeds that text to
     * {@link #valueOf(String)}, which always throws - so the caller's locator would be read for nothing.
     *
     * @param obj the object to convert; may be {@code null}
     * @return the same {@link SQLXML} instance if {@code obj} is a {@link SQLXML}, or {@code null} if {@code obj} is
     *         {@code null}
     * @throws UnsupportedOperationException if {@code obj} is non-null and not a {@link SQLXML}
     */
    @MayReturnNull
    @Override
    public SQLXML valueOf(final Object obj) throws UnsupportedOperationException {
        if (obj == null) {
            return null; // NOSONAR
        } else if (obj instanceof SQLXML value) {
            return value;
        }

        throw new UnsupportedOperationException("SQLXML cannot be created from " + obj.getClass().getName());
    }

    /**
     * Retrieves a SQL XML value from the specified column in the ResultSet.
     * A SQL XML represents XML data stored in the database.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<SQLXML> type = TypeFactory.getType(SQLXML.class);
     * try (ResultSet rs = statement.executeQuery("SELECT config_xml FROM settings")) {
     *     if (rs.next()) {
     *         SQLXML xml = type.get(rs, 1);   // Get XML from first column
     *     }
     * }
     * }</pre>
     *
     * @param rs the ResultSet to read from
     * @param columnIndex the 1-based index of the column to retrieve
     * @return the SQLXML value from the specified column, or {@code null} if the column value is SQL NULL
     * @throws NullPointerException if {@code rs} is null when the JDBC operation is invoked
     * @throws SQLException if a database access error occurs or the column index is invalid
     */
    @Override
    public SQLXML get(final ResultSet rs, final int columnIndex) throws NullPointerException, SQLException {
        return rs.getSQLXML(columnIndex);
    }

    /**
     * Retrieves a SQL XML value from the specified column in the ResultSet.
     * A SQL XML represents XML data stored in the database.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<SQLXML> type = TypeFactory.getType(SQLXML.class);
     * try (ResultSet rs = statement.executeQuery("SELECT config_xml FROM settings")) {
     *     if (rs.next()) {
     *         SQLXML xml = type.get(rs, "config_xml");   // Get XML by column name
     *     }
     * }
     * }</pre>
     *
     * @param rs the ResultSet to read from
     * @param columnName the label of the column to retrieve (column name or alias)
     * @return the SQLXML value from the specified column, or {@code null} if the column value is SQL NULL
     * @throws NullPointerException if {@code rs} is null when the JDBC operation is invoked
     * @throws SQLException if a database access error occurs or the column label is not found
     */
    @Override
    public SQLXML get(final ResultSet rs, final String columnName) throws NullPointerException, SQLException {
        return rs.getSQLXML(columnName);
    }

    /**
     * Sets a SQLXML parameter in a PreparedStatement.
     * The SQLXML represents XML data to be stored in the database.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<SQLXML> type = TypeFactory.getType(SQLXML.class);
     * PreparedStatement stmt = conn.prepareStatement("INSERT INTO settings (config_xml) VALUES (?)");
     * type.set(stmt, 1, xmlData);   // Set XML at parameter index 1
     * }</pre>
     *
     * @param statement the PreparedStatement to set the parameter on
     * @param columnIndex the 1-based index of the parameter to set
     * @param x the SQLXML value to set as the parameter
     * @throws NullPointerException if {@code statement} is null when the JDBC operation is invoked
     * @throws SQLException if a database access error occurs or the parameter index is invalid
     */
    @Override
    public void set(final PreparedStatement statement, final int columnIndex, final SQLXML x) throws NullPointerException, SQLException {
        statement.setSQLXML(columnIndex, x);
    }

    /**
     * Sets a SQLXML parameter in a CallableStatement.
     * The SQLXML represents XML data to be stored in the database.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<SQLXML> type = TypeFactory.getType(SQLXML.class);
     * CallableStatement stmt = conn.prepareCall("{call update_config(?)}");
     * type.set(stmt, "xml_param", xmlData);   // Set XML by parameter name
     * }</pre>
     *
     * @param statement the CallableStatement to set the parameter on
     * @param parameterName the name of the parameter to set
     * @param x the SQLXML value to set as the parameter
     * @throws NullPointerException if {@code statement} is null when the JDBC operation is invoked
     * @throws SQLException if a database access error occurs or the parameter name is not found
     */
    @Override
    public void set(final CallableStatement statement, final String parameterName, final SQLXML x) throws NullPointerException, SQLException {
        statement.setSQLXML(parameterName, x);
    }
}
