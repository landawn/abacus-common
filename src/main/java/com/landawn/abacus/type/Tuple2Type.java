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
import com.landawn.abacus.util.Tuple.Tuple2;

/**
 * Type handler for {@link Tuple2} objects.
 * This class provides serialization and deserialization support for tuple instances
 * containing two elements of potentially different types.
 *
 * @param <T1> the type of the first element in the tuple
 * @param <T2> the type of the second element in the tuple
 */
@SuppressWarnings("java:S2160")
public class Tuple2Type<T1, T2> extends AbstractTupleType<Tuple2<T1, T2>> {

    /**
     * Constructs a Tuple2Type instance with the specified element types.
     * This constructor is package-private and should only be called by TypeFactory.
     *
     * @param t1TypeName the name of the first element type
     * @param t2TypeName the name of the second element type
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    Tuple2Type(final String t1TypeName, final String t2TypeName) {
        super(getTypeName(t1TypeName, t2TypeName, false), getTypeName(t1TypeName, t2TypeName, true), (Class) Tuple2.class,
                List.of(TypeFactory.getType(t1TypeName), TypeFactory.getType(t2TypeName)));
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Tuple2<T1, T2> fromArray(final Object[] converted) {
        return Tuple.of((T1) converted[0], (T2) converted[1]);
    }

    /**
     * Generates the type name for a Tuple2 with the specified element type names.
     *
     * @param t1TypeName the type name of the first element
     * @param t2TypeName the type name of the second element
     * @param isDeclaringName if {@code true}, returns the declaring name (simple class names);
     *                        if {@code false}, returns the full canonical name
     * @return the formatted type name string
     */
    protected static String getTypeName(final String t1TypeName, final String t2TypeName, final boolean isDeclaringName) {
        if (isDeclaringName) {
            return ClassUtil.getSimpleClassName(Tuple2.class) + SK.LESS_THAN + TypeFactory.getType(t1TypeName).declaringName() + SK.COMMA_SPACE
                    + TypeFactory.getType(t2TypeName).declaringName() + SK.GREATER_THAN;
        } else {
            return ClassUtil.getCanonicalClassName(Tuple2.class) + SK.LESS_THAN + TypeFactory.getType(t1TypeName).name() + SK.COMMA_SPACE
                    + TypeFactory.getType(t2TypeName).name() + SK.GREATER_THAN;
        }
    }
}
