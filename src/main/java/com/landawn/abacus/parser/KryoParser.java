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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URL;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.RowId;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Deque;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.Stack;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;

import javax.xml.datatype.XMLGregorianCalendar;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.BiMap;
import com.landawn.abacus.util.BooleanList;
import com.landawn.abacus.util.ByteArrayOutputStream;
import com.landawn.abacus.util.ByteList;
import com.landawn.abacus.util.CharList;
import com.landawn.abacus.util.Dataset;
import com.landawn.abacus.util.DoubleList;
import com.landawn.abacus.util.Duration;
import com.landawn.abacus.util.FloatList;
import com.landawn.abacus.util.Fraction;
import com.landawn.abacus.util.HBaseColumn;
import com.landawn.abacus.util.Holder;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.IntList;
import com.landawn.abacus.util.ListMultimap;
import com.landawn.abacus.util.LongList;
import com.landawn.abacus.util.Multimap;
import com.landawn.abacus.util.Multiset;
import com.landawn.abacus.util.MutableBoolean;
import com.landawn.abacus.util.MutableByte;
import com.landawn.abacus.util.MutableChar;
import com.landawn.abacus.util.MutableDouble;
import com.landawn.abacus.util.MutableFloat;
import com.landawn.abacus.util.MutableInt;
import com.landawn.abacus.util.MutableLong;
import com.landawn.abacus.util.MutableShort;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.Range;
import com.landawn.abacus.util.RowDataset;
import com.landawn.abacus.util.SetMultimap;
import com.landawn.abacus.util.Sheet;
import com.landawn.abacus.util.ShortList;
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
import com.landawn.abacus.util.u.Nullable;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalBoolean;
import com.landawn.abacus.util.u.OptionalByte;
import com.landawn.abacus.util.u.OptionalChar;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.u.OptionalFloat;
import com.landawn.abacus.util.u.OptionalInt;
import com.landawn.abacus.util.u.OptionalLong;
import com.landawn.abacus.util.u.OptionalShort;

/**
 * High-performance binary serialization parser using the Kryo framework.
 * 
 * <p>This parser provides fast and efficient object graph serialization with support for:
 * <ul>
 *   <li>Automatic deep and shallow copying of objects</li>
 *   <li>Binary serialization with optional Base64 encoding</li>
 *   <li>Class registration for improved performance and smaller output</li>
 *   <li>Custom serializers for specific types</li>
 *   <li>Object pooling for better performance</li>
 * </ul>
 * 
 * <p><b>Encoding behavior:</b>
 * <ul>
 *   <li>String/Writer output: Content is Base64 encoded</li>
 *   <li>File/OutputStream output: Content is NOT Base64 encoded (raw binary)</li>
 *   <li>String/Reader input: Content must be Base64 encoded</li>
 *   <li>File/InputStream input: Content must NOT be Base64 encoded (raw binary)</li>
 * </ul>
 * 
 * <p>The parser automatically registers many common Java types for optimal performance.
 * Additional types can be registered using the {@link #register} methods or globally
 * via {@link ParserFactory#registerKryo}.
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * KryoParser parser = new KryoParser();
 * 
 * // Serialize to Base64 string
 * MyObject obj = new MyObject();
 * String serialized = parser.serialize(obj);
 * 
 * // Deserialize from Base64 string
 * MyObject restored = parser.deserialize(serialized, MyObject.class);
 * 
 * // Binary serialization to file (not Base64 encoded)
 * parser.serialize(obj, new File("data.kryo"));
 * 
 * // Register custom types for better performance
 * parser.register(MyCustomType.class);
 * parser.register(MyCustomType.class, 100); // with ID
 * 
 * // Deep copy
 * MyObject copy = parser.clone(obj);
 * 
 * // Shallow copy
 * MyObject shallowCopy = parser.copy(obj);
 * }</pre>
 * 
 * @see ParserFactory#createKryoParser()
 * @see KryoSerializationConfig
 * @see KryoDeserializationConfig
 */
public final class KryoParser extends AbstractParser<KryoSerializationConfig, KryoDeserializationConfig> {

    private static final int BUFFER_SIZE = 8192;

    private static final List<Output> outputPool = new ArrayList<>(POOL_SIZE);

    private static final List<Input> inputPool = new ArrayList<>(POOL_SIZE);

    private final Set<Class<?>> kryoClassSet = new HashSet<>();
    private final Map<Class<?>, Integer> kryoClassIdMap = new ConcurrentHashMap<>();
    private final Map<Class<?>, Serializer<?>> kryoClassSerializerMap = new ConcurrentHashMap<>();
    private final Map<Class<?>, Tuple2<Serializer<?>, Integer>> kryoClassSerializerIdMap = new ConcurrentHashMap<>();

    private final Map<Kryo, Kryo> xPool = new IdentityHashMap<>();

    private final List<Kryo> kryoPool = new ArrayList<>(POOL_SIZE);

    KryoParser() {
    }

    /**
     * Serializes an object to a Base64 encoded string representation.
     *
     * <p>This method converts the object to binary format using Kryo serialization,
     * then encodes the result as a Base64 string suitable for text-based transmission.
     * The output is Base64 encoded to make it suitable for text-based storage and transmission.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MyObject obj = new MyObject();
     * String encoded = parser.serialize(obj, null);
     * // encoded contains Base64 representation
     * }</pre>
     *
     * @param obj the object to serialize (may be {@code null} depending on implementation)
     * @param config the serialization configuration to use (may be {@code null} for default behavior)
     * @return the Base64 encoded string representation of the serialized object
     */
    @Override
    public String serialize(final Object obj, final KryoSerializationConfig config) {
        final ByteArrayOutputStream os = Objectory.createByteArrayOutputStream();

        try {
            write(obj, config, os);

            return Strings.base64Encode(os.toByteArray());
        } finally {
            Objectory.recycle(os);
        }
    }

    /**
     * Serializes an object to a file with raw binary content (NOT Base64 encoded).
     *
     * <p>The file will be created if it doesn't exist, or overwritten if it does.
     * Parent directories must exist or an exception will be thrown. The content
     * is written in raw binary format without Base64 encoding for optimal performance.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MyObject obj = new MyObject();
     * File file = new File("data.kryo");
     * parser.serialize(obj, null, file);
     * // File contains raw binary data
     * }</pre>
     *
     * @param obj the object to serialize (may be {@code null} depending on implementation)
     * @param config the serialization configuration to use (may be {@code null} for default behavior)
     * @param output the output file to write to (must not be {@code null})
     * @throws UncheckedIOException if an I/O error occurs during file writing
     */
    @Override
    public void serialize(final Object obj, final KryoSerializationConfig config, final File output) {
        OutputStream os = null;

        try {
            createNewFileIfNotExists(output);

            os = IOUtil.newFileOutputStream(output);

            serialize(obj, config, os);

            os.flush();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            IOUtil.close(os);
        }
    }

    /**
     * Serializes an object to an output stream with raw binary content (NOT Base64 encoded).
     *
     * <p>The stream is not closed after writing, allowing the caller to manage stream
     * lifecycle. The stream will be flushed after serialization. The content is written
     * in raw binary format without Base64 encoding for optimal performance.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (FileOutputStream fos = new FileOutputStream("data.kryo")) {
     *     parser.serialize(myObject, null, fos);
     * }
     * }</pre>
     *
     * @param obj the object to serialize (may be {@code null} depending on implementation)
     * @param config the serialization configuration to use (may be {@code null} for default behavior)
     * @param output the output stream to write to (must not be {@code null})
     * @throws UncheckedIOException if an I/O error occurs during stream writing
     */
    @Override
    public void serialize(final Object obj, final KryoSerializationConfig config, final OutputStream output) {
        write(obj, config, output);
    }

    /**
     * Serializes an object to a writer with Base64 encoded content.
     *
     * <p>The writer is not closed after writing, allowing the caller to manage writer
     * lifecycle. The writer will be flushed after serialization. The content is Base64
     * encoded to make it suitable for text-based storage and transmission.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * StringWriter sw = new StringWriter();
     * parser.serialize(myObject, null, sw);
     * String base64 = sw.toString();
     * }</pre>
     *
     * @param obj the object to serialize (may be {@code null} depending on implementation)
     * @param config the serialization configuration to use (may be {@code null} for default behavior)
     * @param output the writer to write to (must not be {@code null})
     * @throws UncheckedIOException if an I/O error occurs during writing
     */
    @Override
    public void serialize(final Object obj, final KryoSerializationConfig config, final Writer output) {
        final ByteArrayOutputStream os = Objectory.createByteArrayOutputStream();

        try {
            write(obj, config, os);

            output.write(Strings.base64Encode(os.toByteArray()));

            output.flush();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            Objectory.recycle(os);
        }
    }

    /**
     * Writes an object to an output stream using Kryo serialization.
     * This is the core serialization method that handles binary output.
     * The method creates and manages a Kryo Output instance from the pool.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteArrayOutputStream baos = new ByteArrayOutputStream();
     * parser.write(myObject, null, baos);
     * byte[] serialized = baos.toByteArray();
     * }</pre>
     *
     * @param obj the object to write (may be null)
     * @param config the serialization configuration (may be {@code null} for defaults)
     * @param output the output stream to write to
     */
    protected void write(final Object obj, final KryoSerializationConfig config, final OutputStream output) {
        final Output kryoOutput = createOutput();

        try {
            kryoOutput.setOutputStream(output);

            write(obj, config, kryoOutput);
        } finally {
            recycle(kryoOutput);
        }
    }

    /**
     * Writes an object using Kryo output.
     * This method performs the actual Kryo serialization, either writing just the object
     * or both the class information and the object, depending on the configuration.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Output out = new Output(new ByteArrayOutputStream());
     * KryoSerializationConfig config = KryoSerializationConfig.create().writeClass(true);
     * parser.write(myObject, config, out);
     * }</pre>
     *
     * @param obj the object to write (may be null)
     * @param config the serialization configuration (may be {@code null} for defaults). If config.writeClass()
     *               returns {@code true}, both class and object are written; otherwise only the object is written
     * @param output the Kryo output to write to
     */
    protected void write(final Object obj, final KryoSerializationConfig config, final Output output) {
        check(config);

        final Kryo kryo = createKryo();

        try {
            if (config != null && config.writeClass()) {
                kryo.writeClassAndObject(output, obj);
            } else {
                kryo.writeObject(output, obj);
            }

            output.flush();
        } finally {
            recycle(kryo);
        }
    }

    /**
     * Deserializes an object from a Base64 encoded string representation.
     *
     * <p>This method decodes the Base64 string and uses Kryo deserialization to convert
     * the binary data back to an object. The input string must be Base64 encoded Kryo binary data.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String base64Data = "rO0ABXNyABF..."; // Base64 encoded
     * MyObject obj = parser.deserialize(base64Data, null, MyObject.class);
     * }</pre>
     *
     * @param <T> the target type
     * @param source the Base64 encoded string to deserialize from (must not be {@code null})
     * @param config the deserialization configuration to use (may be {@code null} for default behavior)
     * @param targetClass the class of the object to create (must not be {@code null})
     * @return the deserialized object instance
     */
    @Override
    public <T> T deserialize(final String source, final KryoDeserializationConfig config, final Class<? extends T> targetClass) {
        N.checkArgNotNull(source, cs.source);

        final Input input = createInput();

        try {
            input.setBuffer(Strings.base64Decode(source));

            return read(input, config, targetClass);
        } finally {
            recycle(input);
        }
    }

    /**
     * Deserializes an object from a file containing raw binary data (NOT Base64 encoded).
     *
     * <p>This method reads binary Kryo data from the specified file and deserializes it
     * to an object instance. The file should contain raw binary Kryo data (not Base64 encoded).</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File file = new File("data.kryo");
     * MyObject obj = parser.deserialize(file, null, MyObject.class);
     * }</pre>
     *
     * @param <T> the target type
     * @param source the source file to read from (must not be {@code null} and must exist)
     * @param config the deserialization configuration to use (may be {@code null} for default behavior)
     * @param targetClass the class of the object to create (must not be {@code null})
     * @return the deserialized object instance
     * @throws UncheckedIOException if an I/O error occurs or the file doesn't exist
     */
    @Override
    public <T> T deserialize(final File source, final KryoDeserializationConfig config, final Class<? extends T> targetClass) {
        InputStream is = null;

        try {
            is = IOUtil.newFileInputStream(source);

            return deserialize(is, config, targetClass);
        } finally {
            IOUtil.close(is);
        }
    }

    /**
     * Deserializes an object from an input stream containing raw binary data (NOT Base64 encoded).
     *
     * <p>The stream is not closed after reading, allowing the caller to manage stream lifecycle.
     * The stream should contain raw binary Kryo data (not Base64 encoded).</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (FileInputStream fis = new FileInputStream("data.kryo")) {
     *     MyObject obj = parser.deserialize(fis, null, MyObject.class);
     * }
     * }</pre>
     *
     * @param <T> the target type
     * @param source the input stream to read from (must not be {@code null})
     * @param config the deserialization configuration to use (may be {@code null} for default behavior)
     * @param targetClass the class of the object to create (must not be {@code null})
     * @return the deserialized object instance
     * @throws UncheckedIOException if an I/O error occurs during stream reading
     */
    @Override
    public <T> T deserialize(final InputStream source, final KryoDeserializationConfig config, final Class<? extends T> targetClass) {
        return read(source, config, targetClass);
    }

    /**
     * Deserializes an object from a reader containing Base64 encoded content.
     *
     * <p>The reader is not closed after reading, allowing the caller to manage reader lifecycle.
     * The reader should contain Base64 encoded Kryo binary data.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * StringReader sr = new StringReader(base64String);
     * MyObject obj = parser.deserialize(sr, null, MyObject.class);
     * }</pre>
     *
     * @param <T> the target type
     * @param source the reader to read from (must not be {@code null})
     * @param config the deserialization configuration to use (may be {@code null} for default behavior)
     * @param targetClass the class of the object to create (must not be {@code null})
     * @return the deserialized object instance
     * @throws UncheckedIOException if an I/O error occurs during reading
     */
    @Override
    public <T> T deserialize(final Reader source, final KryoDeserializationConfig config, final Class<? extends T> targetClass) {
        return deserialize(IOUtil.readAllToString(source), config, targetClass);
    }

    /**
     * Reads an object from an input stream using Kryo deserialization.
     * This is the core deserialization method that handles binary input.
     * The method creates and manages a Kryo Input instance from the pool.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteArrayInputStream bais = new ByteArrayInputStream(serializedData);
     * MyObject obj = parser.read(bais, null, MyObject.class);
     * }</pre>
     *
     * @param <T> the type of the target object
     * @param source the input stream to read from
     * @param config the deserialization configuration (may be {@code null} for defaults)
     * @param targetClass the target class to deserialize to (may be {@code null} if class info is in stream)
     * @return the deserialized object
     */
    protected <T> T read(final InputStream source, final KryoDeserializationConfig config, final Class<? extends T> targetClass) {
        final Input input = createInput();

        try {
            input.setInputStream(source);

            return read(input, config, targetClass);
        } finally {
            recycle(input);
        }
    }

    /**
     * Reads an object using Kryo input.
     * This method performs the actual Kryo deserialization. If targetClass is {@code null},
     * it reads both class and object information from the stream; otherwise it reads
     * just the object data and instantiates the specified class.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Input in = new Input(new ByteArrayInputStream(data));
     * MyObject obj = parser.read(in, null, MyObject.class);
     * }</pre>
     *
     * @param <T> the type of the target object
     * @param source the Kryo input to read from
     * @param config the deserialization configuration (may be {@code null} for defaults)
     * @param targetClass the target class to deserialize to (may be {@code null} to read class from stream)
     * @return the deserialized object
     */
    @SuppressWarnings("unchecked")
    protected <T> T read(final Input source, final KryoDeserializationConfig config, final Class<? extends T> targetClass) {
        check(config);

        final Kryo kryo = createKryo();

        try {
            return (T) ((targetClass == null) ? kryo.readClassAndObject(source) : kryo.readObject(source, targetClass));
        } finally {
            recycle(kryo);
        }
    }

    /**
     * Validates the serialization configuration.
     * This method can be used to enforce constraints on the serialization configuration.
     * Currently, this implementation accepts any configuration including {@code null}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * KryoSerializationConfig config = KryoSerializationConfig.create();
     * config = parser.check(config); // Validates and returns config
     * }</pre>
     *
     * @param config the configuration to check (may be null)
     * @return the validated configuration (same as input)
     */
    @SuppressWarnings("UnusedReturnValue")
    protected KryoSerializationConfig check(final KryoSerializationConfig config) {
        //        if (config != null) {
        //            throw new ParseException("No serialization configuration is supported");
        //        }
        //
        //        return null;

        return config;
    }

    /**
     * Validates the deserialization configuration.
     * This method can be used to enforce constraints on the deserialization configuration.
     * Currently, this implementation accepts any configuration including {@code null}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * KryoDeserializationConfig config = KryoDeserializationConfig.create();
     * config = parser.check(config); // Validates and returns config
     * }</pre>
     *
     * @param config the configuration to check (may be null)
     * @return the validated configuration (same as input)
     */
    @SuppressWarnings("UnusedReturnValue")
    protected KryoDeserializationConfig check(final KryoDeserializationConfig config) {
        //        if (config != null) {
        //            throw new ParseException("No deserialization configuration is supported");
        //        }
        //
        //        return null;

        return config;
    }

    /**
     * Creates a shallow copy of the source object.
     * Only the object itself is copied, not its referenced objects.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MyObject original = new MyObject();
     * original.setName("Test");
     * original.setList(Arrays.asList("a", "b"));
     * 
     * MyObject copy = parser.copy(original);
     * // copy.getName() equals "Test"
     * // copy.getList() == original.getList() (same reference)
     * }</pre>
     *
     * @param <T> the type of the object
     * @param source the object to copy
     * @return a shallow copy of the source object
     */
    public <T> T copy(final T source) {
        final Kryo kryo = createKryo();

        try {
            return kryo.copyShallow(source);
        } finally {
            recycle(kryo);
        }
    }

    /**
     * Creates a deep copy of the source object.
     * The object and all its referenced objects are copied recursively.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MyObject original = new MyObject();
     * original.setName("Test");
     * original.setList(Arrays.asList("a", "b"));
     * 
     * MyObject clone = parser.clone(original);
     * // clone.getName() equals "Test"
     * // clone.getList() != original.getList() (different reference)
     * // clone.getList().equals(original.getList()) (same content)
     * }</pre>
     *
     * @param <T> the type of the object
     * @param source the object to clone
     * @return a deep copy of the source object
     */
    public <T> T clone(final T source) {
        final Kryo kryo = createKryo();

        try {
            return kryo.copy(source);
        } finally {
            recycle(kryo);
        }
    }

    /**
     * Encodes an object to a byte array.
     * The byte array includes class information and can be decoded without specifying the target class.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MyObject obj = new MyObject();
     * byte[] encoded = parser.encode(obj);
     * // Store or transmit the byte array
     * }</pre>
     *
     * @param source the object to encode
     * @return the encoded byte array
     */
    public byte[] encode(final Object source) {
        final ByteArrayOutputStream os = Objectory.createByteArrayOutputStream();
        final Output output = createOutput();
        final Kryo kryo = createKryo();

        try {
            output.setOutputStream(os);
            kryo.writeClassAndObject(output, source);

            output.flush();

            return os.toByteArray();
        } finally {
            Objectory.recycle(os);
            recycle(output);
            recycle(kryo);
        }
    }

    /**
     * Decodes an object from a byte array.
     * The byte array must have been created with {@link #encode(Object)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] encoded = parser.encode(myObject);
     * MyObject decoded = parser.decode(encoded);
     * }</pre>
     *
     * @param <T> the type of the decoded object
     * @param source the byte array to decode
     * @return the decoded object
     */
    @SuppressWarnings("unchecked")
    public <T> T decode(final byte[] source) {
        final Input input = createInput();
        final Kryo kryo = createKryo();

        try {
            input.setBuffer(source);

            return (T) kryo.readClassAndObject(input);
        } finally {
            recycle(input);
            recycle(kryo);
        }
    }

    /**
     * Registers a class with this parser instance for improved performance.
     * Registration allows Kryo to serialize the class more efficiently.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * parser.register(MyDomainObject.class);
     * parser.register(MyValueObject.class);
     * // Now these classes will serialize more efficiently
     * }</pre>
     *
     * @param type the class to register
     * @throws IllegalArgumentException if type is null
     */
    public void register(final Class<?> type) throws IllegalArgumentException {
        N.checkArgNotNull(type, cs.type);

        synchronized (kryoPool) {
            kryoClassSet.add(type);

            xPool.clear();
            kryoPool.clear();
        }
    }

    /**
     * Registers a class with a specific ID for this parser instance.
     * Using fixed IDs ensures compatibility across different JVM instances.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * parser.register(User.class, 100);
     * parser.register(Order.class, 101);
     * parser.register(Product.class, 102);
     * }</pre>
     *
     * @param type the class to register
     * @param id the unique ID for this class
     * @throws IllegalArgumentException if type is null
     */
    public void register(final Class<?> type, final int id) throws IllegalArgumentException {
        N.checkArgNotNull(type, cs.type);

        synchronized (kryoPool) {
            kryoClassIdMap.put(type, id);

            xPool.clear();
            kryoPool.clear();
        }
    }

    /**
     * Registers a class with a custom serializer for this parser instance.
     * Custom serializers can handle special serialization requirements.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * parser.register(DateTime.class, new DateTimeSerializer());
     * parser.register(Money.class, new MoneySerializer());
     * }</pre>
     *
     * @param type the class to register
     * @param serializer the custom serializer for this class
     * @throws IllegalArgumentException if type or serializer is null
     */
    public void register(final Class<?> type, final Serializer<?> serializer) throws IllegalArgumentException {
        N.checkArgNotNull(type, cs.type);
        N.checkArgNotNull(serializer, cs.serializer);

        synchronized (kryoPool) {
            kryoClassSerializerMap.put(type, serializer);

            xPool.clear();
            kryoPool.clear();
        }
    }

    /**
     * Registers a class with a custom serializer and specific ID for this parser instance.
     * Combines the benefits of custom serialization and fixed IDs.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * parser.register(BigDecimal.class, new BigDecimalSerializer(), 200);
     * parser.register(UUID.class, new UUIDSerializer(), 201);
     * }</pre>
     *
     * @param type the class to register
     * @param serializer the custom serializer for this class
     * @param id the unique ID for this class
     * @throws IllegalArgumentException if type or serializer is null
     */
    public void register(final Class<?> type, final Serializer<?> serializer, final int id) throws IllegalArgumentException {
        N.checkArgNotNull(type, cs.type);
        N.checkArgNotNull(serializer, cs.serializer);

        synchronized (kryoPool) {
            kryoClassSerializerIdMap.put(type, Tuple.of(serializer, id));

            xPool.clear();
            kryoPool.clear();
        }
    }

    private static final Set<Class<?>> builtInClassesToRegister = new LinkedHashSet<>();

    static {
        builtInClassesToRegister.add(boolean.class);
        builtInClassesToRegister.add(char.class);
        builtInClassesToRegister.add(byte.class);
        builtInClassesToRegister.add(short.class);
        builtInClassesToRegister.add(int.class);
        builtInClassesToRegister.add(long.class);
        builtInClassesToRegister.add(float.class);
        builtInClassesToRegister.add(double.class);

        builtInClassesToRegister.add(Boolean.class);
        builtInClassesToRegister.add(Character.class);
        builtInClassesToRegister.add(Byte.class);
        builtInClassesToRegister.add(Short.class);
        builtInClassesToRegister.add(Integer.class);
        builtInClassesToRegister.add(Long.class);
        builtInClassesToRegister.add(Float.class);
        builtInClassesToRegister.add(Double.class);

        builtInClassesToRegister.add(String.class);

        builtInClassesToRegister.add(Enum.class);
        builtInClassesToRegister.add(Class.class);
        builtInClassesToRegister.add(Object.class);

        builtInClassesToRegister.add(BigInteger.class);
        builtInClassesToRegister.add(BigDecimal.class);

        builtInClassesToRegister.add(java.util.Date.class);
        builtInClassesToRegister.add(Calendar.class);
        builtInClassesToRegister.add(GregorianCalendar.class);
        builtInClassesToRegister.add(XMLGregorianCalendar.class);

        builtInClassesToRegister.add(Collection.class);
        builtInClassesToRegister.add(List.class);
        builtInClassesToRegister.add(ArrayList.class);
        builtInClassesToRegister.add(LinkedList.class);
        builtInClassesToRegister.add(Stack.class);
        builtInClassesToRegister.add(Vector.class);
        builtInClassesToRegister.add(Set.class);
        builtInClassesToRegister.add(HashSet.class);
        builtInClassesToRegister.add(LinkedHashSet.class);
        builtInClassesToRegister.add(SortedSet.class);
        builtInClassesToRegister.add(NavigableSet.class);
        builtInClassesToRegister.add(TreeSet.class);
        builtInClassesToRegister.add(Queue.class);
        builtInClassesToRegister.add(Deque.class);
        builtInClassesToRegister.add(BlockingDeque.class);
        builtInClassesToRegister.add(ArrayDeque.class);
        builtInClassesToRegister.add(ArrayBlockingQueue.class);
        builtInClassesToRegister.add(LinkedBlockingQueue.class);
        builtInClassesToRegister.add(ConcurrentLinkedQueue.class);
        builtInClassesToRegister.add(LinkedBlockingDeque.class);
        builtInClassesToRegister.add(ConcurrentLinkedDeque.class);
        builtInClassesToRegister.add(PriorityQueue.class);
        builtInClassesToRegister.add(DelayQueue.class);
        builtInClassesToRegister.add(Map.class);
        builtInClassesToRegister.add(HashMap.class);
        builtInClassesToRegister.add(LinkedHashMap.class);
        builtInClassesToRegister.add(IdentityHashMap.class);
        builtInClassesToRegister.add(ConcurrentMap.class);
        builtInClassesToRegister.add(ConcurrentHashMap.class);
        builtInClassesToRegister.add(SortedMap.class);
        builtInClassesToRegister.add(NavigableMap.class);
        builtInClassesToRegister.add(TreeMap.class);
        builtInClassesToRegister.add(Iterator.class);

        builtInClassesToRegister.add(File.class);
        builtInClassesToRegister.add(InputStream.class);
        builtInClassesToRegister.add(ByteArrayInputStream.class);
        builtInClassesToRegister.add(FileInputStream.class);
        builtInClassesToRegister.add(OutputStream.class);
        builtInClassesToRegister.add(ByteArrayOutputStream.class);
        builtInClassesToRegister.add(FileOutputStream.class);
        builtInClassesToRegister.add(Reader.class);
        builtInClassesToRegister.add(StringReader.class);
        builtInClassesToRegister.add(FileReader.class);
        builtInClassesToRegister.add(InputStreamReader.class);
        builtInClassesToRegister.add(Writer.class);
        builtInClassesToRegister.add(StringWriter.class);
        builtInClassesToRegister.add(FileWriter.class);
        builtInClassesToRegister.add(OutputStreamWriter.class);

        builtInClassesToRegister.add(Date.class);
        builtInClassesToRegister.add(Time.class);
        builtInClassesToRegister.add(Timestamp.class);

        builtInClassesToRegister.add(Blob.class);
        builtInClassesToRegister.add(Clob.class);
        builtInClassesToRegister.add(NClob.class);
        builtInClassesToRegister.add(SQLXML.class);
        builtInClassesToRegister.add(RowId.class);

        builtInClassesToRegister.add(URL.class);

        builtInClassesToRegister.add(BooleanList.class);
        builtInClassesToRegister.add(CharList.class);
        builtInClassesToRegister.add(ByteList.class);
        builtInClassesToRegister.add(ShortList.class);
        builtInClassesToRegister.add(IntList.class);
        builtInClassesToRegister.add(LongList.class);
        builtInClassesToRegister.add(FloatList.class);
        builtInClassesToRegister.add(DoubleList.class);

        builtInClassesToRegister.add(MutableBoolean.class);
        builtInClassesToRegister.add(MutableChar.class);
        builtInClassesToRegister.add(MutableByte.class);
        builtInClassesToRegister.add(MutableShort.class);
        builtInClassesToRegister.add(MutableInt.class);
        builtInClassesToRegister.add(MutableLong.class);
        builtInClassesToRegister.add(MutableFloat.class);
        builtInClassesToRegister.add(MutableDouble.class);

        builtInClassesToRegister.add(OptionalBoolean.class);
        builtInClassesToRegister.add(OptionalChar.class);
        builtInClassesToRegister.add(OptionalByte.class);
        builtInClassesToRegister.add(OptionalShort.class);
        builtInClassesToRegister.add(OptionalInt.class);
        builtInClassesToRegister.add(OptionalLong.class);
        builtInClassesToRegister.add(OptionalFloat.class);
        builtInClassesToRegister.add(OptionalDouble.class);
        builtInClassesToRegister.add(Optional.class);
        builtInClassesToRegister.add(Nullable.class);
        builtInClassesToRegister.add(Holder.class);

        builtInClassesToRegister.add(Fraction.class);
        builtInClassesToRegister.add(Range.class);
        builtInClassesToRegister.add(Duration.class);
        builtInClassesToRegister.add(Pair.class);
        builtInClassesToRegister.add(Triple.class);
        builtInClassesToRegister.add(Tuple.class);
        builtInClassesToRegister.add(Tuple1.class);
        builtInClassesToRegister.add(Tuple2.class);
        builtInClassesToRegister.add(Tuple3.class);
        builtInClassesToRegister.add(Tuple4.class);
        builtInClassesToRegister.add(Tuple5.class);
        builtInClassesToRegister.add(Tuple6.class);
        builtInClassesToRegister.add(Tuple7.class);
        builtInClassesToRegister.add(Tuple8.class);
        builtInClassesToRegister.add(Tuple9.class);

        builtInClassesToRegister.add(BiMap.class);
        builtInClassesToRegister.add(Multimap.class);
        builtInClassesToRegister.add(ListMultimap.class);
        builtInClassesToRegister.add(SetMultimap.class);
        builtInClassesToRegister.add(Multiset.class);
        builtInClassesToRegister.add(HBaseColumn.class);

        builtInClassesToRegister.add(Type.class);
        builtInClassesToRegister.add(Dataset.class);
        builtInClassesToRegister.add(RowDataset.class);
        builtInClassesToRegister.add(Sheet.class);

        builtInClassesToRegister.add(Map.Entry.class);

        builtInClassesToRegister.add(java.time.Duration.class);
        builtInClassesToRegister.add(Instant.class);
        builtInClassesToRegister.add(LocalDate.class);
        builtInClassesToRegister.add(LocalDateTime.class);
        builtInClassesToRegister.add(LocalTime.class);
        builtInClassesToRegister.add(OffsetDateTime.class);
        builtInClassesToRegister.add(OffsetTime.class);
        builtInClassesToRegister.add(ZonedDateTime.class);
        builtInClassesToRegister.add(Year.class);
        builtInClassesToRegister.add(YearMonth.class);

        final List<Class<?>> classes = new ArrayList<>(builtInClassesToRegister);
        for (final Class<?> cls : classes) {
            Class<?> arrayClass = cls;

            for (int i = 0; i < 3; i++) {
                arrayClass = java.lang.reflect.Array.newInstance(arrayClass, 0).getClass();

                builtInClassesToRegister.add(arrayClass);
            }
        }
    }

    /**
     * Creates and configures a new Kryo instance with all registered types.
     * This method retrieves a Kryo instance from the pool if available, or creates a new one
     * configured with all built-in types, globally registered types, and instance-specific types.
     * The Kryo instance is configured with registration not required, allowing serialization
     * of unregistered classes (though registered classes perform better).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Kryo kryo = parser.createKryo();
     * try {
     *     // Use kryo for serialization
     * } finally {
     *     parser.recycle(kryo);
     * }
     * }</pre>
     *
     * @return a configured Kryo instance ready for use
     *
     * <p>The Kryo instance includes registrations from (in order):
     * <ul>
     *   <li>Built-in types (primitives, collections, common Java types, etc.)</li>
     *   <li>Globally registered types via {@link ParserFactory}</li>
     *   <li>Instance-specific registered types via {@link #register} methods</li>
     * </ul>
     */
    protected Kryo createKryo() {
        synchronized (kryoPool) {
            if (kryoPool.size() > 0) {
                return kryoPool.remove(kryoPool.size() - 1);
            }
            final Kryo kryo = new Kryo();

            kryo.setRegistrationRequired(false);

            for (final Class<?> cls : builtInClassesToRegister) {
                kryo.register(cls);
            }

            if (N.notEmpty(ParserFactory._kryoClassSet)) {
                for (final Class<?> cls : ParserFactory._kryoClassSet) {
                    kryo.register(cls);
                }
            }

            if (N.notEmpty(ParserFactory._kryoClassIdMap)) {
                for (Map.Entry<Class<?>, Integer> entry : ParserFactory._kryoClassIdMap.entrySet()) {
                    kryo.register(entry.getKey(), entry.getValue());
                }
            }

            if (N.notEmpty(ParserFactory._kryoClassSerializerMap)) {
                for (Map.Entry<Class<?>, Serializer<?>> entry : ParserFactory._kryoClassSerializerMap.entrySet()) {
                    kryo.register(entry.getKey(), entry.getValue());
                }
            }

            if (N.notEmpty(ParserFactory._kryoClassSerializerIdMap)) {
                for (final Map.Entry<Class<?>, Tuple2<Serializer<?>, Integer>> entry : ParserFactory._kryoClassSerializerIdMap.entrySet()) {
                    kryo.register(entry.getKey(), entry.getValue()._1, entry.getValue()._2);
                }
            }

            if (N.notEmpty(kryoClassSet)) {
                for (final Class<?> cls : kryoClassSet) {
                    kryo.register(cls);
                }
            }

            if (N.notEmpty(kryoClassIdMap)) {
                for (Map.Entry<Class<?>, Integer> entry : kryoClassIdMap.entrySet()) { //NOSONAR
                    kryo.register(entry.getKey(), entry.getValue());
                }
            }

            if (N.notEmpty(kryoClassSerializerMap)) {
                for (Map.Entry<Class<?>, Serializer<?>> entry : kryoClassSerializerMap.entrySet()) { //NOSONAR
                    kryo.register(entry.getKey(), entry.getValue());
                }
            }

            if (N.notEmpty(kryoClassSerializerIdMap)) {
                for (final Map.Entry<Class<?>, Tuple2<Serializer<?>, Integer>> entry : kryoClassSerializerIdMap.entrySet()) {
                    kryo.register(entry.getKey(), entry.getValue()._1, entry.getValue()._2);
                }
            }

            xPool.put(kryo, kryo);

            return kryo;
        }
    }

    /**
     * Recycles a Kryo instance back to the pool for reuse.
     * This method returns the Kryo instance to the object pool if it was originally
     * created by this parser instance and if the pool is not at maximum capacity.
     * Recycling Kryo instances improves performance by avoiding repeated initialization.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Kryo kryo = parser.createKryo();
     * try {
     *     // Use kryo for serialization
     * } finally {
     *     parser.recycle(kryo);
     * }
     * }</pre>
     *
     * @param kryo the Kryo instance to recycle (may be null)
     *
     * <p>The Kryo instance will not be recycled if:
     * <ul>
     *   <li>The kryo instance is null</li>
     *   <li>The kryo instance was not created by this parser (not in xPool)</li>
     *   <li>The pool is already at maximum capacity</li>
     * </ul>
     */
    void recycle(final Kryo kryo) {
        if (kryo == null) {
            return;
        }

        synchronized (kryoPool) {
            if (kryoPool.size() < POOL_SIZE && xPool.containsKey(kryo)) {
                kryoPool.add(kryo);
            }
        }
    }

    /**
     * Creates a new Output instance from the pool or creates a new one if the pool is empty.
     * This method uses object pooling to reduce allocation overhead and improve performance.
     * The returned Output instance is configured with the default buffer size.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Output out = KryoParser.createOutput();
     * out.setOutputStream(myOutputStream);
     * // ... use output ...
     * KryoParser.recycle(out);
     * }</pre>
     *
     * @return a Kryo Output instance ready for use
     */
    static Output createOutput() {
        synchronized (outputPool) {
            if (outputPool.size() > 0) {
                return outputPool.remove(outputPool.size() - 1);
            }
            return new Output(BUFFER_SIZE);
        }
    }

    /**
     * Recycles an Output instance back to the pool for reuse.
     * This method returns the Output to the object pool if it meets size constraints,
     * allowing it to be reused by future operations. The output's stream is set to null
     * before pooling to prevent resource leaks.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Output out = KryoParser.createOutput();
     * try {
     *     // ... use output ...
     * } finally {
     *     KryoParser.recycle(out);
     * }
     * }</pre>
     *
     * @param output the Output instance to recycle (may be null)
     *
     * <p>The output will not be recycled if:
     * <ul>
     *   <li>The output is null</li>
     *   <li>The output's buffer exceeds the default buffer size</li>
     *   <li>The pool is already at maximum capacity</li>
     * </ul>
     */
    static void recycle(final Output output) {
        if ((output == null) || ((output.getBuffer() != null) && (output.getBuffer().length > BUFFER_SIZE))) {
            return;
        }

        synchronized (outputPool) {
            if (outputPool.size() < POOL_SIZE) {
                output.setOutputStream(null);
                outputPool.add(output);
            }
        }
    }

    /**
     * Creates a new Input instance from the pool or creates a new one if the pool is empty.
     * This method uses object pooling to reduce allocation overhead and improve performance.
     * The returned Input instance is configured with the default buffer size.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Input in = KryoParser.createInput();
     * in.setInputStream(myInputStream);
     * // ... use input ...
     * KryoParser.recycle(in);
     * }</pre>
     *
     * @return a Kryo Input instance ready for use
     */
    static Input createInput() {
        synchronized (inputPool) {
            if (inputPool.size() > 0) {
                return inputPool.remove(inputPool.size() - 1);
            }
            return new Input(BUFFER_SIZE);
        }
    }

    /**
     * Recycles an Input instance back to the pool for reuse.
     * This method returns the Input to the object pool if it meets size constraints,
     * allowing it to be reused by future operations. The input's stream is set to null
     * before pooling to prevent resource leaks.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Input in = KryoParser.createInput();
     * try {
     *     // ... use input ...
     * } finally {
     *     KryoParser.recycle(in);
     * }
     * }</pre>
     *
     * @param input the Input instance to recycle (may be null)
     *
     * <p>The input will not be recycled if:
     * <ul>
     *   <li>The input is null</li>
     *   <li>The input's buffer exceeds the default buffer size</li>
     *   <li>The pool is already at maximum capacity</li>
     * </ul>
     */
    static void recycle(final Input input) {
        if ((input == null) || ((input.getBuffer() != null) && (input.getBuffer().length > BUFFER_SIZE))) {
            return;
        }

        synchronized (inputPool) {
            if (inputPool.size() < POOL_SIZE) {
                input.setInputStream(null);
                inputPool.add(input);
            }
        }
    }
}
