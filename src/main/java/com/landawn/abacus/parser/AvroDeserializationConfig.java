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

import org.apache.avro.Schema;

import com.landawn.abacus.annotation.SuppressFBWarnings;
import com.landawn.abacus.util.N;

/**
 *
 * @author Haiyang Li
 * @since 0.8
 */
public class AvroDeserializationConfig extends DeserializationConfig<AvroDeserializationConfig> {

    private Schema schema;

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
    public AvroDeserializationConfig setSchema(Schema schema) {
        this.schema = schema;

        return this;
    }

    //    /**
    //     *
    //     * @return
    //     */
    //    @Override
    //    public AvroDeserializationConfig copy() {
    //        final AvroDeserializationConfig copy = new AvroDeserializationConfig();
    //
    //        copy.setIgnoredPropNames(this.getIgnoredPropNames());
    //        copy.setIgnoreUnmatchedProperty(this.isIgnoreUnmatchedProperty());
    //        copy.setElementType(this.getElementType());
    //        copy.setPropTypes(this.getPropTypes());
    //        copy.setMapKeyType(this.getMapKeyType());
    //        copy.setMapValueType(this.getMapValueType());
    //        copy.setIgnoredPropNames(this.getIgnoredPropNames());
    //        copy.schema = this.schema;
    //
    //        return copy;
    //    }

    @Override
    public int hashCode() {
        int h = 17;
        h = 31 * h + N.hashCode(getIgnoredPropNames());
        h = 31 * h + N.hashCode(isIgnoreUnmatchedProperty());
        h = 31 * h + N.hashCode(getElementType());
        h = 31 * h + N.hashCode(getPropTypes());
        h = 31 * h + N.hashCode(getMapKeyType());
        h = 31 * h + N.hashCode(getMapValueType());
        h = 31 * h + N.hashCode(getIgnoredPropNames());
        return 31 * h + N.hashCode(schema);
    }

    /**
     *
     * @param obj
     * @return true, if successful
     */
    @SuppressFBWarnings
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof AvroDeserializationConfig other) {
            if (N.equals(getIgnoredPropNames(), other.getIgnoredPropNames()) && N.equals(isIgnoreUnmatchedProperty(), other.isIgnoreUnmatchedProperty())
                    && N.equals(getElementType(), other.getElementType()) && N.equals(getPropTypes(), other.getPropTypes())
                    && N.equals(getMapKeyType(), other.getMapKeyType()) && N.equals(getMapValueType(), other.getMapValueType())
                    && N.equals(schema, other.schema)) {

                return true;
            }
        }

        return false;
    }

    @Override
    public String toString() {
        return "{ignoredPropNames=" + N.toString(getIgnoredPropNames()) + ", ignoreUnmatchedProperty=" + N.toString(isIgnoreUnmatchedProperty())
                + ", elementType=" + N.toString(getElementType()) + ", propTypes=" + N.toString(getPropTypes()) + ", mapKeyType=" + N.toString(getMapKeyType())
                + ", mapValueType=" + N.toString(getMapValueType()) + ", ignoredPropNames=" + N.toString(getIgnoredPropNames()) + ", schema="
                + N.toString(schema) + "}";
    }

    /**
     * The Class ADC.
     */
    public static final class ADC extends AvroDeserializationConfig {

        /**
         *
         * @return
         */
        public static AvroDeserializationConfig create() {
            return new AvroDeserializationConfig();
        }

        /**
         *
         * @param elementClass
         * @return
         * @deprecated
         */
        @Deprecated
        public static AvroDeserializationConfig of(Class<?> elementClass) {
            return create().setElementType(elementClass);
        }

        /**
         *
         * @param schema
         * @return
         * @deprecated
         */
        @Deprecated
        public static AvroDeserializationConfig of(Schema schema) {
            return create().setSchema(schema);
        }

        /**
         *
         * @param elementClass
         * @param schema
         * @return
         * @deprecated
         */
        @Deprecated
        public static AvroDeserializationConfig of(Class<?> elementClass, Schema schema) {
            return create().setElementType(elementClass).setSchema(schema);
        }
    }
}
