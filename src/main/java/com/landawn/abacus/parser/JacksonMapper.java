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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.landawn.abacus.exception.UncheckedIOException;

/**
 *
 * @author Haiyang Li
 * @since 0.8
 */
final class JacksonMapper extends AbstractObjectMapper<JacksonMapperConfig> {

    private static final List<ObjectMapper> mapperPool = new ArrayList<>(POOL_SIZE);

    private final JacksonMapperConfig defaultJacksonMapperConfig;

    JacksonMapper() {
        this(null);
    }

    JacksonMapper(final JacksonMapperConfig jmc) {
        this.defaultJacksonMapperConfig = jmc != null ? jmc : new JacksonMapperConfig();
    }

    /**
     *
     * @param obj
     * @param config
     * @return
     */
    @Override
    public String write(Object obj, JacksonMapperConfig config) {
        final ObjectMapper mapper = create(config);

        try {
            return mapper.writeValueAsString(obj);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            recycle(mapper);
        }
    }

    /**
     *
     * @param obj
     * @param config
     * @param output
     */
    @Override
    public void write(Object obj, JacksonMapperConfig config, File output) {
        final ObjectMapper mapper = create(config);

        try {
            mapper.writeValue(output, obj);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            recycle(mapper);
        }
    }

    /**
     *
     * @param obj
     * @param config
     * @param output
     */
    @Override
    public void write(Object obj, JacksonMapperConfig config, OutputStream output) {
        final ObjectMapper mapper = create(config);

        try {
            mapper.writeValue(output, obj);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            recycle(mapper);
        }

    }

    /**
     *
     * @param obj
     * @param config
     * @param output
     */
    @Override
    public void write(Object obj, JacksonMapperConfig config, Writer output) {
        final ObjectMapper mapper = create(config);

        try {
            mapper.writeValue(output, obj);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            recycle(mapper);
        }
    }

    /**
     * 
     *
     * @param <T> 
     * @param source 
     * @param config 
     * @param targetType 
     * @return 
     */
    @Override
    public <T> T read(String source, JacksonMapperConfig config, Class<? extends T> targetType) {
        final ObjectMapper mapper = create(config);

        try {
            return mapper.readValue(source, targetType);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            recycle(mapper);
        }
    }

    /**
     * 
     *
     * @param <T> 
     * @param source 
     * @param config 
     * @param targetType 
     * @return 
     */
    @Override
    public <T> T read(File source, JacksonMapperConfig config, Class<? extends T> targetType) {
        final ObjectMapper mapper = create(config);

        try {
            return mapper.readValue(source, targetType);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            recycle(mapper);
        }
    }

    /**
     * 
     *
     * @param <T> 
     * @param source 
     * @param config 
     * @param targetType 
     * @return 
     */
    @Override
    public <T> T read(InputStream source, JacksonMapperConfig config, Class<? extends T> targetType) {
        final ObjectMapper mapper = create(config);

        try {
            return mapper.readValue(source, targetType);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * 
     *
     * @param <T> 
     * @param source 
     * @param config 
     * @param targetType 
     * @return 
     */
    @Override
    public <T> T read(Reader source, JacksonMapperConfig config, Class<? extends T> targetType) {
        final ObjectMapper mapper = create(config);

        try {
            return mapper.readValue(source, targetType);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            recycle(mapper);
        }
    }

    /**
     *
     * @param c
     * @return
     */
    ObjectMapper create(JacksonMapperConfig c) {
        ObjectMapper mapper = null;
        synchronized (mapperPool) {
            if (mapperPool.size() > 0) {
                mapper = mapperPool.remove(mapperPool.size() - 1);
            } else {
                mapper = new ObjectMapper();

                if (c == null) {
                    c = defaultJacksonMapperConfig;
                }
            }
        }

        if (c == null) {
            return mapper;
        }

        mapper.setConfig(c.getSerializationConfig());
        mapper.setConfig(c.getDeserializationConfig());

        return mapper;
    }

    /**
     *
     * @param mapper
     */
    void recycle(ObjectMapper mapper) {
        if (mapper == null) {
            return;
        }

        synchronized (mapperPool) {
            if (mapperPool.size() < POOL_SIZE) {

                mapper.setConfig(defaultJacksonMapperConfig.getSerializationConfig());
                mapper.setConfig(defaultJacksonMapperConfig.getDeserializationConfig());

                mapperPool.add(mapper);
            }
        }
    }
}
