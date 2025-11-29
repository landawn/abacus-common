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

import java.util.Map;
import java.util.Set;

import com.landawn.abacus.annotation.SuppressFBWarnings;
import com.landawn.abacus.util.N;

/**
 * Configuration class for Kryo serialization settings.
 * 
 * <p>This class extends {@link SerializationConfig} to provide Kryo-specific
 * serialization options such as whether to write class information.</p>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * KryoSerializationConfig config = new KryoSerializationConfig()
 *     .writeClass(true)
 *     .setExclusion(Exclusion.NULL)
 *     .skipTransientField(true);
 * }</pre>
 * 
 * @see SerializationConfig
 * @see KryoParser
 */
public class KryoSerializationConfig extends SerializationConfig<KryoSerializationConfig> {

    protected static final boolean defaultWriteClass = false;

    private boolean writeClass = defaultWriteClass;

    /**
     * Creates a new KryoSerializationConfig with default settings.
     * 
     * <p>Default settings:</p>
     * <ul>
     *   <li>writeClass: false</li>
     *   <li>Inherits defaults from SerializationConfig</li>
     * </ul>
     */
    public KryoSerializationConfig() { //NOSONAR
    }

    /**
     * Checks if class information should be written during serialization.
     * 
     * <p>When {@code true}, Kryo will write the full class name with the serialized data,
     * allowing deserialization without specifying the target class. This increases
     * the serialized data size but provides more flexibility.</p>
     *
     * @return {@code true} if class information should be written, {@code false} otherwise
     */
    public boolean writeClass() {
        return writeClass;
    }

    /**
     * Sets whether class information should be written during serialization.
     * 
     * <p>When set to {@code true}, Kryo will include the full class name in the serialized
     * data. This allows deserializing objects without knowing their type in advance,
     * but increases the size of the serialized data.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * config.writeClass(true);    // Include class information
     * config.writeClass(false);   // Omit class information (more compact)
     * }</pre>
     *
     * @param writeClass {@code true} to write class information, {@code false} to omit it
     * @return this configuration instance for method chaining
     */
    public KryoSerializationConfig writeClass(final boolean writeClass) {
        this.writeClass = writeClass;

        return this;
    }

    /**
     * Computes a hash code for this configuration based on its settings.
     *
     * <p>The hash code includes the writeClass setting in addition to settings
     * inherited from the parent class.</p>
     *
     * @return a hash code value for this configuration
     */
    @Override
    public int hashCode() {
        int h = 17;
        h = 31 * h + N.hashCode(getIgnoredPropNames());
        h = 31 * h + N.hashCode(getExclusion());
        h = 31 * h + N.hashCode(skipTransientField());
        return 31 * h + N.hashCode(writeClass);
    }

    /**
     * Determines whether this configuration is equal to another object.
     * 
     * <p>Two KryoSerializationConfig instances are considered equal if they have
     * the same writeClass setting and all inherited settings are equal.</p>
     *
     * @param obj the object to compare with
     * @return {@code true} if the configurations are equal, {@code false} otherwise
     */
    @SuppressFBWarnings
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof KryoSerializationConfig other) { //NOSONAR
            return N.equals(getIgnoredPropNames(), other.getIgnoredPropNames()) && N.equals(getExclusion(), other.getExclusion()) //NOSONAR
                    && N.equals(skipTransientField(), other.skipTransientField()) && N.equals(writeClass, other.writeClass);
        }

        return false;
    }

    /**
     * Returns a string representation of this configuration.
     *
     * <p>The string includes all configuration settings in the format:
     * {@code {ignoredPropNames=..., exclusion=..., skipTransientField=..., writeClass=...}}</p>
     *
     * @return a string representation of this configuration
     */
    @Override
    public String toString() {
        return "{ignoredPropNames=" + N.toString(getIgnoredPropNames()) + ", exclusion=" + N.toString(getExclusion()) + ", skipTransientField="
                + N.toString(skipTransientField()) + ", writeClass=" + N.toString(writeClass) + "}";
    }

    /**
     * Factory class for creating KryoSerializationConfig instances.
     * 
     * <p>Provides static factory methods for convenient configuration creation.
     * This inner class is named KSC for brevity.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * KryoSerializationConfig config = KSC.create()
     *     .writeClass(true)
     *     .setExclusion(Exclusion.NULL);
     * }</pre>
     */
    public static final class KSC extends KryoSerializationConfig {

        /**
         * Constructs a new KSC instance.
         */
        public KSC() {
        }

        /**
         * Creates a new KryoSerializationConfig instance with default settings.
         *
         * @return a new KryoSerializationConfig instance
         */
        public static KryoSerializationConfig create() {
            return new KryoSerializationConfig();
        }

        /**
         * Creates a new KryoSerializationConfig with the specified writeClass setting.
         * 
         * @param writeClass whether to write class information
         * @return a new configured KryoSerializationConfig instance
         * @deprecated to be removed in a future version. Use {@link #create()} followed by {@link #writeClass(boolean)} instead.
         */
        @Deprecated
        public static KryoSerializationConfig of(final boolean writeClass) {
            return create().writeClass(writeClass);
        }

        /**
         * Creates a new KryoSerializationConfig with the specified exclusion and ignored properties.
         * 
         * @param exclusion the exclusion strategy
         * @param ignoredPropNames map of ignored property names by class
         * @return a new configured KryoSerializationConfig instance
         * @deprecated to be removed in a future version. Use {@link #create()} with method chaining instead.
         */
        @Deprecated
        public static KryoSerializationConfig of(final Exclusion exclusion, final Map<Class<?>, Set<String>> ignoredPropNames) {
            return create().setExclusion(exclusion).setIgnoredPropNames(ignoredPropNames);
        }

        /**
         * Creates a new KryoSerializationConfig with all specified settings.
         * 
         * @param writeClass whether to write class information
         * @param exclusion the exclusion strategy
         * @param ignoredPropNames map of ignored property names by class
         * @return a new configured KryoSerializationConfig instance
         * @deprecated to be removed in a future version. Use {@link #create()} with method chaining instead.
         */
        @Deprecated
        public static KryoSerializationConfig of(final boolean writeClass, final Exclusion exclusion, final Map<Class<?>, Set<String>> ignoredPropNames) {
            return create().writeClass(writeClass).setExclusion(exclusion).setIgnoredPropNames(ignoredPropNames);
        }
    }
}
