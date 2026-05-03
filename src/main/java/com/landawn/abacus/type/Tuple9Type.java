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
import com.landawn.abacus.util.Tuple.Tuple9;

/**
 * Type handler for {@link Tuple9} objects.
 * This class provides serialization and deserialization support for 9-element tuple instances
 * containing elements of potentially different types. The serialization format is a JSON array:
 * {@code [t1, t2, t3, t4, t5, t6, t7, t8, t9]}.
 *
 * @param <T1> the type of the first element
 * @param <T2> the type of the second element
 * @param <T3> the type of the third element
 * @param <T4> the type of the fourth element
 * @param <T5> the type of the fifth element
 * @param <T6> the type of the sixth element
 * @param <T7> the type of the seventh element
 * @param <T8> the type of the eighth element
 * @param <T9> the type of the ninth element
 */
@SuppressWarnings("java:S2160")
public class Tuple9Type<T1, T2, T3, T4, T5, T6, T7, T8, T9> extends AbstractTupleType<Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9>> {

    /**
     * Constructs a Tuple9Type instance with the specified element types.
     * This constructor is package-private and should only be called by TypeFactory.
     *
     * @param t1TypeName the name of the first element type
     * @param t2TypeName the name of the second element type
     * @param t3TypeName the name of the third element type
     * @param t4TypeName the name of the fourth element type
     * @param t5TypeName the name of the fifth element type
     * @param t6TypeName the name of the sixth element type
     * @param t7TypeName the name of the seventh element type
     * @param t8TypeName the name of the eighth element type
     * @param t9TypeName the name of the ninth element type
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    Tuple9Type(final String t1TypeName, final String t2TypeName, final String t3TypeName, final String t4TypeName, final String t5TypeName,
            final String t6TypeName, final String t7TypeName, final String t8TypeName, final String t9TypeName) {
        super(getTypeName(t1TypeName, t2TypeName, t3TypeName, t4TypeName, t5TypeName, t6TypeName, t7TypeName, t8TypeName, t9TypeName, false),
                getTypeName(t1TypeName, t2TypeName, t3TypeName, t4TypeName, t5TypeName, t6TypeName, t7TypeName, t8TypeName, t9TypeName, true),
                (Class) Tuple9.class,
                List.of(TypeFactory.getType(t1TypeName), TypeFactory.getType(t2TypeName), TypeFactory.getType(t3TypeName), TypeFactory.getType(t4TypeName),
                        TypeFactory.getType(t5TypeName), TypeFactory.getType(t6TypeName), TypeFactory.getType(t7TypeName), TypeFactory.getType(t8TypeName),
                        TypeFactory.getType(t9TypeName)));
    }

    @SuppressWarnings({ "unchecked", "deprecation" })
    @Override
    protected Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9> fromArray(final Object[] converted) {
        return Tuple.of((T1) converted[0], (T2) converted[1], (T3) converted[2], (T4) converted[3], (T5) converted[4], (T6) converted[5], (T7) converted[6],
                (T8) converted[7], (T9) converted[8]);
    }

    /**
     * Generates the type name for a Tuple9 with the specified element type names.
     *
     * @param t1TypeName the type name of the first element
     * @param t2TypeName the type name of the second element
     * @param t3TypeName the type name of the third element
     * @param t4TypeName the type name of the fourth element
     * @param t5TypeName the type name of the fifth element
     * @param t6TypeName the type name of the sixth element
     * @param t7TypeName the type name of the seventh element
     * @param t8TypeName the type name of the eighth element
     * @param t9TypeName the type name of the ninth element
     * @param isDeclaringName if {@code true}, returns the declaring name (simple class names);
     *                        if {@code false}, returns the full canonical name
     * @return the formatted type name string
     */
    protected static String getTypeName(final String t1TypeName, final String t2TypeName, final String t3TypeName, final String t4TypeName,
            final String t5TypeName, final String t6TypeName, final String t7TypeName, final String t8TypeName, final String t9TypeName,
            final boolean isDeclaringName) {
        if (isDeclaringName) {
            return ClassUtil.getSimpleClassName(Tuple9.class) + SK.LESS_THAN + TypeFactory.getType(t1TypeName).declaringName() + SK.COMMA_SPACE
                    + TypeFactory.getType(t2TypeName).declaringName() + SK.COMMA_SPACE + TypeFactory.getType(t3TypeName).declaringName() + SK.COMMA_SPACE
                    + TypeFactory.getType(t4TypeName).declaringName() + SK.COMMA_SPACE + TypeFactory.getType(t5TypeName).declaringName() + SK.COMMA_SPACE
                    + TypeFactory.getType(t6TypeName).declaringName() + SK.COMMA_SPACE + TypeFactory.getType(t7TypeName).declaringName() + SK.COMMA_SPACE
                    + TypeFactory.getType(t8TypeName).declaringName() + SK.COMMA_SPACE + TypeFactory.getType(t9TypeName).declaringName() + SK.GREATER_THAN;
        } else {
            return ClassUtil.getCanonicalClassName(Tuple9.class) + SK.LESS_THAN + TypeFactory.getType(t1TypeName).name() + SK.COMMA_SPACE
                    + TypeFactory.getType(t2TypeName).name() + SK.COMMA_SPACE + TypeFactory.getType(t3TypeName).name() + SK.COMMA_SPACE
                    + TypeFactory.getType(t4TypeName).name() + SK.COMMA_SPACE + TypeFactory.getType(t5TypeName).name() + SK.COMMA_SPACE
                    + TypeFactory.getType(t6TypeName).name() + SK.COMMA_SPACE + TypeFactory.getType(t7TypeName).name() + SK.COMMA_SPACE
                    + TypeFactory.getType(t8TypeName).name() + SK.COMMA_SPACE + TypeFactory.getType(t9TypeName).name() + SK.GREATER_THAN;
        }
    }
}
