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
import com.landawn.abacus.util.Tuple.Tuple3;

/**
 * Type handler for {@link Tuple3} objects.
 * This class provides serialization and deserialization support for 3-element tuple instances
 * containing elements of potentially different types. The serialization format is a JSON array:
 * {@code [t1, t2, t3]}.
 *
 * @param <T1> the type of the first element in the tuple
 * @param <T2> the type of the second element in the tuple
 * @param <T3> the type of the third element in the tuple
 */
@SuppressWarnings("java:S2160")
public class Tuple3Type<T1, T2, T3> extends AbstractTupleType<Tuple3<T1, T2, T3>> {

    /**
     * Constructs a Tuple3Type instance with the specified element types.
     * This constructor is package-private and should only be called by TypeFactory.
     *
     * @param t1TypeName the name of the first element type
     * @param t2TypeName the name of the second element type
     * @param t3TypeName the name of the third element type
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    Tuple3Type(final String t1TypeName, final String t2TypeName, final String t3TypeName) {
        super(getTypeName(t1TypeName, t2TypeName, t3TypeName, false), getTypeName(t1TypeName, t2TypeName, t3TypeName, true), (Class) Tuple3.class,
                List.of(TypeFactory.getType(t1TypeName), TypeFactory.getType(t2TypeName), TypeFactory.getType(t3TypeName)));
    }

    /**
     * Reconstructs a {@link Tuple3} from an already type-converted element array.
     * The array is expected to contain exactly three elements matching the tuple's parameter types.
     *
     * @param converted the array of converted element values (must be of length 3)
     * @return a new {@code Tuple3} containing the three elements in order
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Tuple3<T1, T2, T3> fromArray(final Object[] converted) {
        return Tuple.of((T1) converted[0], (T2) converted[1], (T3) converted[2]);
    }

    /**
     * Generates the type name for a Tuple3 with the specified element type names.
     *
     * @param t1TypeName the type name of the first element
     * @param t2TypeName the type name of the second element
     * @param t3TypeName the type name of the third element
     * @param isDeclaringName if {@code true}, returns the declaring name (simple class names);
     *                        if {@code false}, returns the full canonical name
     * @return the formatted type name string
     */
    protected static String getTypeName(final String t1TypeName, final String t2TypeName, final String t3TypeName, final boolean isDeclaringName) {
        if (isDeclaringName) {
            return ClassUtil.getSimpleClassName(Tuple3.class) + SK.LESS_THAN + TypeFactory.getType(t1TypeName).declaringName() + SK.COMMA_SPACE
                    + TypeFactory.getType(t2TypeName).declaringName() + SK.COMMA_SPACE + TypeFactory.getType(t3TypeName).declaringName() + SK.GREATER_THAN;
        } else {
            return ClassUtil.getCanonicalClassName(Tuple3.class) + SK.LESS_THAN + TypeFactory.getType(t1TypeName).name() + SK.COMMA_SPACE
                    + TypeFactory.getType(t2TypeName).name() + SK.COMMA_SPACE + TypeFactory.getType(t3TypeName).name() + SK.GREATER_THAN;
        }
    }
}
