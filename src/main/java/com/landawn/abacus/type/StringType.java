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

/**
 * Type handler for {@link String} objects. This class provides the standard
 * implementation for String type handling in the type system. It extends
 * AbstractStringType which provides the core functionality for string operations.
 */
public class StringType extends AbstractStringType {

    /**
     * The type name identifier for String type.
     */
    public static final String STRING = String.class.getSimpleName();

    StringType() {
        super(STRING);
    }
}
