/*
 * Copyright (C) 2025 HaiYang Li
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class UUIDType2025Test extends TestBase {

    private final UUIDType type = new UUIDType();

    @Test
    public void test_clazz() {
        assertEquals(UUID.class, type.clazz());
    }

    @Test
    public void test_name() {
        assertEquals("UUID", type.name());
    }

    @Test
    public void test_stringOf() {
        UUID uuid = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        assertEquals("550e8400-e29b-41d4-a716-446655440000", type.stringOf(uuid));
        assertNull(type.stringOf(null));
    }

    @Test
    public void test_valueOf_String() {
        UUID expected = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        UUID result = type.valueOf("123e4567-e89b-12d3-a456-426614174000");
        assertEquals(expected, result);

        assertNull(type.valueOf((String) null));
        assertNull(type.valueOf(""));
    }

    @Test
    public void test_valueOf_String_InvalidFormat() {
        assertThrows(IllegalArgumentException.class, () -> {
            type.valueOf("invalid-uuid-format");
        });
    }

    @Test
    public void test_valueOf_String_RandomUUID() {
        UUID uuid = UUID.randomUUID();
        String uuidStr = uuid.toString();
        UUID result = type.valueOf(uuidStr);
        assertEquals(uuid, result);
    }

    @Test
    public void test_defaultValue() {
        assertNull(type.defaultValue());
    }
}
