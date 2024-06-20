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
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;

/**
 * The intention of this class is to provide concise and consistent interface for Jackson JSON/XML/... data bind.
 *
 * @author Haiyang Li
 * @param <C>
 * @since 0.8
 */
public interface ObjectMapper<C extends MapperConfig> {

    /**
     *
     * @param obj
     * @return
     */
    String write(Object obj);

    /**
     *
     * @param obj
     * @param config
     * @return
     */
    String write(Object obj, C config);

    /**
     *
     * @param obj
     * @param output
     */
    void write(Object obj, File output);

    /**
     *
     * @param obj
     * @param config
     * @param output
     */
    void write(Object obj, C config, File output);

    /**
     *
     * @param obj
     * @param output
     */
    void write(Object obj, OutputStream output);

    /**
     *
     * @param obj
     * @param config
     * @param output
     */
    void write(Object obj, C config, OutputStream output);

    /**
     *
     * @param obj
     * @param output
     */
    void write(Object obj, Writer output);

    /**
     *
     * @param obj
     * @param config
     * @param output
     */
    void write(Object obj, C config, Writer output);

    /**
     * 
     *
     * @param <T> 
     * @param source 
     * @param targetType 
     * @return 
     */
    <T> T read(String source, Class<? extends T> targetType);

    /**
     * 
     *
     * @param <T> 
     * @param source 
     * @param config 
     * @param targetType 
     * @return 
     */
    <T> T read(String source, C config, Class<? extends T> targetType);

    /**
     * 
     *
     * @param <T> 
     * @param source 
     * @param targetType 
     * @return 
     */
    <T> T read(File source, Class<? extends T> targetType);

    /**
     * 
     *
     * @param <T> 
     * @param source 
     * @param config 
     * @param targetType 
     * @return 
     */
    <T> T read(File source, C config, Class<? extends T> targetType);

    /**
     * 
     *
     * @param <T> 
     * @param source 
     * @param targetType 
     * @return 
     */
    <T> T read(InputStream source, Class<? extends T> targetType);

    /**
     * 
     *
     * @param <T> 
     * @param source 
     * @param config 
     * @param targetType 
     * @return 
     */
    <T> T read(InputStream source, C config, Class<? extends T> targetType);

    /**
     * 
     *
     * @param <T> 
     * @param source 
     * @param targetType 
     * @return 
     */
    <T> T read(Reader source, Class<? extends T> targetType);

    /**
     * 
     *
     * @param <T> 
     * @param source 
     * @param config 
     * @param targetType 
     * @return 
     */
    <T> T read(Reader source, C config, Class<? extends T> targetType);
}
