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
 * Annotations for API contracts, entity mapping, JSON/XML serialization, mutability, nullability,
 * and stream-pipeline characteristics.
 *
 * <p>Unless an annotation states otherwise, it documents a contract and does not by itself enforce
 * that contract at runtime. Implementations, parsers, code-generation tools, and static analysis
 * may honor the metadata.</p>
 *
 * <h2>Nullability</h2>
 * <ul>
 *   <li>{@link NotNull} &mdash; the annotated parameter, return value, or field must not be {@code null}.</li>
 *   <li>{@link MayReturnNull} &mdash; the annotated method may return {@code null}.</li>
 *   <li>{@link NullSafe} &mdash; the annotated method accepts {@code null} input without throwing.</li>
 * </ul>
 *
 * <h2>Mutability and identity</h2>
 * <ul>
 *   <li>{@link Immutable} / {@link Mutable} &mdash; values of the annotated type are (or are not) mutated after construction.</li>
 *   <li>{@link ReadOnly} / {@link ReadOnlyId} &mdash; a mapped property or identifier is not written back.</li>
 *   <li>{@link Stateful} &mdash; the annotated type or lambda retains state across invocations.</li>
 * </ul>
 *
 * <h2>Entity and column mapping</h2>
 * <p>{@link Entity}, {@link Table}, {@link Column}, {@link Id}, {@link NonColumn}, {@link NonUpdatable},
 * {@link JoinedBy}, {@link AccessFieldByMethod}, {@link com.landawn.abacus.annotation.Record}, and {@link Transient} describe how
 * beans map to tables, columns, and serialized fields. {@link Type} names the Abacus type handler
 * for a property.</p>
 *
 * <h2>JSON and XML</h2>
 * <p>{@link JsonXmlConfig}, {@link JsonXmlField}, {@link JsonXmlValue}, and {@link JsonXmlCreator}
 * customize serialization and deserialization performed by
 * {@link com.landawn.abacus.parser.ParserFactory} parsers.</p>
 *
 * <h2>Stream and sequence pipelines</h2>
 * <p>{@link IntermediateOp}, {@link TerminalOp}, {@link TerminalOpTriggered}, {@link LazyEvaluation},
 * {@link ParallelSupported}, and {@link SequentialOnly} mark pipeline methods on
 * {@link com.landawn.abacus.util.stream.Stream}, primitive streams,
 * {@link com.landawn.abacus.util.stream.EntryStream}, and {@link com.landawn.abacus.util.Seq}.
 * They are documentation markers (typically {@code CLASS} retention); the runtime does not branch on them.</p>
 *
 * <h2>API status</h2>
 * <ul>
 *   <li>{@link Beta} &mdash; the API may still change.</li>
 *   <li>{@link Internal} &mdash; not part of the supported public surface.</li>
 *   <li>{@link UnsupportedOperation} &mdash; the method is not implemented and throws.</li>
 *   <li>{@link Test}, {@link DiffIgnore}, {@link SuppressFBWarnings} &mdash; test, bean-diff, and FindBugs metadata.</li>
 * </ul>
 */
package com.landawn.abacus.annotation;
