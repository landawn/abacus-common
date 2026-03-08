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

import static com.landawn.abacus.parser.AbstractJsonReader.eventChars;
import static com.landawn.abacus.parser.JsonReader.COLON;
import static com.landawn.abacus.parser.JsonReader.COMMA;
import static com.landawn.abacus.parser.JsonReader.END_BRACE;
import static com.landawn.abacus.parser.JsonReader.END_BRACKET;
import static com.landawn.abacus.parser.JsonReader.END_DOUBLE_QUOTE;
import static com.landawn.abacus.parser.JsonReader.END_SINGLE_QUOTE;
import static com.landawn.abacus.parser.JsonReader.EOF;
import static com.landawn.abacus.parser.JsonReader.START_BRACE;
import static com.landawn.abacus.parser.JsonReader.START_BRACKET;
import static com.landawn.abacus.parser.JsonReader.START_DOUBLE_QUOTE;
import static com.landawn.abacus.parser.JsonReader.START_SINGLE_QUOTE;
import static com.landawn.abacus.parser.JsonReader.UNDEFINED;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.CharBuffer;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Supplier;

import com.landawn.abacus.annotation.JsonXmlField;
import com.landawn.abacus.exception.ParsingException;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.parser.ParserUtil.BeanInfo;
import com.landawn.abacus.parser.ParserUtil.PropInfo;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.Array;
import com.landawn.abacus.util.BufferedJsonWriter;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.Dataset;
import com.landawn.abacus.util.EntityId;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.IdentityHashSet;
import com.landawn.abacus.util.ImmutableEntry;
import com.landawn.abacus.util.MapEntity;
import com.landawn.abacus.util.MutableBoolean;
import com.landawn.abacus.util.MutableInt;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.NamingPolicy;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.RowDataset;
import com.landawn.abacus.util.Seid;
import com.landawn.abacus.util.Sheet;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.Triple;
import com.landawn.abacus.util.Tuple;
import com.landawn.abacus.util.Tuple.Tuple1;
import com.landawn.abacus.util.Tuple.Tuple2;
import com.landawn.abacus.util.Tuple.Tuple3;
import com.landawn.abacus.util.Tuple.Tuple4;
import com.landawn.abacus.util.Tuple.Tuple5;
import com.landawn.abacus.util.Tuple.Tuple6;
import com.landawn.abacus.util.Tuple.Tuple7;
import com.landawn.abacus.util.Tuple.Tuple8;
import com.landawn.abacus.util.Tuple.Tuple9;
import com.landawn.abacus.util.cs;
import com.landawn.abacus.util.stream.Stream;

/**
 * Internal implementation of JSON parser for serialization and deserialization operations.
 *
 * <p>This class provides the core implementation for converting Java objects to JSON format
 * and vice versa. It extends {@link AbstractJsonParser} and provides optimized handling for
 * various data types including primitives, collections, maps, beans, datasets, and entity IDs.</p>
 *
 * <p>Key features:</p>
 * <ul>
 *   <li>Efficient JSON serialization with configurable formatting options</li>
 *   <li>Robust JSON deserialization with type safety</li>
 *   <li>Support for complex types (Dataset, Sheet, EntityId, MapEntity)</li>
 *   <li>Circular reference detection and handling</li>
 *   <li>Streaming support for large JSON arrays</li>
 *   <li>Configurable {@code null} value handling and property exclusion</li>
 *   <li>Pretty printing and indentation support</li>
 * </ul>
 *
 * <p>This class is package-private and should be accessed through {@link ParserFactory} or
 * {@link JsonParser} interface.</p>
 *
 * @see AbstractJsonParser
 * @see JsonSerConfig
 * @see JsonDeserConfig
 * @see ParserFactory
 */
@SuppressWarnings("deprecation")
final class JsonParserImpl extends AbstractJsonParser {

    private static final String ENTITY_NAME = "beanName";

    private static final String ENTITY_TYPE = "beanType";

    private static final String COLUMN_NAMES = "columnNames";

    private static final String COLUMN_TYPES = "columnTypes";

    private static final String COLUMNS = "columns";

    private static final String PROPERTIES = "properties";

    private static final String IS_FROZEN = "isFrozen";

    private static final String ROW_KEY_SET = "rowKeySet";
    private static final String COLUMN_KEY_SET = "columnKeySet";
    private static final String ROW_KEY_TYPE = "rowKeyType";
    private static final String COLUMN_KEY_TYPE = "columnKeyType";

    private static final Map<String, Integer> datasetSheetPropOrder = new HashMap<>();

    static {
        datasetSheetPropOrder.put(ENTITY_NAME, 1);
        datasetSheetPropOrder.put(ENTITY_TYPE, 2);
        datasetSheetPropOrder.put(COLUMN_NAMES, 3);
        datasetSheetPropOrder.put(COLUMN_TYPES, 4);
        datasetSheetPropOrder.put(PROPERTIES, 5);
        datasetSheetPropOrder.put(IS_FROZEN, 6);
        datasetSheetPropOrder.put(COLUMNS, 7);
        datasetSheetPropOrder.put(ROW_KEY_SET, 8);
        datasetSheetPropOrder.put(COLUMN_KEY_SET, 9);
        datasetSheetPropOrder.put(ROW_KEY_TYPE, 10);
        datasetSheetPropOrder.put(COLUMN_KEY_TYPE, 11);
    }

    private static final JsonDeserConfig jdcForStringElement = JsonDeserConfig.create().setElementType(String.class);

    private static final JsonDeserConfig jdcForTypeElement = JsonDeserConfig.create().setElementType(Type.class);

    private static final JsonDeserConfig jdcForPropertiesElement = JsonDeserConfig.create().setElementType(String.class).setMapKeyType(String.class);

    JsonParserImpl() {
    }

    JsonParserImpl(final JsonSerConfig jsc, final JsonDeserConfig jdc) {
        super(jsc, jdc);
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation uses an internal {@link JsonReader} for efficient parsing
     * and supports circular reference detection when enabled in the configuration.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JsonDeserConfig config = new JsonDeserConfig()
     *     .setIgnoreUnmatchedProperty(true)
     *     .setReadNullToEmpty(true);
     *
     * String json = "{\"name\":\"John\",\"age\":30}";
     * User user = parser.parse(json, config, Type.of(User.class));
     *
     * // Deserialize collection with type safety
     * String jsonArray = "[{\"id\":1},{\"id\":2}]";
     * List<Item> items = parser.parse(jsonArray, config, new TypeReference<List<Item>>() {});
     * }</pre>
     *
     * @param <T> the type of the target object
     * @param source the JSON string to parse; may be {@code null} or empty
     * @param config the deserialization configuration to use; may be {@code null} to use default configuration
     * @param targetType the type of the target object to deserialize into; must not be {@code null}
     * @return the deserialized object of type {@code T}, or {@code null} if the source is {@code null}
     * @throws UncheckedIOException if an I/O error occurs during parsing
     * @throws ParsingException if the JSON structure is invalid or doesn't match the target type
     */
    @Override
    public <T> T parse(String source, JsonDeserConfig config, Type<? extends T> targetType) {
        final JsonDeserConfig configToUse = check(config);

        if ((Strings.isEmpty(source) && configToUse.isReadNullToEmpty()) || (source != null && source.isEmpty())) {
            return emptyOrDefault(targetType);
        } else if (source == null) {
            return targetType.defaultValue();
        }

        final char[] cbuf = Objectory.createCharArrayBuffer();

        try {
            final JsonReader jr = JsonStringReader.parse(source, cbuf);

            return parse(source, jr, configToUse, targetType, null);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            Objectory.recycle(cbuf);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation delegates to {@link #parse(String, JsonDeserConfig, Type)}
     * after converting the class to a Type.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JsonDeserConfig config = new JsonDeserConfig()
     *     .setIgnoreUnmatchedProperty(true);
     *
     * String json = "{\"name\":\"John\",\"age\":30}";
     * User user = parser.parse(json, config, User.class);
     * }</pre>
     *
     * @param <T> the type of the target class
     * @param source the JSON string to parse; may be {@code null} or empty
     * @param config the deserialization configuration to use; may be {@code null} to use default configuration
     * @param targetClass the class of the target object to deserialize into; must not be {@code null}
     * @return the deserialized object of type {@code T}, or {@code null} if the source is {@code null}
     * @throws UncheckedIOException if an I/O error occurs during parsing
     * @throws ParsingException if the JSON structure is invalid or doesn't match the target class
     */
    @Override
    public <T> T parse(final String source, final JsonDeserConfig config, final Class<? extends T> targetClass) {
        return parse(source, config, Type.of(targetClass));
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation parses a JSON array and populates the provided array with
     * deserialized elements. The array must be pre-allocated with sufficient size.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JsonDeserConfig config = new JsonDeserConfig();
     * String json = "[1, 2, 3, 4, 5]";
     * Integer[] numbers = new Integer[5];
     * parser.parse(json, config, numbers);
     * // numbers array is now filled: [1, 2, 3, 4, 5]
     * }</pre>
     *
     * @param source the JSON array string to parse; may be {@code null} (in which case no action is taken)
     * @param config the deserialization configuration to use; may be {@code null} to use default configuration
     * @param output the pre-allocated array to populate with parsed values; must not be {@code null}
     * @throws UncheckedIOException if an I/O error occurs during parsing
     * @throws ParsingException if the JSON structure is invalid or not an array
     * @throws IndexOutOfBoundsException if the JSON array contains more elements than the output array can hold
     */
    @Override
    public void parse(final String source, final JsonDeserConfig config, final Object[] output) {
        final JsonDeserConfig configToUse = check(config);

        //    if (N.isEmpty(str)) { // TODO ?
        //        return;
        //    }

        if (source == null) {
            return;
        }

        final char[] cbuf = Objectory.createCharArrayBuffer();

        try {
            final JsonReader jr = JsonStringReader.parse(source, cbuf);

            parse(source, jr, configToUse, Type.of(output.getClass()), output);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            Objectory.recycle(cbuf);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation parses a JSON array and adds the deserialized elements to the
     * provided collection. The collection is not cleared before adding elements.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JsonDeserConfig config = new JsonDeserConfig();
     * String json = "[\"apple\", \"banana\", \"orange\"]";
     * List<String> fruits = new ArrayList<>();
     * parser.parse(json, config, fruits);
     * // fruits now contains: ["apple", "banana", "orange"]
     * }</pre>
     *
     * @param source the JSON array string to parse; may be {@code null} (in which case no action is taken)
     * @param config the deserialization configuration to use; may be {@code null} to use default configuration
     * @param output the collection to populate with parsed elements; must not be {@code null}
     * @throws UncheckedIOException if an I/O error occurs during parsing
     * @throws ParsingException if the JSON structure is invalid or not an array
     * @throws UnsupportedOperationException if the collection is unmodifiable
     */
    @Override
    public void parse(final String source, final JsonDeserConfig config, final Collection<?> output) {
        final JsonDeserConfig configToUse = check(config);

        //    if (N.isEmpty(str)) { // TODO ?
        //        return;
        //    }

        if (source == null) {
            return;
        }

        final char[] cbuf = Objectory.createCharArrayBuffer();

        try {
            final JsonReader jr = JsonStringReader.parse(source, cbuf);

            parse(source, jr, configToUse, Type.of(output.getClass()), output);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            Objectory.recycle(cbuf);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation parses a JSON object and adds the deserialized key-value pairs
     * to the provided map. The map is not cleared before adding entries.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JsonDeserConfig config = new JsonDeserConfig();
     * String json = "{\"key1\":\"value1\",\"key2\":\"value2\"}";
     * Map<String, String> map = new HashMap<>();
     * parser.parse(json, config, map);
     * // map now contains: {key1=value1, key2=value2}
     * }</pre>
     *
     * @param source the JSON object string to parse; may be {@code null} or empty (in which case no action is taken)
     * @param config the deserialization configuration to use; may be {@code null} to use default configuration
     * @param output the map to populate with parsed key-value pairs; must not be {@code null}
     * @throws UncheckedIOException if an I/O error occurs during parsing
     * @throws ParsingException if the JSON structure is invalid or not an object
     * @throws UnsupportedOperationException if the map is unmodifiable
     */
    @Override
    public void parse(final String source, final JsonDeserConfig config, final Map<?, ?> output) {
        final JsonDeserConfig configToUse = check(config);

        if (Strings.isEmpty(source)) {
            return;
        }

        final char[] cbuf = Objectory.createCharArrayBuffer();

        try {
            final JsonReader jr = JsonStringReader.parse(source, cbuf);

            parse(source, jr, configToUse, Type.of(output.getClass()), output);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            Objectory.recycle(cbuf);
        }
    }

    /**
     * Reads and deserializes a JSON string using a JsonReader into the specified target class.
     *
     * <p>This is an internal method that performs the actual JSON parsing using a JsonReader.
     * It handles various serialization types including arrays, collections, maps, beans, datasets,
     * sheets, and entity IDs. The method dispatches to type-specific read methods based on the
     * serialization type of the target class.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JsonReader jr = JsonStringReader.parse(jsonString, charBuffer);
     * MyBean bean = parse(jsonString, jr, config, Type.of(MyBean.class), null);
     * }</pre>
     *
     * @param <T> the type of the target object
     * @param source the original JSON string source
     * @param jr the JsonReader instance for parsing the JSON
     * @param config the deserialization configuration
     * @param targetType the type of the target object to deserialize into
     * @param output optional pre-existing output object (array, collection, or map) to populate; may be {@code null}
     * @return the deserialized object of type {@code T}
     * @throws IOException if an I/O error occurs during reading
     * @throws ParsingException if the JSON structure doesn't match the target class or is invalid
     */
    @SuppressWarnings("unchecked")
    protected <T> T parse(final String source, final JsonReader jr, final JsonDeserConfig config, final Type<? extends T> targetType, final Object output)
            throws IOException {
        final Class<? extends T> targetClass = targetType.javaType();
        final Object[] a = (output instanceof Object[]) ? (Object[]) output : null;
        final Collection<Object> c = (output instanceof Collection) ? (Collection<Object>) output : null;
        final Map<Object, Object> m = (output instanceof Map) ? (Map<Object, Object>) output : null;

        switch (targetType.serializationType()) {
            case SERIALIZABLE:
                if (targetType.isArray()) {
                    return readArray(jr, config, null, true, targetClass, targetType, a);
                } else if (targetType.isCollection()) {
                    return readCollection(jr, config, null, null, true, targetClass, targetType, c);
                } else {
                    return (T) readNullToEmpty(targetType, targetType.valueOf(source), config.isReadNullToEmpty());
                }

            case ENTITY:
                return readBean(jr, config, true, targetClass, targetType);

            case MAP:
                return readMap(jr, config, null, true, targetClass, targetType, m);

            case ARRAY:
                return readArray(jr, config, null, true, targetClass, targetType, a);

            case COLLECTION:
                return readCollection(jr, config, null, null, true, targetClass, targetType, c);

            case MAP_ENTITY:
                return readMapEntity(jr, config, true, targetClass, targetType);

            case DATA_SET:
                return readDataset(jr, UNDEFINED, config, true, targetClass, targetType);

            case SHEET:
                return readSheet(jr, UNDEFINED, config, true, targetClass, targetType);

            case ENTITY_ID:
                return readEntityId(jr, config, true, targetClass, targetType);

            default:
                final int firstToken = jr.nextToken();

                if (Object.class.equals(targetClass)) {
                    if (firstToken == START_BRACE) {
                        return (T) readMap(jr, config, null, false, Map.class, null, null);
                    } else if (firstToken == START_BRACKET) {
                        return (T) readCollection(jr, config, null, null, false, List.class, null, null);
                    }
                }

                throw new ParsingException("Unsupported class: " + ClassUtil.getCanonicalClassName(targetType.javaType()) //NOSONAR
                        + ". Only Array/List/Map and Bean class with getter/setter methods are supported"); //NOSONAR
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation converts Java objects to JSON string format with support for:</p>
     * <ul>
     *   <li>Circular reference detection and handling (when enabled in config)</li>
     *   <li>Pretty printing and indentation</li>
     *   <li>Custom property naming policies</li>
     *   <li>Selective property inclusion/exclusion</li>
     *   <li>Special handling for Dataset, Sheet, EntityId, and MapEntity types</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * User user = new User("John", 30);
     * JsonSerConfig config = new JsonSerConfig()
     *     .setPrettyFormat(true)
     *     .skipNullValue(true);
     *
     * String json = parser.serialize(user, config);
     * // Result (formatted):
     * // {
     * //   "name": "John",
     * //   "age": 30
     * // }
     * }</pre>
     *
     * @param obj the object to serialize; may be {@code null}
     * @param config the serialization configuration to use; may be {@code null} to use default configuration
     * @return the JSON string representation of the object, or {@code null} if the object is {@code null}
     * @throws UncheckedIOException if an I/O error occurs during serialization
     */
    @Override
    public String serialize(final Object obj, final JsonSerConfig config) {
        final JsonSerConfig configToUse = check(config);

        if (obj == null) {
            return null;
        }

        final Class<?> cls = obj.getClass();
        final Type<Object> type = Type.of(cls);

        if (type.isSerializable() && !(type.isCollection() || type.isArray() || type.javaType().isEnum())) {
            return type.stringOf(obj);
        }

        final BufferedJsonWriter bw = Objectory.createBufferedJsonWriter();
        final IdentityHashSet<Object> serializedObjects = !configToUse.isSupportCircularReference() ? null : new IdentityHashSet<>();

        try {
            write(obj, configToUse, serializedObjects, type, bw, false);

            return bw.toString();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            Objectory.recycle(bw);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation serializes the object to JSON and writes it to the specified file.
     * If the file doesn't exist, it will be created. If it exists, it will be overwritten.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * User user = new User("John", 30);
     * JsonSerConfig config = new JsonSerConfig()
     *     .setPrettyFormat(true);
     *
     * File outputFile = new File("user.json");
     * parser.serialize(user, config, outputFile);
     * // File now contains the JSON representation of the user
     * }</pre>
     *
     * @param obj the object to serialize; may be {@code null} (writes empty string to file)
     * @param config the serialization configuration to use; may be {@code null} to use default configuration
     * @param output the file to write the JSON to; must not be {@code null}
     * @throws UncheckedIOException if an I/O error occurs during serialization or file writing
     */
    @Override
    public void serialize(final Object obj, final JsonSerConfig config, final File output) {
        final JsonSerConfig configToUse = check(config);

        if (obj == null) {
            try {
                IOUtil.write(Strings.EMPTY, output);
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }
            return;
        }

        final Class<?> cls = obj.getClass();
        final Type<Object> type = Type.of(cls);

        if (type.isSerializable() && !(type.isCollection() || type.isArray() || type.javaType().isEnum())) {
            try {
                IOUtil.write(type.stringOf(obj), output);
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }

            return;
        }

        Writer writer = null;

        try {
            createNewFileIfNotExists(output);

            writer = IOUtil.newFileWriter(output);

            serialize(obj, configToUse, writer);

            writer.flush();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            IOUtil.close(writer);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation serializes the object to JSON and writes it to the specified output stream.
     * The output stream is not closed by this method and should be managed by the caller.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * User user = new User("John", 30);
     * JsonSerConfig config = new JsonSerConfig();
     *
     * try (OutputStream os = new FileOutputStream("user.json")) {
     *     parser.serialize(user, config, os);
     *     // Stream now contains the JSON representation
     * }
     * }</pre>
     *
     * @param obj the object to serialize; may be {@code null} (writes empty string to stream)
     * @param config the serialization configuration to use; may be {@code null} to use default configuration
     * @param output the output stream to write the JSON to; must not be {@code null}
     * @throws UncheckedIOException if an I/O error occurs during serialization or stream writing
     */
    @Override
    public void serialize(final Object obj, final JsonSerConfig config, final OutputStream output) {
        final JsonSerConfig configToUse = check(config);

        if (obj == null) {
            try {
                IOUtil.write(Strings.EMPTY, output);
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }

            return;
        }

        final Class<?> cls = obj.getClass();
        final Type<Object> type = Type.of(cls);

        if (type.isSerializable() && !(type.isCollection() || type.isArray() || type.javaType().isEnum())) {
            try {
                IOUtil.write(type.stringOf(obj), output, true);
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }

            return;
        }

        final BufferedJsonWriter bw = Objectory.createBufferedJsonWriter(output);
        final IdentityHashSet<Object> serializedObjects = !configToUse.isSupportCircularReference() ? null : new IdentityHashSet<>();

        try {
            write(obj, configToUse, serializedObjects, type, bw, true);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            Objectory.recycle(bw);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation serializes the object to JSON and writes it to the specified writer.
     * The writer is not closed by this method and should be managed by the caller.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * User user = new User("John", 30);
     * JsonSerConfig config = new JsonSerConfig()
     *     .setPrettyFormat(true);
     *
     * try (Writer writer = new FileWriter("user.json")) {
     *     parser.serialize(user, config, writer);
     *     // Writer now contains the JSON representation
     * }
     * }</pre>
     *
     * @param obj the object to serialize; may be {@code null} (writes empty string to writer)
     * @param config the serialization configuration to use; may be {@code null} to use default configuration
     * @param output the writer to write the JSON to; must not be {@code null}
     * @throws UncheckedIOException if an I/O error occurs during serialization or writing
     */
    @Override
    public void serialize(final Object obj, final JsonSerConfig config, final Writer output) {
        final JsonSerConfig configToUse = check(config);

        if (obj == null) {
            try {
                IOUtil.write(Strings.EMPTY, output);
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }

            return;
        }

        final Class<?> cls = obj.getClass();
        final Type<Object> type = Type.of(cls);

        if (type.isSerializable() && !(type.isCollection() || type.isArray() || type.javaType().isEnum())) {
            try {
                IOUtil.write(type.stringOf(obj), output, true);
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }

            return;
        }

        final boolean isBufferedWriter = output instanceof BufferedJsonWriter;
        final BufferedJsonWriter bw = isBufferedWriter ? (BufferedJsonWriter) output : Objectory.createBufferedJsonWriter(output);
        final IdentityHashSet<Object> serializedObjects = !configToUse.isSupportCircularReference() ? null : new IdentityHashSet<>();

        try {
            write(obj, configToUse, serializedObjects, type, bw, true);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            if (!isBufferedWriter) {
                Objectory.recycle(bw);
            }
        }
    }

    @SuppressWarnings("rawtypes")
    protected void write(final Object obj, final JsonSerConfig config, final boolean isFirstCall, final String indentation,
            final IdentityHashSet<Object> serializedObjects, final BufferedJsonWriter bw) throws IOException {
        if (hasCircularReference(obj, serializedObjects, config, bw)) {
            return;
        }

        final Type<Object> type = Type.of(obj.getClass());

        switch (type.serializationType()) {
            case SERIALIZABLE:
                type.writeCharacter(bw, obj, config);

                break;

            case ENTITY:
                writeBean(obj, config, isFirstCall, indentation, serializedObjects, type, bw);

                break;

            case MAP:
                writeMap((Map<?, ?>) obj, config, isFirstCall, indentation, serializedObjects, type, bw);

                break;

            case ARRAY:
                writeArray(obj, config, isFirstCall, indentation, serializedObjects, type, bw);

                break;

            case COLLECTION:
                writeCollection((Collection<?>) obj, config, isFirstCall, indentation, serializedObjects, type, bw);

                break;

            case MAP_ENTITY:
                writeMapEntity((MapEntity) obj, config, isFirstCall, indentation, serializedObjects, type, bw);

                break;

            case ENTITY_ID:
                writeEntityId((EntityId) obj, config, isFirstCall, indentation, serializedObjects, type, bw);

                break;

            case DATA_SET:
                writeDataset((Dataset) obj, config, isFirstCall, indentation, serializedObjects, type, bw);

                break;

            case SHEET:
                writeSheet((Sheet) obj, config, isFirstCall, indentation, serializedObjects, type, bw);

                break;

            default:
                if (config == null || config.isFailOnEmptyBean()) {
                    throw new ParsingException("Unsupported class: " + ClassUtil.getCanonicalClassName(type.javaType())
                            + ". Only Array/List/Map and Bean class with getter/setter methods are supported");
                } else {
                    bw.write("{}");
                }
        }
    }

    protected void write(final Object obj, final JsonSerConfig config, final IdentityHashSet<Object> serializedObjects, final Type<Object> type,
            final BufferedJsonWriter bw, final boolean flush) throws IOException {
        if (config.isBracketRootValue() || !type.isSerializable()) {
            write(obj, config, true, null, serializedObjects, type, bw, flush);
        } else {
            if (type.isObjectArray()) {
                writeArray(obj, config, true, null, serializedObjects, type, bw);
            } else if (type.isCollection()) {
                writeCollection((Collection<?>) obj, config, true, null, serializedObjects, type, bw);
            } else if (type.isPrimitiveArray()) {
                writeArray(obj, config, true, null, serializedObjects, type, bw);
            } else {
                write(obj, config, true, null, serializedObjects, type, bw, flush);
            }
        }
    }

    @SuppressWarnings("unused")
    protected void write(final Object obj, final JsonSerConfig config, final boolean isFirstCall, final String indentation,
            final IdentityHashSet<Object> serializedObjects, final Type<Object> type, final BufferedJsonWriter bw, final boolean flush) throws IOException {
        if (obj == null) {
            return;
        }

        write(obj, config, isFirstCall, indentation, serializedObjects, bw);

        if (flush) {
            bw.flush();
        }
    }

    protected void writeBean(final Object obj, final JsonSerConfig config, final boolean isFirstCall, final String indentation,
            final IdentityHashSet<Object> serializedObjects, final Type<Object> type, final BufferedJsonWriter bw) throws IOException {
        //    if (hasCircularReference(obj, serializedObjects, config, bw)) {
        //        return;
        //    }

        final Class<?> cls = type.javaType();
        final BeanInfo beanInfo = ParserUtil.getBeanInfo(type.reflectType());

        if (N.isEmpty(beanInfo.jsonXmlSerializablePropInfos)) {
            throw new ParsingException("No serializable property is found in class: " + ClassUtil.getCanonicalClassName(cls));
        }

        final Exclusion exclusion = getExclusion(config, beanInfo);

        final boolean ignoreNullProperty = (exclusion == Exclusion.NULL) || (exclusion == Exclusion.DEFAULT);
        final boolean ignoreDefaultProperty = (exclusion == Exclusion.DEFAULT);

        final Collection<String> ignoredClassPropNames = config.getIgnoredPropNames(cls);
        final boolean writeNullToEmpty = config.isWriteNullToEmpty();
        final boolean quotePropName = config.isQuotePropName();
        final boolean isPrettyFormat = config.isPrettyFormat();
        final NamingPolicy jsonXmlNamingPolicy = config.getPropNamingPolicy() == null ? beanInfo.jsonXmlNamingPolicy : config.getPropNamingPolicy();
        final int nameTagIdx = jsonXmlNamingPolicy.ordinal();

        final PropInfo[] propInfoList = config.isSkipTransientField() ? beanInfo.nonTransientSeriPropInfos : beanInfo.jsonXmlSerializablePropInfos;
        PropInfo propInfo = null;
        String propName = null;
        Object propValue = null;

        if (config.isBracketRootValue() || !isFirstCall) {
            bw.write(_BRACE_L);
        }

        String nextIndentation = isPrettyFormat ? ((indentation == null ? Strings.EMPTY : indentation) + config.getIndentation()) : null;

        if (config.isWrapRootValue()) {
            if (isPrettyFormat) {
                bw.write(IOUtil.LINE_SEPARATOR_UNIX);

                if (indentation != null) {
                    bw.write(indentation);
                }

                bw.write(config.getIndentation());
            }

            if (config.isQuotePropName()) {
                bw.write(_DOUBLE_QUOTE);
                bw.write(ClassUtil.getSimpleClassName(cls));
                bw.write(_DOUBLE_QUOTE);
            } else {
                bw.write(ClassUtil.getSimpleClassName(cls));
            }

            bw.write(COLON_SPACE_CHAR_ARRAY);
            bw.write(_BRACE_L);

            nextIndentation += config.getIndentation();
        }

        int cnt = 0;

        for (final PropInfo element : propInfoList) {
            propInfo = element;
            propName = propInfo.name;

            if (propInfo.jsonXmlExpose == JsonXmlField.Direction.DESERIALIZE_ONLY
                    || ((ignoredClassPropNames != null) && ignoredClassPropNames.contains(propName))) {
                continue;
            }

            propValue = propInfo.getPropValue(obj);

            if ((ignoreNullProperty && propValue == null) || (ignoreDefaultProperty && propValue != null && (propInfo.jsonXmlType != null)
                    && propInfo.jsonXmlType.isPrimitive() && propValue.equals(propInfo.jsonXmlType.defaultValue()))) {
                continue;
            }

            if (cnt++ > 0) {
                if (isPrettyFormat) {
                    bw.write(_COMMA);
                } else {
                    bw.write(COMMA_SPACE_CHAR_ARRAY);
                }
            }

            if (isPrettyFormat) {
                bw.write(IOUtil.LINE_SEPARATOR_UNIX);
                bw.write(nextIndentation);
            }

            if (propValue == null) {
                if (writeNullToEmpty) {
                    if (quotePropName) {
                        bw.write(propInfo.jsonNameTags[nameTagIdx].quotedNameWithColon);
                    } else {
                        bw.write(propInfo.jsonNameTags[nameTagIdx].nameWithColon);
                    }

                    writeNullToEmpty(bw, propInfo.type);

                } else {
                    if (quotePropName) {
                        bw.write(propInfo.jsonNameTags[nameTagIdx].quotedNameNull);
                    } else {
                        bw.write(propInfo.jsonNameTags[nameTagIdx].nameNull);
                    }
                }
            } else {
                if (quotePropName) {
                    bw.write(propInfo.jsonNameTags[nameTagIdx].quotedNameWithColon);
                } else {
                    bw.write(propInfo.jsonNameTags[nameTagIdx].nameWithColon);
                }

                if (propInfo.isJsonRawValue) {
                    strType.writeCharacter(bw, serialize(propValue, config), config);
                } else if (propInfo.jsonXmlType.isSerializable()) {
                    propInfo.writePropValue(bw, propValue, config);
                } else {
                    write(propValue, config, false, nextIndentation, serializedObjects, bw);
                }
            }
        }

        if (config.isWrapRootValue())

        {
            if (isPrettyFormat && cnt > 0) {
                bw.write(IOUtil.LINE_SEPARATOR_UNIX);

                if (indentation != null) {
                    bw.write(indentation);
                }

                bw.write(config.getIndentation());
            }

            bw.write(_BRACE_R);
        }

        if (config.isBracketRootValue() || !isFirstCall) {
            if (isPrettyFormat && (config.isWrapRootValue() || cnt > 0)) {
                bw.write(IOUtil.LINE_SEPARATOR_UNIX);

                if (indentation != null) {
                    bw.write(indentation);
                }
            }

            bw.write(_BRACE_R);
        }
    }

    @SuppressWarnings("unused")
    protected void writeMap(final Map<?, ?> m, final JsonSerConfig config, final boolean isFirstCall, final String indentation,
            final IdentityHashSet<Object> serializedObjects, final Type<Object> type, final BufferedJsonWriter bw) throws IOException {
        //    if (hasCircularReference(m, serializedObjects, config, bw)) {
        //        return;
        //    }

        final Collection<String> ignoredClassPropNames = config.getIgnoredPropNames(Map.class);
        // final boolean ignoreNullProperty = (config.getExclusion() == Exclusion.NULL) || (config.getExclusion() == Exclusion.DEFAULT);
        final boolean isQuoteMapKey = config.isQuoteMapKey();
        final boolean isPrettyFormat = config.isPrettyFormat();

        Type<Object> keyType = null;
        int i = 0;

        if (config.isBracketRootValue() || !isFirstCall) {
            bw.write(_BRACE_L);
        }

        final String nextIndentation = isPrettyFormat ? ((indentation == null ? Strings.EMPTY : indentation) + config.getIndentation()) : null;

        Object key = null;
        Object value = null;

        for (final Map.Entry<Object, Object> entry : ((Map<Object, Object>) m).entrySet()) {
            key = entry.getKey();

            if (key != null && (ignoredClassPropNames != null) && ignoredClassPropNames.contains(key.toString())) {
                continue;
            }

            value = entry.getValue();

            // ignoreNullProperty only for
            //    if (ignoreNullProperty && value == null) {
            //        continue;
            //    }

            if (i++ > 0) {
                if (isPrettyFormat) {
                    bw.write(_COMMA);
                } else {
                    bw.write(COMMA_SPACE_CHAR_ARRAY);
                }
            }

            if (isPrettyFormat) {
                bw.write(IOUtil.LINE_SEPARATOR_UNIX);
                bw.write(nextIndentation);
            }

            if (key == null) {
                if (isQuoteMapKey) {
                    bw.write(_DOUBLE_QUOTE);
                    bw.write(NULL_CHAR_ARRAY);
                    bw.write(_DOUBLE_QUOTE);
                } else {
                    bw.write(NULL_CHAR_ARRAY);
                }
            } else {
                keyType = Type.of(key.getClass());

                if (keyType.isSerializable() && !(keyType.isArray() || keyType.isCollection() || keyType.javaType().isEnum())) {
                    if (isQuoteMapKey || !(keyType.isNumber() || keyType.isBoolean())) {
                        bw.write(_DOUBLE_QUOTE);
                        bw.writeCharacter(keyType.stringOf(key));
                        bw.write(_DOUBLE_QUOTE);
                    } else {
                        bw.writeCharacter(keyType.stringOf(key));
                    }
                } else {
                    write(key, config, false, nextIndentation, serializedObjects, bw);
                }
            }

            bw.write(COLON_SPACE_CHAR_ARRAY);

            if (value == null) {
                bw.write(NULL_CHAR_ARRAY);
            } else {
                write(value, config, false, nextIndentation, serializedObjects, bw);
            }
        }

        if (config.isBracketRootValue() || !isFirstCall) {
            if (isPrettyFormat && N.notEmpty(m)) {
                bw.write(IOUtil.LINE_SEPARATOR_UNIX);

                if (indentation != null) {
                    bw.write(indentation);
                }
            }

            bw.write(_BRACE_R);
        }
    }

    protected void writeArray(final Object obj, final JsonSerConfig config, final boolean isFirstCall, final String indentation,
            final IdentityHashSet<Object> serializedObjects, final Type<Object> type, final BufferedJsonWriter bw) throws IOException {
        //    if (hasCircularReference(obj, serializedObjects, config, bw)) {
        //        return;
        //    }

        final boolean isPrimitiveArray = type.isPrimitiveArray();
        final boolean isPrettyFormat = config.isPrettyFormat();

        // TODO what to do if it's primitive array(e.g: int[]...)
        if (config.isBracketRootValue() || !isFirstCall) {
            bw.write(_BRACKET_L);
        }

        final String nextIndentation = isPrettyFormat ? ((indentation == null ? Strings.EMPTY : indentation) + config.getIndentation()) : null;

        final Object[] a = isPrimitiveArray ? null : (Object[]) obj;
        final int len = isPrimitiveArray ? Array.getLength(obj) : a.length;
        Object element = null;

        for (int i = 0; i < len; i++) {
            element = isPrimitiveArray ? Array.get(obj, i) : a[i];

            if (i > 0) {
                if (isPrettyFormat) {
                    bw.write(_COMMA);
                } else {
                    bw.write(COMMA_SPACE_CHAR_ARRAY);
                }
            }

            if (isPrettyFormat) {
                bw.write(IOUtil.LINE_SEPARATOR_UNIX);
                bw.write(nextIndentation);
            }

            if (element == null) {
                bw.write(NULL_CHAR_ARRAY);
            } else {
                write(element, config, false, nextIndentation, serializedObjects, bw);
            }
        }

        if (config.isBracketRootValue() || !isFirstCall) {
            if (isPrettyFormat && len > 0) {
                bw.write(IOUtil.LINE_SEPARATOR_UNIX);

                if (indentation != null) {
                    bw.write(indentation);
                }
            }

            bw.write(_BRACKET_R);
        }
    }

    @SuppressWarnings("unused")
    protected void writeCollection(final Collection<?> c, final JsonSerConfig config, final boolean isFirstCall, final String indentation,
            final IdentityHashSet<Object> serializedObjects, final Type<Object> type, final BufferedJsonWriter bw) throws IOException {
        //    if (hasCircularReference(c, serializedObjects, config, bw)) {
        //        return;
        //    }

        final boolean isPrettyFormat = config.isPrettyFormat();

        if (config.isBracketRootValue() || !isFirstCall) {
            bw.write(_BRACKET_L);
        }

        final String nextIndentation = isPrettyFormat ? ((indentation == null ? Strings.EMPTY : indentation) + config.getIndentation()) : null;
        int i = 0;

        for (final Object element : c) {
            if (i++ > 0) {
                if (isPrettyFormat) {
                    bw.write(_COMMA);
                } else {
                    bw.write(COMMA_SPACE_CHAR_ARRAY);
                }
            }

            if (isPrettyFormat) {
                bw.write(IOUtil.LINE_SEPARATOR_UNIX);
                bw.write(nextIndentation);
            }

            if (element == null) {
                bw.write(NULL_CHAR_ARRAY);
            } else {
                write(element, config, false, nextIndentation, serializedObjects, bw);
            }
        }

        if (config.isBracketRootValue() || !isFirstCall) {
            if (isPrettyFormat && N.notEmpty(c)) {
                bw.write(IOUtil.LINE_SEPARATOR_UNIX);

                if (indentation != null) {
                    bw.write(indentation);
                }
            }

            bw.write(_BRACKET_R);
        }
    }

    @SuppressWarnings("unused")
    protected void writeMapEntity(final MapEntity mapEntity, final JsonSerConfig config, final boolean isFirstCall, final String indentation,
            final IdentityHashSet<Object> serializedObjects, final Type<Object> type, final BufferedJsonWriter bw) throws IOException {
        //    if (hasCircularReference(mapEntity, serializedObjects, config, bw)) {
        //        return;
        //    }

        final NamingPolicy jsonXmlNamingPolicy = config.getPropNamingPolicy();
        final boolean quotePropName = config.isQuotePropName();
        final boolean isPrettyFormat = config.isPrettyFormat();

        if (config.isBracketRootValue() || !isFirstCall) {
            bw.write(_BRACE_L);
        }

        {
            if (isPrettyFormat) {
                bw.write(IOUtil.LINE_SEPARATOR_UNIX);

                if (indentation != null) {
                    bw.write(indentation);
                }

                bw.write(config.getIndentation());
            }
        }

        if (quotePropName) {
            bw.write(_DOUBLE_QUOTE);
            bw.write(mapEntity.entityName());
            bw.write(_DOUBLE_QUOTE);
        } else {
            bw.write(mapEntity.entityName());
        }

        bw.write(COLON_SPACE_CHAR_ARRAY);

        bw.write(_BRACE_L);

        if (!mapEntity.isEmpty()) {
            final String nextIndentation = isPrettyFormat
                    ? ((indentation == null ? Strings.EMPTY : indentation) + config.getIndentation() + config.getIndentation())
                    : null;
            int i = 0;

            for (final String propName : mapEntity.keySet()) {
                if (i++ > 0) {
                    if (isPrettyFormat) {
                        bw.write(_COMMA);
                    } else {
                        bw.write(COMMA_SPACE_CHAR_ARRAY);
                    }
                }

                if (isPrettyFormat) {
                    bw.write(IOUtil.LINE_SEPARATOR_UNIX);
                    bw.write(nextIndentation);
                }

                if (quotePropName) {
                    bw.write(_DOUBLE_QUOTE);
                    bw.write(jsonXmlNamingPolicy == null ? propName : jsonXmlNamingPolicy.convert(propName));
                    bw.write(_DOUBLE_QUOTE);
                } else {
                    bw.write(jsonXmlNamingPolicy == null ? propName : jsonXmlNamingPolicy.convert(propName));
                }

                bw.write(COLON_SPACE_CHAR_ARRAY);

                write(mapEntity.get(propName), config, false, nextIndentation, serializedObjects, bw);
            }
        }

        {
            if (isPrettyFormat && !mapEntity.isEmpty()) {
                bw.write(IOUtil.LINE_SEPARATOR_UNIX);

                if (indentation != null) {
                    bw.write(indentation);
                }

                bw.write(config.getIndentation());
            }
        }

        bw.write(_BRACE_R);

        if (config.isBracketRootValue() || !isFirstCall) {
            if (isPrettyFormat) {
                bw.write(IOUtil.LINE_SEPARATOR_UNIX);

                if (indentation != null) {
                    bw.write(indentation);
                }
            }

            bw.write(_BRACE_R);
        }
    }

    @SuppressWarnings("unused")
    protected void writeEntityId(final EntityId entityId, final JsonSerConfig config, final boolean isFirstCall, final String indentation,
            final IdentityHashSet<Object> serializedObjects, final Type<Object> type, final BufferedJsonWriter bw) throws IOException {
        //    if (hasCircularReference(entityId, serializedObjects, config, bw)) {
        //        return;
        //    }

        final NamingPolicy jsonXmlNamingPolicy = config.getPropNamingPolicy();
        final boolean quotePropName = config.isQuotePropName();
        final boolean isPrettyFormat = config.isPrettyFormat();

        if (config.isBracketRootValue() || !isFirstCall) {
            bw.write(_BRACE_L);
        }

        {
            if (isPrettyFormat) {
                bw.write(IOUtil.LINE_SEPARATOR_UNIX);

                if (indentation != null) {
                    bw.write(indentation);
                }

                bw.write(config.getIndentation());
            }
        }

        if (quotePropName) {
            bw.write(_DOUBLE_QUOTE);
            bw.write(entityId.entityName());
            bw.write(_DOUBLE_QUOTE);
        } else {
            bw.write(entityId.entityName());
        }

        bw.write(COLON_SPACE_CHAR_ARRAY);

        bw.write(_BRACE_L);

        if (!entityId.isEmpty()) {
            final String nextIndentation = isPrettyFormat
                    ? ((indentation == null ? Strings.EMPTY : indentation) + config.getIndentation() + config.getIndentation())
                    : null;
            int i = 0;

            for (final String propName : entityId.keySet()) {
                if (i++ > 0) {
                    if (isPrettyFormat) {
                        bw.write(_COMMA);
                    } else {
                        bw.write(COMMA_SPACE_CHAR_ARRAY);
                    }
                }

                if (isPrettyFormat) {
                    bw.write(IOUtil.LINE_SEPARATOR_UNIX);
                    bw.write(nextIndentation);
                }

                if (quotePropName) {
                    bw.write(_DOUBLE_QUOTE);
                    bw.write(jsonXmlNamingPolicy == null ? propName : jsonXmlNamingPolicy.convert(propName));
                    bw.write(_DOUBLE_QUOTE);
                } else {
                    bw.write(jsonXmlNamingPolicy == null ? propName : jsonXmlNamingPolicy.convert(propName));
                }

                bw.write(COLON_SPACE_CHAR_ARRAY);

                write(entityId.get(propName), config, false, nextIndentation, serializedObjects, bw);
            }
        }

        {
            if (isPrettyFormat && !entityId.isEmpty()) {
                bw.write(IOUtil.LINE_SEPARATOR_UNIX);

                if (indentation != null) {
                    bw.write(indentation);
                }

                bw.write(config.getIndentation());
            }
        }

        bw.write(_BRACE_R);

        if (config.isBracketRootValue() || !isFirstCall) {
            if (isPrettyFormat) {
                bw.write(IOUtil.LINE_SEPARATOR_UNIX);

                if (indentation != null) {
                    bw.write(indentation);
                }
            }

            bw.write(_BRACE_R);
        }
    }

    @SuppressWarnings({ "unused" })
    protected void writeDataset(final Dataset ds, final JsonSerConfig config, final boolean isFirstCall, final String indentation,
            final IdentityHashSet<Object> serializedObjects, final Type<Object> type, final BufferedJsonWriter bw) throws IOException {
        //    if (hasCircularReference(ds, serializedObjects, config, bw)) {
        //        return;
        //    }

        if (config.isWriteDatasetByRow()) {
            writeCollection(ds.toList(LinkedHashMap.class), config, isFirstCall, indentation, serializedObjects, type, bw);
            return;
        }

        final boolean quotePropName = config.isQuotePropName();
        final boolean isPrettyFormat = config.isPrettyFormat();
        final boolean writeColumnType = config.isWriteColumnType();

        final String nextIndentation = isPrettyFormat ? ((indentation == null ? Strings.EMPTY : indentation) + config.getIndentation()) : null;

        if (config.isBracketRootValue() || !isFirstCall) {
            bw.write(_BRACE_L);
        }

        final List<String> columnNames = ds.columnNames();

        {
            if (isPrettyFormat) {
                bw.write(IOUtil.LINE_SEPARATOR_UNIX);

                if (indentation != null) {
                    bw.write(indentation);
                }

                bw.write(config.getIndentation());
            }
        }

        if (quotePropName) {
            bw.write(_DOUBLE_QUOTE);
            bw.write(COLUMN_NAMES);
            bw.write(_DOUBLE_QUOTE);
        } else {
            bw.write(COLUMN_NAMES);
        }

        bw.write(COLON_SPACE_CHAR_ARRAY);

        write(columnNames, config, false, nextIndentation, serializedObjects, bw);

        if (isPrettyFormat) {
            bw.write(_COMMA);
        } else {
            bw.write(COMMA_SPACE_CHAR_ARRAY);
        }

        if (writeColumnType) {
            {
                if (isPrettyFormat) {
                    bw.write(IOUtil.LINE_SEPARATOR_UNIX);

                    if (indentation != null) {
                        bw.write(indentation);
                    }

                    bw.write(config.getIndentation());
                }
            }

            if (quotePropName) {
                bw.write(_DOUBLE_QUOTE);
                bw.write(COLUMN_TYPES);
                bw.write(_DOUBLE_QUOTE);
            } else {
                bw.write(COLUMN_TYPES);
            }

            bw.write(COLON_SPACE_CHAR_ARRAY);

            final List<String> types = Objectory.createList();
            Class<?> eleTypeClass;

            for (int i = 0, len = columnNames.size(); i < len; i++) {
                eleTypeClass = getElementType(ds.getColumn(i));

                types.add(eleTypeClass == null ? null : Type.of(eleTypeClass).name());
            }

            write(types, config, false, nextIndentation, serializedObjects, bw);

            Objectory.recycle(types);

            if (isPrettyFormat) {
                bw.write(_COMMA);
            } else {
                bw.write(COMMA_SPACE_CHAR_ARRAY);
            }
        }

        if (N.notEmpty(ds.getProperties())) {
            {
                if (isPrettyFormat) {
                    bw.write(IOUtil.LINE_SEPARATOR_UNIX);

                    if (indentation != null) {
                        bw.write(indentation);
                    }

                    bw.write(config.getIndentation());
                }
            }

            if (quotePropName) {
                bw.write(_DOUBLE_QUOTE);
                bw.write(PROPERTIES);
                bw.write(_DOUBLE_QUOTE);
            } else {
                bw.write(PROPERTIES);
            }

            bw.write(COLON_SPACE_CHAR_ARRAY);

            write(ds.getProperties(), config, false, nextIndentation, serializedObjects, bw);

            if (isPrettyFormat) {
                bw.write(_COMMA);
            } else {
                bw.write(COMMA_SPACE_CHAR_ARRAY);
            }
        }

        if (ds.isFrozen()) {
            {
                if (isPrettyFormat) {
                    bw.write(IOUtil.LINE_SEPARATOR_UNIX);

                    if (indentation != null) {
                        bw.write(indentation);
                    }

                    bw.write(config.getIndentation());
                }
            }

            if (quotePropName) {
                bw.write(_DOUBLE_QUOTE);
                bw.write(IS_FROZEN);
                bw.write(_DOUBLE_QUOTE);
            } else {
                bw.write(IS_FROZEN);
            }

            bw.write(COLON_SPACE_CHAR_ARRAY);

            bw.write(ds.isFrozen());

            if (isPrettyFormat) {
                bw.write(_COMMA);
            } else {
                bw.write(COMMA_SPACE_CHAR_ARRAY);
            }
        }

        {
            if (isPrettyFormat) {
                bw.write(IOUtil.LINE_SEPARATOR_UNIX);

                if (indentation != null) {
                    bw.write(indentation);
                }

                bw.write(config.getIndentation());
            }
        }

        if (quotePropName) {
            bw.write(_DOUBLE_QUOTE);
            bw.write(COLUMNS);
            bw.write(_DOUBLE_QUOTE);
        } else {
            bw.write(COLUMNS);
        }

        bw.write(COLON_SPACE_CHAR_ARRAY);
        bw.write(_BRACE_L);

        if (columnNames.size() > 0) {
            final String doubleIndentation = Strings.nullToEmpty(indentation) + Strings.nullToEmpty(config.getIndentation())
                    + Strings.nullToEmpty(config.getIndentation());
            String columnName = null;
            List<Object> column = null;

            for (int i = 0, len = columnNames.size(); i < len; i++) {
                columnName = columnNames.get(i);
                column = ds.getColumn(i);

                if (i > 0) {
                    if (isPrettyFormat) {
                        bw.write(_COMMA);
                    } else {
                        bw.write(COMMA_SPACE_CHAR_ARRAY);
                    }
                }

                if (isPrettyFormat) {
                    bw.write(IOUtil.LINE_SEPARATOR_UNIX);
                    bw.write(doubleIndentation);
                }

                if (quotePropName) {
                    bw.write(_DOUBLE_QUOTE);
                    bw.write(columnName);
                    bw.write(_DOUBLE_QUOTE);
                } else {
                    bw.write(columnName);
                }

                bw.write(COLON_SPACE_CHAR_ARRAY);

                write(column, config, false, doubleIndentation, serializedObjects, bw);
            }
        }

        {
            if (isPrettyFormat && columnNames.size() > 0) {
                bw.write(IOUtil.LINE_SEPARATOR_UNIX);

                if (indentation != null) {
                    bw.write(indentation);
                }

                bw.write(config.getIndentation());
            }
        }

        bw.write(_BRACE_R);

        if (config.isBracketRootValue() || !isFirstCall) {
            if (isPrettyFormat) {
                bw.write(IOUtil.LINE_SEPARATOR_UNIX);

                if (indentation != null) {
                    bw.write(indentation);
                }
            }

            bw.write(_BRACE_R);
        }
    }

    @SuppressWarnings({ "unused", "rawtypes" })
    protected void writeSheet(final Sheet sheet, final JsonSerConfig config, final boolean isFirstCall, final String indentation,
            final IdentityHashSet<Object> serializedObjects, final Type<Object> type, final BufferedJsonWriter bw) throws IOException {
        //    if (hasCircularReference(sheet, serializedObjects, config, bw)) {
        //        return;
        //    }

        final boolean quotePropName = config.isQuotePropName();
        final boolean isPrettyFormat = config.isPrettyFormat();
        final boolean writeRowColumnKeyType = config.isWriteRowColumnKeyType();
        final boolean writeColumnType = config.isWriteColumnType();

        final String nextIndentation = isPrettyFormat ? ((indentation == null ? Strings.EMPTY : indentation) + config.getIndentation()) : null;

        if (config.isBracketRootValue() || !isFirstCall) {
            bw.write(_BRACE_L);
        }

        if (writeRowColumnKeyType) {
            {
                if (isPrettyFormat) {
                    bw.write(IOUtil.LINE_SEPARATOR_UNIX);

                    if (indentation != null) {
                        bw.write(indentation);
                    }

                    bw.write(config.getIndentation());
                }
            }

            if (quotePropName) {
                bw.write(_DOUBLE_QUOTE);
                bw.write(ROW_KEY_TYPE);
                bw.write(_DOUBLE_QUOTE);
            } else {
                bw.write(ROW_KEY_TYPE);
            }

            bw.write(COLON_SPACE_CHAR_ARRAY);

            Class<?> eleTypeClass = getElementType(sheet.rowKeySet());
            final String rowKeyTypeName = eleTypeClass == null ? null : Type.of(eleTypeClass).name();

            if (rowKeyTypeName != null) {
                bw.write(_DOUBLE_QUOTE);
                bw.write(rowKeyTypeName);
                bw.write(_DOUBLE_QUOTE);
            } else {
                bw.write(NULL_CHAR_ARRAY);
            }

            if (isPrettyFormat) {
                bw.write(_COMMA);
            } else {
                bw.write(COMMA_SPACE_CHAR_ARRAY);
            }

            {
                if (isPrettyFormat) {
                    bw.write(IOUtil.LINE_SEPARATOR_UNIX);

                    if (indentation != null) {
                        bw.write(indentation);
                    }

                    bw.write(config.getIndentation());
                }
            }

            if (quotePropName) {
                bw.write(_DOUBLE_QUOTE);
                bw.write(COLUMN_KEY_TYPE);
                bw.write(_DOUBLE_QUOTE);
            } else {
                bw.write(COLUMN_KEY_TYPE);
            }

            bw.write(COLON_SPACE_CHAR_ARRAY);

            eleTypeClass = getElementType(sheet.columnKeySet());
            final String columnKeyTypeName = eleTypeClass == null ? null : Type.of(eleTypeClass).name();

            if (columnKeyTypeName != null) {
                bw.write(_DOUBLE_QUOTE);
                bw.write(columnKeyTypeName);
                bw.write(_DOUBLE_QUOTE);
            } else {
                bw.write(NULL_CHAR_ARRAY);
            }

            if (isPrettyFormat) {
                bw.write(_COMMA);
            } else {
                bw.write(COMMA_SPACE_CHAR_ARRAY);
            }
        }

        {
            if (isPrettyFormat) {
                bw.write(IOUtil.LINE_SEPARATOR_UNIX);

                if (indentation != null) {
                    bw.write(indentation);
                }

                bw.write(config.getIndentation());
            }
        }

        if (quotePropName) {
            bw.write(_DOUBLE_QUOTE);
            bw.write(ROW_KEY_SET);
            bw.write(_DOUBLE_QUOTE);
        } else {
            bw.write(ROW_KEY_SET);
        }

        bw.write(COLON_SPACE_CHAR_ARRAY);

        write(sheet.rowKeySet(), config, false, nextIndentation, serializedObjects, bw);

        if (isPrettyFormat) {
            bw.write(_COMMA);
        } else {
            bw.write(COMMA_SPACE_CHAR_ARRAY);
        }

        {
            if (isPrettyFormat) {
                bw.write(IOUtil.LINE_SEPARATOR_UNIX);

                if (indentation != null) {
                    bw.write(indentation);
                }

                bw.write(config.getIndentation());
            }
        }

        if (quotePropName) {
            bw.write(_DOUBLE_QUOTE);
            bw.write(COLUMN_KEY_SET);
            bw.write(_DOUBLE_QUOTE);
        } else {
            bw.write(COLUMN_KEY_SET);
        }

        bw.write(COLON_SPACE_CHAR_ARRAY);

        write(sheet.columnKeySet(), config, false, nextIndentation, serializedObjects, bw);

        if (isPrettyFormat) {
            bw.write(_COMMA);
        } else {
            bw.write(COMMA_SPACE_CHAR_ARRAY);
        }

        if (writeColumnType) {
            {
                if (isPrettyFormat) {
                    bw.write(IOUtil.LINE_SEPARATOR_UNIX);

                    if (indentation != null) {
                        bw.write(indentation);
                    }

                    bw.write(config.getIndentation());
                }
            }

            if (quotePropName) {
                bw.write(_DOUBLE_QUOTE);
                bw.write(COLUMN_TYPES);
                bw.write(_DOUBLE_QUOTE);
            } else {
                bw.write(COLUMN_TYPES);
            }

            bw.write(COLON_SPACE_CHAR_ARRAY);

            final List<String> types = Objectory.createList();
            Class<?> eleTypeClass = null;

            for (final Object columnKey : sheet.columnKeySet()) {
                eleTypeClass = getElementType(sheet.columnValues(columnKey));

                types.add(eleTypeClass == null ? null : Type.of(eleTypeClass).name());
            }

            write(types, config, false, nextIndentation, serializedObjects, bw);

            Objectory.recycle(types);

            if (isPrettyFormat) {
                bw.write(_COMMA);
            } else {
                bw.write(COMMA_SPACE_CHAR_ARRAY);
            }
        }

        {
            if (isPrettyFormat) {
                bw.write(IOUtil.LINE_SEPARATOR_UNIX);

                if (indentation != null) {
                    bw.write(indentation);
                }

                bw.write(config.getIndentation());
            }
        }

        if (quotePropName) {
            bw.write(_DOUBLE_QUOTE);
            bw.write(COLUMNS);
            bw.write(_DOUBLE_QUOTE);
        } else {
            bw.write(COLUMNS);
        }

        bw.write(COLON_SPACE_CHAR_ARRAY);
        bw.write(_BRACE_L);

        if (sheet.columnKeySet().size() > 0) {
            final String doubleIndentation = Strings.nullToEmpty(indentation) + Strings.nullToEmpty(config.getIndentation())
                    + Strings.nullToEmpty(config.getIndentation());
            String columnName = null;
            List column = null;
            int i = 0;

            for (final Object columnKey : sheet.columnKeySet()) {
                columnName = N.stringOf(columnKey);
                column = sheet.columnValues(columnKey);

                if (i++ > 0) {
                    if (isPrettyFormat) {
                        bw.write(_COMMA);
                    } else {
                        bw.write(COMMA_SPACE_CHAR_ARRAY);
                    }
                }

                if (isPrettyFormat) {
                    bw.write(IOUtil.LINE_SEPARATOR_UNIX);
                    bw.write(doubleIndentation);
                }

                if (quotePropName) {
                    bw.write(_DOUBLE_QUOTE);
                    bw.write(columnName);
                    bw.write(_DOUBLE_QUOTE);
                } else {
                    bw.write(columnName);
                }

                bw.write(COLON_SPACE_CHAR_ARRAY);

                write(column, config, false, doubleIndentation, serializedObjects, bw);
            }
        }

        {
            if (isPrettyFormat && sheet.columnKeySet().size() > 0) {
                bw.write(IOUtil.LINE_SEPARATOR_UNIX);

                if (indentation != null) {
                    bw.write(indentation);
                }

                bw.write(config.getIndentation());
            }
        }

        bw.write(_BRACE_R);

        if (config.isBracketRootValue() || !isFirstCall) {
            if (isPrettyFormat) {
                bw.write(IOUtil.LINE_SEPARATOR_UNIX);

                if (indentation != null) {
                    bw.write(indentation);
                }
            }

            bw.write(_BRACE_R);
        }
    }

    private Class<?> getElementType(final Collection<?> c) {
        Class<?> cls = null;
        Class<?> eleClass = null;

        for (final Object e : c) {
            if (e != null) {
                eleClass = e.getClass();

                if ((cls == null) || eleClass.isAssignableFrom(cls)) {
                    cls = eleClass;
                } else if (cls.isAssignableFrom(eleClass)) {
                    // continue;
                } else {
                    return null; // end here because there are two incompatible type elements.
                }
            }
        }

        return cls;
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation deserializes a JSON string into an object of the specified type,
     * with full support for complex types including arrays, collections, maps, beans, datasets,
     * sheets, and entity IDs.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JsonDeserConfig config = new JsonDeserConfig()
     *     .setIgnoreUnmatchedProperty(true)
     *     .setReadNullToEmpty(true);
     *
     * String json = "{\"name\":\"John\",\"age\":30}";
     * User user = parser.deserialize(json, config, Type.of(User.class));
     *
     * // For complex collections
     * String jsonArray = "[{\"id\":1},{\"id\":2}]";
     * List<Item> items = parser.deserialize(jsonArray, config, new TypeReference<List<Item>>() {});
     * }</pre>
     *
     * @param <T> the type of the target object
     * @param source the JSON string to deserialize; may be {@code null} or empty
     * @param config the deserialization configuration to use; may be {@code null} to use default configuration
     * @param targetType the type of the target object to deserialize into; must not be {@code null}
     * @return the deserialized object of type {@code T}, or {@code null} if the source is {@code null}
     * @throws UncheckedIOException if an I/O error occurs during deserialization
     * @throws ParsingException if the JSON structure is invalid or doesn't match the target type
     */
    @Override
    public <T> T deserialize(String source, JsonDeserConfig config, Type<? extends T> targetType) {
        final JsonDeserConfig configToUse = check(config);

        if ((Strings.isEmpty(source) && configToUse.isReadNullToEmpty()) || (source != null && source.isEmpty())) {
            return emptyOrDefault(targetType);
        } else if (source == null) {
            return targetType.defaultValue();
        }

        final char[] cbuf = Objectory.createCharArrayBuffer();

        try {
            final JsonReader jr = JsonStringReader.parse(source, cbuf);

            return read(source, jr, configToUse, targetType.javaType(), targetType);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            Objectory.recycle(cbuf);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation delegates to {@link #deserialize(String, JsonDeserConfig, Type)}
     * after converting the class to a Type.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JsonDeserConfig config = new JsonDeserConfig();
     * String json = "{\"name\":\"John\",\"age\":30}";
     * User user = parser.deserialize(json, config, User.class);
     * }</pre>
     *
     * @param <T> the type of the target class
     * @param source the JSON string to deserialize; may be {@code null} or empty
     * @param config the deserialization configuration to use; may be {@code null} to use default configuration
     * @param targetClass the class of the target object to deserialize into; must not be {@code null}
     * @return the deserialized object of type {@code T}, or {@code null} if the source is {@code null}
     * @throws UncheckedIOException if an I/O error occurs during deserialization
     * @throws ParsingException if the JSON structure is invalid or doesn't match the target class
     */
    @Override
    public <T> T deserialize(final String source, final JsonDeserConfig config, final Class<? extends T> targetClass) {
        return deserialize(source, config, Type.of(targetClass));
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation efficiently parses a substring of a JSON string without creating
     * an intermediate String object, which improves performance when working with large strings.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String json = "prefix{\"name\":\"John\",\"age\":30}suffix";
     * JsonDeserConfig config = new JsonDeserConfig();
     *
     * // Parse only the JSON object part (indices 6 to 30)
     * User user = parser.deserialize(json, 6, 30, config, Type.of(User.class));
     * }</pre>
     *
     * @param <T> the type of the target object
     * @param source the JSON string containing the data to parse; must not be {@code null}
     * @param fromIndex the starting index (inclusive) of the JSON content
     * @param toIndex the ending index (exclusive) of the JSON content
     * @param config the deserialization configuration to use; may be {@code null} to use default configuration
     * @param targetType the type of the target object to deserialize into; must not be {@code null}
     * @return the deserialized object of type {@code T}
     * @throws IndexOutOfBoundsException if the indices are out of bounds or fromIndex &gt; toIndex
     * @throws UncheckedIOException if an I/O error occurs during deserialization
     * @throws ParsingException if the JSON structure is invalid or doesn't match the target type
     */
    @Override
    public <T> T deserialize(String source, int fromIndex, int toIndex, JsonDeserConfig config, Type<? extends T> targetType) {
        N.checkFromToIndex(fromIndex, toIndex, N.len(source));

        final JsonDeserConfig configToUse = check(config);

        if ((Strings.isEmpty(source) && configToUse.isReadNullToEmpty()) || (source != null && fromIndex == toIndex)) {
            return emptyOrDefault(targetType);
        } else if (source == null) {
            return targetType.defaultValue();
        }

        final char[] cbuf = Objectory.createCharArrayBuffer();

        try {
            final JsonReader jr = JsonStringReader.parse(source, fromIndex, toIndex, cbuf);

            Object sourceToUse = fromIndex == 0 && toIndex == N.len(source) ? source : CharBuffer.wrap(source, fromIndex, toIndex);
            return read(sourceToUse, jr, configToUse, targetType.javaType(), targetType);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            Objectory.recycle(cbuf);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation delegates to {@link #deserialize(String, int, int, JsonDeserConfig, Type)}
     * after converting the class to a Type.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String json = "prefix{\"name\":\"John\"}suffix";
     * JsonDeserConfig config = new JsonDeserConfig();
     * User user = parser.deserialize(json, 6, 23, config, User.class);
     * }</pre>
     *
     * @param <T> the type of the target class
     * @param source the JSON string containing the data to parse; must not be {@code null}
     * @param fromIndex the starting index (inclusive) of the JSON content
     * @param toIndex the ending index (exclusive) of the JSON content
     * @param config the deserialization configuration to use; may be {@code null} to use default configuration
     * @param targetClass the class of the target object to deserialize into; must not be {@code null}
     * @return the deserialized object of type {@code T}
     * @throws IndexOutOfBoundsException if the indices are out of bounds or fromIndex &gt; toIndex
     * @throws UncheckedIOException if an I/O error occurs during deserialization
     * @throws ParsingException if the JSON structure is invalid or doesn't match the target class
     */
    @Override
    public <T> T deserialize(final String source, final int fromIndex, final int toIndex, final JsonDeserConfig config, final Class<? extends T> targetClass)
            throws IndexOutOfBoundsException {
        return deserialize(source, fromIndex, toIndex, config, Type.of(targetClass));
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation reads and deserializes a JSON file into an object of the specified type.
     * The file is automatically closed after reading, even if an exception occurs.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JsonDeserConfig config = new JsonDeserConfig()
     *     .setIgnoreUnmatchedProperty(true);
     *
     * File jsonFile = new File("user.json");
     * User user = parser.deserialize(jsonFile, config, Type.of(User.class));
     * }</pre>
     *
     * @param <T> the type of the target object
     * @param source the file containing the JSON data to deserialize; must not be {@code null}
     * @param config the deserialization configuration to use; may be {@code null} to use default configuration
     * @param targetType the type of the target object to deserialize into; must not be {@code null}
     * @return the deserialized object of type {@code T}
     * @throws UncheckedIOException if an I/O error occurs during file reading or deserialization
     * @throws ParsingException if the JSON structure is invalid or doesn't match the target type
     */
    @Override
    public <T> T deserialize(File source, JsonDeserConfig config, Type<? extends T> targetType) {
        Reader reader = null;

        try {
            reader = IOUtil.newFileReader(source);

            return deserialize(reader, config, targetType);
        } finally {
            IOUtil.closeQuietly(reader);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation delegates to {@link #deserialize(File, JsonDeserConfig, Type)}
     * after converting the class to a Type.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JsonDeserConfig config = new JsonDeserConfig();
     * File jsonFile = new File("user.json");
     * User user = parser.deserialize(jsonFile, config, User.class);
     * }</pre>
     *
     * @param <T> the type of the target class
     * @param source the file containing the JSON data to deserialize; must not be {@code null}
     * @param config the deserialization configuration to use; may be {@code null} to use default configuration
     * @param targetClass the class of the target object to deserialize into; must not be {@code null}
     * @return the deserialized object of type {@code T}
     * @throws UncheckedIOException if an I/O error occurs during file reading or deserialization
     * @throws ParsingException if the JSON structure is invalid or doesn't match the target class
     */
    @Override
    public <T> T deserialize(final File source, final JsonDeserConfig config, final Class<? extends T> targetClass) {
        return deserialize(source, config, Type.of(targetClass));
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation reads and deserializes JSON from an input stream into an object of the specified type.
     * The input stream is not closed by this method and should be managed by the caller.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JsonDeserConfig config = new JsonDeserConfig();
     *
     * try (InputStream is = new FileInputStream("user.json")) {
     *     User user = parser.deserialize(is, config, Type.of(User.class));
     * }
     * }</pre>
     *
     * @param <T> the type of the target object
     * @param source the input stream containing the JSON data to deserialize; must not be {@code null}
     * @param config the deserialization configuration to use; may be {@code null} to use default configuration
     * @param targetType the type of the target object to deserialize into; must not be {@code null}
     * @return the deserialized object of type {@code T}
     * @throws UncheckedIOException if an I/O error occurs during stream reading or deserialization
     * @throws ParsingException if the JSON structure is invalid or doesn't match the target type
     */
    @Override
    public <T> T deserialize(InputStream source, JsonDeserConfig config, Type<? extends T> targetType) {
        // No need to close the reader because the InputStream will/should be closely externally.
        final Reader reader = IOUtil.newInputStreamReader(source); // NOSONAR

        return deserialize(reader, config, targetType);
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation delegates to {@link #deserialize(InputStream, JsonDeserConfig, Type)}
     * after converting the class to a Type.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JsonDeserConfig config = new JsonDeserConfig();
     *
     * try (InputStream is = new FileInputStream("user.json")) {
     *     User user = parser.deserialize(is, config, User.class);
     * }
     * }</pre>
     *
     * @param <T> the type of the target class
     * @param source the input stream containing the JSON data to deserialize; must not be {@code null}
     * @param config the deserialization configuration to use; may be {@code null} to use default configuration
     * @param targetClass the class of the target object to deserialize into; must not be {@code null}
     * @return the deserialized object of type {@code T}
     * @throws UncheckedIOException if an I/O error occurs during stream reading or deserialization
     * @throws ParsingException if the JSON structure is invalid or doesn't match the target class
     */
    @Override
    public <T> T deserialize(final InputStream source, final JsonDeserConfig config, final Class<? extends T> targetClass) {
        return deserialize(source, config, Type.of(targetClass));
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation reads and deserializes JSON from a Reader into an object of the specified type.
     * The reader is not closed by this method and should be managed by the caller.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JsonDeserConfig config = new JsonDeserConfig();
     *
     * try (Reader reader = new FileReader("user.json")) {
     *     User user = parser.deserialize(reader, config, Type.of(User.class));
     * }
     * }</pre>
     *
     * @param <T> the type of the target object
     * @param source the reader containing the JSON data to deserialize; must not be {@code null}
     * @param config the deserialization configuration to use; may be {@code null} to use default configuration
     * @param targetType the type of the target object to deserialize into; must not be {@code null}
     * @return the deserialized object of type {@code T}
     * @throws UncheckedIOException if an I/O error occurs during reading or deserialization
     * @throws ParsingException if the JSON structure is invalid or doesn't match the target type
     */
    @Override
    public <T> T deserialize(Reader source, JsonDeserConfig config, final Type<? extends T> targetType) {
        return read(source, config, targetType);
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation delegates to {@link #deserialize(Reader, JsonDeserConfig, Type)}
     * after converting the class to a Type.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JsonDeserConfig config = new JsonDeserConfig();
     *
     * try (Reader reader = new FileReader("user.json")) {
     *     User user = parser.deserialize(reader, config, User.class);
     * }
     * }</pre>
     *
     * @param <T> the type of the target class
     * @param source the reader containing the JSON data to deserialize; must not be {@code null}
     * @param config the deserialization configuration to use; may be {@code null} to use default configuration
     * @param targetClass the class of the target object to deserialize into; must not be {@code null}
     * @return the deserialized object of type {@code T}
     * @throws UncheckedIOException if an I/O error occurs during reading or deserialization
     * @throws ParsingException if the JSON structure is invalid or doesn't match the target class
     */
    @Override
    public <T> T deserialize(final Reader source, final JsonDeserConfig config, final Class<? extends T> targetClass) {
        return deserialize(source, config, Type.of(targetClass));
    }

    protected <T> T read(final Reader source, final JsonDeserConfig config, final Type<? extends T> targetType) {
        final JsonDeserConfig configToUse = check(config);
        final char[] rbuf = Objectory.createCharArrayBuffer();
        final char[] cbuf = Objectory.createCharArrayBuffer();

        try {
            final JsonReader jr = JsonStreamReader.parse(source, rbuf, cbuf);

            return read(source, jr, configToUse, targetType.javaType(), targetType);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            Objectory.recycle(cbuf);
            Objectory.recycle(rbuf);
        }
    }

    protected <T> T read(final Object source, final JsonReader jr, final JsonDeserConfig config, final Class<? extends T> targetClass,
            final Type<? extends T> targetType) throws IOException {
        return read(source, jr, UNDEFINED, config, true, targetClass, targetType);
    }

    @SuppressWarnings("unchecked")
    protected <T> T read(final Object source, final JsonReader jr, final int lastToken, final JsonDeserConfig config, final boolean isFirstCall,
            final Class<? extends T> targetClass, final Type<? extends T> targetType) throws IOException {
        switch (targetType.serializationType()) {
            case SERIALIZABLE:
                if (targetType.isArray()) {
                    return readArray(jr, config, null, isFirstCall, targetClass, targetType, null);
                } else if (targetType.isCollection()) {
                    return readCollection(jr, config, null, null, isFirstCall, targetClass, targetType, null);
                } else {
                    return (T) readNullToEmpty(targetType, targetType.valueOf(source instanceof String ? (String) source
                            : (source instanceof Reader ? IOUtil.readAllToString(((Reader) source)) : source.toString())), config.isReadNullToEmpty());
                }

            case ENTITY:
                return readBean(jr, config, isFirstCall, targetClass, targetType);

            case MAP:
                return readMap(jr, config, null, isFirstCall, targetClass, targetType, null);

            case ARRAY:
                return readArray(jr, config, null, isFirstCall, targetClass, targetType, null);

            case COLLECTION:
                return readCollection(jr, config, null, null, isFirstCall, targetClass, targetType, null);

            case MAP_ENTITY:
                return readMapEntity(jr, config, isFirstCall, targetClass, targetType);

            case DATA_SET:
                return readDataset(jr, lastToken, config, isFirstCall, targetClass, targetType);

            case SHEET:
                return readSheet(jr, lastToken, config, isFirstCall, targetClass, targetType);

            case ENTITY_ID:
                return readEntityId(jr, config, isFirstCall, targetClass, targetType);

            default:
                final int firstTokenToUse = isFirstCall ? jr.nextToken() : lastToken;

                if (Object.class.equals(targetClass)) {
                    if (firstTokenToUse == START_BRACE) {
                        return (T) readMap(jr, config, null, false, Map.class, null, null);
                    } else if (firstTokenToUse == START_BRACKET) {
                        return (T) readCollection(jr, config, null, null, false, List.class, null, null);
                    }
                }

                throw new ParsingException("Unsupported class: " + ClassUtil.getCanonicalClassName(targetType.javaType())
                        + ". Only Array/List/Map and Bean class with getter/setter methods are supported", firstTokenToUse);
        }
    }

    @SuppressWarnings("unused")
    protected <T> T readBean(final JsonReader jr, final JsonDeserConfig config, final boolean isFirstCall, final Class<? extends T> targetClass,
            final Type<? extends T> targetType) throws IOException {
        final boolean hasValueTypes = config.hasValueTypes();
        final boolean ignoreUnmatchedProperty = config.isIgnoreUnmatchedProperty();
        final boolean ignoreNullOrEmpty = config.isIgnoreNullOrEmpty();
        final boolean readNullToEmpty = config.isReadNullToEmpty();
        final Collection<String> ignoredClassPropNames = config.getIgnoredPropNames(targetClass);
        final BeanInfo beanInfo = ParserUtil.getBeanInfo(targetType.reflectType());
        final Object result = beanInfo.createBeanResult();

        PropInfo propInfo = null;
        String propName = null;
        Object propValue = null;
        boolean isPropName = true;
        Type<Object> propType = null;

        final int firstToken = isFirstCall ? jr.nextToken() : START_BRACE;

        if (firstToken == EOF) {
            if (Strings.isNotEmpty(jr.getText())) {
                throw new ParsingException("Can't parse: " + jr.getText(), firstToken); //NOSONAR
            }

            return null; // result;
        }

        // for (int token = firstToken == START_BRACE ? jr.nextToken() :
        // firstToken;; token = isPropName ? jr.nextNameToken() :
        // jr.nextToken()) { // TODO .Why it's even slower by jr.nextNameToken
        // which has less comparison. Fuck???!!!...
        for (int token = firstToken == START_BRACE ? jr.nextToken() : firstToken;; token = jr.nextToken(propInfo == null ? strType : propInfo.jsonXmlType)) {
            switch (token) {
                case START_DOUBLE_QUOTE, START_SINGLE_QUOTE:

                    break;

                case END_DOUBLE_QUOTE, END_SINGLE_QUOTE:

                    if (isPropName) {
                        // propName = jr.getText();
                        // propName = jr.readPropName(beanInfo);
                        // propInfo = beanInfo.getPropInfo(propName);

                        propInfo = jr.readPropInfo(beanInfo);

                        if (propInfo == null) {
                            propName = jr.getText();
                            propInfo = beanInfo.getPropInfo(propName);
                        } else {
                            propName = propInfo.name;
                        }

                        if (propInfo == null) {
                            propType = null;
                        } else {
                            propType = hasValueTypes ? config.getValueType(propName, propInfo.jsonXmlType) : propInfo.jsonXmlType;
                        }

                        if (propName != null && ignoredClassPropNames != null && ignoredClassPropNames.contains(propName)) {
                            break;
                        }

                        if (propInfo == null) {
                            if (ignoreUnmatchedProperty) {
                                break;
                            } else {
                                throw new ParsingException("Unknown property: " + propName);
                            }
                        }
                    } else {
                        if (propInfo == null || propInfo.jsonXmlExpose == JsonXmlField.Direction.SERIALIZE_ONLY
                                || (propName != null && ignoredClassPropNames != null && ignoredClassPropNames.contains(propName))) {
                            // ignore.
                        } else {
                            propValue = readValue(jr, readNullToEmpty, propInfo, propType == null ? propInfo.jsonXmlType : propType);
                            setPropValue(propInfo, propValue, result, ignoreNullOrEmpty);
                        }
                    }

                    break;

                case COLON:

                    if (isPropName) {
                        isPropName = false;

                        if (jr.hasText()) {
                            propName = jr.getText();
                            propInfo = beanInfo.getPropInfo(propName);

                            if (propInfo == null) {
                                propType = null;
                            } else {
                                propType = hasValueTypes ? config.getValueType(propName, propInfo.jsonXmlType) : propInfo.jsonXmlType;
                            }

                            if (propName != null && ignoredClassPropNames != null && ignoredClassPropNames.contains(propName)) {
                                break;
                            }

                            if (propInfo == null) {
                                if (ignoreUnmatchedProperty) {
                                    break;
                                } else {
                                    throw new ParsingException("Unknown property: " + propName);
                                }
                            }
                        }
                    } else {
                        throw new ParsingException(getErrorMsg(jr, token), token);
                    }

                    break;

                case COMMA:

                    if (isPropName) {
                        throw new ParsingException(getErrorMsg(jr, token), token);
                    } else {
                        isPropName = true;

                        if (jr.hasText()) {
                            if (propInfo == null || propInfo.jsonXmlExpose == JsonXmlField.Direction.SERIALIZE_ONLY
                                    || (propName != null && ignoredClassPropNames != null && ignoredClassPropNames.contains(propName))) {
                                // ignore.
                            } else {
                                propValue = readValue(jr, readNullToEmpty, propInfo, propType == null ? propInfo.jsonXmlType : propType);
                                setPropValue(propInfo, propValue, result, ignoreNullOrEmpty);
                            }
                        }
                    }

                    break;

                case START_BRACE:

                    if (isPropName) {
                        throw new ParsingException(getErrorMsg(jr, token), token);
                    }

                    if (propInfo == null || propInfo.jsonXmlExpose == JsonXmlField.Direction.SERIALIZE_ONLY
                            || (propName != null && ignoredClassPropNames != null && ignoredClassPropNames.contains(propName))) {
                        readMap(jr, defaultJsonDeserConfig, null, false, Map.class, null, null);
                    } else {
                        if (propInfo.isJsonRawValue && propInfo.jsonXmlType.isCharSequence()) {
                            final StringBuilder sb = Objectory.createStringBuilder();
                            sb.append('{');

                            try {
                                int startBraceCount = 1;
                                int nextToken = 0;

                                while (startBraceCount > 0) {
                                    nextToken = jr.nextToken();

                                    if (nextToken == START_BRACE) {
                                        startBraceCount++;
                                    } else if (nextToken == END_BRACE) {
                                        startBraceCount--;
                                    }

                                    sb.append(jr.getText());

                                    if (nextToken == EOF) {
                                        break;
                                    } else if (nextToken == COMMA || nextToken == COLON) {
                                        sb.append(eventChars[nextToken]);
                                        sb.append(' ');
                                    } else {
                                        sb.append(eventChars[nextToken]);
                                    }
                                }

                                propValue = sb.toString();
                            } finally {
                                Objectory.recycle(sb);
                            }
                        } else {
                            propValue = readBracedValue(jr, config, propType);
                        }

                        setPropValue(propInfo, propValue, result, ignoreNullOrEmpty);
                    }

                    break;

                case START_BRACKET:

                    if (isPropName) {
                        throw new ParsingException(getErrorMsg(jr, token), token);
                    }

                    if (propInfo == null || propInfo.jsonXmlExpose == JsonXmlField.Direction.SERIALIZE_ONLY
                            || (propName != null && ignoredClassPropNames != null && ignoredClassPropNames.contains(propName))) {
                        readCollection(jr, defaultJsonDeserConfig, null, Strings.isEmpty(propName) ? null : config.getPropHandler(propName), false, List.class,
                                null, null);
                    } else {
                        if (propInfo.isJsonRawValue && propInfo.jsonXmlType.isCharSequence()) {
                            final StringBuilder sb = Objectory.createStringBuilder();
                            sb.append('[');

                            try {
                                int startBracketCount = 1;
                                int nextToken = 0;

                                while (startBracketCount > 0) {
                                    nextToken = jr.nextToken();

                                    if (nextToken == START_BRACKET) {
                                        startBracketCount++;
                                    } else if (nextToken == END_BRACKET) {
                                        startBracketCount--;
                                    }

                                    sb.append(jr.getText());

                                    if (nextToken == EOF) {
                                        break;
                                    } else if (nextToken == COMMA || nextToken == COLON) {
                                        sb.append(eventChars[nextToken]);
                                        sb.append(' ');
                                    } else {
                                        sb.append(eventChars[nextToken]);
                                    }
                                }

                                propValue = sb.toString();
                            } finally {
                                Objectory.recycle(sb);
                            }
                        } else {
                            propValue = readBracketedValue(jr, config, Strings.isEmpty(propName) ? null : config.getPropHandler(propName), propType);
                        }

                        setPropValue(propInfo, propValue, result, ignoreNullOrEmpty);
                    }

                    break;

                case END_BRACE, EOF:

                    if ((isPropName && propInfo != null) /* check for empty JSON text {} */
                            || (isPropName && Strings.isEmpty(propName) && jr.hasText()) /*check for invalid JSON text: {abc}*/) {
                        throw new ParsingException(getErrorMsg(jr, token), token);
                    } else if ((firstToken == START_BRACE) == (token != END_BRACE)) {
                        throw new ParsingException("The JSON text should be wrapped or unwrapped with \"[]\" or \"{}\"", token); //NOSONAR
                    } else {
                        if (jr.hasText()) {
                            if (propInfo == null || propInfo.jsonXmlExpose == JsonXmlField.Direction.SERIALIZE_ONLY
                                    || (propName != null && ignoredClassPropNames != null && ignoredClassPropNames.contains(propName))) {
                                // ignore.
                            } else {
                                propValue = readValue(jr, readNullToEmpty, propInfo, propType == null ? propInfo.jsonXmlType : propType);
                                setPropValue(propInfo, propValue, result, ignoreNullOrEmpty);
                            }
                        }
                    }

                    return beanInfo.finishBeanResult(result);
                default:
                    throw new ParsingException(getErrorMsg(jr, token), token);
            }
        }
    }

    <T> void setPropValue(final PropInfo propInfo, final Object propValue, final T result, final boolean ignoreNullOrEmpty) {
        if (!ignoreNullOrEmpty || !isNullOrEmptyValue(propInfo.jsonXmlType, propValue)) {
            propInfo.setPropValue(result, propValue);
        }
    }

    @SuppressWarnings("rawtypes")
    private boolean isNullOrEmptyValue(final Type<?> type, final Object value) {
        if (value == null) {
            return true;
        } else if (type.isCharSequence()) {
            return value instanceof CharSequence && ((CharSequence) value).isEmpty();
        } else if (type.isCollection()) {
            return value instanceof Collection && ((Collection) value).size() == 0;
        } else if (type.isArray()) {
            return value.getClass().isArray() && Array.getLength(value) == 0;
        } else if (type.isMap()) {
            return value instanceof Map && ((Map) value).isEmpty();
        }

        return false;
    }

    private static final BiConsumer<Collection<Object>, Object> DEFAULT_PROP_HANDLER = Collection::add;

    @SuppressWarnings("unchecked")
    protected <T> T readMap(final JsonReader jr, final JsonDeserConfig config, Type<?> propType, final boolean isFirstCall,
            final Class<? extends T> targetClass, final Type<? extends T> targetType, final Map<Object, Object> output) throws IOException {
        Type<?> keyType = defaultKeyType;

        if (propType != null && propType.isMap() && !propType.parameterTypes()[0].isObject()) {
            keyType = propType.parameterTypes()[0];
        } else if (targetType != null && targetType.isMap() && !targetType.parameterTypes()[0].isObject()) {
            keyType = targetType.parameterTypes()[0];
        } else if ((propType == null || !propType.isObject()) && (config.getMapKeyType() != null && !config.getMapKeyType().isObject())) {
            keyType = config.getMapKeyType();
        }

        final boolean isStringKey = String.class == keyType.javaType();

        Type<?> valueType = defaultValueType;

        if (propType != null && propType.isMap() && !propType.parameterTypes()[1].isObject()) {
            valueType = propType.parameterTypes()[1];
        } else if (targetType != null && targetType.isMap() && !targetType.parameterTypes()[1].isObject()) {
            valueType = targetType.parameterTypes()[1];
        } else if ((propType == null || !propType.isObject()) && (config.getMapValueType() != null && !config.getMapValueType().isObject())) {
            valueType = config.getMapValueType();
        }

        final boolean hasValueTypes = config.hasValueTypes();
        final Collection<String> ignoredClassPropNames = config.getIgnoredPropNames(Map.class);
        final boolean ignoreNullOrEmpty = config.isIgnoreNullOrEmpty();
        final boolean readNullToEmpty = config.isReadNullToEmpty();

        final Tuple2<Function<Class<?>, Object>, Function<Object, Object>> creatorAndConverter = getCreatorAndConverterForTargetType(targetClass, null);

        @SuppressWarnings("rawtypes")
        final Map<Object, Object> result = output == null
                ? (Map.class.isAssignableFrom(targetClass) ? (Map<Object, Object>) creatorAndConverter._1.apply(targetClass)
                        : N.newMap(Map.class.equals(targetClass) ? config.getMapInstanceType() : (Class<Map>) targetClass))
                : output;

        String propName = null;
        boolean isKey = true;
        propType = null;

        Object key = null;
        Object value = null;

        final int firstToken = isFirstCall ? jr.nextToken() : START_BRACE;

        if (firstToken == EOF) {
            //    if (isFirstCall && N.notEmpty(jr.getText())) {
            //        throw new ParseException("Can't parse: " + jr.getText());
            //    }
            //
            //    return null; // (T) result;

            return (T) creatorAndConverter._2.apply(result);
        }

        for (int token = firstToken == START_BRACE ? jr.nextToken() : firstToken;; token = jr.nextToken()) {
            switch (token) {
                case START_DOUBLE_QUOTE, START_SINGLE_QUOTE:

                    break;

                case END_DOUBLE_QUOTE, END_SINGLE_QUOTE:

                    if (isKey) {
                        key = readValue(jr, readNullToEmpty, keyType);
                        propName = isStringKey ? (String) key : (key == null ? "null" : key.toString());
                        propType = hasValueTypes ? config.getValueType(propName, valueType) : valueType;
                    } else {
                        if (key != null && ignoredClassPropNames != null && ignoredClassPropNames.contains(key.toString())) {
                            // ignore.
                        } else {
                            value = readValue(jr, readNullToEmpty, propType);

                            if (!ignoreNullOrEmpty || (!isNullOrEmptyValue(keyType, key) && !isNullOrEmptyValue(propType, value))) {
                                result.put(key, value);
                            }
                        }
                    }

                    break;

                case COLON:

                    if (isKey) {
                        isKey = false;

                        if (jr.hasText()) {
                            key = readValue(jr, readNullToEmpty, keyType);
                            propName = isStringKey ? (String) key : (key == null ? "null" : key.toString());
                            propType = hasValueTypes ? config.getValueType(propName, valueType) : valueType;
                        }
                    } else {
                        throw new ParsingException(getErrorMsg(jr, token), token);
                    }

                    break;

                case COMMA:

                    if (isKey) {
                        throw new ParsingException(getErrorMsg(jr, token), token);
                    } else {
                        isKey = true;

                        if (jr.hasText()) {
                            if (key != null && ignoredClassPropNames != null && ignoredClassPropNames.contains(key.toString())) {
                                // ignore.
                            } else {
                                value = readValue(jr, readNullToEmpty, propType);

                                if (!ignoreNullOrEmpty || (!isNullOrEmptyValue(keyType, key) && !isNullOrEmptyValue(propType, value))) {
                                    result.put(key, value);
                                }
                            }
                        }
                    }

                    break;

                case START_BRACE:

                    if (isKey) {
                        key = readBracedValue(jr, config, keyType);
                        propName = isStringKey ? (String) key : (key == null ? "null" : key.toString());
                        propType = hasValueTypes ? config.getValueType(propName, valueType) : valueType;
                    } else {
                        if (propName != null && ignoredClassPropNames != null && ignoredClassPropNames.contains(propName)) {
                            readMap(jr, defaultJsonDeserConfig, null, false, Map.class, null, null);
                        } else {
                            //noinspection DataFlowIssue
                            value = readBracedValue(jr, config, propType);

                            if (!ignoreNullOrEmpty || (!isNullOrEmptyValue(keyType, key) && !isNullOrEmptyValue(propType, value))) {
                                result.put(key, value);
                            }
                        }
                    }

                    break;

                case START_BRACKET:

                    if (isKey) {
                        key = readBracketedValue(jr, config, null, keyType);
                        propName = isStringKey ? (String) key : (key == null ? "null" : key.toString());
                        propType = hasValueTypes ? config.getValueType(propName, valueType) : valueType;
                    } else {
                        if (propName != null && ignoredClassPropNames != null && ignoredClassPropNames.contains(propName)) {
                            readCollection(jr, defaultJsonDeserConfig, null, Strings.isEmpty(propName) ? null : config.getPropHandler(propName), false,
                                    List.class, null, null);
                        } else {
                            //noinspection DataFlowIssue
                            value = readBracketedValue(jr, config,
                                    key instanceof String && Strings.isNotEmpty((String) key) ? config.getPropHandler((String) key) : null, propType);

                            if (!ignoreNullOrEmpty || (!isNullOrEmptyValue(keyType, key) && !isNullOrEmptyValue(propType, value))) {
                                result.put(key, value);
                            }
                        }
                    }

                    break;

                case END_BRACE, EOF:

                    if (isKey && key != null /* check for empty JSON text {} */
                            || (isKey && key == null && jr.hasText()) /*check for invalid JSON text: {abc}*/) {
                        throw new ParsingException(getErrorMsg(jr, token), token);
                    } else if ((firstToken == START_BRACE) == (token != END_BRACE)) {
                        throw new ParsingException("The JSON text should be wrapped or unwrapped with \"[]\" or \"{}\"", token);
                    } else {
                        if (jr.hasText()) {
                            if (key != null && ignoredClassPropNames != null && ignoredClassPropNames.contains(key.toString())) {
                                // ignore.
                            } else {
                                value = readValue(jr, readNullToEmpty, propType);

                                if (!ignoreNullOrEmpty || (!isNullOrEmptyValue(keyType, key) && !isNullOrEmptyValue(propType, value))) {
                                    result.put(key, value);
                                }
                            }
                        }
                    }

                    return (T) creatorAndConverter._2.apply(result);

                default:
                    throw new ParsingException(getErrorMsg(jr, token), token);
            }
        }
    }

    @SuppressWarnings({ "unchecked" })
    protected <T> T readArray(final JsonReader jr, final JsonDeserConfig config, final Type<?> propType, final boolean isFirstCall,
            final Class<? extends T> targetClass, final Type<? extends T> targetType, Object[] output) throws IOException {
        Type<?> eleType = defaultValueType;

        if (propType != null && (propType.isArray() || propType.isCollection()) && !propType.elementType().isObject()) {
            eleType = propType.elementType();
        } else if (targetType != null && (targetType.isArray() || targetType.isCollection()) && !targetType.elementType().isObject()) {
            eleType = targetType.elementType();
        } else if (propType == null || !propType.isObject()) {
            if (config.getElementType() != null && !config.getElementType().isObject()) {
                eleType = config.getElementType();
            } else {
                eleType = Type
                        .of(targetClass.isArray() && !Object.class.equals(targetClass.getComponentType()) ? targetClass.getComponentType() : Object.class);
            }
        }

        final boolean ignoreNullOrEmpty = config.isIgnoreNullOrEmpty();
        final boolean readNullToEmpty = config.isReadNullToEmpty();

        final int firstToken = isFirstCall ? jr.nextToken() : START_BRACKET;

        if (firstToken == EOF) {
            //    if (isFirstCall && N.notEmpty(jr.getText())) {
            //        throw new ParseException("Can't parse: " + jr.getText());
            //    }
            //
            //    return null; // (T) (a == null ? N.newArray(targetClass.getComponentType(), 0) : a);

            final Object value = readValue(jr, readNullToEmpty, eleType);

            if (!ignoreNullOrEmpty || !isNullOrEmptyValue(eleType, value)) {
                if (output == null || output.length == 0) {
                    output = N.newArray(targetClass.getComponentType(), 1);
                }

                output[0] = value;
            } else if (output == null) {
                output = N.newArray(targetClass.getComponentType(), 0);
            }

            return (T) output;
        }

        if (output == null) {
            final List<Object> c = Objectory.createList();
            Object value = null;

            try {
                for (int preToken = firstToken,
                        token = firstToken == START_BRACKET ? jr.nextToken() : firstToken;; preToken = token, token = jr.nextToken(eleType)) {
                    switch (token) {
                        case START_DOUBLE_QUOTE, START_SINGLE_QUOTE:

                            break;

                        case END_DOUBLE_QUOTE, END_SINGLE_QUOTE:

                            value = readValue(jr, readNullToEmpty, eleType);

                            if (!ignoreNullOrEmpty || !isNullOrEmptyValue(eleType, value)) {
                                c.add(value);
                            }

                            break;

                        case COMMA:

                            if (jr.hasText() || preToken == COMMA || (preToken == START_BRACKET && c.size() == 0)) {
                                value = readValue(jr, readNullToEmpty, eleType);

                                if (!ignoreNullOrEmpty || !isNullOrEmptyValue(eleType, value)) {
                                    c.add(value);
                                }
                            }

                            break;

                        case START_BRACE:

                            value = readBracedValue(jr, config, eleType);

                            if (!ignoreNullOrEmpty || !isNullOrEmptyValue(eleType, value)) {
                                c.add(value);
                            }

                            break;

                        case START_BRACKET:

                            value = readBracketedValue(jr, config, null, eleType);

                            if (!ignoreNullOrEmpty || !isNullOrEmptyValue(eleType, value)) {
                                c.add(value);
                            }

                            break;

                        case END_BRACKET, EOF:

                            if ((firstToken == START_BRACKET) == (token != END_BRACKET)) {
                                throw new ParsingException("The JSON text should be wrapped or unwrapped with \"[]\" or \"{}\"", token);
                            } else if (jr.hasText() || preToken == COMMA) {
                                value = readValue(jr, readNullToEmpty, eleType);

                                if (!ignoreNullOrEmpty || !isNullOrEmptyValue(eleType, value)) {
                                    c.add(value);
                                }
                            }

                            return collectionToArray(c, targetType);

                        default:
                            throw new ParsingException(getErrorMsg(jr, token), token);
                    }
                }
            } finally {
                Objectory.recycle(c);
            }
        } else {
            int idx = 0;
            Object value = null;

            for (int preToken = firstToken,
                    token = firstToken == START_BRACKET ? jr.nextToken() : firstToken;; preToken = token, token = jr.nextToken(eleType)) {
                switch (token) {
                    case START_DOUBLE_QUOTE, START_SINGLE_QUOTE:

                        break;

                    case END_DOUBLE_QUOTE, END_SINGLE_QUOTE:

                        value = readValue(jr, readNullToEmpty, eleType);

                        if (!ignoreNullOrEmpty || !isNullOrEmptyValue(eleType, value)) {
                            output[idx++] = value;
                        }

                        break;

                    case COMMA:

                        if (jr.hasText() || preToken == COMMA || (preToken == START_BRACKET && idx == 0)) {
                            value = readValue(jr, readNullToEmpty, eleType);

                            if (!ignoreNullOrEmpty || !isNullOrEmptyValue(eleType, value)) {
                                output[idx++] = value;
                            }
                        }

                        break;

                    case START_BRACE:

                        value = readBracedValue(jr, config, eleType);

                        if (!ignoreNullOrEmpty || !isNullOrEmptyValue(eleType, value)) {
                            output[idx++] = value;
                        }

                        break;

                    case START_BRACKET:

                        value = readBracketedValue(jr, config, null, eleType);

                        if (!ignoreNullOrEmpty || !isNullOrEmptyValue(eleType, value)) {
                            output[idx++] = value;
                        }

                        break;

                    case END_BRACKET, EOF:

                        if ((firstToken == START_BRACKET) == (token != END_BRACKET)) {
                            throw new ParsingException("The JSON text should be wrapped or unwrapped with \"[]\" or \"{}\"", token);
                        } else if (jr.hasText() || preToken == COMMA) {
                            value = readValue(jr, readNullToEmpty, eleType);

                            if (!ignoreNullOrEmpty || !isNullOrEmptyValue(eleType, value)) {
                                output[idx++] = value;
                            }
                        }

                        return (T) output;

                    default:
                        throw new ParsingException(getErrorMsg(jr, token), token);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    protected <T> T readCollection(final JsonReader jr, final JsonDeserConfig config, final Type<?> propType,
            final BiConsumer<? super Collection<Object>, ?> propHandler, final boolean isFirstCall, final Class<? extends T> targetClass,
            final Type<? extends T> targetType, final Collection<Object> output) throws IOException {
        Type<?> eleType = defaultValueType;

        if (propType != null && (propType.isCollection() || propType.isArray()) && !propType.elementType().isObject()) {
            eleType = propType.elementType();
        } else if (targetType != null && (targetType.isCollection() || targetType.isArray()) && !targetType.elementType().isObject()) {
            eleType = targetType.elementType();
        } else if ((propType == null || !propType.isObject()) && (config.getElementType() != null && !config.getElementType().isObject())) {
            eleType = config.getElementType();
        }

        final boolean ignoreNullOrEmpty = config.isIgnoreNullOrEmpty();
        final boolean readNullToEmpty = config.isReadNullToEmpty();
        @SuppressWarnings("rawtypes")
        final BiConsumer<Collection<Object>, Object> propHandlerToUse = propHandler == null ? DEFAULT_PROP_HANDLER : (BiConsumer) propHandler;

        final Tuple2<Function<Class<?>, Object>, Function<Object, Object>> creatorAndConverter = getCreatorAndConverterForTargetType(targetClass, null);

        final Collection<Object> result = output == null
                ? (Collection.class.isAssignableFrom(targetClass) ? (Collection<Object>) creatorAndConverter._1.apply(targetClass) : new ArrayList<>())
                : output;

        Object value = null;

        final int firstToken = isFirstCall ? jr.nextToken() : START_BRACKET;

        if (firstToken == EOF) {
            //    if (isFirstCall && N.notEmpty(jr.getText())) {
            //        throw new ParseException("Can't parse: " + jr.getText());
            //    }
            //
            //    return null; // (T) result;

            value = readValue(jr, readNullToEmpty, eleType);

            if (!ignoreNullOrEmpty || !isNullOrEmptyValue(eleType, value)) {
                // result.add(propValue);
                propHandlerToUse.accept(result, value);
            }

            return (T) creatorAndConverter._2.apply(result);
        }

        for (int preToken = firstToken, token = firstToken == START_BRACKET ? jr.nextToken() : firstToken;; preToken = token, token = jr.nextToken(eleType)) {
            switch (token) {
                case START_DOUBLE_QUOTE, START_SINGLE_QUOTE:

                    break;

                case END_DOUBLE_QUOTE, END_SINGLE_QUOTE:

                    value = readValue(jr, readNullToEmpty, eleType);

                    if (!ignoreNullOrEmpty || !isNullOrEmptyValue(eleType, value)) {
                        // result.add(propValue);
                        propHandlerToUse.accept(result, value);
                    }

                    break;

                case COMMA:

                    if (jr.hasText() || preToken == COMMA || (preToken == START_BRACKET && result.size() == 0)) {
                        value = readValue(jr, readNullToEmpty, eleType);

                        if (!ignoreNullOrEmpty || !isNullOrEmptyValue(eleType, value)) {
                            // result.add(propValue);
                            propHandlerToUse.accept(result, value);
                        }
                    }

                    break;

                case START_BRACE:

                    value = readBracedValue(jr, config, eleType);

                    if (!ignoreNullOrEmpty || !isNullOrEmptyValue(eleType, value)) {
                        // result.add(propValue);
                        propHandlerToUse.accept(result, value);
                    }

                    break;

                case START_BRACKET:

                    value = readBracketedValue(jr, config, null, eleType);

                    if (!ignoreNullOrEmpty || !isNullOrEmptyValue(eleType, value)) {
                        // result.add(propValue);
                        propHandlerToUse.accept(result, value);
                    }

                    break;

                case END_BRACKET, EOF:

                    if ((firstToken == START_BRACKET) == (token != END_BRACKET)) {
                        throw new ParsingException("The JSON text should be wrapped or unwrapped with \"[]\" or \"{}\"", token);
                    } else if (jr.hasText() || preToken == COMMA) {
                        value = readValue(jr, readNullToEmpty, eleType);

                        if (!ignoreNullOrEmpty || !isNullOrEmptyValue(eleType, value)) {
                            // result.add(propValue);
                            propHandlerToUse.accept(result, value);
                        }
                    }

                    return (T) creatorAndConverter._2.apply(result);

                default:
                    throw new ParsingException(getErrorMsg(jr, token), token);
            }
        }
    }

    @SuppressWarnings("unused")
    protected <T> T readMapEntity(final JsonReader jr, final JsonDeserConfig config, final boolean isFirstCall, final Class<? extends T> targetClass,
            final Type<? extends T> targetType) throws IOException {
        final int firstToken = isFirstCall ? jr.nextToken() : START_BRACE;

        if (firstToken == EOF) {
            if (Strings.isNotEmpty(jr.getText())) {
                throw new ParsingException("Can't parse: " + jr.getText(), firstToken);
            }

            return null;
        }

        MapEntity mapEntity = null;

        for (int token = firstToken == START_BRACE ? jr.nextToken() : firstToken;; token = jr.nextToken()) {
            switch (token) {
                case START_DOUBLE_QUOTE, START_SINGLE_QUOTE:

                    break;

                case END_DOUBLE_QUOTE, END_SINGLE_QUOTE, COLON:

                    if (jr.hasText()) {
                        if (mapEntity == null) {
                            mapEntity = new MapEntity(jr.getText());
                        } else {
                            throw new ParsingException(getErrorMsg(jr, token), token);
                        }
                    } else {
                        if (mapEntity == null) {
                            throw new ParsingException("Bean name cannot be null or empty", token);
                        }
                    }

                    break;

                case START_BRACE:
                    final Map<String, Object> props = readMap(jr, config, null, false, Map.class, null, null);

                    //noinspection DataFlowIssue
                    mapEntity.set(props);

                    break;

                case END_BRACE, EOF:

                    if ((firstToken == START_BRACE) == (token != END_BRACE)) {
                        throw new ParsingException("The JSON text should be wrapped or unwrapped with \"[]\" or \"{}\"", token);
                    }

                    return (T) mapEntity;

                default:
                    throw new ParsingException(getErrorMsg(jr, token), token);
            }
        }
    }

    @SuppressWarnings({ "unused" })
    protected <T> T readEntityId(final JsonReader jr, final JsonDeserConfig config, final boolean isFirstCall, final Class<? extends T> targetClass,
            final Type<? extends T> targetType) throws IOException {
        final int firstToken = isFirstCall ? jr.nextToken() : START_BRACE;

        if (firstToken == EOF) {
            if (Strings.isNotEmpty(jr.getText())) {
                throw new ParsingException("Can't parse: " + jr.getText(), firstToken);
            }

            return null;
        }

        Seid entityId = null;

        for (int token = firstToken == START_BRACE ? jr.nextToken() : firstToken;; token = jr.nextToken()) {
            switch (token) {
                case START_DOUBLE_QUOTE, START_SINGLE_QUOTE:

                    break;

                case END_DOUBLE_QUOTE, END_SINGLE_QUOTE, COLON:

                    if (jr.hasText()) {
                        if (entityId == null) {
                            entityId = Seid.of(jr.getText());
                        } else {
                            throw new ParsingException(getErrorMsg(jr, token), token);
                        }
                    } else {
                        if (entityId == null) {
                            throw new ParsingException("Bean name cannot be null or empty", token);
                        }
                    }

                    break;

                case START_BRACE:
                    final Map<String, Object> props = readMap(jr, config, null, false, Map.class, null, null);

                    //noinspection DataFlowIssue
                    entityId.set(props);

                    break;

                case END_BRACE, EOF:

                    if ((firstToken == START_BRACE) == (token != END_BRACE)) {
                        throw new ParsingException("The JSON text should be wrapped or unwrapped with \"[]\" or \"{}\"", token);
                    }

                    return (T) entityId;

                default:
                    throw new ParsingException(getErrorMsg(jr, token), token);
            }
        }
    }

    @SuppressWarnings("unused")
    protected <T> T readDataset(final JsonReader jr, final int lastToken, final JsonDeserConfig config, final boolean isFirstCall,
            final Class<? extends T> targetClass, final Type<? extends T> targetType) throws IOException {

        final int firstToken = isFirstCall ? jr.nextToken() : lastToken;

        if (firstToken == EOF) {
            if (isFirstCall && Strings.isNotEmpty(jr.getText())) {
                throw new ParsingException("Can't parse: " + jr.getText(), firstToken);
            }

            return null;
        }

        Dataset rs = null;

        if (firstToken == START_BRACKET) {
            final Map<String, List<Object>> result = new LinkedHashMap<>();

            int token = jr.nextToken();

            while (token == COMMA) {
                token = jr.nextToken();
            }

            if (token == START_BRACE) {
                final boolean hasValueTypes = config.hasValueTypes();
                final Collection<String> ignoredClassPropNames = config.getIgnoredPropNames(Map.class);
                final boolean readNullToEmpty = config.isReadNullToEmpty();

                final Type<?> keyType = strType;
                Type<?> valueType = objType;
                boolean isKey = true;
                String key = null;
                Object value = null;
                int valueCount = 0;
                boolean isBraceEnded = false;

                token = jr.nextToken();

                for (;; token = jr.nextToken()) {
                    switch (token) {
                        case START_DOUBLE_QUOTE, START_SINGLE_QUOTE:

                            break;

                        case END_DOUBLE_QUOTE, END_SINGLE_QUOTE:
                            if (isKey) {
                                key = (String) readValue(jr, readNullToEmpty, keyType);
                                valueType = hasValueTypes ? config.getValueType(key, objType) : objType;
                            } else {
                                if (key != null && ignoredClassPropNames != null && ignoredClassPropNames.contains(key)) {
                                    // ignore.
                                } else {
                                    value = readValue(jr, readNullToEmpty, valueType);

                                    addDatasetColumnValue(key, value, valueCount, result);
                                }
                            }

                            break;

                        case COLON:
                            if (isKey) {
                                isKey = false;

                                if (jr.hasText()) {
                                    key = (String) readValue(jr, readNullToEmpty, keyType);
                                    valueType = hasValueTypes ? config.getValueType(key, objType) : objType;
                                }
                            } else {
                                throw new ParsingException(getErrorMsg(jr, token), token);
                            }

                            break;

                        case COMMA:
                            if (isKey) {
                                throw new ParsingException(getErrorMsg(jr, token), token);
                            } else {
                                isKey = true;

                                if (jr.hasText()) {
                                    if (key != null && ignoredClassPropNames != null && ignoredClassPropNames.contains(key)) {
                                        // ignore.
                                    } else {
                                        value = readValue(jr, readNullToEmpty, valueType);

                                        addDatasetColumnValue(key, value, valueCount, result);
                                    }
                                }
                            }

                            break;

                        case START_BRACE:
                            if (isKey) {
                                throw new ParsingException(getErrorMsg(jr, token), token);
                            } else {
                                if (key != null && ignoredClassPropNames != null && ignoredClassPropNames.contains(key)) {
                                    readMap(jr, defaultJsonDeserConfig, null, false, Map.class, null, null);
                                } else {
                                    value = readBracedValue(jr, config, valueType);

                                    addDatasetColumnValue(key, value, valueCount, result);
                                }
                            }

                            break;

                        case START_BRACKET:
                            if (isKey) {
                                throw new ParsingException(getErrorMsg(jr, token), token);
                            } else {
                                if (key != null && ignoredClassPropNames != null && ignoredClassPropNames.contains(key)) {
                                    readCollection(jr, defaultJsonDeserConfig, null, Strings.isEmpty(key) ? null : config.getPropHandler(key), false,
                                            List.class, null, null);
                                } else {
                                    value = readBracketedValue(jr, config, Strings.isEmpty(key) ? null : config.getPropHandler(key), valueType);

                                    addDatasetColumnValue(key, value, valueCount, result);
                                }
                            }

                            break;

                        case END_BRACE:
                            if (isKey && key != null /* check for empty JSON text {} */) {
                                throw new ParsingException(getErrorMsg(jr, token), token);
                            } else {
                                if (jr.hasText()) {
                                    if (key != null && ignoredClassPropNames != null && ignoredClassPropNames.contains(key)) {
                                        // ignore.
                                    } else {
                                        value = readValue(jr, readNullToEmpty, valueType);

                                        addDatasetColumnValue(key, value, valueCount, result);
                                    }
                                }
                            }

                            final int maxValueSize = Stream.of(result.values()).mapToInt(List::size).max().orElse(0);

                            for (final List<Object> vc : result.values()) {
                                if (vc.size() < maxValueSize) {
                                    vc.add(null);
                                }
                            }

                            valueCount++;

                            do {
                                token = jr.nextToken();
                            } while (token == COMMA);

                            isKey = true;
                            isBraceEnded = true;

                            break;
                        default:
                            throw new ParsingException(getErrorMsg(jr, token), token);
                    }

                    if (isBraceEnded) {
                        if (token == END_BRACKET || token == EOF /*should not happen*/) {
                            break;
                        } else if (token != START_BRACE) {
                            throw new ParsingException(getErrorMsg(jr, token), token);
                        }

                        isBraceEnded = false;
                    }
                }
            } else if (token == END_BRACKET || token == EOF /*should not happen*/) {
                // end
            } else {
                throw new ParsingException(getErrorMsg(jr, token), token);
            }

            rs = new RowDataset(new ArrayList<>(result.keySet()), new ArrayList<>(result.values()));

            return (T) rs;
        } else {
            //        String beanName = null;
            //        Class<?> beanClass = null;
            List<String> columnNameList = null;
            List<List<Object>> columnList = null;
            Map<String, Object> properties = null;
            boolean isFrozen = false;

            List<Type<?>> columnTypeList = null;

            String columnName = null;
            Type<?> valueType = defaultValueType;
            boolean isKey = true;

            for (int token = firstToken == START_BRACE ? jr.nextToken() : firstToken;; token = jr.nextToken()) {
                switch (token) {
                    case START_DOUBLE_QUOTE, START_SINGLE_QUOTE:

                        break;

                    case END_DOUBLE_QUOTE, END_SINGLE_QUOTE:

                        if (isKey) {
                            columnName = jr.getText();
                        } else {
                            final Integer order = datasetSheetPropOrder.get(columnName);

                            if (order == null) {
                                throw new ParsingException(getErrorMsg(jr, token), token);
                            }

                            if (order == 6) {
                                isFrozen = jr.readValue(boolType);
                            } else {
                                throw new ParsingException(getErrorMsg(jr, token), token);
                            }
                        }

                        break;

                    case COLON:
                        if (isKey) {
                            isKey = false;

                            if (jr.hasText()) {
                                columnName = jr.getText();
                            }
                        } else {
                            throw new ParsingException(getErrorMsg(jr, token), token);
                        }

                        break;

                    case COMMA:
                        if (isKey) {
                            throw new ParsingException(getErrorMsg(jr, token), token);
                        } else {
                            isKey = true;

                            if (jr.hasText()) {
                                final Integer order = datasetSheetPropOrder.get(columnName);

                                if (order == null) {
                                    throw new ParsingException(getErrorMsg(jr, token), token);
                                }

                                if (order == 6) {
                                    isFrozen = jr.readValue(boolType);
                                } else {
                                    throw new ParsingException(getErrorMsg(jr, token), token);
                                }
                            }
                        }

                        break;

                    case START_BRACKET:
                        final Integer order = datasetSheetPropOrder.get(columnName);

                        if (order == null) {
                            throw new ParsingException(getErrorMsg(jr, token), token);
                        }

                        switch (order) {
                            case 3:
                                columnNameList = readCollection(jr, jdcForStringElement, null, null, false, List.class, null, null);
                                break;

                            case 4:
                                columnTypeList = readCollection(jr, jdcForTypeElement, null, null, false, List.class, null, null);
                                break;

                            default:
                                throw new ParsingException(getErrorMsg(jr, token), token);
                        }

                        break;

                    case START_BRACE:
                        if (PROPERTIES.equals(columnName)) {
                            properties = readMap(jr, jdcForPropertiesElement, null, false, Map.class, null, null);
                        } else if (COLUMNS.equals(columnName)) {
                            columnName = null;
                            isKey = true;
                            boolean readingColumns = true;

                            do {
                                token = jr.nextToken();

                                switch (token) {
                                    case START_DOUBLE_QUOTE, START_SINGLE_QUOTE:

                                        break;

                                    case END_DOUBLE_QUOTE, END_SINGLE_QUOTE:
                                        if (isKey) {
                                            columnName = jr.getText();
                                        } else {
                                            throw new ParsingException(getErrorMsg(jr, token), token);
                                        }

                                        break;

                                    case COLON:
                                        if (isKey) {
                                            isKey = false;

                                            if (jr.hasText()) {
                                                columnName = jr.getText();
                                            }
                                        } else {
                                            throw new ParsingException(getErrorMsg(jr, token), token);
                                        }

                                        break;

                                    case COMMA:
                                        if (isKey) {
                                            throw new ParsingException(getErrorMsg(jr, token), token);
                                        } else {
                                            isKey = true;

                                            if (jr.hasText()) {
                                                throw new ParsingException(getErrorMsg(jr, token), token);
                                            }
                                        }

                                        break;

                                    case START_BRACKET:
                                        final int index = N.indexOf(columnNameList, columnName);

                                        if (index == N.INDEX_NOT_FOUND) {
                                            throw new ParsingException("Column: " + columnName + " is not found column list: " + columnNameList);
                                        }

                                        valueType = N.isEmpty(columnTypeList) ? null : columnTypeList.get(index);

                                        if (valueType == null) {
                                            valueType = defaultValueType;
                                        }

                                        final List<Object> column = readCollection(jr, JsonDeserConfig.create().setElementType(valueType), null,
                                                Strings.isEmpty(columnName) ? null : config.getPropHandler(columnName), false, List.class, null, null);

                                        if (columnList == null) {
                                            //noinspection DataFlowIssue
                                            columnList = new ArrayList<>(columnNameList.size());
                                            N.fill(columnList, 0, columnNameList.size(), null);
                                        }

                                        columnList.set(index, column);

                                        break;

                                    case END_BRACE:
                                        if (jr.hasText()) {
                                            // it should not happen.
                                            throw new ParsingException(getErrorMsg(jr, token), token);
                                        }

                                        columnName = null;
                                        isKey = true;
                                        readingColumns = false;
                                        break;

                                    default:
                                        throw new ParsingException(getErrorMsg(jr, token), token);
                                }
                            } while (readingColumns);
                        } else {
                            throw new ParsingException(getErrorMsg(jr, token) + ". Key: " + columnName + ",  Value: " + jr.getText(), token);
                        }

                        break;

                    case END_BRACE, EOF:

                        if ((firstToken == START_BRACE) == (token != END_BRACE)) {
                            throw new ParsingException("The JSON text should be wrapped or unwrapped with \"[]\" or \"{}\"", token);
                        } else if ((isKey && columnName != null) || jr.hasText()) {
                            throw new ParsingException(getErrorMsg(jr, token), token);
                        }

                        if (columnNameList == null) {
                            columnNameList = new ArrayList<>();
                        }

                        if (columnList == null) {
                            columnList = new ArrayList<>();
                        }

                        // rs = new RowDataset(beanName, beanClass, columnNameList, columnList, properties);
                        rs = new RowDataset(columnNameList, columnList, properties);

                        if (isFrozen) {
                            rs.freeze();
                        }

                        return (T) rs;

                    default:
                        throw new ParsingException(getErrorMsg(jr, token), token);
                }
            }
        }
    }

    @SuppressWarnings("unused")
    private void addDatasetColumnValue(final String key, final Object value, final int valueCount, final Map<String, List<Object>> output) {
        // Value should not be ignored for Dataset column.
        // if (!ignoreNullOrEmpty || (!isNullOrEmptyValue(keyType, key) && !isNullOrEmptyValue(valueType, value))) {
        List<Object> values = output.get(key);

        if (values == null) {
            values = new ArrayList<>();

            if (valueCount > 0) {
                N.fill(values, 0, valueCount, null);
            }

            output.put(key, values);
        }

        values.add(value);
        //}
    }

    @SuppressWarnings({ "unused", "rawtypes" })
    protected <T> T readSheet(final JsonReader jr, final int lastToken, final JsonDeserConfig config, final boolean isFirstCall,
            final Class<? extends T> targetClass, final Type<? extends T> targetType) throws IOException {

        final int firstToken = isFirstCall ? jr.nextToken() : lastToken;

        if (firstToken == EOF) {
            if (isFirstCall && Strings.isNotEmpty(jr.getText())) {
                throw new ParsingException("Can't parse: " + jr.getText(), firstToken);
            }

            return null;
        }

        Sheet sheet = null;

        List<Object> rowKeyList = null;
        List<Object> columnKeyList = null;
        List<List<Object>> columnList = null;

        String rowKeyType = null;
        String columnKeyType = null;
        List<Type<?>> columnTypeList = null;
        Type<?> columnKeyValueType = strType;

        String columnName = null;
        Type<?> valueType = defaultValueType;
        boolean isKey = true;

        for (int token = firstToken == START_BRACE ? jr.nextToken() : firstToken;; token = jr.nextToken()) {
            switch (token) {
                case START_DOUBLE_QUOTE, START_SINGLE_QUOTE:

                    break;

                case END_DOUBLE_QUOTE, END_SINGLE_QUOTE:
                    if (isKey) {
                        columnName = jr.getText();
                    } else {
                        final Integer order = datasetSheetPropOrder.get(columnName);

                        if (order == null) {
                            throw new ParsingException(getErrorMsg(jr, token), token);
                        }

                        switch (order) { //NOSONAR
                            case 10:
                                rowKeyType = jr.readValue(strType);
                                break;

                            case 11:
                                columnKeyType = jr.readValue(strType);
                                columnKeyValueType = Strings.isEmpty(columnKeyType) ? strType : Type.of(columnKeyType);
                                break;

                            default:
                                throw new ParsingException(getErrorMsg(jr, token), token);
                        }
                    }

                    break;

                case COLON:
                    if (isKey) {
                        isKey = false;

                        if (jr.hasText()) {
                            columnName = jr.getText();
                        }
                    } else {
                        throw new ParsingException(getErrorMsg(jr, token), token);
                    }

                    break;

                case COMMA:
                    if (isKey) {
                        throw new ParsingException(getErrorMsg(jr, token), token);
                    } else {
                        isKey = true;

                        if (jr.hasText()) {
                            final Integer order = datasetSheetPropOrder.get(columnName);

                            if (order == null) {
                                throw new ParsingException(getErrorMsg(jr, token), token);
                            }

                            switch (order) { //NOSONAR
                                case 10:
                                    rowKeyType = jr.readValue(strType);
                                    break;

                                case 11:
                                    columnKeyType = jr.readValue(strType);
                                    columnKeyValueType = Strings.isEmpty(columnKeyType) ? strType : Type.of(columnKeyType);
                                    break;

                                default:
                                    throw new ParsingException(getErrorMsg(jr, token), token);
                            }
                        }
                    }

                    break;

                case START_BRACKET:
                    final Integer order = datasetSheetPropOrder.get(columnName);

                    if (order == null) {
                        throw new ParsingException(getErrorMsg(jr, token), token);
                    }

                    switch (order) {
                        case 4:
                            columnTypeList = readCollection(jr, jdcForTypeElement, null, null, false, List.class, null, null);
                            break;

                        case 8:
                            rowKeyList = readCollection(jr,
                                    JsonDeserConfig.create().setElementType(Strings.isEmpty(rowKeyType) ? strType : Type.of(rowKeyType)), null, null, false,
                                    List.class, null, null);
                            break;

                        case 9:
                            columnKeyList = readCollection(jr, JsonDeserConfig.create().setElementType(columnKeyValueType), null, null, false, List.class, null,
                                    null);
                            break;

                        default:
                            throw new ParsingException(getErrorMsg(jr, token), token);
                    }

                    break;

                case START_BRACE:
                    if (COLUMNS.equals(columnName)) {
                        columnName = null;
                        isKey = true;
                        boolean readingColumns = true;

                        do {
                            token = jr.nextToken();

                            switch (token) {
                                case START_DOUBLE_QUOTE, START_SINGLE_QUOTE:

                                    break;

                                case END_DOUBLE_QUOTE, END_SINGLE_QUOTE:
                                    if (isKey) {
                                        columnName = jr.getText();
                                    } else {
                                        throw new ParsingException(getErrorMsg(jr, token), token);
                                    }

                                    break;

                                case COLON:
                                    if (isKey) {
                                        isKey = false;

                                        if (jr.hasText()) {
                                            columnName = jr.getText();
                                        }
                                    } else {
                                        throw new ParsingException(getErrorMsg(jr, token), token);
                                    }

                                    break;

                                case COMMA:
                                    if (isKey) {
                                        throw new ParsingException(getErrorMsg(jr, token), token);
                                    } else {
                                        isKey = true;

                                        if (jr.hasText()) {
                                            throw new ParsingException(getErrorMsg(jr, token), token);
                                        }
                                    }

                                    break;

                                case START_BRACKET:
                                    final Object parsedColumnKey;

                                    try {
                                        parsedColumnKey = columnKeyValueType.valueOf(columnName);
                                    } catch (final Exception e) {
                                        throw new ParsingException("Column: " + columnName + " can't be parsed as type: " + columnKeyValueType.name(), e);
                                    }

                                    final int index = N.indexOf(columnKeyList, parsedColumnKey);

                                    if (index == N.INDEX_NOT_FOUND) {
                                        throw new ParsingException("Column: " + columnName + " is not found column list: " + columnKeyList);
                                    }

                                    valueType = N.isEmpty(columnTypeList) ? null : columnTypeList.get(index);

                                    if (valueType == null) {
                                        valueType = defaultValueType;
                                    }

                                    final List<Object> column = readCollection(jr, JsonDeserConfig.create().setElementType(valueType), null,
                                            Strings.isEmpty(columnName) ? null : config.getPropHandler(columnName), false, List.class, null, null);

                                    if (columnList == null) {
                                        //noinspection DataFlowIssue
                                        columnList = new ArrayList<>(columnKeyList.size());
                                        N.fill(columnList, 0, columnKeyList.size(), null);
                                    }

                                    columnList.set(index, column);

                                    break;

                                case END_BRACE:
                                    if (jr.hasText()) {
                                        // it should not happen.
                                        throw new ParsingException(getErrorMsg(jr, token), token);
                                    }

                                    columnName = null;
                                    isKey = true;
                                    readingColumns = false;
                                    break;

                                default:
                                    throw new ParsingException(getErrorMsg(jr, token), token);
                            }
                        } while (readingColumns);
                    } else {
                        throw new ParsingException(getErrorMsg(jr, token) + ". Key: " + columnName + ",  Value: " + jr.getText(), token);
                    }

                    break;

                case END_BRACE, EOF:

                    if ((firstToken == START_BRACE) == (token != END_BRACE)) {
                        throw new ParsingException("The JSON text should be wrapped or unwrapped with \"[]\" or \"{}\"", token);
                    } else if ((isKey && columnName != null) || jr.hasText()) {
                        throw new ParsingException(getErrorMsg(jr, token), token);
                    }

                    if (rowKeyList == null) {
                        rowKeyList = new ArrayList<>();
                    }

                    if (columnList == null) {
                        columnList = new ArrayList<>();
                    }

                    sheet = Sheet.columns(rowKeyList, columnKeyList, columnList);

                    return (T) sheet;

                default:
                    throw new ParsingException(getErrorMsg(jr, token), token);
            }
        }
    }

    protected Object readBracketedValue(final JsonReader jr, JsonDeserConfig config, final BiConsumer<? super Collection<Object>, ?> propHandler,
            final Type<?> type) throws IOException {
        if (N.len(type.parameterTypes()) == 1) {
            config = config.copy();
            config.setElementType(type.parameterTypes()[0]);
        }

        if (type.isArray()) {
            return readArray(jr, config, type, false, type.javaType(), type, null);
        } else if (type.isCollection()) {
            return readCollection(jr, config, type, propHandler, false, type.javaType(), type, null);
        } else if (type.isDataset()) {
            return readDataset(jr, START_BRACKET, config, false, type.javaType(), type);
        } else {
            final List<?> list = readCollection(jr, config, type, propHandler, false, List.class, null, null);
            final BiFunction<List<?>, Type<?>, Object> converter = list2PairTripleConverterMap.get(type.javaType());

            return converter == null ? list : converter.apply(list, type);
        }
    }

    protected Object readBracedValue(final JsonReader jr, JsonDeserConfig config, final Type<?> type) throws IOException {
        if (N.len(type.parameterTypes()) == 2) {
            config = config.copy();
            config.setMapKeyType(type.parameterTypes()[0]);
            config.setMapValueType(type.parameterTypes()[1]);
        }

        if (type.isBean()) {
            return readBean(jr, config, false, type.javaType(), type);
        } else if (type.isMap()) {
            return readMap(jr, config, type, false, type.javaType(), type, null);
        } else if (type.isDataset()) {
            return readDataset(jr, START_BRACE, config, false, type.javaType(), type);
        } else if (Sheet.class.isAssignableFrom(type.javaType())) {
            return readSheet(jr, START_BRACE, config, false, type.javaType(), type);
        } else if (type.isMapEntity()) {
            return readMapEntity(jr, config, false, type.javaType(), type);
        } else if (type.isEntityId()) {
            return readEntityId(jr, config, false, type.javaType(), type);
        } else {
            final Map<Object, Object> map = readMap(jr, config, type, false, Map.class, null, null);
            final Function<Map<Object, Object>, ?> converter = map2TargetTypeConverterMap.get(type.javaType());

            if (converter == null) {
                if (AbstractMap.SimpleImmutableEntry.class.isAssignableFrom(type.javaType())) {
                    return map2TargetTypeConverterMap.get(AbstractMap.SimpleImmutableEntry.class).apply(map);
                } else if (Map.Entry.class.isAssignableFrom(type.javaType())) {
                    return map2TargetTypeConverterMap.get(Map.Entry.class).apply(map);
                } else {
                    return map;
                }
            } else {
                return converter.apply(map);
            }
        }
    }

    protected Object readValue(final JsonReader jr, final boolean nullToEmpty, final Type<?> valueType) {
        return readNullToEmpty(valueType, jr.readValue(valueType), nullToEmpty);
    }

    protected Object readValue(final JsonReader jr, final boolean nullToEmpty, final PropInfo propInfo, final Type<?> valueType) {
        return readNullToEmpty(valueType, propInfo != null && propInfo.hasFormat ? propInfo.readPropValue(jr.readValue(strType)) : jr.readValue(valueType),
                nullToEmpty);
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation creates a lazy stream that deserializes JSON array elements on-demand,
     * providing memory-efficient processing of large JSON arrays without loading all elements into memory.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String json = "[{\"name\":\"John\",\"age\":30},{\"name\":\"Jane\",\"age\":25}]";
     * JsonDeserConfig config = new JsonDeserConfig();
     *
     * try (Stream<User> users = parser.stream(json, config, Type.of(User.class))) {
     *     users.filter(u -> u.getAge() > 25)
     *          .forEach(System.out::println);
     * }
     * // Stream is automatically closed with try-with-resources
     * }</pre>
     *
     * @param <T> the type of elements in the stream
     * @param source the JSON array string to stream; may be {@code null} or empty (returns empty stream)
     * @param config the deserialization configuration to use; may be {@code null} to use default configuration
     * @param elementType the type of elements to deserialize; must not be {@code null}
     * @return a Stream of deserialized elements; never {@code null}
     * @throws IllegalArgumentException if the element type is not supported for streaming
     * @throws UncheckedIOException if an I/O error occurs during parsing
     * @throws ParsingException if the JSON structure is invalid or not an array
     */
    @Override
    public <T> Stream<T> stream(final String source, final JsonDeserConfig config, final Type<? extends T> elementType) {
        checkStreamSupportedType(elementType);

        final JsonDeserConfig configToUse = check(config);

        if (Strings.isEmpty(source) || "[]".equals(source)) {
            return Stream.empty();
        }

        final char[] cbuf = Objectory.createCharArrayBuffer();
        Stream<T> result = null;

        try {
            final JsonReader jr = JsonStringReader.parse(source, cbuf);

            result = this.<T> stream(source, jr, configToUse, elementType).onClose(() -> Objectory.recycle(cbuf));
        } finally {
            if (result == null) {
                Objectory.recycle(cbuf);
            }
        }

        return result;
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation creates a lazy stream from a JSON array file, automatically managing
     * the file reader resource. The file is opened when the stream is created and closed when
     * the stream is closed.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File jsonFile = new File("users.json");
     * JsonDeserConfig config = new JsonDeserConfig();
     *
     * try (Stream<User> users = parser.stream(jsonFile, config, Type.of(User.class))) {
     *     long count = users.filter(u -> u.getAge() > 18)
     *                       .count();
     *     System.out.println("Adult users: " + count);
     * }
     * // File is automatically closed when stream is closed
     * }</pre>
     *
     * @param <T> the type of elements in the stream
     * @param source the file containing the JSON array to stream; must not be {@code null}
     * @param config the deserialization configuration to use; may be {@code null} to use default configuration
     * @param elementType the type of elements to deserialize; must not be {@code null}
     * @return a Stream of deserialized elements; never {@code null}
     * @throws IllegalArgumentException if the element type is not supported for streaming
     * @throws UncheckedIOException if an I/O error occurs during file reading or parsing
     * @throws ParsingException if the JSON structure is invalid or not an array
     */
    @Override
    public <T> Stream<T> stream(final File source, final JsonDeserConfig config, final Type<? extends T> elementType) {
        Stream<T> result = null;
        Reader reader = null;

        try {
            reader = IOUtil.newFileReader(source);

            result = stream(reader, true, config, elementType);
        } finally {
            if (result == null) {
                IOUtil.closeQuietly(reader);
            }
        }

        return result;
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation creates a lazy stream from a JSON array input stream with configurable
     * resource management. The caller can control whether the input stream should be automatically
     * closed when the stream is closed.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * InputStream is = new FileInputStream("users.json");
     * JsonDeserConfig config = new JsonDeserConfig();
     *
     * // Stream will close the input stream when closed
     * try (Stream<User> users = parser.stream(is, config, true, Type.of(User.class))) {
     *     users.limit(10)
     *          .forEach(System.out::println);
     * }
     * // Input stream is automatically closed
     * }</pre>
     *
     * @param <T> the type of elements in the stream
     * @param source the input stream containing the JSON array to stream; must not be {@code null}
     * @param closeInputStreamWhenStreamIsClosed {@code true} to close the input stream when the stream is closed
     * @param config the deserialization configuration to use; may be {@code null} to use default configuration
     * @param elementType the type of elements to deserialize; must not be {@code null}
     * @return a Stream of deserialized elements; never {@code null}
     * @throws IllegalArgumentException if the element type is not supported for streaming
     * @throws UncheckedIOException if an I/O error occurs during stream reading or parsing
     * @throws ParsingException if the JSON structure is invalid or not an array
     */
    @Override
    public <T> Stream<T> stream(final InputStream source, final boolean closeInputStreamWhenStreamIsClosed, final JsonDeserConfig config,
            final Type<? extends T> elementType) {
        final Reader reader = IOUtil.newInputStreamReader(source); // NOSONAR

        return stream(reader, closeInputStreamWhenStreamIsClosed, config, elementType);
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation creates a lazy stream from a JSON array reader with configurable
     * resource management. The caller can control whether the reader should be automatically
     * closed when the stream is closed.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Reader reader = new FileReader("users.json");
     * JsonDeserConfig config = new JsonDeserConfig();
     *
     * // Stream will close the reader when closed
     * try (Stream<User> users = parser.stream(reader, config, true, Type.of(User.class))) {
     *     List<User> topUsers = users.sorted(Comparator.comparing(User::getAge).reversed())
     *                                .limit(5)
     *                                .collect(Collectors.toList());
     * }
     * // Reader is automatically closed
     * }</pre>
     *
     * @param <T> the type of elements in the stream
     * @param source the reader containing the JSON array to stream; must not be {@code null}
     * @param closeReaderWhenStreamIsClosed {@code true} to close the reader when the stream is closed
     * @param config the deserialization configuration to use; may be {@code null} to use default configuration
     * @param elementType the type of elements to deserialize; must not be {@code null}
     * @return a Stream of deserialized elements; never {@code null}
     * @throws IllegalArgumentException if the source is {@code null} or the element type is not supported for streaming
     * @throws UncheckedIOException if an I/O error occurs during reading or parsing
     * @throws ParsingException if the JSON structure is invalid or not an array
     */
    @Override
    public <T> Stream<T> stream(final Reader source, final boolean closeReaderWhenStreamIsClosed, final JsonDeserConfig config,
            final Type<? extends T> elementType) throws IllegalArgumentException {
        N.checkArgNotNull(source, cs.source);
        checkStreamSupportedType(elementType);

        Stream<T> result = null;
        final char[] rbuf = Objectory.createCharArrayBuffer();
        final char[] cbuf = Objectory.createCharArrayBuffer();

        try {
            final JsonDeserConfig configToUse = check(config);

            final JsonReader jr = JsonStreamReader.parse(source, rbuf, cbuf);

            result = this.<T> stream(source, jr, configToUse, elementType).onClose(() -> {
                Objectory.recycle(rbuf);
                Objectory.recycle(cbuf);

                if (closeReaderWhenStreamIsClosed) {
                    IOUtil.closeQuietly(source);
                }
            });
        } finally {
            if (result == null) {
                Objectory.recycle(rbuf);
                Objectory.recycle(cbuf);

                if (closeReaderWhenStreamIsClosed) {
                    IOUtil.closeQuietly(source);
                }
            }
        }

        return result;
    }

    private <T> Stream<T> stream(final Object source, final JsonReader jr, final JsonDeserConfig configToUse, final Type<? extends T> elementType) {
        final Class<? extends T> elementClass = elementType.javaType();
        final int firstToken = jr.nextToken();

        if (firstToken == EOF) {
            return Stream.empty();
        } else if (firstToken != START_BRACKET) {
            throw new UnsupportedOperationException("Only Collection/Array JSON are supported by stream Methods");
        }

        final MutableBoolean hasNextFlag = MutableBoolean.of(false);
        final MutableInt tokenHolder = MutableInt.of(START_BRACKET);

        final BooleanSupplier hasNext = () -> {
            if (hasNextFlag.isTrue()) {
                return true;
            }

            if (tokenHolder.value() == START_BRACKET) {
                if (tokenHolder.setAndGet(jr.nextToken()) != END_BRACKET) {
                    hasNextFlag.setTrue();

                    return true;
                }
            } else {
                if (tokenHolder.setAndGet(jr.nextToken()) == COMMA) {
                    tokenHolder.setAndGet(jr.nextToken());
                }

                if (tokenHolder.value() != END_BRACKET) {
                    hasNextFlag.setTrue();

                    return true;
                }
            }

            return false;
        };

        final Supplier<T> next = () -> {
            hasNextFlag.setFalse();

            try {
                T result = null;

                if (tokenHolder.value() == COMMA) {
                    result = jr.readValue(elementType);
                } else {
                    result = read(source, jr, tokenHolder.value(), configToUse, false, elementClass, elementType);
                }

                tokenHolder.setAndGet(jr.lastToken());

                return result;
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }
        };

        return Stream.iterate(hasNext, next);
    }

    private void checkStreamSupportedType(final Type<?> elementType) {
        switch (elementType.serializationType()) { // NOSONAR
            case ENTITY, MAP, ARRAY, COLLECTION, MAP_ENTITY, DATA_SET, SHEET, ENTITY_ID:
                break;

            default:
                if (!(elementType.isBean() || elementType.isMap() || elementType.isCollection() || elementType.isArray())) {
                    throw new IllegalArgumentException(
                            "Only Bean/Map/Collection/Object Array/Dataset element types are supported by stream methods at present");
                }
        }
    }

    <T> T emptyOrDefault(final Type<? extends T> type) {
        if (type.isCollection() || type.isArray()) {
            return type.valueOf("[]");
        } else if (type.isMap()) {
            return type.valueOf("{}");
        } else if (type.isCharSequence()) {
            return type.valueOf("");
        } else {
            return type.defaultValue();
        }
    }

    private void writeNullToEmpty(final BufferedJsonWriter bw, final Type<?> type) throws IOException {
        if (type.isCollection() || type.isArray()) {
            bw.write("[]");
        } else if (type.isMap()) {
            bw.write("{}");
        } else if (type.isCharSequence()) {
            bw.write("\"\"");
        } else {
            bw.write(NULL_CHAR_ARRAY);
        }
    }

    Object readNullToEmpty(final Type<?> type, final Object value, final boolean readNullToEmpty) {
        if (value == null && readNullToEmpty) {
            if (type.isCollection() || type.isArray()) {
                return type.valueOf("[]");
            } else if (type.isMap()) {
                return type.valueOf("{}");
            } else if (type.isCharSequence()) {
                return type.valueOf("");
            }
        }

        return value;
    }

    private static final Map<Class<?>, Function<Map<Object, Object>, ?>> map2TargetTypeConverterMap = new HashMap<>();

    static {
        map2TargetTypeConverterMap.put(Map.Entry.class, t -> N.isEmpty(t) ? null : t.entrySet().iterator().next());

        map2TargetTypeConverterMap.put(AbstractMap.SimpleEntry.class, t -> N.isEmpty(t) ? null : new AbstractMap.SimpleEntry<>(t.entrySet().iterator().next()));

        map2TargetTypeConverterMap.put(AbstractMap.SimpleImmutableEntry.class,
                t -> N.isEmpty(t) ? null : new AbstractMap.SimpleImmutableEntry<>(t.entrySet().iterator().next()));

        map2TargetTypeConverterMap.put(ImmutableEntry.class, t -> N.isEmpty(t) ? null : ImmutableEntry.copyOf(t.entrySet().iterator().next()));
    }

    private static final Map<Class<?>, BiFunction<List<?>, Type<?>, Object>> list2PairTripleConverterMap = new HashMap<>();

    static {
        list2PairTripleConverterMap.put(Pair.class, (list, eleType) -> {
            final Type<?>[] paramTypes = eleType.parameterTypes();
            return Pair.of(N.convert(list.get(0), paramTypes[0]), N.convert(list.get(1), paramTypes[1]));
        });

        list2PairTripleConverterMap.put(Triple.class, (list, eleType) -> {
            final Type<?>[] paramTypes = eleType.parameterTypes();
            return Triple.of(N.convert(list.get(0), paramTypes[0]), N.convert(list.get(1), paramTypes[1]), N.convert(list.get(2), paramTypes[2]));
        });

        list2PairTripleConverterMap.put(Tuple1.class, (list, eleType) -> {
            final Type<?>[] paramTypes = eleType.parameterTypes();
            return Tuple.of(N.convert(list.get(0), paramTypes[0]));
        });

        list2PairTripleConverterMap.put(Tuple2.class, (list, eleType) -> {
            final Type<?>[] paramTypes = eleType.parameterTypes();
            return Tuple.of(N.convert(list.get(0), paramTypes[0]), N.convert(list.get(1), paramTypes[1]));
        });

        list2PairTripleConverterMap.put(Tuple3.class, (list, eleType) -> {
            final Type<?>[] paramTypes = eleType.parameterTypes();
            return Tuple.of(N.convert(list.get(0), paramTypes[0]), N.convert(list.get(1), paramTypes[1]), N.convert(list.get(2), paramTypes[2]));
        });

        list2PairTripleConverterMap.put(Tuple4.class, (list, eleType) -> {
            final Type<?>[] paramTypes = eleType.parameterTypes();
            return Tuple.of(N.convert(list.get(0), paramTypes[0]), N.convert(list.get(1), paramTypes[1]), N.convert(list.get(2), paramTypes[2]),
                    N.convert(list.get(3), paramTypes[3]));
        });

        list2PairTripleConverterMap.put(Tuple5.class, (list, eleType) -> {
            final Type<?>[] paramTypes = eleType.parameterTypes();
            return Tuple.of(N.convert(list.get(0), paramTypes[0]), N.convert(list.get(1), paramTypes[1]), N.convert(list.get(2), paramTypes[2]),
                    N.convert(list.get(3), paramTypes[3]), N.convert(list.get(4), paramTypes[4]));
        });

        list2PairTripleConverterMap.put(Tuple6.class, (list, eleType) -> {
            final Type<?>[] paramTypes = eleType.parameterTypes();
            return Tuple.of(N.convert(list.get(0), paramTypes[0]), N.convert(list.get(1), paramTypes[1]), N.convert(list.get(2), paramTypes[2]),
                    N.convert(list.get(3), paramTypes[3]), N.convert(list.get(4), paramTypes[4]), N.convert(list.get(5), paramTypes[5]));
        });

        list2PairTripleConverterMap.put(Tuple7.class, (list, eleType) -> {
            final Type<?>[] paramTypes = eleType.parameterTypes();
            return Tuple.of(N.convert(list.get(0), paramTypes[0]), N.convert(list.get(1), paramTypes[1]), N.convert(list.get(2), paramTypes[2]),
                    N.convert(list.get(3), paramTypes[3]), N.convert(list.get(4), paramTypes[4]), N.convert(list.get(5), paramTypes[5]),
                    N.convert(list.get(6), paramTypes[6]));
        });

        list2PairTripleConverterMap.put(Tuple8.class, (list, eleType) -> {
            final Type<?>[] paramTypes = eleType.parameterTypes();
            return Tuple.of(N.convert(list.get(0), paramTypes[0]), N.convert(list.get(1), paramTypes[1]), N.convert(list.get(2), paramTypes[2]),
                    N.convert(list.get(3), paramTypes[3]), N.convert(list.get(4), paramTypes[4]), N.convert(list.get(5), paramTypes[5]),
                    N.convert(list.get(6), paramTypes[6]), N.convert(list.get(7), paramTypes[7]));
        });

        list2PairTripleConverterMap.put(Tuple9.class, (list, eleType) -> {
            final Type<?>[] paramTypes = eleType.parameterTypes();
            return Tuple.of(N.convert(list.get(0), paramTypes[0]), N.convert(list.get(1), paramTypes[1]), N.convert(list.get(2), paramTypes[2]),
                    N.convert(list.get(3), paramTypes[3]), N.convert(list.get(4), paramTypes[4]), N.convert(list.get(5), paramTypes[5]),
                    N.convert(list.get(6), paramTypes[6]), N.convert(list.get(7), paramTypes[7]), N.convert(list.get(8), paramTypes[8]));
        });
    }

    private String getErrorMsg(final JsonReader jr, final int token) {
        switch (token) {
            case START_BRACE:
                return "Error on parsing at '{' with " + jr.getText();

            case END_BRACE:
                return "Error on parsing at '}' with " + jr.getText();

            case START_BRACKET:
                return "Error on parsing at '[' with " + jr.getText();

            case END_BRACKET:
                return "Error on parsing at ']' with " + jr.getText();

            case START_DOUBLE_QUOTE:
                return "Error on parsing at starting '\"' with " + jr.getText();

            case END_DOUBLE_QUOTE:
                return "Error on parsing at ending '\"' with " + jr.getText();

            case START_SINGLE_QUOTE:
                return "Error on parsing at starting ''' with " + jr.getText();

            case END_SINGLE_QUOTE:
                return "Error on parsing at ending ''' with " + jr.getText();

            case COLON:
                return "Error on parsing at ':' with " + jr.getText();

            case COMMA:
                return "Error on parsing at ',' with " + jr.getText();

            default:
                return "Unknown error on event : " + ((char) token) + " with " + jr.getText();
        }
    }
}
