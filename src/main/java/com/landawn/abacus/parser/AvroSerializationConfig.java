/*
 * Copyright (C) 2015 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

public class AvroSerializationConfig extends SerializationConfig<AvroSerializationConfig> {

    private Schema schema;

    public AvroSerializationConfig() { //NOSONAR
    }

    /**
     * Gets the schema.
     *
     * @return
     */
    public Schema getSchema() {
        return schema;
    }

    /**
     * Sets the schema.
     *
     * @param schema
     * @return
     */
    public AvroSerializationConfig setSchema(final Schema schema) {
        this.schema = schema;

        return this;
    }

    //    /**
    //     *
    //     * @return
    //     */
    //    @Override
    //    public AvroSerializationConfig copy() {
    //        final AvroSerializationConfig copy = new AvroSerializationConfig();
    //
    //        copy.setIgnoredPropNames(this.getIgnoredPropNames());
    //        copy.setCharQuotation(this.getCharQuotation());
    //        copy.setStringQuotation(this.getStringQuotation());
    //        copy.setDateTimeFormat(this.getDateTimeFormat());
    //        copy.setExclusion(this.getExclusion());
    //        copy.setSkipTransientField(this.isSkipTransientField());
    //        copy.setPrettyFormat(this.isPrettyFormat());
    //        copy.supportCircularReference(this.supportCircularReference());
    //        copy.writeBigDecimalAsPlain(this.writeBigDecimalAsPlain());
    //        copy.setIndentation(this.getIndentation());
    //        copy.setPropNamingPolicy(this.getPropNamingPolicy());
    //        copy.setIgnoredPropNames(this.getIgnoredPropNames());
    //        copy.schema = this.schema;
    //
    //        return copy;
    //    }

    @Override
    public int hashCode() {
        int h = 17;
        h = 31 * h + N.hashCode(getIgnoredPropNames());
        h = 31 * h + N.hashCode(getExclusion());
        h = 31 * h + N.hashCode(skipTransientField());
        return 31 * h + N.hashCode(schema);
    }

    /**
     *
     * @param obj
     * @return {@code true}, if successful
     */
    @SuppressFBWarnings
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof final AvroSerializationConfig other) {
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
     * The Class ASC.
     */
    public static final class ASC extends AvroSerializationConfig {

        public static AvroSerializationConfig create() {
            return new AvroSerializationConfig();
        }

        /**
         *
         * @param schema
         * @return
         * @deprecated
         */
        @Deprecated
        public static AvroSerializationConfig of(final Schema schema) {
            return create().setSchema(schema);
        }

        /**
         *
         * @param schema
         * @param exclusion
         * @param ignoredPropNames
         * @return
         * @deprecated
         */
        @Deprecated
        public static AvroSerializationConfig of(final Schema schema, final Exclusion exclusion, final Map<Class<?>, Set<String>> ignoredPropNames) {
            return create().setSchema(schema).setExclusion(exclusion).setIgnoredPropNames(ignoredPropNames);
        }
    }
}
