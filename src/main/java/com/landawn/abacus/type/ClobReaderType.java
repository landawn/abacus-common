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

import java.io.Reader;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Type handler for CLOB (Character Large Object) values accessed as character {@link Reader}s.
 * This class extends {@link ReaderType} and overrides the JDBC accessors so that CLOB columns
 * are read via {@link Clob#getCharacterStream()} and written via
 * {@link java.sql.PreparedStatement#setClob(int, Reader) setClob}.
 *
 * @see ReaderType
 * @see Clob
 * @see Reader
 */
public class ClobReaderType extends ReaderType {

    /**
     * The type name constant used for registration, equal to {@code "ClobReader"}.
     */
    public static final String CLOB_READER = "ClobReader";

    /**
     * Package-private constructor for {@code ClobReaderType}.
     * Instances are created by the {@code TypeFactory}.
     */
    ClobReaderType() {
        super(CLOB_READER);
    }

    /**
     * Retrieves a CLOB column as a character {@link Reader} from a {@link java.sql.ResultSet}
     * at the specified column index.
     * The CLOB is obtained via {@link java.sql.ResultSet#getClob(int)} and its character stream
     * is returned via {@link Clob#getCharacterStream()}.
     *
     * @param rs          the {@link java.sql.ResultSet} to read from
     * @param columnIndex the 1-based column index
     * @return a {@link Reader} for the CLOB's character stream,
     *         or {@code null} if the column value is SQL {@code NULL}
     * @throws SQLException if a database access error occurs or the column index is invalid
     */
    @Override
    public Reader get(final ResultSet rs, final int columnIndex) throws SQLException {
        final Clob clob = rs.getClob(columnIndex);
        return clobToReader(clob);
    }

    /**
     * Retrieves a CLOB column as a character {@link Reader} from a {@link java.sql.ResultSet}
     * using the specified column label.
     * The CLOB is obtained via {@link java.sql.ResultSet#getClob(String)} and its character stream
     * is returned via {@link Clob#getCharacterStream()}.
     *
     * @param rs         the {@link java.sql.ResultSet} to read from
     * @param columnName the label of the column to retrieve
     * @return a {@link Reader} for the CLOB's character stream,
     *         or {@code null} if the column value is SQL {@code NULL}
     * @throws SQLException if a database access error occurs or the column label is not found
     */
    @Override
    public Reader get(final ResultSet rs, final String columnName) throws SQLException {
        return clobToReader(rs.getClob(columnName));
    }

    /**
     * Sets a {@link Reader} as a CLOB parameter in a {@link java.sql.PreparedStatement}.
     * The reader's content is bound via {@link java.sql.PreparedStatement#setClob(int, Reader)}.
     *
     * @param stmt        the {@link java.sql.PreparedStatement} in which to set the parameter
     * @param columnIndex the 1-based parameter index
     * @param x           the {@link Reader} whose content will be stored as CLOB data; may be {@code null}
     * @throws SQLException if a database access error occurs or the parameter index is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final Reader x) throws SQLException {
        stmt.setClob(columnIndex, x);
    }

    /**
     * Sets a {@link Reader} as a named CLOB parameter in a {@link java.sql.CallableStatement}.
     * The reader's content is bound via {@link java.sql.CallableStatement#setClob(String, Reader)}.
     *
     * @param stmt          the {@link java.sql.CallableStatement} in which to set the parameter
     * @param parameterName the name of the parameter to set
     * @param x             the {@link Reader} whose content will be stored as CLOB data; may be {@code null}
     * @throws SQLException if a database access error occurs or the parameter name is not found
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final Reader x) throws SQLException {
        stmt.setClob(parameterName, x);
    }

    /**
     * Sets a {@link Reader} as a CLOB parameter in a {@link java.sql.PreparedStatement},
     * specifying the number of characters to read.
     * The reader's content is bound via {@link java.sql.PreparedStatement#setClob(int, Reader, long)}.
     *
     * @param stmt            the {@link java.sql.PreparedStatement} in which to set the parameter
     * @param columnIndex     the 1-based parameter index
     * @param x               the {@link Reader} whose content will be stored as CLOB data; may be {@code null}
     * @param sqlTypeOrLength the number of characters to read from the reader
     * @throws SQLException if a database access error occurs or the parameter index is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final Reader x, final int sqlTypeOrLength) throws SQLException {
        stmt.setClob(columnIndex, x, sqlTypeOrLength);
    }

    /**
     * Sets a {@link Reader} as a named CLOB parameter in a {@link java.sql.CallableStatement},
     * specifying the number of characters to read.
     * The reader's content is bound via {@link java.sql.CallableStatement#setClob(String, Reader, long)}.
     *
     * @param stmt            the {@link java.sql.CallableStatement} in which to set the parameter
     * @param parameterName   the name of the parameter to set
     * @param x               the {@link Reader} whose content will be stored as CLOB data; may be {@code null}
     * @param sqlTypeOrLength the number of characters to read from the reader
     * @throws SQLException if a database access error occurs or the parameter name is not found
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final Reader x, final int sqlTypeOrLength) throws SQLException {
        stmt.setClob(parameterName, x, sqlTypeOrLength);
    }

    /**
     * Extracts a character {@link Reader} from a {@link Clob}.
     * This is a package-private utility used by both {@code get} overloads.
     *
     * @param clob the {@link Clob} to read from; may be {@code null}
     * @return a {@link Reader} for the CLOB's character stream,
     *         or {@code null} if {@code clob} is {@code null}
     * @throws SQLException if a database access error occurs while accessing the CLOB
     */
    static Reader clobToReader(final Clob clob) throws SQLException {
        if (clob != null) {
            return clob.getCharacterStream();
        }

        return null; // NOSONAR
    }
}
