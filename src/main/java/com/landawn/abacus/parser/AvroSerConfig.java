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
 * Configuration class for Apache Avro serialization operations.
 * This class extends {@link SerializationConfig} and adds Avro-specific configuration options,
 * particularly the Avro schema required for serialization.
 *
 * <p><b>Design note:</b> The {@link #getSchema()}/{@link #setSchema(Schema)} pair is intentionally
 * duplicated between this class and {@link AvroDeserConfig}. A shared {@code AvroConfig} base is not
 * possible because the two classes have different (and unrelated) supertypes —
 * {@code AvroSerConfig extends SerializationConfig} while {@code AvroDeserConfig extends DeserializationConfig} —
 * so the schema accessors (and their {@code equals}/{@code hashCode}/{@code toString} contributions) are
 * declared independently on each.</p>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * Schema schema = new Schema.Parser().parse(schemaString);
 * AvroSerConfig config = new AvroSerConfig()
 *     .setSchema(schema)
 *     .setExclusion(Exclusion.NULL);
 * }</pre>
 *
 * @see SerializationConfig
 * @see AvroDeserConfig
 * @see AvroParser
 */
public class AvroSerConfig extends SerializationConfig<AvroSerConfig> {

    private Schema schema;

    /**
     * Constructs a new instance of {@code AvroSerConfig} with default settings.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * AvroSerConfig config = new AvroSerConfig()
     *     .setSchema(schema)
     *     .setExclusion(Exclusion.NULL);
     * }</pre>
     *
     */
    public AvroSerConfig() { //NOSONAR
    }

    /**
     * Gets the Avro {@link Schema} used for serialization.
     * The schema defines the structure of the data to be serialized.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * AvroSerConfig config = new AvroSerConfig();
     * Schema none = config.getSchema();  // returns null (not set)
     *
     * Schema schema = new Schema.Parser().parse(schemaJson);
     * config.setSchema(schema);
     * Schema current = config.getSchema();  // returns the configured schema
     * }</pre>
     *
     * @return the Avro schema, or {@code null} if not set
     */
    public Schema getSchema() {
        return schema;
    }

    /**
     * Sets the Avro {@link Schema} for serialization.
     * The schema is required for serializing objects that are not {@code SpecificRecord} instances.
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
    public AvroSerConfig setSchema(final Schema schema) {
        this.schema = schema;

        return this;
    }

    /**
     * Computes a hash code for this configuration based on its settings.
     *
     * <p>The hash code includes the schema setting in addition to settings
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

        if (obj instanceof AvroSerConfig other) {
            return N.equals(getIgnoredPropNames(), other.getIgnoredPropNames()) && N.equals(getExclusion(), other.getExclusion()) //NOSONAR
                    && N.equals(isSkipTransientField(), other.isSkipTransientField()) && N.equals(schema, other.schema);
        }

        return false;
    }

    /**
     * Returns a string representation of this configuration.
     *
     * <p>The string includes all configuration settings in the format:
     * {@code {ignoredPropNames=..., exclusion=..., skipTransientField=..., schema=...}}</p>
     *
     * @return a string representation of this configuration
     */
    @Override
    public String toString() {
        return "{ignoredPropNames=" + N.toString(getIgnoredPropNames()) + ", exclusion=" + N.toString(getExclusion()) + ", skipTransientField="
                + N.toString(isSkipTransientField()) + ", schema=" + N.toString(schema) + "}";
    }

    /**
     * Creates a new instance of {@code AvroSerConfig} with default settings.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * AvroSerConfig config = AvroSerConfig.create()
     *     .setSchema(schema)
     *     .setSkipTransientField(true);
     * }</pre>
     *
     * @return a new {@code AvroSerConfig} instance
     */
    public static AvroSerConfig create() {
        return new AvroSerConfig();
    }

}
