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
     * @param output
     * @param obj
     * @throws UncheckedIOException the unchecked IO exception
     */
    void serialize(File output, Object obj) throws UncheckedIOException;

    /**
     *
     * @param output
     * @param obj
     * @param config
     * @throws UncheckedIOException the unchecked IO exception
     */
    void serialize(File output, Object obj, SC config) throws UncheckedIOException;

    /**
     *
     * @param output
     * @param obj
     * @throws UncheckedIOException the unchecked IO exception
     */
    void serialize(OutputStream output, Object obj) throws UncheckedIOException;

    /**
     *
     * @param output
     * @param obj
     * @param config
     * @throws UncheckedIOException the unchecked IO exception
     */
    void serialize(OutputStream output, Object obj, SC config) throws UncheckedIOException;

    /**
     *
     * @param output
     * @param obj
     * @throws UncheckedIOException the unchecked IO exception
     */
    void serialize(Writer output, Object obj) throws UncheckedIOException;

    /**
     *
     * @param output
     * @param obj
     * @param config
     * @throws UncheckedIOException the unchecked IO exception
     */
    void serialize(Writer output, Object obj, SC config) throws UncheckedIOException;

    /**
     *
     * @param <T>
     * @param targetClass
     * @param source
     * @return
     */
    <T> T deserialize(Class<? extends T> targetClass, String source);

    /**
     *
     * @param <T>
     * @param targetClass
     * @param source
     * @param config
     * @return
     */
    <T> T deserialize(Class<? extends T> targetClass, String source, DC config);

    /**
     *
     * @param <T>
     * @param targetClass
     * @param source
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    <T> T deserialize(Class<? extends T> targetClass, File source) throws UncheckedIOException;

    /**
     *
     * @param <T>
     * @param targetClass
     * @param source
     * @param config
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    <T> T deserialize(Class<? extends T> targetClass, File source, DC config) throws UncheckedIOException;

    /**
     *
     * @param <T>
     * @param targetClass
     * @param source
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    <T> T deserialize(Class<? extends T> targetClass, InputStream source) throws UncheckedIOException;

    /**
     *
     * @param <T>
     * @param targetClass
     * @param source
     * @param config
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    <T> T deserialize(Class<? extends T> targetClass, InputStream source, DC config) throws UncheckedIOException;

    /**
     *
     * @param <T>
     * @param targetClass
     * @param source
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    <T> T deserialize(Class<? extends T> targetClass, Reader source) throws UncheckedIOException;

    /**
     *
     * @param <T>
     * @param targetClass
     * @param source
     * @param config
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    <T> T deserialize(Class<? extends T> targetClass, Reader source, DC config) throws UncheckedIOException;
}
