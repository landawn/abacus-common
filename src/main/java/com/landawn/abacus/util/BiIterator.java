/*
 * Copyright (c) 2018, Haiyang Li.
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import com.landawn.abacus.util.Fn.Suppliers;
import com.landawn.abacus.util.function.BiConsumer;
import com.landawn.abacus.util.function.BiFunction;
import com.landawn.abacus.util.function.BooleanSupplier;
import com.landawn.abacus.util.function.Consumer;
import com.landawn.abacus.util.function.IndexedConsumer;
import com.landawn.abacus.util.stream.Stream;

/**
 *
 * @author Haiyang Li
 * @param <A>
 * @param <B>
 * @since 1.2.10
 */
public abstract class BiIterator<A, B> extends ImmutableIterator<Pair<A, B>> {

    @SuppressWarnings("rawtypes")
    private static final BiIterator EMPTY = new BiIterator() {
        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public Object next() {
            throw new NoSuchElementException();
        }

        @Override
        public void forEachRemaining(Throwables.BiConsumer action) throws Exception {
            N.checkArgNotNull(action);
        }

        @Override
        public ObjIterator map(BiFunction mapper) {
            N.checkArgNotNull(mapper);

            return ObjIterator.empty();
        }
    };

    /**
     *
     * @param <A>
     * @param <B>
     * @return
     */
    public static <A, B> BiIterator<A, B> empty() {
        return EMPTY;
    }

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param map
     * @return
     */
    public static <K, V> BiIterator<K, V> of(final Map<K, V> map) {
        if (N.isNullOrEmpty(map)) {
            return empty();
        }

        return of(map.entrySet().iterator());
    }

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param iter
     * @return
     */
    public static <K, V> BiIterator<K, V> of(final Iterator<Map.Entry<K, V>> iter) {
        if (iter == null) {
            return empty();
        }

        return new BiIterator<K, V>() {

            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public Pair<K, V> next() {
                return Pair.from(iter.next());
            }

            @Override
            public <E extends Exception> void forEachRemaining(final Throwables.BiConsumer<? super K, ? super V, E> action) throws E {
                N.checkArgNotNull(action);

                Map.Entry<K, V> entry = null;

                while (iter.hasNext()) {
                    entry = iter.next();
                    action.accept(entry.getKey(), entry.getValue());
                }
            }

            @Override
            public <R> ObjIterator<R> map(final BiFunction<? super K, ? super V, R> mapper) {
                N.checkArgNotNull(mapper);

                return new ObjIterator<R>() {
                    private Map.Entry<K, V> entry = null;

                    @Override
                    public boolean hasNext() {
                        return iter.hasNext();
                    }

                    @Override
                    public R next() {
                        entry = iter.next();

                        return mapper.apply(entry.getKey(), entry.getValue());
                    }
                };
            }
        };
    }

    /**
     * Returns an infinite {@code BiIterator}.
     *
     * @param <A>
     * @param <B>
     * @param output transfer the next values.
     * @return
     */
    public static <A, B> BiIterator<A, B> generate(final Consumer<Pair<A, B>> output) {
        return generate(BooleanSupplier.TRUE, output);
    }

    /**
     *
     * @param <A>
     * @param <B>
     * @param hasNext
     * @param output
     * @return
     */
    public static <A, B> BiIterator<A, B> generate(final BooleanSupplier hasNext, final Consumer<Pair<A, B>> output) {
        N.checkArgNotNull(hasNext);
        N.checkArgNotNull(output);

        return new BiIterator<A, B>() {
            private final Pair<A, B> tmp = new Pair<A, B>();

            @Override
            public boolean hasNext() {
                return hasNext.getAsBoolean();
            }

            @Override
            public Pair<A, B> next() {
                if (hasNext() == false) {
                    throw new NoSuchElementException();
                }

                output.accept(tmp);

                return Pair.of(tmp.left, tmp.right);
            }

            @Override
            public <E extends Exception> void forEachRemaining(final Throwables.BiConsumer<? super A, ? super B, E> action) throws E {
                N.checkArgNotNull(action);

                while (hasNext.getAsBoolean()) {
                    output.accept(tmp);

                    action.accept(tmp.left, tmp.right);
                }
            }

            @Override
            public <R> ObjIterator<R> map(final BiFunction<? super A, ? super B, R> mapper) {
                N.checkArgNotNull(mapper);

                return new ObjIterator<R>() {
                    @Override
                    public boolean hasNext() {
                        return hasNext.getAsBoolean();
                    }

                    @Override
                    public R next() {
                        if (hasNext() == false) {
                            throw new NoSuchElementException();
                        }

                        output.accept(tmp);

                        return mapper.apply(tmp.left, tmp.right);
                    }
                };
            }
        };
    }

    /**
     *
     * @param <A>
     * @param <B>
     * @param fromIndex
     * @param toIndex
     * @param output
     * @return
     */
    public static <A, B> BiIterator<A, B> generate(final int fromIndex, final int toIndex, final IndexedConsumer<Pair<A, B>> output) {
        N.checkFromToIndex(fromIndex, toIndex, Integer.MAX_VALUE);
        N.checkArgNotNull(output);

        return new BiIterator<A, B>() {
            private final MutableInt cursor = MutableInt.of(fromIndex);
            private final Pair<A, B> tmp = new Pair<A, B>();

            @Override
            public boolean hasNext() {
                return cursor.value() < toIndex;
            }

            @Override
            public Pair<A, B> next() {
                if (hasNext() == false) {
                    throw new NoSuchElementException();
                }

                output.accept(cursor.getAndIncrement(), tmp);

                return Pair.of(tmp.left, tmp.right);
            }

            @Override
            public <E extends Exception> void forEachRemaining(final Throwables.BiConsumer<? super A, ? super B, E> action) throws E {
                N.checkArgNotNull(action);

                while (cursor.value() < toIndex) {
                    output.accept(cursor.getAndIncrement(), tmp);

                    action.accept(tmp.left, tmp.right);
                }
            }

            @Override
            public <R> ObjIterator<R> map(final BiFunction<? super A, ? super B, R> mapper) {
                N.checkArgNotNull(mapper);

                return new ObjIterator<R>() {
                    @Override
                    public boolean hasNext() {
                        return cursor.value() < toIndex;
                    }

                    @Override
                    public R next() {
                        if (hasNext() == false) {
                            throw new NoSuchElementException();
                        }

                        output.accept(cursor.getAndIncrement(), tmp);

                        return mapper.apply(tmp.left, tmp.right);
                    }
                };
            }
        };
    }

    /**
     *
     * @param <A>
     * @param <B>
     * @param a
     * @param b
     * @return
     */
    public static <A, B> BiIterator<A, B> zip(final A[] a, final B[] b) {
        return zip(Array.asList(a), Array.asList(b));
    }

    /**
     *
     * @param <A>
     * @param <B>
     * @param a
     * @param b
     * @param valueForNoneA
     * @param valueForNoneB
     * @return
     */
    public static <A, B> BiIterator<A, B> zip(final A[] a, final B[] b, final A valueForNoneA, final B valueForNoneB) {
        return zip(Array.asList(a), Array.asList(b), valueForNoneA, valueForNoneB);
    }

    /**
     *
     * @param <A>
     * @param <B>
     * @param a
     * @param b
     * @return
     */
    public static <A, B> BiIterator<A, B> zip(final Collection<A> a, final Collection<B> b) {
        return zip(a == null ? null : a.iterator(), b == null ? null : b.iterator());
    }

    /**
     *
     * @param <A>
     * @param <B>
     * @param a
     * @param b
     * @param valueForNoneA
     * @param valueForNoneB
     * @return
     */
    public static <A, B> BiIterator<A, B> zip(final Collection<A> a, final Collection<B> b, final A valueForNoneA, final B valueForNoneB) {
        return zip(a == null ? null : a.iterator(), b == null ? null : b.iterator(), valueForNoneA, valueForNoneB);
    }

    /**
     *
     * @param <A>
     * @param <B>
     * @param iterA
     * @param iterB
     * @return
     */
    public static <A, B> BiIterator<A, B> zip(final Iterator<A> iterA, final Iterator<B> iterB) {
        if (iterA == null || iterB == null) {
            return empty();
        }

        return new BiIterator<A, B>() {
            @Override
            public boolean hasNext() {
                return iterA.hasNext() && iterB.hasNext();
            }

            @Override
            public Pair<A, B> next() {
                if (hasNext() == false) {
                    throw new NoSuchElementException();
                }

                return Pair.of(iterA.next(), iterB.next());
            }

            @Override
            public <E extends Exception> void forEachRemaining(final Throwables.BiConsumer<? super A, ? super B, E> action) throws E {
                N.checkArgNotNull(action);

                while (iterA.hasNext() && iterB.hasNext()) {
                    action.accept(iterA.next(), iterB.next());
                }
            }

            @Override
            public <R> ObjIterator<R> map(final BiFunction<? super A, ? super B, R> mapper) {
                N.checkArgNotNull(mapper);

                return new ObjIterator<R>() {
                    @Override
                    public boolean hasNext() {
                        return iterA.hasNext() && iterB.hasNext();
                    }

                    @Override
                    public R next() {
                        if (hasNext() == false) {
                            throw new NoSuchElementException();
                        }

                        return mapper.apply(iterA.next(), iterB.next());
                    }
                };
            }
        };
    }

    /**
     *
     * @param <A>
     * @param <B>
     * @param iterA
     * @param iterB
     * @param valueForNoneA
     * @param valueForNoneB
     * @return
     */
    public static <A, B> BiIterator<A, B> zip(final Iterator<A> iterA, final Iterator<B> iterB, final A valueForNoneA, final B valueForNoneB) {
        final Iterator<A> iter1 = iterA == null ? ObjIterator.<A> empty() : iterA;
        final Iterator<B> iter2 = iterB == null ? ObjIterator.<B> empty() : iterB;

        return new BiIterator<A, B>() {
            @Override
            public boolean hasNext() {
                return iter1.hasNext() || iter2.hasNext();
            }

            @Override
            public Pair<A, B> next() {
                if (hasNext() == false) {
                    throw new NoSuchElementException();
                }

                return Pair.of(iter1.hasNext() ? iter1.next() : valueForNoneA, iter2.hasNext() ? iter2.next() : valueForNoneB);
            }

            @Override
            public <E extends Exception> void forEachRemaining(final Throwables.BiConsumer<? super A, ? super B, E> action) throws E {
                N.checkArgNotNull(action);

                while (iter1.hasNext() || iter2.hasNext()) {
                    action.accept(iter1.hasNext() ? iter1.next() : valueForNoneA, iter2.hasNext() ? iter2.next() : valueForNoneB);
                }
            }

            @Override
            public <R> ObjIterator<R> map(final BiFunction<? super A, ? super B, R> mapper) {
                N.checkArgNotNull(mapper);

                return new ObjIterator<R>() {
                    @Override
                    public boolean hasNext() {
                        return iter1.hasNext() || iter2.hasNext();
                    }

                    @Override
                    public R next() {
                        if (hasNext() == false) {
                            throw new NoSuchElementException();
                        }

                        return mapper.apply(iter1.hasNext() ? iter1.next() : valueForNoneA, iter2.hasNext() ? iter2.next() : valueForNoneB);
                    }
                };
            }
        };
    }

    /**
     *
     * @param <T>
     * @param <L>
     * @param <R>
     * @param iter
     * @param unzip output parameter.
     * @return
     */
    public static <T, L, R> BiIterator<L, R> unzip(final Iterator<? extends T> iter, final BiConsumer<? super T, Pair<L, R>> unzip) {
        if (iter == null) {
            return BiIterator.empty();
        }

        final BooleanSupplier hasNext = new BooleanSupplier() {
            @Override
            public boolean getAsBoolean() {
                return iter.hasNext();
            }
        };

        final Consumer<Pair<L, R>> output = new Consumer<Pair<L, R>>() {
            @Override
            public void accept(Pair<L, R> out) {
                unzip.accept(iter.next(), out);
            }
        };

        return BiIterator.generate(hasNext, output);
    }

    /**
     * For each remaining.
     *
     * @param <E>
     * @param action
     * @throws E the e
     */
    public abstract <E extends Exception> void forEachRemaining(final Throwables.BiConsumer<? super A, ? super B, E> action) throws E;

    /**
     * It's preferred to call <code>forEachRemaining(Try.BiConsumer)</code> to avoid the create the unnecessary <code>Pair</code> Objects.
     *
     * @param action
     * @deprecated
     */
    @Override
    @Deprecated
    public void forEachRemaining(java.util.function.Consumer<? super Pair<A, B>> action) {
        super.forEachRemaining(action);
    }

    /**
     *
     * @param <R>
     * @param mapper
     * @return
     */
    public abstract <R> ObjIterator<R> map(final BiFunction<? super A, ? super B, R> mapper);

    /**
     *
     * @param <R>
     * @param mapper
     * @return
     */
    public <R> Stream<R> stream(final BiFunction<? super A, ? super B, R> mapper) {
        N.checkArgNotNull(mapper);

        return Stream.of(map(mapper));
    }

    public Pair<A, B>[] toArray() {
        return toArray(new Pair[0]);
    }

    public <T> T[] toArray(final T[] a) {
        return toList().toArray(a);
    }

    public List<Pair<A, B>> toList() {
        return toCollection(Suppliers.<Pair<A, B>> ofList());
    }
}
