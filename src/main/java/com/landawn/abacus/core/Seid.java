/*
 * Copyright (c) 2015, Haiyang Li.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.landawn.abacus.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import com.landawn.abacus.EntityId;
import com.landawn.abacus.annotation.Internal;
import com.landawn.abacus.parser.ParserUtil;
import com.landawn.abacus.parser.ParserUtil.EntityInfo;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Objectory;

/**
 *
 * @author Haiyang Li
 * @since 0.8
 */
public class Seid implements EntityId, Cloneable {

    private static final Comparator<String> keyComparator = new Comparator<String>() {
        @Override
        public int compare(String a, String b) {
            return N.compare(a, b);
        }
    };

    private final String entityName;

    private Map<String, Object> values = Collections.emptyMap();

    private String strValue;

    // for Kryo.
    protected Seid() {
        entityName = N.EMPTY_STRING;
    }

    @Deprecated
    @Internal
    public Seid(String entityName) {
        //    if (N.isNullOrEmpty(entityName)) {
        //        throw new IllegalArgumentException("Entity name can't be null or empty");
        //    }

        this.entityName = entityName == null ? N.EMPTY_STRING : entityName;
    }

    public Seid(String propName, Object propValue) {
        this(NameUtil.getParentName(propName));

        set(propName, propValue);
    }

    public Seid(Map<String, Object> nameValues) {
        this(NameUtil.getParentName(nameValues.keySet().iterator().next()));

        set(nameValues);
    }

    /**
     *
     * @param entityName
     * @return
     * @deprecated for internal use only
     */
    @Deprecated
    @Internal
    public static Seid of(String entityName) {
        return new Seid(entityName);
    }

    /**
     *
     * @param propName
     * @param propValue
     * @return
     */
    public static Seid of(String propName, Object propValue) {
        return new Seid(propName, propValue);
    }

    /**
     *
     * @param propName1
     * @param propValue1
     * @param propName2
     * @param propValue2
     * @return
     */
    public static Seid of(String propName1, Object propValue1, String propName2, Object propValue2) {
        final Seid result = new Seid(propName1, propValue1);
        result.set(propName2, propValue2);
        return result;
    }

    /**
     *
     * @param propName1
     * @param propValue1
     * @param propName2
     * @param propValue2
     * @param propName3
     * @param propValue3
     * @return
     */
    public static Seid of(String propName1, Object propValue1, String propName2, Object propValue2, String propName3, Object propValue3) {
        final Seid result = new Seid(propName1, propValue1);
        result.set(propName2, propValue2);
        result.set(propName3, propValue3);
        return result;
    }

    /**
     *
     * @param nameValues
     * @return
     * @deprecated replaced by {@link #from(Map)}
     */
    @Deprecated
    public static Seid of(Map<String, Object> nameValues) {
        return from(nameValues);
    }

    /**
     *
     * @param nameValues
     * @return
     */
    public static Seid from(Map<String, Object> nameValues) {
        return new Seid(nameValues);
    }

    /**
     *
     * @param entity
     * @return
     */
    public static Seid from(Object entity) {
        @SuppressWarnings("deprecation")
        final List<String> idPropNames = ClassUtil.getIdFieldNames(entity.getClass());

        if (N.isNullOrEmpty(idPropNames)) {
            throw new IllegalArgumentException("No id property defined in class: " + ClassUtil.getCanonicalClassName(entity.getClass()));
        }

        return from(entity, idPropNames);
    }

    /**
     *
     * @param entity
     * @param idPropNames
     * @return
     */
    public static Seid from(Object entity, Collection<String> idPropNames) {
        if (N.isNullOrEmpty(idPropNames)) {
            throw new IllegalArgumentException("Id property names can't be null or empty");
        }

        final Class<?> cls = entity.getClass();
        final EntityInfo entityInfo = ParserUtil.getEntityInfo(cls);
        final Seid seid = Seid.of(ClassUtil.getSimpleClassName(cls));

        for (String idPropName : idPropNames) {
            seid.set(idPropName, entityInfo.getPropInfo(idPropName).getPropValue(entity));
        }

        return seid;
    }

    @Override
    public String entityName() {
        return entityName;
    }

    /**
     *
     * @param <T>
     * @param propName
     * @return
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(String propName) {
        if (NameUtil.isCanonicalName(entityName, propName)) {
            return (T) values.get(NameUtil.getSimpleName(propName));
        } else {
            return (T) values.get(propName);
        }
    }

    @Override
    public int getInt(String propName) {
        final Object value = get(propName);
        return value instanceof Number ? ((Number) value).intValue() : N.convert(value, int.class);
    }

    @Override
    public long getLong(String propName) {
        final Object value = get(propName);
        return value instanceof Number ? ((Number) value).longValue() : N.convert(value, long.class);
    }

    /**
     *
     * @param <T>
     * @param clazz
     * @param propName
     * @return
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(Class<T> clazz, String propName) {
        Object propValue = get(propName);

        if (propValue == null) {
            propValue = N.defaultValueOf(clazz);
        }

        return N.convert(propValue, clazz);
    }

    /**
     *
     * @param propName
     * @param propValue
     * @return
     * @deprecated for internal use only
     */
    @Deprecated
    @Internal
    public Seid set(String propName, Object propValue) {
        final String simplePropName = NameUtil.isCanonicalName(entityName, propName) ? NameUtil.getSimpleName(propName) : propName;

        if (values.isEmpty() || (values.size() == 1 && values.containsKey(simplePropName))) {
            values = Collections.singletonMap(simplePropName, propValue);
        } else {
            final Map<String, Object> newVlaues = new TreeMap<>(keyComparator);
            newVlaues.putAll(this.values);
            this.values = newVlaues;

            values.put(simplePropName, propValue);
        }

        strValue = null;

        return this;
    }

    /**
     *
     * @param nameValues
     * @deprecated for internal use only
     */
    @Deprecated
    @Internal
    public void set(Map<String, Object> nameValues) {
        if (N.isNullOrEmpty(nameValues)) {
            return;
        } else if (nameValues.size() == 1) {
            final Map.Entry<String, Object> entry = nameValues.entrySet().iterator().next();
            set(entry.getKey(), entry.getValue());
        } else {
            if (!(values instanceof TreeMap)) {
                final Map<String, Object> newVlaues = new TreeMap<>(keyComparator);
                newVlaues.putAll(this.values);
                this.values = newVlaues;
            }

            for (Map.Entry<String, Object> entry : nameValues.entrySet()) {
                if (NameUtil.isCanonicalName(entityName, entry.getKey())) {
                    values.put(NameUtil.getSimpleName(entry.getKey()), entry.getValue());
                } else {
                    values.put(entry.getKey(), entry.getValue());
                }
            }
        }

        strValue = null;
    }

    /**
     *
     * @param propName
     * @return
     * @deprecated for internal use only
     */
    @Deprecated
    @Internal
    public Object remove(String propName) {
        if (values.size() == 0) {
            return null;
        }

        final String simplePropName = NameUtil.isCanonicalName(entityName, propName) ? NameUtil.getSimpleName(propName) : propName;
        Object result = null;

        if (values.size() == 1) {
            if (values.containsKey(simplePropName)) {
                result = values.values().iterator().next();
                values = Collections.emptyMap();
            }
        } else {
            result = values.remove(simplePropName);
        }

        strValue = null;

        return result;
    }

    /**
     * Removes the all.
     *
     * @param propNames
     * @deprecated for internal use only
     */
    @Deprecated
    @Internal
    public void removeAll(Collection<String> propNames) {
        for (String propName : propNames) {
            remove(propName);
        }

        strValue = null;
    }

    /**
     *
     * @param propName
     * @return true, if successful
     */
    @Override
    public boolean containsKey(String propName) {
        if (values.size() == 0) {
            return false;
        }

        if (NameUtil.isCanonicalName(entityName, propName)) {
            return values.containsKey(NameUtil.getSimpleName(propName));
        } else {
            return values.containsKey(propName);
        }
    }

    @Override
    public Set<String> keySet() {
        return values.keySet();
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        return values.entrySet();
    }

    @Override
    public int size() {
        return values.size();
    }

    /**
     * Checks if is empty.
     *
     * @return true, if is empty
     */
    @Override
    public boolean isEmpty() {
        return values.size() == 0;
    }

    /**
     * Clear.
     * @deprecated for internal use only
     */
    @Deprecated
    @Internal
    public void clear() {
        values = Collections.emptyMap();

        strValue = null;
    }

    /**
     *
     * @return
     * @deprecated for internal use only
     */
    @Deprecated
    @Internal
    public Seid copy() {
        final Seid copy = new Seid(entityName);

        copy.set(this.values);
        copy.strValue = strValue;

        return copy;
    }

    /**
     *
     * @return
     * @deprecated for internal use only
     */
    @Deprecated
    @Internal
    @Override
    public Seid clone() {
        return copy();
    }

    /**
     *
     * @param obj
     * @return true, if successful
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        return (obj instanceof EntityId) ? toString().equals(obj.toString()) : false;
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    @Override
    public String toString() {
        return stringValue();
    }

    private String stringValue() {
        if (strValue == null) {

            final Set<Map.Entry<String, Object>> entrySet = values.entrySet();

            switch (values.size()) {
                case 0: {
                    strValue = entityName + ": {}";

                    break;
                }

                case 1: {
                    Map.Entry<String, Object> entry = entrySet.iterator().next();
                    String propName = NameUtil.isCanonicalName(entityName, entry.getKey()) ? NameUtil.getSimpleName(entry.getKey()) : entry.getKey();

                    strValue = entityName + ": {" + propName + "=" + N.stringOf(entry.getValue()) + "}";

                    break;
                }

                case 2: {
                    Iterator<Map.Entry<String, Object>> it = entrySet.iterator();
                    Map.Entry<String, Object> entry1 = it.next();
                    String propName1 = NameUtil.isCanonicalName(entityName, entry1.getKey()) ? NameUtil.getSimpleName(entry1.getKey()) : entry1.getKey();
                    Map.Entry<String, Object> entry2 = it.next();
                    String propName2 = NameUtil.isCanonicalName(entityName, entry2.getKey()) ? NameUtil.getSimpleName(entry2.getKey()) : entry2.getKey();

                    strValue = entityName + ": {" + propName1 + "=" + N.stringOf(entry1.getValue()) + N.ELEMENT_SEPARATOR + propName2 + "="
                            + N.stringOf(entry2.getValue()) + "}";

                    break;
                }

                default: {

                    List<String> keys = new ArrayList<>(values.keySet());
                    N.sort(keys);

                    final StringBuilder sb = Objectory.createStringBuilder();

                    sb.append(entityName);
                    sb.append(": {");

                    int i = 0;

                    for (Map.Entry<String, Object> entry : entrySet) {
                        if (i++ > 0) {
                            sb.append(N.ELEMENT_SEPARATOR_CHAR_ARRAY);
                        }

                        String propName = NameUtil.isCanonicalName(entityName, entry.getKey()) ? NameUtil.getSimpleName(entry.getKey()) : entry.getKey();

                        sb.append(propName);
                        sb.append('=');
                        sb.append(N.stringOf(entry.getValue()));
                    }

                    sb.append('}');

                    strValue = sb.toString();

                    Objectory.recycle(sb);
                }
            }
        }

        return strValue;
    }
}
