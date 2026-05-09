/*
 * Copyright (C) 2017 HaiYang Li
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

import java.util.List;

import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.SK;
import com.landawn.abacus.util.Tuple;
import com.landawn.abacus.util.Tuple.Tuple1;

/**
 * Type handler for {@link Tuple1} objects.
 * This class provides serialization and deserialization support for 1-element tuple instances.
 * The serialization format is a JSON array: {@code [t1]}.
 *
 * @param <T1> the type of the single element in the tuple
 */
@SuppressWarnings("java:S2160")
public class Tuple1Type<T1> extends AbstractTupleType<Tuple1<T1>> {

    /**
     * Constructs a Tuple1Type instance with the specified element type.
     * This constructor is package-private and should only be called by TypeFactory.
     *
     * @param t1TypeName the name of the element type
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    Tuple1Type(final String t1TypeName) {
        super(getTypeName(t1TypeName, false), getTypeName(t1TypeName, true), (Class) Tuple1.class, List.of(TypeFactory.getType(t1TypeName)));
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Tuple1<T1> fromArray(final Object[] converted) {
        return Tuple.of((T1) converted[0]);
    }

    /**
     * Generates the type name for a Tuple1 type with the specified element type.
     *
     * @param t1TypeName the name of the element type
     * @param isDeclaringName if {@code true}, uses simple class names; if {@code false}, uses canonical class names
     * @return the generated type name for Tuple1 with the specified element type
     */
    protected static String getTypeName(final String t1TypeName, final boolean isDeclaringName) {
        if (isDeclaringName) {
            return ClassUtil.getSimpleClassName(Tuple1.class) + SK.LESS_THAN + TypeFactory.getType(t1TypeName).declaringName() + SK.GREATER_THAN;
        } else {
            return ClassUtil.getCanonicalClassName(Tuple1.class) + SK.LESS_THAN + TypeFactory.getType(t1TypeName).name() + SK.GREATER_THAN;
        }
    }
}
