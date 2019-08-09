/*
 * Copyright (C) 2019 HaiYang Li
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

import com.landawn.abacus.annotation.Type.EnumType;

// TODO: Auto-generated Javadoc
/**
 * The Interface JsonXmlField.
 */
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonXmlField {

    /**
     * Name.
     *
     * @return the string
     */
    String name() default "";

    /**
     * Type.
     *
     * @return the string
     */
    String type() default "";

    /**
     * Enumerated.
     *
     * @return the enum type
     */
    EnumType enumerated() default EnumType.STRING;

    /**
     * Date format.
     *
     * @return the string
     */
    String dateFormat() default "";

    /**
     * Time zone.
     *
     * @return the string
     */
    String timeZone() default "";

    /**
     * Number format.
     *
     * @return the string
     * @see DecimalFormat
     */
    String numberFormat() default "";

    /**
     * Ignore.
     *
     * @return true, if successful
     */
    boolean ignore() default false;
}
