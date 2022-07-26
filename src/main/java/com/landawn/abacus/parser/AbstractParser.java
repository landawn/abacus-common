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

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;

import com.landawn.abacus.exception.ParseException;
import com.landawn.abacus.logging.Logger;
import com.landawn.abacus.logging.LoggerFactory;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.type.TypeFactory;
import com.landawn.abacus.util.BiMap;
import com.landawn.abacus.util.ImmutableBiMap;
import com.landawn.abacus.util.ImmutableCollection;
import com.landawn.abacus.util.ImmutableList;
import com.landawn.abacus.util.ImmutableMap;
import com.landawn.abacus.util.ImmutableNavigableSet;
import com.landawn.abacus.util.ImmutableSet;
import com.landawn.abacus.util.ImmutableSortedMap;
import com.landawn.abacus.util.ImmutableSortedSet;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Tuple;
import com.landawn.abacus.util.Tuple.Tuple2;

/**
 *
 * @author Haiyang Li
 * @param <SC>
 * @param <DC>
 * @since 0.8
 */
abstract class AbstractParser<SC extends SerializationConfig<?>, DC extends DeserializationConfig<?>> implements Parser<SC, DC> {

    private static final Logger logger = LoggerFactory.getLogger(AbstractParser.class);

    static final String ELEMENT_SEPARATOR = ", ".intern();

    static final char[] ELEMENT_SEPARATOR_CHAR_ARRAY = ELEMENT_SEPARATOR.toCharArray();

    static final String NULL_STRING = "null".intern();

    static final char[] NULL_CHAR_ARRAY = NULL_STRING.toCharArray();

    static final String TRUE = Boolean.TRUE.toString().intern();

    static final char[] TRUE_CHAR_ARRAY = TRUE.toCharArray();

    static final String FALSE = Boolean.FALSE.toString().intern();

    static final char[] FALSE_CHAR_ARRAY = FALSE.toCharArray();

    // ... it has to be big enough to make it's safety to add element to
    // ArrayBlockingQueue.
    static final int POOL_SIZE = 1000;

    static final int CLS_POOL_SIZE = 3000;

    static final Type<Object> objType = TypeFactory.getType(Object.class);

    static final Type<String> strType = TypeFactory.getType(String.class);

    static final Type<Boolean> boolType = TypeFactory.getType(Boolean.class);

    static final Type<Map<String, Object>> propsMapType = TypeFactory.getType("Map<String, Object>");

    /**
     *
     * @param obj
     * @return
     */
    @Override
    public String serialize(Object obj) {
        return serialize(obj, null);
    }

    /**
     *
     * @param file
     * @param obj
     */
    @Override
    public void serialize(File file, Object obj) {
        serialize(file, obj, null);
    }

    /**
     *
     * @param os
     * @param obj
     */
    @Override
    public void serialize(OutputStream os, Object obj) {
        serialize(os, obj, null);
    }

    /**
     *
     * @param writer
     * @param obj
     */
    @Override
    public void serialize(Writer writer, Object obj) {
        serialize(writer, obj, null);
    }

    /**
     *
     * @param <T>
     * @param targetClass
     * @param st
     * @return
     */
    @Override
    public <T> T deserialize(Class<T> targetClass, String st) {
        return deserialize(targetClass, st, null);
    }

    /**
     *
     * @param <T>
     * @param targetClass
     * @param file
     * @return
     */
    @Override
    public <T> T deserialize(Class<T> targetClass, File file) {
        return deserialize(targetClass, file, null);
    }

    /**
     *
     * @param <T>
     * @param targetClass
     * @param is
     * @return
     */
    @Override
    public <T> T deserialize(Class<T> targetClass, InputStream is) {
        return deserialize(targetClass, is, null);
    }

    /**
     *
     * @param <T>
     * @param targetClass
     * @param reader
     * @return
     */
    @Override
    public <T> T deserialize(Class<T> targetClass, Reader reader) {
        return deserialize(targetClass, reader, null);
    }

    static final Map<Class<?>, Tuple2<Function<Class<?>, Object>, Function<Object, Object>>> mapOfCreatorAndConvertorForTargetType = new HashMap<>();

    static {
        mapOfCreatorAndConvertorForTargetType.put(ImmutableList.class,
                Tuple.<Function<Class<?>, Object>, Function<Object, Object>> of(t -> new ArrayList<>(), new Function<Object, Object>() {
                    @SuppressWarnings("rawtypes")
                    @Override
                    public Object apply(Object t) {
                        return ImmutableList.of((List) t);
                    }
                }));

        mapOfCreatorAndConvertorForTargetType.put(ImmutableSet.class,
                Tuple.<Function<Class<?>, Object>, Function<Object, Object>> of(t -> new HashSet<>(), new Function<Object, Object>() {
                    @SuppressWarnings("rawtypes")
                    @Override
                    public Object apply(Object t) {
                        return ImmutableSet.of((Set) t);
                    }
                }));

        mapOfCreatorAndConvertorForTargetType.put(ImmutableSortedSet.class,
                Tuple.<Function<Class<?>, Object>, Function<Object, Object>> of(t -> new TreeSet<>(), new Function<Object, Object>() {
                    @SuppressWarnings("rawtypes")
                    @Override
                    public Object apply(Object t) {
                        return ImmutableSortedSet.of((SortedSet) t);
                    }
                }));

        mapOfCreatorAndConvertorForTargetType.put(ImmutableNavigableSet.class,
                Tuple.<Function<Class<?>, Object>, Function<Object, Object>> of(t -> new TreeSet<>(), new Function<Object, Object>() {
                    @SuppressWarnings("rawtypes")
                    @Override
                    public Object apply(Object t) {
                        return ImmutableNavigableSet.of((NavigableSet) t);
                    }
                }));

        mapOfCreatorAndConvertorForTargetType.put(ImmutableCollection.class, mapOfCreatorAndConvertorForTargetType.get(ImmutableList.class));

        mapOfCreatorAndConvertorForTargetType.put(ImmutableMap.class,
                Tuple.<Function<Class<?>, Object>, Function<Object, Object>> of(t -> new HashMap<>(), new Function<Object, Object>() {
                    @SuppressWarnings("rawtypes")
                    @Override
                    public Object apply(Object t) {
                        return ImmutableMap.of((Map) t);
                    }
                }));

        mapOfCreatorAndConvertorForTargetType.put(ImmutableBiMap.class,
                Tuple.<Function<Class<?>, Object>, Function<Object, Object>> of(t -> new BiMap<>(), new Function<Object, Object>() {
                    @SuppressWarnings("rawtypes")
                    @Override
                    public Object apply(Object t) {
                        return ImmutableBiMap.of((BiMap) t);
                    }
                }));

        mapOfCreatorAndConvertorForTargetType.put(ImmutableSortedMap.class,
                Tuple.<Function<Class<?>, Object>, Function<Object, Object>> of(t -> new TreeMap<>(), new Function<Object, Object>() {
                    @SuppressWarnings("rawtypes")
                    @Override
                    public Object apply(Object t) {
                        return ImmutableSortedMap.of((SortedMap) t);
                    }
                }));

        mapOfCreatorAndConvertorForTargetType.put(Object.class, Tuple.<Function<Class<?>, Object>, Function<Object, Object>> of(t -> N.newInstance(t), t -> t));
    }

    protected static Class<?> choosePropClass(Class<?> propClass, Class<?> attribeTypeClass) {
        if ((attribeTypeClass != null) && ((propClass == null) || propClass.isAssignableFrom(attribeTypeClass))) {
            return attribeTypeClass;
        }

        return propClass;
    }

    protected static Tuple2<Function<Class<?>, Object>, Function<Object, Object>> getCreatorAndConvertorForTargetType(Class<?> propClass,
            Class<?> attribeTypeClass) {
        final Class<?> t = choosePropClass(propClass, attribeTypeClass);

        Tuple2<Function<Class<?>, Object>, Function<Object, Object>> result = mapOfCreatorAndConvertorForTargetType.get(t);

        if (result == null) {
            result = mapOfCreatorAndConvertorForTargetType.get(Object.class);
        }

        return result;
    }

    /**
     * New prop instance.
     *
     * @param <T>
     * @param propClass
     * @param attribeTypeClass
     * @return
     */
    @SuppressWarnings("unchecked")
    protected static <T> T newPropInstance(Class<?> propClass, Class<?> attribeTypeClass) {
        if ((attribeTypeClass != null) && ((propClass == null) || propClass.isAssignableFrom(attribeTypeClass))) {
            try {
                return (T) N.newInstance(attribeTypeClass);
            } catch (Exception e) {
                if (logger.isInfoEnabled()) {
                    logger.info("Failed to new instance by type attribute: " + attribeTypeClass.getCanonicalName());
                }
            }
        }

        if (propClass != null) {
            return (T) N.newInstance(propClass);
        }

        throw new ParseException("Failed to create property instance with property class: " + propClass + " and attribute " + attribeTypeClass);
    }

    /**
     * Gets the concrete class.
     *
     * @param targetClass
     * @param typeClass
     * @return
     */
    protected static Class<?> getConcreteClass(Class<?> targetClass, Class<?> typeClass) {
        if (typeClass == null) {
            return targetClass;
        } else if (targetClass == null) {
            return typeClass;
        } else if (targetClass == typeClass || targetClass.isAssignableFrom(typeClass)) {
            return typeClass;
        } else {
            return targetClass;
        }
    }

    /**
     * Collection 2 array.
     *
     * @param <T>
     * @param targetClass
     * @param c
     * @return
     */
    protected static <T> T collection2Array(Class<?> targetClass, final Collection<?> c) {
        if (c == null) {
            return null;
        }

        final Type<?> targetType = N.typeOf(targetClass);

        if (targetType.isPrimitiveArray()) {
        } else {
            // looking for the right array class.
            for (Object e : c) {
                if (e != null) {
                    if (targetClass.getComponentType().isAssignableFrom(e.getClass())) {
                        return (T) targetType.collection2Array(c);
                    } else {
                        return (T) c.toArray((Object[]) N.newArray(e.getClass(), c.size()));
                    }
                }
            }
        }

        return (T) targetType.collection2Array(c);
    }
}
