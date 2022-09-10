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

/**
 *
 * @author Haiyang Li
 * @since 0.8
 */
public interface XMLParser extends Parser<XMLSerializationConfig, XMLDeserializationConfig> {

    /**
     *
     * @param <T>
     * @param targetClass
     * @param node
     * @return
     */
    <T> T deserialize(Class<? extends T> targetClass, Node node);

    /**
     *
     * @param <T>
     * @param targetClass
     * @param node
     * @param config
     * @return
     */
    <T> T deserialize(Class<? extends T> targetClass, Node node, XMLDeserializationConfig config);

    /**
     *
     * @param <T>
     * @param nodeClasses
     * @param source
     * @param config
     * @return
     */
    <T> T deserialize(Map<String, Class<?>> nodeClasses, InputStream source, XMLDeserializationConfig config);

    /**
     *
     * @param <T>
     * @param nodeClasses
     * @param source
     * @param config
     * @return
     */
    <T> T deserialize(Map<String, Class<?>> nodeClasses, Reader source, XMLDeserializationConfig config);

    /**
     *
     * @param <T>
     * @param nodeClasses
     * @param node
     * @param config
     * @return
     */
    <T> T deserialize(Map<String, Class<?>> nodeClasses, Node node, XMLDeserializationConfig config);
}
