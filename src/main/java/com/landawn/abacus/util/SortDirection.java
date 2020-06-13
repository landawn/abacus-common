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

package com.landawn.abacus.util;

/**
 * The Enum SortDirection.
 *
 * @author Haiyang Li
 * @since 0.8
 */
public enum SortDirection {

    /** The asc. */
    ASC("ASC"),
    /** The desc. */
    DESC("DESC");

    /** The name. */
    private String name;

    /**
     * Instantiates a new sort direction.
     */
    // For Kryo
    SortDirection() {
    }

    /**
     * Instantiates a new sort direction.
     *
     * @param name
     */
    SortDirection(String name) {
        this.name = name;
    }

    /**
     * Gets the name.
     *
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     *
     * @return
     */
    @Override
    public String toString() {
        return name;
    }
}
