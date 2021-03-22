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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.util.ByteArrayOutputStream;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Objectory;

/**
 * The content is encoded with Base64 if the target output is String or Writer, otherwise the content is NOT encoded with Base64 if the target output is File or OutputStream.
 * So content must be encoded with Base64 if the specified input is String or Reader, otherwise the content must NOT be encoded with Base64 if the specified input is File or InputStream.
 * The reason not to encoded the content with Base64 for File/OutputStream is to provide higher performance solution.
 *
 * @author Haiyang Li
 * @since 0.8
 */
public final class KryoParser extends AbstractParser<KryoSerializationConfig, KryoDeserializationConfig> {

    private static final int BUFFER_SIZE = 8192;

    private static final List<Output> outputPool = new ArrayList<>(POOL_SIZE);

    private static final List<Input> inputPool = new ArrayList<>(POOL_SIZE);

    private final Map<Class<?>, Integer> kryoClassIdMap = new ConcurrentHashMap<>();

    private final Map<Class<?>, Serializer<?>> kryoClassSerializerMap = new ConcurrentHashMap<>();

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
    public String serialize(Object obj, KryoSerializationConfig config) {
        final ByteArrayOutputStream os = Objectory.createByteArrayOutputStream();

        try {
            write(os, obj, config);

            return N.base64Encode(os.toByteArray());
        } finally {
            Objectory.recycle(os);
        }
    }

    /**
     *
     * @param file content is NOT encoded with base64
     * @param obj
     * @param config
     */
    @Override
    public void serialize(File file, Object obj, KryoSerializationConfig config) {
        OutputStream os = null;

        try {
            if (!file.exists()) {
                file.createNewFile();
            }

            os = new FileOutputStream(file);

            write(os, obj, config);

            os.flush();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            IOUtil.close(os);
        }
    }

    /**
     *
     * @param os content is NOT encoded with base64
     * @param obj
     * @param config
     */
    @Override
    public void serialize(OutputStream os, Object obj, KryoSerializationConfig config) {
        write(os, obj, config);
    }

    /**
     *
     * @param writer content is encoded with base64
     * @param obj
     * @param config
     */
    @Override
    public void serialize(Writer writer, Object obj, KryoSerializationConfig config) {
        final ByteArrayOutputStream os = Objectory.createByteArrayOutputStream();

        try {
            write(os, obj, config);

            writer.write(N.base64Encode(os.toByteArray()));

            writer.flush();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            Objectory.recycle(os);
        }
    }

    /**
     *
     * @param os
     * @param obj
     * @param config
     */
    protected void write(OutputStream os, Object obj, KryoSerializationConfig config) {
        Output output = createOutput();

        try {
            output.setOutputStream(os);

            write(output, obj, config);
        } finally {
            recycle(output);
        }
    }

    /**
     *
     * @param output
     * @param obj
     * @param config
     */
    protected void write(Output output, Object obj, KryoSerializationConfig config) {
        check(config);

        Kryo kryo = createKryo();

        try {
            if (config != null && config.isWriteClass()) {
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
     * @param targetClass
     * @param st A Base64 encoded String
     * @param config
     * @return
     */
    @Override
    public <T> T deserialize(Class<T> targetClass, String st, KryoDeserializationConfig config) {
        Input input = createInput();

        try {
            input.setBuffer(N.base64Decode(st));

            return read(targetClass, input, config);
        } finally {
            recycle(input);
        }
    }

    /**
     *
     * @param <T>
     * @param targetClass
     * @param file
     * @param config
     * @return
     */
    @Override
    public <T> T deserialize(Class<T> targetClass, File file, KryoDeserializationConfig config) {
        InputStream is = null;

        try {
            is = new FileInputStream(file);

            return read(targetClass, is, config);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            IOUtil.close(is);
        }
    }

    /**
     *
     * @param <T>
     * @param targetClass
     * @param is
     * @param config
     * @return
     */
    @Override
    public <T> T deserialize(Class<T> targetClass, InputStream is, KryoDeserializationConfig config) {
        return read(targetClass, is, config);
    }

    /**
     *
     * @param <T>
     * @param targetClass
     * @param reader content is encoded with base64
     * @param config
     * @return
     */
    @Override
    public <T> T deserialize(Class<T> targetClass, Reader reader, KryoDeserializationConfig config) {
        return deserialize(targetClass, IOUtil.readString(reader), config);
    }

    /**
     *
     * @param <T>
     * @param targetClass
     * @param is
     * @param config
     * @return
     */
    protected <T> T read(Class<T> targetClass, InputStream is, KryoDeserializationConfig config) {
        Input input = createInput();

        try {
            input.setInputStream(is);

            return read(targetClass, input, config);
        } finally {
            recycle(input);
        }
    }

    /**
     *
     * @param <T>
     * @param targetClass
     * @param input
     * @param config
     * @return
     */
    @SuppressWarnings("unchecked")
    protected <T> T read(Class<T> targetClass, Input input, KryoDeserializationConfig config) {
        check(config);

        Kryo kryo = createKryo();

        try {
            return (T) ((targetClass == null) ? kryo.readClassAndObject(input) : kryo.readObject(input, targetClass));
        } finally {
            recycle(kryo);
        }
    }

    /**
     *
     * @param config
     * @return
     */
    protected KryoSerializationConfig check(KryoSerializationConfig config) {
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
    protected KryoDeserializationConfig check(KryoDeserializationConfig config) {
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
     * @param obj
     * @return
     */
    public <T> T copy(T obj) {
        Kryo kryo = createKryo();

        try {
            return kryo.copyShallow(obj);
        } finally {
            recycle(kryo);
        }
    }

    /**
     * Copy the property values deeply.
     *
     * @param <T>
     * @param obj
     * @return
     */
    public <T> T clone(T obj) {
        Kryo kryo = createKryo();

        try {
            return kryo.copy(obj);
        } finally {
            recycle(kryo);
        }
    }

    /**
     *
     * @param obj
     * @return
     */
    public byte[] encode(Object obj) {
        final ByteArrayOutputStream os = Objectory.createByteArrayOutputStream();
        Output output = createOutput();
        Kryo kryo = createKryo();

        try {
            output.setOutputStream(os);
            kryo.writeClassAndObject(output, obj);

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
     * @param bytes
     * @return
     */
    @SuppressWarnings("unchecked")
    public <T> T decode(byte[] bytes) {
        Input input = createInput();
        Kryo kryo = createKryo();

        try {
            input.setBuffer(bytes);

            return (T) kryo.readClassAndObject(input);
        } finally {
            recycle(input);
            recycle(kryo);
        }
    }

    /**
     *
     * @param cls
     * @param id
     */
    public void register(Class<?> cls, int id) {
        synchronized (kryoPool) {
            kryoClassIdMap.put(cls, id);

            xPool.clear();
            kryoPool.clear();
        }
    }

    /**
     *
     * @param cls
     * @param serializer
     */
    public void register(Class<?> cls, Serializer<?> serializer) {
        synchronized (kryoPool) {
            kryoClassSerializerMap.put(cls, serializer);

            xPool.clear();
            kryoPool.clear();
        }
    }

    /**
     * Creates the kryo.
     *
     * @return
     */
    protected Kryo createKryo() {
        synchronized (kryoPool) {
            if (kryoPool.size() > 0) {
                return kryoPool.remove(kryoPool.size() - 1);
            } else {
                Kryo kryo = new Kryo();

                if (N.notNullOrEmpty(kryoClassIdMap)) {
                    for (Class<?> cls : kryoClassIdMap.keySet()) {
                        kryo.register(cls, kryoClassIdMap.get(cls));
                    }
                }

                if (N.notNullOrEmpty(kryoClassSerializerMap)) {
                    for (Class<?> cls : kryoClassSerializerMap.keySet()) {
                        kryo.register(cls, kryoClassSerializerMap.get(cls));
                    }
                }

                xPool.put(kryo, kryo);

                return kryo;
            }
        }
    }

    /**
     *
     * @param kryo
     */
    protected void recycle(Kryo kryo) {
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
     * Creates the output.
     *
     * @return
     */
    protected static Output createOutput() {
        synchronized (outputPool) {
            if (outputPool.size() > 0) {
                return outputPool.remove(outputPool.size() - 1);
            } else {
                return new Output(BUFFER_SIZE);
            }
        }
    }

    /**
     *
     * @param output
     */
    protected static void recycle(Output output) {
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
     * Creates the input.
     *
     * @return
     */
    protected static Input createInput() {
        synchronized (inputPool) {
            if (inputPool.size() > 0) {
                return inputPool.remove(inputPool.size() - 1);
            } else {
                return new Input(BUFFER_SIZE);
            }
        }
    }

    /**
     *
     * @param input
     */
    protected static void recycle(Input input) {
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
