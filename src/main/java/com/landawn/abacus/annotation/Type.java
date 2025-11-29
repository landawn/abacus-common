/*
 * Copyright (C) 2016 HaiYang Li
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

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Specifies custom type handling for fields or methods during serialization and persistence operations.
 * This powerful annotation provides fine-grained control over how values are converted between
 * different representations in contexts such as JSON/XML serialization, database persistence,
 * and data transformation.
 * 
 * <p><b>Key capabilities:</b></p>
 * <ul>
 *   <li>Override default type conversion behavior</li>
 *   <li>Specify custom type converters for complex transformations</li>
 *   <li>Control enum representation (name vs ordinal)</li>
 *   <li>Scope type handling to specific contexts (serialization, persistence, or both)</li>
 * </ul>
 * 
 * <p><b>Common use cases:</b></p>
 * <ul>
 *   <li>Custom date/time formatting for serialization</li>
 *   <li>Encrypting/decrypting sensitive data during persistence</li>
 *   <li>Converting between different representations (e.g., storing JSON as String in DB)</li>
 *   <li>Handling legacy data formats</li>
 *   <li>Custom enum mappings</li>
 * </ul>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 *
 * public class User {
 *     @Type(name = "EncryptedString", scope = Scope.PERSISTENCE)
 *     private String password;  // Encrypted when saved to DB
 *
 *     @Type(enumerated = EnumBy.ORDINAL)
 *     private Status status;  // Stored as integer in DB
 *
 *     @Type(clazz = CustomDateType.class)
 *     private Date createdDate;  // Uses custom date formatting
 *
 *     @Type(name = "JsonString", scope = Scope.SERIALIZATION)
 *     private Object metadata;  // Serialized as JSON string in output
 * }
 * }</pre>
 *
 * @see JsonXmlField
 * @see Column
 */
@Documented
@Target({ FIELD, METHOD })
@Retention(RUNTIME)
public @interface Type {

    /**
     * Use {@code name} to specify attribute explicitly
     *
     * @return the type name value, or empty string if not specified
     * @deprecated use {@code name} to specify attribute explicitly.
     */
    @Deprecated
    String value() default "";

    /**
     * Specifies the type name to use for type conversion.
     * This should match a registered type name in the type factory.
     *
     * <p>Common built-in type names include:</p>
     * <ul>
     *   <li>"String", "Integer", "Long", etc. - basic types</li>
     *   <li>"Date", "LocalDateTime", "Instant" - temporal types</li>
     *   <li>"BigDecimal", "BigInteger" - precise numeric types</li>
     *   <li>Custom type names registered with TypeFactory</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * @Type(name = "EncryptedString")
     * private String sensitiveData;
     *
     * @Type(name = "LocalDateTime")
     * private Date timestamp;
     * }</pre>
     *
     * @return the type name, or empty string to use default type handling
     */
    String name() default "";

    /**
     * Specifies a custom Type class to handle value conversion.
     * The specified class must extend {@link com.landawn.abacus.type.Type} and
     * implement the necessary conversion methods.
     *
     * <p>This is useful for complex custom conversions that can't be handled
     * by the built-in types. The custom type class should:</p>
     * <ul>
     *   <li>Have a no-argument constructor</li>
     *   <li>Override the necessary conversion methods</li>
     *   <li>Be thread-safe if used in concurrent contexts</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * public class EncryptedStringType extends Type<String> {
     *     @Override
     *     public String valueOf(String value) {
     *         return decrypt(value);
     *     }
     *
     *     @Override
     *     public String stringOf(String value) {
     *         return encrypt(value);
     *     }
     * }
     *
     * @Type(clazz = EncryptedStringType.class)
     * private String sensitiveData;
     * }</pre>
     *
     * @return the custom Type implementation class, or base Type.class for default handling
     */
    @SuppressWarnings("rawtypes")
    Class<? extends com.landawn.abacus.type.Type> clazz() default com.landawn.abacus.type.Type.class;

    /**
     * Specifies how enum values should be represented during conversion.
     * This affects both serialization and persistence operations.
     *
     * <p><b>EnumBy.NAME (default):</b></p>
     * <ul>
     *   <li>Uses the enum constant name as a string</li>
     *   <li>More readable and maintainable</li>
     *   <li>Resilient to enum reordering</li>
     *   <li>Larger storage size</li>
     * </ul>
     *
     * <p><b>EnumBy.ORDINAL:</b></p>
     * <ul>
     *   <li>Uses the enum ordinal position as an integer</li>
     *   <li>Smaller storage size</li>
     *   <li>Fragile - breaks if enum order changes</li>
     *   <li>Less readable in raw data</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * public enum Priority { LOW, MEDIUM, HIGH, URGENT }
     *
     * // Using NAME (default) - stored as "LOW", "MEDIUM", etc.
     * @Type(enumerated = EnumBy.NAME)
     * private Priority priority;
     *
     * // Using ORDINAL - stored as 0, 1, 2, 3
     * @Type(enumerated = EnumBy.ORDINAL)
     * private Priority priority;
     * }</pre>
     *
     * @return the enum representation strategy, defaults to EnumBy.NAME
     */
    EnumBy enumerated() default EnumBy.NAME;

    /**
     * Specifies the scope where this type conversion should apply.
     * This allows different type handling for different contexts.
     *
     * <p><b>Scope options:</b></p>
     * <ul>
     *   <li>SERIALIZATION - Only for JSON/XML/etc. serialization</li>
     *   <li>PERSISTENCE - Only for database operations</li>
     *   <li>ALL (default) - Applies to both contexts</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * public class User {
     *     // Encrypted in database but not in JSON/XML
     *     @Type(name = "EncryptedString", scope = Scope.PERSISTENCE)
     *     private String password;
     *
     *     // Custom date format only for JSON/XML output
     *     @Type(name = "ISODate", scope = Scope.SERIALIZATION)
     *     private Date birthDate;
     *
     *     // Custom handling for both database and JSON/XML
     *     @Type(name = "JsonString", scope = Scope.ALL)
     *     private Map<String, Object> metadata;
     * }
     * }</pre>
     *
     * @return the scope of type conversion, defaults to Scope.ALL
     */
    Scope scope() default Scope.ALL;

    /**
     * Defines strategies for representing enum values during type conversion.
     * The choice between NAME and ORDINAL affects data portability, storage size,
     * and resilience to code changes.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * public enum Status { PENDING, ACTIVE, SUSPENDED, CLOSED }
     *
     * // Using NAME - stored as "ACTIVE", "SUSPENDED", etc.
     * @Type(enumerated = EnumBy.NAME)
     * private Status accountStatus;
     *
     * // Using ORDINAL - stored as 0, 1, 2, 3
     * @Type(enumerated = EnumBy.ORDINAL)
     * private Status orderStatus;
     * }</pre>
     */
    enum EnumBy {
        /**
         * Persist enumerated type property or field as an integer using its ordinal position.
         *
         * <p><b>Warning:</b> This representation is fragile as it depends on the declaration order
         * of enum constants. Adding, removing, or reordering enum values will break
         * compatibility with existing persisted data.</p>
         *
         * <p><b>Example:</b> For enum {@code Status { PENDING, ACTIVE, CLOSED }},
         * values are stored as 0, 1, 2 respectively. If you later change it to
         * {@code Status { ACTIVE, PENDING, CLOSED }}, all existing data will be incorrect.</p>
         *
         * <p><b>Use when:</b></p>
         * <ul>
         *   <li>Storage space is critical</li>
         *   <li>The enum is guaranteed to never change order</li>
         *   <li>Performance is a priority and enum order is stable</li>
         * </ul>
         */
        ORDINAL,

        /**
         * Persist enumerated type property or field as a string using its constant name.
         *
         * <p>This is the recommended approach as it's readable, maintainable, and resilient
         * to enum reordering. The only concern is renaming enum constants, which would
         * require data migration.</p>
         *
         * <p><b>Example:</b> For enum {@code Status { PENDING, ACTIVE, CLOSED }},
         * values are stored as "PENDING", "ACTIVE", "CLOSED". You can safely reorder
         * or add new values without breaking existing data.</p>
         *
         * <p><b>Benefits:</b></p>
         * <ul>
         *   <li>Human-readable data in storage</li>
         *   <li>Resilient to enum reordering</li>
         *   <li>Easier debugging and data inspection</li>
         *   <li>Safe to add new enum values</li>
         * </ul>
         */
        NAME
    }

    /**
     * Defines the operational contexts where custom type conversion should be applied.
     * This enables different data representations for different use cases, such as
     * human-readable formats for APIs versus efficient storage for databases.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * public class SecureDocument {
     *     // Encrypted only when persisting to database
     *     @Type(name = "EncryptedString", scope = Scope.PERSISTENCE)
     *     private String content;
     *
     *     // Custom format only for JSON/XML output
     *     @Type(name = "ISO8601Date", scope = Scope.SERIALIZATION)
     *     private Date publishedDate;
     *
     *     // Custom handling everywhere
     *     @Type(name = "JsonString", scope = Scope.ALL)
     *     private Object metadata;
     * }
     * }</pre>
     */
    enum Scope {
        /**
         * Apply type conversion for serialization/deserialization operations.
         * This includes JSON, XML, and other data interchange formats.
         *
         * <p>Useful for controlling how data appears in API responses, configuration files,
         * or when transferring data between systems.</p>
         *
         * <p><b>Use cases:</b></p>
         * <ul>
         *   <li>Custom date formats for REST APIs</li>
         *   <li>Simplified representations for external consumers</li>
         *   <li>Masking sensitive data in responses</li>
         *   <li>Converting complex types to strings for JSON</li>
         * </ul>
         *
         * <p><b>Example:</b></p>
         * <pre>{@code
         * @Type(name = "ISO8601", scope = Scope.SERIALIZATION)
         * private Date timestamp;  // Only affects JSON/XML, not database
         * }</pre>
         */
        SERIALIZATION,

        /**
         * Apply type conversion for database persistence operations.
         * This affects how values are stored in and retrieved from the database.
         *
         * <p>Useful for custom database mappings, encryption, compression, or adapting
         * to legacy database schemas.</p>
         *
         * <p><b>Use cases:</b></p>
         * <ul>
         *   <li>Encrypting sensitive data before storage</li>
         *   <li>Compressing large text fields</li>
         *   <li>Converting between incompatible types</li>
         *   <li>Adapting to legacy column types</li>
         * </ul>
         *
         * <p><b>Example:</b></p>
         * <pre>{@code
         * @Type(name = "EncryptedString", scope = Scope.PERSISTENCE)
         * private String password;  // Only affects database, not JSON/XML
         * }</pre>
         */
        PERSISTENCE,

        /**
         * Apply type conversion in all contexts (both serialization and persistence).
         * This ensures consistent data handling across all operations.
         *
         * <p>Use this when the same conversion logic should apply everywhere,
         * providing uniform behavior regardless of the operation type.</p>
         *
         * <p><b>Use cases:</b></p>
         * <ul>
         *   <li>Consistent date/time formatting</li>
         *   <li>Standard enum representations</li>
         *   <li>Universal type mappings</li>
         *   <li>Cross-cutting concerns</li>
         * </ul>
         *
         * <p><b>Example:</b></p>
         * <pre>{@code
         * @Type(name = "JsonString", scope = Scope.ALL)
         * private Map<String, Object> settings;  // Affects both DB and JSON/XML
         * }</pre>
         */
        ALL
    }
}
