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
 * Enumeration representing various states of a service lifecycle.
 * 
 * <p>This enum provides a standardized way to track and manage service states throughout
 * their lifecycle, from initial default state through various operational and terminal states.
 * Each status has an associated integer value for efficient storage and comparison.</p>
 * 
 * <p><b>Status progression example:</b></p>
 * <pre>{@code
 * ServiceStatus status = ServiceStatus.DEFAULT;
 * // Service becomes active
 * status = ServiceStatus.ACTIVE;
 * // Service expires
 * status = ServiceStatus.EXPIRED;
 * 
 * // Convert from stored integer
 * int storedValue = 2;
 * ServiceStatus restoredStatus = ServiceStatus.valueOf(storedValue); // SUSPENDED
 * }</pre>
 * 
 * @see AccountStatus
 * @see Status
 * @see WeekDay
 * @see Color
 */
public enum ServiceStatus {
    /**
     * Default initial state of a service (value: 0).
     * Typically represents a newly created or uninitialized service.
     */
    DEFAULT(0),

    /**
     * Service is currently active and operational (value: 1).
     * The service is running normally and accepting requests.
     */
    ACTIVE(1),

    /**
     * Service has been temporarily suspended (value: 2).
     * The service is paused but can potentially be reactivated.
     */
    SUSPENDED(2),

    /**
     * Service has expired due to time constraints (value: 3).
     * The service validity period has ended.
     */
    EXPIRED(3),

    /**
     * Service has been concluded successfully (value: 4).
     * The service completed its intended purpose and ended normally.
     */
    CONCLUDED(4),

    /**
     * Service has been revoked by administrative action (value: 5).
     * The service was terminated due to policy violation or administrative decision.
     */
    REVOKED(5),

    /**
     * Service has been refunded (value: 6).
     * Applicable for paid services where a refund was issued.
     */
    REFUNDED(6),

    /**
     * Service has been cancelled (value: 7).
     * The service was terminated at user request or due to other cancellation reasons.
     */
    CANCELLED(7);

    private final int intValue;

    ServiceStatus(final int intValue) {
        this.intValue = intValue;
    }

    /**
     * Returns the integer value associated with this service status.
     * 
     * <p>This value can be used for efficient storage in databases or for
     * transmission over networks where integer representation is preferred.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ServiceStatus status = ServiceStatus.ACTIVE;
     * int value = status.intValue(); // returns 1
     * // Store in database
     * database.saveStatus(serviceId, value);
     * }</pre>
     * 
     * @return the integer value of this status
     */
    public int intValue() {
        return intValue;
    }

    /**
     * Returns the ServiceStatus corresponding to the specified integer value.
     * 
     * <p>This method provides a way to reconstruct a ServiceStatus from its
     * integer representation, useful when retrieving status values from storage
     * or network protocols.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Retrieve from database
     * int storedValue = database.getStatus(serviceId);
     * ServiceStatus status = ServiceStatus.valueOf(storedValue);
     * 
     * if (status == ServiceStatus.ACTIVE) {
     *     // Handle active service
     * }
     * }</pre>
     *
     * @param intValue the integer value to convert to ServiceStatus
     * @return the ServiceStatus corresponding to the given integer value
     * @throws IllegalArgumentException if no ServiceStatus exists for the given integer value
     */
    public static ServiceStatus valueOf(final int intValue) {
        switch (intValue) {
            case 0:
                return DEFAULT;

            case 1:
                return ACTIVE;

            case 2:
                return SUSPENDED;

            case 3:
                return EXPIRED;

            case 4:
                return CONCLUDED;

            case 5:
                return REVOKED;

            case 6:
                return REFUNDED;

            case 7:
                return CANCELLED;

            default:
                throw new IllegalArgumentException("No mapping instance found by int value: " + intValue);
        }
    }
}
