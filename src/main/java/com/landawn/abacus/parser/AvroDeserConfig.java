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
 * This class extends {@link DeserializationConfig} and adds Avro-specific configuration options,
 * particularly the Avro schema required for deserialization.
 *
 * <p><b>Design note:</b> The {@link #getSchema()}/{@link #setSchema(Schema)} pair is intentionally
 * duplicated between this class and {@link AvroSerConfig}. A shared {@code AvroConfig} base is not
 * possible because the two classes have different (and unrelated) supertypes —
 * {@code AvroDeserConfig extends DeserializationConfig} while {@code AvroSerConfig extends SerializationConfig} —
 * so the schema accessors (and their {@code equals}/{@code hashCode}/{@code toString} contributions) are
 * declared independently on each.</p>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * Schema schema = new Schema.Parser().parse(schemaString);
 * AvroDeserConfig config = AvroDeserConfig.create()
 *     .setSchema(schema)
 *     .setElementType(Person.class)
 *     .setIgnoreUnmatchedProperty(true);
 *
 * List<Person> people = parser.deserialize(inputStream, config, List.class);
 * }</pre>
 *
 * @see DeserializationConfig
 * @see AvroSerConfig
 * @see AvroParser
 */
public class AvroDeserConfig extends DeserializationConfig<AvroDeserConfig> {
    private Schema schema;

    /**
     * Creates a new instance of {@code AvroDeserConfig} with default settings.
     * This constructor is package-private; external callers should use the public factory method {@link #create()}.
     */
    AvroDeserConfig() {

    }

    /**
     * Gets the Avro schema used for deserialization.
     * The schema defines the expected structure of the data to be deserialized.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * AvroDeserConfig config = AvroDeserConfig.create();
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
     * Sets the Avro schema for deserialization.
     * The schema is required for deserializing data that is not a {@code SpecificRecord} instance.
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
    public AvroDeserConfig setSchema(final Schema schema) {
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
        h = 31 * h + N.hashCode(isIgnoreUnmatchedProperty());
        h = 31 * h + N.hashCode(elementType);
        h = 31 * h + N.hashCode(mapKeyType);
        h = 31 * h + N.hashCode(mapValueType);
        h = 31 * h + N.hashCode(valueTypeMap);
        h = 31 * h + N.hashCode(beanInfoForValueTypes);
        return 31 * h + N.hashCode(schema);
    }

    /**
     * Compares this configuration with another object for equality.
     * Two configurations are considered equal if they have the same ignored properties,
     * unmatched property handling, element/map key/map value types, value-type mappings, and schema.
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

        if (obj instanceof AvroDeserConfig other) { //NOSONAR
            return N.equals(getIgnoredPropNames(), other.getIgnoredPropNames()) && N.equals(isIgnoreUnmatchedProperty(), other.isIgnoreUnmatchedProperty())
                    && N.equals(elementType, other.elementType) && N.equals(mapKeyType, other.mapKeyType) && N.equals(mapValueType, other.mapValueType)
                    && N.equals(valueTypeMap, other.valueTypeMap) && N.equals(beanInfoForValueTypes, other.beanInfoForValueTypes)
                    && N.equals(schema, other.schema);
        }

        return false;
    }

    /**
     * Returns a string representation of this configuration.
     *
     * <p>The string includes all configuration settings in the format:
     * {@code {ignoredPropNames=..., ignoreUnmatchedProperty=..., elementType=..., mapKeyType=..., mapValueType=..., valueTypeMap=..., beanInfoForValueTypes=..., schema=...}}</p>
     *
     * @return a string representation of this configuration
     */
    @Override
    public String toString() {
        return "{ignoredPropNames=" + N.toString(getIgnoredPropNames()) + ", ignoreUnmatchedProperty=" + N.toString(isIgnoreUnmatchedProperty())
                + ", elementType=" + N.toString(elementType) + ", mapKeyType=" + N.toString(mapKeyType) + ", mapValueType=" + N.toString(mapValueType)
                + ", valueTypeMap=" + N.toString(valueTypeMap) + ", beanInfoForValueTypes=" + N.toString(beanInfoForValueTypes) + ", schema="
                + N.toString(schema) + "}";
    }

    /**
     * Creates a new instance of {@code AvroDeserConfig} with default settings.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * AvroDeserConfig config = AvroDeserConfig.create()
     *     .setSchema(schema)
     *     .setIgnoreUnmatchedProperty(true);
     * }</pre>
     *
     * @return a new {@code AvroDeserConfig} instance
     */
    public static AvroDeserConfig create() {
        return new AvroDeserConfig();
    }
}
