/*
 * Copyright (c) 2017, Haiyang Li.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.landawn.abacus.util;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;

import com.landawn.abacus.util.function.Function;
import com.landawn.abacus.util.function.ToBooleanFunction;
import com.landawn.abacus.util.function.ToByteFunction;
import com.landawn.abacus.util.function.ToCharFunction;
import com.landawn.abacus.util.function.ToDoubleFunction;
import com.landawn.abacus.util.function.ToFloatFunction;
import com.landawn.abacus.util.function.ToIntFunction;
import com.landawn.abacus.util.function.ToLongFunction;
import com.landawn.abacus.util.function.ToShortFunction;

/**
 * 
 * Factory utility class for Comparator.
 * 
 * @author haiyangl
 *
 */
public abstract class Comparators {

    @SuppressWarnings("rawtypes")
    static final Comparator NULL_FIRST_COMPARATOR = new Comparator<Comparable>() {
        @Override
        public int compare(final Comparable a, final Comparable b) {
            return a == null ? (b == null ? 0 : -1) : (b == null ? 1 : a.compareTo(b));
        }
    };

    @SuppressWarnings("rawtypes")
    static final Comparator NULL_LAST_COMPARATOR = new Comparator<Comparable>() {
        @Override
        public int compare(final Comparable a, final Comparable b) {
            return a == null ? (b == null ? 0 : 1) : (b == null ? -1 : a.compareTo(b));
        }
    };

    @SuppressWarnings("rawtypes")
    static final Comparator NATURAL_ORDER = NULL_FIRST_COMPARATOR;

    @SuppressWarnings("rawtypes")
    static final Comparator REVERSED_ORDER = Collections.reverseOrder(NATURAL_ORDER);

    static final Comparator<String> COMPARING_IGNORE_CASE = new Comparator<String>() {
        @Override
        public int compare(String a, String b) {
            return N.compareIgnoreCase(a, b);
        }
    };

    static final Comparator<CharSequence> COMPARING_BY_LENGTH = new Comparator<CharSequence>() {
        @Override
        public int compare(CharSequence a, CharSequence b) {
            return (a == null ? 0 : a.length()) - (b == null ? 0 : b.length());
        }
    };

    @SuppressWarnings("rawtypes")
    static final Comparator<Collection> COMPARING_BY_SIZE = new Comparator<Collection>() {
        @Override
        public int compare(Collection a, Collection b) {
            return (a == null ? 0 : a.size()) - (b == null ? 0 : b.size());
        }
    };

    @SuppressWarnings("rawtypes")
    static final Comparator<Map> COMPARING_BY_SIZEE = new Comparator<Map>() {
        @Override
        public int compare(Map a, Map b) {
            return (a == null ? 0 : a.size()) - (b == null ? 0 : b.size());
        }
    };

    Comparators() {
        // Singleton
    }

    @SuppressWarnings("unchecked")
    public static <T> Comparator<T> naturalOrder() {
        return NATURAL_ORDER;
    }

    public static <T> Comparator<T> reversedOrder() {
        return REVERSED_ORDER;
    }

    public static <T> Comparator<T> reversedOrder(final Comparator<T> cmp) {
        if (cmp == null || cmp == NATURAL_ORDER) {
            return REVERSED_ORDER;
        } else if (cmp == REVERSED_ORDER) {
            return NATURAL_ORDER;
        }

        return Collections.reverseOrder(cmp);
    }

    public static <T> Comparator<T> nullsFirst() {
        return NULL_FIRST_COMPARATOR;
    }

    public static <T> Comparator<T> nullsFirst(final Comparator<T> cmp) {
        if (cmp == null || cmp == NULL_FIRST_COMPARATOR) {
            return NULL_FIRST_COMPARATOR;
        }

        return new Comparator<T>() {
            @Override
            public int compare(T a, T b) {
                return a == null ? (b == null ? 0 : -1) : (b == null ? 1 : cmp.compare(a, b));
            }
        };
    }

    public static <T> Comparator<T> nullsLast() {
        return NULL_LAST_COMPARATOR;
    }

    public static <T> Comparator<T> nullsLast(final Comparator<T> cmp) {
        if (cmp == null || cmp == NULL_LAST_COMPARATOR) {
            return NULL_LAST_COMPARATOR;
        }

        return new Comparator<T>() {
            @Override
            public int compare(T a, T b) {
                return a == null ? (b == null ? 0 : 1) : (b == null ? -1 : cmp.compare(a, b));
            }
        };
    }

    @SuppressWarnings("rawtypes")
    public static <T, U extends Comparable> Comparator<T> comparingBy(final Function<? super T, ? extends U> keyMapper) {
        N.checkArgNotNull(keyMapper);

        return new Comparator<T>() {
            @Override
            public int compare(T a, T b) {
                return N.compare(keyMapper.apply(a), keyMapper.apply(b));
            }
        };
    }

    @SuppressWarnings("rawtypes")
    public static <T, U extends Comparable> Comparator<T> reversedComparingBy(final Function<? super T, ? extends U> keyMapper) {
        N.checkArgNotNull(keyMapper);

        return new Comparator<T>() {
            @Override
            public int compare(T a, T b) {
                return N.compare(keyMapper.apply(b), keyMapper.apply(a));
            }
        };
    }

    public static <T, U> Comparator<T> comparingBy(final Function<? super T, ? extends U> keyMapper, final Comparator<? super U> keyComparator) {
        N.checkArgNotNull(keyMapper);
        N.checkArgNotNull(keyComparator);

        return new Comparator<T>() {
            @Override
            public int compare(T a, T b) {
                return keyComparator.compare(keyMapper.apply(a), keyMapper.apply(b));
            }
        };
    }

    public static <T> Comparator<T> comparingBoolean(final ToBooleanFunction<? super T> keyMapper) {
        N.checkArgNotNull(keyMapper);

        return new Comparator<T>() {
            @Override
            public int compare(T a, T b) {
                return Boolean.compare(keyMapper.applyAsBoolean(a), keyMapper.applyAsBoolean(b));
            }
        };
    }

    public static <T> Comparator<T> comparingChar(final ToCharFunction<? super T> keyMapper) {
        N.checkArgNotNull(keyMapper);

        return new Comparator<T>() {
            @Override
            public int compare(T a, T b) {
                return Character.compare(keyMapper.applyAsChar(a), keyMapper.applyAsChar(b));
            }
        };
    }

    public static <T> Comparator<T> comparingByte(final ToByteFunction<? super T> keyMapper) {
        N.checkArgNotNull(keyMapper);

        return new Comparator<T>() {
            @Override
            public int compare(T a, T b) {
                return Byte.compare(keyMapper.applyAsByte(a), keyMapper.applyAsByte(b));
            }
        };
    }

    public static <T> Comparator<T> comparingShort(final ToShortFunction<? super T> keyMapper) {
        N.checkArgNotNull(keyMapper);

        return new Comparator<T>() {
            @Override
            public int compare(T a, T b) {
                return Short.compare(keyMapper.applyAsShort(a), keyMapper.applyAsShort(b));
            }
        };
    }

    public static <T> Comparator<T> comparingInt(final ToIntFunction<? super T> keyMapper) {
        N.checkArgNotNull(keyMapper);

        return new Comparator<T>() {
            @Override
            public int compare(T a, T b) {
                return Integer.compare(keyMapper.applyAsInt(a), keyMapper.applyAsInt(b));
            }
        };
    }

    public static <T> Comparator<T> comparingLong(final ToLongFunction<? super T> keyMapper) {
        N.checkArgNotNull(keyMapper);

        return new Comparator<T>() {
            @Override
            public int compare(T a, T b) {
                return Long.compare(keyMapper.applyAsLong(a), keyMapper.applyAsLong(b));
            }
        };
    }

    public static <T> Comparator<T> comparingFloat(final ToFloatFunction<? super T> keyMapper) {
        N.checkArgNotNull(keyMapper);

        return new Comparator<T>() {
            @Override
            public int compare(T a, T b) {
                return Float.compare(keyMapper.applyAsFloat(a), keyMapper.applyAsFloat(b));
            }
        };
    }

    public static <T> Comparator<T> comparingDouble(final ToDoubleFunction<? super T> keyMapper) {
        N.checkArgNotNull(keyMapper);

        return new Comparator<T>() {
            @Override
            public int compare(T a, T b) {
                return Double.compare(keyMapper.applyAsDouble(a), keyMapper.applyAsDouble(b));
            }
        };
    }

    public static Comparator<String> comparingIgnoreCase() {
        return COMPARING_IGNORE_CASE;
    }

    public static <T> Comparator<T> comparingIgnoreCase(final Function<? super T, String> keyMapper) {
        N.checkArgNotNull(keyMapper);

        return new Comparator<T>() {
            @Override
            public int compare(T a, T b) {
                return N.compareIgnoreCase(keyMapper.apply(a), keyMapper.apply(b));
            }
        };
    }

    @SuppressWarnings("rawtypes")
    public static <K extends Comparable, V> Comparator<Map.Entry<K, V>> comparingByKey() {
        return new Comparator<Map.Entry<K, V>>() {
            @Override
            public int compare(Map.Entry<K, V> a, Map.Entry<K, V> b) {
                return N.compare(a.getKey(), b.getKey());
            }
        };
    }

    @SuppressWarnings("rawtypes")
    public static <K extends Comparable, V> Comparator<Map.Entry<K, V>> reversedComparingByKey() {
        return new Comparator<Map.Entry<K, V>>() {
            @Override
            public int compare(Map.Entry<K, V> a, Map.Entry<K, V> b) {
                return N.compare(b.getKey(), a.getKey());
            }
        };
    }

    @SuppressWarnings("rawtypes")
    public static <K, V extends Comparable> Comparator<Map.Entry<K, V>> comparingByValue() {
        return new Comparator<Map.Entry<K, V>>() {
            @Override
            public int compare(Map.Entry<K, V> a, Map.Entry<K, V> b) {
                return N.compare(a.getValue(), b.getValue());
            }
        };
    }

    @SuppressWarnings("rawtypes")
    public static <K, V extends Comparable> Comparator<Map.Entry<K, V>> reversedComparingByValue() {
        return new Comparator<Map.Entry<K, V>>() {
            @Override
            public int compare(Map.Entry<K, V> a, Map.Entry<K, V> b) {
                return N.compare(b.getValue(), a.getValue());
            }
        };
    }

    public static <K, V> Comparator<Map.Entry<K, V>> comparingByKey(final Comparator<? super K> cmp) {
        N.checkArgNotNull(cmp);

        return new Comparator<Map.Entry<K, V>>() {
            @Override
            public int compare(Map.Entry<K, V> a, Map.Entry<K, V> b) {
                return cmp.compare(a.getKey(), b.getKey());
            }
        };
    }

    public static <K, V> Comparator<Map.Entry<K, V>> comparingByValue(final Comparator<? super V> cmp) {
        N.checkArgNotNull(cmp);

        return new Comparator<Map.Entry<K, V>>() {
            @Override
            public int compare(Map.Entry<K, V> a, Map.Entry<K, V> b) {
                return cmp.compare(a.getValue(), b.getValue());
            }
        };
    }

    @Deprecated
    public static <K, V> Comparator<Map.Entry<K, V>> reversedComparingByKey(final Comparator<? super K> cmp) {
        N.checkArgNotNull(cmp);

        return new Comparator<Map.Entry<K, V>>() {
            @Override
            public int compare(Map.Entry<K, V> a, Map.Entry<K, V> b) {
                return cmp.compare(b.getKey(), a.getKey());
            }
        };
    }

    @Deprecated
    public static <K, V> Comparator<Map.Entry<K, V>> reversedComparingByValue(final Comparator<? super V> cmp) {
        N.checkArgNotNull(cmp);

        return new Comparator<Map.Entry<K, V>>() {
            @Override
            public int compare(Map.Entry<K, V> a, Map.Entry<K, V> b) {
                return cmp.compare(b.getValue(), a.getValue());
            }
        };
    }

    public static <T extends CharSequence> Comparator<T> comparingByLength() {
        return (Comparator<T>) COMPARING_BY_LENGTH;
    }

    @SuppressWarnings("rawtypes")
    public static <T extends Collection> Comparator<T> comparingBySize() {
        return (Comparator<T>) COMPARING_BY_SIZE;
    }

    @SuppressWarnings("rawtypes")
    static <T extends Map> Comparator<T> comparingBySizee() {
        return (Comparator<T>) COMPARING_BY_SIZEE;
    }

}
