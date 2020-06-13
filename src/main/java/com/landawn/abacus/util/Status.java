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
 * The Enum Status.
 *
 * @author Haiyang Li
 * @since 0.8
 */
public enum Status {

    /** General account/service status. */
    BLANK,
    /** The active. */
    ACTIVE,
    /** The suspended. */
    SUSPENDED,
    /** The expired. */
    EXPIRED,
    /** The retired. */
    RETIRED,
    /** The concluded. */
    CONCLUDED,
    /** The revoked. */
    REVOKED,
    /** The refunded. */
    REFUNDED,
    /** The cancelled. */
    CANCELLED,
    /** The closed. */
    CLOSED,
    /** The removed. */
    REMOVED,
    /** The deleted. */
    DELETED,

    /** Running status. */
    STARTED,
    /** The running. */
    RUNNING,
    /** The paused. */
    PAUSED,
    /** The stopped. */
    STOPPED,

    /** order/process status. */
    PREPARING,
    /** The pending. */
    PENDING,
    /** The processing. */
    PROCESSING,
    /** The processed. */
    PROCESSED,
    /** The approved. */
    APPROVED,
    /** The placed. */
    PLACED,
    /** The shipped. */
    SHIPPED,
    /** The delivered. */
    DELIVERED,
    /** The accepted. */
    ACCEPTED,
    /** The returned. */
    RETURNED,

    /** development. */
    DEVELOPING,
    /** The testing. */
    TESTING,
    /** The released. */
    RELEASED,
    /** The deployed. */
    DEPLOYED,

    /** privilege. */
    READ_ONLY,
    /** The updatable. */
    UPDATABLE,
    /** The insertable. */
    INSERTABLE,
    /** The writable. */
    WRITABLE,

    /** The frozen. */
    FROZEN,
    /** The finalized. */
    FINALIZED,
    /** The locked. */
    LOCKED,
    /** The unlocked. */
    UNLOCKED,
    /** The available. */
    AVAILABLE,
    /** The unavailable. */
    UNAVAILABLE,
    /** The disabled. */
    DISABLED,
    /** The destroyed. */
    DESTROYED;
}
