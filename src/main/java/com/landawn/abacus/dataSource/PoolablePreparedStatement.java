/*
 * Copyright (C) 2015 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.landawn.abacus.dataSource;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Map;

import com.landawn.abacus.dataSource.PoolableConnection.CachedStatmentKey;
import com.landawn.abacus.logging.Logger;
import com.landawn.abacus.logging.LoggerFactory;
import com.landawn.abacus.pool.AbstractPoolable;
import com.landawn.abacus.util.ExceptionUtil;

// TODO: Auto-generated Javadoc
/**
 * The Class PoolablePreparedStatement.
 *
 * @author Haiyang Li
 * @since 0.8
 */
class PoolablePreparedStatement extends AbstractPoolable implements PreparedStatement {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(PoolablePreparedStatement.class);

    /** The Constant DEFAULT_LIVE_TIME. */
    private static final long DEFAULT_LIVE_TIME = 24 * 60 * 60 * 1000L;

    /** The Constant DEFAULT_MAX_IDLE_TIME. */
    private static final long DEFAULT_MAX_IDLE_TIME = 30 * 60 * 1000L;

    /** The id. */
    private final CachedStatmentKey id;

    /** The internal stmt. */
    private final java.sql.PreparedStatement internalStmt;

    /** The poolable conn. */
    private final PoolableConnection poolableConn;

    /** The is closed. */
    private boolean isClosed = false;

    /**
     * Instantiates a new poolable prepared statement.
     *
     * @param stmt
     * @param conn
     * @param id
     */
    public PoolablePreparedStatement(java.sql.PreparedStatement stmt, PoolableConnection conn, CachedStatmentKey id) {
        super(DEFAULT_LIVE_TIME, DEFAULT_MAX_IDLE_TIME);
        internalStmt = stmt;
        poolableConn = conn;
        this.id = id;
    }

    /**
     * Gets the id.
     *
     * @return
     */
    CachedStatmentKey getId() {
        return id;
    }

    /**
     *
     * @return boolean
     * @throws SQLException the SQL exception
     * @see java.sql.Statement#isPoolable()
     */
    @Override
    public boolean isPoolable() throws SQLException {
        return internalStmt.isPoolable();
    }

    /**
     *
     * @return boolean
     * @throws SQLException the SQL exception
     * @see java.sql.Statement#isClosed()
     */
    @Override
    public boolean isClosed() throws SQLException {
        if (!isClosed) {
            try {
                if (internalStmt.isClosed()) {
                    destroy();
                }
            } catch (SQLException e) {
                // ignore
                destroy();

                if (logger.isWarnEnabled()) {
                    logger.warn(ExceptionUtil.getMessage(e));
                }
            }
        }

        return isClosed;
    }

    /**
     *
     * @throws SQLException the SQL exception
     * @see java.sql.Statement#close()
     */
    @Override
    public void close() throws SQLException {
        if (isClosed) {
            return;
        }

        if ((id == null) || (poolableConn == null) || internalStmt.isPoolable() == false) {
            isClosed = true;

            internalStmt.close();
        } else {
            poolableConn.cachePreparedStatement(this);
        }
    }

    /**
     * Destroy.
     */
    @Override
    public void destroy() {
        if (isClosed) {
            return;
        }

        isClosed = true;

        try {
            internalStmt.close();
        } catch (SQLException e) {
            // ignore;

            if (logger.isWarnEnabled()) {
                logger.warn(ExceptionUtil.getMessage(e));
            }
        }
    }

    /**
     *
     * @throws SQLException the SQL exception
     * @see java.sql.PreparedStatement#addBatch()
     */
    @Override
    public void addBatch() throws SQLException {
        internalStmt.addBatch();
    }

    /**
     *
     * @throws SQLException the SQL exception
     * @see java.sql.PreparedStatement#clearParameters()
     */
    @Override
    public void clearParameters() throws SQLException {
        internalStmt.clearParameters();
    }

    /**
     *
     * @return boolean
     * @throws SQLException the SQL exception
     * @see java.sql.PreparedStatement#execute()
     */
    @Override
    public boolean execute() throws SQLException {
        boolean isOk = false;

        try {
            boolean result = internalStmt.execute();
            isOk = true;

            return result;
        } finally {
            poolableConn.updateLastSQLExecutionTime(isOk);
        }
    }

    /**
     *
     * @return ResultSet
     * @throws SQLException the SQL exception
     * @see java.sql.PreparedStatement#executeQuery()
     */
    @Override
    public ResultSet executeQuery() throws SQLException {
        boolean isOk = false;

        try {
            final ResultSet result = wrap(internalStmt.executeQuery());
            isOk = true;
            return result;
        } finally {
            poolableConn.updateLastSQLExecutionTime(isOk);
        }
    }

    /**
     *
     * @return int
     * @throws SQLException the SQL exception
     * @see java.sql.PreparedStatement#executeUpdate()
     */
    @Override
    public int executeUpdate() throws SQLException {
        boolean isOk = false;

        try {
            int result = internalStmt.executeUpdate();
            isOk = true;

            return result;
        } finally {
            poolableConn.updateLastSQLExecutionTime(isOk);
        }
    }

    /**
     *
     * @param sql
     * @return boolean
     * @throws SQLException the SQL exception
     * @see java.sql.Statement#execute(String)
     */
    @Override
    public boolean execute(String sql) throws SQLException {
        boolean isOk = false;

        try {
            boolean result = internalStmt.execute(sql);
            isOk = true;

            return result;
        } finally {
            poolableConn.updateLastSQLExecutionTime(isOk);
        }
    }

    /**
     *
     * @param sql
     * @param autoGeneratedKeys
     * @return boolean
     * @throws SQLException the SQL exception
     * @see java.sql.Statement#execute(String, int)
     */
    @Override
    public boolean execute(String sql, int autoGeneratedKeys) throws SQLException {
        boolean isOk = false;

        try {
            boolean result = internalStmt.execute(sql, autoGeneratedKeys);
            isOk = true;

            return result;
        } finally {
            poolableConn.updateLastSQLExecutionTime(isOk);
        }
    }

    /**
     *
     * @param sql
     * @param columnIndexes
     * @return boolean
     * @throws SQLException the SQL exception
     * @see java.sql.Statement#execute(String, int[])
     */
    @Override
    public boolean execute(String sql, int[] columnIndexes) throws SQLException {
        boolean isOk = false;

        try {
            boolean result = internalStmt.execute(sql, columnIndexes);
            isOk = true;

            return result;
        } finally {
            poolableConn.updateLastSQLExecutionTime(isOk);
        }
    }

    /**
     *
     * @param sql
     * @param columnNames
     * @return boolean
     * @throws SQLException the SQL exception
     * @see java.sql.Statement#execute(String, String[])
     */
    @Override
    public boolean execute(String sql, String[] columnNames) throws SQLException {
        boolean isOk = false;

        try {
            boolean result = internalStmt.execute(sql, columnNames);
            isOk = true;

            return result;
        } finally {
            poolableConn.updateLastSQLExecutionTime(isOk);
        }
    }

    /**
     *
     * @return int[]
     * @throws SQLException the SQL exception
     * @see java.sql.Statement#executeBatch()
     */
    @Override
    public int[] executeBatch() throws SQLException {
        boolean isOk = false;

        try {
            int[] result = internalStmt.executeBatch();
            isOk = true;

            return result;
        } finally {
            poolableConn.updateLastSQLExecutionTime(isOk);
        }
    }

    /**
     *
     * @param sql
     * @return ResultSet
     * @throws SQLException the SQL exception
     * @see java.sql.Statement#executeQuery(String)
     */
    @Override
    public ResultSet executeQuery(String sql) throws SQLException {
        boolean isOk = false;

        try {
            final ResultSet result = wrap(internalStmt.executeQuery(sql));
            isOk = true;
            return result;
        } finally {
            poolableConn.updateLastSQLExecutionTime(isOk);
        }
    }

    /**
     *
     * @param rs
     * @return
     */
    private ResultSet wrap(ResultSet rs) {
        return new ResultSetProxy(rs, this);
    }

    /**
     *
     * @param sql
     * @return int
     * @throws SQLException the SQL exception
     * @see java.sql.Statement#executeUpdate(String)
     */
    @Override
    public int executeUpdate(String sql) throws SQLException {
        boolean isOk = false;

        try {
            int result = internalStmt.executeUpdate(sql);
            isOk = true;

            return result;
        } finally {
            poolableConn.updateLastSQLExecutionTime(isOk);
        }
    }

    /**
     *
     * @param sql
     * @param autoGeneratedKeys
     * @return int
     * @throws SQLException the SQL exception
     * @see java.sql.Statement#executeUpdate(String, int)
     */
    @Override
    public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException {
        boolean isOk = false;

        try {
            int result = internalStmt.executeUpdate(sql, autoGeneratedKeys);
            isOk = true;

            return result;
        } finally {
            poolableConn.updateLastSQLExecutionTime(isOk);
        }
    }

    /**
     *
     * @param sql
     * @param columnIndexes
     * @return int
     * @throws SQLException the SQL exception
     * @see java.sql.Statement#executeUpdate(String, int[])
     */
    @Override
    public int executeUpdate(String sql, int[] columnIndexes) throws SQLException {
        boolean isOk = false;

        try {
            int result = internalStmt.executeUpdate(sql, columnIndexes);
            isOk = true;

            return result;
        } finally {
            poolableConn.updateLastSQLExecutionTime(isOk);
        }
    }

    /**
     *
     * @param sql
     * @param columnNames
     * @return int
     * @throws SQLException the SQL exception
     * @see java.sql.Statement#executeUpdate(String, String[])
     */
    @Override
    public int executeUpdate(String sql, String[] columnNames) throws SQLException {
        boolean isOk = false;

        try {
            int result = internalStmt.executeUpdate(sql, columnNames);
            isOk = true;

            return result;
        } finally {
            poolableConn.updateLastSQLExecutionTime(isOk);
        }
    }

    /**
     *
     * @return ResultSetMetaData
     * @throws SQLException the SQL exception
     * @see java.sql.PreparedStatement#getMetaData()
     */
    @Override
    public ResultSetMetaData getMetaData() throws SQLException {
        return internalStmt.getMetaData();
    }

    /**
     *
     * @return ParameterMetaData
     * @throws SQLException the SQL exception
     * @see java.sql.PreparedStatement#getParameterMetaData()
     */
    @Override
    public ParameterMetaData getParameterMetaData() throws SQLException {
        return internalStmt.getParameterMetaData();
    }

    /**
     *
     * @param parameterIndex
     * @param x
     * @throws SQLException the SQL exception
     * @see java.sql.PreparedStatement#setArray(int, Array)
     */
    @Override
    public void setArray(int parameterIndex, Array x) throws SQLException {
        internalStmt.setArray(parameterIndex, x);
    }

    /**
     *
     * @param parameterIndex
     * @param x
     * @throws SQLException the SQL exception
     * @see java.sql.PreparedStatement#setAsciiStream(int, InputStream)
     */
    @Override
    public void setAsciiStream(int parameterIndex, InputStream x) throws SQLException {
        internalStmt.setAsciiStream(parameterIndex, x);
    }

    /**
     *
     * @param parameterIndex
     * @param x
     * @param length
     * @throws SQLException the SQL exception
     * @see java.sql.PreparedStatement#setAsciiStream(int, InputStream, int)
     */
    @Override
    public void setAsciiStream(int parameterIndex, InputStream x, int length) throws SQLException {
        internalStmt.setAsciiStream(parameterIndex, x, length);
    }

    /**
     *
     * @param parameterIndex
     * @param x
     * @param length
     * @throws SQLException the SQL exception
     * @see java.sql.PreparedStatement#setAsciiStream(int, InputStream, long)
     */
    @Override
    public void setAsciiStream(int parameterIndex, InputStream x, long length) throws SQLException {
        internalStmt.setAsciiStream(parameterIndex, x, length);
    }

    /**
     *
     * @param parameterIndex
     * @param x
     * @throws SQLException the SQL exception
     * @see java.sql.PreparedStatement#setBigDecimal(int, BigDecimal)
     */
    @Override
    public void setBigDecimal(int parameterIndex, BigDecimal x) throws SQLException {
        internalStmt.setBigDecimal(parameterIndex, x);
    }

    /**
     *
     * @param parameterIndex
     * @param x
     * @throws SQLException the SQL exception
     * @see java.sql.PreparedStatement#setBinaryStream(int, InputStream)
     */
    @Override
    public void setBinaryStream(int parameterIndex, InputStream x) throws SQLException {
        internalStmt.setBinaryStream(parameterIndex, x);
    }

    /**
     *
     * @param parameterIndex
     * @param x
     * @param length
     * @throws SQLException the SQL exception
     * @see java.sql.PreparedStatement#setBinaryStream(int, InputStream, int)
     */
    @Override
    public void setBinaryStream(int parameterIndex, InputStream x, int length) throws SQLException {
        internalStmt.setBinaryStream(parameterIndex, x, length);
    }

    /**
     *
     * @param parameterIndex
     * @param x
     * @param length
     * @throws SQLException the SQL exception
     * @see java.sql.PreparedStatement#setBinaryStream(int, InputStream, long)
     */
    @Override
    public void setBinaryStream(int parameterIndex, InputStream x, long length) throws SQLException {
        internalStmt.setBinaryStream(parameterIndex, x, length);
    }

    /**
     *
     * @param parameterIndex
     * @param x
     * @throws SQLException the SQL exception
     * @see java.sql.PreparedStatement#setBlob(int, Blob)
     */
    @Override
    public void setBlob(int parameterIndex, Blob x) throws SQLException {
        internalStmt.setBlob(parameterIndex, x);
    }

    /**
     *
     * @param parameterIndex
     * @param inputStream
     * @throws SQLException the SQL exception
     * @see java.sql.PreparedStatement#setBlob(int, InputStream)
     */
    @Override
    public void setBlob(int parameterIndex, InputStream inputStream) throws SQLException {
        internalStmt.setBlob(parameterIndex, inputStream);
    }

    /**
     *
     * @param parameterIndex
     * @param inputStream
     * @param length
     * @throws SQLException the SQL exception
     * @see java.sql.PreparedStatement#setBlob(int, InputStream, long)
     */
    @Override
    public void setBlob(int parameterIndex, InputStream inputStream, long length) throws SQLException {
        internalStmt.setBlob(parameterIndex, inputStream, length);
    }

    /**
     *
     * @param parameterIndex
     * @param x
     * @throws SQLException the SQL exception
     * @see java.sql.PreparedStatement#setBoolean(int, boolean)
     */
    @Override
    public void setBoolean(int parameterIndex, boolean x) throws SQLException {
        internalStmt.setBoolean(parameterIndex, x);
    }

    /**
     *
     * @param parameterIndex
     * @param x
     * @throws SQLException the SQL exception
     * @see java.sql.PreparedStatement#setByte(int, byte)
     */
    @Override
    public void setByte(int parameterIndex, byte x) throws SQLException {
        internalStmt.setByte(parameterIndex, x);
    }

    /**
     *
     * @param parameterIndex
     * @param x
     * @throws SQLException the SQL exception
     * @see java.sql.PreparedStatement#setBytes(int, byte[])
     */
    @Override
    public void setBytes(int parameterIndex, byte[] x) throws SQLException {
        internalStmt.setBytes(parameterIndex, x);
    }

    /**
     *
     * @param parameterIndex
     * @param reader
     * @throws SQLException the SQL exception
     * @see java.sql.PreparedStatement#setCharacterStream(int, Reader)
     */
    @Override
    public void setCharacterStream(int parameterIndex, Reader reader) throws SQLException {
        internalStmt.setCharacterStream(parameterIndex, reader);
    }

    /**
     *
     * @param parameterIndex
     * @param reader
     * @param length
     * @throws SQLException the SQL exception
     * @see java.sql.PreparedStatement#setCharacterStream(int, Reader, int)
     */
    @Override
    public void setCharacterStream(int parameterIndex, Reader reader, int length) throws SQLException {
        internalStmt.setCharacterStream(parameterIndex, reader, length);
    }

    /**
     *
     * @param parameterIndex
     * @param reader
     * @param length
     * @throws SQLException the SQL exception
     * @see java.sql.PreparedStatement#setCharacterStream(int, Reader, long)
     */
    @Override
    public void setCharacterStream(int parameterIndex, Reader reader, long length) throws SQLException {
        internalStmt.setCharacterStream(parameterIndex, reader, length);
    }

    /**
     *
     * @param parameterIndex
     * @param x
     * @throws SQLException the SQL exception
     * @see java.sql.PreparedStatement#setClob(int, Clob)
     */
    @Override
    public void setClob(int parameterIndex, Clob x) throws SQLException {
        internalStmt.setClob(parameterIndex, x);
    }

    /**
     *
     * @param parameterIndex
     * @param reader
     * @throws SQLException the SQL exception
     * @see java.sql.PreparedStatement#setClob(int, Reader)
     */
    @Override
    public void setClob(int parameterIndex, Reader reader) throws SQLException {
        internalStmt.setClob(parameterIndex, reader);
    }

    /**
     *
     * @param parameterIndex
     * @param reader
     * @param length
     * @throws SQLException the SQL exception
     * @see java.sql.PreparedStatement#setClob(int, Reader, long)
     */
    @Override
    public void setClob(int parameterIndex, Reader reader, long length) throws SQLException {
        internalStmt.setClob(parameterIndex, reader, length);
    }

    /**
     *
     * @param parameterIndex
     * @param x
     * @throws SQLException the SQL exception
     * @see java.sql.PreparedStatement#setDate(int, Date)
     */
    @Override
    public void setDate(int parameterIndex, Date x) throws SQLException {
        internalStmt.setDate(parameterIndex, x);
    }

    /**
     *
     * @param parameterIndex
     * @param x
     * @param cal
     * @throws SQLException the SQL exception
     * @see java.sql.PreparedStatement#setDate(int, Date, Calendar)
     */
    @Override
    public void setDate(int parameterIndex, Date x, Calendar cal) throws SQLException {
        internalStmt.setDate(parameterIndex, x, cal);
    }

    /**
     *
     * @param parameterIndex
     * @param x
     * @throws SQLException the SQL exception
     * @see java.sql.PreparedStatement#setDouble(int, double)
     */
    @Override
    public void setDouble(int parameterIndex, double x) throws SQLException {
        internalStmt.setDouble(parameterIndex, x);
    }

    /**
     *
     * @param parameterIndex
     * @param x
     * @throws SQLException the SQL exception
     * @see java.sql.PreparedStatement#setFloat(int, float)
     */
    @Override
    public void setFloat(int parameterIndex, float x) throws SQLException {
        internalStmt.setFloat(parameterIndex, x);
    }

    /**
     *
     * @param parameterIndex
     * @param x
     * @throws SQLException the SQL exception
     * @see java.sql.PreparedStatement#setInt(int, int)
     */
    @Override
    public void setInt(int parameterIndex, int x) throws SQLException {
        internalStmt.setInt(parameterIndex, x);
    }

    /**
     *
     * @param parameterIndex
     * @param x
     * @throws SQLException the SQL exception
     * @see java.sql.PreparedStatement#setLong(int, long)
     */
    @Override
    public void setLong(int parameterIndex, long x) throws SQLException {
        internalStmt.setLong(parameterIndex, x);
    }

    /**
     *
     * @param parameterIndex
     * @param value
     * @throws SQLException the SQL exception
     * @see java.sql.PreparedStatement#setNCharacterStream(int, Reader)
     */
    @Override
    public void setNCharacterStream(int parameterIndex, Reader value) throws SQLException {
        internalStmt.setNCharacterStream(parameterIndex, value);
    }

    /**
     *
     * @param parameterIndex
     * @param value
     * @param length
     * @throws SQLException the SQL exception
     * @see java.sql.PreparedStatement#setNCharacterStream(int, Reader, long)
     */
    @Override
    public void setNCharacterStream(int parameterIndex, Reader value, long length) throws SQLException {
        internalStmt.setNCharacterStream(parameterIndex, value, length);
    }

    /**
     *
     * @param parameterIndex
     * @param value
     * @throws SQLException the SQL exception
     * @see java.sql.PreparedStatement#setNClob(int, NClob)
     */
    @Override
    public void setNClob(int parameterIndex, NClob value) throws SQLException {
        internalStmt.setNClob(parameterIndex, value);
    }

    /**
     *
     * @param parameterIndex
     * @param reader
     * @throws SQLException the SQL exception
     * @see java.sql.PreparedStatement#setNClob(int, Reader)
     */
    @Override
    public void setNClob(int parameterIndex, Reader reader) throws SQLException {
        internalStmt.setNClob(parameterIndex, reader);
    }

    /**
     *
     * @param parameterIndex
     * @param reader
     * @param length
     * @throws SQLException the SQL exception
     * @see java.sql.PreparedStatement#setNClob(int, Reader, long)
     */
    @Override
    public void setNClob(int parameterIndex, Reader reader, long length) throws SQLException {
        internalStmt.setNClob(parameterIndex, reader, length);
    }

    /**
     *
     * @param parameterIndex
     * @param value
     * @throws SQLException the SQL exception
     * @see java.sql.PreparedStatement#setNString(int, String)
     */
    @Override
    public void setNString(int parameterIndex, String value) throws SQLException {
        internalStmt.setNString(parameterIndex, value);
    }

    /**
     *
     * @param parameterIndex
     * @param sqlType
     * @throws SQLException the SQL exception
     * @see java.sql.PreparedStatement#setNull(int, int)
     */
    @Override
    public void setNull(int parameterIndex, int sqlType) throws SQLException {
        internalStmt.setNull(parameterIndex, sqlType);
    }

    /**
     *
     * @param parameterIndex
     * @param sqlType
     * @param typeName
     * @throws SQLException the SQL exception
     * @see java.sql.PreparedStatement#setNull(int, int, String)
     */
    @Override
    public void setNull(int parameterIndex, int sqlType, String typeName) throws SQLException {
        internalStmt.setNull(parameterIndex, sqlType, typeName);
    }

    /**
     *
     * @param parameterIndex
     * @param x
     * @throws SQLException the SQL exception
     * @see java.sql.PreparedStatement#setObject(int, Object)
     */
    @Override
    public void setObject(int parameterIndex, Object x) throws SQLException {
        internalStmt.setObject(parameterIndex, x);
    }

    /**
     *
     * @param parameterIndex
     * @param x
     * @param targetSqlType
     * @throws SQLException the SQL exception
     * @see java.sql.PreparedStatement#setObject(int, Object, int)
     */
    @Override
    public void setObject(int parameterIndex, Object x, int targetSqlType) throws SQLException {
        internalStmt.setObject(parameterIndex, x, targetSqlType);
    }

    /**
     *
     * @param parameterIndex
     * @param x
     * @param targetSqlType
     * @param scaleOrLength
     * @throws SQLException the SQL exception
     * @see java.sql.PreparedStatement#setObject(int, Object, int, int)
     */
    @Override
    public void setObject(int parameterIndex, Object x, int targetSqlType, int scaleOrLength) throws SQLException {
        internalStmt.setObject(parameterIndex, x, targetSqlType, scaleOrLength);
    }

    /**
     *
     * @param parameterIndex
     * @param x
     * @throws SQLException the SQL exception
     * @see java.sql.PreparedStatement#setRef(int, Ref)
     */
    @Override
    public void setRef(int parameterIndex, Ref x) throws SQLException {
        internalStmt.setRef(parameterIndex, x);
    }

    /**
     *
     * @param parameterIndex
     * @param x
     * @throws SQLException the SQL exception
     * @see java.sql.PreparedStatement#setRowId(int, RowId)
     */
    @Override
    public void setRowId(int parameterIndex, RowId x) throws SQLException {
        internalStmt.setRowId(parameterIndex, x);
    }

    /**
     *
     * @param parameterIndex
     * @param xmlObject
     * @throws SQLException the SQL exception
     * @see java.sql.PreparedStatement#setSQLXML(int, SQLXML)
     */
    @Override
    public void setSQLXML(int parameterIndex, SQLXML xmlObject) throws SQLException {
        internalStmt.setSQLXML(parameterIndex, xmlObject);
    }

    /**
     *
     * @param parameterIndex
     * @param x
     * @throws SQLException the SQL exception
     * @see java.sql.PreparedStatement#setShort(int, short)
     */
    @Override
    public void setShort(int parameterIndex, short x) throws SQLException {
        internalStmt.setShort(parameterIndex, x);
    }

    /**
     *
     * @param parameterIndex
     * @param x
     * @throws SQLException the SQL exception
     * @see java.sql.PreparedStatement#setString(int, String)
     */
    @Override
    public void setString(int parameterIndex, String x) throws SQLException {
        internalStmt.setString(parameterIndex, x);
    }

    /**
     *
     * @param parameterIndex
     * @param x
     * @throws SQLException the SQL exception
     * @see java.sql.PreparedStatement#setTime(int, Time)
     */
    @Override
    public void setTime(int parameterIndex, Time x) throws SQLException {
        internalStmt.setTime(parameterIndex, x);
    }

    /**
     *
     * @param parameterIndex
     * @param x
     * @param cal
     * @throws SQLException the SQL exception
     * @see java.sql.PreparedStatement#setTime(int, Time, Calendar)
     */
    @Override
    public void setTime(int parameterIndex, Time x, Calendar cal) throws SQLException {
        internalStmt.setTime(parameterIndex, x, cal);
    }

    /**
     *
     * @param parameterIndex
     * @param x
     * @throws SQLException the SQL exception
     * @see java.sql.PreparedStatement#setTimestamp(int, Timestamp)
     */
    @Override
    public void setTimestamp(int parameterIndex, Timestamp x) throws SQLException {
        internalStmt.setTimestamp(parameterIndex, x);
    }

    /**
     *
     * @param parameterIndex
     * @param x
     * @param cal
     * @throws SQLException the SQL exception
     * @see java.sql.PreparedStatement#setTimestamp(int, Timestamp, Calendar)
     */
    @Override
    public void setTimestamp(int parameterIndex, Timestamp x, Calendar cal) throws SQLException {
        internalStmt.setTimestamp(parameterIndex, x, cal);
    }

    /**
     *
     * @param parameterIndex
     * @param x
     * @throws SQLException the SQL exception
     * @see java.sql.PreparedStatement#setURL(int, URL)
     */
    @Override
    public void setURL(int parameterIndex, URL x) throws SQLException {
        internalStmt.setURL(parameterIndex, x);
    }

    /**
     *
     * @param parameterIndex
     * @param x
     * @param length
     * @throws SQLException the SQL exception
     * @see java.sql.PreparedStatement#setUnicodeStream(int, InputStream, int)
     */
    @Override
    @SuppressWarnings("deprecation")
    public void setUnicodeStream(int parameterIndex, InputStream x, int length) throws SQLException {
        internalStmt.setUnicodeStream(parameterIndex, x, length);
    }

    /**
     *
     * @param sql
     * @throws SQLException the SQL exception
     * @see java.sql.Statement#addBatch(String)
     */
    @Override
    public void addBatch(String sql) throws SQLException {
        internalStmt.addBatch(sql);
    }

    /**
     *
     * @throws SQLException the SQL exception
     * @see java.sql.Statement#cancel()
     */
    @Override
    public void cancel() throws SQLException {
        internalStmt.cancel();
    }

    /**
     *
     * @throws SQLException the SQL exception
     * @see java.sql.Statement#clearBatch()
     */
    @Override
    public void clearBatch() throws SQLException {
        internalStmt.clearBatch();
    }

    /**
     *
     * @throws SQLException the SQL exception
     * @see java.sql.Statement#clearWarnings()
     */
    @Override
    public void clearWarnings() throws SQLException {
        internalStmt.clearWarnings();
    }

    /**
     *
     * @return java.sql.Connection
     * @throws SQLException the SQL exception
     * @see java.sql.Statement#getConnection()
     */
    @Override
    public PoolableConnection getConnection() throws SQLException {
        return poolableConn;
    }

    /**
     *
     * @return int
     * @throws SQLException the SQL exception
     * @see java.sql.Statement#getFetchDirection()
     */
    @Override
    public int getFetchDirection() throws SQLException {
        return internalStmt.getFetchDirection();
    }

    /**
     *
     * @return int
     * @throws SQLException the SQL exception
     * @see java.sql.Statement#getFetchSize()
     */
    @Override
    public int getFetchSize() throws SQLException {
        return internalStmt.getFetchSize();
    }

    /**
     *
     * @return ResultSet
     * @throws SQLException the SQL exception
     * @see java.sql.Statement#getGeneratedKeys()
     */
    @Override
    public ResultSet getGeneratedKeys() throws SQLException {
        return wrap(internalStmt.getGeneratedKeys());
    }

    /**
     *
     * @return int
     * @throws SQLException the SQL exception
     * @see java.sql.Statement#getMaxFieldSize()
     */
    @Override
    public int getMaxFieldSize() throws SQLException {
        return internalStmt.getMaxFieldSize();
    }

    /**
     *
     * @return int
     * @throws SQLException the SQL exception
     * @see java.sql.Statement#getMaxRows()
     */
    @Override
    public int getMaxRows() throws SQLException {
        return internalStmt.getMaxRows();
    }

    /**
     *
     * @return boolean
     * @throws SQLException the SQL exception
     * @see java.sql.Statement#getMoreResults()
     */
    @Override
    public boolean getMoreResults() throws SQLException {
        return internalStmt.getMoreResults();
    }

    /**
     *
     * @param current
     * @return boolean
     * @throws SQLException the SQL exception
     * @see java.sql.Statement#getMoreResults(int)
     */
    @Override
    public boolean getMoreResults(int current) throws SQLException {
        return internalStmt.getMoreResults(current);
    }

    /**
     *
     * @return int
     * @throws SQLException the SQL exception
     * @see java.sql.Statement#getQueryTimeout()
     */
    @Override
    public int getQueryTimeout() throws SQLException {
        return internalStmt.getQueryTimeout();
    }

    /**
     *
     * @return ResultSet
     * @throws SQLException the SQL exception
     * @see java.sql.Statement#getResultSet()
     */
    @Override
    public ResultSet getResultSet() throws SQLException {
        return wrap(internalStmt.getResultSet());
    }

    /**
     *
     * @return int
     * @throws SQLException the SQL exception
     * @see java.sql.Statement#getResultSetConcurrency()
     */
    @Override
    public int getResultSetConcurrency() throws SQLException {
        return internalStmt.getResultSetConcurrency();
    }

    /**
     *
     * @return int
     * @throws SQLException the SQL exception
     * @see java.sql.Statement#getResultSetHoldability()
     */
    @Override
    public int getResultSetHoldability() throws SQLException {
        return internalStmt.getResultSetHoldability();
    }

    /**
     *
     * @return int
     * @throws SQLException the SQL exception
     * @see java.sql.Statement#getResultSetType()
     */
    @Override
    public int getResultSetType() throws SQLException {
        return internalStmt.getResultSetType();
    }

    /**
     *
     * @return int
     * @throws SQLException the SQL exception
     * @see java.sql.Statement#getUpdateCount()
     */
    @Override
    public int getUpdateCount() throws SQLException {
        return internalStmt.getUpdateCount();
    }

    /**
     *
     * @return SQLWarning
     * @throws SQLException the SQL exception
     * @see java.sql.Statement#getWarnings()
     */
    @Override
    public SQLWarning getWarnings() throws SQLException {
        return internalStmt.getWarnings();
    }

    /**
     *
     * @param name the new cursor name
     * @throws SQLException the SQL exception
     * @see java.sql.Statement#setCursorName(String)
     */
    @Override
    public void setCursorName(String name) throws SQLException {
        internalStmt.setCursorName(name);
    }

    /**
     *
     * @param enable the new escape processing
     * @throws SQLException the SQL exception
     * @see java.sql.Statement#setEscapeProcessing(boolean)
     */
    @Override
    public void setEscapeProcessing(boolean enable) throws SQLException {
        internalStmt.setEscapeProcessing(enable);
    }

    /**
     *
     * @param direction the new fetch direction
     * @throws SQLException the SQL exception
     * @see java.sql.Statement#setFetchDirection(int)
     */
    @Override
    public void setFetchDirection(int direction) throws SQLException {
        this.fetchDirection = internalStmt.getFetchDirection();
        internalStmt.setFetchDirection(direction);
    }

    /**
     *
     * @param rows the new fetch size
     * @throws SQLException the SQL exception
     * @see java.sql.Statement#setFetchSize(int)
     */
    @Override
    public void setFetchSize(int rows) throws SQLException {
        this.fetchSize = internalStmt.getFetchSize();
        internalStmt.setFetchSize(rows);
    }

    /**
     *
     * @param max the new max field size
     * @throws SQLException the SQL exception
     * @see java.sql.Statement#setMaxFieldSize(int)
     */
    @Override
    public void setMaxFieldSize(int max) throws SQLException {
        this.maxFieldSize = internalStmt.getMaxFieldSize();
        internalStmt.setMaxFieldSize(max);
    }

    /**
     *
     * @param max the new max rows
     * @throws SQLException the SQL exception
     * @see java.sql.Statement#setMaxRows(int)
     */
    @Override
    public void setMaxRows(int max) throws SQLException {
        this.maxRows = internalStmt.getMaxRows();
        internalStmt.setMaxRows(max);
    }

    /**
     *
     * @param seconds the new query timeout
     * @throws SQLException the SQL exception
     * @see java.sql.Statement#setQueryTimeout(int)
     */
    @Override
    public void setQueryTimeout(int seconds) throws SQLException {
        this.queryTimeout = internalStmt.getQueryTimeout();
        internalStmt.setQueryTimeout(seconds);
    }

    /**
     *
     * @param poolable the new poolable
     * @throws SQLException the SQL exception
     * @see java.sql.Statement#setPoolable(boolean)
     */
    @Override
    public void setPoolable(boolean poolable) throws SQLException {
        internalStmt.setPoolable(poolable);
    }

    /**
     *
     * @param iface
     * @return boolean
     * @throws SQLException the SQL exception
     * @see java.sql.Wrapper#isWrapperFor(Class<?>)
     */
    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return internalStmt.isWrapperFor(iface);
    }

    /**
     *
     * @param <T>
     * @param iface
     * @return T
     * @throws SQLException the SQL exception
     * @see java.sql.Wrapper#unwrap(Class<T>)
     */
    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return internalStmt.unwrap(iface);
    }

    /**
     * 
     * @return String
     */
    @Override
    public String toString() {
        return internalStmt.toString();
    }

    /**
     * 
     * @return int
     */
    @Override
    public int hashCode() {
        return internalStmt.hashCode();
    }

    /**
     *
     * @param obj
     * @return boolean
     */
    @Override
    public boolean equals(Object obj) {
        return this == obj || (obj instanceof PoolablePreparedStatement && ((PoolablePreparedStatement) obj).internalStmt.equals(internalStmt));
    }

    /**
     * Close on completion.
     *
     * @throws SQLException the SQL exception
     */
    @Override
    public void closeOnCompletion() throws SQLException {
        internalStmt.closeOnCompletion();
    }

    /**
     * Checks if is close on completion.
     *
     * @return true, if is close on completion
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean isCloseOnCompletion() throws SQLException {
        return internalStmt.isCloseOnCompletion();
    }

    /** The fetch size. */
    private int fetchSize = -1;

    /** The fetch direction. */
    private int fetchDirection = -1;

    /** The max rows. */
    private int maxRows = -1;

    /** The max field size. */
    private int maxFieldSize = -1;

    /** The query timeout. */
    private int queryTimeout = -1;

    /**
     *
     * @throws SQLException the SQL exception
     */
    protected void reset() throws SQLException {
        //    internalStmt.clearParameters();
        //    internalStmt.clearBatch();
        //    internalStmt.clearWarnings();

        if (fetchSize != -1) {
            internalStmt.setFetchSize(fetchSize);
        }

        if (fetchDirection != -1) {
            internalStmt.setFetchDirection(fetchDirection);
        }

        if (maxRows != -1) {
            internalStmt.setMaxRows(maxRows);
        }

        if (maxFieldSize != -1) {
            internalStmt.setMaxFieldSize(maxFieldSize);
        }

        if (queryTimeout != -1) {
            internalStmt.setQueryTimeout(queryTimeout);
        }
    }

    /**
     * The Class ResultSetProxy.
     */
    static class ResultSetProxy implements ResultSet {

        /** The internal RS. */
        private final ResultSet internalRS;

        /** The poolable stmt. */
        private final PoolablePreparedStatement poolableStmt;

        /**
         * Instantiates a new result set proxy.
         *
         * @param rs
         * @param stmt
         */
        ResultSetProxy(ResultSet rs, PoolablePreparedStatement stmt) {
            this.internalRS = rs;
            this.poolableStmt = stmt;
        }

        /**
         *
         * @param <T>
         * @param iface
         * @return
         * @throws SQLException the SQL exception
         */
        @Override
        public <T> T unwrap(Class<T> iface) throws SQLException {
            return internalRS.unwrap(iface);
        }

        /**
         * Checks if is wrapper for.
         *
         * @param iface
         * @return true, if is wrapper for
         * @throws SQLException the SQL exception
         */
        @Override
        public boolean isWrapperFor(Class<?> iface) throws SQLException {
            return internalRS.isWrapperFor(iface);
        }

        /**
         *
         * @return true, if successful
         * @throws SQLException the SQL exception
         */
        @Override
        public boolean next() throws SQLException {
            return internalRS.next();
        }

        /**
         *
         * @throws SQLException the SQL exception
         */
        @Override
        public void close() throws SQLException {
            internalRS.close();
        }

        /**
         *
         * @return true, if successful
         * @throws SQLException the SQL exception
         */
        @Override
        public boolean wasNull() throws SQLException {
            return internalRS.wasNull();
        }

        /**
         * Gets the string.
         *
         * @param columnIndex
         * @return
         * @throws SQLException the SQL exception
         */
        @Override
        public String getString(int columnIndex) throws SQLException {
            return internalRS.getString(columnIndex);
        }

        /**
         * Gets the boolean.
         *
         * @param columnIndex
         * @return
         * @throws SQLException the SQL exception
         */
        @Override
        public boolean getBoolean(int columnIndex) throws SQLException {
            return internalRS.getBoolean(columnIndex);
        }

        /**
         * Gets the byte.
         *
         * @param columnIndex
         * @return
         * @throws SQLException the SQL exception
         */
        @Override
        public byte getByte(int columnIndex) throws SQLException {
            return internalRS.getByte(columnIndex);
        }

        /**
         * Gets the short.
         *
         * @param columnIndex
         * @return
         * @throws SQLException the SQL exception
         */
        @Override
        public short getShort(int columnIndex) throws SQLException {
            return internalRS.getShort(columnIndex);
        }

        /**
         * Gets the int.
         *
         * @param columnIndex
         * @return
         * @throws SQLException the SQL exception
         */
        @Override
        public int getInt(int columnIndex) throws SQLException {
            return internalRS.getInt(columnIndex);
        }

        /**
         * Gets the long.
         *
         * @param columnIndex
         * @return
         * @throws SQLException the SQL exception
         */
        @Override
        public long getLong(int columnIndex) throws SQLException {
            return internalRS.getLong(columnIndex);
        }

        /**
         * Gets the float.
         *
         * @param columnIndex
         * @return
         * @throws SQLException the SQL exception
         */
        @Override
        public float getFloat(int columnIndex) throws SQLException {
            return internalRS.getFloat(columnIndex);
        }

        /**
         * Gets the double.
         *
         * @param columnIndex
         * @return
         * @throws SQLException the SQL exception
         */
        @Override
        public double getDouble(int columnIndex) throws SQLException {
            return internalRS.getDouble(columnIndex);
        }

        /**
         * Gets the big decimal.
         *
         * @param columnIndex
         * @param scale
         * @return
         * @throws SQLException the SQL exception
         */
        @SuppressWarnings("deprecation")
        @Override
        public BigDecimal getBigDecimal(int columnIndex, int scale) throws SQLException {
            return internalRS.getBigDecimal(columnIndex, scale);
        }

        /**
         * Gets the bytes.
         *
         * @param columnIndex
         * @return
         * @throws SQLException the SQL exception
         */
        @Override
        public byte[] getBytes(int columnIndex) throws SQLException {
            return internalRS.getBytes(columnIndex);
        }

        /**
         * Gets the date.
         *
         * @param columnIndex
         * @return
         * @throws SQLException the SQL exception
         */
        @Override
        public Date getDate(int columnIndex) throws SQLException {
            return internalRS.getDate(columnIndex);
        }

        /**
         * Gets the time.
         *
         * @param columnIndex
         * @return
         * @throws SQLException the SQL exception
         */
        @Override
        public Time getTime(int columnIndex) throws SQLException {
            return internalRS.getTime(columnIndex);
        }

        /**
         * Gets the timestamp.
         *
         * @param columnIndex
         * @return
         * @throws SQLException the SQL exception
         */
        @Override
        public Timestamp getTimestamp(int columnIndex) throws SQLException {
            return internalRS.getTimestamp(columnIndex);
        }

        /**
         * Gets the ascii stream.
         *
         * @param columnIndex
         * @return
         * @throws SQLException the SQL exception
         */
        @Override
        public InputStream getAsciiStream(int columnIndex) throws SQLException {
            return internalRS.getAsciiStream(columnIndex);
        }

        /**
         * Gets the unicode stream.
         *
         * @param columnIndex
         * @return
         * @throws SQLException the SQL exception
         */
        @SuppressWarnings("deprecation")
        @Override
        public InputStream getUnicodeStream(int columnIndex) throws SQLException {
            return internalRS.getUnicodeStream(columnIndex);
        }

        /**
         * Gets the binary stream.
         *
         * @param columnIndex
         * @return
         * @throws SQLException the SQL exception
         */
        @Override
        public InputStream getBinaryStream(int columnIndex) throws SQLException {
            return internalRS.getBinaryStream(columnIndex);
        }

        /**
         * Gets the string.
         *
         * @param columnLabel
         * @return
         * @throws SQLException the SQL exception
         */
        @Override
        public String getString(String columnLabel) throws SQLException {
            return internalRS.getString(columnLabel);
        }

        /**
         * Gets the boolean.
         *
         * @param columnLabel
         * @return
         * @throws SQLException the SQL exception
         */
        @Override
        public boolean getBoolean(String columnLabel) throws SQLException {
            return internalRS.getBoolean(columnLabel);
        }

        /**
         * Gets the byte.
         *
         * @param columnLabel
         * @return
         * @throws SQLException the SQL exception
         */
        @Override
        public byte getByte(String columnLabel) throws SQLException {
            return internalRS.getByte(columnLabel);
        }

        /**
         * Gets the short.
         *
         * @param columnLabel
         * @return
         * @throws SQLException the SQL exception
         */
        @Override
        public short getShort(String columnLabel) throws SQLException {
            return internalRS.getShort(columnLabel);
        }

        /**
         * Gets the int.
         *
         * @param columnLabel
         * @return
         * @throws SQLException the SQL exception
         */
        @Override
        public int getInt(String columnLabel) throws SQLException {
            return internalRS.getInt(columnLabel);
        }

        /**
         * Gets the long.
         *
         * @param columnLabel
         * @return
         * @throws SQLException the SQL exception
         */
        @Override
        public long getLong(String columnLabel) throws SQLException {
            return internalRS.getLong(columnLabel);
        }

        /**
         * Gets the float.
         *
         * @param columnLabel
         * @return
         * @throws SQLException the SQL exception
         */
        @Override
        public float getFloat(String columnLabel) throws SQLException {
            return internalRS.getFloat(columnLabel);
        }

        /**
         * Gets the double.
         *
         * @param columnLabel
         * @return
         * @throws SQLException the SQL exception
         */
        @Override
        public double getDouble(String columnLabel) throws SQLException {
            return internalRS.getDouble(columnLabel);
        }

        /**
         * Gets the big decimal.
         *
         * @param columnLabel
         * @param scale
         * @return
         * @throws SQLException the SQL exception
         */
        @SuppressWarnings("deprecation")
        @Override
        public BigDecimal getBigDecimal(String columnLabel, int scale) throws SQLException {
            return internalRS.getBigDecimal(columnLabel, scale);
        }

        /**
         * Gets the bytes.
         *
         * @param columnLabel
         * @return
         * @throws SQLException the SQL exception
         */
        @Override
        public byte[] getBytes(String columnLabel) throws SQLException {
            return internalRS.getBytes(columnLabel);
        }

        /**
         * Gets the date.
         *
         * @param columnLabel
         * @return
         * @throws SQLException the SQL exception
         */
        @Override
        public Date getDate(String columnLabel) throws SQLException {
            return internalRS.getDate(columnLabel);
        }

        /**
         * Gets the time.
         *
         * @param columnLabel
         * @return
         * @throws SQLException the SQL exception
         */
        @Override
        public Time getTime(String columnLabel) throws SQLException {
            return internalRS.getTime(columnLabel);
        }

        /**
         * Gets the timestamp.
         *
         * @param columnLabel
         * @return
         * @throws SQLException the SQL exception
         */
        @Override
        public Timestamp getTimestamp(String columnLabel) throws SQLException {
            return internalRS.getTimestamp(columnLabel);
        }

        /**
         * Gets the ascii stream.
         *
         * @param columnLabel
         * @return
         * @throws SQLException the SQL exception
         */
        @Override
        public InputStream getAsciiStream(String columnLabel) throws SQLException {
            return internalRS.getAsciiStream(columnLabel);
        }

        /**
         * Gets the unicode stream.
         *
         * @param columnLabel
         * @return
         * @throws SQLException the SQL exception
         */
        @SuppressWarnings("deprecation")
        @Override
        public InputStream getUnicodeStream(String columnLabel) throws SQLException {
            return internalRS.getUnicodeStream(columnLabel);
        }

        /**
         * Gets the binary stream.
         *
         * @param columnLabel
         * @return
         * @throws SQLException the SQL exception
         */
        @Override
        public InputStream getBinaryStream(String columnLabel) throws SQLException {
            return internalRS.getBinaryStream(columnLabel);
        }

        /**
         * Gets the warnings.
         *
         * @return
         * @throws SQLException the SQL exception
         */
        @Override
        public SQLWarning getWarnings() throws SQLException {
            return internalRS.getWarnings();
        }

        /**
         *
         * @throws SQLException the SQL exception
         */
        @Override
        public void clearWarnings() throws SQLException {
            internalRS.clearWarnings();
        }

        /**
         * Gets the cursor name.
         *
         * @return
         * @throws SQLException the SQL exception
         */
        @Override
        public String getCursorName() throws SQLException {
            return internalRS.getCursorName();
        }

        /**
         * Gets the meta data.
         *
         * @return
         * @throws SQLException the SQL exception
         */
        @Override
        public ResultSetMetaData getMetaData() throws SQLException {
            return internalRS.getMetaData();
        }

        /**
         * Gets the object.
         *
         * @param columnIndex
         * @return
         * @throws SQLException the SQL exception
         */
        @Override
        public Object getObject(int columnIndex) throws SQLException {
            return internalRS.getObject(columnIndex);
        }

        /**
         * Gets the object.
         *
         * @param columnLabel
         * @return
         * @throws SQLException the SQL exception
         */
        @Override
        public Object getObject(String columnLabel) throws SQLException {
            return internalRS.getObject(columnLabel);
        }

        /**
         *
         * @param columnLabel
         * @return
         * @throws SQLException the SQL exception
         */
        @Override
        public int findColumn(String columnLabel) throws SQLException {
            return internalRS.findColumn(columnLabel);
        }

        /**
         * Gets the character stream.
         *
         * @param columnIndex
         * @return
         * @throws SQLException the SQL exception
         */
        @Override
        public Reader getCharacterStream(int columnIndex) throws SQLException {
            return internalRS.getCharacterStream(columnIndex);
        }

        /**
         * Gets the character stream.
         *
         * @param columnLabel
         * @return
         * @throws SQLException the SQL exception
         */
        @Override
        public Reader getCharacterStream(String columnLabel) throws SQLException {
            return internalRS.getCharacterStream(columnLabel);
        }

        /**
         * Gets the big decimal.
         *
         * @param columnIndex
         * @return
         * @throws SQLException the SQL exception
         */
        @Override
        public BigDecimal getBigDecimal(int columnIndex) throws SQLException {
            return internalRS.getBigDecimal(columnIndex);
        }

        /**
         * Gets the big decimal.
         *
         * @param columnLabel
         * @return
         * @throws SQLException the SQL exception
         */
        @Override
        public BigDecimal getBigDecimal(String columnLabel) throws SQLException {
            return internalRS.getBigDecimal(columnLabel);
        }

        /**
         * Checks if is before first.
         *
         * @return true, if is before first
         * @throws SQLException the SQL exception
         */
        @Override
        public boolean isBeforeFirst() throws SQLException {
            return internalRS.isBeforeFirst();
        }

        /**
         * Checks if is after last.
         *
         * @return true, if is after last
         * @throws SQLException the SQL exception
         */
        @Override
        public boolean isAfterLast() throws SQLException {
            return internalRS.isAfterLast();
        }

        /**
         * Checks if is first.
         *
         * @return true, if is first
         * @throws SQLException the SQL exception
         */
        @Override
        public boolean isFirst() throws SQLException {
            return internalRS.isFirst();
        }

        /**
         * Checks if is last.
         *
         * @return true, if is last
         * @throws SQLException the SQL exception
         */
        @Override
        public boolean isLast() throws SQLException {
            return internalRS.isLast();
        }

        /**
         *
         * @throws SQLException the SQL exception
         */
        @Override
        public void beforeFirst() throws SQLException {
            internalRS.beforeFirst();
        }

        /**
         *
         * @throws SQLException the SQL exception
         */
        @Override
        public void afterLast() throws SQLException {
            internalRS.afterLast();
        }

        /**
         *
         * @return true, if successful
         * @throws SQLException the SQL exception
         */
        @Override
        public boolean first() throws SQLException {
            return internalRS.first();
        }

        /**
         *
         * @return true, if successful
         * @throws SQLException the SQL exception
         */
        @Override
        public boolean last() throws SQLException {
            return internalRS.last();
        }

        /**
         * Gets the row.
         *
         * @return
         * @throws SQLException the SQL exception
         */
        @Override
        public int getRow() throws SQLException {
            return internalRS.getRow();
        }

        /**
         *
         * @param row
         * @return true, if successful
         * @throws SQLException the SQL exception
         */
        @Override
        public boolean absolute(int row) throws SQLException {
            return internalRS.absolute(row);
        }

        /**
         *
         * @param rows
         * @return true, if successful
         * @throws SQLException the SQL exception
         */
        @Override
        public boolean relative(int rows) throws SQLException {
            return internalRS.relative(rows);
        }

        /**
         *
         * @return true, if successful
         * @throws SQLException the SQL exception
         */
        @Override
        public boolean previous() throws SQLException {
            return internalRS.previous();
        }

        /**
         * Sets the fetch direction.
         *
         * @param direction the new fetch direction
         * @throws SQLException the SQL exception
         */
        @Override
        public void setFetchDirection(int direction) throws SQLException {
            internalRS.setFetchDirection(direction);
        }

        /**
         * Gets the fetch direction.
         *
         * @return
         * @throws SQLException the SQL exception
         */
        @Override
        public int getFetchDirection() throws SQLException {
            return internalRS.getFetchDirection();
        }

        /**
         * Sets the fetch size.
         *
         * @param rows the new fetch size
         * @throws SQLException the SQL exception
         */
        @Override
        public void setFetchSize(int rows) throws SQLException {
            internalRS.setFetchSize(rows);
        }

        /**
         * Gets the fetch size.
         *
         * @return
         * @throws SQLException the SQL exception
         */
        @Override
        public int getFetchSize() throws SQLException {
            return internalRS.getFetchSize();
        }

        /**
         * Gets the type.
         *
         * @return
         * @throws SQLException the SQL exception
         */
        @Override
        public int getType() throws SQLException {
            return internalRS.getType();
        }

        /**
         * Gets the concurrency.
         *
         * @return
         * @throws SQLException the SQL exception
         */
        @Override
        public int getConcurrency() throws SQLException {
            return internalRS.getConcurrency();
        }

        /**
         *
         * @return true, if successful
         * @throws SQLException the SQL exception
         */
        @Override
        public boolean rowUpdated() throws SQLException {
            return internalRS.rowUpdated();
        }

        /**
         *
         * @return true, if successful
         * @throws SQLException the SQL exception
         */
        @Override
        public boolean rowInserted() throws SQLException {
            return internalRS.rowInserted();
        }

        /**
         *
         * @return true, if successful
         * @throws SQLException the SQL exception
         */
        @Override
        public boolean rowDeleted() throws SQLException {
            return internalRS.rowDeleted();
        }

        /**
         *
         * @param columnIndex
         * @throws SQLException the SQL exception
         */
        @Override
        public void updateNull(int columnIndex) throws SQLException {
            internalRS.updateNull(columnIndex);
        }

        /**
         *
         * @param columnIndex
         * @param x
         * @throws SQLException the SQL exception
         */
        @Override
        public void updateBoolean(int columnIndex, boolean x) throws SQLException {
            internalRS.updateBoolean(columnIndex, x);
        }

        /**
         *
         * @param columnIndex
         * @param x
         * @throws SQLException the SQL exception
         */
        @Override
        public void updateByte(int columnIndex, byte x) throws SQLException {
            internalRS.updateByte(columnIndex, x);
        }

        /**
         *
         * @param columnIndex
         * @param x
         * @throws SQLException the SQL exception
         */
        @Override
        public void updateShort(int columnIndex, short x) throws SQLException {
            internalRS.updateShort(columnIndex, x);
        }

        /**
         *
         * @param columnIndex
         * @param x
         * @throws SQLException the SQL exception
         */
        @Override
        public void updateInt(int columnIndex, int x) throws SQLException {
            internalRS.updateInt(columnIndex, x);
        }

        /**
         *
         * @param columnIndex
         * @param x
         * @throws SQLException the SQL exception
         */
        @Override
        public void updateLong(int columnIndex, long x) throws SQLException {
            internalRS.updateLong(columnIndex, x);
        }

        /**
         *
         * @param columnIndex
         * @param x
         * @throws SQLException the SQL exception
         */
        @Override
        public void updateFloat(int columnIndex, float x) throws SQLException {
            internalRS.updateFloat(columnIndex, x);
        }

        /**
         *
         * @param columnIndex
         * @param x
         * @throws SQLException the SQL exception
         */
        @Override
        public void updateDouble(int columnIndex, double x) throws SQLException {
            internalRS.updateDouble(columnIndex, x);
        }

        /**
         * Update big decimal.
         *
         * @param columnIndex
         * @param x
         * @throws SQLException the SQL exception
         */
        @Override
        public void updateBigDecimal(int columnIndex, BigDecimal x) throws SQLException {
            internalRS.updateBigDecimal(columnIndex, x);
        }

        /**
         *
         * @param columnIndex
         * @param x
         * @throws SQLException the SQL exception
         */
        @Override
        public void updateString(int columnIndex, String x) throws SQLException {
            internalRS.updateString(columnIndex, x);
        }

        /**
         *
         * @param columnIndex
         * @param x
         * @throws SQLException the SQL exception
         */
        @Override
        public void updateBytes(int columnIndex, byte[] x) throws SQLException {
            internalRS.updateBytes(columnIndex, x);
        }

        /**
         *
         * @param columnIndex
         * @param x
         * @throws SQLException the SQL exception
         */
        @Override
        public void updateDate(int columnIndex, Date x) throws SQLException {
            internalRS.updateDate(columnIndex, x);
        }

        /**
         *
         * @param columnIndex
         * @param x
         * @throws SQLException the SQL exception
         */
        @Override
        public void updateTime(int columnIndex, Time x) throws SQLException {
            internalRS.updateTime(columnIndex, x);
        }

        /**
         *
         * @param columnIndex
         * @param x
         * @throws SQLException the SQL exception
         */
        @Override
        public void updateTimestamp(int columnIndex, Timestamp x) throws SQLException {
            internalRS.updateTimestamp(columnIndex, x);
        }

        /**
         * Update ascii stream.
         *
         * @param columnIndex
         * @param x
         * @param length
         * @throws SQLException the SQL exception
         */
        @Override
        public void updateAsciiStream(int columnIndex, InputStream x, int length) throws SQLException {
            internalRS.updateAsciiStream(columnIndex, x, length);
        }

        /**
         * Update binary stream.
         *
         * @param columnIndex
         * @param x
         * @param length
         * @throws SQLException the SQL exception
         */
        @Override
        public void updateBinaryStream(int columnIndex, InputStream x, int length) throws SQLException {
            internalRS.updateBinaryStream(columnIndex, x, length);
        }

        /**
         * Update character stream.
         *
         * @param columnIndex
         * @param x
         * @param length
         * @throws SQLException the SQL exception
         */
        @Override
        public void updateCharacterStream(int columnIndex, Reader x, int length) throws SQLException {
            internalRS.updateCharacterStream(columnIndex, x, length);
        }

        /**
         *
         * @param columnIndex
         * @param x
         * @param scaleOrLength
         * @throws SQLException the SQL exception
         */
        @Override
        public void updateObject(int columnIndex, Object x, int scaleOrLength) throws SQLException {
            internalRS.updateObject(columnIndex, x, scaleOrLength);
        }

        /**
         *
         * @param columnIndex
         * @param x
         * @throws SQLException the SQL exception
         */
        @Override
        public void updateObject(int columnIndex, Object x) throws SQLException {
            internalRS.updateObject(columnIndex, x);
        }

        /**
         *
         * @param columnLabel
         * @throws SQLException the SQL exception
         */
        @Override
        public void updateNull(String columnLabel) throws SQLException {
            internalRS.updateNull(columnLabel);
        }

        /**
         *
         * @param columnLabel
         * @param x
         * @throws SQLException the SQL exception
         */
        @Override
        public void updateBoolean(String columnLabel, boolean x) throws SQLException {
            internalRS.updateBoolean(columnLabel, x);
        }

        /**
         *
         * @param columnLabel
         * @param x
         * @throws SQLException the SQL exception
         */
        @Override
        public void updateByte(String columnLabel, byte x) throws SQLException {
            internalRS.updateByte(columnLabel, x);
        }

        /**
         *
         * @param columnLabel
         * @param x
         * @throws SQLException the SQL exception
         */
        @Override
        public void updateShort(String columnLabel, short x) throws SQLException {
            internalRS.updateShort(columnLabel, x);
        }

        /**
         *
         * @param columnLabel
         * @param x
         * @throws SQLException the SQL exception
         */
        @Override
        public void updateInt(String columnLabel, int x) throws SQLException {
            internalRS.updateInt(columnLabel, x);
        }

        /**
         *
         * @param columnLabel
         * @param x
         * @throws SQLException the SQL exception
         */
        @Override
        public void updateLong(String columnLabel, long x) throws SQLException {
            internalRS.updateLong(columnLabel, x);
        }

        /**
         *
         * @param columnLabel
         * @param x
         * @throws SQLException the SQL exception
         */
        @Override
        public void updateFloat(String columnLabel, float x) throws SQLException {
            internalRS.updateFloat(columnLabel, x);
        }

        /**
         *
         * @param columnLabel
         * @param x
         * @throws SQLException the SQL exception
         */
        @Override
        public void updateDouble(String columnLabel, double x) throws SQLException {
            internalRS.updateDouble(columnLabel, x);
        }

        /**
         * Update big decimal.
         *
         * @param columnLabel
         * @param x
         * @throws SQLException the SQL exception
         */
        @Override
        public void updateBigDecimal(String columnLabel, BigDecimal x) throws SQLException {
            internalRS.updateBigDecimal(columnLabel, x);
        }

        /**
         *
         * @param columnLabel
         * @param x
         * @throws SQLException the SQL exception
         */
        @Override
        public void updateString(String columnLabel, String x) throws SQLException {
            internalRS.updateString(columnLabel, x);
        }

        /**
         *
         * @param columnLabel
         * @param x
         * @throws SQLException the SQL exception
         */
        @Override
        public void updateBytes(String columnLabel, byte[] x) throws SQLException {
            internalRS.updateBytes(columnLabel, x);
        }

        /**
         *
         * @param columnLabel
         * @param x
         * @throws SQLException the SQL exception
         */
        @Override
        public void updateDate(String columnLabel, Date x) throws SQLException {
            internalRS.updateDate(columnLabel, x);
        }

        /**
         *
         * @param columnLabel
         * @param x
         * @throws SQLException the SQL exception
         */
        @Override
        public void updateTime(String columnLabel, Time x) throws SQLException {
            internalRS.updateTime(columnLabel, x);
        }

        /**
         *
         * @param columnLabel
         * @param x
         * @throws SQLException the SQL exception
         */
        @Override
        public void updateTimestamp(String columnLabel, Timestamp x) throws SQLException {
            internalRS.updateTimestamp(columnLabel, x);
        }

        /**
         * Update ascii stream.
         *
         * @param columnLabel
         * @param x
         * @param length
         * @throws SQLException the SQL exception
         */
        @Override
        public void updateAsciiStream(String columnLabel, InputStream x, int length) throws SQLException {
            internalRS.updateAsciiStream(columnLabel, x, length);
        }

        /**
         * Update binary stream.
         *
         * @param columnLabel
         * @param x
         * @param length
         * @throws SQLException the SQL exception
         */
        @Override
        public void updateBinaryStream(String columnLabel, InputStream x, int length) throws SQLException {
            internalRS.updateBinaryStream(columnLabel, x, length);
        }

        /**
         * Update character stream.
         *
         * @param columnLabel
         * @param reader
         * @param length
         * @throws SQLException the SQL exception
         */
        @Override
        public void updateCharacterStream(String columnLabel, Reader reader, int length) throws SQLException {
            internalRS.updateCharacterStream(columnLabel, reader, length);
        }

        /**
         *
         * @param columnLabel
         * @param x
         * @param scaleOrLength
         * @throws SQLException the SQL exception
         */
        @Override
        public void updateObject(String columnLabel, Object x, int scaleOrLength) throws SQLException {
            internalRS.updateObject(columnLabel, x, scaleOrLength);
        }

        /**
         *
         * @param columnLabel
         * @param x
         * @throws SQLException the SQL exception
         */
        @Override
        public void updateObject(String columnLabel, Object x) throws SQLException {
            internalRS.updateObject(columnLabel, x);
        }

        /**
         *
         * @throws SQLException the SQL exception
         */
        @Override
        public void insertRow() throws SQLException {
            internalRS.insertRow();
        }

        /**
         *
         * @throws SQLException the SQL exception
         */
        @Override
        public void updateRow() throws SQLException {
            internalRS.updateRow();
        }

        /**
         *
         * @throws SQLException the SQL exception
         */
        @Override
        public void deleteRow() throws SQLException {
            internalRS.deleteRow();
        }

        /**
         *
         * @throws SQLException the SQL exception
         */
        @Override
        public void refreshRow() throws SQLException {
            internalRS.refreshRow();
        }

        /**
         * Cancel row updates.
         *
         * @throws SQLException the SQL exception
         */
        @Override
        public void cancelRowUpdates() throws SQLException {
            internalRS.cancelRowUpdates();
        }

        /**
         * Move to insert row.
         *
         * @throws SQLException the SQL exception
         */
        @Override
        public void moveToInsertRow() throws SQLException {
            internalRS.moveToInsertRow();
        }

        /**
         * Move to current row.
         *
         * @throws SQLException the SQL exception
         */
        @Override
        public void moveToCurrentRow() throws SQLException {
            internalRS.moveToCurrentRow();
        }

        /**
         * Gets the statement.
         *
         * @return
         * @throws SQLException the SQL exception
         */
        @Override
        public Statement getStatement() throws SQLException {
            return poolableStmt;
        }

        /**
         * Gets the object.
         *
         * @param columnIndex
         * @param map
         * @return
         * @throws SQLException the SQL exception
         */
        @Override
        public Object getObject(int columnIndex, Map<String, Class<?>> map) throws SQLException {
            return internalRS.getObject(columnIndex, map);
        }

        /**
         * Gets the ref.
         *
         * @param columnIndex
         * @return
         * @throws SQLException the SQL exception
         */
        @Override
        public Ref getRef(int columnIndex) throws SQLException {
            return internalRS.getRef(columnIndex);
        }

        /**
         * Gets the blob.
         *
         * @param columnIndex
         * @return
         * @throws SQLException the SQL exception
         */
        @Override
        public Blob getBlob(int columnIndex) throws SQLException {
            return internalRS.getBlob(columnIndex);
        }

        /**
         * Gets the clob.
         *
         * @param columnIndex
         * @return
         * @throws SQLException the SQL exception
         */
        @Override
        public Clob getClob(int columnIndex) throws SQLException {
            return internalRS.getClob(columnIndex);
        }

        /**
         * Gets the array.
         *
         * @param columnIndex
         * @return
         * @throws SQLException the SQL exception
         */
        @Override
        public Array getArray(int columnIndex) throws SQLException {
            return internalRS.getArray(columnIndex);
        }

        /**
         * Gets the object.
         *
         * @param columnLabel
         * @param map
         * @return
         * @throws SQLException the SQL exception
         */
        @Override
        public Object getObject(String columnLabel, Map<String, Class<?>> map) throws SQLException {
            return internalRS.getObject(columnLabel, map);
        }

        /**
         * Gets the ref.
         *
         * @param columnLabel
         * @return
         * @throws SQLException the SQL exception
         */
        @Override
        public Ref getRef(String columnLabel) throws SQLException {
            return internalRS.getRef(columnLabel);
        }

        /**
         * Gets the blob.
         *
         * @param columnLabel
         * @return
         * @throws SQLException the SQL exception
         */
        @Override
        public Blob getBlob(String columnLabel) throws SQLException {
            return internalRS.getBlob(columnLabel);
        }

        /**
         * Gets the clob.
         *
         * @param columnLabel
         * @return
         * @throws SQLException the SQL exception
         */
        @Override
        public Clob getClob(String columnLabel) throws SQLException {
            return internalRS.getClob(columnLabel);
        }

        /**
         * Gets the array.
         *
         * @param columnLabel
         * @return
         * @throws SQLException the SQL exception
         */
        @Override
        public Array getArray(String columnLabel) throws SQLException {
            return internalRS.getArray(columnLabel);
        }

        /**
         * Gets the date.
         *
         * @param columnIndex
         * @param cal
         * @return
         * @throws SQLException the SQL exception
         */
        @Override
        public Date getDate(int columnIndex, Calendar cal) throws SQLException {
            return internalRS.getDate(columnIndex, cal);
        }

        /**
         * Gets the date.
         *
         * @param columnLabel
         * @param cal
         * @return
         * @throws SQLException the SQL exception
         */
        @Override
        public Date getDate(String columnLabel, Calendar cal) throws SQLException {
            return internalRS.getDate(columnLabel, cal);
        }

        /**
         * Gets the time.
         *
         * @param columnIndex
         * @param cal
         * @return
         * @throws SQLException the SQL exception
         */
        @Override
        public Time getTime(int columnIndex, Calendar cal) throws SQLException {
            return internalRS.getTime(columnIndex, cal);
        }

        /**
         * Gets the time.
         *
         * @param columnLabel
         * @param cal
         * @return
         * @throws SQLException the SQL exception
         */
        @Override
        public Time getTime(String columnLabel, Calendar cal) throws SQLException {
            return internalRS.getTime(columnLabel, cal);
        }

        /**
         * Gets the timestamp.
         *
         * @param columnIndex
         * @param cal
         * @return
         * @throws SQLException the SQL exception
         */
        @Override
        public Timestamp getTimestamp(int columnIndex, Calendar cal) throws SQLException {
            return internalRS.getTimestamp(columnIndex, cal);
        }

        /**
         * Gets the timestamp.
         *
         * @param columnLabel
         * @param cal
         * @return
         * @throws SQLException the SQL exception
         */
        @Override
        public Timestamp getTimestamp(String columnLabel, Calendar cal) throws SQLException {
            return internalRS.getTimestamp(columnLabel, cal);
        }

        /**
         * Gets the url.
         *
         * @param columnIndex
         * @return
         * @throws SQLException the SQL exception
         */
        @Override
        public URL getURL(int columnIndex) throws SQLException {
            return internalRS.getURL(columnIndex);
        }

        /**
         * Gets the url.
         *
         * @param columnLabel
         * @return
         * @throws SQLException the SQL exception
         */
        @Override
        public URL getURL(String columnLabel) throws SQLException {
            return internalRS.getURL(columnLabel);
        }

        /**
         *
         * @param columnIndex
         * @param x
         * @throws SQLException the SQL exception
         */
        @Override
        public void updateRef(int columnIndex, Ref x) throws SQLException {
            internalRS.updateRef(columnIndex, x);
        }

        /**
         *
         * @param columnLabel
         * @param x
         * @throws SQLException the SQL exception
         */
        @Override
        public void updateRef(String columnLabel, Ref x) throws SQLException {
            internalRS.updateRef(columnLabel, x);
        }

        /**
         *
         * @param columnIndex
         * @param x
         * @throws SQLException the SQL exception
         */
        @Override
        public void updateBlob(int columnIndex, Blob x) throws SQLException {
            internalRS.updateBlob(columnIndex, x);
        }

        /**
         *
         * @param columnLabel
         * @param x
         * @throws SQLException the SQL exception
         */
        @Override
        public void updateBlob(String columnLabel, Blob x) throws SQLException {
            internalRS.updateBlob(columnLabel, x);
        }

        /**
         *
         * @param columnIndex
         * @param x
         * @throws SQLException the SQL exception
         */
        @Override
        public void updateClob(int columnIndex, Clob x) throws SQLException {
            internalRS.updateClob(columnIndex, x);
        }

        /**
         *
         * @param columnLabel
         * @param x
         * @throws SQLException the SQL exception
         */
        @Override
        public void updateClob(String columnLabel, Clob x) throws SQLException {
            internalRS.updateClob(columnLabel, x);
        }

        /**
         *
         * @param columnIndex
         * @param x
         * @throws SQLException the SQL exception
         */
        @Override
        public void updateArray(int columnIndex, Array x) throws SQLException {
            internalRS.updateArray(columnIndex, x);
        }

        /**
         *
         * @param columnLabel
         * @param x
         * @throws SQLException the SQL exception
         */
        @Override
        public void updateArray(String columnLabel, Array x) throws SQLException {
            internalRS.updateArray(columnLabel, x);
        }

        /**
         * Gets the row id.
         *
         * @param columnIndex
         * @return
         * @throws SQLException the SQL exception
         */
        @Override
        public RowId getRowId(int columnIndex) throws SQLException {
            return internalRS.getRowId(columnIndex);
        }

        /**
         * Gets the row id.
         *
         * @param columnLabel
         * @return
         * @throws SQLException the SQL exception
         */
        @Override
        public RowId getRowId(String columnLabel) throws SQLException {
            return internalRS.getRowId(columnLabel);
        }

        /**
         * Update row id.
         *
         * @param columnIndex
         * @param x
         * @throws SQLException the SQL exception
         */
        @Override
        public void updateRowId(int columnIndex, RowId x) throws SQLException {
            internalRS.updateRowId(columnIndex, x);
        }

        /**
         * Update row id.
         *
         * @param columnLabel
         * @param x
         * @throws SQLException the SQL exception
         */
        @Override
        public void updateRowId(String columnLabel, RowId x) throws SQLException {
            internalRS.updateRowId(columnLabel, x);
        }

        /**
         * Gets the holdability.
         *
         * @return
         * @throws SQLException the SQL exception
         */
        @Override
        public int getHoldability() throws SQLException {
            return internalRS.getHoldability();
        }

        /**
         * Checks if is closed.
         *
         * @return true, if is closed
         * @throws SQLException the SQL exception
         */
        @Override
        public boolean isClosed() throws SQLException {
            return internalRS.isClosed();
        }

        /**
         * Update N string.
         *
         * @param columnIndex
         * @param nString
         * @throws SQLException the SQL exception
         */
        @Override
        public void updateNString(int columnIndex, String nString) throws SQLException {
            internalRS.updateNString(columnIndex, nString);
        }

        /**
         * Update N string.
         *
         * @param columnLabel
         * @param nString
         * @throws SQLException the SQL exception
         */
        @Override
        public void updateNString(String columnLabel, String nString) throws SQLException {
            internalRS.updateNString(columnLabel, nString);
        }

        /**
         * Update N clob.
         *
         * @param columnIndex
         * @param nClob
         * @throws SQLException the SQL exception
         */
        @Override
        public void updateNClob(int columnIndex, NClob nClob) throws SQLException {
            internalRS.updateNClob(columnIndex, nClob);
        }

        /**
         * Update N clob.
         *
         * @param columnLabel
         * @param nClob
         * @throws SQLException the SQL exception
         */
        @Override
        public void updateNClob(String columnLabel, NClob nClob) throws SQLException {
            internalRS.updateNClob(columnLabel, nClob);
        }

        /**
         * Gets the n clob.
         *
         * @param columnIndex
         * @return
         * @throws SQLException the SQL exception
         */
        @Override
        public NClob getNClob(int columnIndex) throws SQLException {
            return internalRS.getNClob(columnIndex);
        }

        /**
         * Gets the n clob.
         *
         * @param columnLabel
         * @return
         * @throws SQLException the SQL exception
         */
        @Override
        public NClob getNClob(String columnLabel) throws SQLException {
            return internalRS.getNClob(columnLabel);
        }

        /**
         * Gets the sqlxml.
         *
         * @param columnIndex
         * @return
         * @throws SQLException the SQL exception
         */
        @Override
        public SQLXML getSQLXML(int columnIndex) throws SQLException {
            return internalRS.getSQLXML(columnIndex);
        }

        /**
         * Gets the sqlxml.
         *
         * @param columnLabel
         * @return
         * @throws SQLException the SQL exception
         */
        @Override
        public SQLXML getSQLXML(String columnLabel) throws SQLException {
            return internalRS.getSQLXML(columnLabel);
        }

        /**
         *
         * @param columnIndex
         * @param xmlObject
         * @throws SQLException the SQL exception
         */
        @Override
        public void updateSQLXML(int columnIndex, SQLXML xmlObject) throws SQLException {
            internalRS.updateSQLXML(columnIndex, xmlObject);
        }

        /**
         *
         * @param columnLabel
         * @param xmlObject
         * @throws SQLException the SQL exception
         */
        @Override
        public void updateSQLXML(String columnLabel, SQLXML xmlObject) throws SQLException {
            internalRS.updateSQLXML(columnLabel, xmlObject);
        }

        /**
         * Gets the n string.
         *
         * @param columnIndex
         * @return
         * @throws SQLException the SQL exception
         */
        @Override
        public String getNString(int columnIndex) throws SQLException {
            return internalRS.getNString(columnIndex);
        }

        /**
         * Gets the n string.
         *
         * @param columnLabel
         * @return
         * @throws SQLException the SQL exception
         */
        @Override
        public String getNString(String columnLabel) throws SQLException {
            return internalRS.getNString(columnLabel);
        }

        /**
         * Gets the n character stream.
         *
         * @param columnIndex
         * @return
         * @throws SQLException the SQL exception
         */
        @Override
        public Reader getNCharacterStream(int columnIndex) throws SQLException {
            return internalRS.getNCharacterStream(columnIndex);
        }

        /**
         * Gets the n character stream.
         *
         * @param columnLabel
         * @return
         * @throws SQLException the SQL exception
         */
        @Override
        public Reader getNCharacterStream(String columnLabel) throws SQLException {
            return internalRS.getNCharacterStream(columnLabel);
        }

        /**
         * Update N character stream.
         *
         * @param columnIndex
         * @param x
         * @param length
         * @throws SQLException the SQL exception
         */
        @Override
        public void updateNCharacterStream(int columnIndex, Reader x, long length) throws SQLException {
            internalRS.updateNCharacterStream(columnIndex, x, length);
        }

        /**
         * Update N character stream.
         *
         * @param columnLabel
         * @param reader
         * @param length
         * @throws SQLException the SQL exception
         */
        @Override
        public void updateNCharacterStream(String columnLabel, Reader reader, long length) throws SQLException {
            internalRS.updateNCharacterStream(columnLabel, reader, length);
        }

        /**
         * Update ascii stream.
         *
         * @param columnIndex
         * @param x
         * @param length
         * @throws SQLException the SQL exception
         */
        @Override
        public void updateAsciiStream(int columnIndex, InputStream x, long length) throws SQLException {
            internalRS.updateAsciiStream(columnIndex, x, length);
        }

        /**
         * Update binary stream.
         *
         * @param columnIndex
         * @param x
         * @param length
         * @throws SQLException the SQL exception
         */
        @Override
        public void updateBinaryStream(int columnIndex, InputStream x, long length) throws SQLException {
            internalRS.updateBinaryStream(columnIndex, x, length);
        }

        /**
         * Update character stream.
         *
         * @param columnIndex
         * @param x
         * @param length
         * @throws SQLException the SQL exception
         */
        @Override
        public void updateCharacterStream(int columnIndex, Reader x, long length) throws SQLException {
            internalRS.updateCharacterStream(columnIndex, x, length);
        }

        /**
         * Update ascii stream.
         *
         * @param columnLabel
         * @param x
         * @param length
         * @throws SQLException the SQL exception
         */
        @Override
        public void updateAsciiStream(String columnLabel, InputStream x, long length) throws SQLException {
            internalRS.updateAsciiStream(columnLabel, x, length);
        }

        /**
         * Update binary stream.
         *
         * @param columnLabel
         * @param x
         * @param length
         * @throws SQLException the SQL exception
         */
        @Override
        public void updateBinaryStream(String columnLabel, InputStream x, long length) throws SQLException {
            internalRS.updateBinaryStream(columnLabel, x, length);
        }

        /**
         * Update character stream.
         *
         * @param columnLabel
         * @param reader
         * @param length
         * @throws SQLException the SQL exception
         */
        @Override
        public void updateCharacterStream(String columnLabel, Reader reader, long length) throws SQLException {
            internalRS.updateCharacterStream(columnLabel, reader, length);
        }

        /**
         *
         * @param columnIndex
         * @param inputStream
         * @param length
         * @throws SQLException the SQL exception
         */
        @Override
        public void updateBlob(int columnIndex, InputStream inputStream, long length) throws SQLException {
            internalRS.updateBlob(columnIndex, inputStream, length);
        }

        /**
         *
         * @param columnLabel
         * @param inputStream
         * @param length
         * @throws SQLException the SQL exception
         */
        @Override
        public void updateBlob(String columnLabel, InputStream inputStream, long length) throws SQLException {
            internalRS.updateBlob(columnLabel, inputStream, length);
        }

        /**
         *
         * @param columnIndex
         * @param reader
         * @param length
         * @throws SQLException the SQL exception
         */
        @Override
        public void updateClob(int columnIndex, Reader reader, long length) throws SQLException {
            internalRS.updateClob(columnIndex, reader, length);
        }

        /**
         *
         * @param columnLabel
         * @param reader
         * @param length
         * @throws SQLException the SQL exception
         */
        @Override
        public void updateClob(String columnLabel, Reader reader, long length) throws SQLException {
            internalRS.updateClob(columnLabel, reader, length);
        }

        /**
         * Update N clob.
         *
         * @param columnIndex
         * @param reader
         * @param length
         * @throws SQLException the SQL exception
         */
        @Override
        public void updateNClob(int columnIndex, Reader reader, long length) throws SQLException {
            internalRS.updateNClob(columnIndex, reader, length);
        }

        /**
         * Update N clob.
         *
         * @param columnLabel
         * @param reader
         * @param length
         * @throws SQLException the SQL exception
         */
        @Override
        public void updateNClob(String columnLabel, Reader reader, long length) throws SQLException {
            internalRS.updateNClob(columnLabel, reader, length);
        }

        /**
         * Update N character stream.
         *
         * @param columnIndex
         * @param x
         * @throws SQLException the SQL exception
         */
        @Override
        public void updateNCharacterStream(int columnIndex, Reader x) throws SQLException {
            internalRS.updateNCharacterStream(columnIndex, x);
        }

        /**
         * Update N character stream.
         *
         * @param columnLabel
         * @param reader
         * @throws SQLException the SQL exception
         */
        @Override
        public void updateNCharacterStream(String columnLabel, Reader reader) throws SQLException {
            internalRS.updateNCharacterStream(columnLabel, reader);
        }

        /**
         * Update ascii stream.
         *
         * @param columnIndex
         * @param x
         * @throws SQLException the SQL exception
         */
        @Override
        public void updateAsciiStream(int columnIndex, InputStream x) throws SQLException {
            internalRS.updateAsciiStream(columnIndex, x);
        }

        /**
         * Update binary stream.
         *
         * @param columnIndex
         * @param x
         * @throws SQLException the SQL exception
         */
        @Override
        public void updateBinaryStream(int columnIndex, InputStream x) throws SQLException {
            internalRS.updateBinaryStream(columnIndex, x);
        }

        /**
         * Update character stream.
         *
         * @param columnIndex
         * @param x
         * @throws SQLException the SQL exception
         */
        @Override
        public void updateCharacterStream(int columnIndex, Reader x) throws SQLException {
            internalRS.updateCharacterStream(columnIndex, x);
        }

        /**
         * Update ascii stream.
         *
         * @param columnLabel
         * @param x
         * @throws SQLException the SQL exception
         */
        @Override
        public void updateAsciiStream(String columnLabel, InputStream x) throws SQLException {
            internalRS.updateAsciiStream(columnLabel, x);
        }

        /**
         * Update binary stream.
         *
         * @param columnLabel
         * @param x
         * @throws SQLException the SQL exception
         */
        @Override
        public void updateBinaryStream(String columnLabel, InputStream x) throws SQLException {
            internalRS.updateBinaryStream(columnLabel, x);
        }

        /**
         * Update character stream.
         *
         * @param columnLabel
         * @param reader
         * @throws SQLException the SQL exception
         */
        @Override
        public void updateCharacterStream(String columnLabel, Reader reader) throws SQLException {
            internalRS.updateCharacterStream(columnLabel, reader);
        }

        /**
         *
         * @param columnIndex
         * @param inputStream
         * @throws SQLException the SQL exception
         */
        @Override
        public void updateBlob(int columnIndex, InputStream inputStream) throws SQLException {
            internalRS.updateBlob(columnIndex, inputStream);
        }

        /**
         *
         * @param columnLabel
         * @param inputStream
         * @throws SQLException the SQL exception
         */
        @Override
        public void updateBlob(String columnLabel, InputStream inputStream) throws SQLException {
            internalRS.updateBlob(columnLabel, inputStream);
        }

        /**
         *
         * @param columnIndex
         * @param reader
         * @throws SQLException the SQL exception
         */
        @Override
        public void updateClob(int columnIndex, Reader reader) throws SQLException {
            internalRS.updateClob(columnIndex, reader);
        }

        /**
         *
         * @param columnLabel
         * @param reader
         * @throws SQLException the SQL exception
         */
        @Override
        public void updateClob(String columnLabel, Reader reader) throws SQLException {
            internalRS.updateClob(columnLabel, reader);
        }

        /**
         * Update N clob.
         *
         * @param columnIndex
         * @param reader
         * @throws SQLException the SQL exception
         */
        @Override
        public void updateNClob(int columnIndex, Reader reader) throws SQLException {
            internalRS.updateNClob(columnIndex, reader);
        }

        /**
         * Update N clob.
         *
         * @param columnLabel
         * @param reader
         * @throws SQLException the SQL exception
         */
        @Override
        public void updateNClob(String columnLabel, Reader reader) throws SQLException {
            internalRS.updateNClob(columnLabel, reader);
        }

        /**
         * Gets the object.
         *
         * @param <T>
         * @param columnIndex
         * @param type
         * @return
         * @throws SQLException the SQL exception
         */
        @Override
        public <T> T getObject(int columnIndex, Class<T> type) throws SQLException {
            return internalRS.getObject(columnIndex, type);
        }

        /**
         * Gets the object.
         *
         * @param <T>
         * @param columnLabel
         * @param type
         * @return
         * @throws SQLException the SQL exception
         */
        @Override
        public <T> T getObject(String columnLabel, Class<T> type) throws SQLException {
            return internalRS.getObject(columnLabel, type);
        }
    }
}
