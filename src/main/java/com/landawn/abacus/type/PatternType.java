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

import java.util.regex.Pattern;

import com.landawn.abacus.util.Strings;

/**
 * Type handler for java.util.regex.Pattern objects, providing conversion between Pattern instances
 * and their string representations. This type allows for seamless serialization and deserialization
 * of regular expression patterns.
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * Type<Pattern> patternType = TypeFactory.getType("Pattern");
 *
 * // Convert Pattern to string
 * Pattern emailPattern = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
 * String patternStr = patternType.stringOf(emailPattern); // Returns "^[A-Za-z0-9+_.-]+@(.+)$"
 *
 * // Convert string to Pattern
 * Pattern restored = patternType.valueOf("^[A-Za-z0-9+_.-]+@(.+)$");
 * boolean matches = restored.matcher("test@example.com").matches(); // true
 * }</pre>
 */
public class PatternType extends AbstractType<Pattern> {

    public static final String PATTERN = "Pattern";

    PatternType() {
        super(PATTERN);
    }

    /**
     * Returns the Class object representing the Pattern type.
     *
     * @return the Class object for java.util.regex.Pattern
     */
    @Override
    public Class<Pattern> clazz() {
        return Pattern.class;
    }

    /**
     * Converts a Pattern object to its string representation.
     * Returns the pattern string by calling toString() on the Pattern object.
     * Returns {@code null} if the input Pattern is {@code null}.
     *
     * @param t the Pattern object to convert
     * @return the pattern string, or {@code null} if the input is null
     */
    @Override
    public String stringOf(final Pattern t) {
        return (t == null) ? null : t.toString();
    }

    /**
     * Parses a string representation to create a Pattern instance.
     * Compiles the string into a Pattern using Pattern.compile().
     * Returns {@code null} if the input string is {@code null} or empty.
     *
     * @param str the regular expression string to compile
     * @return a compiled Pattern object, or {@code null} if the input is {@code null} or empty
     */
    @Override
    public Pattern valueOf(final String str) {
        return (Strings.isEmpty(str)) ? null : Pattern.compile(str);
    }
}
