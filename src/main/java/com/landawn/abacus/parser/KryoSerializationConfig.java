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

public class KryoSerializationConfig extends SerializationConfig<KryoSerializationConfig> {

    protected static final boolean defaultWriteClass = false;

    private boolean writeClass = defaultWriteClass;

    public KryoSerializationConfig() { //NOSONAR
    }

    /**
     * Checks if class name should be written.
     *
     * @return {@code true}, if class name should be written
     */
    public boolean writeClass() {
        return writeClass;
    }

    /**
     * Sets the write class.
     *
     * @param writeClass
     * @return
     */
    public KryoSerializationConfig writeClass(final boolean writeClass) {
        this.writeClass = writeClass;

        return this;
    }

    //    /**
    //     *
    //     * @return
    //     */
    //    @Override
    //    public KryoSerializationConfig copy() {
    //        final KryoSerializationConfig copy = new KryoSerializationConfig();
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
    //        copy.writeClass = this.writeClass;
    //
    //        return copy;
    //    }

    @Override
    public int hashCode() {
        int h = 17;
        h = 31 * h + N.hashCode(getIgnoredPropNames());
        h = 31 * h + N.hashCode(getExclusion());
        h = 31 * h + N.hashCode(skipTransientField());
        return 31 * h + N.hashCode(writeClass);
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

        if (obj instanceof KryoSerializationConfig other) { //NOSONAR
            return N.equals(getIgnoredPropNames(), other.getIgnoredPropNames()) && N.equals(getExclusion(), other.getExclusion()) //NOSONAR
                    && N.equals(skipTransientField(), other.skipTransientField()) && N.equals(writeClass, other.writeClass);
        }

        return false;
    }

    @Override
    public String toString() {
        return "{ignoredPropNames=" + N.toString(getIgnoredPropNames()) + ", exclusion=" + N.toString(getExclusion()) + ", skipTransientField="
                + N.toString(skipTransientField()) + ", writeClass=" + N.toString(writeClass) + "}";
    }

    /**
     * The Class KSC.
     */
    public static final class KSC extends KryoSerializationConfig {

        public static KryoSerializationConfig create() {
            return new KryoSerializationConfig();
        }

        /**
         *
         * @param writeClass
         * @return
         * @deprecated to be removed in future version.
         */
        @Deprecated
        public static KryoSerializationConfig of(final boolean writeClass) {
            return create().writeClass(writeClass);
        }

        /**
         *
         * @param exclusion
         * @param ignoredPropNames
         * @return
         * @deprecated to be removed in future version.
         */
        @Deprecated
        public static KryoSerializationConfig of(final Exclusion exclusion, final Map<Class<?>, Set<String>> ignoredPropNames) {
            return create().setExclusion(exclusion).setIgnoredPropNames(ignoredPropNames);
        }

        /**
         *
         * @param writeClass
         * @param exclusion
         * @param ignoredPropNames
         * @return
         * @deprecated to be removed in future version.
         */
        @Deprecated
        public static KryoSerializationConfig of(final boolean writeClass, final Exclusion exclusion, final Map<Class<?>, Set<String>> ignoredPropNames) {
            return create().writeClass(writeClass).setExclusion(exclusion).setIgnoredPropNames(ignoredPropNames);
        }
    }
}
