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
 * The Enum Status.
 *
 */
public enum Status {

    /** General account/service/object status. */
    BLANK(0),
    ACTIVE(2),
    SUSPENDED(4),
    EXPIRED(6),
    CANCELED(8),
    CLOSED(10),
    REMOVED(12),
    DELETED(14),
    CONCLUDED(16),
    REVOKED(18),
    REFUNDED(20),
    COMPLETED(22),
    TERMINATED(24),
    RETIRED(26),

    /** Running status. */
    STARTED(30),
    RUNNING(32),
    PAUSED(34),
    STOPPED(36),

    /** order/process status. */
    PREPARING(40),
    PROCESSING(42),
    PROCESSED(44),
    PENDING(46),
    APPROVED(48),
    PLACED(50),
    SHIPPED(52),
    DELIVERED(54),
    ACCEPTED(56),
    RETURNED(58),

    /** development. */
    DEVELOPING(60),
    TESTING(62),
    RELEASED(64),
    DEPLOYED(66),

    /** privilege. */
    READ_ONLY(70),
    UPDATABLE(72),
    INSERTABLE(74),
    WRITABLE(76),

    FROZEN(80),
    FINALIZED(82),
    LOCKED(84),
    UNLOCKED(86),
    AVAILABLE(88),
    UNAVAILABLE(90),
    DISABLED(92),
    DESTROYED(94);

    private final int code;

    Status(int code) {
        this.code = code;
    }

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

    public static Status fromCode(final int code) {
        return code < 0 || code >= MAX_CODE ? null : cache[code];
    }
}
