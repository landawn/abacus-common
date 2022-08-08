/*
 * Copyright (c) 2016, Haiyang Li. All rights reserved.
 */

package com.landawn.abacus.type;

import java.util.Map;

/**
 *
 * @author Haiyang Li
 * @param <T>
 * @since 0.8
 */
public abstract class AbstractOptionalType<T> extends AbstractType<T> {

    protected static final String IS_PRESENT = "isPresent";

    protected static final String VALUE = "value";

    private static Type<Map<Object, Object>> mapType = null;

    protected AbstractOptionalType(String typeName) {
        super(typeName);
    }

    /**
     * Gets the map type.
     *
     * @return
     */
    protected static synchronized Type<Map<Object, Object>> getMapType() {
        if (mapType == null) {
            mapType = TypeFactory.getType("Map<Object, Object>");
        }

        return mapType;
    }
}
