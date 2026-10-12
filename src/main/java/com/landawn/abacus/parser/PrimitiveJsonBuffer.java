/*
 * Copyright (C) 2026 HaiYang Li
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

import java.util.AbstractList;
import com.landawn.abacus.type.AbstractPrimitiveListType;
import com.landawn.abacus.type.PrimitiveBooleanArrayType;
import com.landawn.abacus.type.PrimitiveBooleanListType;
import com.landawn.abacus.type.PrimitiveBooleanType;
import com.landawn.abacus.type.PrimitiveByteArrayType;
import com.landawn.abacus.type.PrimitiveByteListType;
import com.landawn.abacus.type.PrimitiveByteType;
import com.landawn.abacus.type.PrimitiveCharArrayType;
import com.landawn.abacus.type.PrimitiveCharListType;
import com.landawn.abacus.type.PrimitiveCharType;
import com.landawn.abacus.type.PrimitiveDoubleArrayType;
import com.landawn.abacus.type.PrimitiveDoubleListType;
import com.landawn.abacus.type.PrimitiveDoubleType;
import com.landawn.abacus.type.PrimitiveFloatArrayType;
import com.landawn.abacus.type.PrimitiveFloatListType;
import com.landawn.abacus.type.PrimitiveFloatType;
import com.landawn.abacus.type.PrimitiveIntArrayType;
import com.landawn.abacus.type.PrimitiveIntListType;
import com.landawn.abacus.type.PrimitiveIntType;
import com.landawn.abacus.type.PrimitiveLongArrayType;
import com.landawn.abacus.type.PrimitiveLongListType;
import com.landawn.abacus.type.PrimitiveLongType;
import com.landawn.abacus.type.PrimitiveShortArrayType;
import com.landawn.abacus.type.PrimitiveShortListType;
import com.landawn.abacus.type.PrimitiveShortType;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.BooleanList;
import com.landawn.abacus.util.ByteList;
import com.landawn.abacus.util.CharList;
import com.landawn.abacus.util.DoubleList;
import com.landawn.abacus.util.FloatList;
import com.landawn.abacus.util.IntList;
import com.landawn.abacus.util.LongList;
import com.landawn.abacus.util.ShortList;

/** Private append-only storage for built-in primitive JSON containers. Scalar conversion remains in the parser. */
abstract class PrimitiveJsonBuffer extends AbstractList<Object> {
    abstract Object array();

    abstract Object primitiveList();

    static PrimitiveJsonBuffer createList(final Type<?> type) {
        final Class<?> cls = type.getClass();
        if (cls != PrimitiveBooleanListType.class && cls != PrimitiveCharListType.class && cls != PrimitiveByteListType.class
                && cls != PrimitiveShortListType.class && cls != PrimitiveIntListType.class && cls != PrimitiveLongListType.class
                && cls != PrimitiveFloatListType.class && cls != PrimitiveDoubleListType.class) {
            return null;
        }
        return create(((AbstractPrimitiveListType<?>) type).arrayType(), type.elementType());
    }

    static PrimitiveJsonBuffer create(final Type<?> arrayType, final Type<?> elementType) {
        if (arrayType == null || elementType == null || arrayType.elementType() != elementType) {
            return null;
        }
        if (arrayType.getClass() == PrimitiveBooleanArrayType.class && elementType.getClass() == PrimitiveBooleanType.class) {
            return new PrimitiveJsonBuffer() {
                private final BooleanList values = new BooleanList();

                @Override
                public int size() {
                    return values.size();
                }

                @Override
                public Object get(final int index) {
                    return values.get(index);
                }

                @Override
                public boolean add(final Object value) {
                    values.add((Boolean) value);
                    return true;
                }

                @Override
                Object array() {
                    return values.toArray();
                }

                @Override
                Object primitiveList() {
                    return values;
                }
            };
        }
        if (arrayType.getClass() == PrimitiveCharArrayType.class && elementType.getClass() == PrimitiveCharType.class) {
            return new PrimitiveJsonBuffer() {
                private final CharList values = new CharList();

                @Override
                public int size() {
                    return values.size();
                }

                @Override
                public Object get(final int index) {
                    return values.get(index);
                }

                @Override
                public boolean add(final Object value) {
                    values.add((Character) value);
                    return true;
                }

                @Override
                Object array() {
                    return values.toArray();
                }

                @Override
                Object primitiveList() {
                    return values;
                }
            };
        }
        if (arrayType.getClass() == PrimitiveByteArrayType.class && elementType.getClass() == PrimitiveByteType.class) {
            return new PrimitiveJsonBuffer() {
                private final ByteList values = new ByteList();

                @Override
                public int size() {
                    return values.size();
                }

                @Override
                public Object get(final int index) {
                    return values.get(index);
                }

                @Override
                public boolean add(final Object value) {
                    values.add((Byte) value);
                    return true;
                }

                @Override
                Object array() {
                    return values.toArray();
                }

                @Override
                Object primitiveList() {
                    return values;
                }
            };
        }
        if (arrayType.getClass() == PrimitiveShortArrayType.class && elementType.getClass() == PrimitiveShortType.class) {
            return new PrimitiveJsonBuffer() {
                private final ShortList values = new ShortList();

                @Override
                public int size() {
                    return values.size();
                }

                @Override
                public Object get(final int index) {
                    return values.get(index);
                }

                @Override
                public boolean add(final Object value) {
                    values.add((Short) value);
                    return true;
                }

                @Override
                Object array() {
                    return values.toArray();
                }

                @Override
                Object primitiveList() {
                    return values;
                }
            };
        }
        if (arrayType.getClass() == PrimitiveIntArrayType.class && elementType.getClass() == PrimitiveIntType.class) {
            return new PrimitiveJsonBuffer() {
                private final IntList values = new IntList();

                @Override
                public int size() {
                    return values.size();
                }

                @Override
                public Object get(final int index) {
                    return values.get(index);
                }

                @Override
                public boolean add(final Object value) {
                    values.add((Integer) value);
                    return true;
                }

                @Override
                Object array() {
                    return values.toArray();
                }

                @Override
                Object primitiveList() {
                    return values;
                }
            };
        }
        if (arrayType.getClass() == PrimitiveLongArrayType.class && elementType.getClass() == PrimitiveLongType.class) {
            return new PrimitiveJsonBuffer() {
                private final LongList values = new LongList();

                @Override
                public int size() {
                    return values.size();
                }

                @Override
                public Object get(final int index) {
                    return values.get(index);
                }

                @Override
                public boolean add(final Object value) {
                    values.add((Long) value);
                    return true;
                }

                @Override
                Object array() {
                    return values.toArray();
                }

                @Override
                Object primitiveList() {
                    return values;
                }
            };
        }
        if (arrayType.getClass() == PrimitiveFloatArrayType.class && elementType.getClass() == PrimitiveFloatType.class) {
            return new PrimitiveJsonBuffer() {
                private final FloatList values = new FloatList();

                @Override
                public int size() {
                    return values.size();
                }

                @Override
                public Object get(final int index) {
                    return values.get(index);
                }

                @Override
                public boolean add(final Object value) {
                    values.add((Float) value);
                    return true;
                }

                @Override
                Object array() {
                    return values.toArray();
                }

                @Override
                Object primitiveList() {
                    return values;
                }
            };
        }
        if (arrayType.getClass() == PrimitiveDoubleArrayType.class && elementType.getClass() == PrimitiveDoubleType.class) {
            return new PrimitiveJsonBuffer() {
                private final DoubleList values = new DoubleList();

                @Override
                public int size() {
                    return values.size();
                }

                @Override
                public Object get(final int index) {
                    return values.get(index);
                }

                @Override
                public boolean add(final Object value) {
                    values.add((Double) value);
                    return true;
                }

                @Override
                Object array() {
                    return values.toArray();
                }

                @Override
                Object primitiveList() {
                    return values;
                }
            };
        }
        return null;
    }
}
