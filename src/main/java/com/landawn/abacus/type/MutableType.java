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

package com.landawn.abacus.type;

import com.landawn.abacus.util.Mutable;

/**
 * Abstract base class for type handlers that work with mutable objects implementing {@link Mutable}.
 * This class extends {@link AbstractType} to provide a common foundation for type converters
 * and serializers that deal with mutable value objects (objects whose state can be modified
 * after creation).
 *
 * <p>The generic parameter {@code T} is bounded by {@link Mutable}, giving a compile-time
 * guarantee that any subclass works exclusively with mutable types.
 *
 * <p><b>Note on mutability:</b> unlike immutable types, objects handled by this class can be
 * modified in place. Callers must take care when caching or sharing instances across threads.
 *
 * <p><b>Note on usage:</b> the {@code MutableXxxType} type handlers shipped in this package
 * (e.g. {@code MutableIntType}, {@code MutableBooleanType}) do <i>not</i> currently extend
 * this class; most extend {@link NumberType} or {@link AbstractType} directly. This class
 * is provided as an optional base for custom handlers of {@link Mutable} types.
 *
 * @param <T> the specific mutable type handled by this type handler; must implement {@link Mutable}
 * @see AbstractType
 * @see Mutable
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
