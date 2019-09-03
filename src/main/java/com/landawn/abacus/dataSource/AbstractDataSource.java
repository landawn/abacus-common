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

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;

import javax.sql.DataSource;

import com.landawn.abacus.util.ClassUtil;

// TODO: Auto-generated Javadoc
/**
 * The Class AbstractDataSource.
 *
 * @author Haiyang Li
 * @since 0.8
 */
public abstract class AbstractDataSource implements DataSource {

    /**
     * Gets the connection.
     *
     * @param username
     * @param password
     * @return
     * @throws SQLException the SQL exception
     */
    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Gets the parent logger.
     *
     * @return
     * @throws SQLFeatureNotSupportedException the SQL feature not supported exception
     */
    @Override
    public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new UnsupportedOperationException();
    }

    /**
     * Gets the log writer.
     *
     * @return
     * @throws SQLException the SQL exception
     */
    @Override
    public PrintWriter getLogWriter() throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Sets the log writer.
     *
     * @param out the new log writer
     * @throws SQLException the SQL exception
     */
    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Sets the login timeout.
     *
     * @param seconds the new login timeout
     * @throws SQLException the SQL exception
     */
    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Gets the login timeout.
     *
     * @return
     * @throws SQLException the SQL exception
     */
    @Override
    public int getLoginTimeout() throws SQLException {
        throw new UnsupportedOperationException();
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
        if (isWrapperFor(iface)) {
            return (T) this;
        } else {
            throw new IllegalArgumentException(
                    ClassUtil.getCanonicalClassName(this.getClass()) + " doesn't implemented interface: " + ClassUtil.getCanonicalClassName(iface));
        }
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
        return iface.isAssignableFrom(this.getClass());
    }
}
