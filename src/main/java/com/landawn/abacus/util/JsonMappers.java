/*
 * Copyright (C) 2024 HaiYang Li
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
package com.landawn.abacus.util;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;

/**
 * Utility class based on Jackson {@code JsonMapper}.
 *
 * @see JsonMapper
 * @see TypeReference
 */
public final class JsonMappers {
    private static final int POOL_SIZE = 128;
    private static final List<JsonMapper> mapperPool = new ArrayList<>(POOL_SIZE);

    private static final JsonMapper defaultJsonMapper = new JsonMapper();
    private static final JsonMapper defaultJsonMapperForPretty = (JsonMapper) new JsonMapper().enable(SerializationFeature.INDENT_OUTPUT);
    private static final SerializationConfig defaultSerializationConfig = defaultJsonMapper.getSerializationConfig();
    private static final DeserializationConfig defaultDeserializationConfig = defaultJsonMapper.getDeserializationConfig();

    private static final SerializationConfig defaultSerializationConfigForCopy;
    private static final SerializationFeature serializationFeatureNotEnabledByDefault;
    private static final DeserializationConfig defaultDeserializationConfigForCopy;
    private static final DeserializationFeature deserializationFeatureNotEnabledByDefault;

    static {
        {
            SerializationFeature tmp = null;
            for (SerializationFeature serializationFeature : SerializationFeature.values()) {
                if (defaultSerializationConfig.isEnabled(serializationFeature) == false) {
                    tmp = serializationFeature;
                    break;
                }
            }

            serializationFeatureNotEnabledByDefault = tmp;
            defaultSerializationConfigForCopy = defaultSerializationConfig.with(serializationFeatureNotEnabledByDefault);
        }

        {
            DeserializationFeature tmp = null;
            for (DeserializationFeature deserializationFeature : DeserializationFeature.values()) {
                if (defaultDeserializationConfig.isEnabled(deserializationFeature) == false) {
                    tmp = deserializationFeature;
                    break;
                }
            }

            deserializationFeatureNotEnabledByDefault = tmp;
            defaultDeserializationConfigForCopy = defaultDeserializationConfig.with(deserializationFeatureNotEnabledByDefault);
        }
    }

    private JsonMappers() {
        // singleton for utility class.
    }

    /**
     *
     * @param obj
     * @return
     */
    public static String toJson(final Object obj) {
        try {
            return defaultJsonMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw N.toRuntimeException(e);
        }
    }

    /**
     *
     * @param obj
     * @param prettyFormat
     * @return
     */
    public static String toJson(final Object obj, final boolean prettyFormat) {
        try {
            if (prettyFormat) {
                return defaultJsonMapperForPretty.writeValueAsString(obj);
            } else {
                return defaultJsonMapper.writeValueAsString(obj);
            }
        } catch (JsonProcessingException e) {
            throw N.toRuntimeException(e);
        }
    }

    /**
     *
     * @param obj
     * @param first
     * @param features
     * @return
     */
    public static String toJson(final Object obj, final SerializationFeature first, final SerializationFeature... features) {
        return toJson(obj, defaultSerializationConfig.with(first, features));
    }

    /**
     *
     * @param obj
     * @param config
     * @return
     */
    public static String toJson(final Object obj, final SerializationConfig config) {
        final JsonMapper jsonMapper = getJsonMapper(config);

        try {
            return jsonMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw N.toRuntimeException(e);
        } finally {
            recycle(jsonMapper);
        }
    }

    /**
     *
     * @param obj
     * @param output
     */
    public static void toJson(final Object obj, final File output) {
        try {
            defaultJsonMapper.writeValue(output, obj);
        } catch (IOException e) {
            throw N.toRuntimeException(e);
        }
    }

    /**
     *
     * @param obj
     * @param output
     * @param config
     */
    public static void toJson(final Object obj, final File output, final SerializationConfig config) {
        final JsonMapper jsonMapper = getJsonMapper(config);

        try {
            jsonMapper.writeValue(output, obj);
        } catch (IOException e) {
            throw N.toRuntimeException(e);
        } finally {
            recycle(jsonMapper);
        }
    }

    /**
     *
     * @param obj
     * @param output
     */
    public static void toJson(final Object obj, final OutputStream output) {
        try {
            defaultJsonMapper.writeValue(output, obj);
        } catch (IOException e) {
            throw N.toRuntimeException(e);
        }
    }

    /**
     *
     * @param obj
     * @param output
     * @param config
     */
    public static void toJson(final Object obj, final OutputStream output, final SerializationConfig config) {
        final JsonMapper jsonMapper = getJsonMapper(config);

        try {
            jsonMapper.writeValue(output, obj);
        } catch (IOException e) {
            throw N.toRuntimeException(e);
        } finally {
            recycle(jsonMapper);
        }
    }

    /**
     *
     * @param obj
     * @param output
     */
    public static void toJson(final Object obj, final Writer output) {
        try {
            defaultJsonMapper.writeValue(output, obj);
        } catch (IOException e) {
            throw N.toRuntimeException(e);
        }
    }

    /**
     *
     * @param obj
     * @param output
     * @param config
     */
    public static void toJson(final Object obj, final Writer output, final SerializationConfig config) {
        final JsonMapper jsonMapper = getJsonMapper(config);

        try {
            jsonMapper.writeValue(output, obj);
        } catch (IOException e) {
            throw N.toRuntimeException(e);
        } finally {
            recycle(jsonMapper);
        }
    }

    /**
     *
     * @param obj
     * @param output
     */
    public static void toJson(final Object obj, final DataOutput output) {
        try {
            defaultJsonMapper.writeValue(output, obj);
        } catch (IOException e) {
            throw N.toRuntimeException(e);
        }
    }

    /**
     *
     * @param obj
     * @param output
     * @param config
     */
    public static void toJson(final Object obj, final DataOutput output, final SerializationConfig config) {
        final JsonMapper jsonMapper = getJsonMapper(config);

        try {
            jsonMapper.writeValue(output, obj);
        } catch (IOException e) {
            throw N.toRuntimeException(e);
        } finally {
            recycle(jsonMapper);
        }
    }

    /**
     *
     *
     * @param <T>
     * @param json
     * @param targetType
     * @return
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromJson(final byte[] json, final Class<? extends T> targetType) {
        try {
            return defaultJsonMapper.readValue(json, targetType);
        } catch (IOException e) {
            throw N.toRuntimeException(e);
        }
    }

    /**
     *
     *
     * @param <T>
     * @param json
     * @param offset
     * @param len
     * @param targetType
     * @return
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromJson(final byte[] json, int offset, int len, final Class<? extends T> targetType) {
        try {
            return defaultJsonMapper.readValue(json, offset, len, targetType);
        } catch (IOException e) {
            throw N.toRuntimeException(e);
        }
    }

    /**
     *
     *
     * @param <T>
     * @param json
     * @param targetType
     * @return
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromJson(final String json, final Class<? extends T> targetType) {
        try {
            return defaultJsonMapper.readValue(json, targetType);
        } catch (JsonProcessingException e) {
            throw N.toRuntimeException(e);
        }
    }

    /**
     *
     *
     * @param <T>
     * @param json
     * @param targetType
     * @param defaultIfNull
     * @return
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromJson(final String json, final Class<? extends T> targetType, final T defaultIfNull) {
        if (N.isEmpty(json)) {
            return defaultIfNull;
        }

        try {
            return N.defaultIfNull(defaultJsonMapper.readValue(json, targetType), defaultIfNull);
        } catch (JsonProcessingException e) {
            throw N.toRuntimeException(e);
        }
    }

    /**
     *
     *
     * @param <T>
     * @param json
     * @param targetType
     * @param first
     * @param features
     * @return
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromJson(final String json, final Class<? extends T> targetType, final DeserializationFeature first,
            final DeserializationFeature... features) {
        return fromJson(json, targetType, defaultDeserializationConfig.with(first, features));
    }

    /**
     *
     *
     * @param <T>
     * @param json
     * @param targetType
     * @param config
     * @return
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromJson(final String json, final Class<? extends T> targetType, final DeserializationConfig config) {
        final JsonMapper jsonMapper = getJsonMapper(config);

        try {
            return jsonMapper.readValue(json, targetType);
        } catch (IOException e) {
            throw N.toRuntimeException(e);
        } finally {
            recycle(jsonMapper);
        }
    }

    /**
     *
     *
     * @param <T>
     * @param json
     * @param targetType
     * @return
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromJson(final File json, final Class<? extends T> targetType) {
        try {
            return defaultJsonMapper.readValue(json, targetType);
        } catch (IOException e) {
            throw N.toRuntimeException(e);
        }
    }

    /**
     *
     *
     * @param <T>
     * @param json
     * @param targetType
     * @param config
     * @return
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromJson(final File json, final Class<? extends T> targetType, final DeserializationConfig config) {
        final JsonMapper jsonMapper = getJsonMapper(config);

        try {
            return jsonMapper.readValue(json, targetType);
        } catch (IOException e) {
            throw N.toRuntimeException(e);
        } finally {
            recycle(jsonMapper);
        }
    }

    /**
     *
     *
     * @param <T>
     * @param json
     * @param targetType
     * @return
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromJson(final InputStream json, final Class<? extends T> targetType) {
        try {
            return defaultJsonMapper.readValue(json, targetType);
        } catch (IOException e) {
            throw N.toRuntimeException(e);
        }
    }

    /**
     *
     *
     * @param <T>
     * @param json
     * @param targetType
     * @param config
     * @return
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromJson(final InputStream json, final Class<? extends T> targetType, final DeserializationConfig config) {
        final JsonMapper jsonMapper = getJsonMapper(config);

        try {
            return jsonMapper.readValue(json, targetType);
        } catch (IOException e) {
            throw N.toRuntimeException(e);
        } finally {
            recycle(jsonMapper);
        }
    }

    /**
     *
     *
     * @param <T>
     * @param json
     * @param targetType
     * @return
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromJson(final Reader json, final Class<? extends T> targetType) {
        try {
            return defaultJsonMapper.readValue(json, targetType);
        } catch (IOException e) {
            throw N.toRuntimeException(e);
        }
    }

    /**
     *
     *
     * @param <T>
     * @param json
     * @param targetType
     * @param config
     * @return
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromJson(final Reader json, final Class<? extends T> targetType, final DeserializationConfig config) {
        final JsonMapper jsonMapper = getJsonMapper(config);

        try {
            return jsonMapper.readValue(json, targetType);
        } catch (IOException e) {
            throw N.toRuntimeException(e);
        } finally {
            recycle(jsonMapper);
        }
    }

    /**
     *
     *
     * @param <T>
     * @param json
     * @param targetType
     * @return
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromJson(final URL json, final Class<? extends T> targetType) {
        try {
            return defaultJsonMapper.readValue(json, targetType);
        } catch (IOException e) {
            throw N.toRuntimeException(e);
        }
    }

    /**
     *
     *
     * @param <T>
     * @param json
     * @param targetType
     * @param config
     * @return
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromJson(final URL json, final Class<? extends T> targetType, final DeserializationConfig config) {
        final JsonMapper jsonMapper = getJsonMapper(config);

        try {
            return jsonMapper.readValue(json, targetType);
        } catch (IOException e) {
            throw N.toRuntimeException(e);
        } finally {
            recycle(jsonMapper);
        }
    }

    /**
     *
     *
     * @param <T>
     * @param json
     * @param targetType
     * @return
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromJson(final DataInput json, final Class<? extends T> targetType) {
        try {
            return defaultJsonMapper.readValue(json, targetType);
        } catch (IOException e) {
            throw N.toRuntimeException(e);
        }
    }

    /**
     *
     *
     * @param <T>
     * @param json
     * @param targetType
     * @param config
     * @return
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromJson(final DataInput json, final Class<? extends T> targetType, final DeserializationConfig config) {
        final JsonMapper jsonMapper = getJsonMapper(config);

        try {
            return jsonMapper.readValue(json, targetType);
        } catch (IOException e) {
            throw N.toRuntimeException(e);
        } finally {
            recycle(jsonMapper);
        }
    }

    /**
     *
     *
     * @param <T>
     * @param json
     * @param targetType
     * @return
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromJson(final byte[] json, final TypeReference<? extends T> targetType) {
        try {
            return defaultJsonMapper.readValue(json, targetType);
        } catch (IOException e) {
            throw N.toRuntimeException(e);
        }
    }

    /**
     *
     *
     * @param <T>
     * @param json
     * @param offset
     * @param len
     * @param targetType
     * @return
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromJson(final byte[] json, int offset, int len, final TypeReference<? extends T> targetType) {
        try {
            return defaultJsonMapper.readValue(json, offset, len, targetType);
        } catch (IOException e) {
            throw N.toRuntimeException(e);
        }
    }

    /**
     *
     *
     * @param <T>
     * @param json
     * @param targetType can be the {@code Type} of {@code Bean/Array/Collection/Map}.
     * @return
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromJson(final String json, final TypeReference<? extends T> targetType) {
        try {
            return defaultJsonMapper.readValue(json, targetType);
        } catch (IOException e) {
            throw N.toRuntimeException(e);
        }
    }

    /**
     *
     *
     * @param <T>
     * @param json
     * @param targetType
     * @param defaultIfNull
     * @return
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromJson(final String json, final TypeReference<? extends T> targetType, final T defaultIfNull) {
        if (N.isEmpty(json)) {
            return defaultIfNull;
        }

        try {
            return N.defaultIfNull(defaultJsonMapper.readValue(json, targetType), defaultIfNull);
        } catch (JsonProcessingException e) {
            throw N.toRuntimeException(e);
        }
    }

    /**
     *
     *
     * @param <T>
     * @param json
     * @param targetType
     * @param first
     * @param features
     * @return
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromJson(final String json, final TypeReference<? extends T> targetType, final DeserializationFeature first,
            final DeserializationFeature... features) {
        return fromJson(json, targetType, defaultDeserializationConfig.with(first, features));
    }

    /**
     *
     *
     * @param <T>
     * @param json
     * @param targetType can be the {@code Type} of {@code Bean/Array/Collection/Map}.
     * @param config
     * @return
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromJson(final String json, final TypeReference<? extends T> targetType, final DeserializationConfig config) {
        final JsonMapper jsonMapper = getJsonMapper(config);

        try {
            return jsonMapper.readValue(json, targetType);
        } catch (IOException e) {
            throw N.toRuntimeException(e);
        } finally {
            recycle(jsonMapper);
        }
    }

    /**
     *
     *
     * @param <T>
     * @param json
     * @param targetType can be the {@code Type} of {@code Bean/Array/Collection/Map}.
     * @return
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromJson(final File json, final TypeReference<? extends T> targetType) {
        try {
            return defaultJsonMapper.readValue(json, targetType);
        } catch (IOException e) {
            throw N.toRuntimeException(e);
        }
    }

    /**
     *
     *
     * @param <T>
     * @param json
     * @param targetType can be the {@code Type} of {@code Bean/Array/Collection/Map}.
     * @param config
     * @return
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromJson(final File json, final TypeReference<? extends T> targetType, final DeserializationConfig config) {
        final JsonMapper jsonMapper = getJsonMapper(config);

        try {
            return jsonMapper.readValue(json, targetType);
        } catch (IOException e) {
            throw N.toRuntimeException(e);
        } finally {
            recycle(jsonMapper);
        }
    }

    /**
     *
     *
     * @param <T>
     * @param json
     * @param targetType can be the {@code Type} of {@code Bean/Array/Collection/Map}.
     * @return
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromJson(final InputStream json, final TypeReference<? extends T> targetType) {
        try {
            return defaultJsonMapper.readValue(json, targetType);
        } catch (IOException e) {
            throw N.toRuntimeException(e);
        }
    }

    /**
     *
     *
     * @param <T>
     * @param json
     * @param targetType can be the {@code Type} of {@code Bean/Array/Collection/Map}.
     * @param config
     * @return
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromJson(final InputStream json, final TypeReference<? extends T> targetType, final DeserializationConfig config) {
        final JsonMapper jsonMapper = getJsonMapper(config);

        try {
            return jsonMapper.readValue(json, targetType);
        } catch (IOException e) {
            throw N.toRuntimeException(e);
        } finally {
            recycle(jsonMapper);
        }
    }

    /**
     *
     *
     * @param <T>
     * @param json
     * @param targetType can be the {@code Type} of {@code Bean/Array/Collection/Map}.
     * @return
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromJson(final Reader json, final TypeReference<? extends T> targetType) {
        try {
            return defaultJsonMapper.readValue(json, targetType);
        } catch (IOException e) {
            throw N.toRuntimeException(e);
        }
    }

    /**
     *
     *
     * @param <T>
     * @param json
     * @param targetType can be the {@code Type} of {@code Bean/Array/Collection/Map}.
     * @param config
     * @return
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromJson(final Reader json, final TypeReference<? extends T> targetType, final DeserializationConfig config) {
        final JsonMapper jsonMapper = getJsonMapper(config);

        try {
            return jsonMapper.readValue(json, targetType);
        } catch (IOException e) {
            throw N.toRuntimeException(e);
        } finally {
            recycle(jsonMapper);
        }
    }

    /**
     *
     *
     * @param <T>
     * @param json
     * @param targetType
     * @return
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromJson(final URL json, final TypeReference<? extends T> targetType) {
        try {
            return defaultJsonMapper.readValue(json, targetType);
        } catch (IOException e) {
            throw N.toRuntimeException(e);
        }
    }

    /**
     *
     *
     * @param <T>
     * @param json
     * @param targetType
     * @param config
     * @return
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromJson(final URL json, final TypeReference<? extends T> targetType, final DeserializationConfig config) {
        final JsonMapper jsonMapper = getJsonMapper(config);

        try {
            return jsonMapper.readValue(json, targetType);
        } catch (IOException e) {
            throw N.toRuntimeException(e);
        } finally {
            recycle(jsonMapper);
        }
    }

    /**
     *
     *
     * @param <T>
     * @param json
     * @param targetType
     * @return
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromJson(final DataInput json, final TypeReference<? extends T> targetType) {
        try {
            return defaultJsonMapper.readValue(json, defaultJsonMapper.constructType(targetType));
        } catch (IOException e) {
            throw N.toRuntimeException(e);
        }
    }

    /**
     *
     *
     * @param <T>
     * @param json
     * @param targetType
     * @param config
     * @return
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromJson(final DataInput json, final TypeReference<? extends T> targetType, final DeserializationConfig config) {
        final JsonMapper jsonMapper = getJsonMapper(config);

        try {
            return jsonMapper.readValue(json, jsonMapper.constructType(targetType));
        } catch (IOException e) {
            throw N.toRuntimeException(e);
        } finally {
            recycle(jsonMapper);
        }
    }

    /**
     *
     * @return
     */
    public static SerializationConfig createSerializationConfig() {
        // final SerializationConfig copy = defaultSerializationConfigForCopy.without(serializationFeatureNotEnabledByDefault);

        return defaultSerializationConfigForCopy.without(serializationFeatureNotEnabledByDefault);
    }

    /**
     *
     * @return
     */
    public static DeserializationConfig createDeserializationConfig() {
        // final DeserializationConfig copy = defaultDeserializationConfigForCopy.without(deserializationFeatureNotEnabledByDefault);

        return defaultDeserializationConfigForCopy.without(deserializationFeatureNotEnabledByDefault);
    }

    //    /**
    //     *
    //     * @param setter first parameter is the copy of default {@code SerializationConfig}
    //     * @return
    //     */
    //    public static SerializationConfig createSerializationConfig(final Function<? super SerializationConfig, ? extends SerializationConfig> setter) {
    //        return setter.apply(createSerializationConfig());
    //    }
    //
    //    /**
    //     *
    //     * @param setter first parameter is the copy of default {@code DeserializationConfig}
    //     * @return
    //     */
    //    public static DeserializationConfig createDeserializationConfig(final Function<? super DeserializationConfig, ? extends DeserializationConfig> setter) {
    //        return setter.apply(createDeserializationConfig());
    //    }

    static JsonMapper getJsonMapper(final SerializationConfig config) {
        if (config == null) {
            return defaultJsonMapper;
        }

        JsonMapper mapper = null;

        synchronized (mapperPool) {
            if (mapperPool.size() > 0) {
                mapper = mapperPool.remove(mapperPool.size() - 1);
            }
        }

        if (mapper == null) {
            mapper = new JsonMapper();
        }

        mapper.setConfig(config);

        return mapper;
    }

    static JsonMapper getJsonMapper(final DeserializationConfig config) {
        if (config == null) {
            return defaultJsonMapper;
        }

        JsonMapper mapper = null;

        synchronized (mapperPool) {
            if (mapperPool.size() > 0) {
                mapper = mapperPool.remove(mapperPool.size() - 1);
            }
        }

        if (mapper == null) {
            mapper = new JsonMapper();
        }

        mapper.setConfig(config);

        return mapper;
    }

    static void recycle(final JsonMapper mapper) {
        if (mapper == null) {
            return;
        }

        mapper.setConfig(defaultSerializationConfig);
        mapper.setConfig(defaultDeserializationConfig);

        synchronized (mapperPool) {
            if (mapperPool.size() < POOL_SIZE) {

                mapperPool.add(mapper);
            }
        }
    }

    /**
     *
     *
     * @param jsonMapper
     * @return
     */
    public static One wrap(final ObjectMapper jsonMapper) {
        return new One(jsonMapper);
    }

    public static final class One {

        private final ObjectMapper jsonMapper;
        private final ObjectMapper jsonMapperForPretty;

        One(final ObjectMapper jsonMapper) {
            this.jsonMapper = jsonMapper;
            this.jsonMapperForPretty = jsonMapper.copy();

            jsonMapperForPretty.enable(SerializationFeature.INDENT_OUTPUT);
        }

        /**
         *
         * @param obj
         * @return
         */
        public String toJson(final Object obj) {
            try {
                return jsonMapper.writeValueAsString(obj);
            } catch (JsonProcessingException e) {
                throw N.toRuntimeException(e);
            }
        }

        /**
         *
         * @param obj
         * @param prettyFormat
         * @return
         */
        public String toJson(final Object obj, final boolean prettyFormat) {
            try {
                if (prettyFormat) {
                    return jsonMapperForPretty.writeValueAsString(obj);
                } else {
                    return jsonMapper.writeValueAsString(obj);
                }
            } catch (JsonProcessingException e) {
                throw N.toRuntimeException(e);
            }
        }

        /**
         *
         * @param obj
         * @param output
         */
        public void toJson(final Object obj, final File output) {
            try {
                jsonMapper.writeValue(output, obj);
            } catch (IOException e) {
                throw N.toRuntimeException(e);
            }
        }

        /**
         *
         * @param obj
         * @param output
         */
        public void toJson(final Object obj, final OutputStream output) {
            try {
                jsonMapper.writeValue(output, obj);
            } catch (IOException e) {
                throw N.toRuntimeException(e);
            }
        }

        /**
         *
         * @param obj
         * @param output
         */
        public void toJson(final Object obj, final Writer output) {
            try {
                jsonMapper.writeValue(output, obj);
            } catch (IOException e) {
                throw N.toRuntimeException(e);
            }
        }

        /**
         *
         * @param obj
         * @param output
         */
        public void toJson(final Object obj, final DataOutput output) {
            try {
                jsonMapper.writeValue(output, obj);
            } catch (IOException e) {
                throw N.toRuntimeException(e);
            }
        }

        /**
         *
         *
         * @param <T>
         * @param json
         * @param targetType
         * @return
         * @see com.fasterxml.jackson.core.type.TypeReference
         */
        public <T> T fromJson(final byte[] json, final Class<? extends T> targetType) {
            try {
                return jsonMapper.readValue(json, targetType);
            } catch (IOException e) {
                throw N.toRuntimeException(e);
            }
        }

        /**
         *
         *
         * @param <T>
         * @param json
         * @param offset
         * @param len
         * @param targetType
         * @return
         * @see com.fasterxml.jackson.core.type.TypeReference
         */
        public <T> T fromJson(final byte[] json, int offset, int len, final Class<? extends T> targetType) {
            try {
                return jsonMapper.readValue(json, offset, len, targetType);
            } catch (IOException e) {
                throw N.toRuntimeException(e);
            }
        }

        /**
         *
         *
         * @param <T>
         * @param json
         * @param targetType
         * @return
         * @see com.fasterxml.jackson.core.type.TypeReference
         */
        public <T> T fromJson(final String json, final Class<? extends T> targetType) {
            try {
                return jsonMapper.readValue(json, targetType);
            } catch (JsonProcessingException e) {
                throw N.toRuntimeException(e);
            }
        }

        /**
         *
         *
         * @param <T>
         * @param json
         * @param targetType
         * @param defaultIfNull
         * @return
         * @see com.fasterxml.jackson.core.type.TypeReference
         */
        public <T> T fromJson(final String json, final Class<? extends T> targetType, final T defaultIfNull) {
            if (N.isEmpty(json)) {
                return defaultIfNull;
            }

            try {
                return N.defaultIfNull(jsonMapper.readValue(json, targetType), defaultIfNull);
            } catch (JsonProcessingException e) {
                throw N.toRuntimeException(e);
            }
        }

        /**
         *
         *
         * @param <T>
         * @param json
         * @param targetType
         * @return
         * @see com.fasterxml.jackson.core.type.TypeReference
         */
        public <T> T fromJson(final File json, final Class<? extends T> targetType) {
            try {
                return jsonMapper.readValue(json, targetType);
            } catch (IOException e) {
                throw N.toRuntimeException(e);
            }
        }

        /**
         *
         *
         * @param <T>
         * @param json
         * @param targetType
         * @return
         * @see com.fasterxml.jackson.core.type.TypeReference
         */
        public <T> T fromJson(final InputStream json, final Class<? extends T> targetType) {
            try {
                return jsonMapper.readValue(json, targetType);
            } catch (IOException e) {
                throw N.toRuntimeException(e);
            }
        }

        /**
         *
         *
         * @param <T>
         * @param json
         * @param targetType
         * @return
         * @see com.fasterxml.jackson.core.type.TypeReference
         */
        public <T> T fromJson(final Reader json, final Class<? extends T> targetType) {
            try {
                return jsonMapper.readValue(json, targetType);
            } catch (IOException e) {
                throw N.toRuntimeException(e);
            }
        }

        /**
         *
         *
         * @param <T>
         * @param json
         * @param targetType
         * @return
         * @see com.fasterxml.jackson.core.type.TypeReference
         */
        public <T> T fromJson(final URL json, final Class<? extends T> targetType) {
            try {
                return jsonMapper.readValue(json, targetType);
            } catch (IOException e) {
                throw N.toRuntimeException(e);
            }
        }

        /**
         *
         *
         * @param <T>
         * @param json
         * @param targetType
         * @return
         * @see com.fasterxml.jackson.core.type.TypeReference
         */
        public <T> T fromJson(final DataInput json, final Class<? extends T> targetType) {
            try {
                return jsonMapper.readValue(json, targetType);
            } catch (IOException e) {
                throw N.toRuntimeException(e);
            }
        }

        /**
         *
         *
         * @param <T>
         * @param json
         * @param targetType
         * @return
         * @see com.fasterxml.jackson.core.type.TypeReference
         */
        public <T> T fromJson(final byte[] json, final TypeReference<? extends T> targetType) {
            try {
                return jsonMapper.readValue(json, targetType);
            } catch (IOException e) {
                throw N.toRuntimeException(e);
            }
        }

        /**
         *
         *
         * @param <T>
         * @param json
         * @param offset
         * @param len
         * @param targetType
         * @return
         * @see com.fasterxml.jackson.core.type.TypeReference
         */
        public <T> T fromJson(final byte[] json, int offset, int len, final TypeReference<? extends T> targetType) {
            try {
                return jsonMapper.readValue(json, offset, len, targetType);
            } catch (IOException e) {
                throw N.toRuntimeException(e);
            }
        }

        /**
         *
         *
         * @param <T>
         * @param json
         * @param targetType can be the {@code Type} of {@code Bean/Array/Collection/Map}.
         * @return
         * @see com.fasterxml.jackson.core.type.TypeReference
         */
        public <T> T fromJson(final String json, final TypeReference<? extends T> targetType) {
            try {
                return jsonMapper.readValue(json, targetType);
            } catch (IOException e) {
                throw N.toRuntimeException(e);
            }
        }

        /**
         *
         *
         * @param <T>
         * @param json
         * @param targetType
         * @param defaultIfNull
         * @return
         * @see com.fasterxml.jackson.core.type.TypeReference
         */
        public <T> T fromJson(final String json, final TypeReference<? extends T> targetType, final T defaultIfNull) {
            if (N.isEmpty(json)) {
                return defaultIfNull;
            }

            try {
                return N.defaultIfNull(jsonMapper.readValue(json, targetType), defaultIfNull);
            } catch (JsonProcessingException e) {
                throw N.toRuntimeException(e);
            }
        }

        /**
         *
         *
         * @param <T>
         * @param json
         * @param targetType can be the {@code Type} of {@code Bean/Array/Collection/Map}.
         * @return
         * @see com.fasterxml.jackson.core.type.TypeReference
         */
        public <T> T fromJson(final File json, final TypeReference<? extends T> targetType) {
            try {
                return jsonMapper.readValue(json, targetType);
            } catch (IOException e) {
                throw N.toRuntimeException(e);
            }
        }

        /**
         *
         *
         * @param <T>
         * @param json
         * @param targetType can be the {@code Type} of {@code Bean/Array/Collection/Map}.
         * @return
         * @see com.fasterxml.jackson.core.type.TypeReference
         */
        public <T> T fromJson(final InputStream json, final TypeReference<? extends T> targetType) {
            try {
                return jsonMapper.readValue(json, targetType);
            } catch (IOException e) {
                throw N.toRuntimeException(e);
            }
        }

        /**
         *
         *
         * @param <T>
         * @param json
         * @param targetType can be the {@code Type} of {@code Bean/Array/Collection/Map}.
         * @return
         * @see com.fasterxml.jackson.core.type.TypeReference
         */
        public <T> T fromJson(final Reader json, final TypeReference<? extends T> targetType) {
            try {
                return jsonMapper.readValue(json, targetType);
            } catch (IOException e) {
                throw N.toRuntimeException(e);
            }
        }

        /**
         *
         *
         * @param <T>
         * @param json
         * @param targetType
         * @return
         * @see com.fasterxml.jackson.core.type.TypeReference
         */
        public <T> T fromJson(final URL json, final TypeReference<? extends T> targetType) {
            try {
                return jsonMapper.readValue(json, targetType);
            } catch (IOException e) {
                throw N.toRuntimeException(e);
            }
        }

        /**
         *
         *
         * @param <T>
         * @param json
         * @param targetType
         * @return
         * @see com.fasterxml.jackson.core.type.TypeReference
         */
        public <T> T fromJson(final DataInput json, final TypeReference<? extends T> targetType) {
            try {
                return jsonMapper.readValue(json, jsonMapper.constructType(targetType));
            } catch (IOException e) {
                throw N.toRuntimeException(e);
            }
        }

    }
}
