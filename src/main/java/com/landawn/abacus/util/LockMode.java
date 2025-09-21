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
 * Represents different lock modes for database operations, where each mode controls what operations
 * are permitted or restricted. The lock modes use a bitmask pattern where:
 * <ul>
 * <li>{@code R} (Read) - Prevents others from reading by bean ID (queries by condition are still allowed)</li>
 * <li>{@code A} (Add) - Prevents others from inserting new records</li>
 * <li>{@code U} (Update) - Prevents others from modifying existing records</li>
 * <li>{@code D} (Delete) - Prevents others from deleting records</li>
 * </ul>
 * 
 * <p>Lock modes can be combined to create composite locks that restrict multiple operations.
 * For example, RU prevents both reading by ID and updating.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * LockMode mode = LockMode.RU;  // Prevents read by ID and update
 * if (mode.isXLockOf(LockMode.R)) {
 *     // This lock includes read restriction
 * }
 * }</pre>
 *
 * <p>Note: Some lock modes marked with {@code @Deprecated} are not currently supported.</p>
 */
public enum LockMode {
    /**
     * Read lock mode. When this lock is held, others cannot read by bean ID,
     * though queries by condition are still permitted.
     */
    R(1),

    /**
     * Add lock mode. When this lock is held, others cannot insert new records.
     *
     * @deprecated not supported at present
     */
    @Deprecated
    A(2),

    /**
     * Update lock mode. When this lock is held, others cannot modify existing records.
     */
    U(4),

    /**
     * Delete lock mode. When this lock is held, others cannot delete records.
     */
    D(8),

    /**
     * Combined Read-Add lock mode. When this lock is held, others cannot read by bean ID
     * or insert new records.
     *
     * @deprecated not supported at present
     */
    @Deprecated
    RA(R.intValue + A.intValue),

    /**
     * Combined Read-Update lock mode. When this lock is held, others cannot read by bean ID
     * or modify existing records.
     */
    RU(R.intValue + U.intValue),

    /**
     * Combined Read-Delete lock mode. When this lock is held, others cannot read by bean ID
     * or delete records.
     */
    RD(R.intValue + D.intValue),

    /**
     * Combined Add-Update lock mode. When this lock is held, others cannot insert new records
     * or modify existing records.
     *
     * @deprecated not supported at present
     */
    @Deprecated
    AU(A.intValue + U.intValue),

    /**
     * Combined Add-Delete lock mode. When this lock is held, others cannot insert new records
     * or delete records.
     *
     * @deprecated not supported at present
     */
    @Deprecated
    AD(A.intValue + D.intValue),

    /**
     * Combined Update-Delete lock mode. When this lock is held, others cannot modify existing records
     * or delete records.
     */
    UD(U.intValue + D.intValue),

    /**
     * Combined Read-Add-Update lock mode. When this lock is held, others cannot read by bean ID,
     * insert new records, or modify existing records.
     *
     * @deprecated not supported at present
     */
    @Deprecated
    RAU(R.intValue + A.intValue + U.intValue),

    /**
     * Combined Read-Add-Delete lock mode. When this lock is held, others cannot read by bean ID,
     * insert new records, or delete records.
     *
     * @deprecated not supported at present
     */
    @Deprecated
    RAD(R.intValue + A.intValue + D.intValue),

    /**
     * Combined Read-Update-Delete lock mode. When this lock is held, others cannot read by bean ID,
     * modify existing records, or delete records.
     */
    RUD(R.intValue + U.intValue + D.intValue),

    /**
     * Combined Add-Update-Delete lock mode. When this lock is held, others cannot insert new records,
     * modify existing records, or delete records.
     *
     * @deprecated not supported at present
     */
    @Deprecated
    AUD(A.intValue + U.intValue + D.intValue),

    /**
     * Combined Read-Add-Update-Delete lock mode. This is the most restrictive lock mode.
     * When this lock is held, others cannot read by bean ID, insert new records,
     * modify existing records, or delete records.
     *
     * @deprecated not supported at present
     */
    @Deprecated
    RAUD(R.intValue + A.intValue + U.intValue + D.intValue);

    /**
     * The integer value representing this lock mode as a bitmask.
     */
    private final int intValue;

    /**
     * Constructs a LockMode with the specified integer value.
     *
     * @param value the integer value representing this lock mode as a bitmask
     */
    LockMode(final int value) {
        intValue = value;
    }

    /**
     * Returns the integer value of this lock mode. The value is a bitmask where each bit
     * represents a different lock type (R=1, A=2, U=4, D=8).
     *
     * <p>Example:</p>
     * <pre>{@code
     * int value = LockMode.RU.intValue(); // Returns 5 (1 + 4)
     * }</pre>
     *
     * @return the integer value of this lock mode
     */
    public int intValue() {
        return intValue;
    }

    /**
     * Returns the LockMode corresponding to the specified integer value. The value should be
     * a valid combination of the lock mode bits (R=1, A=2, U=4, D=8).
     *
     * <p>Example:</p>
     * <pre>{@code
     * LockMode mode = LockMode.valueOf(5); // Returns LockMode.RU
     * LockMode mode2 = LockMode.valueOf(12); // Returns LockMode.UD
     * }</pre>
     *
     * @param intValue the integer value to convert to a LockMode
     * @return the corresponding LockMode
     * @throws IllegalArgumentException if the intValue does not correspond to a valid LockMode
     */
    public static LockMode valueOf(final int intValue) {
        switch (intValue) {
            case 1:
                return R;

            case 2:
                return A;

            case 4:
                return U;

            case 8:
                return D;

            case 3:
                return RA;

            case 5:
                return RU;

            case 9:
                return RD;

            case 6:
                return AU;

            case 10:
                return AD;

            case 12:
                return UD;

            case 7:
                return RAU;

            case 11:
                return RAD;

            case 13:
                return RUD;

            case 14:
                return AUD;

            case 15:
                return RAUD;

            default:
                throw new IllegalArgumentException("Invalid lock mode value[" + intValue + "]. ");
        }
    }

    /**
     * Checks if this LockMode is locked by (is a subset of) the specified LockMode.
     * This method uses bitwise AND operation to determine if all lock types in this mode
     * are also present in the specified mode.
     *
     * <p>Example:</p>
     * <pre>{@code
     * LockMode currentLock = LockMode.R;
     * LockMode checkLock = LockMode.RU;
     * boolean isLocked = currentLock.isXLockOf(checkLock); // Returns true
     * 
     * LockMode currentLock2 = LockMode.UD;
     * LockMode checkLock2 = LockMode.U;
     * boolean isLocked2 = currentLock2.isXLockOf(checkLock2); // Returns true
     * }</pre>
     *
     * @param lockMode the LockMode to check against
     * @return {@code true} if this LockMode's operations are locked by the specified lockMode, {@code false} otherwise
     */
    public boolean isXLockOf(final LockMode lockMode) {
        return (intValue & lockMode.intValue) > 0;
    }
}