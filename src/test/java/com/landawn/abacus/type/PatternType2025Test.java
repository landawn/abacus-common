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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.regex.Pattern;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class PatternType2025Test extends TestBase {

    private final PatternType type = new PatternType();

    @Test
    public void test_clazz() {
        assertEquals(Pattern.class, type.clazz());
    }

    @Test
    public void test_name() {
        assertEquals("Pattern", type.name());
    }

    @Test
    public void test_stringOf() {
        Pattern pattern = Pattern.compile("[a-z]+");
        assertEquals("[a-z]+", type.stringOf(pattern));

        Pattern complexPattern = Pattern.compile("\\d{3}-\\d{2}-\\d{4}");
        assertEquals("\\d{3}-\\d{2}-\\d{4}", type.stringOf(complexPattern));

        assertNull(type.stringOf(null));
    }

    @Test
    public void test_valueOf_String() {
        Pattern result = type.valueOf("[a-z]+");
        assertNotNull(result);
        assertEquals("[a-z]+", result.pattern());

        Pattern digitPattern = type.valueOf("\\d+");
        assertNotNull(digitPattern);
        assertTrue(digitPattern.matcher("123").matches());
        assertFalse(digitPattern.matcher("abc").matches());

        assertNull(type.valueOf((String) null));
        assertNull(type.valueOf(""));
    }

    @Test
    public void test_valueOf_String_ComplexPatterns() {
        // Email pattern
        Pattern emailPattern = type.valueOf("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
        assertNotNull(emailPattern);

        // Phone number pattern
        Pattern phonePattern = type.valueOf("\\d{3}-\\d{3}-\\d{4}");
        assertNotNull(phonePattern);
        assertTrue(phonePattern.matcher("123-456-7890").matches());

        // URL pattern
        Pattern urlPattern = type.valueOf("https?://[a-zA-Z0-9.-]+");
        assertNotNull(urlPattern);
        assertTrue(urlPattern.matcher("https://example.com").matches());
    }

    @Test
    public void test_valueOf_String_SpecialCharacters() {
        Pattern dotPattern = type.valueOf("\\.");
        assertNotNull(dotPattern);
        assertTrue(dotPattern.matcher(".").matches());

        Pattern wordBoundary = type.valueOf("\\bword\\b");
        assertNotNull(wordBoundary);
        assertTrue(wordBoundary.matcher("word").matches());
    }

    @Test
    public void test_roundTrip() {
        Pattern original = Pattern.compile("[A-Z][a-z]+");
        String stringForm = type.stringOf(original);
        Pattern recovered = type.valueOf(stringForm);

        assertNotNull(recovered);
        assertEquals(original.pattern(), recovered.pattern());
    }

    @Test
    public void test_defaultValue() {
        assertNull(type.defaultValue());
    }
}
