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

package com.landawn.abacus.exception;

import java.sql.SQLException;

/**
 * 
 * @since 0.8
 * 
 * @author Haiyang Li
 */
public class UncheckedSQLException extends UncheckedException {
    /**
     * Field serialVersionUID.
     */
    private static final long serialVersionUID = 3184989675852404972L;

    /**
     * Constructor for UncheckedSQLException.
     * 
     * @param cause
     */
    public UncheckedSQLException(SQLException cause) {
        super(cause);

    }

    /**
     * Constructor for UncheckedSQLException.
     * 
     * @param errorMsg
     */
    public UncheckedSQLException(String errorMsg, SQLException cause) {
        super(errorMsg, cause);
    }

    /**
     * Method getSQLState.
     * 
     * @return String
     */
    public String getSQLState() {
        return getCause().getSQLState();
    }

    /**
     * Method getErrorCode
     * 
     * @return int
     */
    public int getErrorCode() {
        return getCause().getErrorCode();
    }

    /**
     * Returns the cause of this exception.
     *
     * @return  the {@code IOException} which is the cause of this exception.
     */
    @Override
    public SQLException getCause() {
        return (SQLException) super.getCause();
    }
}
