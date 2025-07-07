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

import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.util.Collection;
import java.util.Map;

import com.landawn.abacus.type.Type;
import com.landawn.abacus.type.TypeFactory;
import com.landawn.abacus.util.WD;
import com.landawn.abacus.util.stream.Stream;

abstract class AbstractJSONParser extends AbstractParser<JSONSerializationConfig, JSONDeserializationConfig> implements JSONParser {

    protected static final char _BRACE_L = WD._BRACE_L;

    protected static final char _BRACE_R = WD._BRACE_R;

    protected static final char _BRACKET_L = WD._BRACKET_L;

    protected static final char _BRACKET_R = WD._BRACKET_R;

    protected static final char _D_QUOTATION = WD._QUOTATION_D;

    protected static final char _S_QUOTATION = WD._QUOTATION_S;

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
        defaultJSONSerializationConfig = jsc != null ? jsc : new JSONSerializationConfig();
        defaultJSONDeserializationConfig = jdc != null ? jdc : new JSONDeserializationConfig();
    }

    /**
     *
     * @param <T>
     * @param source
     * @param targetClass
     * @return
     */
    @Override
    public <T> T readString(final String source, final Class<? extends T> targetClass) {
        return readString(source, null, targetClass);
    }

    /**
     *
     * @param <T>
     * @param source
     * @param config
     * @param targetClass
     * @return
     * @throws UnsupportedOperationException
     */
    @Override
    public <T> T readString(final String source, final JSONDeserializationConfig config, final Class<? extends T> targetClass)
            throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     *
     * @param source
     * @param output
     */
    @Override
    public void readString(final String source, final Object[] output) {
        readString(source, null, output);
    }

    /**
     *
     * @param source
     * @param config
     * @param output
     * @throws UnsupportedOperationException
     */
    @Override
    public void readString(final String source, final JSONDeserializationConfig config, final Object[] output) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     *
     * @param source
     * @param output
     */
    @Override
    public void readString(final String source, final Collection<?> output) {
        readString(source, null, output);
    }

    /**
     *
     * @param source
     * @param config
     * @param output
     * @throws UnsupportedOperationException
     */
    @Override
    public void readString(final String source, final JSONDeserializationConfig config, final Collection<?> output) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     *
     * @param source
     * @param output
     */
    @Override
    public void readString(final String source, final Map<?, ?> output) {
        readString(source, null, output);
    }

    /**
     *
     * @param source
     * @param config
     * @param output
     * @throws UnsupportedOperationException
     */
    @Override
    public void readString(final String source, final JSONDeserializationConfig config, final Map<?, ?> output) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     *
     * @param <T>
     * @param source
     * @param fromIndex
     * @param toIndex
     * @param targetClass
     * @return
     */
    @Override
    public <T> T deserialize(final String source, final int fromIndex, final int toIndex, final Class<? extends T> targetClass) {
        return deserialize(source, fromIndex, toIndex, null, targetClass);
    }

    /**
     *
     * @param <T>
     * @param source
     * @param fromIndex
     * @param toIndex
     * @param config
     * @param targetClass
     * @return
     */
    @Override
    public <T> T deserialize(final String source, final int fromIndex, final int toIndex, final JSONDeserializationConfig config,
            final Class<? extends T> targetClass) {
        return deserialize(source.substring(fromIndex, toIndex), config, targetClass);
    }

    /**
     *
     * @param <T>
     * @param source
     * @param elementType
     * @return
     */
    @Override
    public <T> Stream<T> stream(final String source, final Type<? extends T> elementType) {
        return stream(source, null, elementType);
    }

    /**
     *
     * @param <T>
     * @param source
     * @param elementType
     * @return
     */
    @Override
    public <T> Stream<T> stream(final File source, final Type<? extends T> elementType) {
        return stream(source, null, elementType);
    }

    /**
     *
     * @param <T>
     * @param source
     * @param closeInputStreamWhenStreamIsClosed
     * @param elementType
     * @return
     */
    @Override
    public <T> Stream<T> stream(final InputStream source, final boolean closeInputStreamWhenStreamIsClosed, final Type<? extends T> elementType) {
        return stream(source, null, closeInputStreamWhenStreamIsClosed, elementType);
    }

    /**
     *
     * @param <T>
     * @param reader
     * @param closeReaderWhenStreamIsClosed
     * @param elementType
     * @return
     */
    @Override
    public <T> Stream<T> stream(final Reader reader, final boolean closeReaderWhenStreamIsClosed, final Type<? extends T> elementType) {
        return stream(reader, null, closeReaderWhenStreamIsClosed, elementType);
    }

    protected JSONSerializationConfig check(JSONSerializationConfig config) {
        return config == null ? defaultJSONSerializationConfig : config;
    }

    protected JSONDeserializationConfig check(JSONDeserializationConfig config) {
        return config == null ? defaultJSONDeserializationConfig : config;
    }
}
