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

/**
 * <code>R</code> --- Read; <code>A</code> --- Add; <code>U</code> --- Update; <code>D</code> --- Delete.
 * 
 * @since 0.8
 * 
 * @author Haiyang Li
 */
public enum LockMode {
    /**
     * Others can't read by entity id (but can query by condition) if lock on this level.
     */
    R(1),
    /**
     * Others can't add(insert) if lock on this level.
     * 
     * @deprecated not supported at present.
     */
    A(2),
    /**
     * Others can't modify(update) if lock on this level.
     */
    U(4),
    /**
     * Others can't delete if lock on this level.
     */
    D(8),
    /**
     * Others can't read by entity id (but can query by condition) and add(insert) if lock on this level.
     * 
     * @deprecated not supported at present.
     */
    RA(R.intValue + A.intValue),
    /**
     * Others can't read by entity id (but can query by condition) and modify(update) if lock on this level.
     */
    RU(R.intValue + U.intValue),
    /**
     * Others can't read by entity id (but can query by condition) and delete if lock on this level.
     */
    RD(R.intValue + D.intValue),
    /**
     * Others can't add(insert) and modify(update) if lock on this level.
     * 
     * @deprecated not supported at present.
     */
    AU(A.intValue + U.intValue),
    /**
     * Others can't add(insert) and delete if lock on this level.
     * 
     * @deprecated not supported at present.
     */
    AD(A.intValue + D.intValue),
    /**
     * Others can't modify(update) and delete if lock on this level.
     */
    UD(U.intValue + D.intValue),
    /**
     * Others can't read by entity id (but can query by condition), add(insert) and modify(update) if lock on this
     * level.
     * 
     * @deprecated not supported at present.
     */
    RAU(R.intValue + A.intValue + U.intValue),
    /**
     * Others can't read by entity id (but can query by condition), add(insert) and delete if lock on this level.
     * 
     * @deprecated not supported at present.
     */
    RAD(R.intValue + A.intValue + D.intValue),
    /**
     * Others can't read by entity id (but can query by condition), modify(update) and delete if lock on this level.
     */
    RUD(R.intValue + U.intValue + D.intValue),
    /**
     * Others can't add(insert), modify(update) and delete if lock on this level.
     * 
     * @deprecated not supported at present.
     */
    AUD(A.intValue + U.intValue + D.intValue),

    /**
     * Others read by entity id (but can query by condition), add(insert), modify(update) and delete if lock on this
     * level.
     * 
     * @deprecated not supported at present.
     */
    RAUD(R.intValue + A.intValue + U.intValue + D.intValue);
    /**
     *
     */
    private final int intValue;

    /**
     * Constructor.
     * 
     * @param value
     */
    private LockMode(int value) {
        intValue = value;
    }

    /**
     * Method intValue.
     * 
     * @return int
     */
    public int intValue() {
        return intValue;
    }

    /**
     * Method valueOf.
     * 
     * @param intValue
     * @return LockMode
     */
    public static LockMode valueOf(int intValue) {
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
     * Check if this {@code LockMode} is locked by the specified {@code byLockMode}.
     * 
     * @param lockMode
     * @return boolean
     */
    public boolean isXLockOf(LockMode lockMode) {
        return (intValue & lockMode.intValue) > 0;
    }
}
