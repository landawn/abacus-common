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

import com.landawn.abacus.util.DateTimeFormat;
import com.landawn.abacus.util.N;

/**
 * Abstract base class for serialization configuration that provides common settings
 * for controlling how objects are serialized.
 * 
 * <p>This class extends {@link ParserConfig} and provides additional serialization-specific
 * settings such as field exclusion and transient field handling.</p>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * SerializationConfig config = new MySerializationConfig()
 *     .setExclusion(Exclusion.NULL)
 *     .skipTransientField(true);
 * }</pre>
 *
 * @param <C> the concrete configuration type for method chaining
 * @see ParserConfig
 * @see Exclusion
 */
public abstract class SerializationConfig<C extends SerializationConfig<C>> extends ParserConfig<C> {

    protected static final Exclusion defaultExclusion = null; // Keep it null for default behavior. Do not set it to Exclusion.NULL or Exclusion.DEFAULT.

    protected static final DateTimeFormat defaultDateTimeFormat = DateTimeFormat.LONG;

    protected static final boolean defaultSkipTransientField = true;

    Exclusion exclusion = defaultExclusion;

    boolean skipTransientField = defaultSkipTransientField;

    /**
     * Gets the current exclusion strategy for field serialization.
     * 
     * <p>The exclusion strategy determines which fields should be excluded from serialization
     * based on their values or other criteria.</p>
     *
     * @return the current {@link Exclusion} strategy, or null if no exclusion is set
     * @see Exclusion
     */
    public Exclusion getExclusion() {
        return exclusion;
    }

    /**
     * Sets the exclusion strategy for field serialization in beans.
     * 
     * <p>The exclusion strategy allows you to control which fields are included
     * or excluded during serialization based on their values or other criteria.</p>
     * 
     * <p>Example:</p>
     * <pre>{@code
     * config.setExclusion(Exclusion.NULL); // Exclude null fields
     * config.setExclusion(Exclusion.EMPTY); // Exclude empty collections/strings
     * }</pre>
     *
     * @param exclusion the exclusion strategy to use, or null for no exclusion
     * @return this configuration instance for method chaining
     * @see Exclusion
     */
    public C setExclusion(final Exclusion exclusion) {
        this.exclusion = exclusion;

        return (C) this;
    }

    /**
     * Checks whether transient fields should be skipped during serialization.
     * 
     * <p>When true, fields marked with the {@code transient} keyword or
     * {@code @Transient} annotation will be excluded from serialization.</p>
     *
     * @return {@code true} if transient fields should be skipped, {@code false} otherwise
     */
    public boolean skipTransientField() {
        return skipTransientField;
    }

    /**
     * Sets whether transient fields should be skipped during serialization.
     * 
     * <p>When set to true, fields marked with the {@code transient} keyword or
     * {@code @Transient} annotation will be excluded from serialization.
     * This is useful for excluding fields that should not be persisted or transmitted.</p>
     * 
     * <p>Example:</p>
     * <pre>{@code
     * config.skipTransientField(true); // Skip transient fields
     * config.skipTransientField(false); // Include transient fields
     * }</pre>
     *
     * @param skipTransientField {@code true} to skip transient fields, {@code false} to include them
     * @return this configuration instance for method chaining
     */
    public C skipTransientField(final boolean skipTransientField) {
        this.skipTransientField = skipTransientField;

        return (C) this;
    }

    /**
     * Computes a hash code for this configuration based on its settings.
     * 
     * <p>The hash code is computed using the ignored property names, exclusion strategy,
     * and skipTransientField setting.</p>
     *
     * @return a hash code value for this configuration
     */
    @Override
    public int hashCode() {
        int h = 17;
        h = 31 * h + N.hashCode(getIgnoredPropNames());
        h = 31 * h + N.hashCode(exclusion);
        h = 31 * h + N.hashCode(skipTransientField);
        return 31 * h + N.hashCode(skipTransientField);
    }

    /**
     * Determines whether this configuration is equal to another object.
     * 
     * <p>Two SerializationConfig instances are considered equal if they have the same
     * ignored property names, exclusion strategy, and skipTransientField setting.</p>
     *
     * @param obj the object to compare with
     * @return {@code true} if the configurations are equal, {@code false} otherwise
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof SerializationConfig) {
            final SerializationConfig<C> other = (SerializationConfig<C>) obj;

            return N.equals(getIgnoredPropNames(), other.getIgnoredPropNames()) && N.equals(exclusion, other.exclusion)
                    && N.equals(skipTransientField, other.skipTransientField);
        }

        return false;
    }

    /**
     * Returns a string representation of this configuration.
     * 
     * <p>The string includes the ignored property names, exclusion strategy,
     * and skipTransientField setting in a readable format.</p>
     *
     * @return a string representation of this configuration
     */
    @Override
    public String toString() {
        return "{ignoredPropNames=" + N.toString(getIgnoredPropNames()) + ", exclusion=" + N.toString(exclusion) + ", skipTransientField="
                + N.toString(skipTransientField) + "}";
    }
}