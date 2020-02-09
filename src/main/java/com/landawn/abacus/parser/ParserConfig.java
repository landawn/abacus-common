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
import java.util.HashMap;
import java.util.Map;

import com.landawn.abacus.annotation.JsonXmlField;

// TODO: Auto-generated Javadoc
/**
 * The Class ParserConfig.
 *
 * @author Haiyang Li
 * @param <C>
 * @see JsonXmlField
 * @since 0.8
 */
public abstract class ParserConfig<C extends ParserConfig<C>> {

    /** The ignored prop names. */
    Map<Class<?>, Collection<String>> ignoredPropNames = null;

    /**
     * Gets the ignored prop names.
     *
     * @return
     */
    public Map<Class<?>, Collection<String>> getIgnoredPropNames() {
        return ignoredPropNames;
    }

    /**
     * Gets the ignored prop names.
     *
     * @param cls
     * @return
     */
    public Collection<String> getIgnoredPropNames(Class<?> cls) {
        if (this.ignoredPropNames == null) {
            return null;
        }

        Collection<String> result = this.ignoredPropNames.get(cls);

        if (result == null) {
            result = this.ignoredPropNames.get(Object.class);
        }

        return result;
    }

    /**
     * Sets the ignored prop names.
     *
     * @param ignoredPropNames
     * @return
     */
    public C setIgnoredPropNames(Collection<String> ignoredPropNames) {
        return setIgnoredPropNames(Object.class, ignoredPropNames);
    }

    /**
     * Sets the ignored prop names.
     *
     * @param cls
     * @param ignoredPropNames
     * @return
     */
    public C setIgnoredPropNames(Class<?> cls, Collection<String> ignoredPropNames) {
        if (this.ignoredPropNames == null) {
            this.ignoredPropNames = new HashMap<>();
        }

        this.ignoredPropNames.put(cls, ignoredPropNames);

        return (C) this;
    }

    /**
     * Sets the ignored prop names.
     *
     * @param ignoredPropNames
     * @return
     */
    public C setIgnoredPropNames(Map<Class<?>, Collection<String>> ignoredPropNames) {
        this.ignoredPropNames = ignoredPropNames;

        return (C) this;
    }

    /**
     *
     * @return
     */
    public abstract C copy();
}
