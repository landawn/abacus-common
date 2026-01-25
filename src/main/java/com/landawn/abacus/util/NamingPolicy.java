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

import java.util.function.Function;

import com.landawn.abacus.annotation.Beta;

/**
 * An enumeration representing different naming conventions for string transformation.
 * 
 * <p>This enum provides a set of predefined naming policies that can be used to convert
 * strings between different naming conventions commonly used in programming. Each policy
 * encapsulates a transformation function that converts a string according to its rules.</p>
 * 
 * <p>The available naming policies are:</p>
 * <ul>
 *   <li>{@link #CAMEL_CASE} - Converts to camelCase (e.g., "myVariableName")</li>
 *   <li>{@link #SNAKE_CASE} - Converts to lower_case_with_underscore (e.g., "my_variable_name")</li>
 *   <li>{@link #SCREAMING_SNAKE_CASE} - Converts to SCREAMING_SNAKE_CASE (e.g., "MY_VARIABLE_NAME")</li>
 *   <li>{@link #NO_CHANGE} - Leaves the string unchanged</li>
 * </ul>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * String original = "user-name";
 * String camelCase = NamingPolicy.CAMEL_CASE.convert(original);   // "userName"
 * String snakeCase = NamingPolicy.SNAKE_CASE.convert(original);   // "user_name"
 * }</pre>
 * 
 * @see Strings#toCamelCase(String)
 * @see Strings#toSnakeCase(String)
 * @see Strings#toScreamingSnakeCase(String)
 */
public enum NamingPolicy {

    /**
     * Lower camel case naming policy (e.g., "myVariableName").
     *
     * <p>This policy converts strings to camelCase format where the first word
     * starts with a lowercase letter and subsequent words start with uppercase letters.
     * Words are joined without any separators.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String result1 = NamingPolicy.CAMEL_CASE.convert("user_name");     // "userName"
     * String result2 = NamingPolicy.CAMEL_CASE.convert("first-name");    // "firstName"
     * String result3 = NamingPolicy.CAMEL_CASE.convert("MY_CONSTANT");   // "myConstant"
     * }</pre>
     *
     * @see #convert(String)
     * @see Strings#toCamelCase(String)
     */
    CAMEL_CASE(Strings::toCamelCase),

    //    In Java, UpperCamelCase is the better choice — both technically and culturally.
    //
    //    Short answer
    //
    //    Prefer UpperCamelCase in Java documentation, APIs, and naming utilities.
    //
    //    UpperCamelCase is understood, but it is non-idiomatic in Java.
    //
    //    Why UpperCamelCase is better in Java
    //    1️⃣ It matches Java’s official terminology
    //
    //    The Java Language Specification, JDK docs, and common Java libraries consistently use:
    //
    //    camel case
    //
    //    lower camel case
    //
    //    upper camel case
    //
    //    Examples:
    //
    //    JLS: “class names are written in UpperCamelCase”
    //
    //    Guava, Apache Commons, Spring → all use UpperCamelCase terminology
    //
    //    You’ll rarely see UpperCamelCase in authoritative Java docs.
    //
    //    2️⃣ UpperCamelCase is language-agnostic, not Java-centric
    //
    //    UpperCamelCase originated in:
    //
    //    Pascal
    //
    //    C#
    //
    //    .NET ecosystem
    //
    //    In C# docs:
    //
    //    UpperCamelCase → types, methods
    //
    //    camelCase → locals, parameters
    //
    //    Java never adopted this terminology.
    //
    //    3️⃣ Java naming rules map cleanly
    //    Java element    Case
    //    Class / Interface / Enum    UpperCamelCase
    //    Method  camelCase
    //    Field (non-constant)    camelCase
    //    Constant    SCREAMING_SNAKE_CASE
    //    Package lowercase
    //
    //    This symmetry (UpperCamelCase ↔ camelCase) is conceptually clean.

    /**
     * Upper camel case naming policy (e.g., "MyVariableName").
     *
     * <p>This policy converts strings to UpperCamelCase format where each word starts
     * with an uppercase letter and words are joined without any separators. This is
     * commonly used in Java class names.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String result1 = NamingPolicy.UPPER_CAMEL_CASE.convert("user_name");     // "UserName"
     * String result2 = NamingPolicy.UPPER_CAMEL_CASE.convert("first-name");    // "FirstName"
     * String result3 = NamingPolicy.UPPER_CAMEL_CASE.convert("my_constant");   // "MyConstant"
     * }</pre>
     *
     * @see #convert(String)
     * @see Strings#toUpperCamelCase(String)
     */
    UPPER_CAMEL_CASE(Strings::toUpperCamelCase),

    /**
     * Lower case with underscores naming policy (e.g., "my_variable_name").
     *
     * <p>This policy converts strings to snake_case format where all letters are
     * lowercase and words are separated by underscores. This is commonly used in
     * Python, Ruby, and database column names.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String result1 = NamingPolicy.SNAKE_CASE.convert("userName");     // "user_name"
     * String result2 = NamingPolicy.SNAKE_CASE.convert("FirstName");    // "first_name"
     * String result3 = NamingPolicy.SNAKE_CASE.convert("myConstant");   // "my_constant"
     * }</pre>
     *
     * @see #convert(String)
     * @see Strings#toSnakeCase(String)
     */
    SNAKE_CASE(Strings::toSnakeCase),

    /**
     * Upper case with underscores naming policy (e.g., "MY_VARIABLE_NAME").
     *
     * <p>This policy converts strings to UPPER_SNAKE_CASE format where all letters
     * are uppercase and words are separated by underscores. This is commonly used
     * for constants and configuration keys.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String result1 = NamingPolicy.SCREAMING_SNAKE_CASE.convert("userName");     // "USER_NAME"
     * String result2 = NamingPolicy.SCREAMING_SNAKE_CASE.convert("firstName");    // "FIRST_NAME"
     * String result3 = NamingPolicy.SCREAMING_SNAKE_CASE.convert("myConstant");   // "MY_CONSTANT"
     * }</pre>
     *
     * @see #convert(String)
     * @see Strings#toScreamingSnakeCase(String)
     */
    SCREAMING_SNAKE_CASE(Strings::toScreamingSnakeCase),

    /**
     * Lower case with hyphens naming policy (e.g., "my-variable-name").
     *
     * <p>This policy converts strings to kebab-case format where all letters are
     * lowercase and words are separated by hyphens.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String result1 = NamingPolicy.KEBAB_CASE.convert("userName");     // "user-name"
     * String result2 = NamingPolicy.KEBAB_CASE.convert("FirstName");    // "first-name"
     * String result3 = NamingPolicy.KEBAB_CASE.convert("myConstant");   // "my-constant"
     * }</pre>
     *
     * @see #convert(String)
     * @see Strings#toKebabCase(String)
     */
    KEBAB_CASE(Strings::toKebabCase),

    /**
     * No change naming policy - returns the string as-is.
     *
     * <p>This policy performs no transformation and returns the input string unchanged.
     * It can be useful when you need a policy object but don't want any transformation
     * to occur.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String result1 = NamingPolicy.NO_CHANGE.convert("any-String_123");   // "any-String_123"
     * String result2 = NamingPolicy.NO_CHANGE.convert("MixedCase");        // "MixedCase"
     * String result3 = NamingPolicy.NO_CHANGE.convert("UPPER_CASE");       // "UPPER_CASE"
     * }</pre>
     *
     * @see #convert(String)
     */
    @Beta
    NO_CHANGE(str -> str);

    private final Function<String, String> converter;

    NamingPolicy(final Function<String, String> converter) {
        this.converter = converter;
    }

    /**
     * Converts the specified string according to this naming policy's transformation rules.
     *
     * <p>This method applies the transformation function associated with this naming policy
     * to convert the input string to the desired format. The exact transformation depends on
     * which naming policy constant is used (e.g., CAMEL_CASE, SCREAMING_SNAKE_CASE, etc.).</p>
     *
     * <p>The method handles various input formats and intelligently detects word boundaries based on:
     * <ul>
     *   <li>Underscores (_)</li>
     *   <li>Hyphens (-)</li>
     *   <li>Spaces</li>
     *   <li>Case transitions (e.g., "camelCase" to "camel case")</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Convert to lower camel case
     * String result1 = NamingPolicy.CAMEL_CASE.convert("user-name");            // "userName"
     * String result2 = NamingPolicy.CAMEL_CASE.convert("USER_NAME");            // "userName"
     *
     * // Convert to snake case
     * String result3 = NamingPolicy.SNAKE_CASE.convert("userName");   // "user_name"
     *
     * // Null and empty string handling
     * String result4 = NamingPolicy.CAMEL_CASE.convert(null);                   // null
     * String result5 = NamingPolicy.CAMEL_CASE.convert("");                     // ""
     * }</pre>
     *
     * @param str the string to convert; may be {@code null}, empty, or contain various separators
     *            (underscores, hyphens, spaces) or be in camelCase/UpperCamelCase format
     * @return the converted string according to this naming policy's rules; returns {@code null} if
     *         the input is {@code null}, returns an empty string if the input is empty, otherwise
     *         returns the result of applying the policy's transformation function to the input string
     * @see #func()
     */
    public String convert(final String str) {
        return converter.apply(str);
    }

    /**
     * Returns the underlying function that performs the string conversion.
     *
     * <p>This method provides access to the raw conversion function, which can be
     * useful when you need to pass the converter to methods that accept functions
     * or when composing multiple transformations.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Function<String, String> converter = NamingPolicy.CAMEL_CASE.func();
     * List<String> names = Arrays.asList("user_name", "first_name");
     * List<String> camelCaseNames = names.stream()
     *     .map(converter)
     *     .collect(Collectors.toList());
     * }</pre>
     *
     * @return the function that performs the string transformation for this policy
     * @deprecated This method is deprecated. Use {@link #convert(String)} directly or obtain
     *             the converter through other means. This API is experimental and may be removed
     *             in a future release.
     */
    @Deprecated
    @Beta
    public Function<String, String> func() {
        return converter;
    }
}
