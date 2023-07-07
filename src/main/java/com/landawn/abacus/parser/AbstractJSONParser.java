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
import java.util.Set;

import com.landawn.abacus.type.Type;
import com.landawn.abacus.type.TypeFactory;
import com.landawn.abacus.util.ExceptionalStream;
import com.landawn.abacus.util.N;
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

    protected static final Set<String> ignoredDirtyMarkerPropNames = N.asSet("signedPropNames", "dirtyPropNames", "frozen", "version", "isDirty",
            "dirtyMarkerImpl");

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
     * @param <T>
     * @param targetClass
     * @param str
     * @return
     */
    @Override
    public <T> T readString(Class<? extends T> targetClass, String str) {
        return readString(targetClass, str, null);
    }

    /**
     *
     *
     * @param <T>
     * @param targetClass
     * @param str
     * @param config
     * @return
     * @throws UnsupportedOperationException
     */
    @Override
    public <T> T readString(Class<? extends T> targetClass, String str, JSONDeserializationConfig config) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     *
     * @param outResult
     * @param str
     */
    @Override
    public void readString(Object[] outResult, String str) {
        readString(outResult, str, null);
    }

    /**
     *
     *
     * @param outResult
     * @param str
     * @param config
     * @throws UnsupportedOperationException
     */
    @Override
    public void readString(Object[] outResult, String str, JSONDeserializationConfig config) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     *
     * @param outResult
     * @param str
     */
    @Override
    public void readString(Collection<?> outResult, String str) {
        readString(outResult, str, null);
    }

    /**
     *
     *
     * @param outResult
     * @param str
     * @param config
     * @throws UnsupportedOperationException
     */
    @Override
    public void readString(Collection<?> outResult, String str, JSONDeserializationConfig config) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     *
     * @param outResult
     * @param str
     */
    @Override
    public void readString(Map<?, ?> outResult, String str) {
        readString(outResult, str, null);
    }

    /**
     *
     *
     * @param outResult
     * @param str
     * @param config
     * @throws UnsupportedOperationException
     */
    @Override
    public void readString(Map<?, ?> outResult, String str, JSONDeserializationConfig config) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     *
     * @param <T>
     * @param targetClass
     * @param str
     * @param fromIndex
     * @param toIndex
     * @return
     */
    @Override
    public <T> T deserialize(Class<? extends T> targetClass, String str, int fromIndex, int toIndex) {
        return deserialize(targetClass, str, fromIndex, toIndex, null);
    }

    /**
     *
     * @param <T>
     * @param targetClass
     * @param str
     * @param fromIndex
     * @param toIndex
     * @param config
     * @return
     */
    @Override
    public <T> T deserialize(Class<? extends T> targetClass, String str, int fromIndex, int toIndex, JSONDeserializationConfig config) {
        return deserialize(targetClass, str.substring(fromIndex, toIndex), config);
    }

    /**
     *
     *
     * @param <T>
     * @param elementClass
     * @param json
     * @return
     */
    @Override
    public <T> ExceptionalStream<T, IOException> stream(Class<? extends T> elementClass, String json) {
        return stream(elementClass, json, null);
    }

    /**
     *
     *
     * @param <T>
     * @param elementClass
     * @param file
     * @return
     */
    @Override
    public <T> ExceptionalStream<T, IOException> stream(Class<? extends T> elementClass, File file) {
        return stream(elementClass, file, null);
    }

    /**
     *
     *
     * @param <T>
     * @param elementClass
     * @param is
     * @param closeInputStreamWhenStreamIsClosed
     * @return
     */
    @Override
    public <T> ExceptionalStream<T, IOException> stream(Class<? extends T> elementClass, InputStream is, boolean closeInputStreamWhenStreamIsClosed) {
        return stream(elementClass, is, closeInputStreamWhenStreamIsClosed, null);
    }

    /**
     *
     *
     * @param <T>
     * @param elementClass
     * @param reader
     * @param closeReaderWhenStreamIsClosed
     * @return
     */
    @Override
    public <T> ExceptionalStream<T, IOException> stream(Class<? extends T> elementClass, Reader reader, boolean closeReaderWhenStreamIsClosed) {
        return stream(elementClass, reader, closeReaderWhenStreamIsClosed, null);
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
