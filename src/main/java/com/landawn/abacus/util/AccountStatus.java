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

package com.landawn.abacus.util;

/**
 * Enumeration representing various states of an account lifecycle.
 * Each status has an associated integer value that can be used for persistence
 * or integration with systems that require numeric status codes.
 * 
 * <p>The account statuses follow a typical lifecycle progression:</p>
 * <ul>
 *   <li>DEFAULT (0) - Initial state of an account</li>
 *   <li>ACTIVE (1) - Account is active and fully functional</li>
 *   <li>SUSPENDED (2) - Account is temporarily disabled</li>
 *   <li>RETIRED (3) - Account is no longer in active use but retained</li>
 *   <li>CLOSED (4) - Account has been formally closed</li>
 *   <li>DELETED (5) - Account has been deleted from the system</li>
 * </ul>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * AccountStatus status = AccountStatus.ACTIVE;
 * int statusCode = status.code();   // Returns 1
 * 
 * // Convert from int to enum
 * AccountStatus restored = AccountStatus.fromCode(1);   // Returns ACTIVE
 * 
 * // Use in switch statements
 * switch (status) {
 *     case ACTIVE:
 *         // Handle active account
 *         break;
 *     case SUSPENDED:
 *         // Handle suspended account
 *         break;
 *     default:
 *         // Handle other statuses
 * }
 * }</pre>
 * 
 * @see ServiceStatus
 * @see UnifiedStatus
 * @see WeekDay
 * @see Color
 */
public enum AccountStatus {

    /** Default initial state of an account (value: 0) */
    BLANK(0),

    /** Account is active and fully functional (value: 1) */
    ACTIVE(1),

    /** Account is temporarily disabled (value: 2) */
    SUSPENDED(2),

    /** Account is no longer in active use but retained for historical purposes (value: 3) */
    RETIRED(3),

    /** Account has been formally closed (value: 4) */
    CLOSED(4),

    /** Account has been deleted from the system (value: 5) */
    DELETED(5);

    private final int code;

    AccountStatus(final int code) {
        this.code = code;
    }

    /**
     * Returns the integer value associated with this account status.
     * This can be useful for database persistence or API communication
     * where numeric codes are preferred.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * AccountStatus status = AccountStatus.SUSPENDED;
     * int code = status.code();   // Returns 2
     * }</pre>
     *
     * @return the integer value of this status
     */
    public int code() {
        return code;
    }

    /**
     * Returns the AccountStatus corresponding to the specified integer value.
     * This method performs a reverse lookup from integer code to enum constant.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * AccountStatus status = AccountStatus.fromCode(1);      // Returns ACTIVE
     * AccountStatus suspended = AccountStatus.fromCode(2);   // Returns SUSPENDED
     * }</pre>
     *
     * @param code the integer value to convert
     * @return the corresponding AccountStatus
     * @throws IllegalArgumentException if no AccountStatus exists with the specified integer value
     */
    public static AccountStatus fromCode(final int code) {
        switch (code) {
            case 0:
                return BLANK;

            case 1:
                return ACTIVE;

            case 2:
                return SUSPENDED;

            case 3:
                return RETIRED;

            case 4:
                return CLOSED;

            case 5:
                return DELETED;

            default:
                throw new IllegalArgumentException("No mapping instance found by int value: " + code);
        }
    }
}
