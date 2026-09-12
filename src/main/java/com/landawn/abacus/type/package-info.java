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
 * The Abacus runtime type system: conversion, comparison, JSON/XML writing, and JDBC binding for a
 * Java type.
 *
 * <p>{@link Type} is the application-facing descriptor. Obtain one with {@link Type#of(Class)},
 * {@link Type#of(java.lang.reflect.Type)}, {@link Type#of(String)}, or
 * {@link Type#of(com.landawn.abacus.util.TypeReference)} (the last captures generic parameters).
 * Convenience factories such as {@link Type#ofList(Class)} and {@link Type#ofMap(Class, Class)}
 * build parameterized collection and map types. Type handlers are immutable and thread-safe;
 * {@link Type#isImmutable()} describes values of the represented Java type, not the handler.</p>
 *
 * <p>{@link TypeFactory} resolves and caches handlers. Unrecognized classes become a bean handler
 * or a generic {@code Object} handler rather than failing. Register a custom handler with
 * {@link TypeFactory#registerType}.</p>
 *
 * <p>Built-in families include:</p>
 * <ul>
 *   <li>Primitives, wrappers, arrays, and Abacus primitive lists ({@code IntList}, …)</li>
 *   <li>Strings, numbers, enums, UUID/URI/URL, {@code Pattern}, optional and nullable wrappers</li>
 *   <li>JDK and Joda time, SQL date/time, JDBC Blob/Clob/Array/SQLXML</li>
 *   <li>Collections, maps, Guava {@code Multiset}/{@code Multimap} (when Guava is present)</li>
 *   <li>Abacus {@code Pair}, {@code Triple}, {@code Tuple}, {@code Range}, {@code Dataset}, {@code Sheet},
 *       {@code Multiset}, {@code Multimap}, immutable collections</li>
 *   <li>Bean types and JSON/XML-as-value wrappers</li>
 * </ul>
 *
 * <p>Concrete {@code *Type} classes in this package are the registered implementations. Prefer
 * {@link Type#of} over constructing them. For capturing generics at a call site, use
 * {@link com.landawn.abacus.util.TypeReference}.</p>
 *
 * @see Type
 * @see TypeFactory
 * @see com.landawn.abacus.util.TypeReference
 */
package com.landawn.abacus.type;
