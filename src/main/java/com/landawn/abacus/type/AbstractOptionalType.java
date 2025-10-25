/*
 * Copyright (c) 2016, Haiyang Li. All rights reserved.
 */

package com.landawn.abacus.type;

import java.util.Map;

/**
 * Abstract base class for Optional types in the type system.
 * This class provides common functionality for handling Optional-like types
 * that can represent values that may or may not be present.
 * It defines standard field names used for serialization/deserialization
 * of optional values.
 *
 * @param <T> the Optional type (e.g., Optional, OptionalInt, OptionalLong)
 */
public abstract class AbstractOptionalType<T> extends AbstractType<T> {

    /** Standard field name for the presence indicator in serialized form */
    protected static final String IS_PRESENT = "isPresent";

    /** Standard field name for the actual value in serialized form */
    protected static final String VALUE = "value";

    /** Cached instance of the Map type used for internal operations */
    private static Type<Map<Object, Object>> mapType = null;

    protected AbstractOptionalType(final String typeName) {
        super(typeName);
    }

    /**
     * Gets the cached Map type instance used for internal operations.
     * This method is synchronized to ensure thread-safe lazy initialization
     * of the Map type. The Map type is used internally for converting
     * Optional values to/from Map representations during serialization.
     *
     * @return the Map type instance for Map&lt;Object, Object&gt;
     */
    protected static synchronized Type<Map<Object, Object>> getMapType() {
        if (mapType == null) {
            mapType = TypeFactory.getType("Map<Object, Object>");
        }

        return mapType;
    }

    /**
     * Checks if this type represents an Optional or nullable type.
     * This method always returns {@code true} for Optional types,
     * indicating that values of this type may or may not be present.
     *
     * @return {@code true}, indicating this is an Optional or nullable type
     */
    @Override
    public boolean isOptionalOrNullable() {
        return true;
    }
}