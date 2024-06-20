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

import com.landawn.abacus.util.CheckedStream;

/**
 *
 * @author Haiyang Li
 * @since 0.8
 */
public interface JSONParser extends Parser<JSONSerializationConfig, JSONDeserializationConfig> {

    /**
     * 
     *
     * @param <T> 
     * @param source 
     * @param targetClass 
     * @return 
     */
    <T> T readString(String source, Class<? extends T> targetClass);

    /**
     * 
     *
     * @param <T> 
     * @param source 
     * @param config 
     * @param targetClass 
     * @return 
     */
    <T> T readString(String source, JSONDeserializationConfig config, Class<? extends T> targetClass);

    /**
     *
     * @param source
     * @param output
     */
    void readString(String source, Object[] output);

    /**
     *
     * @param source
     * @param config
     * @param output
     */
    void readString(String source, JSONDeserializationConfig config, Object[] output);

    /**
     *
     * @param source
     * @param output
     */
    void readString(String source, Collection<?> output);

    /**
     *
     * @param source
     * @param config
     * @param output
     */
    void readString(String source, JSONDeserializationConfig config, Collection<?> output);

    /**
     *
     * @param source
     * @param output
     */
    void readString(String source, Map<?, ?> output);

    /**
     *
     * @param source
     * @param config
     * @param output
     */
    void readString(String source, JSONDeserializationConfig config, Map<?, ?> output);

    /**
     * 
     *
     * @param <T> 
     * @param source 
     * @param fromIndex 
     * @param toIndex 
     * @param targetClass 
     * @return 
     */
    <T> T deserialize(String source, int fromIndex, int toIndex, Class<? extends T> targetClass);

    /**
     * 
     *
     * @param <T> 
     * @param source 
     * @param fromIndex 
     * @param toIndex 
     * @param config 
     * @param targetClass 
     * @return 
     */
    <T> T deserialize(String source, int fromIndex, int toIndex, JSONDeserializationConfig config, Class<? extends T> targetClass);

    /**
     * 
     *
     * @param <T> 
     * @param source 
     * @param elementClass Only Bean/Map/Collection/Array/DataSet element types are supported at present.
     * @return 
     */
    <T> CheckedStream<T, IOException> stream(String source, Class<? extends T> elementClass);

    /**
     * 
     *
     * @param <T> 
     * @param source 
     * @param config 
     * @param elementClass Only Bean/Map/Collection/Array/DataSet element types are supported at present.
     * @return 
     */
    <T> CheckedStream<T, IOException> stream(String source, JSONDeserializationConfig config, Class<? extends T> elementClass);

    /**
     * 
     *
     * @param <T> 
     * @param source 
     * @param elementClass Only Bean/Map/Collection/Array/DataSet element types are supported at present.
     * @return 
     */
    <T> CheckedStream<T, IOException> stream(File source, Class<? extends T> elementClass);

    /**
     * 
     *
     * @param <T> 
     * @param source 
     * @param config 
     * @param elementClass Only Bean/Map/Collection/Array/DataSet element types are supported at present.
     * @return 
     */
    <T> CheckedStream<T, IOException> stream(File source, JSONDeserializationConfig config, Class<? extends T> elementClass);

    /**
     * 
     *
     * @param <T> 
     * @param source 
     * @param closeInputStreamWhenStreamIsClosed 
     * @param elementClass Only Bean/Map/Collection/Array/DataSet element types are supported at present.
     * @return 
     */
    <T> CheckedStream<T, IOException> stream(InputStream source, boolean closeInputStreamWhenStreamIsClosed, Class<? extends T> elementClass);

    /**
     * 
     *
     * @param <T> 
     * @param source 
     * @param config 
     * @param closeInputStreamWhenStreamIsClosed 
     * @param elementClass Only Bean/Map/Collection/Array/DataSet element types are supported at present.
     * @return 
     */
    <T> CheckedStream<T, IOException> stream(InputStream source, JSONDeserializationConfig config, boolean closeInputStreamWhenStreamIsClosed,
            Class<? extends T> elementClass);

    /**
     * 
     *
     * @param <T> 
     * @param source 
     * @param closeReaderWhenStreamIsClosed 
     * @param elementClass Only Bean/Map/Collection/Array/DataSet element types are supported at present.
     * @return 
     */
    <T> CheckedStream<T, IOException> stream(Reader source, boolean closeReaderWhenStreamIsClosed, Class<? extends T> elementClass);

    /**
     * 
     *
     * @param <T> 
     * @param source 
     * @param config 
     * @param closeReaderWhenStreamIsClosed 
     * @param elementClass Only Bean/Map/Collection/Array/DataSet element types are supported at present.
     * @return 
     */
    <T> CheckedStream<T, IOException> stream(Reader source, JSONDeserializationConfig config, boolean closeReaderWhenStreamIsClosed,
            Class<? extends T> elementClass);
}
