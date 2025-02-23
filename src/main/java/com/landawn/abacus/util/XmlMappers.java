/*
 * Copyright (C) 2024 HaiYang Li
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
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

/**
 * Utility class based on Jackson {@code XmlMapper}.
 *
 * @see XmlMapper
 * @see TypeReference
 */
public final class XmlMappers {
    private static final int POOL_SIZE = 128;
    private static final List<XmlMapper> mapperPool = new ArrayList<>(POOL_SIZE);

    private static final XmlMapper defaultXmlMapper = new XmlMapper();
    private static final XmlMapper defaultXmlMapperForPretty = (XmlMapper) new XmlMapper().enable(SerializationFeature.INDENT_OUTPUT);
    private static final SerializationConfig defaultSerializationConfig = defaultXmlMapper.getSerializationConfig();
    private static final DeserializationConfig defaultDeserializationConfig = defaultXmlMapper.getDeserializationConfig();

    private static final SerializationConfig defaultSerializationConfigForCopy;
    private static final SerializationFeature serializationFeatureNotEnabledByDefault;
    private static final DeserializationConfig defaultDeserializationConfigForCopy;
    private static final DeserializationFeature deserializationFeatureNotEnabledByDefault;

    static {
        {
            SerializationFeature tmp = null;
            for (final SerializationFeature serializationFeature : SerializationFeature.values()) {
                if (!defaultSerializationConfig.isEnabled(serializationFeature)) {
                    tmp = serializationFeature;
                    break;
                }
            }

            serializationFeatureNotEnabledByDefault = tmp;
            defaultSerializationConfigForCopy = defaultSerializationConfig.with(serializationFeatureNotEnabledByDefault);
        }

        {
            DeserializationFeature tmp = null;
            for (final DeserializationFeature deserializationFeature : DeserializationFeature.values()) {
                if (!defaultDeserializationConfig.isEnabled(deserializationFeature)) {
                    tmp = deserializationFeature;
                    break;
                }
            }

            deserializationFeatureNotEnabledByDefault = tmp;
            defaultDeserializationConfigForCopy = defaultDeserializationConfig.with(deserializationFeatureNotEnabledByDefault);
        }
    }

    private XmlMappers() {
        // singleton for utility class.
    }

    /**
     *
     * @param obj
     * @return
     */
    public static String toXml(final Object obj) {
        try {
            return defaultXmlMapper.writeValueAsString(obj);
        } catch (final JsonProcessingException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     *
     * @param obj
     * @param prettyFormat
     * @return
     */
    public static String toXml(final Object obj, final boolean prettyFormat) {
        try {
            if (prettyFormat) {
                return defaultXmlMapperForPretty.writeValueAsString(obj);
            } else {
                return defaultXmlMapper.writeValueAsString(obj);
            }
        } catch (final JsonProcessingException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     *
     * @param obj
     * @param first
     * @param features
     * @return
     */
    public static String toXml(final Object obj, final SerializationFeature first, final SerializationFeature... features) {
        return toXml(obj, defaultSerializationConfig.with(first, features));
    }

    /**
     *
     * @param obj
     * @param config
     * @return
     */
    public static String toXml(final Object obj, final SerializationConfig config) {
        final XmlMapper xmlMapper = getXmlMapper(config);

        try {
            return xmlMapper.writeValueAsString(obj);
        } catch (final JsonProcessingException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        } finally {
            recycle(xmlMapper);
        }
    }

    /**
     *
     * @param obj
     * @param output
     */
    public static void toXml(final Object obj, final File output) {
        try {
            defaultXmlMapper.writeValue(output, obj);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     *
     * @param obj
     * @param output
     * @param config
     */
    public static void toXml(final Object obj, final File output, final SerializationConfig config) {
        final XmlMapper xmlMapper = getXmlMapper(config);

        try {
            xmlMapper.writeValue(output, obj);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        } finally {
            recycle(xmlMapper);
        }
    }

    /**
     *
     * @param obj
     * @param output
     */
    public static void toXml(final Object obj, final OutputStream output) {
        try {
            defaultXmlMapper.writeValue(output, obj);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     *
     * @param obj
     * @param output
     * @param config
     */
    public static void toXml(final Object obj, final OutputStream output, final SerializationConfig config) {
        final XmlMapper xmlMapper = getXmlMapper(config);

        try {
            xmlMapper.writeValue(output, obj);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        } finally {
            recycle(xmlMapper);
        }
    }

    /**
     *
     * @param obj
     * @param output
     */
    public static void toXml(final Object obj, final Writer output) {
        try {
            defaultXmlMapper.writeValue(output, obj);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     *
     * @param obj
     * @param output
     * @param config
     */
    public static void toXml(final Object obj, final Writer output, final SerializationConfig config) {
        final XmlMapper xmlMapper = getXmlMapper(config);

        try {
            xmlMapper.writeValue(output, obj);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        } finally {
            recycle(xmlMapper);
        }
    }

    /**
     *
     * @param obj
     * @param output
     */
    public static void toXml(final Object obj, final DataOutput output) {
        try {
            defaultXmlMapper.writeValue(output, obj);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     *
     * @param obj
     * @param output
     * @param config
     */
    public static void toXml(final Object obj, final DataOutput output, final SerializationConfig config) {
        final XmlMapper xmlMapper = getXmlMapper(config);

        try {
            xmlMapper.writeValue(output, obj);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        } finally {
            recycle(xmlMapper);
        }
    }

    /**
     *
     * @param <T>
     * @param json
     * @param targetType
     * @return
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromXml(final byte[] json, final Class<? extends T> targetType) {
        try {
            return defaultXmlMapper.readValue(json, targetType);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     *
     * @param <T>
     * @param json
     * @param offset
     * @param len
     * @param targetType
     * @return
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromXml(final byte[] json, final int offset, final int len, final Class<? extends T> targetType) {
        try {
            return defaultXmlMapper.readValue(json, offset, len, targetType);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     *
     * @param <T>
     * @param json
     * @param targetType
     * @return
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromXml(final String json, final Class<? extends T> targetType) {
        try {
            return defaultXmlMapper.readValue(json, targetType);
        } catch (final JsonProcessingException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     *
     * @param <T>
     * @param json
     * @param targetType
     * @param defaultIfNull
     * @return
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromXml(final String json, final Class<? extends T> targetType, final T defaultIfNull) {
        if (N.isEmpty(json)) {
            return defaultIfNull;
        }

        try {
            return N.defaultIfNull(defaultXmlMapper.readValue(json, targetType), defaultIfNull);
        } catch (final JsonProcessingException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     *
     * @param <T>
     * @param json
     * @param targetType
     * @param first
     * @param features
     * @return
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromXml(final String json, final Class<? extends T> targetType, final DeserializationFeature first,
            final DeserializationFeature... features) {
        return fromXml(json, targetType, defaultDeserializationConfig.with(first, features));
    }

    /**
     *
     * @param <T>
     * @param json
     * @param targetType
     * @param config
     * @return
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromXml(final String json, final Class<? extends T> targetType, final DeserializationConfig config) {
        final XmlMapper xmlMapper = getXmlMapper(config);

        try {
            return xmlMapper.readValue(json, targetType);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        } finally {
            recycle(xmlMapper);
        }
    }

    /**
     *
     * @param <T>
     * @param json
     * @param targetType
     * @return
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromXml(final File json, final Class<? extends T> targetType) {
        try {
            return defaultXmlMapper.readValue(json, targetType);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     *
     * @param <T>
     * @param json
     * @param targetType
     * @param config
     * @return
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromXml(final File json, final Class<? extends T> targetType, final DeserializationConfig config) {
        final XmlMapper xmlMapper = getXmlMapper(config);

        try {
            return xmlMapper.readValue(json, targetType);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        } finally {
            recycle(xmlMapper);
        }
    }

    /**
     *
     * @param <T>
     * @param json
     * @param targetType
     * @return
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromXml(final InputStream json, final Class<? extends T> targetType) {
        try {
            return defaultXmlMapper.readValue(json, targetType);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     *
     * @param <T>
     * @param json
     * @param targetType
     * @param config
     * @return
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromXml(final InputStream json, final Class<? extends T> targetType, final DeserializationConfig config) {
        final XmlMapper xmlMapper = getXmlMapper(config);

        try {
            return xmlMapper.readValue(json, targetType);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        } finally {
            recycle(xmlMapper);
        }
    }

    /**
     *
     * @param <T>
     * @param json
     * @param targetType
     * @return
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromXml(final Reader json, final Class<? extends T> targetType) {
        try {
            return defaultXmlMapper.readValue(json, targetType);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     *
     * @param <T>
     * @param json
     * @param targetType
     * @param config
     * @return
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromXml(final Reader json, final Class<? extends T> targetType, final DeserializationConfig config) {
        final XmlMapper xmlMapper = getXmlMapper(config);

        try {
            return xmlMapper.readValue(json, targetType);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        } finally {
            recycle(xmlMapper);
        }
    }

    /**
     *
     * @param <T>
     * @param json
     * @param targetType
     * @return
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromXml(final URL json, final Class<? extends T> targetType) {
        try {
            return defaultXmlMapper.readValue(json, targetType);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     *
     * @param <T>
     * @param json
     * @param targetType
     * @param config
     * @return
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromXml(final URL json, final Class<? extends T> targetType, final DeserializationConfig config) {
        final XmlMapper xmlMapper = getXmlMapper(config);

        try {
            return xmlMapper.readValue(json, targetType);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        } finally {
            recycle(xmlMapper);
        }
    }

    /**
     *
     * @param <T>
     * @param json
     * @param targetType
     * @return
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromXml(final DataInput json, final Class<? extends T> targetType) {
        try {
            return defaultXmlMapper.readValue(json, targetType);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     *
     * @param <T>
     * @param json
     * @param targetType
     * @param config
     * @return
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromXml(final DataInput json, final Class<? extends T> targetType, final DeserializationConfig config) {
        final XmlMapper xmlMapper = getXmlMapper(config);

        try {
            return xmlMapper.readValue(json, targetType);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        } finally {
            recycle(xmlMapper);
        }
    }

    /**
     *
     * @param <T>
     * @param json
     * @param targetType
     * @return
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromXml(final byte[] json, final TypeReference<? extends T> targetType) {
        try {
            return defaultXmlMapper.readValue(json, targetType);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     *
     * @param <T>
     * @param json
     * @param offset
     * @param len
     * @param targetType
     * @return
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromXml(final byte[] json, final int offset, final int len, final TypeReference<? extends T> targetType) {
        try {
            return defaultXmlMapper.readValue(json, offset, len, targetType);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     *
     * @param <T>
     * @param json
     * @param targetType can be the {@code Type} of {@code Bean/Array/Collection/Map}.
     * @return
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromXml(final String json, final TypeReference<? extends T> targetType) {
        try {
            return defaultXmlMapper.readValue(json, targetType);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     *
     * @param <T>
     * @param json
     * @param targetType
     * @param defaultIfNull
     * @return
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromXml(final String json, final TypeReference<? extends T> targetType, final T defaultIfNull) {
        if (N.isEmpty(json)) {
            return defaultIfNull;
        }

        try {
            return N.defaultIfNull(defaultXmlMapper.readValue(json, targetType), defaultIfNull);
        } catch (final JsonProcessingException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     *
     * @param <T>
     * @param json
     * @param targetType
     * @param first
     * @param features
     * @return
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromXml(final String json, final TypeReference<? extends T> targetType, final DeserializationFeature first,
            final DeserializationFeature... features) {
        return fromXml(json, targetType, defaultDeserializationConfig.with(first, features));
    }

    /**
     *
     * @param <T>
     * @param json
     * @param targetType can be the {@code Type} of {@code Bean/Array/Collection/Map}.
     * @param config
     * @return
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromXml(final String json, final TypeReference<? extends T> targetType, final DeserializationConfig config) {
        final XmlMapper xmlMapper = getXmlMapper(config);

        try {
            return xmlMapper.readValue(json, targetType);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        } finally {
            recycle(xmlMapper);
        }
    }

    /**
     *
     * @param <T>
     * @param json
     * @param targetType can be the {@code Type} of {@code Bean/Array/Collection/Map}.
     * @return
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromXml(final File json, final TypeReference<? extends T> targetType) {
        try {
            return defaultXmlMapper.readValue(json, targetType);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     *
     * @param <T>
     * @param json
     * @param targetType can be the {@code Type} of {@code Bean/Array/Collection/Map}.
     * @param config
     * @return
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromXml(final File json, final TypeReference<? extends T> targetType, final DeserializationConfig config) {
        final XmlMapper xmlMapper = getXmlMapper(config);

        try {
            return xmlMapper.readValue(json, targetType);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        } finally {
            recycle(xmlMapper);
        }
    }

    /**
     *
     * @param <T>
     * @param json
     * @param targetType can be the {@code Type} of {@code Bean/Array/Collection/Map}.
     * @return
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromXml(final InputStream json, final TypeReference<? extends T> targetType) {
        try {
            return defaultXmlMapper.readValue(json, targetType);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     *
     * @param <T>
     * @param json
     * @param targetType can be the {@code Type} of {@code Bean/Array/Collection/Map}.
     * @param config
     * @return
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromXml(final InputStream json, final TypeReference<? extends T> targetType, final DeserializationConfig config) {
        final XmlMapper xmlMapper = getXmlMapper(config);

        try {
            return xmlMapper.readValue(json, targetType);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        } finally {
            recycle(xmlMapper);
        }
    }

    /**
     *
     * @param <T>
     * @param json
     * @param targetType can be the {@code Type} of {@code Bean/Array/Collection/Map}.
     * @return
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromXml(final Reader json, final TypeReference<? extends T> targetType) {
        try {
            return defaultXmlMapper.readValue(json, targetType);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     *
     * @param <T>
     * @param json
     * @param targetType can be the {@code Type} of {@code Bean/Array/Collection/Map}.
     * @param config
     * @return
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromXml(final Reader json, final TypeReference<? extends T> targetType, final DeserializationConfig config) {
        final XmlMapper xmlMapper = getXmlMapper(config);

        try {
            return xmlMapper.readValue(json, targetType);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        } finally {
            recycle(xmlMapper);
        }
    }

    /**
     *
     * @param <T>
     * @param json
     * @param targetType
     * @return
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromXml(final URL json, final TypeReference<? extends T> targetType) {
        try {
            return defaultXmlMapper.readValue(json, targetType);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     *
     * @param <T>
     * @param json
     * @param targetType
     * @param config
     * @return
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromXml(final URL json, final TypeReference<? extends T> targetType, final DeserializationConfig config) {
        final XmlMapper xmlMapper = getXmlMapper(config);

        try {
            return xmlMapper.readValue(json, targetType);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        } finally {
            recycle(xmlMapper);
        }
    }

    /**
     *
     * @param <T>
     * @param json
     * @param targetType
     * @return
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromXml(final DataInput json, final TypeReference<? extends T> targetType) {
        try {
            return defaultXmlMapper.readValue(json, defaultXmlMapper.constructType(targetType));
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     *
     * @param <T>
     * @param json
     * @param targetType
     * @param config
     * @return
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromXml(final DataInput json, final TypeReference<? extends T> targetType, final DeserializationConfig config) {
        final XmlMapper xmlMapper = getXmlMapper(config);

        try {
            return xmlMapper.readValue(json, xmlMapper.constructType(targetType));
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        } finally {
            recycle(xmlMapper);
        }
    }

    public static SerializationConfig createSerializationConfig() {
        // final SerializationConfig copy = defaultSerializationConfigForCopy.without(serializationFeatureNotEnabledByDefault);

        return defaultSerializationConfigForCopy.without(serializationFeatureNotEnabledByDefault);
    }

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

    static XmlMapper getXmlMapper(final SerializationConfig config) {
        if (config == null) {
            return defaultXmlMapper;
        }

        XmlMapper mapper = null;

        synchronized (mapperPool) {
            if (mapperPool.size() > 0) {
                mapper = mapperPool.remove(mapperPool.size() - 1);
            }
        }

        if (mapper == null) {
            mapper = new XmlMapper();
        }

        mapper.setConfig(config);

        return mapper;
    }

    static XmlMapper getXmlMapper(final DeserializationConfig config) {
        if (config == null) {
            return defaultXmlMapper;
        }

        XmlMapper mapper = null;

        synchronized (mapperPool) {
            if (mapperPool.size() > 0) {
                mapper = mapperPool.remove(mapperPool.size() - 1);
            }
        }

        if (mapper == null) {
            mapper = new XmlMapper();
        }

        mapper.setConfig(config);

        return mapper;
    }

    static void recycle(final XmlMapper mapper) {
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
     * @param xmlMapper
     * @return
     */
    public static One wrap(final XmlMapper xmlMapper) {
        return new One(xmlMapper);
    }

    public static final class One {
        private final XmlMapper xmlMapper;
        private final XmlMapper xmlMapperForPretty;

        One(final XmlMapper xmlMapper) {
            this.xmlMapper = xmlMapper;
            xmlMapperForPretty = xmlMapper.copy();

            xmlMapperForPretty.enable(SerializationFeature.INDENT_OUTPUT);
        }

        /**
         *
         * @param obj
         * @return
         */
        public String toXml(final Object obj) {
            try {
                return xmlMapper.writeValueAsString(obj);
            } catch (final JsonProcessingException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }

        /**
         *
         * @param obj
         * @param prettyFormat
         * @return
         */
        public String toXml(final Object obj, final boolean prettyFormat) {
            try {
                if (prettyFormat) {
                    return xmlMapperForPretty.writeValueAsString(obj);
                } else {
                    return xmlMapper.writeValueAsString(obj);
                }
            } catch (final JsonProcessingException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }

        /**
         *
         * @param obj
         * @param output
         */
        public void toXml(final Object obj, final File output) {
            try {
                xmlMapper.writeValue(output, obj);
            } catch (final IOException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }

        /**
         *
         * @param obj
         * @param output
         */
        public void toXml(final Object obj, final OutputStream output) {
            try {
                xmlMapper.writeValue(output, obj);
            } catch (final IOException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }

        /**
         *
         * @param obj
         * @param output
         */
        public void toXml(final Object obj, final Writer output) {
            try {
                xmlMapper.writeValue(output, obj);
            } catch (final IOException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }

        /**
         *
         * @param obj
         * @param output
         */
        public void toXml(final Object obj, final DataOutput output) {
            try {
                xmlMapper.writeValue(output, obj);
            } catch (final IOException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }

        /**
         *
         * @param <T>
         * @param json
         * @param targetType
         * @return
         * @see com.fasterxml.jackson.core.type.TypeReference
         */
        public <T> T fromXml(final byte[] json, final Class<? extends T> targetType) {
            try {
                return xmlMapper.readValue(json, targetType);
            } catch (final IOException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }

        /**
         *
         * @param <T>
         * @param json
         * @param offset
         * @param len
         * @param targetType
         * @return
         * @see com.fasterxml.jackson.core.type.TypeReference
         */
        public <T> T fromXml(final byte[] json, final int offset, final int len, final Class<? extends T> targetType) {
            try {
                return xmlMapper.readValue(json, offset, len, targetType);
            } catch (final IOException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }

        /**
         *
         * @param <T>
         * @param json
         * @param targetType
         * @return
         * @see com.fasterxml.jackson.core.type.TypeReference
         */
        public <T> T fromXml(final String json, final Class<? extends T> targetType) {
            try {
                return xmlMapper.readValue(json, targetType);
            } catch (final JsonProcessingException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }

        /**
         *
         * @param <T>
         * @param json
         * @param targetType
         * @param defaultIfNull
         * @return
         * @see com.fasterxml.jackson.core.type.TypeReference
         */
        public <T> T fromXml(final String json, final Class<? extends T> targetType, final T defaultIfNull) {
            if (N.isEmpty(json)) {
                return defaultIfNull;
            }

            try {
                return N.defaultIfNull(xmlMapper.readValue(json, targetType), defaultIfNull);
            } catch (final JsonProcessingException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }

        /**
         *
         * @param <T>
         * @param json
         * @param targetType
         * @return
         * @see com.fasterxml.jackson.core.type.TypeReference
         */
        public <T> T fromXml(final File json, final Class<? extends T> targetType) {
            try {
                return xmlMapper.readValue(json, targetType);
            } catch (final IOException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }

        /**
         *
         * @param <T>
         * @param json
         * @param targetType
         * @return
         * @see com.fasterxml.jackson.core.type.TypeReference
         */
        public <T> T fromXml(final InputStream json, final Class<? extends T> targetType) {
            try {
                return xmlMapper.readValue(json, targetType);
            } catch (final IOException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }

        /**
         *
         * @param <T>
         * @param json
         * @param targetType
         * @return
         * @see com.fasterxml.jackson.core.type.TypeReference
         */
        public <T> T fromXml(final Reader json, final Class<? extends T> targetType) {
            try {
                return xmlMapper.readValue(json, targetType);
            } catch (final IOException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }

        /**
         *
         * @param <T>
         * @param json
         * @param targetType
         * @return
         * @see com.fasterxml.jackson.core.type.TypeReference
         */
        public <T> T fromXml(final URL json, final Class<? extends T> targetType) {
            try {
                return xmlMapper.readValue(json, targetType);
            } catch (final IOException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }

        /**
         *
         * @param <T>
         * @param json
         * @param targetType
         * @return
         * @see com.fasterxml.jackson.core.type.TypeReference
         */
        public <T> T fromXml(final DataInput json, final Class<? extends T> targetType) {
            try {
                return xmlMapper.readValue(json, targetType);
            } catch (final IOException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }

        /**
         *
         * @param <T>
         * @param json
         * @param targetType
         * @return
         * @see com.fasterxml.jackson.core.type.TypeReference
         */
        public <T> T fromXml(final byte[] json, final TypeReference<? extends T> targetType) {
            try {
                return xmlMapper.readValue(json, targetType);
            } catch (final IOException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }

        /**
         *
         * @param <T>
         * @param json
         * @param offset
         * @param len
         * @param targetType
         * @return
         * @see com.fasterxml.jackson.core.type.TypeReference
         */
        public <T> T fromXml(final byte[] json, final int offset, final int len, final TypeReference<? extends T> targetType) {
            try {
                return xmlMapper.readValue(json, offset, len, targetType);
            } catch (final IOException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }

        /**
         *
         * @param <T>
         * @param json
         * @param targetType can be the {@code Type} of {@code Bean/Array/Collection/Map}.
         * @return
         * @see com.fasterxml.jackson.core.type.TypeReference
         */
        public <T> T fromXml(final String json, final TypeReference<? extends T> targetType) {
            try {
                return xmlMapper.readValue(json, targetType);
            } catch (final IOException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }

        /**
         *
         * @param <T>
         * @param json
         * @param targetType
         * @param defaultIfNull
         * @return
         * @see com.fasterxml.jackson.core.type.TypeReference
         */
        public <T> T fromXml(final String json, final TypeReference<? extends T> targetType, final T defaultIfNull) {
            if (N.isEmpty(json)) {
                return defaultIfNull;
            }

            try {
                return N.defaultIfNull(xmlMapper.readValue(json, targetType), defaultIfNull);
            } catch (final JsonProcessingException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }

        /**
         *
         * @param <T>
         * @param json
         * @param targetType can be the {@code Type} of {@code Bean/Array/Collection/Map}.
         * @return
         * @see com.fasterxml.jackson.core.type.TypeReference
         */
        public <T> T fromXml(final File json, final TypeReference<? extends T> targetType) {
            try {
                return xmlMapper.readValue(json, targetType);
            } catch (final IOException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }

        /**
         *
         * @param <T>
         * @param json
         * @param targetType can be the {@code Type} of {@code Bean/Array/Collection/Map}.
         * @return
         * @see com.fasterxml.jackson.core.type.TypeReference
         */
        public <T> T fromXml(final InputStream json, final TypeReference<? extends T> targetType) {
            try {
                return xmlMapper.readValue(json, targetType);
            } catch (final IOException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }

        /**
         *
         * @param <T>
         * @param json
         * @param targetType can be the {@code Type} of {@code Bean/Array/Collection/Map}.
         * @return
         * @see com.fasterxml.jackson.core.type.TypeReference
         */
        public <T> T fromXml(final Reader json, final TypeReference<? extends T> targetType) {
            try {
                return xmlMapper.readValue(json, targetType);
            } catch (final IOException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }

        /**
         *
         * @param <T>
         * @param json
         * @param targetType
         * @return
         * @see com.fasterxml.jackson.core.type.TypeReference
         */
        public <T> T fromXml(final URL json, final TypeReference<? extends T> targetType) {
            try {
                return xmlMapper.readValue(json, targetType);
            } catch (final IOException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }

        /**
         *
         * @param <T>
         * @param json
         * @param targetType
         * @return
         * @see com.fasterxml.jackson.core.type.TypeReference
         */
        public <T> T fromXml(final DataInput json, final TypeReference<? extends T> targetType) {
            try {
                return xmlMapper.readValue(json, xmlMapper.constructType(targetType));
            } catch (final IOException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }
    }
}
