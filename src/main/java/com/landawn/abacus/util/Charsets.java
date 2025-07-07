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
import java.nio.charset.StandardCharsets;
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
 * <p>Example usage:</p>
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
 * @since 1.0
 */
public final class Charsets {

    /**
     * Seven-bit ASCII, also known as ISO646-US, also known as the
     * Basic Latin block of the Unicode character set.
     * 
     * <p>This charset is guaranteed to be available on all Java platforms.</p>
     * 
     * <p>Example:</p>
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
     * <p>Example:</p>
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
     * <p>Example:</p>
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
     * <p>Example:</p>
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
     * <p>Example:</p>
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
     * <p>Example:</p>
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
     * <p>Example:</p>
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
     * Returns a charset object for the named charset.
     * 
     * <p>This method first checks an internal cache for the charset. If not found
     * in the cache, it creates a new charset instance using {@link Charset#forName}
     * and caches it for future use. This provides better performance than calling
     * {@code Charset.forName} directly when the same charset is used repeatedly.</p>
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Charset gbk = Charsets.get("GBK");
     * Charset utf32 = Charsets.get("UTF-32");
     * 
     * // These return the same cached instance
     * Charset utf8_1 = Charsets.get("UTF-8");
     * Charset utf8_2 = Charsets.get("UTF-8");
     * assert utf8_1 == utf8_2; // Same instance
     * }</pre>
     *
     * @param charsetName the name of the requested charset; may be either
     *        a canonical name or an alias
     * @return a charset object for the named charset
     * @throws IllegalArgumentException if the given charset name is illegal
     * @throws UnsupportedOperationException if no support for the named charset
     *         is available in this instance of the Java virtual machine
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