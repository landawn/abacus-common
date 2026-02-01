/*
 * Copyright (C) 2015 HaiYang Li
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

package com.landawn.abacus.type;

import com.landawn.abacus.util.Mutable;

/**
 * An abstract base class for type handlers that work with mutable objects implementing {@link Mutable}.
 * This class extends {@link AbstractType} to provide specialized type handling capabilities for objects
 * whose state can be modified after creation.
 *
 * <p><b>Purpose:</b>
 * Serves as a foundation for creating type converters and serializers for mutable value objects,
 * enabling consistent handling of objects that allow modification of their internal state.
 *
 * <p><b>Type Parameter Constraints:</b>
 * The generic parameter {@code T} is bounded by {@link Mutable}, ensuring that all concrete
 * implementations work exclusively with mutable types. This provides:
 * <ul>
 *   <li><b>Type Safety:</b> Compile-time guarantee that handled objects are mutable</li>
 *   <li><b>Contract Enforcement:</b> All objects support the {@code Mutable} interface contract</li>
 *   <li><b>API Consistency:</b> Uniform behavior across all mutable type handlers</li>
 * </ul>
 *
 * <p><b>Design Pattern:</b>
 * This class follows the Template Method pattern, inheriting core type conversion logic from
 * {@link AbstractType} while allowing subclasses to implement mutable-specific behavior.
 *
 * <p><b>Common Use Cases:</b>
 * <ul>
 *   <li>Creating type handlers for {@code MutableInt}, {@code MutableLong}, {@code MutableDouble}</li>
 *   <li>Implementing serialization/deserialization for mutable wrapper types</li>
 *   <li>Building database column mappings for mutable value objects</li>
 *   <li>Supporting ORM frameworks with mutable entity properties</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Example mutable type handler
 * public class MutableIntType extends MutableType<MutableInt> {
 *     public MutableIntType() {
 *         super("MutableInt");
 *     }
 *     
 *     @Override
 *     public MutableInt valueOf(String str) {
 *         return MutableInt.of(Integer.parseInt(str));
 *     }
 * }
 * }</pre>
 *
 * <p><b>Mutability Implications:</b>
 * Unlike immutable types, objects handled by this class can be modified in-place, which means:
 * <ul>
 *   <li>Care must be taken when caching or sharing instances</li>
 *   <li>Thread safety considerations may be required for concurrent access</li>
 *   <li>Defensive copying may be necessary in certain contexts</li>
 * </ul>
 *
 * <p><b>Thread Safety:</b>
 * Thread safety is determined by the concrete implementation and the mutability characteristics
 * of the handled type {@code T}.
 *
 * @param <T> the specific mutable type handled by this type converter. Must implement {@link Mutable}.
 * @see AbstractType
 * @see Mutable
 * @see com.landawn.abacus.util.MutableInt
 * @see com.landawn.abacus.util.MutableLong
 * @see com.landawn.abacus.util.MutableDouble
 */
public abstract class MutableType<T extends Mutable> extends AbstractType<T> {

    /**
     * Constructs a MutableType with the specified type name.
     * This constructor is used by subclasses to initialize the type handler with a custom type name.
     *
     * @param typeName the name of the type, typically the simple class name of the mutable type
     */
    protected MutableType(final String typeName) {
        super(typeName);
    }
}
