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

package com.landawn.abacus.parser;

/**
 * The Enum Exclusion.
 *
 * @author Haiyang Li
 * @since 0.8
 */
public enum Exclusion {
    /**
     * Excludes the bean properties with null values.
     */
    NULL,
    /**
     * Excludes the bean properties with default values.
     */
    DEFAULT,
    /**
     * No bean property is excluded.
     */
    NONE;
}
