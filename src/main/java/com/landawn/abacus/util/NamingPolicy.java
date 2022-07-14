/*
 * Copyright (C) 2015 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.landawn.abacus.util;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.util.function.Function;

/**
 * The Enum NamingPolicy.
 *
 * @author Haiyang Li
 * @since 0.8
 */
public enum NamingPolicy {

    LOWER_CAMEL_CASE(str -> Strings.toCamelCase(str)),

    LOWER_CASE_WITH_UNDERSCORE(str -> Strings.toLowerCaseWithUnderscore(str)),

    UPPER_CASE_WITH_UNDERSCORE(str -> Strings.toUpperCaseWithUnderscore(str)),

    @Beta
    NO_CHANGE(str -> str);

    private final Function<String, String> converter;

    NamingPolicy(Function<String, String> converter) {
        this.converter = converter;
    }

    /**
     *
     * @param str
     * @return
     */
    public String convert(String str) {
        return converter.apply(str);
    }
}
