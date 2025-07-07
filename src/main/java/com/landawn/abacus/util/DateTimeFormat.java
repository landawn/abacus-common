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

package com.landawn.abacus.util;

/**
 * Enumeration defining standard date and time formatting options.
 * This enum provides predefined formats for representing dates and timestamps
 * in various standard formats including numeric (long) representation and ISO 8601 formats.
 * 
 * <p>The formats are commonly used for data serialization, API communication,
 * and standardized date/time representation across different systems.</p>
 * 
 * <p>Example usage:</p>
 * <pre>
 * // Using with a formatter
 * DateTimeFormat format = DateTimeFormat.ISO_8601_DATE_TIME;
 * 
 * // Converting based on format type
 * switch (format) {
 *     case LONG:
 *         return System.currentTimeMillis();
 *     case ISO_8601_DATE_TIME:
 *         return "2023-12-25T10:30:45Z";
 *     case ISO_8601_TIMESTAMP:
 *         return "2023-12-25T10:30:45.123Z";
 * }
 * </pre>
 * 
 * @see java.time.format.DateTimeFormatter
 * @see java.text.SimpleDateFormat
 */
public enum DateTimeFormat {
    /**
     * Represents date/time as a long number (milliseconds since Unix epoch).
     * This format stores the date/time as the number of milliseconds since
     * January 1, 1970, 00:00:00 GMT (Unix epoch).
     * 
     * <p>This format is useful for:</p>
     * <ul>
     *   <li>Efficient storage and transmission</li>
     *   <li>Easy date arithmetic and comparison</li>
     *   <li>Language and locale-independent representation</li>
     * </ul>
     * 
     * <p>Example value: {@code 1703503845000} (represents 2023-12-25 10:30:45 GMT)</p>
     */
    LONG,

    /**
     * ISO 8601 date-time format without milliseconds: {@code yyyy-MM-dd'T'HH:mm:ss'Z'}.
     * This format represents date and time in the ISO 8601 standard with second precision.
     * The 'Z' suffix indicates UTC timezone (Zulu time).
     * 
     * <p>Format pattern: {@code yyyy-MM-dd'T'HH:mm:ss'Z'}</p>
     * <p>Example value: {@code 2023-12-25T10:30:45Z}</p>
     * 
     * <p>This format is widely used in:</p>
     * <ul>
     *   <li>REST APIs and web services</li>
     *   <li>International data exchange</li>
     *   <li>Database storage for timezone-aware timestamps</li>
     * </ul>
     * 
     * @see <a href="https://en.wikipedia.org/wiki/ISO_8601">ISO 8601 Standard</a>
     */
    ISO_8601_DATE_TIME,

    /**
     * ISO 8601 timestamp format with milliseconds: {@code yyyy-MM-dd'T'HH:mm:ss.SSS'Z'}.
     * This format represents date and time in the ISO 8601 standard with millisecond precision.
     * The 'Z' suffix indicates UTC timezone (Zulu time).
     * 
     * <p>Format pattern: {@code yyyy-MM-dd'T'HH:mm:ss.SSS'Z'}</p>
     * <p>Example value: {@code 2023-12-25T10:30:45.123Z}</p>
     * 
     * <p>This format is preferred when:</p>
     * <ul>
     *   <li>Millisecond precision is required</li>
     *   <li>Logging timestamps with high precision</li>
     *   <li>Recording precise event timing</li>
     *   <li>Compatibility with systems requiring sub-second precision</li>
     * </ul>
     * 
     * @see <a href="https://en.wikipedia.org/wiki/ISO_8601">ISO 8601 Standard</a>
     */
    ISO_8601_TIMESTAMP
}