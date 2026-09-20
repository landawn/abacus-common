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
import com.landawn.abacus.exception.ParsingException;
import com.landawn.abacus.exception.UncheckedIOException;
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
import com.landawn.abacus.util.cs;
import com.landawn.abacus.util.SK;
import com.landawn.abacus.util.Tuple;
import com.landawn.abacus.util.Tuple.Tuple2;

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

    static final char _COLON = SK._COLON;

    static final String COLON_SPACE = SK.COLON_SPACE;

    static final char[] COLON_SPACE_CHAR_ARRAY = COLON_SPACE.toCharArray();

    static final char _COMMA = SK._COMMA;

    static final String COMMA_SPACE = SK.COMMA_SPACE;

    static final char[] COMMA_SPACE_CHAR_ARRAY = COMMA_SPACE.toCharArray();

    static final String NULL_STRING = "null";

    static final char[] NULL_CHAR_ARRAY = NULL_STRING.toCharArray();

    static final String TRUE = Boolean.TRUE.toString().intern();

    static final char[] TRUE_CHAR_ARRAY = TRUE.toCharArray();

    static final String FALSE = Boolean.FALSE.toString().intern();

    static final char[] FALSE_CHAR_ARRAY = FALSE.toCharArray();

    // Keep this large enough that adding an element to an ArrayBlockingQueue is safe.
    static final int POOL_SIZE = 1000;

    static final int CLS_POOL_SIZE = 3000;

    static final Type<Object> objType = TypeFactory.getType(Object.class);

    static final Type<Boolean> boolType = TypeFactory.getType(Boolean.class);

    static final Type<String> strType = TypeFactory.getType(String.class);

    static final Type<String[]> strArrayType = TypeFactory.getType(String[].class);

    @SuppressWarnings("rawtypes")
    static final Type<List> listType = TypeFactory.getType(List.class);

    @SuppressWarnings("rawtypes")
    static final Type<Map> mapType = TypeFactory.getType(Map.class);

    @SuppressWarnings("rawtypes")
    static final Type<Map> linkedHashMapType = TypeFactory.getType(LinkedHashMap.class);

    static final Type<MapEntity> mapEntityType = TypeFactory.getType(MapEntity.class);

    static final Type<Map<String, Object>> propsMapType = TypeFactory.getType("Map<String, Object>");

    /**
     * {@inheritDoc}
     */
    @Override
    public String serialize(final Object obj) {
        return serialize(obj, (SC) null);
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException if {@code output} is {@code null}.
     * @throws UncheckedIOException if creating or opening the destination file, writing the serialized data, or finishing its output
     *         fails
     */
    @Override
    public void serialize(final Object obj, final File output) throws IllegalArgumentException, UncheckedIOException {
        serialize(obj, null, output);
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException if {@code output} is {@code null}.
     * @throws UncheckedIOException if writing the serialized data to {@code output} or flushing buffered output fails
     */
    @Override
    public void serialize(final Object obj, final OutputStream output) throws IllegalArgumentException, UncheckedIOException {
        serialize(obj, null, output);
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException if {@code output} is {@code null}.
     * @throws UncheckedIOException if writing the serialized data to {@code output} or flushing buffered output fails
     */
    @Override
    public void serialize(final Object obj, final Writer output) throws IllegalArgumentException, UncheckedIOException {
        serialize(obj, null, output);
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException if {@code targetType} is {@code null}, or if {@code source} is {@code null} and the
     *         concrete parser rejects it (the Avro and Kryo parsers do; the JSON and XML parsers return the target type's
     *         default value for a {@code null} source instead)
     */
    @Override
    public <T> T deserialize(final String source, final Type<? extends T> targetType) throws IllegalArgumentException {
        return deserialize(source, null, targetType);
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException if {@code targetType} is {@code null}, or if {@code source} is {@code null} and the
     *         concrete parser rejects it (the Avro and Kryo parsers do; the JSON and XML parsers return the target type's
     *         default value for a {@code null} source instead)
     */
    @Override
    public <T> T deserialize(final String source, final Class<? extends T> targetType) throws IllegalArgumentException {
        return deserialize(source, null, targetType);
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException if {@code source} or {@code targetType} is {@code null}, or {@code source} is a directory.
     * @throws UncheckedIOException if opening or reading {@code source} fails, including a missing file, or an implementation cannot
     *         close its owned file resource
     */
    @Override
    public <T> T deserialize(final File source, final Type<? extends T> targetType) throws IllegalArgumentException, UncheckedIOException {
        return deserialize(source, null, targetType);
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException if {@code source} or {@code targetType} is {@code null}, or {@code source} is a directory.
     * @throws UncheckedIOException if opening or reading {@code source} fails, including a missing file, or an implementation cannot
     *         close its owned file resource
     */
    @Override
    public <T> T deserialize(final File source, final Class<? extends T> targetType) throws IllegalArgumentException, UncheckedIOException {
        return deserialize(source, null, targetType);
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException if {@code source} or {@code targetType} is {@code null}.
     * @throws UncheckedIOException if reading the serialized data from {@code source} fails
     */
    @Override
    public <T> T deserialize(final InputStream source, final Type<? extends T> targetType) throws IllegalArgumentException, UncheckedIOException {
        return deserialize(source, null, targetType);
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException if {@code source} or {@code targetType} is {@code null}.
     * @throws UncheckedIOException if reading the serialized data from {@code source} fails
     */
    @Override
    public <T> T deserialize(final InputStream source, final Class<? extends T> targetType) throws IllegalArgumentException, UncheckedIOException {
        return deserialize(source, null, targetType);
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException if {@code source} or {@code targetType} is {@code null}.
     * @throws UncheckedIOException if reading the serialized data from {@code source} fails
     */
    @Override
    public <T> T deserialize(final Reader source, final Type<? extends T> targetType) throws IllegalArgumentException, UncheckedIOException {
        return deserialize(source, null, targetType);
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException if {@code source} or {@code targetType} is {@code null}.
     * @throws UncheckedIOException if reading the serialized data from {@code source} fails
     */
    @Override
    public <T> T deserialize(final Reader source, final Class<? extends T> targetType) throws IllegalArgumentException, UncheckedIOException {
        return deserialize(source, null, targetType);
    }

    static final Map<Class<?>, Tuple2<Function<Class<?>, Object>, Function<Object, Object>>> mapOfCreatorAndConverterForTargetType = new HashMap<>();

    static {
        mapOfCreatorAndConverterForTargetType.put(ImmutableList.class, Tuple.of(t -> new ArrayList<>(), t -> ImmutableList.wrap((List<?>) t)));

        mapOfCreatorAndConverterForTargetType.put(ImmutableSet.class, Tuple.of(t -> new HashSet<>(), t -> ImmutableSet.wrap((Set<?>) t)));

        mapOfCreatorAndConverterForTargetType.put(ImmutableSortedSet.class, Tuple.of(t -> new TreeSet<>(), t -> ImmutableSortedSet.wrap((SortedSet<?>) t)));

        mapOfCreatorAndConverterForTargetType.put(ImmutableNavigableSet.class,
                Tuple.of(t -> new TreeSet<>(), t -> ImmutableNavigableSet.wrap((NavigableSet<?>) t)));

        mapOfCreatorAndConverterForTargetType.put(ImmutableCollection.class, mapOfCreatorAndConverterForTargetType.get(ImmutableList.class));

        mapOfCreatorAndConverterForTargetType.put(ImmutableMap.class, Tuple.of(t -> new HashMap<>(), t -> ImmutableMap.wrap((Map<?, ?>) t)));

        mapOfCreatorAndConverterForTargetType.put(ImmutableBiMap.class, Tuple.of(t -> new BiMap<>(), t -> ImmutableBiMap.wrap((BiMap<?, ?>) t)));

        mapOfCreatorAndConverterForTargetType.put(ImmutableSortedMap.class, Tuple.of(t -> new TreeMap<>(), t -> ImmutableSortedMap.wrap((SortedMap<?, ?>) t)));

        mapOfCreatorAndConverterForTargetType.put(ImmutableNavigableMap.class,
                Tuple.of(t -> new TreeMap<>(), t -> ImmutableNavigableMap.wrap((NavigableMap<?, ?>) t)));

        mapOfCreatorAndConverterForTargetType.put(Object.class, Tuple.of(N::newInstance, t -> t));
    }

    /**
     * Chooses the appropriate property class based on declared type and attribute type information.
     *
     * <p>This method is used during deserialization to determine the actual class to instantiate
     * when both a declared property type and an attribute-specified type are available. It follows
     * these rules:</p>
     * <ul>
     *   <li>If attribute type is provided and compatible with property type, use attribute type</li>
     *   <li>If attribute type is incompatible or {@code null}, use property type</li>
     *   <li>This enables proper handling of polymorphic deserialization</li>
     * </ul>
     *
     * @param propClass the declared property class from the target bean (may be {@code null})
     * @param attributeTypeClass the type specified by a type attribute in the serialized data (may be {@code null})
     * @return the class to use for instantiation, preferring the attribute type when compatible
     */
    protected static Class<?> choosePropClass(final Class<?> propClass, final Class<?> attributeTypeClass) {
        if ((attributeTypeClass != null) && ((propClass == null) || propClass.isAssignableFrom(attributeTypeClass))) {
            return attributeTypeClass;
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
     * @param attributeTypeClass the type specified by a type attribute in the serialized data (may be {@code null})
     * @return a tuple containing the creator function and converter function for the target type
     */
    protected static Tuple2<Function<Class<?>, Object>, Function<Object, Object>> getCreatorAndConverterForTargetType(final Class<?> propClass,
            final Class<?> attributeTypeClass) {
        final Class<?> t = choosePropClass(propClass, attributeTypeClass);

        Tuple2<Function<Class<?>, Object>, Function<Object, Object>> result = mapOfCreatorAndConverterForTargetType.get(t);

        if (result == null) {
            result = mapOfCreatorAndConverterForTargetType.get(Object.class);
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
     *   <li>If no property class remains after the attribute attempt, throw ParsingException;
     *       failures instantiating a non-null property class propagate unchanged</li>
     * </ol>
     *
     * <p>This enables polymorphic deserialization where the serialized data contains type information
     * that specifies a more concrete type than the declared property type.</p>
     *
     * @param <T> the return type
     * @param propClass the declared property class from the target bean (may be {@code null})
     * @param attributeTypeClass the type specified by a type attribute in the serialized data (may be {@code null})
     * @return a new instance of the appropriate type
     * @throws ParsingException if no property class is available and the type-attribute class is missing, incompatible, or cannot be instantiated
     */
    @SuppressFBWarnings("NP_LOAD_OF_KNOWN_NULL_VALUE")
    @SuppressWarnings("unchecked")
    protected static <T> T newPropInstance(final Class<?> propClass, final Class<?> attributeTypeClass) throws ParsingException {
        if ((attributeTypeClass != null) && ((propClass == null) || propClass.isAssignableFrom(attributeTypeClass))) {
            try {
                return (T) N.newInstance(attributeTypeClass);
            } catch (final Exception e) {
                if (logger.isWarnEnabled()) {
                    logger.warn("Failed to new instance by type attribute: " + attributeTypeClass.getCanonicalName(), e);
                }
            }
        }

        if (propClass != null) {
            return (T) N.newInstance(propClass);
        }

        throw new ParsingException(
                "Failed to create property instance: no property class is available and the type attribute is missing or unusable: " + attributeTypeClass);
    }

    /**
     * Determines the concrete class to use for instantiation based on target and type information.
     *
     * <p>This method resolves which class should be used when both a target class and a type class
     * are available, following these rules:</p>
     * <ul>
     *   <li>If type class is {@code null}, use target class</li>
     *   <li>If target class is {@code null} or the same as type class, use type class</li>
     *   <li>If type class is assignable to target class, use type class (more specific), <b>unless</b> it is a
     *       {@code Map}/{@code Collection} implementation the readers cannot instantiate (an immutable or
     *       unmodifiable wrapper such as {@code ImmutableMap}, {@code List.of(..)} or
     *       {@code Collections.emptyMap()}); such a type class is dropped in favour of the target class, which
     *       is what actually gets filled</li>
     *   <li>Otherwise, use target class (type class is incompatible)</li>
     * </ul>
     *
     * @param typeClass the type class from serialized type information (may be {@code null})
     * @param targetClass the target class from the deserialization context (may be {@code null})
     * @return the concrete class to instantiate
     */
    protected static Class<?> getConcreteClass(final Class<?> typeClass, final Class<?> targetClass) {
        if (typeClass == null) {
            return targetClass;
        } else if ((targetClass == null) || (targetClass == typeClass || targetClass.isAssignableFrom(typeClass))) {
            // A container is instantiated and then filled, so a runtime class without a no-argument constructor
            // (an immutable/unmodifiable wrapper such as ImmutableMap, which the XML writer legitimately records as
            // type="ImmutableMap<...>") cannot be used: preferring it would make the parser's own output unreadable
            // with "No default constructor found". Scalars are converted from text and never instantiated this way,
            // so they keep the more specific class (Integer, Instant, ... have no no-argument constructor either).
            if (targetClass != null && isUninstantiableContainerClass(typeClass)) {
                return targetClass;
            }

            return typeClass;
        } else {
            return targetClass;
        }
    }

    private static final Map<Class<?>, Boolean> uninstantiableContainerClassPool = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * Returns whether {@code cls} is a concrete {@code Map}/{@code Collection} implementation that the readers
     * cannot create. Interfaces and abstract types are excluded: those are mapped to a default implementation when
     * the instance is created.
     */
    private static boolean isUninstantiableContainerClass(final Class<?> cls) {
        if (!(Map.class.isAssignableFrom(cls) || Collection.class.isAssignableFrom(cls)) || cls.isInterface()
                || java.lang.reflect.Modifier.isAbstract(cls.getModifiers())) {
            return false;
        }

        return uninstantiableContainerClassPool.computeIfAbsent(cls, c -> {
            try {
                N.newInstance(c);
                return false;
            } catch (final Exception e) { // NOSONAR
                // Ask exactly the question the reader will ask, because a missing no-argument constructor
                // (ImmutableMap, List.of(..), Arrays.asList(..)) is only one of the ways this fails: the
                // java.util.Collections empty/unmodifiable classes DO declare one, but it is private in a package
                // java.base does not open, so the reflective call fails there instead. One throwaway instance per
                // container class, then the answer is cached for the life of the JVM.
                return true;
            }
        });
    }

    /**
     * Converts a collection to an array of the specified target type.
     *
     * <p>This method handles the conversion of collections to arrays during deserialization,
     * with special handling for:</p>
     * <ul>
     *   <li>Primitive arrays: Uses the target type's conversion method</li>
     *   <li>Object arrays: Always creates an array of the declared component type</li>
     *   <li>Empty collections: Returns an empty array of the target type</li>
     * </ul>
     *
     * <p>For an object array whose component type is not {@code Object}, every {@code non-null} element must be an
     * instance of the declared component type. An element that is not (for example a nested {@code [...]} or
     * {@code {...}} parsed into a {@code String[]} target) is reported as a {@link ParsingException} at the point of
     * parsing; the method never substitutes an array of the element's runtime class, which would only surface later
     * as a {@code ClassCastException} at an unrelated assignment.</p>
     *
     * @param <T> the return type
     * @param c the collection to convert (may be {@code null})
     * @param targetType the target array type
     * @return an array containing the collection elements, or {@code null} if the collection is {@code null}
     * @throws ParsingException if a {@code non-null} element cannot be stored in an array of the declared
     *         component type
     */
    protected static <T> T collectionToArray(final Collection<?> c, final Type<?> targetType) throws ParsingException {
        if (c == null) {
            return null;
        }

        if (!targetType.isPrimitiveArray()) {
            final Type<?> elementType = targetType.elementType();
            final Class<?> componentClass = elementType.javaType();

            // Object[] accepts everything; skip the scan on that hot path (Type.isObject() is also true for plain
            // bean types, so compare the class). For any other component type a mis-typed element must fail here
            // as a parse error, not as an ArrayStoreException from targetType.collectionToArray or a
            // ClassCastException at the caller's assignment.
            if (componentClass != Object.class) {
                int index = 0;

                for (final Object e : c) {
                    if (e != null && !componentClass.isInstance(e)) {
                        throw new ParsingException("Element at index " + index + " is a " + e.getClass().getName() + ", which cannot be stored in "
                                + targetType.name() + " (expected " + elementType.name() + ")");
                    }

                    index++;
                }
            }
        }

        return (T) targetType.collectionToArray(c);
    }

    /**
     * Creates a new file if it does not already exist.
     *
     * <p>This utility method is used by serialization methods that write to files.
     * It ensures the target file exists before attempting to write to it.</p>
     *
     * <p>Note: If the parent directories do not exist, an attempt is made to
     * create them automatically before the file is created.</p>
     *
     * @param file the file to create if it doesn't exist (must not be {@code null})
     * @throws IllegalArgumentException if {@code file} is {@code null}.
     * @throws IOException if the file cannot be created
     */
    protected static void createNewFileIfNotExists(final File file) throws IllegalArgumentException, IOException {
        N.checkArgNotNull(file, cs.file);

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
        return config != null && config.getExclusion() != null ? config.getExclusion()
                : (beanInfo.jsonXmlSeriExclusion == null ? Exclusion.NULL : beanInfo.jsonXmlSeriExclusion);
    }

    /**
     * Checks whether the given object creates a circular reference in the serialization graph.
     *
     * <p>If a circular reference is detected and the configuration does not enable
     * {@link JsonXmlSerConfig#isCircularReferenceSupported() circularReferenceSupported},
     * a {@link ParsingException} is thrown. When circular-reference support is enabled, this
     * method returns {@code true} so the caller can skip re-serializing the object (typically
     * by writing {@code null} in its place).</p>
     *
     * <p>Only bean, map, collection, object-array, and {@code MapEntity} objects are tracked;
     * primitive wrappers and simple value types are never considered circular.</p>
     *
     * @param obj the object currently being serialized; may be {@code null} (returns {@code false})
     * @param serializedObjects the set of objects already visited on the current serialization path;
     *        may be {@code null}, in which case no tracking is performed
     * @param config the serialization configuration used to determine whether circular references
     *        are permitted; may be {@code null}
     * @param bw the character writer for the current serialization output (currently unused)
     * @return {@code true} if {@code obj} was already encountered (circular reference detected and
     *         allowed by config), {@code false} otherwise
     * @throws ParsingException if a circular reference is detected and
     *         {@code config.isCircularReferenceSupported()} is {@code false}
     */
    protected static boolean hasCircularReference(final Object obj, final IdentityHashSet<Object> serializedObjects, final JsonXmlSerConfig<?> config,
            @SuppressWarnings("unused") final CharacterWriter bw) throws ParsingException {
        final Type<?> type = obj == null ? null : Type.of(obj.getClass());
        if (obj != null && serializedObjects != null //
                && (type.isBean() || type.isMap() || type.isCollection() || type.isObjectArray() || type.isMapEntity())) {
            if (serializedObjects.contains(obj)) {
                if (config == null || !config.isCircularReferenceSupported()) {
                    throw new ParsingException("Self reference found in obj: " + ClassUtil.getClassName(obj.getClass()));
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
