/*
 * Copyright (C) 2019 HaiYang Li
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

/**
 * Configures JSON and XML serialization/deserialization behavior for individual fields.
 * This annotation provides fine-grained control over how a specific field is handled
 * during JSON or XML processing, allowing customization of field names, formats, and serialization rules.
 * 
 * <p>This annotation can be used to:</p>
 * <ul>
 *   <li>Customize field names in the serialized output</li>
 *   <li>Define aliases for deserialization</li>
 *   <li>Specify formatting for dates, numbers, and enums</li>
 *   <li>Control field inclusion/exclusion in serialization</li>
 *   <li>Handle raw JSON values</li>
 * </ul>
 * 
 * <p><b>Example usage:</b></p>
 * <pre>
 * public class User {
 *     {@literal @}JsonXmlField(name = "user_id")
 *     private Long id;
 *     
 *     {@literal @}JsonXmlField(name = "full_name", alias = {"name", "userName"})
 *     private String fullName;
 *     
 *     {@literal @}JsonXmlField(dateFormat = "yyyy-MM-dd", timeZone = "UTC")
 *     private Date birthDate;
 *     
 *     {@literal @}JsonXmlField(numberFormat = "#.##")
 *     private Double salary;
 *     
 *     {@literal @}JsonXmlField(ignore = true)
 *     private String password;
 * }
 * </pre>
 * 
 * @author HaiYang Li
 * @since 2019
 * @see JsonXmlConfig
 * @see JsonXmlCreator
 * @see JsonXmlValue
 */
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonXmlField {

    /**
     * Specifies the field name to use in JSON/XML serialization.
     * If not specified (empty string), the Java field name is used.
     * 
     * <p><b>Example:</b></p>
     * <pre>
     * {@literal @}JsonXmlField(name = "user_id")
     * private Long id;  // Serialized as "user_id" instead of "id"
     * </pre>
     * 
     * @return the field name for serialization, empty string for default Java field name
     */
    String name() default "";

    /**
     * Defines alternative names that can be used during deserialization.
     * This allows the field to be deserialized from JSON/XML using any of the specified alias names.
     * 
     * <p><b>Example:</b></p>
     * <pre>
     * {@literal @}JsonXmlField(name = "full_name", alias = {"name", "userName", "user_name"})
     * private String fullName;
     * // Can be deserialized from any of: "full_name", "name", "userName", "user_name"
     * </pre>
     * 
     * @return array of alias names for deserialization
     */
    String[] alias() default {};

    /**
     * Specifies the explicit type for field serialization/deserialization.
     * This can be used to override the default type inference.
     * 
     * <p><b>Example:</b></p>
     * <pre>
     * {@literal @}JsonXmlField(type = "java.util.Date")
     * private Object dateValue;  // Forces treatment as Date type
     * </pre>
     * 
     * @return the explicit type string, empty string for automatic type detection
     */
    String type() default "";

    /**
     * Specifies how enum values should be serialized for this field.
     * 
     * <p><b>Options:</b></p>
     * <ul>
     *   <li>NAME - Use the enum constant name (default)</li>
     *   <li>ORDINAL - Use the enum ordinal value</li>
     *   <li>TO_STRING - Use the enum's toString() method</li>
     * </ul>
     * 
     * <p><b>Example:</b></p>
     * <pre>
     * {@literal @}JsonXmlField(enumerated = EnumBy.ORDINAL)
     * private Status status;  // Serialized as 0, 1, 2... instead of "ACTIVE", "INACTIVE"...
     * </pre>
     * 
     * @return the enum serialization strategy
     */
    EnumBy enumerated() default EnumBy.NAME;

    /**
     * Specifies the date format pattern for date/time field serialization.
     * Uses SimpleDateFormat pattern syntax.
     * 
     * <p><b>Example:</b></p>
     * <pre>
     * {@literal @}JsonXmlField(dateFormat = "yyyy-MM-dd HH:mm:ss")
     * private Date created;  // Serialized as "2023-12-25 10:30:45"
     * 
     * {@literal @}JsonXmlField(dateFormat = "ISO_INSTANT")
     * private Instant timestamp;  // Serialized as "2023-12-25T10:30:45.123Z"
     * </pre>
     * 
     * @return the date format pattern, empty string for default format
     * @see java.text.SimpleDateFormat
     * @see java.time.format.DateTimeFormatter
     */
    String dateFormat() default "";

    /**
     * Specifies the time zone for date/time field serialization.
     * 
     * <p><b>Example:</b></p>
     * <pre>
     * {@literal @}JsonXmlField(dateFormat = "yyyy-MM-dd HH:mm:ss", timeZone = "UTC")
     * private Date eventTime;  // Always serialized in UTC timezone
     * 
     * {@literal @}JsonXmlField(timeZone = "America/New_York")
     * private LocalDateTime meetingTime;
     * </pre>
     * 
     * @return the time zone ID (e.g., "UTC", "America/New_York"), empty string for system default
     */
    String timeZone() default "";

    /**
     * Specifies the number format pattern for numeric field serialization.
     * Uses DecimalFormat pattern syntax.
     * 
     * <p><b>Example:</b></p>
     * <pre>
     * {@literal @}JsonXmlField(numberFormat = "#.##")
     * private Double price;  // Serialized with 2 decimal places: "19.99"
     * 
     * {@literal @}JsonXmlField(numberFormat = "###,###.00")
     * private BigDecimal amount;  // Serialized with comma separators: "1,234.56"
     * </pre>
     * 
     * @return the number format pattern, empty string for default formatting
     * @see DecimalFormat
     */
    String numberFormat() default "";

    /**
     * Specifies whether this field should be completely ignored during serialization and deserialization.
     * When set to {@code true}, the field will be excluded from JSON/XML processing.
     * 
     * <p><b>Example:</b></p>
     * <pre>
     * public class User {
     *     private String username;
     *     
     *     {@literal @}JsonXmlField(ignore = true)
     *     private String password;  // Never included in JSON/XML
     *     
     *     {@literal @}JsonXmlField(ignore = true)
     *     private String internalId;  // Hidden from serialization
     * }
     * </pre>
     * 
     * @return {@code true} to ignore this field during JSON/XML processing, {@code false} to include it
     */
    boolean ignore() default false;

    /**
     * Indicates whether the field value should be treated as raw JSON/XML content.
     * When {@code true}, the field's string value is inserted directly into the output
     * without additional JSON/XML escaping or formatting.
     * 
     * <p>This is useful when a field contains pre-serialized JSON/XML that should be
     * embedded as-is in the output.</p>
     * 
     * <p><b>Example:</b></p>
     * <pre>
     * public class Document {
     *     private String title;
     *     
     *     {@literal @}JsonXmlField(isJsonRawValue = true)
     *     private String metadata;  // Contains: "{"key":"value"}"
     *     // Result: {"title":"My Doc","metadata":{"key":"value"}}
     *     // Instead of: {"title":"My Doc","metadata":"{\"key\":\"value\"}"}
     * }
     * </pre>
     * 
     * @return {@code true} if the field contains raw JSON/XML content, {@code false} for normal processing
     */
    boolean isJsonRawValue() default false;

    //    /**
    //     *
    //     * @return
    //     * @deprecated should use the order of fields defined.
    //     */
    //    @Deprecated
    //    int ordinal() default -1;

    /**
     * Controls whether the field is included during serialization, deserialization, or both.
     * 
     * <p><b>Options:</b></p>
     * <ul>
     *   <li>DEFAULT - Include in both serialization and deserialization (default)</li>
     *   <li>SERIALIZE_ONLY - Include only when writing JSON/XML output</li>
     *   <li>DESERIALIZE_ONLY - Include only when reading from JSON/XML input</li>
     * </ul>
     * 
     * <p><b>Example:</b></p>
     * <pre>
     * public class User {
     *     {@literal @}JsonXmlField(expose = Expose.SERIALIZE_ONLY)
     *     private String displayName;  // Written to JSON but not read from it
     *     
     *     {@literal @}JsonXmlField(expose = Expose.DESERIALIZE_ONLY)
     *     private String tempPassword;  // Read from JSON but not written to it
     * }
     * </pre>
     * 
     * @return the exposure mode for this field
     */
    Expose expose() default Expose.DEFAULT;

    /**
     * Defines the exposure levels for field serialization and deserialization operations.
     * This enum controls whether a field participates in JSON/XML reading, writing, or both.
     */
    enum Expose {
        /**
         * The field participates in both serialization and deserialization operations.
         * This is the standard behavior for most fields.
         * 
         * @deprecated don't need to set it. It's {@code DEFAULT} by default.
         */
        @Deprecated
        DEFAULT,

        /**
         * The field is only included during serialization (object to JSON/XML).
         * During deserialization (JSON/XML to object), this field will be ignored.
         * 
         * <p>Use this for:</p>
         * <ul>
         *   <li>Computed fields that should appear in output but not be set from input</li>
         *   <li>Read-only properties like timestamps, IDs, or calculated values</li>
         *   <li>Fields that contain sensitive information in responses but shouldn't be settable</li>
         * </ul>
         */
        SERIALIZE_ONLY,

        /**
         * The field is only included during deserialization (JSON/XML to object).
         * During serialization (object to JSON/XML), this field will be ignored.
         * 
         * <p>Use this for:</p>
         * <ul>
         *   <li>Input-only fields like passwords or temporary data</li>
         *   <li>Fields that should accept values but never expose them</li>
         *   <li>Configuration fields that are needed for setup but not for output</li>
         * </ul>
         */
        DESERIALIZE_ONLY
    }
}
