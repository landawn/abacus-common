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

import java.sql.Connection;

/**
 * 
 * @since 0.8
 * 
 * @author Haiyang Li
 * 
 * @see <a href="http://docs.oracle.com/javase/tutorial/jdbc/basics/transactions.html">http://docs.oracle.com/javase/tutorial/jdbc/basics/transactions.html</a>
 */
public enum IsolationLevel {
    /**
     * Field DEFAULT. No {@code Connection.setTransactionIsolation(...)} will be called to set transaction isolation
     * level.
     */
    DEFAULT(-1),

    /**
     * Field NONE.
     * @deprecated
     */
    NONE(Connection.TRANSACTION_NONE),

    /**
     * Field READ_UNCOMMITTED.
     */
    READ_UNCOMMITTED(Connection.TRANSACTION_READ_UNCOMMITTED),

    /**
     * Field READ_COMMITTED.
     */
    READ_COMMITTED(Connection.TRANSACTION_READ_COMMITTED),

    /**
     * Field REPEATABLE_READ.
     */
    REPEATABLE_READ(Connection.TRANSACTION_REPEATABLE_READ),

    /**
     * Field SERIALIZABLE.
     */
    SERIALIZABLE(Connection.TRANSACTION_SERIALIZABLE);

    /**
     * Field intValue.
     */
    private final int intValue;

    /**
     * Constructor.
     * 
     * @param intValue
     */
    IsolationLevel(int intValue) {
        this.intValue = intValue;
    }

    /**
     * Method intValue
     * 
     * @return int
     */
    public int intValue() {
        return intValue;
    }

    /**
     * Method valueOf
     * 
     * @param intValue
     * @return IsolationLevel
     */
    public static IsolationLevel valueOf(int intValue) {
        switch (intValue) {
            case -1:
                return DEFAULT;

            case Connection.TRANSACTION_NONE:
                return NONE;

            case Connection.TRANSACTION_READ_UNCOMMITTED:
                return READ_UNCOMMITTED;

            case Connection.TRANSACTION_READ_COMMITTED:
                return READ_COMMITTED;

            case Connection.TRANSACTION_REPEATABLE_READ:
                return REPEATABLE_READ;

            case Connection.TRANSACTION_SERIALIZABLE:
                return SERIALIZABLE;

            default:
                throw new IllegalArgumentException("Invalid isolation level value: " + intValue + ".");
        }
    }
}
