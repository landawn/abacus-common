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

// TODO: Auto-generated Javadoc
/**
 * The Interface Transaction.
 *
 * @author Haiyang Li
 * @since 0.8
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
     * 
     * @return Status
     */
    Status status();

    /**
     * Checks if is active.
     *
     * @return true, if is active
     */
    boolean isActive();

    /**
     *
     * @throws UncheckedSQLException the unchecked SQL exception
     */
    void commit() throws UncheckedSQLException;

    /**
     *
     * @throws UncheckedSQLException the unchecked SQL exception
     */
    void rollback() throws UncheckedSQLException;

    /**
     * The Enum Status.
     *
     * @author Haiyang Li
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
     * The Enum Action.
     *
     * @author Haiyang Li
     */
    enum Action {

        /** The commit. */
        COMMIT,

        /** The rollback. */
        ROLLBACK;
    }
}
