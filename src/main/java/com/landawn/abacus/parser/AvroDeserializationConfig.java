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

import org.apache.avro.Schema;

import com.landawn.abacus.annotation.SuppressFBWarnings;
import com.landawn.abacus.util.N;

/**
 * Configuration class for Apache Avro deserialization operations.
 * This class extends DeserializationConfig and adds Avro-specific configuration options,
 * particularly the Avro schema required for deserialization.
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * Schema schema = new Schema.Parser().parse(schemaString);
 * AvroDeserializationConfig config = new AvroDeserializationConfig()
 *     .setSchema(schema)
 *     .setElementType(Person.class)
 *     .ignoreUnmatchedProperty(true);
 *
 * List<Person> people = parser.deserialize(inputStream, config, List.class);
 * }</pre>
 * 
 * @since 0.8
 */
public class AvroDeserializationConfig extends DeserializationConfig<AvroDeserializationConfig> {
    private Schema schema;

    /**
     * Creates a new instance of AvroDeserializationConfig with default settings.
     * Package-private constructor to encourage use of factory methods.
     */
    AvroDeserializationConfig() {

    }

    /**
     * Gets the Avro schema used for deserialization.
     * The schema defines the expected structure of the data to be deserialized.
     *
     * @return the Avro schema, or null if not set
     */
    public Schema getSchema() {
        return schema;
    }

    /**
     * Sets the Avro schema for deserialization.
     * The schema is required for deserializing data that is not SpecificRecord instances.
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
     * @param schema the Avro schema to use for deserialization
     * @return this instance for method chaining
     */
    public AvroDeserializationConfig setSchema(final Schema schema) {
        this.schema = schema;

        return this;
    }

    @Override
    public int hashCode() {
        int h = 17;
        h = 31 * h + N.hashCode(getIgnoredPropNames());
        h = 31 * h + N.hashCode(ignoreUnmatchedProperty());
        return 31 * h + N.hashCode(schema);
    }

    /**
     * Compares this configuration with another object for equality.
     * Two configurations are considered equal if they have the same ignored properties,
     * unmatched property handling, and schema.
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

        return (obj instanceof AvroDeserializationConfig other) // NOSONAR
                && (N.equals(getIgnoredPropNames(), other.getIgnoredPropNames()) && N.equals(ignoreUnmatchedProperty(), other.ignoreUnmatchedProperty())
                //NOSONAR
                        && N.equals(schema, other.schema));
    }

    @Override
    public String toString() {
        return "{ignoredPropNames=" + N.toString(getIgnoredPropNames()) + ", ignoreUnmatchedProperty=" + N.toString(ignoreUnmatchedProperty()) + ", schema="
                + N.toString(schema) + "}";
    }

    /**
     * Factory class for creating AvroDeserializationConfig instances.
     * Provides convenient static factory methods for creating configurations.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * AvroDeserializationConfig config = ADC.create()
     *     .setSchema(mySchema)
     *     .setElementType(Person.class)
     *     .ignoreUnmatchedProperty(true);
     * }</pre>
     */
    public static final class ADC extends AvroDeserializationConfig {

        /**
         * Creates a new instance of AvroDeserializationConfig with default settings.
         *
         * @return a new AvroDeserializationConfig instance
         */
        public static AvroDeserializationConfig create() {
            return new AvroDeserializationConfig();
        }
    }
}
