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

import static com.landawn.abacus.parser.AbstractJSONReader.eventChars;
import static com.landawn.abacus.parser.JSONReader.COLON;
import static com.landawn.abacus.parser.JSONReader.COMMA;
import static com.landawn.abacus.parser.JSONReader.END_BRACE;
import static com.landawn.abacus.parser.JSONReader.END_BRACKET;
import static com.landawn.abacus.parser.JSONReader.END_QUOTATION_D;
import static com.landawn.abacus.parser.JSONReader.END_QUOTATION_S;
import static com.landawn.abacus.parser.JSONReader.EOF;
import static com.landawn.abacus.parser.JSONReader.START_BRACE;
import static com.landawn.abacus.parser.JSONReader.START_BRACKET;
import static com.landawn.abacus.parser.JSONReader.START_QUOTATION_D;
import static com.landawn.abacus.parser.JSONReader.START_QUOTATION_S;
import static com.landawn.abacus.parser.JSONReader.UNDEFINED;

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
import com.landawn.abacus.exception.ParseException;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.parser.JSONDeserializationConfig.JDC;
import com.landawn.abacus.parser.ParserUtil.BeanInfo;
import com.landawn.abacus.parser.ParserUtil.PropInfo;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.Array;
import com.landawn.abacus.util.BufferedJSONWriter;
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
 * and vice versa. It extends {@link AbstractJSONParser} and provides optimized handling for
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
 * {@link JSONParser} interface.</p>
 *
 * @see AbstractJSONParser
 * @see JSONSerializationConfig
 * @see JSONDeserializationConfig
 * @see ParserFactory
 */
@SuppressWarnings("deprecation")
final class JSONParserImpl extends AbstractJSONParser {

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

    private static final JSONDeserializationConfig jdcForStringElement = JDC.create().setElementType(String.class);

    private static final JSONDeserializationConfig jdcForTypeElement = JDC.create().setElementType(Type.class);

    private static final JSONDeserializationConfig jdcForPropertiesElement = JDC.create().setElementType(String.class).setMapKeyType(String.class);

    JSONParserImpl() {
    }

    JSONParserImpl(final JSONSerializationConfig jsc, final JSONDeserializationConfig jdc) {
        super(jsc, jdc);
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation uses an internal {@link JSONReader} for efficient parsing
     * and supports circular reference detection when enabled in the configuration.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JSONDeserializationConfig config = new JSONDeserializationConfig()
     *     .ignoreUnmatchedProperty(true)
     *     .readNullToEmpty(true);
     *
     * String json = "{\"name\":\"John\",\"age\":30}";
     * User user = parser.readString(json, config, Type.of(User.class));
     *
     * // Deserialize collection with type safety
     * String jsonArray = "[{\"id\":1},{\"id\":2}]";
     * List<Item> items = parser.readString(jsonArray, config, new TypeReference<List<Item>>() {});
     * }</pre>
     *
     * @param <T> the type of the target object
     * @param source the JSON string to parse; may be {@code null} or empty
     * @param config the deserialization configuration to use; may be {@code null} to use default configuration
     * @param targetType the type of the target object to deserialize into; must not be {@code null}
     * @return the deserialized object of type {@code T}, or {@code null} if the source is {@code null}
     * @throws UncheckedIOException if an I/O error occurs during parsing
     * @throws ParseException if the JSON structure is invalid or doesn't match the target type
     */
    @Override
    public <T> T readString(String source, JSONDeserializationConfig config, Type<? extends T> targetType) {
        final JSONDeserializationConfig configToUse = check(config);

        if ((Strings.isEmpty(source) && configToUse.readNullToEmpty()) || (source != null && source.isEmpty())) {
            return emptyOrDefault(targetType);
        } else if (source == null) {
            return targetType.defaultValue();
        }

        final char[] cbuf = Objectory.createCharArrayBuffer();

        try {
            final JSONReader jr = JSONStringReader.parse(source, cbuf);

            return readString(source, jr, configToUse, targetType, null);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            Objectory.recycle(cbuf);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation delegates to {@link #readString(String, JSONDeserializationConfig, Type)}
     * after converting the class to a Type.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JSONDeserializationConfig config = new JSONDeserializationConfig()
     *     .ignoreUnmatchedProperty(true);
     *
     * String json = "{\"name\":\"John\",\"age\":30}";
     * User user = parser.readString(json, config, User.class);
     * }</pre>
     *
     * @param <T> the type of the target class
     * @param source the JSON string to parse; may be {@code null} or empty
     * @param config the deserialization configuration to use; may be {@code null} to use default configuration
     * @param targetClass the class of the target object to deserialize into; must not be {@code null}
     * @return the deserialized object of type {@code T}, or {@code null} if the source is {@code null}
     * @throws UncheckedIOException if an I/O error occurs during parsing
     * @throws ParseException if the JSON structure is invalid or doesn't match the target class
     */
    @Override
    public <T> T readString(final String source, final JSONDeserializationConfig config, final Class<? extends T> targetClass) {
        return readString(source, config, Type.of(targetClass));
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation parses a JSON array and populates the provided array with
     * deserialized elements. The array must be pre-allocated with sufficient size.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JSONDeserializationConfig config = new JSONDeserializationConfig();
     * String json = "[1, 2, 3, 4, 5]";
     * Integer[] numbers = new Integer[5];
     * parser.readString(json, config, numbers);
     * // numbers array is now filled: [1, 2, 3, 4, 5]
     * }</pre>
     *
     * @param source the JSON array string to parse; may be {@code null} (in which case no action is taken)
     * @param config the deserialization configuration to use; may be {@code null} to use default configuration
     * @param output the pre-allocated array to populate with parsed values; must not be {@code null}
     * @throws UncheckedIOException if an I/O error occurs during parsing
     * @throws ParseException if the JSON structure is invalid or not an array
     * @throws IndexOutOfBoundsException if the JSON array contains more elements than the output array can hold
     */
    @Override
    public void readString(final String source, final JSONDeserializationConfig config, final Object[] output) {
        final JSONDeserializationConfig configToUse = check(config);

        //    if (N.isEmpty(str)) { // TODO ?
        //        return;
        //    }

        if (source == null) {
            return;
        }

        final char[] cbuf = Objectory.createCharArrayBuffer();

        try {
            final JSONReader jr = JSONStringReader.parse(source, cbuf);

            readString(source, jr, configToUse, Type.of(output.getClass()), output);
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
     * JSONDeserializationConfig config = new JSONDeserializationConfig();
     * String json = "[\"apple\", \"banana\", \"orange\"]";
     * List<String> fruits = new ArrayList<>();
     * parser.readString(json, config, fruits);
     * // fruits now contains: ["apple", "banana", "orange"]
     * }</pre>
     *
     * @param source the JSON array string to parse; may be {@code null} (in which case no action is taken)
     * @param config the deserialization configuration to use; may be {@code null} to use default configuration
     * @param output the collection to populate with parsed elements; must not be {@code null}
     * @throws UncheckedIOException if an I/O error occurs during parsing
     * @throws ParseException if the JSON structure is invalid or not an array
     * @throws UnsupportedOperationException if the collection is unmodifiable
     */
    @Override
    public void readString(final String source, final JSONDeserializationConfig config, final Collection<?> output) {
        final JSONDeserializationConfig configToUse = check(config);

        //    if (N.isEmpty(str)) { // TODO ?
        //        return;
        //    }

        if (source == null) {
            return;
        }

        final char[] cbuf = Objectory.createCharArrayBuffer();

        try {
            final JSONReader jr = JSONStringReader.parse(source, cbuf);

            readString(source, jr, configToUse, Type.of(output.getClass()), output);
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
     * JSONDeserializationConfig config = new JSONDeserializationConfig();
     * String json = "{\"key1\":\"value1\",\"key2\":\"value2\"}";
     * Map<String, String> map = new HashMap<>();
     * parser.readString(json, config, map);
     * // map now contains: {key1=value1, key2=value2}
     * }</pre>
     *
     * @param source the JSON object string to parse; may be {@code null} or empty (in which case no action is taken)
     * @param config the deserialization configuration to use; may be {@code null} to use default configuration
     * @param output the map to populate with parsed key-value pairs; must not be {@code null}
     * @throws UncheckedIOException if an I/O error occurs during parsing
     * @throws ParseException if the JSON structure is invalid or not an object
     * @throws UnsupportedOperationException if the map is unmodifiable
     */
    @Override
    public void readString(final String source, final JSONDeserializationConfig config, final Map<?, ?> output) {
        final JSONDeserializationConfig configToUse = check(config);

        if (Strings.isEmpty(source)) {
            return;
        }

        final char[] cbuf = Objectory.createCharArrayBuffer();

        try {
            final JSONReader jr = JSONStringReader.parse(source, cbuf);

            readString(source, jr, configToUse, Type.of(output.getClass()), output);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            Objectory.recycle(cbuf);
        }
    }

    /**
     * Reads and deserializes a JSON string using a JSONReader into the specified target class.
     *
     * <p>This is an internal method that performs the actual JSON parsing using a JSONReader.
     * It handles various serialization types including arrays, collections, maps, beans, datasets,
     * sheets, and entity IDs. The method dispatches to type-specific read methods based on the
     * serialization type of the target class.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JSONReader jr = JSONStringReader.parse(jsonString, charBuffer);
     * MyBean bean = readString(jsonString, jr, config, Type.of(MyBean.class), null);
     * }</pre>
     *
     * @param <T> the type of the target object
     * @param source the original JSON string source
     * @param jr the JSONReader instance for parsing the JSON
     * @param config the deserialization configuration
     * @param targetType the type of the target object to deserialize into
     * @param output optional pre-existing output object (array, collection, or map) to populate; may be {@code null}
     * @return the deserialized object of type {@code T}
     * @throws IOException if an I/O error occurs during reading
     * @throws ParseException if the JSON structure doesn't match the target class or is invalid
     */
    @SuppressWarnings("unchecked")
    protected <T> T readString(final String source, final JSONReader jr, final JSONDeserializationConfig config, final Type<? extends T> targetType,
            final Object output) throws IOException {
        final Class<? extends T> targetClass = targetType.clazz();
        final Object[] a = (output instanceof Object[]) ? (Object[]) output : null;
        final Collection<Object> c = (output instanceof Collection) ? (Collection<Object>) output : null;
        final Map<Object, Object> m = (output instanceof Map) ? (Map<Object, Object>) output : null;

        switch (targetType.getSerializationType()) {
            case SERIALIZABLE:
                if (targetType.isArray()) {
                    return readArray(jr, config, null, true, targetClass, targetType, a);
                } else if (targetType.isCollection()) {
                    return readCollection(jr, config, null, null, true, targetClass, targetType, c);
                } else {
                    return (T) readNullToEmpty(targetType, targetType.valueOf(source), config.readNullToEmpty());
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

                throw new ParseException("Unsupported class: " + ClassUtil.getCanonicalClassName(targetType.clazz()) //NOSONAR
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
     * JSONSerializationConfig config = new JSONSerializationConfig()
     *     .prettyFormat(true)
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
    public String serialize(final Object obj, final JSONSerializationConfig config) {
        final JSONSerializationConfig configToUse = check(config);

        if (obj == null) {
            return null;
        }

        final Class<?> cls = obj.getClass();
        final Type<Object> type = Type.of(cls);

        if (type.isSerializable() && !(type.isCollection() || type.isArray() || type.clazz().isEnum())) {
            return type.stringOf(obj);
        }

        final BufferedJSONWriter bw = Objectory.createBufferedJSONWriter();
        final IdentityHashSet<Object> serializedObjects = !configToUse.supportCircularReference() ? null : new IdentityHashSet<>();

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
     * JSONSerializationConfig config = new JSONSerializationConfig()
     *     .prettyFormat(true);
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
    public void serialize(final Object obj, final JSONSerializationConfig config, final File output) {
        final JSONSerializationConfig configToUse = check(config);

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

        if (type.isSerializable() && !(type.isCollection() || type.isArray() || type.clazz().isEnum())) {
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
     * JSONSerializationConfig config = new JSONSerializationConfig();
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
    public void serialize(final Object obj, final JSONSerializationConfig config, final OutputStream output) {
        final JSONSerializationConfig configToUse = check(config);

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

        if (type.isSerializable() && !(type.isCollection() || type.isArray() || type.clazz().isEnum())) {
            try {
                IOUtil.write(type.stringOf(obj), output, true);
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }

            return;
        }

        final BufferedJSONWriter bw = Objectory.createBufferedJSONWriter(output);
        final IdentityHashSet<Object> serializedObjects = !configToUse.supportCircularReference() ? null : new IdentityHashSet<>();

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
     * JSONSerializationConfig config = new JSONSerializationConfig()
     *     .prettyFormat(true);
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
    public void serialize(final Object obj, final JSONSerializationConfig config, final Writer output) {
        final JSONSerializationConfig configToUse = check(config);

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

        if (type.isSerializable() && !(type.isCollection() || type.isArray() || type.clazz().isEnum())) {
            try {
                IOUtil.write(type.stringOf(obj), output, true);
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }

            return;
        }

        final boolean isBufferedWriter = output instanceof BufferedJSONWriter;
        final BufferedJSONWriter bw = isBufferedWriter ? (BufferedJSONWriter) output : Objectory.createBufferedJSONWriter(output);
        final IdentityHashSet<Object> serializedObjects = !configToUse.supportCircularReference() ? null : new IdentityHashSet<>();

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
    protected void write(final Object obj, final JSONSerializationConfig config, final boolean isFirstCall, final String indentation,
            final IdentityHashSet<Object> serializedObjects, final BufferedJSONWriter bw) throws IOException {
        if (hasCircularReference(obj, serializedObjects, config, bw)) {
            return;
        }

        final Type<Object> type = Type.of(obj.getClass());

        switch (type.getSerializationType()) {
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
                if (config == null || config.failOnEmptyBean()) {
                    throw new ParseException("Unsupported class: " + ClassUtil.getCanonicalClassName(type.clazz())
                            + ". Only Array/List/Map and Bean class with getter/setter methods are supported");
                } else {
                    bw.write("{}");
                }
        }
    }

    protected void write(final Object obj, final JSONSerializationConfig config, final IdentityHashSet<Object> serializedObjects, final Type<Object> type,
            final BufferedJSONWriter bw, final boolean flush) throws IOException {
        if (config.bracketRootValue() || !type.isSerializable()) {
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
    protected void write(final Object obj, final JSONSerializationConfig config, final boolean isFirstCall, final String indentation,
            final IdentityHashSet<Object> serializedObjects, final Type<Object> type, final BufferedJSONWriter bw, final boolean flush) throws IOException {
        if (obj == null) {
            return;
        }

        write(obj, config, isFirstCall, indentation, serializedObjects, bw);

        if (flush) {
            bw.flush();
        }
    }

    protected void writeBean(final Object obj, final JSONSerializationConfig config, final boolean isFirstCall, final String indentation,
            final IdentityHashSet<Object> serializedObjects, final Type<Object> type, final BufferedJSONWriter bw) throws IOException {
        //    if (hasCircularReference(obj, serializedObjects, config, bw)) {
        //        return;
        //    }

        final Class<?> cls = type.clazz();
        final BeanInfo beanInfo = ParserUtil.getBeanInfo(type.javaType());

        if (N.isEmpty(beanInfo.jsonXmlSerializablePropInfos)) {
            throw new ParseException("No serializable property is found in class: " + ClassUtil.getCanonicalClassName(cls));
        }

        final Exclusion exclusion = getExclusion(config, beanInfo);

        final boolean ignoreNullProperty = (exclusion == Exclusion.NULL) || (exclusion == Exclusion.DEFAULT);
        final boolean ignoreDefaultProperty = (exclusion == Exclusion.DEFAULT);

        final Collection<String> ignoredClassPropNames = config.getIgnoredPropNames(cls);
        final boolean writeNullToEmpty = config.writeNullToEmpty();
        final boolean quotePropName = config.quotePropName();
        final boolean isPrettyFormat = config.prettyFormat();
        final NamingPolicy jsonXmlNamingPolicy = config.getPropNamingPolicy() == null ? beanInfo.jsonXmlNamingPolicy : config.getPropNamingPolicy();
        final int nameTagIdx = jsonXmlNamingPolicy.ordinal();

        final PropInfo[] propInfoList = config.skipTransientField() ? beanInfo.nonTransientSeriPropInfos : beanInfo.jsonXmlSerializablePropInfos;
        PropInfo propInfo = null;
        String propName = null;
        Object propValue = null;

        if (config.bracketRootValue() || !isFirstCall) {
            bw.write(_BRACE_L);
        }

        String nextIndentation = isPrettyFormat ? ((indentation == null ? Strings.EMPTY : indentation) + config.getIndentation()) : null;

        if (config.wrapRootValue()) {
            if (isPrettyFormat) {
                bw.write(IOUtil.LINE_SEPARATOR_UNIX);

                if (indentation != null) {
                    bw.write(indentation);
                }

                bw.write(config.getIndentation());
            }

            if (config.quotePropName()) {
                bw.write(_D_QUOTATION);
                bw.write(ClassUtil.getSimpleClassName(cls));
                bw.write(_D_QUOTATION);
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

            if (propInfo.jsonXmlExpose == JsonXmlField.Expose.DESERIALIZE_ONLY
                    || ((ignoredClassPropNames != null) && ignoredClassPropNames.contains(propName))) {
                continue;
            }

            propValue = propInfo.getPropValue(obj);

            if ((ignoreNullProperty && propValue == null) || (ignoreDefaultProperty && propValue != null && (propInfo.jsonXmlType != null)
                    && propInfo.jsonXmlType.isPrimitiveType() && propValue.equals(propInfo.jsonXmlType.defaultValue()))) {
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

        if (config.wrapRootValue())

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

        if (config.bracketRootValue() || !isFirstCall) {
            if (isPrettyFormat && (config.wrapRootValue() || cnt > 0)) {
                bw.write(IOUtil.LINE_SEPARATOR_UNIX);

                if (indentation != null) {
                    bw.write(indentation);
                }
            }

            bw.write(_BRACE_R);
        }
    }

    @SuppressWarnings("unused")
    protected void writeMap(final Map<?, ?> m, final JSONSerializationConfig config, final boolean isFirstCall, final String indentation,
            final IdentityHashSet<Object> serializedObjects, final Type<Object> type, final BufferedJSONWriter bw) throws IOException {
        //    if (hasCircularReference(m, serializedObjects, config, bw)) {
        //        return;
        //    }

        final Collection<String> ignoredClassPropNames = config.getIgnoredPropNames(Map.class);
        // final boolean ignoreNullProperty = (config.getExclusion() == Exclusion.NULL) || (config.getExclusion() == Exclusion.DEFAULT);
        final boolean isQuoteMapKey = config.quoteMapKey();
        final boolean isPrettyFormat = config.prettyFormat();

        Type<Object> keyType = null;
        int i = 0;

        if (config.bracketRootValue() || !isFirstCall) {
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
                bw.write(NULL_CHAR_ARRAY);
            } else {
                keyType = Type.of(key.getClass());

                if (keyType.isSerializable() && !(keyType.isArray() || keyType.isCollection() || keyType.clazz().isEnum())) {
                    if (isQuoteMapKey || !(keyType.isNumber() || keyType.isBoolean())) {
                        bw.write(_D_QUOTATION);
                        bw.writeCharacter(keyType.stringOf(key));
                        bw.write(_D_QUOTATION);
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

        if (config.bracketRootValue() || !isFirstCall) {
            if (isPrettyFormat && N.notEmpty(m)) {
                bw.write(IOUtil.LINE_SEPARATOR_UNIX);

                if (indentation != null) {
                    bw.write(indentation);
                }
            }

            bw.write(_BRACE_R);
        }
    }

    protected void writeArray(final Object obj, final JSONSerializationConfig config, final boolean isFirstCall, final String indentation,
            final IdentityHashSet<Object> serializedObjects, final Type<Object> type, final BufferedJSONWriter bw) throws IOException {
        //    if (hasCircularReference(obj, serializedObjects, config, bw)) {
        //        return;
        //    }

        final boolean isPrimitiveArray = type.isPrimitiveArray();
        final boolean isPrettyFormat = config.prettyFormat();

        // TODO what to do if it's primitive array(e.g: int[]...)
        if (config.bracketRootValue() || !isFirstCall) {
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

        if (config.bracketRootValue() || !isFirstCall) {
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
    protected void writeCollection(final Collection<?> c, final JSONSerializationConfig config, final boolean isFirstCall, final String indentation,
            final IdentityHashSet<Object> serializedObjects, final Type<Object> type, final BufferedJSONWriter bw) throws IOException {
        //    if (hasCircularReference(c, serializedObjects, config, bw)) {
        //        return;
        //    }

        final boolean isPrettyFormat = config.prettyFormat();

        if (config.bracketRootValue() || !isFirstCall) {
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

        if (config.bracketRootValue() || !isFirstCall) {
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
    protected void writeMapEntity(final MapEntity mapEntity, final JSONSerializationConfig config, final boolean isFirstCall, final String indentation,
            final IdentityHashSet<Object> serializedObjects, final Type<Object> type, final BufferedJSONWriter bw) throws IOException {
        //    if (hasCircularReference(mapEntity, serializedObjects, config, bw)) {
        //        return;
        //    }

        final NamingPolicy jsonXmlNamingPolicy = config.getPropNamingPolicy();
        final boolean quotePropName = config.quotePropName();
        final boolean isPrettyFormat = config.prettyFormat();

        if (config.bracketRootValue() || !isFirstCall) {
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
            bw.write(_D_QUOTATION);
            bw.write(mapEntity.entityName());
            bw.write(_D_QUOTATION);
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
                    bw.write(_D_QUOTATION);
                    bw.write(jsonXmlNamingPolicy == null ? propName : jsonXmlNamingPolicy.convert(propName));
                    bw.write(_D_QUOTATION);
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

        if (config.bracketRootValue() || !isFirstCall) {
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
    protected void writeEntityId(final EntityId entityId, final JSONSerializationConfig config, final boolean isFirstCall, final String indentation,
            final IdentityHashSet<Object> serializedObjects, final Type<Object> type, final BufferedJSONWriter bw) throws IOException {
        //    if (hasCircularReference(entityId, serializedObjects, config, bw)) {
        //        return;
        //    }

        final NamingPolicy jsonXmlNamingPolicy = config.getPropNamingPolicy();
        final boolean quotePropName = config.quotePropName();
        final boolean isPrettyFormat = config.prettyFormat();

        if (config.bracketRootValue() || !isFirstCall) {
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
            bw.write(_D_QUOTATION);
            bw.write(entityId.entityName());
            bw.write(_D_QUOTATION);
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
                    bw.write(_D_QUOTATION);
                    bw.write(jsonXmlNamingPolicy == null ? propName : jsonXmlNamingPolicy.convert(propName));
                    bw.write(_D_QUOTATION);
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

        if (config.bracketRootValue() || !isFirstCall) {
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
    protected void writeDataset(final Dataset ds, final JSONSerializationConfig config, final boolean isFirstCall, final String indentation,
            final IdentityHashSet<Object> serializedObjects, final Type<Object> type, final BufferedJSONWriter bw) throws IOException {
        //    if (hasCircularReference(ds, serializedObjects, config, bw)) {
        //        return;
        //    }

        if (config.writeDatasetByRow()) {
            writeCollection(ds.toList(LinkedHashMap.class), config, isFirstCall, indentation, serializedObjects, type, bw);
            return;
        }

        final boolean quotePropName = config.quotePropName();
        final boolean isPrettyFormat = config.prettyFormat();
        final boolean writeColumnType = config.writeColumnType();

        final String nextIndentation = isPrettyFormat ? ((indentation == null ? Strings.EMPTY : indentation) + config.getIndentation()) : null;

        if (config.bracketRootValue() || !isFirstCall) {
            bw.write(_BRACE_L);
        }

        final List<String> columnNames = ds.columnNameList();

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
            bw.write(_D_QUOTATION);
            bw.write(COLUMN_NAMES);
            bw.write(_D_QUOTATION);
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
                bw.write(_D_QUOTATION);
                bw.write(COLUMN_TYPES);
                bw.write(_D_QUOTATION);
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
                bw.write(_D_QUOTATION);
                bw.write(PROPERTIES);
                bw.write(_D_QUOTATION);
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
                bw.write(_D_QUOTATION);
                bw.write(IS_FROZEN);
                bw.write(_D_QUOTATION);
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
            bw.write(_D_QUOTATION);
            bw.write(COLUMNS);
            bw.write(_D_QUOTATION);
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
                    bw.write(_D_QUOTATION);
                    bw.write(columnName);
                    bw.write(_D_QUOTATION);
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

        if (config.bracketRootValue() || !isFirstCall) {
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
    protected void writeSheet(final Sheet sheet, final JSONSerializationConfig config, final boolean isFirstCall, final String indentation,
            final IdentityHashSet<Object> serializedObjects, final Type<Object> type, final BufferedJSONWriter bw) throws IOException {
        //    if (hasCircularReference(sheet, serializedObjects, config, bw)) {
        //        return;
        //    }

        final boolean quotePropName = config.quotePropName();
        final boolean isPrettyFormat = config.prettyFormat();
        final boolean writeRowColumnKeyType = config.writeRowColumnKeyType();
        final boolean writeColumnType = config.writeColumnType();

        final String nextIndentation = isPrettyFormat ? ((indentation == null ? Strings.EMPTY : indentation) + config.getIndentation()) : null;

        if (config.bracketRootValue() || !isFirstCall) {
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
                bw.write(_D_QUOTATION);
                bw.write(ROW_KEY_TYPE);
                bw.write(_D_QUOTATION);
            } else {
                bw.write(ROW_KEY_TYPE);
            }

            bw.write(COLON_SPACE_CHAR_ARRAY);

            Class<?> eleTypeClass = getElementType(sheet.rowKeySet());
            final String rowKeyTypeName = eleTypeClass == null ? null : Type.of(eleTypeClass).name();

            if (rowKeyTypeName != null) {
                bw.write(_D_QUOTATION);
                bw.write(rowKeyTypeName);
                bw.write(_D_QUOTATION);
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
                bw.write(_D_QUOTATION);
                bw.write(COLUMN_KEY_TYPE);
                bw.write(_D_QUOTATION);
            } else {
                bw.write(COLUMN_KEY_TYPE);
            }

            bw.write(COLON_SPACE_CHAR_ARRAY);

            eleTypeClass = getElementType(sheet.columnKeySet());
            final String columnKeyTypeName = eleTypeClass == null ? null : Type.of(eleTypeClass).name();

            if (columnKeyTypeName != null) {
                bw.write(_D_QUOTATION);
                bw.write(columnKeyTypeName);
                bw.write(_D_QUOTATION);
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
            bw.write(_D_QUOTATION);
            bw.write(ROW_KEY_SET);
            bw.write(_D_QUOTATION);
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
            bw.write(_D_QUOTATION);
            bw.write(COLUMN_KEY_SET);
            bw.write(_D_QUOTATION);
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
                bw.write(_D_QUOTATION);
                bw.write(COLUMN_TYPES);
                bw.write(_D_QUOTATION);
            } else {
                bw.write(COLUMN_TYPES);
            }

            bw.write(COLON_SPACE_CHAR_ARRAY);

            final List<String> types = Objectory.createList();
            Class<?> eleTypeClass = null;

            for (final Object columnKey : sheet.columnKeySet()) {
                eleTypeClass = getElementType(sheet.getColumn(columnKey));

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
            bw.write(_D_QUOTATION);
            bw.write(COLUMNS);
            bw.write(_D_QUOTATION);
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
                column = sheet.getColumn(columnKey);

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
                    bw.write(_D_QUOTATION);
                    bw.write(columnName);
                    bw.write(_D_QUOTATION);
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

        if (config.bracketRootValue() || !isFirstCall) {
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
     * JSONDeserializationConfig config = new JSONDeserializationConfig()
     *     .ignoreUnmatchedProperty(true)
     *     .readNullToEmpty(true);
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
     * @throws ParseException if the JSON structure is invalid or doesn't match the target type
     */
    @Override
    public <T> T deserialize(String source, JSONDeserializationConfig config, Type<? extends T> targetType) {
        final JSONDeserializationConfig configToUse = check(config);

        if ((Strings.isEmpty(source) && configToUse.readNullToEmpty()) || (source != null && source.isEmpty())) {
            return emptyOrDefault(targetType);
        } else if (source == null) {
            return targetType.defaultValue();
        }

        final char[] cbuf = Objectory.createCharArrayBuffer();

        try {
            final JSONReader jr = JSONStringReader.parse(source, cbuf);

            return read(source, jr, configToUse, targetType.clazz(), targetType);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            Objectory.recycle(cbuf);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation delegates to {@link #deserialize(String, JSONDeserializationConfig, Type)}
     * after converting the class to a Type.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JSONDeserializationConfig config = new JSONDeserializationConfig();
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
     * @throws ParseException if the JSON structure is invalid or doesn't match the target class
     */
    @Override
    public <T> T deserialize(final String source, final JSONDeserializationConfig config, final Class<? extends T> targetClass) {
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
     * JSONDeserializationConfig config = new JSONDeserializationConfig();
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
     * @throws ParseException if the JSON structure is invalid or doesn't match the target type
     */
    @Override
    public <T> T deserialize(String source, int fromIndex, int toIndex, JSONDeserializationConfig config, Type<? extends T> targetType) {
        N.checkFromToIndex(fromIndex, toIndex, N.len(source));

        final JSONDeserializationConfig configToUse = check(config);

        if ((Strings.isEmpty(source) && configToUse.readNullToEmpty()) || (source != null && fromIndex == toIndex)) {
            return emptyOrDefault(targetType);
        } else if (source == null) {
            return targetType.defaultValue();
        }

        final char[] cbuf = Objectory.createCharArrayBuffer();

        try {
            final JSONReader jr = JSONStringReader.parse(source, fromIndex, toIndex, cbuf);

            Object sourceToUse = fromIndex == 0 && toIndex == N.len(source) ? source : CharBuffer.wrap(source, fromIndex, toIndex);
            return read(sourceToUse, jr, configToUse, targetType.clazz(), targetType);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            Objectory.recycle(cbuf);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation delegates to {@link #deserialize(String, int, int, JSONDeserializationConfig, Type)}
     * after converting the class to a Type.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String json = "prefix{\"name\":\"John\"}suffix";
     * JSONDeserializationConfig config = new JSONDeserializationConfig();
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
     * @throws ParseException if the JSON structure is invalid or doesn't match the target class
     */
    @Override
    public <T> T deserialize(final String source, final int fromIndex, final int toIndex, final JSONDeserializationConfig config,
            final Class<? extends T> targetClass) throws IndexOutOfBoundsException {
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
     * JSONDeserializationConfig config = new JSONDeserializationConfig()
     *     .ignoreUnmatchedProperty(true);
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
     * @throws ParseException if the JSON structure is invalid or doesn't match the target type
     */
    @Override
    public <T> T deserialize(File source, JSONDeserializationConfig config, Type<? extends T> targetType) {
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
     * <p>This implementation delegates to {@link #deserialize(File, JSONDeserializationConfig, Type)}
     * after converting the class to a Type.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JSONDeserializationConfig config = new JSONDeserializationConfig();
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
     * @throws ParseException if the JSON structure is invalid or doesn't match the target class
     */
    @Override
    public <T> T deserialize(final File source, final JSONDeserializationConfig config, final Class<? extends T> targetClass) {
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
     * JSONDeserializationConfig config = new JSONDeserializationConfig();
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
     * @throws ParseException if the JSON structure is invalid or doesn't match the target type
     */
    @Override
    public <T> T deserialize(InputStream source, JSONDeserializationConfig config, Type<? extends T> targetType) {
        // No need to close the reader because the InputStream will/should be closely externally.
        final Reader reader = IOUtil.newInputStreamReader(source); // NOSONAR

        return deserialize(reader, config, targetType);
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation delegates to {@link #deserialize(InputStream, JSONDeserializationConfig, Type)}
     * after converting the class to a Type.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JSONDeserializationConfig config = new JSONDeserializationConfig();
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
     * @throws ParseException if the JSON structure is invalid or doesn't match the target class
     */
    @Override
    public <T> T deserialize(final InputStream source, final JSONDeserializationConfig config, final Class<? extends T> targetClass) {
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
     * JSONDeserializationConfig config = new JSONDeserializationConfig();
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
     * @throws ParseException if the JSON structure is invalid or doesn't match the target type
     */
    @Override
    public <T> T deserialize(Reader source, JSONDeserializationConfig config, final Type<? extends T> targetType) {
        return read(source, config, targetType);
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation delegates to {@link #deserialize(Reader, JSONDeserializationConfig, Type)}
     * after converting the class to a Type.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JSONDeserializationConfig config = new JSONDeserializationConfig();
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
     * @throws ParseException if the JSON structure is invalid or doesn't match the target class
     */
    @Override
    public <T> T deserialize(final Reader source, final JSONDeserializationConfig config, final Class<? extends T> targetClass) {
        return deserialize(source, config, Type.of(targetClass));
    }

    protected <T> T read(final Reader source, final JSONDeserializationConfig config, final Type<? extends T> targetType) {
        final JSONDeserializationConfig configToUse = check(config);
        final char[] rbuf = Objectory.createCharArrayBuffer();
        final char[] cbuf = Objectory.createCharArrayBuffer();

        try {
            final JSONReader jr = JSONStreamReader.parse(source, rbuf, cbuf);

            return read(source, jr, configToUse, targetType.clazz(), targetType);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            Objectory.recycle(cbuf);
            Objectory.recycle(rbuf);
        }
    }

    protected <T> T read(final Object source, final JSONReader jr, final JSONDeserializationConfig config, final Class<? extends T> targetClass,
            final Type<? extends T> targetType) throws IOException {
        return read(source, jr, UNDEFINED, config, true, targetClass, targetType);
    }

    @SuppressWarnings("unchecked")
    protected <T> T read(final Object source, final JSONReader jr, final int lastToken, final JSONDeserializationConfig config, final boolean isFirstCall,
            final Class<? extends T> targetClass, final Type<? extends T> targetType) throws IOException {
        switch (targetType.getSerializationType()) {
            case SERIALIZABLE:
                if (targetType.isArray()) {
                    return readArray(jr, config, null, isFirstCall, targetClass, targetType, null);
                } else if (targetType.isCollection()) {
                    return readCollection(jr, config, null, null, isFirstCall, targetClass, targetType, null);
                } else {
                    return (T) readNullToEmpty(targetType, targetType.valueOf(source instanceof String ? (String) source
                            : (source instanceof Reader ? IOUtil.readAllToString(((Reader) source)) : source.toString())), config.readNullToEmpty());
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

                throw new ParseException(firstTokenToUse, "Unsupported class: " + ClassUtil.getCanonicalClassName(targetType.clazz())
                        + ". Only Array/List/Map and Bean class with getter/setter methods are supported");
        }
    }

    @SuppressWarnings("unused")
    protected <T> T readBean(final JSONReader jr, final JSONDeserializationConfig config, final boolean isFirstCall, final Class<? extends T> targetClass,
            final Type<? extends T> targetType) throws IOException {
        final boolean hasValueTypes = config.hasValueTypes();
        final boolean ignoreUnmatchedProperty = config.ignoreUnmatchedProperty();
        final boolean ignoreNullOrEmpty = config.ignoreNullOrEmpty();
        final boolean readNullToEmpty = config.readNullToEmpty();
        final Collection<String> ignoredClassPropNames = config.getIgnoredPropNames(targetClass);
        final BeanInfo beanInfo = ParserUtil.getBeanInfo(targetType.javaType());
        final Object result = beanInfo.createBeanResult();

        PropInfo propInfo = null;
        String propName = null;
        Object propValue = null;
        boolean isPropName = true;
        Type<Object> propType = null;

        final int firstToken = isFirstCall ? jr.nextToken() : START_BRACE;

        if (firstToken == EOF) {
            if (Strings.isNotEmpty(jr.getText())) {
                throw new ParseException(firstToken, "Can't parse: " + jr.getText()); //NOSONAR
            }

            return null; // result;
        }

        // for (int token = firstToken == START_BRACE ? jr.nextToken() :
        // firstToken;; token = isPropName ? jr.nextNameToken() :
        // jr.nextToken()) { // TODO .Why it's even slower by jr.nextNameToken
        // which has less comparison. Fuck???!!!...
        for (int token = firstToken == START_BRACE ? jr.nextToken() : firstToken;; token = jr.nextToken(propInfo == null ? strType : propInfo.jsonXmlType)) {
            switch (token) {
                case START_QUOTATION_D, START_QUOTATION_S:

                    break;

                case END_QUOTATION_D, END_QUOTATION_S:

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
                                throw new ParseException("Unknown property: " + propName);
                            }
                        }
                    } else {
                        if (propInfo == null || propInfo.jsonXmlExpose == JsonXmlField.Expose.SERIALIZE_ONLY
                                || (propName != null && ignoredClassPropNames != null && ignoredClassPropNames.contains(propName))) {
                            // ignore.
                        } else {
                            propValue = readValue(jr, readNullToEmpty, propInfo, propInfo.jsonXmlType);
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
                                    throw new ParseException("Unknown property: " + propName);
                                }
                            }
                        }
                    } else {
                        throw new ParseException(token, getErrorMsg(jr, token));
                    }

                    break;

                case COMMA:

                    if (isPropName) {
                        throw new ParseException(token, getErrorMsg(jr, token));
                    } else {
                        isPropName = true;

                        if (jr.hasText()) {
                            if (propInfo == null || propInfo.jsonXmlExpose == JsonXmlField.Expose.SERIALIZE_ONLY
                                    || (propName != null && ignoredClassPropNames != null && ignoredClassPropNames.contains(propName))) {
                                // ignore.
                            } else {
                                propValue = readValue(jr, readNullToEmpty, propInfo, propInfo.jsonXmlType);
                                setPropValue(propInfo, propValue, result, ignoreNullOrEmpty);
                            }
                        }
                    }

                    break;

                case START_BRACE:

                    if (isPropName) {
                        throw new ParseException(token, getErrorMsg(jr, token));
                    }

                    if (propInfo == null || propInfo.jsonXmlExpose == JsonXmlField.Expose.SERIALIZE_ONLY
                            || (propName != null && ignoredClassPropNames != null && ignoredClassPropNames.contains(propName))) {
                        readMap(jr, defaultJSONDeserializationConfig, null, false, Map.class, null, null);
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
                        throw new ParseException(token, getErrorMsg(jr, token));
                    }

                    if (propInfo == null || propInfo.jsonXmlExpose == JsonXmlField.Expose.SERIALIZE_ONLY
                            || (propName != null && ignoredClassPropNames != null && ignoredClassPropNames.contains(propName))) {
                        readCollection(jr, defaultJSONDeserializationConfig, null, config.getPropHandler(propName), false, List.class, null, null);
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
                            propValue = readBracketedValue(jr, config, config.getPropHandler(propName), propType);
                        }

                        setPropValue(propInfo, propValue, result, ignoreNullOrEmpty);
                    }

                    break;

                case END_BRACE, EOF:

                    if ((isPropName && propInfo != null) /* check for empty JSON text {} */
                            || (isPropName && Strings.isEmpty(propName) && jr.hasText()) /*check for invalid JSON text: {abc}*/) {
                        throw new ParseException(token, getErrorMsg(jr, token));
                    } else if ((firstToken == START_BRACE) == (token != END_BRACE)) {
                        throw new ParseException(token, "The JSON text should be wrapped or unwrapped with \"[]\" or \"{}\""); //NOSONAR
                    } else {
                        if (jr.hasText()) {
                            if (propInfo == null || propInfo.jsonXmlExpose == JsonXmlField.Expose.SERIALIZE_ONLY
                                    || (propName != null && ignoredClassPropNames != null && ignoredClassPropNames.contains(propName))) {
                                // ignore.
                            } else {
                                propValue = readValue(jr, readNullToEmpty, propInfo, propInfo.jsonXmlType);
                                setPropValue(propInfo, propValue, result, ignoreNullOrEmpty);
                            }
                        }
                    }

                    return beanInfo.finishBeanResult(result);
                default:
                    throw new ParseException(token, getErrorMsg(jr, token));
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
    protected <T> T readMap(final JSONReader jr, final JSONDeserializationConfig config, Type<?> propType, final boolean isFirstCall,
            final Class<? extends T> targetClass, final Type<? extends T> targetType, final Map<Object, Object> output) throws IOException {
        Type<?> keyType = defaultKeyType;

        if (propType != null && propType.isMap() && !propType.getParameterTypes()[0].isObjectType()) {
            keyType = propType.getParameterTypes()[0];
        } else if (targetType != null && targetType.isMap() && !targetType.getParameterTypes()[0].isObjectType()) {
            keyType = targetType.getParameterTypes()[0];
        } else if ((propType == null || !propType.isObjectType()) && (config.getMapKeyType() != null && !config.getMapKeyType().isObjectType())) {
            keyType = config.getMapKeyType();
        }

        final boolean isStringKey = String.class == keyType.clazz();

        Type<?> valueType = defaultValueType;

        if (propType != null && propType.isMap() && !propType.getParameterTypes()[1].isObjectType()) {
            valueType = propType.getParameterTypes()[1];
        } else if (targetType != null && targetType.isMap() && !targetType.getParameterTypes()[1].isObjectType()) {
            valueType = targetType.getParameterTypes()[1];
        } else if ((propType == null || !propType.isObjectType()) && (config.getMapValueType() != null && !config.getMapValueType().isObjectType())) {
            valueType = config.getMapValueType();
        }

        final boolean hasValueTypes = config.hasValueTypes();
        final Collection<String> ignoredClassPropNames = config.getIgnoredPropNames(Map.class);
        final boolean ignoreNullOrEmpty = config.ignoreNullOrEmpty();
        final boolean readNullToEmpty = config.readNullToEmpty();

        final Tuple2<Function<Class<?>, Object>, Function<Object, Object>> creatorAndConvertor = getCreatorAndConvertorForTargetType(targetClass, null);

        @SuppressWarnings("rawtypes")
        final Map<Object, Object> result = output == null
                ? (Map.class.isAssignableFrom(targetClass) ? (Map<Object, Object>) creatorAndConvertor._1.apply(targetClass)
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

            return (T) creatorAndConvertor._2.apply(result);
        }

        for (int token = firstToken == START_BRACE ? jr.nextToken() : firstToken;; token = jr.nextToken()) {
            switch (token) {
                case START_QUOTATION_D, START_QUOTATION_S:

                    break;

                case END_QUOTATION_D, END_QUOTATION_S:

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
                        throw new ParseException(token, getErrorMsg(jr, token));
                    }

                    break;

                case COMMA:

                    if (isKey) {
                        throw new ParseException(token, getErrorMsg(jr, token));
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
                        propType = valueType;
                    } else {
                        if (propName != null && ignoredClassPropNames != null && ignoredClassPropNames.contains(propName)) {
                            readMap(jr, defaultJSONDeserializationConfig, null, false, Map.class, null, null);
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
                        propType = valueType;
                    } else {
                        if (propName != null && ignoredClassPropNames != null && ignoredClassPropNames.contains(propName)) {
                            readCollection(jr, defaultJSONDeserializationConfig, null, config.getPropHandler(propName), false, List.class, null, null);
                        } else {
                            //noinspection DataFlowIssue
                            value = readBracketedValue(jr, config, key instanceof String ? config.getPropHandler((String) key) : null, propType);

                            if (!ignoreNullOrEmpty || (!isNullOrEmptyValue(keyType, key) && !isNullOrEmptyValue(propType, value))) {
                                result.put(key, value);
                            }
                        }
                    }

                    break;

                case END_BRACE, EOF:

                    if (isKey && key != null /* check for empty JSON text {} */
                            || (isKey && key == null && jr.hasText()) /*check for invalid JSON text: {abc}*/) {
                        throw new ParseException(token, getErrorMsg(jr, token));
                    } else if ((firstToken == START_BRACE) == (token != END_BRACE)) {
                        throw new ParseException(token, "The JSON text should be wrapped or unwrapped with \"[]\" or \"{}\"");
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

                    return (T) creatorAndConvertor._2.apply(result);

                default:
                    throw new ParseException(token, getErrorMsg(jr, token));
            }
        }
    }

    @SuppressWarnings({ "unchecked" })
    protected <T> T readArray(final JSONReader jr, final JSONDeserializationConfig config, final Type<?> propType, final boolean isFirstCall,
            final Class<? extends T> targetClass, final Type<? extends T> targetType, Object[] output) throws IOException {
        Type<?> eleType = defaultValueType;

        if (propType != null && (propType.isArray() || propType.isCollection()) && !propType.getElementType().isObjectType()) {
            eleType = propType.getElementType();
        } else if (targetType != null && (targetType.isArray() || targetType.isCollection()) && !targetType.getElementType().isObjectType()) {
            eleType = targetType.getElementType();
        } else if (propType == null || !propType.isObjectType()) {
            if (config.getElementType() != null && !config.getElementType().isObjectType()) {
                eleType = config.getElementType();
            } else {
                eleType = Type
                        .of(targetClass.isArray() && !Object.class.equals(targetClass.getComponentType()) ? targetClass.getComponentType() : Object.class);
            }
        }

        final boolean ignoreNullOrEmpty = config.ignoreNullOrEmpty();
        final boolean readNullToEmpty = config.readNullToEmpty();

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
                        case START_QUOTATION_D, START_QUOTATION_S:

                            break;

                        case END_QUOTATION_D, END_QUOTATION_S:

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
                                throw new ParseException(token, "The JSON text should be wrapped or unwrapped with \"[]\" or \"{}\"");
                            } else if (jr.hasText() || preToken == COMMA) {
                                value = readValue(jr, readNullToEmpty, eleType);

                                if (!ignoreNullOrEmpty || !isNullOrEmptyValue(eleType, value)) {
                                    c.add(value);
                                }
                            }

                            return collection2Array(c, targetType);

                        default:
                            throw new ParseException(token, getErrorMsg(jr, token));
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
                    case START_QUOTATION_D, START_QUOTATION_S:

                        break;

                    case END_QUOTATION_D, END_QUOTATION_S:

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
                            throw new ParseException(token, "The JSON text should be wrapped or unwrapped with \"[]\" or \"{}\"");
                        } else if (jr.hasText() || preToken == COMMA) {
                            value = readValue(jr, readNullToEmpty, eleType);

                            if (!ignoreNullOrEmpty || !isNullOrEmptyValue(eleType, value)) {
                                output[idx++] = value;
                            }
                        }

                        return (T) output;

                    default:
                        throw new ParseException(token, getErrorMsg(jr, token));
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    protected <T> T readCollection(final JSONReader jr, final JSONDeserializationConfig config, final Type<?> propType,
            final BiConsumer<? super Collection<Object>, ?> propHandler, final boolean isFirstCall, final Class<? extends T> targetClass,
            final Type<? extends T> targetType, final Collection<Object> output) throws IOException {
        Type<?> eleType = defaultValueType;

        if (propType != null && (propType.isCollection() || propType.isArray()) && !propType.getElementType().isObjectType()) {
            eleType = propType.getElementType();
        } else if (targetType != null && (targetType.isCollection() || targetType.isArray()) && !targetType.getElementType().isObjectType()) {
            eleType = targetType.getElementType();
        } else if ((propType == null || !propType.isObjectType()) && (config.getElementType() != null && !config.getElementType().isObjectType())) {
            eleType = config.getElementType();
        }

        final boolean ignoreNullOrEmpty = config.ignoreNullOrEmpty();
        final boolean readNullToEmpty = config.readNullToEmpty();
        @SuppressWarnings("rawtypes")
        final BiConsumer<Collection<Object>, Object> propHandlerToUse = propHandler == null ? DEFAULT_PROP_HANDLER : (BiConsumer) propHandler;

        final Tuple2<Function<Class<?>, Object>, Function<Object, Object>> creatorAndConvertor = getCreatorAndConvertorForTargetType(targetClass, null);

        final Collection<Object> result = output == null
                ? (Collection.class.isAssignableFrom(targetClass) ? (Collection<Object>) creatorAndConvertor._1.apply(targetClass) : new ArrayList<>())
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

            return (T) creatorAndConvertor._2.apply(result);
        }

        for (int preToken = firstToken, token = firstToken == START_BRACKET ? jr.nextToken() : firstToken;; preToken = token, token = jr.nextToken(eleType)) {
            switch (token) {
                case START_QUOTATION_D, START_QUOTATION_S:

                    break;

                case END_QUOTATION_D, END_QUOTATION_S:

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
                        throw new ParseException(token, "The JSON text should be wrapped or unwrapped with \"[]\" or \"{}\"");
                    } else if (jr.hasText() || preToken == COMMA) {
                        value = readValue(jr, readNullToEmpty, eleType);

                        if (!ignoreNullOrEmpty || !isNullOrEmptyValue(eleType, value)) {
                            // result.add(propValue);
                            propHandlerToUse.accept(result, value);
                        }
                    }

                    return (T) creatorAndConvertor._2.apply(result);

                default:
                    throw new ParseException(token, getErrorMsg(jr, token));
            }
        }
    }

    @SuppressWarnings("unused")
    protected <T> T readMapEntity(final JSONReader jr, final JSONDeserializationConfig config, final boolean isFirstCall, final Class<? extends T> targetClass,
            final Type<? extends T> targetType) throws IOException {
        final int firstToken = isFirstCall ? jr.nextToken() : START_BRACKET;

        if (firstToken == EOF) {
            if (Strings.isNotEmpty(jr.getText())) {
                throw new ParseException(firstToken, "Can't parse: " + jr.getText());
            }

            return null;
        }

        MapEntity mapEntity = null;

        for (int token = firstToken == START_BRACE ? jr.nextToken() : firstToken;; token = jr.nextToken()) {
            switch (token) {
                case START_QUOTATION_D, START_QUOTATION_S:

                    break;

                case END_QUOTATION_D, END_QUOTATION_S, COLON:

                    if (jr.hasText()) {
                        if (mapEntity == null) {
                            mapEntity = new MapEntity(jr.getText());
                        } else {
                            throw new ParseException(token, getErrorMsg(jr, token));
                        }
                    } else {
                        if (mapEntity == null) {
                            throw new ParseException(token, "Bean name can't be null or empty");
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
                        throw new ParseException(token, "The JSON text should be wrapped or unwrapped with \"[]\" or \"{}\"");
                    }

                    return (T) mapEntity;

                default:
                    throw new ParseException(token, getErrorMsg(jr, token));
            }
        }
    }

    @SuppressWarnings({ "unused" })
    protected <T> T readEntityId(final JSONReader jr, final JSONDeserializationConfig config, final boolean isFirstCall, final Class<? extends T> targetClass,
            final Type<? extends T> targetType) throws IOException {
        final int firstToken = isFirstCall ? jr.nextToken() : START_BRACKET;

        if (firstToken == EOF) {
            if (Strings.isNotEmpty(jr.getText())) {
                throw new ParseException(firstToken, "Can't parse: " + jr.getText());
            }

            return null;
        }

        Seid entityId = null;

        for (int token = firstToken == START_BRACE ? jr.nextToken() : firstToken;; token = jr.nextToken()) {
            switch (token) {
                case START_QUOTATION_D, START_QUOTATION_S:

                    break;

                case END_QUOTATION_D, END_QUOTATION_S, COLON:

                    if (jr.hasText()) {
                        if (entityId == null) {
                            entityId = Seid.of(jr.getText());
                        } else {
                            throw new ParseException(token, getErrorMsg(jr, token));
                        }
                    } else {
                        if (entityId == null) {
                            throw new ParseException(token, "Bean name can't be null or empty");
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
                        throw new ParseException(token, "The JSON text should be wrapped or unwrapped with \"[]\" or \"{}\"");
                    }

                    return (T) entityId;

                default:
                    throw new ParseException(token, getErrorMsg(jr, token));
            }
        }
    }

    @SuppressWarnings("unused")
    protected <T> T readDataset(final JSONReader jr, final int lastToken, final JSONDeserializationConfig config, final boolean isFirstCall,
            final Class<? extends T> targetClass, final Type<? extends T> targetType) throws IOException {

        final int firstToken = isFirstCall ? jr.nextToken() : lastToken;

        if (firstToken == EOF) {
            if (isFirstCall && Strings.isNotEmpty(jr.getText())) {
                throw new ParseException(firstToken, "Can't parse: " + jr.getText());
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
                final boolean readNullToEmpty = config.readNullToEmpty();

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
                        case START_QUOTATION_D, START_QUOTATION_S:

                            break;

                        case END_QUOTATION_D, END_QUOTATION_S:
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
                                throw new ParseException(token, getErrorMsg(jr, token));
                            }

                            break;

                        case COMMA:
                            if (isKey) {
                                throw new ParseException(token, getErrorMsg(jr, token));
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
                                throw new ParseException(token, getErrorMsg(jr, token));
                            } else {
                                if (key != null && ignoredClassPropNames != null && ignoredClassPropNames.contains(key)) {
                                    readMap(jr, defaultJSONDeserializationConfig, null, false, Map.class, null, null);
                                } else {
                                    value = readBracedValue(jr, config, valueType);

                                    addDatasetColumnValue(key, value, valueCount, result);
                                }
                            }

                            break;

                        case START_BRACKET:
                            if (isKey) {
                                throw new ParseException(token, getErrorMsg(jr, token));
                            } else {
                                if (key != null && ignoredClassPropNames != null && ignoredClassPropNames.contains(key)) {
                                    readCollection(jr, defaultJSONDeserializationConfig, null, config.getPropHandler(key), false, List.class, null, null);
                                } else {
                                    value = readBracketedValue(jr, config, config.getPropHandler(key), valueType);

                                    addDatasetColumnValue(key, value, valueCount, result);
                                }
                            }

                            break;

                        case END_BRACE:
                            if (isKey && key != null /* check for empty JSON text {} */) {
                                throw new ParseException(token, getErrorMsg(jr, token));
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
                            throw new ParseException(token, getErrorMsg(jr, token));
                    }

                    if (isBraceEnded) {
                        if (token == END_BRACKET || token == EOF /*should not happen*/) {
                            break;
                        } else if (token != START_BRACE) {
                            throw new ParseException(token, getErrorMsg(jr, token));
                        }

                        isBraceEnded = false;
                    }
                }
            } else if (token == END_BRACKET || token == EOF /*should not happen*/) {
                // end
            } else {
                throw new ParseException(token, getErrorMsg(jr, token));
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
                    case START_QUOTATION_D, START_QUOTATION_S:

                        break;

                    case END_QUOTATION_D, END_QUOTATION_S:

                        if (isKey) {
                            columnName = jr.getText();
                        } else {
                            final Integer order = datasetSheetPropOrder.get(columnName);

                            if (order == null) {
                                throw new ParseException(token, getErrorMsg(jr, token));
                            }

                            if (order == 6) {
                                isFrozen = jr.readValue(boolType);
                            } else {
                                throw new ParseException(token, getErrorMsg(jr, token));
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
                            throw new ParseException(token, getErrorMsg(jr, token));
                        }

                        break;

                    case COMMA:
                        if (isKey) {
                            throw new ParseException(token, getErrorMsg(jr, token));
                        } else {
                            isKey = true;

                            if (jr.hasText()) {
                                final Integer order = datasetSheetPropOrder.get(columnName);

                                if (order == null) {
                                    throw new ParseException(token, getErrorMsg(jr, token));
                                }

                                if (order == 6) {
                                    isFrozen = jr.readValue(boolType);
                                } else {
                                    throw new ParseException(token, getErrorMsg(jr, token));
                                }
                            }
                        }

                        break;

                    case START_BRACKET:
                        final Integer order = datasetSheetPropOrder.get(columnName);

                        if (order == null) {
                            throw new ParseException(token, getErrorMsg(jr, token));
                        }

                        switch (order) {
                            case 3:
                                columnNameList = readCollection(jr, jdcForStringElement, null, null, false, List.class, null, null);
                                break;

                            case 4:
                                columnTypeList = readCollection(jr, jdcForTypeElement, null, null, false, List.class, null, null);
                                break;

                            default:
                                throw new ParseException(token, getErrorMsg(jr, token));
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
                                    case START_QUOTATION_D, START_QUOTATION_S:

                                        break;

                                    case END_QUOTATION_D, END_QUOTATION_S:
                                        if (isKey) {
                                            columnName = jr.getText();
                                        } else {
                                            throw new ParseException(token, getErrorMsg(jr, token));
                                        }

                                        break;

                                    case COLON:
                                        if (isKey) {
                                            isKey = false;

                                            if (jr.hasText()) {
                                                columnName = jr.getText();
                                            }
                                        } else {
                                            throw new ParseException(token, getErrorMsg(jr, token));
                                        }

                                        break;

                                    case COMMA:
                                        if (isKey) {
                                            throw new ParseException(token, getErrorMsg(jr, token));
                                        } else {
                                            isKey = true;

                                            if (jr.hasText()) {
                                                throw new ParseException(token, getErrorMsg(jr, token));
                                            }
                                        }

                                        break;

                                    case START_BRACKET:
                                        final int index = N.indexOf(columnNameList, columnName);

                                        if (index == N.INDEX_NOT_FOUND) {
                                            throw new ParseException("Column: " + columnName + " is not found column list: " + columnNameList);
                                        }

                                        valueType = N.isEmpty(columnTypeList) ? null : columnTypeList.get(index);

                                        if (valueType == null) {
                                            valueType = defaultValueType;
                                        }

                                        final List<Object> column = readCollection(jr, JDC.create().setElementType(valueType), null,
                                                config.getPropHandler(columnName), false, List.class, null, null);

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
                                            throw new ParseException(token, getErrorMsg(jr, token));
                                        }

                                        columnName = null;
                                        isKey = true;
                                        readingColumns = false;
                                        break;

                                    default:
                                        throw new ParseException(token, getErrorMsg(jr, token));
                                }
                            } while (readingColumns);
                        } else {
                            throw new ParseException(token, getErrorMsg(jr, token) + ". Key: " + columnName + ",  Value: " + jr.getText());
                        }

                        break;

                    case END_BRACE, EOF:

                        if ((firstToken == START_BRACE) == (token != END_BRACE)) {
                            throw new ParseException(token, "The JSON text should be wrapped or unwrapped with \"[]\" or \"{}\"");
                        } else if ((isKey && columnName != null) || jr.hasText()) {
                            throw new ParseException(token, getErrorMsg(jr, token));
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
                        throw new ParseException(token, getErrorMsg(jr, token));
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
    protected <T> T readSheet(final JSONReader jr, final int lastToken, final JSONDeserializationConfig config, final boolean isFirstCall,
            final Class<? extends T> targetClass, final Type<? extends T> targetType) throws IOException {

        final int firstToken = isFirstCall ? jr.nextToken() : lastToken;

        if (firstToken == EOF) {
            if (isFirstCall && Strings.isNotEmpty(jr.getText())) {
                throw new ParseException(firstToken, "Can't parse: " + jr.getText());
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

        String columnName = null;
        Type<?> valueType = defaultValueType;
        boolean isKey = true;

        for (int token = firstToken == START_BRACE ? jr.nextToken() : firstToken;; token = jr.nextToken()) {
            switch (token) {
                case START_QUOTATION_D, START_QUOTATION_S:

                    break;

                case END_QUOTATION_D, END_QUOTATION_S:
                    if (isKey) {
                        columnName = jr.getText();
                    } else {
                        final Integer order = datasetSheetPropOrder.get(columnName);

                        if (order == null) {
                            throw new ParseException(token, getErrorMsg(jr, token));
                        }

                        switch (order) { //NOSONAR
                            case 10:
                                rowKeyType = jr.readValue(strType);
                                break;

                            case 11:
                                columnKeyType = jr.readValue(strType);
                                break;

                            default:
                                throw new ParseException(token, getErrorMsg(jr, token));
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
                        throw new ParseException(token, getErrorMsg(jr, token));
                    }

                    break;

                case COMMA:
                    if (isKey) {
                        throw new ParseException(token, getErrorMsg(jr, token));
                    } else {
                        isKey = true;

                        if (jr.hasText()) {
                            final Integer order = datasetSheetPropOrder.get(columnName);

                            if (order == null) {
                                throw new ParseException(token, getErrorMsg(jr, token));
                            }

                            switch (order) { //NOSONAR
                                case 10:
                                    rowKeyType = jr.readValue(strType);
                                    break;

                                case 11:
                                    columnKeyType = jr.readValue(strType);
                                    break;

                                default:
                                    throw new ParseException(token, getErrorMsg(jr, token));
                            }
                        }
                    }

                    break;

                case START_BRACKET:
                    final Integer order = datasetSheetPropOrder.get(columnName);

                    if (order == null) {
                        throw new ParseException(token, getErrorMsg(jr, token));
                    }

                    switch (order) {
                        case 4:
                            columnTypeList = readCollection(jr, jdcForTypeElement, null, null, false, List.class, null, null);
                            break;

                        case 8:
                            rowKeyList = readCollection(jr, JDC.create().setElementType(Strings.isEmpty(rowKeyType) ? strType : Type.of(rowKeyType)), null,
                                    null, false, List.class, null, null);
                            break;

                        case 9:
                            columnKeyList = readCollection(jr, JDC.create().setElementType(Strings.isEmpty(columnKeyType) ? strType : Type.of(columnKeyType)),
                                    null, null, false, List.class, null, null);
                            break;

                        default:
                            throw new ParseException(token, getErrorMsg(jr, token));
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
                                case START_QUOTATION_D, START_QUOTATION_S:

                                    break;

                                case END_QUOTATION_D, END_QUOTATION_S:
                                    if (isKey) {
                                        columnName = jr.getText();
                                    } else {
                                        throw new ParseException(token, getErrorMsg(jr, token));
                                    }

                                    break;

                                case COLON:
                                    if (isKey) {
                                        isKey = false;

                                        if (jr.hasText()) {
                                            columnName = jr.getText();
                                        }
                                    } else {
                                        throw new ParseException(token, getErrorMsg(jr, token));
                                    }

                                    break;

                                case COMMA:
                                    if (isKey) {
                                        throw new ParseException(token, getErrorMsg(jr, token));
                                    } else {
                                        isKey = true;

                                        if (jr.hasText()) {
                                            throw new ParseException(token, getErrorMsg(jr, token));
                                        }
                                    }

                                    break;

                                case START_BRACKET:
                                    final int index = N.indexOf(columnKeyList, columnName);

                                    if (index == N.INDEX_NOT_FOUND) {
                                        throw new ParseException("Column: " + columnName + " is not found column list: " + columnKeyList);
                                    }

                                    valueType = N.isEmpty(columnTypeList) ? null : columnTypeList.get(index);

                                    if (valueType == null) {
                                        valueType = defaultValueType;
                                    }

                                    final List<Object> column = readCollection(jr, JDC.create().setElementType(valueType), null,
                                            config.getPropHandler(columnName), false, List.class, null, null);

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
                                        throw new ParseException(token, getErrorMsg(jr, token));
                                    }

                                    columnName = null;
                                    isKey = true;
                                    readingColumns = false;
                                    break;

                                default:
                                    throw new ParseException(token, getErrorMsg(jr, token));
                            }
                        } while (readingColumns);
                    } else {
                        throw new ParseException(token, getErrorMsg(jr, token) + ". Key: " + columnName + ",  Value: " + jr.getText());
                    }

                    break;

                case END_BRACE, EOF:

                    if ((firstToken == START_BRACE) == (token != END_BRACE)) {
                        throw new ParseException(token, "The JSON text should be wrapped or unwrapped with \"[]\" or \"{}\"");
                    } else if ((isKey && columnName != null) || jr.hasText()) {
                        throw new ParseException(token, getErrorMsg(jr, token));
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
                    throw new ParseException(token, getErrorMsg(jr, token));
            }
        }
    }

    protected Object readBracketedValue(final JSONReader jr, JSONDeserializationConfig config, final BiConsumer<? super Collection<Object>, ?> propHandler,
            final Type<?> type) throws IOException {
        if (N.len(type.getParameterTypes()) == 1) {
            config = config.copy();
            config.setElementType(type.getParameterTypes()[0]);
        }

        if (type.isArray()) {
            return readArray(jr, config, type, false, type.clazz(), type, null);
        } else if (type.isCollection()) {
            return readCollection(jr, config, type, propHandler, false, type.clazz(), type, null);
        } else if (type.isDataset()) {
            return readDataset(jr, START_BRACKET, config, false, type.clazz(), type);
        } else {
            final List<?> list = readCollection(jr, config, type, propHandler, false, List.class, null, null);
            final BiFunction<List<?>, Type<?>, Object> converter = list2PairTripleConverterMap.get(type.clazz());

            return converter == null ? list : converter.apply(list, type);
        }
    }

    protected Object readBracedValue(final JSONReader jr, JSONDeserializationConfig config, final Type<?> type) throws IOException {
        if (N.len(type.getParameterTypes()) == 2) {
            config = config.copy();
            config.setMapKeyType(type.getParameterTypes()[0]);
            config.setMapValueType(type.getParameterTypes()[1]);
        }

        if (type.isBean()) {
            return readBean(jr, config, false, type.clazz(), type);
        } else if (type.isMap()) {
            return readMap(jr, config, type, false, type.clazz(), type, null);
        } else if (type.isDataset()) {
            return readDataset(jr, START_BRACE, config, false, type.clazz(), type);
        } else if (Sheet.class.isAssignableFrom(type.clazz())) {
            return readSheet(jr, START_BRACE, config, false, type.clazz(), type);
        } else if (type.isMapEntity()) {
            return readMapEntity(jr, config, false, type.clazz(), type);
        } else if (type.isEntityId()) {
            return readEntityId(jr, config, false, type.clazz(), type);
        } else {
            final Map<Object, Object> map = readMap(jr, config, type, false, Map.class, null, null);
            final Function<Map<Object, Object>, ?> converter = map2TargetTypeConverterMap.get(type.clazz());

            if (converter == null) {
                if (AbstractMap.SimpleImmutableEntry.class.isAssignableFrom(type.clazz())) {
                    return map2TargetTypeConverterMap.get(AbstractMap.SimpleImmutableEntry.class).apply(map);
                } else if (Map.Entry.class.isAssignableFrom(type.clazz())) {
                    return map2TargetTypeConverterMap.get(Map.Entry.class).apply(map);
                } else {
                    return map;
                }
            } else {
                return converter.apply(map);
            }
        }
    }

    protected Object readValue(final JSONReader jr, final boolean nullToEmpty, final Type<?> valueType) {
        return readNullToEmpty(valueType, jr.readValue(valueType), nullToEmpty);
    }

    protected Object readValue(final JSONReader jr, final boolean nullToEmpty, final PropInfo propInfo, final Type<?> valueType) {
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
     * JSONDeserializationConfig config = new JSONDeserializationConfig();
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
     * @throws ParseException if the JSON structure is invalid or not an array
     */
    @Override
    public <T> Stream<T> stream(final String source, final JSONDeserializationConfig config, final Type<? extends T> elementType) {
        checkStreamSupportedType(elementType);

        final JSONDeserializationConfig configToUse = check(config);

        if (Strings.isEmpty(source) || "[]".equals(source)) {
            return Stream.empty();
        }

        final char[] cbuf = Objectory.createCharArrayBuffer();
        Stream<T> result = null;

        try {
            final JSONReader jr = JSONStringReader.parse(source, cbuf);

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
     * JSONDeserializationConfig config = new JSONDeserializationConfig();
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
     * @throws ParseException if the JSON structure is invalid or not an array
     */
    @Override
    public <T> Stream<T> stream(final File source, final JSONDeserializationConfig config, final Type<? extends T> elementType) {
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
     * JSONDeserializationConfig config = new JSONDeserializationConfig();
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
     * @throws ParseException if the JSON structure is invalid or not an array
     */
    @Override
    public <T> Stream<T> stream(final InputStream source, final boolean closeInputStreamWhenStreamIsClosed, final JSONDeserializationConfig config,
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
     * JSONDeserializationConfig config = new JSONDeserializationConfig();
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
     * @throws ParseException if the JSON structure is invalid or not an array
     */
    @Override
    public <T> Stream<T> stream(final Reader source, final boolean closeReaderWhenStreamIsClosed, final JSONDeserializationConfig config,
            final Type<? extends T> elementType) throws IllegalArgumentException {
        N.checkArgNotNull(source, cs.source);
        checkStreamSupportedType(elementType);

        Stream<T> result = null;
        final char[] rbuf = Objectory.createCharArrayBuffer();
        final char[] cbuf = Objectory.createCharArrayBuffer();

        try {
            final JSONDeserializationConfig configToUse = check(config);

            final JSONReader jr = JSONStreamReader.parse(source, rbuf, cbuf);

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

    private <T> Stream<T> stream(final Object source, final JSONReader jr, final JSONDeserializationConfig configToUse, final Type<? extends T> elementType) {
        final Class<? extends T> elementClass = elementType.clazz();
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
        switch (elementType.getSerializationType()) { // NOSONAR
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

    private void writeNullToEmpty(final BufferedJSONWriter bw, final Type<?> type) throws IOException {
        if (type.isCollection() || type.isArray()) {
            bw.write("[]");
        } else if (type.isMap()) {
            bw.write("{}");
        } else if (type.isCharSequence()) {
            bw.write("");
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
            final Type<?>[] paramTypes = eleType.getParameterTypes();
            return Pair.of(N.convert(list.get(0), paramTypes[0]), N.convert(list.get(1), paramTypes[1]));
        });

        list2PairTripleConverterMap.put(Triple.class, (list, eleType) -> {
            final Type<?>[] paramTypes = eleType.getParameterTypes();
            return Triple.of(N.convert(list.get(0), paramTypes[0]), N.convert(list.get(1), paramTypes[1]), N.convert(list.get(2), paramTypes[2]));
        });

        list2PairTripleConverterMap.put(Tuple1.class, (list, eleType) -> {
            final Type<?>[] paramTypes = eleType.getParameterTypes();
            return Tuple.of(N.convert(list.get(0), paramTypes[0]));
        });

        list2PairTripleConverterMap.put(Tuple2.class, (list, eleType) -> {
            final Type<?>[] paramTypes = eleType.getParameterTypes();
            return Tuple.of(N.convert(list.get(0), paramTypes[0]), N.convert(list.get(1), paramTypes[1]));
        });

        list2PairTripleConverterMap.put(Tuple3.class, (list, eleType) -> {
            final Type<?>[] paramTypes = eleType.getParameterTypes();
            return Tuple.of(N.convert(list.get(0), paramTypes[0]), N.convert(list.get(1), paramTypes[1]), N.convert(list.get(2), paramTypes[2]));
        });

        list2PairTripleConverterMap.put(Tuple4.class, (list, eleType) -> {
            final Type<?>[] paramTypes = eleType.getParameterTypes();
            return Tuple.of(N.convert(list.get(0), paramTypes[0]), N.convert(list.get(1), paramTypes[1]), N.convert(list.get(2), paramTypes[2]),
                    N.convert(list.get(3), paramTypes[3]));
        });

        list2PairTripleConverterMap.put(Tuple5.class, (list, eleType) -> {
            final Type<?>[] paramTypes = eleType.getParameterTypes();
            return Tuple.of(N.convert(list.get(0), paramTypes[0]), N.convert(list.get(1), paramTypes[1]), N.convert(list.get(2), paramTypes[2]),
                    N.convert(list.get(3), paramTypes[3]), N.convert(list.get(4), paramTypes[4]));
        });

        list2PairTripleConverterMap.put(Tuple6.class, (list, eleType) -> {
            final Type<?>[] paramTypes = eleType.getParameterTypes();
            return Tuple.of(N.convert(list.get(0), paramTypes[0]), N.convert(list.get(1), paramTypes[1]), N.convert(list.get(2), paramTypes[2]),
                    N.convert(list.get(3), paramTypes[3]), N.convert(list.get(4), paramTypes[4]), N.convert(list.get(5), paramTypes[5]));
        });

        list2PairTripleConverterMap.put(Tuple7.class, (list, eleType) -> {
            final Type<?>[] paramTypes = eleType.getParameterTypes();
            return Tuple.of(N.convert(list.get(0), paramTypes[0]), N.convert(list.get(1), paramTypes[1]), N.convert(list.get(2), paramTypes[2]),
                    N.convert(list.get(3), paramTypes[3]), N.convert(list.get(4), paramTypes[4]), N.convert(list.get(5), paramTypes[5]),
                    N.convert(list.get(6), paramTypes[6]));
        });

        list2PairTripleConverterMap.put(Tuple8.class, (list, eleType) -> {
            final Type<?>[] paramTypes = eleType.getParameterTypes();
            return Tuple.of(N.convert(list.get(0), paramTypes[0]), N.convert(list.get(1), paramTypes[1]), N.convert(list.get(2), paramTypes[2]),
                    N.convert(list.get(3), paramTypes[3]), N.convert(list.get(4), paramTypes[4]), N.convert(list.get(5), paramTypes[5]),
                    N.convert(list.get(6), paramTypes[6]), N.convert(list.get(7), paramTypes[7]));
        });

        list2PairTripleConverterMap.put(Tuple9.class, (list, eleType) -> {
            final Type<?>[] paramTypes = eleType.getParameterTypes();
            return Tuple.of(N.convert(list.get(0), paramTypes[0]), N.convert(list.get(1), paramTypes[1]), N.convert(list.get(2), paramTypes[2]),
                    N.convert(list.get(3), paramTypes[3]), N.convert(list.get(4), paramTypes[4]), N.convert(list.get(5), paramTypes[5]),
                    N.convert(list.get(6), paramTypes[6]), N.convert(list.get(7), paramTypes[7]), N.convert(list.get(8), paramTypes[8]));
        });
    }

    private String getErrorMsg(final JSONReader jr, final int token) {
        switch (token) {
            case START_BRACE:
                return "Error on parsing at '{' with " + jr.getText();

            case END_BRACE:
                return "Error on parsing at '}' with " + jr.getText();

            case START_BRACKET:
                return "Error on parsing at '[' with " + jr.getText();

            case END_BRACKET:
                return "Error on parsing at ']' with " + jr.getText();

            case START_QUOTATION_D:
                return "Error on parsing at starting '\"' with " + jr.getText();

            case END_QUOTATION_D:
                return "Error on parsing at ending '\"' with " + jr.getText();

            case START_QUOTATION_S:
                return "Error on parsing at starting ''' with " + jr.getText();

            case END_QUOTATION_S:
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
