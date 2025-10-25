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
 * Type handler for character stream values.
 * This class extends ReaderType to provide specific handling for character streams
 * in database operations and serialization contexts.
 * It serves as a semantic alias for ReaderType with a distinct type name.
 */
public class CharacterStreamType extends ReaderType {

    public static final String CHARACTER_STREAM = "CharacterStream";

    CharacterStreamType() {
        super(CHARACTER_STREAM);
    }
}