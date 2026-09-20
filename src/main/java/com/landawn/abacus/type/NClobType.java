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
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.exception.UncheckedSQLException;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.Strings;

/**
 * Type handler for {@link NClob} (National Character Large Object) objects, providing
 * database interaction capabilities for handling large Unicode text data.
 * The {@link #stringOf(NClob)} method reads the entire NCLOB content into a {@link String}
 * and then frees the NCLOB; {@link #valueOf(String)} is not supported.
 * {@link #valueOf(Object)} returns an object that already is an instance of the handled
 * {@link NClob} class unchanged, maps {@code null} to {@code null}, and rejects everything else
 * without touching it.
 */
@SuppressWarnings("java:S2160")
public class NClobType extends AbstractType<NClob> {

    /** The type name constant for NClob type identification, equal to {@code "NClob"}. */
    public static final String NCLOB = NClob.class.getSimpleName();

    private final Class<NClob> clazz;

    /**
     * Package-private constructor for NClobType using the standard {@link NClob} class.
     * This constructor is called by the TypeFactory to create NClob type instances.
     */
    NClobType() {
        this(NClob.class);
    }

    /**
     * Package-private constructor for {@code NClobType} bound to a specific {@link NClob}
     * implementation class. The handler name is derived from {@code clazz}, so a driver-specific
     * implementation remains distinguishable from the standard {@code NClob} interface in type metadata.
     *
     * @param clazz the specific {@link NClob} class or subclass to handle; must not be {@code null}
     * @throws IllegalArgumentException if {@code clazz} is {@code null}.
     */
    NClobType(final Class<? extends NClob> clazz) throws IllegalArgumentException {
        super(ClassUtil.getSimpleClassName(clazz));
        this.clazz = (Class<NClob>) clazz;
    }

    /**
     * Returns the Java class type that this type handler manages.
     *
     * @return the configured {@link NClob} class (or {@link NClob} subclass) this handler was created for
     */
    @Override
    public Class<NClob> javaType() {
        return clazz;
    }

    /**
     * Converts an {@link NClob} object to its string representation by extracting the full
     * character content. The {@code NClob} is freed via {@link NClob#free()} after extraction.
     * This operation loads the entire NCLOB content into memory and is therefore not suitable
     * for very large objects.
     *
     * @param x the {@code NClob} object to convert, may be {@code null}
     * @return the string content of the {@code NClob}, an empty string if the NCLOB has zero length,
     *         or {@code null} if the input is {@code null}
     * @throws UnsupportedOperationException if the NCLOB length exceeds {@link Integer#MAX_VALUE}
     * @throws UncheckedSQLException if a database access error occurs during extraction or freeing
     */
    @MayReturnNull
    @Override
    public String stringOf(final NClob x) throws UnsupportedOperationException, UncheckedSQLException {
        if (x == null) {
            return null;
        }

        Throwable primaryException = null;

        try {
            final long len = x.length();
            if (len > Integer.MAX_VALUE) {
                throw new UnsupportedOperationException("NClob too large to convert to String: " + len + " characters");
            }
            // Position 1 does not exist in a zero-length lob, so getSubString(1, 0) is rejected by
            // some implementations (e.g. javax.sql.rowset.serial.SerialClob). Stay inside the try so
            // the finally still frees the locator.
            return len == 0 ? Strings.EMPTY : x.getSubString(1, (int) len);
        } catch (final SQLException e) {
            final UncheckedSQLException uncheckedException = new UncheckedSQLException(e);
            primaryException = uncheckedException;
            throw uncheckedException;
        } catch (final RuntimeException | Error e) {
            primaryException = e;
            throw e;
        } finally {
            try {
                x.free();
            } catch (final SQLException e) {
                final UncheckedSQLException freeException = new UncheckedSQLException(e);
                if (primaryException != null) {
                    primaryException.addSuppressed(freeException);
                } else {
                    throw freeException; //NOSONAR
                }
            } catch (final RuntimeException | Error e) {
                if (primaryException == null) {
                    throw e;
                } else if (primaryException != e) {
                    primaryException.addSuppressed(e);
                }
            }
        }
    }

    /**
     * Converts a string representation to an {@link NClob} object.
     * This operation is not supported as NCLOBs cannot be created from strings directly
     * and must be obtained from database operations.
     *
     * @param str the string to convert
     * @return never returns normally
     * @throws UnsupportedOperationException always thrown as this operation is not supported
     */
    @Override
    public NClob valueOf(final String str) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("NClob cannot be created from string representation");
    }

    /**
     * Returns {@code obj} unchanged if it already is an instance of the {@link NClob} class handled by
     * this type; otherwise the conversion is not supported.
     * Unlike the inherited default, this method never reads or frees the supplied object (including a
     * plain {@link java.sql.Clob}, which is not an {@code NClob}): an NCLOB cannot be reconstructed
     * from its string form, so converting through {@link #stringOf(NClob)} would only destroy the
     * caller's locator before failing.
     *
     * @param obj the object to convert; may be {@code null}
     * @return the same {@link NClob} instance if {@code obj} is an instance of {@link #javaType()},
     *         or {@code null} if {@code obj} is {@code null}
     * @throws UnsupportedOperationException if {@code obj} is non-null and not an instance of {@link #javaType()}
     */
    @MayReturnNull
    @Override
    public NClob valueOf(final Object obj) throws UnsupportedOperationException {
        if (obj == null) {
            return null; // NOSONAR
        } else if (clazz.isInstance(obj)) {
            return clazz.cast(obj);
        }

        throw new UnsupportedOperationException("NClob cannot be created from " + obj.getClass().getName());
    }

    /**
     * Retrieves an {@link NClob} value from the specified column in the {@link ResultSet}.
     *
     * @param rs the {@code ResultSet} to read from
     * @param columnIndex the 1-based index of the column to retrieve the {@code NClob} from
     * @return the {@code NClob} object, or {@code null} if the column value is SQL {@code NULL}
     * @throws NullPointerException if {@code rs} is null when the JDBC operation is invoked
     * @throws SQLException if a database access error occurs or {@code columnIndex} is invalid
     */
    @Override
    public NClob get(final ResultSet rs, final int columnIndex) throws NullPointerException, SQLException {
        return rs.getNClob(columnIndex);
    }

    /**
     * Retrieves an {@link NClob} value from the specified column in the {@link ResultSet}.
     *
     * @param rs the {@code ResultSet} to read from
     * @param columnName the label of the column to retrieve (as specified in the SQL AS clause)
     * @return the {@code NClob} object, or {@code null} if the column value is SQL {@code NULL}
     * @throws NullPointerException if {@code rs} is null when the JDBC operation is invoked
     * @throws SQLException if a database access error occurs or {@code columnName} is not found
     */
    @Override
    public NClob get(final ResultSet rs, final String columnName) throws NullPointerException, SQLException {
        return rs.getNClob(columnName);
    }

    /**
     * Sets a parameter in a {@link PreparedStatement} at the specified index to an {@link NClob} value.
     *
     * @param stmt the {@code PreparedStatement} to set the parameter on
     * @param columnIndex the 1-based index of the parameter to set
     * @param x the {@code NClob} value to set, or {@code null} to set SQL {@code NULL}
     * @throws NullPointerException if {@code stmt} is null when the JDBC operation is invoked
     * @throws SQLException if a database access error occurs or {@code columnIndex} is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final NClob x) throws NullPointerException, SQLException {
        stmt.setNClob(columnIndex, x);
    }

    /**
     * Sets a parameter in a {@link CallableStatement} by name to an {@link NClob} value.
     *
     * @param stmt the {@code CallableStatement} to set the parameter on
     * @param parameterName the name of the parameter to set
     * @param x the {@code NClob} value to set, or {@code null} to set SQL {@code NULL}
     * @throws NullPointerException if {@code stmt} is null when the JDBC operation is invoked
     * @throws SQLException if a database access error occurs or {@code parameterName} is not found
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final NClob x) throws NullPointerException, SQLException {
        stmt.setNClob(parameterName, x);
    }
}
