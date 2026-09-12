/*
 * Copyright (c) 2026, Haiyang Li.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.landawn.abacus.util;

/**
 * Utility class providing various TriConsumer implementations and factory methods.
 * This class is reserved for future TriConsumer utilities.
 *
 * <p>This class is a top-level sibling of {@link Fn}, not a nested type. Use {@link Fn} for the
 * general functional-interface factory and {@link Fnn} for {@link Throwables} variants that can
 * declare checked exceptions. For one- and two-argument consumers see {@link Consumers} and
 * {@link BiConsumers}.</p>
 *
 * @see Fn
 * @see Fnn
 * @see Consumers
 * @see BiConsumers
 * @see TriFunctions
 * @see TriPredicates
 */
public final class TriConsumers {
    private TriConsumers() {
    }
}
