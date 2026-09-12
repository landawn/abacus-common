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
 * Guava-backed file and graph utilities adapted to Abacus APIs.
 *
 * <ul>
 *   <li>{@link Files} wraps Guava {@code com.google.common.io.Files} / {@code MoreFiles} and
 *       {@code java.nio.file.Files} for reading, writing, copying, deleting, memory-mapping, and
 *       ByteSource/ByteSink/CharSource/CharSink access. No method accepts a {@code null} argument.</li>
 *   <li>{@link Traverser} wraps Guava graph traversal and returns
 *       {@link com.landawn.abacus.util.stream.Stream} sequences. Use {@link Traverser#forTree}
 *       for trees and {@link Traverser#forGraph} for general graphs; {@link Traverser#FILES} and
 *       {@link Traverser#PATHS} traverse file-system trees. Breadth-first, depth-first pre-order,
 *       and depth-first post-order walks are supported.</li>
 * </ul>
 *
 * <p>Portions are derived from Google Guava under the Apache License 2.0. Hashing lives in the
 * nested {@link com.landawn.abacus.guava.hash} package.</p>
 *
 * @see Files
 * @see Traverser
 * @see com.landawn.abacus.guava.hash.Hashing
 */
package com.landawn.abacus.guava;
