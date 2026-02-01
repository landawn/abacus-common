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
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Type handler for java.sql.Ref objects.
 * A SQL REF is a reference to an SQL structured type value in the database. It is used in
 * object-relational mapping to reference rows or objects in the database.
 *
 * <p>This type handler provides limited functionality because Ref objects are database-specific
 * and cannot be meaningfully serialized to or deserialized from strings. The primary use case
 * is for reading Ref values from ResultSets and setting them in PreparedStatements or CallableStatements.
 *
 * <p><b>Important Notes:</b></p>
 * <ul>
 *   <li>This type is NOT serializable (isSerializable() returns false)</li>
 *   <li>stringOf() and valueOf(String) operations are not supported and will throw UnsupportedOperationException</li>
 *   <li>Ref objects are typically obtained from database queries and represent references to structured types</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * Type<Ref> type = TypeFactory.getType(Ref.class);
 *
 * // Reading a Ref from database
 * try (ResultSet rs = stmt.executeQuery("SELECT person_ref FROM employees WHERE id = 1")) {
 *     if (rs.next()) {
 *         Ref personRef = type.get(rs, 1);
 *         String baseTypeName = personRef.getBaseTypeName();
 *         Object referencedObject = personRef.getObject();
 *     }
 * }
 *
 * // Using Ref in PreparedStatement
 * PreparedStatement updateStmt = conn.prepareStatement("UPDATE employees SET person_ref = ? WHERE id = ?");
 * type.set(updateStmt, 1, personRef);
 * updateStmt.setInt(2, employeeId);
 * updateStmt.executeUpdate();
 *
 * // Note: These operations are NOT supported
 * // String str = type.stringOf(ref);   // Throws UnsupportedOperationException
 * // Ref ref = type.valueOf("string");   // Throws UnsupportedOperationException
 * }</pre>
 */
public class RefType extends AbstractType<Ref> {

    public static final String REF = Ref.class.getSimpleName();

    /**
     * Constructs a new RefType instance.
     * This constructor is package-private and intended to be called only by the TypeFactory.
     */
    RefType() {
        super(REF);
    }

    /**
     * Returns the Class object representing the SQL Ref type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Ref> type = TypeFactory.getType(Ref.class);
     * Class<Ref> clazz = type.clazz();
     * System.out.println(clazz.getName());   // Output: java.sql.Ref
     * }</pre>
     *
     * @return the Class object for java.sql.Ref.class
     */
    @Override
    public Class<Ref> clazz() {
        return Ref.class;
    }

    /**
     * Indicates whether this type is serializable.
     * SQL Ref types are not serializable as they represent database references.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Ref> type = TypeFactory.getType(Ref.class);
     * boolean serializable = type.isSerializable();
     * System.out.println(serializable);   // Output: false
     * }</pre>
     *
     * @return {@code false}, indicating this type is not serializable
     */
    @Override
    public boolean isSerializable() {
        return false;
    }

    /**
     * Converts a Ref object to its string representation.
     * This operation is not supported for SQL Ref types as they represent
     * database-specific references that cannot be meaningfully serialized to string.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Ref> type = TypeFactory.getType(Ref.class);
     * Ref ref = org.mockito.Mockito.mock(Ref.class);
     * try {
     *     String str = type.stringOf(ref);
     * } catch (UnsupportedOperationException e) {
     *     System.out.println("Cannot convert Ref to string");   // This will execute
     * }
     * }</pre>
     *
     * @param x the Ref object to convert
     * @return never returns normally
     * @throws UnsupportedOperationException always thrown as Ref cannot be converted to string
     */
    @Override
    public String stringOf(final Ref x) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("Ref cannot be converted to string representation");
    }

    /**
     * Creates a Ref object from a string representation.
     * This operation is not supported for SQL Ref types as they represent
     * database-specific references that cannot be created from string.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Ref> type = TypeFactory.getType(Ref.class);
     * try {
     *     Ref ref = type.valueOf("some_string");
     * } catch (UnsupportedOperationException e) {
     *     System.out.println("Cannot create Ref from string");   // This will execute
     * }
     * }</pre>
     *
     * @param str the string to convert
     * @return never returns normally
     * @throws UnsupportedOperationException always thrown as Ref cannot be created from string
     */
    @Override
    public Ref valueOf(final String str) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("Ref cannot be created from string representation");
    }

    /**
     * Retrieves a SQL REF value from the specified column in the ResultSet.
     * A REF value represents a reference to an SQL structured type value in the database.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Ref> type = TypeFactory.getType(Ref.class);
     * // Assuming rs is a ResultSet with a REF value in column 1
     * Ref ref = type.get(rs, 1);
     * if (ref != null) {
     *     String baseTypeName = ref.getBaseTypeName();
     *     System.out.println("Referenced type: " + baseTypeName);
     * }
     * }</pre>
     *
     * @param rs the ResultSet to read from
     * @param columnIndex the 1-based index of the column to retrieve
     * @return the Ref value from the specified column, or {@code null} if the column value is SQL NULL
     * @throws SQLException if a database access error occurs or the column index is invalid
     */
    @Override
    public Ref get(final ResultSet rs, final int columnIndex) throws SQLException {
        return rs.getRef(columnIndex);
    }

    /**
     * Retrieves a SQL REF value from the specified column in the ResultSet.
     * A REF value represents a reference to an SQL structured type value in the database.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Ref> type = TypeFactory.getType(Ref.class);
     * // Assuming rs is a ResultSet with a REF value in column "object_ref"
     * Ref ref = type.get(rs, "object_ref");
     * if (ref != null) {
     *     Object referencedObject = ref.getObject();
     *     System.out.println("Referenced object: " + referencedObject);
     * }
     * }</pre>
     *
     * @param rs the ResultSet to read from
     * @param columnLabel the label of the column to retrieve (column name or alias)
     * @return the Ref value from the specified column, or {@code null} if the column value is SQL NULL
     * @throws SQLException if a database access error occurs or the column label is not found
     */
    @Override
    public Ref get(final ResultSet rs, final String columnLabel) throws SQLException {
        return rs.getRef(columnLabel);
    }

    /**
     * Sets a Ref parameter in a PreparedStatement.
     * The Ref represents a reference to an SQL structured type value in the database.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Ref> type = TypeFactory.getType(Ref.class);
     * PreparedStatement stmt = org.mockito.Mockito.mock(PreparedStatement.class);
     * Ref ref = org.mockito.Mockito.mock(Ref.class);
     * type.set(stmt, 1, ref);
     * stmt.setInt(2, 123);
     * stmt.executeUpdate();
     * }</pre>
     *
     * @param stmt the PreparedStatement to set the parameter on
     * @param columnIndex the 1-based index of the parameter to set
     * @param x the Ref value to set as the parameter
     * @throws SQLException if a database access error occurs or the parameter index is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final Ref x) throws SQLException {
        stmt.setRef(columnIndex, x);
    }

    /**
     * Sets a Ref parameter in a CallableStatement.
     * The Ref represents a reference to an SQL structured type value in the database.
     * Note: This method uses setObject instead of setRef as CallableStatement may not support setRef with parameter names.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Ref> type = TypeFactory.getType(Ref.class);
     * CallableStatement stmt = org.mockito.Mockito.mock(CallableStatement.class);
     * Ref ref = org.mockito.Mockito.mock(Ref.class);
     * type.set(stmt, "ref_param", ref);
     * stmt.setInt("id_param", 123);
     * stmt.execute();
     * }</pre>
     *
     * @param stmt the CallableStatement to set the parameter on
     * @param parameterName the name of the parameter to set
     * @param x the Ref value to set as the parameter
     * @throws SQLException if a database access error occurs or the parameter name is not found
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final Ref x) throws SQLException {
        // stmt.setRef(parameterName, x);

        stmt.setObject(parameterName, x);
    }
}