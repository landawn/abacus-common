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
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.TypeVariable;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

import com.landawn.abacus.annotation.AccessFieldByMethod;
import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.annotation.Column;
import com.landawn.abacus.annotation.Entity;
import com.landawn.abacus.annotation.Id;
import com.landawn.abacus.annotation.Internal;
import com.landawn.abacus.annotation.JsonXmlConfig;
import com.landawn.abacus.annotation.JsonXmlField;
import com.landawn.abacus.annotation.JsonXmlField.Expose;
import com.landawn.abacus.annotation.ReadOnly;
import com.landawn.abacus.annotation.ReadOnlyId;
import com.landawn.abacus.annotation.SuppressFBWarnings;
import com.landawn.abacus.annotation.Table;
import com.landawn.abacus.annotation.Transient;
import com.landawn.abacus.annotation.Type.EnumBy;
import com.landawn.abacus.annotation.Type.Scope;
import com.landawn.abacus.logging.Logger;
import com.landawn.abacus.logging.LoggerFactory;
import com.landawn.abacus.type.ObjectType;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.type.TypeFactory;
import com.landawn.abacus.util.Array;
import com.landawn.abacus.util.Beans;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.Dates;
import com.landawn.abacus.util.ExceptionUtil;
import com.landawn.abacus.util.ImmutableList;
import com.landawn.abacus.util.ImmutableMap;
import com.landawn.abacus.util.InternalUtil;
import com.landawn.abacus.util.Multiset;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.NamingPolicy;
import com.landawn.abacus.util.Numbers;
import com.landawn.abacus.util.ObjectPool;
import com.landawn.abacus.util.Splitter;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.Tuple.Tuple3;
import com.landawn.abacus.util.WD;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.stream.Stream;

/**
 * Utility class for parser-related operations, providing methods for handling
 * bean metadata, property information, and serialization/deserialization configurations.
 * 
 * <p>This class is marked as {@code @Internal} and is not intended for direct use
 * by application code. It provides low-level utilities for the parser framework.</p>
 * * <p><b>Usage Examples:</b></p>
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

    // ...
    private static final String GET = "get";

    private static final String SET = "set";

    private static final String IS = "is";

    private static final String HAS = "has";

    private static final Set<Class<?>> idTypeSet = N.asSet(int.class, Integer.class, long.class, Long.class, String.class, Timestamp.class, UUID.class);

    @SuppressWarnings("deprecation")
    private static final int POOL_SIZE = InternalUtil.POOL_SIZE;

    private static final int defaultNameIndex = NamingPolicy.LOWER_CAMEL_CASE.ordinal();

    // ...
    private static final Map<java.lang.reflect.Type, BeanInfo> beanInfoPool = new ObjectPool<>(POOL_SIZE);

    private ParserUtil() {
        // Singleton.
    }

    /**
     * Determines whether a field should be serialized to JSON or XML based on its modifiers and annotations.
     * 
     * <p>A field is considered serializable if it is not static and not explicitly marked as ignored
     * through various annotation mechanisms including {@code @JsonXmlField(ignore=true)},
     * {@code @JSONField(serialize=false)}, or {@code @JsonIgnore}.</p>
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
        if (field == null) {
            return true;
        }

        if (Modifier.isStatic(field.getModifiers()) || (field.isAnnotationPresent(JsonXmlField.class) && field.getAnnotation(JsonXmlField.class).ignore())) {
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

        if (jsonXmlConfig != null && N.notEmpty(jsonXmlConfig.ignoredFields())) {
            final String fieldName = field.getName();

            for (final String ignoreFieldName : jsonXmlConfig.ignoredFields()) {
                if (fieldName.equals(ignoreFieldName) || fieldName.matches(ignoreFieldName)) {
                    return false;
                }
            }
        }

        return true;
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
     *   <li>Default to {@code EnumBy.NAME}</li>
     * </ol>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Field statusField = MyBean.class.getDeclaredField("status");
     * JsonXmlConfig config = MyBean.class.getAnnotation(JsonXmlConfig.class);
     * EnumBy enumBy = ParserUtil.getEnumerated(statusField, config);
     * // Returns EnumBy.ORDINAL if specified in annotations
     * }</pre>
     * 
     * @param field the field to check for enumeration annotations
     * @param jsonXmlConfig the configuration that may contain a default enumeration setting
     * @return the enumeration strategy, never {@code null} (defaults to {@code EnumBy.NAME})
     */
    static EnumBy getEnumerated(final Field field, final JsonXmlConfig jsonXmlConfig) {
        if ((field != null) && (field.isAnnotationPresent(JsonXmlField.class) && field.getAnnotation(JsonXmlField.class).enumerated() != null)) {
            return field.getAnnotation(JsonXmlField.class).enumerated();
        }

        if (jsonXmlConfig != null && jsonXmlConfig.enumerated() != null) {
            return jsonXmlConfig.enumerated();
        }

        return EnumBy.NAME;
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
     * // tags[0].name = "firstName" (LOWER_CAMEL_CASE)
     * // tags[1].name = "FirstName" (UPPER_CAMEL_CASE)
     * // tags[2].name = "first_name" (LOWER_CASE_WITH_UNDERSCORES)
     * }</pre>
     * 
     * @param name the original name to convert
     * @return an array of JSON name tags for all naming policies
     */
    static JsonNameTag[] getJsonNameTags(final String name) {
        final JsonNameTag[] result = new JsonNameTag[NamingPolicy.values().length];

        for (final NamingPolicy np : NamingPolicy.values()) {
            result[np.ordinal()] = new JsonNameTag(convertName(name, np));
        }

        return result;
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
     * // tags[0].name = "firstName", typeName = "string", isBean = false
     * }</pre>
     * 
     * @param name the original name to convert
     * @param typeName the type name for XML serialization
     * @param isBean whether this represents a bean type
     * @return an array of XML name tags for all naming policies
     */
    static XmlNameTag[] getXmlNameTags(final String name, final String typeName, final boolean isBean) {
        final XmlNameTag[] result = new XmlNameTag[NamingPolicy.values().length];

        for (final NamingPolicy np : NamingPolicy.values()) {
            result[np.ordinal()] = new XmlNameTag(convertName(name, np), typeName, isBean);
        }

        return result;
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
     * @return an array of JSON name tags
     * @throws IllegalArgumentException if the custom name contains leading/trailing whitespace
     */
    static JsonNameTag[] getJsonNameTags(final String propName, final Field field) {
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

        final JsonNameTag[] result = new JsonNameTag[NamingPolicy.values().length];

        for (final NamingPolicy np : NamingPolicy.values()) {
            result[np.ordinal()] = new JsonNameTag(Strings.isEmpty(jsonXmlFieldName) ? convertName(propName, np) : jsonXmlFieldName);
        }

        return result;
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
     * @return an array of XML name tags
     * @throws IllegalArgumentException if the custom name contains leading/trailing whitespace
     */
    static XmlNameTag[] getXmlNameTags(final String propName, final Field field, final String typeName, final boolean isBean) {
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

        final XmlNameTag[] result = new XmlNameTag[NamingPolicy.values().length];

        for (final NamingPolicy np : NamingPolicy.values()) {
            result[np.ordinal()] = new XmlNameTag(Strings.isEmpty(jsonXmlFieldName) ? convertName(propName, np) : jsonXmlFieldName, typeName, isBean);
        }

        return result;
    }

    /**
     * Retrieves the aliases for a field from various annotation sources.
     * 
     * <p>This method checks for aliases from the following annotations:</p>
     * <ul>
     *   <li>{@code @JsonXmlField(alias={...})}</li>
     *   <li>{@code @JSONField(alternateNames={...})}</li>
     *   <li>{@code @JsonAlias(value={...})}</li>
     * </ul>
     * 
     * <p>The field's own name is automatically removed from the alias list if present.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Field field = MyBean.class.getDeclaredField("firstName");
     * String[] aliases = ParserUtil.getAliases(field);
     * // Returns ["first_name", "fname"] if specified in annotations
     * }</pre>
     * 
     * @param field the field to check for alias annotations
     * @return an array of aliases, or {@code null} if no aliases are defined
     */
    static String[] getAliases(final Field field) {
        String[] alias = null;

        if (field != null) {
            if (field.isAnnotationPresent(JsonXmlField.class) && N.notEmpty(field.getAnnotation(JsonXmlField.class).alias())) {
                alias = field.getAnnotation(JsonXmlField.class).alias();
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
        return namingPolicy == null || namingPolicy == NamingPolicy.NO_CHANGE || namingPolicy == NamingPolicy.LOWER_CAMEL_CASE ? name
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
     * int hash = ParserUtil.hashCode(chars, 0, 5);  // Hash of "hello"
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
     * annotations, and type details.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BeanInfo beanInfo = ParserUtil.getBeanInfo(MyBean.class);
     * List<PropInfo> properties = beanInfo.propInfoList;
     * }</pre>
     *
     * @param beanType the java type of the bean class to get information for
     * @return a BeanInfo instance containing metadata about the class
     * @throws IllegalArgumentException if the class is not a bean class (no properties)
     * @see BeanInfo
     */
    public static BeanInfo getBeanInfo(final java.lang.reflect.Type beanType) {
        Class<?> beanClass = null;

        if (beanType instanceof ParameterizedType pt && pt.getRawType() instanceof Class cls) {
            beanClass = cls;
        } else {
            beanClass = (Class<?>) beanType;
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
     * @throws IllegalArgumentException if the class is not a bean class (no properties)
     * @see BeanInfo
     */
    public static BeanInfo getBeanInfo(final Class<?> beanClass) {
        return getBeanInfo(beanClass, beanClass);
    }

    private static BeanInfo getBeanInfo(final Class<?> beanClass, java.lang.reflect.Type javaType) {
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
     * <p>For internal test only.</p>
     * 
     * Retrieves or creates a {@link BeanInfo} instance for the specified class,
     * optionally supporting ASM-based property access.
     * 
     * <p>This method is similar to {@link #getBeanInfo(java.lang.reflect.Type)} but allows specifying
     * whether ASM support is enabled, which can improve performance for certain operations.</p>
     *
     * @param beanType the java type of the bean class to get information for
     * @param isASMSupported whether ASM support is enabled
     * @return a BeanInfo instance containing metadata about the class
     * @see BeanInfo
     * @deprecated
     */
    @Deprecated
    @Internal
    static BeanInfo getBeanInfo(final java.lang.reflect.Type beanType, final boolean isASMSupported) {
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
     * Container class holding comprehensive metadata about a bean class.
     * 
     * <p>BeanInfo provides access to all property information, annotations, naming policies,
     * and other metadata needed for serialization/deserialization operations.</p>
     * 
     * <p>This class implements {@link JSONReader.SymbolReader} for efficient property lookup
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
    public static class BeanInfo implements JSONReader.SymbolReader {

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

        final NamingPolicy jsonXmlNamingPolicy;

        final Exclusion jsonXmlSeriExclusion;

        final String typeName;

        final JsonNameTag[] jsonNameTags;

        final XmlNameTag[] xmlNameTags;

        final PropInfo[] propInfos;

        final PropInfo[] jsonXmlSerializablePropInfos;

        final PropInfo[] nonTransientSeriPropInfos;

        final PropInfo[] transientSeriPropInfos;

        final Set<String> transientSeriPropNameSet = N.newHashSet();

        private final Map<String, Optional<PropInfo>> propInfoMap;

        private final Map<String, List<PropInfo>> propInfoQueueMap;

        private final PropInfo[] propInfoArray;

        private final Map<Integer, PropInfo> hashPropInfoMap;

        /** Optional table name if this bean is mapped to a database table */
        public final Optional<String> tableName;

        private final Class<?>[] fieldTypes;
        private final Object[] defaultFieldValues;
        private final Constructor<?> noArgsConstructor;
        private final Constructor<?> allArgsConstructor;

        /** Whether this bean is immutable (e.g., a record or has no setters) */
        public final boolean isImmutable;
        private final boolean isByBuilder;
        private final Tuple3<Class<?>, ? extends Supplier<Object>, ? extends Function<Object, Object>> builderInfo;

        /** Whether this class is marked with @Entity or similar annotations */
        public final boolean isMarkedToBean;

        /**
         * Constructs a new BeanInfo for the specified class.
         * 
         * <p>This constructor analyzes the class structure, extracts property information,
         * processes annotations, and builds comprehensive metadata about the bean.</p>
         * 
         * @param beanClass the class to analyze
         * @param beanType the Java type of the class
         */
        BeanInfo(final Class<?> beanClass, final java.lang.reflect.Type beanType) {
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
         */
        @SuppressWarnings("deprecation")
        BeanInfo(final Class<?> beanClass, final java.lang.reflect.Type beanType, final boolean isASMSupported) {
            // Constructor implementation remains the same...
            annotations = ImmutableMap.wrap(getAnnotations(beanClass));
            simpleClassName = ClassUtil.getSimpleClassName(beanClass);
            canonicalClassName = ClassUtil.getCanonicalClassName(beanClass);
            clazz = (Class<Object>) beanClass;
            this.javaType = beanType;
            type = Type.of(beanType);
            typeName = type.name();

            final Map<TypeVariable<?>, java.lang.reflect.Type> typeParamArgMap = new HashMap<>();

            if (beanType instanceof ParameterizedType pType) {
                final java.lang.reflect.Type[] typeArgs = pType.getActualTypeArguments();
                final TypeVariable<?>[] typeParams = beanClass.getTypeParameters();

                for (int i = 0, len = typeParams.length; i < len; i++) {
                    typeParamArgMap.put(typeParams[i], typeArgs[i]);
                }
            }

            propNameList = Beans.getPropNameList(beanClass);

            boolean localIsImmutable = true;

            if (Beans.isRecordClass(beanClass)) {
                //noinspection DataFlowIssue
                localIsImmutable = true;
            } else if (Beans.isRegisteredXMLBindingClass(beanClass)) {
                localIsImmutable = false;
            } else {
                try {
                    final Object tmp = N.newInstance(beanClass);
                    Field field = null;
                    Method setMethod = null;

                    for (final String propName : propNameList) {
                        field = Beans.getPropField(beanClass, propName);
                        setMethod = Beans.getPropSetMethod(beanClass, propName);

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
            jsonXmlNamingPolicy = jsonXmlConfig == null || jsonXmlConfig.namingPolicy() == null ? NamingPolicy.LOWER_CAMEL_CASE : jsonXmlConfig.namingPolicy();
            jsonXmlSeriExclusion = jsonXmlConfig == null || jsonXmlConfig.exclusion() == null ? Exclusion.NULL : jsonXmlConfig.exclusion();

            final String name = Beans.formalizePropName(simpleClassName);
            jsonNameTags = getJsonNameTags(name);
            xmlNameTags = getXmlNameTags(name, typeName, true);

            final List<String> idPropNames = new ArrayList<>();
            final List<String> readOnlyIdPropNames = new ArrayList<>();

            if (beanClass.isAnnotationPresent(Id.class)) {
                final String[] values = beanClass.getAnnotation(Id.class).value();
                N.checkArgNotEmpty(values, "values for annotation @Id on Type/Class can't be null or empty");
                idPropNames.addAll(Arrays.asList(values));
            }

            if (beanClass.isAnnotationPresent(ReadOnlyId.class)) {
                final String[] values = beanClass.getAnnotation(ReadOnlyId.class).value();
                N.checkArgNotEmpty(values, "values for annotation @ReadOnlyId on Type/Class can't be null or empty");
                idPropNames.addAll(Arrays.asList(values));
                readOnlyIdPropNames.addAll(Arrays.asList(values));
            }

            final List<PropInfo> seriPropInfoList = new ArrayList<>();
            final List<PropInfo> nonTransientSeriPropInfoList = new ArrayList<>();
            final List<PropInfo> transientSeriPropInfoList = new ArrayList<>();

            propInfos = new PropInfo[propNameList.size()];
            propInfoMap = new ObjectPool<>((propNameList.size() + 1) * 2);
            propInfoQueueMap = new ObjectPool<>((propNameList.size() + 1) * 2);
            hashPropInfoMap = new ObjectPool<>((propNameList.size() + 1) * 2);

            PropInfo propInfo = null;
            int idx = 0;

            final Multiset<Integer> multiSet = N.newMultiset(propNameList.size() + 16);
            int maxLength = 0;
            Field field = null;
            Method getMethod = null;
            Method setMethod = null;

            for (final String propName : propNameList) {
                field = Beans.getPropField(beanClass, propName);
                getMethod = Beans.getPropGetMethod(beanClass, propName);
                setMethod = isByBuilder ? Beans.getPropSetMethod(builderInfo._1, propName) : Beans.getPropSetMethod(beanClass, propName);

                propInfo = ASMUtil.isASMAvailable() && isASMSupported
                        ? new ASMPropInfo(propName, field, getMethod, setMethod, jsonXmlConfig, annotations, idx, isImmutable, isByBuilder, idPropNames,
                                readOnlyIdPropNames, typeParamArgMap)
                        : new PropInfo(propName, field, getMethod, setMethod, jsonXmlConfig, annotations, idx, isImmutable, isByBuilder, idPropNames,
                                readOnlyIdPropNames, typeParamArgMap);

                propInfos[idx++] = propInfo;

                final Optional<PropInfo> propInfoOpt = Optional.of(propInfo);
                propInfoMap.put(propName, propInfoOpt);
                String jsonTagName = null;

                for (final JsonNameTag nameTag : propInfo.jsonNameTags) {
                    jsonTagName = String.valueOf(nameTag.name);

                    if (!propInfoMap.containsKey(jsonTagName)) {
                        propInfoMap.put(jsonTagName, propInfoOpt);
                    }
                }

                if (propInfo.columnName.isPresent() && !propInfoMap.containsKey(propInfo.columnName.get())) {
                    propInfoMap.put(propInfo.columnName.get(), propInfoOpt);

                    if (!propInfoMap.containsKey(propInfo.columnName.get().toLowerCase())) {
                        propInfoMap.put(propInfo.columnName.get().toLowerCase(), propInfoOpt);
                    }

                    if (!propInfoMap.containsKey(propInfo.columnName.get().toUpperCase())) {
                        propInfoMap.put(propInfo.columnName.get().toUpperCase(), propInfoOpt);
                    }
                }

                final ImmutableList<String> aliases = propInfo.aliases;

                if (N.notEmpty(aliases)) {
                    for (final String str : aliases) {
                        if (propInfoMap.containsKey(str)) {
                            throw new IllegalArgumentException("Can't set alias: " + str + " for property/field: " + propInfo.field + " because " + str
                                    + " is a property/field name in class: " + beanClass);
                        }

                        propInfoMap.put(str, propInfoOpt);
                    }
                }

                if (!isJsonXmlSerializable(propInfo.field, jsonXmlConfig)) {
                    if (propInfo.jsonXmlExpose != JsonXmlField.Expose.DEFAULT) {
                        throw new IllegalArgumentException(
                                "JsonXmlField.Expose can't be: " + propInfo.jsonXmlExpose + " for non-serializable field: " + propInfo.field);
                    }

                    // skip
                } else {
                    seriPropInfoList.add(propInfo);

                    if (propInfo.isTransient) {
                        if (propInfo.jsonXmlExpose != JsonXmlField.Expose.DEFAULT) {
                            throw new IllegalArgumentException(
                                    "JsonXmlField.Expose can't be: " + propInfo.jsonXmlExpose + " for transient field: " + propInfo.field);
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

            jsonXmlSerializablePropInfos = seriPropInfoList.toArray(new PropInfo[0]);
            nonTransientSeriPropInfos = nonTransientSeriPropInfoList.toArray(new PropInfo[0]);
            transientSeriPropInfos = transientSeriPropInfoList.toArray(new PropInfo[0]);

            propInfoArray = new PropInfo[maxLength + 1];

            for (final PropInfo e : propInfos) {
                hashPropInfoMap.put(ParserUtil.hashCode(e.jsonNameTags[defaultNameIndex].name), e);

                if (multiSet.getCount(e.jsonNameTags[defaultNameIndex].name.length) == 1) {
                    propInfoArray[e.jsonNameTags[defaultNameIndex].name.length] = e;
                }
            }

            propInfoList = ImmutableList.wrap(N.asList(propInfos));

            final List<PropInfo> tmpIdPropInfoList = N.filter(propInfos, it -> it.isMarkedToId);

            if (N.isEmpty(tmpIdPropInfoList)) {
                tmpIdPropInfoList.addAll(N.filter(propInfos, it -> "id".equals(it.name) && idTypeSet.contains(it.clazz)));
            }

            idPropInfoList = ImmutableList.wrap(tmpIdPropInfoList);
            idPropNameList = ImmutableList.wrap(N.map(idPropInfoList, it -> it.name));

            readOnlyIdPropInfoList = ImmutableList.wrap(N.filter(propInfos, it -> it.isMarkedToReadOnlyId));
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

            isMarkedToBean = tmpIsMarkedToBean;
        }

        /**
         * Gets property information by property name.
         *
         * <p>This method supports various property name formats including:</p>
         * <ul>
         *   <li>Direct property names</li>
         *   <li>Nested property paths using dot notation (e.g., "address.street")</li>
         *   <li>Aliases defined via annotations</li>
         *   <li>Column names from database mappings</li>
         * </ul>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * PropInfo nameInfo = beanInfo.getPropInfo("name");
         * PropInfo nestedInfo = beanInfo.getPropInfo("address.city");
         * }</pre>
         *
         * @param propName the property name to look up
         * @return the PropInfo for the property, or {@code null} if not found
         * @see PropInfo
         */
        @Override
        public PropInfo getPropInfo(final String propName) {
            // Implementation remains the same...
            Optional<PropInfo> propInfoOpt = propInfoMap.get(propName);

            if (propInfoOpt == null) {
                PropInfo propInfo = null;

                final Method method = Beans.getPropGetMethod(clazz, propName);

                if (method != null) {
                    propInfoOpt = propInfoMap.get(Beans.getPropNameByMethod(method));
                }

                if (propInfoOpt == null) {
                    for (final Map.Entry<String, Optional<PropInfo>> entry : propInfoMap.entrySet()) { //NOSONAR
                        if (isPropName(clazz, propName, entry.getKey())) {
                            propInfoOpt = entry.getValue();

                            break;
                        }
                    }

                    if ((propInfoOpt == null) && !propName.equalsIgnoreCase(Beans.formalizePropName(propName))) {
                        propInfo = getPropInfo(Beans.formalizePropName(propName));

                        if (propInfo != null) {
                            propInfoOpt = Optional.of(propInfo);
                        }
                    }
                }

                // set method mask to avoid querying next time.
                if (propInfoOpt == null) {
                    propInfoOpt = Optional.empty();
                } else {
                    if (propInfoOpt.isEmpty()) {
                        // ignore.
                    } else {
                        propInfo = propInfoOpt.orElseThrow();
                        hashPropInfoMap.put(ParserUtil.hashCode(propInfo.jsonNameTags[defaultNameIndex].name), propInfo);
                    }
                }

                propInfoMap.put(propName, propInfoOpt);
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
         * @return the property value, or default value if property is null
         * @throws RuntimeException if no getter method found for the property
         */
        @SuppressWarnings("unchecked")
        public <T> T getPropValue(final Object obj, final String propName) {
            final PropInfo propInfo = getPropInfo(propName);

            if (propInfo == null) {
                final List<PropInfo> propInfoQueue = getPropInfoQueue(propName);

                if (propInfoQueue.size() == 0) {
                    throw new RuntimeException("No getter method found with property name: " + propName + " in class: " + clazz.getCanonicalName());
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
         * @throws IllegalArgumentException if no setter method found and ignoreUnmatchedProperty is false
         */
        public void setPropValue(final Object obj, final String propName, final Object propValue) {
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
         * @param ignoreUnmatchedProperty if {@code true}, silently ignore properties that don't exist
         * @return {@code true} if the property was set, {@code false} if it was ignored
         * @throws IllegalArgumentException if no setter found and ignoreUnmatchedProperty is false
         */
        @SuppressWarnings("rawtypes")
        public boolean setPropValue(final Object obj, final String propName, final Object propValue, final boolean ignoreUnmatchedProperty) {
            PropInfo propInfo = getPropInfo(propName);

            if (propInfo == null) {
                final List<PropInfo> propInfoQueue = getPropInfoQueue(propName);

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
                                    subPropValue = N.newInstance(propInfo.type.getElementType().clazz());
                                    final Collection c = N.newCollection((Class) propInfo.type.clazz());
                                    c.add(subPropValue);
                                    propInfo.setPropValue(propBean, c);
                                } else {
                                    // TODO what's about if propInfo.clazz is immutable (Record)?
                                    // For example: set "account.Name.firstName" key in Beans.map2Bean, if Account.Name is a Record?
                                    subPropValue = N.newInstance(propInfo.clazz);
                                    propInfo.setPropValue(propBean, subPropValue);
                                }
                            } else if (propInfo.type.isCollection()) {
                                final Collection c = (Collection) subPropValue;

                                if (c.size() == 0) {
                                    subPropValue = N.newCollection((Class<Collection>) propInfo.type.getElementType().clazz());
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
         */
        public void setPropValue(final Object obj, final PropInfo propInfoFromOtherBean, final Object propValue) {
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
         * @param ignoreUnmatchedProperty if {@code true}, silently ignore properties that don't exist
         * @return {@code true} if the property was set, {@code false} if it was ignored
         * @throws RuntimeException if no setter found and ignoreUnmatchedProperty is false
         */
        public boolean setPropValue(final Object obj, final PropInfo propInfoFromOtherBean, final Object propValue, final boolean ignoreUnmatchedProperty) {
            if (propInfoFromOtherBean.aliases.isEmpty()) {
                return setPropValue(obj, propInfoFromOtherBean.name, propValue, ignoreUnmatchedProperty);
            } else {
                if (!setPropValue(obj, propInfoFromOtherBean.name, propValue, true)) {
                    for (final String alias : propInfoFromOtherBean.aliases) {
                        if (setPropValue(obj, alias, propValue, true)) {
                            return true;
                        }
                    }
                }

                if (!ignoreUnmatchedProperty) {
                    throw new RuntimeException(
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
         * @throws RuntimeException if the property name exceeds 128 characters
         */
        private boolean isPropName(final Class<?> cls, String inputPropName, final String propNameByMethod) {
            if (inputPropName.length() > 128) {
                throw new RuntimeException("The property name exceed 128: " + inputPropName);
            }

            inputPropName = inputPropName.trim();

            return inputPropName.equalsIgnoreCase(propNameByMethod) || inputPropName.replace(WD.UNDERSCORE, Strings.EMPTY).equalsIgnoreCase(propNameByMethod)
                    || inputPropName.equalsIgnoreCase(ClassUtil.getSimpleClassName(cls) + WD._PERIOD + propNameByMethod)
                    || (inputPropName.startsWith(GET) && inputPropName.substring(3).equalsIgnoreCase(propNameByMethod))
                    || (inputPropName.startsWith(SET) && inputPropName.substring(3).equalsIgnoreCase(propNameByMethod))
                    || (inputPropName.startsWith(IS) && inputPropName.substring(2).equalsIgnoreCase(propNameByMethod))
                    || (inputPropName.startsWith(HAS) && inputPropName.substring(2).equalsIgnoreCase(propNameByMethod));
        }

        /**
         * Gets a queue of property information for nested property paths.
         * 
         * <p>This method parses dot-separated property paths and returns a list of
         * PropInfo objects representing each level of the path.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * List<PropInfo> queue = beanInfo.getPropInfoQueue("address.street.name");
         * // Returns [PropInfo(address), PropInfo(street), PropInfo(name)]
         * }</pre>
         *
         * @param propName the property path (e.g., "address.street")
         * @return a list of PropInfo objects for each level, or empty list if invalid
         */
        public List<PropInfo> getPropInfoQueue(final String propName) {
            List<PropInfo> propInfoQueue = propInfoQueueMap.get(propName);

            if (propInfoQueue == null) {
                propInfoQueue = new ArrayList<>();

                final String[] strs = Splitter.with(PROP_NAME_SEPARATOR).splitToArray(propName);

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
                            propClass = propInfo.type.getElementType().clazz();
                        } else {
                            propClass = propInfo.clazz;
                        }
                    }
                }

                propInfoQueue = N.isEmpty(propInfoQueue) ? N.emptyList() : ImmutableList.wrap(propInfoQueue);

                propInfoQueueMap.put(propName, propInfoQueue);
            }

            return propInfoQueue;
        }

        /**
         * Reads property information from a character buffer for efficient parsing.
         * 
         * <p>This method is used internally by parsers for fast property lookup
         * during deserialization. It uses hash-based lookup for optimal performance.</p>
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

            if (propInfo != null) {
                final char[] tmp = propInfo.jsonNameTags[defaultNameIndex].name;

                if (tmp.length == len) {
                    for (int i = 0; i < len; i++) {
                        if (cbuf[i + fromIndex] == tmp[i]) {
                            // continue;
                        } else {
                            return null;
                        }
                    }
                }
            }

            return propInfo;
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
        <T> T newInstance() {
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
        <T> T newInstance(final Object... args) {
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
            return isImmutable ? (builderInfo != null ? builderInfo._2.get() : createArgsForConstructor()) : N.newInstance(clazz);
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
         * // Populate intermediate object...
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

            return isImmutable ? (builderInfo != null ? (T) builderInfo._3.apply(result) : newInstance(((Object[]) result))) : (T) result;
        }

        /**
         * Creates an array of default values for all constructor arguments.
         * 
         * <p>This method is used internally when constructing immutable beans without builders.</p>
         *
         * @return an array of default values matching the all-args constructor
         * @throws UnsupportedOperationException if no all-args constructor exists
         */
        private Object[] createArgsForConstructor() {
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
     * // Get property value
     * PropInfo propInfo = ... // obtained from BeanInfo
     * Object value = propInfo.getPropValue(myObject);
     * 
     * // Set property value with automatic type conversion
     * propInfo.setPropValue(myObject, "new value");
     * }</pre>
     * 
     * @see BeanInfo
     * @see Type
     * @see JsonXmlField
     */
    public static class PropInfo {

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

        /**
         * Immutable map of all annotations present on this property.
         * Includes annotations from the field, getter, and setter.
         */
        public final ImmutableMap<Class<? extends Annotation>, Annotation> annotations;

        /**
         * Array of JSON name tags for custom JSON field naming.
         */
        final JsonNameTag[] jsonNameTags;

        /**
         * Array of XML name tags for custom XML element/attribute naming.
         */
        final XmlNameTag[] xmlNameTags;

        /**
         * Indicates whether the field is accessible for direct read access.
         * True if the field exists, is accessible, and not marked for method-only access.
         */
        final boolean isFieldAccessible;

        /**
         * Indicates whether the field is settable directly.
         * True if the field is accessible and not final (and not in a builder pattern).
         */
        final boolean isFieldSettable;

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
         * Null if no date format is specified.
         */
        final DateTimeFormatter dateTimeFormatter;

        /**
         * Indicates whether this property should be serialized as raw JSON value.
         * When {@code true}, the value is written directly without quotes or escaping.
         */
        final boolean isJsonRawValue;

        /**
         * Holder for Joda-Time formatter, if Joda-Time is available.
         * Null if Joda-Time is not in the classpath.
         */
        final JodaDateTimeFormatterHolder jodaDTFH;

        /**
         * Indicates whether the date format is "long" (epoch milliseconds).
         */
        final boolean isLongDateFormat;

        /**
         * The number format for formatting/parsing numeric values.
         * Null if no specific format is specified.
         */
        final NumberFormat numberFormat;

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
         */
        public final Expose jsonXmlExpose;

        /**
         * Indicates whether this property is marked as an identifier (primary key).
         * True if annotated with @Id or listed in id property names.
         */
        public final boolean isMarkedToId;

        /**
         * Indicates whether this property is marked as a read-only identifier.
         * True if annotated with @ReadOnlyId or is an @Id with @ReadOnly.
         */
        public final boolean isMarkedToReadOnlyId;

        /**
         * Indicates whether this property is explicitly marked as a database column.
         * True if annotated with @Column (from any supported persistence API).
         */
        public final boolean isMarkedToColumn;

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
         */
        @SuppressWarnings("deprecation")
        PropInfo(final String propName, final Field field, final Method getMethod, final Method setMethod, final JsonXmlConfig jsonXmlConfig,
                final ImmutableMap<Class<? extends Annotation>, Annotation> classAnnotations, final int fieldOrder, final boolean isImmutableBean,
                final boolean isByBuilder, final List<String> idPropNames, final List<String> readOnlyIdPropNames,
                final Map<TypeVariable<?>, java.lang.reflect.Type> typeParamArgMap) {
            // Constructor implementation remains the same...
            declaringClass = (Class<Object>) (field != null ? field.getDeclaringClass() : getMethod.getDeclaringClass());
            this.field = field;
            name = propName;
            aliases = ImmutableList.of(getAliases(field));
            this.getMethod = getMethod;
            this.setMethod = setMethod;
            annotations = ImmutableMap.wrap(getAnnotations());
            isTransient = annotations.containsKey(Transient.class) || annotations.keySet().stream().anyMatch(it -> it.getSimpleName().equals("Transient"))
                    || (field != null && Modifier.isTransient(field.getModifiers()));

            final Class<?> propClass = field == null ? (setMethod == null ? getMethod.getReturnType() : setMethod.getParameterTypes()[0]) : field.getType();

            type = getType(getAnnoType(this.field, propClass, jsonXmlConfig), this.field, this.getMethod, this.setMethod, declaringClass, typeParamArgMap);

            jsonXmlType = getType(getJsonXmlAnnoType(this.field, propClass, jsonXmlConfig), this.field, this.getMethod, this.setMethod, declaringClass,
                    typeParamArgMap);

            dbType = getType(getDBAnnoType(propClass), this.field, this.getMethod, this.setMethod, declaringClass, typeParamArgMap);

            clazz = type.clazz();

            jsonNameTags = getJsonNameTags(propName, field);
            xmlNameTags = getXmlNameTags(propName, field, jsonXmlType.name(), false);

            final boolean isAccessFieldByMethod = annotations.containsKey(AccessFieldByMethod.class) || classAnnotations.containsKey(AccessFieldByMethod.class);

            if (field != null && !isAccessFieldByMethod) {
                ClassUtil.setAccessibleQuietly(field, true);
            }

            isFieldAccessible = field != null && !isAccessFieldByMethod && field.isAccessible();
            isFieldSettable = isFieldAccessible && !Modifier.isFinal(field.getModifiers()) && !isByBuilder;

            final String timeZoneStr = Strings.trim(getTimeZone(field, jsonXmlConfig));
            final String dateFormatStr = Strings.trim(getDateFormat(field, jsonXmlConfig));
            dateFormat = Strings.isEmpty(dateFormatStr) ? null : dateFormatStr;
            timeZone = Strings.isEmpty(timeZoneStr) ? TimeZone.getDefault() : TimeZone.getTimeZone(timeZoneStr);
            zoneId = timeZone.toZoneId();
            dateTimeFormatter = Strings.isEmpty(dateFormat) ? null : DateTimeFormatter.ofPattern(dateFormat).withZone(zoneId);
            isJsonRawValue = isJsonRawValue(field);

            JodaDateTimeFormatterHolder tmpJodaDTFH = null;

            try {
                if (Class.forName("org.joda.time.DateTime") != null) {
                    tmpJodaDTFH = new JodaDateTimeFormatterHolder(dateFormat, timeZone);
                }
            } catch (final Throwable e) {
                // ignore.
            }

            jodaDTFH = tmpJodaDTFH;

            isLongDateFormat = Strings.isNotEmpty(dateFormat) && "long".equalsIgnoreCase(dateFormat);

            if (isLongDateFormat && (java.time.LocalTime.class.isAssignableFrom(clazz) || java.time.LocalDate.class.isAssignableFrom(clazz))) {
                throw new UnsupportedOperationException("Date format can't be 'long' for type java.time.LocalTime/LocalDate");
            }

            final String numberFormatStr = Strings.trim(getNumberFormat(field, jsonXmlConfig));
            numberFormat = Strings.isEmpty(numberFormatStr) ? null : new DecimalFormat(numberFormatStr);

            hasFormat = Strings.isNotEmpty(dateFormat) || numberFormat != null;

            jsonXmlExpose = field != null && field.isAnnotationPresent(JsonXmlField.class) ? field.getAnnotation(JsonXmlField.class).expose()
                    : JsonXmlField.Expose.DEFAULT;

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

            isMarkedToId = tmpIsMarkedToId;

            isMarkedToReadOnlyId = annotations.containsKey(ReadOnlyId.class) || (isMarkedToId && annotations.containsKey(ReadOnly.class))
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

            isMarkedToColumn = tmpIsMarkedToColumn;

            isSubEntity = !isMarkedToColumn && (type.isBean() || (type.isCollection() && type.getElementType().isBean()));

            columnName = Strings.isEmpty(tmpColumnName) ? Optional.empty() : Optional.ofNullable(tmpColumnName);

            tablePrefix = type.isBean() && clazz.getAnnotation(Table.class) != null ? Optional.ofNullable(clazz.getAnnotation(Table.class).alias())
                    : Optional.empty();

            canSetFieldByGetMethod = Beans.isRegisteredXMLBindingClass(declaringClass) && getMethod != null
                    && (Map.class.isAssignableFrom(getMethod.getReturnType()) || Collection.class.isAssignableFrom(getMethod.getReturnType()));

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
         * Person person = new Person("John", 30);
         * PropInfo nameProp = ... // property info for "name"
         * String name = nameProp.getPropValue(person);  // returns "John"
         * }</pre>
         *
         * @param <T> the expected type of the property value
         * @param obj the object to get the property value from
         * @return the property value, cast to type T
         * @throws RuntimeException if reflection access fails
         */
        @SuppressWarnings("unchecked")
        public <T> T getPropValue(final Object obj) {
            if (isImmutableBean && obj instanceof Object[]) {
                return (T) ((Object[]) obj)[fieldOrder];
            }

            try {
                return (T) (isFieldAccessible ? field.get(obj) : getMethod.invoke(obj));
            } catch (final Exception e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
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
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Person person = new Person();
         * PropInfo ageProp = ... // property info for "age"
         * ageProp.setPropValue(person, 25);
         * ageProp.setPropValue(person, "30");  // automatic conversion from String
         * }</pre>
         *
         * @param obj the object to set the property value on
         * @param propValue the value to set (will be converted if necessary)
         * @throws RuntimeException if reflection access fails or type conversion fails
         */
        @SuppressFBWarnings
        public void setPropValue(final Object obj, Object propValue) {
            if (isJsonRawValue && propValue != null && !clazz.isAssignableFrom(propValue.getClass())) {
                propValue = N.toJson(propValue);
            }

            if (isImmutableBean && !isByBuilder) {
                ((Object[]) obj)[fieldOrder] = propValue;
                return;
            }

            propValue = propValue == null ? type.defaultValue() : propValue;

            if (failureCountForSetProp > 100 && propValue != null && !clazz.isAssignableFrom(propValue.getClass())) {
                propValue = N.convert(propValue, jsonXmlType);

                try {
                    if (isFieldSettable) {
                        field.set(obj, propValue);
                    } else if (setMethod != null) {
                        setMethod.invoke(obj, propValue);
                    } else if (canSetFieldByGetMethod) {
                        Beans.setPropValueByGet(obj, getMethod, propValue);
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
                        Beans.setPropValueByGet(obj, getMethod, propValue);
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

                    if (logger.isWarnEnabled() && (failureCountForSetProp % 1000 == 0)) {
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
                            Beans.setPropValueByGet(obj, getMethod, propValue);
                        } else {
                            field.set(obj, propValue);
                        }
                    } catch (IllegalAccessException | InvocationTargetException e2) {
                        throw ExceptionUtil.toRuntimeException(e, true);
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
            // Static initializer block remains the same...
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
                    return Numbers.toLong(strValue);
                }

                @Override
                public void write(final PropInfo propInfo, final Long x, final CharacterWriter writer) throws IOException {
                    writer.write(x);
                }
            });

            propFuncMap.put(Long.class, new DateTimeReaderWriter<Long>() {
                @Override
                public Long read(final PropInfo propInfo, final String strValue) {
                    return strValue == null ? null : Numbers.toLong(strValue);
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

            // Static initializer block remains the same...
            propFuncMap.put(java.util.Date.class, new DateTimeReaderWriter<java.util.Date>() {
                @Override
                public java.util.Date read(final PropInfo propInfo, final String strValue) {
                    if (propInfo.isLongDateFormat) {
                        return new java.util.Date(Numbers.toLong(strValue));
                    } else {
                        return Dates.parseJUDate(strValue, propInfo.dateFormat, propInfo.timeZone);
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
                    if (propInfo.isLongDateFormat) {
                        final Calendar calendar = Calendar.getInstance();
                        calendar.setTimeInMillis(Numbers.toLong(strValue));
                        calendar.setTimeZone(propInfo.timeZone);
                        return calendar;
                    } else {
                        return Dates.parseCalendar(strValue, propInfo.dateFormat, propInfo.timeZone);
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
                    if (propInfo.isLongDateFormat) {
                        return new java.sql.Timestamp(Numbers.toLong(strValue));
                    } else {
                        return Dates.parseTimestamp(strValue, propInfo.dateFormat, propInfo.timeZone);
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
                    if (propInfo.isLongDateFormat) {
                        return new java.sql.Date(Numbers.toLong(strValue));
                    } else {
                        return Dates.parseDate(strValue, propInfo.dateFormat, propInfo.timeZone);
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
                    if (propInfo.isLongDateFormat) {
                        return new java.sql.Time(Numbers.toLong(strValue));
                    } else {
                        return Dates.parseTime(strValue, propInfo.dateFormat, propInfo.timeZone);
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
                    if (propInfo.isLongDateFormat) {
                        return new java.sql.Timestamp(Numbers.toLong(strValue)).toLocalDateTime();
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
                @Override
                public java.time.LocalDate read(final PropInfo propInfo, final String strValue) {
                    if (propInfo.isLongDateFormat) {
                        throw new UnsupportedOperationException("Date format can't be 'long' for type java.time.LocalDate");
                    } else {
                        return java.time.LocalDate.parse(strValue, propInfo.dateTimeFormatter);
                    }
                }

                @Override
                public void write(final PropInfo propInfo, final java.time.LocalDate x, final CharacterWriter writer) {
                    if (propInfo.isLongDateFormat) {
                        throw new UnsupportedOperationException("Date format can't be 'long' for type java.time.LocalDate");
                    } else {
                        propInfo.dateTimeFormatter.formatTo(x, writer);
                    }
                }
            });

            propFuncMap.put(java.time.LocalTime.class, new DateTimeReaderWriter<java.time.LocalTime>() {
                @Override
                public java.time.LocalTime read(final PropInfo propInfo, final String strValue) {
                    if (propInfo.isLongDateFormat) {
                        throw new UnsupportedOperationException("Date format can't be 'long' for type java.time.LocalTime");
                    } else {
                        return java.time.LocalTime.parse(strValue, propInfo.dateTimeFormatter);
                    }
                }

                @Override
                public void write(final PropInfo propInfo, final java.time.LocalTime x, final CharacterWriter writer) {
                    if (propInfo.isLongDateFormat) {
                        throw new UnsupportedOperationException("Date format can't be 'long' for type java.time.LocalTime");
                    } else {
                        propInfo.dateTimeFormatter.formatTo(x, writer);
                    }
                }
            });

            propFuncMap.put(java.time.ZonedDateTime.class, new DateTimeReaderWriter<java.time.ZonedDateTime>() {
                @Override
                public java.time.ZonedDateTime read(final PropInfo propInfo, final String strValue) {
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
         * PropInfo dateProp = ... // property with date format "yyyy-MM-dd"
         * Date date = (Date) dateProp.readPropValue("2023-12-25");
         * 
         * PropInfo longDateProp = ... // property with "long" date format
         * Date date2 = (Date) longDateProp.readPropValue("1703462400000");
         * }</pre>
         *
         * @param strValue the string value to parse
         * @return the parsed value in the appropriate type
         * @throws UnsupportedOperationException if date format is specified for unsupported types
         */
        public Object readPropValue(final String strValue) {
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
                    try {
                        return numberFormat.parse(strValue);
                    } catch (ParseException e) {
                        throw new RuntimeException("Failed to parse number value: " + strValue + " with format: " + numberFormat, e);
                    }
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
         * PropInfo dateProp = ... // property with date format
         * CharacterWriter writer = new CharacterWriter();
         * Date now = new Date();
         * dateProp.writePropValue(writer, now, config);
         * }</pre>
         *
         * @param writer the character writer to write to
         * @param x the value to write
         * @param config the serialization configuration
         * @throws IOException if an I/O error occurs during writing
         * @throws UnsupportedOperationException if date format is specified for unsupported types
         */
        public void writePropValue(final CharacterWriter writer, final Object x, final JSONXMLSerializationConfig<?> config) throws IOException {
            if (hasFormat) {
                if (x == null) {
                    writer.write(NULL_CHAR_ARRAY);
                } else if (dateFormat != null) {
                    final boolean isQuote = (config != null) && (config.getStringQuotation() != 0);

                    if (isQuote) {
                        writer.write(config.getStringQuotation());
                    }

                    @SuppressWarnings("rawtypes")
                    final DateTimeReaderWriter func = propFuncMap.get(clazz);

                    if (func == null) {
                        throw new UnsupportedOperationException("'DateFormat' annotation for field: " + field
                                + " is only supported for types: java.util.Date/Calendar, java.sql.Date/Time/Timestamp, java.time.LocalDateTime/LocalDate/LocalTime/ZonedDateTime, not supported for: "
                                + ClassUtil.getCanonicalClassName(clazz));
                    }

                    func.write(this, x, writer);

                    if (isQuote) {
                        writer.write(config.getStringQuotation());
                    }
                } else {
                    writer.write(numberFormat.format(x));
                }
            } else if (isJsonRawValue) {
                if (x == null) {
                    writer.write(NULL_CHAR_ARRAY);
                } else {
                    writer.write(jsonXmlType.stringOf(x));
                }
            } else {
                jsonXmlType.writeCharacter(writer, x, config);
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
         * If the same annotation is present in multiple places, the precedence order is:
         * field annotations, getter annotations, then setter annotations.</p>
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
         * @param field the field
         * @param propClass the property class
         * @param jsonXmlConfig the configuration
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
                    return ClassUtil.getCanonicalClassName(propClass) + "(" + (getEnumerated(field, jsonXmlConfig) == EnumBy.ORDINAL) + ")";
                }
            }

            if (jsonXmlConfig != null && propClass.isEnum()) {
                return ClassUtil.getCanonicalClassName(propClass) + "(" + (getEnumerated(field, jsonXmlConfig) == EnumBy.ORDINAL) + ")";
            }

            return null;
        }

        /**
         * Gets the annotated type name specifically for JSON/XML serialization.
         * 
         * @param field the field
         * @param propClass the property class
         * @param jsonXmlConfig the configuration
         * @return the type name or {@code null} if not specified
         */
        @SuppressWarnings("unused")
        private String getJsonXmlAnnoType(final Field field, final Class<?> propClass, final JsonXmlConfig jsonXmlConfig) {
            final JsonXmlField jsonXmlFieldAnno = getAnnotation(JsonXmlField.class);

            if (jsonXmlFieldAnno != null) {
                if (Strings.isNotEmpty(jsonXmlFieldAnno.type())) {
                    return jsonXmlFieldAnno.type();
                } else if (propClass.isEnum()) {
                    return ClassUtil.getCanonicalClassName(propClass) + "(" + (getEnumerated(field, jsonXmlConfig) == EnumBy.ORDINAL) + ")";
                }
            }

            if (jsonXmlConfig != null && propClass.isEnum()) {
                return ClassUtil.getCanonicalClassName(propClass) + "(" + (getEnumerated(field, jsonXmlConfig) == EnumBy.ORDINAL) + ")";
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

        private String getTypeName(final com.landawn.abacus.annotation.Type typeAnno, final Class<?> propClass) {
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
                return ClassUtil.getCanonicalClassName(propClass) + "(" + (typeAnno.enumerated() == EnumBy.ORDINAL) + ")";
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
                    return Type.of(typeParamArgMap.get(genericType));
                } else if (genericType instanceof GenericArrayType genericArrayType) {
                    if (typeParamArgMap.containsKey(genericArrayType.getGenericComponentType())) {
                        final Class<?> componentActual = (Class<?>) typeParamArgMap.get(genericArrayType.getGenericComponentType());

                        return Type.<T> of(Array.newInstance(componentActual, 0).getClass());
                    } else {
                        return Type.<T> of(Array.newInstance(getType(genericArrayType.getGenericComponentType(), typeParamArgMap).clazz(), 0).getClass());
                    }
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
                            final String str = annoType.substring(start, i);

                            if (!str.isEmpty() && Type.of(str).isObjectType() && !Type.of(pkgName + "." + str).isObjectType()) {
                                sb.append(pkgName).append(".").append(str);
                            } else {
                                sb.append(str);
                            }

                            sb.append(ch);
                            start = i + 1;
                        }
                    }

                    if (start < annoType.length()) {
                        final String str = annoType.substring(start);

                        if (Type.of(str).isObjectType() && !Type.of(pkgName + "." + str).isObjectType()) {
                            sb.append(pkgName).append(".").append(str);
                        } else {
                            sb.append(str);
                        }
                    }

                    localType = Type.of(sb.toString());
                }

                return localType;
            }
        }

        private <T> Type<T> getType(java.lang.reflect.Type genericType, final Map<TypeVariable<?>, java.lang.reflect.Type> typeParamArgMap) {
            if ((genericType instanceof TypeVariable) && typeParamArgMap.containsKey(genericType)) {
                return Type.of(typeParamArgMap.get(genericType));
            } else if (genericType instanceof GenericArrayType genericArrayType) {
                if (typeParamArgMap.containsKey(genericArrayType.getGenericComponentType())) {
                    final Class<?> componentActual = (Class<?>) typeParamArgMap.get(genericArrayType.getGenericComponentType());

                    return Type.<T> of(Array.newInstance(componentActual, 0).getClass());
                } else {
                    return Type.<T> of(Array.newInstance(getType(genericArrayType.getGenericComponentType(), typeParamArgMap).clazz(), 0).getClass());
                }
            } else if (genericType instanceof ParameterizedType parameterizedType) {
                final java.lang.reflect.Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
                final List<Type<?>> actualArgTypes = new ArrayList<>(actualTypeArguments.length);

                for (java.lang.reflect.Type actualTypeArgument : actualTypeArguments) {
                    actualArgTypes.add(getType(actualTypeArgument, typeParamArgMap));
                }

                final String typeName = ClassUtil.getCanonicalClassName((Class<?>) parameterizedType.getRawType())
                        + Stream.of(actualArgTypes).map(Type::name).join(",", "<", ">");

                return Type.of(typeName);
            } else {
                return Type.of(genericType);
            }
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
            return this == obj || ((obj instanceof PropInfo) && ((PropInfo) obj).name.equals(name)) && N.equals(((PropInfo) obj).field, field);
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

    @SuppressFBWarnings("EQ_DOESNT_OVERRIDE_EQUALS")
    static class ASMPropInfo extends PropInfo { //NOSONAR
        final com.esotericsoftware.reflectasm.MethodAccess getMethodAccess;
        final com.esotericsoftware.reflectasm.MethodAccess setMethodAccess;
        final com.esotericsoftware.reflectasm.FieldAccess fieldAccess;

        final int getMethodAccessIndex;
        final int setMethodAccessIndex;
        final int fieldAccessIndex;

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
            fieldAccessIndex = (field == null || !isFieldAccessible || !Modifier.isPublic(field.getModifiers()) || Modifier.isFinal(field.getModifiers())) ? -1
                    : fieldAccess.getIndex(field.getName());
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> T getPropValue(final Object obj) {
            return (T) ((fieldAccessIndex > -1) ? fieldAccess.get(obj, fieldAccessIndex) : getMethodAccess.invoke(obj, getMethodAccessIndex));
        }

        @SuppressFBWarnings
        @Override
        public void setPropValue(final Object obj, Object propValue) {
            if (isImmutableBean && !isByBuilder) {
                ((Object[]) obj)[fieldOrder] = propValue;

                return;
            }

            propValue = propValue == null ? type.defaultValue() : propValue;

            if (failureCountForSetProp > 100 && propValue != null && !clazz.isAssignableFrom(propValue.getClass())) {
                propValue = N.convert(propValue, jsonXmlType);

                if (isFieldSettable && fieldAccessIndex > -1) {
                    fieldAccess.set(obj, fieldAccessIndex, propValue);
                } else if (setMethodAccessIndex > -1) {
                    setMethodAccess.invoke(obj, setMethodAccessIndex, propValue);
                } else if (canSetFieldByGetMethod) {
                    Beans.setPropValueByGet(obj, getMethod, propValue);
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
                        Beans.setPropValueByGet(obj, getMethod, propValue);
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

                    if (logger.isWarnEnabled() && (failureCountForSetProp % 1000 == 0)) {
                        logger.warn("Failed to set value for field: {} in class: {} with value type {}", field == null ? name : field.getName(),
                                ClassUtil.getClassName(declaringClass), propValue == null ? "null" : ClassUtil.getClassName(propValue.getClass()));
                    }

                    propValue = N.convert(propValue, jsonXmlType);

                    if (isFieldSettable && fieldAccessIndex > -1) {
                        fieldAccess.set(obj, fieldAccessIndex, propValue);
                    } else if (setMethodAccessIndex > -1) {
                        setMethodAccess.invoke(obj, setMethodAccessIndex, propValue);
                    } else if (canSetFieldByGetMethod) {
                        Beans.setPropValueByGet(obj, getMethod, propValue);
                    } else {
                        try {
                            field.set(obj, propValue); //NOSONAR
                        } catch (final IllegalAccessException e2) {
                            throw ExceptionUtil.toRuntimeException(e, true);
                        }
                    }
                }
            }
        }
    }

    static class JsonNameTag {
        final char[] name;
        final char[] nameWithColon;
        final char[] nameNull;
        final char[] quotedName;
        final char[] quotedNameWithColon;
        final char[] quotedNameNull;

        public JsonNameTag(final String name) {
            this.name = name.toCharArray();
            nameWithColon = (name + ": ").toCharArray();
            nameNull = (name + ": null").toCharArray();
            quotedName = ("\"" + name + "\"").toCharArray();
            quotedNameWithColon = ("\"" + name + "\": ").toCharArray();
            quotedNameNull = ("\"" + name + "\": null").toCharArray();
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

    static class XmlNameTag {
        final char[] name;
        final char[] epStart;
        final char[] epStartWithType;
        final char[] epEnd;
        final char[] epNull;
        final char[] epNullWithType;
        final char[] namedStart;
        final char[] namedStartWithType;
        final char[] namedEnd;
        final char[] namedNull;
        final char[] namedNullWithType;

        public XmlNameTag(final String name, final String typeName, final boolean isBean) {
            this.name = name.toCharArray();

            final String typeAttr = typeName.replace("<", "&lt;").replace(">", "&gt;"); //NOSONAR

            if (isBean) {
                epStart = ("<bean name=\"" + name + "\">").toCharArray();
                epStartWithType = ("<bean name=\"" + name + "\" type=\"" + typeAttr + "\">").toCharArray();
                epEnd = ("</bean>").toCharArray();
                epNull = ("<bean name=\"" + name + "\" isNull=\"true\" />").toCharArray();
                epNullWithType = ("<bean name=\"" + name + "\" type=\"" + typeAttr + "\" isNull=\"true\" />").toCharArray();
            } else {
                epStart = ("<property name=\"" + name + "\">").toCharArray();
                epStartWithType = ("<property name=\"" + name + "\" type=\"" + typeAttr + "\">").toCharArray();
                epEnd = ("</property>").toCharArray();
                epNull = ("<property name=\"" + name + "\" isNull=\"true\" />").toCharArray();
                epNullWithType = ("<property name=\"" + name + "\" type=\"" + typeAttr + "\" isNull=\"true\" />").toCharArray();
            }

            namedStart = ("<" + name + ">").toCharArray();
            namedStartWithType = ("<" + name + " type=\"" + typeAttr + "\">").toCharArray();
            namedEnd = ("</" + name + ">").toCharArray();
            namedNull = ("<" + name + " isNull=\"true\" />").toCharArray();
            namedNullWithType = ("<" + name + " type=\"" + typeAttr + "\" isNull=\"true\" />").toCharArray();
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

    interface DateTimeReaderWriter<T> {
        T read(PropInfo propInfo, String strValue);

        void write(PropInfo propInfo, T x, CharacterWriter writer) throws IOException;
    }

    static class JodaDateTimeFormatterHolder {
        final org.joda.time.DateTimeZone dtz;
        final org.joda.time.format.DateTimeFormatter dtf;

        JodaDateTimeFormatterHolder(final String dateFormat, final TimeZone timeZone) {
            dtz = org.joda.time.DateTimeZone.forTimeZone(timeZone);
            dtf = org.joda.time.format.DateTimeFormat.forPattern(dateFormat).withZone(dtz);
        }
    }

}
