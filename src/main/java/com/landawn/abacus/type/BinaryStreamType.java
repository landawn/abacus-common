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
 * Type handler for binary InputStream operations.
 * This class extends InputStreamType to provide specific handling for binary data streams.
 * It inherits all database and I/O operations from InputStreamType while identifying
 * itself specifically as a binary stream type.
 */
class BinaryStreamType extends InputStreamType {

    /**
     * The type name constant for binary stream type identification.
     */
    public static final String BINARY_STREAM = "BinaryStream";

    BinaryStreamType() {
        super(BINARY_STREAM);
    }

}
