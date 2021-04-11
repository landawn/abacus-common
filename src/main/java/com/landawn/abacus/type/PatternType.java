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

package com.landawn.abacus.type;

import java.util.regex.Pattern;

import com.landawn.abacus.util.N;

/**
 *
 * @author Haiyang Li
 * @since 0.8
 */
public class PatternType extends AbstractType<Pattern> {

    public static final String PATTERN = "Pattern";

    PatternType() {
        super(PATTERN);
    }

    @Override
    public Class<Pattern> clazz() {
        return Pattern.class;
    }

    /**
     *
     * @param t
     * @return
     */
    @Override
    public String stringOf(Pattern t) {
        return (t == null) ? null : t.toString();
    }

    /**
     *
     * @param st
     * @return
     */
    @Override
    public Pattern valueOf(String st) {
        return (N.isNullOrEmpty(st)) ? null : Pattern.compile(st);
    }
}
