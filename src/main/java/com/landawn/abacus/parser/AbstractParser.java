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
import java.util.LinkedHashMap;
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
import com.landawn.abacus.util.MapEntity;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Tuple;
import com.landawn.abacus.util.Tuple.Tuple2;
import com.landawn.abacus.util.WD;

/**
 * Abstract base class providing common functionality for parser implementations.
 * This class implements the {@link Parser} interface and provides default implementations
 * and utility methods for serialization and deserialization operations.
 *
 * <p>Key responsibilities:</p>
 * <ul>
 *   <li>Provides default method implementations that delegate to configuration-based variants</li>
 *   <li>Manages common constants and type definitions used across parsers</li>
 *   <li>Handles circular reference detection during serialization</li>
 *   <li>Provides utilities for instance creation and type conversion</li>
 * </ul>
 *
 * <p>This class serves as the foundation for specific parser implementations such as
 * JSON parsers and XML parsers, providing shared infrastructure and behavior.</p>
 *
 * @param <SC> the serialization configuration type
 * @param <DC> the deserialization configuration type
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

    static final Type<Boolean> boolType = TypeFactory.getType(Boolean.class);

    static final Type<String> strType = TypeFactory.getType(String.class);

    static final Type<String> strArrayType = TypeFactory.getType(String[].class);

    @SuppressWarnings("rawtypes")
    static final Type<List> listType = TypeFactory.getType(List.class);

    @SuppressWarnings("rawtypes")
    static final Type<Map> mapType = TypeFactory.getType(Map.class);

    @SuppressWarnings("rawtypes")
    static final Type<Map> linkedHashMapType = TypeFactory.getType(LinkedHashMap.class);

    static final Type<MapEntity> mapEntityType = TypeFactory.getType(MapEntity.class);

    static final Type<Map<String, Object>> propsMapType = TypeFactory.getType("Map<String, Object>");

    @Override
    public String serialize(final Object obj) {
        return serialize(obj, (SC) null);
    }

    @Override
    public void serialize(final Object obj, final File output) {
        serialize(obj, null, output);
    }

    @Override
    public void serialize(final Object obj, final OutputStream output) {
        serialize(obj, null, output);
    }

    @Override
    public void serialize(final Object obj, final Writer output) {
        serialize(obj, null, output);
    }

    @Override
    public <T> T deserialize(final String source, final Type<? extends T> targetType) {
        return deserialize(source, null, targetType);
    }

    @Override
    public <T> T deserialize(final String source, final Class<? extends T> targetType) {
        return deserialize(source, null, targetType);
    }

    @Override
    public <T> T deserialize(final File source, final Type<? extends T> targetType) {
        return deserialize(source, null, targetType);
    }

    @Override
    public <T> T deserialize(final File source, final Class<? extends T> targetType) {
        return deserialize(source, null, targetType);
    }

    @Override
    public <T> T deserialize(final InputStream source, final Type<? extends T> targetType) {
        return deserialize(source, null, targetType);
    }

    @Override
    public <T> T deserialize(final InputStream source, final Class<? extends T> targetType) {
        return deserialize(source, null, targetType);
    }

    @Override
    public <T> T deserialize(final Reader source, final Type<? extends T> targetType) {
        return deserialize(source, null, targetType);
    }

    @Override
    public <T> T deserialize(final Reader source, final Class<? extends T> targetType) {
        return deserialize(source, null, targetType);
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

    /**
     * Chooses the appropriate property class based on declared type and attribute type information.
     *
     * <p>This method is used during deserialization to determine the actual class to instantiate
     * when both a declared property type and an attribute-specified type are available. It follows
     * these rules:</p>
     * <ul>
     *   <li>If attribute type is provided and compatible with property type, use attribute type</li>
     *   <li>If attribute type is incompatible or null, use property type</li>
     *   <li>This enables proper handling of polymorphic deserialization</li>
     * </ul>
     *
     * @param propClass the declared property class from the target bean (may be {@code null})
     * @param attribeTypeClass the type specified by a type attribute in the serialized data (may be {@code null})
     * @return the class to use for instantiation, preferring the attribute type when compatible
     */
    protected static Class<?> choosePropClass(final Class<?> propClass, final Class<?> attribeTypeClass) {
        if ((attribeTypeClass != null) && ((propClass == null) || propClass.isAssignableFrom(attribeTypeClass))) {
            return attribeTypeClass;
        }

        return propClass;
    }

    /**
     * Gets the creator function and converter function for a target type.
     *
     * <p>This method is used for deserializing immutable collection types and other special types
     * that require a two-phase instantiation process:</p>
     * <ol>
     *   <li>Creator function: Creates a mutable intermediate instance (e.g., ArrayList for ImmutableList)</li>
     *   <li>Converter function: Converts the intermediate instance to the final type (e.g., wraps ArrayList in ImmutableList)</li>
     * </ol>
     *
     * <p>For standard types, the creator creates the instance and the converter is a no-op.</p>
     *
     * @param propClass the declared property class from the target bean (may be {@code null})
     * @param attribeTypeClass the type specified by a type attribute in the serialized data (may be {@code null})
     * @return a tuple containing the creator function and converter function for the target type
     */
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
     * Creates a new instance of a property based on declared and attribute type information.
     *
     * <p>This method attempts to instantiate the most specific type available, with the following priority:</p>
     * <ol>
     *   <li>If attribute type is provided and compatible, instantiate attribute type</li>
     *   <li>If attribute type instantiation fails or is incompatible, fall back to property class</li>
     *   <li>If both fail or are null, throw ParseException</li>
     * </ol>
     *
     * <p>This enables polymorphic deserialization where the serialized data contains type information
     * that specifies a more concrete type than the declared property type.</p>
     *
     * @param <T> the return type
     * @param propClass the declared property class from the target bean (may be {@code null})
     * @param attrTypeClass the type specified by a type attribute in the serialized data (may be {@code null})
     * @return a new instance of the appropriate type
     * @throws ParseException if no suitable type is available or instantiation fails
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
     * Determines the concrete class to use for instantiation based on target and type information.
     *
     * <p>This method resolves which class should be used when both a target class and a type class
     * are available, following these rules:</p>
     * <ul>
     *   <li>If type class is null, use target class</li>
     *   <li>If target class is null or the same as type class, use type class</li>
     *   <li>If type class is assignable to target class, use type class (more specific)</li>
     *   <li>Otherwise, use target class (type class is incompatible)</li>
     * </ul>
     * @param typeClass the type class from serialized type information (may be {@code null})
     * @param targetClass the target class from the deserialization context (may be {@code null}) 
     * @return the concrete class to instantiate
     */
    protected static Class<?> getConcreteClass(final Class<?> typeClass, final Class<?> targetClass) {
        if (typeClass == null) {
            return targetClass;
        } else if ((targetClass == null) || (targetClass == typeClass || targetClass.isAssignableFrom(typeClass))) {
            return typeClass;
        } else {
            return targetClass;
        }
    }

    /**
     * Converts a collection to an array of the specified target type.
     *
     * <p>This method handles the conversion of collections to arrays during deserialization,
     * with special handling for:</p>
     * <ul>
     *   <li>Primitive arrays: Uses the target type's conversion method</li>
     *   <li>Object arrays: Attempts to determine the actual element type from collection contents</li>
     *   <li>Empty collections: Returns an empty array of the target type</li>
     * </ul>
     *
     * <p>The method inspects the first non-null element to determine if a more specific
     * array type should be used than the declared target type.</p>
     *
     * @param <T> the return type
     * @param c the collection to convert (may be {@code null})
     * @param targetType the target array type
     * @return an array containing the collection elements, or {@code null} if the collection is {@code null}
     */
    protected static <T> T collection2Array(final Collection<?> c, final Type<?> targetType) {
        if (c == null) {
            return null;
        }

        //noinspection StatementWithEmptyBody
        if (targetType.isPrimitiveArray()) {
            // continue
        } else {
            // looking for the right array class.
            for (final Object e : c) {
                if (e != null) {
                    if (targetType.getElementType().clazz().isAssignableFrom(e.getClass())) {
                        return (T) targetType.collection2Array(c);
                    } else {
                        return (T) c.toArray((Object[]) N.newArray(e.getClass(), c.size()));
                    }
                }
            }
        }

        return (T) targetType.collection2Array(c);
    }

    /**
     * Creates a new file if it does not already exist.
     *
     * <p>This utility method is used by serialization methods that write to files.
     * It ensures the target file exists before attempting to write to it.</p>
     *
     * <p>Note: This method does not create parent directories. Parent directories
     * must already exist or the file creation will fail.</p>
     *
     * @param file the file to create if it doesn't exist (must not be {@code null})
     * @throws IOException if the file cannot be created or if parent directories don't exist
     */
    protected static void createNewFileIfNotExists(final File file) throws IOException {
        if (!file.exists() && !IOUtil.createFileIfNotExists(file)) {
            throw new IOException("Failed to create new file: " + file.getName());
        }
    }

    /**
     * Determines the effective exclusion strategy to use for serialization.
     *
     * <p>This method resolves the exclusion strategy by checking multiple sources in order of priority:</p>
     * <ol>
     *   <li>Configuration-level exclusion (highest priority)</li>
     *   <li>Bean-level exclusion from {@code @JsonXmlConfig} annotation</li>
     *   <li>Default exclusion of {@code Exclusion.NULL} (lowest priority)</li>
     * </ol>
     *
     * <p>This allows per-call configuration to override bean-level settings, which in turn
     * override the default behavior.</p>
     *
     * @param config the serialization configuration (may be {@code null})
     * @param beanInfo the bean metadata containing annotation-based exclusion settings
     * @return the effective exclusion strategy to use
     */
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
     */
    protected static boolean hasCircularReference(final Object obj, final IdentityHashSet<Object> serializedObjects, final JSONXMLSerializationConfig<?> config,
            @SuppressWarnings("unused") final CharacterWriter bw) {
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
