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

import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Map;

/**
 * A utility class providing convenient access to commonly used character encodings
 * and charset management functionality.
 * 
 * <p>This class provides static constants for standard charsets and a caching
 * mechanism for charset lookup by name. All standard charset constants are
 * guaranteed to be available on every implementation of the Java platform.</p>
 * 
 * <p>The class maintains an internal cache of charset instances to avoid the
 * overhead of repeated charset lookups. The cache is pre-populated with all
 * standard charsets and grows as new charsets are requested.</p>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Using predefined constants
 * byte[] utf8Bytes = "Hello".getBytes(Charsets.UTF_8);
 * String text = new String(utf8Bytes, Charsets.UTF_8);
 * 
 * // Getting charset by name (cached)
 * Charset gbk = Charsets.get("GBK");
 * byte[] gbkBytes = "你好".getBytes(gbk);
 * }</pre>
 * 
 * @see java.nio.charset.Charset
 * @see java.nio.charset.StandardCharsets
 */
public final class Charsets {

    /**
     * Seven-bit ASCII, also known as ISO646-US, also known as the
     * Basic Latin block of the Unicode character set.
     * 
     * <p>This charset is guaranteed to be available on all Java platforms.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] asciiBytes = "Hello".getBytes(Charsets.US_ASCII);
     * }</pre>
     */
    public static final Charset US_ASCII = StandardCharsets.US_ASCII;

    /**
     * ISO Latin Alphabet No. 1, also known as ISO-LATIN-1.
     * 
     * <p>This charset is guaranteed to be available on all Java platforms.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] latinBytes = "Café".getBytes(Charsets.ISO_8859_1);
     * }</pre>
     */
    public static final Charset ISO_8859_1 = StandardCharsets.ISO_8859_1;

    /**
     * Eight-bit UCS Transformation Format.
     * 
     * <p>UTF-8 is the most widely used character encoding on the web and
     * is recommended for new applications. This charset is guaranteed to
     * be available on all Java platforms.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] utf8Bytes = "Hello 世界".getBytes(Charsets.UTF_8);
     * String text = new String(utf8Bytes, Charsets.UTF_8);
     * }</pre>
     */
    public static final Charset UTF_8 = StandardCharsets.UTF_8;

    /**
     * Sixteen-bit UCS Transformation Format, byte order identified by an
     * optional byte-order mark.
     * 
     * <p>This charset is guaranteed to be available on all Java platforms.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] utf16Bytes = "Hello".getBytes(Charsets.UTF_16);
     * }</pre>
     */
    public static final Charset UTF_16 = StandardCharsets.UTF_16;

    /**
     * Sixteen-bit UCS Transformation Format, big-endian byte order.
     * 
     * <p>This charset is guaranteed to be available on all Java platforms.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] utf16beBytes = "Hello".getBytes(Charsets.UTF_16BE);
     * }</pre>
     */
    public static final Charset UTF_16BE = StandardCharsets.UTF_16BE;

    /**
     * Sixteen-bit UCS Transformation Format, little-endian byte order.
     * 
     * <p>This charset is guaranteed to be available on all Java platforms.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] utf16leBytes = "Hello".getBytes(Charsets.UTF_16LE);
     * }</pre>
     */
    public static final Charset UTF_16LE = StandardCharsets.UTF_16LE;

    /**
     * Returns the default charset of this Java virtual machine.
     * 
     * <p>The default charset is determined during virtual-machine startup
     * and typically depends upon the locale and charset of the underlying
     * operating system.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Charset systemDefault = Charsets.DEFAULT;
     * System.out.println("System default charset: " + systemDefault.name());
     * }</pre>
     */
    public static final Charset DEFAULT = Charset.defaultCharset();

    private static final Map<String, Charset> charsetPool = new ObjectPool<>(128);

    static {
        charsetPool.put(US_ASCII.name(), US_ASCII);
        charsetPool.put(ISO_8859_1.name(), ISO_8859_1);
        charsetPool.put(UTF_8.name(), UTF_8);
        charsetPool.put(UTF_16BE.name(), UTF_16BE);
        charsetPool.put(UTF_16LE.name(), UTF_16LE);
        charsetPool.put(UTF_16.name(), UTF_16);
    }

    private Charsets() {
        // singleton.
    }

    /**
     * Returns a charset object for the named charset, utilizing an internal cache for improved performance.
     *
     * <p>This method provides efficient access to charset instances by maintaining an internal cache.
     * The cache is pre-populated with all standard charsets ({@code US-ASCII}, {@code ISO-8859-1},
     * {@code UTF-8}, {@code UTF-16}, {@code UTF-16BE}, {@code UTF-16LE}). When a charset is requested:</p>
     * <ol>
     *   <li>The method first checks the internal cache for an existing instance</li>
     *   <li>If found, the cached instance is returned immediately</li>
     *   <li>If not found, a new charset is created via {@link Charset#forName(String)}</li>
     *   <li>The newly created charset is cached for future requests</li>
     * </ol>
     *
     * <p>This caching mechanism significantly improves performance compared to repeatedly calling
     * {@code Charset.forName()} directly, especially in scenarios where the same charset is accessed
     * frequently. The method is thread-safe, ensuring correct behavior in concurrent environments.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Get charset by canonical name
     * Charset gbk = Charsets.get("GBK");
     * byte[] bytes = "你好".getBytes(gbk);
     *
     * // Get charset by alias
     * Charset utf32 = Charsets.get("UTF-32");
     *
     * // Multiple calls return the same cached instance (reference equality)
     * Charset utf8_1 = Charsets.get("UTF-8");
     * Charset utf8_2 = Charsets.get("UTF-8");
     * assert utf8_1 == utf8_2;  // true - same cached instance
     * }</pre>
     *
     * @param charsetName the name of the requested charset; may be either a canonical name
     *                    (e.g., "UTF-8") or an alias (e.g., "utf8"). Must not be {@code null}.
     * @return a charset object for the named charset, either from cache or newly created
     * @throws IllegalCharsetNameException if the given charset name is illegal (as defined by
     *         {@link Charset#forName(String)})
     * @throws IllegalArgumentException if the charset name is null
     * @throws UnsupportedCharsetException if no support for the named charset is available
     *         in this instance of the Java virtual machine
     * @see Charset#forName(String)
     * @see StandardCharsets
     */
    public static Charset get(final String charsetName) {
        Charset charset = charsetPool.get(charsetName);

        if (charset == null) {
            charset = Charset.forName(charsetName);
            charsetPool.put(charsetName, charset);
        }

        return charset;
    }
}
