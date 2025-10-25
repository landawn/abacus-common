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

package com.landawn.abacus.http;

/**
 * Enum representing various content formats for HTTP requests and responses.
 * Each format specifies a content type and content encoding combination.
 * 
 * <p>This enum is used to define how data should be serialized/deserialized 
 * and compressed/decompressed during HTTP communication.</p>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * ContentFormat format = ContentFormat.JSON_GZIP;
 * String contentType = format.contentType(); // returns "application/json"
 * String encoding = format.contentEncoding(); // returns "gzip"
 * }</pre>
 */
public enum ContentFormat {
    NONE("", ""), //
    JSON("application/json", ""), // //NOSONAR
    JSON_LZ4("application/json", "lz4"), //
    JSON_SNAPPY("application/json", "snappy"), // //NOSONAR
    JSON_GZIP("application/json", "gzip"), //
    JSON_BR("application/json", "br"), //
    XML("application/xml", ""), // //NOSONAR
    XML_LZ4("application/xml", "lz4"), //
    XML_SNAPPY("application/xml", "snappy"), //
    XML_GZIP("application/xml", "gzip"), //
    XML_BR("application/xml", "br"), //
    FormUrlEncoded("application/x-www-form-urlencoded", ""), //
    KRYO("", "kryo"), //
    LZ4("", "lz4"), //
    SNAPPY("", "snappy"), //
    GZIP("", "gzip"), //
    BR("", "br");

    private final String contentType;
    private final String contentEncoding;

    ContentFormat(final String contentType, final String contentEncoding) {
        this.contentType = contentType;
        this.contentEncoding = contentEncoding;
    }

    /**
     * Returns the MIME content type associated with this format.
     * 
     * <p>The content type indicates the media type of the resource, such as
     * "application/json" for JSON data or "application/xml" for XML data.
     * An empty string is returned for formats that don't have a specific content type.</p>
     * 
     * @return the content type string, or an empty string if not applicable
     */
    public String contentType() {
        return contentType;
    }

    /**
     * Returns the content encoding (compression algorithm) associated with this format.
     * 
     * <p>The content encoding indicates how the content has been compressed, such as
     * "gzip" for GZIP compression, "lz4" for LZ4 compression, or "br" for Brotli compression.
     * An empty string is returned for uncompressed formats.</p>
     * 
     * @return the content encoding string, or an empty string if no compression is used
     */
    public String contentEncoding() {
        return contentEncoding;
    }
}