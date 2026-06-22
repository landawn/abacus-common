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

/**
 * Configuration class for XML deserialization settings.
 *
 * <p>This class extends {@link DeserializationConfig} to provide XML-specific
 * deserialization options. It inherits all configuration options from its parent
 * class without adding XML-specific settings.</p>
 *
 * <p><b>Note — intentional omission of the JSON-only deser knobs:</b> the
 * {@code setMapInstanceType} / {@code setReadNullToEmpty} / {@code setIgnoreNullOrEmpty}
 * options live on {@link JsonDeserConfig} on purpose: they are read only by the JSON
 * deserializer ({@code JsonParserImpl}). The XML deserializers ({@code XmlParserImpl} /
 * {@code AbacusXmlParserImpl}) do not consume them, so exposing them here would create
 * dead configuration. They are therefore deliberately NOT provided on {@code XmlDeserConfig}.</p>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * XmlDeserConfig config = new XmlDeserConfig()
 *     .setIgnoreUnmatchedProperty(true)
 *     .setElementType(String.class)
 *     .setMapKeyType(String.class)
 *     .setMapValueType(Integer.class);
 * }</pre>
 *
 * @see DeserializationConfig
 * @see XmlParser
 */
public class XmlDeserConfig extends DeserializationConfig<XmlDeserConfig> {

    /**
     * Creates a new XmlDeserConfig with default settings.
     *
     * <p>Inherits all default settings from {@link DeserializationConfig}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * XmlDeserConfig config = new XmlDeserConfig()
     *     .setIgnoreUnmatchedProperty(true)
     *     .setElementType(String.class);
     * }</pre>
     *
     */
    public XmlDeserConfig() {
        super();
    }

    /**
     * Creates a new XmlDeserConfig instance with default settings.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * XmlDeserConfig config = XmlDeserConfig.create()
     *     .setMapKeyType(String.class)
     *     .setMapValueType(Integer.class);
     * }</pre>
     *
     * @return a new XmlDeserConfig instance
     */
    public static XmlDeserConfig create() {
        return new XmlDeserConfig();
    }

}
