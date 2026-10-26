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

import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.exception.UncheckedSQLException;
import com.landawn.abacus.util.N;

/**
 * Type handler for {@link Ref} objects and their driver-specific implementations.
 * A SQL REF is a reference to an SQL structured type value in the database. It is used in
 * object-relational mapping to reference rows or objects in the database.
 *
 * <p>This type handler reads Ref values from ResultSets and binds them to PreparedStatements or CallableStatements.
 * {@link #stringOf(Ref)} dereferences the value with {@link Ref#getObject()} and formats the result with
 * {@link N#stringOf(Object)}, wrapping SQL failures in {@link UncheckedSQLException}. The formatted value
 * cannot recreate the database reference through {@link #valueOf(String)}.</p>
 *
 * <p>{@link TypeFactory#getType(Class)} supports both {@code Ref.class} and implementation classes.
 * {@link #javaType()} retains the requested class, while {@link #name()} remains {@code "Ref"}.
 * Selecting an implementation class does not construct or convert reference values; JDBC operations
 * transfer the supplied or retrieved instances unchanged.</p>
 *
 * <p><b>Important Notes:</b></p>
 * <ul>
 *   <li>This type is NOT serializable (isSerializable() returns false)</li>
 *   <li>stringOf() formats the referenced object; valueOf(String) throws UnsupportedOperationException</li>
 *   <li>valueOf(Object) returns an object that already is a Ref unchanged, maps null to null, and rejects
 *       everything else without touching it</li>
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
 *
 *         // Use the Ref while it is in scope
 *         try (PreparedStatement updateStmt = conn.prepareStatement("UPDATE employees SET person_ref = ? WHERE id = ?")) {
 *             type.set(updateStmt, 1, personRef);
 *             updateStmt.setInt(2, employeeId);
 *             updateStmt.executeUpdate();
 *         }
 *     }
 * }
 *
 * // Format the referenced object for display; this may access the database.
 * String str = type.stringOf(ref);
 * // Reconstructing the Ref from text is NOT supported.
 * // Ref ref = type.valueOf("string");   // Throws UnsupportedOperationException
 * }</pre>
 *
 */
public class RefType extends AbstractType<Ref> {

    /**
     * The type name identifier for SQL Ref type, equal to the simple class name {@code "Ref"}.
     */
    public static final String REF = Ref.class.getSimpleName();

    /** The {@link Ref} interface or implementation class represented by this handler. */
    private final Class<Ref> clazz;

    /**
     * Constructs a handler for the standard Ref interface, for use by {@link TypeFactory}.
     */
    RefType() {
        this(Ref.class);
    }

    /**
     * Constructs a handler for a specific SQL Ref interface or implementation class.
     * The class is retained as metadata; the handler name remains {@code "Ref"}.
     *
     * @param targetClass the Ref interface or implementation class managed by this handler
     */
    RefType(final Class<? extends Ref> targetClass) {
        super(REF);
        this.clazz = (Class<Ref>) targetClass;
    }

    /**
     * Returns the Ref interface or implementation class selected for this handler.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Ref> type = TypeFactory.getType(Ref.class);
     * Class<Ref> clazz = type.javaType();
     * System.out.println(clazz.getName());   // Output: java.sql.Ref
     * Type<Ref> implementationType = TypeFactory.getType(ref.getClass());
     * Class<Ref> implementationClass = implementationType.javaType();   // Returns ref.getClass()
     * }</pre>
     *
     * @return the Ref interface or implementation class managed by this handler
     */
    @Override
    public Class<Ref> javaType() {
        return clazz;
    }

    /**
     * Indicates whether this type is serializable.
     * Formatting the referenced object is supported, but reconstructing the database reference from that text is not.
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
     * Reads the referenced object with {@link Ref#getObject()} and formats it with {@link N#stringOf(Object)}.
     * The runtime type handler of the referenced object determines the text format. This operation may
     * access the database; it does not preserve the reference identity in the text or support reconstruction
     * through {@link #valueOf(String)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Ref> type = TypeFactory.getType(Ref.class);
     * String referencedValue = type.stringOf(ref);
     * }</pre>
     *
     * @param x the Ref to dereference; may be {@code null}
     * @return the formatted referenced object, or {@code null} if {@code x} or the referenced object is null
     * @throws UncheckedSQLException if reading the referenced object fails; the original SQLException is retained as the cause
     */
    @Override
    public String stringOf(final Ref x) throws UncheckedSQLException {
        if (x == null) {
            return null;
        }

        try {
            return N.stringOf(x.getObject());
        } catch (SQLException e) {
            throw new UncheckedSQLException(e);
        }
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
     *     System.out.println("Cannot create Ref from string");   // This branch always runs
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
     * Returns {@code obj} unchanged if it already is a {@link Ref}; otherwise the conversion is not supported.
     * Unlike the inherited default, this method never serializes the value: {@code AbstractType.valueOf(Object)}
     * renders {@code obj} with the handler registered for its own runtime class and feeds that text to
     * {@link #valueOf(String)}, which always throws - so the caller's locator would be read for nothing.
     *
     * @param obj the object to convert; may be {@code null}
     * @return the same {@link Ref} instance if {@code obj} is a {@link Ref}, or {@code null} if {@code obj} is
     *         {@code null}
     * @throws UnsupportedOperationException if {@code obj} is non-null and not a {@link Ref}
     */
    @MayReturnNull
    @Override
    public Ref valueOf(final Object obj) throws UnsupportedOperationException {
        if (obj == null) {
            return null; // NOSONAR
        } else if (obj instanceof Ref value) {
            return value;
        }

        throw new UnsupportedOperationException("Ref cannot be created from " + obj.getClass().getName());
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
     * @throws NullPointerException if {@code rs} is null when the JDBC operation is invoked
     * @throws SQLException if a database access error occurs or the column index is invalid
     */
    @Override
    public Ref get(final ResultSet rs, final int columnIndex) throws NullPointerException, SQLException {
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
     * @param columnName the label of the column to retrieve (column name or alias)
     * @return the Ref value from the specified column, or {@code null} if the column value is SQL NULL
     * @throws NullPointerException if {@code rs} is null when the JDBC operation is invoked
     * @throws SQLException if a database access error occurs or the column label is not found
     */
    @Override
    public Ref get(final ResultSet rs, final String columnName) throws NullPointerException, SQLException {
        return rs.getRef(columnName);
    }

    /**
     * Sets a Ref parameter in a PreparedStatement.
     * The Ref represents a reference to an SQL structured type value in the database.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Ref> type = TypeFactory.getType(Ref.class);
     * try (PreparedStatement stmt = conn.prepareStatement("UPDATE employees SET person_ref = ? WHERE id = ?")) {
     *     type.set(stmt, 1, personRef);
     *     stmt.setInt(2, 123);
     *     stmt.executeUpdate();
     * }
     * }</pre>
     *
     * @param statement the PreparedStatement to set the parameter on
     * @param columnIndex the 1-based index of the parameter to set
     * @param x the Ref value to set as the parameter
     * @throws NullPointerException if {@code statement} is null when the JDBC operation is invoked
     * @throws SQLException if a database access error occurs or the parameter index is invalid
     */
    @Override
    public void set(final PreparedStatement statement, final int columnIndex, final Ref x) throws NullPointerException, SQLException {
        statement.setRef(columnIndex, x);
    }

    /**
     * Sets a Ref parameter in a CallableStatement.
     * The Ref represents a reference to an SQL structured type value in the database.
     * Note: this method uses {@link CallableStatement#setObject(String, Object)} rather than {@code setRef},
     * because JDBC declares {@code setRef} only for the positional
     * {@link PreparedStatement#setRef(int, Ref)} form; there is no name-based overload.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Ref> type = TypeFactory.getType(Ref.class);
     * try (CallableStatement stmt = conn.prepareCall("{call update_employee(?, ?)}")) {
     *     type.set(stmt, "ref_param", personRef);
     *     stmt.setInt("id_param", 123);
     *     stmt.execute();
     * }
     * }</pre>
     *
     * @param statement the CallableStatement to set the parameter on
     * @param parameterName the name of the parameter to set
     * @param x the Ref value to set as the parameter
     * @throws NullPointerException if {@code statement} is null when the JDBC operation is invoked
     * @throws SQLException if a database access error occurs or the parameter name is not found
     */
    @Override
    public void set(final CallableStatement statement, final String parameterName, final Ref x) throws NullPointerException, SQLException {
        // stmt.setRef(parameterName, x);

        statement.setObject(parameterName, x);
    }
}
