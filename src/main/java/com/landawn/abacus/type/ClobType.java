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
import java.sql.Clob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.exception.UncheckedSQLException;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.Strings;

/**
 * Type handler for JDBC {@link Clob} (Character Large Object) values.
 * This class provides database read/write operations for {@link Clob} objects.
 *
 * <p>{@link #stringOf} reads the complete character content of a CLOB and returns it as a
 * {@link String}. The CLOB is freed automatically after reading.
 * {@link #valueOf(String)} is not supported and always throws {@link UnsupportedOperationException},
 * because CLOB objects must be created through a JDBC {@link java.sql.Connection}.
 * {@link #valueOf(Object)} returns an object that already is an instance of the handled
 * {@link Clob} class unchanged, maps {@code null} to {@code null}, and rejects everything else
 * without touching it.</p>
 *
 * @see AbstractType
 * @see Clob
 */
@SuppressWarnings("java:S2160")
public class ClobType extends AbstractType<Clob> {

    /** The type name constant for Clob type identification, equal to {@code "Clob"}. */
    public static final String CLOB = Clob.class.getSimpleName();

    /** The concrete {@link Clob} implementation class managed by this type handler. */
    private final Class<Clob> clazz;

    /**
     * Package-private constructor for {@code ClobType}.
     * Instances are created by the {@code TypeFactory}.
     */
    ClobType() {
        this(Clob.class);
    }

    /**
     * Package-private constructor for {@code ClobType} with a specific {@link Clob} implementation class.
     * Used when a concrete JDBC driver CLOB subtype needs to be registered. The handler name is
     * derived from {@code clazz}, so the subtype remains distinguishable from the standard
     * {@code Clob} interface in type metadata.
     *
     * @param clazz the specific {@link Clob} implementation class to handle
     */
    ClobType(final Class<? extends Clob> clazz) {
        super(ClassUtil.getSimpleClassName(clazz));
        this.clazz = (Class<Clob>) clazz;
    }

    /**
     * Returns the Java class represented by this type handler.
     *
     * @return the concrete {@link Clob} implementation class, defaulting to {@code Clob.class}
     */
    @Override
    public Class<Clob> javaType() {
        return clazz;
    }

    /**
     * Returns the complete character content of a {@link Clob} as a {@link String}.
     * After reading, the CLOB is freed via {@link Clob#free()}.
     * If an exception occurs during the read, any subsequent exception from {@code free()} is
     * attached as a suppressed exception.
     *
     * @param x the {@link Clob} to read; may be {@code null}
     * @return the full character content of the CLOB, an empty string if the CLOB has zero length,
     *         or {@code null} if {@code x} is {@code null}
     * @throws UnsupportedOperationException if the CLOB length exceeds {@link Integer#MAX_VALUE} characters
     * @throws UncheckedSQLException         if a {@link java.sql.SQLException} occurs while reading the CLOB
     *                                       content or freeing its resources
     */
    @MayReturnNull
    @Override
    public String stringOf(final Clob x) throws UnsupportedOperationException, UncheckedSQLException {
        if (x == null) {
            return null;
        }

        Throwable primaryException = null;

        try {
            final long len = x.length();
            if (len > Integer.MAX_VALUE) {
                throw new UnsupportedOperationException("Clob too large to convert to String: " + len + " characters");
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
     * Not supported for {@link Clob} objects.
     * CLOB instances must be created through a JDBC {@link java.sql.Connection} and cannot be
     * reconstructed from a plain string.
     *
     * @param str ignored
     * @return never returns normally
     * @throws UnsupportedOperationException always
     */
    @Override
    public Clob valueOf(final String str) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("Clob cannot be created from string representation");
    }

    /**
     * Returns {@code obj} unchanged if it already is an instance of the {@link Clob} class handled by
     * this type; otherwise the conversion is not supported.
     * Unlike the inherited default, this method never reads or frees the supplied object: a
     * {@link Clob} cannot be reconstructed from its string form, so converting through
     * {@link #stringOf(Clob)} would only destroy the caller's locator before failing.
     *
     * @param obj the object to convert; may be {@code null}
     * @return the same {@link Clob} instance if {@code obj} is an instance of {@link #javaType()},
     *         or {@code null} if {@code obj} is {@code null}
     * @throws UnsupportedOperationException if {@code obj} is non-null and not an instance of {@link #javaType()}
     */
    @MayReturnNull
    @Override
    public Clob valueOf(final Object obj) throws UnsupportedOperationException {
        if (obj == null) {
            return null; // NOSONAR
        } else if (clazz.isInstance(obj)) {
            return clazz.cast(obj);
        }

        throw new UnsupportedOperationException("Clob cannot be created from " + obj.getClass().getName());
    }

    /**
     * Retrieves a {@link Clob} value from a {@link java.sql.ResultSet} at the specified column index.
     *
     * @param rs          the {@link java.sql.ResultSet} to read from
     * @param columnIndex the 1-based column index
     * @return the {@link Clob} at the specified column,
     *         or {@code null} if the column value is SQL {@code NULL}
     * @throws NullPointerException if {@code rs} is null when the JDBC operation is invoked
     * @throws SQLException if a database access error occurs or the column index is invalid
     */
    @Override
    public Clob get(final ResultSet rs, final int columnIndex) throws NullPointerException, SQLException {
        return rs.getClob(columnIndex);
    }

    /**
     * Retrieves a {@link Clob} value from a {@link java.sql.ResultSet} using the specified column label.
     *
     * @param rs         the {@link java.sql.ResultSet} to read from
     * @param columnName the label of the column to retrieve
     * @return the {@link Clob} in the specified column,
     *         or {@code null} if the column value is SQL {@code NULL}
     * @throws NullPointerException if {@code rs} is null when the JDBC operation is invoked
     * @throws SQLException if a database access error occurs or the column label is not found
     */
    @Override
    public Clob get(final ResultSet rs, final String columnName) throws NullPointerException, SQLException {
        return rs.getClob(columnName);
    }

    /**
     * Sets a {@link Clob} value as a parameter in a {@link java.sql.PreparedStatement}.
     *
     * @param stmt        the {@link java.sql.PreparedStatement} in which to set the parameter
     * @param columnIndex the 1-based parameter index
     * @param x           the {@link Clob} to set; may be {@code null}
     * @throws NullPointerException if {@code stmt} is null when the JDBC operation is invoked
     * @throws SQLException if a database access error occurs or the parameter index is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final Clob x) throws NullPointerException, SQLException {
        stmt.setClob(columnIndex, x);
    }

    /**
     * Sets a {@link Clob} value as a named parameter in a {@link java.sql.CallableStatement}.
     *
     * @param stmt          the {@link java.sql.CallableStatement} in which to set the parameter
     * @param parameterName the name of the parameter to set
     * @param x             the {@link Clob} to set; may be {@code null}
     * @throws NullPointerException if {@code stmt} is null when the JDBC operation is invoked
     * @throws SQLException if a database access error occurs or the parameter name is not found
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final Clob x) throws NullPointerException, SQLException {
        stmt.setClob(parameterName, x);
    }
}
