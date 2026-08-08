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
 * Sequential and parallel stream processing for object and primitive values.
 *
 * <p>The package provides {@link com.landawn.abacus.util.stream.Stream}, primitive stream variants,
 * {@link com.landawn.abacus.util.stream.EntryStream}, collectors, extended iterators, and internal
 * stream implementations. Streams support lazy intermediate operations and single-use terminal
 * consumption with explicit resource-closing behavior.</p>
 *
 * <p><b>Shared naming with {@link com.landawn.abacus.util.Seq Seq}:</b> pipeline operation names
 * (including the intentional {@code flatMap}/{@code flatmap}/{@code flattMap}/{@code flatMapArray}
 * casing, first/last/find* terminals, and boolean match terminals such as
 * {@code hasMatchCountBetween}) are documented in the canonical glossary on
 * {@link com.landawn.abacus.util.stream.Stream} &mdash; see that class's
 * <i>Shared pipeline naming</i> section ({@code Stream.html#shared-pipeline-naming}).</p>
 */
package com.landawn.abacus.util.stream;
