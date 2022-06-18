/*
 * Copyright (C) 2016 HaiYang Li
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

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.util.Locale;
import java.util.TimeZone;

import com.alibaba.fastjson.TypeReference;
import com.alibaba.fastjson.parser.Feature;
import com.alibaba.fastjson.parser.ParserConfig;
import com.alibaba.fastjson.parser.deserializer.FieldDeserializer;
import com.alibaba.fastjson.parser.deserializer.ObjectDeserializer;
import com.alibaba.fastjson.parser.deserializer.ParseProcess;
import com.alibaba.fastjson.serializer.JSONSerializer;
import com.alibaba.fastjson.serializer.SerializeFilter;
import com.alibaba.fastjson.serializer.SerializeWriter;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alibaba.fastjson.util.FieldInfo;
import com.alibaba.fastjson.util.JavaBeanInfo;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.N;

/**
 * It's a wrapper for fastJson because of it's performance.
 *
 * @author Haiyang Li
 * @since 0.8
 */
public final class Fj {

    public static final TimeZone DEFAULT_TIME_ZONE = com.alibaba.fastjson.JSON.defaultTimeZone;

    public static final Locale DFAULT_LOCAL = com.alibaba.fastjson.JSON.defaultLocale;

    public static final String DEFAULT_TYPE_KEY = com.alibaba.fastjson.JSON.DEFAULT_TYPE_KEY;

    public static final String DEFFAULT_DATE_FORMAT = com.alibaba.fastjson.JSON.DEFFAULT_DATE_FORMAT;

    public static final int DEFAULT_SERIALIZE_FEATURE;
    static {
        int features = 0;
        features |= SerializerFeature.QuoteFieldNames.getMask();
        features |= SerializerFeature.SkipTransientField.getMask();
        features |= SerializerFeature.WriteEnumUsingName.getMask();
        // features |= SerializerFeature.SortField.getMask();
        DEFAULT_SERIALIZE_FEATURE = features;
    }

    public static final int DEFAULT_DESERIALIZE_FEATURE;

    static {
        int features = 0;
        features |= Feature.AutoCloseSource.getMask();
        features |= Feature.InternFieldNames.getMask();
        features |= Feature.UseBigDecimal.getMask();
        features |= Feature.AllowUnQuotedFieldNames.getMask();
        features |= Feature.AllowSingleQuotes.getMask();
        features |= Feature.AllowArbitraryCommas.getMask();
        features |= Feature.SortFeidFastMatch.getMask();
        features |= Feature.IgnoreNotMatch.getMask();
        DEFAULT_DESERIALIZE_FEATURE = features;
    }

    static final SerializeFilter[] emptyFilters = {};

    static final SerializerFeature[] emptySerializerFeatures = {};

    static final Feature[] emptyFeatures = {};

    private Fj() {
        // singleton
    }

    /**
     *
     * @param srcObject
     * @return
     */
    public static String toJSON(Object srcObject) {
        return com.alibaba.fastjson.JSON.toJSONString(srcObject, DEFAULT_SERIALIZE_FEATURE);
    }

    /**
     *
     * @param srcObject
     * @param config
     * @return
     */
    public static String toJSON(Object srcObject, SerializeConfig config) {
        com.alibaba.fastjson.serializer.SerializeConfig seriConfig = SerializeConfig.globalInstance;
        SerializeFilter[] filters = emptyFilters;
        String dateFormat = Fj.DEFFAULT_DATE_FORMAT;
        int defaultFeatures = Fj.DEFAULT_SERIALIZE_FEATURE;
        SerializerFeature[] features = emptySerializerFeatures;

        if (config != null) {
            seriConfig = config.seriConfig;
            filters = config.getFilters();
            dateFormat = config.getDateformat();
            defaultFeatures = config.getDefaultFeatures();
            features = config.getFeatures();
        }

        if (seriConfig == null) {
            seriConfig = SerializeConfig.globalInstance;
        }

        if (filters == null) {
            filters = emptyFilters;
        }

        if (features == null) {
            features = emptySerializerFeatures;
        }

        return com.alibaba.fastjson.JSON.toJSONString(srcObject, seriConfig, filters, dateFormat, defaultFeatures, features);
    }

    /**
     *
     * @param output
     * @param srcObject
     */
    public static void toJSON(File output, Object srcObject) {
        toJSON(output, srcObject, null);
    }

    /**
     *
     * @param output
     * @param srcObject
     * @param config
     */
    public static void toJSON(File output, Object srcObject, SerializeConfig config) {
        IOUtil.createIfNotExists(output);

        final Writer writer = IOUtil.newBufferedWriter(output);

        try {
            toJSON(writer, srcObject, config);
        } finally {
            IOUtil.closeQuietly(writer);
        }
    }

    /**
     *
     * @param output
     * @param srcObject
     */
    public static void toJSON(OutputStream output, Object srcObject) {
        toJSON(output, srcObject, null);
    }

    /**
     *
     * @param output
     * @param srcObject
     * @param config
     */
    public static void toJSON(OutputStream output, Object srcObject, SerializeConfig config) {
        final Writer writer = IOUtil.newBufferedWriter(output);

        try {
            toJSON(writer, srcObject, config);
        } finally {
            // IOUtil.closeQuietly(writer);
        }
    }

    /**
     *
     * @param output
     * @param srcObject
     */
    public static void toJSON(Writer output, Object srcObject) {
        toJSON(output, srcObject, null);
    }

    /**
     *
     * @param output
     * @param srcObject
     * @param config
     */
    public static void toJSON(Writer output, Object srcObject, SerializeConfig config) {
        com.alibaba.fastjson.serializer.SerializeConfig seriConfig = SerializeConfig.globalInstance;
        SerializeFilter[] filters = emptyFilters;
        String dateFormat = Fj.DEFFAULT_DATE_FORMAT;
        int defaultFeatures = Fj.DEFAULT_SERIALIZE_FEATURE;
        SerializerFeature[] features = emptySerializerFeatures;

        if (config != null) {
            seriConfig = config.seriConfig;
            filters = config.getFilters();
            dateFormat = config.getDateformat();
            defaultFeatures = config.getDefaultFeatures();
            features = config.getFeatures();
        }

        if (seriConfig == null) {
            seriConfig = SerializeConfig.globalInstance;
        }

        if (filters == null) {
            filters = emptyFilters;
        }

        if (features == null) {
            features = emptySerializerFeatures;
        }

        try (final SerializeWriter writer = new SerializeWriter(output, defaultFeatures, features)) {
            final JSONSerializer serializer = new JSONSerializer(writer, seriConfig);

            if (N.notNullOrEmpty(dateFormat)) {
                serializer.setDateFormat(dateFormat);
                serializer.config(SerializerFeature.WriteDateUseDateFormat, true);
            }

            if (N.notNullOrEmpty(filters)) {
                for (SerializeFilter filter : filters) {
                    serializer.addFilter(filter);
                }
            }

            serializer.write(srcObject);
        }
    }

    /**
     *
     * @param <T>
     * @param targetType
     * @param json
     * @return
     */
    public static <T> T fromJSON(Class<T> targetType, String json) {
        return fromJSON(targetType, json, null);
    }

    /**
     *
     * @param <T>
     * @param targetType
     * @param json
     * @param config
     * @return
     */
    public static <T> T fromJSON(Class<T> targetType, String json, DeserializeConfig config) {
        ParserConfig parseConfig = DeserializeConfig.globalInstance;
        ParseProcess processor = null;
        int featureValues = Fj.DEFAULT_DESERIALIZE_FEATURE;
        Feature[] features = emptyFeatures;

        if (config != null) {
            parseConfig = config.parserConfig;
            processor = config.getParseProcess();
            featureValues = config.getFeatureValues();
            features = config.getFeatures();
        }

        if (parseConfig == null) {
            parseConfig = DeserializeConfig.globalInstance;
        }

        if (features == null) {
            features = emptyFeatures;
        }

        return com.alibaba.fastjson.JSON.parseObject(json, targetType, parseConfig, processor, featureValues, features);
    }

    /**
     *
     * @param <T>
     * @param targetType
     * @param json
     * @return
     */
    public static <T> T fromJSON(Class<T> targetType, File json) {
        return fromJSON(targetType, json, null);
    }

    /**
     *
     * @param <T>
     * @param targetType
     * @param json
     * @param config
     * @return
     */
    public static <T> T fromJSON(Class<T> targetType, File json, DeserializeConfig config) {
        final Reader reader = IOUtil.newBufferedReader(json);

        try {
            return fromJSON(targetType, reader, config);
        } finally {
            IOUtil.closeQuietly(reader);
        }
    }

    /**
     *
     * @param <T>
     * @param targetType
     * @param json
     * @return
     */
    public static <T> T fromJSON(Class<T> targetType, InputStream json) {
        return fromJSON(targetType, json, null);
    }

    /**
     *
     * @param <T>
     * @param targetType
     * @param json
     * @param config
     * @return
     */
    public static <T> T fromJSON(Class<T> targetType, InputStream json, DeserializeConfig config) {
        final Reader reader = IOUtil.newBufferedReader(json);

        try {
            return fromJSON(targetType, reader, config);
        } finally {
            // IOUtil.closeQuietly(reader);
        }
    }

    /**
     *
     * @param <T>
     * @param targetType
     * @param json
     * @return
     */
    public static <T> T fromJSON(Class<T> targetType, Reader json) {
        return fromJSON(targetType, json, null);
    }

    /**
     *
     * @param <T>
     * @param targetType
     * @param json
     * @param config
     * @return
     */
    public static <T> T fromJSON(Class<T> targetType, Reader json, DeserializeConfig config) {
        return fromJSON(targetType, IOUtil.readString(json), config);
    }

    /**
     *
     * @param <T>
     * @param targetType
     * @param json
     * @return
     */
    public static <T> T fromJSON(TypeReference<T> targetType, String json) {
        return fromJSON(targetType, json, null);
    }

    /**
     *
     * @param <T>
     * @param targetType
     * @param json
     * @param config
     * @return
     */
    public static <T> T fromJSON(TypeReference<T> targetType, String json, DeserializeConfig config) {
        ParserConfig parseConfig = DeserializeConfig.globalInstance;
        ParseProcess processor = null;
        int featureValues = Fj.DEFAULT_DESERIALIZE_FEATURE;
        Feature[] features = emptyFeatures;

        if (config != null) {
            parseConfig = config.parserConfig;
            processor = config.getParseProcess();
            featureValues = config.getFeatureValues();
            features = config.getFeatures();
        }

        if (parseConfig == null) {
            parseConfig = DeserializeConfig.globalInstance;
        }

        if (features == null) {
            features = emptyFeatures;
        }

        return com.alibaba.fastjson.JSON.parseObject(json, targetType.getType(), parseConfig, processor, featureValues, features);
    }

    /**
     *
     * @param <T>
     * @param targetType
     * @param json
     * @return
     */
    public static <T> T fromJSON(TypeReference<T> targetType, File json) {
        return fromJSON(targetType, json, null);
    }

    /**
     *
     * @param <T>
     * @param targetType
     * @param json
     * @param config
     * @return
     */
    public static <T> T fromJSON(TypeReference<T> targetType, File json, DeserializeConfig config) {
        final Reader reader = IOUtil.newBufferedReader(json);

        try {
            return fromJSON(targetType, reader, config);
        } finally {
            IOUtil.closeQuietly(reader);
        }
    }

    /**
     *
     * @param <T>
     * @param targetType
     * @param json
     * @return
     */
    public static <T> T fromJSON(TypeReference<T> targetType, InputStream json) {
        return fromJSON(targetType, json, null);
    }

    /**
     *
     * @param <T>
     * @param targetType
     * @param json
     * @param config
     * @return
     */
    public static <T> T fromJSON(TypeReference<T> targetType, InputStream json, DeserializeConfig config) {
        final Reader reader = IOUtil.newBufferedReader(json);

        try {
            return fromJSON(targetType, reader, config);
        } finally {
            // IOUtil.closeQuietly(reader);
        }
    }

    /**
     *
     * @param <T>
     * @param targetType
     * @param json
     * @return
     */
    public static <T> T fromJSON(TypeReference<T> targetType, Reader json) {
        return fromJSON(targetType, json, null);
    }

    /**
     *
     * @param <T>
     * @param targetType
     * @param json
     * @param config
     * @return
     */
    public static <T> T fromJSON(TypeReference<T> targetType, Reader json, DeserializeConfig config) {
        return fromJSON(targetType, IOUtil.readString(json), config);
    }

    /**
     * The Class SerializeConfig.
     */
    public static class SerializeConfig {

        /** The Constant globalInstance. */
        static final com.alibaba.fastjson.serializer.SerializeConfig globalInstance = com.alibaba.fastjson.serializer.SerializeConfig.getGlobalInstance();

        /** The seri config. */
        private com.alibaba.fastjson.serializer.SerializeConfig seriConfig = null;

        /** The filters. */
        private SerializeFilter[] filters = Fj.emptyFilters;

        /** The date format. */
        private String dateFormat = Fj.DEFFAULT_DATE_FORMAT;

        /** The default features. */
        private int defaultFeatures = Fj.DEFAULT_SERIALIZE_FEATURE;

        /** The features. */
        private SerializerFeature[] features = Fj.emptySerializerFeatures;

        /**
         * Instantiates a new serialize config.
         */
        SerializeConfig() {

        }

        /**
         * Instantiates a new serialize config.
         *
         * @param seriConfig
         */
        SerializeConfig(com.alibaba.fastjson.serializer.SerializeConfig seriConfig) {
            this.seriConfig = seriConfig;
        }

        /**
         * Checks if is asm enable.
         *
         * @return true, if is asm enable
         */
        public boolean isAsmEnable() {
            return seriConfig == null ? globalInstance.isAsmEnable() : seriConfig.isAsmEnable();
        }

        /**
         * Sets the asm enable.
         *
         * @param asmEnable
         * @return
         */
        public SerializeConfig setAsmEnable(boolean asmEnable) {
            if (seriConfig == null) {
                seriConfig = new com.alibaba.fastjson.serializer.SerializeConfig();
            }

            seriConfig.setAsmEnable(asmEnable);

            return this;
        }

        /**
         * Gets the serialize config.
         *
         * @return
         */
        public com.alibaba.fastjson.serializer.SerializeConfig getSerializeConfig() {
            if (seriConfig == null) {
                seriConfig = new com.alibaba.fastjson.serializer.SerializeConfig();
            }

            return seriConfig;
        }

        /**
         * Sets the serialize config.
         *
         * @param seriConfig
         * @return
         */
        public SerializeConfig setSerializeConfig(com.alibaba.fastjson.serializer.SerializeConfig seriConfig) {
            this.seriConfig = seriConfig;

            return this;
        }

        /**
         * Gets the filters.
         *
         * @return
         */
        public SerializeFilter[] getFilters() {
            return this.filters;
        }

        /**
         * Sets the filters.
         *
         * @param filters
         * @return
         */
        @SafeVarargs
        public final SerializeConfig setFilters(SerializeFilter... filters) {
            this.filters = filters;

            return this;
        }

        /**
         * Gets the features.
         *
         * @return
         */
        public SerializerFeature[] getFeatures() {
            return this.features;
        }

        /**
         * Sets the features.
         *
         * @param features
         * @return
         */
        @SafeVarargs
        public final SerializeConfig setFeatures(SerializerFeature... features) {
            this.features = features;

            return this;
        }

        /**
         * Gets the default features.
         *
         * @return
         */
        public int getDefaultFeatures() {
            return this.defaultFeatures;
        }

        /**
         * Sets the default features.
         *
         * @param defaultFeatures
         * @return
         */
        public SerializeConfig setDefaultFeatures(int defaultFeatures) {
            this.defaultFeatures = defaultFeatures;

            return this;
        }

        /**
         * Gets the dateformat.
         *
         * @return
         */
        public String getDateformat() {
            return this.dateFormat;
        }

        /**
         * Sets the dateformat.
         *
         * @param dateFormat
         * @return
         */
        public SerializeConfig setDateformat(String dateFormat) {
            this.dateFormat = dateFormat;

            return this;
        }

        /**
         * The Class FSC.
         */
        public static final class FSC {

            /**
             *
             * @return
             */
            public static SerializeConfig create() {
                return new SerializeConfig();
            }

            /**
             *
             * @param seriConfig
             * @return
             */
            public static SerializeConfig create(com.alibaba.fastjson.serializer.SerializeConfig seriConfig) {
                return new SerializeConfig(seriConfig);
            }

            /**
             *
             * @param filters
             * @return
             */
            @SafeVarargs
            public static SerializeConfig of(SerializeFilter... filters) {
                return new SerializeConfig().setFilters(filters);
            }

            /**
             *
             * @param features
             * @return
             */
            @SafeVarargs
            public static SerializeConfig of(SerializerFeature... features) {
                return new SerializeConfig().setFeatures(features);
            }
        }
    }

    /**
     * The Class DeserializeConfig.
     */
    public static class DeserializeConfig {

        /** The Constant globalInstance. */
        static final ParserConfig globalInstance = ParserConfig.getGlobalInstance();

        /** The parser config. */
        private ParserConfig parserConfig = null;

        /** The processor. */
        private ParseProcess processor;

        /** The feature values. */
        private int featureValues = Fj.DEFAULT_DESERIALIZE_FEATURE;

        /** The features. */
        private Feature[] features = Fj.emptyFeatures;

        /**
         * Instantiates a new deserialize config.
         */
        DeserializeConfig() {

        }

        /**
         * Instantiates a new deserialize config.
         *
         * @param parserConfig
         */
        DeserializeConfig(ParserConfig parserConfig) {
            this.parserConfig = parserConfig;
        }

        /**
         * Creates the java bean deserializer.
         *
         * @param clazz
         * @param type
         * @return
         */
        public ObjectDeserializer createJavaBeanDeserializer(Class<?> clazz, Type type) {
            return parserConfig == null ? globalInstance.createJavaBeanDeserializer(clazz, type) : parserConfig.createJavaBeanDeserializer(clazz, type);
        }

        /**
         * Creates the field deserializer.
         *
         * @param mapping
         * @param beanInfo
         * @param fieldInfo
         * @return
         */
        public FieldDeserializer createFieldDeserializer(ParserConfig mapping, //
                JavaBeanInfo beanInfo, //
                FieldInfo fieldInfo) {
            return parserConfig == null ? globalInstance.createFieldDeserializer(mapping, beanInfo, fieldInfo)
                    : parserConfig.createFieldDeserializer(mapping, beanInfo, fieldInfo);
        }

        /**
         * Checks if is asm enable.
         *
         * @return true, if is asm enable
         */
        public boolean isAsmEnable() {
            return parserConfig == null ? globalInstance.isAsmEnable() : parserConfig.isAsmEnable();
        }

        /**
         * Sets the asm enable.
         *
         * @param asmEnable
         * @return
         */
        public DeserializeConfig setAsmEnable(boolean asmEnable) {
            if (parserConfig == null) {
                parserConfig = new ParserConfig();
            }

            parserConfig.setAsmEnable(asmEnable);

            return this;
        }

        /**
         * Gets the parser config.
         *
         * @return
         */
        public ParserConfig getParserConfig() {
            if (parserConfig == null) {
                parserConfig = new ParserConfig();
            }

            return parserConfig;
        }

        /**
         * Sets the parser config.
         *
         * @param parserConfig
         * @return
         */
        public DeserializeConfig setParserConfig(ParserConfig parserConfig) {
            this.parserConfig = parserConfig;

            return this;
        }

        /**
         * Gets the parses the process.
         *
         * @return
         */
        public ParseProcess getParseProcess() {
            return processor;
        }

        /**
         * Sets the parse process.
         *
         * @param processor
         * @return
         */
        public DeserializeConfig setParseProcess(ParseProcess processor) {
            this.processor = processor;

            return this;
        }

        /**
         * Gets the features.
         *
         * @return
         */
        public Feature[] getFeatures() {
            return this.features;
        }

        /**
         * Sets the features.
         *
         * @param features
         * @return
         */
        @SafeVarargs
        public final DeserializeConfig setFeatures(Feature... features) {
            this.features = features;

            return this;
        }

        /**
         * Gets the feature values.
         *
         * @return
         */
        public int getFeatureValues() {
            return this.featureValues;
        }

        /**
         * Sets the feature values.
         *
         * @param featureValues
         * @return
         */
        public DeserializeConfig setFeatureValues(int featureValues) {
            this.featureValues = featureValues;

            return this;
        }

        /**
         * The Class FDC.
         */
        public static final class FDC extends DeserializeConfig {

            /**
             *
             * @return
             */
            public static DeserializeConfig create() {
                return new DeserializeConfig();
            }

            /**
             *
             * @param parserConfig
             * @return
             */
            public static DeserializeConfig create(ParserConfig parserConfig) {
                return new DeserializeConfig(parserConfig);
            }

            /**
             *
             * @param features
             * @return
             */
            @SafeVarargs
            public static DeserializeConfig of(Feature... features) {
                return new DeserializeConfig().setFeatures(features);
            }
        }
    }
}
