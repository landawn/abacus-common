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
 * An enumeration of naming conventions used to transform identifier strings.
 *
 * <p>Each constant other than {@link #NO_CHANGE} delegates to the matching {@link Strings}
 * converter. Those converters detect word boundaries at underscores, hyphens, whitespace, and
 * case transitions (an uppercase or titlecase letter that is preceded or followed by a lowercase
 * letter). {@code null} and {@code ""} are returned unchanged.</p>
 *
 * <p>The conversions are not inverses. Camel-case policies remove and collapse separators
 * (leading and trailing separators disappear), so {@code CAMEL_CASE.convert("a__b")} is
 * {@code "aB"}. Snake- and kebab-case policies collapse any adjacent internal run of {@code '_'},
 * {@code '-'}, and/or whitespace to a single output delimiter and drop leading/trailing runs
 * ({@code SNAKE_CASE.convert("a__b")} is {@code "a_b"};
 * {@code SNAKE_CASE.convert("_first__name_")} is {@code "first_name"}).</p>
 *
 * <p>The camel-case policies are also not idempotent: {@code UPPER_CAMEL_CASE.convert("a__b")} is
 * {@code "AB"}, but converting that result again yields {@code "Ab"} — the separator that created
 * the word boundary is gone, so {@code "AB"} reads as a single all-caps word. Apply a camel-case
 * policy to an original identifier once; do not re-apply it to its own output. {@link #SNAKE_CASE},
 * {@link #SCREAMING_SNAKE_CASE}, {@link #KEBAB_CASE} and {@link #NO_CHANGE} are idempotent.</p>
 *
 * <p>The available naming policies are:</p>
 * <ul>
 *   <li>{@link #CAMEL_CASE} — camelCase (e.g. {@code "myVariableName"})</li>
 *   <li>{@link #UPPER_CAMEL_CASE} — UpperCamelCase (e.g. {@code "MyVariableName"})</li>
 *   <li>{@link #SNAKE_CASE} — snake_case (e.g. {@code "my_variable_name"})</li>
 *   <li>{@link #SCREAMING_SNAKE_CASE} — SCREAMING_SNAKE_CASE (e.g. {@code "MY_VARIABLE_NAME"})</li>
 *   <li>{@link #KEBAB_CASE} — kebab-case (e.g. {@code "my-variable-name"})</li>
 *   <li>{@link #NO_CHANGE} — the input is returned as-is</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * NamingPolicy.CAMEL_CASE.convert("user-name");          // "userName"
 * NamingPolicy.SNAKE_CASE.convert("userName");           // "user_name"
 * NamingPolicy.SNAKE_CASE.convert(" first-name ");       // "first_name"
 * NamingPolicy.KEBAB_CASE.convert(" _first_name_ ");     // "first-name"
 * }</pre>
 *
 * @see Strings#toCamelCase(String)
 * @see Strings#toUpperCamelCase(String)
 * @see Strings#toSnakeCase(String)
 * @see Strings#toScreamingSnakeCase(String)
 * @see Strings#toKebabCase(String)
 */
public enum NamingPolicy {

    /**
     * Lower camel case (e.g. {@code "myVariableName"}).
     *
     * <p>Delegates to {@link Strings#toCamelCase(String)}. The first word is lowercased; each later
     * word is capitalized. Separators are removed and collapsed, so leading and trailing
     * {@code '_'}, {@code '-'}, and whitespace disappear, and {@code "a__b"} becomes {@code "aB"}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * NamingPolicy.CAMEL_CASE.convert("user_name");     // "userName"
     * NamingPolicy.CAMEL_CASE.convert("first-name");    // "firstName"
     * NamingPolicy.CAMEL_CASE.convert("MY_CONSTANT");   // "myConstant"
     * NamingPolicy.CAMEL_CASE.convert("XMLParser");     // "xmlParser"
     * NamingPolicy.CAMEL_CASE.convert("_helloWorld");   // "helloWorld"
     * }</pre>
     *
     * @see #convert(String)
     * @see Strings#toCamelCase(String)
     */
    CAMEL_CASE(Strings::toCamelCase),

    /**
     * Upper camel case / PascalCase (e.g. {@code "MyVariableName"}).
     *
     * <p>Delegates to {@link Strings#toUpperCamelCase(String)}. Same word-boundary and separator
     * rules as {@link #CAMEL_CASE}, except every word — including the first — is capitalized.
     * {@code "XMLParser"} becomes {@code "XmlParser"}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * NamingPolicy.UPPER_CAMEL_CASE.convert("user_name");     // "UserName"
     * NamingPolicy.UPPER_CAMEL_CASE.convert("first-name");    // "FirstName"
     * NamingPolicy.UPPER_CAMEL_CASE.convert("XMLParser");     // "XmlParser"
     * }</pre>
     *
     * @see #convert(String)
     * @see Strings#toUpperCamelCase(String)
     */
    UPPER_CAMEL_CASE(Strings::toUpperCamelCase),

    /**
     * Lower case with underscores (e.g. {@code "my_variable_name"}).
     *
     * <p>Delegates to {@link Strings#toSnakeCase(String)}. Case boundaries become {@code '_'}.
     * Any adjacent internal run of {@code '_'}, {@code '-'}, and/or whitespace collapses to a single
     * underscore; leading and trailing runs are dropped: {@code "a__b"} and {@code "a-_b"} become
     * {@code "a_b"}; {@code "_first__name_"} becomes {@code "first_name"}; {@code "-a-"} becomes
     * {@code "a"}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * NamingPolicy.SNAKE_CASE.convert("userName");      // "user_name"
     * NamingPolicy.SNAKE_CASE.convert("first-name");    // "first_name"
     * NamingPolicy.SNAKE_CASE.convert("first name");    // "first_name"
     * NamingPolicy.SNAKE_CASE.convert("a__b");          // "a_b"
     * NamingPolicy.SNAKE_CASE.convert(" -hello- ");     // "hello"
     * }</pre>
     *
     * @see #convert(String)
     * @see Strings#toSnakeCase(String)
     */
    SNAKE_CASE(Strings::toSnakeCase),

    /**
     * Upper case with underscores (e.g. {@code "MY_VARIABLE_NAME"}).
     *
     * <p>Delegates to {@link Strings#toScreamingSnakeCase(String)}. Same delimiter rules as
     * {@link #SNAKE_CASE}: any adjacent internal run of {@code '_'}, {@code '-'}, and/or whitespace
     * collapses to a single underscore; leading and trailing runs are dropped.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * NamingPolicy.SCREAMING_SNAKE_CASE.convert("userName");      // "USER_NAME"
     * NamingPolicy.SCREAMING_SNAKE_CASE.convert("first-name");    // "FIRST_NAME"
     * NamingPolicy.SCREAMING_SNAKE_CASE.convert(" -hello- ");     // "HELLO"
     * }</pre>
     *
     * @see #convert(String)
     * @see Strings#toScreamingSnakeCase(String)
     */
    SCREAMING_SNAKE_CASE(Strings::toScreamingSnakeCase),

    /**
     * Lower case with hyphens (e.g. {@code "my-variable-name"}).
     *
     * <p>Delegates to {@link Strings#toKebabCase(String)}. Case boundaries become {@code '-'}.
     * Any adjacent internal run of {@code '_'}, {@code '-'}, and/or whitespace collapses to a single
     * hyphen; leading and trailing runs are dropped: {@code " _a_ "} and {@code "-a-"} become
     * {@code "a"}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * NamingPolicy.KEBAB_CASE.convert("userName");      // "user-name"
     * NamingPolicy.KEBAB_CASE.convert("first_name");    // "first-name"
     * NamingPolicy.KEBAB_CASE.convert("first name");    // "first-name"
     * NamingPolicy.KEBAB_CASE.convert(" _hello_ ");     // "hello"
     * }</pre>
     *
     * @see #convert(String)
     * @see Strings#toKebabCase(String)
     */
    KEBAB_CASE(Strings::toKebabCase),

    /**
     * Identity policy: the input is returned unchanged, including {@code null}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * NamingPolicy.NO_CHANGE.convert("any-String_123");   // "any-String_123"
     * NamingPolicy.NO_CHANGE.convert("MixedCase");        // "MixedCase"
     * NamingPolicy.NO_CHANGE.convert(null);               // null
     * }</pre>
     *
     * @see #convert(String)
     */
    @Beta
    NO_CHANGE(str -> str);

    private final Function<String, String> converter;

    /**
     * Constructs a {@code NamingPolicy} with the given conversion function.
     *
     * @param converter the function that transforms an input string according to this policy's rules
     */
    NamingPolicy(final Function<String, String> converter) {
        this.converter = converter;
    }

    /**
     * Converts {@code str} according to this policy. Delegates to the corresponding {@link Strings}
     * method except for {@link #NO_CHANGE}, which returns the input unchanged.
     *
     * <p>Every converting policy splits on {@code '_'}, {@code '-'}, whitespace, and case transitions
     * (an uppercase or titlecase letter that is preceded or followed by a lowercase letter). The
     * remainder of each word is then cased for the target style. {@code CAMEL_CASE.convert("helloWorldAPI")}
     * is {@code "helloWorldApi"}; {@code UPPER_CAMEL_CASE.convert("XMLParser")} is {@code "XmlParser"}.</p>
     *
     * <p>{@link #SNAKE_CASE} and {@link #SCREAMING_SNAKE_CASE} insert {@code '_'} at case boundaries
     * and collapse any adjacent internal run of {@code '_'}, {@code '-'}, and/or whitespace to a
     * single underscore; leading and trailing runs are dropped.
     * {@link #KEBAB_CASE} inserts {@code '-'} at case boundaries and collapses the same separator
     * set to a single hyphen.</p>
     *
     * <p>These conversions are not inverses. Camel-case policies remove and collapse separators, so
     * {@code CAMEL_CASE.convert("_helloWorld")} is {@code "helloWorld"} and
     * {@code CAMEL_CASE.convert("a__b")} is {@code "aB"}, while
     * {@code SNAKE_CASE.convert("a__b")} is {@code "a_b"}.</p>
     *
     * <p>The camel-case policies are also not idempotent: {@code UPPER_CAMEL_CASE.convert("a__b")} is
     * {@code "AB"}, but converting that result again yields {@code "Ab"} — the separator that
     * created the word boundary is gone, so {@code "AB"} reads as a single all-caps word. Apply a
     * camel-case policy to an original identifier once; do not re-apply it to its own output.
     * {@link #SNAKE_CASE}, {@link #SCREAMING_SNAKE_CASE}, {@link #KEBAB_CASE} and {@link #NO_CHANGE}
     * are idempotent.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * NamingPolicy.CAMEL_CASE.convert("user-name");    // "userName"
     * NamingPolicy.CAMEL_CASE.convert("USER_NAME");    // "userName"
     * NamingPolicy.SNAKE_CASE.convert("userName");     // "user_name"
     * NamingPolicy.SNAKE_CASE.convert(" first-name "); // "first_name"
     * NamingPolicy.CAMEL_CASE.convert(null);           // null
     * NamingPolicy.CAMEL_CASE.convert("");             // ""
     * }</pre>
     *
     * @param str the string to convert; may be {@code null} or empty
     * @return the converted string; {@code null} if {@code str} is {@code null}, {@code ""} if
     *         {@code str} is empty
     * @see #asFunction()
     * @see Strings#toCamelCase(String)
     * @see Strings#toSnakeCase(String)
     * @see Strings#toKebabCase(String)
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
     * Function<String, String> converter = NamingPolicy.CAMEL_CASE.asFunction();
     * List<String> names = Arrays.asList("user_name", "first_name");
     * List<String> camelCaseNames = names.stream()
     *     .map(converter)
     *     .collect(Collectors.toList());
     * }</pre>
     *
     * @return the function that performs the string transformation for this policy
     * @deprecated Use {@link #convert(String)} directly, or the method reference {@code policy::convert}
     *             where a {@link Function} is required (e.g. in {@code stream().map(...)}). This method
     *             may be removed in a future release.
     */
    @Deprecated
    @Beta
    public Function<String, String> asFunction() {
        return converter;
    }
}
