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
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.RowId;
import java.sql.SQLException;

import com.landawn.abacus.parser.JsonXmlSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;

/**
 * Type handler for java.sql.RowId objects.
 * A SQL ROWID is a unique identifier for a row in a database table, representing the physical
 * address of the row. RowId values are database-specific and typically used for efficient
 * row identification and retrieval.
 *
 * <p>This type handler provides limited functionality because RowId objects are database-specific
 * identifiers that cannot be reliably reconstructed from strings across different database sessions
 * or database restarts. The primary use cases are for reading RowId values from ResultSets and
 * setting them in PreparedStatements or CallableStatements.
 *
 * <p><b>Important Notes:</b></p>
 * <ul>
 *   <li>This type is NOT serializable (isSerializable() returns false)</li>
 *   <li>valueOf(String) operation is not supported and will throw UnsupportedOperationException</li>
 *   <li>stringOf() is supported for display purposes but the result cannot be converted back to RowId</li>
 *   <li>RowId values are typically only valid within a single database session</li>
 *   <li>RowId lifetime and validity are database-specific</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * Type<RowId> type = TypeFactory.getType(RowId.class);
 *
 * // Reading a RowId from database (Oracle example)
 * try (ResultSet rs = stmt.executeQuery("SELECT ROWID, name FROM employees WHERE id = 1")) {
 *     if (rs.next()) {
 *         RowId rowId = type.get(rs, 1);
 *         String name = rs.getString(2);
 *
 *         // Use RowId for quick row access
 *         PreparedStatement updateStmt = conn.prepareStatement("UPDATE employees SET name = ? WHERE ROWID = ?");
 *         updateStmt.setString(1, "New Name");
 *         type.set(updateStmt, 2, rowId);
 *         updateStmt.executeUpdate();
 *     }
 * }
 *
 * // Converting to string for logging (one-way conversion)
 * String rowIdStr = type.stringOf(rowId);   // OK for display
 * System.out.println("Row ID: " + rowIdStr);
 *
 * // Note: This operation is NOT supported
 * // RowId restored = type.valueOf(rowIdStr);   // Throws UnsupportedOperationException
 * }</pre>
 */
public class RowIdType extends AbstractType<RowId> {

    public static final String ROW_ID = RowId.class.getSimpleName();

    /**
     * Constructs a new RowIdType instance.
     * This constructor is package-private and intended to be called only by the TypeFactory.
     */
    RowIdType() {
        super(ROW_ID);
    }

    /**
     * Returns the Class object representing the SQL RowId type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<RowId> type = TypeFactory.getType(RowId.class);
     * Class<RowId> clazz = type.clazz();   // Returns RowId.class
     * }</pre>
     *
     * @return the Class object for java.sql.RowId.class
     */
    @Override
    public Class<RowId> clazz() {
        return RowId.class;
    }

    /**
     * Indicates whether this type is serializable.
     * SQL RowId types are not serializable as they represent database-specific row identifiers.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<RowId> type = TypeFactory.getType(RowId.class);
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
     * Converts a RowId object to its string representation.
     * The string representation is obtained by calling toString() on the RowId object.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<RowId> type = TypeFactory.getType(RowId.class);
     * RowId rowId = resultSet.getRowId(1);
     * String str = type.stringOf(rowId);   // Converts RowId to String
     * }</pre>
     *
     * @param x the RowId object to convert
     * @return the string representation of the RowId, or {@code null} if the input is null
     */
    @Override
    public String stringOf(final RowId x) {
        return x == null ? null : x.toString();
    }

    /**
     * Creates a RowId object from a string representation.
     * This operation is not supported for SQL RowId types as they are database-specific
     * and cannot be reliably reconstructed from a string.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<RowId> type = TypeFactory.getType(RowId.class);
     * // This will throw UnsupportedOperationException
     * RowId rowId = type.valueOf("some-string");
     * }</pre>
     *
     * @param str the string to convert
     * @return never returns normally
     * @throws UnsupportedOperationException always thrown as RowId cannot be created from string
     */
    @Override
    public RowId valueOf(final String str) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("RowId cannot be created from string representation");
    }

    /**
     * Retrieves a SQL ROWID value from the specified column in the ResultSet.
     * A ROWID is a unique identifier for a row in a database table.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<RowId> type = TypeFactory.getType(RowId.class);
     * ResultSet rs = statement.executeQuery("SELECT ROWID, name FROM users");
     * RowId rowId = type.get(rs, 1);   // Get RowId from first column
     * }</pre>
     *
     * @param rs the ResultSet to read from
     * @param columnIndex the 1-based index of the column to retrieve
     * @return the RowId value from the specified column, or {@code null} if the column value is SQL NULL
     * @throws SQLException if a database access error occurs or the column index is invalid
     */
    @Override
    public RowId get(final ResultSet rs, final int columnIndex) throws SQLException {
        return rs.getRowId(columnIndex);
    }

    /**
     * Retrieves a SQL ROWID value from the specified column in the ResultSet.
     * A ROWID is a unique identifier for a row in a database table.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<RowId> type = TypeFactory.getType(RowId.class);
     * ResultSet rs = statement.executeQuery("SELECT ROWID as row_id, name FROM users");
     * RowId rowId = type.get(rs, "row_id");   // Get RowId by column name
     * }</pre>
     *
     * @param rs the ResultSet to read from
     * @param columnLabel the label of the column to retrieve (column name or alias)
     * @return the RowId value from the specified column, or {@code null} if the column value is SQL NULL
     * @throws SQLException if a database access error occurs or the column label is not found
     */
    @Override
    public RowId get(final ResultSet rs, final String columnLabel) throws SQLException {
        return rs.getRowId(columnLabel);
    }

    /**
     * Sets a RowId parameter in a PreparedStatement.
     * The RowId represents a unique identifier for a row in a database table.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<RowId> type = TypeFactory.getType(RowId.class);
     * PreparedStatement stmt = conn.prepareStatement("UPDATE users SET status = ? WHERE ROWID = ?");
     * type.set(stmt, 2, rowId);   // Set RowId at parameter index 2
     * }</pre>
     *
     * @param stmt the PreparedStatement to set the parameter on
     * @param columnIndex the 1-based index of the parameter to set
     * @param x the RowId value to set as the parameter
     * @throws SQLException if a database access error occurs or the parameter index is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final RowId x) throws SQLException {
        stmt.setRowId(columnIndex, x);
    }

    /**
     * Sets a RowId parameter in a CallableStatement.
     * The RowId represents a unique identifier for a row in a database table.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<RowId> type = TypeFactory.getType(RowId.class);
     * CallableStatement stmt = conn.prepareCall("{call update_user_status(?, ?)}");
     * type.set(stmt, "user_rowid", rowId);   // Set RowId by parameter name
     * }</pre>
     *
     * @param stmt the CallableStatement to set the parameter on
     * @param parameterName the name of the parameter to set
     * @param x the RowId value to set as the parameter
     * @throws SQLException if a database access error occurs or the parameter name is not found
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final RowId x) throws SQLException {
        stmt.setRowId(parameterName, x);
    }

    /**
     * Writes the character representation of a RowId to the given CharacterWriter.
     * If the RowId is {@code null}, writes "null". Otherwise, writes the string representation
     * of the RowId obtained from its toString() method.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<RowId> type = TypeFactory.getType(RowId.class);
     * CharacterWriter writer = new CharacterWriter();
     * type.writeCharacter(writer, rowId, config);   // Writes RowId to character stream
     * }</pre>
     *
     * @param writer the CharacterWriter to write to
     * @param x the RowId to write
     * @param config the serialization configuration (currently unused for RowId)
     * @throws IOException if an I/O error occurs during the write operation
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, final RowId x, final JsonXmlSerializationConfig<?> config) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            writer.write(stringOf(x));
        }
    }
}
