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

import java.util.Collection;
import java.util.Map;

import org.apache.avro.Schema;

import com.landawn.abacus.annotation.SuppressFBWarnings;
import com.landawn.abacus.util.DateTimeFormat;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.NamingPolicy;

/**
 *
 * @author Haiyang Li
 * @since 0.8
 */
public class AvroSerializationConfig extends SerializationConfig<AvroSerializationConfig> {

    private Schema schema;

    public AvroSerializationConfig() {
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
    public AvroSerializationConfig setSchema(Schema schema) {
        this.schema = schema;

        return this;
    }

    /**
     * Gets the char quotation.
     *
     * @return
     * @deprecated UnsupportedOperationException
     */
    @Deprecated
    @Override
    public char getCharQuotation() {
        throw new UnsupportedOperationException();
    }

    /**
     * Sets the char quotation.
     *
     * @param charQuotation
     * @return
     * @deprecated UnsupportedOperationException
     */
    @Deprecated
    @Override
    public AvroSerializationConfig setCharQuotation(char charQuotation) {
        throw new UnsupportedOperationException();
    }

    /**
     * Gets the string quotation.
     *
     * @return
     * @deprecated UnsupportedOperationException
     */
    @Deprecated
    @Override
    public char getStringQuotation() {
        throw new UnsupportedOperationException();
    }

    /**
     * Sets the string quotation.
     *
     * @param stringQuotation
     * @return
     * @deprecated UnsupportedOperationException
     */
    @Deprecated
    @Override
    public AvroSerializationConfig setStringQuotation(char stringQuotation) {
        throw new UnsupportedOperationException();
    }

    /**
     * The default format is: <code>LONG</code>.
     *
     * @return
     * @deprecated UnsupportedOperationException
     */
    @Deprecated
    @Override
    public DateTimeFormat getDateTimeFormat() {
        throw new UnsupportedOperationException();
    }

    /**
     * Sets the date time format.
     *
     * @param dateTimeFormat
     * @return
     * @deprecated UnsupportedOperationException
     */
    @Deprecated
    @Override
    public AvroSerializationConfig setDateTimeFormat(DateTimeFormat dateTimeFormat) {
        throw new UnsupportedOperationException();
    }

    /**
     * Checks if is pretty format.
     *
     * @return true, if is pretty format
     * @deprecated UnsupportedOperationException
     */
    @Deprecated
    @Override
    public boolean isPrettyFormat() {
        throw new UnsupportedOperationException();
    }

    /**
     * Sets the pretty format.
     *
     * @param prettyFormat
     * @return
     * @deprecated UnsupportedOperationException
     */
    @Deprecated
    @Override
    public AvroSerializationConfig setPrettyFormat(boolean prettyFormat) {
        throw new UnsupportedOperationException();
    }

    /**
     * Gets the indentation.
     *
     * @return
     * @deprecated UnsupportedOperationException
     */
    @Deprecated
    @Override
    public String getIndentation() {
        throw new UnsupportedOperationException();
    }

    /**
     * Sets the indentation.
     *
     * @param indentation
     * @return
     * @deprecated UnsupportedOperationException
     */
    @Deprecated
    @Override
    public AvroSerializationConfig setIndentation(String indentation) {
        throw new UnsupportedOperationException();
    }

    /**
     * Gets the prop naming policy.
     *
     * @return
     * @deprecated UnsupportedOperationException
     */
    @Deprecated
    @Override
    public NamingPolicy getPropNamingPolicy() {
        throw new UnsupportedOperationException();
    }

    /**
     * Sets the prop naming policy.
     *
     * @param propNamingPolicy
     * @return
     * @deprecated UnsupportedOperationException
     */
    @Deprecated
    @Override
    public AvroSerializationConfig setPropNamingPolicy(NamingPolicy propNamingPolicy) {
        throw new UnsupportedOperationException();
    }

    /**
     * Support circular reference.
     *
     * @return true, if successful
     * @deprecated UnsupportedOperationException
     */
    @Deprecated
    @Override
    public boolean supportCircularReference() {
        throw new UnsupportedOperationException();
    }

    /**
     * Support circular reference.
     *
     * @param supportCircularReference
     * @return
     * @deprecated UnsupportedOperationException
     */
    @Deprecated
    @Override
    public AvroSerializationConfig supportCircularReference(boolean supportCircularReference) {
        throw new UnsupportedOperationException();
    }

    /**
     *
     * @return
     * @deprecated UnsupportedOperationException
     */
    @Deprecated
    @Override
    public boolean writeBigDecimalAsPlain() {
        throw new UnsupportedOperationException();
    }

    /**
     *
     * @param writeBigDecimalAsPlain
     * @return
     * @deprecated UnsupportedOperationException
     */
    @Deprecated
    @Override
    public AvroSerializationConfig writeBigDecimalAsPlain(boolean writeBigDecimalAsPlain) {
        throw new UnsupportedOperationException();
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
        h = 31 * h + N.hashCode(isSkipTransientField());
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

        if (obj instanceof AvroSerializationConfig other) {
            if (N.equals(getIgnoredPropNames(), other.getIgnoredPropNames()) && N.equals(getExclusion(), other.getExclusion())
                    && N.equals(isSkipTransientField(), other.isSkipTransientField()) && N.equals(getIgnoredPropNames(), other.getIgnoredPropNames())
                    && N.equals(schema, other.schema)) {

                return true;
            }
        }

        return false;
    }

    @Override
    public String toString() {
        return "{ignoredPropNames=" + N.toString(getIgnoredPropNames()) + ", exclusion=" + N.toString(getExclusion()) + ", skipTransientField="
                + N.toString(isSkipTransientField()) + ", ignoredPropNames=" + N.toString(getIgnoredPropNames()) + ", schema=" + N.toString(schema) + "}";
    }

    /**
     * The Class ASC.
     */
    public static final class ASC extends AvroSerializationConfig {

        /**
         *
         * @return
         */
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
        public static AvroSerializationConfig of(Schema schema) {
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
        public static AvroSerializationConfig of(Schema schema, Exclusion exclusion, Map<Class<?>, Collection<String>> ignoredPropNames) {
            return create().setSchema(schema).setExclusion(exclusion).setIgnoredPropNames(ignoredPropNames);
        }
    }
}
