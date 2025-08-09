/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Date;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.AbstractTest;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalBoolean;
import com.landawn.abacus.util.u.OptionalByte;
import com.landawn.abacus.util.u.OptionalChar;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.u.OptionalFloat;
import com.landawn.abacus.util.u.OptionalInt;
import com.landawn.abacus.util.u.OptionalLong;
import com.landawn.abacus.util.u.OptionalShort;

public class OptionalTest extends AbstractTest {

    @Test
    public void test_cache() {

        for (int i = Byte.MIN_VALUE; i <= Byte.MAX_VALUE; i++) {
            assertTrue(OptionalByte.of((byte) i).get() == i);
        }

        for (int i = -100000; i <= 100000; i++) {
            assertTrue(OptionalInt.of(i).get() == i);
        }

    }

    @Test
    public void test_01() {
        {
            Type<OptionalBoolean> type = N.typeOf(OptionalBoolean.class);
            assertEquals(OptionalBoolean.of(true), type.valueOf(type.stringOf(OptionalBoolean.of(true))));
        }
        {
            Type<OptionalChar> type = N.typeOf(OptionalChar.class);
            assertEquals(OptionalChar.of('a'), type.valueOf(type.stringOf(OptionalChar.of('a'))));
        }
        {
            Type<OptionalByte> type = N.typeOf(OptionalByte.class);
            assertEquals(OptionalByte.of((byte) 1), type.valueOf(type.stringOf(OptionalByte.of((byte) 1))));
        }
        {
            Type<OptionalShort> type = N.typeOf(OptionalShort.class);
            assertEquals(OptionalShort.of((short) 1), type.valueOf(type.stringOf(OptionalShort.of((short) 1))));
        }
        {
            Type<OptionalInt> type = N.typeOf(OptionalInt.class);
            assertEquals(OptionalInt.of(1), type.valueOf(type.stringOf(OptionalInt.of(1))));
        }
        {
            Type<OptionalLong> type = N.typeOf(OptionalLong.class);
            assertEquals(OptionalLong.of(1), type.valueOf(type.stringOf(OptionalLong.of(1))));
        }

        {
            Type<OptionalFloat> type = N.typeOf(OptionalFloat.class);
            assertEquals(OptionalFloat.of(1.1f), type.valueOf(type.stringOf(OptionalFloat.of(1.1f))));
        }

        {
            Type<OptionalDouble> type = N.typeOf(OptionalDouble.class);
            assertEquals(OptionalDouble.of(1.1), type.valueOf(type.stringOf(OptionalDouble.of(1.1))));
        }

        {
            Type<Optional<Float>> type = N.typeOf("Optional<Float>");
            assertEquals(Optional.of(1.1f), type.valueOf(type.stringOf(Optional.of(1.1f))));
        }

        {
            Type<Optional<Date>> type = N.typeOf("Optional<Date>");
            assertEquals(Optional.of(Dates.parseDate("2016-02-02")), type.valueOf(type.stringOf(Optional.of(Dates.parseDate("2016-02-02")))));
        }
    }
}
