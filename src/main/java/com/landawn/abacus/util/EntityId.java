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

package com.landawn.abacus.util;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Identity of an entity.
 *
 */
public interface EntityId {

    /**
     *
     * @param propName property name with entity name, for example {@code Account.id}
     * @param propValue
     * @return
     */
    static EntityId of(final String propName, final Object propValue) {
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
    static EntityId of(final String entityName, final String propName, final Object propValue) {
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
    static EntityId of(final String propName1, final Object propValue1, final String propName2, final Object propValue2) {
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
    static EntityId of(final String entityName, final String propName1, final Object propValue1, final String propName2, final Object propValue2) {
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
    static EntityId of(final String propName1, final Object propValue1, final String propName2, final Object propValue2, final String propName3,
            final Object propValue3) {
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
    static EntityId of(final String entityName, final String propName1, final Object propValue1, final String propName2, final Object propValue2,
            final String propName3, final Object propValue3) {
        return Seid.of(entityName).set(propName1, propValue1).set(propName2, propValue2).set(propName3, propValue3);
    }

    /**
     *
     * @param nameValues
     * @return
     */
    static EntityId create(final Map<String, Object> nameValues) {
        return Seid.create(nameValues);
    }

    /**
     *
     * @param entityName
     * @param nameValues
     * @return
     */
    @SuppressWarnings("deprecation")
    static EntityId create(final String entityName, final Map<String, Object> nameValues) {
        final Seid seid = Seid.of(entityName);
        seid.set(nameValues);
        return seid;
    }

    /**
     *
     * @param entity
     * @return
     */
    static EntityId create(final Object entity) {
        return Seid.create(entity);
    }

    /**
     *
     * @param entity
     * @param idPropNames
     * @return
     */
    static EntityId create(final Object entity, final Collection<String> idPropNames) {
        return Seid.create(entity, idPropNames);
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
     * @param propName
     * @param targetType
     * @return T
     */
    <T> T get(String propName, Class<? extends T> targetType);

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
     *
     * @return
     */
    Set<Map.Entry<String, Object>> entrySet();

    /**
     *
     *
     * @return
     */
    int size();

    /**
     *
     *
     * @return
     */
    boolean isEmpty();

    /**
     *
     *
     * @return
     */
    static EntityIdBuilder builder() {
        return new EntityIdBuilder();
    }

    /**
     *
     *
     * @param entityName
     * @return
     */
    static EntityIdBuilder builder(final String entityName) {
        return new EntityIdBuilder(entityName);
    }

    public static class EntityIdBuilder {
        private Seid entityId = null;

        EntityIdBuilder() {
        }

        @SuppressWarnings("deprecation")
        EntityIdBuilder(final String entityName) {
            entityId = new Seid(entityName);
        }

        /**
         *
         *
         * @param idPropName
         * @param idPropVal
         * @return
         */
        @SuppressWarnings("deprecation")
        public EntityIdBuilder put(final String idPropName, final Object idPropVal) {
            if (entityId == null) {
                entityId = new Seid(idPropName, idPropVal);
            } else {
                entityId.set(idPropName, idPropVal);
            }

            return this;
        }

        /**
         *
         *
         * @return
         */
        @SuppressWarnings("deprecation")
        public EntityId build() {
            if (entityId == null) {
                entityId = new Seid(Strings.EMPTY_STRING);
            }

            return entityId;
        }
    }
}
