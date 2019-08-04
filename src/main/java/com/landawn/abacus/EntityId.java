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

/**
 * Identity of an entity
 * 
 * @since 0.8
 * 
 * @author Haiyang Li
 */
public interface EntityId {

    public static EntityId of(String entityName) {
        return Seid.of(entityName);
    }

    public static EntityId of(String propName, Object propValue) {
        return Seid.of(propName, propValue);
    }

    public static EntityId of(String propName1, Object propValue1, String propName2, Object propValue2) {
        return Seid.of(propName1, propValue1, propName2, propValue2);
    }

    public static EntityId of(String propName1, Object propValue1, String propName2, Object propValue2, String propName3, Object propValue3) {
        return Seid.of(propName1, propValue1, propName2, propValue2, propName3, propValue3);
    }

    public static EntityId of(Map<String, Object> nameValues) {
        return Seid.of(nameValues);
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
     * @param propName
     * @return T
     * @throws NullPonterException
     *             if the property is null and {@code T} is primitive type.
     */
    <T> T get(String propName);

    /**
     * Method get.
     * 
     * @param clazz
     * @param propName
     * @return T
     */
    <T> T get(Class<T> clazz, String propName);

    /**
     * Method set.
     * 
     * @param propName
     * @param propValue
     * @return
     */
    EntityId set(String propName, Object propValue);

    /**
     * 
     * @param nameValues
     */
    void set(Map<String, Object> nameValues);

    /**
     * Method remove.
     * 
     * @param propName
     * @return
     */
    Object remove(String propName);

    /**
     * 
     * @param propNames
     */
    void removeAll(Collection<String> propNames);

    /**
     * 
     * @param containsKey
     * @return
     */
    boolean containsKey(String propName);

    /**
     * Method keySet.
     * 
     * @return Set<String>
     */
    Set<String> keySet();

    /**
     * 
     * @return
     */
    Set<Map.Entry<String, Object>> entrySet();

    int size();

    /**
     * 
     * @return
     */
    boolean isEmpty();

    /**
     */
    void clear();

    /**
     * 
     * @return EntityId
     */
    EntityId copy();
}
