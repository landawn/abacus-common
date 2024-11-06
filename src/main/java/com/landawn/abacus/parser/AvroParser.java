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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.apache.avro.file.DataFileStream;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.generic.GenericData.Record;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.specific.SpecificRecord;

import com.landawn.abacus.annotation.SuppressFBWarnings;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.parser.ParserUtil.BeanInfo;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.ByteArrayOutputStream;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.Maps;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.Strings;

/**
 * The content is encoded with Base64 if the target output is String or Writer, otherwise the content is NOT encoded with Base64 if the target output is File or OutputStream.
 * So content must be encoded with Base64 if the specified input is String or Reader, otherwise the content must NOT be encoded with Base64 if the specified input is File or InputStream.
 * The reason not to encoded the content with Base64 for File/OutputStream is to provide higher performance solution.
 *
 */
public final class AvroParser extends AbstractParser<AvroSerializationConfig, AvroDeserializationConfig> {

    /**
     *
     * @param obj
     * @param config
     * @return
     */
    @Override
    public String serialize(final Object obj, final AvroSerializationConfig config) {
        final ByteArrayOutputStream os = Objectory.createByteArrayOutputStream();

        try {
            serialize(obj, config, os);

            return Strings.base64Encode(os.toByteArray());
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
    @Override
    public void serialize(final Object obj, final AvroSerializationConfig config, final File output) {
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
     * @param output
     */
    @SuppressFBWarnings
    @SuppressWarnings("resource")
    @Override
    public void serialize(final Object obj, final AvroSerializationConfig config, final OutputStream output) {
        final Type<Object> type = N.typeOf(obj.getClass());

        if (obj instanceof final SpecificRecord specificRecord) {
            final DatumWriter<SpecificRecord> datumWriter = new SpecificDatumWriter<>((Class<SpecificRecord>) specificRecord.getClass());
            final DataFileWriter<SpecificRecord> dataFileWriter = new DataFileWriter<>(datumWriter);

            try {
                dataFileWriter.create(specificRecord.getSchema(), output);
                dataFileWriter.append(specificRecord);
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            } finally {
                IOUtil.close(dataFileWriter);
            }
        } else if (type.isCollection() && ((Collection<Object>) obj).size() > 0 && ((Collection<Object>) obj).iterator().next() instanceof SpecificRecord) {
            final Collection<SpecificRecord> c = (Collection<SpecificRecord>) obj;
            final SpecificRecord specificRecord = c.iterator().next();
            final DatumWriter<SpecificRecord> datumWriter = new SpecificDatumWriter<>((Class<SpecificRecord>) specificRecord.getClass());
            final DataFileWriter<SpecificRecord> dataFileWriter = new DataFileWriter<>(datumWriter);

            try {
                dataFileWriter.create(specificRecord.getSchema(), output);

                for (final SpecificRecord e : c) {
                    dataFileWriter.append(e);
                }
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            } finally {
                IOUtil.close(dataFileWriter);
            }
        } else if (type.isObjectArray() && ((Object[]) obj).length > 0 && ((Object[]) obj)[0] instanceof SpecificRecord) {
            final Object[] a = (Object[]) obj;
            final SpecificRecord specificRecord = (SpecificRecord) a[0];
            final DatumWriter<SpecificRecord> datumWriter = new SpecificDatumWriter<>((Class<SpecificRecord>) specificRecord.getClass());
            final DataFileWriter<SpecificRecord> dataFileWriter = new DataFileWriter<>(datumWriter);

            try {
                dataFileWriter.create(specificRecord.getSchema(), output);

                for (final Object e : a) {
                    dataFileWriter.append((SpecificRecord) e);
                }
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            } finally {
                IOUtil.close(dataFileWriter);
            }
        } else {
            if (config == null || config.getSchema() == null) {
                throw new IllegalArgumentException("Schema is not specified");
            }

            final Schema schema = config.getSchema();
            final DatumWriter<GenericRecord> datumWriter = new GenericDatumWriter<>(schema);
            final DataFileWriter<GenericRecord> dataFileWriter = new DataFileWriter<>(datumWriter);

            try {
                dataFileWriter.create(schema, output);

                if (obj instanceof GenericRecord) {
                    dataFileWriter.append((GenericRecord) obj);
                } else if (type.isBean() || type.isMap()) {
                    dataFileWriter.append(toGenericRecord(obj, schema));
                } else if (type.isCollection()) {
                    boolean isMapOrBean = false;
                    final Collection<Object> c = (Collection<Object>) obj;

                    for (final Object e : c) {
                        if (e != null && (e instanceof Map || ClassUtil.isBeanClass(e.getClass()) || e instanceof GenericRecord)) {
                            isMapOrBean = true;
                            break;
                        }
                    }

                    if (isMapOrBean) {
                        for (final Object e : c) {
                            dataFileWriter.append(toGenericRecord(e, schema));
                        }
                    } else {
                        dataFileWriter.append(toGenericRecord(obj, schema));
                    }
                } else if (type.isObjectArray()) {
                    boolean isMapOrBean = false;
                    final Object[] a = (Object[]) obj;

                    for (final Object e : a) {
                        if (e != null && (e instanceof Map || ClassUtil.isBeanClass(e.getClass()) || e instanceof GenericRecord)) {
                            isMapOrBean = true;
                            break;
                        }
                    }

                    if (isMapOrBean) {
                        for (final Object e : a) {
                            dataFileWriter.append(toGenericRecord(e, schema));
                        }
                    } else {
                        dataFileWriter.append(toGenericRecord(obj, schema));
                    }
                } else if (type.isPrimitiveArray()) {
                    dataFileWriter.append(toGenericRecord(obj, schema));
                } else {
                    throw new IllegalArgumentException("Unsupprted type: " + type.name()); //NOSONAR
                }
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            } finally {
                IOUtil.close(dataFileWriter);
            }
        }
    }

    /**
     *
     *
     * @param obj
     * @param config
     * @param output
     * @throws UnsupportedOperationException
     */
    @Override
    public void serialize(final Object obj, final AvroSerializationConfig config, final Writer output) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * To generic record.
     *
     * @param obj
     * @param schema
     * @return
     */
    private GenericRecord toGenericRecord(final Object obj, final Schema schema) {
        if (obj instanceof GenericRecord) {
            return (GenericRecord) obj;
        }

        final Class<?> cls = obj.getClass();
        final Type<Object> type = N.typeOf(cls);

        if (type.isBean()) {
            return toGenericRecord(Maps.bean2Map(obj), schema);
        } else if (type.isMap()) {
            final Map<String, Object> m = (Map<String, Object>) obj;
            final Record localRecord = new Record(schema);

            for (final Map.Entry<String, Object> entry : m.entrySet()) {
                localRecord.put(entry.getKey(), entry.getValue());
            }

            return localRecord;
        } else if (type.isCollection()) {
            final Collection<Object> c = (Collection<Object>) obj;
            final Record localRecord = new Record(schema);

            int index = 0;
            for (final Object e : c) {
                localRecord.put(index++, e);
            }

            return localRecord;
        } else if (type.isObjectArray()) {
            final Object[] a = (Object[]) obj;
            final Record localRecord = new Record(schema);

            int index = 0;
            for (final Object e : a) {
                localRecord.put(index++, e);
            }

            return localRecord;
        } else if (type.isPrimitiveArray()) {
            return toGenericRecord(type.array2Collection(obj, List.class), schema);
        } else {
            throw new IllegalArgumentException("Unsupprted type: " + type.name());
        }
    }

    /**
     *
     *
     * @param <T>
     * @param source
     * @param config
     * @param targetClass
     * @return
     */
    @Override
    public <T> T deserialize(final String source, final AvroDeserializationConfig config, final Class<? extends T> targetClass) {
        return deserialize(new ByteArrayInputStream(Strings.base64Decode(source)), config, targetClass);
    }

    /**
     *
     *
     * @param <T>
     * @param source
     * @param config
     * @param targetClass
     * @return
     */
    @Override
    public <T> T deserialize(final File source, final AvroDeserializationConfig config, final Class<? extends T> targetClass) {
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
     *
     * @param <T>
     * @param source
     * @param config
     * @param targetClass
     * @return
     */
    @Override
    public <T> T deserialize(final InputStream source, final AvroDeserializationConfig config, final Class<? extends T> targetClass) {
        final Type<T> targetType = N.typeOf(targetClass);
        final Type<Object> eleType = config == null ? null : config.getElementType();

        if (SpecificRecord.class.isAssignableFrom(targetClass)) {
            final DatumReader<T> datumReader = new SpecificDatumReader<>((Class<T>) targetClass);
            T bean = null;

            try (DataFileStream<T> dataFileReader = new DataFileStream<>(source, datumReader)) {

                if (dataFileReader.hasNext()) {
                    bean = dataFileReader.next();
                }
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }

            return bean;
        } else if ((targetType.isCollection() || targetType.isObjectArray()) && ((eleType != null && SpecificRecord.class.isAssignableFrom(eleType.clazz()))
                || (targetType.isObjectArray() && SpecificRecord.class.isAssignableFrom(targetClass.getComponentType())))) {
            final Class<Object> eleClass = eleType != null && SpecificRecord.class.isAssignableFrom(eleType.clazz()) ? eleType.clazz()
                    : (Class<Object>) targetClass.getComponentType();
            @SuppressWarnings("rawtypes")
            final Collection<Object> c = targetType.isCollection() ? ((Collection<Object>) N.newCollection((Class<Collection>) targetClass))
                    : new ArrayList<>();
            final DatumReader<Object> datumReader = new SpecificDatumReader<>(eleClass);

            try (DataFileStream<Object> dataFileReader = new DataFileStream<>(source, datumReader)) {

                while (dataFileReader.hasNext()) {
                    c.add(dataFileReader.next());
                }
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }

            return (T) (targetType.isCollection() ? c : c.toArray((Object[]) N.newArray(targetClass.getComponentType(), c.size())));
        } else {
            if (config == null || config.getSchema() == null) {
                throw new IllegalArgumentException("Schema is not specified");
            }

            final Schema schema = config.getSchema();
            final DatumReader<GenericRecord> datumReader = new GenericDatumReader<>(schema);

            try (DataFileStream<GenericRecord> dataFileReader = new DataFileStream<>(source, datumReader)) {
                if (targetClass.isAssignableFrom(GenericRecord.class)) {
                    return (T) (dataFileReader.hasNext() ? dataFileReader.next() : null);
                } else if (targetType.isBean() || targetType.isMap()) {
                    return dataFileReader.hasNext() ? fromGenericRecord(dataFileReader.next(), targetClass) : null;
                } else if (targetType.isCollection() || targetType.isObjectArray()) {
                    if (eleType != null && (eleType.isBean() || eleType.isMap() || GenericRecord.class.isAssignableFrom(eleType.clazz()))) {
                        @SuppressWarnings("rawtypes")
                        final Collection<Object> c = targetType.isCollection() ? ((Collection<Object>) N.newCollection((Class<Collection>) targetClass))
                                : new ArrayList<>();

                        while (dataFileReader.hasNext()) {
                            c.add(fromGenericRecord(dataFileReader.next(), eleType.clazz()));
                        }

                        return (T) (targetType.isCollection() ? c : c.toArray((Object[]) N.newArray(targetClass.getComponentType(), c.size())));
                    } else if (targetType.isObjectArray() && (targetType.getElementType().isBean() || targetType.getElementType().isMap()
                            || GenericRecord.class.isAssignableFrom(targetClass.getComponentType()))) {
                        @SuppressWarnings("rawtypes")
                        final Collection<Object> c = targetType.isCollection() ? ((Collection<Object>) N.newCollection((Class<Collection>) targetClass))
                                : new ArrayList<>();

                        while (dataFileReader.hasNext()) {
                            c.add(fromGenericRecord(dataFileReader.next(), targetClass.getComponentType()));
                        }

                        return (T) (targetType.isCollection() ? c : c.toArray((Object[]) N.newArray(targetClass.getComponentType(), c.size())));
                    } else {
                        return dataFileReader.hasNext() ? fromGenericRecord(dataFileReader.next(), targetClass) : null;
                    }
                } else if (targetType.isPrimitiveArray()) {
                    return dataFileReader.hasNext() ? fromGenericRecord(dataFileReader.next(), targetClass) : null;
                } else {
                    throw new IllegalArgumentException("Unsupprted type: " + targetType.name());
                }
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    /**
     *
     *
     * @param <T>
     * @param source
     * @param config
     * @param targetClass
     * @return
     * @throws UnsupportedOperationException
     */
    @Override
    public <T> T deserialize(final Reader source, final AvroDeserializationConfig config, final Class<? extends T> targetClass)
            throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * From generic record.
     * @param source
     * @param targetClass
     *
     * @param <T>
     * @return
     */
    private <T> T fromGenericRecord(final GenericRecord source, final Class<? extends T> targetClass) {
        if (targetClass.isAssignableFrom(source.getClass())) {
            return (T) source;
        }

        final Type<Object> type = N.typeOf(targetClass);

        if (type.isBean()) {
            final BeanInfo entitInfo = ParserUtil.getBeanInfo(targetClass);
            final Object result = entitInfo.createBeanResult();
            Object propValue = null;

            for (final Field field : source.getSchema().getFields()) {
                propValue = source.get(field.name());

                if (propValue != null) {
                    entitInfo.setPropValue(result, field.name(), propValue);
                }
            }
            return entitInfo.finishBeanResult(result);
        } else if (type.isMap()) {
            @SuppressWarnings("rawtypes")
            final Map<String, Object> m = N.newMap((Class<Map>) targetClass);
            Object propValue = null;

            for (final Field field : source.getSchema().getFields()) {
                propValue = source.get(field.name());

                if (propValue != null) {
                    m.put(field.name(), propValue);
                }
            }

            return (T) m;
        } else if (type.isCollection() || type.isObjectArray()) {
            @SuppressWarnings("rawtypes")
            final Collection<Object> c = type.isCollection() ? (Collection<Object>) N.newCollection((Class<Collection>) targetClass) : new ArrayList<>();
            Object propValue = null;

            for (final Field field : source.getSchema().getFields()) {
                propValue = source.get(field.pos());

                if (propValue != null) {
                    c.add(propValue);
                }
            }

            return (T) (type.isCollection() ? c : c.toArray((Object[]) N.newArray(targetClass.getComponentType(), c.size())));
        } else if (type.isPrimitiveArray()) {
            final Collection<Object> c = new ArrayList<>();
            Object propValue = null;

            for (final Field field : source.getSchema().getFields()) {
                propValue = source.get(field.pos());

                if (propValue != null) {
                    c.add(propValue);
                }
            }

            return (T) type.collection2Array(c);
        } else {
            throw new IllegalArgumentException("Unsupprted type: " + type.name());
        }
    }
}
