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

package com.landawn.abacus.parser;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.NumberFormat;
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

import com.landawn.abacus.annotation.AccessFieldByMethod;
import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.annotation.Column;
import com.landawn.abacus.annotation.Entity;
import com.landawn.abacus.annotation.Id;
import com.landawn.abacus.annotation.Internal;
import com.landawn.abacus.annotation.JsonXmlConfig;
import com.landawn.abacus.annotation.JsonXmlField;
import com.landawn.abacus.annotation.JsonXmlField.Expose;
import com.landawn.abacus.annotation.ReadOnlyId;
import com.landawn.abacus.annotation.Table;
import com.landawn.abacus.annotation.Transient;
import com.landawn.abacus.annotation.Type.EnumBy;
import com.landawn.abacus.annotation.Type.Scope;
import com.landawn.abacus.logging.Logger;
import com.landawn.abacus.logging.LoggerFactory;
import com.landawn.abacus.type.ObjectType;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.type.TypeFactory;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.DateUtil;
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
import com.landawn.abacus.util.Strings.MoreStringUtil;
import com.landawn.abacus.util.Tuple.Tuple3;
import com.landawn.abacus.util.WD;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.function.Function;
import com.landawn.abacus.util.function.Supplier;

/**
 *
 * @author Haiyang Li
 * @since 0.8
 */
@Internal
@SuppressWarnings("java:S1192")
public final class ParserUtil {

    static final Logger logger = LoggerFactory.getLogger(ParserUtil.class);

    private static final PropInfo PROP_INFO_MASK = new PropInfo("PROP_INFO_MASK");

    private static final char PROP_NAME_SEPARATOR = '.';

    // ...
    private static final String GET = "get".intern();

    private static final String SET = "set".intern();

    private static final String IS = "is".intern();

    private static final String HAS = "has".intern();

    private static final Set<Class<?>> idTypeSet = N.<Class<?>> asSet(int.class, Integer.class, long.class, Long.class, String.class, Timestamp.class,
            UUID.class);

    @SuppressWarnings("deprecation")
    private static final int POOL_SIZE = InternalUtil.POOL_SIZE;

    private static final int defaultNameIndex = NamingPolicy.LOWER_CAMEL_CASE.ordinal();

    // ...
    private static final Map<Class<?>, BeanInfo> beanInfoPool = new ObjectPool<>(POOL_SIZE);

    private ParserUtil() {
        // Singleton.
    }

    static boolean isJsonXmlSerializable(final Field field, final JsonXmlConfig jsonXmlConfig) {
        if (field == null) {
            return true;
        }

        if (Modifier.isStatic(field.getModifiers()) || (field.isAnnotationPresent(JsonXmlField.class) && field.getAnnotation(JsonXmlField.class).ignore())) {
            return false;
        }

        try {
            if (field.isAnnotationPresent(com.alibaba.fastjson.annotation.JSONField.class)
                    && !field.getAnnotation(com.alibaba.fastjson.annotation.JSONField.class).serialize()) {
                return false;
            }
        } catch (Throwable e) {
            // ignore
        }

        try {
            if (field.isAnnotationPresent(com.alibaba.fastjson2.annotation.JSONField.class)
                    && !field.getAnnotation(com.alibaba.fastjson2.annotation.JSONField.class).serialize()) {
                return false;
            }
        } catch (Throwable e) {
            // ignore
        }

        try {
            if (field.isAnnotationPresent(com.fasterxml.jackson.annotation.JsonIgnore.class)
                    && field.getAnnotation(com.fasterxml.jackson.annotation.JsonIgnore.class).value()) {
                return false;
            }
        } catch (Throwable e) {
            // ignore
        }

        if (jsonXmlConfig != null && N.notNullOrEmpty(jsonXmlConfig.ignoredFields())) {
            String fieldName = field.getName();

            for (String ignoreFieldName : jsonXmlConfig.ignoredFields()) {
                if (fieldName.equals(ignoreFieldName) || fieldName.matches(ignoreFieldName)) {
                    return false;
                }
            }
        }

        return true;
    }

    static String getDateFormat(final Field field, final JsonXmlConfig jsonXmlConfig) {
        if (field != null) {
            if (field.isAnnotationPresent(JsonXmlField.class) && Strings.isNotEmpty(field.getAnnotation(JsonXmlField.class).dateFormat())) {
                return field.getAnnotation(JsonXmlField.class).dateFormat();
            }

            try {
                if (field.isAnnotationPresent(com.alibaba.fastjson.annotation.JSONField.class)
                        && Strings.isNotEmpty(field.getAnnotation(com.alibaba.fastjson.annotation.JSONField.class).format())) {
                    return field.getAnnotation(com.alibaba.fastjson.annotation.JSONField.class).format();
                }
            } catch (Throwable e) {
                // ignore
            }

            try {
                if (field.isAnnotationPresent(com.alibaba.fastjson2.annotation.JSONField.class)
                        && Strings.isNotEmpty(field.getAnnotation(com.alibaba.fastjson2.annotation.JSONField.class).format())) {
                    return field.getAnnotation(com.alibaba.fastjson2.annotation.JSONField.class).format();
                }
            } catch (Throwable e) {
                // ignore
            }

            try {
                if (field.isAnnotationPresent(com.fasterxml.jackson.annotation.JsonFormat.class)
                        && Strings.isNotEmpty(field.getAnnotation(com.fasterxml.jackson.annotation.JsonFormat.class).pattern())) {
                    return field.getAnnotation(com.fasterxml.jackson.annotation.JsonFormat.class).pattern();
                }
            } catch (Throwable e) {
                // ignore
            }
        }

        if (jsonXmlConfig != null && Strings.isNotEmpty(jsonXmlConfig.dateFormat())) {
            return jsonXmlConfig.dateFormat();
        }

        return null;
    }

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
            } catch (Throwable e) {
                // ignore
            }
        }

        if (jsonXmlConfig != null && Strings.isNotEmpty(jsonXmlConfig.timeZone())) {
            return jsonXmlConfig.timeZone();
        }

        return null;
    }

    static String getNumberFormat(final Field field, final JsonXmlConfig jsonXmlConfig) {
        if ((field != null) && (field.isAnnotationPresent(JsonXmlField.class) && Strings.isNotEmpty(field.getAnnotation(JsonXmlField.class).numberFormat()))) {
            return field.getAnnotation(JsonXmlField.class).numberFormat();
        }

        if (jsonXmlConfig != null && Strings.isNotEmpty(jsonXmlConfig.numberFormat())) {
            return jsonXmlConfig.numberFormat();
        }

        return null;
    }

    static EnumBy getEnumerated(final Field field, final JsonXmlConfig jsonXmlConfig) {
        if ((field != null) && (field.isAnnotationPresent(JsonXmlField.class) && field.getAnnotation(JsonXmlField.class).enumerated() != null)) {
            return field.getAnnotation(JsonXmlField.class).enumerated();
        }

        if (jsonXmlConfig != null && jsonXmlConfig.enumerated() != null) {
            return jsonXmlConfig.enumerated();
        }

        return EnumBy.NAME;
    }

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
            } catch (Throwable e) {
                // ignore.
            }
        }

        if (!isJsonRawValue) {
            try {
                if (field != null && field.isAnnotationPresent(com.alibaba.fastjson.annotation.JSONField.class)
                        && field.getAnnotation(com.alibaba.fastjson.annotation.JSONField.class).jsonDirect()) {
                    isJsonRawValue = true;
                }
            } catch (Throwable e) {
                // ignore.
            }
        }

        if (!isJsonRawValue) {
            try {
                if (field != null && field.isAnnotationPresent(com.alibaba.fastjson2.annotation.JSONField.class)
                        && field.getAnnotation(com.alibaba.fastjson2.annotation.JSONField.class).jsonDirect()) {
                    isJsonRawValue = true;
                }
            } catch (Throwable e) {
                // ignore.
            }
        }

        if (isJsonRawValue && !CharSequence.class.isAssignableFrom(field.getType())) {
            throw new IllegalArgumentException("'isJsonRawValue' can only be applied to CharSequence type field");
        }

        return isJsonRawValue;
    }

    static JsonNameTag[] getJsonNameTags(final String name) {
        final JsonNameTag[] result = new JsonNameTag[NamingPolicy.values().length];

        for (NamingPolicy np : NamingPolicy.values()) {
            result[np.ordinal()] = new JsonNameTag(convertName(name, np));
        }

        return result;
    }

    static XmlNameTag[] getXmlNameTags(final String name, final String typeName, final boolean isBean) {
        final XmlNameTag[] result = new XmlNameTag[NamingPolicy.values().length];

        for (NamingPolicy np : NamingPolicy.values()) {
            result[np.ordinal()] = new XmlNameTag(convertName(name, np), typeName, isBean);
        }

        return result;
    }

    static JsonNameTag[] getJsonNameTags(final String propName, final Field field) {
        String jsonXmlFieldName = null;

        if (field != null) {
            if (field.isAnnotationPresent(JsonXmlField.class) && Strings.isNotEmpty(field.getAnnotation(JsonXmlField.class).name())) {
                jsonXmlFieldName = field.getAnnotation(JsonXmlField.class).name();
            } else {
                try {
                    if (field.isAnnotationPresent(com.alibaba.fastjson.annotation.JSONField.class)
                            && Strings.isNotEmpty(field.getAnnotation(com.alibaba.fastjson.annotation.JSONField.class).name())) {
                        jsonXmlFieldName = field.getAnnotation(com.alibaba.fastjson.annotation.JSONField.class).name();
                    }
                } catch (Throwable e) {
                    // ignore
                }

                if (Strings.isEmpty(jsonXmlFieldName)) {
                    try {
                        if (field.isAnnotationPresent(com.alibaba.fastjson2.annotation.JSONField.class)
                                && Strings.isNotEmpty(field.getAnnotation(com.alibaba.fastjson2.annotation.JSONField.class).name())) {
                            jsonXmlFieldName = field.getAnnotation(com.alibaba.fastjson2.annotation.JSONField.class).name();
                        }
                    } catch (Throwable e) {
                        // ignore
                    }
                }

                if (Strings.isEmpty(jsonXmlFieldName)) {
                    try {
                        if (field.isAnnotationPresent(com.fasterxml.jackson.annotation.JsonProperty.class)
                                && Strings.isNotEmpty(field.getAnnotation(com.fasterxml.jackson.annotation.JsonProperty.class).value())) {
                            jsonXmlFieldName = field.getAnnotation(com.fasterxml.jackson.annotation.JsonProperty.class).value();
                        }
                    } catch (Throwable e) {
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

        for (NamingPolicy np : NamingPolicy.values()) {
            result[np.ordinal()] = new JsonNameTag(Strings.isEmpty(jsonXmlFieldName) ? convertName(propName, np) : jsonXmlFieldName);
        }

        return result;
    }

    static XmlNameTag[] getXmlNameTags(final String propName, final Field field, final String typeName, final boolean isBean) {
        String jsonXmlFieldName = null;

        if (field != null) {
            if (field.isAnnotationPresent(JsonXmlField.class) && Strings.isNotEmpty(field.getAnnotation(JsonXmlField.class).name())) {
                jsonXmlFieldName = field.getAnnotation(JsonXmlField.class).name();
            } else {
                try {
                    if (field.isAnnotationPresent(com.alibaba.fastjson.annotation.JSONField.class)
                            && Strings.isNotEmpty(field.getAnnotation(com.alibaba.fastjson.annotation.JSONField.class).name())) {
                        jsonXmlFieldName = field.getAnnotation(com.alibaba.fastjson.annotation.JSONField.class).name();
                    }
                } catch (Throwable e) {
                    // ignore
                }

                if (Strings.isEmpty(jsonXmlFieldName)) {
                    try {
                        if (field.isAnnotationPresent(com.alibaba.fastjson2.annotation.JSONField.class)
                                && Strings.isNotEmpty(field.getAnnotation(com.alibaba.fastjson2.annotation.JSONField.class).name())) {
                            jsonXmlFieldName = field.getAnnotation(com.alibaba.fastjson2.annotation.JSONField.class).name();
                        }
                    } catch (Throwable e) {
                        // ignore
                    }
                }

                if (Strings.isEmpty(jsonXmlFieldName)) {
                    try {
                        if (field.isAnnotationPresent(com.fasterxml.jackson.annotation.JsonProperty.class)
                                && Strings.isNotEmpty(field.getAnnotation(com.fasterxml.jackson.annotation.JsonProperty.class).value())) {
                            jsonXmlFieldName = field.getAnnotation(com.fasterxml.jackson.annotation.JsonProperty.class).value();
                        }
                    } catch (Throwable e) {
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

        for (NamingPolicy np : NamingPolicy.values()) {
            result[np.ordinal()] = new XmlNameTag(Strings.isEmpty(jsonXmlFieldName) ? convertName(propName, np) : jsonXmlFieldName, typeName, isBean);
        }

        return result;
    }

    static String[] getAliases(final Field field) {
        String[] alias = null;

        if (field != null) {
            if (field.isAnnotationPresent(JsonXmlField.class) && N.notNullOrEmpty(field.getAnnotation(JsonXmlField.class).alias())) {
                alias = field.getAnnotation(JsonXmlField.class).alias();
            } else {
                try {
                    if (field.isAnnotationPresent(com.alibaba.fastjson.annotation.JSONField.class)
                            && N.notNullOrEmpty(field.getAnnotation(com.alibaba.fastjson.annotation.JSONField.class).alternateNames())) {
                        alias = field.getAnnotation(com.alibaba.fastjson.annotation.JSONField.class).alternateNames();
                    }
                } catch (Throwable e) {
                    // ignore
                }

                if (N.isNullOrEmpty(alias)) {
                    try {
                        if (field.isAnnotationPresent(com.alibaba.fastjson2.annotation.JSONField.class)
                                && N.notNullOrEmpty(field.getAnnotation(com.alibaba.fastjson2.annotation.JSONField.class).alternateNames())) {
                            alias = field.getAnnotation(com.alibaba.fastjson2.annotation.JSONField.class).alternateNames();
                        }
                    } catch (Throwable e) {
                        // ignore
                    }
                }

                if (N.isNullOrEmpty(alias)) {
                    try {
                        if (field.isAnnotationPresent(com.fasterxml.jackson.annotation.JsonAlias.class)
                                && N.notNullOrEmpty(field.getAnnotation(com.fasterxml.jackson.annotation.JsonAlias.class).value())) {
                            alias = field.getAnnotation(com.fasterxml.jackson.annotation.JsonAlias.class).value();
                        }
                    } catch (Throwable e) {
                        // ignore
                    }
                }
            }
        }

        if (N.notNullOrEmpty(alias) && field != null) {
            alias = N.removeAllOccurrences(alias, field.getName());
        }

        return alias;
    }

    private static String convertName(final String name, final NamingPolicy namingPolicy) {
        return namingPolicy == null || namingPolicy == NamingPolicy.LOWER_CAMEL_CASE || namingPolicy == NamingPolicy.NO_CHANGE ? name
                : namingPolicy.convert(name);
    }

    /**
     * Gets the bean info.
     *
     * @param cls
     * @return
     */
    public static BeanInfo getBeanInfo(Class<?> cls) {
        if (!ClassUtil.isBeanClass(cls)) {
            throw new IllegalArgumentException(
                    "No property getter/setter method or public field found in the specified bean: " + ClassUtil.getCanonicalClassName(cls));
        }

        BeanInfo beanInfo = beanInfoPool.get(cls);

        if (beanInfo == null) {
            synchronized (beanInfoPool) {
                beanInfo = beanInfoPool.get(cls);

                if (beanInfo == null) {
                    beanInfo = new BeanInfo(cls);
                    beanInfoPool.put(cls, beanInfo);
                }
            }
        }

        return beanInfo;
    }

    static int hashCode(char[] a) {
        int result = 1;

        for (char e : a) {
            result = 31 * result + e;
        }

        return result;
    }

    static int hashCode(char[] a, int fromIndex, int toIndex) {
        return N.hashCode(a, fromIndex, toIndex);
    }

    // private int hashCode(String str, int fromIndex, int toIndex) {
    // int result = 1;
    //
    // for (int i = fromIndex; i < toIndex; i++) {
    // result = 31 * result + str.charAt(i);
    // }
    //
    // return result;
    // }

    /**
     * Refresh bean prop info.
     *
     * @param cls
     * @deprecated internal use only.
     */
    @Deprecated
    @Internal
    public static void refreshBeanPropInfo(Class<?> cls) {
        synchronized (beanInfoPool) {
            beanInfoPool.remove(cls);
        }
    }

    public static class BeanInfo implements JSONReader.SymbolReader {

        public final Class<Object> clazz;

        public final String simpleClassName;

        public final String canonicalClassName;

        final String name;

        public final Type<Object> type;

        public final ImmutableList<PropInfo> propInfoList;

        public final ImmutableList<PropInfo> idPropInfoList;

        public final ImmutableList<String> idPropNameList;

        public final ImmutableList<PropInfo> readOnlyIdPropInfoList;

        public final ImmutableList<String> readOnlyIdPropNameList;

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

        private final Map<String, PropInfo> propInfoMap;

        private final Map<String, List<PropInfo>> propInfoQueueMap;

        private final PropInfo[] propInfoArray;

        private final Map<Integer, PropInfo> hashPropInfoMap;

        public final Optional<String> tableName;

        private final Class<?>[] fieldTypes;
        private final Object[] defaultFieldValues;
        private final Constructor<?> noArgsConstructor;
        private final Constructor<?> allArgsConstructor;

        public final boolean isImmutable;
        private final boolean isByBuilder;
        private final Tuple3<Class<?>, Supplier<Object>, Function<Object, Object>> builderInfo;

        public final boolean isMarkedToBean;

        @SuppressWarnings("deprecation")
        BeanInfo(final Class<?> cls) {
            this.annotations = ImmutableMap.wrap(getAnnotations(cls));
            simpleClassName = ClassUtil.getSimpleClassName(cls);
            canonicalClassName = ClassUtil.getCanonicalClassName(cls);
            name = ClassUtil.formalizePropName(simpleClassName);
            this.clazz = (Class<Object>) cls;
            type = N.typeOf(cls);
            typeName = type.name();

            final List<String> propNameList = ClassUtil.getPropNameList(cls);

            boolean localIsImmutable = true;

            if (ClassUtil.isRecordClass(cls)) {
                localIsImmutable = true;
            } else if (ClassUtil.isRegisteredXMLBindingClass(cls)) {
                localIsImmutable = false;
            } else {
                try {
                    final Object tmp = N.newInstance(cls);
                    Field field = null;
                    Method setMethod = null;

                    for (String propName : propNameList) {
                        field = ClassUtil.getPropField(cls, propName);
                        setMethod = ClassUtil.getPropSetMethod(cls, propName);

                        if (setMethod != null) {
                            localIsImmutable = false;
                            break;
                        } else if (field != null) {
                            try { //NOSONAR
                                field.set(tmp, N.defaultValueOf(field.getType())); //NOSONAR
                                localIsImmutable = false;

                                break;
                            } catch (Throwable e) {
                                // ignore.
                            }
                        }
                    }
                } catch (Throwable e) {
                    // ignore.
                }
            }

            this.isImmutable = localIsImmutable;
            this.builderInfo = localIsImmutable ? ClassUtil.getBuilderInfo(cls) : null;
            this.isByBuilder = localIsImmutable && builderInfo != null;

            final JsonXmlConfig jsonXmlConfig = (JsonXmlConfig) annotations.get(JsonXmlConfig.class);
            this.jsonXmlNamingPolicy = jsonXmlConfig == null || jsonXmlConfig.namingPolicy() == null ? NamingPolicy.LOWER_CAMEL_CASE
                    : jsonXmlConfig.namingPolicy();
            this.jsonXmlSeriExclusion = jsonXmlConfig == null || jsonXmlConfig.exclusion() == null ? Exclusion.NULL : jsonXmlConfig.exclusion();

            jsonNameTags = getJsonNameTags(name);
            xmlNameTags = getXmlNameTags(name, typeName, true);

            final List<String> idPropNames = new ArrayList<>();
            final List<String> readOnlyIdPropNames = new ArrayList<>();

            if (cls.isAnnotationPresent(Id.class)) {
                String[] values = cls.getAnnotation(Id.class).value();
                N.checkArgNotNullOrEmpty(values, "values for annotation @Id on Type/Class can't be null or empty");
                idPropNames.addAll(Arrays.asList(values));
            }

            if (cls.isAnnotationPresent(ReadOnlyId.class)) {
                String[] values = cls.getAnnotation(ReadOnlyId.class).value();
                N.checkArgNotNullOrEmpty(values, "values for annotation @ReadOnlyId on Type/Class can't be null or empty");
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

            for (String propName : propNameList) {
                field = ClassUtil.getPropField(cls, propName);
                getMethod = ClassUtil.getPropGetMethod(cls, propName);
                setMethod = isByBuilder ? ClassUtil.getPropSetMethod(builderInfo._1, propName) : ClassUtil.getPropSetMethod(cls, propName);

                propInfo = ASMUtil.isASMAvailable()
                        ? new ASMPropInfo(propName, field, getMethod, setMethod, jsonXmlConfig, annotations, idx, this.isImmutable, this.isByBuilder,
                                idPropNames, readOnlyIdPropNames)
                        : new PropInfo(propName, field, getMethod, setMethod, jsonXmlConfig, annotations, idx, this.isImmutable, this.isByBuilder, idPropNames,
                                readOnlyIdPropNames);

                propInfos[idx++] = propInfo;
                propInfoMap.put(propName, propInfo);
                String jsonTagName = null;

                for (JsonNameTag nameTag : propInfo.jsonNameTags) {
                    jsonTagName = new String(nameTag.name);

                    if (!propInfoMap.containsKey(jsonTagName)) {
                        propInfoMap.put(jsonTagName, propInfo);
                    }
                }

                if (propInfo.columnName.isPresent() && !propInfoMap.containsKey(propInfo.columnName.get())) {
                    propInfoMap.put(propInfo.columnName.get(), propInfo);

                    if (!propInfoMap.containsKey(propInfo.columnName.get().toLowerCase())) {
                        propInfoMap.put(propInfo.columnName.get().toLowerCase(), propInfo);
                    }

                    if (!propInfoMap.containsKey(propInfo.columnName.get().toUpperCase())) {
                        propInfoMap.put(propInfo.columnName.get().toUpperCase(), propInfo);
                    }
                }

                final ImmutableList<String> aliases = propInfo.aliases;

                if (N.notNullOrEmpty(aliases)) {
                    for (String str : aliases) {
                        if (propInfoMap.containsKey(str)) {
                            throw new IllegalArgumentException("Can't set alias: " + str + " for property/field: " + propInfo.field + " because " + str
                                    + " is a property/field name in class: " + cls);
                        }

                        propInfoMap.put(str, propInfo);
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

            jsonXmlSerializablePropInfos = seriPropInfoList.toArray(new PropInfo[seriPropInfoList.size()]);
            nonTransientSeriPropInfos = nonTransientSeriPropInfoList.toArray(new PropInfo[nonTransientSeriPropInfoList.size()]);
            transientSeriPropInfos = transientSeriPropInfoList.toArray(new PropInfo[transientSeriPropInfoList.size()]);

            propInfoArray = new PropInfo[maxLength + 1];

            for (PropInfo e : propInfos) {
                hashPropInfoMap.put(ParserUtil.hashCode(e.jsonNameTags[defaultNameIndex].name), e);

                if (multiSet.get(e.jsonNameTags[defaultNameIndex].name.length) == 1) {
                    propInfoArray[e.jsonNameTags[defaultNameIndex].name.length] = e;
                }
            }

            this.propInfoList = ImmutableList.wrap(N.asList(propInfos));

            final List<PropInfo> tmpIdPropInfoList = N.filter(propInfos, it -> it.isMarkedToId);

            if (N.isNullOrEmpty(tmpIdPropInfoList)) {
                tmpIdPropInfoList.addAll(N.filter(propInfos, it -> "id".equals(it.name) && idTypeSet.contains(it.clazz)));
            }

            this.idPropInfoList = ImmutableList.wrap(tmpIdPropInfoList);
            this.idPropNameList = ImmutableList.wrap(N.map(idPropInfoList, it -> it.name));

            this.readOnlyIdPropInfoList = ImmutableList.wrap(N.filter(propInfos, it -> it.isMarkedToReadOnlyId));
            this.readOnlyIdPropNameList = ImmutableList.wrap(N.map(readOnlyIdPropInfoList, it -> it.name));

            String tmpTableName = null;

            if (this.annotations.containsKey(Table.class)) {
                tmpTableName = ((Table) this.annotations.get(Table.class)).value();

                if (Strings.isEmpty(tmpTableName)) {
                    tmpTableName = ((Table) this.annotations.get(Table.class)).name();
                }
            } else {
                try {
                    if (this.annotations.containsKey(javax.persistence.Table.class)) {
                        tmpTableName = ((javax.persistence.Table) this.annotations.get(javax.persistence.Table.class)).name();
                    }
                } catch (Throwable e) {
                    // ignore
                }
            }

            if (Strings.isNotEmpty(tmpTableName) && !tmpTableName.equals(Strings.strip(tmpTableName))) {
                throw new IllegalArgumentException("Table name: \"" + tmpTableName + "\" must not start or end with any whitespace in class: " + cls);
            }

            this.tableName = Strings.isEmpty(tmpTableName) ? Optional.<String> empty() : Optional.ofNullable(tmpTableName);

            this.fieldTypes = new Class[propInfos.length];
            this.defaultFieldValues = new Object[propInfos.length];

            for (int i = 0, len = propInfos.length; i < len; i++) {
                fieldTypes[i] = propInfos[i].field == null ? propInfos[i].clazz : propInfos[i].field.getType();
                defaultFieldValues[i] = N.defaultValueOf(fieldTypes[i]);
            }

            this.noArgsConstructor = ClassUtil.getDeclaredConstructor(cls);
            this.allArgsConstructor = ClassUtil.getDeclaredConstructor(cls, fieldTypes);

            if (this.noArgsConstructor != null) {
                ClassUtil.setAccessibleQuietly(noArgsConstructor, true);
            }

            if (this.allArgsConstructor != null) {
                ClassUtil.setAccessibleQuietly(allArgsConstructor, true);
            }

            boolean tmpIsMarkedToBean = this.annotations.containsKey(Entity.class);

            if (tmpIsMarkedToBean == false) {
                try {
                    tmpIsMarkedToBean = this.annotations.containsKey(javax.persistence.Entity.class);
                } catch (Throwable e) {
                    // ignore
                }
            }

            this.isMarkedToBean = tmpIsMarkedToBean;

            //    if (this.noArgsConstructor == null && this.allArgsConstructor == null) {
            //        throw new RuntimeException("No Constructor found with empty arg or full args: " + N.toString(fieldTypes) + " in Bean/Record class: "
            //                + ClassUtil.getCanonicalClassName(cls));
            //    }

        }

        /**
         * Gets the prop info.
         *
         * @param propName
         * @return
         */
        @Override
        public PropInfo getPropInfo(String propName) {
            // slower?
            // int len = propName.length();
            //
            // if (len < propInfoArray.length && propInfoArray[len] != null &&
            // propInfoArray[len].name.equals(propName)) {
            // return propInfoArray[len];
            // }

            PropInfo propInfo = propInfoMap.get(propName);

            if (propInfo == null) {
                Method method = ClassUtil.getPropGetMethod(clazz, propName);

                if (method != null) {
                    propInfo = propInfoMap.get(ClassUtil.getPropNameByMethod(method));
                }

                if (propInfo == null) {
                    for (String key : propInfoMap.keySet()) { //NOSONAR
                        if (isPropName(clazz, propName, key)) {
                            propInfo = propInfoMap.get(key);

                            break;
                        }
                    }

                    if ((propInfo == null) && !propName.equalsIgnoreCase(ClassUtil.formalizePropName(propName))) {
                        propInfo = getPropInfo(ClassUtil.formalizePropName(propName));
                    }
                }

                // set method mask to avoid query next time.
                if (propInfo == null) {
                    propInfo = PROP_INFO_MASK;
                } else {
                    if (propInfo == PROP_INFO_MASK) {
                        // ignore.
                    } else {
                        hashPropInfoMap.put(ParserUtil.hashCode(propInfo.jsonNameTags[defaultNameIndex].name), propInfo);
                    }
                }

                //                if (propInfo == PROP_INFO_MASK) {
                //                    // logger.warn("No property info found by name: " + propName + " in class: " + cls.getCanonicalName());
                //                    String msg = "No property info found by name: " + propName + " in class: " + cls.getCanonicalName() + ". The defined properties are: "
                //                            + propInfoMap;
                //                    logger.error(msg, new RuntimeException(msg));
                //                }

                propInfoMap.put(propName, propInfo);
            }

            return (propInfo == PROP_INFO_MASK) ? null : propInfo;
        }

        /**
         * Gets the prop info.
         *
         * @param propInfoFromOtherBean
         * @return
         */
        public PropInfo getPropInfo(PropInfo propInfoFromOtherBean) {
            if (propInfoFromOtherBean.aliases.isEmpty()) {
                return getPropInfo(propInfoFromOtherBean.name);
            } else {
                PropInfo ret = getPropInfo(propInfoFromOtherBean.name);

                if (ret == null) {
                    for (String alias : propInfoFromOtherBean.aliases) {
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
         * Gets the prop value.
         *
         * @param <T>
         * @param obj
         * @param propName
         * @return
         */
        @SuppressWarnings("unchecked")
        public <T> T getPropValue(Object obj, String propName) {
            PropInfo propInfo = getPropInfo(propName);

            if (propInfo == null) {
                final List<PropInfo> propInfoQueue = getPropInfoQueue(propName);

                if (propInfoQueue.size() == 0) {
                    throw new RuntimeException("No property method found with property name: " + propName + " in class: " + clazz.getCanonicalName());
                } else {
                    Object propBean = obj;

                    for (int i = 0, len = propInfoQueue.size(); i < len; i++) {
                        propBean = propInfoQueue.get(i).getPropValue(propBean);

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
         *
         *
         * @param obj
         * @param propName
         * @param propValue
         */
        public void setPropValue(final Object obj, final String propName, final Object propValue) {
            setPropValue(obj, propName, propValue, false);
        }

        /**
         * Sets the prop value.
         *
         * @param obj
         * @param propName
         * @param propValue
         * @param ignoreUnmatchedProperty
         * @return
         */
        @SuppressWarnings("rawtypes")
        public boolean setPropValue(final Object obj, final String propName, final Object propValue, final boolean ignoreUnmatchedProperty) {
            PropInfo propInfo = getPropInfo(propName);

            if (propInfo == null) {
                final List<PropInfo> propInfoQueue = getPropInfoQueue(propName);

                if (propInfoQueue.size() == 0) {
                    if (!ignoreUnmatchedProperty) {
                        throw new RuntimeException("No property method found with property name: " + propName + " in class: " + clazz.getCanonicalName());
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
                                    // For example: set "account.Name.firstName" key in Maps.map2Bean, if Account.Name is a Record?
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
         *
         *
         * @param obj
         * @param propInfoFromOtherBean
         * @param propValue
         */
        public void setPropValue(final Object obj, final PropInfo propInfoFromOtherBean, final Object propValue) {
            setPropValue(obj, propInfoFromOtherBean, propValue, false);
        }

        /**
         *
         *
         * @param obj
         * @param propInfoFromOtherBean
         * @param propValue
         * @param ignoreUnmatchedProperty
         * @return
         */
        public boolean setPropValue(final Object obj, final PropInfo propInfoFromOtherBean, final Object propValue, final boolean ignoreUnmatchedProperty) {
            if (propInfoFromOtherBean.aliases.isEmpty()) {
                return setPropValue(obj, propInfoFromOtherBean.name, propValue, ignoreUnmatchedProperty);
            } else {
                if (!setPropValue(obj, propInfoFromOtherBean.name, propValue, true)) {
                    for (String alias : propInfoFromOtherBean.aliases) {
                        if (setPropValue(obj, alias, propValue, true)) {
                            return true;
                        }
                    }
                }

                if (!ignoreUnmatchedProperty) {
                    throw new RuntimeException(
                            "No property method found with property name: " + propInfoFromOtherBean.name + " in class: " + clazz.getCanonicalName());
                }

                return false;
            }
        }

        /**
         * Checks if is prop name.
         *
         * @param cls
         * @param inputPropName
         * @param propNameByMethod
         * @return true, if is prop name
         */
        private boolean isPropName(Class<?> cls, String inputPropName, String propNameByMethod) {
            if (inputPropName.length() > 128) {
                throw new RuntimeException("The property name execeed 128: " + inputPropName);
            }

            inputPropName = inputPropName.trim();

            return inputPropName.equalsIgnoreCase(propNameByMethod) || inputPropName.replace(WD.UNDERSCORE, Strings.EMPTY_STRING).equalsIgnoreCase(propNameByMethod)
                    || inputPropName.equalsIgnoreCase(ClassUtil.getSimpleClassName(cls) + WD._PERIOD + propNameByMethod)
                    || (inputPropName.startsWith(GET) && inputPropName.substring(3).equalsIgnoreCase(propNameByMethod))
                    || (inputPropName.startsWith(SET) && inputPropName.substring(3).equalsIgnoreCase(propNameByMethod))
                    || (inputPropName.startsWith(IS) && inputPropName.substring(2).equalsIgnoreCase(propNameByMethod))
                    || (inputPropName.startsWith(HAS) && inputPropName.substring(2).equalsIgnoreCase(propNameByMethod));
        }

        /**
         * Gets the prop info queue.
         *
         * @param propName
         * @return
         */
        @SuppressWarnings("deprecation")
        public List<PropInfo> getPropInfoQueue(String propName) {
            List<PropInfo> propInfoQueue = propInfoQueueMap.get(propName);

            if (propInfoQueue == null) {
                propInfoQueue = new ArrayList<>();

                final String[] strs = Splitter.with(PROP_NAME_SEPARATOR).splitToArray(propName);

                if (strs.length > 1) {
                    Class<?> propClass = clazz;
                    BeanInfo propBeanInfo = null;

                    PropInfo propInfo = null;

                    for (int i = 0, len = strs.length; i < len; i++) {
                        propBeanInfo = ClassUtil.isBeanClass(propClass) ? ParserUtil.getBeanInfo(propClass) : null;
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

                propInfoQueue = N.isNullOrEmpty(propInfoQueue) ? N.<PropInfo> emptyList() : ImmutableList.wrap(propInfoQueue);

                propInfoQueueMap.put(propName, propInfoQueue);
            }

            return propInfoQueue;
        }

        /**
         * Read prop info.
         *
         * @param cbuf
         * @param fromIndex
         * @param toIndex
         * @return
         */
        @Override
        public PropInfo readPropInfo(final char[] cbuf, int fromIndex, int toIndex) {
            int len = toIndex - fromIndex;

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
                // TODO skip the comparison for performance improvement or:
                //
                // for (int i = 0; i < len; i += 2) {
                // if (cbuf[i + from] == propInfo.jsonInfo.name[i]) {
                // // continue;
                // } else {
                // propInfo = null;
                //
                // break;
                // }
                // }
                //

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

        // @Override
        // public PropInfo readPropInfo(String str, int fromIndex, int toIndex) {
        // if (N.isCharsOfStringReadable()) {
        // return readPropInfo(StrUtil.getCharsForReadOnly(str), fromIndex, toIndex);
        // }
        //
        // int len = toIndex - fromIndex;
        //
        // if (len == 0) {
        // return null;
        // }
        //
        // PropInfo propInfo = null;
        //
        // if (len < propInfoArray.length) {
        // propInfo = propInfoArray[len];
        // }
        //
        // if (propInfo == null) {
        // propInfo = hashPropInfoMap.get(hashCode(str, from, to));
        // }
        //
        // if (propInfo != null) {
        // // TODO skip the comparison for performance improvement or:
        // //
        // // for (int i = 0; i < len; i += 2) {
        // // if (str.charAt(i + from) == propInfo.jsonInfo.name[i]) {
        // // // continue;
        // // } else {
        // // propInfo = null;
        // //
        // // break;
        // // }
        // // }
        //
        // final char[] tmp = propInfo.jsonInfo.name;
        // for (int i = 0; i < len; i++) {
        // if (str.charAt(i + from) == tmp[i]) {
        // // continue;
        // } else {
        // return null;
        // }
        // }
        // }
        //
        // return propInfo;
        // }

        /**
         *
         *
         * @param annotationClass
         * @return
         */
        public boolean isAnnotationPresent(final Class<? extends Annotation> annotationClass) {
            return annotations.containsKey(annotationClass);
        }

        /**
         *
         *
         * @param <T>
         * @param annotationClass
         * @return
         */
        public <T extends Annotation> T getAnnotation(final Class<T> annotationClass) {
            return (T) annotations.get(annotationClass);
        }

        private Map<Class<? extends Annotation>, Annotation> getAnnotations(final Class<?> cls) {
            final Map<Class<? extends Annotation>, Annotation> annos = new HashMap<>();

            final Set<Class<?>> classes = ClassUtil.getAllSuperTypes(cls);
            N.reverse(classes);
            classes.add(cls);

            for (Class<?> e : classes) {
                if (N.notNullOrEmpty(e.getAnnotations())) {
                    for (Annotation anno : e.getAnnotations()) {
                        annos.put(anno.annotationType(), anno);
                    }
                }
            }

            return annos;
        }

        /**
         *
         * @param <T>
         * @return
         */
        @Beta
        <T> T newInstance() {
            return (T) (noArgsConstructor == null ? ClassUtil.invokeConstructor(allArgsConstructor, defaultFieldValues)
                    : ClassUtil.invokeConstructor(noArgsConstructor));
        }

        /**
         *
         * @param <T>
         * @param args
         * @return
         */
        @Beta
        <T> T newInstance(final Object... args) {
            if (N.isNullOrEmpty(args)) {
                return newInstance();
            }

            return (T) ClassUtil.invokeConstructor(allArgsConstructor, args);
        }

        /**
         *
         * @return
         */
        @Beta
        public Object createBeanResult() {
            return isImmutable ? (builderInfo != null ? builderInfo._2.get() : createArgsForConstructor()) : N.newInstance(clazz);
        }

        /**
         *
         * @param <T>
         * @param result
         * @return
         */
        @Beta
        public <T> T finishBeanResult(final Object result) {
            if (result == null) {
                return null;
            }

            return isImmutable ? (builderInfo != null ? (T) builderInfo._3.apply(result) : newInstance(((Object[]) result))) : (T) result;
        }

        private Object[] createArgsForConstructor() {
            if (allArgsConstructor == null) {
                throw new UnsupportedOperationException("No all arguments constructor found in class: " + ClassUtil.getCanonicalClassName(clazz));
            }

            return defaultFieldValues.clone();
        }

        /**
         *
         * @return
         */
        @Override
        public int hashCode() {
            return (clazz == null) ? 0 : clazz.hashCode();
        }

        /**
         *
         * @param obj
         * @return true, if successful
         */
        @Override
        public boolean equals(Object obj) {
            return this == obj || (obj instanceof BeanInfo && N.equals(((BeanInfo) obj).clazz, clazz));
        }

        /**
         *
         *
         * @return
         */
        @Override
        public String toString() {
            return ClassUtil.getCanonicalClassName(clazz);
        }
    }

    public static class PropInfo {

        static final char[] NULL_CHAR_ARRAY = "null".toCharArray();

        public final Class<Object> declaringClass;

        public final String name;

        public final ImmutableList<String> aliases;

        public final Class<Object> clazz;

        public final Type<Object> type;

        public final Field field;

        public final Method getMethod;

        public final Method setMethod;

        public final ImmutableMap<Class<? extends Annotation>, Annotation> annotations;

        public final Type<Object> jsonXmlType;

        public final Type<Object> dbType;

        final JsonNameTag[] jsonNameTags;

        final XmlNameTag[] xmlNameTags;

        final boolean isFieldAccessible;

        final boolean isFieldSettable;

        final String dateFormat;

        final TimeZone timeZone;

        final ZoneId zoneId;

        final DateTimeFormatter dateTimeFormatter;

        final boolean isJsonRawValue;

        final JodaDateTimeFormatterHolder jodaDTFH;

        final boolean isLongDateFormat;

        final NumberFormat numberFormat;

        final boolean hasFormat;

        public final boolean isTransient;

        public final Expose jsonXmlExpose;

        public final boolean isMarkedToId;

        public final boolean isMarkedToReadOnlyId;

        public final boolean isMarkedToColumn;

        public final Optional<String> columnName;

        final boolean canSetFieldByGetMethod;

        final int fieldOrder;

        final boolean isImmutableBean;

        final boolean isByBuilder;

        @SuppressWarnings("deprecation")
        PropInfo(String propName) {
            this.declaringClass = null;
            this.name = propName;
            this.aliases = ImmutableList.empty();
            field = null;
            getMethod = null;
            setMethod = null;
            annotations = ImmutableMap.empty();
            clazz = null;
            type = null;

            jsonXmlType = null;
            dbType = null;
            xmlNameTags = null;
            jsonNameTags = null;
            isFieldAccessible = false;
            isFieldSettable = false;
            dateFormat = null;
            timeZone = null;
            zoneId = null;
            dateTimeFormatter = null;
            isJsonRawValue = false;
            jodaDTFH = null;
            isLongDateFormat = false;
            numberFormat = null;
            hasFormat = false;

            isTransient = false;
            isMarkedToId = false;
            isMarkedToReadOnlyId = false;
            jsonXmlExpose = JsonXmlField.Expose.DEFAULT;

            isMarkedToColumn = false;
            columnName = Optional.<String> empty();
            canSetFieldByGetMethod = false;

            fieldOrder = -1;
            isImmutableBean = false;
            isByBuilder = false;
        }

        @SuppressWarnings("deprecation")
        PropInfo(final String propName, final Field field, final Method getMethod, final Method setMethod, final JsonXmlConfig jsonXmlConfig,
                final ImmutableMap<Class<? extends Annotation>, Annotation> classAnnotations, final int fieldOrder, final boolean isImmutableBean,
                final boolean isByBuilder, final List<String> idPropNames, final List<String> readOnlyIdPropNames) {
            this.declaringClass = (Class<Object>) (field != null ? field.getDeclaringClass() : getMethod.getDeclaringClass());
            this.field = field;
            this.name = propName;
            this.aliases = ImmutableList.of(getAliases(field));
            this.getMethod = getMethod;
            this.setMethod = setMethod; // ClassUtil.getPropSetMethod(declaringClass, propName);
            this.annotations = ImmutableMap.wrap(getAnnotations());
            this.isTransient = annotations.containsKey(Transient.class) || annotations.keySet().stream().anyMatch(it -> it.getSimpleName().equals("Transient")) //NOSONAR
                    || (field != null && Modifier.isTransient(field.getModifiers()));

            this.clazz = (Class<Object>) (field == null ? (setMethod == null ? getMethod.getReturnType() : setMethod.getParameterTypes()[0]) : field.getType());
            this.type = getType(getAnnoType(this.field, this.getMethod, this.setMethod, clazz, jsonXmlConfig), this.field, this.getMethod, this.setMethod,
                    clazz, declaringClass);
            this.jsonXmlType = getType(getJsonXmlAnnoType(this.field, this.getMethod, this.setMethod, clazz, jsonXmlConfig), this.field, this.getMethod,
                    this.setMethod, clazz, declaringClass);
            this.dbType = getType(getDBAnnoType(this.field, this.getMethod, this.setMethod, clazz), this.field, this.getMethod, this.setMethod, clazz,
                    declaringClass);

            this.jsonNameTags = getJsonNameTags(propName, field);
            this.xmlNameTags = getXmlNameTags(propName, field, jsonXmlType.name(), false);

            if (field != null && !this.annotations.containsKey(AccessFieldByMethod.class) && !classAnnotations.containsKey(AccessFieldByMethod.class)) {
                ClassUtil.setAccessibleQuietly(field, true);
            }

            isFieldAccessible = field != null && field.isAccessible();
            isFieldSettable = field != null && field.isAccessible() && !Modifier.isFinal(field.getModifiers()) && !isByBuilder;

            String timeZoneStr = Strings.trim(getTimeZone(field, jsonXmlConfig));
            String dateFormatStr = Strings.trim(getDateFormat(field, jsonXmlConfig));
            this.dateFormat = Strings.isEmpty(dateFormatStr) ? null : dateFormatStr;
            this.timeZone = Strings.isEmpty(timeZoneStr) ? TimeZone.getDefault() : TimeZone.getTimeZone(timeZoneStr);
            this.zoneId = timeZone.toZoneId();
            this.dateTimeFormatter = Strings.isEmpty(dateFormat) ? null : DateTimeFormatter.ofPattern(dateFormat).withZone(zoneId);
            this.isJsonRawValue = isJsonRawValue(field);

            JodaDateTimeFormatterHolder tmpJodaDTFH = null;

            try {
                if (Class.forName("org.joda.time.DateTime") != null) {
                    tmpJodaDTFH = new JodaDateTimeFormatterHolder(dateFormat, timeZone);
                }
            } catch (Throwable e) {
                // ignore.
            }

            this.jodaDTFH = tmpJodaDTFH;

            this.isLongDateFormat = Strings.isNotEmpty(dateFormat) && "long".equalsIgnoreCase(dateFormat);

            if (isLongDateFormat && (java.time.LocalTime.class.isAssignableFrom(clazz) || java.time.LocalDate.class.isAssignableFrom(clazz))) {
                throw new UnsupportedOperationException("Date format can't be 'long' for type java.time.LocalTime/LocalDate");
            }

            String numberFormatStr = Strings.trim(getNumberFormat(field, jsonXmlConfig));
            this.numberFormat = Strings.isEmpty(numberFormatStr) ? null : new DecimalFormat(numberFormatStr);

            this.hasFormat = Strings.isNotEmpty(dateFormat) || numberFormat != null;

            this.jsonXmlExpose = field != null && field.isAnnotationPresent(JsonXmlField.class) ? field.getAnnotation(JsonXmlField.class).expose()
                    : JsonXmlField.Expose.DEFAULT;

            boolean tmpIsMarkedToId = this.annotations.containsKey(Id.class) || this.annotations.containsKey(ReadOnlyId.class)
                    || idPropNames.contains(propName);

            if (!tmpIsMarkedToId) {
                try {
                    tmpIsMarkedToId = this.annotations.containsKey(javax.persistence.Id.class);
                } catch (Throwable e) {
                    // ignore
                }
            }

            this.isMarkedToId = tmpIsMarkedToId;

            this.isMarkedToReadOnlyId = this.annotations.containsKey(ReadOnlyId.class) || readOnlyIdPropNames.contains(propName);

            String tmpColumnName = null;
            boolean tmpIsMarkedToColumn = false;

            if (this.annotations.containsKey(Column.class)) {
                tmpIsMarkedToColumn = true;

                tmpColumnName = ((Column) this.annotations.get(Column.class)).value();

                if (Strings.isEmpty(tmpColumnName)) {
                    tmpColumnName = ((Column) this.annotations.get(Column.class)).name();
                }
            } else {
                try {
                    if (this.annotations.containsKey(javax.persistence.Column.class)) {
                        tmpIsMarkedToColumn = true;

                        tmpColumnName = ((javax.persistence.Column) this.annotations.get(javax.persistence.Column.class)).name();
                    }
                } catch (Throwable e) {
                    // ignore
                }
            }

            if (Strings.isNotEmpty(tmpColumnName) && !tmpColumnName.equals(Strings.strip(tmpColumnName))) {
                throw new IllegalArgumentException("Column name: \"" + tmpColumnName + "\" must not start or end with any whitespace for field: " + field);
            }

            this.isMarkedToColumn = tmpIsMarkedToColumn;

            this.columnName = Strings.isEmpty(tmpColumnName) ? Optional.<String> empty() : Optional.ofNullable(tmpColumnName);

            this.canSetFieldByGetMethod = ClassUtil.isRegisteredXMLBindingClass(declaringClass) && getMethod != null
                    && (Map.class.isAssignableFrom(getMethod.getReturnType()) || Collection.class.isAssignableFrom(getMethod.getReturnType()));

            this.fieldOrder = fieldOrder;
            this.isImmutableBean = isImmutableBean;
            this.isByBuilder = isByBuilder;
        }

        /**
         * Gets the prop value.
         *
         * @param <T>
         * @param obj
         * @return
         */
        @SuppressWarnings("unchecked")
        public <T> T getPropValue(Object obj) {
            if (isImmutableBean && obj instanceof Object[]) {
                return (T) ((Object[]) obj)[fieldOrder];
            }

            try {
                return (T) (isFieldAccessible ? field.get(obj) : getMethod.invoke(obj));
            } catch (Exception e) {
                throw ExceptionUtil.toRuntimeException(e);
            }
        }

        /**
         * Sets the prop value.
         *
         * @param obj
         * @param propValue
         */
        public void setPropValue(final Object obj, Object propValue) {
            if (isJsonRawValue && propValue != null && !clazz.isAssignableFrom(propValue.getClass())) {
                propValue = N.toJSON(propValue);
            }

            if (isImmutableBean && !isByBuilder) {
                ((Object[]) obj)[fieldOrder] = propValue;
                return;
            }

            propValue = propValue == null ? type.defaultValue() : propValue;

            try {
                if (isFieldSettable) {
                    field.set(obj, propValue); //NOSONAR
                } else if (setMethod != null) {
                    setMethod.invoke(obj, propValue);
                } else if (canSetFieldByGetMethod) {
                    ClassUtil.setPropValueByGet(obj, getMethod, propValue);
                } else {
                    field.set(obj, propValue); //NOSONAR
                }

            } catch (Exception e) {
                if (logger.isWarnEnabled()) {
                    logger.warn("Failed to set value for field: {} in class: {} with value type {}", field == null ? name : field.getName(),
                            declaringClass.getName(), propValue == null ? "null" : propValue.getClass().getName());
                }

                propValue = N.convert(propValue, jsonXmlType);

                try {
                    if (isFieldSettable) {
                        field.set(obj, propValue); //NOSONAR
                    } else if (setMethod != null) {
                        setMethod.invoke(obj, propValue);
                    } else {
                        field.set(obj, propValue); //NOSONAR
                    }
                } catch (Exception e2) {
                    throw ExceptionUtil.toRuntimeException(e);
                }
            }
        }

        static final Map<Class<?>, DateTimeReaderWriter<?>> propFuncMap = new HashMap<>();

        static {
            propFuncMap.put(java.util.Date.class, new DateTimeReaderWriter<java.util.Date>() {
                @Override
                public java.util.Date read(PropInfo propInfo, String strValue) {
                    if (propInfo.isLongDateFormat) {
                        return new java.util.Date(Numbers.toLong(strValue));
                    } else {
                        return DateUtil.parseJUDate(strValue, propInfo.dateFormat, propInfo.timeZone);
                    }
                }

                @Override
                public void write(PropInfo propInfo, java.util.Date x, CharacterWriter writer) throws IOException {
                    if (propInfo.isLongDateFormat) {
                        writer.write(x.getTime());
                    } else {
                        DateUtil.format(writer, x, propInfo.dateFormat, propInfo.timeZone);
                    }
                }
            });

            propFuncMap.put(java.util.Calendar.class, new DateTimeReaderWriter<java.util.Calendar>() {
                @Override
                public java.util.Calendar read(PropInfo propInfo, String strValue) {
                    if (propInfo.isLongDateFormat) {
                        final Calendar calendar = Calendar.getInstance();
                        calendar.setTimeInMillis(Numbers.toLong(strValue));
                        calendar.setTimeZone(propInfo.timeZone);
                        return calendar;
                    } else {
                        return DateUtil.parseCalendar(strValue, propInfo.dateFormat, propInfo.timeZone);
                    }
                }

                @Override
                public void write(PropInfo propInfo, java.util.Calendar x, CharacterWriter writer) throws IOException {
                    if (propInfo.isLongDateFormat) {
                        writer.write(x.getTimeInMillis());
                    } else {
                        DateUtil.format(writer, x, propInfo.dateFormat, propInfo.timeZone);
                    }

                }
            });

            propFuncMap.put(java.sql.Timestamp.class, new DateTimeReaderWriter<java.sql.Timestamp>() {
                @Override
                public java.sql.Timestamp read(PropInfo propInfo, String strValue) {
                    if (propInfo.isLongDateFormat) {
                        return new java.sql.Timestamp(Numbers.toLong(strValue));
                    } else {
                        return DateUtil.parseTimestamp(strValue, propInfo.dateFormat, propInfo.timeZone);
                    }
                }

                @Override
                public void write(PropInfo propInfo, java.sql.Timestamp x, CharacterWriter writer) throws IOException {
                    if (propInfo.isLongDateFormat) {
                        writer.write(x.getTime());
                    } else {
                        DateUtil.format(writer, x, propInfo.dateFormat, propInfo.timeZone);
                    }
                }
            });

            propFuncMap.put(java.sql.Date.class, new DateTimeReaderWriter<java.sql.Date>() {
                @Override
                public java.sql.Date read(PropInfo propInfo, String strValue) {
                    if (propInfo.isLongDateFormat) {
                        return new java.sql.Date(Numbers.toLong(strValue));
                    } else {
                        return DateUtil.parseDate(strValue, propInfo.dateFormat, propInfo.timeZone);
                    }
                }

                @Override
                public void write(PropInfo propInfo, java.sql.Date x, CharacterWriter writer) throws IOException {
                    if (propInfo.isLongDateFormat) {
                        writer.write(x.getTime());
                    } else {
                        DateUtil.format(writer, x, propInfo.dateFormat, propInfo.timeZone);
                    }
                }
            });

            propFuncMap.put(java.sql.Time.class, new DateTimeReaderWriter<java.sql.Time>() {
                @Override
                public java.sql.Time read(PropInfo propInfo, String strValue) {
                    if (propInfo.isLongDateFormat) {
                        return new java.sql.Time(Numbers.toLong(strValue));
                    } else {
                        return DateUtil.parseTime(strValue, propInfo.dateFormat, propInfo.timeZone);
                    }
                }

                @Override
                public void write(PropInfo propInfo, java.sql.Time x, CharacterWriter writer) throws IOException {
                    if (propInfo.isLongDateFormat) {
                        writer.write(x.getTime());
                    } else {
                        DateUtil.format(writer, x, propInfo.dateFormat, propInfo.timeZone);
                    }
                }
            });

            propFuncMap.put(java.time.LocalDateTime.class, new DateTimeReaderWriter<java.time.LocalDateTime>() {
                @Override
                public java.time.LocalDateTime read(PropInfo propInfo, String strValue) {
                    if (propInfo.isLongDateFormat) {
                        return new java.sql.Timestamp(Numbers.toLong(strValue)).toLocalDateTime();
                    } else {
                        return java.time.LocalDateTime.parse(strValue, propInfo.dateTimeFormatter);
                    }
                }

                @Override
                public void write(PropInfo propInfo, java.time.LocalDateTime x, CharacterWriter writer) throws IOException {
                    if (propInfo.isLongDateFormat) {
                        writer.write(x.atZone(propInfo.zoneId).toInstant().toEpochMilli());
                    } else {
                        propInfo.dateTimeFormatter.formatTo(x, writer);
                    }
                }
            });

            propFuncMap.put(java.time.LocalDate.class, new DateTimeReaderWriter<java.time.LocalDate>() {
                @Override
                public java.time.LocalDate read(PropInfo propInfo, String strValue) {
                    if (propInfo.isLongDateFormat) {
                        // return new java.sql.Date(N.parseLong(strValue)).toLocalDate();
                        throw new UnsupportedOperationException("Date format can't be 'long' for type java.time.LocalDate");
                    } else {
                        return java.time.LocalDate.parse(strValue, propInfo.dateTimeFormatter);
                    }
                }

                @Override
                public void write(PropInfo propInfo, java.time.LocalDate x, CharacterWriter writer) throws IOException {
                    if (propInfo.isLongDateFormat) {
                        // writer.write(x.atStartOfDay(propInfo.zoneId).toInstant().toEpochMilli());
                        throw new UnsupportedOperationException("Date format can't be 'long' for type java.time.LocalDate");
                    } else {
                        propInfo.dateTimeFormatter.formatTo(x, writer);
                    }
                }
            });

            propFuncMap.put(java.time.LocalTime.class, new DateTimeReaderWriter<java.time.LocalTime>() {
                @Override
                public java.time.LocalTime read(PropInfo propInfo, String strValue) {
                    if (propInfo.isLongDateFormat) {
                        // return new java.sql.Time(N.parseLong(strValue)).toLocalTime();
                        throw new UnsupportedOperationException("Date format can't be 'long' for type java.time.LocalTime");
                    } else {
                        return java.time.LocalTime.parse(strValue, propInfo.dateTimeFormatter);
                    }
                }

                @Override
                public void write(PropInfo propInfo, java.time.LocalTime x, CharacterWriter writer) throws IOException {
                    if (propInfo.isLongDateFormat) {
                        // writer.write(java.sql.Time.valueOf(x).getTime());
                        throw new UnsupportedOperationException("Date format can't be 'long' for type java.time.LocalTime");
                    } else {
                        propInfo.dateTimeFormatter.formatTo(x, writer);
                    }
                }
            });

            propFuncMap.put(java.time.ZonedDateTime.class, new DateTimeReaderWriter<java.time.ZonedDateTime>() {
                @Override
                public java.time.ZonedDateTime read(PropInfo propInfo, String strValue) {
                    if (propInfo.isLongDateFormat) {
                        return ZonedDateTime.ofInstant(Instant.ofEpochMilli(Numbers.toLong(strValue)), propInfo.zoneId);
                    } else {
                        return ZonedDateTime.parse(strValue, propInfo.dateTimeFormatter);
                    }
                }

                @Override
                public void write(PropInfo propInfo, java.time.ZonedDateTime x, CharacterWriter writer) throws IOException {
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
                        public org.joda.time.DateTime read(PropInfo propInfo, String strValue) {
                            if (propInfo.isLongDateFormat) {
                                final org.joda.time.DateTime dt = new org.joda.time.DateTime(Numbers.toLong(strValue));
                                return dt.getZone().equals(propInfo.jodaDTFH.dtz) ? dt : dt.withZone(propInfo.jodaDTFH.dtz);
                            } else {
                                return propInfo.jodaDTFH.dtf.parseDateTime(strValue);
                            }
                        }

                        @Override
                        public void write(PropInfo propInfo, org.joda.time.DateTime x, CharacterWriter writer) throws IOException {
                            if (propInfo.isLongDateFormat) {
                                writer.write(x.getMillis());
                            } else {
                                propInfo.jodaDTFH.dtf.printTo(writer, x);
                            }
                        }
                    });

                    propFuncMap.put(org.joda.time.MutableDateTime.class, new DateTimeReaderWriter<org.joda.time.MutableDateTime>() {
                        @Override
                        public org.joda.time.MutableDateTime read(PropInfo propInfo, String strValue) {
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
                        public void write(PropInfo propInfo, org.joda.time.MutableDateTime x, CharacterWriter writer) throws IOException {
                            if (propInfo.isLongDateFormat) {
                                writer.write(x.getMillis());
                            } else {
                                propInfo.jodaDTFH.dtf.printTo(writer, x);
                            }
                        }
                    });
                }
            } catch (Throwable e) {
                // ignore.
            }
        }

        /**
         * Read prop value.
         *
         * @param strValue
         * @return
         */
        public Object readPropValue(String strValue) {
            if (hasFormat) {
                final DateTimeReaderWriter<?> func = propFuncMap.get(clazz);

                if (func == null) {
                    //    if (isLongDateFormat) {
                    //        return type.valueOf(strValue);
                    //    } else {
                    //        return type.valueOf(DateUtil.parseJUDate(strValue, this.dateFormat).getTime());
                    //    }

                    throw new UnsupportedOperationException("'DateFormat' annotation for field: " + field
                            + " is only supported for types: java.util.Date/Calendar, java.sql.Date/Time/Timestamp, java.time.LocalDateTime/LocalDate/LocalTime/ZonedDateTime, not supported for: "
                            + ClassUtil.getCanonicalClassName(clazz));
                }

                return func.read(this, strValue);
            } else {
                return jsonXmlType.valueOf(strValue);
            }
        }

        /**
         * Write prop value.
         *
         * @param writer
         * @param x
         * @param config
         * @throws IOException Signals that an I/O exception has occurred.
         */
        public void writePropValue(CharacterWriter writer, Object x, SerializationConfig<?> config) throws IOException {
            if (hasFormat) {
                if (x == null) {
                    writer.write(NULL_CHAR_ARRAY);
                } else if (dateFormat != null) {
                    boolean isQuote = (config != null) && (config.getStringQuotation() != 0);

                    if (isQuote) {
                        writer.write(config.getStringQuotation());
                    }

                    @SuppressWarnings("rawtypes")
                    final DateTimeReaderWriter func = propFuncMap.get(clazz);

                    if (func == null) {
                        //    if (isLongDateFormat) {
                        //        return type.valueOf(strValue);
                        //    } else {
                        //        return type.valueOf(DateUtil.parseJUDate(strValue, this.dateFormat).getTime());
                        //    }

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
                    writer.write(((CharSequence) x).toString());
                }
            } else {
                jsonXmlType.writeCharacter(writer, x, config);
            }
        }

        /**
         *
         *
         * @param annotationClass
         * @return
         */
        public boolean isAnnotationPresent(final Class<? extends Annotation> annotationClass) {
            return annotations.containsKey(annotationClass);
        }

        /**
         *
         *
         * @param <T>
         * @param annotationClass
         * @return
         */
        public <T extends Annotation> T getAnnotation(final Class<T> annotationClass) {
            return (T) annotations.get(annotationClass);
        }

        private Map<Class<? extends Annotation>, Annotation> getAnnotations() {
            final Map<Class<? extends Annotation>, Annotation> annos = new HashMap<>();

            if (field != null && N.notNullOrEmpty(field.getAnnotations())) {
                for (Annotation anno : field.getAnnotations()) {
                    annos.put(anno.annotationType(), anno);
                }
            }

            if (getMethod != null && N.notNullOrEmpty(getMethod.getAnnotations())) {
                for (Annotation anno : getMethod.getAnnotations()) {
                    annos.put(anno.annotationType(), anno);
                }
            }

            if (setMethod != null && N.notNullOrEmpty(setMethod.getAnnotations())) {
                for (Annotation anno : setMethod.getAnnotations()) {
                    annos.put(anno.annotationType(), anno);
                }
            }

            return annos;
        }

        @SuppressWarnings("unused")
        private String getAnnoType(final Field field, final Method getMethod, final Method setMethod, final Class<?> propClass,
                final JsonXmlConfig jsonXmlConfig) {
            final com.landawn.abacus.annotation.Type typeAnno = getAnnotation(com.landawn.abacus.annotation.Type.class);

            if (typeAnno != null && (typeAnno.scope() == Scope.ALL || typeAnno.scope() == Scope.PARSER)) {
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

        @SuppressWarnings("unused")
        private String getJsonXmlAnnoType(final Field field, final Method getMethod, final Method setMethod, final Class<?> propClass,
                final JsonXmlConfig jsonXmlConfig) {
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

            if (typeAnno != null && (typeAnno.scope() == Scope.ALL || typeAnno.scope() == Scope.PARSER)) {
                final String typeName = getTypeName(typeAnno, propClass);

                if (Strings.isNotEmpty(typeName)) {
                    return typeName;
                }
            }

            return null;
        }

        /**
         * Gets the DB anno type.
         *
         * @param field
         * @param getMethod
         * @param setMethod
         * @param propClass
         * @return
         */
        @SuppressWarnings("unused")
        private String getDBAnnoType(final Field field, final Method getMethod, final Method setMethod, final Class<?> propClass) {
            final com.landawn.abacus.annotation.Type typeAnno = getAnnotation(com.landawn.abacus.annotation.Type.class);

            if (typeAnno != null && (typeAnno.scope() == Scope.ALL || typeAnno.scope() == Scope.DB)) {
                final String typeName = getTypeName(typeAnno, propClass);

                if (Strings.isNotEmpty(typeName)) {
                    return typeName;
                }
            }

            return null;
        }

        private String getTypeName(final com.landawn.abacus.annotation.Type typeAnno, final Class<?> propClass) {
            @SuppressWarnings("deprecation")

            final Optional<String> typeName = MoreStringUtil.firstNonEmpty(typeAnno.value(), typeAnno.name());

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
                } catch (Exception e) {
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

        /**
         * Gets the type.
         *
         * @param <T>
         * @param annoType
         * @param field
         * @param getMethod
         * @param setMethod
         * @param propClass
         * @param beanClass
         * @return
         */
        @SuppressWarnings("unused")
        private <T> Type<T> getType(final String annoType, final Field field, final Method getMethod, final Method setMethod, final Class<?> propClass,
                final Class<?> beanClass) {
            if (Strings.isEmpty(annoType)) {
                final String parameterizedTypeName = field != null ? ClassUtil.getParameterizedTypeNameByField(field)
                        : ClassUtil.getParameterizedTypeNameByMethod((setMethod == null) ? getMethod : setMethod);

                return N.typeOf(parameterizedTypeName);
            } else {
                Type<T> localType = null;

                try {
                    localType = N.typeOf(annoType);
                } catch (Exception e) {
                    // ignore
                }

                if ((localType == null || localType.getClass().equals(ObjectType.class)) && Strings.isNotEmpty(ClassUtil.getPackageName(beanClass))) {
                    final String pkgName = ClassUtil.getPackageName(beanClass);
                    final StringBuilder sb = new StringBuilder();
                    int start = 0;

                    for (int i = 0, len = annoType.length(); i < len; i++) {
                        char ch = annoType.charAt(i);

                        if (ch == '<' || ch == '>' || ch == ' ' || ch == ',') {
                            String str = annoType.substring(start, i);

                            if (str.length() > 0 && N.typeOf(str).isObjectType() && !N.typeOf(pkgName + "." + str).isObjectType()) {
                                sb.append(pkgName + "." + str);
                            } else {
                                sb.append(str);
                            }

                            sb.append(ch);
                            start = i + 1;
                        }
                    }

                    if (start < annoType.length()) {
                        String str = annoType.substring(start);

                        if (N.typeOf(str).isObjectType() && !N.typeOf(pkgName + "." + str).isObjectType()) {
                            sb.append(pkgName + "." + str);
                        } else {
                            sb.append(str);
                        }
                    }

                    localType = N.typeOf(sb.toString());
                }

                return localType;
            }
        }

        /**
         *
         * @return
         */
        @Override
        public int hashCode() {
            return ((name == null) ? 0 : name.hashCode()) * 31 + ((field == null) ? 0 : field.hashCode());
        }

        /**
         *
         * @param obj
         * @return true, if successful
         */
        @Override
        public boolean equals(Object obj) {
            return this == obj || ((obj instanceof PropInfo) && ((PropInfo) obj).name.equals(name)) && N.equals(((PropInfo) obj).field, field);
        }

        /**
         *
         * @return
         */
        @Override
        public String toString() {
            return name;
        }
    }

    static class ASMPropInfo extends PropInfo { //NOSONAR
        final com.esotericsoftware.reflectasm.MethodAccess getMethodAccess;
        final com.esotericsoftware.reflectasm.MethodAccess setMethodAccess;
        final com.esotericsoftware.reflectasm.FieldAccess fieldAccess;

        final int getMethodAccessIndex;
        final int setMethodAccessIndex;
        final int fieldAccessIndex;

        ASMPropInfo(final String name, final Field field, final Method getMethod, final Method setMethod, final JsonXmlConfig jsonXmlConfig,
                final ImmutableMap<Class<? extends Annotation>, Annotation> classAnnotations, final int fieldOrder, final boolean isImmutableBean,
                final boolean isByBuilder, final List<String> idPropNames, final List<String> readOnlyIdPropNames) {
            super(name, field, getMethod, setMethod, jsonXmlConfig, classAnnotations, fieldOrder, isImmutableBean, isByBuilder, idPropNames,
                    readOnlyIdPropNames);

            getMethodAccess = getMethod == null ? null : com.esotericsoftware.reflectasm.MethodAccess.get(getMethod.getDeclaringClass());
            setMethodAccess = setMethod == null ? null : com.esotericsoftware.reflectasm.MethodAccess.get(setMethod.getDeclaringClass());
            fieldAccess = field == null ? null : com.esotericsoftware.reflectasm.FieldAccess.get(field.getDeclaringClass());

            getMethodAccessIndex = getMethod == null ? -1 : getMethodAccess.getIndex(getMethod.getName(), 0);
            setMethodAccessIndex = setMethod == null ? -1 : setMethodAccess.getIndex(setMethod.getName(), setMethod.getParameterTypes());
            fieldAccessIndex = (field == null || !this.isFieldAccessible || !Modifier.isPublic(field.getModifiers()) || Modifier.isFinal(field.getModifiers()))
                    ? -1
                    : fieldAccess.getIndex(field.getName());
        }

        /**
         * Gets the prop value.
         *
         * @param <T>
         * @param obj
         * @return
         */
        @Override
        @SuppressWarnings("unchecked")
        public <T> T getPropValue(Object obj) {
            return (T) ((fieldAccessIndex > -1) ? fieldAccess.get(obj, fieldAccessIndex) : getMethodAccess.invoke(obj, getMethodAccessIndex));
        }

        /**
         * Sets the prop value.
         *
         * @param obj
         * @param propValue
         */
        @Override
        public void setPropValue(final Object obj, Object propValue) {
            if (isImmutableBean && !isByBuilder) {
                ((Object[]) obj)[fieldOrder] = propValue;

                return;
            }

            propValue = propValue == null ? type.defaultValue() : propValue;

            try {
                if (isFieldSettable && fieldAccessIndex > -1) {
                    fieldAccess.set(obj, fieldAccessIndex, propValue);
                } else if (setMethodAccessIndex > -1) {
                    setMethodAccess.invoke(obj, setMethodAccessIndex, propValue);
                } else if (canSetFieldByGetMethod) {
                    ClassUtil.setPropValueByGet(obj, getMethod, propValue);
                } else {
                    field.set(obj, propValue); //NOSONAR
                }

            } catch (Exception e) {
                if (logger.isWarnEnabled()) {
                    logger.warn("Failed to set value for field: {} in class: {} with value type {}", field == null ? name : field.getName(),
                            declaringClass.getName(), propValue == null ? "null" : propValue.getClass().getName());
                }

                propValue = N.convert(propValue, jsonXmlType);

                if (fieldAccessIndex > -1) {
                    fieldAccess.set(obj, fieldAccessIndex, propValue);
                } else if (setMethodAccessIndex > -1) {
                    getMethodAccess.invoke(obj, setMethodAccessIndex, propValue);
                } else {
                    try {
                        field.set(obj, propValue); //NOSONAR
                    } catch (Exception e2) {
                        throw ExceptionUtil.toRuntimeException(e);
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

        public JsonNameTag(String name) {
            this.name = name.toCharArray();
            this.nameWithColon = (name + ":").toCharArray();
            this.nameNull = (name + ":null").toCharArray();
            this.quotedName = ("\"" + name + "\"").toCharArray();
            this.quotedNameWithColon = ("\"" + name + "\":").toCharArray();
            this.quotedNameNull = ("\"" + name + "\":null").toCharArray();
        }

        @Override
        public int hashCode() {
            return (name == null) ? 0 : N.hashCode(name);
        }

        @Override
        public boolean equals(Object obj) {
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

        public XmlNameTag(String name, String typeName, boolean isBean) {
            this.name = name.toCharArray();

            final String typeAttr = typeName.replaceAll("<", "&lt;").replaceAll(">", "&gt;"); //NOSONAR

            if (isBean) {
                this.epStart = ("<bean name=\"" + name + "\">").toCharArray();
                this.epStartWithType = ("<bean name=\"" + name + "\" type=\"" + typeAttr + "\">").toCharArray();
                this.epEnd = ("</bean>").toCharArray();
                this.epNull = ("<bean name=\"" + name + "\" isNull=\"true\" />").toCharArray();
                this.epNullWithType = ("<bean name=\"" + name + "\" type=\"" + typeAttr + "\" isNull=\"true\" />").toCharArray();
            } else {
                this.epStart = ("<property name=\"" + name + "\">").toCharArray();
                this.epStartWithType = ("<property name=\"" + name + "\" type=\"" + typeAttr + "\">").toCharArray();
                this.epEnd = ("</property>").toCharArray();
                this.epNull = ("<property name=\"" + name + "\" isNull=\"true\" />").toCharArray();
                this.epNullWithType = ("<property name=\"" + name + "\" type=\"" + typeAttr + "\" isNull=\"true\" />").toCharArray();
            }

            this.namedStart = ("<" + name + ">").toCharArray();
            this.namedStartWithType = ("<" + name + " type=\"" + typeAttr + "\">").toCharArray();
            this.namedEnd = ("</" + name + ">").toCharArray();
            this.namedNull = ("<" + name + " isNull=\"true\" />").toCharArray();
            this.namedNullWithType = ("<" + name + " type=\"" + typeAttr + "\" isNull=\"true\" />").toCharArray();
        }

        @Override
        public int hashCode() {
            return (name == null) ? 0 : N.hashCode(name);
        }

        @Override
        public boolean equals(Object obj) {
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
            this.dtz = org.joda.time.DateTimeZone.forTimeZone(timeZone);
            this.dtf = org.joda.time.format.DateTimeFormat.forPattern(dateFormat).withZone(dtz);
        }
    }

    //    private static final Map<Class<?>, RecordInfo<?>> recordInfoMap = new ConcurrentHashMap<>();
    //
    //    /**
    //     *
    //     * @param recordClass
    //     * @return
    //     * @deprecated for internal use only
    //     */
    //    @Deprecated
    //    @Beta
    //    public static <T> RecordInfo<T> getRecordInfo(final Class<T> recordClass) {
    //        if (!ClassUtil.isRecordClass(recordClass)) {
    //            throw new IllegalArgumentException(ClassUtil.getCanonicalClassName(recordClass) + " is not a Record class");
    //        }
    //
    //        RecordInfo<T> recordInfo = (RecordInfo<T>) recordInfoMap.get(recordClass);
    //
    //        if (recordInfo == null) {
    //            final BeanInfo beanInfo = ParserUtil.getBeanInfo(recordClass);
    //
    //            final Field[] fields = recordClass.getDeclaredFields();
    //            final Map<String, Tuple5<String, Field, Method, PropInfo, Integer>> map = new LinkedHashMap<>(fields.length);
    //
    //            try {
    //                PropInfo propInfo = null;
    //                String name = null;
    //                int idx = 0;
    //
    //                for (Field field : fields) {
    //                    name = field.getName();
    //                    propInfo = beanInfo.getPropInfo(name);
    //
    //                    map.put(name, Tuple.of(name, field, recordClass.getDeclaredMethod(name), propInfo, idx++));
    //                }
    //            } catch (NoSuchMethodException | SecurityException e) {
    //                // Should never happen.
    //                throw ExceptionUtil.toRuntimeException(e);
    //            }
    //
    //            final Constructor<?> constructor = recordClass.getDeclaredConstructors()[0];
    //
    //            final Function<Object[], T> creator = new Function<Object[], T>() {
    //                @Override
    //                public T apply(final Object[] args) throws RuntimeException {
    //                    try {
    //                        return (T) constructor.newInstance(args);
    //                    } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
    //                        // Should never happen.
    //                        throw ExceptionUtil.toRuntimeException(e);
    //                    }
    //                }
    //            };
    //
    //            recordInfo = new RecordInfo<>(recordClass, beanInfo, creator, ImmutableList.copyOf(map.keySet()), ImmutableMap.wrap(map));
    //
    //            recordInfoMap.put(recordClass, recordInfo);
    //        }
    //
    //        return recordInfo;
    //    }
    //
    //    @Value
    //    @Accessors(fluent = true)
    //    public static final class RecordInfo<T> {
    //        private final Class<T> clazz;
    //        private final BeanInfo beanInfo;
    //        private final Function<Object[], T> creator;
    //        private) final ImmutableList<String> fieldNames;
    //        private final ImmutableMap<String, Tuple5<String, Field, Method, PropInfo, Integer>> fieldMap;
    //    }
}
