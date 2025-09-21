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

import java.io.File;
import java.io.IOException;
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
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;

import com.landawn.abacus.annotation.SuppressFBWarnings;
import com.landawn.abacus.exception.ParseException;
import com.landawn.abacus.logging.Logger;
import com.landawn.abacus.logging.LoggerFactory;
import com.landawn.abacus.parser.ParserUtil.BeanInfo;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.type.TypeFactory;
import com.landawn.abacus.util.BiMap;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.IdentityHashSet;
import com.landawn.abacus.util.ImmutableBiMap;
import com.landawn.abacus.util.ImmutableCollection;
import com.landawn.abacus.util.ImmutableList;
import com.landawn.abacus.util.ImmutableMap;
import com.landawn.abacus.util.ImmutableNavigableMap;
import com.landawn.abacus.util.ImmutableNavigableSet;
import com.landawn.abacus.util.ImmutableSet;
import com.landawn.abacus.util.ImmutableSortedMap;
import com.landawn.abacus.util.ImmutableSortedSet;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Tuple;
import com.landawn.abacus.util.Tuple.Tuple2;
import com.landawn.abacus.util.WD;

/**
 *
 * @param <SC>
 * @param <DC>
 */
abstract class AbstractParser<SC extends SerializationConfig<?>, DC extends DeserializationConfig<?>> implements Parser<SC, DC> {

    private static final Logger logger = LoggerFactory.getLogger(AbstractParser.class);

    static final char _COLON = WD._COLON;

    static final String COLON_SPACE = WD.COLON_SPACE;

    static final char[] COLON_SPACE_CHAR_ARRAY = COLON_SPACE.toCharArray();

    static final char _COMMA = WD._COMMA;

    static final String COMMA_SPACE = WD.COMMA_SPACE;

    static final char[] COMMA_SPACE_CHAR_ARRAY = COMMA_SPACE.toCharArray();

    static final String NULL_STRING = "null";

    static final char[] NULL_CHAR_ARRAY = NULL_STRING.toCharArray();

    static final String TRUE = Boolean.TRUE.toString().intern();

    static final char[] TRUE_CHAR_ARRAY = TRUE.toCharArray();

    static final String FALSE = Boolean.FALSE.toString().intern();

    static final char[] FALSE_CHAR_ARRAY = FALSE.toCharArray();

    // ... it has to be big enough to make it's safety to add an element to ArrayBlockingQueue.
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
    public String serialize(final Object obj) {
        return serialize(obj, (SC) null);
    }

    /**
     *
     * @param obj
     * @param output
     */
    @Override
    public void serialize(final Object obj, final File output) {
        serialize(obj, null, output);
    }

    /**
     *
     * @param obj
     * @param output
     */
    @Override
    public void serialize(final Object obj, final OutputStream output) {
        serialize(obj, null, output);
    }

    /**
     *
     * @param obj
     * @param output
     */
    @Override
    public void serialize(final Object obj, final Writer output) {
        serialize(obj, null, output);
    }

    /**
     *
     * @param <T>
     * @param source
     * @param targetClass
     * @return
     */
    @Override
    public <T> T deserialize(final String source, final Class<? extends T> targetClass) {
        return deserialize(source, null, targetClass);
    }

    /**
     *
     * @param <T>
     * @param source
     * @param targetClass
     * @return
     */
    @Override
    public <T> T deserialize(final File source, final Class<? extends T> targetClass) {
        return deserialize(source, null, targetClass);
    }

    /**
     *
     * @param <T>
     * @param source
     * @param targetClass
     * @return
     */
    @Override
    public <T> T deserialize(final InputStream source, final Class<? extends T> targetClass) {
        return deserialize(source, null, targetClass);
    }

    /**
     *
     * @param <T>
     * @param source
     * @param targetClass
     * @return
     */
    @Override
    public <T> T deserialize(final Reader source, final Class<? extends T> targetClass) {
        return deserialize(source, null, targetClass);
    }

    static final Map<Class<?>, Tuple2<Function<Class<?>, Object>, Function<Object, Object>>> mapOfCreatorAndConvertorForTargetType = new HashMap<>();

    static {
        mapOfCreatorAndConvertorForTargetType.put(ImmutableList.class, Tuple.of(t -> new ArrayList<>(), t -> ImmutableList.wrap((List<?>) t)));

        mapOfCreatorAndConvertorForTargetType.put(ImmutableSet.class, Tuple.of(t -> new HashSet<>(), t -> ImmutableSet.wrap((Set<?>) t)));

        mapOfCreatorAndConvertorForTargetType.put(ImmutableSortedSet.class, Tuple.of(t -> new TreeSet<>(), t -> ImmutableSortedSet.wrap((SortedSet<?>) t)));

        mapOfCreatorAndConvertorForTargetType.put(ImmutableNavigableSet.class,
                Tuple.of(t -> new TreeSet<>(), t -> ImmutableNavigableSet.wrap((NavigableSet<?>) t)));

        mapOfCreatorAndConvertorForTargetType.put(ImmutableCollection.class, mapOfCreatorAndConvertorForTargetType.get(ImmutableList.class));

        mapOfCreatorAndConvertorForTargetType.put(ImmutableMap.class, Tuple.of(t -> new HashMap<>(), t -> ImmutableMap.wrap((Map<?, ?>) t)));

        mapOfCreatorAndConvertorForTargetType.put(ImmutableBiMap.class, Tuple.of(t -> new BiMap<>(), t -> ImmutableBiMap.wrap((BiMap<?, ?>) t)));

        mapOfCreatorAndConvertorForTargetType.put(ImmutableSortedMap.class, Tuple.of(t -> new TreeMap<>(), t -> ImmutableSortedMap.wrap((SortedMap<?, ?>) t)));

        mapOfCreatorAndConvertorForTargetType.put(ImmutableNavigableMap.class,
                Tuple.of(t -> new TreeMap<>(), t -> ImmutableNavigableMap.wrap((NavigableMap<?, ?>) t)));

        mapOfCreatorAndConvertorForTargetType.put(Object.class, Tuple.of(N::newInstance, t -> t));
    }

    protected static Class<?> choosePropClass(final Class<?> propClass, final Class<?> attribeTypeClass) {
        if ((attribeTypeClass != null) && ((propClass == null) || propClass.isAssignableFrom(attribeTypeClass))) {
            return attribeTypeClass;
        }

        return propClass;
    }

    protected static Tuple2<Function<Class<?>, Object>, Function<Object, Object>> getCreatorAndConvertorForTargetType(final Class<?> propClass,
            final Class<?> attribeTypeClass) {
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
     * @param attrTypeClass
     * @return
     */
    @SuppressFBWarnings("NP_LOAD_OF_KNOWN_NULL_VALUE")
    @SuppressWarnings("unchecked")
    protected static <T> T newPropInstance(final Class<?> propClass, final Class<?> attrTypeClass) {
        if ((attrTypeClass != null) && ((propClass == null) || propClass.isAssignableFrom(attrTypeClass))) {
            try {
                return (T) N.newInstance(attrTypeClass);
            } catch (final Exception e) {
                if (logger.isWarnEnabled()) {
                    logger.warn("Failed to new instance by type attribute: " + attrTypeClass.getCanonicalName());
                }
            }
        }

        if (propClass != null) {
            return (T) N.newInstance(propClass);
        }

        throw new ParseException("Failed to create property instance with property class by attribute " + attrTypeClass);
    }

    /**
     * Gets the concrete class.
     *
     * @param targetClass
     * @param typeClass
     * @return
     */
    protected static Class<?> getConcreteClass(final Class<?> targetClass, final Class<?> typeClass) {
        if (typeClass == null) {
            return targetClass;
        } else if ((targetClass == null) || (targetClass == typeClass || targetClass.isAssignableFrom(typeClass))) {
            return typeClass;
        } else {
            return targetClass;
        }
    }

    /**
     * Collection 2 array.
     * @param c
     * @param targetClass
     *
     * @param <T>
     * @return
     */
    protected static <T> T collection2Array(final Collection<?> c, final Class<?> targetClass) {
        if (c == null) {
            return null;
        }

        final Type<?> targetType = N.typeOf(targetClass);

        //noinspection StatementWithEmptyBody
        if (targetType.isPrimitiveArray()) {
            // continue
        } else {
            // looking for the right array class.
            for (final Object e : c) {
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

    protected static void createNewFileIfNotExists(final File file) throws IOException {
        if (!file.exists() && !IOUtil.createFileIfNotExists(file)) {
            throw new IOException("Failed to create new file: " + file.getName());
        }
    }

    protected static Exclusion getExclusion(final SerializationConfig<?> config, final BeanInfo beanInfo) {
        return config.getExclusion() == null ? (beanInfo.jsonXmlSeriExclusion == null ? Exclusion.NULL : beanInfo.jsonXmlSeriExclusion) : config.getExclusion();
    }

    /**
     * Checks if an object has already been serialized (circular reference detection).
     * 
     * @param obj the object to check
     * @param serializedObjects set of already serialized objects
     * @param config the serialization configuration
     * @param bw the XML writer
     * @return {@code true} if circular reference was found and handled, {@code false} otherwise
     * @throws IOException if an I/O error occurs
     */
    protected static boolean hasCircularReference(final Object obj, final IdentityHashSet<Object> serializedObjects, final JSONXMLSerializationConfig<?> config,
            @SuppressWarnings("unused") final CharacterWriter bw) throws IOException {
        final Type<?> type = obj == null ? null : Type.of(obj.getClass());
        if (obj != null && serializedObjects != null //
                && (type.isBean() || type.isMap() || type.isCollection() || type.isObjectArray() || type.isMapEntity())) {
            if (serializedObjects.contains(obj)) {
                if (config == null || !config.supportCircularReference()) {
                    throw new ParseException("Self reference found in obj: " + ClassUtil.getClassName(obj.getClass()));
                }

                // bw.write("null");

                return true;
            } else {
                serializedObjects.add(obj);
            }
        }

        return false;
    }
}
