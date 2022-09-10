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
import com.landawn.abacus.parser.ParserUtil.EntityInfo;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.ByteArrayOutputStream;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.Maps;
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
public final class AvroParser extends AbstractParser<AvroSerializationConfig, AvroDeserializationConfig> {

    /**
     *
     * @param obj
     * @param config
     * @return
     */
    @Override
    public String serialize(Object obj, AvroSerializationConfig config) {
        final ByteArrayOutputStream os = Objectory.createByteArrayOutputStream();

        try {
            serialize(os, obj, config);

            return N.base64Encode(os.toByteArray());
        } finally {
            Objectory.recycle(os);
        }
    }

    /**
     *
     * @param file
     * @param obj
     * @param config
     */
    @Override
    public void serialize(File file, Object obj, AvroSerializationConfig config) {
        OutputStream os = null;

        try {
            createNewFileIfNotExists(file);

            os = IOUtil.newFileOutputStream(file);

            serialize(os, obj, config);

            os.flush();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            IOUtil.close(os);
        }
    }

    /**
     *
     * @param os
     * @param obj
     * @param config
     */
    @SuppressFBWarnings
    @SuppressWarnings("resource")
    @Override
    public void serialize(OutputStream os, Object obj, AvroSerializationConfig config) {
        final Type<Object> type = N.typeOf(obj.getClass());

        if (obj instanceof SpecificRecord record) {
            final DatumWriter<SpecificRecord> datumWriter = new SpecificDatumWriter<>((Class<SpecificRecord>) record.getClass());
            final DataFileWriter<SpecificRecord> dataFileWriter = new DataFileWriter<>(datumWriter);

            try {
                dataFileWriter.create(record.getSchema(), os);
                dataFileWriter.append(record);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            } finally {
                IOUtil.close(dataFileWriter);
            }
        } else if (type.isCollection() && ((Collection<Object>) obj).size() > 0 && ((Collection<Object>) obj).iterator().next() instanceof SpecificRecord) {
            final Collection<SpecificRecord> c = (Collection<SpecificRecord>) obj;
            final SpecificRecord record = c.iterator().next();
            final DatumWriter<SpecificRecord> datumWriter = new SpecificDatumWriter<>((Class<SpecificRecord>) record.getClass());
            final DataFileWriter<SpecificRecord> dataFileWriter = new DataFileWriter<>(datumWriter);

            try {
                dataFileWriter.create(record.getSchema(), os);

                for (SpecificRecord e : c) {
                    dataFileWriter.append(e);
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            } finally {
                IOUtil.close(dataFileWriter);
            }
        } else if (type.isObjectArray() && ((Object[]) obj).length > 0 && ((Object[]) obj)[0] instanceof SpecificRecord) {
            final Object[] a = (Object[]) obj;
            final SpecificRecord record = (SpecificRecord) a[0];
            final DatumWriter<SpecificRecord> datumWriter = new SpecificDatumWriter<>((Class<SpecificRecord>) record.getClass());
            final DataFileWriter<SpecificRecord> dataFileWriter = new DataFileWriter<>(datumWriter);

            try {
                dataFileWriter.create(record.getSchema(), os);

                for (Object e : a) {
                    dataFileWriter.append((SpecificRecord) e);
                }
            } catch (IOException e) {
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
                dataFileWriter.create(schema, os);

                if (obj instanceof GenericRecord) {
                    dataFileWriter.append((GenericRecord) obj);
                } else if (type.isEntity() || type.isMap()) {
                    dataFileWriter.append(toGenericRecord(obj, schema));
                } else if (type.isCollection()) {
                    boolean isMapOrEntity = false;
                    final Collection<Object> c = (Collection<Object>) obj;

                    for (Object e : c) {
                        if (e != null && (e instanceof Map || ClassUtil.isEntity(e.getClass()) || e instanceof GenericRecord)) {
                            isMapOrEntity = true;
                            break;
                        }
                    }

                    if (isMapOrEntity) {
                        for (Object e : c) {
                            dataFileWriter.append(toGenericRecord(e, schema));
                        }
                    } else {
                        dataFileWriter.append(toGenericRecord(obj, schema));
                    }
                } else if (type.isObjectArray()) {
                    boolean isMapOrEntity = false;
                    final Object[] a = (Object[]) obj;

                    for (Object e : a) {
                        if (e != null && (e instanceof Map || ClassUtil.isEntity(e.getClass()) || e instanceof GenericRecord)) {
                            isMapOrEntity = true;
                            break;
                        }
                    }

                    if (isMapOrEntity) {
                        for (Object e : a) {
                            dataFileWriter.append(toGenericRecord(e, schema));
                        }
                    } else {
                        dataFileWriter.append(toGenericRecord(obj, schema));
                    }
                } else if (type.isPrimitiveArray()) {
                    dataFileWriter.append(toGenericRecord(obj, schema));
                } else {
                    throw new IllegalArgumentException("Unsupprted type: " + type.name());
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            } finally {
                IOUtil.close(dataFileWriter);
            }
        }
    }

    /**
     *
     * @param writer
     * @param obj
     * @param config
     */
    @Override
    public void serialize(Writer writer, Object obj, AvroSerializationConfig config) throws UnsupportedOperationException {
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

        if (type.isEntity()) {
            return toGenericRecord(Maps.entity2Map(obj), schema);
        } else if (type.isMap()) {
            final Map<String, Object> m = (Map<String, Object>) obj;
            final Record record = new Record(schema);

            for (Map.Entry<String, Object> entry : m.entrySet()) {
                record.put(entry.getKey(), entry.getValue());
            }

            return record;
        } else if (type.isCollection()) {
            final Collection<Object> c = (Collection<Object>) obj;
            final Record record = new Record(schema);

            int index = 0;
            for (Object e : c) {
                record.put(index++, e);
            }

            return record;
        } else if (type.isObjectArray()) {
            final Object[] a = (Object[]) obj;
            final Record record = new Record(schema);

            int index = 0;
            for (Object e : a) {
                record.put(index++, e);
            }

            return record;
        } else if (type.isPrimitiveArray()) {
            return toGenericRecord(type.array2Collection(List.class, obj), schema);
        } else {
            throw new IllegalArgumentException("Unsupprted type: " + type.name());
        }
    }

    /**
     *
     * @param <T>
     * @param targetClass
     * @param st
     * @param config
     * @return
     */
    @Override
    public <T> T deserialize(Class<? extends T> targetClass, String st, AvroDeserializationConfig config) {
        return deserialize(targetClass, new ByteArrayInputStream(N.base64Decode(st)), config);
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
    public <T> T deserialize(Class<? extends T> targetClass, File file, AvroDeserializationConfig config) {
        InputStream is = null;

        try {
            is = IOUtil.newFileInputStream(file);

            return deserialize(targetClass, is, config);
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
    public <T> T deserialize(Class<? extends T> targetClass, InputStream is, AvroDeserializationConfig config) {
        final Type<T> targetType = N.typeOf(targetClass);
        final Type<Object> eleType = config == null ? null : config.getElementType();

        if (SpecificRecord.class.isAssignableFrom(targetClass)) {
            final DatumReader<T> datumReader = new SpecificDatumReader<>((Class<T>) targetClass);
            DataFileStream<T> dataFileReader = null;
            T entity = null;

            try {
                dataFileReader = new DataFileStream<>(is, datumReader);

                if (dataFileReader.hasNext()) {
                    entity = dataFileReader.next();
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            } finally {
                IOUtil.close(dataFileReader);
            }

            return entity;
        } else if ((targetType.isCollection() || targetType.isObjectArray()) && ((eleType != null && SpecificRecord.class.isAssignableFrom(eleType.clazz()))
                || (targetType.isObjectArray() && SpecificRecord.class.isAssignableFrom(targetClass.getComponentType())))) {
            final Class<Object> eleClass = eleType != null && SpecificRecord.class.isAssignableFrom(eleType.clazz()) ? eleType.clazz()
                    : (Class<Object>) targetClass.getComponentType();
            final Collection<Object> c = targetType.isCollection() ? ((Collection<Object>) N.newCollection(targetClass)) : new ArrayList<>();
            final DatumReader<Object> datumReader = new SpecificDatumReader<>(eleClass);
            DataFileStream<Object> dataFileReader = null;

            try {
                dataFileReader = new DataFileStream<>(is, datumReader);

                while (dataFileReader.hasNext()) {
                    c.add(dataFileReader.next());
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            } finally {
                IOUtil.close(dataFileReader);
            }

            return (T) (targetType.isCollection() ? c : c.toArray((Object[]) N.newArray(targetClass.getComponentType(), c.size())));
        } else {
            if (config == null || config.getSchema() == null) {
                throw new IllegalArgumentException("Schema is not specified");
            }

            final Schema schema = config.getSchema();
            final DatumReader<GenericRecord> datumReader = new GenericDatumReader<>(schema);
            DataFileStream<GenericRecord> dataFileReader = null;

            try {
                dataFileReader = new DataFileStream<>(is, datumReader);

                if (targetClass.isAssignableFrom(GenericRecord.class)) {
                    return (T) (dataFileReader.hasNext() ? dataFileReader.next() : null);
                } else if (targetType.isEntity() || targetType.isMap()) {
                    return dataFileReader.hasNext() ? fromGenericRecord(targetClass, dataFileReader.next()) : null;
                } else if (targetType.isCollection() || targetType.isObjectArray()) {
                    if (eleType != null && (eleType.isEntity() || eleType.isMap() || GenericRecord.class.isAssignableFrom(eleType.clazz()))) {
                        final Collection<Object> c = targetType.isCollection() ? ((Collection<Object>) N.newCollection(targetClass)) : new ArrayList<>();

                        while (dataFileReader.hasNext()) {
                            c.add(fromGenericRecord(eleType.clazz(), dataFileReader.next()));
                        }

                        return (T) (targetType.isCollection() ? c : c.toArray((Object[]) N.newArray(targetClass.getComponentType(), c.size())));
                    } else if (targetType.isObjectArray() && (targetType.getElementType().isEntity() || targetType.getElementType().isMap()
                            || GenericRecord.class.isAssignableFrom(targetClass.getComponentType()))) {
                        final Collection<Object> c = targetType.isCollection() ? ((Collection<Object>) N.newCollection(targetClass)) : new ArrayList<>();

                        while (dataFileReader.hasNext()) {
                            c.add(fromGenericRecord(targetClass.getComponentType(), dataFileReader.next()));
                        }

                        return (T) (targetType.isCollection() ? c : c.toArray((Object[]) N.newArray(targetClass.getComponentType(), c.size())));
                    } else {
                        return dataFileReader.hasNext() ? fromGenericRecord(targetClass, dataFileReader.next()) : null;
                    }
                } else if (targetType.isPrimitiveArray()) {
                    return dataFileReader.hasNext() ? fromGenericRecord(targetClass, dataFileReader.next()) : null;
                } else {
                    throw new IllegalArgumentException("Unsupprted type: " + targetType.name());
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            } finally {
                IOUtil.close(dataFileReader);
            }
        }
    }

    /**
     *
     * @param <T>
     * @param targetClass
     * @param reader
     * @param config
     * @return
     */
    @Override
    public <T> T deserialize(Class<? extends T> targetClass, Reader reader, AvroDeserializationConfig config) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * From generic record.
     *
     * @param <T>
     * @param targetClass
     * @param record
     * @return
     */
    private <T> T fromGenericRecord(final Class<? extends T> targetClass, final GenericRecord record) {
        if (targetClass.isAssignableFrom(record.getClass())) {
            return (T) record;
        }

        final Type<Object> type = N.typeOf(targetClass);

        if (type.isEntity()) {
            final EntityInfo entitInfo = ParserUtil.getEntityInfo(targetClass);
            final Object result = entitInfo.createEntityResult();
            Object propValue = null;

            for (Field field : record.getSchema().getFields()) {
                propValue = record.get(field.name());

                if (propValue != null) {
                    entitInfo.setPropValue(result, field.name(), propValue);
                }
            }
            return entitInfo.finishEntityResult(result);
        } else if (type.isMap()) {
            final Map<String, Object> m = N.newMap(targetClass);
            Object propValue = null;

            for (Field field : record.getSchema().getFields()) {
                propValue = record.get(field.name());

                if (propValue != null) {
                    m.put(field.name(), propValue);
                }
            }

            return (T) m;
        } else if (type.isCollection() || type.isObjectArray()) {
            final Collection<Object> c = type.isCollection() ? (Collection<Object>) N.newCollection(targetClass) : new ArrayList<>();
            Object propValue = null;

            for (Field field : record.getSchema().getFields()) {
                propValue = record.get(field.pos());

                if (propValue != null) {
                    c.add(propValue);
                }
            }

            return (T) (type.isCollection() ? c : c.toArray((Object[]) N.newArray(targetClass.getComponentType(), c.size())));
        } else if (type.isPrimitiveArray()) {
            final Collection<Object> c = new ArrayList<>();
            Object propValue = null;

            for (Field field : record.getSchema().getFields()) {
                propValue = record.get(field.pos());

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
