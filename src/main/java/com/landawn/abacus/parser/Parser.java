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

import com.landawn.abacus.annotation.JsonXmlField;
import com.landawn.abacus.exception.UncheckedIOException;

/**
 * Design principles: <li>1, Simple (is beautiful)</li> <li>2, Fast (is powerful)</li> <li>3, Concepts (must be integral
 * and consistent)</li> <br/>
 * These principles can't be broken by any change or reason. And basically programmable is > configurable. There is no
 * extra support by configuration file or annotation.
 *
 * All the implementation should be multi-thread safety.
 *
 * @author Haiyang Li
 * @param <SC>
 * @param <DC>
 * @see JsonXmlField
 * @since 0.8
 */
public interface Parser<SC extends SerializationConfig<?>, DC extends DeserializationConfig<?>> {
    /**
     *
     * @param obj
     * @return
     */
    String serialize(Object obj);

    /**
     *
     * @param obj
     * @param config
     * @return
     */
    String serialize(Object obj, SC config);

    /**
     *
     * @param obj
     * @param output
     * @throws UncheckedIOException the unchecked IO exception
     */
    void serialize(Object obj, File output) throws UncheckedIOException;

    /**
     *
     * @param obj
     * @param config
     * @param output
     * @throws UncheckedIOException the unchecked IO exception
     */
    void serialize(Object obj, SC config, File output) throws UncheckedIOException;

    /**
     *
     * @param obj
     * @param output
     * @throws UncheckedIOException the unchecked IO exception
     */
    void serialize(Object obj, OutputStream output) throws UncheckedIOException;

    /**
     *
     * @param obj
     * @param config
     * @param output
     * @throws UncheckedIOException the unchecked IO exception
     */
    void serialize(Object obj, SC config, OutputStream output) throws UncheckedIOException;

    /**
     *
     * @param obj
     * @param output
     * @throws UncheckedIOException the unchecked IO exception
     */
    void serialize(Object obj, Writer output) throws UncheckedIOException;

    /**
     *
     * @param obj
     * @param config
     * @param output
     * @throws UncheckedIOException the unchecked IO exception
     */
    void serialize(Object obj, SC config, Writer output) throws UncheckedIOException;

    /**
     *
     * @param source
     * @param targetClass
     * @param <T>
     * @return
     */
    <T> T deserialize(String source, Class<? extends T> targetClass);

    /**
     *
     * @param source
     * @param config
     * @param targetClass
     * @param <T>
     * @return
     */
    <T> T deserialize(String source, DC config, Class<? extends T> targetClass);

    /**
     *
     * @param source
     * @param targetClass
     * @param <T>
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    <T> T deserialize(File source, Class<? extends T> targetClass) throws UncheckedIOException;

    /**
     *
     * @param source
     * @param config
     * @param targetClass
     * @param <T>
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    <T> T deserialize(File source, DC config, Class<? extends T> targetClass) throws UncheckedIOException;

    /**
     *
     * @param source
     * @param targetClass
     * @param <T>
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    <T> T deserialize(InputStream source, Class<? extends T> targetClass) throws UncheckedIOException;

    /**
     *
     * @param source
     * @param config
     * @param targetClass
     * @param <T>
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    <T> T deserialize(InputStream source, DC config, Class<? extends T> targetClass) throws UncheckedIOException;

    /**
     *
     * @param source
     * @param targetClass
     * @param <T>
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    <T> T deserialize(Reader source, Class<? extends T> targetClass) throws UncheckedIOException;

    /**
     *
     * @param source
     * @param config
     * @param targetClass
     * @param <T>
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    <T> T deserialize(Reader source, DC config, Class<? extends T> targetClass) throws UncheckedIOException;
}
