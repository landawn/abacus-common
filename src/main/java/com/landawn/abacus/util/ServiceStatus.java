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
 * The Enum ServiceStatus.
 *
 * @author Haiyang Li
 * @since 0.8
 */
public enum ServiceStatus {

    DEFAULT(0), ACTIVE(1), SUSPENDED(2), EXPIRED(3), CONCLUDED(4), REVOKED(5), REFUNDED(6), CANCELLED(7);

    private int intValue;

    ServiceStatus(int intValue) {
        this.intValue = intValue;
    }

    public int intValue() {
        return intValue;
    }

    /**
     *
     * @param intValue
     * @return
     */
    public static ServiceStatus valueOf(int intValue) {
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
