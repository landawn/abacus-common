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
import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.apache.avro.file.DataFileStream;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericData.Record;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericEnumSymbol;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.specific.SpecificRecord;
import org.apache.avro.util.ClassSecurityValidator;

import com.landawn.abacus.annotation.SuppressFBWarnings;
import com.landawn.abacus.exception.ParsingException;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.parser.ParserUtil.BeanInfo;
import com.landawn.abacus.parser.ParserUtil.PropInfo;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.Beans;
import com.landawn.abacus.util.ByteArrayOutputStream;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.cs;

/**
 * Parser implementation for Apache Avro format serialization and deserialization.
 *
 * <p>This parser handles Avro data serialization with special encoding rules:</p>
 * <ul>
 *   <li>Content is Base64 encoded when output is a String (via {@link #serialize(Object, AvroSerConfig)})</li>
 *   <li>Content is NOT Base64 encoded when output is File or OutputStream (raw binary, for performance)</li>
 *   <li>Input must be Base64 encoded when source is a String</li>
 *   <li>Input must NOT be Base64 encoded when source is File or InputStream</li>
 *   <li>Writer output and Reader input are not supported and throw {@link UnsupportedOperationException}</li>
 * </ul>
 *
 * <p>The reason for not encoding content with Base64 for File/OutputStream is to provide
 * a higher performance solution for binary data handling.</p>
 *
 * <p>Collection targets use the requested collection implementation and recursively convert
 * declared/configured element types, including nested collections and maps. Untyped Object values
 * retain their Avro representations. An array datum supplies the collection contents (only the first
 * array datum is read); record datums supply successive collection elements. A configured element
 * type overrides the outer declared element type. Scalar conversions use the target Type's range checks.
 * A schema field without a matching bean property is skipped while the inherited
 * {@code ignoreUnmatchedProperty} option is enabled (the default) and rejected with a
 * {@link ParsingException} otherwise.</p>
 *
 * <p>Serialization applies the same Type range checks to {@code Number} values written to {@code int} and
 * {@code long} fields, array items and map values: {@code Integer}/{@code Short}/{@code Byte} (and {@code Long}
 * for a {@code long} field) are written as they are, any other {@code Number} is converted through the target
 * Type, so a {@code Long} of 5,000,000,000 for an {@code int} field fails with {@code ArithmeticException} and a
 * floating-point or decimal value such as {@code 3.7} fails with {@code NumberFormatException} instead of being
 * silently truncated. {@code double} values written to a {@code float} field are narrowed without a range check
 * (an out-of-range value becomes infinity), as on the read side. Non-{@code Number} values such as a
 * {@code String} for an {@code int} field are still rejected by Avro. Java {@code Enum} constants and symbol
 * names are converted to Avro enum symbols for {@code enum} fields; an undefined symbol fails with
 * {@code IllegalArgumentException}. Bean properties and map keys that are not fields of the record schema are
 * skipped.</p>
 *
 * <p>For serialization with a supplied schema, a collection under a record schema writes zero or
 * more record datums; an array schema writes exactly one array datum, including for an empty collection.
 * Nested beans/maps/collections are converted using their field, value and element schemas. Nullable
 * unions with one non-null branch use that branch; other unions use Avro's datum-based resolution.
 * An explicit array schema is honored for SpecificRecord collections; otherwise their record schema
 * is inferred as before. Without an explicit array schema, inferred SpecificRecord collections must
 * contain one record class and no null elements.</p>
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
 * AvroSerConfig config = new AvroSerConfig().setSchema(schema);
 *
 * Map<String, Object> data = Map.of("name", "John", "age", 30);
 * parser.serialize(data, config, outputStream);
 * }</pre>
 *
 * @see AvroSerConfig
 * @see AvroDeserConfig
 * @see ParserFactory#createAvroParser()
 */
public final class AvroParser extends AbstractParser<AvroSerConfig, AvroDeserConfig> {

    private static final Object AVRO_CLASS_TRUST_LOCK = new Object();

    /**
     * Constructs a new AvroParser instance.
     * This parser can be used to serialize and deserialize objects to/from Apache Avro format.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * AvroParser parser = new AvroParser();  // creates a ready-to-use parser
     *
     * Schema schema = new Schema.Parser().parse(schemaJson);
     * AvroSerConfig config = new AvroSerConfig().setSchema(schema);
     * Map<String, Object> data = Map.of("name", "John", "age", 30);
     * String encoded = parser.serialize(data, config);  // returns a Base64 encoded string
     * }</pre>
     *
     */
    public AvroParser() {
        // Default constructor
    }

    /**
     * Serializes an object to a Base64 encoded string representation.
     *
     * <p>This method converts the object to Avro binary format, then encodes the result
     * as a Base64 string suitable for text-based transmission. The output is Base64 encoded
     * to make it suitable for text-based storage and transmission.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * User user = new User("John", 30);
     * AvroSerConfig config = new AvroSerConfig()
     *     .setSchema(User.getClassSchema());
     * String base64Encoded = parser.serialize(user, config);
     * }</pre>
     *
     * @param obj the object to serialize (may be {@code null}; serializes nothing in that case)
     * @param config the serialization configuration to use (must contain a schema unless {@code obj} is a
     *        {@code SpecificRecord} or a collection of {@code SpecificRecord}s)
     * @return the Base64 encoded string representation of the serialized object, or an empty string if
     *         {@code obj} is {@code null}
     * @throws IllegalArgumentException if a required schema is missing, the source type is unsupported, an
     *         inferred {@code SpecificRecord} collection contains nulls or more than one record class, or an
     *         enum value is not one of the schema's symbols.
     * @throws ArithmeticException if a {@code Number} does not fit an {@code int}/{@code long} field
     * @throws NumberFormatException if a floating-point or decimal {@code Number} is written to an {@code int}/{@code long} field
     */
    @Override
    public String serialize(final Object obj, final AvroSerConfig config) throws IllegalArgumentException, ArithmeticException, NumberFormatException {
        final ByteArrayOutputStream os = Objectory.createByteArrayOutputStream();

        try {
            serialize(obj, config, os);

            return Strings.base64Encode(os.toByteArray());
        } finally {
            Objectory.recycle(os);
        }
    }

    /**
     * Serializes an object to a file with raw binary content (NOT Base64 encoded).
     *
     * <p>The file will be created if it doesn't exist, or overwritten if it does.
     * Missing parent directories are created automatically. The content
     * is written in raw binary format without Base64 encoding for optimal performance.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<User> users = List.of(
     *     new User("John", 30),
     *     new User("Jane", 25)
     * );
     * // SpecificRecord collections carry their own schema, so no config is required.
     * parser.serialize(users, null, new File("users.avro"));
     * }</pre>
     *
     * <p><b>Implementation note:</b> The source type and required schema are validated before the
     * destination is opened, so these validation failures do not truncate an existing file.</p>
     *
     * @param obj the object to serialize (may be {@code null}; serializes nothing in that case)
     * @param config the serialization configuration to use (may be {@code null} for default behavior)
     * @param output the output file to write to (must not be {@code null})
     * @throws IllegalArgumentException if a required schema is missing, the source type is unsupported, an
     *         inferred {@code SpecificRecord} collection contains nulls or more than one record class, or an
     *         enum value is not one of the schema's symbols.
     * @throws NullPointerException if {@code output} is {@code null}.
     * @throws UncheckedIOException if creating or opening the output file, writing the Avro container, or flushing the output fails.
     * @throws ArithmeticException if a {@code Number} does not fit an {@code int}/{@code long} field
     * @throws NumberFormatException if a floating-point or decimal {@code Number} is written to an {@code int}/{@code long} field
     */
    @Override
    public void serialize(final Object obj, final AvroSerConfig config, final File output)
            throws IllegalArgumentException, NullPointerException, UncheckedIOException, ArithmeticException, NumberFormatException {
        validateSerialization(obj, config);

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
     * <p>This method supports:</p>
     * <ul>
     *   <li>SpecificRecord instances (Avro generated classes)</li>
     *   <li>Collections of SpecificRecord</li>
     *   <li>GenericRecord instances</li>
     *   <li>Regular Java beans and Maps (requires schema in config)</li>
     * </ul>
     *
     * <p>For beans and Maps, properties or keys that are not fields of the record schema are skipped.
     * {@code Number} values for {@code int}/{@code long} fields are range-checked (see the class
     * documentation) and Java {@code Enum} constants or symbol names are converted for {@code enum} fields.</p>
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
     * AvroSerConfig config = new AvroSerConfig()
     *     .setSchema(personSchema);
     * parser.serialize(person, config, outputStream);
     * }</pre>
     *
     * @param obj the object to serialize (may be {@code null}; serializes nothing in that case)
     * @param config the serialization configuration to use (may be {@code null} for default behavior)
     * @param output the output stream to write to (must not be {@code null})
     * @throws IllegalArgumentException if a required schema is missing, the source type is unsupported, an
     *         inferred {@code SpecificRecord} collection contains nulls or more than one record class, or an
     *         enum value is not one of the schema's symbols.
     * @throws NullPointerException if {@code obj} is non-null and {@code output} is {@code null}.
     * @throws UncheckedIOException if writing the Avro container header or records, or flushing the output fails.
     * @throws ArithmeticException if a {@code Number} does not fit an {@code int}/{@code long} field
     * @throws NumberFormatException if a floating-point or decimal {@code Number} is written to an {@code int}/{@code long} field
     */
    @SuppressFBWarnings
    @Override
    public void serialize(final Object obj, final AvroSerConfig config, final OutputStream output)
            throws IllegalArgumentException, NullPointerException, UncheckedIOException, ArithmeticException, NumberFormatException {
        validateSerialization(obj, config);

        if (obj == null) {
            return;
        }

        final OutputStream targetOutput = nonClosingOutputStream(output);
        final Type<Object> type = Type.of(obj.getClass());

        if (obj instanceof SpecificRecord specificRecord) {
            final SpecificDatumWriter<SpecificRecord> datumWriter = new SpecificDatumWriter<>((Class<SpecificRecord>) specificRecord.getClass());

            try (final DataFileWriter<SpecificRecord> dataFileWriter = new DataFileWriter<>(datumWriter).create(specificRecord.getSchema(), targetOutput)) {
                dataFileWriter.append(specificRecord);
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }
        } else if (type.isCollection() && !hasArraySchema(config) && ((Collection<Object>) obj).size() > 0
                && ((Collection<Object>) obj).iterator().next() instanceof SpecificRecord) {
            final Collection<SpecificRecord> c = (Collection<SpecificRecord>) obj;
            final SpecificRecord specificRecord = c.iterator().next();
            final DatumWriter<SpecificRecord> datumWriter = new SpecificDatumWriter<>((Class<SpecificRecord>) specificRecord.getClass());

            try (final DataFileWriter<SpecificRecord> dataFileWriter = new DataFileWriter<>(datumWriter).create(specificRecord.getSchema(), targetOutput)) {
                for (final SpecificRecord e : c) {
                    dataFileWriter.append(e);
                }
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }
        } else {
            final Schema schema = config.getSchema();
            final DatumWriter<Object> datumWriter = new GenericDatumWriter<>(schema);

            try (final DataFileWriter<Object> dataFileWriter = new DataFileWriter<>(datumWriter).create(schema, targetOutput)) {
                // The file schema fixes the datum shape even when there are no elements to inspect.
                if (obj instanceof Collection<?> collection && schema.getType() == Schema.Type.RECORD) {
                    for (final Object element : collection) {
                        dataFileWriter.append(toAvroDatum(element, schema));
                    }
                } else {
                    dataFileWriter.append(toAvroDatum(obj, schema));
                }
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    /**
     * Validates failures that can be detected without writing an Avro container header. Keeping
     * this check separate allows file overloads to preserve an existing destination on invalid input.
     *
     * @param obj the source object, or {@code null}
     * @param config the serialization configuration, or {@code null}
     * @throws IllegalArgumentException if a required schema is missing, the source type is unsupported, or an
     *         inferred {@code SpecificRecord} collection is heterogeneous.
     */
    private static void validateSerialization(final Object obj, final AvroSerConfig config) throws IllegalArgumentException {
        if (obj == null || obj instanceof SpecificRecord) {
            return;
        }

        final Type<Object> type = Type.of(obj.getClass());

        if (type.isCollection() && !hasArraySchema(config) && !((Collection<?>) obj).isEmpty()
                && ((Collection<?>) obj).iterator().next() instanceof SpecificRecord first) {
            for (final Object element : (Collection<?>) obj) {
                if (!(element instanceof SpecificRecord) || element.getClass() != first.getClass()) {
                    throw new IllegalArgumentException("A SpecificRecord collection must contain only one record type");
                }
            }

            return;
        }

        if (config == null || config.getSchema() == null) {
            throw new IllegalArgumentException("Schema is not specified");
        }

        if (!(obj instanceof GenericRecord) && !type.isBean() && !type.isMap() && !type.isCollection()) {
            throw new IllegalArgumentException("Unsupported type: " + type.name());
        }
    }

    private static boolean hasArraySchema(final AvroSerConfig config) {
        return config != null && config.getSchema() != null && config.getSchema().getType() == Schema.Type.ARRAY;
    }

    private static final Type<Integer> INT_TYPE = Type.of(int.class);

    private static final Type<Long> LONG_TYPE = Type.of(long.class);

    /**
     * Converts nested Java containers according to their datum schema, range-checks {@code Number} values for
     * {@code int}/{@code long} schemas with the same {@link Type} conversion the read side uses, and converts
     * enum values to Avro symbols. Other scalar validation is left to Avro.
     *
     * @param value the Java value, or {@code null}
     * @param schema the schema for this value
     * @return a datum with schema-shaped containers, or {@code null} for a null value
     * @throws ArithmeticException if a {@code Number} does not fit an {@code int}/{@code long} schema
     * @throws NumberFormatException if a floating-point or decimal {@code Number} is given for an {@code int}/{@code long} schema
     * @throws IllegalArgumentException if an enum value is not one of the schema's symbols
     */
    private Object toAvroDatum(final Object value, final Schema schema) throws ArithmeticException, NumberFormatException, IllegalArgumentException {
        if (value == null) {
            return null;
        }
        switch (schema.getType()) {
            case INT:
                // GenericDatumWriter narrows with Number.intValue() and never range-checks, so a Long/Double/
                // BigDecimal that does not fit was silently truncated. Widening wrappers pass through untouched;
                // any other Number takes the read side's Type conversion. Non-Numbers (String "5") stay with Avro.
                return value instanceof Integer || value instanceof Short || value instanceof Byte || !(value instanceof Number) ? value
                        : INT_TYPE.valueOf(value);
            case LONG:
                return value instanceof Long || value instanceof Integer || value instanceof Short || value instanceof Byte || !(value instanceof Number)
                        ? value
                        : LONG_TYPE.valueOf(value);
            case ENUM:
                // GenericDatumWriter accepts only GenericEnumSymbol (Java enums are a SpecificDatumWriter feature),
                // while the read side already maps a symbol back to a Java enum through Type.valueOf.
                return value instanceof GenericEnumSymbol ? value : toEnumSymbol(value, schema);
            case RECORD:
                return value instanceof GenericRecord || value instanceof SpecificRecord ? value : toGenericRecord(value, schema);
            case ARRAY:
                if (value instanceof Collection<?> collection) {
                    final Collection<Object> result = new ArrayList<>(collection.size());
                    for (final Object element : collection) {
                        result.add(toAvroDatum(element, schema.getElementType()));
                    }
                    return result;
                }
                return value;
            case MAP:
                if (value instanceof Map<?, ?> map) {
                    final Map<Object, Object> result = new LinkedHashMap<>();
                    for (final Map.Entry<?, ?> entry : map.entrySet()) {
                        result.put(entry.getKey(), toAvroDatum(entry.getValue(), schema.getValueType()));
                    }
                    return result;
                }
                return value;
            case UNION:
                Schema nonNullBranch = null;
                int nonNullCount = 0;
                for (final Schema branch : schema.getTypes()) {
                    if (branch.getType() != Schema.Type.NULL) {
                        nonNullBranch = branch;
                        nonNullCount++;
                    }
                }
                // A nullable record may be represented by a Java Map before conversion. With more
                // than one non-null branch, preserve Avro's resolution rather than guessing a record.
                return toAvroDatum(value, nonNullCount == 1 ? nonNullBranch : schema.getTypes().get(GenericData.get().resolveUnion(schema, value)));
            default:
                return value;
        }
    }

    /**
     * Converts a Java {@code Enum} constant or a symbol name to the Avro symbol of {@code schema}.
     *
     * @param value the enum constant or symbol name (not {@code null})
     * @param schema the enum schema
     * @return the Avro enum symbol
     * @throws IllegalArgumentException if the symbol is not defined by {@code schema}
     */
    private static GenericData.EnumSymbol toEnumSymbol(final Object value, final Schema schema) throws IllegalArgumentException {
        final String symbol = value instanceof Enum<?> e ? e.name() : value.toString();

        // Schema.getEnumOrdinal unboxes a null for an unknown symbol, which would surface as an NPE from Avro.
        if (!schema.hasEnumSymbol(symbol)) {
            throw new IllegalArgumentException("Symbol '" + symbol + "' is not defined in enum " + schema.getFullName() + ": " + schema.getEnumSymbols());
        }

        return new GenericData.EnumSymbol(schema, symbol);
    }

    /**
     * Serialization to Writer is not supported for Avro format.
     * Use {@link #serialize(Object, AvroSerConfig)} to get Base64 encoded string
     * or {@link #serialize(Object, AvroSerConfig, OutputStream)} for binary output.
     *
     * @param obj the object to serialize
     * @param config the serialization configuration
     * @param output the writer (not supported)
     * @throws UnsupportedOperationException always thrown as this operation is not supported
     * @deprecated This method is deprecated and will always throw UnsupportedOperationException.
     */
    @Deprecated
    @Override
    public void serialize(final Object obj, final AvroSerConfig config, final Writer output) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Converts a Java object to an Avro GenericRecord.
     * Supports beans, named map fields, and positional collection fields, recursively converting
     * each supplied field with its schema. Bean properties and map keys that are not fields of the
     * schema are skipped. Existing GenericRecord instances are retained.
     *
     * @param obj the object to convert (may be a bean, map, collection, or GenericRecord)
     * @param schema the Avro schema defining the structure
     * @return GenericRecord representation of the object
     * @throws IllegalArgumentException if the object type is not supported.
     */
    private GenericRecord toGenericRecord(final Object obj, final Schema schema) throws IllegalArgumentException {
        if (obj instanceof GenericRecord genericrecord) {
            return genericrecord;
        }

        final Class<?> cls = obj.getClass();
        final Type<Object> type = Type.of(cls);

        if (type.isBean()) {
            return toGenericRecord(Beans.beanToMap(obj), schema);
        } else if (type.isMap()) {
            final Map<String, Object> m = (Map<String, Object>) obj;
            final Record localRecord = new Record(schema);

            for (final Map.Entry<String, Object> entry : m.entrySet()) {
                final Field field = schema.getField(entry.getKey());

                // Record.put(String, ..) rejects any name outside the schema, so a property the schema does
                // not know (a derived getter, a subclass field, an extra map key) is skipped rather than fatal.
                if (field != null) {
                    localRecord.put(entry.getKey(), toAvroDatum(entry.getValue(), field.schema()));
                }
            }

            return localRecord;
        } else if (type.isCollection()) {
            final Collection<Object> c = (Collection<Object>) obj;
            final Record localRecord = new Record(schema);

            int index = 0;
            for (final Object e : c) {
                localRecord.put(index, toAvroDatum(e, schema.getFields().get(index).schema()));
                index++;
            }

            return localRecord;
        } else {
            throw new IllegalArgumentException("Unsupported type: " + type.name());
        }
    }

    /**
     * Deserializes an object from a Base64 encoded string representation.
     *
     * <p>This method decodes the Base64 string and uses Avro deserialization to convert
     * the binary data back to an object. The input string must be Base64 encoded Avro binary data.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * AvroParser parser = new AvroParser();
     * User original = new User("John", 30);
     * AvroSerConfig serConfig = new AvroSerConfig()
     *     .setSchema(User.getClassSchema());
     * String base64Data = parser.serialize(original, serConfig);
     * AvroDeserConfig config = AvroDeserConfig.create()
     *     .setSchema(User.getClassSchema());
     * User user = parser.deserialize(base64Data, config, User.class);
     * }</pre>
     *
     * @param <T> the target type
     * @param source the Base64 encoded string to deserialize from (must not be {@code null}); an empty string,
     *        which is what {@link #serialize(Object, AvroSerConfig)} returns for a {@code null} object, yields
     *        the default value of the target type
     * @param config the deserialization configuration to use (may be {@code null} for default behavior)
     * @param targetType the type of the object to create (must not be {@code null})
     * @return the deserialized object instance, or the default value of {@code targetType} ({@code null} for
     *         beans, maps and collections) if {@code source} is empty
     * @throws IllegalArgumentException if {@code source} or {@code targetType} is {@code null}, or schema is not specified for
     *         non-SpecificRecord types.
     * @throws UncheckedIOException if the nonempty Base64-decoded bytes have an invalid or truncated Avro container header or record
     * @throws ParsingException if a schema field has no matching bean property and
     *         {@code ignoreUnmatchedProperty} is disabled
     */
    @Override
    public <T> T deserialize(String source, AvroDeserConfig config, Type<? extends T> targetType)
            throws IllegalArgumentException, UncheckedIOException, ParsingException {
        N.checkArgNotNull(targetType, cs.targetType);
        N.checkArgNotNull(source, cs.source);

        if (source.isEmpty()) {
            // serialize(null, ..) yields "", which is not an Avro container; mirror the JSON/JAXB parsers and
            // hand back the target's default instead of an UncheckedIOException("Not an Avro data file").
            return targetType.defaultValue();
        }

        return deserialize(new ByteArrayInputStream(Strings.base64Decode(source)), config, targetType);
    }

    /**
     * Deserializes an object from a Base64 encoded string representation.
     *
     * <p>This method decodes the Base64 string and uses Avro deserialization to convert
     * the binary data back to an object. The input string must be Base64 encoded Avro binary data.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * AvroParser parser = new AvroParser();
     * User original = new User("John", 30);
     * AvroSerConfig serConfig = new AvroSerConfig()
     *     .setSchema(User.getClassSchema());
     * String base64Data = parser.serialize(original, serConfig);
     * AvroDeserConfig config = AvroDeserConfig.create()
     *     .setSchema(User.getClassSchema());
     * User user = parser.deserialize(base64Data, config, User.class);
     * }</pre>
     *
     * @param <T> the target type
     * @param source the Base64 encoded string to deserialize from (must not be {@code null}); an empty string,
     *        which is what {@link #serialize(Object, AvroSerConfig)} returns for a {@code null} object, yields
     *        the default value of the target class
     * @param config the deserialization configuration to use (may be {@code null} for default behavior)
     * @param targetClass the class of the object to create (must not be {@code null})
     * @return the deserialized object instance, or the default value of {@code targetClass} ({@code null} for
     *         beans, maps and collections) if {@code source} is empty
     * @throws IllegalArgumentException if {@code source} is {@code null}, or if schema is not specified for
     *         non-SpecificRecord types.
     * @throws UncheckedIOException if the nonempty Base64-decoded bytes have an invalid or truncated Avro container header or record
     * @throws ParsingException if a schema field has no matching bean property and
     *         {@code ignoreUnmatchedProperty} is disabled
     */
    @Override
    public <T> T deserialize(final String source, final AvroDeserConfig config, final Class<? extends T> targetClass)
            throws IllegalArgumentException, UncheckedIOException, ParsingException {
        N.checkArgNotNull(source, cs.source);

        if (source.isEmpty()) {
            return N.defaultValueOf(targetClass);
        }

        return deserialize(new ByteArrayInputStream(Strings.base64Decode(source)), config, targetClass);
    }

    /**
     * Deserializes an object from a file containing raw binary data (NOT Base64 encoded).
     *
     * <p>This method reads binary Avro data from the specified file and deserializes it
     * to an object instance. The file should contain raw binary Avro data (not Base64 encoded).</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File avroFile = new File("users.avro");
     * List<User> users = parser.deserialize(avroFile, null, List.class);
     * }</pre>
     *
     * @param <T> the target type
     * @param source the source file to read from (must not be {@code null} and must exist)
     * @param config the deserialization configuration to use (may be {@code null} for default behavior)
     * @param targetType the type of the object to create (must not be {@code null})
     * @return the deserialized object instance
     * @throws IllegalArgumentException if {@code source} or {@code targetType} is {@code null}, or schema is not specified for non-SpecificRecord types.
     * @throws UncheckedIOException if opening, reading or closing {@code source} fails, including a missing file or invalid or truncated
     *         Avro container data
     * @throws ParsingException if a schema field has no matching bean property and
     *         {@code ignoreUnmatchedProperty} is disabled
     */
    @Override
    public <T> T deserialize(File source, AvroDeserConfig config, Type<? extends T> targetType)
            throws IllegalArgumentException, UncheckedIOException, ParsingException {
        N.checkArgNotNull(targetType, cs.targetType);
        N.checkArgNotNull(source, cs.source);

        InputStream is = null;

        try {
            is = IOUtil.newFileInputStream(source);

            return deserialize(is, config, targetType);
        } finally {
            IOUtil.close(is);
        }
    }

    /**
     * Deserializes an object from a file containing raw binary data (NOT Base64 encoded).
     *
     * <p>This method reads binary Avro data from the specified file and deserializes it
     * to an object instance. The file should contain raw binary Avro data (not Base64 encoded).</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File avroFile = new File("users.avro");
     * List<User> users = parser.deserialize(avroFile, null, List.class);
     * }</pre>
     *
     * @param <T> the target type
     * @param source the source file to read from (must not be {@code null} and must exist)
     * @param config the deserialization configuration to use (may be {@code null} for default behavior)
     * @param targetClass the class of the object to create (must not be {@code null})
     * @return the deserialized object instance
     * @throws IllegalArgumentException if schema is not specified for non-SpecificRecord types.
     * @throws UncheckedIOException if opening, reading or closing {@code source} fails, including a missing file or invalid or truncated
     *         Avro container data
     * @throws ParsingException if a schema field has no matching bean property and
     *         {@code ignoreUnmatchedProperty} is disabled
     */
    @Override
    public <T> T deserialize(final File source, final AvroDeserConfig config, final Class<? extends T> targetClass)
            throws IllegalArgumentException, UncheckedIOException, ParsingException {
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
     * The stream should contain raw binary Avro data (not Base64 encoded).</p>
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
     * AvroDeserConfig config = AvroDeserConfig.create()
     *     .setSchema(schema)
     *     .setElementType(Person.class);
     * List<Person> people = parser.deserialize(inputStream, config, List.class);
     * }</pre>
     *
     * <p>When the target is a bean, a schema field without a matching bean property is skipped while the
     * inherited {@code ignoreUnmatchedProperty} option is enabled (the default) and rejected with a
     * {@link ParsingException} otherwise.</p>
     *
     * @param <T> the target type
     * @param source the input stream to read from (must not be {@code null}); an empty stream is not a
     *        valid Avro container
     * @param config the deserialization configuration to use (may be {@code null} for default behavior)
     * @param targetType the type of the object to create (must not be {@code null})
     * @return the deserialized object instance
     * @throws IllegalArgumentException if {@code source} or {@code targetType} is {@code null}, or schema is not specified for non-SpecificRecord
     *         types.
     * @throws UncheckedIOException if reading the Avro container header or records from {@code source} fails, including an empty,
     *         invalid or truncated container
     * @throws ParsingException if a schema field has no matching bean property and {@code ignoreUnmatchedProperty} is disabled
     */
    @Override
    public <T> T deserialize(InputStream source, AvroDeserConfig config, Type<? extends T> targetType)
            throws IllegalArgumentException, UncheckedIOException, ParsingException {
        N.checkArgNotNull(targetType, cs.targetType);
        N.checkArgNotNull(source, cs.source);

        final InputStream targetInput = nonClosingInputStream(source);
        final Class<? extends T> targetClass = targetType.javaType();
        final boolean ignoreUnmatchedProperty = config == null || config.isIgnoreUnmatchedProperty();
        final Type<Object> eleType = config == null || config.getElementType() == null
                ? (targetType.isCollection() && !targetType.elementType().isObject() ? (Type<Object>) targetType.elementType() : null)
                : config.getElementType();

        if (SpecificRecord.class.isAssignableFrom(targetClass)) {
            trustSpecificRecordClass(targetClass);
            final SpecificDatumReader<T> datumReader = new SpecificDatumReader<>((Class<T>) targetClass);
            T bean = null;

            try (DataFileStream<T> dataFileReader = new DataFileStream<>(targetInput, datumReader)) {
                if (dataFileReader.hasNext()) {
                    bean = dataFileReader.next();
                }
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }

            return bean;
        } else if (targetType.isCollection() && (eleType != null && SpecificRecord.class.isAssignableFrom(eleType.javaType()))) {
            final Class<Object> eleClass = eleType.javaType();
            @SuppressWarnings("rawtypes")
            final Collection<Object> c = N.newCollection((Class<Collection>) targetClass);
            trustSpecificRecordClass(eleClass);
            final SpecificDatumReader<Object> datumReader = new SpecificDatumReader<>(eleClass);

            try (DataFileStream<Object> dataFileReader = new DataFileStream<>(targetInput, datumReader)) {
                if (dataFileReader.getSchema().getType() == Schema.Type.ARRAY) {
                    // The class-based reader starts with a record schema; wrap it for an array datum.
                    datumReader.setExpected(Schema.createArray(datumReader.getExpected()));
                    return dataFileReader.hasNext()
                            ? (T) convertAvroCollection((Collection<?>) dataFileReader.next(), targetType, eleType, ignoreUnmatchedProperty)
                            : null;
                }
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

            try (DataFileStream<Object> dataFileReader = new DataFileStream<>(targetInput, datumReader)) {
                if (GenericRecord.class.isAssignableFrom(targetClass)) {
                    return (T) (dataFileReader.hasNext() ? dataFileReader.next() : null);
                } else if (targetType.isBean() || targetType.isMap()) {
                    return dataFileReader.hasNext() ? fromGenericRecord((GenericRecord) dataFileReader.next(), targetType, ignoreUnmatchedProperty) : null;
                } else if (targetType.isCollection()) {
                    // An array is one datum, whereas record collections are a sequence of datums.
                    // Inspect the datum before dispatching on its requested element type.
                    final boolean arraySchema = dataFileReader.getSchema().getType() == Schema.Type.ARRAY;
                    if (!dataFileReader.hasNext()) {
                        return arraySchema ? null : (T) newAvroCollection(targetType);
                    }
                    final Object first = dataFileReader.next();
                    if (arraySchema || first instanceof Collection) {
                        return (T) convertAvroCollection((Collection<?>) first, targetType, eleType, ignoreUnmatchedProperty);
                    }

                    final Collection<Object> result = newAvroCollection(targetType);
                    result.add(convertAvroValue(first, eleType, ignoreUnmatchedProperty));
                    while (dataFileReader.hasNext()) {
                        result.add(convertAvroValue(dataFileReader.next(), eleType, ignoreUnmatchedProperty));
                    }
                    return (T) result;
                } else {
                    throw new IllegalArgumentException("Unsupported type: " + targetType.name());
                }
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    /**
     * Avro 1.12.2+ refuses to instantiate SpecificRecord types that are not on its trust list.
     * Trust the class the caller asked to deserialize, composing with any existing global predicate.
     */
    private static void trustSpecificRecordClass(final Class<?> cls) {
        synchronized (AVRO_CLASS_TRUST_LOCK) {
            final ClassSecurityValidator.ClassSecurityPredicate current = ClassSecurityValidator.getGlobal();
            if (current != null && current.isTrusted(cls)) {
                return;
            }
            final ClassSecurityValidator.ClassSecurityPredicate added = ClassSecurityValidator.builder().add(cls).build();
            ClassSecurityValidator.setGlobal(current == null ? added : ClassSecurityValidator.composite(current, added));
        }
    }

    private static OutputStream nonClosingOutputStream(final OutputStream output) {
        return new FilterOutputStream(output) {
            @Override
            public void write(final byte[] b, final int off, final int len) throws IOException {
                // FilterOutputStream's default bulk write loops byte-by-byte through write(int);
                // delegate directly to avoid one syscall per byte for every Avro block.
                out.write(b, off, len);
            }

            @Override
            public void close() throws IOException {
                flush();
            }
        };
    }

    /**
     * Returns a view of {@code input} whose {@code close()} does nothing, because Avro's
     * {@code DataFileStream} closes its source at end of file although the parser contract leaves the
     * stream to the caller.
     *
     * @param input the caller's stream
     * @return a stream over {@code input} that ignores {@code close()}
     */
    private static InputStream nonClosingInputStream(final InputStream input) {
        return new FilterInputStream(input) {
            @Override
            public void close() {
                // no-op: caller owns the source stream lifecycle
            }
        };
    }

    /**
     * Deserializes an object from an input stream containing raw binary data (NOT Base64 encoded).
     *
     * <p>The stream is not closed after reading, allowing the caller to manage stream lifecycle.
     * The stream should contain raw binary Avro data (not Base64 encoded).</p>
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
     * AvroDeserConfig config = AvroDeserConfig.create()
     *     .setSchema(schema)
     *     .setElementType(Person.class);
     * List<Person> people = parser.deserialize(inputStream, config, List.class);
     * }</pre>
     *
     * <p>When the target is a bean, a schema field without a matching bean property is skipped while the
     * inherited {@code ignoreUnmatchedProperty} option is enabled (the default) and rejected with a
     * {@link ParsingException} otherwise.</p>
     *
     * @param <T> the target type
     * @param source the input stream to read from (must not be {@code null}); an empty stream is not a
     *        valid Avro container
     * @param config the deserialization configuration to use (may be {@code null} for default behavior)
     * @param targetClass the class of the object to create (must not be {@code null})
     * @return the deserialized object instance
     * @throws IllegalArgumentException if schema is not specified for non-SpecificRecord types.
     * @throws UncheckedIOException if reading the Avro container header or records from {@code source} fails, including an empty,
     *         invalid or truncated container
     * @throws ParsingException if a schema field has no matching bean property and
     *         {@code ignoreUnmatchedProperty} is disabled
     */
    @Override
    public <T> T deserialize(final InputStream source, final AvroDeserConfig config, final Class<? extends T> targetClass)
            throws IllegalArgumentException, UncheckedIOException, ParsingException {
        return deserialize(source, config, Type.of(targetClass));
    }

    /**
     * Deserialization from Reader is not supported for Avro format.
     * Use {@link #deserialize(String, AvroDeserConfig, Type)} for Base64 encoded input
     * or {@link #deserialize(InputStream, AvroDeserConfig, Type)} for binary input.
     *
     * @param <T> the target type
     * @param source the reader (not supported)
     * @param config the deserialization configuration
     * @param targetType the target type
     * @return never returns
     * @throws UnsupportedOperationException always thrown as this operation is not supported
     * @deprecated This method is deprecated and will always throw UnsupportedOperationException.
     */
    @Deprecated
    @Override
    public <T> T deserialize(Reader source, AvroDeserConfig config, Type<? extends T> targetType) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Deserialization from Reader is not supported for Avro format.
     * Use {@link #deserialize(String, AvroDeserConfig, Class)} for Base64 encoded input
     * or {@link #deserialize(InputStream, AvroDeserConfig, Class)} for binary input.
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
    public <T> T deserialize(final Reader source, final AvroDeserConfig config, final Class<? extends T> targetClass) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static Collection<Object> newAvroCollection(final Type<?> type) {
        return N.newCollection((Class<Collection>) type.javaType());
    }

    private Collection<Object> convertAvroCollection(final Collection<?> source, final Type<?> targetType, final Type<?> elementType,
            final boolean ignoreUnmatchedProperty) {
        if (source == null) {
            return null;
        }
        final Collection<Object> result = newAvroCollection(targetType);
        for (final Object value : source) {
            result.add(convertAvroValue(value, elementType, ignoreUnmatchedProperty));
        }
        return result;
    }

    // Container assignability alone is insufficient: a List<String> may still hold Avro Utf8 values.
    /**
     * @throws IllegalArgumentException if a non-null value required to become a collection is not an Avro array or cannot be converted to its target type
     * @throws ParsingException if a record contains an unmatched property and unmatched properties are not ignored
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private Object convertAvroValue(final Object value, final Type<?> targetType, final boolean ignoreUnmatchedProperty)
            throws IllegalArgumentException, ParsingException {
        if (value == null || targetType == null || targetType.isObject()) {
            return value;
        }
        if (targetType.isCollection()) {
            if (!(value instanceof Collection<?> collection)) {
                throw new IllegalArgumentException("Expected an Avro array for collection type: " + targetType.name());
            }
            return convertAvroCollection(collection, targetType, targetType.elementType(), ignoreUnmatchedProperty);
        }
        if (value instanceof GenericRecord record) {
            return fromGenericRecord(record, targetType, ignoreUnmatchedProperty);
        }
        if (targetType.isMap() && value instanceof Map<?, ?> map) {
            final Map<Object, Object> result = N.newMap((Class<Map>) targetType.javaType());
            final Type<?> keyType = targetType.parameterTypes().get(0);
            final Type<?> valueType = targetType.parameterTypes().get(1);
            for (final Map.Entry<?, ?> entry : map.entrySet()) {
                result.put(convertAvroValue(entry.getKey(), keyType, ignoreUnmatchedProperty),
                        convertAvroValue(entry.getValue(), valueType, ignoreUnmatchedProperty));
            }
            return result;
        }
        return targetType.javaType().isInstance(value) ? value : targetType.valueOf(value);
    }

    /**
     * Converts an Avro record using the complete requested type, preserving explicit GenericRecord
     * targets and recursively converting typed bean properties or map values.
     *
     * @param <T> the result type
     * @param source the Avro record, or {@code null}
     * @param targetType the complete requested Java type
     * @param ignoreUnmatchedProperty whether a schema field without a matching bean property is skipped
     *        ({@code true}) or rejected ({@code false})
     * @return the converted value, or {@code null} for a null record
     * @throws IllegalArgumentException if the record cannot be converted to the target type
     * @throws ParsingException if a schema field has no matching bean property and
     *         {@code ignoreUnmatchedProperty} is {@code false}
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private <T> T fromGenericRecord(final GenericRecord source, final Type<? extends T> targetType, final boolean ignoreUnmatchedProperty)
            throws IllegalArgumentException, ParsingException {
        if (source == null || targetType.javaType().isAssignableFrom(source.getClass())) {
            return (T) source;
        }

        if (targetType.isBean()) {
            final BeanInfo beanInfo = ParserUtil.getBeanInfo(targetType.reflectType());
            final Object result = beanInfo.createBeanResult();
            for (final Field field : source.getSchema().getFields()) {
                final PropInfo property = beanInfo.getPropInfo(field.name());

                // A schema that evolved by adding a field is read into the older bean: honour the configured
                // policy here, because BeanInfo.setPropValue(obj, name, value) fails for an unknown name anyway.
                if (property == null) {
                    if (!ignoreUnmatchedProperty) {
                        throw new ParsingException("Unknown property: " + field.name() + " in class: " + targetType.name());
                    }

                    continue;
                }

                final Object value = source.get(field.name());
                if (value != null) {
                    beanInfo.setPropValue(result, field.name(), convertAvroValue(value, property.jsonXmlType, ignoreUnmatchedProperty));
                }
            }
            return beanInfo.finishBeanResult(result);
        } else if (targetType.isMap()) {
            final Map<Object, Object> result = N.newMap((Class<Map>) targetType.javaType());
            final Type<?> keyType = targetType.parameterTypes().get(0);
            final Type<?> valueType = targetType.parameterTypes().get(1);
            for (final Field field : source.getSchema().getFields()) {
                result.put(convertAvroValue(field.name(), keyType, ignoreUnmatchedProperty),
                        convertAvroValue(source.get(field.name()), valueType, ignoreUnmatchedProperty));
            }
            return (T) result;
        } else {
            throw new IllegalArgumentException("Unsupported record target type: " + targetType.name());
        }
    }
}
