/*
 * Copyright (C) 2017 HaiYang Li
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

import java.util.Collection;
import java.util.Map;
import java.util.function.Supplier;

import com.landawn.abacus.annotation.Beta;

/**
 * This class is mainly designed for functional programming.
 * Generally the traditional "{@code if-else}" or ternary operator: "{@code ? : }" is preferred over this class.
 *
 * @see N#ifOrEmpty(boolean, Throwables.Supplier)
 * @see N#ifOrElse(boolean, Throwables.Runnable, Throwables.Runnable)
 * @see N#ifNotNull(Object, Throwables.Consumer)
 * @see N#ifNotEmpty(CharSequence, Throwables.Consumer)
 * @see N#ifNotEmpty(Collection, Throwables.Consumer)
 * @see N#ifNotEmpty(Map, Throwables.Consumer)
 *
 */
@Beta
public final class If {

    private static final If TRUE = new If(true);

    private static final If FALSE = new If(false);

    private final boolean b;

    If(final boolean b) {
        this.b = b;
    }

    /**
     * Returns an instance of If based on the provided boolean value.
     *
     * @param b
     * @return
     */
    public static If is(final boolean b) {
        return b ? TRUE : FALSE;
    }

    /**
     * Returns an instance of If based on the provided boolean value.
     *
     * @param b
     * @return
     */
    public static If not(final boolean b) {
        return b ? FALSE : TRUE;
    }

    /**
     * {@code true} for {@code index >= 0}, {@code false} for {@code index < 0}.
     *
     * @param index
     * @return
     */
    public static If exists(final int index) {
        return index >= 0 ? TRUE : FALSE;
    }

    /**
     * Checks if is {@code null} or empty.
     *
     * @param s
     * @return
     */
    public static If isEmpty(final CharSequence s) {
        return is(Strings.isEmpty(s));
    }

    /**
     * Checks if is {@code null} or empty.
     *
     * @param a
     * @return
     */
    public static If isEmpty(final boolean[] a) {
        return is(N.isEmpty(a));
    }

    /**
     * Checks if is {@code null} or empty.
     *
     * @param a
     * @return
     */
    public static If isEmpty(final char[] a) {
        return is(N.isEmpty(a));
    }

    /**
     * Checks if is {@code null} or empty.
     *
     * @param a
     * @return
     */
    public static If isEmpty(final byte[] a) {
        return is(N.isEmpty(a));
    }

    /**
     * Checks if is {@code null} or empty.
     *
     * @param a
     * @return
     */
    public static If isEmpty(final short[] a) {
        return is(N.isEmpty(a));
    }

    /**
     * Checks if is {@code null} or empty.
     *
     * @param a
     * @return
     */
    public static If isEmpty(final int[] a) {
        return is(N.isEmpty(a));
    }

    /**
     * Checks if is {@code null} or empty.
     *
     * @param a
     * @return
     */
    public static If isEmpty(final long[] a) {
        return is(N.isEmpty(a));
    }

    /**
     * Checks if is {@code null} or empty.
     *
     * @param a
     * @return
     */
    public static If isEmpty(final float[] a) {
        return is(N.isEmpty(a));
    }

    /**
     * Checks if is {@code null} or empty.
     *
     * @param a
     * @return
     */
    public static If isEmpty(final double[] a) {
        return is(N.isEmpty(a));
    }

    /**
     * Checks if is {@code null} or empty.
     *
     * @param a
     * @return
     */
    public static If isEmpty(final Object[] a) {
        return is(N.isEmpty(a));
    }

    /**
     * Checks if is {@code null} or empty.
     *
     * @param c
     * @return
     */
    public static If isEmpty(final Collection<?> c) {
        return is(N.isEmpty(c));
    }

    /**
     * Checks if is {@code null} or empty.
     *
     * @param m
     * @return
     */
    public static If isEmpty(final Map<?, ?> m) {
        return is(N.isEmpty(m));
    }

    /**
     * Checks if is {@code null} or empty.
     *
     * @param list
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static If isEmpty(final PrimitiveList list) {
        return is(N.isEmpty(list));
    }

    /**
     * Checks if is {@code null} or empty.
     *
     * @param s
     * @return
     */
    public static If isEmpty(final Multiset<?> s) {
        return is(N.isEmpty(s));
    }

    /**
     * Checks if is {@code null} or empty.
     *
     * @param m
     * @return
     */
    public static If isEmpty(final Multimap<?, ?, ?> m) {
        return is(N.isEmpty(m));
    }

    /**
     * Checks if is {@code null} or empty or blank.
     *
     * @param s
     * @return
     */
    // DON'T change 'OrEmptyOrBlank' to 'OrBlank' because of the occurring order in the auto-completed context menu.
    public static If isBlank(final CharSequence s) {
        return is(Strings.isBlank(s));
    }

    /**
     * Not {@code null} or empty.
     *
     * @param s
     * @return
     */
    public static If notEmpty(final CharSequence s) {
        return is(Strings.isNotEmpty(s));
    }

    /**
     * Not {@code null} or empty.
     *
     * @param a
     * @return
     */
    public static If notEmpty(final boolean[] a) {
        return is(N.notEmpty(a));
    }

    /**
     * Not {@code null} or empty.
     *
     * @param a
     * @return
     */
    public static If notEmpty(final char[] a) {
        return is(N.notEmpty(a));
    }

    /**
     * Not {@code null} or empty.
     *
     * @param a
     * @return
     */
    public static If notEmpty(final byte[] a) {
        return is(N.notEmpty(a));
    }

    /**
     * Not {@code null} or empty.
     *
     * @param a
     * @return
     */
    public static If notEmpty(final short[] a) {
        return is(N.notEmpty(a));
    }

    /**
     * Not {@code null} or empty.
     *
     * @param a
     * @return
     */
    public static If notEmpty(final int[] a) {
        return is(N.notEmpty(a));
    }

    /**
     * Not {@code null} or empty.
     *
     * @param a
     * @return
     */
    public static If notEmpty(final long[] a) {
        return is(N.notEmpty(a));
    }

    /**
     * Not {@code null} or empty.
     *
     * @param a
     * @return
     */
    public static If notEmpty(final float[] a) {
        return is(N.notEmpty(a));
    }

    /**
     * Not {@code null} or empty.
     *
     * @param a
     * @return
     */
    public static If notEmpty(final double[] a) {
        return is(N.notEmpty(a));
    }

    /**
     * Not {@code null} or empty.
     *
     * @param a
     * @return
     */
    public static If notEmpty(final Object[] a) {
        return is(N.notEmpty(a));
    }

    /**
     * Not {@code null} or empty.
     *
     * @param c
     * @return
     */
    public static If notEmpty(final Collection<?> c) {
        return is(N.notEmpty(c));
    }

    /**
     * Not {@code null} or empty.
     *
     * @param m
     * @return
     */
    public static If notEmpty(final Map<?, ?> m) {
        return is(N.notEmpty(m));
    }

    /**
     * Not {@code null} or empty.
     *
     * @param list
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static If notEmpty(final PrimitiveList list) {
        return is(N.notEmpty(list));
    }

    /**
     * Not {@code null} or empty.
     *
     * @param s
     * @return
     */
    public static If notEmpty(final Multiset<?> s) {
        return is(N.notEmpty(s));
    }

    /**
     * Not {@code null} or empty.
     *
     * @param m
     * @return
     */
    public static If notEmpty(final Multimap<?, ?, ?> m) {
        return is(N.notEmpty(m));
    }

    /**
     * Not {@code null} or empty or blank.
     *
     * @param s
     * @return
     */
    // DON'T change 'OrEmptyOrBlank' to 'OrBlank' because of the occurring order in the auto-completed context menu.
    public static If notBlank(final CharSequence s) {
        return is(Strings.isNotBlank(s));
    }

    //    public <E extends Throwable> void thenRun(final Throwables.Runnable<E> cmd) throws E {
    //        if (b) {
    //            cmd.run();
    //        }
    //    }
    //
    //    public <T, E extends Throwable> void thenRun(final T init, final Try.Consumer<? super T, E> action) throws E {
    //        if (b) {
    //            action.accept(init);
    //        }
    //    }
    //
    //    public <T, E extends Throwable> Nullable<T> thenCall(final Try.Callable<? extends T, E> callable) throws E {
    //        return b ? Nullable.of(callable.call()) : Nullable.<T> empty();
    //    }
    //
    //    public <T, R, E extends Throwable> Nullable<R> thenCall(final T init, final Try.Function<? super T, ? extends R, E> func) throws E {
    //        return b ? Nullable.of(func.apply(init)) : Nullable.<R> empty();
    //    }

    /**
     * Then do nothing.
     *
     * @return
     */
    public OrElse thenDoNothing() {
        return OrElse.of(b);
    }

    /**
     *
     *
     * @param <E>
     * @param cmd
     * @return
     * @throws IllegalArgumentException
     * @throws E the e
     */
    public <E extends Throwable> OrElse then(final Throwables.Runnable<E> cmd) throws IllegalArgumentException, E {
        N.checkArgNotNull(cmd);

        if (b) {
            cmd.run();
        }

        return OrElse.of(b);
    }

    /**
     *
     *
     * @param <T>
     * @param <E>
     * @param init
     * @param action
     * @return
     * @throws IllegalArgumentException
     * @throws E the e
     */
    @Beta
    public <T, E extends Throwable> OrElse then(final T init, final Throwables.Consumer<? super T, E> action) throws IllegalArgumentException, E {
        N.checkArgNotNull(action);

        if (b) {
            action.accept(init);
        }

        return OrElse.of(b);
    }

    /**
     *
     *
     * @param <E>
     * @param exceptionSupplier
     * @return
     * @throws IllegalArgumentException
     * @throws E the e
     */
    public <E extends Throwable> OrElse thenThrow(final Supplier<? extends E> exceptionSupplier) throws IllegalArgumentException, E {
        N.checkArgNotNull(exceptionSupplier);

        if (b) {
            throw exceptionSupplier.get();
        }

        return OrElse.of(b);
    }

    //    public <T, E extends Throwable> Nullable<T> then(final Try.Callable<T, E> callable) throws E {
    //        return b ? Nullable.of(callable.call()) : Nullable.<T> empty();
    //    }
    //
    //    public <T, R, E extends Throwable> Nullable<R> then(final T init, final Try.Function<? super T, ? extends R, E> func) throws E {
    //        return b ? Nullable.of(func.apply(init)) : Nullable.<R> empty();
    //    }

    /**
     * The Class OrElse.
     */
    public static final class OrElse {
        /**
         * For internal only
         */
        @SuppressWarnings("hiding")
        public static final OrElse TRUE = new OrElse(true);

        /**
         * For internal only
         */
        @SuppressWarnings("hiding")
        public static final OrElse FALSE = new OrElse(false);

        /** The b. */
        private final boolean isIfTrue;

        /**
         * Instantiates a new or.
         *
         * @param b
         */
        OrElse(final boolean b) {
            isIfTrue = b;
        }

        /**
         *
         * @param b
         * @return
         */
        static OrElse of(final boolean b) {
            return b ? TRUE : FALSE;
        }

        /**
         * Or else do nothing.
         */
        void orElseDoNothing() {
            // Do nothing.
        }

        /**
         *
         *
         * @param <E>
         * @param cmd
         * @throws IllegalArgumentException
         * @throws E the e
         */
        public <E extends Throwable> void orElse(final Throwables.Runnable<E> cmd) throws IllegalArgumentException, E {
            N.checkArgNotNull(cmd);

            if (!isIfTrue) {
                cmd.run();
            }
        }

        /**
         *
         *
         * @param <T>
         * @param <E>
         * @param init
         * @param action
         * @throws IllegalArgumentException
         * @throws E the e
         */
        @Beta
        public <T, E extends Throwable> void orElse(final T init, final Throwables.Consumer<? super T, E> action) throws IllegalArgumentException, E {
            N.checkArgNotNull(action);

            if (!isIfTrue) {
                action.accept(init);
            }
        }

        /**
         * Or else throw.
         *
         * @param <E>
         * @param exceptionSupplier
         * @throws IllegalArgumentException
         * @throws E the e
         */
        public <E extends Throwable> void orElseThrow(final Supplier<? extends E> exceptionSupplier) throws IllegalArgumentException, E {
            N.checkArgNotNull(exceptionSupplier);

            if (!isIfTrue) {
                throw exceptionSupplier.get();
            }
        }
    }

    //    /**
    //     * This class is mainly designed for functional programming.
    //     * Generally the traditional "{@code if-else}" or ternary operator: "{@code ? : }" is preferred over this class.
    //     *
    //     *
    //     */
    //    @Beta
    //    @Deprecated
    //    public static final class IF {
    //        private static final IF TRUE = new IF(true);
    //        private static final IF FALSE = new IF(false);
    //
    //        @SuppressWarnings("rawtypes")
    //        private static final Or FALSE_OR = new FalseOr();
    //
    //        private final boolean b;
    //
    //        IF(boolean b) {
    //            this.b = b;
    //        }
    //
    //        public static IF is(final boolean b) {
    //            return b ? TRUE : FALSE;
    //        }
    //
    //        public static IF not(final boolean b) {
    //            return b ? FALSE : TRUE;
    //        }
    //
    //        /**
    //         * {@code true} for {@code index >= 0}, {@code false} for {@code index < 0}.
    //         *
    //         * @param index
    //         * @return
    //         */
    //        public static IF exists(final int index) {
    //            return index >= 0 ? TRUE : FALSE;
    //        }
    //
    //        public static IF isEmpty(final CharSequence s) {
    //            return is(N.isEmpty(s));
    //        }
    //
    //        public static IF isEmpty(final boolean[] a) {
    //            return is(N.isEmpty(a));
    //        }
    //
    //        public static IF isEmpty(final char[] a) {
    //            return is(N.isEmpty(a));
    //        }
    //
    //        public static IF isEmpty(final byte[] a) {
    //            return is(N.isEmpty(a));
    //        }
    //
    //        public static IF isEmpty(final short[] a) {
    //            return is(N.isEmpty(a));
    //        }
    //
    //        public static IF isEmpty(final int[] a) {
    //            return is(N.isEmpty(a));
    //        }
    //
    //        public static IF isEmpty(final long[] a) {
    //            return is(N.isEmpty(a));
    //        }
    //
    //        public static IF isEmpty(final float[] a) {
    //            return is(N.isEmpty(a));
    //        }
    //
    //        public static IF isEmpty(final double[] a) {
    //            return is(N.isEmpty(a));
    //        }
    //
    //        public static IF isEmpty(final Object[] a) {
    //            return is(N.isEmpty(a));
    //        }
    //
    //        public static IF isEmpty(final Collection<?> c) {
    //            return is(N.isEmpty(c));
    //        }
    //
    //        public static IF isEmpty(final Map<?, ?> m) {
    //            return is(N.isEmpty(m));
    //        }
    //
    //        @SuppressWarnings("rawtypes")
    //        public static IF isEmpty(final PrimitiveList list) {
    //            return is(N.isEmpty(list));
    //        }
    //
    //        public static IF isEmpty(final Multiset<?> s) {
    //            return is(N.isEmpty(s));
    //        }
    //
    //        public static IF isEmpty(final LongMultiset<?> s) {
    //            return is(N.isEmpty(s));
    //        }
    //
    //        public static IF isEmpty(final Multimap<?, ?, ?> m) {
    //            return is(N.isEmpty(m));
    //        }
    //
    //        // DON'T change 'OrEmptyOrBlank' to 'OrBlank' because of the occurring order in the auto-completed context menu.
    //        public static IF isBlank(final CharSequence s) {
    //            return is(N.isBlank(s));
    //        }
    //
    //        public static IF notEmpty(final CharSequence s) {
    //            return is(N.notEmpty(s));
    //        }
    //
    //        public static IF notEmpty(final boolean[] a) {
    //            return is(N.notEmpty(a));
    //        }
    //
    //        public static IF notEmpty(final char[] a) {
    //            return is(N.notEmpty(a));
    //        }
    //
    //        public static IF notEmpty(final byte[] a) {
    //            return is(N.notEmpty(a));
    //        }
    //
    //        public static IF notEmpty(final short[] a) {
    //            return is(N.notEmpty(a));
    //        }
    //
    //        public static IF notEmpty(final int[] a) {
    //            return is(N.notEmpty(a));
    //        }
    //
    //        public static IF notEmpty(final long[] a) {
    //            return is(N.notEmpty(a));
    //        }
    //
    //        public static IF notEmpty(final float[] a) {
    //            return is(N.notEmpty(a));
    //        }
    //
    //        public static IF notEmpty(final double[] a) {
    //            return is(N.notEmpty(a));
    //        }
    //
    //        public static IF notEmpty(final Object[] a) {
    //            return is(N.notEmpty(a));
    //        }
    //
    //        public static IF notEmpty(final Collection<?> c) {
    //            return is(N.notEmpty(c));
    //        }
    //
    //        public static IF notEmpty(final Map<?, ?> m) {
    //            return is(N.notEmpty(m));
    //        }
    //
    //        @SuppressWarnings("rawtypes")
    //        public static IF notEmpty(final PrimitiveList list) {
    //            return is(N.notEmpty(list));
    //        }
    //
    //        public static IF notEmpty(final Multiset<?> s) {
    //            return is(N.notEmpty(s));
    //        }
    //
    //        public static IF notEmpty(final LongMultiset<?> s) {
    //            return is(N.notEmpty(s));
    //        }
    //
    //        public static IF notEmpty(final Multimap<?, ?, ?> m) {
    //            return is(N.notEmpty(m));
    //        }
    //
    //        // DON'T change 'OrEmptyOrBlank' to 'OrBlank' because of the occurring order in the auto-completed context menu.
    //        public static IF notBlank(final CharSequence s) {
    //            return is(N.notBlank(s));
    //        }
    //
    //        public <T, E extends Throwable> Nullable<T> thenGet(Try.Supplier<T, E> supplier) throws E {
    //            return b ? Nullable.of(supplier.get()) : Nullable.<T> empty();
    //        }
    //
    //        public <T, U, E extends Throwable> Nullable<T> thenApply(final U init, final Try.Function<? super U, T, E> func) throws E {
    //            return b ? Nullable.of(func.apply(init)) : Nullable.<T> empty();
    //        }
    //
    //        public <T, E extends Throwable> Or<T> then(final Try.Callable<T, E> callable) throws E {
    //            N.checkArgNotNull(callable);
    //
    //            return b ? new TrueOr<>(callable.call()) : FALSE_OR;
    //        }
    //
    //        public <T, U, E extends Throwable> Or<T> then(final U init, final Try.Function<? super U, T, E> func) throws E {
    //            N.checkArgNotNull(func);
    //
    //            return b ? new TrueOr<>(func.apply(init)) : FALSE_OR;
    //        }
    //
    //        public abstract static class Or<T> {
    //            Or() {
    //            }
    //
    //            public abstract <E extends Throwable> T orElse(final Try.Callable<T, E> callable) throws E;
    //
    //            public abstract <U, E extends Throwable> T orElse(final U init, final Try.Function<? super U, T, E> func) throws E;
    //
    //            public abstract <E extends Throwable> T orElseThrow(final Supplier<? extends E> exceptionSupplier) throws E;
    //        }
    //
    //        static final class TrueOr<T> extends Or<T> {
    //            private final T result;
    //
    //            TrueOr(final T result) {
    //                this.result = result;
    //            }
    //
    //            @Override
    //            public <E extends Throwable> T orElse(final Try.Callable<T, E> callable) throws E {
    //                N.checkArgNotNull(callable);
    //
    //                return result;
    //            }
    //
    //            @Override
    //            public <U, E extends Throwable> T orElse(final U init, final Try.Function<? super U, T, E> func) throws E {
    //                N.checkArgNotNull(func);
    //
    //                return result;
    //            }
    //
    //            @Override
    //            public <E extends Throwable> T orElseThrow(final Supplier<? extends E> exceptionSupplier) throws E {
    //                N.checkArgNotNull(exceptionSupplier);
    //
    //                return result;
    //            }
    //        }
    //
    //        static final class FalseOr<T> extends Or<T> {
    //            FalseOr() {
    //            }
    //
    //            @Override
    //            public <E extends Throwable> T orElse(final Try.Callable<T, E> callable) throws E {
    //                return callable.call();
    //            }
    //
    //            @Override
    //            public <U, E extends Throwable> T orElse(final U init, final Try.Function<? super U, T, E> func) throws E {
    //                return func.apply(init);
    //            }
    //
    //            @Override
    //            public <E extends Throwable> T orElseThrow(final Supplier<? extends E> exceptionSupplier) throws E {
    //                throw exceptionSupplier.get();
    //            }
    //        }
    //    }
}
