/*
 * Copyright (c) 2016, Haiyang Li. All rights reserved.
 */

package com.landawn.abacus.type;

import java.util.Map;

/**
 * The Abstract base class for {@code Optional} types in the type system.
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

    /** Cached instance of the {@code Map} type used for internal operations */
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
     * Gets the cached {@code Map} type instance used for internal operations.
     * <p>
     * This method is synchronized to ensure thread-safe lazy initialization
     * of the {@code Map} type. The {@code Map} type is used internally for converting
     * Optional values to/from {@code Map} representations during serialization.
     * </p>
     *
     * <p>Usage Examples:</p>
     * <pre>{@code
     * // Internal use for Optional serialization
     * Type<Map<Object, Object>> mapType = AbstractOptionalType.getMapType();
     * // The map type is used to convert Optional values to Maps with structure:
     * // {
     * //   "isPresent": true/false,
     * //   "value": actual_value (if present)
     * // }
     * }</pre>
     *
     * @return the {@code Map} type instance for {@code Map<Object, Object>}
     */
    protected static synchronized Type<Map<Object, Object>> getMapType() {
        if (mapType == null) {
            mapType = TypeFactory.getType("Map<Object, Object>");
        }

        return mapType;
    }

    /**
     * Checks if this type represents an {@code Optional} or {@code nullable} type.
     * <p>
     * This method always returns {@code true} for {@code Optional} types,
     * indicating that values of this type may or may not be present.
     * </p>
     *
     * @return {@code true}, indicating this is an {@code Optional} or {@code nullable} type
     */
    @Override
    public boolean isOptionalOrNullable() {
        return true;
    }
}
