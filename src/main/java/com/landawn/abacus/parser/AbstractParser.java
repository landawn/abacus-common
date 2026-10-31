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
import java.util.EnumMap;
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
import java.util.concurrent.CopyOnWriteArrayList;
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
     * @throws UnsupportedOperationException if the parser does not support character-stream output (the Avro parser always throws it, before
     *         validating the arguments)
     * @throws IllegalArgumentException if {@code output} is {@code null}.
     * @throws UncheckedIOException if writing the serialized data to {@code output} or flushing buffered output fails
     */
    @Override
    public void serialize(final Object obj, final Writer output) throws UnsupportedOperationException, IllegalArgumentException, UncheckedIOException {
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
     * @throws IllegalArgumentException if {@code targetType} is {@code null} (except on the Kryo parser, which reads
     *         class-and-object data for a {@code null} class), or if {@code source} is {@code null} and the
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
     * @throws IllegalArgumentException if {@code source} or {@code targetType} is {@code null} (a {@code null} class is accepted by
     *         the Kryo parser, which then reads class-and-object data), or {@code source} is a directory.
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
     * @throws IllegalArgumentException if {@code source} or {@code targetType} is {@code null} (a {@code null} class is accepted by
     *         the Kryo parser, which then reads class-and-object data).
     * @throws UncheckedIOException if reading the serialized data from {@code source} fails
     */
    @Override
    public <T> T deserialize(final InputStream source, final Class<? extends T> targetType) throws IllegalArgumentException, UncheckedIOException {
        return deserialize(source, null, targetType);
    }

    /**
     * {@inheritDoc}
     *
     * @throws UnsupportedOperationException if the parser does not support character-stream input (the Avro parser always throws it, before
     *         validating the arguments)
     * @throws IllegalArgumentException if {@code source} or {@code targetType} is {@code null}.
     * @throws UncheckedIOException if reading the serialized data from {@code source} fails
     */
    @Override
    public <T> T deserialize(final Reader source, final Type<? extends T> targetType)
            throws UnsupportedOperationException, IllegalArgumentException, UncheckedIOException {
        return deserialize(source, null, targetType);
    }

    /**
     * {@inheritDoc}
     *
     * @throws UnsupportedOperationException if the parser does not support character-stream input (the Avro parser always throws it, before
     *         validating the arguments)
     * @throws IllegalArgumentException if {@code source} or {@code targetType} is {@code null} (a {@code null} class is accepted by
     *         the Kryo parser, which then reads class-and-object data).
     * @throws UncheckedIOException if reading the serialized data from {@code source} fails
     */
    @Override
    public <T> T deserialize(final Reader source, final Class<? extends T> targetType)
            throws UnsupportedOperationException, IllegalArgumentException, UncheckedIOException {
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
     * Creates the map for an {@code EnumMap}-typed target. {@code EnumMap} has no no-arg constructor (the generic
     * creator fails with "No default constructor found") and {@code N.newMap(EnumMap.class)} is documented to
     * return a {@code HashMap}, which a caller that declared {@code EnumMap} cannot use. A subclass of {@code EnumMap}
     * is created as itself through {@link N#newInstance(Class)}, so an anonymous subclass is rejected there.
     *
     * @param targetClass the requested {@code EnumMap} class or subclass
     * @param keyType the resolved key type (property type, {@code Type.of("java.util.EnumMap<K, V>")} or
     *        {@code setMapKeyType} of the parser's deserialization config)
     * @param configClass the deserialization config class of the calling parser ({@code JsonDeserConfig} or
     *        {@code XmlDeserConfig}); its simple name tells the caller where to declare the key type
     * @return an empty {@code EnumMap} keyed by that enum, or a new instance of the requested subclass
     * @throws IllegalArgumentException if {@code targetClass} is an anonymous subclass or otherwise cannot be created by
     *         {@link N#newInstance(Class)}
     * @throws RuntimeException if constructing a subclass reflectively is inaccessible or its constructor throws
     * @throws ParsingException if the resolved key type is not an enum (e.g. a raw {@code EnumMap.class} target), or is not
     *         the enum a subclass declares as its key type ({@code class DayMap extends EnumMap<DayOfWeek, Integer>})
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    static Map<Object, Object> newEnumMap(final Class<?> targetClass, final Type<?> keyType, final Class<?> configClass)
            throws IllegalArgumentException, RuntimeException, ParsingException {
        // A subclass is created as itself, before the key check, as any other concrete map is: a plain EnumMap is not an
        // instance of the requested class, and an anonymous subclass must be rejected like other anonymous maps.
        final Map<Object, Object> subclassInstance = targetClass == EnumMap.class ? null : (Map<Object, Object>) N.newInstance(targetClass);

        if (!keyType.javaType().isEnum()) {
            throw new ParsingException("EnumMap requires an enum key type; declare it via the property type, Type.of(\"java.util.EnumMap<K, V>\") " + "or "
                    + configClass.getSimpleName() + ".setMapKeyType (resolved key type: " + keyType.name() + ")");
        }

        if (subclassInstance == null) {
            return new EnumMap(keyType.javaType());
        }

        // A key of another enum would fail in EnumMap.put with a bare ClassCastException; name both enums instead. The
        // declared key enum is known only when the subclass parameterizes EnumMap with a class (DayMap extends
        // EnumMap<DayOfWeek, Integer>); otherwise a mismatch still surfaces at the first put.
        final Class<?> declaredKeyClass = declaredEnumMapKeyClass(targetClass);

        if (declaredKeyClass != null && declaredKeyClass != keyType.javaType()) {
            throw new ParsingException(
                    "The key type " + keyType.name() + " does not match the key enum " + declaredKeyClass.getName() + " of " + targetClass.getName());
        }

        return subclassInstance;
    }

    /**
     * The key enum an {@code EnumMap} subclass declares through its {@code EnumMap<K, V>} superclass, if that type
     * argument is a class.
     *
     * @param enumMapSubclass a subclass of {@code EnumMap}
     * @return the declared key class, or {@code null} if it is a type variable or the superclass is raw
     */
    private static Class<?> declaredEnumMapKeyClass(final Class<?> enumMapSubclass) {
        for (Class<?> cls = enumMapSubclass; cls != null && cls != EnumMap.class; cls = cls.getSuperclass()) {
            if (cls.getSuperclass() == EnumMap.class) {
                return cls.getGenericSuperclass() instanceof java.lang.reflect.ParameterizedType pt
                        && pt.getActualTypeArguments()[0] instanceof Class<?> keyClass ? keyClass : null;
            }
        }

        return null;
    }

    /**
     * Per-class cache of {@link Class#isAnonymousClass()}, which is not an intrinsic; {@link #newReaderMap(Class)} and
     * {@link #newReaderCollection(Class)} run once per container read. {@code ClassValue}, so a cached application class
     * stays collectable.
     */
    private static final ClassValue<Boolean> anonymousClassPool = new ClassValue<>() {
        @Override
        protected Boolean computeValue(final Class<?> type) {
            return type.isAnonymousClass();
        }
    };

    /**
     * Creates the target map of the StAX readers or JSON's configured map implementation through {@link N#newMap(Class)},
     * so a factory registered for a named or abstract class is used. An anonymous class is rejected first, by
     * {@link N#newInstance(Class)}: a registered supplier could otherwise mix initializer entries into the parsed data.
     *
     * @param targetClass the requested map class
     * @return a new map
     * @throws IllegalArgumentException if {@code targetClass} is an anonymous class or cannot be created
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    static Map<Object, Object> newReaderMap(final Class<?> targetClass) throws IllegalArgumentException {
        // Only anonymous classes are taken away from the factories: a named class may need its registered factory (a
        // constructor argument, a comparator), and ignoring it would silently change the result.
        return anonymousClassPool.get(targetClass) ? (Map<Object, Object>) N.newInstance(targetClass) : N.newMap((Class<Map>) targetClass);
    }

    /**
     * The collection counterpart of {@link #newReaderMap(Class)}, used by the StAX collection readers and by
     * {@code XmlParserImpl} for a collection property written as child elements (on both of its backends).
     *
     * @param targetClass the requested collection class
     * @return a new collection
     * @throws IllegalArgumentException if {@code targetClass} is an anonymous class or cannot be created
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    static Collection<Object> newReaderCollection(final Class<?> targetClass) throws IllegalArgumentException {
        return anonymousClassPool.get(targetClass) ? (Collection<Object>) N.newInstance(targetClass) : N.newCollection((Class<Collection>) targetClass);
    }

    private static final Tuple2<Function<Class<?>, Object>, Function<Object, Object>> COPY_ON_WRITE_LIST = Tuple.of(cls -> new ArrayList<>(),
            values -> new CopyOnWriteArrayList<>((Collection<?>) values));

    /**
     * Builds an exact JDK copy-on-write list in bulk when no caller observes intermediate insertions.
     * Subclasses retain their own construction and insertion behavior; the exact JDK class cannot have a
     * registered collection factory. JSON callers must disable this path for existing output or property handlers.
     *
     * @param targetClass the requested collection class
     * @param allowBulkCopyOnWrite whether an exact {@code CopyOnWriteArrayList} may be accumulated in a temporary list
     * @return the creator and final converter, or {@code null} if no special construction is registered for the class
     */
    protected static Tuple2<Function<Class<?>, Object>, Function<Object, Object>> collectionCreatorAndConverter(final Class<?> targetClass,
            final boolean allowBulkCopyOnWrite) {
        return allowBulkCopyOnWrite && targetClass == CopyOnWriteArrayList.class ? COPY_ON_WRITE_LIST : mapOfCreatorAndConverterForTargetType.get(targetClass);
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
     *   <li>If attribute type is provided and compatible, instantiate attribute type; a JDK container implementation
     *       class outside the public API ({@code Collections.emptyList()}'s class, for example) is never instantiated
     *       and counts as a failed instantiation, with or without {@code --add-opens}</li>
     *   <li>If attribute type instantiation fails or is incompatible, fall back to property class</li>
     *   <li>If no property class remains after the attribute attempt, throw ParsingException, whose cause is the
     *       type-attribute failure, if any; failures instantiating a non-null property class propagate unchanged</li>
     * </ol>
     *
     * <p>This enables polymorphic deserialization where the serialized data contains type information
     * that specifies a more concrete type than the declared property type.</p>
     *
     * @param <T> the return type
     * @param propClass the declared property class from the target bean (may be {@code null})
     * @param attributeTypeClass the type specified by a type attribute in the serialized data (may be {@code null})
     * @return a new instance of the appropriate type
     * @throws IllegalArgumentException if the type attribute is not used and {@code propClass} is an unsupported abstract
     *         type or has no usable no-argument (or enclosing-instance) constructor
     * @throws RuntimeException if the type attribute is not used and constructing {@code propClass} reflectively is
     *         inaccessible or its constructor throws
     * @throws ParsingException if no property class is available and the type-attribute class is missing, incompatible, or cannot be instantiated
     */
    @SuppressFBWarnings("NP_LOAD_OF_KNOWN_NULL_VALUE")
    @SuppressWarnings("unchecked")
    protected static <T> T newPropInstance(final Class<?> propClass, final Class<?> attributeTypeClass)
            throws IllegalArgumentException, RuntimeException, ParsingException {
        Exception attributeFailure = null;

        if ((attributeTypeClass != null) && ((propClass == null) || propClass.isAssignableFrom(attributeTypeClass))) {
            try {
                // Fails as the constructor does when java.util is not opened, whatever the JVM's module flags are.
                if (isNonPublicJdkContainerClass(attributeTypeClass)) {
                    throw new IllegalArgumentException(
                            "Not created from type metadata: " + attributeTypeClass.getName() + " is a JDK implementation class outside the public API");
                }

                return (T) N.newInstance(attributeTypeClass);
            } catch (final Exception e) {
                // One line, no stack trace: the property class is tried next. When there is none, the failure (with its
                // stack trace) becomes the cause of the ParsingException below.
                attributeFailure = e;

                if (logger.isWarnEnabled()) {
                    logger.warn("Failed to new instance by type attribute: " + attributeTypeClass.getName() + " - " + e);
                }
            }
        }

        if (propClass != null) {
            // Not addSuppressed(attributeFailure) here: a constructor may throw a shared exception instance, which would
            // then grow with every failed read (or suppress itself).
            return (T) N.newInstance(propClass);
        }

        throw new ParsingException(
                "Failed to create property instance: no property class is available and the type attribute is missing or unusable: " + attributeTypeClass,
                attributeFailure);
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
     *       is what actually gets filled. A JDK implementation class outside the public API is always dropped,
     *       even when {@code --add-opens} would make its constructor usable</li>
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

    private static final ClassValue<ContainerInstantiation> uninstantiableContainerClassPool = new ClassValue<>() {
        @Override
        protected ContainerInstantiation computeValue(final Class<?> type) {
            return new ContainerInstantiation();
        }
    };

    // ClassValue may compute competing values, so keep the constructor probe outside computeValue: only the
    // installed holder performs it, once. No Class reference is retained in the holder. Reads after the first
    // probe are lock-free, and a recursively re-entered constructor retains computeIfAbsent's failure behavior.
    private static final class ContainerInstantiation {
        private volatile Boolean failed;
        private boolean probing;

        boolean failed(final Class<?> type) {
            final Boolean result = failed;
            if (result != null) {
                return result;
            }
            synchronized (this) {
                if (failed != null) {
                    return failed;
                }
                if (probing) {
                    throw new IllegalStateException("Recursive update");
                }
                probing = true;
                try {
                    N.newInstance(type);
                    failed = false;
                } catch (final Exception e) { // NOSONAR
                    failed = true;
                } finally {
                    probing = false;
                }
                return failed;
            }
        }
    }

    /**
     * Returns whether {@code targetClass} is a concrete {@code Map}/{@code Collection} implementation that the readers
     * cannot create. Interfaces and abstract types are excluded: those are mapped to a default implementation when
     * the instance is created. A JDK implementation class outside the public API is never created
     * (see {@link #isNonPublicJdkContainerClass(Class)}).
     */
    private static boolean isUninstantiableContainerClass(final Class<?> targetClass) {
        if (!(Map.class.isAssignableFrom(targetClass) || Collection.class.isAssignableFrom(targetClass)) || targetClass.isInterface()
                || java.lang.reflect.Modifier.isAbstract(targetClass.getModifiers())) {
            return false;
        }

        // Before the probe: with --add-opens java.base/java.util the probe would succeed for Collections$EmptyList.
        if (isNonPublicJdkContainerClass(targetClass)) {
            return true;
        }

        // Ask exactly the question the reader will ask, including inaccessible or throwing constructors.
        // Retain the answer for this class's lifetime without pinning its class loader in a global map.
        return uninstantiableContainerClassPool.get(targetClass).failed(targetClass);
    }

    /**
     * Returns whether {@code containerClass} is a concrete {@code Map}/{@code Collection} implementation of the JDK that
     * is not public API: a class of a {@code java.*} or {@code jdk.*} module of the boot layer that is not public, is
     * nested in a class that is not public, or is declared in a package that its module does not export unconditionally.
     * Examples are the classes of {@code Collections.emptyList()}, {@code Collections.singletonMap(..)},
     * {@code Collections.unmodifiableSet(..)}, {@code List.of(..)}, {@code Arrays.asList(..)} and {@code HashMap.keySet()}.
     *
     * <p>The readers never construct such a class when document type metadata names it; they take the fallback that
     * they use when a constructor fails. Whether reflection can reach these constructors depends only on the JVM's
     * module flags ({@code --add-opens java.base/java.util} makes the private no-argument constructor of
     * {@code Collections$EmptyList} usable), and what it would create is an immutable or view container, or a second
     * instance of a JDK singleton, which the reader could not fill. The result of a read must not depend on how the JVM
     * was started.</p>
     *
     * @param containerClass the class named by type metadata
     * @return {@code true} if the readers must not construct {@code containerClass}
     */
    static boolean isNonPublicJdkContainerClass(final Class<?> containerClass) {
        return nonPublicJdkContainerClassPool.get(containerClass);
    }

    private static final ClassValue<Boolean> nonPublicJdkContainerClassPool = new ClassValue<>() {
        @Override
        protected Boolean computeValue(final Class<?> type) {
            if (!(Map.class.isAssignableFrom(type) || Collection.class.isAssignableFrom(type)) || type.isInterface()
                    || java.lang.reflect.Modifier.isAbstract(type.getModifiers())) {
                return false;
            }

            // JDK classes are those of the boot layer's java.* and jdk.* modules (the rule of CommonUtil.isBuiltinClass).
            // Module flags such as --add-opens change nothing for a class of the unnamed module or of an application module.
            final Module module = type.getModule();

            if (!module.isNamed() || module.getLayer() != ModuleLayer.boot()
                    || !(module.getName().startsWith("java.") || module.getName().startsWith("jdk."))) {
                return false;
            }

            // Unconditional exports only: --add-exports ...=ALL-UNNAMED must not turn an internal package into public API.
            if (!module.isExported(type.getPackageName())) {
                return true;
            }

            for (Class<?> cls = type; cls != null; cls = cls.getEnclosingClass()) {
                if (!java.lang.reflect.Modifier.isPublic(cls.getModifiers())) {
                    return true;
                }
            }

            return false;
        }
    };

    /**
     * Converts a collection to an array of the specified target type.
     *
     * <p>This method handles the conversion of collections to arrays during deserialization,
     * with special handling for:</p>
     * <ul>
     *   <li>Primitive arrays: Uses the target type's conversion method; a {@code null} element (an explicit null
     *       marker in the input) becomes the primitive default value ({@code 0}, {@code false}, ...), as the JSON
     *       parser reads {@code [1, null]} into an {@code int[]}</li>
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
     *         component type (for a primitive array: an element that is not an instance of the component's wrapper
     *         class, such as a nested array or map)
     */
    @SuppressWarnings("unchecked")
    protected static <T> T collectionToArray(final Collection<?> c, final Type<?> targetType) throws ParsingException {
        if (c == null) {
            return null;
        }

        if (targetType.isPrimitiveArray()) {
            try {
                try {
                    return (T) targetType.collectionToArray(c);
                } catch (final NullPointerException e) {
                    // The primitive types' collectionToArray unboxes every element, so a null element (an explicit null
                    // marker such as <e isNull="true"/>) escaped as a raw NullPointerException. It reads as the primitive
                    // default instead, like the JSON parser's primitive arrays. Handled on this failure path only, so the
                    // common all-non-null conversion pays nothing.
                    if (!N.anyNull(c)) {
                        throw e;
                    }

                    final Object defaultValue = targetType.elementType().defaultValue();
                    final List<Object> list = new ArrayList<>(c.size());

                    for (final Object element : c) {
                        list.add(element == null ? defaultValue : element);
                    }

                    return (T) targetType.collectionToArray(list);
                }
            } catch (final ClassCastException e) {
                // The unboxing cast also fails for a mistyped element (a nested array, map or list item). Report it as
                // the same parse error as the object-array check below instead of a raw ClassCastException; found on
                // this failure path only, so the common conversion pays nothing.
                throw primitiveElementMismatch(c, targetType, e);
            }
        } else {
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

    // The ParsingException for the first non-null element of c that the primitive array targetType cannot hold (not an
    // instance of the component's wrapper class); the original exception when no such element is found (a failure from
    // the collection itself).
    private static RuntimeException primitiveElementMismatch(final Collection<?> c, final Type<?> targetType, final ClassCastException cause) {
        final Type<?> elementType = targetType.elementType();
        final Class<?> wrapperClass = ClassUtil.wrap(elementType.javaType());
        int index = 0;

        for (final Object e : c) {
            if (e != null && !wrapperClass.isInstance(e)) {
                return new ParsingException("Element at index " + index + " is a " + e.getClass().getName() + ", which cannot be stored in " + targetType.name()
                        + " (expected " + elementType.name() + ")", cause);
            }

            index++;
        }

        return cause;
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
     * @throws UncheckedIOException if the filesystem reports an I/O error while creating {@code file} or its missing parent
     *         directories
     * @throws IOException if {@code file} did not exist but no new file was created at its path (for example because a dangling
     *         symbolic link occupies it)
     */
    protected static void createNewFileIfNotExists(final File file) throws IllegalArgumentException, UncheckedIOException, IOException {
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
     * primitive wrappers and simple value types are never considered circular. A tracked object that is
     * not yet in {@code serializedObjects} is added to it (and {@code false} is returned), so the caller
     * must remove it again once the object is fully written; otherwise a second, non-circular reference
     * to the same object would be reported as circular.</p>
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
        // Without an identity set (circular references not tracked) the type lookup below is not needed.
        if (obj == null || serializedObjects == null) {
            return false;
        }

        final Type<?> type = Type.of(obj.getClass());

        // add() answers false when obj is already present, sparing a separate contains() lookup.
        if ((type.isBean() || type.isMap() || type.isCollection() || type.isObjectArray() || type.isMapEntity()) && !serializedObjects.add(obj)) {
            if (config == null || !config.isCircularReferenceSupported()) {
                throw new ParsingException("Self reference found in obj: " + ClassUtil.getClassName(obj.getClass()));
            }

            // bw.write("null");

            return true;
        }

        return false;
    }
}
