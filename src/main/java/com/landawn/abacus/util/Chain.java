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

import java.util.Comparator;

/**
 * 
 * @author HaiYang Li
 *
 */
public final class Chain {
    private Chain() {
        // singleton
    }

    /**
     * Compares two comparable objects as specified by {@link
     * Comparable#compareTo}, <i>if</i> the result of this comparison chain
     * has not already been determined.
     * 
     * @param left
     * @param right
     * @return {@code ComparisonChain}
     */
    public static <T extends Comparable<? super T>> ComparisonChain compare(T left, T right) {
        return new ComparisonChain().compare(left, right);
    }

    /**
     * Compares two objects using a comparator, <i>if</i> the result of this
     * comparison chain has not already been determined.
     * 
     * @param left
     * @param right
     * @return {@code ComparisonChain}
     */
    public static <T> ComparisonChain compare(T left, T right, Comparator<T> comparator) {
        return new ComparisonChain().compare(left, right, comparator);
    }

    /**
     * 
     * @param left
     * @param right
     * @param func
     * @return
     * @throws E
     */
    public static <T, E extends Exception> ComparisonChain compare(T left, T right, Try.BiFunction<? super T, ? super T, Integer, E> func) throws E {
        return new ComparisonChain().compare(left, right, func);
    }

    /**
     * Compares two {@code boolean} values, considering {@code false} to be less
     * than {@code true}, <i>if</i> the result of this comparison chain has not
     * already been determined.
     * 
     * @param left
     * @param right
     * @return {@code ComparisonChain}
     *
     */
    public static ComparisonChain compareFalseLess(boolean left, boolean right) {
        return new ComparisonChain().compareFalseLess(left, right);
    }

    /**
     * Compares two {@code boolean} values, considering {@code true} to be less
     * than {@code false}, <i>if</i> the result of this comparison chain has not
     * already been determined.
     * 
     * @param left
     * @param right
     * @return {@code ComparisonChain}
     */
    public static ComparisonChain compareTrueLess(boolean left, boolean right) {
        return new ComparisonChain().compareTrueLess(left, right);
    }

    /**
     * Compares two {@code char} values as specified by {@link N#compare},
     * <i>if</i> the result of this comparison chain has not already been
     * determined.
     * 
     * @param left
     * @param right
     * @return {@code ComparisonChain}
     */
    public static ComparisonChain compare(char left, char right) {
        return new ComparisonChain().compare(left, right);
    }

    /**
     * Compares two {@code byte} values as specified by {@link N#compare},
     * <i>if</i> the result of this comparison chain has not already been
     * determined.
     * 
     * @param left
     * @param right
     * @return {@code ComparisonChain}
     */
    public static ComparisonChain compare(byte left, byte right) {
        return new ComparisonChain().compare(left, right);
    }

    /**
     * Compares two {@code short} values as specified by {@link N#compare},
     * <i>if</i> the result of this comparison chain has not already been
     * determined.
     * 
     * @param left
     * @param right
     * @return {@code ComparisonChain}
     */
    public static ComparisonChain compare(short left, short right) {
        return new ComparisonChain().compare(left, right);
    }

    /**
     * Compares two {@code int} values as specified by {@link N#compare},
     * <i>if</i> the result of this comparison chain has not already been
     * determined.
     * 
     * @param left
     * @param right
     * @return {@code ComparisonChain}
     */
    public static ComparisonChain compare(int left, int right) {
        return new ComparisonChain().compare(left, right);
    }

    /**
     * Compares two {@code long} values as specified by {@link N#compare},
     * <i>if</i> the result of this comparison chain has not already been
     * determined.
     * 
     * @param left
     * @param right
     * @return {@code ComparisonChain}
     */
    public static ComparisonChain compare(long left, long right) {
        return new ComparisonChain().compare(left, right);
    }

    /**
     * Compares two {@code float} values as specified by {@link
     * Float#compare}, <i>if</i> the result of this comparison chain has not
     * already been determined.
     * 
     * @param left
     * @param right
     * @return {@code ComparisonChain}
     */
    public static ComparisonChain compare(float left, float right) {
        return new ComparisonChain().compare(left, right);
    }

    /**
     * Compares two {@code double} values as specified by {@link
     * Double#compare}, <i>if</i> the result of this comparison chain has not
     * already been determined.
     * 
     * @param left
     * @param right
     * @return {@code ComparisonChain}
     */
    public static ComparisonChain compare(double left, double right) {
        return new ComparisonChain().compare(left, right);
    }

    /**
     * Compares two comparable objects as specified by {@link
     * N#equals(Object, Object)}, <i>if</i> the result of this equivalence chain
     * has not already been determined.
     * 
     * @param left
     * @param right
     * @return {@code EquivalenceChain}
     */
    public static EquivalenceChain equals(Object left, Object right) {
        return new EquivalenceChain().equals(left, right);
    }

    public static <T, E extends Exception> EquivalenceChain equals(T left, T right, Try.BiFunction<? super T, ? super T, Boolean, E> func) throws E {
        return new EquivalenceChain().equals(left, right, func);
    }

    /**
     * Compares two {@code boolean} values as specified by {@code left == right},
     * <i>if</i> the result of this equivalence chain has not already been
     * determined.
     * 
     * @param left
     * @param right
     * @return {@code EquivalenceChain}
     */
    public static EquivalenceChain equals(boolean left, boolean right) {
        return new EquivalenceChain().equals(left, right);
    }

    /**
     * Compares two {@code char} values as specified by {@code left == right},
     * <i>if</i> the result of this equivalence chain has not already been
     * determined.
     * 
     * @param left
     * @param right
     * @return {@code EquivalenceChain}
     */
    public static EquivalenceChain equals(char left, char right) {
        return new EquivalenceChain().equals(left, right);
    }

    /**
     * Compares two {@code byte} values as specified by {@code left == right},
     * <i>if</i> the result of this equivalence chain has not already been
     * determined.
     * 
     * @param left
     * @param right
     * @return {@code EquivalenceChain}
     */
    public static EquivalenceChain equals(byte left, byte right) {
        return new EquivalenceChain().equals(left, right);
    }

    /**
     * Compares two {@code short} values as specified by {@code left == right},
     * <i>if</i> the result of this equivalence chain has not already been
     * determined.
     * 
     * @param left
     * @param right
     * @return {@code EquivalenceChain}
     */
    public static EquivalenceChain equals(short left, short right) {
        return new EquivalenceChain().equals(left, right);
    }

    /**
     * Compares two {@code int} values as specified by {@code left == right},
     * <i>if</i> the result of this equivalence chain has not already been
     * determined.
     * 
     * @param left
     * @param right
     * @return {@code EquivalenceChain}
     */
    public static EquivalenceChain equals(int left, int right) {
        return new EquivalenceChain().equals(left, right);
    }

    /**
     * Compares two {@code long} values as specified by {@code left == right},
     * <i>if</i> the result of this equivalence chain has not already been
     * determined.
     * 
     * @param left
     * @param right
     * @return {@code EquivalenceChain}
     */
    public static EquivalenceChain equals(long left, long right) {
        return new EquivalenceChain().equals(left, right);
    }

    /**
     * Compares two {@code float} values as specified by {@link
     * Float#compare}, <i>if</i> the result of this equivalence chain has not
     * already been determined.
     * 
     * @param left
     * @param right
     * @return {@code EquivalenceChain}
     */
    public static EquivalenceChain equals(float left, float right) {
        return new EquivalenceChain().equals(left, right);
    }

    /**
     * Compares two {@code double} values as specified by {@link
     * Double#compare}, <i>if</i> the result of this equivalence chain has not
     * already been determined.
     * 
     * @param left
     * @param right
     * @return {@code EquivalenceChain}
     */
    public static EquivalenceChain equals(double left, double right) {
        return new EquivalenceChain().equals(left, right);
    }

    /**
     * Add the hash code of the specified {@code value} to result.
     * 
     * @param value
     * @return this
     */
    public static HashCodeChain hash(Object value) {
        return new HashCodeChain().hash(value);
    }

    public static <T, E extends Exception> HashCodeChain hash(T value, Try.ToIntFunction<? super T, E> func) throws E {
        return new HashCodeChain().hash(value, func);
    }

    /**
     * Add the hash code of the specified {@code value} to result.
     * 
     * @param value
     * @return this
     */
    public static HashCodeChain hash(boolean value) {
        return new HashCodeChain().hash(value);
    }

    /**
     * Add the hash code of the specified {@code value} to result.
     * 
     * @param value
     * @return this
     */
    public static HashCodeChain hash(char value) {
        return new HashCodeChain().hash(value);
    }

    /**
     * Add the hash code of the specified {@code value} to result.
     * 
     * @param value
     * @return this
     */
    public static HashCodeChain hash(byte value) {
        return new HashCodeChain().hash(value);
    }

    /**
     * Add the hash code of the specified {@code value} to result.
     * 
     * @param value
     * @return this
     */
    public static HashCodeChain hash(short value) {
        return new HashCodeChain().hash(value);
    }

    /**
     * Add the hash code of the specified {@code value} to result.
     * 
     * @param value
     * @return this
     */
    public static HashCodeChain hash(int value) {
        return new HashCodeChain().hash(value);
    }

    /**
     * Add the hash code of the specified {@code value} to result.
     * 
     * @param value
     * @return this
     */
    public static HashCodeChain hash(long value) {
        return new HashCodeChain().hash(value);
    }

    /**
     * Add the hash code of the specified {@code value} to result.
     * 
     * @param value
     * @return this
     */
    public static HashCodeChain hash(float value) {
        return new HashCodeChain().hash(value);
    }

    /**
     * Add the hash code of the specified {@code value} to result.
     * 
     * @param value
     * @return this
     */
    public static HashCodeChain hash(double value) {
        return new HashCodeChain().hash(value);
    }

    public static class ComparisonChain {
        private int result = 0;

        private ComparisonChain() {
            // singleton.
        }

        /**
         * Compares two comparable objects as specified by {@link
         * Comparable#compareTo}, <i>if</i> the result of this comparison chain
         * has not already been determined.
         * 
         * @param left
         * @param right
         * @return this
         */
        public <T extends Comparable<? super T>> ComparisonChain compare(T left, T right) {
            if (result == 0) {
                result = N.compare(left, right);
            }

            return this;
        }

        /**
         * Compares two objects using a comparator, <i>if</i> the result of this
         * comparison chain has not already been determined.
         * 
         * @param left
         * @param right
         * @return this
         */
        public <T> ComparisonChain compare(T left, T right, Comparator<T> comparator) {
            if (result == 0) {
                result = N.compare(left, right, comparator);
            }

            return this;
        }

        public <T, E extends Exception> ComparisonChain compare(T left, T right, Try.BiFunction<? super T, ? super T, Integer, E> func) throws E {
            N.checkArgNotNull(func, "func");

            if (result == 0) {
                result = func.apply(left, right);
            }

            return this;
        }

        /**
         * Compares two {@code boolean} values, considering {@code false} to be less
         * than {@code true}, <i>if</i> the result of this comparison chain has not
         * already been determined.
         *
         * @param left
         * @param right
         * @return this
         */
        public ComparisonChain compareFalseLess(boolean left, boolean right) {
            if (result == 0) {
                result = left == right ? 0 : (left ? -1 : 1);
            }

            return this;
        }

        /**
         * Compares two {@code boolean} values, considering {@code true} to be less
         * than {@code false}, <i>if</i> the result of this comparison chain has not
         * already been determined.
         * 
         * @param left
         * @param right
         * @return this
         */
        public ComparisonChain compareTrueLess(boolean left, boolean right) {
            if (result == 0) {
                result = left == right ? 0 : (left ? 1 : -1);
            }

            return this;
        }

        /**
         * Compares two {@code char} values as specified by {@link N#compare},
         * <i>if</i> the result of this comparison chain has not already been
         * determined.
         * 
         * @param left
         * @param right
         * @return this
         */
        public ComparisonChain compare(char left, char right) {
            if (result == 0) {
                result = N.compare(left, right);
            }

            return this;
        }

        /**
         * Compares two {@code byte} values as specified by {@link N#compare},
         * <i>if</i> the result of this comparison chain has not already been
         * determined.
         * 
         * @param left
         * @param right
         * @return this
         */
        public ComparisonChain compare(byte left, byte right) {
            if (result == 0) {
                result = N.compare(left, right);
            }

            return this;
        }

        /**
         * Compares two {@code short} values as specified by {@link N#compare},
         * <i>if</i> the result of this comparison chain has not already been
         * determined.
         * 
         * @param left
         * @param right
         * @return this
         */
        public ComparisonChain compare(short left, short right) {
            if (result == 0) {
                result = N.compare(left, right);
            }

            return this;
        }

        /**
         * Compares two {@code int} values as specified by {@link N#compare},
         * <i>if</i> the result of this comparison chain has not already been
         * determined.
         * 
         * @param left
         * @param right
         * @return this
         */
        public ComparisonChain compare(int left, int right) {
            if (result == 0) {
                result = N.compare(left, right);
            }

            return this;
        }

        /**
         * Compares two {@code long} values as specified by {@link N#compare},
         * <i>if</i> the result of this comparison chain has not already been
         * determined.
         * 
         * @param left
         * @param right
         * @return this
         */
        public ComparisonChain compare(long left, long right) {
            if (result == 0) {
                result = N.compare(left, right);
            }

            return this;
        }

        /**
         * Compares two {@code float} values as specified by {@link
         * Float#compare}, <i>if</i> the result of this comparison chain has not
         * already been determined.
         * 
         * @param left
         * @param right
         * @return this
         */
        public ComparisonChain compare(float left, float right) {
            if (result == 0) {
                result = N.compare(left, right);
            }

            return this;
        }

        /**
         * Compares two {@code double} values as specified by {@link
         * Double#compare}, <i>if</i> the result of this comparison chain has not
         * already been determined.
         * 
         * @param left
         * @param right
         * @return this
         */
        public ComparisonChain compare(double left, double right) {
            if (result == 0) {
                result = N.compare(left, right);
            }

            return this;
        }

        public int result() {
            return result;
        }
    }

    public static class EquivalenceChain {
        private boolean result = true;

        private EquivalenceChain() {
            // singleton.
        }

        /**
         * Compares two comparable objects as specified by {@link
         * N#equals(Object, Object)}, <i>if</i> the result of this equivalence chain
         * has not already been determined.
         * 
         * @param left
         * @param right
         * @return this
         */
        public EquivalenceChain equals(Object left, Object right) {
            if (result) {
                result = N.equals(left, right);
            }

            return this;
        }

        public <T, E extends Exception> EquivalenceChain equals(T left, T right, Try.BiFunction<? super T, ? super T, Boolean, E> func) throws E {
            N.checkArgNotNull(func, "func");

            if (result) {
                result = func.apply(left, right);
            }

            return this;
        }

        /**
         * Compares two {@code boolean} values as specified by {@code left == right},
         * <i>if</i> the result of this equivalence chain has not already been
         * determined.
         * 
         * @param left
         * @param right
         * @return this
         */
        public EquivalenceChain equals(boolean left, boolean right) {
            if (result) {
                result = left == right;
            }

            return this;
        }

        /**
         * Compares two {@code char} values as specified by {@code left == right},
         * <i>if</i> the result of this equivalence chain has not already been
         * determined.
         * 
         * @param left
         * @param right
         * @return this
         */
        public EquivalenceChain equals(char left, char right) {
            if (result) {
                result = left == right;
            }

            return this;
        }

        /**
         * Compares two {@code byte} values as specified by {@code left == right},
         * <i>if</i> the result of this equivalence chain has not already been
         * determined.
         * 
         * @param left
         * @param right
         * @return this
         */
        public EquivalenceChain equals(byte left, byte right) {
            if (result) {
                result = left == right;
            }

            return this;
        }

        /**
         * Compares two {@code int} values as specified by {@code left == right},
         * <i>if</i> the result of this equivalence chain has not already been
         * determined.
         * 
         * @param left
         * @param right
         * @return this
         */
        public EquivalenceChain equals(short left, short right) {
            if (result) {
                result = left == right;
            }

            return this;
        }

        /**
         * Compares two {@code int} values as specified by {@code left == right},
         * <i>if</i> the result of this equivalence chain has not already been
         * determined.
         * 
         * @param left
         * @param right
         * @return this
         */
        public EquivalenceChain equals(int left, int right) {
            if (result) {
                result = left == right;
            }

            return this;
        }

        /**
         * Compares two {@code long} values as specified by {@code left == right},
         * <i>if</i> the result of this equivalence chain has not already been
         * determined.
         * 
         * @param left
         * @param right
         * @return this
         */
        public EquivalenceChain equals(long left, long right) {
            if (result) {
                result = left == right;
            }

            return this;
        }

        /**
         * Compares two {@code float} values as specified by {@link
         * Float#compare}, <i>if</i> the result of this equivalence chain has not
         * already been determined.
         * 
         * @param left
         * @param right
         * @return this
         */
        public EquivalenceChain equals(float left, float right) {
            if (result) {
                result = Float.compare(left, right) == 0;
            }

            return this;
        }

        /**
         * Compares two {@code double} values as specified by {@link
         * Double#compare}, <i>if</i> the result of this equivalence chain has not
         * already been determined.
         * 
         * @param left
         * @param right
         * @return this
         */
        public EquivalenceChain equals(double left, double right) {
            if (result) {
                result = Double.compare(left, right) == 0;
            }

            return this;
        }

        public boolean result() {
            return result;
        }
    }

    public static class HashCodeChain {
        private int result = 0;

        private HashCodeChain() {
            // singleton.
        }

        /**
         * Add the hash code of the specified {@code value} to result.
         * 
         * @param value
         * @return this
         */
        public HashCodeChain hash(Object value) {
            result = result * 31 + N.hashCode(value);

            return this;
        }

        public <T, E extends Exception> HashCodeChain hash(T value, Try.ToIntFunction<? super T, E> func) throws E {
            N.checkArgNotNull(func, "func");

            result = result * 31 + func.applyAsInt(value);

            return this;
        }

        /**
         * Add the hash code of the specified {@code value} to result.
         * 
         * @param value
         * @return this
         */
        public HashCodeChain hash(boolean value) {
            result = result * 31 + (value ? 1231 : 1237);

            return this;
        }

        /**
         * Add the hash code of the specified {@code value} to result.
         * 
         * @param value
         * @return this
         */
        public HashCodeChain hash(char value) {
            result = result * 31 + N.hashCode(value);

            return this;
        }

        /**
         * Add the hash code of the specified {@code value} to result.
         * 
         * @param value
         * @return this
         */
        public HashCodeChain hash(byte value) {
            result = result * 31 + N.hashCode(value);

            return this;
        }

        /**
         * Add the hash code of the specified {@code value} to result.
         * 
         * @param value
         * @return this
         */
        public HashCodeChain hash(short value) {
            result = result * 31 + N.hashCode(value);

            return this;
        }

        /**
         * Add the hash code of the specified {@code value} to result.
         * 
         * @param value
         * @return this
         */
        public HashCodeChain hash(int value) {
            result = result * 31 + N.hashCode(value);

            return this;
        }

        /**
         * Add the hash code of the specified {@code value} to result.
         * 
         * @param value
         * @return this
         */
        public HashCodeChain hash(long value) {
            result = result * 31 + N.hashCode(value);

            return this;
        }

        /**
         * Add the hash code of the specified {@code value} to result.
         * 
         * @param value
         * @return this
         */
        public HashCodeChain hash(float value) {
            result = result * 31 + N.hashCode(value);

            return this;
        }

        /**
         * Add the hash code of the specified {@code value} to result.
         * 
         * @param value
         * @return this
         */
        public HashCodeChain hash(double value) {
            result = result * 31 + N.hashCode(value);

            return this;
        }

        public int result() {
            return result;
        }
    }
}
