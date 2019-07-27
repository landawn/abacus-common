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
import com.landawn.abacus.exception.AbacusException;
import com.landawn.abacus.logging.Logger;
import com.landawn.abacus.logging.LoggerFactory;
import com.landawn.abacus.pool.AbstractPoolable;

/**
 * 
 * @since 0.8
 * 
 * @author Haiyang Li
 */
class PoolablePreparedStatement extends AbstractPoolable implements PreparedStatement {
    private static final Logger logger = LoggerFactory.getLogger(PoolablePreparedStatement.class);

    private static final long DEFAULT_LIVE_TIME = 24 * 60 * 60 * 1000L;
    private static final long DEFAULT_MAX_IDLE_TIME = 30 * 60 * 1000L;
    private final CachedStatmentKey id;
    private final java.sql.PreparedStatement internalStmt;
    private final PoolableConnection poolableConn;

    private boolean isClosed = false;

    public PoolablePreparedStatement(java.sql.PreparedStatement stmt, PoolableConnection conn, CachedStatmentKey id) {
        super(DEFAULT_LIVE_TIME, DEFAULT_MAX_IDLE_TIME);
        internalStmt = stmt;
        poolableConn = conn;
        this.id = id;
    }

    CachedStatmentKey getId() {
        return id;
    }

    /**
     * Method isPoolable.
     * 
     * @return boolean
     * @throws SQLException
     * @see java.sql.Statement#isPoolable()
     */
    @Override
    public boolean isPoolable() throws SQLException {
        return internalStmt.isPoolable();
    }

    /**
     * Method isClosed.
     * 
     * @return boolean
     * @throws SQLException
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
                    logger.warn(AbacusException.getErrorMsg(e));
                }
            }
        }

        return isClosed;
    }

    /**
     * Method close.
     * 
     * @throws SQLException
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
                logger.warn(AbacusException.getErrorMsg(e));
            }
        }
    }

    /**
     * Method addBatch.
     * 
     * @throws SQLException
     * @see java.sql.PreparedStatement#addBatch()
     */
    @Override
    public void addBatch() throws SQLException {
        internalStmt.addBatch();
    }

    /**
     * Method clearParameters.
     * 
     * @throws SQLException
     * @see java.sql.PreparedStatement#clearParameters()
     */
    @Override
    public void clearParameters() throws SQLException {
        internalStmt.clearParameters();
    }

    /**
     * Method execute.
     * 
     * @return boolean
     * @throws SQLException
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
     * Method executeQuery.
     * 
     * @return ResultSet
     * @throws SQLException
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
     * Method executeUpdate.
     * 
     * @return int
     * @throws SQLException
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
     * Method execute.
     * 
     * @param sql
     * @return boolean
     * @throws SQLException
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
     * Method execute.
     * 
     * @param sql
     * @param autoGeneratedKeys
     * @return boolean
     * @throws SQLException
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
     * Method execute.
     * 
     * @param sql
     * @param columnIndexes
     * @return boolean
     * @throws SQLException
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
     * Method execute.
     * 
     * @param sql
     * @param columnNames
     * @return boolean
     * @throws SQLException
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
     * Method executeBatch.
     * 
     * @return int[]
     * @throws SQLException
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
     * Method executeQuery.
     * 
     * @param sql
     * @return ResultSet
     * @throws SQLException
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

    private ResultSet wrap(ResultSet rs) {
        return new ResultSetProxy(rs, this);
    }

    /**
     * Method executeUpdate.
     * 
     * @param sql
     * @return int
     * @throws SQLException
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
     * Method executeUpdate.
     * 
     * @param sql
     * @param autoGeneratedKeys
     * @return int
     * @throws SQLException
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
     * Method executeUpdate.
     * 
     * @param sql
     * @param columnIndexes
     * @return int
     * @throws SQLException
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
     * Method executeUpdate.
     * 
     * @param sql
     * @param columnNames
     * @return int
     * @throws SQLException
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
     * Method getMetaData.
     * 
     * @return ResultSetMetaData
     * @throws SQLException
     * @see java.sql.PreparedStatement#getMetaData()
     */
    @Override
    public ResultSetMetaData getMetaData() throws SQLException {
        return internalStmt.getMetaData();
    }

    /**
     * Method getParameterMetaData.
     * 
     * @return ParameterMetaData
     * @throws SQLException
     * @see java.sql.PreparedStatement#getParameterMetaData()
     */
    @Override
    public ParameterMetaData getParameterMetaData() throws SQLException {
        return internalStmt.getParameterMetaData();
    }

    /**
     * Method setArray.
     * 
     * @param parameterIndex
     * @param x
     * @throws SQLException
     * @see java.sql.PreparedStatement#setArray(int, Array)
     */
    @Override
    public void setArray(int parameterIndex, Array x) throws SQLException {
        internalStmt.setArray(parameterIndex, x);
    }

    /**
     * Method setAsciiStream.
     * 
     * @param parameterIndex
     * @param x
     * @throws SQLException
     * @see java.sql.PreparedStatement#setAsciiStream(int, InputStream)
     */
    @Override
    public void setAsciiStream(int parameterIndex, InputStream x) throws SQLException {
        internalStmt.setAsciiStream(parameterIndex, x);
    }

    /**
     * Method setAsciiStream.
     * 
     * @param parameterIndex
     * @param x
     * @param length
     * @throws SQLException
     * @see java.sql.PreparedStatement#setAsciiStream(int, InputStream, int)
     */
    @Override
    public void setAsciiStream(int parameterIndex, InputStream x, int length) throws SQLException {
        internalStmt.setAsciiStream(parameterIndex, x, length);
    }

    /**
     * Method setAsciiStream.
     * 
     * @param parameterIndex
     * @param x
     * @param length
     * @throws SQLException
     * @see java.sql.PreparedStatement#setAsciiStream(int, InputStream, long)
     */
    @Override
    public void setAsciiStream(int parameterIndex, InputStream x, long length) throws SQLException {
        internalStmt.setAsciiStream(parameterIndex, x, length);
    }

    /**
     * Method setBigDecimal.
     * 
     * @param parameterIndex
     * @param x
     * @throws SQLException
     * @see java.sql.PreparedStatement#setBigDecimal(int, BigDecimal)
     */
    @Override
    public void setBigDecimal(int parameterIndex, BigDecimal x) throws SQLException {
        internalStmt.setBigDecimal(parameterIndex, x);
    }

    /**
     * Method setBinaryStream.
     * 
     * @param parameterIndex
     * @param x
     * @throws SQLException
     * @see java.sql.PreparedStatement#setBinaryStream(int, InputStream)
     */
    @Override
    public void setBinaryStream(int parameterIndex, InputStream x) throws SQLException {
        internalStmt.setBinaryStream(parameterIndex, x);
    }

    /**
     * Method setBinaryStream.
     * 
     * @param parameterIndex
     * @param x
     * @param length
     * @throws SQLException
     * @see java.sql.PreparedStatement#setBinaryStream(int, InputStream, int)
     */
    @Override
    public void setBinaryStream(int parameterIndex, InputStream x, int length) throws SQLException {
        internalStmt.setBinaryStream(parameterIndex, x, length);
    }

    /**
     * Method setBinaryStream.
     * 
     * @param parameterIndex
     * @param x
     * @param length
     * @throws SQLException
     * @see java.sql.PreparedStatement#setBinaryStream(int, InputStream, long)
     */
    @Override
    public void setBinaryStream(int parameterIndex, InputStream x, long length) throws SQLException {
        internalStmt.setBinaryStream(parameterIndex, x, length);
    }

    /**
     * Method setBlob.
     * 
     * @param parameterIndex
     * @param x
     * @throws SQLException
     * @see java.sql.PreparedStatement#setBlob(int, Blob)
     */
    @Override
    public void setBlob(int parameterIndex, Blob x) throws SQLException {
        internalStmt.setBlob(parameterIndex, x);
    }

    /**
     * Method setBlob.
     * 
     * @param parameterIndex
     * @param inputStream
     * @throws SQLException
     * @see java.sql.PreparedStatement#setBlob(int, InputStream)
     */
    @Override
    public void setBlob(int parameterIndex, InputStream inputStream) throws SQLException {
        internalStmt.setBlob(parameterIndex, inputStream);
    }

    /**
     * Method setBlob.
     * 
     * @param parameterIndex
     * @param inputStream
     * @param length
     * @throws SQLException
     * @see java.sql.PreparedStatement#setBlob(int, InputStream, long)
     */
    @Override
    public void setBlob(int parameterIndex, InputStream inputStream, long length) throws SQLException {
        internalStmt.setBlob(parameterIndex, inputStream, length);
    }

    /**
     * Method setBoolean.
     * 
     * @param parameterIndex
     * @param x
     * @throws SQLException
     * @see java.sql.PreparedStatement#setBoolean(int, boolean)
     */
    @Override
    public void setBoolean(int parameterIndex, boolean x) throws SQLException {
        internalStmt.setBoolean(parameterIndex, x);
    }

    /**
     * Method setByte.
     * 
     * @param parameterIndex
     * @param x
     * @throws SQLException
     * @see java.sql.PreparedStatement#setByte(int, byte)
     */
    @Override
    public void setByte(int parameterIndex, byte x) throws SQLException {
        internalStmt.setByte(parameterIndex, x);
    }

    /**
     * Method setBytes.
     * 
     * @param parameterIndex
     * @param x
     * @throws SQLException
     * @see java.sql.PreparedStatement#setBytes(int, byte[])
     */
    @Override
    public void setBytes(int parameterIndex, byte[] x) throws SQLException {
        internalStmt.setBytes(parameterIndex, x);
    }

    /**
     * Method setCharacterStream.
     * 
     * @param parameterIndex
     * @param reader
     * @throws SQLException
     * @see java.sql.PreparedStatement#setCharacterStream(int, Reader)
     */
    @Override
    public void setCharacterStream(int parameterIndex, Reader reader) throws SQLException {
        internalStmt.setCharacterStream(parameterIndex, reader);
    }

    /**
     * Method setCharacterStream.
     * 
     * @param parameterIndex
     * @param reader
     * @param length
     * @throws SQLException
     * @see java.sql.PreparedStatement#setCharacterStream(int, Reader, int)
     */
    @Override
    public void setCharacterStream(int parameterIndex, Reader reader, int length) throws SQLException {
        internalStmt.setCharacterStream(parameterIndex, reader, length);
    }

    /**
     * Method setCharacterStream.
     * 
     * @param parameterIndex
     * @param reader
     * @param length
     * @throws SQLException
     * @see java.sql.PreparedStatement#setCharacterStream(int, Reader, long)
     */
    @Override
    public void setCharacterStream(int parameterIndex, Reader reader, long length) throws SQLException {
        internalStmt.setCharacterStream(parameterIndex, reader, length);
    }

    /**
     * Method setClob.
     * 
     * @param parameterIndex
     * @param x
     * @throws SQLException
     * @see java.sql.PreparedStatement#setClob(int, Clob)
     */
    @Override
    public void setClob(int parameterIndex, Clob x) throws SQLException {
        internalStmt.setClob(parameterIndex, x);
    }

    /**
     * Method setClob.
     * 
     * @param parameterIndex
     * @param reader
     * @throws SQLException
     * @see java.sql.PreparedStatement#setClob(int, Reader)
     */
    @Override
    public void setClob(int parameterIndex, Reader reader) throws SQLException {
        internalStmt.setClob(parameterIndex, reader);
    }

    /**
     * Method setClob.
     * 
     * @param parameterIndex
     * @param reader
     * @param length
     * @throws SQLException
     * @see java.sql.PreparedStatement#setClob(int, Reader, long)
     */
    @Override
    public void setClob(int parameterIndex, Reader reader, long length) throws SQLException {
        internalStmt.setClob(parameterIndex, reader, length);
    }

    /**
     * Method setDate.
     * 
     * @param parameterIndex
     * @param x
     * @throws SQLException
     * @see java.sql.PreparedStatement#setDate(int, Date)
     */
    @Override
    public void setDate(int parameterIndex, Date x) throws SQLException {
        internalStmt.setDate(parameterIndex, x);
    }

    /**
     * Method setDate.
     * 
     * @param parameterIndex
     * @param x
     * @param cal
     * @throws SQLException
     * @see java.sql.PreparedStatement#setDate(int, Date, Calendar)
     */
    @Override
    public void setDate(int parameterIndex, Date x, Calendar cal) throws SQLException {
        internalStmt.setDate(parameterIndex, x, cal);
    }

    /**
     * Method setDouble.
     * 
     * @param parameterIndex
     * @param x
     * @throws SQLException
     * @see java.sql.PreparedStatement#setDouble(int, double)
     */
    @Override
    public void setDouble(int parameterIndex, double x) throws SQLException {
        internalStmt.setDouble(parameterIndex, x);
    }

    /**
     * Method setFloat.
     * 
     * @param parameterIndex
     * @param x
     * @throws SQLException
     * @see java.sql.PreparedStatement#setFloat(int, float)
     */
    @Override
    public void setFloat(int parameterIndex, float x) throws SQLException {
        internalStmt.setFloat(parameterIndex, x);
    }

    /**
     * Method setInt.
     * 
     * @param parameterIndex
     * @param x
     * @throws SQLException
     * @see java.sql.PreparedStatement#setInt(int, int)
     */
    @Override
    public void setInt(int parameterIndex, int x) throws SQLException {
        internalStmt.setInt(parameterIndex, x);
    }

    /**
     * Method setLong.
     * 
     * @param parameterIndex
     * @param x
     * @throws SQLException
     * @see java.sql.PreparedStatement#setLong(int, long)
     */
    @Override
    public void setLong(int parameterIndex, long x) throws SQLException {
        internalStmt.setLong(parameterIndex, x);
    }

    /**
     * Method setNCharacterStream.
     * 
     * @param parameterIndex
     * @param value
     * @throws SQLException
     * @see java.sql.PreparedStatement#setNCharacterStream(int, Reader)
     */
    @Override
    public void setNCharacterStream(int parameterIndex, Reader value) throws SQLException {
        internalStmt.setNCharacterStream(parameterIndex, value);
    }

    /**
     * Method setNCharacterStream.
     * 
     * @param parameterIndex
     * @param value
     * @param length
     * @throws SQLException
     * @see java.sql.PreparedStatement#setNCharacterStream(int, Reader, long)
     */
    @Override
    public void setNCharacterStream(int parameterIndex, Reader value, long length) throws SQLException {
        internalStmt.setNCharacterStream(parameterIndex, value, length);
    }

    /**
     * Method setNClob.
     * 
     * @param parameterIndex
     * @param value
     * @throws SQLException
     * @see java.sql.PreparedStatement#setNClob(int, NClob)
     */
    @Override
    public void setNClob(int parameterIndex, NClob value) throws SQLException {
        internalStmt.setNClob(parameterIndex, value);
    }

    /**
     * Method setNClob.
     * 
     * @param parameterIndex
     * @param reader
     * @throws SQLException
     * @see java.sql.PreparedStatement#setNClob(int, Reader)
     */
    @Override
    public void setNClob(int parameterIndex, Reader reader) throws SQLException {
        internalStmt.setNClob(parameterIndex, reader);
    }

    /**
     * Method setNClob.
     * 
     * @param parameterIndex
     * @param reader
     * @param length
     * @throws SQLException
     * @see java.sql.PreparedStatement#setNClob(int, Reader, long)
     */
    @Override
    public void setNClob(int parameterIndex, Reader reader, long length) throws SQLException {
        internalStmt.setNClob(parameterIndex, reader, length);
    }

    /**
     * Method setNString.
     * 
     * @param parameterIndex
     * @param value
     * @throws SQLException
     * @see java.sql.PreparedStatement#setNString(int, String)
     */
    @Override
    public void setNString(int parameterIndex, String value) throws SQLException {
        internalStmt.setNString(parameterIndex, value);
    }

    /**
     * Method setNull.
     * 
     * @param parameterIndex
     * @param sqlType
     * @throws SQLException
     * @see java.sql.PreparedStatement#setNull(int, int)
     */
    @Override
    public void setNull(int parameterIndex, int sqlType) throws SQLException {
        internalStmt.setNull(parameterIndex, sqlType);
    }

    /**
     * Method setNull.
     * 
     * @param parameterIndex
     * @param sqlType
     * @param typeName
     * @throws SQLException
     * @see java.sql.PreparedStatement#setNull(int, int, String)
     */
    @Override
    public void setNull(int parameterIndex, int sqlType, String typeName) throws SQLException {
        internalStmt.setNull(parameterIndex, sqlType, typeName);
    }

    /**
     * Method setObject.
     * 
     * @param parameterIndex
     * @param x
     * @throws SQLException
     * @see java.sql.PreparedStatement#setObject(int, Object)
     */
    @Override
    public void setObject(int parameterIndex, Object x) throws SQLException {
        internalStmt.setObject(parameterIndex, x);
    }

    /**
     * Method setObject.
     * 
     * @param parameterIndex
     * @param x
     * @param targetSqlType
     * @throws SQLException
     * @see java.sql.PreparedStatement#setObject(int, Object, int)
     */
    @Override
    public void setObject(int parameterIndex, Object x, int targetSqlType) throws SQLException {
        internalStmt.setObject(parameterIndex, x, targetSqlType);
    }

    /**
     * Method setObject.
     * 
     * @param parameterIndex
     * @param x
     * @param targetSqlType
     * @param scaleOrLength
     * @throws SQLException
     * @see java.sql.PreparedStatement#setObject(int, Object, int, int)
     */
    @Override
    public void setObject(int parameterIndex, Object x, int targetSqlType, int scaleOrLength) throws SQLException {
        internalStmt.setObject(parameterIndex, x, targetSqlType, scaleOrLength);
    }

    /**
     * Method setRef.
     * 
     * @param parameterIndex
     * @param x
     * @throws SQLException
     * @see java.sql.PreparedStatement#setRef(int, Ref)
     */
    @Override
    public void setRef(int parameterIndex, Ref x) throws SQLException {
        internalStmt.setRef(parameterIndex, x);
    }

    /**
     * Method setRowId.
     * 
     * @param parameterIndex
     * @param x
     * @throws SQLException
     * @see java.sql.PreparedStatement#setRowId(int, RowId)
     */
    @Override
    public void setRowId(int parameterIndex, RowId x) throws SQLException {
        internalStmt.setRowId(parameterIndex, x);
    }

    /**
     * Method setSQLXML.
     * 
     * @param parameterIndex
     * @param xmlObject
     * @throws SQLException
     * @see java.sql.PreparedStatement#setSQLXML(int, SQLXML)
     */
    @Override
    public void setSQLXML(int parameterIndex, SQLXML xmlObject) throws SQLException {
        internalStmt.setSQLXML(parameterIndex, xmlObject);
    }

    /**
     * Method setShort.
     * 
     * @param parameterIndex
     * @param x
     * @throws SQLException
     * @see java.sql.PreparedStatement#setShort(int, short)
     */
    @Override
    public void setShort(int parameterIndex, short x) throws SQLException {
        internalStmt.setShort(parameterIndex, x);
    }

    /**
     * Method setString.
     * 
     * @param parameterIndex
     * @param x
     * @throws SQLException
     * @see java.sql.PreparedStatement#setString(int, String)
     */
    @Override
    public void setString(int parameterIndex, String x) throws SQLException {
        internalStmt.setString(parameterIndex, x);
    }

    /**
     * Method setTime.
     * 
     * @param parameterIndex
     * @param x
     * @throws SQLException
     * @see java.sql.PreparedStatement#setTime(int, Time)
     */
    @Override
    public void setTime(int parameterIndex, Time x) throws SQLException {
        internalStmt.setTime(parameterIndex, x);
    }

    /**
     * Method setTime.
     * 
     * @param parameterIndex
     * @param x
     * @param cal
     * @throws SQLException
     * @see java.sql.PreparedStatement#setTime(int, Time, Calendar)
     */
    @Override
    public void setTime(int parameterIndex, Time x, Calendar cal) throws SQLException {
        internalStmt.setTime(parameterIndex, x, cal);
    }

    /**
     * Method setTimestamp.
     * 
     * @param parameterIndex
     * @param x
     * @throws SQLException
     * @see java.sql.PreparedStatement#setTimestamp(int, Timestamp)
     */
    @Override
    public void setTimestamp(int parameterIndex, Timestamp x) throws SQLException {
        internalStmt.setTimestamp(parameterIndex, x);
    }

    /**
     * Method setTimestamp.
     * 
     * @param parameterIndex
     * @param x
     * @param cal
     * @throws SQLException
     * @see java.sql.PreparedStatement#setTimestamp(int, Timestamp, Calendar)
     */
    @Override
    public void setTimestamp(int parameterIndex, Timestamp x, Calendar cal) throws SQLException {
        internalStmt.setTimestamp(parameterIndex, x, cal);
    }

    /**
     * Method setURL.
     * 
     * @param parameterIndex
     * @param x
     * @throws SQLException
     * @see java.sql.PreparedStatement#setURL(int, URL)
     */
    @Override
    public void setURL(int parameterIndex, URL x) throws SQLException {
        internalStmt.setURL(parameterIndex, x);
    }

    /**
     * Method setUnicodeStream.
     * 
     * @param parameterIndex
     * @param x
     * @param length
     * @throws SQLException
     * @see java.sql.PreparedStatement#setUnicodeStream(int, InputStream, int)
     */
    @Override
    @SuppressWarnings("deprecation")
    public void setUnicodeStream(int parameterIndex, InputStream x, int length) throws SQLException {
        internalStmt.setUnicodeStream(parameterIndex, x, length);
    }

    /**
     * Method addBatch.
     * 
     * @param sql
     * @throws SQLException
     * @see java.sql.Statement#addBatch(String)
     */
    @Override
    public void addBatch(String sql) throws SQLException {
        internalStmt.addBatch(sql);
    }

    /**
     * Method cancel.
     * 
     * @throws SQLException
     * @see java.sql.Statement#cancel()
     */
    @Override
    public void cancel() throws SQLException {
        internalStmt.cancel();
    }

    /**
     * Method clearBatch.
     * 
     * @throws SQLException
     * @see java.sql.Statement#clearBatch()
     */
    @Override
    public void clearBatch() throws SQLException {
        internalStmt.clearBatch();
    }

    /**
     * Method clearWarnings.
     * 
     * @throws SQLException
     * @see java.sql.Statement#clearWarnings()
     */
    @Override
    public void clearWarnings() throws SQLException {
        internalStmt.clearWarnings();
    }

    /**
     * Method getConnection.
     * 
     * @return java.sql.Connection
     * @throws SQLException
     * @see java.sql.Statement#getConnection()
     */
    @Override
    public PoolableConnection getConnection() throws SQLException {
        return poolableConn;
    }

    /**
     * Method getFetchDirection.
     * 
     * @return int
     * @throws SQLException
     * @see java.sql.Statement#getFetchDirection()
     */
    @Override
    public int getFetchDirection() throws SQLException {
        return internalStmt.getFetchDirection();
    }

    /**
     * Method getFetchSize.
     * 
     * @return int
     * @throws SQLException
     * @see java.sql.Statement#getFetchSize()
     */
    @Override
    public int getFetchSize() throws SQLException {
        return internalStmt.getFetchSize();
    }

    /**
     * Method getGeneratedKeys.
     * 
     * @return ResultSet
     * @throws SQLException
     * @see java.sql.Statement#getGeneratedKeys()
     */
    @Override
    public ResultSet getGeneratedKeys() throws SQLException {
        return wrap(internalStmt.getGeneratedKeys());
    }

    /**
     * Method getMaxFieldSize.
     * 
     * @return int
     * @throws SQLException
     * @see java.sql.Statement#getMaxFieldSize()
     */
    @Override
    public int getMaxFieldSize() throws SQLException {
        return internalStmt.getMaxFieldSize();
    }

    /**
     * Method getMaxRows.
     * 
     * @return int
     * @throws SQLException
     * @see java.sql.Statement#getMaxRows()
     */
    @Override
    public int getMaxRows() throws SQLException {
        return internalStmt.getMaxRows();
    }

    /**
     * Method getMoreResults.
     * 
     * @return boolean
     * @throws SQLException
     * @see java.sql.Statement#getMoreResults()
     */
    @Override
    public boolean getMoreResults() throws SQLException {
        return internalStmt.getMoreResults();
    }

    /**
     * Method getMoreResults.
     * 
     * @param current
     * @return boolean
     * @throws SQLException
     * @see java.sql.Statement#getMoreResults(int)
     */
    @Override
    public boolean getMoreResults(int current) throws SQLException {
        return internalStmt.getMoreResults(current);
    }

    /**
     * Method getQueryTimeout.
     * 
     * @return int
     * @throws SQLException
     * @see java.sql.Statement#getQueryTimeout()
     */
    @Override
    public int getQueryTimeout() throws SQLException {
        return internalStmt.getQueryTimeout();
    }

    /**
     * Method getResultSet.
     * 
     * @return ResultSet
     * @throws SQLException
     * @see java.sql.Statement#getResultSet()
     */
    @Override
    public ResultSet getResultSet() throws SQLException {
        return wrap(internalStmt.getResultSet());
    }

    /**
     * Method getResultSetConcurrency.
     * 
     * @return int
     * @throws SQLException
     * @see java.sql.Statement#getResultSetConcurrency()
     */
    @Override
    public int getResultSetConcurrency() throws SQLException {
        return internalStmt.getResultSetConcurrency();
    }

    /**
     * Method getResultSetHoldability.
     * 
     * @return int
     * @throws SQLException
     * @see java.sql.Statement#getResultSetHoldability()
     */
    @Override
    public int getResultSetHoldability() throws SQLException {
        return internalStmt.getResultSetHoldability();
    }

    /**
     * Method getResultSetType.
     * 
     * @return int
     * @throws SQLException
     * @see java.sql.Statement#getResultSetType()
     */
    @Override
    public int getResultSetType() throws SQLException {
        return internalStmt.getResultSetType();
    }

    /**
     * Method getUpdateCount.
     * 
     * @return int
     * @throws SQLException
     * @see java.sql.Statement#getUpdateCount()
     */
    @Override
    public int getUpdateCount() throws SQLException {
        return internalStmt.getUpdateCount();
    }

    /**
     * Method getWarnings.
     * 
     * @return SQLWarning
     * @throws SQLException
     * @see java.sql.Statement#getWarnings()
     */
    @Override
    public SQLWarning getWarnings() throws SQLException {
        return internalStmt.getWarnings();
    }

    /**
     * Method setCursorName.
     * 
     * @param name
     * @throws SQLException
     * @see java.sql.Statement#setCursorName(String)
     */
    @Override
    public void setCursorName(String name) throws SQLException {
        internalStmt.setCursorName(name);
    }

    /**
     * Method setEscapeProcessing.
     * 
     * @param enable
     * @throws SQLException
     * @see java.sql.Statement#setEscapeProcessing(boolean)
     */
    @Override
    public void setEscapeProcessing(boolean enable) throws SQLException {
        internalStmt.setEscapeProcessing(enable);
    }

    /**
     * Method setFetchDirection.
     * 
     * @param direction
     * @throws SQLException
     * @see java.sql.Statement#setFetchDirection(int)
     */
    @Override
    public void setFetchDirection(int direction) throws SQLException {
        this.fetchDirection = internalStmt.getFetchDirection();
        internalStmt.setFetchDirection(direction);
    }

    /**
     * Method setFetchSize.
     * 
     * @param rows
     * @throws SQLException
     * @see java.sql.Statement#setFetchSize(int)
     */
    @Override
    public void setFetchSize(int rows) throws SQLException {
        this.fetchSize = internalStmt.getFetchSize();
        internalStmt.setFetchSize(rows);
    }

    /**
     * Method setMaxFieldSize.
     * 
     * @param max
     * @throws SQLException
     * @see java.sql.Statement#setMaxFieldSize(int)
     */
    @Override
    public void setMaxFieldSize(int max) throws SQLException {
        this.maxFieldSize = internalStmt.getMaxFieldSize();
        internalStmt.setMaxFieldSize(max);
    }

    /**
     * Method setMaxRows.
     * 
     * @param max
     * @throws SQLException
     * @see java.sql.Statement#setMaxRows(int)
     */
    @Override
    public void setMaxRows(int max) throws SQLException {
        this.maxRows = internalStmt.getMaxRows();
        internalStmt.setMaxRows(max);
    }

    /**
     * Method setQueryTimeout.
     * 
     * @param seconds
     * @throws SQLException
     * @see java.sql.Statement#setQueryTimeout(int)
     */
    @Override
    public void setQueryTimeout(int seconds) throws SQLException {
        this.queryTimeout = internalStmt.getQueryTimeout();
        internalStmt.setQueryTimeout(seconds);
    }

    /**
     * Method setPoolable.
     * 
     * @param poolable
     * @throws SQLException
     * @see java.sql.Statement#setPoolable(boolean)
     */
    @Override
    public void setPoolable(boolean poolable) throws SQLException {
        internalStmt.setPoolable(poolable);
    }

    /**
     * Method isWrapperFor.
     * 
     * @param iface
     * @return boolean
     * @throws SQLException
     * @see java.sql.Wrapper#isWrapperFor(Class<?>)
     */
    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return internalStmt.isWrapperFor(iface);
    }

    /**
     * Method unwrap.
     * 
     * @param iface
     * @return T
     * @throws SQLException
     * @see java.sql.Wrapper#unwrap(Class<T>)
     */
    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return internalStmt.unwrap(iface);
    }

    /**
     * Method toString.
     * 
     * @return String
     */
    @Override
    public String toString() {
        return internalStmt.toString();
    }

    /**
     * Method hashCode.
     * 
     * @return int
     */
    @Override
    public int hashCode() {
        return internalStmt.hashCode();
    }

    /**
     * Method equals.
     * 
     * @param obj
     * @return boolean
     */
    @Override
    public boolean equals(Object obj) {
        return this == obj || (obj instanceof PoolablePreparedStatement && ((PoolablePreparedStatement) obj).internalStmt.equals(internalStmt));
    }

    @Override
    public void closeOnCompletion() throws SQLException {
        internalStmt.closeOnCompletion();
    }

    @Override
    public boolean isCloseOnCompletion() throws SQLException {
        return internalStmt.isCloseOnCompletion();
    }

    private int fetchSize = -1;
    private int fetchDirection = -1;
    private int maxRows = -1;
    private int maxFieldSize = -1;
    private int queryTimeout = -1;

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

    static class ResultSetProxy implements ResultSet {
        private final ResultSet internalRS;
        private final PoolablePreparedStatement poolableStmt;

        ResultSetProxy(ResultSet rs, PoolablePreparedStatement stmt) {
            this.internalRS = rs;
            this.poolableStmt = stmt;
        }

        @Override
        public <T> T unwrap(Class<T> iface) throws SQLException {
            return internalRS.unwrap(iface);
        }

        @Override
        public boolean isWrapperFor(Class<?> iface) throws SQLException {
            return internalRS.isWrapperFor(iface);
        }

        @Override
        public boolean next() throws SQLException {
            return internalRS.next();
        }

        @Override
        public void close() throws SQLException {
            internalRS.close();
        }

        @Override
        public boolean wasNull() throws SQLException {
            return internalRS.wasNull();
        }

        @Override
        public String getString(int columnIndex) throws SQLException {
            return internalRS.getString(columnIndex);
        }

        @Override
        public boolean getBoolean(int columnIndex) throws SQLException {
            return internalRS.getBoolean(columnIndex);
        }

        @Override
        public byte getByte(int columnIndex) throws SQLException {
            return internalRS.getByte(columnIndex);
        }

        @Override
        public short getShort(int columnIndex) throws SQLException {
            return internalRS.getShort(columnIndex);
        }

        @Override
        public int getInt(int columnIndex) throws SQLException {
            return internalRS.getInt(columnIndex);
        }

        @Override
        public long getLong(int columnIndex) throws SQLException {
            return internalRS.getLong(columnIndex);
        }

        @Override
        public float getFloat(int columnIndex) throws SQLException {
            return internalRS.getFloat(columnIndex);
        }

        @Override
        public double getDouble(int columnIndex) throws SQLException {
            return internalRS.getDouble(columnIndex);
        }

        @SuppressWarnings("deprecation")
        @Override
        public BigDecimal getBigDecimal(int columnIndex, int scale) throws SQLException {
            return internalRS.getBigDecimal(columnIndex, scale);
        }

        @Override
        public byte[] getBytes(int columnIndex) throws SQLException {
            return internalRS.getBytes(columnIndex);
        }

        @Override
        public Date getDate(int columnIndex) throws SQLException {
            return internalRS.getDate(columnIndex);
        }

        @Override
        public Time getTime(int columnIndex) throws SQLException {
            return internalRS.getTime(columnIndex);
        }

        @Override
        public Timestamp getTimestamp(int columnIndex) throws SQLException {
            return internalRS.getTimestamp(columnIndex);
        }

        @Override
        public InputStream getAsciiStream(int columnIndex) throws SQLException {
            return internalRS.getAsciiStream(columnIndex);
        }

        @SuppressWarnings("deprecation")
        @Override
        public InputStream getUnicodeStream(int columnIndex) throws SQLException {
            return internalRS.getUnicodeStream(columnIndex);
        }

        @Override
        public InputStream getBinaryStream(int columnIndex) throws SQLException {
            return internalRS.getBinaryStream(columnIndex);
        }

        @Override
        public String getString(String columnLabel) throws SQLException {
            return internalRS.getString(columnLabel);
        }

        @Override
        public boolean getBoolean(String columnLabel) throws SQLException {
            return internalRS.getBoolean(columnLabel);
        }

        @Override
        public byte getByte(String columnLabel) throws SQLException {
            return internalRS.getByte(columnLabel);
        }

        @Override
        public short getShort(String columnLabel) throws SQLException {
            return internalRS.getShort(columnLabel);
        }

        @Override
        public int getInt(String columnLabel) throws SQLException {
            return internalRS.getInt(columnLabel);
        }

        @Override
        public long getLong(String columnLabel) throws SQLException {
            return internalRS.getLong(columnLabel);
        }

        @Override
        public float getFloat(String columnLabel) throws SQLException {
            return internalRS.getFloat(columnLabel);
        }

        @Override
        public double getDouble(String columnLabel) throws SQLException {
            return internalRS.getDouble(columnLabel);
        }

        @SuppressWarnings("deprecation")
        @Override
        public BigDecimal getBigDecimal(String columnLabel, int scale) throws SQLException {
            return internalRS.getBigDecimal(columnLabel, scale);
        }

        @Override
        public byte[] getBytes(String columnLabel) throws SQLException {
            return internalRS.getBytes(columnLabel);
        }

        @Override
        public Date getDate(String columnLabel) throws SQLException {
            return internalRS.getDate(columnLabel);
        }

        @Override
        public Time getTime(String columnLabel) throws SQLException {
            return internalRS.getTime(columnLabel);
        }

        @Override
        public Timestamp getTimestamp(String columnLabel) throws SQLException {
            return internalRS.getTimestamp(columnLabel);
        }

        @Override
        public InputStream getAsciiStream(String columnLabel) throws SQLException {
            return internalRS.getAsciiStream(columnLabel);
        }

        @SuppressWarnings("deprecation")
        @Override
        public InputStream getUnicodeStream(String columnLabel) throws SQLException {
            return internalRS.getUnicodeStream(columnLabel);
        }

        @Override
        public InputStream getBinaryStream(String columnLabel) throws SQLException {
            return internalRS.getBinaryStream(columnLabel);
        }

        @Override
        public SQLWarning getWarnings() throws SQLException {
            return internalRS.getWarnings();
        }

        @Override
        public void clearWarnings() throws SQLException {
            internalRS.clearWarnings();
        }

        @Override
        public String getCursorName() throws SQLException {
            return internalRS.getCursorName();
        }

        @Override
        public ResultSetMetaData getMetaData() throws SQLException {
            return internalRS.getMetaData();
        }

        @Override
        public Object getObject(int columnIndex) throws SQLException {
            return internalRS.getObject(columnIndex);
        }

        @Override
        public Object getObject(String columnLabel) throws SQLException {
            return internalRS.getObject(columnLabel);
        }

        @Override
        public int findColumn(String columnLabel) throws SQLException {
            return internalRS.findColumn(columnLabel);
        }

        @Override
        public Reader getCharacterStream(int columnIndex) throws SQLException {
            return internalRS.getCharacterStream(columnIndex);
        }

        @Override
        public Reader getCharacterStream(String columnLabel) throws SQLException {
            return internalRS.getCharacterStream(columnLabel);
        }

        @Override
        public BigDecimal getBigDecimal(int columnIndex) throws SQLException {
            return internalRS.getBigDecimal(columnIndex);
        }

        @Override
        public BigDecimal getBigDecimal(String columnLabel) throws SQLException {
            return internalRS.getBigDecimal(columnLabel);
        }

        @Override
        public boolean isBeforeFirst() throws SQLException {
            return internalRS.isBeforeFirst();
        }

        @Override
        public boolean isAfterLast() throws SQLException {
            return internalRS.isAfterLast();
        }

        @Override
        public boolean isFirst() throws SQLException {
            return internalRS.isFirst();
        }

        @Override
        public boolean isLast() throws SQLException {
            return internalRS.isLast();
        }

        @Override
        public void beforeFirst() throws SQLException {
            internalRS.beforeFirst();
        }

        @Override
        public void afterLast() throws SQLException {
            internalRS.afterLast();
        }

        @Override
        public boolean first() throws SQLException {
            return internalRS.first();
        }

        @Override
        public boolean last() throws SQLException {
            return internalRS.last();
        }

        @Override
        public int getRow() throws SQLException {
            return internalRS.getRow();
        }

        @Override
        public boolean absolute(int row) throws SQLException {
            return internalRS.absolute(row);
        }

        @Override
        public boolean relative(int rows) throws SQLException {
            return internalRS.relative(rows);
        }

        @Override
        public boolean previous() throws SQLException {
            return internalRS.previous();
        }

        @Override
        public void setFetchDirection(int direction) throws SQLException {
            internalRS.setFetchDirection(direction);
        }

        @Override
        public int getFetchDirection() throws SQLException {
            return internalRS.getFetchDirection();
        }

        @Override
        public void setFetchSize(int rows) throws SQLException {
            internalRS.setFetchSize(rows);
        }

        @Override
        public int getFetchSize() throws SQLException {
            return internalRS.getFetchSize();
        }

        @Override
        public int getType() throws SQLException {
            return internalRS.getType();
        }

        @Override
        public int getConcurrency() throws SQLException {
            return internalRS.getConcurrency();
        }

        @Override
        public boolean rowUpdated() throws SQLException {
            return internalRS.rowUpdated();
        }

        @Override
        public boolean rowInserted() throws SQLException {
            return internalRS.rowInserted();
        }

        @Override
        public boolean rowDeleted() throws SQLException {
            return internalRS.rowDeleted();
        }

        @Override
        public void updateNull(int columnIndex) throws SQLException {
            internalRS.updateNull(columnIndex);
        }

        @Override
        public void updateBoolean(int columnIndex, boolean x) throws SQLException {
            internalRS.updateBoolean(columnIndex, x);
        }

        @Override
        public void updateByte(int columnIndex, byte x) throws SQLException {
            internalRS.updateByte(columnIndex, x);
        }

        @Override
        public void updateShort(int columnIndex, short x) throws SQLException {
            internalRS.updateShort(columnIndex, x);
        }

        @Override
        public void updateInt(int columnIndex, int x) throws SQLException {
            internalRS.updateInt(columnIndex, x);
        }

        @Override
        public void updateLong(int columnIndex, long x) throws SQLException {
            internalRS.updateLong(columnIndex, x);
        }

        @Override
        public void updateFloat(int columnIndex, float x) throws SQLException {
            internalRS.updateFloat(columnIndex, x);
        }

        @Override
        public void updateDouble(int columnIndex, double x) throws SQLException {
            internalRS.updateDouble(columnIndex, x);
        }

        @Override
        public void updateBigDecimal(int columnIndex, BigDecimal x) throws SQLException {
            internalRS.updateBigDecimal(columnIndex, x);
        }

        @Override
        public void updateString(int columnIndex, String x) throws SQLException {
            internalRS.updateString(columnIndex, x);
        }

        @Override
        public void updateBytes(int columnIndex, byte[] x) throws SQLException {
            internalRS.updateBytes(columnIndex, x);
        }

        @Override
        public void updateDate(int columnIndex, Date x) throws SQLException {
            internalRS.updateDate(columnIndex, x);
        }

        @Override
        public void updateTime(int columnIndex, Time x) throws SQLException {
            internalRS.updateTime(columnIndex, x);
        }

        @Override
        public void updateTimestamp(int columnIndex, Timestamp x) throws SQLException {
            internalRS.updateTimestamp(columnIndex, x);
        }

        @Override
        public void updateAsciiStream(int columnIndex, InputStream x, int length) throws SQLException {
            internalRS.updateAsciiStream(columnIndex, x, length);
        }

        @Override
        public void updateBinaryStream(int columnIndex, InputStream x, int length) throws SQLException {
            internalRS.updateBinaryStream(columnIndex, x, length);
        }

        @Override
        public void updateCharacterStream(int columnIndex, Reader x, int length) throws SQLException {
            internalRS.updateCharacterStream(columnIndex, x, length);
        }

        @Override
        public void updateObject(int columnIndex, Object x, int scaleOrLength) throws SQLException {
            internalRS.updateObject(columnIndex, x, scaleOrLength);
        }

        @Override
        public void updateObject(int columnIndex, Object x) throws SQLException {
            internalRS.updateObject(columnIndex, x);
        }

        @Override
        public void updateNull(String columnLabel) throws SQLException {
            internalRS.updateNull(columnLabel);
        }

        @Override
        public void updateBoolean(String columnLabel, boolean x) throws SQLException {
            internalRS.updateBoolean(columnLabel, x);
        }

        @Override
        public void updateByte(String columnLabel, byte x) throws SQLException {
            internalRS.updateByte(columnLabel, x);
        }

        @Override
        public void updateShort(String columnLabel, short x) throws SQLException {
            internalRS.updateShort(columnLabel, x);
        }

        @Override
        public void updateInt(String columnLabel, int x) throws SQLException {
            internalRS.updateInt(columnLabel, x);
        }

        @Override
        public void updateLong(String columnLabel, long x) throws SQLException {
            internalRS.updateLong(columnLabel, x);
        }

        @Override
        public void updateFloat(String columnLabel, float x) throws SQLException {
            internalRS.updateFloat(columnLabel, x);
        }

        @Override
        public void updateDouble(String columnLabel, double x) throws SQLException {
            internalRS.updateDouble(columnLabel, x);
        }

        @Override
        public void updateBigDecimal(String columnLabel, BigDecimal x) throws SQLException {
            internalRS.updateBigDecimal(columnLabel, x);
        }

        @Override
        public void updateString(String columnLabel, String x) throws SQLException {
            internalRS.updateString(columnLabel, x);
        }

        @Override
        public void updateBytes(String columnLabel, byte[] x) throws SQLException {
            internalRS.updateBytes(columnLabel, x);
        }

        @Override
        public void updateDate(String columnLabel, Date x) throws SQLException {
            internalRS.updateDate(columnLabel, x);
        }

        @Override
        public void updateTime(String columnLabel, Time x) throws SQLException {
            internalRS.updateTime(columnLabel, x);
        }

        @Override
        public void updateTimestamp(String columnLabel, Timestamp x) throws SQLException {
            internalRS.updateTimestamp(columnLabel, x);
        }

        @Override
        public void updateAsciiStream(String columnLabel, InputStream x, int length) throws SQLException {
            internalRS.updateAsciiStream(columnLabel, x, length);
        }

        @Override
        public void updateBinaryStream(String columnLabel, InputStream x, int length) throws SQLException {
            internalRS.updateBinaryStream(columnLabel, x, length);
        }

        @Override
        public void updateCharacterStream(String columnLabel, Reader reader, int length) throws SQLException {
            internalRS.updateCharacterStream(columnLabel, reader, length);
        }

        @Override
        public void updateObject(String columnLabel, Object x, int scaleOrLength) throws SQLException {
            internalRS.updateObject(columnLabel, x, scaleOrLength);
        }

        @Override
        public void updateObject(String columnLabel, Object x) throws SQLException {
            internalRS.updateObject(columnLabel, x);
        }

        @Override
        public void insertRow() throws SQLException {
            internalRS.insertRow();
        }

        @Override
        public void updateRow() throws SQLException {
            internalRS.updateRow();
        }

        @Override
        public void deleteRow() throws SQLException {
            internalRS.deleteRow();
        }

        @Override
        public void refreshRow() throws SQLException {
            internalRS.refreshRow();
        }

        @Override
        public void cancelRowUpdates() throws SQLException {
            internalRS.cancelRowUpdates();
        }

        @Override
        public void moveToInsertRow() throws SQLException {
            internalRS.moveToInsertRow();
        }

        @Override
        public void moveToCurrentRow() throws SQLException {
            internalRS.moveToCurrentRow();
        }

        @Override
        public Statement getStatement() throws SQLException {
            return poolableStmt;
        }

        @Override
        public Object getObject(int columnIndex, Map<String, Class<?>> map) throws SQLException {
            return internalRS.getObject(columnIndex, map);
        }

        @Override
        public Ref getRef(int columnIndex) throws SQLException {
            return internalRS.getRef(columnIndex);
        }

        @Override
        public Blob getBlob(int columnIndex) throws SQLException {
            return internalRS.getBlob(columnIndex);
        }

        @Override
        public Clob getClob(int columnIndex) throws SQLException {
            return internalRS.getClob(columnIndex);
        }

        @Override
        public Array getArray(int columnIndex) throws SQLException {
            return internalRS.getArray(columnIndex);
        }

        @Override
        public Object getObject(String columnLabel, Map<String, Class<?>> map) throws SQLException {
            return internalRS.getObject(columnLabel, map);
        }

        @Override
        public Ref getRef(String columnLabel) throws SQLException {
            return internalRS.getRef(columnLabel);
        }

        @Override
        public Blob getBlob(String columnLabel) throws SQLException {
            return internalRS.getBlob(columnLabel);
        }

        @Override
        public Clob getClob(String columnLabel) throws SQLException {
            return internalRS.getClob(columnLabel);
        }

        @Override
        public Array getArray(String columnLabel) throws SQLException {
            return internalRS.getArray(columnLabel);
        }

        @Override
        public Date getDate(int columnIndex, Calendar cal) throws SQLException {
            return internalRS.getDate(columnIndex, cal);
        }

        @Override
        public Date getDate(String columnLabel, Calendar cal) throws SQLException {
            return internalRS.getDate(columnLabel, cal);
        }

        @Override
        public Time getTime(int columnIndex, Calendar cal) throws SQLException {
            return internalRS.getTime(columnIndex, cal);
        }

        @Override
        public Time getTime(String columnLabel, Calendar cal) throws SQLException {
            return internalRS.getTime(columnLabel, cal);
        }

        @Override
        public Timestamp getTimestamp(int columnIndex, Calendar cal) throws SQLException {
            return internalRS.getTimestamp(columnIndex, cal);
        }

        @Override
        public Timestamp getTimestamp(String columnLabel, Calendar cal) throws SQLException {
            return internalRS.getTimestamp(columnLabel, cal);
        }

        @Override
        public URL getURL(int columnIndex) throws SQLException {
            return internalRS.getURL(columnIndex);
        }

        @Override
        public URL getURL(String columnLabel) throws SQLException {
            return internalRS.getURL(columnLabel);
        }

        @Override
        public void updateRef(int columnIndex, Ref x) throws SQLException {
            internalRS.updateRef(columnIndex, x);
        }

        @Override
        public void updateRef(String columnLabel, Ref x) throws SQLException {
            internalRS.updateRef(columnLabel, x);
        }

        @Override
        public void updateBlob(int columnIndex, Blob x) throws SQLException {
            internalRS.updateBlob(columnIndex, x);
        }

        @Override
        public void updateBlob(String columnLabel, Blob x) throws SQLException {
            internalRS.updateBlob(columnLabel, x);
        }

        @Override
        public void updateClob(int columnIndex, Clob x) throws SQLException {
            internalRS.updateClob(columnIndex, x);
        }

        @Override
        public void updateClob(String columnLabel, Clob x) throws SQLException {
            internalRS.updateClob(columnLabel, x);
        }

        @Override
        public void updateArray(int columnIndex, Array x) throws SQLException {
            internalRS.updateArray(columnIndex, x);
        }

        @Override
        public void updateArray(String columnLabel, Array x) throws SQLException {
            internalRS.updateArray(columnLabel, x);
        }

        @Override
        public RowId getRowId(int columnIndex) throws SQLException {
            return internalRS.getRowId(columnIndex);
        }

        @Override
        public RowId getRowId(String columnLabel) throws SQLException {
            return internalRS.getRowId(columnLabel);
        }

        @Override
        public void updateRowId(int columnIndex, RowId x) throws SQLException {
            internalRS.updateRowId(columnIndex, x);
        }

        @Override
        public void updateRowId(String columnLabel, RowId x) throws SQLException {
            internalRS.updateRowId(columnLabel, x);
        }

        @Override
        public int getHoldability() throws SQLException {
            return internalRS.getHoldability();
        }

        @Override
        public boolean isClosed() throws SQLException {
            return internalRS.isClosed();
        }

        @Override
        public void updateNString(int columnIndex, String nString) throws SQLException {
            internalRS.updateNString(columnIndex, nString);
        }

        @Override
        public void updateNString(String columnLabel, String nString) throws SQLException {
            internalRS.updateNString(columnLabel, nString);
        }

        @Override
        public void updateNClob(int columnIndex, NClob nClob) throws SQLException {
            internalRS.updateNClob(columnIndex, nClob);
        }

        @Override
        public void updateNClob(String columnLabel, NClob nClob) throws SQLException {
            internalRS.updateNClob(columnLabel, nClob);
        }

        @Override
        public NClob getNClob(int columnIndex) throws SQLException {
            return internalRS.getNClob(columnIndex);
        }

        @Override
        public NClob getNClob(String columnLabel) throws SQLException {
            return internalRS.getNClob(columnLabel);
        }

        @Override
        public SQLXML getSQLXML(int columnIndex) throws SQLException {
            return internalRS.getSQLXML(columnIndex);
        }

        @Override
        public SQLXML getSQLXML(String columnLabel) throws SQLException {
            return internalRS.getSQLXML(columnLabel);
        }

        @Override
        public void updateSQLXML(int columnIndex, SQLXML xmlObject) throws SQLException {
            internalRS.updateSQLXML(columnIndex, xmlObject);
        }

        @Override
        public void updateSQLXML(String columnLabel, SQLXML xmlObject) throws SQLException {
            internalRS.updateSQLXML(columnLabel, xmlObject);
        }

        @Override
        public String getNString(int columnIndex) throws SQLException {
            return internalRS.getNString(columnIndex);
        }

        @Override
        public String getNString(String columnLabel) throws SQLException {
            return internalRS.getNString(columnLabel);
        }

        @Override
        public Reader getNCharacterStream(int columnIndex) throws SQLException {
            return internalRS.getNCharacterStream(columnIndex);
        }

        @Override
        public Reader getNCharacterStream(String columnLabel) throws SQLException {
            return internalRS.getNCharacterStream(columnLabel);
        }

        @Override
        public void updateNCharacterStream(int columnIndex, Reader x, long length) throws SQLException {
            internalRS.updateNCharacterStream(columnIndex, x, length);
        }

        @Override
        public void updateNCharacterStream(String columnLabel, Reader reader, long length) throws SQLException {
            internalRS.updateNCharacterStream(columnLabel, reader, length);
        }

        @Override
        public void updateAsciiStream(int columnIndex, InputStream x, long length) throws SQLException {
            internalRS.updateAsciiStream(columnIndex, x, length);
        }

        @Override
        public void updateBinaryStream(int columnIndex, InputStream x, long length) throws SQLException {
            internalRS.updateBinaryStream(columnIndex, x, length);
        }

        @Override
        public void updateCharacterStream(int columnIndex, Reader x, long length) throws SQLException {
            internalRS.updateCharacterStream(columnIndex, x, length);
        }

        @Override
        public void updateAsciiStream(String columnLabel, InputStream x, long length) throws SQLException {
            internalRS.updateAsciiStream(columnLabel, x, length);
        }

        @Override
        public void updateBinaryStream(String columnLabel, InputStream x, long length) throws SQLException {
            internalRS.updateBinaryStream(columnLabel, x, length);
        }

        @Override
        public void updateCharacterStream(String columnLabel, Reader reader, long length) throws SQLException {
            internalRS.updateCharacterStream(columnLabel, reader, length);
        }

        @Override
        public void updateBlob(int columnIndex, InputStream inputStream, long length) throws SQLException {
            internalRS.updateBlob(columnIndex, inputStream, length);
        }

        @Override
        public void updateBlob(String columnLabel, InputStream inputStream, long length) throws SQLException {
            internalRS.updateBlob(columnLabel, inputStream, length);
        }

        @Override
        public void updateClob(int columnIndex, Reader reader, long length) throws SQLException {
            internalRS.updateClob(columnIndex, reader, length);
        }

        @Override
        public void updateClob(String columnLabel, Reader reader, long length) throws SQLException {
            internalRS.updateClob(columnLabel, reader, length);
        }

        @Override
        public void updateNClob(int columnIndex, Reader reader, long length) throws SQLException {
            internalRS.updateNClob(columnIndex, reader, length);
        }

        @Override
        public void updateNClob(String columnLabel, Reader reader, long length) throws SQLException {
            internalRS.updateNClob(columnLabel, reader, length);
        }

        @Override
        public void updateNCharacterStream(int columnIndex, Reader x) throws SQLException {
            internalRS.updateNCharacterStream(columnIndex, x);
        }

        @Override
        public void updateNCharacterStream(String columnLabel, Reader reader) throws SQLException {
            internalRS.updateNCharacterStream(columnLabel, reader);
        }

        @Override
        public void updateAsciiStream(int columnIndex, InputStream x) throws SQLException {
            internalRS.updateAsciiStream(columnIndex, x);
        }

        @Override
        public void updateBinaryStream(int columnIndex, InputStream x) throws SQLException {
            internalRS.updateBinaryStream(columnIndex, x);
        }

        @Override
        public void updateCharacterStream(int columnIndex, Reader x) throws SQLException {
            internalRS.updateCharacterStream(columnIndex, x);
        }

        @Override
        public void updateAsciiStream(String columnLabel, InputStream x) throws SQLException {
            internalRS.updateAsciiStream(columnLabel, x);
        }

        @Override
        public void updateBinaryStream(String columnLabel, InputStream x) throws SQLException {
            internalRS.updateBinaryStream(columnLabel, x);
        }

        @Override
        public void updateCharacterStream(String columnLabel, Reader reader) throws SQLException {
            internalRS.updateCharacterStream(columnLabel, reader);
        }

        @Override
        public void updateBlob(int columnIndex, InputStream inputStream) throws SQLException {
            internalRS.updateBlob(columnIndex, inputStream);
        }

        @Override
        public void updateBlob(String columnLabel, InputStream inputStream) throws SQLException {
            internalRS.updateBlob(columnLabel, inputStream);
        }

        @Override
        public void updateClob(int columnIndex, Reader reader) throws SQLException {
            internalRS.updateClob(columnIndex, reader);
        }

        @Override
        public void updateClob(String columnLabel, Reader reader) throws SQLException {
            internalRS.updateClob(columnLabel, reader);
        }

        @Override
        public void updateNClob(int columnIndex, Reader reader) throws SQLException {
            internalRS.updateNClob(columnIndex, reader);
        }

        @Override
        public void updateNClob(String columnLabel, Reader reader) throws SQLException {
            internalRS.updateNClob(columnLabel, reader);
        }

        @Override
        public <T> T getObject(int columnIndex, Class<T> type) throws SQLException {
            return internalRS.getObject(columnIndex, type);
        }

        @Override
        public <T> T getObject(String columnLabel, Class<T> type) throws SQLException {
            return internalRS.getObject(columnLabel, type);
        }
    }
}
