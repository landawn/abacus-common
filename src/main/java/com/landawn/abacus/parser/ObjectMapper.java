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
     * @param output
     * @param obj
     */
    void write(File output, Object obj);

    /**
     *
     * @param output
     * @param obj
     * @param config
     */
    void write(File output, Object obj, C config);

    /**
     *
     * @param output
     * @param obj
     */
    void write(OutputStream output, Object obj);

    /**
     *
     * @param output
     * @param obj
     * @param config
     */
    void write(OutputStream output, Object obj, C config);

    /**
     *
     * @param output
     * @param obj
     */
    void write(Writer output, Object obj);

    /**
     *
     * @param output
     * @param obj
     * @param config
     */
    void write(Writer output, Object obj, C config);

    /**
     *
     * @param <T>
     * @param cls
     * @param from
     * @return
     */
    <T> T read(Class<? extends T> cls, String from);

    /**
     *
     * @param <T>
     * @param cls
     * @param from
     * @param config
     * @return
     */
    <T> T read(Class<? extends T> cls, String from, C config);

    /**
     *
     * @param <T>
     * @param cls
     * @param from
     * @return
     */
    <T> T read(Class<? extends T> cls, File from);

    /**
     *
     * @param <T>
     * @param cls
     * @param from
     * @param config
     * @return
     */
    <T> T read(Class<? extends T> cls, File from, C config);

    /**
     *
     * @param <T>
     * @param cls
     * @param from
     * @return
     */
    <T> T read(Class<? extends T> cls, InputStream from);

    /**
     *
     * @param <T>
     * @param cls
     * @param from
     * @param config
     * @return
     */
    <T> T read(Class<? extends T> cls, InputStream from, C config);

    /**
     *
     * @param <T>
     * @param cls
     * @param from
     * @return
     */
    <T> T read(Class<? extends T> cls, Reader from);

    /**
     *
     * @param <T>
     * @param cls
     * @param from
     * @param config
     * @return
     */
    <T> T read(Class<? extends T> cls, Reader from, C config);
}
