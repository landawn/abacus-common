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
     *
     * @param propName property name with entity name, for example {@code Account.id}
     * @param propValue
     * @return
     */
    public static EntityId of(String propName, Object propValue) {
        return Seid.of(propName, propValue);
    }

    /**
     *
     * @param entityName
     * @param propName
     * @param propValue
     * @return
     */
    @SuppressWarnings("deprecation")
    public static EntityId of(String entityName, String propName, Object propValue) {
        return Seid.of(entityName).set(propName, propValue);
    }

    /**
     *
     * @param propName1 property name with entity name, for example {@code Account.id}
     * @param propValue1
     * @param propName2
     * @param propValue2
     * @return
     */
    public static EntityId of(String propName1, Object propValue1, String propName2, Object propValue2) {
        return Seid.of(propName1, propValue1, propName2, propValue2);
    }

    /**
     *
     * @param entityName
     * @param propName1
     * @param propValue1
     * @param propName2
     * @param propValue2
     * @return
     */
    @SuppressWarnings("deprecation")
    public static EntityId of(String entityName, String propName1, Object propValue1, String propName2, Object propValue2) {
        return Seid.of(entityName).set(propName1, propValue1).set(propName2, propValue2);
    }

    /**
     *
     * @param propName1 property name with entity name, for example {@code Account.id}
     * @param propValue1
     * @param propName2
     * @param propValue2
     * @param propName3
     * @param propValue3
     * @return
     */
    public static EntityId of(String propName1, Object propValue1, String propName2, Object propValue2, String propName3, Object propValue3) {
        return Seid.of(propName1, propValue1, propName2, propValue2, propName3, propValue3);
    }

    /**
     *
     * @param entityName
     * @param propName1
     * @param propValue1
     * @param propName2
     * @param propValue2
     * @param propName3
     * @param propValue3
     * @return
     */
    @SuppressWarnings("deprecation")
    public static EntityId of(String entityName, String propName1, Object propValue1, String propName2, Object propValue2, String propName3,
            Object propValue3) {
        return Seid.of(entityName).set(propName1, propValue1).set(propName2, propValue2).set(propName3, propValue3);
    }

    /**
     *
     * @param nameValues
     * @return
     */
    public static EntityId from(Map<String, Object> nameValues) {
        return Seid.from(nameValues);
    }

    /**
     *
     * @param entityName
     * @param nameValues
     * @return
     */
    @SuppressWarnings("deprecation")
    public static EntityId from(String entityName, Map<String, Object> nameValues) {
        final Seid seid = Seid.of(entityName);
        seid.set(nameValues);
        return seid;
    }

    /**
     *
     * @param entity
     * @return
     */
    public static EntityId from(Object entity) {
        return Seid.from(entity);
    }

    /**
     *
     * @param entity
     * @param idPropNames
     * @return
     */
    public static EntityId from(Object entity, Collection<String> idPropNames) {
        return Seid.from(entity, idPropNames);
    }

    /**
     *
     * @return String
     */
    String entityName();

    /**
     *
     * @param <T>
     * @param propName
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
     *
     * @param <T>
     * @param clazz
     * @param propName
     * @return T
     */
    <T> T get(Class<T> clazz, String propName);

    /**
     *
     * @param propName
     * @return
     */
    boolean containsKey(String propName);

    /**
     *
     * @return Set<String>
     */
    Set<String> keySet();

    /**
     *
     * @return
     */
    Set<Map.Entry<String, Object>> entrySet();

    /**
     *
     * @return
     */
    int size();

    /**
     *
     * @return
     */
    boolean isEmpty();
}
