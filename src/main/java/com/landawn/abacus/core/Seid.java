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
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Objectory;

// TODO: Auto-generated Javadoc
/**
 * The Class Seid.
 *
 * @author Haiyang Li
 * @since 0.8
 */
public class Seid implements EntityId, Cloneable {

    /** The Constant keyComparator. */
    private static final Comparator<String> keyComparator = new Comparator<String>() {
        @Override
        public int compare(String a, String b) {
            return N.compare(a, b);
        }
    };

    /** The entity name. */
    private final String entityName;

    /** The values. */
    private Map<String, Object> values = Collections.emptyMap();

    /** The str value. */
    private String strValue;

    /**
     * Instantiates a new seid.
     */
    // for Kryo.
    protected Seid() {
        entityName = N.EMPTY_STRING;
    }

    /**
     * Instantiates a new seid.
     *
     * @param entityName the entity name
     * @deprecated for internal use only
     */
    @Deprecated
    @Internal
    public Seid(String entityName) {
        if (N.isNullOrEmpty(entityName)) {
            throw new IllegalArgumentException("Entity name can't be null or empty");
        }

        this.entityName = entityName;
    }

    /**
     * Instantiates a new seid.
     *
     * @param propName the prop name
     * @param propValue the prop value
     */
    public Seid(String propName, Object propValue) {
        this(NameUtil.getParentName(propName));

        set(propName, propValue);
    }

    /**
     * Instantiates a new seid.
     *
     * @param nameValues the name values
     */
    public Seid(Map<String, Object> nameValues) {
        this(NameUtil.getParentName(nameValues.keySet().iterator().next()));

        set(nameValues);
    }

    /**
     * Of.
     *
     * @param entityName the entity name
     * @return the seid
     * @deprecated for internal use only
     */
    @Deprecated
    @Internal
    public static Seid of(String entityName) {
        return new Seid(entityName);
    }

    /**
     * Of.
     *
     * @param propName the prop name
     * @param propValue the prop value
     * @return the seid
     */
    public static Seid of(String propName, Object propValue) {
        return new Seid(propName, propValue);
    }

    /**
     * Of.
     *
     * @param propName1 the prop name 1
     * @param propValue1 the prop value 1
     * @param propName2 the prop name 2
     * @param propValue2 the prop value 2
     * @return the seid
     */
    public static Seid of(String propName1, Object propValue1, String propName2, Object propValue2) {
        final Seid result = new Seid(propName1, propValue1);
        result.set(propName2, propValue2);
        return result;
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
     * @return the seid
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
     * @param nameValues the name values
     * @return the seid
     */
    public static Seid from(Map<String, Object> nameValues) {
        return new Seid(nameValues);
    }

    /**
     * Entity name.
     *
     * @return the string
     */
    @Override
    public String entityName() {
        return entityName;
    }

    /**
     * Gets the.
     *
     * @param <T> the generic type
     * @param propName the prop name
     * @return the t
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
     * Gets the.
     *
     * @param <T> the generic type
     * @param clazz the clazz
     * @param propName the prop name
     * @return the t
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
     * Sets the.
     *
     * @param propName the prop name
     * @param propValue the prop value
     * @return the entity id
     * @deprecated for internal use only
     */
    @Deprecated
    @Internal
    public Seid set(String propName, Object propValue) {
        final String simplePropName = NameUtil.isCanonicalName(entityName, propName) ? NameUtil.getSimpleName(propName) : propName;

        if (values.isEmpty() || (values.size() == 1 && values.containsKey(simplePropName))) {
            values = Collections.singletonMap(simplePropName, propValue);
        } else {
            final Map<String, Object> newVlaues = new TreeMap<String, Object>(keyComparator);
            newVlaues.putAll(this.values);
            this.values = newVlaues;

            values.put(simplePropName, propValue);
        }

        strValue = null;

        return this;
    }

    /**
     * Sets the.
     *
     * @param nameValues the name values
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
                final Map<String, Object> newVlaues = new TreeMap<String, Object>(keyComparator);
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
     * Removes the.
     *
     * @param propName the prop name
     * @return the object
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
     * @param propNames the prop names
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
     * Contains key.
     *
     * @param propName the prop name
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

    /**
     * Key set.
     *
     * @return the sets the
     */
    @Override
    public Set<String> keySet() {
        return values.keySet();
    }

    /**
     * Entry set.
     *
     * @return the sets the
     */
    @Override
    public Set<Entry<String, Object>> entrySet() {
        return values.entrySet();
    }

    /**
     * Size.
     *
     * @return the int
     */
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
     * Copy.
     *
     * @return the seid
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
     * Clone.
     *
     * @return the seid
     * @deprecated for internal use only
     */
    @Deprecated
    @Internal
    @Override
    public Seid clone() {
        return copy();
    }

    /**
     * Equals.
     *
     * @param obj the obj
     * @return true, if successful
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        return (obj instanceof EntityId) ? toString().equals(obj.toString()) : false;
    }

    /**
     * Hash code.
     *
     * @return the int
     */
    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    /**
     * To string.
     *
     * @return the string
     */
    @Override
    public String toString() {
        return stringValue();
    }

    /**
     * String value.
     *
     * @return the string
     */
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

                    strValue = entityName + ": {" + propName1 + "=" + N.stringOf(entry1.getValue()) + Type.ELEMENT_SEPARATOR + propName2 + "="
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
                            sb.append(Type.ELEMENT_SEPARATOR_CHAR_ARRAY);
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
