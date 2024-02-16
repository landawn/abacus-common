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
import java.util.Set;

import com.landawn.abacus.annotation.JsonXmlField;
import com.landawn.abacus.annotation.MayReturnNull;

/**
 *
 * @author Haiyang Li
 * @param <C>
 * @see JsonXmlField
 * @since 0.8
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
     * @return {@code null} if {@code (this.beanIgnoredPropNameMap == null)}. (auto-generated java doc for return)
     */
    @MayReturnNull
    public Collection<String> getIgnoredPropNames(Class<?> cls) {
        if (this.ignoredBeanPropNameMap == null) {
            return null; // NOSONAR
        }

        Collection<String> result = this.ignoredBeanPropNameMap.get(cls);

        if (result == null) {
            result = this.ignoredBeanPropNameMap.get(Object.class);
        }

        return result;
    }

    /**
     * Sets the ignored prop names.
     *
     * @param ignoredPropNames
     * @return
     */
    public C setIgnoredPropNames(Set<String> ignoredPropNames) {
        return setIgnoredPropNames(Object.class, ignoredPropNames);
    }

    /**
     * Sets the ignored prop names.
     *
     * @param cls
     * @param ignoredPropNames
     * @return
     */
    public C setIgnoredPropNames(Class<?> cls, Set<String> ignoredPropNames) {
        if (this.ignoredBeanPropNameMap == null) {
            this.ignoredBeanPropNameMap = new HashMap<>();
        }

        this.ignoredBeanPropNameMap.put(cls, ignoredPropNames);

        return (C) this;
    }

    /**
     * Sets the ignored prop names.
     *
     * @param ignoredPropNames
     * @return
     */
    public C setIgnoredPropNames(Map<Class<?>, Set<String>> ignoredPropNames) {
        this.ignoredBeanPropNameMap = ignoredPropNames;

        return (C) this;
    }

    /**
     *
     *
     * @return
     */
    public C copy() {
        try {
            return (C) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e); // should never happen.
        }
    }
}
