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
import java.io.Reader;
import java.util.Collection;
import java.util.Map;

import com.landawn.abacus.type.Type;
import com.landawn.abacus.type.TypeFactory;
import com.landawn.abacus.util.CheckedStream;
import com.landawn.abacus.util.WD;

/**
 *
 * @author Haiyang Li
 * @since 0.8
 */
abstract class AbstractJSONParser extends AbstractParser<JSONSerializationConfig, JSONDeserializationConfig> implements JSONParser {

    protected static final char _BRACE_L = WD._BRACE_L;

    protected static final char _BRACE_R = WD._BRACE_R;

    protected static final char _BRACKET_L = WD._BRACKET_L;

    protected static final char _BRACKET_R = WD._BRACKET_R;

    protected static final char _D_QUOTATION = WD._QUOTATION_D;

    protected static final char _S_QUOTATION = WD._QUOTATION_S;

    protected static final char _COLON = WD._COLON;

    protected static final char _COMMA = WD._COMMA;

    protected static final Type<Object> objType = TypeFactory.getType(Object.class);

    protected static final Type<String> strType = TypeFactory.getType(String.class);

    protected static final Type<Boolean> boolType = TypeFactory.getType(Boolean.class);

    protected static final Type<?> defaultKeyType = objType;

    protected static final Type<?> defaultValueType = objType;

    protected static final String[] REPLACEMENT_CHARS;

    static {
        final int length = 128;
        REPLACEMENT_CHARS = new String[length];

        for (int i = 0; i < length; i++) {
            REPLACEMENT_CHARS[i] = String.format("\\u%04x", i);
        }
    }

    protected final JSONSerializationConfig defaultJSONSerializationConfig;

    protected final JSONDeserializationConfig defaultJSONDeserializationConfig;

    protected AbstractJSONParser() {
        this(null, null);
    }

    protected AbstractJSONParser(final JSONSerializationConfig jsc, final JSONDeserializationConfig jdc) {
        this.defaultJSONSerializationConfig = jsc != null ? jsc : new JSONSerializationConfig();
        this.defaultJSONDeserializationConfig = jdc != null ? jdc : new JSONDeserializationConfig();
    }

    /**
     *
     * @param source
     * @param targetClass
     * @param <T>
     * @return
     */
    @Override
    public <T> T readString(String source, Class<? extends T> targetClass) {
        return readString(source, null, targetClass);
    }

    /**
     *
     *
     * @param source
     * @param config
     * @param targetClass
     * @param <T>
     * @return
     * @throws UnsupportedOperationException
     */
    @Override
    public <T> T readString(String source, JSONDeserializationConfig config, Class<? extends T> targetClass) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     *
     * @param source
     * @param output
     */
    @Override
    public void readString(String source, Object[] output) {
        readString(source, null, output);
    }

    /**
     *
     *
     * @param source
     * @param config
     * @param output
     * @throws UnsupportedOperationException
     */
    @Override
    public void readString(String source, JSONDeserializationConfig config, Object[] output) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     *
     * @param source
     * @param output
     */
    @Override
    public void readString(String source, Collection<?> output) {
        readString(source, null, output);
    }

    /**
     *
     *
     * @param source
     * @param config
     * @param output
     * @throws UnsupportedOperationException
     */
    @Override
    public void readString(String source, JSONDeserializationConfig config, Collection<?> output) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     *
     * @param source
     * @param output
     */
    @Override
    public void readString(String source, Map<?, ?> output) {
        readString(source, null, output);
    }

    /**
     *
     *
     * @param config
     * @param output
     * @param source
     * @throws UnsupportedOperationException
     */
    @Override
    public void readString(String source, JSONDeserializationConfig config, Map<?, ?> output) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     *
     * @param source
     * @param fromIndex
     * @param toIndex
     * @param targetClass
     * @param <T>
     * @return
     */
    @Override
    public <T> T deserialize(String source, int fromIndex, int toIndex, Class<? extends T> targetClass) {
        return deserialize(source, fromIndex, toIndex, null, targetClass);
    }

    /**
     *
     * @param source
     * @param fromIndex
     * @param toIndex
     * @param config
     * @param targetClass
     * @param <T>
     * @return
     */
    @Override
    public <T> T deserialize(String source, int fromIndex, int toIndex, JSONDeserializationConfig config, Class<? extends T> targetClass) {
        return deserialize(source.substring(fromIndex, toIndex), config, targetClass);
    }

    /**
     *
     *
     * @param elementClass
     * @param source
     * @param <T>
     * @return
     */
    @Override
    public <T> CheckedStream<T, IOException> stream(String source, Class<? extends T> elementClass) {
        return stream(source, null, elementClass);
    }

    /**
     *
     *
     * @param elementClass
     * @param source
     * @param <T>
     * @return
     */
    @Override
    public <T> CheckedStream<T, IOException> stream(File source, Class<? extends T> elementClass) {
        return stream(source, null, elementClass);
    }

    /**
     *
     *
     * @param closeInputStreamWhenStreamIsClosed
     * @param elementClass
     * @param source
     * @param <T>
     * @return
     */
    @Override
    public <T> CheckedStream<T, IOException> stream(InputStream source, boolean closeInputStreamWhenStreamIsClosed, Class<? extends T> elementClass) {
        return stream(source, null, closeInputStreamWhenStreamIsClosed, elementClass);
    }

    /**
     *
     *
     * @param closeReaderWhenStreamIsClosed
     * @param elementClass
     * @param reader
     * @param <T>
     * @return
     */
    @Override
    public <T> CheckedStream<T, IOException> stream(Reader reader, boolean closeReaderWhenStreamIsClosed, Class<? extends T> elementClass) {
        return stream(reader, null, closeReaderWhenStreamIsClosed, elementClass);
    }

    /**
     *
     * @param config
     * @return
     */
    protected JSONSerializationConfig check(JSONSerializationConfig config) {
        if (config == null) {
            config = defaultJSONSerializationConfig;
        }

        return config;
    }

    /**
     *
     * @param config
     * @return
     */
    protected JSONDeserializationConfig check(JSONDeserializationConfig config) {
        if (config == null) {
            config = defaultJSONDeserializationConfig;
        }

        return config;
    }
}
