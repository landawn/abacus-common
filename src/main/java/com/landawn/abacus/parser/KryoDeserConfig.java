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
 * Configuration class for Kryo deserialization operations.
 * This class extends {@link DeserializationConfig} to provide Kryo-specific deserialization settings.
 *
 * <p>The configuration allows control over various aspects of the deserialization process,
 * including property handling, type mappings, and value conversions inherited from the parent class.</p>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * KryoDeserConfig config = KryoDeserConfig.create()
 *     .setIgnoreUnmatchedProperty(true)
 *     .setElementType(String.class);
 *
 * KryoParser parser = new KryoParser();
 * MyObject obj = parser.deserialize(kryoData, config, MyObject.class);
 * }</pre>
 *
 * @see DeserializationConfig
 * @see KryoParser
 */
public class KryoDeserConfig extends DeserializationConfig<KryoDeserConfig> {

    /**
     * Constructs a new {@code KryoDeserConfig} with default settings.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * KryoDeserConfig config = new KryoDeserConfig()
     *     .setIgnoreUnmatchedProperty(true)
     *     .setElementType(String.class);
     * }</pre>
     *
     */
    public KryoDeserConfig() {
    }

    /**
     * Computes a hash code for this configuration based on its settings.
     *
     * @return a hash code value for this configuration
     */
    @Override
    public int hashCode() {
        return super.hashCode();
    }

    /**
     * Compares this configuration with another object for equality.
     *
     * @param obj the object to compare with
     * @return {@code true} if the objects are equal, {@code false} otherwise
     */
    @Override
    public boolean equals(final Object obj) {
        return this == obj || (obj instanceof KryoDeserConfig && super.equals(obj));
    }

    /**
     * Returns a string representation of this configuration.
     *
     * <p>The string includes all configuration settings in the format:
     * {@code {ignoredPropNames=..., ignoreUnmatchedProperty=..., ...}}</p>
     *
     * @return a string representation of this configuration
     */
    @Override
    public String toString() {
        return super.toString();
    }

    /**
     * Creates a new instance of {@code KryoDeserConfig} with default settings.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * KryoDeserConfig config = KryoDeserConfig.create()
     *     .setMapKeyType(String.class)
     *     .setMapValueType(Object.class);
     * }</pre>
     *
     * @return a new {@code KryoDeserConfig} instance
     */
    public static KryoDeserConfig create() {
        return new KryoDeserConfig();
    }
}
