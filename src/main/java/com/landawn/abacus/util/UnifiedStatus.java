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
 * int statusCode = orderStatus.code();   // Returns 32
 * 
 * // Retrieving status from code
 * Status retrieved = Status.fromCode(32);   // Returns PROCESSING
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
public enum UnifiedStatus {

    // ====== General account/service/object status.

    /**
     * Blank/uninitialized status (code: 0).
     * Represents an unset or default state.
     */
    BLANK(0),

    /**
     * Active status (code: 1).
     * Indicates the entity is currently active and operational.
     */
    ACTIVE(1),

    /**
     * Suspended status (code: 2).
     * Indicates temporary de-activation that can be reversed.
     */
    SUSPENDED(2),

    /**
     * Expired status (code: 3).
     * Indicates the entity has passed its valid time period.
     */
    EXPIRED(3),

    /**
     * Canceled status (code: 4).
     * Indicates intentional termination before completion.
     */
    CANCELED(4),

    /**
     * Refunded status (code: 5).
     * Indicates money or resources have been returned.
     */
    REFUNDED(5),

    /**
     * Completed status (code: 6).
     * Indicates successful finishing of a task or process.
     */
    COMPLETED(6),

    /**
     * Closed status (code: 7).
     * Indicates normal termination or completion.
     */
    CLOSED(7),

    /**
     * Removed status (code: 8).
     * Indicates the entity has been removed from active use.
     */
    REMOVED(8),

    /**
     * Deleted status (code: 9).
     * Indicates permanent removal or deletion.
     */
    DELETED(9),

    // ====== Termination:

    /**
     * Concluded status (code: 11).
     * Indicates successful completion of a process or agreement.
     */
    CONCLUDED(11),

    /**
     * Revoked status (code: 12).
     * Indicates withdrawal of previously granted rights or status.
     */
    REVOKED(12),

    /**
     * Terminated status (code: 13).
     * Indicates forced ending of a process or agreement.
     */
    TERMINATED(13),

    /**
     * Retired status (code: 14).
     * Indicates planned phase-out or end of service.
     */
    RETIRED(14),

    /**
     * Destroyed status (code: 15).
     * Indicates permanent destruction.
     */
    DESTROYED(15),

    // ====== Running status.

    /**
     * Started status (code: 21).
     * Indicates a process has begun execution.
     */
    STARTED(21),

    /**
     * Running status (code: 22).
     * Indicates a process is currently executing.
     */
    RUNNING(22),

    /**
     * Paused status (code: 23).
     * Indicates temporary suspension of execution.
     */
    PAUSED(23),

    /**
     * Stopped status (code: 24).
     * Indicates execution has been halted.
     */
    STOPPED(24),

    //  ====== process status.

    /**
     * Preparing status (code: 31).
     * Indicates initial preparation phase.
     */
    PREPARING(31),

    /**
     * Processing status (code: 32).
     * Indicates active processing is occurring.
     */
    PROCESSING(32),

    /**
     * Processed status (code: 33).
     * Indicates processing has been completed.
     */
    PROCESSED(33),

    /**
     * Pending status (code: 34).
     * Indicates waiting for action or approval.
     */
    PENDING(34),

    /**
     * Approved status (code: 35).
     * Indicates formal approval has been granted.
     */
    APPROVED(35),

    /**
     * Rejected status (code: 36).
     * Indicates formal approval has been rejected.
     */
    REJECTED(36),

    // order
    /**
     * Placed status (code: 41).
     * Indicates an order has been placed.
     */
    PLACED(41),

    /**
     * Shipped status (code: 42).
     * Indicates goods have been dispatched.
     */
    SHIPPED(42),

    /**
     * Delivered status (code: 43).
     * Indicates successful delivery.
     */
    DELIVERED(43),

    /**
     * Accepted status (code: 44).
     * Indicates formal acceptance by recipient.
     */
    ACCEPTED(44),

    /**
     * Returned status (code: 45).
     * Indicates items have been sent back.
     */
    RETURNED(45),

    // development.

    /**
     * Developing status (code: 51).
     * Indicates active development phase.
     */
    DEVELOPING(51),

    /**
     * Testing status (code: 52).
     * Indicates testing/QA phase.
     */
    TESTING(52),

    /**
     * Released status (code: 53).
     * Indicates software/product has been released.
     */
    RELEASED(53),

    /**
     * Deployed status (code: 54).
     * Indicates deployment to production environment.
     */
    DEPLOYED(54),

    /**
     * Verified status (code: 55).
     * Indicates deployment has been verified.
     */
    VERIFIED(55),

    // privilege.

    /**
     * Read-only status (code: 61).
     * Indicates only read access is permitted.
     */
    READ_ONLY(61),

    /**
     * Updatable status (code: 62).
     * Indicates update operations are permitted.
     */
    UPDATABLE(62),

    /**
     * Insertable status (code: 63).
     * Indicates insert operations are permitted.
     */
    INSERTABLE(63),

    /**
     * Writable status (code: 64).
     * Indicates full write access is permitted.
     */
    WRITABLE(64),

    /**
     * Frozen status (code: 71).
     * Indicates no changes are allowed.
     */
    FROZEN(71),

    /**
     * Finalized status (code: 72).
     * Indicates final state has been reached.
     */
    FINALIZED(72),

    /**
     * Locked status (code: 73).
     * Indicates access is restricted.
     */
    LOCKED(73),

    /**
     * Unlocked status (code: 74).
     * Indicates access restrictions have been removed.
     */
    UNLOCKED(74),

    /**
     * Available status (code: 75).
     * Indicates resource is available for use.
     */
    AVAILABLE(75),

    /**
     * Unavailable status (code: 76).
     * Indicates resource is not available for use.
     */
    UNAVAILABLE(76),

    /**
     * Enabled status (code: 77).
     * Indicates functionality has been enabled.
     */
    ENABLED(77),

    /**
     * Disabled status (code: 78).
     * Indicates functionality has been disabled.
     */
    DISABLED(78);

    //  ====== code starting from 100 are reserved

    private final int code;

    UnifiedStatus(int code) {
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
     * int code = status.code();   // Returns 2
     * }</pre>
     *
     * @return the numeric code for this status
     */
    public int code() {
        return code;
    }

    private static final int MAX_CODE = 128;
    private static final UnifiedStatus[] cache = new UnifiedStatus[MAX_CODE];

    static {
        for (UnifiedStatus unifiedStatus : UnifiedStatus.values()) {
            cache[unifiedStatus.code] = unifiedStatus;
        }
    }

    /**
     * Returns the Status enum constant associated with the specified code.
     * This method provides O(1) lookup performance for valid codes.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Status status = Status.fromCode(1);     // Returns ACTIVE
     * Status invalid = Status.fromCode(999);   // Returns null
     * }</pre>
     *
     * @param code the numeric code to look up
     * @return the Status associated with the code, or {@code null} if no Status exists for that code
     */
    public static UnifiedStatus fromCode(final int code) {
        return code < 0 || code >= MAX_CODE ? null : cache[code];
    }
}
