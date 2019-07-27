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

package com.landawn.abacus;

import com.landawn.abacus.exception.UncheckedSQLException;

/**
 * 
 * @since 0.8
 * 
 * @author Haiyang Li
 */
public interface Transaction {
    /**
     * Returns the identifier of this transaction if it's supported.
     * 
     * @return
     */
    String id();

    /**
     * 
     * @return
     */
    IsolationLevel isolationLevel();

    /**
     * Method status.
     * 
     * @return Status
     */
    Status status();

    /**
     * 
     * @return
     */
    boolean isActive();

    /**
     * Method commit.
     * 
     * @throws UncheckedSQLException
     */
    void commit() throws UncheckedSQLException;

    /**
     * Method rollback.
     * 
     * @throws UncheckedSQLException
     */
    void rollback() throws UncheckedSQLException;

    /**
     * @author Haiyang Li
     * 
     * @version $Revision: 0.8 $ 07/01/15
     */
    enum Status {
        /**
         * Field ACTIVE.
         */
        ACTIVE,
        /**
         * Field MARKED_ROLLBACK.
         */
        MARKED_ROLLBACK,
        /**
         * Field COMMITTED.
         */
        COMMITTED,
        /**
         * Field FAILED_COMMIT.
         */
        FAILED_COMMIT,
        /**
         * Field ROLLBACKED.
         */
        ROLLED_BACK,
        /**
         * Field FAILED_ROLLBACK.
         */
        FAILED_ROLLBACK;
    }

    /**
     * 
     * @author Haiyang Li
     * 
     */
    enum Action {

        /**
         * 
         */
        COMMIT,

        /**
         * 
         */
        ROLLBACK;
    }
}
