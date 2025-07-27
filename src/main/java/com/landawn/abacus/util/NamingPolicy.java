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
 *   <li>{@link #LOWER_CAMEL_CASE} - Converts to lowerCamelCase (e.g., "myVariableName")</li>
 *   <li>{@link #LOWER_CASE_WITH_UNDERSCORE} - Converts to lower_case_with_underscore (e.g., "my_variable_name")</li>
 *   <li>{@link #UPPER_CASE_WITH_UNDERSCORE} - Converts to UPPER_CASE_WITH_UNDERSCORE (e.g., "MY_VARIABLE_NAME")</li>
 *   <li>{@link #NO_CHANGE} - Leaves the string unchanged</li>
 * </ul>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * String original = "user-name";
 * String camelCase = NamingPolicy.LOWER_CAMEL_CASE.convert(original); // "userName"
 * String snakeCase = NamingPolicy.LOWER_CASE_WITH_UNDERSCORE.convert(original); // "user_name"
 * }</pre>
 * 
 * @see Strings#toCamelCase(String)
 * @see Strings#toLowerCaseWithUnderscore(String)
 * @see Strings#toUpperCaseWithUnderscore(String)
 */
public enum NamingPolicy {

    /**
     * Lower camel case naming policy (e.g., "myVariableName").
     * 
     * <p>This policy converts strings to lowerCamelCase format where the first word
     * starts with a lowercase letter and subsequent words start with uppercase letters.
     * Words are joined without any separators.</p>
     * 
     * <p>Example: "user_name" → "userName", "first-name" → "firstName"</p>
     */
    LOWER_CAMEL_CASE(Strings::toCamelCase),

    /**
     * Upper camel case naming policy (e.g., "MyVariableName").
     * 
     * <p>This policy converts strings to UpperCamelCase format where each word starts
     * with an uppercase letter and words are joined without any separators. This is
     * commonly used in Java class names.</p>
     * 
     * <p>Example: "user_name" → "UserName", "first-name" → "FirstName"</p>
     */
    UPPER_CAMEL_CASE(Strings::toPascalCase),

    /**
     * Lower case with underscore naming policy (e.g., "my_variable_name").
     * 
     * <p>This policy converts strings to snake_case format where all letters are
     * lowercase and words are separated by underscores. This is commonly used in
     * Python, Ruby, and database column names.</p>
     * 
     * <p>Example: "userName" → "user_name", "FirstName" → "first_name"</p>
     */
    LOWER_CASE_WITH_UNDERSCORE(Strings::toLowerCaseWithUnderscore),

    /**
     * Upper case with underscore naming policy (e.g., "MY_VARIABLE_NAME").
     * 
     * <p>This policy converts strings to UPPER_SNAKE_CASE format where all letters
     * are uppercase and words are separated by underscores. This is commonly used
     * for constants and configuration keys.</p>
     * 
     * <p>Example: "userName" → "USER_NAME", "firstName" → "FIRST_NAME"</p>
     */
    UPPER_CASE_WITH_UNDERSCORE(Strings::toUpperCaseWithUnderscore),

    /**
     * No change naming policy - returns the string as-is.
     * 
     * <p>This policy performs no transformation and returns the input string unchanged.
     * It can be useful when you need a policy object but don't want any transformation
     * to occur.</p>
     * 
     * <p>Example: "any-String_123" → "any-String_123"</p>
     */
    @Beta
    NO_CHANGE(str -> str);

    private final Function<String, String> converter;

    /**
     * Constructs a NamingPolicy with the specified converter function.
     * 
     * @param converter the function that performs the string transformation
     */
    NamingPolicy(final Function<String, String> converter) {
        this.converter = converter;
    }

    /**
     * Converts the specified string according to this naming policy.
     * 
     * <p>This method applies the transformation function associated with this
     * naming policy to convert the input string to the desired format.</p>
     * 
     * <p>Example:</p>
     * <pre>{@code
     * String original = "user-name";
     * String converted = NamingPolicy.LOWER_CAMEL_CASE.convert(original); // "userName"
     * }</pre>
     * 
     * @param str the string to convert
     * @return the converted string according to this naming policy
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
     * <p>Example:</p>
     * <pre>{@code
     * Function<String, String> converter = NamingPolicy.LOWER_CAMEL_CASE.func();
     * List<String> names = Arrays.asList("user_name", "first_name");
     * List<String> camelCaseNames = names.stream()
     *     .map(converter)
     *     .collect(Collectors.toList());
     * }</pre>
     * 
     * @return the function that performs the string transformation for this policy
     * @deprecated This method is marked as Beta and may be subject to change
     */
    @Beta
    public Function<String, String> func() {
        return converter;
    }
}