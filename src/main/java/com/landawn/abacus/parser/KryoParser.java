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
import com.landawn.abacus.util.DataSet;
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
import com.landawn.abacus.util.RowDataSet;
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
 * The content is encoded with Base64 if the target output is String or Writer, otherwise the content is NOT encoded with Base64 if the target output is File or OutputStream.
 * So content must be encoded with Base64 if the specified input is String or Reader, otherwise the content must NOT be encoded with Base64 if the specified input is File or InputStream.
 * The reason not to encode the content with Base64 for File/OutputStream is to provide a higher performance solution.
 *
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
     *
     * @param obj
     * @param config
     * @return a Base64 encoded String
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
     *
     * @param obj
     * @param config
     * @param output content is NOT encoded with base64
     */
    @Override
    public void serialize(final Object obj, final KryoSerializationConfig config, final File output) {
        Writer writer = null;

        try {
            createNewFileIfNotExists(output);

            writer = IOUtil.newFileWriter(output);

            serialize(obj, config, writer);

            writer.flush();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            IOUtil.close(writer);
        }
    }

    /**
     *
     * @param obj
     * @param config
     * @param output content is NOT encoded with base64
     */
    @Override
    public void serialize(final Object obj, final KryoSerializationConfig config, final OutputStream output) {
        write(obj, config, output);
    }

    /**
     *
     * @param obj
     * @param config
     * @param output content is encoded with base64
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
     *
     * @param obj
     * @param config
     * @param output
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
     *
     * @param obj
     * @param config
     * @param output
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
     *
     * @param <T>
     * @param source A Base64 encoded String
     * @param config
     * @param targetClass
     * @return
     */
    @Override
    public <T> T deserialize(final String source, final KryoDeserializationConfig config, final Class<? extends T> targetClass) {
        final Input input = createInput();

        try {
            input.setBuffer(Strings.base64Decode(source));

            return read(input, config, targetClass);
        } finally {
            recycle(input);
        }
    }

    /**
     *
     * @param <T>
     * @param source
     * @param config
     * @param targetClass
     * @return
     */
    @Override
    public <T> T deserialize(final File source, final KryoDeserializationConfig config, final Class<? extends T> targetClass) {
        Reader reader = null;

        try {
            reader = IOUtil.newFileReader(source);

            return deserialize(reader, config, targetClass);
        } finally {
            IOUtil.close(reader);
        }
    }

    /**
     *
     * @param <T>
     * @param source
     * @param config
     * @param targetClass
     * @return
     */
    @Override
    public <T> T deserialize(final InputStream source, final KryoDeserializationConfig config, final Class<? extends T> targetClass) {
        return read(source, config, targetClass);
    }

    /**
     *
     * @param <T>
     * @param source content is encoded with base64
     * @param config
     * @param targetClass
     * @return
     */
    @Override
    public <T> T deserialize(final Reader source, final KryoDeserializationConfig config, final Class<? extends T> targetClass) {
        return deserialize(IOUtil.readAllToString(source), config, targetClass);
    }

    /**
     *
     * @param source
     * @param config
     * @param targetClass
     * @param <T>
     * @return
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
     *
     * @param source
     * @param config
     * @param targetClass
     * @param <T>
     * @return
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
     *
     * @param config
     * @return
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
     *
     * @param config
     * @return
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
     * Copy the property values shallowly.
     *
     * @param <T>
     * @param source
     * @return
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
     * Copy the property values deeply.
     *
     * @param <T>
     * @param source
     * @return
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
     *
     * @param source
     * @return
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
     *
     * @param <T>
     * @param source
     * @return
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
     *
     * @param type
     * @throws IllegalArgumentException
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
     *
     * @param type
     * @param id
     * @throws IllegalArgumentException
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
     *
     * @param type
     * @param serializer
     * @throws IllegalArgumentException
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
     *
     * @param type
     * @param serializer
     * @param id
     * @throws IllegalArgumentException
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
        builtInClassesToRegister.add(DataSet.class);
        builtInClassesToRegister.add(RowDataSet.class);
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

    static Output createOutput() {
        synchronized (outputPool) {
            if (outputPool.size() > 0) {
                return outputPool.remove(outputPool.size() - 1);
            }
            return new Output(BUFFER_SIZE);
        }
    }

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

    static Input createInput() {
        synchronized (inputPool) {
            if (inputPool.size() > 0) {
                return inputPool.remove(inputPool.size() - 1);
            }
            return new Input(BUFFER_SIZE);
        }
    }

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
