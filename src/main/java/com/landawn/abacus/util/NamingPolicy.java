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
 * The NamingPolicy enum represents different naming conventions.
 * It provides a set of predefined naming policies such as LOWER_CAMEL_CASE, LOWER_CASE_WITH_UNDERSCORE, UPPER_CASE_WITH_UNDERSCORE, and NO_CHANGE.
 * Each policy is associated with a function that converts a string according to the policy.
 *
 */
public enum NamingPolicy {

    LOWER_CAMEL_CASE(Strings::toCamelCase),

    LOWER_CASE_WITH_UNDERSCORE(Strings::toLowerCaseWithUnderscore),

    UPPER_CASE_WITH_UNDERSCORE(Strings::toUpperCaseWithUnderscore),

    @Beta
    NO_CHANGE(str -> str);

    private final Function<String, String> converter;

    NamingPolicy(final Function<String, String> converter) {
        this.converter = converter;
    }

    /**
     *
     * @param str
     * @return
     */
    public String convert(final String str) {
        return converter.apply(str);
    }

    /**
     * Returns the function that converts a string according to the policy.
     *
     * @return
     */
    @Beta
    public Function<String, String> func() {
        return converter;
    }
}
