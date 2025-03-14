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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.landawn.abacus.annotation.JsonXmlField;
import com.landawn.abacus.annotation.MayReturnNull;

/**
 *
 * @param <C>
 * @see JsonXmlField
 */
public abstract class ParserConfig<C extends ParserConfig<C>> implements Cloneable {

    Map<Class<?>, Set<String>> ignoredBeanPropNameMap = null;

    /**
     * Gets the ignored prop names.
     *
     * @return
     */
    public Map<Class<?>, Set<String>> getIgnoredPropNames() {
        return ignoredBeanPropNameMap;
    }

    /**
     * Gets the ignored prop names.
     *
     * @param cls
     * @return
     */
    @MayReturnNull
    public Collection<String> getIgnoredPropNames(final Class<?> cls) {
        if (ignoredBeanPropNameMap == null) {
            return null; // NOSONAR
        }

        Collection<String> result = ignoredBeanPropNameMap.get(cls);

        if (result == null) {
            result = ignoredBeanPropNameMap.get(Object.class);
        }

        return result;
    }

    /**
     * Sets the ignored prop names.
     *
     * @param ignoredPropNames
     * @return
     */
    public C setIgnoredPropNames(final Set<String> ignoredPropNames) {
        return setIgnoredPropNames(Object.class, ignoredPropNames);
    }

    /**
     * Sets the ignored prop names.
     *
     * @param cls
     * @param ignoredPropNames
     * @return
     */
    public C setIgnoredPropNames(final Class<?> cls, final Set<String> ignoredPropNames) {
        if (ignoredBeanPropNameMap == null) {
            ignoredBeanPropNameMap = new HashMap<>();
        }

        ignoredBeanPropNameMap.put(cls, ignoredPropNames);

        return (C) this;
    }

    /**
     * Sets the ignored prop names.
     *
     * @param ignoredPropNames
     * @return
     */
    public C setIgnoredPropNames(final Map<Class<?>, Set<String>> ignoredPropNames) {
        ignoredBeanPropNameMap = ignoredPropNames;

        return (C) this;
    }

    public C copy() {
        try {
            return (C) super.clone();
        } catch (final CloneNotSupportedException e) {
            throw new RuntimeException(e); // should never happen.
        }
    }
}
