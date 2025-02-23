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
 * The Enum MediaType.
 *
 */
public enum MediaType {

    BINARY(0), AUDIO(1), VIDEO(2), IMAGE(3), TEXT(4), RECORD(5);

    private final int intValue;

    MediaType(final int intValue) {
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
    public static MediaType valueOf(final int intValue) {
        switch (intValue) {
            case 0:
                return BINARY;

            case 1:
                return AUDIO;

            case 2:
                return VIDEO;

            case 3:
                return IMAGE;

            case 4:
                return TEXT;

            case 5:
                return RECORD;

            default:
                throw new IllegalArgumentException("No mapping instance found by int value: " + intValue);
        }
    }
}
