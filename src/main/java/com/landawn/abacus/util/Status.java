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
 * Enumeration representing various status states for entities, services, processes, and operations.
 * Each status has an associated numeric code for efficient storage and comparison.
 * 
 * <p>This enum provides a comprehensive set of status values commonly used in applications:
 * <ul>
 *   <li>General account/service/object statuses (ACTIVE, SUSPENDED, EXPIRED, etc.)</li>
 *   <li>Running statuses (STARTED, RUNNING, PAUSED, STOPPED)</li>
 *   <li>Order/process statuses (PREPARING, PROCESSING, PENDING, etc.)</li>
 *   <li>Development statuses (DEVELOPING, TESTING, RELEASED, DEPLOYED)</li>
 *   <li>Privilege statuses (READ_ONLY, UPDATABLE, INSERTABLE, WRITABLE)</li>
 *   <li>State statuses (FROZEN, LOCKED, AVAILABLE, DISABLED, etc.)</li>
 * </ul>
 * 
 * <p>The status codes are designed to be stored efficiently in databases and provide
 * fast lookups through the {@link #fromCode(int)} method.
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Using status in an entity
 * Status orderStatus = Status.PROCESSING;
 * int statusCode = orderStatus.getCode(); // Returns 42
 * 
 * // Retrieving status from code
 * Status retrieved = Status.fromCode(42); // Returns PROCESSING
 * 
 * // Checking status
 * if (userStatus == Status.ACTIVE) {
 *     // Allow access
 * }
 * }</pre>
 * 
 * @see ServiceStatus
 * @see AccountStatus
 * @see WeekDay
 * @see Color
 */
public enum Status {

    // General account/service/object status.

    /**
     * Blank/uninitialized status (code: 0).
     * Represents an unset or default state.
     */
    BLANK(0),

    /**
     * Active status (code: 2).
     * Indicates the entity is currently active and operational.
     */
    ACTIVE(2),

    /**
     * Suspended status (code: 4).
     * Indicates temporary deactivation that can be reversed.
     */
    SUSPENDED(4),

    /**
     * Expired status (code: 6).
     * Indicates the entity has passed its valid time period.
     */
    EXPIRED(6),

    /**
     * Canceled status (code: 8).
     * Indicates intentional termination before completion.
     */
    CANCELED(8),

    /**
     * Closed status (code: 10).
     * Indicates normal termination or completion.
     */
    CLOSED(10),

    /**
     * Removed status (code: 12).
     * Indicates the entity has been removed from active use.
     */
    REMOVED(12),

    /**
     * Deleted status (code: 14).
     * Indicates permanent removal or deletion.
     */
    DELETED(14),

    /**
     * Concluded status (code: 16).
     * Indicates successful completion of a process or agreement.
     */
    CONCLUDED(16),

    /**
     * Revoked status (code: 18).
     * Indicates withdrawal of previously granted rights or status.
     */
    REVOKED(18),

    /**
     * Refunded status (code: 20).
     * Indicates money or resources have been returned.
     */
    REFUNDED(20),

    /**
     * Completed status (code: 22).
     * Indicates successful finishing of a task or process.
     */
    COMPLETED(22),

    /**
     * Terminated status (code: 24).
     * Indicates forced ending of a process or agreement.
     */
    TERMINATED(24),

    /**
     * Retired status (code: 26).
     * Indicates planned phase-out or end of service.
     */
    RETIRED(26),

    // Running status.

    /**
     * Started status (code: 30).
     * Indicates a process has begun execution.
     */
    STARTED(30),

    /**
     * Running status (code: 32).
     * Indicates a process is currently executing.
     */
    RUNNING(32),

    /**
     * Paused status (code: 34).
     * Indicates temporary suspension of execution.
     */
    PAUSED(34),

    /**
     * Stopped status (code: 36).
     * Indicates execution has been halted.
     */
    STOPPED(36),

    // order/process status.

    /**
     * Preparing status (code: 40).
     * Indicates initial preparation phase.
     */
    PREPARING(40),

    /**
     * Processing status (code: 42).
     * Indicates active processing is occurring.
     */
    PROCESSING(42),

    /**
     * Processed status (code: 44).
     * Indicates processing has been completed.
     */
    PROCESSED(44),

    /**
     * Pending status (code: 46).
     * Indicates waiting for action or approval.
     */
    PENDING(46),

    /**
     * Approved status (code: 48).
     * Indicates formal approval has been granted.
     */
    APPROVED(48),

    /**
     * Placed status (code: 50).
     * Indicates an order has been placed.
     */
    PLACED(50),

    /**
     * Shipped status (code: 52).
     * Indicates goods have been dispatched.
     */
    SHIPPED(52),

    /**
     * Delivered status (code: 54).
     * Indicates successful delivery.
     */
    DELIVERED(54),

    /**
     * Accepted status (code: 56).
     * Indicates formal acceptance by recipient.
     */
    ACCEPTED(56),

    /**
     * Returned status (code: 58).
     * Indicates items have been sent back.
     */
    RETURNED(58),

    // development.

    /**
     * Developing status (code: 60).
     * Indicates active development phase.
     */
    DEVELOPING(60),

    /**
     * Testing status (code: 62).
     * Indicates testing/QA phase.
     */
    TESTING(62),

    /**
     * Released status (code: 64).
     * Indicates software/product has been released.
     */
    RELEASED(64),

    /**
     * Deployed status (code: 66).
     * Indicates deployment to production environment.
     */
    DEPLOYED(66),

    // privilege.

    /**
     * Read-only status (code: 70).
     * Indicates only read access is permitted.
     */
    READ_ONLY(70),

    /**
     * Updatable status (code: 72).
     * Indicates update operations are permitted.
     */
    UPDATABLE(72),

    /**
     * Insertable status (code: 74).
     * Indicates insert operations are permitted.
     */
    INSERTABLE(74),

    /**
     * Writable status (code: 76).
     * Indicates full write access is permitted.
     */
    WRITABLE(76),

    /**
     * Frozen status (code: 80).
     * Indicates no changes are allowed.
     */
    FROZEN(80),

    /**
     * Finalized status (code: 82).
     * Indicates final state has been reached.
     */
    FINALIZED(82),

    /**
     * Locked status (code: 84).
     * Indicates access is restricted.
     */
    LOCKED(84),

    /**
     * Unlocked status (code: 86).
     * Indicates access restrictions have been removed.
     */
    UNLOCKED(86),

    /**
     * Available status (code: 88).
     * Indicates resource is available for use.
     */
    AVAILABLE(88),

    /**
     * Unavailable status (code: 90).
     * Indicates resource is not available for use.
     */
    UNAVAILABLE(90),

    /**
     * Disabled status (code: 92).
     * Indicates functionality has been disabled.
     */
    DISABLED(92),

    /**
     * Destroyed status (code: 94).
     * Indicates permanent destruction.
     */
    DESTROYED(94);

    private final int code;

    Status(int code) {
        this.code = code;
    }

    /**
     * Returns the numeric code associated with this status.
     * Codes are designed to be efficiently stored in databases and
     * provide fast lookups.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Status status = Status.ACTIVE;
     * int code = status.getCode(); // Returns 2
     * }</pre>
     *
     * @return the numeric code for this status
     */
    public int getCode() {
        return code;
    }

    private static final int MAX_CODE = 128;
    private static final Status[] cache = new Status[MAX_CODE];

    static {
        for (Status status : Status.values()) {
            cache[status.code] = status;
        }
    }

    /**
     * Returns the Status enum constant associated with the specified code.
     * This method provides O(1) lookup performance for valid codes.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Status status = Status.fromCode(42); // Returns PROCESSING
     * Status invalid = Status.fromCode(999); // Returns null
     * }</pre>
     *
     * @param code the numeric code to look up
     * @return the Status associated with the code, or {@code null} if no Status exists for that code
     */
    public static Status fromCode(final int code) {
        return code < 0 || code >= MAX_CODE ? null : cache[code];
    }
}
