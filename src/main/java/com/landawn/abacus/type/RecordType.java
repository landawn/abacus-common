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

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

import com.landawn.abacus.parser.JSONDeserializationConfig;
import com.landawn.abacus.parser.JSONDeserializationConfig.JDC;
import com.landawn.abacus.parser.SerializationConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.ClassUtil.RecordInfo;
import com.landawn.abacus.util.Clazz;
import com.landawn.abacus.util.Maps;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Tuple.Tuple5;

/**
 *
 * @author Haiyang Li
 * @param <T>
 * @since 0.8
 */
public final class RecordType<T> extends AbstractType<T> {
    private final Class<T> typeClass;

    private final Type<Map<String, Object>> mapType;
    private final JSONDeserializationConfig jdc;

    @SuppressWarnings("deprecation")
    RecordType(Class<T> cls) {
        super(TypeFactory.getClassName(cls));
        this.typeClass = cls;

        jdc = JDC.create();

        final RecordInfo<?> recordInfo = ClassUtil.getRecordInfo(cls);

        for (Tuple5<String, Field, Method, Type<Object>, Integer> tp : recordInfo.fieldMap().values()) {
            jdc.setPropType(tp._1, tp._4);
        }

        mapType = TypeFactory.getType("Map<String, Object>");
    }

    @Override
    public Class<T> clazz() {
        return typeClass;
    }

    /**
     * Checks if is Record.
     *
     * @return true, if is Record
     */
    @Override
    public boolean isRecord() {
        return true;
    }

    /**
     * Checks if is serializable.
     *
     * @return true, if is serializable
     */
    @Override
    public boolean isSerializable() {
        return true;
    }

    /**
     *
     * @param x
     * @return
     */
    @Override
    public String stringOf(T x) {
        return (x == null) ? null : Utils.jsonParser.serialize(toMap(x), Utils.jsc);
    }

    /**
     *
     * @param st
     * @return
     */
    @Override
    public T valueOf(String st) {
        return (N.isNullOrEmpty(st)) ? null : fromMap(Utils.jsonParser.deserialize(Clazz.<String, Object> ofMap(), st, jdc));
    }

    /**
     *
     * @param writer
     * @param x
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public void write(final Writer writer, final T x) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            mapType.write(writer, toMap(x));
        }
    }

    /**
     *
     * @param writer
     * @param x
     * @param config
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, T x, final SerializationConfig<?> config) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            mapType.writeCharacter(writer, toMap(x), config);
        }
    }

    private Map<String, Object> toMap(final T x) {
        return Maps.record2Map(x);
    }

    private T fromMap(final Map<String, Object> map) {
        return Maps.map2Record(map, typeClass);
    }
}
