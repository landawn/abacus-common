/*
 * Copyright (C) 2016 HaiYang Li
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

package com.landawn.abacus.util;

/**
 * 
 * @since 0.8
 * 
 * @author Haiyang Li
 */
public final class IndexedChar extends AbstractIndexed {
    private final char value;

    IndexedChar(long index, char value) {
        super(index);
        this.value = value;
    }

    public static IndexedChar of(char value, int index) {
        N.checkArgNotNegative(index, "index");

        return new IndexedChar(index, value);
    }

    public static IndexedChar of(char value, long index) {
        N.checkArgNotNegative(index, "index");

        return new IndexedChar(index, value);
    }

    public static ObjIterator<IndexedChar> of(final CharIterator iter) {
        return of(iter, 0);
    }

    public static ObjIterator<IndexedChar> of(final CharIterator iter, final int startIndex) {
        return of(iter, (long) startIndex);
    }

    public static ObjIterator<IndexedChar> of(final CharIterator iter, final long startIndex) {
        if (startIndex < 0) {
            throw new IllegalArgumentException("Invalid start index: " + startIndex);
        }

        return new ObjIterator<IndexedChar>() {
            private long idx = startIndex;

            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public IndexedChar next() {
                return IndexedChar.of(iter.nextChar(), idx++);
            }
        };
    }

    public char value() {
        return value;
    }

    @Override
    public int hashCode() {
        return (int) index + value * 31;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof IndexedChar && ((IndexedChar) obj).index == index && N.equals(((IndexedChar) obj).value, value);
    }

    @Override
    public String toString() {
        return "[" + index + "]=" + value;
    }
}
