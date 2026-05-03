/*
 * Copyright (c) 2016, Haiyang Li. All rights reserved.
 */

package com.landawn.abacus.type;

import java.util.Map;

/**
 * The abstract base class for {@code Optional} types in the type system.
 * <p>
 * This class provides common functionality for handling {@code Optional}-like types
 * that can represent values that may or may not be present.
 * It defines standard field names used for serialization/deserialization
 * of optional values.
 * </p>
 *
 * @param <T> the {@code Optional} type (e.g., {@code Optional}, {@code OptionalInt}, {@code OptionalLong})
 */
public abstract class AbstractOptionalType<T> extends AbstractType<T> {

    /** Standard field name for the presence indicator in serialized form */
    protected static final String IS_PRESENT = "isPresent";

    /** Standard field name for the actual value in serialized form */
    protected static final String VALUE = "value";

    /** Lazily-initialized cached instance of the {@code Map<Object, Object>} type used for internal serialization operations */
    private static Type<Map<Object, Object>> mapType = null;

    /**
     * Constructs an {@code AbstractOptionalType} with the specified type name.
     *
     * @param typeName the name of the Optional type (e.g., "Optional", "OptionalInt", "OptionalLong")
     */
    protected AbstractOptionalType(final String typeName) {
        super(typeName);
    }

    /**
     * Returns the cached {@code Map} type instance used for internal serialization operations.
     * <p>
     * This method uses lazy initialization with synchronization to ensure thread-safe access.
     * The {@code Map} type is used internally to convert Optional values to/from
     * {@code Map<Object, Object>} representations during serialization.
     * </p>
     *
     * @return the shared {@code Type} instance for {@code Map<Object, Object>}
     */
    protected static synchronized Type<Map<Object, Object>> getMapType() {
        if (mapType == null) {
            mapType = TypeFactory.getType("Map<Object, Object>");
        }

        return mapType;
    }

    /**
     * Returns {@code true} because this type represents an {@code Optional} or
     * {@code Nullable} container type — values may or may not be present.
     *
     * @return {@code true}
     */
    @Override
    public boolean isOptionalOrNullable() {
        return true;
    }
}
