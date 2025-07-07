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
 * including property handling, type mappings, and value conversions inherited from the parent class.
 * 
 * <p>Example usage:
 * <pre>{@code
 * KryoDeserializationConfig config = KryoDeserializationConfig.create()
 *     .ignoreUnmatchedProperty(true)
 *     .setElementType(String.class);
 * 
 * KryoParser parser = new KryoParser();
 * MyObject obj = parser.deserialize(kryoData, config, MyObject.class);
 * }</pre>
 * 
 * @see DeserializationConfig
 * @see KryoParser
 * @since 1.0
 */
public class KryoDeserializationConfig extends DeserializationConfig<KryoDeserializationConfig> {

    //    /**
    //     *
    //     * @return
    //     */
    //    @Override
    //    public KryoDeserializationConfig copy() {
    //        final KryoDeserializationConfig copy = new KryoDeserializationConfig();
    //
    //        copy.setIgnoredPropNames(this.getIgnoredPropNames());
    //        copy.ignoreUnmatchedProperty = this.ignoreUnmatchedProperty;
    //        copy.elementType = this.elementType;
    //        copy.keyType = this.keyType;
    //        copy.valueType = this.valueType;
    //        copy.propTypes = this.propTypes;
    //
    //        return copy;
    //    }

    /**
     * Factory class for creating {@link KryoDeserializationConfig} instances.
     * Provides convenient static factory methods for configuration creation.
     * 
     * <p>Example usage:
     * <pre>{@code
     * // Create a default configuration
     * KryoDeserializationConfig config = KDC.create();
     * 
     * // Create and configure in one line
     * KryoDeserializationConfig config = KDC.create()
     *     .ignoreUnmatchedProperty(true)
     *     .setElementType(String.class);
     * }</pre>
     */
    public static final class KDC extends KryoDeserializationConfig {

        /**
         * Creates a new instance of {@link KryoDeserializationConfig} with default settings.
         * 
         * @return a new {@link KryoDeserializationConfig} instance
         */
        public static KryoDeserializationConfig create() {
            return new KryoDeserializationConfig();
        }
    }
}