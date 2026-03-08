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
 * Enumeration representing distinct types of database operations.
 * Each operation type has an associated integer flag value that can participate in bitwise
 * combinations, although this enum itself models only the individual operations.
 * 
 * <p>This enum is typically used in ORM frameworks and data access layers to specify
 * what type of operation is being performed on entities or database records.</p>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * OperationType opType = OperationType.UPDATE;
 * int intValue = opType.intValue();   // Returns 4
 * 
 * // Convert from int value
 * OperationType retrieved = OperationType.valueOf(4);   // Returns UPDATE
 * }</pre>
 */
public enum OperationType {
    /**
     * Represents a query/read operation.
     * Associated integer value: 1
     */
    QUERY(1),
    /**
     * Represents an add/insert operation.
     * Associated integer value: 2
     */
    ADD(2),
    /**
     * Represents an update/modify operation.
     * Associated integer value: 4
     */
    UPDATE(4),
    /**
     * Represents a delete/remove operation.
     * Associated integer value: 8
     */
    DELETE(8);

    private final int intValue;

    OperationType(final int intValue) {
        this.intValue = intValue;
    }

    /**
     * Returns the integer flag associated with this operation type.
     * The returned value can be combined with other flags in external code, but
     * {@link #valueOf(int)} accepts only the exact values defined by this enum.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * int readWrite = OperationType.QUERY.intValue() | OperationType.UPDATE.intValue();
     * // readWrite = 5 (binary: 101)
     * }</pre>
     *
     * @return the integer value of this operation type
     */
    public int intValue() {
        return intValue;
    }

    /**
     * Returns the {@code OperationType} corresponding to the specified integer value.
     * This method resolves only the exact flag value for a single enum constant.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * OperationType op = OperationType.valueOf(2);
     * // op = OperationType.ADD
     * }</pre>
     *
     * @param intValue the integer value to convert
     * @return the corresponding {@code OperationType}
     * @throws IllegalArgumentException if {@code intValue} does not match one of the defined
     *         enum constants exactly
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
