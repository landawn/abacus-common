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

public class SQLXMLType extends AbstractType<SQLXML> {

    public static final String SQL_XML = SQLXML.class.getSimpleName();

    SQLXMLType() {
        super(SQL_XML);
    }

    /**
     * Returns the Class object representing the SQL XML type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<SQLXML> type = TypeFactory.getType(SQLXML.class);
     * Class<SQLXML> clazz = type.clazz();   // Returns SQLXML.class
     * }</pre>
     *
     * @return the Class object for java.sql.SQLXML.class
     */
    @Override
    public Class<SQLXML> clazz() {
        return SQLXML.class;
    }

    /**
     * Indicates whether this type is serializable.
     * SQL XML types are not serializable as they represent database-specific XML data.
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
     * Converts a SQLXML object to its string representation.
     * This operation is not supported for SQL XML types as they are database-specific
     * and require special handling for XML data extraction.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<SQLXML> type = TypeFactory.getType(SQLXML.class);
     * // This will throw UnsupportedOperationException
     * String str = type.stringOf(sqlXml);
     * }</pre>
     *
     * @param x the SQLXML object to convert
     * @return never returns normally
     * @throws UnsupportedOperationException always thrown as SQLXML cannot be directly converted to string
     */
    @Override
    public String stringOf(final SQLXML x) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
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
        throw new UnsupportedOperationException();
    }

    /**
     * Retrieves a SQL XML value from the specified column in the ResultSet.
     * A SQL XML represents XML data stored in the database.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<SQLXML> type = TypeFactory.getType(SQLXML.class);
     * ResultSet rs = statement.executeQuery("SELECT config_xml FROM settings");
     * SQLXML xml = type.get(rs, 1);   // Get XML from first column
     * }</pre>
     *
     * @param rs the ResultSet to read from
     * @param columnIndex the 1-based index of the column to retrieve
     * @return the SQLXML value from the specified column, or {@code null} if the column value is SQL NULL
     * @throws SQLException if a database access error occurs or the column index is invalid
     */
    @Override
    public SQLXML get(final ResultSet rs, final int columnIndex) throws SQLException {
        return rs.getSQLXML(columnIndex);
    }

    /**
     * Retrieves a SQL XML value from the specified column in the ResultSet.
     * A SQL XML represents XML data stored in the database.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<SQLXML> type = TypeFactory.getType(SQLXML.class);
     * ResultSet rs = statement.executeQuery("SELECT config_xml FROM settings");
     * SQLXML xml = type.get(rs, "config_xml");   // Get XML by column name
     * }</pre>
     *
     * @param rs the ResultSet to read from
     * @param columnLabel the label of the column to retrieve (column name or alias)
     * @return the SQLXML value from the specified column, or {@code null} if the column value is SQL NULL
     * @throws SQLException if a database access error occurs or the column label is not found
     */
    @Override
    public SQLXML get(final ResultSet rs, final String columnLabel) throws SQLException {
        return rs.getSQLXML(columnLabel);
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
     * @param stmt the PreparedStatement to set the parameter on
     * @param columnIndex the 1-based index of the parameter to set
     * @param x the SQLXML value to set as the parameter
     * @throws SQLException if a database access error occurs or the parameter index is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final SQLXML x) throws SQLException {
        stmt.setSQLXML(columnIndex, x);
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
     * @param stmt the CallableStatement to set the parameter on
     * @param parameterName the name of the parameter to set
     * @param x the SQLXML value to set as the parameter
     * @throws SQLException if a database access error occurs or the parameter name is not found
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final SQLXML x) throws SQLException {
        stmt.setSQLXML(parameterName, x);
    }
}
