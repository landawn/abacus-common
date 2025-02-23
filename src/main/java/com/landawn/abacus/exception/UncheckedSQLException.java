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

package com.landawn.abacus.exception;

import java.io.Serial;
import java.sql.SQLException;

public class UncheckedSQLException extends UncheckedException {

    @Serial
    private static final long serialVersionUID = 9083988895292299710L;

    private final SQLException sqlException;

    /**
     * Constructor for UncheckedSQLException.
     *
     * @param cause
     */
    public UncheckedSQLException(final SQLException cause) {
        super(cause);
        sqlException = cause;

    }

    /**
     * Constructor for UncheckedSQLException.
     *
     * @param errorMsg
     * @param cause
     */
    public UncheckedSQLException(final String errorMsg, final SQLException cause) {
        super(errorMsg, cause);
        sqlException = cause;
    }

    /**
     * Retrieves the SQLState for the underlying SQLException.
     *
     * @return the SQLState string of the SQLException
     */
    public String getSQLState() {
        return sqlException.getSQLState();
    }

    /**
     * Retrieves the error code for the underlying SQLException.
     *
     * @return the error code of the SQLException
     */
    public int getErrorCode() {
        return sqlException.getErrorCode();
    }

}
