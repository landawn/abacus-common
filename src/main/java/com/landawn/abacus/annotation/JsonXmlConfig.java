/*
 * Copyright (C) 2020 HaiYang Li
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

package com.landawn.abacus.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.text.DecimalFormat;

import com.landawn.abacus.annotation.Type.EnumBy;
import com.landawn.abacus.parser.Exclusion;
import com.landawn.abacus.util.NamingPolicy;

/**
 * Configures JSON and XML serialization/deserialization behavior for the annotated class.
 * This annotation allows customization of various aspects of the serialization process
 * including naming policies, field exclusion, date/time formatting, and more.
 * 
 * <p>When applied to a class, the configuration affects how instances of that class
 * are serialized to and deserialized from JSON or XML formats.</p>
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonXmlConfig {

    /**
     * Specifies the naming policy to use when converting field names during serialization.
     * The default is LOWER_CAMEL_CASE (e.g., "myFieldName").
     * 
     * @return the naming policy to apply to field names
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
     * Specifies the date format pattern to use for date/time serialization.
     * If empty, the default format is used.
     * 
     * @return the date format pattern (e.g., "yyyy-MM-dd HH:mm:ss"), empty string for default
     */
    String dateFormat() default "";

    /**
     * Specifies the time zone to use for date/time serialization.
     * If empty, the system default time zone is used.
     * 
     * @return the time zone ID (e.g., "UTC", "America/New_York"), empty string for default
     */
    String timeZone() default "";

    /**
     * Specifies the number format pattern to use for numeric values during serialization.
     * If empty, numbers are serialized using their default representation.
     * 
     * @return the number format pattern, empty string for default
     * @see DecimalFormat
     */
    String numberFormat() default "";

    /**
     * Specifies how enum values should be serialized.
     * The default is EnumBy.NAME which uses the enum constant name.
     * 
     * @return the enum serialization strategy
     */
    EnumBy enumerated() default EnumBy.NAME;

    /**
     * Specifies the exclusion policy for field serialization.
     * The default is Exclusion.NULL which excludes null fields from serialization.
     * 
     * @return the exclusion policy to apply
     */
    Exclusion exclusion() default Exclusion.NULL;
}
