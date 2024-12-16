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

import com.landawn.abacus.util.DateTimeFormat;
import com.landawn.abacus.util.N;

/**
 *
 * @param <C>
 */
public abstract class SerializationConfig<C extends SerializationConfig<C>> extends ParserConfig<C> {

    protected static final Exclusion defaultExclusion = null;

    protected static final DateTimeFormat defaultDateTimeFormat = DateTimeFormat.LONG;

    protected static final boolean defaultSkipTransientField = true;

    Exclusion exclusion = defaultExclusion;

    boolean skipTransientField = defaultSkipTransientField;

    /**
     * Gets the exclusion.
     *
     * @return
     */
    public Exclusion getExclusion() {
        return exclusion;
    }

    /**
     * Sets the exclusion for field in bean.
     *
     * @param exclusion
     * @return
     */
    public C setExclusion(final Exclusion exclusion) {
        this.exclusion = exclusion;

        return (C) this;
    }

    /**
     * Checks if is skip transient field.
     *
     * @return {@code true}, if is skip transient field
     */
    public boolean skipTransientField() {
        return skipTransientField;
    }

    /**
     * Sets the skip transient field.
     *
     * @param skipTransientField
     * @return
     */
    public C skipTransientField(final boolean skipTransientField) {
        this.skipTransientField = skipTransientField;

        return (C) this;
    }

    @Override
    public int hashCode() {
        int h = 17;
        h = 31 * h + N.hashCode(getIgnoredPropNames());
        h = 31 * h + N.hashCode(exclusion);
        h = 31 * h + N.hashCode(skipTransientField);
        return 31 * h + N.hashCode(skipTransientField);
    }

    /**
     *
     * @param obj
     * @return {@code true}, if successful
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

    @Override
    public String toString() {
        return "{ignoredPropNames=" + N.toString(getIgnoredPropNames()) + ", exclusion=" + N.toString(exclusion) + ", skipTransientField="
                + N.toString(skipTransientField) + "}";
    }
}
