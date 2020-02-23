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

package com.landawn.abacus.http;

/**
 * The Enum ContentFormat.
 *
 * @author Haiyang Li
 * @since 0.8
 */
public enum ContentFormat {

    /** The none. */
    NONE,
    /** The xml. */
    XML,
    /** The xml lz4. */
    XML_LZ4,
    /** The xml snappy. */
    XML_SNAPPY,
    /** The xml gzip. */
    XML_GZIP,
    /** The json. */
    JSON,
    /** The json lz4. */
    JSON_LZ4,
    /** The json snappy. */
    JSON_SNAPPY,
    /** The json gzip. */
    JSON_GZIP,
    /** The kryo. */
    KRYO,
    /** The lz4. */
    LZ4,
    /** The snappy. */
    SNAPPY,
    /** The gzip. */
    GZIP;
}
