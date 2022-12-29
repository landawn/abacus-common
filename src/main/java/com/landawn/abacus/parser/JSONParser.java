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

import com.landawn.abacus.util.ExceptionalStream;

/**
 *
 * @author Haiyang Li
 * @since 0.8
 */
public interface JSONParser extends Parser<JSONSerializationConfig, JSONDeserializationConfig> {

    /**
     *
     * @param <T>
     * @param targetClass
     * @param str
     * @return
     */
    <T> T readString(Class<? extends T> targetClass, String str);

    /**
     *
     * @param <T>
     * @param targetClass
     * @param str
     * @param config
     * @return
     */
    <T> T readString(Class<? extends T> targetClass, String str, JSONDeserializationConfig config);

    /**
     *
     * @param outResult
     * @param str
     */
    void readString(Object[] outResult, String str);

    /**
     *
     * @param outResult
     * @param str
     * @param config
     */
    void readString(Object[] outResult, String str, JSONDeserializationConfig config);

    /**
     *
     * @param outResult
     * @param str
     */
    void readString(Collection<?> outResult, String str);

    /**
     *
     * @param outResult
     * @param str
     * @param config
     */
    void readString(Collection<?> outResult, String str, JSONDeserializationConfig config);

    /**
     *
     * @param outResult
     * @param str
     */
    void readString(Map<?, ?> outResult, String str);

    /**
     *
     * @param outResult
     * @param str
     * @param config
     */
    void readString(Map<?, ?> outResult, String str, JSONDeserializationConfig config);

    /**
     *
     * @param <T>
     * @param targetClass
     * @param str
     * @param fromIndex
     * @param toIndex
     * @return
     */
    <T> T deserialize(Class<? extends T> targetClass, String str, int fromIndex, int toIndex);

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
    <T> T deserialize(Class<? extends T> targetClass, String str, int fromIndex, int toIndex, JSONDeserializationConfig config);

    /**
     *
     * @param <T>
     * @param elementClass Only Bean/Map/Collection/Array/DataSet element types are supported at present.
     * @param source
     * @return
     */
    <T> ExceptionalStream<T, IOException> stream(Class<? extends T> elementClass, String source);

    /**
     *
     * @param <T>
     * @param elementClass Only Bean/Map/Collection/Array/DataSet element types are supported at present.
     * @param source
     * @param config
     * @return
     */
    <T> ExceptionalStream<T, IOException> stream(Class<? extends T> elementClass, String source, JSONDeserializationConfig config);

    /**
     *
     * @param <T>
     * @param elementClass Only Bean/Map/Collection/Array/DataSet element types are supported at present.
     * @param source
     * @return
     */
    <T> ExceptionalStream<T, IOException> stream(Class<? extends T> elementClass, File source);

    /**
     *
     * @param <T>
     * @param elementClass Only Bean/Map/Collection/Array/DataSet element types are supported at present.
     * @param source
     * @param config
     * @return
     */
    <T> ExceptionalStream<T, IOException> stream(Class<? extends T> elementClass, File source, JSONDeserializationConfig config);

    /**
     *
     * @param <T>
     * @param elementClass Only Bean/Map/Collection/Array/DataSet element types are supported at present.
     * @param source
     * @param closeInputStreamWhenStreamIsClosed
     * @return
     */
    <T> ExceptionalStream<T, IOException> stream(Class<? extends T> elementClass, InputStream source, boolean closeInputStreamWhenStreamIsClosed);

    /**
     *
     * @param <T>
     * @param elementClass Only Bean/Map/Collection/Array/DataSet element types are supported at present.
     * @param source
     * @param closeInputStreamWhenStreamIsClosed
     * @param config
     * @return
     */
    <T> ExceptionalStream<T, IOException> stream(Class<? extends T> elementClass, InputStream source, boolean closeInputStreamWhenStreamIsClosed,
            JSONDeserializationConfig config);

    /**
     *
     * @param <T>
     * @param elementClass Only Bean/Map/Collection/Array/DataSet element types are supported at present.
     * @param source
     * @param closeReaderWhenStreamIsClosed
     * @return
     */
    <T> ExceptionalStream<T, IOException> stream(Class<? extends T> elementClass, Reader source, boolean closeReaderWhenStreamIsClosed);

    /**
     *
     * @param <T>
     * @param elementClass Only Bean/Map/Collection/Array/DataSet element types are supported at present.
     * @param source
     * @param closeReaderWhenStreamIsClosed
     * @param config
     * @return
     */
    <T> ExceptionalStream<T, IOException> stream(Class<? extends T> elementClass, Reader source, boolean closeReaderWhenStreamIsClosed,
            JSONDeserializationConfig config);
}
