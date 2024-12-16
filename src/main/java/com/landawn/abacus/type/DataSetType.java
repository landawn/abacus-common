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

package com.landawn.abacus.type;

import com.landawn.abacus.util.DataSet;
import com.landawn.abacus.util.Strings;

@SuppressWarnings("java:S2160")
public class DataSetType extends AbstractType<DataSet> {

    public static final String DATA_SET = DataSet.class.getSimpleName();

    private final Class<DataSet> typeClass;

    DataSetType() {
        super(DATA_SET);

        typeClass = DataSet.class;
    }

    @Override
    public Class<DataSet> clazz() {
        return typeClass;
    }

    /**
     * Checks if is data set.
     *
     * @return {@code true}, if is data set
     */
    @Override
    public boolean isDataSet() {
        return true;
    }

    /**
     * Checks if is serializable.
     *
     * @return {@code true}, if is serializable
     */
    @Override
    public boolean isSerializable() {
        return false;
    }

    /**
     * Gets the serialization type.
     *
     * @return
     */
    @Override
    public SerializationType getSerializationType() {
        return SerializationType.DATA_SET;
    }

    /**
     *
     * @param x
     * @return
     */
    @Override
    public String stringOf(final DataSet x) {
        return (x == null) ? null : Utils.jsonParser.serialize(x, Utils.jsc);
    }

    /**
     *
     * @param str
     * @return
     */
    @Override
    public DataSet valueOf(final String str) {
        return (Strings.isEmpty(str)) ? null : Utils.jsonParser.deserialize(str, typeClass);
    }
}
