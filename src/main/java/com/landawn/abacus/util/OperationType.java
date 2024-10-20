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

package com.landawn.abacus.util;

/**
 * The Enum OperationType.
 *
 */
public enum OperationType {
    /**
     * Field QUERY.
     */
    QUERY(1),
    /**
     * Field ADD.
     */
    ADD(2),
    /**
     * Field UPDATE.
     */
    UPDATE(4),
    /**
     * Field DELETE.
     */
    DELETE(8);

    private final int intValue;

    OperationType(final int intValue) {
        this.intValue = intValue;
    }

    /**
     *
     *
     * @return
     */
    public int intValue() {
        return intValue;
    }

    /**
     *
     * @param intValue
     * @return
     */
    public static OperationType valueOf(final int intValue) {
        switch (intValue) {
            case 1:
                return QUERY;

            case 2:
                return ADD;

            case 4:
                return UPDATE;

            case 8:
                return DELETE;

            default:
                throw new IllegalArgumentException("Not found the mapping OperationType for int value[" + intValue + "]. ");
        }
    }
}
