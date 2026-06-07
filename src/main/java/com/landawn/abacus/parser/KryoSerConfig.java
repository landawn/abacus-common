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
 * KryoSerConfig config = new KryoSerConfig()
 *     .setWriteClass(true)
 *     .setExclusion(Exclusion.NULL)
 *     .setSkipTransientField(true);
 * }</pre>
 *
 * @see SerializationConfig
 * @see KryoParser
 */
public class KryoSerConfig extends SerializationConfig<KryoSerConfig> {

    /** The default value for whether to write class information. */
    protected static final boolean defaultWriteClass = false;

    private boolean writeClass = defaultWriteClass;

    /**
     * Creates a new {@code KryoSerConfig} with default settings.
     *
     * <p>Default settings:</p>
     * <ul>
     *   <li>{@code writeClass}: {@code false}</li>
     *   <li>Inherits defaults from {@link SerializationConfig}</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * KryoSerConfig config = new KryoSerConfig()
     *     .setWriteClass(true)
     *     .setSkipTransientField(true);
     * }</pre>
     *
     */
    public KryoSerConfig() { //NOSONAR
    }

    /**
     * Checks if class information should be written during serialization.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * KryoSerConfig config = new KryoSerConfig();
     * boolean writes = config.isWriteClass();  // returns false (default)
     *
     * config.setWriteClass(true);
     * boolean enabled = config.isWriteClass();  // returns true
     * }</pre>
     *
     * @return {@code true} if class information should be written, {@code false} otherwise
     */
    public boolean isWriteClass() {
        return writeClass;
    }

    /**
     * Sets whether class information should be written during serialization.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * KryoSerConfig config = new KryoSerConfig();
     * config.setWriteClass(true);                 // returns this (config) for chaining
     * boolean enabled = config.isWriteClass();    // returns true
     *
     * config.setWriteClass(false);                // returns this (config) for chaining
     * boolean disabled = config.isWriteClass();   // returns false (default)
     * }</pre>
     *
     * @param writeClass {@code true} to write class information, {@code false} to omit it
     * @return this configuration instance for method chaining
     */
    public KryoSerConfig setWriteClass(final boolean writeClass) {
        this.writeClass = writeClass;

        return this;
    }

    /**
     * Computes a hash code for this configuration based on its settings.
     *
     * <p>The hash code includes the {@code writeClass} setting in addition to settings
     * inherited from the parent class.</p>
     *
     * @return a hash code value for this configuration
     */
    @Override
    public int hashCode() {
        int h = 17;
        h = 31 * h + N.hashCode(getIgnoredPropNames());
        h = 31 * h + N.hashCode(getExclusion());
        h = 31 * h + N.hashCode(isSkipTransientField());
        return 31 * h + N.hashCode(writeClass);
    }

    /**
     * Determines whether this configuration is equal to another object.
     *
     * <p>Two {@code KryoSerConfig} instances are considered equal if they have
     * the same {@code writeClass} setting and all inherited settings are equal.</p>
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

        if (obj instanceof KryoSerConfig other) { //NOSONAR
            return N.equals(getIgnoredPropNames(), other.getIgnoredPropNames()) && N.equals(getExclusion(), other.getExclusion()) //NOSONAR
                    && N.equals(isSkipTransientField(), other.isSkipTransientField()) && N.equals(writeClass, other.writeClass);
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
                + N.toString(isSkipTransientField()) + ", writeClass=" + N.toString(writeClass) + "}";
    }

    /**
     * Creates a new {@code KryoSerConfig} instance with default settings.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * KryoSerConfig config = KryoSerConfig.create()
     *     .setWriteClass(true)
     *     .setExclusion(Exclusion.NULL);
     * }</pre>
     *
     * @return a new {@code KryoSerConfig} instance
     */
    public static KryoSerConfig create() {
        return new KryoSerConfig();
    }

}
