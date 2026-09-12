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

package com.landawn.abacus.parser;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.VarHandle;
import java.lang.invoke.VarHandle.AccessMode;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import com.landawn.abacus.annotation.AccessFieldByMethod;
import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.annotation.Column;
import com.landawn.abacus.annotation.Entity;
import com.landawn.abacus.annotation.Id;
import com.landawn.abacus.annotation.Internal;
import com.landawn.abacus.annotation.JsonXmlConfig;
import com.landawn.abacus.annotation.JsonXmlField;
import com.landawn.abacus.annotation.JsonXmlField.Direction;
import com.landawn.abacus.annotation.ReadOnly;
import com.landawn.abacus.annotation.ReadOnlyId;
import com.landawn.abacus.annotation.SuppressFBWarnings;
import com.landawn.abacus.annotation.Table;
import com.landawn.abacus.annotation.Transient;
import com.landawn.abacus.annotation.Type.Scope;
import com.landawn.abacus.exception.ParsingException;
import com.landawn.abacus.logging.Logger;
import com.landawn.abacus.logging.LoggerFactory;
import com.landawn.abacus.type.ObjectType;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.type.TypeFactory;
import com.landawn.abacus.util.Beans;
import com.landawn.abacus.util.BufferedJsonWriter;
import com.landawn.abacus.util.BufferedXmlWriter;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.ConcurrentCacheMap;
import com.landawn.abacus.util.Dates;
import com.landawn.abacus.util.EnumType;
import com.landawn.abacus.util.EscapeUtil;
import com.landawn.abacus.util.ExceptionUtil;
import com.landawn.abacus.util.ImmutableList;
import com.landawn.abacus.util.ImmutableMap;
import com.landawn.abacus.util.InternalUtil;
import com.landawn.abacus.util.Multiset;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.NamingPolicy;
import com.landawn.abacus.util.Numbers;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.SK;
import com.landawn.abacus.util.Splitter;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.cs;

/**
 * Utility class for parser-related operations, providing methods for handling
 * bean metadata, property information, and serialization/deserialization configurations.
 *
 * <p>This class is marked as {@code @Internal} and is not intended for direct use
 * by application code. It provides low-level utilities for the parser framework.</p>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Get bean metadata
 * BeanInfo beanInfo = ParserUtil.getBeanInfo(MyBean.class);
 * List<PropInfo> properties = beanInfo.propInfoList;
 *
 * // Access property information
 * PropInfo nameProp = beanInfo.getPropInfo("name");
 * Object value = nameProp.getPropValue(myBeanInstance);
 * }</pre>
 *
 * @see BeanInfo
 * @see PropInfo
 */
@Internal
@SuppressWarnings({ "java:S1192", "java:S1942", "java:S2143" })
public final class ParserUtil {

    static final Logger logger = LoggerFactory.getLogger(ParserUtil.class);

    static final char[] NULL_CHAR_ARRAY = "null".toCharArray();

    // private static final PropInfo PROP_INFO_MASK = new PropInfo("PROP_INFO_MASK");

    private static final char PROP_NAME_SEPARATOR = '.';

    // Shared, stateless splitter for nested property paths (e.g. "address.city"); reused to avoid per-call allocation.
    private static final Splitter PROP_NAME_SPLITTER = Splitter.with(PROP_NAME_SEPARATOR);

    // Conventional JavaBean accessor prefixes.
    private static final String GET = "get";

    private static final String SET = "set";

    private static final String IS = "is";

    private static final String HAS = "has";

    private static final Set<Class<?>> idTypeSet = N.asSet(int.class, Integer.class, long.class, Long.class, String.class, Timestamp.class, UUID.class);

    @SuppressWarnings("deprecation")
    private static final int POOL_SIZE = InternalUtil.POOL_SIZE;

    private static final int defaultNameIndex = NamingPolicy.CAMEL_CASE.ordinal();

    // Cached once: NamingPolicy.values() clones its backing array on every call, and name tags are built per property.
    private static final NamingPolicy[] NAMING_POLICIES = NamingPolicy.values();

    // Bean metadata cache, keyed by the complete reflective type.
    private static final Map<java.lang.reflect.Type, BeanInfo> beanInfoPool = new ConcurrentCacheMap<>(POOL_SIZE);

    /**
     * The point past which a {@link BeanInfo}'s lookup-time caches ({@code propInfoMap} fuzzy hits, the
     * negative cache of unknown names and {@code propInfoQueueMap}) stop accepting new entries.
     *
     * <p>Those caches are keyed by caller-supplied strings (JSON keys, XML element names, map keys), so without
     * a cap a stream of documents with distinct unknown names grows them for the lifetime of the JVM (the
     * {@code BeanInfo} itself is pooled forever). Skipping an insert only costs a recomputation on the next
     * lookup and changes no result. Same precedent as {@code Beans.MAX_CACHED_NAMES}.</p>
     */
    private static final int MAX_CACHED_PROP_NAMES = POOL_SIZE * 2;

    private ParserUtil() {
        // Singleton.
    }

    /**
     * Determines whether a field should be serialized to JSON or XML based on its modifiers and annotations.
     *
     * <p>A field is considered serializable if it is not static and not explicitly marked as ignored
     * through various annotation mechanisms including {@code @JsonXmlField(ignore=true)},
     * {@code @JSONField(serialize=false)}, or {@code @JsonIgnore}. It is also excluded when its name
     * equals, or matches as a regular expression, one of {@code JsonXmlConfig.ignoredFields()}.</p>
     *
     * <p>A {@code null} field is reported as serializable: this overload knows the property only through its
     * field, so it cannot apply {@code ignoredFields} to a method-only property. Bean introspection uses
     * {@link #isJsonXmlSerializable(String, Field, JsonXmlConfig)}, which matches by property name and
     * therefore also honours {@code ignoredFields} for getter-only (computed) properties.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Field field = MyBean.class.getDeclaredField("name");
     * JsonXmlConfig config = MyBean.class.getAnnotation(JsonXmlConfig.class);
     * boolean canSerialize = ParserUtil.isJsonXmlSerializable(field, config);
     * }</pre>
     *
     * @param field the field to check for serializability
     * @param jsonXmlConfig the JSON/XML configuration that may contain ignored field patterns
     * @return {@code true} if the field should be serialized, {@code false} otherwise
     */
    static boolean isJsonXmlSerializable(final Field field, final JsonXmlConfig jsonXmlConfig) {
        return isJsonXmlSerializable(field == null ? null : field.getName(), field, jsonXmlConfig);
    }

    /**
     * Determines whether a property should be serialized to JSON or XML based on its name, its backing field's
     * modifiers and annotations, and the class-level {@code JsonXmlConfig}.
     *
     * <p>A field-backed property is excluded when the field is static or explicitly marked as ignored through
     * {@code @JsonXmlField(ignore=true)}, {@code @JSONField(serialize=false)} or {@code @JsonIgnore}. Any
     * property, field-backed or not, is excluded when {@code propName} (or, for a field-backed property whose
     * field is named differently, the field name) equals or matches as a regular expression one of
     * {@code JsonXmlConfig.ignoredFields()}. That is what lets {@code ignoredFields} drop a getter-only
     * computed property such as {@code getFullName()}, the "computed values" use case the annotation
     * documents.</p>
     *
     * @param propName the property name; {@code null} when unknown, in which case only the field is consulted
     * @param field the backing field, or {@code null} for a method-only property
     * @param jsonXmlConfig the JSON/XML configuration that may contain ignored field patterns
     * @return {@code true} if the property should be serialized, {@code false} otherwise
     */
    static boolean isJsonXmlSerializable(final String propName, final Field field, final JsonXmlConfig jsonXmlConfig) {
        if (field != null) {
            if (Modifier.isStatic(field.getModifiers())
                    || (field.isAnnotationPresent(JsonXmlField.class) && field.getAnnotation(JsonXmlField.class).ignore())) {
                return false;
            }

            try {
                if (field.isAnnotationPresent(com.alibaba.fastjson2.annotation.JSONField.class)
                        && !field.getAnnotation(com.alibaba.fastjson2.annotation.JSONField.class).serialize()) {
                    return false;
                }
            } catch (final Throwable e) { // NOSONAR
                // ignore
            }

            try {
                if (field.isAnnotationPresent(com.fasterxml.jackson.annotation.JsonIgnore.class)
                        && field.getAnnotation(com.fasterxml.jackson.annotation.JsonIgnore.class).value()) {
                    return false;
                }
            } catch (final Throwable e) { // NOSONAR
                // ignore
            }
        }

        if (jsonXmlConfig != null && N.notEmpty(jsonXmlConfig.ignoredFields())) {
            // Match the PROPERTY name so a getter-only property can be ignored; keep matching the field name
            // too so an existing config that lists a differently-named backing field keeps working.
            final String fieldName = field == null || field.getName().equals(propName) ? null : field.getName();

            for (final String ignoreFieldName : jsonXmlConfig.ignoredFields()) {
                if (isIgnoredName(propName, ignoreFieldName) || isIgnoredName(fieldName, ignoreFieldName)) {
                    return false;
                }
            }
        }

        return true;
    }

    private static boolean isIgnoredName(final String name, final String ignoreFieldName) {
        return name != null && (name.equals(ignoreFieldName) || name.matches(ignoreFieldName));
    }

    /**
     * Retrieves the date format pattern for a field based on annotations and configuration.
     *
     * <p>The method checks for date format patterns in the following order:</p>
     * <ol>
     *   <li>{@code @JsonXmlField(dateFormat="...")}</li>
     *   <li>{@code @JSONField(format="...")}</li>
     *   <li>{@code @JsonFormat(pattern="...")}</li>
     *   <li>Global date format from {@code JsonXmlConfig}</li>
     * </ol>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Field dateField = MyBean.class.getDeclaredField("createdDate");
     * JsonXmlConfig config = MyBean.class.getAnnotation(JsonXmlConfig.class);
     * String format = ParserUtil.getDateFormat(dateField, config);
     * // Returns "yyyy-MM-dd" if specified in annotations
     * }</pre>
     *
     * @param field the field to check for date format annotations
     * @param jsonXmlConfig the configuration that may contain a default date format
     * @return the date format pattern, or {@code null} if none is specified
     */
    static String getDateFormat(final Field field, final JsonXmlConfig jsonXmlConfig) {
        if (field != null) {
            if (field.isAnnotationPresent(JsonXmlField.class) && Strings.isNotEmpty(field.getAnnotation(JsonXmlField.class).dateFormat())) {
                return field.getAnnotation(JsonXmlField.class).dateFormat();
            }

            try {
                if (field.isAnnotationPresent(com.alibaba.fastjson2.annotation.JSONField.class)
                        && Strings.isNotEmpty(field.getAnnotation(com.alibaba.fastjson2.annotation.JSONField.class).format())) {
                    return field.getAnnotation(com.alibaba.fastjson2.annotation.JSONField.class).format();
                }
            } catch (final Throwable e) { // NOSONAR
                // ignore
            }

            try {
                if (field.isAnnotationPresent(com.fasterxml.jackson.annotation.JsonFormat.class)
                        && Strings.isNotEmpty(field.getAnnotation(com.fasterxml.jackson.annotation.JsonFormat.class).pattern())) {
                    return field.getAnnotation(com.fasterxml.jackson.annotation.JsonFormat.class).pattern();
                }
            } catch (final Throwable e) { // NOSONAR
                // ignore
            }
        }

        if (jsonXmlConfig != null && Strings.isNotEmpty(jsonXmlConfig.dateFormat())) {
            return jsonXmlConfig.dateFormat();
        }

        return null;
    }

    /**
     * Retrieves the time zone for a field based on annotations and configuration.
     *
     * <p>The method checks for time zone settings in the following order:</p>
     * <ol>
     *   <li>{@code @JsonXmlField(timeZone="...")}</li>
     *   <li>{@code @JsonFormat(timezone="...")}</li>
     *   <li>Global time zone from {@code JsonXmlConfig}</li>
     * </ol>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Field timestampField = MyBean.class.getDeclaredField("timestamp");
     * JsonXmlConfig config = MyBean.class.getAnnotation(JsonXmlConfig.class);
     * String timeZone = ParserUtil.getTimeZone(timestampField, config);
     * // Returns "UTC" if specified in annotations
     * }</pre>
     *
     * @param field the field to check for time zone annotations
     * @param jsonXmlConfig the configuration that may contain a default time zone
     * @return the time zone identifier, or {@code null} if none is specified
     */
    static String getTimeZone(final Field field, final JsonXmlConfig jsonXmlConfig) {
        if (field != null) {
            if (field.isAnnotationPresent(JsonXmlField.class) && Strings.isNotEmpty(field.getAnnotation(JsonXmlField.class).timeZone())) {
                return field.getAnnotation(JsonXmlField.class).timeZone();
            }

            try {
                if (field.isAnnotationPresent(com.fasterxml.jackson.annotation.JsonFormat.class)
                        && Strings.isNotEmpty(field.getAnnotation(com.fasterxml.jackson.annotation.JsonFormat.class).timezone())) {
                    return field.getAnnotation(com.fasterxml.jackson.annotation.JsonFormat.class).timezone();
                }
            } catch (final Throwable e) { // NOSONAR
                // ignore
            }
        }

        if (jsonXmlConfig != null && Strings.isNotEmpty(jsonXmlConfig.timeZone())) {
            return jsonXmlConfig.timeZone();
        }

        return null;
    }

    /**
     * Retrieves the number format pattern for a field based on annotations and configuration.
     *
     * <p>The method checks for number format patterns in the following order:</p>
     * <ol>
     *   <li>{@code @JsonXmlField(numberFormat="...")}</li>
     *   <li>Global number format from {@code JsonXmlConfig}</li>
     * </ol>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Field priceField = MyBean.class.getDeclaredField("price");
     * JsonXmlConfig config = MyBean.class.getAnnotation(JsonXmlConfig.class);
     * String format = ParserUtil.getNumberFormat(priceField, config);
     * // Returns "#,##0.00" if specified in annotations
     * }</pre>
     *
     * @param field the field to check for number format annotations
     * @param jsonXmlConfig the configuration that may contain a default number format
     * @return the number format pattern, or {@code null} if none is specified
     */
    static String getNumberFormat(final Field field, final JsonXmlConfig jsonXmlConfig) {
        if ((field != null) && (field.isAnnotationPresent(JsonXmlField.class) && Strings.isNotEmpty(field.getAnnotation(JsonXmlField.class).numberFormat()))) {
            return field.getAnnotation(JsonXmlField.class).numberFormat();
        }

        if (jsonXmlConfig != null && Strings.isNotEmpty(jsonXmlConfig.numberFormat())) {
            return jsonXmlConfig.numberFormat();
        }

        return null;
    }

    /**
     * Determines how enum values should be serialized for a field.
     *
     * <p>The method checks for enumeration settings in the following order:</p>
     * <ol>
     *   <li>{@code @JsonXmlField(enumerated=...)}</li>
     *   <li>Global enumeration setting from {@code JsonXmlConfig}</li>
     *   <li>Default to {@code EnumType.NAME}</li>
     * </ol>
     *
     * <p>The field-level value is only taken into account when it differs from {@code EnumType.NAME}:
     * the annotation attribute always has a value, so a field annotated only for some other attribute
     * would otherwise silently override the class-level {@code @JsonXmlConfig(enumerated = ...)} setting.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Field statusField = MyBean.class.getDeclaredField("status");
     * JsonXmlConfig config = MyBean.class.getAnnotation(JsonXmlConfig.class);
     * EnumType enumType = ParserUtil.getEnumerated(statusField, config);
     * // Returns EnumType.ORDINAL if specified in annotations
     * }</pre>
     *
     * @param field the field to check for enumeration annotations
     * @param jsonXmlConfig the configuration that may contain a default enumeration setting
     * @return the enumeration strategy, never {@code null} (defaults to {@code EnumType.NAME})
     */
    static EnumType getEnumerated(final Field field, final JsonXmlConfig jsonXmlConfig) {
        // Annotation methods never return null, so compare against the NAME default to detect an unset
        // field-level value; otherwise a field annotated for any other attribute (e.g. name) would always
        // report NAME and silently override the class-level @JsonXmlConfig(enumerated = ...) setting.
        if ((field != null) && field.isAnnotationPresent(JsonXmlField.class) && field.getAnnotation(JsonXmlField.class).enumerated() != EnumType.NAME) {
            return field.getAnnotation(JsonXmlField.class).enumerated();
        }

        if (jsonXmlConfig != null && jsonXmlConfig.enumerated() != null) {
            return jsonXmlConfig.enumerated();
        }

        return EnumType.NAME;
    }

    /**
     * Determines whether a field's value should be serialized as raw JSON.
     *
     * <p>A field is considered a raw JSON value if it's annotated with:</p>
     * <ul>
     *   <li>{@code @JsonXmlField(isJsonRawValue=true)}</li>
     *   <li>{@code @JsonRawValue}</li>
     *   <li>{@code @JSONField(jsonDirect=true)}</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Field jsonField = MyBean.class.getDeclaredField("rawJson");
     * boolean isRaw = ParserUtil.isJsonRawValue(jsonField);
     * // Returns true if field has @JsonRawValue annotation
     * }</pre>
     *
     * @param field the field to check for raw JSON value annotations
     * @return {@code true} if the field should be serialized as raw JSON, {@code false} otherwise
     */
    static boolean isJsonRawValue(final Field field) {
        boolean isJsonRawValue = false;

        if (field != null && field.isAnnotationPresent(JsonXmlField.class)) {
            isJsonRawValue = field.getAnnotation(JsonXmlField.class).isJsonRawValue();
        }

        if (!isJsonRawValue) {
            try {
                if (field != null && field.isAnnotationPresent(com.fasterxml.jackson.annotation.JsonRawValue.class)
                        && field.getAnnotation(com.fasterxml.jackson.annotation.JsonRawValue.class).value()) {
                    isJsonRawValue = true;
                }
            } catch (final Throwable e) { // NOSONAR
                // ignore.
            }
        }

        if (!isJsonRawValue) {
            try {
                if (field != null && field.isAnnotationPresent(com.alibaba.fastjson2.annotation.JSONField.class)
                        && field.getAnnotation(com.alibaba.fastjson2.annotation.JSONField.class).jsonDirect()) {
                    isJsonRawValue = true;
                }
            } catch (final Throwable e) { // NOSONAR
                // ignore.
            }
        }

        //    if (isJsonRawValue && !CharSequence.class.isAssignableFrom(field.getType())) {
        //        throw new IllegalArgumentException("'isJsonRawValue' can only be applied to CharSequence type field");
        //    }

        return isJsonRawValue;
    }

    /**
     * Generates JSON name tags for all naming policies for a given name.
     *
     * <p>This method creates an array of {@code JsonNameTag} objects, one for each
     * {@code NamingPolicy}, containing the converted name according to that policy.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JsonNameTag[] tags = ParserUtil.getJsonNameTags("firstName");
     * // tags[0].name holds "firstName"  (NamingPolicy.CAMEL_CASE)
     * // tags[1].name holds "FirstName"  (NamingPolicy.UPPER_CAMEL_CASE)
     * // tags[2].name holds "first_name" (NamingPolicy.SNAKE_CASE)
     * }</pre>
     *
     * @param name the original name to convert
     * @return an array of JSON name tags, indexed by {@link NamingPolicy#ordinal()}
     */
    static JsonNameTag[] getJsonNameTags(final String name) {
        final NamingPolicy[] namingPolicies = NAMING_POLICIES;
        final String[] names = new String[namingPolicies.length];

        for (int i = 0, len = namingPolicies.length; i < len; i++) {
            names[i] = convertName(name, namingPolicies[i]);
        }

        return buildJsonNameTags(names);
    }

    /**
     * Generates XML name tags for all naming policies for a given name.
     *
     * <p>This method creates an array of {@code XmlNameTag} objects, one for each
     * {@code NamingPolicy}, containing the converted name, type name, and bean flag.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * XmlNameTag[] tags = ParserUtil.getXmlNameTags("firstName", "string", false);
     * // tags[0].namedStart holds "<firstName>"  (NamingPolicy.CAMEL_CASE)
     * // tags[2].namedStart holds "<first_name>" (NamingPolicy.SNAKE_CASE)
     * }</pre>
     *
     * @param name the original name to convert
     * @param typeName the type name for XML serialization
     * @param isBean whether this represents a bean type
     * @return an array of XML name tags, indexed by {@link NamingPolicy#ordinal()}
     */
    static XmlNameTag[] getXmlNameTags(final String name, final String typeName, final boolean isBean) {
        final NamingPolicy[] namingPolicies = NAMING_POLICIES;
        final String[] names = new String[namingPolicies.length];

        for (int i = 0, len = namingPolicies.length; i < len; i++) {
            names[i] = convertName(name, namingPolicies[i]);
        }

        return buildXmlNameTags(names, typeName, isBean);
    }

    /**
     * Generates JSON name tags for a property field considering custom naming annotations.
     *
     * <p>This method checks for custom field names from various annotations including
     * {@code @JsonXmlField}, {@code @JSONField}, and {@code @JsonProperty}. If a custom
     * name is found, it's used for all naming policies; otherwise, standard conversion applies.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Field field = MyBean.class.getDeclaredField("firstName");
     * JsonNameTag[] tags = ParserUtil.getJsonNameTags("firstName", field);
     * // If field has @JsonProperty("first_name"), all tags will use "first_name"
     * }</pre>
     *
     * @param propName the property name
     * @param field the field to check for naming annotations
     * @return an array of JSON name tags, indexed by {@link NamingPolicy#ordinal()}; every entry holds the
     *         custom name when one is configured, otherwise the policy-converted property name
     * @throws IllegalArgumentException if the custom name contains leading/trailing whitespace.
     */
    static JsonNameTag[] getJsonNameTags(final String propName, final Field field) throws IllegalArgumentException {
        final String jsonXmlFieldName = getCustomFieldName(field);

        final NamingPolicy[] namingPolicies = NAMING_POLICIES;
        final String[] names = new String[namingPolicies.length];
        final boolean hasCustomName = Strings.isNotEmpty(jsonXmlFieldName);

        for (int i = 0, len = namingPolicies.length; i < len; i++) {
            names[i] = hasCustomName ? jsonXmlFieldName : convertName(propName, namingPolicies[i]);
        }

        return buildJsonNameTags(names);
    }

    /**
     * Resolves the custom JSON/XML field name for a property from naming annotations.
     *
     * <p>Checks {@code @JsonXmlField(name=...)}, then {@code @JSONField(name=...)}, then
     * {@code @JsonProperty(value=...)}, returning the first non-empty value found.</p>
     *
     * @param field the field to inspect (may be {@code null})
     * @return the custom field name, or {@code null} if none is specified
     * @throws IllegalArgumentException if the custom name contains leading/trailing whitespace.
     */
    private static String getCustomFieldName(final Field field) throws IllegalArgumentException {
        String jsonXmlFieldName = null;

        if (field != null) {
            if (field.isAnnotationPresent(JsonXmlField.class) && Strings.isNotEmpty(field.getAnnotation(JsonXmlField.class).name())) {
                jsonXmlFieldName = field.getAnnotation(JsonXmlField.class).name();
            } else {
                if (Strings.isEmpty(jsonXmlFieldName)) {
                    try {
                        if (field.isAnnotationPresent(com.alibaba.fastjson2.annotation.JSONField.class)
                                && Strings.isNotEmpty(field.getAnnotation(com.alibaba.fastjson2.annotation.JSONField.class).name())) {
                            jsonXmlFieldName = field.getAnnotation(com.alibaba.fastjson2.annotation.JSONField.class).name();
                        }
                    } catch (final Throwable e) { // NOSONAR
                        // ignore
                    }
                }

                if (Strings.isEmpty(jsonXmlFieldName)) {
                    try {
                        if (field.isAnnotationPresent(com.fasterxml.jackson.annotation.JsonProperty.class)
                                && Strings.isNotEmpty(field.getAnnotation(com.fasterxml.jackson.annotation.JsonProperty.class).value())) {
                            jsonXmlFieldName = field.getAnnotation(com.fasterxml.jackson.annotation.JsonProperty.class).value();
                        }
                    } catch (final Throwable e) { // NOSONAR
                        // ignore
                    }
                }
            }
        }

        if (Strings.isNotEmpty(jsonXmlFieldName) && !jsonXmlFieldName.equals(Strings.strip(jsonXmlFieldName))) {
            throw new IllegalArgumentException(
                    "JsonXmlFieldName name: \"" + jsonXmlFieldName + "\" must not start or end with any whitespace for field: " + field);
        }

        return jsonXmlFieldName;
    }

    /**
     * Generates XML name tags for a property field considering custom naming annotations.
     *
     * <p>Similar to {@link #getJsonNameTags(String, Field)} but includes XML-specific
     * information such as type name and bean flag.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Field field = MyBean.class.getDeclaredField("firstName");
     * XmlNameTag[] tags = ParserUtil.getXmlNameTags("firstName", field, "string", false);
     * }</pre>
     *
     * @param propName the property name
     * @param field the field to check for naming annotations
     * @param typeName the type name for XML serialization
     * @param isBean whether this represents a bean type
     * @return an array of XML name tags, indexed by {@link NamingPolicy#ordinal()}; every entry holds the
     *         custom name when one is configured, otherwise the policy-converted property name
     * @throws IllegalArgumentException if the custom name contains leading/trailing whitespace.
     */
    static XmlNameTag[] getXmlNameTags(final String propName, final Field field, final String typeName, final boolean isBean) throws IllegalArgumentException {
        final String jsonXmlFieldName = getCustomFieldName(field);

        final NamingPolicy[] namingPolicies = NAMING_POLICIES;
        final String[] names = new String[namingPolicies.length];
        final boolean hasCustomName = Strings.isNotEmpty(jsonXmlFieldName);

        for (int i = 0, len = namingPolicies.length; i < len; i++) {
            names[i] = hasCustomName ? jsonXmlFieldName : convertName(propName, namingPolicies[i]);
        }

        return buildXmlNameTags(names, typeName, isBean);
    }

    /**
     * Retrieves the aliases for a field from various annotation sources.
     *
     * <p>This method checks for aliases from the following annotations:</p>
     * <ul>
     *   <li>{@code @JsonXmlField(aliases={...})}</li>
     *   <li>{@code @JSONField(alternateNames={...})}</li>
     *   <li>{@code @JsonAlias(value={...})}</li>
     * </ul>
     *
     * <p>The field's own name is automatically removed from the alias list if present.</p>
     *
     * <p>An alias is an <i>explicit</i> binding, and {@link BeanInfo} registers it before the naming-policy
     * spellings it derives from the property names, in one pass over all properties. So the outcome does not
     * depend on the order the fields are declared in, and an alias wins over another property's derived
     * spelling: {@code @JsonXmlField(aliases = {"first_name"}) String surname} claims {@code "first_name"}
     * even when the bean also declares {@code firstName}, whose SNAKE_CASE spelling that is. An alias that
     * collides with another property's <i>own</i> name, {@code @Column} name or alias is rejected with an
     * {@link IllegalArgumentException} when the {@code BeanInfo} is built, again in every declaration order.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Field field = MyBean.class.getDeclaredField("firstName");
     * String[] aliases = ParserUtil.getAliases(field);
     * // Returns ["first_name", "fname"] if specified in annotations
     * }</pre>
     *
     * @param field the field to check for alias annotations
     * @return an array of aliases, or {@code null} if none is defined. The array is empty when the only
     *         declared alias was the field's own name, which is always removed
     */
    static String[] getAliases(final Field field) {
        String[] alias = null;

        if (field != null) {
            if (field.isAnnotationPresent(JsonXmlField.class) && N.notEmpty(field.getAnnotation(JsonXmlField.class).aliases())) {
                alias = field.getAnnotation(JsonXmlField.class).aliases();
            } else {
                if (N.isEmpty(alias)) {
                    try {
                        if (field.isAnnotationPresent(com.alibaba.fastjson2.annotation.JSONField.class)
                                && N.notEmpty(field.getAnnotation(com.alibaba.fastjson2.annotation.JSONField.class).alternateNames())) {
                            alias = field.getAnnotation(com.alibaba.fastjson2.annotation.JSONField.class).alternateNames();
                        }
                    } catch (final Throwable e) { // NOSONAR
                        // ignore
                    }
                }

                if (N.isEmpty(alias)) {
                    try {
                        if (field.isAnnotationPresent(com.fasterxml.jackson.annotation.JsonAlias.class)
                                && N.notEmpty(field.getAnnotation(com.fasterxml.jackson.annotation.JsonAlias.class).value())) {
                            alias = field.getAnnotation(com.fasterxml.jackson.annotation.JsonAlias.class).value();
                        }
                    } catch (final Throwable e) { // NOSONAR
                        // ignore
                    }
                }
            }
        }

        //noinspection ConstantValue
        if (N.notEmpty(alias) && field != null) {
            alias = N.removeAllOccurrences(alias, field.getName());
        }

        return alias;
    }

    /**
     * Builds one {@link JsonNameTag} per naming policy from the already-resolved per-policy names,
     * sharing a single tag instance whenever two policies resolve to the same name.
     *
     * <p>Many properties resolve to the same name under several policies (e.g. {@code "id"} is
     * identical for camel/snake/kebab/no-change, and an annotation-supplied name is identical for
     * all policies). Since {@link JsonNameTag} is immutable and the returned array is only ever
     * indexed by {@link NamingPolicy#ordinal()} and read, sharing instances is behavior-preserving
     * and avoids allocating duplicate tags and their backing {@code char[]} arrays.</p>
     *
     * @param names the converted name for each naming policy, indexed by ordinal
     * @return the name tags, indexed by naming-policy ordinal
     */
    private static JsonNameTag[] buildJsonNameTags(final String[] names) {
        final int len = names.length;
        final JsonNameTag[] result = new JsonNameTag[len];

        for (int i = 0; i < len; i++) {
            JsonNameTag tag = null;

            for (int j = 0; j < i; j++) {
                if (names[j].equals(names[i])) {
                    tag = result[j];
                    break;
                }
            }

            result[i] = tag != null ? tag : new JsonNameTag(names[i]);
        }

        return result;
    }

    /**
     * Builds one {@link XmlNameTag} per naming policy from the already-resolved per-policy names,
     * sharing a single tag instance whenever two policies resolve to the same name. See
     * {@link #buildJsonNameTags(String[])} for the rationale. Within a single call {@code typeName}
     * and {@code isBean} are constant, so equal names yield identical tags.
     *
     * @param names the converted name for each naming policy, indexed by ordinal
     * @param typeName the type name for XML serialization
     * @param isBean whether this represents a bean type
     * @return the name tags, indexed by naming-policy ordinal
     */
    private static XmlNameTag[] buildXmlNameTags(final String[] names, final String typeName, final boolean isBean) {
        final int len = names.length;
        final XmlNameTag[] result = new XmlNameTag[len];

        for (int i = 0; i < len; i++) {
            XmlNameTag tag = null;

            for (int j = 0; j < i; j++) {
                if (names[j].equals(names[i])) {
                    tag = result[j];
                    break;
                }
            }

            result[i] = tag != null ? tag : new XmlNameTag(names[i], typeName, isBean);
        }

        return result;
    }

    /**
     * Converts a name according to the specified naming policy.
     *
     * <p>Special handling is provided for names starting with underscore when using
     * {@code UPPER_CAMEL_CASE} policy.</p>
     *
     * @param name the name to convert
     * @param namingPolicy the naming policy to apply
     * @return the converted name
     */
    private static String convertName(final String name, final NamingPolicy namingPolicy) {
        return namingPolicy == null || namingPolicy == NamingPolicy.NO_CHANGE || namingPolicy == NamingPolicy.CAMEL_CASE ? name
                : ((namingPolicy == NamingPolicy.UPPER_CAMEL_CASE && name.startsWith("_")) ? "_" + namingPolicy.convert(name.substring(1))
                        : namingPolicy.convert(name));
    }

    /**
     * Calculates the hash code for a character array.
     *
     * <p>The hash code is computed using the standard Java hash algorithm:
     * {@code result = 31 * result + element} for each character.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * char[] chars = "hello".toCharArray();
     * int hash = ParserUtil.hashCode(chars);
     * }</pre>
     *
     * @param a the character array
     * @return the hash code value
     */
    static int hashCode(final char[] a) {
        int result = 1;

        for (final char e : a) {
            result = 31 * result + e;
        }

        return result;
    }

    /**
     * Calculates the hash code for a portion of a character array.
     *
     * <p>This method computes the hash code for the specified range within the array,
     * useful for substring operations without creating new arrays.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * char[] chars = "hello world".toCharArray();
     * int hash = ParserUtil.hashCode(chars, 0, 5);   // hash is computed for "hello"
     * }</pre>
     *
     * @param a the character array
     * @param fromIndex the starting index (inclusive)
     * @param toIndex the ending index (exclusive)
     * @return the hash code value for the specified range
     */
    static int hashCode(final char[] a, final int fromIndex, final int toIndex) {
        return N.hashCode(a, fromIndex, toIndex);
    }

    /**
     * Retrieves or creates a {@link BeanInfo} instance for the specified java type.
     *
     * <p>This method maintains a cache of BeanInfo instances to improve performance.
     * The BeanInfo contains metadata about the class including property information,
     * annotations, and type details. Property types retain resolved arguments from generic
     * superclasses and their parameterized owners, including reordered owner arguments.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BeanInfo beanInfo = ParserUtil.getBeanInfo(MyBean.class);
     * List<PropInfo> properties = beanInfo.propInfoList;
     * }</pre>
     *
     * @param beanType the java type of the bean class to get information for: a {@link Class}, or a
     *        {@link ParameterizedType} whose raw type is a {@code Class}
     * @return a BeanInfo instance containing metadata about the class
     * @throws IllegalArgumentException if {@code beanType} is {@code null}, is neither a {@code Class} nor a
     *         {@code ParameterizedType} with a {@code Class} raw type (for example a {@code TypeVariable},
     *         {@code WildcardType} or {@code GenericArrayType}), or is not a bean class (no properties).
     * @see BeanInfo
     */
    public static BeanInfo getBeanInfo(final java.lang.reflect.Type beanType) throws IllegalArgumentException {
        N.checkArgNotNull(beanType, cs.beanType);

        final Class<?> beanClass;

        if (beanType instanceof ParameterizedType pt && pt.getRawType() instanceof Class cls) {
            beanClass = cls;
        } else if (beanType instanceof Class cls) {
            beanClass = cls;
        } else {
            throw new IllegalArgumentException("Unsupported bean type: " + beanType + " (" + beanType.getClass().getName()
                    + "); expected a Class or a ParameterizedType whose raw type is a Class");
        }

        return getBeanInfo(beanClass, beanType);
    }

    /**
     * Retrieves or creates a {@link BeanInfo} instance for the specified class.
     *
     * <p>This method maintains a cache of BeanInfo instances to improve performance.
     * The BeanInfo contains metadata about the class including property information,
     * annotations, and type details.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BeanInfo beanInfo = ParserUtil.getBeanInfo(MyBean.class);
     * List<PropInfo> properties = beanInfo.propInfoList;
     * }</pre>
     *
     * @param beanClass the class to get bean information for
     * @return a BeanInfo instance containing metadata about the class
     * @throws IllegalArgumentException if the class is not a bean class (no properties).
     * @see BeanInfo
     */
    public static BeanInfo getBeanInfo(final Class<?> beanClass) throws IllegalArgumentException {
        return getBeanInfo(beanClass, beanClass);
    }

    /**
     * @throws IllegalArgumentException if the class has no bean properties or its property metadata contains conflicting aliases, invalid names, or incompatible field exposure
     */
    private static BeanInfo getBeanInfo(final Class<?> beanClass, java.lang.reflect.Type javaType) throws IllegalArgumentException {
        if (!Beans.isBeanClass(beanClass)) {
            throw new IllegalArgumentException(
                    "No property getter/setter method or public field found in the specified bean: " + ClassUtil.getCanonicalClassName(beanClass));
        }

        BeanInfo beanInfo = beanInfoPool.get(javaType);

        if (beanInfo == null) {
            synchronized (beanInfoPool) {
                beanInfo = beanInfoPool.get(javaType);

                if (beanInfo == null) {
                    beanInfo = new BeanInfo(beanClass, javaType);
                    beanInfoPool.put(javaType, beanInfo);
                }
            }
        }

        return beanInfo;
    }

    /**
     * Retrieves or creates a {@link BeanInfo} instance for the specified class,
     * optionally supporting ASM-based property access.
     *
     * <p>This method is similar to {@link #getBeanInfo(java.lang.reflect.Type)} but allows specifying
     * whether ASM support is enabled, which can improve performance for certain operations.</p>
     *
     * <p><b>For internal test only.</b></p>
     *
     * @param beanType the java type of the bean class to get information for
     * @param isASMSupported whether ASM support is enabled
     * @return a BeanInfo instance containing metadata about the class
     * @throws IllegalArgumentException if the class has no bean properties or its property metadata contains conflicting aliases, invalid names, or incompatible field exposure
     * @see BeanInfo
     * @deprecated This overload is for internal tests only. Use {@link #getBeanInfo(java.lang.reflect.Type)} for normal bean metadata lookup.
     */
    @Deprecated
    @Internal
    static BeanInfo getBeanInfo(final java.lang.reflect.Type beanType, final boolean isASMSupported) throws IllegalArgumentException {
        Class<?> beanClass = null;

        if (beanType instanceof ParameterizedType pt && pt.getRawType() instanceof Class cls) {
            beanClass = cls;
        } else {
            beanClass = (Class<?>) beanType;
        }

        return new BeanInfo(beanClass, beanType, isASMSupported);
    }

    /**
     * Refreshes the cached bean property information for the specified class.
     *
     * <p>This method removes the cached BeanInfo for the specified class, forcing
     * it to be recreated on the next call to {@link #getBeanInfo(java.lang.reflect.Type)}.</p>
     *
     * <p>This method is primarily intended for internal framework use and testing scenarios
     * where bean definitions may change at runtime (e.g., through bytecode manipulation or
     * dynamic class reloading).</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BeanInfo first = ParserUtil.getBeanInfo(User.class);
     * BeanInfo cached = ParserUtil.getBeanInfo(User.class);      // same cached instance as first
     *
     * ParserUtil.refreshBeanPropInfo(User.class);                // evicts the cached BeanInfo for User.class
     *
     * BeanInfo recreated = ParserUtil.getBeanInfo(User.class);   // a new BeanInfo instance (not the cached one)
     * }</pre>
     *
     * @param beanType the java type of the bean class to refresh
     * @deprecated This method is for internal use only and should not be called by application code.
     *             Bean property information is automatically cached and refreshed as needed by the framework.
     *             If you encounter stale cached data, consider whether the underlying bean class
     *             definition has been modified at runtime (which is generally not recommended in production).
     *             There is no public replacement as this operation should not be needed in normal usage.
     */
    @Deprecated
    @Internal
    public static void refreshBeanPropInfo(final java.lang.reflect.Type beanType) {
        synchronized (beanInfoPool) {
            beanInfoPool.remove(beanType);
        }
    }

    /**
     * Creates a {@link VarHandle} for direct read/write access to the specified field.
     *
     * <p>A plain lookup is attempted first; if that is rejected, a private lookup in
     * {@code entityClass} is tried. Failure is not fatal - the caller falls back to
     * ordinary reflection - so this method logs at debug level and returns {@code null}
     * instead of throwing.</p>
     *
     * @param entityClass the class used as the host for a private lookup when the plain lookup fails
     * @param field the field to unreflect, may be {@code null}
     * @return a VarHandle for the field, or {@code null} if {@code field} is {@code null} or access was denied
     */
    static VarHandle unreflect(final Class<?> entityClass, final Field field) {
        if (field == null) {
            return null;
        }

        final Lookup lookup = MethodHandles.lookup();

        try {
            return lookup.unreflectVarHandle(field);
        } catch (IllegalAccessException e) {
            try {
                final Lookup privateLookup = MethodHandles.privateLookupIn(entityClass, lookup);

                return privateLookup.unreflectVarHandle(field);
            } catch (IllegalAccessException e2) {
                logger.debug("Failed to unreflect field: {}, error: {}", field, e);
            }
        }

        return null;
    }

    /**
     * Container class holding comprehensive metadata about a bean class.
     *
     * <p>BeanInfo provides access to all property information, annotations, naming policies,
     * and other metadata needed for serialization/deserialization operations.</p>
     *
     * <p>This class implements {@link JsonReader.SymbolReader} for efficient property lookup
     * during JSON parsing.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BeanInfo beanInfo = ParserUtil.getBeanInfo(MyBean.class);
     *
     * // Access property information
     * PropInfo nameProp = beanInfo.getPropInfo("name");
     * Object value = nameProp.getPropValue(myBeanInstance);
     *
     * // Check annotations
     * if (beanInfo.isAnnotationPresent(Entity.class)) {
     *     String tableName = beanInfo.tableName.orElse(beanInfo.simpleClassName);
     * }
     * }</pre>
     *
     * @see PropInfo
     */
    public static class BeanInfo implements JsonReader.SymbolReader {

        /** Type information for this class */
        public final Type<Object> type;

        /** The Java type of the class */
        public final java.lang.reflect.Type javaType;

        /** The class this BeanInfo describes */
        public final Class<Object> clazz;

        /** Simple class name without package */
        public final String simpleClassName;

        /** Fully qualified class name */
        public final String canonicalClassName;

        /** Immutable list of all property names */
        public final ImmutableList<String> propNameList;

        /** Immutable list of all property information */
        public final ImmutableList<PropInfo> propInfoList;

        /** Immutable list of property names marked as IDs */
        public final ImmutableList<String> idPropNameList;

        /** Immutable list of property information for ID properties */
        public final ImmutableList<PropInfo> idPropInfoList;

        /** Immutable list of read-only ID property names */
        public final ImmutableList<String> readOnlyIdPropNameList;

        /** Immutable list of read-only ID property information */
        public final ImmutableList<PropInfo> readOnlyIdPropInfoList;

        /** Immutable list of sub-entity property names */
        public final ImmutableList<String> subEntityPropNameList;

        /** Immutable list of sub-entity property information */
        public final ImmutableList<PropInfo> subEntityPropInfoList;

        /** All annotations present on this class and its superclasses */
        public final ImmutableMap<Class<? extends Annotation>, Annotation> annotations;

        /** Naming policy applied to property names during JSON/XML processing; {@code CAMEL_CASE} when unconfigured. */
        final NamingPolicy jsonXmlNamingPolicy;

        /** Value-exclusion rule applied during serialization; {@code Exclusion.NULL} when unconfigured. */
        final Exclusion jsonXmlSeriExclusion;

        /** The name of {@link #type}, used as the XML {@code type} attribute for this bean. */
        final String typeName;

        /** JSON tags for this bean's own element name, indexed by {@link NamingPolicy#ordinal()}. */
        final JsonNameTag[] jsonNameTags;

        /** XML tags for this bean's own element name, indexed by {@link NamingPolicy#ordinal()}. */
        final XmlNameTag[] xmlNameTags;

        /** All properties of the bean, in declaration order; the array backing {@link #propInfoList}. */
        final PropInfo[] propInfos;

        /** The subset of {@link #propInfos} that is not excluded from JSON/XML serialization. */
        final PropInfo[] jsonXmlSerializablePropInfos;

        /** The serializable properties that are not transient - the ones written by default. */
        final PropInfo[] nonTransientSeriPropInfos;

        /** The serializable properties that are transient - written only when transient output is requested. */
        final PropInfo[] transientSeriPropInfos;

        /** The names of {@link #transientSeriPropInfos}, for fast membership tests. */
        final Set<String> transientSeriPropNameSet = N.newHashSet();

        /**
         * Every spelling that resolves to a property: names, naming-policy tags, column names and aliases
         * (registered at construction, unconditionally) plus fuzzy hits memoized by {@link #getPropInfo(String)}
         * (capped at {@link ParserUtil#MAX_CACHED_PROP_NAMES}). Holds real bindings only; misses live in
         * {@link #missedPropNames} so the fuzzy scan in {@code getPropInfo} never has to walk over them.
         */
        private final Map<String, Optional<PropInfo>> propInfoMap;

        /**
         * Negative cache of {@link #getPropInfo(String)}: names that resolved to nothing. Bounded by
         * {@link ParserUtil#MAX_CACHED_PROP_NAMES} because the keys are caller-supplied (unknown JSON keys).
         */
        private final Set<String> missedPropNames = ConcurrentHashMap.newKeySet();

        private final Map<String, List<PropInfo>> propInfoQueueMap;

        private final PropInfo[] propInfoArray;

        private final Map<Integer, PropInfo> hashPropInfoMap;

        /**
         * {@code true} when some property's naming-policy tag or column name is bound in {@link #propInfoMap}
         * to a DIFFERENT property (e.g. {@code userName} and {@code user_name} both spell {@code USER_NAME}).
         * Lets {@link #readPropInfo(char[], int, int)} skip the exact-binding check for every bean without such
         * a collision.
         */
        private final boolean hasSharedNameTags;

        /** Optional table name if this bean is mapped to a database table */
        public final Optional<String> tableName;

        private final Class<?>[] fieldTypes;
        private final Object[] defaultFieldValues;
        private final Constructor<?> noArgsConstructor;
        private final Constructor<?> allArgsConstructor;

        /** Whether this bean is immutable (e.g., a record or has no setters) */
        public final boolean isImmutable;
        private final boolean isByBuilder;
        private final Beans.BuilderInfo builderInfo;

        /** Whether this class is marked with @Entity or similar annotations */
        public final boolean isMarkedAsBean;

        /**
         * Constructs a new BeanInfo for the specified class.
         *
         * <p>This constructor analyzes the class structure, extracts property information,
         * processes annotations, and builds comprehensive metadata about the bean.</p>
         *
         * @param beanClass the class to analyze
         * @param beanType the Java type of the class
         * @throws IllegalArgumentException if property aliases conflict, a table name has surrounding whitespace, or field exposure conflicts with a transient or non-serializable field
         */
        BeanInfo(final Class<?> beanClass, final java.lang.reflect.Type beanType) throws IllegalArgumentException {
            this(beanClass, beanType, ASMUtil.isASMAvailable());
        }

        /**
         * Constructs a new BeanInfo for the specified class with optional ASM support.
         *
         * <p>When ASM support is enabled, property access may be optimized using bytecode
         * generation instead of reflection.</p>
         *
         * @param beanClass the class to analyze
         * @param beanType the Java type of the class
         * @param isASMSupported whether to enable ASM-based optimizations
         * @throws IllegalArgumentException if property aliases conflict, a table name has surrounding whitespace, or field exposure conflicts with a transient or non-serializable field
         */
        @SuppressWarnings("deprecation")
        BeanInfo(final Class<?> beanClass, final java.lang.reflect.Type beanType, final boolean isASMSupported) throws IllegalArgumentException {
            annotations = ImmutableMap.wrap(getAnnotations(beanClass));
            simpleClassName = ClassUtil.getSimpleClassName(beanClass);
            canonicalClassName = ClassUtil.getCanonicalClassName(beanClass);
            clazz = (Class<Object>) beanClass;
            this.javaType = beanType;
            type = Type.of(beanType);
            typeName = type.name();

            final Map<TypeVariable<?>, java.lang.reflect.Type> typeParamArgMap = new HashMap<>();
            collectTypeArguments(beanType, typeParamArgMap, new HashSet<>());

            propNameList = Beans.getPropNameList(beanClass);

            boolean localIsImmutable = true;

            if (Beans.isRecordClass(beanClass)) {
                //noinspection DataFlowIssue
                localIsImmutable = true;
            } else if (Beans.isRegisteredXmlBindingClass(beanClass)) {
                localIsImmutable = false;
            } else {
                try {
                    final Object tmp = N.newInstance(beanClass);
                    Field field = null;
                    Method setMethod = null;

                    for (final String propName : propNameList) {
                        field = Beans.getPropField(beanClass, propName);
                        setMethod = Beans.getPropSetter(beanClass, propName);

                        if (setMethod != null) {
                            localIsImmutable = false;
                            break;
                        } else if (field != null) {
                            try { //NOSONAR
                                field.set(tmp, N.defaultValueOf(field.getType())); //NOSONAR
                                localIsImmutable = false;

                                break;
                            } catch (final Throwable e) { // NOSONAR
                                // ignore.
                            }
                        }
                    }
                } catch (final Throwable e) { // NOSONAR
                    // ignore.
                }
            }

            isImmutable = localIsImmutable;
            builderInfo = localIsImmutable ? Beans.getBuilderInfo(beanClass) : null;
            isByBuilder = localIsImmutable && builderInfo != null;

            final JsonXmlConfig jsonXmlConfig = (JsonXmlConfig) annotations.get(JsonXmlConfig.class);
            jsonXmlNamingPolicy = jsonXmlConfig == null || jsonXmlConfig.namingPolicy() == null ? NamingPolicy.CAMEL_CASE : jsonXmlConfig.namingPolicy();
            jsonXmlSeriExclusion = jsonXmlConfig == null || jsonXmlConfig.exclusion() == null ? Exclusion.NULL : jsonXmlConfig.exclusion();

            final String name = Beans.normalizePropName(simpleClassName);
            jsonNameTags = getJsonNameTags(name);
            xmlNameTags = getXmlNameTags(name, typeName, true);

            final List<String> idPropNames = new ArrayList<>();
            final List<String> readOnlyIdPropNames = new ArrayList<>();

            if (beanClass.isAnnotationPresent(Id.class)) {
                final String[] values = beanClass.getAnnotation(Id.class).value();
                N.checkArgNotEmpty(values, "values for annotation @Id on Type/Class cannot be null or empty");
                idPropNames.addAll(Arrays.asList(values));
            }

            if (beanClass.isAnnotationPresent(ReadOnlyId.class)) {
                final String[] values = beanClass.getAnnotation(ReadOnlyId.class).value();
                N.checkArgNotEmpty(values, "values for annotation @ReadOnlyId on Type/Class cannot be null or empty");
                idPropNames.addAll(Arrays.asList(values));
                readOnlyIdPropNames.addAll(Arrays.asList(values));
            }

            final List<PropInfo> seriPropInfoList = new ArrayList<>();
            final List<PropInfo> nonTransientSeriPropInfoList = new ArrayList<>();
            final List<PropInfo> transientSeriPropInfoList = new ArrayList<>();

            propInfos = new PropInfo[propNameList.size()];
            propInfoMap = new ConcurrentCacheMap<>((propNameList.size() + 1) * 2);
            propInfoQueueMap = new ConcurrentCacheMap<>((propNameList.size() + 1) * 2);
            hashPropInfoMap = new ConcurrentCacheMap<>((propNameList.size() + 1) * 2);

            PropInfo propInfo = null;
            int idx = 0;

            final Multiset<Integer> multiSet = N.newMultiset(propNameList.size() + 16);
            int maxLength = 0;
            Field field = null;
            Method getMethod = null;
            Method setMethod = null;

            for (final String propName : propNameList) {
                field = Beans.getPropField(beanClass, propName);
                getMethod = Beans.getPropGetter(beanClass, propName);
                setMethod = isByBuilder ? Beans.getPropSetter(builderInfo.builderClass(), propName) : Beans.getPropSetter(beanClass, propName);

                propInfo = ASMUtil.isASMAvailable() && isASMSupported
                        ? new ASMPropInfo(propName, field, getMethod, setMethod, jsonXmlConfig, annotations, idx, isImmutable, isByBuilder, idPropNames,
                                readOnlyIdPropNames, typeParamArgMap)
                        : new PropInfo(propName, field, getMethod, setMethod, jsonXmlConfig, annotations, idx, isImmutable, isByBuilder, idPropNames,
                                readOnlyIdPropNames, typeParamArgMap);

                propInfos[idx++] = propInfo;

                // Pass 1 of the name registration: the property's own name. Column names, aliases and
                // naming-policy tags are registered in the passes after this loop.
                propInfoMap.put(propName, Optional.of(propInfo));

                // These two checks reject a DECLARED direction on a property that cannot honour it; the implicit
                // SERIALIZE_ONLY of a read-only (getter-only) property is not a declaration.
                if (!isJsonXmlSerializable(propInfo.name, propInfo.field, jsonXmlConfig)) {
                    if (propInfo.jsonXmlExpose != JsonXmlField.Direction.BOTH && !propInfo.isReadOnlyProperty) {
                        throw new IllegalArgumentException(
                                "JsonXmlField.Expose cannot be: " + propInfo.jsonXmlExpose + " for non-serializable field: " + propInfo.field);
                    }

                    // skip
                } else {
                    seriPropInfoList.add(propInfo);

                    if (propInfo.isTransient) {
                        if (propInfo.jsonXmlExpose != JsonXmlField.Direction.BOTH && !propInfo.isReadOnlyProperty) {
                            throw new IllegalArgumentException(
                                    "JsonXmlField.Expose cannot be: " + propInfo.jsonXmlExpose + " for transient field: " + propInfo.field);
                        }

                        transientSeriPropNameSet.add(propName);

                        transientSeriPropInfoList.add(propInfo);
                    } else {
                        nonTransientSeriPropInfoList.add(propInfo);
                    }
                }

                multiSet.add(propInfo.jsonNameTags[defaultNameIndex].name.length);
                maxLength = Math.max(propInfo.jsonNameTags[defaultNameIndex].name.length, maxLength);
            }

            // Name registration is done in passes over ALL properties, explicit bindings before derived
            // ones, so the outcome for a bean does not depend on the order its fields are declared in:
            //   pass 1 (above): property names;
            //   pass 2: @Column names (+ ROOT lower/upper case), first declared wins;
            //   pass 3: aliases - an alias may not restate another property's name/column/alias;
            //   pass 4: naming-policy tags, first declared wins, and never over an explicit binding, so an
            //           explicit alias beats another property's derived spelling in every declaration order.
            for (final PropInfo e : propInfos) {
                if (e.columnName.isPresent() && !propInfoMap.containsKey(e.columnName.get())) {
                    final Optional<PropInfo> propInfoOpt = Optional.of(e);
                    propInfoMap.put(e.columnName.get(), propInfoOpt);

                    // Use Locale.ROOT so case-folded lookup keys match across JVM locales.
                    // tr_TR turns "ID".toLowerCase() into "ıd" (dotless i), breaking lookups.
                    final String lower = e.columnName.get().toLowerCase(java.util.Locale.ROOT);
                    if (!propInfoMap.containsKey(lower)) {
                        propInfoMap.put(lower, propInfoOpt);
                    }

                    final String upper = e.columnName.get().toUpperCase(java.util.Locale.ROOT);
                    if (!propInfoMap.containsKey(upper)) {
                        propInfoMap.put(upper, propInfoOpt);
                    }
                }
            }

            for (final PropInfo e : propInfos) {
                if (N.notEmpty(e.aliases)) {
                    for (final String alias : e.aliases) {
                        final Optional<PropInfo> existing = propInfoMap.get(alias);

                        if (existing != null) {
                            // An alias restating the property's own name or column is redundant, not a
                            // collision. Only explicit bindings are registered at this point, so anything
                            // else here really is another property's name, column or alias.
                            if (existing.orElseNull() == e) {
                                continue;
                            }

                            throw new IllegalArgumentException("Cannot set alias: " + alias + " for property/field: " + (e.field == null ? e.name : e.field)
                                    + " because " + alias + " is already the name, column name or alias of property: " + existing.orElseNull().name
                                    + " in class: " + beanClass);
                        }

                        propInfoMap.put(alias, Optional.of(e));
                    }
                }
            }

            for (final PropInfo e : propInfos) {
                final Optional<PropInfo> propInfoOpt = Optional.of(e);
                String jsonTagName = null;

                for (final JsonNameTag nameTag : e.jsonNameTags) {
                    jsonTagName = String.valueOf(nameTag.name);

                    if (!propInfoMap.containsKey(jsonTagName)) {
                        propInfoMap.put(jsonTagName, propInfoOpt);
                    }
                }
            }

            jsonXmlSerializablePropInfos = seriPropInfoList.toArray(new PropInfo[0]);
            nonTransientSeriPropInfos = nonTransientSeriPropInfoList.toArray(new PropInfo[0]);
            transientSeriPropInfos = transientSeriPropInfoList.toArray(new PropInfo[0]);

            propInfoArray = new PropInfo[maxLength + 1];
            boolean sharedNameTags = false;

            for (final PropInfo e : propInfos) {
                hashPropInfoMap.put(ParserUtil.hashCode(e.jsonNameTags[defaultNameIndex].name), e);

                if (multiSet.getCount(e.jsonNameTags[defaultNameIndex].name.length) == 1) {
                    propInfoArray[e.jsonNameTags[defaultNameIndex].name.length] = e;
                }

                // readPropInfo accepts a candidate through any of its tags/column; note whether one of those
                // spellings is bound to another property, so that only such beans pay for the exact check.
                for (final JsonNameTag nameTag : e.jsonNameTags) {
                    sharedNameTags |= propInfoMap.get(String.valueOf(nameTag.name)).orElseNull() != e;
                }

                if (e.columnName.isPresent()) {
                    sharedNameTags |= propInfoMap.get(e.columnName.get()).orElseNull() != e;
                }
            }

            hasSharedNameTags = sharedNameTags;

            propInfoList = ImmutableList.wrap(N.toList(propInfos));

            final List<PropInfo> tmpIdPropInfoList = N.filter(propInfos, it -> it.isMarkedAsId);

            if (N.isEmpty(tmpIdPropInfoList)) {
                tmpIdPropInfoList.addAll(N.filter(propInfos, it -> "id".equals(it.name) && idTypeSet.contains(it.clazz)));
            }

            idPropInfoList = ImmutableList.wrap(tmpIdPropInfoList);
            idPropNameList = ImmutableList.wrap(N.map(idPropInfoList, it -> it.name));

            readOnlyIdPropInfoList = ImmutableList.wrap(N.filter(propInfos, it -> it.isMarkedAsReadOnlyId));
            readOnlyIdPropNameList = ImmutableList.wrap(N.map(readOnlyIdPropInfoList, it -> it.name));

            subEntityPropInfoList = ImmutableList.wrap(N.filter(propInfos, it -> it.isSubEntity));
            subEntityPropNameList = ImmutableList.wrap(N.map(subEntityPropInfoList, it -> it.name));

            String tmpTableName = null;

            if (annotations.containsKey(Table.class)) {
                tmpTableName = ((Table) annotations.get(Table.class)).value();

                if (Strings.isEmpty(tmpTableName)) {
                    tmpTableName = ((Table) annotations.get(Table.class)).name();
                }
            } else {
                try {
                    if (annotations.containsKey(javax.persistence.Table.class)) {
                        tmpTableName = ((javax.persistence.Table) annotations.get(javax.persistence.Table.class)).name();
                    }
                } catch (final Throwable e) { // NOSONAR
                    // ignore
                }

                if (Strings.isEmpty(tmpTableName)) {
                    try {
                        if (annotations.containsKey(jakarta.persistence.Table.class)) {
                            tmpTableName = ((jakarta.persistence.Table) annotations.get(jakarta.persistence.Table.class)).name();
                        }
                    } catch (final Throwable e) { // NOSONAR
                        // ignore
                    }
                }
            }

            if (Strings.isNotEmpty(tmpTableName) && !tmpTableName.equals(Strings.strip(tmpTableName))) {
                throw new IllegalArgumentException("Table name: \"" + tmpTableName + "\" must not start or end with any whitespace in class: " + beanClass);
            }

            tableName = Strings.isEmpty(tmpTableName) ? Optional.empty() : Optional.ofNullable(tmpTableName);

            fieldTypes = new Class[propInfos.length];
            defaultFieldValues = new Object[propInfos.length];

            for (int i = 0, len = propInfos.length; i < len; i++) {
                fieldTypes[i] = propInfos[i].field == null ? propInfos[i].clazz : propInfos[i].field.getType();
                defaultFieldValues[i] = N.defaultValueOf(fieldTypes[i]);
            }

            noArgsConstructor = ClassUtil.getDeclaredConstructor(beanClass);
            allArgsConstructor = ClassUtil.getDeclaredConstructor(beanClass, fieldTypes);

            if (noArgsConstructor != null) {
                ClassUtil.setAccessibleQuietly(noArgsConstructor, true);
            }

            if (allArgsConstructor != null) {
                ClassUtil.setAccessibleQuietly(allArgsConstructor, true);
            }

            boolean tmpIsMarkedToBean = annotations.containsKey(Entity.class);

            if (!tmpIsMarkedToBean) {
                try {
                    tmpIsMarkedToBean = annotations.containsKey(javax.persistence.Entity.class);
                } catch (final Throwable e) { // NOSONAR
                    // ignore
                }
            }

            if (!tmpIsMarkedToBean) {
                try {
                    tmpIsMarkedToBean = annotations.containsKey(jakarta.persistence.Entity.class);
                } catch (final Throwable e) { // NOSONAR
                    // ignore
                }
            }

            isMarkedAsBean = tmpIsMarkedToBean;
        }

        private static void collectTypeArguments(final java.lang.reflect.Type currentType, final Map<TypeVariable<?>, java.lang.reflect.Type> typeParamArgMap,
                final Set<Class<?>> visitedClasses) {
            final Class<?> currentClass;

            if (currentType instanceof ParameterizedType parameterizedType && parameterizedType.getRawType() instanceof Class<?>) {
                currentClass = (Class<?>) parameterizedType.getRawType();

                if (!visitedClasses.add(currentClass)) {
                    return;
                }

                // A non-static member type can use type variables declared by its enclosing
                // class. Collect only the lexical owner's bindings here: the member does not
                // inherit the owner's superclass/interfaces, and visiting that hierarchy would
                // wrongly prevent the member's own (nearer) superclass binding from winning.
                // Resolve all arguments against the inherited bindings before owner variables are rebound.
                // An owner such as Outer<B, A> must not overwrite A/B before either substitution completes.
                final ParameterizedType resolvedType = (ParameterizedType) PropInfo.resolvePropertyType(parameterizedType, typeParamArgMap);
                collectOwnerTypeArguments(resolvedType.getOwnerType(), typeParamArgMap);

                final TypeVariable<?>[] typeParams = currentClass.getTypeParameters();
                final java.lang.reflect.Type[] typeArgs = resolvedType.getActualTypeArguments();
                final Map<TypeVariable<?>, java.lang.reflect.Type> inheritedBindings = new HashMap<>();

                for (int i = 0, len = typeParams.length; i < len; i++) {
                    // Resolve all arguments against the existing lexical bindings before replacing
                    // any of them; inherited self-references and swapped owner variables need parallel substitution.
                    final java.lang.reflect.Type resolved = PropInfo.resolvePropertyType(typeArgs[i], typeParamArgMap);
                    if (!typeParams[i].equals(resolved)) {
                        inheritedBindings.put(typeParams[i], resolved);
                    }
                }
                typeParamArgMap.putAll(inheritedBindings);
            } else if (currentType instanceof Class<?>) {
                currentClass = (Class<?>) currentType;

                if (!visitedClasses.add(currentClass)) {
                    return;
                }
            } else {
                return;
            }

            collectTypeArguments(currentClass.getGenericSuperclass(), typeParamArgMap, visitedClasses);

            for (final java.lang.reflect.Type interfaceType : currentClass.getGenericInterfaces()) {
                collectTypeArguments(interfaceType, typeParamArgMap, visitedClasses);
            }
        }

        private static void collectOwnerTypeArguments(final java.lang.reflect.Type ownerType,
                final Map<TypeVariable<?>, java.lang.reflect.Type> typeParamArgMap) {
            if (!(ownerType instanceof ParameterizedType parameterizedOwner) || !(parameterizedOwner.getRawType() instanceof Class<?> ownerClass)) {
                return;
            }

            collectOwnerTypeArguments(parameterizedOwner.getOwnerType(), typeParamArgMap);

            final TypeVariable<?>[] typeParams = ownerClass.getTypeParameters();
            final java.lang.reflect.Type[] typeArgs = parameterizedOwner.getActualTypeArguments();

            for (int i = 0, len = typeParams.length; i < len; i++) {
                if (!typeParams[i].equals(typeArgs[i])) {
                    typeParamArgMap.put(typeParams[i], typeArgs[i]);
                }
            }
        }

        /**
         * Gets property information by property name.
         *
         * <p>This method resolves a single property and supports various property name formats including:</p>
         * <ul>
         *   <li>Direct property names</li>
         *   <li>Aliases defined via annotations</li>
         *   <li>Column names from database mappings</li>
         *   <li>Case-insensitive and normalized name matching</li>
         * </ul>
         *
         * <p>This method does not resolve nested property paths (dot notation). Use
         * {@link #getPropInfoChain(String)} for nested paths such as {@code "address.city"}.</p>
         *
         * <p>Lookup outcomes are memoized per spelling, hits and misses alike, but only up to a fixed number of
         * caller-supplied names per bean; beyond that the answer is recomputed on every call. This keeps a
         * stream of documents with ever-new unknown keys from growing the bean's caches without bound.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * PropInfo nameInfo = beanInfo.getPropInfo("name");
         * }</pre>
         *
         * @param propName the property name to look up
         * @return the PropInfo for the property, or {@code null} if not found
         * @throws IllegalArgumentException if {@code propName} is {@code null}
         * @see PropInfo
         */
        @Override
        public PropInfo getPropInfo(final String propName) throws IllegalArgumentException {
            Optional<PropInfo> propInfoOpt = propInfoMap.get(propName);

            if (propInfoOpt == null) {
                N.checkArgNotNull(propName, cs.propName);

                if (missedPropNames.contains(propName)) {
                    return null;
                }

                PropInfo propInfo = null;

                final Method method = Beans.getPropGetter(clazz, propName);

                if (method != null) {
                    propInfoOpt = propInfoMap.get(Beans.getPropNameByMethod(method));
                }

                if (propInfoOpt == null) {
                    // propInfoMap holds bindings only (misses are kept apart in missedPropNames), so this scan
                    // is bounded by the bean's own vocabulary plus the capped fuzzy hits, not by the number of
                    // unknown keys ever seen. The isPresent() guard stays as a belt-and-braces check.
                    for (final Map.Entry<String, Optional<PropInfo>> entry : propInfoMap.entrySet()) { //NOSONAR
                        if (entry.getValue().isPresent() && isPropName(clazz, propName, entry.getKey())) {
                            propInfoOpt = entry.getValue();

                            break;
                        }
                    }

                    if ((propInfoOpt == null) && !propName.equalsIgnoreCase(Beans.normalizePropName(propName))) {
                        // The recursive call memoizes its own outcome under the normalized spelling, through
                        // the same bounded caches.
                        propInfo = getPropInfo(Beans.normalizePropName(propName));

                        if (propInfo != null) {
                            propInfoOpt = Optional.of(propInfo);
                        }
                    }
                }

                // Memoize the outcome, but only while the caches are below MAX_CACHED_PROP_NAMES: both keys
                // are caller data (a fuzzy HIT can be spelled in unboundedly many ways too). size() on a
                // ConcurrentHashMap is cheap and a small overshoot under concurrency is harmless.
                if (propInfoOpt == null || !propInfoOpt.isPresent()) {
                    if (missedPropNames.size() < MAX_CACHED_PROP_NAMES) {
                        missedPropNames.add(propName);
                    }

                    return null;
                }

                propInfo = propInfoOpt.orElseThrow();
                hashPropInfoMap.put(ParserUtil.hashCode(propInfo.jsonNameTags[defaultNameIndex].name), propInfo);

                if (propInfoMap.size() < MAX_CACHED_PROP_NAMES) {
                    propInfoMap.put(propName, propInfoOpt);
                }
            }

            return propInfoOpt.orElseNull();
        }

        /**
         * Gets property information using property information from another bean.
         *
         * <p>This method attempts to find a matching property by first checking the
         * property name, then checking any aliases if the direct name match fails.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * BeanInfo sourceBeanInfo = ParserUtil.getBeanInfo(SourceBean.class);
         * BeanInfo targetBeanInfo = ParserUtil.getBeanInfo(TargetBean.class);
         * PropInfo sourceProp = sourceBeanInfo.getPropInfo("firstName");
         * PropInfo targetProp = targetBeanInfo.getPropInfo(sourceProp);
         * }</pre>
         *
         * @param propInfoFromOtherBean property information from another bean
         * @return matching PropInfo in this bean, or {@code null} if no match found
         */
        public PropInfo getPropInfo(final PropInfo propInfoFromOtherBean) {
            if (propInfoFromOtherBean.aliases.isEmpty()) {
                return getPropInfo(propInfoFromOtherBean.name);
            } else {
                PropInfo ret = getPropInfo(propInfoFromOtherBean.name);

                if (ret == null) {
                    for (final String alias : propInfoFromOtherBean.aliases) {
                        ret = getPropInfo(alias);

                        if (ret != null) {
                            break;
                        }
                    }
                }

                return ret;
            }
        }

        /**
         * Gets the value of a property from the specified object.
         *
         * <p>Supports nested property access using dot notation (e.g., "address.street").
         * If any intermediate property in a nested path is {@code null}, returns the default
         * value for the final property type.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Person person = new Person("John", 30);
         * Object value = beanInfo.getPropValue(person, "name");
         * Object nestedValue = beanInfo.getPropValue(person, "address.city");
         * }</pre>
         *
         * @param <T> the expected type of the property value
         * @param obj the object to get the property value from
         * @param propName the property name (supports nested paths)
         * @return the property value, or the type's default value if an intermediate object in a nested path is {@code null}
         * @throws IllegalArgumentException if no getter method is found for the property.
         */
        @SuppressWarnings("unchecked")
        public <T> T getPropValue(final Object obj, final String propName) throws IllegalArgumentException {
            final PropInfo propInfo = getPropInfo(propName);

            if (propInfo == null) {
                final List<PropInfo> propInfoQueue = getPropInfoChain(propName);

                if (propInfoQueue.size() == 0) {
                    throw new IllegalArgumentException("No getter method found with property name: " + propName + " in class: " + clazz.getCanonicalName());
                } else {
                    final int len = propInfoQueue.size();
                    Object propBean = obj;

                    for (final PropInfo info : propInfoQueue) {
                        propBean = info.getPropValue(propBean);

                        if (propBean == null) {
                            return (T) propInfoQueue.get(len - 1).type.defaultValue();
                        }
                    }

                    return (T) propBean;
                }
            } else {
                return propInfo.getPropValue(obj);
            }
        }

        /**
         * Sets the value of a property on the specified object.
         *
         * <p>This method delegates to {@link #setPropValue(Object, String, Object, boolean)}
         * with {@code ignoreUnmatchedProperty} set to {@code false}.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Person person = new Person();
         * beanInfo.setPropValue(person, "name", "John");
         * beanInfo.setPropValue(person, "age", 30);
         * }</pre>
         *
         * @param obj the object to set the property value on
         * @param propName the property name
         * @param propValue the value to set
         * @throws IllegalArgumentException if no setter is found for {@code propName}, or the property is
         *         read-only (a getter with no backing field and no setter).
         */
        public void setPropValue(final Object obj, final String propName, final Object propValue) throws IllegalArgumentException {
            setPropValue(obj, propName, propValue, false);
        }

        /**
         * Sets the value of a property on the specified object with optional unmatched property handling.
         *
         * <p>Supports nested property access using dot notation. For nested properties,
         * intermediate objects are created as needed if they are {@code null}.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Person person = new Person();
         * beanInfo.setPropValue(person, "name", "John", false);
         * beanInfo.setPropValue(person, "address.city", "New York", true);
         * }</pre>
         *
         * @param obj the object to set the property value on
         * @param propName the property name (supports nested paths)
         * @param propValue the value to set
         * @param ignoreUnmatchedProperty if {@code true}, silently ignore properties that don't exist or that are
         *        read-only (a getter with no backing field and no setter)
         * @return {@code true} if the property was set, {@code false} if it was ignored
         * @throws IllegalArgumentException if no setter is found for {@code propName}, or the property is read-only,
         *         and {@code ignoreUnmatchedProperty} is {@code false}.
         */
        @SuppressWarnings("rawtypes")
        public boolean setPropValue(final Object obj, final String propName, final Object propValue, final boolean ignoreUnmatchedProperty)
                throws IllegalArgumentException {
            PropInfo propInfo = getPropInfo(propName);

            if (propInfo != null && propInfo.isReadOnlyProperty) {
                // Nothing can store the value; treat it like an unmatched name rather than failing inside setPropValue.
                if (!ignoreUnmatchedProperty) {
                    throw new IllegalArgumentException("No setter method found with property name: " + propName + " in class: " + clazz.getCanonicalName()
                            + " (the property is read-only: a getter without a backing field or setter)");
                }

                return false;
            }

            if (propInfo == null) {
                final List<PropInfo> propInfoQueue = getPropInfoChain(propName);

                if (propInfoQueue.size() == 0) {
                    if (!ignoreUnmatchedProperty) {
                        throw new IllegalArgumentException("No setter method found with property name: " + propName + " in class: " + clazz.getCanonicalName());
                    } else {
                        return false;
                    }
                } else {
                    Object propBean = obj;
                    Object subPropValue = null;

                    for (int i = 0, len = propInfoQueue.size(); i < len; i++) {
                        propInfo = propInfoQueue.get(i);

                        if (i == (len - 1)) {
                            propInfo.setPropValue(propBean, propValue);
                        } else {
                            subPropValue = propInfo.getPropValue(propBean);

                            if (subPropValue == null) {
                                if (propInfo.type.isCollection()) {
                                    subPropValue = N.newInstance(propInfo.type.elementType().javaType());
                                    final Collection c = N.newCollection((Class) propInfo.type.javaType());
                                    c.add(subPropValue);
                                    propInfo.setPropValue(propBean, c);
                                } else {
                                    // TODO: What about when propInfo.clazz is immutable, such as a record?
                                    // For example: set "account.Name.firstName" key in Beans.mapToBean, if Account.Name is a Record?
                                    subPropValue = N.newInstance(propInfo.clazz);
                                    propInfo.setPropValue(propBean, subPropValue);
                                }
                            } else if (propInfo.type.isCollection()) {
                                final Collection c = (Collection) subPropValue;

                                if (c.size() == 0) {
                                    subPropValue = N.newInstance(propInfo.type.elementType().javaType());
                                    c.add(subPropValue);
                                } else if (propInfo.type.isList()) {
                                    subPropValue = ((List) c).get(0);
                                } else {
                                    subPropValue = N.firstOrNullIfEmpty(c);
                                }
                            }

                            propBean = subPropValue;
                        }
                    }
                }
            } else {
                propInfo.setPropValue(obj, propValue);
            }

            return true;
        }

        /**
         * Sets a property value using property information from another bean.
         *
         * <p>This method delegates to {@link #setPropValue(Object, PropInfo, Object, boolean)}
         * with {@code ignoreUnmatchedProperty} set to {@code false}.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Person source = new Person("John", 30);
         * Person target = new Person();
         * PropInfo sourceProp = sourceBeanInfo.getPropInfo("name");
         * targetBeanInfo.setPropValue(target, sourceProp, source.getName());
         * }</pre>
         *
         * @param obj the object to set the property value on
         * @param propInfoFromOtherBean property information from another bean
         * @param propValue the value to set
         * @throws IllegalArgumentException if no writable property is found for the name or any of its aliases (a
         *         read-only property - a getter with no backing field and no setter - counts as no match).
         */
        public void setPropValue(final Object obj, final PropInfo propInfoFromOtherBean, final Object propValue) throws IllegalArgumentException {
            setPropValue(obj, propInfoFromOtherBean, propValue, false);
        }

        /**
         * Sets a property value using property information from another bean with optional unmatched property handling.
         *
         * <p>This method attempts to match properties by name and aliases defined in the
         * property information from the other bean.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Person source = new Person("John", 30);
         * Person target = new Person();
         * PropInfo sourceProp = sourceBeanInfo.getPropInfo("name");
         * boolean wasSet = targetBeanInfo.setPropValue(target, sourceProp, source.getName(), true);
         * }</pre>
         *
         * @param obj the object to set the property value on
         * @param propInfoFromOtherBean property information from another bean
         * @param propValue the value to set
         * @param ignoreUnmatchedProperty if {@code true}, silently ignore properties that don't exist or that are
         *        read-only (a getter with no backing field and no setter)
         * @return {@code true} if the property was set, {@code false} if it was ignored
         * @throws IllegalArgumentException if no writable property is found for the name or any of its aliases and
         *         {@code ignoreUnmatchedProperty} is {@code false}.
         */
        public boolean setPropValue(final Object obj, final PropInfo propInfoFromOtherBean, final Object propValue, final boolean ignoreUnmatchedProperty)
                throws IllegalArgumentException {
            if (propInfoFromOtherBean.aliases.isEmpty()) {
                return setPropValue(obj, propInfoFromOtherBean.name, propValue, ignoreUnmatchedProperty);
            } else {
                if (setPropValue(obj, propInfoFromOtherBean.name, propValue, true)) {
                    return true;
                }

                for (final String alias : propInfoFromOtherBean.aliases) {
                    if (setPropValue(obj, alias, propValue, true)) {
                        return true;
                    }
                }

                if (!ignoreUnmatchedProperty) {
                    throw new IllegalArgumentException(
                            "No setter method found with property name: " + propInfoFromOtherBean.name + " in class: " + clazz.getCanonicalName());
                }

                return false;
            }
        }

        /**
         * Checks if the given input property name matches a method-based property name.
         *
         * <p>This method handles various naming conventions and patterns including:</p>
         * <ul>
         *   <li>Case-insensitive matching</li>
         *   <li>Underscore removal</li>
         *   <li>Class name prefixing</li>
         *   <li>Getter/setter method prefixes (get, set, is, has)</li>
         * </ul>
         *
         * @param cls the class containing the property
         * @param inputPropName the input property name to match
         * @param propNameByMethod the actual property name derived from a method
         * @return {@code true} if the names match according to any supported pattern
         */
        private boolean isPropName(final Class<?> cls, String inputPropName, final String propNameByMethod) {
            // A copy of Beans.isPropName (private there, different package here). Keep the two in sync - they
            // diverged on exactly this length check once, with Beans' copy throwing where this one returns
            // false, which turned every "ignore unmatched property" lookup into a hard failure.
            // Trim before measuring: the cap is about the length of the name, not of the caller's padding.
            inputPropName = inputPropName.trim();

            if (inputPropName.length() > 128) {
                return false;
            }

            return inputPropName.equalsIgnoreCase(propNameByMethod) || inputPropName.replace(SK.UNDERSCORE, Strings.EMPTY).equalsIgnoreCase(propNameByMethod)
                    || inputPropName.equalsIgnoreCase(ClassUtil.getSimpleClassName(cls) + SK._PERIOD + propNameByMethod)
                    || (inputPropName.startsWith(GET) && inputPropName.length() > 3 && inputPropName.substring(3).equalsIgnoreCase(propNameByMethod))
                    || (inputPropName.startsWith(SET) && inputPropName.length() > 3 && inputPropName.substring(3).equalsIgnoreCase(propNameByMethod))
                    || (inputPropName.startsWith(IS) && inputPropName.length() > 2 && inputPropName.substring(2).equalsIgnoreCase(propNameByMethod))
                    || (inputPropName.startsWith(HAS) && inputPropName.length() > 3 && inputPropName.substring(3).equalsIgnoreCase(propNameByMethod));
        }

        /**
         * Gets a queue of property information for nested property paths.
         *
         * <p>This method parses dot-separated property paths and returns a list of
         * PropInfo objects representing each level of the path.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * List<PropInfo> queue = beanInfo.getPropInfoChain("address.street.name");
         * // Returns [PropInfo(address), PropInfo(street), PropInfo(name)]
         * }</pre>
         *
         * @param propName the property path (e.g., "address.street")
         * @return an immutable list holding one PropInfo per path segment; an empty list if {@code propName}
         *         contains no {@code '.'} separator, or if any segment cannot be resolved
         * @throws IllegalArgumentException if {@code propName} is {@code null}
         */
        public List<PropInfo> getPropInfoChain(final String propName) throws IllegalArgumentException {
            N.checkArgNotNull(propName, cs.propName);

            List<PropInfo> propInfoQueue = propInfoQueueMap.get(propName);

            if (propInfoQueue == null) {
                // A name without a separator can never be a chain. Answer without caching: the tolerant map/bean
                // paths ask for every unmatched flat key, and each one would otherwise pin an empty list here.
                if (propName.indexOf(PROP_NAME_SEPARATOR) < 0) {
                    return N.emptyList();
                }

                propInfoQueue = new ArrayList<>();

                final String[] strs = PROP_NAME_SPLITTER.splitToArray(propName);

                if (strs.length > 1) {
                    Class<?> propClass = clazz;
                    BeanInfo propBeanInfo = null;

                    PropInfo propInfo = null;

                    for (int i = 0, len = strs.length; i < len; i++) {
                        propBeanInfo = Beans.isBeanClass(propClass) ? ParserUtil.getBeanInfo(propClass) : null;
                        propInfo = propBeanInfo == null ? null : propBeanInfo.getPropInfo(strs[i]);

                        if (propInfo == null) {
                            if (i == 0) {
                                return N.emptyList(); // return directly because the first part is not valid property/field name of the target bean class.
                            }

                            propInfoQueue.clear();
                            break;
                        }

                        propInfoQueue.add(propInfo);

                        if (propInfo.type.isCollection()) {
                            propClass = propInfo.type.elementType().javaType();
                        } else {
                            propClass = propInfo.clazz;
                        }
                    }
                }

                propInfoQueue = N.isEmpty(propInfoQueue) ? N.emptyList() : ImmutableList.wrap(propInfoQueue);

                // Keys are caller-supplied paths; cap the memoization like getPropInfo does.
                if (propInfoQueueMap.size() < MAX_CACHED_PROP_NAMES) {
                    propInfoQueueMap.put(propName, propInfoQueue);
                }
            }

            return propInfoQueue;
        }

        /**
         * Reads property information from a character buffer for efficient parsing.
         *
         * <p>This method is used internally by parsers for fast property lookup
         * during deserialization. It uses hash-based lookup for optimal performance.</p>
         *
         * <p>The answer is the same one {@link #getPropInfo(String)} gives for the exact text: when two
         * properties share a naming-policy spelling or column name (for example {@code userName} and
         * {@code user_name} both spell {@code USER_NAME}), the explicit binding - {@code @Column}, alias, then
         * declaration order - wins, whichever property the length/hash shortcut proposed.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * char[] buffer = "name".toCharArray();
         * PropInfo propInfo = beanInfo.readPropInfo(buffer, 0, buffer.length);
         * }</pre>
         *
         * @param cbuf the character buffer containing the property name
         * @param fromIndex the starting index in the buffer
         * @param toIndex the ending index in the buffer
         * @return the PropInfo if found, {@code null} otherwise
         */
        @Override
        public PropInfo readPropInfo(final char[] cbuf, final int fromIndex, final int toIndex) {
            final int len = toIndex - fromIndex;

            if (len == 0) {
                return null;
            }

            PropInfo propInfo = null;

            if (len < propInfoArray.length) {
                propInfo = propInfoArray[len];
            }

            if (propInfo == null) {
                propInfo = hashPropInfoMap.get(ParserUtil.hashCode(cbuf, fromIndex, toIndex));
            }

            if (propInfo != null && !matches(cbuf, fromIndex, len, propInfo.name)) {
                if (!matchesNameTagOrColumn(propInfo, cbuf, fromIndex, len)) {
                    propInfo = null;
                } else if (hasSharedNameTags) {
                    // The shortcut candidate matched only through a derived spelling, and in this bean some
                    // derived spelling belongs to another property: let the exact binding decide, so that the
                    // JSON path routes the key exactly like getPropInfo (map/XML/Avro) does. Beans without such
                    // a collision, and every exact-name key, never reach this allocation.
                    final Optional<PropInfo> exact = propInfoMap.get(new String(cbuf, fromIndex, len));

                    if (exact != null && exact.isPresent()) {
                        propInfo = exact.orElseThrow();
                    }
                }
            }

            if (propInfo == null) {
                final Optional<PropInfo> propInfoOpt = propInfoMap.get(new String(cbuf, fromIndex, len));
                propInfo = propInfoOpt == null ? null : propInfoOpt.orElse(null);
            }

            return propInfo;
        }

        private boolean matchesNameTagOrColumn(final PropInfo propInfo, final char[] cbuf, final int fromIndex, final int len) {
            for (final JsonNameTag nameTag : propInfo.jsonNameTags) {
                if (matches(cbuf, fromIndex, len, nameTag.name)) {
                    return true;
                }
            }

            if (propInfo.columnName.isPresent()) {
                return matches(cbuf, fromIndex, len, propInfo.columnName.get());
            }

            return false;
        }

        private boolean matches(final char[] cbuf, final int fromIndex, final int len, final String propName) {
            if (propName == null || propName.length() != len) {
                return false;
            }

            for (int i = 0; i < len; i++) {
                if (cbuf[fromIndex + i] != propName.charAt(i)) {
                    return false;
                }
            }

            return true;
        }

        private boolean matches(final char[] cbuf, final int fromIndex, final int len, final char[] propName) {
            if (propName == null || propName.length != len) {
                return false;
            }

            for (int i = 0; i < len; i++) {
                if (cbuf[fromIndex + i] != propName[i]) {
                    return false;
                }
            }

            return true;
        }

        /**
         * Checks if this class has the specified annotation.
         *
         * <p>This method checks for annotations on the class and all its superclasses.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * if (beanInfo.isAnnotationPresent(Entity.class)) {
         *     // Process as entity
         * }
         * }</pre>
         *
         * @param annotationClass the annotation class to check for
         * @return {@code true} if the annotation is present, {@code false} otherwise
         */
        public boolean isAnnotationPresent(final Class<? extends Annotation> annotationClass) {
            return annotations.containsKey(annotationClass);
        }

        /**
         * Gets the specified annotation from this class.
         *
         * <p>This method returns annotations from the class or any of its superclasses.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Table tableAnnotation = beanInfo.getAnnotation(Table.class);
         * if (tableAnnotation != null) {
         *     String tableName = tableAnnotation.value();
         * }
         * }</pre>
         *
         * @param <T> the annotation type
         * @param annotationClass the annotation class to retrieve
         * @return the annotation instance, or {@code null} if not present
         */
        public <T extends Annotation> T getAnnotation(final Class<T> annotationClass) {
            return (T) annotations.get(annotationClass);
        }

        /**
         * Collects all annotations from the class hierarchy.
         *
         * <p>This method traverses the class hierarchy from superclasses to the target class,
         * collecting all annotations. Annotations on subclasses override those on superclasses.</p>
         *
         * @param cls the class to collect annotations from
         * @return a map of annotation types to annotation instances
         */
        private Map<Class<? extends Annotation>, Annotation> getAnnotations(final Class<?> cls) {
            final Map<Class<? extends Annotation>, Annotation> annos = new HashMap<>();

            final Set<Class<?>> classes = ClassUtil.getAllSuperTypes(cls);
            N.reverse(classes);
            classes.add(cls);

            for (final Class<?> e : classes) {
                if (N.notEmpty(e.getAnnotations())) {
                    for (final Annotation anno : e.getAnnotations()) {
                        annos.put(anno.annotationType(), anno);
                    }
                }
            }

            return annos;
        }

        /**
         * Creates a new instance of this bean class using the no-args constructor.
         *
         * <p>If no no-args constructor is available, the all-args constructor is invoked
         * with default values for every argument.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * BeanInfo beanInfo = ParserUtil.getBeanInfo(Person.class);
         * Person person = beanInfo.newInstance();
         * }</pre>
         *
         * @param <T> the type of the instance
         * @return a new instance of the bean class
         * @throws RuntimeException if instantiation fails
         */
        @Beta
        <T> T newInstance() throws RuntimeException {
            return (T) (noArgsConstructor == null ? ClassUtil.invokeConstructor(allArgsConstructor, defaultFieldValues)
                    : ClassUtil.invokeConstructor(noArgsConstructor));
        }

        /**
         * Creates a new instance of this bean class using the specified arguments.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * BeanInfo beanInfo = ParserUtil.getBeanInfo(Person.class);
         * Person person = beanInfo.newInstance("John", 30);
         * }</pre>
         *
         * @param <T> the type of the instance
         * @param args constructor arguments
         * @return a new instance of the bean class
         * @throws RuntimeException if instantiation fails
         */
        @Beta
        <T> T newInstance(final Object... args) throws RuntimeException {
            if (N.isEmpty(args)) {
                return newInstance();
            }

            return (T) ClassUtil.invokeConstructor(allArgsConstructor, args);
        }

        /**
         * Creates an intermediate result object for bean construction.
         *
         * <p>For immutable beans, this returns either a builder instance or an array
         * to collect constructor arguments. For mutable beans, returns a new instance.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Object result = beanInfo.createBeanResult();
         * // For immutable beans: returns builder or args array
         * // For mutable beans: returns new instance
         * }</pre>
         *
         * @return an intermediate object for bean construction
         */
        @Beta
        public Object createBeanResult() {
            return isImmutable ? (builderInfo != null ? builderInfo.newBuilder() : createArgsForConstructor()) : N.newInstance(clazz);
        }

        /**
         * Finalizes bean construction from an intermediate result object.
         *
         * <p>For immutable beans with builders, calls the build method. For immutable
         * beans without builders, calls the all-args constructor. For mutable beans,
         * returns the object as-is.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Object intermediate = beanInfo.createBeanResult();
         * beanInfo.setPropValue(intermediate, "name", "Ada");
         * Person person = beanInfo.finishBeanResult(intermediate);
         * }</pre>
         *
         * @param <T> the type of the finished bean
         * @param result the intermediate result from createBeanResult
         * @return the finished bean instance
         */
        @Beta
        public <T> T finishBeanResult(final Object result) {
            if (result == null) {
                return null;
            }

            return isImmutable ? (builderInfo != null ? (T) builderInfo.build(result) : newInstance(((Object[]) result))) : (T) result;
        }

        /**
         * Creates an array of default values for all constructor arguments.
         *
         * <p>This method is used internally when constructing immutable beans without builders.</p>
         *
         * @return an array of default values matching the all-args constructor
         * @throws UnsupportedOperationException if no all-args constructor exists
         */
        private Object[] createArgsForConstructor() throws UnsupportedOperationException {
            if (allArgsConstructor == null) {
                throw new UnsupportedOperationException("No all arguments constructor found in class: " + ClassUtil.getCanonicalClassName(clazz));
            }

            return defaultFieldValues.clone();
        }

        /**
         * Computes the hash code for this BeanInfo based on the class.
         *
         * @return the hash code value
         */
        @Override
        public int hashCode() {
            return (clazz == null) ? 0 : clazz.hashCode();
        }

        /**
         * Checks if this BeanInfo equals another object.
         *
         * <p>Two BeanInfo instances are considered equal if they represent the same class.</p>
         *
         * @param obj the object to compare with
         * @return {@code true} if the objects are equal
         */
        @Override
        public boolean equals(final Object obj) {
            return this == obj || (obj instanceof BeanInfo && N.equals(((BeanInfo) obj).clazz, clazz));
        }

        /**
         * Returns the canonical class name of the bean type this {@code BeanInfo} describes.
         *
         * <p>Useful for logging and debugging; the value is the same as
         * {@link ClassUtil#getCanonicalClassName(Class)} applied to the represented class.</p>
         *
         * @return the canonical name of the bean class; never {@code null} for a valid {@code BeanInfo}
         */
        @Override
        public String toString() {
            return ClassUtil.getCanonicalClassName(clazz);
        }
    }

    /**
     * Represents metadata and runtime information about a property (field or getter/setter pair) in a Java class.
     *
     * <p>This class encapsulates all the information needed to access and manipulate a property at runtime,
     * including reflection metadata, type information, formatting rules, and database mapping details.
     * It serves as a central hub for property introspection and manipulation in serialization/deserialization
     * and ORM contexts.</p>
     *
     * <p>Key features:</p>
     * <ul>
     *   <li>Unified access to properties via fields or methods</li>
     *   <li>Support for various date/time and number formatting options</li>
     *   <li>JSON/XML serialization configuration</li>
     *   <li>Database column mapping information</li>
     *   <li>Performance optimizations for property access</li>
     *   <li>Support for immutable beans and builder patterns</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * class Person {
     *     public String name;
     * }
     * Person person = new Person();
     * person.name = "old value";
     * PropInfo propInfo = ParserUtil.getBeanInfo(Person.class).getPropInfo("name");
     * Object value = propInfo.getPropValue(person); // returns "old value"
     * propInfo.setPropValue(person, "new value");
     * }</pre>
     *
     * @see BeanInfo
     * @see Type
     * @see JsonXmlField
     */
    public static class PropInfo {
        private static final Pattern JSON_NUMBER_TOKEN = Pattern.compile("-?(?:0|[1-9][0-9]*)(?:\\.[0-9]+)?(?:[eE][+-]?[0-9]+)?");

        /**
         * The class that declares this property.
         * This is the class where the field or getter method is defined.
         */
        public final Class<Object> declaringClass;

        /**
         * The name of the property.
         * This is typically the field name or the property name derived from getter/setter methods.
         */
        public final String name;

        /**
         * Immutable list of alternative names (aliases) for this property.
         * These can be used during deserialization to map different input names to this property.
         */
        public final ImmutableList<String> aliases;

        /**
         * The Type object representing this property's type.
         * This includes generic type information for parameterized types.
         */
        public final Type<Object> type;

        /**
         * The Type object specifically for JSON/XML serialization.
         * May differ from the general type if custom type mappings are specified.
         */
        public final Type<Object> jsonXmlType;

        /**
         * The Type object specifically for database operations.
         * May differ from the general type if custom database type mappings are specified.
         */
        public final Type<Object> dbType;

        /**
         * The Java class type of this property.
         * For collections, this is the collection type, not the element type.
         */
        public final Class<Object> clazz;

        /**
         * Immutable map of all annotations present on this property.
         * Includes annotations from the field, getter, and setter.
         */
        public final ImmutableMap<Class<? extends Annotation>, Annotation> annotations;

        /**
         * The Field object for direct field access, or {@code null} if property is only accessible via methods.
         */
        public final Field field;

        /**
         * The getter method for this property, or {@code null} if the property is write-only.
         */
        public final Method getMethod;

        /**
         * The setter method for this property, or {@code null} if the property is read-only.
         */
        public final Method setMethod;

        final VarHandle fieldHandle;

        //    final MethodHandle getMethodHandle;
        //    final MethodHandle setMethodHandle;

        /**
         * Indicates whether the field is accessible for direct read access.
         * True if the field exists, is accessible, and not marked for method-only access.
         */
        final boolean isFieldGettable;

        /**
         * Indicates whether the field is settable directly.
         * True if the field is accessible and not final (and not in a builder pattern).
         */
        final boolean isFieldSettable;

        final boolean isFieldHandleGettable;
        final boolean isFieldHandleSettable;

        //    final boolean isMethodHandleGettable;
        //    final boolean isMethodHandleSettable;

        /**
         * Array of JSON name tags for custom JSON field naming.
         */
        final JsonNameTag[] jsonNameTags;

        /**
         * Array of XML name tags for custom XML element/attribute naming.
         */
        final XmlNameTag[] xmlNameTags;

        /**
         * The date format pattern for formatting/parsing date values.
         * Null if no specific format is specified.
         */
        final String dateFormat;

        /**
         * The timezone to use for date/time operations.
         * Defaults to system timezone if not specified.
         */
        final TimeZone timeZone;

        /**
         * The ZoneId corresponding to the timezone.
         * Used for Java 8+ time API operations.
         */
        final ZoneId zoneId;

        /**
         * Pre-compiled DateTimeFormatter for Java 8+ date/time types.
         * Null if no date format is specified, or if the format is the {@code "long"} epoch-millis marker.
         */
        final DateTimeFormatter dateTimeFormatter;

        /**
         * Indicates whether this property should be serialized as raw JSON value.
         * When {@code true}, the value is written directly without quotes or escaping.
         */
        final boolean isJsonRawValue;

        /**
         * Holder for Joda-Time formatter, if Joda-Time is available.
         * Null if no date format is specified or Joda-Time is not on the classpath.
         */
        final JodaDateTimeFormatterHolder jodaDTFH;

        /**
         * Indicates whether the date format is "long" (epoch milliseconds).
         */
        final boolean isLongDateFormat;

        /**
         * Prototype number format for formatting/parsing numeric values.
         * Null if no specific format is specified. Not used concurrently — callers must use
         * {@link #numberFormatTL} (or clone) because {@link NumberFormat} is not thread-safe.
         */
        final NumberFormat numberFormat;

        /**
         * Per-thread clones of {@link #numberFormat} to avoid synchronized parse/format on the hot path.
         * Null when {@link #numberFormat} is {@code null}.
         */
        private final ThreadLocal<NumberFormat> numberFormatTL;

        /**
         * The (boxed) property class a number-format read must hand back; {@code null} when
         * {@link #numberFormat} is {@code null}. Boxed so a {@code Double} parsed for a primitive
         * {@code double} property is recognized as already having the right type.
         */
        private final Class<?> numberTargetClass;

        /**
         * Indicates whether any formatting (date or number) is specified for this property.
         */
        final boolean hasFormat;

        /**
         * Indicates whether this property is transient and should be skipped during serialization.
         * True if marked with @Transient annotation or has the transient modifier.
         */
        public final boolean isTransient;

        /**
         * The JSON/XML exposure setting for this property.
         * Controls when this property should be included in serialization/deserialization.
         *
         * <p>Taken from {@code @JsonXmlField(direction = ...)} when declared; otherwise {@link Direction#BOTH},
         * except for a read-only property ({@link #isReadOnlyProperty}), which is {@link Direction#SERIALIZE_ONLY}
         * so that a document carrying its value is read without error and the value is skipped.</p>
         */
        public final Direction jsonXmlExpose;

        /**
         * Indicates whether this property is marked as an identifier (primary key).
         * True if annotated with @Id or listed in id property names.
         */
        public final boolean isMarkedAsId;

        /**
         * Indicates whether this property is marked as a read-only identifier.
         * True if annotated with @ReadOnlyId, is an @Id with @ReadOnly, or listed in read-only id property names.
         */
        public final boolean isMarkedAsReadOnlyId;

        /**
         * Indicates whether this property is explicitly marked as a database column.
         * True if annotated with @Column (from any supported persistence API).
         */
        public final boolean isMarkedAsColumn;

        /**
         * Indicates whether this property represents a sub-entity relationship.
         * True if the type is a bean or collection of beans and not marked as a column.
         */
        public final boolean isSubEntity;

        /**
         * The database column name for this property.
         * Empty if no custom column name is specified.
         */
        public final Optional<String> columnName;

        /**
         * The table alias prefix for entity properties.
         * Used in SQL generation for disambiguating columns from different tables.
         */
        public final Optional<String> tablePrefix;

        /**
         * Indicates whether this property can be set through its getter method.
         * True for certain collection/map properties in XML binding contexts.
         */
        final boolean canSetFieldByGetMethod;

        /**
         * {@code true} when nothing can store a value into this property: it has a getter but no backing
         * field, no setter and no getter-returned collection to fill (a computed property such as
         * {@code getFullName()}), and it is not a component of an immutable bean assembled through a
         * constructor or builder. Such a property is exposed as {@link Direction#SERIALIZE_ONLY}, so the parsers
         * skip its value on read instead of failing, and {@link #setPropValue(Object, Object)} rejects it.
         */
        final boolean isReadOnlyProperty;

        /**
         * The ordinal position of this field in the bean.
         * Used for array-based storage in immutable beans.
         */
        final int fieldOrder;

        /**
         * Indicates whether this property belongs to an immutable bean.
         */
        final boolean isImmutableBean;

        /**
         * Indicates whether this property is set via a builder pattern.
         */
        final boolean isByBuilder;

        /**
         * Counter for tracking repeated failures in setting property values.
         * Used for performance optimization to avoid repeated conversion attempts.
         */
        volatile int failureCountForSetProp = 0;

        /**
         * Constructs a new PropInfo instance with complete metadata about a property.
         *
         * <p>This constructor performs extensive initialization including:</p>
         * <ul>
         *   <li>Annotation processing from field and methods</li>
         *   <li>Type resolution with generic information</li>
         *   <li>Format configuration (date/number)</li>
         *   <li>Database mapping detection</li>
         *   <li>Accessibility configuration</li>
         * </ul>
         *
         * @param propName the name of the property
         * @param field the field object (may be {@code null} for method-only properties)
         * @param getMethod the getter method (may be {@code null} for write-only properties)
         * @param setMethod the setter method (may be {@code null} for read-only properties)
         * @param jsonXmlConfig configuration for JSON/XML processing
         * @param classAnnotations annotations from the declaring class
         * @param fieldOrder the ordinal position of this field
         * @param isImmutableBean whether this property belongs to an immutable bean
         * @param isByBuilder whether this property uses builder pattern
         * @param idPropNames list of property names that are identifiers
         * @param readOnlyIdPropNames list of property names that are read-only identifiers
         * @param typeParamArgMap mapping of type variables to actual types for generic resolution
         * @throws UnsupportedOperationException if the long date format is selected for LocalDate or LocalTime
         * @throws IllegalArgumentException if a column name has surrounding whitespace
         */
        @SuppressWarnings("deprecation")
        PropInfo(final String propName, final Field field, final Method getMethod, final Method setMethod, final JsonXmlConfig jsonXmlConfig,
                final ImmutableMap<Class<? extends Annotation>, Annotation> classAnnotations, final int fieldOrder, final boolean isImmutableBean,
                final boolean isByBuilder, final List<String> idPropNames, final List<String> readOnlyIdPropNames,
                final Map<TypeVariable<?>, java.lang.reflect.Type> typeParamArgMap) throws UnsupportedOperationException, IllegalArgumentException {
            declaringClass = (Class<Object>) (field != null ? field.getDeclaringClass() : getMethod.getDeclaringClass());
            this.field = field;
            this.name = propName;
            this.aliases = ImmutableList.wrap(N.toList(getAliases(field)));

            this.getMethod = getMethod;
            this.setMethod = setMethod;
            annotations = ImmutableMap.wrap(getAnnotations());

            this.fieldHandle = unreflect(declaringClass, field);
            //    this.getMethodHandle = unreflect(declaringClass, getMethod);
            //    this.setMethodHandle = unreflect(declaringClass, setMethod);

            final boolean isAccessFieldByMethod = annotations.containsKey(AccessFieldByMethod.class) || classAnnotations.containsKey(AccessFieldByMethod.class);

            if (field != null && !isAccessFieldByMethod) {
                ClassUtil.setAccessibleQuietly(field, true);
            }

            this.isFieldGettable = field != null && !isAccessFieldByMethod && field.isAccessible();
            this.isFieldSettable = field != null && isFieldGettable && !Modifier.isFinal(field.getModifiers()) && !isByBuilder;

            // Must honor @AccessFieldByMethod just like isFieldGettable/isFieldSettable above (and like the ASM
            // path, where fieldAccessIndex is -1 when !isFieldGettable): otherwise getPropValue/setPropValue would
            // prefer the VarHandle and read/write the field directly, bypassing the mandated getter/setter.
            // isFieldHandleSettable must also exclude isByBuilder just like isFieldSettable: setPropValue's target for
            // a builder-based bean is the BUILDER instance, while the VarHandle's coordinate is the BEAN, so the retry
            // branch that prefers the VarHandle threw "ClassCastException: Cannot cast Bean$Builder to Bean" for a
            // non-final field. The Gettable twin must NOT exclude it - getPropValue's target is the finished BEAN,
            // which is how serialization reads a builder-based bean.
            this.isFieldHandleGettable = !isAccessFieldByMethod && fieldHandle != null && fieldHandle.isAccessModeSupported(AccessMode.GET);
            this.isFieldHandleSettable = !isAccessFieldByMethod && fieldHandle != null && fieldHandle.isAccessModeSupported(AccessMode.SET) && !isByBuilder;

            isTransient = annotations.containsKey(Transient.class) || annotations.keySet().stream().anyMatch(it -> it.getSimpleName().equals("Transient"))
                    || (field != null && Modifier.isTransient(field.getModifiers()));

            final Class<?> propClass = field == null ? (setMethod == null ? getMethod.getReturnType() : setMethod.getParameterTypes()[0]) : field.getType();

            type = getType(getAnnoType(this.field, propClass, jsonXmlConfig), this.field, this.getMethod, this.setMethod, declaringClass, typeParamArgMap);

            jsonXmlType = getType(getJsonXmlAnnoType(this.field, propClass, jsonXmlConfig), this.field, this.getMethod, this.setMethod, declaringClass,
                    typeParamArgMap);

            dbType = getType(getDBAnnoType(propClass), this.field, this.getMethod, this.setMethod, declaringClass, typeParamArgMap);

            clazz = type.javaType();

            jsonNameTags = getJsonNameTags(propName, field);
            xmlNameTags = getXmlNameTags(propName, field, jsonXmlType.name(), false);

            final String timeZoneStr = Strings.trim(getTimeZone(field, jsonXmlConfig));
            final String dateFormatStr = Strings.trim(getDateFormat(field, propFuncMap.containsKey(clazz) ? jsonXmlConfig : null));
            dateFormat = Strings.isEmpty(dateFormatStr) ? null : dateFormatStr;
            timeZone = Strings.isEmpty(timeZoneStr) ? TimeZone.getDefault() : TimeZone.getTimeZone(timeZoneStr);
            zoneId = timeZone.toZoneId();
            isLongDateFormat = Strings.isNotEmpty(dateFormat) && "long".equalsIgnoreCase(dateFormat);

            // "long" is the epoch-millis marker, not a pattern; DateTimeFormatter.ofPattern would reject it.
            dateTimeFormatter = Strings.isEmpty(dateFormat) || isLongDateFormat ? null : DateTimeFormatter.ofPattern(dateFormat).withZone(zoneId);
            isJsonRawValue = isJsonRawValue(field);

            JodaDateTimeFormatterHolder tmpJodaDTFH = null;

            // Only build the Joda holder when an actual date format is present. Otherwise the holder's
            // DateTimeFormat.forPattern(dateFormat) throws IllegalArgumentException that was silently
            // caught below - one thrown+discarded exception per non-date property at bean-info build time.
            // (An empty format left jodaDTFH == null anyway, so behavior is unchanged.)
            if (Strings.isNotEmpty(dateFormat)) {
                try {
                    if (Class.forName("org.joda.time.DateTime") != null) {
                        tmpJodaDTFH = new JodaDateTimeFormatterHolder(dateFormat, timeZone);
                    }
                } catch (final Throwable e) {
                    // ignore.
                }
            }

            jodaDTFH = tmpJodaDTFH;

            if (isLongDateFormat && (java.time.LocalTime.class.isAssignableFrom(clazz) || java.time.LocalDate.class.isAssignableFrom(clazz))) {
                throw new UnsupportedOperationException("Date format cannot be 'long' for type java.time.LocalTime/LocalDate");
            }

            final String numberFormatStr = Strings.trim(getNumberFormat(field, type.isNumber() ? jsonXmlConfig : null));

            if (Strings.isEmpty(numberFormatStr)) {
                numberFormat = null;
            } else {
                // Use Locale.ROOT-derived symbols so the wire format ("#,##0.00") parses/formats
                // identically on every JVM. Default-locale DecimalFormat would write "1.234,56" on
                // de_DE (German) and "1,234.56" elsewhere, breaking round-trips across systems.
                final DecimalFormat decimalFormat = new DecimalFormat(numberFormatStr, DecimalFormatSymbols.getInstance(java.util.Locale.ROOT));

                // DecimalFormat.parse answers Long/Double by default, which silently rounds a BigDecimal
                // (12345678901234567.89 -> ...568) or a BigInteger beyond 2^53. Exact BigDecimal parsing fixes
                // that for the exact and integral targets only: for float/double targets it would turn "-0.00"
                // into BigDecimal 0.00 (no negative zero) and break the -0.0 round trip, and for Number/Object
                // targets it would change the runtime class the caller receives.
                if (isExactOrIntegralNumberType(clazz)) {
                    decimalFormat.setParseBigDecimal(true);
                }

                numberFormat = decimalFormat;
            }

            // The clone inherits parseBigDecimal, so set it on the prototype before the ThreadLocal is built.
            numberFormatTL = numberFormat == null ? null : ThreadLocal.withInitial(() -> (NumberFormat) numberFormat.clone());
            numberTargetClass = numberFormat == null ? null : ClassUtil.wrap(clazz);

            hasFormat = Strings.isNotEmpty(dateFormat) || numberFormat != null;

            boolean tmpIsMarkedToId = annotations.containsKey(Id.class) || annotations.containsKey(ReadOnlyId.class) || idPropNames.contains(propName);

            if (!tmpIsMarkedToId) {
                try {
                    tmpIsMarkedToId = annotations.containsKey(javax.persistence.Id.class);
                } catch (final Throwable e) {
                    // ignore
                }
            }

            if (!tmpIsMarkedToId) {
                try {
                    tmpIsMarkedToId = annotations.containsKey(jakarta.persistence.Id.class);
                } catch (final Throwable e) {
                    // ignore
                }
            }

            isMarkedAsId = tmpIsMarkedToId;

            isMarkedAsReadOnlyId = annotations.containsKey(ReadOnlyId.class) || (isMarkedAsId && annotations.containsKey(ReadOnly.class))
                    || readOnlyIdPropNames.contains(propName);

            String tmpColumnName = null;
            boolean tmpIsMarkedToColumn = false;

            if (annotations.containsKey(Column.class)) {
                tmpIsMarkedToColumn = true;

                tmpColumnName = ((Column) annotations.get(Column.class)).value();

                if (Strings.isEmpty(tmpColumnName)) {
                    tmpColumnName = ((Column) annotations.get(Column.class)).name();
                }
            } else {
                try {
                    if (annotations.containsKey(javax.persistence.Column.class)) {
                        tmpIsMarkedToColumn = true;

                        tmpColumnName = ((javax.persistence.Column) annotations.get(javax.persistence.Column.class)).name();
                    }
                } catch (final Throwable e) {
                    // ignore
                }

                if (!tmpIsMarkedToColumn) {
                    try {
                        if (annotations.containsKey(jakarta.persistence.Column.class)) {
                            tmpIsMarkedToColumn = true;

                            tmpColumnName = ((jakarta.persistence.Column) annotations.get(jakarta.persistence.Column.class)).name();
                        }
                    } catch (final Throwable e) {
                        // ignore
                    }
                }
            }

            if (Strings.isNotEmpty(tmpColumnName) && !tmpColumnName.equals(Strings.strip(tmpColumnName))) {
                throw new IllegalArgumentException("Column name: \"" + tmpColumnName + "\" must not start or end with any whitespace for field: " + field);
            }

            isMarkedAsColumn = tmpIsMarkedToColumn;

            isSubEntity = !isMarkedAsColumn && (type.isBean() || (type.isCollection() && type.elementType().isBean()));

            columnName = Strings.isEmpty(tmpColumnName) ? Optional.empty() : Optional.ofNullable(tmpColumnName);

            final String tmpTableAlias = type.isBean() && clazz.getAnnotation(Table.class) != null ? clazz.getAnnotation(Table.class).alias() : null;

            tablePrefix = Strings.isEmpty(tmpTableAlias) ? Optional.empty() : Optional.of(tmpTableAlias);

            canSetFieldByGetMethod = Beans.isRegisteredXmlBindingClass(declaringClass) && getMethod != null
                    && (Map.class.isAssignableFrom(getMethod.getReturnType()) || Collection.class.isAssignableFrom(getMethod.getReturnType()));

            // An immutable bean stores values into a constructor/builder slot, so its components are never read-only
            // even without a setter; everything else needs a field, a setter or a fillable getter collection.
            isReadOnlyProperty = field == null && setMethod == null && !canSetFieldByGetMethod && !isImmutableBean;

            // A declared direction wins; a read-only property is implicitly serialize-only, which is exactly what the
            // JSON/XML parsers need to skip its value on read (a document may legitimately carry the computed value
            // this bean wrote out) instead of dereferencing the missing field.
            jsonXmlExpose = field != null && field.isAnnotationPresent(JsonXmlField.class) ? field.getAnnotation(JsonXmlField.class).direction()
                    : (isReadOnlyProperty ? JsonXmlField.Direction.SERIALIZE_ONLY : JsonXmlField.Direction.BOTH);

            this.fieldOrder = fieldOrder;
            this.isImmutableBean = isImmutableBean;
            this.isByBuilder = isByBuilder;
        }

        /**
         * Gets the value of this property from the specified object.
         *
         * <p>This method handles different access strategies:</p>
         * <ul>
         *   <li>For immutable beans stored as arrays, directly accesses the array element</li>
         *   <li>For regular objects, uses field access if available, otherwise uses the getter method</li>
         * </ul>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * class Person {
         *     public String name;
         * }
         * Person person = new Person();
         * person.name = "John";
         * PropInfo nameProp = ParserUtil.getBeanInfo(Person.class).getPropInfo("name");
         * String name = nameProp.getPropValue(person); // returns "John"
         * }</pre>
         *
         * @param <T> the expected type of the property value
         * @param obj the object to get the property value from
         * @return the property value, cast to type T
         * @throws RuntimeException if reflection access fails
         */
        @SuppressWarnings("unchecked")
        public <T> T getPropValue(final Object obj) throws RuntimeException {
            if (isImmutableBean && obj instanceof Object[]) {
                return (T) ((Object[]) obj)[fieldOrder];
            }

            try {
                if (isFieldHandleGettable) {
                    return (T) fieldHandle.get(obj);
                } else {
                    return (T) (isFieldGettable ? field.get(obj) : getMethod.invoke(obj));
                }
            } catch (final Exception e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }

        /**
         * The message for the {@code UnsupportedOperationException} raised when a value is stored into a
         * read-only property.
         */
        final String readOnlyMessage() {
            return "Property '" + name + "' of class " + ClassUtil.getCanonicalClassName(declaringClass)
                    + " is read-only (a getter without a backing field or setter); its value cannot be set";
        }

        /**
         * Sets the value of this property on the specified object.
         *
         * <p>This method provides intelligent property setting with the following features:</p>
         * <ul>
         *   <li>Automatic type conversion when necessary</li>
         *   <li>Support for immutable beans and builder patterns</li>
         *   <li>Handling of JSON raw values</li>
         *   <li>Performance optimization for repeated operations</li>
         *   <li>Fallback strategies for different access methods</li>
         * </ul>
         *
         * <p>The method attempts to set the value in the following order:</p>
         * <ol>
         *   <li>Direct field access (if accessible and settable)</li>
         *   <li>Setter method invocation</li>
         *   <li>Setting via getter for collections/maps (XML binding)</li>
         *   <li>Forced field access as last resort</li>
         * </ol>
         *
         * <p>For a builder-based immutable bean {@code obj} is the builder instance, so the value is routed to the
         * <i>builder's</i> setter. A property whose builder declares no setter is silently skipped - the bean's own
         * field cannot be written into the builder, and the builder author chose not to accept that property.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * class Person {
         *     public int age;
         * }
         * Person person = new Person();
         * PropInfo ageProp = ParserUtil.getBeanInfo(Person.class).getPropInfo("age");
         * ageProp.setPropValue(person, 25);
         * ageProp.setPropValue(person, "30");   // value is converted from String
         * }</pre>
         *
         * @param obj the object to set the property value on
         * @param propValue the value to set (will be converted if necessary)
         * @throws UnsupportedOperationException if this property is read-only and cannot accept {@code propValue}
         * @throws RuntimeException if reflection access fails or type conversion fails
         */
        @SuppressFBWarnings
        public void setPropValue(final Object obj, Object propValue) throws UnsupportedOperationException, RuntimeException {
            if (isReadOnlyProperty) {
                throw new UnsupportedOperationException(readOnlyMessage());
            }

            if (isJsonRawValue && propValue != null && !clazz.isAssignableFrom(propValue.getClass())) {
                propValue = N.toJson(propValue);
            }

            if (isImmutableBean) {
                if (!isByBuilder) {
                    // Coalesce nulls to the type's default before storing in the Object[] slot.
                    // The canonical-constructor invocation later auto-unboxes primitive slots; a raw
                    // null overwriting the default would NPE on unbox (e.g. record R(int x) and JSON
                    // {"x": null} -> NPE on Integer.intValue()).
                    ((Object[]) obj)[fieldOrder] = propValue == null ? type.defaultValue() : propValue;
                    return;
                }

                if (setMethod == null) {
                    // `obj` is the BUILDER and the builder declares no setter for this property, so the value has
                    // nowhere to go: every writer below ends up at `field`, which belongs to the BEAN, and writing it
                    // into the builder throws ("Can not set final ... field Bean.x to Bean$Builder"). The builder
                    // author chose not to accept this property, so skip it - the same thing the parsers do for a
                    // value the target cannot take (ignoreUnmatchedProperty defaults to true), and what keeps
                    // fromJson(toJson(bean)), mapToBean(beanToMap(bean)) and copyAs(bean, sameClass) working for a
                    // bean whose builder omits a property the bean itself still serializes.
                    return;
                }
            }

            propValue = propValue == null ? type.defaultValue() : propValue;

            if (failureCountForSetProp > 100 && propValue != null && !clazz.isAssignableFrom(propValue.getClass())) {
                propValue = N.convert(propValue, jsonXmlType);

                try {
                    if (isFieldHandleSettable) {
                        fieldHandle.set(obj, propValue);
                    } else if (isFieldSettable) {
                        field.set(obj, propValue);
                    } else if (setMethod != null) {
                        setMethod.invoke(obj, propValue);
                    } else if (canSetFieldByGetMethod) {
                        Beans.setPropValueByGetter(obj, getMethod, propValue);
                    } else {
                        field.set(obj, propValue);
                    }

                    if (failureCountForSetProp > 0) {
                        failureCountForSetProp--;
                    }
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw ExceptionUtil.toRuntimeException(e, true);
                }
            } else {
                try {
                    if (isFieldSettable) {
                        field.set(obj, propValue);
                    } else if (setMethod != null) {
                        setMethod.invoke(obj, propValue);
                    } else if (canSetFieldByGetMethod) {
                        Beans.setPropValueByGetter(obj, getMethod, propValue);
                    } else {
                        field.set(obj, propValue);
                    }

                    if (failureCountForSetProp > 0) {
                        failureCountForSetProp--;
                    }
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw ExceptionUtil.toRuntimeException(e, true);
                } catch (final Exception e) {
                    if (failureCountForSetProp < 1000) {
                        failureCountForSetProp++;
                    }

                    if (logger.isWarnEnabled() && (failureCountForSetProp % 100 == 0)) {
                        logger.warn("Failed to set value for field: {} in class: {} with value type {}", field == null ? name : field.getName(),
                                ClassUtil.getClassName(declaringClass), propValue == null ? "null" : ClassUtil.getClassName(propValue.getClass()));
                    }

                    propValue = N.convert(propValue, jsonXmlType);

                    try {
                        if (isFieldSettable) {
                            field.set(obj, propValue);
                        } else if (setMethod != null) {
                            setMethod.invoke(obj, propValue);
                        } else if (canSetFieldByGetMethod) {
                            Beans.setPropValueByGetter(obj, getMethod, propValue);
                        } else {
                            field.set(obj, propValue);
                        }
                    } catch (IllegalAccessException | InvocationTargetException e2) {
                        throw ExceptionUtil.toRuntimeException(e2, true);
                    }
                }
            }
        }

        /**
         * Map of property type to their corresponding date/time reader/writer implementations.
         * Supports various date/time types from Java SE and Joda-Time.
         */
        static final Map<Class<?>, DateTimeReaderWriter<?>> propFuncMap = new HashMap<>();

        static {
            propFuncMap.put(String.class, new DateTimeReaderWriter<String>() {
                @Override
                public String read(final PropInfo propInfo, final String strValue) {
                    return strValue;
                }

                @Override
                public void write(final PropInfo propInfo, final String x, final CharacterWriter writer) throws IOException {
                    writer.write(x);
                }
            });

            propFuncMap.put(long.class, new DateTimeReaderWriter<Long>() {
                @Override
                public Long read(final PropInfo propInfo, final String strValue) {
                    // Same null markers as the ten temporal readers below: Numbers.toLong("") returns 0,
                    // which fabricated the 1970 epoch, and the literal "null" threw NumberFormatException.
                    return isNullDateText(strValue) ? 0L : Numbers.toLong(strValue);
                }

                @Override
                public void write(final PropInfo propInfo, final Long x, final CharacterWriter writer) throws IOException {
                    writer.write(x);
                }
            });

            propFuncMap.put(Long.class, new DateTimeReaderWriter<Long>() {
                @Override
                public Long read(final PropInfo propInfo, final String strValue) {
                    // Same null markers as the ten temporal readers below: Numbers.toLong("") returns 0,
                    // which fabricated the 1970 epoch, and the literal "null" threw NumberFormatException.
                    return isNullDateText(strValue) ? null : Numbers.toLong(strValue);
                }

                @Override
                public void write(final PropInfo propInfo, final Long x, final CharacterWriter writer) throws IOException {
                    if (x == null) {
                        writer.write(Strings.NULL);
                    } else {
                        writer.write(x);
                    }
                }
            });

            propFuncMap.put(java.util.Date.class, new DateTimeReaderWriter<java.util.Date>() {
                @Override
                public java.util.Date read(final PropInfo propInfo, final String strValue) {
                    if (isNullDateText(strValue)) {
                        return null;
                    }

                    if (propInfo.isLongDateFormat) {
                        return new java.util.Date(Numbers.toLong(strValue));
                    } else {
                        return Dates.parseToJUDate(strValue, propInfo.dateFormat, propInfo.timeZone);
                    }
                }

                @Override
                public void write(final PropInfo propInfo, final java.util.Date x, final CharacterWriter writer) throws IOException {
                    if (propInfo.isLongDateFormat) {
                        writer.write(x.getTime());
                    } else {
                        Dates.formatTo(x, propInfo.dateFormat, propInfo.timeZone, writer);
                    }
                }
            });

            // Additional date/time type handlers...
            propFuncMap.put(java.util.Calendar.class, new DateTimeReaderWriter<java.util.Calendar>() {
                @Override
                public java.util.Calendar read(final PropInfo propInfo, final String strValue) {
                    if (isNullDateText(strValue)) {
                        return null;
                    }

                    if (propInfo.isLongDateFormat) {
                        final Calendar calendar = Calendar.getInstance();
                        calendar.setTimeInMillis(Numbers.toLong(strValue));
                        calendar.setTimeZone(propInfo.timeZone);
                        return calendar;
                    } else {
                        return Dates.parseToCalendar(strValue, propInfo.dateFormat, propInfo.timeZone);
                    }
                }

                @Override
                public void write(final PropInfo propInfo, final java.util.Calendar x, final CharacterWriter writer) throws IOException {
                    if (propInfo.isLongDateFormat) {
                        writer.write(x.getTimeInMillis());
                    } else {
                        Dates.formatTo(x, propInfo.dateFormat, propInfo.timeZone, writer);
                    }

                }
            });

            propFuncMap.put(java.sql.Timestamp.class, new DateTimeReaderWriter<java.sql.Timestamp>() {
                @Override
                public java.sql.Timestamp read(final PropInfo propInfo, final String strValue) {
                    if (isNullDateText(strValue)) {
                        return null;
                    }

                    if (propInfo.isLongDateFormat) {
                        return new java.sql.Timestamp(Numbers.toLong(strValue));
                    } else {
                        return Dates.parseToTimestamp(strValue, propInfo.dateFormat, propInfo.timeZone);
                    }
                }

                @Override
                public void write(final PropInfo propInfo, final java.sql.Timestamp x, final CharacterWriter writer) throws IOException {
                    if (propInfo.isLongDateFormat) {
                        writer.write(x.getTime());
                    } else {
                        Dates.formatTo(x, propInfo.dateFormat, propInfo.timeZone, writer);
                    }
                }
            });

            propFuncMap.put(java.sql.Date.class, new DateTimeReaderWriter<java.sql.Date>() {
                @Override
                public java.sql.Date read(final PropInfo propInfo, final String strValue) {
                    if (isNullDateText(strValue)) {
                        return null;
                    }

                    if (propInfo.isLongDateFormat) {
                        return new java.sql.Date(Numbers.toLong(strValue));
                    } else {
                        // The write side renders wall-clock fields in propInfo.zoneId, so anchor the parsed
                        // civil date at start-of-day in that same zone to keep the round-trip stable.
                        return new java.sql.Date(
                                Dates.parseToLocalDate(strValue, propInfo.dateFormat).atStartOfDay(propInfo.zoneId).toInstant().toEpochMilli());
                    }
                }

                @Override
                public void write(final PropInfo propInfo, final java.sql.Date x, final CharacterWriter writer) throws IOException {
                    if (propInfo.isLongDateFormat) {
                        writer.write(x.getTime());
                    } else {
                        Dates.formatTo(x, propInfo.dateFormat, propInfo.timeZone, writer);
                    }
                }
            });

            propFuncMap.put(java.sql.Time.class, new DateTimeReaderWriter<java.sql.Time>() {
                @Override
                public java.sql.Time read(final PropInfo propInfo, final String strValue) {
                    if (isNullDateText(strValue)) {
                        return null;
                    }

                    if (propInfo.isLongDateFormat) {
                        return new java.sql.Time(Numbers.toLong(strValue));
                    } else {
                        // Same zone anchoring as the java.sql.Date reader above, on the SQL time epoch date.
                        return new java.sql.Time(Dates.parseToLocalTime(strValue, propInfo.dateFormat)
                                .atDate(LocalDate.of(1970, 1, 1))
                                .atZone(propInfo.zoneId)
                                .toInstant()
                                .toEpochMilli());
                    }
                }

                @Override
                public void write(final PropInfo propInfo, final java.sql.Time x, final CharacterWriter writer) throws IOException {
                    if (propInfo.isLongDateFormat) {
                        writer.write(x.getTime());
                    } else {
                        Dates.formatTo(x, propInfo.dateFormat, propInfo.timeZone, writer);
                    }
                }
            });

            propFuncMap.put(java.time.LocalDateTime.class, new DateTimeReaderWriter<java.time.LocalDateTime>() {
                @Override
                public java.time.LocalDateTime read(final PropInfo propInfo, final String strValue) {
                    if (isNullDateText(strValue)) {
                        return null;
                    }

                    if (propInfo.isLongDateFormat) {
                        // Use the configured zone (mirrors the write path x.atZone(propInfo.zoneId)) instead of
                        // Timestamp.toLocalDateTime(), which would silently apply the JVM-default zone and break
                        // the epoch-millis round-trip whenever a non-default field timeZone is configured.
                        return java.time.LocalDateTime.ofInstant(Instant.ofEpochMilli(Numbers.toLong(strValue)), propInfo.zoneId);
                    } else {
                        return java.time.LocalDateTime.parse(strValue, propInfo.dateTimeFormatter);
                    }
                }

                @Override
                public void write(final PropInfo propInfo, final java.time.LocalDateTime x, final CharacterWriter writer) throws IOException {
                    if (propInfo.isLongDateFormat) {
                        writer.write(x.atZone(propInfo.zoneId).toInstant().toEpochMilli());
                    } else {
                        propInfo.dateTimeFormatter.formatTo(x, writer);
                    }
                }
            });

            propFuncMap.put(java.time.LocalDate.class, new DateTimeReaderWriter<java.time.LocalDate>() {
                /**
                 * @throws UnsupportedOperationException if the property uses the long date format and the input is not null, empty, or the null literal
                 */
                @Override
                public java.time.LocalDate read(final PropInfo propInfo, final String strValue) throws UnsupportedOperationException {
                    if (isNullDateText(strValue)) {
                        return null;
                    }

                    if (propInfo.isLongDateFormat) {
                        throw new UnsupportedOperationException("Date format cannot be 'long' for type java.time.LocalDate");
                    } else {
                        return java.time.LocalDate.parse(strValue, propInfo.dateTimeFormatter);
                    }
                }

                /**
                 * @throws UnsupportedOperationException if the property uses the long date format
                 */
                @Override
                public void write(final PropInfo propInfo, final java.time.LocalDate x, final CharacterWriter writer) throws UnsupportedOperationException {
                    if (propInfo.isLongDateFormat) {
                        throw new UnsupportedOperationException("Date format cannot be 'long' for type java.time.LocalDate");
                    } else {
                        propInfo.dateTimeFormatter.formatTo(x, writer);
                    }
                }
            });

            propFuncMap.put(java.time.LocalTime.class, new DateTimeReaderWriter<java.time.LocalTime>() {
                /**
                 * @throws UnsupportedOperationException if the property uses the long date format and the input is not null, empty, or the null literal
                 */
                @Override
                public java.time.LocalTime read(final PropInfo propInfo, final String strValue) throws UnsupportedOperationException {
                    if (isNullDateText(strValue)) {
                        return null;
                    }

                    if (propInfo.isLongDateFormat) {
                        throw new UnsupportedOperationException("Date format cannot be 'long' for type java.time.LocalTime");
                    } else {
                        return java.time.LocalTime.parse(strValue, propInfo.dateTimeFormatter);
                    }
                }

                /**
                 * @throws UnsupportedOperationException if the property uses the long date format
                 */
                @Override
                public void write(final PropInfo propInfo, final java.time.LocalTime x, final CharacterWriter writer) throws UnsupportedOperationException {
                    if (propInfo.isLongDateFormat) {
                        throw new UnsupportedOperationException("Date format cannot be 'long' for type java.time.LocalTime");
                    } else {
                        propInfo.dateTimeFormatter.formatTo(x, writer);
                    }
                }
            });

            propFuncMap.put(java.time.ZonedDateTime.class, new DateTimeReaderWriter<java.time.ZonedDateTime>() {
                @Override
                public java.time.ZonedDateTime read(final PropInfo propInfo, final String strValue) {
                    if (isNullDateText(strValue)) {
                        return null;
                    }

                    if (propInfo.isLongDateFormat) {
                        return ZonedDateTime.ofInstant(Instant.ofEpochMilli(Numbers.toLong(strValue)), propInfo.zoneId);
                    } else {
                        return ZonedDateTime.parse(strValue, propInfo.dateTimeFormatter);
                    }
                }

                @Override
                public void write(final PropInfo propInfo, final java.time.ZonedDateTime x, final CharacterWriter writer) throws IOException {
                    if (propInfo.isLongDateFormat) {
                        writer.write(x.toInstant().toEpochMilli());
                    } else {
                        propInfo.dateTimeFormatter.formatTo(x, writer);
                    }
                }
            });

            try {
                if (Class.forName("org.joda.time.DateTime") != null) {
                    propFuncMap.put(org.joda.time.DateTime.class, new DateTimeReaderWriter<org.joda.time.DateTime>() {
                        @Override
                        public org.joda.time.DateTime read(final PropInfo propInfo, final String strValue) {
                            if (isNullDateText(strValue)) {
                                return null;
                            }

                            if (propInfo.isLongDateFormat) {
                                final org.joda.time.DateTime dt = new org.joda.time.DateTime(Numbers.toLong(strValue));
                                return dt.getZone().equals(propInfo.jodaDTFH.dtz) ? dt : dt.withZone(propInfo.jodaDTFH.dtz);
                            } else {
                                return propInfo.jodaDTFH.dtf.parseDateTime(strValue);
                            }
                        }

                        @Override
                        public void write(final PropInfo propInfo, final org.joda.time.DateTime x, final CharacterWriter writer) throws IOException {
                            if (propInfo.isLongDateFormat) {
                                writer.write(x.getMillis());
                            } else {
                                propInfo.jodaDTFH.dtf.printTo(writer, x);
                            }
                        }
                    });

                    propFuncMap.put(org.joda.time.MutableDateTime.class, new DateTimeReaderWriter<org.joda.time.MutableDateTime>() {
                        @Override
                        public org.joda.time.MutableDateTime read(final PropInfo propInfo, final String strValue) {
                            if (isNullDateText(strValue)) {
                                return null;
                            }

                            if (propInfo.isLongDateFormat) {
                                final org.joda.time.MutableDateTime dt = new org.joda.time.MutableDateTime(Numbers.toLong(strValue));

                                if (!propInfo.jodaDTFH.dtz.equals(dt.getZone())) {
                                    dt.setZone(propInfo.jodaDTFH.dtz);
                                }

                                return dt;
                            } else {
                                return propInfo.jodaDTFH.dtf.parseMutableDateTime(strValue);
                            }
                        }

                        @Override
                        public void write(final PropInfo propInfo, final org.joda.time.MutableDateTime x, final CharacterWriter writer) throws IOException {
                            if (propInfo.isLongDateFormat) {
                                writer.write(x.getMillis());
                            } else {
                                propInfo.jodaDTFH.dtf.printTo(writer, x);
                            }
                        }
                    });
                }
            } catch (final Throwable e) {
                // ignore.
            }
        }

        /**
         * Reads and converts a string value to the appropriate property type.
         *
         * <p>This method handles parsing of string values according to the property's type
         * and format configuration. It supports:</p>
         * <ul>
         *   <li>Date/time parsing with custom formats or epoch milliseconds</li>
         *   <li>Number parsing with custom formats</li>
         *   <li>General type conversion for other types</li>
         * </ul>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * class Event {
         *     @JsonXmlField(dateFormat = "yyyy-MM-dd", timeZone = "UTC")
         *     public java.util.Date date;
         *
         *     @JsonXmlField(dateFormat = "long")
         *     public java.util.Date createdAt;
         * }
         *
         * BeanInfo beanInfo = ParserUtil.getBeanInfo(Event.class);
         * PropInfo dateProp = beanInfo.getPropInfo("date");
         * java.util.Date date = (java.util.Date) dateProp.readPropValue("2023-12-25");
         *
         * PropInfo longDateProp = beanInfo.getPropInfo("createdAt");
         * java.util.Date date2 = (java.util.Date) longDateProp.readPropValue("1703462400000");
         * }</pre>
         *
         * @param strValue the string value to parse
         * @return the parsed value in the property's own type. A number-format read converts the parsed number
         *         to the property type ({@code Integer} for an {@code int}/{@code Integer} property, an exact
         *         {@code BigDecimal}/{@code BigInteger} for those property types, and so on), so the value can
         *         be stored without a further conversion. Returns {@code null} when {@code strValue} is
         *         {@code null}: the number-format branch, the date-format readers and most other types all
         *         pass {@code null} through; every date-format reader also returns {@code null} for empty text
         *         and the literal {@code "null"} (case-insensitive), the same markers the no-format date types
         *         accept. Two entries differ: a primitive {@code long} property with the {@code "long"} date
         *         format returns {@code 0L} for those markers, and a {@code String} property - whose reader is
         *         the identity - returns the text it was handed unchanged
         * @throws UnsupportedOperationException if a date format is specified for an unsupported type
         * @throws ParsingException if a number format is specified and {@code strValue} cannot be parsed
         *         against it in full
         * @throws RuntimeException if the parsed number cannot be converted to the property type (for example
         *         {@code NaN} into a {@code BigDecimal} property, which raises an {@code ArithmeticException})
         */
        public Object readPropValue(final String strValue) throws UnsupportedOperationException, ParsingException, RuntimeException {
            if (hasFormat) {
                if (dateFormat != null) {
                    final DateTimeReaderWriter<?> func = propFuncMap.get(clazz);

                    if (func == null) {
                        throw new UnsupportedOperationException("'DateFormat' annotation for field: " + field
                                + " is only supported for types: java.util.Date/Calendar, java.sql.Date/Time/Timestamp, java.time.LocalDateTime/LocalDate/LocalTime/ZonedDateTime, not supported for: "
                                + ClassUtil.getCanonicalClassName(clazz));
                    }

                    return func.read(this, strValue);
                } else {
                    // Date-format readers in propFuncMap each null-check strValue. NumberFormat.parse(null)
                    // does not — it NPEs. JSON literal null deserializes to a Java null string here, so
                    // for @JsonXmlField(numberFormat=...) fields with a null source value, return null
                    // instead of NPE'ing.
                    if (strValue == null) {
                        return null;
                    }

                    final ParsePosition position = new ParsePosition(0);
                    final Number result = numberFormatTL.get().parse(strValue, position);

                    if (result == null || position.getIndex() != strValue.length()) {
                        // ParsingException, not a bare RuntimeException: a caller catching the parser's own
                        // failure type must catch this malformed-value failure with it.
                        throw new ParsingException(
                                "Failed to parse complete number value: " + strValue + " with format: " + numberFormat + " at index: " + position.getIndex());
                    }

                    // Hand back the property's own type. Leaving the raw Long/Double/BigDecimal to setPropValue
                    // made every formatted int/short/byte/BigDecimal read go through its exception-and-retry
                    // branch (one IllegalArgumentException per value plus a spurious "Failed to set value" warning
                    // every 100). A value that already has the right class (a Double for a Double property, which
                    // also keeps -0.0 intact) is returned as is.
                    return numberTargetClass.isInstance(result) ? result : N.convert(result, jsonXmlType);
                }
            } else {
                return jsonXmlType.valueOf(strValue);
            }
        }

        /**
         * Writes a property value to a character writer with appropriate formatting.
         *
         * <p>This method handles serialization of property values according to their type
         * and format configuration. It supports:</p>
         * <ul>
         *   <li>Date/time formatting with custom patterns or epoch milliseconds</li>
         *   <li>Number formatting with custom patterns</li>
         *   <li>JSON raw value output (unquoted)</li>
         *   <li>Standard type serialization with optional quoting</li>
         * </ul>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * class Event {
         *     @JsonXmlField(dateFormat = "yyyy-MM-dd", timeZone = "UTC")
         *     public java.util.Date date;
         * }
         *
         * PropInfo dateProp = ParserUtil.getBeanInfo(Event.class).getPropInfo("date");
         * com.landawn.abacus.util.BufferedJsonWriter writer =
         *         com.landawn.abacus.util.Objectory.createBufferedJsonWriter();
         * try {
         *     dateProp.writePropValue(writer, new java.util.Date(), new JsonSerConfig());
         *     String jsonValue = writer.toString();
         * } finally {
         *     com.landawn.abacus.util.Objectory.recycle(writer);
         * }
         * }</pre>
         *
         * @param writer the character writer to write to
         * @param x the value to write
         * @param config the serialization configuration
         * @throws UnsupportedOperationException if date format is specified for unsupported types
         * @throws IOException if writing the formatted, raw or type-serialized property value, its quotation marks or the null literal to
         *         {@code writer} fails
         */
        public void writePropValue(final CharacterWriter writer, final Object x, final JsonXmlSerConfig<?> config)
                throws UnsupportedOperationException, IOException {
            if (hasFormat) {
                if (x == null) {
                    writer.write(NULL_CHAR_ARRAY);
                } else if (dateFormat != null) {
                    final boolean isQuote = (config != null) && (config.getStringQuotation() != 0);

                    @SuppressWarnings("rawtypes")
                    final DateTimeReaderWriter func = propFuncMap.get(clazz);

                    if (func == null) {
                        throw new UnsupportedOperationException("'DateFormat' annotation for field: " + field
                                + " is only supported for types: java.util.Date/Calendar, java.sql.Date/Time/Timestamp, java.time.LocalDateTime/LocalDate/LocalTime/ZonedDateTime, not supported for: "
                                + ClassUtil.getCanonicalClassName(clazz));
                    }

                    if (config instanceof JsonSerConfig && isQuote) {
                        // Formatters write literal text; their quotes, backslashes and controls must
                        // pass through the same escaping as other JSON strings.
                        final BufferedJsonWriter formatted = Objectory.createBufferedJsonWriter();
                        try {
                            func.write(this, x, formatted);
                            Type.of(String.class).serializeTo(writer, formatted.toString(), config);
                        } finally {
                            Objectory.recycle(formatted);
                        }
                    } else {
                        if (isQuote) {
                            writer.write(config.getStringQuotation());
                        }
                        func.write(this, x, writer);
                        if (isQuote) {
                            writer.write(config.getStringQuotation());
                        }
                    }
                } else {
                    final String formatted = numberFormatTL.get().format(x);
                    // Grouping separators, affixes and leading zeroes are legal DecimalFormat output,
                    // but not JSON numbers. Use the normal string writer for quoting and escaping.
                    if (config instanceof JsonSerConfig && !JSON_NUMBER_TOKEN.matcher(formatted).matches()) {
                        Type.of(String.class).serializeTo(writer, formatted, config);
                    } else {
                        writer.write(formatted);
                    }
                }
            } else if (isJsonRawValue) {
                if (x == null) {
                    writer.write(NULL_CHAR_ARRAY);
                } else {
                    writer.write(jsonXmlType.stringOf(x));
                }
            } else {
                jsonXmlType.serializeTo(writer, x, config);
            }
        }

        /**
         * Checks if this property has the specified annotation.
         *
         * <p>This method checks for annotations on the field, getter method, and setter method.
         * It provides a unified way to check for property-level annotations regardless of
         * where they are declared.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * if (propInfo.isAnnotationPresent(NotNull.class)) {
         *     // Validate that the property is not null
         * }
         * }</pre>
         *
         * @param annotationClass the annotation class to check for
         * @return {@code true} if the annotation is present, {@code false} otherwise
         */
        public boolean isAnnotationPresent(final Class<? extends Annotation> annotationClass) {
            return annotations.containsKey(annotationClass);
        }

        /**
         * Gets the specified annotation from this property.
         *
         * <p>This method retrieves annotations from the field, getter method, or setter method.
         * If the same annotation is present in multiple places, the value collected last wins,
         * so the precedence order is: setter annotations, then getter annotations, then field annotations.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * JsonXmlField jsonField = propInfo.getAnnotation(JsonXmlField.class);
         * if (jsonField != null) {
         *     String customName = jsonField.name();
         * }
         * }</pre>
         *
         * @param <T> the annotation type
         * @param annotationClass the annotation class to retrieve
         * @return the annotation instance, or {@code null} if not present
         */
        public <T extends Annotation> T getAnnotation(final Class<T> annotationClass) {
            return (T) annotations.get(annotationClass);
        }

        /**
         * Collects all annotations from the field and methods into a single map.
         *
         * @return map of annotation classes to annotation instances
         */
        private Map<Class<? extends Annotation>, Annotation> getAnnotations() {
            final Map<Class<? extends Annotation>, Annotation> annos = new HashMap<>();

            if (field != null && N.notEmpty(field.getAnnotations())) {
                for (final Annotation anno : field.getAnnotations()) {
                    annos.put(anno.annotationType(), anno);
                }
            }

            if (getMethod != null && N.notEmpty(getMethod.getAnnotations())) {
                for (final Annotation anno : getMethod.getAnnotations()) {
                    annos.put(anno.annotationType(), anno);
                }
            }

            if (setMethod != null && N.notEmpty(setMethod.getAnnotations())) {
                for (final Annotation anno : setMethod.getAnnotations()) {
                    annos.put(anno.annotationType(), anno);
                }
            }

            return annos;
        }

        /**
         * Gets the annotated type name for general serialization.
         *
         * @param field the declared field for the property being serialized
         * @param propClass the property class
         * @param jsonXmlConfig the optional JSON/XML serialization configuration
         * @return the type name or {@code null} if not specified
         */
        @SuppressWarnings("unused")
        private String getAnnoType(final Field field, final Class<?> propClass, final JsonXmlConfig jsonXmlConfig) {
            final com.landawn.abacus.annotation.Type typeAnno = getAnnotation(com.landawn.abacus.annotation.Type.class);

            if (typeAnno != null && (typeAnno.scope() == Scope.ALL || typeAnno.scope() == Scope.SERIALIZATION)) {
                final String typeName = getTypeName(typeAnno, propClass);

                if (Strings.isNotEmpty(typeName)) {
                    return typeName;
                }
            }

            final JsonXmlField jsonXmlFieldAnno = getAnnotation(JsonXmlField.class);

            if (jsonXmlFieldAnno != null) {
                if (Strings.isNotEmpty(jsonXmlFieldAnno.type())) {
                    return jsonXmlFieldAnno.type();
                } else if (propClass.isEnum()) {
                    return ClassUtil.getCanonicalClassName(propClass) + "(" + getEnumerated(field, jsonXmlConfig).name() + ")";
                }
            }

            if (jsonXmlConfig != null && propClass.isEnum()) {
                return ClassUtil.getCanonicalClassName(propClass) + "(" + getEnumerated(field, jsonXmlConfig).name() + ")";
            }

            return null;
        }

        /**
         * Gets the annotated type name specifically for JSON/XML serialization.
         *
         * @param field the declared field for the property being serialized
         * @param propClass the property class
         * @param jsonXmlConfig the optional JSON/XML serialization configuration
         * @return the type name or {@code null} if not specified
         */
        @SuppressWarnings("unused")
        private String getJsonXmlAnnoType(final Field field, final Class<?> propClass, final JsonXmlConfig jsonXmlConfig) {
            final JsonXmlField jsonXmlFieldAnno = getAnnotation(JsonXmlField.class);

            if (jsonXmlFieldAnno != null) {
                if (Strings.isNotEmpty(jsonXmlFieldAnno.type())) {
                    return jsonXmlFieldAnno.type();
                } else if (propClass.isEnum()) {
                    return ClassUtil.getCanonicalClassName(propClass) + "(" + getEnumerated(field, jsonXmlConfig).name() + ")";
                }
            }

            if (jsonXmlConfig != null && propClass.isEnum()) {
                return ClassUtil.getCanonicalClassName(propClass) + "(" + getEnumerated(field, jsonXmlConfig).name() + ")";
            }

            final com.landawn.abacus.annotation.Type typeAnno = getAnnotation(com.landawn.abacus.annotation.Type.class);

            if (typeAnno != null && (typeAnno.scope() == Scope.ALL || typeAnno.scope() == Scope.SERIALIZATION)) {
                final String typeName = getTypeName(typeAnno, propClass);

                if (Strings.isNotEmpty(typeName)) {
                    return typeName;
                }
            }

            return null;
        }

        /**
         * Gets the annotated type name for database operations.
         *
         * @param propClass the property class
         * @return the type name or {@code null} if not specified
         */
        @SuppressWarnings("unused")
        private String getDBAnnoType(final Class<?> propClass) {
            final com.landawn.abacus.annotation.Type typeAnno = getAnnotation(com.landawn.abacus.annotation.Type.class);

            if (typeAnno != null && (typeAnno.scope() == Scope.ALL || typeAnno.scope() == Scope.PERSISTENCE)) {
                final String typeName = getTypeName(typeAnno, propClass);

                if (Strings.isNotEmpty(typeName)) {
                    return typeName;
                }
            }

            return null;
        }

        /**
         * @throws IllegalArgumentException if an explicit type name has surrounding whitespace or the type handler lacks a required no-argument constructor
         */
        private String getTypeName(final com.landawn.abacus.annotation.Type typeAnno, final Class<?> propClass) throws IllegalArgumentException {
            @SuppressWarnings("deprecation")
            final Optional<String> typeName = N.firstNonEmpty(typeAnno.value(), typeAnno.name());

            if (typeName.isPresent() && !typeName.get().equals(Strings.strip(typeName.get()))) {
                throw new IllegalArgumentException("Type name: \"" + typeName.get() + "\" must not start or end with any whitespace for field: " + field);
            }

            @SuppressWarnings("rawtypes")
            final Class<? extends Type> typeClass = typeAnno.clazz();

            if (typeClass != null && !typeClass.equals(Type.class)) {
                Type<?> localType = null;
                @SuppressWarnings("rawtypes")
                Constructor<? extends Type> constructor = null;

                if (typeName.isPresent()) {
                    constructor = ClassUtil.getDeclaredConstructor(typeClass, String.class);

                    if (constructor != null) {
                        ClassUtil.setAccessibleQuietly(constructor, true);
                        localType = ClassUtil.invokeConstructor(constructor, typeName.get());
                    } else {
                        constructor = ClassUtil.getDeclaredConstructor(typeClass);

                        if (constructor == null) {
                            throw new IllegalArgumentException("No default constructor found in type class: " + typeClass);
                        }

                        ClassUtil.setAccessibleQuietly(constructor, true);
                        localType = ClassUtil.invokeConstructor(constructor);
                    }
                } else {
                    constructor = ClassUtil.getDeclaredConstructor(typeClass);

                    if (constructor == null) {
                        throw new IllegalArgumentException("No default constructor found in type class: " + typeClass);
                    }

                    ClassUtil.setAccessibleQuietly(constructor, true);
                    localType = ClassUtil.invokeConstructor(constructor);
                }

                try {
                    TypeFactory.registerType(localType);
                } catch (final Exception e) {
                    // ignore.
                }

                return localType.name();
            } else if (typeName.isPresent()) {
                return typeName.get();
            } else if (propClass.isEnum()) {
                return ClassUtil.getCanonicalClassName(propClass) + "(" + (typeAnno.enumerated() == null ? EnumType.NAME.name() : typeAnno.enumerated().name())
                        + ")";
            }

            return null;
        }

        @SuppressWarnings("unused")
        private <T> Type<T> getType(final String annoType, final Field field, final Method getMethod, final Method setMethod, final Class<?> beanClass,
                final Map<TypeVariable<?>, java.lang.reflect.Type> typeParamArgMap) {
            if (Strings.isEmpty(annoType)) {
                java.lang.reflect.Type genericType = field != null ? field.getGenericType()
                        : setMethod != null ? setMethod.getGenericParameterTypes()[0] : getMethod.getGenericReturnType();

                if ((genericType instanceof TypeVariable) && typeParamArgMap.containsKey(genericType)) {
                    return getType(genericType, typeParamArgMap);
                } else if (genericType instanceof GenericArrayType genericArrayType) {
                    return getType(genericArrayType, typeParamArgMap);
                } else if (genericType instanceof ParameterizedType parameterizedType) {
                    return getType(parameterizedType, typeParamArgMap);
                } else {
                    final String parameterizedTypeName = field != null ? ClassUtil.getParameterizedTypeNameByField(field)
                            : ClassUtil.getParameterizedTypeNameByMethod((setMethod != null) ? setMethod : getMethod);

                    return Type.of(parameterizedTypeName);
                }
            } else {
                Type<T> localType = null;

                try {
                    localType = Type.of(annoType);
                } catch (final Exception e) {
                    // ignore
                }

                if ((localType == null || localType.getClass().equals(ObjectType.class)) && Strings.isNotEmpty(ClassUtil.getPackageName(beanClass))) {
                    final String pkgName = ClassUtil.getPackageName(beanClass);
                    final StringBuilder sb = new StringBuilder();
                    int start = 0;

                    for (int i = 0, len = annoType.length(); i < len; i++) {
                        final char ch = annoType.charAt(i);

                        if (ch == '<' || ch == '>' || ch == ' ' || ch == ',') {
                            final String typeSegment = annoType.substring(start, i);

                            if (!typeSegment.isEmpty() && Type.of(typeSegment).isObject() && !Type.of(pkgName + "." + typeSegment).isObject()) {
                                sb.append(pkgName).append(".").append(typeSegment);
                            } else {
                                sb.append(typeSegment);
                            }

                            sb.append(ch);
                            start = i + 1;
                        }
                    }

                    if (start < annoType.length()) {
                        final String typeSegment = annoType.substring(start);

                        if (Type.of(typeSegment).isObject() && !Type.of(pkgName + "." + typeSegment).isObject()) {
                            sb.append(pkgName).append(".").append(typeSegment);
                        } else {
                            sb.append(typeSegment);
                        }
                    }

                    localType = Type.of(sb.toString());
                }

                return localType;
            }
        }

        // Keep structural generic metadata when substituting bean property variables. Rendering a
        // name here erases bean component arguments before TypeFactory can preserve them.
        private static java.lang.reflect.Type resolvePropertyType(final java.lang.reflect.Type source,
                final Map<TypeVariable<?>, java.lang.reflect.Type> resolvedVariables) {
            return resolvePropertyType(source, resolvedVariables, new HashSet<>());
        }

        private static java.lang.reflect.Type resolvePropertyType(final java.lang.reflect.Type source,
                final Map<TypeVariable<?>, java.lang.reflect.Type> resolvedVariables, final Set<TypeVariable<?>> resolvingVariables) {
            if (source instanceof TypeVariable<?> variable) {
                final java.lang.reflect.Type resolved = resolvedVariables.get(variable);
                if (resolved == null || resolved == variable || !resolvingVariables.add(variable)) {
                    return variable;
                }

                try {
                    return resolvePropertyType(resolved, resolvedVariables, resolvingVariables);
                } finally {
                    resolvingVariables.remove(variable);
                }
            }

            if (source instanceof ParameterizedType parameterizedType) {
                final java.lang.reflect.Type owner = parameterizedType.getOwnerType();
                final java.lang.reflect.Type resolvedOwner = owner == null ? null : resolvePropertyType(owner, resolvedVariables, resolvingVariables);
                final java.lang.reflect.Type[] arguments = parameterizedType.getActualTypeArguments();
                final java.lang.reflect.Type[] resolvedArguments = new java.lang.reflect.Type[arguments.length];
                boolean changed = resolvedOwner != owner;

                for (int i = 0; i < arguments.length; i++) {
                    resolvedArguments[i] = resolvePropertyType(arguments[i], resolvedVariables, resolvingVariables);
                    changed |= resolvedArguments[i] != arguments[i];
                }

                return changed ? new ResolvedParameterizedType(resolvedOwner, parameterizedType.getRawType(), resolvedArguments) : parameterizedType;
            }

            if (source instanceof GenericArrayType arrayType) {
                final java.lang.reflect.Type componentType = arrayType.getGenericComponentType();
                final java.lang.reflect.Type resolvedComponentType = resolvePropertyType(componentType, resolvedVariables, resolvingVariables);

                if (resolvedComponentType == componentType) {
                    return arrayType;
                }

                return resolvedComponentType instanceof Class<?> componentClass ? java.lang.reflect.Array.newInstance(componentClass, 0).getClass()
                        : new ResolvedGenericArrayType(resolvedComponentType);
            }

            if (source instanceof WildcardType wildcardType) {
                final java.lang.reflect.Type[] upperBounds = resolvePropertyTypes(wildcardType.getUpperBounds(), resolvedVariables, resolvingVariables);
                final java.lang.reflect.Type[] lowerBounds = resolvePropertyTypes(wildcardType.getLowerBounds(), resolvedVariables, resolvingVariables);

                return Arrays.equals(upperBounds, wildcardType.getUpperBounds()) && Arrays.equals(lowerBounds, wildcardType.getLowerBounds()) ? wildcardType
                        : new ResolvedWildcardType(upperBounds, lowerBounds);
            }

            return source;
        }

        private static java.lang.reflect.Type[] resolvePropertyTypes(final java.lang.reflect.Type[] sources,
                final Map<TypeVariable<?>, java.lang.reflect.Type> resolvedVariables, final Set<TypeVariable<?>> resolvingVariables) {
            final java.lang.reflect.Type[] result = new java.lang.reflect.Type[sources.length];

            for (int i = 0; i < sources.length; i++) {
                result[i] = resolvePropertyType(sources[i], resolvedVariables, resolvingVariables);
            }

            return result;
        }

        private static final class ResolvedParameterizedType implements ParameterizedType {
            private final java.lang.reflect.Type ownerType;
            private final java.lang.reflect.Type rawType;
            private final java.lang.reflect.Type[] typeArguments;

            ResolvedParameterizedType(final java.lang.reflect.Type ownerType, final java.lang.reflect.Type rawType,
                    final java.lang.reflect.Type[] typeArguments) {
                this.ownerType = ownerType;
                this.rawType = rawType;
                this.typeArguments = typeArguments.clone();
            }

            @Override
            public java.lang.reflect.Type[] getActualTypeArguments() {
                return typeArguments.clone();
            }

            @Override
            public java.lang.reflect.Type getRawType() {
                return rawType;
            }

            @Override
            public java.lang.reflect.Type getOwnerType() {
                return ownerType;
            }

            @Override
            public boolean equals(final Object obj) {
                return obj instanceof ParameterizedType other && Objects.equals(ownerType, other.getOwnerType()) && Objects.equals(rawType, other.getRawType())
                        && Arrays.equals(typeArguments, other.getActualTypeArguments());
            }

            @Override
            public int hashCode() {
                return Arrays.hashCode(typeArguments) ^ Objects.hashCode(ownerType) ^ Objects.hashCode(rawType);
            }

            @Override
            public String getTypeName() {
                final StringBuilder result;

                if (ownerType != null && rawType instanceof Class<?> rawClass) {
                    result = new StringBuilder(ownerType.getTypeName()).append('$').append(rawClass.getSimpleName());
                } else {
                    result = new StringBuilder(rawType.getTypeName());
                }

                if (typeArguments.length == 0) {
                    return result.toString();
                }

                result.append('<');

                for (int i = 0; i < typeArguments.length; i++) {
                    if (i > 0) {
                        result.append(", ");
                    }

                    result.append(typeArguments[i].getTypeName());
                }

                return result.append('>').toString();
            }

            @Override
            public String toString() {
                return getTypeName();
            }
        }

        /**
         * Produced by {@link #resolvePropertyType} when a generic array's component resolves to something other than a
         * {@link Class}, such as the parameterized component {@code List<String>} in {@code List<String>[]}.
         * Preserves the substituted component for reflection and {@link TypeFactory} resolution.
         */
        private static final class ResolvedGenericArrayType implements GenericArrayType {
            private final java.lang.reflect.Type componentType;

            ResolvedGenericArrayType(final java.lang.reflect.Type componentType) {
                this.componentType = componentType;
            }

            @Override
            public java.lang.reflect.Type getGenericComponentType() {
                return componentType;
            }

            @Override
            public boolean equals(final Object obj) {
                return obj instanceof GenericArrayType other && Objects.equals(componentType, other.getGenericComponentType());
            }

            @Override
            public int hashCode() {
                return Objects.hashCode(componentType);
            }

            @Override
            public String getTypeName() {
                return componentType.getTypeName() + "[]";
            }

            @Override
            public String toString() {
                return getTypeName();
            }
        }

        private static final class ResolvedWildcardType implements WildcardType {
            private final java.lang.reflect.Type[] upperBounds;
            private final java.lang.reflect.Type[] lowerBounds;

            ResolvedWildcardType(final java.lang.reflect.Type[] upperBounds, final java.lang.reflect.Type[] lowerBounds) {
                this.upperBounds = upperBounds.clone();
                this.lowerBounds = lowerBounds.clone();
            }

            @Override
            public java.lang.reflect.Type[] getUpperBounds() {
                return upperBounds.clone();
            }

            @Override
            public java.lang.reflect.Type[] getLowerBounds() {
                return lowerBounds.clone();
            }

            @Override
            public boolean equals(final Object obj) {
                return obj instanceof WildcardType other && Arrays.equals(upperBounds, other.getUpperBounds())
                        && Arrays.equals(lowerBounds, other.getLowerBounds());
            }

            @Override
            public int hashCode() {
                return Arrays.hashCode(upperBounds) ^ Arrays.hashCode(lowerBounds);
            }

            @Override
            public String getTypeName() {
                if (lowerBounds.length > 0) {
                    return "? super " + lowerBounds[0].getTypeName();
                }

                return upperBounds.length == 0 || upperBounds[0] == Object.class ? "?" : "? extends " + upperBounds[0].getTypeName();
            }

            @Override
            public String toString() {
                return getTypeName();
            }
        }

        private <T> Type<T> getType(java.lang.reflect.Type genericType, final Map<TypeVariable<?>, java.lang.reflect.Type> typeParamArgMap) {
            return Type.of(resolvePropertyType(genericType, typeParamArgMap));
        }

        /**
         * Returns a hash code value for this PropInfo.
         *
         * <p>The hash code is computed based on the property name and field.
         * Two PropInfo objects with the same name and field will have the same hash code.</p>
         *
         * @return the hash code value
         */
        @Override
        public int hashCode() {
            return ((name == null) ? 0 : name.hashCode()) * 31 + ((field == null) ? 0 : field.hashCode());
        }

        /**
         * Compares this PropInfo with another object for equality.
         *
         * <p>Two PropInfo objects are considered equal if they have the same name
         * and the same field. This allows PropInfo objects to be used as keys
         * in collections.</p>
         *
         * @param obj the object to compare with
         * @return {@code true} if the objects are equal, {@code false} otherwise
         */
        @Override
        public boolean equals(final Object obj) {
            return this == obj || ((obj instanceof PropInfo) && ((PropInfo) obj).name.equals(name) && N.equals(((PropInfo) obj).field, field));
        }

        /**
         * Returns a string representation of this PropInfo.
         *
         * <p>The string representation is simply the property name, which provides
         * a concise and useful representation for debugging and logging.</p>
         *
         * @return the property name
         */
        @Override
        public String toString() {
            return name;
        }
    }

    /**
     * A {@link PropInfo} that reads and writes the property through reflectasm-generated accessors
     * instead of {@code java.lang.reflect}.
     *
     * <p>Instances are created only when reflectasm is available on the classpath and ASM support is
     * enabled for the bean. Behavior is identical to {@code PropInfo}; only the access mechanism differs.
     * When an accessor index cannot be resolved (index {@code -1}) the corresponding reflective path is
     * used as the fallback.</p>
     *
     * @see PropInfo
     */
    @SuppressFBWarnings("EQ_DOESNT_OVERRIDE_EQUALS")
    static class ASMPropInfo extends PropInfo { //NOSONAR
        /** Generated accessor for the declaring class of the getter, or {@code null} if there is no getter. */
        final com.esotericsoftware.reflectasm.MethodAccess getMethodAccess;
        /** Generated accessor for the declaring class of the setter, or {@code null} if there is no setter. */
        final com.esotericsoftware.reflectasm.MethodAccess setMethodAccess;
        /** Generated accessor for the declaring class of the field, or {@code null} if there is no field. */
        final com.esotericsoftware.reflectasm.FieldAccess fieldAccess;

        /** Index of the getter within {@link #getMethodAccess}, or {@code -1} if unavailable. */
        final int getMethodAccessIndex;
        /** Index of the setter within {@link #setMethodAccess}, or {@code -1} if unavailable. */
        final int setMethodAccessIndex;
        /** Index of the field within {@link #fieldAccess}, or {@code -1} if the field is not public, is final, or is not directly gettable. */
        final int fieldAccessIndex;

        /**
         * Constructs an ASM-backed PropInfo, resolving the reflectasm accessors and their indexes.
         *
         * @param name the name of the property
         * @param field the field object (may be {@code null} for method-only properties)
         * @param getMethod the getter method (may be {@code null} for write-only properties)
         * @param setMethod the setter method (may be {@code null} for read-only properties)
         * @param jsonXmlConfig configuration for JSON/XML processing
         * @param classAnnotations annotations from the declaring class
         * @param fieldOrder the ordinal position of this field
         * @param isImmutableBean whether this property belongs to an immutable bean
         * @param isByBuilder whether this property uses builder pattern
         * @param idPropNames list of property names that are identifiers
         * @param readOnlyIdPropNames list of property names that are read-only identifiers
         * @param typeParamArgMap mapping of type variables to actual types for generic resolution
         */
        ASMPropInfo(final String name, final Field field, final Method getMethod, final Method setMethod, final JsonXmlConfig jsonXmlConfig,
                final ImmutableMap<Class<? extends Annotation>, Annotation> classAnnotations, final int fieldOrder, final boolean isImmutableBean,
                final boolean isByBuilder, final List<String> idPropNames, final List<String> readOnlyIdPropNames,
                final Map<TypeVariable<?>, java.lang.reflect.Type> typeParamArgMap) {
            super(name, field, getMethod, setMethod, jsonXmlConfig, classAnnotations, fieldOrder, isImmutableBean, isByBuilder, idPropNames,
                    readOnlyIdPropNames, typeParamArgMap);

            getMethodAccess = getMethod == null ? null : com.esotericsoftware.reflectasm.MethodAccess.get(getMethod.getDeclaringClass());
            setMethodAccess = setMethod == null ? null : com.esotericsoftware.reflectasm.MethodAccess.get(setMethod.getDeclaringClass());
            fieldAccess = field == null ? null : com.esotericsoftware.reflectasm.FieldAccess.get(field.getDeclaringClass());

            getMethodAccessIndex = getMethod == null ? -1 : getMethodAccess.getIndex(getMethod.getName(), 0);
            setMethodAccessIndex = setMethod == null ? -1 : setMethodAccess.getIndex(setMethod.getName(), setMethod.getParameterTypes());
            fieldAccessIndex = (field == null || !isFieldGettable || !Modifier.isPublic(field.getModifiers()) || Modifier.isFinal(field.getModifiers())) ? -1
                    : fieldAccess.getIndex(field.getName());
        }

        /**
         * {@inheritDoc}
         *
         * <p>This override uses reflectasm ({@code FieldAccess}/{@code MethodAccess}) for faster
         * property access than standard reflection.</p>
         */
        @SuppressWarnings("unchecked")
        @Override
        public <T> T getPropValue(final Object obj) {
            if (isImmutableBean && obj instanceof Object[]) {
                return (T) ((Object[]) obj)[fieldOrder];
            }

            return (T) ((fieldAccessIndex > -1) ? fieldAccess.get(obj, fieldAccessIndex) : getMethodAccess.invoke(obj, getMethodAccessIndex));
        }

        /**
         * {@inheritDoc}
         *
         * <p>This override uses reflectasm ({@code FieldAccess}/{@code MethodAccess}) for faster
         * property access than standard reflection.</p>
         * @throws UnsupportedOperationException if this property is read-only and cannot accept {@code propValue}
         */
        @SuppressFBWarnings
        @Override
        public void setPropValue(final Object obj, Object propValue) throws UnsupportedOperationException {
            if (isReadOnlyProperty) {
                throw new UnsupportedOperationException(readOnlyMessage());
            }

            if (isImmutableBean) {
                if (!isByBuilder) {
                    // Coalesce nulls to the type default; otherwise primitive-typed record/builder
                    // components NPE on auto-unbox during canonical-constructor invocation.
                    ((Object[]) obj)[fieldOrder] = propValue == null ? type.defaultValue() : propValue;

                    return;
                }

                if (setMethod == null) {
                    // Same as PropInfo.setPropValue: with no setter on the builder the value has nowhere to go, and
                    // the `field.set(obj, ..)` fallback below would write the BEAN's field into the BUILDER instance.
                    return;
                }
            }

            propValue = propValue == null ? type.defaultValue() : propValue;

            if (failureCountForSetProp > 100 && propValue != null && !clazz.isAssignableFrom(propValue.getClass())) {
                propValue = N.convert(propValue, jsonXmlType);

                if (isFieldSettable && fieldAccessIndex > -1) {
                    fieldAccess.set(obj, fieldAccessIndex, propValue);
                } else if (setMethodAccessIndex > -1) {
                    setMethodAccess.invoke(obj, setMethodAccessIndex, propValue);
                } else if (canSetFieldByGetMethod) {
                    Beans.setPropValueByGetter(obj, getMethod, propValue);
                } else {
                    try {
                        field.set(obj, propValue); //NOSONAR
                    } catch (final IllegalAccessException e) {
                        throw ExceptionUtil.toRuntimeException(e, true);
                    }
                }

                if (failureCountForSetProp > 0) {
                    //noinspection NonAtomicOperationOnVolatileField
                    failureCountForSetProp--; // NOSONAR
                }
            } else {
                try {
                    if (isFieldSettable && fieldAccessIndex > -1) {
                        fieldAccess.set(obj, fieldAccessIndex, propValue);
                    } else if (setMethodAccessIndex > -1) {
                        setMethodAccess.invoke(obj, setMethodAccessIndex, propValue);
                    } else if (canSetFieldByGetMethod) {
                        Beans.setPropValueByGetter(obj, getMethod, propValue);
                    } else {
                        field.set(obj, propValue); //NOSONAR
                    }

                    if (failureCountForSetProp > 0) {
                        //noinspection NonAtomicOperationOnVolatileField
                        failureCountForSetProp--; // NOSONAR
                    }
                } catch (final IllegalAccessException e) {
                    throw ExceptionUtil.toRuntimeException(e, true);
                } catch (final Exception e) {
                    // Why don't check the value type first before set? Because it's expected 99% chance set will success.
                    // Checking the value type first may not improve performance.

                    if (failureCountForSetProp < 1000) {
                        //noinspection NonAtomicOperationOnVolatileField
                        failureCountForSetProp++; // NOSONAR
                    }

                    if (logger.isWarnEnabled() && (failureCountForSetProp % 100 == 0)) {
                        logger.warn("Failed to set value for field: {} in class: {} with value type {}", field == null ? name : field.getName(),
                                ClassUtil.getClassName(declaringClass), propValue == null ? "null" : ClassUtil.getClassName(propValue.getClass()));
                    }

                    propValue = N.convert(propValue, jsonXmlType);

                    if (isFieldSettable && fieldAccessIndex > -1) {
                        fieldAccess.set(obj, fieldAccessIndex, propValue);
                    } else if (setMethodAccessIndex > -1) {
                        setMethodAccess.invoke(obj, setMethodAccessIndex, propValue);
                    } else if (canSetFieldByGetMethod) {
                        Beans.setPropValueByGetter(obj, getMethod, propValue);
                    } else {
                        try {
                            field.set(obj, propValue); //NOSONAR
                        } catch (final IllegalAccessException e2) {
                            throw ExceptionUtil.toRuntimeException(e2, true);
                        }
                    }
                }
            }
        }
    }

    /**
     * An immutable bundle of pre-rendered JSON fragments for one property name under one naming policy.
     *
     * <p>Serialization writes these {@code char[]} fragments straight to the output buffer, so the
     * quoting, the {@code ": "} separator and the {@code null} literal never have to be concatenated
     * per value. Instances are shared between naming policies that resolve to the same name.</p>
     */
    static class JsonNameTag {
        /** The bare name, e.g. {@code id}. */
        final char[] name;
        /** The bare name followed by {@code ": "}. */
        final char[] nameWithColon;
        /** The bare name followed by {@code ": null"}. */
        final char[] nameNull;
        /** The name wrapped in double quotes, e.g. {@code "id"}. */
        final char[] quotedName;
        /** The quoted name followed by {@code ": "}. */
        final char[] quotedNameWithColon;
        /** The quoted name followed by {@code ": null"}. */
        final char[] quotedNameNull;

        /**
         * Creates a JSON name tag and precomputes common JSON field name variants.
         *
         * <p>This constructor prepares raw and quoted forms of the name, including
         * variants with a trailing colon and {@code null} literal suffixes for fast
         * JSON output formatting.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * ParserUtil.JsonNameTag tag = new ParserUtil.JsonNameTag("id");
         * char[] quoted = tag.quotedName; // "\"id\""
         * }</pre>
         *
         * @param name the property name to prepare tag variants for
         */
        public JsonNameTag(final String name) {
            this.name = name.toCharArray();
            nameWithColon = (name + ": ").toCharArray();
            nameNull = (name + ": null").toCharArray();
            final String escapedName = EscapeUtil.escapeJson(name);
            quotedName = ("\"" + escapedName + "\"").toCharArray();
            quotedNameWithColon = ("\"" + escapedName + "\": ").toCharArray();
            quotedNameNull = ("\"" + escapedName + "\": null").toCharArray();
        }

        @Override
        public int hashCode() {
            return (name == null) ? 0 : N.hashCode(name);
        }

        @Override
        public boolean equals(final Object obj) {
            return obj == this || (obj instanceof JsonNameTag && N.equals(((JsonNameTag) obj).name, name));
        }

        @Override
        public String toString() {
            return N.toString(name);
        }
    }

    /**
     * An immutable bundle of pre-rendered XML fragments for one property name under one naming policy.
     *
     * <p>Two element styles are prepared: the {@code ep*} fragments use a generic
     * {@code <bean name="..."/>} or {@code <property name="..."/>} element, while the {@code named*}
     * fragments use the property name as the element name itself. Each style additionally provides a
     * variant carrying a {@code type} attribute and a self-closing {@code isNull="true"} variant.
     * Instances are shared between naming policies that resolve to the same name.</p>
     */
    static class XmlNameTag {
        /** The bare name, e.g. {@code age}. */
        final char[] name;
        /** Generic start tag, e.g. {@code <property name="age">}. */
        final char[] epStart;
        /** Generic start tag including the {@code type} attribute. */
        final char[] epStartWithType;
        /** Generic end tag, e.g. {@code </property>}. */
        final char[] epEnd;
        /** Generic self-closing null element, e.g. {@code <property name="age" isNull="true" />}. */
        final char[] epNull;
        /** Generic self-closing null element including the {@code type} attribute. */
        final char[] epNullWithType;
        /** Name-as-element start tag, e.g. {@code <age>}. */
        final char[] namedStart;
        /** Name-as-element start tag including the {@code type} attribute. */
        final char[] namedStartWithType;
        /** Name-as-element end tag, e.g. {@code </age>}. */
        final char[] namedEnd;
        /** Name-as-element self-closing null element, e.g. {@code <age isNull="true" />}. */
        final char[] namedNull;
        /** Name-as-element self-closing null element including the {@code type} attribute. */
        final char[] namedNullWithType;

        /**
         * Creates an XML name tag and precomputes common XML fragments for the given name.
         *
         * <p>When {@code isBean} is {@code true}, this tag uses {@code <bean name="...">}
         * elements. Otherwise it uses {@code <property name="...">} elements. The
         * {@code *WithType} fragment variants embed {@code typeName} as a {@code type="..."}
         * attribute. Both the name and the type name are escaped for attribute position (see
         * {@link #escapeXmlAttributeValue(String)}); the {@code named*} fragments, which put the name in
         * element-name position where XML has no escaping mechanism, use it verbatim.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * ParserUtil.XmlNameTag tag = new ParserUtil.XmlNameTag("age", "int", false);
         * char[] start = tag.namedStart; // "<age>"
         * }</pre>
         *
         * @param name the element/property name to encode
         * @param typeName the type name to include in {@code type} attributes
         * @param isBean whether to use bean-style tags instead of property-style tags
         */
        public XmlNameTag(final String name, final String typeName, final boolean isBean) {
            this.name = name.toCharArray();

            // The name is arbitrary text: @JsonXmlField(name = "..."), @JSONField(name = "...") and
            // @JsonProperty("...") each hand their value straight through. Interpolated verbatim it wrote
            // <property name="a&b">, which no XML reader accepts, and a name holding a tab/newline/carriage
            // return survived parsing but was renamed by attribute-value normalisation, so the property was
            // silently dropped on read. Element-name position (the named* fragments below) has no escaping
            // mechanism at all, so a name that is not a valid XML element name can only be rejected; that
            // check belongs to the writers, which alone know whether the named* or the ep* style is in use.
            final String nameAttr = escapeXmlAttributeValue(name);
            final String typeAttr = escapeXmlAttributeValue(typeName);

            if (isBean) {
                epStart = ("<bean name=\"" + nameAttr + "\">").toCharArray();
                epStartWithType = ("<bean name=\"" + nameAttr + "\" type=\"" + typeAttr + "\">").toCharArray();
                epEnd = ("</bean>").toCharArray();
                epNull = ("<bean name=\"" + nameAttr + "\" isNull=\"true\" />").toCharArray();
                epNullWithType = ("<bean name=\"" + nameAttr + "\" type=\"" + typeAttr + "\" isNull=\"true\" />").toCharArray();
            } else {
                epStart = ("<property name=\"" + nameAttr + "\">").toCharArray();
                epStartWithType = ("<property name=\"" + nameAttr + "\" type=\"" + typeAttr + "\">").toCharArray();
                epEnd = ("</property>").toCharArray();
                epNull = ("<property name=\"" + nameAttr + "\" isNull=\"true\" />").toCharArray();
                epNullWithType = ("<property name=\"" + nameAttr + "\" type=\"" + typeAttr + "\" isNull=\"true\" />").toCharArray();
            }

            namedStart = ("<" + name + ">").toCharArray();
            namedStartWithType = ("<" + name + " type=\"" + typeAttr + "\">").toCharArray();
            namedEnd = ("</" + name + ">").toCharArray();
            namedNull = ("<" + name + " isNull=\"true\" />").toCharArray();
            namedNullWithType = ("<" + name + " type=\"" + typeAttr + "\" isNull=\"true\" />").toCharArray();
        }

        /**
         * Escapes {@code value} so it can be written inside a double-quoted XML attribute, applying the same
         * substitutions {@link BufferedXmlWriter} applies to text: {@code &}, {@code <}, {@code >}, {@code "}
         * and {@code '} become the predefined entities and every character below {@code U+0020}, plus
         * {@code U+007F}, becomes a numeric character reference. The three escapes that matter here are
         * {@code &}, {@code <} and {@code "}, which make the document unreadable, and tab/newline/carriage
         * return, which an XML reader would otherwise normalise to spaces and so rename the property.
         *
         * @param value the attribute text to escape
         * @return {@code value} with the characters above replaced; the same String content when it holds none
         */
        private static String escapeXmlAttributeValue(final String value) {
            final BufferedXmlWriter writer = Objectory.createBufferedXmlWriter();

            try {
                writer.writeCharacter(value);

                return writer.toString();
            } catch (final IOException e) {
                // Unreachable: a buffer-backed BufferedXmlWriter never fails.
                throw new UncheckedIOException(e); //NOSONAR
            } finally {
                Objectory.recycle(writer);
            }
        }

        @Override
        public int hashCode() {
            return (name == null) ? 0 : N.hashCode(name);
        }

        @Override
        public boolean equals(final Object obj) {
            return obj == this || (obj instanceof XmlNameTag && N.equals(((XmlNameTag) obj).name, name));
        }

        @Override
        public String toString() {
            return N.toString(name);
        }
    }

    /**
     * Returns {@code true} for the nullable date text markers accepted by every formatted date reader (legacy,
     * java.time and Joda alike): a {@code null} reference, empty text, or the case-insensitive literal
     * {@code "null"}. These are the same markers the no-format date types treat as {@code null}; blank text
     * such as {@code " "} is deliberately not one of them and is left to the parser to reject.
     */
    private static boolean isNullDateText(final String strValue) {
        return strValue == null || strValue.isEmpty() || "null".equalsIgnoreCase(strValue);
    }

    /**
     * Returns {@code true} for the property types whose {@code numberFormat} reads use exact
     * {@code BigDecimal} parsing: {@code BigDecimal}, {@code BigInteger} and the integral primitives and
     * wrappers. Floating-point targets keep {@code DecimalFormat}'s default parse mode so {@code -0.0} survives,
     * and {@code Number}/{@code Object} targets keep it so the runtime class they receive does not change.
     */
    private static boolean isExactOrIntegralNumberType(final Class<?> cls) {
        return cls == java.math.BigDecimal.class || cls == java.math.BigInteger.class //
                || cls == int.class || cls == Integer.class //
                || cls == long.class || cls == Long.class //
                || cls == short.class || cls == Short.class //
                || cls == byte.class || cls == Byte.class;
    }

    /**
     * Reads and writes one date/time (or number-like) representation on behalf of a formatted property.
     *
     * <p>One implementation is registered per supported property class in
     * {@code PropInfo.propFuncMap}; {@link PropInfo#readPropValue(String)} and
     * {@link PropInfo#writePropValue(CharacterWriter, Object, JsonXmlSerConfig)} dispatch through it
     * whenever the property declares a date format. Implementations honor the property's
     * {@code dateFormat}, {@code timeZone} and the {@code "long"} epoch-millis marker.</p>
     *
     * @param <T> the value type handled by this reader/writer
     */
    interface DateTimeReaderWriter<T> {
        /**
         * Parses a string value into the target date/time type for the specified property.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DateTimeReaderWriter<java.util.Date> readerWriter =
         *         (DateTimeReaderWriter<java.util.Date>) PropInfo.propFuncMap.get(java.util.Date.class);
         * java.util.Date value = readerWriter.read(propInfo, "2026-01-01");
         * }</pre>
         *
         * @param propInfo metadata for the target property.
         * @param strValue the source string value to parse.
         * @return the parsed date/time value.
         */
        T read(PropInfo propInfo, String strValue);

        /**
         * Writes a date/time value to the character writer using the property's formatting settings.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * readerWriter.write(propInfo, value, writer);
         * }</pre>
         *
         * @param propInfo metadata for the target property.
         * @param x the date/time value to serialize.
         * @param writer the destination writer.
         * @throws IOException if writing to the destination fails.
         */
        void write(PropInfo propInfo, T x, CharacterWriter writer) throws IOException;
    }

    /**
     * Holds the Joda-Time zone and formatter derived from a property's date format and time zone.
     *
     * <p>Kept in a separate class so that the Joda-Time types are only loaded when Joda-Time is
     * actually present on the classpath.</p>
     */
    static class JodaDateTimeFormatterHolder {
        /** The Joda zone corresponding to the property's {@link TimeZone}. */
        final org.joda.time.DateTimeZone dtz;
        /** The formatter for the property's pattern, or {@code null} for the {@code "long"} epoch-millis format. */
        final org.joda.time.format.DateTimeFormatter dtf;

        /**
         * Builds the zone and, unless the format is the {@code "long"} epoch-millis marker, the pattern formatter.
         *
         * @param dateFormat the property's date format pattern, or {@code "long"} for epoch milliseconds
         * @param timeZone the property's time zone
         * @throws IllegalArgumentException if {@code dateFormat} is neither {@code "long"} nor a valid Joda pattern.
         */
        JodaDateTimeFormatterHolder(final String dateFormat, final TimeZone timeZone) throws IllegalArgumentException {
            dtz = org.joda.time.DateTimeZone.forTimeZone(timeZone);
            // "long" means epoch millis (isLongDateFormat); only the zone is needed then and
            // DateTimeFormat.forPattern would reject "long" as a pattern.
            dtf = "long".equalsIgnoreCase(dateFormat) ? null : org.joda.time.format.DateTimeFormat.forPattern(dateFormat).withZone(dtz);
        }
    }

}
