/*
 * Copyright (c) 2025, Haiyang Li.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.landawn.abacus.util;

import java.util.regex.Pattern;

/**
 *
 * @see <a href="https://regexr.com/">https://regexr.com/</a>
 */
public class Patterns {

    public static final Pattern JAVA_IDENTIFIER_PATTERN = Pattern.compile("^([a-zA-Z_$][a-zA-Z\\d_$]*)$");

    // https://www.baeldung.com/java-email-validation-regex
    // https://owasp.org/www-community/OWASP_Validation_Regex_Repository
    // https://stackoverflow.com/questions/201323/how-can-i-validate-an-email-address-using-a-regular-expression
    public static final Pattern EMAIL_ADDRESS_RFC_5322_PATTERN = Pattern.compile(
            "(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:(2(5[0-5]|[0-4][0-9])|1[0-9][0-9]|[1-9]?[0-9]))\\.){3}(?:(2(5[0-5]|[0-4][0-9])|1[0-9][0-9]|[1-9]?[0-9])|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])");

    // https://stackoverflow.com/questions/3809401/what-is-a-good-regular-expression-to-match-a-url
    public static final Pattern URL_PATTERN = Pattern.compile("[-a-zA-Z0-9@:%._\\+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()@:%_\\+.~#?&//=]*)");

    public static final Pattern HTTP_URL_PATTERN = Pattern.compile("[-a-zA-Z0-9@:%._\\+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()@:%_\\+.~#?&//=]*)");

    // https://stackoverflow.com/questions/1449817/what-are-some-of-the-most-useful-regular-expressions-for-programmers
    public static final Pattern INTEGER_PATTERN = Pattern.compile("^-?\\d+$");
    public static final Pattern POSITIVE_INTEGER_PATTERN = Pattern.compile("^\\d+$");
    public static final Pattern NEGATIVE_INTEGER_PATTERN = Pattern.compile("^-\\d+$");
    public static final Pattern POSITIVE_NUMBER_PATTERN = Pattern.compile("^\\d*\\.?\\d+$");
    public static final Pattern NEGATIVE_NUMBER_PATTERN = Pattern.compile("^-\\d*\\.?\\d+$");
    public static final Pattern NUMBER_PATTERN = Pattern.compile("^-?\\d*\\.?\\d+$");
    public static final Pattern PHONE_NUMBER_PATTERN = Pattern.compile("^\\+?[\\d\\s]{3,}$");
    public static final Pattern PHONE_NUMBER_WITH_CODE_PATTERN = Pattern.compile("^\\+?[\\d\\s]+\\(?[\\d\\s]{10,}$");
    public static final Pattern ALPHANUMERIC_WITH_SPACE_PATTERN = Pattern.compile("^\\+?[\\d\\s]+\\(?[\\d\\s]{10,}$");
    public static final Pattern ALPHANUMERIC_WITHOUT_SPACE_PATTERN = Pattern.compile("^\\+?[\\d\\s]+\\(?[\\d\\s]{10,}$");
}
