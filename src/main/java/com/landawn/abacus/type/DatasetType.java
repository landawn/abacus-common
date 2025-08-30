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
 * Type handler for Dataset values.
 * This class provides serialization and deserialization for Dataset objects,
 * which are tabular data structures used for data manipulation and analysis.
 * Datasets are serialized to and from JSON format.
 */
@SuppressWarnings("java:S2160")
public class DatasetType extends AbstractType<Dataset> {

    public static final String DATA_SET = Dataset.class.getSimpleName();

    private final Class<Dataset> typeClass;

    DatasetType() {
        super(DATA_SET);

        typeClass = Dataset.class;
    }

    /**
     * Returns the Java class type handled by this type handler.
     *
     * @return The Class object representing Dataset.class
     */
    @Override
    public Class<Dataset> clazz() {
        return typeClass;
    }

    /**
     * Indicates whether this type represents a Dataset.
     * Always returns true for DatasetType.
     *
     * @return true, as this type handler specifically handles Dataset objects
     */
    @Override
    public boolean isDataset() {
        return true;
    }

    /**
     * Indicates whether this Dataset type is serializable in the type system.
     * Datasets require special JSON serialization handling.
     *
     * @return false, indicating Datasets are not simply serializable
     */
    @Override
    public boolean isSerializable() {
        return false;
    }

    /**
     * Returns the serialization type category for Dataset objects.
     *
     * @return SerializationType.DATA_SET, indicating special Dataset serialization handling
     */
    @Override
    public SerializationType getSerializationType() {
        return SerializationType.DATA_SET;
    }

    /**
     * Converts a Dataset to its JSON string representation.
     * The Dataset is serialized with all its columns, rows, and metadata.
     *
     * @param x the Dataset to convert. Can be null.
     * @return A JSON string representation of the Dataset, or null if input is null
     */
    @Override
    public String stringOf(final Dataset x) {
        return (x == null) ? null : Utils.jsonParser.serialize(x, Utils.jsc);
    }

    /**
     * Converts a JSON string representation back to a Dataset object.
     * The string should contain a valid JSON representation of a Dataset
     * with its structure and data.
     *
     * @param str the JSON string to parse. Can be null or empty.
     * @return A Dataset parsed from the JSON string, or null if input is null/empty
     */
    @Override
    public Dataset valueOf(final String str) {
        return (Strings.isEmpty(str)) ? null : Utils.jsonParser.deserialize(str, typeClass);
    }
}