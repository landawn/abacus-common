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

import com.landawn.abacus.util.DataSet;
import com.landawn.abacus.util.Strings;

/**
 * Type handler for DataSet values.
 * This class provides serialization and deserialization for DataSet objects,
 * which are tabular data structures used for data manipulation and analysis.
 * DataSets are serialized to and from JSON format.
 */
@SuppressWarnings("java:S2160")
public class DataSetType extends AbstractType<DataSet> {

    public static final String DATA_SET = DataSet.class.getSimpleName();

    private final Class<DataSet> typeClass;

    DataSetType() {
        super(DATA_SET);

        typeClass = DataSet.class;
    }

    /**
     * Returns the Java class type handled by this type handler.
     *
     * @return The Class object representing DataSet.class
     */
    @Override
    public Class<DataSet> clazz() {
        return typeClass;
    }

    /**
     * Indicates whether this type represents a DataSet.
     * Always returns true for DataSetType.
     *
     * @return true, as this type handler specifically handles DataSet objects
     */
    @Override
    public boolean isDataSet() {
        return true;
    }

    /**
     * Indicates whether this DataSet type is serializable in the type system.
     * DataSets require special JSON serialization handling.
     *
     * @return false, indicating DataSets are not simply serializable
     */
    @Override
    public boolean isSerializable() {
        return false;
    }

    /**
     * Returns the serialization type category for DataSet objects.
     *
     * @return SerializationType.DATA_SET, indicating special DataSet serialization handling
     */
    @Override
    public SerializationType getSerializationType() {
        return SerializationType.DATA_SET;
    }

    /**
     * Converts a DataSet to its JSON string representation.
     * The DataSet is serialized with all its columns, rows, and metadata.
     *
     * @param x the DataSet to convert. Can be null.
     * @return A JSON string representation of the DataSet, or null if input is null
     */
    @Override
    public String stringOf(final DataSet x) {
        return (x == null) ? null : Utils.jsonParser.serialize(x, Utils.jsc);
    }

    /**
     * Converts a JSON string representation back to a DataSet object.
     * The string should contain a valid JSON representation of a DataSet
     * with its structure and data.
     *
     * @param str the JSON string to parse. Can be null or empty.
     * @return A DataSet parsed from the JSON string, or null if input is null/empty
     */
    @Override
    public DataSet valueOf(final String str) {
        return (Strings.isEmpty(str)) ? null : Utils.jsonParser.deserialize(str, typeClass);
    }
}