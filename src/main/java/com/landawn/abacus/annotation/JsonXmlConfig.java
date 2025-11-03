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
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * @JsonXmlConfig(namingPolicy = NamingPolicy.UPPER_CAMEL_CASE,
 *                exclusion = Exclusion.DEFAULT)
 * public class User { }
 * }</pre>
 *
 * @see NamingPolicy
 * @see Exclusion
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonXmlConfig {

    /**
     * Specifies the naming policy to use when converting field names during serialization.
     * This controls how Java field names are transformed into JSON/XML property names.
     *
     * <p><b>Common naming policies:</b></p>
     * <ul>
     *   <li>LOWER_CAMEL_CASE: myFieldName (default)</li>
     *   <li>UPPER_CAMEL_CASE: MyFieldName</li>
     *   <li>LOWER_CASE_WITH_UNDERSCORE: my_field_name</li>
     *   <li>UPPER_CASE_WITH_UNDERSCORE: MY_FIELD_NAME</li>
     * </ul>
     *
     * @return the naming policy to apply to field names during serialization
     */
    NamingPolicy namingPolicy() default NamingPolicy.LOWER_CAMEL_CASE;

    /**
     * Specifies fields to ignore during serialization/deserialization.
     * Values can be exact field names or regular expressions for pattern matching.
     *
     * <p>This is useful for excluding sensitive data, computed fields, or fields
     * that should not be persisted.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * @JsonXmlConfig(ignoredFields = {"password", "internal.*"})
     * public class User {
     *     private String password;      // Will be ignored
     *     private String internalToken; // Will be ignored (matches "internal.*")
     * }
     * }</pre>
     *
     * @return array of field names or regex patterns to exclude from serialization
     * @see String#matches(String)
     */
    String[] ignoredFields() default {};

    /**
     * Specifies the date format pattern to use for date/time serialization.
     * If empty, the default ISO-8601 format is used.
     *
     * <p>The pattern must conform to {@link java.text.SimpleDateFormat} conventions.</p>
     *
     * <p><b>Common patterns:</b></p>
     * <ul>
     *   <li>"yyyy-MM-dd" for dates only</li>
     *   <li>"yyyy-MM-dd HH:mm:ss" for date and time</li>
     *   <li>"yyyy-MM-dd'T'HH:mm:ss.SSSZ" for ISO-8601 with milliseconds</li>
     * </ul>
     *
     * @return the date format pattern, empty string for default ISO-8601 format
     * @see java.text.SimpleDateFormat
     */
    String dateFormat() default "";

    /**
     * Specifies the time zone to use for date/time serialization.
     * If empty, the system default time zone is used.
     *
     * <p>This ensures consistent date/time representation across different environments.
     * Common practice is to use UTC for storing and transmitting dates.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <ul>
     *   <li>"UTC" - Coordinated Universal Time</li>
     *   <li>"America/New_York" - Eastern Time</li>
     *   <li>"Europe/London" - British Time</li>
     *   <li>"Asia/Tokyo" - Japan Standard Time</li>
     * </ul>
     *
     * @return the time zone ID, empty string for system default time zone
     * @see java.util.TimeZone#getTimeZone(String)
     */
    String timeZone() default "";

    /**
     * Specifies the number format pattern to use for numeric values during serialization.
     * If empty, numbers are serialized using their default string representation.
     *
     * <p>This is particularly useful for controlling decimal precision and formatting
     * of monetary values or scientific notations.</p>
     *
     * <p><b>Common patterns:</b></p>
     * <ul>
     *   <li>"#,##0.00" - Two decimal places with thousand separators</li>
     *   <li>"0.000" - Three decimal places, always showing zeros</li>
     *   <li>"#.##" - Up to two decimal places, omitting trailing zeros</li>
     *   <li>"0.00E0" - Scientific notation</li>
     * </ul>
     *
     * @return the number format pattern, empty string for default representation
     * @see DecimalFormat
     */
    String numberFormat() default "";

    /**
     * Specifies how enum values should be serialized.
     * Controls whether enums are represented by their name or ordinal value in JSON/XML.
     *
     * <p><b>Options:</b></p>
     * <ul>
     *   <li>EnumBy.NAME (default): Uses the enum constant name (e.g., "ACTIVE")</li>
     *   <li>EnumBy.ORDINAL: Uses the enum's ordinal position (e.g., 0, 1, 2)</li>
     * </ul>
     *
     * <p><b>Recommendation:</b> EnumBy.NAME is more readable and resilient to enum
     * reordering, while EnumBy.ORDINAL is more compact but fragile.</p>
     *
     * @return the enum serialization strategy
     */
    EnumBy enumerated() default EnumBy.NAME;

    /**
     * Specifies the exclusion policy for field serialization.
     * Determines which fields should be excluded from the serialized output.
     *
     * <p><b>Common exclusion policies:</b></p>
     * <ul>
     *   <li>Exclusion.NULL (default): Excludes fields with {@code null} values</li>
     *   <li>Exclusion.DEFAULT: Excludes {@code null} values and primitive type default values (0, false, etc.)</li>
     *   <li>Exclusion.NONE: Includes all fields</li>
     * </ul>
     *
     * <p>This helps reduce payload size and improve clarity by omitting unnecessary data.</p>
     *
     * @return the exclusion policy to apply during serialization
     * @see Exclusion
     */
    Exclusion exclusion() default Exclusion.NULL;
}
