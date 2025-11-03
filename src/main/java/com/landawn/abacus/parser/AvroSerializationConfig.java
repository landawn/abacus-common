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

import org.apache.avro.Schema;

import com.landawn.abacus.annotation.SuppressFBWarnings;
import com.landawn.abacus.util.N;

/**
 * Configuration class for Apache Avro serialization operations.
 * This class extends SerializationConfig and adds Avro-specific configuration options,
 * particularly the Avro schema required for serialization.
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * Schema schema = new Schema.Parser().parse(schemaString);
 * AvroSerializationConfig config = new AvroSerializationConfig()
 *     .setSchema(schema)
 *     .setExclusion(Exclusion.NULL);
 * }</pre>
 * 
 */
public class AvroSerializationConfig extends SerializationConfig<AvroSerializationConfig> {

    private Schema schema;

    /**
     * Creates a new instance of AvroSerializationConfig with default settings.
     */
    public AvroSerializationConfig() { //NOSONAR
    }

    /**
     * Gets the Avro schema used for serialization.
     * The schema defines the structure of the data to be serialized.
     *
     * @return the Avro schema, or {@code null} if not set
     */
    public Schema getSchema() {
        return schema;
    }

    /**
     * Sets the Avro schema for serialization.
     * The schema is required for serializing objects that are not SpecificRecord instances.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String schemaJson = "{\"type\":\"record\",\"name\":\"User\"," +
     *     "\"fields\":[{\"name\":\"name\",\"type\":\"string\"}," +
     *     "{\"name\":\"age\",\"type\":\"int\"}]}";
     * Schema schema = new Schema.Parser().parse(schemaJson);
     * config.setSchema(schema);
     * }</pre>
     *
     * @param schema the Avro schema to use for serialization
     * @return this instance for method chaining
     */
    public AvroSerializationConfig setSchema(final Schema schema) {
        this.schema = schema;

        return this;
    }

    @Override
    public int hashCode() {
        int h = 17;
        h = 31 * h + N.hashCode(getIgnoredPropNames());
        h = 31 * h + N.hashCode(getExclusion());
        h = 31 * h + N.hashCode(skipTransientField());
        return 31 * h + N.hashCode(schema);
    }

    /**
     * Compares this configuration with another object for equality.
     * Two configurations are considered equal if they have the same ignored properties,
     * exclusion settings, transient field handling, and schema.
     *
     * @param obj the object to compare with
     * @return {@code true} if the objects are equal, {@code false} otherwise
     */
    @SuppressFBWarnings
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof AvroSerializationConfig other) {
            return N.equals(getIgnoredPropNames(), other.getIgnoredPropNames()) && N.equals(getExclusion(), other.getExclusion()) //NOSONAR
                    && N.equals(skipTransientField(), other.skipTransientField()) && N.equals(schema, other.schema);
        }

        return false;
    }

    @Override
    public String toString() {
        return "{ignoredPropNames=" + N.toString(getIgnoredPropNames()) + ", exclusion=" + N.toString(getExclusion()) + ", skipTransientField="
                + N.toString(skipTransientField()) + ", schema=" + N.toString(schema) + "}";
    }

    /**
     * Factory class for creating AvroSerializationConfig instances.
     * Provides convenient static factory methods for creating configurations.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * AvroSerializationConfig config = ASC.create()
     *     .setSchema(mySchema)
     *     .skipTransientField(true);
     * }</pre>
     */
    public static final class ASC extends AvroSerializationConfig {

        /**
         * Private constructor to prevent instantiation.
         * Use the static factory method {@link #create()} instead.
         */
        private ASC() {
            // Private constructor
        }

        /**
         * Creates a new instance of AvroSerializationConfig with default settings.
         *
         * @return a new AvroSerializationConfig instance
         */
        public static AvroSerializationConfig create() {
            return new AvroSerializationConfig();
        }

        /**
         * Creates a new AvroSerializationConfig with the specified schema.
         *
         * @param schema the Avro schema to use
         * @return a new configured AvroSerializationConfig instance
         * @deprecated to be removed in a future version. Use {@link #create()} with method chaining instead.
         */
        @Deprecated
        public static AvroSerializationConfig of(final Schema schema) {
            return create().setSchema(schema);
        }

        /**
         * Creates a new AvroSerializationConfig with schema, exclusion policy, and ignored properties.
         *
         * @param schema the Avro schema to use
         * @param exclusion the exclusion policy for properties
         * @param ignoredPropNames map of class to set of property names to ignore
         * @return a new configured AvroSerializationConfig instance
         * @deprecated to be removed in a future version. Use {@link #create()} with method chaining instead.
         */
        @Deprecated
        public static AvroSerializationConfig of(final Schema schema, final Exclusion exclusion, final Map<Class<?>, Set<String>> ignoredPropNames) {
            return create().setSchema(schema).setExclusion(exclusion).setIgnoredPropNames(ignoredPropNames);
        }
    }
}
