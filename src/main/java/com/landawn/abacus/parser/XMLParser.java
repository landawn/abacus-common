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

import java.io.InputStream;
import java.io.Reader;
import java.util.Map;

import org.w3c.dom.Node;

public interface XMLParser extends Parser<XMLSerializationConfig, XMLDeserializationConfig> {

    /**
     *
     *
     * @param <T>
     * @param source
     * @param targetClass
     * @return
     */
    <T> T deserialize(Node source, Class<? extends T> targetClass);

    /**
     *
     *
     * @param <T>
     * @param source
     * @param config
     * @param targetClass
     * @return
     */
    <T> T deserialize(Node source, XMLDeserializationConfig config, Class<? extends T> targetClass);

    /**
     *
     *
     * @param <T>
     * @param source
     * @param config
     * @param nodeClasses
     * @return
     */
    <T> T deserialize(InputStream source, XMLDeserializationConfig config, Map<String, Class<?>> nodeClasses);

    /**
     *
     *
     * @param <T>
     * @param source
     * @param config
     * @param nodeClasses
     * @return
     */
    <T> T deserialize(Reader source, XMLDeserializationConfig config, Map<String, Class<?>> nodeClasses);

    /**
     *
     *
     * @param <T>
     * @param source
     * @param config
     * @param nodeClasses
     * @return
     */
    <T> T deserialize(Node source, XMLDeserializationConfig config, Map<String, Class<?>> nodeClasses);
}
