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
 * Enumeration of supported compression modes for data compression operations.
 * This enum defines the available compression algorithms that can be used throughout the framework.
 * 
 * <p>Supported compression modes:
 * <ul>
 *   <li>{@link #NONE} - No compression</li>
 *   <li>{@link #LZ4} - LZ4 compression algorithm (fast compression/decompression)</li>
 *   <li>{@link #SNAPPY} - Google's Snappy compression (optimized for speed)</li>
 *   <li>{@link #GZIP} - GNU zip compression (better compression ratio)</li>
 * </ul>
 * 
 * <p>Example usage:
 * <pre>{@code
 * CompressionMode mode = CompressionMode.GZIP;
 * // Use with compression utilities
 * byte[] compressed = compress(data, mode);
 * byte[] decompressed = decompress(compressed, mode);
 * }</pre>
 * 
 * @since 1.0
 * @see java.util.zip.GZIPOutputStream
 * @see java.util.zip.GZIPInputStream
 */
public enum CompressionMode {

    /**
     * No compression mode.
     * Data is stored or transmitted without any compression applied.
     * This mode offers the fastest processing but uses the most storage space.
     */
    NONE,

    /**
     * LZ4 compression mode.
     * LZ4 is a lossless compression algorithm focused on high-speed compression and decompression.
     * It offers the best performance among the compression algorithms but with moderate compression ratios.
     * Ideal for scenarios where speed is more important than compression ratio.
     */
    LZ4,

    /**
     * Snappy compression mode.
     * Google's Snappy compression algorithm optimized for speed rather than compression ratio.
     * Provides reasonable compression with very fast compression and decompression speeds.
     * Suitable for real-time compression scenarios where latency is critical.
     */
    SNAPPY,

    /**
     * GZIP compression mode.
     * GNU zip compression based on the DEFLATE algorithm.
     * Provides good compression ratios but is slower than LZ4 and Snappy.
     * Best suited when storage space is more important than compression speed.
     * Compatible with standard gzip tools and widely supported across platforms.
     */
    GZIP
}