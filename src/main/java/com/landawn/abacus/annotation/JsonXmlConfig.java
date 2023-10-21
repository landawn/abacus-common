/*
 * Copyright (C) 2020 HaiYang Li
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

package com.landawn.abacus.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.text.DecimalFormat;

import com.landawn.abacus.annotation.Type.EnumBy;
import com.landawn.abacus.parser.Exclusion;
import com.landawn.abacus.util.NamingPolicy;

@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonXmlConfig {

    /**
     *
     *
     * @return
     */
    NamingPolicy namingPolicy() default NamingPolicy.LOWER_CAMEL_CASE;

    /**
     * values can be regular expressions.
     *
     * @return
     * @see String#matches(String)
     */
    String[] ignoredFields() default {};

    /**
     *
     *
     * @return
     */
    String dateFormat() default "";

    /**
     *
     *
     * @return
     */
    String timeZone() default "";

    /**
     *
     * @return
     * @see DecimalFormat
     */
    String numberFormat() default "";

    /**
     *
     *
     * @return
     */
    EnumBy enumerated() default EnumBy.NAME;

    /**
     *
     *
     * @return
     */
    Exclusion exclusion() default Exclusion.NULL;
}
