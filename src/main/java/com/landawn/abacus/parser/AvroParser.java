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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
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
import com.landawn.abacus.util.Beans;
import com.landawn.abacus.util.ByteArrayOutputStream;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.Strings;

/**
 * Parser implementation for Apache Avro format serialization and deserialization.
 * 
 * <p>This parser handles Avro data serialization with special encoding rules:</p>
 * <ul>
 *   <li>Content is Base64 encoded when output is String or Writer</li>
 *   <li>Content is NOT Base64 encoded when output is File or OutputStream</li>
 *   <li>Input must be Base64 encoded when source is String or Reader</li>
 *   <li>Input must NOT be Base64 encoded when source is File or InputStream</li>
 * </ul>
 * 
 * <p>The reason for not encoding content with Base64 for File/OutputStream is to provide
 * a higher performance solution for binary data handling.</p>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // With SpecificRecord
 * User user = new User("John", 30);
 * AvroParser parser = new AvroParser();
 * 
 * // Serialize to file (not Base64 encoded)
 * parser.serialize(user, new File("user.avro"));
 * 
 * // Serialize to string (Base64 encoded)
 * String encoded = parser.serialize(user);
 * 
 * // With generic records and schema
 * Schema schema = new Schema.Parser().parse(schemaJson);
 * AvroSerializationConfig config = new AvroSerializationConfig().setSchema(schema);
 * 
 * Map<String, Object> data = Map.of("name", "John", "age", 30);
 * parser.serialize(data, config, outputStream);
 * }</pre>
 * 
 * @since 0.8
 */
public final class AvroParser extends AbstractParser<AvroSerializationConfig, AvroDeserializationConfig> {

    /**
     * Constructs a new AvroParser instance.
     * This parser can be used to serialize and deserialize objects to/from Apache Avro format.
     */
    public AvroParser() {
        // Default constructor
    }

    /**
     * Serializes an object to Avro format encoded as a Base64 string.
     * The content is Base64 encoded to make it suitable for text-based transmission.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * User user = new User("John", 30);
     * AvroSerializationConfig config = new AvroSerializationConfig()
     *     .setSchema(User.getSchema());
     * String base64Encoded = parser.serialize(user, config);
     * }</pre>
     *
     * @param obj the object to serialize
     * @param config the serialization configuration (must contain schema if obj is not SpecificRecord)
     * @return Base64 encoded string of the Avro binary data
     * @throws IllegalArgumentException if schema is not specified for non-SpecificRecord objects
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
     * Serializes an object to an Avro file.
     * The content is written in binary format without Base64 encoding.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<User> users = Arrays.asList(
     *     new User("John", 30),
     *     new User("Jane", 25)
     * );
     * parser.serialize(users, new File("users.avro"));
     * }</pre>
     *
     * @param obj the object to serialize
     * @param config the serialization configuration
     * @param output the output file
     * @throws UncheckedIOException if I/O error occurs
     */
    @Override
    public void serialize(final Object obj, final AvroSerializationConfig config, final File output) {
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
     * Serializes an object to an OutputStream in Avro binary format.
     * The content is NOT Base64 encoded for performance reasons.
     * 
     * <p>This method supports:</p>
     * <ul>
     *   <li>SpecificRecord instances (Avro generated classes)</li>
     *   <li>Collections of SpecificRecord</li>
     *   <li>GenericRecord instances</li>
     *   <li>Regular Java beans and Maps (requires schema in config)</li>
     * </ul>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // With SpecificRecord
     * User user = new User("John", 30);
     * try (OutputStream os = new FileOutputStream("user.avro")) {
     *     parser.serialize(user, null, os);
     * }
     * 
     * // With regular bean and schema
     * Person person = new Person("John", 30);
     * AvroSerializationConfig config = new AvroSerializationConfig()
     *     .setSchema(personSchema);
     * parser.serialize(person, config, outputStream);
     * }</pre>
     *
     * @param obj the object to serialize
     * @param config the serialization configuration (optional for SpecificRecord)
     * @param output the output stream
     * @throws IllegalArgumentException if schema is not specified for non-SpecificRecord objects
     * @throws UncheckedIOException if I/O error occurs
     */
    @SuppressFBWarnings
    @Override
    public void serialize(final Object obj, final AvroSerializationConfig config, final OutputStream output) {
        final Type<Object> type = N.typeOf(obj.getClass());

        if (obj instanceof SpecificRecord specificRecord) {
            final SpecificDatumWriter<SpecificRecord> datumWriter = new SpecificDatumWriter<>((Class<SpecificRecord>) specificRecord.getClass());

            try (final DataFileWriter<SpecificRecord> dataFileWriter = new DataFileWriter<>(datumWriter).create(specificRecord.getSchema(), output)) {
                dataFileWriter.append(specificRecord);
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }
        } else if (type.isCollection() && ((Collection<Object>) obj).size() > 0 && ((Collection<Object>) obj).iterator().next() instanceof SpecificRecord) {
            final Collection<SpecificRecord> c = (Collection<SpecificRecord>) obj;
            final SpecificRecord specificRecord = c.iterator().next();
            final DatumWriter<SpecificRecord> datumWriter = new SpecificDatumWriter<>((Class<SpecificRecord>) specificRecord.getClass());

            try (final DataFileWriter<SpecificRecord> dataFileWriter = new DataFileWriter<>(datumWriter).create(specificRecord.getSchema(), output)) {
                for (final SpecificRecord e : c) {
                    dataFileWriter.append(e);
                }
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }
        } else {
            if (config == null || config.getSchema() == null) {
                throw new IllegalArgumentException("Schema is not specified");
            }

            final Schema schema = config.getSchema();
            final DatumWriter<Object> datumWriter = new GenericDatumWriter<>(schema);

            try (final DataFileWriter<Object> dataFileWriter = new DataFileWriter<>(datumWriter).create(schema, output)) {
                if (obj instanceof GenericRecord genericRecord) {
                    dataFileWriter.append(genericRecord);
                } else if (type.isBean() || type.isMap()) {
                    dataFileWriter.append(toGenericRecord(obj, schema));
                } else if (type.isCollection()) {
                    boolean isMapOrBean = false;
                    final Collection<Object> c = (Collection<Object>) obj;

                    for (final Object e : c) {
                        if (e != null && (e instanceof Map || Beans.isBeanClass(e.getClass()) || e instanceof GenericRecord)) {
                            isMapOrBean = true;
                            break;
                        }
                    }

                    if (isMapOrBean) {
                        for (final Object e : c) {
                            dataFileWriter.append(toGenericRecord(e, schema));
                        }
                    } else {
                        dataFileWriter.append(c);
                    }
                } else {
                    throw new IllegalArgumentException("Unsupported type: " + type.name()); //NOSONAR
                }
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    /**
     * Serialization to Writer is not supported for Avro format.
     * Use {@link #serialize(Object, AvroSerializationConfig)} to get Base64 encoded string
     * or {@link #serialize(Object, AvroSerializationConfig, OutputStream)} for binary output.
     *
     * @param obj the object to serialize
     * @param config the serialization configuration
     * @param output the writer (not supported)
     * @throws UnsupportedOperationException always thrown as this operation is not supported
     * @deprecated This method is deprecated and will always throw UnsupportedOperationException.
     */
    @Deprecated
    @Override
    public void serialize(final Object obj, final AvroSerializationConfig config, final Writer output) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Converts a Java object to an Avro GenericRecord.
     * Supports beans, maps, collections.
     *
     * @param obj the object to convert
     * @param schema the Avro schema
     * @return GenericRecord representation of the object
     * @throws IllegalArgumentException if the object type is not supported
     */
    private GenericRecord toGenericRecord(final Object obj, final Schema schema) {
        if (obj instanceof GenericRecord genericrecord) {
            return genericrecord;
        }

        final Class<?> cls = obj.getClass();
        final Type<Object> type = N.typeOf(cls);

        if (type.isBean()) {
            return toGenericRecord(Beans.bean2Map(obj), schema);
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
        } else {
            throw new IllegalArgumentException("Unsupported type: " + type.name());
        }
    }

    /**
     * Deserializes a Base64 encoded Avro string to an object.
     * The input string must be Base64 encoded Avro binary data.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String base64Data = "..."; // Base64 encoded Avro data
     * AvroDeserializationConfig config = new AvroDeserializationConfig()
     *     .setSchema(User.getSchema());
     * User user = parser.deserialize(base64Data, config, User.class);
     * }</pre>
     *
     * @param <T> the target type
     * @param source Base64 encoded Avro data
     * @param config the deserialization configuration
     * @param targetClass the target class
     * @return the deserialized object
     */
    @Override
    public <T> T deserialize(final String source, final AvroDeserializationConfig config, final Class<? extends T> targetClass) {
        return deserialize(new ByteArrayInputStream(Strings.base64Decode(source)), config, targetClass);
    }

    /**
     * Deserializes an Avro file to an object.
     * The file should contain binary Avro data (not Base64 encoded).
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File avroFile = new File("users.avro");
     * List<User> users = parser.deserialize(avroFile, null, List.class);
     * }</pre>
     *
     * @param <T> the target type
     * @param source the Avro file
     * @param config the deserialization configuration
     * @param targetClass the target class
     * @return the deserialized object
     */
    @Override
    public <T> T deserialize(final File source, final AvroDeserializationConfig config, final Class<? extends T> targetClass) {
        InputStream is = null;

        try {
            is = IOUtil.newFileInputStream(source);

            return deserialize(is, config, targetClass);
        } finally {
            IOUtil.close(is);
        }
    }

    /**
     * Deserializes Avro data from an InputStream to an object.
     * The stream should contain binary Avro data (not Base64 encoded).
     * 
     * <p>This method supports:</p>
     * <ul>
     *   <li>SpecificRecord classes (Avro generated classes)</li>
     *   <li>Collections of SpecificRecord</li>
     *   <li>GenericRecord</li>
     *   <li>Regular Java beans and Maps (requires schema in config)</li>
     * </ul>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Deserialize to SpecificRecord
     * try (InputStream is = new FileInputStream("user.avro")) {
     *     User user = parser.deserialize(is, null, User.class);
     * }
     * 
     * // Deserialize to regular bean with schema
     * AvroDeserializationConfig config = new AvroDeserializationConfig()
     *     .setSchema(schema)
     *     .setElementType(Person.class);
     * List<Person> people = parser.deserialize(inputStream, config, List.class);
     * }</pre>
     *
     * @param <T> the target type
     * @param source the input stream with Avro data
     * @param config the deserialization configuration
     * @param targetClass the target class
     * @return the deserialized object
     * @throws IllegalArgumentException if schema is not specified for non-SpecificRecord types
     * @throws UncheckedIOException if I/O error occurs
     */
    @Override
    public <T> T deserialize(final InputStream source, final AvroDeserializationConfig config, final Class<? extends T> targetClass) {
        final Type<T> targetType = N.typeOf(targetClass);
        final Type<Object> eleType = config == null ? null : config.getElementType();

        if (SpecificRecord.class.isAssignableFrom(targetClass)) {
            final SpecificDatumReader<T> datumReader = new SpecificDatumReader<>((Class<T>) targetClass);
            T bean = null;

            try (DataFileStream<T> dataFileReader = new DataFileStream<>(source, datumReader)) {
                if (dataFileReader.hasNext()) {
                    bean = dataFileReader.next();
                }
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }

            return bean;
        } else if (targetType.isCollection() && (eleType != null && SpecificRecord.class.isAssignableFrom(eleType.clazz()))) {
            final Class<Object> eleClass = eleType.clazz();
            @SuppressWarnings("rawtypes")
            final Collection<Object> c = targetType.isCollection() ? N.newCollection((Class<Collection>) targetClass) : new ArrayList<>();
            final DatumReader<Object> datumReader = new SpecificDatumReader<>(eleClass);

            try (DataFileStream<Object> dataFileReader = new DataFileStream<>(source, datumReader)) {
                while (dataFileReader.hasNext()) {
                    c.add(dataFileReader.next());
                }
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }

            return (T) c;
        } else {
            if (config == null || config.getSchema() == null) {
                throw new IllegalArgumentException("Schema is not specified");
            }

            final Schema schema = config.getSchema();
            final DatumReader<Object> datumReader = new GenericDatumReader<>(schema);

            try (DataFileStream<Object> dataFileReader = new DataFileStream<>(source, datumReader)) {
                if (targetClass.isAssignableFrom(GenericRecord.class)) {
                    return (T) (dataFileReader.hasNext() ? dataFileReader.next() : null);
                } else if (targetType.isBean() || targetType.isMap()) {
                    return dataFileReader.hasNext() ? fromGenericRecord((GenericRecord) dataFileReader.next(), targetClass) : null;
                } else if (targetType.isCollection()) {
                    if (eleType != null && (eleType.isBean() || eleType.isMap() || GenericRecord.class.isAssignableFrom(eleType.clazz()))) {
                        @SuppressWarnings("rawtypes")
                        final Collection<Object> c = targetType.isCollection() ? N.newCollection((Class<Collection>) targetClass) : new ArrayList<>();

                        while (dataFileReader.hasNext()) {
                            c.add(fromGenericRecord((GenericRecord) dataFileReader.next(), eleType.clazz()));
                        }

                        return (T) c;
                    } else {
                        return dataFileReader.hasNext() ? (T) dataFileReader.next() : null;
                    }
                } else {
                    throw new IllegalArgumentException("Unsupported type: " + targetType.name());
                }
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    /**
     * Deserialization from Reader is not supported for Avro format.
     * Use {@link #deserialize(String, AvroDeserializationConfig, Class)} for Base64 encoded input
     * or {@link #deserialize(InputStream, AvroDeserializationConfig, Class)} for binary input.
     *
     * @param <T> the target type
     * @param source the reader (not supported)
     * @param config the deserialization configuration
     * @param targetClass the target class
     * @return never returns
     * @throws UnsupportedOperationException always thrown as this operation is not supported
     * @deprecated This method is deprecated and will always throw UnsupportedOperationException.
     */
    @Deprecated
    @Override
    public <T> T deserialize(final Reader source, final AvroDeserializationConfig config, final Class<? extends T> targetClass)
            throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Converts an Avro GenericRecord to a Java object.
     * Supports conversion to beans, maps, collections, and arrays.
     * 
     * @param source the GenericRecord to convert
     * @param targetClass the target class
     * @param <T> the target type
     * @return the converted object
     * @throws IllegalArgumentException if the target type is not supported
     */
    private <T> T fromGenericRecord(final GenericRecord source, final Class<? extends T> targetClass) {
        if (targetClass.isAssignableFrom(source.getClass())) {
            return (T) source;
        }

        final Type<Object> type = N.typeOf(targetClass);

        if (type.isBean()) {
            final BeanInfo beanInfo = ParserUtil.getBeanInfo(targetClass);
            final Object result = beanInfo.createBeanResult();
            String propName = null;
            Object propValue = null;

            for (final Field field : source.getSchema().getFields()) {
                propName = field.name();
                propValue = source.get(propName);

                if (propValue != null) {
                    beanInfo.setPropValue(result, propName, propValue);
                }
            }
            return beanInfo.finishBeanResult(result);
        } else if (type.isMap()) {
            @SuppressWarnings("rawtypes")
            final Map<String, Object> m = N.newMap((Class<Map>) targetClass);
            String propName = null;
            Object propValue = null;

            for (final Field field : source.getSchema().getFields()) {
                propName = field.name();
                propValue = source.get(propName);

                if (propValue != null) {
                    m.put(propName, propValue);
                }
            }

            return (T) m;
        } else if (type.isCollection()) {
            @SuppressWarnings("rawtypes")
            final Collection<Object> c = type.isCollection() ? N.newCollection((Class<Collection>) targetClass) : new ArrayList<>();
            Object propValue = null;

            for (final Field field : source.getSchema().getFields()) {
                propValue = source.get(field.pos());

                if (propValue != null) {
                    c.add(propValue);
                }
            }

            return (T) c;
        } else {
            throw new IllegalArgumentException("Unsupported type: " + type.name());
        }
    }
}
