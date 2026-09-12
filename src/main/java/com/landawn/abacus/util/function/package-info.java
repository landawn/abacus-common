/*
 * Copyright (C) 2015 HaiYang Li
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

/**
 * Functional interfaces used throughout Abacus collections, utilities, and streams.
 *
 * <p>Object-arity types with JDK counterparts ({@link Consumer}, {@link Function}, {@link Predicate},
 * {@link Supplier}, {@link BiConsumer}, {@link BiFunction}, {@link BiPredicate}, {@link UnaryOperator},
 * {@link BinaryOperator}, and the {@code java.util.function} primitive specializations for
 * {@code int}/{@code long}/{@code double}) extend those JDK interfaces and also extend the matching
 * {@link com.landawn.abacus.util.Throwables} nested type with {@code RuntimeException}. They can be
 * used anywhere the JDK type is required. Additional arities and primitives have no JDK counterpart.</p>
 *
 * <h2>What this package adds</h2>
 * <ul>
 *   <li>Primitive specializations for {@code boolean}, {@code char}, {@code byte}, {@code short}, and
 *       {@code float} (consumers, functions, predicates, suppliers, unary/binary/ternary operators).</li>
 *   <li>Three- and four-argument types: {@link TriConsumer}, {@link TriFunction}, {@link TriPredicate},
 *       {@link QuadConsumer}, {@link QuadFunction}, {@link QuadPredicate}.</li>
 *   <li>Variable-arity {@link NConsumer}, {@link NFunction}, {@link NPredicate}.</li>
 *   <li>Mixed primitive/object signatures such as {@link ObjIntConsumer}, {@link IntObjFunction},
 *       {@link BiIntObjPredicate}, and {@code ToXxx} conversions ({@link ToBooleanFunction},
 *       {@link ToByteFunction}, {@link ToCharFunction}, {@link ToFloatFunction}, {@link ToShortFunction}).</li>
 *   <li>{@link com.landawn.abacus.util.function.Callable} and
 *       {@link com.landawn.abacus.util.function.Runnable} that extend the matching {@code Throwables} types.</li>
 * </ul>
 *
 * <p>Ready-made instances live in {@code com.landawn.abacus.util}, not here:
 * {@link com.landawn.abacus.util.Fn} and {@link com.landawn.abacus.util.Fnn} (the latter for
 * {@code Throwables} callbacks), plus {@link com.landawn.abacus.util.Consumers},
 * {@link com.landawn.abacus.util.Predicates}, {@link com.landawn.abacus.util.Functions},
 * {@link com.landawn.abacus.util.Suppliers}, {@link com.landawn.abacus.util.BiConsumers},
 * {@link com.landawn.abacus.util.BiPredicates}, {@link com.landawn.abacus.util.BiFunctions},
 * {@link com.landawn.abacus.util.BinaryOperators}, {@link com.landawn.abacus.util.UnaryOperators},
 * {@link com.landawn.abacus.util.TriConsumers}, {@link com.landawn.abacus.util.TriPredicates},
 * {@link com.landawn.abacus.util.TriFunctions}, {@link com.landawn.abacus.util.IntFunctions}, and
 * {@link com.landawn.abacus.util.LongSuppliers}.</p>
 *
 * @see java.util.function
 * @see com.landawn.abacus.util.Throwables
 * @see com.landawn.abacus.util.Fn
 * @see com.landawn.abacus.util.Fnn
 */
package com.landawn.abacus.util.function;
