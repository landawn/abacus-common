/*
 * Copyright (C) 2011 The Guava Authors
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

package com.landawn.abacus.hash;

import java.nio.charset.Charset;

import com.landawn.abacus.util.N;
import com.landawn.abacus.util.function.BiConsumer;

/**
 * Note: It's copied from Google Guava under Apache License 2.0
 * 
 * An abstract composition of multiple hash functions. {@linkplain #newHasher()} delegates to the
 * {@code Hasher} objects of the delegate hash functions, and in the end, they are used by
 * {@linkplain #makeHash(Hasher[])} that constructs the final {@code HashCode}.
 *
 * @author Dimitris Andreou
 */
abstract class AbstractCompositeHashFunction extends AbstractStreamingHashFunction {
    final HashFunction[] functions;

    AbstractCompositeHashFunction(HashFunction... functions) {
        for (HashFunction function : functions) {
            N.checkArgNotNull(function);
        }
        this.functions = functions;
    }

    /**
     * Constructs a {@code HashCode} from the {@code Hasher} objects of the functions. Each of them
     * has consumed the entire input and they are ready to output a {@code HashCode}. The order of the
     * hashers are the same order as the functions given to the constructor.
     */
    // this could be cleaner if it passed HashCode[], but that would create yet another array...
    /* protected */ abstract HashCode makeHash(Hasher[] hashers);

    @Override
    public Hasher newHasher() {
        final Hasher[] hashers = new Hasher[functions.length];
        for (int i = 0; i < hashers.length; i++) {
            hashers[i] = functions[i].newHasher();
        }
        return new Hasher() {
            @Override
            public Hasher put(byte b) {
                for (Hasher hasher : hashers) {
                    hasher.put(b);
                }
                return this;
            }

            @Override
            public Hasher put(byte[] bytes) {
                for (Hasher hasher : hashers) {
                    hasher.put(bytes);
                }
                return this;
            }

            @Override
            public Hasher put(byte[] bytes, int off, int len) {
                for (Hasher hasher : hashers) {
                    hasher.put(bytes, off, len);
                }
                return this;
            }

            @Override
            public Hasher put(short s) {
                for (Hasher hasher : hashers) {
                    hasher.put(s);
                }
                return this;
            }

            @Override
            public Hasher put(int i) {
                for (Hasher hasher : hashers) {
                    hasher.put(i);
                }
                return this;
            }

            @Override
            public Hasher put(long l) {
                for (Hasher hasher : hashers) {
                    hasher.put(l);
                }
                return this;
            }

            @Override
            public Hasher put(float f) {
                for (Hasher hasher : hashers) {
                    hasher.put(f);
                }
                return this;
            }

            @Override
            public Hasher put(double d) {
                for (Hasher hasher : hashers) {
                    hasher.put(d);
                }
                return this;
            }

            @Override
            public Hasher put(boolean b) {
                for (Hasher hasher : hashers) {
                    hasher.put(b);
                }
                return this;
            }

            @Override
            public Hasher put(char c) {
                for (Hasher hasher : hashers) {
                    hasher.put(c);
                }
                return this;
            }

            @Override
            public Hasher put(char[] chars) {
                for (Hasher hasher : hashers) {
                    hasher.put(chars);
                }
                return this;
            }

            @Override
            public Hasher put(char[] chars, int off, int len) {
                for (Hasher hasher : hashers) {
                    hasher.put(chars, off, len);
                }
                return this;
            }

            @Override
            public Hasher put(CharSequence chars) {
                for (Hasher hasher : hashers) {
                    hasher.put(chars);
                }
                return this;
            }

            @Override
            public Hasher put(CharSequence chars, Charset charset) {
                for (Hasher hasher : hashers) {
                    hasher.put(chars, charset);
                }
                return this;
            }

            @Override
            public <T> Hasher put(T instance, BiConsumer<? super T, ? super Hasher> funnel) {
                for (Hasher hasher : hashers) {
                    hasher.put(instance, funnel);
                }
                return this;
            }

            @Override
            public HashCode hash() {
                return makeHash(hashers);
            }
        };
    }
}
