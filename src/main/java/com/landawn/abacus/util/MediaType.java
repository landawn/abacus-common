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
 * An enumeration representing different media types or content categories.
 * 
 * <p>This enum provides a type-safe way to work with media types, avoiding
 * the use of string constants or numeric codes that can be error-prone. Each
 * media type is associated with a numeric value for easy serialization and
 * interoperability with systems that use numeric media type codes.</p>
 * 
 * <p>The media types are designed to categorize different forms of digital content,
 * making it easier to handle content-specific operations in a generic way. The numeric
 * values associated with each type ensure backward compatibility with legacy systems
 * that may use integer-based media type identification.</p>
 * 
 * <p>The available media types are:</p>
 * <ul>
 *   <li>{@link #BINARY} - Generic binary data (value: 0)</li>
 *   <li>{@link #AUDIO} - Audio content such as music or voice recordings (value: 1)</li>
 *   <li>{@link #VIDEO} - Video content including movies and clips (value: 2)</li>
 *   <li>{@link #IMAGE} - Image content such as photos and graphics (value: 3)</li>
 *   <li>{@link #TEXT} - Text content including documents and plain text (value: 4)</li>
 *   <li>{@link #RECORD} - Structured data records or database entries (value: 5)</li>
 * </ul>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Using MediaType for content classification
 * MediaType type = MediaType.IMAGE;
 * int code = type.intValue(); // returns 3
 * 
 * // Converting from numeric code to MediaType
 * MediaType decoded = MediaType.valueOf(2); // returns VIDEO
 * 
 * // Using in switch statements
 * switch (contentType) {
 *     case AUDIO:
 *         processAudio();
 *         break;
 *     case VIDEO:
 *         processVideo();
 *         break;
 *     // ... other cases
 * }
 * }</pre>
 * 
 * @since 1.0
 */
public enum MediaType {

    /**
     * Binary media type representing generic binary data.
     * 
     * <p>This type is used for any binary content that doesn't fit into other
     * specific categories. It serves as a catch-all for binary files such as
     * executables, compressed archives, or proprietary binary formats.</p>
     * 
     * <p>Numeric value: 0</p>
     */
    BINARY(0),

    /**
     * Audio media type representing sound or music content.
     * 
     * <p>This type is used for audio files including music tracks, podcasts,
     * voice recordings, and sound effects. Common formats include MP3, WAV,
     * FLAC, and AAC.</p>
     * 
     * <p>Numeric value: 1</p>
     */
    AUDIO(1),

    /**
     * Video media type representing moving visual content.
     * 
     * <p>This type is used for video files including movies, TV shows, video
     * clips, and animations. Common formats include MP4, AVI, MKV, and MOV.</p>
     * 
     * <p>Numeric value: 2</p>
     */
    VIDEO(2),

    /**
     * Image media type representing static visual content.
     * 
     * <p>This type is used for image files including photographs, drawings,
     * diagrams, and graphics. Common formats include JPEG, PNG, GIF, BMP,
     * and SVG.</p>
     * 
     * <p>Numeric value: 3</p>
     */
    IMAGE(3),

    /**
     * Text media type representing textual content.
     * 
     * <p>This type is used for text-based files including plain text documents,
     * source code, configuration files, and markup languages. Common formats
     * include TXT, HTML, XML, JSON, and various source code files.</p>
     * 
     * <p>Numeric value: 4</p>
     */
    TEXT(4),

    /**
     * Record media type representing structured data records.
     * 
     * <p>This type is used for structured data formats including database records,
     * CSV files, spreadsheets, and other tabular or hierarchical data structures.
     * It's particularly useful for data that needs to be processed programmatically.</p>
     * 
     * <p>Numeric value: 5</p>
     */
    RECORD(5);

    private final int intValue;

    /**
     * Constructs a MediaType with the specified integer value.
     * 
     * @param intValue the numeric value associated with this media type
     */
    MediaType(final int intValue) {
        this.intValue = intValue;
    }

    /**
     * Returns the numeric value associated with this media type.
     * 
     * <p>This method is useful for serialization, database storage, or when
     * interfacing with systems that use numeric codes for media types. The
     * returned value is guaranteed to be unique for each media type and
     * consistent across different instances of the application.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MediaType audioType = MediaType.AUDIO;
     * int audioCode = audioType.intValue(); // returns 1
     * 
     * // Storing in database
     * preparedStatement.setInt(1, mediaType.intValue());
     * }</pre>
     * 
     * @return the numeric value of this media type (0-5)
     */
    public int intValue() {
        return intValue;
    }

    /**
     * Returns the MediaType enum constant corresponding to the specified numeric value.
     * 
     * <p>This static factory method provides a way to convert from numeric media type
     * codes to the corresponding MediaType enum constant. This is particularly useful
     * when deserializing data, reading from databases, or working with external systems
     * that use numeric codes.</p>
     * 
     * <p>The method performs a fast lookup using a switch statement, ensuring optimal
     * performance even when called frequently.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Reading from database
     * int typeCode = resultSet.getInt("media_type");
     * MediaType type = MediaType.valueOf(typeCode);
     * 
     * // Deserializing from network protocol
     * MediaType received = MediaType.valueOf(buffer.readInt());
     * }</pre>
     * 
     * @param intValue the numeric value of the media type (must be between 0 and 5 inclusive)
     * @return the MediaType enum constant corresponding to the specified value
     * @throws IllegalArgumentException if the intValue does not correspond to any MediaType
     *         (i.e., if intValue is not in the range 0-5)
     */
    public static MediaType valueOf(final int intValue) {
        switch (intValue) {
            case 0:
                return BINARY;

            case 1:
                return AUDIO;

            case 2:
                return VIDEO;

            case 3:
                return IMAGE;

            case 4:
                return TEXT;

            case 5:
                return RECORD;

            default:
                throw new IllegalArgumentException("No mapping instance found by int value: " + intValue);
        }
    }
}