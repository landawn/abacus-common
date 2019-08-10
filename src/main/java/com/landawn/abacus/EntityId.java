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

package com.landawn.abacus;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import com.landawn.abacus.core.Seid;

// TODO: Auto-generated Javadoc
/**
 * Identity of an entity.
 *
 * @author Haiyang Li
 * @since 0.8
 */
public interface EntityId {

    /**
     * Of.
     *
     * @param entityName the entity name
     * @return the entity id
     */
    public static EntityId of(String entityName) {
        return Seid.of(entityName);
    }

    /**
     * Of.
     *
     * @param propName the prop name
     * @param propValue the prop value
     * @return the entity id
     */
    public static EntityId of(String propName, Object propValue) {
        return Seid.of(propName, propValue);
    }

    /**
     * Of.
     *
     * @param propName1 the prop name 1
     * @param propValue1 the prop value 1
     * @param propName2 the prop name 2
     * @param propValue2 the prop value 2
     * @return the entity id
     */
    public static EntityId of(String propName1, Object propValue1, String propName2, Object propValue2) {
        return Seid.of(propName1, propValue1, propName2, propValue2);
    }

    /**
     * Of.
     *
     * @param propName1 the prop name 1
     * @param propValue1 the prop value 1
     * @param propName2 the prop name 2
     * @param propValue2 the prop value 2
     * @param propName3 the prop name 3
     * @param propValue3 the prop value 3
     * @return the entity id
     */
    public static EntityId of(String propName1, Object propValue1, String propName2, Object propValue2, String propName3, Object propValue3) {
        return Seid.of(propName1, propValue1, propName2, propValue2, propName3, propValue3);
    }

    /** 
     *
     * @param nameValues the name values
     * @return the entity id
     * @deprecated replaced by {@link #from(Map)}
     */
    @Deprecated
    public static EntityId of(Map<String, Object> nameValues) {
        return Seid.of(nameValues);
    }

    /** 
     *
     * @param nameValues the name values
     * @return the entity id
     */
    public static EntityId from(Map<String, Object> nameValues) {
        return Seid.from(nameValues);
    }

    /**
     * Method entityName.
     * 
     * @return String
     */
    String entityName();

    /**
     * Method get.
     *
     * @param <T> the generic type
     * @param propName the prop name
     * @return T
     */
    <T> T get(String propName);

    /**
     * 
     * @param propName
     * @return
     */
    int getInt(String propName);

    /**
     * 
     * @param propName
     * @return
     */
    long getLong(String propName);

    /**
     * Method get.
     *
     * @param <T> the generic type
     * @param clazz the clazz
     * @param propName the prop name
     * @return T
     */
    <T> T get(Class<T> clazz, String propName);

    /**
     * Method set.
     *
     * @param propName the prop name
     * @param propValue the prop value
     * @return the entity id
     */
    EntityId set(String propName, Object propValue);

    /**
     * Sets the.
     *
     * @param nameValues the name values
     */
    void set(Map<String, Object> nameValues);

    /**
     * Method remove.
     *
     * @param propName the prop name
     * @return the object
     */
    Object remove(String propName);

    /**
     * Removes the all.
     *
     * @param propNames the prop names
     */
    void removeAll(Collection<String> propNames);

    /**
     * Contains key.
     *
     * @param propName the prop name
     * @return true, if successful
     */
    boolean containsKey(String propName);

    /**
     * Method keySet.
     * 
     * @return Set<String>
     */
    Set<String> keySet();

    /**
     * Entry set.
     *
     * @return the sets the
     */
    Set<Map.Entry<String, Object>> entrySet();

    /**
     * Size.
     *
     * @return the int
     */
    int size();

    /**
     * Checks if is empty.
     *
     * @return true, if is empty
     */
    boolean isEmpty();

    /**
     * Clear.
     */
    void clear();

    /**
     * Copy.
     *
     * @return EntityId
     */
    EntityId copy();
}
