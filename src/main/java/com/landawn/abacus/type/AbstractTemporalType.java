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

package com.landawn.abacus.type;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;

import com.landawn.abacus.util.Dates;

public abstract class AbstractTemporalType<T extends Temporal> extends AbstractType<T> {

    protected static final ZoneId DEFAULT_ZONE_ID = Dates.DEFAULT_ZONE_ID;

    protected static final DateTimeFormatter iso8601DateTimeDTF = DateTimeFormatter.ofPattern(Dates.ISO_8601_DATE_TIME_FORMAT).withZone(DEFAULT_ZONE_ID);

    protected static final DateTimeFormatter iso8601TimestampDTF = DateTimeFormatter.ofPattern(Dates.ISO_8601_TIMESTAMP_FORMAT).withZone(DEFAULT_ZONE_ID);

    protected AbstractTemporalType(final String typeName) {
        super(typeName);
    }

    /**
     * Checks if is non quoted csv type.
     *
     * @return {@code true}, if is non quoted csv type
     */
    @Override
    public boolean isNonQuotableCsvType() {
        return true;
    }

}
