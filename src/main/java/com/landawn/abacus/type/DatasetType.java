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

package com.landawn.abacus.type;

import com.landawn.abacus.util.Dataset;
import com.landawn.abacus.util.Strings;

/**
 * Type handler for {@link Dataset} values.
 * {@link Dataset} is a tabular data structure used for in-memory data manipulation and analysis.
 *
 * <p>Datasets are serialized to and from JSON format. The serialization preserves all column
 * names, row data, and the overall tabular structure.
 *
 * <p>Note: {@link #isSerializable()} returns {@code false}; the type system routes Dataset
 * serialization through {@link SerializationType#DATA_SET} rather than the generic serializable path.
 *
 * @see AbstractType
 * @see Dataset
 */
@SuppressWarnings("java:S2160")
public class DatasetType extends AbstractType<Dataset> {

    /** The type name constant for Dataset type identification, equal to {@code "Dataset"}. */
    public static final String DATA_SET = Dataset.class.getSimpleName();

    private final Class<Dataset> typeClass;

    /**
     * Package-private constructor for {@code DatasetType}.
     * Instances are created by the {@code TypeFactory}.
     */
    DatasetType() {
        super(DATA_SET);

        typeClass = Dataset.class;
    }

    /**
     * Returns the Java class represented by this type handler.
     *
     * @return {@code Dataset.class}
     */
    @Override
    public Class<Dataset> javaType() {
        return typeClass;
    }

    /**
     * Indicates whether this type represents a {@link Dataset}.
     *
     * @return {@code true}, always, because this handler is dedicated to {@link Dataset} objects
     */
    @Override
    public boolean isDataset() {
        return true;
    }

    /**
     * Indicates whether this type is handled by the generic serializable path.
     * {@link Dataset} uses a dedicated serialization path ({@link SerializationType#DATA_SET}).
     *
     * @return {@code false}, always
     */
    @Override
    public boolean isSerializable() {
        return false;
    }

    /**
     * Returns the serialization type category for {@link Dataset} objects.
     *
     * @return {@link SerializationType#DATA_SET}
     */
    @Override
    public SerializationType serializationType() {
        return SerializationType.DATA_SET;
    }

    /**
     * Serializes a {@link Dataset} to its JSON string representation, preserving all
     * column names, row data, and tabular structure.
     *
     * <p>The returned string is a serializable representation designed to be parsed back into an equivalent value
     * via {@link #valueOf(String)}; {@code stringOf} and {@code valueOf} are inverse operations that round-trip. This
     * is the key distinction from {@link Object#toString()}, whose result is not guaranteed to be convertible back
     * into the original value.</p>
     *
     * @param x the {@link Dataset} to serialize; may be {@code null}
     * @return the JSON string, or {@code null} if {@code x} is {@code null}
     * @see #valueOf(String)
     * @see #valueOf(Object)
     */
    @Override
    public String stringOf(final Dataset x) {
        return (x == null) ? null : Utils.jsonParser.serialize(x, Utils.jsc);
    }

    /**
     * Deserializes a JSON string back into a {@link Dataset} object.
     * The JSON must represent a valid {@link Dataset} structure with column names and row data.
     *
     * <p>This method is the inverse of {@code stringOf} and round-trips with it: it parses the string produced by
     * {@code stringOf} back into a value of this type. Strings produced by {@link Object#toString()} are not
     * guaranteed to be parseable in this way.</p>
     *
     * @param str the JSON string to parse; may be {@code null} or empty
     * @return the deserialized {@link Dataset}, or {@code null} if {@code str} is {@code null} or empty
     * @see #valueOf(Object)
     * @see #stringOf(Dataset)
     */
    @Override
    public Dataset valueOf(final String str) {
        return (Strings.isEmpty(str)) ? null : Utils.jsonParser.deserialize(str, typeClass);
    }
}
